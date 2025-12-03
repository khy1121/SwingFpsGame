# GameConstants.java & Protocol.java 코드 리뷰

## 📋 파일 개요

### GameConstants.java
- **경로**: `src/com/fpsgame/common/GameConstants.java`
- **목적**: 게임 전반의 공통 상수 정의
- **라인 수**: 62줄
- **패턴**: 유틸리티 클래스

### Protocol.java  
- **경로**: `src/com/fpsgame/common/Protocol.java`
- **목적**: 네트워크 프로토콜 및 메시지 타입 정의
- **라인 수**: 120줄
- **패턴**: 프로토콜 정의 클래스

---

# GameConstants.java 리뷰

## 🎯 주요 기능

### 1. 서버 설정
```java
public static final int DEFAULT_PORT = 7777;
public static final int MAX_PLAYERS = 4;
```

### 2. 게임 화면 설정
```java
public static final int GAME_WIDTH = 1280;
public static final int GAME_HEIGHT = 720;
```

### 3. 플레이어 설정
```java
public static final int PLAYER_SIZE = 40;
public static final int PLAYER_SPEED = 5;
public static final int MAX_HP = 100;
```

### 4. 미사일 설정
```java
public static final int MISSILE_SPEED = 10;
public static final int MISSILE_SIZE = 8;
public static final int MISSILE_DAMAGE = 20;
```

### 5. 팀 구분
```java
public static final int TEAM_RED = 0;
public static final int TEAM_BLUE = 1;
```

## ✅ 장점

### 1. **final 클래스 + private 생성자**
```java
public final class GameConstants {
    private GameConstants() {
        throw new AssertionError("Cannot instantiate GameConstants");
    }
}
```
**효과**:
- 인스턴스 생성 완전 차단
- 상속 불가 (`final`)
- 유틸리티 클래스 의도 명확

### 2. **카테고리별 구분**
```java
// ===== 서버 설정 =====
// ===== 게임 화면 =====
// ===== 플레이어 설정 =====
```
- 가독성 우수
- 관련 상수 그룹화

### 3. **명확한 이름**
```java
DEFAULT_PORT (O)  vs  PORT (X)
MAX_PLAYERS (O)   vs  MAX (X)
PLAYER_SIZE (O)   vs  SIZE (X)
```

### 4. **적절한 타입 선택**
```java
public static final int DEFAULT_PORT = 7777;  // int (포트 번호)
public static final int MAX_HP = 100;         // int (체력)
```

## ⚠️ 개선 가능 영역

### 1. **매직 넘버 여전히 존재**
**현재 코드:**
```java
public static final int TEAM_RED = 0;
public static final int TEAM_BLUE = 1;
```

**문제점**:
- 다른 코드에서 0, 1 직접 사용 가능
- 컴파일러가 검증 못 함

**개선 제안: Enum 사용**
```java
public enum Team {
    RED(0, "빨강 팀"),
    BLUE(1, "파랑 팀");
    
    private final int id;
    private final String displayName;
    
    Team(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    public int getId() { return id; }
    public String getDisplayName() { return displayName; }
}

// 사용
if (player.getTeam() == Team.RED) { ... }
```

**장점**:
- 타입 안전성
- switch 문에서 누락 케이스 경고
- 추가 메타데이터 (색상, 아이콘 등)

### 2. **값 간 관계 표현 부족**
**현재 코드:**
```java
public static final int GAME_WIDTH = 1280;
public static final int GAME_HEIGHT = 720;
```

**개선 제안:**
```java
public static final int GAME_WIDTH = 1280;
public static final int GAME_HEIGHT = 720;
public static final float ASPECT_RATIO = (float) GAME_WIDTH / GAME_HEIGHT;
public static final int GAME_CENTER_X = GAME_WIDTH / 2;
public static final int GAME_CENTER_Y = GAME_HEIGHT / 2;
```

### 3. **설정 변경 불가**
**현재 코드:**
```java
public static final int GAME_WIDTH = 1280; // 고정
```

**문제점**:
- 다른 해상도 지원 불가
- 테스트 시 값 변경 어려움

