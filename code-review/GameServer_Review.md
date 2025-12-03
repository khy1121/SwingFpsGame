# GameServer.java 코드 리뷰

## 📋 파일 개요
- **경로**: `src/com/fpsgame/server/GameServer.java`
- **역할**: 멀티플레이어 게임 서버 로직 총괄
- **라인 수**: 1,193줄
- **주요 기능**: 네트워크 통신, 게임 상태 동기화, 충돌 감지, 라운드 관리, 스킬 처리
- **최근 업데이트**: 캐릭터 선택 제한 강화 (3단계 검증), 맵 랜덤 선택, HP 동기화 개선

---

## 🎯 주요 기능

### 1. 서버 아키텍처
```java
public class GameServer {
    private final ServerSocket serverSocket;
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final int MAX_PLAYERS = 4;
    
    // 게임 오브젝트
    private final Map<Integer, PlacedObject> placedObjects = new ConcurrentHashMap<>();
    private final Map<String, ActiveAura> activeAuras = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledStrike> scheduledStrikes = new ConcurrentHashMap<>();
    
    // 라운드 시스템
    private int roundCount = 0;
    private int redWins = 0;
    private int blueWins = 0;
    private boolean roundEnded = false;
    private static final int MAX_WINS = 2; // 3판 2선승
    
    // 캐릭터 선택 제한
    private long currentRoundStartTime = 0;
    private final Map<String, Boolean> playerCharacterChanged = new ConcurrentHashMap<>();
}
```
- **동시성 안전**: `ConcurrentHashMap` 사용으로 멀티스레드 안전 보장
- **최대 4명**: `GameConstants.MAX_PLAYERS` 제한
- **3판 2선승**: `MAX_WINS = 2` (2번 먼저 이기는 팀 우승)

### 2. 내부 클래스 (게임 오브젝트)

#### PlacedObject (설치 오브젝트)
```java
static class PlacedObject {
    int id;
    String type; // "tech_mine", "tech_turret"
    String owner;
    int team;
    int x, y;
    int hp;
    int maxHp;
    long createdAt;
}
```
- **지뢰 (tech_mine)**: HP 40, 밟으면 60 데미지, 즉시 폭발
- **터렛 (tech_turret)**: HP 100, 자동 공격 (사거리 180, 0.9초 간격, 20 데미지)

#### ActiveAura (General 오라)
```java
static class ActiveAura {
    String ownerName;
    int ownerTeam;
    int x, y;
    float radius; // 150 픽셀
    long expiresAt;
    Set<String> currentlyBuffed = new HashSet<>();
}
```
- **범위 버프**: 150 픽셀 반경 내 팀원에게 이동속도 +10%, 공격속도 +15%
- **동적 추적**: 소유자 위치를 따라 이동
- **자동 관리**: 범위 진입/이탈 시 버프 적용/제거

#### ScheduledStrike (General 에어스트라이크)
```java
static class ScheduledStrike {
    int id;
    String owner;
    int team;
    int targetX, targetY;
    long impactAt; // 호출 후 2초 뒤 임팩트
}
```
- **2초 지연**: 마커 표시 후 2초 뒤 폭발
- **범위 데미지**: 120 픽셀 반경, 50 데미지
- **킬 크레딧**: 호출자가 킬 획득
- **오브젝트 파괴**: 범위 내 터렛/지뢰 전부 파괴

### 3. 클라이언트 연결 관리
```java
public void start() {
    while (running) {
        try {
            Socket clientSocket = serverSocket.accept();
            
            // 서버 가득 참 체크
            if (clients.size() >= GameConstants.MAX_PLAYERS) {
                DataOutputStream tmpOut = new DataOutputStream(clientSocket.getOutputStream());
                tmpOut.writeUTF("서버가 가득 찼습니다. 나중에 다시 시도하세요.");
                tmpOut.flush();
                clientSocket.close();
                continue;
            }
            
            // 새 클라이언트 핸들러 생성
            ClientHandler handler = new ClientHandler(clientSocket);
            new Thread(handler).start();
        } catch (IOException e) {
            if (running) e.printStackTrace();
        }
    }
}
```
- **전용 스레드**: 각 클라이언트마다 별도 스레드
- **용량 제한**: 최대 4명, 초과 시 연결 거부
- **소켓 최적화**: TCP_NODELAY, KeepAlive, 버퍼 크기 64KB

### 4. 프로토콜 처리 (ClientHandler)

