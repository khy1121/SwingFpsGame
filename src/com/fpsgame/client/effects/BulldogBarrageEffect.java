package com.fpsgame.client.effects;

import java.awt.*;

public class BulldogBarrageEffect extends SkillEffect {

    public BulldogBarrageEffect(float duration) {
        super("bull_barrage", duration);
    }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        // 주변으로 퍼지는 폭발 효과
        float progress = 1.0f - (remaining / duration);
        int radius = (int) (50 + progress * 100);

        g2d.setColor(new Color(255, 100, 0, (int) (255 * (remaining / duration))));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        // 내부 파편
        g2d.setColor(Color.ORANGE);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            int px = x + (int) (Math.cos(angle) * radius);
            int py = y + (int) (Math.sin(angle) * radius);
            g2d.fillRect(px - 2, py - 2, 4, 4);
        }
    }
}
