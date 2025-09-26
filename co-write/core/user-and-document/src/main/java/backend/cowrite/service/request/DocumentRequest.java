package backend.cowrite.service.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DocumentRequest(
        @NotNull String title,
        @NotNull String content,
        @NotEmpty List<String> userDocuments
) {
}
