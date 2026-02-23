package ru.miacomsoft.EasyWebServer;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Оптимизированный класс для работы с PostgreSQL
 * Добавлены: пул соединений, кэширование, улучшенная производительность
 */
public class PostgreQuery {

    // Пул соединений для каждой конфигурации БД
    private static final Map<String, ConnectionPool> connectionPools = new HashMap<>();

    // Кэш для обработанных SQL запросов
    private static final Map<String, ParsedSql> sqlCache = new LRUCache<>(1000);

    // Кэш для метаданных таблиц
    private static final Map<String, TableMetadata> metadataCache = new LRUCache<>(500);

    // Кэш для prepared statements (по SQL + схема)
    private static final Map<String, PreparedStatementCache> statementCache = new LRUCache<>(200);

    // Флаг отладки
    private static boolean DEBUG = false;

    // Размер пула соединений по умолчанию
    private static final int DEFAULT_POOL_SIZE = 10;

    // Таймаут получения соединения из пула
    private static final int CONNECTION_TIMEOUT = 5000;

    // Максимальное количество prepared statements в кэше
    private static final int MAX_STATEMENTS_PER_CONNECTION = 50;

    // Статическая карта процедур (сохраняем для обратной совместимости)
    public static HashMap<String, HashMap<String, Object>> procedureList = new HashMap<>();

    /**
     * Установка режима отладки
     */
    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    /**
     * Функция подключения к PostgreSQL (с пулом соединений)
     */
    public static Connection getConnect(String userName, String userPass) {
        return getConnect(userName, userPass, (String) null);
    }

    /**
     * Функция подключения к PostgreSQL с указанием конкретной БД из конфигурации
     */
    public static Connection getConnect(String userName, String userPass, String dbName) {
        DatabaseConfig dbConfig = ServerConstant.config.getDatabaseConfig(dbName);

        if (dbConfig != null) {
            return getConnectFromConfig(dbConfig, userName, userPass);
        }

        // Используем стандартную конфигурацию
        return getConnectFromUrl(ServerConstant.config.DATABASE_NAME, userName, userPass);
    }

