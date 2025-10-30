#!/usr/bin/env python3
"""
Query MovieLens database and show statistics

Usage:
    python3 scripts/query_movielens.py
"""
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'python-client'))

from pgview_client import PGViewClient

def main():
    print("="*60)
    print("MovieLens Knowledge Graph - Query Examples")
    print("="*60)
    
    client = PGViewClient()
    
    # Check health
    health = client.health_check()
    if not health.get('success'):
        print("❌ API server not running!")
        return
    
    print("\n✓ Connected to API server")
    
    # Connect and use graph
    client.connect('pg')
    client.use_graph('MovieLens')
    
    print("\n" + "="*60)
    print("QUERY 1: Count all nodes by type")
    print("="*60)
    
    # Count movies
    result = client.query('MATCH (m:Movie) FROM g RETURN (m)')
    if result.get('success'):
        print(f"Movies: {result.get('resultInfo', '').split('#:')[1].split()[0] if 'resultInfo' in result else 'N/A'}")
    
    # Count users
    result = client.query('MATCH (u:User) FROM g RETURN (u)')
    if result.get('success'):
        print(f"Users: {result.get('resultInfo', '').split('#:')[1].split()[0] if 'resultInfo' in result else 'N/A'}")
    
    # Count genres
    result = client.query('MATCH (g:Genre) FROM g RETURN (g)')
    if result.get('success'):
        print(f"Genres: {result.get('resultInfo', '').split('#:')[1].split()[0] if 'resultInfo' in result else 'N/A'}")
    
    print("\n" + "="*60)
    print("QUERY 2: Sample movies with their genres")
    print("="*60)
    
    result = client.query('MATCH (m:Movie)-[h:HAS_GENRE]->(g:Genre) FROM g RETURN (m),(g)')
    if result.get('success'):
        print(f"✓ Found movie-genre relationships")
        print(f"  Result info: {result.get('resultInfo', 'N/A')}")
    
    print("\n" + "="*60)
    print("QUERY 3: Sample user ratings")
    print("="*60)
    
    result = client.query('MATCH (u:User)-[r:RATED]->(m:Movie) FROM g RETURN (u),(r),(m)')
    if result.get('success'):
        print(f"✓ Found user ratings")
        print(f"  Result info: {result.get('resultInfo', 'N/A')}")
    
    print("\n" + "="*60)
    print("QUERY 4: Check views")
    print("="*60)
    
    views = client.list_views()
    if views.get('success'):
        print("✓ Available views:")
        if 'views' in views:
            for view in views['views']:
                print(f"  - {view}")
    
    print("\n" + "="*60)
    print("SUCCESS! MovieLens graph is ready")
    print("="*60)
    print("\nVisualization:")
    print("  1. Open: http://localhost:8080")
    print("  2. Go to 'Graphs' tab")
    print("  3. Select 'MovieLens'")
    print("  4. Go to 'Visualize' tab")
    print("  5. Adjust node limit (try 100-200)")
    print("  6. Click 'Visualize'")
    print("\nYou should see:")
    print("  🔵 Blue nodes = Movies")
    print("  🟣 Purple nodes = Users")
    print("  🟠 Orange nodes = Genres")
    print("="*60)

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
