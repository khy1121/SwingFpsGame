# CharacterData.java 코드 리뷰

## 📋 파일 개요
- **경로**: `src/com/fpsgame/common/CharacterData.java`
- **역할**: 캐릭터 메타데이터 및 스킬 생성 Factory
- **라인 수**: 291줄
- **주요 기능**: 10개 캐릭터 정의, 스탯 관리, 스킬 생성, ID 기반 조회
- **캐릭터 수**: 10개 (활성 4개, 비활성 6개)

---

## 🎯 주요 기능

### 1. 캐릭터 데이터 구조
```java
public class CharacterData {
    /** 캐릭터 고유 ID (예: "raven", "piper") */
    public final String id;
    
    /** 캐릭터 표시 이름 (예: "Raven", "Piper") */
    public final String name;
    
    /** 캐릭터 설명 */
    public final String description;
    
    /** 기본 체력 */
    public final float health;
    
    /** 이동 속도 */
    public final float speed;
    
    /** 방어력 (데미지 감소율) */
    public final float armor;
    
    /** 역할 (공격형, 지원형, 정찰형 등) */
    public final String role;
    
    /** 스킬 이름 배열 (UI 표시용) */
    public final String[] abilities;
}
```
- **불변 설계**: 모든 필드 `final` (변경 불가)
- **명확한 타입**: float (체력, 속도, 방어력), String (ID, 이름, 역할)
- **UI 연동**: `abilities` 배열로 스킬 이름 표시

### 2. 전체 캐릭터 목록 (10개)

#### 활성 캐릭터 (4개)
```java
public static final CharacterData[] CHARACTERS = {
    // Raven - 공격형
    new CharacterData(
        "raven", "Raven", "빠른 기동성과 높은 화력",
        100f, 6.5f, 20f, "공격형",
        new String[] { "고속 연사 권총", "대쉬", "과충전" }
    ),
    
    // Piper - 정찰형
    new CharacterData(
        "piper", "Piper", "장거리 스나이퍼",
        80f, 5.5f, 15f, "정찰형",
        new String[] { "저격 소총", "적 표시", "열감지 스코프" }
    ),
    
    // Technician - 지원형
    new CharacterData(
        "technician", "Technician", "공학 유틸리티 전문가",
        100f, 5.0f, 8f, "지원형",
        new String[] { "플라즈마 건", "지뢰", "터렛" }
    ),
    
    // General - 밸런스형
    new CharacterData(
        "general", "General", "지휘관 역할",
        120f, 5.0f, 12f, "밸런스형",
        new String[] { "전술 소총", "지휘 오라", "공습" }
    ),
    
    // ... 비활성 캐릭터 6개 (Bulldog, Wildcat, Ghost, Skull, Steam, Sage)
};
```

**캐릭터 밸런스 비교**:
| 캐릭터 | HP | 속도 | 방어력 | 역할 |
|--------|-----|------|--------|------|
| Raven | 100 | 6.5 | 20 | 공격형 (빠르고 공격적) |
| Piper | 80 | 5.5 | 15 | 정찰형 (장거리 저격) |
| Technician | 100 | 5.0 | 8 | 지원형 (설치 오브젝트) |
| General | 120 | 5.0 | 12 | 밸런스형 (오라, 공습) |

#### 비활성 캐릭터 (6개)
```java
// Bulldog - 탱커 (200 HP, 40 방어력)
new CharacterData(
    "bulldog", "Bulldog", "높은 방어력과 화력",
    200f, 4.5f, 40f, "탱커",
    new String[] { "미니건", "엄폐 자세", "폭발탄 난사" }
),

// Wildcat - 돌격형 (110 HP, 산탄총)
new CharacterData(
    "wildcat", "Wildcat", "근접 전투 특화",
    110f, 5.2f, 10f, "돌격형",
    new String[] { "산탄총", "돌파 사격", "광폭화" }
),

// Ghost - 암살형 (120 HP, 투명화)
new CharacterData(
    "ghost", "Ghost", "은신과 위장 전문가",
    120f, 6.0f, 1f, "암살형",
    new String[] { "소음기 기관단총", "투명화", "열감지 무효화" }
),

// Skull - 공격형 (120 HP, 아드레날린)
new CharacterData(
    "skull", "Skull", "용병 스타일",
    120f, 5.0f, 12f, "공격형",
    new String[] { "카빈 소총", "아드레날린", "탄약 보급" }
),

// Steam - 밸런스형 (110 HP, EMP)
new CharacterData(
    "steam", "Steam", "특수부대",
    110f, 5.4f, 10f, "밸런스형",
    new String[] { "돌격 소총", "EMP 수류탄", "전술 리셋" }
),

// Sage - 힐러 (100 HP, 치료/부활)
new CharacterData(
    "sage", "Sage", "치료와 보조",
    100f, 5.3f, 8f, "힐러",
    new String[] { "기관단총", "치료 키트", "부활 드론" }
)
```

