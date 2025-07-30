package ahqpck.maintenance.report.repository;

import ahqpck.maintenance.report.entity.ComplaintPart;
import ahqpck.maintenance.report.entity.ComplaintPartId;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintPartRepository extends JpaRepository<ComplaintPart, ComplaintPartId> {
}