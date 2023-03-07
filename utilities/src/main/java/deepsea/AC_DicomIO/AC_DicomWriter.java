package AC_DicomIO;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

// import org.apache.log4j.Logger;

public class AC_DicomWriter {
	
	// static Logger logger = Logger.getLogger(AC_DicomWriter.class);
	private static final int ID_OFFSET = 128;  //location of "DICM"
	private static final String DICM = "DICM";
	private BufferedOutputStream m_buffOutStream = null;
	private boolean m_flagLittle = true;
	

	private static String m_byteSplit = "\\\\"; 
	
	private File m_file = null;
	 
	public AC_DicomWriter(String sFilePath) {

		setFile( new File(sFilePath));
	}
	 
	public AC_DicomWriter(File inFile) {

		setFile( inFile);
	}
	
	
	public void setFile(String sFilePath)
	{
		// TODO Auto-generated method stub
		setFile( new File(sFilePath));
		
	}

	public void setFile(File inFile)
	{
		// TODO Auto-generated method stub
		m_file = inFile;
	}

	
	public void writeDCMFile(AC_DcmStructure inDCMStuc)
	{
		if(m_buffOutStream!=null)
		{
			try {
				m_buffOutStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// logger.error("Dcm Write buffoupterr");
				e.printStackTrace();
			}finally {
				m_buffOutStream  = null;
			}
			
		}
		
		try {
			m_buffOutStream = new BufferedOutputStream(new FileOutputStream(m_file.getAbsolutePath()));
			
			wirteDCMI(m_buffOutStream);
			byte[] attribute = cnvDcmStruc2Bytearr(inDCMStuc);
			
		
			
			
			m_buffOutStream.write(attribute);			
			m_buffOutStream.write(getBytesPixelData(inDCMStuc));
			m_buffOutStream.flush();

			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			// logger.error("Dcm Write OutStream");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



	
	}
	private void wirteDCMI(BufferedOutputStream inBufOutStream) throws IOException {
		// TODO Auto-generated method stub
		for(int i=0; i<AC_DCMStandard.DCMI_LEN; i++)
			inBufOutStream.write((byte)0x00);
		//44 49 43 4D
		inBufOutStream.write(0x44);
		inBufOutStream.write(0x49);
		inBufOutStream.write(0x43);
		inBufOutStream.write(0x4D);
		
	}

	private byte[] getBytesPixelData(AC_DcmStructure inDCMStuc)
	{
		//E0 7F 10 00 4F 57 00 00
		
		boolean b_flagUnVR = inDCMStuc.getPixelVR()==AC_VR.Undefined;
		
		byte[] abTag =  getIntTag2Btyes(0x7FE00010);
		byte[] abVR  = {0x4f, 0x57,0x00,0x00};
		byte[] abPixel =  inDCMStuc.getPixelData();
		byte[] abLenght = getInt2Bytes(abPixel.length);
		int margeLength = abTag.length+abVR.length+abPixel.length+abLenght.length;
		
		if(b_flagUnVR)
		{
			margeLength = abTag.length+abPixel.length+abLenght.length;
			return ByteBuffer.allocate(margeLength).put(abTag).put(abLenght).put(abPixel).array();
		}
		
		return ByteBuffer.allocate(margeLength).put(abTag).put(abVR).put(abLenght).put(abPixel).array();
	}
	
	
	
	private byte[] cnvDcmStruc2Bytearr(AC_DcmStructure inDCMStuc)
	{
		
		LinkedHashMap<Integer, String[]> attirbutes
		= inDCMStuc.getAttributes();
		LinkedHashMap<Integer, AC_DcmStructure> seqenceMap
		= inDCMStuc.getSequence();
		
		Set<Integer> keyset = attirbutes.keySet();
		Iterator<Integer> tagItr = keyset.iterator();
		
		LinkedList<byte[]> dcmByteList = new LinkedList<>();
		
		while(tagItr.hasNext())
		{
	

			
			int tmpTag = tagItr.next();
			
			
			//if(tmpTag!=0x00090010)
			//	continue;
			
			String[] tmp  = attirbutes.get(tmpTag);

			// logger.debug(String.format("TAG : %08x , vr : %s, value : %s", tmpTag,tmp[0], tmp[1]   ));
			
			
			byte[] tmparrByte = getByteValue(tmpTag, tmp);
			

			
		
			
			
			if(seqenceMap.get(tmpTag)==null)
			{
				dcmByteList.add(tmparrByte);
			}else
			{
				AC_DcmStructure tmpSequnce = seqenceMap.get(tmpTag);
				dcmByteList.add(getSQBytes(tmpTag, tmp, tmpSequnce));
			}
				
		}
		
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		for(int i=0; i<dcmByteList.size();i++)
		{
			try {
				outputStream.write(dcmByteList.get(i));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return outputStream.toByteArray();
	}
	
	private byte[] getSQBytes(int inTag, String[] inValue,AC_DcmStructure inSQStruc)
	{
		//byte[] abTag =  getIntTag2Btyes(inTag);
		int iVR  = Integer.parseInt(inValue[0]);
		String sValue  = inValue[1];
		int SQType = 0;
		
		
		byte[] abTag =  getIntTag2Btyes(inTag);
		byte[] abVR  = new byte[2];
		ByteBuffer.wrap(abVR).putShort((short)iVR);
		//ByteBuffer.wrap(abVR).putShort((short)iVR);
		byte[] abValue = null; 

		
		
		byte[] tmpSQbyte = new byte[0];
		byte[] tmpSQDelimi = new byte[0];
		if(sValue.equals("-1"))
		{
			if(iVR==AC_VR.SQ)
			{
				tmpSQbyte = AC_DCMStandard.BYTES_SQ_STANDARD_THREEITEM.clone();
			}
			else if(iVR==AC_VR.Undefined)
			{
				tmpSQbyte = AC_DCMStandard.BYTES_SQ_STANDARD_TWOITEM.clone();
			}
			for(int idx = 0 ; idx< abTag.length;idx++)
				tmpSQbyte[idx] = abTag[idx];
			tmpSQDelimi =  AC_DCMStandard.BYTES_SQ_DElIMITATION;	
		}else
		{
			abVR = paddingZero(abVR, 4);
			if(sValue.equals("-1") )
				sValue = "0";
			 if(iVR==AC_VR.Undefined)
				abVR = new byte[0];
			abValue = getInt2Bytes(Integer.parseInt(sValue));
			tmpSQbyte =  ByteBuffer.allocate(abTag.length+abVR.length+abValue.length).put(abTag).put(abVR).put(abValue).array();	
		}
		
		byte[] bTmpSequnce = cnvDcmStruc2Bytearr(inSQStruc);

		return ByteBuffer.allocate(tmpSQbyte.length+bTmpSequnce.length+tmpSQDelimi.length).
				put(tmpSQbyte).put(bTmpSequnce).put(tmpSQDelimi).array();
	}
	
	
	private byte[] getByteValue(int inTag, String[] inString)
	{
	
		boolean flagUnDefVR = false;
		int inVR = Integer.parseInt(inString[0]);
		
		byte[] abTag =  getIntTag2Btyes(inTag);
		byte[] abVR  = new byte[2];
		ByteBuffer.wrap(abVR).putShort((short)inVR);
	
		byte[] abValue = new byte[0]; 
		byte[] abLenght = {0x00, 0x00};
	
		
		int tmp = 0;
		
		if( AC_VR.Undefined==inVR)
		{ 
			flagUnDefVR = true;
			inVR = AC_DicomDictionary.getTagVR(inTag);
		}

		if(inTag== AC_Tag.Item)
		{
			if(inString[1].equals(""))
			{
				if(m_flagLittle)
					return AC_DCMStandard.BYTES_ITEM_STANDARD_TWOITEM_LITTLE;
				else
					return AC_DCMStandard.BYTES_ITEM_STANDARD_TWOITEM_BIG;
			}else
			{
				tmp  = Integer.parseInt(inString[1]);
				abValue = getInt2Bytes(tmp);
				return ByteBuffer.allocate(8).put(abTag).put(abValue).array();
			}
		}
		
		
		if(inTag== AC_Tag.ItemDelimitationItem)
		{
			return AC_DCMStandard.BYTES_ITEM_DElIMITATION;
		}
		
	
		
		String[] split = inString[1].split(m_byteSplit+m_byteSplit);
		if(split.length==0)
		{
			split = new String[1];
			split[0] = inString[1];
		}
		


		switch(inVR) 
		{
		



		case AC_VR.AE: case AC_VR.AS: case AC_VR.AT: case AC_VR.CS: case AC_VR.DA: case AC_VR.DS: case  AC_VR.DT: 
		case  AC_VR.IS: case  AC_VR.LO: case AC_VR.LT: case AC_VR.PN: case AC_VR.SH: case AC_VR.ST: case AC_VR.TM: case AC_VR.UI:

			//	abVR = paddingZero(abVR, 4);
			abValue = getString2Bytes(inString[1]);

			break;
		case AC_VR.OB: case  AC_VR.OW: case AC_VR.OF :
		case  AC_VR.UN: case  AC_VR.UT: case AC_VR.Undefined:
		
			abVR = paddingZero(abVR, 4);
			 abLenght  = paddingZero(abLenght, 4);
			String[] tmpS = inString[1].split(m_byteSplit+m_byteSplit);
			abValue = new byte[tmpS.length];
			
			
			for(int i=0; i<tmpS.length;i++)
			{
				if(tmpS[i]!="")
					abValue[i] = Byte.parseByte(tmpS[i]);
				else
					abValue[i] = 0x00;
			}
			
			if(inString[1]=="")
			{
				abValue = null;
				
			}

			break;
			


		case AC_VR.US:
			int typeSize = Short.BYTES;
			abValue = new byte[split.length*typeSize];			
			for(int i=0; i<split.length;i++)
			{
				Short tmpShort  = Short.parseShort(split[i]);
				byte[] tmpBytes = getShort2Bytes(tmpShort);
				for(int j=0; j < typeSize;j++)
					abValue[(typeSize*i)+j] = tmpBytes[j];
			}
		
			break;

		case AC_VR.UL:
			
			typeSize = Integer.BYTES;
			abValue = new byte[split.length*typeSize];		
			
			for(int i=0; i<split.length;i++)
			{
				int tmpInt  = Integer.parseInt(split[i]);
				byte[] tmpBytes = getInt2Bytes(tmpInt);
				for(int j=0; j < typeSize;j++)
					abValue[(typeSize*i)+j] = tmpBytes[j];
			}
			break;

		case AC_VR.FL:
			
			typeSize = Float.BYTES;
			abValue = new byte[split.length*typeSize];			
			for(int i=0; i<split.length;i++)
			{
				float tmpFloat  = Float.parseFloat(split[i]);
				byte[] tmpBytes = getFloat2Bytes(tmpFloat);
				for(int j=0; j < typeSize;j++)
					abValue[(typeSize*i)+j] = tmpBytes[j];
			}
			break;

		case AC_VR.FD:
			typeSize = Double.BYTES;
			abValue = new byte[split.length*typeSize];			
			for(int i=0; i<split.length;i++)
			{
				double tmpDouble  = Double.parseDouble(split[i]);
				byte[] tmpBytes = getDouble2Bytes(tmpDouble);
				for(int j=0; j < typeSize;j++)
					abValue[(typeSize*i)+j] = tmpBytes[j];
			}
			break;

		}
		if(flagUnDefVR)
		{
			 abVR = new  byte[0];
			 abLenght  = paddingZero(abLenght, 4);
		}
		
		
		
		byte[] concatBytes = null;
	
	
			if(abVR.length == 4|| flagUnDefVR)
			{
				abLenght = getInt2Bytes(abValue.length);
				//abVR = paddingZero(abVR, 4);
			}else
				abLenght = getShort2Bytes((short)abValue.length);

			int margeLength = abTag.length+abVR.length+abValue.length+abLenght.length;
			concatBytes =  ByteBuffer.allocate(margeLength).put(abTag).put(abVR).put(abLenght).put(abValue).array();
	
			
		

		
		
		
		return concatBytes;
	}
	
	private byte[] arrayReviers(byte[] inBytes)
	{
		byte[] outBytes = new byte[inBytes.length];
		for(int i=0; i< inBytes.length;i++)
			outBytes[i] = inBytes[inBytes.length-1-i];
		return outBytes;
	}
	
	private byte[] getIntTag2Btyes(int inInput)
	{    
		byte[] outBytes = new byte[4];
		ByteBuffer.wrap(outBytes).putInt(inInput);
		
		if(m_flagLittle)
		{
			outBytes = arrayReviers(outBytes);
			return new byte[] {outBytes[2], outBytes[3], outBytes[0], outBytes[1]};
		}else	
			return outBytes;
		
	}
	
	private byte[] getDouble2Bytes(double inInput)
	{    
		byte[] outBytes = new byte[8];
		ByteBuffer.wrap(outBytes).putDouble(inInput);
		
		if(m_flagLittle)
		{
			return arrayReviers(outBytes);
		}else	
			return outBytes;
		
	}
	
	private byte[] getFloat2Bytes(float inInput)
	{    
		byte[] outBytes = new byte[4];
		ByteBuffer.wrap(outBytes).putFloat(inInput);
		
		if(m_flagLittle)
		{
			return arrayReviers(outBytes);
		}else	
			return outBytes;
		
	}
	
	
	
	private byte[] getInt2Bytes(int inInput)
	{
		byte[] outBytes = new byte[4];
		ByteBuffer.wrap(outBytes).putInt(inInput);
		
		if(m_flagLittle)
		{
			return arrayReviers(outBytes);
		}else	
			return outBytes;
	}
	
	
	private byte[] getShort2Bytes(short inInput)
	{
		byte[] outBytes = new byte[2];
		ByteBuffer.wrap(outBytes).putShort(inInput);
		
		if(m_flagLittle)
		{
			return arrayReviers(outBytes);
		}else	
			return outBytes;
	}
	
	private byte[] getString2Bytes(String inString)
	{
		byte[] outBytes = inString.getBytes();
		if(outBytes.length%2!=0)
			outBytes = paddingZero(outBytes,outBytes.length+1);
		return outBytes;
	}
	
	private  byte[] paddingZero( byte[] inBytes, int inSize)
	{
		byte[] tmp = new byte[inSize];
		for(int i=0; i<inBytes.length;i++)
			tmp[i] = inBytes[i];
		for(int i=inBytes.length; i<inSize;i++)
			tmp[i] = 0x00;

		return tmp.clone();
	}
	


	

	
	
	
	

}
