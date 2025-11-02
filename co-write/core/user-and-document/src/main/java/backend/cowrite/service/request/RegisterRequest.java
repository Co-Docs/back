package backend.cowrite.publisher.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RegisterRequest(
        @NotNull String username,
        @NotNull String password,
        @NotNull String passwordConfirm,
        @NotNull String nickname,
        @NotNull LocalDateTime birth,
        @NotNull String email,
        @NotNull String phoneNumber
) {
}
