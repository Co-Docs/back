# WebSocket 계약: 실시간 편집

## 연결

| 항목 | 값 |
|------|----|
| 엔드포인트 | `ws://{host}/ws` (SockJS fallback 지원) |
| 프로토콜 | STOMP over SockJS |
| 인증 | STOMP CONNECT 헤더에 `Authorization: Bearer {JWT}` |

---

## 클라이언트 → 서버 (발행)

### 편집 요청

**STOMP Destination**: `/app/{documentId}`

```json
{
  "version": 5,
  "operationId": "op-uuid-1234",
  "operation": {
    "type": "INSERT",
    "targetPosition": 10,
    "insertText": "안녕",
    "sessionId": "session-abc"
  }
}
```

또는:

```json
{
  "version": 5,
  "operationId": "op-uuid-5678",
  "operation": {
    "type": "DELETE",
    "targetPosition": 10,
    "operationCount": 3
  }
}
```

**필드 설명**:
- `version`: 클라이언트가 마지막으로 알고 있는 서버 버전 (baseVersion)
- `operationId`: 클라이언트가 생성한 고유 연산 ID (중복 감지용)
- `sessionId`: WebSocket 세션 식별자 (동일 위치 충돌 시 순서 결정에 사용)

---

## 서버 → 클라이언트 (구독)

### 편집 결과 브로드캐스트

**STOMP Destination**: `/topic/{documentId}`

```json
{
  "editedContent": "전체 문서 내용...",
  "version": 6,
  "targetPosition": 10,
  "operationId": "op-uuid-1234"
}
```

**수신 대상**: 해당 문서를 구독 중인 모든 클라이언트

---

### 편집 실패 알림 (신규)

**STOMP Destination**: `/topic/{documentId}/errors` *(해당 사용자 세션에만)*

```json
{
  "operationId": "op-uuid-1234",
  "errorCode": "EDIT_FAILED",
  "message": "편집 처리에 실패했습니다. 다시 시도해주세요."
}
```

**errorCode 종류**:
- `EDIT_FAILED`: OT 처리 중 오류
- `RATE_LIMIT_EXCEEDED`: 초당 10회 초과
- `SERVICE_UNAVAILABLE`: Kafka 장애 등 인프라 문제

---

### 문서 삭제 알림 (신규)

**STOMP Destination**: `/topic/{documentId}`

```json
{
  "type": "DOCUMENT_DELETED",
  "documentId": 123456789,
  "message": "문서가 삭제되었습니다."
}
```

**수신 대상**: 해당 문서를 구독 중인 모든 클라이언트
**클라이언트 동작**: 수신 즉시 편집 화면을 닫고 문서 목록으로 이동

---

## 속도 제한 동작

- 사용자당 초당 10회 초과 시 해당 요청은 처리하지 않고 `RATE_LIMIT_EXCEEDED` 에러 반환
- 다른 사용자의 편집에는 영향 없음
