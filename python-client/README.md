# PG-View Python Client - Knowledge Graph API

Complete documentation for accessing PG-View Knowledge Graph System from Python via REST API.

## Table of Contents

1. [System Overview](#system-overview)
2. [Native GQL Query Language](#native-gql-query-language)
3. [REST API Reference](#rest-api-reference)
4. [Python Client Library](#python-client-library)
5. [Examples](#examples)

---

## System Overview

**PG-View** is a knowledge graph database system with support for transformation views over property graphs. It provides:

- **Multiple Backends**: PostgreSQL, SimpleDatalog, LogicBlox, Neo4j
- **Graph Schema**: Typed nodes and edges
- **Property Support**: Key-value properties on nodes and edges
- **View System**: Virtual, materialized, and hybrid views with transformation capabilities
- **Query Language**: Graph query language (GQL) for pattern matching and querying

### Supported Backends

| Backend | Code | Description |
|---------|------|-------------|
| PostgreSQL | `pg` | Relational database backend with SQL translation |
| SimpleDatalog | `sd` | In-memory Datalog engine (good for testing) |
| LogicBlox | `lb` | Commercial Datalog system (requires LogicBlox installation) |
| Neo4j | `n4` | Native graph database (requires Neo4j installation) |

---

## Native GQL Query Language

### Core Concepts

The native system uses a Graph Query Language (GQL) with the following components:

#### 1. **Graph Structure**
- **Base Graph**: Always named `g`
- **Nodes**: Represented as `N(id, label)`
- **Edges**: Represented as `E(id, src, dst, label)`
- **Node Properties**: `NP(node_id, prop_name, prop_value)`
- **Edge Properties**: `EP(edge_id, prop_name, prop_value)`

#### 2. **Connection Management**

```gql
-- Connect to a backend
connect pg;
connect sd;
connect lb;
connect n4;

-- Disconnect
disconnect;
```

#### 3. **Graph Management**

```gql
-- Create a new graph database
create graph MyGraph;

-- Switch to a graph
use MyGraph;

-- Drop a graph
drop graph MyGraph;

-- List all graphs
list;
```

#### 4. **Schema Definition**

```gql
-- Define node types
create node Person;
create node Company;
create node Concept;

-- Define edge types with source and target constraints
create edge Knows(Person -> Person);
create edge WorksFor(Person -> Company);
create edge RelatesTo(Concept -> Concept);

-- View schema
schema;
```

#### 5. **Data Insertion**

```gql
-- Insert nodes: N(id, "Label")
insert N(1, "Person");
insert N(2, "Person");
insert N(3, "Company");

-- Insert edges: E(id, from_id, to_id, "Label")
insert E(10, 1, 2, "Knows");
insert E(11, 1, 3, "WorksFor");

-- Insert node properties: NP(node_id, "property", "value")
insert NP(1, "name", "Alice");
insert NP(1, "age", "30");
insert NP(2, "name", "Bob");

-- Insert edge properties: EP(edge_id, "property", "value")
insert EP(10, "since", "2020");
insert EP(10, "strength", "strong");
```

#### 6. **Data Import**

```gql
-- Import from CSV files
import N from "/path/to/nodes.csv";
import E from "/path/to/edges.csv";
import NP from "/path/to/node_properties.csv";
import EP from "/path/to/edge_properties.csv";
```

**CSV Format:**
- Nodes: `id,label`
- Edges: `id,from,to,label`
- Properties: `id,key,value`

#### 7. **Querying**

```gql
-- Basic pattern matching
MATCH (a:Person)-[x:Knows]->(b:Person) FROM g RETURN (a),(b),(x);

-- Query with WHERE clause
MATCH (a:Person)-[x:Knows]->(b:Person) 
FROM g 
WHERE a.age > 25 
RETURN (a),(b),(x);

-- Multi-hop patterns
MATCH (a:Person)-[x:Knows]->(b:Person)-[y:Knows]->(c:Person) 
FROM g 
RETURN (a),(b),(c);

-- Path patterns with regex
MATCH (a:Person)-[x:Knows*]->(b:Person) 
FROM g 
RETURN (a),(b);
```

#### 8. **View Creation**

Views allow you to create derived graphs with transformations.

**Selection View (Virtual):**
```gql
-- Simple selection (automatic DEFAULT MAP)
CREATE virtual VIEW FriendsView ON g (
  MATCH (a:Person)-[x:Knows]->(b:Person)
);

-- Query the view
MATCH (a:Person)-[x:Knows]->(b:Person) 
FROM FriendsView 
RETURN (a),(b),(x);
```

**Transformation View:**
```gql
-- Transform labels and structure
CREATE virtual VIEW UserNetwork ON g WITH DEFAULT MAP (
  MATCH (a:Person)-[x:Knows]->(b:Person)
  CONSTRUCT (a:User)-[x:ConnectedTo]->(b:User)
);
```

**View with Skolem Functions (ID generation):**
```gql
CREATE virtual VIEW DerivedView ON g (
  MATCH (a:Person)-[x:Knows]->(b:Person)
  CONSTRUCT (a:Person)-[y:Derived]->(b:Person)
  SET y = SK("derived", x)
);
```

**Mapping Views:**
```gql
-- Map specific nodes/edges
CREATE virtual VIEW MappedView ON g (
  MATCH (a:Person)-[x:Knows]->(b:Person)
  MAP FROM a TO a
  MAP FROM b TO b
  CONSTRUCT (a:Person)-[y:NewRelation]->(b:Person)
  SET y = SK("new", a, b)
);
```

**Add/Delete Operations:**
```gql
-- Add new elements
CREATE virtual VIEW AugmentedView ON g (
  MATCH (a:Person)-[x:Knows]->(b:Person)
  ADD (c:Concept)
  SET c = SK("concept", a, b)
);

-- Delete elements
CREATE virtual VIEW FilteredView ON g (
  MATCH (a:Person)-[x:Knows]->(b:Person)
  DELETE (x)  -- Remove the edge
);
```

**Union Views:**
```gql
CREATE virtual VIEW CombinedView ON g (
  MATCH (a:Person)-[x:Knows]->(b:Person)
  
  UNION
  
  MATCH (a:Person)-[y:WorksWith]->(b:Person)
);
```

**Materialized Views:**
```gql
-- Store results physically for faster access
CREATE materialized VIEW FastView ON g (
  MATCH (a:Person)-[x:Knows]->(b:Person)
);
```

**Hybrid Views:**
```gql
-- Combination of virtual and materialized
CREATE hybrid VIEW HybridView ON g (
  MATCH (a:Person)-[x:Knows]->(b:Person)
);
```

**Views on Views:**
```gql
-- Create cascading views
CREATE virtual VIEW Level1 ON g (
  MATCH (a:Person)-[x:Knows]->(b:Person)
);

CREATE virtual VIEW Level2 ON Level1 (
  MATCH (a:Person)-[x:Knows]->(b:Person)
  WHERE a.age > 25
);
```

#### 9. **View Management**

```gql
-- List all views
views;

-- See the Datalog program
program;

-- Create specialized indexes
create ssr on ViewName;
```

#### 10. **System Commands**

```gql
-- Print current schema
schema;

-- Print EGDs (Equality Generating Dependencies)
egds;

-- Load and execute a script
load "/path/to/script.gql";

-- Enable/disable options
option typecheck on;
option typecheck off;
option prunequery on;
option ivm on;

-- Quit
quit;
```

---

## REST API Reference

### Base URL
```
http://localhost:7070
```

### Endpoints

#### Health Check
```http
GET /health
```

**Response:**
```json
{
  "status": "ok",
  "platform": "pg",
  "version": "1.0.0"
}
```

#### Execute Command
```http
POST /execute
Content-Type: application/json

{
  "command": "schema"
}
```

**Response:**
```json
{
  "success": true,
  "output": "...",
  "command": "schema"
}
```

#### Execute Batch
```http
POST /execute-batch
Content-Type: application/json

{
  "commands": [
    "create node Person",
    "create node Company"
  ]
}
```

**Response:**
```json
{
  "success": true,
  "results": [
    {"success": true, "command": "create node Person", "output": "..."},
    {"success": true, "command": "create node Company", "output": "..."}
  ]
}
```

#### Connect to Platform
```http
POST /connect
Content-Type: application/json

{
  "platform": "pg"
}
```

#### Create Graph
```http
POST /graph/create
Content-Type: application/json

{
  "name": "MyGraph"
}
```

#### Use Graph
```http
POST /graph/use
Content-Type: application/json

{
  "name": "MyGraph"
}
```

#### Drop Graph
```http
DELETE /graph/{name}
```

#### List Graphs
```http
GET /graphs
```

#### Add Node Schema
```http
POST /schema/node
Content-Type: application/json

{
  "label": "Person"
}
```

#### Add Edge Schema
```http
POST /schema/edge
Content-Type: application/json

{
  "label": "Knows",
  "from": "Person",
  "to": "Person"
}
```

#### Get Schema
```http
GET /schema
```

#### Insert Data
```http
POST /data/insert
Content-Type: application/json

{
  "relName": "N",
  "args": "1,\"Person\""
}
```

#### Import CSV
```http
POST /data/import
Content-Type: application/json

{
  "relName": "N",
  "filePath": "/path/to/nodes.csv"
}
```

#### Create View
```http
POST /view/create
Content-Type: application/json

{
  "definition": "CREATE virtual VIEW FriendsView ON g ( MATCH (a:Person)-[x:Knows]->(b:Person) )"
}
```

#### List Views
```http
GET /views
```

#### Execute Query
```http
POST /query
Content-Type: application/json

{
  "query": "MATCH (a:Person)-[x:Knows]->(b:Person) FROM g RETURN (a),(b),(x)"
}
```

**Response:**
```json
{
  "success": true,
  "query": "...",
  "output": "...",
  "resultInfo": "query result #: 2 etime[25] #ofRules: 1"
}
```

#### Get Program
```http
GET /program
```

---

## Python Client Library

### Installation

```bash
# Install required package
pip install requests

# The pgview_client.py file is self-contained
```

### Quick Start

```python
from pgview_client import PGViewClient

# Initialize client
client = PGViewClient("http://localhost:7070")

# Check server health
health = client.health_check()
print(health)

# Setup graph
client.setup_graph("MyKnowledgeGraph", platform="pg")

# Define schema
client.add_node_schema("Entity")
client.add_node_schema("Concept")
client.add_edge_schema("RelatesTo", "Entity", "Entity")

# Insert data
client.insert_node(1, "Entity")
client.insert_node(2, "Entity")
client.insert_edge(10, 1, 2, "RelatesTo")

# Query
result = client.query(
    "MATCH (e1:Entity)-[r:RelatesTo]->(e2:Entity) FROM g RETURN (e1),(e2),(r)"
)
print(result)
```

### Complete Python API

See `pgview_client.py` for full API documentation with docstrings.

---

## Examples

### Example 1: Knowledge Graph for AI Agent

```python
from pgview_client import PGViewClient

# Initialize
client = PGViewClient()
client.setup_graph("AIKnowledgeBase", "pg")

# Define ontology
client.add_node_schema("Entity")
client.add_node_schema("Concept")
client.add_node_schema("Event")
client.add_edge_schema("IsA", "Entity", "Concept")
client.add_edge_schema("RelatesTo", "Entity", "Entity")
client.add_edge_schema("ParticipatesIn", "Entity", "Event")

# Add entities
client.insert_node(1, "Entity")
client.insert_node_property(1, "name", "Alice")
client.insert_node_property(1, "type", "person")

client.insert_node(2, "Entity")
client.insert_node_property(2, "name", "Bob")
client.insert_node_property(2, "type", "person")

client.insert_node(100, "Concept")
client.insert_node_property(100, "name", "Person")

# Add relationships
client.insert_edge(10, 1, 2, "RelatesTo")
client.insert_edge_property(10, "relationship", "knows")

client.insert_edge(11, 1, 100, "IsA")

# Create view for persons only
view_def = """CREATE virtual VIEW PersonNetwork ON g (
  MATCH (e1:Entity)-[r:RelatesTo]->(e2:Entity)
  WHERE e1.type = "person" AND e2.type = "person"
)"""
client.create_view(view_def)

# Query the view
result = client.query(
    "MATCH (e1:Entity)-[r:RelatesTo]->(e2:Entity) FROM PersonNetwork RETURN (e1),(e2),(r)"
)
print(f"Found {result['resultInfo']} relationships")
```

### Example 2: Multi-hop Reasoning

```python
# Define schema
client.add_node_schema("Person")
client.add_edge_schema("Knows", "Person", "Person")
client.add_edge_schema("Trusts", "Person", "Person")

# Add data
for i in range(1, 6):
    client.insert_node(i, "Person")
    client.insert_node_property(i, "name", f"Person{i}")

# Create chain: 1->2->3->4->5
for i in range(1, 5):
    client.insert_edge(10+i, i, i+1, "Knows")

# Create transitive trust view
view_def = """CREATE virtual VIEW TransitiveTrust ON g (
  MATCH (a:Person)-[x:Knows*]->(b:Person)
  CONSTRUCT (a:Person)-[y:Trusts]->(b:Person)
  SET y = SK("trust", a, b)
)"""
client.create_view(view_def)

# Find all trust relationships
result = client.query(
    "MATCH (a:Person)-[t:Trusts]->(b:Person) FROM TransitiveTrust RETURN (a),(b)"
)
```

### Example 3: Batch Operations

```python
# Build graph in batch
commands = [
    "create node Document",
    "create node Topic",
    "create edge About(Document -> Topic)",
    "insert N(1, \"Document\")",
    "insert N(2, \"Document\")",
    "insert N(100, \"Topic\")",
    "insert E(10, 1, 100, \"About\")",
    "insert NP(1, \"title\", \"AI Paper\")",
    "insert NP(100, \"name\", \"Machine Learning\")"
]

result = client.execute_batch(commands)
for r in result['results']:
    print(f"Command: {r['command']}, Success: {r['success']}")
```

---

## Running the API Server

### Start the Server

```bash
cd /home/yankee/src/pg-view
mvn exec:java@api -Dexec.args="conf/graphview.conf"
```

The server will start on `http://localhost:7070`

### Configuration

Edit `conf/graphview.conf` to configure:
- Database connection settings
- Platform-specific options
- Performance tuning parameters

---

## Advanced Features

### View Types

1. **Virtual Views**: Computed on-the-fly (default)
2. **Materialized Views**: Pre-computed and stored
3. **Hybrid Views**: Combination of virtual and materialized
4. **ASR Views**: With Answer Set Rewriting optimization

### Query Optimization

- **Type Checking**: Prune invalid query paths
- **SSR (Simplified Symbolic Rewriting)**: Create optimized indexes
- **IVM (Incremental View Maintenance)**: Update views incrementally

### Advanced Patterns

- **Path Regex**: `(AB)*` for repeating patterns
- **Negation**: `NOT EXISTS` patterns  
- **Aggregation**: Count, sum, etc. (backend-dependent)
- **Property Filters**: Complex WHERE clauses

---

## Troubleshooting

### Server Won't Start
- Check if port 7070 is available
- Verify Java 11+ is installed
- Check configuration file path

### Connection Issues
- Ensure backend (PostgreSQL/Neo4j/LogicBlox) is running
- Verify connection settings in config file
- Check network connectivity

### Query Errors
- Verify syntax matches GQL grammar
- Ensure schema is defined before inserting data
- Check that views exist before querying them

---

## Support & Documentation

- **Main Repository**: Check source code and examples
- **Configuration**: See `conf/graphview.conf`
- **Grammar**: See `src/main/antlr4/.../GraphTransQuery.g4`
- **Paper**: Refer to DBMSPaper.pdf for theoretical background

---

## License

See project LICENSE file.

