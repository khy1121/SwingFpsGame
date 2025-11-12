package com.fpsgame.client.effects;

import java.awt.*;

/** Steam 전술 "EMP": 전자 펄스 느낌 파란/하양 이중 링 */
public class SteamEmpEffect extends SkillEffect {
    public SteamEmpEffect(float duration) { super("steam_emp", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float e = (duration - remaining);
        int r = 32 + (int)(Math.sin(e * 8) * 4);
        int alpha = (int)(160 * (remaining / duration)); alpha = Math.max(60, alpha);
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(new Color(90, 180, 255, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        g2d.setColor(new Color(200, 230, 255, alpha/2));
        g2d.drawOval(x - r - 6, y - r - 6, (r + 6) * 2, (r + 6) * 2);
    }
}