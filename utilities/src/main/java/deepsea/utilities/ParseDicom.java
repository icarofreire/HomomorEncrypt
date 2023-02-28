/*
 * Classe para interpretar arquivos DICOM;
 */
package deepsea.utilities;

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
 * Classe para interpretar arquivos DICOM;
 */
public final class ParseDicom {

	private HashMap<String, String> tagsValues = new HashMap<String, String>();

	private final List<Integer> listTags = java.util.Arrays.asList(
		0x0010, 0x0020,
		0x0010, 0x0010,
		0x0010, 0x1010,
		0x0010, 0x1020,
		0x0010, 0x0030,
		0x0010, 0x0040,
		0x0010, 0x1040,
		0x0018, 0x5100,
		0x0020, 0x0010,
		0x0008, 0x1030,
		0x0018, 0x0015,
		0x0008, 0x0020,
		0x0008, 0x0080,
		0x0008, 0x0081,
		0x0054, 0x0400
	);

	public boolean seContainTags(List<Integer> listTags, int grup, int ele){
		for(int i=0; i<listTags.size(); i++){
			if((i+1) < listTags.size()){
				int par1 = Integer.decode(String.valueOf(listTags.get(i)));
				int par2 = Integer.decode(String.valueOf(listTags.get(i+1)));
				if(par1 == grup && par2 == ele){
					return true;
				}
			}
		}
		return false;
	}

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
			// HashMap<String,String> values = shortInfoDicom(outs);
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
				String procedimentoTag = ((semanticTags.get(tags) != null) ? (String.valueOf(semanticTags.get(tags).get("name"))) : (tags));
				tagsValues.put(procedimentoTag, String.valueOf(v.getValue()));
			});
		}
	}

	private HashMap<String,String> shortInfoDicom(List<Map<Tag, DataElement>> outs){
		HashMap<String,String> semanticTags = new HashMap<String,String>();
		for(Map<Tag, DataElement> elem: outs){
			elem.forEach((k, v) -> {
				String grupoId = insertZeros(Integer.toHexString(k.getGroupId()));
				String elementNumber = insertZeros(Integer.toHexString(k.getElementNumber()));
				String tags = "(" + grupoId + "," + elementNumber + ")";
				boolean setags =
				seContainTags(listTags,
					k.getGroupId(),
					k.getElementNumber()
				);
				if(setags){
					semanticTags.put(tags, String.valueOf(v.getValue()));
					// System.out.println( tags + " : " +  String.valueOf(v.getValue()) + " -> " + " C:" + k.getGroupId() + ":" + k.getElementNumber() );
				}
			});
		}
		return semanticTags;
	}

}