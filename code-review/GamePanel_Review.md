# GamePanel.java 코드 리뷰

## 📋 파일 개요
- **경로**: `src/com/fpsgame/client/GamePanel.java`
- **역할**: 게임의 메인 화면 및 게임 로직 총괄 클래스
- **라인 수**: 3,811줄 (대규모 클래스)
- **주요 기능**: 렌더링, 입력 처리, 네트워크 통신, 게임 상태 관리, 맵 시스템, 스킬 이펙트

---

## 🎯 주요 기능

### 1. 게임 렌더링 시스템
```java
class GameCanvas extends JPanel {
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                             RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 1. 맵 배경
        if (mapImage != null) {
            g2d.drawImage(mapImage, -cameraX, -cameraY, mapWidth, mapHeight, null);
        }
        
        // 2. 장애물, 에어스트라이크 마커
        drawObstacles(g2d);
        drawStrikeMarkersMain(g2d);
        
        // 3. 플레이어들, 미사일, 스킬 이펙트
        // 4. UI (HP바, 미니맵, 스킬 쿨다운)
    }
}
```
- **레이어 렌더링**: 배경 → 오브젝트 → UI 순서
- **안티앨리어싱**: 부드러운 그래픽

### 2. 카메라 시스템
```java
private int cameraX = 0; // 카메라 위치 (플레이어 중심)
private int cameraY = 0;
private int mapWidth = 3200; // 맵 전체 크기 (화면의 4배)
private int mapHeight = 2400;

// 카메라를 플레이어 중심으로 이동
cameraX = playerX - GameConstants.GAME_WIDTH / 2;
cameraY = playerY - GameConstants.GAME_HEIGHT / 2;

// 맵 경계 제한
cameraX = Math.max(0, Math.min(cameraX, mapWidth - GameConstants.GAME_WIDTH));
cameraY = Math.max(0, Math.min(cameraY, mapHeight - GameConstants.GAME_HEIGHT));
```
- **부드러운 추적**: 플레이어를 화면 중앙에 유지
- **경계 처리**: 맵 끝에서 카메라 멈춤

### 3. 타일 기반 맵 시스템
```java
private static final int TILE_SIZE = 32;
private boolean[][] walkableGrid; // true = 이동 가능
private int gridCols, gridRows;

// 맵 편집 모드 (F4)
private boolean editMode = false;
private int editPaintMode = 0; // 0=walkable, 1=unwalkable, 2=RED 스폰, 3=BLUE 스폰
```
- **타일 기반 충돌**: 32x32 픽셀 단위
- **실시간 편집**: F4로 맵 편집 모드 진입
- **스폰 존 설정**: 팀별 리스폰 영역

### 4. 네트워크 동기화
```java
private final Socket socket;
private final DataOutputStream out;
private final DataInputStream in;

// 플레이어 위치 전송
private void sendPosition() {
    out.writeByte(Protocol.PLAYER_MOVE);
    out.writeInt(playerX);
    out.writeInt(playerY);
    out.writeInt(myDirection);
    out.flush();
}

// 스킬 사용 전송
private void sendSkillUse(int slotIndex, String type, int targetX, int targetY) {
    out.writeByte(Protocol.SKILL_USE);
    out.writeUTF(selectedCharacter);
    out.writeInt(slotIndex);
    out.writeUTF(type);
    out.writeInt(targetX);
    out.writeInt(targetY);
    out.flush();
}
```
- **TCP 소켓**: 신뢰성 있는 통신
- **프로토콜 기반**: `Protocol` 클래스의 상수 사용
- **플러시 패턴**: 즉시 전송 보장

### 5. 스킬 시스템 통합
```java
private Ability[] abilities; // [기본공격, 전술스킬, 궁극기]
private final SkillEffectManager skillEffects = new SkillEffectManager();

// 캐릭터별 런타임 상태
private float ravenDashRemaining = 0f;
private float ravenOverchargeRemaining = 0f;
private float piperMarkRemaining = 0f;
private float piperThermalRemaining = 0f;
private float teamMarkRemaining = 0f; // 팀 공유 버프
private float teamThermalRemaining = 0f;
```
- **10개 캐릭터**: 각각 고유 스킬 3개 (기본/전술/궁극기)
- **이펙트 매니저**: 시각적 피드백 관리
- **팀 버프**: Piper의 스킬은 팀원에게도 적용

