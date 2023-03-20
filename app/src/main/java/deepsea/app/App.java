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

/*\/ parse dicom; */
import AC_DicomIO.AC_DcmStructure;
import AC_DicomIO.AC_DicomReader;

public class App {
    public static void main(String[] args) {

        Scheduler sche = new Scheduler();
        sche.ini();

        // JDBCConnect banco = new JDBCConnect();
        // banco.transferImagesToMinio();
        // banco.transferImagesCompactToMinio();
        // banco.close();

        // FileOperationsMinio minio = new FileOperationsMinio("icaroteste");
        // if(!minio.getErrorConnection()){
        //     minio.listObjects();
        //     System.out.println("T: " + minio.totalObjects());
        // //     // minio.downloadObjectAndUnzipFileToInputStream("1.2.392.200036.9107.307.31409.20230222.225031.1066960.zip", "/home/icaro/Documentos/DeepSea");
        // }

    }
}
