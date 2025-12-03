# GameConfig.java & KeyBindingConfig.java 코드 리뷰

## 📋 파일 개요

### GameConfig.java
- **경로**: `src/com/fpsgame/client/GameConfig.java`
- **역할**: 게임 설정 저장/로드
- **라인 수**: 81줄
- **저장 형식**: Properties 파일 (game_config.properties)
- **주요 기능**: 선택한 캐릭터 저장/로드

### KeyBindingConfig.java
- **경로**: `src/com/fpsgame/client/KeyBindingConfig.java`
- **역할**: 키 바인딩 설정 관리
- **라인 수**: 182줄
- **저장 형식**: Properties 파일 (keybindings.properties)
- **주요 기능**: 키 바인딩 저장/로드, 기본값 관리

---

## 🎯 GameConfig.java 주요 기능

### 1. Properties 파일 저장
```java
public class GameConfig {
    /** 설정 파일 경로 */
    private static final String CONFIG_FILE = "game_config.properties";
    
    /** 캐릭터 설정 키 */
    private static final String KEY_CHARACTER = "selected_character";
    
    /** 캐릭터 기본값 (null = 미선택 상태) */
    private static final String DEFAULT_CHARACTER = null;
}
```
**Properties 파일 형식**:
```properties
# game_config.properties
selected_character=raven
```

### 2. 캐릭터 저장
```java
/**
 * 선택한 캐릭터를 설정 파일에 저장
 */
public static void saveCharacter(String characterId) {
    Properties props = new Properties();
    
    // 1단계: 기존 설정 로드 (다른 설정 덮어쓰지 않기)
    try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
        props.load(in);
    } catch (IOException e) {
        // 파일이 없으면 새로 생성되므로 무시
    }

    // 2단계: 캐릭터 ID 정규화 (공백 제거, 소문자 변환)
    String normalized = characterId != null ? characterId.trim().toLowerCase() : "";
    props.setProperty(KEY_CHARACTER, normalized);

    // 3단계: 설정 파일에 저장
    try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
        props.store(out, "Game Configuration");
    } catch (IOException e) {
        System.err.println("[Config] Failed to save character: " + characterId);
        e.printStackTrace(System.err);
    }
}
```
**저장 과정**:
1. **기존 설정 로드**: 다른 설정 유지
2. **정규화**: `trim()` + `toLowerCase()` (대소문자 무시)
3. **파일 저장**: `props.store()` 호출

**try-with-resources**:
- 자동으로 Stream 닫기
- 메모리 누수 방지

### 3. 캐릭터 로드
```java
/**
 * 저장된 캐릭터 정보를 로드
 */
public static String loadCharacter() {
    Properties props = new Properties();
    
    try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
        props.load(in);
        String value = props.getProperty(KEY_CHARACTER);
        
        // 값이 존재하고 비어있지 않으면 반환
        if (value != null) {
            value = value.trim().toLowerCase();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return DEFAULT_CHARACTER;
    } catch (IOException e) {
        // 파일이 없거나 읽기 실패 시 기본값 반환
        return DEFAULT_CHARACTER;
    }
}
```
**검증 단계**:
1. **null 체크**: `value != null`
2. **정규화**: `trim().toLowerCase()`
3. **빈 문자열 체크**: `!value.isEmpty()`
4. **기본값 반환**: 실패 시 `null`

---

## 🎯 KeyBindingConfig.java 주요 기능

### 1. 키 바인딩 저장소
```java
public class KeyBindingConfig {
    /** 키 바인딩 설정 파일 경로 */
    private static final String CONFIG_FILE = "keybindings.properties";
    
    /** 키 바인딩 맵 (액션 이름 -> 키 코드) */
    private static final Map<String, Integer> keyBindings = new HashMap<>();

    // 키 액션 상수
    public static final String KEY_MOVE_FORWARD = "이동_앞";
    public static final String KEY_MOVE_BACKWARD = "이동_뒤";
    public static final String KEY_MOVE_LEFT = "이동_왼쪽";
    public static final String KEY_MOVE_RIGHT = "이동_오른쪽";
    public static final String KEY_TACTICAL_SKILL = "전술스킬";
    public static final String KEY_ULTIMATE_SKILL = "궁극기";
    public static final String KEY_CHARACTER_SELECT = "캐릭터선택";
    public static final String KEY_MINIMAP_TOGGLE = "미니맵";
}
```
**8개 액션**:
- 이동: 앞, 뒤, 왼쪽, 오른쪽
- 스킬: 전술스킬 (E), 궁극기 (R)
- UI: 캐릭터 선택 (B), 미니맵 (M)

