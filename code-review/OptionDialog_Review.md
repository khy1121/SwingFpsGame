# OptionDialog 코드 리뷰

## 파일 정보
- **파일명**: `OptionDialog.java`
- **패키지**: `com.fpsgame.client`
- **라인 수**: 263줄
- **역할**: 게임 설정 다이얼로그 (일반 설정 + 키 설정)

---

## 파일 개요

`OptionDialog`는 게임의 다양한 설정을 변경할 수 있는 모달 다이얼로그입니다. **2개의 탭**(일반 설정, 키 설정)으로 구성되어 있으며, 사용자가 **사운드 볼륨, 마우스 감도, 키 바인딩**을 조정할 수 있습니다. `LobbyFrame`의 메뉴바 "옵션 > 설정"에서 접근 가능합니다.

주요 기능:
1. **일반 설정 탭**: 사운드 볼륨 (0~100), 마우스 감도 (1~20)
2. **키 설정 탭**: 8개 액션 키 바인딩 (이동 WASD, 스킬 E/R, UI B/M)
3. **기본값 복원**: 모든 설정을 기본값으로 리셋
4. **설정 저장**: `KeyBindingConfig.saveBindings()` 호출

---

## 주요 컴포넌트

### 1. 탭 패널 구조

```java
private void initUI() {
    Color bgDark = new Color(32, 34, 37);
    Color panelBg = new Color(47, 49, 54);
    Font koreanFont = new Font("맑은 고딕", Font.PLAIN, 14);
    Font koreanBold = new Font("맑은 고딕", Font.BOLD, 14);

    // 탭 패널
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.setBackground(bgDark);
    tabbedPane.setFont(koreanBold);

    // 1. 일반 설정 탭
    JPanel generalPanel = createGeneralPanel(panelBg, koreanFont);
    tabbedPane.addTab("일반", generalPanel);

    // 2. 키 설정 탭
    JPanel keyPanel = createKeyPanel(panelBg, koreanFont, koreanBold);
    tabbedPane.addTab("키 설정", keyPanel);

    add(tabbedPane, BorderLayout.CENTER);
    // 하단 버튼 (저장/취소)
    add(createButtonPanel(bgDark, koreanBold), BorderLayout.SOUTH);
}
```

**특징**:
- **2개 탭**: "일반", "키 설정"
- **Material Design**: Discord 스타일 다크 테마 (`#202225`, `#2F3136`)
- **한글 폰트**: "맑은 고딕" 사용

### 2. 일반 설정 탭

```java
// 소리 크기
JLabel lblSound = new JLabel("사운드 볼륨:");
lblSound.setForeground(Color.WHITE);
lblSound.setFont(koreanFont);

soundSlider = new JSlider(0, 100, 50);
soundSlider.setBackground(panelBg);
soundSlider.setForeground(Color.WHITE);
soundSlider.setMajorTickSpacing(25);
soundSlider.setMinorTickSpacing(5);
soundSlider.setPaintTicks(true);
soundSlider.setPaintLabels(true);

// 마우스 감도
JLabel lblMouse = new JLabel("마우스 감도:");
lblMouse.setForeground(Color.WHITE);
lblMouse.setFont(koreanFont);

mouseSlider = new JSlider(1, 20, 10);
mouseSlider.setBackground(panelBg);
mouseSlider.setForeground(Color.WHITE);
mouseSlider.setMajorTickSpacing(5);
mouseSlider.setMinorTickSpacing(1);
mouseSlider.setPaintTicks(true);
mouseSlider.setPaintLabels(true);
```

**특징**:
- **사운드 볼륨**: 0~100 범위, 기본값 50, 눈금 간격 25
- **마우스 감도**: 1~20 범위, 기본값 10, 눈금 간격 5
- **시각적 피드백**: 눈금과 라벨 표시 (`setPaintTicks(true)`, `setPaintLabels(true)`)
- **주의**: 현재는 UI만 있고 저장/로드 로직은 미구현 (TODO)

### 3. 키 설정 탭

