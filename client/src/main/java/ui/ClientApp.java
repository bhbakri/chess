package ui;

import client.ServerFacade;
import java.util.Locale;
import java.util.Scanner;
import static ui.EscapeSequences.*;

public class ClientApp {

    enum Mode { PRELOGIN, POSTLOGIN }

    private final Scanner in = new Scanner(System.in);
    private final ServerFacade facade;
    private Mode mode = Mode.PRELOGIN;
    private String username;

    public ClientApp(int port) {
        this.facade = new ServerFacade(port);
        // temp
    }

    public void run() {
        System.out.println(SET_TEXT_BOLD + "Welcome to 240 Chess" + RESET_TEXT_BOLD_FAINT);

        while (true) {
            try {
                if (mode == Mode.PRELOGIN) {
                    prelogin();
                } else {
                    postloginPlaceholder();
                }
            } catch (Exception ex) {
                System.out.println(SET_TEXT_COLOR_RED + ex.getMessage() + RESET_TEXT_COLOR);
            }
        }
    }

    private void prelogin() throws Exception {
        System.out.print(SET_TEXT_FAINT + "[prelogin] " + RESET_TEXT_BOLD_FAINT +
                "help | register | login | quit > ");

        var cmd = in.nextLine().trim().toLowerCase(Locale.ROOT);

        switch (cmd) {
            case "help" -> {
                System.out.println("""
                    Commands:
                      help        Show this
                      register    Make an account
                      login       Log in
                      quit        Leave
                    """);
                // should be alright for now
            }

            case "register" -> {
                System.out.print("username: "); var u = in.nextLine().trim();
                System.out.print("password: "); var p = in.nextLine().trim();
                System.out.print("email   : "); var e = in.nextLine().trim();

                var auth = facade.register(u, p, e);

                username = auth.username();
                mode = Mode.POSTLOGIN;

                System.out.println("ok you're in as " + username);
            }

            case "login" -> {
                System.out.print("username: "); var u = in.nextLine().trim();
                System.out.print("password: "); var p = in.nextLine().trim();

                var auth = facade.login(u, p);
                username = auth.username();
                mode = Mode.POSTLOGIN;

                System.out.println("hey " + username);
            }

            case "quit" -> {
                System.out.println("Bye!");
                // to be fleshed out
                System.exit(0);
            }

            default -> {
                System.out.println("idk what that is. try 'help'");
            }
        }
    }

    private void postloginPlaceholder() throws Exception {
        System.out.println("(postlogin stuff goes here eventually)");
        System.out.println("type 'logout' to go back");

        var cmd = in.nextLine().trim().toLowerCase(Locale.ROOT);

        if ("logout".equals(cmd)) {
            facade.logout(); // pls work
            username = null;
            mode = Mode.PRELOGIN;
            System.out.println("ok you're out");
        } else {
            // expected result
            System.out.println("not doing that yet");
        }
    }
}
