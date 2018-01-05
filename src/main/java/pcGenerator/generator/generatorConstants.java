package pcGenerator.generator;

import generalpurpose.pcDevBoosterSettings;
import java.util.ArrayList;


public class generatorConstants {

	public enum SELFTYPE { SRC , TGT , SQ , TRANS }
	
	public enum PORTTYPE { UNKNOWN , SOURCE , SOURCE_QUALIFIER, TARGET , TRANSFORMATION , TOEXTERNAL }
	
	public enum INSTRUCTION_TYPE { UNKNOWN , PASSTHRU , CONST , RTRIM , LTRIM , RLTRIM , 
		                           MAINFRAMEINT , MAINFRAMEDECIMAL , INLINE , 
		                           EDWNULL , NULLTOZERO , NULLTOZEROSTR , NOW , SYSDATE , EDWSIGN ,  EDWTIMESTAMPTOSTR ,
		                           KEY , FKEY , CRC, 
		                           LOWER , UPPER , STOP ,  
		                           NOP , CSSDSSNULL ,
		                           LEFTSTR , RIGHTSTR , PREPEND , APPEND ,
		                           DQBIGINT , DQINTEGER , DQFLOAT , DQDECIMAL , DQDIVDECIMAL , DQTIMESTAMP, DQDATE , DQTIME , 
		                           FKEY_0 , FKEY_1 , FKEY_2 , FKEY_3 , FKEY_4 , FKEY_5 , FKEY_6 , FKEY_7, FKEY_8 , FKEY_9 ,
		                           TSTQUAL }   // 
	
	enum SRC_TGT_TIPE { SOURCE , TARGET , SOURCEQUALIFIER, UNKNOWN}
	
	enum OPTION { UNKNOWN ,
		 // FLat file
		 CODEPAGE, CONSECDELIMITERASONE, DELIMITED, DELIMITERS
		,ESCAPE_CHARACTER, KEEPESCAPECHAR, LINESEQUENTIAL, MULTIDELIMITERSASAND
		,NULLCHARTYPE, NULL_CHARACTER, PADBYTES, QUOTE_CHARACTER
		,REPEATABLE, ROWDELIMITER, SHIFTSENSITIVEDATA, SKIPROWS
		,STRIPTRAILINGBLANKS 
		
		// SOURCE QUAL
		,SQL_QUERY, USER_DEFINED_JOIN, SOURCE_FILTER
		,NUMBER_OF_SORTED_PORTS, TRACING_LEVEL,SELECT_DISTINCT, IS_PARTITIONABLE
        ,OUTPUT_IS_DETERMINISTIC, OUTPUT_IS_REPEATABLE
		
		,PRE_SQL, POST_SQL 
		// Source Target tableattributes
		,BASE_TABLE_NAME
		,SEARCH_SPECIFICATION
		,SORT_SPECIFICATION
		,DATETIME_FORMAT
		,THOUSAND_SEPARATOR
		,DECIMAL_SEPARATOR
		,ADD_CURRENTLY_PROCESSED_FLAT_FILE_NAME_PORT
		}
	
	
	public enum CONSTRAINT  { UNKNOWN , PRIMARY , FOREIGN , UNIQUE }
	
	// TODO read form config file
	
	pcDevBoosterSettings xMSet = null;
	
	//----------------------------------------------------------------
	class genConstant
	//----------------------------------------------------------------
	{
		String Tag;
		String InfaCode;
		String Value;
		genConstant(String st, String sc , String sv)
		{
			Tag   = st;
			InfaCode  = sc;
			Value = sv;
		}
	}
	ArrayList<genConstant> clist = null;
	
