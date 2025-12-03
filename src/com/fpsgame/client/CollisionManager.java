package com.fpsgame.client;

import java.awt.Rectangle;
import java.util.List;

/**
 * 충돌 감지 관리 클래스
 * 플레이어, 미사일, 오브젝트 간의 충돌을 처리
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
     */
    public void updateMapData(boolean[][] walkableGrid, int gridRows, int gridCols, List<Rectangle> obstacles) {
        this.walkableGrid = walkableGrid;
        this.gridRows = gridRows;
        this.gridCols = gridCols;
        this.obstacles = obstacles;
    }
    
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
