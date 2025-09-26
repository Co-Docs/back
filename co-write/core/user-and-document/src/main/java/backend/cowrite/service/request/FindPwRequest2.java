package backend.cowrite.service.request;

import jakarta.validation.constraints.NotNull;

public record FindPwRequest2(
        @NotNull String username,
        @NotNull String newPassword,
        @NotNull String newPasswordConfirm
) {
}
