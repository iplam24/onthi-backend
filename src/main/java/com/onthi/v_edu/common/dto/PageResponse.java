package com.onthi.v_edu.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

	private List<T> items;
	private int page;
	private int size;
	private long totalElements;
	private int totalPages;
	private int numberOfElements;
	private boolean first;
	private boolean last;
	private boolean hasNext;
	private boolean hasPrevious;

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.getNumberOfElements(),
				page.isFirst(),
				page.isLast(),
				page.hasNext(),
				page.hasPrevious()
		);
	}
}
