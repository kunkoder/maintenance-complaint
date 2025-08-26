package ahqpck.maintenance.report.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import ahqpck.maintenance.report.entity.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String id;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotBlank(message = "Employee id is mandatory")
    private String employeeId;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    private String email;

    private String password;

    private String image;
    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;
    private String activationToken;
    private String passwordResetToken;
    private LocalDateTime passwordResetTokenExpiry;

    private User.Status status;

    private Set<String> roleNames = new HashSet<>();
    private Set<RoleDTO> roles = new HashSet<>();
}