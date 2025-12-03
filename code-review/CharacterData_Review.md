# CharacterData.java 코드 리뷰

## 📋 파일 개요
- **경로**: `src/com/fpsgame/common/CharacterData.java`
- **목적**: 게임 캐릭터의 스탯 및 스킬 데이터 정의
- **라인 수**: 275줄
- **역할**: 캐릭터 메타데이터 제공 + 스킬 생성 팩토리

## 🎯 주요 기능

### 1. 캐릭터 메타데이터
```java
public final String id;          // "raven", "piper"
public final String name;        // "Raven", "Piper"
public final String description; // 설명
public final float health;       // 체력 (80~200)
public final float speed;        // 속도 (4.5~6.5)
public final float armor;        // 방어력 (1~40)
public final String role;        // 역할
public final String[] abilities; // 스킬 이름
```

### 2. 10개 캐릭터 데이터
- **사용 가능**: Raven, Piper, Technician, General
- **미구현**: Bulldog, Wildcat, Ghost, Skull, Steam, Sage

### 3. 스킬 생성 팩토리
```java
public static Ability[] createAbilities(String characterId) {
    switch (characterId.toLowerCase()) {
        case "raven": return [기본공격, 대쉬, 과충전];
        case "piper": return [저격, 적표시, 열감지];
        // ...
    }
}
```

## ✅ 장점

### 1. **불변 데이터 클래스**
```java
public final String id;
public final float health;
// 모든 필드가 final
```
**효과**:
- 생성 후 수정 불가
- Thread-safe
- 예측 가능한 동작

### 2. **중앙 집중식 데이터**
```java
public static final CharacterData[] CHARACTERS = { ... };
```
**장점**:
- 한 곳에서 모든 캐릭터 관리
- UI, 게임 로직 모두 동일 데이터 사용
- 데이터 불일치 방지

### 3. **타입 안전성**
```java
public final float health; // int가 아닌 float
```
- 소수점 스탯 지원 (100.5 체력)
- 정밀한 밸런싱 가능

### 4. **ID 기반 조회**
```java
public static CharacterData getById(String id) {
    for (CharacterData data : CHARACTERS) {
        if (data.id.equalsIgnoreCase(id)) {
            return data;
        }
    }
    return CHARACTERS[0]; // 폴백
}
```
- 대소문자 무시 ("RAVEN" == "raven")
- 기본값 제공 (실패 시 첫 캐릭터)

### 5. **팩토리 메서드 패턴**
```java
Ability[] abilities = CharacterData.createAbilities("raven");
```
- 캐릭터별 스킬 생성 로직 캡슐화
- switch 문으로 명확한 분기

### 6. **상세한 문서화**
```java
/**
 * 캐릭터 데이터
 * 
 * 주요 정보:
 * - 캐릭터 ID 및 이름
 * ...
 * 
 * 현재 사용 가능한 캐릭터:
 * - Raven (공격형)
 * ...
 */
```
- 한글 JavaDoc
- 사용 가능/불가능 명시

## ⚠️ 개선 가능 영역

### 1. **배열 직접 노출**
**현재 코드:**
```java
public final String[] abilities;

// 외부에서 수정 가능
CharacterData data = CHARACTERS[0];
data.abilities[0] = "Hacked!"; // 😱
```

**문제점**:
- `final` 참조지만 배열 내용은 변경 가능
- 불변성 보장 안 됨

**개선 제안:**
```java
public final List<String> abilities;

public CharacterData(..., String[] abilities) {
    this.abilities = List.of(abilities); // 불변 리스트
}

// 또는 방어적 복사
public String[] getAbilities() {
    return abilities.clone();
}
```

### 2. **CHARACTERS 배열도 노출**
**현재 코드:**
```java
public static final CharacterData[] CHARACTERS = { ... };

// 외부에서 수정 가능
CHARACTERS[0] = null; // 😱
```

**개선 제안:**
```java
private static final CharacterData[] CHARACTERS_INTERNAL = { ... };

public static List<CharacterData> getCharacters() {
    return List.of(CHARACTERS_INTERNAL); // 불변 리스트
}
```

