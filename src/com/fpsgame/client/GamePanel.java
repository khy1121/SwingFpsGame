package com.fpsgame.client;

import com.fpsgame.client.effects.*;
import com.fpsgame.common.Ability;
import com.fpsgame.common.CharacterData;
import com.fpsgame.common.GameConstants;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * 게임 패널 - 기존 디자인 유지하면서 간단한 로직
 * 예제 코드처럼 단순한 게임 화면
 * 
 * 리팩토링: GameState, NetworkClient, GameRenderer 사용하여 책임 분리
 */
public class GamePanel extends JFrame implements KeyListener {

    // 게임 상태 관리 객체 (리팩토링)
    final GameState gameState;
    
    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;
    
    // 네트워크 통신 관리 (리팩토링)
    private final NetworkClient networkClient;
    
    // 렌더링 관리 (리팩토링)
    private final GameRenderer gameRenderer;
    
    // 메시지 처리 관리 (리팩토링)
    private final GameMessageHandler messageHandler;

    // Backward compatibility - keep fields but sync with gameState
    final String playerName;
    final int team;
    
    private javax.swing.Timer timer;
    int playerX = 400;
    int playerY = 300;
    private final int SPEED = 5;
    private final boolean[] keys = new boolean[256];

    // 스킬 시스템 (매 프레임 update 필요하므로 로컬 유지)
    Ability[] abilities; // [기본공격, 전술스킬, 궁극기]

    // 스킬 이펙트 (네트워크 포함)
    public static class ActiveEffect {
        String abilityId;
        String type; // BASIC, TACTICAL, ULTIMATE
        float duration; // 총 지속시간(초)
        float remaining; // 남은 시간(초)
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

    // 다른 플레이어 이펙트, 내 이펙트
    final Map<String, java.util.List<ActiveEffect>> effectsByPlayer = new HashMap<>();
    final java.util.List<ActiveEffect> myEffects = new ArrayList<>();
    // Structured skill effects
    final SkillEffectManager skillEffects = new SkillEffectManager();

    // Raven 전용 런타임 상태(첫 캐릭터 구현 시작점)
    private float ravenDashRemaining = 0f;
    private float ravenOverchargeRemaining = 0f;
    private float missileSpeedMultiplier = 1f; // 과충전 시 투사체 속도 상승
    // Piper 전용 런타임 상태
    private float piperMarkRemaining = 0f; // 전술 스킬: 적 표시 (시야 확장)
    private float piperThermalRemaining = 0f; // 궁극기: 열감지 (전체 위치 표시)
    // 팀 공유 (같은 팀 Piper가 사용 시 우리도 혜택)
    private float teamMarkRemaining = 0f;
    private float teamThermalRemaining = 0f;

    // 다른 플레이어들
    final Map<String, PlayerData> players = new HashMap<>();

    // 미사일 리스트
    final List<Missile> missiles = new ArrayList<>();

    // 게임 패널
    private GameCanvas canvas;

    // 마우스 위치 (조준선용)
    private int mouseX = 400;
    private int mouseY = 300;

    // 미니맵 표시 여부
    private boolean showMinimap = true;

    // 맵 시스템
    private java.awt.image.BufferedImage mapImage; // 맵 배경 이미지
    private int mapWidth = 3200; // 맵 전체 크기 (넓은 맵, 화면의 4배)
    private int mapHeight = 2400; // 화면의 4배
    private int cameraX = 0; // 카메라 위치 (플레이어 중심)
    private int cameraY = 0;
    String currentMapName = "map"; // 기본 맵 (서버가 ROUND_START에서 변경 가능)
    // 타일 그리드
    private static final int TILE_SIZE = 32;
    private boolean[][] walkableGrid; // true = 이동 가능
    private int gridCols, gridRows;
    
    // UI 상수
    private static final int MINIMAP_WIDTH = 200;
    private static final int MINIMAP_HEIGHT = 150;
    private static final int UI_MARGIN = 20;
    public static final int CHAT_PANEL_WIDTH = 250;
    private Rectangle redSpawnZone, blueSpawnZone; // 팀 스폰 구역
    // 스폰 타일 원본 목록 (랜덤 스폰을 타일 단위로 정확히 하도록 유지)
    private final java.util.List<int[]> redSpawnTiles = new ArrayList<>();
    private final java.util.List<int[]> blueSpawnTiles = new ArrayList<>();

    // 장애물 시스템
    private final java.util.List<Rectangle> obstacles = new ArrayList<>();
    // 디버그 토글
    private boolean debugObstacles = false; // F3로 토글

    // 맵 편집 모드 (타일 walkable 페인팅)
    private boolean editMode = false; // F4 토글
    private int hoverCol = -1, hoverRow = -1; // 마우스 오버 타일
    // 드래그 페인트 상태: -1=없음, 0=unwalkable로 칠하기, 1=walkable로 칠하기
    private int paintState = -1;
    // 편집 페인트 모드: 0=이동 가능 칠하기, 1=이동 불가(장애물) 칠하기, 2=RED 스폰, 3=BLUE 스폰
    private int editPaintMode = 0;
    // 맵 순환 목록 및 인덱스 (F6)
    private java.util.List<String> mapCycle = new ArrayList<>();
    private int mapIndex = 0;

    // 채팅 UI
    private JTextArea chatArea;
    private JTextField chatInput;
    private JScrollPane chatScroll;

    // 방향 (GameState에 없는 애니메이션용 필드)
    private int myDirection = 0; // 0:Down, 1:Up, 2:Left, 3:Right
    private SpriteAnimation[] myAnimations;

    // 설치된 오브젝트 (지뢰, 터렛)
    final Map<Integer, PlacedObjectClient> placedObjects = new HashMap<>();

    // 에어스트라이크 마커
    final Map<Integer, StrikeMarker> strikeMarkers = new HashMap<>();

    // General 궁극기: 미니맵 타겟팅 대기 상태
    private boolean awaitingMinimapTarget = false;

    // 버프 상태 (gen_aura 등)
    float moveSpeedMultiplier = 1.0f;
    float attackSpeedMultiplier = 1.0f; // TODO: 공격 속도 버프 미구현

    // 라운드 시스템
    public enum RoundState {
        WAITING, PLAYING, ENDED
    }

    RoundState roundState = RoundState.WAITING;
    int roundCount = 0;
    int redWins = 0;
    int blueWins = 0;
    long roundStartTime = 0;
    public static final int ROUND_READY_TIME = 10000; // 10초 대기
    String centerMessage = "";
    long centerMessageEndTime = 0;

    // 캐릭터 선택 제한 관련 변수
    boolean hasChangedCharacterInRound = false;
    private static final long CHARACTER_CHANGE_TIME_LIMIT = 10000; // 10초

    class PlacedObjectClient {
        int id;
        String type; // "tech_mine", "tech_turret"
        int x, y;
        int hp, maxHp;
        String owner;
        int team;

        PlacedObjectClient(int id, String type, int x, int y, int hp, int maxHp, String owner, int team) {
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

    class StrikeMarker {
        int id;
        int x, y;
        long createdAt;

        StrikeMarker(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.createdAt = System.currentTimeMillis();
        }
    }

    class PlayerData {
        int x, y;
        int targetX, targetY; // 보간을 위한 목표 위치
        int team;
        int hp;
        int maxHp;
        String characterId;
        int kills;
        int deaths;
        int direction = 0; // 0:Down, 1:Up, 2:Left, 3:Right
        SpriteAnimation[] animations; // [WalkDown, WalkUp, WalkLeft, WalkRight]

        PlayerData(int x, int y, int team) {
            this.x = x;
            this.y = y;
            this.targetX = x;
            this.targetY = y;
            this.team = team;
            this.hp = GameConstants.MAX_HP;
            this.maxHp = GameConstants.MAX_HP;
            this.kills = 0;
            this.deaths = 0;
        }

        // 부드러운 보간 업데이트
        void smoothUpdate() {
            // 목표 위치로 부드럽게 이동 (보간 계수 0.5 = 50% 이동 - 더 빠른 반응)
            float interpolation = 0.5f;
            x += (int) ((targetX - x) * interpolation);
            y += (int) ((targetY - y) * interpolation);
        }
    }

    class Missile {
        int x, y;
        int dx, dy;
        int team;
        String owner; // 발사자 이름(킬 크레딧/피격 리포트용)

        Missile(int x, int y, int dx, int dy, int team, String owner) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.team = team;
            this.owner = owner;
        }
    }

    class GameCanvas extends JPanel {
        public GameCanvas() {
            // 초기 크기: 1150x800 (채팅 패널 250px 제외)
            // 스케일링으로 1280x720 영역만 보임
            setPreferredSize(new Dimension(1150, 800));
            setBackground(new Color(20, 25, 35));
            setFocusable(true);
            addKeyListener(GamePanel.this);

            // 스프라이트 로드
            GamePanel.this.loadSprites();

            // 마우스 클릭으로 기본 공격 (마우스 방향으로 발사)
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // 스케일 보정: 실제 마우스 좌표를 고정 해상도 좌표로 변환
                    java.awt.Point scaled = scaleMouseCoordinates(e.getX(), e.getY());
                    int scaledMouseX = scaled.x;
                    int scaledMouseY = scaled.y;
                    
                    // 미니맵 타겟팅 모드: General 에어스트라이크
                    if (awaitingMinimapTarget && e.getButton() == MouseEvent.BUTTON1) {
                        // 미니맵 영역 체크 (고정 해상도 기준)
                        int minimapX = GameConstants.GAME_WIDTH - MINIMAP_WIDTH - UI_MARGIN;
                        int minimapY = UI_MARGIN;

                        if (scaledMouseX >= minimapX && scaledMouseX <= minimapX + MINIMAP_WIDTH &&
                                scaledMouseY >= minimapY && scaledMouseY <= minimapY + MINIMAP_HEIGHT) {
                            // 미니맵 좌표를 맵 좌표로 변환
                            float mapScaleX = (float) MINIMAP_WIDTH / mapWidth;
                            float mapScaleY = (float) MINIMAP_HEIGHT / mapHeight;
                            int targetMapX = (int) ((scaledMouseX - minimapX) / mapScaleX);
                            int targetMapY = (int) ((scaledMouseY - minimapY) / mapScaleY);

                            // 에어스트라이크 전송
                            sendSkillUse(2, "ULTIMATE", targetMapX, targetMapY);
                            appendChatMessage("[General] 에어스트라이크 호출! 좌표: (" + targetMapX + "," + targetMapY + ")");
                            awaitingMinimapTarget = false;

                            // 로컬 이펙트
                            if (abilities != null && abilities.length > 2) {
                                addLocalEffect(abilities[2]);
                            }
                            return;
                        }
                    }

                    // 편집 모드: 타일 페인팅
                    if (editMode) {
                        int mapX = scaledMouseX + cameraX;
                        int mapY = scaledMouseY + cameraY;
                        startPaintAt(mapX, mapY);
                        return;
                    }
                    // 게임 모드: 좌클릭 공격
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        int targetMapX = scaledMouseX + cameraX;
                        int targetMapY = scaledMouseY + cameraY;
                        useBasicAttack(targetMapX, targetMapY);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (editMode) {
                        paintState = -1;
                    }
                }
            });

            // 마우스 이동 추적 (조준선용 - 화면 좌표 그대로 사용)
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    // 스케일 보정: 실제 마우스 좌표를 고정 해상도 좌표로 변환
                    java.awt.Point scaled = scaleMouseCoordinates(e.getX(), e.getY());
                    mouseX = scaled.x;
                    mouseY = scaled.y;
                    if (editMode)
                        updateHoverTile(mouseX + cameraX, mouseY + cameraY);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    // 스케일 보정: 실제 마우스 좌표를 고정 해상도 좌표로 변환
                    java.awt.Point scaled = scaleMouseCoordinates(e.getX(), e.getY());
                    mouseX = scaled.x;
                    mouseY = scaled.y;
                    if (editMode) {
                        int mapX = mouseX + cameraX;
                        int mapY = mouseY + cameraY;
                        updateHoverTile(mapX, mapY);
                        continuePaintAt(mapX, mapY);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // RenderContext를 생성하여 GameRenderer에 전달
            GameRenderer.RenderContext ctx = GamePanel.this.createRenderContext();
            gameRenderer.render(g, ctx);
        }
    }

