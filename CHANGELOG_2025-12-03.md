# 변경 사항 (2025-12-03)

## 1. 프로토콜 메시지 핸들러 완성

### 추가된 핸들러 (7개)
- `WELCOME`: 서버 접속 환영 메시지
- `TEAM_ROSTER`: 팀 구성원 목록
- `SHOOT`: 총구 섬광 이펙트
- `OBJ_UPDATE`: 오브젝트(터렛/지뢰) 상태 업데이트
- `UNBUFF`: 버프/디버프 제거
- `ROUND_WIN`: 라운드 승리 처리
- `GAME_OVER`: 게임 종료 처리

### 수정된 핸들러
- `TURRET_SHOOT`: 터렛 소유자(owner) 정보 추가

**총 25개 메시지 타입 처리 완료**

---

## 2. 터렛/지뢰 공격 판정 수정

### 문제점
- 터렛이 자기 소유자도 공격함
- 터렛 데미지가 캐릭터 공격력으로 계산됨
- 자기 터렛 미사일에 피격됨

### 해결
**GameServer.java**
- `checkTurretTargets()`: 소유자 체크 추가
  ```java
  if (ch.playerName.equals(obj.owner)) continue;
  ```
- `HITME` 처리: 터렛 데미지 고정 20으로 분리
  ```java
  int dmg = isTurretDamage ? 20 : resolveBasicDamage(...);
  ```

**GamePanel.java**
- `checkCollisions()`: 터렛 소유자 미사일 방어 로직 추가
  ```java
  if (ownerInfo.startsWith("TURRET:")) {
      String turretOwner = ownerInfo.substring(7);
      if (turretOwner.equals(playerName)) continue;
  }
  ```

---

## 3. 캐릭터 HIT 판정 수정

### 문제점
- 내 미사일이 적과 충돌해도 서버에 HIT 메시지 전송 안 함
- 서버에서 킬 메시지가 반대로 표시됨 (피격자가 공격자를 처치)

### 해결
**GamePanel.java - checkCollisions()**
```java
// 충돌 시 서버에 HIT 메시지 전송
if (m.team == team && m.owner != null && m.owner.equals(playerName)) {
    networkClient.sendHitReport("HIT:" + targetName);
}
```

**GameServer.java - HIT 처리**
```java
// 킬 메시지 수정: "공격자가 피격자를 처치"
broadcast("CHAT:" + playerName + " 님이 " + hitPlayer + " 님을 처치", null);
```

---

## 4. 게임 상태 초기화 시스템

### 추가된 기능
**GameServer.java**
- `resetGameState()` 메서드 추가
  - 라운드 카운트 초기화
  - 팀 승수 초기화
  - 모든 플레이어 상태 초기화
  - 설치된 오브젝트 초기화

- `GAME_OVER` 후 10초 뒤 자동 초기화

### 캐릭터 선택 검증 강화
- `JOIN` 처리: 빈 이름/캐릭터 ID 거부
- `CHARACTER_SELECT`: 
  - 빈 ID 체크
  - `CharacterData.getById()` 검증
  - 존재하지 않는 캐릭터 거부

---

## 5. 중앙 메시지 표시 수정

### 문제점
- "x 라운드 x 팀 win" 등 중앙 메시지가 안 보임
- 10초 카운트가 사라짐
- 레드팀 승리 시에도 블루팀 승리로 표시됨

### 해결
**GameMessageHandler.java**
- `handleRoundWin()`: 팀 ID 처리 수정
  ```java
  // "0" = RED, "1" = BLUE (GameConstants 참조)
  String winTeamName = winner.equals("0") ? "레드" : "블루";
  ```
- `handleRoundStart()`: centerMessage 초기화로 10초 카운트 표시

**GameRenderer.java**
- 렌더링 우선순위 조정
  ```java
  // WAITING 상태면 10초 카운트 표시
  if (ctx.roundState == GamePanel.RoundState.WAITING) {
      // 카운트 다운
  }
  // WAITING이 아니면 centerMessage 표시 (ROUND_WIN, GAME_OVER)
  else if (!ctx.centerMessage.isEmpty() && ...) {
      // 승리/종료 메시지
  }
  ```

---

## 6. 고정 해상도 렌더링 + 스케일링 시스템

### 목표
창 크기가 변해도 보이는 맵 영역은 동일하게 유지 (형평성 보장)

### 구현
**GameRenderer.java**
```java
// 고정 해상도(1280x720)로 렌더링 후 스케일링
double scaleX = (double) ctx.actualCanvasWidth / GameConstants.GAME_WIDTH;
double scaleY = (double) ctx.actualCanvasHeight / GameConstants.GAME_HEIGHT;
g2d.scale(scaleX, scaleY);
```

**RenderContext**
- `canvasWidth/Height`: 고정 렌더링 크기 (1280x720)
- `actualCanvasWidth/Height`: 실제 창 크기

**마우스 좌표 스케일 보정**
```java
double scaleX = (double) getWidth() / GameConstants.GAME_WIDTH;
double scaleY = (double) getHeight() / GameConstants.GAME_HEIGHT;
mouseX = (int) (e.getX() / scaleX);
mouseY = (int) (e.getY() / scaleY);
```

### 레이아웃 조정
- 게임 캔버스: **1150x800** (스케일링 후 1280x720 영역만 표시)
- 채팅 패널: **250x800** (오른쪽)
- 총 창 크기: **1400x800**
- 채팅 패널과 게임 화면 겹치지 않음

---

## 수정된 파일 목록

### 서버
- `src/com/fpsgame/server/GameServer.java`
  - 터렛 공격 판정 수정
  - 게임 초기화 로직 추가
  - 캐릭터 선택 검증 강화

### 클라이언트
- `src/com/fpsgame/client/GameMessageHandler.java`
  - 25개 메시지 핸들러 추가/수정
  - 팀 ID 처리 수정

- `src/com/fpsgame/client/GamePanel.java`
  - HIT 판정 전송 추가
  - 터렛 미사일 방어 로직
  - 마우스 좌표 스케일 보정
  - 캔버스 크기 조정 (1150x800)

- `src/com/fpsgame/client/GameRenderer.java`
  - 고정 해상도 렌더링 + 스케일링
  - 중앙 메시지 표시 우선순위 조정

---

## 테스트 항목

✅ 터렛/지뢰가 소유자를 공격하지 않음  
✅ 터렛 데미지 고정 20  
✅ 캐릭터 HIT 판정 정상 작동  
✅ 킬 메시지 올바른 방향  
✅ 게임 종료 후 자동 초기화  
✅ 캐릭터 미선택 시 입장 거부  
✅ 중앙 메시지 표시 (라운드 승리, 게임 종료)  
✅ 10초 카운트 다운 표시  
✅ 팀 승리 메시지 정확함 (RED/BLUE)  
✅ 창 크기 변경 시 맵 영역 고정  
✅ 마우스 조작 정상 작동  
✅ 채팅 패널과 게임 화면 분리  

---

## 기술적 개선 사항

### 코드 품질
- 프로토콜 처리 완성도 향상
- 게임 로직 버그 수정
- 형평성 보장 (고정 시야)

### 사용자 경험
- 명확한 메시지 표시
- 안정적인 게임 흐름
- 창 크기 자유로운 조정
- 깔끔한 UI 레이아웃

### 유지보수성
- 스케일링 시스템으로 해상도 관리 용이
- 명확한 팀 ID 처리 (GameConstants 참조)
- 중앙화된 렌더링 로직
