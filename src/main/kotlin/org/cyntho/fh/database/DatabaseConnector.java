package org.cyntho.fh.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The DatabaseConnector class provides a mysql instance to
 * interact with one database connection.
 * Additionally it provides a method called executeQuery
 * to dynamically create prepared statements
 *
 * @author  Cyntho
 * @version 1.0
 */
public class DatabaseConnector {

    private String dbHost;
    private String dbUser;
    private String dbPass;
    private String dbBase;
    private String dbPref;
    private int dbPort;

    private Connection connection = null;
    private boolean isInitialized = false;



    //TODO: escape prefix
    public DatabaseConnector(String host,
                             int port,
                             String user,
                             String pass,
                             String database,
                             String prefix){
        dbHost = host;
        dbPort = port;
        dbUser = user;
        dbPass = pass;
        dbBase = database;
        dbPref = prefix;


        System.out.println("DatabaseConnector.constructor()");

        try {
            initialize();
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private void initialize() throws Exception {
        try {
            // Load Driver class
            try {
                Class.forName("com.mysql.jdbc.Driver");
                //Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e){
                System.out.println("Unable to load driver: " + e.getMessage());
                System.exit(1);
            }

            System.out.println("Con String: " + getConnectionString());

            connection = DriverManager.getConnection(getConnectionString(), dbUser, dbPass);

            System.out.println("Database init successfully");


            isInitialized = true;
        } catch (Exception e){
            throw e;
        }
    }

    public String getConnectionString() { return ("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbBase + "?user=" + dbUser + "&pass=" + dbPass); }
    //public String getConnectionString() { return ("jdbc:mysql://localhost/teamspeak_heimdall?serverTimezone=GMT"); }

    public Connection getConnection() { return this.connection; }

    public Connection getConnectionInstance() throws SQLException {
        return DriverManager.getConnection(getConnectionString(), dbUser, dbPass);
    }

    public void close() throws SQLException {
        if (this.connection != null){
            connection.close();
        }
    }

    /* Prefixed queries */

    public ResultSet executeQuery(String qry, Object[] args){

        try {

            // Try to replace the prefix
            if (!dbPref.equalsIgnoreCase("")){
                if (qry.contains("prefix")){
                    qry = qry.replace("prefix", dbPref);
                }
            }



            PreparedStatement stmt = this.connection.prepareStatement(qry);

            int i = 1;
            for (Object o : args) {

                if (o instanceof Integer) {
                    stmt.setInt(i, Integer.parseInt(o.toString()));
                } else if (o instanceof String){
                    stmt.setString(i, o.toString());
                } else if (o instanceof Boolean){
                    stmt.setBoolean(i, (Boolean) o);
                } else if (o instanceof Long){
                    stmt.setLong(i, Long.parseLong(o.toString()));
                } else if (o instanceof Float){
                    stmt.setFloat(i, (Float) o);
                } else if (o instanceof Double){
                    stmt.setDouble(i, (Double) o);
                } else {
                    System.out.println("Undefined object type : " + o.toString());
                }
                i++;
            }

            return stmt.executeQuery();
        } catch (NumberFormatException e){
            e.printStackTrace();
        } catch (SQLException e){
            //BotLogger.getInstance().log(LogLevelType.DATABASE_ERROR, "Error while executing query: " + qry);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get a List of Strings containing all columns from the
     * requested table
     * @param table String  The name of the requested database table
     * @param ignore String[] Optional array of Columns that will not be included
     * @return List<String> on success, NULL on failure
     */
    @Deprecated
    public List<String> getDatabaseColumnsOld(String table, String... ignore){
        List<String> columns = new ArrayList<>();

        try {
            String qry = "SELECT * FROM ?";

            Connection con = getConnectionInstance();

            PreparedStatement stmt = con.prepareStatement(qry);
            stmt.setString(1, table);

            ResultSet resultSet = stmt.executeQuery();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

            for (int i = 0; i < resultSetMetaData.getColumnCount(); i++){
                String col = resultSetMetaData.getColumnName(i);
                if (!Arrays.asList(ignore).contains(col)){
                    columns.add(col);
                }
            }
            return columns;

        } catch (SQLException e){
            //BotLogger.getInstance().log(LogLevelType.DATABASE_ERROR, "Could not resolve columns for database '" + table + "', query was: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}