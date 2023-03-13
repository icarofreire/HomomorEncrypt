/*
 * 
 */
package deepsea.utilities;

import java.io.File;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.io.InputStream;
import org.postgresql.Driver;


/**
 * 
 */
public final class JDBCConnect {
    private Connection connection = null;
    private Statement stmt = null;
    private final String ipPorta = "localhost:5432";
    private final String banco = "galen";
    private final String usuario = "postgres";
    private final String senha = "postgres";

    public JDBCConnect(){
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("Postgres driver not configured correctly.");
        }

        try {
            connection = DriverManager.getConnection("jdbc:postgresql://" + ipPorta + "/" + banco, usuario, senha);
            stmt = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
        criarTabelaSeNaoExistir();
    }

    public ResultSet executeQuery(String query){
        ResultSet result = null;
        if(stmt != null){
            try{
                result = stmt.executeQuery(query);
            }catch(SQLException e){
                try{
                    connection.rollback();
                }catch(SQLException eroll){ eroll.printStackTrace(); }
                e.printStackTrace();
            }
        }
        return result;
    }

    public boolean seConectado(){
        try{
            return (!connection.isClosed());
        }catch(SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    public void close(){
        try{
            connection.close();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public long consultarImagem(String caminhoImagem){
        long count = 0;
        final String query = "SELECT count(*) AS count FROM public.tb_images_dicom t WHERE t.absolute_path_file = '" + caminhoImagem + "';";
        try{
            ResultSet result = executeQuery(query);
            while(result.next()){
                count = result.getLong("count");
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return count;
    }

    public long ultimoIdTabela(){
        long count = 0;
        final String query = "SELECT t.id FROM public.tb_images_dicom t order by t.id desc limit 1;";
        try{
            ResultSet result = executeQuery(query);
            while(result.next()){
                count = result.getLong("id");
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return count;
    }

    public boolean inserir(List<String> values, InputStream bytes){
        boolean error = false;
        final String query =
        "INSERT INTO tb_images_dicom (" +
        "id," +
        "dicom," +
        "patient_id," +
        "patient_name," +
        "patient_age," +
        "patient_birth_date," +
        "patient_sex," +
        "institution_name," +
        "study_date," +
        "study_id," +
        "series_number," +
        "absolute_path_file" +
        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try{
            long ultimoId = ultimoIdTabela();
            PreparedStatement ps = connection.prepareStatement(query);
            int index = 1;
            ps.setLong(index, ultimoId+1);
            index++;
            ps.setBinaryStream(index, bytes);
            for(String v : values){
                index++;
                ps.setString(index, v);
            }

            int retorno = ps.executeUpdate();
            if(retorno > 0){
                error = false;
            }else{
                error = true;
            }
        }catch(SQLException e){
            error = true;
            e.printStackTrace();
        }
        return error;
    }

    public String seTabelaExiste(){
        String count = null;
        final String query = "SELECT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename  = 'tb_images_dicom');";
        try{
            ResultSet result = executeQuery(query);
            while(result.next()){
                count = result.getString("exists");
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return count;
    }

    /*
    --
    -- Type: TABLE ; Name: tb_images_dicom; Owner: postgres
    --
    CREATE TABLE public.tb_images_dicom (
        id bigint NOT NULL,
        dicom bytea NOT NULL,
        patient_id character varying(255) NOT NULL,
        patient_name character varying(255),
        patient_age character varying(255),
        patient_birth_date character varying(255),
        patient_sex character varying(255),
        institution_name character varying(255),
        study_date character varying(255),
        study_id character varying(255),
        series_number character varying(255),
        absolute_path_file character varying(255) NOT NULL
    );
    */
    public String criarTabela(){
        String count = null;
        final String query =
        "CREATE TABLE public.tb_images_dicom ( \n" +
        "id bigint NOT NULL, \n" +
        "dicom bytea NOT NULL, \n" +
        "patient_id character varying(255) NOT NULL, \n" +
        "patient_name character varying(255), \n" +
        "patient_age character varying(255), \n" +
        "patient_birth_date character varying(255), \n" +
        "patient_sex character varying(255), \n" +
        "institution_name character varying(255), \n" +
        "study_date character varying(255), \n" +
        "study_id character varying(255), \n" +
        "series_number character varying(255), \n" +
        "absolute_path_file character varying(255) NOT NULL \n" +
        ");";
        try{
            stmt.execute(query);
        }catch(SQLException e){
            e.printStackTrace();
        }
        return count;
    }

    public void criarTabelaSeNaoExistir(){
        if(seConectado()){
            String res = seTabelaExiste();
            if(res.equalsIgnoreCase("f") || res.equalsIgnoreCase("false")){
                criarTabela();
            }
        }
    }

}