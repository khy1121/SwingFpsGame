package com.fpsgame.client;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * 맵 로딩, 장애물 관리, 에디터 모드를 담당하는 클래스
 * GamePanel에서 분리된 MapManager
 */
public class MapManager {
    
    // 맵 시스템
    private BufferedImage mapImage;
    private int mapWidth = 3200;
    private int mapHeight = 2400;
    String currentMapName = "map";
    
    // 타일 그리드
    private static final int TILE_SIZE = 32;
    private boolean[][] walkableGrid;
    private int gridCols, gridRows;
    
    private Rectangle redSpawnZone, blueSpawnZone;
    private final List<int[]> redSpawnTiles = new ArrayList<>();
    private final List<int[]> blueSpawnTiles = new ArrayList<>();
    
    // 장애물 시스템
    private final List<Rectangle> obstacles = new ArrayList<>();
    
    // 맵 순환 목록
    private List<String> mapCycle = new ArrayList<>();
    private int mapIndex = 0;
    
    // 에디터 모드
    private boolean editMode = false;
    private int hoverCol = -1, hoverRow = -1;
    private int paintState = -1;
    private int editPaintMode = 0;
    
    // 채팅 콜백 (메시지 출력용)
    private final MessageCallback messageCallback;
    
    @FunctionalInterface
    public interface MessageCallback {
        void appendMessage(String message);
    }
    
    public MapManager(MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
        rebuildMapCycle();
    }
    
    // ==================== Getters ====================
    
    public BufferedImage getMapImage() { return mapImage; }
    public int getMapWidth() { return mapWidth; }
    public int getMapHeight() { return mapHeight; }
    public String getCurrentMapName() { return currentMapName; }
    public boolean[][] getWalkableGrid() { return walkableGrid; }
    public int getGridCols() { return gridCols; }
    public int getGridRows() { return gridRows; }
    public Rectangle getRedSpawnZone() { return redSpawnZone; }
    public Rectangle getBlueSpawnZone() { return blueSpawnZone; }
    public List<int[]> getRedSpawnTiles() { return redSpawnTiles; }
    public List<int[]> getBlueSpawnTiles() { return blueSpawnTiles; }
    public List<Rectangle> getObstacles() { return obstacles; }
    public boolean isEditMode() { return editMode; }
    public int getHoverCol() { return hoverCol; }
    public int getHoverRow() { return hoverRow; }
    public static int getTileSize() { return TILE_SIZE; }
    
    public void setEditMode(boolean editMode) { this.editMode = editMode; }
    
    // ==================== 맵 로딩 ====================
    
    /**
     * 맵 로드 및 장애물 설정
     */
    public void loadMap(String mapName) {
        try {
            File mapFile = new File("assets/maps/" + mapName + ".png");
            if (mapFile.exists()) {
                mapImage = ImageIO.read(mapFile);
                if (mapImage != null) {
                    mapWidth = mapImage.getWidth();
                    mapHeight = mapImage.getHeight();
                    messageCallback.appendMessage("[시스템] 맵 로드 완료: " + mapName + " (" + mapWidth + "x" + mapHeight + ")");
                } else {
                    messageCallback.appendMessage("[시스템] 맵 이미지 읽기 실패, 기본 크기 사용");
                }
            } else {
                messageCallback.appendMessage("[시스템] 맵 파일 없음: " + mapFile.getAbsolutePath());
            }
        } catch (IOException e) {
            messageCallback.appendMessage("[시스템] 맵 로드 에러: " + e.getMessage());
        }
        
        // 그리드 초기화
        gridCols = Math.max(1, mapWidth / TILE_SIZE);
        gridRows = Math.max(1, mapHeight / TILE_SIZE);
        walkableGrid = new boolean[gridRows][gridCols];
        
        // JSON 로딩 시도
        boolean loadedFromJson = loadMapFromJsonIfAvailable(mapName);
        
        // JSON 없으면 이미지 분석
        if (!loadedFromJson) {
            setupObstacles(mapName);
        }
        
        // 스폰 구역 walkable 보장
        ensureSpawnZonesWalkable();
        
        if (redSpawnZone == null || blueSpawnZone == null) {
            messageCallback.appendMessage("[경고] 스폰 구역이 JSON에 정의되지 않았습니다!");
        }
    }
    
