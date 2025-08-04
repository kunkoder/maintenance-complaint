package ahqpck.maintenance.report.specification;

import ahqpck.maintenance.report.entity.Part;
import org.springframework.data.jpa.domain.Specification;

public class PartSpecification {

    public static Specification<Part> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("code")), likePattern),
                cb.like(cb.lower(root.get("name")), likePattern),
                cb.like(cb.lower(root.get("category")), likePattern),
                cb.like(cb.lower(root.get("supplier")), likePattern)
            );
        };
    }

    public static Specification<Part> hasCode(String code) {
        return (root, query, cb) -> {
            if (code == null || code.trim().isEmpty()) return cb.conjunction();
            return cb.equal(root.get("code"), code);
        };
    }
}