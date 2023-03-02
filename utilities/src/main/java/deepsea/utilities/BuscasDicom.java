/*
 * Buscas de arquivos DICOM;
 */
package deepsea.utilities;

import com.jcraft.jsch.*;
import java.util.Properties;
import java.util.Vector;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.io.File;
import java.nio.file.Files;
import java.io.FileWriter;
import java.io.IOException;
import deepsea.utilities.LogFile;
import deepsea.utilities.SftpClient;
import deepsea.utilities.ZipUtility;
import deepsea.utilities.ChecksumFile;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Buscas de arquivos DICOM;
 */
public final class BuscasDicom extends SftpClient {
    /*\/ profundidade de pastas da recursividade; */
    private final long maxProfund = 50;//500;
    /*\/ tipos de arquivos a procurar nas buscas em pasta e subpastas; */
    private final List<String> filesTypes = Arrays.asList(".dcm", ".ima", ".css"); // << ".css" para fins de testes;
    /*\/ pasta base onde a busca é iniciada; */
    private String pastaBase = null;
    /*\/ profundidade de subpastas em uma pasta encontrada na busca; */
    private final int maxSubpastas = 7;
    /*\/ pastas que possuem profundo grau de subpastas encontradas na busca; */
    private final List<String> pastasEvit = new ArrayList<String>();
    private final List<String> pastasVisi = new ArrayList<String>();
    /*\/ ; */
    private final String fileLogChecksum = "files-env.txt";
    /*\/ nome do arquivo .zip para compactar os arquivos baixados do servidor; */
    private final String nameFileCompactZip = "DownDicoms";
    /*\/ caminho completo dos arquivos dicoms encontrados no servidor; */
    private final List<String> filesDicom = new ArrayList<String>();

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
        System.out.printf("Listing [%s]...%n", remoteDir);
        if(this.pastaBase == null){
            this.pastaBase = remoteDir;
        }
        if(!this.pastasVisi.contains(remoteDir) && this.pastasVisi.size() < this.maxProfund){
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
                            // int quan = quantDicoms(subPasta);
                            if(sub != null){
                                // if(quan > 0){
                                // }
                                String pastaRet = this.chequeDeepVoltaBaseMax(subPasta);
                                if(pastaRet != null) subPasta = pastaRet;
                                freeWalk(subPasta);
                            }
                        }
                    }else if(this.seDICOM(name)){
                        String pathFile = remoteDir + java.io.File.separator + name;
                        this.filesDicom.add(pathFile);
                    }
                }
            }
        }
    }

    private List<String> lerLogEnvFiles(){
        final List<String> lcheks = new ArrayList<>();
        // pass the path to the file as a parameter
        File file = new File(this.fileLogChecksum);
        if(file.exists()){
            try{
                Scanner sc = new Scanner(file);
                while (sc.hasNextLine()){
                    String linha = sc.nextLine();
                    String ped[] = linha.split("\\|");
                    if(ped.length > 0){
                        lcheks.add(ped[0]);
                        // System.out.println(ped[0] + " -> " + ped[1] + " -> " + ped[2]);
                    }
                }
            }catch(java.io.FileNotFoundException e){}
        }
        return lcheks;
    }

    /*\/ auxiliar no retorno da diferença de duas listas de arvores de diretórios; */
    private <T> List<T> diffTwoLists(List<T>listOne, List<T>listTwo) {
        List<T> differences = listOne.stream()
            .filter(element -> !listTwo.contains(element))
            .collect(Collectors.toList());
        return differences;
    }

    private void delDir(Path dir) {
        try{
            Files.walk(dir) // Traverse the file tree in depth-first order
            .sorted(java.util.Comparator.reverseOrder())
            .forEach(path -> {
                try {
                    Files.delete(path);  //delete each file or directory
                } catch (IOException e) { e.printStackTrace(); }
            });
        }catch(IOException e){}
    }

    /*\/
    * download dos arquivos dicoms do servidor;
    * criar log de checksum dos arquivos;
    * compactar arquivos dicoms;
    * */
    private void downDicomsECompact(final List<String> caminhoDicomsDown) {
        if(caminhoDicomsDown.size() > 0){
            File dirBase = new File(this.nameFileCompactZip);
            if(!dirBase.exists()){
                dirBase.mkdir();
            }
            int con = 0;//<< fins de testes;
            boolean error = false;
            for(String pathFile : caminhoDicomsDown){
                try{
                    if(con<5){//<< fins de testes;
                        downloadFile(pathFile, dirBase.getAbsolutePath());
                        con++;//<< fins de testes;
                    }
                }catch(SftpException e){
                    error = true;
                    e.printStackTrace();
                }
            }
            if(!error){
                boolean errorZip = false;
                final ZipUtility zip = new ZipUtility();
                try{
                    zip.zipFiles(dirBase.listFiles(), this.nameFileCompactZip + ".zip");
                    /*\/ criar log de arquivos baixados do servidor;
                    * OBS: analisar se o log deve ser criado após enviar os arquivos
                    para algum servidor, ou anteriormente; */
                    this.createLogFilesEnv();
                }catch(java.io.IOException ex){
                    errorZip = true;
                    ex.printStackTrace();
                }
                if(!errorZip){
                    /*\/ deletar pasta com arquivos dicoms baixados do servidor; */
                    this.delDir(dirBase.toPath());
                }
            }
        }
    }

    /*\/ criar log de arquivos baixados do servidor; */
    private void createLogFilesEnv() {
        if(this.filesDicom.size() > 0){
            /*\/ date now(); */
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String formattedDateTime = LocalDateTime.now().format(formatter);
            File logCheck = new File(this.fileLogChecksum);
            boolean append = logCheck.exists();
            try {
                FileWriter myWriter = new FileWriter(this.fileLogChecksum, append);
                for(String file : this.filesDicom){
                    myWriter.write(file + "|" + formattedDateTime + ";\n");
                }
                myWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* \/
    * */
    public void getDiffLogAndServer(String remoteDir) throws SftpException, JSchException {
        this.freeWalk(remoteDir);

        if(this.filesDicom.size() > 0){
            List<String> lEnvFiles = this.lerLogEnvFiles();
            if(lEnvFiles.size() > 0){
                /*\/ arquivos diferentes que não foram baixados do servidor; */
                List<String> diffFilesEnv = this.diffTwoLists(this.filesDicom, lEnvFiles);
                if(diffFilesEnv.size() > 0) this.downDicomsECompact(diffFilesEnv);
            }else{
                this.downDicomsECompact(this.filesDicom);
            }
        }

        this.close();
        this.sendZipToServer();
    }

    /*\/ enviar arquivos para o servidor; */
    private void sendZipToServer() {
        File fileZip = new File(this.nameFileCompactZip + ".zip");
        if(fileZip.exists()){
            /*... enviar servidor; */


            /* deletar arquivo zip após enviar ao servidor; */
            // fileZip.delete();
        }
    }

    public void close() {
        super.close();
    }
}