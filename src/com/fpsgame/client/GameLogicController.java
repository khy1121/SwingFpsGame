package com.fpsgame.client;

import com.fpsgame.common.GameConstants;
import java.awt.Rectangle;
import java.util.List;

/**
 * 플레이어 이동, 충돌 감지, 라운드 관리를 담당하는 클래스
 * GamePanel에서 분리된 GameLogicController
 */
public class GameLogicController {
    
    // 플레이어 상태
    private int playerX = 400;
    private int playerY = 300;
    private final int SPEED = 5;
    
    // 방향
    private int myDirection = 0; // 0:Down, 1:Up, 2:Left, 3:Right
    
    // 라운드 시스템
    public enum RoundState {
        WAITING, PLAYING, ENDED
    }
    
    private RoundState roundState = RoundState.WAITING;
    private int roundCount = 0;
    private int redWins = 0;
    private int blueWins = 0;
    private long roundStartTime = 0;
    public static final int ROUND_READY_TIME = 10000; // 10초
    
    private String centerMessage = "";
    private long centerMessageEndTime = 0;
    
    // 캐릭터 선택 제한
    private boolean hasChangedCharacterInRound = false;
    private static final long CHARACTER_CHANGE_TIME_LIMIT = 10000;
    
    // 버프 상태
    private float moveSpeedMultiplier = 1.0f;
    
    // 맵 참조 (충돌 검사용)
    private boolean[][] walkableGrid;
    private List<Rectangle> obstacles;
    private int mapWidth;
    private int mapHeight;
    
    // 콜백
    private final MessageCallback messageCallback;
    
    @FunctionalInterface
    public interface MessageCallback {
        void appendMessage(String message);
    }
    
    public GameLogicController(MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }
    
    // ==================== Getters/Setters ====================
    
    public int getPlayerX() { return playerX; }
    public void setPlayerX(int x) { this.playerX = x; }
    
    public int getPlayerY() { return playerY; }
    public void setPlayerY(int y) { this.playerY = y; }
    
    public int getMyDirection() { return myDirection; }
    public void setMyDirection(int dir) { this.myDirection = dir; }
    
    public RoundState getRoundState() { return roundState; }
    public void setRoundState(RoundState state) { this.roundState = state; }
    
    public int getRoundCount() { return roundCount; }
    public void setRoundCount(int count) { this.roundCount = count; }
    
    public int getRedWins() { return redWins; }
    public void setRedWins(int wins) { this.redWins = wins; }
    
    public int getBlueWins() { return blueWins; }
    public void setBlueWins(int wins) { this.blueWins = wins; }
    
    public long getRoundStartTime() { return roundStartTime; }
    public void setRoundStartTime(long time) { this.roundStartTime = time; }
    
    public String getCenterMessage() { return centerMessage; }
    public void setCenterMessage(String msg) { this.centerMessage = msg; }
    
    public long getCenterMessageEndTime() { return centerMessageEndTime; }
    public void setCenterMessageEndTime(long time) { this.centerMessageEndTime = time; }
    
    public boolean hasChangedCharacterInRound() { return hasChangedCharacterInRound; }
    public void setHasChangedCharacterInRound(boolean value) { this.hasChangedCharacterInRound = value; }
    
    public float getMoveSpeedMultiplier() { return moveSpeedMultiplier; }
    public void setMoveSpeedMultiplier(float mult) { this.moveSpeedMultiplier = mult; }
    
    public void setMapData(boolean[][] walkableGrid, List<Rectangle> obstacles, int mapWidth, int mapHeight) {
        this.walkableGrid = walkableGrid;
        this.obstacles = obstacles;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }
    
    // ==================== 플레이어 이동 ====================
    
    /**
     * 플레이어 위치 업데이트 (키 입력 기반)
     */
    public void updatePlayerPosition(boolean[] keys) {
        int newX = playerX;
        int newY = playerY;
        int speed = (int) (SPEED * moveSpeedMultiplier);
        
        boolean moved = false;
        
        if (keys['W'] || keys['w']) {
            newY -= speed;
            myDirection = 1; // Up
            moved = true;
        }
        if (keys['S'] || keys['s']) {
            newY += speed;
            myDirection = 0; // Down
            moved = true;
        }
        if (keys['A'] || keys['a']) {
            newX -= speed;
            myDirection = 2; // Left
            moved = true;
        }
        if (keys['D'] || keys['d']) {
            newX += speed;
            myDirection = 3; // Right
            moved = true;
        }
        
        if (moved) {
            // 맵 경계 체크
            newX = Math.max(20, Math.min(newX, mapWidth - 20));
            newY = Math.max(20, Math.min(newY, mapHeight - 20));
            
            // 장애물 충돌 체크
            if (!checkCollisionWithObstacles(newX, newY) && isPositionWalkable(newX, newY)) {
                playerX = newX;
                playerY = newY;
            }
        }
    }
    
