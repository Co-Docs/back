package backend.cowrite.service.request;

public record LoginRequest(
        String username,
        String password
) {
}
