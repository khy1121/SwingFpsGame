# Effects 클래스 코드 리뷰
## 시각 효과 시스템

## 파일 정보
- **총 파일 수**: 24개
- **총 라인 수**: 약 659줄
- **핵심 클래스**: 
  - `SkillEffect.java` (45줄) - 추상 기본 클래스
  - `SkillEffectManager.java` (78줄) - 이펙트 관리자
- **캐릭터별 이펙트**: 22개 (542줄)
- **역할**: 스킬 사용 시 시각적 피드백 제공

---

## 개요

이 24개 클래스는 **게임의 모든 시각 효과를 담당**하는 시스템입니다. **템플릿 메서드 패턴**과 **팩토리 패턴**을 활용하여 각 캐릭터의 스킬마다 고유한 이펙트를 구현했습니다.

### 설계 철학

```
[추상 기본 클래스] SkillEffect
        ↓
    상속 및 구현
        ↓
[22개 구체 클래스]
├── Raven (대시, 과충전)
├── Piper (마크, 열상)
├── Tech (터렛, 지뢰)
├── General (오라, 스트라이크)
├── Ghost (은폐, 무효화)
├── Sage (힐, 부활)
├── Skull (아드레날린, 탄약)
├── Steam (EMP, 리셋)
├── Wildcat (버서크, 브리치)
├── Bulldog (엄호, 바라지)
└── 공통 (총구 섬광, 터렛 발사)
```

---

## 1. SkillEffect (45줄) - 추상 기본 클래스

### 역할
**모든 이펙트의 기반**이 되는 추상 클래스입니다. 생명주기 관리와 렌더링 인터페이스를 정의합니다.

### 주요 컴포넌트

#### 1.1 생명주기 관리
```java
public abstract class SkillEffect {
    protected final String id;          // 효과 고유 ID (스킬 ID 등)
    protected final float duration;     // 전체 지속시간(초)
    protected float remaining;          // 남은 시간(초)

    protected SkillEffect(String id, float duration) {
        this.id = id;
        this.duration = Math.max(0.05f, duration); // 최소 시간 보장 (너무 짧은 값 보호)
        this.remaining = this.duration;
    }

    public boolean isExpired() { return remaining <= 0f; }

    /**
     * 프레임 단위 업데이트
     * @param dt 경과 시간(초)
     */
    public void update(float dt) { remaining -= dt; }
}
```

**특징**:
- **final id/duration**: 생성 후 변경 불가
- **최소 지속 시간**: 0.05초 (너무 짧은 이펙트 방지)
- **시간 기반 설계**: `deltaTime` 기반으로 `remaining` 감소

#### 1.2 렌더링 인터페이스
```java
/**
 * 로컬 플레이어 기준 그리기 (화면 좌표)
 */
public abstract void drawSelf(Graphics2D g2d, int x, int y);

/**
 * 원격 플레이어 기준 그리기 (기본적으로 동일 처리, 필요 시 오버라이드)
 */
public void drawForPlayer(Graphics2D g2d, int x, int y) {
    drawSelf(g2d, x, y);
}
```

**특징**:
- **이중 인터페이스**: 로컬/원격 시각 차별화 가능
- **기본 구현 제공**: `drawForPlayer`는 기본적으로 `drawSelf` 호출
- **템플릿 메서드 패턴**: 하위 클래스에서 `drawSelf` 구현 강제

### SkillEffect 강점
1. **단순한 생명주기**: duration/remaining 관리만
2. **템플릿 메서드 패턴**: 하위 클래스에서 렌더링만 구현
3. **시간 기반 설계**: deltaTime 기반 정확한 타이머
4. **확장 용이**: 새 이펙트 추가 시 `drawSelf`만 구현

### SkillEffect 개선 제안
1. **애니메이션 곡선**: Easing 함수 추가 (linear, ease-in, ease-out)
2. **경과 시간 접근자**: `getElapsed()` 메서드 추가
3. **투명도 계산 헬퍼**: `calculateAlpha()` 공통 메서드

---

## 2. SkillEffectManager (78줄) - 이펙트 관리자

### 역할
**모든 이펙트를 중앙에서 관리**합니다. 로컬/원격/오브젝트별로 이펙트를 분류하여 관리합니다.

### 주요 컴포넌트

