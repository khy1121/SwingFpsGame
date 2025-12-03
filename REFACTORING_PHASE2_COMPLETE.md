# Phase 2 리팩토링 완료 보고서

## 개요
GamePanel의 책임을 더욱 세분화하여 MVC 패턴을 강화하는 Phase 2 리팩토링을 완료했습니다.

## 완료 날짜
2025-12-03

## 리팩토링 목표
- GamePanel의 과도한 책임을 전문화된 매니저 클래스로 분리
- 코드 재사용성 및 테스트 가능성 향상
- 각 시스템의 독립성 강화

## 생성된 매니저 클래스

### 1. CollisionManager
**파일**: `src/com/fpsgame/client/CollisionManager.java`

**책임**:
- 플레이어-장애물 충돌 감지
- 미사일-장애물 충돌 감지
- 미사일-플레이어 충돌 감지
- 미사일-오브젝트 충돌 감지
- 타일 기반 walkable 체크

**주요 메서드**:
```java
updateMapData(boolean[][] walkableGrid, int gridRows, int gridCols, List<Rectangle> obstacles)
checkCollisionWithObstacles(int x, int y)
isPositionWalkable(int x, int y)
isMissileBlocked(int x, int y)
checkMissilePlayerCollision(int missileX, int missileY, int playerX, int playerY)
checkMissileObjectCollision(int missileX, int missileY, int objectX, int objectY)
```

**통합 지점**:
- 맵 로드 시 자동 업데이트 (`loadMap()`)
- 장애물 재구성 시 자동 업데이트 (`rebuildObstaclesFromWalkable()`)

---

### 2. PlayerMovementController
**파일**: `src/com/fpsgame/client/PlayerMovementController.java`

**책임**:
- 플레이어 이동 로직
- 키 입력 처리
- 충돌 감지 및 위치 보정
- 카메라 위치 계산

**주요 메서드**:
```java
updatePlayerPosition(int currentX, int currentY, boolean[] keys, PlayerPosition outPosition)
updateCamera(int playerX, int playerY, CameraPosition outCamera)
updateMapSize(int mapWidth, int mapHeight)
```

**특징**:
- 이동 속도 버프(`moveSpeedMultiplier`) 지원
- 대각선 이동 시 한 축만 이동 시도 (부드러운 벽 슬라이딩)
- CollisionManager와 연동

**내부 클래스**:
- `PlayerPosition`: 플레이어 위치 결과
- `CameraPosition`: 카메라 위치 결과

---

### 3. SpawnManager
**파일**: `src/com/fpsgame/client/SpawnManager.java`

**책임**:
- 팀별 스폰 구역 관리
- 스폰 타일 목록 관리
- 랜덤 스폰 위치 계산
- 초기 스폰 위치 계산

**주요 메서드**:
```java
setSpawnZones(Rectangle redZone, Rectangle blueZone)
setSpawnTiles(List<int[]> redTiles, List<int[]> blueTiles)
getRandomSpawnPosition(int team)
getInitialSpawnPosition(int team, int mapWidth, int mapHeight)
getSpawnZone(int team)
hasValidSpawnZones()
```

**특징**:
- 타일 기반 랜덤 스폰
- 폴백 로직 (타일 없음 → 구역 중앙 → 맵 중앙)
- JSON 로드 시 자동 업데이트

**내부 클래스**:
- `SpawnPosition`: 스폰 위치 결과

---

### 4. GameObjectManager
**파일**: `src/com/fpsgame/client/GameObjectManager.java`

**책임**:
- 미사일 관리
- 설치된 오브젝트(터렛, 지뢰) 관리
- 스트라이크 마커 관리
- 게임 오브젝트 업데이트

**주요 메서드**:
```java
// 미사일
addMissile(Missile missile)
updateMissiles()
getMissiles()
clearMissiles()

// 오브젝트
putPlacedObject(int id, PlacedObjectClient obj)
removePlacedObject(int id)
getPlacedObject(int id)
getPlacedObjects()
clearPlacedObjects()

// 스트라이크 마커
addStrikeMarker(int id, StrikeMarker marker)
removeStrikeMarker(int id)
getStrikeMarkers()
clearStrikeMarkers()

// 전체
clearAll()
```

**내부 클래스**:
- `Missile`: 미사일 데이터
- `PlacedObjectClient`: 설치된 오브젝트 (터렛, 지뢰)
- `StrikeMarker`: 에어스트라이크 마커

**특징**:
- GamePanel의 내부 클래스를 GameObjectManager로 이동
- CollisionManager와 연동하여 미사일 충돌 처리

---

## 수정된 기존 파일

### GamePanel.java
**변경 사항**:
- 4개의 매니저 필드 추가
- 생성자에서 매니저 초기화
- 충돌, 이동, 스폰, 오브젝트 관련 로직을 매니저로 위임
- 내부 클래스 제거 (GameObjectManager로 이동)
- 코드 라인 수 감소: 2637줄 → 2545줄 (약 92줄 감소)

**매니저 필드**:
```java
private final CollisionManager collisionManager;
private final PlayerMovementController movementController;
private final SpawnManager spawnManager;
final GameObjectManager objectManager; // package-private
```

