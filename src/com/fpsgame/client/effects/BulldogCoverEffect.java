package com.fpsgame.client.effects;

import java.awt.*;

/**
 * Bulldog 캐릭터 이펙트
 * 현재 게임에서는 사용 불가.
 */
public class BulldogCoverEffect extends SkillEffect {

    public BulldogCoverEffect(float duration) {
        super("bull_cover", duration);
    }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        // 방패 모양의 오라
        g2d.setColor(new Color(100, 100, 255, 100));
        g2d.setStroke(new BasicStroke(3f));
        g2d.drawArc(x - 30, y - 30, 60, 60, 0, 360);

        g2d.setColor(new Color(50, 50, 200, 50));
        g2d.fillArc(x - 30, y - 30, 60, 60, 0, 360);

        // 텍스트
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("SHIELD", x - 20, y - 35);
    }
}
