package com.fpsgame.client;

import java.awt.event.*;
import java.util.function.Consumer;

/**
 * InputController - 키보드/마우스 입력 처리 전담 클래스
 * GamePanel에서 분리하여 모든 입력 이벤트를 관리
 */
public class InputController {
    
    private static final int TILE_SIZE = 32;
    
    // 키 상태 배열
    private final boolean[] keys = new boolean[256];
    
    // 마우스 위치
    private int mouseX = 400;
    private int mouseY = 300;
    
    // 편집 모드 상태
    private boolean editMode = false;
    private int paintState = -1; // -1=없음, 0=unwalkable로 칠하기, 1=walkable로 칠하기
    private int editPaintMode = 0; // 0=이동가능, 1=이동불가, 2=RED스폰, 3=BLUE스폰
    private int hoverCol = -1;
    private int hoverRow = -1;
    
    // 디버그 모드
    private boolean debugObstacles = false;
    
    // 미니맵 토글
    private boolean showMinimap = true;
    
    // 콜백 인터페이스들
    private Runnable onTacticalSkill;
    private Runnable onUltimateSkill;
    private Runnable onCharacterSelect;
    private Runnable onSaveMap;
    private Runnable onSwitchMap;
    private Runnable onFocusChat;
    private Consumer<int[]> onBasicAttack; // targetX, targetY (맵 좌표)
    private Consumer<int[]> onMinimapClick; // targetX, targetY (맵 좌표)
    private Consumer<int[]> onTilePaintStart; // mapX, mapY
    private Consumer<int[]> onTilePaintContinue; // mapX, mapY
    private Runnable onTilePaintEnd;
    private Consumer<int[]> onMouseMove; // x, y
    private Consumer<int[]> onHoverTileUpdate; // mapX, mapY
    private Consumer<Integer> onEditPaintModeChange; // mode
    
    public InputController() {
    }
    
