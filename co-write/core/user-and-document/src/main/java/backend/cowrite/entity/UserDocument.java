package backend.cowrite.entity;

import backend.cowrite.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.List;

@Entity
@Getter
@Table(name = "user_document")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDocument extends BaseEntity {

    @Id
    private Long userDocumentId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static UserDocument userToUserDocument(User user, Document document) {
        UserDocument userDocument = new UserDocument();
        userDocument.user = user;
        userDocument.document = document;
        return userDocument;
    }
}
