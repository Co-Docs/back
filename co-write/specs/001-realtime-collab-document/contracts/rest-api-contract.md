# REST API 계약

**Base URL**: `/api`
**인증**: `Authorization: Bearer {JWT}` (명시된 경우 필수)

---

## 문서 API (`/api/document`)

### GET `/api/document` — 문서 목록 조회
- **인증**: 필수
- **Query**: `page`, `size` (Pageable)
- **Response 200**:
```json
{
  "status": "success",
  "data": {
    "documents": [
      { "documentId": 1, "title": "제목", "updatedAt": "2026-06-14T10:00:00" }
    ]
  }
}
```

### GET `/api/document/title/{title}` — 제목으로 검색
- **인증**: 필수
- **Path**: `title` (검색어)
- **Query**: `page`, `size`
- **Response 200**: 목록 조회와 동일

### GET `/api/document/{documentId}` — 문서 상세 조회
- **인증**: 불필요
- **Response 200**:
```json
{
  "status": "success",
  "data": {
    "documentId": 1,
    "title": "제목",
    "content": "문서 내용",
    "participants": ["닉네임A", "닉네임B"]
  }
}
```

### POST `/api/document` — 문서 생성
- **인증**: 필수
- **Request Body**:
```json
{
  "title": "새 문서",
  "password": null,
  "userDocuments": ["username1", "username2"]
}
```
- **Response 200**:
```json
{ "status": "success", "data": 123456789 }
```

### PATCH `/api/document/{documentId}` — 문서 수정 (제목/내용)
- **인증**: 필수 (Redisson 락 적용)
- **Request Body**:
```json
{ "title": "수정된 제목", "content": "수정된 내용" }
```
- **Response 200**:
```json
{ "status": "success", "data": 123456789 }
```

### PUT `/api/document/{documentId}` — 참여자 추가
- **인증**: 필수 (Redisson 락 적용)
- **Request Body**:
```json
{ "username": "newuser@email.com" }
```
- **Response 200**:
```json
{ "status": "success", "data": "닉네임" }
```

### DELETE `/api/document/{documentId}` — 문서 삭제 (소유자)
- **인증**: 필수
- **동작**: 소유자가 문서 전체 삭제. 편집 중인 참여자에게 WebSocket 삭제 알림 전송 후 연결 종료.
- **Response 200**:
```json
{ "status": "success", "data": null }
```

### DELETE `/api/document/{documentId}/leave` — 문서 나가기 (참여자) *(신규)*
- **인증**: 필수
- **동작**: 본인을 참여자 목록에서 제거 (문서는 유지됨)
- **Response 200**:
```json
{ "status": "success", "data": null }
```

---

## 사용자 API (`/api/user`)

### GET `/api/user` — 내 정보 조회
- **인증**: 필수
- **Response 200**: 사용자 정보 (userId, email, nickname)

### POST `/api/user/register` — 회원가입
- **Request Body**: `{ "email": "", "password": "", "nickname": "" }`
- **Response 200**: 생성된 userId

### POST `/api/user/login` — 로그인
- **Request Body**: `{ "username": "", "password": "" }`
- **Response 200**: `{ "accessToken": "", "refreshToken": "" }`

### POST `/api/user/find-id` — 아이디 찾기
- **Request Body**: `{ "nickname": "" }`

### POST `/api/user/find-pw` — 비밀번호 재설정 (1단계: 이메일 확인)
- **Request Body**: `{ "email": "" }`

### POST `/api/user/find-pw/reset` — 비밀번호 재설정 (2단계: 새 비밀번호 설정)
- **Request Body**: `{ "email": "", "newPassword": "" }`

---

## 공통 에러 응답

```json
{
  "status": "error",
  "errorCode": "DOCS_NOT_FOUND",
  "message": "해당 문서를 찾을 수 없습니다."
}
```

**주요 에러 코드**:
- `DOCS_NOT_FOUND`: 문서 없음
- `UNABLE_TO_OBTAIN_LOCK`: 분산 락 획득 실패 (재시도 3회 후)
- `OWNER_CANNOT_BE_PARTICIPANT`: 소유자를 참여자로 추가 시도
