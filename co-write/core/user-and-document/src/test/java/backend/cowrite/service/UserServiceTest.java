package backend.cowrite.service;

import backend.cowrite.common.Role;
import backend.cowrite.common.snowflake.Snowflake;
import backend.cowrite.entity.User;
import backend.cowrite.exception.CustomException;
import backend.cowrite.exception.ErrorCode;
import backend.cowrite.repository.UserRepository;
import backend.cowrite.service.response.UserCacheDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private Snowflake snowflake;
    @Mock private BCryptPasswordEncoder bCryptPasswordEncoder;

    private UserService userService;

    private static final LocalDateTime BIRTH = LocalDateTime.of(1995, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, snowflake, bCryptPasswordEncoder);
    }

    private User createUser(Long id, String username) {
        return User.registerUser(id, username, "encodedPw", "nick", BIRTH, "a@b.com", "010-0000-0000");
    }

    // ─────────────────────────────────────────────────────────────
    // findById()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("존재하는 userId면 User를 반환한다")
        void findById_found_returnsUser() {
            User user = createUser(1L, "user01");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            User result = userService.findById(1L);
            assertThat(result.getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않으면 USER_NOT_FOUND 예외 발생")
        void findById_notFound_throwsException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(99L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findByName()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findByName()")
    class FindByName {

        @Test
        @DisplayName("존재하는 nickname이면 User를 반환한다")
        void findByName_found_returnsUser() {
            User user = createUser(1L, "user01");
            when(userRepository.findByName("nick")).thenReturn(Optional.of(user));

            User result = userService.findByName("nick");
            assertThat(result).isEqualTo(user);
        }

        @Test
        @DisplayName("존재하지 않으면 USER_NOT_FOUND 예외 발생")
        void findByName_notFound_throwsException() {
            when(userRepository.findByName("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByName("unknown"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // register()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("비밀번호가 인코딩된 채로 저장되고 userId를 반환한다")
        void register_encodesPasswordAndReturnsId() {
            when(snowflake.nextId()).thenReturn(42L);
            when(bCryptPasswordEncoder.encode("rawPw")).thenReturn("encoded");
            User savedUser = createUser(42L, "user01");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            Long userId = userService.register("user01", "rawPw", "rawPw", "nick", BIRTH, "a@b.com", "010");

            assertThat(userId).isEqualTo(42L);
            verify(bCryptPasswordEncoder, times(2)).encode("rawPw"); // password + passwordConfirm
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("비밀번호 확인이 다르면 PASSWORD_CONFIRM_DENIED 예외 발생")
        void register_passwordMismatch_throwsException() {
            assertThatThrownBy(() ->
                    userService.register("user01", "rawPw", "wrong", "nick", BIRTH, "a@b.com", "010")
            )
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PASSWORD_CONFIRM_DENIED));

            verify(userRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findByUsernameNoCache()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findByUsernameNoCache()")
    class FindByUsernameNoCache {

        @Test
        @DisplayName("존재하는 username이면 User를 반환한다")
        void findByUsernameNoCache_found_returnsUser() {
            User user = createUser(1L, "user01");
            when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));

            User result = userService.findByUsernameNoCache("user01");
            assertThat(result.getUsername()).isEqualTo("user01");
        }

        @Test
        @DisplayName("존재하지 않으면 USER_NOT_FOUND 예외 발생")
        void findByUsernameNoCache_notFound_throwsException() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByUsernameNoCache("ghost"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // changePassword()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("비밀번호가 일치하면 User의 password가 변경된다")
        void changePassword_matching_updatesUser() {
            User user = createUser(1L, "user01");
            when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));

            userService.changePassword("user01", "newPw", "newPw");

            assertThat(user.getPassword()).isEqualTo("newPw");
        }

        @Test
        @DisplayName("username이 없으면 USER_NOT_FOUND 예외 발생")
        void changePassword_userNotFound_throwsException() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changePassword("ghost", "newPw", "newPw"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("비밀번호 불일치 시 PASSWORD_CONFIRM_DENIED 예외 발생")
        void changePassword_mismatch_throwsException() {
            User user = createUser(1L, "user01");
            when(userRepository.findByUsername("user01")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.changePassword("user01", "newPw", "wrong"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PASSWORD_CONFIRM_DENIED));
        }
    }
}