```java
String[] keyActions = {
        KeyBindingConfig.KEY_MOVE_FORWARD,
        KeyBindingConfig.KEY_MOVE_BACKWARD,
        KeyBindingConfig.KEY_MOVE_LEFT,
        KeyBindingConfig.KEY_MOVE_RIGHT,
        KeyBindingConfig.KEY_TACTICAL_SKILL,
        KeyBindingConfig.KEY_ULTIMATE_SKILL,
        KeyBindingConfig.KEY_CHARACTER_SELECT,
        KeyBindingConfig.KEY_MINIMAP_TOGGLE
};

String[] keyDisplayNames = {
        "앞으로 이동", "뒤로 이동", "왼쪽 이동", "오른쪽 이동",
        "전술 스킬 (E)", "궁극기 (R)", "캐릭터 선택 (B)", "미니맵 토글 (M)"
};

for (int i = 0; i < keyActions.length; i++) {
    String action = keyActions[i];
    String displayName = keyDisplayNames[i];

    JLabel label = new JLabel(displayName + ":");
    label.setForeground(Color.WHITE);
    label.setFont(koreanFont);

    int currentKey = KeyBindingConfig.getKey(action);
    JButton keyButton = new JButton(KeyEvent.getKeyText(currentKey));
    keyButton.setFont(koreanFont);
    keyButton.setBackground(new Color(88, 101, 242));
    keyButton.setForeground(Color.BLACK);
    keyButton.setFocusPainted(false);
    keyButton.addActionListener(e -> captureKey(action, keyButton));

    keyButtons.put(action, keyButton);
    keyPanel.add(label);
    keyPanel.add(keyButton);
}
```

**특징**:
- **8개 액션**: 이동(WASD), 스킬(E, R), UI(B, M)
- **한글 라벨**: "앞으로 이동", "전술 스킬 (E)" 등
- **현재 키 표시**: `KeyEvent.getKeyText(keyCode)` 사용 (예: "W", "E")
- **버튼 맵**: `keyButtons` Map에 저장하여 나중에 업데이트 가능

### 4. 키 입력 캡처

```java
private void captureKey(String actionName, JButton button) {
    button.setText("키를 누르세요...");
    button.setBackground(new Color(255, 152, 0)); // 주황색

    // KeyListener 등록
    KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();
            if (keyCode != KeyEvent.VK_ESCAPE) {
                KeyBindingConfig.setKey(actionName, keyCode);
                button.setText(KeyEvent.getKeyText(keyCode));
            }
            button.setBackground(new Color(88, 101, 242)); // 원래 색상
            button.removeKeyListener(this);
        }
    };

    button.addKeyListener(keyListener);
    button.requestFocusInWindow();
}
```

**특징**:
- **키 입력 대기**: "키를 누르세요..." 텍스트 + 주황색 배경
- **ESC 무시**: ESC키는 무시하고 다른 키만 받음
- **즉시 저장**: `KeyBindingConfig.setKey()` 호출 (메모리에만 저장)
- **KeyListener 제거**: 키 입력 후 리스너 제거 (중복 방지)
- **포커스 요청**: `requestFocusInWindow()` 호출

### 5. 기본값 복원

```java
private void resetToDefaults() {
    KeyBindingConfig.resetToDefaults();

    // UI 업데이트
    for (Map.Entry<String, JButton> entry : keyButtons.entrySet()) {
        String action = entry.getKey();
        JButton button = entry.getValue();
        int keyCode = KeyBindingConfig.getKey(action);
        button.setText(KeyEvent.getKeyText(keyCode));
    }

    soundSlider.setValue(50);
    mouseSlider.setValue(10);

    JOptionPane.showMessageDialog(this,
            "모든 설정이 기본값으로 복원되었습니다.",
            "복원 완료",
            JOptionPane.INFORMATION_MESSAGE);
}
```

**특징**:
- **키 바인딩 리셋**: `KeyBindingConfig.resetToDefaults()` 호출
- **UI 동기화**: 모든 키 버튼 텍스트 업데이트
- **슬라이더 리셋**: 사운드 50, 마우스 감도 10
- **확인 메시지**: "모든 설정이 기본값으로 복원되었습니다."

### 6. 설정 저장

```java
private void saveSettings() {
    // 키 바인딩 저장
    KeyBindingConfig.saveBindings();

    // TODO: 사운드 및 마우스 감도 저장
    System.out.println("[OptionDialog] 설정 저장됨:");
    System.out.println("  사운드: " + soundSlider.getValue());
    System.out.println("  마우스 감도: " + mouseSlider.getValue());

    JOptionPane.showMessageDialog(this,
            "설정이 저장되었습니다.",
            "저장 완료",
            JOptionPane.INFORMATION_MESSAGE);
}
```

**특징**:
- **키 바인딩 저장**: `KeyBindingConfig.saveBindings()` 호출 (Properties 파일로 영속화)
- **사운드/마우스 감도**: 현재는 콘솔에만 출력 (TODO)
- **확인 메시지**: "설정이 저장되었습니다."

---

## 강점

