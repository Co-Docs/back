package backend.cowrite.service.request;

import java.util.List;

public record DocumentRequest(
        String title,
        String content,
        List<String> userDocuments
) {
}
