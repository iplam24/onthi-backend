package com.onthi.v_edu.question.service;

import com.onthi.v_edu.common.constant.ContentFormat;
import com.onthi.v_edu.common.constant.QuestionType;
import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.dto.PageResponse;
import com.onthi.v_edu.config.security.services.UserDetailsImpl;
import com.onthi.v_edu.learning.entity.Topic;
import com.onthi.v_edu.learning.repository.TopicRepository;
import com.onthi.v_edu.question.dto.OptionRequest;
import com.onthi.v_edu.question.dto.OptionResponse;
import com.onthi.v_edu.question.dto.QuestionRequest;
import com.onthi.v_edu.question.dto.QuestionResponse;
import com.onthi.v_edu.question.entity.EssayAnswer;
import com.onthi.v_edu.question.entity.Explanation;
import com.onthi.v_edu.question.entity.Question;
import com.onthi.v_edu.question.entity.QuestionOption;
import com.onthi.v_edu.question.repository.EssayAnswerRepository;
import com.onthi.v_edu.question.repository.ExplanationRepository;
import com.onthi.v_edu.question.repository.QuestionOptionRepository;
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
import java.util.List;

@Service
@Transactional
public class QuestionServiceImpl implements QuestionService {

	private final QuestionRepository questionRepository;
	private final QuestionOptionRepository questionOptionRepository;
	private final EssayAnswerRepository essayAnswerRepository;
	private final ExplanationRepository explanationRepository;
	private final TopicRepository topicRepository;
	private final UserRepository userRepository;

	public QuestionServiceImpl(QuestionRepository questionRepository,
						   QuestionOptionRepository questionOptionRepository,
						   EssayAnswerRepository essayAnswerRepository,
						   ExplanationRepository explanationRepository,
						   TopicRepository topicRepository,
						   UserRepository userRepository) {
		this.questionRepository = questionRepository;
		this.questionOptionRepository = questionOptionRepository;
		this.essayAnswerRepository = essayAnswerRepository;
		this.explanationRepository = explanationRepository;
		this.topicRepository = topicRepository;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResponse<QuestionResponse>> getAllQuestions(Integer subjectId, Integer topicId, Pageable pageable) {
		Page<Question> questionPage;
		if (subjectId != null && topicId != null) {
			questionPage = questionRepository.findByTopic_IdAndTopic_Subject_IdAndDeletedAtIsNull(topicId, subjectId, pageable);
		} else if (subjectId != null) {
			questionPage = questionRepository.findByTopic_Subject_IdAndDeletedAtIsNull(subjectId, pageable);
		} else if (topicId != null) {
			questionPage = questionRepository.findByTopic_IdAndDeletedAtIsNull(topicId, pageable);
		} else {
			questionPage = questionRepository.findByDeletedAtIsNull(pageable);
		}

		Page<QuestionResponse> data = questionPage
				.map(this::toQuestionResponse);
		return new ApiResponse<>(HttpStatus.OK.value(), "Lấy danh sách câu hỏi thành công!", PageResponse.from(data));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<QuestionResponse> getQuestionById(Integer id) {
		return questionRepository.findByIdAndDeletedAtIsNull(id)
				.map(question -> new ApiResponse<>(HttpStatus.OK.value(), "Lấy câu hỏi thành công!", toQuestionResponse(question)))
				.orElseGet(() -> new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy câu hỏi!"));
	}

	@Override
	public ApiResponse<QuestionResponse> createQuestion(QuestionRequest request) {
		Topic topic = topicRepository.findById(request.getTopicId()).orElse(null);
		if (topic == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy topic!");
		}

		String validationError = validateTypePayload(request);
		if (validationError != null) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), validationError);
		}

		User currentUser = getCurrentUser();
		if (currentUser == null) {
			return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Bạn cần đăng nhập để thực hiện thao tác này!");
		}

		Question question = new Question();
		question.setContent(normalize(request.getContent()));
		question.setContentFormat(resolveContentFormat(request.getContentFormat()));
		question.setUrl(normalize(request.getUrl()));
		question.setType(request.getType());
		question.setDifficulty(request.getDifficulty());
		question.setTopic(topic);
		question.setCreatedBy(currentUser);
		question.setCreatedAt(LocalDateTime.now());
		question = questionRepository.save(question);

		syncQuestionDetails(question, request);
		return new ApiResponse<>(HttpStatus.CREATED.value(), "Tạo câu hỏi thành công!", toQuestionResponse(question));
	}

