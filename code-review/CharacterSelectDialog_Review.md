# CharacterSelectDialog 코드 리뷰

## 파일 정보
- **파일명**: `CharacterSelectDialog.java`
- **패키지**: `com.fpsgame.client`
- **라인 수**: 454줄
- **역할**: 캐릭터 선택 모달 다이얼로그 (팀 선택 후 B키로 표시)

---

## 파일 개요

`CharacterSelectDialog`는 플레이어가 게임에서 사용할 캐릭터를 선택하는 모달 창입니다. 팀 선택 후에만 사용 가능하며, **같은 팀 내 캐릭터 중복 선택을 방지**하는 핵심 기능을 제공합니다. 10개의 캐릭터 중 현재 **4개(Raven, Piper, Technician, General)**만 활성화되어 있으며, 나머지 6개는 "추후 업데이트 예정"으로 표시됩니다.

주요 기능:
1. **2x5 그리드 레이아웃**: 10개 캐릭터 카드 배치
2. **캐릭터 정보 표시**: 이름, 역할, 설명, 스탯 (HP, 속도, 방어)
3. **이미지 로드**: 리소스 또는 파일 시스템에서 캐릭터 이미지 로드
4. **중복 방지**: 같은 팀에서 선택된 캐릭터는 비활성화
5. **선택자 표시**: 누가 어떤 캐릭터를 선택했는지 표시
6. **타임아웃 지원**: 선택 시간 제한 기능 (모달 자동 닫기)

---

## 주요 기능

### 1. 생성자 오버로딩

```java
/** 기본 생성자 */
public CharacterSelectDialog(Frame parent) {
    this(parent, new java.util.HashSet<>(), new java.util.HashMap<>());
}

/** 전체 생성자 */
public CharacterSelectDialog(Frame parent, 
                           java.util.Set<String> disabledCharacters,
                           java.util.Map<String, String> characterOwners) {
    super(parent, "캐릭터 선택", true);
    this.disabledCharacters = disabledCharacters != null ? disabledCharacters : new java.util.HashSet<>();
    this.characterOwners = characterOwners != null ? characterOwners : new java.util.HashMap<>();
    initUI();
}
```

**특징**:
- **기본 생성자**: 빈 `disabledCharacters`, `characterOwners` 사용
- **전체 생성자**: 같은 팀에서 선택된 캐릭터 목록과 선택자 정보 전달
- **Null 안전**: 매개변수가 `null`이면 빈 컬렉션으로 초기화

### 2. 캐릭터 이미지 로드

```java
private ImageIcon loadCharacterImage(String characterId, int width, int height) {
    String[] extensions = { ".png", ".jpg", ".jpeg" };

    // 파일명: 첫 글자 대문자 (예: raven -> Raven)
    String fileName = characterId.substring(0, 1).toUpperCase() 
                    + characterId.substring(1).toLowerCase();

    // 1. 먼저 리소스에서 로드 시도 (src/main/resources/assets/characters/)
    for (String ext : extensions) {
        String resourcePath = "/assets/characters/" + fileName + ext;
        try {
            java.net.URL imgURL = getClass().getResource(resourcePath);
            if (imgURL != null) {
                BufferedImage img = ImageIO.read(imgURL);
                if (img != null) {
                    Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaledImg);
                }
            }
        } catch (IOException e) {
            // 리소스 로드 실패 시 다음 확장자 시도
        }
    }

    // 2. 리소스에서 못 찾으면 파일 시스템에서 로드 시도 (assets/characters/)
    for (String ext : extensions) {
        File imageFile = new File("assets/characters/" + fileName + ext);
        if (imageFile.exists()) {
            try {
                BufferedImage img = ImageIO.read(imageFile);
                if (img != null) {
                    Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaledImg);
                }
            } catch (IOException e) {
                // 이미지 로드 실패 시 다음 확장자 시도
            }
        }
    }

    // 이미지가 없으면 null 반환 (대체 UI 표시)
    return null;
}
```

**특징**:
- **2단계 로드**: 리소스 → 파일 시스템 순서로 시도
- **3가지 확장자 지원**: `.png`, `.jpg`, `.jpeg`
- **파일명 정규화**: `raven` → `Raven` (첫 글자 대문자)
- **크기 조절**: `SCALE_SMOOTH` 알고리즘으로 부드럽게 리사이징
- **Null 안전**: 이미지가 없으면 `null` 반환 (대체 UI 표시)

