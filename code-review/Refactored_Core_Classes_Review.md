# 리팩토링 핵심 클래스 코드 리뷰
## GameState, NetworkClient, GameRenderer

## 파일 정보
- **GameState.java**: 437줄 - 게임 상태 관리
- **NetworkClient.java**: 150줄 - 네트워크 통신 전담
- **GameRenderer.java**: 785줄 - 렌더링 전담
- **총 라인 수**: 1,372줄
- **역할**: GamePanel의 MVC 리팩토링 핵심 컴포넌트

---

## 개요

이 3개 클래스는 **GamePanel의 Phase 1 리팩토링**의 핵심 결과물로, **SOLID 원칙 중 단일 책임 원칙(SRP)**을 적용하여 2,539줄의 GamePanel을 역할별로 분리한 것입니다.

### 리팩토링 전 (GamePanel 통합 구조)
```
GamePanel (2,539줄)
├── 게임 상태 관리 (플레이어, 미사일, 맵, 라운드 등)
├── 네트워크 통신 (소켓, 메시지 송수신)
├── 렌더링 (그래픽 그리기)
├── 입력 처리 (키보드, 마우스)
└── 게임 루프 (업데이트, 렌더링 호출)
```

### 리팩토링 후 (역할 분리 구조)
```
GamePanel (2,539줄)
├── GameState (437줄) - 게임 상태 관리
├── NetworkClient (150줄) - 네트워크 통신
├── GameRenderer (785줄) - 렌더링
├── InputController - 입력 처리
└── 8개 Manager 클래스 - 세부 로직
```

---

## 1. GameState (437줄) - 게임 상태 관리

### 역할
**게임의 모든 상태 정보를 중앙에서 관리**하는 클래스입니다. 플레이어, 미사일, 맵, 라운드, 스킬 이펙트 등 게임 진행에 필요한 모든 데이터를 보유합니다.

### 주요 컴포넌트

#### 1.1 플레이어 상태
```java
// 플레이어 기본 정보
private final String playerName;
private final int team;
private int playerX = 400;
private int playerY = 300;
private int myHP = GameConstants.MAX_HP;
private int myMaxHP = GameConstants.MAX_HP;
private int myDirection = 0; // 0:Down, 1:Up, 2:Left, 3:Right
private int kills = 0;
private int deaths = 0;

// 캐릭터 시스템
private String selectedCharacter = "raven";
private CharacterData currentCharacterData;
private Ability[] abilities; // [기본공격, 전술스킬, 궁극기]
private SpriteAnimation[] myAnimations;
```

**특징**:
- **final 필드**: `playerName`, `team`은 변경 불가
- **기본값 초기화**: 위치(400,300), 체력(100), 방향(Down)
- **캐릭터 연동**: `CharacterData`, `Ability[]`, `SpriteAnimation[]`

#### 1.2 캐릭터별 런타임 상태
```java
// Raven 상태
private float ravenDashRemaining = 0f;
private float ravenOverchargeRemaining = 0f;
private float missileSpeedMultiplier = 1f;

// Piper 상태
private float piperMarkRemaining = 0f;
private float piperThermalRemaining = 0f;
private float teamMarkRemaining = 0f;
private float teamThermalRemaining = 0f;

// 버프 상태
private float moveSpeedMultiplier = 1.0f;
private float attackSpeedMultiplier = 1.0f;
```

**특징**:
- **캐릭터 특화 상태**: Raven(대시, 과충전), Piper(마크, 열상)
- **버프 시스템**: 이동속도, 공격속도 배수
- **float 타입**: 시간 기반 관리 (deltaTime 계산)

#### 1.3 스킬 이펙트 시스템
```java
public static class ActiveEffect {
    public String abilityId;
    public String type; // BASIC, TACTICAL, ULTIMATE
    public float duration;
    public float remaining;
    public Color color;

    public ActiveEffect(String abilityId, String type, float duration) {
        this.abilityId = abilityId;
        this.type = type;
        this.duration = Math.max(0.1f, duration);
        this.remaining = this.duration;
        this.color = colorForType(type);
    }

    static Color colorForType(String type) {
        if ("BASIC".equalsIgnoreCase(type))
            return new Color(100, 200, 100);
        if ("ULTIMATE".equalsIgnoreCase(type))
            return new Color(255, 100, 100);
        return new Color(100, 150, 255);
    }
}

private final Map<String, List<ActiveEffect>> effectsByPlayer = new HashMap<>();
private final List<ActiveEffect> myEffects = new ArrayList<>();
```

