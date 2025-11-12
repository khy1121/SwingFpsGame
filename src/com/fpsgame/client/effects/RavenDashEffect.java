package com.fpsgame.client.effects;

import java.awt.*;

/**
 * Raven 대쉬 이펙트 (화면 연출 전용)
 * - SkillEffect 추상 클래스를 상속하여 수명(remaining/duration)에 따라
 *   링 형태의 파형을 그립니다.
 * - 대쉬의 속도감과 잔상을 단순한 원형 파동으로 표현합니다.
 *
 * 사용 위치(예시):
 * - Raven 전술 스킬(대쉬) 사용 시 GamePanel에서 등록하여 매 프레임 그리기
 */
public class RavenDashEffect extends SkillEffect {
    /**
     * @param duration 대쉬 시각 효과 유지 시간(초)
     */
    public RavenDashEffect(float duration) { super("raven_dash", duration); }

    /**
     * 자기 자신(로컬 플레이어) 기준 그리기
     * @param g2d 그리기 컨텍스트
     * @param x   화면 좌표 X (플레이어 중심 기준)
     * @param y   화면 좌표 Y
     */
    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        // 진행도(0~1): 시간이 지날수록 1에 근접
        float progress = 1f - (remaining / duration);
        int baseR = 30; // 기본 반경
        int radius = baseR + (int)(progress * 15); // 진행도에 따라 반경 증가
        // 남은 수명 비율로 알파(투명도) 계산 -> 시간이 지날수록 더 투명
        int alpha = (int)(180 * (remaining / duration));
        alpha = Math.max(40, alpha);

        // 바깥 링: 청록색 계열의 라인을 굵게 그려 속도감 표현
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(new Color(80, 190, 255, alpha));
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);

        // 내부 펄스: 반투명한 안쪽 링으로 다이내믹한 느낌 추가
        g2d.setColor(new Color(120, 220, 255, alpha/2));
        g2d.drawOval(x - radius/2, y - radius/2, radius, radius);
    }
}
