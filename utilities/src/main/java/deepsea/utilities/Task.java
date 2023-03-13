/*
 * ;
 */
package deepsea.utilities;

import deepsea.utilities.BuscasDicom;
import java.util.TimerTask;

/**
 * ;
 */
public final class Task extends TimerTask {

    private final void executarBuscasDicom() {
        try{
            final BuscasDicom busca = new BuscasDicom("172.23.12.15", "root", "ZtO!@#762");
            busca.getDiffLogAndServer("/home/storage-pacs");
        }catch(com.jcraft.jsch.SftpException | com.jcraft.jsch.JSchException e){
            e.printStackTrace();
        }
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