import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Distributed Node implementing Ricart-Agrawala Algorithm
 * 
 * Algorithm:
 * 1. To enter CS: broadcast REQUEST to all N-1 nodes, wait for N-1 REPLYs
 * 2. On receiving REQUEST:
 * - If RELEASED or requester has higher priority: send REPLY immediately
 * - Otherwise: defer REPLY until after exiting CS
 * 3. On exiting CS: send all deferred REPLYs
 * 
 * Priority: Lower timestamp wins; ties broken by lower nodeId
 * Message complexity: 2(N-1) per CS = O(N)
 */
public class DistributedNode {
    // Node identification
    private int nodeId;
    private int totalNodes;
    private int basePort;

    // Lamport logical clock
    private AtomicInteger lamportClock = new AtomicInteger(0);

    // Node state
    private enum State {
        RELEASED, WANTED, HELD
    }

    private volatile State state = State.RELEASED;

    // Current request info
    private volatile int currentRequestTimestamp = 0;
    private volatile int currentCSNumber = 0;

    // Replies tracking
    private Set<Integer> repliesReceived = ConcurrentHashMap.newKeySet();
    private CountDownLatch replyLatch;

    // Deferred replies (nodes waiting for our reply)
    private Queue<Integer> deferredReplies = new ConcurrentLinkedQueue<>();

    // Communication
    private ServerSocket serverSocket;
    private Map<Integer, Socket> peerSockets = new ConcurrentHashMap<>();
    private Map<Integer, PrintWriter> peerWriters = new ConcurrentHashMap<>();
    private Map<Integer, BufferedReader> peerReaders = new ConcurrentHashMap<>();

    // Metrics
    private AtomicInteger requestsSent = new AtomicInteger(0);
    private AtomicInteger requestsReceived = new AtomicInteger(0);
    private AtomicInteger repliesSent = new AtomicInteger(0);
    private AtomicInteger repliesReceivedCount = new AtomicInteger(0);

    // Experiment control
    private int csToExecute;
    private int completedCS = 0;
    private volatile boolean running = true;
    private volatile boolean experimentStarted = false;

    // Timing
    private long[] csLatencies;
    private long totalLatency = 0;

    public DistributedNode(int nodeId, int totalNodes, int basePort, int csToExecute) {
        this.nodeId = nodeId;
        this.totalNodes = totalNodes;
        this.basePort = basePort;
        this.csToExecute = csToExecute;
        this.csLatencies = new long[csToExecute];
    }

