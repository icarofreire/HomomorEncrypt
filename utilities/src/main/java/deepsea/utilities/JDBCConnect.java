/*
 * 
 */
package deepsea.utilities;

import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * 
 */
public final class JDBCConnect {
    private Connection connection = null;
    private Statement stmt = null;
    private final String local = "localhost:3306";
    private final String banco = "jdbc";
    private final String usuario = "root";
    private final String senha = "";

    public void JDBCConnect(){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + local + "/" + banco, usuario, senha);
 
            stmt = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        } /*finally {
            try {
                stmt.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
    }

    public boolean executeQuery(String query){
        boolean f = true;
        if(stmt != null){
            try{
                stmt.execute(query);
            }catch(SQLException e){
                f = false;
                e.printStackTrace();
            }
        }
        return f;
    }

    public boolean seConectado(){
        return (connection != null);
    }

    public boolean close(){
        return (connection != null);
    }
}