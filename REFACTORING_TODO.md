# 리팩터링 할일 목록

## 우선순위: 높음 🔴

### 1. GamePanel 거대 클래스 분해
**현재 상태**: GamePanel.java - 2560줄 (여전히 방대함)

**문제점**:
- 게임 로직, 이벤트 처리, UI 관리가 모두 한 클래스에 존재
- 테스트 및 유지보수 어려움
- 새로운 기능 추가 시 복잡도 증가

**분리 대상**:
```
GamePanel (현재 2560줄)
├─ GameLogicController (~800줄)
│  ├─ 플레이어 이동/공격 로직
│  ├─ 스킬 사용 로직
│  ├─ 충돌 감지
│  └─ 라운드 관리
├─ MapManager (~400줄)
│  ├─ 맵 로딩
│  ├─ 장애물 관리
│  └─ 에디터 모드
├─ SkillManager (~300줄)
│  ├─ 스킬 시스템
│  ├─ 이펙트 관리
│  └─ 쿨다운 관리
└─ UIController (~200줄)
   ├─ 채팅 UI
   ├─ 메뉴바
   └─ 키 입력 처리
```

**예상 효과**:
- 각 클래스 500줄 이하로 관리 가능
- 단위 테스트 가능
- 책임 명확화

---

### 2. GameServer 거대 클래스 분해
**현재 상태**: GameServer.java - 1192줄

**문제점**:
- 네트워크 처리, 게임 로직, 라운드 관리가 혼재
- 새로운 캐릭터/스킬 추가 시 복잡도 증가
- 테스트 어려움

**분리 대상**:
```
GameServer (현재 1192줄)
├─ ServerNetworkManager (~300줄)
│  ├─ 클라이언트 연결 관리
│  ├─ 메시지 브로드캐스트
│  └─ 프로토콜 처리
├─ GameLogicManager (~400줄)
│  ├─ 플레이어 상태 관리
│  ├─ 충돌 판정
│  ├─ 데미지 계산
│  └─ 킬/데스 처리
├─ RoundManager (~200줄)
│  ├─ 라운드 시작/종료
│  ├─ 스코어 관리
│  └─ 게임 초기화
├─ ObjectManager (~200줄)
│  ├─ 터렛/지뢰 관리
│  ├─ 에어스트라이크
│  └─ 버프/디버프
└─ CharacterValidator (~100줄)
   ├─ 캐릭터 선택 검증
   └─ 스킬 사용 검증
```

**예상 효과**:
- 각 매니저별 독립적 테스트 가능
- 새로운 기능 추가 용이
- 버그 추적 쉬워짐

---

### 3. 사용하지 않는 코드/필드 정리
**발견된 문제**:

#### GamePanel.java
```java
// 사용되지 않는 필드들
private static final int VISION_RANGE = ...  // 삭제
private static final float PIPER_MARK_RANGE_FACTOR = 1.7f;  // 삭제
private static final int PIPER_THERMAL_DOT_SIZE = 10;  // 삭제
private static final int MAX_ROUNDS = 3;  // 삭제
float attackSpeedMultiplier = 1.0f;  // 삭제 또는 구현

// 사용되지 않는 메서드
void loadPlayerSprites(PlayerData player, String characterId)  // 삭제
void returnToLobby()  // 삭제 또는 구현

// 읽히지 않는 필드들 (GameState로 이동 또는 삭제)
ActiveEffect.abilityId, type, color
PlacedObjectClient.type, maxHp
PlayerData.kills, deaths, direction, x, y, createdAt
```

#### GameServer.java
```java
// final로 변경 가능한 필드들
private ServerSocket serverSocket;  // → private final ServerSocket
private Map<String, ClientHandler> clients;  // → private final Map
// ... 외 8개

// 읽히지 않는 필드들
PlacedObject.createdAt  // 삭제 또는 TTL 구현
ScheduledStrike.id, impactAt, team  // 사용 또는 삭제
```

**작업**:
- [ ] 미사용 필드 제거
- [ ] 미사용 메서드 제거 또는 구현
- [ ] final로 변경 가능한 필드 수정
- [ ] 읽히지 않는 필드 용도 확인 후 제거/구현

---

## 우선순위: 중간 🟡

### 4. 에러 처리 개선

#### 문제점
```java
// 너무 광범위한 Exception 처리
catch (Exception e) {  // ❌
    e.printStackTrace();  // ❌ 로깅 시스템 없음
}
```

**개선 방안**:
- [ ] 구체적 예외 처리 (IOException, SocketException 등)
- [ ] 로깅 프레임워크 도입 (SLF4J + Logback)
- [ ] printStackTrace() 제거
- [ ] 예외 복구 전략 수립