### 3. ID 기반 조회
```java
/**
 * ID로 캐릭터 데이터 조회
 * 
 * @param id 캐릭터 ID (대소문자 구분 안 함)
 * @return 해당 캐릭터 데이터, 없으면 기본값(Raven) 반환
 */
public static CharacterData getById(String id) {
    for (CharacterData data : CHARACTERS) {
        if (data.id.equalsIgnoreCase(id)) {
            return data;
        }
    }
    return CHARACTERS[0]; // 기본값: Raven
}
```
- **대소문자 무시**: `equalsIgnoreCase()` 사용
- **기본값 제공**: 잘못된 ID → Raven 반환 (null 대신)
- **선형 탐색**: O(n) 시간복잡도 (캐릭터 수가 적어 문제 없음)

### 4. 스킬 생성 Factory

#### Raven (공격형)
```java
case "raven":
    return new Ability[] {
        // 기본 공격: 고속 연사 권총
        new Ability("raven_basic", "고속 연사", "빠른 연사 권총 공격",
            Ability.AbilityType.BASIC, 0.3f, 0f, 500f, 15f),
        // 쿨타임 0.3초, 사거리 500, 데미지 15
        
        // 전술 스킬: 대쉬
        new Ability("raven_dash", "대쉬", "빠르게 전방으로 돌진",
            Ability.AbilityType.TACTICAL, 5f, 0.5f, 200f, 0f),
        // 쿨타임 5초, 지속시간 0.5초, 이동거리 200
        
        // 궁극기: 과충전 (공격속도 증가)
        new Ability("raven_overcharge", "과충전", "공격 속도 대폭 증가",
            Ability.AbilityType.ULTIMATE, 20f, 6f, 0f, 0f)
        // 쿨타임 20초, 지속시간 6초
    };
```
**Raven 특징**:
- **빠른 연사**: 0.3초 쿨타임 (초당 3.33발)
- **기동성**: 대쉬로 빠른 이동
- **화력 증폭**: 과충전으로 공격속도 증가

#### Piper (정찰형 스나이퍼)
```java
case "piper":
    return new Ability[] {
        // 기본 공격: 저격 소총
        new Ability("piper_basic", "저격", "장거리 정확한 저격",
            Ability.AbilityType.BASIC, 1.2f, 0f, 1000f, 80f),
        // 쿨타임 1.2초, 사거리 1000, 데미지 80 (최장 사거리!)
        
        // 전술 스킬: 적 표시
        new Ability("piper_mark", "적 표시", "적을 마킹하여 투시",
            Ability.AbilityType.TACTICAL, 8f, 5f, 800f, 0f),
        // 쿨타임 8초, 지속시간 5초, 사거리 800
        
        // 궁극기: 열감지 스코프
        new Ability("piper_thermal", "열감지", "모든 적 위치 표시",
            Ability.AbilityType.ULTIMATE, 30f, 8f, 0f, 0f)
        // 쿨타임 30초, 지속시간 8초
    };
```
**Piper 특징**:
- **최장 사거리**: 1000 픽셀 (다른 캐릭터의 2배)
- **고데미지**: 80 데미지 (Raven의 5.3배)
- **느린 연사**: 1.2초 쿨타임 (초당 0.83발)
- **정찰 능력**: 적 표시, 열감지로 정보 우위

#### Technician (지원형 공학자)
```java
case "technician":
    return new Ability[] {
        // 기본 공격: 플라즈마 건
        new Ability("tech_basic", "플라즈마", "플라즈마 건 발사",
            Ability.AbilityType.BASIC, 0.4f, 0f, 400f, 20f),
        // 쿨타임 0.4초, 사거리 400, 데미지 20
        
        // 전술 스킬: 지뢰
        new Ability("tech_mine", "지뢰", "지뢰 설치",
            Ability.AbilityType.TACTICAL, 20f, 30f, 100f, 50f),
        // 쿨타임 20초, 지속시간 30초, 데미지 50 (밟으면 폭발)
        
        // 궁극기: 터렛
        new Ability("tech_turret", "터렛", "자동 사격 터렛 배치",
            Ability.AbilityType.ULTIMATE, 40f, 20f, 150f, 25f)
        // 쿨타임 40초, 지속시간 20초, 사거리 150, 데미지 25 (터렛이 자동 공격)
    };
```
**Technician 특징**:
- **설치 오브젝트**: 지뢰 (수동 트리거), 터렛 (자동 공격)
- **영역 장악**: 지뢰로 길목 봉쇄, 터렛으로 거점 방어
- **긴 쿨타임**: 지뢰 20초, 터렛 40초 (신중한 배치 필요)

