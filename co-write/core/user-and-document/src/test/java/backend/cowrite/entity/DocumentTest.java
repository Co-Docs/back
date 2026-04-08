package backend.cowrite.entity;

import backend.cowrite.exception.CustomException;
import backend.cowrite.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTest {

    private User owner;
    private User participant1;
    private User participant2;

    @BeforeEach
    void setUp() {
        LocalDateTime birth = LocalDateTime.of(1995, 1, 1, 0, 0);
        owner = User.registerUser(1L, "owner", "pw", "ownerNick", birth, "o@b.com", "010-0000-0001");
        participant1 = User.registerUser(2L, "user1", "pw", "nick1", birth, "u1@b.com", "010-0000-0002");
        participant2 = User.registerUser(3L, "user2", "pw", "nick2", birth, "u2@b.com", "010-0000-0003");
    }

    // ─────────────────────────────────────────────────────────────
    // addNewDocument()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("addNewDocument() 문서 생성")
    class AddNewDocument {

        @Test
        @DisplayName("문서가 생성되고 owner + participants 모두 UserDocument에 추가된다")
        void addNewDocument_setsFieldsAndParticipants() {
            Document doc = Document.addNewDocument(10L, "제목", "pw1234", owner, List.of(participant1, participant2));

            assertThat(doc.getDocumentId()).isEqualTo(10L);
            assertThat(doc.getTitle()).isEqualTo("제목");
            assertThat(doc.getPassword()).isEqualTo("pw1234");
            assertThat(doc.getUserDocuments()).hasSize(3); // owner + 2 participants
        }

        @Test
        @DisplayName("참여자 없이 owner만으로 문서를 생성할 수 있다")
        void addNewDocument_ownerOnly() {
            Document doc = Document.addNewDocument(10L, "제목", null, owner, List.of());

            assertThat(doc.getUserDocuments()).hasSize(1);
        }

        @Test
        @DisplayName("owner가 participants에 포함되면 OWNER_CANNOT_BE_PARTICIPANT 예외 발생")
        void addNewDocument_ownerInParticipants_throwsException() {
            assertThatThrownBy(() ->
                    Document.addNewDocument(10L, "제목", "pw", owner, List.of(participant1, owner))
            )
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.OWNER_CANNOT_BE_PARTICIPANT));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // addParticipants(User newParticipant)
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("addParticipants(User) 개별 참여자 추가")
    class AddSingleParticipant {

        @Test
        @DisplayName("새로운 참여자가 추가된다")
        void addParticipants_newUser_added() {
            Document doc = Document.addNewDocument(10L, "제목", null, owner, List.of(participant1));
            doc.addParticipants(participant2);

            assertThat(doc.getUserDocuments()).hasSize(3);
        }

        @Test
        @DisplayName("이미 참여 중인 사용자는 중복 추가되지 않는다")
        void addParticipants_existingUser_notDuplicated() {
            Document doc = Document.addNewDocument(10L, "제목", null, owner, List.of(participant1));
            doc.addParticipants(participant1); // 이미 존재

            assertThat(doc.getUserDocuments()).hasSize(2);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // subtractParticipant()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("subtractParticipant() 참여자 제거")
    class SubtractParticipant {

        @Test
        @DisplayName("참여자를 제거하면 UserDocument 목록에서 삭제된다")
        void subtractParticipant_removesFromList() {
            Document doc = Document.addNewDocument(10L, "제목", null, owner, List.of(participant1, participant2));
            doc.subtractParticipant(participant1);

            assertThat(doc.getUserDocuments()).hasSize(2);
            assertThat(doc.getUserDocuments().stream()
                    .map(ud -> ud.getUser().getUserId()))
                    .doesNotContain(participant1.getUserId());
        }

        @Test
        @DisplayName("존재하지 않는 사용자를 제거해도 예외 없이 목록은 그대로다")
        void subtractParticipant_nonExistentUser_noChange() {
            Document doc = Document.addNewDocument(10L, "제목", null, owner, List.of(participant1));
            doc.subtractParticipant(participant2); // 참여 안 함

            assertThat(doc.getUserDocuments()).hasSize(2);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updateDifferences()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateDifferences() 제목/내용 업데이트")
    class UpdateDifferences {

        @Test
        @DisplayName("title이 다를 때만 제목이 변경된다")
        void updateDifferences_differentTitle_updated() {
            Document doc = Document.addNewDocument(10L, "기존 제목", null, owner, List.of());
            doc.updateDifferences("새 제목", "내용");

            assertThat(doc.getTitle()).isEqualTo("새 제목");
        }

        @Test
        @DisplayName("title이 null이면 기존 제목이 유지된다")
        void updateDifferences_nullTitle_keptAsIs() {
            Document doc = Document.addNewDocument(10L, "기존 제목", null, owner, List.of());
            doc.updateDifferences(null, "새 내용");

            assertThat(doc.getTitle()).isEqualTo("기존 제목");
        }

        @Test
        @DisplayName("title이 동일해도 content는 항상 업데이트된다")
        void updateDifferences_sameTitleDifferentContent_contentUpdated() {
            Document doc = Document.addNewDocument(10L, "제목", null, owner, List.of());
            doc.updateDifferences("제목", "새 내용");

            assertThat(doc.getTitle()).isEqualTo("제목");
            assertThat(doc.getContent()).isEqualTo("새 내용");
        }

        @Test
        @DisplayName("content가 null에서 새 값으로 업데이트된다")
        void updateDifferences_contentWasNull_updated() {
            Document doc = Document.addNewDocument(10L, "제목", null, owner, List.of());
            assertThat(doc.getContent()).isNull();

            doc.updateDifferences(null, "첫 내용");
            assertThat(doc.getContent()).isEqualTo("첫 내용");
        }

        @Test
        @DisplayName("title이 같고 content도 같으면 아무것도 변경되지 않는다")
        void updateDifferences_sameValues_noChange() {
            Document doc = Document.addNewDocument(10L, "제목", null, owner, List.of());
            doc.updateDifferences(null, "내용1");
            doc.updateDifferences("제목", "내용1");

            assertThat(doc.getTitle()).isEqualTo("제목");
            assertThat(doc.getContent()).isEqualTo("내용1");
        }
    }
}
