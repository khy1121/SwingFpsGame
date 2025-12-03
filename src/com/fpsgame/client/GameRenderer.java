package com.fpsgame.client;

import com.fpsgame.client.effects.SkillEffectManager;
import com.fpsgame.common.Ability;
import com.fpsgame.common.CharacterData;
import com.fpsgame.common.GameConstants;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.swing.JPanel;

/**
 * GameRenderer - 게임 렌더링 전담 클래스
 * GamePanel에서 분리하여 모든 그리기 로직을 관리
 */
public class GameRenderer {
    
    private static final int TILE_SIZE = 32;
    private static final int VISION_RANGE = (int) (Math.sqrt(
            GameConstants.GAME_WIDTH * GameConstants.GAME_WIDTH +
                    GameConstants.GAME_HEIGHT * GameConstants.GAME_HEIGHT)
            / 2);
    private static final float PIPER_MARK_RANGE_FACTOR = 1.7f;
    private static final int PIPER_THERMAL_DOT_SIZE = 10;
    
    // 렌더링에 필요한 참조
    private final JPanel canvas;
    private BufferedImage mapImage;
    private SkillEffectManager skillEffects;
    
    // 마우스 위치
    private int mouseX = 400;
    private int mouseY = 300;
    
    // 디버그/편집 모드
    private boolean debugObstacles = false;
    private boolean editMode = false;
    private int hoverCol = -1;
    private int hoverRow = -1;
    private int editPaintMode = 0;
    
    // 미니맵 표시 여부
    private boolean showMinimap = true;
    
    public GameRenderer(JPanel canvas) {
        this.canvas = canvas;
        this.skillEffects = new SkillEffectManager();
    }
    
    // Setters
    public void setMapImage(BufferedImage image) { this.mapImage = image; }
    public void setSkillEffects(SkillEffectManager effects) { this.skillEffects = effects; }
    public void setMousePosition(int x, int y) { this.mouseX = x; this.mouseY = y; }
    public void setDebugObstacles(boolean debug) { this.debugObstacles = debug; }
    public void setEditMode(boolean mode) { this.editMode = mode; }
    public void setHoverTile(int col, int row) { this.hoverCol = col; this.hoverRow = row; }
    public void setEditPaintMode(int mode) { this.editPaintMode = mode; }
    public void setShowMinimap(boolean show) { this.showMinimap = show; }
    
