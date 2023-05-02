/*
 * ;
 */
package deepsea.utilities;

import deepsea.utilities.BuscasDicom;
import deepsea.utilities.JDBCConnect;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ;
 */
public final class Scheduler {

    private class Task extends TimerTask {

        private final void executarBuscasDicom() {
            iniciarBuscas();
        }

        public void run() {
            /* ... */
            executarBuscasDicom();
        }

        public long minutesToMilliseconds(long minutes){
            return java.time.Duration.ofMinutes(minutes).toMillis();
        }

        public long secondsToMilliseconds(long seconds){
            return java.time.Duration.ofSeconds(seconds).toMillis();
        }
    }

    /*\/ imagens que são registradas no banco de dados,
    também são transferidas ao servidor do MinIO; */
    private final void iniciarBuscas() {
        try{
            final BuscasDicom busca = new BuscasDicom("172.23.12.15", "root", "ZtO!@#762");
            busca.getDiffLogAndServer("/home/storage-pacs");
        }catch(com.jcraft.jsch.SftpException | com.jcraft.jsch.JSchException e){
            e.printStackTrace();
        }

        // final JDBCConnect banco = new JDBCConnect();
        /*\/ transferir imagens para o servidor MinIO; */
        // banco.transferImagesCompactToMinio();
        // banco.close();
    }

    public void ini() {
        TimeExecution.inicio();
        iniciarBuscas();
        TimeExecution.fim();

        long tempoMedioMillis = TimeExecution.getTimeDurationMilli();

        Timer timer = new Timer();
        Task task = new Task();
        TimerTask timerTask = task;
        /*\/ execução repetida; */
        // timer.schedule(timerTask, 0L, tempoMedioMillis + task.secondsToMilliseconds(5));
        timer.schedule(timerTask, 0L, tempoMedioMillis + task.minutesToMilliseconds(5L));
    }

}