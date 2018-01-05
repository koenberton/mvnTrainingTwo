package pcGenerator.generator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import pcGenerator.ddl.readInfaXML;
import pcGenerator.powercenter.infaInstance;
import pcGenerator.powercenter.infaPair;
import pcGenerator.powercenter.infaSource;
import pcGenerator.powercenter.infaSourceQualifier;
import pcGenerator.powercenter.infaSourceQualifierField;
import generalpurpose.gpPrintStream;
import generalpurpose.gpUtils;
import generalpurpose.pcDevBoosterSettings;

public class generateMapping {
	
	enum PROGPOSITION { ROOT , POWERMART , REPOSITORY , FOLDER , SOURCE , TARGET , MAPPING , TRANSFORMATION }
	enum DO_CONNECT_TYPE {  NAME , RANK , IMPORT , UNKNOWN }
	
	pcDevBoosterSettings xMSet = null;
	gpUtils xU = null;
	generatorSrcTgtManager stManager = null;
	gpPrintStream tempout = null;
	 
	String CRLF = System.getProperty("line.separator");
	
	String sSourceList=null;
	String sTargetList=null;
	String TemplateName=null;
	String OutputTempFileName=null;
	int linesWritten=0;
	int linesRead=0;
	int sourceTracker=0;   
	int targetTracker=0;   
	PROGPOSITION progpos = PROGPOSITION.ROOT;
    //
	ArrayList<infaSourceQualifier> sqList = null;
	//
	ArrayList<infaInstance> instanceList = null;
	//
	// destined to store hints for the generator
	generatorMappingHints xHints=null;
    // support DO CONNECT
    class connectInfo {
    	int fromIdx = -1;
    	int toIdx = -1;
    	String mapFileName = null;
    	DO_CONNECT_TYPE tipe = null;
    	ArrayList<String> excludeList;
    	ArrayList<infaPair> mapList;
    	boolean isCustom = false;
    	connectInfo()
    	{
    		fromIdx = -1;
    		toIdx = -1;
    		tipe = DO_CONNECT_TYPE.UNKNOWN;
    		excludeList = new ArrayList<String>();
    		mapList     = new ArrayList<infaPair>();
    		isCustom = false;
    		mapFileName=null;
    	}
    }
    // SQL Override
    ArrayList<infaPair>SQLOverrideList = null;
    
    
	//----------------------------------------------------------------
	public generateMapping(pcDevBoosterSettings xi , String tmplt , String sSrcList , String sTgtList )
	//----------------------------------------------------------------
	{
	   xMSet = xi;
       xU = xMSet.xU;		
	   TemplateName = tmplt;   
	   sSourceList = sSrcList;
	   sTargetList = sTgtList;
	   xHints = new generatorMappingHints(); 
	   stManager = new generatorSrcTgtManager(xMSet);
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
	
	// multiple src and targets supported via comma delimited list
	//----------------------------------------------------------------
	private boolean make_SrcTgtList(String sSrcList , String sTgtList)
	//----------------------------------------------------------------
	{
		   //
		   if( sSrcList == null ) {
			   errit("Source list is null");
			   return false;
		   }
		   if( sTgtList == null ) {
			   errit("Target list is null");
			   return false;
		   }
		   if( sSrcList.trim().length() == 0 ) {
			   errit("Source list is empty");
			   return false;
		   }
		   if( sTgtList.trim().length() == 0 ) {
			   errit("Target list is empty");
			   return false;
		   }
		   stManager.initializeLists(sSrcList, sTgtList);
		   //
		   return true;
	}
	
	//----------------------------------------------------------------
	public boolean generate()
	//----------------------------------------------------------------
	{
		logit(1,"===============================================");
		long startt = System.currentTimeMillis();
		boolean ib = do_steps();
		long elapsed = System.currentTimeMillis() - startt;
	    logit(1, xMSet.getApplicationId() + " [Lines read=" + linesRead + "] [Lines written=" + linesWritten + "] [Elapsed time=" + (elapsed) + " msec] [Succes=" + ib + "]");
		logit(1,"===============================================");
	    return ib;
	}
	
	//----------------------------------------------------------------
	private boolean do_steps()
	//----------------------------------------------------------------
	{
		// check environment
		String sTempDir = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Temp";
		if( xMSet.xU.IsDir(sTempDir) == false ) {
	        errit( "Cannot access Temp folder for [" + OutputTempFileName + "] y");
	        return false;
		}
		OutputTempFileName = sTempDir + xMSet.xU.ctSlash + "temp_source.txt";
		if( xMSet.xU.IsBestand(OutputTempFileName) == true ) {
			xMSet.xU.VerwijderBestand(OutputTempFileName);
		}
		//
		boolean isOK = make_SrcTgtList( sSourceList , sTargetList );
		if( isOK == false ) return false;
		//
		isOK = firstPass();
		if( isOK == false ) return false;
		//
		isOK = stManager.load_SourceAndTargetSpecs();
		if( isOK == false ) return false;
		if( xHints.syntaxVersion == 1 )
		{
			//
			isOK = load_SourceQualifiers();
			if( isOK == false ) return false;
			//
			instanceList = null;
			//
			isOK = merge_template();
			if( isOK == false ) return false;
		}
		else 
		if( xHints.syntaxVersion == 2 ) {
			// KB 28 AUG
			if( SQLOverrideList != null ) {
				  if( stManager.setSQLOverride(SQLOverrideList) == false ) return false;
			}
			//
			generatorMappingMark2 mpt2 = new generatorMappingMark2( xMSet , stManager );
			mpt2.setOutputTempFileName(OutputTempFileName);
			mpt2.setTemplateName(TemplateName);
			mpt2.setHints(xHints);
			//
			isOK = mpt2.parseTemplate();
			linesWritten = mpt2.getLinesWritten();
			linesRead = mpt2.getLinesRead();
		}
		
		// post generator
		isOK = check_generated_file();
		if( isOK == false ) return false;
		// move
		isOK = moveTempToExport();
		if( isOK == false ) return false;
		//
		return true;
	}

	//----------------------------------------------------------------
	private void prnt(String sIn)
	//----------------------------------------------------------------
	{
		if( tempout == null ) {
			errit("Temp outputfile is not open " );
		}
		tempout.println(sIn);
		linesWritten++;
	}
	
	//----------------------------------------------------------------
	private boolean moveTempToExport()
	//----------------------------------------------------------------
	{
		String sDestDir = xMSet.getProjectDir() + xU.ctSlash + "Export";
		if( xU.IsDir(sDestDir) == false ) {
			errit("Cannot access folder [" + sDestDir + "]");
			return false;
		}
		String FileNaam = xHints.mappingName;
		ArrayList<String> list = xU.GetFilesInDir(sDestDir, null);
		int counter=0;
		for(int i=0;i<list.size();i++)
		{
			if( list.get(i).toLowerCase().startsWith( FileNaam.toLowerCase() ) ) {
				counter++;
			}
		}
	    //
		String FDestFileNaam = xMSet.getProjectDir() + xU.ctSlash + "Export" + xU.ctSlash + FileNaam + ".xml";
		String FNextFileNaam = xMSet.getProjectDir() + xU.ctSlash + "Export" + xU.ctSlash + 
			       FileNaam + "-" + String.format("%04d", counter) + ".xml";
		try {
		 if( xU.IsBestand(FDestFileNaam) ) {
			 xU.copyFile( FDestFileNaam , FNextFileNaam );
			 logit(5,"Backup created [" + FNextFileNaam + "]");
		 }
		}
		catch(Exception e) {
			errit("Error while moving [" + FDestFileNaam + "] to [" + FNextFileNaam +"] " + xU.LogStackTrace(e));
			return false;
		}
		try {
			xU.copyFile( OutputTempFileName , FDestFileNaam );
		}
		catch(Exception e) {
			errit("Error while moving [" + OutputTempFileName+ "] to [" + FDestFileNaam +"] " + xU.LogStackTrace(e));
			return false;
		}
		logit(5,"Generated code available in [" + FDestFileNaam +"]");
		return true;
	}
	
	//----------------------------------------------------------------
	private void changeToFlatFile( char tpe , int changeCounter)
	//----------------------------------------------------------------
	{
		stManager.changeToFlatFile(tpe, changeCounter);
	}
	
	//----------------------------------------------------------------
	private boolean firstPass()
	//----------------------------------------------------------------
	{
		if( xU.IsBestand(TemplateName) == false ) {
			   errit("Cannot find template [" + TemplateName + "]");
			   return false;
		}
		// perform a few basic checks on the template
		// read through template and determine whether source ad/or target are flatfiles
		String ENCODING = xU.getEncoding(TemplateName);
		String sHints = "";
		String ShortImportFileName=null;
		int detectedSyntaxVersion=-1;
		try {
		  BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(TemplateName),ENCODING));
       	  String sLijn = null;
       	  boolean inSource=false;
       	  boolean inTarget=false;
       	  int sourceCounter=0;
       	  int targetCounter=0;
       	  boolean isFlatFile=false;
       	  boolean expectEndSource=false;
       	  boolean expectEndTarget=false;
       	  boolean inHints=false;
       	  while ((sLijn=reader.readLine()) != null) {
       		  sLijn = sLijn.trim();
       		  String sClean = sLijn.trim();
       		  if( sClean.startsWith("<!--")) continue;
       		  if( sClean.startsWith("--")) continue;
       		  if( sClean.startsWith("//")) continue;
       		  //
       		  String sCompress = xU.dedup_spaces(sClean);
       		  if( sCompress.toUpperCase().startsWith("DO FOREACH") == true ) {
       			  if( (detectedSyntaxVersion != -1) && (detectedSyntaxVersion != 1)  ) {
       				  errit("(1) Mix of V1 and V2 syntax constructs");
       				  reader.close();
       				  return false;
       			  }
       			  detectedSyntaxVersion=1;
       		  }
       		  if( sCompress.toUpperCase().startsWith("IMPORT") == true ) {
       			if( (detectedSyntaxVersion != -1) &&  (detectedSyntaxVersion != 2) ) {
     				  errit("(2) Mix of V1 and V2 syntax constructs");
     				  reader.close();
     				  return false;
     			  }
       			  detectedSyntaxVersion=2;
       			  // 
       			  ShortImportFileName = sCompress.trim().substring("IMPORT".length()).trim();
       			  //
      		  }
     		  //
       		  if( sLijn.toUpperCase().indexOf("<HINTS>") >= 0 ) {
       			  inHints = true;
       			  sHints = "";
       		  }
       		  if( inHints ) {
       			sHints += sLijn;
       			if( sLijn.toUpperCase().indexOf("</HINTS>") >= 0 ) {
       			  inHints = false;	
       			}
       			continue;
       		  }
       		  //
       		  if( sLijn.indexOf("<SOURCE ")>=0) {
       			  inSource=true;
       			  isFlatFile=false;
       			  expectEndSource=true;
       			  continue;
       		  }
       		  if( inSource ) {
       			if( sLijn.indexOf("<FLATFILE ")>=0) {  // verander naar flatfile
       				isFlatFile=true;
       			}
       		    if( sLijn.indexOf("</SOURCE>")>=0) {
       			  if( isFlatFile ) changeToFlatFile( 'S' , sourceCounter );
     			  inSource=false;
     			  isFlatFile=false;
     			  expectEndSource=false;
     			  sourceCounter++;
     			  continue;
     		    }
       		  }
       		  //
       		  if( sLijn.indexOf("<TARGET ")>=0) {
     			  inTarget=true;
     			  isFlatFile=false;
     			  expectEndTarget=true;
     			  continue;
     		  }
     		  if( inTarget ) {
     			if( sLijn.indexOf("<FLATFILE ")>=0) {  // verander naar flatfile
     				isFlatFile=true;
     			}
     		    if( sLijn.indexOf("</TARGET>")>=0) {
     			  if( isFlatFile ) changeToFlatFile( 'T' , targetCounter );
   			      inTarget=false;
   			      isFlatFile=false;
   			      targetCounter++;
   			      expectEndTarget=false;
   			     continue;
   		        }
     		  }
          }
       	  reader.close();
          if( expectEndSource ) {
        	  errit(" </SOURCE> tag is missing in [" + TemplateName + "]");
        	  return false;
          }
          if( expectEndTarget ) {
        	  errit(" </TARGET> tag is missing in [" + TemplateName + "]");
        	  return false;
          }
          }
	     catch( Exception e ) {
	    	 errit("Error reading template [" + TemplateName + "] " + xU.LogStackTrace(e) );
	    	 return false;
	     }
		//
		boolean ib = parseHints(sHints);
		if( ib == false ) return false;
		xMSet.setUsenationalCharacterSet( xHints.useNationalCharacterSet );
		//
		if( detectedSyntaxVersion > 0 ) {
			if( xHints.syntaxVersion < 0 ) xHints.syntaxVersion = detectedSyntaxVersion;
			if( detectedSyntaxVersion != xHints.syntaxVersion ) {
				errit("Syntax detected is [" + detectedSyntaxVersion + "] but is hinted to be [" + xHints.syntaxVersion + "]");
				return false;
			}
		}
		else {
			if( xHints.syntaxVersion < 0 ) {
				logit(1,"Syntax version cannot be detected neither has it been specified. Switching to V2");
				xHints.syntaxVersion = 2;
			}
		}
		//
		if( xHints.NbrOfSources != getNbrOfSources() ) {
			errit("Number of sources in template differs from number specified in hints");
			return false;
		}
		if( xHints.NbrOfTargets != getNbrOfTargets() ) {
			errit("Number of targets in template differs from number specified in hints");
			return false;
		}
		logit(1,"Syntax version [" + xHints.syntaxVersion + "]");
		//  
		// Read map file
		if( xHints.syntaxVersion > 1 ) 
		{
		  if( ShortImportFileName != null ) {
			  generatorMapSpecReader rdr = new generatorMapSpecReader(xMSet);
			  ArrayList<generatorMapping> maptest = rdr.ReadTheMapFile(ShortImportFileName, sSourceList , sTargetList);
			  if( maptest == null ) return false;
			  logit(5,"MAP instructions [" + ShortImportFileName + "] successfully completed first pass");
			  //
			  ArrayList<String> flatfile_list = rdr.getFlatFileList();
			  if( flatfile_list == null ) return false;
			  if( flatfile_list.size() > 0 ) {
				  if( stManager.setFilesToFlatFile(flatfile_list) == false ) return false; 
			  }
			  // KB 26 AUG
			  String newMappingName = rdr.getOverruleMappingName();
			  if( newMappingName != null ) {
				  logit(5,"Mappingname [" + xHints.mappingName + "] overrule by [" + newMappingName + "]");
				  xHints.mappingName = newMappingName;
			  }
			  // KB 28 AUG
			  SQLOverrideList = rdr.getSQLOverrideStatements();
			  // KB 09 SEP
			  stManager.optionList = rdr.getOptionList();
		  }
		}
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean parseHints(String sIn)
	//----------------------------------------------------------------
	{
		String sRet = xU.removeBelowSpaces(sIn.trim());
		sRet = this.perform_substitute(sIn);
		//
		xHints.mappingName      = xU.extractXMLValue( sRet , "MappingNameMask" ).toLowerCase();
		xHints.mappingDesc      = xU.extractXMLValue( sRet , "MappingDescription" );
		xHints.NbrOfSources     = xU.NaarInt(xU.extractXMLValue( sRet , "NumberOfSources" ));
		xHints.NbrOfTargets     = xU.NaarInt(xU.extractXMLValue( sRet , "NumberOfTargets" ));
		xHints.syntaxVersion           = xU.NaarInt(xU.extractXMLValue( sRet , "SyntaxVersion" ));
		xHints.useNationalCharacterSet = xU.ValueInBooleanValuePair( "switch=" + xU.extractXMLValue( sRet , "UseNationalCharacterSet" ));
		
		//
		boolean isOK = true;
		if( xHints.mappingName == null ) {
			errit("Please provide MappingNameMask");
			isOK = false;
		}
		if( xHints.mappingDesc == null ) {
			errit("Please provide Mapping Description");
			isOK = false;
		}
		if( (xHints.NbrOfSources < 0) || (xHints.NbrOfSources > 3)  ) {
			errit("Please provide correct number of sources [" + xHints.NbrOfSources + "]");
			isOK = false;
		}
		if( (xHints.NbrOfTargets < 0) || (xHints.NbrOfTargets > 3) ) {
			errit("Please provide correct number of targets [" + xHints.NbrOfTargets + "]");
			isOK = false;
		}
		if( xHints.syntaxVersion != -1 ) {
		 if( (xHints.syntaxVersion < 1) || (xHints.syntaxVersion > 2) ) {
			errit("Please provide correct syntax version [" + xHints.syntaxVersion + "] is not supported");
			isOK = false;
		 }
		}
		logit(5,"Generating -> " + xHints.mappingName );
		return isOK;
	}
	
	
	
