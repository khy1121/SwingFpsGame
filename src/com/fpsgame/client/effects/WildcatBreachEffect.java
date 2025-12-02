package com.fpsgame.client.effects;

import java.awt.*;

/**
 * Wildcat 캐릭터 이펙트
 * 현재 게임에서는 사용 불가.
 */
public class WildcatBreachEffect extends SkillEffect {

    public WildcatBreachEffect(float duration) {
        super("wild_breach", duration);
    }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        // 돌진 이펙트 (잔상 느낌)
        g2d.setColor(new Color(200, 200, 200, 150));
        g2d.setStroke(new BasicStroke(2f));

        // 뒤로 흐르는 선
        g2d.drawLine(x - 20, y - 10, x - 40, y - 20);
        g2d.drawLine(x - 20, y + 10, x - 40, y + 20);
        g2d.drawLine(x - 25, y, x - 50, y);

        g2d.setColor(Color.WHITE);
        g2d.drawString(">>>", x - 10, y + 30);
    }
}
