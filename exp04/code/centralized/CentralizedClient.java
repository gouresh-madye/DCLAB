package code.centralized;

import java.io.*;
import java.net.*;

/**
 * Centralized Mutual Exclusion Client
 * Protocol: REQUEST → GRANT → (Critical Section) → RELEASE
 */
public class CentralizedClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8000;

    private static int clientId;
    private static int messagesSent = 0;
    private static int messagesReceived = 0;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java CentralizedClient <clientId>");
            return;
        }

        clientId = Integer.parseInt(args[0]);

        Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        System.out.println("Client " + clientId + " connected to server");

        // Wait for START signal
        String startMsg = in.readLine();
        messagesReceived++;

        if (!startMsg.startsWith("START:")) {
            System.out.println("Unexpected message: " + startMsg);
            return;
        }

        int csCount = Integer.parseInt(startMsg.split(":")[1]);
        System.out.println("Client " + clientId + " will execute " + csCount + " critical sections");

        for (int i = 0; i < csCount; i++) {
            // Send REQUEST
            out.println("REQUEST");
            messagesSent++;
            System.out.println("[Client" + clientId + "] Sent REQUEST for CS #" + (i + 1));

            // Wait for GRANT
            String response = in.readLine();
            messagesReceived++;

            if (response.equals("GRANT")) {
                System.out.println("[Client" + clientId + "] Received GRANT, entering CS #" + (i + 1));

                // Execute Critical Section
                executeCriticalSection(i + 1);

                // Send RELEASE
                out.println("RELEASE");
                messagesSent++;
                System.out.println("[Client" + clientId + "] Sent RELEASE after CS #" + (i + 1));
            }
        }

        // Wait for DONE
        String doneMsg = in.readLine();
        messagesReceived++;

        System.out.println("\n--- Client " + clientId + " Statistics ---");
        System.out.println("Messages Sent: " + messagesSent);
        System.out.println("Messages Received: " + messagesReceived);
        System.out.println("Total Messages: " + (messagesSent + messagesReceived));

        socket.close();
    }

    private static void executeCriticalSection(int csNumber) {
        System.out.println("[Client" + clientId + "] === CRITICAL SECTION #" + csNumber + " START ===");

        // Simulate some work in critical section
        try {
            Thread.sleep(50 + (int) (Math.random() * 50));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[Client" + clientId + "] === CRITICAL SECTION #" + csNumber + " END ===");
    }
}
