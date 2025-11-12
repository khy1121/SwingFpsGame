package com.fpsgame.client.effects;

import java.awt.*;

/** Technician 전술 "지뢰": 바닥에 깔리는 얕은 빨강 경고 링 */
public class TechMineEffect extends SkillEffect {
    public TechMineEffect(float duration) { super("tech_mine", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        int r = 24;
        int alpha = (int)(150 * (remaining / duration)); alpha = Math.max(50, alpha);
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(new Color(255, 90, 90, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
    }
}
