package in.gram.gov.app.egram_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UserRequestDTO {
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phone;

    @Size(max = 100, message = "Designation must not exceed 100 characters")
    private String designation;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // Image can be provided as file or URL
    private MultipartFile imageFile;
    private String imageUrl;
    private String compressionQuality;
}

