package com.onthi.v_edu.exam.service;

import com.onthi.v_edu.attempt.entity.Answer;
import com.onthi.v_edu.attempt.entity.Attempt;
import com.onthi.v_edu.attempt.repository.AnswerRepository;
import com.onthi.v_edu.attempt.repository.AttemptRepository;
import com.onthi.v_edu.common.constant.AttemptStatus;
import com.onthi.v_edu.common.constant.DifficultyLevel;
import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.dto.PageResponse;
import com.onthi.v_edu.config.security.services.UserDetailsImpl;
import com.onthi.v_edu.exam.dto.ExamPerformanceResponse;
import com.onthi.v_edu.exam.dto.ExamQuestionItemRequest;
import com.onthi.v_edu.exam.dto.ExamQuestionItemResponse;
import com.onthi.v_edu.exam.dto.ExamRequest;
import com.onthi.v_edu.exam.dto.ExamResponse;
import com.onthi.v_edu.exam.dto.ExamSectionResponse;
import com.onthi.v_edu.exam.dto.QuestionOptionResponse;
import com.onthi.v_edu.exam.dto.RandomExamRequest;
import com.onthi.v_edu.exam.dto.RandomExamResponse;
import com.onthi.v_edu.exam.dto.UserExamHistoryResponse;
import com.onthi.v_edu.exam.entity.Exam;
import com.onthi.v_edu.exam.entity.ExamQuestion;
import com.onthi.v_edu.exam.entity.ExamQuestionId;
import com.onthi.v_edu.exam.repository.ExamQuestionRepository;
import com.onthi.v_edu.exam.repository.ExamRepository;
import com.onthi.v_edu.learning.entity.Subject;
import com.onthi.v_edu.learning.entity.Topic;
import com.onthi.v_edu.learning.repository.SubjectRepository;
import com.onthi.v_edu.learning.repository.TopicRepository;
import com.onthi.v_edu.question.entity.Question;
import com.onthi.v_edu.question.repository.QuestionOptionRepository;
import com.onthi.v_edu.question.repository.QuestionRepository;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.repository.UserRepository;
import com.onthi.v_edu.common.constant.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
	private final AttemptRepository attemptRepository;
	private final AnswerRepository answerRepository;
	private final TopicRepository topicRepository;

	public ExamServiceImpl(ExamRepository examRepository,
						   ExamQuestionRepository examQuestionRepository,
						   SubjectRepository subjectRepository,
						   QuestionRepository questionRepository,
						   UserRepository userRepository,
						   QuestionOptionRepository questionOptionRepository,
						   AttemptRepository attemptRepository,
						   AnswerRepository answerRepository,
						   TopicRepository topicRepository) {
		this.examRepository = examRepository;
		this.examQuestionRepository = examQuestionRepository;
		this.subjectRepository = subjectRepository;
		this.questionRepository = questionRepository;
		this.userRepository = userRepository;
		this.questionOptionRepository = questionOptionRepository;
		this.attemptRepository = attemptRepository;
		this.answerRepository = answerRepository;
		this.topicRepository = topicRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResponse<ExamResponse>> getAllExams(Pageable pageable) {
		User currentUser = getCurrentUser();
		Page<Exam> examPage;
		if (currentUser != null && currentUser.getRole() != null && "ROLE_ADMIN".equalsIgnoreCase(currentUser.getRole().getName())) {
			examPage = examRepository.findByDeletedAtIsNull(pageable);
		} else {
			Integer userId = currentUser != null ? currentUser.getId() : -1;
			examPage = examRepository.findVisibleExams(userId, pageable);
		}
		Page<ExamResponse> data = examPage.map(this::toExamResponse);
		return new ApiResponse<>(HttpStatus.OK.value(), "Lấy danh sách đề thi thành công!", PageResponse.from(data));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResponse<ExamResponse>> getExamsBySubjectId(Integer subjectId, Pageable pageable) {
		if (!subjectRepository.existsById(subjectId)) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy subject!");
		}

		User currentUser = getCurrentUser();
		Page<Exam> examPage;
		if (currentUser != null && currentUser.getRole() != null && "ROLE_ADMIN".equalsIgnoreCase(currentUser.getRole().getName())) {
			examPage = examRepository.findBySubject_IdAndDeletedAtIsNull(subjectId, pageable);
		} else {
			Integer userId = currentUser != null ? currentUser.getId() : -1;
			examPage = examRepository.findVisibleExamsBySubject(subjectId, userId, pageable);
		}
		Page<ExamResponse> data = examPage.map(this::toExamResponse);
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
		exam.setIsPublic(request.getIsPublic());
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
				exam.getIsPublic() != null ? exam.getIsPublic() : "MANUAL".equalsIgnoreCase(exam.getType()),
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

	// ========================================================================================
	// ===========================  RANDOM EXAM GENERATION  ==================================
	// ========================================================================================

	private static final List<AttemptStatus> COMPLETED_STATUSES = List.of(AttemptStatus.SUBMITTED, AttemptStatus.EXPIRED);

	@Override
	public ApiResponse<RandomExamResponse> generateRandomExam(RandomExamRequest request) {
		User currentUser = getCurrentUser();
		if (currentUser == null) {
			return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập!");
		}

		Subject subject = subjectRepository.findById(request.getSubjectId()).orElse(null);
		if (subject == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy môn học!");
		}

		// Validate difficulty configs
		if (request.getDifficultyConfigs() != null && !request.getDifficultyConfigs().isEmpty()) {
			int diffTotal = request.getDifficultyConfigs().stream()
					.mapToInt(RandomExamRequest.DifficultyConfig::getCount).sum();
			if (diffTotal != request.getTotalQuestions()) {
				return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(),
						"Tổng số câu trong difficultyConfigs (" + diffTotal + ") phải bằng totalQuestions (" + request.getTotalQuestions() + ")!");
			}
		}

		// Validate topic configs
		if (request.getTopicConfigs() != null && !request.getTopicConfigs().isEmpty()) {
			int topicTotal = request.getTopicConfigs().stream()
					.mapToInt(RandomExamRequest.TopicConfig::getCount).sum();
			if (topicTotal != request.getTotalQuestions()) {
				return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(),
						"Tổng số câu trong topicConfigs (" + topicTotal + ") phải bằng totalQuestions (" + request.getTotalQuestions() + ")!");
			}
			for (RandomExamRequest.TopicConfig tc : request.getTopicConfigs()) {
				if (!topicRepository.existsById(tc.getTopicId())) {
					return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy topic với id=" + tc.getTopicId());
				}
			}
		}

		// Validate topic detailed configs
		if (request.getTopicDetailedConfigs() != null && !request.getTopicDetailedConfigs().isEmpty()) {
			int detailedTotal = 0;
			for (RandomExamRequest.TopicDetailedConfig tc : request.getTopicDetailedConfigs()) {
				int easy = tc.getEasyCount() != null ? tc.getEasyCount() : 0;
				int medium = tc.getMediumCount() != null ? tc.getMediumCount() : 0;
				int hard = tc.getHardCount() != null ? tc.getHardCount() : 0;
				if (easy < 0 || medium < 0 || hard < 0) {
					return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Số lượng câu hỏi không được âm!");
				}
				detailedTotal += (easy + medium + hard);
				if (!topicRepository.existsById(tc.getTopicId())) {
					return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy topic với id=" + tc.getTopicId());
				}
			}
			if (detailedTotal != request.getTotalQuestions()) {
				return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(),
						"Tổng số câu trong topicDetailedConfigs (" + detailedTotal + ") phải bằng totalQuestions (" + request.getTotalQuestions() + ")!");
			}
		}

		// Build exclude list for duplicate avoidance
		boolean avoidDuplicates = !Boolean.FALSE.equals(request.getAvoidDuplicates());
		List<Integer> excludeIds = new ArrayList<>();
		if (avoidDuplicates) {
			List<Integer> usedIds = examQuestionRepository.findUsedQuestionIdsByUserId(currentUser.getId());
			if (usedIds != null && !usedIds.isEmpty()) {
				excludeIds.addAll(usedIds);
			}
		}
		if (excludeIds.isEmpty()) {
			excludeIds.add(-1); // placeholder to avoid empty IN clause
		}

		// Select questions
		List<Question> selectedQuestions;
		boolean hasDuplicates = false;

		if (request.getTopicDetailedConfigs() != null && !request.getTopicDetailedConfigs().isEmpty()) {
			selectedQuestions = selectByTopicDetailed(request, excludeIds);
		} else if (request.getTopicConfigs() != null && !request.getTopicConfigs().isEmpty()
				&& request.getDifficultyConfigs() != null && !request.getDifficultyConfigs().isEmpty()) {
			// Both topic and difficulty specified → distribute proportionally
			selectedQuestions = selectByTopicAndDifficulty(request, excludeIds);
		} else if (request.getTopicConfigs() != null && !request.getTopicConfigs().isEmpty()) {
			// Only topic specified
			selectedQuestions = selectByTopicOnly(request, excludeIds);
		} else if (request.getDifficultyConfigs() != null && !request.getDifficultyConfigs().isEmpty()) {
			// Only difficulty specified → from entire subject
			selectedQuestions = selectByDifficultyOnly(request, excludeIds);
		} else {
			// No config → random from subject
			selectedQuestions = questionRepository.findRandomBySubject(
					request.getSubjectId(), excludeIds, PageRequest.of(0, request.getTotalQuestions()));
		}

		// Fallback: if not enough questions, retry without exclusion
		if (selectedQuestions.size() < request.getTotalQuestions() && avoidDuplicates) {
			hasDuplicates = true;
			List<Integer> fallbackExclude = List.of(-1);
			if (request.getTopicDetailedConfigs() != null && !request.getTopicDetailedConfigs().isEmpty()) {
				selectedQuestions = selectByTopicDetailed(request, fallbackExclude);
			} else if (request.getTopicConfigs() != null && !request.getTopicConfigs().isEmpty()
					&& request.getDifficultyConfigs() != null && !request.getDifficultyConfigs().isEmpty()) {
				selectedQuestions = selectByTopicAndDifficulty(request, fallbackExclude);
			} else if (request.getTopicConfigs() != null && !request.getTopicConfigs().isEmpty()) {
				selectedQuestions = selectByTopicOnly(request, fallbackExclude);
			} else if (request.getDifficultyConfigs() != null && !request.getDifficultyConfigs().isEmpty()) {
				selectedQuestions = selectByDifficultyOnly(request, fallbackExclude);
			} else {
				selectedQuestions = questionRepository.findRandomBySubject(
						request.getSubjectId(), fallbackExclude, PageRequest.of(0, request.getTotalQuestions()));
			}
		}

		if (selectedQuestions.isEmpty()) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Không đủ câu hỏi trong ngân hàng đề để tạo đề thi!");
		}

		// Create Exam entity
		String title = isBlank(request.getTitle())
				? "Đề thi ngẫu nhiên - " + subject.getName() + " - " + LocalDateTime.now().toLocalDate()
				: request.getTitle().trim();

		Exam exam = new Exam();
		exam.setTitle(title);
		exam.setSubject(subject);
		exam.setCreatedBy(currentUser);
		exam.setDuration(request.getDuration());
		exam.setIsActive(true);
		exam.setTotalScore((double) selectedQuestions.size());
		exam.setType("AUTO");
		exam.setIsPublic(false);
		exam.setShuffleQuestions(true);
		exam.setShuffleAnswers(true);
		exam.setMaxAttempts(request.getMaxAttempts());
		exam.setCreatedAt(LocalDateTime.now());
		exam = examRepository.save(exam);

		// Create ExamQuestion entries
		List<ExamQuestion> examQuestions = new ArrayList<>();
		for (int i = 0; i < selectedQuestions.size(); i++) {
			Question q = selectedQuestions.get(i);
			ExamQuestion eq = new ExamQuestion();
			eq.setId(new ExamQuestionId(exam.getId(), q.getId()));
			eq.setExam(exam);
			eq.setQuestion(q);
			eq.setOrderIndex(i + 1);
			eq.setScore(1.0);
			eq.setContentSnapshot(q.getContent());
			eq.setContentFormatSnapshot(q.getContentFormat());
			examQuestions.add(eq);
		}
		examQuestionRepository.saveAll(examQuestions);

		// Build distribution stats
		Map<String, Integer> diffDist = new LinkedHashMap<>();
		Map<String, Integer> topicDist = new LinkedHashMap<>();
		for (Question q : selectedQuestions) {
			String diff = q.getDifficulty() != null ? q.getDifficulty().name() : "UNKNOWN";
			diffDist.merge(diff, 1, Integer::sum);
			String topicName = q.getTopic() != null ? q.getTopic().getName() : "Không xác định";
			topicDist.merge(topicName, 1, Integer::sum);
		}

		RandomExamResponse response = new RandomExamResponse(
				exam.getId(), exam.getTitle(),
				subject.getId(), subject.getName(),
				exam.getDuration(), selectedQuestions.size(),
				diffDist, topicDist,
				!Boolean.FALSE.equals(request.getAllowRetake()),
				request.getMaxAttempts(),
				hasDuplicates,
				exam.getCreatedAt()
		);

		String msg = hasDuplicates
				? "Tạo đề thi thành công! (Lưu ý: do ngân hàng câu hỏi hạn chế, một số câu có thể trùng với đề cũ)"
				: "Tạo đề thi ngẫu nhiên thành công!";
		return new ApiResponse<>(HttpStatus.CREATED.value(), msg, response);
	}

	private List<Question> selectByTopicAndDifficulty(RandomExamRequest request, List<Integer> excludeIds) {
		List<Question> result = new ArrayList<>();
		List<RandomExamRequest.DifficultyConfig> diffs = request.getDifficultyConfigs();
		int totalDiffCount = diffs.stream().mapToInt(RandomExamRequest.DifficultyConfig::getCount).sum();

		for (RandomExamRequest.TopicConfig tc : request.getTopicConfigs()) {
			for (RandomExamRequest.DifficultyConfig dc : diffs) {
				int count = (int) Math.round((double) tc.getCount() * dc.getCount() / totalDiffCount);
				if (count <= 0) continue;
				List<Question> questions = questionRepository.findRandomByTopicAndDifficulty(
						tc.getTopicId(), dc.getDifficulty(), excludeIds, PageRequest.of(0, count));
				result.addAll(questions);
			}
		}
		return result;
	}

	private List<Question> selectByTopicOnly(RandomExamRequest request, List<Integer> excludeIds) {
		List<Question> result = new ArrayList<>();
		for (RandomExamRequest.TopicConfig tc : request.getTopicConfigs()) {
			List<Question> questions = questionRepository.findRandomByTopic(
					tc.getTopicId(), excludeIds, PageRequest.of(0, tc.getCount()));
			result.addAll(questions);
		}
		return result;
	}

	private List<Question> selectByDifficultyOnly(RandomExamRequest request, List<Integer> excludeIds) {
		List<Question> result = new ArrayList<>();
		for (RandomExamRequest.DifficultyConfig dc : request.getDifficultyConfigs()) {
			List<Question> questions = questionRepository.findRandomBySubjectAndDifficulty(
					request.getSubjectId(), dc.getDifficulty(), excludeIds, PageRequest.of(0, dc.getCount()));
			result.addAll(questions);
		}
		return result;
	}

	private List<Question> selectByTopicDetailed(RandomExamRequest request, List<Integer> excludeIds) {
		List<Question> result = new ArrayList<>();
		for (RandomExamRequest.TopicDetailedConfig tc : request.getTopicDetailedConfigs()) {
			int easy = tc.getEasyCount() != null ? tc.getEasyCount() : 0;
			int medium = tc.getMediumCount() != null ? tc.getMediumCount() : 0;
			int hard = tc.getHardCount() != null ? tc.getHardCount() : 0;

			if (easy > 0) {
				List<Question> questions = questionRepository.findRandomByTopicAndDifficulty(
						tc.getTopicId(), DifficultyLevel.EASY, excludeIds, PageRequest.of(0, easy));
				result.addAll(questions);
			}
			if (medium > 0) {
				List<Question> questions = questionRepository.findRandomByTopicAndDifficulty(
						tc.getTopicId(), DifficultyLevel.MEDIUM, excludeIds, PageRequest.of(0, medium));
				result.addAll(questions);
			}
			if (hard > 0) {
				List<Question> questions = questionRepository.findRandomByTopicAndDifficulty(
						tc.getTopicId(), DifficultyLevel.HARD, excludeIds, PageRequest.of(0, hard));
				result.addAll(questions);
			}
		}
		return result;
	}

	// ========================================================================================
	// ==============================  EXAM HISTORY  ==========================================
	// ========================================================================================

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResponse<UserExamHistoryResponse>> getMyExamHistory(Integer subjectId, Pageable pageable) {
		User currentUser = getCurrentUser();
		if (currentUser == null) {
			return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập!");
		}

		Page<Integer> examIdsPage = attemptRepository.findDistinctExamIdsByUserId(
				currentUser.getId(), COMPLETED_STATUSES, subjectId, pageable);

		Page<UserExamHistoryResponse> result = examIdsPage.map(examId -> {
			Exam exam = examRepository.findById(examId).orElse(null);
			if (exam == null) return null;

			long attemptCount = attemptRepository.countCompletedAttempts(currentUser.getId(), examId, COMPLETED_STATUSES);
			Double bestScore = attemptRepository.findBestScore(currentUser.getId(), examId, COMPLETED_STATUSES);
			List<Attempt> latestAttempts = attemptRepository.findLatestAttempts(
					currentUser.getId(), examId, COMPLETED_STATUSES, PageRequest.of(0, 1));
			Attempt latest = latestAttempts.isEmpty() ? null : latestAttempts.get(0);

			Integer maxAttempts = exam.getMaxAttempts();
			boolean canRetake = (maxAttempts == null || maxAttempts <= 0 || attemptCount < maxAttempts)
					&& Boolean.TRUE.equals(exam.getIsActive());

			Subject sub = exam.getSubject();
			return new UserExamHistoryResponse(
					examId, exam.getTitle(),
					sub != null ? sub.getId() : null,
					sub != null ? sub.getName() : null,
					(int) attemptCount, maxAttempts, canRetake,
					bestScore,
					latest != null ? latest.getScore() : null,
					latest != null ? latest.getSubmittedAt() : null,
					exam.getType()
			);
		});

		// Filter null entries
		List<UserExamHistoryResponse> filtered = result.getContent().stream()
				.filter(java.util.Objects::nonNull).toList();

		return new ApiResponse<>(HttpStatus.OK.value(), "Lấy lịch sử thi thành công!", PageResponse.from(result));
	}

	// ========================================================================================
	// ===========================  PERFORMANCE EVALUATION  ==================================
	// ========================================================================================

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<ExamPerformanceResponse> getAttemptPerformance(Integer attemptId) {
		User currentUser = getCurrentUser();
		if (currentUser == null) {
			return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập!");
		}

		Attempt attempt = attemptRepository.findByIdAndUser_Id(attemptId, currentUser.getId()).orElse(null);
		if (attempt == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy lượt làm bài!");
		}

		if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
			return new ApiResponse<>(HttpStatus.OK.value(), "GRADING", null);
		}

		List<Answer> answers = answerRepository.findByAttemptIdWithDetails(attemptId);
		Exam exam = attempt.getExam();
		int totalQuestions = attempt.getTotalQuestions() != null ? attempt.getTotalQuestions() : answers.size();
		int correctCount = attempt.getCorrectCount() != null ? attempt.getCorrectCount() : 0;
		int wrongCount = attempt.getWrongCount() != null ? attempt.getWrongCount() : 0;
		int unanswered = totalQuestions - correctCount - wrongCount;
		double percentage = totalQuestions > 0 ? (double) correctCount / totalQuestions * 100 : 0;

		// --- Topic Analysis ---
		Map<Integer, List<Answer>> byTopic = new LinkedHashMap<>();
		Map<Integer, String> topicNames = new HashMap<>();
		for (Answer a : answers) {
			Question q = a.getQuestion();
			Integer topicId = (q != null && q.getTopic() != null) ? q.getTopic().getId() : -1;
			String topicName = (q != null && q.getTopic() != null) ? q.getTopic().getName() : "Không xác định";
			byTopic.computeIfAbsent(topicId, k -> new ArrayList<>()).add(a);
			topicNames.putIfAbsent(topicId, topicName);
		}

		List<ExamPerformanceResponse.TopicAnalysis> topicAnalyses = new ArrayList<>();
		for (Map.Entry<Integer, List<Answer>> entry : byTopic.entrySet()) {
			List<Answer> topicAnswers = entry.getValue();
			int tCorrect = (int) topicAnswers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
			int tTotal = topicAnswers.size();
			double tPct = tTotal > 0 ? (double) tCorrect / tTotal * 100 : 0;
			topicAnalyses.add(new ExamPerformanceResponse.TopicAnalysis(
					entry.getKey() == -1 ? null : entry.getKey(),
					topicNames.get(entry.getKey()),
					tTotal, tCorrect, Math.round(tPct * 10.0) / 10.0,
					tPct >= 75 ? "STRONG" : tPct >= 50 ? "AVERAGE" : "WEAK"
			));
		}

		// --- Difficulty Analysis ---
		Map<String, List<Answer>> byDifficulty = new LinkedHashMap<>();
		for (Answer a : answers) {
			Question q = a.getQuestion();
			String diff = (q != null && q.getDifficulty() != null) ? q.getDifficulty().name() : "UNKNOWN";
			byDifficulty.computeIfAbsent(diff, k -> new ArrayList<>()).add(a);
		}

		List<ExamPerformanceResponse.DifficultyAnalysis> diffAnalyses = new ArrayList<>();
		for (Map.Entry<String, List<Answer>> entry : byDifficulty.entrySet()) {
			List<Answer> diffAnswers = entry.getValue();
			int dCorrect = (int) diffAnswers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
			int dTotal = diffAnswers.size();
			double dPct = dTotal > 0 ? (double) dCorrect / dTotal * 100 : 0;
			diffAnalyses.add(new ExamPerformanceResponse.DifficultyAnalysis(
					entry.getKey(), dTotal, dCorrect, Math.round(dPct * 10.0) / 10.0,
					dPct >= 75 ? "STRONG" : dPct >= 50 ? "AVERAGE" : "WEAK"
			));
		}

		// --- Weaknesses ---
		List<ExamPerformanceResponse.WeaknessItem> weaknesses = new ArrayList<>();
		for (ExamPerformanceResponse.TopicAnalysis ta : topicAnalyses) {
			if (ta.getPercentage() < 50) {
				weaknesses.add(new ExamPerformanceResponse.WeaknessItem(
						ta.getTopicName(), "TOPIC", ta.getPercentage(),
						"Bạn chỉ đúng " + ta.getCorrectCount() + "/" + ta.getTotalQuestions() + " câu ở chủ đề " + ta.getTopicName()
				));
			}
		}
		for (ExamPerformanceResponse.DifficultyAnalysis da : diffAnalyses) {
			if (da.getPercentage() < 50) {
				String diffLabel = switch (da.getDifficulty()) {
					case "EASY" -> "Dễ";
					case "MEDIUM" -> "Trung bình";
					case "HARD" -> "Khó";
					default -> da.getDifficulty();
				};
				weaknesses.add(new ExamPerformanceResponse.WeaknessItem(
						diffLabel, "DIFFICULTY", da.getPercentage(),
						"Bạn chỉ đúng " + da.getCorrectCount() + "/" + da.getTotalQuestions() + " câu mức độ " + diffLabel
				));
			}
		}

		// --- Recommendations ---
		List<String> recommendations = new ArrayList<>();
		for (ExamPerformanceResponse.WeaknessItem w : weaknesses) {
			if ("TOPIC".equals(w.getType())) {
				recommendations.add("Bạn cần ôn tập thêm chủ đề: " + w.getArea());
			}
		}
		ExamPerformanceResponse.DifficultyAnalysis easyAnalysis = diffAnalyses.stream()
				.filter(d -> "EASY".equals(d.getDifficulty())).findFirst().orElse(null);
		ExamPerformanceResponse.DifficultyAnalysis hardAnalysis = diffAnalyses.stream()
				.filter(d -> "HARD".equals(d.getDifficulty())).findFirst().orElse(null);
		if (easyAnalysis != null && easyAnalysis.getPercentage() < 60) {
			recommendations.add("Bạn cần củng cố kiến thức cơ bản - câu dễ vẫn sai nhiều.");
		}
		if (hardAnalysis != null && hardAnalysis.getPercentage() < 30) {
			recommendations.add("Bạn cần luyện thêm các câu hỏi nâng cao mức khó.");
		}
		if (exam != null && exam.getDuration() != null && attempt.getDurationTaken() != null) {
			double timeUsedPct = (double) attempt.getDurationTaken() / (exam.getDuration() * 60) * 100;
			if (timeUsedPct > 90) {
				recommendations.add("Bạn gần hết thời gian, cần cải thiện tốc độ làm bài.");
			}
		}
		if (percentage >= 90) {
			recommendations.add("Xuất sắc! Hãy thử thách bản thân với đề khó hơn.");
		} else if (percentage < 40) {
			recommendations.add("Điểm còn thấp, hãy ôn tập lại toàn bộ kiến thức trước khi thi lại.");
		}

		// --- Progress Comparison ---
		ExamPerformanceResponse.ProgressComparison progress = null;
		if (exam != null) {
			List<Attempt> prevAttempts = attemptRepository.findByUser_IdAndExam_IdAndStatusInOrderBySubmittedAtDesc(
					currentUser.getId(), exam.getId(), COMPLETED_STATUSES);
			int attemptNumber = prevAttempts.size();
			if (prevAttempts.size() >= 2) {
				// Current is first in list (most recent), previous is second
				Attempt previous = prevAttempts.get(1);
				Double prevScore = previous.getScore();
				Double currScore = attempt.getScore();
				double improvement = (currScore != null && prevScore != null && prevScore > 0)
						? ((currScore - prevScore) / prevScore * 100) : 0;
				String trend = improvement > 5 ? "IMPROVING" : improvement < -5 ? "DECLINING" : "STABLE";
				progress = new ExamPerformanceResponse.ProgressComparison(
						prevScore, currScore, Math.round(improvement * 10.0) / 10.0, trend, attemptNumber
				);
			} else if (attemptNumber == 1) {
				progress = new ExamPerformanceResponse.ProgressComparison(
						null, attempt.getScore(), null, "FIRST_ATTEMPT", 1
				);
			}
		}

		String overallRating = percentage >= 90 ? "EXCELLENT"
				: percentage >= 75 ? "GOOD"
				: percentage >= 60 ? "AVERAGE"
				: percentage >= 40 ? "WEAK" : "VERY_WEAK";

		ExamPerformanceResponse response = new ExamPerformanceResponse(
				attemptId,
				exam != null ? exam.getId() : null,
				exam != null ? exam.getTitle() : null,
				attempt.getScore(),
				Math.round(percentage * 10.0) / 10.0,
				overallRating,
				correctCount, wrongCount, unanswered, totalQuestions,
				attempt.getDurationTaken(),
				topicAnalyses, diffAnalyses,
				weaknesses, recommendations, progress
		);

		return new ApiResponse<>(HttpStatus.OK.value(), "Đánh giá hiệu suất thành công!", response);
	}

	// ========================================================================================
	// ============================  RETAKE ELIGIBILITY  =====================================
	// ========================================================================================

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<Map<String, Object>> checkRetakeEligibility(Integer examId) {
		User currentUser = getCurrentUser();
		if (currentUser == null) {
			return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập!");
		}

		Exam exam = examRepository.findByIdAndDeletedAtIsNull(examId).orElse(null);
		if (exam == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy đề thi!");
		}

		long attemptCount = attemptRepository.countByUser_IdAndExam_Id(currentUser.getId(), examId);
		Integer maxAttempts = exam.getMaxAttempts();
		boolean isActive = Boolean.TRUE.equals(exam.getIsActive());
		boolean withinTime = true;
		LocalDateTime now = LocalDateTime.now();
		if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) withinTime = false;
		if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) withinTime = false;

		boolean canRetake;
		String reason;
		if (!isActive) {
			canRetake = false;
			reason = "Đề thi hiện không được mở.";
		} else if (!withinTime) {
			canRetake = false;
			reason = "Đề thi ngoài khung thời gian cho phép.";
		} else if (maxAttempts != null && maxAttempts > 0 && attemptCount >= maxAttempts) {
			canRetake = false;
			reason = "Bạn đã dùng hết " + maxAttempts + " lượt làm bài.";
		} else {
			canRetake = true;
			reason = maxAttempts != null && maxAttempts > 0
					? "Bạn còn " + (maxAttempts - attemptCount) + " lượt làm bài."
					: "Không giới hạn số lần làm bài.";
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("canRetake", canRetake);
		data.put("reason", reason);
		data.put("attemptCount", (int) attemptCount);
		data.put("maxAttempts", maxAttempts);
		data.put("examActive", isActive);

		return new ApiResponse<>(HttpStatus.OK.value(), "Kiểm tra quyền làm lại thành công!", data);
	}

	// ========================================================================================
	// ================================  HELPERS  =============================================
	// ========================================================================================

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
