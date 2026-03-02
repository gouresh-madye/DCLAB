import java.io.*;
import java.util.*;

/**
 * Node Launcher for Ricart-Agrawala Experiment
 * Orchestrates the distributed mutual exclusion experiment by:
 * 1. Starting all nodes
 * 2. Signaling experiment start
 * 3. Waiting for completion
 * 4. Collecting and reporting statistics
 */
public class NodeLauncher {
    private int numNodes;
    private int csPerNode;
    private int basePort;
    private List<Process> nodeProcesses;
    private static final String JAVA_PATH = "java";

    public NodeLauncher(int numNodes, int csPerNode) {
        this.numNodes = numNodes;
        this.csPerNode = csPerNode;
        this.basePort = 10000;
        this.nodeProcesses = new ArrayList<>();
    }

    public void runExperiment() {
        System.out.println("================================================");
        System.out.println("  Ricart-Agrawala Mutual Exclusion Experiment");
        System.out.println("================================================");
        System.out.println("Number of Nodes: " + numNodes);
        System.out.println("CS per Node: " + csPerNode);
        System.out.println("Expected Messages per CS: 2(" + numNodes + "-1) = " + (2 * (numNodes - 1)));
        System.out.println("");

        // Clean up any previous experiment files
        cleanupFiles();

        long startTime = System.currentTimeMillis();

        // Start all nodes
        startNodes();

        // Give nodes time to connect to each other
        try {
            Thread.sleep(2000);
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
            new File("RA_START_" + i).delete();
            new File("RA_DONE_" + i).delete();
        }
    }

    private void startNodes() {
        System.out.println("Starting " + numNodes + " nodes...\n");

        for (int i = 1; i <= numNodes; i++) {
            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("-cp");
            command.add(".");
            command.add("DistributedNode");
            command.add(String.valueOf(i));
            command.add(String.valueOf(numNodes));
            command.add(String.valueOf(basePort));
            command.add(String.valueOf(csPerNode));

            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                // Redirect output to avoid cluttering main output or just inherit
                pb.inheritIO();
                Process p = pb.start();
                nodeProcesses.add(p);
                System.out.println("Started Node " + i);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void signalStart() {
        System.out.println("\nSignaling experiment start to all nodes...\n");

        for (int i = 1; i <= numNodes; i++) {
            try {
                new File("RA_START_" + i).createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void waitForCompletion() {
        System.out.println("Waiting for all nodes to complete...\n");

        int maxWaitSeconds = 120;
        int waited = 0;

        while (waited < maxWaitSeconds) {
            int completedNodes = 0;
            for (int i = 1; i <= numNodes; i++) {
                if (new File("RA_DONE_" + i).exists()) {
                    completedNodes++;
                }
            }

            if (completedNodes >= numNodes) {
                System.out.println("All nodes completed!\n");
                return;
            }

            try {
                Thread.sleep(500);
                waited++;
            } catch (InterruptedException e) {
                break;
            }
        }

        System.out.println("Warning: Timeout waiting for nodes to complete");
    }

    private void collectAndReportStatistics(long totalTime) {
        int totalRequestsSent = 0;
        int totalRequestsReceived = 0;
        int totalRepliesSent = 0;
        int totalRepliesReceived = 0;
        long totalLatency = 0;
        int nodesReported = 0;
        int totalCS = 0;

        for (int i = 1; i <= numNodes; i++) {
            File doneFile = new File("RA_DONE_" + i);
            if (doneFile.exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(doneFile));
                    br.readLine(); // Skip DONE

                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            switch (parts[0]) {
                                case "RequestsSent":
                                    totalRequestsSent += Integer.parseInt(parts[1]);
                                    break;
                                case "RequestsReceived":
                                    totalRequestsReceived += Integer.parseInt(parts[1]);
                                    break;
                                case "RepliesSent":
                                    totalRepliesSent += Integer.parseInt(parts[1]);
                                    break;
                                case "RepliesReceived":
                                    totalRepliesReceived += Integer.parseInt(parts[1]);
                                    break;
                                case "CompletedCS":
                                    totalCS += Integer.parseInt(parts[1]);
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

        int totalMessages = totalRequestsSent + totalRepliesSent;
        int expectedMessagesPerCS = 2 * (numNodes - 1);
        double actualMessagesPerCS = totalCS > 0 ? (double) totalMessages / totalCS : 0;
        double avgLatency = nodesReported > 0 ? (double) totalLatency / nodesReported : 0;

        System.out.println("================================================");
        System.out.println("   RICART-AGRAWALA MUTUAL EXCLUSION RESULTS");
        System.out.println("================================================");
        System.out.println("Number of Nodes: " + numNodes);
        System.out.println("CS per Node: " + csPerNode);
        System.out.println("Total CS Executions: " + totalCS);
        System.out.println("------------------------------------------------");
        System.out.println("REQUEST Messages Sent: " + totalRequestsSent);
        System.out.println("REQUEST Messages Received: " + totalRequestsReceived);
        System.out.println("REPLY Messages Sent: " + totalRepliesSent);
        System.out.println("REPLY Messages Received: " + totalRepliesReceived);
        System.out.println("------------------------------------------------");
        System.out.println("Total Messages Sent: " + totalMessages);
        System.out.println("Theoretical Messages per CS: " + expectedMessagesPerCS + " [2(N-1)]");
        System.out.println("Actual Avg Messages per CS: " + String.format("%.2f", actualMessagesPerCS));
        System.out.println("------------------------------------------------");
        System.out.println("Total Time: " + totalTime + " ms");
        System.out.println("Avg Latency per CS: " + String.format("%.2f", avgLatency) + " ms");
        System.out.println("================================================\n");

        // CSV format for easy parsing
        System.out.println("CSV_OUTPUT," + numNodes + "," + totalMessages + "," +
                String.format("%.2f", actualMessagesPerCS) + "," + String.format("%.2f", avgLatency) +
                "," + expectedMessagesPerCS);
    }

    private void stopNodes() {
        for (Process p : nodeProcesses) {
            p.destroyForcibly();
        }
        nodeProcesses.clear();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java NodeLauncher <numNodes> <csPerNode>");
            System.out.println("Example: java NodeLauncher 5 3");
            return;
        }

        int numNodes = Integer.parseInt(args[0]);
        int csPerNode = Integer.parseInt(args[1]);

        NodeLauncher launcher = new NodeLauncher(numNodes, csPerNode);
        launcher.runExperiment();
    }
}
