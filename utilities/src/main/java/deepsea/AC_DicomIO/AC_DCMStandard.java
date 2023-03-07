package AC_DicomIO;

public class AC_DCMStandard {
	
	public static final byte[] BYTES_SQ_STANDARD_TWOITEM = {0x00,0x00,0x00,0x00, // Tag
		 (byte) 0xFF,(byte) 0xFF, (byte) 0xFF, (byte) 0xFF  };
	
	public static final byte[] BYTES_SQ_STANDARD_THREEITEM = {0x00,0x00,0x00,0x00, // Tag
			0x53,0x51,//VR - SQ
			0x00, 0x00, (byte) 0xFF,(byte) 0xFF, (byte) 0xFF, (byte) 0xFF  };
	

	
	public static final byte[] BYTES_ITEM_STANDARD_TWOITEM_LITTLE = {(byte)0xFE,(byte) 0xFF, 0x00,(byte) 0xE0, 
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF , (byte) 0xFF};

	
	public static final byte[] BYTES_ITEM_STANDARD_TWOITEM_BIG =  {(byte) 0xFF,(byte)0xFE, (byte) 0xE0, 0x00,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF , (byte) 0xFF};
	
	public static final byte[] BYTES_ITEM_DElIMITATION =  {(byte)0xFE,(byte) 0xFF, 0x0D,(byte) 0xE0, 
			0x00,0x00,0x00,0x00};
	
	public static final byte[] BYTES_SQ_DElIMITATION =  {(byte)0xFE,(byte) 0xFF, (byte) 0xDD,(byte) 0xE0, 
			0x00,0x00,0x00,0x00};
	
	
	public static final byte[] BYTES_DCMI =  {(byte)0xFE,(byte) 0xFF, (byte) 0xDD,(byte) 0xE0, 
			0x00,0x00,0x00,0x00};
	
	public static final int DCMI_LEN = 128;
	
	public static final int SQ_TYPE_A = 1;//Treeitem
	public static final int SQ_TYPE_B = 2;//Two item
	public static final int SQ_TYPE_C = 3;//TAG&Lenght

}
