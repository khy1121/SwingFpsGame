# SpriteAnimation.java 코드 리뷰

## 📋 파일 개요
- **경로**: `src/com/fpsgame/client/SpriteAnimation.java`
- **역할**: 스프라이트 애니메이션 관리
- **라인 수**: 113줄
- **주요 기능**: 프레임 기반 애니메이션, 반복/일회성 재생, 자동 프레임 전환
- **사용 사례**: 캐릭터 워킹, 스킬 이펙트, 폭발 효과

---

## 🎯 주요 기능

### 1. 프레임 기반 애니메이션
```java
public class SpriteAnimation {
    /** 애니메이션 프레임 배열 */
    private final BufferedImage[] frames;
    
    /** 현재 표시 중인 프레임 인덱스 */
    private int currentFrame;
    
    /** 마지막 프레임 갱신 시간 */
    private long lastTime;
    
    /** 각 프레임의 표시 시간 (밀리초) */
    private final long frameDuration;
    
    /** 애니메이션 반복 여부 */
    private final boolean loop;
    
    /** 애니메이션 종료 여부 (loop=false일 때만 의미) */
    private boolean isFinished;
}
```
**프레임 애니메이션 원리**:
```
프레임 0 -> 프레임 1 -> 프레임 2 -> 프레임 3 -> 프레임 0 (반복)
   100ms      100ms      100ms      100ms       100ms
   
loop=true:  0 -> 1 -> 2 -> 3 -> 0 -> 1 -> ...  (무한 반복)
loop=false: 0 -> 1 -> 2 -> 3 (정지, isFinished=true)
```

### 2. 생성자
```java
/**
 * 스프라이트 애니메이션 생성자
 * 
 * @param frames 애니메이션 프레임 이미지 배열
 * @param frameDuration 각 프레임의 표시 시간 (밀리초)
 * @param loop 애니메이션 반복 여부
 */
public SpriteAnimation(BufferedImage[] frames, long frameDuration, boolean loop) {
    this.frames = frames;
    this.frameDuration = frameDuration;
    this.loop = loop;
    this.currentFrame = 0;
    this.lastTime = System.currentTimeMillis();
    this.isFinished = false;
}
```
**사용 예시**:
```java
// 걷기 애니메이션 (8프레임, 100ms/프레임, 반복)
BufferedImage[] walkFrames = ResourceManager.getInstance()
    .getSpriteSheet("assets/walk.png", 64, 64);
SpriteAnimation walkAnim = new SpriteAnimation(walkFrames, 100, true);

// 폭발 이펙트 (6프레임, 50ms/프레임, 일회성)
BufferedImage[] explosionFrames = ResourceManager.getInstance()
    .getSpriteSheet("assets/explosion.png", 128, 128);
SpriteAnimation explosionAnim = new SpriteAnimation(explosionFrames, 50, false);
```

### 3. 프레임 업데이트 로직
```java
/**
 * 애니메이션 상태 업데이트
 */
public void update() {
    // 이미 종료된 애니메이션은 업데이트하지 않음
    if (isFinished)
        return;

    long now = System.currentTimeMillis();
    
    // 프레임 지속 시간이 지났는지 확인
    if (now - lastTime >= frameDuration) {
        currentFrame++;
        lastTime = now;
        
        // 마지막 프레임에 도달했을 때 처리
        if (currentFrame >= frames.length) {
            if (loop) {
                // 반복 애니메이션: 첫 프레임으로 돌아감
                currentFrame = 0;
            } else {
                // 일회성 애니메이션: 마지막 프레임에서 정지
                currentFrame = frames.length - 1;
                isFinished = true;
            }
        }
    }
}
```
**타임라인 예시 (4프레임, 100ms/프레임, loop=true)**:
```
시간(ms):  0     100    200    300    400    500    600    700
프레임:    0      1      2      3      0      1      2      3
           ↑      ↑      ↑      ↑      ↑
         lastTime 갱신
```

**타임라인 예시 (4프레임, 100ms/프레임, loop=false)**:
```
시간(ms):  0     100    200    300    400+
프레임:    0      1      2      3      3 (정지)
                                      ↑
                               isFinished=true
```

