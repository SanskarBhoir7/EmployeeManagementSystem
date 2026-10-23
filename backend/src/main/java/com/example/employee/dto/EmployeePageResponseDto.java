package com.example.employee.dto;

import org.springframework.data.domain.Page;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A page of employee results and its pagination metadata. Page numbering is zero-based.")
public class EmployeePageResponseDto {

    private List<EmployeeResponseDto> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    public EmployeePageResponseDto() {
    }

    public EmployeePageResponseDto(List<EmployeeResponseDto> content, long totalElements,
                                   int totalPages, int page, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.size = size;
    }

    public static EmployeePageResponseDto from(Page<EmployeeResponseDto> page) {
        return new EmployeePageResponseDto(page.getContent(), page.getTotalElements(),
                page.getTotalPages(), page.getNumber(), page.getSize());
    }

    public List<EmployeeResponseDto> getContent() { return content; }
    public void setContent(List<EmployeeResponseDto> content) { this.content = content; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
