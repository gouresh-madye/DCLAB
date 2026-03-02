#!/bin/bash
# Comprehensive Experiment Runner
# Runs both Centralized and Token-Based algorithms with varying node counts

# Set Java path
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
JAVA="/opt/homebrew/opt/openjdk@17/bin/java"
JAVAC="/opt/homebrew/opt/openjdk@17/bin/javac"

echo "=============================================="
echo "  DISTRIBUTED MUTUAL EXCLUSION EXPERIMENT"
echo "=============================================="
echo ""

# Configuration
NODE_COUNTS=(2 3 5 8 10)
CS_PER_NODE=5

# Directories
ROOT_DIR=$(pwd)
mkdir -p "$ROOT_DIR/results/logs"
mkdir -p "$ROOT_DIR/logs"

# Results files
CENTRALIZED_RESULTS="$ROOT_DIR/results/logs/centralized_results.csv"
TOKEN_RESULTS="$ROOT_DIR/results/logs/token_results.csv"

# Initialize result files
echo "Nodes,TotalMessages,AvgMessagesPerCS,AvgLatency" > "$CENTRALIZED_RESULTS"
echo "Nodes,TotalMessages,AvgMessagesPerCS,AvgLatency" > "$TOKEN_RESULTS"

# Change to Centralized directory and compile
echo "Compiling Centralized implementation..."
cd code/centralized
$JAVAC CentralizedServer.java CentralizedClient.java 2>/dev/null
if [ $? -ne 0 ]; then
    echo "Centralized compilation failed!"
    exit 1
fi
cd "$ROOT_DIR"

# Change to Token directory and compile  
echo "Compiling Token-Based implementation..."
cd code/token-based
$JAVAC TokenNode.java TokenRingManager.java 2>/dev/null
if [ $? -ne 0 ]; then
    echo "Token-Based compilation failed!"
    exit 1
fi
cd "$ROOT_DIR"

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
    
    # Start server
    $JAVA CentralizedServer $NUM_NODES $CS_PER_NODE > "$ROOT_DIR/logs/centralized_output_$NUM_NODES.log" 2>&1 &
    SERVER_PID=$!
    sleep 1
    
    # Start clients
    for i in $(seq 1 $NUM_NODES); do
        $JAVA CentralizedClient $i >> "$ROOT_DIR/logs/centralized_output_$NUM_NODES.log" 2>&1 &
    done
    
    # Wait for server
    wait $SERVER_PID 2>/dev/null
    
    # Extract CSV output
    CSV_LINE=$(grep "CSV_OUTPUT" "$ROOT_DIR/logs/centralized_output_$NUM_NODES.log" | tail -1 | cut -d',' -f2-)
    if [ -n "$CSV_LINE" ]; then
        echo "$CSV_LINE" >> "$CENTRALIZED_RESULTS"
        echo "Centralized ($NUM_NODES nodes): $CSV_LINE"
    fi
    
    cd "$ROOT_DIR"
    sleep 2
    
    # Run Token-Based Experiment
    echo ""
    echo "--- Token-Based Algorithm ($NUM_NODES nodes) ---"
    cd code/token-based
    
    $JAVA TokenRingManager $NUM_NODES $CS_PER_NODE > "$ROOT_DIR/logs/token_output_$NUM_NODES.log" 2>&1
    
    # Extract CSV output
    CSV_LINE=$(grep "CSV_OUTPUT" "$ROOT_DIR/logs/token_output_$NUM_NODES.log" | tail -1 | cut -d',' -f2-)
    if [ -n "$CSV_LINE" ]; then
        echo "$CSV_LINE" >> "$TOKEN_RESULTS"
        echo "Token-Based ($NUM_NODES nodes): $CSV_LINE"
    fi
    
    # Cleanup token files
    rm -f START_* DONE_*
    
    cd "$ROOT_DIR"
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
echo "  - $TOKEN_RESULTS"
echo ""
echo "Centralized Results:"
cat $CENTRALIZED_RESULTS
echo ""
echo "Token-Based Results:"
cat $TOKEN_RESULTS