### 4. 프레임 렌더링
```java
/**
 * 현재 프레임을 화면에 그리기
 * 
 * @param g Graphics2D 컨텍스트
 * @param x 그릴 위치 X 좌표
 * @param y 그릴 위치 Y 좌표
 * @param width 그릴 너비
 * @param height 그릴 높이
 */
public void draw(Graphics2D g, int x, int y, int width, int height) {
    if (frames != null && frames.length > 0) {
        g.drawImage(frames[currentFrame], x, y, width, height, null);
    }
}
```
**null 안전 처리**:
- `frames != null`: 배열 존재 확인
- `frames.length > 0`: 빈 배열 체크
- `currentFrame`: 항상 유효한 인덱스 (update()에서 보장)

**사용 예시**:
```java
// 게임 루프
while (running) {
    // 1. 업데이트
    walkAnim.update();
    
    // 2. 렌더링
    walkAnim.draw(g2d, player.x, player.y, 64, 64);
    
    Thread.sleep(16); // ~60fps
}
```

### 5. 애니메이션 리셋
```java
/**
 * 애니메이션을 처음부터 다시 시작
 */
public void reset() {
    currentFrame = 0;
    lastTime = System.currentTimeMillis();
    isFinished = false;
}
```
**사용 사례**:
```java
// 일회성 애니메이션 재생
if (explosionAnim.isFinished()) {
    explosionAnim.reset(); // 처음부터 다시
}

// 캐릭터 상태 변경 시
if (player.isWalking()) {
    walkAnim.reset(); // 걷기 시작 시 첫 프레임부터
}
```

### 6. 종료 상태 확인
```java
/**
 * 애니메이션 종료 여부 확인
 * 
 * @return 애니메이션이 종료되었으면 true (loop=false일 때만 의미)
 */
public boolean isFinished() {
    return isFinished;
}
```
**사용 예시**:
```java
// 폭발 이펙트 제거
if (explosionAnim.isFinished()) {
    effects.remove(explosion); // 리스트에서 제거
}

// 스킬 이펙트 종료 후 처리
if (skillEffect.isFinished()) {
    player.skill.deactivate();
}
```

---

## 💡 강점

### 1. 간결한 구조
- **113줄**: 핵심 기능만 포함
- **명확한 책임**: 프레임 전환만 담당
- **의존성 없음**: BufferedImage만 사용

### 2. 시간 기반 애니메이션
```java
long now = System.currentTimeMillis();
if (now - lastTime >= frameDuration) {
    currentFrame++;
    lastTime = now;
}
```
- **프레임 독립적**: 60fps, 30fps 상관없이 동일한 속도
- **정확한 타이밍**: `System.currentTimeMillis()` 사용

### 3. 반복/일회성 지원
```java
if (loop) {
    currentFrame = 0; // 반복
} else {
    currentFrame = frames.length - 1; // 정지
    isFinished = true;
}
```
- **유연성**: 워킹(반복) vs 폭발(일회성)
- **자동 정지**: isFinished 플래그

### 4. null 안전 처리
```java
if (frames != null && frames.length > 0) {
    g.drawImage(frames[currentFrame], ...);
}
```
- **크래시 방지**: 빈 프레임 배열도 안전

### 5. 종료 애니메이션 최적화
```java
if (isFinished)
    return; // 업데이트 스킵
```
- **CPU 절약**: 종료된 애니메이션은 업데이트 안 함

---

## 🔧 개선 제안

### 1. deltaTime 기반 업데이트 (중요도: 높음)
**현재 상태**: `System.currentTimeMillis()` 사용

**문제점**:
- **정확도**: 밀리초 단위 (1ms 오차 가능)
- **프레임 드랍**: 게임 루프가 느려지면 애니메이션도 느려짐