**적용 위치**:
- GameServer.java: 3곳
- GamePanel.java: 1곳

---

### 5. Switch 문 현대화

#### 문제점
```java
// 구식 switch 문
switch (command) {
    case "JOIN":
        // ...
        break;
    case "MOVE":
        // ...
        break;
}
```

**개선**:
```java
// 모던 switch 표현식 (Java 14+)
switch (command) {
    case "JOIN" -> handleJoin(data);
    case "MOVE" -> handleMove(data);
    case "SHOOT" -> handleShoot(data);
    default -> System.err.println("Unknown command: " + command);
}
```

**적용 위치**:
- GameServer.java: ClientHandler.run()
- GamePanel.java: 캐릭터 로딩 2곳

---

### 6. 중복 코드 제거

#### 스케일 계산 중복
**GamePanel.java** - 3곳에서 동일한 스케일 계산 반복
```java
// mousePressed, mouseMoved, mouseDragged 모두 동일
double scaleX = (double) getWidth() / GameConstants.GAME_WIDTH;
double scaleY = (double) getHeight() / GameConstants.GAME_HEIGHT;
```

**개선**:
```java
private Point getScaledMousePosition(MouseEvent e) {
    double scaleX = (double) getWidth() / GameConstants.GAME_WIDTH;
    double scaleY = (double) getHeight() / GameConstants.GAME_HEIGHT;
    return new Point(
        (int) (e.getX() / scaleX),
        (int) (e.getY() / scaleY)
    );
}
```

#### Integer.parseInt 중복
```java
// 패턴: Integer.parseInt(regex.group(1))
// 발생 위치: 3곳 (mw, mh, ts)
```

**개선**: 유틸리티 메서드 추가
```java
private int parseIntFromMatch(Matcher matcher, int group) {
    return Integer.parseInt(matcher.group(group));
}
```

---

### 7. 매직 넘버 상수화

#### 발견된 매직 넘버들
```java
// GamePanel.java
200  // 미니맵 너비
150  // 미니맵 높이
250  // 채팅 패널 너비
800  // 창 높이
1150 // 캔버스 너비

// GameServer.java
180  // TURRET_RANGE (이미 상수화됨 ✓)
900  // TURRET_ATTACK_INTERVAL (이미 상수화됨 ✓)
2    // MAX_WINS (이미 상수화됨 ✓)
```

**작업**:
- [ ] GamePanel에 UI 상수 추가
```java
private static final int MINIMAP_WIDTH = 200;
private static final int MINIMAP_HEIGHT = 150;
private static final int CHAT_PANEL_WIDTH = 250;
private static final int WINDOW_HEIGHT = 800;
private static final int CANVAS_WIDTH = 1150;
```

---

### 8. 데이터 클래스 불변성 강화

#### 문제점
```java
// 내부 클래스들이 public 필드 사용
class PlayerData {
    String name;  // public by default in package
    int x, y;
    int team;
    // ...
}
```

**개선**:
- [ ] 필드를 private final로 변경
- [ ] getter 메서드 추가
- [ ] 생성자로만 초기화
- [ ] record 타입 고려 (Java 16+)

**적용 대상**:
- PlayerData
- PlacedObjectClient
- Missile
- StrikeMarker
- ActiveEffect

---

## 우선순위: 낮음 🟢

### 9. InputController 활용도 개선

**현재 상태**: InputController가 있지만 GamePanel에서 직접 KeyListener 구현

**개선**:
- [ ] InputController를 완전히 활용
- [ ] GamePanel에서 KeyListener 코드 제거
- [ ] 키 바인딩 설정을 InputController로 통합

---

### 10. Protocol 클래스 개선

**현재**: Protocol.java가 비어있거나 미사용

**개선 방안**:
```java
public class Protocol {
    // 프로토콜 상수 정의
    public static final String JOIN = "JOIN";
    public static final String MOVE = "MOVE";
    public static final String SHOOT = "SHOOT";
    // ... 25개 프로토콜
    
    // 메시지 파싱 유틸리티
    public static Message parse(String rawMessage) { ... }
    public static String format(String type, String... args) { ... }
}
```

---

### 11. 테스트 코드 작성

**현재 상태**: 단위 테스트 없음

**우선순위 테스트 대상**:
1. **GameState** - 상태 관리 로직
2. **NetworkClient** - 메시지 송수신
3. **GameMessageHandler** - 프로토콜 파싱
4. **CharacterData** - 캐릭터 데이터 검증

**테스트 프레임워크**: JUnit 5 + Mockito

