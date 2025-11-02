package backend.cowrite.publisher.request;

import jakarta.validation.constraints.NotNull;

public record DocumentUpdateRequest(
        @NotNull String title,
        @NotNull String content
) {
}

