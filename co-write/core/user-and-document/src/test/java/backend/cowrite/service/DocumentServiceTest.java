package backend.cowrite.service;

import backend.cowrite.common.snowflake.Snowflake;
import backend.cowrite.entity.Document;
import backend.cowrite.entity.User;
import backend.cowrite.exception.CustomException;
import backend.cowrite.exception.ErrorCode;
import backend.cowrite.repository.DocumentRepository;
import backend.cowrite.service.response.DocumentDetailResponse;
import backend.cowrite.service.response.DocumentPreviewResponse;
import backend.cowrite.service.response.UserCacheDto;
import backend.cowrite.common.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private UserService userService;
    @Mock private Snowflake snowflake;

    private DocumentService documentService;

    private static final LocalDateTime BIRTH = LocalDateTime.of(1995, 1, 1, 0, 0);
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(documentRepository, userService, snowflake);
    }

    private User createUser(Long id, String username) {
        return User.registerUser(id, username, "pw", "nick" + id, BIRTH, "a@b.com", "010");
    }

    private Document createDocument(Long docId, User owner) {
        return Document.addNewDocument(docId, "제목", null, owner, List.of());
    }

    // ─────────────────────────────────────────────────────────────
    // readAll()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("readAll()")
    class ReadAll {

        @Test
        @DisplayName("사용자의 문서 목록을 페이지 단위로 반환한다")
        void readAll_returnsDocumentPreview() {
            User owner = createUser(1L, "owner");
            Document doc = createDocument(10L, owner);
            when(documentRepository.readAll(1L, PAGEABLE)).thenReturn(List.of(doc));

            DocumentPreviewResponse response = documentService.readAll(1L, PAGEABLE);

            assertThat(response.documents()).hasSize(1);
            assertThat(response.documents()).extracting("title").containsExactly("제목");
        }

        @Test
        @DisplayName("문서가 없으면 빈 목록을 반환한다")
        void readAll_noDocuments_returnsEmpty() {
            when(documentRepository.readAll(1L, PAGEABLE)).thenReturn(List.of());

            DocumentPreviewResponse response = documentService.readAll(1L, PAGEABLE);

            assertThat(response.documents()).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // addNewDocument()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("addNewDocument()")
    class AddNewDocument {

        @Test
        @DisplayName("owner와 participants로 문서를 생성하고 documentId를 반환한다")
        void addNewDocument_createsAndReturnsId() {
            User owner = createUser(1L, "owner");
            User participant = createUser(2L, "user2");
            when(userService.findById(1L)).thenReturn(owner);
            when(userService.findByUsernameNoCache("user2")).thenReturn(participant);
            when(snowflake.nextId()).thenReturn(100L);
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            Long docId = documentService.addNewDocument(1L, "새 문서", "pw", List.of("user2"));

            assertThat(docId).isEqualTo(100L);
            verify(documentRepository).save(any(Document.class));
        }

        @Test
        @DisplayName("participantsId가 null이면 owner만으로 문서를 생성한다")
        void addNewDocument_nullParticipants_ownerOnly() {
            User owner = createUser(1L, "owner");
            when(userService.findById(1L)).thenReturn(owner);
            when(snowflake.nextId()).thenReturn(100L);
            when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            Long docId = documentService.addNewDocument(1L, "제목", null, null);

            assertThat(docId).isEqualTo(100L);
            verify(userService, never()).findByUsernameNoCache(any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // readDocument()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("readDocument()")
    class ReadDocument {

        @Test
        @DisplayName("documentId로 상세 정보를 반환한다")
        void readDocument_found_returnsDetail() {
            User owner = createUser(1L, "owner");
            Document doc = createDocument(10L, owner);
            when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));

            DocumentDetailResponse response = documentService.readDocument(10L);

            assertThat(response.title()).isEqualTo("제목");
        }

        @Test
        @DisplayName("문서가 없으면 DOCS_NOT_FOUND 예외 발생")
        void readDocument_notFound_throwsException() {
            when(documentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.readDocument(99L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.DOCS_NOT_FOUND));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // deleteDocument()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteDocument()")
    class DeleteDocument {

        @Test
        @DisplayName("documentId로 문서를 완전히 삭제한다")
        void deleteDocument_byId_deletesDocument() {
            User owner = createUser(1L, "owner");
            Document doc = createDocument(10L, owner);
            when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));

            documentService.deleteDocument(10L);

            verify(documentRepository).delete(doc);
        }

        @Test
        @DisplayName("문서가 없으면 DOCS_NOT_FOUND 예외 발생")
        void deleteDocument_byId_notFound_throwsException() {
            when(documentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.deleteDocument(99L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.DOCS_NOT_FOUND));
        }

        @Test
        @DisplayName("username + documentId로 해당 사용자를 문서에서 제거한다")
        void deleteDocument_byUsernameAndId_removesParticipant() {
            User owner = createUser(1L, "owner");
            User participant = createUser(2L, "user2");
            Document doc = createDocument(10L, owner);
            doc.addParticipants(participant);

            when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));
            when(userService.findByUsernameNoCache("user2")).thenReturn(participant);

            documentService.deleteDocument("user2", 10L);

            assertThat(doc.getUserDocuments()).hasSize(1);
            assertThat(doc.getUserDocuments().get(0).getUser().getUsername()).isEqualTo("owner");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updateDocument()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateDocument()")
    class UpdateDocument {

        @Test
        @DisplayName("문서를 업데이트하고 documentId를 반환한다")
        void updateDocument_updatesAndReturnsId() {
            User owner = createUser(1L, "owner");
            Document doc = createDocument(10L, owner);
            when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));

            Long result = documentService.updateDocument(10L, "새 제목", "새 내용");

            assertThat(result).isEqualTo(10L);
            assertThat(doc.getTitle()).isEqualTo("새 제목");
            assertThat(doc.getContent()).isEqualTo("새 내용");
        }

        @Test
        @DisplayName("title이 null이면 제목은 변경되지 않고 content만 업데이트된다")
        void updateDocument_nullTitle_onlyContentUpdated() {
            User owner = createUser(1L, "owner");
            Document doc = createDocument(10L, owner);
            when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));

            documentService.updateDocument(10L, null, "새 내용");

            assertThat(doc.getTitle()).isEqualTo("제목");
            assertThat(doc.getContent()).isEqualTo("새 내용");
        }

        @Test
        @DisplayName("문서가 없으면 DOCS_NOT_FOUND 예외 발생")
        void updateDocument_notFound_throwsException() {
            when(documentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.updateDocument(99L, "제목", "내용"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.DOCS_NOT_FOUND));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updateParticipants()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateParticipants()")
    class UpdateParticipants {

        @Test
        @DisplayName("새 참여자를 문서에 추가하고 nickname을 반환한다")
        void updateParticipants_addsParticipantAndReturnsNickname() {
            User owner = createUser(1L, "owner");
            User newUser = createUser(2L, "user2");
            Document doc = createDocument(10L, owner);

            UserCacheDto cacheDto = new UserCacheDto(2L, "user2", "pw", "nick2", Role.ROLE_USER);
            when(userService.findByUsername("user2")).thenReturn(cacheDto);
            when(userService.findById(2L)).thenReturn(newUser);
            when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));

            String nickname = documentService.updateParticipants(10L, "user2");

            assertThat(nickname).isEqualTo("nick2");
            assertThat(doc.getUserDocuments()).hasSize(2);
        }

        @Test
        @DisplayName("문서가 없으면 DOCS_NOT_FOUND 예외 발생")
        void updateParticipants_documentNotFound_throwsException() {
            UserCacheDto cacheDto = new UserCacheDto(2L, "user2", "pw", "nick2", Role.ROLE_USER);
            when(userService.findByUsername("user2")).thenReturn(cacheDto);
            when(userService.findById(2L)).thenReturn(createUser(2L, "user2"));
            when(documentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.updateParticipants(99L, "user2"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.DOCS_NOT_FOUND));
        }
    }
}
