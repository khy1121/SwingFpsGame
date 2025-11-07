package com.fpsgame.client;

import com.fpsgame.common.CharacterData;
import com.fpsgame.common.GameConstants;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * 게임 패널 - 기존 디자인 유지하면서 간단한 로직
 * 예제 코드처럼 단순한 게임 화면
 */
public class GamePanel extends JFrame implements KeyListener {
    
    private String playerName;
    private int team;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    
    private javax.swing.Timer timer;
    private int playerX = 400;
    private int playerY = 300;
    private final int SPEED = 5;
    private boolean[] keys = new boolean[256];
    
    // 선택된 캐릭터
    private String selectedCharacter = "raven"; // 기본값
    private CharacterData currentCharacterData;
    
    // 다른 플레이어들
    private Map<String, PlayerData> players = new HashMap<>();
    
    // 미사일 리스트
    private List<Missile> missiles = new ArrayList<>();
    
    // 게임 패널
    private GameCanvas canvas;
    
    // 채팅 UI
    private JTextArea chatArea;
    private JTextField chatInput;
    private JScrollPane chatScroll;
    
    // 킬/데스 카운터
    private int kills = 0;
    private int deaths = 0;
    private int myHP = GameConstants.MAX_HP;
    
    class PlayerData {
        int x, y;
        int targetX, targetY; // 보간을 위한 목표 위치
        int team;
        int hp;
        int kills;
        int deaths;
        
        PlayerData(int x, int y, int team) {
            this.x = x;
            this.y = y;
            this.targetX = x;
            this.targetY = y;
            this.team = team;
            this.hp = GameConstants.MAX_HP;
            this.kills = 0;
            this.deaths = 0;
        }
        
        // 부드러운 보간 업데이트
        void smoothUpdate() {
            // 목표 위치로 부드럽게 이동 (보간 계수 0.5 = 50% 이동 - 더 빠른 반응)
            float interpolation = 0.5f;
            x += (int)((targetX - x) * interpolation);
            y += (int)((targetY - y) * interpolation);
        }
    }
    
    class Missile {
        int x, y;
        int dx, dy;
        int team;
        String owner; // 발사자 이름(킬 크레딧/피격 리포트용)
        
