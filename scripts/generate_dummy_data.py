#!/usr/bin/env python3
"""
Generate dummy graph data with ~500 nodes and edges
Nodes: Person, Company, Product (with 2-4 properties)
Edges: works_at, knows, bought (with 2-4 properties)
"""
import random
import json

# Configuration
NUM_PERSONS = 300
NUM_COMPANIES = 100
NUM_PRODUCTS = 100
TOTAL_NODES = NUM_PERSONS + NUM_COMPANIES + NUM_PRODUCTS

# Data for realistic properties
first_names = ["John", "Jane", "Michael", "Sarah", "David", "Emma", "Chris", "Lisa", "Ryan", "Amy", 
               "Alex", "Maria", "Tom", "Laura", "Kevin", "Anna", "Sam", "Nina", "Jake", "Sophia"]
last_names = ["Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Martinez", "Wilson",
              "Anderson", "Taylor", "Thomas", "Moore", "Jackson", "Martin", "Lee", "Thompson", "White", "Harris"]
cities = ["New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia", "San Antonio", "San Diego", 
          "Dallas", "San Jose", "Austin", "Jacksonville", "San Francisco", "Seattle", "Denver"]
companies = ["TechCorp", "DataSystems", "CloudNet", "InfoTech", "CyberSolutions", "WebServices", "AppDev", 
             "SoftwarePlus", "DigitalWorks", "CodeFactory"]
products = ["Laptop", "Smartphone", "Tablet", "Headphones", "Keyboard", "Mouse", "Monitor", "Camera", "Printer", "Speaker"]
departments = ["Engineering", "Sales", "Marketing", "HR", "Finance", "Operations", "IT", "Support"]
skills = ["Python", "Java", "JavaScript", "SQL", "React", "Node.js", "Docker", "AWS", "ML", "DevOps"]

nodes = []
edges = []

# Generate Person nodes (ID: 1-300)
print("Generating Person nodes...")
for i in range(1, NUM_PERSONS + 1):
    node = {
        "id": i,
        "label": "Person",
        "properties": {
            "name": f"{random.choice(first_names)} {random.choice(last_names)}",
            "age": random.randint(22, 65),
            "city": random.choice(cities),
            "email": f"user{i}@example.com"
        }
    }
    nodes.append(node)

# Generate Company nodes (ID: 301-400)
print("Generating Company nodes...")
for i in range(NUM_PERSONS + 1, NUM_PERSONS + NUM_COMPANIES + 1):
    node = {
        "id": i,
        "label": "Company",
        "properties": {
            "name": f"{random.choice(companies)} {i - NUM_PERSONS}",
            "industry": random.choice(["Technology", "Finance", "Healthcare", "Retail", "Manufacturing"]),
            "size": random.choice(["Small", "Medium", "Large", "Enterprise"]),
            "founded": random.randint(1990, 2023)
        }
    }
    nodes.append(node)

# Generate Product nodes (ID: 401-500)
print("Generating Product nodes...")
for i in range(NUM_PERSONS + NUM_COMPANIES + 1, TOTAL_NODES + 1):
    node = {
        "id": i,
        "label": "Product",
        "properties": {
            "name": f"{random.choice(products)} Pro {i - NUM_PERSONS - NUM_COMPANIES}",
            "price": round(random.uniform(50, 2000), 2),
            "category": random.choice(["Electronics", "Computing", "Accessories", "Audio"]),
            "rating": round(random.uniform(3.5, 5.0), 1)
        }
    }
    nodes.append(node)

# Generate edges
print("Generating edges...")
edge_id = 1000

# Person -> Company (works_at)
for person_id in range(1, NUM_PERSONS + 1):
    if random.random() < 0.7:  # 70% employed
        company_id = random.randint(NUM_PERSONS + 1, NUM_PERSONS + NUM_COMPANIES)
        edge = {
            "id": edge_id,
            "from": person_id,
            "to": company_id,
            "label": "works_at",
            "properties": {
                "department": random.choice(departments),
                "years": random.randint(1, 15),
                "role": random.choice(["Engineer", "Manager", "Analyst", "Director", "Specialist"]),
                "salary": random.randint(50000, 200000)
            }
        }
        edges.append(edge)
        edge_id += 1

# Person -> Person (knows)
for person_id in range(1, NUM_PERSONS + 1):
    num_connections = random.randint(2, 10)
    for _ in range(num_connections):
        friend_id = random.randint(1, NUM_PERSONS)
        if friend_id != person_id:
            edge = {
                "id": edge_id,
                "from": person_id,
                "to": friend_id,
                "label": "knows",
                "properties": {
                    "since": random.randint(2010, 2024),
                    "relationship": random.choice(["friend", "colleague", "family", "acquaintance"]),
                    "contact_frequency": random.choice(["daily", "weekly", "monthly", "rarely"])
                }
            }
            edges.append(edge)
            edge_id += 1

# Person -> Product (bought)
for person_id in range(1, NUM_PERSONS + 1):
    num_purchases = random.randint(1, 5)
    for _ in range(num_purchases):
        product_id = random.randint(NUM_PERSONS + NUM_COMPANIES + 1, TOTAL_NODES)
        edge = {
            "id": edge_id,
            "from": person_id,
            "to": product_id,
            "label": "bought",
            "properties": {
                "date": f"2024-{random.randint(1,12):02d}-{random.randint(1,28):02d}",
                "quantity": random.randint(1, 3),
                "review_score": random.randint(1, 5)
            }
        }
        edges.append(edge)
        edge_id += 1

# Company -> Product (produces)
for company_id in range(NUM_PERSONS + 1, NUM_PERSONS + NUM_COMPANIES + 1):
    num_products = random.randint(1, 5)
    for _ in range(num_products):
        product_id = random.randint(NUM_PERSONS + NUM_COMPANIES + 1, TOTAL_NODES)
        edge = {
            "id": edge_id,
            "from": company_id,
            "to": product_id,
            "label": "produces",
            "properties": {
                "since": random.randint(2015, 2024),
                "volume": random.randint(100, 10000)
            }
        }
        edges.append(edge)
        edge_id += 1

data = {
    "nodes": nodes,
    "edges": edges,
    "summary": {
        "total_nodes": len(nodes),
        "total_edges": len(edges),
        "node_types": {
            "Person": NUM_PERSONS,
            "Company": NUM_COMPANIES,
            "Product": NUM_PRODUCTS
        },
        "edge_types": {
            "works_at": sum(1 for e in edges if e["label"] == "works_at"),
            "knows": sum(1 for e in edges if e["label"] == "knows"),
            "bought": sum(1 for e in edges if e["label"] == "bought"),
            "produces": sum(1 for e in edges if e["label"] == "produces")
        }
    }
}

# Save to file
output_file = 'scripts/dummy_graph_data.json'
with open(output_file, 'w') as f:
    json.dump(data, f, indent=2)

print("\n" + "="*50)
print("DUMMY GRAPH DATA GENERATED")
print("="*50)
print(f"Total Nodes: {data['summary']['total_nodes']}")
print(f"  - Person: {data['summary']['node_types']['Person']}")
print(f"  - Company: {data['summary']['node_types']['Company']}")
print(f"  - Product: {data['summary']['node_types']['Product']}")
print(f"\nTotal Edges: {data['summary']['total_edges']}")
for edge_type, count in data['summary']['edge_types'].items():
    print(f"  - {edge_type}: {count}")
print(f"\nData saved to: {output_file}")
print("="*50)
print("\nNext step: Run load_dummy_data.py to load into database")