#### JOIN (입장)
```java
case "JOIN":
    // JOIN:playerName:characterId 형식 파싱
    String[] joinParts = data.split(":");
    playerName = joinParts[0];
    
    // 1. 플레이어 이름 검증
    if (playerName == null || playerName.trim().isEmpty()) {
        sendMessage("CHAT:[시스템] 유효하지 않은 플레이어 이름");
        socket.close();
        return;
    }
    
    // 2. 캐릭터 선택 필수 검증
    if (joinParts.length <= 1 || joinParts[1].trim().isEmpty()) {
        sendMessage("CHAT:[시스템] 캐릭터를 선택한 후 입장해주세요");
        socket.close();
        return;
    }
    
    // 3. 캐릭터 ID 검증
    String joinCharId = joinParts[1].trim().toLowerCase();
    CharacterData cd = CharacterData.getById(joinCharId);
    if (cd == null) {
        sendMessage("CHAT:[시스템] 잘못된 캐릭터 ID: " + joinCharId);
        socket.close();
        return;
    }
    
    // 4. 플레이어 정보 초기화
    playerInfo = new Protocol.PlayerInfo(clients.size(), playerName);
    playerInfo.characterId = joinCharId;
    playerInfo.hp = (int) cd.health;
    clients.put(playerName, this);
    
    // 5. 브로드캐스트
    sendMessage("WELCOME: 서버에 연결되었습니다");
    broadcast("CHAT:" + playerName + " 님이 게임에 참가했습니다!", playerName);
    broadcastStats(playerName, playerInfo);
    broadcastTeamRoster();
```
**3단계 입장 검증**: 이름 → 캐릭터 선택 → 캐릭터 유효성

#### CHARACTER_SELECT (캐릭터 변경)
```java
case "CHARACTER_SELECT":
    String newCharId = data.trim().toLowerCase();
    CharacterData newCharData = CharacterData.getById(newCharId);
    
    // 캐릭터 ID 검증
    if (newCharData == null) {
        sendMessage("CHAT:[시스템] 잘못된 캐릭터 ID: " + data);
        break;
    }
    
    // 라운드 진행 중일 때만 제한 적용 (로비에서는 무제한)
    if (currentRoundStartTime > 0) {
        long now = System.currentTimeMillis();
        long elapsed = now - currentRoundStartTime;
        
        // 1. 시간 제한 (10초) - 엄격하게 체크
        if (elapsed >= 10000) {
            sendMessage("CHAT:[시스템] 라운드 시작 후 10초가 지나 변경 불가 (경과: " + (elapsed/1000) + "초)");
            System.out.println("[CHARACTER_SELECT_DENIED] " + playerName + " - Time limit exceeded: " + elapsed + "ms");
            break;
        }
        
        // 2. 횟수 제한 (라운드당 1회)
        if (playerCharacterChanged.containsKey(playerName)) {
            sendMessage("CHAT:[시스템] 이번 라운드에 이미 변경했습니다 (1회 제한)");
            System.out.println("[CHARACTER_SELECT_DENIED] " + playerName + " - Already changed");
            break;
        }
        
        // 3. 라운드 종료 상태 체크
        if (roundEnded) {
            sendMessage("CHAT:[시스템] 라운드 종료되어 변경 불가");
            break;
        }
        
        // 변경 기록
        playerCharacterChanged.put(playerName, true);
        System.out.println("[CHARACTER_SELECT_ALLOWED] " + playerName + " - Elapsed: " + elapsed + "ms");
    }
    
    // 캐릭터 변경 처리
    playerInfo.characterId = newCharId;
    playerInfo.hp = (int) newCharData.health;
    
    // 변경 성공 알림 + HP 동기화
    broadcast("CHARACTER_SELECT:" + playerName + "," + newCharId, null);
    broadcastStats(playerName, playerInfo);
    broadcast("CHAT:" + playerName + " 님이 " + newCharData.name + " 선택!", null);
```
**3단계 검증 시스템**:
1. **시간 제한**: 라운드 시작 후 10초 이내 (엄격)
2. **횟수 제한**: 라운드당 1회만 변경 가능
3. **상태 체크**: 라운드 종료 시 변경 불가

**로비에서는 무제한**: `currentRoundStartTime > 0`일 때만 제한 적용

#### HIT / HITME (피격 처리)
```java
// HIT: 공격자가 "내가 적을 맞췄다" 보고
case "HIT":
    String hitPlayer = data;
    ClientHandler target = clients.get(hitPlayer);
    if (target != null && target.playerInfo != null) {
        // 스폰 보호 중이거나 이미 사망 상태면 무시
        long now = System.currentTimeMillis();
        if (now < target.spawnProtectedUntil || target.playerInfo.hp <= 0) {
            break;
        }
        
        // 캐릭터별 데미지 적용
        int dmg = resolveBasicDamage(this.playerInfo.characterId);
        target.playerInfo.hp -= dmg;
        
        if (target.playerInfo.hp <= 0) {
            target.playerInfo.hp = 0;
            sendMessage("KILL:" + hitPlayer);
            this.playerInfo.kills++;
            target.playerInfo.deaths++;
            broadcast("CHAT:" + playerName + " 님이 " + hitPlayer + " 처치!", null);
        }
        
        broadcastStats(target.playerName, target.playerInfo);
        checkRoundEnd();
    }
    break;

// HITME: 피해자가 "나는 맞았다" 보고 (더 신뢰할 수 있음)
case "HITME":
    String shooterName = data;
    boolean isTurretDamage = shooterName.startsWith("TURRET:");
    String actualShooter = isTurretDamage ? shooterName.substring(7) : shooterName;
    ClientHandler shooter = clients.get(actualShooter);
    
    if (playerInfo != null) {
        long now = System.currentTimeMillis();
        if (now < spawnProtectedUntil || playerInfo.hp <= 0) {
            break;
        }
        
        // 터렛 데미지 20, 일반 공격은 캐릭터별
        int dmg = isTurretDamage ? 20 : resolveBasicDamage(shooter.playerInfo.characterId);
        playerInfo.hp -= dmg;
        
        if (playerInfo.hp <= 0) {
            playerInfo.hp = 0;
            if (shooter != null) {
                shooter.playerInfo.kills++;
                playerInfo.deaths++;
                shooter.sendMessage("KILL:" + playerName);
                String killMsg = isTurretDamage ? 
                    actualShooter + " 님의 터렛이 " + playerName + " 처치!" :
                    actualShooter + " 님이 " + playerName + " 처치!";
                broadcast("CHAT:" + killMsg, null);
            }
        }
        
        checkRoundEnd();
    }
    break;
```
- **HIT**: 공격자 보고 (클라이언트 측 충돌 감지)
- **HITME**: 피해자 보고 (서버 측 신뢰)
- **스폰 보호**: 리스폰 후 3초간 무적 (`spawnProtectedUntil`)
- **캐릭터별 데미지**: `resolveBasicDamage()` 메서드 사용

