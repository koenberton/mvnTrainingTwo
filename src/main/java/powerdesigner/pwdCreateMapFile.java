package powerdesigner;


import java.util.ArrayList;
import java.util.StringTokenizer;

import office.xlsxWriter;
import pcGenerator.generator.generatorConstants;
import pcGenerator.generator.generatorSrcTgtManager;
import pcGenerator.powercenter.infaConstraint;
import pcGenerator.powercenter.infaSource;
import pcGenerator.powercenter.infaSourceField;
import generalpurpose.gpPrintStream;
import generalpurpose.pcDevBoosterSettings;

public class pwdCreateMapFile {
	

	private boolean DEBUG = true;
	
	enum MAPTYPE { UNKNOWN , SRC2SST , SST2DVST , DV2DQ }
	
	
	boolean GoNative = true;
	boolean PATCH = false;   // TRUE -> EDW_DQ_INFO is not generated
	boolean CLAIMJOBPATCH = false;   // TRUE = ugly fix for CLAIMCOMMENT and INSPECTORREPORT
	
    pcDevBoosterSettings xMSet=null;
    generatorSrcTgtManager stmngr=null;
    gpPrintStream fout = null;   // map.txt
    xlsxWriter exout = null;
	
    //
    private String CSTR_TYPEOFCONCERN = "00000001";    // modified due to findings FK issues Jan-06
    private String CSTR_COMPANY       = "00000003";    // KB 6JAN
    private String CSTR_SOURCEKEY     = "2";           // KB 6JAN  used to be UCHP
    //
    
    private boolean creaOK           = true;
    private MAPTYPE requestedMapTipe = MAPTYPE.UNKNOWN;
	private String sSourceSystem     = null;
	private String sScope            = null;
	private ArrayList<String> requestedTablesList = null;
	private String MapFileName       = null;
	private String ExcelFileName     = null;
	private int MaxColNameLen        = 0;
	private boolean srcIsFlatFile    = false;
	private ArrayList<pwdTableCache> targetcache_list = null;
	private ArrayList<pwdTableCache> sourcecache_list = null;
	private ArrayList<pwdTableCache> dvstcache_list = null;
	private ArrayList<pwdOverrule>   overrule_list = null;
	private ArrayList<pwdGeneralOverrule>   general_overrule_list = null;
	private String includeFile       = null;
	private readPwdXml.LAYER CRCCreateLayer = readPwdXml.LAYER.SST;    //  later naar een input param omvormen
	private int nwarnings=0;
	private int nerrors=0;
	
	// for column mapping
	class maplink
	{
		int left_idx=-1;
		int right_idx=-1;
		int hits=0;
		maplink(int ileft)
		{
			left_idx=ileft;
			right_idx=-1;
			hits=0;
		}
	}
	ArrayList<maplink> mplist = null;
	
	// for CRC sort
	class ColumnLocation {
		String ColName;
		int location;
	}
	// for TESTQUAL
	class DQTuple {
		String colname;
		String DQFunction;
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
	public pwdCreateMapFile(pcDevBoosterSettings im)
	//----------------------------------------------------------------
	{
		xMSet = im;
		requestedTablesList = new ArrayList<String>();
	}
	
	//----------------------------------------------------------------
	private boolean maakRequestedTableList(String sIn)
	//----------------------------------------------------------------
	{
        if( sIn == null ) return true;
        if( sIn.trim().length() == 0 ) return true;
		if( sIn.trim().toUpperCase().startsWith("TABLES=(") == false ) {
			errit("tablelist syntax :  TABLES=( comma separeated enum without spaces) ");
			return false;
		}
		String sTemp = sIn.substring( "TABLES=(".length() );
		if( sTemp.endsWith( ")" ) == false ) {
			errit("tablelist syntax :  TABLES=( comma separeated enum without spaces) : missing right parenthesis");
			return false;
		}
		sTemp = (sTemp.substring(0,sTemp.length()-1)).trim();
		StringTokenizer st = new StringTokenizer( sTemp , ",");
		while(st.hasMoreTokens()) 
		{ 
		  String sElem = st.nextToken().trim().toUpperCase();
		  requestedTablesList.add( sElem );
		}		  
		if( requestedTablesList.size() == 0 ) {
		  errit("There are no tables defined in TABLES=()");
		  return false;
		}
		// ALL => reset
		if( requestedTablesList.get(0).compareToIgnoreCase("ALL") == 0 ) {
			requestedTablesList = null;
			requestedTablesList = new ArrayList<String>();
		}
		logit(5, "Tables requested [#=" + requestedTablesList.size() + "] : " + requestedTablesList.toString() );
		return true;
	}
	
	//----------------------------------------------------------------
	public boolean create_map_file( String sTipe , String sSrcSysIn , String[] args)
	//----------------------------------------------------------------
	{
		long starttme= System.currentTimeMillis();
		creaOK=true;
		//
		sSourceSystem = sSrcSysIn;
		//
		requestedMapTipe = null;
		if( sTipe.compareToIgnoreCase("SRC2SST") == 0 ) requestedMapTipe = MAPTYPE.SRC2SST;
		if( sTipe.compareToIgnoreCase("SST2DVST") == 0 ) requestedMapTipe = MAPTYPE.SST2DVST;
		if( sTipe.compareToIgnoreCase("DV2DQ") == 0 ) requestedMapTipe = MAPTYPE.DV2DQ;
		if( requestedMapTipe == null ) {
			creaOK =false;
			errit("Types supported{SRC2SST,SST2DVST,DV2DQ}");
			return false;
		}
		
		// SCOPE=  SRCTYPE=  TABLES=  
		sScope = null;
		boolean gotSrcType=false;
		for(int i=0;i<args.length;i++)
		{
		  String cmd = args[i].toUpperCase();
		  // SCOPE=
		  if( cmd.startsWith("SCOPE=") ) {
				sScope = args[i].trim();
				sScope = sScope.substring( "SCOPE=".length());
				if( sScope.length() == 0 ) sScope = null;
			    continue;
		  }
		  // TABLES=
		  else
		  if( cmd.startsWith("TABLES=") ) {
			  if( maakRequestedTableList( args[i] ) == false ) {
					creaOK =false;
					return false;
				}
			    continue;
		  }
		  // SRCTYPE
		  else
		  if( cmd.startsWith("SRCTYPE=") ) {
			    String sFla = args[i];
			    gotSrcType=true;
			    if( sFla.compareToIgnoreCase("SRCTYPE=FLATFILE") == 0 ) {
					srcIsFlatFile=true;
				}
				else
				if( sFla.compareToIgnoreCase("SRCTYPE=DIRECT") == 0 ) {
				   srcIsFlatFile = false;		
				}	
				else {
					creaOK =false;
					errit("SRC defintion state syntax SRC={DIRECT|FLATFILE}");
					return false;	
				}
			    continue;
		  }
		  else 
		  if( cmd.startsWith("INCLUDE=") ) {
			  String sF = args[i].substring( "INCLUDE=".length());
			  includeFile = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Import" + xMSet.xU.ctSlash + sF;
			  if( xMSet.xU.IsBestand( includeFile ) == false ) {
				  creaOK =false;
					errit("Cannot locate include file [" + includeFile + "]");
					return false;	
			  }
		  }
		  else 
		  {
			  errit("Unsupported commandline option [" + args[i] + "]  {INCLUDE,SRCTYE,TABLES, SCOPE}");
			  creaOK =false;
			  return false;
		  }
		}
		if( !gotSrcType ) {
			creaOK =false;
			errit("SRC defintion SRC={DIRECT|FLATFILE} is missing");
			return false;
		}
		//
		if( (cacheLookUps() == false) || (creaOK == false) ) return false;
		//
		if( loadOverrule() == false || (creaOK == false) ) return false;
		// 
		determineMaxColNameLen();
		//
		if( mainCreationLoop() == false ) return false;
		//
		starttme = System.currentTimeMillis() - starttme;
		logit(1,"Map file [" + MapFileName + "] created succesfully [Elapsed=" + (starttme/1000) + "s] [#Warnings=" + nwarnings + "] [#Errors=" + nerrors + "]");
		return true;
	}
	
	//----------------------------------------------------------------
	private void determineMaxColNameLen()
	//----------------------------------------------------------------
	{
		for(int i=0;i<targetcache_list.size();i++)
		{
			pwdTableCache t = targetcache_list.get(i);
			for(int j=0;j<t.collist.size();j++)
			{
				if( t.collist.get(j).ColumnName.length() > MaxColNameLen ) MaxColNameLen = t.collist.get(j).ColumnName.length();
			}
		}
		logit(5,"MaxColNameLen [" + MaxColNameLen + "]");
		if( MaxColNameLen < 35 )  MaxColNameLen = 35;
	}

	//----------------------------------------------------------------
	private boolean loadOverrule()
	//----------------------------------------------------------------
	{
	   if( includeFile == null ) return true;
	   pwdReadOverrule ro = new pwdReadOverrule(xMSet);
	   overrule_list = ro.getOverrules(includeFile);
       if( overrule_list == null ) return false;
       for(int i=0;i<overrule_list.size();i++)
       {
    	   if( i == 0 ) logit(5,"Overrule instructions");
    	   pwdOverrule x = overrule_list.get(i);
    	   logit(5, " [SRC=" + x.isSource + "] <" + x.TableName + "." + x.ColumnName + "> " + x.leftSide + " " + x.rightSide + " " + x.Construct );
           //errit(" [SRC=" + x.isSource + "] <" + x.TableName + "." + x.ColumnName + "> " + x.leftSide + " " + x.rightSide + " " + x.Construct );
       }
       //
       general_overrule_list = ro.getGeneralOverrules();
       if( general_overrule_list == null ) return false;
       for(int i=0;i<general_overrule_list.size();i++)
       {
    	   if( i == 0 ) logit(5,"General Overrule instructions");
    	   pwdGeneralOverrule x = general_overrule_list.get(i);
    	   logit( 5 , x.code + " -> " + x.value );
       }
       return true;
	}
	
	//----------------------------------------------------------------
	private String getTargetLayer()
	//----------------------------------------------------------------
	{
		   switch( requestedMapTipe )
		   {
		   case SRC2SST  : return "sst";
		   case SST2DVST : return "dvst"; 
		   case DV2DQ    : return "dq";
		   default : {
			   errit("(getTargetLayer) Unsupported MapType " + requestedMapTipe );
			   return null;
		       }
		   }
	}
	
	//----------------------------------------------------------------
	private String getSourceLayer()
	//----------------------------------------------------------------
	{
			   switch( requestedMapTipe )
			   {
			   case SRC2SST  : {
				   if( (srcIsFlatFile==true) && (sSourceSystem.compareToIgnoreCase("UCHP")==0) ) return "orclext";
				   return "src";
			   }
			   case SST2DVST : return "sst"; 
			   case DV2DQ    : return "stov";   
			   default : {
				   errit("(getSourceLayer) Unsupported MapType");
				   return null;
			       }
			   }
	}
		
	//----------------------------------------------------------------
	private boolean IsTableRequested(String tab )
	//----------------------------------------------------------------
	{
		   if( requestedTablesList.size() == 0 )  return true;   // all tables are requested
		   for(int i=0 ; i<requestedTablesList.size(); i++ )
		   {
			   if( requestedTablesList.get(i).compareToIgnoreCase( tab ) == 0 ) return true;
		   }
		   return false;
	}
	
