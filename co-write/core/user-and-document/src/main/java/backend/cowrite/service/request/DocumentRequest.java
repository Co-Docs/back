package backend.cowrite.publisher.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DocumentRequest(
        @NotNull String title,
        @NotNull String password,
        @NotEmpty List<String> userDocuments
) {
}