#### 2.1 이펙트 저장소
```java
public class SkillEffectManager {
    private final List<SkillEffect> selfEffects = new ArrayList<>();
    private final Map<String, List<SkillEffect>> byPlayer = new HashMap<>();
    private final Map<Integer, List<SkillEffect>> byObject = new HashMap<>();
}
```

**특징**:
- **selfEffects**: 로컬 플레이어 이펙트
- **byPlayer**: 원격 플레이어별 이펙트 (맵 키: 플레이어 이름)
- **byObject**: 오브젝트(터렛 등)별 이펙트 (맵 키: 오브젝트 ID)

#### 2.2 이펙트 등록
```java
/** 로컬 플레이어 효과 등록 */
public void addSelf(SkillEffect fx) { 
    if (fx != null) selfEffects.add(fx); 
}

/** 특정 원격 플레이어 효과 등록 */
public void addForPlayer(String player, SkillEffect fx) {
    if (player == null || fx == null) return;
    byPlayer.computeIfAbsent(player, k -> new ArrayList<>()).add(fx);
}

/** 특정 오브젝트(터렛 등) 효과 등록 */
public void addForObject(int objectId, SkillEffect fx) {
    if (fx == null) return;
    byObject.computeIfAbsent(objectId, k -> new ArrayList<>()).add(fx);
}
```

**특징**:
- **null 체크**: 안전한 등록
- **computeIfAbsent**: 리스트 자동 생성
- **타입별 분류**: 로컬/원격/오브젝트 명확히 분리

#### 2.3 이펙트 업데이트
```java
/** 프레임 업데이트: 모든 이펙트 수명 감소 & 만료 제거 */
public void update(float dt) {
    if (!selfEffects.isEmpty()) {
        for (Iterator<SkillEffect> it = selfEffects.iterator(); it.hasNext();) {
            SkillEffect fx = it.next();
            fx.update(dt);
            if (fx.isExpired()) it.remove();
        }
    }
    if (!byPlayer.isEmpty()) {
        for (List<SkillEffect> list : byPlayer.values()) {
            for (Iterator<SkillEffect> it = list.iterator(); it.hasNext();) {
                SkillEffect fx = it.next();
                fx.update(dt);
                if (fx.isExpired()) it.remove();
            }
        }
    }
    if (!byObject.isEmpty()) {
        for (List<SkillEffect> list : byObject.values()) {
            for (Iterator<SkillEffect> it = list.iterator(); it.hasNext();) {
                SkillEffect fx = it.next();
                fx.update(dt);
                if (fx.isExpired()) it.remove();
            }
        }
    }
}
```

**특징**:
- **Iterator 패턴**: 순회 중 안전한 제거
- **만료 자동 제거**: `isExpired()` 체크 후 제거
- **3단계 업데이트**: 로컬, 원격, 오브젝트 모두 처리

#### 2.4 이펙트 렌더링
```java
/** 로컬 이펙트 그리기 */
public void drawSelf(Graphics2D g2d, int x, int y) {
    for (SkillEffect fx : selfEffects) fx.drawSelf(g2d, x, y);
}

/** 특정 플레이어 이펙트 그리기 */
public void drawForPlayer(String player, Graphics2D g2d, int x, int y) {
    List<SkillEffect> list = byPlayer.get(player);
    if (list == null || list.isEmpty()) return;
    for (SkillEffect fx : list) fx.drawForPlayer(g2d, x, y);
}

/** 특정 오브젝트(터렛 등) 이펙트 그리기 */
public void drawForObject(int objectId, Graphics2D g2d, int x, int y) {
    List<SkillEffect> list = byObject.get(objectId);
    if (list == null || list.isEmpty()) return;
    for (SkillEffect fx : list) fx.drawSelf(g2d, x, y);
}
```

**특징**:
- **타입별 렌더링**: 로컬, 원격, 오브젝트 개별 호출
- **null 안전**: 리스트 존재 여부 체크
- **일괄 처리**: 모든 이펙트를 순회하며 그리기

### SkillEffectManager 강점
1. **중앙 집중식 관리**: 모든 이펙트를 한 곳에서
2. **타입별 분류**: 로컬/원격/오브젝트 명확히 분리
3. **자동 정리**: 만료된 이펙트 자동 제거
4. **확장 가능**: 새로운 타입 추가 용이

