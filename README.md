# 🍽️ MuMuk (오늘 뭐 해먹지?)

> 내 건강 데이터를 기반으로 한 1:1 맞춤 식단 큐레이션 서비스

사용자의 건강 정보, 알레르기, 보유 식재료 등을 분석하여 개인 맞춤형 레시피를 추천합니다.

<br>

## 🏗️ System Architecture

```
[GitHub Push to main]
        ↓ (Webhook)
[Jenkins - 홈서버 자체 호스팅]
  → Gradle Build → Docker Build → Auto Deploy
        ↓
[홈서버 (Ubuntu 24.04)]
  ┌─────────────────────────────────────────┐
  │  Nginx (SSL, Reverse Proxy)             │
  │  Spring Boot API  ← PostgreSQL, Redis   │
  │  Prometheus + Grafana + Loki (모니터링)  │
  └─────────────────────────────────────────┘
```

- **홈서버 직접 구축**: 클라우드(AWS) 대신 개인 홈서버에 전체 인프라를 Docker 기반으로 구성
- **Jenkins CI/CD**: main push 시 자동 빌드 & 배포 (Docker Hub 없이 서버에서 직접 빌드)
- **GitHub Actions CI**: PR 생성 시 빌드/테스트 자동 체크
- **모니터링**: Prometheus + Grafana 대시보드, Loki + Promtail 로그 수집

<br>

## 🛠️ Tech Stack

| 구분 | 기술 |
| --- | --- |
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.3.1 |
| **Database** | PostgreSQL 16, Redis 7 |
| **CI** | GitHub Actions |
| **CD** | Jenkins (홈서버 자체 호스팅) |
| **Infra** | Docker, Nginx (SSL), 홈서버 (Ubuntu 24.04) |
| **Monitoring** | Grafana, Prometheus, Loki, Promtail |
| **API Docs** | Swagger (SpringDoc OpenAPI) |

<br>

## 📋 API Documentation

- **Swagger UI**: https://wonryeol.asuscomm.com/api/swagger-ui/index.html

<br>

## 👥 Contributors

| <img src="https://avatars.githubusercontent.com/u/188818480?v=4" width="100" height="100"> | <img src="https://avatars.githubusercontent.com/u/220421602?v=4" width="100" height="100"> | | | |
|:---:|:---:|:---:|:---:|:---:|
| [이정렬](https://github.com/Jeong-Ryeol) | [이창훈](https://github.com/Chhun-Lee) | TBD | TBD | TBD |

<br>

## 📌 Convention

### Branch
`컨벤션명/#이슈번호`

### Commit
| 커밋 타입 | 설명 | 예시 |
| --- | --- | --- |
| **Feat** | 새로운 기능 추가 | `[FEAT] #이슈번호: 기능 추가` |
| **Fix** | 버그 수정 | `[FIX] #이슈번호: 오류 수정` |
| **Docs** | 문서 수정 | `[DOCS] #이슈번호: README 수정` |
| **Refactor** | 코드 리팩토링 | `[REFACTOR] #이슈번호: 구조 개선` |
| **Chore** | 빌드/설정 변경 | `[CHORE] #이슈번호: 설정 수정` |
| **Remove** | 파일/폴더 삭제 | `[REMOVE] #이슈번호: 파일 삭제` |
| **Rename** | 파일/폴더명 변경 | `[RENAME] #이슈번호: 이름 변경` |
