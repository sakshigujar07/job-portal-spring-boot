package com.sakshi.jobportal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    public UserResponseDTO createUser(@Valid @RequestBody RegisterRequestDTO request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setContact(request.getContact());
        user.setAddress(request.getAddress());
        User savedUser = userRepository.save(user);
        return new UserResponseDTO(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User loginRequest) {
        return userRepository.findByEmail(loginRequest.getEmail())
                .filter(user -> passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()))
                .map(user -> ResponseEntity.ok(jwtUtil.generateToken(user.getEmail(), user.getRole())))
                .orElse(ResponseEntity.status(401).body("Invalid credentials"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findById(id).map(user -> {
            if (!user.getEmail().equals(currentEmail)) {
                return ResponseEntity.status(403).body("You can only update your own account");
            }
            user.setName(updatedUser.getName());
            user.setEmail(updatedUser.getEmail());
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }
            // Role is intentionally NOT updated here, so users cannot change their own role.
            User savedUser = userRepository.save(user);
            return ResponseEntity.ok(new UserResponseDTO(savedUser));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findById(id).map(user -> {
            if (!user.getEmail().equals(currentEmail)) {
                return ResponseEntity.status(403).body("You can only delete your own account");
            }
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}