#### RESPAWN (리스폰)
```java
case "RESPAWN":
    String[] resp = data.split(",");
    if (resp.length >= 2) {
        playerInfo.x = Float.parseFloat(resp[0]);
        playerInfo.y = Float.parseFloat(resp[1]);
        
        // 캐릭터별 최대 HP로 부활
        if (playerInfo.characterId != null) {
            playerInfo.hp = (int) CharacterData.getById(playerInfo.characterId).health;
        } else {
            playerInfo.hp = GameConstants.MAX_HP;
        }
        
        // 3초 스폰 보호
        spawnProtectedUntil = System.currentTimeMillis() + 3000;
        
        broadcastStats(playerName, playerInfo);
        broadcast("PLAYER:" + playerName + "," + playerInfo.x + "," + playerInfo.y + "," + playerInfo.team + "," + playerInfo.hp + "," + characterId, playerName);
        broadcast("CHAT:" + playerName + " 님이 리스폰!", null);
    }
    break;
```

### 5. 스킬 시스템

#### 지뢰 설치 (tech_mine)
```java
if ("tech_mine".equals(abilityId) && targetX >= 0 && targetY >= 0) {
    int id = nextPlacedObjectId.getAndIncrement();
    PlacedObject obj = new PlacedObject(id, "tech_mine", user, playerInfo.team, targetX, targetY, 40);
    placedObjects.put(id, obj);
    
    // 모든 클라이언트에게 브로드캐스트
    String placeMsg = "PLACE:" + id + "," + obj.type + "," + obj.x + "," + obj.y + "," 
                    + obj.hp + "," + obj.maxHp + "," + obj.owner + "," + obj.team;
    for (ClientHandler ch : clients.values()) {
        ch.sendMessage(placeMsg);
    }
}

// 지뢰 밟기 체크 (POS 수신 시)
List<Integer> minesToExplode = new ArrayList<>();
for (PlacedObject obj : placedObjects.values()) {
    if ("tech_mine".equals(obj.type) && obj.hp > 0 && obj.team != playerInfo.team) {
        double dist = Math.sqrt(Math.pow(playerInfo.x - obj.x, 2) + Math.pow(playerInfo.y - obj.y, 2));
        if (dist < 24) { // 밟은 판정
            minesToExplode.add(obj.id);
        }
    }
}
for (int mineId : minesToExplode) {
    PlacedObject mine = placedObjects.remove(mineId);
    if (mine != null) {
        playerInfo.hp -= 60; // 지뢰 데미지
        if (playerInfo.hp <= 0) {
            playerInfo.deaths++;
            broadcast("CHAT:" + playerName + " 님이 지뢰를 밟아 사망!", null);
            checkRoundEnd();
        }
        broadcastStats(playerName, playerInfo);
        broadcast("OBJ_DESTROY:" + mineId, null);
    }
}
```

#### 터렛 자동 공격 (tech_turret)
```java
// 터렛 자동 공격 타이머 (900ms 간격)
turretAttackTimer = new Timer(true);
turretAttackTimer.scheduleAtFixedRate(new TimerTask() {
    @Override
    public void run() {
        checkTurretTargets();
    }
}, 1000, TURRET_ATTACK_INTERVAL);

private void checkTurretTargets() {
    for (PlacedObject obj : placedObjects.values()) {
        if (!"tech_turret".equals(obj.type) || obj.hp <= 0) {
            continue;
        }
        
        for (ClientHandler ch : clients.values()) {
            // 적 팀만 공격 (소유자 본인 제외)
            if (ch.playerInfo == null || ch.playerInfo.hp <= 0 || ch.playerInfo.team == obj.team) {
                continue;
            }
            if (ch.playerName.equals(obj.owner)) {
                continue;
            }
            
            double dist = Math.sqrt(Math.pow(obj.x - ch.playerInfo.x, 2) + Math.pow(obj.y - ch.playerInfo.y, 2));
            if (dist <= TURRET_RANGE) { // 180 픽셀
                // 터렛 미사일 발사 브로드캐스트
                String shootMsg = "TURRET_SHOOT:" + obj.id + "," + (int)ch.playerInfo.x + "," 
                                + (int)ch.playerInfo.y + "," + ch.playerName + "," + obj.owner;
                for (ClientHandler c : clients.values()) {
                    c.sendMessage(shootMsg);
                }
                break; // 한 번에 한 명만 공격
            }
        }
    }
}
```
- **자동 공격**: 0.9초 간격으로 사거리 180 픽셀 내 적 자동 공격
- **소유자 안전**: 본인은 절대 공격하지 않음 (팀 체크 + 이름 체크)
- **데미지**: 20 고정 (터렛 미사일)

