package com.fpsgame.client;

import com.fpsgame.common.CharacterData;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * 캐릭터 선택 다이얼로그
 * B키를 누르면 표시됨
 */
public class CharacterSelectDialog extends JDialog {
    
    private String selectedCharacterId;
    private boolean confirmed = false;
    
    public CharacterSelectDialog(Frame parent) {
        super(parent, "캐릭터 선택", true);
        initUI();
    }
    
    private void initUI() {
        setSize(1000, 700);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        
        // 다크 배경색
        Color bgDark = new Color(32, 34, 37);
        Color cardBg = new Color(47, 49, 54);
        Color hoverBg = new Color(57, 59, 64);
        
        // 한글 폰트
        Font koreanFont = new Font("맑은 고딕", Font.PLAIN, 13);
        Font koreanBold = new Font("맑은 고딕", Font.BOLD, 16);
        Font titleFont = new Font("맑은 고딕", Font.BOLD, 24);
        
        // 타이틀
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(bgDark);
        titlePanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("캐릭터를 선택하세요");
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        
        // 캐릭터 그리드
        JPanel gridPanel = new JPanel(new GridLayout(2, 5, 15, 15));
        gridPanel.setBackground(bgDark);
        gridPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        for (CharacterData charData : CharacterData.CHARACTERS) {
            JPanel charCard = createCharacterCard(charData, cardBg, hoverBg, koreanFont, koreanBold);
            gridPanel.add(charCard);
        }
        
        // 하단 버튼
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        bottomPanel.setBackground(bgDark);
        
        JButton confirmButton = new JButton("선택 확인");
        confirmButton.setFont(koreanBold);
        confirmButton.setPreferredSize(new Dimension(140, 40));
        confirmButton.setBackground(new Color(67, 181, 129));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setFocusPainted(false);
        confirmButton.addActionListener(e -> {
            if (selectedCharacterId != null) {
                confirmed = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "캐릭터를 선택해주세요!", "알림", JOptionPane.WARNING_MESSAGE);
            }
        });
        
        JButton cancelButton = new JButton("취소");
        cancelButton.setFont(koreanBold);
        cancelButton.setPreferredSize(new Dimension(140, 40));
        cancelButton.setBackground(new Color(88, 91, 97));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> dispose());
        
        bottomPanel.add(confirmButton);
        bottomPanel.add(cancelButton);
        
        // 조립
        add(titlePanel, BorderLayout.NORTH);
        add(gridPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        
        getContentPane().setBackground(bgDark);
        
        // ESC키로 닫기
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
    
    private JPanel createCharacterCard(CharacterData data, Color cardBg, Color hoverBg, Font normalFont, Font boldFont) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(cardBg);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(70, 72, 78), 2),
            new EmptyBorder(12, 12, 12, 12)
        ));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
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
        statsPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        
        JLabel hpLabel = new JLabel("HP: " + (int)data.health);
        hpLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        hpLabel.setForeground(new Color(255, 180, 180));
        
        JLabel speedLabel = new JLabel("속도: " + String.format("%.1f", data.speed));
        speedLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        speedLabel.setForeground(new Color(180, 255, 180));
        
        JLabel armorLabel = new JLabel("방어: " + (int)data.armor);
        armorLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        armorLabel.setForeground(new Color(180, 220, 255));
        
        statsPanel.add(hpLabel);
        statsPanel.add(speedLabel);
        statsPanel.add(armorLabel);
        
        card.add(infoPanel, BorderLayout.CENTER);
        card.add(statsPanel, BorderLayout.SOUTH);
        
        // 마우스 이벤트
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(hoverBg);
                card.setBorder(new CompoundBorder(
                    new LineBorder(new Color(100, 150, 255), 2),
                    new EmptyBorder(12, 12, 12, 12)
                ));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!data.id.equals(selectedCharacterId)) {
                    card.setBackground(cardBg);
                    card.setBorder(new CompoundBorder(
                        new LineBorder(new Color(70, 72, 78), 2),
                        new EmptyBorder(12, 12, 12, 12)
                    ));
                }
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedCharacterId = data.id;
                // 모든 카드 초기화 (부모가 패널인지 안전 확인)
                Container parent = card.getParent();
                if (parent instanceof JPanel) {
                    for (Component comp : ((JPanel) parent).getComponents()) {
                        if (comp instanceof JPanel) {
                            comp.setBackground(cardBg);
                            ((JPanel) comp).setBorder(new CompoundBorder(
                                new LineBorder(new Color(70, 72, 78), 2),
                                new EmptyBorder(12, 12, 12, 12)
                            ));
                        }
                    }
                }
                // 선택된 카드 하이라이트
                card.setBackground(new Color(67, 69, 94));
                card.setBorder(new CompoundBorder(
                    new LineBorder(new Color(100, 200, 255), 3),
                    new EmptyBorder(12, 12, 12, 12)
                ));
            }
        };
        
        card.addMouseListener(mouseHandler);
        
        return card;
    }
    
    public String getSelectedCharacterId() {
        return selectedCharacterId;
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public static String showDialog(Frame parent) {
        CharacterSelectDialog dialog = new CharacterSelectDialog(parent);
        dialog.setVisible(true);
        return dialog.isConfirmed() ? dialog.getSelectedCharacterId() : null;
    }
}
