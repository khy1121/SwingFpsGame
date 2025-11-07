package com.fpsgame.common;

/**
 * 캐릭터 정보 데이터
 */
public class CharacterData {
    public final String id;
    public final String name;
    public final String description;
    public final float health;
    public final float speed;
    public final float armor;
    public final String role;
    public final String[] abilities;
    
    public CharacterData(String id, String name, String description, float health, float speed, float armor, String role, String[] abilities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.health = health;
        this.speed = speed;
        this.armor = armor;
        this.role = role;
        this.abilities = abilities;
    }
    
    // 기본 캐릭터 데이터
    public static final CharacterData[] CHARACTERS = {
        new CharacterData(
            "raven", "Raven", "빠른 기동성과 높은 화력",
            100f, 6.5f, 20f, "공격형",
            new String[]{"고속 연사 권총", "대쉬", "과충전"}
        ),
        new CharacterData(
            "piper", "Piper", "장거리 스나이퍼",
            80f, 5.5f, 15f, "정찰형",
            new String[]{"저격 소총", "적 표시", "열감지 스코프"}
        ),
        new CharacterData(
            "technician", "Technician", "공학 유틸리티 전문가",
            100f, 5.0f, 8f, "지원형",
            new String[]{"플라즈마 건", "지뢰", "터렛"}
        ),
        new CharacterData(
            "general", "General", "지휘관 역할",
            120f, 5.0f, 12f, "밸런스형",
            new String[]{"전술 소총", "지휘 오라", "공습"}
        ),
        new CharacterData(
            "bulldog", "Bulldog", "높은 방어력과 화력",
            200f, 4.5f, 40f, "탱커",
            new String[]{"미니건", "엄폐 자세", "폭발탄 난사"}
        ),
        new CharacterData(
            "wildcat", "Wildcat", "근접 전투 특화",
            110f, 5.2f, 10f, "돌격형",
            new String[]{"산탄총", "돌파 사격", "광폭화"}
        ),
        new CharacterData(
            "ghost", "Ghost", "은신과 위장 전문가",
            120f, 6.0f, 1f, "암살형",
            new String[]{"소음기 기관단총", "투명화", "열감지 무효화"}
        ),
        new CharacterData(
            "skull", "Skull", "용병 스타일",
            120f, 5.0f, 12f, "공격형",
            new String[]{"카빈 소총", "아드레날린", "탄약 보급"}
        ),
        new CharacterData(
            "steam", "Steam", "특수부대",
            110f, 5.4f, 10f, "밸런스형",
            new String[]{"돌격 소총", "EMP 수류탄", "전술 리셋"}
        ),
        new CharacterData(
            "sage", "Sage", "치료와 보조",
            100f, 5.3f, 8f, "힐러",
            new String[]{"기관단총", "치료 키트", "부활 드론"}
        )
    };
    
    public static CharacterData getById(String id) {
        for (CharacterData data : CHARACTERS) {
            if (data.id.equalsIgnoreCase(id)) {
                return data;
            }
        }
        return CHARACTERS[0]; // 기본값: Raven
    }
}
