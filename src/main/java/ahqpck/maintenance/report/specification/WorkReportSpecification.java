package ahqpck.maintenance.report.specification;

import ahqpck.maintenance.report.entity.WorkReport;
import ahqpck.maintenance.report.entity.Area;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.entity.User;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;

public class WorkReportSpecification {

    public static Specification<WorkReport> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction(); // no filter
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            // LEFT JOINs to related entities
            Join<WorkReport, Equipment> equipment = root.join("equipment", JoinType.LEFT);
            Join<WorkReport, Area> area = root.join("area", JoinType.LEFT);
            Join<WorkReport, User> technician = root.join("technician", JoinType.LEFT);
            Join<WorkReport, User> supervisor = root.join("supervisor", JoinType.LEFT);

            return cb.or(
                // Search in WorkReport fields
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("problem")), pattern),
                cb.like(cb.lower(root.get("solution")), pattern),
                cb.like(cb.lower(root.get("workType")), pattern),
                cb.like(cb.lower(root.get("remark")), pattern),
                cb.like(cb.lower(root.get("status").as(String.class)), pattern),
                cb.like(cb.lower(root.get("category").as(String.class)), pattern),
                
                // Search in Equipment (via join)
                cb.like(cb.lower(cb.coalesce(equipment.<String>get("name"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(equipment.<String>get("code"), "")), pattern),

                // Search in Area (via join)
                cb.like(cb.lower(cb.coalesce(area.<String>get("name"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(area.<String>get("code"), "")), pattern),

                // Search in Technician
                cb.like(cb.lower(cb.coalesce(technician.<String>get("name"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(technician.<String>get("employeeId"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(technician.<String>get("email"), "")), pattern),

                // Search in Supervisor
                cb.like(cb.lower(cb.coalesce(supervisor.<String>get("name"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(supervisor.<String>get("employeeId"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(supervisor.<String>get("email"), "")), pattern)
            );
        };
    }
}