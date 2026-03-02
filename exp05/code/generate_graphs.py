#!/usr/bin/env python3
"""
Generate graphs for Distributed Mutual Exclusion Experiment
"""

import matplotlib.pyplot as plt
import numpy as np
import os

# Define output directory
script_dir = os.path.dirname(os.path.abspath(__file__))
project_root = os.path.dirname(script_dir)
output_dir = os.path.join(project_root, 'results', 'graphs')

# Create directory if it doesn't exist
os.makedirs(output_dir, exist_ok=True)

# Data from experiments
nodes = [2, 3, 5, 8, 10]

# Centralized Algorithm Results
centralized_total_messages = [34, 51, 85, 136, 170]
centralized_avg_per_cs = [3.40, 3.40, 3.40, 3.40, 3.40]
centralized_latency = [87.70, 77.80, 82.84, 83.38, 78.24]

# Token-Based Algorithm Results
token_total_messages = [405, 1610, 3800, 2302, 1895]
token_avg_per_cs = [40.50, 107.33, 152.00, 57.55, 37.90]
token_latency = [89.00, 80.00, 78.60, 66.50, 81.00]

# Create figure with subplots
fig, axes = plt.subplots(2, 2, figsize=(14, 10))
fig.suptitle('Distributed Mutual Exclusion - Message Overhead Analysis', fontsize=14, fontweight='bold')

# Plot 1: Nodes vs Total Messages
ax1 = axes[0, 0]
x = np.arange(len(nodes))
width = 0.35
bars1 = ax1.bar(x - width/2, centralized_total_messages, width, label='Centralized', color='steelblue')
bars2 = ax1.bar(x + width/2, token_total_messages, width, label='Token-Based', color='darkorange')
ax1.set_xlabel('Number of Nodes')
ax1.set_ylabel('Total Messages')
ax1.set_title('Total Messages vs Number of Nodes')
ax1.set_xticks(x)
ax1.set_xticklabels(nodes)
ax1.legend()
ax1.grid(axis='y', alpha=0.3)

# Plot 2: Nodes vs Avg Messages per CS
ax2 = axes[0, 1]
ax2.plot(nodes, centralized_avg_per_cs, 'o-', linewidth=2, markersize=8, label='Centralized', color='steelblue')
ax2.plot(nodes, token_avg_per_cs, 's-', linewidth=2, markersize=8, label='Token-Based', color='darkorange')
ax2.set_xlabel('Number of Nodes')
ax2.set_ylabel('Average Messages per CS')
ax2.set_title('Average Messages per Critical Section vs Nodes')
ax2.legend()
ax2.grid(True, alpha=0.3)

# Plot 3: Nodes vs Latency
ax3 = axes[1, 0]
ax3.plot(nodes, centralized_latency, 'o-', linewidth=2, markersize=8, label='Centralized', color='steelblue')
ax3.plot(nodes, token_latency, 's-', linewidth=2, markersize=8, label='Token-Based', color='darkorange')
ax3.set_xlabel('Number of Nodes')
ax3.set_ylabel('Average Latency (ms)')
ax3.set_title('Average Latency per CS vs Nodes')
ax3.legend()
ax3.grid(True, alpha=0.3)

# Plot 4: Theoretical Comparison
ax4 = axes[1, 1]
# Theoretical values
theoretical_centralized = [3] * len(nodes)  # O(1) = 3 messages
theoretical_token = nodes  # O(N) worst case

ax4.plot(nodes, theoretical_centralized, 'o--', linewidth=2, markersize=8, 
         label='Centralized O(1)', color='steelblue', alpha=0.7)
ax4.plot(nodes, theoretical_token, 's--', linewidth=2, markersize=8, 
         label='Token-Based O(N)', color='darkorange', alpha=0.7)
ax4.plot(nodes, centralized_avg_per_cs, 'o-', linewidth=2, markersize=8, 
         label='Centralized (Actual)', color='steelblue')
ax4.set_xlabel('Number of Nodes')
ax4.set_ylabel('Messages per CS')
ax4.set_title('Theoretical vs Actual Message Complexity')
ax4.legend()
ax4.grid(True, alpha=0.3)

plt.tight_layout()
output_file = os.path.join(output_dir, 'mutual_exclusion_analysis.png')
plt.savefig(output_file, dpi=150, bbox_inches='tight')
print(f'Graph saved as {output_file}')

# Also create individual graphs
fig2, ax = plt.subplots(figsize=(10, 6))
x = np.arange(len(nodes))
width = 0.35
bars1 = ax.bar(x - width/2, centralized_total_messages, width, label='Centralized', color='steelblue')
bars2 = ax.bar(x + width/2, token_total_messages, width, label='Token-Based', color='darkorange')
ax.set_xlabel('Number of Nodes', fontsize=12)
ax.set_ylabel('Total Messages', fontsize=12)
ax.set_title('Total Messages vs Number of Nodes\nDistributed Mutual Exclusion Comparison', fontsize=14)
ax.set_xticks(x)
ax.set_xticklabels(nodes)
ax.legend(fontsize=11)
ax.grid(axis='y', alpha=0.3)

# Add value labels on bars
for bar in bars1:
    height = bar.get_height()
    ax.annotate(f'{int(height)}', xy=(bar.get_x() + bar.get_width() / 2, height),
                xytext=(0, 3), textcoords="offset points", ha='center', va='bottom', fontsize=9)
for bar in bars2:
    height = bar.get_height()
    ax.annotate(f'{int(height)}', xy=(bar.get_x() + bar.get_width() / 2, height),
                xytext=(0, 3), textcoords="offset points", ha='center', va='bottom', fontsize=9)

plt.tight_layout()
output_file = os.path.join(output_dir, 'total_messages_comparison.png')
plt.savefig(output_file, dpi=150, bbox_inches='tight')
print(f'Graph saved as {output_file}')

# Message complexity graph
fig3, ax = plt.subplots(figsize=(10, 6))
ax.plot(nodes, centralized_avg_per_cs, 'o-', linewidth=2.5, markersize=10, label='Centralized', color='steelblue')
ax.plot(nodes, token_avg_per_cs, 's-', linewidth=2.5, markersize=10, label='Token-Based Ring', color='darkorange')
ax.set_xlabel('Number of Nodes', fontsize=12)
ax.set_ylabel('Average Messages per Critical Section', fontsize=12)
ax.set_title('Message Complexity Analysis\nMessages per Critical Section Execution', fontsize=14)
ax.legend(fontsize=11)
ax.grid(True, alpha=0.3)
plt.tight_layout()
output_file = os.path.join(output_dir, 'messages_per_cs_comparison.png')
plt.savefig(output_file, dpi=150, bbox_inches='tight')
print(f'Graph saved as {output_file}')

print('\nAll graphs generated successfully!')
