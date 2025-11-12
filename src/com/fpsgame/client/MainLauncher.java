package com.fpsgame.client;

import java.awt.*;
import javax.swing.*;

/**
 * 메인 런처 - 기존 디자인 유지
 * 간단한 시작 화면
 */
public class MainLauncher extends JFrame {
    
    private JTextField nameField;
    private JButton startButton;
    private JButton exitButton;
    
    public MainLauncher() {
        super("FPS Game Launcher");
        initUI();
    }
    
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
        
        // 중앙 패널
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(40, 50, 65));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // 이름 입력
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel nameLabel = new JLabel("플레이어 이름:");
        nameLabel.setForeground(Color.BLACK);
        nameLabel.setFont(koreanBold);
        centerPanel.add(nameLabel, gbc);
        
        gbc.gridx = 1;
        nameField = new JTextField(15);
        nameField.setFont(koreanFont);
        centerPanel.add(nameField, gbc);
        
        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        buttonPanel.setBackground(new Color(40, 50, 65));
        
        startButton = new JButton("게임 시작");
        startButton.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        startButton.setPreferredSize(new Dimension(160, 45));
        startButton.setBackground(new Color(76, 175, 80));
        startButton.setForeground(Color.BLACK);
        startButton.setFocusPainted(false);
        startButton.addActionListener(e -> startGame());
        
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
    
    private void startGame() {
        String playerName = nameField.getText().trim();
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
            dispose();
        });
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            MainLauncher launcher = new MainLauncher();
            launcher.setVisible(true);
        });
    }
}