**특징**:
- **타입별 색상**: BASIC(초록), TACTICAL(파랑), ULTIMATE(빨강)
- **플레이어별 관리**: `Map<String, List<ActiveEffect>>`
- **자체 이펙트**: `myEffects` 리스트

#### 1.4 게임 객체 관리
```java
// 다른 플레이어들
private Map<String, PlayerData> players = new HashMap<>();

// 미사일 리스트
private final List<Missile> missiles = new ArrayList<>();

// 설치된 오브젝트 (지뢰, 터렛)
private final Map<Integer, PlacedObjectClient> placedObjects = new HashMap<>();

// 에어스트라이크 마커
private final Map<Integer, StrikeMarker> strikeMarkers = new HashMap<>();
```

**특징**:
- **PlayerData**: 위치, 팀, 체력, 캐릭터, 킬/데스
- **Missile**: 위치, 방향, 팀, 발사자
- **PlacedObjectClient**: ID, 타입(지뢰/터렛), 위치, 체력, 소유자
- **StrikeMarker**: ID, 위치, 생성 시각

#### 1.5 라운드 시스템
```java
public enum RoundState {
    WAITING, PLAYING, ENDED
}

private RoundState roundState = RoundState.WAITING;
private int roundCount = 0;
private int redWins = 0;
private int blueWins = 0;
private long roundStartTime = 0;
private String centerMessage = "";
private long centerMessageEndTime = 0;
```

**특징**:
- **3가지 상태**: WAITING(대기), PLAYING(진행 중), ENDED(종료)
- **3판 2선승**: `redWins`, `blueWins` 카운터
- **중앙 메시지**: "RED TEAM WINS!" 등

#### 1.6 게임 로직 메서드
```java
/**
 * 미사일 업데이트 - 이동 및 충돌 체크
 */
public void updateMissiles() {
    missiles.removeIf(missile -> {
        missile.x += missile.dx;
        missile.y += missile.dy;
        // 맵 밖으로 나가면 제거
        return missile.x < 0 || missile.x > mapWidth || 
               missile.y < 0 || missile.y > mapHeight;
    });
}

/**
 * 스킬 이펙트 시간 업데이트
 */
public void updateEffects(float deltaTime) {
    myEffects.removeIf(effect -> {
        effect.remaining -= deltaTime;
        return effect.remaining <= 0;
    });
    
    // Raven 상태 감소
    if (ravenDashRemaining > 0) {
        ravenDashRemaining -= deltaTime;
        if (ravenDashRemaining <= 0) ravenDashRemaining = 0;
    }
    // ... 나머지 캐릭터 상태 감소
}

/**
 * 카메라를 플레이어 중심으로 업데이트
 */
public void updateCamera(int screenWidth, int screenHeight) {
    cameraX = playerX - screenWidth / 2;
    cameraY = playerY - screenHeight / 2;
    
    // 카메라가 맵 밖으로 나가지 않도록 제한
    cameraX = Math.max(0, Math.min(cameraX, mapWidth - screenWidth));
    cameraY = Math.max(0, Math.min(cameraY, mapHeight - screenHeight));
}
```

### GameState 강점
1. **중앙 집중식 상태 관리**: 모든 게임 상태를 한 곳에서 관리
2. **명확한 Getter/Setter**: 437줄 중 절반은 접근자 메서드 (캡슐화)
3. **내부 클래스 활용**: `PlayerData`, `Missile`, `PlacedObjectClient` 등
4. **시간 기반 업데이트**: `deltaTime` 기반으로 이펙트, 버프 관리
5. **enum 사용**: `RoundState` enum으로 타입 안전성 확보

