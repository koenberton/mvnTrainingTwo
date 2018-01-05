package pcGenerator.netezzaView;

import generalpurpose.gpPrintStream;
import generalpurpose.pcDevBoosterSettings;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import office.xlsxWriterMark2;





public class nzViewParser {
	
	pcDevBoosterSettings xMSet = null;
	
	private gpPrintStream fout = null;
	private xlsxWriterMark2 xcw = null;
    boolean ExcelHasBeenCreated = false;
    private String SheetName=null;
    
	 
    private String SrcDDLShort = null;
	private String TgtDDLShort = null;
	private String SrcDDLFull  = null;
	private String TgtDDLFull  = null;
	private String ParamFull   = null;
	private String ParamShort  = null;
	private String srcEnviron  = null;
	private String tgtEnviron  = null;
	private String prodEnviron  = null;
	private String promotionDatabaseName = null;
	private boolean makeSingleFile = true;
	private boolean makeRedirectScript = false;
	private boolean makeDeployScript = false;
	private boolean makeCompareReport = false;
	private boolean makeTestScript = false;
	private boolean includeMode = false;
	private int nbrViewsWritten=0;
	private int viewsInCurrentFile=0;
	private int fileCounter=0;
	private int globErrorCounter=0;
	
	
	private boolean setNoError = false;
	private boolean setEcho = false;
	
	
    //private char[] delimBuffer = delimList.toCharArray();
    
    private int globaldiepte=0;
    
	private String SQLOperators   = ",()+-/*=!<>";
	private char[] SQLOperatorsBuffer = SQLOperators.toCharArray();
    
    private enum VIEWTOKENTYPE  { SQLCOMMAND , SQLFUNCTION , OPERATOR , ATTRIBUTE , TABLE , VIEW , UNKNOWN }
   
    private enum SQLCOMMAND { SELECT , FROM , INTO , WHERE , GROUP , BY , ORDER , 
    	                      DESC , ASC ,  ASCENDING, DESCENDING , NOT , NULL , NOTNULL , AS ,
    	                      CREATE , OR , REPLACE , VIEW , THEN , WHEN , END , ELSE ,
    	                      LEFT , RIGHT , JOIN , OUTER , INNER , CROSS , ON , UNION ,
    	                      CASE , EXCEPT , MINUS , 
    	                      SUM , COUNT , MIN , MAX , AVERAGE
    	                      }
    
    private enum SQLFUNCTION {  TO_DATE , TO_CHAR , COALESCE , CAST , SUBSTR }

    private enum PATTERN { ERROR , TABLE_PAT_01 , TABLE_PAT_02 , TABLE_PAT_03 , VIEW_PAT_01 , ATTR_PAT_01 , ATTR_PAT_02 , ATTR_PAT_03 , UNKNOWN}
    /*
     * TABLE_PAT_01 : null.databasename.EDW.tablename
     * TABLE_PAT_02 : null.null.EDW.tablename
     * TABLE_PAT_03 : null.null.DEFINITION_SCHEMA.tablename
     * VIEW_PAT_01  : null.null./usename.viewname
     * ATTR_PAT_01  : null.null.ALIAS/TABLENAME.attributename
     * ATTR_PAT_02  : databasename.EDW.tabelaneme.attributename
     * ATTR_PAT_03  : null.DEFINTION_SCHEMA.table.attributename
     */
    
    private enum SCRIPTTYPE { DEPLOY , REDIRECT , TESTSQL }
    
    class ViewToken
    {
    	String value;
    	int level;
    	VIEWTOKENTYPE tipe;
    	SQLCOMMAND sqlcommand;
    	SQLFUNCTION sqlfunction;
    	String Leg1=null;
    	String Leg2=null;
    	String Leg3=null;
    	String Leg4=null;
    	PATTERN pat= PATTERN.UNKNOWN;
    	ViewToken(String sValue , int il , VIEWTOKENTYPE ti)
    	{
    		value = sValue;
    		level = il;
    		tipe = ti;
    		sqlcommand = null;
    		sqlfunction = null;
    		Leg1=null;
     		Leg2=null;
     		Leg3=null;
     		Leg4=null;
     		pat= PATTERN.UNKNOWN;
    	}
    }
   
    
    class MyView
    {
        String FullName;	
        String ShortName;
        ArrayList<ViewToken> tlist;
        int dependency_depth=0;
        String environmentReferenced;
        ArrayList<String> dblist = null;
        MyView(String sIn)
        {
        	FullName = sIn.trim().toUpperCase();
        	ShortName = null;
        	tlist = new ArrayList<ViewToken>();
        	dependency_depth=0;
        	environmentReferenced=null;
        	dblist = new ArrayList<String>();
        }
    }
    
    
    private ArrayList<MyView> srcViewList = null;
    private ArrayList<MyView> tgtViewList = null;
    private ArrayList<String> excludeViewList = null;
    private ArrayList<String> includeViewList = null;
    
    class MyPrefix
    {
    	String value;
    	int tipe;
    }
    private ArrayList<MyPrefix> srcPrefixList = null;
    private ArrayList<MyPrefix> tgtPrefixList = null;
    
    
    class DBNameTranslator
    {
    	String srcDBName=null;
    	String tgtDBName=null;
    	DBNameTranslator(String sLeft , String sRight)
    	{
    		srcDBName = sLeft;
    		tgtDBName = sRight;
    	}
    }
    private ArrayList<DBNameTranslator> dbList = null;
    
	//----------------------------------------------------------------
	public nzViewParser(pcDevBoosterSettings im)
	//----------------------------------------------------------------
	{
			xMSet = im;
	}
		
	//----------------------------------------------------------------
	private void logit(int level , String sLog)
	//----------------------------------------------------------------
	{
			xMSet.logit(level, "[" +  this.getClass().getName() + "] " + sLog );
	}
						
	//----------------------------------------------------------------
	private void errit(String sLog)
	//----------------------------------------------------------------
	{
				logit(0,sLog);	
	}
	
	//----------------------------------------------------------------
	private void usage()
	//----------------------------------------------------------------
	{
		errit("nzviewtool SrcDDL=<DDL file> TgtDDL=<DDL file>  ParameterFile=<Name of parameter file>");
		errit("Parameters");
		errit("  OPTION = {MAKE_REDIRECT_SRIPTS,MAKE_DEPLOY_SCRIPTS,MAKE_MULTIPLE_FILES,MAKE_SINGLE_FILE,MAKE_COMPARE_REPORT,SET_ECHO,SET_NO_ERROR,MAKE_TEST_SCRIPT}");
		errit("  PROMOTION_DATABASE = name of the database used in NN+<name>+main.sql");
		errit("  SOURCE_ENVIRONMENT = name of the environment the source DDL was extracted from");
		errit("  TARGET_ENVIRONMENT = name of the target environment");
		errit("  REDIRECTED_ENVIRONMENT = name of the environment were the PROD data will be extracted from");
		errit("  EXCLUDE = name of the view to exclude");
		errit("  <databasename> -> <databasename>  : translation of the databases used in the Source DDL script");
	}
	
	//----------------------------------------------------------------
	public boolean doit(String[] args)
	//----------------------------------------------------------------
	{
		long startt=System.currentTimeMillis();
        //		
		if( checkFiles(args) == false ) return false;
		if( readParameterFile() == false ) {
			usage();
			return false;
		}
		if( normaliseer(SrcDDLFull, "SRC") == false ) return false;
		if( normaliseer(TgtDDLFull, "TGT") == false ) return false;
		extractAllDatabaseAndUserNames();
		if( assessQuality() == false ) return false;
		if( doAllViewCompare() == false ) return false;
		if( rewriteDDLForDeployment() == false ) return false;
		if( writeTestSQLFile() == false ) return false;
		//
		logit(1,"Completed succesfully - Elapsed time [" + ((System.currentTimeMillis() - startt)/1000L) + "] sec Processed [" + srcViewList.size() + "] views");
		return true;
	}
	

	//----------------------------------------------------------------
	private String getCoreFileName(String sIn)
	//----------------------------------------------------------------
	{
	    int i = sIn.lastIndexOf(".");
	    if( i < 0 ) return sIn;
	    try {
	     String sRet = sIn.substring(0,i);
	     return sRet;
	    }
	    catch(Exception e ) {
	    	return null;
	    }
	}
	
	//----------------------------------------------------------------
	private boolean checkFiles(String[] args)
	//----------------------------------------------------------------
	{
		 boolean isOK = true;
	     String sDir = xMSet.getProjectDir() + xMSet.xU.ctSlash + "DDL" + xMSet.xU.ctSlash;
	
	     for(int i=0;i<args.length;i++)  {
	    	 
	    	 String sCmd = args[i].trim();
	    	 if( sCmd.toUpperCase().startsWith("SRCDDL=") ) {
	    		 SrcDDLShort = sCmd.substring("SRCDDL=".length());
	    		 SrcDDLFull = sDir + SrcDDLShort;
	    	 }
	    	 if( sCmd.toUpperCase().startsWith("TGTDDL=") ) {
	    		 TgtDDLShort = sCmd.substring("TGTDDL=".length());
	    		 TgtDDLFull = sDir + TgtDDLShort;
	    	 }
	    	 if( sCmd.toUpperCase().startsWith("PARAMETERFILE=") ) {
	    		 ParamShort = sCmd.substring("PARAMETERFILE=".length());
	    		 ParamFull = sDir + ParamShort;
	    	 }
	     }
	     //
	     if( SrcDDLFull == null ) {
	    	 errit("Source DDL file not defined ");
	    	 isOK=false;
	     }
	     else {
	        if (xMSet.xU.IsBestand(SrcDDLFull) == false ) {
	        	errit("Cannot find [" + SrcDDLFull + "] source DDL");
	        	isOK=false;
	        }
	     }
	     if( TgtDDLFull == null ) {
	    	 errit("Target DDL file not defined ");
	    	 isOK=false;
	     }
	     else {
	    	 if (xMSet.xU.IsBestand(TgtDDLFull) == false ) {
		        	errit("Cannot find [" + TgtDDLFull + "] target DDL");
		        	isOK=false;
		        }
	     }
	     if( ParamFull == null ) {
	    	 errit("Parameter file not defined ");
	    	 isOK=false;
	     }
	     else {
	    	 if (xMSet.xU.IsBestand(ParamFull) == false ) {
		        	errit("Cannot find [" + ParamFull + "] parameter file");
		        	isOK=false;
		        } 
	     }
	     //
	     return isOK;
	}

