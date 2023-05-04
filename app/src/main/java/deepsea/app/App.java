/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package deepsea.app;

import deepsea.list.LinkedList;

import deepsea.utilities.BuscasDicom;
import deepsea.utilities.TimeExecution;
import deepsea.utilities.Scheduler;
import deepsea.utilities.JDBCConnect;
import deepsea.utilities.FileOperationsMinio;
import deepsea.utilities.Server;

import java.util.Vector;

/*\/ parse dicom; */
import AC_DicomIO.AC_DcmStructure;
import AC_DicomIO.AC_DicomReader;

public class App {
    public static void main(String[] args) {

        /*\/ iniciar schedule para buscas nos servidores; */
        // Scheduler sche = new Scheduler();
        // sche.ini();


        // /*\/ testes;; */
        // try{
        //     final BuscasDicom busca = new BuscasDicom("172.23.12.15", "root", "ZtO!@#762");
        //     busca.getDiffLogAndServer("/home/storage-pacs");
        // }catch(com.jcraft.jsch.SftpException | com.jcraft.jsch.JSchException e){
        //     e.printStackTrace();
        // }

        // JDBCConnect banco = new JDBCConnect();
        // banco.transferImagesToMinio();
        // banco.transferImagesCompactToMinio();
        // System.out.println("TAM: " + banco.tamanhoBanco());
        // banco.close();

        // JDBCConnect banco = new JDBCConnect();
        // byte[] bytes = banco.getBytesImageByID(1);
        // System.out.println("bytes: " + bytes.length);
        // banco.close();

        // FileOperationsMinio minio = new FileOperationsMinio("compact-dicoms");
        // if(!minio.getErrorConnection()){
        //     minio.listObjects();
        //     System.out.println("T: " + minio.totalObjects());
        //     // minio.downloadObjectAndUnzipFileToInputStream("1.2.392.200036.9107.307.31409.20230222.225031.1066960.zip", "/home/icaro/Documentos/DeepSea");
        // }


        Vector<Server> servers = new Vector<Server>();
        servers.add(new Server("172.23.12.15", "root", "ZtO!@#762", "/home/storage-pacs"));
        servers.add(new Server("172.23.12.15", "root", "ZtO!@#762", "/home/storage-pacs"));
        servers.add(new Server("172.23.12.15", "root", "ZtO!@#762", "/home/storage-pacs"));

        Scheduler sche = new Scheduler();
        // sche.iniParallel(servers);

    }
}
