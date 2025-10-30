#!/usr/bin/env python3
"""
Load dummy graph data into PG-View database via REST API

Prerequisites:
1. API server must be running on http://localhost:7070
2. PostgreSQL must be installed and configured
3. Run generate_dummy_data.py first to create dummy_graph_data.json

Usage:
    python3 scripts/load_dummy_data.py
"""
import json
import requests
import os
import sys

API_URL = "http://localhost:7070"

def check_api_health():
    """Check if API server is running"""
    try:
        response = requests.get(f"{API_URL}/health", timeout=5)
        return response.status_code == 200
    except requests.exceptions.RequestException:
        return False

def load_data():
    # Check API is running
    if not check_api_health():
        print("❌ Error: API server is not running!")
        print("Please start the API server first:")
        print("  mvn exec:java@api")
        sys.exit(1)
    
    # Load generated data
    data_file = 'scripts/dummy_graph_data.json'
    if not os.path.exists(data_file):
        print(f"❌ Error: {data_file} not found!")
        print("Please run generate_dummy_data.py first:")
        print("  python3 scripts/generate_dummy_data.py")
        sys.exit(1)
    
    with open(data_file, 'r') as f:
        data = json.load(f)
    
    print("="*60)
    print("LOADING DUMMY DATA INTO DATABASE")
    print("="*60)
    
    # Step 1: Connect to PostgreSQL
    print("\n1. Connecting to PostgreSQL...")
    response = requests.post(f"{API_URL}/connect", json={"platform": "pg"})
    if response.json().get('success'):
        print("   ✓ Connected to PostgreSQL")
    else:
        print("   ✗ Failed to connect")
        print(f"   Error: {response.json()}")
        return
    
    # Step 2: Create database
    print("\n2. Creating database 'DummyGraph'...")
    response = requests.post(f"{API_URL}/graph/create", json={"name": "DummyGraph"})
    if response.json().get('success'):
        print("   ✓ Database created")
    else:
        message = response.json().get('message', response.json().get('error', ''))
        if 'already exists' in message.lower() or 'duplicate' in message.lower():
            print(f"   ⚠ Database already exists, will use existing")
        else:
            print(f"   Note: {message}")
    
    # Step 3: Use database
    print("\n3. Selecting database...")
    response = requests.post(f"{API_URL}/graph/use", json={"name": "DummyGraph"})
    if response.json().get('success'):
        print("   ✓ Database selected")
    else:
        print("   ✗ Failed to select database")
        print(f"   Error: {response.json()}")
        return
    
    # Step 4: Add schemas
    print("\n4. Adding schemas...")
    schemas = [
        {"type": "node", "label": "Person"},
        {"type": "node", "label": "Company"},
        {"type": "node", "label": "Product"},
        {"type": "edge", "label": "works_at", "from": "Person", "to": "Company"},
        {"type": "edge", "label": "knows", "from": "Person", "to": "Person"},
        {"type": "edge", "label": "bought", "from": "Person", "to": "Product"},
        {"type": "edge", "label": "produces", "from": "Company", "to": "Product"}
    ]
    
    for schema in schemas:
        if schema["type"] == "node":
            response = requests.post(f"{API_URL}/schema/node", json={"label": schema["label"]})
        else:
            response = requests.post(f"{API_URL}/schema/edge", json={
                "label": schema["label"],
                "from": schema["from"],
                "to": schema["to"]
            })
        if response.json().get('success'):
            print(f"   ✓ Added {schema['type']}: {schema['label']}")
    
    # Step 5: Insert nodes
    print(f"\n5. Inserting {len(data['nodes'])} nodes...")
    node_count = 0
    for i, node in enumerate(data['nodes']):
        # Insert node
        cmd = f"insert N({node['id']}, \"{node['label']}\");"
        response = requests.post(f"{API_URL}/execute", json={"command": cmd})
        
        # Insert node properties
        for prop_key, prop_value in node['properties'].items():
            # Escape quotes in values
            if isinstance(prop_value, str):
                prop_value = prop_value.replace('"', '\\"')
            cmd = f"insert NP({node['id']}, \"{prop_key}\", \"{prop_value}\");"
            requests.post(f"{API_URL}/execute", json={"command": cmd})
        
        node_count += 1
        if (i + 1) % 100 == 0:
            print(f"   Inserted {i + 1}/{len(data['nodes'])} nodes...")
    
    print(f"   ✓ Inserted {node_count} nodes with properties")
    
    # Step 6: Insert edges
    print(f"\n6. Inserting {len(data['edges'])} edges...")
    edge_count = 0
    for i, edge in enumerate(data['edges']):
        # Insert edge
        cmd = f"insert E({edge['id']}, {edge['from']}, {edge['to']}, \"{edge['label']}\");"
        response = requests.post(f"{API_URL}/execute", json={"command": cmd})
        
        # Insert edge properties
        for prop_key, prop_value in edge['properties'].items():
            if isinstance(prop_value, str):
                prop_value = prop_value.replace('"', '\\"')
            cmd = f"insert EP({edge['id']}, \"{prop_key}\", \"{prop_value}\");"
            requests.post(f"{API_URL}/execute", json={"command": cmd})
        
        edge_count += 1
        if (i + 1) % 500 == 0:
            print(f"   Inserted {i + 1}/{len(data['edges'])} edges...")
    
    print(f"   ✓ Inserted {edge_count} edges with properties")
    
    # Step 7: Create sample view
    print("\n7. Creating sample view...")
    view_cmd = """CREATE virtual VIEW SocialNetworkView ON g (
  MATCH (p:Person)-[k:knows]->(p2:Person)
);"""
    response = requests.post(f"{API_URL}/execute", json={"command": view_cmd})
    if response.json().get('success'):
        print("   ✓ Created 'SocialNetworkView'")
    
    # Summary
    print("\n" + "="*60)
    print("DATA LOADING COMPLETE!")
    print("="*60)
    print(f"Database: DummyGraph (PostgreSQL)")
    print(f"Nodes: {node_count}")
    print(f"Edges: {edge_count}")
    print(f"Node Properties: ~{node_count * 4}")
    print(f"Edge Properties: ~{edge_count * 3}")
    print("\nNode Types:")
    for node_type, count in data['summary']['node_types'].items():
        print(f"  - {node_type}: {count}")
    print("\nEdge Types:")
    for edge_type, count in data['summary']['edge_types'].items():
        print(f"  - {edge_type}: {count}")
    print("\n" + "="*60)
    print("Next Steps:")
    print("1. Open Web UI: http://localhost:8080")
    print("2. Go to 'Graphs' tab and select 'DummyGraph'")
    print("3. Go to 'Visualize' tab to see the graph")
    print("="*60)

if __name__ == "__main__":
    try:
        load_data()
    except KeyboardInterrupt:
        print("\n\n⚠ Operation cancelled by user")
        sys.exit(0)
    except Exception as e:
        print(f"\n\n❌ Error: {e}")
        sys.exit(1)

