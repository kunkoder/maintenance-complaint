package ahqpck.maintenance.report.constant;

public class UrlListConstant {
    public static final String[] WHITE_LIST_URL = {
        "/",
        "/user/apaansih",
        "/user/**",
        "/index.html",
        "/coba.html",
        "/email/**",
        "/assets/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/api-docs/**"
    };

    public static final String AVATAR_PATH = "src/main/resources/static/avatar";
    public static final String EMAIL_TEMPLATE_PATH = "src/main/resources/static/email/";
    public static final String VERIFICATION_URL = "%s/user/%s?email=%s&token=%s";
    // public static final String ACCOUNT_ACTIVATION_URL = "%s/user/account-activation?email=%s&token=%s";
    // public static final String PASSWORD_RESET_URL = "%s/user/password-reset-validation?email=%s&token=%s";
}