#### 오라 버프 (gen_aura)
```java
private void updateAuraBuffs() {
    long now = System.currentTimeMillis();
    
    // 만료된 오라 제거
    activeAuras.entrySet().removeIf(e -> {
        ActiveAura aura = e.getValue();
        if (now >= aura.expiresAt) {
            // 버프 받던 플레이어들에게 제거 알림
            for (String buffedName : aura.currentlyBuffed) {
                ClientHandler ch = clients.get(buffedName);
                if (ch != null) {
                    ch.sendMessage("UNBUFF:gen_aura");
                }
            }
            return true;
        }
        return false;
    });
    
    // 현재 활성 오라에 대해 범위 체크
    for (ActiveAura aura : activeAuras.values()) {
        // 오라 소유자의 현재 위치로 업데이트 (동적 추적)
        ClientHandler owner = clients.get(aura.ownerName);
        if (owner != null && owner.playerInfo != null) {
            aura.x = (int) owner.playerInfo.x;
            aura.y = (int) owner.playerInfo.y;
        }
        
        Set<String> nowInRange = new HashSet<>();
        
        // 모든 플레이어 체크
        for (ClientHandler ch : clients.values()) {
            if (ch.playerInfo == null || ch.playerInfo.hp <= 0) continue;
            if (ch.playerInfo.team != aura.ownerTeam) continue; // 같은 팀만
            if (ch.playerName.equals(aura.ownerName)) continue; // 본인 제외
            
            int dx = (int) ch.playerInfo.x - aura.x;
            int dy = (int) ch.playerInfo.y - aura.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            
            if (dist <= aura.radius) {
                nowInRange.add(ch.playerName);
                
                // 새로 들어온 플레이어에게 버프 적용
                if (!aura.currentlyBuffed.contains(ch.playerName)) {
                    long remaining = Math.max(0, aura.expiresAt - now);
                    float dur = remaining / 1000f;
                    // BUFF:targetName,abilityId,moveSpeedMult,attackSpeedMult,duration
                    String buffMsg = "BUFF:" + ch.playerName + ",gen_aura,1.10,1.15," + dur;
                    ch.sendMessage(buffMsg);
                }
            }
        }
        
        // 범위를 벗어난 플레이어에게 버프 제거
        for (String prevBuffed : aura.currentlyBuffed) {
            if (!nowInRange.contains(prevBuffed)) {
                ClientHandler ch = clients.get(prevBuffed);
                if (ch != null) {
                    ch.sendMessage("UNBUFF:gen_aura");
                }
            }
        }
        
        aura.currentlyBuffed = nowInRange;
    }
}
```
- **동적 추적**: 소유자 위치를 따라 오라 중심 이동
- **범위 150**: 반경 150 픽셀 내 팀원에게 버프
- **자동 관리**: 진입/이탈 시 즉시 버프 적용/제거
- **버프 효과**: 이동속도 +10%, 공격속도 +15%

#### 에어스트라이크 (gen_strike)
```java
// 스트라이크 예약
if ("gen_strike".equals(abilityId) && targetX >= 0 && targetY >= 0) {
    int strikeId = nextStrikeId.getAndIncrement();
    long impactAt = System.currentTimeMillis() + 2000; // 2초 후 임팩트
    ScheduledStrike strike = new ScheduledStrike(strikeId, user, playerInfo.team, targetX, targetY, impactAt);
    scheduledStrikes.put(strikeId, strike);
    
    // 즉시 마커 브로드캐스트
    String markMsg = "STRIKE_MARK:" + strikeId + "," + targetX + "," + targetY;
    for (ClientHandler ch : clients.values()) {
        ch.sendMessage(markMsg);
    }
    
    // 2초 후 임팩트 실행 (별도 스레드)
    new Thread(() -> {
        try {
            Thread.sleep(2000);
            executeStrike(strikeId);
        } catch (InterruptedException ignored) {}
    }).start();
}

private void executeStrike(int strikeId) {
    ScheduledStrike strike = scheduledStrikes.remove(strikeId);
    if (strike == null) return;
    
    int radius = 120;
    String impactMsg = "STRIKE_IMPACT:" + strikeId + "," + strike.targetX + "," + strike.targetY + "," + radius;
    broadcast(impactMsg, null);
    
    // 범위 내 플레이어에게 데미지
    int strikeDamage = 50;
    for (ClientHandler ch : clients.values()) {
        if (ch.playerInfo == null || ch.playerInfo.hp <= 0) continue;
        
        int dx = (int) ch.playerInfo.x - strike.targetX;
        int dy = (int) ch.playerInfo.y - strike.targetY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        
        if (dist <= radius) {
            ch.playerInfo.hp -= strikeDamage;
            if (ch.playerInfo.hp <= 0) {
                ch.playerInfo.hp = 0;
                ClientHandler striker = clients.get(strike.owner);
                if (striker != null && !ch.playerName.equals(strike.owner)) {
                    striker.playerInfo.kills++;
                    ch.playerInfo.deaths++;
                    striker.sendMessage("KILL:" + ch.playerName);
                    broadcast("CHAT:" + strike.owner + " 님이 에어스트라이크로 " + ch.playerName + " 처치!", null);
                } else {
                    ch.playerInfo.deaths++;
                    broadcast("CHAT:" + ch.playerName + " 님이 에어스트라이크에 사망!", null);
                }
            }
            broadcastStats(ch.playerName, ch.playerInfo);
        }
    }
    
    checkRoundEnd();
    
    // 범위 내 오브젝트 파괴
    List<Integer> toDestroy = new ArrayList<>();
    for (PlacedObject obj : placedObjects.values()) {
        int dx = obj.x - strike.targetX;
        int dy = obj.y - strike.targetY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist <= radius) {
            toDestroy.add(obj.id);
        }
    }
    for (int objId : toDestroy) {
        placedObjects.remove(objId);
        broadcast("OBJ_DESTROY:" + objId, null);
    }
}
```
- **2초 지연**: 마커 표시 후 2초 뒤 폭발
- **범위 120**: 반경 120 픽셀 내 모든 플레이어/오브젝트 피해
- **데미지 50**: 고정 50 데미지
- **킬 크레딧**: 호출자가 킬 획득 (자폭 가능)