    /**
     * 맵 전환
     */
    public void switchMap(String newMapName) {
        currentMapName = newMapName;
        loadMap(newMapName);
        messageCallback.appendMessage("[맵 전환] " + newMapName);
    }
    
    /**
     * 다음 맵으로 순환
     */
    public void cycleNextMap() {
        if (mapCycle.isEmpty()) {
            rebuildMapCycle();
        }
        if (!mapCycle.isEmpty()) {
            mapIndex = (mapIndex + 1) % mapCycle.size();
            String nextMap = mapCycle.get(mapIndex);
            switchMap(nextMap);
        }
    }
    
    /**
     * 맵별 장애물 설정
     */
    private void setupObstacles(String mapName) {
        obstacles.clear();
        
        if ("map".equals(mapName) && mapImage != null) {
            extractObstaclesFromImage();
            for (int r = 0; r < gridRows; r++) {
                for (int c = 0; c < gridCols; c++) {
                    int cx = c * TILE_SIZE + TILE_SIZE / 2;
                    int cy = r * TILE_SIZE + TILE_SIZE / 2;
                    if (cx < mapWidth && cy < mapHeight) {
                        Color color = new Color(mapImage.getRGB(cx, cy));
                        walkableGrid[r][c] = isRoadColor(color) || isSpawnAreaColor(color);
                    }
                }
            }
        } else if ("terminal".equals(mapName)) {
            setupTerminalMap();
        }
    }
    
