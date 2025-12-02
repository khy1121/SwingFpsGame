package com.fpsgame.client.effects;

import java.awt.*;

/**
 * Sage 궁극 "부활": 밝은 청록/흰색 듀얼 링
 * 현재 게임에서는 사용 불가.
 */
public class SageReviveEffect extends SkillEffect {
    public SageReviveEffect(float duration) { super("sage_revive", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float e = (duration - remaining);
        int r = 36 + (int)(Math.sin(e * 6) * 5);
        int alpha = (int)(170 * (remaining / duration)); alpha = Math.max(70, alpha);
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(new Color(120, 255, 230, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        g2d.setColor(new Color(230, 255, 255, alpha/2));
        g2d.drawOval(x - r - 8, y - r - 8, (r + 8) * 2, (r + 8) * 2);
    }
}