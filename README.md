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
- **SPACE**: 미사일 발사

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
