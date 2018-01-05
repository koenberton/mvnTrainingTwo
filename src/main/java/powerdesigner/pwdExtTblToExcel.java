package powerdesigner;

import java.util.ArrayList;
import java.util.StringTokenizer;

import office.xlsxWriter;
import pcGenerator.ddl.db2Tokenize;
import pcGenerator.ddl.rdbmsDatatype;
import pcGenerator.powercenter.infaSource;
import pcGenerator.powercenter.infaSourceField;
import generalpurpose.gpPrintStream;
import generalpurpose.pcDevBoosterSettings;

public class pwdExtTblToExcel {
	
	 	// This is a HELPER application to transform an Oracle External Table DDL into Excel format
		// It uses db2Tokenize to parse a DDL into a list of infaSource 
		// The infaSource list is then used to create a headstart Excel for mapping the FLatFiles
	 	pcDevBoosterSettings xMSet=null;
	    private xlsxWriter excel = null;
	 	private String sDDLScript = null;
	 	private ArrayList<infaSource> extList = null;
	 	private int outcntr = 0;
	 	private String curr_mapsheet = null;
	 	
		private String[] uchp = new String[] {
	 			"CAMPAIGN_CCD"			,  "CAMPAIGN",
	 			"CAMPAIGNCOST_CCD"		,  "CAMPAIGN_COST_CCD",      // no longer used
	 			"CAMPAIGNCAUSALPART_CCD",  "CAMPAIGNCAUSALPART",
	 			"CAMPAIGNIMPREGION_CCD"	,  "CAMPAIGNIMPREGION",
	 			"CAMPAIGNLABOUR_CCD"	,  "CAMPAIGNLABOUR",
	 			"CAMPAIGNMATERIAL_CCD"	,  "CAMPAIGNMATERIAL",
	 			"CAMPAIGNTSU_CCD"		,  "CAMPAIGNTSU",
	 			"CAMPAIGNVEHINF_CCD"	,  "CAMPAIGNVEHINFO",
	 			"CAMPAIGNVEHTYPE_CCD"	,  "CAMPAIGNVEHTYPE",
	 			"CLAIMCOST_CCD"			,  "CLAIMCOST",
	 			"CLAIMCOMMENT_CCD"		,  "CLAIMCOMMENT",
	 			"CLAIMCOSTDEBIT_CCD"	,  "CLAIMCOSTDEBIT",
	 			"CLAIMJOBRECPROBLEM_CCD",  "CLAIMJOBRECPROBLEM",
	 			"CLAIMJOBHISTORY_CCD"   ,  "CLAIMJOBHISTORY",
	 			"CLAIMJOBSUPPLEMENT_CCD",  "CLAIMJOBSUPPLEMENT",
	 			"CLAIMJOBTMA_CCD"  		,  "CLAIMJOBTMA" ,
	 			"CLAIMLABOUR_CCD"  		,  "CLAIMLABOUR",
	    		"CLAIMLABOURDEBIT_CCD"  ,  "CLAIMLABOURDEBIT",
	    		"CLAIMMATERIAL_CCD"  	,  "CLAIMMATERIAL",
	    		"CLAIMMATERIALDEBIT_CCD",  "CLAIMMATERIALDEBIT",
	    		"CLAIMMATERIALTMA_CCD"  ,  "CLAIMMATERIALTMA",
	    		"CLAIMREPAIRHEA_CCD"  	,  "CLAIMREPAIRHEADER",
	    		"CLAIMRETURN_CCD"  		,  "CLAIMRETURN",
	    		"CLAIMTRAVEL_CCD"  		,  "CLAIMTRAVEL",
	    		"CLAIMTRAVELDEBIT_CCD"  ,  "CLAIMTRAVELDEBIT",
	    		"CMPVEHSELECTION_CCD"  	,  "CMPVEHSELECTION",
	    		"COSTADJUSTFACTOR_CCD"  ,  "COSTADJUSTFACTOR",
	    		"COVERAGE_CCD"  		,  "COVERAGE",
	    		"COVERAGETEXT_CCD"  	,  "COVERAGETEXT",
	    		"CREDITOCC_CCD"			,  "CREDITOCC",
	    		"CREDITOCCCLAIMJOB_CCD"	,  "CREDITOCCCLAIMJOB",
	    		"CREDITOCCDEBITCODE_CCD",  "CREDITOCCDEBITCODE",
	    		"CREDITOCCDEALER_CCD"  	,  "CREDITOCCDEALER",
	    		"CREDOCCCLAIMJOBVAT_CCD",  "CREDOCCCLAIMJOBVAT",
	    		"CURRENCY_CCD"  		,  "CURRENCY",
	    		"DEALER_CCD"  			,  "DEALER",
	    		"DEALERINTERNAL_CCD"  	,  "DEALERINTERNAL",
	    		"DEBITCODE_CCD"			,  "DEBITCODE" ,
	    		"DEFECTCODE_CCD"		,  "DEFECTCODE",
	    		"FOLLOWUPCODE_CCD"  	,  "FOLLOWUPCODE",
	    		"JOB1_CCD"  			,  "CLAIMJOB",
	    		"JOB2_CCD"  			,  "CLAIMJOB1",
	    		"JOB3_CCD"  			,  "CLAIMJOB2",
	    		"JOB4_CCD"  			,  "CLAIMJOB3",
	    		"JOB5_CCD"  			,  "CLAIMJOB4",
	    		"JOB6_CCD"  			,  "CLAIMJOB5",
	    		"INSPECTORREPORT_CCD"   ,  "INSPECTORREPORT",
	    		"MATERIALINSTR_CCD"     ,  "MATERIALINSTR",
	    		"MATERIALREQ_CCD"  		,  "MATERIALREQ",
	    		"OPERATION_CCD"  		,  "OPERATION",
	    		"RECEIVINGPROBLEM_CCD"  ,  "RECEIVINGPROBLEM",
	    		"REJECTCODE_CCD"		,  "REJECTCODE",
	    		"RETURNCODE_CCD"		,  "RETURNCODE",
	    		"REJECTCODETMA_CCD"  	,  "REJECTCODETMA",
	    		"REQUESTERTMA_CCD"  	,  "REQUESTERTMA",
	    		"STAIRCONNECTION_CCD"  	,  "STAIRCONNECTION",
	    		"STAIRS_CCD"  			,  "STAIRS",
	    		"STAIRSTEP_CCD"  		,  "STAIRSTEP",
	    		"STORAGECLAIMJOBTMA_CCD",  "STORAGECLAIMJOBTMA",
	    		"STORAGELOCATION_CCD"  	,  "STORAGELOCATION",
	    		"SUPPLIER_CCD"  		,  "SUPPLIER",
	    		"TMASTATELOG_CCD"  		,  "TMASTATELOG",
	    		"TMASYSTEMPARAM_CCD"  	,  "TMASYSTEMPARAM",
	    };
/*
	 	 	private String[] uchp = new String[] {
	 			"CAMPAIGN_CCD"			,  "CAMPAIGN",
	 			"CAMPAIGNCOST_CCD"		,  "CAMPAIGN_COST_CCD",
	 			"CAMPAIGNCAUSALPART_CCD",  "CAMPAIGN_CAUSAL_PART",
	 			"CAMPAIGNIMPREGION_CCD"	,  "CAMPAIGN_IMPORTER_REGION",
	 			"CAMPAIGNLABOUR_CCD"	,  "CAMPAIGN_LABOUR",
	 			"CAMPAIGNMATERIAL_CCD"	,  "CAMPAIGN_MATERIAL",
	 			"CAMPAIGNTSU_CCD"		,  "CAMPAIGN_TSA",
	 			"CAMPAIGNVEHINF_CCD"	,  "CAMPAIGN_VEHICLE_INFO",
	 			"CAMPAIGNVEHTYPE_CCD"	,  "CAMPAIGN_VEHICLE_TYPE",
	 			"CLAIMCOST_CCD"			,  "CLAIM_COST",
	 			"CLAIMCOMMENT_CCD"		,  "CLAIM_COMMENT",
	 			"CLAIMCOSTDEBIT_CCD"	,  "CLAIM_COST_DEBIT",
	 			"CLAIMJOBRECPROBLEM_CCD",  "CLAIM_JOB_RECEIVING_PROBLEM",
	 			"CLAIMJOBHISTORY_CCD"   ,  "CLAIM_JOB_STATE_HISTORY",
	 			"CLAIMJOBSUPPLEMENT_CCD",  "CLAIM_JOB_SUPPLEMENT",
	 			"CLAIMJOBTMA_CCD"  		,  "CLAIM_JOB_TMA" ,
	 			"CLAIMLABOUR_CCD"  		,  "CLAIM_LABOUR",
	    		"CLAIMLABOURDEBIT_CCD"  ,  "CLAIM_LABOUR_DEBIT",
	    		"CLAIMMATERIAL_CCD"  	,  "CLAIM_MATERIAL",
	    		"CLAIMMATERIALDEBIT_CCD",  "CLAIM_MATERIAL_DEBIT",
	    		"CLAIMMATERIALTMA_CCD"  ,  "CLAIM_MATERIAL_TMA",
	    		"CLAIMREPAIRHEA_CCD"  	,  "CLAIM_REPAIR_HEADER",
	    		"CLAIMRETURN_CCD"  		,  "CLAIM_RETURN",
	    		"CLAIMTRAVEL_CCD"  		,  "CLAIM_TRAVEL",
	    		"CLAIMTRAVELDEBIT_CCD"  ,  "CLAIM_TRAVEL_DEBIT",
	    		"CMPVEHSELECTION_CCD"  	,  "CAMPAIGN_VEHICLE_SELECTION",
	    		"COSTADJUSTFACTOR_CCD"  ,  "COST_ADJUSTMENT_FACTOR",
	    		"COVERAGE_CCD"  		,  "COVERAGE_SPECIFICATION",
	    		"COVERAGETEXT_CCD"  	,  "COVERAGE_TEXT",
	    		"CREDITOCC_CCD"			,  "CREDIT_OCCASION",
	    		"CREDITOCCCLAIMJOB_CCD"	,  "CREDIT_OCCASION_CLAIM_JOB",
	    		"CREDITOCCDEBITCODE_CCD",  "CREDIT_OCCASION_DEBIT_CODE",
	    		"CREDITOCCDEALER_CCD"  	,  "CREDIT_OCCASION_DEALER",
	    		"CREDOCCCLAIMJOBVAT_CCD",  "CREDIT_OCCASION_CLAIM_JOB_VAT",
	    		"CURRENCY_CCD"  		,  "CURRENCY",
	    		"DEALER_CCD"  			,  "REPAIRING_DEALER",
	    		"DEALERINTERNAL_CCD"  	,  "DEALERINTERNAL",
	    		"DEBITCODE_CCD"			,  "DEBIT_CODE" ,
	    		"DEFECTCODE_CCD"		,  "DEFECT_CODE",
	    		"FOLLOWUPCODE_CCD"  	,  "FOLLOWUP_CODE",
	    		"JOB1_CCD"  			,  "CLAIM_JOB",
	    		"JOB2_CCD"  			,  "CLAIM_JOB1",
	    		"JOB3_CCD"  			,  "CLAIM_JOB2",
	    		"JOB4_CCD"  			,  "CLAIM_JOB3",
	    		"JOB5_CCD"  			,  "CLAIM_JOB4",
	    		"JOB6_CCD"  			,  "CLAIM_JOB5",
	    		"INSPECTORREPORT_CCD"   ,  "INSPECTOR_REPORT",
	    		"MATERIALINSTR_CCD"     ,  "MATERIAL_INSTRUCTION",
	    		"MATERIALREQ_CCD"  		,  "MATERIAL_REQUEST",
	    		"OPERATION_CCD"  		,  "SERVICE_OPERATION",
	    		"RECEIVINGPROBLEM_CCD"  ,  "RECEIVING_PROBLEM",
	    		"REJECTCODE_CCD"		,  "REJECT_CODE",
	    		"RETURNCODE_CCD"		,  "RETURN_CODE",
	    		"REJECTCODETMA_CCD"  	,  "REJECT_CODE_TMA",
	    		"REQUESTERTMA_CCD"  	,  "REQUESTOR_TMA",
	    		"STAIRCONNECTION_CCD"  	,  "STAIR_CONNECTION",
	    		"STAIRS_CCD"  			,  "STAIRS",
	    		"STAIRSTEP_CCD"  		,  "STAIR_STEP",
	    		"STORAGECLAIMJOBTMA_CCD",  "STORAGECLAIMJOBTMA",
	    		"STORAGELOCATION_CCD"  	,  "STORAGE_LOCATION",
	    		"SUPPLIER_CCD"  		,  "SUPPLIER",
	    		"TMASTATELOG_CCD"  		,  "TMASTATELOG",
	    		"TMASYSTEMPARAM_CCD"  	,  "TMASYSTEMPARAM",
	    };
	*/   
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
		public pwdExtTblToExcel( pcDevBoosterSettings im)
		//----------------------------------------------------------------
		{
			xMSet = im;
			excel = new xlsxWriter(xMSet);
		}
	 
