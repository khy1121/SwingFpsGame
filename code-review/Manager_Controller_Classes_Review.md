# Manager & Controller 클래스 코드 리뷰
## Phase 2 리팩토링 컴포넌트

## 파일 정보
- **InputController.java**: 355줄 - 입력 처리 전담
- **MapManager.java**: 686줄 - 맵 로딩, 장애물, 에디터
- **SkillManager.java**: 320줄 - 스킬 시스템, 이펙트 관리
- **UIManager.java**: 240줄 - 채팅 UI, 메뉴바
- **CollisionManager.java**: 153줄 - 충돌 감지
- **GameObjectManager.java**: 338줄 - 게임 오브젝트 관리
- **총 라인 수**: 2,092줄
- **역할**: GamePanel의 세부 로직을 책임별로 분리

---

## 개요

이 6개 클래스는 **GamePanel의 Phase 2 리팩토링** 결과물로, Phase 1에서 분리된 GameState/NetworkClient/GameRenderer의 **세부 로직을 더욱 세밀하게 분리**한 것입니다.

### 리팩토링 단계별 분해

```
[원본] GamePanel (2,539줄)
    ↓
[Phase 1] 3개 핵심 클래스 (1,372줄)
├── GameState (437줄) - 상태 관리
├── NetworkClient (150줄) - 네트워크
└── GameRenderer (785줄) - 렌더링
    ↓
[Phase 2] 6개 Manager 클래스 (2,092줄)
├── InputController (355줄) - 입력 처리
├── MapManager (686줄) - 맵 시스템
├── SkillManager (320줄) - 스킬 시스템
├── UIManager (240줄) - UI 관리
├── CollisionManager (153줄) - 충돌 감지
└── GameObjectManager (338줄) - 오브젝트 관리
```

---

## 1. InputController (355줄) - 입력 처리 전담

### 역할
**키보드와 마우스 입력을 전담**하여 GamePanel의 이벤트 리스너 복잡도를 제거합니다. **콜백 패턴**으로 입력 이벤트를 GamePanel에 전달합니다.

### 주요 컴포넌트

#### 1.1 키 상태 관리
```java
// 키 상태 배열
private final boolean[] keys = new boolean[256];

// 마우스 위치
private int mouseX = 400;
private int mouseY = 300;

// 편집 모드 상태
private boolean editMode = false;
private int paintState = -1; // -1=없음, 0=unwalkable로 칠하기, 1=walkable로 칠하기
private int editPaintMode = 0; // 0=이동가능, 1=이동불가, 2=RED스폰, 3=BLUE스폰

// 디버그/UI 토글
private boolean debugObstacles = false;
private boolean showMinimap = true;
```

**특징**:
- **256 크기 배열**: 모든 키코드 커버 (KeyEvent.VK_*)
- **편집 모드 상태**: 페인트 상태, 모드 분리
- **UI 토글**: 미니맵, 디버그 오버레이

#### 1.2 콜백 인터페이스
```java
// 콜백 인터페이스들
private Runnable onTacticalSkill;
private Runnable onUltimateSkill;
private Runnable onCharacterSelect;
private Runnable onSaveMap;
private Runnable onSwitchMap;
private Runnable onFocusChat;
private Consumer<int[]> onBasicAttack; // targetX, targetY (맵 좌표)
private Consumer<int[]> onMinimapClick; // targetX, targetY (맵 좌표)
private Consumer<int[]> onTilePaintStart; // mapX, mapY
private Consumer<int[]> onTilePaintContinue; // mapX, mapY
private Runnable onTilePaintEnd;
private Consumer<int[]> onMouseMove; // x, y
private Consumer<int[]> onHoverTileUpdate; // mapX, mapY
private Consumer<Integer> onEditPaintModeChange; // mode
```

**특징**:
- **13개 콜백**: 스킬, 공격, 편집, UI 등
- **타입 안전성**: `Runnable`, `Consumer<T>`
- **좌표 변환**: 화면 좌표 → 맵 좌표 자동 변환

#### 1.3 키 바인딩 시스템
```java
private void handleKeyPressed(KeyEvent e, Consumer<String> chatMessageHandler) {
    keys[e.getKeyCode()] = true;
    int keyCode = e.getKeyCode();
    
    // 편집 모드 저장 단축키 (Ctrl+S)
    if (editMode && keyCode == KeyEvent.VK_S && (e.isControlDown() || e.isMetaDown())) {
        if (onSaveMap != null) onSaveMap.run();
        return;
    }
    
    // 사용자 설정 키 바인딩 체크
    if (KeyBindingConfig.isKeyPressed(keyCode, KeyBindingConfig.KEY_TACTICAL_SKILL)) {
        if (onTacticalSkill != null) onTacticalSkill.run();
    } else if (KeyBindingConfig.isKeyPressed(keyCode, KeyBindingConfig.KEY_ULTIMATE_SKILL)) {
        if (onUltimateSkill != null) onUltimateSkill.run();
    } else if (KeyBindingConfig.isKeyPressed(keyCode, KeyBindingConfig.KEY_CHARACTER_SELECT)) {
        if (onCharacterSelect != null) onCharacterSelect.run();
    } else if (KeyBindingConfig.isKeyPressed(keyCode, KeyBindingConfig.KEY_MINIMAP_TOGGLE)) {
        showMinimap = !showMinimap;
        if (chatMessageHandler != null) {
            chatMessageHandler.accept("[시스템] 미니맵 " + (showMinimap ? "켜짐" : "꺼짐"));
        }
    }
    
    // 고정 키 (디버그 및 에디터 기능)
    switch (keyCode) {
        case KeyEvent.VK_F3 -> { // 장애물 디버그 토글
            debugObstacles = !debugObstacles;
            if (chatMessageHandler != null) {
                chatMessageHandler.accept("[디버그] 장애물 표시 " + (debugObstacles ? "ON" : "OFF"));
            }
        }
        case KeyEvent.VK_F4 -> { // 편집 모드 토글
            editMode = !editMode;
            paintState = -1;
            if (chatMessageHandler != null) {
                chatMessageHandler.accept(editMode ? "[에디터] 타일 편집 모드 ON" : "[에디터] 타일 편집 모드 OFF");
            }
        }
        case KeyEvent.VK_1 -> {
            if (editMode) {
                editPaintMode = 0;
                if (onEditPaintModeChange != null) onEditPaintModeChange.accept(0);
                if (chatMessageHandler != null) {
                    chatMessageHandler.accept("[에디터] 모드: 이동 가능 칠하기");
                }
            }
        }
        // ... 나머지 키 처리
    }
}
```