### GameState 개선 제안
1. **불변성 강화**: `final` 필드 더 늘리기 (예: `abilities`, `myAnimations`)
2. **빌더 패턴**: 생성자 매개변수가 많아지면 빌더 패턴 고려
3. **상태 검증**: setter에서 유효성 검증 추가 (예: HP가 음수가 되지 않도록)

---

## 2. NetworkClient (150줄) - 네트워크 통신

### 역할
**서버와의 소켓 통신을 전담**하는 클래스입니다. 메시지 송수신을 담당하며, **프로토콜 파싱은 GamePanel에 위임**합니다.

### 주요 컴포넌트

#### 2.1 소켓 관리
```java
private final Socket socket;
private final DataOutputStream out;
private final DataInputStream in;
private Thread receiveThread;
private volatile boolean running = false;

// 메시지 처리 콜백
private Consumer<String> onMessageReceived;

public NetworkClient(Socket socket, DataOutputStream out, DataInputStream in) {
    this.socket = socket;
    this.out = out;
    this.in = in;
}
```

**특징**:
- **final 소켓**: 생성 후 변경 불가
- **콜백 패턴**: `Consumer<String>` 콜백으로 메시지 전달
- **스레드 안전**: `volatile boolean running`

#### 2.2 메시지 수신
```java
/**
 * 메시지 수신 스레드 시작
 */
public void startReceiving() {
    if (running) return;
    
    running = true;
    receiveThread = new Thread(this::receiveGameUpdates, "NetworkReceiveThread");
    receiveThread.setDaemon(true);
    receiveThread.start();
}

private void receiveGameUpdates() {
    try {
        String message;
        while (running && (message = in.readUTF()) != null) {
            if (onMessageReceived != null) {
                onMessageReceived.accept(message);
            }
        }
    } catch (IOException ex) {
        if (running) {
            System.out.println("서버와의 연결이 끊어졌습니다.");
        }
    }
}
```

**특징**:
- **데몬 스레드**: JVM 종료 시 자동으로 종료
- **블로킹 방식**: `readUTF()` 블로킹 (LobbyFrame의 폴링 방식과 대조)
- **콜백 호출**: 메시지 수신 시 `onMessageReceived.accept(message)` 호출

#### 2.3 메시지 송신
```java
/**
 * 서버로 메시지 전송
 */
public synchronized void sendMessage(String message) {
    if (out == null) return;
    
    try {
        out.writeUTF(message);
        out.flush();
    } catch (IOException e) {
        System.err.println("메시지 전송 실패: " + e.getMessage());
    }
}

/**
 * 위치 업데이트 전송
 */
public void sendPosition(int x, int y, int direction) {
    sendMessage("POS:" + x + "," + y + "," + direction);
}

/**
 * 발사 이벤트 전송
 */
public void sendShoot(int x, int y, int dx, int dy) {
    sendMessage("SHOOT:" + x + "," + y + "," + dx + "," + dy);
}

/**
 * 스킬 사용 전송
 */
public void sendSkillUse(String skillData) {
    sendMessage("SKILL:" + skillData);
}

/**
 * 채팅 메시지 전송
 */
public void sendChat(String message) {
    sendMessage("CHAT:" + message);
}
```

**특징**:
- **synchronized**: 여러 스레드에서 동시 호출 방지
- **편의 메서드**: `sendPosition()`, `sendShoot()` 등
- **프로토콜 조립**: "POS:", "SHOOT:", "SKILL:" prefix 추가

#### 2.4 연결 관리
```java
/**
 * 네트워크 연결 종료
 */
public void stop() {
    running = false;
    if (receiveThread != null) {
        receiveThread.interrupt();
    }
    try {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    } catch (IOException e) {
        System.err.println("소켓 종료 중 오류: " + e.getMessage());
    }
}

/**
 * 연결 상태 확인
 */
public boolean isConnected() {
    return socket != null && !socket.isClosed() && running;
}
```

