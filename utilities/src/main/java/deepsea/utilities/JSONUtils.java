/*
 * 
 */
package deepsea.utilities;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.File;
import java.io.IOException;
import java.io.Reader;

/* \/ Gson; */
import com.google.gson.Gson;

/**
 * Ex:
 * https://howtodoinjava.com/java/library/json-simple-read-write-json-examples/
 */
public final class JSONUtils {

    private HashMap<String, Object> mapa = new HashMap<String, Object>();

    public HashMap<String, Object> getMapaJson() {
        return mapa;
    }

    public void readJSON() {

        try {
            // create Gson instance
            Gson gson = new Gson();

            /*\/ /home/USUARIO-PC/Documentos/DeepSea/app/... */
            Path resourceDirectory = Paths.get("..", "utilities", "src", "main", "java", "deepsea", "dicom", "io", "tags-dicom.json");

            // create a reader
            Reader reader = Files.newBufferedReader(resourceDirectory);

            // convert JSON file to map
            Map<?, ?> map = gson.fromJson(reader, Map.class);
            mapa = (HashMap<String, Object>)map;

            // // print map entries
            // for (Map.Entry<?, ?> entry : map.entrySet()) {
            //     System.out.println(entry.getKey() + " --> " + entry.getValue());
            // }

            // close reader
            reader.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}