	//----------------------------------------------------------------
	private boolean cacheLookUps()
	//----------------------------------------------------------------
	{
		   String sTgtLayer = getTargetLayer();
		   if( sTgtLayer == null ) return false;
		   //  Target cache
		   String sDir = xMSet.getProjectDir() + xMSet.xU.ctSlash + "LookUp" + xMSet.xU.ctSlash;
		   String FName = sDir + "Lkp_" + (sSourceSystem + "_" + sTgtLayer).trim().toLowerCase() + ".xml";
		   pwdReadCache rdr = new pwdReadCache(xMSet);
		   targetcache_list = rdr.getCachedLookUps(FName);
		   if( targetcache_list == null ) {
			   errit("Eror reading cached information from [" + FName + "]");
			   return false;
		   }
		   // remove the tables not in scope from the cache (omgekeerd)
		   int aantal = targetcache_list.size();
		   for(int k=0;k<aantal;k++)
		   {
			   for(int i=0;i< targetcache_list.size();i++)
			   {
				   if( IsTableRequested(targetcache_list.get(i).Name.trim()) ) continue;
				   targetcache_list.remove(i);
				   break;
			   }
		   }
		   // quality check - see if all requested tables are covered
		   for(int i=0 ; i<requestedTablesList.size(); i++ )
		   {
			   String stab = requestedTablesList.get(i).trim().toUpperCase();
			   for(int j=0;j<targetcache_list.size();j++)
			   {
				   if( stab.compareToIgnoreCase(targetcache_list.get(j).Name.trim()) == 0 ) {
					   stab=null;
					   break;
				   }
			   }
			   if( stab != null ) {
				   errit("Could not find table in Targetcache [" + stab + "]");
			   }
		   }
		   // Source cache
		   String sSrcLayer = getSourceLayer();
		   if( sSrcLayer == null ) return false;
		   FName = sDir + "Lkp_" + (sSourceSystem + "_" + sSrcLayer).trim().toLowerCase() + ".xml";
		   pwdReadCache xdr = new pwdReadCache(xMSet);
		   sourcecache_list = xdr.getCachedLookUps(FName);
		   if( sourcecache_list == null ) {
			   errit("Eror reading cached information from [" + FName + "]");
			   return false;
		   }
		   //
		   //  DVST cache 
		   FName = sDir + "Lkp_" + (sSourceSystem + "_DVST").trim().toLowerCase() + ".xml";
		   pwdReadCache ddr = new pwdReadCache(xMSet);
		   dvstcache_list = ddr.getCachedLookUps(FName);
		   if( dvstcache_list == null ) {
			   errit("Eror reading cached information from [" + FName + "]");
			   return false;
		   }
		   //
		   logit(5,"[Target cache=" + targetcache_list.size() + "] [sourcecache=" + sourcecache_list.size() + "] [DVST cache=" + dvstcache_list.size() + "]");
		   return true;
	 }
		
		
	//----------------------------------------------------------------
	private boolean open_mapfile()
	//----------------------------------------------------------------
	{
		String FName = "Map_" + this.requestedMapTipe + "_" + this.sSourceSystem;
		String sDir = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Temp" + xMSet.xU.ctSlash;
		MapFileName = sDir + FName + ".txt";
		ExcelFileName = sDir + FName + ".xlsx";
		fout = new gpPrintStream ( MapFileName, "UTF-8");
		exout = new xlsxWriter(xMSet);
		if( exout.addSheet("Map") == false ) return false;
		
		// header
		writeline("--");
		writeline("-- Generated MAP file");
		writeline("-- Application     : " + xMSet.getApplicationId() );
	    writeline("-- Source          : " + sSourceSystem );
	    writeline("-- Project         : " + xMSet.getCurrentProject() );
	    writeline("-- Map Type        : " + requestedMapTipe );
	    writeline("-- Native Datatype : " + GoNative );
	    writeline("-- CRC Created at  : " + CRCCreateLayer);
	    writeline("-- Created on      : " + xMSet.xU.prntStandardDateTime(System.currentTimeMillis()) );
	    writeline("-- Created by      : " + xMSet.whoami() );
	    writeline("--");
	    //
	    return true;
	}
	
	//----------------------------------------------------------------
	private boolean mainCreationLoop()
	//----------------------------------------------------------------
	{
		if( open_mapfile() == false ) return false;
	    //
		for(int i=0;i<targetcache_list.size();i++)
		{
		    pwdTableCache tc = targetcache_list.get(i);
		    //errit( tc.Name );
		    if( IsTableRequested( tc.Name ) == false ) {
				logit( 5 , "Skipping [" + tc.Name + "] Not requested");
				continue;   // always true because list is already filtered
			}
			if( sScope != null ) {
				String cr = (tc.CR == null) ? "" : tc.CR.trim();
				if( cr.compareToIgnoreCase( sScope ) != 0 ) {
					logit( 5 , "Table [" + tc.Name + "] not in scope [" + cr + "] [" + sScope + "]");
					continue;
				}
			}
			//errit("attempt " + tc.Name);
			if( createMapping(i) == false ) {
				logit( 5 , "Map for [" + tc.Name + "] could not be created");
				break;
			}
		}
		if( fout != null ) fout.close();
		if( exout != null ) exout.dumpToExcel(ExcelFileName);
		return true;
	}
	
	
	// map the Powerdesign name onto the Oracle or other source table name
	//----------------------------------------------------------------
	private String getSourceTableNameViaTargetTableName( String targettabname )
	//----------------------------------------------------------------
	{
		if( targettabname == null ) {
			errit("(getSourceTableNameViaTargetTableName) System Error - input is null");
			return null;
		}
		if( sourcecache_list == null ) {
			errit("(getSourceTableNameViaTargetTableName) System Error - sourcecache list is not initialized");
			return null;
		}
		try {
		for(int i=0;i<sourcecache_list.size();i++)
		{
			if(  sourcecache_list.get(i).Name == null ) {
				errit("(getSourceTableNameViaTargetTableName) System Error - NULL name on sourcecache_list.get(" + i + ")");
				return null;
			}
			if( sourcecache_list.get(i).Name.trim().compareToIgnoreCase(targettabname) == 0 ) {
				if( sourcecache_list.get(i).SourceTableName != null )	return sourcecache_list.get(i).SourceTableName.trim().toUpperCase();
				return sourcecache_list.get(i).Name;
			}
		}
		return null;
		}
		catch(Exception e ) {
			errit("(getSourceTableNameViaTargetTableName) System Error" + xMSet.xU.LogStackTrace(e));
			return null;
		}
	}
	
	//----------------------------------------------------------------
	private String getSourceTableName(pwdTableCache tab)
	//----------------------------------------------------------------
	{
		 String sName=null;
		 switch( requestedMapTipe )
		   {
		   case SRC2SST  : {
			   String sFF = ( srcIsFlatFile == true ) ? "FF_" : "";
			   if( this.sourcecache_list != null ) {
				   if( DEBUG ) errit("attempting -> " + tab.Code );
				   sName = getSourceTableNameViaTargetTableName( tab.Code );
				   if( sName == null ) return null;
				   sName = sFF + sName;
			   }
			   else sName = sFF + tab.Code ; 
			   break; 
			}
		   case SST2DVST : { 
			                  //sName = "SST_" + tab.Name;
			                  sName = "SST_" + sSourceSystem + "_" + tab.Name;   // KB 9 nov
			                  break; }
		   case DV2DQ    : { sName = "STOV_" + sSourceSystem + "_" + tab.Name + "_ACT"; break; }
		   default : {
			   errit("(getSourceTableName) Unsupported MapType " + requestedMapTipe);
	           break;		   
		       }
		   }
		   if ( sName==null ) errit("Cannot create sourcetablename for target [" + tab.Name + "]");
		   return sName;
	}
	
	//----------------------------------------------------------------
	private String getTargetTableName(pwdTableCache tab)
	//----------------------------------------------------------------
	{
			 String sName=null;
			 switch( requestedMapTipe )
			   {
			   case SRC2SST  : { 
				                 //sName = "SST_" + tab.Name ; 
				                 sName = "SST_" + sSourceSystem + "_" + tab.Name;
				                 break; }
			   case SST2DVST : { sName = "DVST_" + sSourceSystem + "_" + tab.Name; break; }
			   case DV2DQ    : { //sName = "DQ_" + tab.Name;
				                 sName = "DQ_" + sSourceSystem + "_" + tab.Name;
			                     break;}
			   default : {
				   errit("(getTargetTableName) Unsupported MapType " + requestedMapTipe);
		           break;		   
			       }
			   }
			   if ( sName==null ) errit("Cannot create sourcetablename for [" + tab.Name + "]");
			   return sName;
	}
	
	//----------------------------------------------------------------
	private boolean doINeedToTruncate()
	//----------------------------------------------------------------
	{
		for(int i=0;i<general_overrule_list.size();i++)
		{
			pwdGeneralOverrule ge = general_overrule_list.get(i);
			if( ge.code.compareToIgnoreCase("TRUNCATE") != 0 ) continue;
			if( ge.value.compareToIgnoreCase("TRUE")==0) return true;
			return false;
		}
		return false;
	}
	
	
	//----------------------------------------------------------------
	private String getTimeStampFormat()
	//----------------------------------------------------------------
	{
		for(int i=0;i<general_overrule_list.size();i++)
		{
			pwdGeneralOverrule ge = general_overrule_list.get(i);
			if( ge.code.compareToIgnoreCase("TIMESTAMPFORMAT") != 0 ) continue;
			return ge.value.trim();
		}
		return null;
	}
	
	//----------------------------------------------------------------
	private boolean createMapping( int TgtCacheIdx ) 
	//----------------------------------------------------------------
	{
	    pwdTableCache tc = targetcache_list.get( TgtCacheIdx );
	    //
	    String sSourceTableName = getSourceTableName(tc);
	    if( sSourceTableName == null ) {
	    	errit( "Could not create the source name for [" + tc.Name + "] - Maybe you need to map the SourceTableName to the Powerdesigner name");
	    	writeline("-- Error: [" + tc.Name + "] Could not determine the SourceTableName. Review the mapping of the SourceTableName to the Powerdesigner name");
	    	return true;  // in order not to stop the generator
	    }
	    String sTargetTableName = getTargetTableName(tc);
	    if( sTargetTableName==null)  {
	    	errit( "Could not create the target name for [" + tc.Name + "]");
	    	return false;
	    }
		//
	    if( stmngr != null ) stmngr = null;
	    stmngr = new generatorSrcTgtManager(xMSet);
	    if( stmngr.initializeLists( sSourceTableName , sTargetTableName ) == false ) return false;
	    if( srcIsFlatFile ) {
	    	stmngr.changeToFlatFile( 'S' , 0);
	    }
		if( stmngr.load_SourceAndTargetSpecs() == false ) return false;
	    //
	    
		// make header
		writeline("--");
		this.exout.setColor( xlsxWriter.XCOLOR.LIGHT_RED );
		writeline("MAP: " + sSourceTableName + " TO " + sTargetTableName);
		String sMapName = ("m_" + requestedMapTipe + "_" + tc.Name).toLowerCase();
		writeline("MappingName: " + sMapName );
		if( (requestedMapTipe == MAPTYPE.SRC2SST) && (srcIsFlatFile==true) ) {
			writeline("FlatFiles:" + sSourceTableName );
		}
		this.exout.setColor( xlsxWriter.XCOLOR.NONE );
		//
		//  Flat file options
		if( (requestedMapTipe == MAPTYPE.SRC2SST) && (general_overrule_list != null)) {
			for(int i=0;i<general_overrule_list.size();i++)
			{
				pwdGeneralOverrule ge = general_overrule_list.get(i);
				if( ge.code.compareToIgnoreCase("SOURCEOPTION") == 0 ) writeline( "SOURCEOPTION : " + ge.value );
			}
	    }
		// TRUNCATE
		// Incomplete : you cannot set a PRE SAL on a flat file source qual, so it needs to be on the pre sal target
		// which is not supported yet
		if( (requestedMapTipe == MAPTYPE.SRC2SST)||(requestedMapTipe == MAPTYPE.SST2DVST) ) {
			/*
			if( doINeedToTruncate() ) {
				//SRCQUALOPTION : { 01 , PRE_SQL="truncate table dmst_campaignlabour" }
				writeline( "SRCQUALOPTION : { 01 , PRE_SQL=\"TRUNCATE TABLE " + sTargetTableName.trim().toLowerCase() + "\" }");
			}
			*/
		}		
	    //
		// make column map
		if( requestedMapTipe == MAPTYPE.DV2DQ ) {
			 if( create_DQ_columnMap(TgtCacheIdx) == false ) return false;
		}
		else {
		 if( create_columnMap(TgtCacheIdx) == false ) return false;
		}
		// 
		if( requestedMapTipe == MAPTYPE.SRC2SST ) 
		{
			if( create_CRC_map() == false ) return false;
			if( create_TESTQUAL_map(TgtCacheIdx) == false ) return false;  // KB 26 NOV Native
		}
		// make Keys - fks - crc 
		else
		if( requestedMapTipe == MAPTYPE.SST2DVST ) 
		{
			if( create_CRC_map() == false ) return false;
			if( report_constraints() == false ) return false;
			if( create_primary_key_map() == false ) return false;
			if( create_foreign_key_map() == false ) return false;
		}
	    
		//
		mplist=null;
	    stmngr = null;
		return true;
	}
	
