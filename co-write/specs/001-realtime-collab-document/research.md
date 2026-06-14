# Research: 실시간 협업 문서 편집 및 관리 시스템

## 기술 컨텍스트 확인

### Decision 1: OT vs CRDT

- **Decision**: OT(Operational Transformation) — 이미 구현됨
- **Rationale**: `OperatorRebaseUtil`이 Insert/Delete 4가지 케이스를 구현하고 있으며, sessionId 기반 결정론적 충돌 해결이 적용되어 있음. CRDT로 전환 시 전체 재설계 필요.
- **Alternatives considered**: CRDT(Yjs, Automerge) — 오프라인 지원에 유리하나 현재 아키텍처와 맞지 않음.

### Decision 2: Kafka 파티셔닝 전략

- **Decision**: documentId를 Kafka 메시지 키로 사용 → 동일 문서의 편집 이벤트는 동일 파티션에서 순서 보장
- **Rationale**: `OutboxEventPublisher.publish(EventType.UPDATE, payload, documentId)` 확인 — documentId가 키로 전달됨. 파티션 내 메시지 순서가 곧 편집 순서.
- **Alternatives considered**: 타임스탬프 기반 정렬 — 분산 환경에서 clock skew 문제 있음.

### Decision 3: 속도 제한(Rate Limiting) 구현 방식

- **Decision**: Redis 기반 슬라이딩 윈도우 카운터 (사용자당 초당 10회)
- **Rationale**: 이미 Redis가 인프라에 포함되어 있어 추가 의존성 없음. `StringRedisTemplate`의 `increment` + `expire`로 구현 가능.
- **Alternatives considered**: Spring `@RateLimiter`(Resilience4j) — 단일 인스턴스에서만 유효, 분산 환경 미지원.

### Decision 4: 문서 삭제 시 WebSocket 참여자 알림 방식

- **Decision**: `SimpMessagingTemplate.convertAndSend()`로 삭제 알림 메시지를 해당 문서 구독 경로(`/topic/{documentId}`)에 브로드캐스트
- **Rationale**: 이미 `DocumentUpdateEventConsumer`에서 동일한 방식으로 편집 결과를 브로드캐스트하고 있음. 재사용 가능.
- **Alternatives considered**: 개별 세션별 `convertAndSendToUser()` — 참여자 목록 추적 복잡성 증가.

### Decision 5: Kafka 장애 감지 및 클라이언트 알림

- **Decision**: `DocumentUpdatePublisher`에서 발행 예외 캐치 → WebSocket 에러 메시지 전송 (즉시 실패 처리)
- **Rationale**: Outbox 패턴이므로 발행 실패 시 트랜잭션 롤백. 클라이언트에 즉시 피드백이 UX상 유리.
- **Alternatives considered**: Circuit Breaker(Resilience4j) — 운영 복잡성 증가, 현 단계에서 과도한 엔지니어링.

### Decision 6: deleteDocument 버그 분석

- **현상**: `DocumentService.deleteDocument(String username, Long documentId)`가 실제로 문서를 삭제하지 않고 해당 유저를 참여자 목록에서 제거함 (`subtractParticipant`).
- **Decision**: 두 메서드의 역할을 명확히 분리. 소유자만 문서 전체 삭제 가능 (`deleteDocument(Long documentId)`), 참여자는 자신을 제거 (`leaveDocument(String username, Long documentId)`).
- **영향**: `DocumentController`의 DELETE 엔드포인트 분리 필요.

### Decision 7: DocumentRedissonLock 코드 중복

- **현상**: `updateDocument()`와 `updateParticipants()`가 동일한 락 획득 로직을 중복 구현.
- **Decision**: 공통 락 획득 로직을 private 헬퍼 메서드(`executeWithLock()`)로 추출하는 리팩토링 필요.
- **영향**: 단일 책임 원칙(헌법 원칙 IV) 및 DRY 원칙 위반 해소.
