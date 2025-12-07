package com.fpsgame.client;

import java.util.*;

/**
 * 게임 오브젝트 관리 클래스 - Phase 2 리팩토링
 * 
 * <p>게임 내 모든 동적 오브젝트를 중앙에서 관리하는 매니저 클래스입니다.
 * 미사일 투사체, 설치형 오브젝트(터렛, 지뢰), 스트라이크 마커 등의 생명주기를 담당합니다.</p>
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><b>미사일 관리</b>: 기본 공격 투사체의 생성, 이동, 충돌, 소멸 처리</li>
 *   <li><b>설치 오브젝트 관리</b>: Tech의 터렛, 지뢰 등 지속형 오브젝트 추적</li>
 *   <li><b>스트라이크 마커 관리</b>: General의 에어스트라이크 표시 UI</li>
 *   <li><b>자동 정리</b>: 맵 밖으로 나가거나 벽에 충돌한 오브젝트 자동 제거</li>
 * </ul>
 * 
 * <h3>사용 예시:</h3>
 * <pre><code>
 * // 미사일 추가
 * Missile missile = new Missile(x, y, dx, dy, team, owner);
 * objectManager.addMissile(missile);
 * 
 * // 매 프레임 업데이트 (게임 루프에서 호출)
 * objectManager.updateMissiles();
 * 
 * // 설치 오브젝트 추가 (서버에서 OBJECT 패킷 수신 시)
 * PlacedObjectClient obj = new PlacedObjectClient(id, "turret", x, y, hp, maxHp, owner, team);
 * objectManager.putPlacedObject(id, obj);
 * 
 * // 라운드 종료 시 초기화
 * objectManager.clearAll();
 * </code></pre>
 * 
 * @author NetFps Team
 * @version 1.1 (Phase 2 리팩토링)
 * @since 2025-12-03
 * @see GamePanel
 * @see CollisionManager
 * @see GameServer
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
            
            // 사거리 체크 (maxRange > 0이면 제한 있음)
            if (m.maxRange > 0) {
                float dx = m.x - m.startX;
                float dy = m.y - m.startY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance > m.maxRange) {
                    it.remove();
                    continue;
                }
            }
            
            // 맵 밖이면 제거
            if (m.x < 0 || m.x > mapWidth || m.y < 0 || m.y > mapHeight) {
                it.remove();
                continue;
            }
            
            // 벽 충돌 (정수 좌표로 변환하여 체크)
            if (collisionManager.isMissileBlocked((int)m.x, (int)m.y)) {
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
     * 미사일 투사체 클래스 (기본 공격)
     * 
     * <p>플레이어의 기본 공격으로 발사되는 투사체를 표현합니다.
     * 직선 궤도로 이동하며 벽 또는 맵 경계에 닿으면 소멸합니다.</p>
     * 
     * <h4>필드 설명:</h4>
     * <ul>
     *   <li><b>x, y</b>: 현재 위치 (픽셀 좌표)</li>
     *   <li><b>dx, dy</b>: 이동 방향 및 속도 (픽셀/프레임)</li>
     *   <li><b>team</b>: 소속 팀 (RED=1, BLUE=2)</li>
     *   <li><b>owner</b>: 발사한 플레이어 닉네임</li>
     * </ul>
     * 
     * <h4>사용 예시:</h4>
     * <pre><code>
     * // 마우스 클릭 방향으로 미사일 발사
     * int dx = (mouseX - playerX) / 10;
     * int dy = (mouseY - playerY) / 10;
     * Missile missile = new Missile(playerX, playerY, dx, dy, myTeam, myNickname);
     * </code></pre>
     */
    public static class Missile {
        private static int nextId = 1; // 미사일 ID 카운터
        
        public int id;            // 미사일 고유 ID
        public float x, y;        // 실수 좌표로 변경 (정밀한 이동)
        public float dx, dy;      // 실수 이동량으로 변경 (정밀한 각도)
        public int team;
        public String owner;
        public float startX, startY;  // 발사 위치
        public float maxRange;        // 최대 사거리 (0이면 무제한)
        
        public Missile(float x, float y, float dx, float dy, int team, String owner) {
            this(x, y, dx, dy, team, owner, 0f);
        }
        
        public Missile(float x, float y, float dx, float dy, int team, String owner, float maxRange) {
            this.id = nextId++; // 고유 ID 할당
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.team = team;
            this.owner = owner;
            this.startX = x;
            this.startY = y;
            this.maxRange = maxRange;
        }
    }
    
    /**
     * 설치형 오브젝트 클래스 (터렛, 지뢰)
     * 
     * <p>Tech 캐릭터의 스킬로 맵에 배치되는 지속형 오브젝트를 표현합니다.
     * 서버에서 OBJECT 패킷으로 동기화되며, HP가 0이 되면 파괴됩니다.</p>
     * 
     * <h4>오브젝트 타입:</h4>
     * <ul>
     *   <li><b>"turret"</b>: 자동 조준 터렛 (tech_turret)
     *     <ul>
     *       <li>반경 300px 내 적 자동 공격</li>
     *       <li>HP 50, 지속시간 12초</li>
     *     </ul>
     *   </li>
     *   <li><b>"mine"</b>: 근접 지뢰 (tech_mine)
     *     <ul>
     *       <li>반경 60px 내 적 감지 시 폭발</li>
     *       <li>HP 1 (1회성), 지속시간 15초</li>
     *     </ul>
     *   </li>
     * </ul>
     * 
     * <h4>필드 설명:</h4>
     * <ul>
     *   <li><b>id</b>: 서버 부여 고유 ID (OBJECT_PLACE 패킷)</li>
     *   <li><b>type</b>: "turret" 또는 "mine"</li>
     *   <li><b>x, y</b>: 맵 내 고정 위치</li>
     *   <li><b>hp, maxHp</b>: 현재/최대 체력</li>
     *   <li><b>owner</b>: 설치한 플레이어 닉네임</li>
     *   <li><b>team</b>: 소속 팀 (아군에게 무해)</li>
     * </ul>
     * 
     * <h4>네트워크 프로토콜:</h4>
     * <pre>
     * C → S: OBJECT_PLACE:tech_turret:x:y
     * S → C: OBJECT:id:turret:x:y:50:50:owner:team
     * S → C: OBJECT_DAMAGE:id:newHp
     * S → C: OBJECT_REMOVE:id
     * </pre>
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
     * 스트라이크 마커 클래스 (에어스트라이크 예고 표시)
     * 
     * <p>General 캐릭터의 궁극기(gen_strike) 실행 위치를 표시하는 UI 마커입니다.
     * 3초 후 해당 위치에 광역 피해가 발생하므로 플레이어에게 경고를 제공합니다.</p>
     * 
     * <h4>동작 흐름:</h4>
     * <pre>
     * 1. General이 R키로 위치 지정
     * 2. 서버가 STRIKE:id:x:y 브로드캐스트
     * 3. 모든 클라이언트가 해당 위치에 빨간 마커 표시 (3초간)
     * 4. 3초 후 서버가 EXECUTE_STRIKE:id 전송
     * 5. 반경 100px 내 모든 적에게 70 피해 + 마커 제거
     * </pre>
     * 
     * <h4>필드 설명:</h4>
     * <ul>
     *   <li><b>id</b>: 스트라이크 고유 ID (서버 부여)</li>
     *   <li><b>x, y</b>: 타격 중심 좌표</li>
     *   <li><b>createdAt</b>: 생성 시각 (밀리초) - 3초 타이머 계산용</li>
     * </ul>
     * 
     * <h4>렌더링:</h4>
     * <ul>
     *   <li>빨간 원형 마커 (반지름 100px)</li>
     *   <li>경고 아이콘 표시</li>
     *   <li>남은 시간 텍스트 (3 → 2 → 1)</li>
     * </ul>
     * 
     * @see GameServer#executeStrike()
     * @see GameRenderer#drawStrikeMarkers(Graphics2D)
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
