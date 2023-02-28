/*
 * generate Checksum for a file;
 */
package deepsea.utilities;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.math.BigInteger;
import java.security.MessageDigest;


/**
 * generate Checksum for a file;
 */
public final class ChecksumFile {

    /*\/ generate the MD5 checksum for a file; */
    public String MD5Checksum(String filePath){
        String checksum = null;
        try{
            byte[] data = Files.readAllBytes(Paths.get(filePath));
            byte[] hash = MessageDigest.getInstance("MD5").digest(data);
            checksum = new BigInteger(1, hash).toString(16);
        }catch(IOException | java.security.NoSuchAlgorithmException e){}
        return checksum;
    }

    /*\/ generate the MD5 checksum for a file; */
    public String MD5Checksum(File file){
        String checksum = null;
        try{
            byte[] data = Files.readAllBytes(file.toPath());
            byte[] hash = MessageDigest.getInstance("MD5").digest(data);
            checksum = new BigInteger(1, hash).toString(16);
        }catch(IOException | java.security.NoSuchAlgorithmException e){}
        return checksum;
    }
}