### 6. 라운드 시스템
```java
private enum RoundState { WAITING, PLAYING, ENDED }
private RoundState roundState = RoundState.WAITING;
private int roundCount = 1;
private int redWins = 0, blueWins = 0;
private static final int MAX_ROUNDS = 3; // 3판 2선승
private static final int ROUND_READY_TIME = 10000; // 10초 대기
```
- **3판 2선승**: 경쟁 게임 모드
- **준비 시간**: 라운드 시작 전 10초
- **캐릭터 변경 제한**: 라운드 시작 후 10초 이내 1회만 가능

---

## ✅ 강점 (Strengths)

### 1. **포괄적인 게임 기능** ⭐⭐⭐⭐⭐
```java
// 한 클래스에서 게임의 모든 핵심 기능 제공
- 렌더링 (paintComponent)
- 입력 처리 (KeyListener, MouseListener)
- 네트워크 (Socket 통신)
- 게임 로직 (충돌, 스킬, HP)
- UI (채팅, 미니맵, HUD)
- 맵 편집 (F4 에디터 모드)
```
- **장점**: 프로토타입 빠른 개발, 통합 테스트 용이
- **사용 사례**: 교육용, 게임잼, 초기 프로토타입

### 2. **실시간 맵 편집 기능** ⭐⭐⭐⭐⭐
```java
// F4: 편집 모드 토글
// F5: 현재 맵 저장 (map_edited.json)
// F6: 다음 맵으로 전환
// 1키: walkable 페인트
// 2키: unwalkable (장애물) 페인트
// 3키: RED 스폰 존 페인트
// 4키: BLUE 스폰 존 페인트

private void drawEditorOverlay(Graphics2D g2d) {
    // 타일 그리드 표시
    // 마우스 오버 타일 하이라이트
    // 스폰 존 색상 표시 (빨강/파랑)
}
```
- **생산성**: 게임 실행 중 맵 수정 가능
- **직관성**: 마우스 드래그로 타일 페인팅
- **즉시 피드백**: 변경 사항 실시간 반영

### 3. **플레이어 보간 (Smooth Movement)** ⭐⭐⭐⭐
```java
class PlayerData {
    int x, y;
    int targetX, targetY; // 보간을 위한 목표 위치
    
    void smoothUpdate() {
        float interpolation = 0.5f; // 50% 이동
        x += (int) ((targetX - x) * interpolation);
        y += (int) ((targetY - y) * interpolation);
    }
}

// 네트워크 수신 시
PlayerData p = players.get(playerName);
p.targetX = x; // 목표 위치만 설정
p.targetY = y;
// smoothUpdate()가 부드럽게 이동 처리
```
- **효과**: 네트워크 지연 시에도 부드러운 움직임
- **성능**: CPU 부담 최소화 (단순 선형 보간)

### 4. **시야 시스템 (Fog of War)** ⭐⭐⭐⭐
```java
private static final int VISION_RANGE = (int) (Math.sqrt(
    GameConstants.GAME_WIDTH * GameConstants.GAME_WIDTH +
    GameConstants.GAME_HEIGHT * GameConstants.GAME_HEIGHT) / 2
);

// Piper 스킬: 시야 확장
private static final float PIPER_MARK_RANGE_FACTOR = 1.7f;

// 적 플레이어 그리기 전 거리 체크
int distance = (int) Math.sqrt((screenX - myScreenX) * (screenX - myScreenX) + 
                                (screenY - myScreenY) * (screenY - myScreenY));
if (distance <= currentVisionRange) {
    // 시야 내: 실제 모델 그리기
} else {
    // 시야 밖: 그리지 않음 (전략적 요소)
}
```
- **전략성**: 적 위치 파악의 중요성
- **캐릭터 차별화**: Piper의 정찰 역할

### 5. **캐릭터 다양성 (10개 캐릭터)** ⭐⭐⭐⭐⭐
```java
// 각 캐릭터별 고유 런타임 상태 관리
private float ravenDashRemaining = 0f;       // Raven: 돌진
private float ravenOverchargeRemaining = 0f; // Raven: 과충전
private float piperMarkRemaining = 0f;       // Piper: 표적 지정
private float piperThermalRemaining = 0f;    // Piper: 열감지
// ... General, Ghost, Bulldog, Sage, Skull, Steam, Tech, Wildcat
```
- **개성**: 각 캐릭터가 독특한 플레이 스타일
- **밸런스**: 역할 분담 (탱커, 딜러, 서포터, 정찰)

