package com.fpsgame.client.effects;

import java.awt.*;

/**
 * Ghost 궁극 "열감지 무효": 열 차단 상징 보라 링 + 회전 작은 아크
 * 현재 게임에서는 사용 불가.
 */
public class GhostNullifyEffect extends SkillEffect {
    public GhostNullifyEffect(float duration) { super("ghost_nullify", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float elapsed = (duration - remaining);
        int r = 34;
        int alpha = (int)(160 * (remaining / duration)); alpha = Math.max(60, alpha);
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(new Color(170, 120, 255, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        int arcStart = (int)((elapsed * 220) % 360);
        g2d.setColor(new Color(120, 70, 200, alpha));
        g2d.drawArc(x - r, y - r, r * 2, r * 2, arcStart, 50);
    }
}