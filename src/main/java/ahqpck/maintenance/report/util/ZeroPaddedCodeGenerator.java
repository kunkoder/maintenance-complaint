package ahqpck.maintenance.report.util;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Component
public class ZeroPaddedCodeGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Generates the next zero-padded code for a given entity and prefix.
     *
     * @param entityClass The JPA entity class (e.g., Complaint.class)
     * @param codeField   The name of the field representing the code (e.g., "code")
     * @param prefix      The prefix to filter by (e.g., "CP", "WO")
     * @return Next code in format: {prefix}{6-digit number}, e.g., CP000001
     */
    public String generate(Class<?> entityClass, String codeField, String prefix) {
        String jpql = "SELECT MAX(e." + codeField + ") " +
                      "FROM " + entityClass.getSimpleName() + " e " +
                      "WHERE e." + codeField + " LIKE :prefixPattern";

        try {
            TypedQuery<String> query = entityManager.createQuery(jpql, String.class);
            query.setParameter("prefixPattern", prefix + "%");

            String maxCode = query.getSingleResult();
            int nextNumber = 1;

            if (maxCode != null && maxCode.length() > prefix.length()) {
                try {
                    nextNumber = Integer.parseInt(maxCode.substring(prefix.length())) + 1;
                } catch (NumberFormatException e) {
                    // If parsing fails, fall back to 1
                }
            }

            return prefix + String.format("%06d", nextNumber);
        } catch (IllegalArgumentException | NoResultException e) {
            // Handle invalid query or no results gracefully
            return prefix + "000001";
        }
    }
}