# Ability.java 코드 리뷰

## 📋 파일 개요
- **경로**: `src/com/fpsgame/common/Ability.java`
- **역할**: 캐릭터 스킬 기본 클래스
- **라인 수**: 107줄
- **주요 기능**: 스킬 메타데이터, 쿨다운 관리, 활성화 상태 추적
- **특징**: 마나 없음, 쿨다운 기반 스킬 시스템

---

## 🎯 주요 기능

### 1. 스킬 타입 열거형
```java
public enum AbilityType {
    BASIC,      // 기본 공격 (좌클릭)
    TACTICAL,   // 전술 스킬 (E키)
    ULTIMATE    // 궁극기 (R키)
}
```
- **3가지 타입**: 기본 공격, 전술 스킬, 궁극기
- **키 바인딩 명확**: 좌클릭 (BASIC), E (TACTICAL), R (ULTIMATE)
- **타입 안전**: Enum으로 오타 방지

### 2. 스킬 메타데이터
```java
public class Ability {
    // 불변 속성 (스킬 정의)
    public final String id;              // "raven_basic", "piper_mark" 등
    public final String name;            // "고속 연사", "적 표시" 등
    public final String description;     // 스킬 설명
    public final AbilityType type;       // BASIC, TACTICAL, ULTIMATE
    
    // 스킬 수치
    public final float cooldown;         // 쿨다운 (초)
    public final float duration;         // 지속 시간 (초, 0이면 즉발)
    public final float range;            // 사거리 (0이면 자신에게)
    public final float damage;           // 데미지 (0이면 공격 스킬 아님)
    
    // 런타임 상태 (변경 가능)
    private float currentCooldown = 0f;  // 현재 쿨다운 (0이면 사용 가능)
    private boolean isActive = false;    // 활성화 상태
    private float activeDuration = 0f;   // 활성화 지속 시간
    private float cooldownMultiplier = 1f; // 쿨다운 배수 (버프/디버프)
}
```
**설계 특징**:
- **불변 속성**: `final` 키워드로 스킬 정의 변경 불가
- **가변 상태**: `private` 필드로 런타임 상태 관리
- **명확한 의미**: `duration = 0` → 즉발, `damage = 0` → 비공격 스킬

### 3. 쿨다운 관리

#### 프레임별 업데이트
```java
/**
 * 프레임마다 호출 (쿨다운 감소)
 */
public void update(float deltaTime) {
    // 쿨다운 감소
    if (currentCooldown > 0) {
        currentCooldown = Math.max(0, currentCooldown - deltaTime);
    }
    
    // 지속 시간 감소 (활성화 상태일 때만)
    if (isActive && duration > 0) {
        activeDuration -= deltaTime;
        if (activeDuration <= 0) {
            deactivate(); // 자동 비활성화
        }
    }
}
```
**deltaTime 기반**:
- **프레임 독립적**: 60fps, 30fps 상관없이 동일한 쿨다운 감소
- **음수 방지**: `Math.max(0, ...)` 사용

#### 사용 가능 여부 체크
```java
/**
 * 스킬 사용 가능 여부
 */
public boolean canUse() {
    return currentCooldown <= 0;
}
```
- **간단한 로직**: 쿨다운이 0 이하면 사용 가능
- **별칭 메서드**: `isReady()`와 동일 (중복)

### 4. 스킬 활성화

#### 활성화 로직
```java
/**
 * 스킬 활성화
 */
public void activate() {
    if (currentCooldown <= 0) {
        // 쿨다운 배수 적용 (버프/디버프)
        float mul = cooldownMultiplier > 0 ? cooldownMultiplier : 1f;
        currentCooldown = cooldown * mul;
        
        // 지속 시간이 있으면 활성화 상태로 전환
        if (duration > 0) {
            isActive = true;
            activeDuration = duration;
        }
    }
}
```
**핵심 로직**:
1. **쿨다운 체크**: `currentCooldown <= 0`일 때만 실행
2. **쿨다운 시작**: `cooldown * cooldownMultiplier`
3. **활성화 상태**: `duration > 0`일 때만 활성화

**사용 예시**:
```java
// Raven 대쉬 (5초 쿨다운, 0.5초 지속)
Ability dash = new Ability("raven_dash", "대쉬", "빠르게 전방으로 돌진",
    Ability.AbilityType.TACTICAL, 5f, 0.5f, 200f, 0f);

// 사용
if (dash.canUse()) {
    dash.activate();
    // currentCooldown = 5초
    // isActive = true
    // activeDuration = 0.5초
}

// 매 프레임 업데이트
dash.update(deltaTime);

// 0.5초 후 자동 비활성화
// isActive = false
// currentCooldown = 4.5초 (계속 감소)
```

