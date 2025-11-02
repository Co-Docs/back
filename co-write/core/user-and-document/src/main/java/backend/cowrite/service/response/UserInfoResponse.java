package backend.cowrite.publisher.response;

import backend.cowrite.entity.User;

import java.time.LocalDateTime;

public record UserInfoResponse(
        String username,
        String nickname,
        LocalDateTime birth,
        String email,
        String phoneNumber
) {
    public static UserInfoResponse of(User user) {
        return new UserInfoResponse(
                user.getUsername(), user.getNickname(), user.getBirth(), user.getEmail(), user.getPhoneNumber()
        );
    }
}
