import re

with open('E:/projectOnthi/onthi-backend/src/main/java/com/onthi/v_edu/exam/service/ExamServiceImpl.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace validateRequest
old_validate = '''	private String validateRequest(ExamRequest request, Integer subjectId) {
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
	}'''

new_validate = '''	private String validateRequest(ExamRequest request, Integer subjectId) {
		if (request.getDuration() == null || request.getDuration() <= 0) {
			return "Thời lượng đề thi phải lớn hơn 0!";
		}
		if (request.getStartTime() != null && request.getEndTime() != null
				&& request.getEndTime().isBefore(request.getStartTime())) {
			return "Thời gian kết thúc phải sau thời gian bắt đầu!";
		}

		List<com.onthi.v_edu.exam.dto.ExamSectionRequest> sections = request.getSections() == null
				? Collections.emptyList()
				: request.getSections();
		Set<Integer> uniqueIds = new HashSet<>();
		for (com.onthi.v_edu.exam.dto.ExamSectionRequest section : sections) {
			if (section.getItems() != null) {
				for (ExamQuestionItemRequest item : section.getItems()) {
					if (item.getGroupId() != null) {
						// Group ID provided, skip individual question validation here
						// We'll trust the group's questions for now
						continue;
					}
					Integer questionId = item.getQuestionId();
					if (questionId == null) {
						return "Danh sách câu hỏi có phần tử thiếu questionId và groupId!";
					}
					if (!uniqueIds.add(questionId)) {
						return "Danh sách câu hỏi bị trùng questionId=" + questionId;
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
			}
		}
		return null;
	}'''
content = content.replace(old_validate, new_validate)

# Replace syncExamQuestions
old_sync = '''	private void syncExamQuestions(Exam exam, List<ExamQuestionItemRequest> items) {
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
	}'''

new_sync = '''	private void syncExamQuestions(Exam exam, List<com.onthi.v_edu.exam.dto.ExamSectionRequest> sections) {
		Integer examId = exam.getId();
		examQuestionRepository.softDeleteByExamId(examId);
		if (sections == null || sections.isEmpty()) {
			return;
		}

		List<ExamQuestion> examQuestions = new ArrayList<>();
		int globalOrderIndex = 1;
		
		for (com.onthi.v_edu.exam.dto.ExamSectionRequest section : sections) {
			if (section.getItems() == null) continue;
			for (ExamQuestionItemRequest item : section.getItems()) {
				if (item.getGroupId() != null) {
					// Fetch all questions for this group
					List<Question> groupQuestions = questionRepository.findByQuestionGroup_IdAndDeletedAtIsNull(item.getGroupId());
					for (Question question : groupQuestions) {
						ExamQuestion examQuestion = new ExamQuestion();
						examQuestion.setId(new ExamQuestionId(exam.getId(), question.getId()));
						examQuestion.setExam(exam);
						examQuestion.setQuestion(question);
						examQuestion.setOrderIndex(globalOrderIndex++);
						examQuestion.setScore(item.getScore() != null ? item.getScore() : 1.0); // Distribute score or default 1.0
						examQuestion.setSectionName(section.getSectionName());
						examQuestion.setContentSnapshot(normalize(question.getContent()));
						examQuestion.setContentFormatSnapshot(question.getContentFormat());
						examQuestions.add(examQuestion);
					}
				} else if (item.getQuestionId() != null) {
					Question question = questionRepository.findByIdAndDeletedAtIsNull(item.getQuestionId()).orElse(null);
					if (question != null) {
						ExamQuestion examQuestion = new ExamQuestion();
						examQuestion.setId(new ExamQuestionId(exam.getId(), question.getId()));
						examQuestion.setExam(exam);
						examQuestion.setQuestion(question);
						examQuestion.setOrderIndex(globalOrderIndex++);
						examQuestion.setScore(item.getScore());
						examQuestion.setSectionName(section.getSectionName());
						examQuestion.setContentSnapshot(normalize(item.getContentSnapshot() != null ? item.getContentSnapshot() : question.getContent()));
						examQuestion.setContentFormatSnapshot(item.getContentFormatSnapshot() != null ? item.getContentFormatSnapshot() : question.getContentFormat());
						examQuestions.add(examQuestion);
					}
				}
			}
		}
		examQuestionRepository.saveAll(examQuestions);
	}'''
content = content.replace(old_sync, new_sync)

# Replace createExam and updateExam calls to syncExamQuestions
content = content.replace('syncExamQuestions(exam, request.getQuestions());', 'syncExamQuestions(exam, request.getSections());')

# In toExamResponse, set sectionName
to_exam_response_find = '''							item.getScore(),
							item.getContentSnapshot(),
							item.getContentFormatSnapshot() != null ? item.getContentFormatSnapshot() : question.getContentFormat(),
							options
					);'''
to_exam_response_replace = '''							item.getScore(),
							item.getSectionName(),
							item.getContentSnapshot(),
							item.getContentFormatSnapshot() != null ? item.getContentFormatSnapshot() : question.getContentFormat(),
							options
					);'''
content = content.replace(to_exam_response_find, to_exam_response_replace)

# Replace buildSections
old_build_sections = '''	private List<ExamSectionResponse> buildSections(List<ExamQuestionItemResponse> questionItems) {
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

		if (type == null) {
			return prefix;
		}

		String suffix = switch (type) {
			case MCQ -> "Trắc nghiệm khách quan";
			case ESSAY -> "Tự luận";
			default -> "";
		};

		if (suffix.isEmpty()) {
			return prefix;
		}

		return prefix + ". " + suffix;
	}'''

new_build_sections = '''	private List<ExamSectionResponse> buildSections(List<ExamQuestionItemResponse> questionItems) {
		if (questionItems == null || questionItems.isEmpty()) {
			return Collections.emptyList();
		}
	
		// Group questions by sectionName. Keep insertion order of sections.
		Map<String, List<ExamQuestionItemResponse>> groupedBySection = new LinkedHashMap<>();
		for (ExamQuestionItemResponse item : questionItems) {
			String sectionName = item.getSectionName() != null ? item.getSectionName() : "Mặc định";
			groupedBySection.computeIfAbsent(sectionName, k -> new ArrayList<>()).add(item);
		}
	
		List<ExamSectionResponse> sections = new ArrayList<>();
		int sectionIndex = 0;
	
		for (Map.Entry<String, List<ExamQuestionItemResponse>> entry : groupedBySection.entrySet()) {
			String sectionName = entry.getKey();
			List<ExamQuestionItemResponse> questionsForSection = entry.getValue();
			if (questionsForSection.isEmpty()) {
				continue;
			}
			
			questionsForSection.sort(Comparator.comparingInt(item -> item.getOrderIndex() == null ? Integer.MAX_VALUE : item.getOrderIndex()));
			int startOrder = questionsForSection.get(0).getOrderIndex() != null ? questionsForSection.get(0).getOrderIndex() : 0;
			int endOrder = questionsForSection.get(questionsForSection.size() - 1).getOrderIndex() != null ? questionsForSection.get(questionsForSection.size() - 1).getOrderIndex() : 0;
			double totalScore = questionsForSection.stream().mapToDouble(q -> q.getScore() == null ? 0 : q.getScore()).sum();
	
			sections.add(new ExamSectionResponse(
					++sectionIndex,
					sectionName,
					"MIXED",
					questionsForSection.size(),
					totalScore,
					startOrder,
					endOrder,
					List.copyOf(questionsForSection)
			));
		}
		return sections;
	}'''
content = content.replace(old_build_sections, new_build_sections)

with open('E:/projectOnthi/onthi-backend/src/main/java/com/onthi/v_edu/exam/service/ExamServiceImpl.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("Done")
