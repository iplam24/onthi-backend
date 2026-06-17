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
import com.onthi.v_edu.question.repository.QuestionGroupRepository;
import com.onthi.v_edu.question.entity.QuestionGroup;
import com.onthi.v_edu.question.dto.QuestionGroupRequest;
import com.onthi.v_edu.question.dto.QuestionGroupResponse;
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

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import com.onthi.v_edu.common.fileupload.service.FileUpLoadService;
import com.onthi.v_edu.common.fileupload.dto.UploadedFileResponse;
import com.onthi.v_edu.common.constant.DifficultyLevel;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

@Service
@Transactional
public class QuestionServiceImpl implements QuestionService {

	private final QuestionRepository questionRepository;
	private final QuestionOptionRepository questionOptionRepository;
	private final EssayAnswerRepository essayAnswerRepository;
	private final ExplanationRepository explanationRepository;
	private final TopicRepository topicRepository;
	private final UserRepository userRepository;
	private final FileUpLoadService fileUpLoadService;
	private final QuestionGroupRepository questionGroupRepository;

	public QuestionServiceImpl(QuestionRepository questionRepository,
						   QuestionOptionRepository questionOptionRepository,
						   EssayAnswerRepository essayAnswerRepository,
						   ExplanationRepository explanationRepository,
						   TopicRepository topicRepository,
						   UserRepository userRepository,
						   FileUpLoadService fileUpLoadService,
						   QuestionGroupRepository questionGroupRepository) {
		this.questionRepository = questionRepository;
		this.questionOptionRepository = questionOptionRepository;
		this.essayAnswerRepository = essayAnswerRepository;
		this.explanationRepository = explanationRepository;
		this.topicRepository = topicRepository;
		this.userRepository = userRepository;
		this.fileUpLoadService = fileUpLoadService;
		this.questionGroupRepository = questionGroupRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResponse<QuestionResponse>> getAllQuestions(Integer subjectId, Integer topicId, Pageable pageable) {
		Page<Question> questionPage;
		if (subjectId != null && topicId != null) {
			questionPage = questionRepository.findByTopic_IdAndTopic_Subject_IdAndQuestionGroupIsNullAndDeletedAtIsNull(topicId, subjectId, pageable);
		} else if (subjectId != null) {
			questionPage = questionRepository.findByTopic_Subject_IdAndQuestionGroupIsNullAndDeletedAtIsNull(subjectId, pageable);
		} else if (topicId != null) {
			questionPage = questionRepository.findByTopic_IdAndQuestionGroupIsNullAndDeletedAtIsNull(topicId, pageable);
		} else {
			questionPage = questionRepository.findByQuestionGroupIsNullAndDeletedAtIsNull(pageable);
		}

		Page<QuestionResponse> data = questionPage
				.map(this::toQuestionResponse);
		return new ApiResponse<>(HttpStatus.OK.value(), "Lấy danh sách câu hỏi thành công!", PageResponse.from(data));
	}

	@Override
	public ApiResponse<PageResponse<QuestionGroupResponse>> getAllQuestionGroups(Integer subjectId, Integer topicId, Pageable pageable) {
		Page<QuestionGroup> groupPage;
		if (topicId != null) {
			groupPage = questionGroupRepository.findByTopic_Id(topicId, pageable);
		} else if (subjectId != null) {
			groupPage = questionGroupRepository.findByTopic_Subject_Id(subjectId, pageable);
		} else {
			groupPage = questionGroupRepository.findAll(pageable);
		}

		Page<QuestionGroupResponse> data = groupPage.map(group -> new QuestionGroupResponse(
				group.getId(),
				group.getTitle(),
				group.getContent(),
				group.getContentFormat() != null ? group.getContentFormat() : ContentFormat.PLAIN_TEXT,
				group.getAudioUrl(),
				questionRepository.findByQuestionGroup_IdAndDeletedAtIsNull(group.getId())
					.stream().map(this::toQuestionResponse).toList(),
				group.getTopic() != null ? group.getTopic().getId() : null,
				group.getTopic() != null ? group.getTopic().getName() : null,
				group.getCreatedBy() != null ? group.getCreatedBy().getId() : null,
				group.getCreatedBy() != null ? group.getCreatedBy().getUsername() : null,
				group.getCreatedAt()
		));

		return new ApiResponse<>(HttpStatus.OK.value(), "Lấy danh sách nhóm câu hỏi thành công!", PageResponse.from(data));
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
		question.setAudioUrl(normalize(request.getAudioUrl()));
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
		question.setAudioUrl(normalize(request.getAudioUrl()));
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

	@Override
	public ApiResponse<QuestionGroupResponse> createQuestionGroup(QuestionGroupRequest request) {
		Topic topic = topicRepository.findById(request.getTopicId()).orElse(null);
		if (topic == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy topic!");
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		User user = userRepository.findById(userDetails.getId()).orElse(null);

		QuestionGroup group = new QuestionGroup();
		group.setTitle(request.getTitle());
		group.setContent(request.getContent());
		group.setContentFormat(request.getContentFormat() != null ? request.getContentFormat() : ContentFormat.PLAIN_TEXT);
		group.setAudioUrl(request.getAudioUrl());
		group.setTopic(topic);
		group.setCreatedBy(user);
		
		group = questionGroupRepository.save(group);

		List<QuestionResponse> childQuestions = new ArrayList<>();
		if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
			for (QuestionRequest qReq : request.getQuestions()) {
				Question child = new Question();
				child.setContent(normalize(qReq.getContent()));
				child.setContentFormat(resolveContentFormat(qReq.getContentFormat()));
				child.setUrl(normalize(qReq.getUrl()));
				child.setType(qReq.getType());
				child.setDifficulty(qReq.getDifficulty());
				child.setTopic(topic);
				child.setCreatedBy(user);
				child.setQuestionGroup(group);
				
				child = questionRepository.save(child);
				syncQuestionDetails(child, qReq);
				childQuestions.add(toQuestionResponse(child));
			}
		}

		QuestionGroupResponse res = new QuestionGroupResponse();
		res.setId(group.getId());
		res.setTitle(group.getTitle());
		res.setContent(group.getContent());
		res.setContentFormat(group.getContentFormat());
		res.setAudioUrl(group.getAudioUrl());
		res.setQuestions(childQuestions);

		return new ApiResponse<>(HttpStatus.CREATED.value(), "Tạo nhóm câu hỏi thành công!", res);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<QuestionGroupResponse> getQuestionGroupById(Integer id) {
		QuestionGroup group = questionGroupRepository.findById(id).orElse(null);
		if (group == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy nhóm câu hỏi!");
		}

		List<QuestionResponse> childQuestions = questionRepository.findByQuestionGroup_IdAndDeletedAtIsNull(group.getId())
				.stream().map(this::toQuestionResponse).toList();

		QuestionGroupResponse res = new QuestionGroupResponse(
				group.getId(),
				group.getTitle(),
				group.getContent(),
				group.getContentFormat() != null ? group.getContentFormat() : ContentFormat.PLAIN_TEXT,
				group.getAudioUrl(),
				childQuestions,
				group.getTopic() != null ? group.getTopic().getId() : null,
				group.getTopic() != null ? group.getTopic().getName() : null,
				group.getCreatedBy() != null ? group.getCreatedBy().getId() : null,
				group.getCreatedBy() != null ? group.getCreatedBy().getUsername() : null,
				group.getCreatedAt()
		);

		return new ApiResponse<>(HttpStatus.OK.value(), "Lấy nhóm câu hỏi thành công!", res);
	}

	@Override
	public ApiResponse<QuestionGroupResponse> updateQuestionGroup(Integer id, QuestionGroupRequest request) {
		QuestionGroup group = questionGroupRepository.findById(id).orElse(null);
		if (group == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy nhóm câu hỏi!");
		}

		Topic topic = topicRepository.findById(request.getTopicId()).orElse(null);
		if (topic == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy topic!");
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		User user = userRepository.findById(userDetails.getId()).orElse(null);

		group.setTitle(request.getTitle());
		group.setContent(request.getContent());
		group.setContentFormat(request.getContentFormat() != null ? request.getContentFormat() : ContentFormat.PLAIN_TEXT);
		group.setAudioUrl(request.getAudioUrl());
		group.setTopic(topic);
		group = questionGroupRepository.save(group);

		// Handle child questions
		List<Question> existingChildren = questionRepository.findByQuestionGroup_IdAndDeletedAtIsNull(group.getId());
		
		// If request has questions, we sync them. (For simplicity, we delete missing and add/update existing)
		List<Integer> requestQuestionIds = new ArrayList<>();
		List<QuestionResponse> childResponses = new ArrayList<>();
		
		if (request.getQuestions() != null) {
			for (QuestionRequest qReq : request.getQuestions()) {
				Question child;
				if (qReq.getId() != null) {
					child = questionRepository.findByIdAndDeletedAtIsNull(qReq.getId()).orElse(new Question());
					requestQuestionIds.add(qReq.getId());
				} else {
					child = new Question();
				}
				
				child.setContent(normalize(qReq.getContent()));
				child.setContentFormat(resolveContentFormat(qReq.getContentFormat()));
				child.setUrl(normalize(qReq.getUrl()));
				child.setType(qReq.getType());
				child.setDifficulty(qReq.getDifficulty());
				child.setTopic(topic);
				child.setCreatedBy(user);
				child.setQuestionGroup(group);
				
				child = questionRepository.save(child);
				syncQuestionDetails(child, qReq);
				childResponses.add(toQuestionResponse(child));
			}
		}

		// Delete questions that are not in the new request
		for (Question oldChild : existingChildren) {
			if (!requestQuestionIds.contains(oldChild.getId())) {
				deleteQuestion(oldChild.getId());
			}
		}

		QuestionGroupResponse res = new QuestionGroupResponse(
				group.getId(),
				group.getTitle(),
				group.getContent(),
				group.getContentFormat() != null ? group.getContentFormat() : ContentFormat.PLAIN_TEXT,
				group.getAudioUrl(),
				childResponses,
				group.getTopic() != null ? group.getTopic().getId() : null,
				group.getTopic() != null ? group.getTopic().getName() : null,
				group.getCreatedBy() != null ? group.getCreatedBy().getId() : null,
				group.getCreatedBy() != null ? group.getCreatedBy().getUsername() : null,
				group.getCreatedAt()
		);

		return new ApiResponse<>(HttpStatus.OK.value(), "Cập nhật nhóm câu hỏi thành công!", res);
	}

	@Override
	public ApiResponse<Void> deleteQuestionGroup(Integer id) {
		QuestionGroup group = questionGroupRepository.findById(id).orElse(null);
		if (group == null) {
			return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Không tìm thấy nhóm câu hỏi!");
		}

		List<Question> existingChildren = questionRepository.findByQuestionGroup_IdAndDeletedAtIsNull(group.getId());
		for (Question child : existingChildren) {
			deleteQuestion(child.getId());
		}

		questionGroupRepository.delete(group);
		return new ApiResponse<>(HttpStatus.OK.value(), "Xoá nhóm câu hỏi thành công!");
	}

	@Override
	public ApiResponse<List<QuestionRequest>> previewQuestionsFromExcel(MultipartFile file, String imageFolderPath) {
		if (file == null || file.isEmpty()) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "File Excel trống!", null);
		}

		String imageDir = imageFolderPath;
		if (!org.springframework.util.StringUtils.hasText(imageDir)) {
			imageDir = System.getProperty("user.home") + java.io.File.separator + "Downloads";
		}

		List<QuestionRequest> requests = new ArrayList<>();

		try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rows = sheet.iterator();

			if (rows.hasNext()) {
				rows.next(); // Skip header
			}

			while (rows.hasNext()) {
				Row currentRow = rows.next();
				if (isRowEmpty(currentRow)) break;

				try {
					QuestionRequest req = new QuestionRequest();

					Cell topicIdCell = currentRow.getCell(1);
					req.setTopicId((int) topicIdCell.getNumericCellValue());

					Cell contentCell = currentRow.getCell(2);
					req.setContent(getCellValueAsString(contentCell));

					Cell formatCell = currentRow.getCell(3);
					req.setContentFormat(parseContentFormat(getCellValueAsString(formatCell)));

					Cell imageCell = currentRow.getCell(4);
					String imageName = getCellValueAsString(imageCell);
					if (org.springframework.util.StringUtils.hasText(imageName)) {
						String fullImagePath = Paths.get(imageDir, imageName).toString();
						ApiResponse<UploadedFileResponse> uploadRes = fileUpLoadService.uploadLocalFile(fullImagePath);
						if (uploadRes.getStatus() == HttpStatus.CREATED.value() && uploadRes.getData() != null) {
							req.setUrl(uploadRes.getData().getUrl());
						}
					}

					Cell typeCell = currentRow.getCell(5);
					req.setType(parseQuestionType(getCellValueAsString(typeCell)));

					Cell diffCell = currentRow.getCell(6);
					req.setDifficulty(parseDifficulty(getCellValueAsString(diffCell)));

					Cell expCell = currentRow.getCell(7);
					req.setExplanation(getCellValueAsString(expCell));

					Cell essayCell = currentRow.getCell(8);
					if (req.getType() == QuestionType.ESSAY) {
						req.setSampleAnswer(getCellValueAsString(essayCell));
					} else {
						List<OptionRequest> options = new ArrayList<>();
						String correctListStr = getCellValueAsString(currentRow.getCell(13));
						List<String> correctLetters = new ArrayList<>();
						if (org.springframework.util.StringUtils.hasText(correctListStr)) {
							String cleanCorrect = correctListStr.toUpperCase().replaceAll("\\s+", "");
							for (String part : cleanCorrect.split(",")) {
								if (org.springframework.util.StringUtils.hasText(part)) {
									correctLetters.add(part.trim());
								}
							}
						}

						char[] optionLetters = {'A', 'B', 'C', 'D'};
						for (int i = 0; i < optionLetters.length; i++) {
							int colIdx = 9 + i;
							String optContent = getCellValueAsString(currentRow.getCell(colIdx));
							if (org.springframework.util.StringUtils.hasText(optContent)) {
								OptionRequest opt = new OptionRequest();
								opt.setContent(optContent);
								String letter = String.valueOf(optionLetters[i]);
								opt.setIsCorrect(correctLetters.contains(letter));
								options.add(opt);
							}
						}
						req.setOptions(options);
					}

					requests.add(req);
				} catch (Exception e) {
					System.err.println("Lỗi parse dòng trong Excel: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Lỗi khi đọc file Excel: " + e.getMessage(), null);
		}

		if (requests.isEmpty()) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Không tìm thấy dữ liệu hợp lệ trong file Excel!", null);
		}

		return new ApiResponse<>(HttpStatus.OK.value(), "Đọc file thành công", requests);
	}

	@Override
	public ApiResponse<Void> importQuestionsFromExcel(MultipartFile file, String imageFolderPath) {
		ApiResponse<List<QuestionRequest>> previewRes = previewQuestionsFromExcel(file, imageFolderPath);
		if (previewRes.getStatus() != HttpStatus.OK.value()) {
			return new ApiResponse<>(previewRes.getStatus(), previewRes.getMessage());
		}
		return createQuestions(previewRes.getData());
	}

	@Override
	public ResponseEntity<Resource> generateExcelTemplate() {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Import Template");
			Row headerRow = sheet.createRow(0);

			String[] headers = {
					"STT", "Topic ID", "Nội dung câu hỏi", "Định dạng (PLAIN_TEXT/LATEX)", 
					"Tên file ảnh (tuỳ chọn)", "Loại (MCQ/ESSAY)", "Độ khó (EASY/MEDIUM/HARD)", 
					"Giải thích (tuỳ chọn)", "Đáp án tự luận (Nếu ESSAY)",
					"Đáp án A", "Đáp án B", "Đáp án C", "Đáp án D", "Đáp án đúng (Ví dụ: A hoặc A, B)"
			};

			CellStyle headerStyle = workbook.createCellStyle();
			Font font = workbook.createFont();
			font.setBold(true);
			headerStyle.setFont(font);
			headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
			headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			headerStyle.setBorderBottom(BorderStyle.THIN);
			headerStyle.setBorderTop(BorderStyle.THIN);
			headerStyle.setBorderLeft(BorderStyle.THIN);
			headerStyle.setBorderRight(BorderStyle.THIN);
			headerStyle.setAlignment(HorizontalAlignment.CENTER);
			headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			headerStyle.setWrapText(true);

			CellStyle dataStyle = workbook.createCellStyle();
			dataStyle.setBorderBottom(BorderStyle.THIN);
			dataStyle.setBorderTop(BorderStyle.THIN);
			dataStyle.setBorderLeft(BorderStyle.THIN);
			dataStyle.setBorderRight(BorderStyle.THIN);
			dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			dataStyle.setWrapText(true);

			headerRow.setHeightInPoints(30);

			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
				
				if (i == 2 || i == 7 || i == 8) {
					sheet.setColumnWidth(i, 12000);
				} else if (i >= 9) {
					sheet.setColumnWidth(i, 8000);
				} else {
					sheet.setColumnWidth(i, 6000);
				}
			}

			// Ví dụ 1: MCQ bình thường
			Row row1 = sheet.createRow(1);
			row1.setHeightInPoints(45);
			Object[] row1Data = {
					1, 1, "Thủ đô của Việt Nam là gì?", "PLAIN_TEXT", "hanoi_map.jpg", "MCQ", "EASY", 
					"Hà Nội là thủ đô của Việt Nam.", "", "Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Cần Thơ", "A"
			};
			for (int i = 0; i < row1Data.length; i++) {
				Cell cell = row1.createCell(i);
				if (row1Data[i] instanceof Integer) cell.setCellValue((Integer) row1Data[i]);
				else cell.setCellValue((String) row1Data[i]);
				cell.setCellStyle(dataStyle);
			}

			// Ví dụ 2: ESSAY
			Row row2 = sheet.createRow(2);
			row2.setHeightInPoints(45);
			Object[] row2Data = {
					2, 1, "Hãy phân tích nhân vật Chí Phèo.", "PLAIN_TEXT", "", "ESSAY", "HARD", 
					"", "Chí Phèo là một kiệt tác của Nam Cao...", "", "", "", "", ""
			};
			for (int i = 0; i < row2Data.length; i++) {
				Cell cell = row2.createCell(i);
				if (row2Data[i] instanceof Integer) cell.setCellValue((Integer) row2Data[i]);
				else cell.setCellValue((String) row2Data[i]);
				cell.setCellStyle(dataStyle);
			}

			// Ví dụ 3: Toán học (LATEX)
			Row row3 = sheet.createRow(3);
			row3.setHeightInPoints(45);
			Object[] row3Data = {
					3, 1, "Tính đạo hàm của hàm số \\( f(x) = x^2 + 3x + 1 \\).", "LATEX", "", "MCQ", "MEDIUM", 
					"Dùng quy tắc cơ bản.", "", "\\( f'(x) = 2x + 3 \\)", "\\( f'(x) = 2x - 3 \\)", 
					"\\( x^2 + 3 \\)", "\\( 2x + 1 \\)", "A"
			};
			for (int i = 0; i < row3Data.length; i++) {
				Cell cell = row3.createCell(i);
				if (row3Data[i] instanceof Integer) cell.setCellValue((Integer) row3Data[i]);
				else cell.setCellValue((String) row3Data[i]);
				cell.setCellStyle(dataStyle);
			}

			// Sheet: Công thức Toán
			Sheet mathSheet = workbook.createSheet("Công thức Toán (LaTeX)");
			Row mathHeader = mathSheet.createRow(0);
			mathHeader.setHeightInPoints(30);
			String[] mHeaders = {"Tên công thức", "Mã LaTeX", "Ví dụ"};
			for (int i = 0; i < mHeaders.length; i++) {
				Cell cell = mathHeader.createCell(i);
				cell.setCellValue(mHeaders[i]);
				cell.setCellStyle(headerStyle);
				mathSheet.setColumnWidth(i, 10000);
			}
			String[][] mathExamples = {
				{"Phân số", "\\\\frac{a}{b}", "\\\\( \\\\frac{1}{2} \\\\)"},
				{"Căn thức", "\\\\sqrt{x}", "\\\\( \\\\sqrt{2} \\\\)"},
				{"Số mũ / Chỉ số", "x^n, x_i", "\\\\( x^2, a_1 \\\\)"},
				{"Tổng / Tích phân", "\\\\sum, \\\\int", "\\\\( \\\\int_{0}^{1} x dx \\\\)"},
				{"Hệ phương trình", "\\\\begin{cases} ... \\\\end{cases}", "\\\\( \\\\begin{cases} x=1 \\\\\\\\ y=2 \\\\end{cases} \\\\)"}
			};
			for (int i = 0; i < mathExamples.length; i++) {
				Row r = mathSheet.createRow(i + 1);
				for (int j = 0; j < mathExamples[i].length; j++) {
					Cell c = r.createCell(j);
					c.setCellValue(mathExamples[i][j]);
					c.setCellStyle(dataStyle);
				}
			}

			// Sheet: Danh sách Topic
			Sheet topicSheet = workbook.createSheet("Danh sách Chủ đề (Topics)");
			Row topicHeader = topicSheet.createRow(0);
			topicHeader.setHeightInPoints(30);
			String[] tHeaders = {"Topic ID", "Tên chủ đề", "Môn học", "Cấp học"};
			for (int i = 0; i < tHeaders.length; i++) {
				Cell cell = topicHeader.createCell(i);
				cell.setCellValue(tHeaders[i]);
				cell.setCellStyle(headerStyle);
				topicSheet.setColumnWidth(i, 8000);
			}

			List<Topic> allTopics = topicRepository.findAll();
			int rIdx = 1;
			for (Topic t : allTopics) {
				Row r = topicSheet.createRow(rIdx++);
				r.createCell(0).setCellValue(t.getId());
				r.createCell(1).setCellValue(t.getName() != null ? t.getName() : "");
				r.createCell(2).setCellValue(t.getSubject() != null ? t.getSubject().getName() : "");
				r.createCell(3).setCellValue(t.getSubject() != null && t.getSubject().getLevel() != null ? t.getSubject().getLevel().getName() : "");
				for(int i=0; i<4; i++) {
					if(r.getCell(i) != null) r.getCell(i).setCellStyle(dataStyle);
				}
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workbook.write(out);
			ByteArrayResource resource = new ByteArrayResource(out.toByteArray());
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=question_import_template.xlsx")
					.contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
					.body(resource);

		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}

	private boolean isRowEmpty(Row row) {
		if (row == null) return true;
		for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
			Cell cell = row.getCell(c);
			if (cell != null && cell.getCellType() != CellType.BLANK && org.springframework.util.StringUtils.hasText(cell.toString())) {
				return false;
			}
		}
		return true;
	}

	private String getCellValueAsString(Cell cell) {
		if (cell == null) return null;
		switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue().trim();
			case NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					return cell.getDateCellValue().toString();
				}
				return String.valueOf(cell.getNumericCellValue());
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			default:
				return "";
		}
	}

	private ContentFormat parseContentFormat(String val) {
		if ("LATEX".equalsIgnoreCase(val)) return ContentFormat.LATEX;
		return ContentFormat.PLAIN_TEXT;
	}

	private QuestionType parseQuestionType(String val) {
		if ("LISTENING".equalsIgnoreCase(val)) return QuestionType.LISTENING;
		if ("SPEAKING".equalsIgnoreCase(val)) return QuestionType.SPEAKING;
		if ("ESSAY".equalsIgnoreCase(val)) return QuestionType.ESSAY;
		return QuestionType.MCQ;
	}

	private DifficultyLevel parseDifficulty(String val) {
		if ("HARD".equalsIgnoreCase(val)) return DifficultyLevel.HARD;
		if ("MEDIUM".equalsIgnoreCase(val)) return DifficultyLevel.MEDIUM;
		return DifficultyLevel.EASY;
	}

	private void syncQuestionDetails(Question question, QuestionRequest request) {
		Integer questionId = question.getId();
		questionOptionRepository.softDeleteByQuestionId(questionId);
		essayAnswerRepository.softDeleteByQuestionId(questionId);

		if (request.getType() == QuestionType.MCQ || request.getType() == QuestionType.LISTENING) {
			List<OptionRequest> optionRequests = request.getOptions() == null ? Collections.emptyList() : request.getOptions();
			List<QuestionOption> options = optionRequests.stream()
					.map(optionRequest -> {
						QuestionOption option = new QuestionOption();
						option.setQuestion(question);
						option.setContent(normalize(optionRequest.getContent()));
						option.setIsCorrect(optionRequest.getIsCorrect());
						option.setImageUrl(normalize(optionRequest.getImageUrl()));
						return option;
					})
					.toList();
			questionOptionRepository.saveAll(options);
		}

		if (request.getType() == QuestionType.ESSAY || request.getType() == QuestionType.LISTENING) {
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
		if (request.getType() == QuestionType.MCQ || request.getType() == QuestionType.LISTENING) {
			List<OptionRequest> options = request.getOptions() == null ? Collections.emptyList() : request.getOptions();
			if (options.size() < 2) {
				// LISTENING có thể là dạng điền từ, cho phép không có options
				if (request.getType() == QuestionType.LISTENING && options.isEmpty()) {
					if (isBlank(request.getSampleAnswer())) {
						return "Câu hỏi LISTENING phải có đáp án (options hoặc sampleAnswer)!";
					}
					return null;
				}
				return "Câu hỏi MCQ/LISTENING phải có ít nhất 2 đáp án!";
			}

			long correctCount = options.stream()
					.filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
					.count();
			if (correctCount == 0) {
				return "Câu hỏi MCQ/LISTENING phải có ít nhất 1 đáp án đúng!";
			}
			return null;
		}

		if (request.getType() == QuestionType.ESSAY) {
			if (isBlank(request.getSampleAnswer())) {
				return "Câu hỏi ESSAY phải có đáp án mẫu!";
			}
			return null;
		}

		// SPEAKING không yêu cầu đáp án mẫu bắt buộc
		return null;
	}

	private QuestionResponse toQuestionResponse(Question question) {
		Integer questionId = question.getId();
		List<OptionResponse> options = questionOptionRepository.findByQuestion_IdAndDeletedAtIsNullOrderByIdAsc(questionId).stream()
				.map(option -> new OptionResponse(option.getId(), option.getContent(), option.getIsCorrect(), option.getImageUrl()))
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
				question.getAudioUrl(),
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
				explanation != null ? explanation.getCreatedAt() : null,
				question.getQuestionGroup() != null ? question.getQuestionGroup().getId() : null
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
