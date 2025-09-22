package backend.cowrite.service.request;

import java.time.LocalDateTime;

public record RegisterRequest(
        String username,
        String password,
        String passwordConfirm,
        String nickname,
        LocalDateTime birth,
        String email,
        String phoneNumber
) {
}
