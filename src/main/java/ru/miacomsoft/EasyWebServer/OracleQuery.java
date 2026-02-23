package ru.miacomsoft.EasyWebServer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.io.Reader;
import java.io.StringWriter;
import java.io.IOException;
import java.util.TimeZone;

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
            // Устанавливаем часовой пояс (опционально)
            TimeZone timeZone = TimeZone.getTimeZone("Asia/Kolkata");
            TimeZone.setDefault(timeZone);

            Class.forName(dbConfig.getDriver());
            conn = DriverManager.getConnection(
                    dbConfig.getJdbcUrl(),
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
     * Выполняет SQL запрос и возвращает результат в JSONArray
     */
    public static JSONArray executeQuery(Connection conn, String sql) {
        JSONArray result = new JSONArray();

        if (conn == null) {
            return result;
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                JSONObject row = new JSONObject();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);

                    if (value == null) {
                        row.put(columnName, JSONObject.NULL);
                    } else if (value instanceof java.sql.Timestamp) {
                        row.put(columnName, rs.getTimestamp(i).toString());
                    } else if (value instanceof java.sql.Date) {
                        row.put(columnName, rs.getDate(i).toString());
                    } else if (value instanceof java.sql.Clob) {
                        // Обработка CLOB через стандартный JDBC интерфейс
                        Clob clob = (Clob) value;
                        try (Reader reader = clob.getCharacterStream()) {
                            StringWriter writer = new StringWriter();
                            char[] buffer = new char[4096];
                            int charsRead;
                            while ((charsRead = reader.read(buffer)) != -1) {
                                writer.write(buffer, 0, charsRead);
                            }
                            row.put(columnName, writer.toString());
                        } catch (IOException e) {
                            row.put(columnName, "[CLOB read error]");
                        }
                    } else {
                        row.put(columnName, value);
                    }
                }
                result.put(row);
            }

        } catch (SQLException e) {
            System.err.println("Oracle query error: " + e.getMessage());
            JSONObject error = new JSONObject();
            error.put("error", e.getMessage());
            result.put(error);
        }

        return result;
    }

    /**
     * Выполняет обновление (INSERT, UPDATE, DELETE)
     */
    public static int executeUpdate(Connection conn, String sql) {
        if (conn == null) {
            return -1;
        }

        try (Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Oracle update error: " + e.getMessage());
            return -1;
        }
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
            sql += "owner = '" + schema.toUpperCase() + "' AND ";
        }
        sql += "table_name = '" + tableName.toUpperCase() + "'";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1) > 0;
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

        String sql = "SELECT COUNT(*) FROM all_users WHERE username = '" + schemaName.toUpperCase() + "'";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking schema existence: " + e.getMessage());
        }

        return false;
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