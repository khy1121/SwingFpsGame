package com.fpsgame.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * 리소스 관리 싱글턴 클래스
 * 
 * 게임에서 사용하는 이미지와 스프라이트 시트를 캐싱하여 관리합니다.
 * 한 번 로드된 이미지는 메모리에 저장되어 재사용됩니다.
 */
public class ResourceManager {
    /** 싱글턴 인스턴스 */
    private static ResourceManager instance;
    
    /** 이미지 캐시 (경로 -> 이미지) */
    private final Map<String, BufferedImage> images = new HashMap<>();
    
    /** 스프라이트 시트 캐시 (키 -> 스프라이트 배열) */
    private final Map<String, BufferedImage[]> spriteSheets = new HashMap<>();

    /**
     * private 생성자 (싱글턴 패턴)
     */
    private ResourceManager() {
    }

    /**
     * 싱글턴 인스턴스 가져오기
     * 
     * @return ResourceManager 인스턴스
     */
    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }

    /**
     * 이미지 로드 (캐싱 지원)
     * 
     * 지정된 경로에서 이미지를 로드합니다.
     * 이미 로드된 이미지는 캐시에서 반환됩니다.
     * 
     * @param path 이미지 파일 경로
     * @return 로드된 이미지, 실패 시 null
     */
    public BufferedImage getImage(String path) {
        // 캐시에 있으면 바로 반환
        if (images.containsKey(path)) {
            return images.get(path);
        }
        
        // 파일에서 이미지 로드
        try {
            BufferedImage img = ImageIO.read(new File(path));
            images.put(path, img);
            return img;
        } catch (IOException e) {
            System.err.println("[ResourceManager] Failed to load image: " + path);
            e.printStackTrace(System.err);
            return null;
        }
    }

    /**
     * 스프라이트 시트를 개별 프레임으로 분할 (캐싱 지원)
     * 
     * 큰 스프라이트 시트 이미지를 지정된 크기의 프레임들로 잘라냅니다.
     * 캐릭터 애니메이션, 이펙트 등에 사용됩니다.
     * 
     * @param path 스프라이트 시트 이미지 경로
     * @param frameWidth 각 프레임의 너비 (픽셀)
     * @param frameHeight 각 프레임의 높이 (픽셀)
     * @return 분할된 프레임 배열, 실패 시 null
     */
    public BufferedImage[] getSpriteSheet(String path, int frameWidth, int frameHeight) {
        // 캐시 키 생성 (경로_너비_높이)
        String key = path + "_" + frameWidth + "_" + frameHeight;
        
        // 캐시에 있으면 바로 반환
        if (spriteSheets.containsKey(key)) {
            return spriteSheets.get(key);
        }

        // 스프라이트 시트 이미지 로드
        BufferedImage sheet = getImage(path);
        if (sheet == null)
            return null;

        // 시트를 그리드로 분할
        int cols = sheet.getWidth() / frameWidth;
        int rows = sheet.getHeight() / frameHeight;
        BufferedImage[] sprites = new BufferedImage[cols * rows];

        // 각 셀을 개별 이미지로 추출
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                sprites[y * cols + x] = sheet.getSubimage(
                    x * frameWidth, 
                    y * frameHeight, 
                    frameWidth, 
                    frameHeight
                );
            }
        }

        // 캐시에 저장하고 반환
        spriteSheets.put(key, sprites);
        return sprites;
    }
}
