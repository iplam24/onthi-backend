package com.onthi.v_edu.learning.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.exam.repository.ExamRepository;
import com.onthi.v_edu.learning.entity.Level;
import com.onthi.v_edu.learning.entity.Subject;
import com.onthi.v_edu.learning.entity.Topic;
import com.onthi.v_edu.learning.repository.LevelRepository;
import com.onthi.v_edu.learning.repository.SubjectRepository;
import com.onthi.v_edu.learning.repository.TopicRepository;
import com.onthi.v_edu.progress.repository.ProgressRepository;
import com.onthi.v_edu.question.repository.QuestionRepository;
import com.onthi.v_edu.user.repository.UserInformationRepository;
import com.onthi.v_edu.userquestion.repository.UserQuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LearningServiceImpl implements LearningService {

	private final LevelRepository levelRepository;
	private final SubjectRepository subjectRepository;
	private final TopicRepository topicRepository;
	private final UserInformationRepository userInformationRepository;
	private final ExamRepository examRepository;
	private final QuestionRepository questionRepository;
	private final ProgressRepository progressRepository;
	private final UserQuestionRepository userQuestionRepository;

	public LearningServiceImpl(LevelRepository levelRepository,
							   SubjectRepository subjectRepository,
							   TopicRepository topicRepository,
							   UserInformationRepository userInformationRepository,
							   ExamRepository examRepository,
							   QuestionRepository questionRepository,
							   ProgressRepository progressRepository,
							   UserQuestionRepository userQuestionRepository) {
		this.levelRepository = levelRepository;
		this.subjectRepository = subjectRepository;
		this.topicRepository = topicRepository;
		this.userInformationRepository = userInformationRepository;
		this.examRepository = examRepository;
		this.questionRepository = questionRepository;
		this.progressRepository = progressRepository;
		this.userQuestionRepository = userQuestionRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<LevelResponse>> getAllLevels() {
		List<LevelResponse> data = levelRepository.findAll().stream()
				.map(this::toLevelResponse)
				.toList();
		return new ApiResponse<>(HttpStatus.OK.value(), "Lấy danh sách level thành công!", data);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<LevelResponse> getLevelById(Integer id) {
		return levelRepository.findById(id)
				.map(level -> new ApiResponse<>(HttpStatus.OK.value(), "Lấy level thành công!", toLevelResponse(level)))
				.orElseGet(() -> new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy level!"));
	}

	@Override
	public ApiResponse<LevelResponse> createLevel(LevelRequest request) {
		String name = normalize(request.name());
		if (levelRepository.existsByName(name)) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Tên level đã tồn tại!");
		}

		Level level = new Level();
		level.setName(name);
		level = levelRepository.save(level);

		return new ApiResponse<>(HttpStatus.CREATED.value(), "Tạo level thành công!", toLevelResponse(level));
	}

	@Override
	public ApiResponse<LevelResponse> updateLevel(Integer id, LevelRequest request) {
		Level level = levelRepository.findById(id)
				.orElse(null);
		if (level == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy level!");
		}

		String name = normalize(request.name());
		if (levelRepository.existsByNameAndIdNot(name, id)) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Tên level đã tồn tại!");
		}

		level.setName(name);
		level = levelRepository.save(level);
		return new ApiResponse<>(HttpStatus.OK.value(), "Cập nhật level thành công!", toLevelResponse(level));
	}

	@Override
	public ApiResponse<Void> deleteLevel(Integer id) {
		Level level = levelRepository.findById(id)
				.orElse(null);
		if (level == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy level!");
		}

		long inUseByUserInformation = userInformationRepository.countByLevel_Id(id);
		long inUseBySubjects = subjectRepository.countByLevel_Id(id);
		if (inUseByUserInformation > 0 || inUseBySubjects > 0) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Không thể xoá level vì đang được sử dụng!");
		}

		levelRepository.delete(level);
		return new ApiResponse<>(HttpStatus.OK.value(), "Xoá level thành công!");
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<SubjectResponse>> getAllSubjects() {
		List<SubjectResponse> data = subjectRepository.findAll().stream()
				.map(this::toSubjectResponse)
				.toList();
		return new ApiResponse<>(HttpStatus.OK.value(), "Lấy danh sách subject thành công!", data);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<SubjectResponse> getSubjectById(Integer id) {
		return subjectRepository.findById(id)
				.map(subject -> new ApiResponse<>(HttpStatus.OK.value(), "Lấy subject thành công!", toSubjectResponse(subject)))
				.orElseGet(() -> new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy subject!"));
	}

	@Override
	public ApiResponse<SubjectResponse> createSubject(SubjectRequest request) {
		Level level = levelRepository.findById(request.levelId())
				.orElse(null);
		if (level == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy level!");
		}

		String name = normalize(request.name());
		if (subjectRepository.existsByNameAndLevel_Id(name, level.getId())) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Subject này đã tồn tại trong level đã chọn!");
		}

		Subject subject = new Subject();
		subject.setName(name);
		subject.setImageUrl(request.imageUrl());
		subject.setLevel(level);
		subject = subjectRepository.save(subject);

		return new ApiResponse<>(HttpStatus.CREATED.value(), "Tạo subject thành công!", toSubjectResponse(subject));
	}

	@Override
	public ApiResponse<SubjectResponse> updateSubject(Integer id, SubjectRequest request) {
		Subject subject = subjectRepository.findById(id)
				.orElse(null);
		if (subject == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy subject!");
		}

		Level level = levelRepository.findById(request.levelId())
				.orElse(null);
		if (level == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy level!");
		}

		String name = normalize(request.name());
		if (subjectRepository.existsByNameAndLevel_IdAndIdNot(name, level.getId(), id)) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Subject này đã tồn tại trong level đã chọn!");
		}

		subject.setName(name);
		subject.setImageUrl(request.imageUrl());
		subject.setLevel(level);
		subject = subjectRepository.save(subject);

		return new ApiResponse<>(HttpStatus.OK.value(), "Cập nhật subject thành công!", toSubjectResponse(subject));
	}

	@Override
	public ApiResponse<Void> deleteSubject(Integer id) {
		Subject subject = subjectRepository.findById(id)
				.orElse(null);
		if (subject == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy subject!");
		}

		long inUseByExams = examRepository.countBySubject_Id(id);
		long inUseByTopics = topicRepository.countBySubject_Id(id);
		if (inUseByExams > 0 || inUseByTopics > 0) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Không thể xoá subject vì đang được sử dụng!");
		}

		subjectRepository.delete(subject);
		return new ApiResponse<>(HttpStatus.OK.value(), "Xoá subject thành công!");
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<TopicResponse>> getAllTopics() {
		List<TopicResponse> data = topicRepository.findAll().stream()
				.map(this::toTopicResponse)
				.toList();
		return new ApiResponse<>(HttpStatus.OK.value(), "Lấy danh sách topic thành công!", data);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<TopicResponse> getTopicById(Integer id) {
		return topicRepository.findById(id)
				.map(topic -> new ApiResponse<>(HttpStatus.OK.value(), "Lấy topic thành công!", toTopicResponse(topic)))
				.orElseGet(() -> new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy topic!"));
	}

	@Override
	public ApiResponse<TopicResponse> createTopic(TopicRequest request) {
		Subject subject = subjectRepository.findById(request.subjectId())
				.orElse(null);
		if (subject == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy subject!");
		}

		String name = normalize(request.name());
		if (topicRepository.existsByNameAndSubject_Id(name, subject.getId())) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Topic này đã tồn tại trong subject đã chọn!");
		}

		Topic topic = new Topic();
		topic.setName(name);
		topic.setSubject(subject);
		topic = topicRepository.save(topic);

		return new ApiResponse<>(HttpStatus.CREATED.value(), "Tạo topic thành công!", toTopicResponse(topic));
	}

	@Override
	public ApiResponse<TopicResponse> updateTopic(Integer id, TopicRequest request) {
		Topic topic = topicRepository.findById(id)
				.orElse(null);
		if (topic == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy topic!");
		}

		Subject subject = subjectRepository.findById(request.subjectId())
				.orElse(null);
		if (subject == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy subject!");
		}

		String name = normalize(request.name());
		if (topicRepository.existsByNameAndSubject_IdAndIdNot(name, subject.getId(), id)) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Topic này đã tồn tại trong subject đã chọn!");
		}

		topic.setName(name);
		topic.setSubject(subject);
		topic = topicRepository.save(topic);

		return new ApiResponse<>(HttpStatus.OK.value(), "Cập nhật topic thành công!", toTopicResponse(topic));
	}

	@Override
	public ApiResponse<Void> deleteTopic(Integer id) {
		Topic topic = topicRepository.findById(id)
				.orElse(null);
		if (topic == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy topic!");
		}

		long inUseByQuestions = questionRepository.countByTopic_Id(id);
		long inUseByProgress = progressRepository.countByTopic_Id(id);
		long inUseByUserQuestions = userQuestionRepository.countByTopic_Id(id);
		if (inUseByQuestions > 0 || inUseByProgress > 0 || inUseByUserQuestions > 0) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Không thể xoá topic vì đang được sử dụng!");
		}

		topicRepository.delete(topic);
		return new ApiResponse<>(HttpStatus.OK.value(), "Xoá topic thành công!");
	}

	private String normalize(String value) {
		return value == null ? null : value.trim();
	}

	private LevelResponse toLevelResponse(Level level) {
		return new LevelResponse(level.getId(), level.getName());
	}

	private SubjectResponse toSubjectResponse(Subject subject) {
		Level level = subject.getLevel();
		Integer levelId = level != null ? level.getId() : null;
		String levelName = level != null ? level.getName() : null;
		return new SubjectResponse(subject.getId(), subject.getName(), subject.getImageUrl(), levelId, levelName);
	}

	private TopicResponse toTopicResponse(Topic topic) {
		Subject subject = topic.getSubject();
		Level level = subject != null ? subject.getLevel() : null;
		Integer subjectId = subject != null ? subject.getId() : null;
		String subjectName = subject != null ? subject.getName() : null;
		Integer levelId = level != null ? level.getId() : null;
		String levelName = level != null ? level.getName() : null;
		return new TopicResponse(topic.getId(), topic.getName(), subjectId, subjectName, levelId, levelName);
	}
}
