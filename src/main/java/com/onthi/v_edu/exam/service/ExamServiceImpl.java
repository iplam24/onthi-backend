package com.onthi.v_edu.exam.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.dto.PageResponse;
import com.onthi.v_edu.config.security.services.UserDetailsImpl;
import com.onthi.v_edu.exam.dto.ExamQuestionItemRequest;
import com.onthi.v_edu.exam.dto.ExamQuestionItemResponse;
import com.onthi.v_edu.exam.dto.ExamRequest;
import com.onthi.v_edu.exam.dto.ExamResponse;
import com.onthi.v_edu.exam.entity.Exam;
import com.onthi.v_edu.exam.entity.ExamQuestion;
import com.onthi.v_edu.exam.entity.ExamQuestionId;
import com.onthi.v_edu.exam.repository.ExamQuestionRepository;
import com.onthi.v_edu.exam.repository.ExamRepository;
import com.onthi.v_edu.learning.entity.Subject;
import com.onthi.v_edu.learning.repository.SubjectRepository;
import com.onthi.v_edu.question.entity.Question;
import com.onthi.v_edu.question.repository.QuestionRepository;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ExamServiceImpl implements ExamService {

	private final ExamRepository examRepository;
	private final ExamQuestionRepository examQuestionRepository;
	private final SubjectRepository subjectRepository;
	private final QuestionRepository questionRepository;
	private final UserRepository userRepository;

	public ExamServiceImpl(ExamRepository examRepository,
						   ExamQuestionRepository examQuestionRepository,
						   SubjectRepository subjectRepository,
						   QuestionRepository questionRepository,
						   UserRepository userRepository) {
		this.examRepository = examRepository;
		this.examQuestionRepository = examQuestionRepository;
		this.subjectRepository = subjectRepository;
		this.questionRepository = questionRepository;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResponse<ExamResponse>> getAllExams(Pageable pageable) {
		Page<ExamResponse> data = examRepository.findAll(pageable)
				.map(this::toExamResponse);
		return new ApiResponse<>(HttpStatus.OK.value(), "Lấy danh sách đề thi thành công!", PageResponse.from(data));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResponse<ExamResponse>> getExamsBySubjectId(Integer subjectId, Pageable pageable) {
		if (!subjectRepository.existsById(subjectId)) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy subject!");
		}

		Page<ExamResponse> data = examRepository.findBySubject_Id(subjectId, pageable)
				.map(this::toExamResponse);
		return new ApiResponse<>(HttpStatus.OK.value(), "Lấy danh sách đề thi theo môn học thành công!", PageResponse.from(data));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<ExamResponse> getExamById(Integer id) {
		return examRepository.findById(id)
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
		Exam exam = examRepository.findById(id).orElse(null);
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

		examQuestionRepository.deleteByExam_Id(id);
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
			Question question = questionRepository.findById(questionId).orElse(null);
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
		exam.setShuffleQuestions(request.getShuffleQuestions() != null ? request.getShuffleQuestions() : Boolean.FALSE);
		exam.setShuffleAnswers(request.getShuffleAnswers() != null ? request.getShuffleAnswers() : Boolean.FALSE);
		exam.setMaxAttempts(request.getMaxAttempts());
	}

	private void syncExamQuestions(Exam exam, List<ExamQuestionItemRequest> items) {
		Integer examId = exam.getId();
		examQuestionRepository.deleteByExam_Id(examId);
		if (items == null || items.isEmpty()) {
			return;
		}

		List<ExamQuestion> examQuestions = items.stream()
				.map(item -> buildExamQuestion(exam, item))
				.toList();
		examQuestionRepository.saveAll(examQuestions);
	}

	private ExamQuestion buildExamQuestion(Exam exam, ExamQuestionItemRequest item) {
		Question question = questionRepository.findById(item.getQuestionId()).orElseThrow();

		ExamQuestion examQuestion = new ExamQuestion();
		examQuestion.setId(new ExamQuestionId(exam.getId(), question.getId()));
		examQuestion.setExam(exam);
		examQuestion.setQuestion(question);
		examQuestion.setOrderIndex(item.getOrderIndex());
		examQuestion.setScore(item.getScore());
		examQuestion.setContentSnapshot(normalize(item.getContentSnapshot()));
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
				.findByExam_IdOrderByOrderIndexAscQuestion_IdAsc(exam.getId())
				.stream()
				.map(item -> new ExamQuestionItemResponse(
						item.getQuestion() != null ? item.getQuestion().getId() : null,
						item.getQuestion() != null ? item.getQuestion().getContent() : null,
						item.getOrderIndex(),
						item.getScore(),
						item.getContentSnapshot()
				))
				.toList();

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
				exam.getShuffleQuestions(),
				exam.getShuffleAnswers(),
				exam.getMaxAttempts(),
				exam.getCreatedAt(),
				exam.getUpdatedAt(),
				questionItems
		);
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