**개선 제안: 설정 파일**
```java
public final class GameConstants {
    // 기본값
    public static int GAME_WIDTH = 1280;
    public static int GAME_HEIGHT = 720;
    
    static {
        loadFromConfig();
    }
    
    private static void loadFromConfig() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("game.properties")) {
            props.load(fis);
            GAME_WIDTH = Integer.parseInt(props.getProperty("width", "1280"));
            GAME_HEIGHT = Integer.parseInt(props.getProperty("height", "720"));
        } catch (IOException e) {
            // 기본값 사용
        }
    }
}
```

### 4. **JavaDoc 부족**
**현재 코드:**
```java
/** 기본 서버 포트 번호 */
public static final int DEFAULT_PORT = 7777;
```

**개선 제안:**
```java
/**
 * 기본 서버 포트 번호
 * 
 * <p>클라이언트와 서버 간 TCP 연결에 사용됩니다.
 * 7777번 포트는 일반적으로 게임 서버에서 사용되며,
 * 방화벽에서 허용해야 합니다.
 * 
 * @see GameServer#start(int)
 */
public static final int DEFAULT_PORT = 7777;
```

### 5. **단위 명시 부족**
**현재 코드:**
```java
public static final int PLAYER_SPEED = 5; // 5 뭐?
public static final int PLAYER_SIZE = 40;  // 40 뭐?
```

**개선 제안:**
```java
/** 플레이어 이동 속도 (픽셀/프레임) */
public static final int PLAYER_SPEED_PX_PER_FRAME = 5;

/** 플레이어 히트박스 크기 (픽셀, 정사각형) */
public static final int PLAYER_HITBOX_SIZE_PX = 40;

/** 플레이어 렌더링 크기 (픽셀, 정사각형) */
public static final int PLAYER_RENDER_SIZE_PX = 64;
```

## 📊 사용 통계

### 참조 빈도 예상
| 상수 | 예상 사용 횟수 | 위치 |
|------|----------------|------|
| GAME_WIDTH/HEIGHT | 100+ | 렌더링, 충돌, 카메라 |
| PLAYER_SIZE | 50+ | 렌더링, 충돌 감지 |
| TEAM_RED/BLUE | 30+ | 팀 로직, UI |
| DEFAULT_PORT | 2 | 클라이언트, 서버 시작 |

---

# Protocol.java 리뷰

## 🎯 주요 기능

### 1. 메시지 타입 정의
```java
public static final byte CHAT = 1;
public static final byte WELCOME = 2;
public static final byte PLAYER_UPDATE = 3;
public static final byte PLAYER_SHOOT = 4;
public static final byte GAME_STATE = 5;
public static final byte CHARACTER_SELECT = 6;
```

### 2. 메시지 클래스
```java
public static class Message {
    public byte type;
    public String content;
}
```

### 3. 플레이어 정보 클래스
```java
public static class PlayerInfo {
    public int id;
    public String name;
    public float x, y;
    public float angle;
    public int hp;
    public int team;
    public String characterId;
    public int kills;
    public int deaths;
}
```

## ✅ 장점

### 1. **byte 타입 메시지 ID**
```java
public static final byte CHAT = 1;
```
**효과**:
- 1바이트만 사용 (vs int 4바이트)
- 네트워크 대역폭 절약
- 최대 127개 메시지 타입 지원 (충분)

### 2. **내부 클래스 사용**
```java
public static class Message { ... }
public static class PlayerInfo { ... }
```
- 네임스페이스 오염 방지
- 논리적 그룹화

### 3. **간단한 프로토콜**
```java
public static class Message {
    public byte type;
    public String content;
}
```
- 복잡한 직렬화 불필요
- 빠른 프로토타이핑

### 4. **final 클래스**
```java
public final class Protocol { ... }
```
- 상속 방지
- 프로토콜 수정 방지

## ⚠️ 개선 가능 영역

### 1. **public 필드**
**현재 코드:**
```java
public static class Message {
    public byte type;
    public String content;
}

// 외부에서 직접 수정 가능
Message msg = new Message(CHAT, "Hello");
msg.type = WELCOME; // 😱 의미 변경됨
```

**개선 제안:**
```java
public static class Message {
    private final byte type;
    private final String content;
    
    public Message(byte type, String content) {
        this.type = type;
        this.content = content;
    }
    
    public byte getType() { return type; }
    public String getContent() { return content; }
}
```

### 2. **타입 검증 부족**
**현재 코드:**
```java
Message msg = new Message((byte) 99, "Invalid"); // 정의되지 않은 타입
```