### 6. 라운드 시스템

#### 라운드 시작
```java
private void startNextRound() {
    // 라운드 카운터 증가
    if (roundCount == 0) {
        roundCount = 1;
    } else if (roundEnded) {
        roundCount++;
    }
    roundEnded = false;
    
    // 랜덤 맵 선택
    String[] availableMaps = { "map", "map2", "map3", "village" };
    String selectedMap = availableMaps[new Random().nextInt(availableMaps.length)];
    
    // 게임 상태 초기화
    placedObjects.clear();
    activeAuras.clear();
    scheduledStrikes.clear();
    
    // 캐릭터 변경 제한 초기화
    playerCharacterChanged.clear();
    currentRoundStartTime = System.currentTimeMillis();
    
    // 모든 플레이어 HP 초기화
    for (ClientHandler ch : clients.values()) {
        if (ch.playerInfo != null && ch.playerInfo.characterId != null) {
            ch.playerInfo.hp = (int) CharacterData.getById(ch.playerInfo.characterId).health;
        }
    }
    
    // ROUND_START 패킷: roundNumber,mapId;playerCount;name1,charId1,hp1,maxHp1;...
    StringBuilder roundStartMsg = new StringBuilder();
    roundStartMsg.append("ROUND_START:").append(roundCount).append(",").append(selectedMap).append(";");
    roundStartMsg.append(clients.size());
    
    for (ClientHandler ch : clients.values()) {
        if (ch.playerInfo != null) {
            String charId = (ch.playerInfo.characterId != null) ? ch.playerInfo.characterId : "raven";
            int maxHp = (int) CharacterData.getById(charId).health;
            roundStartMsg.append(";").append(ch.playerName).append(",")
                         .append(charId).append(",").append(ch.playerInfo.hp).append(",").append(maxHp);
        }
    }
    
    broadcast(roundStartMsg.toString(), null);
    
    // 각 플레이어 스탯 브로드캐스트
    for (ClientHandler ch : clients.values()) {
        if (ch.playerInfo != null) {
            broadcastStats(ch.playerName, ch.playerInfo);
        }
    }
    
    broadcastTeamRoster();
}
```
- **랜덤 맵**: 4개 맵 중 무작위 선택 (`map`, `map2`, `map3`, `village`)
- **HP 초기화**: 캐릭터별 최대 HP로 리셋
- **캐릭터 변경 제한 시작**: `currentRoundStartTime` 설정 (10초 제한)
- **상태 초기화**: 오브젝트, 오라, 스트라이크 전부 제거

#### 라운드 종료 체크
```java
private void checkRoundEnd() {
    if (roundEnded) return;
    if (clients.size() < 2) return; // 혼자는 라운드 진행 불가
    
    int redAlive = 0, blueAlive = 0;
    int redTotal = 0, blueTotal = 0;
    
    for (ClientHandler ch : clients.values()) {
        if (ch.playerInfo == null) continue;
        if (ch.playerInfo.team == GameConstants.TEAM_RED) {
            redTotal++;
            if (ch.playerInfo.hp > 0) redAlive++;
        } else if (ch.playerInfo.team == GameConstants.TEAM_BLUE) {
            blueTotal++;
            if (ch.playerInfo.hp > 0) blueAlive++;
        }
    }
    
    // 한 팀 전멸 시 라운드 종료
    if (redTotal > 0 && redAlive == 0) {
        endRound(GameConstants.TEAM_BLUE);
    } else if (blueTotal > 0 && blueAlive == 0) {
        endRound(GameConstants.TEAM_RED);
    }
}
```