### 3. 캐릭터 카드 생성

```java
private JPanel createCharacterCard(CharacterData data, Color cardBg, Color hoverBg, 
                                   Font normalFont, Font boldFont) {
    // 비활성화 여부 확인: 같은 팀에서 선택됨 OR 아직 구현되지 않은 캐릭터
    boolean isDisabled = disabledCharacters.contains(data.id) 
                      || !AVAILABLE_CHARACTERS.contains(data.id);
    boolean isComingSoon = !AVAILABLE_CHARACTERS.contains(data.id); // 추후 업데이트 예정

    JPanel card = new JPanel(new BorderLayout(8, 8));

    // 비활성화된 캐릭터는 어두운 배경
    Color actualCardBg = isDisabled ? new Color(40, 40, 40) : cardBg;
    Color actualHoverBg = isDisabled ? new Color(50, 50, 50) : hoverBg;

    card.setBackground(actualCardBg);
    card.setBorder(new CompoundBorder(
            new LineBorder(new Color(70, 72, 78), 2),
            new EmptyBorder(12, 12, 12, 12)));

    // 비활성화된 캐릭터는 기본 커서
    card.setCursor(isDisabled ? Cursor.getDefaultCursor() 
                              : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    // 상단: 캐릭터 이미지 또는 대체 아이콘
    JLabel imageLabel = new JLabel();
    imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    imageLabel.setPreferredSize(new Dimension(80, 80));

    ImageIcon characterIcon = loadCharacterImage(data.id, 80, 80);
    if (characterIcon != null) {
        imageLabel.setIcon(characterIcon);
        // 비활성화된 캐릭터는 이미지를 반투명하게
        if (isDisabled) {
            imageLabel.setEnabled(false);
        }
    } else {
        // 이미지가 없으면 첫 글자를 큰 텍스트로 표시
        imageLabel.setText(data.name.substring(0, 1).toUpperCase());
        imageLabel.setFont(new Font("맑은 고딕", Font.BOLD, 48));
        imageLabel.setForeground(isDisabled ? new Color(80, 80, 80) : new Color(100, 150, 255));
    }

    card.add(imageLabel, BorderLayout.NORTH);

    // 캐릭터 정보
    JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
    infoPanel.setOpaque(false);

    JLabel nameLabel = new JLabel(data.name);
    nameLabel.setFont(boldFont);
    nameLabel.setForeground(Color.WHITE);
    nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel roleLabel = new JLabel("[" + data.role + "]");
    roleLabel.setFont(normalFont);
    roleLabel.setForeground(new Color(150, 200, 255));
    roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel descLabel = new JLabel("<html><center>" + data.description + "</center></html>");
    descLabel.setFont(normalFont);
    descLabel.setForeground(Color.LIGHT_GRAY);
    descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    infoPanel.add(nameLabel);
    infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    infoPanel.add(roleLabel);
    infoPanel.add(Box.createRigidArea(new Dimension(0, 8)));
    infoPanel.add(descLabel);

    // 스탯
    JPanel statsPanel = new JPanel(new GridLayout(3, 1, 0, 3));
    statsPanel.setOpaque(false);

    JLabel hpLabel = new JLabel("HP: " + (int) data.health);
    hpLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
    hpLabel.setForeground(new Color(255, 180, 180));

    JLabel speedLabel = new JLabel("속도: " + String.format("%.1f", data.speed));
    speedLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
    speedLabel.setForeground(new Color(180, 255, 180));

    JLabel armorLabel = new JLabel("방어: " + (int) data.armor);
    armorLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
    armorLabel.setForeground(new Color(180, 220, 255));

    statsPanel.add(hpLabel);
    statsPanel.add(speedLabel);
    statsPanel.add(armorLabel);

    card.add(infoPanel, BorderLayout.CENTER);
    card.add(statsPanel, BorderLayout.SOUTH);

    // 비활성화된 캐릭터에는 마우스 이벤트 없음
    if (!isDisabled) {
        // 마우스 이벤트
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(actualHoverBg);
                card.setBorder(new CompoundBorder(
                        new LineBorder(new Color(100, 150, 255), 2),
                        new EmptyBorder(12, 12, 12, 12)));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!data.id.equals(selectedCharacterId)) {
                    card.setBackground(actualCardBg);
                    card.setBorder(new CompoundBorder(
                            new LineBorder(new Color(70, 72, 78), 2),
                            new EmptyBorder(12, 12, 12, 12)));
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                selectedCharacterId = data.id;
                // 모든 카드 초기화
                Container parent = card.getParent();
                if (parent instanceof JPanel) {
                    for (Component comp : ((JPanel) parent).getComponents()) {
                        if (comp instanceof JPanel) {
                            comp.setBackground(actualCardBg);
                            ((JPanel) comp).setBorder(new CompoundBorder(
                                    new LineBorder(new Color(70, 72, 78), 2),
                                    new EmptyBorder(12, 12, 12, 12)));
                        }
                    }
                }
                // 선택된 카드 하이라이트
                card.setBackground(new Color(67, 69, 94));
                card.setBorder(new CompoundBorder(
                        new LineBorder(new Color(100, 200, 255), 3),
                        new EmptyBorder(12, 12, 12, 12)));
            }
        };

        card.addMouseListener(mouseHandler);
    } else {
        // 비활성화된 캐릭터에는 선택한 플레이어 이름 또는 "추후 업데이트 예정" 표시
        String labelText;
        Color labelColor;
        
        if (isComingSoon) {
            labelText = "추후 업데이트 예정";
            labelColor = new Color(200, 200, 100);
        } else {
            String ownerName = characterOwners.get(data.id);
            labelText = ownerName != null ? ownerName + " 선택함" : "(선택됨)";
            labelColor = new Color(255, 100, 100);
        }

        JLabel disabledLabel = new JLabel(labelText);
        disabledLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        disabledLabel.setForeground(labelColor);
        disabledLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(disabledLabel, BorderLayout.NORTH);
    }

    return card;
}
```

