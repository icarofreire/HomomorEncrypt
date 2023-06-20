/*
 * 
 */
package deepsea.utilities;

import java.io.File;
import java.io.FileWriter;
import java.util.Vector;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.io.IOException;

/**
 * classe para escrita de log de arquivos inválidos
 * encontrados na busca;
 */
public final class LogEvitFile {

    /*\/ log de arquivos inválidos sobre cada host; */
    private final String logFilesInv = "invalidFiles";
    /*\/ vetor de nomes de arquivos e host's a serem evitados na busca; */
    private final Vector<String> invalidFilesDicom = new Vector<String>();
    private String host;

    public void setHost(String host){
        this.host = host;
    }

    public String getHost(){
        return this.host;
    }

    /*\/ criar arquivo de log de nomes de arquivos inálidos a serem obtidos no servidor; */
    public void createLogInvFiles(String fileDescription) {
        File logCheck = new File(logFilesInv);
        boolean append = logCheck.exists();
        try {
            FileWriter myWriter = new FileWriter(logFilesInv, append);
            myWriter.write(fileDescription + "\n");
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*\/ ler log de arquivos inválidos a serem evitados nas próximas buscas;  */
    public void readInvFiles() {
        File logCheck = new File(logFilesInv);
        if(logCheck.exists()){
            try {
                Scanner scanner = new Scanner(new File(logFilesInv));
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    invalidFilesDicom.add(line);
                }
                scanner.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean seContemArquivoInv(String fileName) {
        /*\/ se arquivo contém na lista de arquivos a serem evitados; */
        Vector<String> filesEvit = invalidFilesDicom.stream().filter(f -> {
            String nameInv = f.substring(0, f.indexOf(":")).trim();
            String nameHost = f.substring(f.indexOf(":")+1, f.length()).trim();
            return ( nameInv.equals(fileName) && nameHost.equals(host) );
        }).collect(Collectors.toCollection(Vector::new));
        return (filesEvit.size() > 0);
    }

}