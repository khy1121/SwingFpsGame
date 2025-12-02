package com.fpsgame.client.effects;

import java.awt.*;

/**
 * Ghost 전술 "투명화": 희미한 파랑/보라 반투명 외곽
 * 현재 게임에서는 사용 불가.
 */
public class GhostCloakEffect extends SkillEffect {
    public GhostCloakEffect(float duration) { super("ghost_cloak", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        int alpha = (int)(140 * (remaining / duration)); alpha = Math.max(40, alpha);
        int r = 30;
        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(new Color(110, 150, 255, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        g2d.setColor(new Color(160, 120, 255, alpha/2));
        g2d.drawOval(x - r - 6, y - r - 6, (r + 6) * 2, (r + 6) * 2);
    }
}