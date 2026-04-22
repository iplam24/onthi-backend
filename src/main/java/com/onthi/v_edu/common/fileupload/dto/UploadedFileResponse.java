package com.onthi.v_edu.common.fileupload.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFileResponse {

	private String originalName;
	private String fileName;
	private String contentType;
	private long size;
	private String url;
	private String storagePath;
	private LocalDateTime uploadedAt;
}

