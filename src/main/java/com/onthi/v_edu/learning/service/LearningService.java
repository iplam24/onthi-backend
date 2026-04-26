package com.onthi.v_edu.learning.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
// import org.hibernate.validator.constraints.URL; // Temporarily removed

import java.util.List;

public interface LearningService {

	record LevelRequest(@NotBlank String name) {}

	record LevelResponse(Integer id, String name) {}

	record SubjectRequest(@NotBlank String name, /*@URL*/ String imageUrl, @NotNull Integer levelId) {}

	record SubjectResponse(Integer id, String name, String imageUrl, Integer levelId, String levelName) {}

	record TopicRequest(@NotBlank String name, @NotNull Integer subjectId) {}

	record TopicResponse(Integer id, String name, Integer subjectId, String subjectName, Integer levelId, String levelName) {}

	ApiResponse<List<LevelResponse>> getAllLevels();

	ApiResponse<LevelResponse> getLevelById(Integer id);

	ApiResponse<LevelResponse> createLevel(LevelRequest request);

	ApiResponse<LevelResponse> updateLevel(Integer id, LevelRequest request);

	ApiResponse<Void> deleteLevel(Integer id);

	ApiResponse<List<SubjectResponse>> getAllSubjects();

	ApiResponse<SubjectResponse> getSubjectById(Integer id);

	ApiResponse<SubjectResponse> createSubject(SubjectRequest request);

	ApiResponse<SubjectResponse> updateSubject(Integer id, SubjectRequest request);

	ApiResponse<Void> deleteSubject(Integer id);



	ApiResponse<List<TopicResponse>> getAllTopics();

	ApiResponse<TopicResponse> getTopicById(Integer id);

	ApiResponse<TopicResponse> createTopic(TopicRequest request);

	ApiResponse<TopicResponse> updateTopic(Integer id, TopicRequest request);



	ApiResponse<Void> deleteTopic(Integer id);
}
