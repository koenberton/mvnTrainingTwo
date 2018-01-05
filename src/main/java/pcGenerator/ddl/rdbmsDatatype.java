package pcGenerator.ddl;

import generalpurpose.pcDevBoosterSettings;


// a class to manage the various RDBMS data types
public class rdbmsDatatype {

	pcDevBoosterSettings xMSet = null;
	
	public enum DBMAKE  { 	FLATFILE, POWERCENTER , DB2 , ORACLE , NETEZZA , UNKNOWN }
	//
	public enum DT_INFA { 	BINARY , STRING , DATETIME , INTEGER , BIGINT , DECIMAL , 
		                  	DOUBLE , NSTRING , REAL , SMALLINTEGER , TEXT , NTEXT  }
	//
	public enum DT_DB2  { 	CHAR_FOR_BIT_DATA ,  CHAR , 
		                  	VARCHAR_FOR_BIT_DATA , VARCHAR , LONG_VARCHAR , 
		                  	VARGRAPHIC , LONG_VARGRAPHIC ,
		                  	TIMESTMP , TIMESTAMP , DATE ,
		                  	INTEGER , SMALLINT , DECIMAL ,  BIGINT , FLOAT , REAL , 
		                  	BLOB , ROWID , CLOB }
	//
	public enum DT_ORACLE { CHAR, NCHAR,
 							VARCHAR, VARCHAR2, NVARCHAR2 ,
 							SMALLINT, LONG ,
 							NUMBER, NUMBERPS ,
 							BINARY_FLOAT,BINARY_DOUBLE,
 							DATE,TIMESTAMP,
 							RAW, NCLOB,
 							UROWID, ROWID , INTEGER}
	//  note. NATIONAL_CHARACTER_VARYING and CHARACTER VARYING are processed in tokenizer
	public enum DT_NETEZZA {BIGINT, INT8, INTEGER, INT4, SMALLINT, INT2, BYTEINT, INT1,
		 					DOUBLE , DOUBLE_PRECISION , 
							NUMERIC, DECIMAL, FLOAT, REAL,
							CHAR, VARCHAR, NVARCHAR, NCHAR,
							DATE, TIME, TIMESTAMP	}
	
	
	
	//----------------------------------------------------------------
	public rdbmsDatatype(pcDevBoosterSettings im)
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
	public DBMAKE getDBMAKEType(String sD)
	//----------------------------------------------------------------
	{
	    String sRet = ( sD == null ) ? "null" : sD.trim().toUpperCase(); 
	    if( sD.trim().compareToIgnoreCase("FLAT_FILE") == 0 ) sRet = "FLATFILE";
	    if( sD.trim().compareToIgnoreCase("FLAT FILE") == 0 ) sRet = "FLATFILE";
		for(int i=0;i<DBMAKE.values().length;i++)
		{
			if( DBMAKE.values()[i].toString().compareToIgnoreCase(sRet) == 0 ) return DBMAKE.values()[i];
		}
		errit("Unsupported database make [" + sRet + "]");
		return null;
	}
		
	//----------------------------------------------------------------
	public String getDatatypeString(DBMAKE im , String sDatatype)
	//----------------------------------------------------------------
	{
		switch( im )
		{
		case NETEZZA     : return getNetezzaDataTypeString( sDatatype );
		case POWERCENTER : return getINFADataTypeString( sDatatype );
		case DB2         : return getDB2DataTypeString( sDatatype );
		case ORACLE      : return getOracleDataTypeString( sDatatype );
		default : {
			errit("getdatatype() - Unsupported database make [" + im + "]");
			return null;
		 }
		}
	}
	
