"""
PG-View Python Client Library

A Python client for interacting with the PG-View Knowledge Graph REST API.
"""

import requests
from typing import Dict, List, Any, Optional
import json


class PGViewClient:
    """
    Python client for PG-View Knowledge Graph System
    
    This client provides a Python interface to interact with the PG-View REST API,
    allowing you to create graphs, manage schemas, insert data, create views, and execute queries.
    
    Args:
        base_url: The base URL of the PG-View API server (default: http://localhost:7070)
        timeout: Request timeout in seconds (default: 30)
    
    Example:
        >>> client = PGViewClient()
        >>> client.connect("pg")  # Connect to PostgreSQL backend
        >>> client.create_graph("MyKnowledgeGraph")
        >>> client.use_graph("MyKnowledgeGraph")
    """
    
    def __init__(self, base_url: str = "http://localhost:7070", timeout: int = 30):
        self.base_url = base_url.rstrip('/')
        self.timeout = timeout
        self.session = requests.Session()
    
    def _request(self, method: str, endpoint: str, data: Optional[Dict] = None) -> Dict[str, Any]:
        """Make an HTTP request to the API"""
        url = f"{self.base_url}{endpoint}"
        
        try:
            if method == "GET":
                response = self.session.get(url, timeout=self.timeout)
            elif method == "POST":
                response = self.session.post(url, json=data, timeout=self.timeout)
            elif method == "DELETE":
                response = self.session.delete(url, timeout=self.timeout)
            else:
                raise ValueError(f"Unsupported HTTP method: {method}")
            
            response.raise_for_status()
            return response.json()
        
        except requests.exceptions.RequestException as e:
            return {
                "success": False,
                "error": str(e)
            }
    
    def health_check(self) -> Dict[str, Any]:
        """Check if the API server is running"""
        return self._request("GET", "/health")
    
    def connect(self, platform: str) -> Dict[str, Any]:
        """
        Connect to a database backend
        
        Args:
            platform: Backend platform ('pg' for PostgreSQL, 'sd' for SimpleDatalog, 
                     'lb' for LogicBlox, 'n4' for Neo4j)
        
        Returns:
            Response dictionary with success status
            
        Example:
            >>> client.connect("pg")
            {'success': True, 'platform': 'pg', 'message': 'Connected to pg'}
        """
        return self._request("POST", "/connect", {"platform": platform})
    
    def create_graph(self, name: str) -> Dict[str, Any]:
        """
        Create a new graph database
        
        Args:
            name: Name of the graph to create
            
        Returns:
            Response dictionary with success status
            
        Example:
            >>> client.create_graph("MyGraph")
            {'success': True, 'graph': 'MyGraph', 'message': 'Graph created'}
        """
        return self._request("POST", "/graph/create", {"name": name})
    
    def use_graph(self, name: str) -> Dict[str, Any]:
        """
        Switch to using a specific graph
        
        Args:
            name: Name of the graph to use
            
        Returns:
            Response dictionary with success status
        """
        return self._request("POST", "/graph/use", {"name": name})
    
    def drop_graph(self, name: str) -> Dict[str, Any]:
        """
        Drop (delete) a graph database
        
        Args:
            name: Name of the graph to drop
            
        Returns:
            Response dictionary with success status
        """
        return self._request("DELETE", f"/graph/{name}")
    
    def list_graphs(self) -> Dict[str, Any]:
        """
        List all available graphs
        
        Returns:
            Response dictionary with list of graphs
        """
        return self._request("GET", "/graphs")
    
    def add_node_schema(self, label: str) -> Dict[str, Any]:
        """
        Add a node type to the graph schema
        
        Args:
            label: Label for the node type (e.g., "Person", "Entity")
            
        Returns:
            Response dictionary with success status
            
        Example:
            >>> client.add_node_schema("Person")
            {'success': True, 'label': 'Person', 'message': 'Node schema added'}
        """
        return self._request("POST", "/schema/node", {"label": label})
    
    def add_edge_schema(self, label: str, from_node: str, to_node: str) -> Dict[str, Any]:
        """
        Add an edge type to the graph schema
        
        Args:
            label: Label for the edge type (e.g., "Knows", "RelatesTo")
            from_node: Source node type label
            to_node: Target node type label
            
        Returns:
            Response dictionary with success status
            
        Example:
            >>> client.add_edge_schema("Knows", "Person", "Person")
            {'success': True, 'label': 'Knows', 'from': 'Person', 'to': 'Person'}
        """
        return self._request("POST", "/schema/edge", {
            "label": label,
            "from": from_node,
            "to": to_node
        })
    
    def get_schema(self) -> Dict[str, Any]:
        """
        Get the current graph schema
        
        Returns:
            Response dictionary with schema information
        """
        return self._request("GET", "/schema")
    
    def insert_node(self, node_id: int, label: str) -> Dict[str, Any]:
        """
        Insert a node into the graph
        
        Args:
            node_id: Unique identifier for the node
            label: Node type label
            
        Returns:
            Response dictionary with success status
            
        Example:
            >>> client.insert_node(1, "Person")
            {'success': True, 'message': 'Data inserted'}
        """
        return self._request("POST", "/data/insert", {
            "relName": "N",
            "args": f'{node_id},"{label}"'
        })
    
    def insert_edge(self, edge_id: int, from_id: int, to_id: int, label: str) -> Dict[str, Any]:
        """
        Insert an edge into the graph
        
        Args:
            edge_id: Unique identifier for the edge
            from_id: Source node ID
            to_id: Target node ID
            label: Edge type label
            
        Returns:
            Response dictionary with success status
            
        Example:
            >>> client.insert_edge(10, 1, 2, "Knows")
            {'success': True, 'message': 'Data inserted'}
        """
        return self._request("POST", "/data/insert", {
            "relName": "E",
            "args": f'{edge_id},{from_id},{to_id},"{label}"'
        })
    
    def insert_node_property(self, node_id: int, prop_name: str, prop_value: str) -> Dict[str, Any]:
        """
        Insert a property for a node
        
        Args:
            node_id: Node identifier
            prop_name: Property name
            prop_value: Property value
            
        Returns:
            Response dictionary with success status
            
        Example:
            >>> client.insert_node_property(1, "name", "Alice")
            {'success': True, 'message': 'Data inserted'}
        """
        return self._request("POST", "/data/insert", {
            "relName": "NP",
            "args": f'{node_id},"{prop_name}","{prop_value}"'
        })
    
    def insert_edge_property(self, edge_id: int, prop_name: str, prop_value: str) -> Dict[str, Any]:
        """
        Insert a property for an edge
        
        Args:
            edge_id: Edge identifier
            prop_name: Property name
            prop_value: Property value
            
        Returns:
            Response dictionary with success status
        """
        return self._request("POST", "/data/insert", {
            "relName": "EP",
            "args": f'{edge_id},"{prop_name}","{prop_value}"'
        })
    
    def import_csv(self, rel_name: str, file_path: str) -> Dict[str, Any]:
        """
        Import data from a CSV file
        
        Args:
            rel_name: Relation name (N, E, NP, or EP)
            file_path: Path to the CSV file
            
        Returns:
            Response dictionary with success status
        """
        return self._request("POST", "/data/import", {
            "relName": rel_name,
            "filePath": file_path
        })
    
    def create_view(self, view_definition: str) -> Dict[str, Any]:
        """
        Create a view on the graph
        
        Args:
            view_definition: Complete view definition in GQL syntax
            
        Returns:
            Response dictionary with success status
            
        Example:
            >>> view_def = '''CREATE virtual VIEW FriendsView ON g (
            ...   MATCH (a:Person)-[x:Knows]->(b:Person)
            ... )'''
            >>> client.create_view(view_def)
            {'success': True, 'message': 'View created'}
        """
        return self._request("POST", "/view/create", {
            "definition": view_definition
        })
    
    def list_views(self) -> Dict[str, Any]:
        """
        List all views
        
        Returns:
            Response dictionary with view information
        """
        return self._request("GET", "/views")
    
    def query(self, query: str) -> Dict[str, Any]:
        """
        Execute a graph query
        
        Args:
            query: Query in GQL syntax (MATCH ... FROM ... RETURN ...)
            
        Returns:
            Response dictionary with query results
            
        Example:
            >>> result = client.query(
            ...     "MATCH (a:Person)-[x:Knows]->(b:Person) FROM g RETURN (a),(b),(x)"
            ... )
            >>> print(result['resultInfo'])
            'query result #: 2 etime[25] #ofRules: 1'
        """
        return self._request("POST", "/query", {"query": query})
    
    def get_program(self) -> Dict[str, Any]:
        """
        Get the current Datalog program
        
        Returns:
            Response dictionary with program information
        """
        return self._request("GET", "/program")
    
    def execute_command(self, command: str) -> Dict[str, Any]:
        """
        Execute a raw GQL command
        
        Args:
            command: Command in GQL syntax
            
        Returns:
            Response dictionary with command output
            
        Example:
            >>> client.execute_command("schema")
            {'success': True, 'output': '...', 'command': 'schema'}
        """
        return self._request("POST", "/execute", {"command": command})
    
    def execute_batch(self, commands: List[str]) -> Dict[str, Any]:
        """
        Execute multiple commands in batch
        
        Args:
            commands: List of GQL commands
            
        Returns:
            Response dictionary with results for each command
            
        Example:
            >>> commands = [
            ...     "create node Person",
            ...     "create node Company"
            ... ]
            >>> client.execute_batch(commands)
            {'success': True, 'results': [...]}
        """
        return self._request("POST", "/execute-batch", {"commands": commands})
    
    # Convenience methods for common workflows
    
    def setup_graph(self, graph_name: str, platform: str = "pg") -> bool:
        """
        Convenience method to connect to a platform and setup a graph
        
        Args:
            graph_name: Name of the graph to create/use
            platform: Backend platform (default: "pg")
            
        Returns:
            True if successful, False otherwise
        """
        result = self.connect(platform)
        if not result.get("success"):
            return False
        
        self.create_graph(graph_name)  # May fail if exists, but that's ok
        
        result = self.use_graph(graph_name)
        return result.get("success", False)
    
    def create_simple_view(self, view_name: str, 
                          node_label: str, 
                          edge_label: str,
                          base_graph: str = "g") -> Dict[str, Any]:
        """
        Convenience method to create a simple selection view
        
        Args:
            view_name: Name for the view
            node_label: Node type label to match
            edge_label: Edge type label to match
            base_graph: Base graph name (default: "g")
            
        Returns:
            Response dictionary with success status
            
        Example:
            >>> client.create_simple_view("FriendsView", "Person", "Knows")
        """
        view_def = f"""CREATE virtual VIEW {view_name} ON {base_graph} (
  MATCH (a:{node_label})-[x:{edge_label}]->(b:{node_label})
)"""
        return self.create_view(view_def)

