package ru.miacomsoft.EasyWebServer;

import org.json.JSONObject;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * класс работы с PostgreSQL
 */
public class PostgreQuery {
    public static ConcurrentHashMap<String, Map<String, Object>> procedureList = new ConcurrentHashMap<>();

    /**
     * Функция подключения к Postgre SQL с автоматическим созданием базы данных
     * URL подключения берется из конфигурационного файла config.ini (DATABASE_NAME)
     *
     * @param userName - имя пользователя
     * @param userPass - пароль пользователя
     * @return
     */
    public static Connection getConnect(String userName, String userPass) {
        return getConnect(userName, userPass, null);
    }

    /**
     * Функция подключения к Postgre SQL с автоматическим созданием базы данных
     *
     * @param userName - имя пользователя
     * @param userPass - пароль пользователя
     * @param info     - JSON результат создания функции (опционально)
     * @return
     */
    public static Connection getConnect(String userName, String userPass, JSONObject info) {
        Connection conn = null;

        try {
            // Загружаем драйвер PostgreSQL
            Class.forName("org.postgresql.Driver");

            // Пробуем подключиться напрямую к указанной базе данных
            try {
                conn = DriverManager.getConnection(ServerConstant.config.DATABASE_NAME, userName, userPass);
                System.out.println("✅ Successfully connected to database: " + extractDatabaseName(ServerConstant.config.DATABASE_NAME));

                if (info != null) {
                    info.put("connection", "success");
                    info.put("database_exists", true);
                    info.put("database", extractDatabaseName(ServerConstant.config.DATABASE_NAME));
                }

                return conn;

            } catch (SQLException e) {
                // Анализируем ошибку подключения
                String sqlState = e.getSQLState();

                // SQLState "3D000" означает "database does not exist"
                // SQLState "28P01" - invalid password
                // SQLState "08001" - connection error
                if ("3D000".equals(sqlState)) {
                    System.out.println("⚠️ Database does not exist. Attempting to create it...");

                    if (info != null) {
                        info.put("database_exists", false);
                        info.put("create_attempt", true);
                        info.put("error", e.getMessage());
                    }

                    // Создаем базу данных
                    if (createDatabase(userName, userPass, info)) {
                        System.out.println("✅ Database created successfully. Reconnecting...");

                        // Повторно подключаемся к созданной базе данных
                        try {
                            conn = DriverManager.getConnection(ServerConstant.config.DATABASE_NAME, userName, userPass);
                            System.out.println("✅ Successfully connected to newly created database: " + extractDatabaseName(ServerConstant.config.DATABASE_NAME));

                            if (info != null) {
                                info.put("database_created", true);
                                info.put("reconnect", "success");
                            }

                            return conn;
                        } catch (SQLException ex) {
                            System.err.println("❌ Failed to connect after database creation: " + ex.getMessage());
                            if (info != null) {
                                info.put("reconnect_error", ex.getMessage());
                            }
                            return null;
                        }
                    } else {
                        System.err.println("❌ Failed to create database");
                        return null;
                    }
                } else {
                    // Другая ошибка подключения (неправильный пароль, сервер недоступен и т.д.)
                    System.err.println("❌ Database connection error: " + e.getMessage());
                    System.err.println("SQL State: " + sqlState);

                    if (info != null) {
                        info.put("connection_error", e.getMessage());
                        info.put("sql_state", sqlState);
                    }

                    return null;
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ PostgreSQL JDBC Driver not found: " + e.getMessage());
            if (info != null) {
                info.put("driver_error", "PostgreSQL JDBC Driver not found: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Получение текущей кодировки и collation из шаблонной базы данных
     */
    private static String[] getTemplateCollation(Connection adminConn) {
        String[] collation = new String[2]; // [0] - encoding, [1] - collation
        try {
            Statement stmt = adminConn.createStatement();
            // Получаем кодировку template1
            ResultSet rs = stmt.executeQuery(
                    "SELECT pg_encoding_to_char(encoding), datcollate FROM pg_database WHERE datname = 'template1'"
            );
            if (rs.next()) {
                collation[0] = rs.getString(1); // encoding
                collation[1] = rs.getString(2); // collation
                System.out.println("📌 Template database encoding: " + collation[0]);
                System.out.println("📌 Template database collation: " + collation[1]);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("⚠️ Could not get template collation: " + e.getMessage());
            // Значения по умолчанию
            collation[0] = "UTF8";
            collation[1] = "en_US.utf8";
        }
        return collation;
    }

    /**
     * Создание базы данных, если она не существует
     *
     * @param adminUser - административный пользователь (обычно postgres)
     * @param adminPass - пароль администратора
     * @param info      - JSON объект для информации о процессе
     * @return true если база данных создана или уже существует
     */
    private static boolean createDatabase(String adminUser, String adminPass, JSONObject info) {
        Connection adminConn = null;
        Statement stmt = null;

        try {
            // Извлекаем имя базы данных из URL
            String dbUrl = ServerConstant.config.DATABASE_NAME;
            String dbName = extractDatabaseName(dbUrl);
            String hostPort = extractHostPort(dbUrl);

            if (dbName == null || dbName.isEmpty()) {
                System.err.println("❌ Could not extract database name from URL: " + dbUrl);
                if (info != null) {
                    info.put("create_error", "Could not extract database name from URL");
                }
                return false;
            }

            System.out.println("📌 Database name to create: " + dbName);
            System.out.println("📌 Host: " + hostPort);

            // Формируем URL для подключения к стандартной базе данных postgres
            String adminDbUrl = "jdbc:postgresql://" + hostPort + "/postgres";
            System.out.println("📌 Connecting to admin database: " + adminDbUrl);

            // Подключаемся к базе данных postgres
            adminConn = DriverManager.getConnection(adminDbUrl, adminUser, adminPass);
            stmt = adminConn.createStatement();

            // Проверяем, существует ли уже база данных
            ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'"
            );

            boolean exists = rs.next();
            rs.close();

            if (exists) {
                System.out.println("✅ Database already exists: " + dbName);
                if (info != null) {
                    info.put("database_already_exists", true);
                }
                return true;
            }

            // Получаем кодировку из template1
            String[] templateCollation = getTemplateCollation(adminConn);
            String encoding = templateCollation[0];
            String collation = templateCollation[1];

            // Создаем базу данных, используя template0 для избежания проблем с кодировкой
            System.out.println("🔄 Creating database: " + dbName);

            // Простой вариант - без указания кодировки (использует настройки template1)
            String createDbSQL = "CREATE DATABASE \"" + dbName + "\"";

            // Пробуем сначала простой вариант
            try {
                stmt.executeUpdate(createDbSQL);
                System.out.println("✅ Database created successfully (simple)");
            } catch (SQLException e) {
                // Если простой вариант не работает, пробуем с template0
                System.out.println("⚠️ Simple creation failed, trying with template0...");
                createDbSQL = "CREATE DATABASE \"" + dbName + "\" TEMPLATE template0";

                try {
                    stmt.executeUpdate(createDbSQL);
                    System.out.println("✅ Database created successfully with template0");
                } catch (SQLException e2) {
                    // Последний вариант - с явным указанием кодировки
                    System.out.println("⚠️ Template0 creation failed, trying with explicit encoding...");
                    createDbSQL = "CREATE DATABASE \"" + dbName +
                            "\" ENCODING '" + encoding + "' LC_COLLATE '" + collation +
                            "' LC_CTYPE '" + collation + "' TEMPLATE template0";
                    stmt.executeUpdate(createDbSQL);
                    System.out.println("✅ Database created successfully with explicit encoding");
                }
            }

            System.out.println("✅ Database created: " + dbName);

            if (info != null) {
                info.put("database_created", true);
                info.put("created_database", dbName);
            }

            // Даем небольшую задержку, чтобы база данных успела создаться
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            return true;

        } catch (SQLException e) {
            System.err.println("❌ Error creating database: " + e.getMessage());
            e.printStackTrace();

            if (info != null) {
                info.put("create_error", e.getMessage());
                info.put("create_sql_state", e.getSQLState());
            }

            return false;

        } finally {
            try {
                if (stmt != null) stmt.close();
                if (adminConn != null) adminConn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Проверка существования базы данных
     *
     * @return true если база данных существует
     */
    public static boolean checkDatabaseExists() {
        return checkDatabaseExists(null);
    }

    /**
     * Проверка существования базы данных с информационным объектом
     *
     * @param info - JSON объект для информации
     * @return true если база данных существует
     */
    public static boolean checkDatabaseExists(JSONObject info) {
        Connection conn = getConnect(ServerConstant.config.DATABASE_USER_NAME,
                ServerConstant.config.DATABASE_USER_PASS,
                info);

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return true;
        }

        return false;
    }

    /**
     * Создание таблицы, если она не существует
     *
     * @param conn      - соединение с базой данных
     * @param tableName - имя таблицы
     * @param createSQL - SQL для создания таблицы
     * @return true если таблица создана или уже существует
     */
    public static boolean createTableIfNotExists(Connection conn, String tableName, String createSQL) {
        if (!tableExists(conn, tableName)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createSQL);
                System.out.println("✅ Table created: " + tableName);
                return true;
            } catch (SQLException e) {
                System.err.println("❌ Error creating table " + tableName + ": " + e.getMessage());
                return false;
            }
        }
        System.out.println("ℹ️ Table already exists: " + tableName);
        return false;
    }

    /**
     * Проверка существования таблицы
     *
     * @param conn      - соединение с базой данных
     * @param tableName - имя таблицы
     * @return true если таблица существует
     */
    public static boolean tableExists(Connection conn, String tableName) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"});
            boolean exists = rs.next();
            rs.close();
            return exists;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Проверка существования схемы
     *
     * @param conn       - соединение с базой данных
     * @param schemaName - имя схемы
     * @return true если схема существует
     */
    public static boolean schemaExists(Connection conn, String schemaName) {
        try {
            ResultSet rs = conn.getMetaData().getSchemas(null, schemaName);
            boolean exists = rs.next();
            rs.close();
            return exists;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Создание схемы, если она не существует
     *
     * @param conn       - соединение с базой данных
     * @param schemaName - имя схемы
     * @return true если схема создана или уже существует
     */
    public static boolean createSchemaIfNotExists(Connection conn, String schemaName) {
        if (!schemaExists(conn, schemaName)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE SCHEMA \"" + schemaName + "\"");
                System.out.println("✅ Schema created: " + schemaName);
                return true;
            } catch (SQLException e) {
                System.err.println("❌ Error creating schema " + schemaName + ": " + e.getMessage());
                return false;
            }
        }
        System.out.println("ℹ️ Schema already exists: " + schemaName);
        return false;
    }

    /**
     * Выполнение SQL скрипта из строки
     *
     * @param conn      - соединение с базой данных
     * @param sqlScript - SQL скрипт
     * @return true если скрипт выполнен успешно
     */
    public static boolean executeSqlScript(Connection conn, String sqlScript) {
        String[] statements = sqlScript.split(";");
        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                sql = sql.trim();
                if (!sql.isEmpty()) {
                    stmt.executeUpdate(sql);
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Error executing SQL script: " + e.getMessage());
            return false;
        }
    }

    /**
     * Извлечение имени базы данных из JDBC URL
     *
     * @param jdbcUrl - JDBC URL
     * @return имя базы данных
     */
    public static String extractDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            return null;
        }

        // Пример: jdbc:postgresql://localhost:5432/mydb
        int lastSlash = jdbcUrl.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < jdbcUrl.length() - 1) {
            String afterSlash = jdbcUrl.substring(lastSlash + 1);
            // Удаляем возможные параметры после ?
            int paramIndex = afterSlash.indexOf('?');
            if (paramIndex >= 0) {
                return afterSlash.substring(0, paramIndex);
            }
            return afterSlash;
        }
        return null;
    }

    /**
     * Извлечение хоста и порта из JDBC URL
     *
     * @param jdbcUrl - JDBC URL
     * @return строка с хостом и портом
     */
    public static String extractHostPort(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            return null;
        }

        // Пример: jdbc:postgresql://localhost:5432/mydb
        String withoutPrefix = jdbcUrl.substring("jdbc:postgresql://".length());
        int slashIndex = withoutPrefix.indexOf('/');
        if (slashIndex >= 0) {
            return withoutPrefix.substring(0, slashIndex);
        }
        return withoutPrefix;
    }

    /**
     * Проверка подключения к PostgreSQL
     *
     * @return true если подключение успешно
     */
    public static boolean testConnection() {
        return testConnection(null);
    }

    /**
     * Проверка подключения к PostgreSQL с информационным объектом
     *
     * @param info - JSON объект для информации
     * @return true если подключение успешно
     */
    public static boolean testConnection(JSONObject info) {
        Connection conn = getConnect(ServerConstant.config.DATABASE_USER_NAME,
                ServerConstant.config.DATABASE_USER_PASS,
                info);

        if (conn != null) {
            try {
                // Проверяем, что соединение действительно работает
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 1");
                boolean hasResult = rs.next();
                rs.close();
                stmt.close();

                if (info != null) {
                    info.put("test_query", "success");
                }

                conn.close();
                return hasResult;
            } catch (SQLException e) {
                System.err.println("❌ Connection test query failed: " + e.getMessage());
                if (info != null) {
                    info.put("test_query_error", e.getMessage());
                }
                try {
                    conn.close();
                } catch (SQLException ex) {
                    // ignore
                }
                return false;
            }
        }

        return false;
    }

    /**
     * Функция получения версии PostgreSQL
     *
     * @param userName - имя пользователя
     * @param userPass - пароль пользователя
     * @return версия PostgreSQL
     */
    public static String getVercionPostgres(String userName, String userPass) {
        Connection conn = null;
        String sql = "SELECT version();";
        StringBuilder result = new StringBuilder();

        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(ServerConstant.config.DATABASE_NAME, userName, userPass);
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                result.append(rs.getString(1));
            }
            rs.close();
            st.close();
            conn.close();
        } catch (Exception e) {
            result.append("Error: ").append(e.getClass().getName()).append(": ").append(e.getMessage());
        }
        return result.toString();
    }

