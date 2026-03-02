#!/bin/bash
# Launcher for Centralized Mutual Exclusion Experiment

NUM_CLIENTS=${1:-3}
CS_PER_CLIENT=${2:-5}

echo "=== Centralized Mutual Exclusion Experiment ==="
echo "Number of Clients: $NUM_CLIENTS"
echo "CS per Client: $CS_PER_CLIENT"
echo ""

# Compile
echo "Compiling..."
javac CentralizedServer.java CentralizedClient.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Start server in background
echo "Starting server..."
java CentralizedServer $NUM_CLIENTS $CS_PER_CLIENT &
SERVER_PID=$!
sleep 1

# Start clients
echo "Starting $NUM_CLIENTS clients..."
for i in $(seq 1 $NUM_CLIENTS); do
    java CentralizedClient $i &
done

# Wait for server to finish
wait $SERVER_PID

echo ""
echo "Experiment completed!"
