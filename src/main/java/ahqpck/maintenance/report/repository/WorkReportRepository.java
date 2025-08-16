package ahqpck.maintenance.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.entity.WorkReport;

import java.util.List;

@Repository
public interface WorkReportRepository extends JpaRepository<WorkReport, String>, JpaSpecificationExecutor<WorkReport> {

    /**
     * Find all work reports by status
     */
    List<WorkReport> findByStatus(WorkReport.Status status);

    /**
     * Find by equipment (useful for equipment history)
     */
    List<WorkReport> findByEquipment(Equipment equipment);

    /**
     * Optional: find by technician
     */
    List<WorkReport> findByTechnician(User technician);

    /**
     * Optional: find by supervisor
     */
    List<WorkReport> findBySupervisor(User supervisor);

    /**
     * Optional: check if a code exists (for uniqueness)
     */
    // boolean existsByCodeIgnoreCase(String code);
}