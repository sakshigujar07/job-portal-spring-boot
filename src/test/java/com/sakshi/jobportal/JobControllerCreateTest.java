package com.sakshi.jobportal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class JobControllerCreateTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private JobRepository jobRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CompanyRepository companyRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private User buildUser(String email, String role) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setRole(role);
        user.setName("Test User");
        return user;
    }

    private String validJobJson() {
        return """
            {
                "title": "Backend Developer",
                "description": "Spring Boot role",
                "companyName": "TechNova",
                "location": "Pune",
                "salary": "600000"
            }
            """;
    }

    @Test
    void createJob_asEmployer_returnsOk() throws Exception {
        when(userRepository.findByEmail("employer@test.com"))
                .thenReturn(Optional.of(buildUser("employer@test.com", "employer")));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/jobs")
                        .with(user("employer@test.com").roles("employer"))
                        .contentType("application/json")
                        .content(validJobJson()))
                .andExpect(status().isOk());
    }

    @Test
    void createJob_asJobseeker_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .with(user("jobseeker@test.com").roles("jobseeker"))
                        .contentType("application/json")
                        .content(validJobJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createJob_withoutAuthentication_returnsUnauthorizedOrForbidden() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType("application/json")
                        .content(validJobJson()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createJob_withBlankTitle_returnsBadRequest() throws Exception {
        String invalidJson = """
            {
                "title": "",
                "description": "Spring Boot role",
                "companyName": "TechNova",
                "location": "Pune",
                "salary": "600000"
            }
            """;

        mockMvc.perform(post("/api/jobs")
                        .with(user("employer@test.com").roles("employer"))
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}