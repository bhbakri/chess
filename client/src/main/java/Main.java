package ui;

public class Main {
    public static void main(String[] args) {
        // When you run the client, your server should already be running on this port.
        int port = 8080; // change if your server uses a different port

        if (args.length == 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                // ignore bad arg, just use default port
            }
        }

        new ClientApp(port).run();
    }
}
