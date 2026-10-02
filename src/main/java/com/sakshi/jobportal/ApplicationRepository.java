package com.sakshi.jobportal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByApplicantId(Long applicantId);

    List<Application> findByJobIdIn(List<Long> jobIds);

    boolean existsByJobIdAndApplicantId(Long jobId, Long applicantId);
}