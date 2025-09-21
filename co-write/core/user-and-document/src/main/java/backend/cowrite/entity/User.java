package backend.cowrite.entity;

import backend.cowrite.common.BaseEntity;
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

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @BatchSize(size = 20)
    private List<UserDocument> userDocuments = new ArrayList<>();
}
