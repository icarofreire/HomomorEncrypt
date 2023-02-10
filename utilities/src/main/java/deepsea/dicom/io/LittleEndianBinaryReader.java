package org.dicom.io;

import java.io.InputStream;

public class LittleEndianBinaryReader extends BinaryReader {

	public LittleEndianBinaryReader(InputStream inputs) {
		super(inputs);
	}

	@Override
	int endian(int i, int len) {
		return len - i - 1;
	}

}
