package com.fpsgame.client.effects;

import java.awt.*;

/**
 * 총구 섬광 효과 (아주 짧은 시간 유지)
 * SkillEffect 추상 클래스를 상속하여 GamePanel에서 공통 관리.
 * 각도(angle) 방향으로 작은 원뿔/삼각형 형태의 광채를 그려 순간 발사 임팩트 강조.
 */
public class MuzzleFlashEffect extends SkillEffect {
    private final double angle; // 라디안 방향

    /**
     * 기본 지속 0.12초 섬광 생성
     * @param angleRad 발사 방향(라디안)
     */
    public MuzzleFlashEffect(double angleRad) {
        this(angleRad, 0.12f);
    }

    /**
     * 사용자 지정 지속 시간 섬광
     * @param angleRad 방향
     * @param duration 지속(초)
     */
    public MuzzleFlashEffect(double angleRad, float duration) {
        super("muzzle_flash", duration);
        this.angle = angleRad;
    }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        int alpha = (int)(200 * (getRemaining() / getDuration()));
        alpha = Math.max(60, alpha);
        int len = 26;       // 섬광 길이
        int halfWidth = 8;  // 섬광 반폭
        double cx = Math.cos(angle), cy = Math.sin(angle); // 진행 방향 벡터
        double nx = -cy, ny = cx; // 법선 벡터 (좌우 퍼짐)
        int x1 = x + (int)(cx * len);
        int y1 = y + (int)(cy * len);
        int x2 = x + (int)(nx * halfWidth);
        int y2 = y + (int)(ny * halfWidth);
        int x3 = x - (int)(nx * halfWidth);
        int y3 = y - (int)(ny * halfWidth);
        Polygon cone = new Polygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
        g2d.setColor(new Color(255, 230, 80, alpha));
        g2d.fillPolygon(cone);
        g2d.setColor(new Color(255, 200, 60, alpha));
        g2d.drawPolygon(cone);
    }
}