    /**
     * 장애물 충돌 체크
     */
    public boolean checkCollisionWithObstacles(int x, int y) {
        if (obstacles == null) return false;
        
        Rectangle playerBounds = new Rectangle(x - 20, y - 20, 40, 40);
        for (Rectangle obs : obstacles) {
            if (playerBounds.intersects(obs)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 플레이어 반경을 샘플링하여 해당 위치가 모두 walkable인지 확인
     */
    public boolean isPositionWalkable(int x, int y) {
        if (walkableGrid == null) return true;
        
        int tileSize = 32;
        int gridCols = mapWidth / tileSize;
        int gridRows = mapHeight / tileSize;
        
        int[] offsets = {-15, 0, 15};
        for (int dx : offsets) {
            for (int dy : offsets) {
                int checkX = x + dx;
                int checkY = y + dy;
                
                int col = checkX / tileSize;
                int row = checkY / tileSize;
                
                if (row < 0 || row >= gridRows || col < 0 || col >= gridCols) {
                    return false;
                }
                
                if (!walkableGrid[row][col]) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    // ==================== 스폰 ====================
    
    /**
     * 스폰 위치 설정
     */
    public void setSpawnPosition(int x, int y) {
        playerX = x;
        playerY = y;
    }
    
    /**
     * 리스폰
     */
    public void respawn(int team, Rectangle redSpawnZone, Rectangle blueSpawnZone, 
                       List<int[]> redSpawnTiles, List<int[]> blueSpawnTiles) {
        Rectangle spawnZone = (team == GameConstants.TEAM_RED) ? redSpawnZone : blueSpawnZone;
        List<int[]> spawnTiles = (team == GameConstants.TEAM_RED) ? redSpawnTiles : blueSpawnTiles;
        
        if (spawnTiles != null && !spawnTiles.isEmpty()) {
            int[] tile = spawnTiles.get((int) (Math.random() * spawnTiles.size()));
            playerX = tile[0] * 32 + 16;
            playerY = tile[1] * 32 + 16;
        } else if (spawnZone != null) {
            int minX = spawnZone.x + 20;
            int maxX = spawnZone.x + spawnZone.width - 20;
            int minY = spawnZone.y + 20;
            int maxY = spawnZone.y + spawnZone.height - 20;
            
            playerX = minX + (int) (Math.random() * (maxX - minX));
            playerY = minY + (int) (Math.random() * (maxY - minY));
        } else {
            playerX = (team == GameConstants.TEAM_RED) ? 200 : (mapWidth - 200);
            playerY = mapHeight / 2;
        }
        
        // 위치 검증
        playerX = Math.max(20, Math.min(playerX, mapWidth - 20));
        playerY = Math.max(20, Math.min(playerY, mapHeight - 20));
    }
    
    // ==================== 라운드 관리 ====================
    
    /**
     * 라운드 시작
     */
    public void startRound() {
        roundState = RoundState.WAITING;
        roundStartTime = System.currentTimeMillis();
        centerMessage = "라운드 " + (roundCount + 1) + " 준비...";
        centerMessageEndTime = System.currentTimeMillis() + ROUND_READY_TIME;
        hasChangedCharacterInRound = false;
    }
    
    /**
     * 라운드 상태 업데이트
     */
    public void updateRoundState() {
        long now = System.currentTimeMillis();
        
        if (roundState == RoundState.WAITING) {
            if (now >= roundStartTime + ROUND_READY_TIME) {
                roundState = RoundState.PLAYING;
                centerMessage = "";
            }
        }
        
        // 중앙 메시지 만료
        if (now > centerMessageEndTime) {
            centerMessage = "";
        }
    }
    
    /**
     * 캐릭터 변경 가능 여부 확인
     */
    public boolean canChangeCharacter() {
        if (hasChangedCharacterInRound) {
            return false;
        }
        
        if (roundState != RoundState.WAITING) {
            return false;
        }
        
        long elapsed = System.currentTimeMillis() - roundStartTime;
        return elapsed <= CHARACTER_CHANGE_TIME_LIMIT;
    }
    
    /**
     * 라운드 승리 처리
     */
    public void handleRoundWin(int winningTeam) {
        roundState = RoundState.ENDED;
        
        if (winningTeam == GameConstants.TEAM_RED) {
            redWins++;
            centerMessage = "RED 팀 승리!";
        } else {
            blueWins++;
            centerMessage = "BLUE 팀 승리!";
        }
        
        centerMessageEndTime = System.currentTimeMillis() + 3000;
        
        if (messageCallback != null) {
            messageCallback.appendMessage("[라운드] " + centerMessage);
        }
    }
    
    /**
     * 게임 종료 처리
     */
    public void handleGameOver(String winningTeam) {
        centerMessage = winningTeam + " 팀이 게임에서 승리했습니다!";
        centerMessageEndTime = System.currentTimeMillis() + 10000;
        
        if (messageCallback != null) {
            messageCallback.appendMessage("[게임 종료] " + centerMessage);
        }
    }
    
    /**
     * 게임 상태 리셋
     */
    public void resetGame() {
        roundState = RoundState.WAITING;
        roundCount = 0;
        redWins = 0;
        blueWins = 0;
        roundStartTime = 0;
        centerMessage = "";
        centerMessageEndTime = 0;
        hasChangedCharacterInRound = false;
        moveSpeedMultiplier = 1.0f;
    }
}