**개선 제안:**
```java
private static final Set<Byte> VALID_TYPES = Set.of(
    CHAT, WELCOME, PLAYER_UPDATE, PLAYER_SHOOT, GAME_STATE, CHARACTER_SELECT
);

public Message(byte type, String content) {
    if (!VALID_TYPES.contains(type)) {
        throw new IllegalArgumentException("Invalid message type: " + type);
    }
    this.type = type;
    this.content = content;
}
```

### 3. **PlayerInfo 불변성 부족**
**현재 코드:**
```java
PlayerInfo player = new PlayerInfo(1, "Alice");
player.hp = -100; // 😱
player.team = 99;  // 😱
```

**개선 제안:**
```java
public static class PlayerInfo {
    private final int id;
    private final String name;
    private float x, y;
    private float angle;
    private int hp;
    private int team;
    // ...
    
    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    
    // Setters with validation
    public void setHp(int hp) {
        if (hp < 0) throw new IllegalArgumentException("HP cannot be negative");
        this.hp = hp;
    }
    
    public void setTeam(int team) {
        if (team != 0 && team != 1) {
            throw new IllegalArgumentException("Invalid team: " + team);
        }
        this.team = team;
    }
}
```

### 4. **직렬화 미지원**
**현재 코드:**
```java
// 네트워크로 전송 방법?
Message msg = new Message(CHAT, "Hello");
// ??? → byte[]
```

**개선 제안 1: JSON (간단)**
```java
public String toJson() {
    return String.format("{\"type\":%d,\"content\":\"%s\"}", type, content);
}

public static Message fromJson(String json) {
    // JSON 파싱
}
```

**개선 제안 2: 바이너리 (효율적)**
```java
public byte[] toBytes() {
    ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + content.length());
    buffer.put(type);
    buffer.putInt(content.length());
    buffer.put(content.getBytes(StandardCharsets.UTF_8));
    return buffer.array();
}

public static Message fromBytes(byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);
    byte type = buffer.get();
    int length = buffer.getInt();
    byte[] contentBytes = new byte[length];
    buffer.get(contentBytes);
    return new Message(type, new String(contentBytes, StandardCharsets.UTF_8));
}
```

### 5. **메시지 타입 문서화 부족**
**현재 코드:**
```java
/** 채팅 메시지 */
public static final byte CHAT = 1;
```

**개선 제안:**
```java
/**
 * 채팅 메시지
 * 
 * <p>플레이어 간 텍스트 메시지 전송에 사용됩니다.
 * 
 * <h3>구조</h3>
 * <pre>
 * type: CHAT (1)
 * content: "[발신자]: 메시지 내용"
 * </pre>
 * 
 * <h3>예시</h3>
 * <pre>
 * Message msg = new Message(Protocol.CHAT, "Player1: Hello!");
 * </pre>
 * 
 * @see Message
 */
public static final byte CHAT = 1;
```

### 6. **Enum 대신 상수 사용**
**현재 코드:**
```java
public static final byte CHAT = 1;
public static final byte WELCOME = 2;
// ...
```

**개선 제안:**
```java
public enum MessageType {
    CHAT(1),
    WELCOME(2),
    PLAYER_UPDATE(3),
    PLAYER_SHOOT(4),
    GAME_STATE(5),
    CHARACTER_SELECT(6);
    
    private final byte id;
    
    MessageType(int id) {
        this.id = (byte) id;
    }
    
    public byte getId() { return id; }
    
    public static MessageType fromId(byte id) {
        for (MessageType type : values()) {
            if (type.id == id) return type;
        }
        throw new IllegalArgumentException("Unknown message type: " + id);
    }
}

// 사용
Message msg = new Message(MessageType.CHAT, "Hello");
```

## 📊 프로토콜 분석

### 메시지 타입별 빈도 (예상)
| 타입 | 예상 빈도 | 대역폭 |
|------|-----------|--------|
| PLAYER_UPDATE | 60/초 | 높음 |
| PLAYER_SHOOT | 10/초 | 중간 |
| CHAT | 0.1/초 | 낮음 |
| GAME_STATE | 1/초 | 높음 |
| WELCOME | 1회 | 낮음 |
| CHARACTER_SELECT | 1회 | 낮음 |