#### 비활성화 로직
```java
/**
 * 스킬 비활성화
 */
public void deactivate() {
    isActive = false;
    activeDuration = 0;
}
```
- **수동 비활성화**: 스킬 중단 시 호출
- **자동 비활성화**: `update()` 메서드에서 지속 시간 종료 시 호출

### 5. 쿨다운 배수 시스템
```java
private float cooldownMultiplier = 1f; // 기본값 1.0 (100%)

public void setCooldownMultiplier(float mul) { 
    this.cooldownMultiplier = mul; 
}

public float getCooldownMultiplier() { 
    return cooldownMultiplier; 
}
```
**사용 사례**:
- **버프**: `setCooldownMultiplier(0.5f)` → 쿨다운 50% (2배 빠름)
- **디버프**: `setCooldownMultiplier(2.0f)` → 쿨다운 200% (2배 느림)
- **General 오라**: 공격속도 +15% → `0.85f` (15% 감소)

**예시**:
```java
// Raven 과충전 (공격속도 증가)
ravenBasic.setCooldownMultiplier(0.5f); // 0.3초 → 0.15초

// 6초 후 버프 종료
ravenBasic.setCooldownMultiplier(1.0f); // 원래대로
```

### 6. 쿨다운 퍼센트
```java
/**
 * 쿨다운 진행률 (0.0 ~ 1.0)
 * UI에서 쿨다운 게이지 표시용
 */
public float getCooldownPercent() { 
    return cooldown > 0 ? currentCooldown / cooldown : 0; 
}
```
**사용 예시**:
```java
// UI 렌더링
float percent = ability.getCooldownPercent();
int cooldownWidth = (int)(50 * percent); // 50픽셀 게이지
graphics.fillRect(x, y, cooldownWidth, 10);

// 텍스트 표시
String cooldownText = String.format("%.1f초", ability.getCurrentCooldown());
```

### 7. 쿨다운 리셋
```java
/**
 * 쿨다운 리셋 (테스트용)
 */
public void resetCooldown() {
    currentCooldown = 0;
}
```
**사용 사례**:
- **테스트**: 스킬 즉시 재사용
- **특수 스킬**: Skull의 "탄약 보급" (모든 스킬 쿨타임 초기화)
- **치트 모드**: 개발/디버깅용

### 8. Getter 메서드
```java
// 메타데이터 조회
public String getName() { return name; }
public String getDescription() { return description; }
public AbilityType getType() { return type; }

// 상태 조회
public float getCurrentCooldown() { return currentCooldown; }
public float getCooldownPercent() { return cooldown > 0 ? currentCooldown / cooldown : 0; }
public boolean isActive() { return isActive; }
public float getActiveDuration() { return activeDuration; }
public boolean isReady() { return currentCooldown <= 0; }
```
**중복 메서드**:
- `canUse()` vs `isReady()`: 동일한 기능 (둘 다 `currentCooldown <= 0` 체크)

---

## 💡 강점

### 1. 단순하고 명확한 설계
- **3가지 타입**: BASIC, TACTICAL, ULTIMATE (이해하기 쉬움)
- **4가지 수치**: cooldown, duration, range, damage (직관적)
- **deltaTime 기반**: 프레임 독립적 업데이트

### 2. 불변성과 캡슐화
```java
// 스킬 정의는 불변
public final String id;
public final float cooldown;

// 런타임 상태는 private
private float currentCooldown;
private boolean isActive;
```
- **스킬 정의 보호**: `final` 키워드로 변경 불가
- **상태 숨김**: `private` 필드로 캡슐화

### 3. 자동 비활성화
```java
public void update(float deltaTime) {
    if (isActive && duration > 0) {
        activeDuration -= deltaTime;
        if (activeDuration <= 0) {
            deactivate(); // 자동 비활성화
        }
    }
}
```
- **메모리 누수 방지**: 지속 시간 종료 시 자동으로 비활성화
- **편의성**: 수동 비활성화 호출 불필요

### 4. 쿨다운 배수 시스템
- **유연한 버프/디버프**: `cooldownMultiplier`로 런타임 조정
- **다양한 활용**: 공격속도 증가, EMP 디버프, 힐 버프 등

### 5. UI 친화적 메서드
```java
public float getCooldownPercent() { /* ... */ } // 게이지 표시
public boolean isActive() { /* ... */ }         // 활성화 표시
public float getCurrentCooldown() { /* ... */ } // 쿨다운 텍스트
```
- **즉시 사용 가능**: UI에서 직접 호출

---

## 🔧 개선 제안

