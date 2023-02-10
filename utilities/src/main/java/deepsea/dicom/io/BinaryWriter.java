package org.dicom.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public abstract class BinaryWriter implements Closeable {

	public static final int SIZE_OF_LONG = 8;
	public static final int SIZE_OF_INT = 4;
	public static final int SIZE_OF_SHORT = 2;

	public BinaryWriter(OutputStream outputs) {
		this.outputs = outputs;
	}

	private OutputStream outputs;

	@Override
	public void close() throws IOException {
		outputs.close();
	}

	public void writeByte(byte value) throws IOException {
		outputs.write(value);
	}

	public void writeShort(short value) throws IOException {
		write(value, SIZE_OF_SHORT);
	}

	public void writeInt(int value) throws IOException {
		write(value, SIZE_OF_INT);
	}

	public void writeLong(long value) throws IOException {
		write(value, SIZE_OF_LONG);
	}

	public void writeFloat(float value) throws IOException {
		int bits = Float.floatToIntBits(value);
		writeInt(bits);
	}

	public void writeDouble(double value) throws IOException {
		long bits = Double.doubleToLongBits(value);
		writeLong(bits);
	}

	abstract int endian(int i, int len);

	private void write(long value, int size) throws IOException {
		for (int i = 0; i < size; i++) {
			int idx = endian(i, size);
			buf[idx] = (byte) value;
			value >>>= 8;
		}
		outputs.write(buf, 0, size);
	}

	private byte[] buf = new byte[SIZE_OF_LONG];
}
