package backend.cowrite.service.request;

import jakarta.validation.constraints.NotNull;

public record DocumentUpdateRequest(
        @NotNull String title,
        @NotNull String content
) {
}