---

## ⚠️ 개선 영역 (Areas for Improvement)

### 1. **God Object 안티패턴** 🔴 HIGH
**현재 코드:**
```java
public class GamePanel extends JFrame implements KeyListener {
    // 3,811줄의 단일 클래스
    // 렌더링, 네트워크, 게임 로직, UI, 입력 처리 모두 포함
}
```

**문제점:**
- **단일 책임 원칙 위반**: 한 클래스가 너무 많은 역할
- **유지보수 어려움**: 버그 수정 시 영향 범위 파악 힘듦
- **테스트 불가능**: 단위 테스트 작성 불가능
- **재사용 불가능**: 다른 프로젝트에서 일부만 사용 불가

**개선안 - MVC 패턴 적용:**
```java
// Model - 게임 상태
public class GameState {
    private Map<String, Player> players;
    private List<Missile> missiles;
    private List<PlacedObject> placedObjects;
    private RoundManager roundManager;
    
    public void update(float deltaTime) {
        // 게임 로직만 처리
    }
}

// View - 렌더링
public class GameRenderer {
    public void render(Graphics2D g, GameState state, Camera camera) {
        renderMap(g, camera);
        renderPlayers(g, state.getPlayers(), camera);
        renderMissiles(g, state.getMissiles(), camera);
        renderUI(g, state);
    }
}

// Controller - 입력 처리
public class InputController {
    private KeyBindingConfig keyConfig;
    
    public void handleInput(GameState state, NetworkClient network) {
        if (keyConfig.isKeyPressed("MOVE_UP")) {
            state.movePlayer(0, -1);
            network.sendPosition(state.getMyPlayer());
        }
    }
}

// Network - 통신
public class NetworkClient {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    
    public void sendPosition(Player player) { /* ... */ }
    public void sendSkillUse(Skill skill) { /* ... */ }
}

// Main Panel - 조합
public class GamePanel extends JPanel {
    private GameState state = new GameState();
    private GameRenderer renderer = new GameRenderer();
    private InputController input = new InputController();
    private NetworkClient network = new NetworkClient();
    
    @Override
    protected void paintComponent(Graphics g) {
        renderer.render((Graphics2D) g, state, camera);
    }
    
    private void gameLoop() {
        input.handleInput(state, network);
        state.update(deltaTime);
        repaint();
    }
}
```

**장점:**
- **명확한 책임**: 각 클래스가 하나의 역할만 수행
- **테스트 가능**: GameState 단독으로 단위 테스트 가능
- **재사용성**: GameRenderer를 리플레이 시스템에서 재사용 가능
- **병렬 개발**: 팀원들이 다른 컴포넌트 동시 작업 가능

---

### 2. **캐릭터별 하드코딩된 상태** 🔴 HIGH
**현재 코드:**
```java
// 10개 캐릭터 × 3개 스킬 = 30개 상태 변수
private float ravenDashRemaining = 0f;
private float ravenOverchargeRemaining = 0f;
private float piperMarkRemaining = 0f;
private float piperThermalRemaining = 0f;
private float generalAuraRemaining = 0f;
private float generalStrikeRemaining = 0f;
// ... 24개 더
```

**문제점:**
- **확장 불가능**: 새 캐릭터 추가 시 클래스 수정 필요
- **코드 중복**: 비슷한 로직이 30개 변수마다 반복
- **버그 위험**: 한 캐릭터 수정 시 다른 캐릭터에 영향

