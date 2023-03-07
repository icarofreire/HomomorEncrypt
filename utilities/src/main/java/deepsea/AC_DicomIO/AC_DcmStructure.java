package AC_DicomIO;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

// import org.apache.log4j.Logger;


public class AC_DcmStructure {
	
	// static Logger logger = Logger.getLogger(AC_DcmStructure.class);
	
	
	private LinkedHashMap<Integer, String[]> m_attirbutes
	= new LinkedHashMap<Integer, String[]>() ;
	private LinkedHashMap<Integer, AC_DcmStructure> m_SequenceMap
	= new LinkedHashMap<Integer, AC_DcmStructure>();
	private byte[] m_pixelData = null;
	private int m_pixelVR = AC_VR.OW;;;
	
	
	// [0] VR
	// [1] Value
	public int getPixelVR()
	{
		return m_pixelVR;
	}
	
	public void setPixelData(byte[] input, int inVR)
	{
		if( m_pixelData !=null)
			m_pixelData = null;
		m_pixelVR  = inVR;
		m_pixelData = input;
	}
	
	public void setSequenceValue(int iTag, AC_DcmStructure value)
	{
		if(value==null)
			value = new AC_DcmStructure();
		
		m_SequenceMap.put(iTag,  value);
	}
	
	public AC_DcmStructure getSequenceValue(int iTag)
	{
		
		
		return m_SequenceMap.get(iTag);
	}
	
	public LinkedHashMap<Integer, AC_DcmStructure> getSequence()
	{

		return m_SequenceMap;
	}
	
	public LinkedHashMap<Integer, String[]> getAttributes()
	{
		return m_attirbutes;
	}
	
	public String[] getAttribute(int input)
	{
		return m_attirbutes.get(input);
	}
	
	public byte[] getPixelData()
	{
		return m_pixelData;
	}

	public void setAttribute(int tag,String[] input)
	{
		try 
		{
			if(input.length!=2)
			{
		
				// logger.error("Input Size Error : input size-> " + input.length );
				throw new Exception(); 
			}
			
			m_attirbutes.put(tag,input);

		}catch(Exception e)
		{
		   /// ���� �߻��� ó�� �κ�
			// logger.error(e);
		}
	}
	
	public void setAttributes(LinkedHashMap<Integer, String[]> input)
	{
		try 
		{
			if(input.size()==0)
			{
		
				// logger.error("Input Size Error : input size-> " + input.size() );
				throw new Exception(); 
			}
			
			m_attirbutes.putAll(input);
		}catch(Exception e)
		{
		   /// ���� �߻��� ó�� �κ�
			// logger.error(e);
		}
	}
	
	private boolean isSequnce(int inTag)
	{
		if(m_SequenceMap.get(inTag) !=null)
			return true;
		return false;
	}
	
	public void printInfo(String input)
	{

		Set<Integer> keyset = m_attirbutes.keySet();
		Iterator<Integer> linkitr = keyset.iterator();
		
		while(linkitr.hasNext())
		{
			int tmpTag = linkitr.next();
			
			String[] tmp  = m_attirbutes.get(tmpTag);

			// logger.debug(String.format(input + "TAG : %08x  VR : %s Value : %s", tmpTag,AC_VR.getVRName(Integer.parseInt(tmp[0])),tmp[1]));
			
		
			if(this.isSequnce(tmpTag))
			{
				AC_DcmStructure tmpSequnce = getSequenceValue(tmpTag);
				tmpSequnce.printInfo(input+ " ");
			}
	
		}

	}
	public void Alnaysis(int type)
	{
		
		Set<Integer> keyset = m_attirbutes.keySet();
		Iterator<Integer> tagItr = keyset.iterator();
		
		
		LinkedHashMap<Integer, String[]> tmpAttri
		= new LinkedHashMap<Integer, String[]>() ;

		while(tagItr.hasNext())
		{
			int tmpTag = tagItr.next();
			String[] tmpVRnValue  = m_attirbutes.get(tmpTag);
			String sVR = tmpVRnValue[0];
			String sValue = tmpVRnValue[1];
			int iVR = Integer.parseInt(sVR);
			if(Integer.parseInt(sVR)==AC_VR.Undefined)
			{
				iVR = AC_DicomDictionary.getTagVR(tmpTag);
			}
		
		


			String[] tmpInput = {Integer.toString(iVR), sValue };
			tmpAttri.put(tmpTag, tmpInput);
			
			if(m_SequenceMap.get(tmpTag)!=null)
				m_SequenceMap.get(tmpTag).Alnaysis(1);
				

		}
		m_attirbutes.clear();
		m_attirbutes.putAll(tmpAttri); 
		

				
		
	}
	

	

}










































































