# CUTY 운영 배포 체크리스트

## DNS와 HTTPS

1. EC2에 고정 IP를 연결하고 도메인의 A 레코드를 해당 IP로 지정한다.
2. `deploy/Caddyfile.example`의 `CUTY_DOMAIN` 환경변수에 최종 도메인을 넣는다.
3. 외부에는 80/443만 열고 Spring 8080과 MySQL 3306은 공개하지 않는다.
4. `https://<도메인>/actuator/health`와 브라우저 마이크 권한을 확인한다.

## 서버 환경변수

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- 충분히 긴 Base64 `JWT_SECRET` (Voice Render에도 같은 값을 Secret으로 등록)
- 필요할 때만 `CORS_ALLOWED_ORIGINS=https://<도메인>`
- 실제 문자 사용 시에만 `SMS_API_KEY`, `SMS_API_SECRET`, `SMS_SENDER`

## 외부 서비스 설정

- Client 저장소 Secret `SERVER_REPO_TOKEN`: 서버 저장소 `repository_dispatch` 최소 권한
- Render Secret `GROQ_API_KEY`, `JWT_SECRET`, `CORS_ORIGINS=https://<도메인>`
- Render `ENVIRONMENT=production`

## 배포 후 확인

- 로그인, 토큰 갱신, 상품 검색, 견적, 예약, Mock 결제, 취소가 동작해야 한다.
- Voice 요청은 로그인 상태에서만 성공하고, 비로그인·만료 토큰은 401이어야 한다.
- Swagger 경로는 운영 프로필에서 노출되지 않아야 한다.
- MySQL 백업과 복구를 실제로 한 번 검증한다.
