package in.gram.gov.app.egram_service.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PanchayatRequestDTO {
    // Optional for partial updates - only required when creating new panchayat
    @Size(max = 200, message = "Panchayat name must not exceed 200 characters")
    private String panchayatName;

    // Optional for partial updates - only required when creating new panchayat
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Size(max = 100, message = "Slug must not exceed 100 characters")
    private String slug;

    // Optional for partial updates - only required when creating new panchayat
    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;

    // Optional for partial updates - only required when creating new panchayat
    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(max = 1000, message = "Address must not exceed 1000 characters")
    private String address;

    // Allow empty or null, but if provided, must be valid format
    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Phone number must be 10 digits or empty")
    private String contactPhone;

    // Allow empty or null, but if provided, must be valid email
    @Pattern(regexp = "^$|^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Invalid email format")
    private String contactEmail;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 10000, message = "About text must not exceed 10000 characters")
    private String aboutText;

    @Size(max = 200, message = "Hero title must not exceed 200 characters")
    private String heroTitle;

    @Size(max = 200, message = "Hero subtitle must not exceed 200 characters")
    private String heroSubtitle;

    @Size(max = 200, message = "About title must not exceed 200 characters")
    private String aboutTitle;

    @Size(max = 5000, message = "About features must not exceed 5000 characters")
    private String aboutFeatures; // JSON array stored as string

    @Size(max = 1000, message = "Office address must not exceed 1000 characters")
    private String officeAddress;

    // Allow empty or null, but if provided, must be valid format
    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Phone number must be 10 digits or empty")
    private String officePhone;

    // Allow empty or null, but if provided, must be valid email
    @Pattern(regexp = "^$|^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Invalid email format")
    private String officeEmail;

    @Size(max = 200, message = "Map coordinates must not exceed 200 characters")
    private String mapCoordinates;

    @Size(max = 500, message = "Office hours must not exceed 500 characters")
    private String officeHours;

    private Long population;

    @Size(max = 50, message = "Area must not exceed 50 characters")
    private String area;

    private Integer wards;

    private Integer establishedYear;

    // New fields for settings
    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String logoUrl;

    @Size(max = 500, message = "Hero image URL must not exceed 500 characters")
    private String heroImageUrl;
}

