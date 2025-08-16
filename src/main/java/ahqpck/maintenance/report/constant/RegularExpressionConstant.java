package ahqpck.maintenance.report.constant;

import java.util.Arrays;
import java.util.stream.Collectors;

public class RegularExpressionConstant {
    public static final String NUMERIC = "\\d+";
    public static final String ALPHANUMERIC = "^[a-zA-Z0-9]+$";
    public static final String ALPHABET = "^[a-zA-Z]+$";
    public static final String ALPHANUMERIC_WITH_DOT_AND_SPACE = "^[a-zA-Z0-9.' ]+$";
    public static final String ROLE_ENUM = "^ROLE_(ADMIN|STAFF|PUBLIC)$";
    public static final String GENDER_ENUM = "^(LAKI_LAKI|PEREMPUAN)$";
    public static final String STATUS_ENUM = "^(AKTIF|LULUS|KELUAR)$";
    public static final String SCHOOL_GRADE_ENUM = "^(SMP|SMA)$";
    public static final String SCORE_GRADE_ENUM = "^(A|B|C|D|E)$";
    public static final String LEVEL_ENUM = "^(SATU|DUA|TIGA|IDAD)$";
    public static final String DEGREE_ENUM = "^(SMP|SMA|D3|S1|S2|S3)$";
    public static final String SUBJECT_TYPE_ENUM = "^(KEPONDOKAN|UMUM|SIKAP|KETERAMPILAN)$";
    public static final String EXAM_TYPE_ENUM = "^(BULANAN|UTS|UAS|NASIONAL|REMEDIAL)$";
    public static final String PERIODE_ENUM = "^(GANJIL|GENAP)$";
    public static final String ACADEMIC_YEAR_ENUM = "^\\d{4}/\\d{4}$";

    public static <T extends Enum<T>> String convertEnumToString(Class<T> e) {
        String regex = "^(" + Arrays.stream(e.getEnumConstants()).map(Enum::name).collect(Collectors.joining("|")) + ")$";
        return regex;
    }
}
