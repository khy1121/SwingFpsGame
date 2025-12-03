package com.fpsgame.client;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * NetworkClient - 네트워크 통신 전담 클래스
 * GamePanel에서 분리하여 서버와의 소켓 통신을 관리
 * 
 * 책임:
 * - 소켓 연결 관리 (생성/종료)
 * - 메시지 송신 (sendMessage 계열 메서드)
 * - 메시지 수신 스레드 관리
 * - 수신된 메시지를 콜백으로 전달
 * 
 * 비책임 (GamePanel이 처리):
 * - 프로토콜 파싱 (CHAT:, PLAYER:, SHOOT: 등)
 * - 게임 상태 업데이트 (플레이어 위치, HP, 스킬 등)
 * - UI 업데이트 (채팅, 애니메이션 등)
 */
public class NetworkClient {
    
    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;
    private Thread receiveThread;
    private volatile boolean running = false;
    
    // 메시지 처리 콜백
    private Consumer<String> onMessageReceived;
    
    public NetworkClient(Socket socket, DataOutputStream out, DataInputStream in) {
        this.socket = socket;
        this.out = out;
        this.in = in;
    }
    
    /**
     * 메시지 수신 콜백 등록
     */
    public void setOnMessageReceived(Consumer<String> callback) {
        this.onMessageReceived = callback;
    }
    
    /**
     * 메시지 수신 스레드 시작
     */
    public void startReceiving() {
        if (running) return;
        
        running = true;
        receiveThread = new Thread(this::receiveGameUpdates, "NetworkReceiveThread");
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
    
    /**
     * 네트워크 연결 종료
     */
    public void stop() {
        running = false;
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("소켓 종료 중 오류: " + e.getMessage());
        }
    }
    
    /**
     * 서버로 메시지 전송
     */
    public synchronized void sendMessage(String message) {
        if (out == null) return;
        
        try {
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("메시지 전송 실패: " + e.getMessage());
        }
    }
    
    /**
     * 위치 업데이트 전송
     */
    public void sendPosition(int x, int y, int direction) {
        sendMessage("POS:" + x + "," + y + "," + direction);
    }
    
    /**
     * 발사 이벤트 전송
     */
    public void sendShoot(int x, int y, int dx, int dy) {
        sendMessage("SHOOT:" + x + "," + y + "," + dx + "," + dy);
    }
    
    /**
     * 스킬 사용 전송 (메시지 데이터 직접 전달)
     */
    public void sendSkillUse(String skillData) {
        sendMessage("SKILL:" + skillData);
    }
    
    /**
     * 채팅 메시지 전송
     */
    public void sendChat(String message) {
        sendMessage("CHAT:" + message);
    }
    
    /**
     * 캐릭터 선택 전송
     */
    public void sendCharacterSelect(String characterId) {
        sendMessage("CHARACTER_SELECT:" + characterId);
    }
    
    /**
     * 피격 리포트 전송
     */
    public void sendHitReport(String hitData) {
        sendMessage(hitData);  // 이미 "HIT_OBJ:" 또는 "HITME:" prefix 포함
    }
    
    /**
     * 오브젝트 설치 전송
     */
    public void sendPlaceObject(String type, int x, int y) {
        sendMessage("PLACE_OBJECT:" + type + "," + x + "," + y);
    }
    
    // ========== 내부 메서드 ==========
    
    private void receiveGameUpdates() {
        try {
            String message;
            while (running && (message = in.readUTF()) != null) {
                if (onMessageReceived != null) {
                    onMessageReceived.accept(message);
                }
            }
        } catch (IOException ex) {
            if (running) {
                System.out.println("서버와의 연결이 끊어졌습니다.");
            }
        }
    }
    
    /**
     * 연결 상태 확인
     */
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && running;
    }
}