### 1. **탭 구조로 설정 분류**
- **일반 탭**: 게임 설정 (사운드, 마우스 감도)
- **키 설정 탭**: 키 바인딩 설정
- **확장 가능**: 나중에 "그래픽", "네트워크" 등의 탭 추가 가능

### 2. **키 입력 캡처 UI**
- **직관적**: "키를 누르세요..." 텍스트 + 주황색 배경
- **ESC 무시**: ESC키는 무시하고 다른 키만 받음
- **즉시 피드백**: 키를 누르면 바로 버튼 텍스트 변경

### 3. **기본값 복원**
- **완전한 리셋**: 키 바인딩 + 슬라이더 모두 기본값으로
- **UI 동기화**: 모든 버튼 텍스트 업데이트
- **확인 메시지**: 사용자에게 명확한 피드백

### 4. **Material Design 스타일**
- **다크 테마**: Discord 스타일 배경색
- **일관된 버튼 색상**: 파란색(`#5865F2`), 빨간색(`#F44336`)
- **한글 폰트**: "맑은 고딕" 사용

### 5. **`KeyBindingConfig` 통합**
- **깔끔한 API**: `getKey()`, `setKey()`, `resetToDefaults()`, `saveBindings()`
- **영속성**: Properties 파일로 저장
- **타입 안전**: 상수 사용 (`KEY_MOVE_FORWARD` 등)

---

## 개선 제안

### 1. **사운드/마우스 감도 저장** (중요도: 높음)

현재는 키 바인딩만 저장되고, 사운드/마우스 감도는 저장되지 않습니다. `GameConfig` 또는 새로운 설정 클래스를 사용하여 저장해야 합니다.

```java
public class GeneralConfig {
    private static final String CONFIG_FILE = "general_config.properties";
    
    public static void saveSoundVolume(int volume) {
        Properties props = loadProperties();
        props.setProperty("sound.volume", String.valueOf(volume));
        saveProperties(props);
    }

    public static int loadSoundVolume() {
        Properties props = loadProperties();
        return Integer.parseInt(props.getProperty("sound.volume", "50"));
    }

    public static void saveMouseSensitivity(int sensitivity) {
        Properties props = loadProperties();
        props.setProperty("mouse.sensitivity", String.valueOf(sensitivity));
        saveProperties(props);
    }

    public static int loadMouseSensitivity() {
        Properties props = loadProperties();
        return Integer.parseInt(props.getProperty("mouse.sensitivity", "10"));
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
        } catch (IOException e) {
            // 파일 없음 (첫 실행) - 기본값 사용
        }
        return props;
    }

    private static void saveProperties(Properties props) {
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "General Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// 사용
private void saveSettings() {
    KeyBindingConfig.saveBindings();
    GeneralConfig.saveSoundVolume(soundSlider.getValue());
    GeneralConfig.saveMouseSensitivity(mouseSlider.getValue());
    
    JOptionPane.showMessageDialog(this, "설정이 저장되었습니다.", 
            "저장 완료", JOptionPane.INFORMATION_MESSAGE);
}

public OptionDialog(Frame owner) {
    super(owner, "게임 설정", true);
    setLayout(new BorderLayout());
    setSize(600, 500);
    setLocationRelativeTo(owner);

    // 저장된 설정 로드
    int savedVolume = GeneralConfig.loadSoundVolume();
    int savedSensitivity = GeneralConfig.loadMouseSensitivity();
    
    initUI();
    
    // UI 업데이트
    soundSlider.setValue(savedVolume);
    mouseSlider.setValue(savedSensitivity);
}
```

### 2. **키 중복 검증** (중요도: 높음)

현재는 여러 액션에 같은 키를 할당할 수 있습니다. 중복 검증을 추가해야 합니다.

