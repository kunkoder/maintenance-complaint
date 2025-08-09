package ahqpck.maintenance.report.specification;

import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.Area;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.entity.User;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaBuilder;

public class ComplaintSpecification {

    public static Specification<Complaint> search(String keyword) {
        return (Root<Complaint> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction(); // no condition (matches all)
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            // Join relationships
            Join<Complaint, User> reporter = root.join("reporter");
            Join<Complaint, User> assignee = root.join("assignee");
            Join<Complaint, Area> area = root.join("area");
            Join<Complaint, Equipment> equipment = root.join("equipment");

            return cb.or(
                // Search in Complaint fields
                cb.like(cb.lower(root.get("subject")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("status").as(String.class)), pattern),
                cb.like(cb.lower(root.get("priority").as(String.class)), pattern),
                cb.like(cb.lower(root.get("category").as(String.class)), pattern),

                // Search by Reporter
                cb.like(cb.lower(reporter.get("name")), pattern),
                cb.like(cb.lower(reporter.get("employeeId")), pattern),
                cb.like(cb.lower(reporter.get("email")), pattern),

                // Search by Assignee
                cb.like(cb.lower(assignee.get("name")), pattern),
                cb.like(cb.lower(assignee.get("employeeId")), pattern),
                cb.like(cb.lower(assignee.get("email")), pattern),

                // Search by Area
                cb.like(cb.lower(area.get("code")), pattern),
                cb.like(cb.lower(area.get("name")), pattern),

                // Search by Equipment
                cb.like(cb.lower(equipment.get("name")), pattern),
                cb.like(cb.lower(equipment.get("code")), pattern)
            );
        };
    }
}