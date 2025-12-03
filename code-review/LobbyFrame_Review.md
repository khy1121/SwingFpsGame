# LobbyFrame 코드 리뷰

## 파일 정보
- **파일명**: `LobbyFrame.java`
- **패키지**: `com.fpsgame.client`
- **라인 수**: 1,036줄
- **역할**: 게임 시작 전 대기 공간 (서버 연결, 팀 선택, 캐릭터 선택, 채팅, 게임 시작)

---

## 파일 개요

`LobbyFrame`은 게임의 메인 로비 화면으로, 플레이어가 서버에 연결하고 팀을 선택하며 캐릭터를 고르는 일련의 준비 과정을 관리하는 중추적인 클래스입니다. Material Design 스타일의 다크 테마 UI를 구현하며, 다음과 같은 핵심 기능을 제공합니다:

1. **서버 연결**: 자동으로 서버 연결 시도, 재연결 옵션 제공
2. **맵 선택**: 4개의 맵 카드 (Map, Map 2, Map 3, Village)
3. **팀 선택**: RED/BLUE 팀 선택 버튼
4. **캐릭터 선택**: `CharacterSelectDialog` 통합, 저장된 캐릭터 로드
5. **팀 로스터**: 실시간 팀 목록 업데이트 (각 팀 최대 5명)
6. **준비 시스템**: READY/UNREADY 토글, 준비 상태 시각 표시
7. **채팅 시스템**: 로비 채팅 + 시스템 로그 (스로틀링 적용)
8. **게임 시작**: 팀 밸런스 검증 후 START 버튼 활성화
9. **설정 메뉴**: `OptionDialog` 통합 (메뉴바)

---

## 주요 컴포넌트

### 1. FilledButton (내부 클래스)

커스텀 버튼 클래스로, Swing의 기본 버튼을 Material Design 스타일로 확장했습니다.

```java
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

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 버튼 상태에 따른 색상 결정
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

        // 배경 그리기
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
```

**특징**:
- **Anti-aliasing**: 부드러운 테두리
- **5가지 상태 시각화**: 기본, 비활성, hover, pressed, selected
- **색상 조절 메서드**: `brighter()`, `darker()` - 명도 조절
- **선택 표시**: 노란 테두리로 선택된 버튼 강조

### 2. 네트워크 통신

#### 서버 연결 (자동)

```java
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

private void connectToServer() {
    SwingUtilities.invokeLater(() -> {
        connectButton.setText("연결 중...");
        connectButton.setBackground(new Color(88, 101, 242));
    });

    try {
        String server = serverField.getText().trim();
        int port = (Integer) portSpinner.getValue();

        socket = new Socket(server, port);
        // 낮은 지연을 위한 TCP 설정
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setSendBufferSize(64 * 1024);
        socket.setReceiveBufferSize(64 * 1024);

        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        // 플레이어 이름과 캐릭터 ID 전송
        String charId = (selectedCharacterId != null && !selectedCharacterId.isEmpty()) 
                ? selectedCharacterId : "raven";
        out.writeUTF("JOIN:" + playerName + ":" + charId);
        out.flush();

        // 서버 메시지 수신 스레드 시작
        lobbyListening = true;
        lobbyThread = new Thread(this::receiveMessages, "LobbyReceiver");
        lobbyThread.setDaemon(true);
        lobbyThread.start();

        SwingUtilities.invokeLater(() -> {
            appendChat("서버에 연결되었습니다!");
            connectButton.setText("연결됨");
            connectButton.setBackground(new Color(67, 181, 129));
            chatInput.setEnabled(true);
            readyButton.setEnabled(true);
        });

    } catch (IOException ex) {
        // 재연결 옵션 제공
        SwingUtilities.invokeLater(() -> {
            connectButton.setText("연결 실패");
            connectButton.setBackground(new Color(244, 67, 54));
            int result = JOptionPane.showConfirmDialog(this,
                    "서버 연결 실패: " + ex.getMessage() + "\n\n다시 연결하시겠습니까?",
                    "연결 오류", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                connectButton.setText("재연결");
                connectButton.addActionListener(e -> {
                    connectButton.setEnabled(false);
                    new Thread(this::connectToServer).start();
                });
            }
        });
    }
}
```

