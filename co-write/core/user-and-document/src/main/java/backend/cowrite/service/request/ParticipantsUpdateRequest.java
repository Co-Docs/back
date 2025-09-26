package backend.cowrite.service.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ParticipantsUpdateRequest(
        @NotEmpty List<Long> userId
) {
}
