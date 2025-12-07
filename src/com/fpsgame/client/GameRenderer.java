package com.fpsgame.client;

import com.fpsgame.client.effects.SkillEffectManager;
import com.fpsgame.common.Ability;
import com.fpsgame.common.CharacterData;
import com.fpsgame.common.GameConstants;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * GameRenderer - 게임 렌더링 전담
 * 
 * SOLID 원칙 준수:
 * - Single Responsibility: 렌더링만 담당
 * - GamePanel로부터 완전히 분리된 렌더링 로직
 * - 필요한 데이터를 RenderContext로 받아 렌더링
 */
public class GameRenderer {
    
    // 렌더링 상수
    private static final int VISION_RANGE = 800;
    private static final float PIPER_MARK_RANGE_FACTOR = 1.5f;
    private static final int PIPER_THERMAL_DOT_SIZE = 10;
    
    // 미사일 이미지
    private BufferedImage bulletImage;
    
    public GameRenderer() {
        // 파라미터 없는 생성자 - 렌더링에 필요한 데이터는 메서드 파라미터로 전달
        loadBulletImage();
    }
    
    private void loadBulletImage() {
        try {
            bulletImage = ImageIO.read(new File("assets/bullets/raven_bullet.png"));
            System.out.println("[BULLET] Loaded bullet image: " + bulletImage.getWidth() + "x" + bulletImage.getHeight());
        } catch (Exception e) {
            System.err.println("[BULLET] Failed to load bullet image: " + e.getMessage());
            bulletImage = null;
        }
    }
    
    /**
     * 메인 렌더링 진입점
     */
    public void render(Graphics g, RenderContext ctx) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 고정 해상도 렌더링 + 스케일링
        // 실제 캔버스 크기가 달라져도 항상 1280x720 영역만 보이도록
        double scaleX = (double) ctx.actualCanvasWidth / GameConstants.GAME_WIDTH;
        double scaleY = (double) ctx.actualCanvasHeight / GameConstants.GAME_HEIGHT;
        
        // 스케일 적용
        g2d.scale(scaleX, scaleY);
        
        // 이후 모든 렌더링은 1280x720 좌표계에서 진행
        // ctx.canvasWidth와 canvasHeight는 항상 고정값 사용
        
        // 1. 맵 배경
        drawMap(g2d, ctx);
        
        // 2. 장애물 디버그
        drawObstacles(g2d, ctx);
        
        // 3. 에어스트라이크 마커
        drawStrikeMarkers(g2d, ctx);
        
        // 4. 편집 모드 오버레이
        if (ctx.editMode) {
            drawEditorOverlay(g2d, ctx);
        }
        
        // 5. 다른 플레이어들
        drawOtherPlayers(g2d, ctx);
        
        // 6. 로컬 플레이어
        drawLocalPlayer(g2d, ctx);
        
        // 7. 조준선
        drawAimLine(g2d, ctx);
        
        // 8. 미사일
        drawMissiles(g2d, ctx);
        
        // 9. 설치된 오브젝트 (지뢰/터렛)
        drawPlacedObjects(g2d, ctx);
        
        // 10. UI 요소들 (항상 마지막)
        if (ctx.showMinimap) {
            drawMinimap(g2d, ctx);
        }
        drawHUD(g2d, ctx);
        drawRoundInfo(g2d, ctx);
        drawTeamStatus(g2d, ctx);
        
