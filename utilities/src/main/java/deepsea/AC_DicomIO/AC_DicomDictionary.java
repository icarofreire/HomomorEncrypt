package AC_DicomIO;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public  class AC_DicomDictionary {
	

	private static final String VERSION = "1.0.1";
	public String getVersion() {return VERSION;};
	
	

	
	public static List<Integer> m_TagID = new ArrayList<>();
	public static List<Integer> m_TagVR = new ArrayList<>();
	
	public static final String GroupLength ="0000";
	private static boolean m_flagSetup = false;
	

	
	
	AC_DicomDictionary() {
		// TODO Auto-generated constructor stub
		setupList();
	}
	
	public static boolean isSetup()
	{
		return m_flagSetup;
	}
	public static void setupList()
	{
		AC_Tag TagList = new AC_Tag();
		Field[] TagIDFields = TagList.getClass().getFields();
		int nTagID = TagIDFields.length;
		
		AC_TagVR VRList = new AC_TagVR();
		Field[] VRFields = VRList.getClass().getFields();
		int nVR = VRFields.length;
		
		if(!TagList.getVersion().equals(VRList.getVersion())
				||nTagID!=nVR)
		{
			 System.out.println("TagID and TagVR are not the same version.");
			 System.out.println("Please check the version.");
			 return;
		}
		
		try {
			for(int i=0; i<nTagID;i++)
			{
				String sTagIDName = TagIDFields[i].getName();
				String sTagVRName = VRFields[i].getName();
				if(!sTagIDName.equals(sTagVRName))
				{
						 System.out.println("TagID and TagVR are not the same idx.");
						 System.out.println("Please check the element.");
						 return;
				}
				

				int iTagID = (Integer)TagIDFields[i].get(TagList);
				int iTagVR = (Integer)VRFields[i].get(VRList);
				
				
				
				m_TagID.add(iTagID);
				m_TagVR.add(iTagVR);//m_TagID.add(`)
			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			m_flagSetup = false;
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			m_flagSetup = false;
			e.printStackTrace();
		}
		
		m_flagSetup = true;
	}
	
	public static  int getTagVR(int TagID)
	{
		int iSerchIdx = m_TagID.indexOf(TagID);
		if(iSerchIdx==-1)
			return chkElementTag(TagID);
		return m_TagVR.get(iSerchIdx);
	}
	
	public static  int chkElementTag(int TagID) {
		String sTmp = Integer.toHexString(TagID);
		int iEndidx = sTmp.length();
		String sElementTag = sTmp.substring(iEndidx-4, iEndidx);
	
		
		//GoupLeng
		if(sElementTag.equals(GroupLength))
			return AC_VR.UL;
		
		
		return 0x2d2d;
	}
	
	public void clear()
	{
		m_TagID.clear(); 
		m_TagVR.clear();
		
	}
	
	
	

}
