package com.sakshi.jobportal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyById(@PathVariable Long id) {
        return companyRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Company not found"));
    }

    @PreAuthorize("hasRole('employer')")
    @GetMapping("/my")
    public ResponseEntity<?> getMyCompany() {
        User currentUser = getCurrentUser();
        return companyRepository.findByOwnerId(currentUser.getId())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("No company found for this employer"));
    }

    @PreAuthorize("hasRole('employer')")
    @PostMapping
    public ResponseEntity<?> createCompany(@Valid @RequestBody CompanyRequestDTO request) {
        User currentUser = getCurrentUser();

        if (companyRepository.findByOwnerId(currentUser.getId()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("You already have a company. Use edit instead.");
        }

        Company company = new Company();
        company.setName(request.getName());
        company.setDescription(request.getDescription());
        company.setWebsite(request.getWebsite());
        company.setLocation(request.getLocation());
        company.setLogoUrl(request.getLogoUrl());
        company.setOwnerId(currentUser.getId());
        company.setCreatedDate(LocalDateTime.now());

        return ResponseEntity.ok(companyRepository.save(company));
    }

    @PreAuthorize("hasRole('employer')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompany(@PathVariable Long id, @Valid @RequestBody CompanyRequestDTO request) {
        User currentUser = getCurrentUser();
        return companyRepository.findById(id).map(company -> {
            if (!company.getOwnerId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You can only edit your own company");
            }
            company.setName(request.getName());
            company.setDescription(request.getDescription());
            company.setWebsite(request.getWebsite());
            company.setLocation(request.getLocation());
            company.setLogoUrl(request.getLogoUrl());

            return ResponseEntity.ok(companyRepository.save(company));
        }).orElse(ResponseEntity.notFound().build());
    }
}