### 3. **getById() 성능**
**현재 코드:**
```java
public static CharacterData getById(String id) {
    for (CharacterData data : CHARACTERS) { // O(n)
        if (data.id.equalsIgnoreCase(id)) {
            return data;
        }
    }
    return CHARACTERS[0];
}
```

**문제점**:
- 선형 탐색 O(n)
- 10개 캐릭터면 괜찮지만 확장 시 느림

**개선 제안:**
```java
private static final Map<String, CharacterData> CHARACTER_MAP = new HashMap<>();

static {
    for (CharacterData data : CHARACTERS) {
        CHARACTER_MAP.put(data.id.toLowerCase(), data);
    }
}

public static CharacterData getById(String id) {
    CharacterData data = CHARACTER_MAP.get(id.toLowerCase());
    return data != null ? data : CHARACTERS[0];
}
```

**효과**:
- O(n) → O(1)
- 대소문자 처리도 한 번만

### 4. **createAbilities() 중복 코드**
**현재 코드:**
```java
case "raven":
    return new Ability[] {
        new Ability("raven_basic", "고속 연사", ...),
        new Ability("raven_dash", "대쉬", ...),
        new Ability("raven_overcharge", "과충전", ...)
    };
case "piper":
    return new Ability[] {
        new Ability("piper_basic", "저격", ...),
        // ...
    };
```

**패턴 반복**:
- 모든 캐릭터가 3개 스킬 (기본/전술/궁극)
- 비슷한 구조 반복

**개선 제안 1: 빌더 패턴**
```java
private static Ability[] createAbilitiesFor(String prefix, 
                                            String basicName, String basicDesc,
                                            String tacticalName, String tacticalDesc,
                                            String ultimateName, String ultimateDesc) {
    return new Ability[] {
        new Ability(prefix + "_basic", basicName, basicDesc, BASIC, ...),
        new Ability(prefix + "_tactical", tacticalName, tacticalDesc, TACTICAL, ...),
        new Ability(prefix + "_ultimate", ultimateName, ultimateDesc, ULTIMATE, ...)
    };
}

case "raven":
    return createAbilitiesFor("raven",
        "고속 연사", "빠른 연사 권총 공격",
        "대쉬", "빠르게 전방으로 돌진",
        "과충전", "공격 속도 대폭 증가"
    );
```

**개선 제안 2: 데이터 파일**
```json
{
  "raven": {
    "basic": {"name": "고속 연사", "cooldown": 0.3, ...},
    "tactical": {"name": "대쉬", "cooldown": 5.0, ...},
    "ultimate": {"name": "과충전", "cooldown": 20.0, ...}
  }
}
```

### 5. **기본값 반환 불명확**
**현재 코드:**
```java
return CHARACTERS[0]; // 기본값: Raven
```

**문제점**:
- CHARACTERS[0]의 의미 불명확
- 배열 순서 변경 시 기본값도 변경됨

**개선 제안:**
```java
private static final CharacterData DEFAULT_CHARACTER = 
    new CharacterData("default", "Unknown", "기본 캐릭터", 100f, 5.0f, 10f, "기본", 
                      new String[]{"기본 공격", "기본 스킬", "기본 궁극기"});

public static CharacterData getById(String id) {
    // ...
    return DEFAULT_CHARACTER;
}

// 또는 명시적 상수
private static final int DEFAULT_CHARACTER_INDEX = 0;
return CHARACTERS[DEFAULT_CHARACTER_INDEX];
```

### 6. **스탯 검증 부족**
**현재 코드:**
```java
public CharacterData(..., float health, float speed, ...) {
    this.health = health; // 음수도 가능? 😱
    this.speed = speed;
}
```

**개선 제안:**
```java
public CharacterData(..., float health, float speed, float armor, ...) {
    if (health <= 0) {
        throw new IllegalArgumentException("Health must be positive");
    }
    if (speed <= 0) {
        throw new IllegalArgumentException("Speed must be positive");
    }
    if (armor < 0) {
        throw new IllegalArgumentException("Armor cannot be negative");
    }
    
    this.health = health;
    this.speed = speed;
    this.armor = armor;
    // ...
}
```

### 7. **캐릭터 사용 가능 여부 체크 부족**
**현재 코드:**
```java
// 사용 가능 캐릭터 확인 방법 없음
```

