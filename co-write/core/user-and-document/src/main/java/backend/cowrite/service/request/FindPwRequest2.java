package backend.cowrite.service.request;

public record FindPwRequest2(
        String username,
        String newPassword,
        String newPasswordConfirm
) {
}
