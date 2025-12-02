package com.fpsgame.client;

import java.io.*;
import java.util.Properties;

public class GameConfig {
    private static final String CONFIG_FILE = "game_config.properties";
    private static final String KEY_CHARACTER = "selected_character";
    private static final String DEFAULT_CHARACTER = null;

    public static void saveCharacter(String characterId) {
        Properties props = new Properties();
        // Load existing props first to not overwrite other settings
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
        } catch (IOException e) {
            // File might not exist yet, ignore
        }

        String normalized = characterId != null ? characterId.trim().toLowerCase() : "";
        props.setProperty(KEY_CHARACTER, normalized);

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Game Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String loadCharacter() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            String value = props.getProperty(KEY_CHARACTER);
            if (value != null) {
                value = value.trim().toLowerCase();
                if (!value.isEmpty()) {
                    return value;
                }
            }
            return DEFAULT_CHARACTER;
        } catch (IOException e) {
            return DEFAULT_CHARACTER;
        }
    }
}
