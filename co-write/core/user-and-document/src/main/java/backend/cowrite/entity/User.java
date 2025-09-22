package backend.cowrite.entity;

import backend.cowrite.common.BaseEntity;
import backend.cowrite.common.Role;
import backend.cowrite.exception.CustomException;
import backend.cowrite.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id @Column(name = "user_id")
    private Long userId;
    private String username;
    private String password;
    private String nickname;
    private LocalDateTime birth;
    private String email;
    private String phoneNumber;
    private Role role;


    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @BatchSize(size = 20)
    private List<UserDocument> userDocuments = new ArrayList<>();

    public String roleName() {
        return role.name();
    }

    public static User registerUser(Long userId, String username, String password, String nickname, LocalDateTime birth, String email, String phoneNumber){
        User user = new User();
        user.userId = userId;
        user.username = username;
        user.password = password;
        user.nickname = nickname;
        user.birth = birth;
        user.email = email;
        user.phoneNumber = phoneNumber;
        user.role = Role.ROLE_USER;
        return user;
    }

    public static User registerUser(Long userId, String username, String password, String passwordConfirm, String nickname, LocalDateTime birth, String email, String phoneNumber){
        if (!password.equals(passwordConfirm)){
            throw new CustomException(ErrorCode.PASSWORD_CONFIRM_DENIED);
        }
        return registerUser(userId, username, password, nickname, birth, email, phoneNumber);
    }

    public void changePassword(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)){
            throw new CustomException(ErrorCode.PASSWORD_CONFIRM_DENIED);
        }
        this.password = password;
    }
}