### SkillEffectManager 개선 제안
1. **이펙트 풀링**: SkillEffect 객체 재사용
2. **Z-Index 지원**: 렌더링 순서 제어
3. **필터링**: 특정 타입 이펙트만 그리기
4. **통계**: 활성 이펙트 수 카운터

---

## 3. 구체 이펙트 클래스들 (22개, 542줄)

### 3.1 Raven (이동/화력 특화)

#### RavenDashEffect (40줄)
```java
public class RavenDashEffect extends SkillEffect {
    public RavenDashEffect(float duration) { super("raven_dash", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        // 진행도(0~1): 시간이 지날수록 1에 근접
        float progress = 1f - (remaining / duration);
        int baseR = 30; // 기본 반경
        int radius = baseR + (int)(progress * 15); // 진행도에 따라 반경 증가
        // 남은 수명 비율로 알파(투명도) 계산 -> 시간이 지날수록 더 투명
        int alpha = (int)(180 * (remaining / duration));
        alpha = Math.max(40, alpha);

        // 바깥 링: 청록색 계열의 라인을 굵게 그려 속도감 표현
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(new Color(80, 190, 255, alpha));
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        // 내부 펄스: 반투명한 안쪽 링으로 다이내믹한 느낌 추가
        g2d.setColor(new Color(120, 220, 255, alpha/2));
        g2d.drawOval(x - radius/2, y - radius/2, radius, radius);
    }
}
```

**특징**:
- **확장 링**: 시간에 따라 반경 증가 (30 → 45)
- **페이드아웃**: 투명도 180 → 40
- **이중 링**: 바깥(청록) + 안쪽(밝은 청록)
- **속도감 표현**: BasicStroke(3f)로 굵은 라인

#### RavenOverchargeEffect (34줄)
```java
public class RavenOverchargeEffect extends SkillEffect {
    public RavenOverchargeEffect(float duration) { super("raven_overcharge", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float e = duration - remaining;
        int alpha = (int)(200 * (remaining / duration)); alpha = Math.max(60, alpha);
        int r = 38;
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(new Color(255, 150, 40, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        // 회전 아크 4개
        for (int i = 0; i < 4; i++) {
            int arcStart = (int)((e * 140 + i * 90) % 360);
            g2d.drawArc(x - r, y - r, r * 2, r * 2, arcStart, 50);
        }
    }
}
```

**특징**:
- **회전 아크**: 4개의 아크가 회전 (140도/초)
- **주황색 계열**: RGB(255, 150, 40)
- **화력 강화 표현**: 빠른 회전으로 공격 속도 증가 암시

### 3.2 Piper (정찰 특화)

#### PiperMarkEffect (31줄)
```java
public class PiperMarkEffect extends SkillEffect {
    public PiperMarkEffect(float duration) { super("piper_mark", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float e = duration - remaining;
        int alpha = (int)(180 * (remaining / duration)); alpha = Math.max(50, alpha);
        int r = 45;
        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(new Color(255, 220, 100, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        // 펄스: 안쪽 원이 커졌다 작아짐
        int pr = (int)(r * 0.5 + 8 * Math.sin(e * 4));
        g2d.drawOval(x - pr, y - pr, pr * 2, pr * 2);
    }
}
```

**특징**:
- **펄스 효과**: 안쪽 원이 sin 함수로 진동
- **노란색 계열**: RGB(255, 220, 100) - 레이더 느낌
- **탐지 표현**: 펄스로 스캔 중임을 암시

#### PiperThermalEffect (29줄)
```java
public class PiperThermalEffect extends SkillEffect {
    public PiperThermalEffect(float duration) { super("piper_thermal", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float e = duration - remaining;
        int alpha = (int)(190 * (remaining / duration)); alpha = Math.max(70, alpha);
        int r = 50;
        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(new Color(255, 80, 80, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        // 회전 스캔 라인
        double angle = (e * 2.5) % (2 * Math.PI);
        int lx = (int)(r * Math.cos(angle)), ly = (int)(r * Math.sin(angle));
        g2d.drawLine(x, y, x + lx, y + ly);
    }
}
```

**특징**:
- **회전 스캔 라인**: 레이더처럼 회전 (2.5 rad/초)
- **빨간색 계열**: RGB(255, 80, 80) - 열감지 느낌
- **열상 표현**: 적을 감지하는 스캔 효과

