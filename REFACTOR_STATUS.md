# GamePanel 리팩토링 현황

## 브랜치: refactor/gamepanel-mvc

## 완료 항목 ✅

### 1. NetworkClient 분리 및 통합 (완료)
- **파일**: `NetworkClient.java` (169 lines)
- **목적**: Socket I/O 레이어 분리
- **통합 상태**: 100% 완료
- **변경 사항**:
  - 모든 네트워크 전송: `sendPosition()`, `sendShoot()`, `sendChat()`, `sendSkillUse()`, `sendCharacterSelect()`, `sendHitReport()`
  - 수신 스레드: `networkClient.startReceiving()` - 콜백 기반 비동기 처리
  - 프로토콜 파싱: `processGameMessage()` 메서드는 GamePanel에 유지 (게임 로직 결합)
- **테스트**: 게임 내 정상 작동 확인
- **커밋**: `677dbe5` "Integrate NetworkClient"

### 2. 캐릭터 선택 10초 제한 버그 수정 (완료)
- **문제**: 10초 경과 후에도 캐릭터 변경 허용되던 버그
- **해결**:
  1. 시간 체크를 최우선으로 이동 (라운드 상태 체크보다 먼저)
  2. 다이얼로그 종료 후 최종 검증 추가
  3. 라운드 시작 시 `hasChangedCharacterInRound = false` 리셋
- **결과**: 10초 제한 엄격히 적용, 다이얼로그 열림 방지 및 선택 무효화
- **커밋**: `4a376d0` "Fix character selection 10-second time limit enforcement"

---

## 생성된 클래스 (부분 사용)

### 3. GameState.java (437 lines) - 인스턴스 생성됨
- **목적**: 게임 상태 중앙화 관리
- **현재 상태**: GamePanel 생성자에서 인스턴스 생성, 필드 마이그레이션 미완
- **필드**: `playerX/Y`, `cameraX/Y`, `myHP/MaxHP`, `missiles`, `players`, `obstacles`, `placedObjects` 등
- **API**: 전체 getter/setter 구현 완료
- **통합 상태**: ❌ **미완료** - GamePanel의 기존 필드들이 여전히 사용 중
- **향후 작업**: 수백 개의 필드 참조를 `gameState.getXXX()`로 점진적 변경 필요

### 4. GameRenderer.java (680 lines) - 생성만 됨
- **목적**: 렌더링 로직 분리
- **현재 상태**: 클래스 생성, GamePanel에서 인스턴스 생성됨
- **메서드**: `render()`, `drawGrid()`, `drawObstacles()`, `drawHUD()`, `drawMinimap()` 등
- **통합 상태**: ❌ **미완료** - `paintComponent()`가 여전히 GamePanel에 존재 (~480 lines)
- **향후 작업**: `paintComponent()`를 `gameRenderer.render(g2d, ...)`로 대체 필요

### 5. InputController.java (355 lines) - 생성만 됨
- **목적**: 입력 처리 콜백 기반 분리
- **현재 상태**: 클래스만 생성
- **통합 상태**: ❌ **미사용** - 게임 로직과의 긴밀한 결합으로 통합 복잡
- **향후 작업**: 게임 로직 리팩토링 후 통합 가능

---

## 파일 변경 요약

### GamePanel.java (3,806 lines)
- **Line 23-34**: GameState, NetworkClient, GameRenderer 필드 추가
- **Line 1173-1177**: NetworkClient 초기화
- **Line 1937**: `networkClient.startReceiving()` 호출
- **Lines 2353-2365**: 모든 네트워크 전송이 NetworkClient 메서드 사용
- **Lines 3507-3512**: `startRound()`에 `hasChangedCharacterInRound` 리셋 추가
- **Lines 3514-3600**: `openCharacterSelect()` 시간 제한 로직 수정
- **Line 371**: `paintComponent()` 시작 (~480 lines, 아직 분리 안 됨)

---

## 다음 단계 (향후 과제)

### Phase 1: GameRenderer 통합 (우선순위: 높음) - **매우 복잡함**
- `paintComponent()` 메서드(~200 lines)를 `gameRenderer.render()` 호출로 대체
- **도전 과제**:
  - paintComponent가 수십 개의 GamePanel 필드에 직접 의존
  - draw 메서드들 (drawMinimap, drawHUD, drawGrid 등) 모두 내부 상태 사용
  - 모든 draw 메서드를 GameRenderer로 이동하려면 대규모 리팩토링 필요
  - 추정 작업량: **수백 개의 필드 참조 변경, 메서드 이동**
- **권장 접근**:
  - 단계적 접근: 한 번에 하나의 draw 메서드씩 GameRenderer로 이동
  - GameRenderer에 GamePanel 참조 전달하여 필드 접근 가능하게 함
  - 또는 새 기능 개발 시에만 GameRenderer 사용

