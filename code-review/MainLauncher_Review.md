# MainLauncher.java 코드 리뷰

## 📋 파일 개요
- **경로**: `src/com/fpsgame/client/MainLauncher.java`
- **목적**: 게임 진입점 및 플레이어 이름 입력 UI
- **라인 수**: ~170줄
- **역할**: 애플리케이션 시작 → 이름 입력 → 로비로 전환

## 🎯 주요 기능

### 1. 프로그램 진입점
```java
public static void main(String[] args) {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    SwingUtilities.invokeLater(() -> {
        MainLauncher launcher = new MainLauncher();
        launcher.setVisible(true);
    });
}
```
- JVM이 가장 먼저 실행하는 메서드
- Look and Feel 설정 후 UI 생성

### 2. 플레이어 이름 입력
```java
private JTextField nameField;
private void startGame() {
    String playerName = nameField.getText().trim();
    if (playerName.isEmpty()) {
        JOptionPane.showMessageDialog(...);
        return;
    }
    // 로비로 진입
}
```
- 텍스트 필드로 이름 입력
- 빈 문자열 검증

### 3. UI 초기화
```java
private void initUI() {
    // 타이틀 패널
    // 입력 패널
    // 버튼 패널
}
```
- BorderLayout으로 3개 영역 구성
- 한글 폰트 적용

## ✅ 장점

### 1. **SwingUtilities.invokeLater 사용**
```java
SwingUtilities.invokeLater(() -> {
    MainLauncher launcher = new MainLauncher();
    launcher.setVisible(true);
});
```
**효과**:
- EDT(Event Dispatch Thread)에서 UI 생성
- Thread-safety 보장
- Swing 권장 패턴 준수

### 2. **입력 검증**
```java
String playerName = nameField.getText().trim();
if (playerName.isEmpty()) {
    JOptionPane.showMessageDialog(this, "이름을 입력해주세요!", ...);
    return;
}
```
- 공백 제거 후 검증
- 사용자 친화적 오류 메시지
- 빈 이름으로 게임 진입 방지

### 3. **시스템 Look and Feel**
```java
UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
```
**장점**:
- Windows에서는 Windows 스타일
- macOS에서는 macOS 스타일
- 네이티브한 사용자 경험

### 4. **예외 처리**
```java
} catch (Exception e) {
    System.err.println("[MainLauncher] Failed to set Look and Feel");
    e.printStackTrace(System.err);
}
```
- Look and Feel 설정 실패해도 게임 실행
- 기본 스타일로 폴백

### 5. **리소스 정리**
```java
dispose(); // 런처 창 닫기
```
- 로비 열 때 런처 창 제거
- 메모리 누수 방지

### 6. **레이아웃 조립 패턴**
```java
add(titlePanel, BorderLayout.NORTH);
add(centerPanel, BorderLayout.CENTER);
add(buttonPanel, BorderLayout.SOUTH);
```
- 명확한 3단 구조
- 유지보수 쉬움

## ⚠️ 개선 가능 영역

### 1. **매직 넘버**
**현재 코드:**
```java
setSize(500, 350);
titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 36));
startButton.setPreferredSize(new Dimension(160, 45));
```

**문제점**:
- 하드코딩된 숫자들
- 의미 파악 어려움
- 수정 시 일관성 유지 힘듦

**개선 제안:**
```java
// 상수로 정의
private static final int WINDOW_WIDTH = 500;
private static final int WINDOW_HEIGHT = 350;
private static final int TITLE_FONT_SIZE = 36;
private static final int BUTTON_FONT_SIZE = 18;
private static final Dimension BUTTON_SIZE = new Dimension(160, 45);

// 사용
setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, TITLE_FONT_SIZE));
```

### 2. **폰트 하드코딩**
**현재 코드:**
```java
Font koreanFont = new Font("맑은 고딕", Font.PLAIN, 14);
```

**문제점**:
- "맑은 고딕"이 없는 시스템에서 기본 폰트로 폴백
- Linux/macOS에서 다른 폰트 사용

**개선 제안:**
```java
private static Font getDefaultFont(int style, int size) {
    String os = System.getProperty("os.name").toLowerCase();
    String fontName;
    
    if (os.contains("win")) {
        fontName = "맑은 고딕";
    } else if (os.contains("mac")) {
        fontName = "Apple SD Gothic Neo";
    } else {
        fontName = "Noto Sans CJK KR";
    }
    
    return new Font(fontName, style, size);
}
```

### 3. **색상 하드코딩**
**현재 코드:**
```java
titlePanel.setBackground(new Color(30, 40, 55));
centerPanel.setBackground(new Color(40, 50, 65));
startButton.setBackground(new Color(76, 175, 80));
```

