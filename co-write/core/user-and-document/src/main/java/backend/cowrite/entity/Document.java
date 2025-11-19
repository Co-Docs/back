package backend.cowrite.entity;

import backend.cowrite.common.BaseEntity;
import backend.cowrite.exception.CustomException;
import backend.cowrite.exception.ErrorCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Table(name = "document")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends BaseEntity {

    @Id @Column(name="document_id")
    private Long documentId;
    @NotNull
    private String title;
    private String password;
    private String content;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<UserDocument> userDocuments = new ArrayList<>();

    /*새로운 문서 추가 메서드*/
    public static Document addNewDocument(Long documentId, String title, String password, User owner, List<User> participants){
        Document newDocs = new Document();
        newDocs.documentId = documentId;
        newDocs.title = title;
        newDocs.password = password;
        newDocs.addParticipants(owner, participants);
        return newDocs;
    }

    public void addParticipants(User owner, List<User> participants) {
        if(participants.stream().anyMatch(user -> java.util.Objects.equals(user.getUserId(), owner.getUserId()))){
            throw new CustomException(ErrorCode.OWNER_CANNOT_BE_PARTICIPANT,"문서 생성자는 참가자가 될 수 없습니다.");
        }
        UserDocument documentOwner = UserDocument.userToUserDocument(owner, this);
        List<UserDocument> documentUsers = new ArrayList<>(participants.stream().map(participant -> UserDocument.userToUserDocument(participant, this)).toList());
        documentUsers.add(documentOwner);
        this.userDocuments.addAll(documentUsers);
    }

    public void addParticipants(User newParticipant) {
        Set<Long> exists = this.userDocuments.stream().map(ud -> ud.getUser().getUserId()).collect(Collectors.toSet());
        if(exists.add(newParticipant.getUserId()))
            this.userDocuments.add(UserDocument.userToUserDocument(newParticipant, this));
    }

    public void updateDifferences(String title, String content) {
        if(title!= null && !this.title.equals(title))
            this.title = title;
        if(this.content == null || !this.content.equals(content))
            this.content = content;
    }
}