**개선안 - 다형성 활용:**
```java
// 캐릭터 인터페이스
public interface Character {
    void updateSkills(float deltaTime);
    void useBasicAttack(int targetX, int targetY);
    void useTacticalSkill(int targetX, int targetY);
    void useUltimate(int targetX, int targetY);
    void renderEffects(Graphics2D g, int x, int y);
}

// 구체적 구현
public class RavenCharacter implements Character {
    private float dashRemaining = 0f;
    private float overchargeRemaining = 0f;
    
    @Override
    public void updateSkills(float deltaTime) {
        if (dashRemaining > 0) {
            dashRemaining -= deltaTime;
            // 대시 효과 적용
        }
        if (overchargeRemaining > 0) {
            overchargeRemaining -= deltaTime;
            // 과충전 효과 적용
        }
    }
    
    @Override
    public void useTacticalSkill(int targetX, int targetY) {
        dashRemaining = 0.5f; // 0.5초 대시
        // 대시 로직
    }
    
    @Override
    public void useUltimate(int targetX, int targetY) {
        overchargeRemaining = 8f; // 8초 과충전
        // 과충전 로직
    }
}

public class PiperCharacter implements Character {
    private float markRemaining = 0f;
    private float thermalRemaining = 0f;
    // Piper만의 로직
}

// GamePanel에서 사용
public class GamePanel {
    private Character myCharacter;
    private Map<String, Character> characterInstances = new HashMap<>();
    
    private void selectCharacter(String characterId) {
        switch (characterId) {
            case "raven": myCharacter = new RavenCharacter(); break;
            case "piper": myCharacter = new PiperCharacter(); break;
            // ...
        }
    }
    
    private void gameLoop() {
        myCharacter.updateSkills(deltaTime);
    }
}
```

**장점:**
- **확장성**: 새 캐릭터는 새 클래스만 추가
- **캡슐화**: 캐릭터 로직이 자신의 클래스 내부에만 존재
- **타입 안전**: 컴파일 타임에 오류 감지

---

### 3. **과도한 float 타이머 변수** 🟡 MEDIUM
**현재 코드:**
```java
private float ravenDashRemaining = 0f;
private float ravenOverchargeRemaining = 0f;
private float piperMarkRemaining = 0f;
// ... 수십 개

// 게임 루프에서
if (ravenDashRemaining > 0) {
    ravenDashRemaining -= deltaTime;
    if (ravenDashRemaining <= 0) {
        // 효과 종료
    }
}
// 모든 변수마다 동일 패턴 반복
```

**문제점:**
- **보일러플레이트**: 같은 로직 반복
- **실수 가능성**: `<=` vs `<` 조건 실수
- **일관성 부족**: 각 타이머 업데이트 방식 다를 수 있음

**개선안 - Timer 유틸리티 클래스:**
```java
public class Timer {
    private float remaining;
    private float duration;
    private boolean active;
    
    public Timer(float duration) {
        this.duration = duration;
        this.remaining = 0f;
        this.active = false;
    }
    
    public void start() {
        remaining = duration;
        active = true;
    }
    
    public void update(float deltaTime) {
        if (active) {
            remaining -= deltaTime;
            if (remaining <= 0) {
                remaining = 0;
                active = false;
            }
        }
    }
    
    public boolean isActive() { return active; }
    public float getProgress() { return 1.0f - (remaining / duration); }
    public float getRemaining() { return remaining; }
}

// 사용
public class RavenCharacter {
    private Timer dashTimer = new Timer(0.5f);
    private Timer overchargeTimer = new Timer(8.0f);
    
    public void updateSkills(float deltaTime) {
        dashTimer.update(deltaTime);
        overchargeTimer.update(deltaTime);
    }
    
    public void useTacticalSkill() {
        if (canUseDash()) {
            dashTimer.start();
        }
    }
    
    public boolean isDashing() { return dashTimer.isActive(); }
}
```

**장점:**
- **재사용성**: 모든 타이머에 일관된 로직
- **기능 추가 용이**: pause(), reset() 등 쉽게 추가
- **버그 감소**: 한 곳에서만 로직 관리

---

### 4. **동기화되지 않은 네트워크 쓰레드** 🔴 HIGH
**현재 코드:**
```java
private void receiveMessages() {
    new Thread(() -> {
        while (true) {
            byte msg = in.readByte();
            switch (msg) {
                case Protocol.PLAYER_JOIN:
                    String name = in.readUTF();
                    players.put(name, new PlayerData(...)); // ⚠️ 스레드 안전하지 않음
                    break;
            }
        }
    }).start();
}

// 메인 게임 루프 (EDT)
private void gameLoop() {
    for (PlayerData p : players.values()) { // ⚠️ ConcurrentModificationException 가능
        p.smoothUpdate();
    }
}
```

**문제점:**
- **Race Condition**: 네트워크 스레드와 EDT가 동시에 `players` 맵 접근
- **ConcurrentModificationException**: 이터레이션 중 맵 수정
- **데이터 불일치**: 동기화 없이 읽기/쓰기

