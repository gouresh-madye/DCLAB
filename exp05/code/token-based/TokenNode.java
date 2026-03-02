import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.*;

/**
 * Token-Based Ring Mutual Exclusion Node
 * 
 * Each node:
 * - Has a unique ID and port
 * - Knows the next node in the ring
 * - Can hold the token
 * - Can request CS when it wants
 * 
 * Message complexity: O(N) worst case, O(1) best case, N/2 average
 */
public class TokenNode {
    private int nodeId;
    private int myPort;
    private int nextNodePort;
    private boolean hasToken;
    private boolean wantsCS;
    private int csToExecute;
    private int completedCS;

    // Metrics
    private AtomicInteger messagesSent = new AtomicInteger(0);
    private AtomicInteger messagesReceived = new AtomicInteger(0);
    private AtomicInteger tokenPassCount = new AtomicInteger(0);

    private ServerSocket serverSocket;
    private Socket nextNodeSocket;
    private PrintWriter nextNodeOut;

    // Timing
    private long[] csStartTimes;
    private long[] csEndTimes;
    private long totalLatency = 0;

    private volatile boolean running = true;
    private volatile boolean experimentStarted = false;

    public TokenNode(int nodeId, int myPort, int nextNodePort, boolean hasToken, int csToExecute) {
        this.nodeId = nodeId;
        this.myPort = myPort;
        this.nextNodePort = nextNodePort;
        this.hasToken = hasToken;
        this.csToExecute = csToExecute;
        this.completedCS = 0;
        this.csStartTimes = new long[csToExecute];
        this.csEndTimes = new long[csToExecute];
    }

    public void start() {
        // Start listener thread
        Thread listenerThread = new Thread(this::listen);
        listenerThread.start();

        // Give time for all nodes to start their listeners
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Connect to next node
        connectToNextNode();

        // Wait for experiment start signal
        waitForStart();

        // If this node has the token initially, start the ring
        if (hasToken) {
            System.out.println("[Node" + nodeId + "] I have the initial token");
            processToken();
        }

        try {
            listenerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void waitForStart() {
        // Wait for START file to appear (simple synchronization)
        File startFile = new File("START_" + nodeId);
        while (!startFile.exists()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
        experimentStarted = true;
        System.out.println("[Node" + nodeId + "] Experiment started");
    }

    private void listen() {
        try {
            serverSocket = new ServerSocket(myPort);
            System.out.println("[Node" + nodeId + "] Listening on port " + myPort);

            Socket clientSocket = serverSocket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String message;
            while (running && (message = in.readLine()) != null) {
                messagesReceived.incrementAndGet();

                if (message.equals("TOKEN")) {
                    System.out.println("[Node" + nodeId + "] Received TOKEN");
                    hasToken = true;
                    processToken();
                }
            }

            clientSocket.close();
            serverSocket.close();

        } catch (IOException e) {
            if (running) {
                e.printStackTrace();
            }
        }
    }

    private void connectToNextNode() {
        int retries = 10;
        while (retries > 0) {
            try {
                nextNodeSocket = new Socket("localhost", nextNodePort);
                nextNodeOut = new PrintWriter(nextNodeSocket.getOutputStream(), true);
                System.out.println("[Node" + nodeId + "] Connected to next node on port " + nextNodePort);
                return;
            } catch (IOException e) {
                retries--;
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }
        System.err.println("[Node" + nodeId + "] Failed to connect to next node");
    }

    private synchronized void processToken() {
        if (!experimentStarted) {
            passToken();
            return;
        }

        if (completedCS < csToExecute) {
            // We want to execute critical section
            wantsCS = true;
            csStartTimes[completedCS] = System.currentTimeMillis();

            executeCriticalSection();

            csEndTimes[completedCS] = System.currentTimeMillis();
            totalLatency += (csEndTimes[completedCS] - csStartTimes[completedCS]);
            completedCS++;
            wantsCS = false;

            System.out.println("[Node" + nodeId + "] Completed CS " + completedCS + "/" + csToExecute);
        }

        if (completedCS >= csToExecute) {
            // Write completion marker
            try {
                PrintWriter pw = new PrintWriter(new FileWriter("DONE_" + nodeId));
                pw.println("DONE");
                pw.println("MessagesSent:" + messagesSent.get());
                pw.println("MessagesReceived:" + messagesReceived.get());
                pw.println("TokenPasses:" + tokenPassCount.get());
                pw.println("AvgLatency:" + (totalLatency / csToExecute));
                pw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("\n[Node" + nodeId + "] === COMPLETED ===");
            System.out.println("[Node" + nodeId + "] Messages Sent: " + messagesSent.get());
            System.out.println("[Node" + nodeId + "] Messages Received: " + messagesReceived.get());
            System.out.println("[Node" + nodeId + "] Token Passes: " + tokenPassCount.get());
            System.out.println("[Node" + nodeId + "] Avg Latency: " + (totalLatency / csToExecute) + " ms");
        }

        passToken();
    }

    private void passToken() {
        if (nextNodeOut != null) {
            nextNodeOut.println("TOKEN");
            messagesSent.incrementAndGet();
            tokenPassCount.incrementAndGet();
            hasToken = false;
            System.out.println("[Node" + nodeId + "] Passed TOKEN to next node");
        }
    }

    private void executeCriticalSection() {
        System.out.println("[Node" + nodeId + "] === CRITICAL SECTION START ===");

        // Simulate work in critical section
        try {
            Thread.sleep(50 + (int) (Math.random() * 50));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[Node" + nodeId + "] === CRITICAL SECTION END ===");
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (nextNodeSocket != null && !nextNodeSocket.isClosed()) {
                nextNodeSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: java TokenNode <nodeId> <myPort> <nextNodePort> <hasToken> <csToExecute>");
            System.out.println("Example: java TokenNode 1 9001 9002 true 5");
            return;
        }

        int nodeId = Integer.parseInt(args[0]);
        int myPort = Integer.parseInt(args[1]);
        int nextNodePort = Integer.parseInt(args[2]);
        boolean hasToken = Boolean.parseBoolean(args[3]);
        int csToExecute = Integer.parseInt(args[4]);

        TokenNode node = new TokenNode(nodeId, myPort, nextNodePort, hasToken, csToExecute);
        node.start();
    }
}
