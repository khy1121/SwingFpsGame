package com.fpsgame.client.effects;

import java.awt.*;

public class WildcatBerserkEffect extends SkillEffect {

    public WildcatBerserkEffect(float duration) {
        super("wild_berserk", duration);
    }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        // 붉은 오라
        g2d.setColor(new Color(255, 0, 0, 100));
        g2d.fillOval(x - 25, y - 25, 50, 50);

        // 눈 강조
        g2d.setColor(Color.RED);
        g2d.fillOval(x - 10, y - 5, 8, 8);
        g2d.fillOval(x + 2, y - 5, 8, 8);

        // 텍스트
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("RAGE", x - 20, y - 30);
    }
}
