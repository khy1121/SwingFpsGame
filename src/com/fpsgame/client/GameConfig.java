package com.fpsgame.client;

import java.io.*;
import java.util.Properties;

/**
 * 게임 설정 관리 클래스
 * 
 * 게임의 설정(선택한 캐릭터 등)을 파일에 저장하고 로드합니다.
 * Properties 파일 형식을 사용하여 키=값 형태로 저장됩니다.
 */
public class GameConfig {
    /** 설정 파일 경로 */
    private static final String CONFIG_FILE = "game_config.properties";
    
    /** 캐릭터 설정 키 */
    private static final String KEY_CHARACTER = "selected_character";
    
    /** 캐릭터 기본값 (null = 미선택 상태) */
    private static final String DEFAULT_CHARACTER = null;

    /**
     * 선택한 캐릭터를 설정 파일에 저장
     * 
     * 기존 설정을 유지하면서 캐릭터 정보만 업데이트합니다.
     * 저장 실패 시 스택 트레이스를 출력하지만 프로그램은 계속 실행됩니다.
     * 
     * @param characterId 저장할 캐릭터 ID (예: "raven", "piper")
     */
    public static void saveCharacter(String characterId) {
        Properties props = new Properties();
        
        // 기존 설정 로드 (다른 설정을 덮어쓰지 않기 위함)
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
        } catch (IOException e) {
            // 파일이 없으면 새로 생성되므로 무시
        }

        // 캐릭터 ID 정규화 (공백 제거, 소문자 변환)
        String normalized = characterId != null ? characterId.trim().toLowerCase() : "";
        props.setProperty(KEY_CHARACTER, normalized);

        // 설정 파일에 저장
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Game Configuration");
        } catch (IOException e) {
            System.err.println("[Config] Failed to save character: " + characterId);
            e.printStackTrace(System.err);
        }
    }

    /**
     * 저장된 캐릭터 정보를 로드
     * 
     * 설정 파일에서 선택한 캐릭터 ID를 읽어옵니다.
     * 파일이 없거나 읽기 실패 시 기본값(null)을 반환합니다.
     * 
     * @return 저장된 캐릭터 ID, 없으면 null
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
}
