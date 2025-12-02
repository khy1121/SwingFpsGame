package com.fpsgame.client;

import com.fpsgame.common.GameConstants;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;
import javax.swing.border.*;

/**
 * 로비 프레임 - 개선된 UI
 * 맵 선택, 팀 선택, 채팅, 플레이어 목록
 */
public class LobbyFrame extends JFrame {

    // 커스텀 버튼: 전체 배경을 solid color로 채움 (hover/pressed/disabled 포함)
    private static class FilledButton extends JButton {
        private Color disabledBg = new Color(100, 100, 100);

        FilledButton(String text, Color bg) {
            super(text);
            setBackground(bg);
            setForeground(Color.BLACK);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        }

        void setDisabledBackground(Color c) {
            this.disabledBg = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color fill;
            if (!isEnabled()) {
                fill = disabledBg;
            } else if (getModel().isPressed() || getModel().isSelected()) {
                fill = darker(getBackground(), 0.4f);
            } else if (getModel().isRollover()) {
                fill = brighter(getBackground(), 0.05f);
            } else {
                fill = getBackground();
            }

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            // 선택된 버튼은 노란 테두리 표시
            if (getModel().isSelected()) {
                g2.setColor(new Color(255, 220, 0));
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
            }

            g2.dispose();

            super.paintComponent(g);
        }

        private static Color brighter(Color c, float factor) {
            int r = Math.min(255, (int) (c.getRed() + 255 * factor));
            int g = Math.min(255, (int) (c.getGreen() + 255 * factor));
            int b = Math.min(255, (int) (c.getBlue() + 255 * factor));
            return new Color(r, g, b, c.getAlpha());
        }

        private static Color darker(Color c, float factor) {
            int r = Math.max(0, (int) (c.getRed() - 255 * factor));
            int g = Math.max(0, (int) (c.getGreen() - 255 * factor));
            int b = Math.max(0, (int) (c.getBlue() - 255 * factor));
            return new Color(r, g, b, c.getAlpha());
        }
    }

    private String playerName;
    private JTextField serverField;
    private JSpinner portSpinner;
    private JButton connectButton;
    private JButton readyButton;
    private JButton startButton;
    private JButton redTeamButton;
    private JButton blueTeamButton;
    private JButton characterSelectButton; // 캐릭터 선택 버튼 추가
    private JTextArea chatArea;
    private JTextField chatInput;

    private JPanel redTeamList;
    private JPanel blueTeamList;
    private JLabel[] redSlots = new JLabel[5];
    private JLabel[] blueSlots = new JLabel[5];

    private String selectedMap = "map";

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private int selectedTeam = -1; // -1=미선택, 0=RED, 1=BLUE
    private boolean isReady = false;
    // 로비 수신 스레드 제어용
    private volatile boolean lobbyListening = false;
    private Thread lobbyThread;

    // 팀 로스터 (서버로부터 받아 업데이트)
    private java.util.Map<String, Integer> playerTeams = new java.util.HashMap<>();
    private java.util.Set<String> readyPlayers = new java.util.HashSet<>();
    private java.util.Map<String, String> playerCharacters = new java.util.HashMap<>(); // 플레이어별 선택한 캐릭터

    public LobbyFrame(String playerName) {
        super("FPS 게임");
        this.playerName = playerName;
        initUI();

        // 프레임이 표시된 후 자동으로 서버 연결
        SwingUtilities.invokeLater(() -> {
            connectToServer();
        });

        // 저장된 캐릭터 로드
        String savedChar = GameConfig.loadCharacter();
        if (savedChar != null && !savedChar.isEmpty()) {
            selectedCharacterId = savedChar;
            System.out.println("[Lobby] Loaded saved character: " + selectedCharacterId);
        }
    }

