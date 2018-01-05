package pcGenerator.ddl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import pcGenerator.powercenter.infaSource;
import pcGenerator.powercenter.infaSourceField;
import generalpurpose.gpUtils;
import generalpurpose.pcDevBoosterSettings;

public class infaImport {
	
	pcDevBoosterSettings xMSet = null;
	gpUtils xU = null;
	
	ArrayList<infaSource> srcList = null;
	readInfaXML rinfa = null;
	
	private boolean isOK=false;

	//----------------------------------------------------------------
	public boolean getCompletionStatus()
	//----------------------------------------------------------------
	{
		return isOK;
	}
	
	//----------------------------------------------------------------
	public infaImport(pcDevBoosterSettings xi , String FInName )
	//----------------------------------------------------------------
	{
		   xMSet = xi;
		   xU = xMSet.xU;
		   //
		   if( xU.IsBestand( FInName ) == false ) {
			   errit("Cannot open [" + FInName + "] for reading.");
			   return;
		   }
		   // Kijk of dit een Informatica XML is
		   if( isInfaXml( FInName ) == false ) {
			   errit("[" + FInName + "] is not a regular PowerCenter XML export");
			   return;
		   }
		   //
		   logit(5,"infaImport [" + FInName + "]" );
		   rinfa = new readInfaXML(xMSet);
		   //	
		   boolean ib = create_infoSets( FInName );
		   if( ib == false ) {
			   errit("An error occurred while processing [" + FInName + "]");
		   }
		   isOK=ib;
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
	private boolean isInfaXml( String FInName )
	//----------------------------------------------------------------
	{
	   String sText = xU.ReadContentFromFile(FInName , 20);
	   int conf=0;
	   if ( sText.indexOf("<!DOCTYPE POWERMART SYSTEM \"powrmart.dtd\">") >= 0 ) conf++;
	   if ( sText.indexOf("<infoset>") >= 0 ) conf++;
	   if( conf == 0 ) return false;
	   return true;
	}
	
	//----------------------------------------------------------------
	private boolean create_infoSets(String FNameIn)
	//----------------------------------------------------------------
	{
		boolean ib = create_SourceSet(FNameIn);
		if( ib == false ) return false;
		//
		ib = create_TargetSet(FNameIn);
		if( ib == false ) return false;
		//
		return true;
	}
		
	//----------------------------------------------------------------
	private boolean create_SourceSet(String FInName)
	//----------------------------------------------------------------
	{
	   srcList = rinfa.parse_Export( FInName , readInfaXML.ParseType.SOURCETABLE , null);
	   if  ( srcList == null ) return false;
	   boolean ib = perform_export(FInName);
	   if (ib == false ) return false;
	   srcList=null;
	   //
	   srcList = rinfa.parse_Export( FInName , readInfaXML.ParseType.SOURCEFLATFILE , null);
	   if  ( srcList == null ) return false;
	   ib = perform_export(FInName);
	   if (ib == false ) return false;
	   srcList=null;
	   //
	   return ib;	
	}
	
	//----------------------------------------------------------------
	private boolean create_TargetSet(String FInName)
	//----------------------------------------------------------------
	{
		srcList = rinfa.parse_Export( FInName , readInfaXML.ParseType.TARGETTABLE , null);
		if  ( srcList == null ) return false;
		boolean ib = perform_export(FInName);
		if (ib == false ) return false;
		srcList=null;
		//
		srcList = rinfa.parse_Export( FInName , readInfaXML.ParseType.TARGETFLATFILE , null);
		if  ( srcList == null ) return false;
		ib = perform_export(FInName);
		if (ib == false ) return false;
		srcList=null;
		//
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean perform_export(String FInName)
	//----------------------------------------------------------------
	{
         writeInfaMetadata mwriter = new writeInfaMetadata( xMSet , FInName );
         return mwriter.createMetadataFiles( srcList );
   }
	
		
	
}
