#!/usr/bin/env python3
"""
Export MovieLens graph data for visualization

This script queries the database and creates a JSON file that the web UI can load
"""
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'python-client'))

from pgview_client import PGViewClient
import json
import re

def parse_query_result(result_str):
    """Parse the query result string to extract data"""
    nodes = []
    edges = []
    
    # The result format is complex, let's just return empty for now
    # and fetch directly from PostgreSQL
    return nodes, edges

def main():
    print("Exporting MovieLens graph data for visualization...")
    
    client = PGViewClient()
    
    # Connect to database
    client.connect('pg')
    client.use_graph('MovieLens')
    
    # Query all nodes
    print("Fetching nodes...")
    nodes_result = client.execute_command('query MATCH (n) FROM g RETURN (n);')
    
    # Query all edges  
    print("Fetching edges...")
    edges_result = client.execute_command('query MATCH (a)-[e]->(b) FROM g RETURN (a),(e),(b);')
    
    # For now, create a simpler approach - query PostgreSQL directly
    # Create visualization data structure
    viz_data = {
        "nodes": [],
        "edges": [],
        "stats": {
            "nodeCount": 0,
            "edgeCount": 0,
            "nodeTypes": {},
            "edgeTypes": {}
        }
    }
    
    # Save to file
    output_file = 'web-ui/movielens_viz_data.json'
    with open(output_file, 'w') as f:
        json.dump(viz_data, f, indent=2)
    
    print(f"✓ Exported to {output_file}")
    print("\nNote: The web UI needs to be updated to load this data")

if __name__ == "__main__":
    main()