#### General (밸런스형 지휘관)
```java
case "general":
    return new Ability[] {
        // 기본 공격: 전술 소총
        new Ability("gen_basic", "전술 소총", "정확한 소총 사격",
            Ability.AbilityType.BASIC, 0.4f, 0f, 600f, 25f),
        // 쿨타임 0.4초, 사거리 600, 데미지 25
        
        // 전술 스킬: 지휘 오라
        new Ability("gen_aura", "지휘 오라", "아군 버프 제공",
            Ability.AbilityType.TACTICAL, 15f, 10f, 500f, 0f),
        // 쿨타임 15초, 지속시간 10초, 범위 500 (이동속도+10%, 공격속도+15%)
        
        // 궁극기: 공습 (에어스트라이크)
        new Ability("gen_strike", "공습", "지정 지역 폭격",
            Ability.AbilityType.ULTIMATE, 40f, 3f, 800f, 150f)
        // 쿨타임 40초, 지속시간 3초, 사거리 800, 데미지 150
    };
```
**General 특징**:
- **팀 버프**: 지휘 오라로 아군 강화 (이동속도+10%, 공격속도+15%)
- **강력한 궁극기**: 공습 150 데미지 (최대 데미지!)
- **밸런스**: 25 데미지 기본 공격, 600 사거리

#### 비활성 캐릭터 스킬 (일부)

**Ghost (암살형)**:
```java
case "ghost":
    return new Ability[] {
        // 소음기 SMG: 0.2초 쿨타임, 18 데미지
        new Ability("ghost_basic", "소음기 SMG", "조용한 기관단총",
            Ability.AbilityType.BASIC, 0.2f, 0f, 300f, 18f),
        
        // 투명화: 6초 지속
        new Ability("ghost_cloak", "투명화", "일시적 투명 상태",
            Ability.AbilityType.TACTICAL, 15f, 6f, 0f, 0f),
        
        // 열감지 무효: 10초 지속 (Piper 카운터)
        new Ability("ghost_nullify", "열감지 무효", "감지 불가 상태",
            Ability.AbilityType.ULTIMATE, 30f, 10f, 0f, 0f)
    };
```

**Sage (힐러)**:
```java
case "sage":
    return new Ability[] {
        // SMG: 0.2초 쿨타임, 16 데미지
        new Ability("sage_basic", "SMG", "빠른 기관단총",
            Ability.AbilityType.BASIC, 0.2f, 0f, 350f, 16f),
        
        // 치료: 60 HP 회복 (음수 데미지 = 힐)
        new Ability("sage_heal", "치료", "아군 또는 자신 회복",
            Ability.AbilityType.TACTICAL, 15f, 0f, 200f, -60f),
        
        // 부활: 100 HP로 부활 (90초 쿨타임!)
        new Ability("sage_revive", "부활", "쓰러진 아군 부활",
            Ability.AbilityType.ULTIMATE, 90f, 3f, 300f, -100f)
    };
```

**Bulldog (탱커)**:
```java
case "bulldog":
    return new Ability[] {
        // 미니건: 0.1초 쿨타임, 8 데미지 (초당 10발!)
        new Ability("bull_basic", "미니건", "고속 연사 미니건",
            Ability.AbilityType.BASIC, 0.1f, 0f, 400f, 8f),
        
        // 엄폐: 4초 지속 (방어력 대폭 증가)
        new Ability("bull_cover", "엄폐", "방어력 대폭 증가",
            Ability.AbilityType.TACTICAL, 12f, 4f, 0f, 0f),
        
        // 폭발탄 난사: 광역 폭발
        new Ability("bull_barrage", "폭발탄", "주변 광역 폭발",
            Ability.AbilityType.ULTIMATE, 35f, 0f, 0f, 0f)
    };
```

