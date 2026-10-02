package com.sakshi.jobportal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByPostedBy(Long postedBy);

    Page<Job> findByPostedBy(Long postedBy, Pageable pageable);

    long countByPostedBy(Long postedBy);

    Page<Job> findByTitleContainingIgnoreCaseOrCompanyNameContainingIgnoreCaseOrLocationContainingIgnoreCase(
            String title, String companyName, String location, Pageable pageable);
}