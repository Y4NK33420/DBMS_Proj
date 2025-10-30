#!/bin/bash
# Run MovieLens Knowledge Graph Visualization

set -e

echo "================================================================================"
echo "MovieLens Knowledge Graph - DBMS Project"
echo "================================================================================"
echo ""

# Check if API server is running
echo "Checking if API server is running..."
if ! curl -s http://localhost:7070/health > /dev/null 2>&1; then
    echo "❌ ERROR: API server is not running on http://localhost:7070"
    echo ""
    echo "Please start the API server first:"
    echo "  cd /home/vainlab/src/DBMS_Proj"
    echo "  mvn exec:java@api"
    echo ""
    echo "Then in another terminal, run this script again."
    exit 1
fi
echo "✓ API server is running"
echo ""

# Load MovieLens data
echo "Loading MovieLens data into database..."
echo "--------------------------------------------------------------------------------"
python3 scripts/load_movielens_data.py

if [ $? -ne 0 ]; then
    echo ""
    echo "❌ ERROR: Failed to load MovieLens data"
    exit 1
fi

echo ""
echo "================================================================================"
echo "SUCCESS! MovieLens data loaded into database"
echo "================================================================================"
echo ""
echo "Access the visualization:"
echo "  1. Open Web UI: http://localhost:8080"
echo "  2. Go to 'Graphs' tab"
echo "  3. Select 'MovieLens' from dropdown"
echo "  4. Go to 'Visualize' tab"
echo "  5. Click 'Visualize' to see the graph"
echo ""
echo "Graph Structure:"
echo "  🔵 Blue nodes = Movies"
echo "  🟣 Purple nodes = Users"
echo "  🟠 Orange nodes = Genres"
echo ""
echo "Relationships:"
echo "  User -[RATED]-> Movie"
echo "  Movie -[HAS_GENRE]-> Genre"
echo ""
echo "================================================================================"
