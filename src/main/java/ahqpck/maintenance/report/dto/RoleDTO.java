package ahqpck.maintenance.report.dto;

import ahqpck.maintenance.report.entity.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {
    private String id;

    @NotBlank(message = "Name is mandatory")
    private Role.Name name;

    public RoleDTO(Role role) {
        this.id = role.getId();
        this.name = role.getName(); 
    }
}