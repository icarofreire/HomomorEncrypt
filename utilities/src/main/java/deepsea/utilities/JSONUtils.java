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
import com.google.gson.reflect.TypeToken;

/**
 * Ex:
 * https://howtodoinjava.com/java/library/json-simple-read-write-json-examples/
 */
public final class JSONUtils {

    private HashMap<String,HashMap<String,Object>> semanticTags = new HashMap<String,HashMap<String,Object>>();

    public HashMap<String,HashMap<String,Object>> getMapaJson() {
        return semanticTags;
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
            semanticTags = gson.fromJson(reader, new TypeToken<HashMap<String,HashMap<String,Object>>>() {}.getType());

            // close reader
            reader.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}