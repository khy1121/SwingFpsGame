import React, { useState } from 'react';

const CodeSection = () => {
  const [selectedCode, setSelectedCode] = useState('gameloop');

  const codeExamples = {
    gameloop: {
      title: 'Game Loop (60 FPS)',
      description: '16ms마다 실행되는 메인 게임 루프입니다. 60 FPS (1000ms / 60 = 16.67ms)를 유지하며 게임 상태를 업데이트하고 화면을 다시 그립니다. javax.swing.Timer는 EDT(Event Dispatch Thread)에서 실행되어 Swing 컴포넌트와 안전하게 상호작용합니다.',
      language: 'java',
      code: `// GamePanel.java - 60 FPS 게임 루프
private void startGame() {
    // 게임 업데이트 타이머 (60 FPS)
    // 16ms = 1000ms / 60 ≈ 60 FPS
    // javax.swing.Timer는 자동으로 EDT에서 실행
    timer = new javax.swing.Timer(16, e -> {
        updateGame();
        canvas.repaint();
    });
    timer.start();
    
    // 서버 메시지 수신 스레드
    networkClient.startReceiving();
}

private void updateGame() {
    // 1. 라운드 상태 체크
    // WAITING 상태에서 10초 대기 후 PLAYING으로 전환
    if (roundState == RoundState.WAITING) {
        long elapsed = System.currentTimeMillis() - roundStartTime;
        if (elapsed >= ROUND_READY_TIME) {
            roundState = RoundState.PLAYING;
            centerMessage = "Round Start!";
        }
    }
    
    // 2. 플레이어 위치 업데이트
    // 키 입력(W/A/S/D)을 읽어서 playerX, playerY 갱신
    // 장애물 충돌 체크, 카메라 추적, 서버에 위치 전송
    updatePlayerPosition();
    
    // 3. 미사일 이동
    // 모든 미사일의 위치를 속도벡터에 따라 갱신
    // 화면 밖으로 나가거나 TTL(Time To Live) 초과 시 제거
    updateMissiles();
    
    // 4. 충돌 감지
    // 플레이어-미사일 간 원형 히트박스 충돌 체크 (거리 계산)
    // 충돌 시 서버에 "HITME" 또는 "HIT" 메시지 전송
    checkCollisions();
    
    // 5. 스킬 쿨다운 업데이트
    // 모든 스킬의 currentCooldown을 deltaTime만큼 감소
    // 스킬 지속시간(duration)도 함께 업데이트
    updateAbilities();
    
    // 6. 다른 플레이어 보간 (Interpolation)
    // 서버에서 받은 위치와 현재 위치 사이를 선형 보간
    // 네트워크 지연을 부드럽게 처리하여 끊김 방지
    for (PlayerData pd : players.values()) {
        pd.smoothUpdate(); // targetX/Y로 0.5씩 이동
    }
}`
    },
    network: {
      title: '네트워크 통신 (TCP Socket)',
      description: 'NetworkClient 클래스로 분리된 실시간 네트워크 통신입니다. TCP 소켓을 사용하여 안정적인 패킷 전송을 보장합니다. 모든 메시지는 "프로토콜:데이터" 형식의 문자열로 전송되며, DataOutputStream.writeUTF()로 인코딩됩니다.',
      language: 'java',
      code: `// NetworkClient.java - 서버 통신 전담
// TCP Socket으로 Port 7777 서버에 연결
// DataOutputStream/DataInputStream으로 UTF-8 문자열 송수신
public class NetworkClient {
    private final Socket socket;              // 서버 연결
    private final DataOutputStream out;        // 송신 스트림
    private final DataInputStream in;          // 수신 스트림
    private Consumer<String> onMessageReceived; // 메시지 핸들러
    
    // 위치 전송 (이동할 때마다)
    // 프로토콜: "POS:x,y,direction" (direction: 0=DOWN, 1=UP, 2=LEFT, 3=RIGHT)
    // 60 FPS로 업데이트되므로 초당 최대 60회 전송 가능
    // flush()로 버퍼를 즉시 비워서 지연 최소화
    public void sendPosition(int x, int y, int direction) {
        try {
            out.writeUTF("POS:" + x + "," + y + "," + direction);
            out.flush(); // 버퍼를 즉시 비워서 전송 (Nagle 알고리즘 우회)
        } catch (IOException e) {
            System.err.println("Failed to send position");
        }
    }
    
    // 발사 전송 (마우스 클릭)
    // 프로토콜: "SHOOT:startX,startY,targetX,targetY"
    // 서버는 이 정보로 미사일 궤적을 계산하여 모든 클라이언트에 브로드캐스트
    public void sendShoot(int sx, int sy, int dx, int dy) {
        try {
            out.writeUTF("SHOOT:" + sx + "," + sy + "," + dx + "," + dy);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send shoot");
        }
    }
    
    // 피격 보고 (클라이언트 측 충돌 감지)
    // 프로토콜: "HIT:targetName" (내 미사일이 적을 맞춤)
    //          "HITME:shooterName,damage,missileId" (적 미사일에 맞음)
    // 클라이언트가 직접 충돌 체크 후 서버에 보고 (지연 최소화)
    public void sendHitReport(String message) {
        try {
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send hit report");
        }
    }
    
    // 메시지 수신 스레드
    // 별도 스레드에서 무한 루프로 서버 메시지 대기 (블로킹 I/O)
    // 메시지 수신 시 onMessageReceived 콜백 호출
    // GameMessageHandler가 프로토콜 파싱 후 적절한 처리 수행
    public void startReceiving() {
        new Thread(() -> {
            try {
                while (true) {
                    String message = in.readUTF(); // 블로킹 - 메시지 올 때까지 대기
                    if (onMessageReceived != null) {
                        // Consumer<String> 콜백 호출
                        // GameMessageHandler.handleMessage()로 연결됨
                        onMessageReceived.accept(message);
                    }
                }
            } catch (IOException e) {
                System.err.println("Connection lost");
                // TODO: 재연결 로직 또는 게임 종료 처리
            }
        }).start(); // 데몬 스레드가 아니므로 명시적 종료 필요
    }
}`
    },
    collision: {
      title: '충돌 감지 (Collision Detection)',
      description: 'CollisionManager로 분리된 충돌 감지입니다. 원형 히트박스로 충돌을 체크하며, 유클리드 거리 공식(√((x2-x1)² + (y2-y1)²))을 사용합니다. 반경 20픽셀 이내면 충돌로 판정하며, 200ms 쿨다운으로 중복 피격을 방지합니다.',
      language: 'java',
      code: `// GamePanel.java - 충돌 감지 및 처리
// 원형 히트박스: 플레이어와 미사일 모두 반경 20px 원으로 간주
// 두 원의 중심점 거리가 (반경1 + 반경2) 미만이면 충돌
private void checkCollisions() {
    // 내 미사일이 적 플레이어에 맞았는지 체크
    // Iterator 사용 이유: 루프 중 안전하게 요소 제거 가능
    Iterator<Missile> myIt = myMissiles.iterator();
    while (myIt.hasNext()) {
        Missile m = myIt.next();
        boolean hit = false;
        
        // 다른 플레이어와 충돌 체크
        for (Map.Entry<String, PlayerData> entry : players.entrySet()) {
            PlayerData pd = entry.getValue();
            // 적팀이고 생존 중인 플레이어만 체크
            if (pd.team != team && pd.hp > 0) {
                // 유클리드 거리 공식: d = √((x₂-x₁)² + (y₂-y₁)²)
                // Math.pow(a, 2) 대신 a*a가 더 빠르지만 가독성 우선
                double dist = Math.sqrt(
                    Math.pow(m.x - pd.x, 2) + 
                    Math.pow(m.y - pd.y, 2)
                );
                
                // 20px = 플레이어 반경(10px) + 미사일 반경(10px)
                if (dist < 20) { // 히트박스 충돌!
                    myIt.remove(); // 미사일 제거
                    hit = true;
                    
                    // 서버에 피격 보고 (서버가 데미지 처리)
                    String targetName = entry.getKey();
                    networkClient.sendHitReport("HIT:" + targetName);
                    System.out.println("[HIT] My missile hit " + targetName);
                    break; // 하나만 관통
                }
            }
        }
        
        // 장애물/타일 충돌
        if (!hit && isMissileBlocked((int)m.x, (int)m.y)) {
            myIt.remove();
        }
    }
    
    // 적 미사일이 나에게 맞았는지 체크 (피격자 입장)
    Iterator<Missile> enemyIt = enemyMissiles.iterator();
    while (enemyIt.hasNext()) {
        Missile m = enemyIt.next();
        // 내 플레이어와 적 미사일 간 거리 계산
        double dist = Math.sqrt(
            Math.pow(m.x - playerX, 2) + 
            Math.pow(m.y - playerY, 2)
        );
        
        if (dist < 20) { // 충돌!
            enemyIt.remove(); // 미사일 제거
            if (m.owner != null) {
                // 중복 피격 방지 메커니즘:
                // 같은 미사일 ID로부터 200ms 이내 재피격 무시
                // 이유: 네트워크 지연으로 같은 미사일이 여러 프레임에 걸쳐 감지될 수 있음
                long now = System.currentTimeMillis();
                if (!lastHitTime.containsKey(m.id) || 
                    now - lastHitTime.get(m.id) > 200) {
                    lastHitTime.put(m.id, now); // 마지막 피격 시간 기록
                    // 서버에 "나 맞았어요!" 보고
                    networkClient.sendHitReport("HITME:" + ownerInfo);
                }
            }
            break; // 한 프레임에 하나만 처리
        }
    }
}`
    },
    skill: {
      title: '스킬 시스템 (Ability System)',
      description: 'CharacterData와 Ability 클래스 기반의 스킬 시스템입니다. 각 캐릭터는 기본공격, 전술스킬, 궁극기 3개를 가지며, Factory Pattern으로 캐릭터별 스킬을 생성합니다. 쿨다운과 지속시간을 deltaTime(초 단위)으로 업데이트하여 프레임 독립적 동작을 보장합니다.',
      language: 'java',
      code: `// Ability.java - 스킬 기본 클래스
// Immutable 디자인: final 필드로 스킬 속성 고정
public class Ability {
    // Enum으로 타입 안전성 확보
    public enum AbilityType {
        BASIC,      // 기본 공격 (좌클릭) - 쿨타임 짧음
        TACTICAL,   // 전술 스킬 (E키) - 중간 쿨타임
        ULTIMATE    // 궁극기 (R키) - 긴 쿨타임, 강력한 효과
    }
    
    // 스킬 고유 정보 (생성 시 설정, 변경 불가)
    public final String id;              // 고유 ID: "raven_basic", "piper_mark"
    public final String name;            // 표시 이름: "고속 연사", "전술 표시"
    public final String description;     // 툴팁 설명
    public final AbilityType type;       // 스킬 타입 (BASIC/TACTICAL/ULTIMATE)
    
    // 스킬 스탯 (초 단위)
    public final float cooldown;         // 쿨다운 시간 (초) - 재사용 대기
    public final float duration;         // 지속 시간 (초) - 버프/디버프 유효 시간
    public final float range;            // 사거리 (픽셀) - 스킬 도달 범위
    public final float damage;           // 데미지 (HP) - 공격 스킬 데미지
    
    // 런타임 상태 (매 프레임 업데이트)
    private float currentCooldown;       // 남은 쿨다운 (0이면 사용 가능)
    private boolean isActive;            // 스킬 활성 여부
    private float activeDuration;        // 남은 지속 시간
    
    // 스킬 사용 가능 여부 체크
    public boolean canUse() {
        return currentCooldown <= 0; // 쿨다운이 끝났으면 true
    }
    
    // 스킬 활성화
    public void activate() {
        if (canUse()) {
            currentCooldown = cooldown;       // 쿨다운 시작
            isActive = true;                  // 활성화 플래그
            activeDuration = duration;        // 지속 시간 설정
        }
    }
    
    // 매 프레임 호출 (16ms마다)
    // deltaTime = 0.016초 (60 FPS 기준)
    public void update(float deltaTime) {
        // 쿨다운 감소
        if (currentCooldown > 0) {
            currentCooldown -= deltaTime; // 0.016초씩 감소
            if (currentCooldown < 0) currentCooldown = 0;
        }
        
        // 지속 시간 감소
        if (isActive) {
            activeDuration -= deltaTime;
            if (activeDuration <= 0) {
                isActive = false; // 효과 종료
            }
        }
    }
}

// CharacterData.java - Factory Pattern으로 스킬 생성
// createAbilities(String characterId) 메서드는 캐릭터별 스킬 배열 반환
case "raven": // Assault - 빠른 공격형 캐릭터
    return new Ability[] {
        // [0] 기본 공격: 고속 연사 (0.3초 쿨타임, 15 데미지)
        // 마우스 좌클릭 시 발동, DPS = 15 / 0.3 = 50
        new Ability("raven_basic", "고속 연사", "빠른 연사 권총 공격",
            Ability.AbilityType.BASIC, 0.3f, 0f, 500f, 15f),
        
        // [1] 전술 스킬: 대쉬 (E키, 5초 쿨타임, 0.5초 지속)
        // ravenDashRemaining 변수로 200px 빠른 이동 구현
        new Ability("raven_dash", "대쉬", "빠르게 전방으로 돌진",
            Ability.AbilityType.TACTICAL, 5f, 0.5f, 200f, 0f),
        
        // [2] 궁극기: 과충전 (R키, 20초 쿨타임, 6초 지속)
        // 공격 쿨다운 50% 감소 (0.3초 → 0.15초), 투사체 속도 1.5배
        new Ability("raven_overcharge", "과충전", "공격 속도 대폭 증가",
            Ability.AbilityType.ULTIMATE, 20f, 6f, 0f, 0f)
    };`
    },
    renderer: {
      title: '렌더링 (Graphics2D)',
      description: 'GameRenderer 클래스로 분리된 렌더링 시스템입니다. RenderContext DTO를 받아 불변 데이터로 그리기를 수행합니다. Graphics2D로 안티앨리어싱과 알파 블렌딩을 활용하며, JPanel.paintComponent()의 double buffering으로 화면 깜박임을 방지합니다.',
      language: 'java',
      code: `// GameRenderer.java - 렌더링 전담 클래스
// Model-View 분리: GamePanel(로직) → RenderContext(DTO) → GameRenderer(뷰)
public class GameRenderer {
    // 렌더링 메인 메서드 - 매 프레임 호출 (60 FPS)
    public void render(Graphics g, RenderContext ctx) {
        Graphics2D g2d = (Graphics2D) g; // 고급 기능 사용을 위해 캐스팅
        
        // 1. 맵 렌더링 (카메라 오프셋 적용)
        // 카메라 좌표만큼 음수 오프셋으로 맵 전체를 이동 시키기
        // 플레이어는 화면 중앙 고정, 맵이 이동하는 효과
        if (ctx.mapImage != null) {
            g2d.drawImage(ctx.mapImage, 
                -ctx.cameraX, -ctx.cameraY,          // 카메라 오프셋
                ctx.mapWidth, ctx.mapHeight, null);   // 크기 유지
        }
        
        // 2. 플레이어 렌더링 (스프라이트 애니메이션)
        // 4방향 보행 애니메이션: DOWN(0), UP(1), LEFT(2), RIGHT(3)
        for (PlayerData pd : ctx.players.values()) {
            if (pd.animations != null && 
                pd.direction < pd.animations.length) {
                // SpriteAnimation.getCurrentFrame()으로 현재 프레임 획득
                // 150ms마다 프레임 자동 전환 (4프레임 루프)
                BufferedImage frame = 
                    pd.animations[pd.direction].getCurrentFrame();
                
                // 스크린 좌표로 변환: 월드 좌표 - 카메라 오프셋
                // 스프라이트 중심 정렬: -24, -32 오프셋
                int screenX = pd.x - ctx.cameraX - 24;
                int screenY = pd.y - ctx.cameraY - 32;
                g2d.drawImage(frame, screenX, screenY, 48, 64, null); // 48x64px 캐릭터
            }
            
            // HP 바 렌더링 (플레이어 머리 위 5px 높이)
            int barY = pd.y - ctx.cameraY - 40;
            // 배경: 빨간색 전체 바 (50px)
            g2d.setColor(Color.RED);
            g2d.fillRect(pd.x - ctx.cameraX - 25, barY, 50, 5);
            
            // 현재 HP: 초록색 부분 바 (HP 비율만큼)
            float hpRatio = (float)pd.hp / pd.maxHp;
            g2d.setColor(Color.GREEN);
            g2d.fillRect(pd.x - ctx.cameraX - 25, barY, 
                (int)(50 * hpRatio), 5); // HP에 비례하는 너비
        }
        
        // 3. 미사일 렌더링
        // 노란색 6x6px 원으로 표현
        g2d.setColor(Color.YELLOW);
        for (Missile m : ctx.myMissiles) {
            int sx = (int)m.x - ctx.cameraX;
            int sy = (int)m.y - ctx.cameraY;
            g2d.fillOval(sx - 3, sy - 3, 6, 6); // 중심점 기준 그리기
        }
        
        // 4. UI 렌더링 (스크린 고정 위치 - 카메라 무관)
        // 스킬 쿨다운, 미니맵, K/D 표시, HP/체력 바 등
        renderUI(g2d, ctx);
    }
    
    private void renderUI(Graphics2D g2d, RenderContext ctx) {
        // 스킬 쿨다운 UI 표시 (화면 왼쪽 하단)
        if (ctx.abilities != null) {
            int skillX = 20; // 시작 X 좌표
            for (int i = 0; i < ctx.abilities.length; i++) {
                Ability ab = ctx.abilities[i];
                
                // 쿨다운 원형 배경 (반투명 회색)
                g2d.setColor(new Color(40, 40, 50, 180));
                g2d.fillOval(skillX, 650, 50, 50);
                
                // 쿨다운 진행률 표시 (Arc 그리기)
                // 12시 방향(90°)부터 시계 반대 방향으로 채워짐
                if (ab.getCurrentCooldown() > 0) {
                    float ratio = ab.getCurrentCooldown() / ab.cooldown;
                    g2d.setColor(new Color(255, 100, 100, 150)); // 반투명 빨간색
                    g2d.fillArc(skillX, 650, 50, 50, 90,  // 12시부터 시작
                        -(int)(360 * ratio)); // 쿨다운 비율만큼 각도
                }
                
                skillX += 60; // 다음 스킬 위치
            }
        }
    }
}`
    },
    mapeditor: {
      title: '맵 에디터 (F3/F4/F5)',
      description: 'MapManager로 분리된 맵 에디터입니다. F4로 편집 모드 활성화, 마우스 드래그로 타일 페인팅, F5로 JSON 파일로 저장합니다. 32x32px 타일 기반 그리드 시스템으로 장애물과 스폰 지역을 편집합니다.',
      language: 'java',
      code: `// GamePanel.java - 맵 에디터 키 바인딩
// KeyListener 인터페이스 구현으로 키보드 입력 처리
@Override
public void keyPressed(KeyEvent e) {
    int keyCode = e.getKeyCode();
    keys[keyCode] = true; // 키 상태 배열 업데이트
    
    // switch expression (자바 14+)
    switch (keyCode) {
        case KeyEvent.VK_F3 -> { // 장애물 디버그 토글
            debugObstacles = !debugObstacles;
            // walkableGrid 배열을 시각적으로 표시 (빨간색 타일)
            appendChatMessage("[디버그] 장애물 표시: " + debugObstacles);
        }
        case KeyEvent.VK_F4 -> { // 편집 모드 토글
            editMode = !editMode;
            if (editMode) {
                // 4가지 페인트 모드: 0=이동가능, 1=장애물, 2=RED스폰, 3=BLUE스폰
                appendChatMessage("[편집] 모드 활성화 (1:이동가능, 2:장애물, 3:RED스폰, 4:BLUE스폰)");
            } else {
                appendChatMessage("[편집] 모드 비활성화");
            }
        }
        case KeyEvent.VK_F5 -> { // 맵 저장
            saveEditedMap(); // JSON 파일로 직렬화
        }
        case KeyEvent.VK_F6 -> { // 맵 순환 (map → map2 → map3 → village)
            if (mapCycle != null && !mapCycle.isEmpty()) {
                int idx = mapCycle.indexOf(currentMapName);
                idx = (idx >= 0) ? (idx + 1) % mapCycle.size() : 0; // 순환
                mapIndex = idx;
                switchMap(mapCycle.get(idx)); // 맵 로드
            }
        }
        // 1~4 키: 페인트 모드 선택
        case KeyEvent.VK_1 -> { if (editMode) editPaintMode = 0; } // 이동 가능
        case KeyEvent.VK_2 -> { if (editMode) editPaintMode = 1; } // 장애물
        case KeyEvent.VK_3 -> { if (editMode) editPaintMode = 2; } // RED 스폰
        case KeyEvent.VK_4 -> { if (editMode) editPaintMode = 3; } // BLUE 스폰
    }
}

// 맵 JSON 저장 - 수동 직렬화 (Jackson/Gson 대신)
// StringBuilder로 JSON 문자열 직접 생성
private void saveEditedMap() {
    try {
        StringBuilder json = new StringBuilder();
        json.append("{\\n"); // JSON 시작
        json.append("  \\"meta\\": {\\n"); // 메타 정보
        json.append("    \\"map_pixel_size\\": {\\"w\\":").append(mapWidth)
           .append(", \\"h\\":").append(mapHeight).append("},\\n");
        json.append("    \\"tile_size\\": ").append(TILE_SIZE).append("\\n"); // 32px
        json.append("  },\\n");
        
        // 장애물 타일 저장 (walkableGrid == false인 타일)
        json.append("  \\"obstacles\\": [\\n");
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                if (!walkableGrid[row][col]) { // 이동 불가 타일
                    json.append("    {\\"x\\":").append(col)
                       .append(", \\"y\\":").append(row).append("},\\n");
                }
            }
        }
        json.append("  ],\\n");
        
        // 스폰 지역 저장 (타일 좌표 배열)
        json.append("  \\"spawns\\": {\\n");
        json.append("    \\"red\\": [").append(redSpawnTiles).append("],\\n");
        json.append("    \\"blue\\": [").append(blueSpawnTiles).append("]\\n");
        json.append("  }\\n");
        json.append("}"); // JSON 종료
        
        // 파일 저장 (Java 11+ Files.writeString)
        String filename = "assets/maps/" + currentMapName + "_edited.json";
        Files.writeString(Paths.get(filename), json.toString());
        appendChatMessage("[저장] " + filename);
    } catch (IOException e) {
        appendChatMessage("[에러] 저장 실패: " + e.getMessage());
    }
}`
    },
    scoreboard: {
      title: '스코어보드 (TAB키)',
      description: 'TAB 키로 실시간 스코어보드를 표시합니다. 팀별 플레이어 목록, K/D, HP, 궁극기 충전 상태를 보여줍니다.',
      language: 'java',
      code: `// GameRenderer.java - 스코어보드 렌더링
private void renderScoreboard(Graphics2D g2d, RenderContext ctx) {
    if (!ctx.showScoreboard) return;
    
    // 반투명 배경
    g2d.setColor(new Color(0, 0, 0, 200));
    g2d.fillRect(200, 100, 800, 500);
    
    // 타이틀
    g2d.setColor(Color.WHITE);
    g2d.setFont(new Font("Noto Sans KR", Font.BOLD, 24));
    g2d.drawString("스코어보드 (TAB)", 450, 140);
    
    // RED 팀 헤더
    g2d.setColor(new Color(220, 80, 80));
    g2d.fillRect(220, 160, 360, 40);
    g2d.setColor(Color.WHITE);
    g2d.drawString("RED TEAM", 250, 185);
    
    // BLUE 팀 헤더
    g2d.setColor(new Color(80, 120, 220));
    g2d.fillRect(620, 160, 360, 40);
    g2d.setColor(Color.WHITE);
    g2d.drawString("BLUE TEAM", 650, 185);
    
    int redY = 220;
    int blueY = 220;
    
    // 플레이어 목록
    for (Map.Entry<String, PlayerData> entry : ctx.players.entrySet()) {
        PlayerData pd = entry.getValue();
        String name = entry.getKey();
        
        if (pd.team == GameConstants.TEAM_RED) {
            // RED 팀 플레이어
            g2d.setColor(Color.WHITE);
            g2d.drawString(name, 240, redY);
            g2d.drawString(pd.kills + " / " + pd.deaths, 360, redY);
            g2d.drawString("HP: " + pd.hp, 460, redY);
            
            // 궁극기 충전 상태
            if (pd.characterId != null) {
                Ability[] abilities = CharacterData.createAbilities(pd.characterId);
                Ability ult = abilities[2]; // 궁극기
                
                g2d.setColor(Color.GRAY);
                g2d.fillRect(520, redY - 10, 40, 15);
                
                float chargeRatio = 1.0f - (ult.getCurrentCooldown() / ult.cooldown);
                if (chargeRatio >= 1.0f) {
                    g2d.setColor(Color.YELLOW);
                    g2d.drawString("✓", 525, redY);
                } else {
                    g2d.setColor(Color.ORANGE);
                    g2d.fillRect(520, redY - 10, (int)(40 * chargeRatio), 15);
                }
            }
            
            redY += 30;
        } else {
            // BLUE 팀 플레이어
            g2d.setColor(Color.WHITE);
            g2d.drawString(name, 640, blueY);
            g2d.drawString(pd.kills + " / " + pd.deaths, 760, blueY);
            g2d.drawString("HP: " + pd.hp, 860, blueY);
            
            // 궁극기 상태
            if (pd.characterId != null) {
                Ability[] abilities = CharacterData.createAbilities(pd.characterId);
                Ability ult = abilities[2];
                
                float chargeRatio = 1.0f - (ult.getCurrentCooldown() / ult.cooldown);
                if (chargeRatio >= 1.0f) {
                    g2d.setColor(Color.YELLOW);
                    g2d.drawString("✓", 925, blueY);
                } else {
                    g2d.setColor(Color.ORANGE);
                    g2d.fillRect(920, blueY - 10, (int)(40 * chargeRatio), 15);
                }
            }
            
            blueY += 30;
        }
    }
    
    // 라운드 정보
    g2d.setColor(Color.WHITE);
    g2d.drawString("Round: " + ctx.roundCount, 450, 560);
    g2d.drawString("Score: " + ctx.redWins + " - " + ctx.blueWins, 450, 590);
}`
    },
    server: {
      title: 'GameServer (멀티플레이어)',
      description: '최대 4명의 플레이어를 지원하는 게임 서버입니다. 라운드 시스템, 캐릭터 선택 제한, 팀 밸런스를 관리합니다.',
      language: 'java',
      code: `// GameServer.java - 메인 서버 클래스
public class GameServer {
    private final ServerSocket serverSocket;
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final int MAX_PLAYERS = 4;
    
    // 라운드 시스템 (3판 2선승)
    private int roundCount = 0;
    private int redWins = 0;
    private int blueWins = 0;
    private boolean roundEnded = false;
    private static final int MAX_WINS = 2;
    private String selectedMap = null;
    
    // 캐릭터 선택 제한 (라운드당 1회, 10초 이내)
    private long currentRoundStartTime = 0;
    private final Map<String, Boolean> playerCharacterChanged = new ConcurrentHashMap<>();
    
    // 게임 오브젝트
    private final Map<Integer, PlacedObject> placedObjects = new ConcurrentHashMap<>();
    private final Map<String, ActiveAura> activeAuras = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledStrike> scheduledStrikes = new ConcurrentHashMap<>();
    
    public void start() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (clients.size() < MAX_PLAYERS) {
                        new Thread(new ClientHandler(clientSocket)).start();
                    } else {
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }).start();
    }
    
    // 라운드 시작
    private void startNextRound() {
        roundCount = (roundCount == 0) ? 1 : roundCount + 1;
        roundEnded = false;
        
        // 랜덤 맵 선택
        String[] availableMaps = {"map", "map2", "map3", "village"};
        selectedMap = availableMaps[new Random().nextInt(availableMaps.length)];
        
        // 게임 상태 초기화
        placedObjects.clear();
        activeAuras.clear();
        scheduledStrikes.clear();
        playerCharacterChanged.clear();
        currentRoundStartTime = System.currentTimeMillis();
        
        // 모든 플레이어 HP 초기화
        for (ClientHandler ch : clients.values()) {
            if (ch.playerInfo != null && ch.playerInfo.characterId != null) {
                CharacterData cd = CharacterData.getById(ch.playerInfo.characterId);
                ch.playerInfo.hp = (int) cd.health;
            }
        }
        
        // ROUND_START 패킷 전송
        StringBuilder msg = new StringBuilder();
        msg.append("ROUND_START:").append(roundCount).append(",").append(selectedMap).append(";");
        msg.append(clients.size());
        
        for (ClientHandler ch : clients.values()) {
            if (ch.playerInfo != null) {
                String charId = ch.playerInfo.characterId != null ? ch.playerInfo.characterId : "raven";
                int maxHp = (int) CharacterData.getById(charId).health;
                msg.append(";").append(ch.playerName).append(",")
                   .append(charId).append(",")
                   .append(ch.playerInfo.hp).append(",")
                   .append(maxHp);
            }
        }
        
        broadcast(msg.toString(), null);
        System.out.println("[ROUND_START] Round " + roundCount + " on " + selectedMap);
    }
    
    // 라운드 종료 체크
    private void checkRoundEnd() {
        if (roundEnded || clients.size() < 2) return;
        
        int redAlive = 0, blueAlive = 0;
        for (ClientHandler ch : clients.values()) {
            if (ch.playerInfo == null) continue;
            if (ch.playerInfo.team == GameConstants.TEAM_RED && ch.playerInfo.hp > 0) redAlive++;
            else if (ch.playerInfo.team == GameConstants.TEAM_BLUE && ch.playerInfo.hp > 0) blueAlive++;
        }
        
        // 한 팀 전멸 시 라운드 종료
        if (redAlive == 0) endRound(GameConstants.TEAM_BLUE);
        else if (blueAlive == 0) endRound(GameConstants.TEAM_RED);
    }
    
    private void endRound(int winningTeam) {
        roundEnded = true;
        String winTeamName = (winningTeam == GameConstants.TEAM_RED) ? "RED" : "BLUE";
        
        if (winningTeam == GameConstants.TEAM_RED) redWins++;
        else blueWins++;
        
        broadcast("CHAT:=== 라운드 종료! " + winTeamName + " 팀 승리! ===", null);
        broadcast("ROUND_WIN:" + winningTeam + "," + redWins + "," + blueWins, null);
        
        // 게임 종료 체크 (2선승)
        if (redWins >= MAX_WINS || blueWins >= MAX_WINS) {
            broadcast("GAME_OVER:" + winTeamName, null);
            new Timer().schedule(new TimerTask() {
                public void run() { resetGameState(); }
            }, 10000);
        } else {
            new Timer().schedule(new TimerTask() {
                public void run() { startNextRound(); }
            }, 3000);
        }
    }
}`
    },

    sprite: {
      title: '스프라이트 애니메이션',
      description: '48x64 픽셀 스프라이트 시트를 4방향(Down/Right/Up/Left)으로 분할하여 150ms마다 프레임 전환. ResourceManager가 이미지를 캐싱하고, SpriteAnimation이 시간 기반 프레임 업데이트를 처리합니다.',
      language: 'java',
      code: `// GamePanel.java - 스프라이트 로딩
void loadSprites() {
    String spritePath = "assets/characters/" + charId + "_48_64.png";
    
    // 스프라이트 시트 로드 (48x64 크기, 4행 구조)
    BufferedImage[] walkSheet = rm.getSpriteSheet(spritePath, 48, 64);
    
    if (walkSheet != null && walkSheet.length > 0) {
        int framesPerRow = walkSheet.length / 4; // 보통 8프레임/방향
        
        // 각 방향별 프레임 추출
        BufferedImage[] downFrames = new BufferedImage[framesPerRow];
        BufferedImage[] rightFrames = new BufferedImage[framesPerRow];
        BufferedImage[] upFrames = new BufferedImage[framesPerRow];
        BufferedImage[] leftFrames = new BufferedImage[framesPerRow];
        
        for (int i = 0; i < framesPerRow; i++) {
            downFrames[i] = walkSheet[0 * framesPerRow + i];  // Row 0
            rightFrames[i] = walkSheet[1 * framesPerRow + i]; // Row 1
            upFrames[i] = walkSheet[2 * framesPerRow + i];    // Row 2
            leftFrames[i] = walkSheet[3 * framesPerRow + i];  // Row 3
        }
        
        // 애니메이션 객체 생성 (150ms/프레임, loop=true)
        myAnimations[0] = new SpriteAnimation(downFrames, 150, true);
        myAnimations[3] = new SpriteAnimation(rightFrames, 150, true);
    }
}

// SpriteAnimation.java - 프레임 업데이트
public void update() {
    if (isFinished) return;
    
    long now = System.currentTimeMillis();
    if (now - lastTime >= frameDuration) { // 150ms 경과?
        currentFrame++;
        lastTime = now;
        
        if (currentFrame >= frames.length) {
            currentFrame = loop ? 0 : frames.length - 1;
            isFinished = !loop;
        }
    }
}`
    },

    skilleffect: {
      title: '스킬 이펙트 구현',
      description: 'SkillEffect 추상 클래스를 상속받아 22개 캐릭터별 스킬 이펙트 구현. 각 이펙트는 duration 동안 remaining 시간을 감소시키며, Graphics2D로 링/아크/라인을 그립니다.',
      language: 'java',
      code: `// SkillEffect.java - 추상 기본 클래스
public abstract class SkillEffect {
    protected final String id;
    protected final float duration;
    protected float remaining;
    
    protected SkillEffect(String id, float duration) {
        this.id = id;
        this.duration = Math.max(0.05f, duration);
        this.remaining = this.duration;
    }
    
    public void update(float dt) { remaining -= dt; }
    public boolean isExpired() { return remaining <= 0f; }
    
    public abstract void drawSelf(Graphics2D g2d, int x, int y);
}

// RavenDashEffect.java - Raven 전술 스킬
public class RavenDashEffect extends SkillEffect {
    public RavenDashEffect(float duration) {
        super("raven_dash", duration);
    }
    
    @Override
    public void drawSelf(Graphics2D g2d, int x, int y) {
        float elapsed = duration - remaining;
        int alpha = (int)(200 * (remaining / duration));
        
        int r = 35;
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(new Color(100, 200, 255, alpha));
        
        // 빠른 회전 아크 4개 (대쉬 속도감)
        for (int i = 0; i < 4; i++) {
            int arcStart = (int)((elapsed * 400 + i * 90) % 360);
            g2d.drawArc(x-r, y-r, r*2, r*2, arcStart, 60);
        }
    }
}

// GamePanel.java - 이펙트 사용
private void addLocalEffect(Ability ability) {
    if ("raven_dash".equals(ability.id)) {
        skillEffects.addSelf(new RavenDashEffect(ability.duration));
    }
}`
    },

    mapkeys: {
      title: '맵 에디터 (F3~F6 키)',
      description: 'F3=디버그 토글, F4=편집모드 토글, F5=JSON 저장, F6=맵 순환. 편집모드에서는 1~4키로 Paint Mode 전환(walkable/red/blue/obstacle)하여 타일 그리드를 실시간 수정합니다.',
      language: 'java',
      code: `// GamePanel.java - 키보드 입력 처리
@Override
public void keyPressed(KeyEvent e) {
    int code = e.getKeyCode();
    
    // F3: 장애물 디버그 표시 토글
    if (code == KeyEvent.VK_F3) {
        debugObstacles = !debugObstacles;
        appendChatMessage("[DEBUG] 장애물: " + debugObstacles);
    }
    
    // F4: 맵 편집 모드 토글
    else if (code == KeyEvent.VK_F4) {
        editMode = !editMode;
        appendChatMessage("[EDITOR] 편집: " + editMode);
    }
    
    // F5: JSON 저장
    else if (code == KeyEvent.VK_F5 && editMode) {
        saveMapToJson();
    }
    
    // F6: 맵 순환
    else if (code == KeyEvent.VK_F6) {
        cycleNextMap();
    }
    
    // 1~4: Paint Mode (편집 모드)
    if (editMode) {
        if (code == KeyEvent.VK_1) editPaintMode = 0; // walkable
        else if (code == KeyEvent.VK_2) editPaintMode = 1; // red spawn
        else if (code == KeyEvent.VK_3) editPaintMode = 2; // blue spawn
        else if (code == KeyEvent.VK_4) editPaintMode = 3; // obstacle
    }
}`
    },

    mapselect: {
      title: '맵 랜덤 선택 & 렌더링',
      description: '서버가 라운드 시작 시 4개 맵(map/map2/map3/village) 중 랜덤 선택하여 ROUND_START 패킷에 포함. 클라이언트는 서버가 선택한 맵을 loadMap()으로 로드하고, GameRenderer가 카메라 오프셋을 적용하여 렌더링합니다.',
      language: 'java',
      code: `// GameServer.java - 서버 측 맵 랜덤 선택
private void startNextRound() {
    roundCount = (roundCount == 0) ? 1 : roundCount + 1;
    
    // 랜덤 맵 선택 (4개 중 1개)
    String[] availableMaps = {"map", "map2", "map3", "village"};
    selectedMap = availableMaps[new Random().nextInt(availableMaps.length)];
    
    // ROUND_START 패킷: "ROUND_START:roundNum,mapName;..."
    StringBuilder msg = new StringBuilder();
    msg.append("ROUND_START:").append(roundCount)
       .append(",").append(selectedMap).append(";");
    
    broadcast(msg.toString(), null);
}

// GameMessageHandler.java - 클라이언트 맵 로드
private void handleRoundStart(String data) {
    String[] parts = data.split(",");
    String newMapId = parts[1].trim();
    
    if (!newMapId.equals(gamePanel.currentMapName)) {
        gamePanel.currentMapName = newMapId;
        gamePanel.loadMap(newMapId);
    }
}

// GameRenderer.java - 맵 렌더링
private void drawMap(Graphics2D g2d, RenderContext ctx) {
    if (ctx.mapImage != null) {
        g2d.drawImage(ctx.mapImage, 
                     -ctx.cameraX, -ctx.cameraY,
                     ctx.mapWidth, ctx.mapHeight, null);
    }
}`
    },

    charskill: {
      title: '캐릭터별 스킬 (E/R) 구현',
      description: '각 캐릭터는 전술 스킬(E)과 궁극기(R)를 가짐. CharacterData.createAbilities()가 캐릭터 ID로 Ability 배열을 생성하고, 쿨다운/지속시간/데미지를 정의합니다.',
      language: 'java',
      code: `// CharacterData.java - 스킬 생성
public static Ability[] createAbilities(String characterId) {
    switch (characterId) {
        case "raven":
            return new Ability[] {
                new Ability("raven_basic", "권총", "빠른 연사",
                    BASIC, 0.3f, 0f, 600f, 15f),
                new Ability("raven_dash", "대쉬", "순간 이동",
                    TACTICAL, 8f, 0.5f, 0f, 0f),
                new Ability("raven_overcharge", "과충전", "공속 증가",
                    ULTIMATE, 20f, 6f, 0f, 0f)
            };
        case "piper":
            return new Ability[] {
                new Ability("piper_basic", "저격", "장거리",
                    BASIC, 1.2f, 0f, 1000f, 80f),
                new Ability("piper_mark", "적 표시", "마킹",
                    TACTICAL, 8f, 5f, 800f, 0f)
            };
    }
}

// GamePanel.java - 전술 스킬 (E키)
private void useTacticalSkill() {
    if (abilities[1].canUse()) {
        abilities[1].activate();
        
        if ("raven_dash".equals(abilities[1].id)) {
            ravenDashRemaining = abilities[1].duration;
        }
        
        sendSkillUse(1, "TACTICAL");
        addLocalEffect(abilities[1]);
    }
}`
    }
  };

  const categories = [
    { id: 'gameloop', name: '게임 루프', icon: '🔄' },
    { id: 'network', name: '네트워크', icon: '🌐' },
    { id: 'collision', name: '충돌 감지', icon: '💥' },
    { id: 'skill', name: '스킬 시스템', icon: '⚡' },
    { id: 'renderer', name: '렌더링', icon: '🎨' },
    { id: 'mapeditor', name: '맵 에디터', icon: '🗺️' },
    { id: 'scoreboard', name: '스코어보드', icon: '📊' },
    { id: 'server', name: '서버', icon: '🖥️' },
    { id: 'sprite', name: '스프라이트', icon: '🎭' },
    { id: 'skilleffect', name: '스킬 이펙트', icon: '✨' },
    { id: 'mapkeys', name: '맵 F3-F6', icon: '🔧' },
    { id: 'mapselect', name: '맵 랜덤', icon: '🎲' },
    { id: 'charskill', name: '캐릭터 스킬', icon: '🎯' }
  ];

  const relatedConcepts = {
    gameloop: ['Timer', 'SwingUtilities', 'Repaint', 'DeltaTime'],
    network: ['TCP Socket', 'DataOutputStream', 'Thread', 'Consumer'],
    collision: ['Math.sqrt', 'Circle collision', 'AABB', 'Hitbox'],
    skill: ['Enum', 'Factory Pattern', 'Cooldown', 'Switch-case'],
    renderer: ['Graphics2D', 'BufferedImage', 'Double buffering', 'Camera'],
    mapeditor: ['KeyListener', 'JSON', 'File I/O', 'Grid system'],
    scoreboard: ['HashMap', 'Entry', 'Team sorting', 'Real-time update'],
    server: ['ServerSocket', 'ConcurrentHashMap', 'Timer', 'Broadcast'],
    sprite: ['BufferedImage[]', 'SpriteAnimation', '150ms/frame', '4방향'],
    skilleffect: ['SkillEffect', 'Graphics2D', 'Alpha', 'drawSelf()'],
    mapkeys: ['F3 debug', 'F4 edit', 'F5 save', 'F6 cycle'],
    mapselect: ['Random', 'ROUND_START', 'selectedMap', 'Broadcast'],
    charskill: ['Raven E/R', 'Piper E/R', 'Tech E/R', 'General E/R']
  };

  const performanceTips = [
    { icon: '⚡', tip: '60 FPS 유지를 위해 16ms 타이머 사용' },
    { icon: '🔄', tip: '플레이어 보간으로 부드러운 움직임' },
    { icon: '📡', tip: '위치 변경 시에만 패킷 전송 (네트워크 최적화)' },
    { icon: '💾', tip: 'ConcurrentHashMap으로 멀티스레드 안전성' },
    { icon: '🎯', tip: '200ms 쿨다운으로 중복 피격 방지' },
    { icon: '🗺️', tip: 'walkableGrid 배열로 O(1) 충돌 체크' }
  ];

  return (
    <section className="mb-16">
      <h2 className="text-3xl font-bold text-slate-800 mb-4">💻 코드 분석</h2>
      <p className="text-slate-600 mb-8">
        실제 프로젝트에서 사용된 Java 코드입니다. MVC 패턴, Manager 클래스, 네트워크 프로토콜 등 핵심 구현을 확인할 수 있습니다.
      </p>

      {/* 코드 카테고리 선택 */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        {categories.map((category) => (
          <button
            key={category.id}
            onClick={() => setSelectedCode(category.id)}
            className={`p-4 rounded-lg border-2 transition-all ${
              selectedCode === category.id
                ? 'border-blue-500 bg-blue-50'
                : 'border-slate-200 bg-white hover:border-blue-300'
            }`}
          >
            <div className="text-3xl mb-2">{category.icon}</div>
            <div className="text-sm font-semibold text-slate-700">{category.name}</div>
          </button>
        ))}
      </div>

      {/* 선택된 코드 표시 */}
      <div className="bg-white rounded-lg border-2 border-slate-200 overflow-hidden">
        <div className="bg-slate-800 px-6 py-4 flex items-center justify-between">
          <div>
            <h3 className="text-xl font-bold text-white mb-1">
              {codeExamples[selectedCode].title}
            </h3>
            <p className="text-slate-300 text-sm">
              {codeExamples[selectedCode].description}
            </p>
          </div>
          <span className="px-3 py-1 bg-blue-500 text-white text-xs font-semibold rounded">
            {codeExamples[selectedCode].language.toUpperCase()}
          </span>
        </div>

        {/* 코드 블록 */}
        <div className="p-6 bg-slate-900 overflow-x-auto">
          <pre className="text-sm text-slate-100 font-mono leading-relaxed">
            <code>{codeExamples[selectedCode].code}</code>
          </pre>
        </div>

        {/* 관련 개념 */}
        <div className="px-6 py-4 bg-slate-50 border-t border-slate-200">
          <h4 className="text-sm font-semibold text-slate-700 mb-3">관련 개념</h4>
          <div className="flex flex-wrap gap-2">
            {relatedConcepts[selectedCode].map((concept) => (
              <span
                key={concept}
                className="px-3 py-1 bg-blue-100 text-blue-700 text-xs font-medium rounded-full"
              >
                {concept}
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* 성능 최적화 팁 */}
      <div className="mt-8 bg-gradient-to-br from-blue-50 to-purple-50 rounded-lg p-6 border-2 border-blue-200">
        <h3 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
          <span>⚡</span> 성능 최적화 팁
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {performanceTips.map((tip, index) => (
            <div key={index} className="flex items-start gap-3 bg-white p-4 rounded-lg">
              <span className="text-2xl">{tip.icon}</span>
              <p className="text-sm text-slate-700">{tip.tip}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default CodeSection;