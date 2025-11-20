package com.fpsgame.client;

import java.awt.event.KeyEvent;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 키 바인딩 설정 관리 클래스
 * 사용자가 설정한 키를 저장/로드하고 게임에 적용
 */
public class KeyBindingConfig {

    private static final String CONFIG_FILE = "keybindings.properties";
    private static Map<String, Integer> keyBindings = new HashMap<>();

    // 키 액션 상수
    public static final String KEY_MOVE_FORWARD = "이동_앞";
    public static final String KEY_MOVE_BACKWARD = "이동_뒤";
    public static final String KEY_MOVE_LEFT = "이동_왼쪽";
    public static final String KEY_MOVE_RIGHT = "이동_오른쪽";
    public static final String KEY_TACTICAL_SKILL = "전술스킬";
    public static final String KEY_ULTIMATE_SKILL = "궁극기";
    public static final String KEY_CHARACTER_SELECT = "캐릭터선택";
    public static final String KEY_MINIMAP_TOGGLE = "미니맵";

    static {
        loadDefaultBindings();
        loadBindings();
    }

    /**
     * 기본 키 바인딩 설정
     */
    private static void loadDefaultBindings() {
        keyBindings.put(KEY_MOVE_FORWARD, KeyEvent.VK_W);
        keyBindings.put(KEY_MOVE_BACKWARD, KeyEvent.VK_S);
        keyBindings.put(KEY_MOVE_LEFT, KeyEvent.VK_A);
        keyBindings.put(KEY_MOVE_RIGHT, KeyEvent.VK_D);
        keyBindings.put(KEY_TACTICAL_SKILL, KeyEvent.VK_E);
        keyBindings.put(KEY_ULTIMATE_SKILL, KeyEvent.VK_R);
        keyBindings.put(KEY_CHARACTER_SELECT, KeyEvent.VK_B);
        keyBindings.put(KEY_MINIMAP_TOGGLE, KeyEvent.VK_M);
    }

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

            for (String key : keyBindings.keySet()) {
                String value = props.getProperty(key);
                if (value != null) {
                    try {
                        keyBindings.put(key, Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid key binding for " + key + ": " + value);
                    }
                }
            }

            System.out.println("[KeyBindingConfig] 키 바인딩 로드 완료");
        } catch (IOException e) {
            System.err.println("[KeyBindingConfig] 키 바인딩 로드 실패: " + e.getMessage());
        }
    }

    /**
     * 키 바인딩을 파일에 저장
     */
    public static void saveBindings() {
        Properties props = new Properties();

        for (Map.Entry<String, Integer> entry : keyBindings.entrySet()) {
            props.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
        }

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "FPS Game Key Bindings Configuration");
            System.out.println("[KeyBindingConfig] 키 바인딩 저장 완료");
        } catch (IOException e) {
            System.err.println("[KeyBindingConfig] 키 바인딩 저장 실패: " + e.getMessage());
        }
    }

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
     * 모든 키 바인딩 가져오기
     */
    public static Map<String, Integer> getAllBindings() {
        return new HashMap<>(keyBindings);
    }

    /**
     * 키 바인딩 초기화 (기본값으로)
     */
    public static void resetToDefaults() {
        loadDefaultBindings();
        saveBindings();
    }

    /**
     * 특정 키 코드가 특정 액션에 바인딩되어 있는지 확인
     */
    public static boolean isKeyPressed(int keyCode, String action) {
        return keyCode == getKey(action);
    }
}
