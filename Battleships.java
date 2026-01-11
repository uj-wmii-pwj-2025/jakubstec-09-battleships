import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Battleships {

    // --- config ---
    private static String mode;
    private static int port;
    private static String mapFile;
    private static String host;

    public static void main(String[] args) {
        parseArgs(args);

        try {
            // przygotowanie mapy
            String mapContent = prepareMap();
            Board board = new Board(mapContent);
            board.printMyMap();

            // nawiazanie polaczenia
            try (Socket socket = establishConnection()) {
                socket.setSoTimeout(60000); 
                playGame(socket, board);
            }

        } catch (Exception e) {
            System.err.println("blad: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- input/output + setup ---
    
    private static String prepareMap() throws IOException {
        Path path = Path.of(mapFile);
        if (Files.exists(path)) {
            return Files.readString(path);
        } else {
            System.out.println("brak pliu mapy, generowanie...");
            BattleshipBoardGenerator generator = new BattleshipBoardGenerator();
            String content = generator.generateMap();
            Files.writeString(path, content);
            System.out.println("wygenerowano i zapisano do: " + mapFile);
            return content;
        }
    }

    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-mode" -> mode = args[++i];
                case "-port" -> port = Integer.parseInt(args[++i]);
                case "-map"  -> mapFile = args[++i];
                case "-host" -> host = args[++i];
            }
        }
        if (mode == null || port == 0 || mapFile == null) {
            System.out.println("uzycie: -mode [server|client] -port N -map file [-host host]");
            System.exit(1);
        }
    }

    // --- socket setup ---

    private static Socket establishConnection() throws IOException {
        if ("server".equalsIgnoreCase(mode)) {
            System.out.println("Serwer startuje na porcie " + port);
            try (ServerSocket ss = new ServerSocket(port)) { 
                return ss.accept(); 
            }
        } else {
            System.out.println("Klient laczy sie do " + host + ":" + port);
            return new Socket(host, port);
        }
    }

    // --- game loop ---

    private static void playGame(Socket socket, Board board) throws IOException {
        PrintWriter out = new PrintWriter(
            new OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), 
            true
        );
        BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)
        );
        String myLastShotCoord = null;
        String lastMessageSent = null;
        int errorCount = 0;

        // inicjalizacja gry przez klienta
        if ("client".equalsIgnoreCase(mode)) {
            myLastShotCoord = board.generateShot();
            lastMessageSent = "start;" + myLastShotCoord;
            send(out, lastMessageSent);
        }

        boolean gameRunning = true;
        while (gameRunning) {
            try {
                String received = in.readLine();
                if (received == null) throw new IOException("rozlaczono");

                String[] parts = received.split(";");
                String enemyResultInfo = parts[0];
                String enemyAttackCoord = "";
                
                if (parts.length > 1) {
                    enemyAttackCoord = parts[parts.length - 1];
                }

                if (!enemyResultInfo.equals("start")) {
                    System.out.println("WYNIK TWOJEGO STRZAŁU W " + myLastShotCoord + ": " + enemyResultInfo.toUpperCase());
                    board.processMyShotResult(myLastShotCoord, enemyResultInfo);
                }

                if (received.startsWith("ostatni zatopiony")) {
                    board.processMyShotResult(myLastShotCoord, "ostatni zatopiony");
                    System.out.println("\n--- WYGRANA! ---");
                    board.printEnemyMap(true);
                    System.out.println();
                    board.printMyMap();
                    return;
                }

                System.out.println(">> PRZECIWNIK STRZELA W: " + enemyAttackCoord);
                
                String myNextShot = board.generateShot();
                
                String response = board.receiveShotAndGenerateResponse(enemyAttackCoord, myNextShot);

                if (response.startsWith("ostatni zatopiony")) {
                    out.println(response);
                    System.out.println("TRAFIONY OSTATNI STATEK! PRZEGRANA.");
                    board.printEnemyMap(false);
                    System.out.println();
                    board.printMyMap();
                    return;
                }

                lastMessageSent = response;
                myLastShotCoord = myNextShot;
                out.println(lastMessageSent);

            } catch (IOException e) {
                errorCount++;
                System.out.println("blad polaczenia (" + errorCount + "/3). ponawiam...");
                if (errorCount >= 3) {
                    System.out.println("limit bledow. koniec.");
                    break;
                }
                if (lastMessageSent != null) send(out, lastMessageSent);
            }
        }
    }

    private static void send(PrintWriter out, String msg) {
        out.println(msg);
        System.out.println("wysylam: " + msg);
    }

}