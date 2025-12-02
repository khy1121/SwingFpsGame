package com.fpsgame.server;

import com.fpsgame.common.Ability;
import com.fpsgame.common.CharacterData;
import com.fpsgame.common.GameConstants;
import com.fpsgame.common.Protocol;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 간단한 게임 서버
 * 최대 4명의 플레이어 지원
 */
public class GameServer {

    private ServerSocket serverSocket;
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private boolean running = true;

    // 설치형 오브젝트 (지뢰, 터렛 등)
    private Map<Integer, PlacedObject> placedObjects = new ConcurrentHashMap<>();
    private AtomicInteger nextPlacedObjectId = new AtomicInteger(1);

    private Timer turretAttackTimer;
    private static final int TURRET_RANGE = 180;
    private static final int TURRET_ATTACK_INTERVAL = 900; // ms

    // 활성 오라 (gen_aura)
    private Map<String, ActiveAura> activeAuras = new ConcurrentHashMap<>();

    // 에어스트라이크 (gen_strike)
    private Map<Integer, ScheduledStrike> scheduledStrikes = new ConcurrentHashMap<>();
    private AtomicInteger nextStrikeId = new AtomicInteger(1);

    // 라운드 시스템
    private int roundCount = 0;
    private int redWins = 0;
    private int blueWins = 0;
    private boolean roundEnded = false;
    private static final int MAX_WINS = 2; // 3판 2선승

    // 캐릭터 선택 제한 (라운드당 1회, 라운드 시작 10초 이내)
    private long currentRoundStartTime = 0;
    private Map<String, Boolean> playerCharacterChanged = new ConcurrentHashMap<>();

    /**
     * 설치된 오브젝트 (지뢰, 터렛)
     */
    static class PlacedObject {
        int id;
        String type; // "tech_mine", "tech_turret"
        String owner;
        int team;
        int x, y;
        int hp;
        int maxHp;
        long createdAt;

        PlacedObject(int id, String type, String owner, int team, int x, int y, int hp) {
            this.id = id;
            this.type = type;
            this.owner = owner;
            this.team = team;
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.maxHp = hp;
            this.createdAt = System.currentTimeMillis();
        }
    }

    /**
     * 활성 오라 (gen_aura)
     */
    static class ActiveAura {
        String ownerName;
        int ownerTeam;
        int x, y;
        float radius;
        long expiresAt;
        Set<String> currentlyBuffed = new HashSet<>();

        ActiveAura(String owner, int team, int x, int y, float radius, long expiresAt) {
            this.ownerName = owner;
            this.ownerTeam = team;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.expiresAt = expiresAt;
        }
    }

    /**
     * 예약된 에어스트라이크
     */
    static class ScheduledStrike {
        int id;
        String owner;
        int team;
        int targetX, targetY;
        long impactAt;

        ScheduledStrike(int id, String owner, int team, int targetX, int targetY, long impactAt) {
            this.id = id;
            this.owner = owner;
            this.team = team;
            this.targetX = targetX;
            this.targetY = targetY;
            this.impactAt = impactAt;
        }
    }

