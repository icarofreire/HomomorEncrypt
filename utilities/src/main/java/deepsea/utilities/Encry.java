package deepsea.utilities;


import java.util.*;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Vector;
import java.util.stream.Stream;

/**
 * Service class for Encryption and Decryption.
 */
/**
 * Ex:
    Encry encry = new Encry();
    System.out.println( encry.encrypt("teste") );
    System.out.println( encry.decrypt(encry.encrypt("teste")) );

    encry.pbytes("teste".getBytes());
    encry.pbytes(encry.bytesEncrypt("teste"));
    encry.pbytes(encry.bytesDecrypt(encry.bytesEncrypt("teste")));
 */
public class Encry {

    private final String algorithm = "DES";
    private KeyGenerator Mygenerator;
    private SecretKey key;
    private final String secret = "987654321";

    // private Cipher cipher;

    // public Encry() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException {
    //     //Generating Key
    //     this.Mygenerator = KeyGenerator.getInstance(algorithm);
    //     this.key = Mygenerator.generateKey();
    //     // this.cipher = Cipher.getInstance(algorithm);
    // }

    public Encry() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        // If you want to use your own key
        SecretKeyFactory MyKeyFactory = SecretKeyFactory.getInstance(algorithm);
        // String Password = "987654321";
        byte[] mybyte = secret.getBytes();
        DESKeySpec myMaterial = new DESKeySpec(mybyte);
        this.key = MyKeyFactory.generateSecret(myMaterial);
    }

    public String encrypt(String input) throws
        NoSuchPaddingException,
        NoSuchAlgorithmException,
        InvalidKeyException,
        BadPaddingException,
        IllegalBlockSizeException {
        
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, this.key);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getEncoder()
            .encodeToString(cipherText);
    }

    public String decrypt(String cipherText) throws
        NoSuchPaddingException,
        NoSuchAlgorithmException,
        InvalidKeyException,
        BadPaddingException,
        IllegalBlockSizeException {
        
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, this.key);
        byte[] plainText = cipher.doFinal(Base64.getDecoder()
            .decode(cipherText));
        return new String(plainText);
    }

    public byte[] bytesEncrypt(String input) throws
        NoSuchPaddingException,
        NoSuchAlgorithmException,
        InvalidKeyException,
        BadPaddingException,
        IllegalBlockSizeException {
        
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, this.key);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return cipherText;
    }

    public byte[] bytesDecrypt(byte[] cipherText) throws
        NoSuchPaddingException,
        NoSuchAlgorithmException,
        InvalidKeyException,
        BadPaddingException,
        IllegalBlockSizeException {
        
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, this.key);
        byte[] plainText = cipher.doFinal(cipherText);
        return plainText;
    }

    // print message in byte format
    public void pbytes(byte[] bytes){
        System.out.println("*** bytes *****");
        System.out.println(Arrays.toString(bytes));
    }

    public class HomoEncry {
        private int byteEncrylength = 0;

        // function to merge two arrays  
        public byte[] mergeArray(byte[] arr1, byte[] arr2)
        {
            int length = arr1.length + arr2.length;
            byte[] mergedArray = new byte[length];
            int pos = 0;  
            for (byte element : arr1) {
                mergedArray[pos] = element;
                pos++;
            }
            for (byte element : arr2) {  
                mergedArray[pos] = element;
                pos++;
            }
            return mergedArray;
        }

        // public int indexOfSubArray(byte[] haystack, byte[] needle) {
        //     int ind = -1;
        //     if(haystack.length > needle.length){
        //         int count = 0;
        //         for (int i = 0; i <= haystack.length - needle.length; ++i) {
        //             for (int j = 0; j < needle.length; ++j) {
        //                 if (haystack[i + j] == needle[j]) {
        //                     count++;
        //                     if(ind == -1) ind = i+j;
        //                 }else{
        //                     if(count < needle.length){
        //                         ind = -1;
        //                     }
        //                     break;
        //                 }
        //             }
        //         }
        //         if(count == needle.length){
        //             return ind;
        //         }
        //     }
        //     return ind;
        // }

        public String encodeCipherText(byte[] cipherText){
            return Base64.getEncoder().encodeToString(cipherText);
        }

        public byte[] getBytesBase64(String cipherText){
            return Base64.getDecoder().decode(cipherText);
        }

        public String encryptHomo(String input) throws
            NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException
            {
            byte[] sumCry = new byte[0];
            for(int i=0; i<input.length(); i++){
                String s = String.valueOf(input.charAt(i));
                byte[] bytesEncry = bytesEncrypt(s);
                this.byteEncrylength = bytesEncry.length;
                sumCry = mergeArray(sumCry, bytesEncry);
            }
            return encodeCipherText(sumCry);
        }

        public String decryptHomo(String cipherText) throws
            NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException
            {
            String strDec = "";
            byte[] bytesCiphered = getBytesBase64(cipherText);
            Vector<byte[]> lbytes = splitArray(bytesCiphered, 8);
            for(byte[] bytes : lbytes){
                byte[] bytesDecrypted = bytesDecrypt(bytes);
                strDec += new String(bytesDecrypted);
            }

            return strDec;
        }

        public Vector<byte[]> splitArray(byte[] array, int max){
            int x = array.length / max;
            int r = (array.length % max); // remainder
            int lower = 0;
            int upper = 0;

            Vector<byte[]> vec = new Vector<byte[]>();

            for(int i=0; i<x; i++){
                upper += max;
                vec.add(Arrays.copyOfRange(array, lower, upper));
                lower = upper;
            }
            if(r > 0){
                vec.add(Arrays.copyOfRange(array, lower, (lower + r)));
            }
            return vec;
        }

        public boolean searchInHomo(String inputTextoPuro, String cipherText) throws
            NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException
        {
            String bytesCiphed = encryptHomo(inputTextoPuro);
            bytesCiphed = bytesCiphed.substring(0, bytesCiphed.length()-3);
            return (cipherText.indexOf(bytesCiphed) != -1);
        }
    }
}
