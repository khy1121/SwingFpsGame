package com.fpsgame.client;

import com.fpsgame.common.GameConstants;

/**
 * 플레이어 이동 및 카메라 제어 관리 클래스
 */
public class PlayerMovementController {
    
    private final int SPEED;
    private final CollisionManager collisionManager;
    
    // 맵 정보
    private int mapWidth;
    private int mapHeight;
    
    public PlayerMovementController(int speed, CollisionManager collisionManager) {
        this.SPEED = speed;
        this.collisionManager = collisionManager;
    }
    
    /**
     * 맵 크기 업데이트
     */
    public void updateMapSize(int mapWidth, int mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }
    
    /**
     * 플레이어 위치 업데이트 (키 입력 기반)
     * @return 위치가 변경되었으면 true
     */
    public boolean updatePlayerPosition(int currentX, int currentY, boolean[] keys, PlayerPosition outPosition) {
        int oldX = currentX;
        int oldY = currentY;
        int newX = currentX;
        int newY = currentY;
        
        // 키 입력에 따른 이동
        if (keys['W'] || keys['w']) newY -= SPEED;
        if (keys['S'] || keys['s']) newY += SPEED;
        if (keys['A'] || keys['a']) newX -= SPEED;
        if (keys['D'] || keys['d']) newX += SPEED;
        
        // 맵 경계 체크
        newX = Math.max(0, Math.min(newX, mapWidth));
        newY = Math.max(0, Math.min(newY, mapHeight));
        
        // 충돌 체크 및 위치 조정
        if (!collisionManager.checkCollisionWithObstacles(newX, newY)) {
            outPosition.x = newX;
            outPosition.y = newY;
        } else {
            // 대각선 이동 시 한 축만 이동 시도
            if (!collisionManager.checkCollisionWithObstacles(newX, oldY)) {
                outPosition.x = newX;
                outPosition.y = oldY;
            } else if (!collisionManager.checkCollisionWithObstacles(oldX, newY)) {
                outPosition.x = oldX;
                outPosition.y = newY;
            } else {
                outPosition.x = oldX;
                outPosition.y = oldY;
            }
        }
        
        return outPosition.x != oldX || outPosition.y != oldY;
    }
    
    /**
     * 카메라 위치 계산 (플레이어 중심)
     */
    public void updateCamera(int playerX, int playerY, CameraPosition outCamera) {
        // 플레이어를 화면 중앙에 위치
        outCamera.x = playerX - GameConstants.GAME_WIDTH / 2;
        outCamera.y = playerY - GameConstants.GAME_HEIGHT / 2;
        
        // 카메라가 맵 경계를 벗어나지 않도록 제한
        outCamera.x = Math.max(0, Math.min(outCamera.x, mapWidth - GameConstants.GAME_WIDTH));
        outCamera.y = Math.max(0, Math.min(outCamera.y, mapHeight - GameConstants.GAME_HEIGHT));
    }
    
    /**
     * 플레이어 위치 결과를 담는 클래스
     */
    public static class PlayerPosition {
        public int x;
        public int y;
        
        public PlayerPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    
    /**
     * 카메라 위치 결과를 담는 클래스
     */
    public static class CameraPosition {
        public int x;
        public int y;
        
        public CameraPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
