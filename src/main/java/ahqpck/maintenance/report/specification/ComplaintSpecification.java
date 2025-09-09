package ahqpck.maintenance.report.specification;

import ahqpck.maintenance.report.entity.Complaint;
import ahqpck.maintenance.report.entity.Area;
import ahqpck.maintenance.report.entity.Equipment;
import ahqpck.maintenance.report.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;

public class ComplaintSpecification {

    public static Specification<Complaint> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            // Use LEFT JOIN
            Join<Complaint, User> reporter = root.join("reporter", JoinType.LEFT);
            Join<Complaint, User> assignee = root.join("assignee", JoinType.LEFT);
            Join<Complaint, Area> area = root.join("area", JoinType.LEFT);
            Join<Complaint, Equipment> equipment = root.join("equipment", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("subject")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("status").as(String.class)), pattern),
                    cb.like(cb.lower(root.get("priority").as(String.class)), pattern),
                    cb.like(cb.lower(root.get("category").as(String.class)), pattern),

                    // Use coalesce to handle nulls in joined fields
                    cb.like(cb.lower(cb.coalesce(reporter.<String>get("name"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(reporter.<String>get("employeeId"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(reporter.<String>get("email"), "")), pattern),

                    cb.like(cb.lower(cb.coalesce(assignee.<String>get("name"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(assignee.<String>get("employeeId"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(assignee.<String>get("email"), "")), pattern),

                    cb.like(cb.lower(cb.coalesce(area.<String>get("name"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(area.<String>get("code"), "")), pattern),

                    cb.like(cb.lower(cb.coalesce(equipment.<String>get("name"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(equipment.<String>get("code"), "")), pattern));
        };
    }

    public static Specification<Complaint> withReportDateRange(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("reportDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("reportDate"), to));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}