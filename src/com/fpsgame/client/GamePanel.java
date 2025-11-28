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
 */
public class GamePanel extends JFrame implements KeyListener {

    private String playerName;
    private int team;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    private javax.swing.Timer timer;
    private int playerX = 400;
    private int playerY = 300;
    private final int SPEED = 5;
    private boolean[] keys = new boolean[256];

    // 선택된 캐릭터
    private String selectedCharacter = "raven"; // 기본값
    private CharacterData currentCharacterData;

    // 스킬 시스템
    private Ability[] abilities; // [기본공격, 전술스킬, 궁극기]
    private long lastBasicAttackTime = 0;

    // 스킬 이펙트 (네트워크 포함)
    private static class ActiveEffect {
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
    private final Map<String, java.util.List<ActiveEffect>> effectsByPlayer = new HashMap<>();
    private final java.util.List<ActiveEffect> myEffects = new ArrayList<>();
    // Structured skill effects
    private final SkillEffectManager skillEffects = new SkillEffectManager();

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
    private Map<String, PlayerData> players = new HashMap<>();

    // 미사일 리스트
    private List<Missile> missiles = new ArrayList<>();

    // 게임 패널
    private GameCanvas canvas;

    // 마우스 위치 (조준선용)
    private int mouseX = 400;
    private int mouseY = 300;

    // 미니맵 표시 여부
    private boolean showMinimap = true;

    // 시야 범위 (화면 크기 기반 - 화면에 보이는 범위)
    // 대각선 거리의 절반을 시야로 사용 (화면 중앙 기준)
    private static final int VISION_RANGE = (int) (Math.sqrt(
            GameConstants.GAME_WIDTH * GameConstants.GAME_WIDTH +
                    GameConstants.GAME_HEIGHT * GameConstants.GAME_HEIGHT)
            / 2);
    // Piper 마킹 시 시야 배율 (기본보다 넓지만 전체는 아님)
    private static final float PIPER_MARK_RANGE_FACTOR = 1.7f;
    private static final int PIPER_THERMAL_DOT_SIZE = 10; // 열감지 시 점 크기 증가

    // 맵 시스템
    private java.awt.image.BufferedImage mapImage; // 맵 배경 이미지
    private int mapWidth = 3200; // 맵 전체 크기 (넓은 맵, 화면의 4배)
    private int mapHeight = 2400; // 화면의 4배
    private int cameraX = 0; // 카메라 위치 (플레이어 중심)
    private int cameraY = 0;
    private String currentMapName = "map"; // 기본 맵 (map.png 사용)
    // 타일 그리드
    private static final int TILE_SIZE = 32;
    private boolean[][] walkableGrid; // true = 이동 가능
    private int gridCols, gridRows;
    private Rectangle redSpawnZone, blueSpawnZone; // 팀 스폰 구역
    // 스폰 타일 원본 목록 (랜덤 스폰을 타일 단위로 정확히 하도록 유지)
    private java.util.List<int[]> redSpawnTiles = new ArrayList<>();
    private java.util.List<int[]> blueSpawnTiles = new ArrayList<>();

    // 장애물 시스템
    private java.util.List<Rectangle> obstacles = new ArrayList<>();
    // 디버그 토글
    private boolean debugObstacles = true; // 기본 표시, F3로 토글

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

    // 킬/데스 카운터
    private int kills = 0;
    private int deaths = 0;
    private int myHP = GameConstants.MAX_HP;

    // 설치된 오브젝트 (지뢰, 터렛)
    private Map<Integer, PlacedObjectClient> placedObjects = new HashMap<>();

    // 에어스트라이크 마커
    private Map<Integer, StrikeMarker> strikeMarkers = new HashMap<>();

    // General 궁극기: 미니맵 타겟팅 대기 상태
    private boolean awaitingMinimapTarget = false;

    // 버프 상태 (gen_aura 등)
    private float moveSpeedMultiplier = 1.0f;
    private float attackSpeedMultiplier = 1.0f;

    // 라운드 시스템
    private enum RoundState {
        WAITING, PLAYING, ENDED
    }

    private RoundState roundState = RoundState.WAITING;
    private int roundCount = 1;
    private int redWins = 0;
    private int blueWins = 0;
    private long roundStartTime = 0;
    private static final int MAX_ROUNDS = 3; // 3판 2선승
    private static final int ROUND_READY_TIME = 10000; // 10초 대기
    private String centerMessage = "";
    private long centerMessageEndTime = 0;

    // 캐릭터 선택 제한 관련 변수
    private boolean hasChangedCharacterInRound = false;
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
        int kills;
        int deaths;