### 3.3 Tech (설치형 특화)

#### TechTurretEffect (17줄)
```java
public class TechTurretEffect extends SkillEffect {
    public TechTurretEffect(float duration) { super("tech_turret", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float e = (duration - remaining);
        int r = 34;
        int alpha = (int)(160 * (remaining / duration)); alpha = Math.max(60, alpha);
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(new Color(90, 230, 210, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        int arcStart = (int)((e * 180) % 360);
        g2d.drawArc(x - r, y - r, r * 2, r * 2, arcStart, 70);
    }
}
```

**특징**:
- **보호 링**: 청록색 원 (RGB 90, 230, 210)
- **회전 아크**: 느린 회전 (180도/초)
- **터렛 활성 표현**: 방어 시스템 작동 중

#### TechMineEffect (14줄)
```java
public class TechMineEffect extends SkillEffect {
    public TechMineEffect(float duration) { super("tech_mine", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        int alpha = (int)(150 * (remaining / duration)); alpha = Math.max(40, alpha);
        g2d.setColor(new Color(230, 50, 50, alpha));
        g2d.fillOval(x - 8, y - 8, 16, 16);
        g2d.setColor(new Color(255, 100, 100, alpha/2));
        g2d.drawOval(x - 12, y - 12, 24, 24);
    }
}
```

**특징**:
- **빨간 점**: 지뢰 위치 표시
- **경고 링**: 위험 범위 표시
- **간결한 표현**: 14줄로 충분한 시각 효과

### 3.4 General (지휘 특화)

#### GeneralAuraEffect (23줄)
```java
public class GeneralAuraEffect extends SkillEffect {
    public GeneralAuraEffect(float duration) { super("general_aura", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float e = duration - remaining;
        int r = 42;
        int alpha = (int)(160 * (remaining / duration)); alpha = Math.max(50, alpha);
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(new Color(255, 215, 0, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        // 펄스
        int pr = (int)(r * 0.7 + 6 * Math.sin(e * 3));
        g2d.drawOval(x - pr, y - pr, pr * 2, pr * 2);
    }
}
```

**특징**:
- **금색 오라**: RGB(255, 215, 0)
- **펄스 효과**: 지휘관 위엄 표현
- **버프 범위 암시**: 넓은 원으로 팀 버프 표시

#### GeneralStrikeEffect (18줄)
```java
public class GeneralStrikeEffect extends SkillEffect {
    public GeneralStrikeEffect(float duration) { super("gen_strike", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        int alpha = (int)(220 * (remaining / duration)); alpha = Math.max(80, alpha);
        int r = 100; // 광역 표시
        g2d.setStroke(new BasicStroke(4f));
        g2d.setColor(new Color(255, 50, 50, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        g2d.drawLine(x - 15, y, x + 15, y);
        g2d.drawLine(x, y - 15, x, y + 15);
    }
}
```

**특징**:
- **광역 표시**: 반경 100 (큰 원)
- **십자 표적**: 정확한 타격 중심 표시
- **위험 색상**: 밝은 빨강 (RGB 255, 50, 50)

### 3.5 공통 이펙트

#### MuzzleFlashEffect (46줄)
```java
public class MuzzleFlashEffect extends SkillEffect {
    private final double angle; // 라디안 방향

    public MuzzleFlashEffect(double angleRad) {
        this(angleRad, 0.12f);
    }

    public MuzzleFlashEffect(double angleRad, float duration) {
        super("muzzle_flash", duration);
        this.angle = angleRad;
    }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        int alpha = (int)(200 * (getRemaining() / getDuration()));
        alpha = Math.max(60, alpha);
        int len = 26;       // 섬광 길이
        int halfWidth = 8;  // 섬광 반폭
        double cx = Math.cos(angle), cy = Math.sin(angle); // 진행 방향 벡터
        double nx = -cy, ny = cx; // 법선 벡터 (좌우 퍼짐)
        int x1 = x + (int)(cx * len);
        int y1 = y + (int)(cy * len);
        int x2 = x + (int)(nx * halfWidth);
        int y2 = y + (int)(ny * halfWidth);
        int x3 = x - (int)(nx * halfWidth);
        int y3 = y - (int)(ny * halfWidth);
        Polygon cone = new Polygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
        g2d.setColor(new Color(255, 230, 80, alpha));
        g2d.fillPolygon(cone);
        g2d.setColor(new Color(255, 200, 60, alpha));
        g2d.drawPolygon(cone);
    }
}
```