    public void start() {
        try {
            // Start server to listen for messages
            startServer();

            // Wait for all nodes to start
            Thread.sleep(500);

            // Connect to all other nodes
            connectToPeers();

            // Wait for experiment start signal
            waitForStart();

            // Execute critical sections
            runExperiment();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startServer() throws IOException {
        int myPort = basePort + nodeId;
        serverSocket = new ServerSocket(myPort);
        System.out.println("[Node" + nodeId + "] Listening on port " + myPort);

        // Start listener thread
        Thread listenerThread = new Thread(this::listenForConnections);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenForConnections() {
        try {
            while (running) {
                Socket clientSocket = serverSocket.accept();
                Thread handler = new Thread(() -> handleConnection(clientSocket));
                handler.setDaemon(true);
                handler.start();
            }
        } catch (IOException e) {
            if (running) {
                e.printStackTrace();
            }
        }
    }

    private void handleConnection(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;

            while (running && (line = in.readLine()) != null) {
                processMessage(line);
            }
        } catch (IOException e) {
            if (running) {
                // Connection closed
            }
        }
    }

    private void connectToPeers() {
        System.out.println("[Node" + nodeId + "] Connecting to peers...");

        for (int i = 1; i <= totalNodes; i++) {
            if (i != nodeId) {
                connectToPeer(i);
            }
        }

        System.out.println("[Node" + nodeId + "] Connected to " + peerWriters.size() + " peers");
    }

    private void connectToPeer(int peerId) {
        int peerPort = basePort + peerId;
        int retries = 10;

        while (retries > 0) {
            try {
                Socket socket = new Socket("localhost", peerPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                peerSockets.put(peerId, socket);
                peerWriters.put(peerId, out);
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
        System.err.println("[Node" + nodeId + "] Failed to connect to Node" + peerId);
    }

    private void waitForStart() {
        File startFile = new File("RA_START_" + nodeId);
        while (!startFile.exists() && running) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
        experimentStarted = true;
        System.out.println("[Node" + nodeId + "] Experiment started");
    }

    private void runExperiment() {
        for (int i = 0; i < csToExecute && running; i++) {
            currentCSNumber = i + 1;
            long startTime = System.currentTimeMillis();

            requestCriticalSection();
            executeCriticalSection();
            releaseCriticalSection();

            long endTime = System.currentTimeMillis();
            csLatencies[i] = endTime - startTime;
            totalLatency += csLatencies[i];
            completedCS++;

            System.out.println("[Node" + nodeId + "] Completed CS " + completedCS + "/" + csToExecute);

            // Small delay between CS requests
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }

        // Write completion file
        writeCompletionFile();

        // Shutdown
        shutdown();
    }

    /**
     * Request entry to Critical Section
     * Broadcast REQUEST to all N-1 peers and wait for N-1 REPLYs
     */
    private void requestCriticalSection() {
        // Update state
        state = State.WANTED;

        // Increment clock and record request timestamp
        currentRequestTimestamp = lamportClock.incrementAndGet();

        // Prepare to receive replies
        repliesReceived.clear();
        replyLatch = new CountDownLatch(totalNodes - 1);

        System.out.println("[Node" + nodeId + "] Requesting CS #" + currentCSNumber +
                " with timestamp " + currentRequestTimestamp);

        // Broadcast REQUEST to all peers
        String requestMsg = formatMessage("REQUEST", nodeId, currentRequestTimestamp, currentCSNumber);

        for (int peerId : peerWriters.keySet()) {
            sendMessage(peerId, requestMsg);
            requestsSent.incrementAndGet();
            System.out.println("[Node" + nodeId + " -> Node" + peerId + "] REQUEST(ts=" +
                    currentRequestTimestamp + ")");
        }

        // Wait for all replies
        try {
            replyLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[Node" + nodeId + "] Received all " + (totalNodes - 1) + " replies");
    }

    /**
     * Execute Critical Section
     */
    private void executeCriticalSection() {
        state = State.HELD;

        System.out.println("[Node" + nodeId + "] === CRITICAL SECTION #" + currentCSNumber + " START ===");

        // Simulate work in critical section
        try {
            Thread.sleep(50 + (int) (Math.random() * 50));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[Node" + nodeId + "] === CRITICAL SECTION #" + currentCSNumber + " END ===");
    }

    /**
     * Release Critical Section
     * Send deferred REPLYs to waiting nodes
     */
    private void releaseCriticalSection() {
        state = State.RELEASED;

        // Send all deferred replies
        while (!deferredReplies.isEmpty()) {
            Integer deferredNodeId = deferredReplies.poll();
            if (deferredNodeId != null) {
                sendReply(deferredNodeId);
                System.out.println("[Node" + nodeId + " -> Node" + deferredNodeId + "] REPLY (deferred)");
            }
        }
    }

    /**
     * Process incoming message
     */
    private synchronized void processMessage(String msgStr) {
        String[] parts = msgStr.split(",");
        if (parts.length < 4)
            return;

        String type = parts[0];
        int senderId = Integer.parseInt(parts[1]);
        int timestamp = Integer.parseInt(parts[2]);
        int csNum = Integer.parseInt(parts[3]);

        // Update Lamport clock
        lamportClock.set(Math.max(lamportClock.get(), timestamp) + 1);

        if (type.equals("REQUEST")) {
            handleRequest(senderId, timestamp, csNum);
        } else if (type.equals("REPLY")) {
            handleReply(senderId, timestamp);
        }
    }

    /**
     * Handle incoming REQUEST
     * Ricart-Agrawala decision logic
     */
    private void handleRequest(int senderId, int timestamp, int csNum) {
        requestsReceived.incrementAndGet();
        System.out.println("[Node" + nodeId + " <- Node" + senderId + "] REQUEST(ts=" + timestamp + ")");

        boolean shouldDefer = false;

        if (state == State.HELD) {
            // We're in CS - defer reply
            shouldDefer = true;
        } else if (state == State.WANTED) {
            // We also want CS - compare priorities
            // Our priority is higher (we go first) if our timestamp is lower
            // or if timestamps equal and our nodeId is lower
            if (currentRequestTimestamp < timestamp ||
                    (currentRequestTimestamp == timestamp && nodeId < senderId)) {
                // We have higher priority - defer reply
                shouldDefer = true;
            }
        }

        if (shouldDefer) {
            deferredReplies.add(senderId);
            System.out.println("[Node" + nodeId + "] Deferring REPLY to Node" + senderId);
        } else {
            sendReply(senderId);
            System.out.println("[Node" + nodeId + " -> Node" + senderId + "] REPLY (immediate)");
        }
    }

    /**
     * Handle incoming REPLY
     */
    private void handleReply(int senderId, int timestamp) {
        repliesReceivedCount.incrementAndGet();
        System.out.println("[Node" + nodeId + " <- Node" + senderId + "] REPLY");

        if (!repliesReceived.contains(senderId)) {
            repliesReceived.add(senderId);
            if (replyLatch != null) {
                replyLatch.countDown();
            }
        }
    }

    /**
     * Send REPLY to a peer
     */
    private void sendReply(int peerId) {
        String replyMsg = formatMessage("REPLY", nodeId, lamportClock.get(), 0);
        sendMessage(peerId, replyMsg);
        repliesSent.incrementAndGet();
    }

    /**
     * Send message to a peer
     */
    private void sendMessage(int peerId, String message) {
        PrintWriter writer = peerWriters.get(peerId);
        if (writer != null) {
            writer.println(message);
        }
    }

    /**
     * Format message as string
     */
    private String formatMessage(String type, int senderId, int timestamp, int csNum) {
        return type + "," + senderId + "," + timestamp + "," + csNum;
    }

    /**
     * Write completion file with metrics
     */
    private void writeCompletionFile() {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter("RA_DONE_" + nodeId));
            pw.println("DONE");
            pw.println("NodeId:" + nodeId);
            pw.println("RequestsSent:" + requestsSent.get());
            pw.println("RequestsReceived:" + requestsReceived.get());
            pw.println("RepliesSent:" + repliesSent.get());
            pw.println("RepliesReceived:" + repliesReceivedCount.get());
            pw.println("TotalMessagesSent:" + (requestsSent.get() + repliesSent.get()));
            pw.println("TotalMessagesReceived:" + (requestsReceived.get() + repliesReceivedCount.get()));
            pw.println("CompletedCS:" + completedCS);
            pw.println("AvgLatency:" + (completedCS > 0 ? totalLatency / completedCS : 0));
            pw.close();

            System.out.println("\n[Node" + nodeId + "] === EXPERIMENT COMPLETE ===");
            System.out.println("[Node" + nodeId + "] Requests Sent: " + requestsSent.get());
            System.out.println("[Node" + nodeId + "] Requests Received: " + requestsReceived.get());
            System.out.println("[Node" + nodeId + "] Replies Sent: " + repliesSent.get());
            System.out.println("[Node" + nodeId + "] Replies Received: " + repliesReceivedCount.get());
            System.out.println("[Node" + nodeId + "] Total Messages: " +
                    (requestsSent.get() + repliesSent.get() + requestsReceived.get() + repliesReceivedCount.get()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shutdown node
     */
    private void shutdown() {
        running = false;
        try {
            for (Socket socket : peerSockets.values()) {
                socket.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java DistributedNode <nodeId> <totalNodes> <basePort> <csToExecute>");
            System.out.println("Example: java DistributedNode 1 5 10000 3");
            return;
        }

        int nodeId = Integer.parseInt(args[0]);
        int totalNodes = Integer.parseInt(args[1]);
        int basePort = Integer.parseInt(args[2]);
        int csToExecute = Integer.parseInt(args[3]);

        DistributedNode node = new DistributedNode(nodeId, totalNodes, basePort, csToExecute);
        node.start();
    }
}
