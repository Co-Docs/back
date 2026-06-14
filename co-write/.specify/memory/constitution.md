<!--
===== Sync Impact Report =====
버전 변경: (플레이스홀더 템플릿) → 1.0.0
수정된 원칙: 없음 (최초 작성)
추가된 섹션:
  - 핵심 원칙 I~VI (OT 정확성, Kafka 순서 보장, Redis-DB 일관성, 모듈 독립성, 실시간 통신 견고성, 테스트 규율)
  - 동시 편집 문제 정의 및 해결 프로세스
  - 수정 완료 후 코드리뷰 및 리팩토링 프로세스
제거된 섹션: 없음
템플릿 업데이트 현황:
  ✅ .specify/templates/plan-template.md — Constitution Check 게이트 항목 검토 완료
  ✅ .specify/templates/spec-template.md — 동시 편집 요구사항 섹션 검토 완료
  ✅ .specify/templates/tasks-template.md — OT 알고리즘 테스트 태스크 유형 검토 완료
후속 TODO: 없음 (모든 플레이스홀더 채움)
==============================
-->

# co-write 헌법

## 핵심 원칙

### I. OT 알고리즘 정확성 (비협상적)

동시 편집의 모든 연산은 Operational Transformation(OT) 알고리즘을 통해 정확하게 변환되어야 한다.

- `OperatorRebaseUtil`의 `rebase()` 메서드는 Insert-Insert, Insert-Delete, Delete-Insert,
  Delete-Delete 네 가지 충돌 케이스를 모두 올바르게 처리해야 한다.
- `operationCount`가 0 이하인 Delete 연산은 반드시 `null`을 반환하여 무연산으로 처리한다.
- OT 연산 결과는 항상 **수렴(convergence)** 해야 한다: 동일한 초기 상태에서 어떤 순서로 연산이 적용되더라도
  최종 문서 내용이 일치해야 한다.
- 새로운 연산 유형 추가 시 반드시 기존 모든 케이스와의 rebase 메서드를 구현하고 단위 테스트를 작성해야 한다.

### II. Kafka 기반 단일 문서 순서 보장

같은 문서에 대한 편집 이벤트는 반드시 순서가 보장되어야 한다.

- Kafka 토픽 파티셔닝 키는 `documentId`를 사용해야 하며, 동일 문서의 메시지는 항상 같은 파티션으로 전달된다.
- Kafka Consumer는 단일 스레드로 문서 편집 이벤트를 처리해야 한다(동일 파티션 내 메시지 순서 보장).
- Outbox 패턴을 사용하여 편집 이벤트 발행의 원자성을 보장해야 한다.
- DLQ(Dead Letter Queue) Consumer는 복구 불가 메시지를 별도 처리하며, 재처리 로직을 문서화해야 한다.

### III. Redis-DB 캐시 일관성

Redis 캐시와 MySQL 데이터베이스의 문서 상태는 반드시 일관성을 유지해야 한다.

- Redis에 캐싱되는 데이터 항목: `content`(문서 내용), `version`(현재 버전), `operation`(연산 이력), `count`(업데이트 횟수).
- `content` TTL은 5시간, `count` TTL은 12시간으로 유지하며, TTL 변경 시 영향 분석을 반드시 수행해야 한다.
- Redis `operation` ZSet은 최대 200개 항목을 유지하며, 초과 시 오래된 항목을 자동 제거한다.
- 업데이트 횟수(count)가 임계값(10)에 도달하면 즉시 MySQL 영속화 이벤트를 발행해야 한다.
- Redis 캐시 미스 발생 시 `DocumentClient`를 통해 MySQL에서 내용을 가져오고 반드시 Redis에 재캐싱해야 한다.
- Redis에서 삭제 또는 만료된 연산 이력에 대한 rebase 요청은 반드시 감지하고 적절히 처리해야 한다.

### IV. 멀티모듈 독립성

`global` 모듈은 `core` 모듈에 의존하지 않아야 하며, `core` 모듈 간 직접 의존도 허용하지 않는다.

- `global` 모듈: `snowflake`, `data-serializer`, `event`, `outbox-message-relay`, `response-handler`.
  이 모듈들은 순수 공통 기능만 제공하며 비즈니스 로직을 포함하지 않는다.
