# KeyBindingConfig.java 코드 리뷰

## 📋 파일 개요
- **경로**: `src/com/fpsgame/client/KeyBindingConfig.java`
- **목적**: 사용자 정의 키 바인딩 관리 (이동, 스킬 등)
- **라인 수**: ~183줄

## 🎯 주요 기능

### 1. 키 바인딩 저장소
```java
private static final Map<String, Integer> keyBindings = new HashMap<>();
```
- 액션 이름 → 키 코드 매핑
- 예: "이동_앞" → KeyEvent.VK_W

### 2. 8가지 액션 지원
```java
KEY_MOVE_FORWARD     // 앞으로 이동
KEY_MOVE_BACKWARD    // 뒤로 이동
KEY_MOVE_LEFT        // 왼쪽 이동
KEY_MOVE_RIGHT       // 오른쪽 이동
KEY_TACTICAL_SKILL   // 전술 스킬
KEY_ULTIMATE_SKILL   // 궁극기
KEY_CHARACTER_SELECT // 캐릭터 선택 화면
KEY_MINIMAP_TOGGLE   // 미니맵 토글
```

### 3. 3단계 초기화 프로세스
```java
static {
    loadDefaultBindings();  // 1. 기본값 설정
    loadBindings();         // 2. 파일에서 로드
}
```

## ✅ 장점

### 1. **상수 기반 API**
```java
public static final String KEY_MOVE_FORWARD = "이동_앞";

// 사용 예시
int keyCode = KeyBindingConfig.getKey(KeyBindingConfig.KEY_MOVE_FORWARD);
```
**장점**:
- 오타 방지 (컴파일 타임 체크)
- IDE 자동완성 지원
- 리팩토링 안전

### 2. **안전한 기본값 제공**
```java
private static void loadDefaultBindings() {
    keyBindings.put(KEY_MOVE_FORWARD, KeyEvent.VK_W);
    // ...
}
```
**효과**:
- 파일 없어도 게임 실행 가능
- 첫 실행 사용자 경험 개선
- 리셋 기능 쉽게 구현 가능

### 3. **파일 존재 여부 체크**
```java
File configFile = new File(CONFIG_FILE);
if (!configFile.exists()) {
    return; // 기본값 사용
}
```
- 불필요한 I/O 방지
- 첫 실행 시 예외 발생 없음

### 4. **숫자 파싱 예외 처리**
```java
try {
    keyBindings.put(key, Integer.parseInt(value));
} catch (NumberFormatException e) {
    System.err.println("[KeyBindingConfig] Invalid key binding for " + key);
}
```
- 손상된 파일에도 견고
- 잘못된 항목만 무시하고 계속 진행

### 5. **완전한 CRUD 메서드**
| 메서드 | 기능 | 용도 |
|--------|------|------|
| `getKey()` | 조회 | 게임 플레이 중 키 확인 |
| `setKey()` | 수정 | 옵션 화면에서 키 변경 |
| `resetToDefaults()` | 초기화 | 기본값 복원 |
| `loadBindings()` | 로드 | 시작 시 불러오기 |
| `saveBindings()` | 저장 | 변경사항 저장 |

## ⚠️ 개선 가능 영역

### 1. **유틸리티 클래스 인스턴스화 방지**
**현재 코드:**
```java
public class KeyBindingConfig {
    // 생성자 없음
}
```

**개선 제안:**
```java
public final class KeyBindingConfig {
    private KeyBindingConfig() {
        throw new AssertionError("Cannot instantiate KeyBindingConfig");
    }
    // ...
}
```

### 2. **중복 키 검증 부족**
**현재 코드:**
```java
public static void setKey(String action, int keyCode) {
    keyBindings.put(action, keyCode); // 중복 가능
    saveBindings();
}
```

**문제점**:
- 여러 액션에 같은 키 할당 가능
- 예: "이동_앞"과 "이동_뒤" 모두 W 키

