package com.fpsgame.common;

/**
 * 캐릭터 데이터
 * 
 * 게임 내 캐릭터의 기본 정보를 정의합니다.
 * 
 * 주요 정보:
 * - 캐릭터 ID 및 이름
 * - 체력, 속도, 방어력 스탯
 * - 역할(Role) 및 스킬 목록
 * 
 * 현재 사용 가능한 캐릭터:
 * - Raven (공격형)
 * - Piper (정찰형)
 * - Technician (지원형)
 * - General (밸런스형)
 * 
 * 현재 게임에서 사용 불가능한 캐릭터:
 * - Bulldog (탱커)
 * - Wildcat (돌격형)
 * - Ghost (암살형)
 * - Skull (공격형)
 * - Steam (밸런스형)
 * - Sage (힐러)
 */
public class CharacterData {
    /** 캐릭터 고유 ID (예: "raven", "piper") */
    public final String id;
    
    /** 캐릭터 표시 이름 (예: "Raven", "Piper") */
    public final String name;
    
    /** 캐릭터 설명 */
    public final String description;
    
    /** 기본 체력 */
    public final float health;
    
    /** 이동 속도 */
    public final float speed;
    
    /** 방어력 (데미지 감소율) */
    public final float armor;
    
    /** 역할 (공격형, 지원형, 정찰형 등) */
    public final String role;
    
    /** 스킬 이름 배열 (UI 표시용) */
    public final String[] abilities;