    /**
     * 메인 렌더링 메서드
     */
    public void render(Graphics2D g2d, GameState gameState, 
                      String playerName, int team,
                      SpriteAnimation[] myAnimations,
                      CharacterData currentCharacterData,
                      Ability[] abilities,
                      int kills, int deaths,
                      String selectedCharacter,
                      float piperMarkRemaining,
                      float piperThermalRemaining,
                      GameState.RoundState roundState,
                      int roundCount,
                      int redWins,
                      int blueWins,
                      long roundStartTime,
                      String centerMessage,
                      long centerMessageEndTime) {
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int playerX = gameState.getPlayerX();
        int playerY = gameState.getPlayerY();
        int cameraX = gameState.getCameraX();
        int cameraY = gameState.getCameraY();
        int myHP = gameState.getMyHP();
        int myMaxHP = gameState.getMyMaxHP();
        int myDirection = gameState.getMyDirection();
        
        // 1. 맵 배경
        if (mapImage != null) {
            g2d.drawImage(mapImage, -cameraX, -cameraY, gameState.getMapWidth(), gameState.getMapHeight(), null);
        } else {
            drawGrid(g2d, cameraX, cameraY);
        }
        
        // 2. 장애물 디버그
        drawObstacles(g2d, gameState.getObstacles(), cameraX, cameraY);
        
        // 2.1 에어스트라이크 마커
        drawStrikeMarkersMain(g2d, gameState.getStrikeMarkers(), cameraX, cameraY);
        
        // 2.5 편집 모드 오버레이
        if (editMode) {
            drawEditorOverlay(g2d, gameState.getWalkableGrid(), gameState.getGridCols(), gameState.getGridRows(),
                    cameraX, cameraY, gameState.getRedSpawnTiles(), gameState.getBlueSpawnTiles());
        }
        
        // 3. 다른 플레이어들
        for (Map.Entry<String, GameState.PlayerData> entry : gameState.getPlayers().entrySet()) {
            GameState.PlayerData p = entry.getValue();
            int screenX = p.x - cameraX;
            int screenY = p.y - cameraY;
            
            if (isOnScreen(screenX, screenY)) {
                Color playerColor = p.team == GameConstants.TEAM_RED ? 
                        new Color(244, 67, 54) : new Color(33, 150, 243);
                
                // 스프라이트 그리기
                if (p.animations != null && p.direction < p.animations.length && p.animations[p.direction] != null) {
                    p.animations[p.direction].draw(g2d, screenX - 20, screenY - 20, 40, 40);
                } else {
                    g2d.setColor(playerColor);
                    g2d.fillOval(screenX - 20, screenY - 20, 48, 64);
                }
                
                // HP 바
                drawHealthBar(g2d, screenX, screenY + 25, p.hp, p.maxHp);
            }
        }
        
        // 4. 로컬 플레이어
        int myScreenX = playerX - cameraX;
        int myScreenY = playerY - cameraY;
        Color myColor = team == GameConstants.TEAM_RED ? 
                new Color(255, 100, 100) : new Color(100, 150, 255);
        
        if (myAnimations != null && myDirection < myAnimations.length && myAnimations[myDirection] != null) {
            myAnimations[myDirection].draw(g2d, myScreenX - 20, myScreenY - 20, 40, 40);
        } else {
            g2d.setColor(myColor);
            g2d.fillOval(myScreenX - 20, myScreenY - 20, 40, 40);
        }
        
        // 내 이펙트
        drawMyEffects(g2d, gameState.getMyEffects(), myScreenX, myScreenY);
        if (skillEffects != null) {
            skillEffects.drawSelf(g2d, myScreenX, myScreenY);
        }
        
        // 내 이름
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int nameWidth = fm.stringWidth(playerName + " (You)");
        g2d.drawString(playerName + " (You)", myScreenX - nameWidth / 2, myScreenY - 25);
        
        // 내 HP 바
        drawHealthBar(g2d, myScreenX, myScreenY + 25, myHP, myMaxHP);
        
        // 조준선
        drawAimLine(g2d, myScreenX, myScreenY);
        
        // 5. 미사일
        g2d.setColor(Color.YELLOW);
        for (GameState.Missile m : gameState.getMissiles()) {
            int mScreenX = m.x - cameraX;
            int mScreenY = m.y - cameraY;
            if (isOnScreen(mScreenX, mScreenY)) {
                g2d.fillOval(mScreenX - 4, mScreenY - 4, 8, 8);
            }
        }
        
        // 6. 설치된 오브젝트
        for (Map.Entry<Integer, GameState.PlacedObjectClient> entry : gameState.getPlacedObjects().entrySet()) {
            GameState.PlacedObjectClient obj = entry.getValue();
            int objScreenX = obj.x - cameraX;
            int objScreenY = obj.y - cameraY;
            
            if (isOnScreen(objScreenX, objScreenY)) {
                drawPlacedObject(g2d, obj, objScreenX, objScreenY, team);
            }
        }
        
        // UI 요소
        if (showMinimap) {
            drawMinimap(g2d, gameState, playerX, playerY, team);
        }
        
        drawHUD(g2d, playerName, team, currentCharacterData, myHP, myMaxHP, kills, deaths);
        drawSkillHUD(g2d, abilities, selectedCharacter, piperMarkRemaining, piperThermalRemaining);
        drawRoundInfo(g2d, roundState, roundCount, redWins, blueWins, roundStartTime, 
                centerMessage, centerMessageEndTime);
    }
    
    private boolean isOnScreen(int screenX, int screenY) {
        return screenX >= -50 && screenX <= GameConstants.GAME_WIDTH + 50 &&
                screenY >= -50 && screenY <= GameConstants.GAME_HEIGHT + 50;
    }
    
