package org.dicom.core;

import java.util.HashMap;
import java.util.Map;

public enum VRType {
	String, Text, Short, UShort, Int, UInt, Float, Double, SQ, Bytes, Doubles, Floats, Ints, Shorts;

	static Map<String, VRType> entries = new HashMap<String, VRType>() {
		{
			put("AE", VRType.String);// 16 bytes maximum
			put("AS", VRType.String);// 4 bytes fixed
			put("AT", VRType.Shorts);// Tag, 4 bytes fixed
			put("CS", VRType.String);// 16 bytes maximum
			put("DA", VRType.String);// 18 bytes maximum
			put("DS", VRType.String);// 16 bytes maximum
			put("DT", VRType.String);// YYYYMMDDHHMMSS.FFFFFF&ZZXX
			put("FL", VRType.Float);
			put("FD", VRType.Double);
			put("IS", VRType.String);
			put("LO", VRType.Text);
			put("LT", VRType.Text);
			put("OB", VRType.Bytes);
			put("OD", VRType.Doubles);
			put("OF", VRType.Floats);
			put("OL", VRType.Ints);
			put("OV", VRType.Shorts);
			put("OW", VRType.Shorts);
			put("PN", VRType.Text);
			put("SH", VRType.Text);
			put("SL", VRType.Int);
			put("SQ", VRType.SQ);
			put("SS", VRType.Short);
			put("ST", VRType.Text);
			put("SV", VRType.Int);
			put("TM", VRType.String);
			put("UC", VRType.Text);
			put("UI", VRType.String);
			put("UL", VRType.UInt);
			put("UN", VRType.Bytes);
			put("UR", VRType.String);
			put("US", VRType.UShort);
			put("UT", VRType.Text);
			put("UV", VRType.UShort);
		}
		private static final long serialVersionUID = 3100410606968637651L;
	};

	public static VRType of(String VR) {
		return entries.get(VR);
	}
}
