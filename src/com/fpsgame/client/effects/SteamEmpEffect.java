package com.fpsgame.client.effects;

import java.awt.*;

public class SteamEmpEffect extends SkillEffect {

    public SteamEmpEffect(float duration) {
        super("steam_emp", duration);
    }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        // 푸른 전자기 파동
        float progress = 1.0f - (remaining / duration);
        int radius = (int) (20 + progress * 150);

        g2d.setColor(new Color(0, 200, 255, (int) (200 * (remaining / duration))));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        // 번개 효과 (랜덤 라인)
        g2d.setColor(Color.CYAN);
        if (Math.random() > 0.5) {
            g2d.drawLine(x, y, x + (int) (Math.random() * 40 - 20), y + (int) (Math.random() * 40 - 20));
        }
    }
}
