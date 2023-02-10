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

    public void lerDicom(String caminhoArquivoDICOM) throws IOException, ClassNotFoundException {
        File dicom = Path.of(caminhoArquivoDICOM).toFile();
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(dicom));

		JSONUtils j = new JSONUtils();
        j.readJSON();
		HashMap<String, Object> mapa = j.getMapaJson();

		try (DicomReader target = new DicomExplicitReader(new LittleEndianBinaryReader(
				bis
                ))) {
			List<Map<Tag, DataElement>> outs = target.read();
			for(Map<Tag, DataElement> elem: outs){
				elem.forEach((k, v) -> {
					String grupoId = Integer.toHexString(k.getGroupId());

					// if(grupoId.length() < 3)
					// {
					// 	int res = 4 - grupoId.length();
					// 	String zeros = "";
					// 	for(int i=0; i< res; i++){
					// 		zeros += "0";
					// 	}
					// 	grupoId = zeros + grupoId;
					// }

					System.out.println( grupoId + "," + Integer.toHexString(k.getElementNumber()) + " >> " + v.getValue() );
				});
			}
		}
	}

}