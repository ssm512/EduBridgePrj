# EduBridge 서버

학원 관리(출석·회비·성적·공지·상담·AI·알림) 백엔드 + 관리자/강사/학생/학부모 웹 화면.
Spring Boot 4 · Java 25 · PostgreSQL · MyBatis · Flyway · Spring Security(JWT).

## 준비물

- **JDK 25**
- **PostgreSQL** (DB 하나 생성, 예: `edubridge`)
- 프로젝트 루트에 **`.env`** 파일 (git에 포함되지 않음 — 아래 참고)

## 실행

```bash
# 1) .env 작성 (아래 환경변수 표 참고)
# 2) PostgreSQL 실행 + 빈 DB 준비
./gradlew bootRun          # Flyway가 스키마·초기데이터 자동 적용
```

- 기동되면 **http://localhost:8080** 접속.
- 로그인 후 역할에 따라 이동: `/admin/dashboard`(관리자), `/teacherPage`, `/studentPage`, `/parentPage`.
- REST API 공통 프리픽스: **`/api`** (예: `/api/attendance`, `/api/fees`, `/api/notices`).

빌드해서 jar로 실행:

```bash
./gradlew bootJar
java -jar build/libs/*.jar
```

## 환경변수 (`.env`)

| 키 | 설명 | 필수 |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/edubridge` | ✅ |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | DB 접속 계정 | ✅ |
| `JWT_SECRET` | JWT 서명 비밀키(긴 랜덤 문자열) | ✅ |
| `UPLOAD_PATH` | 첨부파일 저장 경로 (예: `D:/edubridge-uploads/edudata`) | ✅ |
| `GEMINI_API_KEY` | AI 기능(리포트/채점)용. 없으면 AI만 비활성 | 선택 |
| `MAIL_ENABLED` / `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` / `MAIL_FROM` | 비밀번호 재설정 메일. 기본은 콘솔 출력(개발용) | 선택 |
| `FCM_ENABLED` / `FCM_CREDENTIALS_PATH` | 푸시 발송. `true` + 서비스계정 JSON 경로일 때만 활성 | 선택 |

> `.env`와 FCM 서비스계정 JSON(`secrets/`)은 **절대 커밋 금지**(이미 `.gitignore` 처리됨).

## 주요 기능

- **출석/비콘·GPS**: BLE 비콘 + GPS + RSSI + 서버시간 다중검증 자동 출석, 출결 이력/통계, 결석 자동화(요일+공휴일 기반 폴링), 활동로그, 출결 CSV 내보내기
- **회비**: 청구/납부/취소/미납, 할인, 월별 매출·상태 통계
- **성적/AI**: 시험·성적 CRUD, 반/과목 평균·추이, Gemini 기반 리포트·자동채점
- **공지/상담**: 대상별 공지·첨부·읽음, 상담 기록
- **알림**: 이벤트 → 저장 → FCM 발송, 알림 이력/읽음
- **설정**: 출석 기준값(GPS 반경/RSSI/허용시간)·공휴일·학원명 등 운영 튜닝
- **대시보드**: 출석·회비·성적·공지 요약

## 앱 연동

모바일 앱(별도 저장소)은 이 서버 주소를 baseUrl로 접속합니다. 실기기 테스트 시 앱 실행에
`--dart-define=API_BASE=http://<이 서버의 LAN IP>:8080`를 넘기거나, 앱 첫 화면에서 서버 주소를 직접 입력합니다.

## 참고 (배포 시)

- 실배포는 **HTTPS 필수**(앱이 평문 차단), 쿠키 `secure(true)`, 프로덕션 계정 시드 분리, 파일 저장소(S3 등) 전환 필요.
- 멀티테넌트: 학원마다 서버·DB를 독립적으로 띄우고(사일로), 앱은 학원을 선택해 접속.
