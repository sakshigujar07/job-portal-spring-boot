package com.sakshi.jobportal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Covers the login endpoint (POST /api/users/login), the most critical path in the app
// since every other feature depends on it. @WebMvcTest loads only the web layer, so
// UserRepository / PasswordEncoder / JwtUtil / JwtFilter are mocked rather than real —
// this test checks UserController's login logic in isolation, not the DB or JWT internals.
//
// Notes on this project's Spring Boot 4.x setup:
// - @MockBean was removed in favor of @MockitoBean (from spring-test directly).
// - @WebMvcTest / @AutoConfigureMockMvc moved to org.springframework.boot.webmvc.test.autoconfigure.
// - ObjectMapper is created directly (new ObjectMapper()) instead of @Autowired, because this
//   project has both Jackson 2 (com.fasterxml.jackson) and Jackson 3 (tools.jackson) on the
//   classpath, which left no autoconfigured ObjectMapper bean available inside the @WebMvcTest
//   slice. A plain instance sidesteps that conflict; we only need it to serialize request bodies.
// - addFilters = false: JwtFilter is a real servlet Filter wired into SecurityConfig, so it must
//   stay a real bean for that config to build. But mocking it with @MockitoBean makes it a no-op
//   that never calls the rest of the filter chain, so every request got silently swallowed before
//   reaching the controller (visible as "Handler: Type = null" in the MockMvc result). Since the
//   login endpoint is permitAll and this test isn't exercising security filters anyway, disabling
//   filter registration in MockMvc is the correct fix rather than trying to make the mock forward
//   the request itself.
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerLoginTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtFilter jwtFilter;

    @Test
    void login_withCorrectCredentials_returnsTokenAndOk() throws Exception {
        User existingUser = new User();
        existingUser.setEmail("test@example.com");
        existingUser.setPassword("hashed-password");
        existingUser.setRole("jobseeker");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correct-password", "hashed-password"))
                .thenReturn(true);
        when(jwtUtil.generateToken("test@example.com", "jobseeker"))
                .thenReturn("fake-jwt-token");

        User loginRequest = new User();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("correct-password");

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("fake-jwt-token"));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        User existingUser = new User();
        existingUser.setEmail("test@example.com");
        existingUser.setPassword("hashed-password");
        existingUser.setRole("jobseeker");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password"))
                .thenReturn(false);

        User loginRequest = new User();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("wrong-password");

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));
    }

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        User loginRequest = new User();
        loginRequest.setEmail("nobody@example.com");
        loginRequest.setPassword("whatever");

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));
    }
}