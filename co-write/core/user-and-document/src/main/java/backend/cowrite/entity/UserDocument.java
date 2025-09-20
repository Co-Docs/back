package backend.cowrite.entity;

import backend.cowrite.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "user_document")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDocument extends BaseEntity {

    @Id
    private Long userDocumentId;

    @ManyToOne(fetch = FetchType.LAZY)
    private Document document;
}
