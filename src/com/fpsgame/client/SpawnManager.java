package com.fpsgame.client;

import com.fpsgame.common.GameConstants;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 스폰 시스템 관리 클래스
 * 
 * <p>팀별 스폰 구역 및 타일 관리를 담당합니다.
 * JSON 맵 데이터에서 스폰 타일 목록을 로드하여,
 * 리스폰 시 해당 타일 중 랜덤 위치를 선택합니다.</p>
 * 
 * <h2>주요 기능:</h2>
 * <ul>
 *   <li>팀별 스폰 구역 설정 (RED, BLUE)</li>
 *   <li>스폰 타일 목록 관리 (JSON 기반)</li>
 *   <li>랜덤 스폰 위치 계산</li>
 *   <li>초기 스폰 위치 제공</li>
 * </ul>
 * 
 * <h2>사용 예시:</h2>
 * <pre>
 * SpawnManager spawnManager = new SpawnManager();
 * spawnManager.setSpawnZones(redZone, blueZone);
 * spawnManager.setSpawnTiles(redTiles, blueTiles);
 * 
 * SpawnPosition pos = spawnManager.getRandomSpawnPosition(GameConstants.TEAM_RED);
 * playerX = pos.x;
 * playerY = pos.y;
 * </pre>
 * 
 * @author NetFps Team
 * @version 1.0 (Phase 2 리팩토링)
 * @see GamePanel
 * @see MapManager
 */
public class SpawnManager {
    
    private static final int TILE_SIZE = 32;
    
    // 스폰 구역
    private Rectangle redSpawnZone;
    private Rectangle blueSpawnZone;
    
    // 스폰 타일 목록 (랜덤 스폰용)
    private final List<int[]> redSpawnTiles = new ArrayList<>();
    private final List<int[]> blueSpawnTiles = new ArrayList<>();
    
    private final Random random = new Random();
    
    /**
     * 스폰 구역 설정
     */
    public void setSpawnZones(Rectangle redZone, Rectangle blueZone) {
        this.redSpawnZone = redZone;
        this.blueSpawnZone = blueZone;
        recomputeSpawnZones();
    }
    
    /**
     * 스폰 타일 직접 설정 (JSON에서 로드)
     */
    public void setSpawnTiles(List<int[]> redTiles, List<int[]> blueTiles) {
        this.redSpawnTiles.clear();
        this.blueSpawnTiles.clear();
        
        if (redTiles != null) {
            this.redSpawnTiles.addAll(redTiles);
        }
        if (blueTiles != null) {
            this.blueSpawnTiles.addAll(blueTiles);
        }
    }
    
    /**
     * 스폰 구역으로부터 타일 목록 재계산
     */
    private void recomputeSpawnZones() {
        redSpawnTiles.clear();
        blueSpawnTiles.clear();
        
        if (redSpawnZone != null) {
            computeTilesFromZone(redSpawnZone, redSpawnTiles);
        }
        if (blueSpawnZone != null) {
            computeTilesFromZone(blueSpawnZone, blueSpawnTiles);
        }
    }
    
    /**
     * 구역으로부터 타일 좌표 추출
     */
    private void computeTilesFromZone(Rectangle zone, List<int[]> tiles) {
        int startCol = zone.x / TILE_SIZE;
        int endCol = (zone.x + zone.width) / TILE_SIZE;
        int startRow = zone.y / TILE_SIZE;
        int endRow = (zone.y + zone.height) / TILE_SIZE;
        
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                tiles.add(new int[]{c, r});
            }
        }
    }
    
    /**
     * 랜덤 스폰 위치 계산
     */
    public SpawnPosition getRandomSpawnPosition(int team) {
        List<int[]> tiles = (team == GameConstants.TEAM_RED) ? redSpawnTiles : blueSpawnTiles;
        
        if (tiles == null || tiles.isEmpty()) {
            // 폴백: 스폰 구역 중앙
            Rectangle zone = (team == GameConstants.TEAM_RED) ? redSpawnZone : blueSpawnZone;
            if (zone != null) {
                return new SpawnPosition(
                    zone.x + zone.width / 2,
                    zone.y + zone.height / 2
                );
            }
            // 최종 폴백: 맵 중앙 (외부에서 맵 크기 전달 필요)
            return null;
        }
        
        int[] tile = tiles.get(random.nextInt(tiles.size()));
        return new SpawnPosition(
            tile[0] * TILE_SIZE + TILE_SIZE / 2,
            tile[1] * TILE_SIZE + TILE_SIZE / 2
        );
    }
    
    /**
     * 초기 스폰 위치 계산 (게임 시작 시)
     */
    public SpawnPosition getInitialSpawnPosition(int team, int mapWidth, int mapHeight) {
        SpawnPosition pos = getRandomSpawnPosition(team);
        if (pos != null) {
            return pos;
        }
        
        // 폴백: 맵 중앙
        return new SpawnPosition(mapWidth / 2, mapHeight / 2);
    }
    
    /**
     * 스폰 구역 가져오기
     */
    public Rectangle getSpawnZone(int team) {
        return (team == GameConstants.TEAM_RED) ? redSpawnZone : blueSpawnZone;
    }
    
    /**
     * 스폰 타일 목록 가져오기
     */
    public List<int[]> getSpawnTiles(int team) {
        return (team == GameConstants.TEAM_RED) ? redSpawnTiles : blueSpawnTiles;
    }
    
    /**
     * 스폰 구역이 유효한지 확인
     */
    public boolean hasValidSpawnZones() {
        return redSpawnZone != null && blueSpawnZone != null;
    }
    
    /**
     * 스폰 위치 결과 클래스
     */
    public static class SpawnPosition {
        public final int x;
        public final int y;
        
        public SpawnPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
