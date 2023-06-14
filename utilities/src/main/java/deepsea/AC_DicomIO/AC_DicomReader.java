package AC_DicomIO;

import java.io.BufferedInputStream;
// import org.apache.log4j.Logger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JOptionPane;



public class AC_DicomReader {
	
	// static Logger logger = Logger.getLogger(AC_DicomReader.class); 
	
	private static final int ID_OFFSET = 128;  //location of "DICM"
	private static final String DICM = "DICM";
	//private static final int IMPLICIT_VR = 0x2D2D; // '--' 
	//private AC_DicomDictionary..// m_DicomDic =  new AC_DicomDictionary();;

	/*\/ tag de finalização do parse; */
	private int tagBreak = 0;
	
	private String m_sFilePath = null;
	private File m_sFile = null;
	BufferedInputStream m_bisInputStream;
	private boolean m_flagFileEnd = false;
//	private int ifReadLoactaion =0;
	private boolean m_bLittleEndian = true;
	///	TransferSyntaxUID
	private String m_sTransferSyntaxUID = null;
	private boolean m_bigEndianTransferSyntax = false;
	private boolean m_Compressed = false;
	
	private static String m_byteSplit = "\\\\"; 

	////// Now property
	private int m_VR;
	private int m_nElementLength = 0;

	private int m_TageID;
	private int m_nLocation = 0;
	private HashMap<Integer, String[]> bitTagToHexParTag = new HashMap<Integer, String[]>();
	

	
	public AC_DicomReader() {
		
		//

	}
	
	public AC_DicomReader(String sFilePath) {
		
		readDCMFile(sFilePath);
		
	}
	public AC_DicomReader(File sFilePath) {

		readDCMFile(sFilePath);
	
	}
	
	public void readDCMFile(File input)
	{
		if(m_sFilePath==null)
			m_sFilePath = input.getAbsolutePath();
	
		init(input);
	}
	
	
	public void readDCMFile(String input)
	{
		m_sFilePath = input;
		m_sFile = new File(m_sFilePath);
		readDCMFile(m_sFile);
	}
	
