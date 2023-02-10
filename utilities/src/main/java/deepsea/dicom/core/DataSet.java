package org.dicom.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataSet extends DataElement {

	@SuppressWarnings("unchecked")
	public DataSet(Tag tag, int len) {
		super(tag, "SQ", new ArrayList<Map<Tag, DataElement>>(), len);
		setElems((ArrayList<Map<Tag, DataElement>>) super.getValue());
	}

	public List<Map<Tag, DataElement>> getElems() {
		return elems;
	}

	private void setElems(List<Map<Tag, DataElement>> elems) {
		this.elems = elems;
	}

	private List<Map<Tag, DataElement>> elems;

}
