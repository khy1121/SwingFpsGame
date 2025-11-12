package com.fpsgame.client.effects;

import java.awt.*;

/** Steam 궁극 "전술 리셋": 순간 위치 변경 강조하는 희미한 파란 회전 아크 */
public class SteamResetEffect extends SkillEffect {
    public SteamResetEffect(float duration) { super("steam_reset", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float e = (duration - remaining);
        int r = 30;
        int alpha = (int)(150 * (remaining / duration)); alpha = Math.max(50, alpha);
        int arcStart = (int)((e * 300) % 360);
        g2d.setStroke(new BasicStroke(4f));
        g2d.setColor(new Color(120, 200, 255, alpha));
        g2d.drawArc(x - r, y - r, r * 2, r * 2, arcStart, 90);
    }
}