package com.fpsgame.client.effects;

import java.awt.Graphics2D;
import java.util.*;

/**
 * SkillEffectManager
 * - 로컬 플레이어용 이펙트(selfEffects)와 원격 플레이어별 이펙트(byPlayer)를 분리 관리
 * - update(dt): 모든 등록된 이펙트의 수명 감소 및 만료 제거
 * - drawSelf / drawForPlayer: GamePanel 렌더 단계에서 호출
 *
 * 사용 패턴:
 *   skillEffects.addSelf(new PiperMarkEffect(...));
 *   skillEffects.addForPlayer("상대이름", new RavenDashEffect(...));
 *
 * 장점:
 *  - 기존 단순 리스트보다 캐릭터/스킬 별 클래스로 세분화 → 유지보수 & 시각적 확장 용이
 *  - 네트워크 처리와 분리되어 시각 효과만 집중 관리
 */
public class SkillEffectManager {
    private final List<SkillEffect> selfEffects = new ArrayList<>();
    private final Map<String, List<SkillEffect>> byPlayer = new HashMap<>();

    /** 로컬 플레이어 효과 등록 */
    public void addSelf(SkillEffect fx) { if (fx != null) selfEffects.add(fx); }

    /** 특정 원격 플레이어 효과 등록 */
    public void addForPlayer(String player, SkillEffect fx) {
        if (player == null || fx == null) return;
        byPlayer.computeIfAbsent(player, k -> new ArrayList<>()).add(fx);
    }

    /** 프레임 업데이트: 모든 이펙트 수명 감소 & 만료 제거 */
    public void update(float dt) {
        if (!selfEffects.isEmpty()) {
            for (Iterator<SkillEffect> it = selfEffects.iterator(); it.hasNext();) {
                SkillEffect fx = it.next();
                fx.update(dt);
                if (fx.isExpired()) it.remove();
            }
        }
        if (!byPlayer.isEmpty()) {
            for (List<SkillEffect> list : byPlayer.values()) {
                for (Iterator<SkillEffect> it = list.iterator(); it.hasNext();) {
                    SkillEffect fx = it.next();
                    fx.update(dt);
                    if (fx.isExpired()) it.remove();
                }
            }
        }
    }

    /** 로컬 이펙트 그리기 */
    public void drawSelf(Graphics2D g2d, int x, int y) {
        for (SkillEffect fx : selfEffects) fx.drawSelf(g2d, x, y);
    }

    /** 특정 플레이어 이펙트 그리기 */
    public void drawForPlayer(String player, Graphics2D g2d, int x, int y) {
        List<SkillEffect> list = byPlayer.get(player);
        if (list == null || list.isEmpty()) return;
        for (SkillEffect fx : list) fx.drawForPlayer(g2d, x, y);
    }
}
