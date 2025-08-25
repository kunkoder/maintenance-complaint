package ahqpck.maintenance.report.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final String INACTIVE_MESSAGE = "not active";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException exception) throws IOException {

        String url = "/login?error";

        // If the failure was due to inactive account, use ?inactive
        if (exception.getMessage() != null &&
            exception.getMessage().toLowerCase().contains(INACTIVE_MESSAGE)) {
            url = "/login?inactive";
        }

        // Perform redirect
        response.sendRedirect(url);
    }
}