	//----------------------------------------------------------------
	private boolean load_SourceQualifiers()
	//----------------------------------------------------------------
	{
		sqList = new ArrayList<infaSourceQualifier>();
		for(int i=0;i<this.getNbrOfSources();i++)
		{
			infaSource tabWork = getInfaSource( i );
			if( tabWork == null ) return false;
			//
			infaSourceQualifier sq = new infaSourceQualifier( xMSet, tabWork );
			// check
			if( (sq.sqFieldList.size() < 1) || (sq.isValid == false)  ) {
				errit("No fields on SQ [" + tabWork.Name + "] or SQ could not be created");
				return false;
			}
			logit( 1 , "Created SQ [" + sq.Name + "] for [" + sq.Dbdname + "." + sq.TableName + "]");
			sqList.add( sq );
		}
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean check_generated_file()
	//----------------------------------------------------------------
	{
	    // er mogen geen %x% meer voorkomen
	    // TODO de tags moeten matchen
		boolean isOK=true;
		if( xU.IsBestand(OutputTempFileName) == false ) {
			errit("Cannot find [" + OutputTempFileName + "]");
			return false;
		}
		String ENCODING = xU.getEncoding(OutputTempFileName);
		if( ENCODING.compareToIgnoreCase("UTF-8") != 0 ) {
			logit(1,"WARNING : Not a UTF-8 file [" + OutputTempFileName + "] but [" + ENCODING + "]");
		}
		String sLijn=null;
		int tagCounter=0;
		int minusCounter=0;
	    int trailingSpaces=0;
		try {
			  BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(OutputTempFileName),ENCODING));
			  while ((sLijn=reader.readLine()) != null) {
	        	  if( sLijn.indexOf("\"%") >= 0 ) tagCounter++;
	        	  if( sLijn.indexOf("\"-1\"") >= 0 ) minusCounter++;
	        	  
	        	  // spaces before double quote
	        	  char[] bfr = sLijn.toCharArray();
	        	  char cprev=(char)0x00;
	        	  boolean inQuote = false;
				  for(int i=0;i<bfr.length;i++) 
				  {	
					 if( bfr[i] == (char)'"' )
					 {
						 inQuote = !inQuote;
						 if( inQuote == false ) // end
						 {
							 if( cprev == (char)0x20 ) 
							{ 
						 	    trailingSpaces++;
							    errit("TRAILING SPACE [loc=" + i + "] "+ sLijn );
							}
						 }
					 }
					 if( inQuote ) 
				     {
						cprev = bfr[i];
					 }
					 else cprev = (char)0x00;
			     }		
			  }	 
			  reader.close();
		}
		catch( Exception e) {
		   errit("Error reading [" + OutputTempFileName + "] " + xU.LogStackTrace(e) );
		   return false;
		}
		if( tagCounter > 0 ) {
			errit("There are (" + tagCounter + ") unsubstituted tags in the generated file [" + OutputTempFileName + "]");
			isOK = false;
		}
		if( minusCounter > 0 ) {
			errit("There are (" + minusCounter + ") \"-1\" occurences [" + OutputTempFileName + "]");
			isOK = false;
		}
		if( trailingSpaces > 0 ) {
			errit("There are (" + trailingSpaces + ") spaces trailing before the closing double quote");
			isOK = false;
		}
		
