import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.time.Duration;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.LinkedHashSet;

// ============================================================================
// PURE JAVA HTTP SERVER (Bridges Gemini AI <-> SQLite DB <-> Java DSA Engine)
// ============================================================================

@SuppressWarnings("all")
public class JupiterServer {

    // Your Gemini API Key from Google AI Studio
    public static final String GEMINI_API_KEY = "AQ.Ab8RN6IFJ3aCuCK01TSfHmHzeIXCeOSafTHjZ0db7lYWt0yF8A";

    private static final CustomLinkedList documentList = new CustomLinkedList();
    private static final CustomStack searchHistoryStack = new CustomStack();
    private static final CustomTrie suggestionTrie = new CustomTrie();

    // Reusable HTTP client for fast Gemini API queries
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    // Pre-populate Trie with popular Computer Science & Astronomy keywords
    static {
        String[] defaults = {
            "Binary Search Algorithm",
            "Linear Search Algorithm",
            "Bubble Sort Algorithm",
            "Insertion Sort Algorithm",
            "Selection Sort Algorithm",
            "Merge Sort Algorithm",
            "Quick Sort Algorithm",
            "Heap Sort Algorithm",
            "Radix Sort Algorithm",
            "Counting Sort Algorithm",
            "Shell Sort Algorithm",
            "Bucket Sort Algorithm",
            "Linked List Implementation",
            "Custom Stack DSA",
            "Queue Data Structure",
            "Binary Search Tree",
            "AVL Tree Balancing",
            "Red-Black Tree",
            "Graph Breadth First Search (BFS)",
            "Graph Depth First Search (DFS)",
            "Graph Dijkstra Algorithm",
            "Jupiter Gas Giant",
            "Jupiter Moons Europa & Io",
            "Jupiter Great Red Spot",
            "Saturn Ring System",
            "Black Hole Singularity",
            "Big Bang Theory",
            "Solar System Exploration"
        };
        for (String s : defaults) {
            suggestionTrie.insert(s);
        }
    }

    // ========================================================================
    // PHASE 1: CUSTOM SINGLY LINKED LIST, STACK & TRIE (Pure Java DSA)
    // ========================================================================

    public static class CustomLinkedList {
        static class Node {
            String title;
            String description;
            double score;
            long timestamp;
            Node next;

            Node(String title, String description, double score, long timestamp) {
                this.title = title;
                this.description = description;
                this.score = score;
                this.timestamp = (timestamp > 0) ? timestamp : System.currentTimeMillis();
            }

            Node(String title, String description, double score) {
                this(title, description, score, System.currentTimeMillis());
            }
        }

        private Node head;

        public synchronized void insert(String title, String description, double score) {
            insert(title, description, score, System.currentTimeMillis());
        }

        public synchronized void insert(String title, String description, double score, long timestamp) {
            Node newNode = new Node(title, description, score, timestamp);
            if (head == null) {
                head = newNode;
                return;
            }
            Node temp = head;
            while (temp.next != null) {
                temp = temp.next;
            }
            temp.next = newNode;
        }

        // ====================================================================
        // PURE DSA MERGE SORT ON SINGLY LINKED LIST: O(n log n) Guaranteed Time
        // Stable sort supporting: "score_desc", "score_asc", "title_az", "date_desc"
        // ====================================================================
        public synchronized void mergeSort(String criteria) {
            head = mergeSortRec(head, (criteria == null) ? "score_desc" : criteria);
        }

        private Node mergeSortRec(Node h, String criteria) {
            if (h == null || h.next == null) {
                return h;
            }

            // 1. Find midpoint of the list using Fast & Slow pointers
            Node middle = getMiddle(h);
            Node nextOfMiddle = middle.next;
            middle.next = null; // Split list into two halves

            // 2. Recursively split and sort left and right halves
            Node left = mergeSortRec(h, criteria);
            Node right = mergeSortRec(nextOfMiddle, criteria);

            // 3. Merge two sorted halves together
            return sortedMerge(left, right, criteria);
        }

        private Node getMiddle(Node h) {
            if (h == null) return h;
            Node slow = h;
            Node fast = h.next;
            while (fast != null && fast.next != null) {
                slow = slow.next;
                fast = fast.next.next;
            }
            return slow;
        }

