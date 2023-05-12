/*
 * 
 */
package deepsea.utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import org.postgresql.Driver;


/**
 * Classe para criar objetos de conexão JDBC;
 */
public final class JDBCConnection {

    private Connection connection = null;
    private Statement statement = null;

    public boolean createConnection(String ipPorta, String banco, String usuario, String senha){
        boolean ok = false;
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Postgres driver not configured correctly.");
            ok = false;
        }

        try {
            connection = DriverManager.getConnection("jdbc:postgresql://" + ipPorta + "/" + banco, usuario, senha);
            statement = connection.createStatement();
            ok = true;
        } catch (Exception e) {
            ok = false;
            e.printStackTrace();
        }
        return ok;
    }

    /*\/ criar banco caso não exista; */
    public boolean createDB(String ipPorta, String usuario, String senha, String nomeBanco){
        boolean ok = false;
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://" + ipPorta + "/", usuario, senha);
            Statement stmt = conn.createStatement();

            boolean exists = false;
            if(stmt != null){
                long count = 0;
                String query = "SELECT count(*) FROM pg_database WHERE datname='" + nomeBanco + "';";
                ResultSet result = null;
                try{
                    result = stmt.executeQuery(query);
                    while(result.next()){
                        count = result.getLong("count");
                    }
                    exists = (count > 0);
                }catch(SQLException e){
                    e.printStackTrace();
                }
            }

            if(!exists){
                String sql = "CREATE DATABASE " + nomeBanco;
                stmt.executeUpdate(sql);
                System.out.println("Database NOT exists;");
                System.out.println("Database created successfully...");
                ok = true;
            }else{
                System.out.println("Database exists;");
                ok = false;
            }
            conn.close();
        } catch (SQLException e) {
            ok = false;
            e.printStackTrace();
        }
        return ok;
    }

    public Connection getConnection(){
        return connection;
    }

    public Statement getStatement(){
        return statement;
    }

    public boolean seConectado(){
        try{
            return (!connection.isClosed());
        }catch(SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    public ResultSet executeQuery(String query){
        ResultSet result = null;
        if(seConectado() && statement != null){
            try{
                result = statement.executeQuery(query);
            }catch(SQLException e){
                try{
                    connection.rollback();
                }catch(SQLException eroll){ eroll.printStackTrace(); }
                e.printStackTrace();
            }
        }
        return result;
    }

    public void close(){
        try{
            connection.close();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

}