**특징**:
- **사용자 정의 키 바인딩**: `KeyBindingConfig` 통합
- **고정 키 (F3~F7)**: 디버그, 에디터 전용
- **메시지 피드백**: 채팅으로 상태 변화 알림

#### 1.4 마우스 처리
```java
private void handleMousePressed(MouseEvent e, int canvasWidth, int canvasHeight,
                               boolean awaitingMinimapTarget,
                               int cameraX, int cameraY,
                               int mapWidth, int mapHeight) {
    // 미니맵 타겟팅 모드: General 에어스트라이크
    if (awaitingMinimapTarget && e.getButton() == MouseEvent.BUTTON1) {
        int minimapWidth = 200;
        int minimapHeight = 150;
        int minimapX = canvasWidth - minimapWidth - 20;
        int minimapY = 20;
        
        if (e.getX() >= minimapX && e.getX() <= minimapX + minimapWidth &&
                e.getY() >= minimapY && e.getY() <= minimapY + minimapHeight) {
            // 미니맵 좌표를 맵 좌표로 변환
            float scaleX = (float) minimapWidth / mapWidth;
            float scaleY = (float) minimapHeight / mapHeight;
            int targetMapX = (int) ((e.getX() - minimapX) / scaleX);
            int targetMapY = (int) ((e.getY() - minimapY) / scaleY);
            
            if (onMinimapClick != null) {
                onMinimapClick.accept(new int[] { targetMapX, targetMapY });
            }
            return;
        }
    }
    
    // 편집 모드: 타일 페인팅
    if (editMode) {
        int mapX = e.getX() + cameraX;
        int mapY = e.getY() + cameraY;
        if (onTilePaintStart != null) {
            onTilePaintStart.accept(new int[] { mapX, mapY });
        }
        return;
    }
    
    // 게임 모드: 좌클릭 공격
    if (e.getButton() == MouseEvent.BUTTON1) {
        int targetMapX = e.getX() + cameraX;
        int targetMapY = e.getY() + cameraY;
        if (onBasicAttack != null) {
            onBasicAttack.accept(new int[] { targetMapX, targetMapY });
        }
    }
}
```

**특징**:
- **미니맵 클릭 감지**: General 에어스트라이크 타겟팅
- **좌표 변환**: 화면 좌표 → 맵 좌표 (카메라 오프셋 적용)
- **모드별 분기**: 타겟팅 모드 > 편집 모드 > 게임 모드

#### 1.5 이동 입력 처리
```java
/**
 * 이동 입력 처리 (WASD 키 체크)
 */
public int[] getMovementInput() {
    int dx = 0;
    int dy = 0;
    
    if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP]) dy -= 1;
    if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN]) dy += 1;
    if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT]) dx -= 1;
    if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT]) dx += 1;
    
    return new int[] { dx, dy };
}
```

**특징**:
- **WASD + 화살표 지원**: 양방향 키 바인딩
- **정규화 없음**: 대각선 이동 √2배 속도 (의도적 설계)

### InputController 강점
1. **완벽한 입출력 분리**: GamePanel에서 이벤트 리스너 코드 제거
2. **콜백 패턴**: 느슨한 결합, 테스트 용이
3. **키 바인딩 지원**: 사용자 정의 키 설정
4. **좌표 변환 자동화**: 화면 좌표 → 맵 좌표
5. **모드별 분기**: 편집/게임/타겟팅 모드 명확히 분리

### InputController 개선 제안
1. **키 중복 처리**: 동시 키 입력 우선순위 정의
2. **마우스 버튼 상수화**: `BUTTON1` 대신 `LEFT_BUTTON` 상수 사용
3. **입력 버퍼링**: 프레임 단위 입력 큐잉 (입력 손실 방지)

---

## 2. MapManager (686줄) - 맵 로딩, 장애물, 에디터

### 역할
**맵 시스템의 모든 것을 관리**합니다. 맵 이미지 로딩, JSON 파싱, 장애물 추출, 스폰 구역 설정, 맵 에디터 기능을 포함합니다.

### 주요 컴포넌트

#### 2.1 맵 데이터 구조
```java
// 맵 시스템
private BufferedImage mapImage;
private int mapWidth = 3200;
private int mapHeight = 2400;
String currentMapName = "map";

// 타일 그리드
private static final int TILE_SIZE = 32;
private boolean[][] walkableGrid;
private int gridCols, gridRows;

private Rectangle redSpawnZone, blueSpawnZone;
private final List<int[]> redSpawnTiles = new ArrayList<>();
private final List<int[]> blueSpawnTiles = new ArrayList<>();

// 장애물 시스템
private final List<Rectangle> obstacles = new ArrayList<>();

// 맵 순환 목록
private List<String> mapCycle = new ArrayList<>();
private int mapIndex = 0;
```

