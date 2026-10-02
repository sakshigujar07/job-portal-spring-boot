package com.sakshi.jobportal;

public class UserResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String role;
    private String contact;
    private String address;

    public UserResponseDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.contact = user.getContact();
        this.address = user.getAddress();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getContact() {
        return contact;
    }

    public String getAddress() {
        return address;
    }
}