export const CHARACTERS = {
  raven: {
    id: 'Raven',
    name: 'Raven', 
    role: 'Speed / Assault', 
    desc: '순간이동(Dash)과 공격 속도 증가(Overcharge)를 사용하는 기동성 중심의 캐릭터입니다.',
    stats: [80, 70, 95, 60], // Damage, Range, Speed, Utility
    skills: [
      'Dash (E): 벽을 통과하지 않는 순간이동 (200px)', 
      'Overcharge (R): 공속 35% 쿨다운 감소, 투사체 속도 1.8배'
    ]
  },
  piper: {
    id: 'Piper',
    name: 'Piper', 
    role: 'Sniper / Recon', 
    desc: '긴 사거리와 시야 확보(Mark)에 특화된 저격수입니다.',
    stats: [95, 100, 50, 80],
    skills: [
      'Mark (E): 시야 범위 1.5배 증가 (팀 공유)', 
      'Thermal (R): 모든 적 위치 미니맵 표시 (팀 공유)'
    ]
  },
  general: {
    id: 'General',
    name: 'General', 
    role: 'Support / Buffer', 
    desc: '주변 아군에게 버프를 제공하고 광역 폭격(Airstrike)을 지원합니다.',
    stats: [70, 70, 60, 90],
    skills: [
      'Aura (E): 반경 150px 아군 이속/공속 증가', 
      'Airstrike (R): 미니맵 지정 위치 2초 후 광역 폭발'
    ]
  },
  technician: {
    id: 'Technician',
    name: 'Technician', 
    role: 'Defense / Trap', 
    desc: '지뢰와 터렛을 설치하여 지역을 장악하는 수비형 캐릭터입니다.',
    stats: [65, 60, 50, 95],
    skills: [
      'Mine (E): 밟으면 폭발하는 지뢰 설치', 
      'Turret (R): 자동 사격 포탑 설치 (소유자 피격 무시)'
    ]
  },
  wildcat: {
    id: 'wildcat',
    name: 'Wildcat', 
    role: 'Melee / Burst', 
    desc: '근접전에 특화되어 있으며 적진을 돌파합니다.',
    stats: [90, 30, 85, 50],
    skills: [
      'Breach (E): 돌진하며 경로상 적 타격', 
      'Berserk (R): 방어력 무시 및 이속 대폭 증가'
    ]
  },
  sage: {
    id: 'Sage',
    name: 'Sage', 
    role: 'Healer', 
    desc: '아군의 체력을 회복시키고 부활시킵니다.',
    stats: [50, 60, 60, 100],
    skills: [
      'Heal (E): 범위 내 아군 체력 회복', 
      'Revive (R): 사망한 아군 부활'
    ]
  }
};

export const ARCHITECTURE_DETAILS = {
  view: { 
    title: "View (GameRenderer)", 
    desc: "화면 렌더링만을 전담합니다. Graphics2D를 사용하여 배경, 플레이어, 미사일, UI를 그립니다. Model 데이터를 참조하지만 직접 수정하지 않습니다.", 
    code: "renderer.render(gameState); // Draws the frame" 
  },
  controller: { 
    title: "Controller (GamePanel)", 
    desc: "사용자의 키보드/마우스 입력을 처리하고, 네트워크 클라이언트를 통해 서버와 통신합니다. 게임 루프의 메인 드라이버입니다.", 
    code: "if (key == 'W') network.sendMove(x, y-1);" 
  },
  model: { 
    title: "Model (GameState)", 
    desc: "게임의 모든 상태(플레이어 위치, HP, 점수 등)를 저장하는 데이터 객체입니다. 서버로부터 받은 패킷으로 동기화됩니다.", 
    code: "class GameState { Map<String, Player> players; ... }" 
  },
  manager_map: { 
    title: "MapManager", 
    desc: "맵 JSON 파일 로딩, walkableGrid 파싱, 장애물 충돌 체크, 맵 에디터 기능(F3/F4/F5) 제공. 686줄.", 
    code: "loadMap('map.json'); boolean walkable = isWalkable(x, y);" 
  },
  manager_skill: { 
    title: "SkillManager", 
    desc: "스킬 활성화, 쿨다운 타이머, 버프/디버프 적용, 설치형 오브젝트(지뢰/터렛) 관리. 320줄.", 
    code: "activateSkill(ability, x, y); update(deltaTime);" 
  },
  manager_col: { 
    title: "CollisionManager", 
    desc: "플레이어-미사일 충돌(반경 20px), 200ms 쿨다운으로 중복 피격 방지, 장애물 AABB 체크. 153줄.", 
    code: "if (checkMissileHit(x, y, missile) && canRegisterHit(id)) {...}" 
  },
  manager_obj: { 
    title: "GameObjectManager", 
    desc: "미사일, 설치물, 이펙트 생명주기 관리. 내 미사일/적 미사일 분리, 수명 체크. 338줄.", 
    code: "spawnMissile(x, y, dx, dy, owner); placeObject('mine', x, y);" 
  },
  manager_ui: {
    title: "UIManager",
    desc: "채팅 UI, 메뉴바, TAB 점수판 렌더링. 최대 50줄 메시지 관리, 시스템 메시지 색상 구분. 240줄.",
    code: "addChatMessage(sender, text); renderChat(g, width, height);"
  },
  manager_logic: {
    title: "GameLogicController",
    desc: "라운드 시작/종료, 3판 2선승 판정, 리스폰 타이머(5초), 라운드 제한(5분). 280줄.",
    code: "startRound(); checkRoundEnd(); endRound(winningTeam);"
  },
  manager_movement: {
    title: "PlayerMovementController",
    desc: "WASD 키 입력, 이동속도 계산(기본 5px), 충돌 회피, 버프 속도 조정. 195줄.",
    code: "handleInput(); movePlayer(dx, dy, direction);"
  },
  manager_spawn: {
    title: "SpawnManager",
    desc: "스폰 존 관리, 팀별 리스폰 위치 할당, 스폰 보호 로직. 약 150줄.",
    code: "getSpawnPosition(team); applySpawnProtection(player);"
  }
};
