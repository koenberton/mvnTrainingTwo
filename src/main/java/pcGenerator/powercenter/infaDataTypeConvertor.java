package pcGenerator.powercenter;

import pcGenerator.ddl.ddlDataTypeConvertor;
import pcGenerator.ddl.rdbmsDatatype;
import generalpurpose.pcDevBoosterSettings;

public class infaDataTypeConvertor {

	pcDevBoosterSettings xMSet = null;
	
	//----------------------------------------------------------------
	public infaDataTypeConvertor(pcDevBoosterSettings im)
	//----------------------------------------------------------------
	{
		xMSet = im;
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
	public infaDataType convertRDBMSToInfa( infaDataType din , String usagetipe )
	//----------------------------------------------------------------
	{
		if( usagetipe.trim().compareToIgnoreCase("SOURCEQUALIFIER") == 0 ) return convertToPwCSourceQualifier(din);
		//
		errit("CONVERTOR - does not support usagetype [" + usagetipe + "] yet");
		return null;
	}

	//----------------------------------------------------------------
	private infaDataType convertToPwCSourceQualifier( infaDataType din )
	//----------------------------------------------------------------
	{
		switch( din.rdbmsmake )
		{
		case NETEZZA : return convertRDBMSToPwC( din , rdbmsDatatype.DBMAKE.NETEZZA );
		case ORACLE  : return convertRDBMSToPwC( din , rdbmsDatatype.DBMAKE.ORACLE );
		case DB2 	 : return convertRDBMSToPwC( din , rdbmsDatatype.DBMAKE.DB2 );//return convertDB2ToPwC(din);
		case FLATFILE : return convertFlatFileToPwC(din);
		default : {
			errit("convert to PwC Source Qualifier - RDBMS [" + din.rdbmsmake + "] currently not supported");
			return null;
		}
		}
	}
	
	//----------------------------------------------------------------
	private infaDataType convertFlatFileToPwC( infaDataType din )
	//----------------------------------------------------------------
	{
		// I guess flatfile is already in PWC format - see what happens
		if( (din.DataType.compareToIgnoreCase("STRING") != 0) && (din.DataType.compareToIgnoreCase("NSTRING") != 0) ) {
			errit("FLAT FILE - datatype not STRING/NSTRING but " + din.DataType + "- please investigate");
		}
		// depending on the global setting of use National Character set
		String sDataType = ( xMSet.getUsenationalCharacterSet() == true ) ? "NSTRING" : "STRING";
		infaDataType ret = new infaDataType( xMSet , "POWERCENTER" , "SOURCEQUALIFIER" , sDataType , din.Precision , din.Scale );
		if( ret != null ) ret.DataType = sDataType.toLowerCase();
		return ret;
	}
	
	/*
	//----------------------------------------------------------------
	private infaDataType convertDB2ToPwC( infaDataType din )
	//----------------------------------------------------------------
	{
		String datatype = din.DataType;
		// map DB2 datatypes onto PowerCenter datatype
		if( datatype.compareToIgnoreCase("CHAR_FOR_BIT_DATA") == 0 ) datatype = "binary";
		else
		if( datatype.compareToIgnoreCase("CHAR") == 0 ) datatype = "string";
		else
		if( datatype.compareToIgnoreCase("VARCHAR") == 0 ) datatype = "string";
		else
		if( datatype.compareToIgnoreCase("CHARACTER VARYING") == 0 ) datatype = "string";
		else
		if( datatype.compareToIgnoreCase("TIMESTAMP") == 0 ) datatype = "date/time";
		else
		if( datatype.compareToIgnoreCase("INTEGER") == 0 ) datatype = "integer";
		else
		if( datatype.compareToIgnoreCase("BIGINT") == 0 ) datatype = "bigint";
		else
		if( datatype.compareToIgnoreCase("DECIMAL") == 0 ) datatype = "decimal";
		else
		if( datatype.compareToIgnoreCase("FLOAT") == 0 ) datatype = "double";
		else
		if( datatype.compareToIgnoreCase("VARGRAPHIC") == 0 ) datatype = "nstring";
		else
		if( datatype.compareToIgnoreCase("SMALLINT") == 0 ) datatype = "small integer";
		else
		if( datatype.compareToIgnoreCase("VARCHAR FOR BIT DATA") == 0 ) datatype = "binary";
		else
		if( datatype.compareToIgnoreCase("DATE") == 0 ) datatype = "date/time";
		else
		if( datatype.compareToIgnoreCase("LONG VARCHAR") == 0 ) datatype = "text";
		else
		if( datatype.compareToIgnoreCase("LONG VARGRAPHIC") == 0 ) datatype = "ntext";
		else
		if( datatype.compareToIgnoreCase("NUMERIC") == 0 ) datatype = "decimal";		
		else
		if( datatype.compareToIgnoreCase("TIME") == 0 ) datatype = "date/time";
		else {
			errit("convertDB2ToPwC datatype [" + datatype + "] currently not supported");
			return null;
		}
		//
		infaDataType ret = new infaDataType( xMSet , "POWERCENTER" , "SOURCEQUALIFIER" , datatype , din.Precision , din.Scale );
		return ret;
	}
	*/
	
	//----------------------------------------------------------------
	private infaDataType convertRDBMSToPwC( infaDataType din ,  rdbmsDatatype.DBMAKE dbmake)
	//----------------------------------------------------------------
	{
		ddlDataTypeConvertor ddlconv = new ddlDataTypeConvertor(xMSet);
		//
		String sPwcDatatype = ddlconv.getEquivalentDataType( din.DataType , din.Precision , din.Scale , dbmake , rdbmsDatatype.DBMAKE.POWERCENTER);
		// If national char set not used transform nstring 
		if(  xMSet.getUsenationalCharacterSet() == false ) {
			if( sPwcDatatype.compareToIgnoreCase("NSTRING") == 0 ) sPwcDatatype = "STRING";
			if( sPwcDatatype.compareToIgnoreCase("NTEXT") == 0 ) sPwcDatatype = "TEXT";
		}
		logit(9,"[" + dbmake + "] [" + din.DataType + "] -> [POWERCENTER] [" + sPwcDatatype + "]");
		if( sPwcDatatype == null )  return null;
		//
		infaDataType ret = new infaDataType( xMSet , "POWERCENTER" , "SOURCEQUALIFIER" , sPwcDatatype , din.Precision , din.Scale );
		return ret;
	}
}
