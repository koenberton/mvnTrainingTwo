package pcGenerator.generator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import pcGenerator.ddl.rdbmsDatatype;
import pcGenerator.ddl.readInfaXML;
import pcGenerator.powercenter.infaConnector;
import pcGenerator.powercenter.infaDataType;
import pcGenerator.powercenter.infaDataTypeConvertor;
import pcGenerator.powercenter.infaInstance;
import pcGenerator.powercenter.infaPair;
import pcGenerator.powercenter.infaSource;
import pcGenerator.powercenter.infaSourceField;
import pcGenerator.powercenter.infaTransformation;
import pcGenerator.powercenter.infaTransformationField;
import generalpurpose.gpPrintStream;
import generalpurpose.pcDevBoosterSettings;

public class generatorMappingMark2 {

	// DO NOT MODIFY THESE RANGES
	private final int SRC_STAGE   = 10;
	private final int SQ_STAGE    = 30;
	private final int TRANS_STAGE = 100;
	private final int TGT_STAGE   = 990;
	private final int PKEY_STAGE  = 901;   
	private final int CRC_STAGE   = 902;   
	private final int QUAL_STAGE  = 903;
	private final int FKEY_STAGE  = 905;  
	
	pcDevBoosterSettings   xMSet = null;
	generatorConstants     xConst = null;
	generatorSrcTgtManager stManager = null;
	rdbmsDatatype          xRdbms = null;
	infaDataTypeConvertor  converter = null;
	generatorTrace         tracer = null;
	
	gpPrintStream tempout = null;
	
	generatorMappingHints xHints=null;
	int linesWritten = 0;
	int linesRead = 0;
	
	// copy from mapping Generator
	String TemplateName = null;
	String OutputTempFileName = null;
	//
	private ArrayList<generatorMapping> mapList = null;
	//
	private ArrayList<generatorMapping> network = null;
	//	
	private ArrayList<infaInstance> instList = null;
	// 
	private ArrayList<infaTransformation> trans_list=null;
	//
	private ArrayList<infaConnector> connector_list=null;
	
    // this is an ARRAY - Caution positions are predetermined 0 = KEY , 1 = CRC ,  2..11 = FKEY, 12=QUAL
	// the variable ndx is used to make a clear distinction when accessing these items
	private FieldCluster[] cluster_list = null;
	
	//----------------------------------------------------------------
	generatorMappingMark2(pcDevBoosterSettings im , generatorSrcTgtManager istm)
	//----------------------------------------------------------------
	{
			xMSet        = im;
			xConst       = new generatorConstants(xMSet);
			stManager    = istm;
			xRdbms       = new rdbmsDatatype(xMSet);
			converter    = new infaDataTypeConvertor(xMSet);
			cluster_list = new FieldCluster[13];
			for(int i=0;i<13;i++)   //  KEY + CRC + 10 times Foreign Keys + QUAL
			{
				FieldCluster x = null;
				cluster_list[i] = x;
			}
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
	//----------------------------------------------------------------
	public void setTemplateName(String sIn)
	{
		TemplateName = sIn;
	}
	public void setOutputTempFileName(String sIn)
	{
		OutputTempFileName = sIn;
	}
	public void setHints(generatorMappingHints xi)
	{
		xHints = xi;
	}
	public int getLinesWritten()
	{
		return linesWritten;
	}
	public int getLinesRead()
	{
		return linesRead;
	}
	
	//----------------------------------------------------------------
	//----------------------------------------------------------------
	
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
	private infaSource localgetInfaSource(String s , int idx )
	//----------------------------------------------------------------
	{
	  infaSource x = stManager.getInfaSource(idx);
	  if( x != null ) return x;
	  errit("getInfaSource error occurred in module [" + s + "]");
	  return null;
	}
	
	//----------------------------------------------------------------
	private infaSource localgetInfaTarget(String s , int idx )
	//----------------------------------------------------------------
	{
		  infaSource x = stManager.getInfaTarget(idx);
		  if( x != null ) return x;
		  errit("getInfaTarget error occurred in module [" + s + "]");
		  return null;
	}
	
	//----------------------------------------------------------------
	public boolean parseTemplate()
	//----------------------------------------------------------------
	{
		 boolean isOK=true;
		 // safety
		 if( xHints == null ) isOK= false;
		 if( TemplateName == null ) isOK = false;
		 if( OutputTempFileName == null ) isOK = false;
		 if( isOK == false ) {
			 errit("Initialization failed/not completed");
			 return false;
		 }
		 //
	   	 tracer = new generatorTrace(xMSet);
		 //
		 String ENCODING = xMSet.xU.getEncoding(TemplateName);
		 boolean inPayLoad = false;
		 try {
			  BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(TemplateName),ENCODING));
			  //
			  tempout = new gpPrintStream(OutputTempFileName, "UTF-8");
		  
			  //
	       	  String sLijn = null;
	       	  
	       	  while ((sLijn=reader.readLine()) != null) {
	       		 linesRead++;
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
	       		 
		         // look for IMPORT <filename>
		         if (sClean.startsWith("IMPORT ") == true ) 
		         {
		        	// import the mapping file
		        	String ShortMappingFileName = sLijn.trim().substring("IMPORT".length()).trim();
		            if( parseMappingFile( ShortMappingFileName ) == false ) { isOK = false;	break; }
		            // KN 09 SEP
		            if( stManager.overrule_specs() == false ) { isOK = false;	break; }
		            //
		            if( makeNetWork() == false ) { isOK = false;	break; }
		            //
		            if( makeTransformations() == false ) { isOK = false; break; }
		            // 
		            if( create_SourceBlock() == false ) { isOK = false; break; }
		            //
		            if( create_TargetBlock() == false ) { isOK = false; break; }
		            //
		            if( create_MappingBlock() == false ) { isOK = false; break; }
		            //
		            if( create_SQBlockAlt() == false ) { isOK = false; break; }
		            //
		            if( write_TransformationBlock() == false ){ isOK = false; break; }
		            //
		            if( create_InstanceBlock() == false ) { isOK = false; break; }
		            //
		            if( create_ConnectBlock() == false ) { isOK = false; break; }
			        //
		            if( create_LoadOrderBlock() == false ) { isOK = false; break; }
		            // done
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
	       	  tracer.close();
	       	  // GC help
	       	  mapList = null;
	       	  network = null;
	       	  instList = null;
	       	  trans_list = null;
	       	  connector_list = null;
	       	  cluster_list=null;
		 }
		 catch( Exception e ) {
			 errit("Error reading [" + TemplateName + "] " + xMSet.xU.LogStackTrace(e));
			 return false;
		 }
		 return true;
	}
	
	//----------------------------------------------------------------
	private boolean parseMappingFile(String sFileNameIn )
	//----------------------------------------------------------------
	{
		String ShortMappingFileName = sFileNameIn.trim();
    	String LongMappingFileName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Templates" + xMSet.xU.ctSlash + ShortMappingFileName;
    	if( xMSet.xU.IsBestand( LongMappingFileName ) == false ) {
    		 errit("Cannot find mapping specification file [" + LongMappingFileName + "] defined after IMPORT");
    		 return false;
    	}
        //	
		String SourceTableNameList = stManager.getSourceListDisplay();
		String TargetTableNameList = stManager.getTargetListDisplay();
		//
		generatorMapSpecReader rdr = new generatorMapSpecReader(xMSet);
 		mapList = rdr.readSpecs(  ShortMappingFileName , SourceTableNameList , TargetTableNameList );
 		if( mapList == null) return false;
 		if( mapList.size() == 0 ) {
 			errit("There is nothing to map ?");
 			return false;
 		}
 		// KB 9 SEP
 		stManager.optionList = rdr.getOptionList();
 		//
 		tracer.dumpTraceHeader( SourceTableNameList , TargetTableNameList );
 		//
 		tracer.dumpMapList( mapList , "Received from parser");
 		//
 		// Quality check
 		// CRC en KEY moeten laatste zijn in instructie lijst
 		for(int i=0;i<mapList.size();i++)
 		{
 		  int lastIdx = mapList.get(i).instr_list.size() - 1;
 		  for(int k=0;k<mapList.get(i).instr_list.size();k++)
 		  {
 		   generatorInstruction instr = mapList.get(i).instr_list.get(k);
 		   if( (instr.tipe == generatorConstants.INSTRUCTION_TYPE.STOP) || 
			   (instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_0) ||
 			   (instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_1) ||
 			   (instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_2) ||
 			   (instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_3) ||
 			   (instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_4) ||
 		       (instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_5) ||
 			   (instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_6) ||
 			   (instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_7) ||   // KB 7 DEC fixed bug was FKEY_6 was mentioned twice
 			   (instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_8) ||
 			   (instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_9) ||
 			   (instr.tipe == generatorConstants.INSTRUCTION_TYPE.TSTQUAL) || 
			   (instr.tipe == generatorConstants.INSTRUCTION_TYPE.CRC) || 
			   (instr.tipe == generatorConstants.INSTRUCTION_TYPE.KEY) ) {
 			   if( k != lastIdx ) {
 				   errit("MAP Syntax error. The {STOP,CRC,KEY,FKEY,TSTQUAL} functions must be the last instruction in a map chain. See [" + mapList.get(i).sourceField + "/" + mapList.get(i).targetField + "] line[" + mapList.get(i).lineno + "]");
 				   return false;
 			  }
 			  // STOP
 			  if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.STOP ) {
 	 				if( mapList.get(i).targetField != null ) {
 	 					if((mapList.get(i).targetField.compareToIgnoreCase("NIL") != 0) &&
 	 					   (mapList.get(i).targetField.compareToIgnoreCase("NOP") != 0) ) {
 	 					  errit("The name of the target field following a STOP instruction must be {NOP,NIL}. Found [" + mapList.get(i).targetField + "] on [" + mapList.get(i).sourceField + "] line [" + mapList.get(i).lineno + "]");
 	 					  return false;
 	 					}
 	 					else mapList.get(i).targetField = null;  // OVERRULE
 	 				}
 	 		  }
 			  else {  // KEY - CRC - FKEY - TSTQUAL
 				  int ndx = -1;
 				  int clstage=-1;
 	 			  switch( instr.tipe )
 	 			  {
 	 			  case KEY    : { ndx =  0; clstage = PKEY_STAGE; break; }
 	 			  case CRC    : { ndx =  1; clstage = CRC_STAGE ; break; }
 	 			  case FKEY_0 : { ndx =  2; clstage = FKEY_STAGE; break; }
 	 			  case FKEY_1 : { ndx =  3; clstage = FKEY_STAGE + 1; break; }
 	 			  case FKEY_2 : { ndx =  4; clstage = FKEY_STAGE + 2; break; }
 	 			  case FKEY_3 : { ndx =  5; clstage = FKEY_STAGE + 3; break; }
 	 			  case FKEY_4 : { ndx =  6; clstage = FKEY_STAGE + 4; break; }
 	 			  case FKEY_5 : { ndx =  7; clstage = FKEY_STAGE + 5; break; }
 	 			  case FKEY_6 : { ndx =  8; clstage = FKEY_STAGE + 6; break; }
 	 			  case FKEY_7 : { ndx =  9; clstage = FKEY_STAGE + 7; break; }
 	 			  case FKEY_8 : { ndx = 10; clstage = FKEY_STAGE + 8; break; }
 	 			  case FKEY_9 : { ndx = 11; clstage = FKEY_STAGE + 9; break; }
 	 			  case TSTQUAL: { ndx = 12; clstage = QUAL_STAGE; break; }
 	 			  default : {
 	 				  errit("(parseParamFile) overrun op de FKEY/CRC/KEY/TSTQUAL");
 	 				  return false;
 	 			   }
 	 			  }
 	 			  if( cluster_list[ndx] == null ) {
 	 				  cluster_list[ndx] = new FieldCluster( mapList.get(i).targetTableIdx , mapList.get(i).targetField , clstage ); 
 	 			  }
 	 			  else {  // verify whether the targetfield has not changed
 	 				  String prevTargetField = cluster_list[ndx].clstrTargetField.FieldName;
 	 				  int prevTargetTableIdx = cluster_list[ndx].clstrTargetField.tableIdx;
 	 				  if( (prevTargetTableIdx == mapList.get(i).targetTableIdx) && 
 	 					  (prevTargetField.compareToIgnoreCase(mapList.get(i).targetField)!=0) ) {
 	 					  errit("Map syntax error. " + instr.tipe + " has 2 different target fields [" + prevTargetField + "] and [" + mapList.get(i).targetField + "]. See line [" + mapList.get(i).lineno + "]");
 	 					  return false;
 	 				  }
 	 			  }
 	 			  String sKeyElement = (mapList.get(i).sourceField == null) ? mapList.get(i).targetField : mapList.get(i).sourceField;
 				  tinyField tif = new tinyField( mapList.get(i).targetTableIdx , sKeyElement );
 				  
 				  // TSTQUAL( n , <check soort> }
 				  if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.TSTQUAL ) {
 					  int kommaindex = instr.rVal.indexOf(",");
 					  if( kommaindex < 0 ) {
 						  errit("Invalid TSTQUAL(n,<testType>) construct for [" + mapList.get(i).sourceField + "." + mapList.get(i).targetField + "] on line [" + mapList.get(i).lineno + "]");
 				    	  return false;
 					  }
 					  try {
 					   String rechts = instr.rVal.substring(0,kommaindex).trim();
 					   String links  = instr.rVal.substring(kommaindex+1).trim();
 					   int rk = xMSet.xU.NaarInt(rechts);
 					   if( rk < 0 ) {
 						  errit("Invalid TSTQUAL(n,<testType>) " + instr.rVal + " construct for [" + mapList.get(i).sourceField + "." + mapList.get(i).targetField + "] on line [" + mapList.get(i).lineno + "] Not a valid number");
 				    	  return false;
 					   }
 					   tif.rank = rk;
 					   tif.dqualfunction = links;
 	//errit( "--> " + tif.rank + " | " + tif.dqualfunction + "|");	
 	                  
 					  }
 					  catch(Exception e) {
 						 errit("Issue on TSTQUAL(n,<testType>) construct for [" + mapList.get(i).sourceField + "." + mapList.get(i).targetField + "] on line [" + mapList.get(i).lineno + "]");
				    	  return false; 
 					  }
 					  
 				  }
 				  else {  //  FKEY(n) , CRC(n) , KEY(n)
 				   int rk = xMSet.xU.NaarInt(instr.rVal);
			       if( (rk < 0) || (rk>999) ) {
			    	  errit("Invalid CRC(n) construct for [" + mapList.get(i).sourceField + "." + mapList.get(i).targetField + "] N={0..99} on line [" + mapList.get(i).lineno + "]");
			    	  return false;
			       }
			       tif.rank = rk;
 				  }
			      cluster_list[ndx].clstrElementList.add( tif );
 			  }
 		   }
 		  }
 		}
 		
