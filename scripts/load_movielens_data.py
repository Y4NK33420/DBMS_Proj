#!/usr/bin/env python3
"""
Load MovieLens dataset into PG-View database via REST API

Prerequisites:
1. API server must be running on http://localhost:7070
2. PostgreSQL must be installed and configured

Usage:
    python3 scripts/load_movielens_data.py
"""
import json
import requests
import os
import sys
import zipfile
import csv
from collections import defaultdict

API_URL = "http://localhost:7070"
MOVIELENS_URL = "https://files.grouplens.org/datasets/movielens/ml-latest-small.zip"
DATA_DIR = "movielens_data"

def check_api_health():
    """Check if API server is running"""
    try:
        response = requests.get(f"{API_URL}/health", timeout=5)
        return response.status_code == 200
    except requests.exceptions.RequestException:
        return False

def download_movielens():
    """Download and extract MovieLens dataset"""
    os.makedirs(DATA_DIR, exist_ok=True)
    zip_path = os.path.join(DATA_DIR, "ml-latest-small.zip")
    
    if not os.path.exists(zip_path):
        print("   Downloading MovieLens dataset...")
        response = requests.get(MOVIELENS_URL, stream=True)
        response.raise_for_status()
        with open(zip_path, 'wb') as f:
            for chunk in response.iter_content(chunk_size=8192):
                f.write(chunk)
        print("   ✓ Downloaded")
    else:
        print("   ✓ Dataset already downloaded")
    
    extract_dir = os.path.join(DATA_DIR, "ml-latest-small")
    if not os.path.exists(extract_dir):
        print("   Extracting...")
        with zipfile.ZipFile(zip_path, 'r') as zip_ref:
            zip_ref.extractall(DATA_DIR)
        print("   ✓ Extracted")
    else:
        print("   ✓ Dataset already extracted")
    
    return extract_dir

