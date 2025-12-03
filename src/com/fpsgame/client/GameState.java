package com.fpsgame.client;

import com.fpsgame.common.Ability;
import com.fpsgame.common.CharacterData;
import com.fpsgame.common.GameConstants;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * GameState - 게임 상태 관리 클래스
 * GamePanel에서 분리하여 게임 로직과 상태를 관리
 */
public class GameState {
    
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
    
    // 캐릭터별 런타임 상태 - Raven
    private float ravenDashRemaining = 0f;
    private float ravenOverchargeRemaining = 0f;
    private float missileSpeedMultiplier = 1f;
    
    // 캐릭터별 런타임 상태 - Piper
    private float piperMarkRemaining = 0f;
    private float piperThermalRemaining = 0f;
    private float teamMarkRemaining = 0f;
    private float teamThermalRemaining = 0f;
    
    // 버프 상태
    private float moveSpeedMultiplier = 1.0f;
    private float attackSpeedMultiplier = 1.0f;
    
    // 스킬 이펙트 시스템
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
    
    // 다른 플레이어들
    private Map<String, PlayerData> players = new HashMap<>();
    
    // 미사일 리스트
    private final List<Missile> missiles = new ArrayList<>();
    
    // 설치된 오브젝트 (지뢰, 터렛)
    private final Map<Integer, PlacedObjectClient> placedObjects = new HashMap<>();
    
    // 에어스트라이크 마커
    private final Map<Integer, StrikeMarker> strikeMarkers = new HashMap<>();
    
    // 맵 시스템
    private int mapWidth = 3200;
    private int mapHeight = 2400;
    private int cameraX = 0;
    private int cameraY = 0;
    private String currentMapName = "map";
    private boolean[][] walkableGrid;
    private int gridCols, gridRows;
    private Rectangle redSpawnZone, blueSpawnZone;
    private final List<int[]> redSpawnTiles = new ArrayList<>();
    private final List<int[]> blueSpawnTiles = new ArrayList<>();
    private final List<Rectangle> obstacles = new ArrayList<>();
    
    // 라운드 시스템
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
    
    // 캐릭터 선택 제한
    private boolean hasChangedCharacterInRound = false;
    
    // General 궁극기 상태
    private boolean awaitingMinimapTarget = false;
    
    // 내부 클래스들
    public static class PlayerData {
        public int x, y;
        public int targetX, targetY;
        public int team;
        public int hp;
        public int maxHp;
        public String characterId;
        public int kills;
        public int deaths;
        public int direction = 0;
        public SpriteAnimation[] animations;

        public PlayerData(int x, int y, int team) {
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

        public void smoothUpdate() {
            float interpolation = 0.5f;
            x += (int) ((targetX - x) * interpolation);
            y += (int) ((targetY - y) * interpolation);
        }
    }

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
    
    // 생성자
    public GameState(String playerName, int team) {
        this.playerName = playerName;
        this.team = team;
    }
    
    // Getter/Setter 메서드들
    public String getPlayerName() { return playerName; }
    public int getTeam() { return team; }
    
    public int getPlayerX() { return playerX; }
    public void setPlayerX(int x) { this.playerX = x; }
    
    public int getPlayerY() { return playerY; }
    public void setPlayerY(int y) { this.playerY = y; }
    
    public int getMyHP() { return myHP; }
    public void setMyHP(int hp) { this.myHP = hp; }
    
    public int getMyMaxHP() { return myMaxHP; }
    public void setMyMaxHP(int maxHP) { this.myMaxHP = maxHP; }
    
    public int getMyDirection() { return myDirection; }
    public void setMyDirection(int direction) { this.myDirection = direction; }
    
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    
    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    
    public String getSelectedCharacter() { return selectedCharacter; }
    public void setSelectedCharacter(String character) { this.selectedCharacter = character; }
    
    public CharacterData getCurrentCharacterData() { return currentCharacterData; }
    public void setCurrentCharacterData(CharacterData data) { this.currentCharacterData = data; }
    
    public Ability[] getAbilities() { return abilities; }
    public void setAbilities(Ability[] abilities) { this.abilities = abilities; }
    
    public SpriteAnimation[] getMyAnimations() { return myAnimations; }
    public void setMyAnimations(SpriteAnimation[] animations) { this.myAnimations = animations; }
    
    // Raven 상태
    public float getRavenDashRemaining() { return ravenDashRemaining; }
    public void setRavenDashRemaining(float value) { this.ravenDashRemaining = value; }
    
    public float getRavenOverchargeRemaining() { return ravenOverchargeRemaining; }
    public void setRavenOverchargeRemaining(float value) { this.ravenOverchargeRemaining = value; }
    
    public float getMissileSpeedMultiplier() { return missileSpeedMultiplier; }
    public void setMissileSpeedMultiplier(float value) { this.missileSpeedMultiplier = value; }
    
    // Piper 상태
    public float getPiperMarkRemaining() { return piperMarkRemaining; }
    public void setPiperMarkRemaining(float value) { this.piperMarkRemaining = value; }
    
    public float getPiperThermalRemaining() { return piperThermalRemaining; }
    public void setPiperThermalRemaining(float value) { this.piperThermalRemaining = value; }
    
    public float getTeamMarkRemaining() { return teamMarkRemaining; }
    public void setTeamMarkRemaining(float value) { this.teamMarkRemaining = value; }
    
    public float getTeamThermalRemaining() { return teamThermalRemaining; }
    public void setTeamThermalRemaining(float value) { this.teamThermalRemaining = value; }
    
    // 버프 상태
    public float getMoveSpeedMultiplier() { return moveSpeedMultiplier; }
    public void setMoveSpeedMultiplier(float value) { this.moveSpeedMultiplier = value; }
    
