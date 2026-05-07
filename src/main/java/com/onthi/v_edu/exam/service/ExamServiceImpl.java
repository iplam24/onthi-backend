package com.onthi.v_edu.exam.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.dto.PageResponse;
import com.onthi.v_edu.config.security.services.UserDetailsImpl;
import com.onthi.v_edu.exam.dto.ExamQuestionItemRequest;
import com.onthi.v_edu.exam.dto.ExamQuestionItemResponse;
import com.onthi.v_edu.exam.dto.ExamRequest;
import com.onthi.v_edu.exam.dto.ExamResponse;
import com.onthi.v_edu.exam.dto.ExamSectionResponse;
import com.onthi.v_edu.exam.dto.QuestionOptionResponse;
import com.onthi.v_edu.exam.entity.Exam;
import com.onthi.v_edu.exam.entity.ExamQuestion;
import com.onthi.v_edu.exam.entity.ExamQuestionId;
import com.onthi.v_edu.exam.repository.ExamQuestionRepository;
import com.onthi.v_edu.exam.repository.ExamRepository;
import com.onthi.v_edu.learning.entity.Subject;
import com.onthi.v_edu.learning.repository.SubjectRepository;
import com.onthi.v_edu.question.entity.Question;
import com.onthi.v_edu.question.repository.QuestionOptionRepository;
import com.onthi.v_edu.question.repository.QuestionRepository;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.repository.UserRepository;
import com.onthi.v_edu.common.constant.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamServiceImpl implements ExamService {

	private final ExamRepository examRepository;
	private final ExamQuestionRepository examQuestionRepository;
	private final SubjectRepository subjectRepository;
	private final QuestionRepository questionRepository;
	private final UserRepository userRepository;
	private final QuestionOptionRepository questionOptionRepository;

	public ExamServiceImpl(ExamRepository examRepository,
						   ExamQuestionRepository examQuestionRepository,
						   SubjectRepository subjectRepository,
						   QuestionRepository questionRepository,
						   UserRepository userRepository,
						   QuestionOptionRepository questionOptionRepository) {
		this.examRepository = examRepository;
		this.examQuestionRepository = examQuestionRepository;
		this.subjectRepository = subjectRepository;
		this.questionRepository = questionRepository;
		this.userRepository = userRepository;
		this.questionOptionRepository = questionOptionRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResponse<ExamResponse>> getAllExams(Pageable pageable) {
		Page<ExamResponse> data = examRepository.findByDeletedAtIsNull(pageable)
				.map(this::toExamResponse);
		return new ApiResponse<>(HttpStatus.OK.value(), "Lấy danh sách đề thi thành công!", PageResponse.from(data));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResponse<ExamResponse>> getExamsBySubjectId(Integer subjectId, Pageable pageable) {
		if (!subjectRepository.existsById(subjectId)) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy subject!");
		}

		Page<ExamResponse> data = examRepository.findBySubject_IdAndDeletedAtIsNull(subjectId, pageable)
				.map(this::toExamResponse);
		return new ApiResponse<>(HttpStatus.OK.value(), "Lấy danh sách đề thi theo môn học thành công!", PageResponse.from(data));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<ExamResponse> getExamById(Integer id) {
		return examRepository.findByIdAndDeletedAtIsNull(id)
				.map(exam -> new ApiResponse<>(HttpStatus.OK.value(), "Lấy đề thi thành công!", toExamResponse(exam)))
				.orElseGet(() -> new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy đề thi!"));
	}

	@Override
	public ApiResponse<ExamResponse> createExam(ExamRequest request) {
		Subject subject = subjectRepository.findById(request.getSubjectId()).orElse(null);
		if (subject == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy subject!");
		}

		String validationError = validateRequest(request, subject.getId());
		if (validationError != null) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), validationError);
		}

		User currentUser = getCurrentUser();
		if (currentUser == null) {
			return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để thực hiện thao tác này!");
		}

		Exam exam = new Exam();
		applyExamFields(exam, request, subject);
		exam.setCreatedBy(currentUser);
		exam.setCreatedAt(LocalDateTime.now());
		exam = examRepository.save(exam);

		syncExamQuestions(exam, request.getQuestions());
		return new ApiResponse<>(HttpStatus.CREATED.value(), "Tạo đề thi thành công!", toExamResponse(exam));
	}

	@Override
	public ApiResponse<ExamResponse> updateExam(Integer id, ExamRequest request) {
		Exam exam = examRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
		if (exam == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy đề thi!");
		}

		Subject subject = subjectRepository.findById(request.getSubjectId()).orElse(null);
		if (subject == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy subject!");
		}

		String validationError = validateRequest(request, subject.getId());
		if (validationError != null) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), validationError);
		}

		applyExamFields(exam, request, subject);
		exam.setUpdatedAt(LocalDateTime.now());
		exam = examRepository.save(exam);

		syncExamQuestions(exam, request.getQuestions());
		return new ApiResponse<>(HttpStatus.OK.value(), "Cập nhật đề thi thành công!", toExamResponse(exam));
	}

	@Override
	public ApiResponse<Void> deleteExam(Integer id) {
		Exam exam = examRepository.findById(id).orElse(null);
		if (exam == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy đề thi!");
		}

		examQuestionRepository.softDeleteByExamId(id);
		examRepository.delete(exam);
		return new ApiResponse<>(HttpStatus.OK.value(), "Xoá đề thi thành công!");
	}

	private String validateRequest(ExamRequest request, Integer subjectId) {
		if (request.getDuration() == null || request.getDuration() <= 0) {
			return "Thời lượng đề thi phải lớn hơn 0!";
		}
		if (request.getStartTime() != null && request.getEndTime() != null
				&& request.getEndTime().isBefore(request.getStartTime())) {
			return "Thời gian kết thúc phải sau thời gian bắt đầu!";
		}

		List<ExamQuestionItemRequest> questionItems = request.getQuestions() == null
				? Collections.emptyList()
				: request.getQuestions();
		Set<Integer> uniqueIds = new HashSet<>();
		for (ExamQuestionItemRequest item : questionItems) {
			Integer questionId = item.getQuestionId();
			if (questionId == null) {
				return "Danh sách câu hỏi có phần tử thiếu questionId!";
			}
			if (!uniqueIds.add(questionId)) {
				return "Danh sách câu hỏi bị trùng questionId!";
			}
			Question question = questionRepository.findByIdAndDeletedAtIsNull(questionId).orElse(null);
			if (question == null) {
				return "Không tìm thấy question với id=" + questionId;
			}

			Integer questionSubjectId = question.getTopic() != null && question.getTopic().getSubject() != null
					? question.getTopic().getSubject().getId()
					: null;
			if (!subjectId.equals(questionSubjectId)) {
				return "Question id=" + questionId + " không thuộc môn học của đề thi";
			}
		}
		return null;
	}

	private void applyExamFields(Exam exam, ExamRequest request, Subject subject) {
		exam.setTitle(normalize(request.getTitle()));
		exam.setSubject(subject);
		exam.setDuration(request.getDuration());
		exam.setIsActive(request.getIsActive() != null ? request.getIsActive() : Boolean.FALSE);
		exam.setStartTime(request.getStartTime());
		exam.setEndTime(request.getEndTime());
		exam.setTotalScore(request.getTotalScore());
		exam.setType(normalize(request.getType()));
		exam.setUiLayoutHint(normalize(request.getUiLayoutHint()));
		exam.setShuffleQuestions(request.getShuffleQuestions() != null ? request.getShuffleQuestions() : Boolean.FALSE);
		exam.setShuffleAnswers(request.getShuffleAnswers() != null ? request.getShuffleAnswers() : Boolean.FALSE);
		exam.setMaxAttempts(request.getMaxAttempts());
	}

	private void syncExamQuestions(Exam exam, List<ExamQuestionItemRequest> items) {
		Integer examId = exam.getId();
		examQuestionRepository.softDeleteByExamId(examId);
		if (items == null || items.isEmpty()) {
			return;
		}

		List<ExamQuestion> examQuestions = items.stream()
				.map(item -> buildExamQuestion(exam, item))
				.toList();
		examQuestionRepository.saveAll(examQuestions);
	}

	private ExamQuestion buildExamQuestion(Exam exam, ExamQuestionItemRequest item) {
		Question question = questionRepository.findByIdAndDeletedAtIsNull(item.getQuestionId()).orElseThrow();

		ExamQuestion examQuestion = new ExamQuestion();
		examQuestion.setId(new ExamQuestionId(exam.getId(), question.getId()));
		examQuestion.setExam(exam);
		examQuestion.setQuestion(question);
		examQuestion.setOrderIndex(item.getOrderIndex());
		examQuestion.setScore(item.getScore());
		examQuestion.setContentSnapshot(normalize(item.getContentSnapshot()));
		examQuestion.setContentFormatSnapshot(item.getContentFormatSnapshot() != null ? item.getContentFormatSnapshot() : question.getContentFormat());
		return examQuestion;
	}

	private ExamResponse toExamResponse(Exam exam) {
		Subject subject = exam.getSubject();
		User creator = exam.getCreatedBy();
		Integer subjectId = subject != null ? subject.getId() : null;
		String subjectName = subject != null ? subject.getName() : null;
		Integer createdById = creator != null ? creator.getId() : null;
		String createdByUsername = creator != null ? creator.getUsername() : null;

		List<ExamQuestionItemResponse> questionItems = examQuestionRepository
				.findByExam_IdAndDeletedAtIsNullOrderByOrderIndexAscQuestion_IdAsc(exam.getId())
				.stream()
				.map(item -> {
					Question question = item.getQuestion();
					if (question == null) {
						return null;
					}

						List<QuestionOptionResponse> options = questionOptionRepository
								.findByQuestion_IdAndDeletedAtIsNullOrderByIdAsc(question.getId())
							.stream()
							.map(option -> new QuestionOptionResponse(option.getId(), option.getContent(), option.getIsCorrect()))
							.collect(Collectors.toList());

					return new ExamQuestionItemResponse(
							question.getId(),
							question.getContent(),
							question.getContentFormat(),
							question.getType(),
							question.getUrl(), // Added
							item.getOrderIndex(),
							item.getScore(),
							item.getContentSnapshot(),
							item.getContentFormatSnapshot() != null ? item.getContentFormatSnapshot() : question.getContentFormat(),
							options
					);
				})
				.filter(java.util.Objects::nonNull)
				.toList();

		String uiLayoutHint = exam.getUiLayoutHint() != null && !exam.getUiLayoutHint().isEmpty() 
		        ? exam.getUiLayoutHint() 
		        : resolveUiLayoutHint(subjectName, questionItems);
		List<ExamSectionResponse> sections = buildSections(questionItems);

		return new ExamResponse(
				exam.getId(),
				exam.getTitle(),
				subjectId,
				subjectName,
				createdById,
				createdByUsername,
				exam.getDuration(),
				exam.getIsActive(),
				exam.getStartTime(),
				exam.getEndTime(),
				exam.getTotalScore(),
				exam.getType(),
							uiLayoutHint,
							sections,
				exam.getShuffleQuestions(),
				exam.getShuffleAnswers(),
				exam.getMaxAttempts(),
				exam.getCreatedAt(),
				exam.getUpdatedAt(),
				questionItems
		);
	}

	private List<ExamSectionResponse> buildSections(List<ExamQuestionItemResponse> questionItems) {
		if (questionItems == null || questionItems.isEmpty()) {
			return Collections.emptyList();
		}
	
		// Group questions by their type (MCQ, ESSAY, etc.)
		Map<QuestionType, List<ExamQuestionItemResponse>> groupedByType = questionItems.stream()
				.collect(Collectors.groupingBy(item -> {
					QuestionType type = item.getQuestionType();
					return type == null ? QuestionType.MCQ : type;
				}));
	
		List<ExamSectionResponse> sections = new ArrayList<>();
		int sectionIndex = 0;
	
		// Define the order of sections explicitly: MCQ first, then ESSAY
		List<QuestionType> sectionOrder = List.of(QuestionType.MCQ, QuestionType.ESSAY);
	
		for (QuestionType type : sectionOrder) {
			if (groupedByType.containsKey(type)) {
				List<ExamQuestionItemResponse> questionsForSection = groupedByType.get(type);
				if (questionsForSection.isEmpty()) {
					continue;
				}
	
				// Sort questions within the section by their original orderIndex
				questionsForSection.sort(Comparator.comparingInt(item -> item.getOrderIndex() == null ? Integer.MAX_VALUE : item.getOrderIndex()));
	
				int startOrder = questionsForSection.get(0).getOrderIndex() != null ? questionsForSection.get(0).getOrderIndex() : 0;
				int endOrder = questionsForSection.get(questionsForSection.size() - 1).getOrderIndex() != null ? questionsForSection.get(questionsForSection.size() - 1).getOrderIndex() : 0;
				double totalScore = questionsForSection.stream().mapToDouble(q -> q.getScore() == null ? 0 : q.getScore()).sum();
	
				sections.add(buildSection(
						++sectionIndex,
						type,
						questionsForSection,
						startOrder,
						endOrder,
						totalScore
				));
			}
		}
	
		// Add any other question types that might exist but are not in the predefined order
		for (Map.Entry<QuestionType, List<ExamQuestionItemResponse>> entry : groupedByType.entrySet()) {
			if (!sectionOrder.contains(entry.getKey())) {
				List<ExamQuestionItemResponse> questionsForSection = entry.getValue();
				if (questionsForSection.isEmpty()) {
					continue;
				}
				questionsForSection.sort(Comparator.comparingInt(item -> item.getOrderIndex() == null ? Integer.MAX_VALUE : item.getOrderIndex()));
				int startOrder = questionsForSection.get(0).getOrderIndex() != null ? questionsForSection.get(0).getOrderIndex() : 0;
				int endOrder = questionsForSection.get(questionsForSection.size() - 1).getOrderIndex() != null ? questionsForSection.get(questionsForSection.size() - 1).getOrderIndex() : 0;
				double totalScore = questionsForSection.stream().mapToDouble(q -> q.getScore() == null ? 0 : q.getScore()).sum();
	
				sections.add(buildSection(
						++sectionIndex,
						entry.getKey(),
						questionsForSection,
						startOrder,
						endOrder,
						totalScore
				));
			}
		}

		return sections;
	}

	private ExamSectionResponse buildSection(int sectionIndex,
	                                       QuestionType sectionType,
	                                       List<ExamQuestionItemResponse> questions,
	                                       int startOrderIndex,
	                                       int endOrderIndex,
	                                       double totalScore) {
		String title = buildSectionTitle(sectionIndex, sectionType);
		String type = sectionType == null ? "MIXED" : sectionType.name();
		return new ExamSectionResponse(
				sectionIndex,
				title,
				type,
				questions.size(),
				totalScore,
				startOrderIndex,
				endOrderIndex,
				List.copyOf(questions)
		);
	}

	private String buildSectionTitle(int sectionIndex, QuestionType type) {
		String prefix = switch (sectionIndex) {
			case 1 -> "Phần 1";
			case 2 -> "Phần 2";
			case 3 -> "Phần 3";
			case 4 -> "Phần 4";
			case 5 -> "Phần 5";
			case 6 -> "Phần 6";
			default -> "Phần " + sectionIndex;
		};

		String suffix = switch (type == null ? QuestionType.MCQ : type) {
			case ESSAY -> " - Tự luận";
			case MCQ -> " - Trắc nghiệm";
			default -> "";
		};
		return prefix + suffix;
	}

	private String resolveUiLayoutHint(String subjectName, List<ExamQuestionItemResponse> questions) {
		String normalizedSubject = subjectName == null ? "" : subjectName.trim().toLowerCase(Locale.ROOT);
		boolean looksLikeLiterature = normalizedSubject.contains("văn") || normalizedSubject.contains("ngu van") || normalizedSubject.contains("literature");

		boolean hasEssay = false;
		boolean hasMcq = false;
		for (ExamQuestionItemResponse item : questions) {
			if (item == null || item.getQuestionType() == null) {
				continue;
			}
			if (item.getQuestionType() == QuestionType.ESSAY) {
				hasEssay = true;
			} else if (item.getQuestionType() == QuestionType.MCQ) {
				hasMcq = true;
			}
		}

		if (looksLikeLiterature) {
			return hasMcq ? "MIXED" : "LITERATURE";
		}
		if (hasEssay && hasMcq) {
			return "MIXED";
		}
		if (hasEssay) {
			return "ESSAY";
		}
		return "STANDARD";
	}

	private User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return null;
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof UserDetailsImpl userDetails) {
			return userRepository.findById(userDetails.getId()).orElse(null);
		}

		String username = authentication.getName();
		if (isBlank(username)) {
			return null;
		}
		return userRepository.findByUsername(username).orElse(null);
	}

	private String normalize(String value) {
		return value == null ? null : value.trim();
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
