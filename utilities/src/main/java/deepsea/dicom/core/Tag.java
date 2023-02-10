package org.dicom.core;

public class Tag implements Comparable<Tag> {

	public Tag(int groupId, int elementNumber) {
		this.groupId = groupId;
		this.elementNumber = elementNumber;
	}

	private int groupId;
	private int elementNumber;

	@Override
	public String toString() {
		return String.format("(%04x, %04x)", groupId, elementNumber);
	}

	@Override
	public int hashCode() {
		return groupId & 0X0000FFFF << 16 + elementNumber;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Tag))
			return false;
		Tag a = (Tag) obj;
		return a.groupId == groupId && a.elementNumber == elementNumber;
	}

	public int getGroupId() {
		return groupId;
	}

	public int getElementNumber() {
		return elementNumber;
	}

	@Override
	public int compareTo(Tag o) {
		return this.hashCode() - o.hashCode();
	}
}
