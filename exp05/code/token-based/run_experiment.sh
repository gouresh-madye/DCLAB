#!/bin/bash
# Launcher for Token-Based Mutual Exclusion Experiment

NUM_NODES=${1:-3}
CS_PER_NODE=${2:-5}

echo "=== Token-Based Mutual Exclusion Experiment ==="
echo "Number of Nodes: $NUM_NODES"
echo "CS per Node: $CS_PER_NODE"
echo ""

# Compile
echo "Compiling..."
javac TokenNode.java TokenRingManager.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Run the experiment
java TokenRingManager $NUM_NODES $CS_PER_NODE

echo ""
echo "Experiment completed!"
