package backend.cowrite.entity;

import backend.cowrite.common.Role;
import backend.cowrite.exception.CustomException;
import backend.cowrite.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime BIRTH = LocalDateTime.of(1995, 3, 15, 0, 0);

    // ─────────────────────────────────────────────────────────────
    // registerUser()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("registerUser() 팩토리 메서드")
    class RegisterUser {

        @Test
        @DisplayName("모든 필드가 올바르게 설정된 User를 생성한다")
        void registerUser_setsAllFields() {
            User user = User.registerUser(USER_ID, "user01", "pw", "nick", BIRTH, "a@b.com", "010-0000-0000");

            assertThat(user.getUserId()).isEqualTo(USER_ID);
            assertThat(user.getUsername()).isEqualTo("user01");
            assertThat(user.getPassword()).isEqualTo("pw");
            assertThat(user.getNickname()).isEqualTo("nick");
            assertThat(user.getBirth()).isEqualTo(BIRTH);
            assertThat(user.getEmail()).isEqualTo("a@b.com");
            assertThat(user.getPhoneNumber()).isEqualTo("010-0000-0000");
        }

        @Test
        @DisplayName("기본 Role은 ROLE_USER다")
        void registerUser_defaultRoleIsUser() {
            User user = User.registerUser(USER_ID, "user01", "pw", "nick", BIRTH, "a@b.com", "010-0000-0000");
            assertThat(user.getRole()).isEqualTo(Role.ROLE_USER);
        }

        @Test
        @DisplayName("passwordConfirm 포함 오버로드도 동일하게 동작한다")
        void registerUser_withConfirm_sameResult() {
            User user = User.registerUser(USER_ID, "user01", "pw", "pw", "nick", BIRTH, "a@b.com", "010-0000-0000");
            assertThat(user.getUsername()).isEqualTo("user01");
            assertThat(user.getPassword()).isEqualTo("pw");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // checkPasswordConfirm()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("checkPasswordConfirm() 비밀번호 확인")
    class CheckPasswordConfirm {

        @Test
        @DisplayName("비밀번호와 확인이 일치하면 예외가 발생하지 않는다")
        void checkPasswordConfirm_matching_noException() {
            User.checkPasswordConfirm("password123", "password123");
            // no exception
        }

        @Test
        @DisplayName("비밀번호와 확인이 다르면 PASSWORD_CONFIRM_DENIED 예외 발생")
        void checkPasswordConfirm_mismatch_throwsException() {
            assertThatThrownBy(() -> User.checkPasswordConfirm("password123", "wrong"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PASSWORD_CONFIRM_DENIED));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // changePassword()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("changePassword() 비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("비밀번호가 일치하면 변경에 성공한다")
        void changePassword_matching_updatesPassword() {
            User user = User.registerUser(USER_ID, "user01", "oldPw", "nick", BIRTH, "a@b.com", "010-0000-0000");
            user.changePassword("newPw", "newPw");
            assertThat(user.getPassword()).isEqualTo("newPw");
        }

        @Test
        @DisplayName("비밀번호가 다르면 PASSWORD_CONFIRM_DENIED 예외 발생 후 변경되지 않는다")
        void changePassword_mismatch_throwsAndNoChange() {
            User user = User.registerUser(USER_ID, "user01", "oldPw", "nick", BIRTH, "a@b.com", "010-0000-0000");

            assertThatThrownBy(() -> user.changePassword("newPw", "wrongPw"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PASSWORD_CONFIRM_DENIED));

            assertThat(user.getPassword()).isEqualTo("oldPw");
        }
    }
}
