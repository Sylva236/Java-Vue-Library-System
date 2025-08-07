import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import utils.ConnectConfig;
import utils.DatabaseConnector;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import entities.*;
import queries.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.*;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    private static LibraryManagementSystem library;

    public static void main(String[] args) {
        try {
            ConnectConfig conf = new ConnectConfig();
            log.info("Successfully parsed connect config: " + conf);

            final DatabaseConnector connector = new DatabaseConnector(conf);
            if (!connector.connect()) {
                log.severe("Failed to connect to database");
                System.exit(1);
            }
            log.info("Successfully connected to database");

            library = new LibraryManagementSystemImpl(connector);

            final HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/cards", new CardHandler());
            server.createContext("/books", new BookHandler());
            server.createContext("/borrows", new BorrowHandler());
            server.start();
            log.info("Server is listening on port 8000");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.stop(0);

                if (connector.release()) {
                    log.info("Database connection released successfully");
                } else {
                    log.warning("Failed to release database connection");
                }
            }));

            Thread.currentThread().join();
        } catch (IOException e) {
            log.severe("IO Error: " + e.getMessage());
            System.exit(1);
        } catch (ClassNotFoundException e) {
            log.severe("Class Not Found: " + e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            log.warning("Main thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    abstract static class BaseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // 统一CORS设置
                setCorsHeaders(exchange);

                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    handleOptions(exchange);
                    return;
                }

                // 路由到具体方法
                switch (exchange.getRequestMethod()) {
                    case "GET":
                        handleGet(exchange);
                        break;
                    case "POST":
                        handlePost(exchange);
                        break;
                    case "PUT":
                        handlePut(exchange);
                        break;
                    case "DELETE":
                        handleDelete(exchange);
                        break;
                    default:
                        sendResponse(exchange, 405, "Method Not Allowed");
                }
            } catch (Exception e) {
                log.severe("Request handling error: " + e.getMessage());
                sendResponse(exchange, 500, "Internal Server Error");
            }
        }

        protected void setCorsHeaders(HttpExchange exchange) {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
        }

        protected void handleOptions(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(204, -1);
        }

        protected String readRequestBody(HttpExchange exchange) throws IOException {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                return reader.lines().collect(Collectors.joining());
            }
        }

        protected Map<String, String> parseQueryParams(String query) {
            Map<String, String> params = new HashMap<>();
            if (query == null) return params;

            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length > 1) {
                    params.put(pair[0], pair[1]);
                }
            }
            return params;
        }

        protected void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        protected void sendApiResult(HttpExchange exchange, ApiResult result) throws IOException {
            JSONObject response = new JSONObject();
            response.put("success", result.ok);
            if (!result.ok) {
                response.put("error", result.message);
            } else if (result.payload != null) {
                response.put("data", result.payload);
            }
            sendResponse(exchange, result.ok ? 200 : 400, response.toJSONString());
        }

        // 抽象方法
        protected abstract void handleGet(HttpExchange exchange) throws IOException;
        protected abstract void handlePost(HttpExchange exchange) throws IOException;
        protected abstract void handlePut(HttpExchange exchange) throws IOException;
        protected abstract void handleDelete(HttpExchange exchange) throws IOException;
    }

    // 借书卡处理器
    static class CardHandler extends BaseHandler {
        @Override
        protected void handleGet(HttpExchange exchange) throws IOException {
            ApiResult result = library.showCards();
            sendApiResult(exchange, result);
        }

        @Override
        protected void handlePost(HttpExchange exchange) throws IOException {
            try {
                String requestBody = readRequestBody(exchange);
                log.info("Received JSON: " + requestBody);
                
                // 解析 JSON 对象
                JSONObject jsonObject = JSON.parseObject(requestBody);
                Card card = new Card();
                card.setName(jsonObject.getString("name"));
                card.setDepartment(jsonObject.getString("department"));
                
                // 处理 type 字段
                String typeStr = jsonObject.getString("type");
                log.info("Card type from request: " + typeStr);
                
                if (typeStr != null) {
                    Card.CardType type = Card.CardType.fromString(typeStr);
                    log.info("Converted card type: " + (type != null ? type.name() : "null"));
                    
                    if (type == null) {
                        JSONObject response = new JSONObject();
                        response.put("success", false);
                        response.put("error", "Invalid card type. Must be 'S' or 'T'");
                        sendResponse(exchange, 400, response.toJSONString());
                        return;
                    }
                    card.setType(type);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("error", "Card type is required");
                    sendResponse(exchange, 400, response.toJSONString());
                    return;
                }

                log.info("Attempting to register card: " + card.toString());
                ApiResult result = library.registerCard(card);
                log.info("Registration result: " + (result.ok ? "success" : "failed - " + result.message));
                sendApiResult(exchange, result);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Handle POST error", e);
                e.printStackTrace(); // 添加堆栈跟踪
                JSONObject response = new JSONObject();
                response.put("success", false);
                response.put("error", "Internal server error: " + e.getMessage());
                sendResponse(exchange, 500, response.toJSONString());
            }
        }

        @Override
        protected void handlePut(HttpExchange exchange) throws IOException {
            try {
                // 从路径中提取cardId
                String path = exchange.getRequestURI().getPath();
                String cardIdStr = path.substring(path.lastIndexOf('/') + 1);
                int cardId = Integer.parseInt(cardIdStr);

                // 读取并解析请求体
                String requestBody = readRequestBody(exchange);
                JSONObject jsonObject = JSON.parseObject(requestBody);
                Card card = new Card();
                card.setCardId(cardId);
                card.setName(jsonObject.getString("name"));
                card.setDepartment(jsonObject.getString("department"));
                
                // 处理type字段
                String typeStr = jsonObject.getString("type");
                if (typeStr != null) {
                    Card.CardType type = Card.CardType.fromString(typeStr);
                    if (type == null) {
                        JSONObject response = new JSONObject();
                        response.put("success", false);
                        response.put("error", "Invalid card type. Must be 'S' or 'T'");
                        sendResponse(exchange, 400, response.toJSONString());
                        return;
                    }
                    card.setType(type);
                } else {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("error", "Card type is required");
                    sendResponse(exchange, 400, response.toJSONString());
                    return;
                }

                // 调用修改方法
                ApiResult result = library.modifyCard(card);
                sendApiResult(exchange, result);

            } catch (NumberFormatException e) {
                JSONObject response = new JSONObject();
                response.put("success", false);
                response.put("error", "Invalid card ID format");
                sendResponse(exchange, 400, response.toJSONString());
            } catch (Exception e) {
                log.log(Level.SEVERE, "Handle PUT error", e);
                JSONObject response = new JSONObject();
                response.put("success", false);
                response.put("error", "Internal server error: " + e.getMessage());
                sendResponse(exchange, 500, response.toJSONString());
            }
        }

        @Override
        protected void handleDelete(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String cardId = path.substring(path.lastIndexOf('/') + 1);

            ApiResult result = library.removeCard(Integer.parseInt(cardId));
            sendApiResult(exchange, result);
        }
    }

    // 图书处理器
    static class BookHandler extends BaseHandler {
        @Override
        protected void handleGet(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());

            BookQueryConditions conditions = new BookQueryConditions();
            // 设置查询条件
            if (params.containsKey("title")) {
                conditions.setTitle(params.get("title"));
            }
            if (params.containsKey("category")) {
                conditions.setCategory(params.get("category"));
            }
            if (params.containsKey("press")) {
                conditions.setPress(params.get("press"));
            }
            if (params.containsKey("author")) {
                conditions.setAuthor(params.get("author"));
            }
            if (params.containsKey("minPublishYear") && !params.get("minPublishYear").isEmpty()) {
                conditions.setMinPublishYear(Integer.parseInt(params.get("minPublishYear")));
            }
            if (params.containsKey("maxPublishYear") && !params.get("maxPublishYear").isEmpty()) {
                conditions.setMaxPublishYear(Integer.parseInt(params.get("maxPublishYear")));
            }
            if (params.containsKey("minPrice") && !params.get("minPrice").isEmpty()) {
                conditions.setMinPrice(Double.parseDouble(params.get("minPrice")));
            }
            if (params.containsKey("maxPrice") && !params.get("maxPrice").isEmpty()) {
                conditions.setMaxPrice(Double.parseDouble(params.get("maxPrice")));
            }
            if (params.containsKey("sortBy")) {
                String sortBy = params.get("sortBy");
                // 添加字段映射逻辑
                Map<String, String> sortByMap = new HashMap<>();
                sortByMap.put("bookId", "BOOK_ID");
                sortByMap.put("category", "CATEGORY");
                sortByMap.put("title", "TITLE");
                sortByMap.put("press", "PRESS");
                sortByMap.put("publishYear", "PUBLISH_YEAR");
                sortByMap.put("author", "AUTHOR");
                sortByMap.put("price", "PRICE");
                sortByMap.put("stock", "STOCK");
                String mappedSortBy = sortByMap.getOrDefault(sortBy, "BOOK_ID");
                conditions.setSortBy(Book.SortColumn.valueOf(mappedSortBy));
            }
            if (params.containsKey("sortOrder")) {
                String sortOrder = params.get("sortOrder");
                if (sortOrder != null && !sortOrder.isEmpty()) {
                    conditions.setSortOrder(SortOrder.valueOf(sortOrder.toUpperCase()));
                }
            }

            ApiResult result = library.queryBook(conditions);
            sendApiResult(exchange, result);
        }

        @Override
        protected void handlePost(HttpExchange exchange) throws IOException {
            try {
                String requestBody = readRequestBody(exchange);

                // 尝试解析为数组
                if (requestBody.startsWith("[")) {
                    List<Book> books = JSON.parseArray(requestBody, Book.class);
                    ApiResult result = library.storeBook(books); // 需要新增方法
                    sendApiResult(exchange, result);
                }
                // 否则按单个图书处理
                else {
                    Book book = JSON.parseObject(requestBody, Book.class);
                    ApiResult result = library.storeBook(book);
                    sendApiResult(exchange, result);
                }
            } catch (Exception e) {
                JSONObject response = new JSONObject();
                response.put("success", false);
                response.put("error", "Invalid request body: " + e.getMessage());
                sendResponse(exchange, 400, response.toJSONString());
            }
        }

        @Override
        protected void handlePut(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String bookId = path.substring(path.lastIndexOf('/') + 1);

            String requestBody = readRequestBody(exchange);
            Book book = JSON.parseObject(requestBody, Book.class);
            book.setBookId(Integer.parseInt(bookId));

            ApiResult result = library.modifyBookInfo(book);
            sendApiResult(exchange, result);
        }

        @Override
        protected void handleDelete(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String bookId = path.substring(path.lastIndexOf('/') + 1);

            ApiResult result = library.removeBook(Integer.parseInt(bookId));
            sendApiResult(exchange, result);
        }
    }

    // 借阅记录处理器
    static class BorrowHandler extends BaseHandler {
        @Override
        protected void handleGet(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String cardIdStr = path.substring(path.lastIndexOf('/') + 1);
            int cardId = Integer.parseInt(cardIdStr);

            ApiResult result = library.showBorrowHistory(cardId);
            sendApiResult(exchange, result);
        }

        @Override
        protected void handlePost(HttpExchange exchange) throws IOException {
            String requestBody = readRequestBody(exchange);
            Borrow borrow = JSON.parseObject(requestBody, Borrow.class);
            ApiResult result = library.borrowBook(borrow);
            sendApiResult(exchange, result);
        }

        @Override
        protected void handlePut(HttpExchange exchange) throws IOException {
            // 还书操作
            String requestBody = readRequestBody(exchange);
            Borrow borrow = JSON.parseObject(requestBody, Borrow.class);
            ApiResult result = library.returnBook(borrow);
            sendApiResult(exchange, result);
        }

        @Override
        protected void handleDelete(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 403, "{\"error\":\"Deletion not allowed\"}");
        }
    }
}
