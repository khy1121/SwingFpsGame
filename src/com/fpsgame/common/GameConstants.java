package com.fpsgame.common;

/**
 * 게임 상수 정의
 */
public class GameConstants {
    
    // 서버 설정
    public static final int DEFAULT_PORT = 7777;
    public static final int MAX_PLAYERS = 4; // 간단하게 4명으로 제한
    
    // 게임 화면
    public static final int GAME_WIDTH = 800;
    public static final int GAME_HEIGHT = 600;
    
    // 플레이어 설정
    public static final int PLAYER_SIZE = 40;
    public static final int PLAYER_SPEED = 5;
    public static final int MAX_HP = 100;
    
    // 미사일 설정
    public static final int MISSILE_SPEED = 10;
    public static final int MISSILE_SIZE = 8;
    public static final int MISSILE_DAMAGE = 20;
    
    // 팀
    public static final int TEAM_RED = 0;
    public static final int TEAM_BLUE = 1;
}