	//----------------------------------------------------------------
	public pcDevBoosterSettings.TRISTATE expectPrecision(DBMAKE im , String sDatatype)
	//----------------------------------------------------------------
	{
		switch( im )
		{
		case NETEZZA: return expectNetezzaPrecision( sDatatype );
		case DB2    : return expectDB2Precision( sDatatype );
		case ORACLE : return expectOraclePrecision( sDatatype );
		default : {
			errit("expectPrecision() - Unsupported database make [" + im + "]");
			return pcDevBoosterSettings.TRISTATE.NULL;
		 }
		}
	}
	
	//----------------------------------------------------------------
	public String getINFADataTypeString(String sD)
	//----------------------------------------------------------------
	{
		 	String sRet = ( sD == null ) ? "null" : sD.trim().toUpperCase(); 
			if( sRet.compareToIgnoreCase("DATE/TIME") == 0 ) sRet = "DATETIME";
			if( sRet.compareToIgnoreCase("SMALL INTEGER") == 0 ) sRet = "SMALLINTEGER";
			for(int i=0;i<DT_INFA.values().length;i++)
			{
				if( DT_INFA.values()[i].toString().compareToIgnoreCase(sRet) == 0 ) return sRet;
			}
			errit("Unsupported INFA datatype [" + sRet + "]");
			return null;
	}
		
	//----------------------------------------------------------------
	public DT_INFA getINFADataType(String sD)
	//----------------------------------------------------------------
	{
		    String sRet = ( sD == null ) ? "null" : sD.trim().toUpperCase(); 
			for(int i=0;i<DT_INFA.values().length;i++)
			{
				if( DT_INFA.values()[i].toString().compareToIgnoreCase(sRet) == 0 ) return DT_INFA.values()[i];
			}
			errit("Unsupported INFA datatype [" + sRet + "]");
			return null;
	}
	
	//----------------------------------------------------------------
	public DT_DB2 getDB2DataType(String sD)
	//----------------------------------------------------------------
	{
		    String sRet = ( sD == null ) ? "null" : sD.trim().toUpperCase(); 
			for(int i=0;i<DT_DB2.values().length;i++)
			{
				if( DT_DB2.values()[i].toString().compareToIgnoreCase(sRet) == 0 ) return DT_DB2.values()[i];
			}
		    errit("Unsupported DB2 datatype [" + sD+ "]");
			return null;
	}
	
	//----------------------------------------------------------------
	public String getDB2DataTypeString( String sD)
	//----------------------------------------------------------------
	{
		    String sRet = ( sD == null ) ? "null" : sD.trim().toUpperCase();  
		    // CHAR FOR BIT DATA en andere type met spaties omzetten naar underscored waarden
			if( sRet.compareToIgnoreCase("CHAR FOR BIT DATA") == 0 ) sRet = "CHAR_FOR_BIT_DATA";
			if( sRet.compareToIgnoreCase("VARCHAR FOR BIT DATA") == 0 ) sRet = "VARCHAR_FOR_BIT_DATA";
			if( sRet.compareToIgnoreCase("LONG VARCHAR") == 0 ) sRet = "LONG_VARCHAR";
			if( sRet.compareToIgnoreCase("LONG VARGRAPHIC") == 0 ) sRet = "LONG_VARGRAPHIC";
		    //	
			for(int i=0;i<DT_DB2.values().length;i++)
			{
				if( DT_DB2.values()[i].toString().compareToIgnoreCase(sRet) == 0 ) return sRet;
			}
			errit("Unsupported DB2 datatype [" + sRet + "]");
			return null;
	}
	
	//----------------------------------------------------------------
	public String getOracleDataTypeString( String sD)
	//----------------------------------------------------------------
	{
		    String sRet = ( sD == null ) ? "null" : sD.trim().toUpperCase();  
		    //	
			for(int i=0;i<DT_ORACLE.values().length;i++)
			{
				if( DT_ORACLE.values()[i].toString().compareToIgnoreCase(sRet) == 0 ) return sRet;
			}
			errit("Unsupported ORACLE datatype [" + sRet + "]");
			return null;
	}
	