### 5. 기본값 처리
```java
default:
    // 기본값 (Raven)
    return new Ability[] {
        new Ability("default_basic", "기본 공격", "기본 공격",
            Ability.AbilityType.BASIC, 0.5f, 0f, 400f, 20f),
        new Ability("default_tactical", "전술 스킬", "전술 스킬",
            Ability.AbilityType.TACTICAL, 10f, 0f, 300f, 0f),
        new Ability("default_ultimate", "궁극기", "궁극기",
            Ability.AbilityType.ULTIMATE, 60f, 5f, 0f, 0f)
    };
```
- **폴백 메커니즘**: 알 수 없는 캐릭터 → 기본 스킬 반환
- **null 방지**: 항상 3개 스킬 배열 보장

---

## 💡 강점

### 1. 불변 설계 (Immutable)
- **모든 필드 final**: 생성 후 변경 불가
- **스레드 안전**: 멀티스레드 환경에서 안전
- **버그 방지**: 의도하지 않은 수정 차단

### 2. 명확한 Factory 패턴
```java
// 사용 예시
Ability[] ravenSkills = CharacterData.createAbilities("raven");
CharacterData ravenData = CharacterData.getById("raven");
```
- **단일 진입점**: `createAbilities()` 메서드 하나로 모든 스킬 생성
- **중앙 집중화**: 캐릭터 메타데이터가 한 곳에 집중

### 3. 풍부한 캐릭터 밸런스
| 캐릭터 | DPS (초당 데미지) | 사거리 | 특화 |
|--------|-------------------|--------|------|
| Raven | 50 (15 × 3.33) | 500 | 공격형 |
| Piper | 66.4 (80 × 0.83) | 1000 | 정찰형 |
| Technician | 50 (20 × 2.5) | 400 | 지원형 |
| General | 62.5 (25 × 2.5) | 600 | 밸런스형 |
| Ghost | 90 (18 × 5) | 300 | 암살형 |
| Bulldog | 80 (8 × 10) | 400 | 탱커 |

- **역할 다양성**: 공격, 정찰, 지원, 밸런스, 암살, 탱커, 힐러
- **트레이드오프**: 높은 사거리 = 낮은 연사, 높은 DPS = 짧은 사거리

### 4. 확장 가능한 구조
- **새 캐릭터 추가 용이**: `CHARACTERS` 배열에 추가만 하면 됨
- **스킬 추가 용이**: `createAbilities()`에 case 추가
- **중앙 관리**: 모든 밸런스 수치가 한 파일에 집중

### 5. 기본값 안전장치
- **null 대신 Raven**: `getById()` 실패 시 Raven 반환
- **기본 스킬**: 알 수 없는 캐릭터도 기본 스킬 제공
- **크래시 방지**: NullPointerException 원천 차단

---

## 🔧 개선 제안

### 1. HashMap 캐싱 (중요도: 중간)
**현재 상태**: `getById()` 메서드에서 선형 탐색

**문제점**:
- O(n) 시간복잡도 (캐릭터 수 × 호출 횟수)
- 매 호출마다 배열 순회

**제안**:
```java
public class CharacterData {
    // 기존 코드...
    
    // 정적 초기화 블록에서 HashMap 생성
    private static final Map<String, CharacterData> CHARACTER_MAP = new HashMap<>();
    
    static {
        for (CharacterData data : CHARACTERS) {
            CHARACTER_MAP.put(data.id.toLowerCase(), data);
        }
    }
    
    /**
     * O(1) 조회
     */
    public static CharacterData getById(String id) {
        CharacterData data = CHARACTER_MAP.get(id.toLowerCase());
        return (data != null) ? data : CHARACTERS[0]; // Raven
    }
}
```
**예상 효과**:
- 시간복잡도 O(n) → O(1)
- 빈번한 조회 시 성능 향상

### 2. Ability 배열 캐싱 (중요도: 높음)
**현재 상태**: `createAbilities()` 호출 시마다 새 Ability[] 생성

**문제점**:
- 매 호출마다 객체 생성 (GC 부담)
- 불필요한 중복 생성

