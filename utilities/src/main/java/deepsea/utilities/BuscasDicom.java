/*
 * Buscas de arquivos DICOM;
 */
package deepsea.utilities;

import com.jcraft.jsch.*;
import java.util.Properties;
import java.util.Vector;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.io.File;
import java.nio.file.Files;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import deepsea.utilities.LogFile;
import deepsea.utilities.SftpClient;
import deepsea.utilities.ZipUtility;
import deepsea.utilities.ChecksumFile;
import deepsea.utilities.JDBCConnect;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*\/ parse dicom; */
import AC_DicomIO.AC_DcmStructure;
import AC_DicomIO.AC_DicomReader;

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
    private final int maxSubpastas = 10;//7;
    /*\/ pastas que possuem profundo grau de subpastas encontradas na busca; */
    private final List<String> pastasEvit = new ArrayList<String>();
    private final List<String> pastasVisi = new ArrayList<String>();
    /*\/ arquvo de log de arquivos baixados do servidor; */
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
                connectAndSendFiles(dirBase, caminhoDicomsDown);
                /*\/ deletar pasta com arquivos dicoms baixados do servidor; */
                this.delDir(dirBase.toPath());
            }
        }
    }

    private void connectAndSendFiles(File dirBase, List<String> caminhoDicomsDown) {
        try{
            if(dirBase.exists()){
                File[] arqsBaixados = dirBase.listFiles();
                final JDBCConnect banco = new JDBCConnect();
                for(int i=0; i<arqsBaixados.length; i++){
                    File file = arqsBaixados[i];
                    InputStream fileStream = new FileInputStream(file);

                    LinkedHashMap<Integer, String[]> atributesDicom = parseDicom(file);
                    if(atributesDicom != null){
                        List<String> values = new ArrayList<>();
                        values.add( atributesDicom.get((0x0010 << 16 | 0x0020))[1] ); // patientid;
                        values.add( atributesDicom.get((0x0010 << 16 | 0x0010))[1] ); // patient_name;
                        values.add( atributesDicom.get((0x0010 << 16 | 0x1010))[1] ); // patient_age;
                        values.add( atributesDicom.get((0x0010 << 16 | 0x0030))[1] ); // patient_birth_date;
                        values.add( atributesDicom.get((0x0010 << 16 | 0x0040))[1] ); // patient_sex;
                        values.add( atributesDicom.get((0x0008 << 16 | 0x0080))[1] ); // institutio_name;
                        values.add( atributesDicom.get((0x0008 << 16 | 0x0020))[1] ); // study_date;
                        values.add( caminhoDicomsDown.get(i) ); // caminho;

                        if(banco.seConectado()){
                            banco.inserir(values, fileStream);
                        }
                    }

                    /*\/\/ fins de testes;;;; */
                    // List<String> values = new ArrayList<>();
                    // values.add("patientid;");
                    // values.add("patient_name;");
                    // values.add("patient_age;");
                    // values.add("patient_birth_date;");
                    // values.add("patient_sex;");
                    // values.add("institutio_name;");
                    // values.add("study_date;");
                    // values.add(caminhoDicomsDown.get(i)); // caminho;
                    // if(banco.seConectado()){
                    //     banco.inserir(values, fileStream);
                    // }
                    /*\/\/ fins de testes;;;; */
                }
                banco.close();
            }
        }catch(java.io.IOException ex){ ex.printStackTrace(); }
    }

    private LinkedHashMap<Integer, String[]> parseDicom(File dicom) {
        LinkedHashMap<Integer, String[]> attr = null;
        AC_DicomReader dicomReader = new AC_DicomReader();
        dicomReader.readDCMFile(dicom.getAbsolutePath());
        AC_DcmStructure dcmStructure = null;
        try {
            dcmStructure = dicomReader.getAttirbutes();
            attr = dcmStructure.getAttributes();
            // HashMap<Integer, String[]> partags = dicomReader.getBitTagToHexParTag();
        } catch (java.io.IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return attr;
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

    private List<String> consultarArquivosBanco() {
        List<String> arqsInexis = null;
        final JDBCConnect con = new JDBCConnect();
        if(con.seConectado()){
            /*\/ arquivos inexistentes no banco; */
            arqsInexis = this.filesDicom.stream().filter(file -> (con.consultarImagem(file) == 0)).collect(Collectors.toList());
        }
        con.close();
        return arqsInexis;
    }

    /* \/ navegar recursivamente em busca de arquivos dicoms,
    a partir de uma pasta base do servidor;
    efetuar downloads apenas de arquivos não registrados no log de arquivos enviados; 
    * */
    public void getDiffLogAndServer(String remoteDir) throws SftpException, JSchException {
        this.freeWalk(remoteDir);

        if(this.filesDicom.size() > 0){
            /*\/ consultar os arquivos no banco, antes de realizar o download dos mesmos; */
            List<String> inexistsFiles = this.consultarArquivosBanco();
            if(inexistsFiles.size() > 0){
                this.downDicomsECompact(inexistsFiles);
            }
        }
        this.close();
        // this.sendZipToServer();
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