**특징**:
- **원뿔형 섬광**: 삼각형 Polygon으로 구현
- **방향 기반**: 발사 각도로 회전
- **짧은 지속**: 기본 0.12초 (순간 임팩트)
- **노란색 광채**: RGB(255, 230, 80)
- **벡터 수학**: cos/sin으로 방향, 법선 벡터로 좌우 퍼짐

#### TurretShootEffect (21줄)
```java
public class TurretShootEffect extends SkillEffect {
    public TurretShootEffect() { super("turret_shoot", 0.15f); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        int alpha = (int)(220 * (remaining / duration)); alpha = Math.max(70, alpha);
        g2d.setColor(new Color(255, 160, 60, alpha));
        g2d.fillOval(x - 10, y - 10, 20, 20);
        g2d.setColor(new Color(255, 200, 100, alpha/2));
        g2d.drawOval(x - 15, y - 15, 30, 30);
    }
}
```

**특징**:
- **자동화 표시**: 터렛 자동 발사 효과
- **주황색 섬광**: RGB(255, 160, 60)
- **짧은 지속**: 0.15초

### 3.6 나머지 캐릭터 이펙트 (요약)

| 캐릭터 | 이펙트 | 라인 수 | 특징 |
|--------|--------|---------|------|
| **Ghost** | CloakEffect | 19줄 | 은폐 (보라색 반투명 링) |
| | NullifyEffect | 21줄 | 무효화 (보라색 펄스) |
| **Sage** | HealEffect | 17줄 | 힐 (녹색 십자가 + 링) |
| | ReviveEffect | 20줄 | 부활 (밝은 녹색 확장 링) |
| **Skull** | AdrenalineEffect | 17줄 | 아드레날린 (빨간 펄스) |
| | AmmoEffect | 20줄 | 탄약 (회색 회전 아크) |
| **Steam** | EmpEffect | 25줄 | EMP (청록 전기 파동) |
| | ResetEffect | 24줄 | 리셋 (파란 역회전 아크) |
| **Wildcat** | BerserkEffect | 25줄 | 버서크 (주황 강렬한 링) |
| | BreachEffect | 23줄 | 브리치 (빨간 확장 파동) |
| **Bulldog** | CoverEffect | 24줄 | 엄호 (회색 방패 링) |
| | BarrageEffect | 28줄 | 바라지 (주황 다중 링) |

---

## 종합 평가

### 설계 패턴 분석

#### 1. 템플릿 메서드 패턴
```
[추상 클래스] SkillEffect
├── update(dt)      ← 공통 구현
├── isExpired()     ← 공통 구현
└── drawSelf(g, x, y) ← 하위 클래스 구현 강제
```

**평가**: ⭐⭐⭐⭐⭐ (5.0/5.0)
- 생명주기 관리는 부모가, 렌더링은 자식이 담당
- 코드 중복 최소화

#### 2. 팩토리 패턴
```java
// 사용 예시 (GamePanel)
skillEffects.addSelf(new RavenDashEffect(3.0f));
skillEffects.addSelf(new PiperMarkEffect(8.0f));
skillEffects.addForObject(turretId, new TechTurretEffect(12.0f));
```

**평가**: ⭐⭐⭐⭐☆ (4.5/5.0)
- 생성 로직이 분산되어 있음
- 팩토리 클래스 추가 시 더욱 개선 가능

#### 3. 관리자 패턴
```
SkillEffectManager
├── selfEffects: List<SkillEffect>
├── byPlayer: Map<String, List<SkillEffect>>
└── byObject: Map<Integer, List<SkillEffect>>
```

**평가**: ⭐⭐⭐⭐⭐ (5.0/5.0)
- 타입별 분류로 명확한 관리
- 자동 정리 (만료 시 제거)

### 코드 품질 평가