    public float getAttackSpeedMultiplier() { return attackSpeedMultiplier; }
    public void setAttackSpeedMultiplier(float value) { this.attackSpeedMultiplier = value; }
    
    // 이펙트 시스템
    public Map<String, List<ActiveEffect>> getEffectsByPlayer() { return effectsByPlayer; }
    public List<ActiveEffect> getMyEffects() { return myEffects; }
    
    // 게임 객체들
    public Map<String, PlayerData> getPlayers() { return players; }
    public void setPlayers(Map<String, PlayerData> players) { this.players = players; }
    
    public List<Missile> getMissiles() { return missiles; }
    
    public Map<Integer, PlacedObjectClient> getPlacedObjects() { return placedObjects; }
    public Map<Integer, StrikeMarker> getStrikeMarkers() { return strikeMarkers; }
    
    // 맵 시스템
    public int getMapWidth() { return mapWidth; }
    public void setMapWidth(int width) { this.mapWidth = width; }
    
    public int getMapHeight() { return mapHeight; }
    public void setMapHeight(int height) { this.mapHeight = height; }
    
    public int getCameraX() { return cameraX; }
    public void setCameraX(int x) { this.cameraX = x; }
    
    public int getCameraY() { return cameraY; }
    public void setCameraY(int y) { this.cameraY = y; }
    
    public String getCurrentMapName() { return currentMapName; }
    public void setCurrentMapName(String name) { this.currentMapName = name; }
    
    public boolean[][] getWalkableGrid() { return walkableGrid; }
    public void setWalkableGrid(boolean[][] grid) { this.walkableGrid = grid; }
    
    public int getGridCols() { return gridCols; }
    public void setGridCols(int cols) { this.gridCols = cols; }
    
    public int getGridRows() { return gridRows; }
    public void setGridRows(int rows) { this.gridRows = rows; }
    
    public Rectangle getRedSpawnZone() { return redSpawnZone; }
    public void setRedSpawnZone(Rectangle zone) { this.redSpawnZone = zone; }
    
    public Rectangle getBlueSpawnZone() { return blueSpawnZone; }
    public void setBlueSpawnZone(Rectangle zone) { this.blueSpawnZone = zone; }
    
    public List<int[]> getRedSpawnTiles() { return redSpawnTiles; }
    public List<int[]> getBlueSpawnTiles() { return blueSpawnTiles; }
    public List<Rectangle> getObstacles() { return obstacles; }
    
    // 라운드 시스템
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
    public void setCenterMessage(String message) { this.centerMessage = message; }
    
    public long getCenterMessageEndTime() { return centerMessageEndTime; }
    public void setCenterMessageEndTime(long time) { this.centerMessageEndTime = time; }
    
    public boolean hasChangedCharacterInRound() { return hasChangedCharacterInRound; }
    public void setHasChangedCharacterInRound(boolean changed) { this.hasChangedCharacterInRound = changed; }
    
    public boolean isAwaitingMinimapTarget() { return awaitingMinimapTarget; }
    public void setAwaitingMinimapTarget(boolean awaiting) { this.awaitingMinimapTarget = awaiting; }
    
    // 게임 로직 메서드들
    
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
     * 플레이어 위치 보간 업데이트
     */
    public void updatePlayerInterpolation() {
        for (PlayerData pd : players.values()) {
            pd.smoothUpdate();
        }
    }
    
    /**
     * 스킬 이펙트 시간 업데이트
     * @param deltaTime 프레임 경과 시간 (초)
     */
    public void updateEffects(float deltaTime) {
        // 내 이펙트 업데이트
        myEffects.removeIf(effect -> {
            effect.remaining -= deltaTime;
            return effect.remaining <= 0;
        });
        
        // 다른 플레이어 이펙트 업데이트
        for (List<ActiveEffect> effects : effectsByPlayer.values()) {
            effects.removeIf(effect -> {
                effect.remaining -= deltaTime;
                return effect.remaining <= 0;
            });
        }
        
        // 캐릭터별 상태 시간 감소
        if (ravenDashRemaining > 0) {
            ravenDashRemaining -= deltaTime;
            if (ravenDashRemaining <= 0) ravenDashRemaining = 0;
        }
        if (ravenOverchargeRemaining > 0) {
            ravenOverchargeRemaining -= deltaTime;
            if (ravenOverchargeRemaining <= 0) {
                ravenOverchargeRemaining = 0;
                missileSpeedMultiplier = 1f;
            }
        }
        if (piperMarkRemaining > 0) {
            piperMarkRemaining -= deltaTime;
            if (piperMarkRemaining <= 0) piperMarkRemaining = 0;
        }
        if (piperThermalRemaining > 0) {
            piperThermalRemaining -= deltaTime;
            if (piperThermalRemaining <= 0) piperThermalRemaining = 0;
        }
        if (teamMarkRemaining > 0) {
            teamMarkRemaining -= deltaTime;
            if (teamMarkRemaining <= 0) teamMarkRemaining = 0;
        }
        if (teamThermalRemaining > 0) {
            teamThermalRemaining -= deltaTime;
            if (teamThermalRemaining <= 0) teamThermalRemaining = 0;
        }
    }
    
    /**
     * 카메라를 플레이어 중심으로 업데이트
     * @param screenWidth 화면 너비
     * @param screenHeight 화면 높이
     */
    public void updateCamera(int screenWidth, int screenHeight) {
        cameraX = playerX - screenWidth / 2;
        cameraY = playerY - screenHeight / 2;
        
        // 카메라가 맵 밖으로 나가지 않도록 제한
        cameraX = Math.max(0, Math.min(cameraX, mapWidth - screenWidth));
        cameraY = Math.max(0, Math.min(cameraY, mapHeight - screenHeight));
    }
}
