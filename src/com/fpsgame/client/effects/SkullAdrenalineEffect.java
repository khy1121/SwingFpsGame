package com.fpsgame.client.effects;

import java.awt.*;

/** Skull 전술 "아드레날린": 녹색 회복 링 */
public class SkullAdrenalineEffect extends SkillEffect {
    public SkullAdrenalineEffect(float duration) { super("skull_adrenaline", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        int r = 30 + (int)(Math.sin((duration - remaining) * 6) * 4);
        int alpha = (int)(170 * (remaining / duration)); alpha = Math.max(50, alpha);
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(new Color(90, 220, 120, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
    }
}