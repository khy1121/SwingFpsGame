package com.fpsgame.client.effects;

import java.awt.*;

/** Sage 전술 "치료": 녹청색 힐 링 */
public class SageHealEffect extends SkillEffect {
    public SageHealEffect(float duration) { super("sage_heal", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        int r = 34;
        int alpha = (int)(160 * (remaining / duration)); alpha = Math.max(60, alpha);
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(new Color(90, 230, 200, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
    }
}