**특징**:
- **자동 연결**: 생성자에서 `SwingUtilities.invokeLater()`로 자동 연결
- **TCP 최적화**: `setTcpNoDelay(true)`, 버퍼 크기 64KB
- **JOIN 프로토콜**: `"JOIN:플레이어명:캐릭터ID"` 전송
- **재연결 옵션**: 연결 실패 시 사용자에게 재연결 여부 질문

#### 메시지 수신 (폴링 방식)

```java
private void receiveMessages() {
    try {
        while (lobbyListening) {
            if (in != null && in.available() > 0) {
                String message = in.readUTF();
                final String msg = message;
                SwingUtilities.invokeLater(() -> handleLobbyMessage(msg));
            } else {
                Thread.sleep(15); // 15ms 폴링 주기
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
                    if (ready) readyPlayers.add(name);
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
```

**특징**:
- **폴링 방식**: `in.available() > 0` 체크 후 `readUTF()` (15ms 주기)
- **블로킹 방지**: `GamePanel`과의 충돌 방지를 위해 폴링 사용
- **프로토콜 파싱**: `TEAM_ROSTER:`, `GAME_START` 등 메시지 처리
- **EDT 안전**: 모든 UI 업데이트는 `SwingUtilities.invokeLater()`로 처리

### 3. 팀 선택 시스템

```java
private void selectTeam(int team) {
    if (isReady) {
        appendChat("레디 상태에서는 팀을 변경할 수 없습니다.");
        return;
    }
    if (selectedTeam == team) return;
    
    selectedTeam = team;
    updateTeamButtons();
    characterSelectButton.setEnabled(true); // 팀 선택 후 캐릭터 선택 활성화

    if (out != null) {
        try {
            out.writeUTF("TEAM:" + team);
            out.flush();
        } catch (IOException ex) {
            appendChat("팀 선택 전송 실패");
        }
    }
}

private void updateTeamButtons() {
    if (selectedTeam == GameConstants.TEAM_RED) {
        redTeamButton.setSelected(true);
        redTeamButton.setBackground(new Color(244, 100, 80));
        blueTeamButton.setSelected(false);
        blueTeamButton.setBackground(new Color(100, 150, 255));
    } else if (selectedTeam == GameConstants.TEAM_BLUE) {
        redTeamButton.setSelected(false);
        redTeamButton.setBackground(new Color(220, 80, 60));
        blueTeamButton.setSelected(true);
        blueTeamButton.setBackground(new Color(130, 170, 255));
    } else {
        // 미선택 상태
        redTeamButton.setSelected(false);
        blueTeamButton.setSelected(false);
    }
}
```

**특징**:
- **레디 중 잠금**: 준비 상태에서는 팀 변경 불가
- **순차 활성화**: 팀 선택 → 캐릭터 선택 → 레디 버튼 순서 강제
- **색상 피드백**: 선택된 팀 버튼 밝은 색상 표시

### 4. 캐릭터 선택

```java
private void openCharacterSelect() {
    if (selectedTeam == -1) {
        JOptionPane.showMessageDialog(this, "팀을 먼저 선택해주세요!", "알림", 
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
        com.fpsgame.common.CharacterData charData = 
                com.fpsgame.common.CharacterData.getById(characterId);
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
```

**특징**:
- **중복 방지**: 같은 팀에서 이미 선택된 캐릭터는 비활성화
- **선택자 표시**: 각 캐릭터를 누가 선택했는지 표시
- **영속성**: `GameConfig.saveCharacter()`로 저장
- **서버 동기화**: `CHARACTER_SELECT:` 프로토콜 전송

### 5. 팀 로스터 업데이트

```java
private void updateTeamRoster() {
    // 슬롯 초기화
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
    } else {
        readyButton.setText("READY");
        readyButton.setBackground(new Color(67, 181, 129));
    }

    // START 버튼 활성화 조건
    int redCount = 0, blueCount = 0;
    for (Integer team : playerTeams.values()) {
        if (team == GameConstants.TEAM_RED) redCount++;
        else if (team == GameConstants.TEAM_BLUE) blueCount++;
    }
    boolean teamOk = (redCount > 0 && blueCount > 0 && Math.abs(redCount - blueCount) <= 2);
    boolean canStart = allReady && teamOk;
    startButton.setEnabled(canStart);
    startButton.setBackground(canStart ? new Color(244, 67, 54) : new Color(100, 100, 100));
}
```

