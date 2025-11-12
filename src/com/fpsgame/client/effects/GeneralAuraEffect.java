package com.fpsgame.client.effects;

import java.awt.*;

/**
 * General 전술 스킬 "지휘 오라" 시각 효과
 * - 아군 버프를 상징하는 파란+황금 듀얼 링이 부드럽게 맥동
 * - 단순 오라 표현: 중앙에서 외곽으로 번지는 투명도 차등 링
 */
public class GeneralAuraEffect extends SkillEffect {
    public GeneralAuraEffect(float duration) { super("gen_aura", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float ratio = remaining / duration;
        int baseR = 34;
        int pulse = (int)(Math.sin((duration - remaining) * 4) * 5);
        int r = baseR + pulse;
        int alpha = (int)(150 * ratio); alpha = Math.max(50, alpha);
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(new Color(70, 140, 255, alpha));
        g2d.drawOval(x - r, y - r, r * 2, r * 2);
        g2d.setColor(new Color(255, 210, 90, alpha / 2));
        g2d.drawOval(x - r - 8, y - r - 8, (r + 8) * 2, (r + 8) * 2);
    }
}