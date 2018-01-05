package pcGenerator.generator;

import java.util.ArrayList;

import pcGenerator.ddl.readInfaXML;
import pcGenerator.powercenter.infaConstraint;
import pcGenerator.powercenter.infaPair;
import pcGenerator.powercenter.infaSource;
import generalpurpose.pcDevBoosterSettings;

public class generatorSrcTgtManager {

	pcDevBoosterSettings xMSet = null;
	generatorConstants gConst=null;
	
	ArrayList<generatorSrcTgt> SrcTgtList = null;
	ArrayList<generatorOption> optionList=null;
	
	//----------------------------------------------------------------
	public generatorSrcTgtManager(pcDevBoosterSettings im)
	//----------------------------------------------------------------
	{
		xMSet = im;
		gConst = new generatorConstants(xMSet);
		SrcTgtList = new ArrayList<generatorSrcTgt>();
		optionList = new ArrayList<generatorOption>();
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
	public ArrayList<generatorSrcTgt> getSrcTgtList()
	//----------------------------------------------------------------
	{
		return SrcTgtList;
	}
	
	//----------------------------------------------------------------
	public int getNumberOfListItems()
	//----------------------------------------------------------------
	{
		return SrcTgtList.size();
	}
	//----------------------------------------------------------------
	public generatorSrcTgt getSrcTgtViaIdx(int idx)
	//----------------------------------------------------------------
	{
		 if( (idx < 0) || (idx>=SrcTgtList.size()) ) {
			 errit("Index out of bound [" + idx + "]");
			 return null;
		 }
		return SrcTgtList.get(idx);
	}
	
	//----------------------------------------------------------------
	public boolean initializeLists(String sSrcList, String sTgtList)
	//----------------------------------------------------------------
	{
		   //
		   SrcTgtList = new ArrayList<generatorSrcTgt>();
		   //
		   int ncount = xMSet.xU.TelDelims(sSrcList+',',',');
		   int teller=0;
		   for(int i=0;i<=ncount;i++)
		   {
			   String sSrc = xMSet.xU.GetVeld(sSrcList,i,',');
			   if( sSrc == null ) continue;
			   if( sSrc.length() == 0 ) continue;
			   generatorSrcTgt x = new generatorSrcTgt(sSrc,readInfaXML.ParseType.SOURCETABLE);
			   SrcTgtList.add(x);
			   teller++;
		   }
		   if( teller == 0 ) {
			   errit("No SourceNames found on commandline");
			   return false;
		   }
		   //
		   ncount = xMSet.xU.TelDelims(sTgtList+',',',');
		   teller=0;
		   for(int i=0;i<=ncount;i++)
		   {
			   String sTgt = xMSet.xU.GetVeld(sTgtList,i,',');
			   if( sTgt == null ) continue;
			   if( sTgt.length() == 0 ) continue;
			   generatorSrcTgt x = new generatorSrcTgt(sTgt,readInfaXML.ParseType.TARGETTABLE);
			   SrcTgtList.add(x);
			   teller++;
		   }
		   if( teller == 0 ) {
			   errit("No TargetNames found on commandline");
			   return false;
		   }
		   for(int i=0;i<SrcTgtList.size();i++)
	       {
	       	logit( 9 , "SRC/TGT " + SrcTgtList.get(i).TableName + " " + SrcTgtList.get(i).tipe );
	       }
		   return true;
	}
	
	//----------------------------------------------------------------
	public void changeToFlatFile( char tpe , int changeIndex)
	//----------------------------------------------------------------
	{
			
			int counter = 0;
			for(int i=0;i<SrcTgtList.size();i++)
			{
				if( tpe == 'S' ) {
					if( (SrcTgtList.get(i).tipe == readInfaXML.ParseType.SOURCETABLE) || (SrcTgtList.get(i).tipe == readInfaXML.ParseType.SOURCEFLATFILE) ) {
						if( counter == changeIndex ) SrcTgtList.get(i).tipe = readInfaXML.ParseType.SOURCEFLATFILE;
						logit(5,"The Source/Target type has been set to [" + tpe + "] for Source/Target [" + changeIndex + "]");
						counter++;
					}
				}
	            if( tpe == 'T' ) {
	            	if( (SrcTgtList.get(i).tipe == readInfaXML.ParseType.TARGETTABLE) || (SrcTgtList.get(i).tipe == readInfaXML.ParseType.TARGETFLATFILE) ) {
						if( counter == changeIndex ) SrcTgtList.get(i).tipe = readInfaXML.ParseType.TARGETFLATFILE;
						logit(5,"The Source/Target type has been set to [" + tpe + "] for Source/Target [" + changeIndex + "]");
						counter++;
					}
				}
			}
	}
	
	//----------------------------------------------------------------
	public boolean setFilesToFlatFile(ArrayList<String> list)
	//----------------------------------------------------------------
	{
		for(int i=0 ; i< list.size(); i++)
		{
			boolean found = false;
			for(int k=0;k<SrcTgtList.size();k++)
			{
				if( SrcTgtList.get(k).TableName.compareToIgnoreCase(list.get(i)) != 0 ) continue;
				if( SrcTgtList.get(i).tipe == readInfaXML.ParseType.SOURCETABLE ) {
					SrcTgtList.get(i).tipe = readInfaXML.ParseType.SOURCEFLATFILE;
					logit(5,"Source [" + list.get(i) + "] has been set to [" + SrcTgtList.get(i).tipe  + "]");
					found = true;
				}
				else
				if( SrcTgtList.get(i).tipe == readInfaXML.ParseType.TARGETTABLE ) {
					SrcTgtList.get(i).tipe = readInfaXML.ParseType.TARGETFLATFILE;
					logit(5,"Target [" + list.get(i) + "] has been set to [" + SrcTgtList.get(i).tipe  + "]");
					found = true;
				}
			}
			if( found == false ) {
				errit("Cannot find [" + list.get(i) + "] to set it to flatfile");
			}
		}
		
		return true;
	}
	
	//----------------------------------------------------------------
	private void debugTableInfo(infaSource x)
	//----------------------------------------------------------------
	{
			logit(9,"Table -> " + x.Name +  " " + x.fieldList.size());
			for(int i=0;i<x.fieldList.size();i++)
			{
				logit(9,"->" + 
						x.fieldList.get(i).fieldNumber + " " +
						x.fieldList.get(i).Name + " " +
						x.fieldList.get(i).DataType + " " +
						x.fieldList.get(i).Precision + " " +
						x.fieldList.get(i).scale + " " +
						x.fieldList.get(i).mandatory + " " +
						x.fieldList.get(i).KeyType + " [fp=" +
						x.fieldList.get(i).FieldProperty + "] "
			    );
			}
			if( x.constraintList.size() > 0) {
		     for(int i=0;i<x.constraintList.size();i++)
		     {
		         infaConstraint co = x.constraintList.get(i);	
		         logit(9,"-> Constraint [" + co.Tipe + "] " + co.Name + " " + co.key_list.toString() + " " + co.ReferencedOwner + "." + co.ReferencedTableName + " " +co.ref_list.toString());
 		     }
			}
	}
		
	//----------------------------------------------------------------
	private boolean checkDatabase(String sDir)
	//----------------------------------------------------------------
	{
			if( xMSet.xU.IsDir(sDir) == false ) {
				errit("Cannot locate database directory [" + sDir + "]");
				return false;
			}
			ArrayList<String> list = xMSet.xU.GetFilesInDir( sDir , null );
			
			for(int i=0;i<list.size();i++)
			{
				if( list.get(i).toUpperCase().endsWith(".XML") == false ) list.set(i, null);
			}
			int aantal=list.size();
			for(int i=0;i<aantal;i++)
			{
				for(int j=0;j<list.size();j++)
				{
					if( list.get(j) == null ) {
						list.remove(j);
						break;
					}
				}
			}
			if( list.size() == 0 ) {
				errit("No XMLS found in directory [" + sDir + "]");
				return false;
			}
			return true;
	}
	
	//----------------------------------------------------------------
	private infaSource scanThroughXML(String FName, readInfaXML.ParseType tipe , String lookForTableName )
	//----------------------------------------------------------------
	{
			readInfaXML reader = new readInfaXML(xMSet);
			ArrayList<infaSource> list = reader.parse_Export( FName , tipe , lookForTableName );
			if( list == null ) return null; 
			if( list.size() == 0 ) return null;
			if( list.size() > 1 ) {
				errit("Found multiple occurences of [" + lookForTableName +"]" );
				return null;
			}
			return list.get(0);
	}
	
	//----------------------------------------------------------------
	public boolean load_SourceAndTargetSpecs()
	//----------------------------------------------------------------
	{
	        for(int i=0;i<SrcTgtList.size();i++)
	        {
	      	    logit(9,"Looking for " + SrcTgtList.get(i).tipe + " [" + SrcTgtList.get(i).TableName + "]" );
	        	//
	        	String sDir = null;
	        	switch( SrcTgtList.get(i).tipe )
	        	{
	        	case SOURCEFLATFILE : ;
	        	case SOURCETABLE : {
	        		sDir = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Sources";
	        		break;
	        	}
	        	case TARGETFLATFILE : ;
	        	case TARGETTABLE : {
	        		sDir = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Targets";
	        		break;
	        	}
	        	default : {
	        		errit("System error - load_SourceSpecs - unknow tipe " + SrcTgtList.get(i).tipe );
	        		return false;
	        	}
	        	}
	        	//
	        	if( checkDatabase(sDir) == false ) return false;
	       	    ArrayList<String> list = xMSet.xU.GetFilesInDir( sDir , null );
	       	    // INVERSE Sort - most recent tables will be read first
	       	    for(int k=0;k<list.size();k++)
	       	    {
	       	    	for(int j=0;j<(list.size()-1);j++)
	       	    	{
	       	    	   if( list.get(j).compareToIgnoreCase(list.get(j+1)) < 0 ) {
	       	    		   String z = list.get(j);
	       	    		   list.set( j, list.get(j+1) );
	       	    		   list.set( j+1, z);
	       	    	   }
	       	    	}
	       	    }
	       	    //
	       	    for(int j=0;j<list.size();j++)
	 		    {
	       	     //System.out.println("  Now looking in file [" + list.get(j) + "]");	
	 			 SrcTgtList.get(i).tab_obj = scanThroughXML( sDir + xMSet.xU.ctSlash + list.get(j) , SrcTgtList.get(i).tipe , SrcTgtList.get(i).TableName );
	 			 if( SrcTgtList.get(i).tab_obj != null ) break;
	 			 //System.out.println("  Not found");	
	 			}
	       	    // not found
	       	    if( SrcTgtList.get(i).tab_obj == null ) {
	       	    	errit("Could not locate [" + SrcTgtList.get(i).TableName + "] for [" + SrcTgtList.get(i).tipe + "]" );
	       	    	return false;
	       	    }
	       	    debugTableInfo(SrcTgtList.get(i).tab_obj);
	        }
	   	    return true;
	}
		
	//----------------------------------------------------------------
	public infaSource getInfaSourceViaName( String sName , char tpe )
	//----------------------------------------------------------------
	{
			for(int i=0;i<SrcTgtList.size();i++)
			{
				if( tpe == 'S' ) {
					if( (SrcTgtList.get(i).tipe == readInfaXML.ParseType.SOURCETABLE) || (SrcTgtList.get(i).tipe == readInfaXML.ParseType.SOURCEFLATFILE) ) {
	                  if( SrcTgtList.get(i).TableName.trim().compareTo(sName.trim()) == 0) return SrcTgtList.get(i).tab_obj;		
					}
				}
				else
				if( tpe == 'T' ) {
					if( (SrcTgtList.get(i).tipe == readInfaXML.ParseType.TARGETTABLE) || (SrcTgtList.get(i).tipe == readInfaXML.ParseType.TARGETFLATFILE) ) {
			            if( SrcTgtList.get(i).TableName.trim().compareTo(sName.trim()) == 0) return SrcTgtList.get(i).tab_obj;		
			    	}
				}
				else {
				 errit("Unsupported [tipe=" + tpe + "] in getSourceViaName");
				 return null;
				}
			}
			errit("(getInfaSourceViaName) Cannot find Source/Target object in list for [tipe=" + tpe + "] [Name=" + sName + "]");
			return null;	
	}
	
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
	//----------------------------------------------------------------
	public int getNbrOfSources()
	//----------------------------------------------------------------
	{
		   return getNbrOfSourceTargets('S');	
	}
	//----------------------------------------------------------------
	public int getNbrOfTargets()
	//----------------------------------------------------------------
	{
		   return getNbrOfSourceTargets('T');	
	}
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
	
	//----------------------------------------------------------------
	public infaSource getInfaSource(int idx)
	//----------------------------------------------------------------
	{
			return getSourceOrTarget( 'S' , idx );
	}
	//----------------------------------------------------------------
	public infaSource getInfaTarget(int idx )
	//----------------------------------------------------------------
	{
			return getSourceOrTarget( 'T' , idx );
	}
	//----------------------------------------------------------------
	public String getSourceListDisplay()
	//----------------------------------------------------------------
	{
		String sRet = "";
		int nCount = this.getNbrOfSources();
		for(int i=0;i<nCount;i++)
		{
			String sName = getInfaSource(i).Name;
			if( i == 0 ) sRet = sName;
			else sRet = sRet + "," + sName;
		}
		return "(" + sRet + ")";
	}
	//----------------------------------------------------------------
	public String getTargetListDisplay()
	//----------------------------------------------------------------
	{
			String sRet = "";
			int nCount = this.getNbrOfTargets();
			for(int i=0;i<nCount;i++)
			{
				String sName = getInfaTarget(i).Name;
				if( i == 0 ) sRet = sName;
				else sRet = sRet + "," + sName;
			}
			return "(" + sRet + ")";
	}
	
	// KB 28 AUG - Currently only 1 sql override supported, ie. on the first source
	//----------------------------------------------------------------
	public boolean setSQLOverride(ArrayList<infaPair> lst)
	//----------------------------------------------------------------
	{
		if( lst == null ) {
			errit("strange - should not be null");
			return false;
		}
		for(int i=0;i<lst.size();i++)
		{
			int idx = xMSet.xU.NaarInt(lst.get(i).code);
			if( idx < 0 ) {
				errit("Wrong source number on SQL override [idx=" + lst.get(i).code + "] on [" + lst.get(i).value + "]" );
				return false;
			}
			if( setSQLOverrideOnInfaSource( lst.get(i).value , idx ) == false ) return false;
		}
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean setSQLOverrideOnInfaSource(String sIn , int idx)
	//----------------------------------------------------------------
	{
		logit( 5 , "SQL Override statement [" + sIn + "]");
	    //	
		if( idx != 0 ) {
			errit("Sorry currently the SQL Override can only be set on the first infaSource");
			return false;
		}
	    infaSource x = this.getInfaSource(idx);	
		if( x == null ) {
			errit("Cannot find a source table to attach SQL Override statement");
	    	return false;
		}
		if( x.tipe == readInfaXML.ParseType.SOURCEFLATFILE ) {
			errit("Cannot put an SQL Override statement on a Flat File");
			return false;
		}
		x.SQLOverride = sIn;
		return true;
	}

	
	//----------------------------------------------------------------
	public boolean overrule_specs()
	//----------------------------------------------------------------
	{
		int srcIdx=-1;
		int tgtIdx=-1;
		for(int i=0;i<SrcTgtList.size();i++)
		{
			if( (SrcTgtList.get(i).tipe == readInfaXML.ParseType.SOURCETABLE) || (SrcTgtList.get(i).tipe == readInfaXML.ParseType.SOURCEFLATFILE) ) {
				srcIdx++;
				if( overrule_SourceTargetAttribs( generatorConstants.SRC_TGT_TIPE.SOURCE  , i , srcIdx ) == false ) return false;
				// flatfile
				if( SrcTgtList.get(i).tipe == readInfaXML.ParseType.SOURCEFLATFILE ) {
					if( overrule_FlatFileAttribs( generatorConstants.SRC_TGT_TIPE.SOURCE  , i , srcIdx ) == false ) return false;
				}
			}
			if( (SrcTgtList.get(i).tipe == readInfaXML.ParseType.TARGETTABLE) || (SrcTgtList.get(i).tipe == readInfaXML.ParseType.TARGETFLATFILE) ) {
				tgtIdx++;
				if( overrule_SourceTargetAttribs( generatorConstants.SRC_TGT_TIPE.TARGET  , i , tgtIdx ) == false ) return false;
				// flatfile
				if( SrcTgtList.get(i).tipe == readInfaXML.ParseType.TARGETFLATFILE ) {
					if( overrule_FlatFileAttribs( generatorConstants.SRC_TGT_TIPE.TARGET  , i , tgtIdx ) == false ) return false;
				}
			}
		}
		return true;
	}
	
	/*
   <TABLEATTRIBUTE NAME ="Base Table Name" VALUE =""/>
   <TABLEATTRIBUTE NAME ="Search Specification" VALUE =""/>
   <TABLEATTRIBUTE NAME ="Sort Specification" VALUE =""/>
   <TABLEATTRIBUTE NAME ="Datetime Format" VALUE ="A 19 mm/dd/yyyy hh24:mi:ss"/>
   <TABLEATTRIBUTE NAME ="Thousand Separator" VALUE ="None"/>
   <TABLEATTRIBUTE NAME ="Decimal Separator" VALUE ="."/>
	 */
	
	//----------------------------------------------------------------
	private boolean overrule_SourceTargetAttribs( generatorConstants.SRC_TGT_TIPE tp , int idx , int pdx )   // idx = srctt combined id  pdx = source index
	//----------------------------------------------------------------
	{
		for(int i=0;i<optionList.size();i++)
		{
			generatorOption opt = optionList.get(i);
			if( (opt.tipe != tp) || (opt.index != pdx) ) continue;
			boolean found = false;
	//logit( 5 , "OPT>>>" + opt.option + " " + opt.value );
			infaSource src = SrcTgtList.get(idx).tab_obj;
			for(int j=0;j<src.tableAttributeList.size();j++)
			{
				String sCode = xMSet.xU.Remplaceer(src.tableAttributeList.get(j).code.trim().toUpperCase()," ", "_");
				generatorConstants.OPTION optiecode = gConst.getOption( sCode );
	//logit(5,"   >>>" + src.tableAttributeList.get(j).code +" " + src.tableAttributeList.get(j).value + " " + sCode + " " + optiecode );
				if( opt.option != optiecode ) continue;
				src.tableAttributeList.get(j).value = opt.value;
				logit( 5 , "MATCH [tipe=" + tp + "] [Option=" + optiecode + "] [" +src.tableAttributeList.get(j).value + "] => [" +  opt.value + "]");
				found=true;
			}
			
		}
		
		return true;
	}
	
	/*
	 *  CODEPAGE, CONSECDELIMITERASONE, DELIMITED, DELIMITERS
		,ESCAPECHARACTER, KEEPESCAPECHAR, LINESEQUENTIAL, MULTIDELIMITERSASAND
		,NULLCHARTYPE, NULLCHARACTER, PADBYTES, QUOTECHARACTER
		,REPEATABLE, ROWDELIMITER, SHIFTSENSITIVEDATA, SKIPROWS
		,STRIPTRAILINGBLANKS 
	 */
	//----------------------------------------------------------------
	private boolean overrule_FlatFileAttribs( generatorConstants.SRC_TGT_TIPE tp , int idx , int pdx )   // idx = srctt combined id  pdx = source index
	//----------------------------------------------------------------
	{
			infaSource src = SrcTgtList.get(idx).tab_obj;
			if (src.flafle == null ) {
				errit("[" + SrcTgtList.get(idx).TableName + "] is not a flat file");
				return false;
			}
			for(int i=0;i<optionList.size();i++)
			{
					generatorOption opt = optionList.get(i);
					if( (opt.tipe != tp) || (opt.index != pdx) ) continue;
					//logit( 5 , "Checking flat file [" + src.Name + "] " + opt.option + " " + opt.value );
				
					boolean found = true;
					switch( opt.option )
					{
					case CODEPAGE   			: { src.flafle.CodePage = opt.value; break; }
					case CONSECDELIMITERASONE 	: { src.flafle.Consecdelimiterasone = opt.value; break; }
					case DELIMITED  			: { src.flafle.Delimited = opt.value; break; }
					case DELIMITERS 			: { src.flafle.Delimiters = opt.value; break; }
					case ESCAPE_CHARACTER 		: { src.flafle.EscapeCharacter = opt.value; break; }
					case KEEPESCAPECHAR 		: { src.flafle.Keepescapechar = opt.value; break; }
					case LINESEQUENTIAL 		: { src.flafle.LineSequential = opt.value; break; }
					case MULTIDELIMITERSASAND 	: { src.flafle.Multidelimitersasand = opt.value; break; }
					case NULLCHARTYPE 			: { src.flafle.NullCharType = opt.value; break; }
					case NULL_CHARACTER 		: { src.flafle.Nullcharacter = opt.value; break; }
					case PADBYTES 				: { src.flafle.Padbytes = opt.value; break; }
					case QUOTE_CHARACTER 		: { src.flafle.QuoteCharacter = opt.value; break; }
					case REPEATABLE 			: { src.flafle.Repeatable = opt.value; break; }
					case ROWDELIMITER 			: { src.flafle.RowDelimiter = opt.value; break; }
					case SHIFTSENSITIVEDATA 	: { src.flafle.ShiftSensitiveData = opt.value; break; }
					case SKIPROWS 				: { src.flafle.Skiprows = opt.value; break; }
					case STRIPTRAILINGBLANKS	: { src.flafle.Striptrailingblanks = opt.value; break; }
					default : { found = false; break; }
					}
					if( found ) {
						logit( 5 , "MATCH FLATFILE ATTRIBUTE " + opt.option + " [" + "x" + "] => [" +  opt.value + "]");
					}
			}
			return true;
	}
	
	//----------------------------------------------------------------
	public String getSQOverrideValue( String sCodeIn )
	//----------------------------------------------------------------
	{
		String sCode = xMSet.xU.Remplaceer( sCodeIn.trim().toUpperCase() ," ", "_");
		generatorConstants.OPTION optiecode = gConst.getOption( sCode );
		if( optiecode == null ) return null;
		
		// ONly for first source , ttz 0
		for(int i=0;i<optionList.size();i++)
		{
			generatorOption opt = optionList.get(i);
			if( (opt.tipe != generatorConstants.SRC_TGT_TIPE.SOURCEQUALIFIER) || (opt.index != 0) || ( opt.option != optiecode) ) continue;
            return opt.value;				
		}
		return null;
	}
	
	//----------------------------------------------------------------
	public int getNumberOfSourceForeignKeys(int SrcIdx)
	//----------------------------------------------------------------
	{
		infaSource x = this.getInfaSource( SrcIdx );
		if( x == null ) return -1;
		return getNumberOfForeignKeysViaObject( x );
	}
	
	//----------------------------------------------------------------
	public int getNumberOfTargetForeignKeys(int TgtIdx)
	//----------------------------------------------------------------
	{
		infaSource x = this.getInfaTarget( TgtIdx );
		if( x == null ) return -1;
		return getNumberOfForeignKeysViaObject( x );
	}
	
	//----------------------------------------------------------------
	private int getNumberOfForeignKeysViaObject( infaSource x )
	//----------------------------------------------------------------
	{
		int cntr=0;
		for(int i=0;i<x.constraintList.size();i++)
		{
			if( x.constraintList.get(i).Tipe != generatorConstants.CONSTRAINT.FOREIGN ) continue;
			cntr++;
		}
		return cntr;
	}
	
	//----------------------------------------------------------------
	public infaConstraint getSourceConstraint( int SrcIdx , generatorConstants.CONSTRAINT tipe , int idx , String CallingFunc )
	//----------------------------------------------------------------
	{
		infaSource x = this.getInfaSource( SrcIdx );
		if( x == null ) return null;
		return getConstraint( x , tipe , idx , CallingFunc );
	}
	
	//----------------------------------------------------------------
	public infaConstraint getTargetConstraint( int TgtIdx , generatorConstants.CONSTRAINT tipe , int idx , String CallingFunc )
	//----------------------------------------------------------------
	{
			infaSource x = this.getInfaTarget( TgtIdx );
			if( x == null ) return null;
			return getConstraint( x , tipe , idx , CallingFunc );
	}
	
	//----------------------------------------------------------------
	private infaConstraint getConstraint( infaSource x , generatorConstants.CONSTRAINT tipe , int idx , String CallingFunc)
	//----------------------------------------------------------------
	{
		int cntr=0;
		for(int i=0;i<x.constraintList.size();i++)
		{
			if( x.constraintList.get(i).Tipe != tipe ) continue;
			if( cntr == idx ) return x.constraintList.get( i );
			cntr++;
		}
		errit("(" + CallingFunc + ") Could not find constraint type [" + tipe + "] on [" + x.Name + "] with sequence number [" + idx + "]");
		return null;
	}
	
}
