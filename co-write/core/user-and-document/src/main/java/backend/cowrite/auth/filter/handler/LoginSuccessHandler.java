package backend.cowrite.auth.filter.handler;

import backend.cowrite.auth.CustomUserDetails;
import backend.cowrite.auth.TokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String jwt = tokenProvider.createToken(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        log.info("cacheUserNickname = {}, cacheUserUsername = {}", userDetails.getName(), userDetails.getUsername());
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("username", userDetails.getUsername());
        responseBody.put("nickname", userDetails.getName());
        responseBody.put("accessToken", jwt);
        new ObjectMapper().writeValue(response.getWriter(), responseBody);
        response.getWriter().flush();
    }
}