	//----------------------------------------------------------------
	public generatorConstants(pcDevBoosterSettings im)
	//----------------------------------------------------------------
	{
		xMSet = im;
		clist = new ArrayList<genConstant>();
		initialize();
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
	public generatorConstants.INSTRUCTION_TYPE getInstructionype(String sD)
	//----------------------------------------------------------------
	{
			    String sRet = ( sD == null ) ? "null" : sD.trim().toUpperCase(); 
				for(int i=0;i<INSTRUCTION_TYPE.values().length;i++)
				{
					if( INSTRUCTION_TYPE.values()[i].toString().compareToIgnoreCase(sRet) == 0 ) return INSTRUCTION_TYPE.values()[i];
				}
				errit("Unsupported instruction  [" + sRet + "]");
				return null;
	}
	
	//----------------------------------------------------------------
	public generatorConstants.OPTION getOption(String sD)
	//----------------------------------------------------------------
	{
		    String sRet = ( sD == null ) ? "null" : sD.trim().toUpperCase(); 
			for(int i=0;i<OPTION.values().length;i++)
			{
				if( OPTION.values()[i].toString().compareToIgnoreCase(sRet) == 0 ) return OPTION.values()[i];
			}
			errit("Unsupported option  [" + sRet + "]");
			return null;
	}
		
	//----------------------------------------------------------------
	public generatorConstants.CONSTRAINT getConstraint(String sD)
	//----------------------------------------------------------------
	{
			    String sRet = ( sD == null ) ? "null" : sD.trim().toUpperCase(); 
				for(int i=0;i<CONSTRAINT.values().length;i++)
				{
					if( CONSTRAINT.values()[i].toString().compareToIgnoreCase(sRet) == 0 ) return CONSTRAINT.values()[i];
				}
				errit("Unsupported option  [" + sRet + "]");
				return null;
	}
	
	//----------------------------------------------------------------
	public pcDevBoosterSettings.TRISTATE expectRval( generatorConstants.INSTRUCTION_TYPE tipe )
	//----------------------------------------------------------------
	{
	    switch( tipe )
	    {
	    case KEY          : ; 
	    case CRC          : ;
	    case CONST        : ; 
	    case FKEY         : ;
	    case MAINFRAMEDECIMAL : ;
	    case DQDECIMAL    : ;
	    case DQDIVDECIMAL : ;
	    case DQDATE       : ;
	    case DQTIME       : ;
	    case DQTIMESTAMP  : ;
	    case TSTQUAL      : ;
	    case EDWTIMESTAMPTOSTR : ;
	    case LEFTSTR      : ;
	    case RIGHTSTR     : ;
	    case PREPEND      : ;
	    case APPEND       : ;
	    case INLINE       : return pcDevBoosterSettings.TRISTATE.YES;
	    //
	    case STOP         : ;
	    case PASSTHRU     : ;
	    case LOWER        : ;
	    case UPPER        : ;
	    case RTRIM        : ;
	    case LTRIM        : ;
	    case RLTRIM       : ;
	    case MAINFRAMEINT : ;
	    case NULLTOZERO   : ;
	    case NULLTOZEROSTR   : ;
	    case NOW          : ;
	    case SYSDATE      : ;
	    case EDWSIGN      : ;
	    case DQBIGINT     : ;
	    case DQFLOAT      : ;
	    case DQINTEGER    : ;
	    case CSSDSSNULL   : ;
	    case EDWNULL : return pcDevBoosterSettings.TRISTATE.NO;
	    default : return pcDevBoosterSettings.TRISTATE.NULL;
	    }
	}
	
	//----------------------------------------------------------------
	private void initialize()
	//----------------------------------------------------------------
	{
		for(int i=0;i<1000;i++)
		{
			genConstant c = null;
			switch( i )
			{
			// FLAT FILE TABLE ATTRIBUTES
            case  0 : { c = new genConstant("FTA_Base_Table_Name"       ,"Base Table Name"         , "")      ; break; }
            case  1 : { c = new genConstant("FTA_Search_Specification"  ,"Search Specification"    , "")      ; break; }
            case  2 : { c = new genConstant("FTA_Sort_Specification"    ,"Sort Specification"      , "")      ; break; }
            case  3 : { c = new genConstant("FTA_Datetime_Format"       ,"Datetime Format"         , "A 19 mm/dd/yyyy hh24:mi:ss"); break; }
            case  4 : { c = new genConstant("FTA_Thousand_Separator"    ,"Thousand Separator"      , "None")  ; break; }
            case  5 : { c = new genConstant("FTA_Decimal_Separator"     ,"Decimal Separator"       , ".")     ; break; }
            case  6 : { c = new genConstant("FTA_Line_Endings"          ,"Line Endings"            , "System default"); break; }
            // FLAT FILE  
            // KB 09SEP LineSequentail / nullchartype and nullchar modified
            case  7 : { c = new genConstant("FF_CodePage"               ,"CodePage"                , "MS1252"); break; }
            case  8 : { c = new genConstant("FF_Consecdelimiterasone"   ,"Consecdelimiterasone"    , "NO")    ; break; }
            case  9 : { c = new genConstant("FF_Delimited"              ,"Delimited"               , "YES")   ; break; }
            case 10 : { c = new genConstant("FF_Delimiters"             ,"Delimiters"              , ";")     ; break; }
            case 11 : { c = new genConstant("FF_EscapeCharacter"        ,"EscapeCharacter"         , "")      ; break; }
            case 12 : { c = new genConstant("FF_Keepescapechar"         ,"Keepescapechar"          , "")      ; break; }
            //case 13 : { c = new genConstant("FF_LineSequential"         ,"LineSequential"          , "NO")    ; break; }
            case 13 : { c = new genConstant("FF_LineSequential"         ,"LineSequential"          , "YES")    ; break; }
            case 14 : { c = new genConstant("FF_Multidelimitersasand"   ,"Multidelimitersasand"    , "NO")    ; break; }
            //case 15 : { c = new genConstant("FF_NullCharType"           ,"NullCharType"            , "ASCII") ; break; }
            case 15 : { c = new genConstant("FF_NullCharType"           ,"NullCharType"            , "BINARY") ; break; }
            //case 16 : { c = new genConstant("FF_Nullcharacter"          ,"Nullcharacter"           , "*")     ; break; }
            case 16 : { c = new genConstant("FF_Nullcharacter"          ,"Nullcharacter"           , "0")     ; break; }
            case 17 : { c = new genConstant("FF_Padbytes"               ,"Padbytes"                , "1")     ; break; }
            case 18 : { c = new genConstant("FF_QuoteCharacter"         ,"QuoteCharacter"          , "NONE")  ; break; }
            case 19 : { c = new genConstant("FF_Repeatable"             ,"Repeatable"              , "NO")    ; break; }
            case 20 : { c = new genConstant("FF_RowDelimiter"           ,"RowDelimiter"            , "10")    ; break; }
            case 21 : { c = new genConstant("FF_ShiftSensitiveData"     ,"ShiftSensitiveData"      , "NO")    ; break; }
            case 22 : { c = new genConstant("FF_Skiprows"               ,"Skiprows"                , "0")     ; break; }
            case 23 : { c = new genConstant("FF_Striptrailingblanks"    ,"Striptrailingblanks"     , "NO")    ; break; }
            // SOURCE QUALIFIER
            case 24 : { c = new genConstant("SQ_Sql Query"              ,"Sql Query"               , "")    ; break; }
            case 25 : { c = new genConstant("SQ_User Defined Join"      ,"User Defined Join"       , "")    ; break; }
            case 26 : { c = new genConstant("SQ_Source Filter"          ,"Source Filter"           , "")    ; break; }
            case 27 : { c = new genConstant("SQ_Number Of Sorted Ports" ,"Number Of Sorted Ports"  , "")    ; break; }
            case 28 : { c = new genConstant("SQ_Tracing Level"          ,"Tracing Level"           , "Normal")    ; break; }
            case 29 : { c = new genConstant("SQ_Select Distinct"        ,"Select Distinct"         , "NO")    ; break; }
            case 30 : { c = new genConstant("SQ_Is Partitionable"       ,"Is Partitionable"        , "NO")    ; break; }
            case 31 : { c = new genConstant("SQ_Pre SQL"                ,"Pre SQL"                 , "")     ; break; }
            case 32 : { c = new genConstant("SQ_Post SQL"               ,"Post SQL"                , "")    ; break; }
            case 33 : { c = new genConstant("SQ_Output is deterministic","Output is deterministic" , "NO")    ; break; }
            case 34 : { c = new genConstant("SQ_Output is repeatable"   ,"Output is repeatable"    , "Never")    ; break; }
            //
			}
			if( c == null ) continue;
			clist.add(c);
		}
	}
	
	//----------------------------------------------------------------
	public String getConstantValueFor(String s)
	//----------------------------------------------------------------
    {
          for(int i=0;i<clist.size();i++)
          {
                if( clist.get(i).Tag.trim().compareToIgnoreCase(s.trim()) == 0) return clist.get(i).Value;
          }
          errit("Cannot locate constant [" + s + "]");
          return null;
    }
	
	
}
