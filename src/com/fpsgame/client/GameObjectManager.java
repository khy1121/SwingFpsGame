package com.fpsgame.client;

import java.util.*;

/**
 * 게임 오브젝트 관리 클래스
 * 미사일, 설치된 오브젝트, 스트라이크 마커 등을 관리
 */
public class GameObjectManager {
    
    // 미사일 리스트
    private final List<Missile> missiles = new ArrayList<>();
    
    // 설치된 오브젝트 (터렛, 지뢰 등)
    private final Map<Integer, PlacedObjectClient> placedObjects = new HashMap<>();
    
    // 스트라이크 마커 (General 에어스트라이크)
    private final Map<Integer, StrikeMarker> strikeMarkers = new HashMap<>();
    
    private final CollisionManager collisionManager;
    private int mapWidth;
    private int mapHeight;
    
    public GameObjectManager(CollisionManager collisionManager) {
        this.collisionManager = collisionManager;
    }
    
    /**
     * 맵 크기 업데이트
     */
    public void updateMapSize(int mapWidth, int mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }
    
    // ==================== 미사일 관리 ====================
    
    /**
     * 미사일 추가
     */
    public void addMissile(Missile missile) {
        missiles.add(missile);
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
    
    /**
     * 미사일 리스트 가져오기
     */
    public List<Missile> getMissiles() {
        return missiles;
    }
    
    /**
     * 모든 미사일 제거
     */
    public void clearMissiles() {
        missiles.clear();
    }
    
    // ==================== 설치된 오브젝트 관리 ====================
    
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
    
    /**
     * 오브젝트 가져오기
     */
    public PlacedObjectClient getPlacedObject(int id) {
        return placedObjects.get(id);
    }
    
    /**
     * 모든 오브젝트 맵 가져오기
     */
    public Map<Integer, PlacedObjectClient> getPlacedObjects() {
        return placedObjects;
    }
    
    /**
     * 모든 오브젝트 제거
     */
    public void clearPlacedObjects() {
        placedObjects.clear();
    }
    
    // ==================== 스트라이크 마커 관리 ====================
    
    /**
     * 스트라이크 마커 추가
     */
    public void addStrikeMarker(int id, StrikeMarker marker) {
        strikeMarkers.put(id, marker);
    }
    
    /**
     * 스트라이크 마커 제거
     */
    public void removeStrikeMarker(int id) {
        strikeMarkers.remove(id);
    }
    
    /**
     * 모든 스트라이크 마커 가져오기
     */
    public Map<Integer, StrikeMarker> getStrikeMarkers() {
        return strikeMarkers;
    }
    
    /**
     * 모든 스트라이크 마커 제거
     */
    public void clearStrikeMarkers() {
        strikeMarkers.clear();
    }
    
    /**
     * 모든 게임 오브젝트 초기화
     */
    public void clearAll() {
        missiles.clear();
        placedObjects.clear();
        strikeMarkers.clear();
    }
    
    // ==================== 내부 클래스 정의 ====================
    
    /**
     * 미사일 클래스
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
     * 설치된 오브젝트 클래스 (터렛, 지뢰 등)
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
     * 스트라이크 마커 클래스 (에어스트라이크 위치 표시)
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
}
