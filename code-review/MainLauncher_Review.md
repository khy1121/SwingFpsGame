# MainLauncher.java 코드 리뷰

## 📋 파일 개요
- **경로**: `src/com/fpsgame/client/MainLauncher.java`
- **역할**: 게임 진입점 및 플레이어 이름 입력 화면
- **라인 수**: 153줄
- **UI 프레임워크**: Java Swing
- **주요 기능**: 플레이어 이름 입력, 로비 진입, 게임 시작

---

## 🎯 주요 기능

### 1. JFrame 기반 런처 창
```java
public class MainLauncher extends JFrame {
    /** 플레이어 이름 입력 필드 */
    private JTextField nameField;
    
    /** 게임 시작 버튼 */
    private JButton startButton;
    
    /** 게임 종료 버튼 */
    private JButton exitButton;

    public MainLauncher() {
        super("FPS Game Launcher");
        initUI();
    }
}
```
- **JFrame 상속**: Swing 윈도우 프레임
- **500x350 크기**: 작고 간결한 런처 창
- **중앙 배치**: `setLocationRelativeTo(null)` - 화면 중앙

### 2. UI 레이아웃 (BorderLayout)
```java
private void initUI() {
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setSize(500, 350);
    setLocationRelativeTo(null);
    setLayout(new BorderLayout());
    
    // 레이아웃 조립
    add(titlePanel, BorderLayout.NORTH);   // 상단: 타이틀
    add(centerPanel, BorderLayout.CENTER); // 중앙: 이름 입력
    add(buttonPanel, BorderLayout.SOUTH);  // 하단: 버튼들
}
```
**3단 레이아웃**:
```
┌─────────────────────────────────┐
│ NORTH: 타이틀 ("FPS 게임")       │
├─────────────────────────────────┤
│ CENTER: 이름 입력 필드            │
│   [플레이어 이름: ___________]   │
├─────────────────────────────────┤
│ SOUTH: 버튼                      │
│   [게임 시작]  [종료]            │
└─────────────────────────────────┘
```

### 3. 한글 폰트 설정
```java
// 한글 폰트 설정
Font koreanFont = new Font("맑은 고딕", Font.PLAIN, 14);     // 일반 텍스트
Font koreanBold = new Font("맑은 고딕", Font.BOLD, 16);      // 라벨
Font titleFont = new Font("맑은 고딕", Font.BOLD, 36);       // 타이틀
```
**윈도우 기본 폰트**:
- **맑은 고딕**: Windows Vista 이후 기본 한글 폰트
- **크기 구분**: 타이틀(36pt) > 라벨(16pt) > 일반(14pt)
- **한글 깨짐 방지**: 명시적 폰트 지정

### 4. 타이틀 패널 (어두운 배경)
```java
// 타이틀 패널
JPanel titlePanel = new JPanel();
titlePanel.setBackground(new Color(30, 40, 55)); // 어두운 남색
titlePanel.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20)); // 여백

JLabel titleLabel = new JLabel("FPS 게임");
titleLabel.setFont(titleFont); // 36pt 볼드
titleLabel.setForeground(Color.WHITE); // 흰색 글자
titlePanel.add(titleLabel);
```
**색상 조합**:
- **배경**: RGB(30, 40, 55) - 어두운 남색
- **글자**: 흰색 - 높은 대비
- **여백**: 상30, 좌우20, 하20 픽셀

### 5. 중앙 패널 (이름 입력)
```java
// 중앙 패널 - GridBagLayout
JPanel centerPanel = new JPanel(new GridBagLayout());
centerPanel.setBackground(new Color(40, 50, 65));
GridBagConstraints gbc = new GridBagConstraints();
gbc.insets = new Insets(10, 10, 10, 10); // 여백

// 이름 라벨 (0, 0)
gbc.gridx = 0;
gbc.gridy = 0;
JLabel nameLabel = new JLabel("플레이어 이름:");
nameLabel.setForeground(Color.BLACK);
nameLabel.setFont(koreanBold); // 16pt 볼드
centerPanel.add(nameLabel, gbc);

// 이름 입력 필드 (1, 0)
gbc.gridx = 1;
nameField = new JTextField(15); // 15자 너비
nameField.setFont(koreanFont);
centerPanel.add(nameField, gbc);
```
**GridBagLayout 사용 이유**:
- **정렬 제어**: 라벨과 입력 필드를 깔끔하게 배치
- **여백 설정**: `insets`로 요소 간 간격 조절

