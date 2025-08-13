package ahqpck.maintenance.report.util;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Component
public class ZeroPaddedCodeGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    public String generate(String prefix) {
        // Query the max code value
        String queryStr = "SELECT MAX(c.code) FROM Complaint c WHERE c.code LIKE :prefixPattern";
        TypedQuery<String> query = entityManager.createQuery(queryStr, String.class);
        query.setParameter("prefixPattern", prefix + "%");

        String maxCode = query.getSingleResult();
        int nextNumber = 1;
        if (maxCode != null) {
            try {
                nextNumber = Integer.parseInt(maxCode.substring(prefix.length())) + 1;
            } catch (NumberFormatException e) {
                nextNumber = 1;
            }
        }

        return prefix + String.format("%06d", nextNumber); // e.g., CP000001
    }
}