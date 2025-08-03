package ahqpck.maintenance.report.repository;

import ahqpck.maintenance.report.entity.Part;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PartRepository extends JpaRepository<Part, String>, JpaSpecificationExecutor<Part> {

    Optional<Part> findByCode(String code);
    // Check if a part with this code already exists (ignoring case)
    boolean existsByCodeIgnoreCase(String code);

    // Optional: For update case, exclude current ID
    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);
}