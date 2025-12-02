package com.fpsgame.client.effects;

import java.awt.*;

/**
 * Steam 캐릭터 이펙트
 * 현재 게임에서는 사용 불가.
 */
public class SteamResetEffect extends SkillEffect {

    public SteamResetEffect(float duration) {
        super("steam_reset", duration);
    }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        // 녹색 회복/리셋 느낌
        g2d.setColor(new Color(0, 255, 100, 150));
        g2d.setStroke(new BasicStroke(3f));

        // 위로 올라가는 화살표들
        int offset = (int) ((duration - remaining) * 20) % 20;
        g2d.drawLine(x - 10, y + 10 - offset, x, y - offset);
        g2d.drawLine(x + 10, y + 10 - offset, x, y - offset);

        g2d.setColor(Color.GREEN);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("RESET", x - 20, y - 20);
    }
}
