#!/usr/bin/env python3
"""
Example: Building a Knowledge Graph for an AI Agent using PG-View

This example demonstrates how to use the PG-View Python client to build
a knowledge graph that an AI agent can use for reasoning and information retrieval.
"""

from pgview_client import PGViewClient
import time


def wait_for_server(client, max_retries=10, delay=2):
    """Wait for the API server to be ready"""
    for i in range(max_retries):
        try:
            health = client.health_check()
            if health.get('status') == 'ok':
                print("✓ API server is ready")
                return True
        except:
            if i < max_retries - 1:
                print(f"Waiting for server... ({i+1}/{max_retries})")
                time.sleep(delay)
    return False


def main():
    # Initialize client
    print("=" * 60)
    print("PG-View Knowledge Graph Example")
    print("=" * 60)
    
    client = PGViewClient("http://localhost:7070")
    
    # Check if server is running
    if not wait_for_server(client):
        print("\n❌ Error: API server is not running!")
        print("Please start the server with:")
        print("  mvn exec:java@api -Dexec.args=\"conf/graphview.conf\"")
        return
    
    # Setup graph
    print("\n1. Setting up Knowledge Graph...")
    result = client.setup_graph("AIKnowledgeBase", platform="pg")
    if result:
        print("✓ Graph 'AIKnowledgeBase' created and selected")
    
    # Define schema
    print("\n2. Defining Schema...")
    client.add_node_schema("Entity")
    client.add_node_schema("Concept")
    client.add_node_schema("Document")
    client.add_edge_schema("IsA", "Entity", "Concept")
    client.add_edge_schema("RelatesTo", "Entity", "Entity")
    client.add_edge_schema("Mentions", "Document", "Entity")
    print("✓ Schema defined")
    
    # Insert entities
    print("\n3. Inserting Entities...")
    entities = [
        (1, "Entity", {"name": "Alice", "type": "person", "role": "engineer"}),
        (2, "Entity", {"name": "Bob", "type": "person", "role": "scientist"}),
        (3, "Entity", {"name": "Python", "type": "technology"}),
        (4, "Entity", {"name": "AI", "type": "technology"}),
    ]
    
    for entity_id, label, props in entities:
        client.insert_node(entity_id, label)
        for prop_name, prop_value in props.items():
            client.insert_node_property(entity_id, prop_name, prop_value)
        print(f"  ✓ Added {props.get('name', entity_id)}")
    
    # Insert concepts
    print("\n4. Inserting Concepts...")
    concepts = [
        (100, "Concept", {"name": "Person"}),
        (101, "Concept", {"name": "Technology"}),
    ]
    
    for concept_id, label, props in concepts:
        client.insert_node(concept_id, label)
        for prop_name, prop_value in props.items():
            client.insert_node_property(concept_id, prop_name, prop_value)
        print(f"  ✓ Added concept: {props['name']}")
    
    # Insert documents
    print("\n5. Inserting Documents...")
    documents = [
        (200, "Document", {"title": "AI Research Paper", "year": "2024"}),
        (201, "Document", {"title": "Python Tutorial", "year": "2023"}),
    ]
    
    for doc_id, label, props in documents:
        client.insert_node(doc_id, label)
        for prop_name, prop_value in props.items():
            client.insert_node_property(doc_id, prop_name, prop_value)
        print(f"  ✓ Added document: {props['title']}")
    
    # Insert relationships
    print("\n6. Creating Relationships...")
    relationships = [
        ("IsA", 1, 100, "Alice is a Person"),
        ("IsA", 2, 100, "Bob is a Person"),
        ("IsA", 3, 101, "Python is a Technology"),
        ("IsA", 4, 101, "AI is a Technology"),
        ("RelatesTo", 1, 2, "Alice knows Bob"),
        ("RelatesTo", 1, 3, "Alice uses Python"),
        ("RelatesTo", 2, 4, "Bob researches AI"),
        ("Mentions", 200, 4, "Paper mentions AI"),
        ("Mentions", 200, 2, "Paper mentions Bob"),
        ("Mentions", 201, 3, "Tutorial mentions Python"),
        ("Mentions", 201, 1, "Tutorial mentions Alice"),
    ]
    
    edge_id = 10
    for edge_label, from_id, to_id, description in relationships:
        client.insert_edge(edge_id, from_id, to_id, edge_label)
        client.insert_edge_property(edge_id, "description", description)
        print(f"  ✓ {description}")
        edge_id += 1
    
    # Query the base graph
    print("\n7. Querying Base Graph...")
    print("\n   Query: Find all entities and their types")
    result = client.query(
        "MATCH (e:Entity)-[r:IsA]->(c:Concept) FROM g RETURN (e),(c),(r)"
    )
    print(f"   Result: {result.get('resultInfo', 'N/A')}")
    
    # Create views
    print("\n8. Creating Views...")
    
    # View 1: Person network
    print("\n   Creating PersonNetwork view...")
    view1 = """CREATE virtual VIEW PersonNetwork ON g (
  MATCH (e1:Entity)-[r:RelatesTo]->(e2:Entity)
  WHERE e1.type = "person" AND e2.type = "person"
)"""
    result = client.create_view(view1)
    if result.get('success'):
        print("   ✓ PersonNetwork view created")
    
    # View 2: Technology relationships
    print("\n   Creating TechRelations view...")
    view2 = """CREATE virtual VIEW TechRelations ON g (
  MATCH (e:Entity)-[r:RelatesTo]->(t:Entity)
  WHERE t.type = "technology"
)"""
    result = client.create_view(view2)
    if result.get('success'):
        print("   ✓ TechRelations view created")
    
    # View 3: Document knowledge graph
    print("\n   Creating DocumentKnowledge view...")
    view3 = """CREATE virtual VIEW DocumentKnowledge ON g (
  MATCH (d:Document)-[m:Mentions]->(e:Entity)-[i:IsA]->(c:Concept)
)"""
    result = client.create_view(view3)
    if result.get('success'):
        print("   ✓ DocumentKnowledge view created")
    
    # Query views
    print("\n9. Querying Views...")
    
    print("\n   Query: Find person-to-person relationships")
    result = client.query(
        "MATCH (e1:Entity)-[r:RelatesTo]->(e2:Entity) FROM PersonNetwork RETURN (e1),(e2),(r)"
    )
    print(f"   Result: {result.get('resultInfo', 'N/A')}")
    
    print("\n   Query: Find who uses which technologies")
    result = client.query(
        "MATCH (e:Entity)-[r:RelatesTo]->(t:Entity) FROM TechRelations RETURN (e),(t),(r)"
    )
    print(f"   Result: {result.get('resultInfo', 'N/A')}")
    
    print("\n   Query: Find document-entity-concept relationships")
    result = client.query(
        "MATCH (d:Document)-[m:Mentions]->(e:Entity)-[i:IsA]->(c:Concept) FROM DocumentKnowledge RETURN (d),(e),(c)"
    )
    print(f"   Result: {result.get('resultInfo', 'N/A')}")
    
    # Advanced: Create a derived view
    print("\n10. Creating Advanced View...")
    print("\n   Creating ExpertiseGraph (derived relationships)...")
    view4 = """CREATE virtual VIEW ExpertiseGraph ON g WITH DEFAULT MAP (
  MATCH (p:Entity)-[r1:RelatesTo]->(t:Entity)
  WHERE p.type = "person" AND t.type = "technology"
  CONSTRUCT (p:Entity)-[x:HasExpertiseIn]->(t:Entity)
  SET x = SK("expertise", p, t)
)"""
    result = client.create_view(view4)
    if result.get('success'):
        print("   ✓ ExpertiseGraph view created")
        
        print("\n   Query: Find expertise relationships")
        result = client.query(
            "MATCH (p:Entity)-[x:HasExpertiseIn]->(t:Entity) FROM ExpertiseGraph RETURN (p),(t),(x)"
        )
        print(f"   Result: {result.get('resultInfo', 'N/A')}")
    
    # Summary
    print("\n" + "=" * 60)
    print("Knowledge Graph Summary")
    print("=" * 60)
    
    # Get schema
    schema_result = client.get_schema()
    if schema_result.get('success'):
        print("\n✓ Schema:")
        print(schema_result.get('schema', ''))
    
    # List views
    views_result = client.list_views()
    if views_result.get('success'):
        print("\n✓ Views:")
        print(views_result.get('views', ''))
    
    print("\n" + "=" * 60)
    print("Example Complete!")
    print("=" * 60)
    print("\nYour AI agent can now:")
    print("  • Query entities, concepts, and relationships")
    print("  • Use views for specialized queries")
    print("  • Reason over derived relationships")
    print("  • Scale to millions of entities with PostgreSQL backend")


if __name__ == "__main__":
    main()

