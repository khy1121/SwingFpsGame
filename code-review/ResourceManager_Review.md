# ResourceManager.java 코드 리뷰

## 📋 파일 개요
- **경로**: `src/com/fpsgame/client/ResourceManager.java`
- **역할**: 이미지 및 스프라이트 시트 캐싱 관리
- **라인 수**: 118줄
- **디자인 패턴**: 싱글턴 (Singleton)
- **주요 기능**: 이미지 로드, 스프라이트 시트 분할, 캐싱

---

## 🎯 주요 기능

### 1. 싱글턴 패턴
```java
public class ResourceManager {
    /** 싱글턴 인스턴스 */
    private static ResourceManager instance;
    
    /**
     * private 생성자 (싱글턴 패턴)
     */
    private ResourceManager() {
    }
    
    /**
     * 싱글턴 인스턴스 가져오기
     */
    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }
}
```
**싱글턴 특징**:
- **전역 접근**: 어디서든 `ResourceManager.getInstance()` 호출
- **단일 인스턴스**: 메모리에 하나만 존재
- **캐시 공유**: 모든 코드가 같은 캐시 사용

**사용 예시**:
```java
// 어디서든 동일한 인스턴스 사용
BufferedImage img1 = ResourceManager.getInstance().getImage("assets/player.png");
BufferedImage img2 = ResourceManager.getInstance().getImage("assets/player.png");
// img1 == img2 (캐시에서 반환)
```

### 2. 이미지 캐싱
```java
/** 이미지 캐시 (경로 -> 이미지) */
private final Map<String, BufferedImage> images = new HashMap<>();

/**
 * 이미지 로드 (캐싱 지원)
 */
public BufferedImage getImage(String path) {
    // 1단계: 캐시 체크
    if (images.containsKey(path)) {
        return images.get(path); // 즉시 반환 (파일 I/O 없음)
    }
    
    // 2단계: 파일에서 로드
    try {
        BufferedImage img = ImageIO.read(new File(path));
        images.put(path, img); // 캐시에 저장
        return img;
    } catch (IOException e) {
        System.err.println("[ResourceManager] Failed to load image: " + path);
        e.printStackTrace(System.err);
        return null;
    }
}
```
**캐싱 메커니즘**:
1. **첫 호출**: 파일에서 로드 → 캐시 저장 (느림)
2. **두 번째 호출**: 캐시에서 반환 (빠름, 파일 I/O 없음)
3. **메모리 트레이드오프**: 메모리 사용 ↑, 속도 ↑

**성능 비교**:
```java
// 캐싱 없이 (매번 파일 로드)
for (int i = 0; i < 60; i++) { // 60fps
    BufferedImage img = ImageIO.read(new File("player.png")); // 100ms
    // 총 6000ms = 6초 (불가능!)
}

// 캐싱 있을 때 (한 번만 파일 로드)
BufferedImage img = ResourceManager.getInstance().getImage("player.png"); // 100ms (첫 호출)
for (int i = 0; i < 60; i++) { // 60fps
    BufferedImage cached = ResourceManager.getInstance().getImage("player.png"); // 0.01ms
    // 총 100ms + 0.6ms = 100.6ms (가능!)
}
```

### 3. 스프라이트 시트 분할

#### 스프라이트 시트란?
```
┌─────────────────────────────────────┐
│ Frame 0 │ Frame 1 │ Frame 2 │ Frame 3 │  <- 첫 번째 행
├─────────┼─────────┼─────────┼─────────┤
│ Frame 4 │ Frame 5 │ Frame 6 │ Frame 7 │  <- 두 번째 행
└─────────────────────────────────────┘
 ^         ^         ^         ^
 |         |         |         |
 0,0      64,0     128,0     192,0

전체 크기: 256x128 픽셀
각 프레임: 64x64 픽셀
총 프레임: 8개 (4열 × 2행)
```