    private void initUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 800);
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout(0, 0));

        // 다크 배경색
        Color bgDark = new Color(32, 34, 37);
        Color panelBg = new Color(47, 49, 54);
        Color slotBg = new Color(55, 57, 63);

        // 한글 폰트 설정
        Font koreanFont = new Font("맑은 고딕", Font.PLAIN, 14);
        Font koreanBold = new Font("맑은 고딕", Font.BOLD, 16);

        // 상단 패널 - 서버 정보와 맵 선택
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(bgDark);
        topPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        // 서버 정보
        JPanel serverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        serverPanel.setBackground(bgDark);

        JLabel hostLabel = new JLabel("Host:");
        hostLabel.setForeground(Color.WHITE);
        hostLabel.setFont(koreanFont);
        serverPanel.add(hostLabel);

        serverField = new JTextField("127.0.0.1", 10);
        serverField.setFont(koreanFont);
        serverPanel.add(serverField);

        JLabel portLabel = new JLabel("Port:");
        portLabel.setForeground(Color.WHITE);
        portLabel.setFont(koreanFont);
        serverPanel.add(portLabel);

        portSpinner = new JSpinner(new SpinnerNumberModel(GameConstants.DEFAULT_PORT, 1, 65535, 1));
        portSpinner.setFont(koreanFont);
        serverPanel.add(portSpinner);

        connectButton = new FilledButton("연결 중...", new Color(67, 181, 129));
        connectButton.setFont(koreanBold);
        connectButton.setEnabled(false); // 자동 연결이므로 버튼 비활성화
        serverPanel.add(connectButton);

        topPanel.add(serverPanel, BorderLayout.WEST);

        // 맵 정보 라벨
        JPanel mapInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        mapInfoPanel.setBackground(bgDark);

        JLabel mapInfoLabel = new JLabel("Map Info");
        mapInfoLabel.setFont(koreanBold);
        mapInfoLabel.setForeground(Color.WHITE);
        mapInfoPanel.add(mapInfoLabel);

        JLabel charSelectLabel = new JLabel("Character Select");
        charSelectLabel.setFont(koreanBold);
        charSelectLabel.setForeground(Color.LIGHT_GRAY);
        mapInfoPanel.add(charSelectLabel);

        topPanel.add(mapInfoPanel, BorderLayout.CENTER);

        // 중앙 - 맵 선택과 팀 구성
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(panelBg);

        // 맵 선택 패널 (4개의 맵 카드)
        JPanel mapSelectPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        mapSelectPanel.setBackground(panelBg);
        mapSelectPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] maps = { "Map", "Map 2", "Map 3", "Village" };
        String[] mapIds = { "map", "map2", "map3", "village" };
        String[] mapDescs = { "장거리, 근거리 교전이 섞인 복합 맵.", "통로가 많은 맵이니 뒤를 조심하세요",
                "섬이 다양하고 길이 좁으니, 근접전이 많습니다.", "기본맵과 동일한 구조입니다. " };

        for (int i = 0; i < maps.length; i++) {
            final String mapId = mapIds[i];
            final String mapDesc = mapDescs[i];
            JPanel mapCard = createMapCard(maps[i], mapId, mapDesc, koreanFont, slotBg);
            mapSelectPanel.add(mapCard);
        }

        centerPanel.add(mapSelectPanel, BorderLayout.NORTH);

        // 팀 선택 버튼 패널 (맵 선택 아래, 별도 영역)
        JPanel teamSelectPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        teamSelectPanel.setBackground(panelBg);
        teamSelectPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        redTeamButton = new FilledButton("RED TEAM", new Color(220, 80, 60));
        redTeamButton.setFont(koreanBold);
        redTeamButton.setPreferredSize(new Dimension(0, 50));
        redTeamButton.addActionListener(e -> selectTeam(GameConstants.TEAM_RED));

        blueTeamButton = new FilledButton("BLUE TEAM", new Color(100, 150, 255));
        blueTeamButton.setFont(koreanBold);
        blueTeamButton.setPreferredSize(new Dimension(0, 50));
        blueTeamButton.addActionListener(e -> selectTeam(GameConstants.TEAM_BLUE));

        teamSelectPanel.add(redTeamButton);
        teamSelectPanel.add(blueTeamButton);

        // 하단 - 팀 목록과 채팅
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(panelBg);
        bottomPanel.setBorder(new EmptyBorder(0, 20, 20, 20));

        // 팀 목록과 채팅
        JPanel teamsAndChatPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        teamsAndChatPanel.setBackground(panelBg);

        // RED TEAM 목록
        JPanel redPanel = new JPanel(new BorderLayout());
        redPanel.setBackground(slotBg);
        redPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(185, 65, 50), 2),
                new EmptyBorder(10, 10, 10, 10)));

        JLabel redLabel = new JLabel("RED TEAM", SwingConstants.CENTER);
        redLabel.setFont(koreanBold);
        redLabel.setForeground(new Color(244, 100, 80));
        redPanel.add(redLabel, BorderLayout.NORTH);

        redTeamList = new JPanel(new GridLayout(5, 1, 0, 5));
        redTeamList.setBackground(slotBg);
        redTeamList.setBorder(new EmptyBorder(10, 0, 0, 0));
        for (int i = 0; i < 5; i++) {
            redSlots[i] = new JLabel("Empty", SwingConstants.CENTER);
            redSlots[i].setFont(koreanFont);
            redSlots[i].setForeground(Color.GRAY);
            redSlots[i].setOpaque(true);
            redSlots[i].setBackground(new Color(65, 67, 73));
            redSlots[i].setBorder(new EmptyBorder(8, 5, 8, 5));
            redTeamList.add(redSlots[i]);
        }
        redPanel.add(redTeamList, BorderLayout.CENTER);

        // BLUE TEAM 목록
        JPanel bluePanel = new JPanel(new BorderLayout());
        bluePanel.setBackground(slotBg);
        bluePanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(70, 110, 180), 2),
                new EmptyBorder(10, 10, 10, 10)));

        JLabel blueLabel = new JLabel("BLUE TEAM", SwingConstants.CENTER);
        blueLabel.setFont(koreanBold);
        blueLabel.setForeground(new Color(100, 150, 255));
        bluePanel.add(blueLabel, BorderLayout.NORTH);

        blueTeamList = new JPanel(new GridLayout(5, 1, 0, 5));
        blueTeamList.setBackground(slotBg);
        blueTeamList.setBorder(new EmptyBorder(10, 0, 0, 0));
        for (int i = 0; i < 5; i++) {
            blueSlots[i] = new JLabel("Empty", SwingConstants.CENTER);
            blueSlots[i].setFont(koreanFont);
            blueSlots[i].setForeground(Color.GRAY);
            blueSlots[i].setOpaque(true);
            blueSlots[i].setBackground(new Color(65, 67, 73));
            blueSlots[i].setBorder(new EmptyBorder(8, 5, 8, 5));
            blueTeamList.add(blueSlots[i]);
        }
        bluePanel.add(blueTeamList, BorderLayout.CENTER);

        // 채팅 패널
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(slotBg);
        chatPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(80, 82, 88), 1),
                new EmptyBorder(10, 10, 10, 10)));

        JLabel chatLabel = new JLabel("Chat", SwingConstants.CENTER);
        chatLabel.setFont(koreanBold);
        chatLabel.setForeground(Color.WHITE);
        chatPanel.add(chatLabel, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(40, 42, 48));
        chatArea.setForeground(Color.WHITE);
        chatArea.setFont(koreanFont);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(new EmptyBorder(5, 0, 5, 0));
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel chatInputPanel = new JPanel(new BorderLayout(5, 0));
        chatInputPanel.setBackground(slotBg);
        chatInput = new JTextField();
        chatInput.setFont(koreanFont);
        chatInput.setBackground(new Color(55, 57, 63));
        chatInput.setForeground(Color.WHITE);
        chatInput.setCaretColor(Color.WHITE);
        chatInput.setBorder(new CompoundBorder(
                new LineBorder(new Color(70, 72, 78), 1),
                new EmptyBorder(5, 8, 5, 8)));
        chatInput.addActionListener(e -> sendChat());
        chatInput.setEnabled(false);

        JButton sendButton = new FilledButton("전송", new Color(88, 101, 242));
        sendButton.setFont(koreanBold);
        sendButton.addActionListener(e -> sendChat());

        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);

        teamsAndChatPanel.add(redPanel);
        teamsAndChatPanel.add(bluePanel);
        teamsAndChatPanel.add(chatPanel);

        bottomPanel.add(teamsAndChatPanel, BorderLayout.CENTER);

        // 하단 버튼 패널 (READY, CHARACTER SELECT, START)
        JPanel readyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        readyPanel.setBackground(panelBg);

        // 캐릭터 선택 버튼
        characterSelectButton = new FilledButton("캐릭터 선택", new Color(150, 100, 255));
        characterSelectButton.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        characterSelectButton.setPreferredSize(new Dimension(180, 50));
        characterSelectButton.setEnabled(false); // 팀 선택 후 활성화
        characterSelectButton.addActionListener(e -> openCharacterSelect());

        readyButton = new FilledButton("READY", new Color(67, 181, 129));
        readyButton.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        readyButton.setPreferredSize(new Dimension(180, 50));
        readyButton.setEnabled(false); // 캐릭터 선택 후 활성화
        readyButton.addActionListener(e -> toggleReady());

        // 별도 START 버튼
        startButton = new FilledButton("START", new Color(244, 67, 54));
        startButton.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        startButton.setPreferredSize(new Dimension(180, 50));
        startButton.setEnabled(false);
        startButton.addActionListener(e -> startGame());

        readyPanel.add(characterSelectButton);
        readyPanel.add(readyButton);
        readyPanel.add(startButton);
        bottomPanel.add(readyPanel, BorderLayout.SOUTH);

        // 맵 아래에 팀 선택 버튼, 그 아래에 팀 목록/채팅/READY가 오도록 중간 래퍼 사용
        JPanel midPanel = new JPanel(new BorderLayout());
        midPanel.setBackground(panelBg);
        midPanel.add(teamSelectPanel, BorderLayout.NORTH);
        midPanel.add(bottomPanel, BorderLayout.CENTER);
        centerPanel.add(midPanel, BorderLayout.CENTER);

        // 레이아웃 조립
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(centerPanel, BorderLayout.CENTER);

        // 메뉴바 추가
        createMenuBar();

        updateTeamButtons();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(230, 230, 230)); // 밝은 회색 배경

        JMenu optionsMenu = new JMenu("옵션");
        optionsMenu.setForeground(Color.BLACK); // 검정 텍스트
        optionsMenu.setFont(new Font("맑은 고딕", Font.PLAIN, 14));

        JMenuItem settingsItem = new JMenuItem("설정");
        settingsItem.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        settingsItem.setForeground(Color.BLACK); // 검정 텍스트
        settingsItem.addActionListener(e -> {
            OptionDialog dialog = new OptionDialog(this);
            dialog.setVisible(true);
        });

        optionsMenu.add(settingsItem);
        menuBar.add(optionsMenu);

        setJMenuBar(menuBar);
    }

    private JPanel createMapCard(String mapName, String mapId, String mapDesc, Font font, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(bgColor);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(80, 82, 88), 1),
                new EmptyBorder(10, 10, 10, 10)));

        // 맵 이미지 영역
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBackground(new Color(45, 47, 53));
        imagePanel.setPreferredSize(new Dimension(0, 150));
        imagePanel.setBorder(new LineBorder(new Color(60, 62, 68), 1));

        // 맵 이미지 로드 시도
        try {
            String imagePath = "assets" + File.separator + "maps" + File.separator + mapId + ".png";
            File imageFile = new File(imagePath);

            if (imageFile.exists()) {
                ImageIcon originalIcon = new ImageIcon(imagePath);
                Image scaledImage = originalIcon.getImage().getScaledInstance(280, 150, Image.SCALE_SMOOTH);
                JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
                imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                imagePanel.add(imageLabel, BorderLayout.CENTER);
            } else {
                // 이미지가 없으면 플레이스홀더 표시
                JLabel placeholder = new JLabel("No Image", SwingConstants.CENTER);
                placeholder.setForeground(Color.GRAY);
                placeholder.setFont(font);
                imagePanel.add(placeholder, BorderLayout.CENTER);
            }
        } catch (Exception e) {
            // 이미지 로드 실패 시 플레이스홀더
            JLabel placeholder = new JLabel("Image Load Failed", SwingConstants.CENTER);
            placeholder.setForeground(Color.GRAY);
            placeholder.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            imagePanel.add(placeholder, BorderLayout.CENTER);
        }

        // 하단 정보 패널 (맵 이름 + 설명)
        JPanel infoPanel = new JPanel(new BorderLayout(0, 3));
        infoPanel.setBackground(bgColor);
        infoPanel.setBorder(new EmptyBorder(8, 5, 8, 5));

        // 맵 이름 레이블
        JLabel nameLabel = new JLabel(mapName, SwingConstants.CENTER);
        nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        nameLabel.setForeground(Color.WHITE);

        // 맵 설명 레이블
        JLabel descLabel = new JLabel(mapDesc, SwingConstants.CENTER);
        descLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        descLabel.setForeground(Color.LIGHT_GRAY);

        infoPanel.add(nameLabel, BorderLayout.NORTH);
        infoPanel.add(descLabel, BorderLayout.CENTER);

        card.add(imagePanel, BorderLayout.CENTER);
        card.add(infoPanel, BorderLayout.SOUTH);

        // 클릭 이벤트
        MouseAdapter clickHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedMap = mapId;
                appendChat("맵 선택: " + mapName);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(new CompoundBorder(
                        new LineBorder(new Color(88, 101, 242), 2),
                        new EmptyBorder(9, 9, 9, 9)));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(new CompoundBorder(
                        new LineBorder(new Color(80, 82, 88), 1),
                        new EmptyBorder(10, 10, 10, 10)));
            }
        };
        card.addMouseListener(clickHandler);
        imagePanel.addMouseListener(clickHandler);

        return card;
    }

    private void selectTeam(int team) {
        if (isReady) {
            // 레디 상태에서는 팀 변경 불가
            appendChat("레디 상태에서는 팀을 변경할 수 없습니다.");
            return;
        }
        if (selectedTeam == team)
            return;
        selectedTeam = team;
        updateTeamButtons();
        characterSelectButton.setEnabled(true); // 팀 선택 후 캐릭터 선택 버튼 활성화

        if (out != null) {
            try {
                out.writeUTF("TEAM:" + team);
                out.flush();
            } catch (IOException ex) {
                appendChat("팀 선택 전송 실패");
            }
        }
    }

    // 캐릭터 선택 다이얼로그
    private String selectedCharacterId = null; // 캐릭터 미선택 상태가 기본

    private void openCharacterSelect() {
        if (selectedTeam == -1) {
            JOptionPane.showMessageDialog(this,
                    "팀을 먼저 선택해주세요!",
                    "알림",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 같은 팀에서 이미 선택된 캐릭터 목록 수집
        java.util.Set<String> disabledCharacters = new java.util.HashSet<>();
        java.util.Map<String, String> characterOwners = new java.util.HashMap<>();

        for (java.util.Map.Entry<String, Integer> entry : playerTeams.entrySet()) {
            if (entry.getValue() == selectedTeam) {
                String playerName = entry.getKey();
                String playerCharId = playerCharacters.get(playerName);
                if (playerCharId != null && !playerCharId.isEmpty()) {
                    disabledCharacters.add(playerCharId);
                    characterOwners.put(playerCharId, playerName);
                }
            }
        }

        String characterId = CharacterSelectDialog.showDialog(this, disabledCharacters, characterOwners);
        if (characterId != null) {
            selectedCharacterId = characterId;
            com.fpsgame.common.CharacterData charData = com.fpsgame.common.CharacterData.getById(characterId);
            appendChat("캐릭터 선택: " + charData.name);
            readyButton.setEnabled(true); // 캐릭터 선택 후 레디 버튼 활성화

            // 선택한 캐릭터 저장
            GameConfig.saveCharacter(characterId);

            // 서버에 캐릭터 선택 전송
            if (out != null) {
                try {
                    out.writeUTF("CHARACTER_SELECT:" + characterId);
                    out.flush();
                } catch (IOException ex) {
                    appendChat("캐릭터 선택 전송 실패");
                }
            }
        }
    }

    // 레디 토글
    private void toggleReady() {
        if (selectedTeam == -1) {
            JOptionPane.showMessageDialog(this,
                    "팀을 먼저 선택해주세요!",
                    "알림",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedCharacterId == null) {
            JOptionPane.showMessageDialog(this,
                    "캐릭터를 먼저 선택해주세요!",
                    "알림",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        isReady = !isReady;

        try {
            if (isReady) {
                out.writeUTF("READY");
            } else {
                out.writeUTF("UNREADY");
            }
            out.flush();
        } catch (IOException ex) {
            appendChat("레디 상태 전송 실패");
        }

        // 레디 상태에서는 팀 버튼 비활성, 해제 시 활성
        redTeamButton.setEnabled(!isReady);
        blueTeamButton.setEnabled(!isReady);
        updateTeamRoster();
    }

    private void updateTeamButtons() {
        if (selectedTeam == GameConstants.TEAM_RED) {
            redTeamButton.setSelected(true);
            redTeamButton.setBackground(new Color(244, 100, 80));
            redTeamButton.setForeground(Color.BLACK);
            blueTeamButton.setSelected(false);
            blueTeamButton.setBackground(new Color(100, 150, 255));
            blueTeamButton.setForeground(Color.BLACK);
        } else if (selectedTeam == GameConstants.TEAM_BLUE) {
            redTeamButton.setSelected(false);
            redTeamButton.setBackground(new Color(220, 80, 60));
            redTeamButton.setForeground(Color.BLACK);
            blueTeamButton.setSelected(true);
            blueTeamButton.setBackground(new Color(130, 170, 255));
            blueTeamButton.setForeground(Color.BLACK);
        } else {
            redTeamButton.setSelected(false);
            redTeamButton.setBackground(new Color(220, 80, 60));
            redTeamButton.setForeground(Color.BLACK);
            blueTeamButton.setSelected(false);
            blueTeamButton.setBackground(new Color(100, 150, 255));
            blueTeamButton.setForeground(Color.BLACK);
        }
    }

    private void updateTeamRoster() {
        for (int i = 0; i < 5; i++) {
            redSlots[i].setText("Empty");
            redSlots[i].setForeground(Color.GRAY);
            redSlots[i].setBackground(new Color(65, 67, 73));
            blueSlots[i].setText("Empty");
            blueSlots[i].setForeground(Color.GRAY);
            blueSlots[i].setBackground(new Color(65, 67, 73));
        }

        int redIdx = 0, blueIdx = 0;
        for (java.util.Map.Entry<String, Integer> entry : playerTeams.entrySet()) {
            String name = entry.getKey();
            int team = entry.getValue();
            boolean ready = readyPlayers.contains(name);

            if (team == GameConstants.TEAM_RED && redIdx < 5) {
                redSlots[redIdx].setText(name);
                redSlots[redIdx].setForeground(Color.WHITE);
                redSlots[redIdx].setBackground(ready ? new Color(67, 181, 129) : new Color(65, 67, 73));
                redIdx++;
            } else if (team == GameConstants.TEAM_BLUE && blueIdx < 5) {
                blueSlots[blueIdx].setText(name);
                blueSlots[blueIdx].setForeground(Color.WHITE);
                blueSlots[blueIdx].setBackground(ready ? new Color(67, 181, 129) : new Color(65, 67, 73));
                blueIdx++;
            }
        }

        boolean allReady = !playerTeams.isEmpty() && playerTeams.size() == readyPlayers.size();
        // READY 버튼 텍스트/색상
        if (isReady) {
            readyButton.setText("CANCEL");
            readyButton.setBackground(new Color(230, 200, 80));
            readyButton.setForeground(Color.BLACK);
        } else {
            readyButton.setText("READY");
            readyButton.setBackground(new Color(67, 181, 129));
            readyButton.setForeground(Color.BLACK);
        }

        // START 버튼 활성화 조건: 모든 인원 레디 + 팀 밸런스
        int redCount = 0, blueCount = 0;
        for (Integer team : playerTeams.values()) {
            if (team == GameConstants.TEAM_RED)
                redCount++;
            else if (team == GameConstants.TEAM_BLUE)
                blueCount++;
        }
        boolean teamOk = (redCount > 0 && blueCount > 0 && Math.abs(redCount - blueCount) <= 2);
        boolean canStart = allReady && teamOk;
        startButton.setEnabled(canStart);
        startButton.setBackground(canStart ? new Color(244, 67, 54) : new Color(100, 100, 100));

        // 레디 상태일 때 팀 버튼 잠금 유지
        redTeamButton.setEnabled(!isReady);
        blueTeamButton.setEnabled(!isReady);
    }

    private void connectToServer() {
        // UI 업데이트 - 연결 중 표시
        SwingUtilities.invokeLater(() -> {
            connectButton.setText("연결 중...");
            connectButton.setBackground(new Color(88, 101, 242));
        });

        try {
            String server = serverField.getText().trim();
            int port = (Integer) portSpinner.getValue();

            socket = new Socket(server, port);
            // 낮은 지연을 위한 TCP 설정
            try {
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                socket.setSendBufferSize(64 * 1024);
                socket.setReceiveBufferSize(64 * 1024);
            } catch (Exception ignore) {
            }
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());

            // 연결 성공
            SwingUtilities.invokeLater(() -> {
                appendChat("서버에 연결되었습니다!");
                connectButton.setText("연결됨");
                connectButton.setBackground(new Color(67, 181, 129));
                chatInput.setEnabled(true);
                readyButton.setEnabled(true);
            });

            // 플레이어 이름과 캐릭터 ID 전송 (JOIN에 포함)
            String joinMessage = "JOIN:" + playerName;
            if (selectedCharacterId != null && !selectedCharacterId.isEmpty()) {
                joinMessage += ":" + selectedCharacterId;
                System.out.println("[Lobby] JOIN with character: " + selectedCharacterId);
            }
            out.writeUTF(joinMessage);
            out.flush();

            // 서버 메시지 수신 스레드 시작 (로비 단계에서만 동작)
            lobbyListening = true;
            lobbyThread = new Thread(this::receiveMessages, "LobbyReceiver");
            lobbyThread.setDaemon(true);
            lobbyThread.start();

        } catch (IOException ex) {
            SwingUtilities.invokeLater(() -> {
                connectButton.setText("연결 실패");
                connectButton.setBackground(new Color(244, 67, 54));
                connectButton.setEnabled(true);

                // 재연결 옵션 제공
                int result = JOptionPane.showConfirmDialog(this,
                        "서버 연결 실패: " + ex.getMessage() + "\n\n다시 연결하시겠습니까?",
                        "연결 오류",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE);

                if (result == JOptionPane.YES_OPTION) {
                    // 재연결 시도
                    connectButton.setText("재연결");
                    connectButton.addActionListener(e -> {
                        connectButton.setEnabled(false);
                        new Thread(this::connectToServer).start();
                    });
                } else {
                    appendChat("서버 연결이 취소되었습니다.");
                }
            });
        }
    }

    private void disconnect() {
        try {
            lobbyListening = false;
            if (lobbyThread != null)
                lobbyThread.interrupt();
            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if (socket != null)
                socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void receiveMessages() {
        // 로비 단계에서만 서버 메시지(채팅 등)를 폴링 방식으로 받아와 충돌을 방지
        try {
            while (lobbyListening) {
                if (in != null && in.available() > 0) {
                    String message = in.readUTF();
                    final String msg = message;
                    SwingUtilities.invokeLater(() -> handleLobbyMessage(msg));
                } else {
                    try {
                        Thread.sleep(15);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        } catch (IOException ex) {
            if (lobbyListening) {
                SwingUtilities.invokeLater(() -> appendChat("서버 연결이 끊어졌습니다."));
            }
        }
    }

    private void handleLobbyMessage(String message) {
        if (message.startsWith("TEAM_ROSTER:")) {
            // 형식: TEAM_ROSTER:player1,0,true,raven;player2,1,false,piper
            String data = message.substring("TEAM_ROSTER:".length());
            playerTeams.clear();
            readyPlayers.clear();
            playerCharacters.clear();

            if (!data.isEmpty()) {
                String[] players = data.split(";");
                for (String playerData : players) {
                    String[] parts = playerData.split(",");
                    if (parts.length >= 3) {
                        String name = parts[0];
                        int team = Integer.parseInt(parts[1]);
                        boolean ready = Boolean.parseBoolean(parts[2]);
                        String characterId = parts.length >= 4 ? parts[3] : "";

                        playerTeams.put(name, team);
                        if (ready)
                            readyPlayers.add(name);
                        if (!characterId.isEmpty()) {
                            playerCharacters.put(name, characterId);
                        }
                    }
                }
            }

            updateTeamRoster();
        } else if (message.equals("GAME_START")) {
            launchGame();
        } else {
            appendChat(message);
        }
    }

    private void sendChat() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty() && out != null) {
            try {
                out.writeUTF("CHAT:" + message);
                out.flush();
            } catch (IOException ex) {
                appendChat("메시지 전송 실패");
            }
            chatInput.setText("");
        }
    }

    // 로비 채팅/시스템 로그 스로틀링
    private String lastLobbyMsg = null;
    private long lastLobbyTime = 0L;
    private static final long LOBBY_THROTTLE_MS = 1000;

    private void appendChat(String message) {
        long now = System.currentTimeMillis();
        if (message != null && message.equals(lastLobbyMsg) && (now - lastLobbyTime) < LOBBY_THROTTLE_MS) {
            return;
        }
        lastLobbyMsg = message;
        lastLobbyTime = now;
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void startGame() {
        if (socket == null || socket.isClosed()) {
            JOptionPane.showMessageDialog(this,
                    "서버에 연결되지 않았습니다!",
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 팀 밸런스 검증
        int redCount = 0, blueCount = 0;
        for (Integer team : playerTeams.values()) {
            if (team == GameConstants.TEAM_RED)
                redCount++;
            else if (team == GameConstants.TEAM_BLUE)
                blueCount++;
        }

        // 각 팀에 최소 1명 이상
        if (redCount == 0 || blueCount == 0) {
            JOptionPane.showMessageDialog(this,
                    "각 팀에 최소 1명 이상의 플레이어가 있어야 합니다!",
                    "게임 시작 불가",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 팀 인원 차이가 2명 이하
        if (Math.abs(redCount - blueCount) > 2) {
            JOptionPane.showMessageDialog(this,
                    "팀 인원 차이가 2명을 초과할 수 없습니다!\n(현재: 레드 " + redCount + "명, 블루 " + blueCount + "명)",
                    "게임 시작 불가",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // START 메시지 전송만 하고 서버의 GAME_START 신호를 기다린다
        try {
            out.writeUTF("START");
            out.flush();
        } catch (IOException e) {
            appendChat("게임 시작 신호 전송 실패: " + e.getMessage());
            return;
        }
        appendChat("게임 시작 요청 전송... (서버 승인 대기)");
    }

    private void launchGame() {
        // 로비 수신 스레드 정지 (게임 패널이 InputStream을 단독으로 사용)
        lobbyListening = false;
        if (lobbyThread != null)
            lobbyThread.interrupt();
        appendChat("게임을 시작합니다...");
        appendChat("선택된 캐릭터: " + selectedCharacterId);
        SwingUtilities.invokeLater(() -> {
            GamePanel gamePanel = new GamePanel(playerName, selectedTeam, socket, out, in, selectedCharacterId);
            gamePanel.setVisible(true);
            dispose();
        });
    }
}