def load_movies(data_dir, limit=500):
    """Load movies from CSV"""
    movies_file = os.path.join(data_dir, "movies.csv")
    movies = {}
    
    with open(movies_file, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for i, row in enumerate(reader):
            if i >= limit:
                break
            movie_id = int(row['movieId'])
            genres = row['genres'].split('|') if row['genres'] != '(no genres listed)' else []
            movies[movie_id] = {
                'id': movie_id,
                'title': row['title'],
                'genres': genres
            }
    
    return movies

def load_ratings(data_dir, movie_ids, limit=5000):
    """Load ratings from CSV"""
    ratings_file = os.path.join(data_dir, "ratings.csv")
    ratings = []
    
    with open(ratings_file, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for i, row in enumerate(reader):
            if i >= limit:
                break
            movie_id = int(row['movieId'])
            if movie_id in movie_ids:
                ratings.append({
                    'userId': int(row['userId']),
                    'movieId': movie_id,
                    'rating': float(row['rating']),
                    'timestamp': int(row['timestamp'])
                })
    
    return ratings

def load_data():
    # Check API is running
    if not check_api_health():
        print("❌ Error: API server is not running!")
        print("Please start the API server first:")
        print("  cd /home/vainlab/src/DBMS_Proj")
        print("  mvn exec:java@api")
        sys.exit(1)
    
    print("="*60)
    print("LOADING MOVIELENS DATA INTO DATABASE")
    print("="*60)
    
    # Step 1: Download dataset
    print("\n1. Downloading MovieLens dataset...")
    data_dir = download_movielens()
    
    # Step 2: Load data from CSV
    print("\n2. Loading data from CSV files...")
    movies = load_movies(data_dir, limit=500)
    print(f"   ✓ Loaded {len(movies)} movies")
    
    movie_ids = set(movies.keys())
    ratings = load_ratings(data_dir, movie_ids, limit=5000)
    print(f"   ✓ Loaded {len(ratings)} ratings")
    
    # Get unique users and genres
    users = set(r['userId'] for r in ratings)
    all_genres = set()
    for movie in movies.values():
        all_genres.update(movie['genres'])
    
    print(f"   ✓ Found {len(users)} users")
    print(f"   ✓ Found {len(all_genres)} genres")
    
    # Step 3: Connect to PostgreSQL
    print("\n3. Connecting to PostgreSQL...")
    response = requests.post(f"{API_URL}/connect", json={"platform": "pg"})
    if response.json().get('success'):
        print("   ✓ Connected to PostgreSQL")
    else:
        print("   ✗ Failed to connect")
        print(f"   Error: {response.json()}")
        return
    
    # Step 4: Create database
    print("\n4. Creating database 'MovieLens'...")
    response = requests.post(f"{API_URL}/graph/create", json={"name": "MovieLens"})
    if response.json().get('success'):
        print("   ✓ Database created")
    else:
        message = response.json().get('message', response.json().get('error', ''))
        if 'already exists' in message.lower() or 'duplicate' in message.lower():
            print(f"   ⚠ Database already exists, will use existing")
        else:
            print(f"   Note: {message}")
    
    # Step 5: Use database
    print("\n5. Selecting database...")
    response = requests.post(f"{API_URL}/graph/use", json={"name": "MovieLens"})
    if response.json().get('success'):
        print("   ✓ Database selected")
    else:
        print("   ✗ Failed to select database")
        print(f"   Error: {response.json()}")
        return
    
    # Step 6: Add schemas
    print("\n6. Adding schemas...")
    schemas = [
        {"type": "node", "label": "Movie"},
        {"type": "node", "label": "User"},
        {"type": "node", "label": "Genre"},
        {"type": "edge", "label": "RATED", "from": "User", "to": "Movie"},
        {"type": "edge", "label": "HAS_GENRE", "from": "Movie", "to": "Genre"}
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
    
    # Step 7: Create node ID mappings
    print("\n7. Creating node mappings...")
    node_id = 1
    movie_id_map = {}
    user_id_map = {}
    genre_id_map = {}
    
    # Map movies
    for movie_id_orig in movies.keys():
        movie_id_map[movie_id_orig] = node_id
        node_id += 1
    
    # Map users
    for user_id_orig in users:
        user_id_map[user_id_orig] = node_id
        node_id += 1
    
    # Map genres
    for genre in sorted(all_genres):
        genre_id_map[genre] = node_id
        node_id += 1
    
    print(f"   ✓ Mapped {len(movie_id_map)} movies")
    print(f"   ✓ Mapped {len(user_id_map)} users")
    print(f"   ✓ Mapped {len(genre_id_map)} genres")
    
    # Step 8: Insert Movie nodes
    print(f"\n8. Inserting {len(movies)} Movie nodes...")
    for i, (movie_id_orig, movie_data) in enumerate(movies.items()):
        node_id = movie_id_map[movie_id_orig]
        
        # Insert node
        cmd = f'insert N({node_id}, "Movie");'
        requests.post(f"{API_URL}/execute", json={"command": cmd})
        
        # Insert properties
        title = movie_data['title'].replace('"', '\\"')
        cmd = f'insert NP({node_id}, "title", "{title}");'
        requests.post(f"{API_URL}/execute", json={"command": cmd})
        
        cmd = f'insert NP({node_id}, "movieId", "{movie_id_orig}");'
        requests.post(f"{API_URL}/execute", json={"command": cmd})
        
        if (i + 1) % 100 == 0:
            print(f"   Inserted {i + 1}/{len(movies)} movies...")
    
    print(f"   ✓ Inserted {len(movies)} Movie nodes")
    
    # Step 9: Insert User nodes
    print(f"\n9. Inserting {len(users)} User nodes...")
    for i, user_id_orig in enumerate(users):
        node_id = user_id_map[user_id_orig]
        
        # Insert node
        cmd = f'insert N({node_id}, "User");'
        requests.post(f"{API_URL}/execute", json={"command": cmd})
        
        # Insert property
        cmd = f'insert NP({node_id}, "userId", "{user_id_orig}");'
        requests.post(f"{API_URL}/execute", json={"command": cmd})
        
        if (i + 1) % 50 == 0:
            print(f"   Inserted {i + 1}/{len(users)} users...")
    
    print(f"   ✓ Inserted {len(users)} User nodes")
    
    # Step 10: Insert Genre nodes
    print(f"\n10. Inserting {len(all_genres)} Genre nodes...")
    for genre in sorted(all_genres):
        node_id = genre_id_map[genre]
        
        # Insert node
        cmd = f'insert N({node_id}, "Genre");'
        requests.post(f"{API_URL}/execute", json={"command": cmd})
        
        # Insert property
        cmd = f'insert NP({node_id}, "name", "{genre}");'
        requests.post(f"{API_URL}/execute", json={"command": cmd})
    
    print(f"   ✓ Inserted {len(all_genres)} Genre nodes")
    
    # Step 11: Insert RATED edges
    print(f"\n11. Inserting {len(ratings)} RATED edges...")
    edge_id = 1
    for i, rating in enumerate(ratings):
        from_id = user_id_map[rating['userId']]
        to_id = movie_id_map[rating['movieId']]
        
        # Insert edge
        cmd = f'insert E({edge_id}, {from_id}, {to_id}, "RATED");'
        requests.post(f"{API_URL}/execute", json={"command": cmd})
        
        # Insert properties
        cmd = f'insert EP({edge_id}, "rating", "{rating["rating"]}");'
        requests.post(f"{API_URL}/execute", json={"command": cmd})
        
        cmd = f'insert EP({edge_id}, "timestamp", "{rating["timestamp"]}");'
        requests.post(f"{API_URL}/execute", json={"command": cmd})
        
        edge_id += 1
        
        if (i + 1) % 500 == 0:
            print(f"   Inserted {i + 1}/{len(ratings)} ratings...")
    
    print(f"   ✓ Inserted {len(ratings)} RATED edges")
    
    # Step 12: Insert HAS_GENRE edges
    print(f"\n12. Inserting HAS_GENRE edges...")
    genre_edge_count = 0
    for movie_id_orig, movie_data in movies.items():
        movie_node_id = movie_id_map[movie_id_orig]
        for genre in movie_data['genres']:
            genre_node_id = genre_id_map[genre]
            
            # Insert edge
            cmd = f'insert E({edge_id}, {movie_node_id}, {genre_node_id}, "HAS_GENRE");'
            requests.post(f"{API_URL}/execute", json={"command": cmd})
            
            edge_id += 1
            genre_edge_count += 1
    
    print(f"   ✓ Inserted {genre_edge_count} HAS_GENRE edges")
    
    # Step 13: Create views
    print("\n13. Creating views...")
    
    # View 1: All movies with genres
    view_cmd = """CREATE virtual VIEW MovieGenreView ON g (
  MATCH (m:Movie)-[h:HAS_GENRE]->(g:Genre)
);"""
    response = requests.post(f"{API_URL}/execute", json={"command": view_cmd})
    if response.json().get('success'):
        print("   ✓ Created 'MovieGenreView'")
    
    # View 2: Highly rated movies
    view_cmd = """CREATE virtual VIEW HighlyRatedView ON g (
  MATCH (u:User)-[r:RATED]->(m:Movie)
);"""
    response = requests.post(f"{API_URL}/execute", json={"command": view_cmd})
    if response.json().get('success'):
        print("   ✓ Created 'HighlyRatedView'")
    
    # Summary
    total_edges = len(ratings) + genre_edge_count
    print("\n" + "="*60)
    print("DATA LOADING COMPLETE!")
    print("="*60)
    print(f"Database: MovieLens (PostgreSQL)")
    print(f"Total Nodes: {len(movies) + len(users) + len(all_genres)}")
    print(f"  - Movies: {len(movies)}")
    print(f"  - Users: {len(users)}")
    print(f"  - Genres: {len(all_genres)}")
    print(f"Total Edges: {total_edges}")
    print(f"  - RATED: {len(ratings)}")
    print(f"  - HAS_GENRE: {genre_edge_count}")
    print("\nViews Created:")
    print("  - MovieGenreView")
    print("  - HighlyRatedView")
    print("\n" + "="*60)
    print("Next Steps:")
    print("1. Open Web UI: http://localhost:8080")
    print("2. Go to 'Graphs' tab and select 'MovieLens'")
    print("3. Go to 'Visualize' tab to see the graph")
    print("4. Try different views and queries")
    print("="*60)

if __name__ == "__main__":
    try:
        load_data()
    except KeyboardInterrupt:
        print("\n\n⚠ Operation cancelled by user")
        sys.exit(0)
    except Exception as e:
        print(f"\n\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
