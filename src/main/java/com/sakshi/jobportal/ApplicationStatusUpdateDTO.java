package com.sakshi.jobportal;

import jakarta.validation.constraints.NotNull;

public class ApplicationStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private ApplicationStatus status;

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }
}