### 2. 정적 초기화 블록
```java
static {
    // 클래스 로드 시 기본 바인딩 설정 후 파일에서 로드
    loadDefaultBindings();
    loadBindings();
}
```
**실행 순서**:
1. **기본 바인딩**: WASD, E, R, B, M
2. **파일 로드**: 사용자 설정 덮어쓰기

### 3. 기본 키 바인딩
```java
/**
 * 기본 키 바인딩 설정
 */
private static void loadDefaultBindings() {
    keyBindings.put(KEY_MOVE_FORWARD, KeyEvent.VK_W);     // W
    keyBindings.put(KEY_MOVE_BACKWARD, KeyEvent.VK_S);    // S
    keyBindings.put(KEY_MOVE_LEFT, KeyEvent.VK_A);        // A
    keyBindings.put(KEY_MOVE_RIGHT, KeyEvent.VK_D);       // D
    keyBindings.put(KEY_TACTICAL_SKILL, KeyEvent.VK_E);   // E
    keyBindings.put(KEY_ULTIMATE_SKILL, KeyEvent.VK_R);   // R
    keyBindings.put(KEY_CHARACTER_SELECT, KeyEvent.VK_B); // B
    keyBindings.put(KEY_MINIMAP_TOGGLE, KeyEvent.VK_M);   // M
}
```
**FPS 표준 키 배치**:
```
Q  [W]  E  [R]
 [A] S [D]
```

### 4. 키 바인딩 로드
```java
/**
 * 키 바인딩을 파일에서 로드
 */
public static void loadBindings() {
    File configFile = new File(CONFIG_FILE);
    if (!configFile.exists()) {
        return; // 파일이 없으면 기본값 사용
    }

    Properties props = new Properties();
    try (FileInputStream fis = new FileInputStream(configFile)) {
        props.load(fis);

        // 각 액션에 대해 저장된 키 코드 로드
        for (String key : keyBindings.keySet()) {
            String value = props.getProperty(key);
            if (value != null) {
                try {
                    keyBindings.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    System.err.println("[KeyBindingConfig] Invalid key binding for " + key + ": " + value);
                }
            }
        }

        System.out.println("[KeyBindingConfig] 키 바인딩 로드 완료");
    } catch (IOException e) {
        System.err.println("[KeyBindingConfig] 키 바인딩 로드 실패: " + e.getMessage());
        e.printStackTrace(System.err);
    }
}
```
**에러 처리**:
- **파일 없음**: 기본값 사용 (조용히 무시)
- **NumberFormatException**: 잘못된 키 코드 (에러 로그)
- **IOException**: 파일 읽기 실패 (스택 트레이스)

### 5. 키 바인딩 저장
```java
/**
 * 키 바인딩을 파일에 저장
 */
public static void saveBindings() {
    Properties props = new Properties();

    // 모든 키 바인딩을 Properties에 저장
    for (Map.Entry<String, Integer> entry : keyBindings.entrySet()) {
        props.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
    }

    try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
        props.store(fos, "FPS Game Key Bindings Configuration");
        System.out.println("[KeyBindingConfig] 키 바인딩 저장 완료");
    } catch (IOException e) {
        System.err.println("[KeyBindingConfig] 키 바인딩 저장 실패: " + e.getMessage());
        e.printStackTrace(System.err);
    }
}
```
**저장 형식** (keybindings.properties):
```properties
# FPS Game Key Bindings Configuration
이동_앞=87
이동_뒤=83
이동_왼쪽=65
이동_오른쪽=68
전술스킬=69
궁극기=82
캐릭터선택=66
미니맵=77
```

### 6. API 메서드
```java
/**
 * 특정 액션의 키 코드 가져오기
 */
public static int getKey(String action) {
    return keyBindings.getOrDefault(action, KeyEvent.VK_UNDEFINED);
}

/**
 * 특정 액션의 키 설정
 */
public static void setKey(String action, int keyCode) {
    keyBindings.put(action, keyCode);
}

/**
 * 모든 키 바인딩 가져오기 (복사본)
 */
public static Map<String, Integer> getAllBindings() {
    return new HashMap<>(keyBindings); // 원본 보호
}

/**
 * 키 바인딩 초기화 (기본값으로)
 */
public static void resetToDefaults() {
    loadDefaultBindings();
    saveBindings();
}

/**
 * 특정 키가 특정 액션에 바인딩되어 있는지 확인
 */
public static boolean isKeyPressed(int keyCode, String action) {
    return keyCode == getKey(action);
}
```