 		// A constant cannot be connected to a source field - a constant is injected midstream and must be the first in the instruction chain
 		for(int i=0;i<mapList.size();i++)
 		{
 			  for(int k=0;k<mapList.get(i).instr_list.size();k++)
 	 		  {
 				generatorInstruction instr = mapList.get(i).instr_list.get(k);
 				// There cannot be any FKEY left
 				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY ) {
 					 errit("System error 99 - FKEY tipe cannot feature anymore - See mapping [" + mapList.get(i).sourceField + "/" + mapList.get(i).targetField + "] on line [" + mapList.get(i).lineno + "]");
 					 return false;
 				}
 			    //	
 				if( instr.tipe != generatorConstants.INSTRUCTION_TYPE.CONST ) continue;
 				String sSourceField = mapList.get(i).sourceField.trim().toUpperCase();
 				if( mapList.get(i).sourceField != null ) {
 					if( (sSourceField.compareToIgnoreCase("NIL") != 0) && (sSourceField.compareToIgnoreCase("NOP") != 0) ) {
 					 errit("A constant must be declared in midstream i.e. decoupled from the source / source qualifier See mapping [" + mapList.get(i).sourceField + "/" + mapList.get(i).targetField + "] on line [" + mapList.get(i).lineno + "]");
 					 return false;
 					}
 				}
 				else {
 					errit("System error 100 - got a NULL on sourcefield");
 					return false;
 				}
 	 		  }
 		}
 		
 		// Are there target fields that are mapped twice
 		ArrayList<String> turfList = new ArrayList<String>();
 		for(int i=0;i<mapList.size();i++)
 		{
 			if( mapList.get(i).targetField == null ) continue;  // een STOP
 			String sTargetField = mapList.get(i).targetField.trim().toUpperCase();
 			if( sTargetField == null ) continue;
 			
 			// KEY, CRC and FKEY target fields are fields that can be mapped against multiple times - so skip if this is the case
 			boolean okSkip=false;
 			for(int k=0;k<cluster_list.length;k++)
 			{
 				if( cluster_list[k] == null ) continue;
 	            //errit( "[" + k + "] " + cluster_list[k].clstrTargetField.FieldName );
 				if( (mapList.get(i).targetTableIdx == cluster_list[k].clstrTargetField.tableIdx) &&
 					(mapList.get(i).targetField.compareToIgnoreCase(cluster_list[k].clstrTargetField.FieldName) == 0)) {
 					okSkip = true;
 					break;
 				}
 			}
 			if( okSkip ) continue;
 			//
 			
 			boolean found = false;
 			for(int k=0;k<turfList.size();k++)
 			{
 				if( turfList.get(k).compareToIgnoreCase( sTargetField + "|" + mapList.get(i).targetTableIdx ) == 0 ) {
 					found = true;
 					break;
 				}
 			}
 			if( found ) {
 				errit("Target field [" + sTargetField + "] has multiple incoming connects");
 				return false;
 			}
 			turfList.add( sTargetField + "|" + mapList.get(i).targetTableIdx );
 		}
 		// verify whether the key, crc or fkey target columns are not used twice
 		turfList = null;
 		turfList = new ArrayList<String>();
 		for(int i=0;i<cluster_list.length;i++)
 		{
 			if( cluster_list[i] == null ) continue;
 			boolean found = false;
 			String sTemp = cluster_list[i].clstrTargetField.FieldName + "|" + cluster_list[i].clstrTargetField.tableIdx;
 			for(int k=0;k<turfList.size();k++)
 			{
 			 if( sTemp.compareToIgnoreCase( turfList.get(k) ) == 0 ) {
 				 found = true;
 				 break;
 			 }
 			}
 			if( found ) {
 				errit("Key/CRC or FKEY target field [" + sTemp + "] is mapped occurs in more than 1 cluster");
 				return false;
 			}
 			turfList.add( sTemp );
 		}
 		//
 		// Sort the elements on the KEY - CRC or FKEYs
 		for(int i=0;i<cluster_list.length;i++)
 		{
 			if( cluster_list[i] == null ) continue;
 			if( fixClusterElements( cluster_list[i] ) == false ) return false;
 		}
 		
 	
 		//
 		return true;
	}
	
	//----------------------------------------------------------------
	private boolean fixClusterElements( FieldCluster x)
	//----------------------------------------------------------------
	{
		if( x == null ) return true;  // ok geen key gedefinieerd
		//
		String sRet="";
		for(int i=0;i<x.clstrElementList.size();i++) 
		{
		    sRet += "[(Key=" + x.clstrElementList.get(i).FieldName + ",Rank=" + x.clstrElementList.get(i).rank + ")] ";
		}
		logit(9,sRet);
		
		// make sure to initialize the DisplayNames to NULL
		for(int i=0 ; i < x.clstrElementList.size(); i++ ){
			 x.clstrElementList.get(i).PortName = null;	
		}
		// now sort the elements on their rank defined in the mapping
		int nCount = x.clstrElementList.size();
		for(int k=0;k<nCount;k++)
		{
			for(int i=0;i<(nCount-1);i++)
			{
				if( x.clstrElementList.get(i).rank >  x.clstrElementList.get(i+1).rank  ) 
				{
				   String sL = x.clstrElementList.get(i).FieldName;
				   int iidx  = x.clstrElementList.get(i).tableIdx;
				   int irnk  = x.clstrElementList.get(i).rank;
				   //
				   x.clstrElementList.get(i).FieldName = x.clstrElementList.get(i+1).FieldName;
				   x.clstrElementList.get(i).tableIdx  = x.clstrElementList.get(i+1).tableIdx;
				   x.clstrElementList.get(i).rank      = x.clstrElementList.get(i+1).rank;
				   //
				   x.clstrElementList.get(i+1).FieldName = sL;
				   x.clstrElementList.get(i+1).tableIdx  = iidx;
				   x.clstrElementList.get(i+1).rank      = irnk;
				}
			}
		}
	    // check for double occurences of the same rank
		for(int i=0;i<(x.clstrElementList.size()-1);i++)
		{
			if( x.clstrElementList.get(i).rank == x.clstrElementList.get(i+1).rank ) {
				errit("Error on KEY/CRC/FKEY definition.  Rank [" + x.clstrElementList.get(i).rank + "] occurs more than once, e.g. on " + x.clstrElementList.get(i).FieldName + " and on " + x.clstrElementList.get(i+1).FieldName + " for targetfield " + x.clstrTargetField.FieldName);
				return false;
			}
		}
		//
		//
		sRet="";
		for(int i=0;i<x.clstrElementList.size();i++) 
		{
		    sRet += "[(Key=" + x.clstrElementList.get(i).FieldName + ",Rank=" + x.clstrElementList.get(i).rank + ")] ";
		}
		logit(9,sRet);
		//
		return true;
	}
	
	
	//----------------------------------------------------------------
	private String perform_substitute(String sLijn)
	//----------------------------------------------------------------
	{
			String sRet=sLijn;
			//
			sRet = do_replace(sRet , "%NOW%" , ""+ xMSet.xU.prntDateTime(System.currentTimeMillis(), "dd/MM/yyyy HH:mm:ss"));
			sRet = do_replace(sRet , "%OWNER%"       , xMSet.whoami() );
			sRet = do_replace(sRet , "%APPLICATION%" , xMSet.getApplicationId() );
			//
			sRet = do_replace(sRet , "%MAPPINGNAME%"             , xHints.mappingName );
			sRet = do_replace(sRet , "%MAPPINGDESCRIPTION%"      , xHints.mappingDesc );
			//
			return sRet;
	}
		
	//----------------------------------------------------------------
	private String do_replace(String sIn , String sTag , String sVal )
	//----------------------------------------------------------------
	{
			String sRet = sIn;
			if( sIn.toUpperCase().indexOf(sTag) < 0 ) return sRet;
			sRet = xMSet.xU.RemplaceerIgnoreCase(sIn, sTag, sVal );
			return sRet;
	}
	
	//----------------------------------------------------------------
	private String addToLine( String sLijn , String sTag , String sVal )
	//----------------------------------------------------------------
	{
		return sLijn + " " + sTag + " =\"" + sVal + "\"";
	}
	
	//----------------------------------------------------------------
	private boolean create_SourceBlock()
	//----------------------------------------------------------------
	{
		int nCount = stManager.getNbrOfSources();
		for(int i=0;i<nCount;i++)
		{
			infaSource xSource = localgetInfaSource("CreateSourceblock", i );
			if( xSource == null ) return false;
	        if( create_SourceBlockItem(xSource) == false ) return false;		
		}
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean create_SourceBlockItem(infaSource xSource)
	//----------------------------------------------------------------
	{
		// <SOURCE BUSINESSNAME ="%businessname%" DATABASETYPE ="%databasetype%" 
		// DBDNAME ="%dbdname%" DESCRIPTION ="%description%" NAME ="%name01%" OBJECTVERSION ="%objectversion%" 
		// OWNERNAME ="%ownername%" VERSIONNUMBER ="%versionnumber%">
		String sLijn  = "<SOURCE";
		sLijn = addToLine( sLijn , "BUSINESSNAME"  , xSource.BusinessName );
		sLijn = addToLine( sLijn , "DATABASETYPE"  , xSource.Databasetype );
		sLijn = addToLine( sLijn , "DBDNAME"       , xSource.Dbdname );
		sLijn = addToLine( sLijn , "DESCRIPTION"   , xSource.Description );
		sLijn = addToLine( sLijn , "NAME"          , xSource.Name );
		sLijn = addToLine( sLijn , "OBJECTVERSION" , xSource.ObjectVersion );
		sLijn = addToLine( sLijn , "OWNERNAME"     , xSource.OwnerName );
		sLijn = addToLine( sLijn , "VERSIONNUMBER" , xSource.VersionNumber );
		sLijn = sLijn + ">";
		//
		prnt( sLijn );
		//
		if( xSource.tipe == readInfaXML.ParseType.SOURCEFLATFILE ) return create_source_flatfile(xSource);
		//
		boolean ib = do_source_fields(xSource);
		if( ib == false ) return false;
		//
		return true;
	}
	
	
	//----------------------------------------------------------------
	private boolean create_source_flatfile(infaSource xSource)
	//----------------------------------------------------------------
	{
		//<FLATFILE CODEPAGE ="%codepage%" CONSECDELIMITERSASONE ="%CONSECDELIMITERSASONE%" 
		//DELIMITED ="%delimited%" DELIMITERS ="%delimiters%" 
		//ESCAPE_CHARACTER ="%escapecharacter%" KEEPESCAPECHAR ="NO" 
	    //LINESEQUENTIAL ="NO" NULLCHARTYPE ="ASCII" 
		//NULL_CHARACTER ="*" PADBYTES ="1" QUOTE_CHARACTER ="NONE" REPEATABLE ="NO" 
		//ROWDELIMITER ="10" SHIFTSENSITIVEDATA ="NO" SKIPROWS ="0" STRIPTRAILINGBLANKS ="NO"/>
		
		String sLijn  = "<FLATFILE";
		sLijn = addToLine( sLijn , "CODEPAGE"  , xSource.flafle.CodePage );	
		sLijn = addToLine( sLijn , "CONSECDELIMITERSASONE"  , xSource.flafle.Consecdelimiterasone );	
		sLijn = addToLine( sLijn , "DELIMITED"  , xSource.flafle.Delimited );	
		sLijn = addToLine( sLijn , "DELIMITERS"  , xSource.flafle.Delimiters );
		sLijn = addToLine( sLijn , "ESCAPE_CHARACTER"  , xSource.flafle.EscapeCharacter );
		sLijn = addToLine( sLijn , "KEEPESCAPECHAR"  , xSource.flafle.EscapeCharacter );
		sLijn = addToLine( sLijn , "LINESEQUENTIAL"  , xSource.flafle.LineSequential );
	if( xMSet.getpowermartversion() > 8 ) { 
		sLijn = addToLine( sLijn , "MULTIDELIMITERSASAND"  , xSource.flafle.Multidelimitersasand );    // Not supported in V8
	}
		sLijn = addToLine( sLijn , "NULLCHARTYPE"  , xSource.flafle.NullCharType );
		sLijn = addToLine( sLijn , "NULL_CHARACTER"  , xSource.flafle.Nullcharacter );
		sLijn = addToLine( sLijn , "PADBYTES"  , xSource.flafle.Padbytes );
		sLijn = addToLine( sLijn , "QUOTE_CHARACTER"  , xSource.flafle.QuoteCharacter );
		sLijn = addToLine( sLijn , "REPEATABLE"  , xSource.flafle.Repeatable );
		sLijn = addToLine( sLijn , "ROWDELIMITER"  , xSource.flafle.RowDelimiter );
		sLijn = addToLine( sLijn , "SHIFTSENSITIVEDATA"  , xSource.flafle.ShiftSensitiveData );
		sLijn = addToLine( sLijn , "SKIPROWS"  , xSource.flafle.Skiprows );
		sLijn = addToLine( sLijn , "STRIPTRAILINGBLANKS"  , xSource.flafle.Striptrailingblanks );
		sLijn = sLijn + " />";
		//
		prnt( sLijn );
		//
		boolean ib = do_table_attributes( xSource.tableAttributeList );
		if( ib == false ) return false;
		//
		return do_source_fields( xSource );
	}
	
    //  <TABLEATTRIBUTE NAME ="Base Table Name" VALUE =""/>
	//----------------------------------------------------------------
	private boolean do_table_attributes( ArrayList<infaPair> list)
	//----------------------------------------------------------------
	{
		for(int i=0 ; i< list.size() ; i++ )
		{
			if( list.get(i).value == null ) continue;
			prnt("   <TABLEATTRIBUTE NAME = \"" + 
		             list.get(i).code.trim() + "\" VALUE =\"" +
				     list.get(i).value + 
				     "\" />" ); 
		}
		return true;
	}
	
	
	// to address all kinds of special case
	//----------------------------------------------------------------
	private String pamper_datatype(String sIn)
	//----------------------------------------------------------------
	{
		String sRet = sIn;
		if( sRet.compareToIgnoreCase("NUMBERPS") == 0 ) sRet =  "number(p,s)";
		return sRet.trim().toLowerCase();
	}
	
	//----------------------------------------------------------------
	private boolean do_source_fields(  infaSource xSource )
	//----------------------------------------------------------------
	{
				for(int i=0 ; i < xSource.fieldList.size(); i++)
				{
					infaSourceField fld = xSource.fieldList.get(i);
					String sNul = ( fld.mandatory == true ) ? "NOTNULL" : "NULL";
					int scale = ( fld.scale < 0 ) ? 0 : fld.scale;
					int precision = fld.Precision; 
					if( precision < 0 ) {
						errit("PRECISION is missing on [" + xSource.Name + "." + fld.Name );
						return false;
					}
					//
					String sLijn  = "<SOURCEFIELD";
					sLijn = addToLine( sLijn , "BUSINESSNAME"  	, fld.BusinessName  );	
					sLijn = addToLine( sLijn , "DATATYPE"  		, pamper_datatype(fld.DataType)  );
					sLijn = addToLine( sLijn , "DESCRIPTION"  	, fld.Description  );	
					sLijn = addToLine( sLijn , "FIELDNUMBER"  	, ""+fld.fieldNumber  );
				if( xMSet.getpowermartversion() > 8 ) { 
					sLijn = addToLine( sLijn , "FIELDPROPERTY"  , fld.FieldProperty  );  // Not supported in V8
				}	
					sLijn = addToLine( sLijn , "FIELDTYPE"  	, fld.FieldType  );	
					sLijn = addToLine( sLijn , "HIDDEN"  		, fld.Hidden  );	
					sLijn = addToLine( sLijn , "KEYTYPE"  		, fld.KeyType  );	
					sLijn = addToLine( sLijn , "LENGTH"  		, ""+fld.Length  );
					sLijn = addToLine( sLijn , "LEVEL"  		, ""+fld.Level  );
					sLijn = addToLine( sLijn , "NAME"  			, fld.Name  );
					sLijn = addToLine( sLijn , "NULLABLE"  		, sNul  );
					sLijn = addToLine( sLijn , "OCCURS"  		, ""+fld.Occurs  );
					sLijn = addToLine( sLijn , "OFFSET"  		, ""+fld.offset  );
					sLijn = addToLine( sLijn , "PHYSICALLENGTH" , ""+fld.physicalLength  );
					sLijn = addToLine( sLijn , "PHYSICALOFFSET" , ""+fld.physicalOffset  );
					sLijn = addToLine( sLijn , "PICTURETEXT"  	, ""+fld.PictureText );
					sLijn = addToLine( sLijn , "PRECISION"  	, ""+fld.Precision );
					sLijn = addToLine( sLijn , "SCALE"  		, ""+scale );
					sLijn = addToLine( sLijn , "USAGE_FLAGS"  	, fld.UsageFlags );
					//
					sLijn = sLijn + " />";
					//
					prnt( sLijn);
				}
				prnt("</SOURCE>");
				return true;
	}
	
	//----------------------------------------------------------------
	private boolean create_TargetBlock()
	//----------------------------------------------------------------
	{
		int nCount = stManager.getNbrOfTargets();
		for(int i=0;i<nCount;i++)
		{
			infaSource xTarget = localgetInfaTarget("CreateTargetBlock", i );
			if( xTarget == null ) return false;
			if( create_TargetBlockItem( xTarget )== false ) return false; 
		}
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean create_TargetBlockItem(infaSource xTarget)
	//----------------------------------------------------------------
	{
		    String sLijn  = "<TARGET";
			sLijn = addToLine( sLijn , "BUSINESSNAME"  , xTarget.BusinessName );
			sLijn = addToLine( sLijn , "CONSTRAINT"    , xTarget.Constraint );
			sLijn = addToLine( sLijn , "DATABASETYPE"  , xTarget.Databasetype );
			sLijn = addToLine( sLijn , "DESCRIPTION"   , xTarget.Description );
			sLijn = addToLine( sLijn , "NAME"          , xTarget.Name );
			sLijn = addToLine( sLijn , "OBJECTVERSION" , xTarget.ObjectVersion );
			sLijn = addToLine( sLijn , "TABLEOPTIONS"  , xTarget.TableOptions );
			sLijn = addToLine( sLijn , "VERSIONNUMBER" , xTarget.VersionNumber );
			sLijn = sLijn + ">";
			//
			prnt( sLijn );
			//
			if( xTarget.tipe == readInfaXML.ParseType.TARGETFLATFILE ) return create_target_flatfile(xTarget);
			//
			boolean ib = do_target_fields(xTarget);
			if( ib == false ) return false;
			return true;
	}
	
	//----------------------------------------------------------------
	private boolean create_target_flatfile(infaSource xTarget)
	//----------------------------------------------------------------
	{
		errit("OUTSTANDING - create target flatfile header");
		return false;
	}
	
	//----------------------------------------------------------------
	private boolean do_target_fields(infaSource xTarget)
	//----------------------------------------------------------------
	{
		for(int i=0 ; i < xTarget.fieldList.size(); i++)
		{
			infaSourceField fld = xTarget.fieldList.get(i);
			String sNul = ( fld.mandatory == true ) ? "NOTNULL" : "NULL";
			int scale = ( fld.scale < 0 ) ? 0 : fld.scale;
			int precision = fld.Precision; 
			if( precision < 0 ) {
				errit("PRECISION is missing on [" + xTarget.Name + "." + fld.Name );
				return false;
			}
			//
			String sLijn  = "<TARGETFIELD";
			sLijn = addToLine( sLijn , "BUSINESSNAME"  	, fld.BusinessName  );	
			sLijn = addToLine( sLijn , "DATATYPE"  		, pamper_datatype(fld.DataType)  );
			sLijn = addToLine( sLijn , "DESCRIPTION"  	, fld.Description  );	
			sLijn = addToLine( sLijn , "FIELDNUMBER"  	, ""+fld.fieldNumber  );
		if( xMSet.getpowermartversion() > 8 ) { 
			//sLijn = addToLine( sLijn , "FIELDPROPERTY"  , fld.FieldProperty  );  // Not supported in V8
		}	
			//sLijn = addToLine( sLijn , "FIELDTYPE"  	, fld.FieldType  );	
			//sLijn = addToLine( sLijn , "HIDDEN"  		, fld.Hidden  );	
			sLijn = addToLine( sLijn , "KEYTYPE"  		, fld.KeyType  );	
			//sLijn = addToLine( sLijn , "LENGTH"  		, ""+fld.Length  );
			//sLijn = addToLine( sLijn , "LEVEL"  		, ""+fld.Level  );
			sLijn = addToLine( sLijn , "NAME"  			, fld.Name  );
			sLijn = addToLine( sLijn , "NULLABLE"  		, sNul  );
			//sLijn = addToLine( sLijn , "OCCURS"  		, ""+fld.Occurs  );
			//sLijn = addToLine( sLijn , "OFFSET"  		, ""+fld.offset  );
			//sLijn = addToLine( sLijn , "PHYSICALLENGTH" , ""+fld.physicalLength  );
			//sLijn = addToLine( sLijn , "PHYSICALOFFSET" , ""+fld.physicalOffset  );
			sLijn = addToLine( sLijn , "PICTURETEXT"  	, ""+fld.PictureText );
			sLijn = addToLine( sLijn , "PRECISION"  	, ""+fld.Precision );
			sLijn = addToLine( sLijn , "SCALE"  		, ""+scale );
			//sLijn = addToLine( sLijn , "USAGEFLAGS"  	, fld.UsageFlags );
			//
			sLijn = sLijn + " />";
			//
			prnt( sLijn);
		}
		prnt("</TARGET>");
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean create_MappingBlock()
	//----------------------------------------------------------------
	{
		String sLijn  = "<MAPPING";
		sLijn = addToLine( sLijn , "DESCRIPTION"  	, xHints.mappingDesc  );	
		sLijn = addToLine( sLijn , "ISVALID"  		, "YES"  );
		sLijn = addToLine( sLijn , "NAME"  			, xHints.mappingName  );
		sLijn = addToLine( sLijn , "OBJECTVERSION" 	, "1"  );
		sLijn = addToLine( sLijn , "VERSIONNUMBER" 	, "1"  );
		sLijn = sLijn + ">";
		//
		prnt(sLijn);
		return true;
	}
	
	
	//----------------------------------------------------------------
	private boolean create_SQBlockAlt()
	//----------------------------------------------------------------
	{
		int nCount = stManager.getNbrOfSources();
		for(int i=0;i<nCount;i++)
		{
			infaSource xSource = localgetInfaSource("CreateSQBlockAlt", i );
			if( xSource == null ) return false;
	        if( create_SQBlockItem(xSource , i ) == false ) return false;		
		}
		return true;
	}
	 
	//----------------------------------------------------------------
	private boolean create_SQBlockItem(infaSource xSource , int sourceIdx)
	//----------------------------------------------------------------
	{
		    // go looking for the source qualifier
			int idx = -1;
			for(int i=0;i<trans_list.size();i++)
			{
				if( trans_list.get(i).TransformationType == infaInstance.TRANSFORMATION_TYPE.SOURCE_QUALIFIER ) {
					if( trans_list.get(i).stage == (SQ_STAGE + sourceIdx) ) {
						idx = i;
						break;
					}
				}
			}
			if( idx < 0 ) {
				errit("cannot locate SOURCE QUALIFIER");
				return false;
			}
			logit(9,"SQ = " + idx );
			infaTransformation tx = trans_list.get(idx);
			//
			String sLijn = "<TRANSFORMATION";
			sLijn = addToLine( sLijn , "DESCRIPTION"  	, tx.Description  );
			sLijn = addToLine( sLijn , "NAME"  			, tx.Name  );
			sLijn = addToLine( sLijn , "OBJECTVERSION"  , "1"  );
			sLijn = addToLine( sLijn , "REUSABLE"  		, "NO"  );
			sLijn = addToLine( sLijn , "TYPE"  			, "Source Qualifier"  );
			sLijn = addToLine( sLijn , "VERSIONNUMBER"  , "1"  );
			sLijn = sLijn + " >";
		    //
			prnt( sLijn );
			//
			// de Source qual lines
			String sRet ="";
			for (int i=0;i<tx.txnfld_list.size();i++)
			{
					  infaTransformationField  fld =  tx.txnfld_list.get(i);
					  sRet = "";
					  sRet += " DATATYPE ="     + "\"" + pamper_datatype(fld.Datatype.toLowerCase().trim()) + "\"";
					  sRet += " DEFAULTVALUE =" + "\"" + fld.DefaultValue + "\"";
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
			for(int j=0 ; j<tx.AttrList.size() ; j++ )
			{
						sRet = "     <TABLEATTRIBUTE NAME =\"" + tx.AttrList.get(j).code + "\" VALUE =\"" +  tx.AttrList.get(j).value + "\"/>";
				        prnt(sRet);
			}
			//
			prnt("</TRANSFORMATION>");
			//
			return true;
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
			case EXPRESSION         : return "Expression"; 
			default :  {
			    errit("TTFormat unsupported tipe " + transTipe);
			    return "SHOWSTOPPER";
			 }
			}
	}
	
	//----------------------------------------------------------------
	private boolean create_InstanceBlock()
	//----------------------------------------------------------------
	{
		instList = new ArrayList<infaInstance>();   // Instance list
	    for(int i=0 ; i<trans_list.size();i++)
		{
			infaInstance isrc = new infaInstance(xMSet,null);
			infaTransformation tra = trans_list.get(i);
			switch( tra.TransformationType )
			{
			case SOURCE_DEFINITION : {
				 isrc.InstanceName        			= "SRC_" + tra.Name;
				 isrc.Description        			= tra.Description;
				 isrc.Transformation_Name 			= tra.Name;
				 isrc.Associated_Source_Instance 	= null;
				 isrc.Dbdname 						= tra.Dbdname;
				 isrc.reusable 						= "NO";
				 isrc.Transformation_Type 			= infaInstance.TRANSFORMATION_TYPE.SOURCE_DEFINITION;
				 isrc.Instance_Type       			= infaInstance.INSTANCE_TYPE.SOURCE;
				 isrc.isValid 						= true;
				 isrc.obj 							= null;
				 isrc.stage                         = tra.stage;
				 break;    }
			case TARGET_DEFINITION : {
				 isrc.InstanceName        			= "TGT_" + tra.Name;
				 isrc.Description        			= tra.Description;
				 isrc.Transformation_Name 			= tra.Name;
				 isrc.Associated_Source_Instance 	= null;
				 isrc.Dbdname 						= null;  // not needed
				 isrc.reusable 						= "NO";
				 isrc.Transformation_Type 			= infaInstance.TRANSFORMATION_TYPE.TARGET_DEFINITION;
				 isrc.Instance_Type       			= infaInstance.INSTANCE_TYPE.TARGET;
				 isrc.isValid 						= true;
				 isrc.obj 							= null;
				 isrc.stage                         = tra.stage;
				 break;    }
			case SOURCE_QUALIFIER : {
				 isrc.InstanceName        			= xMSet.xU.Remplaceer(tra.Name, "SQ_" , "SRCQUAL_"); //"SRCQUAL_" + tra.Name;
				 isrc.Description        			= tra.Description;
				 isrc.Transformation_Name 			= tra.Name;
				 isrc.Associated_Source_Instance 	= tra.Assoc;
				 isrc.Dbdname 						= null;  // not needed
				 isrc.reusable 						= "NO";
				 isrc.Transformation_Type 			= infaInstance.TRANSFORMATION_TYPE.SOURCE_QUALIFIER;
				 isrc.Instance_Type       			= infaInstance.INSTANCE_TYPE.TRANSFORMATION;
				 isrc.isValid 						= true;
				 isrc.obj 							= null;
				 isrc.stage                         = tra.stage;
				 break;    }
			case EXPRESSION : {
				 isrc.InstanceName        			= tra.Name;
				 isrc.Description        			= tra.Description;
				 isrc.Transformation_Name 			= tra.Name;
				 isrc.Associated_Source_Instance 	= null;
				 isrc.Dbdname 						= null;  // not needed
				 isrc.reusable 						= null;
				 isrc.Transformation_Type 			= infaInstance.TRANSFORMATION_TYPE.EXPRESSION;
				 isrc.Instance_Type       			= infaInstance.INSTANCE_TYPE.TRANSFORMATION;
				 isrc.isValid 						= true;
				 isrc.obj 							= null;
				 isrc.stage                         = tra.stage;
				 break;    }
			default : { errit("oeps - unsupported transformation type [" + tra.TransformationType + "]"); return false; }
			}
			// Instance name zetten
			trans_list.get(i).InstanceName = isrc.InstanceName;
			//
			instList.add(isrc);
		}
		
		// output
		for(int i=0;i<instList.size();i++)
		{
			infaInstance xnst = instList.get(i);
			xnst.sho();
			
			String sLijn = "<INSTANCE";
			if( xnst.Dbdname != null ) {
		   	sLijn = addToLine( sLijn , "DBDNAME"				, xnst.Dbdname  );
			}
		   	sLijn = addToLine( sLijn , "DESCRIPTION"  			, xnst.Description);
		 	sLijn = addToLine( sLijn , "NAME"  					, xnst.InstanceName  );
			sLijn = addToLine( sLijn , "TRANSFORMATION_NAME"	, xnst.Transformation_Name  );
			sLijn = addToLine( sLijn , "TRANSFORMATION_TYPE"	, TTFormat(xnst.Transformation_Type) );  // Lower cases no underscore
			sLijn = addToLine( sLijn , "TYPE"					, ""+xnst.Instance_Type  );
			if( xnst.Associated_Source_Instance != null ) {
				sLijn = sLijn + ">";
				prnt(sLijn);
				prnt("<ASSOCIATED_SOURCE_INSTANCE ");
				prnt(" NAME=\"" + xnst.Associated_Source_Instance + "\"/>");
				prnt("</INSTANCE>");
			}
			else {
				sLijn = sLijn + " />";
				prnt(sLijn);	
			}
			
		}
		return true;
	}
	
	// <TARGETLOADORDER ORDER ="1" TARGETINSTANCE ="TGT_%TargetTableName01%"/>
	//----------------------------------------------------------------
	private boolean create_LoadOrderBlock()
	//----------------------------------------------------------------
	{
		infaSource xTarget = localgetInfaTarget("createloadorderblock",0);  // probably correct to pick the first one
		if( xTarget == null ) return false;
		String sLijn  = "<TARGETLOADORDER ORDER =\"1\" TARGETINSTANCE =\"TGT_" + xTarget.Name + "\" />";
		prnt( sLijn );
		return true;
	}
	
	//----------------------------------------------------------------
	private infaSourceField getSourceFieldViaName( infaSource x , String sName)
	//----------------------------------------------------------------
	{
		for(int i=0;i<x.fieldList.size();i++)
		{
			if( x.fieldList.get(i).Name.trim().compareToIgnoreCase( sName.trim() ) == 0 ) return x.fieldList.get(i);
		}
		errit("Could not locate Source/Targetfield [" + sName + "] on [" + x.Name + "]");
		return null;
	}
	
	// if there are only CONST transformations a dummy mapping is needed 
	//----------------------------------------------------------------
	private boolean perform_preprocess()
	//----------------------------------------------------------------
	{
		int constants=0;
		int effective=0;
		int genuine=-1;
		for(int i=0;i<mapList.size();i++)
		{
			generatorMapping mp = mapList.get(i);
			if( (mp.sourceTableIdx >=0) && (mp.targetTableIdx>=0) ) {
				if( genuine < 0 ) genuine = i;
			}
			for(int k=0;k<mp.instr_list.size();k++)
			{
				generatorInstruction instr = mp.instr_list.get(k);
				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.KEY ) continue;
				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.CRC ) continue;
				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_0 ) continue;
				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_1 ) continue;
				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_2 ) continue;
				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_3 ) continue;
				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_4 ) continue;
				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_5 ) continue;
				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_6 ) continue;
				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_7 ) continue;
				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_8 ) continue;
				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY_9 ) continue;
				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.TSTQUAL ) continue;
				//
				if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.CONST ) {
					constants++;
					continue;
				}
				effective++;
			}
		}
		if( (constants > 0) && (effective == 0) ) {
			logit(1,"This mapping will result in an unconnected EXP-100. Dummy connection will be created");
			/*
			 * MAPPING Field [0][FF_CLAIMLABOUR_CCD.CLAIM_LABOUR_ID] to [0][SST_CLAIMLABOUR.nil] [Rank=-9]
22JUL 12:51:47.500    [pcGenerator.generator.generatorTrace]         Instr [1] [tipe=STOP] [rval=] [SOURCE_QUALIFIER] [TARGET] [null] [null] [-1.-1]
			 */
			if( mapList.size()< 1) {
				errit("(perform preprocess) Empty mapping");
				return false;
			}
            if( genuine < 0 ) {
            	errit("(perform_preprocess) Cannot locate a genuine column to column mapping");
            	return false;
            }

			generatorMapping mp = mapList.get(genuine);
			generatorMapping mx = new generatorMapping( mp.sourceTableIdx , mp.sourceTableName , mp.sourceField ,
														mp.targetTableIdx , mp.sourceTableName , null );
			generatorInstruction inx = new generatorInstruction( xMSet.getNextUID() , null );
			inx.tipe = generatorConstants.INSTRUCTION_TYPE.STOP;
			inx.fromPortTipe = generatorConstants.PORTTYPE.SOURCE_QUALIFIER;
			inx.toPortTipe = generatorConstants.PORTTYPE.TARGET;
			mx.instr_list.add(inx);
			mapList.add( mx );
		}
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean makeNetWork()
	//----------------------------------------------------------------
	{
		// pre processor
		if( perform_preprocess() == false ) return false;
		//
		network = new ArrayList<generatorMapping>();
		// This is the actual core module of make network
		int nCount = stManager.getNbrOfSources();
		for(int i=0;i<nCount;i++)
		{
			infaSource xSource = localgetInfaSource("makenetwork", i );
			if( xSource == null ) return false;
	        if( makeNetworkFirstPart(xSource , i ) == false ) return false;		
		}
		
		// add the mapping instructions that have a lefthand side of NIL or NOP e.g. CONST
		if( makeNetWork_LeftHandNil() == false ) return false;
		//this.dumpMapList( network , "DEBUG0");
		
		// Set datatype on source from the fieldlist on source
		if( makeNetWork_SetSourceDataType() == false ) return false;
		
		// Set the datatype on target
		if( makeNetWork_SetTargetDataType() == false ) return false;
		//this.dumpMapList(network , "DEBUG1");
		
		// Set the datatype on Source Qualifier
		if( makeNetWork_SetSourceQualifierDataType() == false ) return false;
        //tracer.dumpMapList(network , "DEBUG2");
		
		// copy the SQ datatype to its successors (except target)
		if( makeNetWork_PropagateSQDataType() == false ) return false;
        //tracer.dumpMapList(network , "DEBUG3");
		
		// for those transformations without a type (ie. not connected to a sourcefield ) use the datatype of the final datatype
		if( makeNetWork_BackPropagateDataType() == false ) return false;
        //tracer.dumpMapList(network , "DEBUG4");
		
		// Set rank  - this is the number of times the same sourcefield appears at the beginning of a mapline
		if( makeNetWork_LastPart() == false ) return false;
		
		// Report
		tracer.dumpMapList( network , "Code Network");
		//
		
		return true;
	}
	
	//----------------------------------------------------------------
		private boolean makeNetworkFirstPart(infaSource xSource , int SourceIdx)
		//----------------------------------------------------------------
		{
			// scan through the source and create the network for each of its columns
			// steps :
			// Create a SOURCE
			// CREATE a SOURCE QUALIFIER
			// copy the map instructions form the map text file
			// Create a TOEXTERNAL
					for(int i=0;i<xSource.fieldList.size();i++)
					{
						String sFieldName = xSource.fieldList.get(i).Name.trim();
						boolean found = false;
						//int curlevel = 0;
						for(int k=0;k<mapList.size();k++)
						{
							//
							if( mapList.get(k).sourceTableIdx != SourceIdx ) continue;
							// Source Field is used in mapping
							if( mapList.get(k).sourceField.trim().compareToIgnoreCase(sFieldName) == 0 ) 
							{
								//curlevel=0;
								found = true;
								int idx = k;
								mapList.get(idx).canberemoved = true;
								// create the start of the chain
								generatorMapping gp = new generatorMapping( mapList.get(idx).sourceTableIdx , mapList.get(idx).sourceTableName ,mapList.get(idx).sourceField , 
										                                    mapList.get(idx).targetTableIdx , mapList.get(idx).targetTableName , mapList.get(idx).targetField );
								generatorInstruction in1 = new generatorInstruction( xMSet.getNextUID() , generatorConstants.SELFTYPE.SRC );
								//in1.fromUID      = xSource.fieldList.get(i).UID;
								in1.tipe         = generatorConstants.INSTRUCTION_TYPE.NOP;
								in1.fromPortTipe = generatorConstants.PORTTYPE.SOURCE;
								in1.toPortTipe   = generatorConstants.PORTTYPE.SOURCE_QUALIFIER;
								in1.rVal         = null;
								in1.stage        = SRC_STAGE + mapList.get(idx).sourceTableIdx;  // 10 + x
								gp.instr_list.add( in1 );
								if( mapList.get(idx).instr_list.size() == 0 )  
								{
									generatorInstruction in2 = new generatorInstruction( xMSet.getNextUID() , generatorConstants.SELFTYPE.SQ );
									//in2.fromUID      = xSource.fieldList.get(i).UID;
									in2.tipe         = generatorConstants.INSTRUCTION_TYPE.PASSTHRU;
									in2.fromPortTipe = generatorConstants.PORTTYPE.SOURCE_QUALIFIER;
									in2.toPortTipe   = generatorConstants.PORTTYPE.TARGET;
									in2.rVal         = null;
									in2.stage        = SQ_STAGE + mapList.get(idx).sourceTableIdx;  // 30 + x
									gp.instr_list.add( in2 );	
							    }
								else {  // create een passthru van de SQ naar de TRA
									generatorInstruction in2 = new generatorInstruction( xMSet.getNextUID() , generatorConstants.SELFTYPE.SQ);
									//in2.fromUID      = xSource.fieldList.get(i).UID;
									in2.tipe         = generatorConstants.INSTRUCTION_TYPE.PASSTHRU;
									in2.fromPortTipe = generatorConstants.PORTTYPE.SOURCE_QUALIFIER;
									in2.toPortTipe   = generatorConstants.PORTTYPE.TRANSFORMATION;
									in2.rVal         = null;
									in2.stage        = SQ_STAGE + mapList.get(idx).sourceTableIdx;  // 30 + x
									gp.instr_list.add( in2 );	
								}
							    // re-insert the instructions from the MAP file	
							  	for(int z=0;z<mapList.get(idx).instr_list.size();z++)
							  	{
								  		generatorInstruction oo = mapList.get(idx).instr_list.get(z);
								  		generatorInstruction in3 = new generatorInstruction( xMSet.getNextUID() , generatorConstants.SELFTYPE.TRANS );
										//in3.fromUID      = xSource.fieldList.get(i).UID;
										in3.tipe         = oo.tipe;
										in3.fromPortTipe = oo.fromPortTipe;
										in3.toPortTipe   = oo.toPortTipe;
										in3.rVal         = oo.rVal;
										switch( in3.tipe )
										{
										case KEY    : { in3.stage = PKEY_STAGE; break; }
										case CRC    : { in3.stage = CRC_STAGE; break; }
										case FKEY_0 : { in3.stage = FKEY_STAGE + 0; break; }
										case FKEY_1 : { in3.stage = FKEY_STAGE + 1; break; }
										case FKEY_2 : { in3.stage = FKEY_STAGE + 2; break; }
										case FKEY_3 : { in3.stage = FKEY_STAGE + 3; break; }
										case FKEY_4 : { in3.stage = FKEY_STAGE + 4; break; }
										case FKEY_5 : { in3.stage = FKEY_STAGE + 5; break; }
										case FKEY_6 : { in3.stage = FKEY_STAGE + 6; break; }
										case FKEY_7 : { in3.stage = FKEY_STAGE + 7; break; }
										case FKEY_8 : { in3.stage = FKEY_STAGE + 8; break; }
										case FKEY_9 : { in3.stage = FKEY_STAGE + 9; break; }
										case TSTQUAL: { in3.stage = QUAL_STAGE; break; }  
										default   : { in3.stage = TRANS_STAGE + (50*mapList.get(idx).sourceTableIdx) + z; break; }
										}
								        gp.instr_list.add( in3 );
							  	}
							  	// add a stopgap 
							  	generatorInstruction in4 = new generatorInstruction( xMSet.getNextUID() , generatorConstants.SELFTYPE.TGT);
								//in4.fromUID      = xSource.fieldList.get(i).UID;
								in4.tipe         = generatorConstants.INSTRUCTION_TYPE.NOP;
								in4.fromPortTipe = generatorConstants.PORTTYPE.TARGET;
								in4.toPortTipe   = generatorConstants.PORTTYPE.TOEXTERNAL;
								in4.rVal         = null;
								in4.stage        = TGT_STAGE + mapList.get(idx).targetTableIdx; // OPGEPAST
								gp.instr_list.add( in4 );	
							    //
								network.add( gp );
							}
						} // iterator through the fields on the map file
						
						
						// This is a sourcefield present in metadata but not in the mapping
						// Just add a dummy attaching the source to the SQ
						if( found == false ) {
							generatorMapping gp = new generatorMapping( SourceIdx , xSource.Name , sFieldName , -1 , null , null );
							generatorInstruction in1 = new generatorInstruction( xMSet.getNextUID() , generatorConstants.SELFTYPE.SRC);
							//in1.fromUID      = xSource.fieldList.get(i).UID;
							in1.tipe         = generatorConstants.INSTRUCTION_TYPE.NOP;
							in1.fromPortTipe = generatorConstants.PORTTYPE.SOURCE;
							in1.toPortTipe   = generatorConstants.PORTTYPE.SOURCE_QUALIFIER;
							in1.rVal         = null;
							in1.stage        = SRC_STAGE;
							gp.instr_list.add( in1 );
							//
							network.add( gp );
						}
					} // iterator Source Fields
					return true;
		}
	
	private String shoInstr(generatorMapping m)
	{
		String sRet = "";
		if( m == null ) return sRet;
		if( m.instr_list == null ) return sRet;
		for(int i=0;i<m.instr_list.size();i++)
		{
			if ( i!= 0) sRet += ",";
			sRet += m.instr_list.get(i).tipe + "-" + m.instr_list.get(i).rVal;
		}
		return "[" + sRet + "]";
	}
		
	// add network instructions that have a left hand side of NIL or NOP, e.g. CONST instruction
	//----------------------------------------------------------------
	private boolean makeNetWork_LeftHandNil()
	//----------------------------------------------------------------
	{
		for(int i=0;i<mapList.size();i++)
		{
			if( mapList.get(i).canberemoved == true ) continue;
			// check whether fieldname is really NIL
			if( (mapList.get(i).sourceField.trim().compareToIgnoreCase("NIL") != 0) && (mapList.get(i).sourceField.trim().compareToIgnoreCase("NOP") != 0) ) {
				errit("Left hand mapping instruction needs to be a valid column name or {NIL,NOP} -> [" + mapList.get(i).sourceField + "] [Line=" + mapList.get(i).lineno + "] " + shoInstr(mapList.get(i)) );
				return false;
			}
			//
			if( mapList.get(i).instr_list.size() == 0 ) {
				errit("Left hand {NIL,NOP} must be followed by a set of instructions -> [" + mapList.get(i).sourceField + "] [Line=" + mapList.get(i).lineno + "] " + shoInstr(mapList.get(i)) );
				return false;
			}
			// restrictive : we only support a CONST as the first instruction to follow a NOP/NIL
			if( (mapList.get(i).instr_list.get(0).tipe != generatorConstants.INSTRUCTION_TYPE.CONST) &&
				(mapList.get(i).instr_list.get(0).tipe != generatorConstants.INSTRUCTION_TYPE.NOW) &&
				(mapList.get(i).instr_list.get(0).tipe != generatorConstants.INSTRUCTION_TYPE.SYSDATE) ) {
				errit("Left hand {NIL,NOP} NOT immediately followed by  {CONST,NOW,SYSDATE} is invalid -> [" + mapList.get(i).sourceField + "->" + mapList.get(i).targetField + "] [Line=" + mapList.get(i).lineno + "] " + shoInstr(mapList.get(i)));
				return false;
			}
			//int curlevel=1; // !! one past the SQ
			generatorMapping gp = new generatorMapping( -1 , null , null , mapList.get(i).targetTableIdx , mapList.get(i).targetTableName ,mapList.get(i).targetField );
			// Create PASSTRU from SQ to TRANS
			{
				generatorInstruction in2 = new generatorInstruction( xMSet.getNextUID() , generatorConstants.SELFTYPE.SQ );
				//in2.fromUID      = xSource.fieldList.get(i).UID;
				in2.tipe         = generatorConstants.INSTRUCTION_TYPE.PASSTHRU;
				in2.fromPortTipe = generatorConstants.PORTTYPE.SOURCE_QUALIFIER;
				in2.toPortTipe   = generatorConstants.PORTTYPE.TRANSFORMATION;
				in2.rVal         = null;
				in2.stage        = SQ_STAGE;
				gp.instr_list.add( in2 );	
			}
			for(int k=0;k<mapList.get(i).instr_list.size();k++)
		  	{
		  		generatorInstruction oo = mapList.get(i).instr_list.get(k);
		  		generatorInstruction in3 = new generatorInstruction( xMSet.getNextUID() , generatorConstants.SELFTYPE.TRANS);
				//in3.fromUID      = -1; // there is no sourcefeld so there is no UID
				in3.tipe         = oo.tipe;
				in3.fromPortTipe = oo.fromPortTipe;
				in3.toPortTipe   = oo.toPortTipe;
				in3.rVal         = oo.rVal;
				switch( in3.tipe )
				{
				case KEY    : { in3.stage = PKEY_STAGE; break; }
				case CRC    : { in3.stage = CRC_STAGE; break; }
				case FKEY_0 : { in3.stage = FKEY_STAGE + 0; break; }
				case FKEY_1 : { in3.stage = FKEY_STAGE + 1; break; }
				case FKEY_2 : { in3.stage = FKEY_STAGE + 2; break; }
				case FKEY_3 : { in3.stage = FKEY_STAGE + 3; break; }
				case FKEY_4 : { in3.stage = FKEY_STAGE + 4; break; }
				case FKEY_5 : { in3.stage = FKEY_STAGE + 5; break; }
				case FKEY_6 : { in3.stage = FKEY_STAGE + 6; break; }
				case FKEY_7 : { in3.stage = FKEY_STAGE + 7; break; }
				case FKEY_8 : { in3.stage = FKEY_STAGE + 8; break; }
				case FKEY_9 : { in3.stage = FKEY_STAGE + 9; break; }
				case TSTQUAL: { in3.stage = QUAL_STAGE; break; }  
				default     : { in3.stage = TRANS_STAGE + (50 * 0 ) + k; break; }
				}
			    gp.instr_list.add( in3 );
		  	}
			{  // final leg
				generatorInstruction in4 = new generatorInstruction( xMSet.getNextUID() , generatorConstants.SELFTYPE.TGT);
				//in4.fromUID      = -1;  // there is no sourcefeld so there is no UID
				in4.tipe         = generatorConstants.INSTRUCTION_TYPE.NOP;
				in4.fromPortTipe = generatorConstants.PORTTYPE.TARGET;
				in4.toPortTipe   = generatorConstants.PORTTYPE.TOEXTERNAL;
				in4.rVal         = null;
				in4.stage        = TGT_STAGE;
				gp.instr_list.add( in4 );	
			}
			network.add( gp );
		}
		//  Help the garbage collector - map is no longer needed from here onwards
		mapList = null;
		//
		return true;
	}
	
	// set the datatype on the source
	//----------------------------------------------------------------
	private boolean makeNetWork_SetSourceDataType()
	//----------------------------------------------------------------
	{
		for(int i=0; i< network.size();i++)
		{
		 if ( network.get(i).sourceField == null ) continue;
		 infaSource xSource = localgetInfaSource("maaknetwork-datatype-SOURCE",network.get(i).sourceTableIdx);
		 if( xSource == null ) return false;
		 //
		 infaSourceField fld = getSourceFieldViaName( xSource , network.get(i).sourceField );
		 if( fld == null ) return false;
		 if( network.get(i).instr_list.get(0).fromPortTipe != generatorConstants.PORTTYPE.SOURCE ) {
			 errit("(makeNetwork) System Error 1");
			 return false;
		 }
		 network.get(i).instr_list.get(0).dbmake  = xRdbms.getDBMAKEType( xSource.Databasetype );
		 if(  network.get(i).instr_list.get(0).dbmake == null ) {
			 errit("(makeNetwork) System Error 2 - cannot resolve database type [" + xSource.Databasetype  + "]");
			 return false;
		 }
		 network.get(i).instr_list.get(0).Datatype       = fld.DataType; 
		 network.get(i).instr_list.get(0).Precision      = fld.Precision; 
		 network.get(i).instr_list.get(0).Scale          = fld.scale;
		 network.get(i).instr_list.get(0).Length         = fld.Length;
		 network.get(i).instr_list.get(0).PhysicalLength = fld.physicalLength;
		} // for
		return true;
	}
	
	// set the Datatype on the source
	//----------------------------------------------------------------
	private boolean makeNetWork_SetTargetDataType()
	//----------------------------------------------------------------
	{
		for(int i=0; i< network.size();i++)
		{
		 if ( network.get(i).targetField == null ) continue;  // valid after a STOP instuction and unconnected sourcefields
		 //
		 infaSource xTarget = localgetInfaTarget("maaknetwork-datatype-TARGET",network.get(i).targetTableIdx);
		 if( xTarget == null ) return false;
		 //
	 	 infaSourceField fld = getSourceFieldViaName( xTarget , network.get(i).targetField );
		 if( fld == null ) return false;
		 //
		 int zz = network.get(i).instr_list.size()-1;  // take last one
		 if( network.get(i).instr_list.get(zz).toPortTipe != generatorConstants.PORTTYPE.TOEXTERNAL ) {
			 errit("(makeNetwork) System Error 3 - tipe = [" + network.get(i).instr_list.get(zz).toPortTipe + "]");
			 return false;
		 }
		 network.get(i).instr_list.get(zz).dbmake         = xRdbms.getDBMAKEType( xTarget.Databasetype );
		 if(  network.get(i).instr_list.get(zz).dbmake == null ) {
			 errit("(makeNetwork) System Error 2 - cannot resolve database type [" + xTarget.Databasetype  + "]");
			 return false;
		 }
		 network.get(i).instr_list.get(zz).Datatype       = fld.DataType; 
		 network.get(i).instr_list.get(zz).Precision      = fld.Precision; 
		 network.get(i).instr_list.get(zz).Scale          = fld.scale;
		 network.get(i).instr_list.get(zz).Length         = fld.Length;
		 network.get(i).instr_list.get(zz).PhysicalLength = fld.physicalLength;
		}
		
		return true;
	}
	
	 // Extremity = { SOURCE , TARGET }
	 //----------------------------------------------------------------
	 private infaDataType convertDataType( generatorInstruction instr , String extremity , String sourceTableName , String sourceFieldName)
	 //----------------------------------------------------------------
	 {
		    if( extremity == null ) return null;
		    // either pick a SOURCE or TARGET datatype
		    infaDataType dt = new infaDataType( xMSet , ""+instr.dbmake , extremity , instr.Datatype , instr.Precision , instr.Scale );
		    if( (dt.DataType == null) || (dt.isValid == false) ) {
			    	errit("Cannot create INFA Datatype object");
			    	return null;
		    }
		    // Convert to PowerCenter datatypes for a source qualifier
		    infaDataType sqdt = converter.convertRDBMSToInfa( dt , "SOURCEQUALIFIER" );
		    if( (sqdt == null) || (sqdt.isValid == false) ) {
		    	errit("Cannot transform into INFA Datatype object");
		    	return null;
		    }
		    String sRet = sourceTableName + "." + sourceFieldName + " " + extremity + " " + 
	    		      "[DT:" + dt.DataType + "->" + sqdt.DataTypeDisplay + "]" +
	                  "[PR:" + dt.Precision + "->" + sqdt.Precision + "]" +
	    		      "[SC:" + dt.Scale + "->" + sqdt.Scale + "]" +
	                  "[LE:" + dt.Length + "->" + sqdt.Length + "]" +
	    		      "[PL:" + dt.PhysicalLength + "->" + sqdt.PhysicalLength + "]";
	        logit(9,sRet);
	        dt=null;
	        return sqdt;
	 }
	 
	 // Set the datatype on th Source Qualifier
	 //----------------------------------------------------------------
	 private boolean makeNetWork_SetSourceQualifierDataType()
	 //----------------------------------------------------------------
	 {
		 for(int i=0; i< network.size();i++)
		 {
				 if( network.get(i).instr_list.get(0).fromPortTipe != generatorConstants.PORTTYPE.SOURCE ) continue;
				 if( network.get(i).instr_list.get(0).toPortTipe != generatorConstants.PORTTYPE.SOURCE_QUALIFIER ) continue;
				 
				//logit( 5 , "Checking " + network.get(i).instr_list.get(0).Datatype + " " + network.get(i).instr_list.get(0).dbmake + " " + network.get(i).sourceField );
				 
				// create a datatype object 
				// a. for checking the validity
				// b. as input for the translator
				generatorInstruction instr = network.get(i).instr_list.get(0);
				infaDataType sqdt = convertDataType( instr , "SOURCE" , network.get(i).sourceTableName , network.get(i).sourceField );
				if( sqdt == null ) return false;
				
				/*
				//infaDataType dt = new infaDataType( xMSet , ""+instr.dbmake , "SOURCEQUALIFIER" , instr.Datatype , instr.Precision , instr.Scale );
				infaDataType dt = new infaDataType( xMSet , ""+instr.dbmake , "SOURCE" , instr.Datatype , instr.Precision , instr.Scale );
			    if( (dt.DataType == null) || (dt.isValid == false) ) {
				    	errit("Cannot create INFA Datatype object");
				    	return false;
			    }
			    // Convert to PowerCenter datatypes for a source qualifier
			    infaDataType sqdt = converter.convertRDBMSToInfa( dt , "SOURCEQUALIFIER" );
			    if( (sqdt == null) || (sqdt.isValid == false) ) {
			    	errit("Cannot transform into INFA Datatype object");
			    	return false;
			    }
			    */
				
			    // update 2de leg
			    if( network.get(i).instr_list.size() < 2 ) continue;
			    if( network.get(i).instr_list.get(1).fromPortTipe != generatorConstants.PORTTYPE.SOURCE_QUALIFIER ) {
			    	errit("(makeNetwork) 2nd leg in instrution chain is not a source qualifier");
			    	return false;
			    }
			    // update source qualifier specifications
			    int zz =1;
			    network.get(i).instr_list.get(zz).Datatype       = sqdt.DataTypeDisplay; 
				network.get(i).instr_list.get(zz).Precision      = sqdt.Precision; 
				network.get(i).instr_list.get(zz).Scale          = sqdt.Scale;
				network.get(i).instr_list.get(zz).Length         = sqdt.Length;
				network.get(i).instr_list.get(zz).PhysicalLength = sqdt.PhysicalLength;
				logit(9,"Datatype " + network.get(i).instr_list.get(zz).Datatype + " " + sqdt.DataTypeDisplay + " " + i +  " " + zz);
			    //
			    sqdt=null;
		}
		return true;
	 }
	

	 // Propagate the data type on the source qualifier (excludign the target)
	 //----------------------------------------------------------------
	 private boolean makeNetWork_PropagateSQDataType()
 	 //----------------------------------------------------------------
	 {
		 for(int i=0; i< network.size();i++)
		 {
				int numberOfInstructions = network.get(i).instr_list.size();
				//boolean first = true;
				for(int k=2;k<(numberOfInstructions-1);k++)  // exclude last one
				{
				  //first = false;
				  int zz = k;
				  network.get(i).instr_list.get(zz).Datatype       = network.get(i).instr_list.get(1).Datatype;
				  network.get(i).instr_list.get(zz).Precision      = network.get(i).instr_list.get(1).Precision;
				  network.get(i).instr_list.get(zz).Scale          = network.get(i).instr_list.get(1).Scale;
				  network.get(i).instr_list.get(zz).Length         = network.get(i).instr_list.get(1).Length;
				  network.get(i).instr_list.get(zz).PhysicalLength = network.get(i).instr_list.get(1).PhysicalLength;
				}
		 }
		 return true;
	 }

	 
	 // propagate the target dataqualifier until the beginning of the instruction chain
	 //----------------------------------------------------------------
	 private boolean makeNetWork_BackPropagateDataType()
 	 //----------------------------------------------------------------
	 {
	 
			// for those transformations without a type (ie. not connected to a sourcefield )
			// use the datatype of the final datatype
			for(int i=0; i< network.size();i++)
			{
			    if( network.get(i).instr_list.get(0).fromPortTipe != generatorConstants.PORTTYPE.SOURCE_QUALIFIER ) continue;
			    //if( network.get(i).instr_list.get(0).toPortTipe != generatorConstants.PORTTYPE.TRANSFORMATION ) continue;
			    if( network.get(i).instr_list.get(0).Datatype != null ) {
				   errit("(makeNetwork) System error 7 - this is an unconnected stream and yet datatype is not null");
				   return false;
			    }
			    int numberOfInstructions = network.get(i).instr_list.size();
			    generatorInstruction p = network.get(i).instr_list.get(numberOfInstructions-1);
			    if( p.toPortTipe != generatorConstants.PORTTYPE.TOEXTERNAL ) {
				   errit("(makeNetwork) System error 8 - last instruction in stream is not a TOEXTERNAL");
				   return false; 
			    }
			    // create a datatype object 
			    // a. for checking the validity
			    // b. as input for the translator
				infaDataType sqdt = convertDataType( p , "TARGET" , network.get(i).sourceTableName , network.get(i).sourceField );
				if( sqdt == null ) return false;
			    
			    /*
			    generatorInstruction instr = p;
				infaDataType dt = new infaDataType( xMSet , ""+instr.dbmake , "TARGET" , instr.Datatype , instr.Precision , instr.Scale );
				if( (dt.DataType == null) || (dt.isValid == false) ) {
			    	errit("Cannot create INFA Datatype object");
			    	return false;
				}
				// Convert to PowerCenter datatypes for a source qualifier
				infaDataType sqdt = converter.convertRDBMSToInfa( dt , "SOURCEQUALIFIER" );
				if( (sqdt == null) || (sqdt.isValid == false) ) {
				   	errit("Cannot transform into INFA Datatype object");
				   	return false;
				}
				// update specs
				String sRet = "TGT [DT:" + dt.DataType + "->" + sqdt.DataTypeDisplay + "]" +
				              "[PR:" + dt.Precision + "->" + sqdt.Precision + "]" +
				  		      "[SC:" + dt.Scale + "->" + sqdt.Scale + "]" +
				              "[LE:" + dt.Length + "->" + sqdt.Length + "]" +
				  		      "[PL:" + dt.PhysicalLength + "->" + sqdt.PhysicalLength + "]";
				logit(9,sRet);
				*/
				
				// update specs
				numberOfInstructions = network.get(i).instr_list.size();
				for(int zz=0;zz<(numberOfInstructions-1);zz++)
				{
			    network.get(i).instr_list.get(zz).Datatype       = sqdt.DataTypeDisplay; 
				network.get(i).instr_list.get(zz).Precision      = sqdt.Precision; 
				network.get(i).instr_list.get(zz).Scale          = sqdt.Scale;
				network.get(i).instr_list.get(zz).Length         = sqdt.Length;
				network.get(i).instr_list.get(zz).PhysicalLength = sqdt.PhysicalLength;
				}
			}
		     return true;
	 }

	 // Quality Checks
	 //----------------------------------------------------------------
	 private boolean makeNetWork_LastPart()
 	 //----------------------------------------------------------------
	 {
		 for(int i=0;i<network.size();i++)
			{
				        network.get(i).rank = 0;
			 			String sSourceField = network.get(i).sourceField;
			 			if( sSourceField == null ) continue;   
			 			if( sSourceField.compareToIgnoreCase("NIL") == 0 ) continue;
			 			if( sSourceField.compareToIgnoreCase("NOP") == 0 ) continue;
			 			for(int j=0;j<i;j++)
			 			{
			 				if( sSourceField.compareToIgnoreCase( network.get(j).sourceField ) == 0 ) network.get(i).rank += 1;
			 			}
			}
			// Dangling expression transformations
			// Verify whether there are EXP transformations without an iput port
			// In reality it suffices to check if there is a instruction hitting the EXP_100
		    int[] exp100Entries = new int[1000];
			int[] exp100EffectiveEntries = new int[1000];
			for(int i=0;i<1000;i++)
			{
				exp100Entries[i]=0;
				exp100EffectiveEntries[i]=0;
			}
			for(int i=0;i<network.size();i++)
			{
			  generatorMapping mp = network.get(i);
			  for(int k=0;k<mp.instr_list.size();k++)
			  {
				  generatorInstruction instr = mp.instr_list.get(k);
				  if( (instr.stage < TRANS_STAGE) || (instr.stage >= TGT_STAGE) ) continue;
				  int idx = instr.stage - TRANS_STAGE;
				  if( idx >= 1000 ) {
					  errit("(makeNetwork) array exceeds 1000 (surprisingly");
					  return false;
				  }
				  exp100Entries[idx] += 1;
				  if( mp.sourceField != null ) exp100EffectiveEntries[idx] += 1;
			  }
			}
			
			
			// check - nergens nog een null datatype
			for(int i=0; i< network.size();i++)
			{
				for(int k=0;k<network.get(i).instr_list.size();k++)
				{
					if( network.get(i).instr_list.get(k).Datatype == null ) {
						if( network.get(i).targetField != null ) {
						  errit("(makeNetwork) - system error 10 - found a NULL datatype on [src=" + network.get(i).sourceField + " / tgt=" + network.get(i).targetField + "]");
					   	  return false;
						}
					}
				}
			}
			
			//
		    return true;
	 }
	
	/*
	 * MAPPING Field [QUANTITY] to [QUANTITY]
        Instr [10] [tipe=NOP] [rval=null] [SOURCE] [SOURCE_QUALIFIER] [ [nstring] [6.0]
        Instr [30] [tipe=PASSTHRU] [rval=null] [SOURCE_QUALIFIER] [TRANSFORMATION] [ [nstring] [6.0]
        Instr [100] [tipe=CONST] [rval='245'] [SOURCE_QUALIFIER] [TRANSFORMATION] [ [nstring] [6.0]
        Instr [101] [tipe=LTRIM] [rval=] [TRANSFORMATION] [TRANSFORMATION] [ [nstring] [6.0]
        Instr [104] [tipe=CRC] [rval=] [TRANSFORMATION] [TARGET] [ [nstring] [6.0]
        Instr [999] [tipe=NOP] [rval=null] [TARGET] [TOEXTERNAL] [ [NVARCHAR] [16.0]

	 */
	
	// scan through the network - detect transformations and make those
	//----------------------------------------------------------------
	private boolean makeTransformations()
	//----------------------------------------------------------------
	{
		trans_list = new ArrayList<infaTransformation>();
	    // create source and source qualifier transformations
		int nCount = stManager.getNbrOfSources();
		for(int i=0;i<nCount;i++)
		{
			// SOURCE
			infaTransformation src_trans = maakSourceTransformation( i ); 
			if( src_trans == null ) return false;
			trans_list.add( src_trans );
			// SQ
			infaTransformation sq_trans = maakSourceQualifierTransformation( i ); 
			if( sq_trans == null ) return false;
			trans_list.add( sq_trans );
		}
	    // create target tranformations	
		nCount = stManager.getNbrOfTargets();
		for(int i=0;i<nCount;i++)
		{
			infaTransformation tgt_trans = maakTargetTransformation( i ); 
			if( tgt_trans == null ) return false;
			trans_list.add( tgt_trans );
		}
	
		
		// determine number of transformations
		int maxlevel = 0;
		for(int i=0;i<network.size();i++)
		{
			for(int k=0;k<network.get(i).instr_list.size();k++)
			{
                //errit(""+network.get(i).instr_list.get(k).stage);
				if( network.get(i).instr_list.get(k).stage >= PKEY_STAGE ) continue;
				if( network.get(i).instr_list.get(k).stage > maxlevel ) maxlevel = network.get(i).instr_list.get(k).stage;
			}
		}
        logit(9,"Highest regular transformation number (the ones below 900) = "+maxlevel);		
	
        // determine the number of transformations
        int NbrOfRegularTransformations = maxlevel - TRANS_STAGE + 1;
        // You now need to add the potential number of KEY - CRC - FKEY - TSTQUAL transformations present on cluster_list
        int loopsToDo = NbrOfRegularTransformations + cluster_list.length;
        
        for(int jj=0;jj<loopsToDo;jj++)    
        {
        	int z = -1;
        	if( jj < NbrOfRegularTransformations) z  = jj + TRANS_STAGE; //  
        	else {
        	  int ndx = jj - NbrOfRegularTransformations;
        	  if( ndx >= cluster_list.length ) {
        		  errit("(maketransformations) System error 10");
          		return false; 
        	  }
        	  switch( ndx )
        	  {
        	  case 0  : { z = PKEY_STAGE; break; }
        	  case 1  : { z = CRC_STAGE;  break; }
        	  case 12 : { z = QUAL_STAGE; break; }
        	  default : { z = FKEY_STAGE + ndx - 2; break; }
        	  }
        	  /*
        	  if( ndx == 0 ) z = PKEY_STAGE;
        	  else
        	  if( ndx == 1 ) z = CRC_STAGE;
        	  else z = FKEY_STAGE + ndx - 2;
        	  */ 
        	}
            
        	//	
	    	infaTransformation gtxn = new infaTransformation( "EXP_TRANS_" + String.format("%03d", z) , infaInstance.TRANSFORMATION_TYPE.EXPRESSION , "" );
        	gtxn.stage = z;  //  z not 1
        	gtxn.Description = "This expresssion implements the instructions of stage [" + z + "]";
            //logit(9,gtxn.Description);
        	//
        	int counter=0;
        	for(int i=0;i<network.size();i++)
        	{
        		generatorMapping mp = network.get(i);
        		for(int k=0;k<mp.instr_list.size();k++)
    			{
        			if( mp.instr_list.get(k).stage != z)  continue;
    				counter++;
    				//
//logit( 5, "IDX=" + i + " " + mp.sourceField + " rank=" + mp.rank + " stage=" + mp.instr_list.get(k).stage);
    				if( makePorts( gtxn , mp ,  k ) == false ) return false;
    				// KEY and CRC
    				/*
    				switch( network.get(i).instr_list.get(k).tipe )
    				{
    				case KEY       : { 
    					if( setKeyColumnName( cKey , gtxn.txnfld_list , network.get(i).instr_list.get(k) ) == false ) return false;
    					gtxn.Description = "This Expression Transformation creates a compound natural key"; 
    					// the folloinw function will not perform anything until the last key has been reached
    					if( addKeyTargetPort( gtxn ) == false ) return false;
    					break; } 
    				case CRC       : { 
    					if( setKeyColumnName( cCrc , gtxn.txnfld_list , network.get(i).instr_list.get(k) ) == false ) return false;
    					gtxn.Description = "This Expression Transformation creates an MD5 Hashkey"; 
    					// the folloinw function will not perform anything until the last key has been reached
    					if( addKeyTargetPort( gtxn  ) == false ) return false;
    					break; } 
    				default : break;
    				}
    				*/
    				int ndx = -1;
    				String sDesc = "This expression creates a single column foreign key";
    				switch( mp.instr_list.get(k).tipe )
    				{
    				case KEY    : { ndx=0; sDesc = "This expression creates a compound natural key"; break; }
    				case CRC    : { ndx=1; sDesc = "This expression creates an MD5 hash key"; break; }
    				case FKEY_0 : { ndx=2; break; }
    				case FKEY_1 : { ndx=3; break; }
    				case FKEY_2 : { ndx=4; break; }
    				case FKEY_3 : { ndx=5; break; }
    				case FKEY_4 : { ndx=6; break; }
    				case FKEY_5 : { ndx=7; break; }
    				case FKEY_6 : { ndx=8; break; }
    				case FKEY_7 : { ndx=9; break; }
    				case FKEY_8 : { ndx=10; break; }
    				case FKEY_9 : { ndx=11; break; }
    				case TSTQUAL: { ndx=12; sDesc = "This expression populates the data quality status field"; break; }
    				default     : break; 
    				}
    				if( ndx >= 0) {
    					if( ndx >= cluster_list.length ) {
    						errit("(makeTransformations) System error 11");
    						return false;
    					}
    					if( cluster_list[ndx] == null ) {
    						errit("(makeTransformations) System error 12 - Null cluster on [" + ndx + "] " + mp.instr_list.get(k).tipe);
    						return false;
    					}
    					// some fancy description stuff
    					sDesc += " [" + cluster_list[ndx].clstrTargetField.FieldName + "]";
    					for(int zz=0;zz<cluster_list[ndx].clstrElementList.size();zz++)
    					{
    						if( zz == 0 ) sDesc += " comprised of [" + cluster_list[ndx].clstrElementList.get(zz).FieldName;
    						else sDesc += "," + cluster_list[ndx].clstrElementList.get(zz).FieldName;
    						if( zz == (cluster_list[ndx].clstrElementList.size() - 1) )  sDesc += "]";
    					}
    					//
    					if( setKeyColumnName( cluster_list[ndx] , gtxn.txnfld_list , mp.instr_list.get(k) ) == false ) return false;
    					gtxn.Description = sDesc; 
    					// the following function will not perform anything until the last key has been reached
    					if( addKeyTargetPort( gtxn ) == false ) return false;
    				}
    			}
         	}
      		if( counter > 0 ) logit(9,"Processing [Loop=" + jj + "] [Stage=" + z  + "] [Ports=" + counter + "]"); 	
      		// Table Attributes
        	infaPair pp = new infaPair("Tracing Level","Normal");
        	gtxn.AttrList.add(pp);
        	//
        	if( gtxn.txnfld_list.size() > 0) trans_list.add( gtxn );
        }
	    //
	    tracer.dumpTransformationTrace(trans_list);
	    //
		return true;
	}
	
	//----------------------------------------------------------------
	private infaTransformation maakSourceTransformation(int sourceTableIdx)
	//----------------------------------------------------------------
	{
		infaSource xSource = localgetInfaSource("maaksourcetrans",sourceTableIdx);
		if( xSource == null ) return null;
		//
		infaTransformation itrans = new infaTransformation( xSource.Name, infaInstance.TRANSFORMATION_TYPE.SOURCE_DEFINITION , xSource.Dbdname );
		itrans.Description = (xSource.flafle == null ) ? "Source Flat File structure" : "Source table structure";	
		itrans.Description += " based on [" + xSource.Dbdname + "." + xSource.Name + "]";
		itrans.Dbdname = xSource.Dbdname;
		itrans.Assoc = null;
		itrans.stage = SRC_STAGE + sourceTableIdx;
		//
		for(int i=0;i<network.size();i++)
		{
		  generatorMapping mp = network.get(i);
		  int zz = 0;
		  if( mp.instr_list.get(zz).fromPortTipe != generatorConstants.PORTTYPE.SOURCE ) continue;
		  if( mp.instr_list.get(zz).stage != itrans.stage ) continue;
		  
		  // The same input source field can be mapped multiple times, however this is only after the SQ
		  // So prevent double occurences
		  boolean alreadyOnList = false;
		  String sCandidate = mp.sourceField;
		  for(int k=0;k<itrans.txnfld_list.size();k++)
		  {
			  if( sCandidate.compareToIgnoreCase( itrans.txnfld_list.get(k).Name ) == 0 ) {
				  alreadyOnList=true;
				  break;
			  }
		  }
		  if( alreadyOnList ) continue;
		  //
		  infaTransformationField fld = new infaTransformationField( mp.sourceField );
		  fld.Datatype       = "N/A";
		  fld.DefaultValue   = "";
		  fld.Description    = "";
		  fld.Expression     = null;
		  fld.ExpressionType = null;
		  fld.PictureText    = "";
		  fld.Porttype       = "N/A";
		  fld.Precision      = -1;
		  fld.Scale          = -1;
		  //
		  itrans.txnfld_list.add( fld );
		}
		if( itrans.txnfld_list.size()==0 ) {
			errit("No source fields found to add to transformation for [" + xSource.Name + "]");
			return null;
		}
		return itrans;
	}
	
	//----------------------------------------------------------------
	private infaTransformation maakTargetTransformation(int targetTableIdx)
	//----------------------------------------------------------------
	{
		infaSource xTarget = localgetInfaTarget("maaktargettransformation",targetTableIdx);
		if( xTarget == null ) return null;
		//
		infaTransformation ptrans = new infaTransformation( xTarget.Name, infaInstance.TRANSFORMATION_TYPE.TARGET_DEFINITION , xTarget.Dbdname );
		ptrans.Description = (xTarget.flafle == null ) ? "Target Flat File structure" : "Target table structure";	
		ptrans.Description += " based on [" + xTarget.Dbdname + "." + xTarget.Name + "]";
		ptrans.Dbdname = xTarget.Dbdname;
		ptrans.Assoc = null;
		ptrans.stage = TGT_STAGE + targetTableIdx;
	    //	
		for(int i=0;i<network.size();i++)
		{
		  generatorMapping mp = network.get(i);
		  int zz = mp.instr_list.size() - 1 ;
		  if( mp.instr_list.get(zz).toPortTipe != generatorConstants.PORTTYPE.TOEXTERNAL ) continue;
		  if( mp.instr_list.get(zz).stage != ptrans.stage ) continue;
		  //
		  infaTransformationField fld = new infaTransformationField( mp.targetField );
		  fld.Datatype       = mp.instr_list.get(zz).Datatype;
		  fld.DefaultValue   = "";
		  fld.Description    = "";
		  fld.Expression     = null;
		  fld.ExpressionType = null;
		  fld.PictureText    = "";
		  fld.Porttype       = "N/A";
		  fld.Precision      = mp.instr_list.get(zz).Precision;
		  fld.Scale          = mp.instr_list.get(zz).Scale;
		  //
		  ptrans.txnfld_list.add( fld );
		}
		if( ptrans.txnfld_list.size()==0 ) {
			errit("No target fields found to add to target transformation for [" + xTarget.Name + "]");
			return null;
		}
		return ptrans;
	}
	
	//----------------------------------------------------------------
	private infaTransformation maakSourceQualifierTransformation( int sourceTableIdx )
	//----------------------------------------------------------------
	{
		infaSource xSource = localgetInfaSource("maaksourcequal",sourceTableIdx);
		if( xSource == null ) return null;
		//
		infaTransformation qtrans = new infaTransformation( "SQ_" + xSource.Name, infaInstance.TRANSFORMATION_TYPE.SOURCE_QUALIFIER , xSource.Dbdname );
		qtrans.Description = "Source Qualifier based on [" + xSource.Dbdname + "." + xSource.Name + "]";
		qtrans.Dbdname = xSource.Dbdname;
		qtrans.Assoc = xSource.Name;
		qtrans.stage = SQ_STAGE + sourceTableIdx;  // SOURCE QUAL to TRANSFORM or TARGET 
		for(int i=0;i<network.size();i++)
		{
		  generatorMapping mp = network.get(i);
		  if( mp.sourceField == null ) continue;
		  //
		  int zz =- 1;
		  for(int k=0;k<mp.instr_list.size();k++)   // ie look for source qualifier fields - zz should be 2 always ..
		  {
			  if( mp.instr_list.get(k).fromPortTipe == generatorConstants.PORTTYPE.SOURCE_QUALIFIER ) {
				  if(  mp.instr_list.get(k).stage == (SQ_STAGE + sourceTableIdx) ) {
					  zz = k;
					  break;
				  }
			  }
		  }
		  if( (zz < 0)  && (mp.sourceField != null) ) {  
			  if( mp.targetField != null ) {
			   errit("(makeTransformation) There is NO instruction that maps onto a SOURCE QUALIIFER in the instruction list for [" + mp.sourceField + "/" + mp.targetField + "]");
			   return null;
			  }
		  }
		  if( zz < 0 ) continue;
		
		  // The same input source field can be mapped multiple times, however this is only after the SQ
		  // So prevent double occurences
		  boolean alreadyOnList = false;
		  String sCandidate = mp.sourceField;
		  for(int k=0;k<qtrans.txnfld_list.size();k++)
		  {
			  if( sCandidate.compareToIgnoreCase( qtrans.txnfld_list.get(k).Name ) == 0 ) {
				  alreadyOnList=true;
				  break;
			  }
		  }
		  if( alreadyOnList ) continue;
		  
		  //
		  infaTransformationField fld = new infaTransformationField( mp.sourceField );
		  fld.Datatype       = mp.instr_list.get(zz).Datatype;
		  fld.DefaultValue   = "";
		  fld.Description    = "";
		  fld.Expression     = null;
		  fld.ExpressionType = null;
		  fld.PictureText    = "";
		  fld.Porttype       = "INPUT/OUTPUT";
		  fld.Precision      = mp.instr_list.get(zz).Precision;
		  fld.Scale          = mp.instr_list.get(zz).Scale;
		  //
		  qtrans.txnfld_list.add( fld );
		}
	
		if( qtrans.txnfld_list.size()==0 ) {
			errit("No fields found to add to Source Qualifier transformation for [" + xSource.Name + "]");
			return null;
		}
		// SQ Transformation Attributes
		// KB 10 SEP
		qtrans.AttrList = override_SQAttributeList();
		return qtrans;
	}
	
	//----------------------------------------------------------------
	private  ArrayList<infaPair> override_SQAttributeList()
	//----------------------------------------------------------------
	{
			
				    ArrayList<infaPair> list = new ArrayList<infaPair>();
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
						  return null;
					  }
					  // look for the SQ code on the first source
					  String sOverrule = stManager.getSQOverrideValue( sCode );
					  if( sOverrule != null ) {
						  logit(5,"SQ Option [" + sCode + "] [" + sVal + "] => [" + sOverrule + "]");
						  sVal = sOverrule;  
					  }
					  infaPair x = new infaPair( sCode , sVal );
					  list.add(x);
					}
					return list;
	}
	
	// fetches the name of the field on powercenter expression that calculates the keys
	//----------------------------------------------------------------
	private boolean setKeyColumnName( FieldCluster xKey , ArrayList<infaTransformationField > txnList , generatorInstruction instr)
	//----------------------------------------------------------------
	{
		// pick the last transformationfield from the list and fetch its FieldName
		// why last one ?  because the last one added is a KEY/CRC/TSTQUAL transformation (see above) 
		if( txnList.size() == 0 ) {
			errit("(setKeyColumnName) System Eror 1");
			return false;
		}
		// fetch the instruction because you will find the TVAL there , ie the index of the key
		infaTransformationField fld = txnList.get( txnList.size() - 1 );
		String sFieldName = fld.Name;
		/*
		int idx = xMSet.xU.NaarInt( instr.rVal );
		if( idx < 0 ) {
			errit("(setKeyColumnName) System Eror 2.  Got an [rval=" + instr.rVal + "]");
			return false;
		}
		*/
		int idx = -1;
		if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.TSTQUAL ) {   //   TSTQUAL( n , <tipe> )
			int pdx = instr.rVal.indexOf(",");
			if( pdx < 0 ) {
				errit("(setKeyColumnName) System Eror 3.  Got an [rval=" + instr.rVal + "]");
				return false;
			}
			idx = xMSet.xU.NaarInt( instr.rVal.substring(0,pdx) );
		}
		else {
			idx = xMSet.xU.NaarInt( instr.rVal );
		}
		if( idx < 0 ) {
			errit("(setKeyColumnName) System Eror 2.  Got an [rval=" + instr.rVal + "]");
			return false;
		}
		
		//errit( "looking for " + idx + " to set " + sFieldName );
		for(int i=0;i<xKey.clstrElementList.size();i++)
		{
			if( xKey.clstrElementList.get(i).rank == idx ) {
				xKey.clstrElementList.get(i).PortName = sFieldName ;
				return true;
			}
		}
		errit( "Could not set Key Field Port [" + sFieldName + "] on Key List with index [" + idx + "]");		
		return false;
	}
	
	// get the OUT port of the preceding transformation
	//----------------------------------------------------------------
	private  infaTransformationField  getPrecedingTransformationViaUID( long iUID )
	//----------------------------------------------------------------
	{
		// inverse : we need the last one
		for(int i=(trans_list.size()-1);i>=0;i--)
		{
	       for(int j=0;j<trans_list.get(i).txnfld_list.size();j++)
	       {
  //logit( 9 , ">" +  trans_list.get(i).Name + " " + trans_list.get(i).stage + " " + trans_list.get(i).txnfld_list.get(j).Name + " " + trans_list.get(i).txnfld_list.get(j).InstructionUID +" " + trans_list.get(i).txnfld_list.get(j).Porttype );
	    	   if( trans_list.get(i).txnfld_list.get(j).InstructionUID < 0L ) continue;
	    	   if( trans_list.get(i).txnfld_list.get(j).Porttype.compareToIgnoreCase("OUTPUT") != 0 ) continue;
	    	   if( trans_list.get(i).txnfld_list.get(j).InstructionUID == iUID ) return trans_list.get(i).txnfld_list.get(j);
	       }
		}
		errit("(getPrecedingTransformationViaUID) Could not find preceding instruction for UID [" + iUID + "]");
		return null;
	}
	
	//----------------------------------------------------------------
	private boolean makePorts(infaTransformation itrans , generatorMapping mp , int instr_idx)
	//----------------------------------------------------------------
	{
		if( (instr_idx<0) || (instr_idx>=mp.instr_list.size()) ) {
			errit("(makePorts° system error");
			return false;
		}
		generatorInstruction instr = mp.instr_list.get(instr_idx);
	    String InPortName = (mp.sourceField != null ) ? "IN_" + mp.sourceField : "IN_" + mp.targetField ;
	    if( mp.rank != 0 ) {
	    	InPortName += "_" + String.format("%02d", mp.rank);
	    }
	    //
	    String portDataType = null;
	    int portPrecision = -1;
	    int portScale =-1;
	    //
	    // If not the first TRANSFORMATION in stage -> get the previous transformation for  this field.
	    // ignore if not a TRANSFORMATION
	    if( (instr.stage > TRANS_STAGE) && (instr.stage < PKEY_STAGE) ) {
	        if( instr_idx <= 0 ) {
	        	errit("(makePorts° system error II - no preceding isntruction");
				return false;
	        }
	        long preceding_UID = mp.instr_list.get(instr_idx-1).UID;
	        infaTransformationField preceding_trans = getPrecedingTransformationViaUID( preceding_UID );
	        if( preceding_trans == null ) {
		    	errit("There is no preceding transformation for [Stage=" + instr.stage + "] [" + InPortName + "] [Instruction=" + instr.tipe + "]" );
		    	return false;
		    }
	        logit( 5 , "Preceding [" + instr.stage + "] [Port=" + preceding_trans.Name + "] [DT=" + preceding_trans.Datatype + "]");
	        //
	        portDataType  = preceding_trans.Datatype;
	 	    portPrecision = preceding_trans.Precision;
	 	    portScale     = preceding_trans.Scale;
	    }
	    else {
	    	portDataType  = instr.Datatype;
	 	    portPrecision = instr.Precision;
	 	    portScale     = instr.Scale;
	    }
        //	    
	    
	    infaTransformationField fld = new infaTransformationField( InPortName );
		fld.Datatype       = portDataType;
		fld.DefaultValue   = "";
		fld.Description    = "";
		fld.Expression     = null;
		fld.ExpressionType = null;
		fld.PictureText    = "";
		fld.Porttype       = "INPUT";
		fld.Precision      = portPrecision;
		fld.Scale          = portScale;
		fld.InstructionUID = instr.UID;
		//
		itrans.txnfld_list.add( fld );
		//
		boolean overruledatatype=false;
		boolean outportIsAvariable=false;
		String expression = null;  // important to set to NULL
		String descript   = null;
		switch( instr.tipe )
		{
		case KEY    : ;
		case FKEY_0 : ;
		case FKEY_1 : ; 
		case FKEY_2 : ; 
		case FKEY_3 : ; 
		case FKEY_4 : ; 
		case FKEY_5 : ; 
		case FKEY_6 : ; 
		case FKEY_7 : ; 
		case FKEY_8 : ; 
		case FKEY_9 : ; 
		case CRC    : return true; // DO nothing
		//
		case CONST     : {
			int zz = itrans.txnfld_list.size() - 1;
			itrans.txnfld_list.get(zz).Name           =  (mp.sourceField != null ) ? "OUT_" + mp.sourceField : "OUT_" + mp.targetField ;   
			itrans.txnfld_list.get(zz).Description    = "Initializing constant value [" + instr.rVal + "]";
			itrans.txnfld_list.get(zz).Porttype       = "OUTPUT";
			itrans.txnfld_list.get(zz).Expression     = instr.rVal;
			itrans.txnfld_list.get(zz).ExpressionType = "GENERAL";
			return true;  // nothing more to do
		}
		case NOW  : ;
		case SYSDATE : {
			int zz = itrans.txnfld_list.size() - 1;
			itrans.txnfld_list.get(zz).Name           =  (mp.sourceField != null ) ? "OUT_" + mp.sourceField : "OUT_" + mp.targetField ;   
			itrans.txnfld_list.get(zz).Description    = "System time";
			itrans.txnfld_list.get(zz).Porttype       = "OUTPUT";
			itrans.txnfld_list.get(zz).Expression     = "SYSTIMESTAMP()";
			itrans.txnfld_list.get(zz).ExpressionType = "GENERAL";
			return true;  // nothing more to do
		}
		//
		case UNKNOWN   : { errit("Got an unknown INSTUCTION TYPE on [" + mp.sourceField + "]"); return false; }
		case STOP      : { descript = "STOPGAP instruction";
                           return true; }  // A stopgap is just an input port
		case NOP       : { descript = "NOP instruction defined in mapping";
			               expression  = InPortName; break; }
		case PASSTHRU  : { descript = "PASSTHRU instruction defined in mapping.";   
			               expression  = InPortName; break; }//  IN_<sourcefield>
		case RTRIM     : { expression = "RTRIM(" + InPortName + ")"; break; } 
		case LTRIM     : { expression = "LTRIM(" + InPortName + ")"; break; }
		case RLTRIM    : { expression = "RTRIM(LTRIM(" + InPortName + "))"; break; } 
		case LOWER     : { expression = "LOWER(" + InPortName + ")"; break; }
		case UPPER     : { expression = "UPPER(" + InPortName + ")"; break; }
		case LEFTSTR   : ; // spill over
		case RIGHTSTR  : {  descript = "" + instr.tipe + " construct";
			                if( instr.rVal.trim().length()  <= 0 ) {
								errit("(makePort) [" + instr.tipe + "] does not comprise a length");
								return false;
							}
							int len = xMSet.xU.NaarInt( instr.rVal.trim() );
							if( len < 0 ) {
								errit("(makePort) [" + instr.tipe + "] invalid length");
								return false;
							}
							if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.RIGHTSTR ) len = 0 - len;
							expression = "SUBSTR(" + InPortName + "," + len + ")";
							break;
						}
		case PREPEND   : ;  // spillover
		case APPEND    : {
							descript = "" + instr.tipe + " construct";
							String pattern = instr.rVal.trim();
							if(  pattern.length() <= 0 ) {
								errit("(makePort) [" + instr.tipe + "] does not comprise the string to pre/append");
								return false;
							}
							if( pattern.startsWith("'") || pattern.endsWith("'") ) {
			 					pattern = xMSet.xU.Remplaceer( pattern , "'" , "" );
			 				}
							if(  pattern.length() <= 0 ) {
								errit("(makePort) [" + instr.tipe + "] attempt to pre/append empty string");
								return false;
							}
							if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.PREPEND ) {
								 expression = "CONCAT('" + pattern + "',LTRIM("+InPortName+"))";
							}
							else {
								 expression = "CONCAT(RTRIM("+InPortName+"),'" + pattern + "')";
							}
							break;
						 }
		case EDWNULL   : { descript = "EDW NULL construct"; 
		                   boolean isString = false;
		                   if( fld.Datatype.toUpperCase().indexOf("CHAR")>=0) isString = true;
		                   if( fld.Datatype.toUpperCase().indexOf("STRING")>=0) isString = true;
		                   if( isString )
			                 expression = "IIF(ISNULL(" + InPortName + "),'NULL',"+InPortName+")";
		                   else
		                     expression = "IIF(ISNULL(" + InPortName + "),'NULL',TO_CHAR("+InPortName+"))";
			               overruledatatype=true;
	        		       break; }
		case CSSDSSNULL   : { descript = "CSSDSS NULL construct"; 
        					boolean isString = false;
        					if( fld.Datatype.toUpperCase().indexOf("CHAR")>=0) isString = true;
        					if( fld.Datatype.toUpperCase().indexOf("STRING")>=0) isString = true;
        					//if( isString )
        					expression = "IIF(ISNULL(" + InPortName + "),'NULL',IIF(rtrim(ltrim("+InPortName+"))='*','NULL',rtrim(ltrim("+InPortName+"))))";
        					//else
        					//	expression = "IIF(ISNULL(" + InPortName + "),'NULL',TO_CHAR("+InPortName+"))";
        					overruledatatype=true;
        					break; }
		case NULLTOZERO : {    descript = "Numeric NULL construct"; 
        				       expression = "IIF(ISNULL(" + InPortName + "),0,"+InPortName+")";
        				       overruledatatype=true;
        				       break; }
		case NULLTOZEROSTR : { descript = "Numeric NULL construct"; 
		                       expression = "IIF(ISNULL(" + InPortName + "),'0',"+InPortName+")"; 
		                       break; }
		case INLINE    : { expression = instr.rVal;
		                   overruledatatype=true;
		                   break; }
		case EDWTIMESTAMPTOSTR : {
			 				if( instr.rVal.trim().length()  <= 0 ) {
			 					errit("(makePort) EDWTIMESTAMPTOSTR [" + instr.tipe + "] does not comprise timestamp pattern");
			 					return false;
			 				}
			 				String pattern = "'" + instr.rVal.trim() + "'";
			 				if( pattern.startsWith("'") || pattern.endsWith("'") ) {
			 					pattern = xMSet.xU.Remplaceer( pattern , "'" , "" );
			 				}
			 				if( pattern.startsWith("\"") || pattern.endsWith("\"") ) {
			 					pattern = xMSet.xU.Remplaceer( pattern , "\"" , "" );
			 				}
			  //errit("-->" + pattern);    
			  				descript = "EDW standard Timestamp to string";
		                    expression = "IIF(ISNULL(" + InPortName + "),'NULL',TO_CHAR(" + InPortName + ",'" + pattern + "'))";
		                    overruledatatype=true;
		        //errit(expression);
			             	break;
		                    }
		//case MAINFRAMEINT : {expression= "CONCAT(IIF(REG_MATCH(" + InPortName + ",'\\d-'),'-',''),LTRIM(RTRIM(CONCAT(REPLACECHR(false," + InPortName + ",'+',NULL),REPLACECHR(false," + InPortName + ",'-',NULL)))))"; break; }
		case EDWSIGN : { expression =
				"IIF(INSTR(" + InPortName + " , '+' ) = 1 , SUBSTR(" + InPortName + ",2) ," +
				"IIF(INSTR(" + InPortName + " , '+' , LENGTH(" + InPortName + ")) = LENGTH(" + InPortName + ") , SUBSTR(" + InPortName + ",1 ,LENGTH(" + InPortName + ")-1)," +
				"IIF(INSTR(" + InPortName + " , '-' , LENGTH(" + InPortName + ")) = LENGTH(" + InPortName + ") , CONCAT( '-' , SUBSTR(" + InPortName + ", 1 , LENGTH(" +InPortName + ") - 1)) , " +
				InPortName + ")))";
	            break; }
		//case MAINFRAMEDECIMAL : { expression = InPortName; errit("MAINFRAMEDEC currenlty set to PASSTHRU"); break; }
		case DQBIGINT    : {  descript = "DQ BigInt transform";
		                      expression =  "IIF(ISNULL(" + InPortName + "),0," +
		                                    "IIF(" + InPortName + "='NULL',0," +
		                                    "IIF(" + InPortName + "='',0," +
		                    		        "IIF(IS_NUMBER(" + InPortName + "),TO_BIGINT("+InPortName+"),0))))";
		                      overruledatatype=true;
		                      break; }
		case DQFLOAT     : { descript = "DQ FLOAT transform";
                             expression =  "IIF(ISNULL(" + InPortName + "),0," +
                                           "IIF(" + InPortName + "='NULL',0," +
                                           "IIF(" + InPortName + "='',0," +
          		                           "IIF(IS_NUMBER(" + InPortName + "),TO_FLOAT("+InPortName+"),0))))";
                             overruledatatype=true;
                             break; }
		case DQINTEGER   : { descript = "DQ Integer transform";
        					 expression =  "IIF(ISNULL(" + InPortName + "),0," +
        							 	   "IIF(" + InPortName + "='NULL',0," +
        							 	   "IIF(" + InPortName + "='',0," +
        							 	   "IIF(IS_NUMBER(" + InPortName + "),TO_INTEGER("+InPortName+"),0))))";
        					 overruledatatype=true;
        					 break; }
		case DQDIVDECIMAL   : {   //  speciaal voor UCHP mainframe
		                     int scale = extractScale(instr.rVal);
		                     if( scale < 0 ) {
		                    	 errit("(makePort) DQDECIMAL [" + instr.tipe + "] does not comprise PRECISION,SCALE");
		                    	 return false;
		                     }
		                     //int divisor = (int)Math.pow( 10 , scale );
		                     long divisor = (long)Math.pow( 10 , scale );   // KB 8 DEC moet een long zjin
		                     
		                     descript = "DQ UCHP Mainframe decimal transform";
		                    expression =  "IIF(ISNULL(" + InPortName + "),0," +
		 							 	   "IIF(" + InPortName + "='NULL',0," +
		 							 	   "IIF(" + InPortName + "='',0," +
		 							 	   "IIF(IS_NUMBER(" + InPortName + "),TO_DECIMAL("+InPortName+")/" + divisor + ",0))))";
		 					 overruledatatype=true;
		 					 break; }
		case DQDECIMAL   : {   //  speciaal voor UCHP mainframe
            				int scale = extractScale(instr.rVal);
            				if( scale < 0 ) {
            					errit("(makePort) DQDECIMAL [" + instr.tipe + "] does not comprise PRECISION,SCALE");
            					return false;
            				}
            				descript = "DQ decimal transform";
            				expression =  "IIF(ISNULL(" + InPortName + "),0," +
            						"IIF(" + InPortName + "='NULL',0," +
            						"IIF(" + InPortName + "='',0," +
            						"IIF(IS_NUMBER(" + InPortName + "),TO_DECIMAL("+InPortName+"),0))))";
            				overruledatatype=true;
            				break; }
		case DQTIMESTAMP : {  expression  = "null";
							  String pattern = instr.rVal.trim();
							  if( pattern.startsWith("'") == false ) pattern = "'" + instr.rVal.trim() + "'";
		                      descript = "DQ TIMESTAMP transform";
			 				  expression =  "IIF(ISNULL(" + InPortName + "), TO_DATE('19000101','YYYYMMDD')," +
			 						 	    "IIF(" + InPortName + "='NULL', TO_DATE('19000101','YYYYMMDD')," +
			 							    "IIF(" + InPortName + "='',TO_DATE('19000101','YYYYMMDD')," +
			 							 	"IIF(IS_DATE(" + InPortName + "," + pattern + "),TO_DATE("+InPortName + "," + pattern + "), TO_DATE('19000101','YYYYMMDD') ))))";
		                      overruledatatype=true;
		                      break; }
		case TSTQUAL  : {  // TSTQUAL (  n , {MANDATROY|INTEGER|FLOAT|BIGINT|DECIMAL(n,m)|TIMESTAMP(pattern)} ) ) 
			String currFieldName = (mp.sourceField != null ) ? mp.sourceField : mp.targetField ;
			descript = "Data Quality Test";
			overruledatatype=true;   // become STRING
			outportIsAvariable=true;
			int po = instr.rVal.indexOf(",");
		    if( po < 0 ) {
		        	 errit("(extractScale) TSTQUAL [" + instr.rVal + "] does not comprise comma TSTQUAL( n , <tipe>)");
		        	 return false;
		    }
		    String right= instr.rVal.substring(  po+1 , instr.rVal.length() ).trim().toUpperCase();
		    boolean isNumber = false;
		    boolean isDate = false;
		    boolean isManda = false;
		    expression = null;
		    if( right.toUpperCase().startsWith("INTEGER")) isNumber=true;
		    if( right.toUpperCase().startsWith("FLOAT")) isNumber = true;
		    if( right.toUpperCase().startsWith("BIGINT")) isNumber = true;
		    if( right.toUpperCase().startsWith("DECIMAL")) isNumber = true;
		    if( right.toUpperCase().startsWith("TIMESTAMP")) isDate= true;
		    if( right.toUpperCase().startsWith("MANDATORY")) isManda= true;
		    //
		    if( isNumber  ) {
	    	   descript = "Data Quality Test - Number";
		       expression =  "IIF(ISNULL(" + InPortName + "),''," +
			  	 	         "IIF(" + InPortName + "='NULL',''," +
				 	         "IIF(rtrim(ltrim(" + InPortName + "))='',''," +
				 	         "IIF(IS_NUMBER(rtrim(ltrim(replacechr(0," + InPortName + ",'+-',NULL)))),'','" + currFieldName.trim().toLowerCase() + "|'))))";
		    }
		    else
		    if( isDate ) {  //  TSTQUAL( n , TIMESTAMP('ccccccc') )
		       descript = "Data Quality Test - Timestamp";
		       String pattern = xMSet.xU.GetVeld( right , 2 , '(' ).trim();
		       if( pattern == null ) pattern = "";
		       if( pattern.length() < 6 ) {  // pattern must be at least 6 YYMMDD
		    	  	 errit("(extractScale) TSTQUAL(TIMESTAMP) [" + instr.rVal + "] unsupported timestamp pattern [" + right + "]");
		    	  	 return false;
		       }
		       if( pattern.endsWith(")") == false ) {
		    	   errit("(extractScale) TSTQUAL(TIMESTAMP) [" + instr.rVal + "] unsupported timestamp pattern [" + right + "] no closing bracket");
		    	   return false;
		       }
		       pattern = pattern.substring(0,pattern.length()-1);
		       // add quotes if no quoates
		       if( pattern.startsWith("'") == false ) pattern = "'" + instr.rVal.trim() + "'";
		       //errit(pattern);
		       expression =  "IIF(ISNULL(" + InPortName + "),''," +
		  	 	             "IIF(" + InPortName + "='NULL',''," +
			 	             "IIF(" + InPortName + "='',''," +
			 	             "IIF(IS_DATE(" + InPortName + "," + pattern + "),'','" + currFieldName.trim().toLowerCase() + "|'))))";
		    }
		    else
		    if( isManda )
		    {
		    	descript = "Data Quality Test - Mandatory";
	    	    expression =  "IIF(ISNULL(" + InPortName + "),'" + currFieldName.trim().toLowerCase() + "|','')";
		    }
		    //
		    if( expression == null ) {
		   	 errit("(makeport) TSTQUAL [" + instr.rVal + "] unsupported type");
        	 return false;
		    }
			break;
		}
		default : {
			errit("(makePort) unhandled transformation type [" + instr.tipe + "]");
			return false;
		  }
		}
		
		// KB 15 OKT
		// the transformation will potentiall lead to a datatypechange -> fetch the datatype of the target
		if( overruledatatype ) {
			// get last instruction in series
			int ldx = mp.instr_list.size()-1;
			if( ldx < 0 ) {
				errit("Cannot find last instruction for [" + mp.sourceTableName + "." + InPortName + "]");
				return false;
			}
			generatorInstruction last_instr = mp.instr_list.get(ldx);
			if( last_instr == null ) {
				errit("Empty last instruction for [" + mp.sourceTableName + "." + InPortName + "]");
				return false;
			}
			infaDataType sqdt = convertDataType( last_instr , "TARGET" , mp.sourceTableName , InPortName);
			if( (sqdt == null) || (sqdt.isValid == false) ) {
				errit("Cannot convert target datatype for [" + mp.sourceTableName + "." + InPortName + "]");
				return false;
			}
			//
			logit( 9 , "Datatype overruled from [" + portDataType + "] -> [" + sqdt.DataTypeDisplay + "(" + sqdt.Precision + "," + sqdt.Scale + ")]");
			//
			portDataType  = sqdt.DataTypeDisplay;
			portPrecision = sqdt.Precision;
			portScale     = sqdt.Scale;
		}
		
		// KB 10 May  NULL treatment  
		// because there are so many 'NULL' references in the above the risk is to o high to break the code
		// therefore a postporcess has been created
		if( xMSet.getEDWNULL().compareTo("NULL") != 0 ) {
			if( expression.indexOf("'NULL'") >= 0 ) {
	            String sPrev = expression;
	            expression = xMSet.xU.Remplaceer(expression,"'NULL'", "'" + xMSet.getEDWNULL() + "'");
	            logit( 9, "[" + sPrev + "] -> [" + expression + "]");
			}
		}
	    //
	    String OutPortName = "OUT_" + InPortName.substring("IN_".length());
		infaTransformationField fld2 = new infaTransformationField( OutPortName );
		fld2.Datatype       = portDataType;
		fld2.DefaultValue   = "";
		fld2.Description    = (descript == null ) ? "" : descript;
		fld2.Expression     = expression;
		fld2.ExpressionType = ( expression == null ) ? null : "GENERAL";
		fld2.PictureText    = "";
		fld2.Porttype       = ( outportIsAvariable == true ) ? "LOCAL VARIABLE" : "OUTPUT";
		fld2.Precision      = portPrecision;
		fld2.Scale          = portScale;
		fld2.InstructionUID = instr.UID;
		//
		itrans.txnfld_list.add( fld2 );
		//
		return true;
	}

	//----------------------------------------------------------------
	private int extractScale(String sIn)
	//----------------------------------------------------------------
	{
		 String detail = sIn.trim();
         if( detail == null ) detail = "";
         int po = detail.indexOf(",");
         if( po < 0 ) {
        	 errit("(extractScale) DQDECIMAL [" + detail + "] does not comprise PRECISION,SCALE");
        	 return -99;
         }
         int scale = -1;
         int precisie = -1;
         try {
          String left = detail.substring( 0 , po );
          String right= detail.substring(  po+1 , detail.length() );
          precisie = xMSet.xU.NaarInt( left );
          scale = xMSet.xU.NaarInt( right );
         }
         catch( Exception e ) {
        	 errit("(extractScale) DQDECIMAL [" + detail + "] does not comprise a valid PRECISION,SCALE");
        	 return -99; 
         }
         if( (precisie<0) || (scale<0) ) {
        	 errit("(extractscale) DQDECIMAL [" + detail + "] does not comprise a valid PRECISION,SCALE");
        	 return -99; 
         }
         return scale;
	}
	
	//----------------------------------------------------------------
	private infaDataType getTargetDataType(generatorMapping mp)
	//----------------------------------------------------------------
	{
	   int idx = mp.instr_list.size() - 1;
	   if( idx < 0 )  return null;
	   generatorInstruction instr = mp.instr_list.get(idx);
	   if( instr == null ) return null;
	   if( instr.toPortTipe != generatorConstants.PORTTYPE.TOEXTERNAL ) {
		   errit("(getTargetDataType) last instruction in sereis is not TOEXTERNAL");
		   return null;
	   }
	   infaDataType dt = new infaDataType( xMSet , ""+instr.dbmake , "TARGET" , instr.Datatype , instr.Precision , instr.Scale );
	   if( dt.DataType == null ) {
		   errit("(getTargetDataType) cannot determine datatype of last inst");
		   return null;
	   }
	   return dt;	
	}
	
	//----------------------------------------------------------------
	private boolean addKeyTargetPort(infaTransformation itrans )
	//----------------------------------------------------------------
	{
		int ndx = -1;
		switch( itrans.stage )
		{
		case PKEY_STAGE : { ndx=0; break; }
		case CRC_STAGE  : { ndx=1; break; }
		case QUAL_STAGE : { ndx=12; break; }
		default : { ndx = itrans.stage - FKEY_STAGE + 2; break; }
		}
	    if( (ndx<0) || (ndx>=cluster_list.length) ) {
	    	errit("(addKeyTargetPort) Unsupported stage [" + itrans.stage + "] [ndx=" + ndx + "]");
			return false;	
	    }
		FieldCluster x = cluster_list[ndx];
		if( x == null ) {
			errit("(addKeyTargetPort) strange there is no cluster field information available on [" + itrans.stage + "]");
			return false;
		}
		// the portaddrequestcounter is used to determine when all INPUT ports have been added
		x.portAddRequestCounter++;
		if( x.portAddRequestCounter != x.clstrElementList.size() ) return true;
		//
		String Expression = null;
		switch( itrans.stage )
		{
		case PKEY_STAGE : { Expression = concatenateKeys( x , false); break; }
		case CRC_STAGE  : { Expression = concatenateKeys( x , true); break; }
		case QUAL_STAGE : { Expression = concatenateKeysPlain( x ); break; }      //  ERROR - special function required
		default         : { Expression = concatenateKeys( x , false); break; }   // concatenate - no MD5
		}
		if( Expression ==  null ) {
			errit("Could not concatenate (keys/crc/..) on [EXP_" + x.stage + "]");
			return false;
		}
		//
		String OutPortName = "OUT_" + x.clstrTargetField.FieldName;
		infaTransformationField fld2 = new infaTransformationField( OutPortName );
		fld2.DefaultValue   = "";
		fld2.Description    = "KEY calculator";
		fld2.Expression     = Expression;
		fld2.ExpressionType = "GENERAL";
		fld2.PictureText    = "";
		fld2.Porttype       = "OUTPUT";
		
		// Quick and Dirty
		fld2.Datatype       = "nstring";
		fld2.Precision      = 255;
		fld2.Scale          = 0;
		fld2.InstructionUID = -1;
		
		// KB 1 Aug 2015
		// get the specs of the target
		infaSource xTarget = stManager.getInfaTarget(x.clstrTargetField.tableIdx );
	    if( xTarget == null ) {
	    	errit("cannot find specs for Target [" + x.clstrTargetField.tableIdx  + "]");
	    	return false;
	    }
	    infaSourceField xTgtFld = getSourceFieldViaName( xTarget , x.clstrTargetField.FieldName );
	    if( xTgtFld == null ) {
	    	errit("cannot find specs for TargetField [" + x.clstrTargetField.tableIdx  + "." + x.clstrTargetField.FieldName + "]");
	    	return false;
	    }
	    // Make a datatype based on the specs of the target cluster field
	    infaDataType dt = new infaDataType( xMSet , xTarget.Databasetype , "TARGET" , xTgtFld.DataType , xTgtFld.Precision , xTgtFld.scale );
		if( (dt.DataType == null) || (dt.isValid == false) ) {
	    	errit("Cannot create INFA Datatype object for [" + x.clstrTargetField.tableIdx  + "." + x.clstrTargetField.FieldName + "]");
	    	return false;
		}
		// Convert to PowerCenter datatypes for a source qualifier
		infaDataType tgtdt = converter.convertRDBMSToInfa( dt , "SOURCEQUALIFIER" );
		if( (tgtdt == null) || (tgtdt.isValid == false) ) {
		   	errit("Cannot create INFA Datatype object for [" + x.clstrTargetField.tableIdx  + "." + x.clstrTargetField.FieldName + "]");
		   	errit("Cannot transform into INFA Datatype object");
		}
		String sRet = "Cluster Target Field [" +  x.clstrTargetField.FieldName + "] [DT:" + dt.DataType + "->" + tgtdt.DataTypeDisplay + "]" +
	              "[PR:" + dt.Precision + "->" + tgtdt.Precision + "]" +
	  		      "[SC:" + dt.Scale + "->" + tgtdt.Scale + "]" +
	              "[LE:" + dt.Length + "->" + tgtdt.Length + "]" +
	  		      "[PL:" + dt.PhysicalLength + "->" + tgtdt.PhysicalLength + "]";
		logit(9,sRet);
	
		// overrule specs
		fld2.Datatype       = tgtdt.DataTypeDisplay;    // KB 26 AUG
		fld2.Precision      = tgtdt.Precision;
		fld2.Scale          = tgtdt.Scale;
		//
		logit(5,"Added the (key/crc/fkey/tstqual..) target column [" + x.clstrTargetField.FieldName + "] on [EXP_" + x.stage + "] [" + fld2.Datatype + " " + fld2.Precision + " "+ fld2.Scale + "]");
		//
		itrans.txnfld_list.add( fld2 );
		//
		return true;
	}
	
	//----------------------------------------------------------------
	private String concatenateKeys( FieldCluster x , boolean addMD5)
	//----------------------------------------------------------------
	{
		String sRet="";
		// KB - 9 MAY
		//String delimiter = "'|#'";
		String delimiter = "'#|'";
		int eCount =0 ;
		for(int i=0 ; i < x.clstrElementList.size(); i++)
		{
			if( x.clstrElementList.get(i).PortName == null ) {
				errit("(concatenateKeys) - System error - displayNameList is NULL");
				return null;
			}
		}
		// INVERSE sort
		for(int i=(x.clstrElementList.size()-1);i>=0;i--)
		{
			if( x.clstrElementList.get(i).PortName == null ) {
				errit("(concatenateKeys) - got a null on portname for [" + x.clstrElementList.get(i).FieldName + "]");
				return null;
			}
			eCount++;
			for(int j=0 ; j< 2 ; j++ )
			{
				switch( j )
				{
				case 0 : {
					if( eCount == 1 ) continue;
					else {
						sRet = "CONCAT(" + delimiter + "," + sRet + ")";
					}
					break;
				    }
				case 1 : {
					if( eCount == 1 ) sRet = "RTRIM(LTRIM(" + x.clstrElementList.get(i).PortName  + "))";
					else {	
					   sRet = "CONCAT(RTRIM(LTRIM(" + x.clstrElementList.get(i).PortName  + "))," + sRet + ")";
					}
				    }
				}
			}
		}
		if( addMD5 ) sRet = "MD5(" + sRet + ")";
		//errit(sRet);
		return sRet;
	}
	
	//----------------------------------------------------------------
	private String concatenateKeysPlain( FieldCluster x )
	//----------------------------------------------------------------
	{
			String sRet="";
			int eCount =0 ;
			for(int i=0 ; i < x.clstrElementList.size(); i++)
			{
				if( x.clstrElementList.get(i).PortName == null ) {
					errit("(concatenateKeysPlain) - System error - displayNameList is NULL");
					return null;
				}
			}
			// INVERSE sort
			for(int i=(x.clstrElementList.size()-1);i>=0;i--)
			{
				if( x.clstrElementList.get(i).PortName == null ) {
					errit("(concatenateKeys) - got a null on portname for [" + x.clstrElementList.get(i).FieldName + "]");
					return null;
				}
				eCount++;
				String sPortName = x.clstrElementList.get(i).PortName.trim();
				if( eCount == 1 ) {
					sRet = sPortName;
				}
				else  {
					sRet = "CONCAT(" + sPortName + "," + sRet + ")";
				}
			}
			return sRet;
	}
	
	//----------------------------------------------------------------
	private boolean write_TransformationBlock()
	//----------------------------------------------------------------
	{
		for(int i=0;i<trans_list.size();i++)
		{
			infaTransformation txn = trans_list.get(i);
			if( txn.TransformationType != infaInstance.TRANSFORMATION_TYPE.EXPRESSION ) continue;
			//
			String sLijn = "<TRANSFORMATION";
		 	sLijn = addToLine( sLijn , "DESCRIPTION"   , txn.Description);
		 	sLijn = addToLine( sLijn , "NAME"          , txn.Name);
		 	sLijn = addToLine( sLijn , "OBJECTVERSION" , "1");
		 	sLijn = addToLine( sLijn , "REUSABLE"      , "NO");
		 	sLijn = addToLine( sLijn , "TYPE"          , ""+txn.TransformationType );
		 	sLijn = addToLine( sLijn , "VERSIONNUMBER" , "1");
		 	sLijn = sLijn + " >";
		 	//
		 	prnt(sLijn);
		 	//
		 	for(int k=0;k<txn.txnfld_list.size();k++)
		 	{
		 		infaTransformationField fd = txn.txnfld_list.get(k);
		 		sLijn = "<TRANSFORMFIELD";
		 		sLijn = addToLine( sLijn , "DATATYPE"      , fd.Datatype);
		 		sLijn = addToLine( sLijn , "DEFAULTVALUE"  , fd.DefaultValue);
		 		sLijn = addToLine( sLijn , "DESCRIPTION"   , fd.Description);
		 		if( fd.Expression != null ) {
		 		sLijn = addToLine( sLijn , "EXPRESSION"    , fd.Expression);
		 		sLijn = addToLine( sLijn , "EXPRESSIONTYPE", fd.ExpressionType);
		 		}
		 		sLijn = addToLine( sLijn , "NAME"   , fd.Name);
		 		sLijn = addToLine( sLijn , "PICTURETEXT"   , fd.PictureText);
		 		sLijn = addToLine( sLijn , "PORTTYPE"      , fd.Porttype);
		 		sLijn = addToLine( sLijn , "PRECISION"     , ""+fd.Precision);
		 		sLijn = addToLine( sLijn , "SCALE"         , ""+fd.Scale);
		 		sLijn = sLijn + " />";
		 		prnt(sLijn);
		 	}
		 	do_table_attributes( txn.AttrList );
		 	//
		 	prnt("</TRANSFORMATION>");
		}
			  
		return true;
	}
	
	//----------------------------------------------------------------
	private infaTransformationField getTransformationFieldViaInstructionUID( long iUID )
	//----------------------------------------------------------------
	{
		for(int i=0;i<trans_list.size();i++)
		{
			for(int k=0 ; k < trans_list.get(i).txnfld_list.size(); k ++)
			{
				if( trans_list.get(i).txnfld_list.get(k).InstructionUID == iUID ) return trans_list.get(i).txnfld_list.get(k);
			}
		}
		return null;
	}
	
	//----------------------------------------------------------------
	private infaTransformation getTransformationViaStageNum( int istag)
	//----------------------------------------------------------------
	{
		for(int i=0;i<trans_list.size();i++)
		{
			if( trans_list.get(i).stage == istag) return trans_list.get(i);
		}
		errit("Cannot locate infaTransformation with stage [" + istag + "]");
		return null;
	}
	
	//----------------------------------------------------------------
	private infaInstance getInstanceViaName( String sName )
	//----------------------------------------------------------------
	{
	  if( sName != null) {
		  for(int i=0;i<instList.size();i++)
		  {
		  if( instList.get(i).InstanceName.compareToIgnoreCase(sName) == 0 ) return instList.get(i);
		  }
	  }
	  errit("Cannot locate infaInstance with Instance Name [" + sName + "]");
	  return null;
	}
	
	//----------------------------------------------------------------
	private boolean create_ConnectBlock()
	//----------------------------------------------------------------
	{
	    connector_list = new ArrayList<infaConnector>();
		for(int i=0;i<network.size();i++)
		{
			generatorMapping mp = network.get(i);
			String sSourceField = (mp.sourceField == null) ? mp.targetField : mp.sourceField;
			//logit(9,"[" + sSourceField + "]");
			for(int k=0 ; k<mp.instr_list.size() ; k++)
			{
				generatorInstruction instr = mp.instr_list.get(k);
				if( instr.toPortTipe == generatorConstants.PORTTYPE.TOEXTERNAL ) continue;
				//
				//errit("Connecting " + mp.sourceField + " tipe=" + instr.fromPortTipe + " " + instr.toPortTipe  + " " + instr.tipe + " " + instr.stage );
				
				// fetch instruction in order to get the INSTANCE and verify the column name
				infaTransformation fromTrans = getTransformationViaStageNum( instr.stage );
				if( fromTrans == null ) return false;
				infaInstance fromInstance = getInstanceViaName( fromTrans.InstanceName );
				if( fromInstance == null ) return false;
				//
				String sFromField = null;
				switch( instr.fromPortTipe) 
				{
				case SOURCE           : {sFromField = mp.sourceField; break; }     
				case SOURCE_QUALIFIER : {
					if( instr.selfTipe == generatorConstants.SELFTYPE.SQ) {
						sFromField = sSourceField;
					}
					else
					if( instr.selfTipe == generatorConstants.SELFTYPE.TRANS) {
						sFromField = "OUT_" + sSourceField;
						if( mp.rank != 0 ) sFromField += "_" + String.format("%02d", mp.rank);
					}
					else {
						errit("(Create connectorBlock) System error 1. selftype=" + instr.fromPortTipe);
						return false;
					}
					break; }
				case TRANSFORMATION   : {
					sFromField = "OUT_" + sSourceField; 
					if( mp.rank != 0 ) sFromField += "_" + String.format("%02d", mp.rank);
					break; 
					}
				//
				case TARGET     : ;
				case TOEXTERNAL : ;
				default : {
					errit("Portype [" + instr.fromPortTipe + "] cannot feature as a from");
					return false;
					}
				}
				
				
				// fetch instruction right side
				if( k >= (mp.instr_list.size() - 1)) { // ie. we are already at the last instuction of the chain
					// This is VALID if  only in the case of a SOURCE -> SOURCE QUALIFIER else error
					if( instr.toPortTipe != generatorConstants.PORTTYPE.SOURCE_QUALIFIER ) {
						logit(9,"SYstem Error - End of chain for [" + mp.sourceField  + "] [From=" + instr.fromPortTipe + "] [To=" + instr.toPortTipe + "]");
						return false;
					}
					logit(9,"OK - End of chain for [" + mp.sourceField  + "] [From=" + instr.fromPortTipe + "] [To=" + instr.toPortTipe + "]");
					break;
				}
				
				int nextStage = mp.instr_list.get(k+1).stage;
				infaTransformation toTrans = getTransformationViaStageNum( nextStage );
				if( toTrans == null ) return false;
				infaInstance toInstance = getInstanceViaName( toTrans.InstanceName );
				if( toInstance == null ) return false;
				//
				String sToField = null;
				switch( instr.toPortTipe) 
				{
				case SOURCE_QUALIFIER : {sToField = sSourceField; break; }
				case TRANSFORMATION   : {
					sToField = "IN_" + sSourceField; 
					if( mp.rank != 0 ) sToField += "_" + String.format("%02d", mp.rank);
					break; }
				case TARGET           : {
					sToField = mp.targetField; 
					if( sToField == null ) {  // valid if stop
						if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.STOP ) {
						    logit( 5 , "OK - null targetfiels on tipe [" + instr.tipe  + "]");
						    continue;
						}
						else {
							errit("(create_ConnectBlock) Got a NULL targetField");
							return false;
						}
					}
					// if this is a KEY/CRC/FKEY : TRANSFORMATION+TARGET combination then replace IN_x by OUT_<targetfield>
					// and count on the double checking routine to remove duplicates
					int ndx = -1;
					switch( instr.tipe )
					{
					case KEY    : { ndx=0; break; }
					case CRC    : { ndx=1; break; }
					case FKEY_0 : { ndx=2; break; }
					case FKEY_1 : { ndx=3; break; }
					case FKEY_2 : { ndx=4; break; }
					case FKEY_3 : { ndx=5; break; }
					case FKEY_4 : { ndx=6; break; }
					case FKEY_5 : { ndx=7; break; }
					case FKEY_6 : { ndx=8; break; }
					case FKEY_7 : { ndx=9; break; }
					case FKEY_8 : { ndx=10; break; }
					case FKEY_9 : { ndx=11; break; }
					case TSTQUAL: { ndx=12; break; }
					default : break;
					}
					if( ndx >= 0 ) {
						if( ndx >= cluster_list.length) {
							errit("(create_ConnectBlock) Got an overflow on cluster_list [" + ndx + "]");
							return false;
						}
						if( cluster_list[ndx] == null ) {
							errit("(create_ConnectBlock) Got NULL entry on cluster_list [" + ndx + "]");
							return false;
						}
						logit(5,"Got a [" + instr.tipe + "] mapping [" + sFromField + "->" + sToField + "] - remapped to [" + "OUT_" + cluster_list[ndx].clstrTargetField.FieldName + "->" + sToField + "]");
						// relocate source
						sFromField = "OUT_" + cluster_list[ndx].clstrTargetField.FieldName;
					}
					/*
					// if this is a KEY : TRANSFORMATION TARGET combination then replace IN bY OUT_keytargetfield
					if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.KEY ) {
						logit(5,"Got a KEY mapping [" + sFromField + "->" + sToField + "] - remapped to [" + "OUT_" + cKey.clstrTargetField.FieldName + "->" + sToField + "]");
						// relocate source
						sFromField = "OUT_" + cKey.clstrTargetField.FieldName;
					}
					// Ditto for the CRC
					if( instr.tipe == generatorConstants.INSTRUCTION_TYPE.CRC ) {
						logit(5,"Got a CRC mapping [" + sFromField + "->" + sToField + "] - remapped to [" + "OUT_" + cCrc.clstrTargetField.FieldName + "->" + sToField + "]");
						// relocate source
						sFromField = "OUT_" + cCrc.clstrTargetField.FieldName;
					}
					*/
				    break; }
		        //		
				case SOURCE  : ;
				case TOEXTERNAL : ;
				default : {
					errit("Portype [" + instr.toPortTipe + "] cannot feature as a TO field");
					return false;
					}
				}
				// if target is a constant / NOW / sydate
				if( instr.selfTipe == generatorConstants.SELFTYPE.SQ ) {
					if( (mp.instr_list.get(k+1).tipe == generatorConstants.INSTRUCTION_TYPE.CONST) ||
						(mp.instr_list.get(k+1).tipe == generatorConstants.INSTRUCTION_TYPE.NOW) ||
						(mp.instr_list.get(k+1).tipe == generatorConstants.INSTRUCTION_TYPE.SYSDATE) ) {
						logit(9,"Skipping the creation of SQ link to " + mp.instr_list.get(k+1).tipe  + " from [" + fromInstance.InstanceName + "." + sToField +  "] to [" + toInstance.InstanceName + "." + sToField + "]");
						continue;
					}
				}
				//
				infaConnector p = new infaConnector( sFromField , fromInstance.InstanceName , fromInstance.Transformation_Type ,
						                             sToField , toInstance.InstanceName , toInstance.Transformation_Type , fromInstance.stage );
				//
				// Caution SOURCE->SOURCE QUALIFIER .  It is valid that the same connection occurs more than once
				// i.e.    A -> SQ -> something  and A -> SQ -> KEY(n) -> something
			    // detect these cases and skip
				if( (instr.fromPortTipe == generatorConstants.PORTTYPE.SOURCE) && (instr.toPortTipe == generatorConstants.PORTTYPE.SOURCE_QUALIFIER) ) {
					boolean ia = AlreadyOnConnList( p );
					if( ia == true ) {
						String sL = "Already on list [" + p.FromInstanceName + "." + p.FromFieldName + " - >" + p.toInstanceName + "." + p.toFieldName + "] ==> skipping";
						logit(5,sL);
						continue;
					}
				}
				// In the case of KEY, CRC and FKEY the same TARGET field can be mapped against more than once in the code network
				// if this situation is encountered we simply skip the 2nd occurence creation of a connection
				if( instr.toPortTipe == generatorConstants.PORTTYPE.TARGET ) {
					if( AlreadyOnConnList( p ) ) {
						String acceptableTargetFieldName = "?";
						/*
						if( (cKey != null) && (instr.tipe == generatorConstants.INSTRUCTION_TYPE.KEY) ) {
							acceptableTargetFieldName = "OUT_" + cKey.clstrTargetField.FieldName;
						}
						if( (cCrc != null) && (instr.tipe == generatorConstants.INSTRUCTION_TYPE.CRC) ) {
							acceptableTargetFieldName = "OUT_" + cCrc.clstrTargetField.FieldName;
						}
						*/
						int ndx = -1;
						switch( instr.tipe )
						{
						case KEY    : { ndx=0; break; }
						case CRC    : { ndx=1; break; }
						case FKEY_0 : { ndx=2; break; }
						case FKEY_1 : { ndx=3; break; }
						case FKEY_2 : { ndx=4; break; }
						case FKEY_3 : { ndx=5; break; }
						case FKEY_4 : { ndx=6; break; }
						case FKEY_5 : { ndx=7; break; }
						case FKEY_6 : { ndx=8; break; }
						case FKEY_7 : { ndx=9; break; }
						case FKEY_8 : { ndx=10; break; }
						case FKEY_9 : { ndx=11; break; }
						case TSTQUAL: { ndx=12; break; }
						default : break;
						}
						if( ndx >= 0 ) {
							if( ndx >= cluster_list.length ) {
								errit("(create_ConnectBLock) strange an overrun on NDX [" + ndx + "]");
								return false;
							}
							if( cluster_list[ndx] == null ) {
								errit("(create_ConnectBLock) strange a NULL FieldCluster on NDX [" + ndx + "]");
								return false;
							}
							acceptableTargetFieldName = "OUT_" + cluster_list[ndx].clstrTargetField.FieldName;
						}
						//
						if( p.FromFieldName.compareToIgnoreCase(acceptableTargetFieldName) == 0 ) {
							String sL = "Already on list [" + p.FromInstanceName + "." + p.FromFieldName + " - >" + p.toInstanceName + "." + p.toFieldName + "] ==> OK this is a key/crc/fkey field";
							logit(5,sL);
						    continue;
						}
						else {
							String sL = "Already on list [" + p.FromInstanceName + "." + p.FromFieldName + " - >" + p.toInstanceName + "." + p.toFieldName + "] ==> NOT VALID";
							errit(sL);
						    return false;	
						}
					}
				}
				connector_list.add(p);
				//tracer.dumpConnector(p);
			}
		}
		//
		if( checkConnectors() == false ) return false;
	    //
		writeConnectorOut();
		// only now the connectors are sorted
		tracer.dumpAllConnectors(connector_list);  
		//
		return true;
		
	}
	
	//----------------------------------------------------------------
	private boolean AlreadyOnConnList(infaConnector p)
	//----------------------------------------------------------------
	{
		for(int i=0;i<connector_list.size();i++)
		{
			infaConnector x = connector_list.get(i);
			//
			if( x.FromInstanceName.compareToIgnoreCase(p.FromInstanceName) != 0 ) continue;
			if( x.FromFieldName.compareToIgnoreCase(p.FromFieldName) != 0 ) continue;
			if( x.fromTransformationType != p.fromTransformationType ) continue;
			//
			if( x.toInstanceName.compareToIgnoreCase(p.toInstanceName) != 0 ) continue;
			if( x.toFieldName.compareToIgnoreCase(p.toFieldName) != 0 ) continue;
			if( x.toTransformationType != p.toTransformationType ) continue;
		    //
			return true;
		}
		return false;
	}
	
	//----------------------------------------------------------------
	private boolean checkConnectors()
	//----------------------------------------------------------------
	{
	    // Target field mapped more thanonce ?
		int hits=0;
	    ArrayList<String> turfList = new ArrayList<String>();
	    ArrayList<String> tArfList = new ArrayList<String>();
	   for(int i=0;i<connector_list.size();i++)
		{
			infaConnector x = connector_list.get(i);
			String sSour = (x.FromInstanceName + "." + x.fromTransformationType + "." + x.FromFieldName).trim().toUpperCase();
			String sTarg = (x.toInstanceName + "." + x.toTransformationType + "." + x.toFieldName).trim().toUpperCase();
			boolean found = false;
			for(int k=0;k<turfList.size();k++)
			{
				if( sTarg.compareToIgnoreCase( turfList.get(k) ) == 0 ) {
					 String sLijn = "Target [" + sTarg + "] features more than once as an endpoint of a connector. From :" 
				        + "[" + tArfList.get(k) + "] [" + sSour +"]";
					 errit( sLijn );
					 tracer.trace( sLijn );
					 found = true;
				}
			}
			if( found ) {
				hits++;
				continue;
			}
			turfList.add(sTarg);
			tArfList.add(sSour);
		}
		if( hits > 0 ) return false;
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean writeConnectorOut()
	//----------------------------------------------------------------
	{
		    // sort
		    int nCount = connector_list.size();
		    for(int k=0;k<nCount;k++)
		    {
		    	for(int i=0;i<(nCount-1);i++)
		    	{
		    		String sOne = String.format("%05d", connector_list.get(i).stage) + "-" + connector_list.get(i).FromFieldName;
		    		String sTwo = String.format("%05d", connector_list.get(i+1).stage) + "-" + connector_list.get(i+1).FromFieldName;
		    		if( sOne.compareToIgnoreCase( sTwo ) <= 0 ) continue;
		    		//
		    		String sFromIns = connector_list.get(i).FromInstanceName;
		    		String sFromFld = connector_list.get(i).FromFieldName;
		    		infaInstance.TRANSFORMATION_TYPE tpFrom =  connector_list.get(i).fromTransformationType;
		    		String sToIns = connector_list.get(i).toInstanceName;
		    		String sToFld = connector_list.get(i).toFieldName;
		    		infaInstance.TRANSFORMATION_TYPE tpTo =  connector_list.get(i).toTransformationType;
		    		int istage = connector_list.get(i).stage;
		    		//
		    		connector_list.get(i).FromInstanceName = connector_list.get(i+1).FromInstanceName;
		    		connector_list.get(i).FromFieldName = connector_list.get(i+1).FromFieldName;
		    		connector_list.get(i).fromTransformationType = connector_list.get(i+1).fromTransformationType;
		    		connector_list.get(i).toInstanceName = connector_list.get(i+1).toInstanceName;
		    		connector_list.get(i).toFieldName = connector_list.get(i+1).toFieldName;
		    		connector_list.get(i).toTransformationType = connector_list.get(i+1).toTransformationType;
		    		connector_list.get(i).stage = connector_list.get(i+1).stage;
		    		//
		    		connector_list.get(i+1).FromInstanceName = sFromIns;
		    		connector_list.get(i+1).FromFieldName = sFromFld;
		    		connector_list.get(i+1).fromTransformationType  =tpFrom;
		    		connector_list.get(i+1).toInstanceName = sToIns;
		    		connector_list.get(i+1).toFieldName = sToFld;
		    		connector_list.get(i+1).toTransformationType  = tpTo;
		    		connector_list.get(i+1).stage = istage;
		    		
		    	}
		    }
		    int prevstg=-9;
			for(int i=0;i<connector_list.size();i++)
			{
				infaConnector x = connector_list.get(i);
				if( prevstg != x.stage) {
					prevstg = x.stage;
					prnt("<!-- " + x.FromInstanceName + "-->");
				}
				String sRet = "";
				sRet = "<CONNECTOR" +
				       " FROMFIELD =\""        + x.FromFieldName + "\"" +
				       " FROMINSTANCE =\""     + x.FromInstanceName + "\"" +
				       " FROMINSTANCETYPE =\"" + TTFormat(x.fromTransformationType) + "\"" + // strange
				       " TOFIELD =\""          + x.toFieldName + "\"" +
				       " TOINSTANCE =\""       + x.toInstanceName + "\"" +
				       " TOINSTANCETYPE =\""   + TTFormat(x.toTransformationType) + "\"/>";
		        prnt( sRet );				
			}
			return true;
	}
	
	
	
}