**개선안 - 동기화 및 ConcurrentHashMap:**
```java
// 스레드 안전한 컬렉션 사용
private final Map<String, PlayerData> players = new ConcurrentHashMap<>();

// 또는 EDT로 메시지 디스패치
private final Queue<NetworkMessage> messageQueue = new ConcurrentLinkedQueue<>();

private void receiveMessages() {
    new Thread(() -> {
        while (true) {
            byte msg = in.readByte();
            NetworkMessage netMsg = parseMessage(msg, in);
            messageQueue.offer(netMsg); // 큐에 추가만
        }
    }).start();
}

// EDT에서 처리
private void gameLoop() {
    // 1. 네트워크 메시지 처리
    NetworkMessage msg;
    while ((msg = messageQueue.poll()) != null) {
        processMessage(msg); // EDT에서 안전하게 처리
    }
    
    // 2. 게임 업데이트
    for (PlayerData p : players.values()) {
        p.smoothUpdate();
    }
    
    // 3. 렌더링
    repaint();
}
```

**장점:**
- **스레드 안전**: EDT에서만 게임 상태 수정
- **예측 가능**: Race condition 제거
- **디버깅 용이**: 순차적 실행

---

### 5. **매직 넘버 남용** 🟡 MEDIUM
**현재 코드:**
```java
// 의미 불명확한 숫자들
private int playerX = 400;
private int playerY = 300;
private static final int TURRET_RANGE = 180;
private static final float PIPER_MARK_RANGE_FACTOR = 1.7f;
private static final int PIPER_THERMAL_DOT_SIZE = 10;
```

**개선안:**
```java
// 의미 있는 상수로 교체
public class GameConstants {
    // 초기 스폰 위치 (화면 중앙)
    public static final int INITIAL_PLAYER_X = GAME_WIDTH / 2;
    public static final int INITIAL_PLAYER_Y = GAME_HEIGHT / 2;
    
    // 터렛 공격 범위 (타일 단위로 계산)
    public static final int TURRET_ATTACK_TILES = 5;
    public static final int TURRET_RANGE = TURRET_ATTACK_TILES * TILE_SIZE + TILE_SIZE / 2;
    // = 5 * 32 + 16 = 176 (기존 180과 유사하지만 논리적)
    
    // Piper 스킬 배율 (70% 시야 확장)
    public static final float PIPER_MARK_VISION_BOOST = 1.7f;
    public static final int PIPER_THERMAL_INDICATOR_SIZE = 10;
}
```

---

### 6. **예외 처리 부재** 🔴 HIGH
**현재 코드:**
```java
private void loadMap(String mapName) throws IOException {
    mapImage = ImageIO.read(new File("assets/maps/" + mapName + ".png"));
    // ⚠️ 파일 없으면 크래시
}

private void receiveMessages() {
    while (true) {
        byte msg = in.readByte(); // ⚠️ IOException 미처리
        // ...
    }
}
```

**개선안:**
```java
private void loadMap(String mapName) {
    try {
        File mapFile = new File("assets/maps/" + mapName + ".png");
        if (!mapFile.exists()) {
            System.err.println("맵 파일 없음: " + mapFile.getAbsolutePath());
            // 폴백: 기본 맵 사용
            mapImage = createDefaultMap();
            return;
        }
        mapImage = ImageIO.read(mapFile);
    } catch (IOException e) {
        System.err.println("맵 로드 실패: " + e.getMessage());
        e.printStackTrace();
        mapImage = createDefaultMap();
    }
}

private void receiveMessages() {
    try {
        while (true) {
            byte msg = in.readByte();
            processMessage(msg);
        }
    } catch (EOFException e) {
        System.out.println("서버 연결 종료");
        handleDisconnect();
    } catch (IOException e) {
        System.err.println("네트워크 오류: " + e.getMessage());
        handleNetworkError(e);
    }
}
```

---

## 🏗️ 아키텍처 분석

### 현재 구조 (God Object)
```
GamePanel
├── Rendering (paintComponent + 20+ draw methods)
├── Input Handling (KeyListener, MouseListener)
├── Network (Socket, Protocol parsing)
├── Game Logic (collision, skills, HP, rounds)
├── UI (chat, minimap, HUD, editor)
├── Map System (tiles, obstacles, spawn zones)
└── Inner Classes (PlayerData, Missile, GameCanvas, PlacedObject, StrikeMarker)
```
- **문제**: 모든 기능이 한 클래스에 집중
- **결과**: 3,811줄의 거대 클래스

