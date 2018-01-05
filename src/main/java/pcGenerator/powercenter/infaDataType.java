package pcGenerator.powercenter;

import generalpurpose.pcDevBoosterSettings;
import pcGenerator.ddl.rdbmsDatatype;


/*
 * 
 *  Propagator to Flat file :  DB = DB2 en Usage = SOURCEQUALIFIER
 *  Flat file into SST  :  DB is FLat File en Usage is SOURCEQUALIFIER
 *  PowerDesigner       :  DB is DB2 en Usage is SOURCE maar ook Target
 * 
 */

public class infaDataType {

	pcDevBoosterSettings xMSet = null;
	
	enum DT_USAGETYPE { SOURCE , TARGET , SOURCEQUALIFIER , UNKNOWN }
	
    //			
	public boolean isValid = true;
	public rdbmsDatatype.DBMAKE  rdbmsmake = rdbmsDatatype.DBMAKE.UNKNOWN;
	public DT_USAGETYPE usagetipe = DT_USAGETYPE.UNKNOWN;
	public String DataType = null;
	public String DataTypeDisplay = null;
	public int    Precision = -1;
	public int    Scale = -1;
	public int    Length = 1;
	public int    PhysicalLength = -1;
	
	private boolean DEBUG = false;
	private boolean LengthMustBeCalculated = true;
	private rdbmsDatatype rdbmsdt = null;
	
