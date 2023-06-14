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
import java.io.InputStream;

/**
 * A simple SFTP client using JSCH http://www.jcraft.com/jsch/
 */
public class SftpClient {
    private final String      host;
    private final int         port;
    private final String      username;
    private final JSch        jsch;
    private       ChannelSftp channel;
    private       Session     session;

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
    public final void authPassword(String password) throws JSchException {
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


    public final void authKey(String keyPath, String pass) throws JSchException {
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
    private final void naoUsarRSAkey(){
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
    }

    private final String humanReadableByteCount(long bytes, boolean si) {
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
    public final void listFiles(String remoteDir) throws SftpException, JSchException {
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        // System.out.printf("Listing [%s]...%n", remoteDir);
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
            // System.out.printf("[%s] %s(%s)%n", permissions, name, size);
        }
    }

    /*\/ vetor de conteúdos de um diretório; */
    public final Vector<ChannelSftp.LsEntry> vetorConteudos(String remoteDir){
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
    public final void recursiveListFiles(String remoteDir) throws SftpException, JSchException {
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        // System.out.printf("Listing [%s]...%n", remoteDir);
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
                    // System.out.printf("[%s] %s(%s)%n", permissions, name, size);
                }
            }
        }
    }

    /**
     * Upload a file to remote
     *
     * @param localPath  full path of location file
     * @param remotePath full path of remote file
     * @throws JSchException If there is any problem with connection
     * @throws SftpException If there is any problem with uploading file permissions etc
     */
    public final void uploadFile(String localPath, String remotePath) throws JSchException, SftpException {
        // System.out.printf("Uploading [%s] to [%s]...%n", localPath, remotePath);
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
    public final void downloadFile(String remotePath, String localPath) throws SftpException {
        // System.out.printf("Downloading [%s] to [%s]...%n", remotePath, localPath);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        channel.get(remotePath, localPath);
    }

    /**
     * Download a file from remote
     *
     * @param remotePath full path of remote file
     * @throws SftpException If there is any problem with downloading file related permissions etc
     */
    public final InputStream downloadFile(String remotePath) throws SftpException {
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        return channel.get(remotePath);
    }

    /**
     * Delete a file on remote
     *
     * @param remoteFile full path of remote file
     * @throws SftpException If there is any problem with deleting file related to permissions etc
     */
    public final void delete(String remoteFile) throws SftpException {
        // System.out.printf("Deleting [%s]...%n", remoteFile);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        channel.rm(remoteFile);
    }

    /**
     * Disconnect from remote
     */
    public final void close() {
        if (channel != null) {
            channel.exit();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    public final void cd(String remoteDir) throws SftpException {
        if (channel != null) {
            channel.cd(remoteDir);
        }
    }

    public final ChannelSftp getChannel() {
        return channel;
    }
}
