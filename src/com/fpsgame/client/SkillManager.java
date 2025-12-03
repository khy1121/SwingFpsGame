package com.fpsgame.client;

import com.fpsgame.common.Ability;
import com.fpsgame.client.effects.*;
import java.awt.Color;
import java.util.*;

/**
 * 스킬 시스템, 이펙트 관리, 쿨다운을 담당하는 클래스
 * GamePanel에서 분리된 SkillManager
 */
public class SkillManager {
    
    // 스킬 이펙트
    public static class ActiveEffect {
        String abilityId;
        String type; // BASIC, TACTICAL, ULTIMATE
        float duration;
        float remaining;
        Color color;

        ActiveEffect(String abilityId, String type, float duration) {
            this.abilityId = abilityId;
            this.type = type;
            this.duration = Math.max(0.1f, duration);
            this.remaining = this.duration;
            this.color = colorForType(type);
        }

        static Color colorForType(String type) {
            if ("BASIC".equalsIgnoreCase(type))
                return new Color(100, 200, 100);
            if ("ULTIMATE".equalsIgnoreCase(type))
                return new Color(255, 100, 100);
            return new Color(100, 150, 255); // TACTICAL default
        }
    }
    
    // 이펙트 관리
    private final Map<String, List<ActiveEffect>> effectsByPlayer = new HashMap<>();
    private final List<ActiveEffect> myEffects = new ArrayList<>();
    private final SkillEffectManager skillEffects = new SkillEffectManager();
    
    // 캐릭터별 런타임 상태
    private float ravenDashRemaining = 0f;
    private float ravenOverchargeRemaining = 0f;
    private float missileSpeedMultiplier = 1f;
    private float attackSpeedMultiplier = 1f; // TODO: 공격 속도 버프 미구현
    
    private float piperMarkRemaining = 0f;
    private float piperThermalRemaining = 0f;
    private float teamMarkRemaining = 0f;
    private float teamThermalRemaining = 0f;
    
    // 스킬 배열
    private Ability[] abilities;
    
    // 궁극기 타겟팅 상태
    private boolean awaitingMinimapTarget = false;
    
    // 메시지 콜백
    private final MessageCallback messageCallback;
    
    @FunctionalInterface
    public interface MessageCallback {
        void appendMessage(String message);
    }
    
    public SkillManager(Ability[] abilities, MessageCallback messageCallback) {
        this.abilities = abilities;
        this.messageCallback = messageCallback;
    }
    
    // ==================== Getters/Setters ====================
    
    public Map<String, List<ActiveEffect>> getEffectsByPlayer() { return effectsByPlayer; }
    public List<ActiveEffect> getMyEffects() { return myEffects; }
    public SkillEffectManager getSkillEffects() { return skillEffects; }
    
    public float getRavenDashRemaining() { return ravenDashRemaining; }
    public void setRavenDashRemaining(float value) { this.ravenDashRemaining = value; }
    
    public float getRavenOverchargeRemaining() { return ravenOverchargeRemaining; }
    public void setRavenOverchargeRemaining(float value) { this.ravenOverchargeRemaining = value; }
    
    public float getMissileSpeedMultiplier() { return missileSpeedMultiplier; }
    public void setMissileSpeedMultiplier(float value) { this.missileSpeedMultiplier = value; }
    
    public float getAttackSpeedMultiplier() { return attackSpeedMultiplier; }
    public void setAttackSpeedMultiplier(float value) { this.attackSpeedMultiplier = value; }
    
    public float getPiperMarkRemaining() { return piperMarkRemaining; }
    public void setPiperMarkRemaining(float value) { this.piperMarkRemaining = value; }
    
    public float getPiperThermalRemaining() { return piperThermalRemaining; }
    public void setPiperThermalRemaining(float value) { this.piperThermalRemaining = value; }
    
    public float getTeamMarkRemaining() { return teamMarkRemaining; }
    public void setTeamMarkRemaining(float value) { this.teamMarkRemaining = value; }
    
    public float getTeamThermalRemaining() { return teamThermalRemaining; }
    public void setTeamThermalRemaining(float value) { this.teamThermalRemaining = value; }
    
    public Ability[] getAbilities() { return abilities; }
    public void setAbilities(Ability[] abilities) { this.abilities = abilities; }
    
    public boolean isAwaitingMinimapTarget() { return awaitingMinimapTarget; }
    public void setAwaitingMinimapTarget(boolean value) { this.awaitingMinimapTarget = value; }
    
    // ==================== 업데이트 로직 ====================
    
    /**
     * 쿨다운 업데이트 (프레임마다 호출)
     */
    public void updateAbilities() {
        if (abilities == null) return;
        
        float deltaTime = 0.016f; // ~60 FPS
        for (Ability ability : abilities) {
            if (ability != null) {
                ability.update(deltaTime);
            }
        }
    }
    
    /**
     * 이펙트 타이머 업데이트 및 만료 제거
     */
    public void updateEffects() {
        float deltaTime = 0.016f;
        
        // 내 이펙트 업데이트
        myEffects.removeIf(eff -> {
            eff.remaining -= deltaTime;
            return eff.remaining <= 0;
        });
        
        // 다른 플레이어 이펙트 업데이트
        for (Map.Entry<String, List<ActiveEffect>> entry : effectsByPlayer.entrySet()) {
            List<ActiveEffect> list = entry.getValue();
            list.removeIf(eff -> {
                eff.remaining -= deltaTime;
                return eff.remaining <= 0;
            });
        }
        
        // 구조화된 스킬 이펙트 업데이트
        skillEffects.update(deltaTime);
    }
    