    private void setupTerminalMap() {
        int centerX = mapWidth / 2;
        int centerY = mapHeight / 2;
        obstacles.add(new Rectangle(centerX - 300, centerY - 200, 600, 400));
        
        int leftX = (int) (mapWidth * 0.15);
        obstacles.add(new Rectangle(leftX, (int) (mapHeight * 0.25), 150, 150));
        obstacles.add(new Rectangle(leftX, (int) (mapHeight * 0.42), 150, 150));
        
        int rightX = (int) (mapWidth * 0.80);
        obstacles.add(new Rectangle(rightX, (int) (mapHeight * 0.30), 150, 150));
        obstacles.add(new Rectangle(rightX, (int) (mapHeight * 0.47), 150, 150));
        
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                walkableGrid[r][c] = true;
            }
        }
        
        for (Rectangle obs : obstacles) {
            int c0 = Math.max(0, obs.x / TILE_SIZE);
            int c1 = Math.min(gridCols - 1, (obs.x + obs.width) / TILE_SIZE);
            int r0 = Math.max(0, obs.y / TILE_SIZE);
            int r1 = Math.min(gridRows - 1, (obs.y + obs.height) / TILE_SIZE);
            for (int r = r0; r <= r1; r++) {
                for (int c = c0; c <= c1; c++) {
                    walkableGrid[r][c] = false;
                }
            }
        }
    }
    
    // ==================== JSON 로딩 ====================
    
    private boolean loadMapFromJsonIfAvailable(String mapName) {
        String[] candidates = {
            "assets/maps/" + mapName + "_edited.json",
            "assets/maps/" + mapName + ".edited.json",
            "assets/maps/" + mapName + ".json"
        };
        
        for (String path : candidates) {
            File f = new File(path);
            if (f.exists()) {
                try {
                    String json = readFileAsString(f);
                    parseMapJson(json);
                    messageCallback.appendMessage("[JSON] 로드 성공: " + f.getName());
                    return true;
                } catch (IOException e) {
                    messageCallback.appendMessage("[JSON] 로드 실패: " + e.getMessage());
                }
            }
        }
        return false;
    }
    
    private String readFileAsString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
    
    private void parseMapJson(String json) {
        // meta.map_pixel_size 파싱
        Pattern pMW = Pattern.compile("\"map_pixel_size\"\\s*:\\s*\\{[^}]*\"w\"\\s*:\\s*(\\d+)");
        Pattern pMH = Pattern.compile("\"map_pixel_size\"\\s*:\\s*\\{[^}]*\"h\"\\s*:\\s*(\\d+)");
        Pattern pTS = Pattern.compile("\"tile_size\"\\s*:\\s*(\\d+)");
        
        Matcher mwMatch = pMW.matcher(json);
        Matcher mhMatch = pMH.matcher(json);
        Matcher tsMatch = pTS.matcher(json);
        
        int mw = mapWidth;
        int mh = mapHeight;
        int ts = TILE_SIZE;
        
        if (mwMatch.find()) mw = Integer.parseInt(mwMatch.group(1));
        if (mhMatch.find()) mh = Integer.parseInt(mhMatch.group(1));
        if (tsMatch.find()) ts = Integer.parseInt(tsMatch.group(1));
        
        mapWidth = mw;
        mapHeight = mh;
        
        gridCols = Math.max(1, mapWidth / ts);
        gridRows = Math.max(1, mapHeight / ts);
        walkableGrid = new boolean[gridRows][gridCols];
        
        // obstacles 파싱
        obstacles.clear();
        Pattern pObs = Pattern.compile("\"obstacles\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher obsMatch = pObs.matcher(json);
        if (obsMatch.find()) {
            String obsArr = obsMatch.group(1);
            Pattern pXY = Pattern.compile("\\{\\s*\"x\"\\s*:\\s*(\\d+)\\s*,\\s*\"y\"\\s*:\\s*(\\d+)\\s*\\}");
            Matcher xyMatch = pXY.matcher(obsArr);
            while (xyMatch.find()) {
                int tx = Integer.parseInt(xyMatch.group(1));
                int ty = Integer.parseInt(xyMatch.group(2));
                obstacles.add(new Rectangle(tx * ts, ty * ts, ts, ts));
            }
        }
        
        // walkableGrid 초기화
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                walkableGrid[r][c] = true;
            }
        }
        
        // 장애물 타일을 unwalkable로 설정
        for (Rectangle obs : obstacles) {
            int col = obs.x / ts;
            int row = obs.y / ts;
            if (row >= 0 && row < gridRows && col >= 0 && col < gridCols) {
                walkableGrid[row][col] = false;
            }
        }
        
        // 스폰 구역 파싱
        redSpawnTiles.clear();
        blueSpawnTiles.clear();
        redSpawnZone = extractSpawnZone(json, "red", redSpawnTiles);
        blueSpawnZone = extractSpawnZone(json, "blue", blueSpawnTiles);
    }
    
    private List<int[]> extractTileList(String json, String key) {
        List<int[]> result = new ArrayList<>();
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher m = p.matcher(json);
        if (m.find()) {
            String arr = m.group(1);
            Pattern pXY = Pattern.compile("\\{\\s*\"x\"\\s*:\\s*(\\d+)\\s*,\\s*\"y\"\\s*:\\s*(\\d+)\\s*\\}");
            Matcher xyMatch = pXY.matcher(arr);
            while (xyMatch.find()) {
                int x = Integer.parseInt(xyMatch.group(1));
                int y = Integer.parseInt(xyMatch.group(2));
                result.add(new int[]{x, y});
            }
        }
        return result;
    }
    
    private Rectangle extractSpawnZone(String json, String teamKey, List<int[]> tileStore) {
        Pattern pSpawn = Pattern.compile("\"" + teamKey + "\"\\s*:\\s*\\{([^}]+)\\}");
        Matcher spawnMatch = pSpawn.matcher(json);
        if (!spawnMatch.find()) return null;
        
        String spawnBlock = spawnMatch.group(1);
        Pattern pTiles = Pattern.compile("\"tiles\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher tilesMatch = pTiles.matcher(spawnBlock);
        if (!tilesMatch.find()) return null;
        
        String tilesArr = tilesMatch.group(1);
        Pattern pXY = Pattern.compile("\\{\\s*\"x\"\\s*:\\s*(\\d+)\\s*,\\s*\"y\"\\s*:\\s*(\\d+)\\s*\\}");
        Matcher xyMatch = pXY.matcher(tilesArr);
        
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        
        while (xyMatch.find()) {
            int x = Integer.parseInt(xyMatch.group(1));
            int y = Integer.parseInt(xyMatch.group(2));
            tileStore.add(new int[]{x, y});
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        
        if (tileStore.isEmpty()) return null;
        
        int x = minX * TILE_SIZE;
        int y = minY * TILE_SIZE;
        int w = (maxX - minX + 1) * TILE_SIZE;
        int h = (maxY - minY + 1) * TILE_SIZE;
        
        return new Rectangle(x, y, w, h);
    }
    
    // ==================== 맵 순환 ====================
    
    private void rebuildMapCycle() {
        Set<String> found = new LinkedHashSet<>();
        found.add("map");
        found.add("airport");
        
        File dir = new File("assets/maps");
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = f.getName();
                    if (name.endsWith("_edited.json")) {
                        found.add(name.replace("_edited.json", ""));
                    } else if (name.endsWith(".edited.json")) {
                        found.add(name.replace(".edited.json", ""));
                    } else if (name.endsWith(".json") && !name.startsWith("map_")) {
                        found.add(name.replace(".json", ""));
                    }
                }
            }
        }
        
        mapCycle = new ArrayList<>(found);
        mapIndex = mapCycle.indexOf(currentMapName);
        if (mapIndex < 0) mapIndex = 0;
    }
    
    // ==================== 장애물 추출 ====================
    
    private void extractObstaclesFromImage() {
        if (mapImage == null) return;
        
        boolean[][] visited = new boolean[gridRows][gridCols];
        
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                if (visited[r][c]) continue;
                
                int cx = c * TILE_SIZE + TILE_SIZE / 2;
                int cy = r * TILE_SIZE + TILE_SIZE / 2;
                
                if (cx >= mapWidth || cy >= mapHeight) {
                    visited[r][c] = true;
                    continue;
                }
                
                Color color = new Color(mapImage.getRGB(cx, cy));
                
                if (!isRoadColor(color) && !isSpawnAreaColor(color)) {
                    Rectangle rect = findMaxRectangle(walkableGrid, visited, r, c, gridRows, gridCols);
                    if (rect != null) {
                        obstacles.add(new Rectangle(
                            rect.x * TILE_SIZE,
                            rect.y * TILE_SIZE,
                            rect.width * TILE_SIZE,
                            rect.height * TILE_SIZE
                        ));
                    }
                }
            }
        }
    }
    
    private boolean isRoadColor(Color c) {
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue();
        return (r > 150 && g > 150 && b > 150) && Math.abs(r - g) < 30 && Math.abs(g - b) < 30;
    }
    
    private boolean isSpawnAreaColor(Color c) {
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue();
        boolean isRed = r > 180 && g < 100 && b < 100;
        boolean isBlue = b > 180 && r < 100 && g < 100;
        return isRed || isBlue;
    }
    
    private Rectangle findMaxRectangle(boolean[][] grid, boolean[][] visited,
                                       int startRow, int startCol, int rows, int cols) {
        int maxArea = 0;
        Rectangle bestRect = null;
        
        for (int width = 1; startCol + width <= cols; width++) {
            for (int height = 1; startRow + height <= rows; height++) {
                boolean valid = true;
                
                for (int r = startRow; r < startRow + height && valid; r++) {
                    for (int c = startCol; c < startCol + width && valid; c++) {
                        if (visited[r][c] || grid[r][c]) {
                            valid = false;
                        }
                    }
                }
                
                if (valid) {
                    int area = width * height;
                    if (area > maxArea) {
                        maxArea = area;
                        bestRect = new Rectangle(startCol, startRow, width, height);
                    }
                } else {
                    break;
                }
            }
        }
        
        if (bestRect != null) {
            for (int r = bestRect.y; r < bestRect.y + bestRect.height; r++) {
                for (int c = bestRect.x; c < bestRect.x + bestRect.width; c++) {
                    visited[r][c] = true;
                }
            }
        }
        
        return bestRect;
    }
    
    // ==================== 스폰 구역 ====================
    
    private void ensureSpawnZonesWalkable() {
        if (redSpawnZone != null) markZoneWalkableAndClearObstacles(redSpawnZone, redSpawnTiles);
        if (blueSpawnZone != null) markZoneWalkableAndClearObstacles(blueSpawnZone, blueSpawnTiles);
    }
    
    private void markZoneWalkableAndClearObstacles(Rectangle zone, List<int[]> tiles) {
        if (obstacles != null && !obstacles.isEmpty()) {
            obstacles.removeIf(o -> o.intersects(zone));
        }
        
        if (tiles != null) {
            for (int[] t : tiles) {
                int col = t[0];
                int row = t[1];
                if (row >= 0 && row < gridRows && col >= 0 && col < gridCols) {
                    walkableGrid[row][col] = true;
                }
            }
        }
    }
    
    // ==================== 에디터 모드 ====================
    
    public void updateHoverTile(int mapX, int mapY) {
        hoverCol = mapX / TILE_SIZE;
        hoverRow = mapY / TILE_SIZE;
    }
    
    public void startPaintAt(int mapX, int mapY) {
        int col = mapX / TILE_SIZE;
        int row = mapY / TILE_SIZE;
        if (row < 0 || row >= gridRows || col < 0 || col >= gridCols) return;
        
        boolean current = walkableGrid[row][col];
        paintState = current ? 0 : 1;
        applyEditAction(col, row, false);
    }
    
    public void continuePaintAt(int mapX, int mapY) {
        if (paintState == -1) return;
        int col = mapX / TILE_SIZE;
        int row = mapY / TILE_SIZE;
        if (row < 0 || row >= gridRows || col < 0 || col >= gridCols) return;
        applyEditAction(col, row, true);
    }
    
    public void stopPaint() {
        paintState = -1;
    }
    
    public void setEditPaintMode(int mode) {
        this.editPaintMode = mode;
    }
    
    public int getEditPaintMode() {
        return editPaintMode;
    }
    
    private void applyEditAction(int col, int row, boolean dragging) {
        if (editPaintMode == 0) {
            walkableGrid[row][col] = true;
        } else if (editPaintMode == 1) {
            walkableGrid[row][col] = false;
        } else if (editPaintMode == 2 && !dragging) {
            toggleSpawnTile(redSpawnTiles, col, row);
            recomputeSpawnZones();
        } else if (editPaintMode == 3 && !dragging) {
            toggleSpawnTile(blueSpawnTiles, col, row);
            recomputeSpawnZones();
        }
    }
    
    private void toggleSpawnTile(List<int[]> list, int col, int row) {
        if (isSpawnTile(list, col, row)) {
            removeSpawnTile(list, col, row);
        } else {
            list.add(new int[]{col, row});
        }
    }
    
    private boolean isSpawnTile(List<int[]> list, int col, int row) {
        for (int[] t : list) {
            if (t[0] == col && t[1] == row) return true;
        }
        return false;
    }
    
    private void removeSpawnTile(List<int[]> list, int col, int row) {
        list.removeIf(t -> t[0] == col && t[1] == row);
    }
    
    private void recomputeSpawnZones() {
        redSpawnZone = computeSpawnZoneFromTiles(redSpawnTiles);
        blueSpawnZone = computeSpawnZoneFromTiles(blueSpawnTiles);
    }
    
    private Rectangle computeSpawnZoneFromTiles(List<int[]> tiles) {
        if (tiles.isEmpty()) return null;
        
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        
        for (int[] t : tiles) {
            minX = Math.min(minX, t[0]);
            minY = Math.min(minY, t[1]);
            maxX = Math.max(maxX, t[0]);
            maxY = Math.max(maxY, t[1]);
        }
        
        return new Rectangle(
            minX * TILE_SIZE,
            minY * TILE_SIZE,
            (maxX - minX + 1) * TILE_SIZE,
            (maxY - minY + 1) * TILE_SIZE
        );
    }
    
    public void rebuildObstaclesFromWalkable() {
        obstacles.clear();
        boolean[][] visited = new boolean[gridRows][gridCols];
        
        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                if (!walkableGrid[r][c] && !visited[r][c]) {
                    Rectangle rect = findMaxRectangle(walkableGrid, visited, r, c, gridRows, gridCols);
                    if (rect != null) {
                        obstacles.add(new Rectangle(
                            rect.x * TILE_SIZE,
                            rect.y * TILE_SIZE,
                            rect.width * TILE_SIZE,
                            rect.height * TILE_SIZE
                        ));
                    }
                }
            }
        }
        
        messageCallback.appendMessage("[에디터] 장애물 재구성 완료: " + obstacles.size() + "개");
    }
    
    /**
     * 편집된 맵 JSON 저장
     */
    public void saveEditedMap() {
        String fileName = currentMapName + "_edited.json";
        File dir = new File("assets/maps");
        if (!dir.exists()) dir.mkdirs();
        
        File outFile = new File(dir, fileName);
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outFile), java.nio.charset.StandardCharsets.UTF_8))) {
            bw.write(generateEditedMapJson());
            bw.flush();
            messageCallback.appendMessage("[에디터] 저장 완료: " + outFile.getPath());
        } catch (IOException ex) {
            messageCallback.appendMessage("[에디터] 저장 실패: " + ex.getMessage());
        }
    }
    
    private String generateEditedMapJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"meta\": {\n");
        sb.append("    \"map_pixel_size\": { \"w\": ").append(mapWidth).append(", \"h\": ").append(mapHeight)
                .append(" },\n");
        sb.append("    \"tile_size\": ").append(TILE_SIZE).append("\n");
        sb.append("  },\n");
        sb.append("  \"obstacles\": [\n");
        
        for (int i = 0; i < obstacles.size(); i++) {
            Rectangle obs = obstacles.get(i);
            int tx = obs.x / TILE_SIZE;
            int ty = obs.y / TILE_SIZE;
            sb.append("    { \"x\": ").append(tx).append(", \"y\": ").append(ty).append(" }");
            if (i < obstacles.size() - 1) sb.append(",");
            sb.append("\n");
        }
        
        sb.append("  ],\n");
        sb.append("  \"spawns\": {\n");
        sb.append("    \"red\": {\n");
        sb.append("      \"tiles\": [\n");
        
        for (int i = 0; i < redSpawnTiles.size(); i++) {
            int[] t = redSpawnTiles.get(i);
            sb.append("        { \"x\": ").append(t[0]).append(", \"y\": ").append(t[1]).append(" }");
            if (i < redSpawnTiles.size() - 1) sb.append(",");
            sb.append("\n");
        }
        
        sb.append("      ]\n");
        sb.append("    },\n");
        sb.append("    \"blue\": {\n");
        sb.append("      \"tiles\": [\n");
        
        for (int i = 0; i < blueSpawnTiles.size(); i++) {
            int[] t = blueSpawnTiles.get(i);
            sb.append("        { \"x\": ").append(t[0]).append(", \"y\": ").append(t[1]).append(" }");
            if (i < blueSpawnTiles.size() - 1) sb.append(",");
            sb.append("\n");
        }
        
        sb.append("      ]\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}\n");
        
        return sb.toString();
    }
}
