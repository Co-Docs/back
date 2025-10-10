package backend.cowrite.service.request;

import jakarta.validation.constraints.NotNull;

public record ParticipantsUpdateRequest(
        @NotNull String username
) {
}
