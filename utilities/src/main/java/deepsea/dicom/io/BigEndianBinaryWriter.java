package org.dicom.io;

import java.io.OutputStream;

public class BigEndianBinaryWriter extends BinaryWriter {

	public BigEndianBinaryWriter(OutputStream outputs) {
		super(outputs);
	}

	@Override
	int endian(int i, int len) {
		return len - i - 1;
	}

}