        PlayerData(int x, int y, int team) {
            this.x = x;
            this.y = y;
            this.targetX = x;
            this.targetY = y;
            this.team = team;
            this.hp = GameConstants.MAX_HP;
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
            setPreferredSize(new Dimension(GameConstants.GAME_WIDTH, GameConstants.GAME_HEIGHT));
            setBackground(new Color(20, 25, 35));
            setFocusable(true);
            addKeyListener(GamePanel.this);

            // 마우스 클릭으로 기본 공격 (마우스 방향으로 발사)
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // 미니맵 타겟팅 모드: General 에어스트라이크
                    if (awaitingMinimapTarget && e.getButton() == MouseEvent.BUTTON1) {
                        // 미니맵 영역 체크
                        int minimapWidth = 200;
                        int minimapHeight = 150;
                        int minimapX = getWidth() - minimapWidth - 20;
                        int minimapY = 20;

                        if (e.getX() >= minimapX && e.getX() <= minimapX + minimapWidth &&
                                e.getY() >= minimapY && e.getY() <= minimapY + minimapHeight) {
                            // 미니맵 좌표를 맵 좌표로 변환
                            float scaleX = (float) minimapWidth / mapWidth;
                            float scaleY = (float) minimapHeight / mapHeight;
                            int targetMapX = (int) ((e.getX() - minimapX) / scaleX);
                            int targetMapY = (int) ((e.getY() - minimapY) / scaleY);

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
                        int mapX = e.getX() + cameraX;
                        int mapY = e.getY() + cameraY;
                        startPaintAt(mapX, mapY);
                        return;
                    }
                    // 게임 모드: 좌클릭 공격
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        int targetMapX = e.getX() + cameraX;
                        int targetMapY = e.getY() + cameraY;
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
                    mouseX = e.getX();
                    mouseY = e.getY();
                    if (editMode)
                        updateHoverTile(mouseX + cameraX, mouseY + cameraY);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    mouseX = e.getX();
                    mouseY = e.getY();
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
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 디버그: 주요 UI/데이터 상태 출력
            System.out.println("[DEBUG] paintComponent called");
            System.out.println("[DEBUG] abilities=" + (abilities == null ? "null" : Arrays.toString(abilities)));
            System.out.println("[DEBUG] players.size=" + (players == null ? "null" : players.size()));
            System.out.println("[DEBUG] showMinimap=" + showMinimap);
            System.out.println("[DEBUG] chatArea=" + (chatArea == null ? "null" : "ok"));
            System.out.println("[DEBUG] canvas size=" + getWidth() + "," + getHeight());

            // 1. 맵 배경 그리기 (카메라 오프셋 적용)
            if (mapImage != null) {
                g2d.drawImage(mapImage, -cameraX, -cameraY, mapWidth, mapHeight, null);
            } else {
                // 폴백: 그리드
                drawGrid(g2d);
            }

            // 2. 장애물 디버그 표시 (개발용 - 위치 확인)
            drawObstacles(g2d);

            // 2.1 에어스트라이크 마커 (바닥 표시)
            drawStrikeMarkersMain(g2d);

            // 2.5. 편집 모드 타일 오버레이 (UI 렌더링 이전에 호출)
            if (editMode) {
                drawEditorOverlay(g2d);
            }

            // ...existing code...

            // ...existing code...

            // 3. 다른 플레이어들 그리기 (화면 좌표로 변환)
            for (Map.Entry<String, PlayerData> entry : players.entrySet()) {
                PlayerData p = entry.getValue();

                // 맵 좌표를 화면 좌표로 변환
                int screenX = p.x - cameraX;
                int screenY = p.y - cameraY;

                // 화면 내에 있는 경우에만 그리기 (최적화)
                if (isOnScreen(screenX, screenY)) {
                    Color playerColor = p.team == GameConstants.TEAM_RED ? new Color(244, 67, 54)
                            : new Color(33, 150, 243);

                    g2d.setColor(playerColor);
                    g2d.fillOval(screenX - 20, screenY - 20, 40, 40);

                    // 해당 플레이어 주위 이펙트 (구버전)
                    drawEffectsFor(g2d, entry.getKey(), screenX, screenY);
                    // 구조화된 SkillEffect 렌더링 (원격 플레이어)
                    skillEffects.drawForPlayer(entry.getKey(), g2d, screenX, screenY);

                    // 이름 표시
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    FontMetrics fm = g2d.getFontMetrics();
                    String name = entry.getKey();
                    int nameWidth = fm.stringWidth(name);
                    g2d.drawString(name, screenX - nameWidth / 2, screenY - 25);

                    // HP 바
                    drawHealthBar(g2d, screenX, screenY + 25, p.hp);
                }
            }

            // 4. 로컬 플레이어 그리기 (화면 좌표)
            int myScreenX = playerX - cameraX;
            int myScreenY = playerY - cameraY;

            Color myColor = team == GameConstants.TEAM_RED ? new Color(255, 100, 100) : new Color(100, 150, 255);
            g2d.setColor(myColor);
            g2d.fillOval(myScreenX - 20, myScreenY - 20, 40, 40);

            // 내 이펙트 (구버전)
            drawMyEffects(g2d);
            // 구조화된 SkillEffect (클래스 기반) 렌더링
            skillEffects.drawSelf(g2d, myScreenX, myScreenY);

            // 내 이름
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            int nameWidth = fm.stringWidth(playerName + " (You)");
            g2d.drawString(playerName + " (You)", myScreenX - nameWidth / 2, myScreenY - 25);

            // 내 HP 바 (플레이어 아래)
            drawHealthBar(g2d, myScreenX, myScreenY + 25, myHP);

            // 조준선 그리기 (화면 좌표 기준)
            drawAimLine(g2d);

            // 5. 미사일 그리기 (화면 좌표로 변환)
            g2d.setColor(Color.YELLOW);
            for (Missile m : missiles) {
                int mScreenX = m.x - cameraX;
                int mScreenY = m.y - cameraY;
                if (isOnScreen(mScreenX, mScreenY)) {
                    g2d.fillOval(mScreenX - 4, mScreenY - 4, 8, 8);
                }
            }

            // 6. 설치된 오브젝트 그리기 (지뢰/터렛)
            for (Map.Entry<Integer, PlacedObjectClient> entry : placedObjects.entrySet()) {
                PlacedObjectClient obj = entry.getValue();
                int objScreenX = obj.x - cameraX;
                int objScreenY = obj.y - cameraY;

                if (isOnScreen(objScreenX, objScreenY)) {
                    // 타입별 색상 및 크기
                    Color objColor;
                    int size;
                    String label;

                    if ("tech_mine".equals(obj.type)) {
                        objColor = obj.team == GameConstants.TEAM_RED ? new Color(200, 50, 50)
                                : new Color(50, 100, 200);
                        size = 16;
                        label = "지뢰";
                    } else if ("tech_turret".equals(obj.type)) {
                        objColor = obj.team == GameConstants.TEAM_RED ? new Color(220, 80, 80)
                                : new Color(80, 120, 220);
                        size = 24;
                        label = "터렛";
                    } else {
                        objColor = Color.GRAY;
                        size = 20;
                        label = "?";
                    }

                    // 오브젝트 본체 그리기 (사각형)
                    g2d.setColor(objColor);
                    g2d.fillRect(objScreenX - size / 2, objScreenY - size / 2, size, size);

                    // 테두리
                    g2d.setColor(obj.team == team ? Color.GREEN : Color.RED);
                    g2d.setStroke(new BasicStroke(2f));
                    g2d.drawRect(objScreenX - size / 2, objScreenY - size / 2, size, size);

                    // HP 바 (오브젝트 위)
                    int barWidth = 30;
                    int barHeight = 4;
                    int barY = objScreenY - size / 2 - 8;

                    g2d.setColor(Color.DARK_GRAY);
                    g2d.fillRect(objScreenX - barWidth / 2, barY, barWidth, barHeight);

                    float hpPercent = (float) obj.hp / obj.maxHp;
                    Color hpColor;
                    if (hpPercent > 0.6f)
                        hpColor = Color.GREEN;
                    else if (hpPercent > 0.3f)
                        hpColor = Color.YELLOW;
                    else
                        hpColor = Color.RED;

                    g2d.setColor(hpColor);
                    int currentBarWidth = (int) (barWidth * hpPercent);
                    g2d.fillRect(objScreenX - barWidth / 2, barY, currentBarWidth, barHeight);

                    // 타입 라벨 (오브젝트 아래)
                    g2d.setFont(new Font("맑은 고딕", Font.BOLD, 10));
                    g2d.setColor(Color.WHITE);
                    FontMetrics objFm = g2d.getFontMetrics();
                    int labelWidth = objFm.stringWidth(label);
                    g2d.drawString(label, objScreenX - labelWidth / 2, objScreenY + size / 2 + 12);

                    // HP 수치 표시
                    String hpText = obj.hp + "/" + obj.maxHp;
                    g2d.setFont(new Font("Arial", Font.PLAIN, 9));
                    objFm = g2d.getFontMetrics();
                    int hpWidth = objFm.stringWidth(hpText);
                    g2d.setColor(new Color(255, 255, 255, 200));
                    g2d.drawString(hpText, objScreenX - hpWidth / 2, barY - 2);
                }
            }

            // ...existing code...

            // === UI 요소는 항상 맨 마지막에 그리기 ===
            // 미니맵 그리기 (우측 상단)
            if (showMinimap) {
                drawMinimap(g2d);
            }

            // HUD
            drawHUD(g2d);

            // 라운드 정보 및 중앙 메시지
            drawRoundInfo(g2d);
        }

        private void drawRoundInfo(Graphics2D g) {
            // 상단 점수판
            int centerX = getWidth() / 2;
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(centerX - 100, 0, 200, 40);

            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.setColor(new Color(255, 100, 100));
            g.drawString(String.valueOf(redWins), centerX - 60, 30);

            g.setColor(Color.WHITE);
            g.drawString(":", centerX, 28);

            g.setColor(new Color(100, 150, 255));
            g.drawString(String.valueOf(blueWins), centerX + 40, 30);

            g.setFont(new Font("맑은 고딕", Font.BOLD, 14));
            g.setColor(Color.WHITE);
            g.drawString("Round " + roundCount, centerX - 30, 55);

            // 중앙 메시지 (카운트다운, 승패 등)
            if (roundState == RoundState.WAITING) {
                long remaining = Math.max(0, ROUND_READY_TIME - (System.currentTimeMillis() - roundStartTime));
                int sec = (int) (remaining / 1000) + 1;
                if (remaining > 0) {
                    drawCenterText(g, "라운드 시작까지 " + sec + "초", 40, Color.YELLOW);
                    drawCenterText(g, "캐릭터를 변경할 수 있습니다 (B키)", 20, Color.WHITE, 50);
                }
            } else if (!centerMessage.isEmpty() && System.currentTimeMillis() < centerMessageEndTime) {
                drawCenterText(g, centerMessage, 40, Color.YELLOW);
            }
        }

        private void drawCenterText(Graphics2D g, String text, int size, Color color) {
            drawCenterText(g, text, size, color, 0);
        }

        private void drawCenterText(Graphics2D g, String text, int size, Color color, int yOffset) {
            g.setFont(new Font("맑은 고딕", Font.BOLD, size));
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() / 2) + yOffset;

            // 그림자
            g.setColor(Color.BLACK);
            g.drawString(text, x + 2, y + 2);

            g.setColor(color);
            g.drawString(text, x, y);
        }
    }

