package deepsea.utilities;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
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


/*\/ Apache Commons Compress;
* https://commons.apache.org/proper/commons-compress/
* doc: https://commons.apache.org/proper/commons-compress/apidocs/index.html
*/
/*\/ BZip2:
* bzip2 compresses most files more effectively than the older LZW (.Z) and Deflate (.zip and .gz)
compression algorithms, but is considerably slower. */
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;


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
public final class Compress {

    /*\/ BZip2; */
    public static final String ext = ".bz2";

    /*\/ BZip2; */
    public boolean compress(String arqCompress, String fileFinal) {
        boolean ok = false;
        try{
            Path arqCompPath = Paths.get(arqCompress);
            int size = new Long(Files.size(arqCompPath)).intValue();
            InputStream in = Files.newInputStream(arqCompPath);
            OutputStream fout = Files.newOutputStream(Paths.get(fileFinal));
            BufferedOutputStream out = new BufferedOutputStream(fout);
            BZip2CompressorOutputStream classCompOut = new BZip2CompressorOutputStream(out);
            final byte[] buffer = new byte[size];
            int n = 0;
            while (-1 != (n = in.read(buffer))) {
                classCompOut.write(buffer, 0, n);
            }
            classCompOut.close();
            in.close();
            ok = true;
        }catch(IOException e){}
        return ok;
    }

    /*\/ BZip2; */
    public boolean decompress(String arqCompressed, String fileFinal) {
        boolean ok = false;
        try{
            int size = new Long(Files.size(Paths.get(arqCompressed))).intValue();
            InputStream fin = Files.newInputStream(Paths.get(arqCompressed));
            BufferedInputStream in = new BufferedInputStream(fin);
            OutputStream out = Files.newOutputStream(Paths.get(fileFinal));
            BZip2CompressorInputStream classCompIn = new BZip2CompressorInputStream(in);
            final byte[] buffer = new byte[size];
            int n = 0;
            while (-1 != (n = classCompIn.read(buffer))) {
                out.write(buffer, 0, n);
            }
            out.close();
            classCompIn.close();
            ok = true;
        }catch(IOException e){}
        return ok;
    }

    /*\/ fins de testes; */
    public void printFileSize(File file) {
        if (file.exists()) {
            // size of a file (in bytes)
            long bytes = file.length();
            long kilobytes = (bytes / 1024);
            long megabytes = (kilobytes / 1024);
            long gigabytes = (megabytes / 1024);
            long terabytes = (gigabytes / 1024);
            long petabytes = (terabytes / 1024);
            long exabytes = (petabytes / 1024);
            long zettabytes = (exabytes / 1024);
            long yottabytes = (zettabytes / 1024);

            System.out.println(file.getName() + ":");
            System.out.println(String.format("%,d bytes", bytes));
            System.out.println(String.format("%,d kilobytes", kilobytes));
            System.out.println(String.format("%,d megabytes", megabytes));
            System.out.println(String.format("%,d gigabytes", gigabytes));
            System.out.println(String.format("%,d terabytes", terabytes));
            System.out.println(String.format("%,d petabytes", petabytes));
            System.out.println(String.format("%,d exabytes", exabytes));
            System.out.println(String.format("%,d zettabytes", zettabytes));
            System.out.println(String.format("%,d yottabytes", yottabytes));

        } else {
            System.out.println("File does not exist!");
        }
    }

}