#### 분할 로직
```java
/** 스프라이트 시트 캐시 (키 -> 스프라이트 배열) */
private final Map<String, BufferedImage[]> spriteSheets = new HashMap<>();

/**
 * 스프라이트 시트를 개별 프레임으로 분할 (캐싱 지원)
 */
public BufferedImage[] getSpriteSheet(String path, int frameWidth, int frameHeight) {
    // 1단계: 캐시 키 생성
    String key = path + "_" + frameWidth + "_" + frameHeight;
    // 예: "assets/walk.png_64_64"
    
    // 2단계: 캐시 체크
    if (spriteSheets.containsKey(key)) {
        return spriteSheets.get(key); // 즉시 반환
    }

    // 3단계: 스프라이트 시트 로드
    BufferedImage sheet = getImage(path); // 이미지 캐시 사용!
    if (sheet == null)
        return null;

    // 4단계: 그리드 계산
    int cols = sheet.getWidth() / frameWidth;   // 256 / 64 = 4열
    int rows = sheet.getHeight() / frameHeight; // 128 / 64 = 2행
    BufferedImage[] sprites = new BufferedImage[cols * rows]; // 8개 프레임

    // 5단계: 각 셀 추출
    for (int y = 0; y < rows; y++) {
        for (int x = 0; x < cols; x++) {
            sprites[y * cols + x] = sheet.getSubimage(
                x * frameWidth,  // X 좌표: 0, 64, 128, 192
                y * frameHeight, // Y 좌표: 0, 64
                frameWidth,      // 너비: 64
                frameHeight      // 높이: 64
            );
        }
    }

    // 6단계: 캐시에 저장하고 반환
    spriteSheets.put(key, sprites);
    return sprites;
}
```

**프레임 인덱스 계산**:
```
y=0, x=0 -> sprites[0*4 + 0] = sprites[0]  (첫 번째 프레임)
y=0, x=1 -> sprites[0*4 + 1] = sprites[1]  (두 번째 프레임)
y=0, x=2 -> sprites[0*4 + 2] = sprites[2]
y=0, x=3 -> sprites[0*4 + 3] = sprites[3]
y=1, x=0 -> sprites[1*4 + 0] = sprites[4]
y=1, x=1 -> sprites[1*4 + 1] = sprites[5]
y=1, x=2 -> sprites[1*4 + 2] = sprites[6]
y=1, x=3 -> sprites[1*4 + 3] = sprites[7]
```

**사용 예시**:
```java
// 걷기 애니메이션 (8프레임)
BufferedImage[] walkFrames = ResourceManager.getInstance()
    .getSpriteSheet("assets/walk.png", 64, 64);

// 애니메이션 재생
int currentFrame = 0;
void update() {
    currentFrame = (currentFrame + 1) % walkFrames.length; // 0~7 반복
    graphics.drawImage(walkFrames[currentFrame], x, y, null);
}
```

### 4. 캐시 키 생성
```java
// 이미지 캐시: 경로만 사용
String key1 = "assets/player.png";

// 스프라이트 시트 캐시: 경로 + 크기
String key2 = path + "_" + frameWidth + "_" + frameHeight;
// "assets/walk.png_64_64"
// "assets/walk.png_32_32" (다른 키!)
```
**같은 파일, 다른 크기**:
```java
// 같은 파일을 다른 크기로 분할 가능
BufferedImage[] frames64 = rm.getSpriteSheet("walk.png", 64, 64); // 4x2 = 8프레임
BufferedImage[] frames32 = rm.getSpriteSheet("walk.png", 32, 32); // 8x4 = 32프레임
// 두 개가 별도 캐시에 저장됨
```

---

## 💡 강점

### 1. 완벽한 싱글턴 구현
- **private 생성자**: 외부에서 new 불가
- **전역 접근점**: `getInstance()` 메서드
- **Lazy Initialization**: 첫 호출 시 인스턴스 생성

### 2. 효율적인 캐싱
```java
// 성능 비교 (1000번 호출)
// 캐싱 없음: 1000 × 100ms = 100초
// 캐싱 있음: 100ms + 999 × 0.01ms = 100.01ms (1000배 빠름!)
```
- **메모리 절약**: 같은 이미지 중복 로드 방지
- **속도 향상**: 파일 I/O 한 번만 수행

