package backend.cowrite.service.request;

public record FindIdRequest(
        String nickname,
        String email
) {
}
