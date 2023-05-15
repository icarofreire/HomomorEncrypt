/*
 * 
 */
package deepsea.utilities;

import java.util.Vector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import org.postgresql.Driver;

import deepsea.utilities.JDBCConnection;

/**
 * classe para criar multiplos nós de conexões de banco;
 */
public final class MultiConnections {

    private Vector<JDBCConnection> connections = new Vector<JDBCConnection>();

    public void createImmediateMultiConnections() {
        /*\/ Ex add +1: */
        int ind = 1;
        createCon("ipPorta", "banco", "usuario", "senha");

        /*\/ Ex add +1: */
        createCon("ipPorta", "banco", "usuario", "senha");

        /*\/ Ex add +1: */
        createCon("ipPorta", "banco", "usuario", "senha");
    }

    public Vector<JDBCConnection> getConnections(){
        return connections;
    }

    /*\/ *** OBS: método apenas para facilitar criar muitas conexões em bancos -- 
    ATENTAR-SE em FECHAR TODAS as conexões abertas após o uso das mesmas; */
    private void createCon(String ipPorta, String banco, String usuario, String senha){
        JDBCConnection con = new JDBCConnection();
        con.createConnection(ipPorta, banco, usuario, senha);
        if(con.seConectado()){
            connections.add(con);
        }
    }

    /*\/ fechar todas as conexões; */
    public void closeConnections(){
        connections.stream().forEach(con -> {
            con.close();
        });
    }

}