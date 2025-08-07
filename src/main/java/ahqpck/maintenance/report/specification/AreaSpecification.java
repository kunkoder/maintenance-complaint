package ahqpck.maintenance.report.specification;

import ahqpck.maintenance.report.entity.Area;
import ahqpck.maintenance.report.entity.User;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;

public class AreaSpecification {

    public static Specification<Area> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction(); // no condition
            }

            String pattern = "%" + keyword.toLowerCase() + "%";

            Join<Area, User> responsiblePerson = root.join("responsiblePerson");

            return cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("status").as(String.class)), pattern),
                cb.like(cb.lower(responsiblePerson.get("name")), pattern) // Search by responsible person's name
            );
        };
    }
}