**제안**:
```java
public class CharacterData {
    // Ability 배열 캐시
    private static final Map<String, Ability[]> ABILITY_CACHE = new HashMap<>();
    
    static {
        // 모든 캐릭터의 스킬 미리 생성
        for (CharacterData data : CHARACTERS) {
            ABILITY_CACHE.put(data.id, createAbilitiesInternal(data.id));
        }
    }
    
    /**
     * 캐시된 Ability 배열 반환 (객체 재사용)
     */
    public static Ability[] createAbilities(String characterId) {
        Ability[] cached = ABILITY_CACHE.get(characterId.toLowerCase());
        return (cached != null) ? cached : ABILITY_CACHE.get("raven");
    }
    
    /**
     * 실제 Ability 생성 로직 (private)
     */
    private static Ability[] createAbilitiesInternal(String characterId) {
        switch (characterId.toLowerCase()) {
            case "raven": return new Ability[] { /* ... */ };
            // ... (기존 switch 로직)
        }
    }
}
```
**예상 효과**:
- 메모리 사용량 감소
- GC 압력 감소
- 호출 속도 향상 (객체 생성 비용 제거)

### 3. Enum 기반 캐릭터 정의 (중요도: 낮음)
**현재 상태**: 배열 기반 (`CHARACTERS[]`)

**제안**:
```java
public enum Character {
    RAVEN("raven", "Raven", "빠른 기동성과 높은 화력", 
          100f, 6.5f, 20f, "공격형",
          new String[] { "고속 연사 권총", "대쉬", "과충전" }),
    
    PIPER("piper", "Piper", "장거리 스나이퍼",
          80f, 5.5f, 15f, "정찰형",
          new String[] { "저격 소총", "적 표시", "열감지 스코프" }),
    
    // ... 나머지 캐릭터
    ;
    
    public final String id;
    public final String name;
    public final String description;
    public final float health;
    public final float speed;
    public final float armor;
    public final String role;
    public final String[] abilities;
    
    Character(String id, String name, String description, 
              float health, float speed, float armor, 
              String role, String[] abilities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.health = health;
        this.speed = speed;
        this.armor = armor;
        this.role = role;
        this.abilities = abilities;
    }
    
    // O(1) 조회
    public static Character getById(String id) {
        for (Character c : values()) {
            if (c.id.equalsIgnoreCase(id)) {
                return c;
            }
        }
        return RAVEN; // 기본값
    }
}
```
**장점**:
- IDE 자동완성 지원 (`Character.RAVEN`)
- 타입 안전성 (컴파일 타임 체크)
- null 불가능 (Enum 특성)

**단점**:
- 기존 코드 대대적 수정 필요
- Enum은 상속 불가

### 4. 스킬 파라미터 명확화
**현재 상태**: Ability 생성자 파라미터가 위치에 의존

```java
// 무엇이 무엇인지 불명확
new Ability("raven_basic", "고속 연사", "빠른 연사 권총 공격",
    Ability.AbilityType.BASIC, 0.3f, 0f, 500f, 15f);
    // 0.3f = 쿨타임? 지속시간?
    // 500f = 사거리? 데미지?
```

**제안 1: Builder 패턴**
```java
new Ability.Builder("raven_basic", "고속 연사")
    .description("빠른 연사 권총 공격")
    .type(Ability.AbilityType.BASIC)
    .cooldown(0.3f)
    .duration(0f)
    .range(500f)
    .damage(15f)
    .build();
```

**제안 2: 파라미터 객체**
```java
public class AbilityStats {
    public float cooldown;
    public float duration;
    public float range;
    public float damage;
    
    public AbilityStats(float cooldown, float duration, float range, float damage) {
        this.cooldown = cooldown;
        this.duration = duration;
        this.range = range;
        this.damage = damage;
    }
}

// 사용
new Ability("raven_basic", "고속 연사", "빠른 연사 권총 공격",
    Ability.AbilityType.BASIC,
    new AbilityStats(0.3f, 0f, 500f, 15f));
```

### 5. 비활성 캐릭터 분리
**현재 상태**: 활성/비활성 캐릭터가 한 배열에 혼재

**제안**:
```java
public static final CharacterData[] ACTIVE_CHARACTERS = {
    CHARACTERS[0], // Raven
    CHARACTERS[1], // Piper
    CHARACTERS[2], // Technician
    CHARACTERS[3]  // General
};

public static final CharacterData[] INACTIVE_CHARACTERS = {
    CHARACTERS[4], // Bulldog
    CHARACTERS[5], // Wildcat
    // ... 나머지
};

/**
 * 활성 캐릭터만 반환
 */
public static CharacterData[] getActiveCharacters() {
    return ACTIVE_CHARACTERS;
}

/**
 * 모든 캐릭터 반환 (개발/테스트용)
 */
public static CharacterData[] getAllCharacters() {
    return CHARACTERS;
}
```
**장점**:
- 캐릭터 선택 UI에서 활성 캐릭터만 표시 가능
- 개발 모드에서 비활성 캐릭터 테스트 가능