**개선 제안:**
```java
public static boolean setKey(String action, int keyCode) {
    // 이미 사용 중인 키인지 확인
    for (Map.Entry<String, Integer> entry : keyBindings.entrySet()) {
        if (entry.getValue() == keyCode && !entry.getKey().equals(action)) {
            System.err.println("[KeyBindingConfig] Key already assigned to: " + entry.getKey());
            return false;
        }
    }
    
    keyBindings.put(action, keyCode);
    saveBindings();
    return true;
}
```

### 3. **동시성 문제**
**현재 코드:**
```java
private static final Map<String, Integer> keyBindings = new HashMap<>();
```

**문제점**:
- `HashMap`은 thread-safe하지 않음
- 게임 플레이 중(`getKey()`) + 옵션 화면(`setKey()`) 동시 접근 가능

**개선 제안:**
```java
// 방법 1: ConcurrentHashMap 사용
private static final Map<String, Integer> keyBindings = new ConcurrentHashMap<>();

// 방법 2: 동기화
private static final Object LOCK = new Object();

public static int getKey(String action) {
    synchronized (LOCK) {
        return keyBindings.getOrDefault(action, -1);
    }
}
```

### 4. **초기화 실패 처리 부족**
**현재 코드:**
```java
static {
    loadDefaultBindings();
    loadBindings(); // 예외 발생 시?
}
```

**문제점**:
- static 초기화 블록에서 예외 발생 시 클래스 로드 실패
- `ExceptionInInitializerError` 발생

**개선 제안:**
```java
static {
    try {
        loadDefaultBindings();
        loadBindings();
    } catch (Exception e) {
        System.err.println("[KeyBindingConfig] Initialization failed, using defaults");
        e.printStackTrace(System.err);
    }
}
```

### 5. **getKey() 반환값 불명확**
**현재 코드:**
```java
public static int getKey(String action) {
    return keyBindings.getOrDefault(action, -1);
}
```

**문제점**:
- -1은 유효한 키 코드인가? (실제로는 아님)
- 호출자가 -1의 의미를 알아야 함

**개선 제안:**
```java
// 방법 1: Optional 사용
public static Optional<Integer> getKey(String action) {
    return Optional.ofNullable(keyBindings.get(action));
}

// 방법 2: 예외 던지기
public static int getKey(String action) {
    Integer keyCode = keyBindings.get(action);
    if (keyCode == null) {
        throw new IllegalArgumentException("Unknown action: " + action);
    }
    return keyCode;
}

// 방법 3: 상수로 명확히
public static final int KEY_NOT_BOUND = -1;
```

### 6. **saveBindings() 원자성 부족**
**현재 코드:**
```java
try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
    props.store(fos, "Key Bindings");
}
```

**문제점**:
- 쓰기 중 실패 시 파일 손상 가능
- 다음 실행 시 로드 불가

**개선 제안:**
```java
// 임시 파일에 쓴 후 원본과 교체
File tempFile = new File(CONFIG_FILE + ".tmp");
try (FileOutputStream fos = new FileOutputStream(tempFile)) {
    props.store(fos, "Key Bindings");
}

// 성공 시 원본 교체
File configFile = new File(CONFIG_FILE);
configFile.delete();
tempFile.renameTo(configFile);
```

### 7. **한글 액션 이름**
**현재 코드:**
```java
public static final String KEY_MOVE_FORWARD = "이동_앞";
```

**고려사항**:
- Properties 파일에 한글 키 저장
- 일부 시스템에서 인코딩 문제 가능

**대안:**
```java
// 내부 키는 영문, 표시명만 한글
public static final String KEY_MOVE_FORWARD = "move_forward";

private static final Map<String, String> ACTION_DISPLAY_NAMES = Map.of(
    "move_forward", "앞으로 이동",
    "move_backward", "뒤로 이동"
    // ...
);
```

## 🏗️ 아키텍처 분석

### 설계 패턴
- **싱글톤 변형**: static 메서드/필드로 전역 상태 관리
- **전략 패턴**: 키 매핑을 런타임에 변경 가능
- **파사드 패턴**: Properties API를 간단한 인터페이스로 감춤

