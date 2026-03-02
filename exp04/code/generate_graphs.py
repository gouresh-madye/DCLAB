#!/usr/bin/env python3
"""
Generate graphs for Non-Token Based Mutual Exclusion Experiment
Compares Centralized vs Ricart-Agrawala algorithms
"""

import matplotlib.pyplot as plt
import numpy as np

# Data from experiments
nodes = [2, 3, 5, 8, 10]

# Centralized Algorithm Results (O(1) complexity)
centralized_total_messages = [34, 51, 85, 136, 170]
centralized_avg_per_cs = [3.40, 3.40, 3.40, 3.40, 3.40]
centralized_latency = [78.20, 80.87, 84.16, 81.68, 80.44]

# Ricart-Agrawala Algorithm Results (O(N) complexity)
ra_total_messages = [20, 60, 200, 560, 900]
ra_avg_per_cs = [2.00, 4.00, 8.00, 14.00, 18.00]
ra_latency = [142.50, 223.67, 364.80, 572.63, 769.80]
ra_theoretical = [2, 4, 8, 14, 18]  # 2(N-1)

# Create comprehensive figure
fig, axes = plt.subplots(2, 2, figsize=(14, 10))
fig.suptitle('Non-Token Based Mutual Exclusion - Centralized vs Ricart-Agrawala', 
             fontsize=14, fontweight='bold')

# Plot 1: Nodes vs Total Messages
ax1 = axes[0, 0]
x = np.arange(len(nodes))
width = 0.35
bars1 = ax1.bar(x - width/2, centralized_total_messages, width, label='Centralized', color='steelblue')
bars2 = ax1.bar(x + width/2, ra_total_messages, width, label='Ricart-Agrawala', color='darkorange')
ax1.set_xlabel('Number of Nodes')
ax1.set_ylabel('Total Messages')
ax1.set_title('Total Messages vs Number of Nodes')
ax1.set_xticks(x)
ax1.set_xticklabels(nodes)
ax1.legend()
ax1.grid(axis='y', alpha=0.3)

# Plot 2: Messages per CS - Showing O(1) vs O(N) clearly
ax2 = axes[0, 1]
ax2.plot(nodes, centralized_avg_per_cs, 'o-', linewidth=2.5, markersize=10, 
         label='Centralized O(1)', color='steelblue')
ax2.plot(nodes, ra_avg_per_cs, 's-', linewidth=2.5, markersize=10, 
         label='Ricart-Agrawala O(N)', color='darkorange')
ax2.plot(nodes, ra_theoretical, 'x--', linewidth=1.5, markersize=8, 
         label='Theoretical 2(N-1)', color='red', alpha=0.7)
ax2.set_xlabel('Number of Nodes')
ax2.set_ylabel('Messages per Critical Section')
ax2.set_title('Message Complexity: O(1) vs O(N)')
ax2.legend()
ax2.grid(True, alpha=0.3)

# Plot 3: Latency Comparison
ax3 = axes[1, 0]
ax3.plot(nodes, centralized_latency, 'o-', linewidth=2.5, markersize=10, 
         label='Centralized', color='steelblue')
ax3.plot(nodes, ra_latency, 's-', linewidth=2.5, markersize=10, 
         label='Ricart-Agrawala', color='darkorange')
ax3.set_xlabel('Number of Nodes')
ax3.set_ylabel('Average Latency (ms)')
ax3.set_title('Critical Section Latency vs Nodes')
ax3.legend()
ax3.grid(True, alpha=0.3)

# Plot 4: Theoretical vs Actual (Ricart-Agrawala)
ax4 = axes[1, 1]
width = 0.35
x = np.arange(len(nodes))
bars3 = ax4.bar(x - width/2, ra_theoretical, width, label='Theoretical 2(N-1)', color='lightcoral', alpha=0.8)
bars4 = ax4.bar(x + width/2, ra_avg_per_cs, width, label='Actual', color='darkorange')
ax4.set_xlabel('Number of Nodes')
ax4.set_ylabel('Messages per CS')
ax4.set_title('Ricart-Agrawala: Theoretical vs Actual')
ax4.set_xticks(x)
ax4.set_xticklabels(nodes)
ax4.legend()
ax4.grid(axis='y', alpha=0.3)

# Add value annotations
for i, (t, a) in enumerate(zip(ra_theoretical, ra_avg_per_cs)):
    ax4.annotate(f'{t}', xy=(x[i] - width/2, t), ha='center', va='bottom', fontsize=9)
    ax4.annotate(f'{a:.0f}', xy=(x[i] + width/2, a), ha='center', va='bottom', fontsize=9)