**특징**:
- **비활성화 조건 2가지**:
  1. 같은 팀에서 선택됨 (`disabledCharacters.contains(data.id)`)
  2. 아직 구현되지 않음 (`!AVAILABLE_CHARACTERS.contains(data.id)`)
- **이미지 대체**: 이미지 없으면 첫 글자를 큰 폰트로 표시
- **스탯 색상 코딩**: HP(빨강), 속도(초록), 방어(파랑)
- **hover 효과**: 마우스 오버 시 파란 테두리 + 밝은 배경
- **선택 표시**: 선택된 카드는 파란 테두리 (3px) + 다른 배경색
- **비활성화 표시**: "추후 업데이트 예정" 또는 "플레이어명 선택함"

### 4. 정적 팩토리 메서드

```java
/** 기본 다이얼로그 표시 */
public static String showDialog(Frame parent) {
    return showDialog(parent, new java.util.HashSet<>(), new java.util.HashMap<>());
}

/** 비활성화 목록 포함 */
public static String showDialog(Frame parent, java.util.Set<String> disabledCharacters) {
    return showDialog(parent, disabledCharacters, new java.util.HashMap<>());
}

/** 비활성화 목록 + 선택자 정보 포함 */
public static String showDialog(Frame parent, java.util.Set<String> disabledCharacters,
                                java.util.Map<String, String> characterOwners) {
    return showDialog(parent, disabledCharacters, characterOwners, -1);
}

/** 타임아웃 지원 */
public static String showDialog(Frame parent, java.util.Set<String> disabledCharacters,
                                java.util.Map<String, String> characterOwners, long timeoutMs) {
    CharacterSelectDialog dialog = new CharacterSelectDialog(parent, disabledCharacters, characterOwners);

    javax.swing.Timer autoCloseTimer = null;
    if (timeoutMs > 0) {
        int delay = (int) Math.min(Integer.MAX_VALUE, Math.max(1, timeoutMs));
        autoCloseTimer = new javax.swing.Timer(delay, e -> dialog.dispose());
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();
    }

    dialog.setVisible(true);

    if (autoCloseTimer != null && autoCloseTimer.isRunning()) {
        autoCloseTimer.stop();
    }

    return dialog.isConfirmed() ? dialog.getSelectedCharacterId() : null;
}
```

