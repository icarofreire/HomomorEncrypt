/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package deepsea.app;


import deepsea.utilities.BuscasDicom;
import deepsea.utilities.TimeExecution;
import deepsea.utilities.Scheduler;
import deepsea.utilities.DBOperations;
import deepsea.utilities.Server;
import deepsea.utilities.JDBCConnection;
import deepsea.utilities.DataMigration;
import deepsea.utilities.ReadFileConf;

import java.util.Vector;

/*\/ parse dicom; */
import AC_DicomIO.AC_DcmStructure;
import AC_DicomIO.AC_DicomReader;


/**
 * // deepsea-de11000m36000ft
 * // seadeep-de11000m36000ft
 */
public class App {
    public static void main(String[] args) {

        /*\/ atribuir servidores pacs; */
        Vector<Server> servers = new Vector<Server>();
        // servers.add(new Server("172.23.12.15", "root", "ZtO!@#762", "/home/storage-pacs"));
        // servers.add(new Server("172.23.13.16", "suporte", "F0t012va@", "/storage-pacs"));
        // servers.add(new Server("172.22.17.130", "suporte", "F0t012va@", "/home/storage-pacs"));

        ReadFileConf conf = new ReadFileConf();
        servers = conf.readJsonConf();

        Scheduler sche = new Scheduler();
        sche.setServers(servers);
        /*\/ iniciar schedule para multiplos servidores; */
        // sche.iniParallel();


    }
}
