/*
 * 
 */
package deepsea.utilities;

import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * classe para escrita de log no sistema;
 */
public final class LogFile {

    private Logger logger;
    private FileHandler handler;

    public LogFile(){
        try{
            
            File fileLog = java.nio.file.Paths.get("deepsea.log").toFile();
            if(!fileLog.exists()){
                this.handler = new FileHandler("deepsea.log", true);
                this.logger.addHandler(this.handler);
            }else{
                this.logger = Logger.getLogger("deepsea");
            }
            // this.logger.addHandler(this.handler);
        }catch(IOException e){}
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
