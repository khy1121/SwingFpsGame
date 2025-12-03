package com.fpsgame.client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;

/**
 * 채팅 UI, 메뉴바를 담당하는 클래스
 * GamePanel에서 분리된 UIManager
 */
public class UIManager {
    
    private JTextArea chatArea;
    private JTextField chatInput;
    private JScrollPane chatScroll;
    
    // 채팅 스로틀링
    private String lastChatMessage = null;
    private long lastChatTime = 0L;
    private static final long CHAT_THROTTLE_MS = 1000;
    
    // 콜백 인터페이스
    private final ChatSendCallback chatSendCallback;
    private final MenuActionCallback menuActionCallback;
    
    @FunctionalInterface
    public interface ChatSendCallback {
        void sendChat(String message);
    }
    
    @FunctionalInterface
    public interface MenuActionCallback {
        void onMenuAction(String action);
    }
    
    public UIManager(ChatSendCallback chatSendCallback, MenuActionCallback menuActionCallback) {
        this.chatSendCallback = chatSendCallback;
        this.menuActionCallback = menuActionCallback;
    }
    
    // ==================== Getters ====================
    
    public JTextArea getChatArea() { return chatArea; }
    public JTextField getChatInput() { return chatInput; }
    public JScrollPane getChatScroll() { return chatScroll; }
    
    // ==================== 채팅 UI 초기화 ====================
    
    /**
     * 채팅 패널 생성
     */
    public JPanel createChatPanel(int width, int height) {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setPreferredSize(new Dimension(width, height));
        chatPanel.setBackground(new Color(32, 34, 37));
        
        // 채팅 영역
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(new Color(32, 34, 37));
        chatArea.setForeground(Color.WHITE);
        chatArea.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        
        chatScroll = new JScrollPane(chatArea);
        chatScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScroll.setBorder(BorderFactory.createLineBorder(new Color(50, 52, 55)));
        
        // 입력 필드
        chatInput = new JTextField();
        chatInput.setBackground(new Color(40, 42, 45));
        chatInput.setForeground(Color.WHITE);
        chatInput.setCaretColor(Color.WHITE);
        chatInput.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 62, 65)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        chatInput.addActionListener(e -> {
            String text = chatInput.getText().trim();
            if (!text.isEmpty()) {
                if (chatSendCallback != null) {
                    chatSendCallback.sendChat(text);
                }
                chatInput.setText("");
            }
        });
        
        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(chatInput, BorderLayout.SOUTH);
        
