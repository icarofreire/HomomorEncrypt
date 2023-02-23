/*
 * A simple SFTP client using JSCH http://www.jcraft.com/jsch/
 */
package deepsea.utilities;

import com.jcraft.jsch.*;
import java.util.Properties;
import java.util.Vector;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.stream.Collectors;
import deepsea.utilities.LogFile;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A simple SFTP client using JSCH http://www.jcraft.com/jsch/
 */
public final class SftpClient {
    private final String      host;
    private final int         port;
    private final String      username;
    private final JSch        jsch;
    private       ChannelSftp channel;
    private       Session     session;

    private final treatProfound tProf = new treatProfound();

    /**
     * @param host     remote host
     * @param port     remote port
     * @param username remote username
     */
    public SftpClient(String host, int port, String username) {
        this.host     = host;
        this.port     = port;
        this.username = username;
        jsch          = new JSch();
    }

    /**
     * Use default port 22
     *
     * @param host     remote host
     * @param username username on host
     */
    public SftpClient(String host, String username) {
        this(host, 22, username);
    }

    /**
     * Use default port 22
     *
     * @param host     remote host
     * @param username username on host
     * @param password username on host
     */
    public SftpClient(String host, String username, String password) throws JSchException {
        this(host, 22, username);
        this.authPassword(password);
    }

    /**
     * Authenticate with remote using password
     *
     * @param password password of remote
     * @throws JSchException If there is problem with credentials or connection
     */
    public void authPassword(String password) throws JSchException {
        session = jsch.getSession(username, host, port);
        naoUsarRSAkey();
        //disable known hosts checking
        //if you want to set knows hosts file You can set with jsch.setKnownHosts("path to known hosts file");
        var config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setPassword(password);
        session.connect();
        channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
    }


    public void authKey(String keyPath, String pass) throws JSchException {
        jsch.addIdentity(keyPath, pass);
        session = jsch.getSession(username, host, port);
        naoUsarRSAkey();
        //disable known hosts checking
        //if you want to set knows hosts file You can set with jsch.setKnownHosts("path to known hosts file");
        var config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
    }

    /*\/ configurar para não utilizar o ssh-key(rsa key);
    * Configure JSch to not use "StrictHostKeyChecking" (this introduces insecurities and should only be used for testing purposes); */
    private void naoUsarRSAkey(){
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
    }

    private String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * List all files including directories
     *
     * @param remoteDir Directory on remote from which files will be listed
     * @throws SftpException If there is any problem with listing files related to permissions etc
     * @throws JSchException If there is any problem with connection
     */
    @SuppressWarnings("unchecked")
    public void listFiles(String remoteDir) throws SftpException, JSchException {
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        System.out.printf("Listing [%s]...%n", remoteDir);
        channel.cd(remoteDir);
        Vector<ChannelSftp.LsEntry> files = channel.ls(".");
        for (ChannelSftp.LsEntry file : files) {
            var name        = file.getFilename();
            var attrs       = file.getAttrs();
            var permissions = attrs.getPermissionsString();
            var size        = humanReadableByteCount(attrs.getSize(), true);
            if (attrs.isDir()) {
                size = "PRE";
            }
            System.out.printf("[%s] %s(%s)%n", permissions, name, size);
        }
    }

    /*\/ vetor de conteúdos de um diretório; */
    private Vector<ChannelSftp.LsEntry> vetorConteudos(String remoteDir){
        Vector<ChannelSftp.LsEntry> files = null;
        try{
            files = channel.ls(remoteDir);
            if(!files.isEmpty()){
                Vector<ChannelSftp.LsEntry> filt = files
                .stream()
                .filter(f -> (!f.getFilename().equals(".") && !f.getFilename().equals("..")) )
                .collect(java.util.stream.Collectors.toCollection(Vector::new));
                return filt;
            }
        }catch(SftpException e ){}
        return files;
    }