### 6. 버튼 패널 (시작/종료)
```java
// 버튼 패널
JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
buttonPanel.setBackground(new Color(40, 50, 65));

// 시작 버튼
startButton = new JButton("게임 시작");
startButton.setFont(new Font("맑은 고딕", Font.BOLD, 18));
startButton.setPreferredSize(new Dimension(160, 45)); // 160x45 픽셀
startButton.setBackground(new Color(76, 175, 80));    // 녹색 (Material Green)
startButton.setForeground(Color.BLACK);
startButton.setFocusPainted(false);                   // 포커스 테두리 제거
startButton.addActionListener(e -> startGame());

// 종료 버튼
exitButton = new JButton("종료");
exitButton.setFont(new Font("맑은 고딕", Font.BOLD, 18));
exitButton.setPreferredSize(new Dimension(160, 45));
exitButton.setBackground(new Color(244, 67, 54));     // 빨간색 (Material Red)
exitButton.setForeground(Color.BLACK);
exitButton.setFocusPainted(false);
exitButton.addActionListener(e -> System.exit(0));

buttonPanel.add(startButton);
buttonPanel.add(exitButton);
```
**Material Design 색상**:
- **시작 버튼**: RGB(76, 175, 80) - Green 500
- **종료 버튼**: RGB(244, 67, 54) - Red 500
- **버튼 크기**: 160x45 픽셀 (일관된 크기)

### 7. 게임 시작 처리
```java
/**
 * 게임 시작 처리
 */
private void startGame() {
    String playerName = nameField.getText().trim();
    
    // 이름 입력 검증
    if (playerName.isEmpty()) {
        JOptionPane.showMessageDialog(this,
                "이름을 입력해주세요!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        return;
    }

    // 로비 프레임 열기
    SwingUtilities.invokeLater(() -> {
        LobbyFrame lobby = new LobbyFrame(playerName);
        lobby.setVisible(true);
        dispose(); // 런처 창 닫기
    });
}
```
**처리 단계**:
1. **입력값 가져오기**: `nameField.getText().trim()`
2. **검증**: 빈 문자열 체크
3. **에러 다이얼로그**: `JOptionPane.showMessageDialog()`
4. **로비 생성**: `new LobbyFrame(playerName)`
5. **런처 닫기**: `dispose()`

**EDT 사용**:
- `SwingUtilities.invokeLater()`: Swing UI는 EDT에서만 변경 가능

### 8. 프로그램 진입점
```java
/**
 * 프로그램 진입점
 */
public static void main(String[] args) {
    // 시스템 Look and Feel 적용
    try {
        javax.swing.UIManager.setLookAndFeel(
            javax.swing.UIManager.getSystemLookAndFeelClassName()
        );
    } catch (Exception e) {
        System.err.println("[MainLauncher] Failed to set Look and Feel");
        e.printStackTrace(System.err);
    }

    // 런처 창 표시
    SwingUtilities.invokeLater(() -> {
        MainLauncher launcher = new MainLauncher();
        launcher.setVisible(true);
    });
}
```
**Look and Feel**:
- **시스템 기본**: Windows에서는 Windows 스타일, Mac에서는 Mac 스타일
- **폴백**: 실패 시 기본 Metal L&F 사용

---

## 💡 강점

### 1. 간결한 UI
- **3단 레이아웃**: 타이틀, 입력, 버튼 (명확한 구조)
- **최소한의 요소**: 필요한 것만 표시 (이름 입력, 시작, 종료)
- **적절한 크기**: 500x350 픽셀 (작지만 답답하지 않음)