### 1. 중복 메서드 제거 (중요도: 낮음)
**현재 상태**: `canUse()`와 `isReady()` 동일

```java
public boolean canUse() {
    return currentCooldown <= 0;
}

public boolean isReady() {
    return currentCooldown <= 0;
}
```

**제안**: 하나만 남기고 제거
```java
// canUse() 제거하고 isReady()만 사용
public boolean isReady() {
    return currentCooldown <= 0;
}
```

### 2. 활성화 실패 피드백 (중요도: 중간)
**현재 상태**: `activate()` 메서드가 아무 반환값 없음

```java
public void activate() {
    if (currentCooldown <= 0) {
        // 성공
        currentCooldown = cooldown * cooldownMultiplier;
        // ...
    }
    // 실패 시 아무 일도 안 일어남
}
```

**문제점**:
- 활성화 실패 여부를 알 수 없음
- UI에서 "쿨다운 중" 메시지 표시 불가

**제안**:
```java
/**
 * 스킬 활성화
 * @return 성공 여부
 */
public boolean activate() {
    if (currentCooldown <= 0) {
        float mul = cooldownMultiplier > 0 ? cooldownMultiplier : 1f;
        currentCooldown = cooldown * mul;
        
        if (duration > 0) {
            isActive = true;
            activeDuration = duration;
        }
        return true; // 성공
    }
    return false; // 실패 (쿨다운 중)
}

// 사용
if (!ability.activate()) {
    showMessage("쿨다운 중: " + ability.getCurrentCooldown() + "초");
}
```

### 3. 쿨다운 배수 검증 (중요도: 중간)
**현재 상태**: 음수 배수 체크만 함

```java
float mul = cooldownMultiplier > 0 ? cooldownMultiplier : 1f;
```

**문제점**:
- 극단적인 값 (0.01f, 100f) 허용
- 밸런스 붕괴 가능

**제안**:
```java
public void setCooldownMultiplier(float mul) {
    // 0.1 ~ 5.0 범위 제한 (10배 빠름 ~ 5배 느림)
    if (mul < 0.1f) {
        System.err.println("[경고] 쿨다운 배수가 너무 작음: " + mul + " -> 0.1로 제한");
        mul = 0.1f;
    } else if (mul > 5.0f) {
        System.err.println("[경고] 쿨다운 배수가 너무 큼: " + mul + " -> 5.0으로 제한");
        mul = 5.0f;
    }
    this.cooldownMultiplier = mul;
}
```

### 4. 마나 시스템 준비 (중요도: 낮음)
**현재 상태**: "마나 비용 없음" 주석

```java
/**
 * 캐릭터 스킬(Ability) 기본 클래스
 * 쿨타임만 있고 마나 비용 없음
 */
```

**미래 확장성**:
```java
public class Ability {
    // 기존 필드...
    public final float manaCost;         // 마나 비용 (기본값 0)
    
    public Ability(String id, String name, String description, AbilityType type,
                   float cooldown, float duration, float range, float damage, float manaCost) {
        // ...
        this.manaCost = manaCost;
    }
    
    /**
     * 스킬 사용 가능 여부 (마나 체크 포함)
     */
    public boolean canUse(float currentMana) {
        return currentCooldown <= 0 && currentMana >= manaCost;
    }
}
```

### 5. 스킬 체인/콤보 시스템 (중요도: 낮음)
**현재 상태**: 각 스킬 독립적

**제안**:
```java
public class Ability {
    // 기존 필드...
    public final String[] requiredAbilities; // 사용 전 필요한 스킬 ID 배열
    
    /**
     * 스킬 콤보 체크
     * @param usedAbilities 최근 사용한 스킬 ID 목록
     * @return 콤보 조건 충족 여부
     */
    public boolean checkCombo(List<String> usedAbilities) {
        if (requiredAbilities == null || requiredAbilities.length == 0) {
            return true; // 콤보 조건 없음
        }
        
        for (String required : requiredAbilities) {
            if (!usedAbilities.contains(required)) {
                return false;
            }
        }
        return true;
    }
}

// 예시: Wildcat "광폭화" (돌파 사격 후 사용 가능)
new Ability("wild_berserk", "광폭화", "이동속도 및 공격력 증가",
    Ability.AbilityType.ULTIMATE, 25f, 6f, 0f, 0f,
    new String[] { "wild_breach" }); // 돌파 사격 필요
```

### 6. 스킬 레벨/업그레이드 시스템 (중요도: 낮음)
**현재 상태**: 고정 스킬 수치