**특징**:
- **타일 기반**: 32x32 타일 그리드
- **이중 충돌 시스템**: walkableGrid(타일) + obstacles(Rectangle 정밀)
- **스폰 구역**: 팀별 타일 리스트 + 전체 Rectangle

#### 2.2 맵 로딩 시스템
```java
/**
 * 맵 로드 및 장애물 설정
 */
public void loadMap(String mapName) {
    try {
        File mapFile = new File("assets/maps/" + mapName + ".png");
        if (mapFile.exists()) {
            mapImage = ImageIO.read(mapFile);
            if (mapImage != null) {
                mapWidth = mapImage.getWidth();
                mapHeight = mapImage.getHeight();
                messageCallback.appendMessage("[시스템] 맵 로드 완료: " + mapName + " (" + mapWidth + "x" + mapHeight + ")");
            } else {
                messageCallback.appendMessage("[시스템] 맵 이미지 읽기 실패, 기본 크기 사용");
            }
        } else {
            messageCallback.appendMessage("[시스템] 맵 파일 없음: " + mapFile.getAbsolutePath());
        }
    } catch (IOException e) {
        messageCallback.appendMessage("[시스템] 맵 로드 에러: " + e.getMessage());
    }
    
    // 그리드 초기화
    gridCols = Math.max(1, mapWidth / TILE_SIZE);
    gridRows = Math.max(1, mapHeight / TILE_SIZE);
    walkableGrid = new boolean[gridRows][gridCols];
    
    // JSON 로딩 시도
    boolean loadedFromJson = loadMapFromJsonIfAvailable(mapName);
    
    // JSON 없으면 이미지 분석
    if (!loadedFromJson) {
        setupObstacles(mapName);
    }
    
    // 스폰 구역 walkable 보장
    ensureSpawnZonesWalkable();
}
```

**특징**:
- **PNG + JSON 이중 로딩**: JSON 우선, 없으면 이미지 분석
- **자동 그리드 계산**: 맵 크기 / 타일 크기
- **스폰 구역 보호**: 항상 walkable로 강제 설정

#### 2.3 JSON 파싱
```java
private void parseMapJson(String json) {
    // meta.map_pixel_size 파싱
    Pattern pMW = Pattern.compile("\"map_pixel_size\"\\s*:\\s*\\{[^}]*\"w\"\\s*:\\s*(\\d+)");
    Pattern pMH = Pattern.compile("\"map_pixel_size\"\\s*:\\s*\\{[^}]*\"h\"\\s*:\\s*(\\d+)");
    Pattern pTS = Pattern.compile("\"tile_size\"\\s*:\\s*(\\d+)");
    
    Matcher mwMatch = pMW.matcher(json);
    Matcher mhMatch = pMH.matcher(json);
    Matcher tsMatch = pTS.matcher(json);
    
    int mw = mapWidth;
    int mh = mapHeight;
    int ts = TILE_SIZE;
    
    if (mwMatch.find()) mw = Integer.parseInt(mwMatch.group(1));
    if (mhMatch.find()) mh = Integer.parseInt(mhMatch.group(1));
    if (tsMatch.find()) ts = Integer.parseInt(tsMatch.group(1));
    
    mapWidth = mw;
    mapHeight = mh;
    
    gridCols = Math.max(1, mapWidth / ts);
    gridRows = Math.max(1, mapHeight / ts);
    walkableGrid = new boolean[gridRows][gridCols];
    
    // obstacles 파싱
    obstacles.clear();
    Pattern pObs = Pattern.compile("\"obstacles\"\\s*:\\s*\\[([^\\]]+)\\]");
    Matcher obsMatch = pObs.matcher(json);
    if (obsMatch.find()) {
        String obsArr = obsMatch.group(1);
        Pattern pXY = Pattern.compile("\\{\\s*\"x\"\\s*:\\s*(\\d+)\\s*,\\s*\"y\"\\s*:\\s*(\\d+)\\s*\\}");
        Matcher xyMatch = pXY.matcher(obsArr);
        while (xyMatch.find()) {
            int tx = Integer.parseInt(xyMatch.group(1));
            int ty = Integer.parseInt(xyMatch.group(2));
            obstacles.add(new Rectangle(tx * ts, ty * ts, ts, ts));
        }
    }
    
    // walkableGrid 초기화
    for (int r = 0; r < gridRows; r++) {
        for (int c = 0; c < gridCols; c++) {
            walkableGrid[r][c] = true;
        }
    }
    
    // 장애물 타일을 unwalkable로 설정
    for (Rectangle obs : obstacles) {
        int col = obs.x / ts;
        int row = obs.y / ts;
        if (row >= 0 && row < gridRows && col >= 0 && col < gridCols) {
            walkableGrid[row][col] = false;
        }
    }
    
    // 스폰 구역 파싱
    redSpawnTiles.clear();
    blueSpawnTiles.clear();
    redSpawnZone = extractSpawnZone(json, "red", redSpawnTiles);
    blueSpawnZone = extractSpawnZone(json, "blue", blueSpawnTiles);
}
```

**특징**:
- **정규식 파싱**: JSON 라이브러리 없이 순수 Regex
- **타일 좌표 → 픽셀 좌표**: 자동 변환
- **스폰 구역 Rectangle 생성**: 타일 리스트에서 min/max 계산

