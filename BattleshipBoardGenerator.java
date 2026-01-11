import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BattleshipBoardGenerator {

    private static final int SIZE = 10;
    private static final int MAX_RETRIES = 1000;

    private final Random rand = new Random();
    private final int[][] grid = new int[SIZE][SIZE];

    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };

    private static final int[][] ALL_AROUND = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    private static final int[][] SHIPS = {
            {4, 1},
            {3, 2},
            {2, 3},
            {1, 4}
    };

    public String generateMap() {
        for (int[] shipType : SHIPS) {
            int shipLength = shipType[0];
            int count = shipType[1];
            for (int i = 0; i < count; i++) {
                while (!placeShip(shipLength)) {
                }
            }
        }
        return toStringBoard();
    }

    private boolean placeShip(int length) {
        int attempts = 0;
        while (attempts++ < MAX_RETRIES) {
            int row = rand.nextInt(SIZE);
            int col = rand.nextInt(SIZE);
            int[] dir = DIRECTIONS[rand.nextInt(4)];

            if (canPlaceShip(row, col, dir, length)) {
                setShip(row, col, dir, length);
                return true;
            }
        }
        return false;
    }

    private boolean canPlaceShip(int row, int col, int[] dir, int length) {
        for (int i = 0; i < length; i++) {
            int r = row + dir[0] * i;
            int c = col + dir[1] * i;
            if (!isValid(r, c) || grid[r][c] != 0) return false;

            for (int[] delta : ALL_AROUND) {
                int nr = r + delta[0];
                int nc = c + delta[1];
                if (isValid(nr, nc) && grid[nr][nc] == 1)
                    return false;
            }
        }
        return true;
    }

    private void setShip(int row, int col, int[] dir, int length) {
        List<int[]> cells = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            int r = row + dir[0] * i;
            int c = col + dir[1] * i;
            grid[r][c] = 1;
            cells.add(new int[]{r, c});
        }

        for (int[] cell : cells) {
            for (int[] delta : ALL_AROUND) {
                int nr = cell[0] + delta[0];
                int nc = cell[1] + delta[1];
                if (isValid(nr, nc) && grid[nr][nc] == 0)
                    grid[nr][nc] = 2;
            }
        }
    }

    private boolean isValid(int r, int c) {
        return r >= 0 && r < SIZE && c >= 0 && c < SIZE;
    }

    private String toStringBoard() {
        StringBuilder sb = new StringBuilder(SIZE * SIZE);
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                sb.append(grid[r][c] == 1 ? '#' : '.');
            }
        }
        return sb.toString();
    }
}