#### 라운드/게임 종료
```java
private void endRound(int winningTeam) {
    roundEnded = true;
    String winTeamName = (winningTeam == GameConstants.TEAM_RED) ? "RED" : "BLUE";
    
    if (winningTeam == GameConstants.TEAM_RED) {
        redWins++;
    } else {
        blueWins++;
    }
    
    broadcast("CHAT:=== 라운드 종료! " + winTeamName + " 팀 승리! ===", null);
    broadcast("ROUND_WIN:" + winningTeam + "," + redWins + "," + blueWins, null);
    
    // 게임 종료 체크 (3판 2선승)
    if (redWins >= MAX_WINS || blueWins >= MAX_WINS) {
        broadcast("GAME_OVER:" + winTeamName, null);
        System.out.println("[GAME_OVER] " + winTeamName + " wins! Resetting...");
        
        // 10초 후 초기화
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                resetGameState();
            }
        }, 10000);
    } else {
        // 3초 후 다음 라운드
        broadcast("CHAT:3초 후 다음 라운드...", null);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                startNextRound();
            }
        }, 3000);
    }
}
```

#### 게임 상태 리셋
```java
private void resetGameState() {
    // 게임 카운터 초기화
    roundCount = 0;
    redWins = 0;
    blueWins = 0;
    roundEnded = false;
    
    // 오브젝트 및 스킬 초기화
    placedObjects.clear();
    activeAuras.clear();
    scheduledStrikes.clear();
    playerCharacterChanged.clear();
    
    // 모든 플레이어 상태 초기화
    for (ClientHandler ch : clients.values()) {
        ch.ready = false;
        if (ch.playerInfo != null && ch.playerInfo.characterId != null) {
            CharacterData cd = CharacterData.getById(ch.playerInfo.characterId);
            if (cd != null) {
                ch.playerInfo.hp = (int) cd.health;
            }
            ch.playerInfo.kills = 0;
            ch.playerInfo.deaths = 0;
        }
    }
    
    broadcast("CHAT:[시스템] 게임 종료. 로비로 돌아갑니다.", null);
}
```

### 7. 캐릭터별 데미지 계산
```java
private int resolveBasicDamage(String characterId) {
    if (characterId == null) {
        return GameConstants.MISSILE_DAMAGE; // 기본 15
    }
    
    try {
        Ability[] abs = CharacterData.createAbilities(characterId);
        if (abs != null && abs.length > 0) {
            float dmg = abs[0].damage; // 첫 번째 Ability (기본 공격)
            if (dmg <= 0) {
                return GameConstants.MISSILE_DAMAGE;
            }
            // 서버는 정수 HP 관리 - 반올림
            return Math.max(1, Math.round(dmg));
        }
    } catch (Exception ignored) {}
    
    return GameConstants.MISSILE_DAMAGE;
}
```
- **캐릭터별 데미지**: `CharacterData.createAbilities()`의 첫 번째 Ability.damage
- **기본값**: 15 (`GameConstants.MISSILE_DAMAGE`)
- **정수 변환**: 반올림 (`Math.round()`)

---

## 💡 강점

### 1. 동시성 안전 설계
- **ConcurrentHashMap**: 멀티스레드 환경에서 안전한 컬렉션 사용
- **AtomicInteger**: 오브젝트 ID 생성 시 원자적 연산
- **전용 스레드**: 각 클라이언트마다 별도 스레드로 독립 처리

### 2. 강력한 검증 시스템
- **3단계 입장 검증**: 이름 → 캐릭터 필수 → 캐릭터 유효성
- **3단계 캐릭터 변경 검증**: 시간 제한 → 횟수 제한 → 상태 체크
- **스폰 보호**: 리스폰 후 3초간 무적

### 3. 풍부한 스킬 시스템
- **설치 오브젝트**: 지뢰 (즉시 폭발), 터렛 (자동 공격)
- **버프 시스템**: 오라 (동적 추적, 범위 버프)
- **범위 공격**: 에어스트라이크 (2초 지연, 킬 크레딧)

### 4. 세밀한 로깅
```java
System.out.println("[JOIN_SUCCESS] " + playerName + " joined with " + joinCharId + " (HP: " + playerInfo.hp + ")");
System.out.println("[CHARACTER_SELECT_DENIED] " + playerName + " - Time limit exceeded: " + elapsed + "ms");
System.out.println("[HIT] " + playerName + " hit " + hitPlayer + " (damage: " + dmg + ", remaining HP: " + target.playerInfo.hp + ")");
System.out.println("[TURRET_SHOOT] Turret #" + obj.id + " (owner: " + obj.owner + ", team: " + obj.team + ") attacking " + ch.playerName);
```
- **[대괄호] 접두사**: 이벤트 종류 명확히 표시
- **상세 정보**: 플레이어, HP, 시간, 팀 등 모든 컨텍스트 포함
- **디버깅 용이**: 문제 추적 및 재현 가능

### 5. 우아한 에러 처리
```java
try {
    processMessage(message);
} catch (Exception ex) {
    System.err.println("[ERROR] processMessage 실패 (" + playerName + "): " + message);
    ex.printStackTrace();
    // 계속 진행 (연결은 유지)
}
```
- **연결 유지**: 일부 메시지 처리 실패해도 연결 끊지 않음
- **상세 로깅**: 플레이어명, 메시지 내용, 스택트레이스 모두 출력

