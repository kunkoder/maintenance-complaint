package ahqpck.maintenance.report.constant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MessageConstant {
    public static final String EXIST_USERNAME_MESSAGE = "An account with the username {username} already exists";
    public static final String EXIST_EMAIL_MESSAGE = "An account with the email {email} already exists";
    public static final String EXIST_DATA_MESSAGE = "Data {value} already exists";
    public static final String NOT_FOUND_DATA_MESSAGE = "Data not found";
	public static final String NOT_FOUND_USERNAME_OR_EMAIL_MESSAGE = "Username or email not found";
    public static final String EXPIRED_TOKEN_MESSAGE = "The token was expired";
    public static final String INACTIVE_ACCOUNT_MESSAGE = "The account has not been activated";
    public static final String ACTIVED_ACCOUNT_MESSAGE = "The account has been activated";
    public static final String FORBIDDEN_REQUEST_MESSAGE = "Different {value} with exist data is forbidden";
    public static final String ACCESS_DENIED_MESSAGE = "Access Denied";
    public static final String INVALID_TOKEN_MESSAGE = "Invalid access token";
    public static final String UNSUPPORTED_FORMAT_MESSAGE = "Unsupported image format";
    public static final String ACCOUNT_ACTIVATION_SUBJECT = "Activate your account";
    public static final String PASSWORD_RESET_SUBJECT = "Reset your password";
    public static final String PASSWORD_RESET_DESCRIPTION = "password reset validation";
    public static final String ACCOUNT_ACTIVATION_DESCRIPTION = "account activation";
    public static final String INVALID_LOGIN_MESSAGE = "Username / Password wrong";
    public static final String EMAIL_VERIFICATION_BODY = getEmailBody("verification.html");

    private static String getEmailBody(String file) {
		try {
			return new String(Files.readString(Path.of(UrlListConstant.EMAIL_TEMPLATE_PATH + file), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}