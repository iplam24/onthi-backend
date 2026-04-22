package com.onthi.v_edu.common.fileupload.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.fileupload.dto.UploadedFileResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUpLoadService {

	@Value("${app.file-upload-dir:uploads}")
	private String uploadDir;

	@Value("${app.file-upload-base-url:/uploads}")
	private String uploadBaseUrl;

	private Path uploadRootPath;

	@PostConstruct
	public void init() throws IOException {
		uploadRootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
		Files.createDirectories(uploadRootPath);
	}

	@Override
	public ApiResponse<UploadedFileResponse> uploadFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "File upload không được rỗng!");
		}

		try {
			String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
			if (!StringUtils.hasText(originalName)) {
				return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Tên file không hợp lệ!");
			}

			String dateFolder = buildDateFolder();
			Path targetDir = uploadRootPath.resolve(dateFolder).normalize();
			Files.createDirectories(targetDir);

			String storedFileName = dateFolder + "/" + buildStoredFileName(originalName);
			Path targetPath = uploadRootPath.resolve(storedFileName).normalize();
			if (!targetPath.startsWith(uploadRootPath)) {
				return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Tên file không hợp lệ!");
			}

			Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

			UploadedFileResponse data = new UploadedFileResponse(
					originalName,
					storedFileName,
					file.getContentType(),
					file.getSize(),
					buildPublicUrl(storedFileName),
					targetPath.toString(),
					LocalDateTime.now()
			);
			return new ApiResponse<>(HttpStatus.CREATED.value(), "Tải file lên thành công!", data);
		} catch (IOException ex) {
			return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Không thể tải file lên: " + ex.getMessage());
		}
	}

	@Override
	public ApiResponse<List<UploadedFileResponse>> uploadFiles(List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Danh sách file upload không được rỗng!");
		}

		List<UploadedFileResponse> uploadedFiles = new ArrayList<>();
		for (MultipartFile file : files) {
			ApiResponse<UploadedFileResponse> response = uploadFile(file);
			if (response.getStatus() >= 400) {
				return new ApiResponse<>(response.getStatus(), response.getMessage());
			}
			uploadedFiles.add(response.getData());
		}

		return new ApiResponse<>(HttpStatus.CREATED.value(), "Tải danh sách file lên thành công!", uploadedFiles);
	}

	private String buildStoredFileName(String originalName) {
		String extension = StringUtils.getFilenameExtension(originalName);
		String baseName = StringUtils.stripFilenameExtension(originalName);
		String safeBaseName = sanitizeFileName(baseName);
		String uuid = UUID.randomUUID().toString().replace("-", "");
		String fileName = uuid + "_" + safeBaseName;
		if (StringUtils.hasText(extension)) {
			fileName += "." + extension.toLowerCase();
		}
		return fileName;
	}

	private String buildDateFolder() {
		return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
	}

	private String sanitizeFileName(String value) {
		if (!StringUtils.hasText(value)) {
			return "file";
		}

		String sanitized = value.trim()
				.replaceAll("[^a-zA-Z0-9._-]", "_")
				.replaceAll("_+", "_");
		return StringUtils.hasText(sanitized) ? sanitized : "file";
	}

	private String buildPublicUrl(String storedFileName) {
		String baseUrl = uploadBaseUrl == null ? "/uploads" : uploadBaseUrl.trim();
		if (!baseUrl.startsWith("/")) {
			baseUrl = "/" + baseUrl;
		}
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		return baseUrl + "/" + storedFileName;
	}
}