	//----------------------------------------------------------------
	private void writeline( String s1in , String s2in , String s3in)
	//----------------------------------------------------------------
	{
		    //
		    String s1 = (s1in ==  null ) ? "" : s1in;
		    String s2 = (s2in ==  null ) ? "" : s2in;
		    String s3 = (s2in ==  null ) ? "" : s3in;
			//
			String sLijn =  String.format("%-" + MaxColNameLen + "s" , s1 );
			sLijn += " ";
			sLijn += String.format("%-" + MaxColNameLen + "s" , s2 );
			sLijn += " " + s3;
			fout.println( sLijn );
			// Excel
			Object[] oo = new Object[4];
			sLijn = "";
			if( s1.trim().startsWith("--") ) {
				oo[0] = s1.substring(2);
				oo[1] = s2;
				oo[2] = s3;
				oo[3] = "";
				if( s1.toUpperCase().indexOf("WARNING") >= 0 ) nwarnings++;
				if( s1.toUpperCase().indexOf("ERROR") >= 0 ) nerrors++;
			}
			else {
				oo[0] = "";
				oo[1] = s1;
				oo[2] = s2;
				oo[3] = s3;
			}
			// Excel
			exout.addRow( "Map" , oo );
			oo = null;
	}
	
	//----------------------------------------------------------------
	private void writeline( String s1 )
	//----------------------------------------------------------------
	{
		writeline( s1 , null , null );
	}
	
	//----------------------------------------------------------------
	private boolean IsMDHUBDate( infaSourceField col)
	//----------------------------------------------------------------
	{
		if( col == null ) return false;
		if( col.DataType == null ) return false;
		if( col.DataType.toUpperCase().startsWith("DATE")) return true;
		if( col.DataType.toUpperCase().startsWith("TIMESTAMP")) return true;
		return false;
	}
	
	// Scan through the cache for this source and table field
	//----------------------------------------------------------------
	private boolean IsColumnAccompaniedBySignField( String tabname , String colname )
	//----------------------------------------------------------------
	{
		if( sourcecache_list == null ) return false;
	//logit(9,"trying to find [" + tabname + "." + colname + "] in source cache" );
		for(int i=0 ; i< sourcecache_list.size() ; i++)
		{
		 if( sourcecache_list.get(i).Name.trim().compareToIgnoreCase( tabname.trim() ) != 0 ) continue;
		 for(int j=0 ; j<sourcecache_list.get(i).collist.size() ; j++)
		 {
			if( sourcecache_list.get(i).collist.get(j).ColumnName == null ) continue; 
	//logit(9," found" + sourcecache_list.get(i).collist.get(j).ColumnName );
	        if( (colname + "_SIGN").toUpperCase().compareToIgnoreCase(sourcecache_list.get(i).collist.get(j).ColumnName.trim()) == 0 ) return true;
	        if( (colname + "_S").toUpperCase().compareToIgnoreCase(sourcecache_list.get(i).collist.get(j).ColumnName.trim()) == 0 ) return true;
		 }
		}	
		return false;
	}
	
	//----------------------------------------------------------------
	private String getPowerDesignerColNameViaSourceColName( String targetTabName , String sourceColName )
	//----------------------------------------------------------------
	{
		if( sourcecache_list == null ) return null;
		//logit( 9,  "Tying to map column [" + sourceColName  + "] onto Entity [" + targetTabName + "]");
		for(int i=0 ; i< sourcecache_list.size() ; i++)
		{
			if( sourcecache_list.get(i).Name.trim().compareToIgnoreCase( targetTabName.trim() ) != 0 ) continue;
			for(int j=0 ; j<sourcecache_list.get(i).collist.size() ; j++)
			{
				//errit( ">>" + sourcecache_list.get(i).collist.get(j).ColumnName + " " + sourcecache_list.get(i).collist.get(j).SourceColumnName );
				if( sourcecache_list.get(i).collist.get(j).SourceColumnName == null ) continue;
				if( sourcecache_list.get(i).collist.get(j).SourceColumnName.trim().compareToIgnoreCase(sourceColName.trim()) != 0) continue;
				return sourcecache_list.get(i).collist.get(j).ColumnName.trim().toUpperCase();
			}
		}
		return null;
	}
	
	
	//----------------------------------------------------------------
	private boolean IsTargetPrimaryKeyColumn( String col)
	//----------------------------------------------------------------
	{
		infaConstraint pk = stmngr.getTargetConstraint( 0 , generatorConstants.CONSTRAINT.PRIMARY , 0 , "IsTargetPrimaryKey");
		if( pk == null ) return false;
		if( pk.key_list.size() != 1 ) {
			errit("Strange there is not a single column PK on [" + stmngr.getInfaTarget(0).Name + "]");
			return false;
		}
		if( pk.key_list.get(0).compareToIgnoreCase(col) == 0 ) return true;
		return false;
	}
	
	//----------------------------------------------------------------
	private boolean IsTargetForeignKeyColumn( String col)
	//----------------------------------------------------------------
	{
		int aantal = stmngr.getNumberOfTargetForeignKeys( 0 );
		if( aantal < 0 ) return true;
		for(int i=0 ; i<aantal ; i++)
		{
			infaConstraint co = stmngr.getTargetConstraint( 0 , generatorConstants.CONSTRAINT.FOREIGN , i , "create_foreign_key_map");
			if( co == null ) return false;
			// Construct column name to hold foreign key value
		    String sColName = constructTargetForeignKeyColumnName( co.ReferencedTableName , co.Name );
		    if( sColName == null ) continue;
			if( sColName.compareToIgnoreCase(col) == 0 ) return true;
		}
		return false;
	}
	