    /**
     * GameRenderer에 전달할 RenderContext 생성
     */
    private GameRenderer.RenderContext createRenderContext() {
        GameRenderer.RenderContext ctx = new GameRenderer.RenderContext();
        
        // 맵 정보
        ctx.mapImage = this.mapImage;
        ctx.mapWidth = this.mapWidth;
        ctx.mapHeight = this.mapHeight;
        ctx.cameraX = this.cameraX;
        ctx.cameraY = this.cameraY;
        ctx.obstacles = this.obstacles;
        ctx.debugObstacles = this.debugObstacles;
        
        // 플레이어 정보
        ctx.playerName = this.playerName;
        ctx.team = this.team;
        ctx.playerX = this.playerX;
        ctx.playerY = this.playerY;
        ctx.myDirection = this.myDirection;
        ctx.myAnimations = this.myAnimations;
        ctx.myHP = gameState.getMyHP();
        ctx.myMaxHP = gameState.getMyMaxHP();
        ctx.mouseX = this.mouseX;
        ctx.mouseY = this.mouseY;
        ctx.selectedCharacter = gameState.getSelectedCharacter();
        ctx.currentCharacterData = gameState.getCurrentCharacterData();
        
        // 게임 오브젝트
        ctx.players = this.players;
        ctx.missiles = this.missiles;
        ctx.placedObjects = this.placedObjects;
        ctx.strikeMarkers = this.strikeMarkers;
        
        // 이펙트
        ctx.myEffects = this.myEffects;
        ctx.skillEffects = this.skillEffects;
        
        // UI 상태
        ctx.showMinimap = this.showMinimap;
        ctx.editMode = this.editMode;
        ctx.kills = gameState.getKills();
        ctx.deaths = gameState.getDeaths();
        ctx.abilities = this.abilities;
        
        // 라운드 정보
        ctx.redWins = this.redWins;
        ctx.blueWins = this.blueWins;
        ctx.roundCount = this.roundCount;
        ctx.roundState = this.roundState;
        ctx.roundStartTime = this.roundStartTime;
        ctx.centerMessage = this.centerMessage;
        ctx.centerMessageEndTime = this.centerMessageEndTime;
        
        // 파이퍼 스킬
        ctx.piperMarkRemaining = this.piperMarkRemaining;
        ctx.piperThermalRemaining = this.piperThermalRemaining;
        ctx.teamMarkRemaining = this.teamMarkRemaining;
        ctx.teamThermalRemaining = this.teamThermalRemaining;
        
        // 캔버스 크기 - 고정 렌더링 크기와 실제 크기 분리
        ctx.canvasWidth = GameConstants.GAME_WIDTH;   // 항상 1280 (고정)
        ctx.canvasHeight = GameConstants.GAME_HEIGHT; // 항상 720 (고정)
        ctx.actualCanvasWidth = canvas != null ? canvas.getWidth() : GameConstants.GAME_WIDTH;
        ctx.actualCanvasHeight = canvas != null ? canvas.getHeight() : GameConstants.GAME_HEIGHT;
        
        // 에디터
        ctx.walkableGrid = this.walkableGrid;
        ctx.tileSize = TILE_SIZE;
        ctx.gridCols = this.gridCols;
        ctx.gridRows = this.gridRows;
        
        return ctx;
    }

    public GamePanel(String playerName, int team, Socket socket, DataOutputStream out, DataInputStream in,
            String characterId) {
        super("FPS Game - " + playerName);
        
        // Backward compatibility
        this.playerName = playerName;
        this.team = team;
        
        // GameState 초기화
        this.gameState = new GameState(playerName, team);
        
        this.socket = socket;
        this.out = out;
        this.in = in;
        
        // NetworkClient 초기화
        this.networkClient = new NetworkClient(socket, out, in);
        this.networkClient.setOnMessageReceived(this::processGameMessage);
        
        // GameRenderer 초기화 - 파라미터 없는 생성자 사용
        this.gameRenderer = new GameRenderer();
        
        // GameMessageHandler 초기화
        this.messageHandler = new GameMessageHandler(this);

        // 전달받은 캐릭터 ID 사용 (null이면 기본값)
        String selectedChar = (characterId != null && !characterId.isEmpty()) ? characterId : "raven";
        CharacterData charData = CharacterData.getById(selectedChar);
        
        // GameState에 캐릭터 정보 설정
        gameState.setSelectedCharacter(selectedChar);
        gameState.setCurrentCharacterData(charData);

        // HP 초기화 (중요: 캐릭터별 MaxHP 적용)
        int maxHp = (int) charData.health;
        gameState.setMyMaxHP(maxHp);
        gameState.setMyHP(maxHp);

        // 스킬 초기화 (로컬 유지 - 매 프레임 update)
        this.abilities = CharacterData.createAbilities(selectedChar);
        gameState.setAbilities(this.abilities);

        // 기본 맵 로드 (서버 ROUND_START에서 다른 맵으로 변경 가능)
        loadMap(currentMapName);

        // 플레이어 초기 위치 (팀별 스폰 지역)
        setInitialSpawnPosition();

        initUI();

        // 게임 시작 시 라운드 초기화
        startRound();
        startGame();
    }

    /**
     * 팀별 초기 스폰 위치 설정
     * RED 팀 = 왼쪽 상단 (빨강 스폰 지역)
     * BLUE 팀 = 오른쪽 하단 (파랑 스폰 지역)
     */
    private void setInitialSpawnPosition() {
        java.util.List<int[]> tiles = (team == GameConstants.TEAM_RED ? redSpawnTiles : blueSpawnTiles);
        if (tiles != null && !tiles.isEmpty()) {
            java.util.Random rand = new java.util.Random();
            int[] t = tiles.get(rand.nextInt(tiles.size()));
            playerX = t[0] * TILE_SIZE + TILE_SIZE / 2;
            playerY = t[1] * TILE_SIZE + TILE_SIZE / 2;
        } else {
            appendChatMessage("[경고] 팀 스폰 타일이 비어 있습니다. 맵 JSON의 spawns." +
                    (team == GameConstants.TEAM_RED ? "red" : "blue") + ".tiles를 지정하세요.");
            // 임시 폴백: 맵 중앙
            playerX = mapWidth / 2;
            playerY = mapHeight / 2;
        }
        appendChatMessage("[스폰] " + (team == GameConstants.TEAM_RED ? "RED" : "BLUE") +
                " 팀 스폰 위치: (" + playerX + ", " + playerY + ")");
    }

