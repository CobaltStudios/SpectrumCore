package net.spectrumnation.utils;

import java.net.ConnectException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

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
            connectionProperties.put("password". PASS);

            if(print)
        }
    }

}
