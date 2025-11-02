package backend.cowrite.publisher.request;

import jakarta.validation.constraints.NotNull;

public record ParticipantsUpdateRequest(
        @NotNull String username
) {
}