**주요 변경 메서드**:
- `updatePlayerPosition()`: PlayerMovementController로 위임
- `updateCamera()`: PlayerMovementController로 위임
- `updateMissiles()`: GameObjectManager로 위임
- `checkCollisionWithObstacles()`: CollisionManager로 위임
- `respawn()`: SpawnManager로 위임
- `setInitialSpawnPosition()`: SpawnManager로 위임
- `shootMissile()`: GameObjectManager에 미사일 추가

---

### GameRenderer.java
**변경 사항**:
- `RenderContext`의 타입 변경
  - `List<GamePanel.Missile>` → `List<GameObjectManager.Missile>`
  - `Map<Integer, GamePanel.PlacedObjectClient>` → `Map<Integer, GameObjectManager.PlacedObjectClient>`
  - `Map<Integer, GamePanel.StrikeMarker>` → `Map<Integer, GameObjectManager.StrikeMarker>`

---

### GameMessageHandler.java
**변경 사항**:
- 네트워크 메시지 처리 시 GameObjectManager 사용
- `SHOOT`: `objectManager.addMissile()`
- `PLACE_OBJECT`: `objectManager.putPlacedObject()`
- `REMOVE_OBJECT`: `objectManager.removePlacedObject()`
- `HIT_OBJ`: `objectManager.getPlacedObject()`
- `AIRSTRIKE`: `objectManager.addStrikeMarker()`
- `TURRET_SHOOT`: `objectManager.addMissile()`

---

## 버그 수정

### 마우스 좌표 동기화 문제
**문제**: 마우스 클릭 위치와 실제 미사일 발사 방향이 일치하지 않음

**원인**: 
- `scaleMouseCoordinates()`가 JFrame 전체 크기(`getWidth()/getHeight()`)를 사용
- 실제 렌더링은 canvas 크기 사용
- JFrame은 타이틀바, 테두리 포함으로 canvas보다 큼

**해결**:
```java
// 수정 전
int panelWidth = getWidth();
int panelHeight = getHeight();

// 수정 후
int actualWidth = canvas != null ? canvas.getWidth() : GameConstants.GAME_WIDTH;
int actualHeight = canvas != null ? canvas.getHeight() : GameConstants.GAME_HEIGHT;
```

---

## 아키텍처 개선 효과

### Before (Phase 1)
```
GamePanel (2637 lines)
├── GameState
├── NetworkClient
├── GameRenderer
├── GameMessageHandler
├── MapManager
├── SkillManager
├── UIManager
└── GameLogicController
```

### After (Phase 2)
```
GamePanel (2545 lines) - 약 3.5% 감소
├── GameState
├── NetworkClient
├── GameRenderer
├── GameMessageHandler
├── MapManager
├── SkillManager
├── UIManager
├── GameLogicController
├── CollisionManager ⭐ NEW
├── PlayerMovementController ⭐ NEW
├── SpawnManager ⭐ NEW
└── GameObjectManager ⭐ NEW
```

### 책임 분리 효과
1. **테스트 가능성 향상**: 각 매니저를 독립적으로 테스트 가능
2. **재사용성 증가**: 다른 게임 모드나 프로젝트에서 재사용 가능
3. **유지보수성 향상**: 특정 기능 수정 시 해당 매니저만 수정
4. **코드 가독성**: GamePanel의 복잡도 감소

---

## 성능 영향
- **런타임 성능**: 매니저 호출 오버헤드는 무시할 수 있는 수준
- **메모리**: 매니저 객체 추가로 인한 메모리 증가는 미미함 (약 4개 객체)
- **컴파일 시간**: 클래스 파일 4개 추가로 인한 영향 미미

---

## 테스트 결과
- ✅ 게임 실행 정상 동작
- ✅ 플레이어 이동 정상 동작
- ✅ 미사일 발사 및 충돌 정상 동작
- ✅ 스폰 시스템 정상 동작
- ✅ 마우스 좌표 동기화 정상 동작
- ✅ 네트워크 메시지 처리 정상 동작

---

## 향후 개선 가능 사항

### 추가 리팩토링 후보
1. **NetworkManager**: 네트워크 통신 로직 분리
2. **AnimationManager**: 애니메이션 관리 로직 분리
3. **InputManager**: 키보드/마우스 입력 처리 통합
4. **EffectManager**: 스킬 이펙트 관리 강화

### 코드 정리
- 사용하지 않는 필드 제거 (`hoverCol`, `hoverRow`, `mapIndex` 등)
- Lint 경고 해결 (switch문 개선, 불필요한 continue문 제거 등)

---

## 결론
Phase 2 리팩토링을 통해 GamePanel의 책임을 4개의 전문화된 매니저로 분리하여 코드 품질을 크게 향상시켰습니다. 각 매니저는 단일 책임 원칙(SRP)을 준수하며, 높은 응집도와 낮은 결합도를 유지합니다. 게임의 핵심 기능은 모두 정상 동작하며, 향후 유지보수와 확장이 더욱 용이해졌습니다.