---

### 12. 문서화 개선

**현재 상태**: 
- ✅ Javadoc 작성됨 (GameRenderer, NetworkClient, GameState)
- ❌ 일부 클래스 문서화 부족

**작업**:
- [ ] GamePanel 메서드 Javadoc 추가
- [ ] GameServer 메서드 Javadoc 추가
- [ ] 복잡한 로직에 주석 추가
- [ ] API 문서 생성

---

### 13. 성능 최적화

#### 잠재적 최적화 포인트

1. **렌더링 최적화**
   - 화면 밖 객체 렌더링 스킵 (일부 구현됨)
   - 더티 플래그 패턴으로 불필요한 렌더링 방지

2. **네트워크 최적화**
   - 메시지 배칭 (여러 메시지를 한 번에 전송)
   - 프로토콜 압축

3. **충돌 감지 최적화**
   - Spatial hashing 도입
   - Quad-tree 자료구조 사용

---

## 장기 목표 🎯

### 14. 아키텍처 패턴 적용

#### Entity-Component-System (ECS) 패턴 고려
현재 OOP 방식에서 ECS로 전환 시 장점:
- 캐릭터/스킬 추가가 매우 쉬워짐
- 메모리 효율성 향상
- 멀티스레딩 용이

#### Event-Driven Architecture
- 게임 이벤트 버스 도입
- 느슨한 결합 (Loose Coupling)
- 플러그인 시스템 가능

---

### 15. 멀티플레이어 확장성

**현재**: 4명 제한

**개선**:
- [ ] 서버 인스턴스 분리 (매치메이킹)
- [ ] 스케일아웃 가능한 구조
- [ ] Redis 등 외부 상태 저장소 사용

---

### 16. 보안 강화

**취약점**:
- [ ] 클라이언트 입력 검증 부족
- [ ] 치트 방지 로직 없음
- [ ] 네트워크 암호화 없음

**개선**:
- 서버 측 검증 강화
- 안티-치트 시스템
- TLS/SSL 암호화

---

## 작업 순서 제안

### Phase 1: 코드 품질 개선 (1-2주)
1. 미사용 코드 제거 (#3)
2. 에러 처리 개선 (#4)
3. 중복 코드 제거 (#6)
4. 매직 넘버 상수화 (#7)

### Phase 2: 구조 개선 (2-3주)
5. GamePanel 분해 (#1)
6. GameServer 분해 (#2)
7. Switch 문 현대화 (#5)

### Phase 3: 안정성 향상 (1-2주)
8. 데이터 클래스 불변성 (#8)
9. 테스트 코드 작성 (#11)
10. 문서화 개선 (#12)

### Phase 4: 최적화 (지속적)
11. 성능 최적화 (#13)
12. 보안 강화 (#16)

---

## 측정 지표

### 현재 상태
- **총 라인 수**: ~8,000줄
- **평균 클래스 크기**: ~500줄
- **최대 클래스 크기**: 2560줄 (GamePanel)
- **테스트 커버리지**: 0%
- **Cyclomatic Complexity**: 높음 (추정)

### 목표 (Phase 3 완료 후)
- **총 라인 수**: ~10,000줄 (테스트 포함)
- **평균 클래스 크기**: ~300줄
- **최대 클래스 크기**: <800줄
- **테스트 커버리지**: >60%
- **Cyclomatic Complexity**: 중간

---

## 참고 사항

### 이미 완료된 리팩터링 ✅
1. ✅ GameRenderer 분리 (Phase 1)
2. ✅ GameState 통합 (Phase 2)
3. ✅ NetworkClient 분리
4. ✅ GameMessageHandler 분리
5. ✅ 고정 해상도 렌더링 시스템
6. ✅ 프로토콜 핸들러 완성 (25개)
7. ✅ **Phase 1: 코드 품질 개선** (2025-12-03)
   - 에러 처리 구체화 (IOException, SocketException)
   - GameServer final 키워드 추가
   - 중복 코드 제거 (scaleMouseCoordinates)
   - 매직 넘버 상수화 (UI 상수들)

### 진행 중인 작업 🔄
- **Phase 2: 구조 개선** (진행 중)
  - MapManager 클래스 생성 완료 (~600줄)
  - GamePanel 통합 대기 중 (복잡도로 인해 단계적 접근 필요)

### 변경 시 주의사항
- 기존 게임 로직 동작 유지 (후방 호환성)
- 네트워크 프로토콜 변경 시 클라이언트/서버 동시 업데이트
- 리팩터링 전후 테스트 필수
- 작은 단위로 커밋 (원자적 변경)
