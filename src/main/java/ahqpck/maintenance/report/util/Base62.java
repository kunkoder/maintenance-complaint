package ahqpck.maintenance.report.util;

import java.math.BigInteger;
import java.util.UUID;

public class Base62 {
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();

    public static String encode(UUID uuid) {
        BigInteger number = new BigInteger(1, uuidToBytes(uuid));
        StringBuilder sb = new StringBuilder();
        while (number.compareTo(BigInteger.ZERO) > 0) {
            sb.append(ALPHABET.charAt(number.mod(BigInteger.valueOf(BASE)).intValue()));
            number = number.divide(BigInteger.valueOf(BASE));
        }
        // Pad to 22 chars (typical Base62 UUID length)
        while (sb.length() < 22) {
            sb.append('0');
        }
        return sb.reverse().toString();
    }

    // Helper to convert UUID to byte[]
    private static byte[] uuidToBytes(UUID uuid) {
        byte[] byteArray = new byte[16];
        longToBytes(uuid.getMostSignificantBits(), byteArray, 0);
        longToBytes(uuid.getLeastSignificantBits(), byteArray, 8);
        return byteArray;
    }

    private static void longToBytes(long value, byte[] array, int offset) {
        for (int i = 7; i >= 0; i--) {
            array[offset + i] = (byte) (value & 0xFF);
            value >>>= 8;
        }
    }
}