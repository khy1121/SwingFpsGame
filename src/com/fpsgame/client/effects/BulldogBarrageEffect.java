package com.fpsgame.client.effects;

import java.awt.*;

/** Bulldog 궁극 "폭발탄 난사": 두꺼운 주황/빨강 박동 링 */
public class BulldogBarrageEffect extends SkillEffect {
    public BulldogBarrageEffect(float duration) { super("bull_barrage", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float elapsed = (duration - remaining);
        int r = 45 + (int)(Math.sin(elapsed * 5) * 8);
        int alpha = (int)(170 * (remaining / duration)); alpha = Math.max(60, alpha);
        g2d.setStroke(new BasicStroke(6f));
        g2d.setColor(new Color(255, 140, 50, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
    }
}