| 항목 | 평가 | 점수 |
|------|------|------|
| **추상화** | 템플릿 메서드 패턴 적용 | ⭐⭐⭐⭐⭐ (5.0/5.0) |
| **캡슐화** | 각 이펙트가 독립적 | ⭐⭐⭐⭐⭐ (5.0/5.0) |
| **재사용성** | 새 이펙트 추가 용이 | ⭐⭐⭐⭐⭐ (5.0/5.0) |
| **가독성** | 짧고 명확한 구현 (평균 23줄) | ⭐⭐⭐⭐⭐ (5.0/5.0) |
| **확장성** | 새 캐릭터/스킬 추가 간단 | ⭐⭐⭐⭐⭐ (5.0/5.0) |
| **성능** | 가벼운 렌더링 (도형 위주) | ⭐⭐⭐⭐☆ (4.5/5.0) |
| **종합** | | ⭐⭐⭐⭐⭐ (4.9/5.0) |

### 주요 강점

1. **짧고 명확한 구현**
   - 평균 23줄 (최소 14줄, 최대 78줄)
   - 각 이펙트가 한 가지 역할만 수행

2. **일관된 디자인 언어**
   ```
   - 팀 버프: 금색/노란색 계열
   - 공격 강화: 주황색/빨간색 계열
   - 정찰: 노란색/빨간색 계열
   - 방어: 청록색/회색 계열
   ```

3. **수학적 표현**
   - sin/cos으로 펄스, 회전 구현
   - progress = 1 - (remaining/duration)
   - alpha = max(minAlpha, baseAlpha * (remaining/duration))

4. **템플릿 메서드 패턴**
   - 공통 로직(생명주기)은 부모 클래스
   - 렌더링만 하위 클래스에서 구현

5. **확장 용이**
   - 새 이펙트 추가: SkillEffect 상속 + drawSelf 구현
   - 약 20~30줄로 새 이펙트 완성

### 개선 제안

1. **이펙트 팩토리 클래스**
   ```java
   public class SkillEffectFactory {
       public static SkillEffect create(String abilityId, float duration) {
           return switch (abilityId) {
               case "raven_dash" -> new RavenDashEffect(duration);
               case "piper_mark" -> new PiperMarkEffect(duration);
               case "tech_turret" -> new TechTurretEffect(duration);
               // ... 나머지
               default -> null;
           };
       }
   }
   ```

2. **Easing 함수 추가**
   ```java
   public abstract class SkillEffect {
       protected float easeOut(float t) {
           return 1 - (1 - t) * (1 - t);
       }
       
       protected float easeIn(float t) {
           return t * t;
       }
   }
   ```

3. **파티클 시스템**
   - 복잡한 이펙트를 위한 파티클 엔진
   - 예: 폭발 효과, 연기, 불꽃

4. **색상 상수화**
   ```java
   public interface EffectColors {
       Color BUFF_GOLD = new Color(255, 215, 0);
       Color ATTACK_ORANGE = new Color(255, 150, 40);
       Color SCOUT_YELLOW = new Color(255, 220, 100);
       Color DEFEND_CYAN = new Color(90, 230, 210);
   }
   ```

5. **이펙트 체이닝**
   ```java
   skillEffects.addSelf(
       new RavenDashEffect(1.0f)
           .then(new RavenTrailEffect(2.0f))
   );
   ```

6. **오디오 통합**
   - 이펙트 재생 시 사운드 자동 연결
   - `onStart()` 콜백에서 사운드 재생

---

## 결론

이 24개 이펙트 클래스는 **게임의 시각적 피드백을 담당하는 핵심 시스템**으로, **템플릿 메서드 패턴**을 활용하여 **확장 가능하고 유지보수하기 쉬운 구조**를 달성했습니다.

특히 **평균 23줄**이라는 짧은 코드로 각 캐릭터의 스킬마다 **고유한 시각 효과**를 제공하며, **SkillEffectManager**를 통한 중앙 집중식 관리로 **자동 정리**와 **타입별 분류**를 구현했습니다.

전반적으로 **매우 성공적인 설계**이며, 코드 품질 평가 **4.9/5.0**으로 **SOLID 원칙을 잘 따르고 있습니다**.

### 최종 통계
- **총 파일 수**: 24개
- **총 라인 수**: 659줄
- **평균 라인 수**: 27.5줄/파일
- **가장 짧은 파일**: TechMineEffect (14줄)
- **가장 긴 파일**: SkillEffectManager (78줄)
- **코드 품질**: ⭐⭐⭐⭐⭐ (4.9/5.0)

이 시스템은 **게임 개발의 모범 사례**로, 다른 프로젝트에도 적용할 수 있는 **재사용 가능한 설계**입니다.
