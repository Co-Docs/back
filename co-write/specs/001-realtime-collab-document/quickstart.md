# Quickstart: 기능 검증 가이드

## 사전 조건

- Docker Compose로 MySQL, Redis, Kafka 실행 중
- `core:user-and-document`, `core:edit-document` 서버 실행 중
- WebSocket 테스트 클라이언트 (e.g., Postman, wscat, 또는 브라우저 콘솔)

---

## 시나리오 1: 실시간 동시 편집 (US1 — P1)

**목표**: 두 클라이언트가 동시에 편집해도 동일한 문서로 수렴하는지 확인

1. 두 클라이언트에서 같은 documentId로 WebSocket 구독 (`/topic/{documentId}`)
2. 클라이언트 A에서 편집 발행:
   ```json
   SEND /app/{documentId}
   { "version": 0, "operationId": "op-A1", "operation": { "type": "INSERT", "targetPosition": 0, "insertText": "Hello", "sessionId": "sessionA" } }
   ```
3. 클라이언트 B에서 동시에 편집 발행:
   ```json
   SEND /app/{documentId}
   { "version": 0, "operationId": "op-B1", "operation": { "type": "INSERT", "targetPosition": 0, "insertText": "World", "sessionId": "sessionB" } }
   ```
4. **기대 결과**: 두 클라이언트 모두 `/topic/{documentId}`에서 동일한 `editedContent`를 수신
5. **검증**: `version`이 두 번 증가하고, 두 클라이언트의 최종 `editedContent`가 동일

---

## 시나리오 2: 자동 저장 (US2 — P2)

**목표**: 10번 편집 후 MySQL에 저장되는지 확인

1. 동일 문서에 10번 편집 요청 순차 발행 (각 요청의 `version`을 응답 `version`으로 갱신)
2. 10번째 편집 후 MySQL에서 확인:
   ```sql
   SELECT content FROM document WHERE document_id = {documentId};
   ```
3. **기대 결과**: DB content가 마지막 편집 내용과 일치

---

## 시나리오 3: 속도 제한 (FR-012)

**목표**: 초당 11회 요청 시 11번째가 거부되는지 확인

1. 동일 클라이언트에서 1초 이내에 11회 연속 편집 발행
2. **기대 결과**: 11번째 요청에서 `/topic/{documentId}/errors`로 에러 수신:
   ```json
   { "errorCode": "RATE_LIMIT_EXCEEDED", "message": "..." }
   ```
3. 다른 클라이언트의 편집은 정상 처리됨을 확인

---

## 시나리오 4: 문서 삭제 시 WebSocket 알림 (FR-007)

**목표**: 편집 중에 소유자가 문서를 삭제하면 참여자에게 알림이 가는지 확인

1. 클라이언트 A(소유자), 클라이언트 B(참여자) 모두 구독 중인 상태
2. 소유자가 DELETE 요청:
   ```
   DELETE /api/document/{documentId}
   ```
3. **기대 결과**: 클라이언트 B가 `/topic/{documentId}`에서 수신:
   ```json
   { "type": "DOCUMENT_DELETED", "documentId": ..., "message": "문서가 삭제되었습니다." }
   ```

---

## 시나리오 5: 네트워크 재연결 복구 (Edge Case)

**목표**: 버전 갭 발생 시 스냅샷으로 복구되는지 확인

1. 클라이언트 A가 연결 중단 상태에서 다른 클라이언트들이 편집을 200회 이상 수행
2. 클라이언트 A가 재연결 후 오래된 `baseVersion`으로 편집 요청
3. **기대 결과**: 서버가 전체 `editedContent`(스냅샷)로 응답하거나 `EDIT_FAILED` 에러 후 재동기화 안내

---

## 시나리오 6: 문서 CRUD (US3 — P3)

```bash
# 문서 생성
POST /api/document
{ "title": "테스트 문서", "password": null, "userDocuments": [] }
→ Response: { "data": 123456789 }

# 문서 조회
GET /api/document/123456789
→ Response: { "data": { "title": "테스트 문서", "content": null, ... } }

# 참여자 추가
PUT /api/document/123456789
{ "username": "participant@email.com" }

# 문서 나가기 (참여자)
DELETE /api/document/123456789/leave

# 문서 삭제 (소유자)
DELETE /api/document/123456789
```