```java
private void captureKey(String actionName, JButton button) {
    button.setText("키를 누르세요...");
    button.setBackground(new Color(255, 152, 0));

    KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_ESCAPE) {
                button.setText(KeyEvent.getKeyText(KeyBindingConfig.getKey(actionName)));
                button.setBackground(new Color(88, 101, 242));
                button.removeKeyListener(this);
                return;
            }

            // 중복 검증
            String conflictAction = findConflictingAction(keyCode);
            if (conflictAction != null && !conflictAction.equals(actionName)) {
                int result = JOptionPane.showConfirmDialog(OptionDialog.this,
                        "이 키는 이미 '" + getDisplayName(conflictAction) + "'에 할당되어 있습니다.\n" +
                        "기존 할당을 해제하고 변경하시겠습니까?",
                        "키 중복",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                
                if (result != JOptionPane.YES_OPTION) {
                    button.setText(KeyEvent.getKeyText(KeyBindingConfig.getKey(actionName)));
                    button.setBackground(new Color(88, 101, 242));
                    button.removeKeyListener(this);
                    return;
                }
            }

            // 키 할당
            KeyBindingConfig.setKey(actionName, keyCode);
            button.setText(KeyEvent.getKeyText(keyCode));
            button.setBackground(new Color(88, 101, 242));
            button.removeKeyListener(this);

            // 다른 버튼 UI 업데이트 (중복 제거)
            if (conflictAction != null) {
                updateKeyButton(conflictAction);
            }
        }
    };

    button.addKeyListener(keyListener);
    button.requestFocusInWindow();
}

private String findConflictingAction(int keyCode) {
    for (String action : keyButtons.keySet()) {
        if (KeyBindingConfig.getKey(action) == keyCode) {
            return action;
        }
    }
    return null;
}

private void updateKeyButton(String action) {
    JButton button = keyButtons.get(action);
    if (button != null) {
        int keyCode = KeyBindingConfig.getKey(action);
        button.setText(KeyEvent.getKeyText(keyCode));
    }
}
```

### 3. **설정 변경 감지** (중요도: 중간)

현재는 설정을 변경하지 않아도 "저장" 버튼을 누를 수 있습니다. 변경 감지를 추가하여 변경된 경우에만 저장 가능하도록 하면 좋습니다.

```java
private boolean settingsChanged = false;

public OptionDialog(Frame owner) {
    // ... 기존 코드 ...
    
    // 변경 감지 리스너 추가
    soundSlider.addChangeListener(e -> settingsChanged = true);
    mouseSlider.addChangeListener(e -> settingsChanged = true);
    
    initUI();
}

private void captureKey(String actionName, JButton button) {
    // ... 기존 코드 ...
    KeyBindingConfig.setKey(actionName, keyCode);
    settingsChanged = true; // 변경 플래그 설정
    // ...
}

private void saveSettings() {
    if (!settingsChanged) {
        JOptionPane.showMessageDialog(this,
                "변경된 설정이 없습니다.",
                "알림",
                JOptionPane.INFORMATION_MESSAGE);
        return;
    }
    
    KeyBindingConfig.saveBindings();
    GeneralConfig.saveSoundVolume(soundSlider.getValue());
    GeneralConfig.saveMouseSensitivity(mouseSlider.getValue());
    
    settingsChanged = false;
    JOptionPane.showMessageDialog(this, "설정이 저장되었습니다.", 
            "저장 완료", JOptionPane.INFORMATION_MESSAGE);
}
```

### 4. **취소 시 확인 다이얼로그** (중요도: 낮음)

설정을 변경한 후 저장하지 않고 닫으려 하면 확인 메시지를 표시하면 좋습니다.

```java
btnCancel.addActionListener(e -> {
    if (settingsChanged) {
        int result = JOptionPane.showConfirmDialog(this,
                "변경된 설정이 저장되지 않았습니다.\n정말로 취소하시겠습니까?",
                "확인",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            dispose();
        }
    } else {
        dispose();
    }
});

// 윈도우 닫기 이벤트에도 동일하게 적용
addWindowListener(new WindowAdapter() {
    @Override
    public void windowClosing(WindowEvent e) {
        if (settingsChanged) {
            int result = JOptionPane.showConfirmDialog(OptionDialog.this,
                    "변경된 설정이 저장되지 않았습니다.\n정말로 닫으시겠습니까?",
                    "확인",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            
            if (result != JOptionPane.YES_OPTION) {
                return; // 닫기 취소
            }
        }
        dispose();
    }
});
```

### 5. **슬라이더 값 실시간 표시** (중요도: 낮음)

슬라이더 옆에 현재 값을 표시하는 라벨이 있으면 더 직관적입니다.

```java
// 사운드 볼륨
JLabel soundValueLabel = new JLabel("50");
soundValueLabel.setForeground(Color.WHITE);
soundValueLabel.setFont(koreanBold);

soundSlider.addChangeListener(e -> {
    soundValueLabel.setText(String.valueOf(soundSlider.getValue()));
});

// 레이아웃에 추가
gbc.gridx = 2;
gbc.weightx = 0.1;
generalPanel.add(soundValueLabel, gbc);

// 마우스 감도도 동일하게
JLabel mouseValueLabel = new JLabel("10");
mouseValueLabel.setForeground(Color.WHITE);
mouseValueLabel.setFont(koreanBold);

mouseSlider.addChangeListener(e -> {
    mouseValueLabel.setText(String.valueOf(mouseSlider.getValue()));
});
```