    /**
     * 캐릭터 데이터 생성자
     * 
     * @param id 캐릭터 고유 ID
     * @param name 캐릭터 이름
     * @param description 캐릭터 설명
     * @param health 기본 체력
     * @param speed 이동 속도
     * @param armor 방어력
     * @param role 역할
     * @param abilities 스킬 이름 배열
     */
    public CharacterData(String id, String name, String description, float health, float speed, float armor,
                    String role, String[] abilities) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.health = health;
            this.speed = speed;
            this.armor = armor;
            this.role = role;
            this.abilities = abilities;
    }

    /**
     * 모든 캐릭터 데이터 배열
     * 
     * 게임 내 전체 캐릭터 목록입니다.
     * 처음 4개는 현재 게임에서 사용 가능하고,
     * 나머지는 미구현 상태입니다.
     */
    public static final CharacterData[] CHARACTERS = {
            // 사용 가능한 캐릭터
            new CharacterData(
                    "raven", "Raven", "빠른 기동성과 높은 화력",
                    100f, 6.5f, 20f, "공격형",
                    new String[] { "고속 연사 권총", "대쉬", "과충전" }),
            new CharacterData(
                    "piper", "Piper", "장거리 스나이퍼",
                    80f, 5.5f, 15f, "정찰형",
                    new String[] { "저격 소총", "적 표시", "열감지 스코프" }),
            new CharacterData(
                    "technician", "Technician", "공학 유틸리티 전문가",
                    100f, 5.0f, 8f, "지원형",
                    new String[] { "플라즈마 건", "지뢰", "터렛" }),
            new CharacterData(
                    "general", "General", "지휘관 역할",
                    120f, 5.0f, 12f, "밸런스형",
                    new String[] { "전술 소총", "지휘 오라", "공습" }),
            // 사용 불가능한 캐릭터
            new CharacterData(
                    "bulldog", "Bulldog", "높은 방어력과 화력",
                    200f, 4.5f, 40f, "탱커",
                    new String[] { "미니건", "엄폐 자세", "폭발탄 난사" }),
            new CharacterData(
                    "wildcat", "Wildcat", "근접 전투 특화",
                    110f, 5.2f, 10f, "돌격형",
                    new String[] { "산탄총", "돌파 사격", "광폭화" }),
            new CharacterData(
                    "ghost", "Ghost", "은신과 위장 전문가",
                    120f, 6.0f, 1f, "암살형",
                    new String[] { "소음기 기관단총", "투명화", "열감지 무효화" }),
            new CharacterData(
                    "skull", "Skull", "용병 스타일",
                    120f, 5.0f, 12f, "공격형",
                    new String[] { "카빈 소총", "아드레날린", "탄약 보급" }),
            new CharacterData(
                    "steam", "Steam", "특수부대",
                    110f, 5.4f, 10f, "밸런스형",
                    new String[] { "돌격 소총", "EMP 수류탄", "전술 리셋" }),
            new CharacterData(
                    "sage", "Sage", "치료와 보조",
                    100f, 5.3f, 8f, "힐러",
                    new String[] { "기관단총", "치료 키트", "부활 드론" })
    };

    /**
     * ID로 캐릭터 데이터 조회
     * 
     * @param id 캐릭터 ID (대소문자 구분 안 함)
     * @return 해당 캐릭터 데이터, 없으면 기본값(Raven) 반환
     */
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
     * 
     * 캐릭터 ID에 따라 해당 캐릭터의 스킬 세트를 생성합니다.
     * 각 캐릭터는 3개의 스킬을 가집니다:
     * - 기본 공격 (BASIC)
     * - 전술 스킬 (TACTICAL)
     * - 궁극기 (ULTIMATE)
     * 
     * @param characterId 캐릭터 ID
     * @return [기본공격, 전술스킬, 궁극기] 배열
     */
    public static Ability[] createAbilities(String characterId) {
        switch (characterId.toLowerCase()) {
            // Raven: 공격형 캐릭터
            case "raven":
                return new Ability[] {
                        new Ability("raven_basic", "고속 연사", "빠른 연사 권총 공격",
                                Ability.AbilityType.BASIC, 0.3f, 0f, 500f, 15f),
                        new Ability("raven_dash", "대쉬", "빠르게 전방으로 돌진",
                                Ability.AbilityType.TACTICAL, 5f, 0.5f, 200f, 0f),
                        new Ability("raven_overcharge", "과충전", "공격 속도 대폭 증가",
                                Ability.AbilityType.ULTIMATE, 20f, 6f, 0f, 0f)
                };

            // Piper: 정찰형 스나이퍼
            case "piper":
                return new Ability[] {
                        new Ability("piper_basic", "저격", "장거리 정확한 저격",
                                Ability.AbilityType.BASIC, 1.2f, 0f, 1000f, 80f),
                        new Ability("piper_mark", "적 표시", "적을 마킹하여 투시",
                                Ability.AbilityType.TACTICAL, 8f, 5f, 800f, 0f),
                        new Ability("piper_thermal", "열감지", "모든 적 위치 표시",
                                Ability.AbilityType.ULTIMATE, 30f, 8f, 0f, 0f)
                };

            // Technician: 지원형 공학자
            case "technician":
                return new Ability[] {
                        new Ability("tech_basic", "플라즈마", "플라즈마 건 발사",
                                Ability.AbilityType.BASIC, 0.4f, 0f, 400f, 20f),
                        new Ability("tech_mine", "지뢰", "지뢰 설치",
                                Ability.AbilityType.TACTICAL, 20f, 30f, 100f, 50f),
                        new Ability("tech_turret", "터렛", "자동 사격 터렛 배치",
                                Ability.AbilityType.ULTIMATE, 40f, 20f, 150f, 25f)
                };

            // General: 밸런스형 지휘관
            case "general":
                return new Ability[] {
                        new Ability("gen_basic", "전술 소총", "정확한 소총 사격",
                                Ability.AbilityType.BASIC, 0.4f, 0f, 600f, 25f),
                        new Ability("gen_aura", "지휘 오라", "아군 버프 제공",
                                Ability.AbilityType.TACTICAL, 15f, 10f, 500f, 0f),
                        new Ability("gen_strike", "공습", "지정 지역 폭격",
                                Ability.AbilityType.ULTIMATE, 40f, 3f, 800f, 150f)
                };

                        case "ghost":
                                return new Ability[] {
                                                new Ability("ghost_basic", "소음기 SMG", "조용한 기관단총",
                                                                Ability.AbilityType.BASIC, 0.2f, 0f, 300f,
                                                                18f),
                                                new Ability("ghost_cloak", "투명화", "일시적 투명 상태",
                                                                Ability.AbilityType.TACTICAL, 15f, 6f, 0f, 0f),
                                                new Ability("ghost_nullify", "열감지 무효", "감지 불가 상태",
                                                                Ability.AbilityType.ULTIMATE, 30f, 10f, 0f,
                                                                0f)
                                };

                        case "skull":
                                return new Ability[] {
                                                new Ability("skull_basic", "카빈", "중거리 카빈 소총", Ability.AbilityType.BASIC,
                                                                0.35f, 0f, 500f, 22f),
                                                new Ability("skull_adrenaline", "아드레날린", "체력 회복",
                                                                Ability.AbilityType.TACTICAL, 15f, 0f, 0f,
                                                                -50f),
                                                new Ability("skull_ammo", "탄약 보급", "모든 스킬 쿨타임 초기화",
                                                                Ability.AbilityType.ULTIMATE, 40f, 0f, 0f,
                                                                0f)
                                };

                        case "sage":
                                return new Ability[] {
                                                new Ability("sage_basic", "SMG", "빠른 기관단총", Ability.AbilityType.BASIC,
                                                                0.2f, 0f, 350f, 16f),
                                                new Ability("sage_heal", "치료", "아군 또는 자신 회복",
                                                                Ability.AbilityType.TACTICAL, 15f, 0f, 200f,
                                                                -60f),
                                                new Ability("sage_revive", "부활", "쓰러진 아군 부활",
                                                                Ability.AbilityType.ULTIMATE, 90f, 3f, 300f,
                                                                -100f)
                                };

                        case "bulldog":
                                return new Ability[] {
                                                new Ability("bull_basic", "미니건", "고속 연사 미니건", Ability.AbilityType.BASIC,
                                                                0.1f, 0f, 400f, 8f),
                                                new Ability("bull_cover", "엄폐", "방어력 대폭 증가",
                                                                Ability.AbilityType.TACTICAL, 12f, 4f, 0f, 0f),
                                                new Ability("bull_barrage", "폭발탄", "주변 광역 폭발",
                                                                Ability.AbilityType.ULTIMATE, 35f, 0f, 0f, 0f)
                                };

                        case "wildcat":
                                return new Ability[] {
                                                new Ability("wild_basic", "산탄총", "근거리 강력한 산탄",
                                                                Ability.AbilityType.BASIC, 0.8f, 0f, 250f, 15f), // 펠릿
                                                                                                                 // 구현은
                                                                                                                 // 별도
                                                                                                                 // 필요하지만
                                                                                                                 // 일단
                                                                                                                 // 단발
                                                                                                                 // 강한
                                                                                                                 // 데미지로
                                                new Ability("wild_breach", "돌파", "전방으로 빠르게 돌진하며 밀치기",
                                                                Ability.AbilityType.TACTICAL, 8f, 0.5f,
                                                                0f, 0f),
                                                new Ability("wild_berserk", "광폭화", "이동속도 및 공격력 증가",
                                                                Ability.AbilityType.ULTIMATE, 25f, 6f, 0f,
                                                                0f)
                                };

                        case "steam":
                                return new Ability[] {
                                                new Ability("steam_basic", "돌격소총", "밸런스형 소총", Ability.AbilityType.BASIC,
                                                                0.15f, 0f, 550f, 12f),
                                                new Ability("steam_emp", "EMP", "주변 적 스킬 차단",
                                                                Ability.AbilityType.TACTICAL, 18f, 0f, 300f, 0f),
                                                new Ability("steam_reset", "전술 리셋", "자신과 주변 아군 쿨타임 감소",
                                                                Ability.AbilityType.ULTIMATE, 45f, 0f,
                                                                400f, 0f)
                                };

                        default:
                                // 기본값 (Raven)
                                return new Ability[] {
                                                new Ability("default_basic", "기본 공격", "기본 공격",
                                                                Ability.AbilityType.BASIC, 0.5f, 0f, 400f, 20f),
                                                new Ability("default_tactical", "전술 스킬", "전술 스킬",
                                                                Ability.AbilityType.TACTICAL, 10f, 0f, 300f,
                                                                0f),
                                                new Ability("default_ultimate", "궁극기", "궁극기",
                                                                Ability.AbilityType.ULTIMATE, 60f, 5f, 0f, 0f)
                                };
                }
        }
}
