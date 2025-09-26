package backend.cowrite.service.request;

import jakarta.validation.constraints.NotNull;

public record FindPwRequest1(
        @NotNull String username,
        @NotNull String email
) {
}