---

## 🔧 개선 제안

### 1. 서버 클래스 분리 (중요도: 높음)
**현재 상태**: 1,193줄 단일 클래스

**문제점**:
- 모든 로직이 GameServer와 ClientHandler에 집중
- 테스트 어려움
- 유지보수 복잡도 증가

**제안**:
```java
// 1) 게임 오브젝트 관리 분리
public class GameObjectManager {
    private final Map<Integer, PlacedObject> placedObjects = new ConcurrentHashMap<>();
    private final Map<String, ActiveAura> activeAuras = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledStrike> scheduledStrikes = new ConcurrentHashMap<>();
    
    public void placeObject(PlacedObject obj) { ... }
    public void activateAura(ActiveAura aura) { ... }
    public void scheduleStrike(ScheduledStrike strike) { ... }
}

// 2) 라운드 관리 분리
public class RoundManager {
    private int roundCount = 0;
    private int redWins = 0;
    private int blueWins = 0;
    private boolean roundEnded = false;
    
    public void startNextRound() { ... }
    public void checkRoundEnd(List<ClientHandler> clients) { ... }
    public void endRound(int winningTeam) { ... }
}

// 3) 프로토콜 핸들러 분리
public class ProtocolHandler {
    public void handleJoin(ClientHandler client, String data) { ... }
    public void handleCharacterSelect(ClientHandler client, String data) { ... }
    public void handleHit(ClientHandler attacker, String targetName) { ... }
}
```

**예상 효과**:
- GameServer.java: 1,193줄 → 400줄 (66% 감소)
- 단위 테스트 가능
- 기능별 독립 수정

### 2. 데미지 계산 로직 개선
**현재 상태**: `resolveBasicDamage()` 메서드

**문제점**:
- 매번 `CharacterData.createAbilities()` 호출 (비효율)
- 캐시 없음

**제안**:
```java
// DamageCalculator 클래스 신설
public class DamageCalculator {
    private final Map<String, Integer> damageCache = new ConcurrentHashMap<>();
    
    public int getBasicDamage(String characterId) {
        if (characterId == null) {
            return GameConstants.MISSILE_DAMAGE;
        }
        
        return damageCache.computeIfAbsent(characterId, id -> {
            try {
                Ability[] abs = CharacterData.createAbilities(id);
                if (abs != null && abs.length > 0) {
                    return Math.max(1, Math.round(abs[0].damage));
                }
            } catch (Exception ignored) {}
            return GameConstants.MISSILE_DAMAGE;
        });
    }
}

// GameServer에서 사용
private final DamageCalculator damageCalc = new DamageCalculator();

// HIT 처리 시
int dmg = damageCalc.getBasicDamage(this.playerInfo.characterId);
```

### 3. 브로드캐스트 최적화
**현재 상태**: 모든 메시지 개별 전송

**문제점**:
- 불필요한 반복문
- 네트워크 부하

**제안**:
```java
// 메시지 큐 도입
public class BroadcastManager {
    private final BlockingQueue<BroadcastMessage> messageQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    public BroadcastManager() {
        // 10ms마다 큐에 쌓인 메시지 일괄 전송
        scheduler.scheduleAtFixedRate(this::flushMessages, 0, 10, TimeUnit.MILLISECONDS);
    }
    
    public void queueBroadcast(String message, String excludeClient) {
        messageQueue.offer(new BroadcastMessage(message, excludeClient));
    }
    
    private void flushMessages() {
        List<BroadcastMessage> messages = new ArrayList<>();
        messageQueue.drainTo(messages);
        
        if (messages.isEmpty()) return;
        
        // 클라이언트별로 메시지 그룹화
        Map<String, List<String>> messagesByClient = new HashMap<>();
        // ... 일괄 전송 로직
    }
}
```

### 4. 스킬 처리 Factory 패턴
**현재 상태**: `handleSkillUse()` 메서드에 if-else 나열

**제안**:
```java
// SkillHandler 인터페이스
public interface SkillHandler {
    void handle(ClientHandler client, String[] data);
}

// 개별 핸들러 구현
public class TechMineHandler implements SkillHandler {
    @Override
    public void handle(ClientHandler client, String[] data) {
        int targetX = Integer.parseInt(data[3]);
        int targetY = Integer.parseInt(data[4]);
        // 지뢰 설치 로직
    }
}

// Factory 등록
public class SkillFactory {
    private final Map<String, SkillHandler> handlers = new HashMap<>();
    
    public SkillFactory() {
        handlers.put("tech_mine", new TechMineHandler());
        handlers.put("tech_turret", new TechTurretHandler());
        handlers.put("gen_aura", new GenAuraHandler());
        handlers.put("gen_strike", new GenStrikeHandler());
    }
    
    public void handleSkill(String abilityId, ClientHandler client, String[] data) {
        SkillHandler handler = handlers.get(abilityId);
        if (handler != null) {
            handler.handle(client, data);
        }
    }
}
```

### 5. 프로토콜 타입 안전성
**현재 상태**: 문자열 프로토콜 (`"JOIN"`, `"HIT"`, etc.)

**문제점**:
- 오타 가능성
- IDE 자동완성 없음
- 컴파일 타임 체크 불가