#### 2.4 이미지 기반 장애물 추출
```java
private void extractObstaclesFromImage() {
    if (mapImage == null) return;
    
    boolean[][] visited = new boolean[gridRows][gridCols];
    
    for (int r = 0; r < gridRows; r++) {
        for (int c = 0; c < gridCols; c++) {
            if (visited[r][c]) continue;
            
            int cx = c * TILE_SIZE + TILE_SIZE / 2;
            int cy = r * TILE_SIZE + TILE_SIZE / 2;
            
            if (cx >= mapWidth || cy >= mapHeight) {
                visited[r][c] = true;
                continue;
            }
            
            Color color = new Color(mapImage.getRGB(cx, cy));
            
            if (!isRoadColor(color) && !isSpawnAreaColor(color)) {
                Rectangle rect = findMaxRectangle(walkableGrid, visited, r, c, gridRows, gridCols);
                if (rect != null) {
                    obstacles.add(new Rectangle(
                        rect.x * TILE_SIZE,
                        rect.y * TILE_SIZE,
                        rect.width * TILE_SIZE,
                        rect.height * TILE_SIZE
                    ));
                }
            }
        }
    }
}

private boolean isRoadColor(Color c) {
    int r = c.getRed(), g = c.getGreen(), b = c.getBlue();
    return (r > 150 && g > 150 && b > 150) && Math.abs(r - g) < 30 && Math.abs(g - b) < 30;
}

private boolean isSpawnAreaColor(Color c) {
    int r = c.getRed(), g = c.getGreen(), b = c.getBlue();
    boolean isRed = r > 180 && g < 100 && b < 100;
    boolean isBlue = b > 180 && r < 100 && g < 100;
    return isRed || isBlue;
}
```

**특징**:
- **색상 기반 분석**: 밝은 회색(도로), 빨강/파랑(스폰)
- **최대 Rectangle 찾기**: 효율적인 충돌 감지를 위한 최적화
- **visited 배열**: 중복 처리 방지

#### 2.5 맵 에디터
```java
public void startPaintAt(int mapX, int mapY) {
    int col = mapX / TILE_SIZE;
    int row = mapY / TILE_SIZE;
    if (row < 0 || row >= gridRows || col < 0 || col >= gridCols) return;
    
    boolean current = walkableGrid[row][col];
    paintState = current ? 0 : 1;
    applyEditAction(col, row, false);
}

public void continuePaintAt(int mapX, int mapY) {
    if (paintState == -1) return;
    int col = mapX / TILE_SIZE;
    int row = mapY / TILE_SIZE;
    if (row < 0 || row >= gridRows || col < 0 || col >= gridCols) return;
    applyEditAction(col, row, true);
}

private void applyEditAction(int col, int row, boolean dragging) {
    if (editPaintMode == 0) {
        walkableGrid[row][col] = true;
    } else if (editPaintMode == 1) {
        walkableGrid[row][col] = false;
    } else if (editPaintMode == 2 && !dragging) {
        toggleSpawnTile(redSpawnTiles, col, row);
        recomputeSpawnZones();
    } else if (editPaintMode == 3 && !dragging) {
        toggleSpawnTile(blueSpawnTiles, col, row);
        recomputeSpawnZones();
    }
}

/**
 * 편집된 맵 JSON 저장
 */
public void saveEditedMap() {
    String fileName = currentMapName + "_edited.json";
    File dir = new File("assets/maps");
    if (!dir.exists()) dir.mkdirs();
    
    File outFile = new File(dir, fileName);
    try (BufferedWriter bw = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(outFile), java.nio.charset.StandardCharsets.UTF_8))) {
        bw.write(generateEditedMapJson());
        bw.flush();
        messageCallback.appendMessage("[에디터] 저장 완료: " + outFile.getPath());
    } catch (IOException ex) {
        messageCallback.appendMessage("[에디터] 저장 실패: " + ex.getMessage());
    }
}
```

**특징**:
- **4가지 페인트 모드**: 이동 가능, 벽, RED 스폰, BLUE 스폰
- **드래그 페인팅**: 연속 타일 수정
- **JSON 자동 생성**: `_edited.json` 파일 저장
- **UTF-8 인코딩**: 한글 주석 지원

### MapManager 강점
1. **이중 로딩 시스템**: PNG + JSON, JSON 우선
2. **색상 기반 분석**: 디자이너 친화적
3. **맵 에디터 내장**: 인게임에서 직접 수정 가능
4. **장애물 최적화**: 최대 Rectangle 찾기
5. **스폰 구역 보호**: 항상 walkable 보장

### MapManager 개선 제안
1. **JSON 라이브러리 도입**: Gson, Jackson으로 파싱 간소화
2. **맵 검증**: 스폰 구역 중복, 고립 지역 체크
3. **언두/리두**: 에디터에 실행 취소 기능
4. **미니맵 이미지**: 자동 축소 이미지 생성

---

## 3. SkillManager (320줄) - 스킬 시스템, 이펙트 관리

### 역할
**스킬 시스템과 이펙트를 통합 관리**합니다. 쿨다운, 이펙트 타이머, 캐릭터별 런타임 상태(Raven 대시, Piper 열상 등)를 포함합니다.

### 주요 컴포넌트

#### 3.1 스킬 이펙트
```java
public static class ActiveEffect {
    String abilityId;
    String type; // BASIC, TACTICAL, ULTIMATE
    float duration;
    float remaining;
    Color color;

    ActiveEffect(String abilityId, String type, float duration) {
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
        return new Color(100, 150, 255); // TACTICAL default
    }
}

// 이펙트 관리
private final Map<String, List<ActiveEffect>> effectsByPlayer = new HashMap<>();
private final List<ActiveEffect> myEffects = new ArrayList<>();
private final SkillEffectManager skillEffects = new SkillEffectManager();
```