### NetworkClient 강점
1. **단일 책임 원칙**: 네트워크 통신만 담당
2. **콜백 패턴**: 느슨한 결합으로 GamePanel과 분리
3. **스레드 안전**: `synchronized`, `volatile` 사용
4. **편의 메서드**: 프로토콜 조립을 내부에서 처리
5. **명확한 생명주기**: `startReceiving()` → `stop()`

### NetworkClient 개선 제안
1. **재연결 로직**: 연결 끊김 시 자동 재연결 기능
2. **타임아웃 설정**: 소켓 타임아웃 설정 옵션
3. **로깅 강화**: SLF4J 등 로깅 프레임워크 사용
4. **메시지 큐**: 송신 메시지 큐잉 (버스트 방지)

---

## 3. GameRenderer (785줄) - 렌더링 전담

### 역할
**게임 화면 렌더링을 전담**하는 클래스입니다. `RenderContext`로 필요한 데이터를 받아 화면에 그립니다.

### 주요 컴포넌트

#### 3.1 메인 렌더링 진입점
```java
/**
 * 메인 렌더링 진입점
 */
public void render(Graphics g, RenderContext ctx) {
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
    // 고정 해상도 렌더링 + 스케일링
    double scaleX = (double) ctx.actualCanvasWidth / GameConstants.GAME_WIDTH;
    double scaleY = (double) ctx.actualCanvasHeight / GameConstants.GAME_HEIGHT;
    
    // 스케일 적용
    g2d.scale(scaleX, scaleY);
    
    // 이후 모든 렌더링은 1280x720 좌표계에서 진행
    
    // 1. 맵 배경
    drawMap(g2d, ctx);
    
    // 2. 장애물 디버그
    drawObstacles(g2d, ctx);
    
    // 3. 에어스트라이크 마커
    drawStrikeMarkers(g2d, ctx);
    
    // 4. 편집 모드 오버레이
    if (ctx.editMode) {
        drawEditorOverlay(g2d, ctx);
    }
    
    // 5. 다른 플레이어들
    drawOtherPlayers(g2d, ctx);
    
    // 6. 로컬 플레이어
    drawLocalPlayer(g2d, ctx);
    
    // 7. 조준선
    drawAimLine(g2d, ctx);
    
    // 8. 미사일
    drawMissiles(g2d, ctx);
    
    // 9. 설치된 오브젝트 (지뢰/터렛)
    drawPlacedObjects(g2d, ctx);
    
    // 10. UI 요소들 (항상 마지막)
    if (ctx.showMinimap) {
        drawMinimap(g2d, ctx);
    }
    drawHUD(g2d, ctx);
    drawRoundInfo(g2d, ctx);
}
```

**특징**:
- **Anti-aliasing**: 부드러운 렌더링
- **고정 해상도 + 스케일링**: 1280x720 고정, 실제 크기에 맞춰 스케일
- **명확한 렌더링 순서**: 맵 → 플레이어 → 미사일 → UI
- **조건부 렌더링**: 편집 모드, 미니맵 등

#### 3.2 플레이어 렌더링
```java
private void drawLocalPlayer(Graphics2D g2d, RenderContext ctx) {
    int myScreenX = ctx.playerX - ctx.cameraX;
    int myScreenY = ctx.playerY - ctx.cameraY;
    
    Color myColor = ctx.team == GameConstants.TEAM_RED ? 
        new Color(255, 100, 100) : new Color(100, 150, 255);
    
    // 스프라이트 애니메이션 또는 기본 원
    if (ctx.myAnimations != null) {
        int animIndex = ctx.myDirection;
        if (animIndex < ctx.myAnimations.length && ctx.myAnimations[animIndex] != null) {
            ctx.myAnimations[animIndex].draw(g2d, myScreenX - 20, myScreenY - 20, 40, 40);
        } else {
            g2d.setColor(myColor);
            g2d.fillOval(myScreenX - 20, myScreenY - 20, 40, 40);
        }
    } else {
        g2d.setColor(myColor);
        g2d.fillOval(myScreenX - 20, myScreenY - 20, 40, 40);
    }
    
    // 이펙트
    drawMyEffects(g2d, ctx);
    ctx.skillEffects.drawSelf(g2d, myScreenX, myScreenY);
    
    // 이름
    g2d.setColor(Color.YELLOW);
    g2d.drawString(ctx.playerName + " (You)", myScreenX - nameWidth / 2, myScreenY - 25);
    
    // HP 바
    drawHealthBar(g2d, myScreenX, myScreenY + 25, ctx.myHP, ctx.myMaxHP);
}
```

