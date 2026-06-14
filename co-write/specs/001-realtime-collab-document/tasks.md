---
description: "Task list for 실시간 협업 문서 편집 및 관리 시스템"
---

# Tasks: 실시간 협업 문서 편집 및 관리 시스템

**Input**: Design documents from `specs/001-realtime-collab-document/`

**Prerequisites**: plan.md ✅ spec.md ✅ research.md ✅ data-model.md ✅ contracts/ ✅

**Tests**: 미포함 (명시적 요청 없음)

**Organization**: 유저 스토리별 그룹화 — 각 스토리 독립 구현 및 테스트 가능

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Story]**: 해당 유저 스토리 (US1, US2, US3)
- 모든 태스크에 정확한 파일 경로 포함

---

## Phase 1: Setup

**Purpose**: 신규 공통 구조체 생성 — 이후 모든 스토리에서 참조

- [ ] T001 `EditErrorResult` record 생성 (`core/edit-document/src/main/java/backend/cowrite/service/dto/EditErrorResult.java`) — 필드: `operationId: String`, `errorCode: String`, `message: String`
- [ ] T002 [P] `RateLimitUtil` 클래스 생성 (`core/edit-document/src/main/java/backend/cowrite/utils/RateLimitUtil.java`) — Redis `increment` + `expire(1초)`로 사용자당 초당 10회 제한 구현

**Checkpoint**: T001, T002 완료 후 US1 구현 시작 가능

---

## Phase 2: Foundational (공통 인프라 검증)

**Purpose**: 기존 핵심 플로우 동작 확인 — US1~US3 모두 이 위에서 동작

- [ ] T003 기존 OT 편집 플로우 end-to-end 동작 확인: WebSocket 연결 → `/app/{documentId}` 발행 → Kafka 처리 → `/topic/{documentId}` 수신 정상 동작 검증 (quickstart.md 시나리오 1 기준)
- [ ] T004 [P] 기존 자동 저장 플로우 동작 확인: 10회 편집 후 MySQL `document.content` 저장 검증 (quickstart.md 시나리오 2 기준)

**Checkpoint**: Foundation 검증 완료 — US1~US3 병렬 진행 가능

---

## Phase 3: User Story 1 - 실시간 동시 편집 (Priority: P1) 🎯 MVP

**Goal**: 편집 실패 알림, 속도 제한, Kafka 장애 처리를 추가하여 실시간 편집의 견고성 확보

**Independent Test**: quickstart.md 시나리오 3(속도 제한), 시나리오 5(재연결 복구)로 독립 검증

### Implementation for User Story 1

- [ ] T005 [US1] `EditDocumentController` 수정: `@MessageMapping("/{documentId}")` 핸들러에 `RateLimitUtil` 주입 및 속도 초과 시 `SimpMessagingTemplate`으로 `EditErrorResult(errorCode="RATE_LIMIT_EXCEEDED")` 전송 (`core/edit-document/src/main/java/backend/cowrite/controller/EditDocumentController.java`)
- [ ] T006 [US1] `DocumentUpdatePublisher` 수정: `outboxEventPublisher.publish()` 호출을 try-catch로 감싸고 Kafka 발행 실패 시 `RuntimeException`을 던져 호출부에서 에러 응답 전송 가능하도록 변경 (`core/edit-document/src/main/java/backend/cowrite/publisher/DocumentUpdatePublisher.java`)
- [ ] T007 [US1] `DocumentUpdateEventConsumer` 수정: `processDocumentUpdate()`의 catch 블록에서 `SimpMessagingTemplate.convertAndSend("/topic/{documentId}/errors", EditErrorResult)`로 해당 문서 구독자에게 에러 전송 — 단, 에러 대상을 특정 세션으로 한정하는 방식으로 구현 (`core/edit-document/src/main/java/backend/cowrite/consumer/DocumentUpdateEventConsumer.java`)
- [ ] T008 [P] [US1] 프론트엔드 WebSocket 에러 핸들러 추가: `/topic/{documentId}/errors` 구독 추가 및 `RATE_LIMIT_EXCEEDED`, `EDIT_FAILED`, `SERVICE_UNAVAILABLE` 에러 코드별 사용자 알림 UI 처리 (`..front/co-write-front/src/` 내 WebSocket 관련 파일)

