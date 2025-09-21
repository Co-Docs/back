package backend.cowrite.entity;

import backend.cowrite.common.BaseEntity;
import jakarta.persistence.*;
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
    private String title;
    private String content;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "document", cascade = CascadeType.PERSIST)
    @BatchSize(size = 20)
    private List<UserDocument> userDocuments = new ArrayList<>();

    /*새로운 문서 추가 메서드*/
    public static Document addNewDocument(Long documentId, String title, String content, User owner, List<User> participants){
        Document newDocs = new Document();
        newDocs.documentId = documentId;
        newDocs.title = title;
        newDocs.content = content;
        newDocs.addParticipants(owner, participants);
        return newDocs;
    }

    public void addParticipants(User owner, List<User> participants) {
        UserDocument documentOwner = UserDocument.userToUserDocument(owner, this);
        List<UserDocument> documentUsers = participants.stream().map(user -> UserDocument.userToUserDocument(user, this)).toList();
        documentUsers.add(documentOwner);
        this.userDocuments.addAll(documentUsers);
    }

    public void addParticipants(List<User> newParticipants) {
        Set<Long> exists = this.userDocuments.stream().map(ud -> ud.getUser().getUserId()).collect(Collectors.toSet());
        for (User newParticipant : newParticipants) {
            if(exists.add(newParticipant.getUserId()))
                this.userDocuments.add(UserDocument.userToUserDocument(newParticipant, this));
        }
    }

    public void updateDifferences(String title, String content) {
        if(!this.title.equals(title))
            this.title = title;
        if(!this.content.equals(content))
            this.content = content;
    }
}
