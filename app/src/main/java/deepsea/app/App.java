/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package deepsea.app;

import deepsea.list.LinkedList;

import static deepsea.utilities.StringUtils.join;
import static deepsea.utilities.StringUtils.split;
import static deepsea.app.MessageUtils.getMessage;
import deepsea.utilities.SftpClient;
import deepsea.utilities.ZipUtility;
import deepsea.utilities.ParseDicom;
import deepsea.utilities.JSONUtils;

public class App {
    public static void main(String[] args) {
        
        // try{
        //     SftpClient sftp = new SftpClient("187.17.3.12", "a_fhs", "#fhs2018#");
        //     sftp.recursiveListFiles("/home/a_fhs");
        //     sftp.close();
        // }catch(com.jcraft.jsch.SftpException | com.jcraft.jsch.JSchException e){
        //     e.printStackTrace();
        // }

        // ZipUtility zip = new ZipUtility();
        // try{
        //     // zip.zipDirectory("/home/icaro/Downloads/dicom/teste", "tudo");
        //     zip.unzipFile("tudo.zip", "/home/icaro/Downloads/dicom/testeUnZIPx");
        // }catch(java.io.IOException ex){
        //     ex.printStackTrace();
        // }

        ParseDicom pd = new ParseDicom();
        try{
            pd.lerDicom("/home/icaro/Downloads/dicom/teste/WILLIANE_VITORIA_FONSECA_SILVA.CT.ABDOMEN_ABD_TRI_FASICO_MANUAL_(ADULT).0004.0001.2020.01.22.11.18.07.32196.464565879.IMA");
        }catch(java.io.IOException | java.lang.NegativeArraySizeException | ClassNotFoundException ex){
            ex.printStackTrace();
        }

        // JSONUtils j = new JSONUtils();
        // j.readJSON();

    }
}