    /**
     * Raven 전용 런타임 처리
     */
    public void updateRavenRuntime() {
        float deltaTime = 0.016f;
        
        if (ravenDashRemaining > 0) {
            ravenDashRemaining -= deltaTime;
            if (ravenDashRemaining <= 0) {
                ravenDashRemaining = 0;
            }
        }
        
        if (ravenOverchargeRemaining > 0) {
            ravenOverchargeRemaining -= deltaTime;
            if (ravenOverchargeRemaining <= 0) {
                ravenOverchargeRemaining = 0;
                missileSpeedMultiplier = 1f;
            }
        }
    }
    
    /**
     * Piper 전용 런타임 처리
     */
    public void updatePiperRuntime() {
        float deltaTime = 0.016f;
        
        if (piperMarkRemaining > 0) {
            piperMarkRemaining -= deltaTime;
            if (piperMarkRemaining <= 0) {
                piperMarkRemaining = 0;
            }
        }
        
        if (piperThermalRemaining > 0) {
            piperThermalRemaining -= deltaTime;
            if (piperThermalRemaining <= 0) {
                piperThermalRemaining = 0;
            }
        }
    }
    
    /**
     * 팀 Piper 런타임 처리
     */
    public void updateTeamPiperRuntime() {
        float deltaTime = 0.016f;
        
        if (teamMarkRemaining > 0) {
            teamMarkRemaining -= deltaTime;
            if (teamMarkRemaining <= 0) {
                teamMarkRemaining = 0;
            }
        }
        
        if (teamThermalRemaining > 0) {
            teamThermalRemaining -= deltaTime;
            if (teamThermalRemaining <= 0) {
                teamThermalRemaining = 0;
            }
        }
    }
    
    // ==================== 이펙트 추가 ====================
    
    /**
     * 로컬 이펙트 추가
     */
    public void addLocalEffect(Ability ability) {
        if (ability == null) return;
        
        String type = ability.getType() != null ? ability.getType().name() : "TACTICAL";
        ActiveEffect effect = new ActiveEffect(ability.id, type, ability.duration);
        myEffects.add(effect);
    }
    
    /**
     * 플레이어 이펙트 추가
     */
    public void addPlayerEffect(String playerName, String abilityId, String type, float duration) {
        effectsByPlayer.computeIfAbsent(playerName, k -> new ArrayList<>());
        List<ActiveEffect> list = effectsByPlayer.get(playerName);
        ActiveEffect effect = new ActiveEffect(abilityId, type, duration);
        list.add(effect);
    }
    
    /**
     * 스킬 효과 적용 (캐릭터별 구현)
     */
    public void applySkillEffect(Ability ability, String characterId) {
        if (ability == null) return;
        
        String abilityId = ability.id;
        
        // Raven 스킬 처리
        if ("raven_dash".equals(abilityId)) {
            ravenDashRemaining = ability.duration;
            messageCallback.appendMessage("[Raven] 대쉬 활성화!");
        } else if ("raven_overcharge".equals(abilityId)) {
            ravenOverchargeRemaining = ability.duration;
            missileSpeedMultiplier = 1.5f;
            messageCallback.appendMessage("[Raven] 과충전 활성화!");
        }
        
        // Piper 스킬 처리
        else if ("piper_mark".equals(abilityId)) {
            piperMarkRemaining = ability.duration;
            messageCallback.appendMessage("[Piper] 적 표시 활성화!");
        } else if ("piper_thermal".equals(abilityId)) {
            piperThermalRemaining = ability.duration;
            messageCallback.appendMessage("[Piper] 열감지 활성화!");
        }
    }
    
    /**
     * 스킬 사용 가능 여부 확인
     */
    public boolean canUseSkill(int skillIndex) {
        if (abilities == null || skillIndex < 0 || skillIndex >= abilities.length) {
            return false;
        }
        
        Ability ability = abilities[skillIndex];
        return ability != null && ability.canUse();
    }
    
    /**
     * 스킬 쿨다운 시작
     */
    public void startCooldown(int skillIndex) {
        if (abilities == null || skillIndex < 0 || skillIndex >= abilities.length) {
            return;
        }
        
        Ability ability = abilities[skillIndex];
        if (ability != null) {
            ability.activate();
        }
    }
    
    /**
     * 모든 상태 리셋
     */
    public void reset() {
        myEffects.clear();
        effectsByPlayer.clear();
        
        ravenDashRemaining = 0f;
        ravenOverchargeRemaining = 0f;
        missileSpeedMultiplier = 1f;
        
        piperMarkRemaining = 0f;
        piperThermalRemaining = 0f;
        teamMarkRemaining = 0f;
        teamThermalRemaining = 0f;
        
        awaitingMinimapTarget = false;
        
        // 스킬 쿨다운 리셋
        if (abilities != null) {
            for (Ability ability : abilities) {
                if (ability != null) {
                    ability.resetCooldown();
                }
            }
        }
    }
}