	//----------------------------------------------------------------
	private boolean create_columnMap(int TgtCacheIdx )
	//----------------------------------------------------------------
	{
	    pwdTableCache tc = targetcache_list.get( TgtCacheIdx );
	    if( tc == null ) return false;
		infaSource src = stmngr.getInfaSource(0);
		if( src == null ) return false;
		infaSource tgt = stmngr.getInfaTarget(0);
		if( tgt == null ) return false;
		
		// vul de map link tuples vanaf de linkerzijde 
		mplist = null;
		mplist = new ArrayList<maplink>();
		for(int i=0;i<src.fieldList.size();i++)
		{
		   maplink x = new maplink(i);
		   String lcol = null;
		   //   
		   switch( requestedMapTipe )
		   {
			   case SRC2SST  : {   // use the source cache to get the name of the PowerDesigner colname i.e.  read SourceColName en fetch ColName
				   lcol = getPowerDesignerColNameViaSourceColName( tc.Name , src.fieldList.get(i).Name );  // use the target name to gt to the source
				   break;
			   }
			   case SST2DVST : {
				   lcol = src.fieldList.get(i).Name.trim().toUpperCase();
				   // Geen kolom mapping op de Primary key en de Foreign Key
				   // Jammer genoeg volstaat het niet te checken op _KEY want MDHUB heeft _KEY kolommen
				   //if( lcol.startsWith("KEY_") || lcol.endsWith("_KEY") ) continue;    // geen kolom mapping op _KEY
				   if( IsTargetPrimaryKeyColumn( lcol ) ) {
					   logit( 5 , "Skipping column map on PK key field [" + src.Name + "." + lcol + "]");
					   continue;
				   }
				   if( IsTargetForeignKeyColumn( lcol ) ) {
					   logit( 5 , "Skipping column map on FK key field [" + src.Name + "." + lcol + "]");
					   continue;
				   }
				   break; 
			   }
			   default : {
				   errit("(create columnMap) A Unsupported MapType [" + requestedMapTipe + "]");
		           break;		   
			       }
		   }
		   if( lcol == null ) {
			   errit("(create columnMap) Could not fetch col name for [" + src.Name + "]  colnumber [" + i  + "]");
			   return false;
		   }
		   //
		   for(int j=0;j<tgt.fieldList.size();j++)
		   {
		     if( tgt.fieldList.get(j).Name.trim().compareTo( lcol ) != 0 ) continue; 	
		     x.hits++;
		     if( x.hits > 1 ) {
		    	 errit("(create_columnMap) Duplicate mappping from [" + lcol + "] : [" + tgt.fieldList.get( x.right_idx ) + " and " + tgt.fieldList.get(j).Name + "]");
		    	 return false;
		     }
		     x.right_idx = j;
		   }
		   mplist.add( x );
		}
	    // kijk nu of iedere rechter kant gemapped is	
		for(int i=0; i<tgt.fieldList.size(); i++)
		{
		  String rcol = tgt.fieldList.get(i).Name.trim().toUpperCase();
		  if( rcol.startsWith("KEY_") || rcol.endsWith("_KEY") ) continue;    // geen kolom mapping op _KEY	
		  int idx = -1;
		  for(int j=0;j<mplist.size();j++)
		  {
			  if( mplist.get(j).right_idx == i ) {
				  idx = j;
				  break;
			  }
		  }
		  // no map
		  if( idx < 0 ) {
			  maplink y = new maplink( -1 );
			  y.right_idx = i;
			  mplist.add( y );
		  }
		}
		
		
		// post processing
		for(int i=0 ; i<mplist.size(); i++ )
		{
			maplink z = mplist.get(i);
			if( (z.right_idx>=0 ) && (z.left_idx>=0) ) continue;  // ok
			if( (z.right_idx<0 ) && (z.left_idx<0) ) continue;  // kan niet
			// no source kolom
			if( (z.left_idx < 0) && (z.right_idx>=0) ) {
				// src2sst :  IBMSNAP{OPERATION and LOGMARKER } can be mapped to OPERATION and LOGMARKER
				if( requestedMapTipe == MAPTYPE.SRC2SST ) 
				{
				  String scol = tgt.fieldList.get(mplist.get(i).right_idx).Name.trim().toUpperCase();
				  if( (scol.compareToIgnoreCase("IBMSNAPOPERATION") == 0) || (scol.compareToIgnoreCase("IBMSNAP_OPERATION") == 0) ) {
					  int ndx = getColumnIdxViaName( src , "OPERATION");
					  if( ndx >= 0 ) mplist.get(i).left_idx = ndx;
				  }
				  if( (scol.compareToIgnoreCase("IBMSNAPLOGMARKER") == 0) || (scol.compareToIgnoreCase("IBMSNAP_LOGMARKER") == 0) ) {
					  int ndx = getColumnIdxViaName( src , "LOGMARKER");
					  if( ndx >= 0 ) mplist.get(i).left_idx = ndx;
				  }
				}
			}
			// NO target
			if( (z.right_idx < 0) && (z.left_idx >=0) ) {
				// OPERATION and LOGMARKER can be ignored
				if( requestedMapTipe == MAPTYPE.SRC2SST ) 
				{
					String scol = src.fieldList.get(mplist.get(i).left_idx).Name.trim().toUpperCase();
					if( (scol.compareToIgnoreCase("OPERATION") == 0) ||
						(scol.compareToIgnoreCase("LOGMARKER") == 0) ) {
						mplist.get(i).right_idx = -99;
						mplist.get(i).left_idx = -99;
					}
				}	
			}
		}
		// Speciaal - 4 dec  - 7 dec EDW_CHECKSUM komt al voor
		// Indien 2DVST verwijder de EDW_DQ_INFO en EDW_CRC mappings
		if( (GoNative==true) && (requestedMapTipe == MAPTYPE.SST2DVST) && (PATCH==true)) {
			for(int i=0;i<mplist.size();i++)
			{
				infaSourceField rightc = null;
				if( mplist.get(i).right_idx  >= 0 )  rightc = tgt.fieldList.get( mplist.get(i).right_idx );
				if( rightc == null ) continue;
				String tabname = rightc.Name;
				if( tabname == null ) continue;
				//if( (tabname.compareToIgnoreCase("EDW_DQ_INFO")!=0) && (tabname.compareToIgnoreCase("EDW_CHECKSUM")!=0) ) continue;
				if( tabname.compareToIgnoreCase("EDW_DQ_INFO")!=0 ) continue;
				mplist.get(i).left_idx = -99;
				mplist.get(i).right_idx = -99;
			}
		}
		
        // remove
		int aantal = mplist.size();
		for(int i=0;i<aantal;i++)
		{
			for(int j=0;j<mplist.size();j++)
			{
				if( (mplist.get(j).left_idx == -99) && (mplist.get(j).right_idx == -99) ) {
					mplist.remove(j);
					break;
				}
			}
		}
		
		String part3=null;
		// create lines
		for(int i=0;i<mplist.size();i++)
		{
			//
			infaSourceField leftc = null;
			if( mplist.get(i).left_idx >= 0 ) leftc = src.fieldList.get( mplist.get(i).left_idx );
			infaSourceField rightc = null;
			if( mplist.get(i).right_idx  >= 0 )  rightc = tgt.fieldList.get( mplist.get(i).right_idx );
	        //
			// skip EDW_CHECKSUM when performing a column to column map
			if( CRCCreateLayer == readPwdXml.LAYER.SST ) {
							if( requestedMapTipe == MAPTYPE.SRC2SST ) {
								if( rightc != null ) {
									if ( rightc.Name.compareToIgnoreCase("EDW_CHECKSUM") == 0 ) continue;
								}
							}
			}
			if( CRCCreateLayer == readPwdXml.LAYER.DVST ) {
							if( requestedMapTipe == MAPTYPE.SST2DVST ) {
								if( rightc != null ) {
									if ( rightc.Name.compareToIgnoreCase("EDW_CHECKSUM") == 0 ) continue;
								}
							}
			}
			// Skip the SST.EDW_DQ_NFO column to column map if SRC@SST - wordt apart opgevuld in route TSTQUAL
			if( (GoNative==true) && (requestedMapTipe == MAPTYPE.SRC2SST) ) {
				if( rightc != null ) {
					if ( rightc.Name.compareToIgnoreCase("EDW_DQ_INFO") == 0 ) continue;
				}
			}
			//
			switch( requestedMapTipe )
			{
			   case SRC2SST  : { 
				                 String part4="";
				                 boolean isMandatory = false;
				                 if( rightc != null ) {
			                    	 if ( rightc.mandatory ) isMandatory=true;
			                     }
				                 part3 = (isMandatory == true ) ? "" : "EDWNULL();";
				                 // native - put always a 'NULL' transformation
				                 if( GoNative ) part3 = "EDWNULL();";
				                 //
				                 // UCHP - see whether this is a Column that has an adjoining column_SIGN
				                 if( sSourceSystem.compareToIgnoreCase("UCHP") == 0 ) {
				                	 if( leftc != null ) {
				                		 part4 = IsColumnAccompaniedBySignField( tc.Name , leftc.Name )  ? "EDWSIGN(); " : "";
				                	 }
				                 }
				                 // MDHUB  dates
				                 if( this.sSourceSystem.compareToIgnoreCase("MDHUB")==0) {
				                		 part4 = IsMDHUBDate( leftc ) ? "EDWTIMESTAMPTOSTR('YYYYMMDDHH24MISS'); "  : "";
				                		 if( part4.length() != 0 ) part3=""; // EDWTIMESTAMP also takes care of NULL
				                 }		
			                     //
				                 part3 = (part4 + part3).trim();
			                     if( part3.length() != 0) part3 = "{ " + part3 + " }";
			                     break; }
			   case SST2DVST : { 
				    part3 = do_SST2DVST_cast(leftc,rightc);   
				    if( part3 == null ) {
				    	if( this.sSourceSystem.compareToIgnoreCase("VCE_SURVEY")==0 ) {   // CSS-DSS 
				    		part3 = "{ CSSDSSNULL(); }" ; 
				    	}
				    	else part3 = "{ RLTRIM(); }" ; 
				    }
				    break; }
			   default : {
				   errit("(create columnMap) B Unsupported MapType [" + requestedMapTipe + "]");
		           break;		   
			       }
			}
	
			
			
			// check datatype en lengte
			if( (leftc != null) && (rightc != null) ) {
				String ltipe = leftc.DataType.trim().toUpperCase();
				String rtipe = rightc.DataType.trim().toUpperCase();
				if( ltipe.compareToIgnoreCase("nstring") == 0 ) ltipe = "CHAR";
				if( ltipe.toUpperCase().indexOf("CHAR") >=0 ) ltipe = "CHAR";
				if( rtipe.toUpperCase().indexOf("CHAR") >=0 ) rtipe = "CHAR";
				if( ltipe.compareToIgnoreCase( rtipe ) != 0) {
					boolean isFout=true;
					// Native SST is per defintie CHAR
					if( (GoNative==true) && (requestedMapTipe ==  MAPTYPE.SRC2SST) && (rtipe.compareToIgnoreCase("CHAR")==0) ) isFout=false;
					// Native en SSTDVST geen fout indien de part 3 een DQ functie bevat
					if( part3 != null ) {
					 if( (GoNative==true) && (requestedMapTipe ==  MAPTYPE.SST2DVST) && (part3.toUpperCase().indexOf("DQ")>=0) ) isFout=false;
					}
					if( isFout )
					    writeline("-- Error: [" + src.Name + "] Datatype mismatch : " + leftc.Name + " : " + leftc.DataType +  " -> " + rightc.DataType , null , null);	
				}
				//
				int lprec = leftc.Precision;
				int rprec = rightc.Precision;
				if( lprec > rprec ) {
					writeline("-- Warning: ["  + src.Name + "] Precision loss : " + leftc.Name + " : " + leftc.DataType + "=" + lprec +  " -> " + rightc.DataType  + "=" + rprec , null , null);	
				}
			}
			
			// <null> <null>    => fout
			// <null> <right>   =>   N/A <right>  doch ook  NIL <right> CONST(x)  
			// <left> <null>    =>   --<left>
			// <left> <right>   =>   left right  RLTRIM() voor DVST    EDWNULL(voor SST)
			if( (leftc == null) && (rightc == null) ) {
				errit("(create_columnMap) System fout 1");
				return false;
			}
			if( (leftc != null) && (rightc != null) ) {
				// overrule
				if( source_overrule( src.Name , leftc.Name ) ) continue;
				if( target_overrule( tgt.Name , rightc.Name ) ) continue;
				//
				writeline( leftc.Name , rightc.Name , part3 );
				continue;
			}
			// NO Source Column
			if( (leftc == null) && (rightc != null) ) {
				// overrule
				if( target_overrule( tgt.Name , rightc.Name ) ) continue;
				String sTest = rightc.Name.trim().toUpperCase();
				// target field is _KEY ?  then skip
				/* KB 28 NOV - wellicht niet meer nodig aangezien de  _key velden uit mlist gewijderd zoden moeten zijn
				 * te testen
				if( sTest.startsWith("KEY_")) continue;
				if( sTest.endsWith("_KEY")) continue;
				*/
				//
				// CUSTOM CODE AREA - organized per source system
				if( sSourceSystem.compareToIgnoreCase("UCHP") == 0 ) if( do_UCHP_no_source(rightc.Name) ) continue; 
				if( sSourceSystem.compareToIgnoreCase("AMM") == 0 ) if( do_AMM_no_source(rightc.Name) ) continue; 
				if( sSourceSystem.compareToIgnoreCase("MDHUB") == 0 ) if( do_AMM_no_source(rightc.Name) ) continue;  // same as AMM
				if( sSourceSystem.compareToIgnoreCase("GLOPPS") == 0 ) if( do_AMM_no_source(rightc.Name) ) continue;  // KB25NOV same as AMM
				//
				if( (sTest.compareToIgnoreCase("BATCH_ID")==0) || (sTest.compareToIgnoreCase("BATCHID")==0) ) {
					writeline( "nil" , sTest , "{ CONST($$BATCH_ID); }");
					continue;
				}
				//
				writeline( "-- Error: [" + src.Name + "] Target Column [" + tgt.Name + "." + rightc.Name + "] has no matching Source");
				//writeline( "N/A" , rightc.Name , "" );
				writeline( "nil" , rightc.Name , "{ CONST('NULL'); }" );
				continue;
			}
			// NO Target Column
			if( (leftc != null) && (rightc == null) ) {
				if( source_overrule( src.Name , leftc.Name ) ) continue;
				//
				if( sSourceSystem.compareToIgnoreCase("AMM") == 0 ) { if( do_AMM_no_target( src.Name , leftc.Name) ) continue; }
				// ignore indien patch en EDW_DQ_INFO
				if( (PATCH==true) && (leftc.Name.compareToIgnoreCase("EDW_DQ_INFO")==0)) continue;
				//
				writeline( "-- Error: [" + src.Name + "] Source Column [" + src.Name + "." + leftc.Name + "] has no matching Target");
				writeline( ""  , "--" + leftc.Name , "ERROR");
				continue;
			}
		}
		writeline("--");
		
		
		// ======
		// 14 DEC - Ugly fix op de CLAIMJOB_KEY kolom die foutief gedefinieerd staat
		if( (sSourceSystem.compareToIgnoreCase("UCHP") == 0) && (CLAIMJOBPATCH==true) )  {
			if( (tgt.Name.compareToIgnoreCase("DVST_UCHP_CLAIMCOMMENT")==0) || (tgt.Name.compareToIgnoreCase("DVST_UCHP_INSPECTORREPORT")==0)) {
				writeline("-- Exception [" + tgt.Name + ".CLAIMJOB_KEY  quicka nd dirty fix");
				writeline("nil" , "CLAIMJOB_KEY" , "{CONST('NULL');}");
			}
		}
		// ====
		
		
		return true;
	}
	
	
	//----------------------------------------------------------------
	private boolean source_overrule( String tabname , String colname)
	//----------------------------------------------------------------
	{
		return do_overrule( true , tabname , colname );
	}
	//----------------------------------------------------------------
	private boolean target_overrule( String tabname , String colname)
	//----------------------------------------------------------------
	{
		return do_overrule( false , tabname , colname );
	}
	//----------------------------------------------------------------
	private boolean do_overrule( boolean isSource , String tabname , String colname )
	//----------------------------------------------------------------
	{
		if( overrule_list == null )  return false;
		for(int i=0;i<overrule_list.size();i++)
		{
			pwdOverrule x = overrule_list.get(i);
			if( x.isSource != isSource ) continue;
			if( x.TableName.compareToIgnoreCase(tabname) != 0 ) continue;
			if( x.ColumnName.compareToIgnoreCase(colname) != 0 ) continue;
			//
			writeline("-- Exception: [" + (tabname + "." + colname).toUpperCase() + "] mapping overruled" , null , null);	
			//
			String sLinks  = x.leftSide == null ? "" : x.leftSide.toUpperCase();
			String sRechts = x.rightSide == null ? "" : x.rightSide.toUpperCase();
			String sCons   = x.Construct == null ? "" : x.Construct;
			writeline( sLinks , sRechts , sCons );
			return true;
		}
		return false;
	}
	
