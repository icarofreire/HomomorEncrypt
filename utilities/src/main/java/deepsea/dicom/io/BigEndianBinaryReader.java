package org.dicom.io;

import java.io.InputStream;

public class BigEndianBinaryReader extends BinaryReader {

	public BigEndianBinaryReader(InputStream inputs) {
		super(inputs);
	}

	@Override
	int endian(int i, int len) {
		return i;
	}

}
