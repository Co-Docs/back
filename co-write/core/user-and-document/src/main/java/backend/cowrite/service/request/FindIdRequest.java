package backend.cowrite.publisher.request;

import jakarta.validation.constraints.NotNull;

public record FindIdRequest(
        @NotNull String nickname,
        @NotNull String email
) {
}