---

## 💡 강점

### GameConfig.java

#### 1. 간단한 API
```java
// 저장
GameConfig.saveCharacter("raven");

// 로드
String character = GameConfig.loadCharacter(); // "raven" 또는 null
```

#### 2. 기존 설정 유지
```java
// 기존 설정 로드 후 캐릭터만 업데이트
try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
    props.load(in);
} catch (IOException e) {
    // 파일이 없으면 새로 생성
}
```

#### 3. 입력 정규화
```java
String normalized = characterId != null ? characterId.trim().toLowerCase() : "";
// "Raven " -> "raven"
// "PIPER" -> "piper"
```

### KeyBindingConfig.java

#### 1. 타입 안전한 상수
```java
// 문자열 상수로 오타 방지
public static final String KEY_MOVE_FORWARD = "이동_앞";

// 사용
int key = KeyBindingConfig.getKey(KEY_MOVE_FORWARD);
```

#### 2. 자동 초기화
```java
static {
    loadDefaultBindings(); // 기본값
    loadBindings();        // 사용자 설정
}
```

#### 3. 원본 보호
```java
public static Map<String, Integer> getAllBindings() {
    return new HashMap<>(keyBindings); // 복사본 반환
}
```

#### 4. 기본값 초기화
```java
public static void resetToDefaults() {
    loadDefaultBindings();
    saveBindings();
}
```

---

## 🔧 개선 제안

### 1. 설정 클래스 통합 (중요도: 중간)
**현재 상태**: 2개 파일 (GameConfig, KeyBindingConfig)

**제안**:
```java
/**
 * 통합 설정 관리 클래스
 */
public class ConfigManager {
    private static final String CONFIG_FILE = "game_config.properties";
    private static ConfigManager instance;
    
    private final Properties props = new Properties();
    
    // 싱글턴
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
            instance.load();
        }
        return instance;
    }
    
    // 캐릭터 설정
    public void setCharacter(String characterId) {
        props.setProperty("character", characterId.trim().toLowerCase());
    }
    
    public String getCharacter() {
        return props.getProperty("character", null);
    }
    
    // 키 바인딩
    public void setKeyBinding(String action, int keyCode) {
        props.setProperty("key." + action, String.valueOf(keyCode));
    }
    
    public int getKeyBinding(String action, int defaultKey) {
        String value = props.getProperty("key." + action);
        try {
            return value != null ? Integer.parseInt(value) : defaultKey;
        } catch (NumberFormatException e) {
            return defaultKey;
        }
    }
    
    // 저장/로드
    public void save() { /* ... */ }
    public void load() { /* ... */ }
}
```

### 2. JSON 형식 사용 (중요도: 낮음)
**현재 상태**: Properties 파일

**제안** (Gson 라이브러리 사용):
```java
public class GameConfig {
    public String selectedCharacter;
    public Map<String, Integer> keyBindings;
    
    public void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter("game_config.json")) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static GameConfig load() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader("game_config.json")) {
            return gson.fromJson(reader, GameConfig.class);
        } catch (IOException e) {
            return new GameConfig(); // 기본값
        }
    }
}
```
**JSON 형식**:
```json
{
  "selectedCharacter": "raven",
  "keyBindings": {
    "이동_앞": 87,
    "이동_뒤": 83,
    "전술스킬": 69,
    "궁극기": 82
  }
}
```

### 3. 키 중복 검증 (중요도: 높음)
**현재 상태**: 중복 키 허용

**문제점**:
```java
KeyBindingConfig.setKey(KEY_MOVE_FORWARD, KeyEvent.VK_W);
KeyBindingConfig.setKey(KEY_MOVE_BACKWARD, KeyEvent.VK_W); // 중복!
```

**제안**:
```java
/**
 * 키 설정 (중복 검증)
 * 
 * @return 성공 여부
 */
public static boolean setKey(String action, int keyCode) {
    // 다른 액션에 이미 할당된 키인지 확인
    for (Map.Entry<String, Integer> entry : keyBindings.entrySet()) {
        if (!entry.getKey().equals(action) && entry.getValue() == keyCode) {
            System.err.println("[KeyBindingConfig] 키 " + keyCode + " 는 이미 " + entry.getKey() + " 에 할당됨");
            return false; // 중복
        }
    }
    
    keyBindings.put(action, keyCode);
    return true; // 성공
}

// 사용
if (!KeyBindingConfig.setKey(KEY_MOVE_FORWARD, KeyEvent.VK_W)) {
    showError("이미 사용 중인 키입니다!");
}
```

