package pcGenerator.powercenter;

import generalpurpose.pcDevBoosterSettings;

import java.util.ArrayList;

public class infaSourceQualifier {

	pcDevBoosterSettings xMSet = null;
	
	public String TableName = "";
	public String Dbdname = "";
	public String Description = "";
	public String Name ="";
	public String ObjectVersion = "";
	public String Reusable = "";
	public String VersionNumber = "";
	public ArrayList<infaSourceQualifierField> sqFieldList = null;
	public ArrayList<infaPair> AttrList = null;
	public boolean isValid = true;
	
	//----------------------------------------------------------------
	public infaSourceQualifier(pcDevBoosterSettings im , infaSource src )
	//----------------------------------------------------------------
	{
		xMSet = im;
		//
		TableName     = src.Name;
		Dbdname       = src.Dbdname;
		Description   = "SQ based on [" + Dbdname + "." + TableName + "]";
		Name          = mkName();
		ObjectVersion = "1";
		Reusable      = "NO";
		VersionNumber = "1";
		//
		sqFieldList = new ArrayList<infaSourceQualifierField>();
		if( (isValid = mkFieldList( src ))  == true ) { 
		 AttrList = new ArrayList<infaPair>();
		 isValid = mkAttrList();
		}
		logit(1,"Source Qualifier [" + this.Name + "] has succesfully created for [" + src.Name + "]");
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
	private String mkName()
	//----------------------------------------------------------------
	{
	  String sTemp = TableName.toUpperCase();
	  return "SQ_" + sTemp.trim();
	}
	
	//----------------------------------------------------------------
	private boolean mkFieldList(infaSource src )
	//----------------------------------------------------------------
	{
		infaDataTypeConvertor conv = new infaDataTypeConvertor(xMSet);
		for(int i=0; i < src.fieldList.size(); i++)
		{
			infaSourceField orig = src.fieldList.get(i);
			infaSourceQualifierField x = new infaSourceQualifierField( orig.Name );
			x.Datatype    = orig.DataType;
			x.Description = orig.Description;
			x.PictureText = orig.PictureText;
			x.Precision   = orig.Precision;
			x.Scale       = orig.scale;
			//
			// create a datatype object 
			// a. for checking the validity
			// b. as input for the translator
			infaDataType dt = new infaDataType( xMSet , src.Databasetype , "SOURCEQUALIFIER" , orig.DataType , orig.Precision , orig.scale );
		    if( (dt.DataType == null) || (dt.isValid == false) ) {
		    	errit("Cannot create INFA Datatype object");
		    	return false;
		    }
		    // Convert to PowerCenter datatypes for a source qualifier
		    infaDataType sqdatatype = conv.convertRDBMSToInfa( dt , "SOURCEQUALIFIER" );
		    if( (sqdatatype == null) || (sqdatatype.isValid == false) ) {
		    	errit("Cannot transform into INFA Datatype object");
		    	return false;
		    }
		    // update specs
		    x.Datatype    = sqdatatype.DataTypeDisplay;   
			x.Precision   = sqdatatype.Precision;
			x.Scale       = sqdatatype.Scale;
		    //
			dt = null;
			sqdatatype=null;
		    //
			// Specific for SQ fields
			x.DefaultValue = "";
			x.Porttype = "INPUT/OUTPUT";
	        //				
			sqFieldList.add( x );
		}
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean mkAttrList()
	//----------------------------------------------------------------
	{
		for(int i=0;i<11;i++)
		{
		  String sCode = null;
		  String sVal = null;
		  switch( i )
		  {
			  case  0 : { sCode = "Sql Query"; sVal = ""; break; }
			  case  1 : { sCode = "User Defined Join"; sVal = ""; break; }
			  case  2 : { sCode = "Source Filter"; sVal = ""; break; }
			  case  3 : { sCode = "Number Of Sorted Ports"; sVal = "0"; break; }
			  case  4 : { sCode = "Tracing Level"; sVal = "Normal"; break; }
			  case  5 : { sCode = "Select Distinct"; sVal = "NO"; break; }
			  case  6 : { sCode = "Is Partitionable"; sVal = "NO"; break; }
			  case  7 : { sCode = "Pre SQL"; sVal = ""; break; }
			  case  8 : { sCode = "Post SQL"; sVal = ""; break; }
			  case  9 : { sCode = "Output is deterministic"; sVal = "NO"; break; }
			  case 10 : { sCode = "Output is repeatable"; sVal = "Never"; break; }
		  }
		  if( (sCode == null) || (sVal == null) ) {
			  errit("System error - infasource qualifier ); - init attrs");
			  return false;
		  }
		  infaPair x = new infaPair( sCode , sVal );
		  AttrList.add(x);
		}
		return true;
	}
}


/*
 *     
 *    DESCRIPTION 
 *    NAME 
 *    OBJECTVERSION 
 *    REUSABLE 
 *    TYPE ="r" 
 *    VERSIONNUMBER
 */
