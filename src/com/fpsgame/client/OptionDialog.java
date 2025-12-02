package com.fpsgame.client;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;

/**
 * 옵션 설정 다이얼로그
 * 
 * 게임의 다양한 설정을 변경할 수 있는 창입니다.
 * - 일반 설정: 사운드 볼륨, 마우스 감도
 * - 키 설정: 이동, 스킬 등의 키 바인딩
 */
public class OptionDialog extends JDialog {

    /** 사운드 볼륨 슬라이더 (0-100) */
    private JSlider soundSlider;
    
    /** 마우스 감도 슬라이더 (1-20) */
    private JSlider mouseSlider;
    
    /** 키 바인딩 버튼 맵 (액션 이름 -> 버튼) */
    private final Map<String, JButton> keyButtons = new HashMap<>();

    /**
     * 옵션 다이얼로그 생성자
     * 
     * @param owner 부모 프레임
     */
    public OptionDialog(Frame owner) {
        super(owner, "게임 설정", true);
        setLayout(new BorderLayout());
        setSize(600, 500);
        setLocationRelativeTo(owner);

        initUI();
    }

    /**
     * UI 초기화
     * 
     * 탭 패널을 생성하고 각 설정 탭을 추가합니다.
     */
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
        JPanel generalPanel = new JPanel(new GridBagLayout());
        generalPanel.setBackground(panelBg);
        generalPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 소리 크기
        JLabel lblSound = new JLabel("사운드 볼륨:");
        lblSound.setForeground(Color.WHITE);
        lblSound.setFont(koreanFont);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        generalPanel.add(lblSound, gbc);

        soundSlider = new JSlider(0, 100, 50);
        soundSlider.setBackground(panelBg);
        soundSlider.setForeground(Color.WHITE);
        soundSlider.setMajorTickSpacing(25);
        soundSlider.setMinorTickSpacing(5);
        soundSlider.setPaintTicks(true);
        soundSlider.setPaintLabels(true);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.7;
        generalPanel.add(soundSlider, gbc);

        // 마우스 감도
        JLabel lblMouse = new JLabel("마우스 감도:");
        lblMouse.setForeground(Color.WHITE);
        lblMouse.setFont(koreanFont);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        generalPanel.add(lblMouse, gbc);

        mouseSlider = new JSlider(1, 20, 10);
        mouseSlider.setBackground(panelBg);
        mouseSlider.setForeground(Color.WHITE);
        mouseSlider.setMajorTickSpacing(5);
        mouseSlider.setMinorTickSpacing(1);
        mouseSlider.setPaintTicks(true);
        mouseSlider.setPaintLabels(true);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.7;
        generalPanel.add(mouseSlider, gbc);

        tabbedPane.addTab("일반", generalPanel);

        // 2. 키 설정 탭
        JPanel keyPanel = new JPanel(new GridBagLayout());
        keyPanel.setBackground(panelBg);
        keyPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

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

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i < keyActions.length; i++) {
            String action = keyActions[i];
            String displayName = keyDisplayNames[i];

            JLabel label = new JLabel(displayName + ":");
            label.setForeground(Color.WHITE);
            label.setFont(koreanFont);
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0.6;
            keyPanel.add(label, gbc);

            int currentKey = KeyBindingConfig.getKey(action);
            JButton keyButton = new JButton(KeyEvent.getKeyText(currentKey));
            keyButton.setFont(koreanFont);
            keyButton.setBackground(new Color(88, 101, 242));
            keyButton.setForeground(Color.BLACK); // 검정 텍스트
            keyButton.setFocusPainted(false);
            keyButton.addActionListener(e -> captureKey(action, keyButton));

            keyButtons.put(action, keyButton);

            gbc.gridx = 1;
            gbc.gridy = i;
            gbc.weightx = 0.4;
            keyPanel.add(keyButton, gbc);
        }

        // 기본값 복원 버튼
        JButton resetButton = new JButton("기본값 복원");
        resetButton.setFont(koreanBold);
        resetButton.setBackground(new Color(244, 67, 54));
        resetButton.setForeground(Color.BLACK); // 검정 텍스트
        resetButton.setFocusPainted(false);
        resetButton.addActionListener(e -> resetToDefaults());
        gbc.gridx = 0;
        gbc.gridy = keyActions.length;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 5, 10);
        keyPanel.add(resetButton, gbc);

        tabbedPane.addTab("키 설정", keyPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // 버튼
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBackground(bgDark);

        // 저장
        JButton btnSave = new JButton("저장");
        btnSave.setFont(koreanBold);
        btnSave.setBackground(new Color(67, 181, 129));
        btnSave.setForeground(Color.BLACK); // 검정 텍스트
        btnSave.setFocusPainted(false);
        btnSave.setPreferredSize(new Dimension(100, 35));
        btnSave.addActionListener(e -> {
            saveSettings();
            dispose();
        });

        // 취소
        JButton btnCancel = new JButton("취소");
        btnCancel.setFont(koreanBold);
        btnCancel.setBackground(new Color(100, 100, 100));
        btnCancel.setForeground(Color.BLACK); // 검정 텍스트
        btnCancel.setFocusPainted(false);
        btnCancel.setPreferredSize(new Dimension(100, 35));
        btnCancel.addActionListener(e -> dispose());

        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        add(btnPanel, BorderLayout.SOUTH);
    }

    // 키 입력 캡처
    private void captureKey(String actionName, JButton button) {
        button.setText("키를 누르세요...");
        button.setBackground(new Color(255, 152, 0));

        // KeyListener 등록
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (keyCode != KeyEvent.VK_ESCAPE) {
                    KeyBindingConfig.setKey(actionName, keyCode);
                    button.setText(KeyEvent.getKeyText(keyCode));
                }
                button.setBackground(new Color(88, 101, 242));
                button.removeKeyListener(this);
            }
        };

        button.addKeyListener(keyListener);
        button.requestFocusInWindow();
    }

    // 기본값 복원
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

    // 설정 저장
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
}
