/*
 * Buscas de arquivos DICOM;
 */
package deepsea.utilities;

import com.jcraft.jsch.*;
import java.util.Properties;
import java.util.Vector;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.io.File;
import java.nio.file.Files;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import deepsea.utilities.SftpClient;
import deepsea.utilities.Compress;
import deepsea.utilities.JDBCConnect;
import java.nio.file.Path;

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
    /*\/ extensão do arquivo DICOM; */
    private final String extDICOM = ".dcm";
    /*\/ tipos de arquivos a procurar nas buscas em pasta e subpastas; */
    private final Vector<String> filesTypes = new Vector(Arrays.asList(extDICOM));
    /*\/ pasta base onde a busca é iniciada; */
    private String pastaBase = null;
    /*\/ profundidade de subpastas em uma pasta encontrada na busca; */
    private final int maxSubpastas = 10;
    /*\/ pastas que possuem profundo grau de subpastas encontradas na busca; */
    private final Vector<String> pastasEvit = new Vector<String>();
    /*\/ pastas já visitadas pelas buscas; */
    private final Set<String> pastasVisi = new HashSet<String>();
    /*\/ pasta para baixar os arquivos dicoms encontrados no servidor; */
    private String pastaDownDicoms = "Down";
    /*\/ log de dados do banco; */
    private final String arquivoLogDadosDB = "log-dados-DB";
    /*\/ caminho completo dos arquivos dicoms encontrados no servidor; */
    private final Vector<String> filesDicom = new Vector<String>();
    /*\/ info processo de realização do procedimento; */
    private final boolean verbose = false;
    /*\/ classe para comprimir arquivos; */
    private final Compress comp = new Compress();
    /*\/ uma data predecessora em que os arquivos dicoms gerados devem ser coletados; */
    private final String dataInicioPeriodo = "01/05/2023 00:00:00"; /* formato: dd/MM/yyyy HH:mm:ss */

    public BuscasDicom(String host, String username, String password) throws JSchException {
        super(host, username, password);
    }

    /*\/ ; */
    private final String chequeDeepVoltaBaseMax(String caminho) {
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

    private final boolean seCaminhoEvitar(String caminho) {
        Vector<String> fil = pastasEvit.stream().filter(f -> (f.indexOf(caminho) != -1)).collect(Collectors.toCollection(Vector::new));
        return (fil.size() > 0);
    }

    private final String getPastaEvitar(String pastaBase, String caminho) {
        int ind = caminho.indexOf(pastaBase);
        if(ind != -1){
            return caminho.substring(0, caminho.indexOf(java.io.File.separator, pastaBase.length()+1));
        }
        return null;
    }

    /*\/ se arquivo é do tipo DICOM; */
    private final boolean seDICOM(String arq){
        int lastp = arq.lastIndexOf('.');
        if(lastp != -1){
            String ext = arq.substring(lastp, arq.length()).toLowerCase();
            return (filesTypes.contains(ext));
        }else{
            return false;
        }
    }

    private final int quantDicoms(String remoteDir){
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
    private final void freeWalk(String remoteDir, final JDBCConnect banco) throws SftpException, JSchException {
        if (getChannel() == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        // System.out.printf("Listing [%s]...%n", remoteDir);
        if(this.pastaBase == null){
            this.pastaBase = remoteDir;
        }
        if(!this.pastasVisi.contains(remoteDir) && this.pastasVisi.size() < this.maxProfund && (this.filesDicom.size() < maxDownloadFiles) ){
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
                                freeWalk(subPasta, banco);
                            }
                        }
                    }else if(this.seDICOM(name)){
                        /*\/ verificar se imagem existe no banco de dados; */
                        if(banco.seConectado() && banco.consultarImagem(name) == 0){
                            String pathFile = remoteDir + java.io.File.separator + name;
                            this.filesDicom.add(pathFile);
                        }
                    }
                }
            }
        }else{
            if(verbose) System.out.println("** [Max profund]");
        }
    }

    private final void delDir(Path dir) {
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
    private final void downDicomsECompact(final Vector<String> caminhoDicomsDown) {
        if(caminhoDicomsDown.size() > 0){
            this.pastaDownDicoms += "-" + gerateRandomString(10);
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
                connectAndSendFiles(dirBase);
                /*\/ deletar pasta com arquivos dicoms baixados do servidor; */
                this.delDir(dirBase.toPath());
            }else{
                /*\/ deletar pasta com arquivos dicoms baixados do servidor --
                caso ocorra algum erro em baixar as imagens; */
                this.delDir(dirBase.toPath());
            }
        }
    }

    private final InputStream streamCompactFile(File file) throws java.io.IOException {
        InputStream zipDicomStream = null;
        String absPath = file.getAbsolutePath();
        String compressFileName = absPath.replace(extDICOM, Compress.ext);
        File fileDicomComp = new File(compressFileName);

        if(comp.compress(absPath, compressFileName)){
            zipDicomStream = new FileInputStream(fileDicomComp);
            fileDicomComp.delete();
        }
        return zipDicomStream;
    }

    private final void connectAndSendFiles(File dirBase) {
        try{
            if(dirBase.exists()){
                if(verbose) System.out.println(">> Enviando imagens ao DB;");
                File[] arqsBaixados = dirBase.listFiles();
                final JDBCConnect banco = new JDBCConnect();
                for(int i=0; i<arqsBaixados.length; i++){
                    File file = arqsBaixados[i];
                    InputStream fileStream = new FileInputStream(file);

                    fileStream = streamCompactFile(file);

                    LinkedHashMap<Integer, String[]> atributesDicom = parseDicom(file);
                    if(atributesDicom != null){
                        Vector<String> values = new Vector<String>();
                        values.add( (atributesDicom.containsKey((0x0010 << 16 | 0x0020))) ? (atributesDicom.get((0x0010 << 16 | 0x0020))[1]) : (null) );
                        values.add( (atributesDicom.containsKey((0x0010 << 16 | 0x0010))) ? (atributesDicom.get((0x0010 << 16 | 0x0010))[1]) : (null) );
                        values.add( (atributesDicom.containsKey((0x0010 << 16 | 0x1010))) ? (atributesDicom.get((0x0010 << 16 | 0x1010))[1]) : (null) );
                        values.add( (atributesDicom.containsKey((0x0010 << 16 | 0x0030))) ? (atributesDicom.get((0x0010 << 16 | 0x0030))[1]) : (null) );
                        values.add( (atributesDicom.containsKey((0x0010 << 16 | 0x0040))) ? (atributesDicom.get((0x0010 << 16 | 0x0040))[1]) : (null) );
                        values.add( (atributesDicom.containsKey((0x0008 << 16 | 0x0080))) ? (atributesDicom.get((0x0008 << 16 | 0x0080))[1]) : (null) );
                        values.add( (atributesDicom.containsKey((0x0008 << 16 | 0x0020))) ? (atributesDicom.get((0x0008 << 16 | 0x0020))[1]) : (null) );
                        values.add( (atributesDicom.containsKey((0x0020 << 16 | 0x0010))) ? (atributesDicom.get((0x0020 << 16 | 0x0010))[1]) : (null) );
                        values.add( (atributesDicom.containsKey((0x0020 << 16 | 0x0011))) ? (atributesDicom.get((0x0020 << 16 | 0x0011))[1]) : (null) );
                        values.add( file.getName() );

                        String studyDate = (atributesDicom.containsKey((0x0008 << 16 | 0x0020))) ? (atributesDicom.get((0x0008 << 16 | 0x0020))[1]) : (null);
                        /*\/ ; */
                        boolean regDicom = registrarDicomPorDataEstudo(studyDate);

                        if(banco.seConectado() && regDicom){
                            banco.inserir(values, fileStream);
                        }
                    }
                }
                if(verbose) System.out.println(">> Tamanho DB: " + banco.tamanhoBanco());
                banco.close();
            }
        }catch(java.io.IOException ex){ ex.printStackTrace(); }
    }

    private final LinkedHashMap<Integer, String[]> parseDicom(File dicom) {
        LinkedHashMap<Integer, String[]> attr = null;
        AC_DicomReader dicomReader = new AC_DicomReader();
        dicomReader.readDCMFile(dicom.getAbsolutePath());
        AC_DcmStructure dcmStructure = null;
        try {
            dcmStructure = dicomReader.getAttirbutes();
            if(dcmStructure != null){
                attr = dcmStructure.getAttributes();
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

    /* \/ navegar recursivamente em busca de arquivos dicoms,
    a partir de uma pasta base do servidor;
    efetuar downloads apenas de arquivos não registrados no log de arquivos enviados; 
    * */
    public final void getDiffLogAndServer(String remoteDir) throws SftpException, JSchException {
        if(verbose) System.out.println(">> Realizando buscas no servidor;");
        final JDBCConnect banco = new JDBCConnect();
        this.freeWalk(remoteDir, banco);
        banco.close();

        if(this.filesDicom.size() > 0){
            if(verbose) System.out.println(">> Download + " + this.filesDicom.size() + " imagens;");
            this.downDicomsECompact(this.filesDicom);
        }
        /*\/ fechar conexão remota; */
        this.close();
        this.createLogDadosDB();
        if(verbose) System.out.println(">> Fim;");
    }

    /*\/ criar log de dados do banco; */
    private final void createLogDadosDB() {
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

    /*\/ fechar conexão remota; */
    public final void closeCon() {
        super.close();
    }

    private final LocalDateTime stringToLocalDateTime(String data) {
        String formatoTempo = "dd/MM/yyyy HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatoTempo);
        LocalDateTime dateTime = LocalDateTime.parse(data, formatter);
        return dateTime;
    }

    /*\/ análise da data de estudo do dicom para comparação com data predecessora estabelecida,
    para coleta dos dicoms encontrados; */
    private final boolean registrarDicomPorDataEstudo(String studyDate) {
        boolean ok = false;
        if(studyDate != null){
            /* ex formato da data: "20221208"; ex: "20221209" */
            String ano = studyDate.substring(0, 4); // ano;
            String mes = studyDate.substring(4, 6); // mes;
            String dia = studyDate.substring(6, 8); // dia;
            String data = dia + "/" + mes + "/" + ano + " 00:00:00";
            LocalDateTime dataEstudoDicom = stringToLocalDateTime(data);
            LocalDateTime dataInicioColeta = stringToLocalDateTime(dataInicioPeriodo);
            /*\/ se data de estudo do dicom é posterior a data limite definida para coleta dos dicoms; */
            if(dataEstudoDicom.isAfter(dataInicioColeta)){
                ok = true;
            }
        }
        return ok;
    }

    private final String gerateRandomString(int tam) {
        return new java.util.Random().ints(tam, 97, 122).mapToObj(i -> String.valueOf((char)i)).collect(java.util.stream.Collectors.joining());
    }
    
}