### 4. 설정 변경 리스너 (중요도: 중간)
**현재 상태**: 설정 변경 알림 없음

**제안**:
```java
public class KeyBindingConfig {
    private static final List<ConfigChangeListener> listeners = new ArrayList<>();
    
    public interface ConfigChangeListener {
        void onKeyBindingChanged(String action, int oldKey, int newKey);
    }
    
    public static void addListener(ConfigChangeListener listener) {
        listeners.add(listener);
    }
    
    public static void setKey(String action, int keyCode) {
        int oldKey = keyBindings.get(action);
        keyBindings.put(action, keyCode);
        
        // 리스너 알림
        for (ConfigChangeListener listener : listeners) {
            listener.onKeyBindingChanged(action, oldKey, keyCode);
        }
    }
}

// 사용
KeyBindingConfig.addListener((action, oldKey, newKey) -> {
    System.out.println(action + " 키가 " + oldKey + " -> " + newKey + " 로 변경됨");
    updateUI(); // UI 갱신
});
```

### 5. 설정 검증 (중요도: 중간)
**현재 상태**: 잘못된 값 허용

**제안**:
```java
/**
 * 캐릭터 ID 검증
 */
public static void saveCharacter(String characterId) {
    // 검증
    if (characterId == null || characterId.trim().isEmpty()) {
        throw new IllegalArgumentException("캐릭터 ID는 비어있을 수 없습니다");
    }
    
    String normalized = characterId.trim().toLowerCase();
    
    // 유효한 캐릭터인지 확인
    if (CharacterData.getById(normalized) == null) {
        throw new IllegalArgumentException("존재하지 않는 캐릭터: " + normalized);
    }
    
    // ... (기존 저장 로직)
}
```

### 6. 설정 백업 (중요도: 낮음)
**현재 상태**: 백업 없음

**제안**:
```java
/**
 * 설정 파일 백업
 */
public static void backup() {
    File original = new File(CONFIG_FILE);
    if (!original.exists()) return;
    
    File backup = new File(CONFIG_FILE + ".backup");
    try {
        Files.copy(original.toPath(), backup.toPath(), 
                   StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[Config] 백업 완료: " + backup.getName());
    } catch (IOException e) {
        System.err.println("[Config] 백업 실패");
    }
}

/**
 * 설정 복원
 */
public static void restore() {
    File backup = new File(CONFIG_FILE + ".backup");
    if (!backup.exists()) {
        System.err.println("[Config] 백업 파일이 없습니다");
        return;
    }
    
    File original = new File(CONFIG_FILE);
    try {
        Files.copy(backup.toPath(), original.toPath(), 
                   StandardCopyOption.REPLACE_EXISTING);
        loadBindings(); // 다시 로드
        System.out.println("[Config] 복원 완료");
    } catch (IOException e) {
        System.err.println("[Config] 복원 실패");
    }
}
```

---

## 📊 코드 품질 평가

| 항목 | GameConfig | KeyBindingConfig |
|------|------------|------------------|
| **간결성** | ⭐⭐⭐⭐⭐ (81줄) | ⭐⭐⭐⭐☆ (182줄) |
| **API 설계** | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ |
| **에러 처리** | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ |
| **검증** | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ |
| **확장성** | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ |

**GameConfig 총점: 3.8 / 5.0** ⭐⭐⭐⭐☆  
**KeyBindingConfig 총점: 4.2 / 5.0** ⭐⭐⭐⭐☆

---

## 🎓 결론

두 설정 관리 클래스는 **간결하고 실용적인 설정 시스템**입니다.

### 주요 성과
1. ✅ **Properties 파일**: 간단한 저장/로드
2. ✅ **try-with-resources**: 자동 리소스 관리
3. ✅ **기본값 지원**: 파일 없을 때 안전
4. ✅ **정규화**: 대소문자 무시, 공백 제거
5. ✅ **타입 안전 상수**: 오타 방지 (KeyBindingConfig)

### 개선 방향
1. **키 중복 검증**: setKey() 메서드에 추가 (필수!)
2. **설정 통합**: ConfigManager 싱글턴 클래스
3. **변경 리스너**: 설정 변경 알림
4. **검증 강화**: 잘못된 값 차단

**프로덕션 레벨**이며, 키 중복 검증만 추가하면 **완벽한 설정 시스템**입니다. 🎉
