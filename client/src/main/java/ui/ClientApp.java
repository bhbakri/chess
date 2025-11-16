package ui;

import client.ServerFacade;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import static ui.EscapeSequences.*;

public class ClientApp {

    enum Mode { PRELOGIN, POSTLOGIN }

    private final Scanner in = new Scanner(System.in);
    private final ServerFacade facade;
    private Mode mode = Mode.PRELOGIN;
    private String username;

    // Games from the last 'list' call, to map number -> game
    private List<ServerFacade.GameInfo> lastGames = new ArrayList<>();

    public ClientApp(int port) {
        this.facade = new ServerFacade(port);
    }

    public void run() {
        System.out.println(SET_TEXT_BOLD + "Welcome to 240 Chess" + RESET_TEXT_BOLD_FAINT);
        while (true) {
            try {
                if (mode == Mode.PRELOGIN) {
                    prelogin();
                } else {
                    postlogin();
                }
            } catch (Exception ex) {
                // Friendly error only (no stack trace / status codes)
                System.out.println(SET_TEXT_COLOR_RED + ex.getMessage() + RESET_TEXT_COLOR);
            }
        }
    }

    // ================= PRELOGIN =================

    private void prelogin() throws Exception {
        System.out.print(SET_TEXT_FAINT + "[prelogin] " + RESET_TEXT_BOLD_FAINT +
                "help | register | login | quit > ");
        var cmd = in.nextLine().trim().toLowerCase(Locale.ROOT);

        switch (cmd) {
            case "help" -> printPreloginHelp();

            case "register" -> {
                System.out.print("username: ");
                var u = in.nextLine().trim();
                System.out.print("password: ");
                var p = in.nextLine().trim();
                System.out.print("email   : ");
                var e = in.nextLine().trim();

                var auth = facade.register(u, p, e);
                username = auth.username();
                mode = Mode.POSTLOGIN;
                System.out.println("Registered & logged in as " + username + ".");
            }

            case "login" -> {
                System.out.print("username: ");
                var u = in.nextLine().trim();
                System.out.print("password: ");
                var p = in.nextLine().trim();

                var auth = facade.login(u, p);
                username = auth.username();
                mode = Mode.POSTLOGIN;
                System.out.println("Logged in as " + username + ".");
            }

            case "quit" -> {
                System.out.println("Bye!");
                System.exit(0);
            }

            default -> System.out.println("Unknown command. Type 'help'.");
        }
    }

    private void printPreloginHelp() {
        System.out.println("""
            Commands:
              help        Show this help
              register    Create a new account and log in
              login       Log in with an existing account
              quit        Exit the program
            """);
    }

    // ================= POSTLOGIN =================

    private void postlogin() throws Exception {
        System.out.print(SET_TEXT_FAINT + "[postlogin] " + RESET_TEXT_BOLD_FAINT +
                "help | create | list | play | observe | logout > ");
        var cmd = in.nextLine().trim().toLowerCase(Locale.ROOT);

        switch (cmd) {
            case "help" -> printPostloginHelp();
            case "logout" -> doLogout();
            case "create" -> doCreateGame();
            case "list" -> doListGames();
            case "play" -> doPlayGame();
            case "observe" -> doObserveGame();
            default -> System.out.println("Unknown command. Type 'help'.");
        }
    }

    private void printPostloginHelp() {
        System.out.println("""
            Commands:
              help        Show this help
              create      Create a new game
              list        List existing games
              play        Join a game as WHITE or BLACK (by number)
              observe     Observe a game (by number)
              logout      Log out to prelogin menu
            """);
    }

    private void doLogout() throws Exception {
        facade.logout();
        username = null;
        mode = Mode.PRELOGIN;
        lastGames.clear();
        System.out.println("Logged out.");
    }

    private void doCreateGame() throws Exception {
        System.out.print("Game name: ");
        var name = in.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Please enter a non-empty game name.");
            return;
        }
        int id = facade.createGame(name);
        System.out.println("Created game \"" + name + "\".");
    }

    private void doListGames() throws Exception {
        lastGames = facade.listGames();
        if (lastGames.isEmpty()) {
            System.out.println("No games exist yet. Use 'create' to make one.");
            return;
        }

        System.out.println(SET_TEXT_UNDERLINE + "Games" + RESET_TEXT_UNDERLINE);
        for (int i = 0; i < lastGames.size(); i++) {
            var g = lastGames.get(i);
            var white = g.whiteUsername() == null ? "-" : g.whiteUsername();
            var black = g.blackUsername() == null ? "-" : g.blackUsername();
            System.out.printf("%d) %s  [W: %s | B: %s]%n",
                    i + 1, g.gameName(), white, black);
        }
    }

    private void doPlayGame() throws Exception {
        if (lastGames.isEmpty()) {
            System.out.println("First run 'list' and choose a game number.");
            return;
        }

        int index = askGameIndex("Game number");
        var game = lastGames.get(index);

        System.out.print("Color (WHITE/BLACK): ");
        var color = in.nextLine().trim().toUpperCase(Locale.ROOT);
        if (!color.equals("WHITE") && !color.equals("BLACK")) {
            System.out.println("Please type WHITE or BLACK.");
            return;
        }

        facade.joinGame(game.gameID(), color);
        System.out.println("Joined \"" + game.gameName() + "\" as " + color + ".");

        // Draw initial board: black perspective
        BoardPrinter.drawInitial(color.equals("BLACK"));
    }

    private void doObserveGame() throws Exception {
        if (lastGames.isEmpty()) {
            System.out.println("First run 'list' and choose a game number.");
            return;
        }

        int index = askGameIndex("Game number");
        var game = lastGames.get(index);

        facade.joinGame(game.gameID(), null); // null color = observer
        System.out.println("Observing \"" + game.gameName() + "\".");

        BoardPrinter.drawInitial(false);
    }

    //helper

    private int askGameIndex(String prompt) {
        while (true) {
            try {
                System.out.print(prompt + " (1-" + lastGames.size() + "): ");
                var text = in.nextLine().trim();
                int num = Integer.parseInt(text);
                if (num < 1 || num > lastGames.size()) {
                    System.out.println("Number out of range.");
                    continue;
                }
                return num - 1;
            } catch (NumberFormatException nfe) {
                System.out.println("Please enter a number.");
            }
        }
    }
}