### 제안 구조 (Component 기반)
```
GamePanel (Main Controller)
├── GameState (Model)
│   ├── PlayerManager
│   ├── ProjectileManager
│   ├── RoundManager
│   └── ObjectManager
├── GameRenderer (View)
│   ├── MapRenderer
│   ├── EntityRenderer
│   ├── UIRenderer
│   └── EffectRenderer
├── InputController
│   ├── KeyboardHandler
│   └── MouseHandler
├── NetworkClient
│   ├── MessageSender
│   └── MessageReceiver
├── CharacterSystem
│   ├── RavenCharacter
│   ├── PiperCharacter
│   └── ... (8 more)
└── MapSystem
    ├── TileGrid
    ├── CollisionDetector
    └── MapEditor
```
- **장점**: 명확한 책임 분리, 테스트 가능, 재사용 가능

---

## ⚡ 성능 고려사항

### 1. **과도한 렌더링**
```java
// 현재: 모든 프레임에 모든 플레이어 그리기
for (PlayerData p : players.values()) {
    int screenX = p.x - cameraX;
    int screenY = p.y - cameraY;
    drawPlayer(g2d, screenX, screenY, p);
}
```

**개선 - 뷰포트 컬링:**
```java
for (PlayerData p : players.values()) {
    int screenX = p.x - cameraX;
    int screenY = p.y - cameraY;
    
    // 화면 밖이면 스킵
    if (screenX < -50 || screenX > GAME_WIDTH + 50 ||
        screenY < -50 || screenY > GAME_HEIGHT + 50) {
        continue;
    }
    
    drawPlayer(g2d, screenX, screenY, p);
}
```
- **성능 향상**: 30-50% CPU 절감 (많은 플레이어 시)

### 2. **문자열 연결 최적화**
```java
// 현재
String status = "HP: " + myHP + "/" + myMaxHP + " | Kills: " + kills;

// 개선
StringBuilder sb = new StringBuilder(50);
sb.append("HP: ").append(myHP).append("/").append(myMaxHP)
  .append(" | Kills: ").append(kills);
String status = sb.toString();
```

---

## 🧪 테스트 시나리오

### 1. 충돌 감지
```java
@Test
public void testWalkableGridCollision() {
    GamePanel panel = new GamePanel(...);
    panel.loadMap("test_map");
    
    // 이동 가능한 타일로 이동
    boolean canMove = panel.isWalkable(100, 100);
    assertTrue(canMove);
    
    // 장애물 타일로 이동 시도
    canMove = panel.isWalkable(32, 32); // (1, 1) 타일이 unwalkable
    assertFalse(canMove);
}
```

### 2. 카메라 경계
```java
@Test
public void testCameraBounds() {
    GamePanel panel = new GamePanel(...);
    panel.setPlayerPosition(0, 0); // 맵 좌상단
    panel.updateCamera();
    
    assertEquals(0, panel.getCameraX());
    assertEquals(0, panel.getCameraY());
    
    panel.setPlayerPosition(3200, 2400); // 맵 우하단
    panel.updateCamera();
    
    assertEquals(3200 - GAME_WIDTH, panel.getCameraX());
    assertEquals(2400 - GAME_HEIGHT, panel.getCameraY());
}
```

### 3. 스킬 쿨다운
```java
@Test
public void testSkillCooldown() {
    GamePanel panel = new GamePanel(...);
    panel.selectCharacter("raven");
    
    Ability dash = panel.getAbilities()[1]; // 전술 스킬
    assertTrue(dash.canUse());
    
    panel.useTacticalSkill(500, 500);
    assertFalse(dash.canUse());
    
    panel.update(8.0f); // 8초 경과
    assertTrue(dash.canUse());
}
```

---

## 💡 사용 예시

### 게임 시작
```java
// 서버 연결
Socket socket = new Socket("localhost", 8888);

// GamePanel 생성
GamePanel gamePanel = new GamePanel("Player1", GameConstants.TEAM_RED, socket);

// 캐릭터 선택
gamePanel.selectCharacter("raven");

// 게임 시작
gamePanel.setVisible(true);
```