	public void close()
	{		
	
		try {
			m_bisInputStream.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	

	private void init(File sFilePath)
	{
		 FileInputStream fis = null;
		 
		 if(!AC_DicomDictionary.isSetup())
			 AC_DicomDictionary.setupList();
		 
		 try {
				fis = new FileInputStream(sFilePath);
				m_bisInputStream = new BufferedInputStream(fis);
				m_bisInputStream.mark(400000);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//fis.close();
				e.printStackTrace(); 
			}
	}
	

	
	private boolean checkHeaderStart() throws IOException
	{
		skip(ID_OFFSET);
		
		if (!getString(4).equals(DICM)) 
		{
			if (m_bisInputStream==null) 
			{
				m_bisInputStream.close();
				return false;
			}
			
			if (m_bisInputStream!=null)
			{
				m_bisInputStream.reset();
				 m_flagFileEnd = false;
			}
			
			
		}
		return true;

	}	
	private void checkTSUID() throws IOException
	{

		m_sTransferSyntaxUID = getString(m_nElementLength);


		if (m_sTransferSyntaxUID.indexOf("1.2.840.10008.1.2.2")>=0)
			m_bigEndianTransferSyntax = true;

		if (m_sTransferSyntaxUID.indexOf("1.2.840.10008.1.2.4")>=0)
		{
			m_Compressed = true;
			// logger.error("Compressed Dicom");
			// JOptionPane.showMessageDialog(null, "Compressed DCM�� ���� �������� �ʽ��ϴ�.","File Reader Error",JOptionPane.ERROR_MESSAGE);
		}

	}
	
	public AC_DcmStructure getAttirbutes() throws IOException
	{
		AC_DcmStructure ouptutAttributes = new AC_DcmStructure();
		m_bisInputStream.reset();
		m_flagFileEnd = false;

		checkHeaderStart();

		while(!m_flagFileEnd)
		{
			int tag = getNextTag();

			
			//if(tag!=0x00090010)
		//	{ skip(m_nElementLength);
				//continue;
			//}

			if(AC_VR.getVRName(m_VR) == "" && ouptutAttributes.getAttributes().size() == 0){
				return null;
			}
	
			if(tag==AC_Tag.TransferSyntaxUID)
			{			
				checkTSUID();
				String[] value = {Integer.toString(AC_VR.UI), 
						m_sTransferSyntaxUID};
				ouptutAttributes.setAttribute(tag, value);
			}
			else if(m_VR==AC_VR.SQ || AC_DicomDictionary.getTagVR(tag) == AC_VR.SQ)
			{
			
				String[] seqncvalue = {Integer.toString(m_VR), Integer.toString(m_nElementLength)};
				ouptutAttributes.setAttribute(tag, seqncvalue);
				ouptutAttributes.setSequenceValue(tag, readSequnce(m_nElementLength));
			}else if(tag == AC_Tag.PixelData)
			{
				ouptutAttributes.setPixelData(getPixelData(m_nElementLength),m_VR);
				break;
			}
			else
			{
				String[] value = {Integer.toString(m_VR),getValue(m_VR)};
				ouptutAttributes.setAttribute(tag,value);
			}
			
			/*\/ interromper o parse na tag de finalização; */
			if((tagBreak != 0) && (tag == tagBreak)){
				break;
			}

			// logger.debug(String.format("TAG : %08x , Lengt : %d", tag,m_nElementLength));
		}
		
		return ouptutAttributes;
	}
	
	
	


	private AC_DcmStructure readSequnce(int sqenceLenght) throws IOException
	{
		AC_DcmStructure outputSequnce = new AC_DcmStructure();
		
		
		if(sqenceLenght==-1)
		{
			
			while(true)
			{
				int tag = getNextTag();
				
				if(tag == AC_Tag.SequenceDelimitationItem)
				{
					break;
				}
				else if(m_VR == AC_VR.SQ)
				{
					String[] seqncvalue = {Integer.toString(AC_VR.SQ), Integer.toString(m_nElementLength)};
					outputSequnce.setAttribute(tag, seqncvalue);
					outputSequnce.setSequenceValue(tag, readSequnce(m_nElementLength));
				}
				else
				{
					String[] value = {Integer.toString(m_VR),getValue(m_VR)};
					outputSequnce.setAttribute(tag,value);
				}
				
				// logger.debug(String.format("In SQ TYPE : A  TAG : %08x , Lengt : %d", tag,m_nElementLength));

			}
		}else
		{
			int startSQLocatoin = m_nLocation;
			int SQLenght = m_nElementLength;
	
			
			while(m_nLocation-startSQLocatoin<SQLenght)
			{
				int tag = getNextTag();
				int tmpVR = 0;
				if(m_VR==AC_VR.Undefined)
					tmpVR = AC_DicomDictionary.getTagVR(tag);
				

				if(tag == AC_Tag.Item)
				{
					String[] itmeValue = {Integer.toString(AC_VR.Undefined), Integer.toString(m_nElementLength)};
					outputSequnce.setAttribute(tag,itmeValue);
				}
				else if(m_VR == AC_VR.SQ)
				{
					String[] seqncvalue = {Integer.toString(AC_VR.SQ), Integer.toString(m_nElementLength)};
					outputSequnce.setAttribute(tag, seqncvalue);
					outputSequnce.setSequenceValue(tag, readSequnce(m_nElementLength));
				}
				else
				{
					String[] value = {Integer.toString(m_VR),getValue(m_VR)};
					outputSequnce.setAttribute(tag,value);
				}
				
				// logger.debug(String.format("In SQ TYPE : B  TAG : %08x , Lengt : %d", tag,m_nElementLength));

			}

		}
		
		return outputSequnce;
	}
	
	
	

	

	private int getNextTag() throws IOException
	{
		int igroupWord = getShort();
		if (igroupWord==0x0800 && m_bigEndianTransferSyntax) {
			m_bLittleEndian= false;	
			igroupWord = 0x0008;
		}
		int ielementWord = getShort();
		
		int tag = igroupWord<<16 | ielementWord;

		String[] hexParTag = {Integer.toHexString(igroupWord), Integer.toHexString(ielementWord)};
		bitTagToHexParTag.put(tag, hexParTag);

		m_nElementLength = getLength();


		 m_TageID = tag;
		
		return tag;
	
	}
	
	
	
  	int getLength() throws IOException {
		int b0 = getByte();
		int b1 = getByte();
		
		int b2 = getByte();
		int b3 = getByte();
		
		// We cannot know whether the VR is implicit or explicit
		// without the full DICOM Data Dictionary for public and
		// private groups.
		
		// We will assume the VR is explicit if the two bytes
		// match the known codes. It is possible that these two
		// bytes are part of a 32-bit length for an implicit VR.
		m_VR = (b0<<8) + b1;
		
		
		switch (m_VR) {
		
		
			case  AC_VR.SQ: 
				
				return getInt();
		
		
			case AC_VR.OB: case  AC_VR.OW: case AC_VR.OF :
			case  AC_VR.UN: case  AC_VR.UT:
					
			
				// Explicit VR with 32-bit length if other two bytes are zero
				if ( (b2 == 0) || (b3 == 0) ) 
				{
					return getInt();
				}
				
				
			case AC_VR.AE: case AC_VR.AS: case AC_VR.AT: case AC_VR.CS: case AC_VR.DA: case AC_VR.DS: case AC_VR.DT:  case AC_VR.FD:
			case AC_VR.FL: case AC_VR.IS: case AC_VR.LO: case AC_VR.LT: case AC_VR.PN: case AC_VR.SH: case AC_VR.SL: case AC_VR.SS:
			case AC_VR.ST: case AC_VR.TM:case AC_VR.UI: case AC_VR.UL: case AC_VR.US: case AC_VR.QQ:
				// Explicit vr with 16-bit length
				if (m_bLittleEndian)
					return ((b3<<8) + b2);
				else
					return ((b2<<8) + b3);

			
			default:
				// Implicit VR with 32-bit length...
				m_VR = AC_VR.Undefined;
				if (m_bLittleEndian)
					return ((b3<<24) + (b2<<16) + (b1<<8) + b0);
				else
					return ((b0<<24) + (b1<<16) + (b2<<8) + b3);
		}
	}

	private String getValue(int iVR) throws IOException
	{
		if(m_nElementLength==-1 ||m_nElementLength==0)
			return "";
		
		if(AC_VR.Undefined==iVR)
		{
		//	m_DicomDic = new AC_DicomDictionary();
			
			m_VR = iVR = AC_DicomDictionary.getTagVR(m_TageID);
		}
		
		String sValue ="";
		int ivm =0;
		String fullS = "";
		
		byte b0 = 0;
		byte b1 = 0;
		
	
		switch(iVR)
		{
		case AC_VR.OB: case AC_VR.UN:
			 ivm = m_nElementLength/2;
			 fullS = "";
			
			b0 = (byte)getByte();
			b1 = (byte)getByte();
			fullS = Byte.toString(b0) +m_byteSplit+ Byte.toString(b1);
			//
			for(int i=1; i<ivm;i++)	
			{
				b0 = (byte)getByte();
				b1 = (byte)getByte();
				fullS +=m_byteSplit+Byte.toString(b0) +m_byteSplit+ Byte.toString(b1);
			}
			sValue =fullS;
			//alue = getString(m_nElementLength);
			break;
		case AC_VR.UL:
			sValue =(Integer.toString(getInt()));
			break;
		case AC_VR.FD:
			 ivm = m_nElementLength/8;
			 fullS = "";
			
			
			fullS +=Double.toString(getDouble());
			//
			for(int i=1; i<ivm;i++)	
			{
				fullS +=m_byteSplit+Double.toString(getDouble());
			}
			sValue =fullS;
			break;
		case AC_VR.FL:
			
			if (m_nElementLength==4)
				sValue = Float.toString(getFloat());
			else {
				sValue = "";
				int n = m_nElementLength/4;
				for (int i=0; i<n; i++)
					sValue += Float.toString(getFloat())+m_byteSplit;
			}
			break;
			
		case AC_VR.AE: case AC_VR.AS: case AC_VR.AT: case AC_VR.CS: case AC_VR.DA: case AC_VR.DS: case  AC_VR.DT: 
		case  AC_VR.IS: case  AC_VR.LO: case AC_VR.LT: case AC_VR.PN: case AC_VR.SH: case AC_VR.ST: case AC_VR.TM: case AC_VR.UI:
			sValue = getString(m_nElementLength);
			break;
		case AC_VR.US:
			if (m_nElementLength==2)
				sValue = Integer.toString(getShort());
			else {
				sValue = "";
				int n = m_nElementLength/2;
				for (int i=0; i<n; i++)
					sValue += Integer.toString(getShort())+m_byteSplit;
			}
			break;
		case AC_VR.Undefined:
			 ivm = m_nElementLength/2;
			 fullS = "";
			
			 b0 = (byte)getByte();
			 b1 = (byte)getByte();
			fullS = Byte.toString(b0) +m_byteSplit+ Byte.toString(b1);
			//
			for(int i=1; i<ivm;i++)	
			{
				b0 = (byte)getByte();
				b1 = (byte)getByte();
				fullS += m_byteSplit+ Byte.toString(b0) +m_byteSplit+ Byte.toString(b1);
			}
			sValue =fullS;
			break;
		case AC_VR.SQ:
			sValue = "sqens";
			break;
			/*	boolean privateTag = ((tag>>16)&1)!=0;
		if (tag!=ICON_IMAGE_SEQUENCE && !privateTag)
			break;		*/
			// else fall through and skip icon image sequence or private sequence
		default:
			skip((long)m_nElementLength);
			sValue = "defult";
		}
		
		
		
		sValue = sValue.trim();
		
		if(sValue.equals(""))
			sValue = "";

		return sValue;
	}
	
	
	private void skip(long lskipCount)
	{
		m_nLocation += lskipCount;
		
		while (lskipCount > 0)
			try {
				lskipCount -= m_bisInputStream.skip(lskipCount);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	private byte[] getPixelData(int length) throws IOException
	{
		
		byte[] arrPixelData = new byte[m_nElementLength];
		
		for(int i=0; i<m_nElementLength;i++)
		{
			arrPixelData[i] =  (byte) m_bisInputStream.read();
		}
		
		return arrPixelData;
	}
	
	
	
	String getString(int length) throws IOException {
		byte[] buf = new byte[length];
	
		for(int i=0; i<length;i++)
		{
			buf[i] = (byte)getByte();
		}
		
		m_nLocation += length;
		
		String tmp = new String(buf);
		String newTmp = tmp.replaceAll("(^\\p{Z}+|\\p{Z}+$)", "");
		return newTmp;
	}
  
	int getByte() throws IOException {
		int b = m_bisInputStream.read();
		if (b ==-1) 
		{
			 m_flagFileEnd = true;
			//throw new IOException("unexpected EOF");
		
		}
		++m_nLocation;
		return b;
	}

	int getShort() throws IOException {
		int b0 = getByte();
		int b1 = getByte();
		if (m_bLittleEndian)
			return ((b1 << 8) + b0);
		else
			return ((b0 << 8) + b1);
	}
  
	final int getInt() throws IOException {
		int b0 = getByte();
		int b1 = getByte();
		int b2 = getByte();
		int b3 = getByte();
		if (m_bLittleEndian)
			return ((b3<<24) + (b2<<16) + (b1<<8) + b0);
		else
			return ((b0<<24) + (b1<<16) + (b2<<8) + b3);
	}

	double getDouble() throws IOException {
		int b0 = getByte();
		int b1 = getByte();
		int b2 = getByte();
		int b3 = getByte();
		int b4 = getByte();
		int b5 = getByte();
		int b6 = getByte();
		int b7 = getByte();
		long res = 0;
		if (m_bLittleEndian) {
			res += b0;
			res += ( ((long)b1) << 8);
			res += ( ((long)b2) << 16);
			res += ( ((long)b3) << 24);
			res += ( ((long)b4) << 32);
			res += ( ((long)b5) << 40);
			res += ( ((long)b6) << 48);
			res += ( ((long)b7) << 56);         
		} else {
			res += b7;
			res += ( ((long)b6) << 8);
			res += ( ((long)b5) << 16);
			res += ( ((long)b4) << 24);
			res += ( ((long)b3) << 32);
			res += ( ((long)b2) << 40);
			res += ( ((long)b1) << 48);
			res += ( ((long)b0) << 56);
		}
		return Double.longBitsToDouble(res);
	}
    
	float getFloat() throws IOException {
		int b0 = getByte();
		int b1 = getByte();
		int b2 = getByte();
		int b3 = getByte();
		int res = 0;
		if (m_bLittleEndian ) {
			res += b0;
			res += ( ((long)b1) << 8);
			res += ( ((long)b2) << 16);
			res += ( ((long)b3) << 24);     
		} else {
			res += b3;
			res += ( ((long)b2) << 8);
			res += ( ((long)b1) << 16);
			res += ( ((long)b0) << 24);
		}
		return Float.intBitsToFloat(res);
	}

	public HashMap<Integer, String[]> getBitTagToHexParTag(){
		return bitTagToHexParTag;
	}

	public void setTagBreak(int gru, int ele){
		int oprTag = (gru << 16 | ele);
		tagBreak = oprTag;
	}

}