**특징**:
- **4가지 오버로딩**: 매개변수 개수에 따라 다양한 사용법 지원
- **타임아웃 지원**: `timeoutMs` 후 자동으로 다이얼로그 닫기
- **편리한 API**: `showDialog(parent)` 한 줄로 캐릭터 선택 가능
- **null 반환**: 취소 시 `null` 반환

### 5. 사용 가능한 캐릭터 제한

```java
/** 현재 사용 가능한 캐릭터 목록 */
private static final java.util.Set<String> AVAILABLE_CHARACTERS = 
    new java.util.HashSet<>(java.util.Arrays.asList(
        "raven", "piper", "technician", "general"
    ));
```

**특징**:
- **4개 활성**: Raven, Piper, Technician, General
- **6개 비활성**: Bulldog, Ghost, Sage, Skull, Steam, Wildcat
- **"추후 업데이트 예정" 표시**: 비활성 캐릭터 카드에 노란색 라벨

### 6. ESC키 닫기 지원

```java
// ESC키로 닫기
getRootPane().registerKeyboardAction(
        e -> dispose(),
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW);
```

**특징**:
- **ESC 단축키**: 다이얼로그를 빠르게 닫을 수 있음
- **전역 단축키**: `WHEN_IN_FOCUSED_WINDOW` 사용으로 어느 컴포넌트에서나 동작

---

## 강점

### 1. **중복 방지 시스템**
- **같은 팀 내 중복 방지**: `disabledCharacters` Set으로 선택된 캐릭터 비활성화
- **선택자 표시**: `characterOwners` Map으로 누가 선택했는지 표시
- **시각적 피드백**: 비활성화된 카드는 어두운 배경 + "플레이어명 선택함" 라벨

### 2. **유연한 이미지 로드**
- **2단계 로드**: 리소스 → 파일 시스템
- **3가지 확장자**: `.png`, `.jpg`, `.jpeg`
- **대체 UI**: 이미지 없으면 첫 글자를 큰 폰트로 표시
- **크기 조절**: `SCALE_SMOOTH` 알고리즘

### 3. **정적 팩토리 메서드**
- **4가지 오버로딩**: 다양한 사용 시나리오 지원
- **타임아웃 지원**: 선택 시간 제한 기능
- **간결한 API**: `String characterId = CharacterSelectDialog.showDialog(parent);`

### 4. **Material Design 스타일**
- **다크 테마**: Discord 스타일 배경색
- **hover 효과**: 마우스 오버 시 파란 테두리
- **선택 표시**: 파란 테두리 (3px) + 다른 배경색
- **스탯 색상 코딩**: HP(빨강), 속도(초록), 방어(파랑)

### 5. **미구현 캐릭터 처리**
- **`AVAILABLE_CHARACTERS` Set**: 활성 캐릭터 목록 관리
- **"추후 업데이트 예정" 라벨**: 노란색 라벨로 명확히 표시
- **비활성화 커서**: 기본 커서 사용

### 6. **ESC키 지원**
- **빠른 닫기**: ESC키로 다이얼로그 닫기
- **사용자 편의**: 마우스 없이도 조작 가능

---

## 개선 제안

### 1. **캐릭터 필터링** (중요도: 중간)

현재는 10개 캐릭터를 모두 표시하지만, 활성 캐릭터만 표시하는 옵션이 있으면 좋습니다.

```java
public enum DisplayMode {
    ALL,           // 모든 캐릭터 표시 (기본)
    AVAILABLE_ONLY // 활성 캐릭터만 표시
}

public CharacterSelectDialog(Frame parent, 
                           java.util.Set<String> disabledCharacters,
                           java.util.Map<String, String> characterOwners,
                           DisplayMode mode) {
    // ...
    List<CharacterData> charactersToDisplay = filterCharacters(mode);
    // ...
}

private List<CharacterData> filterCharacters(DisplayMode mode) {
    if (mode == DisplayMode.AVAILABLE_ONLY) {
        return Arrays.stream(CharacterData.CHARACTERS)
                     .filter(c -> AVAILABLE_CHARACTERS.contains(c.id))
                     .collect(Collectors.toList());
    }
    return Arrays.asList(CharacterData.CHARACTERS);
}
```

### 2. **캐릭터 상세 정보 팝업** (중요도: 낮음)