### 2. 한글 지원 완벽
```java
Font koreanFont = new Font("맑은 고딕", Font.PLAIN, 14);
```
- **한글 폰트 명시**: 깨짐 방지
- **윈도우 기본 폰트**: 맑은 고딕 (Windows Vista+)

### 3. Material Design 색상
- **일관된 디자인**: Google Material Design 색상 팔레트
- **시각적 피드백**: 녹색(시작) vs 빨간색(종료)

### 4. 입력 검증
```java
if (playerName.isEmpty()) {
    JOptionPane.showMessageDialog(this, "이름을 입력해주세요!", ...);
    return;
}
```
- **빈 문자열 체크**: 이름 없이 시작 방지
- **trim() 사용**: 공백만 입력한 경우도 차단

### 5. EDT 준수
```java
SwingUtilities.invokeLater(() -> {
    MainLauncher launcher = new MainLauncher();
    launcher.setVisible(true);
});
```
- **스레드 안전**: Swing UI는 EDT에서만 변경

---

## 🔧 개선 제안

### 1. 이름 길이 제한 (중요도: 중간)
**현재 상태**: 이름 길이 제한 없음

**문제점**:
- 매우 긴 이름 입력 가능 (50자 이상)
- UI 레이아웃 깨질 수 있음

**제안**:
```java
// 이름 입력 필드 생성 시
nameField = new JTextField(15);
nameField.setFont(koreanFont);

// DocumentFilter로 최대 길이 제한
((AbstractDocument) nameField.getDocument()).setDocumentFilter(new DocumentFilter() {
    private static final int MAX_LENGTH = 12;
    
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        if ((fb.getDocument().getLength() + string.length()) <= MAX_LENGTH) {
            super.insertString(fb, offset, string, attr);
        }
    }
    
    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        int newLength = fb.getDocument().getLength() - length + text.length();
        if (newLength <= MAX_LENGTH) {
            super.replace(fb, offset, length, text, attrs);
        }
    }
});
```

### 2. 엔터키로 게임 시작 (중요도: 높음)
**현재 상태**: 버튼 클릭만 가능

**제안**:
```java
// 이름 입력 필드에 엔터키 리스너 추가
nameField.addActionListener(e -> startGame());

// 또는 키 리스너
nameField.addKeyListener(new KeyAdapter() {
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            startGame();
        }
    }
});
```

### 3. 이름 중복 체크 (중요도: 낮음)
**현재 상태**: 서버에서만 중복 체크

**제안**:
```java
private void startGame() {
    String playerName = nameField.getText().trim();
    
    if (playerName.isEmpty()) {
        showError("이름을 입력해주세요!");
        return;
    }
    
    // 특수문자 체크
    if (!playerName.matches("^[a-zA-Z0-9가-힣_]+$")) {
        showError("이름에는 영문, 한글, 숫자, 밑줄(_)만 사용할 수 있습니다.");
        return;
    }
    
    // ... (기존 로직)
}

private void showError(String message) {
    JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
}
```

### 4. 아이콘 추가 (중요도: 낮음)
**현재 상태**: 기본 Java 아이콘

**제안**:
```java
private void initUI() {
    // ... (기존 코드)
    
    // 타이틀바 아이콘 설정
    try {
        Image icon = ImageIO.read(new File("assets/icon.png"));
        setIconImage(icon);
    } catch (IOException e) {
        System.err.println("[MainLauncher] Failed to load icon");
    }
}
```

### 5. 플레이어 이름 기억 (중요도: 낮음)
**현재 상태**: 매번 입력 필요

**제안**:
```java
import java.util.prefs.*;

private void loadLastName() {
    Preferences prefs = Preferences.userNodeForPackage(MainLauncher.class);
    String lastPlayerName = prefs.get("playerName", "");
    nameField.setText(lastPlayerName);
    nameField.selectAll(); // 텍스트 선택 상태로
}

private void savePlayerName(String playerName) {
    Preferences prefs = Preferences.userNodeForPackage(MainLauncher.class);
    prefs.put("playerName", playerName);
}

private void startGame() {
    String playerName = nameField.getText().trim();
    
    if (playerName.isEmpty()) {
        showError("이름을 입력해주세요!");
        return;
    }
    
    // 이름 저장
    savePlayerName(playerName);
    
    // ... (기존 로직)
}

public MainLauncher() {
    super("FPS Game Launcher");
    initUI();
    loadLastName(); // 마지막 이름 불러오기
}
```

