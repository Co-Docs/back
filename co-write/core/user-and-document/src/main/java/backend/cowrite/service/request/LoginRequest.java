package backend.cowrite.publisher.request;

public record LoginRequest(
        String username,
        String password
) {
}
