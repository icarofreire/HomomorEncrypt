package org.dicom.io;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public abstract class BinaryReader implements Closeable {

	public static final int SIZE_OF_LONG = 8;
	public static final int SIZE_OF_INT = 4;
	public static final int SIZE_OF_SHORT = 2;

	public BinaryReader(InputStream inputs) {
		this.inputs = inputs;
	}

	InputStream inputs;
	int offset = 0;

	@Override
	public void close() throws IOException {
		inputs.close();
	}

	public long offset() {
		return offset;
	}

	public byte readByte() throws IOException {
		int b = inputs.read();
		if (b == -1) {
			throw new EOFException();
		}
		offset++;
		return (byte) b;
	}

	public byte[] readBytes(int len) throws IOException {
		if(len < 0){
			len = -len;
		}
		byte[] b = new byte[len];
		int len2 = inputs.read(b, 0, len);
		offset += len2;
		if (len != len2) {
			throw new EOFException();
		}
		return b;
	}

	public short readShort() throws IOException {
		return (short) read(SIZE_OF_SHORT);
	}

	public int readInt() throws IOException {
		return (int) read(SIZE_OF_INT);
	}

	public long readLong() throws IOException {
		return read(SIZE_OF_LONG);
	}

	public float readFloat() throws IOException {
		int bits = readInt();
		return Float.intBitsToFloat(bits);
	}

	public double readDouble() throws IOException {
		long bits = readLong();
		return Double.longBitsToDouble(bits);
	}

	abstract int endian(int i, int len);

	private long read(int size) throws IOException {
		assert size <= SIZE_OF_LONG;
		int len = inputs.read(buf, 0, size);
		offset += len;
		if (len == -1) {
			throw new EOFException();
		}
		long value = 0;
		for (int i = 0; i < len; i++) {
			value <<= 8;
			int idx = endian(i, len);
			value += Byte.toUnsignedLong(buf[idx]);
		}
		return value;
	}

	private byte[] buf = new byte[SIZE_OF_LONG];
}
