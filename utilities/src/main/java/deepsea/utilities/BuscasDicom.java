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
import java.util.Set;
import java.util.HashSet;
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
    private final long maxProfund = 500;
    /*\/ quantidade máxima de arquivos a serem baixados do servidor; */
    private final long maxDownloadFiles = 1000;
    /*\/ tipos de arquivos a procurar nas buscas em pasta e subpastas; */
    private final List<String> filesTypes = Arrays.asList(".dcm", ".ima");
    /*\/ pasta base onde a busca é iniciada; */
    private String pastaBase = null;
    /*\/ profundidade de subpastas em uma pasta encontrada na busca; */
    private final int maxSubpastas = 10;
    /*\/ pastas que possuem profundo grau de subpastas encontradas na busca; */
    private final List<String> pastasEvit = new ArrayList<String>();
    // private final List<String> pastasVisi = new ArrayList<String>();
    private final Set<String> pastasVisi = new HashSet<String>();
    /*\/ pasta para baixar os arquivos dicoms encontrados no servidor; */
    private final String pastaDownDicoms = "DownDicoms";
    /*\/ log de dados do banco; */
    private final String arquivoLogDadosDB = "log-dados-DB";
    /*\/ caminho completo dos arquivos dicoms encontrados no servidor; */
    private final List<String> filesDicom = new ArrayList<String>();
    /*\/ info processo de realização do procedimento; */
    private final boolean verbose = true;

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
        // System.out.printf("Listing [%s]...%n", remoteDir);
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
            File dirBase = new File(this.pastaDownDicoms);
            if(!dirBase.exists()){
                dirBase.mkdir();
            }
            int con = 0;
            boolean error = false;
            for(String pathFile : caminhoDicomsDown){
                try{
                    if(con < maxDownloadFiles){
                        downloadFile(pathFile, dirBase.getAbsolutePath());
                        con++;
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

    private String nameFileWithoutExtension(String nameFile) {
        return nameFile.substring(0, nameFile.lastIndexOf("."));
    }

    private InputStream streamCompactFile(File file, final ZipUtility zip) throws java.io.IOException {
        InputStream zipDicomStream = null;
        String zipName = nameFileWithoutExtension(file.getName()) + ZipUtility.format;
        File fileDicomZip = new File(zipName);
        if(zip.zipFile(file, zipName)){
            zipDicomStream = new FileInputStream(fileDicomZip);
            fileDicomZip.delete();
        }
        return zipDicomStream;
    }

    private void connectAndSendFiles(File dirBase, List<String> caminhoDicomsDown) {
        try{
            if(dirBase.exists()){
                if(verbose) System.out.println(">> Enviando imagens ao DB;");
                File[] arqsBaixados = dirBase.listFiles();
                final JDBCConnect banco = new JDBCConnect();
                final ZipUtility zip = new ZipUtility();
                for(int i=0; i<arqsBaixados.length; i++){
                    File file = arqsBaixados[i];
                    InputStream fileStream = new FileInputStream(file);

                    fileStream = streamCompactFile(file, zip);

                    LinkedHashMap<Integer, String[]> atributesDicom = parseDicom(file);
                    if(atributesDicom != null){
                        List<String> values = new ArrayList<>();
                        values.add( (atributesDicom.containsKey((0x0010 << 16 | 0x0020))) ? (atributesDicom.get((0x0010 << 16 | 0x0020))[1]) : (null) ); // patient_id;
                        values.add( (atributesDicom.containsKey((0x0010 << 16 | 0x0010))) ? (atributesDicom.get((0x0010 << 16 | 0x0010))[1]) : (null) ); // patient_name;
                        values.add( (atributesDicom.containsKey((0x0010 << 16 | 0x1010))) ? (atributesDicom.get((0x0010 << 16 | 0x1010))[1]) : (null) ); // patient_age;
                        values.add( (atributesDicom.containsKey((0x0010 << 16 | 0x0030))) ? (atributesDicom.get((0x0010 << 16 | 0x0030))[1]) : (null) ); // patient_birth_date;
                        values.add( (atributesDicom.containsKey((0x0010 << 16 | 0x0040))) ? (atributesDicom.get((0x0010 << 16 | 0x0040))[1]) : (null) ); // patient_sex;
                        values.add( (atributesDicom.containsKey((0x0008 << 16 | 0x0080))) ? (atributesDicom.get((0x0008 << 16 | 0x0080))[1]) : (null) ); // institutio_name;
                        values.add( (atributesDicom.containsKey((0x0008 << 16 | 0x0020))) ? (atributesDicom.get((0x0008 << 16 | 0x0020))[1]) : (null) ); // study_date;
                        values.add( (atributesDicom.containsKey((0x0020 << 16 | 0x0010))) ? (atributesDicom.get((0x0020 << 16 | 0x0010))[1]) : (null) ); // study_id;
                        values.add( (atributesDicom.containsKey((0x0020 << 16 | 0x0011))) ? (atributesDicom.get((0x0020 << 16 | 0x0011))[1]) : (null) ); // series_number;
                        values.add( file.getName() ); // path;

                        // String studyDate = (atributesDicom.containsKey((0x0008 << 16 | 0x0020))) ? (atributesDicom.get((0x0008 << 16 | 0x0020))[1]) : (null);
                        // if(studyDate != null){
                        //     /* formato da data: "20221208"; ex: "20221209" */
                        //     String ano = studyDate.substring(0, 4); // ano;
                        //     String mes = studyDate.substring(4, 6); // mes;
                        //     String dia = studyDate.substring(6, 8); // dia;
                        //     System.out.println(">>" + studyDate);
                        //     System.out.println(">>" + dia + "/" + mes + "/" + ano);
                        // }

                        if(banco.seConectado()){
                            banco.inserir(values, fileStream);
                        }
                    }
                }
                if(verbose) System.out.println(">> Tamanho DB: " + banco.tamanhoBanco());
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
            if(dcmStructure != null){
                attr = dcmStructure.getAttributes();
                // HashMap<Integer, String[]> partags = dicomReader.getBitTagToHexParTag();
            }else{
                /*\/ not dicom(.dcm/.ima) file; */
                // System.out.println(">> [NULL];");
            }
        } catch (java.io.IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return attr;
    }

    private List<String> consultarArquivosBanco() {
        List<String> arqsInexis = null;
        final JDBCConnect con = new JDBCConnect();

        List<String> listVeri = new ArrayList<>();
        if(this.filesDicom.size() > maxDownloadFiles) {
            /*\/ fatia das arquivos a serem enviados pelo limitador de arquvos baixados; */
            List<String> part = this.filesDicom.subList(new Long(maxDownloadFiles).intValue(), this.filesDicom.size()-1);
            listVeri = part;
        }else{
            listVeri = this.filesDicom;
        }

        if(con.seConectado()){
            /*\/ arquivos inexistentes no banco; */
            arqsInexis = listVeri.stream().filter(file -> {

                String[] parts = file.split("\\/");
                file = parts[parts.length-1];

                return (con.consultarImagem(file) == 0);
            }).collect(Collectors.toList());
        }
        con.close();
        return arqsInexis;
    }

    /* \/ navegar recursivamente em busca de arquivos dicoms,
    a partir de uma pasta base do servidor;
    efetuar downloads apenas de arquivos não registrados no log de arquivos enviados; 
    * */
    public void getDiffLogAndServer(String remoteDir) throws SftpException, JSchException {
        if(verbose) System.out.println(">> Realizando buscas no servidor;");
        this.freeWalk(remoteDir);

        if(this.filesDicom.size() > 0){
            System.out.println(this.filesDicom.size() + " imagens encontradas;");
            /*\/ consultar os arquivos no banco, antes de realizar o download dos mesmos; */
            List<String> inexistsFiles = this.consultarArquivosBanco();
            if(inexistsFiles.size() > 0){
                if(verbose) System.out.println(">> Download + " + inexistsFiles.size() + " imagens;");
                this.downDicomsECompact(inexistsFiles);
            }
        }
        this.close();
        // this.createLogDadosDB();
        if(verbose) System.out.println(">> Fim;");
    }

    /*\/ criar log de dados do banco; */
    private void createLogDadosDB() {
        final JDBCConnect banco = new JDBCConnect();
        /*\/ date now(); */
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String formattedDateTime = LocalDateTime.now().format(formatter);
        File logCheck = new File(arquivoLogDadosDB);
        boolean append = logCheck.exists();
        try {
            FileWriter myWriter = new FileWriter(arquivoLogDadosDB, /*append*/ false);
            myWriter.write(">> Tamanho DB: " + banco.tamanhoBanco() + "|" + formattedDateTime + ";\n");
            myWriter.write(">> Número registros: " + banco.numeroRegistros() + "|" + formattedDateTime + ";\n");
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        banco.close();
    }

    public void close() {
        super.close();
    }
}