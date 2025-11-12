package com.fpsgame.client.effects;

import java.awt.*;

/** Bulldog 전술 "엄폐" 효과: 보호막 느낌의 반투명 청록 육중 링 */
public class BulldogCoverEffect extends SkillEffect {
    public BulldogCoverEffect(float duration) { super("bull_cover", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float ratio = remaining / duration;
        int r = 42;
        int alpha = (int)(160 * ratio); alpha = Math.max(70, alpha);
        g2d.setStroke(new BasicStroke(5f));
        g2d.setColor(new Color(80, 200, 190, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
    }
}