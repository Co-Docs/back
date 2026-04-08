package backend.cowrite.facade;

import backend.cowrite.exception.CustomException;
import backend.cowrite.exception.ErrorCode;
import backend.cowrite.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentRedissonLockTest {

    @Mock private DocumentService documentService;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock rLock;

    private DocumentRedissonLock documentRedissonLock;

    private static final Long DOCUMENT_ID = 1L;

    @BeforeEach
    void setUp() {
        documentRedissonLock = new DocumentRedissonLock(documentService, redissonClient);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
    }

    // ─────────────────────────────────────────────────────────────
    // updateDocument()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateDocument() 분산 락")
    class UpdateDocument {

        @Test
        @DisplayName("락 획득 성공 시 DocumentService.updateDocument()를 호출하고 결과를 반환한다")
        void updateDocument_lockAcquired_callsServiceAndReturns() throws InterruptedException {
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(documentService.updateDocument(DOCUMENT_ID, "제목", "내용")).thenReturn(DOCUMENT_ID);

            Long result = documentRedissonLock.updateDocument(DOCUMENT_ID, "제목", "내용");

            assertThat(result).isEqualTo(DOCUMENT_ID);
            verify(documentService).updateDocument(DOCUMENT_ID, "제목", "내용");
            verify(rLock).unlock();
        }

        @Test
        @DisplayName("락 획득 실패가 3회 반복되면 UNABLE_TO_OBTAIN_LOCK 예외 발생")
        void updateDocument_lockNotAcquired_exhaustsRetriesAndThrows() throws InterruptedException {
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);
            when(rLock.isHeldByCurrentThread()).thenReturn(false);

            assertThatThrownBy(() -> documentRedissonLock.updateDocument(DOCUMENT_ID, "제목", "내용"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.UNABLE_TO_OBTAIN_LOCK));

            verify(rLock, times(3)).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
            verify(documentService, never()).updateDocument(any(), any(), any());
        }

        @Test
        @DisplayName("InterruptedException 발생 시 UNABLE_TO_OBTAIN_LOCK 예외로 변환된다")
        void updateDocument_interrupted_throwsUnableToObtainLock() throws InterruptedException {
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                    .thenThrow(new InterruptedException("interrupted"));
            when(rLock.isHeldByCurrentThread()).thenReturn(false);

            assertThatThrownBy(() -> documentRedissonLock.updateDocument(DOCUMENT_ID, "제목", "내용"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.UNABLE_TO_OBTAIN_LOCK));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updateParticipants()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateParticipants() 분산 락")
    class UpdateParticipants {

        @Test
        @DisplayName("락 획득 성공 시 DocumentService.updateParticipants()를 호출하고 nickname을 반환한다")
        void updateParticipants_lockAcquired_callsServiceAndReturns() throws InterruptedException {
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(documentService.updateParticipants(DOCUMENT_ID, "user2")).thenReturn("nick2");

            String nickname = documentRedissonLock.updateParticipants(DOCUMENT_ID, "user2");

            assertThat(nickname).isEqualTo("nick2");
            verify(documentService).updateParticipants(DOCUMENT_ID, "user2");
            verify(rLock).unlock();
        }

        @Test
        @DisplayName("락 획득 실패가 3회 반복되면 UNABLE_TO_OBTAIN_LOCK 예외 발생")
        void updateParticipants_lockNotAcquired_exhaustsRetriesAndThrows() throws InterruptedException {
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);
            when(rLock.isHeldByCurrentThread()).thenReturn(false);

            assertThatThrownBy(() -> documentRedissonLock.updateParticipants(DOCUMENT_ID, "user2"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.UNABLE_TO_OBTAIN_LOCK));

            verify(rLock, times(3)).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
            verify(documentService, never()).updateParticipants(any(), any());
        }
    }
}
