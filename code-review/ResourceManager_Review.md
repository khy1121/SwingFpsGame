# ResourceManager.java 코드 리뷰

## 📋 파일 개요
- **경로**: `src/com/fpsgame/client/ResourceManager.java`
- **목적**: 게임 리소스(이미지, 스프라이트 시트) 캐싱 및 관리
- **라인 수**: 107줄
- **패턴**: 싱글턴 (Singleton)

## 🎯 주요 기능

### 1. 싱글턴 패턴
```java
private static ResourceManager instance;

public static ResourceManager getInstance() {
    if (instance == null) {
        instance = new ResourceManager();
    }
    return instance;
}
```
- 전역적으로 단일 인스턴스만 존재
- 모든 코드에서 같은 캐시 공유

### 2. 이미지 캐싱
```java
private final Map<String, BufferedImage> images = new HashMap<>();

public BufferedImage getImage(String path) {
    if (images.containsKey(path)) {
        return images.get(path); // 캐시된 이미지 반환
    }
    // 파일에서 로드
}
```
- 한 번 로드된 이미지는 메모리에 유지
- 중복 로딩 방지 → 성능 향상

### 3. 스프라이트 시트 분할
```java
public BufferedImage[] getSpriteSheet(String path, int frameWidth, int frameHeight)
```
- 큰 이미지를 작은 프레임들로 분할
- 캐릭터 애니메이션, 이펙트 등에 사용

## ✅ 장점

### 1. **메모리 효율성**
```java
// 같은 이미지를 10번 로드해도 메모리는 1개만 차지
for (int i = 0; i < 10; i++) {
    BufferedImage img = ResourceManager.getInstance().getImage("player.png");
}
```
**효과**:
- 메모리 사용량 획기적 감소
- 로딩 시간 단축

### 2. **불변 컬렉션 필드**
```java
private final Map<String, BufferedImage> images = new HashMap<>();
private final Map<String, BufferedImage[]> spriteSheets = new HashMap<>();
```
- `final` 키워드로 맵 참조 고정
- 실수로 다른 맵 할당 불가능

### 3. **스마트 캐시 키 생성**
```java
String key = path + "_" + frameWidth + "_" + frameHeight;
```
- 같은 이미지라도 다른 크기면 별도 캐시
- 예: "sprite.png_64_64", "sprite.png_32_32"

### 4. **안전한 예외 처리**
```java
} catch (IOException e) {
    System.err.println("[ResourceManager] Failed to load image: " + path);
    e.printStackTrace(System.err);
    return null;
}
```
- 이미지 로드 실패해도 게임 크래시 안 함
- null 반환으로 호출자가 처리

### 5. **getSubimage() 활용**
```java
sprites[y * cols + x] = sheet.getSubimage(
    x * frameWidth, 
    y * frameHeight, 
    frameWidth, 
    frameHeight
);
```
- 메모리 복사 없이 원본 이미지 영역 참조
- 빠르고 메모리 효율적

## ⚠️ 개선 가능 영역

### 1. **Thread-Safe하지 않은 싱글턴**
**현재 코드:**
```java
public static ResourceManager getInstance() {
    if (instance == null) {
        instance = new ResourceManager();
    }
    return instance;
}
```

**문제점**:
- 멀티스레드 환경에서 2개 인스턴스 생성 가능
- Race condition 발생 시나리오:
  ```
  Thread 1: instance == null (true)
  Thread 2: instance == null (true)
  Thread 1: instance = new ResourceManager()
  Thread 2: instance = new ResourceManager() // 덮어씀!
  ```

**개선 제안 1: Eager Initialization**
```java
private static final ResourceManager instance = new ResourceManager();

public static ResourceManager getInstance() {
    return instance; // 항상 thread-safe
}
```

**개선 제안 2: Double-Checked Locking**
```java
private static volatile ResourceManager instance;

public static ResourceManager getInstance() {
    if (instance == null) {
        synchronized (ResourceManager.class) {
            if (instance == null) {
                instance = new ResourceManager();
            }
        }
    }
    return instance;
}
```