- `core:edit-document`와 `core:user-and-document`는 서로 직접 의존하지 않는다.
  통신은 반드시 Kafka 이벤트 또는 HTTP(Feign/RestTemplate) 클라이언트를 통해야 한다.
- `core:read-document`는 별도 읽기 전용 서비스로 유지하며 쓰기 로직을 포함하지 않는다.
- 새 공통 로직 추가 시 `global` 모듈에 배치하고, 특정 도메인 종속적인 경우에만 `core`에 배치한다.

### V. 실시간 통신 견고성

WebSocket/STOMP 연결은 클라이언트 연결 해제, 네트워크 이상, 서버 재시작 상황에서도 견고하게 동작해야 한다.

- WebSocket 구독 경로(`/topic/document/{documentId}`)와 발행 경로(`/app/{documentId}`)는 명확히 분리된다.
- 클라이언트는 WebSocket 재연결 시 마지막 알고 있는 버전(`version`)을 포함하여 재동기화 요청을 보내야 한다.
- 서버는 클라이언트의 `baseVersion`이 Redis에 남아있는 가장 오래된 연산 이력보다 오래된 경우
  전체 문서 내용을 재전송해야 한다(스냅샷 복구).
- 프론트엔드(`../co-write-front`) 코드는 백엔드 API/WebSocket 프로토콜 변경 시 함께 수정되어야 하며,
  두 저장소의 변경은 동일 작업 단위로 묶어서 처리한다.
- `EditedResult` 응답에는 `editedContent`, `version`, `cursorPosition`, `operationId`가 포함되어야 한다.

### VI. 테스트 규율

OT 알고리즘과 동시 편집 로직은 반드시 단위 테스트로 검증되어야 하며, 통합 테스트도 주요 흐름을 커버해야 한다.

- `OperatorRebaseUtil`의 모든 케이스(I+I, I+D, D+I, D+D)에 대해 단위 테스트가 존재해야 한다.
- 경계 조건 테스트 필수: 동일 위치 삽입, 삽입 범위 내 삭제, 전체 범위 삭제 등.
- `DocumentUpdateService` 및 `DocumentUpdateEventHandler`의 통합 테스트는 Mock Redis/Kafka를 사용해도 되지만
  OT 변환 결과는 실제 `OperatorRebaseUtil`을 사용해야 한다(Mock 금지).
- 새 기능 추가 또는 버그 수정 시 재현 테스트를 먼저 작성한 뒤 구현한다(Red-Green 원칙).

## 동시 편집 문제 정의 및 해결 프로세스

이 섹션은 co-write에서 발생 가능한 동시 편집 문제를 정의하고, 각 문제에 대한 표준 해결 절차를 명시한다.

### 문제 유형 정의

| 코드 | 문제 유형 | 설명 |
|------|-----------|------|
| CE-001 | 수렴 실패 | 동일 초기 상태에서 다른 연산 적용 후 최종 내용 불일치 |
| CE-002 | 버전 갭 | Redis 연산 이력(최대 200개)에 없는 버전 간격으로 rebase 불가 |
| CE-003 | 커서 불일치 | rebase 후 다른 클라이언트의 커서 위치가 잘못 계산됨 |
| CE-004 | 유실 업데이트 | Kafka 메시지 처리 실패로 편집 내용 누락 |
| CE-005 | 캐시-DB 불일치 | Redis content 만료 후 MySQL 복구 시 버전 불일치 |
| CE-006 | 중복 연산 | 동일 `operationId`의 연산이 두 번 적용됨 |

### 문제 발견 → 해결 절차

1. **문제 재현**: 재현 가능한 단위 테스트 또는 통합 테스트를 먼저 작성하여 실패를 확인한다.
2. **범위 분류**: 위 문제 유형 코드(CE-XXX)로 분류하고 영향 범위(단일 문서 vs 전체 서비스)를 파악한다.
3. **수정 구현**: 원칙 I~VI를 위반하지 않는 범위에서 수정을 구현한다.
4. **수렴 검증**: OT 수렴 속성을 자동 테스트로 검증한다(A∘B' = B∘A' 검증).
5. **프론트엔드 영향 확인**: 프로토콜 변경이 있는 경우 `../co-write-front` 코드를 함께 수정한다.
6. **코드리뷰 실시**: 수정 완료 후 아래 "수정 완료 후 코드리뷰 프로세스" 절차를 반드시 수행한다.

