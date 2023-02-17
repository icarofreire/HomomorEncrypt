/*
 * 
 */
package deepsea.utilities;

import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;

/**
 * classe para escrita de log no sistema;
 */
public final class LogFile {

    private Logger logger;
    private FileHandler handler;
    private String arquivoLog = "deepsea.log";
    private String nomeLog = "deepsea";

    public LogFile(){
        this.criarObterLog();
    }

    public LogFile(String nomeArquivoLog){
        this.arquivoLog = nomeArquivoLog + ".log";
        this.nomeLog = nomeArquivoLog;
        this.criarObterLog();
    }

    private void criarObterLog(){
        try{
            File fileLog = java.nio.file.Paths.get(arquivoLog).toFile();
            if(!fileLog.exists()){
                this.handler = new FileHandler(arquivoLog, true);
                this.handler.setFormatter(new SimpleFormatter());
                this.logger = Logger.getLogger(nomeLog);
                this.logger.addHandler(this.handler);
            }else{
                this.logger = Logger.getLogger(nomeLog);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /* \/ log de "mensagem de informação"; */
    public void info(String message){
        this.logger.info(message);
    }

    /* \/ log de "mensagem de aviso"; */
    public void warning(String message){
        this.logger.warning(message);
    }

    /* \/ log de "mensagem grave"; */
    public void severe(String message){
        this.logger.severe(message);
    }
}
