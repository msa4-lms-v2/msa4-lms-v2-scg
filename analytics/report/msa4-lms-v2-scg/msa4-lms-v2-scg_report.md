# msa4-lms-v2-scg 작업 리포트

## 2026-08-14 Access Token 공개키 검증 전환

### 요청

- 2026-08-14 재검수(`docs-v2` 감사)에서 확정한 3.1(P0)을 구현했다. Auth가 RSA 개인키로 서명하도록 바뀌어 SCG도 대응이 필요했다.

### 변경 내용

- `JwtProvider`가 공유 시크릿(`JWT_SECRET`) 대신 RSA 공개키(`JWT_PUBLIC_KEY_B64`)만 보유하도록 바꿨다.
- Access Token 검증 시 서명뿐 아니라 발급자(`issuer`), 대상(`aud=lms-api`), 토큰 종류(`token_type=access`), `kid` 일치 여부를 모두 확인하도록 `extractClaims()`를 강화했다. 하나라도 어긋나면 거부한다.

### 브랜치

- `feature/jwt-public-key-verification`

### 검증

- `compileJava` 통과.
- push는 하지 않았다.
