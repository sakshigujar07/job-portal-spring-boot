package com.sakshi.jobportal;

import jakarta.validation.constraints.NotBlank;

public class CompanyRequestDTO {

    @NotBlank(message = "Company name is required")
    private String name;

    private String description;
    private String website;
    private String location;
    private String logoUrl;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
}