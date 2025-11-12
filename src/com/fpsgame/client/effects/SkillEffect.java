package com.fpsgame.client.effects;

import java.awt.*;

/**
 * 스킬/버프/디스플레이용 시각 효과 추상 기본 클래스
 *
 * 설계 목적:
 *  - 단순한 "수명(lifecycle)" 관리: duration(총 지속), remaining(남은 시간)
 *  - 프레임 루프에서 update(dt)로 남은 시간을 감소시키고 isExpired() 판단
 *  - drawSelf / drawForPlayer 이중 인터페이스: 로컬/원격 플레이어 시각 차별화 가능
 *
 * 확장 지침:
 *  - 생성자에서 id(논리 식별자)를 명확히 지정 (네트워크/디버그 추적 용이)
 *  - drawSelf에서 알파(투명도)를 remaining/duration 기반으로 계산하여 자연스러운 페이드아웃 구현 권장
 *  - 복잡한 애니메이션이 필요할 경우 (예: 회전, 펄스) 경과 시간은 (duration - remaining) 값을 이용
 *  - 쓰레드 안전성: GamePanel 렌더/업데이트 스레드 단일 접근 가정 → 별도 동기화 불필요
 */
public abstract class SkillEffect {
    protected final String id;          // 효과 고유 ID (스킬 ID 등)
    protected final float duration;     // 전체 지속시간(초)
    protected float remaining;          // 남은 시간(초)

    protected SkillEffect(String id, float duration) {
        this.id = id;
        this.duration = Math.max(0.05f, duration); // 최소 시간 보장 (너무 짧은 값 보호)
        this.remaining = this.duration;
    }

    public boolean isExpired() { return remaining <= 0f; }

    /**
     * 프레임 단위 업데이트
     * @param dt 경과 시간(초)
     */
    public void update(float dt) { remaining -= dt; }

    /**
     * 로컬 플레이어 기준 그리기 (화면 좌표)
     */
    public abstract void drawSelf(Graphics2D g2d, int x, int y);

    /**
     * 원격 플레이어 기준 그리기 (기본적으로 동일 처리, 필요 시 오버라이드)
     */
    public void drawForPlayer(Graphics2D g2d, int x, int y) {
        drawSelf(g2d, x, y);
    }

    public String getId() { return id; }
    public float getRemaining() { return Math.max(0, remaining); }
    public float getDuration() { return duration; }
}