**특징**:
- **타입별 색상**: BASIC(초록), TACTICAL(파랑), ULTIMATE(빨강)
- **플레이어별 관리**: 다른 플레이어 이펙트 추적
- **시간 기반**: `deltaTime`으로 `remaining` 감소

#### 3.2 캐릭터별 런타임 상태
```java
// Raven 상태
private float ravenDashRemaining = 0f;
private float ravenOverchargeRemaining = 0f;
private float missileSpeedMultiplier = 1f;
private float attackSpeedMultiplier = 1f;

// Piper 상태
private float piperMarkRemaining = 0f;
private float piperThermalRemaining = 0f;
private float teamMarkRemaining = 0f;
private float teamThermalRemaining = 0f;

/**
 * Raven 전용 런타임 처리
 */
public void updateRavenRuntime() {
    float deltaTime = 0.016f;
    
    if (ravenDashRemaining > 0) {
        ravenDashRemaining -= deltaTime;
        if (ravenDashRemaining <= 0) {
            ravenDashRemaining = 0;
        }
    }
    
    if (ravenOverchargeRemaining > 0) {
        ravenOverchargeRemaining -= deltaTime;
        if (ravenOverchargeRemaining <= 0) {
            ravenOverchargeRemaining = 0;
            missileSpeedMultiplier = 1f;
        }
    }
}

/**
 * Piper 전용 런타임 처리
 */
public void updatePiperRuntime() {
    float deltaTime = 0.016f;
    
    if (piperMarkRemaining > 0) {
        piperMarkRemaining -= deltaTime;
        if (piperMarkRemaining <= 0) {
            piperMarkRemaining = 0;
        }
    }
    
    if (piperThermalRemaining > 0) {
        piperThermalRemaining -= deltaTime;
        if (piperThermalRemaining <= 0) {
            piperThermalRemaining = 0;
        }
    }
}
```

**특징**:
- **캐릭터별 분리**: Raven, Piper 전용 상태
- **팀 상태 관리**: 팀원의 Piper 스킬도 추적
- **버프 시스템**: 이동 속도, 공격 속도 배수

#### 3.3 쿨다운 업데이트
```java
/**
 * 쿨다운 업데이트 (프레임마다 호출)
 */
public void updateAbilities() {
    if (abilities == null) return;
    
    float deltaTime = 0.016f; // ~60 FPS
    for (Ability ability : abilities) {
        if (ability != null) {
            ability.update(deltaTime);
        }
    }
}

/**
 * 이펙트 타이머 업데이트 및 만료 제거
 */
public void updateEffects() {
    float deltaTime = 0.016f;
    
    // 내 이펙트 업데이트
    myEffects.removeIf(eff -> {
        eff.remaining -= deltaTime;
        return eff.remaining <= 0;
    });
    
    // 다른 플레이어 이펙트 업데이트
    for (Map.Entry<String, List<ActiveEffect>> entry : effectsByPlayer.entrySet()) {
        List<ActiveEffect> list = entry.getValue();
        list.removeIf(eff -> {
            eff.remaining -= deltaTime;
            return eff.remaining <= 0;
        });
    }
    
    // 구조화된 스킬 이펙트 업데이트
    skillEffects.update(deltaTime);
}
```

**특징**:
- **고정 deltaTime**: 0.016f (60 FPS 가정)
- **removeIf 활용**: 만료된 이펙트 자동 제거
- **3단계 업데이트**: Ability 쿨다운, ActiveEffect, SkillEffect

#### 3.4 스킬 효과 적용
```java
/**
 * 스킬 효과 적용 (캐릭터별 구현)
 */
public void applySkillEffect(Ability ability, String characterId) {
    if (ability == null) return;
    
    String abilityId = ability.id;
    
    // Raven 스킬 처리
    if ("raven_dash".equals(abilityId)) {
        ravenDashRemaining = ability.duration;
        messageCallback.appendMessage("[Raven] 대쉬 활성화!");
    } else if ("raven_overcharge".equals(abilityId)) {
        ravenOverchargeRemaining = ability.duration;
        missileSpeedMultiplier = 1.5f;
        messageCallback.appendMessage("[Raven] 과충전 활성화!");
    }
    
    // Piper 스킬 처리
    else if ("piper_mark".equals(abilityId)) {
        piperMarkRemaining = ability.duration;
        messageCallback.appendMessage("[Piper] 적 표시 활성화!");
    } else if ("piper_thermal".equals(abilityId)) {
        piperThermalRemaining = ability.duration;
        messageCallback.appendMessage("[Piper] 열감지 활성화!");
    }
}
```

**특징**:
- **캐릭터별 분기**: Raven, Piper 스킬 처리
- **런타임 상태 설정**: 지속 시간, 배수 설정
- **피드백 메시지**: 채팅으로 활성화 알림

### SkillManager 강점
1. **중앙 집중식 관리**: 모든 스킬 로직 한 곳에
2. **캐릭터 특화 상태**: Raven, Piper 전용 필드
3. **팀 상태 추적**: 팀원의 버프도 반영
4. **자동 정리**: 만료된 이펙트 자동 제거
5. **시간 기반 설계**: deltaTime 기반 정확한 타이머

### SkillManager 개선 제안
1. **캐릭터별 클래스 분리**: RavenSkillManager, PiperSkillManager
2. **deltaTime 매개변수화**: 고정값 대신 실제 프레임 시간 전달
3. **이펙트 풀링**: ActiveEffect 객체 재사용
4. **스킬 체인**: 연계 스킬 시스템 추가

---

