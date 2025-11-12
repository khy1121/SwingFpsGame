package com.fpsgame.client.effects;

import java.awt.*;

/**
 * Piper 궁극기 "열감지" 시각 이펙트
 * - 강렬한 주황색 글로우 + 빠르게 회전하는 아크(arc)로 전장 전체 감지 상태를 강조
 * - lifeRatio(남은시간/전체시간)를 사용해 지속 종료 시 부드럽게 페이드아웃
 * - pulse: (경과시간 * 주파수) 기반 sin 파형으로 크기 변화 → 살아있는 센서 느낌
 * - arcStart: 경과 시간 * 300도 속도 회전 (빠른 스캔 느낌), 80도 길이의 굵은 아크
 */
public class PiperThermalEffect extends SkillEffect {
    public PiperThermalEffect(float duration) { super("piper_thermal", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float lifeRatio = remaining / duration; // 남은 비율(투명도 계산용)
        int baseR = 34; // 기본 반경
        int pulse = (int)(Math.sin((duration - remaining) * 5) * 6); // 시간 경과에 따른 펄스 진폭
        int radius = baseR + pulse;
        int alpha = (int)(160 * lifeRatio);
        alpha = Math.max(70, alpha);

        g2d.setStroke(new BasicStroke(4f));
        // 기본 글로우 링
        g2d.setColor(new Color(255, 170, 60, alpha));
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        // 회전 아크 (빠른 스캔 UI 느낌)
        int arcStart = (int)(((duration - remaining) * 300) % 360);
        g2d.setColor(new Color(255, 210, 100, alpha));
        g2d.drawArc(x - radius, y - radius, radius * 2, radius * 2, arcStart, 80);
    }
}
