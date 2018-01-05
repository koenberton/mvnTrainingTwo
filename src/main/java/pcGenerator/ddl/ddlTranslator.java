package pcGenerator.ddl;


import java.util.ArrayList;

import generalpurpose.gpPrintStream;
import generalpurpose.pcDevBoosterSettings;
import pcGenerator.generator.generatorConstants;
import pcGenerator.powercenter.infaConstraint;
import pcGenerator.powercenter.infaSource;
import pcGenerator.powercenter.infaSourceField;
import pcGenerator.powercenter.infaSourceUtils;


//KB 28 OCT - V11 - added ePurche support
// KB 29 NOV - V15 - added switches DROP, COMMENT, WARNING
	

public class ddlTranslator {

	pcDevBoosterSettings xMSet = null;
	ddlDataTypeConvertor conv = null;
	rdbmsTokenizerConfig config = null;
	rdbmsDatatype rdtipe = null;
	infaSourceUtils utl = null;
	ArrayList<infaSource> srcList = null;
	
	gpPrintStream fout = null;
	int linesWritten=0;
	String ScriptInName = null;
	String FOutputName = null;
	String ConfigFileNameIn = null;
	boolean shoExplain = true;
	
	class tabledepth
	{
		int idx=-1;
		int diepte=0;
	}
	ArrayList<tabledepth> diepteList = null;
	int globaldiepte=0;
	
