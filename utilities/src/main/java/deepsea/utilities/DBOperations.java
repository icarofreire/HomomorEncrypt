/*
 * 
 */
package deepsea.utilities;

import java.io.File;
import java.nio.file.Files;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;
import java.io.InputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import org.postgresql.Driver;

/*\/ procedimentos para criar testes de autenticidade de imagens dicoms;  */
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
/*\/ parse dicom; */
import AC_DicomIO.AC_DcmStructure;
import AC_DicomIO.AC_DicomReader;

import deepsea.utilities.Compress;
import deepsea.utilities.JDBCConnection;
import deepsea.utilities.MultiConnections;
import deepsea.utilities.DBConf;

/**
 * classe de operações com bancos de dados;
 */
public final class DBOperations {
    private Connection connection = null;
    private Statement stmt = null;

    /*\/ classe para multiplas conexões com bancos;*/
    // private final MultiConnections multiConnections = new MultiConnections();

    /*\/ multiplos nós de conexões com bancos;*/
    // private final Vector<JDBCConnection> vconnections = multiConnections.getConnections();

    /*\/ informações banco principal; */
    private final String ipPorta = DBConf.ipPortaPri;
    private final String banco = DBConf.bancoPri;
    private final String usuario = DBConf.usuarioPri;
    private final String senha = DBConf.senhaPri;

    private final String query_insert =
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
        "name_file" +
        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

    private final String query_create_table =
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
        "name_file character varying(255) NOT NULL \n" +
        ");";

    public DBOperations(){
        final JDBCConnection con = new JDBCConnection();
        if(con.createConnection(ipPorta, banco, usuario, senha)){
            connection = con.getConnection();
            stmt = con.getStatement();
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
        final String query = "SELECT count(*) AS count FROM public.tb_images_dicom t WHERE t.name_file = '" + caminhoImagem + "';";
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

    public boolean inserir(Vector<String> values, InputStream bytes){
        boolean ok = false;
        try{
            long ultimoId = ultimoIdTabela();
            PreparedStatement ps = connection.prepareStatement(query_insert);
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
                ok = true;
            }else{
                ok = false;
            }
        }catch(SQLException e){
            ok = false;
            e.printStackTrace();
        }
        return ok;
    }

    /**
     * inserir em outros nós de conexão JDBC --
     * inserir os mesmos dados em outros bancos inseridos em outros servidores;
    */
    public boolean inserir(Connection connection, Vector<String> values, InputStream bytes){
        boolean ok = false;
        try{
            long ultimoId = ultimoIdTabela();
            PreparedStatement ps = connection.prepareStatement(query_insert);
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
                ok = true;
            }else{
                ok = false;
            }
        }catch(SQLException e){
            ok = false;
            e.printStackTrace();
        }
        return ok;
    }