## 수정 완료 후 코드리뷰 및 리팩토링 프로세스

모든 기능 추가, 버그 수정, 리팩토링 완료 후 반드시 아래 체크리스트를 수행한다.

### 코드리뷰 체크리스트

**정확성 검토**
- [ ] OT 알고리즘 수렴 속성이 변경된 코드에서 유지되는가?
- [ ] 버전 검증 로직(`baseVersion > serverVersion` 거부)이 올바른가?
- [ ] Redis 키 TTL 설정이 의도와 일치하는가?
- [ ] Kafka 메시지 키(documentId)가 올바르게 설정되어 있는가?

**공통 로직 추출 검토**
- [ ] `edit-document`와 `user-and-document` 모듈 사이에 중복 코드가 있는가?
  있다면 `global` 모듈로 추출을 고려한다.
- [ ] EventHandler 인터페이스 패턴이 두 모듈에서 일관되게 사용되는가?
- [ ] Redis 키 생성 패턴이 일관된가? 키 충돌 가능성은 없는가?

**리팩토링 적합성 판단**
- [ ] 3개 이상의 동일 패턴이 반복되는가? → 공통 추상화 고려.
- [ ] 단일 메서드가 3가지 이상의 책임을 가지는가? → 분리 고려.
- [ ] `DocumentUpdateEventHandler.handle()`의 분기 로직이 명확한가?

**프론트엔드 연동 검토**
- [ ] WebSocket 메시지 형식(`EditedResult` 필드)이 변경되었는가?
  → `../co-write-front`의 WebSocket 메시지 파싱 코드 확인 및 수정.
- [ ] 구독/발행 경로가 변경되었는가? → 프론트엔드 경로 설정 수정.
- [ ] 에러 응답 형식이 변경되었는가? → 프론트엔드 에러 핸들러 수정.

**보안 및 성능 검토**
- [ ] WebSocket 연결에 인증 토큰 검증이 적용되어 있는가?
- [ ] Redis `ZSet.rangeByScore()` 쿼리 범위가 적절히 제한되어 있는가?
- [ ] Kafka Consumer의 `ack.acknowledge()` 호출이 처리 완료 후 이루어지는가?

### 리팩토링 실행 기준

리팩토링은 다음 조건 중 하나를 충족할 때만 수행한다:
- 동일 로직이 3개 이상 파일에 복사된 경우
- 테스트 없이 수정이 불가능한 복잡한 메서드가 발견된 경우
- 새 기능 추가가 기존 구조로 인해 원칙 위반을 강요하는 경우

리팩토링 범위는 현재 변경 파일과 직접 연관된 코드로 제한한다. 무관한 파일의 정리는 별도 PR로 분리한다.

## Governance

- 이 헌법은 co-write 프로젝트의 모든 개발 결정보다 우선한다.
- 헌법 개정은 아래 절차를 따른다:
  1. 개정 이유와 영향 범위를 문서화한다.
  2. 개정 내용을 이 파일에 반영하고 버전을 갱신한다.
  3. 영향 받는 `.specify/templates/` 파일을 동시에 업데이트한다.
  4. 개정 사실을 커밋 메시지에 명시한다 (`docs: amend constitution to vX.Y.Z`).
- 버전 정책:
  - MAJOR: 원칙 제거 또는 비호환 재정의.
  - MINOR: 새 원칙 추가 또는 새 섹션 추가.
  - PATCH: 문구 수정, 오타 수정, 비의미적 개정.
- 모든 PR은 변경 내용이 이 헌법의 원칙 I~VI를 준수하는지 확인한 후 머지한다.
- 헌법 준수 여부 검토는 `/speckit-analyze` 명령으로 수행할 수 있다.

**Version**: 1.0.0 | **Ratified**: 2026-06-14 | **Last Amended**: 2026-06-14
