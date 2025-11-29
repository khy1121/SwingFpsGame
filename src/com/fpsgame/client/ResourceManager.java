package com.fpsgame.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class ResourceManager {
    private static ResourceManager instance;
    private Map<String, BufferedImage> images = new HashMap<>();
    private Map<String, BufferedImage[]> spriteSheets = new HashMap<>();

    private ResourceManager() {
    }

    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }

    public BufferedImage getImage(String path) {
        if (images.containsKey(path)) {
            return images.get(path);
        }
        try {
            BufferedImage img = ImageIO.read(new File(path));
            images.put(path, img);
            return img;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public BufferedImage[] getSpriteSheet(String path, int frameWidth, int frameHeight) {
        String key = path + "_" + frameWidth + "_" + frameHeight;
        if (spriteSheets.containsKey(key)) {
            return spriteSheets.get(key);
        }

        BufferedImage sheet = getImage(path);
        if (sheet == null)
            return null;

        int cols = sheet.getWidth() / frameWidth;
        int rows = sheet.getHeight() / frameHeight;
        BufferedImage[] sprites = new BufferedImage[cols * rows];

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                sprites[y * cols + x] = sheet.getSubimage(x * frameWidth, y * frameHeight, frameWidth, frameHeight);
            }
        }

        spriteSheets.put(key, sprites);
        return sprites;
    }
}
