# GameConstants & Protocol 코드 리뷰

## 파일 정보
- **파일명**: `GameConstants.java` (74줄), `Protocol.java` (124줄)
- **패키지**: `com.fpsgame.common`
- **총 라인 수**: 198줄
- **역할**: 게임 상수 정의 및 네트워크 프로토콜 정의

---

## 파일 개요

`GameConstants`와 `Protocol`은 **게임 전반에서 사용되는 공통 정의**를 담은 유틸리티 클래스입니다. 모두 `final` 클래스로 선언되어 **인스턴스 생성을 방지**하며, 모든 멤버가 `public static final`로 선언되어 있습니다.

### GameConstants
게임의 **물리적 상수**와 **설정값**을 정의:
- 서버 설정 (포트, 최대 플레이어)
- 게임 화면 크기
- 플레이어 스탯 (크기, 속도, 체력)
- 미사일 설정 (속도, 크기, 데미지)
- 팀 구분 (RED=0, BLUE=1)

### Protocol
**네트워크 통신 프로토콜**과 **데이터 구조**를 정의:
- 메시지 타입 (CHAT, WELCOME, PLAYER_UPDATE 등)
- Message 클래스 (타입 + 내용)
- PlayerInfo 클래스 (플레이어 상태 정보)

---

## GameConstants 상세 분석

### 1. 클래스 구조

```java
public final class GameConstants {

    /**
     * 인스턴스 생성 방지
     */
    private GameConstants() {
        throw new AssertionError("Cannot instantiate GameConstants");
    }
    
    // ... 상수 정의 ...
}
```

**특징**:
- **`final` 클래스**: 상속 불가
- **private 생성자**: 인스턴스 생성 방지
- **AssertionError**: 리플렉션을 통한 생성 시도도 차단

### 2. 서버 설정

```java
// ===== 서버 설정 =====

/** 기본 서버 포트 번호 */
public static final int DEFAULT_PORT = 7777;

/** 최대 동시 접속 플레이어 수 */
public static final int MAX_PLAYERS = 4;
```

**특징**:
- **DEFAULT_PORT = 7777**: 기본 포트 (변경 가능하나 기본값 제공)
- **MAX_PLAYERS = 4**: 로비 시스템에서는 팀당 5명(총 10명) 가능하나, 초기 설정은 4명

### 3. 게임 화면

```java
// ===== 게임 화면 =====

/** 게임 화면 너비 (픽셀) */
public static final int GAME_WIDTH = 1280;

/** 게임 화면 높이 (픽셀) */
public static final int GAME_HEIGHT = 720;
```

**특징**:
- **1280x720 해상도**: 16:9 비율, HD 해상도
- **고정 해상도**: `GameRenderer`에서 스케일링 처리

### 4. 플레이어 설정

```java
// ===== 플레이어 설정 =====

/** 플레이어 크기 (픽셀, 정사각형) */
public static final int PLAYER_SIZE = 40;

/** 플레이어 기본 이동 속도 */
public static final int PLAYER_SPEED = 5;

/** 플레이어 최대 체력 */
public static final int MAX_HP = 100;
```

**특징**:
- **PLAYER_SIZE = 40**: 40x40 정사각형 충돌 박스
- **PLAYER_SPEED = 5**: 기본 이동 속도 (캐릭터별로 다를 수 있음)
- **MAX_HP = 100**: 기본 체력 (캐릭터별로 다를 수 있음)

### 5. 미사일 설정

```java
// ===== 미사일 설정 =====

/** 미사일 비행 속도 */
public static final int MISSILE_SPEED = 10;

/** 미사일 크기 (픽셀, 정사각형) */
public static final int MISSILE_SIZE = 8;

/** 미사일 기본 데미지 */
public static final int MISSILE_DAMAGE = 20;
```

**특징**:
- **MISSILE_SPEED = 10**: 플레이어 속도의 2배
- **MISSILE_SIZE = 8**: 8x8 정사각형
- **MISSILE_DAMAGE = 20**: 5발에 적 처치 (100 HP 기준)

### 6. 팀 구분

```java
// ===== 팀 구분 =====

/** RED 팀 (빨강 팀) */
public static final int TEAM_RED = 0;

/** BLUE 팀 (파랑 팀) */
public static final int TEAM_BLUE = 1;
```

**특징**:
- **정수 상수**: enum 대신 int 사용 (간단한 2팀 체계)
- **0과 1**: 배열 인덱스로 사용하기 편리

---

## Protocol 상세 분석

### 1. 클래스 구조

```java
public final class Protocol {
    
    /**
     * 인스턴스 생성 방지
     */
    private Protocol() {
        throw new AssertionError("Cannot instantiate Protocol");
    }
    
    // ... 프로토콜 정의 ...
}
```

