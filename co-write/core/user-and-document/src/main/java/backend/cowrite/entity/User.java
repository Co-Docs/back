package backend.cowrite.entity;

import backend.cowrite.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Table(name = "user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    private Long userId;
    private String username;
    private String password;
    private String nickname;
    private LocalDateTime birth;
    private String email;
    private String phoneNumber;

    @OneToMany(fetch = FetchType.LAZY)
    private List<UserDocument> userDocuments;
}