**개선 제안:**
```java
public final boolean available; // 사용 가능 여부

public CharacterData(..., boolean available) {
    this.available = available;
}

// CHARACTERS 정의 시
new CharacterData("raven", ..., true),  // 사용 가능
new CharacterData("bulldog", ..., false), // 미구현

// 사용 가능 캐릭터만 필터링
public static List<CharacterData> getAvailableCharacters() {
    return Arrays.stream(CHARACTERS)
                 .filter(c -> c.available)
                 .collect(Collectors.toList());
}
```

### 8. **toString() 메서드 부재**
**현재 코드:**
```java
System.out.println(CHARACTERS[0]);
// 출력: com.fpsgame.common.CharacterData@1a2b3c
```

**개선 제안:**
```java
@Override
public String toString() {
    return String.format("CharacterData{id='%s', name='%s', health=%.1f, speed=%.1f, armor=%.1f, role='%s'}",
                         id, name, health, speed, armor, role);
}

// 출력: CharacterData{id='raven', name='Raven', health=100.0, speed=6.5, armor=20.0, role='공격형'}
```

## 🏗️ 아키텍처 분석

### 설계 패턴
1. **Value Object**: 불변 데이터 객체
2. **Factory Method**: `createAbilities()` 팩토리
3. **Registry Pattern**: 중앙 `CHARACTERS` 배열

### 의존성
```
CharacterData
    └── Ability (스킬 데이터)
        └── AbilityType (BASIC/TACTICAL/ULTIMATE)
```

### 사용 위치
```
CharacterData
    ├── CharacterSelectDialog (캐릭터 목록 표시)
    ├── GamePanel (플레이어 스탯 적용)
    └── GameServer (서버 측 검증)
```

## 📊 데이터 분석

### 캐릭터 밸런싱
| 캐릭터 | 체력 | 속도 | 방어력 | 역할 |
|--------|------|------|--------|------|
| Raven | 100 | 6.5 | 20 | 공격형 |
| Piper | 80 | 5.5 | 15 | 정찰형 |
| Technician | 100 | 5.0 | 8 | 지원형 |
| General | 120 | 5.0 | 12 | 밸런스 |
| Bulldog | 200 | 4.5 | 40 | 탱커 |

**밸런스 인사이트**:
- Bulldog: 체력 2배, 느린 속도 (탱커)
- Piper: 낮은 체력, 빠른 속도 (스나이퍼)
- 체력 × 속도 × 방어력 = 밸런스 지수

### 메모리 사용
```java
CharacterData 객체: ~200 bytes
10개 × 200 bytes = 2KB (무시 가능)
```

## 🧪 테스트 시나리오

### 1. ID 기반 조회
```java
CharacterData raven = CharacterData.getById("raven");
assertEquals("Raven", raven.name);
assertEquals(100f, raven.health, 0.01);
```

### 2. 대소문자 무시
```java
CharacterData raven1 = CharacterData.getById("raven");
CharacterData raven2 = CharacterData.getById("RAVEN");
assertSame(raven1, raven2);
```

### 3. 존재하지 않는 ID
```java
CharacterData unknown = CharacterData.getById("unknown");
assertNotNull(unknown); // 기본값 반환
assertEquals("raven", unknown.id);
```

### 4. 스킬 생성
```java
Ability[] abilities = CharacterData.createAbilities("raven");
assertEquals(3, abilities.length);
assertEquals("고속 연사", abilities[0].name);
```

### 5. 불변성 테스트 (개선 후)
```java
CharacterData data = CHARACTERS[0];
String[] abilities = data.abilities;
abilities[0] = "Hacked";
assertNotEquals("Hacked", data.abilities[0]); // 원본 보호됨
```

## 📈 사용 예시

### 캐릭터 선택 UI
```java
// CharacterSelectDialog.java
for (CharacterData character : CharacterData.CHARACTERS) {
    JButton btn = new JButton(character.name);
    btn.addActionListener(e -> {
        showCharacterInfo(character);
    });
    panel.add(btn);
}

private void showCharacterInfo(CharacterData character) {
    JLabel healthLabel = new JLabel("체력: " + character.health);
    JLabel speedLabel = new JLabel("속도: " + character.speed);
    JLabel armorLabel = new JLabel("방어력: " + character.armor);
    JLabel roleLabel = new JLabel("역할: " + character.role);
    
    JList<String> skillList = new JList<>(character.abilities);
    // ...
}
```