**제안**:
```java
public class Ability {
    // 기존 필드...
    private int level = 1;                // 스킬 레벨 (1~5)
    
    /**
     * 스킬 업그레이드
     */
    public void upgrade() {
        if (level < 5) {
            level++;
            // 레벨에 따라 수치 증가
            // (cooldown 감소, damage 증가 등)
        }
    }
    
    /**
     * 레벨에 따른 데미지 계산
     */
    public float getEffectiveDamage() {
        return damage * (1 + (level - 1) * 0.1f); // 레벨당 +10%
    }
    
    /**
     * 레벨에 따른 쿨다운 계산
     */
    public float getEffectiveCooldown() {
        return cooldown * (1 - (level - 1) * 0.05f); // 레벨당 -5%
    }
}
```

### 7. 스킬 이펙트 콜백 (중요도: 중간)
**현재 상태**: 스킬 로직이 GamePanel에 하드코딩

**제안**:
```java
public class Ability {
    // 기존 필드...
    private Runnable onActivate;   // 활성화 시 호출
    private Runnable onDeactivate; // 비활성화 시 호출
    
    public void setOnActivate(Runnable callback) {
        this.onActivate = callback;
    }
    
    public void setOnDeactivate(Runnable callback) {
        this.onDeactivate = callback;
    }
    
    public void activate() {
        if (currentCooldown <= 0) {
            // ... (기존 로직)
            
            if (onActivate != null) {
                onActivate.run(); // 콜백 실행
            }
        }
    }
    
    public void deactivate() {
        isActive = false;
        activeDuration = 0;
        
        if (onDeactivate != null) {
            onDeactivate.run(); // 콜백 실행
        }
    }
}

// 사용 예시
ravenDash.setOnActivate(() -> {
    // 대쉬 이펙트 표시
    showEffect(player.x, player.y, "dash_start");
    playSoundEffect("dash.wav");
});

ravenDash.setOnDeactivate(() -> {
    // 대쉬 종료 이펙트
    showEffect(player.x, player.y, "dash_end");
});
```

### 8. 쿨다운 세밀한 정보 (중요도: 낮음)
**현재 상태**: `getCooldownPercent()` 하나뿐

**제안**:
```java
/**
 * 쿨다운 남은 시간
 */
public float getRemainingCooldown() {
    return currentCooldown;
}

/**
 * 쿨다운 경과 시간
 */
public float getElapsedCooldown() {
    return cooldown - currentCooldown;
}

/**
 * 쿨다운 경과 퍼센트 (0.0 ~ 1.0)
 * UI에서 "채워지는" 게이지 표시용
 */
public float getCooldownElapsedPercent() {
    return cooldown > 0 ? getElapsedCooldown() / cooldown : 1.0f;
}
```

---

## 📊 코드 품질 평가

| 항목 | 점수 | 설명 |
|------|------|------|
| **단순성** | ⭐⭐⭐⭐⭐ | 107줄, 명확한 로직 |
| **캡슐화** | ⭐⭐⭐⭐☆ | 불변 속성 + private 상태, 일부 public 필드 |
| **deltaTime 처리** | ⭐⭐⭐⭐⭐ | 프레임 독립적 업데이트 완벽 |
| **확장성** | ⭐⭐⭐☆☆ | 쿨다운 배수 시스템 좋음, 콜백 없음 |
| **UI 연동** | ⭐⭐⭐⭐⭐ | getCooldownPercent(), isActive() 등 완벽 |
| **에러 처리** | ⭐⭐⭐☆☆ | activate() 반환값 없음, 배수 검증 부족 |

**총점: 4.2 / 5.0** ⭐⭐⭐⭐☆

---

## 🎓 결론

Ability.java는 **단순하고 효율적인 스킬 시스템**입니다. 특히 **deltaTime 기반 쿨다운 관리**, **자동 비활성화**, **UI 친화적 메서드**가 인상적입니다.

### 주요 성과
1. ✅ **deltaTime 기반**: 프레임 독립적 업데이트 (60fps, 30fps 동일)
2. ✅ **불변 설계**: 스킬 정의는 final, 런타임 상태는 private
3. ✅ **자동 비활성화**: 지속 시간 종료 시 자동으로 deactivate()
4. ✅ **쿨다운 배수**: cooldownMultiplier로 버프/디버프 구현
5. ✅ **UI 연동**: getCooldownPercent(), isActive() 즉시 사용 가능

### 개선 방향
1. **activate() 반환값**: boolean으로 성공/실패 피드백
2. **쿨다운 배수 검증**: 0.1 ~ 5.0 범위 제한
3. **중복 메서드 제거**: canUse() vs isReady() 통합
4. **콜백 시스템**: onActivate, onDeactivate 추가

**프로덕션 레벨**이며, 작은 개선만으로 **완벽한 스킬 시스템**이 될 것입니다. 🎉
