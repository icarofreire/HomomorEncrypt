/*
 * A simple SFTP client using JSCH http://www.jcraft.com/jsch/
 */
package deepsea.utilities;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;


public final class CompactarArquivosZip {


    public void zipDirectory(String caminhoPasta, String nomeArquivoCompactar) throws IOException {
        FileOutputStream fos = new FileOutputStream(nomeArquivoCompactar + ".zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(caminhoPasta);

        if(fileToZip.exists()){
                this.zipDirectory(fileToZip, fileToZip.getName(), zipOut);
        }else{
            System.out.println("Arquivo não existe!");
        }
        zipOut.close();
        fos.close();
    }

    private <T extends InputStream, OUT extends OutputStream> void InputStreamToOutputStream(T in, OUT out) throws IOException {
        in.transferTo(out);
    }

    private void zipDirectory(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        if(folder.exists()){
            for (File file : folder.listFiles()) {
                if (file.isDirectory()) {
                    zipDirectory(file, parentFolder + File.separator + file.getName(), zos);
                    continue;
                }
                zos.putNextEntry(new ZipEntry(parentFolder + File.separator + file.getName()));
                BufferedInputStream bis = new BufferedInputStream(
                        new FileInputStream(file));
                this.InputStreamToOutputStream(bis, zos);
                zos.closeEntry();
            }
        }
    }

    public void unzipFile(String fileZip, String pastaDesc) throws FileNotFoundException {
        Path filePathToUnzip = Path.of(fileZip);
        Path targetDir = Paths.get(pastaDesc);
        if(!targetDir.toFile().exists()){
            //Open the file
            try (ZipFile zip = new ZipFile(filePathToUnzip.toFile())) {

                FileSystem fileSystem = FileSystems.getDefault();
                java.util.Enumeration<? extends ZipEntry> entries = zip.entries();

                //We will unzip files in this folder
                if (!targetDir.toFile().isDirectory()
                    && !targetDir.toFile().mkdirs()) {
                    throw new IOException("failed to create directory " + targetDir);
                }

                //Iterate over entries
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    File f = new File(targetDir.resolve(Path.of(entry.getName())).toString());

                    //If directory then create a new directory in uncompressed folder
                    if (entry.isDirectory()) {
                        if (!f.isDirectory() && !f.mkdirs()) {
                            throw new IOException("failed to create directory " + f);
                        }
                    }else {
                        File parent = f.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("failed to create directory " + parent);
                        }

                        try(InputStream in = zip.getInputStream(entry)) {
                            Files.copy(in, f.toPath());
                        }
                    }
                }
            } catch (IOException e) { e.printStackTrace(); }
        }else{
            System.out.println("Pasta já existe!");
        }
    }

}