    // Getters
    public boolean[] getKeys() { return keys; }
    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }
    public boolean isEditMode() { return editMode; }
    public int getPaintState() { return paintState; }
    public int getEditPaintMode() { return editPaintMode; }
    public int getHoverCol() { return hoverCol; }
    public int getHoverRow() { return hoverRow; }
    public boolean isDebugObstacles() { return debugObstacles; }
    public boolean isShowMinimap() { return showMinimap; }
    
    // Setters
    public void setEditMode(boolean mode) { this.editMode = mode; }
    public void setPaintState(int state) { this.paintState = state; }
    public void setEditPaintMode(int mode) { this.editPaintMode = mode; }
    public void setHoverTile(int col, int row) { 
        this.hoverCol = col; 
        this.hoverRow = row; 
    }
    public void setDebugObstacles(boolean debug) { this.debugObstacles = debug; }
    public void setShowMinimap(boolean show) { this.showMinimap = show; }
    
    // 콜백 등록
    public void setOnTacticalSkill(Runnable callback) { this.onTacticalSkill = callback; }
    public void setOnUltimateSkill(Runnable callback) { this.onUltimateSkill = callback; }
    public void setOnCharacterSelect(Runnable callback) { this.onCharacterSelect = callback; }
    public void setOnSaveMap(Runnable callback) { this.onSaveMap = callback; }
    public void setOnSwitchMap(Runnable callback) { this.onSwitchMap = callback; }
    public void setOnFocusChat(Runnable callback) { this.onFocusChat = callback; }
    public void setOnBasicAttack(Consumer<int[]> callback) { this.onBasicAttack = callback; }
    public void setOnMinimapClick(Consumer<int[]> callback) { this.onMinimapClick = callback; }
    public void setOnTilePaintStart(Consumer<int[]> callback) { this.onTilePaintStart = callback; }
    public void setOnTilePaintContinue(Consumer<int[]> callback) { this.onTilePaintContinue = callback; }
    public void setOnTilePaintEnd(Runnable callback) { this.onTilePaintEnd = callback; }
    public void setOnMouseMove(Consumer<int[]> callback) { this.onMouseMove = callback; }
    public void setOnHoverTileUpdate(Consumer<int[]> callback) { this.onHoverTileUpdate = callback; }
    public void setOnEditPaintModeChange(Consumer<Integer> callback) { this.onEditPaintModeChange = callback; }
    
    /**
     * KeyListener 구현을 위한 어댑터 생성
     */
    public KeyListener createKeyListener(Consumer<String> chatMessageHandler) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                InputController.this.handleKeyPressed(e, chatMessageHandler);
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                InputController.this.handleKeyReleased(e);
            }
        };
    }
    
    /**
     * MouseListener 구현을 위한 어댑터 생성
     */
    public MouseListener createMouseListener(int canvasWidth, int canvasHeight,
                                            boolean awaitingMinimapTarget,
                                            int cameraX, int cameraY,
                                            int mapWidth, int mapHeight) {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                InputController.this.handleMousePressed(e, canvasWidth, canvasHeight,
                        awaitingMinimapTarget, cameraX, cameraY, mapWidth, mapHeight);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                InputController.this.handleMouseReleased(e);
            }
        };
    }
    
    /**
     * MouseMotionListener 구현을 위한 어댑터 생성
     */
    public MouseMotionListener createMouseMotionListener(int cameraX, int cameraY) {
        return new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                InputController.this.handleMouseMoved(e, cameraX, cameraY);
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                InputController.this.handleMouseDragged(e, cameraX, cameraY);
            }
        };
    }
    
    // ========== 내부 핸들러 메서드 ==========
    
    private void handleKeyPressed(KeyEvent e, Consumer<String> chatMessageHandler) {
        keys[e.getKeyCode()] = true;
        int keyCode = e.getKeyCode();
        
        // 편집 모드 저장 단축키 (Ctrl+S)
        if (editMode && keyCode == KeyEvent.VK_S && (e.isControlDown() || e.isMetaDown())) {
            if (onSaveMap != null) onSaveMap.run();
            return;
        }
        
        // 사용자 설정 키 바인딩 체크
        if (KeyBindingConfig.isKeyPressed(keyCode, KeyBindingConfig.KEY_TACTICAL_SKILL)) {
            if (onTacticalSkill != null) onTacticalSkill.run();
        } else if (KeyBindingConfig.isKeyPressed(keyCode, KeyBindingConfig.KEY_ULTIMATE_SKILL)) {
            if (onUltimateSkill != null) onUltimateSkill.run();
        } else if (KeyBindingConfig.isKeyPressed(keyCode, KeyBindingConfig.KEY_CHARACTER_SELECT)) {
            if (onCharacterSelect != null) onCharacterSelect.run();
        } else if (KeyBindingConfig.isKeyPressed(keyCode, KeyBindingConfig.KEY_MINIMAP_TOGGLE)) {
            showMinimap = !showMinimap;
            if (chatMessageHandler != null) {
                chatMessageHandler.accept("[시스템] 미니맵 " + (showMinimap ? "켜짐" : "꺼짐"));
            }
        }
        
        // 고정 키 (디버그 및 에디터 기능)
        switch (keyCode) {
            case KeyEvent.VK_F3 -> { // 장애물 디버그 토글
                debugObstacles = !debugObstacles;
                if (chatMessageHandler != null) {
                    chatMessageHandler.accept("[디버그] 장애물 표시 " + (debugObstacles ? "ON" : "OFF"));
                }
            }
            case KeyEvent.VK_F4 -> { // 편집 모드 토글
                editMode = !editMode;
                paintState = -1;
                if (chatMessageHandler != null) {
                    chatMessageHandler.accept(editMode ? "[에디터] 타일 편집 모드 ON" : "[에디터] 타일 편집 모드 OFF");
                }
            }
            case KeyEvent.VK_1 -> {
                if (editMode) {
                    editPaintMode = 0;
                    if (onEditPaintModeChange != null) onEditPaintModeChange.accept(0);
                    if (chatMessageHandler != null) {
                        chatMessageHandler.accept("[에디터] 모드: 이동 가능 칠하기");
                    }
                }
            }
            case KeyEvent.VK_2 -> {
                if (editMode) {
                    editPaintMode = 1;
                    if (onEditPaintModeChange != null) onEditPaintModeChange.accept(1);
                    if (chatMessageHandler != null) {
                        chatMessageHandler.accept("[에디터] 모드: 이동 불가(벽) 칠하기");
                    }
                }
            }
            case KeyEvent.VK_3 -> {
                if (editMode) {
                    editPaintMode = 2;
                    if (onEditPaintModeChange != null) onEditPaintModeChange.accept(2);
                    if (chatMessageHandler != null) {
                        chatMessageHandler.accept("[에디터] 모드: RED 스폰 토글");
                    }
                }
            }
            case KeyEvent.VK_4 -> {
                if (editMode) {
                    editPaintMode = 3;
                    if (onEditPaintModeChange != null) onEditPaintModeChange.accept(3);
                    if (chatMessageHandler != null) {
                        chatMessageHandler.accept("[에디터] 모드: BLUE 스폰 토글");
                    }
                }
            }
            case KeyEvent.VK_F6 -> { // 맵 전환
                if (onSwitchMap != null) onSwitchMap.run();
            }
            case KeyEvent.VK_F5 -> { // 수동 저장 키
                if (onSaveMap != null) onSaveMap.run();
            }
            case KeyEvent.VK_T, KeyEvent.VK_ENTER -> {
                if (onFocusChat != null) onFocusChat.run();
            }
        }
    }
    
    private void handleKeyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }
    
    private void handleMousePressed(MouseEvent e, int canvasWidth, int canvasHeight,
                                   boolean awaitingMinimapTarget,
                                   int cameraX, int cameraY,
                                   int mapWidth, int mapHeight) {
        // 미니맵 타겟팅 모드: General 에어스트라이크
        if (awaitingMinimapTarget && e.getButton() == MouseEvent.BUTTON1) {
            int minimapWidth = 200;
            int minimapHeight = 150;
            int minimapX = canvasWidth - minimapWidth - 20;
            int minimapY = 20;
            
            if (e.getX() >= minimapX && e.getX() <= minimapX + minimapWidth &&
                    e.getY() >= minimapY && e.getY() <= minimapY + minimapHeight) {
                // 미니맵 좌표를 맵 좌표로 변환
                float scaleX = (float) minimapWidth / mapWidth;
                float scaleY = (float) minimapHeight / mapHeight;
                int targetMapX = (int) ((e.getX() - minimapX) / scaleX);
                int targetMapY = (int) ((e.getY() - minimapY) / scaleY);
                
                if (onMinimapClick != null) {
                    onMinimapClick.accept(new int[] { targetMapX, targetMapY });
                }
                return;
            }
        }
        
        // 편집 모드: 타일 페인팅
        if (editMode) {
            int mapX = e.getX() + cameraX;
            int mapY = e.getY() + cameraY;
            if (onTilePaintStart != null) {
                onTilePaintStart.accept(new int[] { mapX, mapY });
            }
            return;
        }
        
        // 게임 모드: 좌클릭 공격
        if (e.getButton() == MouseEvent.BUTTON1) {
            int targetMapX = e.getX() + cameraX;
            int targetMapY = e.getY() + cameraY;
            if (onBasicAttack != null) {
                onBasicAttack.accept(new int[] { targetMapX, targetMapY });
            }
        }
    }
    
    private void handleMouseReleased(MouseEvent e) {
        if (editMode) {
            paintState = -1;
            if (onTilePaintEnd != null) onTilePaintEnd.run();
        }
    }
    
    private void handleMouseMoved(MouseEvent e, int cameraX, int cameraY) {
        mouseX = e.getX();
        mouseY = e.getY();
        
        if (onMouseMove != null) {
            onMouseMove.accept(new int[] { mouseX, mouseY });
        }
        
        if (editMode) {
            int mapX = mouseX + cameraX;
            int mapY = mouseY + cameraY;
            updateHoverTile(mapX, mapY);
        }
    }
    
    private void handleMouseDragged(MouseEvent e, int cameraX, int cameraY) {
        mouseX = e.getX();
        mouseY = e.getY();
        
        if (onMouseMove != null) {
            onMouseMove.accept(new int[] { mouseX, mouseY });
        }
        
        if (editMode) {
            int mapX = mouseX + cameraX;
            int mapY = mouseY + cameraY;
            updateHoverTile(mapX, mapY);
            if (onTilePaintContinue != null) {
                onTilePaintContinue.accept(new int[] { mapX, mapY });
            }
        }
    }
    
    private void updateHoverTile(int mapX, int mapY) {
        if (onHoverTileUpdate != null) {
            onHoverTileUpdate.accept(new int[] { mapX, mapY });
        }
    }
    
    /**
     * 이동 입력 처리 (WASD 키 체크)
     */
    public int[] getMovementInput() {
        int dx = 0;
        int dy = 0;
        
        if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP]) dy -= 1;
        if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN]) dy += 1;
        if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT]) dx -= 1;
        if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT]) dx += 1;
        
        return new int[] { dx, dy };
    }
    
    /**
     * 특정 키가 눌려있는지 확인
     */
    public boolean isKeyPressed(int keyCode) {
        if (keyCode < 0 || keyCode >= keys.length) return false;
        return keys[keyCode];
    }
}