**특징**:
- **팀 색상**: RED(빨강), BLUE(파랑)
- **애니메이션 우선**: 애니메이션 있으면 사용, 없으면 원 그리기
- **이펙트 표시**: 스킬 이펙트, 버프 등
- **HP 바**: 체력 비율에 따라 색상 변경

#### 3.3 미니맵
```java
private void drawMinimap(Graphics2D g2d, RenderContext ctx) {
    int minimapWidth = 200;
    int minimapHeight = 150;
    int minimapX = ctx.canvasWidth - minimapWidth - 20;
    int minimapY = 20;
    
    float scaleX = (float) minimapWidth / ctx.mapWidth;
    float scaleY = (float) minimapHeight / ctx.mapHeight;
    
    // 맵 이미지 또는 간단한 배경
    if (ctx.mapImage != null) {
        g2d.drawImage(ctx.mapImage, minimapX, minimapY, minimapWidth, minimapHeight, null);
    } else {
        g2d.setColor(new Color(20, 20, 30, 200));
        g2d.fillRect(minimapX, minimapY, minimapWidth, minimapHeight);
    }
    
    // 플레이어 위치
    int myMinimapX = minimapX + (int) (ctx.playerX * scaleX);
    int myMinimapY = minimapY + (int) (ctx.playerY * scaleY);
    
    g2d.setColor(Color.YELLOW);
    g2d.fillOval(myMinimapX - 4, myMinimapY - 4, 8, 8);
    
    // 시야 범위
    int visionRadius = (int) (VISION_RANGE * ((scaleX + scaleY) * 0.5f));
    g2d.setColor(new Color(255, 255, 255, 30));
    g2d.fillOval(myMinimapX - visionRadius, myMinimapY - visionRadius,
            visionRadius * 2, visionRadius * 2);
    
    // 다른 플레이어 (시야 내 또는 Piper 스킬로 보이는 경우)
    boolean thermalActive = (ctx.piperThermalRemaining > 0f || ctx.teamThermalRemaining > 0f);
    boolean markActive = !thermalActive && (ctx.piperMarkRemaining > 0f || ctx.teamMarkRemaining > 0f);
    
    for (GamePanel.PlayerData pd : ctx.players.values()) {
        int dx = pd.x - ctx.playerX;
        int dy = pd.y - ctx.playerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        boolean shouldShow = false;
        if (thermalActive) {
            shouldShow = true; // 열상: 전체 맵 표시
        } else if (markActive && distance <= extendedRadius) {
            shouldShow = true; // 마크: 확장 시야
        } else if (distance <= VISION_RANGE && inViewport) {
            shouldShow = true; // 일반: 시야 범위 내
        }
        
        if (shouldShow) {
            int otherX = minimapX + (int) (pd.x * scaleX);
            int otherY = minimapY + (int) (pd.y * scaleY);
            g2d.setColor(pd.team == GameConstants.TEAM_BLUE ? Color.BLUE : Color.RED);
            g2d.fillOval(otherX - 3, otherY - 3, 6, 6);
        }
    }
}
```

**특징**:
- **우측 상단 배치**: 200x150 크기
- **스케일 계산**: 맵 크기에 비례하여 축소
- **시야 범위 표시**: 반투명 원으로 시야 표시
- **Piper 스킬 반영**: 열상(전체 맵), 마크(확장 시야)