        // 11. TAB 스코어보드 (최상위)
        if (ctx.showScoreboard) {
            drawScoreboard(g2d, ctx);
        }
    }
    
    private void drawMap(Graphics2D g2d, RenderContext ctx) {
        if (ctx.mapImage != null) {
            g2d.drawImage(ctx.mapImage, -ctx.cameraX, -ctx.cameraY, ctx.mapWidth, ctx.mapHeight, null);
        } else {
            drawGrid(g2d, ctx);
        }
    }
    
    private void drawGrid(Graphics2D g2d, RenderContext ctx) {
        g2d.setColor(new Color(30, 35, 45));
        for (int x = 0; x < ctx.canvasWidth; x += 50) {
            g2d.drawLine(x, 0, x, ctx.canvasHeight);
        }
        for (int y = 0; y < ctx.canvasHeight; y += 50) {
            g2d.drawLine(0, y, ctx.canvasWidth, y);
        }
    }
    
    private void drawObstacles(Graphics2D g2d, RenderContext ctx) {
        if (!ctx.debugObstacles) return;
        
        g2d.setColor(new Color(255, 0, 0, 100));
        for (Rectangle obs : ctx.obstacles) {
            int screenX = obs.x - ctx.cameraX;
            int screenY = obs.y - ctx.cameraY;
            g2d.fillRect(screenX, screenY, obs.width, obs.height);
            g2d.setColor(new Color(255, 255, 0, 150));
            g2d.drawRect(screenX, screenY, obs.width, obs.height);
            g2d.setColor(new Color(255, 0, 0, 100));
        }
    }
    
    private void drawStrikeMarkers(Graphics2D g2d, RenderContext ctx) {
        if (ctx.strikeMarkers.isEmpty()) return;
        
        long currentTime = System.currentTimeMillis();
        for (GameObjectManager.StrikeMarker marker : ctx.strikeMarkers.values()) {
            int screenX = marker.x - ctx.cameraX;
            int screenY = marker.y - ctx.cameraY;
            
            if (isOnScreen(screenX, screenY, ctx)) {
                int radius = 120;
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
    
    private void drawOtherPlayers(Graphics2D g2d, RenderContext ctx) {
        for (Map.Entry<String, GamePanel.PlayerData> entry : ctx.players.entrySet()) {
            GamePanel.PlayerData p = entry.getValue();
            
            // 사망한 플레이어는 렌더링하지 않음
            if (p.hp <= 0) {
                continue;
            }
            
            int screenX = p.x - ctx.cameraX;
            int screenY = p.y - ctx.cameraY;
            
            if (isOnScreen(screenX, screenY, ctx)) {
                Color playerColor = p.team == GameConstants.TEAM_RED ? 
                    new Color(244, 67, 54) : new Color(33, 150, 243);
                
                if (p.animations != null) {
                    int animIndex = p.direction;
                    if (animIndex < p.animations.length && p.animations[animIndex] != null) {
                        p.animations[animIndex].draw(g2d, screenX - 20, screenY - 20, 40, 40);
                    } else {
                        g2d.setColor(playerColor);
                        g2d.fillOval(screenX - 20, screenY - 20, 48, 64);
                    }
                } else {
                    g2d.setColor(playerColor);
                    g2d.fillOval(screenX - 20, screenY - 20, 48, 64);
                }
                
                drawHealthBar(g2d, screenX, screenY + 25, p.hp, p.maxHp);
            }
        }
    }
    
    private void drawLocalPlayer(Graphics2D g2d, RenderContext ctx) {
        // 사망 시 자신의 캐릭터 렌더링 스킵
        if (ctx.myHP <= 0) {
            return;
        }

        int myScreenX = ctx.playerX - ctx.cameraX;
        int myScreenY = ctx.playerY - ctx.cameraY;
        
        Color myColor = ctx.team == GameConstants.TEAM_RED ? 
            new Color(255, 100, 100) : new Color(100, 150, 255);
        
        if (ctx.myAnimations != null) {
            int animIndex = ctx.myDirection;
            if (animIndex < ctx.myAnimations.length && ctx.myAnimations[animIndex] != null) {
                ctx.myAnimations[animIndex].draw(g2d, myScreenX - 20, myScreenY - 20, 40, 40);
            } else {
                g2d.setColor(myColor);
                g2d.fillOval(myScreenX - 20, myScreenY - 20, 40, 40);
            }
        } else {
            g2d.setColor(myColor);
            g2d.fillOval(myScreenX - 20, myScreenY - 20, 40, 40);
        }
        
        // 이펙트
        drawMyEffects(g2d, ctx);
        ctx.skillEffects.drawSelf(g2d, myScreenX, myScreenY);
        
        // 이름
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int nameWidth = fm.stringWidth(ctx.playerName + " (You)");
        g2d.drawString(ctx.playerName + " (You)", myScreenX - nameWidth / 2, myScreenY - 25);
        
        // HP 바
        drawHealthBar(g2d, myScreenX, myScreenY + 25, ctx.myHP, ctx.myMaxHP);
    }
    
    private void drawAimLine(Graphics2D g2d, RenderContext ctx) {
        int myScreenX = ctx.playerX - ctx.cameraX;
        int myScreenY = ctx.playerY - ctx.cameraY;
        
        int vx = ctx.mouseX - myScreenX;
        int vy = ctx.mouseY - myScreenY;
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
        g2d.drawOval(ctx.mouseX - 4, ctx.mouseY - 4, 8, 8);
        g2d.drawLine(ctx.mouseX - 6, ctx.mouseY, ctx.mouseX + 6, ctx.mouseY);
        g2d.drawLine(ctx.mouseX, ctx.mouseY - 6, ctx.mouseX, ctx.mouseY + 6);
    }
    
    private void drawMyEffects(Graphics2D g2d, RenderContext ctx) {
        if (ctx.myEffects.isEmpty()) return;
        
        int myScreenX = ctx.playerX - ctx.cameraX;
        int myScreenY = ctx.playerY - ctx.cameraY;
        
        for (GamePanel.ActiveEffect ef : ctx.myEffects) {
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
    
    private void drawMissiles(Graphics2D g2d, RenderContext ctx) {
        for (GameObjectManager.Missile m : ctx.missiles) {
            int mScreenX = (int)m.x - ctx.cameraX;
            int mScreenY = (int)m.y - ctx.cameraY;
            if (isOnScreen(mScreenX, mScreenY, ctx)) {
                if (bulletImage != null) {
                    // 16x16 이미지를 8x8로 다운스케일하여 렌더링
                    g2d.drawImage(bulletImage, mScreenX - 4, mScreenY - 4, 8, 8, null);
                } else {
                    // 이미지 로드 실패 시 기본 노란 원
                    g2d.setColor(Color.YELLOW);
                    g2d.fillOval(mScreenX - 4, mScreenY - 4, 8, 8);
                }
            }
        }
    }
    
    private void drawPlacedObjects(Graphics2D g2d, RenderContext ctx) {
        for (Map.Entry<Integer, GameObjectManager.PlacedObjectClient> entry : ctx.placedObjects.entrySet()) {
            GameObjectManager.PlacedObjectClient obj = entry.getValue();
            int objScreenX = obj.x - ctx.cameraX;
            int objScreenY = obj.y - ctx.cameraY;
            
            if (isOnScreen(objScreenX, objScreenY, ctx)) {
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
                
                g2d.setColor(obj.team == ctx.team ? Color.GREEN : Color.RED);
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRect(objScreenX - size / 2, objScreenY - size / 2, size, size);
                
                int barWidth = 30;
                int barHeight = 4;
                int barY = objScreenY - size / 2 - 8;
                
                g2d.setColor(Color.DARK_GRAY);
                g2d.fillRect(objScreenX - barWidth / 2, barY, barWidth, barHeight);
                
                float hpPercent = (float) obj.hp / obj.maxHp;
                Color hpColor = hpPercent > 0.6f ? Color.GREEN : 
                               (hpPercent > 0.3f ? Color.YELLOW : Color.RED);
                
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
    
    private void drawMinimap(Graphics2D g2d, RenderContext ctx) {
        int minimapWidth = 200;
        int minimapHeight = 150;
        int minimapX = ctx.canvasWidth - minimapWidth - 20;
        int minimapY = 20;
        
        float scaleX = (float) minimapWidth / ctx.mapWidth;
        float scaleY = (float) minimapHeight / ctx.mapHeight;
        
        if (ctx.mapImage != null) {
            g2d.drawImage(ctx.mapImage, minimapX, minimapY, minimapWidth, minimapHeight, null);
        } else {
            g2d.setColor(new Color(20, 20, 30, 200));
            g2d.fillRect(minimapX, minimapY, minimapWidth, minimapHeight);
            if (ctx.obstacles != null && !ctx.obstacles.isEmpty()) {
                g2d.setColor(new Color(200, 60, 60, 180));
                for (Rectangle obs : ctx.obstacles) {
                    int ox = minimapX + Math.round(obs.x * scaleX);
                    int oy = minimapY + Math.round(obs.y * scaleY);
                    int ow = Math.max(1, Math.round(obs.width * scaleX));
                    int oh = Math.max(1, Math.round(obs.height * scaleY));
                    g2d.fillRect(ox, oy, ow, oh);
                }
            }
        }
        
        g2d.setColor(Color.WHITE);
        g2d.drawRect(minimapX, minimapY, minimapWidth, minimapHeight);
        
        // 팀원들의 시야 범위 먼저 그리기 (네모 박스)
        int viewW = Math.max(1, Math.round(GameConstants.GAME_WIDTH * scaleX));
        int viewH = Math.max(1, Math.round(GameConstants.GAME_HEIGHT * scaleY));
        
        synchronized (ctx.players) {
            for (GamePanel.PlayerData pd : ctx.players.values()) {
                // 같은 팀만 표시
                if (pd.team == ctx.team) {
                    // 팀원의 카메라 위치 계산 (캐릭터를 화면 중앙에 놓는다고 가정)
                    int teammateCameraX = pd.x - GameConstants.GAME_WIDTH / 2;
                    int teammateCameraY = pd.y - GameConstants.GAME_HEIGHT / 2;
                    
                    int teammateViewX = minimapX + Math.round(teammateCameraX * scaleX);
                    int teammateViewY = minimapY + Math.round(teammateCameraY * scaleY);
                    
                    // 팀원 시야 범위 (연한 팀 색상 박스) - 일반 크기
                    if (pd.team == GameConstants.TEAM_BLUE) {
                        g2d.setColor(new Color(100, 150, 255, 30)); // 연한 파란색
                        g2d.drawRect(teammateViewX, teammateViewY, viewW, viewH);
                    } else if (pd.team == GameConstants.TEAM_RED) {
                        g2d.setColor(new Color(255, 100, 100, 30)); // 연한 빨간색
                        g2d.drawRect(teammateViewX, teammateViewY, viewW, viewH);
                    }
                }
            }
        }
        
        // 내 시야 범위 (더 진하게) - Mark 스킬 활성화 시 Piper만 확장
        boolean myMarkActive = (ctx.piperMarkRemaining > 0f);
        int myViewW = viewW;
        int myViewH = viewH;
        int myOffsetX = 0;
        int myOffsetY = 0;
        
        if (myMarkActive) {
            myViewW = Math.round(viewW * PIPER_MARK_RANGE_FACTOR);
            myViewH = Math.round(viewH * PIPER_MARK_RANGE_FACTOR);
            myOffsetX = Math.round((myViewW - viewW) / 2);
            myOffsetY = Math.round((myViewH - viewH) / 2);
        }
        
        int viewX = minimapX + Math.round(ctx.cameraX * scaleX) - myOffsetX;
        int viewY = minimapY + Math.round(ctx.cameraY * scaleY) - myOffsetY;
        g2d.setColor(new Color(255, 255, 255, 120));
        g2d.drawRect(viewX, viewY, myViewW, myViewH);
        
        // 내 캐릭터 아이콘
        int myMinimapX = minimapX + (int) (ctx.playerX * scaleX);
        int myMinimapY = minimapY + (int) (ctx.playerY * scaleY);
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(myMinimapX - 4, myMinimapY - 4, 8, 8);
        g2d.setColor(Color.ORANGE);
        g2d.drawOval(myMinimapX - 5, myMinimapY - 5, 10, 10);
        
        // Mark 스킬로 확장된 시야 범위 계산
        int myExtendedRadius = (int) (VISION_RANGE * (myMarkActive ? PIPER_MARK_RANGE_FACTOR : 1f));
        
        // 팀 전체 Thermal 효과 (팀원이 Thermal 사용 시)
        boolean teamThermalActive = (ctx.teamThermalRemaining > 0f);
        
        synchronized (ctx.players) {
            for (GamePanel.PlayerData pd : ctx.players.values()) {
                int dx = pd.x - ctx.playerX;
                int dy = pd.y - ctx.playerY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                
                boolean inViewport = (pd.x >= ctx.cameraX && pd.x <= ctx.cameraX + ctx.canvasWidth &&
                        pd.y >= ctx.cameraY && pd.y <= ctx.cameraY + ctx.canvasHeight);
                
                boolean shouldShow = false;
                
                // 내 시야에 보이는지 체크
                if (myMarkActive && distance <= myExtendedRadius) {
                    shouldShow = true;
                } else if (!myMarkActive && distance <= VISION_RANGE && inViewport) {
                    shouldShow = true;
                }
                
                // 팀원의 시야에 보이는지 체크 (같은 팀의 적만)
                if (!shouldShow && pd.team != ctx.team) {
                    // 팀 Thermal 활성화 시 모든 적 표시
                    if (teamThermalActive) {
                        shouldShow = true;
                    } else {
                        // 팀원의 일반 시야 체크
                        synchronized (ctx.players) {
                            for (GamePanel.PlayerData teammate : ctx.players.values()) {
                                // 같은 팀원만 체크
                                if (teammate.team == ctx.team) {
                                    // 팀원의 카메라 위치 계산
                                    int teammateCameraX = teammate.x - GameConstants.GAME_WIDTH / 2;
                                    int teammateCameraY = teammate.y - GameConstants.GAME_HEIGHT / 2;
                                    
                                    // 팀원의 뷰포트 안에 있는지 체크 (일반 크기)
                                    boolean inTeammateViewport = (pd.x >= teammateCameraX && 
                                            pd.x <= teammateCameraX + GameConstants.GAME_WIDTH &&
                                            pd.y >= teammateCameraY && 
                                            pd.y <= teammateCameraY + GameConstants.GAME_HEIGHT);
                                    
                                    if (inTeammateViewport) {
                                        int tdx = pd.x - teammate.x;
                                        int tdy = pd.y - teammate.y;
                                        double teamDistance = Math.sqrt(tdx * tdx + tdy * tdy);
                                        
                                        // 팀원의 시야 범위 내에 있으면 표시 (일반 범위)
                                        if (teamDistance <= VISION_RANGE) {
                                            shouldShow = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (shouldShow) {
                    int otherX = minimapX + (int) (pd.x * scaleX);
                    int otherY = minimapY + (int) (pd.y * scaleY);
                    
                    // Thermal 효과로 보이는 적 (파란 점으로 크게 표시)
                    if (teamThermalActive && pd.team != ctx.team) {
                        // 적 팀은 팀 색상으로 표시
                        if (pd.team == GameConstants.TEAM_BLUE) {
                            g2d.setColor(new Color(100, 150, 255)); // 파란색
                        } else if (pd.team == GameConstants.TEAM_RED) {
                            g2d.setColor(new Color(255, 100, 100)); // 빨간색
                        } else {
                            g2d.setColor(Color.YELLOW);
                        }
                        g2d.fillOval(otherX - PIPER_THERMAL_DOT_SIZE / 2, otherY - PIPER_THERMAL_DOT_SIZE / 2,
                                PIPER_THERMAL_DOT_SIZE, PIPER_THERMAL_DOT_SIZE);
                    } else {
                        // 일반 시야로 보이는 플레이어 (작은 점)
                        if (pd.team == GameConstants.TEAM_BLUE) {
                            g2d.setColor(Color.BLUE);
                        } else if (pd.team == GameConstants.TEAM_RED) {
                            g2d.setColor(Color.RED);
                        } else {
                            g2d.setColor(Color.GRAY);
                        }
                        g2d.fillOval(otherX - 3, otherY - 3, 6, 6);
                    }
                }
            }
        }
        
        for (Map.Entry<Integer, GameObjectManager.StrikeMarker> entry : ctx.strikeMarkers.entrySet()) {
            GameObjectManager.StrikeMarker marker = entry.getValue();
            int markerX = minimapX + (int) (marker.x * scaleX);
            int markerY = minimapY + (int) (marker.y * scaleY);
            
            long currentTime = System.currentTimeMillis();
            float pulsePhase = (currentTime % 1000) / 1000f;
            int pulseAlpha = (int) (150 + 105 * Math.sin(pulsePhase * Math.PI * 2));
            g2d.setColor(new Color(255, 0, 0, pulseAlpha));
            g2d.fillOval(markerX - 8, markerY - 8, 16, 16);
            
            g2d.setColor(new Color(255, 255, 0, 255));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawLine(markerX - 10, markerY, markerX + 10, markerY);
            g2d.drawLine(markerX, markerY - 10, markerX, markerY + 10);
            
            g2d.setFont(new Font("Arial", Font.BOLD, 9));
            g2d.setColor(new Color(255, 255, 255, 255));
            g2d.drawString("!", markerX - 3, markerY + 4);
        }
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString("MAP", minimapX + 5, minimapY + 12);
    }
    
    private void drawHUD(Graphics2D g, RenderContext ctx) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(10, 10, 220, 170);
        
        g.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        g.setColor(Color.WHITE);
        
        int yPos = 30;
        g.drawString("플레이어: " + ctx.playerName, 20, yPos);
        yPos += 20;
        g.drawString("팀: " + (ctx.team == GameConstants.TEAM_RED ? "RED" : "BLUE"), 20, yPos);
        yPos += 20;
        g.drawString("캐릭터: " + ctx.currentCharacterData.name, 20, yPos);
        yPos += 20;
        
        g.drawString("HP: " + ctx.myHP + "/" + ctx.myMaxHP, 20, yPos);
        drawHealthBar(g, 130, yPos - 12, ctx.myHP, ctx.myMaxHP);
        yPos += 20;
        
        g.setColor(new Color(255, 215, 0));
        g.drawString("Kills: " + ctx.kills + " / Deaths: " + ctx.deaths, 20, yPos);
        yPos += 20;
        
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        g.setColor(new Color(255, 200, 200));
        g.drawString("최대HP: " + (int) ctx.currentCharacterData.health, 20, yPos);
        yPos += 18;
        g.setColor(new Color(200, 255, 200));
        g.drawString("속도: " + String.format("%.1f", ctx.currentCharacterData.speed), 20, yPos);
        yPos += 18;
        
        // 디버그: 마우스 좌표 표시
        g.setColor(Color.CYAN);
        g.drawString("마우스: (" + ctx.mouseX + ", " + ctx.mouseY + ")", 20, yPos);
        
        drawSkillHUD(g, ctx);
        
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        g.setColor(Color.YELLOW);
        g.drawString("좌클릭: 기본공격 | E: 전술스킬 | R: 궁극기", 20, ctx.canvasHeight - 40);
        g.drawString("B키: 캐릭터 선택", 20, ctx.canvasHeight - 20);
    }
    
    private void drawSkillHUD(Graphics2D g, RenderContext ctx) {
        if (ctx.abilities == null) return;
        
        int hudWidth = 400;
        int hudHeight = 80;
        int hudX = (ctx.canvasWidth - hudWidth) / 2;
        int hudY = ctx.canvasHeight - hudHeight - 40;
        
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
        
        for (int i = 0; i < 3 && i < ctx.abilities.length; i++) {
            Ability ability = ctx.abilities[i];
            int skillX = startX + i * (skillWidth + skillGap);
            
            if (ability.canUse()) {
                g.setColor(skillColors[i]);
            } else {
                g.setColor(new Color(40, 40, 40));
            }
            g.fillRoundRect(skillX, skillY, skillWidth, skillHeight, 8, 8);
            
            boolean piper = "piper".equalsIgnoreCase(ctx.selectedCharacter);
            float remain = 0f;
            Color activeBorder = Color.WHITE;
            if (piper) {
                if (i == 1) remain = Math.max(ctx.piperMarkRemaining, 0f);
                else if (i == 2) remain = Math.max(ctx.piperThermalRemaining, 0f);
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
    
    private void drawRoundInfo(Graphics2D g, RenderContext ctx) {
        int centerX = ctx.canvasWidth / 2;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(centerX - 100, 0, 200, 40);
        
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(new Color(255, 100, 100));
        g.drawString(String.valueOf(ctx.redWins), centerX - 60, 30);
        
        g.setColor(Color.WHITE);
        g.drawString(":", centerX, 28);
        
        g.setColor(new Color(100, 150, 255));
        g.drawString(String.valueOf(ctx.blueWins), centerX + 40, 30);
        
        g.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        g.setColor(Color.WHITE);
        g.drawString("Round " + ctx.roundCount, centerX - 30, 55);
        
        // WAITING 상태면 10초 카운트 표시
        if (ctx.roundState == GamePanel.RoundState.WAITING) {
            long remaining = Math.max(0, GamePanel.ROUND_READY_TIME - (System.currentTimeMillis() - ctx.roundStartTime));
            int sec = (int) (remaining / 1000) + 1;
            if (remaining > 0) {
                drawCenterText(g, "라운드 시작까지 " + sec + "초", 40, Color.YELLOW, ctx);
                drawCenterText(g, "캐릭터를 변경할 수 있습니다 (B키)", 20, Color.WHITE, 50, ctx);
            }
        }
        // WAITING이 아닌 경우에만 중앙 메시지 표시 (ROUND_WIN, GAME_OVER 등)
        else if (!ctx.centerMessage.isEmpty() && System.currentTimeMillis() < ctx.centerMessageEndTime) {
            drawCenterText(g, ctx.centerMessage, 40, Color.YELLOW, ctx);
        }
    }
    
    private void drawCenterText(Graphics2D g, String text, int size, Color color, RenderContext ctx) {
        drawCenterText(g, text, size, color, 0, ctx);
    }
    
    private void drawCenterText(Graphics2D g, String text, int size, Color color, int yOffset, RenderContext ctx) {
        g.setFont(new Font("맑은 고딕", Font.BOLD, size));
        FontMetrics fm = g.getFontMetrics();
        int x = (ctx.canvasWidth - fm.stringWidth(text)) / 2;
        int y = (ctx.canvasHeight / 2) + yOffset;
        
        g.setColor(Color.BLACK);
        g.drawString(text, x + 2, y + 2);
        
        g.setColor(color);
        g.drawString(text, x, y);
    }
    
    private void drawEditorOverlay(Graphics2D g2d, RenderContext ctx) {
        if (ctx.walkableGrid == null) return;
        
        int startCol = ctx.cameraX / ctx.tileSize;
        int startRow = ctx.cameraY / ctx.tileSize;
        int endCol = Math.min(ctx.gridCols - 1, (ctx.cameraX + GameConstants.GAME_WIDTH) / ctx.tileSize + 1);
        int endRow = Math.min(ctx.gridRows - 1, (ctx.cameraY + GameConstants.GAME_HEIGHT) / ctx.tileSize + 1);
        
        Rectangle minimapRect = null;
        if (ctx.showMinimap) {
            int minimapWidth = 200;
            int minimapHeight = 150;
            int minimapX = ctx.canvasWidth - minimapWidth - 20;
            int minimapY = 20;
            minimapRect = new Rectangle(minimapX, minimapY, minimapWidth, minimapHeight);
        }
        
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                boolean walkable = ctx.walkableGrid[r][c];
                int px = c * ctx.tileSize - ctx.cameraX;
                int py = r * ctx.tileSize - ctx.cameraY;
                
                if (minimapRect != null && minimapRect.intersects(px, py, ctx.tileSize, ctx.tileSize)) {
                    continue;
                }
                
                if (walkable) {
                    g2d.setColor(new Color(0, 255, 0, 30));
                } else {
                    g2d.setColor(new Color(255, 0, 0, 60));
                }
                g2d.fillRect(px, py, ctx.tileSize, ctx.tileSize);
                
                g2d.setColor(new Color(150, 150, 150, 100));
                g2d.drawRect(px, py, ctx.tileSize, ctx.tileSize);
            }
        }
    }
    
    private void drawTeamStatus(Graphics2D g, RenderContext ctx) {
        // 상단 중앙 스코어 박스 기준점
        int centerX = ctx.canvasWidth / 2;
        int scoreBoxWidth = 200;
        int scoreBoxHeight = 40;
        int scoreBoxX = centerX - scoreBoxWidth / 2;
        
        // 플레이어들을 팀별로 분류
        java.util.List<Map.Entry<String, GamePanel.PlayerData>> redTeam = new ArrayList<>();
        java.util.List<Map.Entry<String, GamePanel.PlayerData>> blueTeam = new ArrayList<>();
        
        if (ctx.players != null) {
            for (Map.Entry<String, GamePanel.PlayerData> entry : ctx.players.entrySet()) {
                if (entry.getValue().team == GameConstants.TEAM_RED) {
                    redTeam.add(entry);
                } else {
                    blueTeam.add(entry);
                }
            }
        }
        
        // 내 정보도 추가
        if (ctx.team == GameConstants.TEAM_RED) {
            redTeam.add(null); // 자신
        } else {
            blueTeam.add(null); // 자신
        }
        
        int boxSize = scoreBoxHeight;
        int gap = 5;
        
        // RED 팀 (왼쪽)
        int redStartX = scoreBoxX - (redTeam.size() * (boxSize + gap));
        for (int i = 0; i < redTeam.size(); i++) {
            Map.Entry<String, GamePanel.PlayerData> entry = redTeam.get(i);
            int x = redStartX + i * (boxSize + gap);
            int y = 5;
            
            // 배경
            g.setColor(new Color(139, 0, 0, 180));
            g.fillRect(x, y, boxSize, boxSize);
            g.setColor(new Color(255, 0, 0));
            g.setStroke(new BasicStroke(2));
            g.drawRect(x, y, boxSize, boxSize);
            
            if (entry == null) {
                // 자신
                boolean isDead = ctx.myHP <= 0;
                
                if (isDead) {
                    // 사망 상태 표시 (X 표시와 어두운 배경)
                    g.setColor(new Color(60, 0, 0, 220));
                    g.fillRect(x, y, boxSize, boxSize);
                    g.setColor(new Color(255, 0, 0));
                    g.setStroke(new BasicStroke(3));
                    g.drawLine(x + 5, y + 5, x + boxSize - 5, y + boxSize - 5);
                    g.drawLine(x + boxSize - 5, y + 5, x + 5, y + boxSize - 5);
                } else {
                    drawCharacterIcon(g, ctx.selectedCharacter, x + 2, y + 2, boxSize - 4);
                    // HP 바
                    int hpBarHeight = 4;
                    int hpBarY = y + boxSize - hpBarHeight - 2;
                    float hpPercent = (float) ctx.myHP / ctx.myMaxHP;
                    g.setColor(new Color(0, 0, 0, 200));
                    g.fillRect(x + 2, hpBarY, boxSize - 4, hpBarHeight);
                    g.setColor(Color.GREEN);
                    g.fillRect(x + 2, hpBarY, (int)((boxSize - 4) * hpPercent), hpBarHeight);
                }
            } else {
                GamePanel.PlayerData player = entry.getValue();
                boolean isDead = player.hp <= 0;
                
                if (isDead) {
                    // 사망 상태 표시
                    g.setColor(new Color(60, 0, 0, 220));
                    g.fillRect(x, y, boxSize, boxSize);
                    g.setColor(new Color(255, 0, 0));
                    g.setStroke(new BasicStroke(3));
                    g.drawLine(x + 5, y + 5, x + boxSize - 5, y + boxSize - 5);
                    g.drawLine(x + boxSize - 5, y + 5, x + 5, y + boxSize - 5);
                } else {
                    drawCharacterIcon(g, player.characterId, x + 2, y + 2, boxSize - 4);
                    // HP 바
                    int hpBarHeight = 4;
                    int hpBarY = y + boxSize - hpBarHeight - 2;
                    float hpPercent = (float) player.hp / player.maxHp;
                    g.setColor(new Color(0, 0, 0, 200));
                    g.fillRect(x + 2, hpBarY, boxSize - 4, hpBarHeight);
                    g.setColor(Color.GREEN);
                    g.fillRect(x + 2, hpBarY, (int)((boxSize - 4) * hpPercent), hpBarHeight);
                }
            }
        }
        
        // BLUE 팀 (오른쪽)
        int blueStartX = scoreBoxX + scoreBoxWidth + gap;
        for (int i = 0; i < blueTeam.size(); i++) {
            Map.Entry<String, GamePanel.PlayerData> entry = blueTeam.get(i);
            int x = blueStartX + i * (boxSize + gap);
            int y = 5;
            
            // 배경
            g.setColor(new Color(0, 0, 139, 180));
            g.fillRect(x, y, boxSize, boxSize);
            g.setColor(new Color(0, 150, 255));
            g.setStroke(new BasicStroke(2));
            g.drawRect(x, y, boxSize, boxSize);
            
            // 캐릭터 아이콘만 (상대팀은 HP 안 보임)
            if (entry == null) {
                // 자신
                boolean isDead = ctx.myHP <= 0;
                
                if (isDead) {
                    // 사망 상태 표시
                    g.setColor(new Color(0, 0, 60, 220));
                    g.fillRect(x, y, boxSize, boxSize);
                    g.setColor(new Color(100, 150, 255));
                    g.setStroke(new BasicStroke(3));
                    g.drawLine(x + 5, y + 5, x + boxSize - 5, y + boxSize - 5);
                    g.drawLine(x + boxSize - 5, y + 5, x + 5, y + boxSize - 5);
                } else {
                    drawCharacterIcon(g, ctx.selectedCharacter, x + 2, y + 2, boxSize - 4);
                    // HP 바
                    int hpBarHeight = 4;
                    int hpBarY = y + boxSize - hpBarHeight - 2;
                    float hpPercent = (float) ctx.myHP / ctx.myMaxHP;
                    g.setColor(new Color(0, 0, 0, 200));
                    g.fillRect(x + 2, hpBarY, boxSize - 4, hpBarHeight);
                    g.setColor(Color.GREEN);
                    g.fillRect(x + 2, hpBarY, (int)((boxSize - 4) * hpPercent), hpBarHeight);
                }
            } else {
                GamePanel.PlayerData player = entry.getValue();
                boolean isDead = player.hp <= 0;
                
                if (isDead) {
                    // 사망 상태 표시
                    g.setColor(new Color(0, 0, 60, 220));
                    g.fillRect(x, y, boxSize, boxSize);
                    g.setColor(new Color(100, 150, 255));
                    g.setStroke(new BasicStroke(3));
                    g.drawLine(x + 5, y + 5, x + boxSize - 5, y + boxSize - 5);
                    g.drawLine(x + boxSize - 5, y + 5, x + 5, y + boxSize - 5);
                } else {
                    drawCharacterIcon(g, player.characterId, x + 2, y + 2, boxSize - 4);
                    // HP 바
                    int hpBarHeight = 4;
                    int hpBarY = y + boxSize - hpBarHeight - 2;
                    float hpPercent = (float) player.hp / player.maxHp;
                    g.setColor(new Color(0, 0, 0, 200));
                    g.fillRect(x + 2, hpBarY, boxSize - 4, hpBarHeight);
                    g.setColor(Color.GREEN);
                    g.fillRect(x + 2, hpBarY, (int)((boxSize - 4) * hpPercent), hpBarHeight);
                }
            }
        }
    }
    
    private void drawCharacterIcon(Graphics2D g, String characterId, int x, int y, int size) {
        // 캐릭터별 색상으로 간단한 아이콘 표시
        if (characterId == null) return;
        
        Color iconColor = switch (characterId.toLowerCase()) {
            case "raven" -> new Color(120, 80, 160);
            case "piper" -> new Color(255, 140, 0);
            case "technician" -> new Color(70, 180, 70);
            case "general" -> new Color(180, 150, 50);
            default -> Color.GRAY;
        };
        
        g.setColor(iconColor);
        g.fillOval(x + 2, y + 2, size - 4, size - 4);
        
        // 캐릭터 이니셜
        g.setColor(Color.WHITE);
        g.setFont(new Font("맑은 고딕", Font.BOLD, size / 2));
        String initial = characterId.substring(0, 1).toUpperCase();
        FontMetrics fm = g.getFontMetrics();
        int textX = x + (size - fm.stringWidth(initial)) / 2;
        int textY = y + (size + fm.getAscent()) / 2 - 2;
        g.drawString(initial, textX, textY);
    }
    
    private void drawScoreboard(Graphics2D g, RenderContext ctx) {
        // 반투명 오버레이
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, ctx.canvasWidth, ctx.canvasHeight);
        
        int boardWidth = 800;
        int boardHeight = 500;
        int boardX = (ctx.canvasWidth - boardWidth) / 2;
        int boardY = (ctx.canvasHeight - boardHeight) / 2;
        
        // 스코어보드 배경
        g.setColor(new Color(40, 40, 40, 240));
        g.fillRoundRect(boardX, boardY, boardWidth, boardHeight, 15, 15);
        g.setColor(new Color(255, 255, 255, 100));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(boardX, boardY, boardWidth, boardHeight, 15, 15);
        
        // 타이틀
        g.setFont(new Font("맑은 고딕", Font.BOLD, 32));
        g.setColor(Color.WHITE);
        String title = "SCOREBOARD";
        FontMetrics fm = g.getFontMetrics();
        int titleX = boardX + (boardWidth - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, boardY + 50);
        
        // 라운드 스코어
        g.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        String score = "RED " + ctx.redWins + " : " + ctx.blueWins + " BLUE";
        int scoreX = boardX + (boardWidth - g.getFontMetrics().stringWidth(score)) / 2;
        g.drawString(score, scoreX, boardY + 90);
        
        // 플레이어들을 팀별로 분류
        java.util.List<PlayerInfo> redTeam = new ArrayList<>();
        java.util.List<PlayerInfo> blueTeam = new ArrayList<>();
        
        if (ctx.players != null) {
            for (Map.Entry<String, GamePanel.PlayerData> entry : ctx.players.entrySet()) {
                GamePanel.PlayerData p = entry.getValue();
                PlayerInfo info = new PlayerInfo();
                info.name = entry.getKey();
                info.characterId = p.characterId;
                info.kills = p.kills;
                info.deaths = p.deaths;
                info.team = p.team;
                
                if (p.team == GameConstants.TEAM_RED) {
                    redTeam.add(info);
                } else {
                    blueTeam.add(info);
                }
            }
        }
        
        // 내 정보도 추가
        PlayerInfo myInfo = new PlayerInfo();
        myInfo.name = ctx.playerName;
        myInfo.characterId = ctx.selectedCharacter;
        myInfo.kills = ctx.kills;
        myInfo.deaths = ctx.deaths;
        myInfo.team = ctx.team;
        
        if (ctx.team == GameConstants.TEAM_RED) {
            redTeam.add(myInfo);
        } else {
            blueTeam.add(myInfo);
        }
        
        // 정렬 (K/D 비율 순)
        java.util.Comparator<PlayerInfo> kdComparator = (a, b) -> {
            float kdA = a.deaths == 0 ? a.kills : (float)a.kills / a.deaths;
            float kdB = b.deaths == 0 ? b.kills : (float)b.kills / b.deaths;
            return Float.compare(kdB, kdA);
        };
        redTeam.sort(kdComparator);
        blueTeam.sort(kdComparator);
        
        // 헤더
        int headerY = boardY + 130;
        g.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        g.setColor(new Color(200, 200, 200));
        g.drawString("NAME", boardX + 60, headerY);
        g.drawString("CHARACTER", boardX + 250, headerY);
        g.drawString("ULT", boardX + 420, headerY);
        g.drawString("K", boardX + 520, headerY);
        g.drawString("D", boardX + 600, headerY);
        g.drawString("K/D", boardX + 680, headerY);
        
        // RED 팀
        int yPos = headerY + 40;
        g.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        g.setColor(new Color(255, 100, 100));
        g.drawString("RED TEAM", boardX + 20, yPos);
        yPos += 30;
        
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        for (PlayerInfo info : redTeam) {
            boolean isMe = info.name.equals(ctx.playerName);
            if (isMe) {
                g.setColor(new Color(255, 255, 0, 100));
                g.fillRect(boardX + 10, yPos - 18, boardWidth - 20, 25);
            }
            
            // 캐릭터 아이콘
            drawCharacterIcon(g, info.characterId, boardX + 20, yPos - 16, 20);
            
            g.setColor(Color.WHITE);
            g.drawString(info.name, boardX + 60, yPos);
            g.drawString(getCharacterName(info.characterId), boardX + 250, yPos);
            
            // 궁극기 상태 (간단히 ON/OFF)
            String ultStatus = getUltStatus(info, ctx);
            if ("READY".equals(ultStatus)) {
                g.setColor(new Color(0, 255, 0));
            } else {
                g.setColor(new Color(150, 150, 150));
            }
            g.drawString(ultStatus, boardX + 420, yPos);
            
            g.setColor(Color.WHITE);
            g.drawString(String.valueOf(info.kills), boardX + 520, yPos);
            g.drawString(String.valueOf(info.deaths), boardX + 600, yPos);
            float kd = info.deaths == 0 ? info.kills : (float)info.kills / info.deaths;
            g.drawString(String.format("%.2f", kd), boardX + 680, yPos);
            
            yPos += 30;
        }
        
        // BLUE 팀
        yPos += 20;
        g.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        g.setColor(new Color(100, 150, 255));
        g.drawString("BLUE TEAM", boardX + 20, yPos);
        yPos += 30;
        
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        for (PlayerInfo info : blueTeam) {
            boolean isMe = info.name.equals(ctx.playerName);
            if (isMe) {
                g.setColor(new Color(255, 255, 0, 100));
                g.fillRect(boardX + 10, yPos - 18, boardWidth - 20, 25);
            }
            
            // 캐릭터 아이콘
            drawCharacterIcon(g, info.characterId, boardX + 20, yPos - 16, 20);
            
            g.setColor(Color.WHITE);
            g.drawString(info.name, boardX + 60, yPos);
            g.drawString(getCharacterName(info.characterId), boardX + 250, yPos);
            
            // 궁극기 상태
            String ultStatus = getUltStatus(info, ctx);
            if ("READY".equals(ultStatus)) {
                g.setColor(new Color(0, 255, 0));
            } else {
                g.setColor(new Color(150, 150, 150));
            }
            g.drawString(ultStatus, boardX + 420, yPos);
            
            g.setColor(Color.WHITE);
            g.drawString(String.valueOf(info.kills), boardX + 520, yPos);
            g.drawString(String.valueOf(info.deaths), boardX + 600, yPos);
            float kd = info.deaths == 0 ? info.kills : (float)info.kills / info.deaths;
            g.drawString(String.format("%.2f", kd), boardX + 680, yPos);
            
            yPos += 30;
        }
        
        // 하단 안내
        g.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        g.setColor(new Color(200, 200, 200));
        String hint = "TAB 키를 떼면 닫힙니다";
        int hintX = boardX + (boardWidth - g.getFontMetrics().stringWidth(hint)) / 2;
        g.drawString(hint, hintX, boardY + boardHeight - 20);
    }
    
    private String getCharacterName(String characterId) {
        if (characterId == null) return "Unknown";
        return switch (characterId.toLowerCase()) {
            case "raven" -> "Raven";
            case "piper" -> "Piper";
            case "technician" -> "Technician";
            case "general" -> "General";
            default -> characterId;
        };
    }
    
    private String getUltStatus(PlayerInfo info, RenderContext ctx) {
        // 같은 팀만 궁극기 상태 표시
        if (info.team != ctx.team) {
            return "-";
        }
        
        // 자기 자신이면 실제 쿨타임 체크
        if (info.name.equals(ctx.playerName)) {
            if (ctx.abilities != null && ctx.abilities.length > 2) {
                Ability ult = ctx.abilities[2];
                return ult.canUse() ? "READY" : String.format("%.1fs", ult.getCurrentCooldown());
            }
        }
        
        // 다른 팀원은 간단히 표시 (실제 쿨타임 정보는 서버에서 동기화 필요)
        return "READY";
    }
    
    private static class PlayerInfo {
        String name;
        String characterId;
        int kills;
        int deaths;
        int team;
    }
    
    private boolean isOnScreen(int screenX, int screenY, RenderContext ctx) {
        return screenX >= -100 && screenX <= ctx.canvasWidth + 100 &&
               screenY >= -100 && screenY <= ctx.canvasHeight + 100;
    }
    
    /**
     * 렌더링에 필요한 모든 데이터를 담는 컨텍스트 클래스
     */
    public static class RenderContext {
        // 맵 데이터
        public Image mapImage;
        public int mapWidth;
        public int mapHeight;
        public int cameraX;
        public int cameraY;
        public List<Rectangle> obstacles;
        public boolean debugObstacles;
        
        // 플레이어 데이터
        public String playerName;
        public int team;
        public int playerX;
        public int playerY;
        public int myDirection;
        public SpriteAnimation[] myAnimations;
        public int myHP;
        public int myMaxHP;
        public int mouseX;
        public int mouseY;
        public String selectedCharacter;
        public CharacterData currentCharacterData;
        
        // 다른 플레이어들
        public Map<String, GamePanel.PlayerData> players;
        
        // 게임 오브젝트
        public List<GameObjectManager.Missile> missiles;
        public Map<Integer, GameObjectManager.PlacedObjectClient> placedObjects;
        public Map<Integer, GameObjectManager.StrikeMarker> strikeMarkers;
        
        // 이펙트
        public List<GamePanel.ActiveEffect> myEffects;
        public SkillEffectManager skillEffects;
        
        // UI 상태
        public boolean showMinimap;
        public boolean editMode;
        public boolean showScoreboard;
        public int kills;
        public int deaths;
        public Ability[] abilities;
        
        // 라운드 정보
        public int redWins;
        public int blueWins;
        public int roundCount;
        public GamePanel.RoundState roundState;
        public long roundStartTime;
        public String centerMessage;
        public long centerMessageEndTime;
        
        // Piper 스킬 상태
        public float piperMarkRemaining;
        public float piperThermalRemaining;
        public float teamMarkRemaining;
        public float teamThermalRemaining;
        
        // 캔버스 크기
        public int canvasWidth;  // 고정 렌더링 크기 (1280x720)
        public int canvasHeight; // 고정 렌더링 크기 (1280x720)
        public int actualCanvasWidth;  // 실제 윈도우 크기
        public int actualCanvasHeight; // 실제 윈도우 크기
        
        // 편집 모드 데이터
        public boolean[][] walkableGrid;
        public int tileSize;
        public int gridCols;
        public int gridRows;
    }
}