    private void drawGrid(Graphics2D g, int cameraX, int cameraY) {
        g.setColor(new Color(30, 35, 45));
        for (int x = 0; x < canvas.getWidth(); x += 50) {
            g.drawLine(x, 0, x, canvas.getHeight());
        }
        for (int y = 0; y < canvas.getHeight(); y += 50) {
            g.drawLine(0, y, canvas.getWidth(), y);
        }
    }
    
    private void drawObstacles(Graphics2D g2d, List<Rectangle> obstacles, int cameraX, int cameraY) {
        if (!debugObstacles) return;
        
        g2d.setColor(new Color(255, 0, 0, 100));
        for (Rectangle obs : obstacles) {
            int screenX = obs.x - cameraX;
            int screenY = obs.y - cameraY;
            g2d.fillRect(screenX, screenY, obs.width, obs.height);
            g2d.setColor(new Color(255, 255, 0, 150));
            g2d.drawRect(screenX, screenY, obs.width, obs.height);
            g2d.setColor(new Color(255, 0, 0, 100));
        }
    }
    
    private void drawStrikeMarkersMain(Graphics2D g2d, Map<Integer, GameState.StrikeMarker> strikeMarkers,
                                      int cameraX, int cameraY) {
        if (strikeMarkers.isEmpty()) return;
        
        for (GameState.StrikeMarker marker : strikeMarkers.values()) {
            int screenX = marker.x - cameraX;
            int screenY = marker.y - cameraY;
            
            if (isOnScreen(screenX, screenY)) {
                int radius = 120;
                long currentTime = System.currentTimeMillis();
                float pulsePhase = (currentTime % 500) / 500f;
                int alpha = (int) (100 + 50 * Math.sin(pulsePhase * Math.PI * 2));
                
                g2d.setColor(new Color(255, 0, 0, alpha));
                g2d.fillOval(screenX - radius, screenY - radius, radius * 2, radius * 2);
                
                g2d.setColor(new Color(255, 0, 0, 200));
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawOval(screenX - radius, screenY - radius, radius * 2, radius * 2);
                
                g2d.setColor(Color.YELLOW);
                g2d.drawLine(screenX - 20, screenY, screenX + 20, screenY);
                g2d.drawLine(screenX, screenY - 20, screenX, screenY + 20);
                
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                String warning = "WARNING!";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(warning, screenX - fm.stringWidth(warning) / 2, screenY - 10);
            }
        }
    }
    