    /*\/ OBS: melhorar método para buscar recusivamente arquivos DICOM; */
    @SuppressWarnings("unchecked")
    public void recursiveListFiles(String remoteDir) throws SftpException, JSchException {
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        System.out.printf("Listing [%s]...%n", remoteDir);
        channel.cd(remoteDir);
        Vector<ChannelSftp.LsEntry> files = vetorConteudos(remoteDir);
        if(files != null && !files.isEmpty()){
            for (ChannelSftp.LsEntry file : files) {
                var name        = file.getFilename();
                var attrs       = file.getAttrs();
                var permissions = attrs.getPermissionsString();
                var size        = humanReadableByteCount(attrs.getSize(), true);
                if (attrs.isDir()) {
                    size = "DIR";
                }
                // System.out.printf("[%s] %s(%s)%n", permissions, name, size);
                /*\/ entrar em subpastas; */
                if((!name.equals(".") && !name.equals("..")) && attrs.isDir()){
                    String subPasta = remoteDir + java.io.File.separator + name;
                    Vector<ChannelSftp.LsEntry> sub = vetorConteudos(subPasta);
                    if(sub != null){
                        recursiveListFiles(subPasta);
                    }
                    System.out.printf("[%s] %s(%s)%n", permissions, name, size);
                }
            }
        }
    }

    /*\/ auxiliar no retorno da diferença de duas listas de arvores de diretórios; */
    public <T> List<T> diffTwoLists(List<T>listOne, List<T>listTwo) {
        List<T> differences = listOne.stream()
            .filter(element -> !listTwo.contains(element))
            .collect(Collectors.toList());
        return differences;
    }

    private class treatProfound {
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
        /*\/ pastas que possuem profundo grau de subpastas encontradas na busca; */
        private final List<String> pastasEvit = new ArrayList<String>();
        private final List<String> pastasVisi = new ArrayList<String>();

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

        private void gerarLogTreeFiles(){
            if(this.pastasFiles.size() > 0){
                try {
                    FileWriter myWriter = new FileWriter("tree.txt");
                    for(String p: this.pastasFiles){
                        myWriter.write(p + "\n");
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
                final Vector<ChannelSftp.LsEntry> files = channel.ls(remoteDir);
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
            if (channel == null) {
                throw new IllegalArgumentException("Connection is not available");
            }
            System.out.printf("Listing [%s] -> [%d]...%n", remoteDir, this.pastasFiles.size());
            if(this.pastaBase == null){
                this.pastaBase = remoteDir;
            }
            if(!this.pastasVisi.contains(remoteDir) && this.pastasVisi.size() < this.maxProfund){
                channel.cd(remoteDir);
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

    }// end class treatProfound;

    public void freeWalk(String remoteDir) throws SftpException, JSchException {
        tProf.freeWalk(remoteDir);
    }

    public void gerarLogTreeFiles(){
        tProf.gerarLogTreeFiles();
    }

    /**
     * Upload a file to remote
     *
     * @param localPath  full path of location file
     * @param remotePath full path of remote file
     * @throws JSchException If there is any problem with connection
     * @throws SftpException If there is any problem with uploading file permissions etc
     */
    public void uploadFile(String localPath, String remotePath) throws JSchException, SftpException {
        System.out.printf("Uploading [%s] to [%s]...%n", localPath, remotePath);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        channel.put(localPath, remotePath);
    }

    /**
     * Download a file from remote
     *
     * @param remotePath full path of remote file
     * @param localPath  full path of where to save file locally
     * @throws SftpException If there is any problem with downloading file related permissions etc
     */
    public void downloadFile(String remotePath, String localPath) throws SftpException {
        System.out.printf("Downloading [%s] to [%s]...%n", remotePath, localPath);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        channel.get(remotePath, localPath);
    }

    /**
     * Delete a file on remote
     *
     * @param remoteFile full path of remote file
     * @throws SftpException If there is any problem with deleting file related to permissions etc
     */
    public void delete(String remoteFile) throws SftpException {
        System.out.printf("Deleting [%s]...%n", remoteFile);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        channel.rm(remoteFile);
    }

    /**
     * Disconnect from remote
     */
    public void close() {
        if (channel != null) {
            channel.exit();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
