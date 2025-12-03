package com.fpsgame.client;

import java.awt.*;
import javax.swing.*;

/**
 * 메인 런처 클래스
 * 
 * 게임의 진입점으로 플레이어 이름을 입력받고 로비로 진입합니다.
 * 간단한 UI로 게임 시작 전 초기 화면을 제공합니다.
 */
public class MainLauncher extends JFrame {

    /** 플레이어 이름 입력 필드 */
    private JTextField nameField;
    
    /** 게임 시작 버튼 */
    private JButton startButton;
    
    /** 게임 종료 버튼 */
    private JButton exitButton;

    /**
     * 메인 런처 생성자
     */
    public MainLauncher() {
        super("FPS Game Launcher");
        initUI();
    }

    /**
     * UI 초기화
     * 
     * 타이틀, 이름 입력 필드, 시작/종료 버튼을 배치합니다.
     */
    private void initUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 350);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 한글 폰트 설정
        Font koreanFont = new Font("맑은 고딕", Font.PLAIN, 14);
        Font koreanBold = new Font("맑은 고딕", Font.BOLD, 16);
        Font titleFont = new Font("맑은 고딕", Font.BOLD, 36);

        // 타이틀 패널
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(30, 40, 55));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));
        JLabel titleLabel = new JLabel("FPS 게임");
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);

        // 중앙 패널 - 이름 입력
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(40, 50, 65));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // 이름 라벨
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel nameLabel = new JLabel("플레이어 이름:");
        nameLabel.setForeground(Color.BLACK);
        nameLabel.setFont(koreanBold);
        centerPanel.add(nameLabel, gbc);

        // 이름 입력 필드
        gbc.gridx = 1;
        nameField = new JTextField(15);
        nameField.setFont(koreanFont);
        centerPanel.add(nameField, gbc);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        buttonPanel.setBackground(new Color(40, 50, 65));

        // 시작 버튼
        startButton = new JButton("게임 시작");
        startButton.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        startButton.setPreferredSize(new Dimension(160, 45));
        startButton.setBackground(new Color(76, 175, 80));
        startButton.setForeground(Color.BLACK);
        startButton.setFocusPainted(false);
        startButton.addActionListener(e -> startGame());

        // 종료 버튼
        exitButton = new JButton("종료");
        exitButton.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        exitButton.setPreferredSize(new Dimension(160, 45));
        exitButton.setBackground(new Color(244, 67, 54));
        exitButton.setForeground(Color.BLACK);
        exitButton.setFocusPainted(false);
        exitButton.addActionListener(e -> System.exit(0));

        buttonPanel.add(startButton);
        buttonPanel.add(exitButton);

        // 레이아웃 조립
        add(titlePanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 게임 시작 처리
     * 
     * 플레이어 이름을 검증하고 로비 프레임을 엽니다.
     * 이름이 비어있으면 오류 메시지를 표시합니다.
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

    /**
     * 프로그램 진입점
     * 
     * @param args 커맨드 라인 인자 (사용하지 않음)
     */
    public static void main(String[] args) {
        // 시스템 Look and Feel 적용
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
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
}