        private Node sortedMerge(Node a, Node b, String criteria) {
            if (a == null) return b;
            if (b == null) return a;

            boolean aComesFirst = compareNodes(a, b, criteria);

            Node result;
            if (aComesFirst) {
                result = a;
                result.next = sortedMerge(a.next, b, criteria);
            } else {
                result = b;
                result.next = sortedMerge(a, b.next, criteria);
            }
            return result;
        }

        private boolean compareNodes(Node a, Node b, String criteria) {
            if ("title_az".equalsIgnoreCase(criteria)) {
                return a.title.compareToIgnoreCase(b.title) <= 0;
            } else if ("date_desc".equalsIgnoreCase(criteria)) {
                return a.timestamp >= b.timestamp; // Newest first
            } else if ("score_asc".equalsIgnoreCase(criteria)) {
                return a.score <= b.score; // Low to High
            } else {
                // Default: "score_desc" (Highest score first)
                return a.score >= b.score;
            }
        }

        // Traverses the Linked List and converts matching nodes into a valid JSON Array with sorting
        public synchronized String searchAndToJson(String query, String sortBy) {
            if (sortBy != null && !sortBy.trim().isEmpty()) {
                mergeSort(sortBy);
            }
            return searchAndToJson(query);
        }

        // Traverses the Linked List and converts matching nodes into a valid JSON Array
        public synchronized String searchAndToJson(String query) {
            StringBuilder json = new StringBuilder("[");
            Node current = head;
            boolean first = true;
            String q = (query == null) ? "" : query.trim().toLowerCase();

            while (current != null) {
                boolean matches = q.isEmpty()
                    || (current.title != null && current.title.toLowerCase().contains(q))
                    || (current.description != null && current.description.toLowerCase().contains(q));

                if (matches) {
                    if (!first) json.append(",");
                    // Use Locale.US to guarantee standard dot decimal separators in JSON (e.g., 98.5)
                    json.append(String.format(
                        Locale.US,
                        "{\"title\":\"%s\",\"description\":\"%s\",\"score\":%.1f,\"timestamp\":%d,\"date\":\"%s\"}",
                        JupiterServer.escapeJson(current.title),
                        JupiterServer.escapeJson(current.description),
                        current.score,
                        current.timestamp,
                        JupiterServer.formatTimestamp(current.timestamp)
                    ));
                    first = false;
                }
                current = current.next;
            }
            json.append("]");
            return json.toString();
        }

        public synchronized int size() {
            int count = 0;
            Node temp = head;
            while (temp != null) {
                count++;
                temp = temp.next;
            }
            return count;
        }
    }

    public static class CustomStack {
        private String[] stack = new String[50];
        private int top = -1;

        public synchronized void push(String query) {
            // Automatically double capacity when array is full
            if (top >= stack.length - 1) {
                String[] newStack = new String[stack.length * 2];
                System.arraycopy(stack, 0, newStack, 0, stack.length);
                stack = newStack;
            }
            stack[++top] = query;
        }

        public synchronized String peek() {
            return (top >= 0) ? stack[top] : "None";
        }

        public synchronized int size() {
            return top + 1;
        }
    }

    // Custom Trie (Prefix Tree) for Sub-millisecond Auto-Suggestion in Pure Java DSA
    public static class CustomTrie {
        static class TrieNode {
            Map<Character, TrieNode> children = new TreeMap<>();
            boolean isEndOfWord = false;
            Set<String> fullWords = new LinkedHashSet<>();
        }

        private final TrieNode root = new TrieNode();

        // Inserts the full phrase AND every suffix starting from each word boundary
        // E.g., "Bubble Sort Algorithm" will be indexed under:
        // 1. "bubble sort algorithm" -> "Bubble Sort Algorithm"
        // 2. "sort algorithm"        -> "Bubble Sort Algorithm"
        // 3. "algorithm"             -> "Bubble Sort Algorithm"
        public synchronized void insert(String word) {
            if (word == null || word.trim().isEmpty()) return;
            String clean = word.trim();
            String[] tokens = clean.split("\\s+");
            for (int i = 0; i < tokens.length; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = i; j < tokens.length; j++) {
                    if (j > i) sb.append(" ");
                    sb.append(tokens[j]);
                }
                insertPath(sb.toString().toLowerCase(), clean);
            }
        }

        private void insertPath(String key, String originalPhrase) {
            TrieNode current = root;
            for (char c : key.toCharArray()) {
                current.children.putIfAbsent(c, new TrieNode());
                current = current.children.get(c);
            }
            current.isEndOfWord = true;
            current.fullWords.add(originalPhrase);
        }