    /**
     * Подключение через DatabaseConfig
     */
    private static Connection getConnectFromConfig(DatabaseConfig dbConfig, String userName, String userPass) {
        String poolKey = generatePoolKey(dbConfig, userName);
        ConnectionPool pool = connectionPools.get(poolKey);

        if (pool == null) {
            synchronized (PostgreQuery.class) {
                pool = connectionPools.get(poolKey);
                if (pool == null) {
                    pool = new ConnectionPool(dbConfig, userName, userPass, DEFAULT_POOL_SIZE);
                    connectionPools.put(poolKey, pool);
                }
            }
        }

        try {
            return pool.getConnection();
        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("Error getting connection from pool: " + e.getMessage());
            }
            return createDirectConnection(dbConfig, userName, userPass);
        }
    }

    /**
     * Подключение по URL
     */
    private static Connection getConnectFromUrl(String url, String userName, String userPass) {
        String poolKey = url + "|" + userName;
        ConnectionPool pool = connectionPools.get(poolKey);

        if (pool == null) {
            synchronized (PostgreQuery.class) {
                pool = connectionPools.get(poolKey);
                if (pool == null) {
                    pool = new ConnectionPool(url, userName, userPass, DEFAULT_POOL_SIZE);
                    connectionPools.put(poolKey, pool);
                }
            }
        }

        try {
            return pool.getConnection();
        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("Error getting connection from pool: " + e.getMessage());
            }
            return createDirectConnection(url, userName, userPass);
        }
    }

    /**
     * Создание прямого соединения
     */
    private static Connection createDirectConnection(String url, String userName, String userPass) {
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(url, userName, userPass);
            // Оптимальные настройки для PostgreSQL
            conn.setAutoCommit(false);
            return conn;
        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("Direct connection failed: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Создание прямого соединения из конфигурации
     */
    private static Connection createDirectConnection(DatabaseConfig dbConfig, String userName, String userPass) {
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(
                    dbConfig.getJdbcUrl(),
                    userName != null ? userName : dbConfig.getUsername(),
                    userPass != null ? userPass : dbConfig.getPassword()
            );
            conn.setAutoCommit(false);

            // Устанавливаем схему если указана
            if (dbConfig.getSchema() != null && !dbConfig.getSchema().isEmpty()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("SET search_path TO '" + dbConfig.getSchema() + "'");
                }
            }

            return conn;
        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("Direct connection failed: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Генерация ключа для пула
     */
    private static String generatePoolKey(DatabaseConfig dbConfig, String userName) {
        return dbConfig.getHost() + ":" +
                dbConfig.getPort() + ":" +
                dbConfig.getDatabase() + ":" +
                (userName != null ? userName : dbConfig.getUsername());
    }

    /**
     * getConnect с JSONObject (для обратной совместимости)
     */
    public static Connection getConnect(String userName, String userPass, JSONObject info) {
        Connection conn = getConnect(userName, userPass);
        if (conn == null && info != null) {
            info.put("error", "Database connection failed");
        }
        return conn;
    }

    /**
     * Получение версии PostgreSQL (оптимизировано)
     */
    public static String getVersionPostgres(String userName, String userPass) {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;

        try {
            conn = getConnect(userName, userPass);
            if (conn == null) return "[]";

            st = conn.createStatement();
            rs = st.executeQuery("SELECT json_agg(version())");

            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception e) {
            if (DEBUG) {
                System.err.println("Error getting version: " + e.getMessage());
            }
            return "[" + new JSONObject().put("error", e.getMessage()).toString() + "]";
        } finally {
            closeResources(rs, st);
            releaseConnection(conn);
        }

        return "[]";
    }

    /**
     * Выполнение SQL запроса с возвратом JSONArray
     */
    public static JSONArray executeQuery(String sql, String userName, String userPass) {
        return executeQuery(sql, null, userName, userPass);
    }

    /**
     * Выполнение SQL запроса с параметрами
     */
    public static JSONArray executeQuery(String sql, Map<String, Object> params, String userName, String userPass) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnect(userName, userPass);
            if (conn == null) {
                return createErrorResult("Database connection failed");
            }

            ParsedSql parsedSql = getParsedSql(sql);
            pstmt = prepareStatement(conn, parsedSql.processedSql);

            if (params != null && !params.isEmpty()) {
                setParametersOptimized(pstmt, parsedSql.paramNames, params);
            }

            rs = pstmt.executeQuery();
            return resultSetToJSONOptimized(rs);

        } catch (SQLException e) {
            if (DEBUG) {
                System.err.println("Query error: " + e.getMessage());
            }
            return createErrorResult(e.getMessage());
        } finally {
            closeResources(rs, pstmt);
            releaseConnection(conn);
        }
    }

    /**
     * Выполнение запроса с указанием конкретной БД
     */
    public static JSONArray executeQuery(String sql, Map<String, Object> params, String dbName, String userName, String userPass) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnect(userName, userPass, dbName);
            if (conn == null) {
                return createErrorResult("Database connection failed for: " + dbName);
            }

            ParsedSql parsedSql = getParsedSql(sql);
            pstmt = prepareStatement(conn, parsedSql.processedSql);

            if (params != null && !params.isEmpty()) {
                setParametersOptimized(pstmt, parsedSql.paramNames, params);
            }

            rs = pstmt.executeQuery();
            return resultSetToJSONOptimized(rs);

        } catch (SQLException e) {
            if (DEBUG) {
                System.err.println("Query error on " + dbName + ": " + e.getMessage());
            }
            return createErrorResult(e.getMessage());
        } finally {
            closeResources(rs, pstmt);
            releaseConnection(conn);
        }
    }

    /**
     * Выполнение обновления (INSERT, UPDATE, DELETE)
     */
    public static int executeUpdate(String sql, Map<String, Object> params, String userName, String userPass) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = getConnect(userName, userPass);
            if (conn == null) return -1;

            ParsedSql parsedSql = getParsedSql(sql);
            pstmt = prepareStatement(conn, parsedSql.processedSql);

            if (params != null && !params.isEmpty()) {
                setParametersOptimized(pstmt, parsedSql.paramNames, params);
            }

            int result = pstmt.executeUpdate();
            conn.commit();
            return result;

        } catch (SQLException e) {
            rollbackQuietly(conn);
            if (DEBUG) {
                System.err.println("Update error: " + e.getMessage());
            }
            return -1;
        } finally {
            closeResources(null, pstmt);
            releaseConnection(conn);
        }
    }

    /**
     * Получение разобранного SQL из кэша
     */
    private static ParsedSql getParsedSql(String sql) {
        ParsedSql parsed = sqlCache.get(sql);
        if (parsed == null) {
            parsed = parseNamedParameters(sql);
            sqlCache.put(sql, parsed);
        }
        return parsed;
    }

    /**
     * Подготовка statement с кэшированием
     */
    private static PreparedStatement prepareStatement(Connection conn, String sql) throws SQLException {
        if (conn instanceof PooledConnection) {
            String cacheKey = sql.hashCode() + "|" + conn.hashCode();
            PreparedStatementCache cached = statementCache.get(cacheKey);

            if (cached != null && cached.isValid(conn)) {
                return cached.statement;
            }

            PreparedStatement pstmt = conn.prepareStatement(sql);
            statementCache.put(cacheKey, new PreparedStatementCache(pstmt, conn));
            return pstmt;
        }

        return conn.prepareStatement(sql);
    }

    /**
     * Парсинг именованных параметров PostgreSQL (:param)
     */
    private static ParsedSql parseNamedParameters(String sql) {
        if (sql == null || sql.isEmpty()) {
            return new ParsedSql(sql, Collections.emptyList());
        }

        StringBuilder processedSql = new StringBuilder(sql.length() + 16);
        List<String> paramNames = new ArrayList<>();
        StringBuilder paramName = new StringBuilder();
        boolean inParam = false;
        boolean inQuote = false;
        boolean inDollarQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            // Обработка кавычек
            if ((c == '\'' || c == '"') && (i == 0 || sql.charAt(i - 1) != '\\')) {
                if (!inQuote && !inDollarQuote) {
                    inQuote = true;
                    quoteChar = c;
                } else if (inQuote && quoteChar == c) {
                    inQuote = false;
                }
            }

            // Обработка долларовых кавычек для функций PostgreSQL
            if (c == '$' && i + 1 < sql.length() && Character.isLetter(sql.charAt(i + 1))) {
                inDollarQuote = !inDollarQuote;
            }

            if (inQuote || inDollarQuote) {
                processedSql.append(c);
                continue;
            }

            if (c == ':' && !inParam) {
                inParam = true;
                paramName = new StringBuilder();
            } else if (inParam && (Character.isLetterOrDigit(c) || c == '_')) {
                paramName.append(c);
            } else if (inParam) {
                inParam = false;
                if (paramName.length() > 0) {
                    paramNames.add(paramName.toString());
                }
                processedSql.append('?').append(c);
            } else {
                processedSql.append(c);
            }
        }

        if (inParam && paramName.length() > 0) {
            paramNames.add(paramName.toString());
            processedSql.append('?');
        }

        return new ParsedSql(processedSql.toString(), paramNames);
    }

    /**
     * Оптимизированная установка параметров
     */
    private static void setParametersOptimized(PreparedStatement pstmt,
                                               List<String> paramNames,
                                               Map<String, Object> params) throws SQLException {
        for (int i = 0; i < paramNames.size(); i++) {
            String name = paramNames.get(i);
            Object value = params.get(name);

            if (value == null) {
                pstmt.setNull(i + 1, Types.VARCHAR);
                continue;
            }

            Class<?> valueClass = value.getClass();

            if (valueClass == String.class) {
                pstmt.setString(i + 1, (String) value);
            } else if (valueClass == Integer.class) {
                pstmt.setInt(i + 1, (Integer) value);
            } else if (valueClass == Long.class) {
                pstmt.setLong(i + 1, (Long) value);
            } else if (valueClass == Double.class) {
                pstmt.setDouble(i + 1, (Double) value);
            } else if (valueClass == Boolean.class) {
                pstmt.setBoolean(i + 1, (Boolean) value);
            } else if (value instanceof Date) {
                pstmt.setTimestamp(i + 1, new Timestamp(((Date) value).getTime()));
            } else if (value instanceof JSONObject) {
                pstmt.setObject(i + 1, value.toString(), Types.OTHER);
            } else {
                pstmt.setString(i + 1, value.toString());
            }
        }
    }

    /**
     * Оптимизированное преобразование ResultSet в JSONArray
     */
    private static JSONArray resultSetToJSONOptimized(ResultSet rs) throws SQLException {
        JSONArray result = new JSONArray();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        String[] columnNames = new String[columnCount];
        int[] columnTypes = new int[columnCount];

        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = metaData.getColumnLabel(i + 1);
            columnTypes[i] = metaData.getColumnType(i + 1);
        }

        while (rs.next()) {
            JSONObject row = new JSONObject();
            for (int i = 0; i < columnCount; i++) {
                String columnName = columnNames[i];
                int type = columnTypes[i];

                Object value = getValueByType(rs, i + 1, type);

                if (value == null) {
                    row.put(columnName, JSONObject.NULL);
                } else {
                    row.put(columnName, value);
                }
            }
            result.put(row);
        }

        return result;
    }

    /**
     * Получение значения по типу
     */
    private static Object getValueByType(ResultSet rs, int index, int type) throws SQLException {
        switch (type) {
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
                return rs.getString(index);

            case Types.INTEGER:
                return rs.getInt(index);

            case Types.BIGINT:
                return rs.getLong(index);

            case Types.NUMERIC:
            case Types.DECIMAL:
                return rs.getBigDecimal(index);

            case Types.DOUBLE:
            case Types.FLOAT:
                return rs.getDouble(index);

            case Types.BOOLEAN:
            case Types.BIT:
                return rs.getBoolean(index);

            case Types.DATE:
                Date date = rs.getDate(index);
                return date != null ? date.toString() : null;

            case Types.TIMESTAMP:
                Timestamp ts = rs.getTimestamp(index);
                return ts != null ? ts.toString() : null;

            case Types.OTHER:
                // Для JSON/JSONB в PostgreSQL
                Object obj = rs.getObject(index);
                if (obj instanceof org.postgresql.util.PGobject) {
                    return ((org.postgresql.util.PGobject) obj).getValue();
                }
                return obj != null ? obj.toString() : null;

            case Types.ARRAY:
                Array array = rs.getArray(index);
                if (array != null) {
                    Object[] arrayObj = (Object[]) array.getArray();
                    JSONArray jsonArray = new JSONArray();
                    for (Object item : arrayObj) {
                        jsonArray.put(item != null ? item.toString() : null);
                    }
                    return jsonArray;
                }
                return null;

            default:
                Object objDefault = rs.getObject(index);
                return objDefault != null ? objDefault.toString() : null;
        }
    }

    /**
     * Создание JSON с ошибкой
     */
    private static JSONArray createErrorResult(String errorMessage) {
        JSONArray result = new JSONArray();
        JSONObject error = new JSONObject();
        error.put("error", errorMessage);
        result.put(error);
        return result;
    }

    /**
     * Создание процедуры (оптимизировано)
     */
    public static void createProcedure(Connection conn, String nameProcedure, String procText) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.execute("DROP PROCEDURE IF EXISTS " + nameProcedure);
            stmt.execute(procText);
            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn);
            if (DEBUG) {
                System.err.println("Error creating procedure: " + e.getMessage());
            }
        } finally {
            closeResources(null, stmt);
        }
    }

    /**
     * Создание функции (оптимизировано)
     */
    public static void createFunction(Connection conn, String nameProcedure, String procText) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.execute("DROP FUNCTION IF EXISTS " + nameProcedure);
            stmt.execute(procText);
            conn.commit();
        } catch (SQLException e) {
            rollbackQuietly(conn);
            if (DEBUG) {
                System.err.println("Error creating function: " + e.getMessage());
            }
        } finally {
            closeResources(null, stmt);
        }
    }

    /**
     * Проверка существования таблицы
     */
    public static boolean tableExists(Connection conn, String tableName, String schema) {
        String cacheKey = (schema != null ? schema : "public") + "." + tableName.toLowerCase();
        TableMetadata metadata = metadataCache.get(cacheKey);

        if (metadata != null) {
            return metadata.exists;
        }

        String sql = "SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE ";
        List<Object> params = new ArrayList<>();

        if (schema != null && !schema.isEmpty()) {
            sql += "table_schema = ? AND ";
            params.add(schema);
        }
        sql += "table_name = ?)";
        params.add(tableName.toLowerCase());

        boolean exists = false;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.size(); i++) {
                pstmt.setString(i + 1, (String) params.get(i));
            }
            rs = pstmt.executeQuery();
            if (rs.next()) {
                exists = rs.getBoolean(1);
            }
        } catch (SQLException e) {
            if (DEBUG) {
                System.err.println("Error checking table: " + e.getMessage());
            }
        } finally {
            closeResources(rs, pstmt);
        }

        metadataCache.put(cacheKey, new TableMetadata(cacheKey, exists));
        return exists;
    }

    /**
     * Очистка процедур веб-страниц (оптимизировано)
     */
    public void clearWebPageProcedure(Connection conn) {
        CallableStatement cs = null;
        try {
            String procName = "clear_" + ServerConstant.config.APP_NAME + "_proc";

            // Создаем процедуру очистки
            String createProc =
                    "CREATE OR REPLACE PROCEDURE " + procName + "() LANGUAGE plpgsql AS $$\n" +
                            "DECLARE\n" +
                            "    r RECORD;\n" +
                            "BEGIN\n" +
                            "    FOR r IN SELECT proname FROM pg_proc \n" +
                            "             WHERE pronamespace = 'public'::regnamespace\n" +
                            "             AND proname LIKE '" + ServerConstant.config.APP_NAME + "_%'\n" +
                            "    LOOP\n" +
                            "        EXECUTE 'DROP PROCEDURE IF EXISTS ' || r.proname || ' CASCADE';\n" +
                            "    END LOOP;\n" +
                            "END;\n" +
                            "$$;";

            createProcedure(conn, procName, createProc);

            cs = conn.prepareCall("CALL " + procName + "()");
            cs.execute();
            conn.commit();

        } catch (SQLException e) {
            rollbackQuietly(conn);
            if (DEBUG) {
                System.err.println("Error clearing procedures: " + e.getMessage());
            }
        } finally {
            closeResources(null, cs);
        }
    }

    /**
     * Закрытие ресурсов
     */
    private static void closeResources(ResultSet rs, Statement stmt) {
        try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
        try { if (stmt != null) stmt.close(); } catch (SQLException ignored) {}
    }

    /**
     * Возврат соединения в пул
     */
    private static void releaseConnection(Connection conn) {
        if (conn == null) return;

        try {
            if (conn instanceof PooledConnection) {
                conn.close(); // PooledConnection.close() возвращает в пул
            } else {
                conn.close();
            }
        } catch (SQLException ignored) {}
    }

    /**
     * Откат транзакции без проброса исключения
     */
    private static void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {}
        }
    }

    // ==================== Внутренние классы ====================

    /**
     * Класс для хранения разобранного SQL
     */
    private static class ParsedSql {
        final String processedSql;
        final List<String> paramNames;

        ParsedSql(String processedSql, List<String> paramNames) {
            this.processedSql = processedSql;
            this.paramNames = paramNames;
        }
    }

    /**
     * Класс для хранения метаданных
     */
    private static class TableMetadata {
        final String key;
        final boolean exists;
        final long timestamp;

        TableMetadata(String key, boolean exists) {
            this.key = key;
            this.exists = exists;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Кэш для PreparedStatement
     */
    private static class PreparedStatementCache {
        final PreparedStatement statement;
        final Connection connection;
        final long timestamp;

        PreparedStatementCache(PreparedStatement statement, Connection connection) {
            this.statement = statement;
            this.connection = connection;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isValid(Connection currentConn) throws SQLException {
            return connection == currentConn &&
                    !statement.isClosed() &&
                    System.currentTimeMillis() - timestamp < 60000; // 1 минута
        }
    }

    /**
     * LRU кэш
     */
    private static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        LRUCache(int maxSize) {
            super(maxSize + 1, 0.75f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }

    /**
     * Пул соединений
     */
    private static class ConnectionPool {
        private final ArrayBlockingQueue<PooledConnection> pool;
        private final String url;
        private final String userName;
        private final String userPass;
        private final int maxSize;
        private int created = 0;

        ConnectionPool(String url, String userName, String userPass, int size) {
            this.url = url;
            this.userName = userName;
            this.userPass = userPass;
            this.maxSize = size;
            this.pool = new ArrayBlockingQueue<>(size);

            // Создаем начальные соединения
            for (int i = 0; i < Math.min(2, size); i++) {
                try {
                    pool.offer(createPooledConnection());
                    created++;
                } catch (Exception e) {
                    if (DEBUG) {
                        System.err.println("Failed to create initial connection: " + e.getMessage());
                    }
                }
            }
        }

        ConnectionPool(DatabaseConfig dbConfig, String userName, String userPass, int size) {
            this(dbConfig.getJdbcUrl(), userName, userPass, size);
        }

        private PooledConnection createPooledConnection() throws SQLException {
            try {
                Class.forName("org.postgresql.Driver");
                Connection conn = DriverManager.getConnection(url, userName, userPass);
                conn.setAutoCommit(false);
                return new PooledConnection(conn, this);
            } catch (ClassNotFoundException e) {
                throw new SQLException("PostgreSQL driver not found", e);
            }
        }

        Connection getConnection() throws Exception {
            PooledConnection pooled = pool.poll();

            if (pooled != null) {
                Connection conn = pooled.getConnection();
                if (conn != null && !conn.isClosed()) {
                    return conn;
                }
                created--;
            }

            synchronized (this) {
                if (created < maxSize) {
                    created++;
                    return createPooledConnection().getConnection();
                }
            }

            pooled = pool.poll(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
            if (pooled != null) {
                return pooled.getConnection();
            }

            throw new SQLException("Connection timeout");
        }

        boolean releaseConnection(Connection conn) {
            if (conn instanceof PooledConnection) {
                return pool.offer((PooledConnection) conn);
            }
            return false;
        }
    }

    /**
     * Обертка для соединения с пулом
     */
    private static class PooledConnection implements Connection {
        private final Connection delegate;
        private final ConnectionPool pool;
        private boolean closed = false;

        PooledConnection(Connection delegate, ConnectionPool pool) {
            this.delegate = delegate;
            this.pool = pool;
        }

        Connection getConnection() {
            return closed ? null : this;
        }

        @Override
        public void close() throws SQLException {
            if (!closed) {
                closed = true;
                if (!pool.releaseConnection(this)) {
                    delegate.close();
                }
            }
        }

        @Override
        public boolean isClosed() throws SQLException {
            return closed || delegate.isClosed();
        }

        // Делегирование всех остальных методов Connection
        @Override public Statement createStatement() throws SQLException { return delegate.createStatement(); }
        @Override public PreparedStatement prepareStatement(String sql) throws SQLException { return delegate.prepareStatement(sql); }
        @Override public CallableStatement prepareCall(String sql) throws SQLException { return delegate.prepareCall(sql); }
        @Override public String nativeSQL(String sql) throws SQLException { return delegate.nativeSQL(sql); }
        @Override public void setAutoCommit(boolean autoCommit) throws SQLException { delegate.setAutoCommit(autoCommit); }
        @Override public boolean getAutoCommit() throws SQLException { return delegate.getAutoCommit(); }
        @Override public void commit() throws SQLException { delegate.commit(); }
        @Override public void rollback() throws SQLException { delegate.rollback(); }
        @Override public DatabaseMetaData getMetaData() throws SQLException { return delegate.getMetaData(); }
        @Override public void setReadOnly(boolean readOnly) throws SQLException { delegate.setReadOnly(readOnly); }
        @Override public boolean isReadOnly() throws SQLException { return delegate.isReadOnly(); }
        @Override public void setCatalog(String catalog) throws SQLException { delegate.setCatalog(catalog); }
        @Override public String getCatalog() throws SQLException { return delegate.getCatalog(); }
        @Override public void setTransactionIsolation(int level) throws SQLException { delegate.setTransactionIsolation(level); }
        @Override public int getTransactionIsolation() throws SQLException { return delegate.getTransactionIsolation(); }
        @Override public SQLWarning getWarnings() throws SQLException { return delegate.getWarnings(); }
        @Override public void clearWarnings() throws SQLException { delegate.clearWarnings(); }
        @Override public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return delegate.createStatement(resultSetType, resultSetConcurrency);
        }
        @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }
        @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
        }
        @Override public Map<String, Class<?>> getTypeMap() throws SQLException { return delegate.getTypeMap(); }
        @Override public void setTypeMap(Map<String, Class<?>> map) throws SQLException { delegate.setTypeMap(map); }
        @Override public void setHoldability(int holdability) throws SQLException { delegate.setHoldability(holdability); }
        @Override public int getHoldability() throws SQLException { return delegate.getHoldability(); }
        @Override public Savepoint setSavepoint() throws SQLException { return delegate.setSavepoint(); }
        @Override public Savepoint setSavepoint(String name) throws SQLException { return delegate.setSavepoint(name); }
        @Override public void rollback(Savepoint savepoint) throws SQLException { delegate.rollback(savepoint); }
        @Override public void releaseSavepoint(Savepoint savepoint) throws SQLException { delegate.releaseSavepoint(savepoint); }
        @Override public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        @Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return delegate.prepareStatement(sql, autoGeneratedKeys);
        }
        @Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return delegate.prepareStatement(sql, columnIndexes);
        }
        @Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return delegate.prepareStatement(sql, columnNames);
        }
        @Override public Clob createClob() throws SQLException { return delegate.createClob(); }
        @Override public Blob createBlob() throws SQLException { return delegate.createBlob(); }
        @Override public NClob createNClob() throws SQLException { return delegate.createNClob(); }
        @Override public SQLXML createSQLXML() throws SQLException { return delegate.createSQLXML(); }
        @Override public boolean isValid(int timeout) throws SQLException { return delegate.isValid(timeout); }
        @Override public void setClientInfo(String name, String value) throws SQLClientInfoException { delegate.setClientInfo(name, value); }
        @Override public void setClientInfo(Properties properties) throws SQLClientInfoException { delegate.setClientInfo(properties); }
        @Override public String getClientInfo(String name) throws SQLException { return delegate.getClientInfo(name); }
        @Override public Properties getClientInfo() throws SQLException { return delegate.getClientInfo(); }
        @Override public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return delegate.createArrayOf(typeName, elements);
        }
        @Override public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return delegate.createStruct(typeName, attributes);
        }
        @Override public void setSchema(String schema) throws SQLException { delegate.setSchema(schema); }
        @Override public String getSchema() throws SQLException { return delegate.getSchema(); }
        @Override public void abort(java.util.concurrent.Executor executor) throws SQLException { delegate.abort(executor); }
        @Override public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) throws SQLException {
            delegate.setNetworkTimeout(executor, milliseconds);
        }
        @Override public int getNetworkTimeout() throws SQLException { return delegate.getNetworkTimeout(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return delegate.unwrap(iface); }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return delegate.isWrapperFor(iface); }
    }
}