**특징**:
- **최대 5명**: 각 팀별 5개 슬롯 (Empty 표시)
- **준비 상태 표시**: 준비 완료 시 초록색 배경 (`#43B581`)
- **밸런스 검증**: 양 팀 인원 차이 2명 이하
- **START 조건**: 모든 플레이어 준비 완료 + 팀 밸런스 만족

### 6. 게임 시작

```java
private void startGame() {
    if (socket == null || socket.isClosed()) {
        JOptionPane.showMessageDialog(this, "서버에 연결되지 않았습니다!", "오류", 
                JOptionPane.ERROR_MESSAGE);
        return;
    }

    // 팀 밸런스 검증
    int redCount = 0, blueCount = 0;
    for (Integer team : playerTeams.values()) {
        if (team == GameConstants.TEAM_RED) redCount++;
        else if (team == GameConstants.TEAM_BLUE) blueCount++;
    }

    // 각 팀에 최소 1명 이상
    if (redCount == 0 || blueCount == 0) {
        JOptionPane.showMessageDialog(this,
                "각 팀에 최소 1명 이상의 플레이어가 있어야 합니다!",
                "게임 시작 불가", JOptionPane.WARNING_MESSAGE);
        return;
    }

    // 팀 인원 차이가 2명 이하
    if (Math.abs(redCount - blueCount) > 2) {
        JOptionPane.showMessageDialog(this,
                "팀 인원 차이가 2명을 초과할 수 없습니다!\n(현재: 레드 " + redCount + "명, 블루 " + blueCount + "명)",
                "게임 시작 불가", JOptionPane.WARNING_MESSAGE);
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
    if (lobbyThread != null) lobbyThread.interrupt();
    
    appendChat("게임을 시작합니다...");
    appendChat("선택된 캐릭터: " + selectedCharacterId);
    
    SwingUtilities.invokeLater(() -> {
        GamePanel gamePanel = new GamePanel(playerName, selectedTeam, socket, out, in, selectedCharacterId);
        gamePanel.setVisible(true);
        dispose();
    });
}
```

**특징**:
- **2단계 검증**: 클라이언트 검증 → 서버 승인 대기
- **밸런스 규칙**: 각 팀 1명 이상, 인원 차이 2명 이하
- **스레드 정리**: 로비 수신 스레드 정지 후 `GamePanel` 전환
- **소켓 이관**: 로비에서 사용하던 소켓을 `GamePanel`로 전달

### 7. 채팅 스로틀링

```java
private String lastLobbyMsg = null;
private long lastLobbyTime = 0L;
private static final long LOBBY_THROTTLE_MS = 1000;

private void appendChat(String message) {
    long now = System.currentTimeMillis();
    if (message != null && message.equals(lastLobbyMsg) 
            && (now - lastLobbyTime) < LOBBY_THROTTLE_MS) {
        return; // 1초 내 중복 메시지 무시
    }
    lastLobbyMsg = message;
    lastLobbyTime = now;
    chatArea.append(message + "\n");
    chatArea.setCaretPosition(chatArea.getDocument().getLength());
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
```

**특징**:
- **중복 방지**: 1초 내 동일 메시지 무시 (스팸 방지)
- **자동 스크롤**: `setCaretPosition()` 사용
- **CHAT 프로토콜**: `"CHAT:메시지내용"` 형식

### 8. 맵 선택

```java
private JPanel createMapCard(String mapName, String mapId, String mapDesc, Font font, Color bgColor) {
    JPanel card = new JPanel(new BorderLayout());
    card.setBackground(bgColor);

    // 맵 이미지 로드
    JPanel imagePanel = new JPanel(new BorderLayout());
    try {
        String imagePath = "assets" + File.separator + "maps" + File.separator + mapId + ".png";
        File imageFile = new File(imagePath);

        if (imageFile.exists()) {
            ImageIcon originalIcon = new ImageIcon(imagePath);
            Image scaledImage = originalIcon.getImage().getScaledInstance(280, 150, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
            imagePanel.add(imageLabel, BorderLayout.CENTER);
        } else {
            JLabel placeholder = new JLabel("No Image", SwingConstants.CENTER);
            placeholder.setForeground(Color.GRAY);
            imagePanel.add(placeholder, BorderLayout.CENTER);
        }
    } catch (Exception e) {
        // 이미지 로드 실패 시 플레이스홀더
    }

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

    return card;
}
```