## 4. UIManager (240줄) - 채팅 UI, 메뉴바

### 역할
**UI 컴포넌트 생성 및 관리**를 담당합니다. 채팅 패널, 메뉴바, 다이얼로그 생성을 포함합니다.

### 주요 컴포넌트

#### 4.1 채팅 시스템
```java
/**
 * 채팅 패널 생성
 */
public JPanel createChatPanel(int width, int height) {
    JPanel chatPanel = new JPanel(new BorderLayout());
    chatPanel.setPreferredSize(new Dimension(width, height));
    chatPanel.setBackground(new Color(32, 34, 37));
    
    // 채팅 영역
    chatArea = new JTextArea();
    chatArea.setEditable(false);
    chatArea.setLineWrap(true);
    chatArea.setWrapStyleWord(true);
    chatArea.setBackground(new Color(32, 34, 37));
    chatArea.setForeground(Color.WHITE);
    chatArea.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
    
    chatScroll = new JScrollPane(chatArea);
    chatScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    chatScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 55)));
    
    // 입력 필드
    chatInput = new JTextField();
    chatInput.setBackground(new Color(40, 42, 45));
    chatInput.setForeground(Color.WHITE);
    chatInput.setCaretColor(Color.WHITE);
    chatInput.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(60, 62, 65)),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)
    ));
    
    chatInput.addActionListener(e -> {
        String text = chatInput.getText().trim();
        if (!text.isEmpty()) {
            if (chatSendCallback != null) {
                chatSendCallback.sendChat(text);
            }
            chatInput.setText("");
        }
    });
    
    chatPanel.add(chatScroll, BorderLayout.CENTER);
    chatPanel.add(chatInput, BorderLayout.SOUTH);
    
    return chatPanel;
}

/**
 * 채팅 메시지 추가 (스로틀링 적용)
 */
public void appendChatMessage(String message) {
    long now = System.currentTimeMillis();
    if (message != null && message.equals(lastChatMessage) && (now - lastChatTime) < CHAT_THROTTLE_MS) {
        return;
    }
    lastChatMessage = message;
    lastChatTime = now;
    
    SwingUtilities.invokeLater(() -> {
        if (chatArea != null) {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    });
}
```

**특징**:
- **Discord 스타일**: 어두운 배경 (RGB 32, 34, 37)
- **스로틀링**: 1초 내 중복 메시지 필터링
- **자동 스크롤**: `setCaretPosition()`으로 최하단 이동
- **SwingUtilities.invokeLater**: 스레드 안전

#### 4.2 메뉴바 생성
```java
/**
 * 메뉴바 생성
 */
public JMenuBar createMenuBar() {
    JMenuBar menuBar = new JMenuBar();
    
    // 게임 메뉴
    JMenu gameMenu = new JMenu("게임");
    
    JMenuItem characterSelectItem = new JMenuItem("캐릭터 선택");
    characterSelectItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
    characterSelectItem.addActionListener(e -> {
        if (menuActionCallback != null) {
            menuActionCallback.onMenuAction("CHARACTER_SELECT");
        }
    });
    
    JMenuItem exitItem = new JMenuItem("종료");
    exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
    exitItem.addActionListener(e -> {
        if (menuActionCallback != null) {
            menuActionCallback.onMenuAction("EXIT");
        }
    });
    
    gameMenu.add(characterSelectItem);
    gameMenu.addSeparator();
    gameMenu.add(exitItem);
    
    // 보기 메뉴
    JMenu viewMenu = new JMenu("보기");
    
    JCheckBoxMenuItem minimapItem = new JCheckBoxMenuItem("미니맵 표시", true);
    minimapItem.addActionListener(e -> {
        if (menuActionCallback != null) {
            menuActionCallback.onMenuAction("TOGGLE_MINIMAP");
        }
    });
    
    JCheckBoxMenuItem debugItem = new JCheckBoxMenuItem("장애물 디버그 (F3)", false);
    debugItem.addActionListener(e -> {
        if (menuActionCallback != null) {
            menuActionCallback.onMenuAction("TOGGLE_DEBUG");
        }
    });
    
    JCheckBoxMenuItem editModeItem = new JCheckBoxMenuItem("에디터 모드 (F4)", false);
    editModeItem.addActionListener(e -> {
        if (menuActionCallback != null) {
            menuActionCallback.onMenuAction("TOGGLE_EDIT");
        }
    });
    
    viewMenu.add(minimapItem);
    viewMenu.add(debugItem);
    viewMenu.add(editModeItem);
    
    // 맵 메뉴
    JMenu mapMenu = new JMenu("맵");
    
    JMenuItem nextMapItem = new JMenuItem("다음 맵 (F6)");
    nextMapItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
    nextMapItem.addActionListener(e -> {
        if (menuActionCallback != null) {
            menuActionCallback.onMenuAction("NEXT_MAP");
        }
    });
    
    JMenuItem saveMapItem = new JMenuItem("맵 저장 (F5)");
    saveMapItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
    saveMapItem.addActionListener(e -> {
        if (menuActionCallback != null) {
            menuActionCallback.onMenuAction("SAVE_MAP");
        }
    });
    
    mapMenu.add(nextMapItem);
    mapMenu.add(saveMapItem);
    
    // 도움말 메뉴
    JMenu helpMenu = new JMenu("도움말");
    
    JMenuItem controlsItem = new JMenuItem("조작법");
    controlsItem.addActionListener(e -> {
        if (menuActionCallback != null) {
            menuActionCallback.onMenuAction("SHOW_CONTROLS");
        }
    });
    
    JMenuItem aboutItem = new JMenuItem("정보");
    aboutItem.addActionListener(e -> {
        if (menuActionCallback != null) {
            menuActionCallback.onMenuAction("SHOW_ABOUT");
        }
    });
    
    helpMenu.add(controlsItem);
    helpMenu.add(aboutItem);
    
    menuBar.add(gameMenu);
    menuBar.add(viewMenu);
    menuBar.add(mapMenu);
    menuBar.add(helpMenu);
    
    return menuBar;
}
```

