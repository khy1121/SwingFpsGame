package com.fpsgame.client;

import com.fpsgame.common.Ability;
import com.fpsgame.common.CharacterData;
import com.fpsgame.common.GameConstants;
import com.fpsgame.client.effects.*;

import java.util.*;

/**
 * 게임 메시지 처리를 담당하는 핸들러 클래스
 * GamePanel의 processGameMessage를 분리하여 단일 책임 원칙 준수
 */
public class GameMessageHandler {
    
    private final GamePanel gamePanel;
    
    public GameMessageHandler(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }
    
    /**
     * 메시지 라우팅 - 각 메시지를 적절한 핸들러로 전달
     */
    public void handleMessage(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length < 2) return;
        
        String command = parts[0];
        String data = parts[1];
        
        switch (command) {
            case "CHAT" -> handleChat(data);
            case "CHARACTER_SELECT" -> handleCharacterSelect(data);
            case "PLAYER" -> handlePlayer(data);
            case "REMOVE" -> handleRemove(data);
            case "KILL" -> handleKill(data);
            case "STATS" -> handleStats(data);
            case "SKILL" -> handleSkill(data);
            case "MISSILE" -> handleMissile(data);
            case "HIT" -> handleHit(data);
            case "OBJ" -> handleObject(data);
            case "OBJ_DESTROY" -> handleObjectDestroy(data);
            case "BUFF" -> handleBuff(data);
            case "STRIKE" -> handleStrike(data);
            case "TURRET_SHOOT" -> handleTurretShoot(data);
            case "ROUND_END" -> handleRoundEnd(data);
            case "ROUND_START" -> handleRoundStart(data);
            case "GAME_END" -> handleGameEnd(data);
            default -> System.out.println("[알 수 없는 명령어] " + command);
        }
    }
    
    private void handleChat(String data) {
        gamePanel.appendChatMessage(data);
    }
    
    private void handleCharacterSelect(String data) {
        String[] cs = data.split(",");
        if (cs.length < 2) return;
        
        String pName = cs[0];
        String charId = cs[1];
        CharacterData cd = CharacterData.getById(charId);
        int newMaxHp = (int) cd.health;
        
        if (pName.equals(gamePanel.playerName)) {
            handleMyCharacterSelect(charId, cd, newMaxHp);
        } else {
            handleRemoteCharacterSelect(pName, charId, cd, newMaxHp);
        }
    }
    
    private void handleMyCharacterSelect(String charId, CharacterData cd, int newMaxHp) {
        // GameState에 캐릭터 정보 설정
        gamePanel.gameState.setSelectedCharacter(charId);
        gamePanel.gameState.setCurrentCharacterData(cd);
        gamePanel.gameState.setMyMaxHP(newMaxHp);
        gamePanel.gameState.setMyHP(newMaxHp);
        
        // 스킬 재생성 (로컬 유지)
        gamePanel.abilities = CharacterData.createAbilities(charId);
        gamePanel.gameState.setAbilities(gamePanel.abilities);
        gamePanel.hasChangedCharacterInRound = true;
        
        // 선택한 캐릭터 저장
        GameConfig.saveCharacter(charId);
        
        // 스프라이트 재로드
        gamePanel.loadSprites();
        System.out.println("[CHARACTER_SELECT] 캐릭터 변경: " + charId + 
            ", maxHP: " + newMaxHp + ", 현재 HP: " + gamePanel.gameState.getMyHP() + ")");
        gamePanel.repaint();
        gamePanel.appendChatMessage("[캐릭터] " + cd.name + "으로 변경되었습니다.");
    }
    
    private void handleRemoteCharacterSelect(String pName, String charId, CharacterData cd, int newMaxHp) {
        GamePanel.PlayerData pd = gamePanel.players.get(pName);
        if (pd == null) {
            pd = gamePanel.new PlayerData(0, 0, GameConstants.TEAM_RED);
            pd.characterId = charId;
            pd.maxHp = newMaxHp;
            pd.hp = newMaxHp;
            gamePanel.players.put(pName, pd);
        } else {
            pd.characterId = charId;
            pd.maxHp = newMaxHp;
            pd.hp = newMaxHp;
        }
        gamePanel.loadPlayerSprites(pd, charId);
        System.out.println("[CHARACTER_SELECT] 원격 플레이어: " + pName + " -> " + charId + ", HP: " + newMaxHp);
        gamePanel.appendChatMessage("[캐릭터] " + pName + " -> " + cd.name);
    }
    
    private void handlePlayer(String data) {
        String[] playerData = data.split(",");
        if (playerData.length < 5) return;
        
        String name = playerData[0];
        if (name.equals(gamePanel.playerName)) return;
        
        int x = (int) Float.parseFloat(playerData[1]);
        int y = (int) Float.parseFloat(playerData[2]);
        int t = Integer.parseInt(playerData[3]);
        int hp = Integer.parseInt(playerData[4]);
        String charId = (playerData.length >= 6) ? playerData[5] : "raven";
        int direction = (playerData.length >= 7) ? Integer.parseInt(playerData[6]) : 0;
        
        GamePanel.PlayerData pd = gamePanel.players.get(name);
        if (pd == null) {
            pd = gamePanel.new PlayerData(x, y, t);
            pd.hp = hp;
            pd.characterId = charId;
            pd.maxHp = (int) CharacterData.getById(charId).health;
            pd.direction = direction;
            gamePanel.players.put(name, pd);
            gamePanel.loadPlayerSprites(pd, charId);
            System.out.println("[PLAYER] 새 플레이어: " + name + " (Team " + t + ") at (" + x + ", " + y + ")");
        } else {
            pd.targetX = x;
            pd.targetY = y;
            pd.team = t;
            pd.hp = hp;
            pd.direction = direction;
            
            // 캐릭터 변경 감지 시 스프라이트 재로드
            if (charId != null && (!charId.equalsIgnoreCase(pd.characterId) || pd.animations == null)) {
                pd.characterId = charId;
                pd.maxHp = (int) CharacterData.getById(charId).health;
                gamePanel.loadPlayerSprites(pd, charId);
                System.out.println("[PLAYER] 캐릭터 변경: " + name + " -> " + charId + ", maxHP: " + pd.maxHp);
            } else if (charId != null) {
                pd.maxHp = (int) CharacterData.getById(charId).health;
            }
        }
    }
    
    private void handleRemove(String data) {
        gamePanel.players.remove(data);
    }
    
    private void handleKill(String data) {
        gamePanel.appendChatMessage(">>> 당신이 " + data + "를 처치했습니다!");
    }
    
    private void handleStats(String data) {
        String[] s = data.split(",");
        if (s.length < 4) return;
        
        String name = s[0];
        int k = Integer.parseInt(s[1]);
        int d = Integer.parseInt(s[2]);
        int hp = Integer.parseInt(s[3]);
        
        if (name.equals(gamePanel.playerName)) {
            handleMyStats(k, d, hp);
        } else {
            handleRemoteStats(name, k, d, hp);
        }
    }
    
    private void handleMyStats(int kills, int deaths, int hp) {
        gamePanel.gameState.setKills(kills);
        gamePanel.gameState.setDeaths(deaths);
        gamePanel.gameState.setMyHP(hp);
        
        CharacterData currentChar = gamePanel.gameState.getCurrentCharacterData();
        if (currentChar != null) {
            gamePanel.gameState.setMyMaxHP((int) currentChar.health);
        }
        
        System.out.println("[STATS] " + gamePanel.playerName + " HP: " + 
            gamePanel.gameState.getMyHP() + "/" + gamePanel.gameState.getMyMaxHP() + 
            " (Character: " + gamePanel.gameState.getSelectedCharacter() + ")");
        
        if (gamePanel.gameState.getMyHP() <= 0) {
            gamePanel.respawn();
        }
    }
    
    private void handleRemoteStats(String name, int kills, int deaths, int hp) {
        GamePanel.PlayerData pd = gamePanel.players.get(name);
        if (pd != null) {
            pd.kills = kills;
            pd.deaths = deaths;
            pd.hp = hp;
            
            if (pd.characterId != null) {
                pd.maxHp = (int) CharacterData.getById(pd.characterId).health;
            }
            
            System.out.println("[STATS] " + name + " HP: " + pd.hp + "/" + pd.maxHp + 
                " (Character: " + pd.characterId + ")");
        }
    }
    
    private void handleSkill(String data) {
        // SKILL:user,abilityId,type,duration 형식
        String[] sd = data.split(",");
        if (sd.length < 4) return;
        
        String user = sd[0];
        String abilityId = sd[1];
        String type = sd[2];
        float duration = 0f;
        try {
            duration = Float.parseFloat(sd[3]);
        } catch (NumberFormatException ignored) {}
        
        if (!user.equals(gamePanel.playerName)) {
            gamePanel.effectsByPlayer
                .computeIfAbsent(user, k -> new ArrayList<>())
                .add(new GamePanel.ActiveEffect(abilityId, type, Math.max(0.2f, duration)));
            
            // 구조화된 스킬 이펙트 등록
            addSkillEffect(user, abilityId, duration);
        }
    }
    
    private void addSkillEffect(String user, String abilityId, float duration) {
        switch (abilityId) {
            case "piper_mark" -> gamePanel.skillEffects.addForPlayer(user, new PiperMarkEffect(duration));
            case "piper_thermal" -> gamePanel.skillEffects.addForPlayer(user, new PiperThermalEffect(duration));
            case "raven_dash" -> gamePanel.skillEffects.addForPlayer(user, new RavenDashEffect(duration));
            case "raven_overcharge" -> gamePanel.skillEffects.addForPlayer(user, new RavenOverchargeEffect(duration));
            case "gen_aura" -> gamePanel.skillEffects.addForPlayer(user, new GeneralAuraEffect(duration));
            case "gen_strike" -> {} // 시각 효과 없음
            case "ghost_cloak" -> gamePanel.skillEffects.addForPlayer(user, new GhostCloakEffect(duration));
            case "ghost_nullify" -> gamePanel.skillEffects.addForPlayer(user, new GhostNullifyEffect(duration));
            case "skull_adrenaline" -> gamePanel.skillEffects.addForPlayer(user, new SkullAdrenalineEffect(duration));
            case "skull_ammo" -> gamePanel.skillEffects.addForPlayer(user, new SkullAmmoEffect(duration));
            case "tech_mine" -> gamePanel.skillEffects.addForPlayer(user, new TechMineEffect(duration));
            case "tech_turret" -> gamePanel.skillEffects.addForPlayer(user, new TechTurretEffect(duration));
            case "sage_heal" -> gamePanel.skillEffects.addForPlayer(user, new SageHealEffect(duration));
            case "sage_revive" -> gamePanel.skillEffects.addForPlayer(user, new SageReviveEffect(duration));
        }
    }
    
    private void handleMissile(String data) {
        String[] md = data.split(",");
        if (md.length >= 6) {
            int mx = Integer.parseInt(md[0]);
            int my = Integer.parseInt(md[1]);
            int dx = Integer.parseInt(md[2]);
            int dy = Integer.parseInt(md[3]);
            int mTeam = Integer.parseInt(md[4]);
            String owner = md[5];
            gamePanel.missiles.add(gamePanel.new Missile(mx, my, dx, dy, mTeam, owner));
        }
    }
    
    private void handleHit(String data) {
        String[] hd = data.split(",");
        if (hd.length >= 3) {
            int hitX = Integer.parseInt(hd[0]);
            int hitY = Integer.parseInt(hd[1]);
            int damage = Integer.parseInt(hd[2]);
            gamePanel.appendChatMessage("적중! 피해: " + damage + " at (" + hitX + ", " + hitY + ")");
        }
    }
    
    private void handleObject(String data) {
        String[] od = data.split(",");
        if (od.length >= 8) {
            int id = Integer.parseInt(od[0]);
            String objType = od[1];
            int ox = Integer.parseInt(od[2]);
            int oy = Integer.parseInt(od[3]);
            int hp = Integer.parseInt(od[4]);
            int maxHp = Integer.parseInt(od[5]);
            String ownerName = od[6];
            int team = Integer.parseInt(od[7]);
            gamePanel.placedObjects.put(id, gamePanel.new PlacedObjectClient(id, objType, ox, oy, hp, maxHp, ownerName, team));
        }
    }
    
    private void handleObjectDestroy(String data) {
        try {
            int id = Integer.parseInt(data);
            GamePanel.PlacedObjectClient obj = gamePanel.placedObjects.remove(id);
            if (obj != null) {
                gamePanel.appendChatMessage("[오브젝트] " + obj.type + " 파괴됨!");
            }
        } catch (NumberFormatException ignored) {}
    }
    
    private void handleBuff(String data) {
        String[] bd = data.split(",");
        if (bd.length >= 3) {
            String buffType = bd[0];
            float moveMulti = Float.parseFloat(bd[1]);
            float attackMulti = Float.parseFloat(bd[2]);
            gamePanel.moveSpeedMultiplier = moveMulti;
            gamePanel.attackSpeedMultiplier = attackMulti;
            if (moveMulti > 1.0f || attackMulti > 1.0f) {
                gamePanel.appendChatMessage("[버프] " + buffType + " 활성화!");
            }
        }
    }
    
    private void handleStrike(String data) {
        String[] strikeData = data.split(",");
        if (strikeData.length >= 3) {
            int strikeId = Integer.parseInt(strikeData[0]);
            int sx = Integer.parseInt(strikeData[1]);
            int sy = Integer.parseInt(strikeData[2]);
            gamePanel.strikeMarkers.put(strikeId, gamePanel.new StrikeMarker(strikeId, sx, sy));
            gamePanel.appendChatMessage("[경고] 에어스트라이크 발동! (" + sx + ", " + sy + ")");
        }
    }
    
    private void handleTurretShoot(String data) {
        String[] ts = data.split(",");
        if (ts.length >= 4) {
            int turretId = Integer.parseInt(ts[0]);
            int tx = Integer.parseInt(ts[1]);
            int ty = Integer.parseInt(ts[2]);
            
            GamePanel.PlacedObjectClient turret = gamePanel.placedObjects.get(turretId);
            if (turret != null) {
                int sx = turret.x;
                int sy = turret.y;
                int dx = tx - sx;
                int dy = ty - sy;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance > 0) {
                    int speed = 8;
                    int missileVx = (int) (dx / distance * speed);
                    int missileVy = (int) (dy / distance * speed);
                    gamePanel.missiles.add(gamePanel.new Missile(sx, sy, missileVx, missileVy, 
                        gamePanel.team, turret.owner));
                }
            }
        }
    }
    
    private void handleRoundEnd(String data) {
        String[] rd = data.split(",");
        if (rd.length >= 3) {
            String winTeam = rd[0];
            gamePanel.redWins = Integer.parseInt(rd[1]);
            gamePanel.blueWins = Integer.parseInt(rd[2]);
            gamePanel.roundState = GamePanel.RoundState.ENDED;
            gamePanel.appendChatMessage("[라운드] " + winTeam + " 팀 승리! (점수: " + 
                gamePanel.redWins + " : " + gamePanel.blueWins + ")");
        }
    }
    
    private void handleRoundStart(String data) {
        String[] mainParts = data.split(";", 2);
        if (mainParts.length < 1) return;
        
        // 라운드 정보 파싱
        String[] roundParts = mainParts[0].split(",");
        gamePanel.roundCount = Integer.parseInt(roundParts[0]);
        
        // 맵 변경 처리
        if (roundParts.length > 1) {
            handleMapChange(roundParts[1]);
        }
        
        // 플레이어 정보 초기화
        if (mainParts.length > 1) {
            handlePlayerInitialization(mainParts[1]);
        }
        
        // 라운드 시작
        gamePanel.roundState = GamePanel.RoundState.WAITING;
        gamePanel.roundStartTime = System.currentTimeMillis();
        gamePanel.centerMessage = "Round " + gamePanel.roundCount + " Ready";
        gamePanel.centerMessageEndTime = gamePanel.roundStartTime + GamePanel.ROUND_READY_TIME;
        gamePanel.hasChangedCharacterInRound = false;
        
        gamePanel.appendChatMessage("[라운드 " + gamePanel.roundCount + "] 10초 후 시작!");
    }
    
    private void handleMapChange(String newMapId) {
        if (!newMapId.equals(gamePanel.currentMapName)) {
            new Thread(() -> {
                try {
                    System.out.println("[맵] 로딩 시작: " + newMapId);
                    gamePanel.currentMapName = newMapId;
                    gamePanel.loadMap(newMapId);
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        gamePanel.appendChatMessage("[맵] " + newMapId + " 맵으로 변경되었습니다!");
                    });
                    System.out.println("[맵] 로딩 완료: " + newMapId);
                } catch (Exception e) {
                    System.err.println("[맵] 로딩 실패: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }, "MapLoader-Thread").start();
        }
    }
    
    private void handlePlayerInitialization(String playerInfoData) {
        if (playerInfoData == null || playerInfoData.trim().isEmpty()) {
            System.err.println("[ERROR] handlePlayerInitialization: playerInfoData is null or empty");
            return;
        }
        
        String[] playerInfo = playerInfoData.split(";");
        if (playerInfo.length < 1) {
            System.err.println("[ERROR] handlePlayerInitialization: playerInfo.length < 1");
            return;
        }
        
        int playerCount = Integer.parseInt(playerInfo[0]);
        
        System.out.println("[ROUND_START] Parsing " + playerCount + " players from server");
        
        for (int i = 1; i <= playerCount && i < playerInfo.length; i++) {
            String[] pData = playerInfo[i].split(",");
            if (pData.length >= 4) {
                String pName = pData[0];
                String pCharId = pData[1];
                
                if (pName == null || pName.trim().isEmpty()) {
                    System.err.println("[ERROR] handlePlayerInitialization: pName is null or empty at index " + i);
                    continue;
                }
                if (pCharId == null || pCharId.trim().isEmpty()) {
                    System.err.println("[ERROR] handlePlayerInitialization: pCharId is null or empty for " + pName);
                    continue;
                }
                
                try {
                    int pHp = Integer.parseInt(pData[2]);
                    int pMaxHp = Integer.parseInt(pData[3]);
                    
                    System.out.println("[ROUND_START] Player: " + pName + ", Char: " + pCharId + 
                        ", HP: " + pHp + "/" + pMaxHp);
                    
                    if (pName.equals(gamePanel.playerName)) {
                        initializeMyCharacter(pCharId, pHp, pMaxHp);
                    } else {
                        initializeRemotePlayer(pName, pCharId, pHp, pMaxHp);
                    }
                } catch (NumberFormatException ex) {
                    System.err.println("[ERROR] handlePlayerInitialization: Invalid HP values for " + pName);
                    ex.printStackTrace();
                }
            }
        }
    }
    
    private void initializeMyCharacter(String charId, int hp, int maxHp) {
        if (charId == null || charId.trim().isEmpty()) {
            System.err.println("[ERROR] initializeMyCharacter: charId is null or empty");
            return;
        }
        
        CharacterData cd = CharacterData.getById(charId);
        if (cd == null) {
            System.err.println("[ERROR] initializeMyCharacter: CharacterData not found for " + charId);
            return;
        }
        
        gamePanel.gameState.setSelectedCharacter(charId);
        gamePanel.gameState.setCurrentCharacterData(cd);
        gamePanel.gameState.setMyHP(hp);
        gamePanel.gameState.setMyMaxHP(maxHp);
        
        gamePanel.loadSprites();
        
        gamePanel.abilities = CharacterData.createAbilities(charId);
        gamePanel.gameState.setAbilities(gamePanel.abilities);
        if (gamePanel.abilities != null) {
            for (Ability ability : gamePanel.abilities) {
                if (ability != null) {
                    ability.resetCooldown();
                }
            }
        }
        
        System.out.println("[ROUND_START] My character initialized: " + charId + 
            " HP: " + gamePanel.gameState.getMyHP() + "/" + gamePanel.gameState.getMyMaxHP() + ")");
    }
    
    private void initializeRemotePlayer(String name, String charId, int hp, int maxHp) {
        if (name == null || name.trim().isEmpty()) {
            System.err.println("[ERROR] initializeRemotePlayer: name is null or empty");
            return;
        }
        if (charId == null || charId.trim().isEmpty()) {
            System.err.println("[ERROR] initializeRemotePlayer: charId is null or empty for " + name);
            return;
        }
        
        GamePanel.PlayerData pd = gamePanel.players.get(name);
        if (pd != null) {
            pd.characterId = charId;
            pd.hp = hp;
            pd.maxHp = maxHp;
            gamePanel.loadPlayerSprites(pd, charId);
            System.out.println("[ROUND_START] Remote player updated: " + name + 
                " -> " + charId + " HP: " + hp + "/" + maxHp);
        } else {
            System.err.println("[ERROR] initializeRemotePlayer: PlayerData not found for " + name);
        }
    }
    
    private void handleGameEnd(String data) {
        gamePanel.appendChatMessage("========================================");
        gamePanel.appendChatMessage("[게임 종료] " + data + " 팀이 최종 승리했습니다!");
        gamePanel.appendChatMessage("========================================");
        
        javax.swing.Timer returnTimer = new javax.swing.Timer(5000, e -> gamePanel.returnToLobby());
        returnTimer.setRepeats(false);
        returnTimer.start();
    }
}