**제안**:
```java
/**
 * deltaTime 기반 업데이트 (더 정확함)
 * 
 * @param deltaTime 프레임 경과 시간 (초)
 */
public void update(float deltaTime) {
    if (isFinished)
        return;
    
    // 밀리초를 초로 변환
    frameTimer += deltaTime;
    
    // 프레임 지속 시간이 지났는지 확인
    if (frameTimer >= frameDuration / 1000f) {
        currentFrame++;
        frameTimer -= frameDuration / 1000f; // 남은 시간 유지
        
        if (currentFrame >= frames.length) {
            if (loop) {
                currentFrame = 0;
            } else {
                currentFrame = frames.length - 1;
                isFinished = true;
            }
        }
    }
}

// 추가 필드
private float frameTimer = 0f;
```

### 2. 애니메이션 속도 배수 (중요도: 중간)
**현재 상태**: 고정된 frameDuration

**제안**:
```java
private float speedMultiplier = 1f; // 기본값 1.0 (100%)

/**
 * 애니메이션 속도 배수 설정
 * 
 * @param multiplier 속도 배수 (0.5 = 50% 느림, 2.0 = 2배 빠름)
 */
public void setSpeedMultiplier(float multiplier) {
    this.speedMultiplier = Math.max(0.1f, multiplier); // 최소 0.1배
}

public void update() {
    if (isFinished) return;
    
    long now = System.currentTimeMillis();
    long effectiveDuration = (long) (frameDuration / speedMultiplier);
    
    if (now - lastTime >= effectiveDuration) {
        currentFrame++;
        lastTime = now;
        // ... (기존 로직)
    }
}

// 사용 예시
walkAnim.setSpeedMultiplier(1.5f); // 1.5배 빠르게 걷기
```

### 3. 프레임 범위 재생 (중요도: 낮음)
**현재 상태**: 전체 프레임만 재생

**제안**:
```java
private int startFrame = 0;
private int endFrame;

public SpriteAnimation(BufferedImage[] frames, long frameDuration, boolean loop,
                       int startFrame, int endFrame) {
    this.frames = frames;
    this.frameDuration = frameDuration;
    this.loop = loop;
    this.startFrame = startFrame;
    this.endFrame = endFrame;
    this.currentFrame = startFrame;
    this.lastTime = System.currentTimeMillis();
    this.isFinished = false;
}

public void update() {
    // ... (기존 로직)
    
    if (currentFrame > endFrame) { // >= 대신 >
        if (loop) {
            currentFrame = startFrame; // 0 대신 startFrame
        } else {
            currentFrame = endFrame;
            isFinished = true;
        }
    }
}

// 사용 예시
// 8프레임 중 0~3만 재생 (걷기 방향별)
SpriteAnimation walkLeft = new SpriteAnimation(frames, 100, true, 0, 3);
SpriteAnimation walkRight = new SpriteAnimation(frames, 100, true, 4, 7);
```

### 4. 콜백 시스템 (중요도: 중간)
**현재 상태**: 외부에서 isFinished() 체크 필요

**제안**:
```java
private Runnable onComplete;
private Runnable onLoop;

/**
 * 애니메이션 완료 시 콜백 설정
 */
public void setOnComplete(Runnable callback) {
    this.onComplete = callback;
}

/**
 * 애니메이션 루프 시 콜백 설정
 */
public void setOnLoop(Runnable callback) {
    this.onLoop = callback;
}

public void update() {
    // ... (기존 로직)
    
    if (currentFrame >= frames.length) {
        if (loop) {
            currentFrame = 0;
            if (onLoop != null) {
                onLoop.run(); // 루프 콜백 실행
            }
        } else {
            currentFrame = frames.length - 1;
            isFinished = true;
            if (onComplete != null) {
                onComplete.run(); // 완료 콜백 실행
            }
        }
    }
}

// 사용 예시
explosionAnim.setOnComplete(() -> {
    effects.remove(explosion);
    playSound("explosion_end.wav");
});

walkAnim.setOnLoop(() -> {
    // 발소리 재생
    playSound("footstep.wav");
});
```

### 5. 역재생 지원 (중요도: 낮음)
**현재 상태**: 정방향만 재생