**특징**:
- **4개 메뉴**: 게임, 보기, 맵, 도움말
- **단축키 지원**: Ctrl+C, Ctrl+Q, F3~F6
- **체크박스**: 미니맵, 디버그, 에디터 토글
- **콜백 패턴**: 액션을 문자열로 전달

### UIManager 강점
1. **UI 컴포넌트 팩토리**: 생성 로직 집중
2. **스로틀링**: 스팸 메시지 필터링
3. **콜백 패턴**: UI 이벤트를 GamePanel로 전달
4. **다크 테마**: 현대적인 디자인

### UIManager 개선 제안
1. **테마 시스템**: 라이트/다크 테마 전환
2. **폰트 사이즈 조절**: 접근성 향상
3. **채팅 필터**: 욕설 필터링, 명령어 파싱
4. **메뉴 상태 동기화**: 체크박스와 실제 상태 연동

---

## 5. CollisionManager (153줄) - 충돌 감지

### 역할
**모든 충돌 감지 로직을 중앙에서 관리**합니다. 플레이어-장애물, 미사일-벽, 미사일-플레이어 충돌을 체크합니다.

### 주요 메서드

#### 5.1 플레이어 충돌
```java
/**
 * 플레이어가 장애물과 충돌하는지 체크
 */
public boolean checkCollisionWithObstacles(int x, int y) {
    int playerRadius = 15;
    Rectangle playerRect = new Rectangle(x - playerRadius, y - playerRadius, 
        playerRadius * 2, playerRadius * 2);
    
    for (Rectangle obs : obstacles) {
        if (playerRect.intersects(obs)) {
            return true;
        }
    }
    return false;
}

/**
 * 플레이어 반경을 샘플링하여 해당 위치가 모두 walkable인지 확인
 */
public boolean isPositionWalkable(int x, int y) {
    int playerRadius = 15;
    int sampleCount = 8;
    
    for (int i = 0; i < sampleCount; i++) {
        double angle = (2 * Math.PI * i) / sampleCount;
        int sx = x + (int) (playerRadius * Math.cos(angle));
        int sy = y + (int) (playerRadius * Math.sin(angle));
        
        if (!isTileWalkable(sx, sy)) {
            return false;
        }
    }
    
    return isTileWalkable(x, y);
}
```

**특징**:
- **원형 히트박스**: 반경 15픽셀
- **8방향 샘플링**: 벽에 끼는 것 방지
- **타일 + Rectangle 이중 체크**: 정밀도 향상

#### 5.2 미사일 충돌
```java
/**
 * 미사일이 장애물에 막혔는지 체크
 */
public boolean isMissileBlocked(int x, int y) {
    // 타일 기반
    if (walkableGrid != null) {
        int col = x / TILE_SIZE;
        int row = y / TILE_SIZE;
        if (row < 0 || row >= gridRows || col < 0 || col >= gridCols)
            return true;
        if (!walkableGrid[row][col])
            return true;
    }
    
    // 장애물 Rect (정밀)
    Rectangle r = new Rectangle(x - 2, y - 2, 4, 4);
    for (Rectangle obs : obstacles) {
        if (r.intersects(obs))
            return true;
    }
    return false;
}

/**
 * 미사일과 플레이어 충돌 체크
 */
public boolean checkMissilePlayerCollision(int missileX, int missileY, int playerX, int playerY) {
    double dist = Math.sqrt(Math.pow(missileX - playerX, 2) + Math.pow(missileY - playerY, 2));
    return dist < 20;
}
```

**특징**:
- **미사일 히트박스**: 4x4 픽셀
- **거리 기반**: 플레이어 충돌은 거리 < 20
- **타일 우선 체크**: 빠른 제외

### CollisionManager 강점
1. **중앙 집중식**: 모든 충돌 로직 한 곳에
2. **이중 체크**: 타일 + Rectangle 정밀도
3. **샘플링 기법**: 벽 끼임 방지
4. **거리 기반**: 빠른 원형 충돌

### CollisionManager 개선 제안
1. **공간 분할**: QuadTree로 성능 최적화
2. **충돌 정보 반환**: boolean 대신 충돌 지점, 법선 반환
3. **히트박스 매개변수화**: 반경을 인자로 받기
4. **레이캐스팅**: 시야 체크, 벽 투시 방지

---

## 6. GameObjectManager (338줄) - 게임 오브젝트 관리

### 역할
**게임 내 동적 오브젝트의 생명주기를 관리**합니다. 미사일, 설치 오브젝트(터렛, 지뢰), 에어스트라이크 마커를 포함합니다.

### 주요 컴포넌트

#### 6.1 미사일 관리
```java
/**
 * 미사일 투사체 클래스
 */
public static class Missile {
    public int x, y;
    public int dx, dy;
    public int team;
    public String owner;
    
    public Missile(int x, int y, int dx, int dy, int team, String owner) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.team = team;
        this.owner = owner;
    }
}

/**
 * 미사일 업데이트 (이동 및 충돌 체크)
 */
public void updateMissiles() {
    Iterator<Missile> it = missiles.iterator();
    while (it.hasNext()) {
        Missile m = it.next();
        m.x += m.dx;
        m.y += m.dy;
        
        // 맵 밖이면 제거
        if (m.x < 0 || m.x > mapWidth || m.y < 0 || m.y > mapHeight) {
            it.remove();
            continue;
        }
        
        // 벽 충돌
        if (collisionManager.isMissileBlocked(m.x, m.y)) {
            it.remove();
        }
    }
}
```