	//----------------------------------------------------------------
	public DT_ORACLE getOracleDataType(String sD)
	//----------------------------------------------------------------
	{
		    String sRet = ( sD == null ) ? "null" : sD.trim().toUpperCase(); 
		    for(int i=0;i<DT_ORACLE.values().length;i++)
			{
				if( DT_ORACLE.values()[i].toString().compareToIgnoreCase(sRet) == 0 ) return DT_ORACLE.values()[i];
			}
		    errit("Unsupported ORACLE datatype [" + sD + "]");
			return null;
	}
	
	//----------------------------------------------------------------
	public DT_NETEZZA getNettezaDataType(String sD)
	//----------------------------------------------------------------
	{
			    String sRet = ( sD == null ) ? "null" : sD.trim().toUpperCase(); 
				for(int i=0;i<DT_NETEZZA.values().length;i++)
				{
					if( DT_NETEZZA.values()[i].toString().compareToIgnoreCase(sRet) == 0 ) return DT_NETEZZA.values()[i];
				}
			    errit("Unsupported NETEZZA datatype [" + sD+ "]");
				return null;
	}
		
	//----------------------------------------------------------------
	public String getNetezzaDataTypeString( String sD)
	//----------------------------------------------------------------
	{
			    String sRet = ( sD == null ) ? "null" : sD.trim().toUpperCase();
				for(int i=0;i<DT_NETEZZA.values().length;i++)
				{
					if( DT_NETEZZA.values()[i].toString().compareToIgnoreCase(sRet) == 0 ) return sRet;
				}
				errit("Unsupported Netezza datatype [" + sRet + "]");
				return null;
	}
	
	//----------------------------------------------------------------
	private pcDevBoosterSettings.TRISTATE expectDB2Precision(String sDatatype)
	//----------------------------------------------------------------
	{
		DT_DB2 tipe = this.getDB2DataType(sDatatype);
		if( tipe == null ) return pcDevBoosterSettings.TRISTATE.NULL; // should be impossible
		switch( tipe )
		{
		case CHAR_FOR_BIT_DATA :
		case CHAR :
		case VARCHAR_FOR_BIT_DATA :
		case VARCHAR :
		case LONG_VARCHAR :
		case VARGRAPHIC :
		case LONG_VARGRAPHIC :
		case DECIMAL :
		case BLOB: 
		case CLOB : return pcDevBoosterSettings.TRISTATE.YES;
		default : return pcDevBoosterSettings.TRISTATE.NO;
		}
	}
		
	//----------------------------------------------------------------
	private pcDevBoosterSettings.TRISTATE expectOraclePrecision(String sDatatype)
	//----------------------------------------------------------------
	{
		DT_ORACLE tipe = this.getOracleDataType(sDatatype);
		if( tipe == null ) return pcDevBoosterSettings.TRISTATE.NULL; // should be impossible
		switch( tipe )
		{
		case VARCHAR  :
		case VARCHAR2 :
		case NVARCHAR2 :
		case NUMBERPS :
		case NCLOB  : 
		case CHAR   : 
		case NCHAR  : return pcDevBoosterSettings.TRISTATE.YES;
		default : return pcDevBoosterSettings.TRISTATE.NO;
		}	
	}
		
	//----------------------------------------------------------------
	private pcDevBoosterSettings.TRISTATE expectNetezzaPrecision(String sDatatype)
	//----------------------------------------------------------------
	{
			DT_NETEZZA tipe = this.getNettezaDataType(sDatatype);
			if( tipe == null ) return pcDevBoosterSettings.TRISTATE.NULL; // should be impossible
			switch( tipe )
			{
			case NUMERIC : 
			case DECIMAL : 
			//case FLOAT   : 
			case REAL    :
			case CHAR    : 
			case VARCHAR :  
			case NVARCHAR: 
			case NCHAR   : return pcDevBoosterSettings.TRISTATE.YES;
			default : return pcDevBoosterSettings.TRISTATE.NO;
			}	
	}
	
}