### 3. 스프라이트 시트 자동 분할
```java
// 수동 분할 (캐싱 없이)
BufferedImage sheet = ImageIO.read(new File("walk.png"));
BufferedImage frame0 = sheet.getSubimage(0, 0, 64, 64);
BufferedImage frame1 = sheet.getSubimage(64, 0, 64, 64);
// ... 8번 반복 (번거로움)

// 자동 분할 (캐싱 포함)
BufferedImage[] frames = rm.getSpriteSheet("walk.png", 64, 64);
// 한 줄로 완료!
```

### 4. null 안전 처리
```java
BufferedImage sheet = getImage(path);
if (sheet == null)
    return null; // 스프라이트 시트 로드 실패 시 안전하게 null 반환
```
- **NullPointerException 방지**: 파일 없을 때 크래시 대신 null 반환
- **에러 메시지**: `System.err.println()` 로그

### 5. 이미지 캐시 재사용
```java
public BufferedImage[] getSpriteSheet(String path, ...) {
    BufferedImage sheet = getImage(path); // 이미지 캐시 사용!
    // ...
}
```
- **2중 캐싱**: 원본 이미지 캐시 + 스프라이트 배열 캐시
- **메모리 효율**: 원본 이미지 한 번만 로드

---

## 🔧 개선 제안

### 1. 스레드 안전성 (중요도: 높음)
**현재 상태**: 싱글턴이 멀티스레드 환경에서 안전하지 않음

```java
public static ResourceManager getInstance() {
    if (instance == null) { // 스레드 A, B 동시 체크
        instance = new ResourceManager(); // 두 개 생성 가능!
    }
    return instance;
}
```

**문제점**:
- **Race Condition**: 두 스레드가 동시에 `instance == null` 체크
- **중복 생성**: 인스턴스가 2개 이상 생성될 수 있음

**제안 1: Synchronized (간단, 성능 저하)**
```java
public static synchronized ResourceManager getInstance() {
    if (instance == null) {
        instance = new ResourceManager();
    }
    return instance;
}
```

**제안 2: Double-Checked Locking (복잡, 성능 좋음)**
```java
private static volatile ResourceManager instance;

public static ResourceManager getInstance() {
    if (instance == null) { // 첫 번째 체크 (락 없음)
        synchronized (ResourceManager.class) {
            if (instance == null) { // 두 번째 체크 (락 있음)
                instance = new ResourceManager();
            }
        }
    }
    return instance;
}
```

**제안 3: Initialization-on-demand holder idiom (권장!)**
```java
public class ResourceManager {
    private ResourceManager() {}
    
    // 내부 클래스 (JVM이 스레드 안전 보장)
    private static class Holder {
        private static final ResourceManager INSTANCE = new ResourceManager();
    }
    
    public static ResourceManager getInstance() {
        return Holder.INSTANCE; // 스레드 안전, 성능 우수
    }
}
```

### 2. 메모리 관리 (중요도: 중간)
**현재 상태**: 이미지가 메모리에 무한정 유지

**문제점**:
- **메모리 누수**: 사용하지 않는 이미지도 계속 메모리 점유
- **OutOfMemoryError**: 많은 이미지 로드 시 메모리 부족

**제안 1: 수동 해제 메서드**
```java
/**
 * 특정 이미지 캐시 제거
 */
public void unloadImage(String path) {
    images.remove(path);
    System.out.println("[ResourceManager] Unloaded: " + path);
}

/**
 * 모든 이미지 캐시 제거
 */
public void clearCache() {
    images.clear();
    spriteSheets.clear();
    System.out.println("[ResourceManager] Cache cleared");
}
```

**제안 2: WeakHashMap (자동 GC)**
```java
// WeakReference를 사용한 자동 메모리 관리
private final Map<String, BufferedImage> images = new WeakHashMap<>();
// GC가 메모리 부족 시 자동으로 제거
```
**단점**: GC 타이밍 예측 불가, 성능 저하 가능

**제안 3: LRU 캐시 (Least Recently Used)**
```java
import java.util.LinkedHashMap;

private final Map<String, BufferedImage> images = new LinkedHashMap<String, BufferedImage>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
        return size() > 100; // 최대 100개 유지
    }
};
```

