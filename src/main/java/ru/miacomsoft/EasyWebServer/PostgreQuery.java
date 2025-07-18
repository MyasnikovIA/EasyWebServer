package ru.miacomsoft.EasyWebServer;

import org.json.JSONObject;

import java.sql.*;
import java.util.HashMap;

/**
 * класс работы с PostgreSQL
 */
public class
PostgreQuery {
    public static HashMap<String, HashMap<String, Object>> procedureList = new HashMap<>();

    /**
     * Функция подключения к Postgre SQL
     * URL подключения берется из конфигурационного файла config.ini (DATABASE_NAME)
     *
     * @param userName
     * @param userPass
     * @return
     */
    public static Connection getConnect(String userName, String userPass) {
        Connection conn = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            // c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres","postgres", "postgres");
            // conn = DriverManager.getConnection("jdbc:postgresql://" + server + "/" + dbName, userName, userPass);
            conn = DriverManager.getConnection(ServerConstant.config.DATABASE_NAME, userName, userPass);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            return null;
        }
        return conn;
    }

    public static String getVercionPostgres(String userName, String userPass) {
        Connection conn = null;
        String sql = " select json_agg(version());";
        StringBuffer result = new StringBuffer();
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            // c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres","postgres", "postgres");
            // conn = DriverManager.getConnection("jdbc:postgresql://" + server + "/" + dbName, userName, userPass);
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
            result.append(e.getClass().getName() + ": " + e.getMessage());
        }
        return result.toString();
    }
    /**
     * Функция подключения к Postgre SQL
     * URL подключения берется из конфигурационного файла config.ini (DATABASE_NAME)
     *
     * @param userName - имя пользователя
     * @param userPass - пароль пользователя
     * @param info     - JSON результат создания функции
     * @return
     */
    public static Connection getConnect(String userName, String userPass, JSONObject info) {
        Connection conn = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            // c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres","postgres", "postgres");
            // conn = DriverManager.getConnection("jdbc:postgresql://" + server + "/" + dbName, userName, userPass);
            conn = DriverManager.getConnection(ServerConstant.config.DATABASE_NAME, userName, userPass);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            info.put("error", e.getClass().getName() + ": " + e.getMessage());
            return null;
        }
        return conn;
    }

    /**
     * Функция создания таблицы в Postgre (демонстрация, потом УДАЛИТЬ)
     *
     * @param conn
     */
    public void createTable(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            String sql = "CREATE TABLE  IF NOT EXISTS BUSINESS " +
                    "(ID             SERIAL PRIMARY KEY," +
                    " NAME           TEXT    NOT NULL, " +
                    " AGE            INT     NOT NULL, " +
                    " ADDRESS        CHAR(50), " +
                    " SALARY         REAL)";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Функция создания процедуры в Postgre (демонстрация, потом УДАЛИТЬ)
     *
     * @param conn
     */
    public void clearWebPageProcedure(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("" +
                    "CREATE OR REPLACE PROCEDURE clear_"+ServerConstant.config.APP_NAME+"_proc() language plpgsql  AS $$\n" +
                    "DECLARE\n" +
                    "    table_name text;\n" +
                    "BEGIN\n" +
                    "    FOR table_name IN\n" +
                    "        SELECT quote_ident(proc.proname) FROM pg_catalog.pg_namespace namSpace\n" +
                    "          JOIN pg_catalog.pg_proc proc ON proc.pronamespace = namSpace.oid\n" +
                    "         WHERE namSpace.nspname = 'public'\n" +
                    "           and proc.proname LIKE '"+ServerConstant.config.APP_NAME+"_%'\n" +
                    "    LOOP\n" +
                    "        EXECUTE 'DROP PROCEDURE IF EXISTS ' || table_name;\n" +
                    "    END LOOP;\n" +
                    "END $$;\n\n");
            CallableStatement cs2 = conn.prepareCall("call clear_"+ServerConstant.config.APP_NAME+"_proc();");
            cs2.execute();
        } catch (SQLException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
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
        } catch (SQLException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
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
        } catch (SQLException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Вызов  процедуры из Postgre (демонстрация УДАЛИТЬ)
     * @param conn
     */
    public void collProcedure(Connection conn) {
        /*
            Проверка наличия функции
            select 1 from pg_catalog.pg_proc proc where proc.proname = 'myprocinout';

            Список всех пользовательских функций
            SELECT proc.proname,
                   proc.proargnames,
                   proc.prosrc
              FROM pg_catalog.pg_namespace namSpace
                   JOIN pg_catalog.pg_proc proc on proc.pronamespace = namSpace.oid
             WHERE namSpace.nspname = 'public';
         */

        try {
            Statement stmt = conn.createStatement();
            CallableStatement cs = conn.prepareCall("call myprocinout(?);");
            cs.registerOutParameter(1, Types.VARCHAR);
            cs.setString(1, "a string"); // Входящие переменные
            cs.execute();
            String outParam = cs.getString(1);  // Получение ответа
        } catch (SQLException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }


}
