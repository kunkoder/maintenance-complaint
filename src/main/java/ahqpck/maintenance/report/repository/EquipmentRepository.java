package ahqpck.maintenance.report.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import ahqpck.maintenance.report.entity.Equipment;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, String>, JpaSpecificationExecutor<Equipment> {

    Optional<Equipment> findByCode(String code);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);
}