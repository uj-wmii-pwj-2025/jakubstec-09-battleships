import java.util.*;

public class Board {
    // --- constants ---
    private static final int SIZE = 10;
    private static final char SHIP = '#';
    private static final char WATER = '.';
    private static final char HIT_SHIP = '@';
    private static final char MISS = '~';
    
    // pomocnicze kierunki do sprawdzania sasiadow
    private static final int[][] ALL_AROUND = {
        {-1, 0}, {1, 0}, {0, -1}, {0, 1},
        {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    // --- stany planszy ---
    private final char[][] grid = new char[SIZE][SIZE];
    private final List<Ship> ships = new ArrayList<>();
    private final Set<String> enemyShotsHistory = new HashSet<>(); 
    private final char[][] enemyGridKnowledge = new char[SIZE][SIZE];

    // --- wewnetrzne klasy ---
    private class Ship {
        List<Point> coords = new ArrayList<>();
        boolean isSunk() {
            for (Point p : coords) {
                if (grid[p.r][p.c] == SHIP) return false;
            }
            return true;
        }
    }
    private record Point(int r, int c) {}

    // --- setup i inicjalizacja ---
    public Board(String mapString) {
        String cleanMap = mapString.trim().replace("\n", "").replace("\r", "");
        
        if (cleanMap.length() != SIZE * SIZE) {
            cleanMap = cleanMap.replace(" ", "");
        }
        for (int i = 0; i < SIZE * SIZE; i++) {
            int r = i / SIZE;
            int c = i % SIZE;
            grid[r][c] = cleanMap.charAt(i);
            enemyGridKnowledge[r][c] = '.'; 
        }
        detectShips();
    }

    private void detectShips() {
        boolean[][] visited = new boolean[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == SHIP && !visited[r][c]) {
                    Ship ship = new Ship();
                    floodFill(r, c, visited, ship);
                    ships.add(ship);
                }
            }
        }
    }

    private void floodFill(int r, int c, boolean[][] visited, Ship ship) {
        if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) return;
        if (visited[r][c] || grid[r][c] != SHIP) return;

        visited[r][c] = true;
        ship.coords.add(new Point(r, c));

        floodFill(r + 1, c, visited, ship);
        floodFill(r - 1, c, visited, ship);
        floodFill(r, c + 1, visited, ship);
        floodFill(r, c - 1, visited, ship);
    }

    public String receiveShotAndGenerateResponse(String enemyCoordStr, String myNextMoveCoord) {
        Point p = parseCoord(enemyCoordStr);
        String result;

        // handling strzalow w to samo miejsce
        if (enemyShotsHistory.contains(enemyCoordStr)) {
            result = getResultForAlreadyHit(p);
        } else {
            enemyShotsHistory.add(enemyCoordStr);
            result = processNewShot(p);
        }
        return result + ";" + myNextMoveCoord;
    }

    private String getResultForAlreadyHit(Point p) {
        char current = grid[p.r][p.c];
        if (current == WATER || current == MISS) return "pudło";
        
        Ship s = findShipAt(p);
        if (s != null && s.isSunk()) return "trafiony zatopiony";
        return "trafiony";
    }

    private String processNewShot(Point p) {
        if (grid[p.r][p.c] == WATER) {
            grid[p.r][p.c] = MISS;
            return "pudło";
        } else if (grid[p.r][p.c] == SHIP) {
            grid[p.r][p.c] = HIT_SHIP;
            Ship ship = findShipAt(p);
            
            if (ship.isSunk()) {
                if (areAllShipsSunk()) return "ostatni zatopiony";
                return "trafiony zatopiony";
            } else {
                return "trafiony";
            }
        }
        return "pudło";
    }

    public void processMyShotResult(String myShotCoord, String resultMsg) {
        Point p = parseCoord(myShotCoord);
        if (resultMsg.contains("pudło")) {
            enemyGridKnowledge[p.r][p.c] = MISS;
        } else if (resultMsg.contains("trafiony")) {
            enemyGridKnowledge[p.r][p.c] = HIT_SHIP;
            
            if (resultMsg.contains("zatopiony")) {
                markSunkShipSurroundings(p.r, p.c);
            }
        }
    }

    public String generateShot() {
        System.out.println("\n--- TWOJA KOLEJ ---");
        printEnemyMap(false); 

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("podaj koordynaty (A-J, 1-10; np A5): ");
                
                if (!scanner.hasNextLine()) {
                    return "A1"; 
                }

                String input = scanner.nextLine().trim().toUpperCase();

                if (input.matches("^[A-J]([1-9]|10)$")) {
                    return input;
                } else {
                    System.out.println("bledny format.");
                }
            }
        }
    }

    // --- pomocnicze metody ---
    private Ship findShipAt(Point p) {
        for (Ship s : ships) {
            for (Point sp : s.coords) {
                if (sp.r == p.r && sp.c == p.c) return s;
            }
        }
        return null;
    }

    private boolean areAllShipsSunk() {
        for (Ship s : ships) {
            if (!s.isSunk()) return false;
        }
        return true;
    }

    private Point parseCoord(String c) {
        return new Point(c.toUpperCase().charAt(0) - 'A', Integer.parseInt(c.substring(1)) - 1);
    }

    private void markSunkShipSurroundings(int startR, int startC) {
        Set<String> shipParts = new HashSet<>();
        Queue<Point> queue = new LinkedList<>();
        
        Point start = new Point(startR, startC);
        queue.add(start);
        shipParts.add(startR + "," + startC);

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        List<Point> confirmedShipParts = new ArrayList<>();
        confirmedShipParts.add(start);

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            
            for (int[] dir : directions) {
                int nr = current.r + dir[0];
                int nc = current.c + dir[1];
                
                if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE) {
                    if (enemyGridKnowledge[nr][nc] == HIT_SHIP) {
                        String key = nr + "," + nc;
                        if (!shipParts.contains(key)) {
                            shipParts.add(key);
                            Point nextPart = new Point(nr, nc);
                            confirmedShipParts.add(nextPart);
                            queue.add(nextPart);
                        }
                    }
                }
            }
        }

        for (Point part : confirmedShipParts) {
            for (int[] dir : ALL_AROUND) {
                int nr = part.r + dir[0];
                int nc = part.c + dir[1];

                if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE) {
                    if (enemyGridKnowledge[nr][nc] != HIT_SHIP) {
                        enemyGridKnowledge[nr][nc] = MISS;
                    }
                }
            }
        }
    }

    // --- wypisywanie ---
    public void printMyMap() {
        System.out.println("Moja mapa:");
        printGrid(grid);
    }

    public void printEnemyMap(boolean iWon) {
        System.out.println("Mapa przeciwnika:");
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                char k = enemyGridKnowledge[r][c];
                
                if (k == HIT_SHIP) {
                    System.out.print(iWon ? SHIP : HIT_SHIP); 
                } 
                else if (k == MISS) {
                    System.out.print(MISS);
                } 
                else {
                    System.out.print(iWon ? '.' : '?'); 
                }
            }
            System.out.println();
        }
    }

    private void printGrid(char[][] g) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) System.out.print(g[r][c]);
            System.out.println();
        }
    }
}