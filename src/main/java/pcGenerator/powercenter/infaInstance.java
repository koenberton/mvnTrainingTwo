package pcGenerator.powercenter;

import generalpurpose.gpUtils;
import generalpurpose.pcDevBoosterSettings;

import java.util.StringTokenizer;

public class infaInstance {
	
	pcDevBoosterSettings xMSet = null;
	gpUtils xU = null;
	
	public enum TRANSFORMATION_TYPE { TARGET_DEFINITION , SOURCE_DEFINITION , SOURCE_QUALIFIER , EXPRESSION , UNKNOWN }
	public enum INSTANCE_TYPE { SOURCE , TARGET , TRANSFORMATION , UNKNOWN }
	
	public String InstanceName = null;
	public String Description = null;
	public String Transformation_Name = null;
	public String Associated_Source_Instance = null;
	public String Dbdname = null;
	public String reusable = null;
	public TRANSFORMATION_TYPE Transformation_Type = TRANSFORMATION_TYPE.UNKNOWN;
	public INSTANCE_TYPE Instance_Type = INSTANCE_TYPE.UNKNOWN;
	public boolean isValid = false;
	public Object obj = null;
	public int stage=-1;
	
	//----------------------------------------------------------------
	public infaInstance(pcDevBoosterSettings im , String sLijn)
	//----------------------------------------------------------------
	{
		xMSet = im;
		xU = xMSet.xU;
		if( sLijn != null ) {
		  boolean ib = parse( sLijn );
		  isValid = ib;
		  sho();
		}
		else {
			isValid = false;  // instance needs to be created manually
		}
	}

	//----------------------------------------------------------------
	private void logit(int level , String sLog)
	//----------------------------------------------------------------
	{
					xMSet.logit(level, "[" +  this.getClass().getName() + "] " + sLog);
	}
				
	//----------------------------------------------------------------
	private void errit(String sLog)
	//----------------------------------------------------------------
	{
				  logit(0,sLog);	
	}
	