**개선 제안:**
```java
// ColorScheme 클래스 생성
public class ColorScheme {
    public static final Color DARK_BG = new Color(30, 40, 55);
    public static final Color MEDIUM_BG = new Color(40, 50, 65);
    public static final Color SUCCESS_GREEN = new Color(76, 175, 80);
    public static final Color DANGER_RED = new Color(244, 67, 54);
}

// 사용
titlePanel.setBackground(ColorScheme.DARK_BG);
```

### 4. **이름 길이 제한 없음**
**현재 코드:**
```java
if (playerName.isEmpty()) { ... }
```

**문제점**:
- 매우 긴 이름 입력 가능
- 서버/UI에서 표시 문제 발생 가능

**개선 제안:**
```java
private static final int MIN_NAME_LENGTH = 2;
private static final int MAX_NAME_LENGTH = 16;

private void startGame() {
    String playerName = nameField.getText().trim();
    
    if (playerName.isEmpty()) {
        showError("이름을 입력해주세요!");
        return;
    }
    
    if (playerName.length() < MIN_NAME_LENGTH) {
        showError("이름은 최소 " + MIN_NAME_LENGTH + "자 이상이어야 합니다!");
        return;
    }
    
    if (playerName.length() > MAX_NAME_LENGTH) {
        showError("이름은 최대 " + MAX_NAME_LENGTH + "자까지 가능합니다!");
        return;
    }
    
    // ...
}

private void showError(String message) {
    JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
}
```

### 5. **특수문자 검증 부족**
**현재 코드:**
```java
String playerName = nameField.getText().trim();
// 어떤 문자든 허용
```

**문제점**:
- SQL Injection 유사 문제 (서버 측)
- UI 깨짐 (특수문자 렌더링)

**개선 제안:**
```java
private boolean isValidName(String name) {
    // 한글, 영문, 숫자만 허용
    return name.matches("^[a-zA-Z0-9가-힣]+$");
}

private void startGame() {
    String playerName = nameField.getText().trim();
    
    if (!isValidName(playerName)) {
        showError("이름은 한글, 영문, 숫자만 사용 가능합니다!");
        return;
    }
    
    // ...
}
```

### 6. **Enter 키 지원 부족**
**현재 코드:**
```java
nameField = new JTextField(15);
// Enter 키 입력 시 아무 일도 안 일어남
```

**개선 제안:**
```java
nameField = new JTextField(15);
nameField.addActionListener(e -> startGame()); // Enter 키 시 게임 시작

// 또는 getRootPane 사용
getRootPane().setDefaultButton(startButton);
```

### 7. **설정 저장/불러오기 없음**
**현재 상태**:
- 매번 이름 입력 필요

**개선 제안:**
```java
private void initUI() {
    // ...
    
    // 마지막 사용 이름 불러오기
    String lastUsedName = GameConfig.loadPlayerName();
    if (lastUsedName != null) {
        nameField.setText(lastUsedName);
    }
}

private void startGame() {
    String playerName = nameField.getText().trim();
    // ...
    
    // 이름 저장
    GameConfig.savePlayerName(playerName);
    
    // 로비 열기
    // ...
}
```

### 8. **로비 전환 실패 처리 부족**
**현재 코드:**
```java
SwingUtilities.invokeLater(() -> {
    LobbyFrame lobby = new LobbyFrame(playerName);
    lobby.setVisible(true);
    dispose();
});
```

**문제점**:
- LobbyFrame 생성 실패 시?
- 런처가 이미 닫혀서 복구 불가

**개선 제안:**
```java
SwingUtilities.invokeLater(() -> {
    try {
        LobbyFrame lobby = new LobbyFrame(playerName);
        lobby.setVisible(true);
        dispose();
    } catch (Exception ex) {
        ex.printStackTrace(System.err);
        JOptionPane.showMessageDialog(
            this,
            "로비 진입에 실패했습니다: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
});
```

## 🏗️ 아키텍처 분석

### UI 구조
```
MainLauncher (JFrame)
    ├── titlePanel (BorderLayout.NORTH)
    │   └── titleLabel ("FPS 게임")
    ├── centerPanel (BorderLayout.CENTER)
    │   ├── nameLabel ("플레이어 이름:")
    │   └── nameField (JTextField)
    └── buttonPanel (BorderLayout.SOUTH)
        ├── startButton ("게임 시작")
        └── exitButton ("종료")
```

### 이벤트 흐름
```
1. main() 실행
2. Look and Feel 설정
3. MainLauncher 생성
4. initUI() 호출
5. 사용자 이름 입력
6. "게임 시작" 버튼 클릭
7. startGame() 호출
8. 이름 검증
9. LobbyFrame 생성
10. MainLauncher dispose
```

