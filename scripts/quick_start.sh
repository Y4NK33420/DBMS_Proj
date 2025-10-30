#!/bin/bash
# Quick Start Script for PG-View with Visualization
# This script sets up everything needed to run PG-View with sample data

set -e  # Exit on error

echo "=========================================="
echo "  PG-VIEW QUICK START SETUP"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check if port is in use
port_in_use() {
    netstat -tuln 2>/dev/null | grep -q ":$1 "
}

# Step 1: Check prerequisites
echo "Step 1: Checking prerequisites..."
echo ""

if ! command_exists java; then
    echo -e "${RED}✗ Java not found${NC}"
    echo "  Please install Java 11+: sudo apt install openjdk-11-jdk"
    exit 1
fi
echo -e "${GREEN}✓ Java found:${NC} $(java -version 2>&1 | head -1)"

if ! command_exists mvn; then
    echo -e "${RED}✗ Maven not found${NC}"
    echo "  Please install Maven: sudo apt install maven"
    exit 1
fi
echo -e "${GREEN}✓ Maven found:${NC} $(mvn -version | head -1)"

if ! command_exists python3; then
    echo -e "${RED}✗ Python3 not found${NC}"
    echo "  Please install Python3: sudo apt install python3"
    exit 1
fi
echo -e "${GREEN}✓ Python3 found:${NC} $(python3 --version)"

if ! command_exists psql; then
    echo -e "${RED}✗ PostgreSQL not found${NC}"
    echo "  Please install PostgreSQL: sudo apt install postgresql-14"
    exit 1
fi
echo -e "${GREEN}✓ PostgreSQL found:${NC} $(psql --version)"

# Check if PostgreSQL is running
if ! systemctl is-active --quiet postgresql; then
    echo -e "${YELLOW}⚠ PostgreSQL is not running${NC}"
    echo "  Starting PostgreSQL..."
    sudo service postgresql start || {
        echo -e "${RED}✗ Failed to start PostgreSQL${NC}"
        exit 1
    }
fi
echo -e "${GREEN}✓ PostgreSQL is running${NC}"

echo ""

# Step 2: Check if ports are available
echo "Step 2: Checking ports..."
echo ""

if port_in_use 7070; then
    echo -e "${YELLOW}⚠ Port 7070 is already in use (API server may be running)${NC}"
else
    echo -e "${GREEN}✓ Port 7070 is available${NC}"
fi

if port_in_use 8080; then
    echo -e "${YELLOW}⚠ Port 8080 is already in use (Web UI may be running)${NC}"
else
    echo -e "${GREEN}✓ Port 8080 is available${NC}"
fi

echo ""

# Step 3: Compile the project
echo "Step 3: Compiling PG-View..."
echo ""

mvn -q clean compile || {
    echo -e "${RED}✗ Compilation failed${NC}"
    exit 1
}
echo -e "${GREEN}✓ Compilation successful${NC}"

echo ""

# Step 4: Start API server in background
echo "Step 4: Starting API server..."
echo ""

if port_in_use 7070; then
    echo -e "${YELLOW}⚠ API server already running on port 7070${NC}"
else
    nohup mvn -q exec:java@api > /tmp/pgview_api.log 2>&1 &
    API_PID=$!
    echo "  API server started (PID: $API_PID)"
    echo "  Waiting for server to initialize..."
    sleep 10
    
    if ps -p $API_PID > /dev/null; then
        echo -e "${GREEN}✓ API server running on port 7070${NC}"
    else
        echo -e "${RED}✗ API server failed to start${NC}"
        echo "  Check logs: tail -f /tmp/pgview_api.log"
        exit 1
    fi
fi

echo ""

# Step 5: Start Web UI server in background
echo "Step 5: Starting Web UI server..."
echo ""

if port_in_use 8080; then
    echo -e "${YELLOW}⚠ Web UI already running on port 8080${NC}"
else
    cd web-ui
    nohup python3 -m http.server 8080 > /tmp/pgview_webui.log 2>&1 &
    WEBUI_PID=$!
    cd ..
    echo "  Web UI server started (PID: $WEBUI_PID)"
    sleep 2
    
    if ps -p $WEBUI_PID > /dev/null; then
        echo -e "${GREEN}✓ Web UI running on port 8080${NC}"
    else
        echo -e "${RED}✗ Web UI failed to start${NC}"
        exit 1
    fi
fi

echo ""

# Step 6: Generate and load dummy data
echo "Step 6: Setting up sample data..."
echo ""

read -p "Do you want to create sample data (500 nodes, 3000+ edges)? [y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "  Generating dummy data..."
    python3 scripts/generate_dummy_data.py || {
        echo -e "${RED}✗ Failed to generate data${NC}"
        exit 1
    }
    
    echo ""
    echo "  Loading data into database (this may take a few minutes)..."
    python3 scripts/load_dummy_data.py || {
        echo -e "${RED}✗ Failed to load data${NC}"
        exit 1
    }
    
    echo -e "${GREEN}✓ Sample data loaded${NC}"
else
    echo "  Skipping sample data creation"
fi

echo ""

# Summary
echo "=========================================="
echo "  SETUP COMPLETE!"
echo "=========================================="
echo ""
echo "🌐 Access your application:"
echo "   Web UI:  http://localhost:8080"
echo "   API:     http://localhost:7070"
echo "   Status:  http://localhost:7070/status"
echo ""
echo "📊 Sample database created: DummyGraph"
echo ""
echo "📖 Next steps:"
echo "   1. Open http://localhost:8080 in your browser"
echo "   2. Go to 'Connection' tab and connect to PostgreSQL"
echo "   3. Go to 'Graphs' tab and select 'DummyGraph'"
echo "   4. Go to 'Visualize' tab to see your graph!"
echo ""
echo "🛠️  Useful commands:"
echo "   View API logs:    tail -f /tmp/pgview_api.log"
echo "   View Web UI logs: tail -f /tmp/pgview_webui.log"
echo "   Stop servers:     pkill -f 'exec:java@api'; pkill -f 'http.server 8080'"
echo ""
echo "📚 Documentation:"
echo "   README_VISUALIZATION.md - Complete guide"
echo "   API_QUICK_START.md      - API reference"
echo "   python-client/README.md - Python client docs"
echo ""
echo "=========================================="