### 6. 캐릭터 밸런스 검증
**현재 상태**: 잘못된 스탯 입력 시 컴파일은 성공하지만 게임 밸런스 깨짐

**제안**:
```java
public CharacterData(String id, String name, String description, 
                     float health, float speed, float armor,
                     String role, String[] abilities) {
    // 검증 로직 추가
    if (health <= 0 || health > 300) {
        throw new IllegalArgumentException("HP는 1~300 범위여야 함: " + health);
    }
    if (speed <= 0 || speed > 10) {
        throw new IllegalArgumentException("속도는 1~10 범위여야 함: " + speed);
    }
    if (armor < 0 || armor > 100) {
        throw new IllegalArgumentException("방어력은 0~100 범위여야 함: " + armor);
    }
    
    this.id = id;
    this.name = name;
    // ... (기존 코드)
}
```

### 7. 스킬 타입별 분리
**현재 상태**: `createAbilities()` 메서드가 291줄 중 177줄 차지 (61%)

**제안**:
```java
// 별도 클래스로 분리
public class RavenSkills {
    public static Ability[] create() {
        return new Ability[] {
            new Ability("raven_basic", "고속 연사", "빠른 연사 권총 공격",
                Ability.AbilityType.BASIC, 0.3f, 0f, 500f, 15f),
            new Ability("raven_dash", "대쉬", "빠르게 전방으로 돌진",
                Ability.AbilityType.TACTICAL, 5f, 0.5f, 200f, 0f),
            new Ability("raven_overcharge", "과충전", "공격 속도 대폭 증가",
                Ability.AbilityType.ULTIMATE, 20f, 6f, 0f, 0f)
        };
    }
}

public class PiperSkills { /* ... */ }
public class TechnicianSkills { /* ... */ }
// ... 나머지 캐릭터

// CharacterData에서 사용
public static Ability[] createAbilities(String characterId) {
    switch (characterId.toLowerCase()) {
        case "raven": return RavenSkills.create();
        case "piper": return PiperSkills.create();
        case "technician": return TechnicianSkills.create();
        // ...
        default: return DefaultSkills.create();
    }
}
```
**장점**:
- CharacterData.java 라인 수 감소 (291 → 100줄)
- 캐릭터별 스킬 독립 관리
- 테스트 용이

---

## 📊 코드 품질 평가

| 항목 | 점수 | 설명 |
|------|------|------|
| **불변성** | ⭐⭐⭐⭐⭐ | 모든 필드 final, 스레드 안전 |
| **Factory 패턴** | ⭐⭐⭐⭐☆ | createAbilities() 메서드 명확, 캐싱 없음 |
| **밸런스** | ⭐⭐⭐⭐⭐ | 10개 캐릭터 역할 다양, 트레이드오프 명확 |
| **확장성** | ⭐⭐⭐⭐☆ | 새 캐릭터 추가 쉬움, switch 수정 필요 |
| **가독성** | ⭐⭐⭐⭐☆ | 주석 풍부, 파라미터 의미 불명확 |
| **성능** | ⭐⭐⭐☆☆ | O(n) 조회, 매번 Ability 배열 생성 |

**총점: 4.3 / 5.0** ⭐⭐⭐⭐☆

---

## 🎓 결론

CharacterData.java는 **명확하고 확장 가능한 캐릭터 메타데이터 시스템**입니다. 특히 **불변 설계**, **Factory 패턴**, **풍부한 밸런스**가 인상적입니다.

### 주요 성과
1. ✅ **불변 설계**: 모든 필드 final로 스레드 안전 보장
2. ✅ **10개 캐릭터**: 다양한 역할 (공격, 정찰, 지원, 밸런스, 암살, 탱커, 힐러)
3. ✅ **Factory 패턴**: `createAbilities()` 메서드로 중앙 관리
4. ✅ **기본값 안전장치**: null 대신 Raven 반환
5. ✅ **밸런스**: 트레이드오프 명확 (높은 사거리 = 낮은 연사)

### 개선 방향
1. **Ability 캐싱**: 매번 객체 생성 대신 캐시 재사용 (메모리 절약)
2. **HashMap 조회**: O(n) → O(1) (성능 향상)
3. **스킬 분리**: 캐릭터별 스킬 클래스 분리 (291 → 100줄)
4. **Builder 패턴**: Ability 생성 시 파라미터 명확화

**프로덕션 레벨**이며, Ability 캐싱만 추가하면 **완벽한 캐릭터 시스템**입니다. 🎉