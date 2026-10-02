package com.sakshi.jobportal;

import jakarta.validation.constraints.NotNull;

public class ApplicationRequestDTO {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }
}
