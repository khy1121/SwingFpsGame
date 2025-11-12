package com.fpsgame.client.effects;

import java.awt.*;

/** Wildcat 궁극 "광폭화": 붉은 이중 펄스 링 */
public class WildcatBerserkEffect extends SkillEffect {
    public WildcatBerserkEffect(float duration) { super("wild_berserk", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float e = (duration - remaining);
        int r = 34 + (int)(Math.sin(e * 7) * 6);
        int alpha = (int)(180 * (remaining / duration)); alpha = Math.max(70, alpha);
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(new Color(255, 70, 70, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        g2d.setColor(new Color(180, 30, 30, alpha/2));
        g2d.drawOval(x - r - 8, y - r - 8, (r + 8) * 2, (r + 8) * 2);
    }
}