### 의존성
```
MainLauncher
    ├── LobbyFrame (로비로 전환)
    └── Swing 컴포넌트
```

## 📊 성능 고려사항

### 메모리 사용
```java
MainLauncher 객체: ~1KB
└── UI 컴포넌트들: ~10KB
총: ~11KB (무시 가능)
```

### 시작 시간
```
Look and Feel 설정: ~50ms
UI 생성: ~100ms
화면 표시: ~50ms
총: ~200ms (충분히 빠름)
```

## 🧪 테스트 시나리오

### 1. 정상 시나리오
```
1. 프로그램 시작
2. "Player1" 입력
3. "게임 시작" 클릭
→ 로비 열림, 런처 닫힘
```

### 2. 빈 이름 입력
```
1. 이름 입력하지 않음
2. "게임 시작" 클릭
→ 오류 메시지 표시
```

### 3. 공백만 입력
```
1. "   " 입력
2. "게임 시작" 클릭
→ trim() 후 빈 문자열로 인식, 오류 표시
```

### 4. 종료 버튼
```
1. "종료" 버튼 클릭
→ 프로그램 종료 (System.exit(0))
```

## 📈 사용 예시

### 기본 사용
```java
// 실행
java -jar NetFps.jar

// 또는
javac MainLauncher.java
java com.fpsgame.client.MainLauncher
```

### 커스텀 Look and Feel
```java
public static void main(String[] args) {
    try {
        // Nimbus Look and Feel 사용
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                UIManager.setLookAndFeel(info.getClassName());
                break;
            }
        }
    } catch (Exception e) {
        // 폴백
    }
    
    SwingUtilities.invokeLater(() -> {
        new MainLauncher().setVisible(true);
    });
}
```

## 🎓 학습 포인트

### 초보자를 위한 핵심 개념
1. **JFrame 사용법**: Swing 윈도우 생성
2. **BorderLayout**: 5개 영역 레이아웃
3. **ActionListener**: 버튼 클릭 이벤트 처리

### 중급자를 위한 심화 개념
1. **EDT**: Event Dispatch Thread의 중요성
2. **Look and Feel**: 플랫폼별 스타일
3. **dispose() vs setVisible(false)**: 리소스 정리

### 고급 주제
1. **SplashScreen**: 로딩 화면 표시
2. **JLayeredPane**: 복잡한 레이아웃
3. **MVC 패턴**: Model-View-Controller 분리

## 🔍 코드 품질 평가

| 항목 | 평가 | 설명 |
|------|------|------|
| **가독성** | ⭐⭐⭐⭐⭐ | 매우 명확한 구조 |
| **유지보수성** | ⭐⭐⭐⭐ | 간단한 UI, 쉬운 수정 |
| **확장성** | ⭐⭐⭐ | 추가 필드 쉽게 추가 가능 |
| **성능** | ⭐⭐⭐⭐⭐ | 시작 시간 충분히 빠름 |
| **안정성** | ⭐⭐⭐⭐ | 입력 검증, 예외 처리 양호 |

## 📝 종합 평가

### 강점
✅ **EDT 준수**: SwingUtilities.invokeLater 사용  
✅ **입력 검증**: 빈 문자열 체크  
✅ **사용자 친화적**: 한글 폰트, 명확한 메시지  
✅ **시스템 통합**: 네이티브 Look and Feel  

### 개선 제안 우선순위
1. **이름 길이 제한** (높음) - 2~16자
2. **특수문자 검증** (높음) - 정규식 사용
3. **Enter 키 지원** (중간) - 사용성 개선
4. **상수화** (중간) - 매직 넘버 제거
5. **이름 저장** (낮음) - 편의 기능
6. **색상 테마** (낮음) - 일관성

### 결론
**기능적으로 완성도 높은 런처**입니다. 기본 요구사항은 모두 충족하며, Swing 권장 패턴을 잘 따릅니다. 입력 검증 강화만 추가하면 프로덕션 레벨입니다.

**권장사항**:
1. **즉시 적용**:
   - 이름 길이 제한 (2~16자)
   - 특수문자 검증 (정규식)
   - Enter 키 지원
   
2. **다음 버전**:
   - 마지막 사용 이름 저장
   - 색상/폰트 상수화
   
3. **선택 적용**:
   - 다국어 지원 (i18n)
   - 설정 화면 추가
   - 소셜 로그인

**UI/UX 개선 아이디어**:
- 프로필 사진 선택
- 테마 선택 (다크/라이트 모드)
- 최근 사용 이름 드롭다운
- 애니메이션 전환 효과
