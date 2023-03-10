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
        // try{
        //     final BuscasDicom busca = new BuscasDicom("187.17.3.12", "a_fhs", "#fhs2018#");
        //     busca.getDiffLogAndServer("/home/a_fhs");
        // }catch(com.jcraft.jsch.SftpException | com.jcraft.jsch.JSchException e){
        //     e.printStackTrace();
        // }
        System.out.println("Happy Birthday John Doe");
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