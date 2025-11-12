package com.fpsgame.client.effects;

import java.awt.*;

/**
 * Raven 과충전(Overcharge) 이펙트
 * - 공격 속도/투사체 가속 상태를 시각적으로 강렬하게 표현하기 위한 빨간/주황 펄스 링
 * - 회전하는 아크(arc)를 추가하여 에너지 충만감을 강조
 *
 * 표현 요소:
 * 1) 펄스 반경: sin 파형을 이용해 반경이 주기적으로 커졌다 작아짐
 * 2) 알파(투명도): 남은 시간 비율(lifeRatio)에 비례 (종료 근처에서 자연스럽게 사라짐)
 * 3) 회전 아크: 경과 시간 * 240도 속도로 원 둘레를 도는 70도 길이의 아크
 */
public class RavenOverchargeEffect extends SkillEffect {
    public RavenOverchargeEffect(float duration) { super("raven_overcharge", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        // lifeRatio: 남은 시간 비율 (0~1)
        float lifeRatio = remaining / duration;
        int baseR = 34; // 기본 반경
        // 경과 시간 기반 펄스 (진폭 5px)
        int pulse = (int)(Math.sin((duration - remaining) * 6) * 5);
        int radius = baseR + pulse;
        int alpha = (int)(170 * lifeRatio);
        alpha = Math.max(60, alpha);

        // 바깥 펄스 링
        g2d.setStroke(new BasicStroke(4f));
        g2d.setColor(new Color(255, 120, 80, alpha));
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        // 회전 아크 시작 각도 (경과 시간 * 240도 -> 빠른 회전)
        int arcStart = (int)(((duration - remaining) * 240) % 360);
        g2d.setColor(new Color(255, 200, 120, alpha));
        g2d.drawArc(x - radius, y - radius, radius * 2, radius * 2, arcStart, 70);
    }
}
