package edu.upenn.cis.db.graphtrans.api;

import edu.upenn.cis.db.graphtrans.CommandExecutor;
import edu.upenn.cis.db.graphtrans.Config;
import edu.upenn.cis.db.graphtrans.Console;
import edu.upenn.cis.db.graphtrans.GraphTransServer;
import edu.upenn.cis.db.helper.Performance;
import com.google.gson.Gson;
import spark.Request;
import spark.Response;

import java.io.*;
import java.util.*;

import static spark.Spark.*;

/**
 * REST API Server for PG-View Knowledge Graph System
 * 
 * Provides HTTP endpoints for Python and other clients to interact with the graph database
 */
public class GraphViewAPI {
    
    private static Console console;
    private static boolean initialized = false;
    private static final Gson gson = new Gson();
    
    /**
     * Strip ANSI color codes from string for clean JSON output
     */
    private static String stripAnsiCodes(String text) {
        if (text == null) return "";
        // Remove ANSI escape sequences (color codes, cursor movement, etc.)
        return text.replaceAll("\u001B\\[[;\\d]*m", "");
    }
    
    /**
     * Get error message safely (handles null)
     */
    private static String getErrorMessage(Exception e) {
        String msg = e.getMessage();
        return msg != null ? msg : e.getClass().getSimpleName();
    }
    
    public static void main(String[] args) {
        // Initialize the system
        try {
            String configFilePath = args.length > 0 ? args[0] : "conf/graphview.conf";
            initializeSystem(configFilePath);
            
            // Configure Spark
            port(7070);
            
            // Enable CORS - Handle preflight requests
            options("/*", (request, response) -> {
                String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
                if (accessControlRequestHeaders != null) {
                    response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
                }
                
                String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
                if (accessControlRequestMethod != null) {
                    response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
                }
                
                return "OK";
            });
            
            // Enable CORS for all routes
            before((request, response) -> {
                response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
                response.header("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin");
                response.type("application/json");
            });
            
            System.out.println("PG-View REST API Server started on port 7070");
            
            // Health check endpoint
            get("/health", (req, res) -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "ok");
                response.put("platform", Config.getPlatform());
                response.put("version", "1.0.0");
                return gson.toJson(response);
            });
            
            // Execute GQL command
            post("/execute", GraphViewAPI::executeCommand);
            
            // Execute multiple commands
            post("/execute-batch", GraphViewAPI::executeBatch);
            
            // Connect to platform
            post("/connect", GraphViewAPI::connect);
            
            // Create graph
            post("/graph/create", GraphViewAPI::createGraph);
            
            // Use graph
            post("/graph/use", GraphViewAPI::useGraph);
            
            // Drop graph
            delete("/graph/:name", GraphViewAPI::dropGraph);
            
            // List graphs
            get("/graphs", GraphViewAPI::listGraphs);
            
            // Add schema node
            post("/schema/node", GraphViewAPI::addSchemaNode);
            
            // Add schema edge
            post("/schema/edge", GraphViewAPI::addSchemaEdge);
            
            // Get schema
            get("/schema", GraphViewAPI::getSchema);
            
            // Insert data
            post("/data/insert", GraphViewAPI::insertData);
            
            // Import from CSV
            post("/data/import", GraphViewAPI::importCSV);
            
            // Create view
            post("/view/create", GraphViewAPI::createView);
            
            // List views
            get("/views", GraphViewAPI::listViews);
            
            // Execute query
            post("/query", GraphViewAPI::executeQuery);
            
            // Get program
            get("/program", GraphViewAPI::getProgram);
            
            // Get system status (platform, current graph, persistence info)
            get("/status", GraphViewAPI::getSystemStatus);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void initializeSystem(String configFilePath) throws Exception {
        Config.load(configFilePath);
        Config.initialize();
        
        // Set sensible defaults for API mode
        Config.setUseQuerySubQueryInPostgres(false);
        Config.setTypeCheckEnabled(false);
        Config.setTypeCheckPruningEnabled(false);
        Config.setSubQueryPruningEnabled(false);
        Config.setAnswerEnabled(true);
        Config.setUseMatchSSIndex(false);
        
        // Note: Do NOT force a specific platform here - let the connect endpoint handle it
        // The user will choose the platform via /connect endpoint
        // Config.setPostgresEnabled() and Config.setUseSimpleDatalogEngine() are legacy settings
        // that don't actually prevent platform switching via CommandExecutor.connect()
        
        GraphTransServer.initialize();
        Config.setWorkspace("API");
        Performance.setup(Config.getWorkspace(), "API Server");
        
        console = new Console();
        console.setEnabled(false);
        CommandExecutor.setConsole(console);
        
        initialized = true;
        
        System.out.println("[API] Initialized. Use /connect endpoint to select platform (pg, sd, lb, n4)");
    }
    