plt.tight_layout()
plt.savefig('../results/graphs/non_token_analysis.png', dpi=150, bbox_inches='tight')
print('Graph saved as results/graphs/non_token_analysis.png')

# Create focused message complexity graph
fig2, ax = plt.subplots(figsize=(10, 6))
ax.plot(nodes, centralized_avg_per_cs, 'o-', linewidth=3, markersize=12, 
        label='Centralized [O(1)]', color='steelblue')
ax.plot(nodes, ra_avg_per_cs, 's-', linewidth=3, markersize=12, 
        label='Ricart-Agrawala [O(N)]', color='darkorange')
ax.plot(nodes, ra_theoretical, 'x--', linewidth=2, markersize=10, 
        label='Theoretical 2(N-1)', color='red', alpha=0.7)
ax.set_xlabel('Number of Nodes (N)', fontsize=12)
ax.set_ylabel('Messages per Critical Section', fontsize=12)
ax.set_title('Message Complexity Comparison\nCentralized O(1) vs Ricart-Agrawala O(N)', fontsize=14)
ax.legend(fontsize=11)
ax.grid(True, alpha=0.3)

# Add annotations
for i, (c, r) in enumerate(zip(centralized_avg_per_cs, ra_avg_per_cs)):
    ax.annotate(f'{c:.1f}', (nodes[i], c), textcoords="offset points", 
                xytext=(0, 10), ha='center', fontsize=9)
    ax.annotate(f'{r:.0f}', (nodes[i], r), textcoords="offset points", 
                xytext=(0, 10), ha='center', fontsize=9, color='darkorange')

plt.tight_layout()
plt.savefig('../results/graphs/message_complexity_comparison.png', dpi=150, bbox_inches='tight')
print('Graph saved as results/graphs/message_complexity_comparison.png')

# Create total messages bar chart
fig3, ax = plt.subplots(figsize=(10, 6))
x = np.arange(len(nodes))
width = 0.35
bars1 = ax.bar(x - width/2, centralized_total_messages, width, 
               label='Centralized', color='steelblue')
bars2 = ax.bar(x + width/2, ra_total_messages, width, 
               label='Ricart-Agrawala', color='darkorange')
ax.set_xlabel('Number of Nodes', fontsize=12)
ax.set_ylabel('Total Messages', fontsize=12)
ax.set_title('Total Messages Comparison\nCentralized vs Ricart-Agrawala', fontsize=14)
ax.set_xticks(x)
ax.set_xticklabels(nodes)
ax.legend(fontsize=11)
ax.grid(axis='y', alpha=0.3)

# Add value labels
for bar in bars1:
    height = bar.get_height()
    ax.annotate(f'{int(height)}', xy=(bar.get_x() + bar.get_width() / 2, height),
                xytext=(0, 3), textcoords="offset points", ha='center', va='bottom', fontsize=9)
for bar in bars2:
    height = bar.get_height()
    ax.annotate(f'{int(height)}', xy=(bar.get_x() + bar.get_width() / 2, height),
                xytext=(0, 3), textcoords="offset points", ha='center', va='bottom', fontsize=9)

plt.tight_layout()
plt.savefig('../results/graphs/total_messages_comparison.png', dpi=150, bbox_inches='tight')
print('Graph saved as results/graphs/total_messages_comparison.png')

# Create latency comparison
fig4, ax = plt.subplots(figsize=(10, 6))
ax.plot(nodes, centralized_latency, 'o-', linewidth=3, markersize=12, 
        label='Centralized', color='steelblue')
ax.plot(nodes, ra_latency, 's-', linewidth=3, markersize=12, 
        label='Ricart-Agrawala', color='darkorange')
ax.set_xlabel('Number of Nodes', fontsize=12)
ax.set_ylabel('Average Latency per CS (ms)', fontsize=12)
ax.set_title('Latency Comparison\nCentralized vs Ricart-Agrawala', fontsize=14)
ax.legend(fontsize=11)
ax.grid(True, alpha=0.3)
plt.tight_layout()
plt.savefig('../results/graphs/latency_comparison.png', dpi=150, bbox_inches='tight')
print('Graph saved as results/graphs/latency_comparison.png')

print('\nAll graphs generated successfully!')
print('\nKey Findings:')
print('- Centralized: Constant 3.4 messages/CS regardless of nodes')
print('- Ricart-Agrawala: Linear growth matching 2(N-1) exactly')
print('- Latency increases significantly with Ricart-Agrawala due to message overhead')
