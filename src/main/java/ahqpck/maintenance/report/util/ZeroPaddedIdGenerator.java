package ahqpck.maintenance.report.util;

import java.util.concurrent.ConcurrentHashMap;

public class ZeroPaddedIdGenerator {

    private static final int LENGTH = 6; // e.g., 000001
    private static final ConcurrentHashMap<String, Long> counters = new ConcurrentHashMap<>();

    /**
     * Generate a zero-padded ID with the given prefix.
     * Each prefix has its own counter starting at 1.
     *
     * @param prefix e.g., "COM", "USR", "MACHINE"
     * @return e.g., COM-000001
     */
    public static synchronized String generate(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Prefix cannot be null or empty");
        }
        String key = prefix.trim();
        long nextId = counters.getOrDefault(key, 0L) + 1;
        counters.put(key, nextId);
        return key + String.format("%0" + LENGTH + "d", nextId);
    }

    /**
     * Reset or set the counter for a specific prefix.
     * For testing or admin use.
     */
    public static synchronized void setCounter(String prefix, long value) {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Prefix cannot be null or empty");
        }
        counters.put(prefix.trim(), value > 0 ? value : 1);
    }

    /**
     * Get current counter value for a prefix (for debugging)
     */
    public static long getCounter(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) return 1;
        return counters.getOrDefault(prefix.trim(), 1L);
    }
}