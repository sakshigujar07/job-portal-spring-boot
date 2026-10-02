package com.sakshi.jobportal;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private String companyName;
    private String location;
    private String salary;
    private Long postedBy;

    private Integer vacancy;
    private String employmentType;
    private String workArrangement;
    private LocalDate applicationDeadline;
    private String notes;
    private String companyDescription;

    private Long companyId;

    private LocalDateTime postedDate;
}