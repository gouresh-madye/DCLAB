import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Centralized Mutual Exclusion Server
 * Handles REQUEST → GRANT → RELEASE protocol
 * Message complexity: 3 messages per CS execution (constant)
 */
public class CentralizedServer {
    private static final int SERVER_PORT = 8000;
    private static AtomicInteger totalMessagesReceived = new AtomicInteger(0);
    private static AtomicInteger totalMessagesSent = new AtomicInteger(0);
    private static AtomicInteger csExecutions = new AtomicInteger(0);

    private static Queue<ClientHandler> requestQueue = new ConcurrentLinkedQueue<>();
    private static volatile boolean resourceLocked = false;
    private static volatile ClientHandler currentHolder = null;

    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static int expectedClients;
    private static CountDownLatch experimentComplete;
    private static int csPerClient;
    private static long startTime;

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java CentralizedServer <numClients> <csPerClient>");
            System.out.println("Example: java CentralizedServer 3 5");
            return;
        }

        expectedClients = Integer.parseInt(args[0]);
        csPerClient = Integer.parseInt(args[1]);
        experimentComplete = new CountDownLatch(expectedClients);

        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
        System.out.println("=== Centralized Mutual Exclusion Server ===");
        System.out.println("Waiting for " + expectedClients + " clients...");
        System.out.println("Each client will execute " + csPerClient + " critical sections");

        // Accept all clients first
        for (int i = 0; i < expectedClients; i++) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(clientSocket, i + 1);
            clients.add(handler);
            System.out.println("Client " + (i + 1) + " connected from port " + clientSocket.getPort());
        }

        System.out.println("\nAll clients connected. Starting experiment...\n");
        startTime = System.currentTimeMillis();

        // Start all client handlers
        for (ClientHandler handler : clients) {
            new Thread(handler).start();
        }

        // Wait for all clients to complete
        try {
            experimentComplete.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();

        // Print statistics
        printStatistics(endTime - startTime);

        serverSocket.close();
    }

    private static synchronized void processRequest(ClientHandler handler) {
        if (!resourceLocked) {
            resourceLocked = true;
            currentHolder = handler;
            handler.grantAccess();
        } else {
            requestQueue.add(handler);
        }
    }

    private static synchronized void processRelease(ClientHandler handler) {
        if (currentHolder == handler) {
            resourceLocked = false;
            currentHolder = null;
            csExecutions.incrementAndGet();

            // Grant to next in queue if any
            if (!requestQueue.isEmpty()) {
                ClientHandler next = requestQueue.poll();
                resourceLocked = true;
                currentHolder = next;
                next.grantAccess();
            }
        }
    }

    private static void printStatistics(long totalTime) {
        System.out.println("\n========================================");
        System.out.println("   CENTRALIZED MUTUAL EXCLUSION RESULTS");
        System.out.println("========================================");
        System.out.println("Number of Clients: " + expectedClients);
        System.out.println("CS per Client: " + csPerClient);
        System.out.println("Total CS Executions: " + csExecutions.get());
        System.out.println("----------------------------------------");
        System.out.println("Messages Received: " + totalMessagesReceived.get());
        System.out.println("Messages Sent: " + totalMessagesSent.get());
        System.out.println("Total Messages: " + (totalMessagesReceived.get() + totalMessagesSent.get()));
        System.out.println("Avg Messages per CS: " +
                String.format("%.2f",
                        (double) (totalMessagesReceived.get() + totalMessagesSent.get()) / csExecutions.get()));
        System.out.println("----------------------------------------");
        System.out.println("Total Time: " + totalTime + " ms");
        System.out.println("Avg Latency per CS: " +
                String.format("%.2f", (double) totalTime / csExecutions.get()) + " ms");
        System.out.println("========================================\n");

        // CSV format for easy parsing
        System.out.println("CSV_OUTPUT," + expectedClients + "," +
                (totalMessagesReceived.get() + totalMessagesSent.get()) + "," +
                String.format("%.2f",
                        (double) (totalMessagesReceived.get() + totalMessagesSent.get()) / csExecutions.get())
                + "," +
                String.format("%.2f", (double) totalTime / csExecutions.get()));
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private int clientId;
        private BufferedReader in;
        private PrintWriter out;
        private int completedCS = 0;

        ClientHandler(Socket socket, int clientId) throws IOException {
            this.socket = socket;
            this.clientId = clientId;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        void grantAccess() {
            out.println("GRANT");
            totalMessagesSent.incrementAndGet();
            System.out.println("[Server -> Client" + clientId + "] GRANT");
        }

        @Override
        public void run() {
            try {
                // Send START signal to client
                out.println("START:" + csPerClient);
                totalMessagesSent.incrementAndGet();

                String message;
                while ((message = in.readLine()) != null) {
                    totalMessagesReceived.incrementAndGet();

                    if (message.equals("REQUEST")) {
                        System.out.println("[Client" + clientId + " -> Server] REQUEST");
                        processRequest(this);
                    } else if (message.equals("RELEASE")) {
                        System.out.println("[Client" + clientId + " -> Server] RELEASE");
                        processRelease(this);
                        completedCS++;

                        if (completedCS >= csPerClient) {
                            out.println("DONE");
                            totalMessagesSent.incrementAndGet();
                            break;
                        }
                    }
                }

                socket.close();
                experimentComplete.countDown();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
