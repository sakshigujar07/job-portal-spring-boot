package com.sakshi.jobportal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private LocalDate parseDeadline(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        return LocalDate.parse(dateStr);
    }

    private Job syncCompanyName(Job job) {
        if (job.getCompanyId() != null) {
            companyRepository.findById(job.getCompanyId())
                    .ifPresent(company -> job.setCompanyName(company.getName()));
        }
        return job;
    }

    @GetMapping
    public Page<Job> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search
    ) {
        Pageable pageable = PageRequest.of(page, size);

        if (search != null && !search.isBlank()) {
            return jobRepository
                    .findByTitleContainingIgnoreCaseOrCompanyNameContainingIgnoreCaseOrLocationContainingIgnoreCase(
                            search, search, search, pageable)
                    .map(this::syncCompanyName);
        }

        return jobRepository.findAll(pageable).map(this::syncCompanyName);
    }

    @PreAuthorize("hasRole('employer')")
    @GetMapping("/my")
    public Page<Job> getMyJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        return jobRepository.findByPostedBy(currentUser.getId(), pageable)
                .map(this::syncCompanyName);
    }

    @PreAuthorize("hasRole('employer')")
    @GetMapping("/my/count")
    public long getMyJobsCount() {
        User currentUser = getCurrentUser();
        return jobRepository.countByPostedBy(currentUser.getId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJobById(@PathVariable Long id) {
        return jobRepository.findById(id)
                .map(this::syncCompanyName)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Job not found"));
    }

    @PreAuthorize("hasRole('employer')")
    @PostMapping
    public ResponseEntity<?> createJob(@Valid @RequestBody JobRequestDTO request) {
        User currentUser = getCurrentUser();
        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setCompanyName(request.getCompanyName());
        job.setCompanyId(request.getCompanyId());
        job.setLocation(request.getLocation());
        job.setSalary(request.getSalary());
        job.setPostedBy(currentUser.getId());
        job.setPostedDate(LocalDateTime.now());
        job.setVacancy(request.getVacancy());
        job.setEmploymentType(request.getEmploymentType());
        job.setWorkArrangement(request.getWorkArrangement());
        job.setNotes(request.getNotes());
        job.setCompanyDescription(request.getCompanyDescription());

        try {
            job.setApplicationDeadline(parseDeadline(request.getApplicationDeadline()));
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid application deadline format. Use YYYY-MM-DD.");
        }

        return ResponseEntity.ok(jobRepository.save(job));
    }

    @PreAuthorize("hasRole('employer')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateJob(@PathVariable Long id, @Valid @RequestBody JobRequestDTO request) {
        User currentUser = getCurrentUser();
        return jobRepository.findById(id).map(job -> {
            if (!job.getPostedBy().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only edit jobs you posted");
            }
            job.setTitle(request.getTitle());
            job.setDescription(request.getDescription());
            job.setCompanyName(request.getCompanyName());
            job.setCompanyId(request.getCompanyId());
            job.setLocation(request.getLocation());
            job.setSalary(request.getSalary());
            job.setVacancy(request.getVacancy());
            job.setEmploymentType(request.getEmploymentType());
            job.setWorkArrangement(request.getWorkArrangement());
            job.setNotes(request.getNotes());
            job.setCompanyDescription(request.getCompanyDescription());

            try {
                job.setApplicationDeadline(parseDeadline(request.getApplicationDeadline()));
            } catch (DateTimeParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid application deadline format. Use YYYY-MM-DD.");
            }

            return ResponseEntity.ok(jobRepository.save(job));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('employer')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        return jobRepository.findById(id).map(job -> {
            if (!job.getPostedBy().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only delete jobs you posted");
            }
            jobRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}