**특징**:
- **`final` 클래스**: 상속 불가
- **private 생성자**: 인스턴스 생성 방지

### 2. 메시지 타입

```java
// ===== 메시지 타입 =====

/** 채팅 메시지 */
public static final byte CHAT = 1;

/** 서버 접속 환영 메시지 */
public static final byte WELCOME = 2;

/** 플레이어 위치/상태 업데이트 */
public static final byte PLAYER_UPDATE = 3;

/** 플레이어 발사 이벤트 */
public static final byte PLAYER_SHOOT = 4;

/** 게임 전체 상태 */
public static final byte GAME_STATE = 5;

/** 캐릭터 선택 */
public static final byte CHARACTER_SELECT = 6;
```

**특징**:
- **byte 타입**: 1바이트로 메시지 타입 표현 (네트워크 효율)
- **6가지 타입**: CHAT, WELCOME, PLAYER_UPDATE, PLAYER_SHOOT, GAME_STATE, CHARACTER_SELECT
- **주의**: 실제로는 더 많은 프로토콜이 있으나 여기서는 기본만 정의됨

### 3. Message 클래스

```java
/**
 * 메시지 클래스
 * 
 * 서버-클라이언트 간 전송되는 기본 메시지 구조입니다.
 */
public static class Message {
    /** 메시지 타입 (CHAT, WELCOME 등) */
    public byte type;
    
    /** 메시지 내용 */
    public String content;
    
    /**
     * 메시지 생성자
     * 
     * @param type 메시지 타입
     * @param content 메시지 내용
     */
    public Message(byte type, String content) {
        this.type = type;
        this.content = content;
    }
}
```

**특징**:
- **간단한 구조**: 타입 + 내용
- **public 필드**: getter/setter 없이 직접 접근
- **정적 내부 클래스**: `Protocol.Message` 형태로 사용

### 4. PlayerInfo 클래스

```java
/**
 * 플레이어 정보 클래스
 * 
 * 게임 내 플레이어의 상태 정보를 저장합니다.
 * 위치, 체력, 팀, 캐릭터, 킬/데스 등을 포함합니다.
 */
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
    
    public PlayerInfo(int id, String name) {
        this.id = id;
        this.name = name;
        this.hp = 100;
        this.kills = 0;
        this.deaths = 0;
        this.characterId = null; // 플레이어가 직접 선택해야 함
    }
}
```

**특징**:
- **11개 필드**: ID, 이름, 위치, 각도, 체력, 팀, 캐릭터, 킬/데스
- **public 필드**: 직접 접근 가능
- **기본값 초기화**: HP=100, 킬/데스=0, 캐릭터=null
- **float 좌표**: 부드러운 보간을 위해 float 사용

---

## 강점

### 1. **명확한 상수 관리**
- **중앙 집중식**: 모든 게임 상수를 한 곳에서 관리
- **매직 넘버 제거**: `5` 대신 `GameConstants.PLAYER_SPEED` 사용
- **유지보수 용이**: 포트 번호, 화면 크기 등 한 곳에서 변경

### 2. **인스턴스 생성 방지**
- **private 생성자**: new 연산자 차단
- **AssertionError**: 리플렉션 공격 방지
- **final 클래스**: 상속 차단

### 3. **Javadoc 문서화**
- **모든 상수에 주석**: 용도 명확히 설명
- **클래스 레벨 문서**: "게임 상수 정의", "네트워크 프로토콜" 등
- **생성자 문서**: "인스턴스 생성 방지"

### 4. **네임스페이스 분리**
- **GameConstants**: 게임 로직 상수
- **Protocol**: 네트워크 통신 상수
- **명확한 책임 분리**: 혼동 없음

### 5. **효율적인 타입 선택**
- **byte 타입**: 메시지 타입 (1바이트)
- **int 타입**: 게임 상수 (충분한 범위)
- **float 타입**: 좌표 (부드러운 보간)

---

## 개선 제안

### 1. **enum 사용 고려** (중요도: 중간)

현재는 `byte` 상수로 메시지 타입을 정의하지만, `enum`을 사용하면 타입 안전성이 향상됩니다.

```java
// 현재 코드
public static final byte CHAT = 1;
public static final byte WELCOME = 2;
// ... 사용 시
byte msgType = Protocol.CHAT;

// 개선 제안
public enum MessageType {
    CHAT(1),
    WELCOME(2),
    PLAYER_UPDATE(3),
    PLAYER_SHOOT(4),
    GAME_STATE(5),
    CHARACTER_SELECT(6);
    
    private final byte code;
    
    MessageType(int code) {
        this.code = (byte) code;
    }
    
    public byte getCode() {
        return code;
    }
    
    public static MessageType fromCode(byte code) {
        for (MessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type: " + code);
    }
}

// 사용 시
MessageType msgType = MessageType.CHAT;
byte code = msgType.getCode();
```

