package com.sakshi.jobportal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createProfile(@Valid @RequestBody ProfileRequestDTO request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        boolean alreadyExists = profileRepository.findAll().stream()
                .anyMatch(p -> currentUser.getId().equals(p.getUserId()));
        if (alreadyExists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Profile already exists for this user. Use update instead.");
        }

        Profile profile = new Profile();
        profile.setResumePath(request.getResumePath());
        profile.setSkills(request.getSkills());
        profile.setBio(request.getBio());
        profile.setFullName(request.getFullName());
        profile.setExperience(request.getExperience());
        profile.setCurrentLocation(request.getCurrentLocation());
        profile.setExpectedSalary(request.getExpectedSalary());
        profile.setEducation(request.getEducation());
        profile.setUserId(currentUser.getId());
        Profile saved = profileRepository.save(profile);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        Profile profile = profileRepository.findAll().stream()
                .filter(p -> currentUser.getId().equals(p.getUserId()))
                .findFirst()
                .orElse(null);

        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile not found.");
        }
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getProfileByUserId(@PathVariable Long userId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        boolean isEmployer = "employer".equals(currentUser.getRole());
        boolean isOwnProfile = currentUser.getId().equals(userId);

        if (!isEmployer && !isOwnProfile) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only view your own profile.");
        }

        Profile profile = profileRepository.findAll().stream()
                .filter(p -> userId.equals(p.getUserId()))
                .findFirst()
                .orElse(null);

        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile not found.");
        }
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @Valid @RequestBody ProfileRequestDTO request) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        Profile existing = profileRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile not found.");
        }

        if (!currentUser.getId().equals(existing.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are not allowed to edit this profile.");
        }

        existing.setResumePath(request.getResumePath());
        existing.setSkills(request.getSkills());
        existing.setBio(request.getBio());
        existing.setFullName(request.getFullName());
        existing.setExperience(request.getExperience());
        existing.setCurrentLocation(request.getCurrentLocation());
        existing.setExpectedSalary(request.getExpectedSalary());
        existing.setEducation(request.getEducation());
        Profile saved = profileRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        Profile existing = profileRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile not found.");
        }

        if (!currentUser.getId().equals(existing.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are not allowed to delete this profile.");
        }

        profileRepository.delete(existing);
        return ResponseEntity.ok("Profile deleted successfully.");
    }
}