    /**
     * 화면 내에 있는지 체크 (최적화용)
     */
    private boolean isOnScreen(int screenX, int screenY) {
        return screenX >= -50 && screenX <= GameConstants.GAME_WIDTH + 50 &&
                screenY >= -50 && screenY <= GameConstants.GAME_HEIGHT + 50;
    }

    /**
     * 조준선 그리기: 캐릭터 중심에서 마우스 방향으로 (화면 좌표 기준)
     */
    private void drawAimLine(Graphics2D g2d) {
        int myScreenX = playerX - cameraX;
        int myScreenY = playerY - cameraY;

        int vx = mouseX - myScreenX;
        int vy = mouseY - myScreenY;
        if (vx == 0 && vy == 0)
            return; // 마우스가 캐릭터 위치와 같으면 표시 안함

        double len = Math.sqrt(vx * vx + vy * vy);
        double nx = vx / len;
        double ny = vy / len;

        // 조준선 길이 (짧게 조정)
        int lineLength = 50;
        int endX = myScreenX + (int) (nx * lineLength);
        int endY = myScreenY + (int) (ny * lineLength);

        // 반투명 빨간색 선
        g2d.setColor(new Color(255, 0, 0, 100));
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[] { 10f, 5f }, 0f)); // 점선
        g2d.drawLine(myScreenX, myScreenY, endX, endY);
        g2d.setStroke(oldStroke);

        // 마우스 위치에 작은 크로스헤어
        g2d.setColor(new Color(255, 255, 0, 150));
        g2d.drawOval(mouseX - 4, mouseY - 4, 8, 8);
        g2d.drawLine(mouseX - 6, mouseY, mouseX + 6, mouseY);
        g2d.drawLine(mouseX, mouseY - 6, mouseX, mouseY + 6);
    }

    private void drawMyEffects(Graphics2D g2d) {
        if (myEffects.isEmpty())
            return;
        int myScreenX = playerX - cameraX;
        int myScreenY = playerY - cameraY;

        for (ActiveEffect ef : myEffects) {
            float progress = 1f - (ef.remaining / ef.duration);
            int radius = 28 + (int) (Math.sin(progress * 6.28318) * 4);
            int alpha = (int) (160 * (ef.remaining / ef.duration));
            alpha = Math.max(40, Math.min(200, alpha));
            g2d.setColor(new Color(ef.color.getRed(), ef.color.getGreen(), ef.color.getBlue(), alpha));
            Stroke old = g2d.getStroke();
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawOval(myScreenX - radius, myScreenY - radius, radius * 2, radius * 2);
            g2d.setStroke(old);

            // Piper 전용 화려한 효과 강화
            if ("piper_mark".equalsIgnoreCase(ef.abilityId)) {
                // 부드러운 시안 파동 링 두 겹
                g2d.setColor(new Color(100, 220, 255, 90));
                g2d.drawOval(myScreenX - radius - 6, myScreenY - radius - 6, (radius + 6) * 2, (radius + 6) * 2);
                g2d.setColor(new Color(80, 200, 255, 60));
                g2d.drawOval(myScreenX - radius - 12, myScreenY - radius - 12, (radius + 12) * 2,
                        (radius + 12) * 2);
            } else if ("piper_thermal".equalsIgnoreCase(ef.abilityId)) {
                // 주황색 글로우와 회전 아크
                double t = (ef.duration - ef.remaining);
                int glowR = radius + 8;
                g2d.setColor(new Color(255, 160, 40, 110));
                g2d.setStroke(new BasicStroke(4f));
                g2d.drawOval(myScreenX - glowR, myScreenY - glowR, glowR * 2, glowR * 2);
                // 회전 아크
                g2d.setColor(new Color(255, 200, 80, 160));
                int arcStart = (int) ((t * 180) % 360);
                ((Graphics2D) g2d).setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawArc(myScreenX - glowR, myScreenY - glowR, glowR * 2, glowR * 2, arcStart, 60);
            }
        }
    }

    private void drawEffectsFor(Graphics2D g2d, String player, int x, int y) {
        java.util.List<ActiveEffect> list = effectsByPlayer.get(player);
        if (list == null || list.isEmpty())
            return;
        for (ActiveEffect ef : list) {
            float progress = 1f - (ef.remaining / ef.duration);
            int radius = 28 + (int) (Math.sin(progress * 6.28318) * 4);
            int alpha = (int) (160 * (ef.remaining / ef.duration));
            alpha = Math.max(40, Math.min(200, alpha));
            g2d.setColor(new Color(ef.color.getRed(), ef.color.getGreen(), ef.color.getBlue(), alpha));
            Stroke old = g2d.getStroke();
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            g2d.setStroke(old);

            // 원격 Piper 특수 이펙트
            if ("piper_mark".equalsIgnoreCase(ef.abilityId)) {
                g2d.setColor(new Color(100, 220, 255, 80));
                g2d.drawOval(x - radius - 6, y - radius - 6, (radius + 6) * 2, (radius + 6) * 2);
            } else if ("piper_thermal".equalsIgnoreCase(ef.abilityId)) {
                int glowR = radius + 8;
                g2d.setColor(new Color(255, 160, 40, 110));
                g2d.setStroke(new BasicStroke(4f));
                g2d.drawOval(x - glowR, y - glowR, glowR * 2, glowR * 2);
            }
        }
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(30, 35, 45));
        for (int x = 0; x < getWidth(); x += 50) {
            g.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += 50) {
            g.drawLine(0, y, getWidth(), y);
        }
    }

    /**
     * 장애물 디버그 표시 (개발용)
     */
    private void drawObstacles(Graphics2D g2d) {
        if (!debugObstacles)
            return;
        g2d.setColor(new Color(255, 0, 0, 100)); // 반투명 빨간색
        for (Rectangle obs : obstacles) {
            // 맵 좌표를 화면 좌표로 변환
            int screenX = obs.x - cameraX;
            int screenY = obs.y - cameraY;
            g2d.fillRect(screenX, screenY, obs.width, obs.height);
            g2d.setColor(new Color(255, 255, 0, 150));
            g2d.drawRect(screenX, screenY, obs.width, obs.height);
            g2d.setColor(new Color(255, 0, 0, 100));
        }
    }

    /**
     * 에어스트라이크 마커 메인 화면 표시
     */
    private void drawStrikeMarkersMain(Graphics2D g2d) {
        if (strikeMarkers.isEmpty())
            return;

        for (StrikeMarker marker : strikeMarkers.values()) {
            int screenX = marker.x - cameraX;
            int screenY = marker.y - cameraY;

            if (isOnScreen(screenX, screenY)) {
                // 반투명 빨간 원 (경고 영역)
                int radius = 120; // 서버 설정과 동일

                // 펄싱 효과
                long currentTime = System.currentTimeMillis();
                float pulsePhase = (currentTime % 500) / 500f; // 0.5초 주기
                int alpha = (int) (100 + 50 * Math.sin(pulsePhase * Math.PI * 2));

                g2d.setColor(new Color(255, 0, 0, alpha));
                g2d.fillOval(screenX - radius, screenY - radius, radius * 2, radius * 2);

                // 테두리
                g2d.setColor(new Color(255, 0, 0, 200));
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawOval(screenX - radius, screenY - radius, radius * 2, radius * 2);

                // 중앙 십자가
                g2d.setColor(Color.YELLOW);
                g2d.drawLine(screenX - 20, screenY, screenX + 20, screenY);
                g2d.drawLine(screenX, screenY - 20, screenX, screenY + 20);

                // 경고 텍스트
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                String warning = "WARNING!";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(warning, screenX - fm.stringWidth(warning) / 2, screenY - 10);
            }
        }
    }

    private void drawHealthBar(Graphics2D g, int x, int y, int hp) {
        int barWidth = 40;
        int barHeight = 5;

        g.setColor(Color.DARK_GRAY);
        g.fillRect(x - barWidth / 2, y, barWidth, barHeight);

        g.setColor(Color.GREEN);
        int currentWidth = (int) (barWidth * (hp / 100.0));
        g.fillRect(x - barWidth / 2, y, currentWidth, barHeight);
    }

    private void drawMinimap(Graphics2D g2d) {
        // 미니맵 크기 및 위치 설정
        int minimapWidth = 200;
        int minimapHeight = 150;
        // 캔버스 너비 기준으로 배치 (채팅 패널은 캔버스 밖에 있음)
        int minimapX = canvas.getWidth() - minimapWidth - 20;
        int minimapY = 20;

        // 실제 맵 크기 사용
        float scaleX = (float) minimapWidth / GamePanel.this.mapWidth;
        float scaleY = (float) minimapHeight / GamePanel.this.mapHeight;

        // 배경 또는 맵 이미지 그리기
        if (mapImage != null) {
            // 맵 이미지를 축소하여 배경으로 렌더링
            g2d.drawImage(mapImage, minimapX, minimapY, minimapWidth, minimapHeight, null);
        } else {
            // 폴백: 어두운 배경 + 장애물 간단 렌더링
            g2d.setColor(new Color(20, 20, 30, 200));
            g2d.fillRect(minimapX, minimapY, minimapWidth, minimapHeight);
            if (obstacles != null && !obstacles.isEmpty()) {
                g2d.setColor(new Color(200, 60, 60, 180));
                for (Rectangle obs : obstacles) {
                    int ox = minimapX + Math.round(obs.x * scaleX);
                    int oy = minimapY + Math.round(obs.y * scaleY);
                    int ow = Math.max(1, Math.round(obs.width * scaleX));
                    int oh = Math.max(1, Math.round(obs.height * scaleY));
                    g2d.fillRect(ox, oy, ow, oh);
                }
            }
        }

        // 테두리
        g2d.setColor(Color.WHITE);
        g2d.drawRect(minimapX, minimapY, minimapWidth, minimapHeight);

        // 현재 화면(카메라) 뷰포트 표시
        int viewX = minimapX + Math.round(cameraX * scaleX);
        int viewY = minimapY + Math.round(cameraY * scaleY);
        int viewW = Math.max(1, Math.round(GameConstants.GAME_WIDTH * scaleX));
        int viewH = Math.max(1, Math.round(GameConstants.GAME_HEIGHT * scaleY));
        g2d.setColor(new Color(255, 255, 255, 120));
        g2d.drawRect(viewX, viewY, viewW, viewH);

        // 내 위치를 미니맵에 표시
        int myMinimapX = minimapX + (int) (playerX * scaleX);
        int myMinimapY = minimapY + (int) (playerY * scaleY);

        // 내 캐릭터 (노란색, 조금 큰 점)
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(myMinimapX - 4, myMinimapY - 4, 8, 8);
        g2d.setColor(Color.ORANGE);
        g2d.drawOval(myMinimapX - 5, myMinimapY - 5, 10, 10);

        // 시야 범위 원 표시 (반투명)
        int visionRadius = (int) (VISION_RANGE * ((scaleX + scaleY) * 0.5f));
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.fillOval(myMinimapX - visionRadius, myMinimapY - visionRadius,
                visionRadius * 2, visionRadius * 2);
        g2d.setColor(new Color(255, 255, 255, 80));
        g2d.drawOval(myMinimapX - visionRadius, myMinimapY - visionRadius,
                visionRadius * 2, visionRadius * 2);

        // 다른 플레이어들 표시
        boolean thermalActive = (piperThermalRemaining > 0f || teamThermalRemaining > 0f);
        boolean markActive = !thermalActive && (piperMarkRemaining > 0f || teamMarkRemaining > 0f);
        int extendedRadius = (int) (VISION_RANGE * (markActive ? PIPER_MARK_RANGE_FACTOR : 1f));
        synchronized (players) {
            for (PlayerData pd : players.values()) {
                int dx = pd.x - playerX;
                int dy = pd.y - playerY;
                double distance = Math.sqrt(dx * dx + dy * dy);

                // 뷰포트 체크: 적이 현재 화면에 보이는지 확인
                boolean inViewport = (pd.x >= cameraX && pd.x <= cameraX + canvas.getWidth() &&
                        pd.y >= cameraY && pd.y <= cameraY + canvas.getHeight());

                // Piper 스킬(열감지/마크)은 뷰포트 무시, 일반 시야만 뷰포트 체크
                boolean shouldShow = false;
                if (thermalActive) {
                    // 열감지: 전체 맵에서 모든 적 표시 (뷰포트 무시)
                    shouldShow = true;
                } else if (markActive && distance <= extendedRadius) {
                    // 마크: 확장된 범위 내 적 표시 (뷰포트 무시)
                    shouldShow = true;
                } else if (!markActive && !thermalActive && distance <= VISION_RANGE && inViewport) {
                    // 일반 시야: 범위 내 + 화면에 보이는 적만
                    shouldShow = true;
                }

                if (shouldShow) {
                    int otherX = minimapX + (int) (pd.x * scaleX);
                    int otherY = minimapY + (int) (pd.y * scaleY);
                    if (thermalActive) {
                        g2d.setColor(new Color(255, 180, 0));
                        g2d.fillOval(otherX - PIPER_THERMAL_DOT_SIZE / 2, otherY - PIPER_THERMAL_DOT_SIZE / 2,
                                PIPER_THERMAL_DOT_SIZE, PIPER_THERMAL_DOT_SIZE);
                    } else {
                        if (pd.team == GameConstants.TEAM_BLUE) {
                            g2d.setColor(Color.BLUE);
                        } else if (pd.team == GameConstants.TEAM_RED) {
                            g2d.setColor(Color.RED);
                        } else {
                            g2d.setColor(Color.GRAY);
                        }
                        g2d.fillOval(otherX - 3, otherY - 3, 6, 6);
                    }
                }
            }
        }

        // 에어스트라이크 마커 표시 (빨간 십자가)
        for (Map.Entry<Integer, StrikeMarker> entry : strikeMarkers.entrySet()) {
            StrikeMarker marker = entry.getValue();
            int markerX = minimapX + (int) (marker.x * scaleX);
            int markerY = minimapY + (int) (marker.y * scaleY);

            // 빨간 펄싱 원 (경고 효과)
            long currentTime = System.currentTimeMillis();
            float pulsePhase = (currentTime % 1000) / 1000f; // 0~1 반복
            int pulseAlpha = (int) (150 + 105 * Math.sin(pulsePhase * Math.PI * 2));
            g2d.setColor(new Color(255, 0, 0, pulseAlpha));
            g2d.fillOval(markerX - 8, markerY - 8, 16, 16);

            // 빨간 십자가
            g2d.setColor(new Color(255, 255, 0, 255));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawLine(markerX - 10, markerY, markerX + 10, markerY);
            g2d.drawLine(markerX, markerY - 10, markerX, markerY + 10);

            // 경고 텍스트
            g2d.setFont(new Font("Arial", Font.BOLD, 9));
            g2d.setColor(new Color(255, 255, 255, 255));
            g2d.drawString("!", markerX - 3, markerY + 4);
        }

        // 미니맵 레이블
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString("MAP", minimapX + 5, minimapY + 12);
    }

    private void drawHUD(Graphics2D g) {
        // 배경
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(10, 10, 250, 160);

        // 폰트 설정
        g.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        g.setColor(Color.WHITE);

        int yPos = 30;
        g.drawString("플레이어: " + playerName, 20, yPos);
        yPos += 20;
        g.drawString("팀: " + (team == GameConstants.TEAM_RED ? "RED" : "BLUE"), 20, yPos);
        yPos += 20;
        g.drawString("캐릭터: " + (currentCharacterData != null ? currentCharacterData.name : selectedCharacter), 20,
                yPos);
        yPos += 20;

        // HP 바
        g.drawString("HP: " + myHP + "/" + GameConstants.MAX_HP, 20, yPos);
        drawHealthBar(g, 130, yPos - 12, myHP);
        yPos += 20;

        // 킬/데스
        g.setColor(new Color(255, 215, 0)); // 골드
        g.drawString("Kills: " + kills + " / Deaths: " + deaths, 20, yPos);
        yPos += 20;

        // 캐릭터 스탯 표시
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        g.setColor(new Color(255, 200, 200));
        g.drawString("HP: " + (int) currentCharacterData.health, 20, yPos);
        yPos += 18;
        g.setColor(new Color(200, 255, 200));
        g.drawString("속도: " + String.format("%.1f", currentCharacterData.speed), 20, yPos);

        // 스킬 HUD (화면 하단 중앙)
        drawSkillHUD(g);

        // 도움말
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        g.setColor(Color.YELLOW);
        g.drawString("좌클릭: 기본공격 | E: 전술스킬 | R: 궁극기", 20, getHeight() - 40);
        g.drawString("B키: 캐릭터 선택", 20, getHeight() - 20);
    }

    /**
     * 스킬 UI 그리기 (화면 하단 중앙)
     */
    private void drawSkillHUD(Graphics2D g) {
        if (abilities == null)
            return;

        int hudWidth = 400;
        int hudHeight = 80;
        int hudX = (getWidth() - hudWidth) / 2;
        int hudY = getHeight() - hudHeight - 70;

        // 배경
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(hudX, hudY, hudWidth, hudHeight, 10, 10);

        // 각 스킬 박스 그리기
        int skillWidth = 60;
        int skillHeight = 60;
        int skillGap = 20;
        int startX = hudX + (hudWidth - (skillWidth * 3 + skillGap * 2)) / 2;
        int skillY = hudY + 10;

        String[] keyLabels = { "좌클릭", "E", "R" };
        Color[] skillColors = {
                new Color(100, 200, 100), // 기본공격 - 초록
                new Color(100, 150, 255), // 전술스킬 - 파랑
                new Color(255, 100, 100) // 궁극기 - 빨강
        };

        for (int i = 0; i < 3 && i < abilities.length; i++) {
            Ability ability = abilities[i];
            int skillX = startX + i * (skillWidth + skillGap);

            // 스킬 박스 배경
            if (ability.canUse()) {
                g.setColor(skillColors[i]);
            } else {
                g.setColor(new Color(40, 40, 40));
            }
            g.fillRoundRect(skillX, skillY, skillWidth, skillHeight, 8, 8);

            // 테두리 (Piper 활성화 시 컬러)
            boolean piper = "piper".equalsIgnoreCase(selectedCharacter);
            float remain = 0f;
            Color activeBorder = Color.WHITE;
            if (piper) {
                if (i == 1)
                    remain = Math.max(piperMarkRemaining, 0f);
                else if (i == 2)
                    remain = Math.max(piperThermalRemaining, 0f);
                if (remain > 0f) {
                    activeBorder = (i == 1) ? new Color(80, 200, 255) : new Color(255, 160, 40);
                }
            }
            g.setColor(activeBorder);
            g.setStroke(new BasicStroke((piper && remain > 0f) ? 3f : 2f));
            g.drawRoundRect(skillX, skillY, skillWidth, skillHeight, 8, 8);

            // 쿨타임 오버레이 (어둡게)
            if (!ability.canUse()) {
                float cooldownPercent = ability.getCooldownPercent();
                int overlayHeight = (int) (skillHeight * cooldownPercent);
                g.setColor(new Color(0, 0, 0, 160));
                g.fillRoundRect(skillX, skillY + (skillHeight - overlayHeight),
                        skillWidth, overlayHeight, 8, 8);

                // 쿨타임 텍스트
                g.setColor(Color.WHITE);
                g.setFont(new Font("맑은 고딕", Font.BOLD, 16));
                String cooldownText = String.format("%.1f", ability.getCurrentCooldown());
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(cooldownText);
                g.drawString(cooldownText,
                        skillX + (skillWidth - textWidth) / 2,
                        skillY + skillHeight / 2 + 6);
            }

            // 키 라벨
            g.setColor(Color.YELLOW);
            g.setFont(new Font("맑은 고딕", Font.BOLD, 10));
            FontMetrics fm = g.getFontMetrics();
            int labelWidth = fm.stringWidth(keyLabels[i]);
            g.drawString(keyLabels[i],
                    skillX + (skillWidth - labelWidth) / 2,
                    skillY - 5);

            // 스킬 이름 (박스 아래)
            g.setColor(Color.WHITE);
            g.setFont(new Font("맑은 고딕", Font.PLAIN, 10));
            fm = g.getFontMetrics();
            int nameWidth = fm.stringWidth(ability.getName());
            g.drawString(ability.getName(),
                    skillX + (skillWidth - nameWidth) / 2,
                    skillY + skillHeight + 15);

            // Piper 남은 시간 숫자 표시 (활성일 때)
            if ("piper".equalsIgnoreCase(selectedCharacter)
                    && ((i == 1 && piperMarkRemaining > 0f) || (i == 2 && piperThermalRemaining > 0f))) {
                float seconds = (i == 1 ? piperMarkRemaining : piperThermalRemaining);
                String txt = String.format("%.1f s", seconds);
                g.setFont(new Font("맑은 고딕", Font.BOLD, 12));
                g.setColor(Color.WHITE);
                FontMetrics fm2 = g.getFontMetrics();
                int tw = fm2.stringWidth(txt);
                g.drawString(txt, skillX + (skillWidth - tw) / 2, skillY + skillHeight / 2 + 5);
            }
        }
    }

    /** 편집 모드 타일 오버레이 */
    private void drawEditorOverlay(Graphics2D g2d) {
        if (walkableGrid == null)
            return;
        // 반투명 레이어로 전체 walkable/unwalkable 시각화 (간단한 색)
        int startCol = cameraX / TILE_SIZE;
        int startRow = cameraY / TILE_SIZE;
        int endCol = Math.min(gridCols - 1, (cameraX + GameConstants.GAME_WIDTH) / TILE_SIZE + 1);
        int endRow = Math.min(gridRows - 1, (cameraY + GameConstants.GAME_HEIGHT) / TILE_SIZE + 1);

        // 미니맵 영역 계산 (showMinimap이 true일 때만 제외)
        Rectangle minimapRect = null;
        if (showMinimap) {
            int minimapWidth = 200;
            int minimapHeight = 150;
            // 캔버스 기준 좌표 사용
            int minimapX = canvas.getWidth() - minimapWidth - 20;
            int minimapY = 20;
            minimapRect = new Rectangle(minimapX, minimapY, minimapWidth, minimapHeight);
        }

        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                boolean walkable = walkableGrid[r][c];
                int px = c * TILE_SIZE - cameraX;
                int py = r * TILE_SIZE - cameraY;

                // 미니맵이 켜져있으면 미니맵 영역과 겹치는 타일 스킵
                if (minimapRect != null) {
                    Rectangle tileRect = new Rectangle(px, py, TILE_SIZE, TILE_SIZE);
                    if (tileRect.intersects(minimapRect)) {
                        continue;
                    }
                }

                Color base = walkable ? new Color(0, 180, 0, 55) : new Color(180, 0, 0, 60);
                if (isSpawnTile(redSpawnTiles, c, r)) {
                    base = new Color(255, 60, 60, 120);
                } else if (isSpawnTile(blueSpawnTiles, c, r)) {
                    base = new Color(60, 120, 255, 120);
                }
                g2d.setColor(base);
                g2d.fillRect(px, py, TILE_SIZE, TILE_SIZE);
            }
        }
        // 호버 타일 강조
        if (hoverCol >= 0 && hoverRow >= 0 && hoverCol < gridCols && hoverRow < gridRows) {
            int hx = hoverCol * TILE_SIZE - cameraX;
            int hy = hoverRow * TILE_SIZE - cameraY;
            g2d.setColor(new Color(255, 255, 0, 120));
            g2d.drawRect(hx, hy, TILE_SIZE - 1, TILE_SIZE - 1);
        }
        // 모드 안내 텍스트
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        String modeName;
        switch (editPaintMode) {
            case 0 -> modeName = "이동 가능";
            case 1 -> modeName = "이동 불가";
            case 2 -> modeName = "RED 스폰";
            case 3 -> modeName = "BLUE 스폰";
            default -> modeName = "?";
        }
        g2d.drawString("[EDIT MODE] 1=Walk 2=Block 3=RedSpawn 4=BlueSpawn | 현재=" + modeName + " | 좌클릭/드래그 | F4 종료",
                10, 20);
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.drawString("저장: Ctrl+S / F5", 10, 35);
    }

    public GamePanel(String playerName, int team, Socket socket, DataOutputStream out, DataInputStream in,
            String characterId) {
        super("FPS Game - " + playerName);
        this.playerName = playerName;
        this.team = team;
        this.socket = socket;
        this.out = out;
        this.in = in;

        // 전달받은 캐릭터 ID 사용 (null이면 기본값)
        this.selectedCharacter = (characterId != null && !characterId.isEmpty()) ? characterId : "raven";
        this.currentCharacterData = CharacterData.getById(selectedCharacter);

        // 스킬 초기화
        this.abilities = CharacterData.createAbilities(selectedCharacter);

        // 맵 로드
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
        chatPanel.setPreferredSize(new Dimension(250, GameConstants.GAME_HEIGHT));
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
            try {
                // 서버로 전송 (로컬 에코 제거 - 서버 응답만 표시)
                out.writeUTF("CHAT:" + message);
                out.flush();
            } catch (IOException ex) {
                appendChatMessage("채팅 전송 실패");
            }
            chatInput.setText("");
        }
        // 채팅 전송 후 게임 캔버스로 포커스 복귀
        canvas.requestFocusInWindow();
    }

    // 채팅/시스템 로그 스로틀링
    private String lastChatMessage = null;
    private long lastChatTime = 0L;
    private static final long CHAT_THROTTLE_MS = 1000;

    private void appendChatMessage(String message) {
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
    private void loadMap(String mapName) {
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
        } catch (Exception e) {
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

        // 서버 메시지 수신 스레드
        new Thread(this::receiveGameUpdates).start();
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
            if (m.team == team) {
                // 적 플레이어와 충돌 체크
                boolean hit = false;
                for (Map.Entry<String, PlayerData> entry : players.entrySet()) {
                    PlayerData p = entry.getValue();
                    if (p.team != team) {
                        double dist = Math.sqrt(Math.pow(m.x - p.x, 2) + Math.pow(m.y - p.y, 2));
                        if (dist < 20) {
                            it.remove();
                            hit = true;
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
                                try {
                                    out.writeUTF("HIT_OBJ:" + obj.id);
                                    out.flush();
                                } catch (IOException ex) {
                                    appendChatMessage("[네트워크] 오브젝트 피격 전송 실패");
                                }
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
                    try {
                        if (m.owner != null) {
                            out.writeUTF("HITME:" + m.owner);
                        } else {
                            out.writeUTF("DEATH");
                        }
                        out.flush();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    private void respawn() {
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
        myHP = GameConstants.MAX_HP;

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
            try {
                out.writeUTF("POS:" + playerX + "," + playerY);
                out.flush();
            } catch (IOException ex) {
                appendChatMessage("[에러] 위치 전송 실패: " + ex.getMessage());
            }
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

    private void receiveGameUpdates() {
        try {
            String message;
            while ((message = in.readUTF()) != null) {
                processGameMessage(message);
            }
        } catch (IOException ex) {
            System.out.println("서버와의 연결이 끊어졌습니다.");
        }
    }

    private void processGameMessage(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length < 2)
            return;

        String command = parts[0];
        String data = parts[1];

        switch (command) {
            case "CHAT":
                // 채팅 메시지 표시 (서버에서 받은 모든 메시지 표시)
                appendChatMessage(data);
                break;

            case "PLAYER":
                // PLAYER:name,x,y,team,hp
                String[] playerData = data.split(",");
                if (playerData.length >= 5) {
                    String name = playerData[0];
                    if (!name.equals(playerName)) {
                        int x = (int) Float.parseFloat(playerData[1]);
                        int y = (int) Float.parseFloat(playerData[2]);
                        int t = Integer.parseInt(playerData[3]);
                        int hp = Integer.parseInt(playerData[4]);

                        PlayerData pd = players.get(name);
                        if (pd == null) {
                            pd = new PlayerData(x, y, t);
                            pd.hp = hp; // 새 플레이어도 HP 설정
                            players.put(name, pd);
                        } else {
                            // 보간을 위해 목표 위치 설정 (즉시 이동하지 않음)
                            pd.targetX = x;
                            pd.targetY = y;
                            pd.hp = hp;
                        }
                    }
                }
                break;

            case "REMOVE":
                // 플레이어 제거
                players.remove(data);
                break;

            case "KILL":
                // 처치 알림(스탯 증가는 서버에서 STATS로 동기화)
                appendChatMessage(">>> 당신이 " + data + "를 처치했습니다!");
                break;

            case "STATS":
                // STATS:name,kills,deaths,hp
                String[] s = data.split(",");
                if (s.length >= 4) {
                    String name = s[0];
                    int k = Integer.parseInt(s[1]);
                    int d = Integer.parseInt(s[2]);
                    int hp = Integer.parseInt(s[3]);
                    if (name.equals(playerName)) {
                        kills = k;
                        deaths = d;
                        myHP = hp;
                        if (myHP <= 0) {
                            // 서버 기준으로 사망 상태면 즉시 리스폰
                            respawn();
                        }
                    } else {
                        PlayerData pd = players.get(name);
                        if (pd != null) {
                            pd.kills = k;
                            pd.deaths = d;
                            pd.hp = hp;
                        }
                    }
                }
                break;

            case "SHOOT":
                // SHOOT:playerName,x,y,dx,dy
                String[] shootData = data.split(",");
                if (shootData.length >= 5) {
                    String shooter = shootData[0];
                    if (!shooter.equals(playerName)) {
                        int sx = (int) Float.parseFloat(shootData[1]);
                        int sy = (int) Float.parseFloat(shootData[2]);
                        int dx = (int) Float.parseFloat(shootData[3]);
                        int dy = (int) Float.parseFloat(shootData[4]);

                        // 다른 플레이어가 발사한 미사일 생성
                        PlayerData pd = players.get(shooter);
                        if (pd != null) {
                            Missile m = new Missile(sx, sy, dx, dy, pd.team, shooter);
                            missiles.add(m);
                            // 원격 총구 섬광 이펙트 (발사 방향 기반)
                            double ang = Math.atan2(dy, dx);
                            skillEffects.addForPlayer(shooter, new MuzzleFlashEffect(ang));
                        }
                    }
                }
                break;

            case "SKILL": {
                // SKILL:playerName,abilityId,type,duration
                String[] sd = data.split(",");
                if (sd.length >= 4) {
                    String user = sd[0];
                    String abilityId = sd[1];
                    String type = sd[2];
                    float duration = 0.4f;
                    try {
                        duration = Float.parseFloat(sd[3]);
                    } catch (NumberFormatException ignored) {
                    }
                    if (!user.equals(playerName)) {
                        effectsByPlayer.computeIfAbsent(user, k -> new ArrayList<>())
                                .add(new ActiveEffect(abilityId, type, Math.max(0.2f, duration)));
                        // 구조화된 SkillEffect 등록 (원격 플레이어 시각 효과)
                        if ("piper_mark".equals(abilityId)) {
                            skillEffects.addForPlayer(user, new PiperMarkEffect(duration));
                        } else if ("piper_thermal".equals(abilityId)) {
                            skillEffects.addForPlayer(user, new PiperThermalEffect(duration));
                        } else if ("raven_dash".equals(abilityId)) {
                            skillEffects.addForPlayer(user, new RavenDashEffect(duration));
                        } else if ("raven_overcharge".equals(abilityId)) {
                            skillEffects.addForPlayer(user, new RavenOverchargeEffect(duration));
                        } else if ("gen_aura".equals(abilityId)) {
                            skillEffects.addForPlayer(user, new GeneralAuraEffect(duration));
                        } else if ("gen_strike".equals(abilityId)) {
                            skillEffects.addForPlayer(user, new GeneralStrikeEffect(duration));
                            // } else if ("bull_cover".equals(abilityId)) {
                            // skillEffects.addForPlayer(user, new BulldogCoverEffect(duration));
                            // } else if ("bull_barrage".equals(abilityId)) {
                            // skillEffects.addForPlayer(user, new BulldogBarrageEffect(duration));
                            // } else if ("wild_breach".equals(abilityId)) {
                            // skillEffects.addForPlayer(user, new WildcatBreachEffect(duration));
                            // } else if ("wild_berserk".equals(abilityId)) {
                            // skillEffects.addForPlayer(user, new WildcatBerserkEffect(duration));
                        } else if ("ghost_cloak".equals(abilityId)) {
                            skillEffects.addForPlayer(user, new GhostCloakEffect(duration));
                        } else if ("ghost_nullify".equals(abilityId)) {
                            skillEffects.addForPlayer(user, new GhostNullifyEffect(duration));
                        } else if ("skull_adrenaline".equals(abilityId)) {
                            skillEffects.addForPlayer(user, new SkullAdrenalineEffect(duration));
                        } else if ("skull_ammo".equals(abilityId)) {
                            skillEffects.addForPlayer(user, new SkullAmmoEffect(duration));
                            // } else if ("steam_emp".equals(abilityId)) {
                            // skillEffects.addForPlayer(user, new SteamEmpEffect(duration));
                            // } else if ("steam_reset".equals(abilityId)) {
                            // skillEffects.addForPlayer(user, new SteamResetEffect(duration));
                        } else if ("tech_mine".equals(abilityId)) {
                            skillEffects.addForPlayer(user, new TechMineEffect(duration));
                        } else if ("tech_turret".equals(abilityId)) {
                            skillEffects.addForPlayer(user, new TechTurretEffect(duration));
                        } else if ("sage_heal".equals(abilityId)) {
                            skillEffects.addForPlayer(user, new SageHealEffect(duration));
                        } else if ("sage_revive".equals(abilityId)) {
                            skillEffects.addForPlayer(user, new SageReviveEffect(duration));
                        }
                        // 같은 팀 Piper 마킹/열감지 공유
                        PlayerData pdUser = players.get(user);
                        if (pdUser != null && pdUser.team == team) {
                            if ("piper_mark".equalsIgnoreCase(abilityId)) {
                                teamMarkRemaining = Math.max(teamMarkRemaining, duration);
                            } else if ("piper_thermal".equalsIgnoreCase(abilityId)) {
                                teamThermalRemaining = Math.max(teamThermalRemaining, duration);
                            }
                        }
                    }
                }
                break;
            }
            case "TURRET_SHOOT": {
                // TURRET_SHOOT:objId,tx,ty,targetName
                String[] ts = data.split(",");
                if (ts.length >= 4) {
                    try {
                        int turretId = Integer.parseInt(ts[0]);
                        int tx = Integer.parseInt(ts[1]);
                        int ty = Integer.parseInt(ts[2]);
                        String targetName = ts[3];
                        PlacedObjectClient turret = placedObjects.get(turretId);
                        // 타겟 플레이어 객체가 없어도(예: 나 자신, 혹은 시야 밖) 좌표 기반으로 발사
                        if (turret != null) {
                            // Turret fires a missile toward target
                            int sx = turret.x;
                            int sy = turret.y;
                            int dx = tx - sx;
                            int dy = ty - sy;
                            double distance = Math.sqrt(dx * dx + dy * dy);
                            if (distance > 0) {
                                int speed = 8;
                                int missileVx = (int) (dx / distance * speed);
                                int missileVy = (int) (dy / distance * speed);
                                Missile m = new Missile(sx, sy, missileVx, missileVy, turret.team, "TURRET");
                                missiles.add(m);
                            }
                            // Optional: turret muzzle flash effect
                            double ang = Math.atan2(dy, dx);
                            if (skillEffects != null)
                                skillEffects.addForObject(turretId, new TurretShootEffect(ang));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                break;
            }

            case "PLACE": {
                // PLACE:objId,type,x,y,hp,maxHp,owner,team
                String[] placeParts = data.split(",");
                if (placeParts.length >= 8) {
                    try {
                        int id = Integer.parseInt(placeParts[0]);
                        String type = placeParts[1];
                        int x = Integer.parseInt(placeParts[2]);
                        int y = Integer.parseInt(placeParts[3]);
                        int hp = Integer.parseInt(placeParts[4]);
                        int maxHp = Integer.parseInt(placeParts[5]);
                        String owner = placeParts[6];
                        int objTeam = Integer.parseInt(placeParts[7]);

                        PlacedObjectClient obj = new PlacedObjectClient(id, type, x, y, hp, maxHp, owner, objTeam);
                        placedObjects.put(id, obj);
                        appendChatMessage("[오브젝트] " + owner + " 님이 " + type + " 설치!");
                    } catch (NumberFormatException ignored) {
                    }
                }
                break;
            }

            case "OBJ_UPDATE": {
                // OBJ_UPDATE:objId,hp
                String[] updateParts = data.split(",");
                if (updateParts.length >= 2) {
                    try {
                        int id = Integer.parseInt(updateParts[0]);
                        int hp = Integer.parseInt(updateParts[1]);
                        PlacedObjectClient obj = placedObjects.get(id);
                        if (obj != null) {
                            obj.hp = hp;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                break;
            }

            case "OBJ_DESTROY": {
                // OBJ_DESTROY:objId
                try {
                    int id = Integer.parseInt(data);
                    PlacedObjectClient obj = placedObjects.remove(id);
                    if (obj != null) {
                        appendChatMessage("[오브젝트] " + obj.type + " 파괴됨!");
                    }
                } catch (NumberFormatException ignored) {
                }
                break;
            }

            case "BUFF": {
                // BUFF:targetName,abilityId,moveSpeedMult,attackSpeedMult,durationRemaining
                String[] buffParts = data.split(",");
                if (buffParts.length >= 5 && buffParts[0].equals(playerName)) {
                    try {
                        moveSpeedMultiplier = Float.parseFloat(buffParts[2]);
                        attackSpeedMultiplier = Float.parseFloat(buffParts[3]);

                        // 공격속도 버프를 abilities[0] 쿨다운에 적용 (attackSpeedMultiplier가 1.15라면 쿨다운은 1/1.15 =
                        // 0.87로 단축)
                        if (abilities != null && abilities.length > 0) {
                            abilities[0].setCooldownMultiplier(1f / attackSpeedMultiplier);
                        }

                        appendChatMessage("[버프] 오라 버프 활성! (이동속도 +" + ((moveSpeedMultiplier - 1) * 100) + "%, 공격속도 +"
                                + ((attackSpeedMultiplier - 1) * 100) + "%)");
                    } catch (NumberFormatException ignored) {
                    }
                }
                break;
            }

            case "UNBUFF": {
                // UNBUFF:abilityId
                if ("gen_aura".equals(data)) {
                    moveSpeedMultiplier = 1.0f;
                    attackSpeedMultiplier = 1.0f;

                    // 공격속도 버프 해제 (쿨다운 배율 원상복구)
                    if (abilities != null && abilities.length > 0) {
                        abilities[0].setCooldownMultiplier(1f);
                    }

                    appendChatMessage("[버프] 오라 버프 종료");
                }
                break;
            }

            case "STRIKE_MARK": {
                // STRIKE_MARK:strikeId,x,y
                String[] markParts = data.split(",");
                if (markParts.length >= 3) {
                    try {
                        int id = Integer.parseInt(markParts[0]);
                        int x = Integer.parseInt(markParts[1]);
                        int y = Integer.parseInt(markParts[2]);
                        StrikeMarker marker = new StrikeMarker(id, x, y);
                        strikeMarkers.put(id, marker);
                        appendChatMessage("[에어스트라이크] 타겟 지정됨!");
                    } catch (NumberFormatException ignored) {
                    }
                }
                break;
            }

            case "STRIKE_IMPACT": {
                // STRIKE_IMPACT:strikeId,x,y,radius
                String[] impactParts = data.split(",");
                if (impactParts.length >= 4) {
                    try {
                        int id = Integer.parseInt(impactParts[0]);
                        int x = Integer.parseInt(impactParts[1]);
                        int y = Integer.parseInt(impactParts[2]);
                        int radius = Integer.parseInt(impactParts[3]);

                        strikeMarkers.remove(id);
                        appendChatMessage("[에어스트라이크] 임팩트! (" + x + "," + y + ")");

                        // TODO: 임팩트 시각 효과 (폭발 애니메이션)
                    } catch (NumberFormatException ignored) {
                    }
                }
                break;
            }

            case "ROUND_WIN": {
                // ROUND_WIN:winningTeam,redWins,blueWins
                String[] roundData = data.split(",");
                if (roundData.length >= 3) {
                    int winningTeam = Integer.parseInt(roundData[0]);
                    redWins = Integer.parseInt(roundData[1]);
                    blueWins = Integer.parseInt(roundData[2]);

                    String winTeam = (winningTeam == GameConstants.TEAM_RED) ? "RED" : "BLUE";
                    centerMessage = winTeam + " 팀 승리!";
                    centerMessageEndTime = System.currentTimeMillis() + 2000;
                    roundState = RoundState.ENDED;

                    appendChatMessage("[라운드] " + winTeam + " 팀 승리! (점수: " + redWins + " : " + blueWins + ")");
                }
                break;
            }

            case "ROUND_START": {
                // ROUND_START:roundNumber,mapId
                String[] roundParts = data.split(",");
                if (roundParts.length > 0) {
                    roundCount = Integer.parseInt(roundParts[0]);

                    // 맵 ID가 포함되어 있으면 새 맵 로드
                    if (roundParts.length > 1) {
                        String newMapId = roundParts[1];
                        if (!newMapId.equals(currentMapName)) {
                            currentMapName = newMapId;
                            loadMap(newMapId);
                            appendChatMessage("[맵] " + newMapId + " 맵으로 변경되었습니다!");
                        }
                    }
                    roundState = RoundState.WAITING;
                    roundStartTime = System.currentTimeMillis();
                    centerMessage = "Round " + roundCount + " Ready";
                    centerMessageEndTime = roundStartTime + ROUND_READY_TIME;

                    // 라운드 시작 시 캐릭터 변경 플래그 초기화
                    hasChangedCharacterInRound = false;

                    // 라운드 시작 시 오브젝트 및 효과 초기화
                    placedObjects.clear();
                    strikeMarkers.clear();
                    if (effectsByPlayer != null) {
                        effectsByPlayer.clear();
                    }
                    // skillEffects는 별도 초기화 필요 시 추가

                    // 리스폰
                    respawn();

                    appendChatMessage("[라운드] Round " + roundCount + " 시작!");
                }
                break;
            }

            case "CHARACTER_SELECT": {
                // CHARACTER_SELECT:playerName,characterId
                String[] cs = data.split(",");
                if (cs.length >= 2) {
                    String pName = cs[0];
                    String charId = cs[1];

                    if (pName.equals(playerName)) {
                        selectedCharacter = charId;
                        hasChangedCharacterInRound = true; // 내 캐릭터 변경 기록
                        appendChatMessage("[시스템] 캐릭터를 " + charId + "(으)로 변경했습니다.");
                    } else {
                        appendChatMessage("[시스템] " + pName + "님이 캐릭터를 변경했습니다.");
                    }
                }
                break;
            }

            case "GAME_OVER": {
                // GAME_OVER:winningTeamName
                centerMessage = data + " 팀 최종 승리!";
                centerMessageEndTime = System.currentTimeMillis() + 5000;

                appendChatMessage("========================================");
                appendChatMessage("[게임 종료] " + data + " 팀이 최종 승리했습니다!");
                appendChatMessage("========================================");

                // 5초 후 로비로 복귀
                javax.swing.Timer returnTimer = new javax.swing.Timer(5000, e -> {
                    returnToLobby();
                });
                returnTimer.setRepeats(false);
                returnTimer.start();
                break;
            }
        }
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
            ex.printStackTrace();
        }
    }

    /**
     * 로비로 복귀
     */
    /**
     * 로비로 복귀
     */
    private void returnToLobby() {
        // 게임 종료
        if (timer != null) {
            timer.stop();
        }
        disconnect();

        // 현재 창 닫기 및 로비 열기
        javax.swing.SwingUtilities.invokeLater(() -> {
            this.dispose(); // GamePanel이 JFrame이므로 직접 dispose

            // 로비 프레임 열기
            new LobbyFrame(playerName).setVisible(true);
        });
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
        } catch (Exception ex) {
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
                if ("raven".equalsIgnoreCase(selectedCharacter)) {
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
                if ("raven".equalsIgnoreCase(selectedCharacter)) {
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
        if ("piper".equalsIgnoreCase(selectedCharacter)) {
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
            try {
                // abilityId,type,duration[,x,y]
                Ability ability = (abilities != null && skillIndex < abilities.length) ? abilities[skillIndex] : null;
                float dur = (ability != null) ? ability.getActiveDuration() : 0f;
                String abilityId = (ability != null) ? ability.id : ("skill" + skillIndex);
                String msg = "SKILL:" + abilityId + "," + skillType + "," + dur;
                if (targetX >= 0 && targetY >= 0) {
                    msg += "," + targetX + "," + targetY;
                }
                out.writeUTF(msg);
                out.flush();
            } catch (IOException ex) {
                appendChatMessage("스킬 전송 실패: " + ex.getMessage());
            }
        }
    }

    // 라운드 시작
    private void startRound() {
        roundState = RoundState.WAITING;
        roundCount++;
        roundStartTime = System.currentTimeMillis();
        centerMessage = "Round " + roundCount + " Ready";
        centerMessageEndTime = roundStartTime + ROUND_READY_TIME;

        // 랜덤 맵 선택 (첫 라운드 제외, 혹은 매 라운드)
        if (mapCycle != null && !mapCycle.isEmpty()) {
            int nextIdx = (int) (Math.random() * mapCycle.size());
            switchMap(mapCycle.get(nextIdx));
        }

        respawn(); // 전원 리스폰 위치로
    }

    private void openCharacterSelect() {
        // 라운드 시작 후 10초(대기시간) 지나면 변경 불가
        if (roundState != RoundState.WAITING) {
            appendChatMessage("[시스템] 라운드 진행 중에는 캐릭터를 변경할 수 없습니다.");
            return;
        }

        // 게임 일시정지
        if (timer != null) {
            timer.stop();
        }

        // 캐릭터 선택 다이얼로그 표시
        String newCharacter = CharacterSelectDialog.showDialog(this);

        if (newCharacter != null) {
            selectedCharacter = newCharacter;
            currentCharacterData = CharacterData.getById(selectedCharacter);

            // 스킬 재로드
            abilities = CharacterData.createAbilities(selectedCharacter);

            // 서버에 캐릭터 변경 알림
            if (out != null) {
                try {
                    out.writeUTF("CHARACTER:" + selectedCharacter);
                    out.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            appendChatMessage("캐릭터를 " + currentCharacterData.name + "으로 변경했습니다.");

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
}
