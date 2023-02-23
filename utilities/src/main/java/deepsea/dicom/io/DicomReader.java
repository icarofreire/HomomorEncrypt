package org.dicom.io;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.dicom.core.DataElement;
import org.dicom.core.DataSet;
import org.dicom.core.Tag;
import org.dicom.core.Tags;
import org.dicom.core.VRType;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

/*
* Doc DICOM Standard:
* https://dicom.nema.org/dicom/2013/output/chtml/part05/chapter_7.html
*/

public abstract class DicomReader implements Closeable {

	// Logger logger = LoggerFactory.getLogger(this.getClass());

	DicomReader(BinaryReader binaryReader) {
		this.binaryReader = binaryReader;
	}

	BinaryReader binaryReader;
	String charset = "utf-8";

	public void close() throws IOException {
		binaryReader.close();
	}

	public List<Map<Tag, DataElement>> read() throws IOException {
		binaryReader.readBytes(128 + 4);
		List<Map<Tag, DataElement>> listElems = new ArrayList<>();
		try {
			while (true) {
				Map<Tag, DataElement> elems = new TreeMap<>();
				DataElement elem = readElement();

				VRType type = VRType.of(elem.getVR().trim());
				if (type != null) {
					elems.put(elem.getTag(), elem);
					listElems.add(elems);
				}
			}
		} catch (IOException ex) {
			// logger.info("complete to read");
			// System.out.println("*** complete to read!!!");
			return listElems;
		}
	}

	protected Object readValue(String VR, int len) throws IOException {
		VRType type = VRType.of(VR);
		if (type == null) {
			// logger.info("unknown VR {}", VR);
			type = VRType.Bytes;
		}
		switch (type) {
		case String:
			return new String(binaryReader.readBytes(len));
		case Text:
			return new String(binaryReader.readBytes(len), 0, len, charset);
		case Short:
			return binaryReader.readShort();
		case UShort:
			return binaryReader.readShort() & 0X0000FFFF;
		case Int:
			return binaryReader.readInt();
		case UInt:
			return binaryReader.readInt() & 0XFFFFFFFFL;
		case Float:
			return binaryReader.readFloat();
		case Double:
			return binaryReader.readDouble();
		case Bytes:
			return binaryReader.readBytes(len);
		case Shorts:
			int size = len / BinaryReader.SIZE_OF_SHORT;
			short[] ws = new short[size];
			for (int i = 0; i < size; i++) {
				ws[i] = binaryReader.readShort();
			}
			return ws;
		case Ints:
			size = len / BinaryReader.SIZE_OF_INT;
			int[] ints = new int[size];
			for (int i = 0; i < size; i++) {
				ints[i] = binaryReader.readInt();
			}
			return ints;
		case Floats:
			size = len / BinaryReader.SIZE_OF_INT;
			float[] fs = new float[size];
			for (int i = 0; i < size; i++) {
				fs[i] = binaryReader.readFloat();
			}
			return fs;
		case Doubles:
			size = len / BinaryReader.SIZE_OF_LONG;
			double[] ds = new double[size];
			for (int i = 0; i < size; i++) {
				ds[i] = binaryReader.readDouble();
			}
			return ds;
		default:
			return null;
		}
	}

	protected Tag readTag() throws IOException {
		short groupId = binaryReader.readShort();
		short elementNumber = binaryReader.readShort();
		Tag tag = new Tag(groupId & 0X0000FFFF, elementNumber & 0X0000FFFF);
		return tag;
	}

	protected abstract String readVR() throws IOException;

	protected abstract int readLength(String VR) throws IOException;

	private DataElement readElement() throws IOException {
		// logger.info("readElement");
		Tag tag = readTag();
		// if (Tags.SQ_END_TAG.equals(tag) || Tags.ITEM_TAG.equals(tag) || Tags.ITEM_END_TAG.equals(tag)) {
		// 	int len = binaryReader.readInt();
		// 	// logger.info("{} {}", tag, len);
		// 	return new DataElement(tag, null, null, len);
		// }
		String vr = readVR();
		int len = readLength(vr);
		// logger.info("{} {} {}", tag, vr, len);
		Object value = null;
		if (!vr.equals("SQ")) {
			value = readValue(vr, len);
		} else {
			// System.out.println("***");
			// List<Map<Tag, DataElement>> value2;
			// if (len < 0) {
			// 	value2 = readNestedDataSetWithoutLen();
			// } else {
			// 	value2 = readNestedDataSetWithLen(len);
			// }
			// DataSet dataSet = new DataSet(tag, -1);
			// dataSet.getElems().addAll(value2);
			// return dataSet;
		}
		// System.out.println( Integer.toHexString(tag.getGroupId()) + "," + Integer.toHexString(tag.getElementNumber()) + " >> " + value + " -> " + " VR: " + vr + " len: " + len );
		return new DataElement(tag, vr, value, len);
	}

	private List<Map<Tag, DataElement>> readNestedDataSetWithoutLen() throws IOException {
		// logger.info("nest SQ begin");
		List<Map<Tag, DataElement>> dataSet = new ArrayList<>();
		DataElement elem = readElement();
		while (elem.getTag().equals(Tags.ITEM_TAG)) {
			// logger.info("nest item");
			if (elem.getLen() > 0) {
				dataSet.add(readGroup(elem.getLen()));
			} else {
				Map<Tag, DataElement> group = new TreeMap<>();
				DataElement inner = readElement();
				while (!inner.getTag().equals(Tags.ITEM_END_TAG)) {
					group.put(inner.getTag(), inner);
					inner = readElement();
				}
				assert inner.getTag().equals(Tags.ITEM_END_TAG);
				dataSet.add(group);
			}
			elem = readElement();
		}
		assert elem.getTag().equals(Tags.SQ_END_TAG);
		// logger.info("nest SQ end");
		return dataSet;
	}

	private Map<Tag, DataElement> readGroup(int len) throws IOException {
		long begin = binaryReader.offset();
		Map<Tag, DataElement> elems = new TreeMap<>();
		while (binaryReader.offset() - begin < len) {
			DataElement elem = readElement();
			elems.put(elem.getTag(), elem);
		}
		assert binaryReader.offset() - begin == 0;
		return elems;
	}

	private List<Map<Tag, DataElement>> readNestedDataSetWithLen(int len) throws IOException {
		long begin = binaryReader.offset();
		List<Map<Tag, DataElement>> dataSet = new ArrayList<>();
		DataElement elem = readElement();
		while (binaryReader.offset() - begin < len) {
			if (elem.getLen() > 0) {
				Map<Tag, DataElement> group = readGroup(elem.getLen());
				dataSet.add(group);
			} else {
				Map<Tag, DataElement> group = new TreeMap<>();
				DataElement inner = readElement();
				while (!inner.getTag().equals(Tags.ITEM_END_TAG)) {
					group.put(inner.getTag(), inner);
					inner = readElement();
				}
				assert inner.getTag().equals(Tags.ITEM_END_TAG);
				dataSet.add(group);
			}
		}
		assert binaryReader.offset() - begin == 0;
		return dataSet;
	}
}
