package com.sakshi.jobportal;

import lombok.Data;

@Data
public class ApplicationResponseDTO {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private Long applicantId;
    private String applicantName;
    private String status;
    private String resumePath;
}