package com.fpsgame.client.effects;

import java.awt.*;

/**
 * Piper 전술 스킬 "적 표시" 시각 이펙트
 * - 하늘색(시안) 듀얼 파동 링을 통해 시야 확장/정보 제공 능력을 직관적으로 표현
 * - 내부 로직:
 *   progress = 경과 비율(0~1), sin 파형을 이용해 약간의 숨쉬기(pulse) 효과
 *   remaining/duration 비율로 alpha 계산 → 시간이 갈수록 서서히 사라짐
 * - 두 개의 링: 기본 링 + 외곽 흐린 링 (탐지 영역이 넓어진 느낌 강조)
 * - 성능: 단순 원/선 그리기만 사용(저비용) → 다수 플레이어 동시에 활성화되어도 부담 적음
 */
public class PiperMarkEffect extends SkillEffect {
    public PiperMarkEffect(float duration) { super("piper_mark", duration); }

    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        // 진행도(0~1) 경과 시간 기반 파형 반영
        float progress = 1f - (remaining / duration);
        // 살짝 커졌다 줄어드는 펄스 반경 (sin(2π progress) * 4)
        int radius = 28 + (int)(Math.sin(progress * 6.28318) * 4);
        // 남은 시간 비율로 투명도 계산 (최소/최대 범위 클램핑)
        int alpha = (int)(160 * (remaining / duration));
        alpha = Math.max(40, Math.min(200, alpha));

        g2d.setStroke(new BasicStroke(3f));
        // 기본 파동 링
        g2d.setColor(new Color(100, 220, 255, alpha));
        g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        // 외곽 희미한 확장 링 (정보 범위 확장 느낌)
        g2d.setColor(new Color(80, 200, 255, alpha / 2));
        g2d.drawOval(x - radius - 10, y - radius - 10, (radius + 10) * 2, (radius + 10) * 2);
    }
}