    private void drawAimLine(Graphics2D g2d, int myScreenX, int myScreenY) {
        int vx = mouseX - myScreenX;
        int vy = mouseY - myScreenY;
        if (vx == 0 && vy == 0) return;
        
        double len = Math.sqrt(vx * vx + vy * vy);
        double nx = vx / len;
        double ny = vy / len;
        
        int lineLength = 50;
        int endX = myScreenX + (int) (nx * lineLength);
        int endY = myScreenY + (int) (ny * lineLength);
        
        g2d.setColor(new Color(255, 0, 0, 100));
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[] { 10f, 5f }, 0f));
        g2d.drawLine(myScreenX, myScreenY, endX, endY);
        g2d.setStroke(oldStroke);
        
        g2d.setColor(new Color(255, 255, 0, 150));
        g2d.drawOval(mouseX - 4, mouseY - 4, 8, 8);
        g2d.drawLine(mouseX - 6, mouseY, mouseX + 6, mouseY);
        g2d.drawLine(mouseX, mouseY - 6, mouseX, mouseY + 6);
    }
    
    private void drawMyEffects(Graphics2D g2d, List<GameState.ActiveEffect> myEffects, 
                              int myScreenX, int myScreenY) {
        if (myEffects.isEmpty()) return;
        
        for (GameState.ActiveEffect ef : myEffects) {
            float progress = 1f - (ef.remaining / ef.duration);
            int radius = 28 + (int) (Math.sin(progress * 6.28318) * 4);
            int alpha = (int) (160 * (ef.remaining / ef.duration));
            alpha = Math.max(40, Math.min(200, alpha));
            g2d.setColor(new Color(ef.color.getRed(), ef.color.getGreen(), ef.color.getBlue(), alpha));
            Stroke old = g2d.getStroke();
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawOval(myScreenX - radius, myScreenY - radius, radius * 2, radius * 2);
            g2d.setStroke(old);
            
            if ("piper_mark".equalsIgnoreCase(ef.abilityId)) {
                g2d.setColor(new Color(100, 220, 255, 90));
                g2d.drawOval(myScreenX - radius - 6, myScreenY - radius - 6, (radius + 6) * 2, (radius + 6) * 2);
                g2d.setColor(new Color(80, 200, 255, 60));
                g2d.drawOval(myScreenX - radius - 12, myScreenY - radius - 12, (radius + 12) * 2, (radius + 12) * 2);
            } else if ("piper_thermal".equalsIgnoreCase(ef.abilityId)) {
                double t = (ef.duration - ef.remaining);
                int glowR = radius + 8;
                g2d.setColor(new Color(255, 160, 40, 110));
                g2d.setStroke(new BasicStroke(4f));
                g2d.drawOval(myScreenX - glowR, myScreenY - glowR, glowR * 2, glowR * 2);
                g2d.setColor(new Color(255, 200, 80, 160));
                int arcStart = (int) ((t * 180) % 360);
                g2d.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawArc(myScreenX - glowR, myScreenY - glowR, glowR * 2, glowR * 2, arcStart, 60);
            }
        }
    }
    
    private void drawHealthBar(Graphics2D g, int x, int y, int hp, int maxHp) {
        int barWidth = 40;
        int barHeight = 5;
        
        g.setColor(Color.DARK_GRAY);
        g.fillRect(x - barWidth / 2, y, barWidth, barHeight);
        
        g.setColor(Color.GREEN);
        float ratio = (float) hp / Math.max(1, maxHp);
        int currentWidth = (int) (barWidth * ratio);
        g.fillRect(x - barWidth / 2, y, currentWidth, barHeight);
    }
    
    private void drawPlacedObject(Graphics2D g2d, GameState.PlacedObjectClient obj,
                                 int objScreenX, int objScreenY, int myTeam) {
        Color objColor;
        int size;
        String label;
        
        if ("tech_mine".equals(obj.type)) {
            objColor = obj.team == GameConstants.TEAM_RED ? 
                    new Color(200, 50, 50) : new Color(50, 100, 200);
            size = 16;
            label = "지뢰";
        } else if ("tech_turret".equals(obj.type)) {
            objColor = obj.team == GameConstants.TEAM_RED ? 
                    new Color(220, 80, 80) : new Color(80, 120, 220);
            size = 24;
            label = "터렛";
        } else {
            objColor = Color.GRAY;
            size = 20;
            label = "?";
        }
        
        g2d.setColor(objColor);
        g2d.fillRect(objScreenX - size / 2, objScreenY - size / 2, size, size);
        
        g2d.setColor(obj.team == myTeam ? Color.GREEN : Color.RED);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRect(objScreenX - size / 2, objScreenY - size / 2, size, size);
        
        // HP 바
        int barWidth = 30;
        int barHeight = 4;
        int barY = objScreenY - size / 2 - 8;
        
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(objScreenX - barWidth / 2, barY, barWidth, barHeight);
        
        float hpPercent = (float) obj.hp / obj.maxHp;
        Color hpColor = hpPercent > 0.6f ? Color.GREEN : 
                       hpPercent > 0.3f ? Color.YELLOW : Color.RED;
        
        g2d.setColor(hpColor);
        int currentBarWidth = (int) (barWidth * hpPercent);
        g2d.fillRect(objScreenX - barWidth / 2, barY, currentBarWidth, barHeight);
        
        g2d.setFont(new Font("맑은 고딕", Font.BOLD, 10));
        g2d.setColor(Color.WHITE);
        FontMetrics objFm = g2d.getFontMetrics();
        int labelWidth = objFm.stringWidth(label);
        g2d.drawString(label, objScreenX - labelWidth / 2, objScreenY + size / 2 + 12);
        
        String hpText = obj.hp + "/" + obj.maxHp;
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        objFm = g2d.getFontMetrics();
        int hpWidth = objFm.stringWidth(hpText);
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.drawString(hpText, objScreenX - hpWidth / 2, barY - 2);
    }
    
    private void drawMinimap(Graphics2D g2d, GameState gameState, int playerX, int playerY, int myTeam) {
        int minimapWidth = 200;
        int minimapHeight = 150;
        int minimapX = canvas.getWidth() - minimapWidth - 20;
        int minimapY = 20;
        
        float scaleX = (float) minimapWidth / gameState.getMapWidth();
        float scaleY = (float) minimapHeight / gameState.getMapHeight();
        
        if (mapImage != null) {
            g2d.drawImage(mapImage, minimapX, minimapY, minimapWidth, minimapHeight, null);
        } else {
            g2d.setColor(new Color(40, 45, 55));
            g2d.fillRect(minimapX, minimapY, minimapWidth, minimapHeight);
        }
        
        // 테두리
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRect(minimapX, minimapY, minimapWidth, minimapHeight);
        
        // 다른 플레이어들
        for (GameState.PlayerData p : gameState.getPlayers().values()) {
            int px = minimapX + (int) (p.x * scaleX);
            int py = minimapY + (int) (p.y * scaleY);
            Color dotColor = p.team == GameConstants.TEAM_RED ? 
                    new Color(255, 100, 100) : new Color(100, 150, 255);
            g2d.setColor(dotColor);
            g2d.fillOval(px - 3, py - 3, 6, 6);
        }
        
        // 내 위치
        int myMinimapX = minimapX + (int) (playerX * scaleX);
        int myMinimapY = minimapY + (int) (playerY * scaleY);
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(myMinimapX - 4, myMinimapY - 4, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.drawOval(myMinimapX - 4, myMinimapY - 4, 8, 8);
    }
    
    private void drawHUD(Graphics2D g, String playerName, int team, CharacterData currentCharacterData,
                        int myHP, int myMaxHP, int kills, int deaths) {
        if (currentCharacterData == null) {
            return;
        }
        
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(10, 10, 250, 160);
        
        g.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        g.setColor(Color.WHITE);
        
        int yPos = 30;
        g.drawString("플레이어: " + playerName, 20, yPos);
        yPos += 20;
        g.drawString("팀: " + (team == GameConstants.TEAM_RED ? "RED" : "BLUE"), 20, yPos);
        yPos += 20;
        g.drawString("캐릭터: " + currentCharacterData.name, 20, yPos);
        yPos += 20;
        g.drawString("HP: " + myHP + "/" + myMaxHP, 20, yPos);
        drawHealthBar(g, 130, yPos - 12, myHP, myMaxHP);
        yPos += 20;
        
        g.setColor(new Color(255, 215, 0));
        g.drawString("Kills: " + kills + " / Deaths: " + deaths, 20, yPos);
        yPos += 20;
        
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        g.setColor(new Color(255, 200, 200));
        g.drawString("최대HP: " + (int) currentCharacterData.health, 20, yPos);
        yPos += 18;
        g.setColor(new Color(200, 255, 200));
        g.drawString("속도: " + String.format("%.1f", currentCharacterData.speed), 20, yPos);
        
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        g.setColor(Color.YELLOW);
        g.drawString("좌클릭: 기본공격 | E: 전술스킬 | R: 궁극기", 20, canvas.getHeight() - 40);
        g.drawString("B키: 캐릭터 선택", 20, canvas.getHeight() - 20);
    }
    
    private void drawSkillHUD(Graphics2D g, Ability[] abilities, String selectedCharacter,
                             float piperMarkRemaining, float piperThermalRemaining) {
        if (abilities == null) return;
        
        int hudWidth = 400;
        int hudHeight = 80;
        int hudX = (canvas.getWidth() - hudWidth) / 2;
        int hudY = canvas.getHeight() - hudHeight - 70;
        
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(hudX, hudY, hudWidth, hudHeight, 10, 10);
        
        int skillWidth = 60;
        int skillHeight = 60;
        int skillGap = 20;
        int startX = hudX + (hudWidth - (skillWidth * 3 + skillGap * 2)) / 2;
        int skillY = hudY + 10;
        
        String[] keyLabels = { "좌클릭", "E", "R" };
        Color[] skillColors = {
                new Color(100, 200, 100),
                new Color(100, 150, 255),
                new Color(255, 100, 100)
        };
        
        for (int i = 0; i < 3 && i < abilities.length; i++) {
            Ability ability = abilities[i];
            int skillX = startX + i * (skillWidth + skillGap);
            
            if (ability.canUse()) {
                g.setColor(skillColors[i]);
            } else {
                g.setColor(new Color(40, 40, 40));
            }
            g.fillRoundRect(skillX, skillY, skillWidth, skillHeight, 8, 8);
            
            boolean piper = "piper".equalsIgnoreCase(selectedCharacter);
            float remain = 0f;
            Color activeBorder = Color.WHITE;
            if (piper) {
                if (i == 1) remain = Math.max(piperMarkRemaining, 0f);
                else if (i == 2) remain = Math.max(piperThermalRemaining, 0f);
                if (remain > 0f) {
                    activeBorder = (i == 1) ? new Color(80, 200, 255) : new Color(255, 160, 40);
                }
            }
            g.setColor(activeBorder);
            g.setStroke(new BasicStroke((piper && remain > 0f) ? 3f : 2f));
            g.drawRoundRect(skillX, skillY, skillWidth, skillHeight, 8, 8);
            
            if (!ability.canUse()) {
                float cooldownPercent = ability.getCooldownPercent();
                int overlayHeight = (int) (skillHeight * cooldownPercent);
                g.setColor(new Color(0, 0, 0, 160));
                g.fillRoundRect(skillX, skillY + (skillHeight - overlayHeight),
                        skillWidth, overlayHeight, 8, 8);
                
                g.setColor(Color.WHITE);
                g.setFont(new Font("맑은 고딕", Font.BOLD, 16));
                String cooldownText = String.format("%.1f", ability.getCurrentCooldown());
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(cooldownText);
                g.drawString(cooldownText,
                        skillX + (skillWidth - textWidth) / 2,
                        skillY + skillHeight / 2 + 6);
            }
            
            g.setColor(Color.YELLOW);
            g.setFont(new Font("맑은 고딕", Font.BOLD, 10));
            FontMetrics fm = g.getFontMetrics();
            int labelWidth = fm.stringWidth(keyLabels[i]);
            g.drawString(keyLabels[i],
                    skillX + (skillWidth - labelWidth) / 2,
                    skillY - 5);
            
            g.setColor(Color.WHITE);
            g.setFont(new Font("맑은 고딕", Font.PLAIN, 10));
            fm = g.getFontMetrics();
            int nameWidth = fm.stringWidth(ability.getName());
            g.drawString(ability.getName(),
                    skillX + (skillWidth - nameWidth) / 2,
                    skillY + skillHeight + 15);
        }
    }
    
    private void drawRoundInfo(Graphics2D g, GameState.RoundState roundState, int roundCount,
                              int redWins, int blueWins, long roundStartTime,
                              String centerMessage, long centerMessageEndTime) {
        int centerX = canvas.getWidth() / 2;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(centerX - 100, 0, 200, 40);
        
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(new Color(255, 100, 100));
        g.drawString(String.valueOf(redWins), centerX - 60, 30);
        
        g.setColor(Color.WHITE);
        g.drawString(":", centerX, 28);
        
        g.setColor(new Color(100, 150, 255));
        g.drawString(String.valueOf(blueWins), centerX + 40, 30);
        
        g.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        g.setColor(Color.WHITE);
        g.drawString("Round " + roundCount, centerX - 30, 55);
        
        if (roundState == GameState.RoundState.WAITING) {
            long remaining = Math.max(0, 10000 - (System.currentTimeMillis() - roundStartTime));
            int sec = (int) (remaining / 1000) + 1;
            if (remaining > 0) {
                drawCenterText(g, "라운드 시작까지 " + sec + "초", 40, Color.YELLOW, 0);
                drawCenterText(g, "캐릭터를 변경할 수 있습니다 (B키)", 20, Color.WHITE, 50);
            }
        } else if (!centerMessage.isEmpty() && System.currentTimeMillis() < centerMessageEndTime) {
            drawCenterText(g, centerMessage, 40, Color.YELLOW, 0);
        }
    }
    
    private void drawCenterText(Graphics2D g, String text, int size, Color color, int yOffset) {
        g.setFont(new Font("맑은 고딕", Font.BOLD, size));
        FontMetrics fm = g.getFontMetrics();
        int x = (canvas.getWidth() - fm.stringWidth(text)) / 2;
        int y = (canvas.getHeight() / 2) + yOffset;
        
        g.setColor(Color.BLACK);
        g.drawString(text, x + 2, y + 2);
        
        g.setColor(color);
        g.drawString(text, x, y);
    }
    
    private void drawEditorOverlay(Graphics2D g2d, boolean[][] walkableGrid, int gridCols, int gridRows,
                                  int cameraX, int cameraY, List<int[]> redSpawnTiles, List<int[]> blueSpawnTiles) {
        if (walkableGrid == null) return;
        
        int startCol = cameraX / TILE_SIZE;
        int startRow = cameraY / TILE_SIZE;
        int endCol = Math.min(gridCols - 1, (cameraX + GameConstants.GAME_WIDTH) / TILE_SIZE + 1);
        int endRow = Math.min(gridRows - 1, (cameraY + GameConstants.GAME_HEIGHT) / TILE_SIZE + 1);
        
        Rectangle minimapRect = null;
        if (showMinimap) {
            int minimapWidth = 200;
            int minimapHeight = 150;
            int minimapX = canvas.getWidth() - minimapWidth - 20;
            int minimapY = 20;
            minimapRect = new Rectangle(minimapX, minimapY, minimapWidth, minimapHeight);
        }
        
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                boolean walkable = walkableGrid[r][c];
                int px = c * TILE_SIZE - cameraX;
                int py = r * TILE_SIZE - cameraY;
                
                if (minimapRect != null) {
                    Rectangle tileRect = new Rectangle(px, py, TILE_SIZE, TILE_SIZE);
                    if (tileRect.intersects(minimapRect)) {
                        continue;
                    }
                }
                
                Color base = walkable ? new Color(0, 180, 0, 55) : new Color(180, 0, 0, 60);
                if (isSpawnTile(redSpawnTiles, c, r)) {
                    base = new Color(255, 60, 60, 120);
                } else if (isSpawnTile(blueSpawnTiles, c, r)) {
                    base = new Color(60, 120, 255, 120);
                }
                g2d.setColor(base);
                g2d.fillRect(px, py, TILE_SIZE, TILE_SIZE);
            }
        }
        
        if (hoverCol >= 0 && hoverRow >= 0 && hoverCol < gridCols && hoverRow < gridRows) {
            int hx = hoverCol * TILE_SIZE - cameraX;
            int hy = hoverRow * TILE_SIZE - cameraY;
            g2d.setColor(new Color(255, 255, 0, 120));
            g2d.drawRect(hx, hy, TILE_SIZE - 1, TILE_SIZE - 1);
        }
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        String modeName = switch (editPaintMode) {
            case 0 -> "이동 가능";
            case 1 -> "이동 불가";
            case 2 -> "RED 스폰";
            case 3 -> "BLUE 스폰";
            default -> "알 수 없음";
        };
        g2d.drawString("편집 모드: " + modeName + " (F5로 변경)", 10, canvas.getHeight() - 60);
    }
    
    private boolean isSpawnTile(List<int[]> tiles, int col, int row) {
        if (tiles == null) return false;
        for (int[] t : tiles) {
            if (t[0] == col && t[1] == row) return true;
        }
        return false;
    }
}