### 의존성 그래프
```
KeyBindingConfig
    ├── java.awt.event.KeyEvent (키 코드 상수)
    ├── java.util.Map (저장소)
    └── java.util.Properties (파일 I/O)
```

### 파일 형식
```properties
# Key Bindings
이동_앞=87      # W키 (KeyEvent.VK_W)
이동_뒤=83      # S키
이동_왼쪽=65    # A키
이동_오른쪽=68  # D키
전술스킬=69     # E키
궁극기=82       # R키
캐릭터선택=66   # B키
미니맵=77       # M키
```

## 📊 성능 고려사항

### 메모리 사용
```java
Map<String, Integer> keyBindings
// 8개 항목 × ~40 bytes/entry = ~320 bytes (무시 가능)
```

### 조회 성능
```java
int keyCode = KeyBindingConfig.getKey(action);
// HashMap.get() = O(1) 평균 시간
```
- 게임 루프에서 빈번히 호출됨
- 현재 성능으로 충분 (나노초 수준)

### 저장 비용
```java
saveBindings(); // 디스크 I/O
```
- 비용: ~1-5ms
- 빈도: 옵션 화면에서 키 변경 시에만
- 최적화 불필요

## 🧪 테스트 시나리오

### 1. 기본 바인딩 테스트
```java
@Test
public void testDefaultBindings() {
    int wKey = KeyBindingConfig.getKey(KeyBindingConfig.KEY_MOVE_FORWARD);
    assertEquals(KeyEvent.VK_W, wKey);
}
```

### 2. 키 변경 테스트
```java
@Test
public void testSetKey() {
    KeyBindingConfig.setKey(KeyBindingConfig.KEY_MOVE_FORWARD, KeyEvent.VK_UP);
    int upKey = KeyBindingConfig.getKey(KeyBindingConfig.KEY_MOVE_FORWARD);
    assertEquals(KeyEvent.VK_UP, upKey);
}
```

### 3. 리셋 테스트
```java
@Test
public void testResetToDefaults() {
    KeyBindingConfig.setKey(KeyBindingConfig.KEY_MOVE_FORWARD, KeyEvent.VK_UP);
    KeyBindingConfig.resetToDefaults();
    int wKey = KeyBindingConfig.getKey(KeyBindingConfig.KEY_MOVE_FORWARD);
    assertEquals(KeyEvent.VK_W, wKey);
}
```

### 4. 영속성 테스트
```java
@Test
public void testPersistence() {
    KeyBindingConfig.setKey(KeyBindingConfig.KEY_MOVE_FORWARD, KeyEvent.VK_UP);
    KeyBindingConfig.saveBindings();
    
    // 새로운 프로세스 시뮬레이션
    KeyBindingConfig.loadBindings();
    
    int upKey = KeyBindingConfig.getKey(KeyBindingConfig.KEY_MOVE_FORWARD);
    assertEquals(KeyEvent.VK_UP, upKey);
}
```

### 5. 중복 키 테스트 (개선 후)
```java
@Test
public void testDuplicateKeyPrevention() {
    boolean result = KeyBindingConfig.setKey(KEY_MOVE_FORWARD, KeyEvent.VK_W);
    assertTrue(result);
    
    // 같은 키를 다른 액션에 할당 시도
    result = KeyBindingConfig.setKey(KEY_MOVE_BACKWARD, KeyEvent.VK_W);
    assertFalse(result); // 실패해야 함
}
```

## 📈 사용 예시

### 게임 플레이 중 키 확인
```java
// GamePanel.java
public void keyPressed(KeyEvent e) {
    int keyCode = e.getKeyCode();
    
    if (keyCode == KeyBindingConfig.getKey(KeyBindingConfig.KEY_MOVE_FORWARD)) {
        player.moveForward();
    } else if (keyCode == KeyBindingConfig.getKey(KeyBindingConfig.KEY_TACTICAL_SKILL)) {
        player.useTacticalSkill();
    }
    // ...
}
```