        return chatPanel;
    }
    
    /**
     * 채팅 메시지 추가 (스로틀링 적용)
     */
    public void appendChatMessage(String message) {
        long now = System.currentTimeMillis();
        if (message != null && message.equals(lastChatMessage) && (now - lastChatTime) < CHAT_THROTTLE_MS) {
            return;
        }
        lastChatMessage = message;
        lastChatTime = now;
        
        SwingUtilities.invokeLater(() -> {
            if (chatArea != null) {
                chatArea.append(message + "\n");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
        });
    }
    
    // ==================== 메뉴바 생성 ====================
    
    /**
     * 메뉴바 생성
     */
    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // 게임 메뉴
        JMenu gameMenu = new JMenu("게임");
        
        JMenuItem characterSelectItem = new JMenuItem("캐릭터 선택");
        characterSelectItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        characterSelectItem.addActionListener(e -> {
            if (menuActionCallback != null) {
                menuActionCallback.onMenuAction("CHARACTER_SELECT");
            }
        });
        
        JMenuItem exitItem = new JMenuItem("종료");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
        exitItem.addActionListener(e -> {
            if (menuActionCallback != null) {
                menuActionCallback.onMenuAction("EXIT");
            }
        });
        
        gameMenu.add(characterSelectItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        
        // 보기 메뉴
        JMenu viewMenu = new JMenu("보기");
        
        JCheckBoxMenuItem minimapItem = new JCheckBoxMenuItem("미니맵 표시", true);
        minimapItem.addActionListener(e -> {
            if (menuActionCallback != null) {
                menuActionCallback.onMenuAction("TOGGLE_MINIMAP");
            }
        });
        
        JCheckBoxMenuItem debugItem = new JCheckBoxMenuItem("장애물 디버그 (F3)", false);
        debugItem.addActionListener(e -> {
            if (menuActionCallback != null) {
                menuActionCallback.onMenuAction("TOGGLE_DEBUG");
            }
        });
        
        JCheckBoxMenuItem editModeItem = new JCheckBoxMenuItem("에디터 모드 (F4)", false);
        editModeItem.addActionListener(e -> {
            if (menuActionCallback != null) {
                menuActionCallback.onMenuAction("TOGGLE_EDIT");
            }
        });
        
        viewMenu.add(minimapItem);
        viewMenu.add(debugItem);
        viewMenu.add(editModeItem);
        
        // 맵 메뉴
        JMenu mapMenu = new JMenu("맵");
        
        JMenuItem nextMapItem = new JMenuItem("다음 맵 (F6)");
        nextMapItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        nextMapItem.addActionListener(e -> {
            if (menuActionCallback != null) {
                menuActionCallback.onMenuAction("NEXT_MAP");
            }
        });
        
        JMenuItem saveMapItem = new JMenuItem("맵 저장 (F5)");
        saveMapItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        saveMapItem.addActionListener(e -> {
            if (menuActionCallback != null) {
                menuActionCallback.onMenuAction("SAVE_MAP");
            }
        });
        
        JMenuItem rebuildObstaclesItem = new JMenuItem("장애물 재구성 (F7)");
        rebuildObstaclesItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
        rebuildObstaclesItem.addActionListener(e -> {
            if (menuActionCallback != null) {
                menuActionCallback.onMenuAction("REBUILD_OBSTACLES");
            }
        });
        
        mapMenu.add(nextMapItem);
        mapMenu.add(saveMapItem);
        mapMenu.add(rebuildObstaclesItem);
        
        // 도움말 메뉴
        JMenu helpMenu = new JMenu("도움말");
        
        JMenuItem controlsItem = new JMenuItem("조작법");
        controlsItem.addActionListener(e -> {
            if (menuActionCallback != null) {
                menuActionCallback.onMenuAction("SHOW_CONTROLS");
            }
        });
        
        JMenuItem aboutItem = new JMenuItem("정보");
        aboutItem.addActionListener(e -> {
            if (menuActionCallback != null) {
                menuActionCallback.onMenuAction("SHOW_ABOUT");
            }
        });
        
        helpMenu.add(controlsItem);
        helpMenu.add(aboutItem);
        
        menuBar.add(gameMenu);
        menuBar.add(viewMenu);
        menuBar.add(mapMenu);
        menuBar.add(helpMenu);
        
        return menuBar;
    }
    
    /**
     * 조작법 다이얼로그 표시
     */
    public void showControlsDialog(JFrame parent) {
        String controls = """
            === 기본 조작 ===
            WASD: 이동
            마우스 클릭: 공격
            E: 전술 스킬
            R: 궁극기
            
            === UI ===
            Enter: 채팅
            Ctrl+C: 캐릭터 선택
            
            === 에디터 (F4) ===
            1: 이동 가능 타일 칠하기
            2: 장애물 칠하기
            3: RED 스폰 설정
            4: BLUE 스폰 설정
            F5: 맵 저장
            F6: 다음 맵
            F7: 장애물 재구성
            """;
        
        JOptionPane.showMessageDialog(parent, controls, "조작법", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * 정보 다이얼로그 표시
     */
    public void showAboutDialog(JFrame parent) {
        String about = """
            FPS Game
            
            버전: 1.0
            제작: Team
            
            2D 멀티플레이어 슈팅 게임
            """;
        
        JOptionPane.showMessageDialog(parent, about, "정보", JOptionPane.INFORMATION_MESSAGE);
    }
}
