package com.fpsgame.common;

/**
 * 게임 상수 정의
 * 
 * 게임 전반에서 사용되는 공통 상수값을 정의합니다.
 * 
 * 포함 항목:
 * - 서버 설정 (포트, 최대 플레이어 수)
 * - 게임 화면 크기
 * - 플레이어 기본 스탯
 * - 미사일(발사체) 설정
 * - 팀 구분
 */
public final class GameConstants {

    /**
     * 인스턴스 생성 방지
     */
    private GameConstants() {
        throw new AssertionError("Cannot instantiate GameConstants");
    }

    // ===== 서버 설정 =====
    
    /** 기본 서버 포트 번호 */
    public static final int DEFAULT_PORT = 7777;
    
    /** 최대 동시 접속 플레이어 수 */
    public static final int MAX_PLAYERS = 4;

    // ===== 게임 화면 =====
    
    /** 게임 화면 너비 (픽셀) */
    public static final int GAME_WIDTH = 1280;
    
    /** 게임 화면 높이 (픽셀) */
    public static final int GAME_HEIGHT = 720;

    // ===== 플레이어 설정 =====
    
    /** 플레이어 크기 (픽셀, 정사각형) */
    public static final int PLAYER_SIZE = 40;
    
    /** 플레이어 기본 이동 속도 */
    public static final int PLAYER_SPEED = 5;
    
    /** 플레이어 최대 체력 */
    public static final int MAX_HP = 100;

    // ===== 미사일 설정 =====
    
    /** 미사일 비행 속도 */
    public static final int MISSILE_SPEED = 10;
    
    /** 미사일 크기 (픽셀, 정사각형) */
    public static final int MISSILE_SIZE = 8;
    
    /** 미사일 기본 데미지 */
    public static final int MISSILE_DAMAGE = 20;

    // ===== 팀 구분 =====
    
    /** RED 팀 (빨강 팀) */
    public static final int TEAM_RED = 0;
    
    /** BLUE 팀 (파랑 팀) */
    public static final int TEAM_BLUE = 1;
}