### 6. **프리셋 시스템** (중요도: 낮음)

"저감도", "중간", "고감도" 등의 프리셋 버튼이 있으면 편리합니다.

```java
JPanel presetPanel = new JPanel(new FlowLayout());
presetPanel.setBackground(panelBg);

JLabel presetLabel = new JLabel("프리셋:");
presetLabel.setForeground(Color.WHITE);
presetPanel.add(presetLabel);

JButton lowButton = new JButton("저감도");
lowButton.addActionListener(e -> {
    mouseSlider.setValue(5);
    soundSlider.setValue(30);
});
presetPanel.add(lowButton);

JButton mediumButton = new JButton("중간");
mediumButton.addActionListener(e -> {
    mouseSlider.setValue(10);
    soundSlider.setValue(50);
});
presetPanel.add(mediumButton);

JButton highButton = new JButton("고감도");
highButton.addActionListener(e -> {
    mouseSlider.setValue(15);
    soundSlider.setValue(70);
});
presetPanel.add(highButton);

// 일반 설정 탭에 추가
generalPanel.add(presetPanel, gbc);
```

### 7. **설정 가져오기/내보내기** (중요도: 매우 낮음)

설정을 파일로 내보내고 가져오는 기능이 있으면 백업 및 공유가 가능합니다.

```java
JButton exportButton = new JButton("설정 내보내기");
exportButton.addActionListener(e -> {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("설정 파일 저장");
    fileChooser.setSelectedFile(new File("game_settings.properties"));
    
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        // 모든 설정을 파일로 내보내기
        exportSettings(file);
    }
});

JButton importButton = new JButton("설정 가져오기");
importButton.addActionListener(e -> {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("설정 파일 열기");
    
    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        // 파일에서 설정 가져오기
        importSettings(file);
    }
});
```

---

## 코드 품질 평가

| 항목 | 점수 | 설명 |
|------|------|------|
| **가독성** | ⭐⭐⭐⭐⭐ (5.0/5.0) | 메서드명 명확, 주석 충분, 적절한 길이 (263줄) |
| **유지보수성** | ⭐⭐⭐⭐☆ (4.0/5.0) | 탭 구조 명확, 사운드/마우스 감도 저장 미구현 |
| **확장성** | ⭐⭐⭐⭐⭐ (5.0/5.0) | 탭 추가 쉬움, 새로운 설정 항목 추가 용이 |
| **에러 처리** | ⭐⭐⭐☆☆ (3.0/5.0) | 키 중복 검증 없음, 설정 저장 실패 처리 없음 |
| **성능** | ⭐⭐⭐⭐⭐ (5.0/5.0) | 간단한 UI, 성능 이슈 없음 |
| **일관성** | ⭐⭐⭐⭐⭐ (5.0/5.0) | Material Design 스타일 일관 적용 |

### **종합 평가**: ⭐⭐⭐⭐☆ (4.3/5.0)

---

## 결론 및 요약

`OptionDialog`는 **간결하고 명확한 설정 다이얼로그**로, 키 바인딩 설정을 잘 구현하고 있습니다. 탭 구조로 설정을 분류하여 확장 가능하며, Material Design 스타일을 일관되게 적용했습니다.

**주요 성과**:
- **탭 구조**: "일반", "키 설정" 탭으로 설정 분류
- **키 입력 캡처**: 직관적인 UI ("키를 누르세요..." + 주황색 배경)
- **기본값 복원**: 모든 설정을 한 번에 리셋
- **`KeyBindingConfig` 통합**: 깔끔한 API로 키 바인딩 관리
- **Material Design**: 다크 테마, 일관된 버튼 색상

**개선 영역** (중요도 높음):
1. **사운드/마우스 감도 저장**: `GeneralConfig` 클래스로 영속화 필요
2. **키 중복 검증**: 같은 키를 여러 액션에 할당 불가하도록 검증
3. **설정 변경 감지**: 변경된 경우에만 저장 가능하도록

**개선 영역** (선택 사항):
1. 취소 시 확인 다이얼로그
2. 슬라이더 값 실시간 표시
3. 프리셋 시스템
4. 설정 가져오기/내보내기

전반적으로 **잘 구현된 설정 다이얼로그**이며, 사운드/마우스 감도 저장과 키 중복 검증만 추가하면 완벽한 설정 화면이 될 것입니다.