**제안**:
```java
// Protocol 열거형
public enum ProtocolType {
    JOIN("JOIN"),
    CHAT("CHAT"),
    TEAM("TEAM"),
    CHARACTER_SELECT("CHARACTER_SELECT"),
    READY("READY"),
    START("START"),
    POS("POS"),
    SHOOT("SHOOT"),
    SKILL("SKILL"),
    HIT("HIT"),
    HITME("HITME"),
    HIT_OBJ("HIT_OBJ"),
    DEATH("DEATH"),
    RESPAWN("RESPAWN"),
    QUIT("QUIT");
    
    private final String command;
    
    ProtocolType(String command) {
        this.command = command;
    }
    
    public String getCommand() {
        return command;
    }
    
    public static ProtocolType fromCommand(String cmd) {
        for (ProtocolType type : values()) {
            if (type.command.equals(cmd)) {
                return type;
            }
        }
        return null;
    }
}

// 사용
ProtocolType protocol = ProtocolType.fromCommand(command);
switch (protocol) {
    case JOIN -> handleJoin(data);
    case HIT -> handleHit(data);
    // ...
}
```

### 6. 오브젝트 수명 관리
**현재 상태**: 오브젝트가 맵에 무한정 유지

**문제점**:
- 메모리 누수 가능성 (파괴되지 않은 오브젝트)
- 오래된 오브젝트 정리 없음

**제안**:
```java
// 주기적 정리 타이머 추가
Timer cleanupTimer = new Timer(true);
cleanupTimer.scheduleAtFixedRate(new TimerTask() {
    @Override
    public void run() {
        cleanupOldObjects();
    }
}, 60000, 60000); // 1분마다

private void cleanupOldObjects() {
    long now = System.currentTimeMillis();
    long maxAge = 600000; // 10분
    
    placedObjects.entrySet().removeIf(e -> {
        PlacedObject obj = e.getValue();
        if (now - obj.createdAt > maxAge) {
            System.out.println("[CLEANUP] Removing old object: " + obj.id);
            broadcast("OBJ_DESTROY:" + obj.id, null);
            return true;
        }
        return false;
    });
}
```

### 7. HP 동기화 보장 강화
**현재 상태**: `broadcastStats()` 호출

**문제점**:
- 일부 경로에서 누락 가능성
- HP 불일치 발생 시 디버깅 어려움

**제안**:
```java
// PlayerInfo에 HP setter 추가 (자동 브로드캐스트)
public class PlayerInfo {
    private int hp;
    private final Runnable onHpChanged;
    
    public void setHp(int newHp, boolean broadcast) {
        this.hp = newHp;
        if (broadcast && onHpChanged != null) {
            onHpChanged.run();
        }
    }
}

// ClientHandler에서 사용
playerInfo = new PlayerInfo(clients.size(), playerName, () -> {
    broadcastStats(playerName, playerInfo);
});

// HP 변경 시 자동 브로드캐스트
playerInfo.setHp(newHp, true);
```

---

## 📊 코드 품질 평가

| 항목 | 점수 | 설명 |
|------|------|------|
| **아키텍처** | ⭐⭐⭐☆☆ | 단일 클래스에 모든 로직 집중, 분리 필요 |
| **동시성** | ⭐⭐⭐⭐⭐ | ConcurrentHashMap, AtomicInteger 사용 완벽 |
| **검증 시스템** | ⭐⭐⭐⭐⭐ | 3단계 입장/변경 검증, 스폰 보호 등 탄탄 |
| **로깅** | ⭐⭐⭐⭐⭐ | 상세하고 일관된 로그 포맷, 디버깅 용이 |
| **확장성** | ⭐⭐⭐☆☆ | 새 스킬 추가 시 if-else 수정 필요 |
| **성능** | ⭐⭐⭐⭐☆ | 대부분 효율적, 일부 최적화 가능 |

**총점: 4.0 / 5.0** ⭐⭐⭐⭐☆

---

## 🎓 결론

GameServer.java는 **강력한 멀티플레이어 기능**을 제공하는 서버입니다. 특히 **3단계 검증 시스템**, **풍부한 스킬 구현**, **세밀한 로깅**이 인상적입니다.

### 주요 성과
1. ✅ **동시성 안전**: ConcurrentHashMap으로 멀티스레드 안전 보장
2. ✅ **강력한 검증**: 3단계 입장/캐릭터 변경 검증
3. ✅ **풍부한 스킬**: 지뢰, 터렛, 오라, 에어스트라이크 등 다양한 스킬
4. ✅ **세밀한 로깅**: [대괄호] 접두사로 이벤트 종류 명확히 표시
5. ✅ **3판 2선승**: 라운드/게임 종료 시스템 완벽

### 개선 방향
1. **클래스 분리**: GameObjectManager, RoundManager, ProtocolHandler 분리 (1,193 → 400줄)
2. **Factory 패턴**: 스킬 처리 확장성 개선
3. **캐싱**: 데미지 계산 결과 캐싱으로 성능 향상
4. **타입 안전성**: Protocol 열거형 도입

**프로덕션 레벨 도달** 단계이며, 클래스 분리만 하면 **완벽한 멀티플레이어 서버**가 될 것입니다. 🎉
