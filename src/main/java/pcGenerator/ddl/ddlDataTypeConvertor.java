package pcGenerator.ddl;

import generalpurpose.pcDevBoosterSettings;
import pcGenerator.powercenter.infaDataType;

public class ddlDataTypeConvertor {

	pcDevBoosterSettings xMSet = null;
	
	String[][] conv = {
			{ "POWERCENTER"		, "DB2" 				, "ORACLE"			, "NETEZZA"		, "FLATFILE"},
			{ "bigint"    		, "BIGINT" 				, "NUMBER-19"		, "BIGINT"		, "string"	},
			{ "bigint"    		, "BIGINT" 				, "NUMBER-19"		, "INT8" 		, "string"	},
			{ "date/time" 		, "DATE" 				, "DATE" 			, "DATE" 		, "string"	},
			{ "date/time" 		, "TIME" 				, "DATE" 			, "TIME" 		, "string"	},
			{ "date/time" 		, "DATETIME" 			, "TIMESTAMP" 		, "TIMESTAMP" 	, "string"	},
			{ "date/time" 		, "TIMESTAMP" 			, "TIMESTAMP" 		, "TIMESTAMP" 	, "string"	},
			{ "date/time" 		, "TIMESTMP" 			, "TIMESTAMP" 		, "TIMESTAMP" 	, "string"	},
			{ "decimal"   		, "DECIMAL" 			, "NUMBERPS"		, "NUMERIC" 	, "string"	},
			{ "decimal"   		, "DECIMAL" 			, "NUMBERPS"		, "DECIMAL" 	, "string"	},
			{ "double"    		, "FLOAT" 				, "NUMBER" 			, "FLOAT" 		, "string"	},
			{ "double"    		, "FLOAT" 				, "FLOAT" 			, "DOUBLE" 		, "string"	},
			{ "integer"   		, "INTEGER" 			, "NUMBER-11" 		, "INTEGER" 	, "string"	},
			{ "integer"   		, "INTEGER" 			, "NUMBER-11" 		, "INT4" 		, "string"	},
			{ "real"      		, "FLOAT" 				, "NUMBER" 			, "REAL" 		, "string"	},
			//{ "small integer" 	, "SMALLINT" 			, "NUMBER-3" 		, "BYTEINT" 	, "string"	},
			{ "small integer" 	, "SMALLINT" 			, "NUMBER-3" 		, "SMALLINT" 	, "string"	},   // BYTEINT ?? went for smallint   
			{ "small integer" 	, "SMALLINT" 			, "NUMBER-3" 		, "INT1" 		, "string"	},
			{ "small integer" 	, "SMALLINT" 			, "NUMBER-6" 		, "SMALLINT" 	, "string"	},
			{ "small integer" 	, "SMALLINT" 			, "NUMBER-6" 		, "INT2" 		, "string"	},
			{ "string"    		, "CHAR" 				, "CHAR" 			, "CHAR" 		, "string"	},
			{ "string"    		, "VARCHAR" 			, "VARCHAR2" 		, "VARCHAR" 	, "string"	},
			{ "string"    		, "VARCHAR" 			, "VARCHAR" 		, "VARCHAR" 	, "string"	},
			{ "text"     		, "LONG VARCHAR" 		, "N/A" 			, "N/A" 		, "string"	},
			{ "binary"    		, "CHAR FOR BIT DATA"	, "N/A" 			, "N/A" 		, "nstring"	},
			{ "binary"    		, "VARCHAR FOR BIT DATA", "N/A" 			, "N/A" 		, "nstring"	},
			{ "ntext"     		, "LONG VARGRAPHIC" 	, "CLOB" 			, "N/A" 		, "nstring"	},
			{ "nstring"   		, "VARGRAPHIC" 			, "VARCHAR2" 		, "NVARCHAR" 	, "nstring"	},   // KEEP nstrings at back ie lower prio
			{ "nstring"   		, "GRAPHIC" 			, "RAW" 			, "NCHAR" 		, "nstring"	},
			{ "nstring"   		, "VARGRAPHIC" 			, "VARCHAR2" 		, "NVARCHAR" 	, "nstring"	},
			{ "bigint"    		, "BIGINT" 				, "INTEGER"			, "BIGINT"		, "string"	}   // KB added Oracle INTEGER
			};