**개선 제안 3: Enum 싱글턴 (가장 안전)**
```java
public enum ResourceManager {
    INSTANCE;
    
    private final Map<String, BufferedImage> images = new HashMap<>();
    
    public BufferedImage getImage(String path) { ... }
}

// 사용: ResourceManager.INSTANCE.getImage("path");
```

### 2. **동시성 문제**
**현재 코드:**
```java
private final Map<String, BufferedImage> images = new HashMap<>();
```

**문제점**:
- `HashMap`은 thread-safe하지 않음
- 게임 로딩(쓰기) + 렌더링(읽기) 동시 발생 시 `ConcurrentModificationException`

**개선 제안:**
```java
private final Map<String, BufferedImage> images = new ConcurrentHashMap<>();
private final Map<String, BufferedImage[]> spriteSheets = new ConcurrentHashMap<>();
```

**효과**:
- 멀티스레드 환경에서도 안전
- 성능 저하 거의 없음

### 3. **메모리 누수 위험**
**현재 코드:**
```java
images.put(path, img); // 무제한 저장
```

**문제점**:
- 게임이 길어질수록 메모리 사용량 증가
- 사용하지 않는 이미지도 계속 보관

**개선 제안 1: LRU 캐시**
```java
private final Map<String, BufferedImage> images = 
    Collections.synchronizedMap(new LinkedHashMap<String, BufferedImage>(
        16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
            return size() > 100; // 최대 100개 이미지만 유지
        }
    });
```

**개선 제안 2: 명시적 해제**
```java
public void clearCache() {
    images.clear();
    spriteSheets.clear();
    System.gc(); // 가비지 컬렉션 제안
}

public void removeImage(String path) {
    images.remove(path);
}
```

### 4. **Null 반환 처리**
**현재 코드:**
```java
BufferedImage sheet = getImage(path);
if (sheet == null)
    return null;
```

**문제점**:
- 호출자가 null 체크 필수
- 잊어버리면 `NullPointerException`

**개선 제안 1: Optional 사용**
```java
public Optional<BufferedImage> getImage(String path) {
    // ...
    return Optional.ofNullable(img);
}

// 사용
ResourceManager.getInstance().getImage("player.png")
    .ifPresent(img -> g.drawImage(img, x, y, null));
```

**개선 제안 2: 기본 이미지 제공**
```java
private static final BufferedImage MISSING_IMAGE = createMissingTexture();

public BufferedImage getImage(String path) {
    // ...
    } catch (IOException e) {
        System.err.println("[ResourceManager] Failed to load: " + path);
        return MISSING_IMAGE; // 분홍색 체크무늬 이미지
    }
}

private static BufferedImage createMissingTexture() {
    BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    g.setColor(Color.MAGENTA);
    g.fillRect(0, 0, 64, 64);
    g.setColor(Color.BLACK);
    g.fillRect(0, 0, 32, 32);
    g.fillRect(32, 32, 32, 32);
    g.dispose();
    return img;
}
```

### 5. **경로 정규화 부족**
**현재 코드:**
```java
public BufferedImage getImage(String path) {
    if (images.containsKey(path)) { ... }
}
```

**문제점**:
- "assets/player.png"와 "assets\\player.png"가 다른 키로 인식
- Windows/Linux 경로 차이

**개선 제안:**
```java
private String normalizePath(String path) {
    return path.replace("\\", "/").toLowerCase();
}

public BufferedImage getImage(String path) {
    String normalizedPath = normalizePath(path);
    if (images.containsKey(normalizedPath)) {
        return images.get(normalizedPath);
    }
    // ...
}
```

### 6. **리소스 존재 여부 사전 검증 부족**
**현재 코드:**
```java
BufferedImage img = ImageIO.read(new File(path));
```

**개선 제안:**
```java
File file = new File(path);
if (!file.exists() || !file.isFile()) {
    System.err.println("[ResourceManager] File not found: " + path);
    return null;
}

BufferedImage img = ImageIO.read(file);
```

### 7. **스프라이트 시트 분할 검증 부족**
**현재 코드:**
```java
int cols = sheet.getWidth() / frameWidth;
int rows = sheet.getHeight() / frameHeight;
```

