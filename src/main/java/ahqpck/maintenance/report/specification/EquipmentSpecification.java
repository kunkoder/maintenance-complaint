package ahqpck.maintenance.report.specification;

import ahqpck.maintenance.report.entity.Equipment;
import org.springframework.data.jpa.domain.Specification;

public class EquipmentSpecification {

    public static Specification<Equipment> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%" + keyword.toLowerCase() + "%";

            return cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("model")), pattern),
                cb.like(cb.lower(root.get("manufacturer")), pattern),
                cb.like(cb.lower(root.get("serialNo")), pattern),
                cb.like(cb.lower(root.get("capacity")), pattern),
                cb.like(cb.lower(root.get("remarks")), pattern)
            );
        };
    }
}