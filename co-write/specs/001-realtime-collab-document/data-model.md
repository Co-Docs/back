# Data Model: 실시간 협업 문서 편집 및 관리 시스템

## 엔티티 (MySQL / JPA)

### Document

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| documentId | Long | PK, NOT NULL | Snowflake ID |
| title | String | NOT NULL | 문서 제목 |
| password | String | NULLABLE | 문서 비밀번호 (접근 제한용) |
| content | LONGTEXT | NULLABLE | 문서 본문 |
| createdAt | LocalDateTime | NOT NULL | 생성 시각 (BaseEntity) |
| updatedAt | LocalDateTime | NOT NULL | 수정 시각 (BaseEntity) |

**관계**: `UserDocument`(1:N) — 소유자 1명 + 참여자 N명

**상태 전이**:
- 생성 → 편집 중 → 저장됨 (자동, 10회 편집마다)
- 편집 중 → 삭제됨 (소유자 요청 시 → 참여자 전원 알림 후 WebSocket 종료)

---

### User

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| userId | Long | PK, NOT NULL | Snowflake ID |
| email | String | NOT NULL, UNIQUE | 로그인 이메일 |
| password | String | NULLABLE | BCrypt 해시 (소셜 로그인 시 null) |
| nickname | String | NOT NULL | 화면 표시 이름 |
| role | Enum(Role) | NOT NULL | USER / ADMIN |
| loginType | Enum | NOT NULL | LOCAL / GOOGLE |
| createdAt | LocalDateTime | NOT NULL | 생성 시각 |
| updatedAt | LocalDateTime | NOT NULL | 수정 시각 |

---

### UserDocument (연결 테이블)

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long | PK | 자동 생성 |
| user | User | FK, NOT NULL | 참여자 |
| document | Document | FK, NOT NULL | 문서 |

**제약**: 동일 User + Document 조합 중복 불가 (addParticipants에서 Set 체크).
소유자와 참여자 모두 이 테이블에 저장됨 (역할 구분 없음, 소유자 = 최초 등록자).

---

## Redis 캐시 구조 (edit-document 모듈)

| 키 패턴 | 타입 | TTL | 설명 |
|---------|------|-----|------|
| `document::content::{documentId}` | String | 5시간 | 현재 문서 내용 |
| `document::version::{documentId}` | String | 없음 | 현재 서버 버전 (Long) |
| `document::operation::{documentId}` | ZSet | 없음 | 편집 연산 이력 (score=version), 최대 200개 |
| `document::count::{documentId}` | String | 12시간 | 편집 횟수 카운터 |
| `document::lock{documentId}` | (Redisson) | 3초 점유 | 분산 락 |

**신규 추가**:

| 키 패턴 | 타입 | TTL | 설명 |
|---------|------|-----|------|
| `ratelimit::edit::{userId}` | String | 1초 | 사용자별 편집 횟수 카운터 (속도 제한) |

---

## DTO 계층

### 입력 (클라이언트 → 서버)

**DocumentUpdateEventPayload** (WebSocket, edit-document)
```
version: Long          // 클라이언트 기준 버전
operationId: String    // 연산 고유 식별자
operation: Operation   // INSERT 또는 DELETE
  └─ INSERT: targetPosition, insertText, sessionId
  └─ DELETE: targetPosition, operationCount
```

**DocumentRequest** (REST, user-and-document)
```
title: String
password: String (nullable)
userDocuments: List<String> (참여자 username 목록)
```

**DocumentUpdateRequest** (REST PATCH)
```
title: String (nullable)
content: String (nullable)
```

**ParticipantsUpdateRequest** (REST PUT)
```
username: String
```

### 출력 (서버 → 클라이언트)

**EditedResult** (WebSocket 브로드캐스트)
```
editedContent: String   // 편집 후 전체 문서 내용
version: Long           // 새 서버 버전
targetPosition: int     // 적용된 연산 위치
operationId: String     // 원본 연산 식별자
```

**신규: EditErrorResult** (WebSocket 에러 응답)
```
operationId: String     // 실패한 연산 식별자
errorCode: String       // EDIT_FAILED / RATE_LIMIT_EXCEEDED / SERVICE_UNAVAILABLE
message: String         // 사용자 표시 메시지
```

**신규: DocumentDeletedNotification** (WebSocket 삭제 알림)
```
documentId: Long
message: String         // "문서가 삭제되었습니다."
```

**DocumentDetailResponse** (REST)
```
documentId: Long
title: String
content: String
participants: List<String> (nickname 목록)
```

**DocumentPreviewResponse** (REST, 페이징)
```
documents: List<DocumentPreview>
  └─ documentId, title, updatedAt
```