### 네트워크 오버헤드
```
기본 메시지: 1 byte (타입) + 4 bytes (문자열 길이) + N bytes (내용)
최소: 5 bytes
평균: 50 bytes (채팅 메시지)
최대: 1KB+ (게임 상태)
```

## 🧪 테스트 시나리오

### GameConstants 테스트
```java
@Test
public void testConstants() {
    assertEquals(7777, GameConstants.DEFAULT_PORT);
    assertEquals(4, GameConstants.MAX_PLAYERS);
    assertTrue(GameConstants.TEAM_RED < GameConstants.TEAM_BLUE);
}

@Test
public void testCannotInstantiate() {
    assertThrows(AssertionError.class, () -> {
        GameConstants.class.getDeclaredConstructor().newInstance();
    });
}
```

### Protocol 테스트
```java
@Test
public void testMessageCreation() {
    Message msg = new Message(Protocol.CHAT, "Test");
    assertEquals(Protocol.CHAT, msg.type);
    assertEquals("Test", msg.content);
}

@Test
public void testPlayerInfoDefaults() {
    PlayerInfo player = new PlayerInfo(1, "Alice");
    assertEquals(100, player.hp);
    assertEquals(0, player.kills);
    assertEquals(0, player.deaths);
    assertNull(player.characterId);
}
```

## 📈 사용 예시

### GameConstants 사용
```java
// 서버 시작
ServerSocket server = new ServerSocket(GameConstants.DEFAULT_PORT);

// 플레이어 렌더링
g.fillRect(
    player.x, 
    player.y, 
    GameConstants.PLAYER_SIZE, 
    GameConstants.PLAYER_SIZE
);

// 팀 확인
if (player.team == GameConstants.TEAM_RED) {
    g.setColor(Color.RED);
}
```

### Protocol 사용
```java
// 클라이언트: 채팅 전송
Message chatMsg = new Message(Protocol.CHAT, "[Player1]: Hello!");
out.writeByte(chatMsg.type);
out.writeUTF(chatMsg.content);

// 서버: 메시지 수신
byte type = in.readByte();
String content = in.readUTF();

switch (type) {
    case Protocol.CHAT:
        broadcast(new Message(Protocol.CHAT, content));
        break;
    case Protocol.PLAYER_UPDATE:
        updatePlayer(content);
        break;
}
```

## 🔍 코드 품질 평가

| 항목 | GameConstants | Protocol |
|------|---------------|----------|
| **가독성** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **유지보수성** | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **확장성** | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **안정성** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **성능** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

## 📝 종합 평가

### GameConstants 강점
✅ **완벽한 유틸리티 클래스**: final + private 생성자  
✅ **명확한 카테고리**: 주석으로 구분  
✅ **적절한 타입**: int 사용  

### GameConstants 약점
❌ **Enum 미사용**: TEAM_RED/BLUE  
❌ **관계 표현 부족**: 파생 상수 없음  
❌ **설정 파일 미지원**: 하드코딩  

### Protocol 강점
✅ **간단한 구조**: 빠른 구현  
✅ **byte 타입**: 대역폭 절약  
✅ **내부 클래스**: 논리적 그룹화  

### Protocol 약점
❌ **public 필드**: 불변성 부족  
❌ **검증 부족**: 잘못된 값 허용  
❌ **직렬화 미지원**: 수동 구현 필요  

### 개선 제안 우선순위

**GameConstants**:
1. Team Enum 생성 (높음)
2. 파생 상수 추가 (중간)
3. 설정 파일 지원 (낮음)

**Protocol**:
1. private 필드 + getter/setter (높음)
2. 직렬화 메서드 추가 (높음)
3. MessageType Enum (중간)
4. 타입 검증 (중간)

### 결론
두 클래스 모두 **기본 기능은 완벽**합니다. GameConstants는 즉시 사용 가능하며, Protocol은 직렬화만 추가하면 프로덕션 레벨입니다.

**권장사항**:
1. **GameConstants**: Team Enum 생성
2. **Protocol**: 직렬화 메서드, private 필드
3. **장기**: 설정 파일, 프로토콜 버전 관리

**확장 아이디어**:
- 난이도별 상수 세트
- 프로토콜 버전 협상
- 메시지 압축
- 암호화 지원
