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
        this.excluirArquivosLogsSimilares();
    }

    public LogFile(String nomeArquivoLog){
        this.arquivoLog = nomeArquivoLog + ".log";
        this.nomeLog = nomeArquivoLog;
        this.criarObterLog();
        this.excluirArquivosLogsSimilares();
    }

    private void criarObterLog(){
        try{
            File fileLog = java.nio.file.Paths.get(arquivoLog).toFile();
            this.handler = new FileHandler(arquivoLog, true);
            this.handler.setFormatter(new SimpleFormatter());
            this.logger = Logger.getLogger(nomeLog);
            this.logger.addHandler(this.handler);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * \/ excluir arquivos de logs similares gerados pelo paralelismo:
     * Ex: teste.log, teste.log.1 ... teste.log.2;
     */
    private void excluirArquivosLogsSimilares(){
        File baseDir = new File(".");
        File[] arqsBaixados = baseDir.listFiles();
        for(File f: arqsBaixados){
            if(f.getName().indexOf(arquivoLog) == 0 && !arquivoLog.equals(f.getName())){
                f.delete();
            }
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
