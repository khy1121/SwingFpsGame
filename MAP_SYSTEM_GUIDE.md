# 맵 시스템 구현 가이드

## 현재 상태
- ✅ 시야 범위: 화면 크기 기반 (약 500픽셀 반경)
- ✅ 미니맵: 시야 내 적만 표시
- ✅ 좌표 시스템: 800x600 화면 기반
- ⏳ 맵 배경 이미지: 준비 중
- ⏳ 카메라 시스템: 준비 중

## 구현 계획

### 1단계: 맵 이미지 준비
**목표**: 배경으로 사용할 맵 이미지 생성/준비

**필요한 작업**:
```
assets/maps/ 폴더에 맵 이미지 추가
- map_01.png (2400x1800 권장 - 화면의 3배)
- map_02.png (다른 맵)
- map_03.png (추가 맵)
```

**이미지 요구사항**:
- 크기: 2400x1800 픽셀 (또는 800x600의 배수)
- 형식: PNG, JPG
- 내용: FPS 게임에 적합한 탑뷰 맵
  - 벽, 장애물, 커버 위치
  - 통로, 전략적 요충지
  - 리스폰 지역 표시

### 2단계: 맵 로딩 시스템
**목표**: 맵 이미지를 메모리에 로드

**GamePanel.java 수정**:
```java
// 현재 주석 처리된 부분 활성화
private BufferedImage mapImage;
private int mapWidth = 2400;
private int mapHeight = 1800;

// 생성자에서 맵 로드
private void loadMap(String mapName) {
    try {
        mapImage = ImageIO.read(
            getClass().getResourceAsStream("/assets/maps/" + mapName + ".png")
        );
        mapWidth = mapImage.getWidth();
        mapHeight = mapImage.getHeight();
    } catch (Exception e) {
        System.out.println("맵 로드 실패: " + e.getMessage());
        // 폴백: 기본 그리드 사용
    }
}
```

### 3단계: 카메라 시스템
**목표**: 플레이어를 중심으로 화면 이동

**GamePanel.java 수정**:
```java
// 현재 주석 처리된 부분 활성화
private int cameraX = 0;
private int cameraY = 0;

// 매 프레임마다 카메라 업데이트
private void updateCamera() {
    // 플레이어를 화면 중앙에 위치
    cameraX = playerX - GameConstants.GAME_WIDTH / 2;
    cameraY = playerY - GameConstants.GAME_HEIGHT / 2;
    
    // 카메라가 맵 경계를 벗어나지 않도록 제한
    cameraX = Math.max(0, Math.min(cameraX, mapWidth - GameConstants.GAME_WIDTH));
    cameraY = Math.max(0, Math.min(cameraY, mapHeight - GameConstants.GAME_HEIGHT));
}
```

### 4단계: 렌더링 시스템 변경
**목표**: 카메라 오프셋을 적용하여 그리기

**GameCanvas.paintComponent() 수정**:
```java
@Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g;
    
    // 1. 맵 배경 그리기 (카메라 오프셋 적용)
    if (mapImage != null) {
        g2d.drawImage(mapImage, 
            -cameraX, -cameraY,  // 카메라 오프셋
            mapWidth, mapHeight, 
            null);
    } else {
        drawGrid(g2d); // 폴백: 그리드
    }
    
    // 2. 플레이어 그리기 (화면 좌표로 변환)
    int screenX = playerX - cameraX;
    int screenY = playerY - cameraY;
    g2d.setColor(Color.CYAN);
    g2d.fillOval(screenX - 20, screenY - 20, 40, 40);
    
    // 3. 다른 플레이어들 (화면 좌표로 변환)
    for (PlayerData pd : players.values()) {
        int otherScreenX = pd.x - cameraX;
        int otherScreenY = pd.y - cameraY;
        // 화면 내에 있는 경우에만 그리기
        if (isOnScreen(otherScreenX, otherScreenY)) {
            // 그리기...
        }
    }
    
    // 4. 미사일들 (화면 좌표로 변환)
    // ...
}

private boolean isOnScreen(int screenX, int screenY) {
    return screenX >= -50 && screenX <= GameConstants.GAME_WIDTH + 50 &&
           screenY >= -50 && screenY <= GameConstants.GAME_HEIGHT + 50;
}
```

### 5단계: 좌표 시스템 변경
**목표**: 맵 전체 좌표계 사용

