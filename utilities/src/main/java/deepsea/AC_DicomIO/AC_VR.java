package AC_DicomIO;

import java.util.HashMap;

public class AC_VR {
	
	public static final int AE=0x4145, AS=0x4153, AT=0x4154, CS=0x4353, DA=0x4441, DS=0x4453, DT=0x4454,
			FD=0x4644, FL=0x464C, IS=0x4953, LO=0x4C4F, LT=0x4C54, PN=0x504E, SH=0x5348, SL=0x534C, 
			SS=0x5353, ST=0x5354, TM=0x544D, UI=0x5549, UL=0x554C, US=0x5553, UT=0x5554,
			OB=0x4F42, OW=0x4F57, SQ=0x5351, UN=0x554E, OD = 0x4F44, OF = 0x4F46,QQ=0x3F3F, Undefined=0x2D2D;
	
	
	public static final int 
	AE_MAXLEN=16, 
	AS_MAXLEN=4, 
	AT_MAXLEN=4, 
	CS_MAXLEN=16,
	DA_MAXLEN=8,
	DS_MAXLEN=16, 
	DT_MAXLEN=26,
	FL_MAXLEN=4, 
	FD_MAXLEN=4, 
	IS_MAXLEN=16, 
	LO_MAXLEN=64,
	LT_MAXLEN=10240,
	OB_MAXLEN=123,
	OD_MAXLEN=32,//(32-8)
	OF_MAXLEN=32,//(32-4)
	OW_MAXLEN=123, 
	PN_MAXLEN=64, 
	SH_MAXLEN=16,
	SL_MAXLEN=4, 
	SQ_MAXLEN=-1, 
	SS_MAXLEN=2,
	ST_MAXLEN=1024,
	TM_MAXLEN=16,
	UI_MAXLEN=64, 
	UL_MAXLEN=4,
	US_MAXLEN=2,
	UT_MAXLEN=32,//(32-2)


	UN_MAXLEN=123, 
	QQ_MAXLEN=0x3F3F;
	
	private static HashMap<Integer,String> VRNameMap = new HashMap<Integer, String>();
	static {
		VRNameMap.put(AE ,"AE");
		VRNameMap.put(AS ,"AS");
		VRNameMap.put(AT ,"AT");
		VRNameMap.put(CS ,"CS");
		VRNameMap.put(DA ,"DA");
		VRNameMap.put(DS ,"DS");
		VRNameMap.put(DT ,"DT");
		VRNameMap.put(FD ,"FD");
		VRNameMap.put(FL ,"FL");
		VRNameMap.put(IS ,"IS");
		VRNameMap.put(LO ,"LO");
		VRNameMap.put(LT ,"LT");
		VRNameMap.put(PN ,"PN");
		VRNameMap.put(SH ,"SH");
		VRNameMap.put(SL ,"SL");
		VRNameMap.put(SS ,"SS");
		VRNameMap.put(ST ,"ST");
		VRNameMap.put(TM ,"TM");
		VRNameMap.put(UI ,"UI");
		VRNameMap.put(UL ,"UL");
		VRNameMap.put(US ,"US");
		VRNameMap.put(UT ,"UT");
		VRNameMap.put(OB ,"OB");
		VRNameMap.put(OW ,"OW");
		VRNameMap.put(SQ ,"SQ");
		VRNameMap.put(UN ,"UN");
		VRNameMap.put(QQ ,"QQ");
	}
	
	
	public static String getVRName(int VR)
	{
		if(VRNameMap.containsKey(VR))
			return VRNameMap.get(VR);
		return "";
	}
	


}