**장점**:
- **타입 안전**: 컴파일 타임에 오류 검출
- **가독성**: `MessageType.CHAT`이 `Protocol.CHAT`보다 명확
- **변환 메서드**: `fromCode()` 메서드로 역방향 변환

### 2. **팀도 enum으로** (중요도: 낮음)

2개 팀만 있지만, enum으로 표현하면 더 명확해집니다.

```java
// 현재 코드
public static final int TEAM_RED = 0;
public static final int TEAM_BLUE = 1;

// 개선 제안
public enum Team {
    RED(0, "RED", new Color(244, 67, 54)),
    BLUE(1, "BLUE", new Color(33, 150, 243));
    
    private final int id;
    private final String name;
    private final Color color;
    
    Team(int id, String name, Color color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }
    
    public int getId() { return id; }
    public String getName() { return name; }
    public Color getColor() { return color; }
    
    public static Team fromId(int id) {
        for (Team team : values()) {
            if (team.id == id) {
                return team;
            }
        }
        throw new IllegalArgumentException("Unknown team: " + id);
    }
}

// 사용 시
Team myTeam = Team.RED;
Color teamColor = myTeam.getColor();
```

**장점**:
- **색상 통합**: 팀 색상도 enum에 포함
- **확장 가능**: 3팀 이상 추가 시 쉬움
- **타입 안전**: int 대신 Team 타입 사용

### 3. **PlayerInfo 불변성 고려** (중요도: 중간)

현재는 모든 필드가 public으로 노출되어 있어 변경 가능합니다. Builder 패턴이나 setter를 사용하면 더 안전합니다.

```java
// 개선 제안 (Builder 패턴)
public static class PlayerInfo {
    private final int id;
    private final String name;
    private float x, y;
    private float angle;
    private int hp;
    private int team;
    private String characterId;
    private int kills;
    private int deaths;
    
    private PlayerInfo(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.x = builder.x;
        this.y = builder.y;
        this.hp = builder.hp;
        this.team = builder.team;
        this.characterId = builder.characterId;
        this.kills = builder.kills;
        this.deaths = builder.deaths;
    }
    
    // Getter 메서드들
    public int getId() { return id; }
    public String getName() { return name; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    // ... 나머지 getter/setter
    
    public static class Builder {
        private final int id;
        private final String name;
        private float x = 0, y = 0;
        private float angle = 0;
        private int hp = 100;
        private int team = 0;
        private String characterId = null;
        private int kills = 0;
        private int deaths = 0;
        
        public Builder(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public Builder position(float x, float y) {
            this.x = x;
            this.y = y;
            return this;
        }
        
        public Builder team(int team) {
            this.team = team;
            return this;
        }
        
        public Builder character(String characterId) {
            this.characterId = characterId;
            return this;
        }
        
        public PlayerInfo build() {
            return new PlayerInfo(this);
        }
    }
}

// 사용 시
PlayerInfo player = new PlayerInfo.Builder(1, "Player1")
    .position(100, 200)
    .team(Team.RED.getId())
    .character("raven")
    .build();
```

### 4. **프로토콜 확장성** (중요도: 낮음)

현재는 6개 메시지 타입만 정의되어 있으나, 실제로는 더 많은 프로토콜이 사용됩니다. 주석으로 명시하면 좋습니다.

```java
// ===== 메시지 타입 =====

/** 채팅 메시지 */
public static final byte CHAT = 1;

/** 서버 접속 환영 메시지 */
public static final byte WELCOME = 2;

/** 플레이어 위치/상태 업데이트 */
public static final byte PLAYER_UPDATE = 3;

/** 플레이어 발사 이벤트 */
public static final byte PLAYER_SHOOT = 4;

/** 게임 전체 상태 */
public static final byte GAME_STATE = 5;

/** 캐릭터 선택 */
public static final byte CHARACTER_SELECT = 6;

// 주의: 실제로는 더 많은 프로토콜이 있음
// - SKILL: 스킬 사용
// - HIT: 피격 이벤트
// - TEAM_ROSTER: 팀 구성 정보
// - ROUND_START: 라운드 시작
// - ROUND_END: 라운드 종료
// - PLACE_OBJECT: 오브젝트 설치
// 등등... (GameServer.java 참고)
```

### 5. **설정 파일 통합** (중요도: 낮음)

