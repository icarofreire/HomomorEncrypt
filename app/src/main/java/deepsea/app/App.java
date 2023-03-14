/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package deepsea.app;

import deepsea.list.LinkedList;

import deepsea.utilities.BuscasDicom;
import deepsea.utilities.TimeExecution;
import deepsea.utilities.Scheduler;
import deepsea.utilities.JDBCConnect;

/*\/ parse dicom; */
import AC_DicomIO.AC_DcmStructure;
import AC_DicomIO.AC_DicomReader;

public class App {
    public static void main(String[] args) {

        Scheduler sche = new Scheduler();
        sche.ini();

        // JDBCConnect banco = new JDBCConnect();
        // System.out.println( banco.seConectado() );
        // System.out.println(">> Tamanho DB: " + banco.tamanhoBanco());
        // System.out.println(">> Número registros: " + banco.numeroRegistros());
        // /*\/ realizar teste de integridade de imagem dicom salva no banco; */
        // banco.testeBaixarImagemDICOM();

    }
}