### 6. 버튼 호버 효과 (중요도: 낮음)
**현재 상태**: 정적인 버튼

**제안**:
```java
// 버튼 호버 효과 추가
startButton.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseEntered(MouseEvent e) {
        startButton.setBackground(new Color(67, 160, 71)); // 약간 어두운 녹색
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        startButton.setBackground(new Color(76, 175, 80)); // 원래 녹색
    }
});

exitButton.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseEntered(MouseEvent e) {
        exitButton.setBackground(new Color(229, 57, 53)); // 약간 어두운 빨강
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        exitButton.setBackground(new Color(244, 67, 54)); // 원래 빨강
    }
});
```

### 7. 로딩 인디케이터 (중요도: 낮음)
**현재 상태**: 로비 생성 시 즉시 전환

**제안**:
```java
private void startGame() {
    String playerName = nameField.getText().trim();
    
    if (playerName.isEmpty()) {
        showError("이름을 입력해주세요!");
        return;
    }
    
    // 버튼 비활성화
    startButton.setEnabled(false);
    startButton.setText("로딩 중...");
    
    // 백그라운드 스레드에서 로비 생성
    new Thread(() -> {
        try {
            // 리소스 사전 로드 등
            Thread.sleep(500);
            
            // EDT에서 UI 생성
            SwingUtilities.invokeLater(() -> {
                LobbyFrame lobby = new LobbyFrame(playerName);
                lobby.setVisible(true);
                dispose();
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                startButton.setEnabled(true);
                startButton.setText("게임 시작");
                showError("로비 생성 실패: " + e.getMessage());
            });
        }
    }).start();
}
```

---

## 📊 코드 품질 평가

| 항목 | 점수 | 설명 |
|------|------|------|
| **UI 디자인** | ⭐⭐⭐⭐⭐ | 간결하고 명확한 3단 레이아웃 |
| **한글 지원** | ⭐⭐⭐⭐⭐ | 맑은 고딕 폰트 명시적 사용 |
| **입력 검증** | ⭐⭐⭐☆☆ | 빈 문자열만 체크, 길이/특수문자 미체크 |
| **사용성** | ⭐⭐⭐☆☆ | 엔터키 미지원, 이름 기억 안 함 |
| **코드 간결성** | ⭐⭐⭐⭐⭐ | 153줄, 명확한 구조 |
| **EDT 준수** | ⭐⭐⭐⭐⭐ | SwingUtilities.invokeLater 사용 |

**총점: 4.2 / 5.0** ⭐⭐⭐⭐☆

---

## 🎓 결론

MainLauncher.java는 **간결하고 직관적인 게임 진입 화면**입니다. 특히 **한글 폰트 지원**, **Material Design 색상**, **EDT 준수**가 인상적입니다.

### 주요 성과
1. ✅ **간결한 UI**: 3단 레이아웃 (타이틀, 입력, 버튼)
2. ✅ **한글 지원**: 맑은 고딕 폰트 명시적 사용
3. ✅ **Material Design**: 녹색(시작) vs 빨간색(종료)
4. ✅ **입력 검증**: 빈 문자열 체크
5. ✅ **EDT 준수**: SwingUtilities.invokeLater 사용

### 개선 방향
1. **엔터키 지원**: nameField에 ActionListener 추가 (필수!)
2. **이름 길이 제한**: DocumentFilter로 12자 제한
3. **이름 기억**: Preferences API로 마지막 이름 저장
4. **특수문자 체크**: 정규식으로 검증

**프로덕션 레벨**이며, 엔터키 지원만 추가하면 **완벽한 런처 화면**입니다. 🎉
