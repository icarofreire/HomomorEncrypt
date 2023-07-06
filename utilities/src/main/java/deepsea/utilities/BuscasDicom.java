/*
 * Buscas de arquivos DICOM;
 */
package deepsea.utilities;

import com.jcraft.jsch.*;
import java.util.Vector;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import deepsea.utilities.DBOperations;
import deepsea.utilities.MultiConnections;
import deepsea.utilities.LogEvitFile;
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
    private final Set<String> pastasVisi = Collections.synchronizedSet(new HashSet<String>());
    /*\/ pasta para baixar os arquivos dicoms encontrados no servidor; */
    private final String pastaDownDicoms = "Down";
    /*\/ log de dados do banco; */
    private final String arquivoLogDadosDB = "log-dados-DB";
    /*\/ caminho completo dos arquivos dicoms encontrados no servidor; */
    private final Vector<String> filesDicom = new Vector<String>();
    /*\/ info processo de realização do procedimento; */
    private final boolean verbose = true;
    /*\/ classe para comprimir arquivos; */
    private final Compress comp = new Compress();
    /*\/ classe para multiplas conexões com bancos;*/
    // private final MultiConnections multiConnections = new MultiConnections();
    /*\/ vetor de datas de realização de cada imagem(obtida pelo caminho da imagem no servidor pacs); */
    private final Vector<LocalDateTime> datesEvit = new Vector<LocalDateTime>();
    /*\/ operações com arquivos inválidos encontrados na busca, através de log; */
    private final LogEvitFile logEvitFile = new LogEvitFile();

    public BuscasDicom(String host, String username, String password) throws JSchException {
        super(host, username, password);
        logEvitFile.setHost(host);
    }

    /*\/ verificar o número de subpastas de um caminho e inserir a pastabase do caminho para ser evitada,
    caso o número de subpastas seja muito longo, para não prolongar profundidade de análise; */
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

    private final void studyDateAvoid(LocalDateTime studyDate) {
        if(!datesEvit.contains(studyDate)) datesEvit.add(studyDate);
    }

    private final boolean seCaminhoEvitar(String remoteDir) {
        /*\/ evitar caminho com data de realização antiga aos objetivos do projeto; */
        boolean evitCaminhoDataStudo = false;
        /*\/ evitar basta base com longo caminho de subpastas; */
        boolean evitPasta = false;

        /*\/ verificar se pasta base de um caminho remoto foi inserida como uma pasta a ser evitada no vetor,
         * para evitar continuar a profundidade das buscas; */
        if(!remoteDir.equals(this.pastaBase) && pastasEvit.size() > 0){
            Vector<String> fil = pastasEvit.stream().filter(f -> (f.indexOf(remoteDir) != -1)).collect(Collectors.toCollection(Vector::new));
            evitPasta = (fil.size() > 0);
            // if(verbose && evitPasta) System.out.println("EVITAR PASTA:: " + remoteDir);
        }

        /*\/ verificar se data de realização da imagem(obtida pelo caminho remoto),
        contém no vetor de datas a evitar sobre os objetivos do projeto; */
        LocalDateTime studyDate = extractDataPath(remoteDir);
        evitCaminhoDataStudo = datesEvit.contains(studyDate);
        // if(verbose && evitCaminhoDataStudo) System.out.println("EVITAR:: " + studyDate);
        return (evitCaminhoDataStudo || evitPasta);
    }

    /*\/ retorna o caminho da primeira basta após a pasta base, no caminho do arquivo; 
    * Ex:
    * entrada: /home/storage-pacs/HOSPITALDACRIANCA/CR/2022/12/22/1.2.392.200036.9107.307.31409.113140922122215165
    * retorno: /home/storage-pacs/HOSPITALDACRIANCA
    */
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
    private final synchronized void freeWalk(String remoteDir, final DBOperations banco) throws SftpException, JSchException {
        if (getChannel() == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        if(verbose) System.out.printf("[%s] Listing [%s]...%n", getHost(), remoteDir);
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
                        if(!this.seCaminhoEvitar(remoteDir)){
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
                        String pathFile = remoteDir + java.io.File.separator + name;

                        /*\/ se nome de arquivo e host estão listados no log de arquivos a serem evitados; */
                        boolean evitFile = logEvitFile.seContemArquivoInv(name);
                        if(!evitFile){
                            /*\/ análise da data de realização do dicom, pelo caminho do arquivo; */
                            LocalDateTime studyDate = extractDataPath(pathFile);
                            if(studyDate != null){
                                boolean presenteStudyDate = compararDataStudoComdataInicioMesAtual(studyDate);
                                if(presenteStudyDate){
                                    /*\/ verificar se imagem existe no banco de dados; */
                                    if(banco.seConectado() && banco.consultarImagem(name) == 0){
                                        this.filesDicom.add(pathFile);
                                    }
                                }else{
                                    studyDateAvoid(studyDate);
                                }
                            }else{
                                /*\/ não foi encontrada nenhuma data de realização da imagem no caminho
                                da imagem encontrada(em forma de subpastas, ex: </unidadeX/ano/mes/dia/imagem>);
                                Neste caso a imagem será adicionada para download, para verificação posterior
                                da data de estudo do dicom, para verificar se a imagem está com a data de realização
                                compatível com os objetivos do projeto.
                                */
                                /*\/ verificar se imagem existe no banco de dados; */
                                if(banco.seConectado() && banco.consultarImagem(name) == 0){
                                    this.filesDicom.add(pathFile);
                                }
                            }
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
    private final synchronized void downDicomsECompact(final Vector<String> caminhoDicomsDown) {
        if(caminhoDicomsDown.size() > 0){
            File dirBase = new File(this.pastaDownDicoms + "-" + gerateRandomString(10));
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

    private final synchronized void connectAndSendFiles(File dirBase) {
        try{
            if(dirBase.exists()){
                if(verbose) System.out.println(">> Enviando imagens ao DB;");
                File[] arqsBaixados = dirBase.listFiles();
                final DBOperations banco = new DBOperations();
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
                        /*\/ verificar data de realização do dicom; */
                        boolean regDicom = registrarDicomPorDataEstudo(studyDate);
                        /*\/ verificar se imagem não existe no banco; */
                        boolean imageNotExist = (banco.consultarImagem(file.getName()) == 0);

                        if(banco.seConectado() && imageNotExist && regDicom){
                            banco.inserir(values, fileStream);

                            /*\/ inserir valores em multiplas conexões JDBC; */
                            // banco.insertInServers(this.multiConnections, values, fileStream);
                        }
                        /*\/ registrar num log os arquivos inválidos; */
                        if(!regDicom){
                            String descriptInvFile = file.getName() + ":" + logEvitFile.getHost();
                            logEvitFile.createLogInvFiles(descriptInvFile);
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
        final AC_DicomReader dicomReader = new AC_DicomReader();
        dicomReader.readDCMFile(dicom.getAbsolutePath());
        try {
            final AC_DcmStructure dcmStructure = dicomReader.getAttirbutes();
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
    public final synchronized void scanServer(String remoteDir) throws SftpException, JSchException {
        if(verbose) System.out.println(">> Realizando buscas no servidor;");
        final DBOperations banco = new DBOperations();
        logEvitFile.readInvFiles();
        // /*\/ criar multiplas conexões; */
        // this.multiConnections.createImmediateMultiConnections();
        this.freeWalk(remoteDir, banco);
        banco.close();

        if(this.filesDicom.size() > 0){
            if(verbose) System.out.println(">> Download + " + this.filesDicom.size() + " imagens;");
            this.downDicomsECompact(this.filesDicom);
        }
        /*\/ fechar conexão remota; */
        this.close();
        // this.createLogDadosDB();
        if(verbose) System.out.println(">> Fim;");
    }

    /*\/ criar log de dados do banco; */
    private final void createLogDadosDB() {
        final DBOperations banco = new DBOperations();
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

    /*\/ decrementar em dias a data atual; */
    private final LocalDateTime minusDaysActualLocalDateTime() {
        long days = 3;
        LocalDateTime actual = LocalDateTime.now();
        return actual.minusDays(days);
    }

    /*\/ retorna a data de inicio de realização das imagens a serem coletadas; */
    private final LocalDateTime dataInicioColetaImagens() {
        /*\/ somente serão buscadas as imagens que foram realizadas há N dias
        anteriores a data atual de execução do programa; */
        return minusDaysActualLocalDateTime();
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
            /*\/ data inicial do mês atual, em que os arquivos dicoms gerados devem ser coletados; */
            LocalDateTime dataInicioColeta = dataInicioColetaImagens();
            /*\/ se data de estudo do dicom é posterior a data limite definida para coleta dos dicoms; */
            if(dataEstudoDicom.isAfter(dataInicioColeta) || dataEstudoDicom.isEqual(dataInicioColeta)){
                ok = true;
            }
        }
        return ok;
    }

    private final boolean compararDataStudoComdataInicioMesAtual(LocalDateTime dataEstudoDicom) {
        boolean ok = false;
        /*\/ data inicial do mês atual, em que os arquivos dicoms gerados devem ser coletados; */
        LocalDateTime dataInicioColeta = dataInicioColetaImagens();
        /*\/ se data de estudo do dicom é posterior a data limite definida para coleta dos dicoms; */
        if(dataEstudoDicom.isAfter(dataInicioColeta) || dataEstudoDicom.isEqual(dataInicioColeta)){
            ok = true;
        }
        return ok;
    }

    private final String gerateRandomString(int tam) {
        return new java.util.Random().ints(tam, 97, 122).mapToObj(i -> String.valueOf((char)i)).collect(java.util.stream.Collectors.joining());
    }

    private final LocalDateTime extractDataPath(String filePath){
        LocalDateTime dateTime = null;
        Pattern pattern = Pattern.compile(".*(\\d{4}/\\d{2}/\\d{2}).*");
        Matcher matcher = pattern.matcher(filePath);
        if(matcher.matches()) {
            String data = matcher.group(1) + " 00:00:00";
            String formatoTempo = "yyyy/MM/dd HH:mm:ss";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatoTempo);
            dateTime = LocalDateTime.parse(data, formatter);
        }
        return dateTime;
    }
    
}