**Checkpoint**: US1 완료 — 속도 초과 및 편집 실패 시 사용자에게 알림이 표시됨

---

## Phase 4: User Story 2 - 편집 내용 자동 저장 (Priority: P2)

**Goal**: 기존 구현이 스펙과 일치하는지 검증 및 이상 발견 시 수정

**Independent Test**: quickstart.md 시나리오 2 — 10회 편집 후 MySQL 저장 확인

### Implementation for User Story 2

- [ ] T009 [US2] `DocumentUpdateCounter` 동작 검토: 10회 임계값(`THRESHOLD_COUNT=10`), 리셋 로직, TTL(12시간) 정상 동작 확인. 이상 발견 시 `DocumentUpdateCounter.java` 수정 (`core/edit-document/src/main/java/backend/cowrite/service/DocumentUpdateCounter.java`)
- [ ] T010 [P] [US2] `DocumentSaveEventConsumer` DLQ 연동 확인: 저장 실패 메시지가 `document-update-dlq` 토픽으로 이동하는지 확인. `DocumentUpdateEventDLQConsumer`에 저장 실패 로그 외 추가 처리 필요 여부 검토 (`core/edit-document/src/main/java/backend/cowrite/consumer/DocumentUpdateEventDLQConsumer.java`)

**Checkpoint**: US2 완료 — 자동 저장이 안정적으로 동작함

---

## Phase 5: User Story 3 - 문서 생성 및 관리 (Priority: P3)

**Goal**: deleteDocument 버그 수정, 문서 삭제 시 WebSocket 알림, DELETE 엔드포인트 분리

**Independent Test**: quickstart.md 시나리오 4(삭제 알림), 시나리오 6(CRUD) 순서대로 실행

### Implementation for User Story 3

- [ ] T011 [US3] `DocumentService` 버그 수정: `deleteDocument(String username, Long documentId)`가 `subtractParticipant`를 호출하는 것을 `leaveDocument(String username, Long documentId)`로 메서드명 변경하여 역할 명확화. 실제 문서 삭제 메서드 `deleteDocument(Long documentId)`는 유지 (`core/user-and-document/src/main/java/backend/cowrite/service/DocumentService.java`)
- [ ] T012 [US3] `DocumentService.deleteDocument(Long documentId)` 수정: 삭제 전 `SimpMessagingTemplate.convertAndSend("/topic/{documentId}", DocumentDeletedNotification)`으로 편집 중인 참여자 전원에게 삭제 알림 전송 후 문서 삭제 처리 (`core/user-and-document/src/main/java/backend/cowrite/service/DocumentService.java`)
- [ ] T013 [US3] `DocumentController` 수정: 기존 `DELETE /{documentId}` 엔드포인트를 소유자 전용 문서 삭제로 유지하고, 신규 `DELETE /{documentId}/leave` 엔드포인트 추가하여 `documentService.leaveDocument(username, documentId)` 호출 (`core/user-and-document/src/main/java/backend/cowrite/controller/DocumentController.java`)
- [ ] T014 [P] [US3] 프론트엔드 문서 삭제 알림 처리: `/topic/{documentId}` 구독에서 `type == "DOCUMENT_DELETED"` 메시지 수신 시 편집 화면 종료 및 문서 목록 페이지로 이동 처리 (`../co-write-front/src/` 내 편집 페이지 관련 파일)

**Checkpoint**: US3 완료 — 삭제 시 참여자에게 알림이 전송되고, 엔드포인트 역할이 명확히 분리됨

---

## Phase 6: Polish & 코드리뷰

**Purpose**: 코드 중복 제거, 헌법 준수 최종 검토