#### 3.4 HUD (Heads-Up Display)
```java
private void drawHUD(Graphics2D g, RenderContext ctx) {
    // 좌측 상단 정보 패널
    g.setColor(new Color(0, 0, 0, 150));
    g.fillRect(10, 10, 250, 200);
    
    g.setFont(new Font("맑은 고딕", Font.BOLD, 14));
    g.setColor(Color.WHITE);
    
    int yPos = 30;
    g.drawString("플레이어: " + ctx.playerName, 20, yPos);
    yPos += 20;
    g.drawString("팀: " + (ctx.team == GameConstants.TEAM_RED ? "RED" : "BLUE"), 20, yPos);
    yPos += 20;
    g.drawString("캐릭터: " + ctx.currentCharacterData.name, 20, yPos);
    yPos += 20;
    
    g.drawString("HP: " + ctx.myHP + "/" + ctx.myMaxHP, 20, yPos);
    drawHealthBar(g, 130, yPos - 12, ctx.myHP, ctx.myMaxHP);
    yPos += 20;
    
    g.setColor(new Color(255, 215, 0));
    g.drawString("Kills: " + ctx.kills + " / Deaths: " + ctx.deaths, 20, yPos);
    
    drawSkillHUD(g, ctx);
}

private void drawSkillHUD(Graphics2D g, RenderContext ctx) {
    // 하단 중앙 스킬 UI
    int hudWidth = 400;
    int hudHeight = 80;
    int hudX = (ctx.canvasWidth - hudWidth) / 2;
    int hudY = ctx.canvasHeight - hudHeight - 70;
    
    g.setColor(new Color(0, 0, 0, 180));
    g.fillRoundRect(hudX, hudY, hudWidth, hudHeight, 10, 10);
    
    int skillWidth = 60;
    int skillHeight = 60;
    String[] keyLabels = { "좌클릭", "E", "R" };
    Color[] skillColors = {
            new Color(100, 200, 100),
            new Color(100, 150, 255),
            new Color(255, 100, 100)
    };
    
    for (int i = 0; i < 3 && i < ctx.abilities.length; i++) {
        Ability ability = ctx.abilities[i];
        
        // 스킬 박스
        if (ability.canUse()) {
            g.setColor(skillColors[i]);
        } else {
            g.setColor(new Color(40, 40, 40));
        }
        g.fillRoundRect(skillX, skillY, skillWidth, skillHeight, 8, 8);
        
        // 쿨다운 오버레이
        if (!ability.canUse()) {
            float cooldownPercent = ability.getCooldownPercent();
            int overlayHeight = (int) (skillHeight * cooldownPercent);
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRoundRect(skillX, skillY + (skillHeight - overlayHeight),
                    skillWidth, overlayHeight, 8, 8);
            
            // 쿨다운 시간 표시
            String cooldownText = String.format("%.1f", ability.getCurrentCooldown());
            g.drawString(cooldownText, skillX + centerX, skillY + centerY);
        }
        
        // 키 라벨
        g.drawString(keyLabels[i], skillX + centerX, skillY - 5);
        
        // 스킬 이름
        g.drawString(ability.getName(), skillX + centerX, skillY + skillHeight + 15);
    }
}
```

**특징**:
- **좌측 상단**: 플레이어 정보, 체력, 킬/데스
- **하단 중앙**: 스킬 UI (3개 스킬)
- **쿨다운 표시**: 어두운 오버레이 + 남은 시간
- **색상 코딩**: BASIC(초록), TACTICAL(파랑), ULTIMATE(빨강)

