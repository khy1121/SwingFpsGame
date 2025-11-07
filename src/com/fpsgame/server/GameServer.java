package com.fpsgame.server;

import com.fpsgame.common.GameConstants;
import com.fpsgame.common.Protocol;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 간단한 게임 서버
 * 최대 4명의 플레이어 지원
 */
public class GameServer {
    
    private ServerSocket serverSocket;
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private boolean running = true;

    public GameServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("서버 시작: " + port);
        System.out.println("최대 플레이어 수: " + GameConstants.MAX_PLAYERS);
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
                if (running) e.printStackTrace();
            }
        }
    }

    public void stop() {
        running = false;
        try { serverSocket.close(); } catch (IOException ignored) {}
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
            if (ch.playerInfo == null) continue;
            if (!first) sb.append(';');
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
        if (info == null) return;
        String stats = "STATS:" + name + "," + info.kills + "," + info.deaths + "," + info.hp;
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

        ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try {
                try {
                    socket.setTcpNoDelay(true);
                    socket.setKeepAlive(true);
                    socket.setSendBufferSize(64 * 1024);
                    socket.setReceiveBufferSize(64 * 1024);
                } catch (Exception ignore) {}
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
            if (parts.length < 1) return;
            String command = parts[0];
            String data = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "JOIN":
                    playerName = data;
                    playerInfo = new Protocol.PlayerInfo(clients.size(), playerName);
                    playerInfo.x = 400; playerInfo.y = 300; playerInfo.hp = GameConstants.MAX_HP;
                    clients.put(playerName, this);
                    sendMessage("WELCOME: 서버에 " + playerName + " 님이 연결되었습니다.");
                    broadcast("CHAT:" + playerName + " 님이 게임에 참가했습니다!", playerName);
                    // 기존 플레이어 스탯을 새 플레이어에게 전달
                    for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                        ClientHandler ch = e.getValue();
                        if (ch != null && ch.playerInfo != null) {
                            sendMessage("STATS:" + ch.playerName + "," + ch.playerInfo.kills + "," + ch.playerInfo.deaths + "," + ch.playerInfo.hp);
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
                    playerInfo.characterId = data;
                    broadcast("CHAT:" + playerName + " 님이 " + com.fpsgame.common.CharacterData.getById(data).name + " 캐릭터를 선택했습니다!", null);
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
                    int redCount = 0, blueCount = 0; boolean allReady = true;
                    for (Map.Entry<String, ClientHandler> e : clients.entrySet()) {
                        ClientHandler ch = e.getValue();
                        if (ch.playerInfo == null) continue;
                        if (ch.playerInfo.team == GameConstants.TEAM_RED) redCount++;
                        else if (ch.playerInfo.team == GameConstants.TEAM_BLUE) blueCount++;
                        if (!ch.ready) allReady = false;
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
                    break;

                case "POS":
                    String[] coords = data.split(",");
                    if (coords.length >= 2) {
                        playerInfo.x = Float.parseFloat(coords[0]);
                        playerInfo.y = Float.parseFloat(coords[1]);
                        String posUpdate = "PLAYER:" + playerName + "," + playerInfo.x + "," + playerInfo.y + "," + playerInfo.team + "," + playerInfo.hp;
                        broadcast(posUpdate, playerName);
                    }
                    break;

                case "SHOOT":
                    broadcast("SHOOT:" + playerName + "," + data, playerName);
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
                        target.playerInfo.hp -= GameConstants.MISSILE_DAMAGE;
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
                    }
                    break;

                case "HITME":
                    // 피해자(현재 클라이언트)가 자신이 피격되었음을 신고. data = shooterName
                    String shooterName = data;
                    ClientHandler shooter = clients.get(shooterName);
                    if (playerInfo != null) {
                        long now = System.currentTimeMillis();
                        // 스폰 보호 중이거나 이미 사망 상태면 무시
                        if (now < spawnProtectedUntil || playerInfo.hp <= 0) break;
                        playerInfo.hp -= GameConstants.MISSILE_DAMAGE;
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
                    }
                    break;

                case "DEATH":
                    playerInfo.hp = 0; playerInfo.deaths++;
                    broadcastStats(playerName, playerInfo);
                    broadcast("CHAT:" + playerName + " 님이 사망했습니다!", null);
                    break;

                case "RESPAWN":
                    String[] resp = data.split(",");
                    if (resp.length >= 2) {
                        playerInfo.x = Float.parseFloat(resp[0]);
                        playerInfo.y = Float.parseFloat(resp[1]);
                        playerInfo.hp = GameConstants.MAX_HP;
                        // 3초 스폰 보호 시작
                        spawnProtectedUntil = System.currentTimeMillis() + 3000;
                        broadcastStats(playerName, playerInfo);
                        String posUpdate = "PLAYER:" + playerName + "," + playerInfo.x + "," + playerInfo.y + "," + playerInfo.team + "," + playerInfo.hp;
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
                } catch (IOException ignored) {}
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
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
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
}
