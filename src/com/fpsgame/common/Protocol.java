package com.fpsgame.common;

/**
 * 네트워크 프로토콜
 * 
 * 클라이언트와 서버 간 통신에 사용되는 프로토콜을 정의합니다.
 * 
 * 메시지 타입:
 * - CHAT: 채팅 메시지
 * - WELCOME: 서버 접속 환영 메시지
 * - PLAYER_UPDATE: 플레이어 상태 업데이트
 * - PLAYER_SHOOT: 플레이어 발사
 * - GAME_STATE: 게임 상태 정보
 * - CHARACTER_SELECT: 캐릭터 선택
 */
public final class Protocol {
    
    /**
     * 인스턴스 생성 방지
     */
    private Protocol() {
        throw new AssertionError("Cannot instantiate Protocol");
    }
    
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
    
    /**
     * 플레이어 정보 클래스
     * 
     * 게임 내 플레이어의 상태 정보를 저장합니다.
     * 위치, 체력, 팀, 캐릭터, 킬/데스 등을 포함합니다.
     */
    public static class PlayerInfo {
        /** 플레이어 고유 ID */
        public int id;
        
        /** 플레이어 이름 */
        public String name;
        
        /** X 좌표 */
        public float x;
        
        /** Y 좌표 */
        public float y;
        
        /** 바라보는 각도 (라디안) */
        public float angle;
        
        /** 현재 체력 */
        public int hp;
        
        /** 팀 (0=RED, 1=BLUE) */
        public int team;
        
        /** 선택한 캐릭터 ID */
        public String characterId;
        
        /** 킬 수 */
        public int kills;
        
        /** 데스 수 */
        public int deaths;
        
        /**
         * 플레이어 정보 생성자
         * 
         * @param id 플레이어 고유 ID
         * @param name 플레이어 이름
         */
        public PlayerInfo(int id, String name) {
            this.id = id;
            this.name = name;
            this.hp = 100;
            this.kills = 0;
            this.deaths = 0;
            this.characterId = null; // 플레이어가 직접 선택해야 함
        }
    }
}
