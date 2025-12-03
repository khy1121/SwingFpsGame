package com.fpsgame.client;

import com.fpsgame.common.GameConstants;

/**
 * 플레이어 이동 및 카메라 제어 관리 클래스
 * 
 * <p>플레이어의 키 입력 기반 이동과 카메라 추적 로직을 담당합니다.
 * 충돌 감지, 맵 경계 처리, 대각선 이동 시 슬라이딩 처리를 포함합니다.</p>
 * 
 * <h2>주요 기능:</h2>
 * <ul>
 *   <li>WASD/화살표 키 입력 처리</li>
 *   <li>충돌 시 자동 슬라이딩 (한 축만 이동)</li>
 *   <li>플레이어 중심 카메라 추적</li>
 *   <li>카메라 맵 경계 제한</li>
 * </ul>
 * 
 * <h2>사용 예시:</h2>
 * <pre>
 * PlayerMovementController controller = new PlayerMovementController(5, collisionManager);
 * controller.updateMapSize(3200, 2400);
 * 
 * PlayerPosition newPos = new PlayerPosition(playerX, playerY);
 * if (controller.updatePlayerPosition(playerX, playerY, keys, newPos)) {
 *     playerX = newPos.x;
 *     playerY = newPos.y;
 * }
 * </pre>
 * 
 * @author NetFps Team
 * @version 1.0 (Phase 2 리팩토링)
 * @see CollisionManager
 * @see GamePanel
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
     * 
     * <p>WASD 키 입력을 받아 플레이어 위치를 계산합니다.
     * 충돌 시 대각선 이동의 경우 한 축만 이동하는 슬라이딩 처리를 합니다.</p>
     * 
     * @param currentX 현재 플레이어 X 좌표
     * @param currentY 현재 플레이어 Y 좌표
     * @param keys 키 입력 배열 (boolean[256], 'W'/'w', 'A'/'a' 등)
     * @param outPosition 계산된 새 위치를 담을 객체 (출력 파라미터)
     * @return 위치가 변경되었으면 true, 그대로면 false
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
