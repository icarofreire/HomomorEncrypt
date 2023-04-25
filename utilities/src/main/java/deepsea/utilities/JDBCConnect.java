/*
 * 
 */
package deepsea.utilities;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
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

import deepsea.utilities.FileOperationsMinio;
import deepsea.utilities.Compress;

/**
 * 
 */
public final class JDBCConnect {
    private Connection connection = null;
    private Statement stmt = null;

    /*\/ informações banco; */
    // private final String ipPorta = "172.25.190.10:5432";
    // private final String banco = "images_dicom";
    // private final String usuario = "postgres";
    // private final String senha = "PpSes2020!2019ProdPass";
    
    private final String ipPorta = "172.25.190.10:5432";
    private final String banco = "compact_dicoms";
    private final String usuario = "postgres";
    private final String senha = "PpSes2020!2019ProdPass";

    // private final String ipPorta = "localhost:5432";
    // private final String banco = "galen";
    // private final String usuario = "postgres";
    // private final String senha = "postgres";


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
        "name_file" +
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
        "name_file character varying(255) NOT NULL \n" +
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

    // /* \/ escrever objeto por inteiro no MinIO; */
    public void transferImagesToMinio(){
        try{
            FileOperationsMinio minio = new FileOperationsMinio("icaroteste");
            if(!minio.getErrorConnection()){
                long totalObjects = minio.totalObjects();
                long totalRegistrosBanco = numeroRegistros();
                if(totalRegistrosBanco > totalObjects){
                    for(long id = totalObjects; id <= totalRegistrosBanco; id++)
                    {
                        long idImage = id;
                        String nomeArquivoImagem = selectNameImage(idImage);
                        if(nomeArquivoImagem != null){
                            File file = new File(nomeArquivoImagem);
                            OutputStream outStream = new FileOutputStream(file);
                            InputStream initialStream = new ByteArrayInputStream(selectImage(idImage));
                            initialStream.transferTo(outStream);

                            if(!minio.ifObjectExists(file.getName())){
                                if(minio.uploadObject(file.getName(), file.getAbsolutePath())){
                                    // System.out.println( ">> UP Ok;" );
                                }
                            }else{
                                // System.out.println( file.getName() + " já existe no minio;" );
                            }
                            file.delete();
                        }
                    }
                }
            }
            // System.out.println( ">> MinIO Total: " + minio.totalObjects() );
        }catch(IOException e){}
    }

    private String nameFileWithoutExtension(String nameFile) {
        return nameFile.substring(0, nameFile.lastIndexOf("."));
    }

    public void transferImagesCompactToMinio() {
        try{
            Compress comp = new Compress();
            FileOperationsMinio minio = new FileOperationsMinio("comp-dicoms");
            if(!minio.getErrorConnection()){
                long totalObjects = minio.totalObjects();
                long totalRegistrosBanco = numeroRegistros();
                if(totalRegistrosBanco > totalObjects){
                    for(long id = totalObjects; id <= totalRegistrosBanco; id++)
                    {
                        long idImage = id;
                        String nomeArquivoImagem = selectNameImage(idImage);
                        if(nomeArquivoImagem != null){
                            File file = new File(nomeArquivoImagem);
                            OutputStream outStream = new FileOutputStream(file);
                            InputStream initialStream = new ByteArrayInputStream(selectImage(idImage));
                            initialStream.transferTo(outStream);

                            String pathFileCompactedName = nameFileWithoutExtension(file.getName()) + Compress.ext;
                            File fileDicomComp = new File(pathFileCompactedName);
                            if(comp.compress(file.getAbsolutePath(), pathFileCompactedName)){
                                /*\/ *** */
                                if(!minio.ifObjectExists(fileDicomComp.getName())){
                                    if(minio.uploadObject(fileDicomComp.getName(), fileDicomComp.getAbsolutePath())){
                                        // System.out.println( ">> UP Ok;" );
                                    }
                                }else{
                                    // System.out.println( file.getName() + " já existe no minio;" );
                                }
                            }
                            fileDicomComp.delete();
                            file.delete();
                        }
                    }
                }
            }
            // System.out.println( ">> MinIO Total: " + minio.totalObjects() );
        }catch(IOException e){}
    }

}