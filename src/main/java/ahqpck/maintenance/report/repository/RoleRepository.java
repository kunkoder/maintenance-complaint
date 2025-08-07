package ahqpck.maintenance.report.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ahqpck.maintenance.report.entity.Role;

public interface RoleRepository extends JpaRepository<Role, String> {
  Optional<Role> findByName(Role.Name name);
}