	//----------------------------------------------------------------
	public infaDataType (pcDevBoosterSettings im , String iRDBMS , String iusgtipe , String iDataType , int ip , int is )
	//----------------------------------------------------------------
	{
		xMSet = im;
		//
		rdbmsdt = new rdbmsDatatype(xMSet);
		//
		rdbmsmake = getRDBMSMake( iRDBMS );
		if( rdbmsmake == rdbmsDatatype.DBMAKE.UNKNOWN ) isValid = false;
		//
		usagetipe = getUsageTipe( iusgtipe );
		if( usagetipe == null ) isValid = false;
		if( usagetipe == DT_USAGETYPE.UNKNOWN ) isValid = false;
		//
		DataType = getRDBMSDataType( rdbmsmake , iDataType );
		if( DataType == null ) isValid = false;
		DataTypeDisplay = DataType;
		Precision = ip;
		Scale = is;
		Length = -1;
		PhysicalLength = -1;
		if( calculateInfaSizes() == false ) isValid = false;
		if( determineDisplay() == false ) isValid = false;
		if( checkFinal(ip,is) == false ) isValid = false;
		if( DEBUG ) show();
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
	public void show()
	//----------------------------------------------------------------
	{
	  String sRet = "";
	  sRet = "[" + rdbmsmake + "][" + usagetipe + "][" + DataType + "][Prec=" + Precision + "][Scale=" + 
	          Scale + "][PhysLen=" + PhysicalLength + "][Len=" + Length + "]["+DataTypeDisplay +"]";
	  logit( 5 , sRet);
	}

	//----------------------------------------------------------------
	private boolean checkFinal(int inPrecision , int inScale)
	//----------------------------------------------------------------
	{
		String sErr = "";
		if( Precision <= 0 ) sErr += "[Invalid Precision=" + Precision + "] ";
		if( Scale     < 0 )  sErr += "[Invalid Scale=" + Scale + "] ";
		if( LengthMustBeCalculated ) {
		if( Length    < 0 )  sErr += "[Invalid Length=" + Length + "] ";
		if( PhysicalLength < 0 ) sErr += "[Invalid PhysicalLength=" + PhysicalLength + "] ";
		}
		if( sErr.length() == 0 ) return true;
		sErr =  "(checkFinal) Error [" + rdbmsmake + "][" + usagetipe + "][" + DataType + "] [Precison provided=" + inPrecision + "] [Scale provided=" + inScale + "] " + sErr;
		errit(sErr);
		return true;
	}
	
	//----------------------------------------------------------------
	private DT_USAGETYPE getUsageTipe( String it)
	//----------------------------------------------------------------
	{
		String sRet = it.trim().toUpperCase();
		if( sRet.compareToIgnoreCase("SOURCE") == 0 ) return DT_USAGETYPE.SOURCE;
		if( sRet.compareToIgnoreCase("TARGET") == 0 ) return DT_USAGETYPE.TARGET;
		if( sRet.compareToIgnoreCase("SOURCEQUALIFIER") == 0 ) return DT_USAGETYPE.SOURCEQUALIFIER;
		//
		errit("Unsupported usage type [" + it + "]");
		return DT_USAGETYPE.UNKNOWN;
	}
	
	//----------------------------------------------------------------
	private rdbmsDatatype.DBMAKE getRDBMSMake (String iRDBMS)
	//----------------------------------------------------------------
	{
		
		String sRet = iRDBMS.trim().toUpperCase();
		if( sRet.compareToIgnoreCase("FLAT FILE") == 0 ) return rdbmsDatatype.DBMAKE.FLATFILE;
		if( sRet.compareToIgnoreCase("FLAT_FILE") == 0 ) return rdbmsDatatype.DBMAKE.FLATFILE;
		if( sRet.compareToIgnoreCase("FLATFILE") == 0 ) return rdbmsDatatype.DBMAKE.FLATFILE;
		if( sRet.compareToIgnoreCase("POWERCENTER") == 0 ) return rdbmsDatatype.DBMAKE.POWERCENTER;
		if( sRet.compareToIgnoreCase("DB2") == 0 ) return rdbmsDatatype.DBMAKE.DB2;
		if( sRet.compareToIgnoreCase("NETEZZA") == 0 ) return rdbmsDatatype.DBMAKE.NETEZZA;
		if( sRet.compareToIgnoreCase("ORACLE") == 0 ) return rdbmsDatatype.DBMAKE.ORACLE;
		//
		errit("Unsupported RDBMS [" + iRDBMS + "]");
		return rdbmsDatatype.DBMAKE.UNKNOWN;
		
	}
	
	//----------------------------------------------------------------
	private String getRDBMSDataType( rdbmsDatatype.DBMAKE tipe , String sDataType )
	//----------------------------------------------------------------
	{
		switch( tipe )
		{
		case POWERCENTER : ;
		case FLATFILE    : return rdbmsdt.getINFADataTypeString( sDataType);
		case DB2         : return rdbmsdt.getDB2DataTypeString( sDataType );
		case ORACLE      : return rdbmsdt.getOracleDataTypeString( sDataType );
		case NETEZZA     : return rdbmsdt.getNetezzaDataTypeString( sDataType );
		default : {
			errit("(getRDBMSDataType) Unsupported RDBMS [" + tipe + "] to check datatype [" + sDataType + "]");
			return null;
		}
		}
	}
	
	//----------------------------------------------------------------
	public boolean calculateInfaSizes()
	//----------------------------------------------------------------
	{
	    switch( usagetipe )
		{
		case SOURCE   : return calculateSource();
		case TARGET   : return calculateTarget();
		case SOURCEQUALIFIER   : return calculateSourceQualifier();
		default : {
			errit("Unsupported usage type [" + usagetipe + "] size cannot be calcualted");
			return false;
		}
		}
	}
	
	//----------------------------------------------------------------
	private boolean determineDisplay()
	//----------------------------------------------------------------
	{
		switch( rdbmsmake )
		{
		case POWERCENTER : break;
		default : {	DataTypeDisplay = DataType; return true; }
		}
		rdbmsDatatype.DT_INFA tipe = rdbmsdt.getINFADataType( DataType );
		if( tipe == null ) {
			errit("determineDisplay) Invalid datatype [" + DataType + "]" );
			return false;
		}
		switch( tipe )
		{
		case BIGINT       : { DataTypeDisplay = "bigint"; break; }
		case BINARY       : { DataTypeDisplay = "binary"; break; }
		case DATETIME     : { DataTypeDisplay = "date/time"; break; }
		case DECIMAL      : { DataTypeDisplay = "decimal"; break; }
		case DOUBLE       : { DataTypeDisplay = "double"; break; }
		case INTEGER      : { DataTypeDisplay = "integer"; break; }
		case NSTRING      : { DataTypeDisplay = "nstring"; break; }
		case NTEXT        : { DataTypeDisplay = "ntext"; break; }
		case SMALLINTEGER : { DataTypeDisplay = "small integer"; break; }
		case STRING       : { DataTypeDisplay = "string"; break; }
		case TEXT         : { DataTypeDisplay = "text"; break; }
		default : {
			errit("determine display) datatype [" + tipe + "] currently not supported. Contact KB" );
			return false;
		}
		}
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean calculateSource()
	//----------------------------------------------------------------
	{
		switch( rdbmsmake )
		{
		case DB2      : return calculateDB2Source();
		case ORACLE   : return calculateORCLSource(); 
		case NETEZZA  : return calculateNetezzaSource();
		case FLATFILE : return calculateFlatFileSource();
		case POWERCENTER : {
			errit("A PowerCenter Source does not make any sense at all");
			return false;
		}
		default : {
			errit("(calculateSource) calculate Source - unsupported RDBMS [" + rdbmsmake + "]" );
			return false;
		}
		}
	}
	
	//----------------------------------------------------------------
	public boolean calculateTarget()
	//----------------------------------------------------------------
	{
		switch( rdbmsmake )
		{
		case NETEZZA  : return calculateNetezzaTarget();
		case ORACLE   : return calculateOracleTarget();
		default : {
			errit("(calculateTarget) calculate TARGET - unsupported RDBMS [" + rdbmsmake + "]" );
			return false;
		}
		}
	}
	
	//----------------------------------------------------------------
	public boolean calculateSourceQualifier()
	//----------------------------------------------------------------
	{
		LengthMustBeCalculated = false;
		switch( rdbmsmake )
		{
		case NETEZZA : ;
		case ORACLE  : ;
		case DB2     : {
			errit("WARNING - Irrelevant for size calculation [RDBMS=" + rdbmsmake + "] [Usage=SOURCEQUALIFIER] does not exist" );
			return true;  // niet nodig iets te berekenen DB - SQ bestaat niet
		}
		case FLATFILE    : ;
		case POWERCENTER : return calculateSourceQualifierPowerCenter();
		default : {
			errit("(calculateSourceQualifier) calculate Source qualifier - unsupported RDBMS [" + rdbmsmake + "]" );
			return false;
		}
		}
	}

	
	// Physical Length and Length are NOT APPLICABLE voor een SQ
	//----------------------------------------------------------------
	private boolean calculateSourceQualifierPowerCenter()
	//----------------------------------------------------------------
	{
		//System.out.println("DATATYPE [" + DataType + "] [" + usagetipe + "] [" + rdbmsmake + "]");
		rdbmsDatatype.DT_INFA tipe = rdbmsdt.getINFADataType( DataType );
		if( tipe == null ) {
			errit("calculateSourceQualifierPowerCenter) Invalid datatype [" + DataType + "]" );
			return false;
		}
		switch( tipe )
		{
		case BIGINT : {
			Precision = 19;
			Scale = 0;
			break;
		   }
		case BINARY : {
			Precision = Precision;
			Scale = 0;
			break;
		   }
		case DATETIME : {
			Precision = 29;
			Scale = 9;
			break;
		   }
		case DECIMAL : {
			Precision = Precision;
			Scale = Scale;
			break;
		   }
		case DOUBLE : {
			Precision = 15;
			Scale = 0;
			break;
		   }
		case INTEGER : {
			Precision = 10;
			Scale = 0;
			break;
		   }
		case NSTRING : {
			Precision = Precision;
			Scale = 0;
			break;
		   }
		case NTEXT : {
			Precision = Precision;
			Scale = 0;
			break;
		   }
		case REAL : {
			errit("KB - you forgot to investigate REAL specifications");
			return false;
		   }
		case SMALLINTEGER : {
			Precision = 5;
			Scale = 0;
			break;
		   }
		case STRING : {
			Precision = Precision;
			Scale = 0;
			break;
		   }
		case TEXT : {
			Precision = Precision;
			Scale = 0;
			break;
		   }
		default : {
			errit("(calculateSourceQualifierPowerCenter) datatype [" + tipe + "] currently not supported. Contact dev team." );
			return false;
		}
		}
		return true;
	}
	
	// See analysis document on DB2 SOURCE lengths
	//----------------------------------------------------------------
	private boolean calculateDB2Source()
	//----------------------------------------------------------------
	{
		//System.out.println("DATATYPE [" + DataType + "] [" + usagetipe + "] [" + rdbmsmake + "]");
		rdbmsDatatype.DT_DB2 tipe = rdbmsdt.getDB2DataType( DataType );
		if( tipe == null ) {
			errit("calculateDB2Source) Invalid datatype [" + DataType + "]" );
			return false;
		}
		switch( tipe )
		{
		case BIGINT  : {
			Precision = 19;
			Scale = 0;
			PhysicalLength = Precision; 
			Length = 20;
			break;
		 }
		case BLOB  : {
			Precision = Precision;
			Scale = 0;
			PhysicalLength = Precision; 
			Length = 2 * Precision;
			break;
		 }
		case CHAR  : {
			Precision = Precision;
			Scale = 0;
			PhysicalLength = Precision; 
			Length = 0;
			break;
		 }
		case CHAR_FOR_BIT_DATA  : {
			Precision = Precision;
			Scale = 0;
			PhysicalLength = Precision; 
			Length = 2 * Precision;
			break;
		 }
		case CLOB  : {
			Precision = Precision;
			Scale = 0;
			PhysicalLength = Precision; 
			Length = 0;
			break;
		 }
		case DATE  : {
			Precision = 10;
			Scale = 0;
			PhysicalLength = Precision; 
			Length = 19;
			break;
		 }
		case DECIMAL  : {
			Precision = Precision;
			Scale = Scale;
			PhysicalLength = Precision; 
			Length = Precision + 2;
			break;
		 }
		case REAL : ;
		case FLOAT  : {
			Precision = 15;
			Scale = 0;
			PhysicalLength = Precision; 
			Length = 24;
			break;
		 }
		case INTEGER  : {
			Precision = 10;
			Scale = 0;
			PhysicalLength = Precision; 
			Length = 11;
			break;
		 }
		case SMALLINT  : {
			Precision = 5;
			Scale = 0;
			PhysicalLength = Precision; 
			Length = 6;
			break;
		 }
		case TIMESTMP   : ;
		case TIMESTAMP  : {
			Precision = 26;
			Scale = 6;
			PhysicalLength = Precision; 
			Length = 19;
			break;
		 }
		case VARCHAR  : {
			Precision = Precision;
			Scale = 0;
			PhysicalLength = Precision; 
			Length = 0;
			break;
		 }
		case VARCHAR_FOR_BIT_DATA  : {
			Precision = 10;
			Scale = 0;
			PhysicalLength = Precision; 
			Length = 2 * Precision;
			break;
		 }
		case VARGRAPHIC  : {
			Precision = Precision;
			Scale = 0;
			PhysicalLength = Precision; 
			Length = 0;
			break;
		 }
		case ROWID  : {  // z/Os documenration says 17 - untrusted info
			Precision = 17;
			Scale = 0;
			PhysicalLength = Precision; 
			Length = 0;
			break;
		 }
		default : {
			errit("(calculateDB2Source) datatype [" + tipe + "] currently not supported. Contact dev team." );
			return false;
		}
		}
		return true;
	}
	
	// NOT all types assesses yet
	//----------------------------------------------------------------
	private boolean calculateORCLSource()
	//----------------------------------------------------------------
	{
			//System.out.println("DATATYPE [" + DataType + "] [" + usagetipe + "] [" + rdbmsmake + "]");
			rdbmsDatatype.DT_ORACLE tipe = rdbmsdt.getOracleDataType( DataType );
			if( tipe == null ) {
				errit("calculateDB2Source) Invalid datatype [" + DataType + "]" );
				return false;
			}
			switch( tipe )
			{
			case CHAR  : {
				Precision = Precision;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 0;
				break;
			 }
			case VARCHAR2 : ;
			case VARCHAR  : {
				Precision = Precision;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 0;
				break;
			}
			case LONG  : {
				Precision = 10;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 0;
				break;
			 }
			case INTEGER : ;  // KB 27OCT - ePurche change
			case NUMBER  : {
				Precision = 15;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 24;
				break;
			 }
			case NUMBERPS  : {
				Precision = Precision;
				Scale = Scale;
				PhysicalLength = Precision; 
				Length = Precision + 2;
				break;
			 }
			case DATE  : {
				Precision = 19;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 19;
				break;
			 }
			case TIMESTAMP  : {
				Precision = 26;
				Scale = 6;
				PhysicalLength = Precision; 
				Length = 19;
				break;
			 }
			
			
			default : {
				errit("(calculateOracleSource) datatype [" + tipe + "] currently not supported. Contact dev team." );
				return false;
			}
			}
			return true;
		}
	
	// analyzed on 8 july
	//----------------------------------------------------------------
	private boolean calculateNetezzaSource()
	//----------------------------------------------------------------
	{
			//System.out.println("DATATYPE [" + DataType + "] [" + usagetipe + "] [" + rdbmsmake + "]");
			rdbmsDatatype.DT_NETEZZA tipe = rdbmsdt.getNettezaDataType( DataType );
			if( tipe == null ) {
				errit("calculateNetezzaSource) Invalid datatype [" + DataType + "]" );
				return false;
			}
			switch( tipe )
			{
			case NCHAR  : {
				Precision = Precision;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 0;
				break;
			 }
			case NVARCHAR  : {
				Precision = Precision;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 0;
				break;
			 }
			case BIGINT  : {
				Precision = 19;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 20;
				break;
			 }
			case CHAR  : {
				Precision = Precision;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 0;
				break;
			 }
			case DATE  : {
				Precision = 10;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 19;
				break;
			 }
			case NUMERIC : ;
			case DECIMAL  : {
				Precision = Precision;
				Scale = Scale;
				PhysicalLength = Precision; 
				Length = Precision + 2;
				break;
			 }
			case REAL : {
				Precision = 6;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 24;
				break;
			 } 
			case DOUBLE_PRECISION :
			case DOUBLE :
			case FLOAT  : {
				Precision = 15;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 24;
				break;
			 }
			case INTEGER  : {
				Precision = 10;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 11;
				break;
			 }
			case SMALLINT  : {
				Precision = 5;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 6;
				break;
			 }
			case TIME  : {
				Precision = 15;
				Scale = 6;
				PhysicalLength = Precision; 
				Length = 19;
				break;
			 }
			case TIMESTAMP  : {
				Precision = 26;
				Scale = 6;
				PhysicalLength = Precision; 
				Length = 19;
				break;
			 }
			case VARCHAR  : {
				Precision = Precision;
				Scale = 0;
				PhysicalLength = Precision; 
				Length = 0;
				break;
			 }
			default : {
				errit("(calculateNetezzaSource) datatype [" + tipe + "] currently not supported. Contact dev team." );
				return false;
			}
			}
			return true;
		}
	
	// TODO - TARGET ?? wat houdt dit in 
	//----------------------------------------------------------------
	private boolean calculateNetezzaTarget()
	//----------------------------------------------------------------
	{
			rdbmsDatatype.DT_NETEZZA tipe = rdbmsdt.getNettezaDataType( DataType );
			if( tipe == null ) {
				errit("(calculateNetezzaTarget) Invalid datatype [" + DataType + "]" );
				return false;
			}
			// NZ Target is identical to NZ Source, however no Length and Physical Length
			boolean ib = calculateNetezzaSource();
			if( ib == false ) return false;
			// 
			Length = 0;
			PhysicalLength = Precision;
			return true;
	}	
	// 
	//----------------------------------------------------------------
	private boolean calculateOracleTarget()
	//----------------------------------------------------------------
	{
				rdbmsDatatype.DT_ORACLE tipe = rdbmsdt.getOracleDataType( DataType );
				if( tipe == null ) {
					errit("(calculateOracleTarget) Invalid datatype [" + DataType + "]" );
					return false;
				}
				// Target is supposed to be identical to Source, however no Length and Physical Length
				boolean ib = calculateORCLSource();
				if( ib == false ) return false;
				// 
				Length = 0;
				PhysicalLength = Precision;
				return true;
	}	
	
	//----------------------------------------------------------------
	private boolean calculateFlatFileSource()
	//----------------------------------------------------------------
	{
		// must be string or nstring
		// misbruik daarom DT_INFA type
		//	 CURRENTLY ASSUMED ALL FFs are string. So no change on prec/scale");
		rdbmsDatatype.DT_INFA tipe = rdbmsdt.getINFADataType(DataType );
		if( tipe == null ) {
			errit("calculateFlatFileSource) Invalid datatype [" + DataType + "]" );
			return false;
		}
		switch( tipe )
		{
		case NSTRING : {
			Precision = Precision;
			Scale = 0;
			PhysicalLength = Precision;
			Length = Precision;
			break;
		}
		case STRING : {
			Precision = Precision;
			Scale = 0;
			PhysicalLength = Precision;
			Length = Precision;
			break;
		}
		default : {
			errit("(calculateFlatFileSource) datatype [" + tipe + "] currently not supported. Contact dev team." );
			return false;
		}
		}
	    return true;
	
	}
		
}
