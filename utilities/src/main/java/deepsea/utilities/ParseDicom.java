/*
 * A simple SFTP client using JSCH http://www.jcraft.com/jsch/
 */
package deepsea.utilities;

// import org.dcm4che3.data.Attributes;
// import org.dcm4che3.io.DicomInputStream;
// import org.dcm4che3.util.TagUtils;    
import java.io.File;
import java.io.IOException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.dicom.core.DataElement;
import org.dicom.core.Tag;
import org.dicom.io.*;

import deepsea.utilities.JSONUtils;

/**
 * A simple SFTP client using JSCH http://www.jcraft.com/jsch/
 */
public final class ParseDicom {

	private HashMap<String, String> tagsValues = new HashMap<String, String>();

	public HashMap<String, String> getTagsValues(){
		return tagsValues;
	}

	public void exibirValoresDICOM() {
		this.tagsValues.forEach((k, v) -> {
            System.out.println(k + " : " + v);
        });
	}

	private String insertZeros(String number){
		if(number.length() < 3){
			int res = 4 - number.length();
			String zeros = "";
			for(int i=0; i< res; i++){
				zeros += "0";
			}
			number = zeros + number;
		}
		return number;
	}

    public void lerDicom(String caminhoArquivoDICOM) throws IOException, ClassNotFoundException {
        File dicom = Path.of(caminhoArquivoDICOM).toFile();
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(dicom));

		try (DicomReader target = new DicomExplicitReader(new LittleEndianBinaryReader(bis))) {
			List<Map<Tag, DataElement>> outs = target.read();
			associarSemanticaTags(outs);
		}
	}

	private void associarSemanticaTags(List<Map<Tag, DataElement>> outs){
		JSONUtils jsonTags = new JSONUtils();
        jsonTags.readJSON();
		HashMap<String,HashMap<String,Object>> semanticTags = jsonTags.getMapaJson();

		for(Map<Tag, DataElement> elem: outs){
			elem.forEach((k, v) -> {
				String grupoId = insertZeros(Integer.toHexString(k.getGroupId()));
				String elementNumber = insertZeros(Integer.toHexString(k.getElementNumber()));

				String tags = "(" + grupoId + "," + elementNumber + ")";
				String procedimentoTag = ((semanticTags.get(tags) != null) ? (String.valueOf(semanticTags.get(tags).get("name"))) : (""));
				tagsValues.put(procedimentoTag, String.valueOf(v.getValue()));
				// System.out.println( tags + " >> " + v.getValue() + " : " + procedimentoTag );
			});
		}
	}

}