#!/bin/bash
# Comprehensive Experiment Runner for Non-Token Based Mutual Exclusion
# Compares Centralized vs Ricart-Agrawala algorithms

# Set Java path
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
JAVA="/opt/homebrew/opt/openjdk@17/bin/java"
JAVAC="/opt/homebrew/opt/openjdk@17/bin/javac"

echo "=============================================="
echo "  NON-TOKEN BASED MUTUAL EXCLUSION EXPERIMENT"
echo "  Centralized vs Ricart-Agrawala Comparison"
echo "=============================================="
echo ""

# Configuration
NODE_COUNTS=(2 3 5 8 10)
CS_PER_NODE=5

# Results files
CENTRALIZED_RESULTS="results/logs/centralized_results.csv"
RA_RESULTS="results/logs/ricart_agrawala_results.csv"

# Initialize result files
echo "Nodes,TotalMessages,AvgMessagesPerCS,AvgLatency" > $CENTRALIZED_RESULTS
echo "Nodes,TotalMessages,AvgMessagesPerCS,AvgLatency,TheoreticalPerCS" > $RA_RESULTS

# Compile Centralized implementation
echo "Compiling Centralized implementation..."
cd code/centralized
$JAVAC CentralizedServer.java CentralizedClient.java 2>/dev/null
if [ $? -ne 0 ]; then
    echo "Centralized compilation failed!"
    exit 1
fi
cd ../..

# Compile Ricart-Agrawala implementation
echo "Compiling Ricart-Agrawala implementation..."
cd code/non-token-based
$JAVAC Message.java DistributedNode.java NodeLauncher.java 2>/dev/null
if [ $? -ne 0 ]; then
    echo "Ricart-Agrawala compilation failed!"
    exit 1
fi
cd ../..

echo ""
echo "Starting experiments..."
echo ""

# Run experiments for each node count
for NUM_NODES in "${NODE_COUNTS[@]}"; do
    echo "=============================================="
    echo "Running experiments with $NUM_NODES nodes"
    echo "=============================================="
    
    # Run Centralized Experiment
    echo ""
    echo "--- Centralized Algorithm ($NUM_NODES nodes) ---"
    cd code/centralized
    
    # Start server in background
    $JAVA CentralizedServer $NUM_NODES $CS_PER_NODE > ../../logs/centralized_output_$NUM_NODES.log 2>&1 &
    SERVER_PID=$!
    sleep 1
    
    # Start clients
    for i in $(seq 1 $NUM_NODES); do
        $JAVA CentralizedClient $i >> ../../logs/centralized_output_$NUM_NODES.log 2>&1 &
    done
    
    # Wait for server to complete
    wait $SERVER_PID 2>/dev/null
    
    # Extract CSV output
    CSV_LINE=$(grep "CSV_OUTPUT" ../../logs/centralized_output_$NUM_NODES.log | tail -1 | cut -d',' -f2-)
    if [ -n "$CSV_LINE" ]; then
        echo "$CSV_LINE" >> ../../$CENTRALIZED_RESULTS
        echo "Centralized ($NUM_NODES nodes): $CSV_LINE"
    fi
    
    cd ../..
    sleep 2
    
    # Run Ricart-Agrawala Experiment
    echo ""
    echo "--- Ricart-Agrawala Algorithm ($NUM_NODES nodes) ---"
    cd code/non-token-based
    
    $JAVA NodeLauncher $NUM_NODES $CS_PER_NODE > ../../logs/ra_output_$NUM_NODES.log 2>&1
    
    # Extract CSV output
    CSV_LINE=$(grep "CSV_OUTPUT" ../../logs/ra_output_$NUM_NODES.log | tail -1 | cut -d',' -f2-)
    if [ -n "$CSV_LINE" ]; then
        echo "$CSV_LINE" >> ../../$RA_RESULTS
        echo "Ricart-Agrawala ($NUM_NODES nodes): $CSV_LINE"
    fi
    
    # Cleanup signal files
    rm -f RA_START_* RA_DONE_*
    
    cd ../..
    sleep 2
    
    echo ""
done

echo ""
echo "=============================================="
echo "EXPERIMENTS COMPLETED"
echo "=============================================="
echo ""
echo "Results saved to:"
echo "  - $CENTRALIZED_RESULTS"
echo "  - $RA_RESULTS"
echo ""
echo "Centralized Results:"
cat $CENTRALIZED_RESULTS
echo ""
echo "Ricart-Agrawala Results:"
cat $RA_RESULTS
