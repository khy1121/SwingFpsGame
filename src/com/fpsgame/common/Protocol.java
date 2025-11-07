package com.fpsgame.common;

/**
 * 간단한 네트워크 프로토콜
 * - 복잡한 프로토콜 대신 기본적인 메시지만 지원
 */
public class Protocol {
    
    // 메시지 타입
    public static final byte CHAT = 1;
    public static final byte WELCOME = 2;
    public static final byte PLAYER_UPDATE = 3;
    public static final byte PLAYER_SHOOT = 4;
    public static final byte GAME_STATE = 5;
    public static final byte CHARACTER_SELECT = 6;
    
    // 간단한 메시지 클래스
    public static class Message {
        public byte type;
        public String content;
        
        public Message(byte type, String content) {
            this.type = type;
            this.content = content;
        }
    }
    
    // 플레이어 정보
    public static class PlayerInfo {
        public int id;
        public String name;
        public float x, y;
        public float angle;
        public int hp;
        public int team; // 0=RED, 1=BLUE
        public String characterId; // 선택한 캐릭터 ID
        public int kills;
        public int deaths;
        
        public PlayerInfo(int id, String name) {
            this.id = id;
            this.name = name;
            this.hp = 100;
            this.kills = 0;
            this.deaths = 0;
            this.characterId = null; // 기본값 제거: 플레이어가 직접 선택해야 함
        }
    }
}