	@Override
	public ApiResponse<QuestionResponse> updateQuestion(Integer id, QuestionRequest request) {
		Question question = questionRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
		if (question == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy câu hỏi!");
		}

		Topic topic = topicRepository.findById(request.getTopicId()).orElse(null);
		if (topic == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy topic!");
		}

		String validationError = validateTypePayload(request);
		if (validationError != null) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), validationError);
		}

		question.setContent(normalize(request.getContent()));
		question.setContentFormat(resolveContentFormat(request.getContentFormat()));
		question.setUrl(normalize(request.getUrl()));
		question.setType(request.getType());
		question.setDifficulty(request.getDifficulty());
		question.setTopic(topic);
		question = questionRepository.save(question);

		syncQuestionDetails(question, request);
		return new ApiResponse<>(HttpStatus.OK.value(), "Cập nhật câu hỏi thành công!", toQuestionResponse(question));
	}

	@Override
	public ApiResponse<Void> deleteQuestion(Integer id) {
		Question question = questionRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
		if (question == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy câu hỏi!");
		}

		explanationRepository.softDeleteByQuestionId(id);
		essayAnswerRepository.softDeleteByQuestionId(id);
		questionOptionRepository.softDeleteByQuestionId(id);
		questionRepository.delete(question);

		return new ApiResponse<>(HttpStatus.OK.value(), "Xoá câu hỏi thành công!");
	}

	@Override
	public ApiResponse<Void> createQuestions(List<QuestionRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Danh sách câu hỏi trống!");
		}

		for (QuestionRequest request : requests) {
			ApiResponse<QuestionResponse> response = createQuestion(request);
			if (response.getStatus() != HttpStatus.CREATED.value()) {
				throw new RuntimeException("Lỗi khi lưu câu hỏi: " + response.getMessage());
			}
		}

		return new ApiResponse<>(HttpStatus.OK.value(), "Lưu " + requests.size() + " câu hỏi thành công!");
	}

	private void syncQuestionDetails(Question question, QuestionRequest request) {
		Integer questionId = question.getId();
		questionOptionRepository.softDeleteByQuestionId(questionId);
		essayAnswerRepository.softDeleteByQuestionId(questionId);

		if (request.getType() == QuestionType.MCQ) {
			List<OptionRequest> optionRequests = request.getOptions() == null ? Collections.emptyList() : request.getOptions();
			List<QuestionOption> options = optionRequests.stream()
					.map(optionRequest -> {
						QuestionOption option = new QuestionOption();
						option.setQuestion(question);
						option.setContent(normalize(optionRequest.getContent()));
						option.setIsCorrect(optionRequest.getIsCorrect());
						return option;
					})
					.toList();
			questionOptionRepository.saveAll(options);
		} else {
			EssayAnswer essayAnswer = new EssayAnswer();
			essayAnswer.setQuestion(question);
			essayAnswer.setSampleAnswer(normalize(request.getSampleAnswer()));
			essayAnswerRepository.save(essayAnswer);
		}

		String explanationContent = normalize(request.getExplanation());
		if (isBlank(explanationContent)) {
			explanationRepository.softDeleteByQuestionId(questionId);
			return;
		}

		Explanation explanation = explanationRepository.findByQuestionIdAndDeletedAtIsNull(questionId).orElse(null);
		if (explanation == null) {
			explanation = new Explanation();
			explanation.setQuestion(question);
			explanation.setCreatedAt(LocalDateTime.now());
		}
		explanation.setContent(explanationContent);
		explanationRepository.save(explanation);
	}

	private String validateTypePayload(QuestionRequest request) {
		if (request.getType() == QuestionType.MCQ) {
			List<OptionRequest> options = request.getOptions() == null ? Collections.emptyList() : request.getOptions();
			if (options.size() < 2) {
				return "Câu hỏi MCQ phải có ít nhất 2 đáp án!";
			}

			long correctCount = options.stream()
					.filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
					.count();
			if (correctCount == 0) {
				return "Câu hỏi MCQ phải có ít nhất 1 đáp án đúng!";
			}
			return null;
		}

		if (isBlank(request.getSampleAnswer())) {
			return "Câu hỏi ESSAY phải có đáp án mẫu!";
		}
		return null;
	}

	private QuestionResponse toQuestionResponse(Question question) {
		Integer questionId = question.getId();
		List<OptionResponse> options = questionOptionRepository.findByQuestion_IdAndDeletedAtIsNullOrderByIdAsc(questionId).stream()
				.map(option -> new OptionResponse(option.getId(), option.getContent(), option.getIsCorrect()))
				.toList();

		EssayAnswer essayAnswer = essayAnswerRepository.findByQuestion_IdAndDeletedAtIsNull(questionId).orElse(null);
		Explanation explanation = explanationRepository.findByQuestionIdAndDeletedAtIsNull(questionId).orElse(null);

		Topic topic = question.getTopic();
		User creator = question.getCreatedBy();
		Integer topicId = topic != null ? topic.getId() : null;
		String topicName = topic != null ? topic.getName() : null;
		Integer createdById = creator != null ? creator.getId() : null;
		String createdByUsername = creator != null ? creator.getUsername() : null;

		return new QuestionResponse(
				questionId,
				question.getContent(),
				question.getContentFormat(),
				question.getUrl(),
				question.getType(),
				question.getDifficulty(),
				topicId,
				topicName,
				createdById,
				createdByUsername,
				question.getCreatedAt(),
				options,
				essayAnswer != null ? essayAnswer.getSampleAnswer() : null,
				explanation != null ? explanation.getContent() : null,
				explanation != null ? explanation.getCreatedAt() : null
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

	private ContentFormat resolveContentFormat(ContentFormat contentFormat) {
		return contentFormat == null ? ContentFormat.PLAIN_TEXT : contentFormat;
	}
}