- [ ] T015 `DocumentRedissonLock` 리팩토링: `updateDocument()`와 `updateParticipants()`의 중복 락 획득 로직을 `private <T> T executeWithLock(Long documentId, Supplier<T> action)` 헬퍼로 추출 (`core/user-and-document/src/main/java/backend/cowrite/facade/DocumentRedissonLock.java`)
- [ ] T016 [P] 헌법 코드리뷰 체크리스트 수행 (`.specify/memory/constitution.md` 기준): 정확성·공통 로직·리팩토링·프론트엔드 연동·보안·성능 항목 전체 검토. 이상 발견 시 해당 파일 수정
- [ ] T017 [P] quickstart.md 전체 시나리오 수동 검증 (시나리오 1~6 순서대로 실행하여 기대 결과 확인)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 즉시 시작 가능
- **Phase 2 (Foundation 검증)**: Phase 1 완료 후
- **Phase 3 (US1)**: Phase 1 완료 후 (T001, T002 필요) — Phase 2와 병렬 가능
- **Phase 4 (US2)**: Phase 2 완료 후 — Phase 3과 병렬 가능
- **Phase 5 (US3)**: Phase 2 완료 후 — Phase 3, 4와 병렬 가능
- **Phase 6 (Polish)**: Phase 3, 4, 5 모두 완료 후

### User Story Dependencies

- **US1 (P1)**: T001, T002 완료 후 시작 가능. US2, US3와 독립
- **US2 (P2)**: Phase 2 완료 후 시작 가능. US1, US3와 독립
- **US3 (P3)**: Phase 2 완료 후 시작 가능. US1, US2와 독립

### 파일별 의존성

- T005 depends on T001 (EditErrorResult), T002 (RateLimitUtil)
- T007 depends on T001 (EditErrorResult)
- T006 depends on T005 (순서 고려)
- T012 depends on T011 (메서드명 변경 후 알림 추가)
- T013 depends on T011 (leaveDocument 메서드 존재해야 함)
- T015 depends on T013 (최종 상태에서 리팩토링)

---

## Parallel Opportunities

### Phase 1 병렬 실행

```
T001 (EditErrorResult.java 생성)
T002 (RateLimitUtil.java 생성)          ← 동시에 실행 가능
```

### Phase 3 (US1) 내 병렬 실행

```
T006 (DocumentUpdatePublisher 수정)
T008 (프론트엔드 에러 핸들러)            ← T005 완료 후 T006, T008 동시에 실행 가능
```

### Phase 5 (US3) 내 병렬 실행

```
T011 완료 후:
T012 (DocumentService 삭제 알림)
T014 (프론트엔드 삭제 핸들러)            ← 동시에 실행 가능
T013 (DocumentController 엔드포인트)     ← T011 완료 후 바로 시작 가능
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Phase 1: T001, T002 생성
2. Phase 3: T005 → T006, T007 → T008
3. **STOP and VALIDATE**: quickstart.md 시나리오 3 실행 (속도 제한 검증)
4. MVP 완료: 실시간 편집이 견고하게 동작함

### Incremental Delivery

1. Phase 1 + US1 → 편집 견고성 확보 (MVP)
2. Phase 4 + US2 → 자동 저장 검증
3. Phase 5 + US3 → 문서 관리 버그 수정 및 삭제 알림
4. Phase 6 → 코드 품질 향상

---

## Notes

- **프론트엔드 경로**: `../front/co-write-front/src/` 내 실제 파일 경로는 해당 저장소 구조에 맞게 조정 필요
- [P] 태스크 = 다른 파일, 의존성 없음 → 병렬 실행 가능
- [Story] 레이블 = 해당 유저 스토리와 직결된 태스크
- 각 Phase의 Checkpoint에서 해당 스토리가 독립적으로 동작하는지 확인 후 다음 Phase 진행
- T011 수정 전 기존 `deleteDocument(String username, Long documentId)` 호출처 전체 검색 필요
