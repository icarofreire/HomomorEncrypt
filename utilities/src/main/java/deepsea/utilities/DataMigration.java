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

    private final LocalDateTime stringToLocalDateTime(String data) {
        String formatoTempo = "dd/MM/yyyy HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatoTempo);
        LocalDateTime dateTime = LocalDateTime.parse(data, formatter);
        return dateTime;
    }

    private final LocalDateTime studyDateToLocalDateTime(String studyDate) {
        if(studyDate != null){
            /* ex formato da data: "20221208"; ex: "20221209" */
            String ano = studyDate.substring(0, 4); // ano;
            String mes = studyDate.substring(4, 6); // mes;
            String dia = studyDate.substring(6, 8); // dia;
            String data = dia + "/" + mes + "/" + ano + " 00:00:00";
            LocalDateTime dataEstudoDicom = stringToLocalDateTime(data);
            return dataEstudoDicom;
        }
        return null;
    }

    /* obter data/hora atual; */
    private LocalDateTime dataHoraAtualLocalDateTime() {
        LocalDateTime localDateTime = LocalDateTime.now();
        return localDateTime;
    }

    /*\/ verificar se data de estudo é posterior a N meses; */
    private final boolean ifStudyDateIsOld(String studyDate) {
        LocalDateTime studyDateTime = studyDateToLocalDateTime(studyDate);
        LocalDateTime atualDateTime = dataHoraAtualLocalDateTime();
        Map<String, Long> periodo = pediodoEntreDuasDatas_Map(studyDateTime, atualDateTime);
        return (periodo.get("M") >= meses);
    }

    /*\/ retorna o período de diferença entre duas datas;
     * ano, meses, dias, horas, minutos e segundos;
     */
    private Map<String, Long> pediodoEntreDuasDatas_Map(LocalDateTime dataA, LocalDateTime dataB) {
        LocalDateTime tempDateTime = LocalDateTime.from(dataA);

        long years = tempDateTime.until(dataB, ChronoUnit.YEARS);
        tempDateTime = tempDateTime.plusYears(years);

        long months = tempDateTime.until(dataB, ChronoUnit.MONTHS);
        tempDateTime = tempDateTime.plusMonths(months);

        long days = tempDateTime.until(dataB, ChronoUnit.DAYS);
        tempDateTime = tempDateTime.plusDays(days);

        // ***

        long hours = tempDateTime.until(dataB, ChronoUnit.HOURS);
        tempDateTime = tempDateTime.plusHours(hours);

        long minutes = tempDateTime.until(dataB, ChronoUnit.MINUTES);
        tempDateTime = tempDateTime.plusMinutes(minutes);

        long seconds = tempDateTime.until(dataB, ChronoUnit.SECONDS);

        Map<String, Long> tempo = new ConcurrentHashMap<>();

        tempo.put("A", ((years > 0) ? (years) : (0)) );
        tempo.put("M", ((months > 0) ? (months) : (0)) );
        tempo.put("D", ((days > 0) ? (days) : (0)) );
        tempo.put("H", ((hours > 0) ? (hours) : (0)) );
        tempo.put("Min", ((minutes > 0) ? (minutes) : (0)) );

        return tempo;
    }

    /*\/ migrar dados da tabela de imagens dicoms com datas de estudo antigas; */
    public boolean migrateOlderImagens(final JDBCConnection conDB1, final JDBCConnection conDB2) {
        boolean ok = false;
        if(conDB1.seConectado() && conDB2.seConectado()){
            Vector<Object[]> dates = new Vector<Object[]>();
            /*\/ migrar dados; */
            try{
                String query = "SELECT * FROM public.tb_images_dicom;";
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

            try{
                for(Object[] date : dates){
                    /*\/ mover dados da tabela de imagens dicoms, cuja data de estudo de um dicom 
                    seja de N meses de realização anterior a data atual;*/
                    String studyDate = (String)date[8];
                    if(ifStudyDateIsOld(studyDate)){

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
                }
            }catch(SQLException e){
                e.printStackTrace();
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

    /*\/ migrar dados da tabela de imagens dicoms ; */
    public boolean migrateTableImagens(final JDBCConnection conDB1, final JDBCConnection conDB2) {
        boolean ok = false;
        if(conDB1.seConectado()){
            /*\/ migrar dados; */
            try{
                String query = "SELECT * FROM public.tb_images_dicom;";
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

                    PreparedStatement ps = conDB2.getConnection().prepareStatement(query_insert);
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
        conDB1.close();
        conDB2.close();
        return ok;
    }

}
