package backend.cowrite.service.request;

import java.util.List;

public record ParticipantsUpdateRequest(
        List<Long> userId
) {
}