		//----------------------------------------------------------------
		private String tamper(String stab)
		//----------------------------------------------------------------
		{
		   if( stab == null) return null;
		   String sTemp = stab.toUpperCase().trim();
		   if( sTemp.endsWith("_CCD") ) {
			   for(int i=0;i<(uchp.length/2);i++)
			   {
				   String zz = uchp[i*2].trim().toUpperCase();
				   if( zz.compareToIgnoreCase( stab ) != 0 ) continue;
				   return uchp[(i*2)+1].trim().toUpperCase();
			   }
		   }
		   //errit("Could not map [" + stab + "] to PowerDesigner name will therefor default it to [" + sTemp + "]");
		   logit(1, "Could not map [" + stab + "] to PowerDesigner name will therefor default it to [" + sTemp + "]");
		   return sTemp;
		}
		
		//----------------------------------------------------------------
		public boolean make_excel(String Fin , String FOutName)
		//----------------------------------------------------------------
		{
			sDDLScript = Fin;
			if( xMSet.xU.IsBestand( sDDLScript ) == false ) {
				errit("Cannot locate the DDL script [" + sDDLScript + "]");
				return false;
			}
			if( FOutName.toUpperCase().endsWith(".XLSX") == false ) {
				errit("Target file must be an XLSX file instead of [" + FOutName + "]");
				return false;
			}
			// Just parse the DDL
			db2Tokenize db2T = new db2Tokenize( xMSet , "DUMMYORCEXT" , rdbmsDatatype.DBMAKE.ORACLE , null );
			extList = db2T.parseFile(sDDLScript);
			if( extList == null ) {
				errit("Parser did not return any table");
				return false;
			}
			//
			if( dump_ext_table_to_excel() == false ) return false;;
			//
			if( excel.dumpToExcel( FOutName ) == false ) return false;
			//
			logit(1,"Written [" + outcntr + "] lines to [" + FOutName + "]");
			//
			return true;
		}