### 옵션 화면에서 키 변경
```java
// OptionDialog.java
JButton changeKeyButton = new JButton("W");
changeKeyButton.addActionListener(e -> {
    JOptionPane.showMessageDialog(this, "새 키를 입력하세요...");
    
    // 키 입력 대기
    keyInputPanel.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent ke) {
            int newKey = ke.getKeyCode();
            KeyBindingConfig.setKey(KeyBindingConfig.KEY_MOVE_FORWARD, newKey);
            changeKeyButton.setText(KeyEvent.getKeyText(newKey));
        }
    });
});
```

### 기본값 복원 버튼
```java
JButton resetButton = new JButton("기본값 복원");
resetButton.addActionListener(e -> {
    int confirm = JOptionPane.showConfirmDialog(
        this, 
        "모든 키를 기본값으로 되돌릴까요?",
        "확인",
        JOptionPane.YES_NO_OPTION
    );
    
    if (confirm == JOptionPane.YES_OPTION) {
        KeyBindingConfig.resetToDefaults();
        updateKeyButtonLabels(); // UI 업데이트
    }
});
```

## 🎓 학습 포인트

### 초보자를 위한 핵심 개념
1. **HashMap 사용법**: 키-값 매핑
2. **Properties 파일**: 간단한 설정 저장
3. **Static 초기화 블록**: 클래스 로드 시 실행

### 중급자를 위한 심화 개념
1. **전역 상태 관리**: static vs 싱글톤
2. **원자적 파일 쓰기**: 임시 파일 → 교체
3. **동시성 제어**: ConcurrentHashMap, synchronized

### 고급 주제
1. **키 입력 캡처**: KeyListener vs KeyBinding
2. **국제화(i18n)**: 액션 이름 다국어 지원
3. **키 조합**: Ctrl+S, Shift+E 등

## 🔍 코드 품질 평가

| 항목 | 평가 | 설명 |
|------|------|------|
| **가독성** | ⭐⭐⭐⭐⭐ | 명확한 메서드명, 충분한 주석 |
| **유지보수성** | ⭐⭐⭐⭐ | 명확한 책임 분리 |
| **확장성** | ⭐⭐⭐⭐ | 새 액션 추가 쉬움 |
| **성능** | ⭐⭐⭐⭐⭐ | 빠른 조회, 효율적 저장 |
| **안정성** | ⭐⭐⭐ | 예외 처리 있으나 개선 필요 |

## 📝 종합 평가

### 강점
✅ **완벽한 CRUD**: 조회/수정/삭제/초기화 모두 지원  
✅ **안전한 기본값**: 파일 없어도 작동  
✅ **간단한 API**: 3개 주요 메서드로 모든 기능  
✅ **우수한 문서화**: 상세한 한글 주석  

### 주요 약점
❌ **중복 키 미검증**: 같은 키를 여러 액션에 할당 가능  
❌ **동시성 미지원**: 멀티스레드 환경 문제 가능  
❌ **원자적 쓰기 부족**: 저장 중 실패 시 파일 손상  

### 개선 제안 우선순위
1. **중복 키 검증** (높음) - 사용성 크게 개선
2. **동시성 제어** (중간) - ConcurrentHashMap 사용
3. **원자적 파일 쓰기** (중간) - 데이터 무결성
4. **Private 생성자** (낮음) - 인스턴스화 방지
5. **Optional 반환** (낮음) - null 대신 명확한 타입

### 결론
**기능적으로 완성도 높은 키 바인딩 시스템**입니다. 기본 요구사항은 모두 충족하며, 코드 품질도 우수합니다. 주요 개선사항(중복 키 검증)을 적용하면 프로덕션 레벨로 향상됩니다.

**권장사항**:
1. **즉시 적용**: 중복 키 검증 (사용자 경험 크게 개선)
2. **다음 버전**: ConcurrentHashMap, 원자적 쓰기
3. **선택 적용**: 한글 키 → 영문 키 변환 (국제화 고려 시)

**확장 아이디어**:
- 키 조합 지원 (Ctrl+S)
- 프로필 시스템 (게이머용, 캐주얼용)
- 클라우드 동기화
