package org.dicom.io;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DicomExplicitReader extends DicomReader {

	public DicomExplicitReader(BinaryReader binaryReader) {
		super(binaryReader);
	}

	private List<String> veryLongVRs = Arrays.asList("OB", "OD", "OF", "OL", "OW", "SQ", "UC", "UR", "UT", "UN");

	private boolean isVeryLongValue(String VR) {
		return veryLongVRs.contains(VR);
	}

	@Override
	protected String readVR() throws IOException {
		return new String(binaryReader.readBytes(2));
	}

	@Override
	protected int readLength(String VR) throws IOException {
		if (isVeryLongValue(VR)) {
			binaryReader.readShort();
			return binaryReader.readInt();
		} else {
			return binaryReader.readShort();
		}
	}

}