캐릭터 카드를 더블 클릭하면 스킬 정보 등을 보여주는 팝업이 있으면 좋습니다.

```java
@Override
public void mouseClicked(MouseEvent e) {
    if (e.getClickCount() == 1) {
        // 선택
        selectedCharacterId = data.id;
        highlightCard();
    } else if (e.getClickCount() == 2) {
        // 더블 클릭: 상세 정보 표시
        showCharacterDetails(data);
    }
}

private void showCharacterDetails(CharacterData data) {
    JDialog detailDialog = new JDialog(this, data.name + " 정보", true);
    detailDialog.setSize(400, 500);
    
    JPanel panel = new JPanel(new BorderLayout());
    
    // 캐릭터 이미지
    JLabel imageLabel = new JLabel(loadCharacterImage(data.id, 200, 200));
    panel.add(imageLabel, BorderLayout.NORTH);
    
    // 스킬 정보
    JTextArea skillInfo = new JTextArea();
    skillInfo.setText("기본 스킬: " + data.basicSkillName + "\n" +
                     "전술 스킬: " + data.tacticalSkillName + "\n" +
                     "궁극기: " + data.ultimateSkillName);
    skillInfo.setEditable(false);
    panel.add(new JScrollPane(skillInfo), BorderLayout.CENTER);
    
    detailDialog.add(panel);
    detailDialog.setLocationRelativeTo(this);
    detailDialog.setVisible(true);
}
```

### 3. **캐릭터 검색/필터** (중요도: 낮음)

캐릭터가 많아지면 검색이나 역할별 필터가 있으면 편리합니다.

```java
// 상단에 검색 필드 추가
JTextField searchField = new JTextField();
searchField.getDocument().addDocumentListener(new DocumentListener() {
    @Override
    public void insertUpdate(DocumentEvent e) { filterCards(); }
    @Override
    public void removeUpdate(DocumentEvent e) { filterCards(); }
    @Override
    public void changedUpdate(DocumentEvent e) { filterCards(); }
});

private void filterCards() {
    String query = searchField.getText().toLowerCase();
    for (Component comp : gridPanel.getComponents()) {
        if (comp instanceof JPanel) {
            JPanel card = (JPanel) comp;
            // 캐릭터 이름/역할에 검색어가 포함되면 표시, 아니면 숨김
            boolean matches = data.name.toLowerCase().contains(query) ||
                            data.role.toLowerCase().contains(query);
            card.setVisible(matches);
        }
    }
    gridPanel.revalidate();
    gridPanel.repaint();
}

// 역할별 필터 버튼
JPanel filterPanel = new JPanel(new FlowLayout());
filterPanel.add(new JLabel("역할:"));
for (String role : new String[]{"전체", "공격", "방어", "지원"}) {
    JButton roleButton = new JButton(role);
    roleButton.addActionListener(e -> filterByRole(role));
    filterPanel.add(roleButton);
}
```

### 4. **이미지 캐싱** (중요도: 낮음)

같은 이미지를 여러 번 로드하지 않도록 캐싱을 추가하면 성능이 향상됩니다.

```java
private static final Map<String, ImageIcon> imageCache = new HashMap<>();

private ImageIcon loadCharacterImage(String characterId, int width, int height) {
    String cacheKey = characterId + "_" + width + "x" + height;
    
    if (imageCache.containsKey(cacheKey)) {
        return imageCache.get(cacheKey);
    }
    
    ImageIcon icon = loadImageFromDisk(characterId, width, height);
    if (icon != null) {
        imageCache.put(cacheKey, icon);
    }
    return icon;
}
```

### 5. **선택 확인 메시지** (중요도: 낮음)

캐릭터 선택 시 간단한 확인 메시지를 표시하면 좋습니다.

```java
confirmButton.addActionListener(e -> {
    if (selectedCharacterId != null) {
        CharacterData data = CharacterData.getById(selectedCharacterId);
        int result = JOptionPane.showConfirmDialog(this,
                "'" + data.name + "' 캐릭터를 선택하시겠습니까?",
                "선택 확인",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            confirmed = true;
            dispose();
        }
    } else {
        JOptionPane.showMessageDialog(this, "캐릭터를 선택해주세요!", "알림", 
                JOptionPane.WARNING_MESSAGE);
    }
});
```

