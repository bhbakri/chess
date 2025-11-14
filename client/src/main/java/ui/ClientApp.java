package ui;

import client.ServerFacade;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import static ui.EscapeSequences.*;

public class ClientApp {

    // NOTE:
    // This client is still in an early prototype state (about 1/3 done).
    // Only basic login / logout flows are wired up.
    // Game commands (create/list/play/observe) are not implemented yet.

    enum Mode { PRELOGIN, POSTLOGIN }

    private final Scanner in = new Scanner(System.in);
    private final ServerFacade facade;
    private Mode mode = Mode.PRELOGIN;
    private String username;

    // Games from the last 'list' call, to map number -> game
    // TODO(step-2): actually populate this list from the server
    private List<ServerFacade.GameInfo> lastGames = new ArrayList<>();

    public ClientApp(int port) {
        this.facade = new ServerFacade(port);
        // TODO(step-1): support reading host/port from args or config
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
                // TODO(step-3): add optional debug mode to show stack traces
            }
        }
    }

    // ================= PRELOGIN (mostly working) =================

    private void prelogin() throws Exception {
        System.out.print(SET_TEXT_FAINT + "[prelogin] " + RESET_TEXT_BOLD_FAINT +
                "help | register | login | quit > ");
        var cmd = in.nextLine().trim().toLowerCase(Locale.ROOT);

        switch (cmd) {
            case "help" -> printPreloginHelp();

            case "register" -> {
                // TODO(step-2): basic input validation (non-empty, email format, etc.)
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

            TODO (later steps):
              - Remember last logged-in user
              - Add command history / nicer prompt
            """);
    }

    // ================= POSTLOGIN (early stub) =================

    private void postlogin() throws Exception {
        System.out.print(SET_TEXT_FAINT + "[postlogin] " + RESET_TEXT_BOLD_FAINT +
                "help | create | list | play | observe | logout > ");
        var cmd = in.nextLine().trim().toLowerCase(Locale.ROOT);

        switch (cmd) {
            case "help" -> printPostloginHelp();
            case "logout" -> doLogout();

            // The rest of the commands are just placeholders for now.
            case "create", "list", "play", "observe" -> {
                System.out.println(SET_TEXT_COLOR_YELLOW +
                        "Game features are not implemented yet (TODO in later steps)." +
                        RESET_TEXT_COLOR);
            }

            default -> System.out.println("Unknown command. Type 'help'.");
        }
    }

    private void printPostloginHelp() {
        System.out.println("""
            Commands:
              help        Show this help
              logout      Log out to prelogin menu

            TODO (future steps):
              create      Create a new game
              list        List existing games
              play        Join a game as WHITE or BLACK (by number)
              observe     Observe a game (by number)

            NOTE: Game-related commands are not wired up yet.
            """);
    }

    private void doLogout() throws Exception {
        // TODO(step-2): handle case where logout fails (expired token, etc.)
        facade.logout();
        username = null;
        mode = Mode.PRELOGIN;
        lastGames.clear();
        System.out.println("Logged out.");
    }

    // ================= Game-related stubs (not implemented yet) =================
    // These methods are placeholders to show intended structure but are deliberately
    // not finished. They should be implemented in later steps of the assignment.

    /**
     * TODO(step-3): Implement game creation via facade.createGame(name)
     */
    @SuppressWarnings("unused")
    private void doCreateGame() throws Exception {
        throw new UnsupportedOperationException("create game not implemented yet");
    }

    /**
     * TODO(step-3): Call facade.listGames() and render them into lastGames
     */
    @SuppressWarnings("unused")
    private void doListGames() throws Exception {
        throw new UnsupportedOperationException("list games not implemented yet");
    }

    /**
     * TODO(step-4): Join a game as WHITE/BLACK and open an interactive board view
     */
    @SuppressWarnings("unused")
    private void doPlayGame() throws Exception {
        throw new UnsupportedOperationException("play game not implemented yet");
    }

    /**
     * TODO(step-4): Join a game as an observer and open read-only board view
     */
    @SuppressWarnings("unused")
    private void doObserveGame() throws Exception {
        throw new UnsupportedOperationException("observe game not implemented yet");
    }

    // ================= Helpers (will be used once list/play/observe exist) =================

    /**
     * TODO(step-3): Use this helper from doPlayGame / doObserveGame
     * once the list of games is actually retrieved.
     */
    @SuppressWarnings("unused")
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