	//----------------------------------------------------------------
	public void sho()
	//----------------------------------------------------------------
	{
		logit(9, "INSTANCE [Instance Name = " + this.InstanceName + "] [" +
				 "Transformation Name = " + this.Transformation_Name + "] [" +
		         "Description = " + this.Description + "] [" +
				 "Assoc = " + Associated_Source_Instance + "] [" +
		         "Trans type = " + Transformation_Type + "] [" +
				 "Type = " + Instance_Type + "]");
	}
	
	
	//----------------------------------------------------------------
	private boolean parse(String sIn)
	//----------------------------------------------------------------
	{
		
		String sLijn = xU.transformSpacesInQuotes(sIn.trim(), '§' );
		sLijn = xU.removeBelowSpaces(sLijn);
		if( sLijn.startsWith("<INSTANCE") == false ) {
			 errit("INSTANCE line does not start with INSTANCE");
			 return false;
		}
		sLijn = sLijn.substring("<INSTANCE".length());
		if( sLijn.endsWith("</INSTANCE>") ) {
			sLijn = sLijn.substring(0,(sLijn.length()-"</INSTANCE>".length()));
		}
		if( sLijn.endsWith("/>") ) {
			sLijn = sLijn.substring(0,(sLijn.length()-2));
		}
		if( sLijn.endsWith(">") ) {
			sLijn = sLijn.substring(0,(sLijn.length()-1));
		}
		//
		StringTokenizer st = new StringTokenizer(sLijn, "= \n");
		while(st.hasMoreTokens()) 
		{ 
			  String sToken = st.nextToken().trim();
			  if( sToken.startsWith("\"") ) {
				  errit("System error - found double quote");
				  return false;
			  }
			  else {
				 String sVal=null;
				 String sTag = sToken.trim();
				 if ( (sTag.startsWith("<")) && (sTag.length()>1) ) {
					 sTag = sTag.substring(1);
				 }
				 // there must be a next value
				 if( st.hasMoreTokens() ) {
					 sVal = strip(st.nextToken());
				 }
				 else {
					 errit( "System error - no more tokens after [" + sTag + "] in " + sLijn);
					 return false;
				 }
				 
			   
                 if( sTag.compareToIgnoreCase("DBDNAME") == 0 ) {
					 this.Dbdname = sVal;
				 }
                 else
                 if( sTag.compareToIgnoreCase("REUSABLE") == 0 ) {
					 this.reusable = sVal;
				 }
                 else
				 if( sTag.compareToIgnoreCase("DESCRIPTION") == 0 ) {
					 this.Description = sVal;
				 }
				 else 
                 if( sTag.compareToIgnoreCase("NAME") == 0 ) {
					 this.InstanceName = sVal;
				 }
                 else 
                 if( sTag.compareToIgnoreCase("TRANSFORMATION_NAME") == 0 ) {
    				this.Transformation_Name = sVal;	 
    			 }
                 else 
                 if( sTag.compareToIgnoreCase("TRANSFORMATION_TYPE") == 0 ) {
    			   	 if( sVal.compareToIgnoreCase("SOURCE QUALIFIER") == 0 ) {
    			   		 this.Transformation_Type = TRANSFORMATION_TYPE.SOURCE_QUALIFIER;
    			   	 }
    			   	 else
    			   	 if( sVal.compareToIgnoreCase("TARGET DEFINITION") == 0 ) {
       			   		 this.Transformation_Type = TRANSFORMATION_TYPE.TARGET_DEFINITION;
       			   	 }
    			   	 else 
    			   	 if( sVal.compareToIgnoreCase("SOURCE DEFINITION") == 0 ) {
       			   		 this.Transformation_Type = TRANSFORMATION_TYPE.SOURCE_DEFINITION;
       			   	 }
    			   	 else {
    			   		 errit("System error - unsupported transformation type " + sVal);
    			   		 return false;
    			   	 }
    			 }
                 else 
                 if( sTag.compareToIgnoreCase("TYPE") == 0 ) {
                	 if( sVal.compareToIgnoreCase("SOURCE") == 0 ) {
    			   		 this.Instance_Type = INSTANCE_TYPE.SOURCE;
    			   	 }	 
                	 else
                	 if( sVal.compareToIgnoreCase("TARGET") == 0 ) {
        			   		 this.Instance_Type = INSTANCE_TYPE.TARGET;
        			 }
                	 else
                	 if( sVal.compareToIgnoreCase("TRANSFORMATION") == 0 ) {
        			   		 this.Instance_Type = INSTANCE_TYPE.TRANSFORMATION;
        			 }
                	 else {
                		 errit("System error - unsupported Instance type " + sVal);
    			   		 return false; 
                	 }
   				 }
                 else  // ASSOCIATED_SOURCE_INSTANCE  NAME=iets
                 if( (sTag.compareToIgnoreCase("ASSOCIATED_SOURCE_INSTANCE") == 0) &&  (sVal.compareToIgnoreCase("NAME") == 0) ) {
    				 if ( st.hasMoreTokens() ) {
    					 this.Associated_Source_Instance = strip(st.nextToken());
    				 }
    				 else {
    					 errit("ASSOCIATED_SOURCE_INSTANCE NAME= has no value");
    					 return false;
    				 }
    			 }
                 else {
                	 errit("Unsupported Instance parameter [" + sTag + "] in " + sLijn );
                	 return false;
                 }
			  }
		}		  
		return true;
	}
	
	//----------------------------------------------------------------
	private String strip(String sIn)
	//----------------------------------------------------------------
	{
		String sRet = xU.Remplaceer(sIn,"\"","");
		sRet = xU.Remplaceer(sRet,"§"," ");
		if( sRet.endsWith("/>") ) {
			sRet = sRet.substring(0,(sRet.length()-2));
		}
		if( sRet.endsWith(">") ) {
			sRet = sRet.substring(0,(sRet.length()-1));
		}
		return sRet;
	}
}
