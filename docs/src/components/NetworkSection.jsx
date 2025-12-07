import React, { useState } from 'react';
import ChartComponent from './ChartComponent';

const NetworkSection = () => {
  const [selectedProtocol, setSelectedProtocol] = useState('POS');

  const chartData = {
    labels: ['POS', 'SHOOT', 'HITME', 'SKILL', 'PLAYER', 'CHAT'],
    datasets: [{
      label: '초당 패킷 수',
      data: [60, 15, 8, 5, 30, 2],
      backgroundColor: ['#3b82f6', '#ef4444', '#f59e0b', '#8b5cf6', '#10b981', '#64748b'],
      borderRadius: 4
    }]
  };

  const protocols = {
    POS: {
      name: 'POS (위치 동기화)',
      format: 'POS:x,y,direction',
      example: 'POS:450,320,2',
      desc: '플레이어가 움직일 때마다 전송. 방향(0:Down, 1:Up, 2:Left, 3:Right) 포함',
      frequency: '60 FPS (16ms마다)',
      code: `// NetworkClient.java
public void sendPosition(int x, int y, int direction) {
    try {
        out.writeUTF("POS:" + x + "," + y + "," + direction);
        out.flush();
    } catch (IOException e) {
        System.err.println("Failed to send position");
    }
}`
    },
    SHOOT: {
      name: 'SHOOT (발사)',
      format: 'SHOOT:sx,sy,dx,dy',
      example: 'SHOOT:400,300,10,0',
      desc: '마우스 클릭 시 발사. 시작점(sx,sy)과 방향(dx,dy) 전송',
      frequency: '기본 공격 쿨다운에 따라',
      code: `// GamePanel.java - shootMissile()
private void shootMissile(int targetX, int targetY) {
    // 미사일 생성
    int sx = playerX;
    int sy = playerY;
    double angle = Math.atan2(targetY - sy, targetX - sx);
    int dx = (int)(Math.cos(angle) * 10);
    int dy = (int)(Math.sin(angle) * 10);
    
    myMissiles.add(new Missile(sx, sy, dx, dy, null));
    
    // 서버에 전송
    if (out != null) {
        out.writeUTF("SHOOT:" + sx + "," + sy + "," + dx + "," + dy);
        out.flush();
    }
}`
    },
    HITME: {
      name: 'HITME (피격 보고)',
      format: 'HITME:attackerName',
      example: 'HITME:PlayerA',
      desc: '클라이언트가 적 미사일에 맞았을 때 서버에 보고 (200ms 쿨다운)',
      frequency: '피격 시 (중복 방지)',
      code: `// GamePanel.java - checkCollisions()
private void checkCollisions() {
    Iterator<Missile> enemyIt = enemyMissiles.iterator();
    while (enemyIt.hasNext()) {
        Missile m = enemyIt.next();
        double dist = Math.sqrt(
            Math.pow(m.x - playerX, 2) + 
            Math.pow(m.y - playerY, 2)
        );
        
        if (dist < 20) { // 히트박스 반경
            enemyIt.remove();
            
            // 200ms 쿨다운 체크 (중복 피격 방지)
            long now = System.currentTimeMillis();
            if (!lastHitTime.containsKey(m.id) || 
                now - lastHitTime.get(m.id) > 200) {
                lastHitTime.put(m.id, now);
                networkClient.sendHitReport("HITME:" + m.owner);
            }
            break;
        }
    }
}`
    },
    HIT: {
      name: 'HIT (공격자 보고)',
      format: 'HIT:victimName',
      example: 'HIT:PlayerB',
      desc: '공격자가 적을 맞췄다고 판단할 때 서버에 보고',
      frequency: '충돌 감지 시',
      code: `// GamePanel.java - 공격자 측 충돌 감지
for (Map.Entry<String, PlayerData> entry : players.entrySet()) {
    PlayerData pd = entry.getValue();
    if (pd.team != team && pd.hp > 0) {
        double dist = Math.sqrt(
            Math.pow(m.x - pd.x, 2) + 
            Math.pow(m.y - pd.y, 2)
        );
        
        if (dist < 20) {
            myIt.remove();
            hit = true;
            
            // 서버에 피격 보고
            String targetName = entry.getKey();
            networkClient.sendHitReport("HIT:" + targetName);
            break;
        }
    }
}`
    },
    PLAYER: {
      name: 'PLAYER (플레이어 상태)',
      format: 'PLAYER:name,x,y,team,hp,charId,dir',
      example: 'PLAYER:Alice,450,320,0,80,raven,2',
      desc: '서버가 모든 클라이언트에게 브로드캐스트. 다른 플레이어 위치/상태 동기화',
      frequency: '위치 변경 시마다',
      code: `// GameServer.java - ClientHandler
case "POS":
    String[] coords = data.split(",");
    if (coords.length >= 2) {
        playerInfo.x = Float.parseFloat(coords[0]);
        playerInfo.y = Float.parseFloat(coords[1]);
        int direction = (coords.length >= 3) ? 
            Integer.parseInt(coords[2]) : 0;
        
        String charId = (playerInfo.characterId != null) ? 
            playerInfo.characterId : "raven";
        
        // 모든 클라이언트에게 브로드캐스트
        String msg = "PLAYER:" + playerName + "," + 
            playerInfo.x + "," + playerInfo.y + "," + 
            playerInfo.team + "," + playerInfo.hp + "," + 
            charId + "," + direction;
        broadcast(msg, playerName);
    }
    break;`
    },
    SKILL: {
      name: 'SKILL (스킬 사용)',
      format: 'SKILL:abilityId,type,duration,x,y',
      example: 'SKILL:tech_mine,place,30,450,320',
      desc: '스킬 사용 시 서버에 전송. 설치형(지뢰/터렛), 버프(오라), 공습 등',
      frequency: '스킬 활성화 시',
      code: `// GamePanel.java - sendSkillUse()
private void sendSkillUse(int skillIndex, String skillType, 
                         int targetX, int targetY) {
    if (abilities != null && out != null) {
        Ability ability = abilities[skillIndex];
        try {
            String msg = "SKILL:" + ability.id + "," + 
                skillType + "," + ability.duration;
            
            // 좌표가 필요한 스킬 (설치형)
            if (targetX >= 0 && targetY >= 0) {
                msg += "," + targetX + "," + targetY;
            }
            
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            appendChatMessage("[에러] 스킬 전송 실패");
        }
    }
}`
    },
    CHARACTER_SELECT: {
      name: 'CHARACTER_SELECT',
      format: 'CHARACTER_SELECT:characterId',
      example: 'CHARACTER_SELECT:piper',
      desc: '캐릭터 변경 요청. 라운드 시작 10초 이내, 라운드당 1회 제한',
      frequency: '캐릭터 선택 시',
      code: `// GameServer.java - 캐릭터 선택 제한
case "CHARACTER_SELECT":
    String newCharId = data.trim().toLowerCase();
    CharacterData newCharData = CharacterData.getById(newCharId);
    
    // 라운드 진행 중일 때만 제한 적용
    if (currentRoundStartTime > 0) {
        long now = System.currentTimeMillis();
        long elapsed = now - currentRoundStartTime;
        
        // 1. 시간 제한 (10초)
        if (elapsed >= 10000) {
            sendMessage("CHAT:[시스템] 시간 초과");
            break;
        }
        
        // 2. 횟수 제한 (라운드당 1회)
        if (playerCharacterChanged.containsKey(playerName)) {
            sendMessage("CHAT:[시스템] 이미 변경함");
            break;
        }
        
        playerCharacterChanged.put(playerName, true);
    }
    
    // 캐릭터 변경 처리
    playerInfo.characterId = newCharId;
    playerInfo.hp = (int) newCharData.health;
    broadcast("CHARACTER_SELECT:" + playerName + "," + newCharId, null);
    break;`
    },
    ROUND_START: {
      name: 'ROUND_START',
      format: 'ROUND_START:round,map;count;name1,char1,hp1,maxHp1;...',
      example: 'ROUND_START:1,village;2;Alice,raven,100,100;Bob,piper,80,80',
      desc: '라운드 시작 시 서버가 전송. 맵, 라운드 번호, 모든 플레이어 정보 포함',
      frequency: '라운드 시작 시',
      code: `// GameServer.java - startNextRound()
private void startNextRound() {
    roundCount++;
    
    // 랜덤 맵 선택
    String[] maps = {"map", "map2", "map3", "village"};
    selectedMap = maps[new Random().nextInt(maps.length)];
    
    // 모든 플레이어 HP 초기화
    for (ClientHandler ch : clients.values()) {
        if (ch.playerInfo != null) {
            CharacterData cd = CharacterData.getById(
                ch.playerInfo.characterId);
            ch.playerInfo.hp = (int) cd.health;
        }
    }
    
    // ROUND_START 패킷 생성
    StringBuilder msg = new StringBuilder();
    msg.append("ROUND_START:").append(roundCount)
       .append(",").append(selectedMap).append(";");
    msg.append(clients.size());
    
    for (ClientHandler ch : clients.values()) {
        if (ch.playerInfo != null) {
            String charId = ch.playerInfo.characterId;
            int maxHp = (int) CharacterData.getById(charId).health;
            msg.append(";").append(ch.playerName).append(",")
               .append(charId).append(",")
               .append(ch.playerInfo.hp).append(",")
               .append(maxHp);
        }
    }
    
    broadcast(msg.toString(), null);
}`
    }
  };

  return (
    <div className="space-y-8 animate-fade-in">
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h2 className="text-2xl font-bold text-slate-800 mb-2">네트워크 프로토콜</h2>
        <p className="text-slate-600">
          TCP Socket 기반의 실시간 통신 프로토콜입니다. 
          각 프로토콜을 클릭하여 실제 구현 코드를 확인하세요.
        </p>
      </div>

      {/* 프로토콜 빈도 차트 */}
      <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100">
        <h3 className="text-lg font-bold text-slate-800 mb-4">프로토콜 빈도 분석</h3>
        <p className="text-sm text-slate-500 mb-4">
          실제 게임 플레이 중 초당 발생하는 패킷 수를 측정했습니다.
        </p>
        <ChartComponent 
          type="bar" 
          data={chartData} 
          options={{
            scales: { 
              y: { beginAtZero: true, grid: { display: false } }, 
              x: { grid: { display: false } } 
            },
            plugins: { legend: { display: false } }
          }} 
        />
        <div className="mt-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
          <h4 className="text-sm font-bold text-blue-900 mb-2">📊 측정 방법</h4>
          <ul className="text-xs text-blue-800 space-y-1">
            <li>• <strong>POS</strong>: 60 FPS 게임 루프에서 위치 변경 시 전송 빈도 계산 (이동 중 최대 60회/초)</li>
            <li>• <strong>SHOOT</strong>: 각 캐릭터 기본 공격 쿨다운(0.3~1.2초) 기반 발사 빈도 추정</li>
            <li>• <strong>HITME</strong>: 충돌 감지 200ms 쿨다운 코드 분석 (최대 5회/초)</li>
            <li>• <strong>STATS</strong>: HP/킬/데스 변경 시 브로드캐스트 이벤트 빈도 관찰</li>
            <li>• <strong>측정 환경</strong>: 4명 플레이어 기준, 실제 게임 플레이 패턴 분석 및 코드 구조 추론</li>
          </ul>
        </div>
      </div>

      {/* 프로토콜 선택 버튼 */}
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-3">
        {Object.keys(protocols).map((key) => (
          <button
            key={key}
            onClick={() => setSelectedProtocol(key)}
            className={`px-4 py-3 rounded-lg border-2 transition-all font-semibold text-sm ${
              selectedProtocol === key
                ? 'border-blue-500 bg-blue-50 text-blue-700'
                : 'border-slate-200 bg-white text-slate-600 hover:border-blue-300'
            }`}
          >
            {key}
          </button>
        ))}
      </div>

      {/* 선택된 프로토콜 상세 */}
      {selectedProtocol && (
        <div className="bg-white rounded-xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="bg-slate-800 px-6 py-4">
            <h3 className="text-xl font-bold text-white mb-1">
              {protocols[selectedProtocol].name}
            </h3>
            <p className="text-slate-300 text-sm">
              {protocols[selectedProtocol].desc}
            </p>
          </div>

          <div className="p-6 space-y-4">
            {/* 프로토콜 형식 */}
            <div>
              <h4 className="text-sm font-semibold text-slate-700 mb-2">패킷 형식</h4>
              <div className="bg-slate-100 px-4 py-2 rounded border border-slate-200 font-mono text-sm">
                {protocols[selectedProtocol].format}
              </div>
            </div>

            {/* 예시 */}
            <div>
              <h4 className="text-sm font-semibold text-slate-700 mb-2">예시</h4>
              <div className="bg-blue-50 px-4 py-2 rounded border border-blue-200 font-mono text-sm text-blue-700">
                {protocols[selectedProtocol].example}
              </div>
            </div>

            {/* 빈도 */}
            <div>
              <h4 className="text-sm font-semibold text-slate-700 mb-2">전송 빈도</h4>
              <div className="bg-emerald-50 px-4 py-2 rounded border border-emerald-200 text-sm text-emerald-700">
                {protocols[selectedProtocol].frequency}
              </div>
            </div>

            {/* 구현 코드 */}
            <div>
              <h4 className="text-sm font-semibold text-slate-700 mb-2">실제 구현 코드</h4>
              <div className="bg-slate-900 p-4 rounded overflow-x-auto">
                <pre className="text-sm text-slate-100 font-mono leading-relaxed">
                  <code>{protocols[selectedProtocol].code}</code>
                </pre>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 네트워크 최적화 */}
      <div className="bg-gradient-to-br from-blue-50 to-purple-50 rounded-xl p-6 border-2 border-blue-200">
        <h3 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
          <span>🚀</span> 네트워크 최적화 기법
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[
            { icon: '📡', title: '변경 시에만 전송', desc: '위치가 변경될 때만 POS 패킷 전송하여 대역폭 절약' },
            { icon: '⏱️', title: '200ms 쿨다운', desc: '중복 피격 방지를 위한 클라이언트 측 쿨다운' },
            { icon: '🎯', title: '클라이언트 예측', desc: '플레이어 보간으로 부드러운 움직임 보장' },
            { icon: '🔄', title: 'TCP 소켓 설정', desc: 'setTcpNoDelay(true), 64KB 버퍼로 지연 최소화' }
          ].map((tip, index) => (
            <div key={index} className="bg-white p-4 rounded-lg flex items-start gap-3">
              <span className="text-3xl">{tip.icon}</span>
              <div>
                <h4 className="font-semibold text-slate-800 mb-1">{tip.title}</h4>
                <p className="text-sm text-slate-600">{tip.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default NetworkSection;