    private static String executeCommand(Request req, Response res) {
        try {
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String command = body.get("command");
            
            if (command == null || command.trim().isEmpty()) {
                res.status(400);
                return gson.toJson(Map.of("error", "Command is required"));
            }
            
            // Capture output
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream old = System.out;
            System.setOut(ps);
            
            try {
                CommandExecutor.run(command);
                System.out.flush();
                String output = stripAnsiCodes(baos.toString());
                
                return gson.toJson(Map.of(
                    "success", true,
                    "output", output,
                    "command", command
                ));
            } finally {
                System.setOut(old);
            }
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of(
                "error", e.getMessage(),
                "type", e.getClass().getSimpleName()
            ));
        }
    }
    
    private static String executeBatch(Request req, Response res) {
        try {
            Map<String, Object> body = gson.fromJson(req.body(), Map.class);
            List<String> commands = (List<String>) body.get("commands");
            
            if (commands == null || commands.isEmpty()) {
                res.status(400);
                return gson.toJson(Map.of("error", "Commands array is required"));
            }
            
            List<Map<String, Object>> results = new ArrayList<>();
            
            for (String command : commands) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                PrintStream old = System.out;
                System.setOut(ps);
                
                try {
                    CommandExecutor.run(command);
                    System.out.flush();
                    String output = stripAnsiCodes(baos.toString());
                    
                    results.add(Map.of(
                        "success", true,
                        "command", command,
                        "output", output
                    ));
                } catch (Exception e) {
                    results.add(Map.of(
                        "success", false,
                        "command", command,
                        "error", e.getMessage()
                    ));
                } finally {
                    System.setOut(old);
                }
            }
            
            return gson.toJson(Map.of(
                "success", true,
                "results", results
            ));
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String connect(Request req, Response res) {
        try {
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String platform = body.get("platform");
            
            if (platform == null) {
                res.status(400);
                return gson.toJson(Map.of("error", "Platform is required (pg, sd, lb, n4)"));
            }
            
            CommandExecutor.run("connect " + platform);
            
            return gson.toJson(Map.of(
                "success", true,
                "platform", platform,
                "message", "Connected to " + platform
            ));
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String createGraph(Request req, Response res) {
        try {
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String graphName = body.get("name");
            
            if (graphName == null) {
                res.status(400);
                return gson.toJson(Map.of("error", "Graph name is required"));
            }
            
            CommandExecutor.run("create graph " + graphName);
            
            return gson.toJson(Map.of(
                "success", true,
                "graph", graphName,
                "message", "Graph created"
            ));
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String useGraph(Request req, Response res) {
        try {
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String graphName = body.get("name");
            
            if (graphName == null) {
                res.status(400);
                return gson.toJson(Map.of("error", "Graph name is required"));
            }
            
            CommandExecutor.run("use " + graphName);
            
            return gson.toJson(Map.of(
                "success", true,
                "graph", graphName,
                "message", "Using graph " + graphName
            ));
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String dropGraph(Request req, Response res) {
        try {
            String graphName = req.params(":name");
            CommandExecutor.run("drop graph " + graphName);
            
            return gson.toJson(Map.of(
                "success", true,
                "graph", graphName,
                "message", "Graph dropped"
            ));
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String listGraphs(Request req, Response res) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream old = System.out;
            System.setOut(ps);
            
            try {
                CommandExecutor.run("list");
                System.out.flush();
                String output = stripAnsiCodes(baos.toString());
                
                return gson.toJson(Map.of(
                    "success", true,
                    "output", output
                ));
            } finally {
                System.setOut(old);
            }
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String addSchemaNode(Request req, Response res) {
        try {
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String label = body.get("label");
            
            if (label == null) {
                res.status(400);
                return gson.toJson(Map.of("error", "Label is required"));
            }
            
            CommandExecutor.run("create node " + label);
            
            return gson.toJson(Map.of(
                "success", true,
                "label", label,
                "message", "Node schema added"
            ));
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String addSchemaEdge(Request req, Response res) {
        try {
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String label = body.get("label");
            String from = body.get("from");
            String to = body.get("to");
            
            if (label == null || from == null || to == null) {
                res.status(400);
                return gson.toJson(Map.of("error", "Label, from, and to are required"));
            }
            
            CommandExecutor.run("create edge " + label + "(" + from + " -> " + to + ")");
            
            return gson.toJson(Map.of(
                "success", true,
                "label", label,
                "from", from,
                "to", to,
                "message", "Edge schema added"
            ));
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String getSchema(Request req, Response res) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream old = System.out;
            System.setOut(ps);
            
            try {
                CommandExecutor.run("schema");
                System.out.flush();
                String output = stripAnsiCodes(baos.toString());
                
                return gson.toJson(Map.of(
                    "success", true,
                    "schema", output
                ));
            } finally {
                System.setOut(old);
            }
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String insertData(Request req, Response res) {
        try {
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String relName = body.get("relName");
            String args = body.get("args");
            
            if (relName == null || args == null) {
                res.status(400);
                return gson.toJson(Map.of("error", "relName and args are required"));
            }
            
            CommandExecutor.run("insert " + relName + "(" + args + ")");
            
            return gson.toJson(Map.of(
                "success", true,
                "message", "Data inserted"
            ));
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String importCSV(Request req, Response res) {
        try {
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String relName = body.get("relName");
            String filePath = body.get("filePath");
            
            if (relName == null || filePath == null) {
                res.status(400);
                return gson.toJson(Map.of("error", "relName and filePath are required"));
            }
            
            CommandExecutor.run("import " + relName + " from \"" + filePath + "\"");
            
            return gson.toJson(Map.of(
                "success", true,
                "message", "Data imported from " + filePath
            ));
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String createView(Request req, Response res) {
        try {
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String viewDefinition = body.get("definition");
            
            if (viewDefinition == null) {
                res.status(400);
                return gson.toJson(Map.of("error", "View definition is required"));
            }
            
            CommandExecutor.run(viewDefinition);
            
            return gson.toJson(Map.of(
                "success", true,
                "message", "View created"
            ));
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String listViews(Request req, Response res) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream old = System.out;
            System.setOut(ps);
            
            try {
                CommandExecutor.run("views");
                System.out.flush();
                String output = stripAnsiCodes(baos.toString());
                
                return gson.toJson(Map.of(
                    "success", true,
                    "views", output
                ));
            } finally {
                System.setOut(old);
            }
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String executeQuery(Request req, Response res) {
        try {
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String query = body.get("query");
            
            if (query == null) {
                res.status(400);
                return gson.toJson(Map.of("error", "Query is required"));
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream old = System.out;
            System.setOut(ps);
            
            try {
                CommandExecutor.run(query);
                System.out.flush();
                String output = stripAnsiCodes(baos.toString());
                
                // Extract query result count if available
                String resultLine = Arrays.stream(output.split("\n"))
                    .filter(line -> line.contains("query result #:"))
                    .findFirst()
                    .orElse("");
                
                return gson.toJson(Map.of(
                    "success", true,
                    "query", query,
                    "output", output,
                    "resultInfo", resultLine
                ));
            } finally {
                System.setOut(old);
            }
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String getProgram(Request req, Response res) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream old = System.out;
            System.setOut(ps);
            
            try {
                CommandExecutor.run("program");
                System.out.flush();
                String output = stripAnsiCodes(baos.toString());
                
                return gson.toJson(Map.of(
                    "success", true,
                    "program", output
                ));
            } finally {
                System.setOut(old);
            }
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
    
    private static String getSystemStatus(Request req, Response res) {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // Platform information
            String platform = Config.getPlatform();
            status.put("platform", platform);
            
            // Platform descriptions
            Map<String, String> platformInfo = new HashMap<>();
            platformInfo.put("pg", "PostgreSQL (persistent storage in database)");
            platformInfo.put("sd", "Simple Datalog (in-memory only, data lost on restart)");
            platformInfo.put("lb", "LogicBlox (persistent storage)");
            platformInfo.put("n4", "Neo4j (persistent storage)");
            status.put("platformDescription", platformInfo.get(platform));
            
            // Current graph
            String currentGraph = Config.getWorkspace();
            if (currentGraph != null && !currentGraph.equals("API") && !currentGraph.equals("SYN")) {
                status.put("currentGraph", currentGraph);
            } else {
                status.put("currentGraph", null);
            }
            
            // Get list of graphs
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            PrintStream old = System.out;
            System.setOut(ps);
            
            try {
                CommandExecutor.run("list");
                System.out.flush();
                String output = stripAnsiCodes(baos.toString());
                status.put("graphs", output.replace("List of graphs: ", "").replace("[", "").replace("]", "").trim());
            } finally {
                System.setOut(old);
            }
            
            // Get schema if graph is in use
            if (status.get("currentGraph") != null) {
                baos = new ByteArrayOutputStream();
                ps = new PrintStream(baos);
                old = System.out;
                System.setOut(ps);
                
                try {
                    CommandExecutor.run("schema");
                    System.out.flush();
                    String schemaOutput = stripAnsiCodes(baos.toString());
                    status.put("schema", schemaOutput);
                } finally {
                    System.setOut(old);
                }
            }
            
            // Persistence warning
            if ("sd".equals(platform)) {
                status.put("warning", "Simple Datalog stores data in memory only. All data will be lost when the server restarts!");
            }
            
            // Usage notes
            Map<String, String> usageNotes = new HashMap<>();
            usageNotes.put("insert", "Use 'insert N(id, label)' for nodes and 'insert E(id, from, to, label)' for edges (not N_g or E_g)");
            usageNotes.put("query", "Use 'MATCH (a:Label)-[r:RelType]->(b:Label) FROM g RETURN (a),(b),(r)'");
            usageNotes.put("view", "Use 'CREATE virtual VIEW ViewName ON g ( MATCH ... )'");
            status.put("usageNotes", usageNotes);
            
            return gson.toJson(status);
            
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("error", getErrorMessage(e)));
        }
    }
}
