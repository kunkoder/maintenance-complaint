package ahqpck.maintenance.report.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import ahqpck.maintenance.report.entity.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String id;
    private String name;
    private String employeeId;
    private String email;
    private String avatar;
    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;
    private String activationToken;
    private String passwordResetToken;
    private LocalDateTime passwordResetTokenExpiry;
    private User.Status status;
    private Set<RoleDTO> roles = new HashSet<>();
}