	//==============================   CUSTOM AREA ================
	
	// CUSTOM UCHP - return true if line has been written
	//----------------------------------------------------------------
	private boolean do_UCHP_no_source(String scol )
	//----------------------------------------------------------------
	{
		String sColName = scol.trim().toUpperCase();
		if( isEdwTypeOfConcernIssue( sColName )) {
			writeline( "nil" , sColName , "{ CONST('" + CSTR_TYPEOFCONCERN + "'); }");
			return true;
		}
		if( isCOMPANYIssue(sColName)) {
			writeline( "nil" , sColName , "{ CONST('" + CSTR_COMPANY + "'); }");
			return true;
		}
		if( isSOURCEKEYIssue(sColName)) {
			writeline( "nil" , sColName , "{ CONST('" + CSTR_SOURCEKEY + "'); }");
			return true;
		}
		//
		if( (scol.compareToIgnoreCase("IBMSNAP_COMMITSEQ")==0) || (scol.compareToIgnoreCase("IBMSNAPCOMMITSEQ")==0) ) {
			writeline( "nil" , sColName , "{ CONST('NULL'); }");
			return true;
		}
		if( (scol.compareToIgnoreCase("IBMSNAP_INTENTSEQ")==0) || (scol.compareToIgnoreCase("IBMSNAPINTENTSEQ")==0) ) {
			writeline( "nil" , sColName , "{ CONST('NULL'); }");
			return true;
		}
		if( (scol.compareToIgnoreCase("RECORD_SOURCE")==0) || (scol.compareToIgnoreCase("RECORDSOURCE")==0) ) {
			writeline( "nil" , sColName , "{ CONST('" + sSourceSystem.trim().toUpperCase() + "'); }");
			return true;
		}
		if( (scol.compareToIgnoreCase("RECORD_STATUS")==0) || (scol.compareToIgnoreCase("RECORDSTATUS")==0) ) {
			// does OPERATION field feature on source ?
			int ndx = -1;
			infaSource src = stmngr.getInfaSource(0);
			if( src != null ) 
			ndx = this.getColumnIdxViaName( src , "OPERATION" );
			if( ndx < 0 )  return false;
			//
			writeline( "OPERATION" , sColName , "");
			return true;
		}
		return false;
	}
	
	//----------------------------------------------------------------
	private boolean do_UCHP_foreign_key(String sColumnName , String sElem , int cntr , int idx)
	//----------------------------------------------------------------
	{
		// EDW_TYPEOFCONCERN ?
        if( isEdwTypeOfConcernIssue( sElem ) ) {
    		writeline( "nil" , sColumnName , "{ CONST('" + CSTR_TYPEOFCONCERN + "'); FKEY(" + cntr + "," + idx + "); }" );
    		return true;
    	}
        // COMPANYNO
        if( isCOMPANYIssue( sElem ) ) {
    		writeline( "nil" , sColumnName , "{ CONST('" + CSTR_COMPANY + "'); FKEY(" + cntr + "," + idx + "); }" );
    		return true;
    	}
		return false;
	}
	
	//----------------------------------------------------------------
	private boolean do_UCHP_checksum(String sCol , int idx)
	//----------------------------------------------------------------
	{
		if( this.isEdwTypeOfConcernIssue( sCol ) ) {
			writeline( "-- TypeOfConcern provisioning" , null , null );
			writeline( "nil" , "EDW_CHECKSUM" , "{ CONST('" + CSTR_TYPEOFCONCERN + "'); CRC(" + idx + "); }");
			return true;
		}
		if( this.isCOMPANYIssue( sCol ) ) {
			writeline( "-- COMPANYNO provisioning" , null , null );
			writeline( "nil" , "EDW_CHECKSUM" , "{ CONST('" + CSTR_COMPANY + "'); CRC(" + idx + "); }");
			return true;
		}
		return false;
	}
	
	//----------------------------------------------------------------
	private boolean isEdwTypeOfConcernIssue(String scol )
	//----------------------------------------------------------------
	{
		   // TYPEOFCONCERN ?
		   if( (scol.trim().compareToIgnoreCase( "EDW_TYPEOFCONCERN" ) != 0) &&
			   (scol.trim().compareToIgnoreCase( "TYPEOFCONCERN" ) != 0) ) return false;
		   
		   // has TYPEOFCONCERN been mapped ?
		   boolean hasbeenmapped=false;
		   infaSource tgt = stmngr.getInfaTarget(0);
		   if( tgt == null ) return false;  // should not occur
		   for(int i=0;i<mplist.size();i++)
		   {
			 if(  mplist.get(i).right_idx < 0 ) continue;
			 String sTargetCol = tgt.fieldList.get( mplist.get(i).right_idx ).Name;
			 if( sTargetCol.compareToIgnoreCase( scol ) != 0 ) continue;
			 if( mplist.get(i).left_idx >= 0 ) hasbeenmapped = true;
			 break;
		   }
		   return !hasbeenmapped;  // opgepast INVERT
	}
	
	//----------------------------------------------------------------
	private boolean isCOMPANYIssue(String scol )
	//----------------------------------------------------------------
	{
			   // COMPANY ?
		       if( scol.toUpperCase().indexOf("COMPANYNO") < 0 ) return false;
			   // has this column been mapped ?
			   boolean hasbeenmapped=false;
			   infaSource tgt = stmngr.getInfaTarget(0);
			   if( tgt == null ) return false;  // should not occur
			   for(int i=0;i<mplist.size();i++)
			   {
			     if(  mplist.get(i).right_idx < 0 ) continue;
				 String sTargetCol = tgt.fieldList.get( mplist.get(i).right_idx ).Name;
				 if( sTargetCol.compareToIgnoreCase( scol ) != 0 ) continue;
	//errit( scol + " " + mplist.get(i).left_idx );
				 if( mplist.get(i).left_idx >= 0 ) hasbeenmapped = true;
				 break;
			   }
			   return !hasbeenmapped;  // opgepast INVERT
	}
	
	//----------------------------------------------------------------
	private boolean isSOURCEKEYIssue(String scol )
	//----------------------------------------------------------------
	{
				   // EDW_SOURCEKEY ?
			       if( (scol.toUpperCase().indexOf("SOURCEKEY") < 0) && (scol.toUpperCase().indexOf("EDW_SOURCEKEY") <0) ) return false;
				   // has this column been mapped ?
				   boolean hasbeenmapped=false;
				   infaSource tgt = stmngr.getInfaTarget(0);
				   if( tgt == null ) return false;  // should not occur
				   for(int i=0;i<mplist.size();i++)
				   {
				     if(  mplist.get(i).right_idx < 0 ) continue;
					 String sTargetCol = tgt.fieldList.get( mplist.get(i).right_idx ).Name;
					 if( sTargetCol.compareToIgnoreCase( scol ) != 0 ) continue;
		//errit( scol + " " + mplist.get(i).left_idx );
					 if( mplist.get(i).left_idx >= 0 ) hasbeenmapped = true;
					 break;
				   }
				   return !hasbeenmapped;  // opgepast INVERT
	}
	
	
	// CUSTOM AMM - return true if line has been written
	//----------------------------------------------------------------
	private boolean do_AMM_no_source(String scol )
	//----------------------------------------------------------------
	{
			String sColName = scol.trim().toUpperCase();
			if( (scol.compareToIgnoreCase("RECORD_SOURCE")==0) || (scol.compareToIgnoreCase("RECORDSOURCE")==0) ) {
				writeline( "nil" , sColName , "{ CONST('" + sSourceSystem.trim().toUpperCase() + "'); }");
				return true;
			}
			if( (scol.compareToIgnoreCase("RECORD_STATUS")==0) || (scol.compareToIgnoreCase("RECORDSTATUS")==0) ) {
				writeline( "nil" , sColName , "{ CONST('I'); }");
				return true;
			}
			return false;
	}
	
	// CUSTOM AMM - return true if line has been written
	//----------------------------------------------------------------
	private boolean do_AMM_no_target(String sSrc , String scol )
	//----------------------------------------------------------------
	{
		        // FILLER{n}
				String sColName = scol.trim().toUpperCase();
				if( (scol.compareToIgnoreCase("FILLER")!=0) || (scol.startsWith("FILLER")==true) ) {
					writeline("-- Warning: [" + sSrc + "." + scol + "] probably a filler column. Please check." , null , null);	
					writeline( "N/A" , sColName , "");
					return true;
				}
				return false;
	}
	
	//==============================   CUSTOM AREA ================
	
	
	//----------------------------------------------------------------
	private boolean report_constraints()
	//----------------------------------------------------------------
	{
		        writeline("--",null,null);
		        // fetch PRIMARY constraint and fectch the columname
				infaConstraint pk = stmngr.getTargetConstraint( 0 , generatorConstants.CONSTRAINT.PRIMARY , 0 , "report_constraints1");
				if( pk != null ) {
					//
					if( pk.key_list.size() != 1 )  {
					  errit( "There must be exactly 1 Technical Primary key on [" + stmngr.getInfaTarget(0).Name + "]" );
					  return false;
					}
					// is there a primary key field 
				    if( getColumnIdxViaName( stmngr.getInfaTarget(0) , getStrippedTableName(stmngr.getInfaTarget(0).Name) + "_KEY" ) < 0 ) {
				    	if( getColumnIdxViaName( stmngr.getInfaTarget(0) , "KEY_" + getStrippedTableName(stmngr.getInfaTarget(0).Name)  ) < 0 ) {
				    		logit(1,"Strange no Technical / Primary _KEY field on table [" + stmngr.getInfaTarget(0).Name + "]");
				    		return true;
				    	}
				    }
				    String sLijn = pk.Name + " " + pk.Tipe + " " + pk.key_list.toString();
				    writeline( "-- Constraint : " + sLijn.trim() );
				}
			
				// fetch UNIQUE constraint and the loop through its elements
				infaConstraint nk = stmngr.getTargetConstraint( 0 , generatorConstants.CONSTRAINT.UNIQUE , 0 , "report_constraints2" );
			    if( nk != null ) {	
			    	if( nk.key_list.size() == 0 )  {
					  errit( "There must be at least 1 element on the alternative unique key on [" + stmngr.getInfaTarget(0).Name + "]" );
					  return false;
			    	}
 		    	    String sLijn = nk.Name + " " + nk.Tipe + " " + nk.key_list.toString();
				    writeline( "-- Constraint : " + sLijn.trim()  );
				    //errit("OK - got a UNIQUE key ignore previous");
			    }
			    //	
				int aantal = stmngr.getNumberOfTargetForeignKeys( 0 );
				if( aantal < 0 ) return true;
				for(int i=0 ; i<aantal ; i++)
				{
					infaConstraint co = stmngr.getTargetConstraint( 0 , generatorConstants.CONSTRAINT.FOREIGN , i , "report_constraints3" );
					String sLijn = co.Name + " " + co.Tipe + " " + co.key_list.toString() + " " + co.ReferencedOwner + "." + co.ReferencedTableName + " " + co.ref_list.toString();
				    writeline( "-- Constraint : " + sLijn.trim()  );
				}
				 writeline("--");
				return true;
	}
	
