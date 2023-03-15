/*
 * classe para realizar procedimentos de arquivos(Objetos) no servidor do MinIO.
 */
package deepsea.utilities;

import io.minio.StatObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ListObjectsArgs;
import io.minio.UploadObjectArgs;
import io.minio.DownloadObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * classe para realizar procedimentos de arquivos(Objetos) no servidor do MinIO.
 */
public final class FileOperationsMinio {

    private MinioClient minioClient;
    private final String bucketName = "asiatrip";
    private final String minioEndpoint = "http://localhost:9000";
    private final String minioUser = "minioadmin";
    private final String minioPass = "minioadmin";

    public FileOperationsMinio() {
        try{
            createMinioClient();
        }catch(java.io.IOException | java.security.NoSuchAlgorithmException | java.security.InvalidKeyException e){
            e.printStackTrace();
        }
    }

    public void createMinioClient() throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        try {
            // Create a minioClient with the MinIO server playground, its access key and secret key.
            minioClient = MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioUser, minioPass)
                .build();

            // Make 'asiatrip' bucket if not exist.
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                // Make a new bucket called 'asiatrip'.
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                System.out.println("ERROR. :(");
            } else {
                System.out.println("Bucket 'asiatrip' already exists. ;)");
            }
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
            System.out.println("HTTP trace: " + e.httpTrace());
        }
    }

    public boolean uploadObject(String fileName, String fileToUpload) {
        boolean state = false;
        try{
            minioClient.uploadObject(
                UploadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .filename(fileToUpload)
                    .build());
            state = true;
        }catch(IOException | MinioException | InvalidKeyException | NoSuchAlgorithmException e){
                e.printStackTrace();
        }
        return state;
    }

    public boolean downloadObject(String fileName, String localFileFolder) {
        boolean state = false;
        try{
            String downloadedFile = localFileFolder + java.io.File.separator + fileName; 
            minioClient.downloadObject(
                DownloadObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .filename(downloadedFile)
                .build());
            state = true;
        }catch(IOException | MinioException | InvalidKeyException | NoSuchAlgorithmException e){
                e.printStackTrace();
        }
        return state;
    }

    public boolean removeObject(String fileName) {
        boolean state = false;
        try{
            // Remove object.
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName).
                build());
            state = true;
        }catch(IOException | MinioException | InvalidKeyException | NoSuchAlgorithmException e){
                e.printStackTrace();
        }
        return state;
    }

    public void listObjects() {
        try{
            System.out.println("=====");
            // {
            //     // Lists objects information.
            //     Iterable<Result<Item>> results =
            //         minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build());

            //     for (Result<Item> result : results) {
            //         Item item = result.get();
            //         System.out.println(item.lastModified() + "\t" + item.size() + "\t" + item.objectName());
            //     }
            // }
            // System.out.println("***");
            {
                // Lists objects information recursively.
                Iterable<Result<Item>> results =
                    minioClient.listObjects(
                        ListObjectsArgs.builder().bucket(bucketName).recursive(true).build());

                for (Result<Item> result : results) {
                    Item item = result.get();
                    System.out.println(item.lastModified() + "\t size: " + item.size() + "\t name: " + item.objectName());
                }
                long numero = java.util.stream.StreamSupport.stream(results.spliterator(), false).count();
                System.out.println( "Total : " + numero );
            }
            System.out.println("=====");
        } catch (IOException | MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public long totalObjects() {
        long numero = 0;
        Iterable<Result<Item>> results =
            minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).recursive(true).build());
        numero = java.util.stream.StreamSupport.stream(results.spliterator(), false).count();
        return numero;
    }

    public boolean ifObjectExists(String nameObject) {
        boolean exists = false;
        try{
            // Get information of an object.
            minioClient.statObject(
                StatObjectArgs.builder()
                .bucket(bucketName)
                .object(nameObject).build());
            exists = true;
        } catch (IOException | MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
            exists = false;
        }
        return exists;
    }

}
