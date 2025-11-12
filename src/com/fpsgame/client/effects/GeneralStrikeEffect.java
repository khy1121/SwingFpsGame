package com.fpsgame.client.effects;

import java.awt.*;

/** General 궁극기 "공습" 간단 시각 효과: 붉은 목표 링 + 회전 사격 아크 */
public class GeneralStrikeEffect extends SkillEffect {
    public GeneralStrikeEffect(float duration) { super("gen_strike", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float elapsed = (duration - remaining);
        int r = 40 + (int)(Math.sin(elapsed * 3) * 6);
        int alpha = (int)(180 * (remaining / duration)); alpha = Math.max(60, alpha);
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(new Color(255, 80, 80, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        int arcStart = (int)((elapsed * 250) % 360);
        g2d.setColor(new Color(255, 140, 100, alpha));
        g2d.drawArc(x - r, y - r, r * 2, r * 2, arcStart, 60);
    }
}