        return isOK;		
	}
	
	
	//----------------------------------------------------------------
	private PROGPOSITION checkNextPosition( PROGPOSITION current , PROGPOSITION tag )
	//----------------------------------------------------------------
	{
	  switch( tag )
	  {
	  case POWERMART  : { if ( current == PROGPOSITION.ROOT ) return tag; break; }
	  case REPOSITORY : { if ( current == PROGPOSITION.POWERMART ) return tag; break; }
	  case FOLDER : { if ( current == PROGPOSITION.REPOSITORY ) return tag; break; }
	  case SOURCE : { if ( current == PROGPOSITION.FOLDER ) return tag; break; }
	  case TARGET : { if ( current == PROGPOSITION.FOLDER ) return tag; break; }
	  case MAPPING: { if ( current == PROGPOSITION.FOLDER ) return tag; break; }
	  case TRANSFORMATION: { if ( current == PROGPOSITION.MAPPING ) return tag; break; }
	  default : { errit("System error checkNextPosition " + tag + " unknown position" ); break; }
	  }
	  errit("Got new tag <" + tag + "> however prog status is [" + current +"]. Cannot move");
	  return null;
	}
	
	//----------------------------------------------------------------
	private PROGPOSITION shiftDown( PROGPOSITION current , PROGPOSITION endtag )
	//----------------------------------------------------------------
	{
	  if( endtag != current ) {
		  errit("Got endtag </" + endtag + "> however prog status is [" + current +"]. Cannot move");
		  return null;
	  }
	  switch( current )
	  {
	  case TRANSFORMATION : return PROGPOSITION.MAPPING;
	  case MAPPING : return PROGPOSITION.FOLDER;
	  case TARGET : return PROGPOSITION.FOLDER;
	  case SOURCE : return PROGPOSITION.FOLDER;
	  case FOLDER : return PROGPOSITION.REPOSITORY;
	  case REPOSITORY : return PROGPOSITION.POWERMART;
	  case POWERMART : return PROGPOSITION.ROOT;
	  default : { errit("System error shiftdown " + endtag + " unknown position" ); break; }
	  }
	  return null;
	}
	
	
	//----------------------------------------------------------------
	private int fetchCodeIsValue( String sIn , String sPin )
	//----------------------------------------------------------------
	{
		String sRet = xU.removeBelowIncludingSpaces(sIn.toUpperCase().trim());
		String sPattern = sPin.toUpperCase();
		int start = sRet.indexOf(sPattern);
		if ( start >= 0) {
			sRet = sRet.substring(start + sPattern.length() );
			int stop = sRet.indexOf("%\"");
			if( start >= 0 ) {
			  sRet = sRet.substring(0,stop);
			  if( sRet.length() == 0) sRet = "1";
			  int i = (xU.NaarInt( sRet ))-1;
		      if( i < 0 ) {
		    	  errit("Invalid "+ sPattern + "{00..99}% in " + sIn);
		    	  return -1;
		      }
			  return i;
			}
		}
		errit("No pattern [" + sPattern + "{00..99}%\"] found in " + sIn);
		return -1;
	}
	
	//----------------------------------------------------------------
	private int fetchSourceTargetIdx( String sIn)
	//----------------------------------------------------------------
	{
		return fetchCodeIsValue( sIn , "name=\"%name" );
	}
	
	//----------------------------------------------------------------
	private boolean merge_template()
	//----------------------------------------------------------------
	{
		sourceTracker=targetTracker=0;
		String ENCODING = xU.getEncoding(TemplateName);
		boolean inForeach=false;
		String sFOREACH="";
		String sCONNECT="";
		String sInstance=null;
		boolean inInstance=false;
		boolean inConnect=false;
		boolean isOK=true;
		linesRead = 0;
		boolean inPayLoad=false;
		try {
			  BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(TemplateName),ENCODING));
			  //
			  tempout = new gpPrintStream(OutputTempFileName, "UTF-8");
		      //	  
	       	  String sLijn = null;
	       	  while ((sLijn=reader.readLine()) != null) {
	       		 linesRead++;
	       		 //
		         if( sLijn.trim().length() <= 0) continue;
		         if( sLijn.trim().startsWith("<!--")) continue;
		         if( sLijn.trim().startsWith("--")) continue;
		         if( sLijn.trim().startsWith("//")) continue;
		         //
		         if( sLijn.trim().startsWith("<!DOCTYPE POWERMART SYSTEM")) { // we are in business
		        	 inPayLoad = true;
		        	 prnt("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		         }
		         if( !inPayLoad ) continue;
		         // 
		         String sClean = sLijn.toUpperCase().trim();
		         // upshiften
		         if( sClean.startsWith("<POWERMART ") )  {
		        	 if ( (progpos = checkNextPosition( progpos , PROGPOSITION.POWERMART )) == null ) break;
		         }
		         if( sClean.startsWith("<REPOSITORY "))  {
		        	 if ( (progpos = checkNextPosition( progpos , PROGPOSITION.REPOSITORY)) == null ) break;
		         }
		         if( sClean.startsWith("<FOLDER "))  {
		        	 if ( (progpos = checkNextPosition( progpos , PROGPOSITION.FOLDER )) == null ) break;
		         }
		         if( sClean.startsWith("<SOURCE "))  {
		        	 if ( (progpos = checkNextPosition( progpos , PROGPOSITION.SOURCE )) == null ) break;
		        	 // <SOURCE ..  NAME = %name%  or %name{01..99}%
		        	 int idx = fetchSourceTargetIdx( sClean );
		        	 // test
		        	 if( getInfaSource( idx ) == null ) {
		        		 errit( "Cannot find SourceName [" + idx + "] in" + sLijn);
		        		 break;
		        	 }
		        	 sourceTracker = idx;
		         }
		         if( sClean.startsWith("<TARGET "))  {
		        	 if ( (progpos = checkNextPosition( progpos , PROGPOSITION.TARGET )) == null ) break;
		         	 // <TARGET ..  NAME = %name%  or %name{01..99}%
		        	 int idx = fetchSourceTargetIdx( sClean );
		        	 // test
		        	 if( getInfaTarget( idx ) == null ) {
		        		 errit( "Cannot find TargetName [" + idx + "] in " + sLijn);
		        		 break;
		        	 }
		        	 targetTracker = idx;
		         }
		         if( sClean.startsWith("<MAPPING "))  {
		        	 if ( (progpos = checkNextPosition( progpos , PROGPOSITION.MAPPING )) == null ) break;
		         }
		         if( sClean.startsWith("<TRANSFORMATION "))  {
		        	 if ( (progpos = checkNextPosition( progpos , PROGPOSITION.TRANSFORMATION )) == null ) break;
		        	 isOK = do_transformation( sLijn );
		        	 if( isOK == false ) break;
		        	 continue;  
		         }
		         // down shiften
		         //
		         if( sClean.startsWith("</TRANSFORMATION>") ) {
		        	 if( (progpos = shiftDown( progpos , PROGPOSITION.TRANSFORMATION )) == null ) break;
		         }
		         if( sClean.startsWith("</MAPPING>") ) {
		        	 if( (progpos = shiftDown( progpos , PROGPOSITION.MAPPING )) == null ) break;
		         }
		         if( sClean.toUpperCase().trim().startsWith("</TARGET>") ) {
		        	 if( (progpos = shiftDown( progpos , PROGPOSITION.TARGET )) == null ) break;
		         }
		         if( sClean.toUpperCase().trim().startsWith("</SOURCE>") ) {
		        	 if( (progpos = shiftDown( progpos , PROGPOSITION.SOURCE )) == null ) break;
		         }
		         if( sClean.toUpperCase().trim().startsWith("</FOLDER>") ) {
		        	 if( (progpos = shiftDown( progpos , PROGPOSITION.FOLDER )) == null ) break;		
		          }
		         if( sClean.toUpperCase().trim().startsWith("</REPOSITORY>") ) {
		        	 if( (progpos = shiftDown( progpos , PROGPOSITION.REPOSITORY )) == null ) break;
		         }
		         if( sClean.toUpperCase().trim().startsWith("</POWERMART>") ) {
		        	 if( (progpos = shiftDown( progpos , PROGPOSITION.POWERMART )) == null ) break;
		         }
		         // 
		         //
		         // FOR EACH block
		         String sComp = xU.HouLettersEnCijfers(sLijn).trim().toUpperCase();  // compressed
		         if( sComp.startsWith("DOFOREACH") == true ) {
		         	 inForeach = true;
		        	 sFOREACH = null;
		        	 continue;
		         }
                 if( sComp.startsWith("ENDFOREACH") == true ) {
                 	 if( inForeach == false ) {
                		errit("Line " + linesRead + " END FOREACH withhout preceding FOR EACH");
                		isOK=false;
                		break;
                	 }
                	 boolean ib = do_foreach( sFOREACH );
                	 if( ib == false ) return false;
		        	 inForeach = false;
		        	 sFOREACH=null;
		        	 continue;
		         }
                 if( inForeach ) {
                	 if( sFOREACH == null ) sFOREACH = sLijn;  else sFOREACH += CRLF + sLijn; 
                	 continue;
                 }
		         // DO CONNECT
                 if( sComp.startsWith("DOCONNECT") == true ) {
		         	 inConnect = true;
		        	 sCONNECT = null;
		        	 continue;
		         }
                 if( sComp.startsWith("ENDCONNECT") == true ) {
                 	 if( inConnect == false ) {
                		errit("Line " + linesRead + " END CONNECT withhout preceding DO CONNECT");
                		isOK=false;
                		break;
                	 }
                	 boolean ib = do_connect( sCONNECT );
                	 if( ib == false ) return false;
		        	 inConnect = false;
		        	 sCONNECT=null;
		        	 continue;
		         }
                 if( inConnect ) {
                	 if( sCONNECT == null ) sCONNECT = sLijn; else sCONNECT += " " + sLijn;
                	 continue;
                 }
                 //  INSTANCE
                 if( sLijn.startsWith("<INSTANCE ") == true ) {
                	 if( sLijn.trim().endsWith("/>") ) {
                		 boolean ib = do_instance( sLijn );
                    	 if( ib == false ) return false;
                	 }
                	 else {
		         	  inInstance = true;
		        	  sInstance = sLijn;
                	 }
		        	 continue;
		         }
                 if( inInstance ) {
                	 sInstance += CRLF + sLijn;
                	 if( sLijn.trim().startsWith("</INSTANCE>") == true ) {
                     	 if( inInstance == false ) {
                    		errit("Line " + linesRead + " </INSTANCE> withhout preceding <INSTANCE");
                    		isOK=false;
                    		break;
                    	 }
                    	 boolean ib = do_instance( sInstance );
                    	 if( ib == false ) return false;
    		        	 inInstance = false;
    		        	 continue;
    		         }
                	 //sInstance += CRLF + sLijn;
                	 continue;
                 }
                
                 //
		         String sOut = perform_substitute(sLijn);
		         if( sOut == null ) {
		        	 tempout.close();
		        	 errit("Error occurred in substitute module");
		        	 return false;
		         }
		         else prnt( sOut );
	          }
	       	  reader.close();
	       	  tempout.close();
	       	  if( progpos != null ) {
	       		  if( progpos != PROGPOSITION.ROOT ) {
	       			  errit("Template did not close with status ROOT but " + progpos );
	       			  isOK=false;
	       		  }
	       	  }
	       	  else {
	       		  errit("Merge concluded with errors");
	       		  isOK = false;
	       	  }
		}
		catch ( Exception e) {
			errit("Error reading [" + TemplateName + "] during mapping " + xU.LogStackTrace(e));
			return false;
		}
		return isOK;
	}
	
	//----------------------------------------------------------------
	private infaSource getInfaSourceViaName( String sName , char tpe )
	//----------------------------------------------------------------
	{
		return stManager.getInfaSourceViaName(sName, tpe);
	}

	
	/*
	//----------------------------------------------------------------
	private int getNbrOfSourceTargets(char tpe)
	//----------------------------------------------------------------
	{
		int counter = 0;
		for(int i=0;i<SrcTgtList.size();i++)
		{
			if( tpe == 'S' ) {
				if( (SrcTgtList.get(i).tipe == readInfaXML.ParseType.SOURCETABLE) || (SrcTgtList.get(i).tipe == readInfaXML.ParseType.SOURCEFLATFILE) ) {
					counter++;
				}
			}
			else
			if( tpe == 'T' ) {
				if( (SrcTgtList.get(i).tipe == readInfaXML.ParseType.TARGETTABLE) || (SrcTgtList.get(i).tipe == readInfaXML.ParseType.TARGETFLATFILE) ) {
					counter++;
				}
			}
			else {
			 errit("Unsupported [tipe=" + tpe + "] in getSourceOrTarget");
			 return -1;
			}
		}
		return counter;
	}
	*/
	//----------------------------------------------------------------
	private int getNbrOfSources()
	//----------------------------------------------------------------
	{
	   //return getNbrOfSourceTargets('S');
		return stManager.getNbrOfSources();
	}
	//----------------------------------------------------------------
	private int getNbrOfTargets()
	//----------------------------------------------------------------
	{
	   //return getNbrOfSourceTargets('T');	
		return stManager.getNbrOfTargets();
	}
	/*
	//----------------------------------------------------------------
	private infaSource getSourceOrTarget(char tpe , int idx)
	//----------------------------------------------------------------
	{
		int counter=0;
		for(int i=0;i<SrcTgtList.size();i++)
		{
			if( tpe == 'S' ) {
				if( (SrcTgtList.get(i).tipe == readInfaXML.ParseType.SOURCETABLE) || (SrcTgtList.get(i).tipe == readInfaXML.ParseType.SOURCEFLATFILE) ) {
					if( counter == idx ) return SrcTgtList.get(i).tab_obj;
					counter++;
				}
			}
			else
			if( tpe == 'T' ) {
				if( (SrcTgtList.get(i).tipe == readInfaXML.ParseType.TARGETTABLE) || (SrcTgtList.get(i).tipe == readInfaXML.ParseType.TARGETFLATFILE) ) {
					if( counter == idx ) return SrcTgtList.get(i).tab_obj;
					counter++;
				}
			}
			else {
			 errit("Unsupported [tipe=" + tpe + "] in getSourceOrTarget");
			 return null;
			}
		}
		errit("Cannot find Source/Target object in list for [tipe=" + tpe + "] [idx=" + idx + "]");
		return null;	
		
	}
	*/
	//----------------------------------------------------------------
	private infaSource getInfaSource(int idx)
	//----------------------------------------------------------------
	{
		//return getSourceOrTarget( 'S' , idx );
		return stManager.getInfaSource(idx);
	}
	//----------------------------------------------------------------
	private infaSource getInfaTarget(int idx )
	//----------------------------------------------------------------
	{
		//return getSourceOrTarget( 'T' , idx );
		return stManager.getInfaTarget( idx );
	}
	
	//----------------------------------------------------------------
	private String perform_substitute(String sLijn)
	//----------------------------------------------------------------
	{
		int curIdx = -1;
		String sRet=sLijn;
		//
		sRet = do_replace(sRet , "%NOW%" , ""+xU.prntDateTime(System.currentTimeMillis(), "MM/dd/yyyy HH:mm:ss"));
		sRet = do_replace(sRet , "%OWNER%"       , xMSet.whoami() );
		sRet = do_replace(sRet , "%APPLICATION%" , xMSet.getApplicationId() );
		//
		sRet = do_replace(sRet , "%MAPPINGNAME%"             , xHints.mappingName );
		sRet = do_replace(sRet , "%MAPPINGDESCRIPTION%"      , xHints.mappingDesc );
		// 
		infaSource tabWork = null;
		if( progpos == PROGPOSITION.SOURCE ) {
		  tabWork = getInfaSource( sourceTracker );
		  if( tabWork == null ) return null;
		  curIdx = sourceTracker;
		}
		if( progpos == PROGPOSITION.TARGET ) {
		  tabWork = getInfaTarget( targetTracker );
		  if ( tabWork == null ) return null;
		  curIdx = targetTracker;
		}
	   	if( tabWork != null )   // applicable only when in SOURCE or TARGET mode
	   	{
	   		// %NAME% and %NAME01 and %NAME1%
	   		for(int i=0;i<3;i++)
	   		{
	   			String sNum = "";
	   			if( i == 1 ) sNum = String.format("%2d" , (curIdx+1) );
	   			if( i == 2 ) sNum = String.format("%02d", (curIdx+1) );
	   			sNum = sNum.trim();
	            //
	   		 	sRet = do_replace(sRet , "%BUSINESSNAME"  + sNum + "%" , tabWork.BusinessName );
	   			sRet = do_replace(sRet , "%NAME"          + sNum + "%" , tabWork.Name );
	   			sRet = do_replace(sRet , "%DESCRIPTION"   + sNum + "%" , tabWork.Description );
	   			sRet = do_replace(sRet , "%DBDNAME"       + sNum + "%" , tabWork.Dbdname );
	   			sRet = do_replace(sRet , "%DATABASETYPE"  + sNum + "%" , tabWork.Databasetype );
	   			sRet = do_replace(sRet , "%OWNERNAME"     + sNum + "%" , tabWork.OwnerName );
	   			sRet = do_replace(sRet , "%OBJECTVERSION" + sNum + "%" , tabWork.ObjectVersion );
	   			sRet = do_replace(sRet , "%VERSIONNUMBER" + sNum + "%" , tabWork.VersionNumber );
	   			sRet = do_replace(sRet , "%CONSTRAINT"    + sNum + "%" , tabWork.Constraint );
	   			sRet = do_replace(sRet , "%TABLEOPTIONS"  + sNum + "%" , tabWork.TableOptions );
	   			sRet = do_replace(sRet , "%TABLEOPTIONS"  + sNum + "%" , tabWork.TableOptions );
	   		    // flat file
	   			if( (tabWork.tipe == readInfaXML.ParseType.SOURCEFLATFILE) || (tabWork.tipe == readInfaXML.ParseType.TARGETFLATFILE) ) 
	   			{
	   			 sRet = do_replace(sRet , "%CODEPAGE"  + sNum + "%"       , tabWork.flafle.CodePage );
	   			 sRet = do_replace(sRet , "%CONSECDELIMITERSASONE"  + sNum + "%" , tabWork.flafle.Consecdelimiterasone );
	   			 sRet = do_replace(sRet , "%DELIMITED"  + sNum + "%"      , tabWork.flafle.Delimited );
	   			 sRet = do_replace(sRet , "%DELIMITERS" + sNum + "%"      , tabWork.flafle.Delimiters );
	   			 sRet = do_replace(sRet , "%ESCAPECHARACTER" + sNum + "%" , tabWork.flafle.EscapeCharacter );
	   			 sRet = do_replace(sRet , "%KEEPESCAPECHAR" + sNum + "%"  , tabWork.flafle.Keepescapechar );
	   			 sRet = do_replace(sRet , "%LINESEQUENTIAL" + sNum + "%"  , tabWork.flafle.LineSequential );
	   			 sRet = do_replace(sRet , "%MULTIDELIMITERSASAND" + sNum + "%" , tabWork.flafle.LineSequential );
	   			 sRet = do_replace(sRet , "%NULLCHARACTER" + sNum + "%"        , tabWork.flafle.Nullcharacter );
	   			 sRet = do_replace(sRet , "%NULLCHARTYPE" + sNum + "%"         , tabWork.flafle.NullCharType );
	   			 sRet = do_replace(sRet , "%PADBYTES" + sNum + "%"             , tabWork.flafle.Padbytes );
	   			 sRet = do_replace(sRet , "%QUOTECHARACTER" + sNum + "%"       , tabWork.flafle.QuoteCharacter );
	   			 sRet = do_replace(sRet , "%REPEATABLE" + sNum + "%"           , tabWork.flafle.Repeatable );
	   			 sRet = do_replace(sRet , "%ROWDELIMITER" + sNum + "%"         , tabWork.flafle.RowDelimiter );
	   			 sRet = do_replace(sRet , "%SKIPROWS" + sNum + "%"             , tabWork.flafle.Skiprows );
	   			 sRet = do_replace(sRet , "%SHIFTSENSITIVEDATA" + sNum + "%"   , tabWork.flafle.ShiftSensitiveData );
	   			 sRet = do_replace(sRet , "%STRIPTRAILINGBLANKS" + sNum + "%"  , tabWork.flafle.Striptrailingblanks );
	   			}
	   		}
	    }
	   	//
   		int srcTeller=0;
		int tgtTeller=0;
		int nlist = stManager.getNumberOfListItems();
		for(int i=0;i<nlist;i++)
		{
		 generatorSrcTgt SrcTgt = stManager.getSrcTgtViaIdx(i);
		 if( SrcTgt == null ) {
			 errit("Error fetch SourceTarget");
			 return null;
		 }
		 // tab_obj might not have been initialized yet, e.g. parse_hints, tab_obj might be null and cause exceptions
		 String sDBName ="";
		 if( SrcTgt.tab_obj != null ) {
			 sDBName = SrcTgt.tab_obj.Dbdname.trim();
		 }	
		 if( (SrcTgt.tipe == readInfaXML.ParseType.SOURCETABLE) || (SrcTgt.tipe == readInfaXML.ParseType.SOURCEFLATFILE) )  {
				srcTeller++;
				String sLookFor = "";
				if( srcTeller == 1 ) {
					sLookFor = "%SOURCETABLENAME%";  // only for first source
					sRet = do_replace(sRet , sLookFor , SrcTgt.TableName ); 
					sLookFor = "%DBDNAME%";  // only for first source
					sRet = do_replace(sRet , sLookFor , sDBName ); 
			    }
				sLookFor = "%SOURCETABLENAME" + srcTeller + "%"; 
				sRet = do_replace(sRet , sLookFor , SrcTgt.TableName );
				sLookFor = "%DBDNAME" + srcTeller + "%"; 
				sRet = do_replace(sRet , sLookFor , sDBName );
				sLookFor = "%SOURCETABLENAME" + String.format("%02d", srcTeller ) + "%"; 
				sRet = do_replace(sRet , sLookFor , SrcTgt.TableName );
				sLookFor = "%DBDNAME" + String.format("%02d", srcTeller ) + "%"; 
				sRet = do_replace(sRet , sLookFor , sDBName );
		 }
		 else
		 if( (SrcTgt.tipe == readInfaXML.ParseType.TARGETTABLE) || (SrcTgt.tipe == readInfaXML.ParseType.TARGETFLATFILE) )  {
				tgtTeller++;
				String sLookFor = "";
				if( tgtTeller == 1 ) {
					sLookFor = "%TARGETTABLENAME%";  // only for first target
					sRet = do_replace(sRet , sLookFor , SrcTgt.TableName ); 
					sLookFor = "%DBDNAME%";  // only for first target
					sRet = do_replace(sRet , sLookFor , sDBName ); 
			    }
				sLookFor = "%TARGETTABLENAME" + tgtTeller + "%"; 
				sRet = do_replace(sRet , sLookFor , SrcTgt.TableName );
				sLookFor = "%DBDNAME" + tgtTeller + "%"; 
				sRet = do_replace(sRet , sLookFor , sDBName );
				sLookFor = "%TARGETTABLENAME" + String.format("%02d", tgtTeller ) + "%"; 
				sRet = do_replace(sRet , sLookFor , SrcTgt.TableName );
				sLookFor = "%DBDNAME" + String.format("%02d", tgtTeller ) + "%"; 
				sRet = do_replace(sRet , sLookFor , sDBName );
		 }
		 else {
			 errit("(perform substitute) System error");
			 return null;
		 }
		}
		// Source qualifier subsitutes
		if( sqList != null )
		{
		 int sqTeller=0;
		 for(int i=0;i<sqList.size();i++)
		 {
			sqTeller++;
			String sLookFor = "";
			if( sqTeller == 1 ) {
				sLookFor = "%SOURCEQUALIFIERNAME%";  // only for first SQ
				sRet = do_replace(sRet , sLookFor , sqList.get(i).Name ); 
		    }
			sLookFor = "%SOURCEQUALIFIERNAME" + sqTeller + "%"; 
			sRet = do_replace(sRet , sLookFor , sqList.get(i).Name );
			sLookFor = "%SOURCEQUALIFIERNAME" + String.format("%02d",  sqTeller ) + "%"; 
			sRet = do_replace(sRet , sLookFor , sqList.get(i).Name );
		 }
		}
		//
		return sRet;
	}
	
	//----------------------------------------------------------------
	private String do_replace(String sIn , String sTag , String sVal )
	//----------------------------------------------------------------
	{
		String sRet = sIn;
		if( sIn.toUpperCase().indexOf(sTag) < 0 ) return sRet;
		sRet = xU.RemplaceerIgnoreCase(sIn, sTag, sVal );
//System.err.println("SUBS [" + sTag + "] -> [" + sVal + "]");
		return sRet;
	}
	
	//----------------------------------------------------------------
	private boolean do_foreach( String sLijn )
	//----------------------------------------------------------------
	{
			infaSource x = null;
			if( progpos == PROGPOSITION.SOURCE ) {
				x = getInfaSource( sourceTracker );
				if( x == null ) return false;
				// Strange Source needs attributes first
				if ( x.tipe == readInfaXML.ParseType.SOURCEFLATFILE ) {
					do_flatfile_attributes( x );
				}
				if( do_source( sLijn , x ) == false ) return false;
			}
			else 
			if( progpos == PROGPOSITION.TARGET ) {
				x = getInfaTarget( targetTracker );
				if( x == null ) return false;
				if( do_source( sLijn , x) == false ) return false;
				// strange target needs attributes last
				if ( x.tipe == readInfaXML.ParseType.TARGETFLATFILE ) {
					do_flatfile_attributes( x );
				}
			}
			else {
	          errit("System error in do_foreach - unknown progpos " + progpos );
	          return false;
			}
			//			
			return true;
	}
	
	//   <TABLEATTRIBUTE NAME ="Base Table Name" VALUE =""/>
	//----------------------------------------------------------------
	private void do_flatfile_attributes( infaSource x)
	//----------------------------------------------------------------
	{
		for(int i=0 ; i< x.tableAttributeList.size() ; i++ )
		{
			if( x.tableAttributeList.get(i).value == null ) continue;
			prnt("   <TABLEATTRIBUTE NAME = \"" + 
		             x.tableAttributeList.get(i).code.trim() + "\" VALUE =\"" +
				     x.tableAttributeList.get(i).value + 
				     "\"/>" ); 
		}
	}
	
	//----------------------------------------------------------------
	private boolean do_source( String sLijn , infaSource x )
	//----------------------------------------------------------------
	{
		String sOriginal=sLijn;
		for(int i=0 ; i < x.fieldList.size(); i++)
		{
			String sNul = "NULL"; if( x.fieldList.get(i).mandatory ) sNul = "NOTNULL";
			int scale = x.fieldList.get(i).scale; if( scale < 0 ) scale = 0;
			int precision = x.fieldList.get(i).Precision; if( scale < 0 ) {
				errit("PRECISION is missing on [" + x.Name + "." + x.fieldList.get(i).Name );
				return false;
			}
			//
			String sRet=sOriginal;
			sRet = do_replace(sRet , "%BUSINESSNAME%"     , x.fieldList.get(i).BusinessName );
			sRet = do_replace(sRet , "%FIELDNAME%"        , x.fieldList.get(i).Name  );
			sRet = do_replace(sRet , "%NAME%"             , x.fieldList.get(i).Name  );
			sRet = do_replace(sRet , "%DATATYPE%"         , x.fieldList.get(i).DataType.toLowerCase().trim()  );
			sRet = do_replace(sRet , "%DESCRIPTION%"      , x.fieldList.get(i).Description );
			sRet = do_replace(sRet , "%FIELDDESCRIPTION%" , x.fieldList.get(i).Description );
			sRet = do_replace(sRet , "%FIELDNUMBER%"      , ""+x.fieldList.get(i).fieldNumber );
			sRet = do_replace(sRet , "%FIELDPROPERTY%"    , x.fieldList.get(i).FieldProperty );
			sRet = do_replace(sRet , "%FIELDTYPE%"        , x.fieldList.get(i).FieldType );
			sRet = do_replace(sRet , "%KEYTYPE%"          , x.fieldList.get(i).KeyType );
			sRet = do_replace(sRet , "%HIDDEN%"           , x.fieldList.get(i).Hidden );
			sRet = do_replace(sRet , "%NULLABLE%"         , sNul );
			sRet = do_replace(sRet , "%PRECISION%"        , ""+precision );
			sRet = do_replace(sRet , "%SCALE%"            , ""+scale );
			sRet = do_replace(sRet , "%LENGTH%"           , ""+x.fieldList.get(i).Length );
			sRet = do_replace(sRet , "%LEVEL%"            , ""+x.fieldList.get(i).Level );
			sRet = do_replace(sRet , "%OFFSET%"           , ""+x.fieldList.get(i).offset );
			sRet = do_replace(sRet , "%OCCURS%"           , ""+x.fieldList.get(i).Occurs );
			sRet = do_replace(sRet , "%PHYSICALLENGTH%"   , ""+x.fieldList.get(i).physicalLength );
			sRet = do_replace(sRet , "%PHYSICALOFFSET%"   , ""+x.fieldList.get(i).physicalOffset );
			sRet = do_replace(sRet , "%PICTURETEXT%"      , x.fieldList.get(i).PictureText );
			sRet = do_replace(sRet , "%USAGEFLAGS%"       , x.fieldList.get(i).UsageFlags );
			//
			prnt( sRet );
		}
		
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean do_transformation(String sIn)
	//----------------------------------------------------------------
	{
		String sComp = xU.HouLettersEnCijfers(sIn).toUpperCase().trim();
		if( sComp.indexOf("TYPESOURCEQUALIFIER") >= 0 ) {
			return do_sourceQualifier( sIn);
		}
		//
		errit("Unsupported transformation [" + sIn + " " + sComp);
		return false;
	}
	
	//----------------------------------------------------------------
	private boolean do_sourceQualifier(String sIn)
	//----------------------------------------------------------------
	{
		// in toekomst zullen er wellicht eerder SQ types nodig zijn bvb SQL overrride
		
		infaSourceQualifier sq = sqList.get(0);
		
		// substitute the TRANSFORMATION line 
		String sRet = sIn;
		sRet = do_replace ( sRet , "%DESCRIPTION%"        , sq.Description );
		sRet = do_replace ( sRet , "%DBDNAME%"            , sq.Dbdname );
		sRet = do_replace ( sRet , "%NAME%"               , sq.Name ); // name and sourqualifiername 
		sRet = do_replace ( sRet , "%SOURCEQUALIFIERNAME%", sq.Name ); // can be used 
		sRet = do_replace ( sRet , "%OBJECTVERSION%"      , sq.ObjectVersion );
		sRet = do_replace ( sRet , "%REUSABLE%"           , sq.Reusable );
		sRet = do_replace ( sRet , "%VERSIONNUMBER%"      , sq.VersionNumber );
		prnt( sRet );
		
		// de Source qual lines
		// <TRANSFORMFIELD DATATYPE ="string" DEFAULTVALUE ="" DESCRIPTION ="" NAME ="IBMSNAP_OPERATION" PICTURETEXT ="" PORTTYPE ="INPUT/OUTPUT" PRECISION ="1" SCALE ="0"/>
		for (int i=0;i<sq.sqFieldList.size();i++)
		{
		  infaSourceQualifierField fld =  sq.sqFieldList.get(i);
		  sRet = "";
		  sRet += " DATATYPE ="     + "\"" + fld.Datatype.toLowerCase().trim() + "\"";
		  sRet += " DEFAULTVALUE =" + "\"" +fld.DefaultValue + "\"";
		  sRet += " DESCRIPTION ="  + "\"" + fld.Description.trim() + "\"";
		  sRet += " NAME ="         + "\"" + fld.Name.trim() + "\"";
		  sRet += " PICTURETEXT ="  + "\"" + fld.PictureText.trim() + "\"";
		  sRet += " PORTTYPE ="     + "\"" + fld.Porttype + "\"";
		  sRet += " PRECISION ="    + "\"" + fld.Precision + "\"";
		  sRet += " SCALE ="        + "\"" + fld.Scale + "\"";
		  //
		  sRet = "<TRANSFORMFIELD" + sRet + "/>";
		  //
		  prnt( "     " + sRet );
		}
		//   <TABLEATTRIBUTE NAME ="Source Filter" VALUE =""/>
		// Table attributes
		for(int j=0 ; j<sq.AttrList.size() ; j++ )
		{
			sRet = "     <TABLEATTRIBUTE NAME =\"" + sq.AttrList.get(j).code + "\" VALUE =\"" +  sq.AttrList.get(j).value + "\"/>";
	        prnt(sRet);
		}
		
		return true;
	}

	//----------------------------------------------------------------
	private infaSourceQualifier getInfaSourceQualifierViaName( String sName )
	//----------------------------------------------------------------
	{
		for(int i=0;i<sqList.size();i++)
		{
			if( sqList.get(i).Name.trim().compareToIgnoreCase(sName.trim()) == 0 ) return sqList.get(i);
		}
		errit("Cannot find SQ with name [" + sName + "]");
		return null;
	}
	
	//----------------------------------------------------------------
	private boolean do_instance( String sIn)
	//----------------------------------------------------------------
	{
		//
		if (instanceList == null ) instanceList = new ArrayList<infaInstance>();
		// Start by the regular subsitute
		String sRet = this.perform_substitute(sIn);
		if( sRet == null ) {
			errit("(do_instance) Could not resolve all parameters in [" + sIn + "]");
			return false;
		}
		if( sRet.indexOf("\"%") >= 0 ) {
			errit("(do_instance) Could not resolve all parameters in [" + sIn + "]");
			return false;
		}
	    //
		prnt(sRet);
	    //
		// Use the substituted values to populate the infaInstance class
		infaInstance x = new infaInstance( xMSet , sRet );
		if( x.isValid == false ) return false;
	    // find the object
		if( x.Transformation_Type == infaInstance.TRANSFORMATION_TYPE.SOURCE_DEFINITION ) {
			x.obj = (infaSource)getInfaSourceViaName( x.Transformation_Name , 'S' );
			if( x.obj == null ) {
				errit("Cannot find source [" + x.Transformation_Name + "] in sources");
				return false;
			}
			infaSource z = (infaSource)x.obj;
			logit(5,"Instance [" + x.InstanceName + "] TBL--> [" + z.Name + "]");
		}
		else
		if( x.Transformation_Type == infaInstance.TRANSFORMATION_TYPE.TARGET_DEFINITION ) {
			x.obj = (infaSource)getInfaSourceViaName( x.Transformation_Name , 'T' );
			if( x.obj == null ) {
				errit("Cannot find target [" + x.Transformation_Name + "] in sources");
				return false;
			}
			infaSource z = (infaSource)x.obj;
			logit(5,"Instance [" + x.InstanceName + "] TBL--> [" + z.Name + "]");
		}
		else
		if( x.Transformation_Type == infaInstance.TRANSFORMATION_TYPE.SOURCE_QUALIFIER ) {
			x.obj = (infaSourceQualifier)getInfaSourceQualifierViaName( x.Transformation_Name );
			if( x.obj == null ) {
				errit("Cannot find SQ [" + x.Transformation_Name + "]");
				return false;
			}
			infaSourceQualifier z = (infaSourceQualifier)x.obj;
			logit(5,"Instance [" + x.InstanceName + "] SQ--> [" + z.Name + "] TBL--> [" + z.TableName + "]" );
		}
		else {
			errit("Unsupported transformation type [" + x.Transformation_Type + " for " + x.InstanceName);
			return false;
		}
		//
		instanceList.add(x);
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean do_connect( String sLijn )
	//----------------------------------------------------------------
	{
		connectInfo con = new connectInfo();
		//
		String sRet = xU.removeBelowSpaces(sLijn).trim();
		StringTokenizer st = new StringTokenizer(sRet, "= \n");
		while(st.hasMoreTokens()) 
		{ 
			  String sToken = st.nextToken().trim();
			  if( st.hasMoreTokens() == false ) {
				  errit("System error - do_connect() - no toke/value pairs in " + sLijn);
				  return false;
			  }
			  String sVal = st.nextToken().trim();
			  if( sToken.compareToIgnoreCase("FROM") == 0 )  {
				  int i = xU.NaarInt(sVal);
				  if( i>= 0 ) con.fromIdx = i-1;
			  }
			  else
			  if( sToken.compareToIgnoreCase("TO") == 0 )  {
				  int i = xU.NaarInt(sVal);
				  if( i>= 0 ) con.toIdx = i-1;		  
			  }
			  else 
			  if( sToken.compareToIgnoreCase("TYPE") == 0 )  {
				  if( sVal.compareToIgnoreCase("NAME") == 0 )  {
					  con.tipe = DO_CONNECT_TYPE.NAME;
				  }
				  else 
				  if( sVal.compareToIgnoreCase("NUMBER") == 0 )
				  {
					  con.tipe = DO_CONNECT_TYPE.RANK;
				  }
				  else  // IMPORT <file> left right   
				  if( sVal.compareToIgnoreCase("IMPORT") == 0 )
				  {
					  con.tipe = DO_CONNECT_TYPE.IMPORT;
					  if( st.hasMoreTokens() == false ) {
						  errit("System error - do_connect() - no file defined after iMPORT" + sLijn);
						  return false;
					  }
					  con.mapFileName = st.nextToken().trim();
				  }
				  else  {
					  errit("System error - do_connect() - unsupported TYPE [" + sVal  +"] in " + sLijn);
					  return false;
				  }
			  }
			  else
			  if( sToken.compareToIgnoreCase("EXCLUDE") == 0 )  {
				  con.excludeList.add(sVal);
				  con.isCustom = true;
			  }
			  else
			  if( sToken.compareToIgnoreCase("MAP") == 0 )  {
				  String sEen = sVal;
				  if( st.hasMoreTokens() == false ) {
					  errit("System error - do_connect() - no  (x to y) pair after MAP" + sLijn);
					  return false;
				  }
				  String sTwee = st.nextToken().trim().toUpperCase();
				  if( sTwee.compareToIgnoreCase("TO") != 0 ) {
					  errit("System error - do_connect() - Expect TO in  MAP x TO y" + sLijn);
					  return false;
				  }
				  if( st.hasMoreTokens() == false ) {
					  errit("System error - do_connect() - no value after MAP x TO" + sLijn);
					  return false;
				  }
				  sTwee = st.nextToken();
				  infaPair z = new infaPair(sEen , sTwee);
				  con.mapList.add(z);
				  con.isCustom = true;
			  }
			  else {
				  errit("Unsupported DO CONNECT option [" + sToken + "]");
				  return false;
			  }
		}
		if( (con.fromIdx<0) || (con.fromIdx>=instanceList.size()) ) {
			errit("FROM Index [" + con.fromIdx + "] is either to small or too large");
			return false;
		}
		if( (con.toIdx<0) || (con.toIdx>=instanceList.size()) ) {
			errit("TO Index [" + con.toIdx + "] is either to small or too large");
			return false;
		}
		if( con.toIdx == con.fromIdx ) {
			errit("TO Index is IDENTICAL TO FROM index");
			return false;
		}
		//
		boolean ib = do_connect_lines( con );
		if( ib == false ) return false;
		//
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean do_connect_lines( connectInfo con )
	//----------------------------------------------------------------
	{
		infaInstance from = instanceList.get( con.fromIdx );
		infaInstance to   = instanceList.get( con.toIdx );
		
		// FROM moet een SOURCE of een SQ zijn met iNPUT/OUTPUT
		// todo
		// TO moet een TARGT zijn of een S met INPUT output
		//todo
		
		// field name lists
		ArrayList<String> fromList = getFieldNameListFromInstance( from );
		ArrayList<String> toList = getFieldNameListFromInstance( to );
		if( fromList == null ) {
			errit("Bizar : an empty FROM FieldnameList");
			return false;
		}
		if( toList == null ) {
			errit("Bizar : an empty TO FieldnameList");
			return false;
		}
		// IMPORT
		if( con.tipe == DO_CONNECT_TYPE.IMPORT )  {
			return do_connect_import( con , from , to );
		}
		//
		if( con.isCustom == false ) {
		  if( con.tipe == DO_CONNECT_TYPE.NAME ) {
			boolean ib = doFieldNamesMatch( from , to );
			if( ib == false ) return false;
			// just copy the from over the to
			for(int i=0;i<fromList.size();i++) toList.set( i , fromList.get(i) );
		    //	
			return writeConnector( from , fromList , to , toList );
		  }
		  if( con.tipe == DO_CONNECT_TYPE.RANK  )  {
			if( fromList.size() != toList.size() ) {
				errit("Number of columns does not match between " + from.InstanceName + " and " + to.InstanceName);
				return false;
			}
			return writeConnector( from , fromList , to , toList );
		  }
		}
		else {
			
			// exclude from left hand side
			for(int i=0; i<con.excludeList.size();i++)
			{
			    boolean found = false;	
				for(int j=0;j<fromList.size();j++)
				{
				  if( fromList.get(j).trim().compareToIgnoreCase( con.excludeList.get(i).trim() ) == 0 ) {
					fromList.remove(j);
					found = true;
					break;
				  }
				}
				if( found == false ) {
					errit("(do_connect_line) Could not find item to exclude [" + con.excludeList.get(i) + "] on " + fromList);
					return false;
				}
			}
			// reconstruct right hand side
			if( con.tipe == DO_CONNECT_TYPE.NAME ) {
				// check names on the right hand side
				for(int i=0;i<fromList.size();i++) {
					String sFromFieldName = fromList.get(i);
					if( getFieldNameFromInstance( to , sFromFieldName ) == null ) {
						errit("Cannot find field [" + sFromFieldName + "] in TO ");
						return false;
					}
				}
				// rebuild right hand side
				toList = null;
				toList = new ArrayList<String>();
				for(int i=0;i<fromList.size();i++) {
					String st = fromList.get(i);
					toList.add( st );	
				}
			}
			else
			// heb toch wat vragen bij een RANK op een exclude wat indien 1 left naar 2 right gaat ??
			if( con.tipe == DO_CONNECT_TYPE.RANK ) {
				// right hand side smaller than lefthand side
				if( toList.size() < fromList.size() ) {
					errit("Connect error - EXCLUDE - left hand side has more fields than lefthand side");
					return false;
				}
				ArrayList<String> oldList = new ArrayList<String>();
				for(int i=0;i<oldList.size();i++) {
					String st = toList.get(i);
					oldList.add( st );	
				}
				toList = null;
				toList = new ArrayList<String>();
				for(int i=0;i<fromList.size();i++) {
					String st = oldList.get(i);
					toList.add( st );	
				}
			}
			else {
				errit("(do_connect_line) Bizar - unknown type [" + con.tipe + "]");
				return false;
			}
			// debug
			// manuele map instructies
			for(int i=0;i<con.mapList.size();i++)
			{
				// check the names in the mapping instruction MAP x to y
				String sLeft = getFieldNameFromInstance( from , con.mapList.get(i).code );
				if( sLeft == null ) return false;
				String sRight = getFieldNameFromInstance ( to , con.mapList.get(i).value );
				if( sRight == null ) return false;
				// 
				logit(5, "Valid mapping [" + sLeft + "] to [" + sRight + "]");
				// bingo
				fromList.add( sLeft );
				toList.add( sRight );
			}
			// debug
			for(int i=0 ; i< fromList.size(); i++) 
			{
			  logit(9,"CUSTOM MAP [" + fromList.get(i) + "] -> [" + toList.get(i) + "]");	
			}
			//
			return writeConnector( from , fromList , to , toList );
		}
		//
		errit("Not supported CONNECT TYPE " + con.tipe);
		return false;
	}

	// to ensure correct upper/lower case
	//----------------------------------------------------------------
	private String getFieldNameFromInstance( infaInstance inst , String sFieldName)
	//----------------------------------------------------------------
	{
		ArrayList<String> list = getFieldNameListFromInstance( inst );
		if( list ==  null ) {
			errit("System error - cannot fetch fieldnames");
			return null;
		}
		for(int i=0;i<list.size();i++)
		{
			if( list.get(i).trim().compareToIgnoreCase(sFieldName.trim()) == 0 ) return list.get(i);
		}
		errit("Cannot find field [" + sFieldName + "] in INSTANCE [" + inst.InstanceName  + "]");
		return null;
	}
	
	//----------------------------------------------------------------
	private boolean notLinkedTwice(ArrayList<String> list)
	//----------------------------------------------------------------
	{
        ArrayList<String> dupes = new ArrayList<String>();
        for(int i=0;i<list.size();i++)
        {
        	boolean found = false;
        	for(int j=0 ; j < dupes.size(); j++ )
        	{
        		if( list.get(i).trim().compareToIgnoreCase(dupes.get(j).trim()) == 0 ) {
        			found = true;
        			break;
        		}
        	}
        	if( found ) {
        		errit("Target column [" + list.get(i) + "] is linked twice !");
        		return false;
        	}
        	dupes.add( list.get(i) );
        }
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean writeConnector(infaInstance from , ArrayList<String> fromList , infaInstance to , ArrayList<String> toList)
	//----------------------------------------------------------------
	{
		// validate wether a right hand side field is linked more than once
		if ( notLinkedTwice ( toList ) == false ) return false; 
		//
		for(int i=0;i<fromList.size();i++)
		{
			String sRet = "";
			sRet = "<CONNECTOR" +
			       " FROMFIELD =\"" + fromList.get(i) + "\"" +
			       " FROMINSTANCE =\"" + from.InstanceName + "\"" +
			       " FROMINSTANCETYPE =\"" + TTFormat(from.Transformation_Type) + "\"" + // strange
			       " TOFIELD =\"" + toList.get(i) + "\"" +
			       " TOINSTANCE =\"" + to.InstanceName + "\"" +
			       " TOINSTANCETYPE =\"" + TTFormat(to.Transformation_Type) + "\"/>";
	        prnt( sRet );				
		}
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean doFieldNamesMatch( infaInstance from , infaInstance to)
	//----------------------------------------------------------------
	{
		ArrayList<String> fromList = getFieldNameListFromInstance( from );
		ArrayList<String> toList = getFieldNameListFromInstance( to );
	
		if( (fromList == null) || (toList == null) ) {
			errit("System error - cannot fetch instance col names");
			return false;
		}
		if( fromList.size() != toList.size() ) {
			errit("Number of columns does not match between " + from.InstanceName + " and " + to.InstanceName);
			return false;
		}
		for(int i=0;i<fromList.size();i++)
		{
			boolean found = false;
			for(int j=0;j<toList.size();j++)
			{
				if( fromList.get(i).compareToIgnoreCase(toList.get(j)) == 0) {
					found = true;
					break;
				}
			}
			if (found == false ) {
				errit("Cannot find match for " + from.InstanceName + "." + fromList.get(i));
				return false;
			}
		}
		//
		return true;
	}
	
	//----------------------------------------------------------------
	private ArrayList<String> getFieldNameListFromInstance(infaInstance inst)
	//----------------------------------------------------------------
	{
		ArrayList<String> colList = new ArrayList<String>();
		if( inst.Transformation_Type == infaInstance.TRANSFORMATION_TYPE.SOURCE_QUALIFIER ) {
		    infaSourceQualifier sq = (infaSourceQualifier)inst.obj;
			for(int i=0;i<sq.sqFieldList.size();i++)
			{
				String sField = sq.sqFieldList.get(i).Name;
				colList.add( sField );
			}
			return colList;
		}
        if( (inst.Transformation_Type == infaInstance.TRANSFORMATION_TYPE.SOURCE_DEFINITION) || 
        	(inst.Transformation_Type == infaInstance.TRANSFORMATION_TYPE.TARGET_DEFINITION) ) {
        	infaSource src = (infaSource)inst.obj;
			for(int i=0;i<src.fieldList.size();i++)
			{
				String sField = src.fieldList.get(i).Name;
				colList.add( sField );
			}
			return colList;
		}
        errit("Unsupported instance TRANSFORMATION TYPE " + inst.Transformation_Type);
		return null;
	}
	
	//----------------------------------------------------------------
	private String TTFormat(infaInstance.TRANSFORMATION_TYPE transTipe)
	//----------------------------------------------------------------
	{
		switch( transTipe )
		{
		case SOURCE_QUALIFIER   : return "Source Qualifier"; 
		case TARGET_DEFINITION  : return "Target Definition"; 
		case SOURCE_DEFINITION  : return "Source Definition"; 
		default :  {
		    errit("TTFormat unsupported tipe " + transTipe);
		    return "SHOWSTOPPER";
		 }
		}
	}
	
	//----------------------------------------------------------------
	private String getPhyiscalSrcTgtNameFromInstance(infaInstance inst)
	//----------------------------------------------------------------
	{
		if( inst.Associated_Source_Instance == null ) return inst.Transformation_Name;
		String sLookFor = inst.Associated_Source_Instance.trim().toUpperCase();
		logit(9,"Looking for associated source instance -> " + sLookFor );
		for(int i=0;i<instanceList.size();i++)
		{
			if( instanceList.get(i).InstanceName.compareToIgnoreCase(sLookFor) == 0 ) return instanceList.get(i).Transformation_Name.trim();
		}
		return null;
	}
	
	//----------------------------------------------------------------
	private boolean doesTextFeatureOnList( String sField , ArrayList<String> list)
	//----------------------------------------------------------------
	{
	    for(int i=0;i<list.size();i++)
	    {
	    	if( sField.compareToIgnoreCase( list.get(i)  ) == 0 ) return true;
	    }
		return false;
	}
	
	//----------------------------------------------------------------
	private boolean do_connect_import( connectInfo con , infaInstance from , infaInstance to)
	//----------------------------------------------------------------
	{
		// read the mapping specifications file
		String fromName = getPhyiscalSrcTgtNameFromInstance( from );
		if( fromName == null ) {
			errit("(do_connect_import) Cannot determine source table name for instance [" + from.InstanceName + "]");
			return false;
		}
		String toName = getPhyiscalSrcTgtNameFromInstance( to );
		if( toName == null ) {
			errit("(do_connect_import) Cannot determine target table name for instance [" + to.InstanceName + "]");
			return false;
		}
		//
		generatorMapSpecReader rdr = new generatorMapSpecReader(xMSet);
		ArrayList<generatorMapping> mapList = rdr.readSpecs(  con.mapFileName , fromName , toName );
		if( mapList == null) return false;
		if( mapList.size() == 0 ) {
			errit("There is nothing to map ?");
			return false;
		}
		
		// Debug 
		for(int i=0;i<mapList.size();i++)
		{
		  logit(5,"MAPPING Field [" + mapList.get(i).sourceField + "] to [" + mapList.get(i).targetField + "]");
		  ArrayList<generatorInstruction> ilist = mapList.get(i).instr_list;
		  for(int k=0;k<ilist.size();k++)
		  {
		  logit(5,"        Instr [tipe=" + ilist.get(k).tipe + "] [rval=" + ilist.get(k).rVal + "] [" + ilist.get(k).fromPortTipe + "] [" + ilist.get(k).toPortTipe + "]");
		  }
		}
		
		// read the to and from fieldnames
		ArrayList<String> fromList = getFieldNameListFromInstance( from );
		ArrayList<String> toList = getFieldNameListFromInstance( to );
		if( (fromList == null) || (toList == null) ) {
			errit("(do_connect_import) System error - cannot fetch instance col names");
			return false;
		}
		// check whether the fieldnames on the specs match the ones on from and to
		for(int i=0 ; i< mapList.size(); i++)
		{
			if( doesTextFeatureOnList( mapList.get(i).sourceField , fromList ) == false ) {
				errit("Source mapping field [" + mapList.get(i).sourceField + "] is not part of field on instance [" + from.InstanceName + "]");
				return false;
			}
			if( doesTextFeatureOnList( mapList.get(i).targetField , toList ) == false ) {
				errit("Target mapping field [" + mapList.get(i).targetField + "] is not part of field on instance [" + to.InstanceName + "]");
				return false;
			}
		}
		// OK -  got mapping specs that match the source and target fields
	    // just push the spces in 2 arraylists
		fromList = null;
		toList   = null;
		fromList = new ArrayList<String>();
		toList   = new ArrayList<String>();
		for(int i=0 ; i< mapList.size(); i++)
		{
			fromList.add( mapList.get(i).sourceField );
			toList.add( mapList.get(i).targetField );
		}
		//
		return writeConnector( from , fromList , to , toList );
	}
}