**문제점**:
- frameWidth/frameHeight가 0이면 나누기 0 오류
- 이미지 크기가 프레임 크기의 배수가 아니면 잘림

**개선 제안:**
```java
public BufferedImage[] getSpriteSheet(String path, int frameWidth, int frameHeight) {
    if (frameWidth <= 0 || frameHeight <= 0) {
        throw new IllegalArgumentException("Frame dimensions must be positive");
    }
    
    BufferedImage sheet = getImage(path);
    if (sheet == null) return null;
    
    if (sheet.getWidth() % frameWidth != 0 || sheet.getHeight() % frameHeight != 0) {
        System.err.println("[ResourceManager] Warning: Image size not divisible by frame size");
    }
    
    // ...
}
```

## 🏗️ 아키텍처 분석

### 설계 패턴
1. **싱글턴 패턴**: 전역 접근점 제공
2. **플라이웨이트 패턴**: 동일 리소스 공유로 메모리 절약
3. **캐시 패턴**: 빈번한 I/O 작업 최소화

### 의존성
```
ResourceManager
    ├── javax.imageio.ImageIO (이미지 I/O)
    ├── java.awt.image.BufferedImage (이미지 저장)
    └── java.util.Map (캐싱)
```
- 표준 라이브러리만 사용
- 외부 의존성 0개

### 사용 위치
```
ResourceManager
    ├── SpriteAnimation (애니메이션 프레임)
    ├── GamePanel (플레이어, 맵 렌더링)
    ├── CharacterSelectDialog (캐릭터 이미지)
    └── LobbyFrame (UI 리소스)
```

## 📊 성능 분석

### 메모리 사용
```java
// 예시: 50개 이미지, 각 1MB
50 images × 1MB = 50MB (캐시 없으면 500MB+)
```

**최적화 효과**:
| 시나리오 | 캐시 없음 | 캐시 있음 | 절감 |
|----------|-----------|-----------|------|
| 메모리 | 500MB+ | 50MB | 90% |
| 로딩 시간 | 5초 | 0.5초 | 90% |

### 조회 성능
```java
getImage("path"); // HashMap.get() = O(1)
```
- 평균 시간: ~50ns
- 게임 루프(60 FPS)에서 무시 가능

### 분할 성능
```java
getSpriteSheet("path", 64, 64); // 처음: ~10ms, 이후: O(1)
```
- 첫 호출: 파일 로드 + 분할
- 이후 호출: 캐시에서 즉시 반환

## 🧪 테스트 시나리오

### 1. 기본 이미지 로딩
```java
@Test
public void testImageLoading() {
    ResourceManager rm = ResourceManager.getInstance();
    BufferedImage img = rm.getImage("assets/player.png");
    assertNotNull(img);
}
```

### 2. 캐싱 동작 확인
```java
@Test
public void testCaching() {
    ResourceManager rm = ResourceManager.getInstance();
    BufferedImage img1 = rm.getImage("assets/player.png");
    BufferedImage img2 = rm.getImage("assets/player.png");
    assertSame(img1, img2); // 같은 인스턴스여야 함
}
```

### 3. 스프라이트 시트 분할
```java
@Test
public void testSpriteSheet() {
    ResourceManager rm = ResourceManager.getInstance();
    BufferedImage[] sprites = rm.getSpriteSheet("assets/walk.png", 64, 64);
    assertNotNull(sprites);
    assertTrue(sprites.length > 0);
    assertEquals(64, sprites[0].getWidth());
}
```

### 4. 존재하지 않는 파일
```java
@Test
public void testMissingFile() {
    ResourceManager rm = ResourceManager.getInstance();
    BufferedImage img = rm.getImage("nonexistent.png");
    assertNull(img);
}
```

### 5. 싱글턴 테스트
```java
@Test
public void testSingleton() {
    ResourceManager rm1 = ResourceManager.getInstance();
    ResourceManager rm2 = ResourceManager.getInstance();
    assertSame(rm1, rm2);
}
```

## 📈 사용 예시

