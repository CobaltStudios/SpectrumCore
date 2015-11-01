package net.spectrumnation.utils;

import net.spectrumnation.CoreController;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.AccessDeniedException;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Created by sirtidez on 10/31/15.
 */
public class DatabaseUtil {

    public static class DataObject {
        private Object data;
        private String key;

        public DataObject(String key, Object data) {
            this.data = data;
            this.key = key;
        }

        public Object getData() {
            return data;
        }

        public String getKey() {
            return key;
        }
    }

    //TODO: Create Condition class to handle object insertion
    public static abstract class DatabaseCommon {

        protected Connection con = null;

        @Deprecated
        public ResultSet query(String query) throws SQLException {
            return this.con.createStatement().executeQuery(query);
        }

        public ResultSet query(String query, Condition... conditions) {
            try {
                PreparedStatement st = this.con.prepareStatement(query);
                int i = 1;
                for(Condition c : conditions) {
                    st.setObject(i, c.key);
                    i++;
                }

                return st.executeQuery();
            } catch(SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Deprecated
        public int update(String query) throws SQLException {
            return this.con.createStatement().executeUpdate(query);
        }

        public int update(String query, Condition... conditions) {
            try {
                PreparedStatement st = this.con.prepareStatement(query);
                int i = 1;
                for(Condition c : conditions) {
                    st.setObject(i, c.key);
                    i++;
                }

                return st.executeUpdate();
            } catch(SQLException e) {
                e.printStackTrace();
            }

            return -1;
        }

        @SuppressWarnings("unchecked")
        public int update(String query, ArrayList<? extends Object>... objects) {
            try {
                PreparedStatement st = this.con.prepareStatement(query);
                int i = 1;
                for(ArrayList<? extends Object> array : objects) {
                    for(Object o : array) {
                        st.setObject(i, o);
                        i++;
                    }
                }

                return st.executeUpdate();
            } catch(SQLException e) {
                e.printStackTrace();
            }

            return -1;
        }

        public Connection getConnection() {
            return this.con;
        }

        public abstract void close();
        public abstract boolean checkTable(String table_name);
    }

    public static class MySQLUtil extends DatabaseCommon {
        private String HOST = "";
        private String USER = "";
        private String PASS = "";
        private String DATABASE = "";
        private String PORT = "";

        public MySQLUtil(String host, String user, String pass, String database, String port) {
            HOST = host;
            USER = user;
            PASS = pass;
            DATABASE = database;
            PORT = port;
        }

        public Connection open(boolean print) throws SQLException, ConnectException {
            Properties connectionProperties = new Properties();
            connectionProperties.put("user", USER);
            connectionProperties.put("password", PASS);

            if(print) CoreController.getLogger().info("Trying to connect to " + DATABASE + " with user: " + USER);
            this.con = DriverManager.getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE, connectionProperties);

            if(print) {
                if(this.con == null)
                    CoreController.getLogger().log(Level.WARNING, "Database connection failed!");
                else
                    CoreController.getLogger().info("Database connection succeeded!");
            }

            return this.con;
        }

        public boolean checkTable(String tablename) {
            try {
                ResultSet count = query("SELECT count(*) FROM information_schema.TABLES WHERE (TABLE_SCHEMA = '" + DATABASE + "') AND (TABLE_NAME = '" + tablename + "');");
                byte i = 0;
                if (count.next()) {
                    i = count.getByte(1);
                }
                count.close();
                return i == 1;
            } catch(SQLException e) {
                e.printStackTrace();
            }

            return false;
        }

        public void close() {
            try {
                this.con.close();
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }

        public boolean checkConnection() {
            if(con == null)     return false;
            try {
                ResultSet count = query("SELECT count(*) FROM information_schema.SHEMATA");
                boolean give = count.first();
                count.close();
                return give;
            } catch(SQLException e) {
                e.printStackTrace();
            }

            return false;
        }
    }

    public static class SQLiteUtil extends DatabaseCommon {

        public Connection open(String path, boolean print) {
            try {
                Class.forName("org.sqlite.JDBC").newInstance();
                con = DriverManager.getConnection("jdbc:sqlite:" + path);
                if(print) {
                    if(con == null) {
                        CoreController.getLogger().log(Level.WARNING, "Database connection failed!");
                    } else {
                        CoreController.getLogger().info("Database connection succeded!");
                    }
                }
            } catch(ClassNotFoundException e) {
                e.printStackTrace();
            } catch(InstantiationException e) {
                e.printStackTrace();
            } catch(IllegalAccessException e) {
                e.printStackTrace();
            } catch(SQLException e) {
                e.printStackTrace();
            }

            return con;
        }

        public boolean checkTable(String tablename) {
            if(con == null)     return false;
            String command = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='" + tablename + "'";
            byte i = 0;
            try {
                ResultSet count = query(command);
                if(count.next())
                    i = count.getByte(1);
                count.close();
            } catch(SQLException e) {
                e.printStackTrace();
            }

            return (i == 1);
        }

        public void close() {
            try {
                con.close();
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public class DatabaseHandler {
        private String path;
        private String table;
        private DatabaseType databaseType;
        private String primaryKey;

        // MySQL
        private String HOST = "";
        private String USER = "";
        private String PASS = "";
        private String DATABASE = "";
        private String PORT = "";

        private boolean init = false;
        private int constructorUsed;

        // MySQL
        public DatabaseHandler(DatabaseType databaseType, String tableName) {
            this.databaseType = databaseType;
            this.table = tableName;
            this.constructorUsed = 0;
        }

        // SQLite
        public DatabaseHandler(DatabaseType databaseType, String tableName, String path) {
            this.databaseType = databaseType;
            this.table = tableName;
            this.path = path;
            this.constructorUsed = 1;
        }

        public boolean init(String host, String user, String pass, String database, String port, String primaryKey) {
            this.HOST = host;
            this.USER = user;
            this.PASS = pass;
            this.DATABASE = database;
            this.PORT = port;
            this.primaryKey = primaryKey;
            CoreController.getLogger().info("Loading database into memory...");
            if(databaseType.equals(DatabaseType.MYSQL)) {
                if(constructorUsed != 0) {
                    new IllegalAccessError("Improper constructor used for MySQL database use!").printStackTrace();
                    return false;
                }
                if(mysqlIsIinstalled()) {
                    if (createTable()) {
                        if(!mysqlIsUsable()) {
                            CoreController.getLogger().log(Level.WARNING, "Could not connect to database!");
                            return false;
                        } else {
                            init = true;
                            CoreController.getLogger().info("Database loaded correctly!");
                            return true;
                        }
                    } else {
                        CoreController.getLogger().log(Level.WARNING, "Could not check for a table!");
                        return false;
                    }
                } else {
                    CoreController.getLogger().log(Level.WARNING, "MySQL is not installed!");
                    return false;
                }
            } else if(databaseType.equals(DatabaseType.SQLITE)) {
                CoreController.getLogger().info("Checking database file...");
                if(constructorUsed == 1) {
                    if (path == null) {
                        new NullPointerException("The path cannot be null!").printStackTrace();
                        return false;
                    }
                } else {
                    new IllegalAccessError("Improper constructor used for SQLite database use!").printStackTrace();
                    return false;
                }

                CoreController.getLogger().info("Checking connection...");
                if(sqliteIsInstalled()) {
                    if(createTable()) {
                        if(!sqliteUsable()) {
                            CoreController.getLogger().log(Level.WARNING, "SQLite is not usable!");
                            return false;
                        } else {
                            init = true;
                            CoreController.getLogger().info("Database loaded correctly!");
                            return true;
                        }
                    } else {
                        CoreController.getLogger().log(Level.WARNING, "Could not check for a table!");
                        return false;
                    }
                } else {
                    CoreController.getLogger().log(Level.WARNING, "SQLite is not installed!");
                }
            } else {
                new NullPointerException("The DatabaseType can't be null!").printStackTrace();
                return false;
            }

            return false;
        }

        public List<Object> getValues(String data, Condition... conditions) {
            if(init) {
                if(databaseType.equals(DatabaseType.MYSQL)) {
                    return getValueMySQL(data, conditions);
                } else if(databaseType.equals(DatabaseType.SQLITE)) {
                    return getValueSQLite(data, conditions);
                }
            } else {
                new IllegalStateException("The DatabaseHandler wasn't initiated yet!").printStackTrace();
                return null;
            }
            return null;
        }

        public void addColumn(String column, ObjectType type) {
            if(init) {
                if(databaseType.equals(DatabaseType.MYSQL)) {
                    addColumnMySQL(column, type);
                } else if(databaseType.equals(DatabaseType.SQLITE)) {
                    addColumnSQLite(column, type);
                }
            } else {
                new IllegalStateException("The DatabaseHandler wasn't initialized yet").printStackTrace();
            }
            return;
        }

        private String getValueCommandStringSet(ArrayList<String> keys, ArrayList<Object> values) {
            String main = "";
            if(databaseType.equals(DatabaseType.MYSQL)) {
                main = "REPLACE INTO `" + table + "` (";
            } else if(databaseType.equals(DatabaseType.SQLITE)) {
                main = "INSERT OR REPLACE INTO `" + table + "` (";
            }

            for(String key: keys) {
                if(main.endsWith("`")) {
                    main = main + ", `" + key + "`";
                } else {
                    main = main + "`" + key + "`";
                }
            }

            main = main + ") VALUES (";
            for(int i = 0; i < values.size(); i++) {
                if(main.endsWith("?")) {
                    main = main + ", ?";
                } else {
                    main = main + "?";
                }
            }

            main = main + ");";
            return main;
        }

        private List<String> getValueCommandStringUpdate(ArrayList<String> keys, ArrayList<Object> values, Condition... conditions) {
            List<String> commands = new LinkedList<String>();
            List<Object> listObjectGet = getValues(primaryKey, conditions);
            if(listObjectGet.size() == 0) {
                commands.add(getValueCommandStringSet(keys, values));
                return commands;
            } else {
                for(Object actualPrimaryKey : listObjectGet) {
                    String main = "";
                    if(databaseType.equals(DatabaseType.MYSQL)) {
                        main = main + "REPLACE INTO `" + table + "` (";
                    } else if(databaseType.equals(DatabaseType.SQLITE)) {
                        main = main + "INSERT OR REPLACE INTO `" + table + "` (";
                    }
                    main = main + ", `" + primaryKey + "`";

                    for(String key : keys) {
                        main = main + ", `" + key + "`";
                    }

                    main = main + ") VALUES (" + actualPrimaryKey.toString();

                    for(int i = 0; i < values.size(); i++) {
                        main = main + ", ?";
                    }
                    main = main + ")";
                    commands.add(main);
                }
            }

            return commands;
        }

        public void insertValueForce(List<DataObject> objects) {
            DataObject[] array = objects.toArray(new DataObject[objects.size()]);
            insertValueForce(array);
        }

        @SuppressWarnings("unchecked")
        public void insertValueForce(DataObject[] objects) {
            if(init) {
                ArrayList<String> keys = new ArrayList<String>(objects.length + 1);
                ArrayList<Object> values = new ArrayList<Object>(objects.length + 1);
                for(DataObject d : objects) {
                    keys.add(d.getKey());
                    values.add(d.getData());
                }

                if(databaseType.equals(DatabaseType.MYSQL)) {
                    MySQLUtil db = new MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
                    try {
                        db.open(false);
                    } catch(Exception e) {
                        e.printStackTrace();
                    } finally {
                        db.close();
                    }
                } else if(databaseType.equals(DatabaseType.SQLITE)) {
                    SQLiteUtil db = new SQLiteUtil();
                    try {
                        String command = getValueCommandStringSet(keys, values);
                        db.open(path, false);
                        db.update(command, values);
                    } catch(Exception e) {
                        e.printStackTrace();
                    } finally {
                        db.close();
                    }
                } else {
                    new NullPointerException().printStackTrace();
                    return;
                }
            } else {
                new IllegalStateException("The DatabaseHandler wasn't initiated!").printStackTrace();
            }

            return;
        }

        public void insertOrUpdateValue(List<DataObject> objects, Condition... conditions) {
            DataObject[] array = objects.toArray(new DataObject[objects.size()]);
            insertOrUpdateValue(array, conditions);
        }

        public void insertOrUpdateValue(DataObject[] objects, Condition... conditions) {
            if(init) {
                ArrayList<String> keys = new ArrayList<String>(objects.length + 1);
                ArrayList<Object> values = new ArrayList<Object>(objects.length + 1);
                for(DataObject d : objects) {
                    keys.add(d.getKey());
                    values.add(d.getData());
                }

                if(databaseType.equals(DatabaseType.MYSQL)) {
                    MySQLUtil db = new MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
                    try {
                        db.open(false);
                        List<String> allCommands = getValueCommandStringUpdate(keys, values, conditions);
                        for(String command : allCommands) {
                            db.update(command, values);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (ConnectException e) {
                        e.printStackTrace();
                    } finally {
                        db.close();
                    }
                } else if(databaseType.equals(DatabaseType.SQLITE)) {
                    SQLiteUtil db = new SQLiteUtil();
                    try {
                        List<String> allCommands = getValueCommandStringUpdate(keys, values, conditions);
                        db.open(path, false);
                        for(String command : allCommands) {
                            db.update(command, values);
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    } finally {
                        db.close();
                    }
                } else {
                    new NullPointerException("The database type must not be null!").printStackTrace();
                    return;
                }
            } else {
                new IllegalStateException("The DatabaseHandler wasn't initiated!").printStackTrace();
                return;
            }
        }

        public boolean existInTable(Condition... conditions) {
            return (numberObjectsInTable(conditions) > 0);
        }

        public int numberObjectsInTable(Condition... conditions) {
            if(init) {
                String command = null;
                for(Condition c : conditions) {
                    if(command == null) {
                        command = "SELECT count(*) FROM" + table + "WHERE " + c.column_key + "=? ";
                    } else {
                        command = command + "AND " +c.column_key + "=? ";
                    }
                }

                if(databaseType.equals(DatabaseType.MYSQL)) {
                    MySQLUtil db = new MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
                    try {
                        db.open(false);
                        ResultSet result = db.query(command, conditions);
                        byte i = 0;
                        if(result.next()) {
                            i = result.getByte(1);
                        }
                        result.close();
                        return i;
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (ConnectException e) {
                        e.printStackTrace();
                    } finally {
                        db.close();
                    }
                } else if(databaseType.equals(DatabaseType.SQLITE)) {
                    SQLiteUtil db  = new SQLiteUtil();
                    try {
                        db.open(path, false);
                        ResultSet result = db.query(command, conditions);
                        byte i = 0;
                        if(result.next()) {
                            i = result.getByte(1);
                        }
                        result.close();
                        return i;
                    } catch(Exception e) {
                        e.printStackTrace();
                    } finally {
                        db.close();
                    }
                } else {
                    new NullPointerException().printStackTrace();
                    return 0;
                }
            } else {
                new IllegalStateException("The DatabaseHandler wasn't initiated!").printStackTrace();
                return 0;
            }

            return 0;
        }

        private boolean createTable() {
            if(databaseType.equals(DatabaseType.MYSQL)) {
                return createTableMySQL();
            } else if(databaseType.equals(DatabaseType.SQLITE)) {
                return createTableSQLite();
            } else {
                new NullPointerException().printStackTrace();
                return false;
            }
        }

        @SuppressWarnings("deprecation")
        public void clearTable() {
            if(init) {
                String command_clear = "DELETE FROM " + table;
                String command_reset;
                if(databaseType.equals(DatabaseType.MYSQL)) {
                    command_reset = "ALTER TABLE " + table + " AUTO_INCREMENT = 1";
                } else if(databaseType.equals((DatabaseType.SQLITE))) {
                    command_reset = "DELETE FROM sqlite_sequence WHERE name='" + table + "'";
                } else {
                    return;
                }

                if(databaseType.equals(DatabaseType.MYSQL)) {
                    MySQLUtil db = new MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
                    try {
                        db.open(false);
                        db.update(command_clear);
                        db.update(command_reset);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (ConnectException e) {
                        e.printStackTrace();
                    } finally {
                        db.close();
                    }
                } else if(databaseType.equals(DatabaseType.SQLITE)) {
                    SQLiteUtil db = new SQLiteUtil();
                    try {
                        db.open(path, false);
                        db.update(command_clear);
                        db.update(command_reset);
                    } catch(Exception e) {
                        e.printStackTrace();
                    } finally {
                        db.close();
                    }
                } else {
                    new NullPointerException().printStackTrace();
                    return;
                }
            } else {
                new IllegalStateException("The DatabaseHandler wan't initiated!").printStackTrace();
                return;
            }
        }

        private String getValueCommandStringDelete(Condition... conditions) {
            String result = "DELETE FROM `" + table + "` WHERE ";
            for(int i = 0; i < conditions.length; i++) {
                Condition c = conditions[i];
                if(conditions.length > (i + 1)) {
                    result = result + c.column_key + "=? AND ";
                } else {
                    result = result + c.column_key + "=?";
                }
            }

            return result;
        }

        public void deleteObject(Condition... conditions) {
            if(init) {
                if(databaseType.equals(DatabaseType.MYSQL)) {
                    MySQLUtil db = new MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
                    try {
                        db.open(false);
                        String command = getValueCommandStringDelete(conditions);
                        db.update(command, conditions);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (ConnectException e) {
                        e.printStackTrace();
                    } finally {
                        db.close();
                    }
                } else if(databaseType.equals(DatabaseType.SQLITE)) {
                    SQLiteUtil db = new SQLiteUtil();
                    try {
                        String command = getValueCommandStringDelete(conditions);
                        db.open(path, false);
                        db.update(command, conditions);
                    } catch(Exception e) {
                        e.printStackTrace();
                    } finally {
                        db.close();
                    }
                } else {
                    new NullPointerException().printStackTrace();
                    return;
                }
            } else {
                new IllegalStateException("The DatabaseHandler wasn't initiated!").printStackTrace();
                return;
            }
        }

        private boolean mysqlIsIinstalled() {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        private boolean mysqlIsUsable() {
            MySQLUtil db = null;
            try {
                db = new MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
                db.open(true);
                if(db.checkTable(table)) {
                    db.close();
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (ConnectException e) {
                e.printStackTrace();
            } finally {
                db.close();
            }
            return false;
        }

        private boolean sqliteIsInstalled() {
            try {
                Class.forName("org.sqlite.JDBC");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        private boolean sqliteUsable() {
            SQLiteUtil db = null;
            try {
                db = new SQLiteUtil();
                db.open(path, false);
                if(db.checkTable(table)) {
                    db.close();
                    return true;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @SuppressWarnings("deprecation")
        private boolean createTableSQLite() {
            SQLiteUtil db = new SQLiteUtil();
            File f = new File(path);
            try {
                if(!f.getParentFile().exists()) {
                    if(f.getParentFile().getParentFile().canWrite())  {
                        f.mkdirs();
                        if(!f.getParentFile().exists()) {
                            new NullPointerException("The folder where the database should be written doesn't exist!").printStackTrace();
                            return false;
                        }
                    }
                }

                if(!f.exists()) {
                    if(f.getParentFile().canWrite()) {
                        new File(path).createNewFile();
                    } else {
                        new AccessDeniedException(f.getName()).printStackTrace();
                        return false;
                    }
                }

                db.open(path, true);
                String command = "CREATE TABLE IF NOT EXISTS " + table + " ( " + primaryKey + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT);";
                db.update(command);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                db.close();
            }

            return true;
        }

        private List<Object> getValueSQLite(String data, Condition... conditions) {
            SQLiteUtil db = new SQLiteUtil();
            List<Object> ret = new LinkedList<Object>();
            try {
                db.open(path, false);
                String command = "SELECT * FROM `" + table + "` WHERE ";
                for(int i = 0; i < conditions.length; i++) {
                    Condition con = conditions[i];
                    if(i == 0) command = command + con.column_key + "=? ";
                    else command = command + "AND " + con.column_key + "=? ";
                }
                command = command + ";";
                ResultSet result = db.query(command, conditions);
                while(result.next()) {
                    ret.add(result.getObject(data));
                }
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                db.close();
            }

            return ret;
        }

        @SuppressWarnings("deprecation")
        private boolean columnExistSQLite(String column) {
            SQLiteUtil db = new SQLiteUtil();
            try {
                db.open(path, false);
                if (db.checkTable(table))
                    db.query("SELECT " + column + " FROM " + table + ";");
            } catch (SQLException e) {
                return false;
            } finally {
                db.close();
            }

            return true;
        }

        @SuppressWarnings("deprecation")
        private void addColumnSQLite(String column, ObjectType type) {
            SQLiteUtil db = new SQLiteUtil();
            try {
                db.open(path, false);
                if(db.checkTable(table)) {
                    if(!columnExistSQLite(column)) {
                        String command = "ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + type.toString() + "(8000) DEFAULT NULL;";
                        db.update(command);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                db.close();
            }
        }

        @SuppressWarnings("deprecation")
        private boolean createTableMySQL() {
            MySQLUtil db = new MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
            try {
                db.open(false);
                if(db.checkConnection()) {
                    if(!db.checkTable(table)) {
                        String command = "CREATE TABLE IF NOT EXISTS `" + table + "` ( `" + primaryKey + " INTEGER NOT NULL AUTO_INCREMENT, PRIMARY KEY (`" + primaryKey + "`)) ENGINE=InnoDB;";
                        db.update(command);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            } catch (ConnectException e) {
                e.printStackTrace();
                return false;
            } finally {
                db.close();
            }
            return true;
        }

        private List<Object> getValueMySQL(String data, Condition... conditions) {
            MySQLUtil db = new MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
            List<Object> ret = new LinkedList<Object>();
            try {
                db.open(false);
                String command = "SELECT * FROM `" + table + "` WHERE ";
                for(int i = 0; i < conditions.length; i++) {
                    Condition con = conditions[i];
                    if(i == 0) command = command + con.column_key + "=? ";
                    else command = command + "AND " + con.column_key + "=? ";
                }
                command = command + ";";
                ResultSet result = db.query(command, conditions);
                while(result.next()) {
                    ret.add(result.getObject(data));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (ConnectException e) {
                e.printStackTrace();
            } finally {
                db.close();
            }

            return ret;
        }

        @SuppressWarnings("deprecation")
        private boolean columnExistMySQL(String column) {
            MySQLUtil db = new MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
            try {
                db.open(false);
                if(db.checkTable(table)) {
                    db.query("SELECT " + column + " FROM " + table);
                }
            } catch (SQLException e) {
                return false;
            } catch (ConnectException e) {
                return false;
            } finally {
                db.close();
            }

            return true;
        }

        @SuppressWarnings("deprecation")
        private void addColumnMySQL(String column, ObjectType type) {
            MySQLUtil db = new MySQLUtil(HOST, USER, PASS, DATABASE, PORT);
            try {
                db.open(false);
                if(db.checkConnection()) {
                    if(db.checkTable(table)) {
                        if(!columnExistMySQL(column)) {
                            db.update("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + type.toString() + "(8000) DEFAULT NULL;");
                        }
                    }
                } else {
                    CoreController.getLogger().log(Level.WARNING, "Error while using MySQL! Could not connect to database!");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (ConnectException e) {
                e.printStackTrace();
            } finally {
                db.close();
            }
        }
    }

    public static class Condition {
        String key;
        String column_key;

        public Condition(String column_key, String key) {
            this.key = key;
            this.column_key = column_key;
        }
    }

    public enum ObjectType {
        INTEGER,
        BIGINT,
        CHARACTER,
        VARCHAR,
        BOOLEAN,
        FLOAT,
        DECIMAL,
        ARRAY,
        DATE,
        TIME,
        NONE,
        TIMESTAMP;
    }

    public enum DatabaseType {
        MYSQL,
        SQLITE;
    }
}