        Missile(int x, int y, int dx, int dy, int team, String owner) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.team = team;
            this.owner = owner;
        }
    }
    
    class GameCanvas extends JPanel {
        public GameCanvas() {
            setPreferredSize(new Dimension(GameConstants.GAME_WIDTH, GameConstants.GAME_HEIGHT));
            setBackground(new Color(20, 25, 35));
            setFocusable(true);
            addKeyListener(GamePanel.this);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 그리드 배경
            drawGrid(g2d);
            
            // 다른 플레이어들 그리기
            for (Map.Entry<String, PlayerData> entry : players.entrySet()) {
                PlayerData p = entry.getValue();
                Color playerColor = p.team == GameConstants.TEAM_RED ? 
                    new Color(244, 67, 54) : new Color(33, 150, 243);
                
                g2d.setColor(playerColor);
                g2d.fillOval(p.x - 20, p.y - 20, 40, 40);
                
                // 이름 표시
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                FontMetrics fm = g2d.getFontMetrics();
                String name = entry.getKey();
                int nameWidth = fm.stringWidth(name);
                g2d.drawString(name, p.x - nameWidth / 2, p.y - 25);
                
                // HP 바
                drawHealthBar(g2d, p.x, p.y + 25, p.hp);
            }
            
            // 로컬 플레이어 그리기
            Color myColor = team == GameConstants.TEAM_RED ? 
                new Color(255, 100, 100) : new Color(100, 150, 255);
            g2d.setColor(myColor);
            g2d.fillOval(playerX - 20, playerY - 20, 40, 40);
            
            // 내 이름
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            int nameWidth = fm.stringWidth(playerName + " (You)");
            g2d.drawString(playerName + " (You)", playerX - nameWidth / 2, playerY - 25);
            
            // 미사일 그리기
            g2d.setColor(Color.YELLOW);
            for (Missile m : missiles) {
                g2d.fillOval(m.x - 4, m.y - 4, 8, 8);
            }
            
            // HUD
            drawHUD(g2d);
        }
        
        private void drawGrid(Graphics2D g) {
            g.setColor(new Color(30, 35, 45));
            for (int x = 0; x < getWidth(); x += 50) {
                g.drawLine(x, 0, x, getHeight());
            }
            for (int y = 0; y < getHeight(); y += 50) {
                g.drawLine(0, y, getWidth(), y);
            }
        }
        
        private void drawHealthBar(Graphics2D g, int x, int y, int hp) {
            int barWidth = 40;
            int barHeight = 5;
            
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x - barWidth / 2, y, barWidth, barHeight);
            
            g.setColor(Color.GREEN);
            int currentWidth = (int) (barWidth * (hp / 100.0));
            g.fillRect(x - barWidth / 2, y, currentWidth, barHeight);
        }
        
        private void drawHUD(Graphics2D g) {
            // 배경
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(10, 10, 250, 160);
            
            // 폰트 설정
            g.setFont(new Font("맑은 고딕", Font.BOLD, 14));
            g.setColor(Color.WHITE);
            
            int yPos = 30;
            g.drawString("플레이어: " + playerName, 20, yPos);
            yPos += 20;
            g.drawString("팀: " + (team == GameConstants.TEAM_RED ? "RED" : "BLUE"), 20, yPos);
            yPos += 20;
            g.drawString("캐릭터: " + currentCharacterData.name, 20, yPos);
            yPos += 20;
            
            // HP 바
            g.drawString("HP: " + myHP + "/" + GameConstants.MAX_HP, 20, yPos);
            drawHealthBar(g, 130, yPos - 12, myHP);
            yPos += 20;
            
            // 킬/데스
            g.setColor(new Color(255, 215, 0)); // 골드
            g.drawString("Kills: " + kills + " / Deaths: " + deaths, 20, yPos);
            yPos += 20;
            
            // 캐릭터 스탯 표시
            g.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
            g.setColor(new Color(255, 200, 200));
            g.drawString("HP: " + (int)currentCharacterData.health, 20, yPos);
            yPos += 18;
            g.setColor(new Color(200, 255, 200));
            g.drawString("속도: " + String.format("%.1f", currentCharacterData.speed), 20, yPos);
            
            // 도움말
            g.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            g.setColor(Color.YELLOW);
            g.drawString("B키: 캐릭터 선택", 20, getHeight() - 20);
        }
    }
    
    public GamePanel(String playerName, int team, Socket socket, DataOutputStream out, DataInputStream in) {
        super("FPS Game - " + playerName);
        this.playerName = playerName;
        this.team = team;
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.currentCharacterData = CharacterData.getById(selectedCharacter);
        
        initUI();
        startGame();
    }
    
    private void initUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        
        // 메인 패널 레이아웃 설정
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 게임 캔버스 (왼쪽)
        canvas = new GameCanvas();
        mainPanel.add(canvas, BorderLayout.CENTER);
        
        // 채팅 패널 (오른쪽)
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setPreferredSize(new Dimension(250, GameConstants.GAME_HEIGHT));
        chatPanel.setBackground(new Color(32, 34, 37));
        
        // 채팅 영역
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        chatArea.setBackground(new Color(47, 49, 54));
        chatArea.setForeground(Color.WHITE);
        
        chatScroll = new JScrollPane(chatArea);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatPanel.add(chatScroll, BorderLayout.CENTER);
        
        // 채팅 입력 필드
        chatInput = new JTextField();
        chatInput.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        chatInput.setBackground(new Color(64, 68, 75));
        chatInput.setForeground(Color.WHITE);
        chatInput.setCaretColor(Color.WHITE);
        chatInput.addActionListener(e -> sendChatMessage());
        
        // 채팅 입력창에서 ESC 누르면 게임으로 포커스 복귀
        chatInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    canvas.requestFocusInWindow();
                }
            }
        });
        
        chatPanel.add(chatInput, BorderLayout.SOUTH);
        
        mainPanel.add(chatPanel, BorderLayout.EAST);
        
        setContentPane(mainPanel);
        
        pack();
        setLocationRelativeTo(null);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                // 시작 시 즉시 게임 캔버스에 포커스 줘서 조작 가능하게
                SwingUtilities.invokeLater(() -> canvas.requestFocusInWindow());
            }

            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (out != null) {
                        out.writeUTF("QUIT");
                        out.flush();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                disconnect();
            }
        });
    }
    
    private void sendChatMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty() && out != null) {
            try {
                // 서버로 전송 (로컬 에코 제거 - 서버 응답만 표시)
                out.writeUTF("CHAT:" + message);
                out.flush();
            } catch (IOException ex) {
                appendChatMessage("채팅 전송 실패");
            }
            chatInput.setText("");
        }
        // 채팅 전송 후 게임 캔버스로 포커스 복귀
        canvas.requestFocusInWindow();
    }
    
    // 채팅/시스템 로그 스로틀링
    private String lastChatMessage = null;
    private long lastChatTime = 0L;
    private static final long CHAT_THROTTLE_MS = 1000;

    private void appendChatMessage(String message) {
        long now = System.currentTimeMillis();
        if (message != null && message.equals(lastChatMessage) && (now - lastChatTime) < CHAT_THROTTLE_MS) {
            return; // 동일 메시지 연속 출력 최소화
        }
        lastChatMessage = message;
        lastChatTime = now;
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
    
    private void startGame() {
        // 게임 업데이트 타이머
        timer = new javax.swing.Timer(16, e -> {
            updateGame();
            canvas.repaint();
        });
        timer.start();
        
        // 서버 메시지 수신 스레드
        new Thread(this::receiveGameUpdates).start();
    }
    
    private void updateGame() {
        updatePlayerPosition();
        updateMissiles();
        checkCollisions();
        
        // 모든 다른 플레이어의 위치를 부드럽게 보간
        for (PlayerData pd : players.values()) {
            pd.smoothUpdate();
        }
    }
    
    private void updatePlayerPosition() {
        int oldX = playerX;
        int oldY = playerY;
        
        if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP]) {
            playerY -= SPEED;
        }
        if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN]) {
            playerY += SPEED;
        }
        if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT]) {
            playerX -= SPEED;
        }
        if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT]) {
            playerX += SPEED;
        }
        
        // 경계 체크
        playerX = Math.max(20, Math.min(playerX, GameConstants.GAME_WIDTH - 20));
        playerY = Math.max(20, Math.min(playerY, GameConstants.GAME_HEIGHT - 20));
        
        // 위치가 변경되면 서버에 전송 (움직일 때만 전송하여 네트워크 부하 감소)
        if (oldX != playerX || oldY != playerY) {
            sendPosition();
        }
    }
    
    private void updateMissiles() {
        Iterator<Missile> it = missiles.iterator();
        while (it.hasNext()) {
            Missile m = it.next();
            m.x += m.dx;
            m.y += m.dy;
            
            // 화면 밖으로 나가면 제거
            if (m.x < 0 || m.x > GameConstants.GAME_WIDTH || 
                m.y < 0 || m.y > GameConstants.GAME_HEIGHT) {
                it.remove();
            }
        }
    }
    
    private void checkCollisions() {
        // 내 미사일과 다른 플레이어 충돌 (서버 권위 유지: 로컬에서는 시각 효과만, 데미지/리포트는 피해자 측에서 수행)
        Iterator<Missile> it = missiles.iterator();
        while (it.hasNext()) {
            Missile m = it.next();
            if (m.team == team) {
                for (Map.Entry<String, PlayerData> entry : players.entrySet()) {
                    PlayerData p = entry.getValue();
                    if (p.team != team) {
                        double dist = Math.sqrt(Math.pow(m.x - p.x, 2) + Math.pow(m.y - p.y, 2));
                        if (dist < 20) {
                            it.remove();
                            break;
                        }
                    }
                }
            }
        }

        // 적 미사일과 내가 맞았는지 체크 (피해자 측 리포트)
        Iterator<Missile> enemyIt = missiles.iterator();
        while (enemyIt.hasNext()) {
            Missile m = enemyIt.next();
            if (m.team != team) {
                double dist = Math.sqrt(Math.pow(m.x - playerX, 2) + Math.pow(m.y - playerY, 2));
                if (dist < 20) {
                    enemyIt.remove();
                    try {
                        if (m.owner != null) {
                            out.writeUTF("HITME:" + m.owner);
                        } else {
                            // 소유자 정보를 모르면 기존 프로토콜로 최소한의 동작 보장
                            out.writeUTF("DEATH");
                        }
                        out.flush();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    break;
                }
            }
        }
    }
    
    private void respawn() {
        // 랜덤 위치에서 리스폰
        Random rand = new Random();
        if (team == GameConstants.TEAM_RED) {
            playerX = rand.nextInt(200) + 100; // 왼쪽 영역
        } else {
            playerX = rand.nextInt(200) + (GameConstants.GAME_WIDTH - 300); // 오른쪽 영역
        }
        playerY = rand.nextInt(GameConstants.GAME_HEIGHT - 100) + 50;
        myHP = GameConstants.MAX_HP;
        
        // 서버에 리스폰 알림
        try {
            out.writeUTF("RESPAWN:" + playerX + "," + playerY);
            out.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        sendPosition();
    }
    
    private void sendPosition() {
        if (out != null) {
            try {
                out.writeUTF("POS:" + playerX + "," + playerY);
                out.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private void shootMissile() {
        // 마우스 위치 방향으로 발사 (여기서는 위쪽으로 고정)
    Missile missile = new Missile(playerX, playerY, 0, -GameConstants.MISSILE_SPEED, team, playerName);
        missiles.add(missile);
        
        if (out != null) {
            try {
                out.writeUTF("SHOOT:" + playerX + "," + playerY + ",0,-" + GameConstants.MISSILE_SPEED);
                out.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private void receiveGameUpdates() {
        try {
            String message;
            while ((message = in.readUTF()) != null) {
                processGameMessage(message);
            }
        } catch (IOException ex) {
            System.out.println("Disconnected from game server.");
        }
    }
    
    private void processGameMessage(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length < 2) return;
        
        String command = parts[0];
        String data = parts[1];
        
        switch (command) {
            case "CHAT":
                // 채팅 메시지 표시 (서버에서 받은 모든 메시지 표시)
                appendChatMessage(data);
                break;
                
            case "PLAYER":
                // PLAYER:name,x,y,team,hp
                String[] playerData = data.split(",");
                if (playerData.length >= 5) {
                    String name = playerData[0];
                    if (!name.equals(playerName)) {
                        int x = (int) Float.parseFloat(playerData[1]);
                        int y = (int) Float.parseFloat(playerData[2]);
                        int t = Integer.parseInt(playerData[3]);
                        int hp = Integer.parseInt(playerData[4]);
                        
                        PlayerData pd = players.get(name);
                        if (pd == null) {
                            pd = new PlayerData(x, y, t);
                            pd.hp = hp; // 새 플레이어도 HP 설정
                            players.put(name, pd);
                        } else {
                            // 보간을 위해 목표 위치 설정 (즉시 이동하지 않음)
                            pd.targetX = x;
                            pd.targetY = y;
                            pd.hp = hp;
                        }
                    }
                }
                break;
                
            case "REMOVE":
                // 플레이어 제거
                players.remove(data);
                break;
                
            case "KILL":
                // 처치 알림(스탯 증가는 서버에서 STATS로 동기화)
                appendChatMessage(">>> 당신이 " + data + "를 처치했습니다!");
                break;
            
            case "STATS":
                // STATS:name,kills,deaths,hp
                String[] s = data.split(",");
                if (s.length >= 4) {
                    String name = s[0];
                    int k = Integer.parseInt(s[1]);
                    int d = Integer.parseInt(s[2]);
                    int hp = Integer.parseInt(s[3]);
                    if (name.equals(playerName)) {
                        kills = k;
                        deaths = d;
                        myHP = hp;
                        if (myHP <= 0) {
                            // 서버 기준으로 사망 상태면 즉시 리스폰
                            respawn();
                        }
                    } else {
                        PlayerData pd = players.get(name);
                        if (pd != null) {
                            pd.kills = k;
                            pd.deaths = d;
                            pd.hp = hp;
                        }
                    }
                }
                break;
                
            case "SHOOT":
                // SHOOT:playerName,x,y,dx,dy
                String[] shootData = data.split(",");
                if (shootData.length >= 5) {
                    String shooter = shootData[0];
                    if (!shooter.equals(playerName)) {
                        int sx = (int) Float.parseFloat(shootData[1]);
                        int sy = (int) Float.parseFloat(shootData[2]);
                        int dx = (int) Float.parseFloat(shootData[3]);
                        int dy = (int) Float.parseFloat(shootData[4]);
                        
                        // 다른 플레이어가 발사한 미사일 생성
                        PlayerData pd = players.get(shooter);
                        if (pd != null) {
                            Missile m = new Missile(sx, sy, dx, dy, pd.team, shooter);
                            missiles.add(m);
                        }
                    }
                }
                break;
        }
    }
    
    private void disconnect() {
        try {
            if (timer != null) timer.stop();
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
        
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            shootMissile();
        } else if (e.getKeyCode() == KeyEvent.VK_B) {
            // B키: 캐릭터 선택 다이얼로그 표시
            openCharacterSelect();
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_T) {
            // Enter 또는 T키: 채팅 입력창으로 포커스 이동
            chatInput.requestFocusInWindow();
        }
    }
    
    private void openCharacterSelect() {
        // 게임 일시정지
        if (timer != null) {
            timer.stop();
        }
        
        // 캐릭터 선택 다이얼로그 표시
        String newCharacter = CharacterSelectDialog.showDialog(this);
        
        if (newCharacter != null) {
            selectedCharacter = newCharacter;
            currentCharacterData = CharacterData.getById(selectedCharacter);
            
            // 서버에 캐릭터 변경 알림
            if (out != null) {
                try {
                    out.writeUTF("CHARACTER:" + selectedCharacter);
                    out.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            
            // HUD에 캐릭터 정보 표시
            canvas.repaint();
        }
        
        // 게임 재개
        if (timer != null) {
            timer.start();
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
    }
}