**변경 사항**:
```java
// 플레이어 초기 위치 (맵 중앙)
private int playerX = 1200; // mapWidth / 2
private int playerY = 900;  // mapHeight / 2

// 이동 제한 (맵 경계)
private void updatePosition() {
    // 키 입력에 따른 이동
    if (keys[KeyEvent.VK_W]) playerY -= SPEED;
    if (keys[KeyEvent.VK_S]) playerY += SPEED;
    if (keys[KeyEvent.VK_A]) playerX -= SPEED;
    if (keys[KeyEvent.VK_D]) playerX += SPEED;
    
    // 맵 경계 제한
    playerX = Math.max(20, Math.min(playerX, mapWidth - 20));
    playerY = Math.max(20, Math.min(playerY, mapHeight - 20));
}
```

### 6단계: 미니맵 업데이트
**목표**: 전체 맵을 미니맵에 표시

**drawMinimap() 수정**:
```java
// 이미 준비됨 - mapWidth, mapHeight 주석 해제하면 자동 적용
int mapWidth = 2400;  // TODO 주석 제거
int mapHeight = 1800; // TODO 주석 제거
```

### 7단계: 네트워크 동기화
**목표**: 확장된 좌표계를 서버와 동기화

**프로토콜 검증**:
- 좌표 범위: 0~2400 (x), 0~1800 (y)
- 기존 프로토콜 호환성 유지
- 서버의 충돌 감지 범위 확인

## 추가 기능 (선택사항)

### 맵 장애물 시스템
```java
// 장애물 정의
class Obstacle {
    Rectangle bounds;
    boolean isWall; // 완전 차단 vs 반투명 장애물
}

private List<Obstacle> obstacles = new ArrayList<>();

// 충돌 감지
private boolean canMoveTo(int x, int y) {
    Rectangle playerBounds = new Rectangle(x - 20, y - 20, 40, 40);
    for (Obstacle obs : obstacles) {
        if (obs.isWall && playerBounds.intersects(obs.bounds)) {
            return false;
        }
    }
    return true;
}
```

### 리스폰 지역
```java
// 팀별 리스폰 위치
private Point getSpawnPoint(int team) {
    if (team == GameConstants.TEAM_RED) {
        return new Point(300, 300);   // 맵 좌측 상단
    } else {
        return new Point(2100, 1500); // 맵 우측 하단
    }
}
```

### 맵 전환 시스템
```java
private String currentMap = "map_01";

private void changeMap(String newMap) {
    currentMap = newMap;
    loadMap(newMap);
    // 모든 플레이어 리스폰
    playerX = mapWidth / 2;
    playerY = mapHeight / 2;
}
```

## 테스트 체크리스트

- [ ] 맵 이미지 로드 확인
- [ ] 카메라가 플레이어를 따라다니는지 확인
- [ ] 맵 경계에서 카메라가 멈추는지 확인
- [ ] 플레이어 이동이 맵 경계를 넘지 않는지 확인
- [ ] 다른 플레이어가 화면 밖에서 보이지 않는지 확인
- [ ] 미니맵에 전체 맵이 표시되는지 확인
- [ ] 시야 범위가 올바르게 작동하는지 확인
- [ ] 네트워크 동기화 확인 (멀티플레이어)

## 예상 디렉토리 구조

```
NetFps/
├── src/
│   └── com/fpsgame/...
├── bin/
└── assets/                    # 새로 추가
    ├── maps/
    │   ├── map_01.png        # 기본 맵
    │   ├── map_02.png        # 도시 맵
    │   └── map_03.png        # 정글 맵
    └── characters/           # 캐릭터 스프라이트 (선택)
        ├── raven.png
        └── piper.png
```

## 다음 단계

1. **맵 이미지 제작/다운로드**
   - 온라인에서 탑뷰 FPS 맵 에셋 검색
   - 또는 간단한 맵을 직접 디자인 (Paint, GIMP, Photoshop)

2. **assets 폴더 생성**
   ```bash
   mkdir -p src/main/resources/assets/maps
   # 또는 Eclipse에서 src/main/resources 폴더 생성
   ```

3. **단계별 구현**
   - 1단계부터 순서대로 구현
   - 각 단계마다 테스트

4. **성능 최적화**
   - 화면 밖 객체는 그리지 않기
   - 이미지 캐싱
   - 충돌 감지 최적화 (QuadTree 등)

## 참고 자료

- Java BufferedImage: 이미지 로딩 및 렌더링
- 2D 카메라 시스템: 스크롤링 게임 구현
- 타일맵 vs 큰 이미지: 성능 비교
- FPS 맵 디자인 원칙: 3-lane 구조, 커버, 시야선

---

**현재 상태**: 시야 범위 시스템 완료, 맵 시스템 준비 완료 (주석으로 가이드 포함)
**다음 작업**: 맵 이미지 준비 및 1단계 구현