**제안**:
```java
private boolean reverse = false;
private int direction = 1; // 1: 정방향, -1: 역방향

/**
 * 역재생 설정
 */
public void setReverse(boolean reverse) {
    this.reverse = reverse;
    this.direction = reverse ? -1 : 1;
}

public void update() {
    if (isFinished) return;
    
    long now = System.currentTimeMillis();
    
    if (now - lastTime >= frameDuration) {
        currentFrame += direction; // 방향에 따라 증가/감소
        lastTime = now;
        
        if (direction > 0 && currentFrame >= frames.length) {
            // 정방향 끝
            if (loop) {
                currentFrame = 0;
            } else {
                currentFrame = frames.length - 1;
                isFinished = true;
            }
        } else if (direction < 0 && currentFrame < 0) {
            // 역방향 끝
            if (loop) {
                currentFrame = frames.length - 1;
            } else {
                currentFrame = 0;
                isFinished = true;
            }
        }
    }
}
```

### 6. 프레임 스킵 방지 (중요도: 중간)
**현재 상태**: 프레임 드랍 시 애니메이션 느려짐

**제안**:
```java
public void update() {
    if (isFinished) return;
    
    long now = System.currentTimeMillis();
    long elapsed = now - lastTime;
    
    // 여러 프레임을 한 번에 건너뛰기 가능
    while (elapsed >= frameDuration) {
        currentFrame++;
        elapsed -= frameDuration;
        
        if (currentFrame >= frames.length) {
            if (loop) {
                currentFrame = 0;
            } else {
                currentFrame = frames.length - 1;
                isFinished = true;
                break;
            }
        }
    }
    
    lastTime = now - elapsed; // 남은 시간 유지
}
```

### 7. 프레임 보간 (중요도: 낮음)
**현재 상태**: 프레임 전환이 즉시 일어남

**제안** (고급):
```java
/**
 * 프레임 간 보간 (부드러운 전환)
 */
public void drawInterpolated(Graphics2D g, int x, int y, int width, int height) {
    if (frames == null || frames.length == 0) return;
    
    long now = System.currentTimeMillis();
    long elapsed = now - lastTime;
    float progress = Math.min(1f, (float) elapsed / frameDuration);
    
    // 현재 프레임
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - progress));
    g.drawImage(frames[currentFrame], x, y, width, height, null);
    
    // 다음 프레임 (반투명)
    int nextFrame = (currentFrame + 1) % frames.length;
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, progress));
    g.drawImage(frames[nextFrame], x, y, width, height, null);
    
    // 원래대로
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
}
```

---

## 📊 코드 품질 평가

| 항목 | 점수 | 설명 |
|------|------|------|
| **간결성** | ⭐⭐⭐⭐⭐ | 113줄, 핵심 기능만 포함 |
| **시간 기반** | ⭐⭐⭐⭐☆ | 밀리초 단위, deltaTime 미지원 |
| **유연성** | ⭐⭐⭐⭐☆ | 반복/일회성, 속도 배수 미지원 |
| **null 안전** | ⭐⭐⭐⭐⭐ | frames null 체크 완벽 |
| **확장성** | ⭐⭐⭐☆☆ | 콜백, 역재생 미지원 |
| **성능** | ⭐⭐⭐⭐⭐ | isFinished 최적화 |

**총점: 4.3 / 5.0** ⭐⭐⭐⭐☆

---

## 🎓 결론

SpriteAnimation.java는 **간결하고 효율적인 프레임 애니메이션 시스템**입니다. 특히 **시간 기반 업데이트**, **반복/일회성 지원**, **종료 최적화**가 인상적입니다.

### 주요 성과
1. ✅ **시간 기반**: System.currentTimeMillis()로 프레임 독립적
2. ✅ **반복/일회성**: loop 플래그로 유연하게 제어
3. ✅ **자동 정지**: isFinished 플래그로 종료 애니메이션 관리
4. ✅ **null 안전**: frames null 체크
5. ✅ **최적화**: 종료된 애니메이션 업데이트 스킵

### 개선 방향
1. **deltaTime 기반**: 더 정확한 타이밍
2. **속도 배수**: setSpeedMultiplier() 추가
3. **콜백 시스템**: onComplete, onLoop 추가
4. **프레임 스킵 방지**: while 루프로 여러 프레임 건너뛰기

**프로덕션 레벨**이며, deltaTime 지원만 추가하면 **완벽한 애니메이션 시스템**입니다. 🎉
