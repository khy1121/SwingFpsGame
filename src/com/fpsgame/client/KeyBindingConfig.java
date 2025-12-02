package com.fpsgame.client;

import java.awt.event.KeyEvent;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 키 바인딩 설정 관리 클래스
 * 
 * 사용자가 설정한 키를 저장/로드하고 게임에 적용합니다.
 * Properties 파일로 저장되며, 설정 화면에서 키를 변경할 수 있습니다.
 */
public class KeyBindingConfig {

    /** 키 바인딩 설정 파일 경로 */
    private static final String CONFIG_FILE = "keybindings.properties";
    
    /** 키 바인딩 맵 (액션 이름 -> 키 코드) */
    private static final Map<String, Integer> keyBindings = new HashMap<>();

    // 키 액션 상수
    /** 앞으로 이동 키 */
    public static final String KEY_MOVE_FORWARD = "이동_앞";
    
    /** 뒤로 이동 키 */
    public static final String KEY_MOVE_BACKWARD = "이동_뒤";
    
    /** 왼쪽 이동 키 */
    public static final String KEY_MOVE_LEFT = "이동_왼쪽";
    
    /** 오른쪽 이동 키 */
    public static final String KEY_MOVE_RIGHT = "이동_오른쪽";
    
    /** 전술 스킬 키 */
    public static final String KEY_TACTICAL_SKILL = "전술스킬";
    
    /** 궁극기 키 */
    public static final String KEY_ULTIMATE_SKILL = "궁극기";
    
    /** 캐릭터 선택 화면 토글 키 */
    public static final String KEY_CHARACTER_SELECT = "캐릭터선택";
    
    /** 미니맵 토글 키 */
    public static final String KEY_MINIMAP_TOGGLE = "미니맵";

    static {
        // 클래스 로드 시 기본 바인딩 설정 후 파일에서 로드
        loadDefaultBindings();
        loadBindings();
    }

    /**
     * 기본 키 바인딩 설정
     * 
     * 게임의 기본 키 설정을 정의합니다.
     * 파일이 없거나 손상되었을 때 이 설정이 사용됩니다.
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
     * 
     * 설정 파일에서 사용자가 지정한 키 바인딩을 읽어옵니다.
     * 파일이 없으면 기본 설정을 사용합니다.
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

    /**
     * 키 바인딩을 파일에 저장
     * 
     * 현재 설정된 모든 키 바인딩을 파일에 저장합니다.
     * 설정 화면에서 키를 변경한 후 호출됩니다.
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

    /**
     * 특정 액션의 키 코드 가져오기
     * 
     * @param action 액션 이름 (예: KEY_MOVE_FORWARD)
     * @return 해당 액션에 바인딩된 키 코드, 없으면 VK_UNDEFINED
     */
    public static int getKey(String action) {
        return keyBindings.getOrDefault(action, KeyEvent.VK_UNDEFINED);
    }

    /**
     * 특정 액션의 키 설정
     * 
     * 설정 화면에서 키를 변경할 때 사용됩니다.
     * 
     * @param action 액션 이름
     * @param keyCode 새로 할당할 키 코드
     */
    public static void setKey(String action, int keyCode) {
        keyBindings.put(action, keyCode);
    }

    /**
     * 모든 키 바인딩 가져오기
     * 
     * 설정 화면에서 현재 키 설정을 표시할 때 사용됩니다.
     * 
     * @return 키 바인딩 맵의 복사본 (원본 보호)
     */
    public static Map<String, Integer> getAllBindings() {
        return new HashMap<>(keyBindings);
    }

    /**
     * 키 바인딩 초기화 (기본값으로)
     * 
     * 모든 키를 기본 설정으로 되돌리고 파일에 저장합니다.
     */
    public static void resetToDefaults() {
        loadDefaultBindings();
        saveBindings();
    }

    /**
     * 특정 키 코드가 특정 액션에 바인딩되어 있는지 확인
     * 
     * @param keyCode 확인할 키 코드
     * @param action 확인할 액션 이름
     * @return 해당 키가 해당 액션에 바인딩되어 있으면 true
     */
    public static boolean isKeyPressed(int keyCode, String action) {
        return keyCode == getKey(action);
    }
}