### 6. **랜덤 선택 버튼** (중요도: 낮음)

랜덤으로 캐릭터를 선택하는 버튼이 있으면 재미있습니다.

```java
JButton randomButton = new JButton("랜덤 선택");
randomButton.addActionListener(e -> {
    List<CharacterData> availableChars = Arrays.stream(CharacterData.CHARACTERS)
            .filter(c -> AVAILABLE_CHARACTERS.contains(c.id))
            .filter(c -> !disabledCharacters.contains(c.id))
            .collect(Collectors.toList());
    
    if (!availableChars.isEmpty()) {
        Random random = new Random();
        CharacterData randomChar = availableChars.get(random.nextInt(availableChars.size()));
        selectedCharacterId = randomChar.id;
        highlightCard(randomChar.id);
    }
});
```

### 7. **애니메이션 효과** (중요도: 매우 낮음)

카드 선택 시 부드러운 애니메이션이 있으면 더 세련되어 보입니다.

```java
private void animateCardSelection(JPanel card) {
    // 선택 시 카드가 약간 커졌다가 원래 크기로 돌아오는 효과
    Timer timer = new Timer(10, null);
    final int[] step = {0};
    final Dimension originalSize = card.getSize();
    
    timer.addActionListener(e -> {
        if (step[0] < 10) {
            int scale = 2; // 최대 2px 증가
            int offset = (int) (scale * Math.sin(Math.PI * step[0] / 10));
            card.setPreferredSize(new Dimension(
                originalSize.width + offset,
                originalSize.height + offset
            ));
            card.revalidate();
            step[0]++;
        } else {
            card.setPreferredSize(originalSize);
            card.revalidate();
            timer.stop();
        }
    });
    
    timer.start();
}
```

---

## 코드 품질 평가

| 항목 | 점수 | 설명 |
|------|------|------|
| **가독성** | ⭐⭐⭐⭐⭐ (5.0/5.0) | 메서드명 명확, 주석 충분, 적절한 길이 (454줄) |
| **유지보수성** | ⭐⭐⭐⭐⭐ (5.0/5.0) | 정적 팩토리 메서드, Null 안전, 명확한 책임 분리 |
| **확장성** | ⭐⭐⭐⭐☆ (4.0/5.0) | `AVAILABLE_CHARACTERS` Set으로 쉽게 활성 캐릭터 추가 가능 |
| **에러 처리** | ⭐⭐⭐⭐⭐ (5.0/5.0) | 이미지 로드 실패 시 대체 UI 표시, Null 안전 |
| **성능** | ⭐⭐⭐⭐☆ (4.0/5.0) | 이미지 로드는 양호하나 캐싱 추가하면 더 좋음 |
| **일관성** | ⭐⭐⭐⭐⭐ (5.0/5.0) | Material Design 스타일 일관 적용 |

### **종합 평가**: ⭐⭐⭐⭐⭐ (4.7/5.0)

---

## 결론 및 요약

`CharacterSelectDialog`는 **매우 잘 설계된 캐릭터 선택 다이얼로그**로, 중복 방지, 유연한 이미지 로드, 정적 팩토리 메서드 등 우수한 기능을 제공합니다.

**주요 성과**:
- **중복 방지**: 같은 팀 내 캐릭터 중복 선택 불가, 선택자 표시
- **유연한 이미지 로드**: 리소스 → 파일 시스템, 3가지 확장자, 대체 UI
- **정적 팩토리 메서드**: 4가지 오버로딩으로 다양한 사용 시나리오 지원
- **타임아웃 지원**: 선택 시간 제한 기능
- **Material Design**: 다크 테마, hover 효과, 스탯 색상 코딩
- **미구현 캐릭터 처리**: "추후 업데이트 예정" 라벨로 명확히 표시

**개선 영역**:
1. 캐릭터 필터링 (활성 캐릭터만 표시)
2. 이미지 캐싱 (성능 향상)
3. 캐릭터 상세 정보 팝업 (선택 사항)
4. 랜덤 선택 버튼 (재미 요소)

전반적으로 **거의 완벽에 가까운 코드**이며, 이미지 캐싱과 필터링 기능만 추가하면 더욱 강력한 다이얼로그가 될 것입니다.
