package backend.cowrite.service.dto;

public record EditedResult(
        String editedContent,
        Long version
) {
}
