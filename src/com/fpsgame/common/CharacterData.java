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
    public final String[] abilities;  // 스킬 이름 배열 (표시용)
    
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
    
    /**
     * 캐릭터의 스킬 생성
     * @param characterId 캐릭터 ID
     * @return [기본공격, 전술스킬, 궁극기] 배열
     */
    public static Ability[] createAbilities(String characterId) {
        switch (characterId.toLowerCase()) {
            case "raven":
                return new Ability[] {
                    new Ability("raven_basic", "고속 연사", "빠른 연사 권총 공격", Ability.AbilityType.BASIC, 0.3f, 0f, 500f, 15f),
                    new Ability("raven_dash", "대쉬", "빠르게 전방으로 돌진", Ability.AbilityType.TACTICAL, 5f, 0.5f, 200f, 0f),
                    new Ability("raven_overcharge", "과충전", "공격 속도 대폭 증가", Ability.AbilityType.ULTIMATE, 20f, 6f, 0f, 0f)
                };
            
            case "piper":
                return new Ability[] {
                    new Ability("piper_basic", "저격", "장거리 정확한 저격", Ability.AbilityType.BASIC, 1.2f, 0f, 1000f, 80f),
                    new Ability("piper_mark", "적 표시", "적을 마킹하여 투시", Ability.AbilityType.TACTICAL, 8f, 5f, 800f, 0f),
                    new Ability("piper_thermal", "열감지", "모든 적 위치 표시", Ability.AbilityType.ULTIMATE, 30f, 8f, 0f, 0f)
                };
            
            case "technician":
                return new Ability[] {
                    new Ability("tech_basic", "플라즈마", "플라즈마 건 발사", Ability.AbilityType.BASIC, 0.4f, 0f, 400f, 20f),
                    new Ability("tech_mine", "지뢰", "지뢰 설치", Ability.AbilityType.TACTICAL, 20f, 30f, 100f, 50f),
                    new Ability("tech_turret", "터렛", "자동 사격 터렛 배치", Ability.AbilityType.ULTIMATE, 40f, 20f, 150f, 25f)
                };
            
            case "general":
                return new Ability[] {
                    new Ability("gen_basic", "전술 소총", "정확한 소총 사격", Ability.AbilityType.BASIC, 0.4f, 0f, 600f, 25f),
                    new Ability("gen_aura", "지휘 오라", "아군 버프 제공", Ability.AbilityType.TACTICAL, 15f, 10f, 500f, 0f),
                    new Ability("gen_strike", "공습", "지정 지역 폭격", Ability.AbilityType.ULTIMATE, 40f, 3f, 800f, 150f)
                };
            
            case "bulldog":
                return new Ability[] {
                    new Ability("bull_basic", "미니건", "연속 사격", Ability.AbilityType.BASIC, 0.1f, 0f, 350f, 12f),
                    new Ability("bull_cover", "엄폐", "받는 피해 50% 감소", Ability.AbilityType.TACTICAL, 10f, 5f, 0f, 0f),
                    new Ability("bull_barrage", "폭발탄", "폭발 탄환 난사", Ability.AbilityType.ULTIMATE, 25f, 4f, 400f, 40f)
                };
            
            case "wildcat":
                return new Ability[] {
                    new Ability("wild_basic", "산탄총", "근거리 강력한 산탄", Ability.AbilityType.BASIC, 0.8f, 0f, 200f, 60f),
                    new Ability("wild_breach", "돌파", "전방 돌진 공격", Ability.AbilityType.TACTICAL, 6f, 0.3f, 300f, 30f),
                    new Ability("wild_berserk", "광폭화", "이동 속도와 공격력 증가", Ability.AbilityType.ULTIMATE, 30f, 8f, 0f, 0f)
                };
            
            case "ghost":
                return new Ability[] {
                    new Ability("ghost_basic", "소음기 SMG", "조용한 기관단총", Ability.AbilityType.BASIC, 0.2f, 0f, 300f, 18f),
                    new Ability("ghost_cloak", "투명화", "완전 투명 상태", Ability.AbilityType.TACTICAL, 15f, 6f, 0f, 0f),
                    new Ability("ghost_nullify", "열감지 무효", "감지 불가 상태", Ability.AbilityType.ULTIMATE, 30f, 10f, 0f, 0f)
                };
            
            case "skull":
                return new Ability[] {
                    new Ability("skull_basic", "카빈", "중거리 카빈 소총", Ability.AbilityType.BASIC, 0.35f, 0f, 500f, 22f),
                    new Ability("skull_adrenaline", "아드레날린", "체력 회복", Ability.AbilityType.TACTICAL, 15f, 0f, 0f, -50f),
                    new Ability("skull_ammo", "탄약 보급", "모든 스킬 쿨타임 초기화", Ability.AbilityType.ULTIMATE, 40f, 0f, 0f, 0f)
                };
            
            case "steam":
                return new Ability[] {
                    new Ability("steam_basic", "돌격 소총", "균형잡힌 소총", Ability.AbilityType.BASIC, 0.25f, 0f, 450f, 20f),
                    new Ability("steam_emp", "EMP", "적 스킬 봉쇄", Ability.AbilityType.TACTICAL, 15f, 3f, 400f, 10f),
                    new Ability("steam_reset", "전술 리셋", "즉시 위치 변경", Ability.AbilityType.ULTIMATE, 40f, 0f, 500f, 0f)
                };
            
            case "sage":
                return new Ability[] {
                    new Ability("sage_basic", "SMG", "빠른 기관단총", Ability.AbilityType.BASIC, 0.2f, 0f, 350f, 16f),
                    new Ability("sage_heal", "치료", "아군 또는 자신 회복", Ability.AbilityType.TACTICAL, 15f, 0f, 200f, -60f),
                    new Ability("sage_revive", "부활", "쓰러진 아군 부활", Ability.AbilityType.ULTIMATE, 90f, 3f, 300f, -100f)
                };
            
            default:
                // 기본값 (Raven)
                return new Ability[] {
                    new Ability("default_basic", "기본 공격", "기본 공격", Ability.AbilityType.BASIC, 0.5f, 0f, 400f, 20f),
                    new Ability("default_tactical", "전술 스킬", "전술 스킬", Ability.AbilityType.TACTICAL, 10f, 0f, 300f, 0f),
                    new Ability("default_ultimate", "궁극기", "궁극기", Ability.AbilityType.ULTIMATE, 60f, 5f, 0f, 0f)
                };
        }
    }
}
