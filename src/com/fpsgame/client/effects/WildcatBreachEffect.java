package com.fpsgame.client.effects;

import java.awt.*;

/** Wildcat 전술 "돌파": 전방 방향 잔상 삼각 (단순) */
public class WildcatBreachEffect extends SkillEffect {
    public WildcatBreachEffect(float duration) { super("wild_breach", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        int alpha = (int)(200 * (remaining / duration)); alpha = Math.max(50, alpha);
        int len = 36;
        Polygon p = new Polygon(new int[]{x, x - 14, x + 14}, new int[]{y - len, y, y}, 3);
        g2d.setColor(new Color(255, 120, 80, alpha));
        g2d.drawPolygon(p);
    }
}