### 맵 편집 모드
```java
// 1. F4로 편집 모드 진입
// 2. 1키: walkable 페인트 모드
// 3. 마우스 드래그로 타일 칠하기
// 4. F5로 저장 → assets/maps/map_edited.json
// 5. F4로 편집 모드 종료
```

---

## 📚 학습 포인트

### 초급
1. **JFrame과 JPanel**: Swing GUI 기본
2. **KeyListener**: 키보드 입력 처리
3. **Graphics2D**: 2D 그래픽 렌더링

### 중급
1. **게임 루프**: `Timer`로 60 FPS 유지
2. **카메라 시스템**: 스크롤링 맵 구현
3. **네트워크**: Socket TCP 통신

### 고급
1. **God Object 리팩토링**: MVC 패턴 적용
2. **스레드 안전**: EDT와 네트워크 스레드 동기화
3. **성능 최적화**: 뷰포트 컬링, 객체 풀링

---

## 🎓 종합 평가

| 평가 항목 | 점수 | 설명 |
|---------|------|------|
| **기능 완성도** | ⭐⭐⭐⭐⭐ | 모든 게임 기능 작동 |
| **코드 구조** | ⭐⭐ | God Object, 리팩토링 필요 |
| **유지보수성** | ⭐⭐ | 3,811줄, 수정 어려움 |
| **성능** | ⭐⭐⭐⭐ | 대체로 양호, 일부 최적화 가능 |
| **확장성** | ⭐⭐ | 새 기능 추가 시 클래스 비대화 |
| **테스트 가능성** | ⭐ | 단위 테스트 거의 불가능 |

**평균 점수: 2.83 / 5.0**

---

## 🚀 우선순위 개선 사항

### 🔴 HIGH Priority
1. **God Object 분리**
   - GamePanel → GameState + GameRenderer + InputController + NetworkClient
   - 예상 작업: 2-3주 (대규모 리팩토링)

2. **캐릭터 시스템 다형성**
   - 하드코딩된 30개 상태 변수 → Character 인터페이스
   - 예상 작업: 1-2주

3. **네트워크 스레드 동기화**
   - ConcurrentHashMap + 메시지 큐 패턴
   - 예상 작업: 3-4일

### 🟡 MEDIUM Priority
4. **Timer 유틸리티 클래스**
   - float 변수 → Timer 객체
   - 예상 작업: 2-3일

5. **예외 처리 추가**
   - try-catch로 안정성 확보
   - 예상 작업: 1-2일

### 🟢 LOW Priority
6. **매직 넘버 제거**
   - 상수로 교체
   - 예상 작업: 1일

---

## 📖 참고 자료

### 디자인 패턴
- **MVC Pattern**: [Wikipedia](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller)
- **Component Pattern**: [Game Programming Patterns](https://gameprogrammingpatterns.com/component.html)

### 게임 개발
- **Entity Component System**: [GDC Talk](https://www.youtube.com/watch?v=W3aieHjyNvw)
- **Game Loop**: [Fix Your Timestep](https://gafferongames.com/post/fix_your_timestep/)

### Java/Swing
- **EDT Best Practices**: [Oracle Docs](https://docs.oracle.com/javase/tutorial/uiswing/concurrency/)
- **Effective Java**: Item 80 (Thread Safety)

---

## 🎯 결론

`GamePanel.java`는 **기능적으로 완성도 높은 게임**을 제공하지만, **소프트웨어 공학 관점에서는 많은 개선이 필요**합니다.

**주요 성과:**
- ✅ 10개 캐릭터, 30개 스킬 완벽 구현
- ✅ 실시간 맵 편집 기능
- ✅ 부드러운 네트워크 동기화
- ✅ 라운드 시스템, 시야 시스템

**핵심 문제:**
- ❌ 3,811줄의 God Object
- ❌ 테스트 불가능한 구조
- ❌ 스레드 안전성 문제
- ❌ 확장 어려움

**추천 방향:**
이 코드는 **프로토타입이나 학습용으로는 우수**하지만, **프로덕션 레벨**로 발전시키려면 **대규모 리팩토링 (MVC 패턴 적용)**이 필수입니다. 

리팩토링 시 기존 기능을 유지하면서 점진적으로 분리하는 **Strangler Fig 패턴**을 추천합니다. (한 번에 전체를 바꾸지 않고, 일부씩 새 구조로 이전)
