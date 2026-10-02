package com.sakshi.jobportal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    // TEMPORARY - manual create for testing only.
    // Later this will be replaced by automatic creation from JobController / ApplicationController.
    @PostMapping("/create")
    public ResponseEntity<?> createNotification(@RequestBody Notification notification) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        notification.setUserId(currentUser.getId());
        if (notification.getIsRead() == null) {
            notification.setIsRead(false);
        }
        Notification saved = notificationRepository.save(notification);
        return ResponseEntity.ok(saved);
    }

    // Get all notifications belonging to the logged-in user
    @GetMapping("/me")
    public ResponseEntity<?> getMyNotifications() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        List<Notification> myNotifications = notificationRepository.findAll().stream()
                .filter(n -> currentUser.getId().equals(n.getUserId()))
                .toList();

        return ResponseEntity.ok(myNotifications);
    }

    // Mark a notification as read - only the owner can do this
    @PutMapping("/read/{id}")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        Notification existing = notificationRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Notification not found.");
        }

        if (!currentUser.getId().equals(existing.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are not allowed to modify this notification.");
        }

        existing.setIsRead(true);
        Notification saved = notificationRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    // Delete a notification - only the owner can do this
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        Notification existing = notificationRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Notification not found.");
        }

        if (!currentUser.getId().equals(existing.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are not allowed to delete this notification.");
        }

        notificationRepository.delete(existing);
        return ResponseEntity.ok("Notification deleted successfully.");
    }
}