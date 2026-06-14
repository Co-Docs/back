# Implementation Plan: 실시간 협업 문서 편집 및 관리 시스템

**Branch**: `001-realtime-collab-document` | **Date**: 2026-06-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-realtime-collab-document/spec.md`

## Summary

실시간 협업 편집을 위한 `core/edit-document`와 문서·사용자 CRUD를 위한 `core/user-and-document` 모듈의
현재 구현을 검토하여 스펙에서 명세된 신규 기능(속도 제한, 편집 실패 알림, 문서 삭제 WebSocket 알림, Kafka 장애 처리)을
추가하고, 식별된 버그(deleteDocument 동작 불일치)와 코드 중복(DocumentRedissonLock)을 수정한다.

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**: Spring Boot 3.x, Spring Kafka, Spring WebSocket(STOMP), Spring Data Redis, Redisson

**Storage**: MySQL (JPA/Hibernate) + Redis (String, ZSet, 분산 락)

**Testing**: JUnit 5 + Mockito (단위 테스트 위주, OT 로직은 실제 구현체 사용)

**Target Platform**: Linux 서버 (WSL2 개발 환경)

**Project Type**: 멀티모듈 웹 서비스 (core:edit-document + core:user-and-document)

**Performance Goals**: 동시 편집자 10명 기준 1초 이내 브로드캐스트

**Constraints**: 사용자당 초당 10회 편집 제한, Redis 연산 이력 최대 200개

**Scale/Scope**: 소규모 팀 협업 (문서당 최대 10명 동시 편집)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **원칙 I (OT 정확성)**: `OperatorRebaseUtil` 구현 및 단위 테스트 존재. 이번 작업은 OT 로직 자체를 변경하지 않음.
- [x] **원칙 II (Kafka 순서)**: `OutboxEventPublisher`에서 documentId를 메시지 키로 사용 확인.
- [x] **원칙 III (Redis-DB 일관성)**: content TTL 5시간, count TTL 12시간 유지. 신규 ratelimit 키(TTL 1초) 추가.
- [x] **원칙 IV (멀티모듈 독립성)**: core 모듈 간 직접 의존 없음. WebSocket 알림은 각 모듈 내부에서 처리.
- [x] **원칙 V (실시간 통신)**: 문서 삭제 알림 및 편집 실패 알림을 이번 작업에서 구현. `../co-write-front` 수정 포함.
- [x] **원칙 VI (테스트)**: 신규 기능(속도 제한, 삭제 알림)에 대한 단위 테스트 작성 계획 포함.

## Project Structure

### Documentation (this feature)

```text
specs/001-realtime-collab-document/
├── plan.md              ← 이 파일
├── spec.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── websocket-contract.md
│   └── rest-api-contract.md
└── tasks.md             ← /speckit-tasks 명령으로 생성
```

### Source Code (수정 대상)

```text
core/edit-document/
├── controller/
│   └── EditDocumentController.java          ← 속도 제한 검사 추가
├── consumer/
│   └── DocumentUpdateEventConsumer.java     ← 편집 실패 시 에러 메시지 전송
├── publisher/
│   └── DocumentUpdatePublisher.java         ← Kafka 장애 감지 및 에러 전파
├── service/dto/
│   └── EditErrorResult.java                 ← 신규: 에러 응답 DTO
└── utils/
    └── RateLimitUtil.java                   ← 신규: Redis 기반 속도 제한

core/user-and-document/
├── controller/
│   └── DocumentController.java              ← DELETE 엔드포인트 분리 (삭제 vs 나가기)
├── service/
│   └── DocumentService.java                 ← deleteDocument 버그 수정 + WebSocket 알림 추가
└── facade/
    └── DocumentRedissonLock.java            ← 코드 중복 제거 (executeWithLock 헬퍼)

../co-write-front/
└── (해당 WebSocket 핸들러 파일)             ← DOCUMENT_DELETED 이벤트 및 에러 UI 처리
```

## Complexity Tracking

| 위반 항목 | 이유 | 더 단순한 대안을 선택하지 않은 이유 |
|-----------|------|-------------------------------------|
| Redis ratelimit 키 추가 | FR-012 속도 제한은 분산 환경에서 필수 | 단일 인스턴스 인메모리 카운터는 다중 서버 환경에서 비효율 |
| DocumentService에서 SimpMessagingTemplate 주입 | 삭제 시 WebSocket 알림 전송 필요 | 별도 이벤트 발행보다 직접 주입이 단순하며 단일 트랜잭션 내 처리 가능 |
