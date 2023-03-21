package deepsea.utilities;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

/*\/
* ATENÇÃO PARA COMPACTAÇÃO DE ARQUIVOS DICOM.
* 
* Colégio Americano de Radiologia (American College of Radiology),
* não fornecem recomendações claras para compressão, existem certos
* tipos de estudos de imagem que são proibidos de serem compactados.
* Segundo a Associação Canadense de Radiologistas(Canadian Association of Radiologists),
* dependendo da modalidade usada para a imagem e da parte específica do corpo, a compressão pode ou não ser recomendada.
* 
* Embora ambos sejam permitidos na maioria das circunstâncias -- lossless(reversível) e lossy(irreversível) --,
* nunca recomendamos o uso de lossy compression.
* Esse tipo de compactação realmente causa a perda irreversível dos dados da imagem.
* Há uma série de lossless methods("métodos sem perdas") disponíveis, com os benefícios da
* lossy compression(compactação com perdas) sendo aprimorados apenas marginalmente.
* Portanto, fique com a compactação sem perdas(lossless compression).
*/
public final class ZipUtility {

    public final static String format = ".zip";

    public void zipDirectory(String caminhoPasta, String nomeArquivoCompactar) throws IOException {
        FileOutputStream fos = new FileOutputStream(nomeArquivoCompactar + format);
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
        if(filePathToUnzip.toFile().exists()){
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
            System.out.println("Pasta já existe, ou arquivo .zip não existe.");
        }
    }

    /**
     * Compresses a list of files to a destination zip file
     * @param listFiles A collection of files and directories
     * @param destZipFile The path of the destination zip file
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void zipFiles(File[] listFiles, String destZipFile) throws FileNotFoundException, IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destZipFile));
        for (File file : listFiles) {
            if (!file.isDirectory()) {
                this.zipFile(file, zos);
            }
        }
        zos.flush();
        zos.close();
    }

    /**
     * Adds a file to the current zip output stream
     * @param file the file to be added
     * @param zos the current zip output stream
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void zipFile(File file, ZipOutputStream zos) throws FileNotFoundException, IOException {
        zos.putNextEntry(new ZipEntry(file.getName()));
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(
                file));
        this.InputStreamToOutputStream(bis, zos);
        zos.closeEntry();
    }

    public boolean zipFile(File file, String destZipFile) {
        boolean ok = false;
        try{
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destZipFile));
            if (!file.isDirectory()) {
                this.zipFile(file, zos);
            }
            zos.flush();
            zos.close();
            ok = true;
        }catch(IOException e){
            e.printStackTrace();
        }
        return ok;
    }

}