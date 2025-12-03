# GameConfig.java 코드 리뷰

## 📋 파일 개요
- **경로**: `src/com/fpsgame/client/GameConfig.java`
- **목적**: 게임 설정(선택한 캐릭터)을 Properties 파일로 저장/로드
- **라인 수**: 75줄

## 🎯 주요 기능

### 1. 캐릭터 선택 영속성
```java
public static void saveCharacter(String characterId)
public static String loadCharacter()
```
- 사용자가 선택한 캐릭터를 파일에 저장
- 게임 재시작 시 이전 선택 복원

### 2. Properties 파일 관리
```java
private static final String CONFIG_FILE = "game_config.properties";
private static final String KEY_CHARACTER = "selected_character";
```
- 키=값 형태의 텍스트 파일
- 사람이 읽고 수정 가능한 형식

## ✅ 장점

### 1. **유틸리티 클래스 설계**
```java
// 모든 메서드가 static
public class GameConfig {
    public static void saveCharacter(String characterId) { ... }
    public static String loadCharacter() { ... }
}
```
- 인스턴스 생성 불필요
- 전역적으로 접근 가능한 설정 관리

### 2. **기존 설정 보존**
```java
// 기존 파일 로드 후 병합
try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
    props.load(in);
} catch (IOException e) {
    // 파일 없으면 새로 생성
}
props.setProperty(KEY_CHARACTER, normalized);
```
- 다른 설정을 덮어쓰지 않음
- 확장성 좋음 (향후 다른 설정 추가 가능)

### 3. **입력 정규화**
```java
String normalized = characterId != null ? characterId.trim().toLowerCase() : "";
```
- 공백 제거
- 소문자 변환
- 일관된 데이터 저장

### 4. **안전한 예외 처리**
```java
} catch (IOException e) {
    System.err.println("[Config] Failed to save character: " + characterId);
    e.printStackTrace(System.err);
}
```
- 저장 실패해도 게임 크래시 방지
- 에러 로그로 디버깅 가능

### 5. **Try-with-resources 사용**
```java
try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
    props.load(in);
}
```
- 자동 리소스 해제
- 메모리 누수 방지

## ⚠️ 개선 가능 영역

### 1. **유틸리티 클래스 인스턴스화 방지**
**현재 코드:**
```java
public class GameConfig {
    // 생성자 없음
}
```

**문제점**:
- 기본 생성자로 인스턴스 생성 가능
- `new GameConfig()` 실행 가능 (의미 없음)

**개선 제안:**
```java
public final class GameConfig {
    private GameConfig() {
        throw new AssertionError("Cannot instantiate GameConfig");
    }
    // ...
}
```

**효과**:
- 인스턴스 생성 차단
- 유틸리티 클래스 의도 명확화

### 2. **빈 문자열 저장 문제**
**현재 코드:**
```java
String normalized = characterId != null ? characterId.trim().toLowerCase() : "";
props.setProperty(KEY_CHARACTER, normalized); // "" 저장 가능
```

**문제점**:
- 빈 문자열도 저장됨
- `loadCharacter()`에서 다시 필터링 필요

**개선 제안:**
```java
public static void saveCharacter(String characterId) {
    if (characterId == null || characterId.trim().isEmpty()) {
        System.err.println("[Config] Invalid character ID: " + characterId);
        return; // 저장하지 않음
    }
    
    String normalized = characterId.trim().toLowerCase();
    // ...
}
```

### 3. **상수화 부족**
**현재 코드:**
```java
private static final String DEFAULT_CHARACTER = null;

return DEFAULT_CHARACTER; // 여러 곳에서 반복
```

**개선 제안:**
```java
private static final String DEFAULT_CHARACTER = "raven"; // 명확한 기본값

// 또는 Optional 사용
public static Optional<String> loadCharacter() {
    // ...
    return Optional.ofNullable(value);
}
```

### 4. **설정 파일 경로 하드코딩**
**현재 코드:**
```java
private static final String CONFIG_FILE = "game_config.properties";
```

**문제점**:
- 실행 디렉토리에 의존
- 다중 사용자 환경 문제

**개선 제안:**
```java
private static final String CONFIG_FILE = 
    System.getProperty("user.home") + "/.netfps/game_config.properties";

// 또는 OS별 적절한 경로
// Windows: %APPDATA%\NetFps\config.properties
// Linux: ~/.config/netfps/config.properties
```

### 5. **동시성 이슈**
**현재 코드:**
```java
public static void saveCharacter(String characterId) {
    // 파일 읽기 -> 수정 -> 쓰기 (원자적이지 않음)
}
```

**문제점**:
- 여러 스레드가 동시에 저장 시 데이터 손상 가능
- Race condition 발생 가능

**개선 제안:**
```java
private static final Object LOCK = new Object();

public static void saveCharacter(String characterId) {
    synchronized (LOCK) {
        // 파일 작업
    }
}
```

### 6. **로드 실패 로깅 부족**
**현재 코드:**
```java
} catch (IOException e) {
    // 조용히 기본값 반환
    return DEFAULT_CHARACTER;
}
```

**개선 제안:**
```java
} catch (IOException e) {
    System.out.println("[Config] Config file not found, using default character");
    return DEFAULT_CHARACTER;
}
```

## 🏗️ 아키텍처 분석

### 설계 패턴
- **싱글톤 변형**: static 메서드로 전역 접근
- **퍼사드 패턴**: Properties API를 간단한 인터페이스로 감춤

### 의존성
```
GameConfig
    └── java.util.Properties (설정 관리)
    └── java.io.* (파일 입출력)
```
- 표준 라이브러리만 사용
- 외부 의존성 없음