#### 3.5 RenderContext
```java
/**
 * 렌더링에 필요한 모든 데이터를 담는 컨텍스트 클래스
 */
public static class RenderContext {
    // 맵 데이터
    public Image mapImage;
    public int mapWidth, mapHeight;
    public int cameraX, cameraY;
    public List<Rectangle> obstacles;
    public boolean debugObstacles;
    
    // 플레이어 데이터
    public String playerName;
    public int team;
    public int playerX, playerY;
    public int myDirection;
    public SpriteAnimation[] myAnimations;
    public int myHP, myMaxHP;
    public int mouseX, mouseY;
    public String selectedCharacter;
    public CharacterData currentCharacterData;
    
    // 다른 플레이어들
    public Map<String, GamePanel.PlayerData> players;
    
    // 게임 오브젝트
    public List<GameObjectManager.Missile> missiles;
    public Map<Integer, GameObjectManager.PlacedObjectClient> placedObjects;
    public Map<Integer, GameObjectManager.StrikeMarker> strikeMarkers;
    
    // 이펙트
    public List<GamePanel.ActiveEffect> myEffects;
    public SkillEffectManager skillEffects;
    
    // UI 상태
    public boolean showMinimap;
    public boolean editMode;
    public int kills, deaths;
    public Ability[] abilities;
    
    // 라운드 정보
    public int redWins, blueWins;
    public int roundCount;
    public GamePanel.RoundState roundState;
    public long roundStartTime;
    public String centerMessage;
    public long centerMessageEndTime;
    
    // Piper 스킬 상태
    public float piperMarkRemaining;
    public float piperThermalRemaining;
    public float teamMarkRemaining;
    public float teamThermalRemaining;
    
    // 캔버스 크기
    public int canvasWidth, canvasHeight;
    public int actualCanvasWidth, actualCanvasHeight;
    
    // 편집 모드 데이터
    public boolean[][] walkableGrid;
    public int tileSize, gridCols, gridRows;
}
```

**특징**:
- **모든 렌더링 데이터 포함**: 60개 이상의 필드
- **public 필드**: 직접 접근 가능 (DTO 패턴)
- **명확한 그룹화**: 맵, 플레이어, 게임 오브젝트, UI 등

### GameRenderer 강점
1. **단일 책임 원칙**: 렌더링만 담당
2. **RenderContext 패턴**: 필요한 데이터만 전달
3. **명확한 렌더링 순서**: 맵 → 플레이어 → UI
4. **재사용 가능**: `drawHealthBar()`, `drawCenterText()` 등 유틸 메서드
5. **Piper 스킬 반영**: 미니맵에 열상/마크 상태 반영

### GameRenderer 개선 제안
1. **RenderContext 불변성**: final 필드로 변경 (읽기 전용)
2. **렌더링 최적화**: 화면 밖 객체 제외 (`isOnScreen()` 활용)
3. **색상 상수화**: 반복되는 색상을 상수로 정의
4. **편집 모드 분리**: `drawEditorOverlay()`를 별도 클래스로 분리

---

## 종합 평가

### 리팩토링 성과

| 클래스 | 라인 수 | 책임 | 평가 |
|--------|---------|------|------|
| **GameState** | 437줄 | 게임 상태 관리 | ⭐⭐⭐⭐⭐ (5.0/5.0) |
| **NetworkClient** | 150줄 | 네트워크 통신 | ⭐⭐⭐⭐⭐ (5.0/5.0) |
| **GameRenderer** | 785줄 | 렌더링 전담 | ⭐⭐⭐⭐☆ (4.5/5.0) |
| **종합** | 1,372줄 | MVC 리팩토링 | ⭐⭐⭐⭐⭐ (4.8/5.0) |

### 주요 성과
1. **단일 책임 원칙 준수**: 각 클래스가 하나의 책임만 가짐
2. **느슨한 결합**: 콜백 패턴, RenderContext 패턴으로 결합도 최소화
3. **테스트 용이성**: 각 컴포넌트를 독립적으로 테스트 가능
4. **가독성 향상**: 2,539줄 → 437줄 + 150줄 + 785줄로 분산
5. **유지보수 용이**: 네트워크 문제는 NetworkClient만, 렌더링 문제는 GameRenderer만 수정

### 개선 영역
1. **GameState**: 불변성 강화, 상태 검증
2. **NetworkClient**: 재연결 로직, 메시지 큐
3. **GameRenderer**: RenderContext 불변성, 색상 상수화

---

## 결론

이 3개 클래스는 **GamePanel의 MVC 리팩토링 핵심 결과물**로, **SOLID 원칙을 잘 따르고 있으며**, 각각의 책임이 명확합니다. 특히 **RenderContext 패턴**과 **콜백 패턴**을 활용하여 **느슨한 결합**을 달성한 점이 인상적입니다.

전반적으로 **매우 성공적인 리팩토링**이며, 나머지 Phase 2 리팩토링(8개 Manager 클래스 분리)을 완료하면 완벽한 MVC 구조가 될 것입니다.