		//----------------------------------------------------------------
		private void writeline(String sIn)
		//----------------------------------------------------------------
		{
			StringTokenizer st = new StringTokenizer(sIn, "|");
			ArrayList<String> list = new ArrayList<String>();
			while(st.hasMoreTokens()) 
			{ 
			  String sElem = st.nextToken().trim();
			  list.add( sElem );  
			}		  
			Object[] oo = new Object[list.size()];
			for(int i=0;i<list.size();i++) 
			{
				oo[i] = list.get(i);
			}
			list=null;
			excel.addRow( curr_mapsheet , oo );
			oo = null;
			outcntr++;
		}
		
		//----------------------------------------------------------------
		private boolean dump_ext_table_to_excel()
		//----------------------------------------------------------------
		{
			String sLijn = "";
			// table info
			curr_mapsheet = "Table";
			excel.addSheet( curr_mapsheet );
			
			writeline( "NAME|SOURCETABLENAME|SOURCEFILEDELIMITER|");
		    for(int i=0 ; i<extList.size() ; i++)
		    {
		    	infaSource src = extList.get(i);
		    	sLijn = tamper(src.Name) + "|" + src.Name + "|null";
		    	writeline( sLijn );
		    }
			
		    //
		    curr_mapsheet = "Table.Column";
		    excel.addSheet( curr_mapsheet );
			
			writeline( "TABLE|NAME|SOURCECOLUMNNAME|DATATYPE|MANDATORY|POSITION|SOURCEDATATYPE|SOURCEMANDATORY");
			for(int i=0 ; i<extList.size() ; i++)
			{
				infaSource src = extList.get(i);
				
				if (src.flafle == null ) {
					errit( "Not a flat file [" + src.Name + "]");
					continue;  // should not occur
				}
				if( src.flafle.isFixedWidth == false ) {
					errit( "Not fixed width [" + src.Name + "]");
					continue;  // should not occur
				}
				if( src.flafle.extTablePositionList == null ) {
					errit( "No POSITION information on [" + src.Name + "]");
					continue;  // not fatal
				}
				
				int prev_stop = 0;
			    for(int j=0;j<src.fieldList.size();j++)
			    {
			    	 infaSourceField fld = src.fieldList.get(j);
			    	 sLijn  = tamper(src.Name) + "|";
			    	 sLijn += fld.Name + "|";
			    	 sLijn += fld.Name + "|";
			    	 //
			    	 String sPrecScale = "";
					 if( fld.Precision > 0) {
						   if( fld.scale > 0 ) {
								sPrecScale = "(" + fld.Precision + "," + fld.scale + ")";
							}
							else sPrecScale = "(" + fld.Precision + ")";
					 }
					 sLijn +=  fld.DataType + sPrecScale + "|";
			    	 //
			    	 sLijn += fld.mandatory + "|";
			         //
			    	 int start = src.flafle.extTablePositionList.get(j).x;
			    	 int stop  = src.flafle.extTablePositionList.get(j).y; 
			    	 sLijn += "POSITION(" + start + ":" + stop + ")";
			    	 // check
					 if( prev_stop != (start - 1) ) {
						 writeline("Error: [" + src.Name + "." + fld.Name + "] Start position  [" + start + "] not aligned with preceding end position [" + prev_stop + "]|");
					 }
					 if( start > stop ) {
						 writeline("Error: [" + src.Name + "." + fld.Name + "] Start position [" + start + "] is higher than end position [" + stop + "]|");
					 }
					 prev_stop = stop;
					 //
					 sLijn += "|";
					 sLijn += fld.DataType + sPrecScale + "|";   // nogmaals
					 sLijn += fld.mandatory;
					 //
		             writeline( sLijn );
		        }
			    writeline("|");		
			}
			 
			 
			return true;
		}
}
