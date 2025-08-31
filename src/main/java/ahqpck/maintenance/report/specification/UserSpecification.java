package ahqpck.maintenance.report.specification;

import ahqpck.maintenance.report.entity.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction(); // no condition
            }

            String pattern = "%" + keyword.toLowerCase() + "%";

            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("employeeId")), pattern),
                cb.like(cb.lower(root.get("designation")), pattern),
                cb.like(cb.lower(root.get("nationality")), pattern),
                cb.like(cb.lower(root.get("phoneNumber")), pattern),

                // Search by role name (via join)
                cb.like(cb.lower(root.join("roles").get("name").as(String.class)), pattern),

                // Search by status
                cb.like(cb.lower(root.get("status").as(String.class)), pattern)
            );
        };
    }
}