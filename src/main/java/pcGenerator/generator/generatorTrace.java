package pcGenerator.generator;

import generalpurpose.gpPrintStream;
import generalpurpose.pcDevBoosterSettings;

import java.util.ArrayList;

import pcGenerator.powercenter.infaConnector;
import pcGenerator.powercenter.infaTransformation;
import pcGenerator.powercenter.infaTransformationField;

public class generatorTrace {

		pcDevBoosterSettings   xMSet = null;
		gpPrintStream parseout=null;
	    String sDivider = "=======================================================";
	  
	
		//----------------------------------------------------------------
		generatorTrace(pcDevBoosterSettings xi)
		//----------------------------------------------------------------
		{
			 xMSet = xi;
			 //   
			 String FParseFileName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Temp" + xMSet.xU.ctSlash + "ParserTraceFile.txt";//
			 parseout = new gpPrintStream(FParseFileName,"ASCII");
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
		public void trace(String sIn)
		//----------------------------------------------------------------
		{
			logit(5,sIn);
			parseout.println(sIn);
		}
		
		//----------------------------------------------------------------
		public void close()
		//----------------------------------------------------------------
		{
			parseout.close();
		}
		
		//----------------------------------------------------------------
		public void dumpTraceHeader(String sourceName , String targetName)
		//----------------------------------------------------------------
		{
			parseout.println("File        : Parser Trace Dump");
			parseout.println("Application : " + xMSet.getApplicationId());
			parseout.println("Source      : " + sourceName);
			parseout.println("Target      : " + targetName);
			parseout.println("Project     : " + xMSet.getCurrentProject());
			parseout.println("Created on  : " + xMSet.xU.prntStandardDateTime(System.currentTimeMillis()));
			parseout.println("Created by  : " + xMSet.whoami());
			parseout.println("");
		}
		
		//----------------------------------------------------------------
		public void dumpMapList(ArrayList<generatorMapping> list , String sHeader)
		//----------------------------------------------------------------
		{
			trace(sDivider);
			trace(sHeader);
			trace(sDivider);
			try {
			for(int i=0;i<list.size();i++)
			{
				  String sLijn = "MAPPING Field [" + list.get(i).sourceTableIdx + "][" + list.get(i).sourceTableName + "." + list.get(i).sourceField + "] to [" + + list.get(i).targetTableIdx + "]["  + list.get(i).targetTableName + "." + list.get(i).targetField + "] [Rank=" + list.get(i).rank + "]";
				  trace(sLijn);
		 		  ArrayList<generatorInstruction> ilist = list.get(i).instr_list;
		 		  for(int k=0;k<ilist.size();k++)
		 		  {
		 		  sLijn ="        Instr [" + ilist.get(k).stage + "] [tipe=" + ilist.get(k).tipe + "] [rval=" + ilist.get(k).rVal + "] [" + ilist.get(k).fromPortTipe + "] [" + ilist.get(k).toPortTipe + "] " +
		 		         "[SelfType="  + ilist.get(k).selfTipe + "]" +
		                 " [" + ilist.get(k).Datatype + "] [" + ilist.get(k).Precision + "." + ilist.get(k).Scale  + "]" + 
		 		         " [UID=" + ilist.get(k).UID + "]";	  
		 		  trace(sLijn);
		 		  }
		 		  trace("");
			}
			}
			catch(Exception e) {
				errit("Error writing to trace file" + xMSet.xU.LogStackTrace(e));
			}
		}
			
		//----------------------------------------------------------------
		public void dumpTransformationTrace(ArrayList<infaTransformation> list)
		//----------------------------------------------------------------
		{
			trace (sDivider);
			trace ("Transformation objects");
			trace (sDivider);
			for(int k= 0;k<list.size();k++)
			{
				infaTransformation x = list.get(k);
				//
				String sLijn = "[" + x.stage + "] [" + x.TransformationType + "] [Name=" + x.Name +"] [Dbname=" + x.Dbdname + "]";
			    trace(sLijn);
			    for(int i=0;i<x.txnfld_list.size();i++)
			    {
			    	infaTransformationField y = x.txnfld_list.get(i);
			    	sLijn = " [Field=" + y.Name +"] [Datatype=" + y.Datatype + "] [P=" + y.Precision + "." + y.Scale + "] [" + y.Porttype + "] [" + y.Expression + "] [UID=" + y.InstructionUID + "]";
			    	trace( sLijn );
			    }
			    parseout.println("");
			}
			
		}
		
		//----------------------------------------------------------------
		public void dumpConnector(infaConnector c)
		//----------------------------------------------------------------
		{
					
					String sOne = "[" + c.FromInstanceName + "." + c.fromTransformationType + "." + c.FromFieldName + "]";
					String sTwo = "[" + c.toInstanceName + "." + c.toTransformationType + "." + c.toFieldName + "]";
					try {
						sOne = String.format("%65s",sOne);
						sTwo = String.format("%-65s",sTwo);
					}
					catch(Exception e) { 
						errit("String.format() error on trace line - nothing to worry about");
					}
					trace(sOne + " --> " + sTwo  );
		}
		
		//----------------------------------------------------------------
		public void dumpAllConnectors(ArrayList<infaConnector> list)
		//----------------------------------------------------------------
		{
			
			String sPrev = "?";
			for(int i=0;i<list.size();i++)
			{
				if( sPrev.compareToIgnoreCase( list.get(i).FromInstanceName ) != 0 ) {
					sPrev =list.get(i).FromInstanceName;
					trace(sDivider);
					trace("From instance [" + sPrev + "]");
					trace(sDivider);
				}
				dumpConnector ( list.get(i) );
			}
		}
		
}
