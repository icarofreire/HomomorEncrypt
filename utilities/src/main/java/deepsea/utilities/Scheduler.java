/*
 * ;
 */
package deepsea.utilities;

import deepsea.utilities.BuscasDicom;
import deepsea.utilities.DBOperations;
import deepsea.utilities.DataMigration;
import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ;
 */
public final class Scheduler {

    private Vector<Server> servers;

    /**
	* Sets new value of servers
	* @param
	*/
	public void setServers(Vector<Server> servers) {
		this.servers = servers;
	}

    private class Task extends TimerTask {

        private final void executarBuscasDicom() {
            iniciarBuscasParallel();
            migrate();
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

    /*\/ iniciar buscas no servidor de forma paralela; */
    private final void iniciarBuscasParallel() {
        servers.parallelStream().forEach(server -> {
            try{
                final BuscasDicom busca = new BuscasDicom(server.getHost(), server.getUsername(), server.getPassword());
                busca.scanServer(server.getFolderBase());
            }catch(com.jcraft.jsch.SftpException | com.jcraft.jsch.JSchException e){
                e.printStackTrace();
            }
        });
    }

    /*\/ migrar dados; */
    private final void migrate() {
        final DataMigration mig = new DataMigration();
        mig.migrate();
    }

    public void iniParallel() {
        final DBOperations banco = new DBOperations();
        if(banco.seConectado()){
            Timer timer = new Timer();
            Task task = new Task();
            TimerTask timerTask = task;
            /*\/ execução repetida; */
            timer.schedule(timerTask, 0L, task.minutesToMilliseconds(5L));
        }
        banco.close();
    }

}