	//----------------------------------------------------------------
	private boolean create_primary_key_map()
	//----------------------------------------------------------------
	{
		// fetch PRIMARY constraint and fetch the columname
		// fetch UNIQUE constraint and the loop tghrough its elements
		infaConstraint pk = stmngr.getTargetConstraint( 0 , generatorConstants.CONSTRAINT.PRIMARY , 0 , "create_primary_key_map1");
		if( pk == null ) return false;
		infaConstraint nk = stmngr.getTargetConstraint( 0 , generatorConstants.CONSTRAINT.UNIQUE , 0 , "create_primary_key_map2");
		if( nk == null ) return false;
		// is there a primary key field 
	    if( getColumnIdxViaName( stmngr.getInfaTarget(0) , getStrippedTableName(stmngr.getInfaTarget(0).Name) + "_KEY" ) < 0 ) {
	    	if( getColumnIdxViaName( stmngr.getInfaTarget(0) , "KEY_" + getStrippedTableName(stmngr.getInfaTarget(0).Name)  ) < 0 ) {
	    		logit(1,"Strange no Technical / Primary _KEY field on table [" + stmngr.getInfaTarget(0).Name + "]");
	    		return true;
	    	}
	    }
		//
		if( pk.key_list.size() != 1 )  {
		  errit( "There must be exactly 1 Technical Primary key on [" + stmngr.getInfaTarget(0).Name + "]" );
		  return false;
		}
		String sTargetField =  pk.key_list.get(0).trim().toUpperCase();
		if( nk.key_list.size() == 0 )  {
			  errit( "There must be at least 1 element on the alternative unique key on [" + stmngr.getInfaTarget(0).Name + "]" );
			  return false;
		}
		writeline("--",null,null);
		for(int i=0 ; i< nk.key_list.size(); i++ )
		{
			writeline( nk.key_list.get(i).trim().toUpperCase() , sTargetField , "{ KEY(" + i + "); }" ); 
		}
		return true;
	}
	
	// remove DVST_UCHP   <LAYER_><SOURCESYS_>
	//----------------------------------------------------------------
	private String getStrippedTableName(String tab)
	//----------------------------------------------------------------
	{
		String sRet = tab.trim().toUpperCase();
		String sPrefix = (getTargetLayer() + "_" + this.sSourceSystem + "_").toUpperCase();
		if( sRet.startsWith(sPrefix ) ) {  // good
	        sRet = sRet.substring( sPrefix.length() );
		}
		return sRet;
	}
	
	//----------------------------------------------------------------
	private int getColumnIdxViaName( infaSource x , String scol)
	//----------------------------------------------------------------
	{
		for(int i=0;i<x.fieldList.size();i++)
		{
	      if( x.fieldList.get(i).Name.trim().compareToIgnoreCase( scol.trim() ) == 0 ) return i;
		}
		return -1;
	}
	
	private String constructTargetForeignKeyColumnName(String ReferencedTableName , String FKName)
	{
		String sRefTable = getStrippedTableName( ReferencedTableName );  // DVST_UCHP_<table>  , so you need to remove the layer and system
		String sColName =  sRefTable + "_KEY";
	    if( getColumnIdxViaName( stmngr.getInfaTarget(0) , sColName ) < 0 ) {
	    	sColName = "KEY_" + sRefTable;
		    if(  getColumnIdxViaName( stmngr.getInfaTarget(0) , sColName ) < 0 ) {
		    	errit("There is no field to implement foreign key [" + FKName + "] on table [" + stmngr.getInfaTarget(0).Name + "]" );
		    	return null;
		    }
	    }
	    return sColName;
	}
	
	//----------------------------------------------------------------
	private boolean create_foreign_key_map()
	//----------------------------------------------------------------
	{
		int aantal = stmngr.getNumberOfTargetForeignKeys( 0 );
		if( aantal < 0 ) return true;
		int cntr=0;
		for(int i=0 ; i<aantal ; i++)
		{
			infaConstraint co = stmngr.getTargetConstraint( 0 , generatorConstants.CONSTRAINT.FOREIGN , i , "create_foreign_key_map");
			if( co == null ) return false;
			// construct the column name to hold the foreign key value
			/*
			String sRefTable = getStrippedTableName( co.ReferencedTableName );  // DVST_UCHP_<table>  , so you need to remove the layer and system
			String sColName =  sRefTable + "_KEY";
		    if( getColumnIdxViaName( stmngr.getInfaTarget(0) , sColName ) < 0 ) {
		    	sColName = "KEY_" + sRefTable;
			    if(  getColumnIdxViaName( stmngr.getInfaTarget(0) , sColName ) < 0 ) {
			    	errit("There is no field to implement foreign key [" + co.Name + "] on table [" + stmngr.getInfaTarget(0).Name + "]" );
			    	return false;
			    }
		    }
		    */
		    // Construct column name to hold foreign key value
		    String sColName = constructTargetForeignKeyColumnName( co.ReferencedTableName , co.Name );
		    if( sColName == null ) return false;
		    //
		    if( co.key_list.size() < 1 ) {
		    	errit("There are no elements on the foreign key [" + co.Name + "] on table [" + stmngr.getInfaTarget(0).Name + "]" );
		    	return false;
		    }
		    // indien dit de FK naar de single value technical key is dan skippen  (we hebben de values van de natural key velden nodig )
		    if( co.key_list.get(0).compareToIgnoreCase( sColName ) == 0 ) continue;
		    //
		    cntr++;
		    writeline("--");
		    writeline("-- " + co.Name + " -> " + co.ReferencedTableName );
		    for(int j=0;j<co.key_list.size();j++)
		    {
		    	String scol = co.key_list.get(j);
		        //	
		    	// CUSTOM AREA
		    	if( this.sSourceSystem.compareToIgnoreCase("UCHP") == 0 ) if( do_UCHP_foreign_key( sColName , scol , cntr , j) ) continue;
		    	//
		    	//
		    	writeline( scol , sColName , "{ FKEY(" + cntr + "," + j + "); }" );
		    }
		    //
		}
		return true;
	}
	
	//----------------------------------------------------------------
	private pwdColumnCache getColumnTargetCacheIdx( String tab , String col )
	//----------------------------------------------------------------
	{
		for(int i=0;i<targetcache_list.size();i++)
		{
//errit( "looking for [" + tab + "] -> " + targetcache_list.get(i).Name + "<-" + targetcache_list.get(i).Name.trim().compareToIgnoreCase( tab ) );
			if( targetcache_list.get(i).Name.trim().compareToIgnoreCase( tab ) != 0 ) continue;
			pwdTableCache t = targetcache_list.get(i);
			for(int j=0 ; j<t.collist.size(); j++)
			{
			  if( t.collist.get(j).ColumnName.trim().compareToIgnoreCase( col ) != 0 ) continue;
			  return t.collist.get(j);
			}
		}
		errit("Cannot find [" + tab + "].[" + col + "] in Target cache");
		return null;
	}
	
	//----------------------------------------------------------------
	private pwdColumnCache getColumnSourceCacheIdx( String tab , String col )
	//----------------------------------------------------------------
	{
		for(int i=0;i<sourcecache_list.size();i++)
		{
//errit( "looking for [" + tab + "] -> " + sourcecache_list.get(i).Name + "<-" + sourcecache_list.get(i).Name.trim().compareToIgnoreCase( tab ) );
			if( sourcecache_list.get(i).Name.trim().compareToIgnoreCase( tab ) != 0 ) continue;
			pwdTableCache t = sourcecache_list.get(i);
			for(int j=0 ; j<t.collist.size(); j++)
			{
			  if( t.collist.get(j).ColumnName.trim().compareToIgnoreCase( col ) != 0 ) continue;
			  return t.collist.get(j);
			}
		}
		errit("Cannot find [" + tab + "].[" + col + "] in Source cache");
		return null;
	}
	
	//----------------------------------------------------------------
	private pwdColumnCache getSourceColumnSourceCacheIdx( String tab , String col )
	//----------------------------------------------------------------
    {
			for(int i=0;i<sourcecache_list.size();i++)
			{
	//errit( "looking for [" + tab + "] -> " + sourcecache_list.get(i).Name + "<-" + sourcecache_list.get(i).Name.trim().compareToIgnoreCase( tab ) );
				if( sourcecache_list.get(i).Name.trim().compareToIgnoreCase( tab ) != 0 ) continue;
				pwdTableCache t = sourcecache_list.get(i);
				for(int j=0 ; j<t.collist.size(); j++)
				{
				  if( t.collist.get(j).SourceColumnName.trim().compareToIgnoreCase( col ) != 0 ) continue;
				  return t.collist.get(j);
				}
			}
			errit("Cannot find [" + tab + "].[" + col + "] in Source cache");
			return null;
	}
	
	//----------------------------------------------------------------
	private pwdColumnCache getColumnDVSTCacheIdx( String tab , String col )
	//----------------------------------------------------------------
	{
			for(int i=0;i<dvstcache_list.size();i++)
			{
				if( dvstcache_list.get(i).Name.trim().compareToIgnoreCase( tab ) != 0 ) continue;
				pwdTableCache t = dvstcache_list.get(i);
				for(int j=0 ; j<t.collist.size(); j++)
				{
				  if( t.collist.get(j).ColumnName.trim().compareToIgnoreCase( col ) != 0 ) continue;
				  return t.collist.get(j);
				}
			}
			errit("Cannot find [" + tab + "].[" + col + "] in DVST cache");
			return null;
	}
	
