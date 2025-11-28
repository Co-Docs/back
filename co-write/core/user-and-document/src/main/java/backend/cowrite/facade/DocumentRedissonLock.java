package backend.cowrite.facade;

import backend.cowrite.exception.CustomException;
import backend.cowrite.exception.ErrorCode;
import backend.cowrite.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentRedissonLock {

    private final DocumentService documentService;
    private final RedissonClient redissonClient;
    private static final String DOCUMENT_LOCK = "document::lock";
    private static final int MAX_ATTEMPTS = 3;
    private static final Long WAITING_TIME = 5L;
    private static final Long OCCUPYING_TIME = 3L;

    //코드 중복 실화에요?
    public Long updateDocument(Long documentId, String title, String content) {
        RLock lock = redissonClient.getLock(generateKey(documentId));
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            try {
                boolean available = lock.tryLock(WAITING_TIME, OCCUPYING_TIME, TimeUnit.SECONDS);
                if(available) {
                    return documentService.updateDocument(documentId, title, content);
                }
            } catch (InterruptedException e) {
                throw new CustomException(ErrorCode.UNABLE_TO_OBTAIN_LOCK, "lock 획득 중 인터럽트 발생" + e);
            } finally {
                if(lock.isHeldByCurrentThread())
                    lock.unlock();
            }
        }
        throw new CustomException(ErrorCode.UNABLE_TO_OBTAIN_LOCK, "나중에 다시 실행해주세요.");
    }

    public String updateParticipants(Long documentId, String username) {
        RLock lock = redissonClient.getLock(generateKey(documentId));
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            try {
                boolean available = lock.tryLock(WAITING_TIME, OCCUPYING_TIME, TimeUnit.SECONDS);
                if(available) {
                    return documentService.updateParticipants(documentId, username);
                }
            } catch (InterruptedException e) {
                throw new CustomException(ErrorCode.UNABLE_TO_OBTAIN_LOCK, "lock 획득 중 인터럽트 발생" + e);
            } finally {
                if(lock.isHeldByCurrentThread())
                    lock.unlock();
            }
        }
        throw new CustomException(ErrorCode.UNABLE_TO_OBTAIN_LOCK, "나중에 다시 실행해주세요.");
    }

    private String generateKey(Long documentId) {
        return DOCUMENT_LOCK + documentId.toString();
    }
}