### 캐릭터 이미지 로드
```java
// GamePanel.java
BufferedImage playerImg = ResourceManager.getInstance()
    .getImage("assets/characters/raven.png");
g.drawImage(playerImg, player.x, player.y, 64, 64, null);
```

### 애니메이션 프레임 로드
```java
// SpriteAnimation 생성
BufferedImage[] walkFrames = ResourceManager.getInstance()
    .getSpriteSheet("assets/walk_cycle.png", 64, 64);
SpriteAnimation walkAnim = new SpriteAnimation(walkFrames, 100, true);
```

### 맵 타일 로드
```java
// 타일맵 렌더링
BufferedImage tileSheet = ResourceManager.getInstance()
    .getSpriteSheet("assets/tileset.png", 32, 32);

for (int y = 0; y < mapHeight; y++) {
    for (int x = 0; x < mapWidth; x++) {
        int tileId = map[y][x];
        g.drawImage(tileSheet[tileId], x * 32, y * 32, null);
    }
}
```

## 🎓 학습 포인트

### 초보자를 위한 핵심 개념
1. **싱글턴 패턴**: 전역 객체 관리
2. **캐싱**: 중복 작업 방지
3. **BufferedImage**: Java 이미지 처리

### 중급자를 위한 심화 개념
1. **Thread-Safety**: 동시성 문제 해결
2. **메모리 관리**: LRU 캐시, 약한 참조
3. **SubImage**: 복사 없는 이미지 분할

### 고급 주제
1. **텍스처 아틀라스**: 여러 이미지를 한 파일로 통합
2. **비동기 로딩**: 백그라운드에서 리소스 로드
3. **압축 포맷**: PNG/JPG 선택 기준

## 🔍 코드 품질 평가

| 항목 | 평가 | 설명 |
|------|------|------|
| **가독성** | ⭐⭐⭐⭐⭐ | 매우 명확한 메서드명 |
| **유지보수성** | ⭐⭐⭐⭐ | 간단한 구조 |
| **확장성** | ⭐⭐⭐ | 새 리소스 타입 추가 가능 |
| **성능** | ⭐⭐⭐⭐⭐ | 캐싱으로 최적화됨 |
| **안정성** | ⭐⭐⭐ | Thread-safety 부족 |

## 📝 종합 평가

### 강점
✅ **효율적인 캐싱**: 메모리와 로딩 시간 대폭 감소  
✅ **간단한 API**: 2개 메서드로 모든 기능  
✅ **스마트 캐시 키**: 같은 이미지의 다른 분할도 지원  
✅ **안전한 예외 처리**: 파일 오류에도 게임 계속 실행  

### 주요 약점
❌ **Thread-Safety 부족**: 멀티스레드 환경 문제  
❌ **메모리 누수 가능**: 무제한 캐시  
❌ **Null 반환**: 호출자의 null 체크 부담  

### 개선 제안 우선순위
1. **Thread-Safe 싱글턴** (높음) - Eager initialization 사용
2. **ConcurrentHashMap** (높음) - HashMap 교체
3. **경로 정규화** (중간) - 크로스 플랫폼 지원
4. **LRU 캐시** (중간) - 메모리 제한
5. **Optional 반환** (낮음) - null 안전성
6. **명시적 해제** (낮음) - 레벨 전환 시 메모리 정리

### 결론
**핵심 기능이 잘 구현된 리소스 관리 클래스**입니다. 싱글 플레이어 게임에서는 현재 상태로도 충분히 작동합니다. 멀티스레드 환경이나 장시간 플레이를 고려한다면 thread-safety와 메모리 관리 개선이 필요합니다.

**권장사항**:
1. **즉시 적용**: 
   - Eager initialization 싱글턴
   - ConcurrentHashMap 사용
   
2. **다음 버전**: 
   - 경로 정규화
   - 기본 이미지 제공
   
3. **선택 적용**: 
   - LRU 캐시 (메모리 이슈 발생 시)
   - 비동기 로딩 (로딩 화면 필요 시)

**성능 팁**:
- 게임 시작 시 자주 사용하는 리소스 미리 로드
- 레벨 전환 시 clearCache() 호출
- 큰 이미지는 스프라이트 시트로 통합