### 게임 플레이 적용
```java
// Player.java
public class Player {
    private CharacterData characterData;
    private float currentHealth;
    
    public Player(String characterId) {
        this.characterData = CharacterData.getById(characterId);
        this.currentHealth = characterData.health;
    }
    
    public void move() {
        float moveSpeed = characterData.speed;
        x += moveSpeed * Math.cos(angle);
        y += moveSpeed * Math.sin(angle);
    }
    
    public void takeDamage(float damage) {
        float reducedDamage = damage * (100 - characterData.armor) / 100;
        currentHealth -= reducedDamage;
    }
}
```

### 스킬 시스템
```java
// SkillManager.java
Ability[] abilities = CharacterData.createAbilities(player.getCharacterId());

// Q키: 기본 공격
if (keyCode == KeyEvent.VK_Q) {
    abilities[0].activate(player);
}

// E키: 전술 스킬
if (keyCode == KeyEvent.VK_E) {
    abilities[1].activate(player);
}

// R키: 궁극기
if (keyCode == KeyEvent.VK_R) {
    abilities[2].activate(player);
}
```

## 🎓 학습 포인트

### 초보자를 위한 핵심 개념
1. **불변 객체**: `final` 필드로 데이터 보호
2. **배열 vs 리스트**: 고정 크기 데이터
3. **팩토리 메서드**: 객체 생성 로직 분리

### 중급자를 위한 심화 개념
1. **Value Object 패턴**: 불변 데이터 클래스
2. **방어적 복사**: 배열/리스트 보호
3. **HashMap 최적화**: O(n) → O(1) 조회

### 고급 주제
1. **데이터 파일 로딩**: JSON/XML 파싱
2. **핫 리로딩**: 런타임 데이터 갱신
3. **밸런스 조정 툴**: 에디터 개발

## 🔍 코드 품질 평가

| 항목 | 평가 | 설명 |
|------|------|------|
| **가독성** | ⭐⭐⭐⭐⭐ | 매우 명확한 데이터 구조 |
| **유지보수성** | ⭐⭐⭐⭐ | 중앙 집중식 관리 |
| **확장성** | ⭐⭐⭐⭐ | 새 캐릭터 쉽게 추가 |
| **성능** | ⭐⭐⭐ | 선형 탐색, 개선 여지 있음 |
| **안정성** | ⭐⭐⭐ | 불변성 부분적, 검증 부족 |

## 📝 종합 평가

### 강점
✅ **불변 필드**: final 키워드 활용  
✅ **중앙 집중**: 모든 캐릭터 한 곳에서 관리  
✅ **팩토리 패턴**: 스킬 생성 로직 캡슐화  
✅ **상세 문서**: 한글 JavaDoc  

### 주요 약점
❌ **배열 노출**: 불변성 완전하지 않음  
❌ **선형 탐색**: O(n) 조회 성능  
❌ **스탯 검증 부족**: 음수 값 가능  

### 개선 제안 우선순위
1. **HashMap 조회** (높음) - 성능 개선
2. **배열 방어적 복사** (높음) - 불변성 보장
3. **스탯 검증** (중간) - 데이터 무결성
4. **available 필드** (중간) - 사용 가능 여부
5. **toString() 추가** (낮음) - 디버깅 편의
6. **데이터 파일화** (낮음) - 유연성 향상

### 결론
**잘 설계된 데이터 클래스**입니다. 기본 요구사항은 모두 충족하며, Value Object 패턴을 잘 따릅니다. 성능 최적화와 불변성 강화만 추가하면 완벽합니다.

**권장사항**:
1. **즉시 적용**:
   - HashMap 기반 조회
   - List.of()로 불변 리스트
   
2. **다음 버전**:
   - 스탯 검증
   - available 필드
   
3. **장기 계획**:
   - JSON 데이터 파일
   - 밸런스 조정 툴

**확장 아이디어**:
- 스킨 시스템 (같은 캐릭터, 다른 외형)
- 레벨별 스탯 성장
- 패시브 스킬 추가
- 캐릭터 업그레이드 트리
