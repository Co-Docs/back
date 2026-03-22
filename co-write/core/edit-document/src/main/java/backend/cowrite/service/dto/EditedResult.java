package backend.cowrite.service.dto;

public record EditedResult(
        String editedContent,
        Long version,
        int targetPosition,
        String operationId
) {
}