### 3. 비동기 로딩 (중요도: 중간)
**현재 상태**: 이미지 로드 시 블로킹

**문제점**:
- **UI 프리징**: 큰 이미지 로드 시 게임 멈춤
- **로딩 시간**: 여러 이미지 순차 로드 시 오래 걸림

**제안**:
```java
import java.util.concurrent.*;

public class ResourceManager {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    
    /**
     * 비동기 이미지 로드
     */
    public CompletableFuture<BufferedImage> getImageAsync(String path) {
        // 캐시에 있으면 즉시 반환
        if (images.containsKey(path)) {
            return CompletableFuture.completedFuture(images.get(path));
        }
        
        // 백그라운드에서 로드
        return CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage img = ImageIO.read(new File(path));
                images.put(path, img);
                return img;
            } catch (IOException e) {
                System.err.println("[ResourceManager] Failed to load: " + path);
                return null;
            }
        }, executor);
    }
    
    /**
     * 여러 이미지 동시 로드
     */
    public CompletableFuture<Void> preloadImagesAsync(String... paths) {
        CompletableFuture<?>[] futures = new CompletableFuture[paths.length];
        for (int i = 0; i < paths.length; i++) {
            futures[i] = getImageAsync(paths[i]);
        }
        return CompletableFuture.allOf(futures);
    }
}

// 사용 예시
ResourceManager rm = ResourceManager.getInstance();
rm.preloadImagesAsync("player.png", "enemy.png", "map.png")
  .thenRun(() -> System.out.println("모든 이미지 로드 완료!"));
```

### 4. 에러 처리 개선 (중요도: 중간)
**현재 상태**: 에러 시 null 반환

**문제점**:
- **null 처리 부담**: 호출자가 null 체크 필수
- **에러 정보 부족**: 왜 실패했는지 알 수 없음

**제안 1: Optional 반환**
```java
import java.util.Optional;

/**
 * 이미지 로드 (Optional 반환)
 */
public Optional<BufferedImage> getImageSafe(String path) {
    if (images.containsKey(path)) {
        return Optional.of(images.get(path));
    }
    
    try {
        BufferedImage img = ImageIO.read(new File(path));
        images.put(path, img);
        return Optional.of(img);
    } catch (IOException e) {
        System.err.println("[ResourceManager] Failed: " + path);
        return Optional.empty();
    }
}

// 사용
rm.getImageSafe("player.png")
  .ifPresent(img -> graphics.drawImage(img, x, y, null));
```

**제안 2: 기본 이미지 제공**
```java
private BufferedImage defaultImage;

public BufferedImage getImage(String path) {
    // ... (기존 로직)
    catch (IOException e) {
        System.err.println("[ResourceManager] Failed: " + path + ", using default");
        return getDefaultImage(); // null 대신 기본 이미지
    }
}

private BufferedImage getDefaultImage() {
    if (defaultImage == null) {
        // 1x1 빨간 사각형 생성
        defaultImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        defaultImage.setRGB(0, 0, 0xFF0000); // 빨간색
    }
    return defaultImage;
}
```

### 5. 로깅 개선 (중요도: 낮음)
**현재 상태**: `System.err.println()` 직접 호출

**제안**:
```java
import java.util.logging.*;

public class ResourceManager {
    private static final Logger LOGGER = Logger.getLogger(ResourceManager.class.getName());
    
    public BufferedImage getImage(String path) {
        if (images.containsKey(path)) {
            LOGGER.fine("Cache hit: " + path); // 디버그 레벨
            return images.get(path);
        }
        
        try {
            LOGGER.info("Loading image: " + path);
            BufferedImage img = ImageIO.read(new File(path));
            images.put(path, img);
            LOGGER.info("Loaded successfully: " + path);
            return img;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load: " + path, e);
            return null;
        }
    }
}
```

### 6. 캐시 통계 (중요도: 낮음)
**현재 상태**: 캐시 효율 모름