	//----------------------------------------------------------------
	public ddlTranslator(pcDevBoosterSettings im)
	//----------------------------------------------------------------
	{
		xMSet = im;
		conv = new ddlDataTypeConvertor(xMSet);
		utl = new infaSourceUtils(xMSet);
		config = new rdbmsTokenizerConfig(xMSet);
		rdtipe = new rdbmsDatatype(xMSet);
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
	public boolean translateDDLFromTo( String DDLFileNameIn , String srcDBTipe , String tgtDBTipe , String ConfigFileNameIn)
	//----------------------------------------------------------------
	{
		return translateDDLFromToIntern( DDLFileNameIn , srcDBTipe , tgtDBTipe , false , ConfigFileNameIn );
	}
	
	//----------------------------------------------------------------
	public boolean translateDDLFromToNZSST( String DDLFileNameIn , String srcDBTipe )
	//----------------------------------------------------------------
	{
		return translateDDLFromToIntern( DDLFileNameIn , srcDBTipe , "NETEZZA"  , true , null );
	}
	
	//----------------------------------------------------------------
	private boolean translateDDLFromToIntern( String DDLFileNameIn , String sSrcDBTipe , String sTgtDBTipe , boolean SSTConvertor , String ConfigFileNameIn )
	//----------------------------------------------------------------
	{
		rdbmsDatatype rd = new rdbmsDatatype(xMSet);
		rdbmsDatatype.DBMAKE srcDBTipe = rd.getDBMAKEType(sSrcDBTipe);
		if( srcDBTipe == null ) {
			errit("Unsupported database type [" + sSrcDBTipe + "]");
			return false;
		}
		rdbmsDatatype.DBMAKE tgtDBTipe = rd.getDBMAKEType(sTgtDBTipe);
		if( tgtDBTipe == null ) {
			errit("Unsupported database type [" + sTgtDBTipe + "]");
			return false;
		}
		// SSTCovertor makes Netezza SST tables with columns all set to nvarchar and calculated size
		// SSTConvertor is only applicable for Netezza target
		if( (SSTConvertor == true) && ( tgtDBTipe != rdbmsDatatype.DBMAKE.NETEZZA ) ) {
					errit("SSTConvertor can only be used to translate to NETEZZA");
					return false;
		}
		linesWritten=0;
		ScriptInName = DDLFileNameIn;
		//
		logit(5, "DDL Transform " + srcDBTipe + " to " + tgtDBTipe + " of [" + ScriptInName + "]");
		if( xMSet.xU.IsBestand(DDLFileNameIn) == false )  {
			errit("Cannot find DDL script [" + DDLFileNameIn + "]");
			return false;
		}
		// parse the DDL
		db2Tokenize t1 = new db2Tokenize(xMSet , "UNKNOWN" , srcDBTipe , ConfigFileNameIn);
		srcList = t1.parseFile( DDLFileNameIn );
		if( srcList == null ) return false;
		if( srcList.size() <= 0 ) {
			logit(1,"DDL parser did not return any table definitions");
			return false;
		}
		//
		//  Lees de config nogmaals in
		if( config.parseConfig(ConfigFileNameIn) == false ) {
			errit("Cannot parse the DDL configuration file - exiting");
			return false;
		}
		shoExplain = !config.getSuppressWarnings();
		//
		determineCreationOrder();
		//
		String FShort = xMSet.xU.GetFileName(DDLFileNameIn);
		String sPrefix = (SSTConvertor == true ) ? "SST_" : "";
		FOutputName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "DDL" + xMSet.xU.ctSlash + sPrefix + (srcDBTipe + "2" + tgtDBTipe).toLowerCase() + "-" + FShort; 
		//
		/*
		for( int i=0; i<srcList.size(); i++ )
		{
		    makeCreateTable( srcList.get(i) , srcDBTipe , tgtDBTipe , SSTConvertor);	
		}
		*/
		int globaldiepte =0;
		for(int i=0; i<diepteList.size();i++)
		{
			if( diepteList.get(i).diepte > globaldiepte ) globaldiepte = diepteList.get(i).diepte;
		}
		// Drop
		for(int k=globaldiepte;k>=0;k--)
		{
			for( int i=0; i<diepteList.size(); i++ )
			{
				if( diepteList.get(i).diepte != k ) continue;
				makeCreateTable( srcList.get(diepteList.get(i).idx) , srcDBTipe , tgtDBTipe , SSTConvertor , true);
			}
		}
		//
		for(int k=0;k<=globaldiepte;k++)
		{
			for( int i=0; i<diepteList.size(); i++ )
			{
				if( diepteList.get(i).diepte != k ) continue;
			    makeCreateTable( srcList.get(diepteList.get(i).idx) , srcDBTipe , tgtDBTipe , SSTConvertor , false);	
			}
		}
		//
		if( fout != null ) fout.close();
		logit(5, "DDL Transform [LinesWritten=" + linesWritten +"] to [" + FOutputName + "]");
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean openOutput( rdbmsDatatype.DBMAKE srcDBTipe , rdbmsDatatype.DBMAKE tgtDBTipe , boolean sstconvertor)
	//----------------------------------------------------------------
	{
		fout = new gpPrintStream( FOutputName , "ASCII");
	    fout.println("-- Application : " + "DDLCONVERTOR " + xMSet.getApplicationId());
	    fout.println("-- Source      : " + ScriptInName);
	    fout.println("-- From        : " + srcDBTipe );
	    fout.println("-- To          : " + tgtDBTipe );
	    fout.println("-- Netezza SST : " + sstconvertor );
	    fout.println("-- Project     : " + xMSet.getCurrentProject());
	    fout.println("-- Created on  : " + xMSet.xU.prntStandardDateTime(System.currentTimeMillis()));
	    fout.println("-- Created by  : " + xMSet.whoami());
	    fout.println("-- Config      :");
	    fout.println( config.dumpConfig() );
	    fout.println("--");
	    return true;
	}
	
	//----------------------------------------------------------------
	private boolean write(String sLn)
	//----------------------------------------------------------------
	{
		if( fout != null) fout.println( sLn ); else logit(1,sLn);
        linesWritten++;
		return true;
	}
	
	
	//----------------------------------------------------------------
	private String getOwnerName(String ownerin )
	//----------------------------------------------------------------
	{
		String overruled = config.getOwnerFromConfig();
		String sOwner = overruled == null ? ownerin : overruled;
		return sOwner;
	}
	
	//----------------------------------------------------------------
	private String getDatabaseName(String dbin )
	//----------------------------------------------------------------
	{
		String overruled = config.getDatabaseNameFromConfig();
		String sDatabase = overruled == null ? dbin : overruled;
		return sDatabase;
	}
	
	//----------------------------------------------------------------
	private String getTipeFromConfig(String tipein , int prec , int scale , rdbmsDatatype.DBMAKE tgtDBTipe )
	//----------------------------------------------------------------
	{
		String sPrecScale = "";
		if( prec > 0) {
			if( scale > 0 ) {
				sPrecScale = "(" + prec + "," + scale + ")";
			}
			else sPrecScale = "(" + prec  + ")";
		}
		String sTemp = tipein.trim();
		// specific branch for NUMBER => the overrule left side must be explicite, e.g. NUMBER(12) -> smallint
		if( sTemp.startsWith("NUMBER") ) {
			if((prec > 0) && (scale <= 0))  {  // only applies for NONE-Decimals, exclude NUMBER(n,M) for exact match
			   sTemp = "NUMBER" + sPrecScale;			
			}
			//errit( "" + tipein + " " + prec +" " + scale +" "+sTemp);
		}
		
		String overruled = config.getOverruleDataType( sTemp );
		if( overruled == null ) return null; // identificeert dat er geen overrule is
		
		// Must the precision be added
		// Transform the tipes so that these match the itnernal format
		String sTransDatatipe =  overruled ;
		if( overruled.compareToIgnoreCase("NATIONAL CHARACTER VARYING") == 0 ) sTransDatatipe = "NVARCHAR";
		if( overruled.compareToIgnoreCase("CHARACTER VARYING") == 0 ) sTransDatatipe = "VARCHAR";
		//
		pcDevBoosterSettings.TRISTATE tri = rdtipe.expectPrecision( tgtDBTipe , sTransDatatipe );
		if( (tri == pcDevBoosterSettings.TRISTATE.YES) && (sTemp.startsWith("NUMBER")==false) ) {
			overruled += sPrecScale;		
		}
		return overruled;
	}
	
	
	//----------------------------------------------------------------
	private int getIdxViaName( String stab)
	//----------------------------------------------------------------
	{
		if( stab == null ) return -1;
		for(int i=0;i<srcList.size();i++)
		{
			if( srcList.get(i).Name.compareTo(stab.trim())==0) return i;
		}
		return -1;
	}
	
	
	//----------------------------------------------------------------
	private String getDistKey(String stab )
	//----------------------------------------------------------------
	{
		int idx = getIdxViaName(stab);
		infaSource tab = srcList.get(idx);
		for(int i=0;i<tab.constraintList.size();i++)
		{
			infaConstraint co = tab.constraintList.get(i);
			if( co.Tipe != generatorConstants.CONSTRAINT.PRIMARY ) continue;
			if( co.key_list.size()!=1 ) break;
			return co.key_list.get(0);
		}
		return null;
	}
	
	
	//----------------------------------------------------------------
	private boolean makeCreateTable( infaSource src , rdbmsDatatype.DBMAKE srcDBTipe , rdbmsDatatype.DBMAKE tgtDBTipe , boolean SSTConvertor , boolean isDrop)
	//----------------------------------------------------------------
	{
		// SSTCovertor makes Netezza SST tables with columns all set to nvarchar and calculated size
		// SSTConvertor is only applicable for Netezza target
		if( (SSTConvertor == true) && ( tgtDBTipe != rdbmsDatatype.DBMAKE.NETEZZA ) ) {
			errit("SSTConvertor can only be used to translate to NETEZZA");
			return false;
		}
		//
		if( linesWritten == 0 ) {
			openOutput( srcDBTipe , tgtDBTipe , SSTConvertor);
			linesWritten=1;
		}
		write("");
		//
		String currDatabaseName = getDatabaseName( src.Dbdname );
		String currOwnerName = getOwnerName( src.OwnerName );
		String sDbOwTa = (currDatabaseName == null ) ? "" : currDatabaseName;
		String sTableName = (SSTConvertor == true) ? xMSet.xU.Remplaceer("SST_" + src.Name , "_CCD" , "") : src.Name;
		// obsolete sDbOwTa = ( tgtDBTipe == rdbmsDatatype.DBMAKE.NETEZZA ) ? sDbOwTa + "." + currOwnerName + "." + sTableName :  currOwnerName + "." + sTableName;
		if( tgtDBTipe == rdbmsDatatype.DBMAKE.NETEZZA ) {
	        if( currDatabaseName == null ) currDatabaseName = "";
	        if( currDatabaseName.length() == 0 ) sDbOwTa = ""; else sDbOwTa = currDatabaseName + ".";
	        if( currOwnerName == null ) currOwnerName = "";
	        if( currOwnerName.length() == 0 ) sDbOwTa += ""; else sDbOwTa += currOwnerName + ".";
	        sDbOwTa += sTableName;
		}
		else {
			sDbOwTa = currOwnerName + "." + sTableName;
		}
		//
		if( (isDrop==true) && (config.getSuppressDrops()==false) ) {
			write("DROP TABLE " + sDbOwTa.trim().toUpperCase() + ";");
			return true;
		}
		write("CREATE TABLE " + sDbOwTa.trim().toUpperCase() + " (");
		for(int i=0 ; i< src.fieldList.size();i++)
		{
			infaSourceField f = src.fieldList.get(i);
			String sLn = ( i == 0) ? "  " : ", ";
			sLn += String.format("%-40s" , f.Name.trim().toUpperCase() );
			//
			
			String sPrecScale = "";
			if( f.Precision > 0) {
				if( f.scale > 0 ) {
					sPrecScale = "(" + f.Precision + "," + f.scale + ")";
				}
				else sPrecScale = "(" + f.Precision + ")";
			}
			//sLn += f.DataType +  " " + sPrecScale + " -> ";
			//
			String DDLTipe = getTipeFromConfig( f.DataType, f.Precision , f.scale , tgtDBTipe );
			if( DDLTipe != null ) {
			   if( shoExplain ) write("-- Datatype conversion overruled for [" + f.DataType + sPrecScale + "]");
			}
			else {   // as usual
				String tgtTipe = null;
				if( SSTConvertor ) {   //  Only netezza
					tgtTipe = "NVARCHAR";
					int prec_calc = xMSet.NetezzaSSTSizeNEW( f.Precision );
					sPrecScale = "(" + prec_calc + ")";
				}
				else {
					//logit(9,"set " + f.Name + " " + f.DataType + " P=" + f.Precision + " S=" + f.scale  );			
					tgtTipe = conv.getEquivalentDataType( f.DataType, f.Precision , f.scale , srcDBTipe , tgtDBTipe ).trim().toUpperCase();
					if( tgtTipe == null ) return false;
					//logit(9,"got -->     " + f.DataType + " P=" + f.Precision + " S=" + f.scale + " -> " + tgtTipe );
				}
				// VARCHAR2 and size 1
				if ( (tgtTipe.compareToIgnoreCase("VARCHAR")==0) && (f.Precision == 1) ) tgtTipe = "CHAR";
				if ( (tgtTipe.compareToIgnoreCase("VARCHAR2")==0) && (f.Precision == 1) ) tgtTipe = "CHAR";
				if ( (tgtTipe.compareToIgnoreCase("NVARCHAR")==0) && (f.Precision == 1) ) tgtTipe = "NCHAR";
				//
				//
				if( tgtTipe.trim().toUpperCase().startsWith("NUMBER-") ) {
					int prec = xMSet.xU.NaarInt( xMSet.xU.Remplaceer(tgtTipe.trim().toUpperCase() , "NUMBER-" , "") );
					DDLTipe = "NUMBER (" + prec + ")";    	
				}
				else {
					DDLTipe = tgtTipe;
					if( conv.expectPrecision(tgtTipe) == true ) {
						DDLTipe += sPrecScale;
					}
				}
			}
			//
			//  Enforce the rule on avoiding NVARCHAR(1)
			if( (tgtDBTipe == rdbmsDatatype.DBMAKE.NETEZZA) && (f.Precision == 1) && (DDLTipe.toUpperCase().indexOf("CHAR")>=0) ) {
				if( shoExplain ) write("-- " + f.DataType + " => " + DDLTipe + " has been forced to set to CHAR(1) : Precision is 1");
				 DDLTipe = "CHAR";
			}
			//
			sLn += " " + DDLTipe;
			//
			if( f.mandatory ) sLn += " NOT NULL";
			write( sLn );
		}  // einde cols
		// add MD5 Checksum if SST
		if( SSTConvertor ) { 
			write( ", RECORD_STATUS  NVARCHAR(3) NOT NULL");
			write( ", RECORD_SOURCE  NVARCHAR(30) NOT NULL");
			write( ", BATCH_ID       BIGINT NOT NULL");
			write( ", EDW_CHECKSUM   NVARCHAR(50) NOT NULL");
		}
		// PRIMARY and UNIQUE KEY
		// TODO if PK
		for(int k=0;k<src.constraintList.size();k++)
		{
		  infaConstraint co = src.constraintList.get(k);
		  if( (co.Tipe != generatorConstants.CONSTRAINT.PRIMARY) && (co.Tipe != generatorConstants.CONSTRAINT.UNIQUE) ) continue;
		  String sCon=", CONSTRAINT";
		  sCon += " " + co.Name + " ";
		  if( (co.Tipe == generatorConstants.CONSTRAINT.PRIMARY) ) {
			  if( config.getSuppressPrimaryConstraint() == true ) continue;
			  sCon += " PRIMARY KEY ("; 
		  }
		  else {
			  if( config.getSuppressUniqueConstraint() == true ) continue;
			  sCon += " UNIQUE (";
		  }
		  for(int z=0;z<co.key_list.size();z++)
		  {
			if( z!=0 ) sCon += ",";
			sCon += co.key_list.get(z);
		  }
		  sCon += ")";
		  write(sCon);
		}
		// FOREIGN Keys
		if( config.getSuppressForeignConstraint() == false ) 
		{
		 for(int k=0;k<src.constraintList.size();k++)
		 {
		  infaConstraint co = src.constraintList.get(k);
		  if( co.Tipe != generatorConstants.CONSTRAINT.FOREIGN ) continue;
		  String sCon=", CONSTRAINT";
		  sCon += " " + co.Name + " ";
		  sCon += " FOREIGN KEY (";
		  for(int z=0;z<co.key_list.size();z++)
		  {
			if( z!=0 ) sCon += ",";
			sCon += co.key_list.get(z);
		  }
		  sCon += ") REFERENCES ";
		  sCon += co.ReferencedTableName + " (";
		  for(int z=0;z<co.ref_list.size();z++)
		  {
			if( z!=0 ) sCon += ",";
			sCon += co.ref_list.get(z);
		  }
		  sCon += ")";
		  write(sCon);
		 }
		}
		// External table ?
		if( src.flafle.isFixedWidth == true ) {
			fout.println(")");
			if( src.flafle.extTablePositionList == null ) {
						errit( "No POSITION information on [" + src.Name + "]");
						return false;  // not fatal
			}
			for(int k=0;k<src.flafle.extTablePositionList.size();k++)
			{
					  String sL = null;
					  sL = ( k == 0 ) ? "ORGANIZATION EXTERNAL FIELDS ( " + xMSet.xU.ctEOL + "  ": ", ";
					  sL += String.format("%-40s" , src.fieldList.get(k).Name ) + " ";
					  sL += "POSITION (";
					  sL += src.flafle.extTablePositionList.get(k).x + ":" ;
					  sL += src.flafle.extTablePositionList.get(k).y + ")" ;
					  fout.println(sL);
			}
		}
		//
		// For NZ add distribute clause
		if( (SSTConvertor) || (tgtDBTipe == rdbmsDatatype.DBMAKE.NETEZZA) ) { 
			String distkey = getDistKey( src.Name );
			if( distkey == null ) write( ") DISTRIBUTE ON RANDOM;");
			                 else write( ") DISTRIBUTE ON (" + distkey + ");" );
		}
		//
		else   {
		write(");");
		}
		// Comments
		if( config.getSuppressComments() == false )
		{
			int k=0;
			for(int i=0 ; i< src.fieldList.size();i++)
			{
				infaSourceField f = src.fieldList.get(i);
				String sCom = f.Description == null ? "" : f.Description;
				if( sCom.length() < 1 ) continue;
				k++;
				if( k==1 ) write("");
				write( "COMMENT ON COLUMN " + sDbOwTa.trim().toUpperCase() + "." + f.Name + " IS '" + sCom + "';" );
			}		
		}
		write("");
		//
		return true;
	}
	
	//----------------------------------------------------------------
	private void determineCreationOrder()
	//----------------------------------------------------------------
	{
		diepteList = new ArrayList<tabledepth>();
		for(int i=0;i<srcList.size();i++)
		{
			int idx = getIdxViaName( srcList.get(i).Name );
			if( idx < 0 ) {
				errit("Bizar - system error determineOrder");
				continue;
			}
			tabledepth x = new tabledepth();
			x.idx = idx;
			x.diepte = 0;
			diepteList.add(x);
		}
	    for(int i=0;i<diepteList.size();i++)
	    {
	    	globaldiepte=0;
	    	followparent( diepteList.get(i).idx , 0);
	    	diepteList.get(i).diepte = globaldiepte;
	    }
	}
	
	//----------------------------------------------------------------
	private void followparent( int idx , int diepte)
	//----------------------------------------------------------------
	{
		diepte++;
		if( diepte > 100 ) {
			errit("Too many levels in dependency");
			return;
		}
		if( diepte > globaldiepte ) globaldiepte = diepte;
		infaSource src = srcList.get(idx);
		for(int i=0;i<src.constraintList.size();i++)
		{
	      infaConstraint co = src.constraintList.get(i);
		  if( co.Tipe != generatorConstants.CONSTRAINT.FOREIGN ) continue;
		  int pdx = getIdxViaName( co.ReferencedTableName );
		  if( pdx < 0 ) {
			  errit("(followparent) unknown referenced table [" + co.ReferencedTableName + "] on table [" + src.Name + "]");
			  return;
		  }
		  followparent ( pdx , diepte );
		}
	}
	
}
