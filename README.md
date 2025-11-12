## Fixes (2025-11-09)

- Fixed basic attack direction when camera is moved:
  - Removed double camera offset so projectiles always follow mouse direction even outside the initial 21x12 area.
- Made Raven dash collision-safe:
  - Dash now advances in 1px increments per frame and stops immediately when a wall is hit (no sliding during dash), preventing wall clipping.

### Map Editing & Shortcuts Guide
인게임 맵 편집 기능과 단축키 사용법은 `MAP_EDIT_GUIDE.md` 문서를 참고하세요.
- 편집 모드(F4)에서 타일을 클릭/드래그로 이동 가능/불가 상태 변경
- Ctrl+S 또는 F5로 현재 맵을 `<mapName>_edited.json` 저장 (자동 우선 로드)
- F6으로 기본 `map` ↔ `airport` 전환 테스트

Notes:
- If you still see odd collision around tight corners, toggle obstacle debug with F3 to visualize blockers.

# FPS Game - Simplified Version

간단한 멀티플레이어 FPS 게임 (학교 프로젝트용)

## 프로젝트 구조

```
NetFps/
├── src/
│   └── com/
│       └── fpsgame/
│           ├── common/          # 공통 모듈
│           │   ├── Protocol.java
│           │   └── GameConstants.java
│           ├── client/          # 클라이언트 모듈
│           │   ├── MainLauncher.java
│           │   ├── LobbyFrame.java
│           │   └── GamePanel.java
│           └── server/          # 서버 모듈
│               └── GameServer.java
└── bin/
```

## 주요 기능

### 맵 시스템 ✨ NEW!
- **대형 맵**: 3200x2400 픽셀 (화면의 약 4배)
- **카메라 시스템**: 플레이어를 따라다니는 스무스 카메라
- **맵 배경**: terminal.png, forestOutpost.png, neonCity.png 지원
- **장애물 시스템**: 박스와 비행기 등 장애물 충돌 감지
- **자유로운 이동**: 넓은 맵을 자유롭게 돌아다닐 수 있음

### 유지된 디자인
✅ MainLauncher의 디자인 - 깔끔한 시작 화면
✅ 로비 디자인 - 서버 연결, 팀 선택, 채팅
✅ GamePanel 디자인 - 간단하고 명확한 게임 화면

### 단순화된 기능
- 복잡한 프로토콜 → 간단한 텍스트 기반 통신
- 10개 캐릭터 → 4명의 플레이어
- 멀티모듈 유지 (common, client, server)
- 서버-클라이언트 분리 유지

## 실행 방법

### 1. 서버 실행
```bash
# Eclipse에서 GameServer.java 실행
# 또는 터미널에서:
java com.fpsgame.server.GameServer
```

기본 포트: 7777

### 2. 클라이언트 실행
```bash
# Eclipse에서 MainLauncher.java 실행
# 또는 터미널에서:
java com.fpsgame.client.MainLauncher
```

여러 클라이언트를 실행하여 최대 4명까지 접속 가능

## 게임 방법

### 조작법
- **W, A, S, D** 또는 **방향키**: 플레이어 이동
- **마우스 좌클릭**: 미사일 발사 (마우스 방향으로 발사)
- **E**: 전술 스킬 사용
- **R**: 궁극기 사용
- **B**: 캐릭터 선택 다이얼로그
- **M**: 미니맵 토글 (켜기/끄기)
- **Enter** 또는 **T**: 채팅 입력

### 미니맵 시스템
- **위치**: 우측 상단
- **시야 범위**: 화면에 보이는 범위 (약 500픽셀)
- **시야 내 적만 표시**: 가까이 있는 적만 미니맵에 표시됨
- **시각적 표시**: 
  - 노란색 점: 나
  - 파란색 점: 팀원 (팀 1)
  - 빨간색 점: 적 (팀 2)
  - 반투명 원: 시야 범위

### 스킬 시스템
- **Raven 캐릭터**:
  - 기본 공격: 고속 연사
  - E (전술): 대쉬 - 0.5초간 빠른 이동
  - R (궁극기): 과충전 - 6초간 공격 속도 증가 + 투사체 속도 증가

### 게임 흐름
1. 플레이어 이름 입력
2. 서버 연결 (기본: 127.0.0.1:7777)
3. Red Team 또는 Blue Team 선택
4. "Start Game" 버튼 클릭
5. 게임 시작!

## 프로젝트 특징

### 간단한 구조
- 예제 코드(ShootingGame1)처럼 단순한 로직
- 복잡한 캐릭터 시스템 제거
- 기본적인 소켓 통신만 사용

### 학교 프로젝트에 적합
- 쉽게 이해할 수 있는 코드
- 명확한 클래스 분리
- 주석과 문서화

### 확장 가능
- 새로운 기능 추가 용이
- 멀티모듈 구조로 유지보수 편리

## 기술 스택

- **언어**: Java
- **UI**: Swing
- **네트워크**: Socket (TCP)
- **구조**: 멀티모듈 (common, client, server)

## 개발자

리팩토링: 복잡한 프로젝트를 학교 프로젝트 규모로 단순화
원본: https://github.com/khy1121/fpsGame

## 라이선스

교육 목적 프로젝트
