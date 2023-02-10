package org.dicom.io;

import java.io.OutputStream;

public class LittleEndianBinaryWriter extends BinaryWriter {

	public LittleEndianBinaryWriter(OutputStream outputs) {
		super(outputs);
	}

	@Override
	int endian(int i, int len) {
		return i;
	}

}
