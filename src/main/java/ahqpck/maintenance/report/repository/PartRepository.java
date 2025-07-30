package ahqpck.maintenance.report.repository;

import ahqpck.maintenance.report.entity.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, String> {
    Optional<Part> findByCode(String code);
    boolean existsByCode(String code);
}