**특징**:
- **4개 맵**: Map, Map 2, Map 3, Village
- **이미지 표시**: `assets/maps/{mapId}.png` 로드, 실패 시 플레이스홀더
- **hover 효과**: 마우스 오버 시 파란 테두리 표시
- **맵 설명**: 각 맵의 특징을 라벨로 표시

---

## 강점

### 1. **Material Design UI**
- **다크 테마**: Discord 스타일의 배경색 (#202225, #2F3136)
- **FilledButton**: 5가지 상태 시각화 (기본, hover, pressed, selected, disabled)
- **색상 팔레트**: 일관된 색상 체계 (성공=#43B581, 실패=#F44336, 정보=#5865F2)

### 2. **자동 서버 연결**
- **생성자에서 자동 연결**: 사용자가 별도로 연결 버튼을 누를 필요 없음
- **재연결 옵션**: 연결 실패 시 다이얼로그로 재연결 여부 질문
- **TCP 최적화**: `setTcpNoDelay(true)`, 64KB 버퍼

### 3. **순차 활성화 시스템**
- **단계별 잠금**: 팀 선택 → 캐릭터 선택 → 레디 → START 순서 강제
- **명확한 UX**: 버튼 활성화/비활성화로 다음 단계 안내
- **레디 중 잠금**: 준비 상태에서는 팀/캐릭터 변경 불가

### 4. **실시간 팀 로스터**
- **TEAM_ROSTER 프로토콜**: 서버에서 받은 팀 구성 정보 파싱
- **준비 상태 시각화**: 준비 완료 시 초록색 배경
- **최대 5명**: 각 팀별 5개 슬롯

### 5. **캐릭터 중복 방지**
- **팀 내 중복 체크**: 같은 팀에서 선택된 캐릭터 비활성화
- **선택자 표시**: 누가 어떤 캐릭터를 선택했는지 표시
- **영속성**: `GameConfig.saveCharacter()`로 저장

### 6. **채팅 스로틀링**
- **중복 방지**: 1초 내 동일 메시지 무시 (스팸 방지)
- **시스템 로그 통합**: 채팅 + 시스템 메시지 같은 영역 사용

### 7. **팀 밸런스 검증**
- **2단계 검증**: 클라이언트 검증 → 서버 승인
- **명확한 규칙**: 각 팀 1명 이상, 인원 차이 2명 이하
- **에러 메시지**: 규칙 위반 시 구체적인 메시지 표시

### 8. **리소스 정리**
- **스레드 정리**: 게임 시작 시 로비 수신 스레드 정지
- **소켓 이관**: 로비 소켓을 `GamePanel`로 전달
- **윈도우 리스너**: 창 닫을 때 `disconnect()` 호출

---

## 개선 제안

### 1. **네트워크 프로토콜 클래스 분리** (중요도: 높음)

현재는 문자열 파싱을 직접 구현하고 있으나, 프로토콜 클래스로 분리하면 유지보수성이 크게 향상됩니다.

```java
// 현재 코드
if (message.startsWith("TEAM_ROSTER:")) {
    String data = message.substring("TEAM_ROSTER:".length());
    String[] players = data.split(";");
    for (String playerData : players) {
        String[] parts = playerData.split(",");
        // 파싱...
    }
}

// 개선 제안
public class LobbyProtocol {
    public static TeamRoster parseTeamRoster(String message) {
        if (!message.startsWith("TEAM_ROSTER:")) {
            throw new IllegalArgumentException("Invalid protocol");
        }
        String data = message.substring("TEAM_ROSTER:".length());
        // 파싱 로직...
        return new TeamRoster(players);
    }

    public static String serializeTeamSelect(int team) {
        return "TEAM:" + team;
    }
}

// 사용
private void handleLobbyMessage(String message) {
    if (message.startsWith("TEAM_ROSTER:")) {
        TeamRoster roster = LobbyProtocol.parseTeamRoster(message);
        updateTeamRoster(roster);
    }
}
```

### 2. **UI 컴포넌트 팩토리 클래스** (중요도: 중간)

`FilledButton`, 맵 카드 등 반복되는 UI 생성 코드를 팩토리 클래스로 분리하면 재사용성이 향상됩니다.

```java
public class UIFactory {
    public static FilledButton createButton(String text, Color bg, ActionListener listener) {
        FilledButton button = new FilledButton(text, bg);
        button.addActionListener(listener);
        return button;
    }

    public static JPanel createMapCard(String mapName, String mapId, String description, 
                                       Consumer<String> onSelect) {
        // 맵 카드 생성 로직
    }

    public static JLabel createTeamSlot(String text, boolean isReady) {
        JLabel slot = new JLabel(text, SwingConstants.CENTER);
        slot.setBackground(isReady ? Color.GREEN : Color.GRAY);
        return slot;
    }
}
```

### 3. **설정 관리 통합** (중요도: 중간)

현재는 `GameConfig`와 `KeyBindingConfig`가 분리되어 있으나, 로비 설정(서버 주소, 포트 등)도 저장하면 좋습니다.

```java
public class LobbyConfig {
    private static final String CONFIG_FILE = "lobby_config.properties";
    
    public static void saveServerInfo(String host, int port) {
        Properties props = new Properties();
        props.setProperty("server.host", host);
        props.setProperty("server.port", String.valueOf(port));
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Lobby Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ServerInfo loadServerInfo() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            String host = props.getProperty("server.host", "127.0.0.1");
            int port = Integer.parseInt(props.getProperty("server.port", "12345"));
            return new ServerInfo(host, port);
        } catch (IOException e) {
            return new ServerInfo("127.0.0.1", GameConstants.DEFAULT_PORT);
        }
    }
}
```

### 4. **에러 처리 강화** (중요도: 높음)

네트워크 에러 발생 시 스택 트레이스를 로그에 남기고, 사용자에게는 간결한 메시지를 표시해야 합니다.

```java
private void handleNetworkError(IOException ex, String context) {
    // 로그에 상세한 에러 정보 기록
    System.err.println("[LobbyFrame] Network error in " + context);
    ex.printStackTrace();
    
    // 사용자에게는 간결한 메시지만 표시
    SwingUtilities.invokeLater(() -> {
        appendChat("네트워크 오류가 발생했습니다: " + context);
        // 자동 재연결 시도 (선택 사항)
        if (shouldAutoReconnect()) {
            scheduleReconnect();
        }
    });
}
```

### 5. **맵 프리뷰 개선** (중요도: 낮음)

맵 이미지가 없을 때 더 나은 플레이스홀더를 표시하거나, 기본 이미지를 제공하면 좋습니다.

```java
private ImageIcon loadMapImage(String mapId) {
    String[] locations = {
        "assets/maps/" + mapId + ".png",
        "assets/maps/default.png",
        "res/maps/" + mapId + ".png"
    };

    for (String path : locations) {
        File file = new File(path);
        if (file.exists()) {
            try {
                ImageIcon icon = new ImageIcon(path);
                Image scaled = icon.getImage().getScaledInstance(280, 150, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            } catch (Exception e) {
                // 다음 경로 시도
            }
        }
    }
    
    // 모두 실패하면 프로그래밍 방식으로 이미지 생성
    return createDefaultMapImage(mapId);
}

private ImageIcon createDefaultMapImage(String mapId) {
    BufferedImage img = new BufferedImage(280, 150, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = img.createGraphics();
    g2.setColor(new Color(45, 47, 53));
    g2.fillRect(0, 0, 280, 150);
    g2.setColor(Color.LIGHT_GRAY);
    g2.setFont(new Font("맑은 고딕", Font.BOLD, 20));
    g2.drawString(mapId.toUpperCase(), 100, 80);
    g2.dispose();
    return new ImageIcon(img);
}
```

### 6. **연결 상태 아이콘** (중요도: 낮음)

현재는 버튼 텍스트로만 연결 상태를 표시하지만, 아이콘이나 인디케이터를 추가하면 시각적으로 더 명확합니다.

```java
private void updateConnectionStatus(ConnectionState state) {
    switch (state) {
        case CONNECTING:
            statusIcon.setIcon(getIcon("connecting.gif")); // 회전하는 아이콘
            statusLabel.setText("연결 중...");
            break;
        case CONNECTED:
            statusIcon.setIcon(getIcon("connected.png")); // 초록 체크
            statusLabel.setText("연결됨");
            break;
        case DISCONNECTED:
            statusIcon.setIcon(getIcon("disconnected.png")); // 빨간 X
            statusLabel.setText("연결 끊김");
            break;
    }
}
```

### 7. **채팅 명령어 시스템** (중요도: 낮음)

채팅에 `/help`, `/clear` 등의 명령어를 추가하면 편의성이 향상됩니다.

```java
private void sendChat() {
    String message = chatInput.getText().trim();
    if (message.isEmpty()) return;

    // 명령어 처리
    if (message.startsWith("/")) {
        handleChatCommand(message);
        chatInput.setText("");
        return;
    }

    // 일반 채팅
    if (out != null) {
        try {
            out.writeUTF("CHAT:" + message);
            out.flush();
        } catch (IOException ex) {
            appendChat("메시지 전송 실패");
        }
    }
    chatInput.setText("");
}

private void handleChatCommand(String command) {
    if (command.equals("/clear")) {
        chatArea.setText("");
    } else if (command.equals("/help")) {
        appendChat("사용 가능한 명령어:");
        appendChat("  /clear - 채팅 내용 지우기");
        appendChat("  /help - 도움말 표시");
    } else {
        appendChat("알 수 없는 명령어: " + command);
    }
}
```

---

## 코드 품질 평가

| 항목 | 점수 | 설명 |
|------|------|------|
| **가독성** | ⭐⭐⭐⭐☆ (4.0/5.0) | 메서드명 명확, 주석 충분하나 1,000줄 넘는 길이로 분리 필요 |
| **유지보수성** | ⭐⭐⭐⭐☆ (4.0/5.0) | 프로토콜 문자열 파싱 개선 여지 있음 |
| **확장성** | ⭐⭐⭐⭐☆ (4.0/5.0) | 맵/팀 추가는 쉬우나, UI 컴포넌트 팩토리 클래스 분리 권장 |
| **에러 처리** | ⭐⭐⭐⭐☆ (4.0/5.0) | 네트워크 에러 처리 양호, 재연결 로직 있음 |
| **성능** | ⭐⭐⭐⭐⭐ (5.0/5.0) | 폴링 방식(15ms), 채팅 스로틀링(1초), TCP 최적화 |
| **일관성** | ⭐⭐⭐⭐⭐ (5.0/5.0) | Material Design 스타일 일관 적용 |

### **종합 평가**: ⭐⭐⭐⭐☆ (4.2/5.0)

---

## 결론 및 요약

`LobbyFrame`은 NetFps 게임의 메인 로비 화면으로, **Material Design 스타일의 세련된 UI**와 **안정적인 네트워크 통신**을 제공하는 핵심 클래스입니다. 

**주요 성과**:
- **자동 서버 연결**: 생성자에서 자동 연결, 재연결 옵션 제공
- **순차 활성화**: 팀 선택 → 캐릭터 선택 → 레디 순서 강제로 명확한 UX
- **실시간 팀 로스터**: 준비 상태 시각화, 최대 5명씩 팀 구성
- **캐릭터 중복 방지**: 같은 팀 내 중복 선택 불가
- **팀 밸런스 검증**: 2단계 검증 (클라이언트 + 서버)
- **채팅 스로틀링**: 1초 내 중복 메시지 무시
- **FilledButton**: 5가지 상태 시각화로 Material Design 구현

**개선 영역**:
1. 프로토콜 클래스 분리 (문자열 파싱 개선)
2. UI 컴포넌트 팩토리 클래스 분리 (재사용성)
3. 설정 관리 통합 (서버 정보 저장)
4. 에러 처리 강화 (로그 + 사용자 메시지 분리)

전반적으로 **매우 잘 구현된 로비 화면**이며, 프로토콜 파싱과 UI 컴포넌트 분리만 개선하면 완벽에 가까운 코드가 될 것입니다.
