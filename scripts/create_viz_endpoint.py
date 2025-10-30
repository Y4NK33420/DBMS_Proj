#!/usr/bin/env python3
"""
Create a simple HTTP endpoint to serve MovieLens visualization data
This runs alongside the main API server
"""
import psycopg2
import json
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 5432,
    'database': 'movielens',
    'user': 'postgres',
    'password': 'postgres@'
}

class VizHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed_path = urlparse(self.path)
        
        if parsed_path.path == '/viz/movielens':
            self.serve_movielens_data()
        else:
            self.send_error(404, "Not found")
    
    def serve_movielens_data(self):
        try:
            # Connect to PostgreSQL
            conn = psycopg2.connect(**DB_CONFIG)
            cur = conn.cursor()
            
            # Fetch nodes
            cur.execute("""
                SELECT DISTINCT n._0 as id, n._1 as label
                FROM n_g n
                LIMIT 200
            """)
            nodes_raw = cur.fetchall()
            
            # Fetch node properties
            node_props = {}
            cur.execute("""
                SELECT np._0 as id, np._1 as key, np._2 as value
                FROM np_g np
                WHERE np._0 IN (SELECT _0 FROM n_g LIMIT 200)
            """)
            for node_id, key, value in cur.fetchall():
                if node_id not in node_props:
                    node_props[node_id] = {}
                node_props[node_id][key] = value
            
            # Build nodes
            nodes = []
            node_ids = set()
            for node_id, label in nodes_raw:
                node_ids.add(node_id)
                node = {
                    'id': node_id,
                    'label': label,
                    'properties': node_props.get(node_id, {})
                }
                nodes.append(node)
            
            # Fetch edges - using a subquery with the actual node IDs
            node_ids_str = ','.join(str(nid) for nid in node_ids)
            cur.execute(f"""
                SELECT DISTINCT e._0 as id, e._1 as from_id, e._2 as to_id, e._3 as label
                FROM e_g e
                WHERE e._1 IN ({node_ids_str})
                  AND e._2 IN ({node_ids_str})
                LIMIT 500
            """)
            edges_raw = cur.fetchall()
            
            # Fetch edge properties
            edge_props = {}
            edge_ids = [e[0] for e in edges_raw]
            if edge_ids:
                edge_ids_str = ','.join(str(eid) for eid in edge_ids)
                cur.execute(f"""
                    SELECT ep._0 as id, ep._1 as key, ep._2 as value
                    FROM ep_g ep
                    WHERE ep._0 IN ({edge_ids_str})
                """)
            for edge_id, key, value in cur.fetchall():
                if edge_id not in edge_props:
                    edge_props[edge_id] = {}
                edge_props[edge_id][key] = value
            
            # Build edges
            edges = []
            for edge_id, from_id, to_id, label in edges_raw:
                if from_id in node_ids and to_id in node_ids:
                    edge = {
                        'id': edge_id,
                        'from': from_id,
                        'to': to_id,
                        'label': label,
                        'properties': edge_props.get(edge_id, {})
                    }
                    edges.append(edge)
            
            cur.close()
            conn.close()
            
            # Create response
            data = {
                'success': True,
                'nodes': nodes,
                'edges': edges,
                'stats': {
                    'nodeCount': len(nodes),
                    'edgeCount': len(edges)
                }
            }
            
            # Send response
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(json.dumps(data).encode())
            
        except Exception as e:
            self.send_response(500)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            error_data = {'success': False, 'error': str(e)}
            self.wfile.write(json.dumps(error_data).encode())
    
    def log_message(self, format, *args):
        # Suppress default logging
        pass

def run_server(port=8081):
    server = HTTPServer(('0.0.0.0', port), VizHandler)
    print(f"Visualization data server running on http://localhost:{port}")
    print(f"Endpoint: http://localhost:{port}/viz/movielens")
    print("Press Ctrl+C to stop")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nServer stopped")

if __name__ == "__main__":
    run_server()