    public GameServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("서버 시작: " + port);
        System.out.println("최대 플레이어 수: " + GameConstants.MAX_PLAYERS);
        // 터렛 자동 공격 타이머 시작
        turretAttackTimer = new Timer(true);
        turretAttackTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkTurretTargets();
            }
        }, 1000, TURRET_ATTACK_INTERVAL);

    }

    public void start() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();

                if (clients.size() >= GameConstants.MAX_PLAYERS) {
                    DataOutputStream tmpOut = new DataOutputStream(clientSocket.getOutputStream());
                    tmpOut.writeUTF("서버가 가득 찼습니다. 나중에 다시 시도하세요.");
                    tmpOut.flush();
                    clientSocket.close();
                    continue;
                }

                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            } catch (IOException e) {
                if (running)
                    e.printStackTrace();
            }
        }
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    private void broadcast(String message, String excludeClient) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (!entry.getKey().equals(excludeClient)) {
                entry.getValue().sendMessage(message);
            }
        }
    }

    // 팀/레디 상태 전체 브로드캐스트 (캐릭터 정보 포함)
    private void broadcastTeamRoster() {
        StringBuilder sb = new StringBuilder("TEAM_ROSTER:");
        boolean first = true;
        for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
            ClientHandler ch = e.getValue();
            if (ch.playerInfo == null)
                continue;
            if (!first)
                sb.append(';');
            first = false;
            sb.append(ch.playerName)
                    .append(',').append(ch.playerInfo.team)
                    .append(',').append(ch.ready)
                    .append(',').append(ch.playerInfo.characterId != null ? ch.playerInfo.characterId : "");
        }
        String rosterMsg = sb.toString();
        for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
            e.getValue().sendMessage(rosterMsg);
        }
    }

    // 간단한 스탯 브로드캐스트: 이름, kills, deaths, hp
    private void broadcastStats(String name, Protocol.PlayerInfo info) {
        if (info == null)
            return;
        String charId = (info.characterId != null) ? info.characterId : "raven";
        String stats = "STATS:" + name + "," + info.kills + "," + info.deaths + "," + info.hp + "," + charId;
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            entry.getValue().sendMessage(stats);
        }
    }

    class ClientHandler implements Runnable {
        private final Socket socket;
        private DataOutputStream out;
        private DataInputStream in;
        private String playerName;
        private Protocol.PlayerInfo playerInfo;
        private long spawnProtectedUntil = 0L;
        private boolean ready = false;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                try {
                    socket.setTcpNoDelay(true);
                    socket.setKeepAlive(true);
                    socket.setSendBufferSize(64 * 1024);
                    socket.setReceiveBufferSize(64 * 1024);
                } catch (Exception ignore) {
                }
                out = new DataOutputStream(socket.getOutputStream());
                in = new DataInputStream(socket.getInputStream());

                while (true) {
                    String message = in.readUTF();
                    processMessage(message);
                }
            } catch (IOException e) {
                System.out.println("클라이언트 연결 끊김: " + playerName);
            } finally {
                cleanup();
            }
        }

        private void processMessage(String message) {
            String[] parts = message.split(":", 2);
            if (parts.length < 1)
                return;
            String command = parts[0];
            String data = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "JOIN":
                    // JOIN:playerName 또는 JOIN:playerName:characterId 형식 파싱
                    String[] joinParts = data.split(":");
                    playerName = joinParts[0];
                    playerInfo = new Protocol.PlayerInfo(clients.size(), playerName);
                    playerInfo.x = 400;
                    playerInfo.y = 300;
                    playerInfo.hp = GameConstants.MAX_HP;
                    
                    // characterId가 포함된 경우 설정
                    if (joinParts.length > 1 && joinParts[1] != null && !joinParts[1].trim().isEmpty()) {
                        String joinCharId = joinParts[1].trim().toLowerCase();
                        playerInfo.characterId = joinCharId;
                        CharacterData cd = CharacterData.getById(joinCharId);
                        if (cd != null) {
                            playerInfo.hp = (int) cd.health;
                            System.out.println("[JOIN] " + playerName + " joined with " + joinCharId + " (HP: " + playerInfo.hp + ")");
                        }
                    }
                    clients.put(playerName, this);
                    sendMessage("WELCOME: 서버에 " + playerName + " 님이 연결되었습니다.");
                    broadcast("CHAT:" + playerName + " 님이 게임에 참가했습니다!", playerName);
                    // 기존 플레이어 스탯을 새 플레이어에게 전달
                    for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                        ClientHandler ch = e.getValue();
                        if (ch != null && ch.playerInfo != null) {
                            // characterId 포함하여 STATS 전송
                            String charId = (ch.playerInfo.characterId != null) ? ch.playerInfo.characterId : "raven";
                            sendMessage("STATS:" + ch.playerName + "," + ch.playerInfo.kills + ","
                                    + ch.playerInfo.deaths + "," + ch.playerInfo.hp + "," + charId);
                            // 캐릭터 정보 전송 (있다면)
                            if (ch.playerInfo.characterId != null) {
                                sendMessage("CHARACTER_SELECT:" + ch.playerName + "," + ch.playerInfo.characterId);
                            }
                        }
                    }
                    // 새 플레이어 스탯을 모두에게 전달
                    broadcastStats(playerName, playerInfo);
                    System.out.println("플레이어 참가: " + playerName + " (총: " + clients.size() + ")");
                    broadcastTeamRoster();
                    break;

                case "CHAT":
                    broadcast("CHAT:" + playerName + ": " + data, null);
                    break;

                case "TEAM":
                    playerInfo.team = Integer.parseInt(data);
                    broadcastTeamRoster();
                    break;

                case "CHARACTER_SELECT":
                    // 라운드 진행 중일 때만 제한 적용 (로비에서는 무제한)
                    if (currentRoundStartTime > 0) {
                        long elapsed = System.currentTimeMillis() - currentRoundStartTime;
                        // 1. 시간 제한 (10초)
                        if (elapsed > 10000) {
                            sendMessage("CHAT:[시스템] 라운드 시작 후 10초가 지나 캐릭터를 변경할 수 없습니다.");
                            break;
                        }
                        // 2. 횟수 제한 (라운드당 1회)
                        if (playerCharacterChanged.containsKey(playerName)) {
                            sendMessage("CHAT:[시스템] 이번 라운드에 이미 캐릭터를 변경했습니다.");
                            break;
                        }
                        // 변경 기록
                        playerCharacterChanged.put(playerName, true);
                    }

                    // ===== 단일 소스: playerInfo.characterId 업데이트 =====
                    playerInfo.characterId = data;

                    // 캐릭터별 HP 적용 (maxHP 계산)
                    com.fpsgame.common.CharacterData cd = com.fpsgame.common.CharacterData.getById(data);
                    playerInfo.hp = (int) cd.health;

                    System.out.println("[CHARACTER_SELECT] " + playerName + " changed to " + data + " (HP: " + playerInfo.hp + ")");

                    // 변경 성공 알림 (본인 및 타인)
                    // CHARACTER_SELECT:playerName,characterId
                    String charSelectMsg = "CHARACTER_SELECT:" + playerName + "," + data;
                    broadcast(charSelectMsg, null);

                    // ===== HP/스탯 즉시 브로드캐스트 (모든 클라이언트 동기화) =====
                    broadcastStats(playerName, playerInfo);
                    
                    broadcast("CHAT:" + playerName + " 님이 " + cd.name
                            + " 캐릭터를 선택했습니다!", null);
                    
                    broadcastTeamRoster();
                    break;

                case "READY":
                    ready = true;
                    broadcast("CHAT:" + playerName + " 님이 준비 완료했습니다!", null);
                    broadcastTeamRoster();
                    break;

                case "UNREADY":
                    ready = false;
                    broadcast("CHAT:" + playerName + " 님이 준비 해제했습니다.", null);
                    broadcastTeamRoster();
                    break;

                case "START":
                    // 서버 측 검증: 각 팀 1명 이상, 팀 인원 차이 2 이하, 전체 모두 ready
                    int redCount = 0, blueCount = 0;
                    boolean allReady = true;
                    for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                        ClientHandler ch = e.getValue();
                        if (ch.playerInfo == null)
                            continue;
                        if (ch.playerInfo.team == GameConstants.TEAM_RED)
                            redCount++;
                        else if (ch.playerInfo.team == GameConstants.TEAM_BLUE)
                            blueCount++;
                        if (!ch.ready)
                            allReady = false;
                    }
                    if (redCount == 0 || blueCount == 0) {
                        sendMessage("CHAT:Cannot start: 각 팀은 최소 1명의 플레이어가 필요합니다.");
                        break;
                    }
                    if (Math.abs(redCount - blueCount) > 2) {
                        sendMessage("CHAT:팀 인원 차이가 2를 초과합니다 (R=" + redCount + ", B=" + blueCount + ")");
                        break;
                    }
                    if (!allReady) {
                        sendMessage("CHAT:: 모든 플레이어가 준비되지 않았습니다.");
                        break;
                    }
                    broadcast("CHAT:게임이 시작됩니다...", null);
                    // 게임 시작 신호
                    for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                        e.getValue().sendMessage("GAME_START");
                    }
                    // 첫 라운드 시작
                    startNextRound();
                    break;

                case "POS":
                    String[] coords = data.split(",");
                    if (coords.length >= 2) {
                        playerInfo.x = Float.parseFloat(coords[0]);
                        playerInfo.y = Float.parseFloat(coords[1]);
                        // 방향 정보 파싱 (0:Down, 1:Up, 2:Left, 3:Right)
                        int direction = (coords.length >= 3) ? Integer.parseInt(coords[2]) : 0;
                        String charId = (playerInfo.characterId != null) ? playerInfo.characterId : "raven";
                        // PLAYER:이름,x,y,팀,hp,캐릭터ID,방향
                        String posUpdate = "PLAYER:" + playerName + "," + playerInfo.x + "," + playerInfo.y + ","
                                + playerInfo.team + "," + playerInfo.hp + "," + charId + "," + direction;
                        broadcast(posUpdate, playerName);

                        // 오라 범위 체크 및 버프 적용/제거
                        updateAuraBuffs();
                        // 지뢰 밟기 체크 (tech_mine)
                        List<Integer> minesToExplode = new ArrayList<>();
                        for (PlacedObject obj : placedObjects.values()) {
                            if ("tech_mine".equals(obj.type) && obj.hp > 0 && obj.team != playerInfo.team) {
                                double dist = Math
                                        .sqrt(Math.pow(playerInfo.x - obj.x, 2) + Math.pow(playerInfo.y - obj.y, 2));
                                if (dist < 24) // 밟은 판정 (지뢰 크기+캐릭터 크기)
                                    minesToExplode.add(obj.id);
                            }
                        }
                        for (int mineId : minesToExplode) {
                            PlacedObject mine = placedObjects.remove(mineId);
                            if (mine != null) {
                                int mineDamage = 60; // 지뢰 폭발 데미지
                                playerInfo.hp -= mineDamage;
                                if (playerInfo.hp < 0)
                                    playerInfo.hp = 0;
                                
                                // 지뢰로 사망한 경우 사망 처리
                                if (playerInfo.hp <= 0) {
                                    playerInfo.deaths++;
                                    broadcast("CHAT:" + playerName + " 님이 지뢰를 밟아 사망했습니다!", null);
                                    checkRoundEnd();
                                } else {
                                    broadcast("CHAT:" + playerName + " 님이 지뢰를 밟아 폭발! (데미지 " + mineDamage + ")", null);
                                }
                                
                                broadcastStats(playerName, playerInfo);
                                // 지뢰 파괴 메시지
                                String destroyMsg = "OBJ_DESTROY:" + mineId;
                                for (ClientHandler ch : clients.values()) {
                                    ch.sendMessage(destroyMsg);
                                }
                            }
                        }
                    }
                    break;

                case "SHOOT":
                    broadcast("SHOOT:" + playerName + "," + data, playerName);
                    break;

                case "SKILL":
                    // data format from client: abilityId,type,duration[,x,y]
                    // Broadcast to others with player name prefixed
                    // SKILL:playerName,abilityId,type,duration[,x,y]
                    handleSkillUse(playerName, data);
                    break;

                case "HIT_OBJ":
                    // Client reports hitting a placed object: objId
                    handleObjectHit(playerName, data);
                    break;

                case "HIT":
                    String hitPlayer = data;
                    ClientHandler target = clients.get(hitPlayer);
                    if (target != null && target.playerInfo != null) {
                        // 스폰 보호 중이거나 이미 사망 상태면 무시
                        long now = System.currentTimeMillis();
                        if (now < target.spawnProtectedUntil || target.playerInfo.hp <= 0 || this.playerInfo.hp <= 0) {
                            break;
                        }
                        int dmg = resolveBasicDamage(this.playerInfo.characterId);
                        target.playerInfo.hp -= dmg;
                        if (target.playerInfo.hp <= 0) {
                            target.playerInfo.hp = 0;
                            sendMessage("KILL:" + hitPlayer);
                            this.playerInfo.kills++;
                            target.playerInfo.deaths++;
                            broadcastStats(this.playerName, this.playerInfo);
                            broadcastStats(hitPlayer, target.playerInfo);
                            broadcast("CHAT:" + hitPlayer + " 님이 " + playerName + " 님을 처치했습니다!", null);
                        } else {
                            broadcastStats(hitPlayer, target.playerInfo);
                        }

                        // 라운드 종료 체크
                        checkRoundEnd();
                    }
                    break;

                case "HITME":
                    // 피해자(현재 클라이언트)가 자신이 피격되었음을 신고. data = shooterName
                    String shooterName = data;
                    ClientHandler shooter = clients.get(shooterName);
                    if (playerInfo != null) {
                        long now = System.currentTimeMillis();
                        // 스폰 보호 중이거나 이미 사망 상태면 무시
                        if (now < spawnProtectedUntil || playerInfo.hp <= 0)
                            break;
                        int dmg = resolveBasicDamage(shooter != null ? shooter.playerInfo.characterId : null);
                        playerInfo.hp -= dmg;
                        if (playerInfo.hp <= 0) {
                            playerInfo.hp = 0;
                            if (shooter != null && shooter.playerInfo != null) {
                                shooter.playerInfo.kills++;
                                playerInfo.deaths++;
                                broadcastStats(shooterName, shooter.playerInfo);
                                broadcastStats(playerName, playerInfo);
                                shooter.sendMessage("KILL:" + playerName);
                                broadcast("CHAT:" + shooterName + " 님이 " + playerName + " 님을 처치했습니다!", null);
                            } else {
                                // 슈터를 모르면 일반 사망 처리만
                                broadcastStats(playerName, playerInfo);
                                broadcast("CHAT:" + playerName + " 님이 사망했습니다!", null);
                            }
                        } else {
                            broadcastStats(playerName, playerInfo);
                        }

                        // 라운드 종료 체크
                        checkRoundEnd();
                    }
                    break;

                case "DEATH":
                    playerInfo.hp = 0;
                    playerInfo.deaths++;

                    broadcastStats(playerName, playerInfo);
                    broadcast("CHAT:" + playerName + " 님이 사망했습니다!", null);
                    checkRoundEnd();
                    break;

                case "RESPAWN":
                    String[] resp = data.split(",");
                    if (resp.length >= 2) {
                        playerInfo.x = Float.parseFloat(resp[0]);
                        playerInfo.y = Float.parseFloat(resp[1]);
                        // 캐릭터별 최대 HP로 부활
                        if (playerInfo.characterId != null) {
                            playerInfo.hp = (int) com.fpsgame.common.CharacterData
                                    .getById(playerInfo.characterId).health;
                        } else {
                            playerInfo.hp = GameConstants.MAX_HP;
                        }
                        // 3초 스폰 보호 시작
                        spawnProtectedUntil = System.currentTimeMillis() + 3000;
                        broadcastStats(playerName, playerInfo);
                        String charId = (playerInfo.characterId != null) ? playerInfo.characterId : "raven";
                        String posUpdate = "PLAYER:" + playerName + "," + playerInfo.x + "," + playerInfo.y + ","
                                + playerInfo.team + "," + playerInfo.hp + "," + charId;
                        broadcast(posUpdate, playerName);
                        broadcast("CHAT:" + playerName + " 님이 리스폰했습니다!", null);
                    }
                    break;

                case "QUIT":
                    cleanup();
                    break;
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                try {
                    out.writeUTF(message);
                    out.flush();
                } catch (IOException ignored) {
                }
            }
        }

        private void cleanup() {
            try {
                if (playerName != null) {
                    clients.remove(playerName);
                    broadcast("REMOVE:" + playerName, null);
                    broadcast("CHAT:" + playerName + " 님이 게임을 나갔습니다.", null);
                    System.out.println("Player left: " + playerName + " (Total: " + clients.size() + ")");
                    broadcastTeamRoster();
                }
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
                if (socket != null && !socket.isClosed())
                    socket.close();
            } catch (IOException ignored) {
            }
        }

        /**
         * 스킬 사용 처리 (설치형 오브젝트, 오라, 에어스트라이크 등)
         */
        private void handleSkillUse(String user, String data) {
            // data: abilityId,type,duration[,x,y]
            String[] parts = data.split(",");
            if (parts.length < 3)
                return;

            String abilityId = parts[0];
            String type = parts[1];
            float duration = 0f;
            try {
                duration = Float.parseFloat(parts[2]);
            } catch (NumberFormatException ignored) {
            }

            int targetX = -1, targetY = -1;
            if (parts.length >= 5) {
                try {
                    targetX = Integer.parseInt(parts[3]);
                    targetY = Integer.parseInt(parts[4]);
                } catch (NumberFormatException ignored) {
                }
            }

            // 설치형 스킬 처리 (지뢰, 터렛)
            if ("tech_mine".equals(abilityId) && targetX >= 0 && targetY >= 0) {
                int id = nextPlacedObjectId.getAndIncrement();
                PlacedObject obj = new PlacedObject(id, "tech_mine", user, playerInfo.team, targetX, targetY, 40);
                placedObjects.put(id, obj);
                String placeMsg = "PLACE:" + id + "," + obj.type + "," + obj.x + "," + obj.y + ","
                        + obj.hp + "," + obj.maxHp + "," + obj.owner + "," + obj.team;
                for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                    e.getValue().sendMessage(placeMsg);
                }
                System.out.println("[PLACE] " + user + " placed " + obj.type + " at (" + targetX + "," + targetY + ")");
            } else if ("tech_turret".equals(abilityId) && targetX >= 0 && targetY >= 0) {
                int id = nextPlacedObjectId.getAndIncrement();
                PlacedObject obj = new PlacedObject(id, "tech_turret", user, playerInfo.team, targetX, targetY, 100);
                placedObjects.put(id, obj);
                String placeMsg = "PLACE:" + id + "," + obj.type + "," + obj.x + "," + obj.y + ","
                        + obj.hp + "," + obj.maxHp + "," + obj.owner + "," + obj.team;
                for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                    e.getValue().sendMessage(placeMsg);
                }
                System.out.println("[PLACE] " + user + " placed " + obj.type + " at (" + targetX + "," + targetY + ")");
            }
            // 오라 활성화
            else if ("gen_aura".equals(abilityId) && playerInfo != null) {
                float radius = 150f; // 가까운 범위 (CharacterData에서 가져올 수도 있음)
                long expiresAt = System.currentTimeMillis() + (long) (duration * 1000);
                ActiveAura aura = new ActiveAura(user, playerInfo.team, (int) playerInfo.x, (int) playerInfo.y, radius,
                        expiresAt);
                activeAuras.put(user, aura);
                System.out.println(
                        "[AURA] " + user + " activated aura at (" + aura.x + "," + aura.y + "), radius=" + radius);
            }
            // 에어스트라이크 예약
            else if ("gen_strike".equals(abilityId) && targetX >= 0 && targetY >= 0 && playerInfo != null) {
                int strikeId = nextStrikeId.getAndIncrement();
                long impactAt = System.currentTimeMillis() + 2000; // 2초 후 임팩트
                ScheduledStrike strike = new ScheduledStrike(strikeId, user, playerInfo.team, targetX, targetY,
                        impactAt);
                scheduledStrikes.put(strikeId, strike);
                // 즉시 마커 브로드캐스트
                String markMsg = "STRIKE_MARK:" + strikeId + "," + targetX + "," + targetY;
                for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                    e.getValue().sendMessage(markMsg);
                }
                System.out.println(
                        "[STRIKE] " + user + " called airstrike at (" + targetX + "," + targetY + "), id=" + strikeId);
                // 2초 후 임팩트 스케줄 (별도 스레드)
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        executeStrike(strikeId);
                    } catch (InterruptedException ignored) {
                    }
                }).start();
            }

            // 모든 스킬은 클라이언트에게 브로드캐스트 (시각 효과용)
            String skillMsg = "SKILL:" + user + "," + data;
            broadcast(skillMsg, user);
        }

        /**
         * 오브젝트 피격 처리
         */
        private void handleObjectHit(String shooterName, String objIdStr) {
            int objId;
            try {
                objId = Integer.parseInt(objIdStr);
            } catch (NumberFormatException e) {
                return;
            }

            PlacedObject obj = placedObjects.get(objId);
            if (obj == null || obj.hp <= 0)
                return;

            // 데미지 계산 (슈터의 캐릭터 기반)
            ClientHandler shooter = clients.get(shooterName);
            int dmg = resolveBasicDamage(
                    shooter != null && shooter.playerInfo != null ? shooter.playerInfo.characterId : null);

            obj.hp -= dmg;
            if (obj.hp <= 0) {
                obj.hp = 0;
                placedObjects.remove(objId);
                String destroyMsg = "OBJ_DESTROY:" + objId;
                for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                    e.getValue().sendMessage(destroyMsg);
                }
                System.out.println("[OBJ_DESTROY] " + shooterName + " destroyed object " + objId);
            } else {
                String updateMsg = "OBJ_UPDATE:" + objId + "," + obj.hp;
                for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                    e.getValue().sendMessage(updateMsg);
                }
            }
        }

        /**
         * 오라 버프 업데이트 (POS 수신 시마다 호출)
         */
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
            for (Map.Entry<String, ActiveAura> entry : activeAuras.entrySet()) {
                ActiveAura aura = entry.getValue();

                // 오라 소유자의 현재 위치로 업데이트
                ClientHandler owner = clients.get(aura.ownerName);
                if (owner != null && owner.playerInfo != null) {
                    aura.x = (int) owner.playerInfo.x;
                    aura.y = (int) owner.playerInfo.y;
                }

                Set<String> nowInRange = new HashSet<>();

                // 모든 플레이어 체크
                for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                    ClientHandler ch = e.getValue();
                    if (ch.playerInfo == null || ch.playerInfo.hp <= 0)
                        continue;
                    if (ch.playerInfo.team != aura.ownerTeam)
                        continue; // 같은 팀만
                    if (ch.playerName.equals(aura.ownerName))
                        continue; // 본인 제외 (선택사항)

                    int dx = (int) ch.playerInfo.x - aura.x;
                    int dy = (int) ch.playerInfo.y - aura.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);

                    if (dist <= aura.radius) {
                        nowInRange.add(ch.playerName);

                        // 새로 들어온 플레이어에게 버프 적용
                        if (!aura.currentlyBuffed.contains(ch.playerName)) {
                            // BUFF:targetName,abilityId,moveSpeedMult,attackSpeedMult,durationRemaining
                            long remaining = Math.max(0, aura.expiresAt - now);
                            float dur = remaining / 1000f;
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

    }

    /**
     * 에어스트라이크 실행
     */
    private void executeStrike(int strikeId) {
        ScheduledStrike strike = scheduledStrikes.remove(strikeId);
        if (strike == null)
            return;

        int radius = 120; // 임팩트 반경
        String impactMsg = "STRIKE_IMPACT:" + strikeId + "," + strike.targetX + "," + strike.targetY + "," + radius;

        // 브로드캐스트
        for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
            e.getValue().sendMessage(impactMsg);
        }

        // 범위 내 플레이어에게 데미지 (서버 권위)
        int strikeDamage = 50; // 에어스트라이크 고정 데미지
        for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
            ClientHandler ch = e.getValue();
            if (ch.playerInfo == null || ch.playerInfo.hp <= 0)
                continue;

            int dx = (int) ch.playerInfo.x - strike.targetX;
            int dy = (int) ch.playerInfo.y - strike.targetY;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist <= radius) {
                ch.playerInfo.hp -= strikeDamage;
                if (ch.playerInfo.hp <= 0) {
                    ch.playerInfo.hp = 0;
                    // 스트라이크 호출자 킬 크레딧
                    ClientHandler striker = clients.get(strike.owner);
                    if (striker != null && striker.playerInfo != null && !ch.playerName.equals(strike.owner)) {
                        striker.playerInfo.kills++;
                        ch.playerInfo.deaths++;
                        broadcastStats(strike.owner, striker.playerInfo);
                        striker.sendMessage("KILL:" + ch.playerName);
                        broadcast("CHAT:" + strike.owner + " 님이 에어스트라이크로 " + ch.playerName + " 님을 처치했습니다!", null);
                    }
                }
                broadcastStats(ch.playerName, ch.playerInfo);
            }
        }

        // 범위 내 오브젝트 파괴
        List<Integer> toDestroy = new ArrayList<>();
        for (Map.Entry<Integer, PlacedObject> e : placedObjects.entrySet()) {
            PlacedObject obj = e.getValue();
            int dx = obj.x - strike.targetX;
            int dy = obj.y - strike.targetY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist <= radius) {
                toDestroy.add(e.getKey());
            }
        }
        for (int objId : toDestroy) {
            placedObjects.remove(objId);
            String destroyMsg = "OBJ_DESTROY:" + objId;
            for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                e.getValue().sendMessage(destroyMsg);
            }
        }

        System.out.println("[STRIKE_IMPACT] id=" + strikeId + " at (" + strike.targetX + "," + strike.targetY + ")");
    }

    /**
     * 기본 공격 데미지: 캐릭터 첫 번째 Ability.damage 사용 (없으면 기본 상수)
     */
    private int resolveBasicDamage(String characterId) {
        if (characterId == null)
            return GameConstants.MISSILE_DAMAGE;
        try {
            Ability[] abs = CharacterData.createAbilities(characterId);
            if (abs != null && abs.length > 0) {
                float dmg = abs[0].damage;
                if (dmg <= 0)
                    return GameConstants.MISSILE_DAMAGE;
                // 서버는 정수 HP 관리 - 반올림
                return Math.max(1, Math.round(dmg));
            }
        } catch (Exception ignored) {
        }
        return GameConstants.MISSILE_DAMAGE;
    }

    public static void main(String[] args) {
        try {
            int port = GameConstants.DEFAULT_PORT;
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }

            GameServer server = new GameServer(port);

            // Shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n서버를 종료합니다...");
                server.stop();
            }));

            server.start();

        } catch (IOException e) {
            System.err.println("서버 시작 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 모든 터렛에 대해 사거리 내 적을 감지해 공격
    private void checkTurretTargets() {
        for (PlacedObject obj : placedObjects.values()) {
            if (!"tech_turret".equals(obj.type) || obj.hp <= 0)
                continue;
            for (ClientHandler ch : clients.values()) {
                if (ch.playerInfo == null || ch.playerInfo.hp <= 0 || ch.playerInfo.team == obj.team)
                    continue;
                double dist = Math.sqrt(Math.pow(obj.x - ch.playerInfo.x, 2) + Math.pow(obj.y - ch.playerInfo.y, 2));
                if (dist <= TURRET_RANGE) {
                    // 터렛이 적을 감지하면 미사일 발사 메시지 브로드캐스트
                    // Client expects: TURRET_SHOOT:objId,tx,ty,targetName
                    String shootMsg = "TURRET_SHOOT:" + obj.id + "," + (int) ch.playerInfo.x + ","
                            + (int) ch.playerInfo.y + "," + ch.playerName;
                    for (ClientHandler c : clients.values()) {
                        c.sendMessage(shootMsg);
                    }

                    // 터렛 데미지 적용은 클라이언트의 미사일 충돌(HITME)로 위임
                    // (즉시 데미지 제거)
                    broadcastStats(ch.playerName, ch.playerInfo);

                    break; // 한 번에 한 명만 공격
                }
            }
        }
    }

    /**
     * 라운드 종료 조건 확인 (한 팀 전멸)
     */
    private void checkRoundEnd() {
        if (roundEnded)
            return;
        if (clients.size() < 2)
            return; // 혼자서는 라운드 진행 불가 (테스트용 예외 가능)

        int redAlive = 0;
        int blueAlive = 0;
        int redTotal = 0;
        int blueTotal = 0;

        for (ClientHandler ch : clients.values()) {
            if (ch.playerInfo == null)
                continue;
            if (ch.playerInfo.team == GameConstants.TEAM_RED) {
                redTotal++;
                if (ch.playerInfo.hp > 0)
                    redAlive++;
            } else if (ch.playerInfo.team == GameConstants.TEAM_BLUE) {
                blueTotal++;
                if (ch.playerInfo.hp > 0)
                    blueAlive++;
            }
        }

        // 팀이 존재하는데 전멸한 경우
        if (redTotal > 0 && redAlive == 0) {
            endRound(GameConstants.TEAM_BLUE);
        } else if (blueTotal > 0 && blueAlive == 0) {
            endRound(GameConstants.TEAM_RED);
        }
    }

    private void endRound(int winningTeam) {
        roundEnded = true;
        String winTeamName = (winningTeam == GameConstants.TEAM_RED) ? "RED" : "BLUE";

        if (winningTeam == GameConstants.TEAM_RED)
            redWins++;
        else
            blueWins++;

        broadcast("CHAT:=== 라운드 종료! " + winTeamName + " 팀 승리! ===", null);
        broadcast("ROUND_WIN:" + winningTeam + "," + redWins + "," + blueWins, null);

        // 게임 종료 체크
        if (redWins >= MAX_WINS || blueWins >= MAX_WINS) {
            broadcast("GAME_OVER:" + winTeamName, null);
            // 게임 리셋은 별도 명령이나 재시작 필요
        } else {
            // 10초 후 다음 라운드
            broadcast("CHAT:3초 후 다음 라운드가 시작됩니다...", null);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    startNextRound();
                }
            }, 3000);
        }
    }

    private void startNextRound() {
        // 라운드가 끝난 후 호출되므로 카운트 증가
        // 단, 첫 게임 시작(roundCount==0)일 때는 1로 시작
        if (roundCount == 0) {
            roundCount = 1;
        } else if (roundEnded) {
            roundCount++;
        }
        roundEnded = false;

        // 랜덤 맵 선택
        String[] availableMaps = { "map", "map2", "map3", "village" };
        String selectedMap = availableMaps[new java.util.Random().nextInt(availableMaps.length)];

        // 게임 상태 초기화
        placedObjects.clear();
        activeAuras.clear();
        scheduledStrikes.clear();

        // 캐릭터 변경 제한 초기화
        playerCharacterChanged.clear();
        currentRoundStartTime = System.currentTimeMillis();

        // 모든 플레이어 부활 및 위치 초기화 (클라이언트가 알아서 하거나 서버가 강제)
        // 여기서는 HP만 채워주고 클라이언트에게 라운드 시작 알림
        for (ClientHandler ch : clients.values()) {
            if (ch.playerInfo != null) {
                // ===== 단일 소스: playerInfo.characterId 기준으로 HP 초기화 =====
                if (ch.playerInfo.characterId != null) {
                    ch.playerInfo.hp = (int) com.fpsgame.common.CharacterData.getById(ch.playerInfo.characterId).health;
                } else {
                    ch.playerInfo.hp = GameConstants.MAX_HP;
                }
                System.out.println("[ROUND_START] " + ch.playerName + ": " + ch.playerInfo.characterId + " HP=" + ch.playerInfo.hp);
            }
        }

        // ===== ROUND_START 패킷에 모든 플레이어 정보 포함 =====
        // 형식: ROUND_START:roundNumber,mapId;playerCount;name1,charId1,hp1,maxHp1;name2,charId2,hp2,maxHp2;...
        StringBuilder roundStartMsg = new StringBuilder();
        roundStartMsg.append("ROUND_START:").append(roundCount).append(",").append(selectedMap).append(";");
        roundStartMsg.append(clients.size());
        
        for (ClientHandler ch : clients.values()) {
            if (ch.playerInfo != null) {
                String charId = (ch.playerInfo.characterId != null) ? ch.playerInfo.characterId : "raven";
                int maxHp = (int) com.fpsgame.common.CharacterData.getById(charId).health;
                roundStartMsg.append(";");
                roundStartMsg.append(ch.playerName).append(",")
                           .append(charId).append(",")
                           .append(ch.playerInfo.hp).append(",")
                           .append(maxHp);
            }
        }
        
        broadcast(roundStartMsg.toString(), null);
        System.out.println("[ROUND_START] Broadcasting: " + roundStartMsg.toString());
        
        // 각 플레이어에게 최신 스탯 브로드캐스트 (추가 보장)
        for (ClientHandler ch : clients.values()) {
            if (ch.playerInfo != null) {
                broadcastStats(ch.playerName, ch.playerInfo);
            }
        }
        
        broadcastTeamRoster(); // 갱신된 정보 전송
    }
}
