package ahqpck.maintenance.report.repository;

import ahqpck.maintenance.report.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, String> {
    List<Complaint> findByStatus(Complaint.Status status);
    List<Complaint> findByMachine(String machine);
}