package com.onthi.v_edu.common.fileupload.service;

import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.common.fileupload.dto.UploadedFileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileUpLoadService {

	ApiResponse<UploadedFileResponse> uploadFile(MultipartFile file);

	ApiResponse<List<UploadedFileResponse>> uploadFiles(List<MultipartFile> files);
	void deleteFile(String fileUrl);
	ApiResponse<UploadedFileResponse> uploadLocalFile(String localFilePath);
}
