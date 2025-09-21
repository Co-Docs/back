package backend.cowrite.service.request;

import java.util.List;

public record DocumentUpdateRequest(
        String title,
        String content
) {
}

