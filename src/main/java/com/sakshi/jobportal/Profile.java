package com.sakshi.jobportal;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "profiles")
@Data
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String resumePath;
    private String skills;
    private String bio;

    private String fullName;
    private Integer experience;
    private String currentLocation;
    private String expectedSalary;
    private String education;
}