    /*\/ inserir valores em multiplas conexões; */
    public void insertInServers(MultiConnections multiConnections, Vector<String> values, InputStream bytes){
        Vector<JDBCConnection> connections = multiConnections.getConnections();
        connections.stream().forEach(con -> {
            if(con.seConectado()){
                inserir(con.getConnection(), values, bytes);
            }
            con.close();
        });
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

    public String tamanhoBanco(){
        String count = null;
        final String query = "SELECT pg_size_pretty(pg_database_size('"+ banco +"'));";
        try{
            ResultSet result = executeQuery(query);
            while(result.next()){
                count = result.getString("pg_size_pretty");
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return count;
    }

    public long numeroRegistros(){
        long count = 0;
        final String query = "SELECT count(*) AS count FROM public.tb_images_dicom;";
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
        name_file character varying(255) NOT NULL
    );
    */
    public boolean criarTabela(){
        boolean ok = false;
        try{
            stmt.execute(query_create_table);
            ok = true;
        }catch(SQLException e){
            e.printStackTrace();
        }
        return ok;
    }

    public boolean criarTabela(Statement stmt){
        boolean ok = false;
        try{
            stmt.execute(query_create_table);
            ok = true;
        }catch(SQLException e){
            e.printStackTrace();
        }
        return ok;
    }

    public void criarTabelaSeNaoExistir(){
        if(seConectado()){
            String res = seTabelaExiste();
            if(res.equalsIgnoreCase("f") || res.equalsIgnoreCase("false")){
                criarTabela();
            }
        }
    }

    public byte[] selectImage(long id){
        byte[] count = null;
        final String query = "SELECT t.dicom FROM public.tb_images_dicom t where t.id = " + id + ";";
        try{
            ResultSet result = executeQuery(query);
            while(result.next()){
                count = result.getBytes("dicom");
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return count;
    }

    public String selectNameImage(long id){
        String count = null;
        final String query = "SELECT t.name_file FROM public.tb_images_dicom t where t.id = " + id + ";";
        try{
            ResultSet result = executeQuery(query);
            while(result.next()){
                count = result.getString("name_file");
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return count;
    }

    public void testeBaixarImagemDICOM(){
        try{
            File targetFile = new File("teste-down.dcm");
            OutputStream outStream = new FileOutputStream(targetFile);
            InputStream initialStream = new ByteArrayInputStream(selectImage(300));
            initialStream.transferTo(outStream);
            if(parseDicom(targetFile)){
                System.out.println( ">> DICOM OK;" );
            }else{
                System.out.println( ">> ERROR DICOM;" );
            }
            targetFile.delete();
        }catch(IOException e){}
    }

    private void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            inputStream.transferTo(outputStream);
        }
    }

    public byte[] readBytes(File file) {
        byte[] bytes = null;
        try{
            bytes = Files.readAllBytes(file.toPath());
        }catch(IOException e){}
        return bytes;
    }

    public byte[] getBytesImageByID(long id) {
        byte[] bytes = null;
        try{
            Compress comp = new Compress();
            String nameImage = selectNameImage(id).replace(".dcm", "");
            File targetFile = new File(nameImage + Compress.ext);

            OutputStream outStream = new FileOutputStream(targetFile);
            InputStream initialStream = new ByteArrayInputStream(selectImage(id));
            initialStream.transferTo(outStream);

            /*\/ descompactar no mesmo local que o arquivo; */
            comp.decompress(targetFile.getAbsolutePath(), targetFile.getAbsolutePath().replace(Compress.ext, ".dcm"));

            File dicomFile = new File(nameImage + ".dcm");
            if(dicomFile.exists()){
                if(parseDicom(dicomFile)){
                    // System.out.println( ">> DICOM OK;" );
                    bytes = readBytes(dicomFile);
                }else{
                    // System.out.println( ">> ERROR DICOM;" );
                }
            }
            targetFile.delete();
            dicomFile.delete();
        }catch(IOException e){}
        return bytes;
    }

    private boolean parseDicom(File dicom) {
        boolean ok = false;
        LinkedHashMap<Integer, String[]> attr = null;
        AC_DicomReader dicomReader = new AC_DicomReader();
        dicomReader.readDCMFile(dicom.getAbsolutePath());
        AC_DcmStructure dcmStructure = null;
        try {
            dcmStructure = dicomReader.getAttirbutes();
            if(dcmStructure != null){
                attr = dcmStructure.getAttributes();
                ok = (attr.size() > 0);
            }else{
                /*\/ not dicom(.dcm/.ima) file; */
                ok = false;
            }
        } catch (java.io.IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ok;
    }

    public void createDBAndTable(String ipPorta, String usuario, String senha, String nomeBanco) {
        final JDBCConnection con = new JDBCConnection();
        /*\/ criar banco caso não exista; */
        con.createDB(ipPorta, usuario, senha, nomeBanco);
        con.createConnection(ipPorta, nomeBanco, usuario, senha);
        if(con.seConectado()){
            Statement statement = con.getStatement();

            /*\/ verificar existência da tabela; */
            boolean exists = false;
            try{
                String res = "";
                String query = "SELECT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename  = 'tb_images_dicom');";
                ResultSet result = con.executeQuery(query);
                while(result.next()){
                    res = result.getString("exists");
                }
                if(res.equalsIgnoreCase("f") || res.equalsIgnoreCase("false")){
                    exists = false;
                }else{
                    exists = true;
                }
            }catch(SQLException e){
                e.printStackTrace();
            }

            /*\/ criar tabela caso a mesma não exista; */
            if(!exists){
                criarTabela(statement);
            }
        }
        con.close();
    }

    /*\/ migrar dados da tabela de imagens dicoms ; */
    public boolean migrateTableImagens(final JDBCConnection con) {
        boolean ok = false;
        if(con.seConectado()){
            /*\/ migrar dados; */
            try{
                String query = "SELECT * FROM public.tb_images_dicom;";
                ResultSet result = executeQuery(query);
                while(result.next()){
                    long id = result.getLong("id");
                    byte[] dicom = result.getBytes("dicom");
                    String patient_id = result.getString("patient_id");
                    String patient_name = result.getString("patient_name");
                    String patient_age = result.getString("patient_age");
                    String patient_birth_date = result.getString("patient_birth_date");
                    String patient_sex = result.getString("patient_sex");
                    String institution_name = result.getString("institution_name");
                    String study_date = result.getString("study_date");
                    String study_id = result.getString("study_id");
                    String series_number = result.getString("series_number");
                    String name_file = result.getString("name_file");

                    PreparedStatement ps = con.getConnection().prepareStatement(query_insert);
                    int index = 1;
                    ps.setLong(index, id);
                    index++;
                    InputStream dicomStream = new ByteArrayInputStream(dicom);
                    ps.setBinaryStream(index, dicomStream);
                    index++;
                    ps.setString(index, patient_id);
                    index++;
                    ps.setString(index, patient_name);
                    index++;
                    ps.setString(index, patient_age);
                    index++;
                    ps.setString(index, patient_birth_date);
                    index++;
                    ps.setString(index, patient_sex);
                    index++;
                    ps.setString(index, institution_name);
                    index++;
                    ps.setString(index, study_date);
                    index++;
                    ps.setString(index, study_id);
                    index++;
                    ps.setString(index, series_number);
                    index++;
                    ps.setString(index, name_file);

                    int retorno = ps.executeUpdate();
                    if(retorno > 0){
                        ok = true;
                    }else{
                        ok = false;
                    }
                }
            }catch(SQLException e){
                e.printStackTrace();
            }
        }
        con.close();
        return ok;
    }

    /*\/ criar banco/tabela e migrar dados das imagens para outro servidor; */
    public void createDBAndMigrateTable(String ipPorta, String usuario, String senha, String nomeBanco) {
        createDBAndTable(ipPorta, usuario, senha, nomeBanco);
        final JDBCConnection con = new JDBCConnection();
        con.createConnection(ipPorta, nomeBanco, usuario, senha);
        migrateTableImagens(con);
        con.close();
    }

}