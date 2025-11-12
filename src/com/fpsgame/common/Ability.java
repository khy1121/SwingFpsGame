package com.fpsgame.common;

/**
 * 캐릭터 스킬(Ability) 기본 클래스
 * 쿨타임만 있고 마나 비용 없음
 */
public class Ability {
    
    // 스킬 타입
    public enum AbilityType {
        BASIC,      // 기본 공격 (좌클릭)
        TACTICAL,   // 전술 스킬 (E키)
        ULTIMATE    // 궁극기 (R키)
    }
    
    public final String id;              // 스킬 고유 ID
    public final String name;            // 스킬 이름
    public final String description;     // 스킬 설명
    public final AbilityType type;       // 스킬 타입
    
    public final float cooldown;         // 쿨다운 (초)
    public final float duration;         // 지속 시간 (초, 0이면 즉발)
    public final float range;            // 사거리 (0이면 자신에게)
    public final float damage;           // 데미지 (0이면 공격 스킬 아님)
    
    private float currentCooldown = 0f;  // 현재 쿨다운 (0이면 사용 가능)
    private boolean isActive = false;    // 활성화 상태
    private float activeDuration = 0f;   // 활성화 지속 시간
    private float cooldownMultiplier = 1f; // 런타임 쿨다운 배수 (버프 등)
    
    public Ability(String id, String name, String description, AbilityType type,
                   float cooldown, float duration, float range, float damage) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.cooldown = cooldown;
        this.duration = duration;
        this.range = range;
        this.damage = damage;
    }
    
    /**
     * 프레임마다 호출 (쿨다운 감소)
     */
    public void update(float deltaTime) {
        if (currentCooldown > 0) {
            currentCooldown = Math.max(0, currentCooldown - deltaTime);
        }
        
        if (isActive && duration > 0) {
            activeDuration -= deltaTime;
            if (activeDuration <= 0) {
                deactivate();
            }
        }
    }
    
    /**
     * 스킬 사용 가능 여부
     */
    public boolean canUse() {
        return currentCooldown <= 0;
    }
    
    /**
     * 스킬 활성화
     */
    public void activate() {
        if (currentCooldown <= 0) {
            float mul = cooldownMultiplier > 0 ? cooldownMultiplier : 1f;
            currentCooldown = cooldown * mul;
            if (duration > 0) {
                isActive = true;
                activeDuration = duration;
            }
        }
    }
    
    /**
     * 스킬 비활성화
     */
    public void deactivate() {
        isActive = false;
        activeDuration = 0;
    }
    
    /**
     * 쿨다운 리셋 (테스트용)
     */
    public void resetCooldown() {
        currentCooldown = 0;
    }
    
    // Getters
    public void setCooldownMultiplier(float mul) { this.cooldownMultiplier = mul; }
    public float getCooldownMultiplier() { return cooldownMultiplier; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public AbilityType getType() { return type; }
    public float getCurrentCooldown() { return currentCooldown; }
    public float getCooldownPercent() { return cooldown > 0 ? currentCooldown / cooldown : 0; }
    public boolean isActive() { return isActive; }
    public float getActiveDuration() { return activeDuration; }
    public boolean isReady() { return currentCooldown <= 0; }
}
