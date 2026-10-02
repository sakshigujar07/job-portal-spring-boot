package com.sakshi.jobportal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public List<ApplicationResponseDTO> getAllApplications() {
        User currentUser = getCurrentUser();

        List<Application> filtered;
        if ("jobseeker".equals(currentUser.getRole())) {
            filtered = applicationRepository.findByApplicantId(currentUser.getId());
        } else {
            List<Long> myJobIds = jobRepository.findByPostedBy(currentUser.getId()).stream()
                    .map(Job::getId)
                    .collect(Collectors.toList());

            filtered = myJobIds.isEmpty()
                    ? List.of()
                    : applicationRepository.findByJobIdIn(myJobIds);
        }

        List<Long> jobIds = filtered.stream().map(Application::getJobId).distinct().collect(Collectors.toList());
        List<Long> applicantIds = filtered.stream().map(Application::getApplicantId).distinct().collect(Collectors.toList());

        Map<Long, String> jobTitles = jobRepository.findAllById(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, j -> j.getTitle() != null ? j.getTitle() : "Untitled"));
        Map<Long, String> userNames = userRepository.findAllById(applicantIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getName() != null ? u.getName() : "Unknown"));

        return filtered.stream().map(app -> {
            ApplicationResponseDTO dto = new ApplicationResponseDTO();
            dto.setId(app.getId());
            dto.setJobId(app.getJobId());
            dto.setJobTitle(jobTitles.getOrDefault(app.getJobId(), "Unknown Job"));
            dto.setApplicantId(app.getApplicantId());
            dto.setApplicantName(userNames.getOrDefault(app.getApplicantId(), "Unknown Applicant"));
            dto.setStatus(app.getStatus());
            dto.setResumePath(app.getResumePath());
            return dto;
        }).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('jobseeker')")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createApplication(
            @RequestParam("jobId") Long jobId,
            @RequestParam(value = "resume", required = false) MultipartFile resume) {

        User currentUser = getCurrentUser();

        boolean alreadyApplied = applicationRepository.existsByJobIdAndApplicantId(jobId, currentUser.getId());

        if (alreadyApplied) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("You have already applied to this job");
        }

        String savedResumePath = null;

        if (resume != null && !resume.isEmpty()) {
            String contentType = resume.getContentType();
            boolean isAllowedType = contentType != null &&
                    (contentType.equals("application/pdf")
                            || contentType.equals("application/msword")
                            || contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

            if (!isAllowedType) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Only PDF or Word documents are allowed for resumes");
            }

            try {
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String originalFilename = resume.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : "";
                String uniqueFilename = UUID.randomUUID() + extension;

                Path targetPath = uploadPath.resolve(uniqueFilename);
                Files.copy(resume.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                savedResumePath = uploadDir + "/" + uniqueFilename;
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to save resume file");
            }
        }

        Application application = new Application();
        application.setJobId(jobId);
        application.setApplicantId(currentUser.getId());
        application.setStatus(ApplicationStatus.PENDING.name());
        application.setResumePath(savedResumePath);
        Application saved = applicationRepository.save(application);
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('employer')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateApplication(@PathVariable Long id, @Valid @RequestBody ApplicationStatusUpdateDTO request) {
        User currentUser = getCurrentUser();
        return applicationRepository.findById(id).map(app -> {
            Job job = jobRepository.findById(app.getJobId()).orElse(null);
            if (job == null || job.getPostedBy() == null || !job.getPostedBy().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only update applications for your own jobs");
            }

            ApplicationStatus currentStatus = ApplicationStatus.valueOf(app.getStatus());
            ApplicationStatus newStatus = request.getStatus();

            if (!isValidTransition(currentStatus, newStatus)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Cannot change status from " + currentStatus + " to " + newStatus);
            }

            app.setStatus(newStatus.name());
            Application saved = applicationRepository.save(app);

            String jobTitle = job.getTitle() != null ? job.getTitle() : "job #" + app.getJobId();
            Notification notification = new Notification();
            notification.setUserId(app.getApplicantId());
            notification.setMessage("Your application for '" + jobTitle + "' is now " + newStatus.name());
            notification.setIsRead(false);
            notificationRepository.save(notification);

            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    private boolean isValidTransition(ApplicationStatus current, ApplicationStatus next) {
        if (current == ApplicationStatus.HIRED || current == ApplicationStatus.REJECTED) {
            return false;
        }
        if (current == ApplicationStatus.PENDING) {
            return next == ApplicationStatus.SHORTLISTED || next == ApplicationStatus.REJECTED;
        }
        if (current == ApplicationStatus.SHORTLISTED) {
            return next == ApplicationStatus.HIRED || next == ApplicationStatus.REJECTED;
        }
        return false;
    }

    @PreAuthorize("hasRole('jobseeker')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteApplication(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        return applicationRepository.findById(id).map(app -> {
            if (!app.getApplicantId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only delete your own applications");
            }
            applicationRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}