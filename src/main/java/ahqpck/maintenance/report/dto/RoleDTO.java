package ahqpck.maintenance.report.dto;

import ahqpck.maintenance.report.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class RoleDTO {
    private String id;
    private Role.Name name;
}