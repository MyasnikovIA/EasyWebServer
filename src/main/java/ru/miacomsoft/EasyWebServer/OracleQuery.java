package ru.miacomsoft.EasyWebServer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.io.Reader;
import java.io.StringWriter;
import java.io.IOException;
import java.util.TimeZone;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Класс для работы с Oracle Database
 */
public class OracleQuery {

    /**
     * Получает подключение к Oracle по конфигурации
     */
    public static Connection getConnect(DatabaseConfig dbConfig) {
        Connection conn = null;
        try {

            TimeZone timeZone = TimeZone.getTimeZone("Asia/Kolkata");
            TimeZone.setDefault(timeZone);
            Class.forName("oracle.jdbc.driver.OracleDriver");
            // Class.forName(dbConfig.getDriver());
            // Формируем JDBC URL для Oracle
            String jdbcUrl = formatOracleJdbcUrl(dbConfig);
            // Логируем URL для отладки (можно убрать в продакшене)
            System.out.println("Connecting to Oracle with URL: " + jdbcUrl);
            System.out.println("Username: " + dbConfig.getUsername());
            // Создаем подключение
            conn = DriverManager.getConnection(jdbcUrl,dbConfig.getUsername(),dbConfig.getPassword());

            // Устанавливаем некоторые полезные параметры для Oracle
            // conn.setAutoCommit(false); // Отключаем авто-коммит для Oracle

        } catch (ClassNotFoundException e) {
            System.err.println("Oracle JDBC Driver not found: " + e.getMessage());
            System.err.println("Make sure ojdbc8.jar or ojdbc6.jar is in classpath");
            return null;
        } catch (SQLException e) {
            System.err.println("Oracle connection error: " + e.getMessage());
            System.err.println("Error code: " + e.getErrorCode());
            System.err.println("SQL State: " + e.getSQLState());

            // Детальная диагностика ошибки
            if (e.getErrorCode() == 17002) {
                System.err.println("IO Error: Check if Oracle server is reachable at " +
                        dbConfig.getHost() + ":" + dbConfig.getPort());
            } else if (e.getErrorCode() == 1017) {
                System.err.println("Invalid username/password for Oracle");
            } else if (e.getErrorCode() == 12505) {
                System.err.println("Invalid SID/service name: " + dbConfig.getDatabase());
            }
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getClass().getName() + ": " + e.getMessage());
            return null;
        }
        return conn;
    }

    /**
     * Форматирует JDBC URL для Oracle с учетом различных форматов
     */
    private static String formatOracleJdbcUrl(DatabaseConfig dbConfig) {
        String host = dbConfig.getHost();
        String port = dbConfig.getPort();
        String database = dbConfig.getDatabase();

        // Если порт не указан, используем стандартный порт Oracle
        if (port == null || port.trim().isEmpty()) {
            port = "1521"; // Стандартный порт Oracle
        }

        // Проверяем, является ли database SID или service name
        // В Oracle есть два формата:
        // 1. jdbc:oracle:thin:@host:port:SID
        // 2. jdbc:oracle:thin:@//host:port/service_name

        // По умолчанию используем формат с SID (двоеточие)
        return "jdbc:oracle:thin:@" + host + ":" + port + ":" + database;
    }

    /**
     * Альтернативный метод с возможностью выбора формата подключения
     */
    public static Connection getConnect(DatabaseConfig dbConfig, boolean useServiceName) {
        Connection conn = null;
        try {
            TimeZone timeZone = TimeZone.getTimeZone("Asia/Kolkata");
            TimeZone.setDefault(timeZone);

            Class.forName(dbConfig.getDriver());

            String jdbcUrl;
            String host = dbConfig.getHost();
            String port = dbConfig.getPort();
            String database = dbConfig.getDatabase();

            if (port == null || port.trim().isEmpty()) {
                port = "1521";
            }

            if (useServiceName) {
                // Формат для service name: @//host:port/service_name
                jdbcUrl = "jdbc:oracle:thin:@//" + host + ":" + port + "/" + database;
            } else {
                // Формат для SID: @host:port:sid
                jdbcUrl = "jdbc:oracle:thin:@" + host + ":" + port + ":" + database;
            }

            System.out.println("Connecting to Oracle with URL: " + jdbcUrl);

            conn = DriverManager.getConnection(
                    jdbcUrl,
                    dbConfig.getUsername(),
                    dbConfig.getPassword()
            );

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            return null;
        }
        return conn;
    }

    /**
     * Выполняет SQL запрос с параметрами и возвращает результат в JSONArray
     * @param dbConfig конфигурация БД
     * @param sql SQL запрос с именованными параметрами (:paramName)
     * @param params карта параметров (имя параметра -> значение)
     * @return JSONArray с результатами
     */
    public static JSONArray executeQuery(DatabaseConfig dbConfig, String sql, Map<String, Object> params) {
        JSONArray result = new JSONArray();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnect(dbConfig);
            if (conn == null) {
                JSONObject error = new JSONObject();
                error.put("error", "Database connection failed");
                result.put(error);
                return result;
            }

            // Преобразуем именованные параметры в позиционные (?)
            // и создаем PreparedStatement
            String processedSql = processNamedParameters(sql);
            pstmt = conn.prepareStatement(processedSql);

            // Устанавливаем значения параметров по позициям
            if (params != null && !params.isEmpty()) {
                setParameters(pstmt, sql, params);
            }

            rs = pstmt.executeQuery();
            result = resultSetToJSON(rs);

        } catch (SQLException e) {
            System.err.println("Oracle query error: " + e.getMessage());
            System.err.println("Error code: " + e.getErrorCode());
            System.err.println("SQL State: " + e.getSQLState());
            e.printStackTrace();

            JSONObject error = new JSONObject();
            error.put("error", e.getMessage());
            error.put("errorCode", e.getErrorCode());
            error.put("sqlState", e.getSQLState());
            result.put(error);
        } finally {
            closeResources(rs, pstmt, conn);
        }

        return result;
    }

    /**
     * Выполняет SQL запрос с параметрами и возвращает результат
     * Упрощенная версия для обратной совместимости
     */
    public static JSONArray executeQuery(Connection conn, String sql) {
        return executeQuery(conn, sql, null);
    }

    /**
     * Выполняет SQL запрос с параметрами и возвращает результат
     * @param conn существующее подключение
     * @param sql SQL запрос с именованными параметрами (:paramName)
     * @param params карта параметров
     */
    public static JSONArray executeQuery(Connection conn, String sql, Map<String, Object> params) {
        JSONArray result = new JSONArray();
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            if (conn == null || conn.isClosed()) {
                JSONObject error = new JSONObject();
                error.put("error", "Connection is null or closed");
                result.put(error);
                return result;
            }

            String processedSql = processNamedParameters(sql);
            pstmt = conn.prepareStatement(processedSql);

            if (params != null && !params.isEmpty()) {
                setParameters(pstmt, sql, params);
            }

            rs = pstmt.executeQuery();
            result = resultSetToJSON(rs);

        } catch (SQLException e) {
            System.err.println("Oracle query error: " + e.getMessage());
            e.printStackTrace();

            JSONObject error = new JSONObject();
            error.put("error", e.getMessage());
            result.put(error);
        } finally {
            closeResources(rs, pstmt, null);
        }

        return result;
    }

    /**
     * Выполняет обновление (INSERT, UPDATE, DELETE) с параметрами
     */
    public static int executeUpdate(DatabaseConfig dbConfig, String sql, Map<String, Object> params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        int result = -1;

        try {
            conn = getConnect(dbConfig);
            if (conn == null) {
                return -1;
            }

            String processedSql = processNamedParameters(sql);
            pstmt = conn.prepareStatement(processedSql);

            if (params != null && !params.isEmpty()) {
                setParameters(pstmt, sql, params);
            }

            result = pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Oracle update error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(null, pstmt, conn);
        }

        return result;
    }

    /**
     * Выполняет обновление (для обратной совместимости)
     */
    public static int executeUpdate(Connection conn, String sql) {
        return executeUpdate(conn, sql, null);
    }

    /**
     * Выполняет обновление с параметрами по существующему подключению
     */
    public static int executeUpdate(Connection conn, String sql, Map<String, Object> params) {
        PreparedStatement pstmt = null;
        int result = -1;

        try {
            if (conn == null || conn.isClosed()) {
                return -1;
            }

            String processedSql = processNamedParameters(sql);
            pstmt = conn.prepareStatement(processedSql);

            if (params != null && !params.isEmpty()) {
                setParameters(pstmt, sql, params);
            }

            result = pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Oracle update error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(null, pstmt, null);
        }

        return result;
    }

    /**
     * Преобразует ResultSet в JSONArray
     */
    private static JSONArray resultSetToJSON(ResultSet rs) throws SQLException {
        JSONArray result = new JSONArray();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            JSONObject row = new JSONObject();
            for (int i = 1; i <= columnCount; i++) {
                // Используем getColumnLabel для сохранения алиасов и регистра
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);

                if (value == null) {
                    row.put(columnName, JSONObject.NULL);
                } else if (value instanceof Clob) {
                    row.put(columnName, clobToString((Clob) value));
                } else if (value instanceof Timestamp) {
                    row.put(columnName, rs.getTimestamp(i).toString());
                } else if (value instanceof Date) {
                    row.put(columnName, rs.getDate(i).toString());
                } else {
                    row.put(columnName, value);
                }
            }
            result.put(row);
        }

        return result;
    }

    /**
     * Преобразует CLOB в строку
     */
    private static String clobToString(Clob clob) {
        if (clob == null) return null;

        try (Reader reader = clob.getCharacterStream()) {
            StringWriter writer = new StringWriter();
            char[] buffer = new char[4096];
            int charsRead;
            while ((charsRead = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, charsRead);
            }
            return writer.toString();
        } catch (Exception e) {
            System.err.println("Error reading CLOB: " + e.getMessage());
            return "[CLOB read error]";
        }
    }

    /**
     * Обрабатывает именованные параметры (:paramName) и преобразует их в ?
     * Также сохраняет карту соответствия позиций и имен параметров
     */
    private static String processNamedParameters(String sql) {
        if (sql == null || !sql.contains(":")) {
            return sql;
        }

        StringBuilder processedSql = new StringBuilder();
        StringBuilder paramName = new StringBuilder();
        boolean inParam = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (c == ':' && !inParam) {
                inParam = true;
                paramName = new StringBuilder();
                // Не добавляем : в результат
            } else if (inParam && (Character.isLetterOrDigit(c) || c == '_')) {
                paramName.append(c);
            } else if (inParam) {
                // Конец параметра
                inParam = false;
                processedSql.append('?');
                processedSql.append(c);
            } else {
                processedSql.append(c);
            }
        }

        // Если строка заканчивается параметром
        if (inParam) {
            processedSql.append('?');
        }

        return processedSql.toString();
    }

    /**
     * Устанавливает параметры в PreparedStatement на основе исходного SQL с именованными параметрами
     */
    private static void setParameters(PreparedStatement pstmt, String originalSql, Map<String, Object> params)
            throws SQLException {
        if (params == null || params.isEmpty()) return;

        // Извлекаем имена параметров в порядке их появления
        java.util.List<String> paramNames = new java.util.ArrayList<>();
        StringBuilder paramName = new StringBuilder();
        boolean inParam = false;

        for (int i = 0; i < originalSql.length(); i++) {
            char c = originalSql.charAt(i);

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
            }
        }

        // Проверяем, не заканчивается ли строка параметром
        if (inParam && paramName.length() > 0) {
            paramNames.add(paramName.toString());
        }

        // Устанавливаем значения по позициям
        for (int i = 0; i < paramNames.size(); i++) {
            String name = paramNames.get(i);
            Object value = params.get(name);

            if (value == null) {
                pstmt.setNull(i + 1, Types.VARCHAR);
            } else if (value instanceof String) {
                pstmt.setString(i + 1, (String) value);
            } else if (value instanceof Integer) {
                pstmt.setInt(i + 1, (Integer) value);
            } else if (value instanceof Long) {
                pstmt.setLong(i + 1, (Long) value);
            } else if (value instanceof Double) {
                pstmt.setDouble(i + 1, (Double) value);
            } else if (value instanceof Boolean) {
                pstmt.setBoolean(i + 1, (Boolean) value);
            } else if (value instanceof java.util.Date) {
                pstmt.setTimestamp(i + 1, new Timestamp(((java.util.Date) value).getTime()));
            } else {
                pstmt.setString(i + 1, value.toString());
            }
        }
    }

    /**
     * Закрывает ресурсы БД
     */
    private static void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignore */ }
        try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignore */ }
        try { if (conn != null) conn.close(); } catch (SQLException e) { /* ignore */ }
    }

    /**
     * Проверяет существование таблицы
     */
    public static boolean tableExists(Connection conn, String tableName, String schema) {
        if (conn == null) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM all_tables WHERE ";
        if (schema != null && !schema.isEmpty()) {
            sql += "owner = ? AND ";
        }
        sql += "table_name = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            if (schema != null && !schema.isEmpty()) {
                pstmt.setString(paramIndex++, schema.toUpperCase());
            }
            pstmt.setString(paramIndex, tableName.toUpperCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking table existence: " + e.getMessage());
        }

        return false;
    }

    /**
     * Проверяет существование схемы (пользователя) в Oracle
     */
    public static boolean schemaExists(Connection conn, String schemaName) {
        if (conn == null) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM all_users WHERE username = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, schemaName.toUpperCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking schema existence: " + e.getMessage());
        }

        return false;
    }

    /**
     * Проверяет, является ли строка числом
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Преобразование строки в CamelCase
     */
    public static String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpper = true;

        for (char c : input.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }

        return result.toString();
    }

    /**
     * Преобразование строки в camelCase (первая буква маленькая)
     */
    public static String toCamelCaseLower(String input) {
        String camelCase = toCamelCase(input);
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        return Character.toLowerCase(camelCase.charAt(0)) + camelCase.substring(1);
    }
}