### Phase 2: GameState 점진적 마이그레이션 (우선순위: 중간)
- 자주 사용되는 필드부터 점진적 마이그레이션: `playerX/Y`, `cameraX/Y`, `missiles`
- 래퍼 패턴 또는 직접 참조 교체
- **도전 과제**: 3,800+ 라인에 걸쳐 수백 개의 필드 참조 존재

### Phase 3: InputController 통합 (우선순위: 낮음, 연기)
- 게임 로직과의 긴밀한 결합으로 복잡한 리팩토링 필요
- 향후 반복 작업으로 연기

---

## 컴파일 및 테스트

### 컴파일
```bash
javac -encoding UTF-8 -d bin -cp ".:lib/*" src/com/fpsgame/client/*.java src/com/fpsgame/common/*.java src/com/fpsgame/server/*.java
```
- **상태**: ✅ 모든 코드 컴파일 성공

### 실행 테스트
- NetworkClient 통합: ✅ 정상 작동
- 캐릭터 선택 버그: ✅ 수정 완료
- 게임 플레이: ✅ 정상 작동

---

## Git 커밋 히스토리

```
4a376d0 (HEAD -> refactor/gamepanel-mvc) Fix character selection 10-second time limit enforcement
111ae41 Add GameRenderer instance to GamePanel, defer full integration
677dbe5 Integrate NetworkClient: replace socket communication with NetworkClient class
dcd1529 (origin/main, main) 라운드 표시 (Round @ ) 가 제대로 나올 수 있게 roundCount를 초기값 0 으로 설정함
```

---

## 결론

### 성공한 부분 ✅
- **NetworkClient 완전 통합**: 네트워크 레이어 깔끔하게 분리, 테스트 완료
  - 모든 send 작업: `sendPosition()`, `sendShoot()`, `sendChat()`, `sendSkillUse()`, `sendCharacterSelect()`, `sendHitReport()`
  - Receive 스레드: `networkClient.startReceiving()` - 콜백 기반 비동기
  - 프로토콜 파싱: `processGameMessage()` 유지 (게임 로직 결합 이유)
  - **실측 효과**: 약 150 라인의 네트워크 코드 분리, 가독성 향상
- **캐릭터 선택 버그 수정**: 10초 제한 엄격 적용, 정상 작동

### 부분 완료 🔄
- **GameState**: 인스턴스 생성 및 API 준비, 필드 마이그레이션 미완
  - 437 라인 클래스 생성, 전체 getter/setter API 구현
  - GamePanel에서 인스턴스 생성됨
  - **미완료 이유**: 수백 개의 필드 참조 변경 필요 (시간 소요 큼)
  
- **GameRenderer**: 클래스 준비, paintComponent 통합 미완
  - 680 라인 클래스 생성, render() 및 모든 draw 메서드 구현
  - GamePanel에서 인스턴스 생성됨
  - **미완료 이유**: 
    - paintComponent가 수십 개의 GamePanel 필드에 직접 의존
    - draw 메서드들이 모두 내부 상태 사용
    - 전체 통합은 대규모 리팩토링 필요 (수백 개 참조 변경)

### 미사용 ❌
- **InputController**: 생성만 됨 (355 라인), 통합 미진행
  - 게임 로직과 긴밀한 결합으로 통합 복잡

### 향후 전략 및 권장사항

#### 단기 (현재 브랜치에서 가능)
1. **작은 메서드부터 점진적 이동**
   - `drawMinimap()`, `drawHUD()` 같은 독립적인 메서드를 GameRenderer로 한 번에 하나씩 이동
   - GameRenderer에 GamePanel 참조를 전달하여 필드 접근 허용
   
2. **새 기능 개발 시 GameRenderer 사용**
   - 향후 새로운 UI/렌더링 기능 추가 시 GameRenderer에 직접 구현
   - 레거시 코드는 그대로 유지

#### 중기 (별도 리팩토링 이터레이션)
3. **GameState 점진적 마이그레이션**
   - 가장 많이 사용되는 필드부터: `playerX/Y`, `cameraX/Y`, `myHP`
   - 한 필드씩 마이그레이션하고 테스트
   
4. **InputController는 보류**
   - 게임 로직 리팩토링 후 재검토

### 학습 내용
- **God Object 리팩토링의 어려움**: 3,800+ 라인의 클래스를 분리하는 것은 단순히 클래스를 만드는 것 이상의 작업
- **의존성의 중요성**: paintComponent 같은 메서드는 수십 개 필드에 의존하여 완전 분리가 매우 어려움
- **점진적 접근의 필요성**: 한 번에 모든 것을 분리하기보다는, 독립적인 부분부터 단계적으로 진행하는 것이 현실적

---

**작성일**: 2025-12-03
**브랜치**: refactor/gamepanel-mvc
**상태**: NetworkClient 통합 완료 ✅, 나머지는 향후 점진적 작업 필요 🔄