	String requiresPrecision = ",decimal,string,text,ntext,nstring,char,varchar,long varchar,char for bit data,varchar for bit data,long vargraphic,graphic,vargraphic,number,varchar2,numeric,nchar,nvarchar,national character varying,national character,"; 
	
	
	//----------------------------------------------------------------
	public ddlDataTypeConvertor(pcDevBoosterSettings im)
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
	
	// prec/scale is only necessary for Oracle
	//----------------------------------------------------------------
	public String getEquivalentDataType( String sTipe , int prec , int scale , rdbmsDatatype.DBMAKE srcDB , rdbmsDatatype.DBMAKE tgtDB)
	//----------------------------------------------------------------
	{
		if( sTipe == null ) sTipe = "null";
		int srcIdx = -1;
		switch ( srcDB )
		{
		case POWERCENTER: { srcIdx = 0; break; }
		case DB2     	: { srcIdx = 1; break; }
		case ORACLE  	: { srcIdx = 2; break; }
		case NETEZZA 	: { srcIdx = 3; break; }
		case FLATFILE	: { srcIdx = 4; break; }
		default : {
			errit("Unsupported source datatype [" + srcDB + "]" );
			return null;
		}
		}
		int tgtIdx = -1;
		switch ( tgtDB )
		{
		case POWERCENTER: { tgtIdx = 0; break; }
		case DB2     	: { tgtIdx = 1; break; }
		case ORACLE  	: { tgtIdx = 2; break; }
		case NETEZZA 	: { tgtIdx = 3; break; }
		case FLATFILE	: { tgtIdx = 4; break; }
		default : {
			errit("Unsupported target datatype [" + tgtDB + "]" );
			return null;
		}
		}
		// for ORacle you need to premanipulate
		String sLeft = sTipe.trim().toUpperCase();
		if ( srcDB == rdbmsDatatype.DBMAKE.ORACLE ) {    // quick fix for ORCL NZ numerics   KB
			/*
			if( tgtDB == rdbmsDatatype.DBMAKE.NETEZZA ) {
				if( (sLeft.compareToIgnoreCase("NUMBER") == 0) && (scale > 0) ) {  
					sLeft = "NUMBERPS";
					return "DECIMAL";
				}
				else
				if( (sLeft.compareToIgnoreCase("NUMBER") == 0) && (scale == 0) ) {
					sLeft = "NUMBER-19";
					return "BIGINT";
				}	
			}
			*/
			//else {
			 if( (sLeft.compareToIgnoreCase("NUMBER") == 0) && (scale <= 0) ) {
				int hint = 0;
				if( prec <= 3 ) hint = 3;
				else
				if( prec <= 6 ) hint = 6;
				else
				if( prec <= 11) hint = 11;
				else
				if( prec <= 19) hint = 19;
				else hint = 0;
				if( hint != 0 ) sLeft += "-" + hint;
				// overrule if 15
				if( prec == 15 ) sLeft = "NUMBER";  // prec 15 is the Oracle NUMBER; which is a double in informatica
			 }
			 if( (sLeft.compareToIgnoreCase("NUMBER") == 0) && (scale > 0) ) {
				sLeft = "NUMBERPS";
			 }
			 //logit(9,sLeft);
			//}
		}
		//
		int heigth = conv.length;
		int width = conv[0].length;
		String sRet = null;
		for(int i=1 ; i< heigth ; i++ )
		{
           if( conv[i][srcIdx].compareToIgnoreCase(sLeft) == 0 ) {
        	   sRet = conv[i][tgtIdx];
        	   break;
           }
		}
		if( sRet == null ) {
			errit("Cannot convert [" + sLeft + "] : not known for [" + srcDB + "]" );
			return null;
		}
		return sRet;
	
	}
	
	//----------------------------------------------------------------
	public boolean expectPrecision(String sTipe)
	//----------------------------------------------------------------
	{
		String sLookUp = "," + sTipe.toLowerCase().trim() + ",";
		if( requiresPrecision.indexOf(sLookUp) < 0) return false;
		return true;
	}
}
