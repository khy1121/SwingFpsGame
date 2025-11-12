package com.fpsgame.client.effects;

import java.awt.*;

/** Skull 궁극 "탄약 보급": 황금 링 + 빠른 톱니형 회전(단순 호) */
public class SkullAmmoEffect extends SkillEffect {
    public SkullAmmoEffect(float duration) { super("skull_ammo", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float e = (duration - remaining);
        int r = 36;
        int alpha = (int)(170 * (remaining / duration)); alpha = Math.max(60, alpha);
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(new Color(255, 205, 90, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        int arcStart = (int)((e * 260) % 360);
        g2d.drawArc(x - r, y - r, r * 2, r * 2, arcStart, 70);
    }
}