	//----------------------------------------------------------------
	private boolean hasLeftHandSideMappingValue(pwdColumnCache targetcol , infaSource tgt)
	//----------------------------------------------------------------
	{
		if( CRCCreateLayer != readPwdXml.LAYER.SST) return true; 
		if( requestedMapTipe != MAPTYPE.SRC2SST ) return true;
		// zoek de col in de TargetCache - gebruik CODE 
		String scol = targetcol.ColumnName;
		for(int i=0;i<mplist.size();i++)
		{
		  int idx = mplist.get(i).right_idx;
		  if( idx < 0 )  continue; // no target onmap
		  if( idx >= tgt.fieldList.size() ) {
			  errit("Systeem fout 45");
			  return false;
		  }
		  String xcol =  tgt.fieldList.get( idx ).Name;
		  if( scol.compareToIgnoreCase( xcol ) != 0 ) continue;
		  // FOUND
		  //errit( "found " + scol + " " + mplist.get(i).left_idx );
		  if( mplist.get(i).left_idx < 0) return false;
		  return true;
		}
		errit("Systeem fout 46 - could not find scol inmaplist");
		return true;
	}
	
	
	//----------------------------------------------------------------
	private boolean create_CRC_map()
	//----------------------------------------------------------------
	{
	    // check whether the CRC needs to be created	
		boolean doit=false;
		if( (CRCCreateLayer == readPwdXml.LAYER.SST) && (requestedMapTipe == MAPTYPE.SRC2SST) ) doit=true;
		if( (CRCCreateLayer == readPwdXml.LAYER.DVST) && (requestedMapTipe == MAPTYPE.SST2DVST) ) doit=true;
		if( doit == false ) return true;	
		//
		ArrayList<ColumnLocation> list = new ArrayList<ColumnLocation>();   // will contain the CRC columns + sorted
		//
		infaSource tgt = stmngr.getInfaTarget( 0 );
		// only if there is a EDW_CHECKSUM
		pwdColumnCache ccrc = getColumnTargetCacheIdx( getStrippedTableName( tgt.Name ) , "EDW_CHECKSUM" );
		if( ccrc == null ) {
		  logit(1,"There is no EDW_CHECKSUM defined on [" + tgt.Name + "] Stripped name [" + getStrippedTableName( tgt.Name ) + "]");
		  return true;
		}
		//
		for(int i=0;i<tgt.fieldList.size();i++)
		{
			pwdColumnCache cc = getColumnTargetCacheIdx( getStrippedTableName( tgt.Name ) , tgt.fieldList.get(i).Name );
			if( cc == null ) continue;
			if( cc.edwcrc == false ) continue;    // CAUTION : needs to be set
			//
			if( (cc.ColumnCode.startsWith("KEY_")) || (cc.ColumnCode.endsWith("_KEY")) ) {
				// KB 10 NOV - BIj MDHUB komen er _KEY velden voor op de source views, tsja ..
			    if( sSourceSystem.compareToIgnoreCase("MDHUB") != 0 ) {
				  logit(1,"There is a _KEY on a CRC required column - skipping [" + tgt.Name + "].[" + cc.ColumnCode + "]");
				  continue;
			    }
			}
			//  Skip sommige kolommen
			String skipper=null;
			if( cc.ColumnCode.startsWith("IBMSNAP") ) skipper="IBMSNAP";
			if( cc.ColumnCode.compareToIgnoreCase("EDW_CHECKSUM")==0) skipper="EDW_CHECKSUM";
			if( cc.ColumnCode.compareToIgnoreCase("BATCH_ID")==0) skipper="EDW_CHECKSUM";
			if( cc.ColumnCode.compareToIgnoreCase("RECORD_STATUS")==0) skipper="RECORD_STATUS";
			if( cc.ColumnCode.compareToIgnoreCase("RECORD_SOURCE")==0) skipper="RECORD_SOURCE";
			if( cc.ColumnCode.compareToIgnoreCase("EDW_DQ_INFO")==0) skipper="EDW_DQ_INFO";
			//
			if( skipper != null  ) {
				logit(1,"CRC requested on column [" + skipper + "] - but this will be skipped [" + tgt.Name + "].[" + cc.ColumnCode + "]");
				continue;
			}
			// KB 02 dec - kijk na of de de kolom waarop de CRC geplaats wordt wel voorkomt als LeftHand kolom in de mamlist
			// Indien niet betekent dit dat er geen CRC input waarde is
			if( hasLeftHandSideMappingValue( cc , tgt ) == false ) {
				skipper = cc.ColumnName;
				logit(1,"CRC requested on column [" + skipper + "] - but no Left Hand Side/Source Column for target col [" + tgt.Name + "].[" + cc.ColumnCode + "]");
				continue;
			}
			//
			ColumnLocation x = new ColumnLocation();
			x.ColName = cc.ColumnCode.trim().toUpperCase();
			// KB - 9 May MDHUB sometimes has different colnames - xxx
			if( requestedMapTipe == MAPTYPE.SRC2SST ) {
			  x.ColName = cc.SourceColumnName.trim().toUpperCase();
			}
			x.location = tgt.fieldList.get(i).fieldNumber ;
	        list.add( x );
		}
		// Sorteren
		int aantal = list.size();
		if( aantal == 0 ) {
			logit(1,"Strange there are no EDWCHECKSUM columns defined for [" + tgt.Name + "] yet there is an EDW_CHECKSUM column");
			return true;
		}
		for(int k=0;k<aantal;k++)
		{
			boolean swap = false;
			for(int i=0;i<(aantal-1);i++)
			{
				if( list.get(i).location > list.get(i+1).location ) {
					int oo = list.get(i).location;
					String soo = list.get(i).ColName;
					//
					list.get(i).ColName = list.get(i+1).ColName;
					list.get(i).location = list.get(i+1).location;
					list.get(i+1).ColName = soo;
					list.get(i+1).location = oo;
					swap = true;
				}
			}
			if( swap == false ) break;
		}
		// send to map file
		for(int i=0;i<list.size();i++)
		{
			String sCol = list.get(i).ColName;
			
			// CUSTOM CODE AREA
			if( sSourceSystem.compareToIgnoreCase("UCHP") == 0 ) if ( do_UCHP_checksum( sCol , i) ) continue;  
			//
			
			writeline( sCol , "EDW_CHECKSUM" , "{ CRC(" + i + "); }");	
			
		}
		list = null;
		return true;
	}

	
	
	// DQ
	//----------------------------------------------------------------
	private boolean create_DQ_columnMap(int TgtCacheIdx)
	//----------------------------------------------------------------
	{
		 pwdTableCache tc = targetcache_list.get( TgtCacheIdx );
		 if( tc == null ) return false;
		 infaSource src = stmngr.getInfaSource(0);
		 if( src == null ) return false;
		 infaSource tgt = stmngr.getInfaTarget(0);
		 if( tgt == null ) return false;
			
		 // vul de map link tuples vanaf de linkerzijde 
		 mplist = null;
		 mplist = new ArrayList<maplink>();
		 for(int i=0;i<src.fieldList.size();i++)
		 {
			   maplink x = new maplink(i);
			   x.right_idx = -1;
			   String lcol =  src.fieldList.get(i).Name.toUpperCase();
			   // just check if there is a matching rcol
			   for(int j=0;j<tgt.fieldList.size();j++)
			   {
			     if( tgt.fieldList.get(j).Name.trim().compareTo( lcol ) != 0 ) continue; 	
			     x.hits++;
			     if( x.hits > 1 ) {
			    	 errit("(create_columnMap) Duplicate mappping from [" + lcol + "] : [" + tgt.fieldList.get( x.right_idx ) + " and " + tgt.fieldList.get(j).Name + "]");
			    	 return false;
			     }
			     x.right_idx = j;
			   }
			   mplist.add( x );
		 }
		 // check whether all left sides are mapped
		 for(int i=0;i<mplist.size();i++)
		 {
			 if( mplist.get(i).right_idx == -1 ) {
				 errit("(create_columnMa) field [" + src.fieldList.get(i).Name + "] has no target field");
				 return false;
			 }
		 }
		 // Check whether all right sides are mapped  (so loop doorheen target en zoek voor right
		 boolean  gotEDWDQINFO = false;
		 for(int i=0;i<tgt.fieldList.size();i++)
		 {
			 boolean found = false;
			 for(int j=0;j<mplist.size();j++)
			 {
				 if( mplist.get(j).right_idx == i) { found = true; break; }
			 }
			 if( tgt.fieldList.get(i).Name.compareToIgnoreCase("EDW_DQ_INFO") == 0 ) {
				 found = true;
				 gotEDWDQINFO=true;
			 }
			 if( found == false ) {
				 errit("Target column [" + tgt.Name + "." + tgt.fieldList.get(i).Name + "] is not mapped");
				 return false;
			 }
		 }
		 // zet de data kwaliteits functies
		 ArrayList<DQTuple> qualList = new ArrayList<DQTuple>();
		 for(int i=0;i<mplist.size();i++)
		 {
				//
				infaSourceField leftc = null;
				if( mplist.get(i).left_idx >= 0 ) leftc = src.fieldList.get( mplist.get(i).left_idx );
				infaSourceField rightc = null;
				if( mplist.get(i).right_idx  >= 0 )  rightc = tgt.fieldList.get( mplist.get(i).right_idx );
				
				if( (leftc == null) || (rightc == null) ) {
					errit("OOPS - veel te snel");
					continue;
				}
				// if target datatype is char - do nothing
				// if source is not char then do nothing 
				if( (rightc.DataType.toUpperCase().indexOf("CHAR") >= 0) ||	(leftc.DataType.toUpperCase().indexOf("CHAR") < 0)) {
					writeline( leftc.Name , rightc.Name , "");
					continue;
				}
				
				String sInstruc=null;
				if( rightc.DataType.compareToIgnoreCase("BIGINT") == 0 ) sInstruc = "DQBIGINT()";
				if( rightc.DataType.compareToIgnoreCase("FLOAT") == 0 ) sInstruc = "DQFLOAT()";
				if( rightc.DataType.compareToIgnoreCase("DOUBLE") == 0 ) sInstruc = "DQFLOAT()";
				if( rightc.DataType.compareToIgnoreCase("INTEGER") == 0 ) sInstruc = "DQINTEGER()";
				if( rightc.DataType.compareToIgnoreCase("REAL") == 0 ) sInstruc = "DQFLOAT()";
				if( rightc.DataType.compareToIgnoreCase("SMALLINT") == 0 ) sInstruc = "DQINTEGER()";
				if( rightc.DataType.compareToIgnoreCase("BYTEINT") == 0 ) sInstruc = "DQINTEGER()";
				//
				if( rightc.DataType.compareToIgnoreCase("NUMERIC") == 0 ) sInstruc = "DQDECIMAL(" + rightc.Precision + "," + rightc.scale + ")";
				if( rightc.DataType.compareToIgnoreCase("DECIMAL") == 0 ) sInstruc = "DQDECIMAL(" + rightc.Precision + "," + rightc.scale + ")";
				//
				if( rightc.DataType.compareToIgnoreCase("DATE") == 0 ) sInstruc = "DQTIMESTAMP(\"YYYY-MM-DD\")";  // testen
				//if( rightc.DataType.compareToIgnoreCase("TIME") == 0 ) sInstruc = "DQTIME()";
				if( rightc.DataType.compareToIgnoreCase("TIMESTAMP") == 0 ) sInstruc = "DQTIMESTAMP(\"YYYY-MM-DD-HH24.MI.SS.US\")";
				//
				if( sInstruc == null ) {
				  writeline( "-- Error Unsupported DataType[" + rightc.DataType + "] [Prec=" + rightc.Precision + "] [Scale=" + rightc.scale + "]" , "N/A" , leftc.Name  );
				}
				else {
				  writeline( leftc.Name , rightc.Name , "{ " + sInstruc + "; }" );	
				  // bufferen
				  DQTuple dt = new DQTuple();
				  dt.colname = leftc.Name;
				  dt.DQFunction = sInstruc;
				  if( dt.DQFunction.toUpperCase().startsWith("DQ") ) {  //  DQINTEGER => TSTQUAL( n , INTEGER ))
					  dt.DQFunction = dt.DQFunction.substring(2);
				  }
				  qualList.add(dt);
				}
		 } 
	     // TSTQUAL instructies on EDW_DQ_INFO
		 if( gotEDWDQINFO ) {
			
			 for(int i=0;i<qualList.size();i++)
			 {
				 DQTuple tup = qualList.get(i);
				 writeline( tup.colname , "EDW_DQ_INFO" , "{ TSTQUAL(" + i + ","  + tup.DQFunction + "); }" );	
			 }
		 }
		 return true;
	}
	
	//----------------------------------------------------------------
	private int getSourceCacheIdxViaTargetCacheIdx(int TargetCacheIdx)
	//----------------------------------------------------------------
	{
		if( (TargetCacheIdx<0) || (TargetCacheIdx >= targetcache_list.size()) ) return -1;
		String tabname = targetcache_list.get(TargetCacheIdx).Name;
		if( tabname == null ) return -1;
		for(int i=0;i<sourcecache_list.size();i++)
		{
			if( sourcecache_list.get(i).Name.compareToIgnoreCase(tabname) == 0 ) return i;
		}
		errit("Cannot find table [" + tabname + "] in source cache list");
		return -1;
	}
	
	//----------------------------------------------------------------
	private boolean IsAString(String stipe)
	//----------------------------------------------------------------
	{
		if( stipe == null ) return true;
		if( stipe.toUpperCase().indexOf("CHAR")>=0) return true;
		if( stipe.toUpperCase().indexOf("STRING")>=0) return true;
		return false;
	}
	
