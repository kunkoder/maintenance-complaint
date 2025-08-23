package ahqpck.maintenance.report.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank(message = "Email or Employee ID is required")
    private String usernameOrEmployeeId;  // Can be email or employeeId

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}