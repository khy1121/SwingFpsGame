package com.fpsgame.client.effects;

import java.awt.*;

/** Technician 궁극 "터렛": 청록 보호 링 + 느린 회전 아크 */
public class TechTurretEffect extends SkillEffect {
    public TechTurretEffect(float duration) { super("tech_turret", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float e = (duration - remaining);
        int r = 34;
        int alpha = (int)(160 * (remaining / duration)); alpha = Math.max(60, alpha);
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(new Color(90, 230, 210, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        int arcStart = (int)((e * 180) % 360);
        g2d.drawArc(x - r, y - r, r * 2, r * 2, arcStart, 70);
    }
}