	//----------------------------------------------------------------
	private boolean readParameterFile()
	//----------------------------------------------------------------
	{
	 excludeViewList = new ArrayList<String>();
	 includeViewList = new ArrayList<String>();
     // 
	 String FName = ParamFull;	
	 String ENCODING = xMSet.xU.getEncoding(FName);
	 boolean isOK = true;
	 try {
		  BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(FName),ENCODING));
		  String sLijn = null;
		  while ((sLijn=reader.readLine()) != null) {
			 String sComp = sLijn.trim();
			 if( sComp.length() < 1) continue;
			 if( sComp.startsWith("--") ) continue;
			 //
			 sComp = xMSet.xU.Remplaceer(sComp," ","");
			 if( sComp.toUpperCase().startsWith("EXCLUDE=")) {
				 String sExclude =  sComp.substring("EXCLUDE=".length()).trim().toUpperCase();
				 if( sExclude.length() < 2 ) {
					 errit("Probably an error on the following EXCLUDE definiton in parameter file [" + sLijn + "]");
					 isOK=false;
				 }
				 excludeViewList.add(sExclude);
				 continue;
			 }
			 if( sComp.toUpperCase().startsWith("INCLUDE=")) {
				 String sInclude =  sComp.substring("INCLUDE=".length()).trim().toUpperCase();
				 if( sInclude.length() < 2 ) {
					 errit("Probably an error on the following INCLUDE definiton in parameter file [" + sLijn + "]");
					 isOK=false;
				 }
				 if( includeMode == false ) logit(1," --> Now switching to INCLUDE MODE");
				 includeMode=true;
				 includeViewList.add(sInclude);
				 continue;
			 }
			 else if (sComp.indexOf("->") > 0 ) {
				 String sLeft = null;
				 String sRight = null;
				 try {
				  sLeft = sComp.substring(0,sComp.indexOf("->")).trim().toUpperCase();
				  sRight = sComp.substring(sComp.indexOf("->")+2).trim().toUpperCase();
				 }
				 catch( Exception e ) {
					 errit("Cannot extract database names from [" + sLijn + "]");
					 isOK=false;
				 }
				 // Should start by DEV QA, TST or PROD
				 if( !(sLeft.startsWith("DEV_") || sLeft.startsWith("TST_") || sLeft.startsWith("QA_") || sLeft.startsWith("PROD_")) ) {
					 errit("Left hand side database does not start by DEV/TST/QA/PROD [" + sLijn + "]");
					 isOK=false;
				 }
				 if( !(sRight.startsWith("DEV_") || sRight.startsWith("TST_") || sRight.startsWith("QA_") || sRight.startsWith("PROD_")) ) {
					 errit("Right hand side database does not start by DEV/TST/QA/PROD [" + sLijn + "]");
					 isOK=false;
				 }
				
				 //
				 DBNameTranslator y = new DBNameTranslator(sLeft,sRight);
				 if( dbList == null ) dbList = new ArrayList<DBNameTranslator>();
				 dbList.add(y);
				 logit(5,"TRANSLATOR [" + y.srcDBName + "] --> [" + y.tgtDBName + "]");
			 }
			 else if (sComp.toUpperCase().startsWith("SOURCE_ENVIRONMENT=") ) {
				 srcEnviron =  sComp.substring("SOURCE_ENVIRONMENT=".length()).trim().toUpperCase();
				 logit(1,"SOURCE ENVIRONMENT [" + srcEnviron + "]");
				 if( "DEVTGTQAPROD".indexOf(srcEnviron) < 0 ) {
					errit("Unknown Source Environment [" + srcEnviron + "]");
					isOK=false;
				 }
			 }
			 else if (sComp.toUpperCase().startsWith("TARGET_ENVIRONMENT=") ) {
				 tgtEnviron =  sComp.substring("TARGET_ENVIRONMENT=".length()).trim().toUpperCase();
				 logit(1,"TARGET ENVIRONMENT [" + tgtEnviron + "]");
				 if( "DEVTGTQAPROD".indexOf(tgtEnviron) < 0 ) {
						errit("Unknown Target Environment [" + tgtEnviron + "]");
						isOK=false;
					 }
			 }
			 else if (sComp.toUpperCase().startsWith("REDIRECTED_ENVIRONMENT=") ) {
				 prodEnviron =  sComp.substring("REDIRECTED_ENVIRONMENT=".length()).trim().toUpperCase();
				 logit(1,"PREDIRECTED ENVIRONMENT [" + prodEnviron + "]");
				 if( "DEVTGTQAPROD".indexOf(tgtEnviron) < 0 ) {
						errit("Unknown REDIRECTED Environment [" + prodEnviron + "]");
						isOK=false;
					 }
			 }
			 else if (sComp.toUpperCase().startsWith("PROMOTION_DATABASE=") ) {
				 promotionDatabaseName =  sComp.substring("PROMOTION_DATABASE=".length()).trim().toUpperCase();
				 logit(1,"PROMOTION DATABASE [" + promotionDatabaseName + "]");
			 }
			 else if (sComp.toUpperCase().startsWith("OPTION=") ) {
				 String sOption =  sComp.substring("OPTION=".length()).trim().toUpperCase();
				 // {MAKE_REDIRECT_SRIPTS,MAKE_DEPLOY_SCRIPTS,MAKE_MULTIPLE_FILES,MAKE_SINGLE_FILE,MAKE_COMPARE_REPORT"
				
				 if( sOption.compareToIgnoreCase("MAKE_REDIRECT_SCRIPTS")==0) makeRedirectScript=true;
				 else
				 if( sOption.compareToIgnoreCase("MAKE_DEPLOY_SCRIPTS")==0) makeDeployScript=true;
				 else
				 if( sOption.compareToIgnoreCase("MAKE_MULTIPLE_FILE")==0) makeSingleFile=false;
				 else
				 if( sOption.compareToIgnoreCase("MAKE_SINGLE_FILE")==0) makeSingleFile=true;
				 else
				 if( sOption.compareToIgnoreCase("MAKE_MULTIPLE_FILES")==0) makeSingleFile=false;
				 else
				 if( sOption.compareToIgnoreCase("MAKE_SINGLE_FILES")==0) makeSingleFile=true;
				 else
				 if( sOption.compareToIgnoreCase("MAKE_COMPARE_REPORT")==0) makeCompareReport=true;
				 else
				 if( sOption.compareToIgnoreCase("MAKE_TEST_SCRIPT")==0) makeTestScript=true;
				 else
				 if( sOption.compareToIgnoreCase("SET_ECHO")==0) setEcho=true;
				 else
				 if( sOption.compareToIgnoreCase("SET_NO_ERROR")==0) setNoError=true;
				 else {			 
				  errit("Unknown OPTION in parameter file [" + sLijn + "]");	 
				  isOK=false;
				  continue;
				 }
				 //logit(1,"OPTION [" + sOption + "]");
			 }
			 else {
				 errit("Unknown command in parameter file [" + FName + "] : " + sLijn);
				 isOK = false;
			 }
		  }
		  reader.close();
          //
		  // checks
		  if( srcEnviron == null ) {
			  errit("SOURCE_ENVIRONEMENT = has not been set in parameter file");
			  isOK=false;
		  }
		  if( tgtEnviron == null ) {
			  errit("TARGET_ENVIRONEMENT = has not been set in parameter file");
			  isOK=false;
		  }
		  if( prodEnviron == null ) {
			  errit("REDIRECTED_ENVIRONEMENT = has not been set in parameter file");
			  isOK=false;
		  }
		  if( promotionDatabaseName == null ) {
			  errit("PROMOTION_DATABASE = has not been set in parameter file");
			  isOK=false;
		  }
		  //
		  for(int j=0;j<dbList.size();j++)
		  {
			 DBNameTranslator x = dbList.get(j);
			 String sLeft = x.srcDBName;
			 String sRight = x.tgtDBName;
			 if( sLeft == null ) {
				 errit("DB Translator error 1");
				 isOK=false;
				 continue;
			 }
			 if( sRight == null ) {
				 errit("DB Translator error 2");
				 isOK=false;
				 continue;
			 }
			 // Left must be SOURCE environment
			 if( (srcEnviron == null) || (tgtEnviron==null)  ) continue;
			 if( sLeft.startsWith(srcEnviron) == false ) {
				 errit("Left hand side database does not start with [" + srcEnviron + "] in parameter [" + sLeft + " -> " + sRight + "]");
				 isOK=false;
			 }
			 // Right hand side must be Target env or PRODUCTION
			 if( (sRight.startsWith(tgtEnviron) == false) && (sRight.startsWith(prodEnviron) == false) ) {
				 errit("Right hand side database does not start with [" + tgtEnviron + "] nor [" + prodEnviron + "] in parameter [" + sLeft + " -> " + sRight + "]" );
				 isOK=false;
			 }
		  }	
		  // option report
		  logit(5,"OPTION [SINGLE FILE     = " + makeSingleFile + "]");
		  logit(5,"OPTION [REDIRECT SCRIPT = " + makeRedirectScript + "]");
		  logit(5,"OPTION [DEPLOY SCRIPT   = " + makeDeployScript + "]");
		  logit(5,"OPTION [COMPARE REPORT  = " + makeCompareReport + "]");
		  logit(5,"OPTION [TEST SCRIPT     = " + makeTestScript + "]");
		  logit(5,"OPTION [SET ECHO        = " + setEcho + "]");
		  logit(5,"OPTION [SET NO ERROR    = " + setNoError + "]");
          //		  
	  }
	  catch(Exception e) {
	  	errit("Cannot read from [" + FName + "] " + e.getMessage());
	  	return false;
	  }
	  return isOK;
	}
	
	// put an extra space around ( and )
	// KB 24NOV - but not inside quotes
    //----------------------------------------------------------------
    private String expand_round_brackets(String sIn)
    //----------------------------------------------------------------
    {
      String delimList   = "()";
      char[] delimBuffer = delimList.toCharArray();
      String sRet = "";
      char[] buf = sIn.toCharArray();
      boolean found=false;
      boolean inKwoot = false;
      
      for(int i=0;i<buf.length;i++)
      {
        found = false;
        if( buf[i] == (char)'\'') inKwoot = !inKwoot;
        if( inKwoot == false ) {
         for(int j=0;j<delimBuffer.length;j++)  
         {
           if( delimBuffer[j] == buf[i] ) {sRet = sRet + (char)0x20 + buf[i] + (char)0x20; found = true; break; }  
         }
        }
        if( found ) continue;
        sRet = sRet + buf[i];
       }
       return sRet;
    }
	
    //----------------------------------------------------------------
    private String replace_space_in_quote(String sIn)
    //----------------------------------------------------------------
    {
    	 String sRet = "";
    	 boolean sFound=false;
         char[] buf = sIn.toCharArray();
         for(int i=0;i<buf.length;i++)
         {
        	 if( buf[i] == (char)'\'') sFound = !sFound;
        	 if( (buf[i] == (char)0x20) && (sFound) ) sRet += 'µ';
        	 else sRet += buf[i];
         }
         if( sFound ) errit( "Did not find closing quote " + sIn);
         return sRet;
    }
	
    //----------------------------------------------------------------
    private String unsetMu(String sIn)
    //----------------------------------------------------------------
    {
    	 String sRet = "";
    	 char[] buf = sIn.toCharArray();
         for(int i=0;i<buf.length;i++)
         {
        	 if( (buf[i] == (char)'µ')) sRet += " ";
        	 else sRet += buf[i];
         }
         return sRet;
    }
    
    //----------------------------------------------------------------
    private String expand_comma_outside_quote(String sIn)
    //----------------------------------------------------------------
    {
    	 String sRet = "";
    	 boolean sFound=false;
         char[] buf = sIn.toCharArray();
         for(int i=0;i<buf.length;i++)
         {
        	 if( buf[i] == (char)'\'') sFound = !sFound;
        	 if( (buf[i] == (char)',') && (!sFound) ) sRet += " , ";
        	 else sRet += buf[i];
         }
         if( sFound ) errit( "Did not find closing quote " + sIn);
         return sRet;
    }
    
    //----------------------------------------------------------------
    private String replace_expand_semicolon_in_quote(String sIn)
    //----------------------------------------------------------------
    {
    	 String sRet = "";
    	 boolean sFound=false;
         char[] buf = sIn.toCharArray();
         for(int i=0;i<buf.length;i++)
         {
        	 if( buf[i] == (char)'\'') sFound = !sFound;
        	 if( (buf[i] == (char)';') && (sFound) ) sRet += '§';
        	 if( (buf[i] == (char)';') && (!sFound) ) sRet += " ; ";
        	 else sRet += buf[i];
         }
         if( sFound ) errit( "Did not find closing quote " + sIn);
         return sRet;
    }
        
    //----------------------------------------------------------------
    private String unsetPara(String sIn)
    //----------------------------------------------------------------
    {
    	 String sRet = "";
         char[] buf = sIn.toCharArray();
         for(int i=0;i<buf.length;i++)
         {
        	 if( (buf[i] == (char)'§')) sRet += ";";
        	 else sRet += buf[i];
         }
         return sRet;
    }
    
    //----------------------------------------------------------------    
    private boolean isViewExcluded(String sIn)
    //----------------------------------------------------------------    
    {
    	for(int i=0;i<excludeViewList.size();i++)
    	{
           if( sIn.trim().compareToIgnoreCase( excludeViewList.get(i).trim()) == 0) return true; 		
    	}
    	return false;
    }
    
    //----------------------------------------------------------------    
    private boolean isViewIncluded(String sIn)
    //----------------------------------------------------------------    
    {
    	for(int i=0;i<includeViewList.size();i++)
    	{
           if( sIn.trim().compareToIgnoreCase( includeViewList.get(i).trim()) == 0) return true; 		
    	}
    	return false;
    }
    
    //----------------------------------------------------------------    
	private boolean normaliseer(String FName , String sTipe)
	//----------------------------------------------------------------
	{
	    logit(9,"Normalizing [" + FName + "]");
	    //
	    if( sTipe.compareToIgnoreCase("SRC") == 0 ) {
	    	srcViewList = new ArrayList<MyView>();
	    }
	    else
	    if( sTipe.compareToIgnoreCase("TGT") == 0 ) {
	    	tgtViewList = new ArrayList<MyView>();	
	    }
	    else {
	    	errit("Wrong option [" + sTipe + "]");
	    	return false;
	    }
	    //
	    String ENCODING = xMSet.xU.getEncoding(FName);
  		try {
	  			  int linesRead=0;
	  			  BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(FName),ENCODING));
	  			  String sLijn = null;
	  			  String sDML="";
	  			  String sViewName="";
	  			  int haakjesdiepte=0;
	  			  int createSekwentie = 0;
	  			  boolean endOfDML=false;
	  			  while ((sLijn=reader.readLine()) != null) {
	  	       		 linesRead++;
	  	       		 // ignore comments
  	       			 if( sLijn.trim().startsWith("--") ) continue;
  	       			 // read until ; - not surrounded by (, '
	  	       		 if( sLijn.trim().length() < 1 ) continue;
	  	       		 // put an extra space around ( and )
	  	       		 sLijn = expand_round_brackets(sLijn);
	  	       	     // put an extra space around , unless between single quotes
	  	       		 sLijn = expand_comma_outside_quote(sLijn);
	 	       		 // replace the space between single quotes by µ
	  	       		 sLijn = replace_space_in_quote(sLijn);
	 	       		 // replace the ; between single quotes by §  ELSE replace by space ; space
	  	       		 sLijn = replace_expand_semicolon_in_quote(sLijn);
	  	       		 
	  	      	     // parse on spaces
	  	       		 StringTokenizer st = new StringTokenizer(sLijn, " ");
	  	       		 while( st.hasMoreTokens() )
	  	       		 {
	  	       			 String sElem = st.nextToken();
	  	       			 
	  	       			 //
	  	       			 if( sElem.trim().compareToIgnoreCase("(")==0) haakjesdiepte++;
	  	       		     if( sElem.trim().compareToIgnoreCase(")")==0) haakjesdiepte--;	 
	  	       			 
	  	       		     // indien een CREATE OR REPLACE VIEW dan starten
	  	       		     if( (sElem.compareToIgnoreCase("CREATE") == 0 ) && (createSekwentie == 0) ) {createSekwentie++; continue;}
	  	       		     if( (sElem.compareToIgnoreCase("OR") == 0 ) && (createSekwentie == 1) ) {createSekwentie++; continue; }
	  	       		     if( (sElem.compareToIgnoreCase("REPLACE") == 0 ) && (createSekwentie == 2) ) {createSekwentie++; continue;}
	  	       		     if( (sElem.compareToIgnoreCase("VIEW") == 0 ) && (createSekwentie == 3) ) { createSekwentie++; continue; }
 	       		         // fetch viewname
	  	       		     if( createSekwentie == 4 ) {
	  	       		    	 createSekwentie++;
	  	       		    	 //
	  	       		    	 sViewName = sElem.trim().toUpperCase();
	  	       		    	 continue;
	  	       		     }
	  	       		     // 
	  	       		     if( (sElem.compareToIgnoreCase(";") == 0) && (haakjesdiepte==0)  ) {
	  	       		    	 createSekwentie=0;
	  	       		    	 endOfDML=true;
	  	       		    	 sDML += " " + sLijn;
	  	       		     }
	  	       		    
	  	       		     // indien geen ; doorgaan
	  	       		 }
	  	       		 
	  	       		 if( !endOfDML ) {
	  	       			 sDML += " " + sLijn;
	  	       		 }
	  	       		 else {
	  	       		     MyView mv = store_DML( sViewName , sDML.trim() );
	  	       		     if( mv == null ) return false;
	  	       		     boolean remove = true;
	  	       		     //  Include - Exclude
	  	       		     if( includeMode == false ) {
	  	       		       if( isViewExcluded( mv.ShortName ) ) remove = true; else remove = false;
	  	       		     }
	  	       		     else {
	  	       		       if( isViewIncluded( mv.ShortName ) ) remove = false; else remove = true;
	  	       		     }
	  	       		     if( remove == false ) {
	  	       		       if( sTipe.compareToIgnoreCase("SRC") == 0 ) srcViewList.add(mv);
	  	       		                                              else tgtViewList.add(mv);
	  	       		     }
	  	       		     else {
	  	       		    	 logit(1,"Excluding view [" + mv.ShortName + "]");
	  	       		     }
	  	       		     // resettten
	  	       			 endOfDML=false;
	  	       			 sDML="";
	  	       		 }
	  	       		 
	  			  }
	  			  reader.close();
	  			  // set dependencies
	  			 if( sTipe.compareToIgnoreCase("SRC") == 0 ) setCreateDependencies(srcViewList);
                                                         else setCreateDependencies(tgtViewList);
	  			  // unset µ and §
	  			 if( sTipe.compareToIgnoreCase("SRC") == 0 ) unsetMarkers(srcViewList);
                                                        else unsetMarkers(tgtViewList);
  		}
  		catch(Exception e ) {
  			errit("Error reading [" + FName + "]" + e.getMessage());
  			return false;
  		}
  		//
		return true;
	}
		
	//----------------------------------------------------------------
	private void unsetMarkers(ArrayList<MyView> lst)
	//----------------------------------------------------------------
	{
		for(int i=0;i<lst.size();i++)
		{
			MyView vw = lst.get(i);
			for(int j=0;j<vw.tlist.size();j++)
			{
				ViewToken tok = vw.tlist.get(j);
				tok.value = unsetMu( tok.value );
				tok.value = unsetPara( tok.value );
			}
		}
	}
	
	//----------------------------------------------------------------
	private SQLCOMMAND getSQLCOMMAND(String sIn , boolean verbose)
	//----------------------------------------------------------------
	{
		for(int i=0;i<SQLCOMMAND.values().length;i++)
		{
			if( SQLCOMMAND.values()[i].toString().compareToIgnoreCase(sIn) == 0 ) return SQLCOMMAND.values()[i];
		}
		if( verbose ) errit("Unsupported SQLCOMMAND [" + sIn + "]");
		return null;
	}
		
	//----------------------------------------------------------------
	private SQLFUNCTION getSQLFUNCTION(String sIn , boolean verbose)
	//----------------------------------------------------------------
	{
			for(int i=0;i<SQLFUNCTION.values().length;i++)
			{
				if( SQLFUNCTION.values()[i].toString().compareToIgnoreCase(sIn) == 0 ) return SQLFUNCTION.values()[i];
			}
			if( verbose ) errit("Unsupported SQLFUNCTION [" + sIn + "]");
			return null;
	}
	
	//----------------------------------------------------------------
    private boolean isSQLOPERATOR(String sIn)
    //----------------------------------------------------------------
    {
      for(int j=0;j<SQLOperatorsBuffer.length;j++)  
      {
           if( (""+SQLOperatorsBuffer[j]).compareToIgnoreCase(sIn)==0) return true;  
      }
      return false;  
    }
    
    //----------------------------------------------------------------
	private VIEWTOKENTYPE getViewTokenType( String sIn )
	//----------------------------------------------------------------
	{
		VIEWTOKENTYPE vt = VIEWTOKENTYPE.UNKNOWN;
		//  SQL Commannd
		if( getSQLCOMMAND( sIn , false ) != null ) {
		    vt = VIEWTOKENTYPE.SQLCOMMAND;	
		}
		//
		if( vt ==  VIEWTOKENTYPE.UNKNOWN ) {
		  if( getSQLFUNCTION( sIn , false ) != null ) vt = VIEWTOKENTYPE.SQLFUNCTION;
		}
		//
		if( vt ==  VIEWTOKENTYPE.UNKNOWN ) {
		   if ( isSQLOPERATOR(sIn) ) {
        	   vt = VIEWTOKENTYPE.OPERATOR;	
     	   }
		}
		// Maybe a column or table
		if( vt == VIEWTOKENTYPE.UNKNOWN ) {
			if( sIn.indexOf(".") > 0 ) {
				// exclude numerics
				if( xMSet.xU.isNumeric(sIn) == false ) vt = VIEWTOKENTYPE.ATTRIBUTE;
			}
		}
		return vt;
	}
	
	
    // Rule a table is preceded by token FROM or  inner/outer/left/right/cross JOIN
	//----------------------------------------------------------------
	private boolean CouldThisBeATable( MyView mview , String sElem )
	//----------------------------------------------------------------
	{
		SQLCOMMAND lastcommand = null;
		for(int i=0;i<mview.tlist.size();i++)
		{
		   if( mview.tlist.get(i).tipe == VIEWTOKENTYPE.SQLCOMMAND ) lastcommand =  mview.tlist.get(i).sqlcommand;
		}
		if( lastcommand == null ) return false;
		if( lastcommand == SQLCOMMAND.FROM )  return true;
		if( lastcommand == SQLCOMMAND.JOIN )  return true;
		return false;
	}
	
    // Rule a table is preceded by token VIEW
	//----------------------------------------------------------------
	private boolean CouldThisBeAView( MyView mview , String sElem )
		//----------------------------------------------------------------
	{
			SQLCOMMAND lastcommand = null;
			for(int i=0;i<mview.tlist.size();i++)
			{
			   if( mview.tlist.get(i).tipe == VIEWTOKENTYPE.SQLCOMMAND ) lastcommand =  mview.tlist.get(i).sqlcommand;
			}
			if( lastcommand == null ) return false;
			if( lastcommand == SQLCOMMAND.VIEW )  return true;
		
			return false;
	}
	
	//----------------------------------------------------------------
	private int countDots(String sIn)
	//----------------------------------------------------------------
	{
		 int counter=0;
	     char[] buf = sIn.toCharArray();
         for(int i=0;i<buf.length;i++)
         {
        	 if( (buf[i] == (char)'.')) counter++;
         }
         return counter;
	}
	
	//----------------------------------------------------------------
	private void setLegs( ViewToken tok , String sElem )
	//----------------------------------------------------------------
	{
		int ndots = countDots(sElem);
		//  A.B
		if( ndots < 1) return;
		if( ndots == 1 ) {
			tok.Leg1 = null;
			tok.Leg2 = null;
		    tok.Leg3 = sElem.substring(0,sElem.indexOf("."));
	        tok.Leg4 = sElem.substring(sElem.indexOf(".") + 1);
	   }
		// A.B.C
		else if( ndots == 2 ) {
			tok.Leg1 = null;
			tok.Leg2 = sElem.substring(0,sElem.indexOf("."));
	        tok.Leg3 = sElem.substring(sElem.indexOf(".") + 1,sElem.lastIndexOf("."));
	        tok.Leg4 = sElem.substring(sElem.lastIndexOf(".") + 1);
		}
		else if (ndots == 3 ) {
			tok.Leg1 = sElem.substring(0,sElem.indexOf("."));
	        tok.Leg4 = sElem.substring(sElem.lastIndexOf(".") + 1);
	        //
	        String twee =  sElem.substring(sElem.indexOf(".") + 1,sElem.lastIndexOf("."));;
	        tok.Leg2 = twee.substring(0,twee.indexOf("."));
	        tok.Leg3 = twee.substring(twee.indexOf(".") + 1);
	   }
		else {
			errit("Too many dots [" + sElem + "]");
		}
		/*
		if( tok.Leg1 != null ) {
		if( tok.Leg1.indexOf(".") >= 0) errit( "LEG1 " + tok.Leg1 + " [" + sElem + "] " + tok.tipe);
		}
		if( tok.Leg2.indexOf(".") >= 0) errit( "LEG2 " + tok.Leg2 + " [" + sElem + "] " + tok.tipe);
		if( tok.Leg3.indexOf(".") >= 0) errit( "LEG3 " + tok.Leg3 + " [" + sElem + "] " + tok.tipe);
		*/
	}
	
	//----------------------------------------------------------------
	private MyView store_DML(String sViewName , String sDML )
	//----------------------------------------------------------------
	{
		// Just make a visual progress indicator
		System.out.print(".");
		//
		MyView mview = new MyView(sViewName);
		//
		int haakjesdiepte=0;
		StringTokenizer st = new StringTokenizer(sDML, " ");
	 	while( st.hasMoreTokens() )
     	{
     			 String sElem = st.nextToken();
     			 //
     			 if( sElem.trim().compareToIgnoreCase("(")==0) haakjesdiepte++;
       		     if( sElem.trim().compareToIgnoreCase(")")==0) haakjesdiepte--;	 
     			 //
     			 VIEWTOKENTYPE vtipe = getViewTokenType( sElem );
     			 //
     			 ViewToken tok = new ViewToken(sElem , haakjesdiepte  , vtipe );
     			 if( tok.tipe == VIEWTOKENTYPE.SQLCOMMAND ) {
     				 tok.sqlcommand = getSQLCOMMAND(sElem,false);
     			 }
     			 //
     			 if( tok.tipe == VIEWTOKENTYPE.SQLFUNCTION ) {
     				 tok.sqlfunction = getSQLFUNCTION(sElem,false);
     			 }
     	         // if this is an attribute see if it is a table
     			 // Rule a table is preceded by FROM or  inner/outer/left/right/cross JOIN
     			 if( tok.tipe == VIEWTOKENTYPE.ATTRIBUTE ) {
     				 if( CouldThisBeATable( mview , sElem) ) tok.tipe = VIEWTOKENTYPE.TABLE;
     				 if( CouldThisBeAView( mview , sElem) ) tok.tipe = VIEWTOKENTYPE.VIEW;
     			 }
     			 //
     			 if( (tok.tipe == VIEWTOKENTYPE.ATTRIBUTE) || (tok.tipe == VIEWTOKENTYPE.TABLE) || (tok.tipe == VIEWTOKENTYPE.VIEW) ) {
     				 setLegs( tok , sElem );
     			 }
     			 //
     			 mview.tlist.add(tok);
	 	}
        // Look for the view
	 	mview.ShortName = null;
		for(int j=0;j<mview.tlist.size();j++)
		{
			ViewToken tok = mview.tlist.get(j);
			if( tok.tipe == VIEWTOKENTYPE.VIEW ) {
				if( tok.value.compareToIgnoreCase(mview.FullName)  != 0 ) {
					errit("Viewnames do not match [" + mview.FullName + "] [" + tok.value + "]");
					return null;
				}
				mview.ShortName = tok.Leg4;
				break;
			}
		}
     	if( mview.ShortName == null ) {
     		errit("Could not derive short view name [" + mview.FullName + "]");
     		return null;
     	}
		//
		
		return mview;
	}
	
	//---------------------------------------------------------------------------------
	private boolean extractAllDatabaseAndUserNames()
	//---------------------------------------------------------------------------------
	{
		
		
		srcPrefixList = new ArrayList<MyPrefix>();
		extractDatabaseAndUserNames( srcViewList , srcPrefixList);
	//errit("=====================");
		tgtPrefixList = new ArrayList<MyPrefix>();
		extractDatabaseAndUserNames( tgtViewList , tgtPrefixList);
		//dumpDebugInfo( srcViewList );
		
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean assessQuality()
	//---------------------------------------------------------------------------------
	{
		 if( assessQualityViewMetadata( srcViewList ) == false ) return false;
		 if( assessQualityViewMetadata( tgtViewList ) == false ) return false;
		
		 // de patronen dien een DB bevatten moeten geldige DB hebben
		 // de patronen die geen DB bevatten mogen dbname niet bevatten
		 
		 if ( checkDatabaseNames(srcViewList,0) == false ) return false;
		 //if ( checkDatabaseNames(tgtViewList,1) == false ) return false;
		 return true;
	}

	
	//---------------------------------------------------------------------------------
	private boolean checkDatabaseNames(ArrayList<MyView> vlist, int tipe)
	//---------------------------------------------------------------------------------
	{
		boolean isOK=true;
	    for(int i=0;i<vlist.size();i++)
	    {
	      MyView vw = vlist.get(i);
	      for(int k=0;k<vw.tlist.size();k++)
	      {
	    	  ViewToken tok = vw.tlist.get(k);
	    	  if( tok.pat == PATTERN.TABLE_PAT_01 ) {  // null DATABASE EDW tabelname
	    		  if( checkDatabaseName( tok.Leg2 , tipe ) == false ) isOK=false;
	    	  }
	    	  if( tok.pat == PATTERN.ATTR_PAT_02 ) {  // DATABASENAME EDW TABELNAME COLNAME
	    		  if( checkDatabaseName( tok.Leg1 , tipe ) == false ) isOK=false;
	    	  }
	      }
	    }
		return isOK;
	}
	
	//---------------------------------------------------------------------------------
	private boolean checkDatabaseName(String sIn, int tipe)
	//---------------------------------------------------------------------------------
	{
	   for(int i=0;i<dbList.size();i++)
	   {
		   DBNameTranslator x = dbList.get(i);
		   if( x.srcDBName == null ) continue;
		   if( x.tgtDBName == null ) continue;
		   if( (tipe == 0) && (x.srcDBName.compareToIgnoreCase(sIn)==0)) {
			    return true;
		   }
		   if( (tipe == 1) && (x.tgtDBName.compareToIgnoreCase(sIn)==0)) {
			    return true;
		   }
	   }
	   errit("Found Database Name [" + sIn + "] on view statement which is not defined in parameterfile [tipe=" + tipe + "]");
	   return false;
	}
	
	//---------------------------------------------------------------------------------
	private boolean assessQualityViewMetadata(ArrayList<MyView> vlist )
	//---------------------------------------------------------------------------------
	{
		      // TABLE  pattern
		      //   TABLE_PAT_01 null.database.EDW.tablename  
		      //   TABLE_PAT_02 null.null.EDW.tablename    
		      //   TABLE_PAT_03 null.null.DEFINITION_SCHEMA.tablename 
		      // VIEW pattern
		      //   VIEW_PAT_01  null.null.username.view
		      // ATTRIBUTE
		      //   ATTR_PAT_01  null.null.{ALIAS/TABLENAME}.attribute
		      //   ATTR_PAT_02  databasename.EDW.tablename.attribute
		      boolean isOK=true;
			  for(int i=0;i<vlist.size();i++)
		      {
				 MyView vw = vlist.get(i);
		      	 for(int j=0;j<vw.tlist.size();j++)
		    	 {
		      		 ViewToken tok = vlist.get(i).tlist.get(j);
		      		 // TABLE
		      		 if( tok.tipe == VIEWTOKENTYPE.TABLE  ) {
		      			vlist.get(i).tlist.get(j).pat = PATTERN.ERROR; 
		      			if( tok.Leg1 != null ) {
		      				errit("TABLE Probably error on table LEG1 is not empty VW=" + vw.ShortName + " TABLE=" + tok.value);
		      				isOK=false;
		      				continue;
		      			}
		      			if( tok.Leg2 != null ) {  // null.database.EDW.table
		      				if( tok.Leg3 == null ) {
		      					errit("TABLE Pattern mismatch null.databasename.EDW.tablename VW=" + vw.ShortName + " TABLE=" + tok.value);
			      				isOK=false;
			      				continue;
		      				}
		      				else
		      				if( tok.Leg3.compareToIgnoreCase("EDW")!=0) {
		      					errit("TABLE Pattern mismatch null.databasename.EDW.tablename VW=" + vw.ShortName + " TABLE=" + tok.value);
			      				isOK=false;
			      				continue;
		      				}
		      				vlist.get(i).tlist.get(j).pat = PATTERN.TABLE_PAT_01;
		      				continue;
		      			}
		      			if( tok.Leg2 == null ) { // null.null.EDW.tablename
		      				if( tok.Leg3 == null ) {
		      					errit("TABLE Pattern mismatch null.databasename.EDW.tablename VW=" + vw.ShortName + " TABLE=" + tok.value);
			      				isOK=false;
			      				continue;
		      				}
		      				else
		      				if( (tok.Leg3.compareToIgnoreCase("EDW")!=0) && (tok.Leg3.compareToIgnoreCase("DEFINITION_SCHEMA")!=0) ) {
		      					errit("TABLE Pattern mismatch null.null.EDW.tablename VW=" + vw.ShortName + " TABLE=" + tok.value);
			      				isOK=false;
			      				continue;
		      				}
		      				if( tok.Leg3.compareToIgnoreCase("EDW") == 0 ) vlist.get(i).tlist.get(j).pat = PATTERN.TABLE_PAT_02;
		      				if( tok.Leg3.compareToIgnoreCase("DEFINITION_SCHEMA") == 0 ) vlist.get(i).tlist.get(j).pat = PATTERN.TABLE_PAT_03;
			     			continue;
		      			}		
		      			isOK=false;
		      			errit("should not come here");
		      			continue;
		      		 }
		      		 
		      		 // VIEW
		      		 if( tok.tipe == VIEWTOKENTYPE.VIEW  ) {
		      			vlist.get(i).tlist.get(j).pat = PATTERN.ERROR;
		      			if( !((tok.Leg1 == null) && (tok.Leg2 == null)) ) {
		      				errit("VIEW Pattern mismatch null.null.USERNAME.VIEWname VW=" + vw.ShortName + " VIEW=" + tok.value);
		      				isOK=false;
		      				continue;
		      			}
		      			else
		      			if( tok.Leg3 == null ) {
	      					errit("VIEW Pattern mismatch null.null.USERNAME.VIEWname VW=" + vw.ShortName + " VIEW=" + tok.value);
		      				isOK=false;
		      				continue;
	      				}
		      			else // leg3 is USER - so not EDW 
	      				if( !((tok.Leg3.compareToIgnoreCase("EDW")!=0) && (tok.Leg3.compareToIgnoreCase("DEFINITION_SCHEMA")!=0)) ) {
	      					errit("VIEW Pattern mismatch there should not be EDW in null.null.USERNAME.VIEWname VW=" + vw.ShortName + " VIEW=" + tok.value);
		      				isOK=false;
		      				continue;	
		      			}
		      			vlist.get(i).tlist.get(j).pat = PATTERN.VIEW_PAT_01;
	      				continue;
		      		 }
		      		 
		      		 // ATTRIBUTE
		      		if( tok.tipe == VIEWTOKENTYPE.ATTRIBUTE  ) {
		      			 vlist.get(i).tlist.get(j).pat = PATTERN.ERROR;
		      			 if( tok.Leg1 != null ) {   // databasename.EDW.tablename.attribute
		      				 if( (tok.Leg2 == null) || (tok.Leg3 == null) ) {
		      					errit("ATTR Pattern mismatch database.EDW.tablename.attribute VW=" + vw.ShortName + " ATTR=" + tok.value);
			      				isOK=false;
			      				continue;	 
		      				 }
		      				 if( tok.Leg2.compareToIgnoreCase("EDW") != 0 ) {
		      					errit("ATTR Pattern mismatch database.EDW.tablename.attribute VW=" + vw.ShortName + " ATTR=" + tok.value);
			      				isOK=false;
			      				continue;
		      				 }
		      				 // OK
		      				 vlist.get(i).tlist.get(j).pat = PATTERN.ATTR_PAT_02;
		      				 continue;
		      			 }
		      			 if( tok.Leg2 != null ) {  // null.DEFINITION_SCHEMA.table.attr
		      				 if( tok.Leg2.compareToIgnoreCase("DEFINITION_SCHEMA") != 0 ) {
			      					errit("ATTR Pattern mismatch database.EDW.tablename.attribute VW=" + vw.ShortName + " ATTR=" + tok.value);
				      				isOK=false;
				      				continue;
			      			 }
		      				 // OK  ATTR3
		      				 vlist.get(i).tlist.get(j).pat = PATTERN.ATTR_PAT_03;
		      				 continue;
		      			 }
		      			 if( tok.Leg3 == null ) { // null.null.null.attribute
		      				 errit("ATTR Pattern mismatch NULL.NULL.NULL.attr not supported VW=" + vw.ShortName + " ATTR=" + tok.value);
		      				 isOK = false;
		      				 continue;
		      			 }
		      			 // ok
		      			 vlist.get(i).tlist.get(j).pat = PATTERN.ATTR_PAT_01;
		      			 continue;
		      		 }
		    	 }
		      }
			  
			  // aftercare
			  for(int i=0;i<vlist.size();i++)
			  {
				 MyView vw = vlist.get(i);
			     for(int j=0;j<vw.tlist.size();j++)
			     {
			    	 ViewToken tok = vlist.get(i).tlist.get(j);
			    	 if( (tok.tipe == VIEWTOKENTYPE.VIEW) || (tok.tipe == VIEWTOKENTYPE.TABLE) || (tok.tipe == VIEWTOKENTYPE.ATTRIBUTE)  ) {
			    		 if (tok.pat == PATTERN.UNKNOWN ) {
			    			 errit("Unprocessed token [" + tok.value + "]");
			    			 isOK=false;
			    		 }
			    	 }
			    }
			  }
			  return isOK;
	}
		
	
	//---------------------------------------------------------------------------------
	private void dumpDebugInfo(ArrayList<MyView> vlist )
	//---------------------------------------------------------------------------------
	{
	      String sl="";
		  for(int i=0;i<vlist.size();i++)
	      {
			 logit(1," ==> " + vlist.get(i).FullName );
	      	 for(int j=0;j<vlist.get(i).tlist.size();j++)
	    	 {
	    		 sl="";
	    		 ViewToken tok = vlist.get(i).tlist.get(j);
	    		 //if( !((tok.tipe == VIEWTOKENTYPE.VIEW) || (tok.tipe == VIEWTOKENTYPE.TABLE) || (tok.tipe == VIEWTOKENTYPE.ATTRIBUTE)) ) continue;
	    		 //if( !(tok.tipe == VIEWTOKENTYPE.TABLE)  ) continue;
	    		 //if( !(tok.tipe == VIEWTOKENTYPE.VIEW)  ) continue;
	    		 //if( !(tok.tipe == VIEWTOKENTYPE.ATTRIBUTE)  ) continue;
				 			
	    		 if( tok.Leg1 != null ) {
	    			 sl = "[" + tok.Leg1 + "][" + tok.Leg2 + "][" + tok.Leg3 + "][" + tok.Leg4 + "]";
	    		 }
	    		 else 
	    		 if( tok.Leg2 != null ) {
	    			 sl = "[" + tok.Leg2 + "][" + tok.Leg3 + "][" + tok.Leg4 + "]";	 
		    	 }
	    		 else
	    		 if( tok.Leg3 != null ) {
	    			 sl = "[" + tok.Leg3 + "][" + tok.Leg4 + "]";
	    		 }
	    		 else {
	    			 sl = "[" + tok.Leg4 + "]"; 
	    		 }
	    		 logit( 1 , tok.tipe + " " + sl );
	    	 }
	      	 logit(1," == ");
	      }
	}
	
	
	//---------------------------------------------------------------------------------
	private void extractDatabaseAndUserNames(ArrayList<MyView> vlist , ArrayList<MyPrefix> plist)
	//---------------------------------------------------------------------------------
	{
      for(int i=0;i<vlist.size();i++)
      {
    	 for(int j=0;j<vlist.get(i).tlist.size();j++)
    	 {
    		 addMyPrefix( vlist.get(i).tlist.get(j).Leg1 , plist);
    		 addMyPrefix( vlist.get(i).tlist.get(j).Leg2 , plist);
    		 addMyPrefix( vlist.get(i).tlist.get(j).Leg3 , plist);
    	 }
      }
	}
	
	
	//---------------------------------------------------------------------------------
	private void addMyPrefix( String sIn ,  ArrayList<MyPrefix> plist)
	//---------------------------------------------------------------------------------
	{
		if( sIn == null ) return;
		String sPrefix = sIn.trim().toUpperCase();
		if( sPrefix.length() < 1 ) return;
		int idx = -1;
		for(int i=0;i<plist.size();i++)
		{
			if( plist.get(i).value.compareToIgnoreCase(sPrefix) == 0 ) {
				idx = i;
				break;
			}
		}
		if( idx < 0) {
			MyPrefix x = new MyPrefix();
			x.value = sPrefix.trim().toUpperCase();
			plist.add( x );
			//errit( x.value );
		}
	}
	
	
	//---------------------------------------------------------------------------------
	private boolean writeExcelLine( String TabSheetName , String[] sLineIn )
	//---------------------------------------------------------------------------------
	{
				Object[] oi = new Object[sLineIn.length];
				for(int k=0;k<sLineIn.length;k++) oi[k]=sLineIn[k];
				return xcw.addRow( TabSheetName , oi);
	}
				
	//---------------------------------------------------------------------------------
	private boolean writeExcelLine( String TabSheetName , String[] sLineIn , xlsxWriterMark2.XCOLOR[] aColorIn)
	//---------------------------------------------------------------------------------
	{
				Object[] oi = new Object[sLineIn.length];
				Object[] cc = new Object[sLineIn.length];
				for(int k=0;k<sLineIn.length;k++) {
							oi[k]=sLineIn[k];
							cc[k]=aColorIn[k];
				}
				return xcw.addRow( TabSheetName , oi , cc );
	}
	
	//---------------------------------------------------------------------------------
	private boolean maakHeader(String SheetName , String sHeader)
	//---------------------------------------------------------------------------------
	{
			StringTokenizer st = new StringTokenizer(sHeader,"|");
			int nbr = st.countTokens();
			String lijn[] = new String[nbr];
			int i=-1;
			while(st.hasMoreTokens()) 
			{ 
				i++;
				lijn[i] = st.nextToken().trim();
			}	
			// kleur
			xlsxWriterMark2.XCOLOR[] aColor = new xlsxWriterMark2.XCOLOR[nbr];
			for(int k=0;k<aColor.length;k++) aColor[k] = xlsxWriterMark2.XCOLOR.BLUE;
			//
			if( writeExcelLine( SheetName , lijn , aColor) == false ) return false;
			return true;
	}
	
	//----------------------------------------------------------------
	private boolean doAllViewCompare()
	//----------------------------------------------------------------
	{
		if( srcViewList == null ) {
			errit("Empty srcViewList");
			return false;
		}
		if( tgtViewList == null ) {
			errit("Empty tgtViewList");
			return false;
		}
	    //	
		if( this.makeCompareReport == false ) {
			logit(1,"The creation of a VIEW camparison report will be skipped OPTION = MAKE_COMPARE_REPORT has not been set");
			return true;
		}
		//
		String FCore = getCoreFileName(SrcDDLShort);
		if( FCore == null ) {
			errit("Cannot create target XLS");
			return false;
		}
		if( FCore.length() < 2 ) {
			errit("Cannot create target XLS - too short [" + FCore + "]");
			return false;
		}
		String FExcelName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Export" + xMSet.xU.ctSlash + "Comparison-" + FCore + ".xlsx";
		if( xMSet.xU.IsBestand( FExcelName ) ) {
			if( xMSet.xU.VerwijderBestand( FExcelName ) == false ) {
				errit("Cannot remove Excel [" + FExcelName + "] - It is probably in use");
				return false;
			}
		}
		//
		logit( 5 , "Dumping to Excel [" + FExcelName + "]" );
		xcw = new xlsxWriterMark2( xMSet);
		if( xcw == null ) {
			errit("System error VII - no excelwriter");
			return false;
		}
		//
		SheetName = "Summary";
		xcw.addSheet(SheetName);
		String header = "Parameter|Value";
	    if( maakHeader( SheetName , header) == false ) return false;
	    //
		String lijn[] = new String[2];
		//
		lijn[0] = "Application"; 
		lijn[1] = xMSet.getApplicationId();
		if( writeExcelLine( SheetName , lijn ) == false ) return false;
		//
		lijn[0] = "Project"; 
		lijn[1] = xMSet.getCurrentProject() ;
		if( writeExcelLine( SheetName , lijn ) == false ) return false;
		//
		lijn[0] = "Created on"; 
		lijn[1] =  ""+xMSet.xU.prntStandardDateTime(System.currentTimeMillis());
		if( writeExcelLine( SheetName , lijn ) == false ) return false;
		//
		lijn[0] = "Created by"; 
		lijn[1] =  ""+xMSet.whoami();
		if( writeExcelLine( SheetName , lijn ) == false ) return false;
		//
		lijn[0] = "Source DDL View"; 
		lijn[1] = SrcDDLFull;
		if( writeExcelLine( SheetName , lijn ) == false ) return false;
		//
		lijn[0] = "Target DDL View"; 
		lijn[1] = TgtDDLFull;
		if( writeExcelLine( SheetName , lijn ) == false ) return false;
		
		//
		if( doCompareViewList( srcViewList , tgtViewList , 0 ) == false ) return false;
		if( doCompareViewList( tgtViewList , srcViewList , 1 ) == false ) return false;
		
		//
		if( xcw.dumpToExcel(FExcelName) == false ) return false;
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean doCompareViewList ( ArrayList<MyView> src , ArrayList<MyView> tgt  , int run)
	//----------------------------------------------------------------
	{
		SheetName = ( run == 0) ? "SourceAnomalyReport" : "TargetAnomayReport";
		xcw.addSheet(SheetName);
		// header
		String header = "Nbr|Depth|SourceView|TargetView|View|Anomaly|Env";
		if( maakHeader( SheetName , header) == false ) return false;
		
		//
		for(int i=0;i<src.size();i++)
		{
			MyView mv = src.get(i);
			// find matching view
			int idx = -1;
			for(int j=0;j<tgt.size();j++)
			{
			  if( mv.ShortName.compareToIgnoreCase(tgt.get(j).ShortName) == 0 ) {
				  idx = j;
				  break;
			  }
			}
			if( run != 0 ) continue;   // TGT to SRC comparison only on the names
			if( idx < 0 ) doDetailCompare( src.get(i) , null , i );
			         else doDetailCompare( src.get(i) , tgt.get(idx) , i );
		}

		// see if there is an environment mix on the target
		ArrayList<String> envs = new ArrayList<String>();
		for(int i=0;i<tgt.size();i++)
		{
			if(  tgt.get(i).environmentReferenced == null ) continue;
			String se = tgt.get(i).environmentReferenced.trim().toUpperCase();
			int idx=-1;
			for(int j=0;j<envs.size();j++)
			{
				if( envs.get(j).compareToIgnoreCase(se) == 0 ) {
					idx=j;
					break;
				}
			}
			if( idx < 0) envs.add(se);
		}
		if( envs.size() > 1 ) {
			String serr = "Consult Excel report. There is a mix of environments used [";
		    for(int i=0;i<envs.size();i++)
		    {
			 serr += envs.get(i) + ",";
		    }
		    serr += "]";
		    errit(serr);
		}
		
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean doDetailCompare( MyView src , MyView tgt , int counter)
	//----------------------------------------------------------------
	{
		//
		String lijn[] = new String[15];
		xlsxWriterMark2.XCOLOR kleur[] = new xlsxWriterMark2.XCOLOR[lijn.length];
		for(int i=0;i<kleur.length;i++) kleur[i] = xlsxWriterMark2.XCOLOR.NONE;
		for(int i=0;i<lijn.length;i++) lijn[i] = "";
		//
		lijn[0] = ""+(counter+1);
		lijn[1] = ""+src.dependency_depth;
		lijn[2] = src.FullName;
		lijn[3] = "";
		lijn[4] = src.ShortName;
		lijn[5] = "";
		lijn[6] = "";
		//
		if( tgt == null ) 
		{
		    lijn[5] = "Target view not found";	
		    kleur[5] = xlsxWriterMark2.XCOLOR.RED;
		    logit( 1 , lijn[1] + " " + lijn[2] + " " + lijn [3] + " " + lijn[4] );
			if( writeExcelLine( SheetName , lijn , kleur) == false ) return false;
			return false;
		}
		lijn[3] = tgt.FullName;
		//
		if( src.tlist.size() != tgt.tlist.size() ) {
			lijn[5] = "Object count does not match [" + src.tlist.size() + "] [" + tgt.tlist.size() + "]  -  ";
			logit( 1 , lijn[1] + " " + lijn[2] + " " + lijn [3] + " " + lijn[4] );
		}
		//
		boolean isOK=true;
		for(int i=0 ; i<src.tlist.size(); i++ )
		{
		  	ViewToken x = src.tlist.get(i);
		  	ViewToken y = tgt.tlist.get(i);
		  	//
		  	if( x.tipe != y.tipe ) {
		  		isOK = false;
		  		lijn[5] += "Object type mismatch on [" + x.value + "] [" + y.value + "]";
		  		break;
		  	}
		  	//
		  	if( (x.tipe == VIEWTOKENTYPE.VIEW) || (x.tipe == VIEWTOKENTYPE.TABLE) || (x.tipe == VIEWTOKENTYPE.ATTRIBUTE) ) {
		  		if( x.Leg4.compareToIgnoreCase(y.Leg4) != 0 ) {
		  			isOK = false;
		  			lijn[5] += "Object name mismatch on [" + x.value + "] [" + y.value + "]";
		  	  	break;
		  		}
		  		// KB 24NOV  -  see if the target table alternate between PROD and QA
		  		// PAT02 = null.database.edw.table and database = ENVIRONMENT_something
		  		if( (y.tipe == VIEWTOKENTYPE.TABLE) && (y.pat ==  PATTERN.TABLE_PAT_01) ) {
		  			int pdx = (y.Leg2 == null) ?  -1 : y.Leg2.trim().toUpperCase().indexOf("_");
		  			if( pdx > 0 ) {
		  				tgt.environmentReferenced = y.Leg2.trim().toUpperCase().substring(0,pdx);
		  			}
		  			// just add the db
		  			pdx=-1;
		  			for(int z=0;z<tgt.dblist.size();z++)
		  			{
		  				if( tgt.dblist.get(z).compareToIgnoreCase(y.Leg2.trim().toUpperCase()) == 0) {
		  					pdx=z;
		  					break;
		  				}
		  			}
		  			if( pdx < 0 ) tgt.dblist.add(y.Leg2.trim().toUpperCase());
		  		}
		  	}
		  	else {  // brute force compare
		  		if( x.value.compareToIgnoreCase(y.value) != 0 ) {
		  			isOK = false;
		  			lijn[5] += "Object value mismatch on [" + x.value + "] [" + y.value + "]";
				  	break;
		  		}
		  	}
		}
		if( isOK ) {
			lijn[5] += "OK";
		}
		else  {
			kleur[5] = xlsxWriterMark2.XCOLOR.RED;
			if( dumpViewDetailError(lijn[5] , src , tgt) == false ) return false;
		}
		
		logit( 1 , lijn[1] + " " + lijn[2] + " " + lijn [3] + " " + lijn[4] );
		lijn[6] = (tgt.environmentReferenced == null) ? "" : tgt.environmentReferenced;
		for(int i=0;i<tgt.dblist.size();i++)
		{
			int z = 6 + i;
			if( z >= lijn.length ) break;
			lijn[z] = tgt.dblist.get(i);
		}
		//
		if( writeExcelLine( SheetName , lijn , kleur) == false ) return false;
		//
		return true;
	}
	
	//----------------------------------------------------------------
	private void setCreateDependencies(ArrayList<MyView> lst)
	//----------------------------------------------------------------
	{
		for(int i=0;i<lst.size();i++)
		{
			lst.get(i).dependency_depth = 0;
		}
		// scan through all tables and see if it is dependent
		for(int i=0;i<lst.size();i++)
		{
			globaldiepte=0;
		    follow_parent( lst , i , 0);	
		    lst.get(i).dependency_depth = globaldiepte;
		}
	}
	
	//----------------------------------------------------------------
	private void follow_parent(ArrayList<MyView> lst, int idx ,  int diepte)
	//----------------------------------------------------------------
	{
		diepte++;
		if( diepte > globaldiepte ) globaldiepte = diepte;
		if( diepte > 100 ) {  // safety catch
			errit("Too deep");
			return;
		}
		MyView mv = lst.get(idx);
		for(int i=0; i<mv.tlist.size();i++)
		{
		  ViewToken tok = mv.tlist.get(i);
		  if( tok.tipe != VIEWTOKENTYPE.TABLE ) continue;
		  // this is a table ; but could it be that this is actually a VIEW wich is referred to
		  String stab = tok.Leg4;
		  // indien een STV.EDW.something dan niet niet zoeken - refereert naar andere DB - dus nietintern
		  // dus enkel zoeken indien de le1 en leg2 null zijn
		  if( tok.Leg1 != null ) continue;   // refers to another dataase
		  if( tok.Leg2 != null ) continue;   // refers to another dataase  null.DEV_EDW_STOV.
		  int pdx = getViewIndexViaName( lst , stab );
		  if( pdx < 0 )  continue;
          logit( 1 , "VIEW DEPENDENCIES :: " + mv.ShortName + " comprises [" + tok.Leg1 + "." + tok.Leg2 + "." + tok.Leg3 + "." + stab + "] WHICH REFERENCES [" + lst.get(pdx).FullName + "] ");
  	      follow_parent( lst , pdx , diepte);
		}
	}
	
	//----------------------------------------------------------------
	private int getViewIndexViaName( ArrayList<MyView> lst , String sName)
	//----------------------------------------------------------------
	{
		for(int i=0;i<lst.size();i++)
		{
			if( lst.get(i).ShortName.compareToIgnoreCase(sName) == 0 ) return i;
		}
		return -1;
	}
	
	//----------------------------------------------------------------
	private void writeline(String sIn)
	//----------------------------------------------------------------
	{
		fout.println(sIn);
	}
	
	//----------------------------------------------------------------
	private boolean rewriteDDLForDeployment()
	//----------------------------------------------------------------
	{
		// Make a deployment script
		if( makeDeployScript == true ) {
		  if( rewriteDDLForDeploymentDetail(SCRIPTTYPE.DEPLOY) == false ) return false;
		}
		// make redirect script
		if( this.makeRedirectScript == true ) {
		 if( rewriteDDLForDeploymentDetail(SCRIPTTYPE.REDIRECT) == false ) return false;
		}
        return true;		
	}
	
	//----------------------------------------------------------------
	private boolean initiateNewFile(SCRIPTTYPE tipe)
	//----------------------------------------------------------------
	{
		//
		String FCore = getCoreFileName(SrcDDLShort).trim();
		if( FCore == null ) {
			errit("Cannot create target DDL");
			return false;
		}
		if( FCore.length() < 2 ) {
			errit("Cannot create target DDL - too short [" + FCore + "]");
			return false;
		}
		String FName = null;
		/*
		if( tipe == SCRIPTTYPE.DEPLOY ) FName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Export" + xMSet.xU.ctSlash + "Deploy-" + FCore + ".txt";
		else if( tipe == SCRIPTTYPE.REDIRECT ) FName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Export" + xMSet.xU.ctSlash + "Dedirect-" + FCore + ".txt";
		else {
			errit("unsupported tipe - global error 3");
			return false;
		}
		*/
		//
		if( tipe == SCRIPTTYPE.TESTSQL ) fileCounter=48;
		fileCounter++;
		String sDir = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Export" + xMSet.xU.ctSlash;
		int offset = ( tipe == SCRIPTTYPE.DEPLOY ) ? 10 : 50;
		String FShort = String.format("%02d", offset+fileCounter) + "+" + promotionDatabaseName.toUpperCase().trim() + "+main.sql";
		FName = sDir + FShort;
		//
		fout = new gpPrintStream(FName , "UTF-8");
		writeline("--");
		//
		if( tipe == SCRIPTTYPE.DEPLOY )
		writeline("-- Generated View DDL script for deployment");
		else if( tipe == SCRIPTTYPE.REDIRECT )
		writeline("-- Generated View DDL script for PRODUCTION database redirecting");
		else if( tipe == SCRIPTTYPE.TESTSQL )
		writeline("-- Generated View test file");
		else {
			errit("Unkown type " + tipe);
			return false;
		}
		//
		writeline("-- Application     : " + xMSet.getApplicationId() );
	    writeline("-- Project         : " + xMSet.getCurrentProject() );
	    writeline("-- Created on      : " + xMSet.xU.prntStandardDateTime(System.currentTimeMillis()) );
	    writeline("-- Created by      : " + xMSet.whoami() );
	    //
	    if( (this.makeSingleFile) || (tipe == SCRIPTTYPE.TESTSQL ) )
   	    writeline("-- Number of views : " + srcViewList.size() );
	    else
	    writeline("-- View number     : " + (nbrViewsWritten+1) + " on " + srcViewList.size());
	    //
	    writeline("--");
	    //
	    if( setNoError ) {
	    	writeline("\\unset ON_ERROR_STOP");
	    	writeline("--");
	    }	
	    return true;
	}
	
	//----------------------------------------------------------------
	private boolean stopFile()
	//----------------------------------------------------------------
	{
		writeline("--");
		if( setNoError ) {
	    	writeline("\\set ON_ERROR_STOP ON");
	    	writeline("--");
	    }
	    writeline("");
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean rewriteDDLForDeploymentDetail(SCRIPTTYPE tipe)
	//----------------------------------------------------------------
	{
	    // Regenerate the DDL
		// define depth
		int maxdiepte=-1;
		for(int i=0;i<srcViewList.size();i++)
		{
			if( srcViewList.get(i).dependency_depth > maxdiepte ) maxdiepte = srcViewList.get(i).dependency_depth;
		}
		//
		//
	    nbrViewsWritten=0;
	    viewsInCurrentFile=0;
	    fileCounter=-1;
		//
	    if( initiateNewFile(tipe) == false ) return false;
		for(int diepte=0;diepte<=maxdiepte;diepte++)
		{
			for(int i=0;i<srcViewList.size();i++)
			{
				if( srcViewList.get(i).dependency_depth != diepte ) continue;
				if( rewriteViewForDeployment( srcViewList.get(i) , tipe) == false ) return false;
			}
		}
		 //
	    stopFile();
	    //
		fout.close();
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean doStartNewLine(ViewToken tok)
	//----------------------------------------------------------------
	{
		if( tok.tipe == VIEWTOKENTYPE.SQLCOMMAND ) {
			   if( (tok.sqlcommand == SQLCOMMAND.SELECT) || 
				   (tok.sqlcommand == SQLCOMMAND.JOIN) || 
				   (tok.sqlcommand == SQLCOMMAND.UNION) || 
				   (tok.sqlcommand == SQLCOMMAND.EXCEPT) || 
				   (tok.sqlcommand == SQLCOMMAND.MINUS) || 
				   (tok.sqlcommand == SQLCOMMAND.FROM) ) {
			          return true;
			   }
		}
		return false;
	}
	
	//----------------------------------------------------------------
	private boolean doCloseThisLine(ViewToken tok)
	//----------------------------------------------------------------
	{
		if( tok.tipe == VIEWTOKENTYPE.OPERATOR ) {
			   if( tok.value.compareToIgnoreCase(",") == 0 ) {
				   return true;
			   }
		   }
		   if( tok.tipe == VIEWTOKENTYPE.SQLCOMMAND ) {
			   if( (tok.sqlcommand == SQLCOMMAND.SELECT) || 
				   (tok.sqlcommand == SQLCOMMAND.FROM) || 
				   (tok.sqlcommand == SQLCOMMAND.UNION) || 
				   (tok.sqlcommand == SQLCOMMAND.EXCEPT) || 
				   (tok.sqlcommand == SQLCOMMAND.MINUS) || 
				   (tok.sqlcommand == SQLCOMMAND.JOIN) ) {
			          return true;
			   }
		   }
		   if( tok.tipe == VIEWTOKENTYPE.TABLE ) {
				  return true;
		   }
		   return false;
	}
	
	//----------------------------------------------------------------
	private String substituteDBName( String sIn , SCRIPTTYPE tipe)
	//----------------------------------------------------------------
	{
		// tipe 1 = $$EDW_ENV$$
		// tipe 2 = redirector
		if( tipe == SCRIPTTYPE.DEPLOY ) {
			String sRet = xMSet.xU.Remplaceer(sIn , (srcEnviron+"_") , "$$EDW_ENV$$_" );
			return sRet;
		}
		else if( tipe == SCRIPTTYPE.REDIRECT) {
			int idx =-1;
			for(int i=0;i<dbList.size();i++)
			{
				DBNameTranslator x = dbList.get(i);
				if( x.srcDBName == null ) continue;
				if( x.srcDBName.compareToIgnoreCase(sIn) == 0 ) {
					idx=i;
					break;
				}
			}
			if( idx < 0 ) {
				errit("Cannot find databasename [" + sIn + "] in any of the databases defined AA -> BB");
				return null;
			}
			DBNameTranslator x = dbList.get(idx);
			if( x.tgtDBName == null ) {
				errit("Cannot find translated databasename for [" + sIn + "]");
				return null;
			}
			return x.tgtDBName;
		}
		else {
			errit ("unsupported tipe - global error 4)");
		    return null;			
		}
	}
	
	//----------------------------------------------------------------
	private boolean rewriteViewForDeployment( MyView vw , SCRIPTTYPE tipe)
	//----------------------------------------------------------------
	{
		writeline( "-- " + vw.ShortName );
		if( setEcho ) {
			writeline( "\\echo creating " + vw.ShortName );
			writeline( "-- ");
		}
		//
		String sLine = "";
		String DBName = null;
		for(int i=0;i<vw.tlist.size();i++)
		{
		   ViewToken tok = vw.tlist.get(i);
		   //
		   // Start a new line	   
		   if( doStartNewLine( tok ) ) {
			   writeline(sLine.trim());
			   sLine = "";
		   }
           
           //		
		   switch(tok.pat )
		   {
		   case TABLE_PAT_01 : {
			   DBName= substituteDBName( tok.Leg2 , tipe);
			   if( DBName == null ) return false;
			   sLine += DBName.trim() + ".." + tok.Leg4 + " ";
			   break;
		       }
		   case TABLE_PAT_02 : {
			   sLine += tok.Leg4 + " ";
			   break;
		       }
		   case TABLE_PAT_03 : {
			   sLine += tok.Leg4 + " ";
			   break;
		       }
		   case VIEW_PAT_01 : {
			   sLine += tok.Leg4 + " ";
			   break;
		       }
		   case ATTR_PAT_01 : {
			   sLine += tok.Leg3 + "." + tok.Leg4 + " ";
			   break;
		       }
		   case ATTR_PAT_02 : {
			   DBName= substituteDBName( tok.Leg1 , tipe);
			   if( DBName == null ) return false;
			   sLine += DBName.trim() + ".." + tok.Leg3 + "." + tok.Leg4 + " ";
			   break;
		       }
		   case ATTR_PAT_03 : {
			   sLine += tok.Leg3 + "." + tok.Leg4 + " ";
			   break;
		       }
		   case UNKNOWN : {
			   sLine += tok.value + " ";
			   break;
		       }
		   default : {
			   errit("There is an untreated pattern [" + tok.pat + "]");
			   return false;
			  }
		   }
		   
		   // end by a new line
		   if( doCloseThisLine( tok )) {
			   writeline(sLine.trim());
			   sLine = "";
		   }
		   
		   
		}
		writeline(sLine);
		writeline("  ");
		writeline("  ");
		//
		nbrViewsWritten++;
		// do we need to swap to another file
		viewsInCurrentFile ++;
		if( makeSingleFile == false ) {
			int viewsPerFile = makeSingleFile == true ? 100000 : (srcViewList.size() / 45) + 1;
		    if( viewsInCurrentFile >= viewsPerFile ) {
		    	stopFile();
		    	if( fout != null ) fout.close();
		    	if( initiateNewFile(tipe) == false ) return false;
		    	viewsInCurrentFile = 0;
		    }
		}
	    //	
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean writeTestSQLFile()
	//----------------------------------------------------------------
	{
		if( makeTestScript == false ) return true;
	    if( initiateNewFile(SCRIPTTYPE.TESTSQL) == false ) return false;
	    for(int i=0;i<srcViewList.size();i++)
	    {
	       MyView vw = srcViewList.get(i);  	
	       String sLijn = "select '" + vw.ShortName + "' , count(1) from " + vw.ShortName + ";";
	       writeline("--");
	       writeline(sLijn);
	    }
	    stopFile();
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean dumpViewDetailError(String sErr , MyView src , MyView tgt)
	//----------------------------------------------------------------
	{
		globErrorCounter++;
		String SheetName = "ErrorDetail";
		if( globErrorCounter == 1 ) {
		  xcw.addSheet(SheetName);
		  String header = "One|Two";
		  if( maakHeader( SheetName , header) == false ) return false;
		}
		String lijn[] = new String[2];
		xlsxWriterMark2.XCOLOR kleur[] = new xlsxWriterMark2.XCOLOR[lijn.length];
		//
		for(int i=0;i<kleur.length;i++) kleur[i] = xlsxWriterMark2.XCOLOR.LIGHT_GREEN;
		lijn[0] = src.ShortName;
		lijn[1] = sErr;
		if( writeExcelLine( SheetName , lijn , kleur) == false ) return false;
		//
	    int max = src.tlist.size() > tgt.tlist.size() ?   src.tlist.size() :  tgt.tlist.size();
	    for(int i=0;i<max;i++)
	    {
	       lijn[0] = "";
	       lijn[1] = "";
	   	   for(int k=0;k<kleur.length;k++) kleur[k] = xlsxWriterMark2.XCOLOR.NONE;
		   //   
	       if( i < src.tlist.size() ) lijn[0] = src.tlist.get(i).value;
	       if( i < tgt.tlist.size() ) lijn[1] = tgt.tlist.get(i).value;
	       //
	       if( lijn[0].compareToIgnoreCase(lijn[1]) != 0 ) kleur[1] = xlsxWriterMark2.XCOLOR.LIGHT_RED;
	       if( writeExcelLine( SheetName , lijn , kleur) == false ) return false;
	    }
		//
		lijn[0] = "";
		lijn[1] = "";
		if( writeExcelLine( SheetName , lijn , kleur) == false ) return false;
		return true;
	}
}
