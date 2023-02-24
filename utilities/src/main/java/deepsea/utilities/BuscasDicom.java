/*
 * Buscas de arquivos DICOM;
 */
package deepsea.utilities;

import com.jcraft.jsch.*;
import java.util.Properties;
import java.util.Vector;
import java.util.List;
import java.io.File;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.stream.Collectors;
import deepsea.utilities.LogFile;
import java.io.FileWriter;
import java.util.Scanner;
import java.io.IOException;
import deepsea.utilities.SftpClient;

/**
 * Buscas de arquivos DICOM;
 */
public final class BuscasDicom extends SftpClient {
    /*\/ profundidade de pastas da recursividade; */
    private final long maxProfund = 500;
    /*\/ tipos de arquivos a procurar nas buscas em pasta e subpastas; */
    private final List<String> filesTypes = Arrays.asList(".dcm", ".ima", ".css");
    /*\/ pasta base onde a busca é iniciada; */
    private String pastaBase = null;
    /*\/ profundidade de subpastas em uma pasta encontrada na busca; */
    private final int maxSubpastas = 7;
    /*\/ pasta que possui os arquivos procurados; */
    private final List<String> pastasFiles = new ArrayList<String>();
    private final HashMap<String, Integer> pastasQuantDicom = new HashMap<String, Integer>();
    /*\/ pastas que possuem profundo grau de subpastas encontradas na busca; */
    private final List<String> pastasEvit = new ArrayList<String>();
    private final List<String> pastasVisi = new ArrayList<String>();

    public BuscasDicom(String host, String username, String password) throws JSchException {
        super(host, username, password);
    }

    /*\/ ; */
    private String chequeDeepVoltaBaseMax(String caminho) {
        String cur = null;
        String[] blocos = caminho.split(java.io.File.separator);
        if(blocos.length > maxSubpastas){
            String pastaProib = getPastaEvitar(this.pastaBase, caminho);
            if(pastaProib != null){
                pastasEvit.add(pastaProib);
                cur = this.pastaBase; // << retorno a base;
            }
        }
        return cur;
    }

    private boolean seCaminhoEvitar(String caminho) {
        List<String> fil = pastasEvit.stream().filter(f -> (f.indexOf(caminho) != -1)).collect(Collectors.toList());
        return (fil.size() > 0);
    }

    private String getPastaEvitar(String pastaBase, String caminho) {
        int ind = caminho.indexOf(pastaBase);
        if(ind != -1){
            return caminho.substring(0, caminho.indexOf(java.io.File.separator, pastaBase.length()+1));
        }
        return null;
    }

    public void gerarLogTreeFiles(){
        if(this.pastasFiles.size() > 0){
            try {
                FileWriter myWriter = new FileWriter("tree.txt");
                for(String p: this.pastasFiles){
                    myWriter.write(p + ":" + this.pastasQuantDicom.get(p) +"\n");
                }
                myWriter.close();
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }
    }

    /*\/ se arquivo é do tipo DICOM; */
    private boolean seDICOM(String arq){
        int lastp = arq.lastIndexOf('.');
        if(lastp != -1){
            String ext = arq.substring(lastp, arq.length()).toLowerCase();
            return (filesTypes.contains(ext));
        }else{
            return false;
        }
    }

    private int quantDicoms(String remoteDir){
        try{
            final Vector<ChannelSftp.LsEntry> files = getChannel().ls(remoteDir);
            if(!files.isEmpty()){
                Vector<ChannelSftp.LsEntry> filt = files
                .stream()
                .filter(f -> this.seDICOM(f.getFilename()) )
                .collect(java.util.stream.Collectors.toCollection(Vector::new));
                return filt.size();
            }
        }catch(SftpException e ){}
        return 0;
    }

    @SuppressWarnings("unchecked")
    public void freeWalk(String remoteDir) throws SftpException, JSchException {
        if (getChannel() == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        System.out.printf("Listing [%s] -> [%d]...%n", remoteDir, this.pastasFiles.size());
        if(this.pastaBase == null){
            this.pastaBase = remoteDir;
        }
        if(!this.pastasVisi.contains(remoteDir) && this.pastasVisi.size() < this.maxProfund){
            // channel.cd(remoteDir);
            cd(remoteDir);
            Vector<ChannelSftp.LsEntry> files = vetorConteudos(remoteDir);
            if(files != null && !files.isEmpty()){
                for (ChannelSftp.LsEntry file : files) {
                    var name        = file.getFilename();
                    var attrs       = file.getAttrs();

                    /*\/ entrar em subpastas; */
                    if((!name.equals(".") && !name.equals("..")) && attrs.isDir()){
                        if(!this.seCaminhoEvitar(name)){
                            this.pastasVisi.add(remoteDir);
                            String subPasta = remoteDir + java.io.File.separator + name;
                            Vector<ChannelSftp.LsEntry> sub = vetorConteudos(subPasta);
                            int quan = quantDicoms(subPasta);
                            if(sub != null){
                                if(quan > 0){
                                    this.pastasFiles.add(subPasta);
                                    this.pastasQuantDicom.put(subPasta, quan);
                                }
                                String pastaRet = this.chequeDeepVoltaBaseMax(subPasta);
                                if(pastaRet != null) subPasta = pastaRet;
                                freeWalk(subPasta);
                            }
                        }
                    }
                }
            }
        }
    }

    private HashMap<String, Integer> lerLogTree(){
        final HashMap<String, Integer> pastasQuan = new HashMap<String, Integer>();
        // pass the path to the file as a parameter
        File file = new File("tree.txt");
        if(file.exists()){
            try{
                Scanner sc = new Scanner(file);
                while (sc.hasNextLine()){
                    String linha = sc.nextLine();
                    int meio = linha.indexOf(":");
                    if(meio != -1){
                        String caminho = linha.substring(0, meio);
                        String quantid = linha.substring(meio+1, linha.length());
                        // System.out.println(caminho + "->" + quantid);
                        pastasQuan.put(caminho, Integer.parseInt(quantid, 10));
                    }
                    // System.out.println(linha);
                }
            }catch(java.io.FileNotFoundException e){}
        }
        return pastasQuan;
    }

    /*\/ auxiliar no retorno da diferença de duas listas de arvores de diretórios; */
    private <T> List<T> diffTwoLists(List<T>listOne, List<T>listTwo) {
        List<T> differences = listOne.stream()
            .filter(element -> !listTwo.contains(element))
            .collect(Collectors.toList());
        return differences;
    }

    /* TODO;; OBS;; melhorar/continuar...
    * ler arquivo de log das pastas; 
    * fazer uma nova leitura dos diretórios;
    * comparar as listas de pastas para obter diferenças de novos diretórios criados;
    * fazer novas leituras de arquivos dicoms a compactar e enviar a outros servidores;
    * */
    private void lerLogTree(String remoteDir) throws SftpException, JSchException {
        this.freeWalk(remoteDir);
        HashMap<String, Integer> pastasQuan = this.lerLogTree();
        List<String> pastas = pastasQuan.keySet().stream().collect(Collectors.toList());

        List<String> pastasDiff = this.diffTwoLists(pastasFiles, pastas);
        if(pastasDiff.size() > 0){
            /* ... buscas nas pastas diferenciadas com novos arquivos dicoms; */
        }
    }

    public void close() {
        super.close();
    }
}