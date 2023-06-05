/*
 * 
 */
package deepsea.utilities;

import deepsea.utilities.Server;

import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;
import java.util.Vector;
import java.util.Map;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.google.gson.Gson;

/**
 * Ler arquivo externo de configuração para execução do;
 */
public final class ReadFileConf {

    /*\/ nome do arquivo de configuração de inicialização do projeto; */
    private final String fileconf = "deepsea.json";

    private class JsonDTO {
        private Vector<Map<String, String>> servers;

        public Vector<Map<String, String>> getServers(){
            return servers;
        }
    }

	public Vector<Server> readJsonConf() {
        Vector<Server> servers = new Vector<Server>();
        File fjson = new File(fileconf);
        if(fjson.exists()){
            try{
                Reader reader = Files.newBufferedReader(Paths.get(fileconf));
                Gson gson = new Gson();

                JsonDTO jsonDTO = gson.fromJson(reader, JsonDTO.class);
                Vector<Map<String, String>> vservers = jsonDTO.getServers();

                if(vservers != null && vservers.size() > 0){
                    for(Map<String, String> map : vservers){

                        String host = (map.containsKey("host")) ? (map.get("host")) : ("");
                        String user = (map.containsKey("user")) ? (map.get("user")) : ("");
                        String pass = (map.containsKey("password")) ? (map.get("password")) : ("");
                        String fold = (map.containsKey("folder")) ? (map.get("folder")) : ("");

						/*\/ atribuir servidores pacs; */
						servers.add(new Server(host, user, pass, fold));
                    }
                }else{
                    System.out.println( "Erro na leitura de array de servidores no arquivo '"+ fileconf +"';" );
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }else{
            System.out.println("Arquivo de configuração '"+ fileconf +"' não encontrado.");
            System.out.println("Crie um arquivo '"+ fileconf +"' no seguinte formato, inserindo os dados de acesso aos servidores por SSH: \n{");
            System.out.println("\t\"servers\":");
            System.out.println("\t[");
            System.out.println("\t\t{ \"host\": \"172.23.12.13\", \"user\": \"usuario\", \"password\": \"senha\", \"folder\": \"/home/pasta-arquivos-pacs\" },");
            System.out.println("\t\t{ \"host\": \"172.23.12.14\", \"user\": \"usuario\", \"password\": \"senha\", \"folder\": \"/home/pasta-arquivos-pacs\" },");
            System.out.println("\t\t{ \"host\": \"172.23.12.15\", \"user\": \"usuario\", \"password\": \"senha\", \"folder\": \"/home/pasta-arquivos-pacs\" }");
            System.out.println("\t]");
            System.out.println("}");
        }
        return servers;
	}

}