## Co-Write (실시간 공유 문서 편집)

실시간으로 여러 사용자가 같은 문서를 편집할 수 있는 경량 문서 편집 플랫폼입니다.
Spring Boot + Kafka + Redis + STOMP(WebSocket) 아키텍처를 사용해, 문서별 버전 으로 정합성을 보장하고, 리베이스 규칙으로 지연 도착 이벤트를 자연스럽게 해결합니다.

### ✨ 주요 기능

문서 동시 편집: WebSocket(STOMP) 기반 실시간 반영

정합성 보장: serverVersion 워터마크 + Redis 정규화 저장

리베이스(OT-lite): 뒤늦게 도착한 연산을 선행 연산에 맞게 좌표 보정

중복 처리 방지: operationId(클라 생성)로 idempotency 지원(선택)

확장성 고려: Kafka 파티션을 documentId 키로 사용해 순서 보장 & 수평 확장

IME(한글) 대응: 조합 입력 디바운스 및 커서 보존 처리
