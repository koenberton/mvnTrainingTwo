package pcGenerator.generator;

import generalpurpose.pcDevBoosterSettings;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import pcGenerator.powercenter.infaPair;

public class generatorMapSpecReader {

	// 09SEP added support for OPTIONS
	
	pcDevBoosterSettings xMSet = null;
	generatorConstants   xConst = null;
	ArrayList<generatorMapping> list = null;
	//
	ArrayList<String> sourceTableList = null;
	ArrayList<String> targetTableList = null;
	ArrayList<String> flatfile_list = null;
	
	private String overruleMappingName = null;
	ArrayList<infaPair> SQLOverrideList = null;
	
	ArrayList<generatorOption> option_list = null;
	
	//----------------------------------------------------------------
	generatorMapSpecReader(pcDevBoosterSettings im)
	//----------------------------------------------------------------
	{
		xMSet = im;
		xConst = new generatorConstants(xMSet);
		list = new ArrayList<generatorMapping>();
		sourceTableList = new ArrayList<String>();
		targetTableList = new ArrayList<String>();
		flatfile_list = new ArrayList<String>();
		option_list = new ArrayList<generatorOption>();
		SQLOverrideList = new ArrayList<infaPair>();
	}
	
	//----------------------------------------------------------------
	public ArrayList<generatorMapping> ReadTheMapFile(String sFileNameIn , String SourceTableNameList , String TargetTableNameList )
	//----------------------------------------------------------------
	{
		String ShortMappingFileName = sFileNameIn.trim();
    	String LongMappingFileName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Templates" + xMSet.xU.ctSlash + ShortMappingFileName;
    	if( xMSet.xU.IsBestand( LongMappingFileName ) == false ) {
    		 errit("Cannot find mapping specification file [" + LongMappingFileName + "] defined after IMPORT");
    		 return null;
    	}
        //
		ArrayList<generatorMapping> mapList = this.readSpecs(  ShortMappingFileName , SourceTableNameList , TargetTableNameList );
 		if( mapList == null) return null;
 		if( mapList.size() == 0 ) {
 			errit("There is nothing to map ?");
 			return null;
 		}
 		return mapList;
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
	private void putTableOnStack(String sIn , boolean isSource )
	//----------------------------------------------------------------
	{
		StringTokenizer st = new StringTokenizer(sIn, ",()");
		while( st.hasMoreTokens() )
		{
			String sTab = st.nextToken();
			if( isSource ) sourceTableList.add( sTab );
			          else targetTableList.add( sTab);
		}
	}
	//----------------------------------------------------------------
	public ArrayList<String> getFlatFileList()
	//----------------------------------------------------------------
	{
		return flatfile_list;
	}
	
	
	//----------------------------------------------------------------
	public ArrayList<generatorMapping> readSpecs( String FName , String FromNameList , String ToNameList)
	//----------------------------------------------------------------
	{
		
		if( FName == null ) {
			errit("(do_connect_import) NULL mapfile");
			return null;
		}
		String LongFileName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Templates" + xMSet.xU.ctSlash + FName;
		if( xMSet.xU.IsBestand( LongFileName ) == false ) {
			errit("(do_connect_import) Cannot locate mapping specification file [" + LongFileName + "]");
			return null;
		}
	    logit( 1 , "Locating mapping [" + FromNameList + "] to [" + ToNameList + "] in [" + LongFileName  + "]");
	    if( FromNameList == null ) {
	    	errit("(do_connect_import) FromNameList is NULL");
			return null;
	    }
	    if( ToNameList == null ) {
	    	errit("(do_connect_import) ToNameList is NULL");
			return null;
	    }
	    if( FromNameList.trim().length() == 0) {
	    	errit("(do_connect_import) FromNameList is empty");
			return null;
	    }
	    if( ToNameList.trim().length() == 0) {
	    	errit("(do_connect_import) TONameList is empty");
			return null;
	    }
		//
	    // Synthax is  ( table , table )  however is there is only 1 table brackets can be removed
	    //
	    String sFrom = xMSet.xU.removeBelowIncludingSpaces(FromNameList).trim().toUpperCase();
	    if( sFrom.startsWith("(") == false ) sFrom = "(" + sFrom;
	    if( sFrom.endsWith(")") == false ) sFrom = sFrom + ")";
	    putTableOnStack( sFrom , true );
	    if( sFrom.indexOf(",") < 0 ) {
	    	sFrom = sFrom.substring(1,sFrom.length()-1);
	    }
	    //
	    String sTo   = xMSet.xU.removeBelowIncludingSpaces(ToNameList).trim().toUpperCase();
	    if( sTo.startsWith("(") == false ) sTo = "(" + sTo;
	    if( sTo.endsWith(")") == false ) sTo = sTo + ")";
	    putTableOnStack( sTo , false );
	    if( sTo.indexOf(",") < 0 ) {
	    	sTo = sTo.substring(1,sTo.length()-1);
	    }
	    logit(9,FromNameList + "->" + sFrom );
	    logit(9,ToNameList + "->" + sTo );
	    //
	    String sLookFor = ("MAP:" + sFrom.trim() + "TO" + sTo.trim()).trim().toUpperCase();
	    String ENCODING = xMSet.xU.getEncoding(LongFileName);
		try {
			  int linesRead=0;
			  BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(LongFileName),ENCODING));
			  String sLijn = null;
			  boolean inPayLoad=false;
			  boolean inSQLOverride=false;
			  String sSQLOverride=null;
			  String sOption=null;
			  boolean inOption=false;
	       	  while ((sLijn=reader.readLine()) != null) {
	       		 linesRead++;
	       		 String sClean = sLijn.trim();
	       		 if( sClean.length() <= 0 ) continue;
	       		 if( sClean.startsWith("--")) continue;
	       		 if( sClean.startsWith("//")) continue;
	       		
	       		 // MAP:<left>TO<right> 
	       		 String sCompress = xMSet.xU.removeBelowIncludingSpaces(sClean).trim().toUpperCase();
	             if( sCompress.compareToIgnoreCase(sLookFor)==0) {
	            	 inPayLoad=true;
	            	 continue;
	             }
	             // KB 28 AUG 
	             if( inSQLOverride ) {   // continue reading until closing delimiter }
	            	 // detect overruns
	            	 boolean overrun=false;
	            	 if( sClean.indexOf("{") >= 0 ) overrun=true;
	            	 if( sClean.startsWith("MAP:")) overrun=true;
	            	 if( overrun ) {
	            		 errit("SQLOverride probably not terminated correctly [" + sSQLOverride + "]");
	            		 reader.close();
                		 return null;
	            	 }
	            	 sSQLOverride += xMSet.xU.ctEOL + sLijn;
	            	 if( sClean.endsWith("}")) {
	            		 inSQLOverride=false;
	            		 if( do_SQLOverride(sSQLOverride) == false ) {
	            			 reader.close();
	                		 return null;
	            		 }
	            	 }
	            	 continue; 
	             }
	             // KB 09 SEP
	             if( inOption ) {   // continue reading until closing delimiter }
	            	 // detect overruns
	            	 boolean overrun=false;
	            	 if( sClean.indexOf("{") >= 0 ) overrun=true;
	            	 if( sClean.startsWith("MAP:")) overrun=true;
	            	 if( overrun ) {
	            		 errit("SOURCE/TARGETOPTION probably not terminated correctly [" + sOption + "]");
	            		 reader.close();
                		 return null;
	            	 }
	            	 sOption += xMSet.xU.ctEOL + sLijn;
	            	 if( sClean.endsWith("}")) {
	            		 inOption=false;
	            		 if( do_option(sOption) == false ) {
	            			 reader.close();
	                		 return null;
	            		 }
	            	 }
	            	 continue; 
	             }
	             if( inPayLoad ) {
	            	 if( sCompress.startsWith("MAP:")) {
	            		  break;  // einde verwerking
	            	 }
	            	 else {
	            		 // FLATFILES: x,y,z
	                     if( sCompress.toUpperCase().startsWith("FLATFILES:") ) {
	                    	 if( extract_flatfile_list(sCompress.substring("FLATFILES:".length())) == false ) {
	                    		 reader.close();
	                    		 return null;
	                    	 }
	                    	 continue;
	                     }
	                     else   // KB 26 aug
	                     if( sCompress.toUpperCase().startsWith("MAPPINGNAME:") ) {
	                    	 int idx = sClean.indexOf(":");
	                    	 if( idx >= (sClean.length()-8) ) {
	                    		 errit("Mappingname is too short [" + sClean + "]");
	                    		 reader.close();
	                    		 return null;   
	                    	 }
	                    	 String sx = sClean.substring(idx+1).trim();
	                    	 logit(1,"Mappingname requested to be [" + sx + "]");
	                    	 this.overruleMappingName = sx.trim();
	                    	 continue;
	                     }
	                     /*
	                     else  // KB 28 AUG 
                    	 if( sCompress.toUpperCase().startsWith("SQLOVERRIDE:") ) {
                    		   sSQLOverride = sLijn;
                    		   if( sClean.endsWith("}") ) {
                    			   if( do_SQLOverride(sSQLOverride) == false ) {
          	            			 reader.close();
          	                		 return null;
          	            		   }
                    		   }
                    		   else inSQLOverride=true;
                    		   continue;
		                 }
		                 */
                    	 else // KB 09 SEP
                    	 if( (sCompress.toUpperCase().startsWith("SOURCEOPTION:")) ||
                    		 (sCompress.toUpperCase().startsWith("TARGETOPTION:")) ||
                    		 (sCompress.toUpperCase().startsWith("SOURCEQUALIFIEROPTION:")) ||
                    		 (sCompress.toUpperCase().startsWith("SRCQUALOPTION:")) ||
                    	 	 (sCompress.toUpperCase().startsWith("SOURCCEQUALOPTION:")) ) {
                      		   sOption = sLijn;
                      		   if( sClean.endsWith("}") ) {
                      			   if( do_option(sOption) == false ) {
            	            			 reader.close();
            	                		 return null;
            	            		   }
                      		   }
                      		   else inOption=true;
                      		   continue;
  		                 }
	                     else {  // localiseer verkeerde instructies
	                       if( sCompress.contains(":") ) {
	                    	   errit("Found an unsupported instruction on [" + sClean + "] [line=" + linesRead + "]");
	                    	   reader.close();
	                    	   return null;
	                       }
	                     }
	                     generatorMapping mp =  parsePayloadLine( sClean , linesRead );
	            		 if( mp == null ) {
	            			 reader.close();
	            			 return null;
	            		 }
	            		 // ignore
	            		 String sLeft  = mp.sourceField;
	            		 String sRight = mp.targetField;
	            		 if( sLeft.compareToIgnoreCase("?") == 0 ) continue;
	            		 if( sLeft.compareToIgnoreCase("N/A") == 0 ) continue;
	            		 if( sRight.compareToIgnoreCase("?") == 0 ) continue;
	            		 if( sRight.compareToIgnoreCase("N/A") == 0 ) continue;
	            		 //
	            		 //infaPair x = new infaPair(sLeft,sRight);
	            		 list.add( mp );
	            	 }
	             }
	       	  }	 // end while
	       	  reader.close();
	       	  if( list.size() == 0 ) {
	       		  errit("Could not find mapping info in [" + LongFileName + "] for [" + FromNameList + "] to [" + ToNameList + "]");
	       		  return null;
	       	  }
	       	  // ok
	       	  return list;
		}
		catch(Exception e) {
			errit("Error reading [" + LongFileName + "]" + xMSet.xU.LogStackTrace(e));
			return null;
		}
	}
	
	//----------------------------------------------------------------
	private String expandSemicolonAccolades(String sIn)
	//----------------------------------------------------------------
	{
		  String sRet = "";
   	      char[] buf = sIn.toCharArray();
	      for(int i=0;i<buf.length;i++)
	      {
	    	  boolean found = false;
	    	  if( buf[i] == (char)';' ) found = true;
	    	  if( buf[i] == (char)'{' ) found = true;
	    	  if( buf[i] == (char)'}' ) found = true;
	    	  if( buf[i] == (char)'(' ) found = true;
	    	  if( buf[i] == (char)')' ) found = true;
	    	  if( found ) sRet += " " + buf[i] + " ";
	    	         else sRet += buf[i]; 
	      }
	      return sRet;
	}
	
	//----------------------------------------------------------------
	private String removeDoubleQuotes(String sIn)
	//----------------------------------------------------------------
	{
			  String sRet = "";
	   	      char[] buf = sIn.toCharArray();
		      for(int i=0;i<buf.length;i++)
		      {
		    	  if( buf[i] == (char)'"' ) {
		    		  if( i>0 ) {
		    			  if( buf[i-1] != '\\' ) continue;
		    		  }
		    		  else continue;
		    	  }
		    	  sRet += buf[i]; 
		      }
		      return sRet;
	}
		
	// <Field|nill|N/A>  { INSTRUCTION* } <ToField>   (or instruction trailing )
    // <table>.<column>  <table>.<column> { instructions }
    // <table>.<column>  { instructions } <table>.<column>
	//----------------------------------------------------------------
	private generatorMapping parsePayloadLine(String sLine , int linesRead)
	//----------------------------------------------------------------
	{
		String sExpand = expandSemicolonAccolades( sLine );
		sExpand = removeDoubleQuotes( sExpand );
		StringTokenizer st = new StringTokenizer(sExpand, " \t");
		int loc=0;
		String sLeft = null;
		String sRight = null;
		String sPrevToken = "???";
		ArrayList<String> list = new ArrayList<String>();
		//  A B instructions
		//  A instructions B
		//  A B
		boolean InstructionsInBetween = true;
		
		if( st.hasMoreTokens() == false ) return null;  // impossible
		sLeft = st.nextToken();
		if( st.hasMoreTokens() == false ) {
			errit("There is only one column on line [" + sLine + "]");
			return null;
		}
		String sTwo  = st.nextToken();
		if( sTwo.startsWith("{") ) InstructionsInBetween = true; else InstructionsInBetween = false;
		if( InstructionsInBetween )
		{
		  loc = 2;
		  while(st.hasMoreTokens()) 
		  {
			String sToken = st.nextToken();
			loc++;
			//logit(5,sToken);
			if( st.hasMoreTokens() == false ) {
			    sRight = sToken;
				if( loc != 2 )  {  // line has instruction
					if( sPrevToken.compareToIgnoreCase("}") != 0 ) {
						errit("Missing right accolade in [" + sLine + "] " + sPrevToken);
						return null;
					}
				}
				break;
			}
		    if( sToken.compareToIgnoreCase("}") != 0) list.add(sToken);
		    sPrevToken = sToken;
		  }
		}
		else {
		  sRight = sTwo;
		  if( st.hasMoreTokens() == true ) {
			  String sThree = st.nextToken();  // must be {
			  if( sThree.startsWith("{") == false ) {
				  errit("<col> <col> {instruction} expected. Left accolade is missing on [" + sLine + "] at [" + sThree + "]");
				  return null;
			  }
			  // read until end
			  while(st.hasMoreTokens()) 
			  {
				String sToken = st.nextToken().trim();
				String sTk = euh(sToken);    // Excel put ASCII 160 at end of line
				//errit("TOKEN [" + sToken + "] [" + sTk + "]");
				if( sTk.length() > 0 )	list.add(sToken);  
			  }	
			  int lidx = list.size()-1;
			  if( list.get(lidx).startsWith("}") == false ) {
				  errit("<col> <col> {instruction} expected. Right accolade is missing [" + sLine + "] at [" + list.get(lidx) + "]" );
				  return null;
			  }
			  list.remove(lidx);
		  }
		}
//logit(9, "mapline : [" + sLeft + "] [" + sRight + "] [" + list.toString() + "] " + InstructionsInBetween);
		// sleft , sRight   
		//   <tablename.<fieldname>
		//   nn.<fieldname>
		//   <fieldname>
		int sourceTableIndex = getTableIndex( sLeft , true);
		if( sourceTableIndex < 0 ) return null;
		int targetTableIndex = getTableIndex( sRight , false);
		if( targetTableIndex < 0 ) return null;
		String sLeftShort  = getShort(sLeft);
		String sRightShort = getShort(sRight);
	    generatorMapping mp = new generatorMapping( sourceTableIndex , sourceTableList.get(sourceTableIndex) , sLeftShort ,
	    		                                    targetTableIndex , targetTableList.get(targetTableIndex) , sRightShort );
	    //
	    mp.lineno = linesRead;
	    // list has the inner payload withhout accolades
		String sInstructions = "";
		if( list.size() > 0 ) {
			for(int i=0;i<list.size();i++) 
			{
				sInstructions += list.get(i);
			}
			sInstructions = sInstructions.trim();
			//if( sInstructions.endsWith(";") == false ) sInstructions = sInstructions + ";";  // force a concluding semicolon
			if( sInstructions.endsWith(";") == false ) {
				errit("Missing concluding semi-colon on [" + sLine );
				return null;
			}
			ArrayList<generatorInstruction> instructionList = extractInstructionList(sInstructions);
			if( instructionList == null ) return null;
			for(int k=0;k<instructionList.size();k++)
			{
			  //
			  instructionList.get(k).fromPortTipe = generatorConstants.PORTTYPE.TRANSFORMATION;
		      instructionList.get(k).toPortTipe = generatorConstants.PORTTYPE.TRANSFORMATION;
			  if( k==0 ) instructionList.get(k).fromPortTipe = generatorConstants.PORTTYPE.SOURCE_QUALIFIER;
			  if( k == (instructionList.size()-1) )	  instructionList.get(k).toPortTipe = generatorConstants.PORTTYPE.TARGET;
			  //
			  mp.instr_list.add(instructionList.get(k));	
			}
		}
		list = null;
		//
		return mp;
	}
	
	//---------------------------------------------------------------------------------
	public String euh(String sIn)
	//---------------------------------------------------------------------------------
	{
					String sTemp = "";
				    char[] bfr = sIn.toCharArray();
				    for(int i=0;i<bfr.length;i++) 
					{	
						if ( (bfr[i] > (char)0x20) && ((int)bfr[i] < 128)) sTemp = sTemp + bfr[i];
					}		
					return sTemp;
				    	
	}
	
	//  <tablename>.<columnname>   <NN>.columnName    or just <columname>
	//----------------------------------------------------------------
	private int getTableIndex( String sIn , boolean isSource)
	//----------------------------------------------------------------
	{
		   String sFieldName = xMSet.xU.removeBelowIncludingSpaces(sIn).trim();
		   int k = sIn.indexOf(".");
		   if( k < 0 ) return 0;
		   String sTab = sFieldName.substring(0,k);
		   //errit("looking for [" + sTab + "]");
		   int idx = xMSet.xU.NaarInt(sTab);   // try to perform a TO_NUM
		   if( idx >= 1) {
			   if( isSource ) {
				   if( idx <= sourceTableList.size() ) return (idx - 1);
			   }
			   else {
				   if( idx <= targetTableList.size() ) return (idx - 1);
			   }
		   }
		   if( isSource )
		   {
			   for(int i=0;i<sourceTableList.size();i++)
			   {
				  if( sTab.compareToIgnoreCase( sourceTableList.get(i) ) == 0 ) return i;
			   }
			   errit("Cannot locate source [" + sTab + "] defined in field [" + sIn + "]" ); 
			   return -1;
		   }
		   else {
			   for(int i=0;i<targetTableList.size();i++)
			   {
				  if( sTab.compareToIgnoreCase( targetTableList.get(i) ) == 0 ) return i;
			   }
			   errit("Cannot locate target [" + sTab + "] defined in field [" + sIn + "]" ); 
			   return -1;
		   }
	}
	
	//----------------------------------------------------------------
	private String getShort(String sIn)
	//----------------------------------------------------------------
	{
		   String sRet = sIn;
		   int k = sIn.indexOf(".");
		   if( k < 0 ) return sRet;
		   sRet = sRet.substring(k+1);
		   return sRet;
	}
	
	//----------------------------------------------------------------
	private ArrayList<generatorInstruction> extractInstructionList( String sInstructions)
	//----------------------------------------------------------------
	{
		//logit(9,"Instructions -> " + sInstructions );
		ArrayList<generatorInstruction> list = new ArrayList<generatorInstruction>(); 
		StringTokenizer st = new StringTokenizer(sInstructions, ";");
		{
			int level = 0;
			while(st.hasMoreTokens()) 
			{
				level++;
				String instructie = st.nextToken();
				generatorInstruction x = extractInstruction( instructie , level);
				if( x == null ) return null;
				list.add(x);
			} 
		}
		return list;
	}
	
	//  <COMMAND> ( val ) 
	//----------------------------------------------------------------
	private generatorInstruction extractInstruction(String sIn , int level)
	//----------------------------------------------------------------
	{
		String sCmd = sIn.trim();
		if( sCmd.endsWith(")") == false ) {
			errit( "There is no closing ) on [" + sIn + "]");
			return null;
		}
		int idx = sCmd.indexOf("(");
		if( idx < 0 ) {
			errit( "There is no opening ( on [" + sIn + "]");
			return null;
		}
		String sLeft = null;
		String sRight = null;
		try {
		  sLeft  = sCmd.substring(0,idx);
		  sRight = sCmd.substring(idx+1,sCmd.length()-1);
		}
		catch(Exception e) {
			errit("General error : Cannot parse instruction [" + sIn + "]");
			return null;
		}
		//
		generatorInstruction x = new generatorInstruction( xMSet.getNextUID() , null);
		x.tipe = xConst.getInstructionype( sLeft );
		if( x.tipe == null ) return null;
		x.rVal = sRight;
		x.stage = level;
		switch ( xConst.expectRval( x.tipe ) )
		{
		case YES : {if( sRight.trim().length() == 0 ) {errit( "RVAL is required for [" + sIn + "] " + x.tipe); return null; } break; }
		case NO  : {if( sRight.trim().length() > 0 )  {errit( "No RVAL required for [" + sIn + "] " + x.tipe); return null; } break; }
		default  : { errit("Cannot evaluate whether RVAL is required for[" + sIn + "] " + x.tipe); return null; }
		}
		// FKEY ?? -->  rval =  n , y 
		if( x.tipe == generatorConstants.INSTRUCTION_TYPE.FKEY ) {
		  try {
			  int ndx = x.rVal.indexOf(",");
			  if(  ndx < 0 ) {
				  errit( "FKEY requires  ( n , m)  n being the number of the foreign key , m the rank");
				  return null;
			  }
			  String sKeyNum = x.rVal.substring(0,ndx);
			  String sKeyRank = x.rVal.substring(ndx+1);
			  //errit( x.rVal + " (" + sKeyNum + ") (" + sKeyRank + ")");
			  
			  int zz = xMSet.xU.NaarInt( sKeyNum );
			  switch( (zz-1) )
			  {
			  case 0 : {  x.tipe = generatorConstants.INSTRUCTION_TYPE.FKEY_0; break; }
			  case 1 : {  x.tipe = generatorConstants.INSTRUCTION_TYPE.FKEY_1; break; }
			  case 2 : {  x.tipe = generatorConstants.INSTRUCTION_TYPE.FKEY_2; break; }
			  case 3 : {  x.tipe = generatorConstants.INSTRUCTION_TYPE.FKEY_3; break; }
			  case 4 : {  x.tipe = generatorConstants.INSTRUCTION_TYPE.FKEY_4; break; }
			  case 5 : {  x.tipe = generatorConstants.INSTRUCTION_TYPE.FKEY_5; break; }
			  case 6 : {  x.tipe = generatorConstants.INSTRUCTION_TYPE.FKEY_6; break; }
			  case 7 : {  x.tipe = generatorConstants.INSTRUCTION_TYPE.FKEY_7; break; }
			  case 8 : {  x.tipe = generatorConstants.INSTRUCTION_TYPE.FKEY_8; break; }
			  case 9 : {  x.tipe = generatorConstants.INSTRUCTION_TYPE.FKEY_9; break; }
			  default : {
				  			errit("FKEY (N,x) : N needs to be between 1 and 10");
				  			return null;
			  			}
			  }
			  x.rVal = sKeyRank.trim();
		  }
		  catch( Exception e) {
			errit("Error parsing KEY [" + x.rVal + "]");
			return null;
		  }
		  //
		}
		//
		return x;
	}
	
	//----------------------------------------------------------------
	private boolean extract_flatfile_list(String sline)
	//----------------------------------------------------------------
	{
		flatfile_list = new ArrayList<String>();
		if( sline == null ) return false;
		StringTokenizer st = new StringTokenizer(sline.trim() , ",;");
		{
			while(st.hasMoreTokens()) 
			{
				String filename = st.nextToken().trim();
				if (filename == null ) continue;
				if( filename.length() == 0 ) continue;
				
				boolean found = false;
				for(int k=0;k<sourceTableList.size();k++)
				{
					if( filename.compareToIgnoreCase( sourceTableList.get(k) ) == 0 ) {
						found= true;
						break;
					}
				}
				if( found == false )
				{
					for(int k=0;k<targetTableList.size();k++)
					{
						if( filename.compareToIgnoreCase( targetTableList.get(k) ) == 0 ) {
							found= true;
							break;
						}
					}	
				}
				if( found == false ) {
					errit("File defined on FLATFILES: [" + filename + "] is not part of the set of source and target tabels defined");
					return false;
				}
				flatfile_list.add( filename );
			} 
		}
		return true;
	}
	
	// KB 26 AUG 
	//----------------------------------------------------------------
	public String getOverruleMappingName()
	//----------------------------------------------------------------
	{
		if( overruleMappingName == null ) return overruleMappingName;
		String sRet = overruleMappingName.trim();
		if (sRet.length() < 1) return null;
		return sRet;
	}
	// KB 28 AUG 
	//----------------------------------------------------------------
	private boolean do_SQLOverride(String sIn)
	//----------------------------------------------------------------
	{
		// a couple of checks :  select /  from
		boolean looksOK=true;
		if( sIn.toUpperCase().indexOf("SELECT") < 0 ) looksOK=false;
		if( sIn.toUpperCase().indexOf("FROM") < 0 ) looksOK=false;
		if( looksOK == false ) {
			errit("Probably incorrect SQL statement [" + sIn + "]");
			return false;
		}
		//  {  n , <statement }
		String sNum=null;
		try {
			sNum = sIn.substring( sIn.indexOf("{") + 1 , sIn.indexOf(",") ).trim();
		}
		catch(Exception e ) {
			errit("Cannot extract source index number from [" + sIn + "]");
			return false;
		}
		int idx = xMSet.xU.NaarInt( sNum );
		if( idx < 1 ) {
			errit("Missing correct source index number from [" + sIn + "]");
			return false;
		}
		if( idx > this.sourceTableList.size() ) {
			errit("Source index number too large [" + sIn + "]");
			return false;
		}
		//
		String sSQL = null;
		try {
			sSQL = sIn.substring( sIn.indexOf(",")+1 , sIn.indexOf("}") ).trim();
		}
		catch(Exception e ) {
			errit("Cannot extract SQL statement from [" + sIn + "]");
			return false;
		}
		//
		String sSQLXML = toInfaXML( sSQL );
		//logit( 5 , "SQL Override statement [idx=" + idx + "] [" + sSQLXML + "]");
		//
		infaPair x = new infaPair(""+(idx-1) , sSQLXML);  // code in pair is the index (offset 0)
		SQLOverrideList.add(x);
		//
		generatorOption y = new generatorOption();
		y.tipe   = generatorConstants.SRC_TGT_TIPE.SOURCE;
		y.option = generatorConstants.OPTION.SQL_QUERY;
		y.index = idx-1;
		y.value = sSQLXML;
		option_list.add(y);
		//
		return true;
	}
	
	//----------------------------------------------------------------
	private String toInfaXML(String sIn)
	//----------------------------------------------------------------
	{
		String sRet = "";
		char[] sbuf = sIn.toCharArray();
		// Hex
		for(int i=0;i<sbuf.length;i++) 
		{	
			if( sbuf[i] >= 0x20) {
				sRet += xMSet.xU.transformToXMLEscape(""+sbuf[i]);
				continue;
			}
			String hex = Integer.toHexString((int)sbuf[i]);
			sRet += "&#x" + hex.toUpperCase() + ";";
		}		
		return sRet;
	}
	
	//----------------------------------------------------------------
	public ArrayList<infaPair> getSQLOverrideStatements()
	//----------------------------------------------------------------
	{
	      return SQLOverrideList;		
	}
	
	//----------------------------------------------------------------
	private boolean do_option(String sIn )
	//----------------------------------------------------------------
	{
		//logit(5,"OPTION [" + sIn );
		//  {SOURCEOPTION|TARGETOPTION : {  NN ,  option="something" } 
		if( sIn.indexOf("{") < 0 ) {
			errit("Option definition [" + sIn + "] - no opening accoloade");
		}
		if( sIn.indexOf("}") < 0 ) {
			errit("Option definition [" + sIn + "] - no closing accoloade");
		}
		if( sIn.indexOf("=") < 0 ) {
			errit("Option definition [" + sIn + "] - no option=value construct");
		}
		if( sIn.indexOf("\"") < 0 ) {
			errit("Option definition [" + sIn + "] - value probably not enclosed in double quotes");
		}
		try 
		{
			String sTipe = null;
			sTipe = sIn.substring( 0 , sIn.indexOf(":") ).trim();
			String sNum = null;
			sNum = sIn.substring( sIn.indexOf("{") + 1 , sIn.indexOf(",") ).trim();
			String sOption = null;
			sOption = sIn.substring( sIn.indexOf(",")+1 , sIn.indexOf("=") ).trim();
			String sValue = null;
			sValue = sIn.substring( sIn.indexOf("=")+1 , sIn.indexOf("}") ).trim();
			//
			generatorOption x = new generatorOption();
			x.tipe   = generatorConstants.SRC_TGT_TIPE.UNKNOWN;
			//
			if( sTipe.compareToIgnoreCase("SOURCEOPTION") == 0 ) x.tipe  = generatorConstants.SRC_TGT_TIPE.SOURCE;
			if( sTipe.compareToIgnoreCase("TARGETOPTION") == 0 ) x.tipe  = generatorConstants.SRC_TGT_TIPE.TARGET;
			if( sTipe.compareToIgnoreCase("SOURCEQUALOPTION") == 0 ) x.tipe  = generatorConstants.SRC_TGT_TIPE.SOURCEQUALIFIER;
			if( sTipe.compareToIgnoreCase("SRCQUALOPTION") == 0 ) x.tipe  = generatorConstants.SRC_TGT_TIPE.SOURCEQUALIFIER;
			if( sTipe.compareToIgnoreCase("SOURCEQUALIFIEROPTION") == 0 ) x.tipe  = generatorConstants.SRC_TGT_TIPE.SOURCEQUALIFIER;
			if( x.tipe == generatorConstants.SRC_TGT_TIPE.UNKNOWN ) {
				errit("Option defintion [" + sIn + "] does not start with SOURCEOPTION or TARGETOPTION");
				return false;
			}
			//
			int idx = xMSet.xU.NaarInt( sNum );
			if( idx < 1 ) {
				errit("Missing correct source index number from [" + sIn + "]");
				return false;
			}
			if( (idx > this.sourceTableList.size()) && (x.tipe == generatorConstants.SRC_TGT_TIPE.SOURCE )  ) {
				errit("Source index number too large [" + sIn + "]");
				return false;
			}
			if( (idx > this.targetTableList.size()) && (x.tipe == generatorConstants.SRC_TGT_TIPE.TARGET )  ) {
				errit("Source index number too large [" + sIn + "]");
				return false;
			}
			x.index = idx - 1;
			// tipe 
			x.option = xConst.getOption( sOption );
			if( x.option == null ) {
				errit("Option definition [" + sIn + "] incorrect option [" + sOption + "]");
				return false;
			}
			// value must be enclosed in quotes
			if( (sValue.startsWith("\"") == false ) || (sValue.endsWith("\"") == false ) || (sValue.length() < 2) )  {
				errit("Option definition [" + sIn + "] value must be enclosed in double quotes");
				return false;
			}
			// stip surrounding quotes
			sValue = sValue.substring(1, sValue.length() - 1);
			//
			x.raw   = sValue;
			switch( x.option )
			{
			case SQL_QUERY 		   : {
				boolean looksOK=true;
				if( sIn.toUpperCase().indexOf("SELECT") < 0 ) looksOK=false;
				if( sIn.toUpperCase().indexOf("FROM") < 0 ) looksOK=false;
				if( looksOK == false ) {
					errit("Probably incorrect SQL statement [" + sIn + "]");
					return false;
				}
				// geen break;
			}
			case USER_DEFINED_JOIN : ;
			case SOURCE_FILTER     : ;
			case PRE_SQL           : ;
			case POST_SQL          : {
				if( x.index != 0 ) {
					errit("SQ overrides only supported on first Source table[" + (x.index+1) + "]");
					return false;
				}
				x.value = toInfaXML( sValue );
				break;
			}
			default : {
				x.value = sValue;
				break;
			}
			}
			logit( 5 , "Generator Option [" + x.tipe + "] [" + x.index + "] [" + x.option + "] [" + x.value + "]");
			//
			option_list.add( x );
		}
		catch( Exception e) {
			errit("Error when extracting option from [" + sIn + "]");
			return false;
		}
		return true;
	}
	
	//----------------------------------------------------------------
	public ArrayList<generatorOption> getOptionList()
	//----------------------------------------------------------------
	{
		return option_list;
	}
}