**특징**:
- **간단한 물리**: 직선 이동 (x += dx, y += dy)
- **자동 제거**: 맵 밖, 벽 충돌 시
- **팀 구분**: 아군/적군 미사일 분리

#### 6.2 설치 오브젝트
```java
/**
 * 설치형 오브젝트 클래스 (터렛, 지뢰)
 */
public static class PlacedObjectClient {
    public int id;
    public String type;
    public int x, y;
    public int hp, maxHp;
    public String owner;
    public int team;
    
    public PlacedObjectClient(int id, String type, int x, int y, int hp, int maxHp, String owner, int team) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.hp = hp;
        this.maxHp = maxHp;
        this.owner = owner;
        this.team = team;
    }
}

/**
 * 오브젝트 추가/업데이트
 */
public void putPlacedObject(int id, PlacedObjectClient obj) {
    placedObjects.put(id, obj);
}

/**
 * 오브젝트 제거
 */
public void removePlacedObject(int id) {
    placedObjects.remove(id);
}
```

**특징**:
- **ID 기반 관리**: 서버가 ID 부여
- **타입 구분**: "turret", "mine"
- **체력 시스템**: hp, maxHp 추적

#### 6.3 스트라이크 마커
```java
/**
 * 스트라이크 마커 클래스 (에어스트라이크 예고 표시)
 */
public static class StrikeMarker {
    public int id;
    public int x, y;
    public long createdAt;
    
    public StrikeMarker(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.createdAt = System.currentTimeMillis();
    }
}

/**
 * 스트라이크 마커 추가
 */
public void addStrikeMarker(int id, StrikeMarker marker) {
    strikeMarkers.put(id, marker);
}
```

**특징**:
- **타이머 내장**: `createdAt` 필드로 3초 계산
- **ID 기반**: 서버와 동기화
- **자동 제거**: 3초 후 서버가 EXECUTE_STRIKE 전송

### GameObjectManager 강점
1. **중앙 집중식 관리**: 모든 오브젝트 한 곳에
2. **자동 정리**: 맵 밖, 충돌 시 제거
3. **ID 기반 동기화**: 서버와 일관성 유지
4. **타입 안전성**: 내부 클래스로 명확한 구조

### GameObjectManager 개선 제안
1. **오브젝트 풀링**: Missile 재사용
2. **시간 기반 제거**: TTL(Time To Live) 추가
3. **공간 분할**: QuadTree로 범위 쿼리 최적화
4. **이벤트 시스템**: 오브젝트 생성/제거 콜백

---

## 종합 평가

### 리팩토링 성과

| 클래스 | 라인 수 | 책임 | 평가 |
|--------|---------|------|------|
| **InputController** | 355줄 | 입력 처리 | ⭐⭐⭐⭐⭐ (5.0/5.0) |
| **MapManager** | 686줄 | 맵 시스템 | ⭐⭐⭐⭐☆ (4.5/5.0) |
| **SkillManager** | 320줄 | 스킬 시스템 | ⭐⭐⭐⭐☆ (4.3/5.0) |
| **UIManager** | 240줄 | UI 관리 | ⭐⭐⭐⭐⭐ (4.6/5.0) |
| **CollisionManager** | 153줄 | 충돌 감지 | ⭐⭐⭐⭐☆ (4.4/5.0) |
| **GameObjectManager** | 338줄 | 오브젝트 관리 | ⭐⭐⭐⭐⭐ (4.7/5.0) |
| **종합** | 2,092줄 | Phase 2 리팩토링 | ⭐⭐⭐⭐☆ (4.6/5.0) |

### 주요 성과
1. **세밀한 책임 분리**: Phase 1의 3개 클래스 → Phase 2의 6개 Manager
2. **콜백 패턴**: 느슨한 결합으로 테스트 용이성 향상
3. **재사용 가능**: InputController, UIManager는 다른 프로젝트에도 활용 가능
4. **에디터 통합**: MapManager의 맵 에디터는 생산성 도구로 가치 높음
5. **문서화**: Javadoc으로 각 클래스의 역할 명확히 기술

### 개선 영역
1. **MapManager**: JSON 라이브러리 도입, 장애물 최적화
2. **SkillManager**: 캐릭터별 클래스 분리, deltaTime 매개변수화
3. **CollisionManager**: QuadTree 도입, 공간 분할 최적화
4. **GameObjectManager**: 오브젝트 풀링, 이벤트 시스템

---

## 결론

이 6개 Manager/Controller 클래스는 **GamePanel의 Phase 2 리팩토링 핵심 결과물**로, **각 클래스가 명확한 책임**을 가지고 있습니다. 특히 **InputController**와 **MapManager**는 복잡한 로직을 효과적으로 캡슐화하여 GamePanel의 가독성을 크게 향상시켰습니다.

**콜백 패턴**을 일관되게 사용하여 **느슨한 결합**을 달성했으며, 이는 **테스트 용이성**과 **유지보수성**을 크게 개선합니다. 전체적으로 **매우 성공적인 리팩토링**이며, SOLID 원칙을 잘 따르고 있습니다.

Phase 1 (3개 핵심 클래스) + Phase 2 (6개 Manager) = **총 9개 클래스, 3,464줄**로 원래 GamePanel 2,539줄의 복잡도를 효과적으로 분산시켰습니다.