    private void initUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true); // 리사이징 허용

        // 메인 패널 레이아웃 설정
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 게임 캔버스 (왼쪽)
        canvas = new GameCanvas();
        mainPanel.add(canvas, BorderLayout.CENTER);

        // 채팅 패널 (오른쪽)
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setPreferredSize(new Dimension(CHAT_PANEL_WIDTH, 800)); // 창 높이에 맞춤
        chatPanel.setBackground(new Color(32, 34, 37));

        // 채팅 영역
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        chatArea.setBackground(new Color(47, 49, 54));
        chatArea.setForeground(Color.WHITE);

        chatScroll = new JScrollPane(chatArea);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        // 채팅 입력 필드
        chatInput = new JTextField();
        chatInput.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        chatInput.setBackground(new Color(64, 68, 75));
        chatInput.setForeground(Color.WHITE);
        chatInput.setCaretColor(Color.WHITE);
        chatInput.addActionListener(e -> sendChatMessage());

        // 채팅 입력창에서 ESC 누르면 게임으로 포커스 복귀
        chatInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    canvas.requestFocusInWindow();
                }
            }
        });

        chatPanel.add(chatInput, BorderLayout.SOUTH);

        mainPanel.add(chatPanel, BorderLayout.EAST);

        setContentPane(mainPanel);

        // 메뉴바 추가
        createMenuBar();

        // 프레임 크기 자동 조정 (메뉴바 포함)
        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                // 시작 시 즉시 게임 캔버스에 포커스 줘서 조작 가능하게
                SwingUtilities.invokeLater(() -> canvas.requestFocusInWindow());
            }

            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (out != null && socket != null && !socket.isClosed()) {
                        out.writeUTF("QUIT");
                        out.flush();
                    }
                } catch (IOException ex) {
                    // 소켓이 이미 닫혔을 수 있음 - 무시
                }
                disconnect();
            }
        });
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(230, 230, 230)); // 밝은 회색 배경

        JMenu optionsMenu = new JMenu("옵션");
        optionsMenu.setForeground(Color.BLACK); // 검정 텍스트
        optionsMenu.setFont(new Font("맑은 고딕", Font.PLAIN, 14));

        JMenuItem settingsItem = new JMenuItem("설정");
        settingsItem.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        settingsItem.setForeground(Color.BLACK); // 검정 텍스트
        settingsItem.addActionListener(e -> {
            // 게임 일시정지
            if (timer != null)
                timer.stop();

            OptionDialog dialog = new OptionDialog(this);
            dialog.setVisible(true);

            // 게임 재개
            if (timer != null)
                timer.start();
            canvas.requestFocusInWindow();
        });

        optionsMenu.add(settingsItem);
        menuBar.add(optionsMenu);

        setJMenuBar(menuBar);
    }

    private void sendChatMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty() && out != null) {
            networkClient.sendChat(message);
            chatInput.setText("");
        }
        // 채팅 전송 후 게임 캔버스로 포커스 복귀
        canvas.requestFocusInWindow();
    }

    // 채팅/시스템 로그 스로틀링
    private String lastChatMessage = null;
    private long lastChatTime = 0L;
    private static final long CHAT_THROTTLE_MS = 1000;

    void appendChatMessage(String message) {
        long now = System.currentTimeMillis();
        if (message != null && message.equals(lastChatMessage) && (now - lastChatTime) < CHAT_THROTTLE_MS) {
            return; // 동일 메시지 연속 출력 최소화
        }
        lastChatMessage = message;
        lastChatTime = now;
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    /**
     * 맵 로드 및 장애물 설정
     */
    void loadMap(String mapName) {
        try {
            // 맵 이미지 로드 (assets/maps/ 경로)
            java.io.File mapFile = new java.io.File("assets/maps/" + mapName + ".png");
            if (mapFile.exists()) {
                mapImage = javax.imageio.ImageIO.read(mapFile);
                if (mapImage != null) {
                    // 맵 이미지가 있으면 그 크기를 사용
                    mapWidth = mapImage.getWidth();
                    mapHeight = mapImage.getHeight();
                    appendChatMessage("[시스템] 맵 로드 완료: " + mapName + " (" + mapWidth + "x" + mapHeight + ")");
                } else {
                    appendChatMessage("[시스템] 맵 이미지 읽기 실패, 기본 크기 사용");
                }
            } else {
                appendChatMessage("[시스템] 맵 파일 없음: " + mapFile.getAbsolutePath());
            }
        } catch (java.io.IOException e) {
            appendChatMessage("[시스템] 맵 로드 에러: " + e.getMessage());
            // 폴백: 기본 크기 유지
        }
        // 그리드 크기 초기화
        gridCols = Math.max(1, mapWidth / TILE_SIZE);
        gridRows = Math.max(1, mapHeight / TILE_SIZE);
        walkableGrid = new boolean[gridRows][gridCols];

        // 1) JSON 로딩 시도 (assets/maps/<mapName>_edited.json → .edited.json → .json →
        // map_*.json)
        boolean loadedFromJson = loadMapFromJsonIfAvailable(mapName);

        // 2) JSON 없으면 이미지 분석으로 장애물 추출
        if (!loadedFromJson) {
            setupObstacles(mapName);
        }

        // 3) 스폰 구역은 항상 walkable 보장 + 겹치는 장애물 제거
        ensureSpawnZonesWalkable();

        // 4) JSON에 스폰 구역이 정의되지 않은 경우 에러 처리
        if (redSpawnZone == null || blueSpawnZone == null) {
            appendChatMessage("[경고] 스폰 구역이 JSON에 정의되지 않았습니다. 게임 시작 불가!");
        }
    }

    /**
     * 맵별 장애물 설정 (이미지 픽셀 분석 기반 자동 추출)
     */
    private void setupObstacles(String mapName) {
        obstacles.clear();

        appendChatMessage("[디버그] 맵 크기: " + mapWidth + "x" + mapHeight);

        // map.png의 경우 픽셀 분석으로 장애물 자동 추출하고, 그리드 갱신
        if ("map".equals(mapName) && mapImage != null) {
            extractObstaclesFromImage();
            // 이미지 기반일 때: 기본적으로 길이 아닌 곳은 장애물 -> walkableGrid 초기화
            for (int r = 0; r < gridRows; r++) {
                for (int c = 0; c < gridCols; c++) {
                    int cx = c * TILE_SIZE + TILE_SIZE / 2;
                    int cy = r * TILE_SIZE + TILE_SIZE / 2;
                    if (cx < mapWidth && cy < mapHeight) {
                        Color color = new Color(mapImage.getRGB(cx, cy));
                        walkableGrid[r][c] = isRoadColor(color) || isSpawnAreaColor(color);
                    }
                }
            }
        } else if ("terminal".equals(mapName)) {
            // 기존 터미널 맵 (수동 설정)
            int centerX = mapWidth / 2;
            int centerY = mapHeight / 2;
            obstacles.add(new Rectangle(centerX - 300, centerY - 200, 600, 400));

            int leftX = (int) (mapWidth * 0.15);
            obstacles.add(new Rectangle(leftX, (int) (mapHeight * 0.25), 150, 150));
            obstacles.add(new Rectangle(leftX, (int) (mapHeight * 0.42), 150, 150));
            obstacles.add(new Rectangle(leftX, (int) (mapHeight * 0.59), 150, 150));

            int rightX = (int) (mapWidth * 0.85) - 150;
            obstacles.add(new Rectangle(rightX, (int) (mapHeight * 0.25), 150, 150));
            obstacles.add(new Rectangle(rightX, (int) (mapHeight * 0.42), 150, 150));
            obstacles.add(new Rectangle(rightX, (int) (mapHeight * 0.59), 150, 150));

            obstacles.add(new Rectangle(centerX - 200, (int) (mapHeight * 0.1), 120, 120));
            obstacles.add(new Rectangle(centerX + 80, (int) (mapHeight * 0.1), 120, 120));
            obstacles.add(new Rectangle(centerX - 200, (int) (mapHeight * 0.9) - 120, 120, 120));
            obstacles.add(new Rectangle(centerX + 80, (int) (mapHeight * 0.9) - 120, 120, 120));
        }

        appendChatMessage("[디버그] 장애물 " + obstacles.size() + "개 설정 완료");
    }

    /**
     * assets/maps 디렉토리에서 해당 맵 이름의 json 파일만 로딩한다.
     * 우선순위: <name>_edited.json → <name>.edited.json → <name>.json
     */
    private boolean loadMapFromJsonIfAvailable(String mapName) {
        File dir = new File("assets/maps");
        if (!dir.exists())
            return false;

        // 우선순위: <name>_edited.json → <name>.edited.json → <name>.json (타 맵 fallback 금지)
        File editedUnderscore = new File(dir, mapName + "_edited.json");
        File editedDot = new File(dir, mapName + ".edited.json");
        File primary = new File(dir, mapName + ".json");
        File target = editedUnderscore.exists() ? editedUnderscore
                : (editedDot.exists() ? editedDot : (primary.exists() ? primary : null));
        if (target == null || !target.exists())
            return false;

        try {
            String json = new String(java.nio.file.Files.readAllBytes(target.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
            parseMapJson(json);
            appendChatMessage("[맵 데이터] JSON 로드: " + target.getName());
            return true;
        } catch (IOException e) {
            appendChatMessage("[맵 데이터] JSON 로드 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 맵 전환: 이름을 바꾸고 리소스를 다시 로드, 스폰 재배치 및 카메라 갱신
     */
    private void switchMap(String newMapName) {
        this.currentMapName = newMapName;
        appendChatMessage("[시스템] 맵 전환: " + newMapName);
        loadMap(newMapName);
        setInitialSpawnPosition();
        updateCamera();
        missiles.clear();
        sendPosition();
    }

    /**
     * assets/maps 아래에서 사용 가능한 맵 이름 목록을 재구성한다.
     * 우선순위: _edited.json / .edited.json / .json 중 하나라도 있으면 해당 baseName을 포함.
     * 기본으로 "map", "airport"는 항상 선두에 배치(중복 제거됨).
     */
    private void rebuildMapCycle() {
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        // 기본 선호 순서
        names.add("map");
        names.add("map2");
        names.add("map3");
        names.add("village");

        File dir = new File("assets" + File.separator + "maps");
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles(
                    (d, n) -> n.endsWith("_edited.json") || n.endsWith(".edited.json") || n.endsWith(".json"));
            if (files != null) {
                // 사전 정렬하여 일관된 순서
                java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName));
                for (File f : files) {
                    String n = f.getName();
                    String base;
                    if (n.endsWith("_edited.json"))
                        base = n.substring(0, n.length() - "_edited.json".length());
                    else if (n.endsWith(".edited.json"))
                        base = n.substring(0, n.length() - ".edited.json".length());
                    else
                        base = n.substring(0, n.length() - ".json".length());
                    if (!base.isEmpty())
                        names.add(base);
                }
            }
        }
        mapCycle = new ArrayList<>(names);
        int idx = mapCycle.indexOf(currentMapName);
        mapIndex = (idx >= 0 ? idx : 0);
        appendChatMessage("[시스템] 맵 목록: " + String.join(", ", mapCycle));
    }

    /**
     * 매우 단순한 파서로 JSON에서 사각형 목록과 맵 메타데이터를 추출한다.
     * 새 형식: meta.map_pixel_size.w/h, meta.tile_size, obstacles:[{x,y}],
     * spawns.red/blue
     */
    private void parseMapJson(String json) {
        if (json == null)
            return;

        // 메타데이터 (새 형식 meta.map_pixel_size.w/h, meta.tile_size 지원)
        Integer mw = null, mh = null, ts = null;

        // map_pixel_size.w 찾기
        java.util.regex.Matcher mwMatch = java.util.regex.Pattern
                .compile("\"map_pixel_size\"\\s*:\\s*\\{[^}]*\"w\"\\s*:\\s*(\\d+)").matcher(json);
        if (mwMatch.find())
            mw = Integer.parseInt(mwMatch.group(1));

        // map_pixel_size.h 찾기
        java.util.regex.Matcher mhMatch = java.util.regex.Pattern
                .compile("\"map_pixel_size\"\\s*:\\s*\\{[^}]*\"h\"\\s*:\\s*(\\d+)").matcher(json);
        if (mhMatch.find())
            mh = Integer.parseInt(mhMatch.group(1));

        // tile_size 찾기
        java.util.regex.Matcher tsMatch = java.util.regex.Pattern.compile("\"tile_size\"\\s*:\\s*(\\d+)").matcher(json);
        if (tsMatch.find())
            ts = Integer.parseInt(tsMatch.group(1));

        if (mw != null)
            mapWidth = mw;
        if (mh != null)
            mapHeight = mh;
        if (ts != null && ts > 0) {
            gridCols = Math.max(1, mapWidth / ts);
            gridRows = Math.max(1, mapHeight / ts);
            walkableGrid = new boolean[gridRows][gridCols];
        }

        // 새 형식: roads는 이동 가능한 타일 좌표 {x, y} 배열 → 기본 모두 false, roads만 true
        // 하위 호환: obstacles가 있으면 obstacles 방식 사용, 없으면 roads 방식 사용
        java.util.List<int[]> roadTiles = extractTileList(json, "roads");
        java.util.List<int[]> obstacleTiles = extractTileList(json, "obstacles");

        obstacles.clear();

        if (!roadTiles.isEmpty()) {
            // roads 방식: 모든 타일 기본 false(장애물) → roads만 true(이동 가능)
            for (int r = 0; r < gridRows; r++) {
                for (int c = 0; c < gridCols; c++)
                    walkableGrid[r][c] = false;
            }

            for (int[] tile : roadTiles) {
                int col = tile[0];
                int row = tile[1];
                if (row >= 0 && row < gridRows && col >= 0 && col < gridCols) {
                    walkableGrid[row][col] = true;
                }
            }

            // 시각화를 위해 walkable하지 않은 타일을 obstacles에 추가
            for (int r = 0; r < gridRows; r++) {
                for (int c = 0; c < gridCols; c++) {
                    if (!walkableGrid[r][c]) {
                        obstacles.add(new Rectangle(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE));
                    }
                }
            }
        } else if (!obstacleTiles.isEmpty()) {
            // 기존 obstacles 방식: 모든 타일 기본 true(이동 가능) → obstacles만 false(장애물)
            for (int r = 0; r < gridRows; r++) {
                for (int c = 0; c < gridCols; c++)
                    walkableGrid[r][c] = true;
            }

            for (int[] tile : obstacleTiles) {
                int col = tile[0];
                int row = tile[1];
                if (row >= 0 && row < gridRows && col >= 0 && col < gridCols) {
                    walkableGrid[row][col] = false;
                    obstacles.add(new Rectangle(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE));
                }
            }
        }

        // 스폰 구역 (spawns.red, spawns.blue의 tiles 배열)
        // 맵 전환 시 이전 맵의 스폰 타일이 남지 않도록 먼저 초기화
        redSpawnTiles.clear();
        blueSpawnTiles.clear();
        redSpawnZone = extractSpawnZone(json, "red", redSpawnTiles);
        blueSpawnZone = extractSpawnZone(json, "blue", blueSpawnTiles);

        // 스폰 구역은 항상 walkable로 강제
        ensureSpawnZonesWalkable();

        int walkableCount = 0;
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                if (walkableGrid[r][c])
                    walkableCount++;
            }
        }

        String mapType = !roadTiles.isEmpty() ? "roads" : "obstacles";
        appendChatMessage("[맵 JSON] " + mapType + " 방식, 이동 가능 타일 " + walkableCount + "개, RED 스폰 " +
                (redSpawnZone != null ? "설정" : "없음") + ", BLUE 스폰 " +
                (blueSpawnZone != null ? "설정" : "없음"));
    }

    /**
     * JSON에서 타일 좌표 배열 {x, y} 추출
     */
    private java.util.List<int[]> extractTileList(String json, String key) {
        java.util.List<int[]> list = new ArrayList<>();
        java.util.regex.Pattern section = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*\\[(.*?)\\]",
                java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher sec = section.matcher(json);
        if (!sec.find())
            return list;
        String body = sec.group(1);

        // {x: 숫자, y: 숫자} 형식 파싱
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"x\"\\s*:\\s*(\\d+).*?\"y\"\\s*:\\s*(\\d+)", java.util.regex.Pattern.DOTALL).matcher(body);
        while (m.find()) {
            int x = Integer.parseInt(m.group(1));
            int y = Integer.parseInt(m.group(2));
            list.add(new int[] { x, y });
        }
        return list;
    }

    /**
     * spawns.red/blue의 tiles 배열에서 스폰 구역(Rectangle) 생성
     */
    private Rectangle extractSpawnZone(String json, String teamKey, java.util.List<int[]> tileStore) {
        // spawns -> teamKey -> tiles 배열을 비균형 중괄호 환경에서도 안전하게 비탐욕으로 추출
        java.util.regex.Pattern path = java.util.regex.Pattern.compile(
                "\\\"spawns\\\"\\s*:\\s*\\{[\\s\\S]*?\\\"" + teamKey
                        + "\\\"\\s*:\\s*\\{[\\s\\S]*?\\\"tiles\\\"\\s*:\\s*\\[(.*?)\\]",
                java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = path.matcher(json);
        if (!m.find())
            return null;
        String tilesBody = m.group(1);

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        java.util.regex.Matcher tileCoords = java.util.regex.Pattern
                .compile("\\\"x\\\"\\s*:\\s*(\\d+).*?\\\"y\\\"\\s*:\\s*(\\d+)", java.util.regex.Pattern.DOTALL)
                .matcher(tilesBody);
        while (tileCoords.find()) {
            int x = Integer.parseInt(tileCoords.group(1));
            int y = Integer.parseInt(tileCoords.group(2));
            tileStore.add(new int[] { x, y });
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        if (minX == Integer.MAX_VALUE)
            return null;
        return new Rectangle(minX * TILE_SIZE, minY * TILE_SIZE, (maxX - minX + 1) * TILE_SIZE,
                (maxY - minY + 1) * TILE_SIZE);
    }

    /*
     * [DEPRECATED] walkableGrid를 기반으로 좌상단/우하단 스폰 구역 자동 탐지
     * 현재는 JSON에서만 스폰 구역을 로드합니다.
     */
    /*
     * private void autoDetectSpawnZones() {
     * int wantTiles = 6; // 6x6 타일
     * redSpawnZone = findSpawnZoneNear(0, 0, wantTiles);
     * blueSpawnZone = findSpawnZoneNear(gridCols - 1, gridRows - 1, wantTiles);
     * appendChatMessage("[스폰] 자동 탐지 완료");
     * }
     * 
     * private Rectangle findSpawnZoneNear(int startCol, int startRow, int
     * sizeTiles) {
     * int maxRadius = Math.max(gridCols, gridRows);
     * for (int rad = 0; rad < maxRadius; rad++) {
     * int c0 = Math.max(0, startCol - rad);
     * int r0 = Math.max(0, startRow - rad);
     * int c1 = Math.min(gridCols - sizeTiles, startCol + rad);
     * int r1 = Math.min(gridRows - sizeTiles, startRow + rad);
     * for (int r = r0; r <= r1; r++) {
     * for (int c = c0; c <= c1; c++) {
     * if (isAllWalkable(c, r, sizeTiles, sizeTiles)) {
     * return new Rectangle(c * TILE_SIZE, r * TILE_SIZE, sizeTiles * TILE_SIZE,
     * sizeTiles * TILE_SIZE);
     * }
     * }
     * }
     * }
     * return new Rectangle(startCol * TILE_SIZE, startRow * TILE_SIZE, sizeTiles *
     * TILE_SIZE, sizeTiles * TILE_SIZE);
     * }
     * 
     * private boolean isAllWalkable(int col, int row, int wTiles, int hTiles) {
     * for (int r = row; r < row + hTiles && r < gridRows; r++) {
     * for (int c = col; c < col + wTiles && c < gridCols; c++) {
     * if (!walkableGrid[r][c]) return false;
     * }
     * }
     * return true;
     * }
     */

    /**
     * 스폰 구역을 walkableGrid에 강제로 반영하고, 해당 영역과 겹치는 장애물은 제거
     */
    private void ensureSpawnZonesWalkable() {
        if (walkableGrid == null)
            return;
        if (redSpawnZone != null)
            markZoneWalkableAndClearObstacles(redSpawnZone);
        if (blueSpawnZone != null)
            markZoneWalkableAndClearObstacles(blueSpawnZone);
    }

    private void markZoneWalkableAndClearObstacles(Rectangle zone) {
        // walkableGrid true
        int c0 = Math.max(0, zone.x / TILE_SIZE);
        int r0 = Math.max(0, zone.y / TILE_SIZE);
        int c1 = Math.min(gridCols - 1, (zone.x + zone.width - 1) / TILE_SIZE);
        int r1 = Math.min(gridRows - 1, (zone.y + zone.height - 1) / TILE_SIZE);
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++)
                walkableGrid[r][c] = true;
        }
        // 겹치는 장애물 제거
        if (obstacles != null && !obstacles.isEmpty()) {
            obstacles.removeIf(o -> o.intersects(zone));
        }
        // 스폰 타일 목록이 있으면 그것도 walkable (보다 정확한 경계 유지)
        java.util.List<int[]> tiles = (zone == redSpawnZone ? redSpawnTiles
                : (zone == blueSpawnZone ? blueSpawnTiles : null));
        if (tiles != null) {
            for (int[] t : tiles) {
                int col = t[0];
                int row = t[1];
                if (row >= 0 && row < gridRows && col >= 0 && col < gridCols) {
                    walkableGrid[row][col] = true;
                }
            }
        }
    }

    // ==================== 유틸리티 메서드 ====================
    
    /**
     * 화면 좌표를 스케일 보정된 게임 좌표로 변환
     */
    private java.awt.Point scaleMouseCoordinates(int screenX, int screenY) {
        double scaleX = (double) getWidth() / GameConstants.GAME_WIDTH;
        double scaleY = (double) getHeight() / GameConstants.GAME_HEIGHT;
        return new java.awt.Point(
            (int) (screenX / scaleX),
            (int) (screenY / scaleY)
        );
    }
    
    /**
     * 맵 이미지 픽셀 분석으로 장애물 자동 추출
     * - 밝은 회색(길) + 스폰 지역만 이동 가능
     * - 나머지는 모두 장애물 (벽, 나무, 잔디 등)
     */
    private void extractObstaclesFromImage() {
        if (mapImage == null)
            return;

        int tileSize = 32; // 타일 크기 (맵 타일 크기와 동일)
        int cols = mapWidth / tileSize;
        int rows = mapHeight / tileSize;

        // 장애물 타일 감지용 그리드 (이동 불가능 영역)
        boolean[][] obstacleGrid = new boolean[rows][cols];

        // 픽셀 샘플링으로 이동 가능/불가능 영역 감지
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int centerX = col * tileSize + tileSize / 2;
                int centerY = row * tileSize + tileSize / 2;

                // 이미지 범위 체크
                if (centerX >= mapImage.getWidth() || centerY >= mapImage.getHeight()) {
                    obstacleGrid[row][col] = true; // 범위 밖은 장애물
                    continue;
                }

                int rgb = mapImage.getRGB(centerX, centerY);
                Color color = new Color(rgb);

                // 이동 가능한 영역 체크 (밝은 회색 길 + 스폰 지역)
                boolean isWalkable = isRoadColor(color) || isSpawnAreaColor(color);

                // 이동 불가능하면 장애물로 표시
                if (!isWalkable) {
                    obstacleGrid[row][col] = true;
                }
            }
        }

        // 연속된 장애물 타일을 그룹핑하여 큰 Rectangle로 병합
        boolean[][] visited = new boolean[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (obstacleGrid[row][col] && !visited[row][col]) {
                    // 새로운 장애물 영역 발견 - 확장 가능한 사각형 찾기
                    Rectangle rect = findMaxRectangle(obstacleGrid, visited, row, col, rows, cols);
                    if (rect.width > 0 && rect.height > 0) {
                        // 타일 좌표를 픽셀 좌표로 변환
                        obstacles.add(new Rectangle(
                                rect.x * tileSize,
                                rect.y * tileSize,
                                rect.width * tileSize,
                                rect.height * tileSize));
                    }
                }
            }
        }

        appendChatMessage("[맵 분석] 장애물 " + obstacles.size() + "개 자동 추출 완료");
    }

    /**
     * 밝은 회색 길 색상 판단
     */
    private boolean isRoadColor(Color c) {
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();

        // 밝은 회색 (RGB 값이 비슷하고 높은 값)
        // 회색 계열: R, G, B 차이가 작고, 밝기가 높음
        int avg = (r + g + b) / 3;
        int maxDiff = Math.max(Math.abs(r - g), Math.max(Math.abs(g - b), Math.abs(r - b)));

        // 밝은 회색: 평균 밝기 140~200, RGB 차이 30 이하
        return (avg >= 140 && avg <= 200) && (maxDiff <= 30);
    }

    /**
     * 스폰 지역 색상 판단 (왼쪽 상단 = 빨강 계열, 오른쪽 하단 = 파랑 계열)
     */
    private boolean isSpawnAreaColor(Color c) {
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();

        // 빨강 계열 스폰 (RED 팀 - 왼쪽 상단)
        // 빨강이 강하고, 녹색/파랑보다 높음
        boolean isRedSpawn = (r > 150) && (r > g + 30) && (r > b + 30);

        // 파랑 계열 스폰 (BLUE 팀 - 오른쪽 하단)
        // 파랑이 강하고, 빨강/녹색보다 높음
        boolean isBlueSpawn = (b > 150) && (b > r + 30) && (b > g + 30);

        return isRedSpawn || isBlueSpawn;
    }

    /**
     * 그리드에서 최대 사각형 영역 찾기 (Greedy 확장)
     */
    private Rectangle findMaxRectangle(boolean[][] grid, boolean[][] visited,
            int startRow, int startCol, int rows, int cols) {
        // 우측으로 확장 가능한 최대 너비 찾기
        int maxWidth = 0;
        for (int c = startCol; c < cols && grid[startRow][c] && !visited[startRow][c]; c++) {
            maxWidth++;
        }

        // 아래로 확장 가능한 최대 높이 찾기 (너비 유지)
        int maxHeight = 0;
        for (int r = startRow; r < rows; r++) {
            boolean canExpand = true;
            for (int c = startCol; c < startCol + maxWidth; c++) {
                if (!grid[r][c] || visited[r][c]) {
                    canExpand = false;
                    break;
                }
            }
            if (canExpand) {
                maxHeight++;
            } else {
                break;
            }
        }

        // 방문 표시
        for (int r = startRow; r < startRow + maxHeight; r++) {
            for (int c = startCol; c < startCol + maxWidth; c++) {
                visited[r][c] = true;
            }
        }

        return new Rectangle(startCol, startRow, maxWidth, maxHeight);
    }

    private void startGame() {
        // 게임 업데이트 타이머 (60 FPS)
        timer = new javax.swing.Timer(16, e -> {
            updateGame();
            canvas.repaint();
        });
        timer.start();

        // 서버 메시지 수신 스레드 (NetworkClient로 위임)
        networkClient.startReceiving();
    }

    private void updateGame() {
        if (roundState == RoundState.WAITING) {
            long elapsed = System.currentTimeMillis() - roundStartTime;
            if (elapsed >= ROUND_READY_TIME) {
                roundState = RoundState.PLAYING;
                centerMessage = "Round Start!";
                centerMessageEndTime = System.currentTimeMillis() + 2000;
            }
        }

        updatePlayerPosition();
        updateMissiles();
        checkCollisions();
        updateAbilities(); // 스킬 쿨타임 업데이트
        updateEffects(); // 이펙트 타이머 업데이트
        // 구조화된 SkillEffect 수명 업데이트 (효과 클래스 기반)
        skillEffects.update(0.016f);
        updateRavenRuntime(); // Raven 버프/대쉬 처리
        updatePiperRuntime(); // Piper 마킹/열감지 처리
        updateTeamPiperRuntime(); // 원격 Piper 팀 버프 처리
        updateMyAnimation(); // 스프라이트 애니메이션 업데이트

        // 모든 다른 플레이어의 위치를 부드럽게 보간
        for (PlayerData pd : players.values()) {
            pd.smoothUpdate();
        }
    }

    /**
     * 모든 스킬의 쿨타임 업데이트 (매 프레임)
     */
    private void updateAbilities() {
        if (abilities != null) {
            float deltaTime = 0.016f; // 16ms = 60 FPS
            for (Ability ability : abilities) {
                ability.update(deltaTime);
            }
        }
    }

    /**
     * 이펙트 타이머 업데이트 및 만료 제거
     */
    private void updateEffects() {
        float dt = 0.016f;
        // 내 이펙트
        if (!myEffects.isEmpty()) {
            for (Iterator<ActiveEffect> it = myEffects.iterator(); it.hasNext();) {
                ActiveEffect ef = it.next();
                ef.remaining -= dt;
                if (ef.remaining <= 0)
                    it.remove();
            }
        }
        // 다른 플레이어 이펙트
        if (!effectsByPlayer.isEmpty()) {
            for (java.util.List<ActiveEffect> list : effectsByPlayer.values()) {
                for (Iterator<ActiveEffect> it = list.iterator(); it.hasNext();) {
                    ActiveEffect ef = it.next();
                    ef.remaining -= dt;
                    if (ef.remaining <= 0)
                        it.remove();
                }
            }
        }
    }

    /**
     * Raven 전용 런타임 처리: 대쉬 이동, 과충전 만료 처리
     */
    private void updateRavenRuntime() {
        // 대쉬: 남은 시간 동안 추가 이동 (입력 방향 기준, 없으면 위쪽)
        if (ravenDashRemaining > 0f) {
            ravenDashRemaining -= 0.016f;
            int vx = 0, vy = 0;
            if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP])
                vy -= 1;
            if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN])
                vy += 1;
            if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT])
                vx -= 1;
            if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT])
                vx += 1;
            if (vx == 0 && vy == 0) {
                vy = -1;
            } // 기본 전방(위쪽)
            double len = Math.sqrt(vx * vx + vy * vy);
            if (len > 0) {
                double nx = vx / len, ny = vy / len;
                int dashPixelsPerTick = 12; // 한 프레임 대쉬량
                // 대쉬 시엔 벽을 절대 통과하지 않도록 1px 단위로 전진하며 체크 (슬라이딩 없음)
                double fx = playerX;
                double fy = playerY;
                for (int i = 0; i < dashPixelsPerTick; i++) {
                    fx += nx;
                    fy += ny;
                    int targetX = (int) Math.round(fx);
                    int targetY = (int) Math.round(fy);
                    targetX = Math.max(20, Math.min(targetX, mapWidth - 20));
                    targetY = Math.max(20, Math.min(targetY, mapHeight - 20));
                    if (checkCollisionWithObstacles(targetX, targetY)) {
                        // 벽에 닿는 순간 대쉬 종료 (관통 금지)
                        ravenDashRemaining = 0f;
                        break;
                    }
                    playerX = targetX;
                    playerY = targetY;
                }
            }
        }

        // 과충전: 만료 시 멀티플라이어 원복
        if (ravenOverchargeRemaining > 0f) {
            ravenOverchargeRemaining -= 0.016f;
            if (ravenOverchargeRemaining <= 0f) {
                missileSpeedMultiplier = 1f;
                // 기본 공격 쿨다운 배수 원복
                if (abilities != null && abilities.length > 0) {
                    abilities[0].setCooldownMultiplier(1f);
                }
            }
        }
    }

    /**
     * Piper 전용 런타임 처리 (마킹 및 열감지 지속시간 감소)
     */
    private void updatePiperRuntime() {
        if (piperMarkRemaining > 0f) {
            piperMarkRemaining -= 0.016f;
            if (piperMarkRemaining < 0f)
                piperMarkRemaining = 0f;
        }
        if (piperThermalRemaining > 0f) {
            piperThermalRemaining -= 0.016f;
            if (piperThermalRemaining < 0f)
                piperThermalRemaining = 0f;
        }
    }

    private void updateTeamPiperRuntime() {
        if (teamMarkRemaining > 0f) {
            teamMarkRemaining -= 0.016f;
            if (teamMarkRemaining < 0f)
                teamMarkRemaining = 0f;
        }
        if (teamThermalRemaining > 0f) {
            teamThermalRemaining -= 0.016f;
            if (teamThermalRemaining < 0f)
                teamThermalRemaining = 0f;
        }
    }

    private void updatePlayerPosition() {
        int oldX = playerX;
        int oldY = playerY;
        int newX = playerX;
        int newY = playerY;

        // 버프 적용된 이동 속도 계산
        int effectiveSpeed = (int) (SPEED * moveSpeedMultiplier);

        // 사용자 설정 키 바인딩 사용
        if (keys[KeyBindingConfig.getKey(KeyBindingConfig.KEY_MOVE_FORWARD)] || keys[KeyEvent.VK_UP]) {
            newY -= effectiveSpeed;
        }
        if (keys[KeyBindingConfig.getKey(KeyBindingConfig.KEY_MOVE_BACKWARD)] || keys[KeyEvent.VK_DOWN]) {
            newY += effectiveSpeed;
        }
        if (keys[KeyBindingConfig.getKey(KeyBindingConfig.KEY_MOVE_LEFT)] || keys[KeyEvent.VK_LEFT]) {
            newX -= effectiveSpeed;
        }
        if (keys[KeyBindingConfig.getKey(KeyBindingConfig.KEY_MOVE_RIGHT)] || keys[KeyEvent.VK_RIGHT]) {
            newX += effectiveSpeed;
        }

        // 라운드 대기 상태일 때 스폰 구역 이탈 방지
        if (roundState == RoundState.WAITING) {
            Rectangle spawnZone = (team == GameConstants.TEAM_RED) ? redSpawnZone : blueSpawnZone;
            if (spawnZone != null) {
                // 플레이어 중심점 기준 체크 및 클램핑
                // 플레이어 크기 고려 (반경 20)
                int minX = spawnZone.x + 20;
                int maxX = spawnZone.x + spawnZone.width - 20;
                int minY = spawnZone.y + 20;
                int maxY = spawnZone.y + spawnZone.height - 20;

                // 스폰 구역이 너무 작을 경우 예외 처리
                if (minX > maxX) {
                    minX = spawnZone.x + spawnZone.width / 2;
                    maxX = minX;
                }
                if (minY > maxY) {
                    minY = spawnZone.y + spawnZone.height / 2;
                    maxY = minY;
                }

                newX = Math.max(minX, Math.min(newX, maxX));
                newY = Math.max(minY, Math.min(newY, maxY));
            }
        }

        // 맵 경계 체크 (큰 맵)
        newX = Math.max(20, Math.min(newX, mapWidth - 20));
        newY = Math.max(20, Math.min(newY, mapHeight - 20));

        // 장애물 충돌 체크
        if (!checkCollisionWithObstacles(newX, newY)) {
            playerX = newX;
            playerY = newY;
        } else {
            // 장애물과 충돌 시 X축과 Y축 개별 체크
            if (!checkCollisionWithObstacles(newX, oldY)) {
                playerX = newX;
            }
            if (!checkCollisionWithObstacles(oldX, newY)) {
                playerY = newY;
            }
        }

        // 카메라 업데이트
        updateCamera();

        // 위치가 변경되면 서버에 전송 (움직일 때만 전송하여 네트워크 부하 감소)
        if (oldX != playerX || oldY != playerY) {
            sendPosition();
        }
    }

    /**
     * 장애물 충돌 체크
     */
    private boolean checkCollisionWithObstacles(int x, int y) {
        // 1) walkableGrid가 있으면 우선 사용: walkable이 아니면 충돌 간주
        if (walkableGrid != null) {
            if (!isPositionWalkable(x, y))
                return true; // 이동 불가 = 충돌
        }
        // 2) 장애물 사각형 교차 검사 (폴백 및 보강)
        Rectangle playerBounds = new Rectangle(x - 20, y - 20, 40, 40);
        for (Rectangle obs : obstacles) {
            if (playerBounds.intersects(obs)) {
                return true; // 충돌 발생
            }
        }
        return false; // 충돌 없음
    }

    /**
     * 플레이어 반경을 샘플링하여 해당 위치가 모두 walkable인지 확인
     */
    private boolean isPositionWalkable(int x, int y) {
        int r = 18; // 캐릭터 반경 근사
        int[][] samples = new int[][] {
                { x, y }, { x - r, y }, { x + r, y }, { x, y - r }, { x, y + r },
                { x - r, y - r }, { x + r, y - r }, { x - r, y + r }, { x + r, y + r }
        };
        for (int[] p : samples) {
            int cx = Math.max(0, Math.min(p[0], mapWidth - 1));
            int cy = Math.max(0, Math.min(p[1], mapHeight - 1));
            int col = cx / TILE_SIZE;
            int row = cy / TILE_SIZE;
            if (row < 0 || row >= gridRows || col < 0 || col >= gridCols)
                return false;
            if (!walkableGrid[row][col])
                return false;
        }
        return true;
    }

    /**
     * 카메라 업데이트 (플레이어 중심)
     */
    private void updateCamera() {
        // 플레이어를 화면 중앙에 위치
        cameraX = playerX - GameConstants.GAME_WIDTH / 2;
        cameraY = playerY - GameConstants.GAME_HEIGHT / 2;

        // 카메라가 맵 경계를 벗어나지 않도록 제한
        cameraX = Math.max(0, Math.min(cameraX, mapWidth - GameConstants.GAME_WIDTH));
        cameraY = Math.max(0, Math.min(cameraY, mapHeight - GameConstants.GAME_HEIGHT));
    }

    private void updateMissiles() {
        Iterator<Missile> it = missiles.iterator();
        while (it.hasNext()) {
            Missile m = it.next();
            m.x += m.dx;
            m.y += m.dy;
            // 맵 밖이면 제거 (전체 맵 기준)
            if (m.x < 0 || m.x > mapWidth || m.y < 0 || m.y > mapHeight) {
                it.remove();
                continue;
            }
            // 벽 충돌: 타일 walkable 여부 + 장애물 Rect 교차
            if (isMissileBlocked(m.x, m.y)) {
                it.remove();
                continue;
            }
        }
    }

    private boolean isMissileBlocked(int x, int y) {
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

    private void checkCollisions() {
        // 내 미사일과 다른 플레이어 충돌
        Iterator<Missile> it = missiles.iterator();
        while (it.hasNext()) {
            Missile m = it.next();
            if (m.team == team && m.owner != null && m.owner.equals(playerName)) {
                // 적 플레이어와 충돌 체크
                boolean hit = false;
                for (Map.Entry<String, PlayerData> entry : players.entrySet()) {
                    PlayerData p = entry.getValue();
                    if (p.team != team) {
                        double dist = Math.sqrt(Math.pow(m.x - p.x, 2) + Math.pow(m.y - p.y, 2));
                        if (dist < 20) {
                            it.remove();
                            hit = true;
                            // 서버에 적 플레이어 피격 보고
                            String targetName = entry.getKey();
                            networkClient.sendHitReport("HIT:" + targetName);
                            System.out.println("[HIT] My missile hit " + targetName);
                            break;
                        }
                    }
                }

                // 설치된 오브젝트와 충돌 체크 (적 오브젝트만)
                if (!hit) {
                    for (Map.Entry<Integer, PlacedObjectClient> entry : placedObjects.entrySet()) {
                        PlacedObjectClient obj = entry.getValue();
                        if (obj.team != team && obj.hp > 0) {
                            double dist = Math.sqrt(Math.pow(m.x - obj.x, 2) + Math.pow(m.y - obj.y, 2));
                            if (dist < 30) {
                                it.remove();
                                // 서버에 오브젝트 피격 보고
                                networkClient.sendHitReport("HIT_OBJ:" + obj.id);
                                System.out.println("[HIT_OBJ] My missile hit object " + obj.id);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 적 미사일과 내가 맞았는지 체크 (피해자 측 리포트)
        Iterator<Missile> enemyIt = missiles.iterator();
        while (enemyIt.hasNext()) {
            Missile m = enemyIt.next();
            if (m.team != team) {
                double dist = Math.sqrt(Math.pow(m.x - playerX, 2) + Math.pow(m.y - playerY, 2));
                if (dist < 20) {
                    enemyIt.remove();
                    if (m.owner != null) {
                        // 터렛 미사일인 경우 TURRET: 접두사가 이미 포함되어 있음
                        String ownerInfo = m.owner;
                        // 자기 자신의 터렛에 맞지 않도록 체크
                        if (ownerInfo.startsWith("TURRET:")) {
                            String turretOwner = ownerInfo.substring(7);
                            if (turretOwner.equals(playerName)) {
                                System.out.println("[DEBUG] Ignored own turret missile hit");
                                continue; // 자기 터렛 미사일은 무시
                            }
                        }
                        networkClient.sendHitReport("HITME:" + ownerInfo);
                    } else {
                        networkClient.sendHitReport("DEATH");
                    }
                    break;
                }
            }
        }
    }

    void respawn() {
        // 반드시 지정된 스폰 타일 중에서만 랜덤 스폰
        java.util.List<int[]> tiles = (team == GameConstants.TEAM_RED ? redSpawnTiles : blueSpawnTiles);
        if (tiles != null && !tiles.isEmpty()) {
            java.util.Random rand = new java.util.Random();
            int[] t = tiles.get(rand.nextInt(tiles.size()));
            playerX = t[0] * TILE_SIZE + TILE_SIZE / 2;
            playerY = t[1] * TILE_SIZE + TILE_SIZE / 2;
        } else {
            appendChatMessage("[경고] 팀 스폰 타일이 비어 있어 임시 위치에 리스폰합니다. 맵 JSON의 spawns." +
                    (team == GameConstants.TEAM_RED ? "red" : "blue") + ".tiles를 지정하세요.");
            // 임시 폴백: 맵 중앙
            playerX = mapWidth / 2;
            playerY = mapHeight / 2;
        }
        gameState.setMyHP(gameState.getMyMaxHP());

        appendChatMessage("[리스폰] 위치: (" + playerX + ", " + playerY + ")");

        // 서버에 리스폰 알림
        try {
            out.writeUTF("RESPAWN:" + playerX + "," + playerY);
            out.flush();
        } catch (IOException ex) {
            appendChatMessage("[에러] 리스폰 전송 실패: " + ex.getMessage());
        }
        sendPosition();
    }

    private void sendPosition() {
        if (out != null) {
            networkClient.sendPosition(playerX, playerY, myDirection);
        }
    }

    private void shootMissile(int targetX, int targetY) {
        // 플레이어 위치에서 마우스 방향으로 발사
        int speed = (int) (GameConstants.MISSILE_SPEED * missileSpeedMultiplier);
        int sx = playerX;
        int sy = playerY;
        int tx = targetX;
        int ty = targetY;
        int vx = tx - sx;
        int vy = ty - sy;
        if (vx == 0 && vy == 0) {
            vy = -1;
        }
        double len = Math.sqrt(vx * vx + vy * vy);
        double nx = (len > 0) ? (vx / len) : 0.0;
        double ny = (len > 0) ? (vy / len) : -1.0;
        int dx = (int) Math.round(nx * speed);
        int dy = (int) Math.round(ny * speed);
        Missile missile = new Missile(sx, sy, dx, dy, team, playerName);
        missiles.add(missile);

        if (out != null) {
            try {
                out.writeUTF("SHOOT:" + sx + "," + sy + "," + dx + "," + dy);
                out.flush();
            } catch (IOException ex) {
                appendChatMessage("[네트워크] 발사 전송 실패: " + ex.getMessage());
            }
        }

        // 총구 섬광 이펙트 (로컬) - 발사 방향 각도 기반 단발 섬광
        double angle = Math.atan2(ny, nx);
        skillEffects.addSelf(new MuzzleFlashEffect(angle));
    }

    private void processGameMessage(String message) {
        messageHandler.handleMessage(message);
    }

    /**
     * 로비로 복귀
     */
    void returnToLobby() {
        // 게임 종료
        if (timer != null) {
            timer.stop();
        }
        disconnect();

        // 현재 창 닫기 및 로비 열기 (현재 선택된 캐릭터 유지)
        final String currentChar = gameState.getSelectedCharacter(); // ?? ??? ??
        javax.swing.SwingUtilities.invokeLater(() -> {
            this.dispose(); // GamePanel? JFrame??? ?? dispose

            GameConfig.saveCharacter("");
            System.out.println("[??] ? ?? - ??? ???, ??: " + currentChar);
            LobbyFrame lobby = new LobbyFrame(playerName);
            lobby.setVisible(true);
        });
    }

    private void disconnect() {
        try {
            if (timer != null)
                timer.stop();
            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if (socket != null)
                socket.close();
        } catch (IOException ex) {
            System.err.println("[ERROR] Failed to close network resources");
            ex.printStackTrace(System.err);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
        int keyCode = e.getKeyCode();

        // 편집 모드 저장 단축키 (Ctrl+S)
        if (editMode && keyCode == KeyEvent.VK_S && (e.isControlDown() || e.isMetaDown())) {
            saveEditedMap();
            return;
        }

        // 사용자 설정 키 바인딩 체크
        if (KeyBindingConfig.isKeyPressed(keyCode, KeyBindingConfig.KEY_TACTICAL_SKILL)) {
            useTacticalSkill();
        } else if (KeyBindingConfig.isKeyPressed(keyCode, KeyBindingConfig.KEY_ULTIMATE_SKILL)) {
            useUltimateSkill();
        } else if (KeyBindingConfig.isKeyPressed(keyCode, KeyBindingConfig.KEY_CHARACTER_SELECT)) {
            openCharacterSelect();
        } else if (KeyBindingConfig.isKeyPressed(keyCode, KeyBindingConfig.KEY_MINIMAP_TOGGLE)) {
            showMinimap = !showMinimap;
            appendChatMessage("[시스템] 미니맵 " + (showMinimap ? "켜짐" : "꺼짐"));
        }

        // 고정 키 (디버그 및 에디터 기능)
        switch (keyCode) {
            case KeyEvent.VK_F3 -> { // 장애물 디버그 토글
                debugObstacles = !debugObstacles;
                appendChatMessage("[디버그] 장애물 표시 " + (debugObstacles ? "ON" : "OFF"));
            }
            case KeyEvent.VK_F4 -> { // 편집 모드 토글
                editMode = !editMode;
                paintState = -1;
                appendChatMessage(editMode ? "[에디터] 타일 편집 모드 ON" : "[에디터] 타일 편집 모드 OFF");
            }
            case KeyEvent.VK_1 -> {
                if (editMode) {
                    editPaintMode = 0;
                    appendChatMessage("[에디터] 모드: 이동 가능 칠하기");
                }
            }
            case KeyEvent.VK_2 -> {
                if (editMode) {
                    editPaintMode = 1;
                    appendChatMessage("[에디터] 모드: 이동 불가(벽) 칠하기");
                }
            }
            case KeyEvent.VK_3 -> {
                if (editMode) {
                    editPaintMode = 2;
                    appendChatMessage("[에디터] 모드: RED 스폰 토글");
                }
            }
            case KeyEvent.VK_4 -> {
                if (editMode) {
                    editPaintMode = 3;
                    appendChatMessage("[에디터] 모드: BLUE 스폰 토글");
                }
            }
            case KeyEvent.VK_F6 -> { // 맵 전환: 순환 목록 기반
                rebuildMapCycle();
                if (mapCycle == null || mapCycle.isEmpty()) {
                    appendChatMessage("[시스템] 전환 가능한 맵이 없습니다.");
                } else {
                    int idx = mapCycle.indexOf(currentMapName);
                    idx = (idx >= 0) ? (idx + 1) % mapCycle.size() : 0;
                    mapIndex = idx;
                    switchMap(mapCycle.get(idx));
                }
            }
            case KeyEvent.VK_F5 -> { // 수동 저장 키
                saveEditedMap();
            }
            case KeyEvent.VK_T, KeyEvent.VK_ENTER -> chatInput.requestFocusInWindow(); // 채팅 포커스
        }
    }

    // ===== 편집 모드 유틸 =====
    private void updateHoverTile(int mapX, int mapY) {
        if (walkableGrid == null)
            return;
        int col = mapX / TILE_SIZE;
        int row = mapY / TILE_SIZE;
        if (col < 0 || col >= gridCols || row < 0 || row >= gridRows) {
            hoverCol = -1;
            hoverRow = -1;
            return;
        }
        hoverCol = col;
        hoverRow = row;
    }

    private void startPaintAt(int mapX, int mapY) {
        if (walkableGrid == null)
            return;
        int col = mapX / TILE_SIZE;
        int row = mapY / TILE_SIZE;
        if (col < 0 || col >= gridCols || row < 0 || row >= gridRows)
            return;
        applyEditAction(col, row, false);
        hoverCol = col;
        hoverRow = row;
    }

    private void continuePaintAt(int mapX, int mapY) {
        if (walkableGrid == null || paintState == -1)
            return;
        int col = mapX / TILE_SIZE;
        int row = mapY / TILE_SIZE;
        if (col < 0 || col >= gridCols || row < 0 || row >= gridRows)
            return;
        hoverCol = col;
        hoverRow = row;
        applyEditAction(col, row, true);
    }

    // 편집 모드 액션 적용
    private void applyEditAction(int col, int row, boolean dragging) {
        switch (editPaintMode) {
            case 0 -> { // 이동 가능 칠하기
                boolean before = walkableGrid[row][col];
                walkableGrid[row][col] = true;
                if (!dragging)
                    paintState = 1;
                if (before != walkableGrid[row][col])
                    rebuildObstaclesFromWalkable();
            }
            case 1 -> { // 이동 불가 칠하기
                boolean before = walkableGrid[row][col];
                walkableGrid[row][col] = false;
                if (!dragging)
                    paintState = 0;
                if (before != walkableGrid[row][col])
                    rebuildObstaclesFromWalkable();
                // 벽으로 칠하면 해당 타일의 스폰은 제거
                removeSpawnTile(redSpawnTiles, col, row);
                removeSpawnTile(blueSpawnTiles, col, row);
                recomputeSpawnZones();
            }
            case 2 -> { // RED 스폰 토글
                toggleSpawnTile(redSpawnTiles, col, row);
                // 스폰은 항상 walkable
                if (!walkableGrid[row][col]) {
                    walkableGrid[row][col] = true;
                    rebuildObstaclesFromWalkable();
                }
                // 다른 팀 스폰과 겹치지 않게
                removeSpawnTile(blueSpawnTiles, col, row);
                recomputeSpawnZones();
                ensureSpawnZonesWalkable();
                if (!dragging)
                    paintState = 1;
            }
            case 3 -> { // BLUE 스폰 토글
                toggleSpawnTile(blueSpawnTiles, col, row);
                if (!walkableGrid[row][col]) {
                    walkableGrid[row][col] = true;
                    rebuildObstaclesFromWalkable();
                }
                removeSpawnTile(redSpawnTiles, col, row);
                recomputeSpawnZones();
                ensureSpawnZonesWalkable();
                if (!dragging)
                    paintState = 1;
            }
        }
    }

    private boolean isSpawnTile(java.util.List<int[]> list, int col, int row) {
        if (list == null)
            return false;
        for (int[] t : list) {
            if (t[0] == col && t[1] == row)
                return true;
        }
        return false;
    }

    private void toggleSpawnTile(java.util.List<int[]> list, int col, int row) {
        if (isSpawnTile(list, col, row)) {
            removeSpawnTile(list, col, row);
        } else {
            list.add(new int[] { col, row });
        }
    }

    private void removeSpawnTile(java.util.List<int[]> list, int col, int row) {
        if (list == null)
            return;
        list.removeIf(t -> t[0] == col && t[1] == row);
    }

    private void recomputeSpawnZones() {
        redSpawnZone = computeSpawnZoneFromTiles(redSpawnTiles);
        blueSpawnZone = computeSpawnZoneFromTiles(blueSpawnTiles);
    }

    private Rectangle computeSpawnZoneFromTiles(java.util.List<int[]> tiles) {
        if (tiles == null || tiles.isEmpty())
            return null;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (int[] t : tiles) {
            minX = Math.min(minX, t[0]);
            minY = Math.min(minY, t[1]);
            maxX = Math.max(maxX, t[0]);
            maxY = Math.max(maxY, t[1]);
        }
        return new Rectangle(minX * TILE_SIZE, minY * TILE_SIZE, (maxX - minX + 1) * TILE_SIZE,
                (maxY - minY + 1) * TILE_SIZE);
    }

    private void rebuildObstaclesFromWalkable() {
        obstacles.clear();
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                if (!walkableGrid[r][c]) {
                    obstacles.add(new Rectangle(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE));
                }
            }
        }
    }

    /**
     * 편집된 맵을 JSON 파일로 저장 (assets/maps/<mapName>_edited.json)
     */
    private void saveEditedMap() {
        if (walkableGrid == null) {
            appendChatMessage("[에디터] 저장 실패: walkableGrid 없음");
            return;
        }
        String fileName = (currentMapName != null && !currentMapName.isEmpty() ? currentMapName : "map")
                + "_edited.json";
        File dir = new File("assets" + File.separator + "maps");
        if (!dir.exists())
            dir.mkdirs();
        File outFile = new File(dir, fileName);
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outFile), java.nio.charset.StandardCharsets.UTF_8))) {
            bw.write(generateEditedMapJson());
            bw.flush();
            appendChatMessage("[에디터] 저장 완료: " + outFile.getPath());
        } catch (java.io.IOException ex) {
            appendChatMessage("[에디터] 저장 실패: " + ex.getMessage());
        }
    }

    /**
     * 현재 walkableGrid와 스폰 타일을 obstacles 기반 JSON으로 직렬화
     */
    private String generateEditedMapJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"meta\": {\n");
        sb.append("    \"map_pixel_size\": { \"w\": ").append(mapWidth).append(", \"h\": ").append(mapHeight)
                .append(" },\n");
        sb.append("    \"tile_size\": ").append(TILE_SIZE).append("\n");
        sb.append("  },\n");
        // obstacles: walkable == false 타일
        sb.append("  \"obstacles\": [\n");
        int count = 0;
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                if (!walkableGrid[r][c]) {
                    if (count > 0)
                        sb.append(",\n");
                    sb.append("    { \"x\": ").append(c).append(", \"y\": ").append(r).append(" }");
                    count++;
                }
            }
        }
        sb.append("\n  ],\n");
        // spawns
        sb.append("  \"spawns\": {\n");
        // red
        sb.append("    \"red\": { \"tiles\": [");
        if (redSpawnTiles != null && !redSpawnTiles.isEmpty()) {
            for (int i = 0; i < redSpawnTiles.size(); i++) {
                int[] t = redSpawnTiles.get(i);
                if (i > 0)
                    sb.append(", ");
                sb.append("{ \"x\": ").append(t[0]).append(", \"y\": ").append(t[1]).append(" }");
            }
        }
        sb.append("] },\n");
        // blue
        sb.append("    \"blue\": { \"tiles\": [");
        if (blueSpawnTiles != null && !blueSpawnTiles.isEmpty()) {
            for (int i = 0; i < blueSpawnTiles.size(); i++) {
                int[] t = blueSpawnTiles.get(i);
                if (i > 0)
                    sb.append(", ");
                sb.append("{ \"x\": ").append(t[0]).append(", \"y\": ").append(t[1]).append(" }");
            }
        }
        sb.append("] }\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * 기본 공격 사용 (좌클릭)
     * targetX, targetY는 맵 좌표입니다.
     */
    private void useBasicAttack(int targetX, int targetY) {
        if (abilities != null && abilities.length > 0) {
            Ability basicAttack = abilities[0];
            if (basicAttack.canUse()) {
                basicAttack.activate();
                // targetX, targetY는 맵 좌표
                shootMissile(targetX, targetY);

                // 서버에 스킬 사용 알림
                sendSkillUse(0, "BASIC");
                // 로컬 이펙트 추가 (즉발형은 짧게)
                addLocalEffect(basicAttack);
            }
        }
    }

    /**
     * 전술 스킬 사용 (E키)
     */
    private void useTacticalSkill() {
        if (abilities != null && abilities.length > 1) {
            Ability tactical = abilities[1];
            if (tactical.canUse()) {
                tactical.activate();
                appendChatMessage("[스킬] " + tactical.getName() + " 사용!");

                // Technician 지뢰: 플레이어 위치에 설치
                if ("tech_mine".equalsIgnoreCase(tactical.id)) {
                    sendSkillUse(1, "TACTICAL", playerX, playerY);
                } else {
                    sendSkillUse(1, "TACTICAL");
                }

                // 스킬별 효과 적용
                applySkillEffect(tactical);
                addLocalEffect(tactical);
                if ("raven".equalsIgnoreCase(gameState.getSelectedCharacter())) {
                    ravenDashRemaining = Math.max(ravenDashRemaining, tactical.getActiveDuration());
                }
            }
        }
    }

    /**
     * 궁극기 사용 (R키)
     */
    private void useUltimateSkill() {
        if (abilities != null && abilities.length > 2) {
            Ability ultimate = abilities[2];
            if (ultimate.canUse()) {
                // General 에어스트라이크: 미니맵 타겟팅 모드 활성화
                if ("gen_strike".equalsIgnoreCase(ultimate.id)) {
                    awaitingMinimapTarget = true;
                    appendChatMessage("[General] 미니맵을 클릭하여 에어스트라이크 타겟을 지정하세요!");
                    ultimate.activate(); // 쿨다운 시작
                    return;
                }

                // Technician 터렛: 플레이어 위치에 설치
                if ("tech_turret".equalsIgnoreCase(ultimate.id)) {
                    ultimate.activate();
                    appendChatMessage("[궁극기] " + ultimate.getName() + " 발동!");
                    sendSkillUse(2, "ULTIMATE", playerX, playerY);
                    applySkillEffect(ultimate);
                    addLocalEffect(ultimate);
                    return;
                }

                // 기타 궁극기
                ultimate.activate();
                appendChatMessage("[궁극기] " + ultimate.getName() + " 발동!");
                sendSkillUse(2, "ULTIMATE");

                // 스킬별 효과 적용
                applySkillEffect(ultimate);
                addLocalEffect(ultimate);
                if ("raven".equalsIgnoreCase(gameState.getSelectedCharacter())) {
                    ravenOverchargeRemaining = ultimate.getActiveDuration();
                    missileSpeedMultiplier = 1.8f;
                    if (abilities != null && abilities.length > 0) {
                        abilities[0].setCooldownMultiplier(0.35f);
                    }
                }
            }
        }
    }

    private void addLocalEffect(Ability ability) {
        float dur = ability.getActiveDuration() > 0 ? ability.getActiveDuration() : 0.4f;
        myEffects.add(new ActiveEffect(ability.id, ability.getType().name(), dur));
        // === 구조화된 SkillEffect 등록 ===
        // ability.id 에 따라 전용 클래스 이펙트를 추가하여 기존 단순 링보다 풍부한 표현 제공
        // 추후 신규 캐릭터 추가 시 여기 if 블록에 대응 클래스만 연결하면 유지보수 용이.
        String id = ability.id;
        if ("piper_mark".equals(id)) {
            skillEffects.addSelf(new PiperMarkEffect(dur));
        } else if ("piper_thermal".equals(id)) {
            skillEffects.addSelf(new PiperThermalEffect(dur));
        } else if ("raven_dash".equals(id)) {
            skillEffects.addSelf(new RavenDashEffect(dur));
        } else if ("raven_overcharge".equals(id)) {
            skillEffects.addSelf(new RavenOverchargeEffect(dur));
        } else if ("gen_aura".equals(id)) {
            skillEffects.addSelf(new GeneralAuraEffect(dur));
        } else if ("gen_strike".equals(id)) {
            skillEffects.addSelf(new GeneralStrikeEffect(dur));
        } else if ("bull_cover".equals(id)) {
            skillEffects.addSelf(new BulldogCoverEffect(dur));
        } else if ("bull_barrage".equals(id)) {
            skillEffects.addSelf(new BulldogBarrageEffect(dur));
        } else if ("wild_breach".equals(id)) {
            skillEffects.addSelf(new WildcatBreachEffect(dur));
        } else if ("wild_berserk".equals(id)) {
            skillEffects.addSelf(new WildcatBerserkEffect(dur));
        } else if ("ghost_cloak".equals(id)) {
            skillEffects.addSelf(new GhostCloakEffect(dur));
        } else if ("ghost_nullify".equals(id)) {
            skillEffects.addSelf(new GhostNullifyEffect(dur));
        } else if ("skull_adrenaline".equals(id)) {
            skillEffects.addSelf(new SkullAdrenalineEffect(dur));
        } else if ("skull_ammo".equals(id)) {
            skillEffects.addSelf(new SkullAmmoEffect(dur));
        } else if ("steam_emp".equals(id)) {
            skillEffects.addSelf(new SteamEmpEffect(dur));
        } else if ("steam_reset".equals(id)) {
            skillEffects.addSelf(new SteamResetEffect(dur));
        } else if ("tech_mine".equals(id)) {
            skillEffects.addSelf(new TechMineEffect(dur));
        } else if ("tech_turret".equals(id)) {
            skillEffects.addSelf(new TechTurretEffect(dur));
        } else if ("sage_heal".equals(id)) {
            skillEffects.addSelf(new SageHealEffect(dur));
        } else if ("sage_revive".equals(id)) {
            skillEffects.addSelf(new SageReviveEffect(dur));
        }
    }

    /**
     * 스킬 효과 적용 (캐릭터 일부 구현)
     */
    private void applySkillEffect(Ability ability) {
        // Piper: 마킹/열감지 구현 (미니맵 가시성 확장)
        if ("piper".equalsIgnoreCase(gameState.getSelectedCharacter())) {
            if ("piper_mark".equalsIgnoreCase(ability.id)) {
                piperMarkRemaining = ability.getActiveDuration();
                appendChatMessage("[Piper] 적 표시 활성화 (미니맵 시야 확장)");
                teamMarkRemaining = Math.max(teamMarkRemaining, piperMarkRemaining); // 팀 공유
            } else if ("piper_thermal".equalsIgnoreCase(ability.id)) {
                piperThermalRemaining = ability.getActiveDuration();
                appendChatMessage("[Piper] 열감지 활성화 (전체 적 표시)");
                teamThermalRemaining = Math.max(teamThermalRemaining, piperThermalRemaining); // 팀 공유
            }
        }
        System.out.println("스킬 효과: " + ability.getName() + " (ID: " + ability.id + ")");
    }

    /**
     * 서버에 스킬 사용 알림
     */
    private void sendSkillUse(int skillIndex, String skillType) {
        sendSkillUse(skillIndex, skillType, -1, -1);
    }

    /**
     * 서버에 스킬 사용 알림 (좌표 포함)
     */
    private void sendSkillUse(int skillIndex, String skillType, int targetX, int targetY) {
        if (out != null) {
            // abilityId,type,duration[,x,y]
            Ability ability = (abilities != null && skillIndex < abilities.length) ? abilities[skillIndex] : null;
            float dur = (ability != null) ? ability.getActiveDuration() : 0f;
            String abilityId = (ability != null) ? ability.id : ("skill" + skillIndex);
            String msg = abilityId + "," + skillType + "," + dur;
            if (targetX >= 0 && targetY >= 0) {
                msg += "," + targetX + "," + targetY;
            }
            networkClient.sendSkillUse(msg);
        }
    }

    // 라운드 시작
    private void startRound() {
        roundState = RoundState.WAITING;
        roundCount++;
        roundStartTime = System.currentTimeMillis();
        centerMessage = "Round " + roundCount + " Ready";
        centerMessageEndTime = roundStartTime + ROUND_READY_TIME;
        
        // 라운드 시작 시 캐릭터 변경 플래그 초기화
        hasChangedCharacterInRound = false;

        // Map rotation is handled by the server's ROUND_START message; do not switch locally.

        respawn(); // 전원 리스폰 위치로
    }

    private void openCharacterSelect() {
        // 1. 시간 제한 체크 (10초) - 최우선으로 체크
        long now = System.currentTimeMillis();
        long elapsed = now - roundStartTime;
        long remaining = CHARACTER_CHANGE_TIME_LIMIT - elapsed;
        
        if (elapsed >= CHARACTER_CHANGE_TIME_LIMIT) {
            appendChatMessage("[시스템] 캐릭터 변경 가능 시간이 만료되었습니다. (경과: " + (elapsed/1000) + "초)");
            return;
        }
        
        // 2. 라운드 상태 체크 - WAITING 상태가 아니면 변경 불가
        if (roundState != RoundState.WAITING) {
            appendChatMessage("[시스템] 라운드 진행 중에는 캐릭터를 변경할 수 없습니다.");
            return;
        }
        
        // 3. 이미 변경했는지 체크
        if (hasChangedCharacterInRound) {
            appendChatMessage("[시스템] 이번 라운드에 이미 캐릭터를 변경했습니다. (1회 제한)");
            return;
        }

        // 게임 일시정지
        if (timer != null) {
            timer.stop();
        }

        // 현재 팀에서 이미 선택한 캐릭터를 비활성화 목록으로 구성
        java.util.Set<String> disabledCharacters = new java.util.HashSet<>();
        java.util.Map<String, String> characterOwners = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, PlayerData> entry : players.entrySet()) {
            PlayerData pd = entry.getValue();
            if (pd != null && pd.team == team && pd.characterId != null) {
                disabledCharacters.add(pd.characterId);
                characterOwners.put(pd.characterId, entry.getKey());
            }
        }
        // 내 현재 캐릭터는 비활성화 목록에서 제거해 재선택 가능하게 유지
        String currentChar = gameState.getSelectedCharacter();
        if (currentChar != null) {
            disabledCharacters.remove(currentChar);
            characterOwners.remove(currentChar);
        }

        // 캐릭터 선택 다이얼로그 표시 (남은 시간 지나면 자동 닫힘)
        String newCharacter = CharacterSelectDialog.showDialog(this, disabledCharacters, characterOwners, remaining);

        if (newCharacter != null) {
            // 다이얼로그가 닫힌 후 최종 시간 체크 (사용자가 대기하다가 시간 초과 후 선택한 경우 방지)
            long finalElapsed = System.currentTimeMillis() - roundStartTime;
            if (finalElapsed >= CHARACTER_CHANGE_TIME_LIMIT) {
                appendChatMessage("[시스템] 시간이 초과되어 캐릭터 변경이 취소되었습니다.");
                if (timer != null) {
                    timer.start();
                }
                return;
            }
            
            // GameState에 새 캐릭터 설정
            gameState.setSelectedCharacter(newCharacter);
            CharacterData newCharData = CharacterData.getById(newCharacter);
            gameState.setCurrentCharacterData(newCharData);

            // 스킬 재로드 (로컬 유지)
            abilities = CharacterData.createAbilities(newCharacter);
            gameState.setAbilities(abilities);
            
            int newMaxHp = (int) newCharData.health;
            gameState.setMyMaxHP(newMaxHp);
            gameState.setMyHP(newMaxHp);
            
            // 변경 플래그 설정 (서버 응답 전에 먼저 설정하여 중복 요청 방지)
            hasChangedCharacterInRound = true;

            // 서버에 캐릭터 변경 알림
            if (out != null) {
                networkClient.sendCharacterSelect(newCharacter);
                System.out.println("[Client] Character change request sent: " + newCharacter + " at " + elapsed + "ms");
            }

            appendChatMessage("캐릭터를 " + newCharData.name + "으로 변경했습니다.");

            // 스프라이트 재로드
            loadSprites();

            // HUD에 캐릭터 정보 표시
            canvas.repaint();
        }

        // 게임 재개
        if (timer != null) {
            timer.start();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    void loadSprites() {
        try {
            ResourceManager rm = ResourceManager.getInstance();
            myAnimations = new SpriteAnimation[4];

            // 캐릭터별 스프라이트 시트 경로 결정
            String spritePath = "assets/characters/";
            String charId = gameState.getSelectedCharacter();
            if (charId != null) {
                switch (charId.toLowerCase()) {
                    case "raven":
                        spritePath += "Raven_48_64.png";
                        break;
                    case "piper":
                        spritePath += "Piper_48_64.png";
                        break;
                    case "technician":
                    case "tech":
                        spritePath += "Technician_48_64.png";
                        break;
                    case "general":
                    case "gen":
                        spritePath += "General_48_64.png";
                        break;
                    default:
                        spritePath += "Raven_48_64.png";
                        break;
                }
            } else {
                spritePath += "Raven_48_64.png";
            }

            System.out.println("[SPRITE] Loading: " + spritePath);

            // 스프라이트 시트 로드 (48x64 크기, 4행 구조: Down, Left, Up, Right)
            java.awt.image.BufferedImage[] walkSheet = rm.getSpriteSheet(spritePath, 48, 64);

            System.out.println("[SPRITE] Walk sheet: " + (walkSheet != null ? walkSheet.length + " frames" : "NULL"));

            if (walkSheet != null && walkSheet.length > 0) {
                // 프레임 수 계산 (전체 프레임 / 4행)
                int framesPerRow = walkSheet.length / 4;
                System.out.println("[SPRITE] Frames per row: " + framesPerRow);

                // 각 방향의 프레임 추출
                java.awt.image.BufferedImage[] downFrames = new java.awt.image.BufferedImage[framesPerRow];
                java.awt.image.BufferedImage[] leftFrames = new java.awt.image.BufferedImage[framesPerRow];
                java.awt.image.BufferedImage[] upFrames = new java.awt.image.BufferedImage[framesPerRow];
                java.awt.image.BufferedImage[] rightFrames = new java.awt.image.BufferedImage[framesPerRow];

                for (int i = 0; i < framesPerRow; i++) {
                    downFrames[i] = walkSheet[0 * framesPerRow + i]; // Row 0: DOWN
                    rightFrames[i] = walkSheet[1 * framesPerRow + i]; // Row 1: right
                    upFrames[i] = walkSheet[2 * framesPerRow + i]; // Row 2: UP
                    leftFrames[i] = walkSheet[3 * framesPerRow + i]; // Row 3: left
                }

                // Walk animations (4방향만)
                myAnimations[0] = new SpriteAnimation(downFrames, 150, true); // Down
                myAnimations[1] = new SpriteAnimation(upFrames, 150, true); // Up
                myAnimations[2] = new SpriteAnimation(leftFrames, 150, true); // Left
                myAnimations[3] = new SpriteAnimation(rightFrames, 150, true); // Right

                System.out.println("[SPRITE] Walk animations created for " + charId);
            } else {
                System.out.println("[ERROR] Walk sheet invalid!");
            }

            // 각 애니메이션 확인
            for (int i = 0; i < 4; i++) {
                System.out.println("[SPRITE] Animation[" + i + "]: " + (myAnimations[i] != null ? "OK" : "NULL"));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] 스프라이트 로드 에러: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /**
     * 원격 플레이어의 스프라이트를 로드합니다
     */
    void loadPlayerSprites(PlayerData player, String characterId) {
        try {
            System.out.println("[SPRITE] 시작: " + characterId + " 스프라이트 로딩...");
            
            ResourceManager rm = ResourceManager.getInstance();
            player.animations = new SpriteAnimation[4];

            // 캐릭터별 스프라이트 시트 경로 결정
            String spritePath = "assets/characters/";
            if (characterId != null) {
                switch (characterId.toLowerCase()) {
                    case "raven":
                        spritePath += "Raven_48_64.png";
                        break;
                    case "piper":
                        spritePath += "Piper_48_64.png";
                        break;
                    case "technician":
                    case "tech":
                        spritePath += "Technician_48_64.png";
                        break;
                    case "general":
                    case "gen":
                        spritePath += "General_48_64.png";
                        break;
                    default:
                        spritePath += "Raven_48_64.png";
                        System.out.println("[SPRITE] 경고: 알 수 없는 캐릭터 ID '" + characterId + "', Raven으로 대체");
                        break;
                }
            } else {
                spritePath += "Raven_48_64.png";
            }

            System.out.println("[SPRITE] 로딩 경로: " + spritePath);

            // 스프라이트 시트 로드 (48x64 크기, 4행 구조: Down, Left, Up, Right)
            java.awt.image.BufferedImage[] walkSheet = rm.getSpriteSheet(spritePath, 48, 64);

            if (walkSheet != null && walkSheet.length > 0) {
                int framesPerRow = walkSheet.length / 4;

                java.awt.image.BufferedImage[] downFrames = new java.awt.image.BufferedImage[framesPerRow];
                java.awt.image.BufferedImage[] leftFrames = new java.awt.image.BufferedImage[framesPerRow];
                java.awt.image.BufferedImage[] upFrames = new java.awt.image.BufferedImage[framesPerRow];
                java.awt.image.BufferedImage[] rightFrames = new java.awt.image.BufferedImage[framesPerRow];

                for (int i = 0; i < framesPerRow; i++) {
                    downFrames[i] = walkSheet[0 * framesPerRow + i];
                    rightFrames[i] = walkSheet[1 * framesPerRow + i];
                    upFrames[i] = walkSheet[2 * framesPerRow + i];
                    leftFrames[i] = walkSheet[3 * framesPerRow + i];
                }

                player.animations[0] = new SpriteAnimation(downFrames, 150, true);
                player.animations[1] = new SpriteAnimation(upFrames, 150, true);
                player.animations[2] = new SpriteAnimation(leftFrames, 150, true);
                player.animations[3] = new SpriteAnimation(rightFrames, 150, true);

                System.out.println("[SPRITE] ✅ 성공: " + characterId + " 애니메이션 로드 완료 (4방향)");
            } else {
                System.err.println("[SPRITE] ❌ 실패: walkSheet가 null이거나 비어있음");
            }
        } catch (Exception e) {
            System.err.println("[SPRITE] ❌ 치명적 오류: " + characterId + " 로드 실패");
            e.printStackTrace(System.err);
            
            // Fallback: 기본 애니메이션 설정 (null 방지)
            player.animations = null;
        }
    }

    private void updateMyAnimation() {
        if (myAnimations == null)
            return;

        // 이동 키 입력에 따른 방향 설정
        int oldDir = myDirection;
        // 우선순위: 마지막으로 누른 키가 적용되도록 (S > W > A > D 순서로 체크)
        if (keys[KeyEvent.VK_D]) {
            myDirection = 3; // Right
        }
        if (keys[KeyEvent.VK_A]) {
            myDirection = 2; // Left
        }
        if (keys[KeyEvent.VK_W]) {
            myDirection = 1; // Up
        }
        if (keys[KeyEvent.VK_S]) {
            myDirection = 0; // Down
        }
        if (oldDir != myDirection) {
            System.out.println("[ANIM] Direction: " + oldDir + " -> " + myDirection);
        }

        // 현재 애니메이션 업데이트
        if (myDirection < myAnimations.length && myAnimations[myDirection] != null) {
            // 이동 중일 때만 업데이트
            boolean isMoving = keys[KeyEvent.VK_W] || keys[KeyEvent.VK_S] || keys[KeyEvent.VK_A] || keys[KeyEvent.VK_D];
            if (isMoving) {
                myAnimations[myDirection].update();
            } else {
                myAnimations[myDirection].reset(); // 멈추면 첫 프레임으로
            }
        }
    }

}

    