### 파일 형식
```properties
# Game Configuration
selected_character=raven
```
- 사람이 읽기 쉬움
- 수동 편집 가능
- Git 버전 관리 적합

## 📊 성능 고려사항

### 현재 성능 특성
```java
// 매번 파일 I/O 발생
saveCharacter("raven");  // 파일 쓰기
loadCharacter();         // 파일 읽기
```

**비용**:
- 디스크 I/O: ~1-5ms (HDD), ~0.1-1ms (SSD)
- 게임 시작/종료 시에만 호출되므로 허용 가능

### 최적화 옵션 (필요 시)

#### 1. 인메모리 캐싱
```java
private static Properties cachedProps = null;

public static void saveCharacter(String characterId) {
    if (cachedProps == null) {
        cachedProps = new Properties();
        loadFromFile(cachedProps);
    }
    cachedProps.setProperty(KEY_CHARACTER, normalized);
    saveToFile(cachedProps);
}
```

#### 2. 지연 쓰기
```java
private static ScheduledExecutorService scheduler = 
    Executors.newSingleThreadScheduledExecutor();

public static void saveCharacter(String characterId) {
    // 메모리에만 저장
    cachedProps.setProperty(KEY_CHARACTER, normalized);
    
    // 5초 후 디스크에 쓰기 (중복 쓰기 방지)
    scheduler.schedule(() -> saveToFile(cachedProps), 5, TimeUnit.SECONDS);
}
```

## 🧪 테스트 시나리오

### 1. 정상 저장/로드
```java
GameConfig.saveCharacter("piper");
String loaded = GameConfig.loadCharacter();
assertEquals("piper", loaded);
```

### 2. 공백 처리
```java
GameConfig.saveCharacter("  RAVEN  ");
String loaded = GameConfig.loadCharacter();
assertEquals("raven", loaded); // 정규화됨
```

### 3. Null 입력
```java
GameConfig.saveCharacter(null);
String loaded = GameConfig.loadCharacter();
assertNull(loaded); // 또는 기본값
```

### 4. 파일 없음
```java
File configFile = new File("game_config.properties");
configFile.delete();

String loaded = GameConfig.loadCharacter();
assertNull(loaded); // 기본값 반환
```

### 5. 파일 손상
```java
// game_config.properties에 잘못된 내용 작성
String loaded = GameConfig.loadCharacter();
// 예상: 예외 없이 기본값 반환
```

## 📈 사용 예시

### 캐릭터 선택 시
```java
// CharacterSelectDialog.java
okButton.addActionListener(e -> {
    String selectedId = getSelectedCharacterId();
    GameConfig.saveCharacter(selectedId); // 저장
    dispose();
});
```

### 게임 시작 시
```java
// LobbyFrame.java
public LobbyFrame(String playerName) {
    this.playerName = playerName;
    
    // 저장된 캐릭터 로드
    String savedChar = GameConfig.loadCharacter();
    if (savedChar != null && !savedChar.isEmpty()) {
        selectedCharacterId = savedChar;
    }
}
```

## 🎓 학습 포인트

### 초보자를 위한 핵심 개념
1. **Properties 클래스**: Java 설정 파일 관리
2. **Try-with-resources**: 자동 리소스 해제
3. **Static 메서드**: 인스턴스 없이 사용

### 중급자를 위한 심화 개념
1. **유틸리티 클래스 설계**: private 생성자 패턴
2. **예외 처리 전략**: 게임 계속 실행 vs 크래시
3. **파일 시스템 추상화**: OS별 경로 처리

## 🔍 코드 품질 평가

| 항목 | 평가 | 설명 |
|------|------|------|
| **가독성** | ⭐⭐⭐⭐⭐ | 매우 명확한 코드 |
| **유지보수성** | ⭐⭐⭐⭐ | 간단한 구조 |
| **확장성** | ⭐⭐⭐⭐ | 추가 설정 쉽게 추가 가능 |
| **성능** | ⭐⭐⭐⭐ | 빈번한 호출 없어 충분 |
| **안정성** | ⭐⭐⭐ | 예외 처리 있으나 개선 여지 |

## 📝 종합 평가

### 강점
✅ **단순 명쾌**: 2개 메서드로 모든 기능 제공  
✅ **안전한 예외 처리**: 파일 오류 시에도 게임 정상 동작  
✅ **기존 설정 보존**: 확장성 고려한 설계  
✅ **입력 정규화**: 데이터 일관성 유지  

### 개선 제안 우선순위
1. **Private 생성자 추가** (높음) - 인스턴스화 방지
2. **빈 문자열 검증** (높음) - 잘못된 데이터 저장 방지
3. **설정 파일 경로 개선** (중간) - 사용자별 디렉토리
4. **동시성 제어** (낮음) - 멀티스레드 환경 대비
5. **캐싱 추가** (선택) - 성능 최적화 필요 시

### 결론
**실용적이고 견고한 설정 관리 클래스**입니다. 현재 프로젝트 규모에 적합하며, 코드 품질도 우수합니다. 주요 개선사항(private 생성자, 입력 검증)은 10줄 이내로 간단히 적용 가능합니다.

**권장사항**: 
1. 즉시 적용: Private 생성자, 빈 문자열 검증
2. 추후 적용: 사용자별 설정 디렉토리 (베타 테스트 피드백 후)
3. 선택 적용: 캐싱, 동시성 제어 (성능 문제 발생 시)

**보안 고려사항**: 
- 현재는 캐릭터 선택만 저장하므로 보안 이슈 없음
- 향후 비밀번호/토큰 저장 시 암호화 필요