	//----------------------------------------------------------------
	private boolean create_TESTQUAL_map(int TargetCacheIdx)
	//----------------------------------------------------------------
	{
		//
		if( !GoNative ) return true;
		//
		// TESTQUAL(bigint,date,etc) indien de INPUT en STRING is en het native datatype niet
		// daarna TESTMANDATROY op die kolommen NATIVE mandatory
		infaSource tgt = stmngr.getInfaTarget( 0 );
		// only if there is a EDW_DQ_INFO on the taret table
		pwdColumnCache cdqinfo = getColumnTargetCacheIdx( getStrippedTableName( tgt.Name ) , "EDW_DQ_INFO" );
		if( cdqinfo == null ) {
		  logit(1,"There is no EDW_DQ_INFO defined on [" + tgt.Name + "] Stripped name [" + getStrippedTableName( tgt.Name ) + "]");
		  return true;
		}
		
		// zoek de Index voor deze tabel op de SourceIndex
		// de LKP_<x>_src bevat namelijk de NATIVE datatypes
		int SourceCacheIdx = getSourceCacheIdxViaTargetCacheIdx( TargetCacheIdx );
		if( SourceCacheIdx < 0 ) {
			errit("Cannot find the SourceCacheIDx for target table [" + tgt.Name + "]");
			return false;
		}
		//logit( 9 , "SourceCacheIdx=" + SourceCacheIdx + " TargetCacheIdx=" + TargetCacheIdx);
	
		// loop doorheen de column mappings
		infaSource src = stmngr.getInfaSource( 0 );
		String tabname = sourcecache_list.get(SourceCacheIdx).Name;
		int teller=-1;
		ArrayList<String> list = new ArrayList<String>();
	    for(int i=0;i<mplist.size();i++)
	    {
	    	if( (mplist.get(i).left_idx < 0) || (mplist.get(i).right_idx <0) ) continue;
			infaSourceField leftc = null;
			if( mplist.get(i).left_idx >= 0 ) leftc = src.fieldList.get( mplist.get(i).left_idx );
			infaSourceField rightc = null;
			if( mplist.get(i).right_idx  >= 0 )  rightc = tgt.fieldList.get( mplist.get(i).right_idx );
			// op de SOURCE cache col vind je het Native datatype en mandatory
			pwdColumnCache col = getColumnSourceCacheIdx( tabname , leftc.Name );
			// KB - 9 may - 
			if ( col == null ) {
				col = getSourceColumnSourceCacheIdx( tabname , leftc.Name );
				if( col != null ) {
					errit("Success : switching to sourcecolum name");
				}
			}
			if( col == null) {
				errit("(create_TESTQUAL) Cannot find column in source cache [" + tabname + "." + leftc.Name + "]");
				return false;
			}
			
//logit(5,"???" + col.ColumnName + " " + col.DataType + " " + col.SourceColumnName + " " + col.SourceDataType);
			
			// op de DVSt cache vind je de datavault versie van native datatype
			pwdColumnCache dvst = getColumnDVSTCacheIdx( tabname , rightc.Name );
			if( dvst == null) {
				errit("(create_TESTQUAL) Cannot find column in DVST cache [" + tabname + "." + rightc.Name + "]");
				return false;
			}
			String NativeDataType = col.SourceDataType.trim().toUpperCase();
			String SRCDataType = leftc.DataType.trim().toUpperCase();
			String SSTDataType = rightc.DataType.trim().toUpperCase();
			String DVSTDataType = dvst.DataType.trim().toUpperCase();
			boolean NativeMandatory = col.isSourceMandatory;
			//
			if( NativeMandatory ) {
				String sco = leftc.Name.trim();
				list.add(sco);
			}
			//
			//logit( 9 , "(create_TESTQUAL) [" + tabname + "." + leftc.Name + "] [SRCType=" + SRCDataType + "] [SSTType=" + SSTDataType + "] [NativeType=" + NativeDataType + "] [NativeManda=" + NativeMandatory + "] [DVST=" + DVSTDataType + "]");
		    //
			// IF Datavault datatype native is string skip
			if( IsAString( NativeDataType ) ) {
				// voor UCHP speciaal - alle dates staan als VARCHAR2(26) in de BEAT excel - kijk dan gewoon wat er op DVST steekt
				if(sSourceSystem.compareToIgnoreCase("UCHP")==0) {
				   String sx = xMSet.xU.removeBelowIncludingSpaces(NativeDataType);
				   sx = sx.toUpperCase().trim();
				   if( sx.compareToIgnoreCase("VARCHAR2(26)") != 0 ) continue;
				   if( (DVSTDataType.toUpperCase().startsWith("TIME")) || (DVSTDataType.toUpperCase().startsWith("DATE")) ) {
					   NativeDataType = DVSTDataType;   // overrrule de varchar
					   logit(1,"(create_TESTQUAL) TIMESTAMP defined as VARCHAR2 in Beat [" + tabname + "." + leftc.Name + "] [DVST=" + DVSTDataType + "]");
				   }
				   else continue;
				}
				else continue;
			}
			// IF datype op de aanleverende bron (niet de source, want UCHP wordt als flatfile geleverd) geen String dan OK ) betrouw op CAST in Infa
			// indien aanleverende bron is een string en davault type is geen string dan checken
			if( !IsAString( SRCDataType ) ) continue;
			//logit( 5 , "(create_TESTQUAL) [" + tabname + "." + leftc.Name + "] [DVST=" + DVSTDataType + "]");
			// DVST type mag geen CHAR zijn als de native geen char is
			if( IsAString( DVSTDataType ) ) {
				errit("(create TESTQUAL) Native=" + NativeDataType + " -> DVST=" + DVSTDataType );
				errit("DVST datatype should not be CHAR if Native is not a CHAR for [" + tabname + "." + rightc.Name + "] Skipping");
				continue;
			}
			
			//
			String sCheck=null;
			if( DVSTDataType.startsWith("BIGINT") ) sCheck = "BIGINT";
			if( DVSTDataType.startsWith("INTEGER") ) sCheck = "INTEGER";
			if( DVSTDataType.startsWith("NUMERIC") || DVSTDataType.startsWith("DECIMAL") ) {  
				try {
				 String sRest = (DVSTDataType.substring( "NUMERIC".length())).trim(); // gelukkige DEC en NUM zelfde lengete
				 // check
				 int iPrec  = xMSet.xU.extractPrecision(sRest);
				 int iScale = xMSet.xU.extractScale(sRest);
				 if( (iPrec <= 0) || (iScale <=0) ) {
					 errit("(create_TESTQUAL) Cannot extract precision/scale from [" + sRest + "] on column [" + tabname + "." + leftc.Name + "] [Prec=" + iPrec + "] [Scale=" + iScale + "]" );
					 return false;
				 }
				 sCheck = "DECIMAL" + sRest;
				}
				catch(Exception e ) {
					errit("(create_TESTQUAL) Cannot extract precision/scale from [" + tabname + "." + leftc.Name + "]");
					return false;
				}
			}
			if( (DVSTDataType.startsWith("TIMESTAMP")) || (DVSTDataType.startsWith("DATE")) ) {  // Sterk afhankelijk van de bron
				if( sSourceSystem.compareToIgnoreCase("UCHP")==0) {
				  sCheck = "TIMESTAMP('YYYY-MM-DD-HH24.MI.SS.US')";
				}
				else
				if( sSourceSystem.compareToIgnoreCase("MDHUB")==0) {
					  sCheck = "* VERWIJDER NA DEBUG SESSIE *";
				}
				else 
				if( getTimeStampFormat() != null) {
					 sCheck = "TIMESTAMP('" + getTimeStampFormat().trim() + "')";
				}	
				else
				{   
					errit("(create TESTQAL) - DVST/native TIMESTAMP - unsupported sourcesystem [" + sSourceSystem + "]");
					errit("(create TESTQAL) - Or the timestamp format has not been set in overrule file");
					return false;
				}
			}
			if( sCheck == null ) {
				errit("(create_TESTQUAL) [" + tabname + "." + leftc.Name + "] [SRCType=" + SRCDataType + "] [SSTType=" + SSTDataType + "] [NativeType=" + NativeDataType + "] [NativeManda=" + NativeMandatory + "] [DVST=" + DVSTDataType + "] currently unsupported - contact dev team");
				errit("(create TESTQUAL) Native=" + NativeDataType + " -> DVST=" + DVSTDataType );
				return false;
			}
			//
			teller++;
			if( teller == 0 ) writeline("","","");
			writeline(  leftc.Name , "EDW_DQ_INFO" , "{ TSTQUAL(" + teller + "," + sCheck + "); }" );
	    }
		
	    for(int i=0;i<list.size();i++)
	    {
	    	teller++;
	    	if( i == 0 ) writeline("","","");
	    	writeline(  list.get(i) , "EDW_DQ_INFO" , "{ TSTQUAL(" + teller + ",MANDATORY); }" );
	    }
	    
		return true;
	}
	
	
	
	// doe de SST nar DVSt type cast
	//----------------------------------------------------------------
	private String do_SST2DVST_cast(infaSourceField lcol , infaSourceField rcol)
	//----------------------------------------------------------------
	{
		if( GoNative == false )  return null;
		if( (lcol==null) || (rcol==null) ) return null; // enkel echte kolom naar kolom mappingen
		// Kolommen waar geen bewerking nodig is
		if( lcol.Name.compareToIgnoreCase("BATCH_ID")==0) return "";       
		if( lcol.Name.compareToIgnoreCase("RECORD_STATUS")==0) return "";  
		if( lcol.Name.compareToIgnoreCase("RECORD_SOURCE")==0) return "";  
		if( lcol.Name.compareToIgnoreCase("EDW_CHECKSUM")==0) return "";  
		if( lcol.Name.compareToIgnoreCase("EDW_DQ_INFO")==0) return "";
		//
		if( IsAString(rcol.DataType) ) return null;   // target is een STRING -> null wat een RLTRIM wordt
		//
		if( !IsAString(lcol.DataType) ) {  // links moet een STRING zijn
			errit("SST2DVST mapping from [" + rcol.Name + "] SST datatype is not a string but [Datatype=" + lcol.DataType + "]");
			return null;
		}
		//
		String TrgtTipe = rcol.DataType;
		String trans=null;
		if( TrgtTipe.compareToIgnoreCase("BIGINT")==0) trans = "DQBIGINT()";
		if( TrgtTipe.compareToIgnoreCase("INTEGER")==0) trans = "DQINTEGER()";
		if( (TrgtTipe.compareToIgnoreCase("NUMERIC")==0) || (TrgtTipe.compareToIgnoreCase("DECIMAL")==0)) {
			  if( sSourceSystem.compareToIgnoreCase("UCHP") == 0 ) {
		        	trans = "DQdivDECIMAL(" + rcol.Precision + "," + rcol.scale + ")";
		      }
			  else {
			       trans = "DQDECIMAL(" + rcol.Precision + "," + rcol.scale + ")";
			  }
		}
		// source systeem afhankelijk ..
		if( TrgtTipe.compareToIgnoreCase("TIMESTAMP")==0 ) {
	        if( sSourceSystem.compareToIgnoreCase("UCHP") == 0 ) {
	        	trans = "DQTIMESTAMP('YYYY-MM-DD-HH24.MI.SS.US')";
	        }
	        else
	        if( sSourceSystem.compareToIgnoreCase("MDHUB") == 0 ) {
	        	trans = "DQTIMESTAMP('YYYYMMDDHH24MISS')";
	        }
	        else
	        if( getTimeStampFormat() != null ) {
	        	trans = "DQTIMESTAMP('" + getTimeStampFormat().trim() + "')";
	        }
	        else {
	        	errit("(do_SST2DVST_cast) Sourcesystem is [" + sSourceSystem + "] - TIMESTAMP FORMAT UNKNOWN");
	        	errit("(do_SST2DVST_cast) or timestamp format has not been set in the overrule file");
	        	return  null;
	        }
		}
		if( TrgtTipe.compareToIgnoreCase("DATE")==0 ) {
	        if( sSourceSystem.compareToIgnoreCase("UCHP") == 0 ) {
	        	//trans = "DQTIMESTAMP('YYYY-MM-DD-HH24.MI.SS.US')";
	        	errit("UCHP DATE not supported");
	        }
	        else
	        if( sSourceSystem.compareToIgnoreCase("MDHUB") == 0 ) {
	        	trans = "DQTIMESTAMP('YYYYMMDD')";
	        }
	        else {
	        	errit("(do_SST2DVST_cast) Sourcesystem is [" + sSourceSystem + "] - DATE FORMAT UNKNOWN");
	        	return  null;
	        }
		}
		if( trans == null ) {
			errit("(do_SST2DVST_cast) [Col=" + rcol.Name + "] unsupported datatype [" + TrgtTipe + "]");
			return null;
		}
		//
		return "{ " + trans + "; }";
	}
	
}