게임 밸런스 조정을 위해 설정 파일(JSON, Properties)로 관리하면 재컴파일 없이 수정 가능합니다.

```java
// game_constants.properties
server.port=7777
server.max_players=4
game.width=1280
game.height=720
player.size=40
player.speed=5
player.max_hp=100
missile.speed=10
missile.size=8
missile.damage=20

// 로드 코드
public final class GameConstants {
    public static final int DEFAULT_PORT;
    public static final int MAX_PLAYERS;
    // ... 나머지 상수
    
    static {
        Properties props = new Properties();
        try (InputStream in = GameConstants.class.getResourceAsStream("/game_constants.properties")) {
            props.load(in);
            DEFAULT_PORT = Integer.parseInt(props.getProperty("server.port", "7777"));
            MAX_PLAYERS = Integer.parseInt(props.getProperty("server.max_players", "4"));
            // ... 나머지 로드
        } catch (IOException e) {
            throw new RuntimeException("Failed to load game constants", e);
        }
    }
    
    private GameConstants() {
        throw new AssertionError("Cannot instantiate GameConstants");
    }
}
```

**장점**:
- **재컴파일 불필요**: 설정만 변경 후 재시작
- **밸런스 조정 용이**: 개발자가 아닌 기획자도 수정 가능
- **환경별 설정**: 개발/프로덕션 환경별 다른 설정 가능

### 6. **검증 메서드 추가** (중요도: 낮음)

상수 값이 유효한지 검증하는 메서드를 추가하면 좋습니다.

```java
public final class GameConstants {
    // ... 상수 정의 ...
    
    /**
     * 게임 상수 값이 유효한지 검증
     */
    public static void validate() {
        if (DEFAULT_PORT < 1024 || DEFAULT_PORT > 65535) {
            throw new IllegalStateException("Invalid port: " + DEFAULT_PORT);
        }
        if (MAX_PLAYERS < 1 || MAX_PLAYERS > 100) {
            throw new IllegalStateException("Invalid max players: " + MAX_PLAYERS);
        }
        if (GAME_WIDTH <= 0 || GAME_HEIGHT <= 0) {
            throw new IllegalStateException("Invalid game size");
        }
        // ... 나머지 검증
    }
}

// 메인 클래스에서 호출
public static void main(String[] args) {
    GameConstants.validate();
    Protocol.validate();
    // ... 게임 시작
}
```

---

## 코드 품질 평가

| 항목 | 점수 | 설명 |
|------|------|------|
| **가독성** | ⭐⭐⭐⭐⭐ (5.0/5.0) | 명확한 상수명, 충분한 주석, 논리적 그룹화 |
| **유지보수성** | ⭐⭐⭐⭐☆ (4.0/5.0) | 중앙 집중식 관리, enum 사용 시 더 좋음 |
| **확장성** | ⭐⭐⭐⭐☆ (4.0/5.0) | 새로운 상수 추가 쉬움, 설정 파일 통합 권장 |
| **타입 안전성** | ⭐⭐⭐☆☆ (3.0/5.0) | int/byte 상수 사용, enum으로 개선 가능 |
| **문서화** | ⭐⭐⭐⭐⭐ (5.0/5.0) | 모든 상수에 Javadoc, 클래스 레벨 문서 충실 |
| **일관성** | ⭐⭐⭐⭐⭐ (5.0/5.0) | 일관된 네이밍, 그룹화, 주석 스타일 |

### **종합 평가**: ⭐⭐⭐⭐☆ (4.3/5.0)

---

## 결론 및 요약

`GameConstants`와 `Protocol`은 **잘 설계된 상수 관리 클래스**로, 게임 전반에서 사용되는 공통 정의를 명확하게 제공합니다.

**주요 성과**:
- **중앙 집중식 관리**: 모든 게임 상수를 한 곳에서 관리
- **인스턴스 생성 방지**: private 생성자 + AssertionError
- **명확한 문서화**: 모든 상수에 Javadoc 주석
- **논리적 그룹화**: 서버, 화면, 플레이어, 미사일, 팀으로 분류
- **효율적인 타입 선택**: byte(메시지 타입), int(게임 상수), float(좌표)

**개선 영역** (선택 사항):
1. **enum 사용**: MessageType, Team enum으로 타입 안전성 향상
2. **설정 파일 통합**: Properties/JSON으로 재컴파일 없이 수정 가능
3. **PlayerInfo 불변성**: Builder 패턴 또는 getter/setter 사용
4. **검증 메서드**: 상수 값 유효성 검증

전반적으로 **매우 깔끔하고 명확한 코드**이며, enum 사용과 설정 파일 통합만 추가하면 더욱 강력한 상수 관리 시스템이 될 것입니다.
