package deepsea.utilities;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import deepsea.utilities.JDBCConnection;
import deepsea.utilities.DBOperations;

/*\/
* 
*/
public final class DataMigration {

    /*\/ número de meses a serem analizados para comparar as datas
    de estudo dos dicoms sobre a data atual; */
    private final long meses = 3L;

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


    public void migrate() {
        final DBOperations banco = new DBOperations();

        /*\/ conexão do banco principal; */
        JDBCConnection conPri = new JDBCConnection();
        conPri.createConnection("172.25.190.10:5432", "compact_dicoms", "postgres", "PpSes2020!2019ProdPass");

        /*\/ conexão do banco secundário; */
        JDBCConnection conSec = new JDBCConnection();
        banco.createDBAndTable("172.25.190.12:5432", "postgres", "PpSes2020!2019ProdPass", "compact_old_dicoms");
        banco.close();
        conSec.createConnection("172.25.190.12:5432", "compact_old_dicoms", "postgres", "PpSes2020!2019ProdPass");

        migrateOlderImagens(conPri, conSec);
    }

    /* obter data/hora atual; */
    private LocalDateTime dataHoraAtualLocalDateTime() {
        LocalDateTime localDateTime = LocalDateTime.now();
        return localDateTime;
    }

    private LocalDateTime minusMonts(LocalDateTime data) {
        return data.minusMonths(meses);
    }

    /*\/ data formatada da data atual decrescida de alguns meses; */
    private String dataMesesPassados() {
        LocalDateTime dataAnt = minusMonts(dataHoraAtualLocalDateTime());
        /* ex formato da data: "20221208"; ex: "20221209" */
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String formatDateTime = dataAnt.format(formatter);
        return formatDateTime;
    }

    /*\/ migrar dados da tabela de imagens dicoms com datas de estudo antigas; */
    /*\/ mover dados da tabela de imagens dicoms, cuja data de estudo de um dicom 
    seja de N meses de realização anterior a data atual;*/
    public boolean migrateOlderImagens(final JDBCConnection conDB1, final JDBCConnection conDB2) {
        boolean ok = false;
        if(conDB1.seConectado() && conDB2.seConectado()){
            Vector<Object[]> dates = new Vector<Object[]>();
            /*\/ migrar dados antigos; */
            try{
                String dataAnt = dataMesesPassados();
                String query = "SELECT * FROM public.tb_images_dicom WHERE study_date::timestamp <= '" + dataAnt + "'::timestamp;";
                ResultSet result = conDB1.executeQuery(query);
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

                    dates.add( new Object[]{id, dicom, patient_id, patient_name, patient_age, patient_birth_date, patient_sex, institution_name, study_date, study_id, series_number, name_file} );
                }
            }catch(SQLException e){
                e.printStackTrace();
            }
            if(dates.size() > 0){
                try{
                    for(Object[] date : dates){
                        long count = 0;
                        /*\/ verificar se imagem existe no outro banco; */
                        final String query_consulting = "SELECT count(*) AS count FROM public.tb_images_dicom t WHERE t.name_file = '" + (String)date[11] + "';";
                        try{
                            ResultSet resultConsult = conDB2.executeQuery(query_consulting);
                            while(resultConsult.next()){
                                count = resultConsult.getLong("count");
                            }
                        }catch(SQLException e){
                            e.printStackTrace();
                        }

                        if(count == 0){
                            PreparedStatement ps = conDB2.getConnection().prepareStatement(query_insert);
                            int index = 1;
                            ps.setLong(index, (long)date[0]);
                            index++;

                            byte[] dicom = (byte[])date[1];
                            InputStream dicomStream = new ByteArrayInputStream(dicom);
                            ps.setBinaryStream(index, dicomStream);
                            index++;
                            for(int i=2; i<date.length; i++){
                                ps.setString(index, (String)date[i]);
                                index++;
                            }

                            int retorno = ps.executeUpdate();
                            if(retorno > 0){
                                ok = true;
                                /*\/ remover linha do banco primeiro; */
                                deleteRow(conDB1, (long)date[0]);
                            }else{
                                ok = false;
                            }
                        }
                    }
                }catch(SQLException e){
                    e.printStackTrace();
                }
            }
        }
        conDB1.close();
        conDB2.close();
        return ok;
    }

    private boolean deleteRow(final JDBCConnection con, long id) {
        boolean ok = false;
        String query = "DELETE FROM public.tb_images_dicom WHERE id = " + id;
        if(con.seConectado()){
            try{
                int ret = con.getStatement().executeUpdate(query);
                if(ret > 0){
                    ok = true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return ok;
    }

}
