package com.fpsgame.client;

import java.awt.Rectangle;
import java.util.List;

/**
 * 충돌 감지 관리 클래스
 * 
 * <p>게임 내 모든 충돌 감지 로직을 중앙에서 관리합니다.
 * 플레이어, 미사일, 설치 오브젝트 간의 충돌을 체크하며,
 * 타일 기반 walkable grid와 Rectangle 장애물을 모두 지원합니다.</p>
 * 
 * <h2>주요 기능:</h2>
 * <ul>
 *   <li>플레이어-장애물 충돌 감지 (원형 히트박스 기반)</li>
 *   <li>미사일-벽 충돌 감지 (타일 + Rectangle 정밀 체크)</li>
 *   <li>미사일-플레이어 충돌 감지 (거리 기반)</li>
 *   <li>미사일-오브젝트 충돌 감지 (터렛, 지뢰 등)</li>
 * </ul>
 * 
 * <h2>사용 예시:</h2>
 * <pre>
 * CollisionManager collisionManager = new CollisionManager(32);
 * collisionManager.updateMapData(walkableGrid, gridRows, gridCols, obstacles);
 * 
 * if (collisionManager.checkCollisionWithObstacles(playerX, playerY)) {
 *     // 플레이어가 장애물과 충돌함
 * }
 * </pre>
 * 
 * @author NetFps Team
 * @version 1.0 (Phase 2 리팩토링)
 * @see GamePanel
 * @see PlayerMovementController
 */
public class CollisionManager {
    
    private final int TILE_SIZE;
    private boolean[][] walkableGrid;
    private int gridRows;
    private int gridCols;
    private List<Rectangle> obstacles;
    
    public CollisionManager(int tileSize) {
        this.TILE_SIZE = tileSize;
    }
    
    /**
     * 맵 데이터 업데이트
     * 
     * <p>맵이 변경되거나 로드될 때 호출됩니다.
     * 충돌 감지에 필요한 walkable grid와 장애물 목록을 설정합니다.</p>
     * 
     * @param walkableGrid 타일별 이동 가능 여부 (true = 이동 가능)
     * @param gridRows 그리드 행 수
     * @param gridCols 그리드 열 수
     * @param obstacles 장애물 Rectangle 목록 (정밀 충돌용)
     */
    public void updateMapData(boolean[][] walkableGrid, int gridRows, int gridCols, List<Rectangle> obstacles) {
        this.walkableGrid = walkableGrid;
        this.gridRows = gridRows;
        this.gridCols = gridCols;
        this.obstacles = obstacles;
    }
    
    /**
     * 플레이어가 장애물과 충돌하는지 체크
     * 
     * <p>플레이어를 반경 15픽셀의 원형 히트박스로 간주하고,
     * Rectangle 장애물과의 교차 여부를 검사합니다.</p>
     * 
     * @param x 플레이어 중심 X 좌표
     * @param y 플레이어 중심 Y 좌표
     * @return 충돌 시 true, 안전할 경우 false
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
     * 
     * <p>플레이어 중심과 8방향 샘플 포인트가 모두 walkable 타일 위에
     * 있는지 확인합니다. 이를 통해 플레이어가 벽에 끼는 것을 방지합니다.</p>
     * 
     * @param x 플레이어 중심 X 좌표
     * @param y 플레이어 중심 Y 좌표
     * @return 모든 샘플 포인트가 walkable이면 true
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
    
    /**
     * 타일 기반 walkable 체크
     */
    private boolean isTileWalkable(int x, int y) {
        if (walkableGrid == null) return true;
        
        int col = x / TILE_SIZE;
        int row = y / TILE_SIZE;
        
        if (row < 0 || row >= gridRows || col < 0 || col >= gridCols) {
            return false;
        }
        
        return walkableGrid[row][col];
    }
    
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
    
    /**
     * 미사일과 오브젝트 충돌 체크
     */
    public boolean checkMissileObjectCollision(int missileX, int missileY, int objectX, int objectY) {
        double dist = Math.sqrt(Math.pow(missileX - objectX, 2) + Math.pow(missileY - objectY, 2));
        return dist < 30;
    }
}