    /**
     * Функция подключения к Postgre SQL (старая версия для обратной совместимости)
     *
     * @param userName - имя пользователя
     * @param userPass - пароль пользователя
     * @param info     - JSON результат создания функции
     * @return
     */
    public static Connection getConnectOld(String userName, String userPass, JSONObject info) {
        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(ServerConstant.config.DATABASE_NAME, userName, userPass);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            if (info != null) {
                info.put("error", e.getClass().getName() + ": " + e.getMessage());
            }
            return null;
        }
        return conn;
    }

    /**
     * Функция создания таблицы в Postgre (демонстрация)
     *
     * @param conn
     */
    public void createTable(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS BUSINESS (" +
                    "ID             SERIAL PRIMARY KEY," +
                    " NAME           TEXT    NOT NULL, " +
                    " AGE            INT     NOT NULL, " +
                    " ADDRESS        CHAR(50), " +
                    " SALARY         REAL)";
            stmt.executeUpdate(sql);
            System.out.println("✅ Table BUSINESS created or already exists");
        } catch (SQLException e) {
            System.err.println("❌ Error creating table: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Функция создания процедуры в Postgre (демонстрация)
     *
     * @param conn
     */
    public void clearWebPageProcedure(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("" +
                    "CREATE OR REPLACE PROCEDURE clear_" + ServerConstant.config.APP_NAME + "_proc() language plpgsql AS $$\n" +
                    "DECLARE\n" +
                    "    table_name text;\n" +
                    "BEGIN\n" +
                    "    FOR table_name IN\n" +
                    "        SELECT quote_ident(proc.proname) FROM pg_catalog.pg_namespace namSpace\n" +
                    "          JOIN pg_catalog.pg_proc proc ON proc.pronamespace = namSpace.oid\n" +
                    "         WHERE namSpace.nspname = 'public'\n" +
                    "           and proc.proname LIKE '" + ServerConstant.config.APP_NAME + "_%'\n" +
                    "    LOOP\n" +
                    "        EXECUTE 'DROP PROCEDURE IF EXISTS ' || table_name;\n" +
                    "    END LOOP;\n" +
                    "END $$;\n\n");
            CallableStatement cs2 = conn.prepareCall("call clear_" + ServerConstant.config.APP_NAME + "_proc();");
            cs2.execute();
            System.out.println("✅ Procedure clear_" + ServerConstant.config.APP_NAME + "_proc created/updated");
        } catch (SQLException e) {
            System.err.println("❌ Error creating procedure: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Функция создания процедуры в Postgre
     * предварительно старая процедура удаляется, если она была созданна
     *
     * @param conn
     * @param nameProcedure
     * @param procText
     */
    public static void createProcedure(Connection conn, String nameProcedure, String procText) {
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("DROP PROCEDURE IF EXISTS " + nameProcedure + ";");
            stmt.execute(procText);
            System.out.println("✅ Procedure created/updated: " + nameProcedure);
        } catch (SQLException e) {
            System.err.println("❌ Error creating procedure " + nameProcedure + ": " + e.getMessage());
        }
    }

    /**
     * Функция создания в Postgre функции
     * предварительно старая функция удаляется, если она была созданна
     *
     * @param conn
     * @param nameProcedure
     * @param procText
     */
    public static void createFunction(Connection conn, String nameProcedure, String procText) {
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("DROP FUNCTION IF EXISTS " + nameProcedure + ";");
            stmt.execute(procText);
            System.out.println("✅ Function created/updated: " + nameProcedure);
        } catch (SQLException e) {
            System.err.println("❌ Error creating function " + nameProcedure + ": " + e.getMessage());
        }
    }

    /**
     * Вызов процедуры из Postgre (демонстрация)
     *
     * @param conn
     */
    public void collProcedure(Connection conn) {
        try {
            CallableStatement cs = conn.prepareCall("call myprocinout(?);");
            cs.registerOutParameter(1, Types.VARCHAR);
            cs.setString(1, "a string");
            cs.execute();
            String outParam = cs.getString(1);
            System.out.println("Procedure result: " + outParam);
        } catch (SQLException e) {
            System.err.println("❌ Error calling procedure: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Получение списка всех таблиц в базе данных
     *
     * @param conn - соединение с базой данных
     * @return массив имен таблиц
     */
    public static String[] getTableList(Connection conn) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"});

            java.util.ArrayList<String> tables = new java.util.ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
            rs.close();

            return tables.toArray(new String[0]);
        } catch (SQLException e) {
            System.err.println("❌ Error getting table list: " + e.getMessage());
            return new String[0];
        }
    }

    /**
     * Получение списка всех функций в базе данных
     *
     * @param conn - соединение с базой данных
     * @return массив имен функций
     */
    public static String[] getFunctionList(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT proname FROM pg_proc WHERE pronamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')"
            );

            java.util.ArrayList<String> functions = new java.util.ArrayList<>();
            while (rs.next()) {
                functions.add(rs.getString(1));
            }
            rs.close();
            stmt.close();

            return functions.toArray(new String[0]);
        } catch (SQLException e) {
            System.err.println("❌ Error getting function list: " + e.getMessage());
            return new String[0];
        }
    }
}