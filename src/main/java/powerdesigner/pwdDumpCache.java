package powerdesigner;

import java.util.ArrayList;

import generalpurpose.gpPrintStream;
import generalpurpose.pcDevBoosterSettings;

public class pwdDumpCache {
	
	
	pcDevBoosterSettings xMSet=null;
	
	private String sSourceSystem = null;
	private String requestedLayer = null;
	
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
	public pwdDumpCache(pcDevBoosterSettings im)
	//----------------------------------------------------------------
	{
		xMSet = im;
	}

	//----------------------------------------------------------------
	public boolean dumpcache(String srcsys , String slay , ArrayList<pwdTable> list)
	//----------------------------------------------------------------
	{
		requestedLayer = slay;
		sSourceSystem = srcsys;
		return dumpLookup(list);
	}
	
	//----------------------------------------------------------------
	public String getLookupFileName()
	//----------------------------------------------------------------
	{
	  String sDir = xMSet.getProjectDir() + xMSet.xU.ctSlash + "LookUp" + xMSet.xU.ctSlash;
	  String FName = sDir + "Lkp_" + (sSourceSystem + "_" + requestedLayer).trim().toLowerCase() + ".xml";
	  return FName;
	}
	
	//----------------------------------------------------------------
	private String getFileHeader()
	//----------------------------------------------------------------
	{
			 String sLijn = "";
			 sLijn += "-- Application   : " + xMSet.getApplicationId() + xMSet.xU.ctEOL;
		     //sLijn += "-- Source        : " + sXmlFile + xMSet.xU.ctEOL;
		     sLijn += "-- Project       : " + xMSet.getCurrentProject() + xMSet.xU.ctEOL;
		     sLijn += "-- Layer         : " + this.requestedLayer + xMSet.xU.ctEOL;
		     sLijn += "-- Source System : " + sSourceSystem + xMSet.xU.ctEOL;
		     sLijn += "-- Created on    : " + xMSet.xU.prntStandardDateTime(System.currentTimeMillis()) + xMSet.xU.ctEOL;
		     sLijn += "-- Created by    : " + xMSet.whoami() + xMSet.xU.ctEOL;
		     return sLijn;
	}
	
	//----------------------------------------------------------------
	private boolean dumpLookup( ArrayList<pwdTable> table_list)
	//----------------------------------------------------------------
	{
			String FName = this.getLookupFileName();
			gpPrintStream dump = new gpPrintStream( FName , "UTF-8");
			String sLijn = "";
			// XML header
			dump.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			dump.println("<!-- This is a helper file for the create map file module -->" );
			dump.println("<dump><![CDATA[");
			dump.println(getFileHeader());
			//
			dump.println("-- GENERIC INFORMATION");
			dump.println("-- not used yet");
			//
			dump.println("-- TABLE INFORMATION");
			dump.println("TableName|TableCode|SourceFileDelimiter|CR|SourceTableName|");
			for(int i=0;i<table_list.size();i++)
			{
				pwdTable tab = table_list.get(i);
				sLijn = tab.Name + "|" + tab.Code + "|" + tab.SourceFileDelimiter + "|" + tab.CR + "|" + tab.SourceTableName + "|";
				dump.println(sLijn);
			}
			//
			dump.println("-- COLUMN INFORMATION");
			dump.println("TableName|ColumnName|ColumnCode|EDWCheckSum|SourceTargetColumnName|DataType|Mandatory|SourceTargetDataType|SourceTargetMandatory");
			for(int i=0;i<table_list.size();i++)
			{
				pwdTable tab = table_list.get(i);
				for(int j=0;j<tab.col_list.size();j++)
				{
					pwdColumn col = tab.col_list.get(j);
					sLijn = tab.Name + "|" + col.Name + "|" + col.Code + "|" +col.EDWCheckSum + "|" + 
					        col.SourceColumnName + "|" + col.DataType + "|" + col.IsMandatory + "|"
							+ col.SourceDataType + "|" + col.IsSourceMandatory + "|";
					dump.println(sLijn);
				}
			}
			dump.println("-- END OF LOOKUP");
			//
			dump.println("]]></dump>");
			if( dump != null ) dump.close();
			return true;
	}
	
}