**제안**:
```java
private int cacheHits = 0;
private int cacheMisses = 0;

public BufferedImage getImage(String path) {
    if (images.containsKey(path)) {
        cacheHits++; // 캐시 히트
        return images.get(path);
    }
    
    cacheMisses++; // 캐시 미스
    // ... (기존 로직)
}

/**
 * 캐시 통계 출력
 */
public void printStats() {
    int total = cacheHits + cacheMisses;
    float hitRate = total > 0 ? (float) cacheHits / total * 100 : 0;
    
    System.out.println("[ResourceManager Stats]");
    System.out.println("  Cache Hits: " + cacheHits);
    System.out.println("  Cache Misses: " + cacheMisses);
    System.out.println("  Hit Rate: " + String.format("%.2f%%", hitRate));
    System.out.println("  Images Cached: " + images.size());
    System.out.println("  Sprite Sheets Cached: " + spriteSheets.size());
}
```

### 7. 스프라이트 시트 검증 (중요도: 낮음)
**현재 상태**: 잘못된 크기 입력 시 예외 발생

**문제점**:
```java
// 256x128 이미지를 100x100으로 분할 시도
BufferedImage[] sprites = rm.getSpriteSheet("walk.png", 100, 100);
// cols = 256 / 100 = 2
// rows = 128 / 100 = 1
// 일부만 추출됨 (의도치 않음)
```

**제안**:
```java
public BufferedImage[] getSpriteSheet(String path, int frameWidth, int frameHeight) {
    // ... (기존 로직)
    
    BufferedImage sheet = getImage(path);
    if (sheet == null) return null;
    
    // 크기 검증
    if (sheet.getWidth() % frameWidth != 0) {
        System.err.println("[경고] 이미지 너비(" + sheet.getWidth() + 
                          ")가 프레임 너비(" + frameWidth + ")로 나누어떨어지지 않음");
    }
    if (sheet.getHeight() % frameHeight != 0) {
        System.err.println("[경고] 이미지 높이(" + sheet.getHeight() + 
                          ")가 프레임 높이(" + frameHeight + ")로 나누어떨어지지 않음");
    }
    
    // ... (나머지 로직)
}
```

---

## 📊 코드 품질 평가

| 항목 | 점수 | 설명 |
|------|------|------|
| **싱글턴 패턴** | ⭐⭐⭐⭐☆ | 구현 완벽, 스레드 안전성 부족 |
| **캐싱 효율** | ⭐⭐⭐⭐⭐ | 이미지, 스프라이트 시트 2중 캐싱 |
| **코드 간결성** | ⭐⭐⭐⭐⭐ | 118줄, 명확한 로직 |
| **에러 처리** | ⭐⭐⭐☆☆ | null 반환, 기본 이미지 없음 |
| **메모리 관리** | ⭐⭐⭐☆☆ | 수동 해제 메서드 없음 |
| **확장성** | ⭐⭐⭐⭐☆ | 비동기 로딩 미지원 |

**총점: 4.0 / 5.0** ⭐⭐⭐⭐☆

---

## 🎓 결론

ResourceManager.java는 **효율적이고 간결한 리소스 관리 시스템**입니다. 특히 **싱글턴 패턴**, **2중 캐싱**, **스프라이트 시트 자동 분할**이 인상적입니다.

### 주요 성과
1. ✅ **싱글턴 패턴**: 전역 접근, 단일 인스턴스
2. ✅ **2중 캐싱**: 이미지 캐시 + 스프라이트 시트 캐시
3. ✅ **자동 분할**: 스프라이트 시트 → 프레임 배열 (한 줄)
4. ✅ **캐시 재사용**: 스프라이트 시트가 이미지 캐시 사용
5. ✅ **null 안전**: 파일 없을 때 크래시 대신 null 반환

### 개선 방향
1. **스레드 안전성**: Initialization-on-demand holder 패턴 적용
2. **메모리 관리**: `clearCache()`, LRU 캐시 추가
3. **비동기 로딩**: `getImageAsync()`, `preloadImagesAsync()` 추가
4. **기본 이미지**: null 대신 1x1 빨간 사각형 반환

**프로덕션 레벨**이며, 스레드 안전성만 개선하면 **완벽한 리소스 관리자**입니다. 🎉
