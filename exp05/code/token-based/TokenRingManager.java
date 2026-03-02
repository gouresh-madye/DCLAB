import java.io.*;
import java.util.*;

/**
 * Token Ring Manager
 * Orchestrates the token ring experiment by:
 * 1. Starting all nodes
 * 2. Signaling experiment start
 * 3. Waiting for completion
 * 4. Collecting and reporting statistics
 */
public class TokenRingManager {
    private int numNodes;
    private int csPerNode;
    private int basePort;
    private List<Process> nodeProcesses;

    public TokenRingManager(int numNodes, int csPerNode) {
        this.numNodes = numNodes;
        this.csPerNode = csPerNode;
        this.basePort = 9000;
        this.nodeProcesses = new ArrayList<>();
    }

    public void runExperiment() {
        System.out.println("=== Token-Based Ring Mutual Exclusion ===");
        System.out.println("Number of Nodes: " + numNodes);
        System.out.println("CS per Node: " + csPerNode);
        System.out.println("");

        // Clean up any previous experiment files
        cleanupFiles();

        long startTime = System.currentTimeMillis();

        // Start all nodes
        startNodes();

        // Give nodes time to connect
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Signal experiment start
        signalStart();

        // Wait for all nodes to complete
        waitForCompletion();

        long endTime = System.currentTimeMillis();

        // Collect and report statistics
        collectAndReportStatistics(endTime - startTime);

        // Cleanup
        stopNodes();
        cleanupFiles();
    }

    private void cleanupFiles() {
        for (int i = 1; i <= numNodes; i++) {
            new File("START_" + i).delete();
            new File("DONE_" + i).delete();
        }
    }

    private void startNodes() {
        System.out.println("Starting " + numNodes + " nodes in ring configuration...");

        for (int i = 1; i <= numNodes; i++) {
            int myPort = basePort + i;
            int nextPort = (i == numNodes) ? basePort + 1 : basePort + i + 1;
            boolean hasToken = (i == 1); // Node 1 has the initial token

            try {
                String javaPath = "/opt/homebrew/opt/openjdk@17/bin/java";
                ProcessBuilder pb = new ProcessBuilder(
                        javaPath, "TokenNode",
                        String.valueOf(i),
                        String.valueOf(myPort),
                        String.valueOf(nextPort),
                        String.valueOf(hasToken),
                        String.valueOf(csPerNode));
                pb.inheritIO();
                Process p = pb.start();
                nodeProcesses.add(p);
                System.out.println("Started Node " + i + " (port " + myPort + " -> " + nextPort + ")");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void signalStart() {
        System.out.println("\nSignaling experiment start to all nodes...\n");

        for (int i = 1; i <= numNodes; i++) {
            try {
                new File("START_" + i).createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void waitForCompletion() {
        System.out.println("Waiting for all nodes to complete...\n");

        int completedNodes = 0;
        int maxWaitSeconds = 60;
        int waited = 0;

        while (completedNodes < numNodes && waited < maxWaitSeconds) {
            completedNodes = 0;
            for (int i = 1; i <= numNodes; i++) {
                if (new File("DONE_" + i).exists()) {
                    completedNodes++;
                }
            }

            if (completedNodes < numNodes) {
                try {
                    Thread.sleep(500);
                    waited++;
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        if (completedNodes < numNodes) {
            System.out.println("Warning: Only " + completedNodes + "/" + numNodes + " nodes completed");
        }
    }

    private void collectAndReportStatistics(long totalTime) {
        int totalMessagesSent = 0;
        int totalMessagesReceived = 0;
        int totalTokenPasses = 0;
        long totalLatency = 0;
        int nodesReported = 0;

        for (int i = 1; i <= numNodes; i++) {
            File doneFile = new File("DONE_" + i);
            if (doneFile.exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(doneFile));
                    br.readLine(); // Skip DONE

                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            switch (parts[0]) {
                                case "MessagesSent":
                                    totalMessagesSent += Integer.parseInt(parts[1]);
                                    break;
                                case "MessagesReceived":
                                    totalMessagesReceived += Integer.parseInt(parts[1]);
                                    break;
                                case "TokenPasses":
                                    totalTokenPasses += Integer.parseInt(parts[1]);
                                    break;
                                case "AvgLatency":
                                    totalLatency += Long.parseLong(parts[1]);
                                    break;
                            }
                        }
                    }
                    br.close();
                    nodesReported++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        int totalMessages = totalMessagesSent + totalMessagesReceived;
        int totalCS = numNodes * csPerNode;
        double avgMessagesPerCS = (double) totalMessages / totalCS;
        double avgLatency = (double) totalLatency / nodesReported;

        System.out.println("\n========================================");
        System.out.println("   TOKEN-BASED RING MUTUAL EXCLUSION RESULTS");
        System.out.println("========================================");
        System.out.println("Number of Nodes: " + numNodes);
        System.out.println("CS per Node: " + csPerNode);
        System.out.println("Total CS Executions: " + totalCS);
        System.out.println("----------------------------------------");
        System.out.println("Messages Sent: " + totalMessagesSent);
        System.out.println("Messages Received: " + totalMessagesReceived);
        System.out.println("Total Messages: " + totalMessages);
        System.out.println("Total Token Passes: " + totalTokenPasses);
        System.out.println("Avg Messages per CS: " + String.format("%.2f", avgMessagesPerCS));
        System.out.println("----------------------------------------");
        System.out.println("Total Time: " + totalTime + " ms");
        System.out.println("Avg Latency per CS: " + String.format("%.2f", avgLatency) + " ms");
        System.out.println("========================================\n");

        // CSV format for easy parsing
        System.out.println("CSV_OUTPUT," + numNodes + "," + totalMessages + "," +
                String.format("%.2f", avgMessagesPerCS) + "," + String.format("%.2f", avgLatency));
    }

    private void stopNodes() {
        for (Process p : nodeProcesses) {
            p.destroyForcibly();
        }
        nodeProcesses.clear();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java TokenRingManager <numNodes> <csPerNode>");
            System.out.println("Example: java TokenRingManager 5 3");
            return;
        }

        int numNodes = Integer.parseInt(args[0]);
        int csPerNode = Integer.parseInt(args[1]);

        TokenRingManager manager = new TokenRingManager(numNodes, csPerNode);
        manager.runExperiment();
    }
}