        public synchronized List<String> searchPrefix(String prefix, int limit) {
            Set<String> results = new LinkedHashSet<>();
            if (prefix == null || prefix.trim().isEmpty()) return new ArrayList<>();
            String p = prefix.trim().toLowerCase();

            TrieNode current = root;
            for (char c : p.toCharArray()) {
                if (!current.children.containsKey(c)) {
                    return new ArrayList<>(results);
                }
                current = current.children.get(c);
            }

            collectWords(current, results, limit);
            return new ArrayList<>(results);
        }

        private void collectWords(TrieNode node, Set<String> results, int limit) {
            if (node == null || results.size() >= limit) return;
            if (node.isEndOfWord) {
                for (String w : node.fullWords) {
                    results.add(w);
                    if (results.size() >= limit) return;
                }
            }
            for (TrieNode child : node.children.values()) {
                collectWords(child, results, limit);
                if (results.size() >= limit) return;
            }
        }
    }

    // Retrieve combined auto-suggestions from both DSA Trie and SQLite database
    public static List<String> getAutoSuggestions(String prefix, int limit) {
        Set<String> resultSet = new LinkedHashSet<>();
        if (prefix == null || prefix.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String clean = prefix.trim();

        // 1. First get prefix & sub-word matches from Custom Trie (instant memory lookup)
        List<String> trieMatches = suggestionTrie.searchPrefix(clean, limit);
        resultSet.addAll(trieMatches);

        // 2. Fetch matches from SQLite search_history & documents if room permits
        if (resultSet.size() < limit) {
            try (Connection conn = DBConnect.getConnection()) {
                // Search history matches (case-insensitive substring)
                String historySql = "SELECT DISTINCT query FROM search_history WHERE LOWER(query) LIKE ? ORDER BY id DESC LIMIT ?";
                try (PreparedStatement pstmt = conn.prepareStatement(historySql)) {
                    pstmt.setString(1, "%" + clean.toLowerCase() + "%");
                    pstmt.setInt(2, limit);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next() && resultSet.size() < limit) {
                            resultSet.add(rs.getString("query"));
                        }
                    }
                }

                // Document title matches (case-insensitive substring)
                if (resultSet.size() < limit) {
                    String docSql = "SELECT title FROM documents WHERE LOWER(title) LIKE ? LIMIT ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(docSql)) {
                        pstmt.setString(1, "%" + clean.toLowerCase() + "%");
                        pstmt.setInt(2, limit);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next() && resultSet.size() < limit) {
                                resultSet.add(rs.getString("title"));
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                // Ignore DB errors for suggestion endpoint to maintain speed
            }
        }

        return new ArrayList<>(resultSet);
    }

    // Initialize SQLite database, create tables, seed documents, and restore history
    public static void initDatabase() {
        try (Connection conn = DBConnect.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Create documents table if it doesn't exist
            stmt.execute("CREATE TABLE IF NOT EXISTS documents ("
                       + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                       + "title TEXT NOT NULL, "
                       + "description TEXT NOT NULL, "
                       + "score REAL NOT NULL, "
                       + "timestamp INTEGER DEFAULT 0);");

            // Ensure timestamp column exists if table was created previously
            try {
                stmt.execute("ALTER TABLE documents ADD COLUMN timestamp INTEGER DEFAULT 0;");
            } catch (SQLException ignored) {}

            // 2. Create search_history table if it doesn't exist
            stmt.execute("CREATE TABLE IF NOT EXISTS search_history ("
                       + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                       + "query TEXT NOT NULL, "
                       + "searched_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

            // 3. Check if documents table is empty, and seed sample data
            try (ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) AS total FROM documents")) {
                if (rsCount.next() && rsCount.getInt("total") == 0) {
                    String seedSql = "INSERT INTO documents (title, description, score, timestamp) VALUES (?, ?, ?, ?)";
                    long now = System.currentTimeMillis();
                    try (PreparedStatement pstmt = conn.prepareStatement(seedSql)) {
                        insertDoc(pstmt, "Java Data Structures & Algorithms", "Mastering Stack, Queue, Linked List, Binary Tree, and Graph in pure Java.", 99.0, now - 3600000L);
                        insertDoc(pstmt, "Jupiter Gas Giant", "Fifth planet from the Sun and the largest in the Solar System.", 98.5, now - 7200000L);
                        insertDoc(pstmt, "Jupiter Moons (Europa & Io)", "Jupiter has 95 known moons, including Europa with its subsurface ocean.", 94.0, now - 14400000L);
                        insertDoc(pstmt, "Saturn Ring System", "Sixth planet from the Sun, famous for its icy planetary rings.", 91.2, now - 28800000L);
                    }
                    System.out.println("[DB LOG] Seeded initial sample documents into jupiter.db");
                }
            }

            // 4. Load all documents from SQLite into Custom Linked List & Trie
            try (ResultSet rsDocs = stmt.executeQuery("SELECT title, description, score, timestamp FROM documents")) {
                int docCount = 0;
                while (rsDocs.next()) {
                    String title = rsDocs.getString("title");
                    long ts = rsDocs.getLong("timestamp");
                    if (ts <= 0) {
                        ts = System.currentTimeMillis() - ((long) docCount * 86_400_000L);
                    }
                    documentList.insert(
                        title,
                        rsDocs.getString("description"),
                        rsDocs.getDouble("score"),
                        ts
                    );
                    suggestionTrie.insert(title);
                    docCount++;
                }
                // Initial sort using pure DSA Merge Sort (Highest score first)
                documentList.mergeSort("score_desc");
                System.out.println("[DB LOG] Loaded " + docCount + " documents into CustomLinkedList & Trie (Merge Sorted by Score).");
            }

            // 5. Restore past search history from SQLite into Custom Stack & Trie
            try (ResultSet rsHistory = stmt.executeQuery("SELECT query FROM search_history ORDER BY id ASC")) {
                while (rsHistory.next()) {
                    String pastQuery = rsHistory.getString("query");
                    searchHistoryStack.push(pastQuery);
                    suggestionTrie.insert(pastQuery);
                }
                System.out.println("[DB LOG] Restored " + searchHistoryStack.size() + " past searches into Stack & Trie.");
            }

        } catch (SQLException e) {
            System.err.println("[DB ERROR] SQLite initialization error: " + e.getMessage());
            // Fallback: seed in-memory if database connection fails
            long now = System.currentTimeMillis();
            documentList.insert("Java Data Structures & Algorithms", "Mastering Stack, Queue, Linked List, Binary Tree, and Graph in pure Java.", 99.0, now - 3600000L);
            documentList.insert("Jupiter Gas Giant", "Fifth planet from the Sun and the largest in the Solar System.", 98.5, now - 7200000L);
            documentList.insert("Jupiter Moons (Europa & Io)", "Jupiter has 95 known moons, including Europa with its subsurface ocean.", 94.0, now - 14400000L);
            documentList.insert("Saturn Ring System", "Sixth planet from the Sun, famous for its icy planetary rings.", 91.2, now - 28800000L);
            documentList.mergeSort("score_desc");
        }
    }

    private static void insertDoc(PreparedStatement pstmt, String title, String desc, double score, long timestamp) throws SQLException {
        pstmt.setString(1, title);
        pstmt.setString(2, desc);
        pstmt.setDouble(3, score);
        pstmt.setLong(4, timestamp);
        pstmt.executeUpdate();
    }

    // Automatically save search query entered by the user into SQLite jupiter.db
    public static void saveSearchQuery(String query) {
        String sql = "INSERT INTO search_history (query) VALUES (?)";
        try (Connection conn = DBConnect.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, query);
            pstmt.executeUpdate();
            suggestionTrie.insert(query);
            System.out.println("[DB LOG] Saved search query to jupiter.db: \"" + query + "\"");
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Failed to save search query to jupiter.db: " + e.getMessage());
        }
    }

    // ========================================================================
    // GEMINI PRO / FLASH AI SEARCH ENGINE
    // ========================================================================
    public static String queryGeminiAI(String query) {
        if (GEMINI_API_KEY == null || GEMINI_API_KEY.trim().isEmpty()) {
            return null;
        }

        try {
            String prompt = "You are Jupiter AI Search Engine. For the search query: '" + query.replace("\"", "\\\"") + "', "
                          + "generate 2-3 accurate, informative search results. "
                          + "Return ONLY a valid JSON array of objects. Each object must have: "
                          + "\"title\": a short clear title, "
                          + "\"description\": a 2-3 sentence informative explanation, "
                          + "\"score\": a relevance score number between 90.0 and 99.9. "
                          + "Return strictly the JSON array, no extra commentary or markdown formatting.";

            String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt.replace("\n", " ").replace("\"", "\\\"") + "\"}]}]}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=" + GEMINI_API_KEY))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(35))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractJsonArray(response.body());
            } else {
                System.err.println("[GEMINI ERROR] HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (IOException e) {
            System.err.println("[GEMINI ERROR] Network request failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[GEMINI ERROR] Request interrupted: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[GEMINI ERROR] Unexpected error: " + e.getMessage());
        }
        return null;
    }

    // Accurately extracts the inner JSON array [...] from Gemini API's response
    private static String extractJsonArray(String raw) {
        if (raw == null) return null;
        int textIdx = raw.indexOf("\"text\"");
        if (textIdx == -1) textIdx = 0;
        int start = raw.indexOf('[', textIdx);
        if (start == -1) return null;

        int depth = 0;
        int end = -1;
        boolean inQuotes = false;
        boolean escape = false;

        for (int i = start; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (!inQuotes) {
                if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
        }

        if (end != -1) {
            String extracted = raw.substring(start, end + 1);
            extracted = extracted.replace("\\\"", "\"")
                                 .replace("\\n", "\n")
                                 .replace("\\r", "");
            if (extracted.contains("```json")) extracted = extracted.replace("```json", "");
            if (extracted.contains("```")) extracted = extracted.replace("```", "");
            return extracted.trim();
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        // 1. Initialize SQLite database & seed data
        initDatabase();

        // 2. Create Pure Java Server on Port 8080
        int port = 8080;
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (java.net.BindException e) {
            System.err.println("====================================================");
            System.err.println("  ❌ ERROR: Port " + port + " is already in use!");
            System.err.println("  👉 A Java server process is already running on port 8080.");
            System.err.println("  👉 Stop the previous terminal in VS Code to run a new one.");
            System.err.println("====================================================");
            return;
        }

        // Add shutdown hook to close server cleanly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));

        // 3. Register endpoints: /search, /suggest, /add-document, /documents, / (static files)
        server.createContext("/search", new SearchHandler());
        server.createContext("/suggest", new SuggestHandler());
        server.createContext("/add-document", new AddDocumentHandler());
        server.createContext("/documents", new DocumentsHandler());
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // Default executor
        server.start();

        System.out.println("====================================================");
        System.out.println("  JUPITER AI SEARCH ENGINE RUNNING ON PORT " + port);
        System.out.println("  Endpoints: /search, /suggest, /add-document, /documents");
        System.out.println("  Powered by: Google Gemini AI + Pure Java DSA (Trie & LinkedList)");
        System.out.println("  Test URL: http://localhost:8080/search?q=jupiter");
        System.out.println("====================================================");
    }

    // JSON String sanitizer to prevent invalid JSON payloads
    public static String escapeJson(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // Format epoch milliseconds into a human-readable date string
    public static String formatTimestamp(long millis) {
        if (millis <= 0) return "Recently";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        return sdf.format(new Date(millis));
    }

    // Endpoint for Auto-Suggestions
    static class SuggestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                URI requestURI = exchange.getRequestURI();
                String rawQuery = requestURI.getRawQuery();
                String prefix = "";

                if (rawQuery != null) {
                    for (String param : rawQuery.split("&")) {
                        String[] pair = param.split("=", 2);
                        if (pair.length == 2 && "q".equals(pair[0])) {
                            prefix = URLDecoder.decode(pair[1], StandardCharsets.UTF_8).trim();
                            break;
                        }
                    }
                }

                List<String> suggestions = getAutoSuggestions(prefix, 6);
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < suggestions.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append("\"").append(escapeJson(suggestions.get(i))).append("\"");
                }
                json.append("]");

                byte[] responseBytes = json.toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } finally {
                exchange.close();
            }
        }
    }

    // Endpoint for Search Execution
    static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Set CORS headers to allow browser requests
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

                // Handle HTTP OPTIONS preflight request
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                // Extract query parameters "q", "mode", and "sort"
                URI requestURI = exchange.getRequestURI();
                String rawQuery = requestURI.getRawQuery();
                String searchWord = "";
                String searchMode = "auto"; // "auto" (default) or "offline"
                String sortBy = "score_desc"; // "score_desc", "title_az", "date_desc", "score_asc"

                if (rawQuery != null) {
                    for (String param : rawQuery.split("&")) {
                        String[] pair = param.split("=", 2);
                        if (pair.length == 2) {
                            if ("q".equals(pair[0])) {
                                searchWord = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                            } else if ("mode".equals(pair[0])) {
                                searchMode = URLDecoder.decode(pair[1], StandardCharsets.UTF_8).toLowerCase();
                            } else if ("sort".equals(pair[0])) {
                                sortBy = URLDecoder.decode(pair[1], StandardCharsets.UTF_8).toLowerCase();
                            }
                        }
                    }
                }

                // Measure execution time in milliseconds
                long startTime = System.nanoTime();

                // 1. Automatically save query to SQLite database, Stack, and Trie
                if (!searchWord.trim().isEmpty()) {
                    saveSearchQuery(searchWord.trim());
                    searchHistoryStack.push(searchWord.trim());
                }

                // 2. Query execution: Auto (Gemini with Fallback) vs Offline DSA
                String resultsJson = null;
                String engineName = "Gemini AI";

                if ("offline".equals(searchMode)) {
                    // Direct local search across CustomLinkedList (Merge Sorted)
                    engineName = "Java LinkedList DSA (Merge Sort: " + sortBy + ")";
                    resultsJson = documentList.searchAndToJson(searchWord, sortBy);
                    System.out.println("[OFFLINE DSA] Searched & MergeSorted (" + sortBy + ") for: '" + searchWord + "'");
                } else {
                    // Try Gemini AI first, fallback to LinkedList if offline or unavailable
                    if (!searchWord.trim().isEmpty()) {
                        resultsJson = queryGeminiAI(searchWord.trim());
                    }

                    if (resultsJson != null && resultsJson.startsWith("[") && resultsJson.endsWith("]") && !resultsJson.equals("[]")) {
                        System.out.println("[GEMINI AI] Generated intelligent results for: '" + searchWord + "'");
                    } else {
                        // Fallback to local Java Linked List with Merge Sort
                        engineName = "Java LinkedList DSA (Fallback Merge Sort: " + sortBy + ")";
                        resultsJson = documentList.searchAndToJson(searchWord, sortBy);
                        System.out.println("[DSA LOG] Fallback search & MergeSort (" + sortBy + ") for: '" + searchWord + "'");
                    }
                }

                long endTime = System.nanoTime();
                double timeMs = (endTime - startTime) / 1_000_000.0;

                // 3. Build final JSON packet
                String responseJson = String.format(
                    Locale.US,
                    "{\"query\":\"%s\", \"timeMs\":%.2f, \"historyCount\":%d, \"lastSearch\":\"%s\", \"engine\":\"%s\", \"sortBy\":\"%s\", \"results\":%s}",
                    escapeJson(searchWord),
                    timeMs,
                    searchHistoryStack.size(),
                    escapeJson(searchHistoryStack.peek()),
                    escapeJson(engineName),
                    escapeJson(sortBy),
                    resultsJson
                );

                // 4. Send JSON response back to the client
                byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }

                System.out.println("[SERVER LOG] Query: '" + searchWord + "' | Engine: " + engineName + " | Time: " + String.format(Locale.US, "%.2f", timeMs) + "ms");
            } finally {
                exchange.close();
            }
        }
    }

    // ========================================================================
    // ENDPOINT: /add-document (Adds an offline article into SQLite & LinkedList)
    // ========================================================================
    static class AddDocumentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    byte[] err = "{\"success\":false,\"error\":\"Method not allowed. Use POST.\"}"
                        .getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(405, err.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(err); }
                    return;
                }

                // Read request payload
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();

                String title = "";
                String description = "";
                double score = 95.0;

                if (body.startsWith("{") && body.endsWith("}")) {
                    title = extractJsonStringField(body, "title");
                    description = extractJsonStringField(body, "description");
                    String scoreStr = extractJsonStringField(body, "score");
                    if (scoreStr.isEmpty()) {
                        scoreStr = extractJsonNumberField(body, "score");
                    }
                    if (!scoreStr.isEmpty()) {
                        try {
                            score = Double.parseDouble(scoreStr);
                        } catch (NumberFormatException ignored) {}
                    }
                } else {
                    // Handle url-encoded form body: title=...&description=...&score=...
                    for (String param : body.split("&")) {
                        String[] pair = param.split("=", 2);
                        if (pair.length == 2) {
                            String k = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                            String v = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                            if ("title".equalsIgnoreCase(k)) title = v;
                            else if ("description".equalsIgnoreCase(k)) description = v;
                            else if ("score".equalsIgnoreCase(k)) {
                                try { score = Double.parseDouble(v); } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                }

                if (title.trim().isEmpty() || description.trim().isEmpty()) {
                    byte[] err = "{\"success\":false,\"error\":\"Title and Description cannot be empty!\"}"
                        .getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(400, err.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(err); }
                    return;
                }

                title = title.trim();
                description = description.trim();
                long now = System.currentTimeMillis();

                // 1. Insert permanently into SQLite documents table with timestamp
                String sql = "INSERT INTO documents (title, description, score, timestamp) VALUES (?, ?, ?, ?)";
                try (Connection conn = DBConnect.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, title);
                    pstmt.setString(2, description);
                    pstmt.setDouble(3, score);
                    pstmt.setLong(4, now);
                    pstmt.executeUpdate();
                    System.out.println("[DB LOG] Permanently saved article to SQLite: \"" + title + "\"");
                } catch (SQLException e) {
                    System.err.println("[DB ERROR] Failed to save article to SQLite: " + e.getMessage());
                }

                // 2. Insert into in-memory CustomLinkedList with timestamp
                documentList.insert(title, description, score, now);

                // 3. Insert into in-memory CustomTrie for immediate auto-suggestion
                suggestionTrie.insert(title);

                // 4. Return success JSON
                String resp = String.format(
                    Locale.US,
                    "{\"success\":true,\"message\":\"Article saved to SQLite & LinkedList!\",\"title\":\"%s\",\"score\":%.1f,\"timestamp\":%d,\"date\":\"%s\",\"totalDocs\":%d}",
                    escapeJson(title),
                    score,
                    now,
                    formatTimestamp(now),
                    documentList.size()
                );
                byte[] responseBytes = resp.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }

            } catch (Exception e) {
                byte[] err = ("{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, err.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(err); }
            } finally {
                exchange.close();
            }
        }
    }

    // ========================================================================
    // ENDPOINT: /documents (Returns all offline articles in CustomLinkedList)
    // ========================================================================
    static class DocumentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                URI requestURI = exchange.getRequestURI();
                String rawQuery = requestURI.getRawQuery();
                String sortBy = "score_desc";
                if (rawQuery != null) {
                    for (String param : rawQuery.split("&")) {
                        String[] pair = param.split("=", 2);
                        if (pair.length == 2 && "sort".equals(pair[0])) {
                            sortBy = URLDecoder.decode(pair[1], StandardCharsets.UTF_8).toLowerCase();
                        }
                    }
                }

                String json = documentList.searchAndToJson("", sortBy);
                byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } finally {
                exchange.close();
            }
        }
    }

    // Lightweight JSON field extractors (keeps code 100% pure Java without external JSON library)
    public static String extractJsonStringField(String json, String field) {
        String key = "\"" + field + "\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx == -1) return "";
        int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx == -1) return "";
        int startQuote = json.indexOf('"', colonIdx + 1);
        if (startQuote == -1) return "";
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = startQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                if (c == 'n') sb.append('\n');
                else if (c == 'r') sb.append('\r');
                else if (c == 't') sb.append('\t');
                else sb.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String extractJsonNumberField(String json, String field) {
        String key = "\"" + field + "\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx == -1) return "";
        int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx == -1) return "";
        int i = colonIdx + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        int start = i;
        while (i < json.length() && (Character.isDigit(json.charAt(i)) || json.charAt(i) == '.')) {
            i++;
        }
        return (start < i) ? json.substring(start, i) : "";
    }

    // ========================================================================
    // STATIC FILE HANDLER: Serves HTML, CSS, JS, PNG, JPG directly on port 8080
    // ========================================================================
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }
            if (path.startsWith("/")) path = path.substring(1);

            File file = new File("Website", path);
            if (!file.exists() || file.isDirectory()) {
                file = new File(path);
            }

            if (!file.exists() || file.isDirectory()) {
                String notFound = "404 Not Found: " + path;
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(404, notFound.length());
                OutputStream os = exchange.getResponseBody();
                os.write(notFound.getBytes(StandardCharsets.UTF_8));
                os.close();
                return;
            }

            String contentType = "application/octet-stream";
            String lower = path.toLowerCase();
            if (lower.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (lower.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (lower.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (lower.endsWith(".png")) contentType = "image/png";
            else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (lower.endsWith(".svg")) contentType = "image/svg+xml";

            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}