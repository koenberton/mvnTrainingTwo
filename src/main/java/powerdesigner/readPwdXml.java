package powerdesigner;

import java.util.ArrayList;



import java.util.StringTokenizer;

import generalpurpose.gpPrintStream;
import generalpurpose.gpSAX;
import generalpurpose.pcDevBoosterSettings;

/*
 * Caution : Uses the SAX utility, which relies on the Callback functions (endNode/startNode/nodelogit) 
*/


public class readPwdXml {

	boolean DEBUG = false;
	boolean DQINFOPATCH=false;  // true = EDW_DQ_INFO not created
	
	pcDevBoosterSettings xMSet=null;
	private gpPrintStream fout   = null;
	
	enum LAYER    { ORCLEXT , SRC , SST , DVST , STOV , DELTA , DMST , DM}
	
	enum TABSHEET { UNKNOWN, TABLE, COLUMN , REFERENCE , REFERENCEJOIN , KEY , REFERENCE2 }
	
	enum EXCLCOL    { // Table sheet
		            T_NAME, T_CODE, T_COMMENT, T_ENTITYL0, T_ENTITYL1, T_ISHUB, T_ISLINK, 
		            T_HASREFERENCE, T_ISREFERENCED, T_ISSATTELITE, 
		            T_ISREFERENCE, T_SOURCEFILEDELIMITER, T_SOURCEFILENAME, T_SOURCETABLENAME , T_CR, T_CR_HIST, T_VIEW , T_OWNER ,
		            // Column sheet
		            C_TABLE, C_NAME, C_DATATYPE, C_CODE, C_COMMENT, C_LENGTH, C_PRECISION,
		            C_PRIMARY, C_MANDATORY, C_COLUMN_SEQUENCE, C_BUSINESSLONGDESC, C_BUSINESSSHORTDESC,
		            C_COMPOSEDKEYDEFINITION, C_REFTABLE, C_NEWCOLUMNINDICATOR_SST, C_NEWCOLUMNINDICATOR_DVST, 
		            C_VARCHARCONVERTEDDATATYPE, C_SOURCEFILEPOSITION,
		            C_SOURCEDATATYPE, C_SOURCEMANDATORY,
		            C_EDW_CHECKSUM, C_CR , C_CR_HIST,
		            C_SOURCECOLUMNNAME , C_POSITION ,
		            C_BUSINESSLONGDESCVCE , C_BUSINESSSHORTDESCVCE , C_BUSINESSRULE , C_SOURCENAME , C_SOURCECODE ,
		            // REFERENCE SHEET
		            R_NAME , R_PARENT_TABLE , R_CHILD_TABLE ,
		            // REFERNCE JOIN
		            RJ_PARENT , RJ_PARENT_TABLE_COLUMN , RJ_CHILD_TABLE_COLUMN,
		            // KEY
		            K_TABLE , K_NAME , K_PRIMARY , K_COLUMNS ,
		            // Reference2
		            R2_NAME , R2_PARENT_TABLE , R2_CHILD_TABLE , R2_PARENT_KEY
		            }
	
	
	private TABSHEET tabsheet    = TABSHEET.UNKNOWN;
	private boolean parseOK      = false;
	private int currRowIndex     = -1;
	private int currRowNumber    = -1;
	private int prevRowNumber    = -99;
	private int currColumnNumber = -1;
	private LAYER requestedLayer = null;
	private String sSourceSystem = null;
	private String sXmlFile      = null;
	private int maxColWidth      = 20;
	private int globaldiepte     = 0;
    private int foutTeller       = 0;
    private int MAX_ERROR_THRESHOLD = 20;
    private int callbackcounter  = 0;
    private LAYER CRCCreationLayer = LAYER.SST;   // defines where the CRC is to be created - can be a switch later on
    
	//
	private pwdTable cPwdTable = null;
	private pwdColumn cPwdColumn = null;
	
	ArrayList<pwdTable> table_list = null;
	ArrayList <pwdMartTable> mart_scope_list = null;
	
	//
	class HeaderItem
	{
		int  position=-1;
		String ItemName=null;
		EXCLCOL kolom = null;
	}
	ArrayList<HeaderItem> header = null;
	//
	class tempKey
	{
		String TableName;
		String KeyName;
		boolean isPrimary;
		String ColName;
		TABSHEET Origin;
		tempKey()
		{
			TableName=null;
			KeyName=null;
			isPrimary=false;
			ColName=null;
			Origin=null;   // if TABLE.KEY then the NK must be ignored
		}
	}
	private tempKey cTempKey = null;
	ArrayList<tempKey> temp_keystack = null;
	
	class tempRefJoin
	{
		String RefName=null;
		String ParentTableColumn=null;
		String ChildTableColumn=null;
		TABSHEET Origin=null;
		tempRefJoin()
		{
			RefName=null;
			ParentTableColumn=null;
			ChildTableColumn=null;
			Origin=null;
		}
	}
	private tempRefJoin cTempRefJoin = null;
	ArrayList<tempRefJoin> temp_refjoinstack = null;
	
	class tabledepth
	{
		String tablename=null;
		int idx=-1;
		int depth=-1;
	}
	ArrayList<tabledepth> tab_diepte = null;
	
	//----------------------------------------------------------------
	private EXCLCOL getColConstant(String sD)
	//----------------------------------------------------------------
	{
	    String sRet = ( sD == null ) ? "null" : sD.trim().toUpperCase(); 
		for(int i=0;i<EXCLCOL.values().length;i++)
		{
			if( EXCLCOL.values()[i].toString().compareToIgnoreCase(sRet) == 0 ) return EXCLCOL.values()[i];
		}
		errit("Unsupported columns name  [" + sRet + "]");
		return null;
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
	   foutTeller++;
	}
		
	//----------------------------------------------------------------
	public readPwdXml(pcDevBoosterSettings xi )
	//----------------------------------------------------------------
	{
		xMSet = xi;
		table_list = new ArrayList<pwdTable>();
		temp_keystack = new ArrayList<tempKey>();
		temp_refjoinstack = new ArrayList<tempRefJoin>();
	}

	// This is function holds the main logic
	//----------------------------------------------------------------
	public boolean importXml(String FLongName , String sTipe , String sSrcSysIn )
	//----------------------------------------------------------------
	{
			long startt = System.currentTimeMillis();
		    parseOK=true;
			sSourceSystem = sSrcSysIn;
			sXmlFile= FLongName;
			//
			removeLookup();
			//
			requestedLayer = null;
			//if( sTipe.compareToIgnoreCase("FLATFILE") == 0 ) requestedLayer = LAYER.FLATFILE;
			if( sTipe.compareToIgnoreCase("STOV") == 0 ) requestedLayer = LAYER.STOV;
			if( sTipe.compareToIgnoreCase("DELTA") == 0 ) requestedLayer = LAYER.DELTA;
			if( sTipe.compareToIgnoreCase("DM") == 0 ) requestedLayer = LAYER.DM;
			if( sTipe.compareToIgnoreCase("DMST") == 0 ) requestedLayer = LAYER.DMST;
			if( sTipe.compareToIgnoreCase("SST") == 0 ) requestedLayer = LAYER.SST;
			if( sTipe.compareToIgnoreCase("DVST") == 0 ) requestedLayer = LAYER.DVST;
			if( sTipe.compareToIgnoreCase("SRC") == 0 ) requestedLayer = LAYER.SRC;
			if( sTipe.compareToIgnoreCase("ORCLEXT") == 0 ) requestedLayer = LAYER.ORCLEXT;
			if( requestedLayer == null ) {
				parseOK =false;
				errit("Layers supported [ORCLEXT,SRC,SST,DVST,STOV]");
				return false;
			}
			//
			logit(5,"(readPwDXml) [Tipe=" + sTipe + "] [RequestedLayer=" + requestedLayer + "] [Sys=" + sSourceSystem + "]");
			// 
			if( (requestedLayer == LAYER.DELTA) || (requestedLayer == LAYER.DMST) || (requestedLayer == LAYER.DM) ) {
				pwdMartReadFile martrd = new pwdMartReadFile( xMSet );
				if( requestedLayer == LAYER.DM ) {
				  mart_scope_list = martrd.readMartFile("DataMartScope.txt" , false);
				}
				else {
				  mart_scope_list = martrd.readMartFile("DataMartStagingScope.txt" , true);	
				}
				if( mart_scope_list == null ) return false;
				logit(1,"There are [" + this.mart_scope_list.size() + "] tables in scope for the Mart" );
				for(int j=0;j< mart_scope_list.size();j++) logit(1,"Mart table [" + mart_scope_list.get(j).TableName + " -> " + mart_scope_list.get(j).DMEntityName + "]");
			}
		    //		
			if( parseXMLFile ( FLongName ) == false ) return false;
			//
			flushObjects();
			//
			if( DEBUG ) probe("FLUSH");
			//  remove columns and tables not defined in external scoep file
			if ( (requestedLayer == LAYER.DELTA) || (requestedLayer == LAYER.DMST) || (requestedLayer == LAYER.DM) ) {
				if( keepsWhatsInscope(requestedLayer) == false ) return false;
			}
			//
			if( prepareLayers() ==  false ) return false;
			//
			if( (requestedLayer != LAYER.STOV) &&
				(requestedLayer != LAYER.DELTA) &&
				(requestedLayer != LAYER.DMST) &&
				(requestedLayer != LAYER.DM) ) {    // we do not need any keys at all for the STOV
			 //
			 if( getNaturalKeysFromColumnSheet() == false ) return false;
			 //
			 if( consolidateKeys() == false ) return false;
			 //
			 if( getForeignKeysFromColumnSheet() == false ) return false;   // must FOLLOW consolidateKeys
		     //	
			 if( consolidateForeignKeys() == false ) return false;
			}
			//
errit( "Result of Missing columns has been disabled");
			if( patch_missingcolumns() == false ); // return false;
			//
			if( qualityCheck() == false )  return false;
			if( DEBUG ) probe("QC");
			// Fancy sort - voorbereiden
			if ( (requestedLayer == LAYER.DELTA) || (requestedLayer == LAYER.DMST) || (requestedLayer == LAYER.DM) ) {
				if( setDataMartOrder() == false ) return false;
			}
			//
			if( sortColumnOnTable() == false )  return false;
			if( DEBUG ) probe("Sort");
			//
			determineCreationOrder();
			if( DEBUG ) probe("pre-sql");
			//
			if( makeDDL() == false ) return false;
			//
			if( requestedLayer == LAYER.DELTA ) {
				if( makeDeltaView() == false ) return false;
			}
			//
			if( dumpLookup() == false ) return false;
			//
			startt = System.currentTimeMillis() - startt;
			logit(5,"(readPwDXml) Completed [RequestedLayer=" + requestedLayer + "] [Sys=" + sSourceSystem + "] [Elapsed=" + (startt/1000) + "sec]");
			//
			return true;
			
	}
	
	//----------------------------------------------------------------
	private void probe(String plaats)
	//----------------------------------------------------------------
	{
		String tab = "CLAIM_JOB_TMA";
		String col = "ORDERCOMMENT";
		int idx = getTableIdxViaName( tab , "probe");
		if( idx < 0 ) {
			logit(9,"PROBE [" + tab + "." + col + "] found=NOT" );
			return;
		}
		int i = this.getColumnIdx( idx , col , true , "probe");
		logit(9,"PROBE [" + tab + "." + col + "] found=" + i );
	}
	
	//----------------------------------------------------------------
	private boolean prepareSRC()
	//----------------------------------------------------------------
	{
	       for(int i=0;i<table_list.size();i++)
	       {
		    	   pwdTable tab = table_list.get(i);
		    	   for(int j=0 ; j<tab.col_list.size() ; j++)
		    	   {
		    		   String sCol = (tab.col_list.get(j).Name).trim().toUpperCase();
		    		   // There MUST not be a checksum on the SOURCE
		    		   if( sCol.compareToIgnoreCase("EDW_CHECKSUM") == 0 ) {
		    			   tab.col_list.get(j).IsNewColumnIndicator_SST = true;   
		    		   }
		    		   if( sCol.compareToIgnoreCase("EDW_DQ_INFO") == 0 ) {
		    			   tab.col_list.get(j).IsNewColumnIndicator_SST = true;   
		    		   }
		    		   // KB - 22 FEB
		    		   // SAPFM - alleen maar de kolommen met CR1 houden op de SRC
		 			   if( (requestedLayer == LAYER.SRC) && (sSourceSystem.compareToIgnoreCase("SAPFM")==0) ) {
		 				  String inscope = tab.col_list.get(j).CR == null ? "" :  tab.col_list.get(j).CR.trim().toUpperCase();
		 				  if( inscope.compareToIgnoreCase("CR1") != 0 ) {
		 					  tab.col_list.get(j).DataType = "%MUST_BE_REMOVED%";
		 					  continue;
		 				  }
		 			   }
		    		   // 
		    		   //  KB CSS debug sessie - welicht geldig voor alles  new DVSt is new SST
		 			   //  01/04 - disabled the inverse logic
		 			   /*
		    		   if( (sSourceSystem.toUpperCase().trim().startsWith("VCE_SURVEY") == false) &&
		    			   (sSourceSystem.toUpperCase().trim().startsWith("GLAD") == false) ) {
				           // Remove every column that is only new to be created on the SST
 			  	         if( tab.col_list.get(j).IsNewColumnIndicator_SST == false ) continue;
 			  	       }
		    		   else {  // CSS en GLAD
  		    			  if( tab.col_list.get(j).IsNewColumnIndicator_DVST == false ) continue;  
		    		   }
		    		   */
		 			   // remove all columns that are new in DVST
		 			   if( (sSourceSystem.toUpperCase().trim().startsWith("VCE_SURVEY") ) ||
			    		   (sSourceSystem.toUpperCase().trim().startsWith("GLAD") ) ) {
		    			     if( (tab.col_list.get(j).IsNewColumnIndicator_DVST == false) &&   
		    			         (tab.col_list.get(j).IsNewColumnIndicator_SST == false) ) continue;    
	 			  	   }
		    		   else {
		    			    // Remove every column that is only new to be created on the SST
			  	            if( tab.col_list.get(j).IsNewColumnIndicator_SST == false ) continue; 
		    		   }
		    		   
		    		 
		    		   // needs to be removed
		    		   tab.col_list.get(j).DataType = "%MUST_BE_REMOVED%";
		    	   }
	       }
		   return true;
	}
		
	
	// remove all columns that are flagged NEWDVST 
	// and which are not KEY or key because these will be left out in the DDL script creation
	//----------------------------------------------------------------
	private boolean prepareSST()
	//----------------------------------------------------------------
	{
       for(int i=0;i<table_list.size();i++)
       {
    	   pwdTable tab = table_list.get(i);
    	   int remove=0;
    	   for(int j=0 ; j<tab.col_list.size() ; j++)
    	   {
    		   String sCol = (tab.col_list.get(j).Name).trim().toUpperCase();
    		   // If the CRC is not created on the SST then remove the CHECKSUM
    		   if( (sCol.compareToIgnoreCase("EDW_CHECKSUM") == 0) && ( CRCCreationLayer != LAYER.SST) ) {
    			   tab.col_list.get(j).IsNewColumnIndicator_DVST = true;  
    		   }
    		
    		   // Keep the _KEYs for the Foreign key defs
    		   // Remove everything that is only created in the DVST (inverse logic)
    		   if( tab.col_list.get(j).IsNewColumnIndicator_DVST == false ) continue;
    	
    		   // needs to be removed
    		   remove++;
    		   tab.col_list.get(j).DataType = "%MUST_BE_REMOVED%";
    	   }
       }
	   return true;
	}
	//----------------------------------------------------------------
	private boolean prepareSTOV()
	//----------------------------------------------------------------
	{
		return true;
	}
	//----------------------------------------------------------------
	private boolean prepareDELTA()
	//----------------------------------------------------------------
	{
		errit("Prepare Delta - todo");
		return true;
	}
	//----------------------------------------------------------------
	private boolean prepareDMST()
	//----------------------------------------------------------------
	{
		errit("Prepare DMST - todo");
		return true;
	}
	//----------------------------------------------------------------
	private boolean prepareDM()
	//----------------------------------------------------------------
	{
		// for each _KEY make a _SID
		for(int i=0;i<table_list.size();i++)
	    {
	       pwdTable tab = table_list.get(i);
	       for(int j=0 ; j<tab.col_list.size() ; j++)
	  	   {
	    	   String colname = tab.col_list.get(j).Name;
	    	   if( colname == null ) continue;
	    	   String sidName = colname.trim().toUpperCase();
	    	   if( sidName.endsWith("_KEY") == false ) continue;
	    	   sidName = sidName.substring(0,sidName.length()-4);
	    	   if( sidName.compareToIgnoreCase(tab.Name) == 0 ) continue;  // skip if this _KEY is the PK
	    	   sidName = sidName + "_DM_SID";
	    	   int colidx = getColumnIdx( i , sidName , false , "prepareDM");
			   if( colidx >= 0 ) continue;
			   // _SID does not exist
			   if( createColumn( i , sidName ) ) continue;
	           errit( "Required column [" + sidName + "] is missing on [" + tab.Name + "] and cannot be created");
	           return false;
	  	   }
	    }	
		return true;
	}
	//----------------------------------------------------------------
	private boolean prepareLayers()
	//----------------------------------------------------------------
	{
	   if( requestedLayer == LAYER.SRC ) prepareSRC();
	   else
	   if( requestedLayer == LAYER.SST ) prepareSST();
	   else
	   if( requestedLayer == LAYER.STOV ) prepareSTOV();
	   else
	   if( requestedLayer == LAYER.DELTA ) prepareDELTA();
	   else
	   if( requestedLayer == LAYER.DMST ) prepareDMST();
	   else
	   if( requestedLayer == LAYER.DM ) prepareDM();
	   else return true;
	   // remove
 	   int remove=0;
 	   for(int i=0;i<table_list.size();i++)
       {
    	   pwdTable tab = table_list.get(i);
    	   for(int j=0 ; j<tab.col_list.size() ; j++)
  		   {
  			   if( tab.col_list.get(j).DataType.trim().compareToIgnoreCase("%MUST_BE_REMOVED%") == 0 ) remove++;
  		   }
       }	
 	   for(int k=0 ; k<remove ; k++)
 	   {
 		   for(int i=0;i<table_list.size();i++)
 	       {
 	    	   pwdTable tab = table_list.get(i);
 	    	   for(int j=0 ; j<tab.col_list.size() ; j++)
     		   {
     			   if( tab.col_list.get(j).DataType.trim().compareToIgnoreCase("%MUST_BE_REMOVED%") != 0 ) continue;
     			   logit(1,"Removed [" + tab.Name + "." + tab.col_list.get(j).Name + "] defined to be a New SST/DVST field");
     			   tab.col_list.remove( j );
     			   break;
     		   }
 	       }	  
 	   }
	   //
	   return true;
	}
	
	//----------------------------------------------------------------
	private int getColumnIdx( int tabidx , String colname , boolean verbose , String CallingFunc)
	//----------------------------------------------------------------
	{
		if( (tabidx <0) || (tabidx >= table_list.size()) ) {
			errit("Cannot find [" + colname +"] on table [" + tabidx +"]. Index out of bound.");
			return -1;
		}
		for(int i=0;i<table_list.get(tabidx).col_list.size();i++)
		{
		if( table_list.get(tabidx).col_list.get(i).Name.compareToIgnoreCase( colname ) == 0 ) return i;
		}
		if( verbose ) errit("(" + CallingFunc + ") Cannot find [" + colname +"] on table [" + table_list.get(tabidx).Name  +"]. Unknown column.");
		return -1;
	}
	
	//----------------------------------------------------------------
	private boolean qualityCheck()
	//----------------------------------------------------------------
	{
		// zijn key columns geldige velden 
		// verwijzen de foreign keys naar geldige NKs of PKs
		boolean isOK=true;
		if( checkKeyColumns() == false ) isOK = false;
		//
		if( checkReferenceColumns() == false ) isOK = false;
	    //
		errit("checkNetezzaSizes disabled");
		//checkNetezzaSizes();
		//
		if( checkSourceTableAndColumns() == false )  isOK = false;
		//
		return isOK;
	}
	
	//----------------------------------------------------------------
	private boolean checkSourceTableAndColumns()
	//----------------------------------------------------------------
	{
		// SourceTableName is required
		//if( (this.requestedLayer != readPwdXml.LAYER.SST) && (this.requestedLayer != readPwdXml.LAYER.ORCLEXT) ) retrun true;
			
		boolean isok=true;
		for(int i=0;i<table_list.size();i++)
		{
		  pwdTable tab = table_list.get(i);
		  if( tab.SourceTableName == null ) {
			  errit("SourceTableName is NULL on [" + tab.Name + "]");
			  isok=false;
		  }
		  else
		  if( tab.SourceTableName.trim().length() <= 0 ) {
			  errit("SourceTableName is EMPTY on [" + tab.Name + "]");
			  isok=false;
		  }
		  // cols
		  for(int j=0;j<tab.col_list.size();j++)
		  {
			  pwdColumn col = tab.col_list.get(j);
			  if( col.SourceColumnName == null ) {
				  errit("SourceColumnName is NULL for [" + tab.Name + "." + col.Name + "]");
				  isok=false;
			  }
			  else
			  if( col.SourceColumnName.trim().length() <= 0 ) {
				  errit("SourceColumnName is EMPTY for [" + tab.Name + "." + col.Name + "]");
				  isok=false;
			  }
			  // datatypes check
			  if( IsDataTypeOk( col.DataType ) == false ) {
				  errit("Error on the datatype/precision/Scale for [" + tab.Name + "." + col.Name + "] [Datatype=" + col.DataType + "]");
				  isok=false;
			  }
			  // VARCHARCONVERTED check 
			  if( IsVarCharConvertedOk( col.VarcharConvertedDataType , col.Name ) == false ) {
				  errit("Error on the varcharconverted datatype for [" + tab.Name + "." + col.Name + "] [Datatype=" + col.DataType + "]");
				  isok=false;
			  }
		  }
		}	
		return isok;
	}
	
	// KB 30 NOV - Native datatypes force to perform a more rigorous check
	//----------------------------------------------------------------
	private boolean IsDataTypeOk(String sin)
	//----------------------------------------------------------------
	{
		String sTipe = sin == null ? "" : sin.toUpperCase().trim();
	    if( sTipe.length() == 0 ) {
	    	errit("(IsDataTypeOk) Datatype is EMPTY or NULL");
	    	return false;
	    }
		boolean gotComma = sTipe.indexOf(",")>=0 ? true : false;
		if( (sTipe.indexOf("(")>=0) && (sTipe.indexOf(")")<0) ) {
			errit("(IsDataTypeOk) Missing right parentesis");
	    	return false;
		}
		if( (sTipe.indexOf("(")<0) && (sTipe.indexOf(")")>=0) ) {
			errit("(IsDataTypeOk) Missing left parentesis");
	    	return false;
		}
	    boolean gotParentesis =  (sTipe.indexOf("(")>=0) && (sTipe.indexOf(")")>=0) ? true : false;
	    
	    if( (sTipe.indexOf("NUMERIC")>=0) || (sTipe.indexOf("DECIMAL")>=0) ) {
	    	if( (gotComma == true) && (gotParentesis==true) ) {
	    		int iPrec  = xMSet.xU.extractPrecision(sTipe.substring("NUMERIC".length()));
				int iScale = xMSet.xU.extractScale(sTipe.substring("NUMERIC".length()));
				if( (iPrec<=0) || (iScale<=0) ) {
				  errit("(IsDataTypeOk) precision and/or scale cannot be extracted [P=" + iPrec + "] [S=" + iScale + "]");
				  return false;
				}
	    		return true;
	    	}
	    	errit("(IsDataTypeOk) Missing comma, parentesis, precision or scale");
	    	return false;
	    }
	    if( (sTipe.indexOf("CHAR")>=0) || (sTipe.indexOf("STRING")>=0) ) {
	    	if( gotParentesis==true ) return true;
	    	errit("(IsDataTypeOk) Missing parentesis or precision");
	    	return false;
	    }
	    // datatype zondermeer
	    if( gotComma == true ) {
	    	// ORCLEXT and NUMBER
	    	if( requestedLayer == LAYER.ORCLEXT ) {
	    		if(sTipe.indexOf("NUMBER") >= 0) return true;
	    	}
	    	errit("(IsDataTypeOk) Precision or scale are not supported on [" + sTipe + "]");
	    	return false;
	    }
	    if( gotParentesis == true ) {
	    	if( requestedLayer == LAYER.ORCLEXT ) {
	    		if(sTipe.indexOf("NUMBER") >= 0) return true;
	    	}
	    	errit("(IsDataTypeOk) Precision is not supported on [" + sTipe + "]");
	    	return false;
	    }
	  
		return true;
	}
	
	// KB 01 DEC - added extra check just to make sure no mishaps occur on the varcharconverted
	//----------------------------------------------------------------
	private boolean IsVarCharConvertedOk(String sin , String colname)
	//----------------------------------------------------------------
	{
		if( colname.compareToIgnoreCase("BATCH_ID") == 0 ) return true;
		if( colname.compareToIgnoreCase("FILE_LINE_NUMBER") == 0) return true;  // voor CCS
		if( requestedLayer != LAYER.SST ) return true;
		String sTipe = sin == null ? "" : sin.toUpperCase().trim();
	    if( sTipe.length() == 0 ) {
	    	errit("(IsVarCharConverted) Datatype is EMPTY or NULL");
	    	return false;
	    }
		if( sTipe.indexOf("VARCHAR") >= 0 ) return true;
		if( sTipe.indexOf("CHAR") >= 0 ) return true;
		return false;
	}	
	
	//----------------------------------------------------------------
	private boolean checkKeyColumns()
	//----------------------------------------------------------------
	{
		boolean isOK=true;
		// validate column names on the keys and foreign keys
		for(int i=0;i<table_list.size();i++)
		{
			pwdTable tab = table_list.get(i);
			int tabidx = this.getTableIdxViaName( tab.Name , "checkKeyColumns1");
			if( tabidx < 0 ) { isOK = false; continue; }
			for(int j=0;j<tab.key_list.size();j++)
			{
				pwdKey key = tab.key_list.get(j);
				// KB  - fix - geldig voor alles wellicht
				if( (sSourceSystem.toUpperCase().trim().startsWith("VCE_SURVEY")) ||
					(sSourceSystem.toUpperCase().trim().startsWith("GLAD")) ||
					(sSourceSystem.toUpperCase().trim().startsWith("GLOPPS"))) {   // 24NOV KB added GLOPPS
					if( (requestedLayer == LAYER.SRC) ||(requestedLayer == LAYER.SST) ) {
						if( key.isPrimary )  {
							logit(5,"Skipping check on PK [" + key.KeyName + "] not required ");
							continue;
						}
					}
				}
				if( key.keycol_list.size() == 0 ) {
					errit("There are no key columns on [" + key.KeyName + "]");
					isOK=false;
				}
				for(int k=0;k<key.keycol_list.size();k++)
				{
			        //errit("===> " + key.keycol_list.get(k) );
					int colidx = getColumnIdx( tabidx , key.keycol_list.get(k) , true , "checkKeyColumns2");
					if( colidx < 0 ) { isOK=false; continue; }
				}
			}
		}
		logit(5, "Columname check on KEYS : passed [" + isOK + "]");
		return isOK;
	}
	
	//----------------------------------------------------------------
	private int getKeyIdx( int tabidx , String sConcat , String CallingFunc)
	//----------------------------------------------------------------
	{
		if( (tabidx <0) || (tabidx >= table_list.size()) ) {
			errit("(getKeyIdx) on (" + CallingFunc + ") Table index [" + tabidx +"] out of bound");
			return -1;
		}
		pwdTable tab = table_list.get( tabidx );
		for(int i=0;i<tab.key_list.size();i++)
		{
			pwdKey key = tab.key_list.get(i);
			String sTemp="";
			for(int j=0;j<key.keycol_list.size();j++)
			{
				sTemp += "[" + key.keycol_list.get(j).trim() + "]";
			}
			if( sTemp.compareToIgnoreCase( sConcat ) == 0 ) return i;
		}
		errit("(" + CallingFunc + ") Cannot find key " + sConcat + " on table [" + tab.Name + "]");
		return -1;
	}
	
	//----------------------------------------------------------------
	private boolean checkReferenceColumns()
	//----------------------------------------------------------------
	{
			boolean isOK=true;
			// validate column names on the keys and foreign keys
			for(int i=0;i<table_list.size();i++)
			{
				pwdTable tab = table_list.get(i);
				int tabidx = this.getTableIdxViaName( tab.Name , "checkReferenceColumns");
				if( tabidx < 0 ) { isOK = false; continue; }
				//
				for(int j=0;j<tab.ref_list.size();j++)
				{
					pwdReference r = tab.ref_list.get(j);
					//
					if( r.parent_col_list.size() != r.child_col_list.size() ) {
						errit("The number of Child/Parent columns does not match on [" + r.ConstraintName + "]");
						isOK=false;
						continue;
					}
					// Child
					for(int k=0;k<r.child_col_list.size();k++)
					{
					 int colidx = getColumnIdx( tabidx , r.child_col_list.get(k) , true  , "checkReferenceColumns");
					 if( colidx < 0 ) { isOK=false; continue; }
					}
					// parent
					int prntidx = this.getTableIdxViaName( r.ParentTable , "checkReferenceColumns");
					if( prntidx < 0 ) { isOK = false; continue; }
					String sConcat = "";
					for(int k=0;k<r.parent_col_list.size();k++)
					{
					 int colidx = getColumnIdx( prntidx , r.parent_col_list.get(k) , true , "checkReferenceColumns");
					 if( colidx < 0 ) { isOK=false; continue; }
					 sConcat += "[" +  r.parent_col_list.get(k) + "]";
					}
					// is this a valid KEY on parent
					int pkidx = getKeyIdx( prntidx , sConcat , "checkReferenceColumns [Constraint=" + r.ConstraintName + "]" );
					if( pkidx < 0 ) isOK = false;
				}
				
			}
			logit(5, "Columname check on REFERENCES : passed [" + isOK + "]");
			return isOK;
	}
	
	
	//----------------------------------------------------------------
	private boolean checkNetezzaSizes()
	//----------------------------------------------------------------
	{
		// Only needed for the SST
	   	if( requestedLayer != LAYER.SST ) return true;
	   	//
	   	boolean isOK = true;
	    for(int i=0;i<table_list.size();i++)
		{
			pwdTable tab = table_list.get(i);
			int tabidx = this.getTableIdxViaName( tab.Name , "checkNetezzasizes");
			if( tabidx < 0 ) { isOK = false; continue; }
			//
			for(int j=0;j<tab.col_list.size();j++)
			{
			  pwdColumn col = tab.col_list.get(j);
		      String sOrig  = col.DataType.trim().toUpperCase();
		      if( col.DataType.indexOf("CHAR") < 0 ) continue;
		      String sNetez = col.VarcharConvertedDataType;
		      if( sNetez == null )  {
		    	  errit("STRANGE - null on checkNetezza " + tab.Name + "." + col.Name);
		    	  sNetez = "";
		      }
		      // remove all but numbers
		      sOrig = xMSet.xU.justkeepthenumerics( sOrig );
		      sNetez = xMSet.xU.justkeepthenumerics( sNetez );
		      //
		      int iOrig  = toInt( sOrig );
		      int iNetez = toInt( sNetez );
			  //
		      if( (iOrig<0) || (iNetez<0) ) {
		    	  errit("Datatype length cannot be determined [" + tab.Name + "].[" + col.Name + "] [" + sOrig + "] [" + sNetez + "]");
		    	  isOK=false;
		    	  continue;
		      }
		      int iTransformed = xMSet.NetezzaSSTSizeNEW( iOrig );
		      if( (iTransformed != iNetez) ) {
		    	  errit("Netezza length anomaly on [" + tab.Name + "].[" + col.Name + "] [" + sOrig + "] [" + sNetez + "] " + iTransformed + " " + iNetez);
		    	  isOK=false;
		    	  continue;
		      }
			}
		}	
	    logit(5, "Netezza size check : passed [" + isOK + "]");
	    return true;	// Altijd true
	}
	
	//----------------------------------------------------------------
	private boolean patch_missingcolumns()
	//----------------------------------------------------------------
	{
		if( (requestedLayer != LAYER.DVST) && 
			(requestedLayer != LAYER.SST) &&
			(requestedLayer != LAYER.STOV) &&
			(requestedLayer != LAYER.DELTA) &&
			(requestedLayer != LAYER.DMST) &&
			(requestedLayer != LAYER.DM) ) return true;
		//
		boolean isOK = true;
		boolean needsIBM = (this.sSourceSystem.compareToIgnoreCase("UCHP") == 0) ? true : false;
		for(int i=0;i<table_list.size();i++)
		{
		   pwdTable tab = table_list.get(i);
		   int tabidx = this.getTableIdxViaName( tab.Name , "patchMissingColumns");
		   if( tabidx < 0 ) continue;
		   String colname = null;
		   int MAXMIS = 28;
		   for(int k=0;k<MAXMIS;k++)
		   {
			   switch(k)
			   {
			   case  0 : { colname = (needsIBM == true ) ? "IBMSNAP_OPERATION" : null; break; }
			   case  1 : { colname = (needsIBM == true ) ? "IBMSNAP_LOGMARKER" : null; break; }
			   case  2 : { colname = (needsIBM == true ) ? "IBMSNAP_COMMITSEQ" : null; break; }
			   case  3 : { colname = (needsIBM == true ) ? "IBMSNAP_INTENTSEQ" : null; break; }
			   case  4 : { colname = "RECORD_STATUS"; break; }
			   case  5 : { colname = "RECORD_SOURCE"; break; }
			   case  6 : { colname =  (requestedLayer != LAYER.DM) ? "BATCH_ID" : null; break; }
			   case  7 : {  if( requestedLayer == LAYER.SRC ) colname = null;
			   				else
				 			if( requestedLayer == LAYER.ORCLEXT ) colname = null;
				 			else
				 			if( requestedLayer == LAYER.SST ) {
				 				if( CRCCreationLayer != LAYER.SST ) colname = null;
				 			} 
				 			else colname = "EDW_CHECKSUM";
				 			break; 
				 		 }
			   //
			   case  8 : { colname = ((requestedLayer == LAYER.STOV)||(requestedLayer == LAYER.DELTA)||(requestedLayer == LAYER.DMST)) ? "LOAD_ST_DATETIME" : null; break; }
			   case  9 : { colname = ((requestedLayer == LAYER.STOV)||(requestedLayer == LAYER.DELTA)||(requestedLayer == LAYER.DMST)) ? "LOAD_END_DATETIME" : null; break; }
			   case  10: { colname = ((requestedLayer == LAYER.STOV)||(requestedLayer == LAYER.DELTA)||(requestedLayer == LAYER.DMST)) ? "DV_CURRENT_FLAG" : null; break; }
			   case  11: { colname = ((requestedLayer == LAYER.STOV)||(requestedLayer == LAYER.DELTA)||(requestedLayer == LAYER.DMST)) ? "DV_VERSION" : null; break; }
			   case  12: { colname = ((requestedLayer == LAYER.STOV)||(requestedLayer == LAYER.DELTA)||(requestedLayer == LAYER.DMST)) ? "LOAD_DATETIME" : null; break; }
			   case  13: { colname = ((requestedLayer == LAYER.STOV)) ? "XX_SID" : null; break; }
			   case  14: { colname = ((requestedLayer == LAYER.DELTA)||(requestedLayer == LAYER.DMST)||(requestedLayer == LAYER.DM)) ? "DV_SID" : null; break; }
			   case  15: { colname = ((requestedLayer == LAYER.DELTA)||(requestedLayer == LAYER.DMST)||(requestedLayer == LAYER.DM)) ? "DM_SID" : null; break; }
			   case  16: { colname = (requestedLayer == LAYER.SST)||   // PATCH - allen maar bij de SST
				                     ((requestedLayer == LAYER.DVST)&&(DQINFOPATCH==false)) ? "EDW_DQ_INFO" : null; break; }
			   //
			   case  17: { colname = (requestedLayer == LAYER.DM) ? "DM_BATCH_ID_I" : null; break; }
			   case  18: { colname = (requestedLayer == LAYER.DM) ? "DM_BATCH_ID_U" : null; break; }
			   case  19: { colname = (requestedLayer == LAYER.DM) ? "DM_BATCH_ID_D" : null; break; }
			   case  20: { colname = (requestedLayer == LAYER.DM) ? "DM_ST_DATETIME" : null; break; }
			   case  21: { colname = (requestedLayer == LAYER.DM) ? "DM_END_DATETIME" : null; break; }
			   case  22: { colname = (requestedLayer == LAYER.DM) ? "DM_CRE_TMSTMP" : null; break; }
			   case  23: { colname = ((requestedLayer == LAYER.DMST)||(requestedLayer == LAYER.DM)) ? "DV_BATCH_ID" : null; break; }
			   case  24: { colname = ((requestedLayer == LAYER.DELTA)||(requestedLayer == LAYER.DMST)||(requestedLayer == LAYER.DM)) ? "EDW_DQ_INFO" : null; break; }
			   case  25: { colname = (requestedLayer == LAYER.DM) ? "EDW_CHECKSUM" : null; break; }
			   case  26: { colname = (requestedLayer == LAYER.DM) ? "DV_LOAD_DATETIME" : null; break; }
			   //		
			   default : { colname = null; break; }
			   }
			   if ( colname == null ) continue;
			   int colidx = getColumnIdx( tabidx , colname.trim().toUpperCase() , false , "patchMissingColumns");
			   if( colidx >= 0 ) continue;
			   // missing
			   if( createColumn( i , colname ) ) continue;
	           logit( 1 , "Required column [" + colname + "] is missing on [" + tab.Name + "] and cannot be created");
		   }
		   // KEY ? - showstopper
		   if( requestedLayer == LAYER.DVST )
		   {
			   colname = ("KEY_" + tab.Name).toUpperCase(); 
			   int colidx = getColumnIdx( tabidx , colname.trim().toUpperCase() , false , "patchMissingColumns" );
			   if( colidx < 0 ) {
				   colname = (tab.Name + "_KEY").toUpperCase(); 
				   colidx = getColumnIdx( tabidx , colname.trim().toUpperCase() , false , "patchMissingColumns");
				   if( colidx < 0 ) {  // NO technical key 
					   errit("Required TECHNICAL KEY [ {KEY_" + tab.Name + " / "  + tab.Name + "_KEY}] is missing on [" + tab.Name + "]");
					   isOK=false;
				   }
			   }
		   }
		}
		return isOK;
	}

	//----------------------------------------------------------------
	private boolean createColumn(int tab_idx , String colname )
	//----------------------------------------------------------------
	{
		String Datatype = null;
		int prec = -1;
		if( colname.compareToIgnoreCase("RECORD_STATUS") == 0 ) {
			Datatype = "NVARCHAR(3)";
			prec= 3;
		}
		else
		if( colname.compareToIgnoreCase("RECORD_SOURCE") == 0 ) {
			Datatype = "NVARCHAR(30)";
			prec= 30;
		}
		else
		if( colname.compareToIgnoreCase("BATCH_ID") == 0 ) {
			Datatype = "BIGINT";
			prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("LOAD_ST_DATETIME") == 0 ) {
			Datatype = "TIMESTAMP";
			prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("LOAD_END_DATETIME") == 0 ) {
			Datatype = "TIMESTAMP";
			prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("LOAD_DATETIME") == 0 ) {
			Datatype = "TIMESTAMP";
			prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("DV_CURRENT_FLAG") == 0 ) {
			Datatype = "CHAR(1)";
			prec= 1;
		}
		else
		if( colname.compareToIgnoreCase("DV_VERSION") == 0 ) {
			Datatype = "INTEGER";
			prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("EDW_DQ_INFO") == 0 ) {
			Datatype = "NVARCHAR(300)";
			prec= 300;
		}
		else
		if( colname.compareToIgnoreCase("XX_SID") == 0 ) {
			colname =  (table_list.get(tab_idx).Name.trim() + "_SID").toUpperCase();
			Datatype = "BIGINT";
			prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("DV_SID") == 0 ) {
			colname =  (table_list.get(tab_idx).Name.trim() + "_DV_SID").toUpperCase();
			Datatype = "BIGINT";
			prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("DM_SID") == 0 ) {
			colname =  (table_list.get(tab_idx).Name.trim() + "_DM_SID").toUpperCase();
			Datatype = "BIGINT";
			prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("DV_BATCH_ID") == 0 ) {
					Datatype = "BIGINT";
					prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("DM_BATCH_ID_I") == 0 ) {
				Datatype = "BIGINT";
				prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("DM_BATCH_ID_U") == 0 ) {
				Datatype = "BIGINT";
				prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("DM_BATCH_ID_D") == 0 ) {
				Datatype = "BIGINT";
				prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("DM_ST_DATETIME") == 0 ) {
				Datatype = "BIGINT";
				prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("DM_END_DATETIME") == 0 ) {
				Datatype = "BIGINT";
				prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("DM_CRE_TMSTMP") == 0 ) {
				Datatype = "TIMESTAMP";
				prec= 0;
		}
		else
		if( colname.trim().toUpperCase().endsWith("_SID") ) {
				Datatype = "BIGINT";
				prec= 0;
		}
		else
		if( colname.trim().toUpperCase().endsWith("DV_LOAD_DATETIME") ) {
				Datatype = "TIMESTAMP";
				prec= 0;
		}
		else
		if( colname.compareToIgnoreCase("EDW_CHECKSUM") == 0 ) {
			Datatype = "NVARCHAR(250)";
			prec= 250;
		}
		else return false;
		//
		if( prec < 0 ) return false;
		//
		pwdColumn x = new pwdColumn();
		x.Table          = table_list.get(tab_idx).Name;
		x.Name           = colname;
		x.DataType       = Datatype;
		x.Code           = colname;
		x.Length         = prec;
		x.Precision      = prec;
		x.ColumnSequence = table_list.get(tab_idx).col_list.size() + 1;
		x.IsMandatory    = true;
		x.VarcharConvertedDataType = x.DataType;
		x.SourceColumnName = colname;
		//
	    table_list.get( tab_idx ).col_list.add( x );
	    //
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean sortColumnOnTable()
	//----------------------------------------------------------------
	{
		for(int i=0;i<table_list.size();i++)
		{
		   if( sortColumn( i ) == false ) return false;
		}  
		return true;
	}

	//----------------------------------------------------------------
	private boolean sortColumn( int idx )
	//----------------------------------------------------------------
	{
        pwdTable tab = table_list.get(idx);
        pwdColumn temp = new pwdColumn();
        pwdColumnKloon kloon = null;
        for(int k=0;k<tab.col_list.size();k++)
        {
         boolean swap = false; 	
         for(int i=0;i<(tab.col_list.size()-1);i++)
         {
           if( tab.col_list.get(i).ColumnSequence <= tab.col_list.get(i+1).ColumnSequence ) continue;
           if( kloon == null ) {  // lazy
        	   kloon = new pwdColumnKloon();
           }
           kloon.dupliceer(  tab.col_list.get(i) , temp );    // from - to
           kloon.dupliceer(  tab.col_list.get(i+1) , tab.col_list.get(i) );
           kloon.dupliceer(  temp , tab.col_list.get(i+1) );
           swap= true;
         }
         if( swap == false ) break;
        }
		return true;
	}
	
	//----------------------------------------------------------------
	private void showTable(int idx)
	//----------------------------------------------------------------
	{       // -1 shows all tables
			String sLijn = "";
			for(int i=0;i<table_list.size();i++)
			{
			   if( (idx >= 0) && (i != idx) ) continue;
			   pwdTable t = table_list.get(i);
			   sLijn = "[Name=" + t.Name + "] [Code=" + t.Code + "] [SourceTableName=" + t.SourceTableName + "] [entityL0=" + t.EntityL0 + "]";
			   logit(5, sLijn );
			   for(int j=0;j<t.col_list.size();j++)
			   {
				   pwdColumn c = t.col_list.get(j);
				   sLijn = "  COL : "  + c.Name + " " + c.DataType + " " + c.Length + " " + c.Precision + " " + c.IsMandatory;
				   logit(5 , sLijn );
			   }
			   for(int j=0;j<t.key_list.size();j++)
			   {
				   pwdKey k = t.key_list.get(j);
				   sLijn = "  KEY : " + k.KeyName + " " + k.isPrimary + " " + k.keycol_list.toString();
				   logit( 5 , sLijn );
			   }
			   for(int j=0;j<t.ref_list.size();j++)
			   {
				   pwdReference f = t.ref_list.get(j);
				   sLijn = "  REF : " + f.ConstraintName + " FROM " + f.child_col_list.toString() + " TO " + f.ParentTable + " ON " + f.parent_col_list.toString();
				   logit(5 , sLijn );
			   }
			}
	}

	// CALLBACKS
	//----------------------------------------------------------------
	public void nodelogit(String sIn, String a , String b )
	//----------------------------------------------------------------
	{
			logit(5,"(gpSAX) > "+ sIn);
	}
		
	//----------------------------------------------------------------
	public void startNode(String sTag , String sContent , String sHierarchy )
	//----------------------------------------------------------------
	{
			//logit(1, "[tag=" + sTag + "] [Content=" + sContent + "]  [Hier=" + sHierarchy + "]");
		callbackcounter++;
		if( (callbackcounter % 50) == 49 ) System.out.print(".");   // just some visual indicator 
		if( (callbackcounter % 2500) == 2499 ) System.out.println(".");   // just some visual indicator 
		
	}
		
	//----------------------------------------------------------------
	public void endNode(String sTag , String sValue , String sHierarchy )
	//----------------------------------------------------------------
	{
		    
			//logit(1, "[tag=" + sTag + "] [Content=" + sValue + "]  [Hier=" + sHierarchy + "]");
			
		    // Stop if too many errors
		    if( foutTeller >= MAX_ERROR_THRESHOLD ) {
		    	if (foutTeller == MAX_ERROR_THRESHOLD ) {
		    		foutTeller++;
		    		errit("maximum number of errors [" + MAX_ERROR_THRESHOLD + "] has been reached - aborting");
		    	}
		    	parseOK = false;
				return;
			}
			
			
			String sCleanTag = sTag.trim();
			String sValueClean = sValue.trim();

			//
			// ignore the following tags
		    if( sTag.trim().compareToIgnoreCase("COLUMN") == 0 ) {
	        	return;
	        }
	        //
	        if( sTag.trim().compareToIgnoreCase("ROWDUMP") == 0 ) {
	        	return;
	        }
	        //
	        if( sTag.trim().compareToIgnoreCase("ROW") == 0 ) {
	        	return;
	        }
	        if( sTag.trim().compareToIgnoreCase("WORKSHEET") == 0 ) {
	        	return;
	        }
	        if( sTag.trim().compareToIgnoreCase("WORKSHEETINDEX") == 0 ) {
	        	return;
	        }
	        if( sTag.trim().compareToIgnoreCase("EXCEL") == 0 ) {
	        	return;
	        }
	        //
			//
			if( sCleanTag.compareToIgnoreCase("WORKSHEETNAME") == 0 ) {
				if( sValueClean.compareToIgnoreCase("TABLE") == 0 ) {
					flushObjects();
					tabsheet = TABSHEET.TABLE;
					reset_header();
				}
				else
				if( sValueClean.compareToIgnoreCase("TABLE.COLUMN") == 0 ) {
					flushObjects();
				    tabsheet = TABSHEET.COLUMN;
					reset_header();
				}
				else
				if( sValueClean.compareToIgnoreCase("REFERENCE") == 0 ) {
					flushObjects();
					tabsheet = TABSHEET.REFERENCE;
					reset_header();
				}
				else
				if( sValueClean.compareToIgnoreCase("REFERENCE.REFERENCE JOIN") == 0 ) {
					flushObjects();
					tabsheet = TABSHEET.REFERENCEJOIN;
					reset_header();
				}
				else
				if( sValueClean.compareToIgnoreCase("TABLE.KEY") == 0 ) {
					flushObjects();
					tabsheet = TABSHEET.KEY;
					reset_header();
				}
				else
				if( sValueClean.compareToIgnoreCase("REFERENCE2") == 0 ) {
					flushObjects();
					tabsheet = TABSHEET.REFERENCE2;
					reset_header();
				}
				else {
					tabsheet = TABSHEET.UNKNOWN;
					errit("Unknown tabsheet name [" + sValue + "]");
					parseOK=false;
				}
				return;
			}
			//
			switch( tabsheet )
			{
			case TABLE         : { do_table_item( sTag , sValue ); break; }
			case COLUMN        : { do_column_item( sTag , sValue); break; }
			case REFERENCE     : { break; }  // not used
			case REFERENCEJOIN : { break; }  // not used anymore in V2
			case KEY           : { do_key_item( sTag , sValue); break; }
			case REFERENCE2    : { break; }   // not used
			default :  {
				errit("Unknown worksheet [" + tabsheet + "]");
				break;
			}
			}
			return;
	}
	
	//----------------------------------------------------------------
	private boolean parseXMLFile(String FLongName)
	//----------------------------------------------------------------
	{
		if( xMSet.xU.IsBestand( FLongName ) == false ) {
			errit("Cannot read file [" + FLongName + "]" );
			parseOK=false;
			return false;
		}
	    // create a SAX parser	
		gpSAX sax = new gpSAX(this);
		if( sax.ParseXMLFile(FLongName) == false ) return false;
		//errit("parseOK=" + parseOK);
		if( parseOK == false ) return false;
		//
		return parseOK;
	}
	
	//----------------------------------------------------------------
	private void reset_header()
	//----------------------------------------------------------------
	{
	   currRowIndex = -1;
	   currRowNumber = -1;
	   prevRowNumber = -99;
	   currColumnNumber = -1;
	   header = null;
	   header = new ArrayList<HeaderItem>();
	}
	
	// return true when in preamble
	//----------------------------------------------------------------
	private boolean preamble(String sTag , String sValue )
	//----------------------------------------------------------------
	{
		String sCleanTag = sTag.trim();
		String sCleanValue = sValue.trim();
		//
		if( sCleanTag.compareToIgnoreCase("ROWINDEX") == 0 ) {
			currRowIndex = xMSet.xU.NaarInt( sCleanValue );
			if( currRowIndex < 0 ) {
				parseOK=false;
				errit("Error parsing [" + sTag + "=" + sValue + "");
				return true;
			}
			currColumnNumber=-1;
			return true;
		}
		if( sCleanTag.compareToIgnoreCase("ROWNUMBER") == 0 ) {
			currRowNumber = xMSet.xU.NaarInt( sCleanValue );
			if( currRowIndex < 0 ) {
				parseOK=false;
				errit("Error parsing [" + sTag + "=" + sValue + "");
				return true;
			}
			currColumnNumber=-1;
			return true;
		}
        if( sCleanTag.compareToIgnoreCase("COLUMNNUMBER") == 0 ) {
        	currColumnNumber = xMSet.xU.NaarInt( sCleanValue );
			if( currColumnNumber < 0 ) {
				parseOK=false;
				errit("Error parsing [" + sTag + "=" + sValue + "");
				return true;
			}
			return true;
		}
        // store the header information (only the first time )
        if( (currRowNumber == 0) && (sCleanTag.compareToIgnoreCase("VALUE")==0) ) {
        	if( sValue.trim().length() < 1 )  return true;  // empty columname
        	HeaderItem x = new HeaderItem();
        	x.ItemName = sValue.trim();
        	x.position = currColumnNumber;
        	x.kolom    = getColumn( x.ItemName );
        	if( x.kolom == null ) {
        		errit( "Found an unknown columname [" + x.ItemName + "] on tabsheet [" + tabsheet + "]");
        		parseOK=false;
        	}
        	header.add( x );
        	return true;
        }
        if( (currRowIndex == 1) && (currColumnNumber == 0) && (sCleanTag.compareToIgnoreCase("VALUE")==0)) {
        	String sHeader = "HEADER -> ";
        	for(int i=0;i<header.size();i++)
        	{
        		sHeader += "[" + header.get(i).position + "," + header.get(i).ItemName + "] ";
        	}
        	logit(5 , sHeader );
        }
        return false;
	}
	
	//----------------------------------------------------------------
	private EXCLCOL getColumn( String colname )
	//----------------------------------------------------------------
	{
		String sCol = colname.trim().toUpperCase();
		sCol = xMSet.xU.Remplaceer( sCol , " " , "_");
		if( tabsheet == TABSHEET.TABLE ) sCol = "T_" + sCol;
		else		
		if( tabsheet == TABSHEET.COLUMN ) sCol = "C_" + sCol;
		else		
		if( tabsheet == TABSHEET.REFERENCE ) sCol = "R_" + sCol;
		else		
		if( tabsheet == TABSHEET.REFERENCEJOIN ) sCol = "RJ_" + sCol;
		else		
		if( tabsheet == TABSHEET.KEY ) sCol = "K_" + sCol;
		else		
		if( tabsheet == TABSHEET.REFERENCE2 ) sCol = "R2_" + sCol;
		else {
		 errit("(getColumn) System error - not a supported tabsheet [" + sCol + "]");
		 sCol = "%NOP%NIL%ERROR";
		 parseOK=false;
		}
		
		return getColConstant( sCol );
	}
	
	//----------------------------------------------------------------
	private EXCLCOL getColsViaColumnNumber( int idx)
	//----------------------------------------------------------------
	{
		if( header == null )  return null;
		if( idx < 0 ) return null;
		if( idx >= header.size() ) return null;
		return header.get(idx).kolom;
	}
	
	//----------------------------------------------------------------
	private int toInt(String sIn)
	//----------------------------------------------------------------
	{
		String sTemp = sIn;
		if( sIn.endsWith(".0") ) sTemp = sIn.substring( 0 , sIn.length() - 2 );
		//logit(5,sIn + " --> " + sTemp );
		return xMSet.xU.NaarInt(sTemp);
	}
	
	//----------------------------------------------------------------
	private int getTableIdxViaName(String tabname , String CallingFunc)
	//----------------------------------------------------------------
	{
	  try {	
		if( tabname != null ) {
			for(int i=0;i<table_list.size();i++)
			{
				if( table_list.get(i).Name.trim().compareToIgnoreCase( tabname.trim() ) != 0 ) continue;
				return i;
			}
		}
		errit("(" + CallingFunc + ") Could not find table [" + tabname + "]" );
		errit(" >> Make sure that the TABLE tabsheet precedes the COLUMN tabsheet");
		parseOK = false;
		return -1;
	  }
	  catch(Exception e ) {
		  errit( "(getTableIdxViaName) System error XXII when adding table [" + tabname + "] " + xMSet.xU.LogStackTrace(e));
		  return -1;
	  }
	}
	
	//----------------------------------------------------------------
	private void addColumn()
	//----------------------------------------------------------------
	{
		try {
			if( cPwdColumn == null ) return;
			if( cPwdColumn.Table == null ) {
				errit("(addColumn) - No table related to column");
				errit("Most likely : there are NON EMPTY lines at the bottom of the column sheet");
				return;
			}
			int idx = getTableIdxViaName( cPwdColumn.Table , "addColumn");
			//errit("hier2 " + idx);
			if( idx < 0 ) {
				// Beat generated files sometimes have lines starting with "ERROR: "
				if( cPwdColumn.Table.toUpperCase().startsWith("ERROR:") == true ) {
					errit("This Error is due to erors remaining in excel generated from beat");
				}
				errit( "?? [" + cPwdColumn.Name + "] [" + cPwdColumn.Table + "] [" + cPwdColumn.DataType + "]");
				errit("(addColumn) [RowIndex=" + currRowIndex + "] [ColumnIndex=" + currColumnNumber + "]");
				return;
			}
			//errit("hier3");
			if( cPwdColumn.Name == null ) {
				logit(1,"(addColumn) strange got an NULL columnname on [RowIndex=" + currRowIndex + "] [ColumnIndex=" + currColumnNumber + "]");
				return;
			}
			//errit("hier4");
			cPwdColumn.Name = cPwdColumn.Name.trim().toUpperCase();
			cPwdColumn.Code = ( cPwdColumn.Code == null ) ? cPwdColumn.Name : cPwdColumn.Code.trim().toUpperCase() ;
			int len = cPwdColumn.Name.length();
			if( len > maxColWidth ) maxColWidth = len;   // voor display
			//logit(5,"-> Adding " + cPwdColumn.Name + " to " + cPwdColumn.Table + " " + idx);
			table_list.get(idx).col_list.add( cPwdColumn );
		}
		catch( Exception e ) {
			errit("(addColumn) System error XIX [cPwdColumn.Name=" + cPwdColumn.Name + "] [Exception=" + e.getMessage() + "]");
			errit("(addColumn) " + cPwdColumn.Name + " " );
			parseOK=false;
		}
		
	}
	
	// Temporarily store the keys - Keys are defined per column, so they need to be consolidated afterwards
	//----------------------------------------------------------------
	private void pushKeyStack()
	//----------------------------------------------------------------
	{
		if( cTempKey == null ) return;
	//errit("Pushing " + cTempKey.TableName + cTempKey.KeyName );
		temp_keystack.add( cTempKey );
	}
	
	//----------------------------------------------------------------
	private void pushRefJoinStack()
	//----------------------------------------------------------------
	{
		if( cTempRefJoin == null ) return;
		temp_refjoinstack.add( cTempRefJoin );
	}
	
	// Impossible to know when a sheet stops - so there are objects lingering to be added to the various lists
	// isolated the routine - because th eorder of the tabsheets might also change
	//----------------------------------------------------------------
	private void flushObjects()
	//----------------------------------------------------------------
	{
		if( cPwdTable != null )  {
			if( cPwdTable.Name == null ) {
				errit("(flushObjects) - System Error - No table name - probably an empty line on tabsheet table");
			}
			else table_list.add( cPwdTable );   // remainder from processing first tab
	  		cPwdTable = null;
	  	}
		if( cPwdColumn != null )  {
			addColumn();
	  		cPwdColumn = null;
	  	}
		if( cTempKey != null ) {
			pushKeyStack();
			cTempKey = null;
		}
		if( cTempRefJoin != null ) {
			pushRefJoinStack();
			cTempRefJoin = null;
		}
	}
	
	
	// The NK definitions are stored in COLUMN tabsheet in COMMENT field or COMPOSEDKEYDEFINITION
	// not on the KEY tabsheet
	//----------------------------------------------------------------
	private boolean getNaturalKeysFromColumnSheet()
	//----------------------------------------------------------------
	{
	   
	   //if( requestedLayer == LAYER.SST ) return true;
	   
	   //String sLookFor = "COMPOSITE BK COLUMN :";
	   String sLookFor = "BK(";
	   
	
	   /*  Not longer required in V02 : Just go for the COMPOSEDFIELD on the column tabsheet
	   // see whether the COMMENT of COMPOSEDKEyFEDINTION has the NK defintion
	   // if neither of them has NK definition, fall back tot the KEY tabsheet  (by tweakign the ORIGIN field)
	   int CommentCounter=0;
	   int ComposedCounter=0;
	  
	   for(int i=0;i<table_list.size();i++)
	   {
		   pwdTable t = table_list.get(i);
		   for(int j=0;j<t.col_list.size();j++)
		   {
			   if( t.col_list.get(j).Comment == null ) continue;
			   String sTest = t.col_list.get(j).Comment.trim().toUpperCase();
			   sTest = xMSet.xU.compress_spaces(sTest);
			   if( sTest.startsWith(sLookFor) ) CommentCounter++;
			   //
			   if( t.col_list.get(j).ComposedKeyDefinition == null ) continue;
			   sTest = t.col_list.get(j).ComposedKeyDefinition.trim().toUpperCase();
			   sTest = xMSet.xU.compress_spaces(sTest);
			   if( sTest.startsWith(sLookFor) ) ComposedCounter++;
		   }
	   }
	   if( (CommentCounter == 0) && (ComposedCounter == 0) ) {
		   logit(1,"No Natural Key defintions found on COLUMN sheet. Switching to KEY sheet");
		   for(int i=0;i<temp_keystack.size();i++)
		   {
			   temp_keystack.get(i).Origin = TABSHEET.COLUMN;  // gewoon overrulen
		   }
		   return true;
	   }
	   */
	   int ComposedCounter = 100;  // Just overrule to composed field
	

	   // Ok loop nu opnieuw door de COMMENTS/COMPOUNDs en extraheer de kolomnamen
	   for(int i=0;i<table_list.size();i++)
	   {
		   pwdTable t = table_list.get(i);
		   for(int j=0;j<t.col_list.size();j++)
		   {
			   String sKeyText=null;
			   if (ComposedCounter > 0) sKeyText = t.col_list.get(j).ComposedKeyDefinition;
			                       else sKeyText = t.col_list.get(j).Comment;
//logit(5,"ComposedKeyDefinition" + sKeyText );			   
			   if( sKeyText == null) continue;
			   sKeyText = sKeyText.trim().toUpperCase();
			   //sKeyText =  xMSet.xU.compress_spaces(sKeyText);
			   sKeyText =  xMSet.xU.removeBelowIncludingSpaces(sKeyText);
//logit(1,"(extractnaturalkey) [" + t.col_list.get(j).ComposedKeyDefinition + "] [compr=" + sKeyText + "] [lkp=" + sLookFor + "]");			   
			   if( sKeyText.startsWith(sLookFor) ) {
			     if( extractNaturalKey( t.col_list.get(j).Table , t.col_list.get(j).Name , sKeyText ) == false ) return false;
			   }
			   // later eventueel ook de PK
		   }
	   }
	   return true;	 
	}
	
	// count the recently added NKs 
	//----------------------------------------------------------------
	private int getNumberOfNaturalKeys(String tabname)
	//----------------------------------------------------------------
	{
		int ctr=0;
		for(int i=0;i<temp_keystack.size();i++)
		{
			if( temp_keystack.get(i).isPrimary ) continue;
			if( temp_keystack.get(i).Origin != TABSHEET.COLUMN ) continue;
			if( temp_keystack.get(i).TableName.trim().compareToIgnoreCase( tabname ) == 0 ) ctr++;
		}
		return ctr;
	}
	
	// sKeyText format   BK( key ; key ; key )
	//----------------------------------------------------------------
	private boolean extractNaturalKey( String tab , String col , String sKeyText )
	//----------------------------------------------------------------
	{
	    int idx = sKeyText.indexOf("(");   // BK( iets ; iets )
	    if( idx < 0 ) {
	    	errit("Extract NaturalKey Cannot determine start of key list in [" + sKeyText  +"] for[" + tab + "].[" + col + "]");
	    	return false;
	    }
	    String sKeys=null;
	    try {
	      sKeys = sKeyText.substring(idx).trim();
	      if( (sKeys.startsWith("(") == false) || (sKeys.endsWith(")") == false) ) {
		    	errit("Extract NaturalKey Cannot find opening/closing round bracket in [" + sKeyText  +"] for[" + tab + "].[" + col + "] [" + sKeys + "]");
		    	return false;
		  }
	      sKeys = sKeys.substring(1,sKeys.length()-1).trim();
	    }
	    catch(Exception e) {
	    	errit("Extract NaturalKey Cannot find anything past semi colon in [" + sKeyText  +"] for[" + tab + "].[" + col + "]");
	    	return false;	
	    }
	    //  COLUMNNAME moet  KEY_ + table naam zijn of  table_name KEY
	    String sKeyColName = "KEY_" + tab.trim().toUpperCase();
	    if( sKeyColName.compareToIgnoreCase( col.trim() ) != 0 ) {
	    	sKeyColName = tab.trim().toUpperCase() + "_KEY";
	    	 if( sKeyColName.compareToIgnoreCase( col.trim() ) != 0 ) {
	    	 errit("Natural key on table [" + tab + "] is not KEY_<tablename> or <tablename>_KEY [" + col + "]");
	    	 return false;
	    	}
	    }
	    //
	    //logit(5 , "(extractNaturalKeys) [" + tab + "] [" + col + "] [" + sKeys + "]");
	    //
	    // NK 
	    int ctr = getNumberOfNaturalKeys( tab.trim().toUpperCase() );
	    // tokenizer
	 	StringTokenizer st = new StringTokenizer( sKeys, "; ");
		while(st.hasMoreTokens()) 
		{
			    String sCoo   = st.nextToken().trim().toUpperCase();  // should not occur, but ..
			    if( sCoo.length() < 1) continue;
			    tempKey key   = new tempKey();
			    key.TableName = tab.trim().toUpperCase();
			    String prefix = "NK_";
			    if( ctr > 1) prefix = "NK" + ctr + "_";   
			    key.KeyName   = prefix+ tab.trim().toUpperCase();
			    key.isPrimary = false;
			    key.ColName   = sCoo;
			    key.Origin    = TABSHEET.COLUMN;
			    temp_keystack.add( key );
			    //logit(5 , "NATURAL KEY [T=" + key.TableName + "] [N=" + key.KeyName + "] [C=" +  key.ColName + "]");
		}
		return true;
	}

	//----------------------------------------------------------------
	private boolean getForeignKeysFromColumnSheet()
	//----------------------------------------------------------------
	{
		  String sLookFor = "FK(";
		  for(int i=0;i<table_list.size();i++)
		   {
			   pwdTable t = table_list.get(i);
			   for(int j=0;j<t.col_list.size();j++)
			   {
				   String sKeyText=null;
				   sKeyText = t.col_list.get(j).ComposedKeyDefinition;
				   if( sKeyText == null) continue;
				   sKeyText = sKeyText.trim().toUpperCase();
				   sKeyText =  xMSet.xU.compress_spaces(sKeyText);
				   if( sKeyText.startsWith(sLookFor) ) {
				     if( extractForeignKey( t.col_list.get(j).Table , t.col_list.get(j).Name , sKeyText , t.col_list.get(j).ReferencedTable ) == false ) {
				    	 //return false;
				    	 errit("(getForeignKeysFromColumnSheet) The error status is currently ignored, due to bad quality of PwD file. Consider to re-enable.");
				     }
				   }
			   }
		   }
  	       return true;
	}

	// sKeyText format   FK( key ; key ; key )
	//----------------------------------------------------------------
	private boolean extractForeignKey( String tab , String col , String sKeyText , String sReferencedTable )
	//----------------------------------------------------------------
	{
	    int idx = sKeyText.indexOf("(");   // FK( iets ; iets )
	    if( idx < 0 ) {
	    	errit("Extract Foreign Key Cannot determine start of key list in [" + sKeyText  +"] for[" + tab + "].[" + col + "]");
	    	return false;
	    }
	    String sKeys=null;
	    try {
	      sKeys = sKeyText.substring(idx).trim();
	      if( (sKeys.startsWith("(") == false) || (sKeys.endsWith(")") == false) ) {
		    	errit("Extract Foreign Key Cannot find opening/closing round bracket in [" + sKeyText  +"] for[" + tab + "].[" + col + "] [" + sKeys + "]");
		    	return false;
		  }
	      sKeys = sKeys.substring(1,sKeys.length()-1).trim();
	    }
	    catch(Exception e) {
	    	errit("Extract Foreign Key Cannot find anything past semi colon in [" + sKeyText  +"] for[" + tab + "].[" + col + "]");
	    	return false;	
	    }

	    // referenced table is part of the key
	    String sTest = ("KEY_" + sReferencedTable).trim().toUpperCase();
	    if (sTest.compareToIgnoreCase( col ) != 0 ) {
	    	  sTest = (sReferencedTable + "_KEY").trim().toUpperCase();
	    	  if (sTest.compareToIgnoreCase( col ) != 0 ) {
	    		  errit("Foreign key [" + col + "] name probably incorrect. Referenced table is [" + sReferencedTable + "]");
	    		  // return false  // IGNORE
	    	  }
	    }
	    
	    // de referencedkeyList moet exact 2 records bevatten de PK en de NK.
	    ArrayList<pwdKey> referencedkeylist = getKeyListFromTable( sReferencedTable );
	    if( referencedkeylist == null ) {
	    	errit("(extractForeignKey) on [" + tab + "." + col + "] from [" +sKeyText + "]");
	    	errit("Could not find any natural key definition on table [" + sReferencedTable + "]" );
	    	return false;
	    }
	    if( referencedkeylist.size() < 1) {
	    	errit("(extractForeignKey) on [" + tab + "." + col + "] from [" +sKeyText + "]");
	    	errit("Empty PK/NK key definition on table [" + sReferencedTable + "] KeyListSize=" + referencedkeylist.size() );
	    	dumpKeyList(referencedkeylist );
	    	return false;
	    }
	    if( referencedkeylist.size() != 2) {
	    	// KB 22 FEB
	      if( (requestedLayer == LAYER.SST) || (requestedLayer == LAYER.DVST) ) {
	    	errit("(extractForeignKey) on [" + tab + "." + col + "] from [" +sKeyText + "]");
	    	errit("Referenced key list for [" + sReferencedTable + "] MUST contain exactly 2 entries (PK/NK). It now comprises " + referencedkeylist.size());
	    	String Missing =  referencedkeylist.get(0).isPrimary ? "NATURAL" : "PRIMARY";
	    	errit("Looks like the " + Missing + " is missing on referenced table [" + sReferencedTable + "] in the Excel. Now only got [" + referencedkeylist.get(0).KeyName + "] " + referencedkeylist.get(0).keycol_list.toString() );
	    	dumpKeyList(referencedkeylist );
	    	return false;
	      }
	    }

	    // eerst de  FK_key naar de primary key
	    if( (requestedLayer == LAYER.SST) || (requestedLayer == LAYER.DVST) ) { 
	     int pkidx=-1;
	     if( referencedkeylist.get(0).isPrimary ) pkidx=0;
	     if( referencedkeylist.get(1).isPrimary ) pkidx=1;
	     if( pkidx < 0 ) {
	    	errit("Cannot find PRIMARY key on list for table [" + sReferencedTable + "]");
	    	return false;
	     }
	     if( referencedkeylist.get(pkidx).keycol_list.size() !=1 ) {
	    	errit("PRIMARY key on list for table [" + sReferencedTable + "] is not SINGLE column");
	    	return false;
	     }
	     tempRefJoin x = new tempRefJoin();
	     x.RefName           = ("FKX_" + tab + "_" + sReferencedTable).trim().toUpperCase();
	     x.ParentTableColumn = sReferencedTable + "." + referencedkeylist.get(pkidx).keycol_list.get(0);
	     x.ChildTableColumn  = tab + "." + col.trim().toUpperCase();
	     x.Origin            = TABSHEET.COLUMN;
	     temp_refjoinstack.add( x );
	    }
	    
	    // dan de FK_KEY composites uit de lijst halen en die naar de NK van de gerefereede leggen
	    int nkidx=-1;
	    if( referencedkeylist.size() > 0 ) { if( referencedkeylist.get(0).isPrimary == false ) nkidx=0; }
	    if( referencedkeylist.size() > 1 ) { if( referencedkeylist.get(1).isPrimary == false ) nkidx=1; }
	    if( nkidx < 0 ) {
	    	errit("Cannot find NATURAL key on list for table [" + sReferencedTable + "]");
	    	return false;
	    }
	    
	    // determine whether the number of elements matches
	    int aantal = xMSet.xU.TelDelims( sKeys , ';' );
	    if( (aantal+1) != referencedkeylist.get(nkidx).keycol_list.size() ) {
	          errit("Number mismatch on natural key elements on [" + sReferencedTable + "] for FOREIGN KEY [" + tab + "." + col + "] [" + sKeys + "]");
	          errit("   Natural key on [" + sReferencedTable + "] is [" + referencedkeylist.get(nkidx).keycol_list.toString() + "]");
			  return false;	
	    }
	    
	    StringTokenizer st = new StringTokenizer( sKeys, "; ");
	    int counter=-1;
		while(st.hasMoreTokens()) 
		{
		  String sChildCol   = st.nextToken().trim().toUpperCase();  // should not occur, but ..
		  if( sChildCol.length() < 1) continue;
		  counter++;
		  tempRefJoin y = new tempRefJoin();
		  y.RefName           = ("FKN_" + tab + "_" + sReferencedTable).trim().toUpperCase();
		  y.ParentTableColumn = sReferencedTable + "." + referencedkeylist.get(nkidx).keycol_list.get(counter);
		  y.ChildTableColumn  = tab + "." + sChildCol;
		  y.Origin            = TABSHEET.COLUMN;
		  temp_refjoinstack.add( y );
	    }
        
	    //logit(5 , tab + " " + col + " " +sKeyText + " " + referencedkeylist.get(pkidx).keycol_list.toString() + " " + referencedkeylist.get(nkidx).keycol_list.toString() );
	    
	    return true;
	}	

	//----------------------------------------------------------------
	private ArrayList<pwdKey> getKeyListFromTable( String tabname )
	//----------------------------------------------------------------
	{
		for(int i=0;i<table_list.size();i++)
		{
			if( table_list.get(i).Name.compareToIgnoreCase( tabname ) != 0 ) continue;
			return table_list.get(i).key_list;
		}
		return null;
	}
	
	//----------------------------------------------------------------
	private boolean consolidateKeys()
	//----------------------------------------------------------------
	{
	   int primKeyCounter=0;
	   for(int i=0;i<temp_keystack.size();i++)
	   {
			int idx = getTableIdxViaName( temp_keystack.get(i).TableName.trim() , "consolidateKeys");
			if( idx < 0 ) return false;
			tempKey tk = temp_keystack.get(i);
 logit(5,"Checking key [" + tk.KeyName + "] " + tk.isPrimary );
			// EXCEPTIONS  - indien Natural Key dan moet de definite uit de COLUMN tabsheet komen
			if( tk.isPrimary == false ) {
			  if( tk.Origin == null ) {
				errit("(consolidateKeys) Origin Field is not populated correctly (NULL)");
				return false;
			  }
			  if( tk.Origin == TABSHEET.KEY ) continue;
			  if( tk.Origin != TABSHEET.COLUMN ) {
				errit("(consolidateKeys) Origin Field is not populated correctly [" + tk.Origin + "] expecting [KEY or COLUMN]");
				return false;
			  }
			}
			else primKeyCounter++;
			//
            logit(5,"Adding key [" + tk.KeyName + "] to [" + tk.TableName + "]" );
			//
			pwdKey k = new pwdKey();
			k.KeyName = tk.KeyName.trim();
			k.isPrimary = tk.isPrimary;
			// if the key is not thre add it
			int ndx = -1;
			pwdTable tab = table_list.get(idx);
			for(int j=0;j<tab.key_list.size();j++)
			{
				if( tab.key_list.get(j).KeyName.compareToIgnoreCase( tk.KeyName.trim() ) != 0 ) continue;
				ndx = j;
				break;
			}
			if( ndx < 0 ) {
		        tab.key_list.add ( k );
		        ndx = tab.key_list.size() - 1;
			}
			// add the col to the keycol list - sometime colname is preceded by table name .
			try {
			 String sColName = tk.ColName.trim();
		     String sPrefix = tk.TableName + ".";
		     if( sColName.toUpperCase().startsWith( sPrefix.toUpperCase() ) ) {
		    	sColName = sColName.substring( sPrefix.length() ).trim();
		     }
		     tab.key_list.get(ndx).keycol_list.add( sColName.trim()  );
			}
			catch( Exception e ) {
				errit("(consolidate keys) System Error : Cannot add [" + tk.ColName + "] " + e.getMessage() );
				return false;
			}
	   }
	   // The primary keys are not needed for SRC and STOV  (incidentally SRC requires the ALT keys) 
	   if( primKeyCounter == 0 ) {
		   if( (requestedLayer != LAYER.SRC) && 
			   (requestedLayer != LAYER.STOV) &&
			   (requestedLayer != LAYER.DELTA) && 
			   (requestedLayer != LAYER.DMST) &&
			   (requestedLayer != LAYER.DM)) { 
		          errit("Strange no primary keys - is the TABLE.KEY tabsheet correctly filled ?");
		   }
	   }
	   return true;	
	}
	
	//----------------------------------------------------------------
	private boolean consolidateForeignKeys()
	//----------------------------------------------------------------
	{
		for(int i=0;i<temp_refjoinstack.size();i++)
		{
			tempRefJoin rj = temp_refjoinstack.get(i);
			
			int j = rj.ParentTableColumn.indexOf(".");
			if( j < 0 ) {
				errit("Reference Join [" + rj.RefName + "] ParentTableColumn [" + rj.ParentTableColumn + "] nor formated <table>.<column>" );
				return false;
			}
			String sParentTable  = rj.ParentTableColumn.substring( 0 , j ).trim().toUpperCase();
			String sParentColumn = rj.ParentTableColumn.substring( j+1 ).trim().toUpperCase();
			//
			j = rj.ChildTableColumn.indexOf(".");
			if( j < 0 ) {
				errit("Reference Join [" + rj.RefName + "] ChildTableColumn [" + rj.ChildTableColumn + "] nor formated <table>.<column>" );
				return false;
			}
			String sChildTable  = rj.ChildTableColumn.substring( 0 , j ).trim().toUpperCase();
			String sChildColumn = rj.ChildTableColumn.substring( j+1 ).trim().toUpperCase();
			//
			//logit(5, "REF JOIN ON [" +  rj.RefName + "] FROM [" + sChildTable +"].[" + sChildColumn + "] TO [" + sParentTable +"].[" + sParentColumn +"]");
			
			//
			int parent_idx = getTableIdxViaName( sParentTable , "consolidateForeignKeys-parent");
			if( parent_idx < 0 ) {
				errit("Reference Join [" + rj.RefName + "] ChildTableColumn [" + rj.ChildTableColumn + "] unable to find parent table [" + sParentTable + "]" );
				return false;
			}
			int child_idx = getTableIdxViaName( sChildTable , "consolidateForeignKeys-child" );
			if( child_idx < 0 ) {
				errit("Reference Join [" + rj.RefName + "] ChildTableColumn [" + rj.ChildTableColumn + "] unable to find child table [" + sChildTable + "]" );
				return false;
			}
			// locate the refence on the child tabel if not already there then create constraint
			pwdTable tab = table_list.get( child_idx );
			int idx = -1;
			for( int k=0;k<tab.ref_list.size();k++)
			{
			  if( tab.ref_list.get(k).ConstraintName.trim().compareToIgnoreCase(rj.RefName.trim()) != 0 ) continue;
			  idx = k;
			  break;
			}
			if( idx < 0 ) 
			{
			  pwdReference x = new pwdReference();
			  x.ConstraintName = rj.RefName.trim().toUpperCase();
			  x.ParentTable = sParentTable.trim().toUpperCase();
			  tab.ref_list.add( x );
			  idx = tab.ref_list.size() - 1;
			}
			// is de tabelnaam dezelfde
			if( tab.ref_list.get(idx).ParentTable.compareToIgnoreCase( sParentTable.trim() ) != 0 ) {
				errit("FOREIGN KEY CONSTRAINT with multiple elements. Tablename does not match. [" +  rj.RefName + "]  [" + sChildTable +"].[" + sChildColumn + "] TO [" + sParentTable +"].[" + sParentColumn +"]");
				return false;
			}
			// voeg de kolommen toe
			tab.ref_list.get(idx).child_col_list.add( sChildColumn.trim().toUpperCase() );
			tab.ref_list.get(idx).parent_col_list.add ( sParentColumn.trim().toUpperCase() );
		}
		
		return true;
	}
	
	//----------------------------------------------------------------
	private void do_table_item(String sTag , String sValue)
	//----------------------------------------------------------------
	{
        if( preamble( sTag , sValue ) ) return;
        //
        if( prevRowNumber != currRowNumber ) {
        	prevRowNumber = currRowNumber;
        	//
        	if( cPwdTable != null )  {
        		table_list.add( cPwdTable );
        		//if( DEBUG ) showTable( table_list.size()-1 );
        	}
        	// create new object
        	cPwdTable = new pwdTable();
        }
        //
        EXCLCOL c = getColsViaColumnNumber( currColumnNumber );
        if( c == null ) {
        	errit("(do_table_item) Cannot fectch the EXCCOL for [row=" + currRowNumber + "] [Col=" + currColumnNumber + "] [" + sTag + "] [" + sValue + "]");
        	parseOK=false;
        	return;
        }
        
        //logit(5, "[R=" + currRowNumber + "] [C=" + currColumnNumber + "] [" + c + "] "+ sTag + " " + sValue );
        
        switch( c )
        {
        case T_NAME           : { cPwdTable.Name = sValue.trim().toUpperCase(); break; }
        case T_CODE           : { cPwdTable.Code = sValue.trim().toUpperCase(); break; }
        case T_COMMENT        : { cPwdTable.Comment = sValue; break; }
        case T_ENTITYL0       : { cPwdTable.EntityL0 = sValue; break; }
        case T_ENTITYL1       : { cPwdTable.EntityL1 = sValue; break; }
        case T_ISHUB          : { cPwdTable.IsHub = xMSet.xU.ValueInBooleanValuePair("="+ sValue); break; }
        case T_ISLINK         : { cPwdTable.IsLink = xMSet.xU.ValueInBooleanValuePair("="+ sValue); break; }
        case T_HASREFERENCE   : { cPwdTable.HasReference = xMSet.xU.ValueInBooleanValuePair("="+ sValue); break; }
        case T_ISREFERENCED   : { cPwdTable.IsReferenced = xMSet.xU.ValueInBooleanValuePair("="+ sValue); break; }
        case T_ISSATTELITE    : { cPwdTable.IsSattelite = xMSet.xU.ValueInBooleanValuePair("="+ sValue); break; }
        case T_ISREFERENCE    : { cPwdTable.IsReference= xMSet.xU.ValueInBooleanValuePair("="+ sValue); break; }
        case T_SOURCEFILEDELIMITER : { cPwdTable.SourceFileDelimiter = sValue; break; }
        case T_SOURCEFILENAME : { cPwdTable.SourceFileName = sValue.trim(); break; }
        case T_CR             : { cPwdTable.CR = sValue; break; }
        case T_CR_HIST        : { cPwdTable.CR_Hist = sValue; break; }
        case T_SOURCETABLENAME: { cPwdTable.SourceTableName = sValue.trim().toUpperCase(); break; }
        case T_VIEW           : { cPwdTable.SourceTableName = sValue.trim().toUpperCase(); break; }  // re-use for VIEW
        case T_OWNER          : { cPwdTable.OwnerName = sValue; break; }
        default : {
        	errit("(do_table_item) Unsupported columname [" + c + "]");
        	parseOK=false;
        	break;
        	}
        }
   }
	
	//  REF( tablename )
	//----------------------------------------------------------------
	private String extractRefTable(String sIn)
	//----------------------------------------------------------------
	{
		String sRet="";
		if( sIn == null ) return sRet;
		sRet = sIn.trim().toUpperCase();
		if( sRet.length() < 1 )  return sRet;
		if( sRet.startsWith( "REF(" ) == false ) return sRet;
		sRet = xMSet.xU.Remplaceer( sRet , "REF(" , "" );
		sRet = xMSet.xU.Remplaceer( sRet , ")" , "" );
		return sRet.trim();
	}
	
	//----------------------------------------------------------------
	private void do_column_item(String sTag , String sValue )
	//----------------------------------------------------------------
	{
	    if( preamble( sTag , sValue ) ) return;
	    
	    if( prevRowNumber != currRowNumber ) {
        	prevRowNumber = currRowNumber;
        	//
        	if( cPwdColumn != null )  addColumn();
        	// create new object
        	cPwdColumn = new pwdColumn();
        }
	 	  
        //
        EXCLCOL c = getColsViaColumnNumber( currColumnNumber );
        if( c == null ) {
        	errit("(do_column_item) Cannot fetch the EXCLCOL for [row=" + currRowNumber + "] [Col=" + currColumnNumber + "] [" + sTag + "] [" + sValue + "]");
        	parseOK=false;
        	return;
        }
        
   //logit(5, "[R=" + currRowNumber + "] [C=" + currColumnNumber + "] [" + c + "] "+ sTag + " " + sValue );
   //logit(0,"[R=" + currRowNumber + "] [C=" + currColumnNumber + "] [" + c + "] "+ sTag + " " + sValue );
        
        switch( c )
        {
        case C_TABLE     : { cPwdColumn.Table = sValue.trim().toUpperCase(); break; }
        case C_NAME      : { cPwdColumn.Name = sValue.trim().toUpperCase(); break; }
        case C_DATATYPE  : { cPwdColumn.DataType = sValue.trim() ;
        					 if( cPwdColumn.DataType.endsWith(",")) {
        						 cPwdColumn.DataType = cPwdColumn.DataType.substring( 0 , cPwdColumn.DataType.length() - 1 );
        					 }
                             break; }
        case C_CODE      : { cPwdColumn.Code = sValue.trim().toUpperCase(); break; }
        case C_COMMENT   : { cPwdColumn.Comment = sValue; break; }
        case C_LENGTH    : { cPwdColumn.Length = toInt(sValue.trim()); 
                             if( cPwdColumn.Length < 0) { 
                            	 errit("Invalid number [" + sValue + "] on [Row=" +  currRowNumber + "] [" + c + "] ["+ tabsheet  + "]");
                            	 parseOK=false;
                             }
                             break; }
        case C_PRECISION : { cPwdColumn.Precision = toInt(sValue.trim()); 
        					 if( cPwdColumn.Precision < 0) { 
        						 errit("Invalid number [" + sValue + "] on [Row=" +  currRowNumber + "] [" + c + "] ["+ tabsheet  + "]");
        						 parseOK=false;
        					 }
        					 break; }
        case C_PRIMARY         : { cPwdColumn.IsPrimary = xMSet.xU.ValueInBooleanValuePair("="+ sValue); break; }
        case C_MANDATORY       : { cPwdColumn.IsMandatory = xMSet.xU.ValueInBooleanValuePair("="+ sValue); break; }
        case C_COLUMN_SEQUENCE : { cPwdColumn.ColumnSequence = toInt(sValue.trim()); 
		 						   if( cPwdColumn.ColumnSequence < 0) { 
		 							   	errit("Invalid number [" + sValue + "] on [Row=" +  currRowNumber + "] [" + c + "] ["+ tabsheet  + "]");
		 							   	parseOK=false;
		 						   }
		 						   break; }
        case C_BUSINESSLONGDESC         : { cPwdColumn.BusinessLongDesc = sValue; break; }
        case C_BUSINESSSHORTDESC        : { cPwdColumn.BusinessShortDesc = sValue; break; }
        case C_COMPOSEDKEYDEFINITION    : { cPwdColumn.ComposedKeyDefinition = sValue; break; }
        case C_NEWCOLUMNINDICATOR_SST   : { cPwdColumn.IsNewColumnIndicator_SST = xMSet.xU.ValueInBooleanValuePair("="+ sValue);
                                            //errit("SST new " + cPwdColumn.Name );
                                            break; }
        case C_NEWCOLUMNINDICATOR_DVST  : { cPwdColumn.IsNewColumnIndicator_DVST = xMSet.xU.ValueInBooleanValuePair("="+ sValue); break; }
        case C_VARCHARCONVERTEDDATATYPE : { cPwdColumn.VarcharConvertedDataType = (sValue==null) ? "" : sValue; break; }
        case C_SOURCEFILEPOSITION       : { cPwdColumn.SourceFilePosition = sValue; break; }
        //case C_SOURCEFILENAME           : { cPwdColumn.SourceFileName = sValue; break; }
        case C_EDW_CHECKSUM             : { cPwdColumn.EDWCheckSum = xMSet.xU.ValueInBooleanValuePair("="+ sValue); break; }
        //
        case C_REFTABLE           		: { cPwdColumn.ReferencedTable = extractRefTable(sValue); break; }
        case C_SOURCEMANDATORY          : { cPwdColumn.IsSourceMandatory = xMSet.xU.ValueInBooleanValuePair("="+ sValue); break; }
        case C_SOURCEDATATYPE           : { cPwdColumn.SourceDataType = sValue.trim().toUpperCase(); break; }
        case C_CR                       : { cPwdColumn.CR = sValue; break; }
        case C_CR_HIST                  : { cPwdColumn.CR_Hist = sValue; break; }
        //
        case C_SOURCECOLUMNNAME         : { cPwdColumn.SourceColumnName = sValue; break; }
        case C_POSITION                 : { if ( setPosition( sValue ) == false ) {
        	 									errit("Invalid position [" + sValue + "] on [Row=" +  currRowNumber + "] [" + c + "] ["+ tabsheet  + "] field [" + cPwdColumn.Table + "." + cPwdColumn.Name + "]");
        	 									parseOK=false;
        									}; 
        									break; }
        // 22 okt
        case C_BUSINESSLONGDESCVCE  : { break; }
        case C_BUSINESSSHORTDESCVCE : { break; }
        case C_BUSINESSRULE         : { break; }
        case C_SOURCENAME           : { cPwdColumn.SourceColumnName = sValue; break; }  // identical to C_SOURCECOLUMNANME
        case C_SOURCECODE           : { break; }
        //         
        default : {
        	errit("(do_column_item) Unsupported EXCLCOL [" + c + "]");
        	parseOK=false;
        	break;
            }
        } // switch
        
	}
	
	//----------------------------------------------------------------
	private boolean setPosition(String sIn)
	//----------------------------------------------------------------
	{
		try {
			String sTemp = sIn.toUpperCase().trim();
			if( sIn.endsWith(")") == false ) return false;
			int idx = sTemp.indexOf("(");
			if( idx < 0 ) return false;
			sTemp = sTemp.substring( idx+1 , sTemp.length()-1 );
			idx = sTemp.indexOf(":");
			if( idx < 0 ) return false;
			String one = sTemp.substring( 0 , idx );
			String two = sTemp.substring( idx + 1 );
			//logit(5, sIn + "[" + sTemp + "]" + one + " " + two);     
			int een = xMSet.xU.NaarInt( one );
			int twee = xMSet.xU.NaarInt( two );
			if( (een < 0) || ( twee < 0) ) return false;
			if ( cPwdColumn == null ) return false;
			cPwdColumn.positionStart = een;
			cPwdColumn.positionStop = twee;
			return true;
		}
		catch( Exception e ) {
			return false;
		}
	}
	
	
	/* NO LONGER USED in V02
	//----------------------------------------------------------------
	private void do_referencejoin_item(String sTag , String sValue )
	//----------------------------------------------------------------
	{
	    if( preamble( sTag , sValue ) ) return;
	    
	    if( prevRowNumber != currRowNumber ) {
        	prevRowNumber = currRowNumber;
        	//
        	if( cTempRefJoin != null ) pushRefJoinStack();
        	// create new object
        	cTempRefJoin = new tempRefJoin();
        	cTempRefJoin.Origin = TABSHEET.REFERENCEJOIN;
        }
        //
        EXCLCOL c = getColsViaColumnNumber( currColumnNumber );
        if( c == null ) {
        	errit("(do_referencejoin_item) Cannot fetch the EXCLCOL for [row=" + currRowNumber + "] [Col=" + currColumnNumber + "]");
        	parseOK=false;
        	return;
        }
        //
        switch( c )
        {
        case RJ_PARENT 				: { cTempRefJoin.RefName = sValue; break; }
        case RJ_PARENT_TABLE_COLUMN	: { cTempRefJoin.ParentTableColumn = sValue; break; }
        case RJ_CHILD_TABLE_COLUMN  : { cTempRefJoin.ChildTableColumn  = sValue; break; }
        default : {
        	errit("'(do_referencejoin_item) Unsupported EXCLCOL [" + c + "]");
        	parseOK=false;
        	break;
        	}
        }
        //logit(5," --> " + cTempKey.KeyName + " " + c);
			 		
	}
	
	//----------------------------------------------------------------
	private void do_reference_item(String sTag , String sValue )
	//----------------------------------------------------------------
	{
	    if( preamble( sTag , sValue ) ) return;
		 		
	}
	//----------------------------------------------------------------
	private void do_reference2_item(String sTag , String sValue )
	//----------------------------------------------------------------
	{
	    if( preamble( sTag , sValue ) ) return;
	}
	*/
	
	//----------------------------------------------------------------
	private void do_key_item(String sTag , String sValue )
	//----------------------------------------------------------------
	{
		// KB 9 NOV added SRC and stov
		boolean goback=false;
		if( requestedLayer == LAYER.SRC) goback=true; 
		if( requestedLayer == LAYER.STOV) goback=true;
		if( requestedLayer == LAYER.DELTA) goback=true;
		if( requestedLayer == LAYER.DMST) goback=true;
		if( requestedLayer == LAYER.DM) goback=true;
		if( (requestedLayer != LAYER.SST) && (requestedLayer != LAYER.DVST) ) goback=true;
		if( goback ) {
			logit( 1 , "LAYER [" + requestedLayer + "] - parsing the PRIMARY KEY will be skipped");
			return;
		}
		
	    if( preamble( sTag , sValue ) ) return;
	    
	    if( prevRowNumber != currRowNumber ) {
        	prevRowNumber = currRowNumber;
        	//
        	if( cTempKey != null )  pushKeyStack();
        	// create new object
        	cTempKey = new tempKey();
        	cTempKey.Origin = TABSHEET.KEY;
        }
        //
        EXCLCOL c = getColsViaColumnNumber( currColumnNumber );
        if( c == null ) {
        	errit("(do_key_item) Cannot fetch the EXCLCOL for [row=" + currRowNumber + "] [Col=" + currColumnNumber + "]");
        	parseOK=false;
        	return;
        }
        //
        switch( c )
        {
        case K_TABLE 	: { cTempKey.TableName = sValue; break; }
        case K_NAME 	: { cTempKey.KeyName = sValue; break; }
        case K_PRIMARY  : { cTempKey.isPrimary = xMSet.xU.ValueInBooleanValuePair("="+ sValue); break; }
        case K_COLUMNS 	: { cTempKey.ColName = sValue; break; }
        default : {
        	errit("'(do_key_item) Unsupported EXCLCOL [" + c + "]");
        	parseOK=false;
        	break;
        	}
        }
        //logit(5," --> " + cTempKey.KeyName + " " + c);
        
	}
	
	
	
	//----------------------------------------------------------------
	private boolean determineCreationOrder()
	//----------------------------------------------------------------
	{
	    tab_diepte = new ArrayList<tabledepth>();
		for(int i=0;i<table_list.size();i++)
		{
			tabledepth d = new tabledepth();
			d.tablename = table_list.get(i).Name;
			d.idx = i;
			d.depth = 0;
			tab_diepte.add( d );
		}
		for(int i=0;i<tab_diepte.size();i++)
		{
			globaldiepte = 0;
			followparent( tab_diepte.get(i).idx , 0 );
			tab_diepte.get(i).depth = globaldiepte;
			//logit(9,"Depth : " + tab_diepte.get(i).tablename + " -> " + tab_diepte.get(i).depth);
			table_list.get(i).dependency_depth = globaldiepte;
		}
		return true;
	}

	//----------------------------------------------------------------
	private void followparent(int idx , int diepte )
	//----------------------------------------------------------------
	{
	    diepte++;	
	    if( diepte > globaldiepte ) globaldiepte = diepte;
	    // safety catch
	    if( diepte > 100 ) {
	    	errit("(followparent) Too many levels in DDL - aborting");
	        return;
	    }
		pwdTable tab = table_list.get(idx);
		for(int i=0;i<tab.ref_list.size();i++)
		{
		  int pdx = this.getTableIdxViaName( tab.ref_list.get(i).ParentTable , "followParent");
		  if( pdx < 0 ) continue;
		  followparent( pdx , diepte );
		}
	}
	
	//----------------------------------------------------------------
	private boolean makeDDL()
	//----------------------------------------------------------------
	{
	  switch( requestedLayer )
	  {
	  case SST  : return makeDDLSpecific(requestedLayer);
	  case DVST : return makeDDLSpecific(requestedLayer);
	  case SRC : return makeDDLSpecific(requestedLayer);
	  case ORCLEXT : return makeDDLSpecific(requestedLayer);
	  case STOV : return makeDDLSpecific(requestedLayer);
	  case DELTA : return makeDDLSpecific(requestedLayer);
	  case DMST : return makeDDLSpecific(requestedLayer);
	  case DM : return makeDDLSpecific(requestedLayer);
	  
	  default   : {
		  errit("(makeDDL) DDL requested for an unsupported layer [" + requestedLayer + "]");
		  return false;
	      }
	  }
	}
		
	//----------------------------------------------------------------
	private String makeTableName( String tabname , LAYER layer )
	//----------------------------------------------------------------
	{
	    // SST_<tablename>   => so if there is a source systemname remove it
	    // DVST_<sourcesys>_<table>   => so if there is no source systemname add it
		String sPrefix = null;
		String sTabNameTest = table_list.get(0).Name.trim().toUpperCase();
		boolean addSourceSystem=false;
		boolean removeSourceSystem=false;
  	    switch( layer )
	    {
  	      case SRC : ;
		  case ORCLEXT : {
			  int idx = this.getTableIdxViaName( tabname , "makeTableName" );
			  if( idx >= 0 ) {
				  pwdTable tab = table_list.get(idx);
				  return tab.SourceTableName.trim().toUpperCase();
			  }
			  errit("Strange - cannot find table name");
			  sPrefix = ""; break; }
		  case SST      : { 
	          sPrefix = "SST_";
	          // KB 01 DEC ook voor UCHP sourcesytem toevoegen  - dus disable onderstaande voortaan
	          //if (sTabNameTest.startsWith( sSourceSystem + "_" ) == true ) removeSourceSystem = true; 
	          if (sTabNameTest.startsWith( sSourceSystem + "_" ) == false ) addSourceSystem = true; 
	          break;     
	      }
	      case DVST     : { 
	    	  sPrefix = "DVST_";
	  	      if (sTabNameTest.startsWith( sSourceSystem + "_" ) == false ) addSourceSystem = true; 
	    	  break;  
	      }
	      case DELTA : {
	    	  return "VW_STOV_DELTA_" + tabname;
	      }
	      case DMST : {
	    	  return "DMST_" + tabname;
	      }
	      case DM : {
	    	  String martname =  getMartEntityName( tabname );
	    	  return "DM_WRRNTY_"  + martname;  // TODO WRRNTY wordt een input param
	      }
	      case STOV     : { 
	    	  sPrefix = "STOV_";
	  	      if (sTabNameTest.startsWith( sSourceSystem + "_" ) == false ) addSourceSystem = true; 
	  	      if (sTabNameTest.startsWith( "STOV_" + sSourceSystem + "_" ) ) { // sometimes STOV_UCHP_ in the excel
	  	    	sPrefix = ""; 
	  	    	addSourceSystem = false;
	  	      }
	    	  break;  
	      }
	      default       : { errit("(makeTableName) Unsupported layer [" + layer  +"]"); return tabname; }
	    }
  	    String sFullTabName = tabname;
  	    if( addSourceSystem )  sFullTabName = sSourceSystem + "_" + tabname;
  	    if( removeSourceSystem ) {
  	    	sFullTabName = tabname.substring( sSourceSystem.length()+1 );
  	    }
  	    sFullTabName = ( sPrefix + sFullTabName ).trim().toUpperCase();
  	    
  	    // KB - No idea why the above but for MDHUB use this approach
  	    if( this.sSourceSystem.compareToIgnoreCase("MDHUB") == 0 ) {
  	    	sFullTabName = (sPrefix + "MDHUB_" + tabname).trim().toUpperCase();
  	    	if( layer == readPwdXml.LAYER.STOV ) sFullTabName += "_ACT";
  	    }
  	    
  	    return sFullTabName;
	}
	
	
	//----------------------------------------------------------------
	private boolean makeDDLSpecific( LAYER layer )
	//----------------------------------------------------------------
	{
		if( createDDLScript() == false ) return false;
		//
		int maxlevel = 0;
		for(int i=0;i<table_list.size();i++)
		{
			if( table_list.get(i).dependency_depth > maxlevel ) maxlevel = table_list.get(i).dependency_depth;
		}
		for(int i=maxlevel;i>=0;i--)
		{
			makeDropStatement( layer , i );
		}
		for(int i=0;i<=maxlevel;i++)
		{
			if( makeDDLNiveau( layer , i) == false ) return false;
		}
		if( fout != null ) fout.close();
		return true;
	}
	
	//----------------------------------------------------------------
	private String makeOwnerName(String sIn)
	//----------------------------------------------------------------
	{
		String sOwner = sIn;
		if( sOwner == null ) sOwner = "";
		if( sOwner.length() < 1 ) sOwner = "STUDENT15";
		return sOwner.trim().toUpperCase();
	}
	
	//----------------------------------------------------------------
	private void makeDropStatement(LAYER layer , int depth)
	//----------------------------------------------------------------
	{
		for(int i=0;i<table_list.size();i++)
		{
			pwdTable tab = table_list.get(i);
			if( tab.dependency_depth != depth ) continue;  // truc om de creatie goed te krijgen
			String sLijn = "drop table " +  makeOwnerName( tab.OwnerName ) + "." + makeTableName( tab.Name , layer ) + ";";
			writeln( "--");
		    writeln( sLijn);			
		    writeln( "");
		}		
	}

	//----------------------------------------------------------------
	private boolean isPartOfFrameWork( String col )
	//----------------------------------------------------------------
	{
		// fields which are by default mandatory
		   if( col.compareToIgnoreCase("BATCH_ID") == 0 ) return true;
		   if( col.compareToIgnoreCase("EDW_CHECKSUM") == 0 ) return true;
		   if( col.compareToIgnoreCase("EDW_DQ_INFO") == 0 ) return true;
		   if( col.compareToIgnoreCase("RECORD_STATUS") == 0 ) return true;
		   if( col.compareToIgnoreCase("RECORD_SOURCE") == 0 ) return true;
		   if( col.compareToIgnoreCase("IBMSNAP_COMMITSEQ") == 0 ) return true;
		   if( col.compareToIgnoreCase("IBMSNAP_INTENTSEQ") == 0 ) return true;
		   if( col.compareToIgnoreCase("IBMSNAP_OPERATION") == 0 ) return true;
		   if( col.compareToIgnoreCase("IBMSNAP_LOGMARKER") == 0 ) return true;
		   return false;
	}
	
	//----------------------------------------------------------------
	private boolean isPartOfPrimaryKey( String tabname , String col )
	//----------------------------------------------------------------
	{
		   int tabidx = getTableIdxViaName( tabname.trim() , "isPartOfNaturalKey");
		   if( tabidx < 0 ) return false;
		   pwdTable tab = table_list.get( tabidx );
		   for(int i=0 ; i<tab.key_list.size(); i++ )
		   {
			 pwdKey key = tab.key_list.get(i);
			 if( key.isPrimary == false ) continue;
			 for(int j=0 ; j<key.keycol_list.size(); j++)
			 {
				 if( col.trim().compareToIgnoreCase( key.keycol_list.get(j) ) == 0 ) return true;
			 }
		   }
	       return false;		
	}
	
	//----------------------------------------------------------------
	private boolean isPartOfNaturalKey( String tabname , String col )
	//----------------------------------------------------------------
	{
	   int tabidx = getTableIdxViaName( tabname.trim() , "isPartOfNaturalKey");
	   if( tabidx < 0 ) return false;
	   pwdTable tab = table_list.get( tabidx );
	   for(int i=0 ; i<tab.key_list.size(); i++ )
	   {
		 pwdKey key = tab.key_list.get(i);
		 if( key.isPrimary ) continue;
		 for(int j=0 ; j<key.keycol_list.size(); j++)
		 {
			 if( col.trim().compareToIgnoreCase( key.keycol_list.get(j) ) == 0 ) return true;
		 }
	   }
       return false;		
	}
	
	// identifies _KEY columsn, ie . singel element FK that is linked to a single value PK which is the priamry key in dvst
	//----------------------------------------------------------------
	private boolean IsSingleElementFKReferringToSurrogate( String tabname , String col )
	//----------------------------------------------------------------
	{
	   int tabidx = getTableIdxViaName( tabname.trim() , "IsSingleElementFKReferringToSurrogate");
	   if( tabidx < 0 ) return false;
	   pwdTable tab = table_list.get( tabidx );
	   for(int i=0 ; i<tab.ref_list.size(); i++ )
	   {
		 pwdReference ref = tab.ref_list.get(i);
		 //  look for single value  referring 
		 if( ref.child_col_list.size() != 1 ) continue;
		 if( col.trim().compareToIgnoreCase( ref.child_col_list.get(0) ) != 0 ) continue;
         // look for single value referenced
		 if( ref.parent_col_list.size() != 1 ) continue;
		 // must be _KEY or KEY_ for an EDW surrogate key
		 String referenced = ref.parent_col_list.get(0) == null ? "" : ref.parent_col_list.get(0).trim().toUpperCase();
		 if( (referenced.startsWith("KEY_")) || (referenced.endsWith("_KEY")) ) return true;
	   }
       return false;		
	}
	
	//----------------------------------------------------------------
	private boolean makeDDLNiveau(LAYER layer , int depth)
	//----------------------------------------------------------------
	{
		if( table_list.size() < 1 ) {
			errit("(makeDDLNiveau) there is nothing to export");
			return false;
		}
	    //	
		String sLijn = "";
		for(int i=0;i<table_list.size();i++)
		{
			pwdTable tab = table_list.get(i);
			if( tab.dependency_depth != depth ) continue;  // truc om de creatie goed te krijgen
			String sTabName = makeTableName( tab.Name , layer );
			sLijn = "--=====================================================================";
			writeln("--");
			writeln( sLijn );
			//
			writeln("-- Table : " + tab.Name + "  ->  " + sTabName );
			writeln( sLijn );
			writeln("create table " + makeOwnerName( tab.OwnerName ) + "." + sTabName  + " (" );
	        //
			int teller=0;
			for(int j=0;j<tab.col_list.size();j++)
			{
			  pwdColumn col = tab.col_list.get(j);
			  if( requestedLayer == LAYER.SST ) {  // if _KEY en KEY_ then skip
				  String sTest = col.Name.toUpperCase().trim();
				  if( sTest.startsWith("KEY_") || sTest.endsWith("_KEY") ) {
					  if( col.IsNewColumnIndicator_SST == true ) {
						  errit("makeDDLNiveau - skipped creation of " + col.Name );
						  continue;
					  }
					  //continue; // skip
				  }
			  }
			  // 
			  teller++;
			  if( teller != 1 ) sLijn = ","; else sLijn = " "; 
			  String sColOut="";
			  switch( layer )
		      {
		      case SST     : {
		    	  // indien de primary key , dan overslaan
		    	  if( isPartOfPrimaryKey( tab.Name , col.Name ) ) {
		    		  errit("WARNING - newDVST flag is missing on [" + tab.Name + "." + col.Name + "]. DVST Surrogate Primary Key. Skipping");
		    		  teller--;
		    		  continue;
		    	  }
		    	  sColOut = col.Name; 
		    	  break; }
		      case DVST    : { sColOut = col.Name; break; }
		      case SRC     : { sColOut = col.SourceColumnName; break; }
		      case ORCLEXT : { sColOut = col.SourceColumnName; break; }
		      case STOV    : { sColOut = col.Name; break; }
		      case DELTA   : { sColOut = col.Name; break; }
		      case DMST    : { sColOut = col.Name; break; }
		      case DM      : { sColOut = col.Name; break; }
		      default      : { errit("(makeDDLNiveau-I) (sorceCol) Unsupported layer [" + layer  +"]"); return false; }
		      }
	//errit("" + sColOut + " " + col.DataType + " " + col.SourceDataType );		  
		      sLijn += String.format( "%-" + (this.maxColWidth+10) + "s" , sColOut.toUpperCase() );	
		      //
		      String sDataType = null;
		      switch( layer )
		      {
		      case SST     : {
		    	  // KB 10 May - for MDUB use the datatype nort converted to keep in line with previous version
		    	  // toch niet want anders heb je bigints op de SST en daarop TSTQ
		    	  //if( this.sSourceSystem.compareToIgnoreCase("MDHUB")==0) {
		    		//  sDataType = col.DataType; break;
		    	  //}
		    	  //else {
		    	    sDataType = col.VarcharConvertedDataType; break;
		    	  //}
		    	  }
		      //case DVST    : { sDataType = col.DataType; break; }
		      //case DVST    : { sDataType = col.VarcharConvertedDataType; break; }   // KB 6 NOV
		      case DVST    : { sDataType = col.DataType;
		                       // 29NOV2016 KB - Native Plus if GLOPPS : if text than use the varchar converted type
		                       // To prevent truncation on texts 
		                       if( sSourceSystem.trim().toUpperCase().startsWith("GLOP") ) {
		                    	   if( sDataType.trim().toUpperCase().indexOf("CHAR")>=0  ) {
		                    	   sDataType = col.VarcharConvertedDataType;    
		                    	   }
		                       }
		                       break; }   // KB 26 NOV - Native
			  case ORCLEXT : { sDataType = col.DataType; break; }
		      //case SRC     : { sDataType = col.DataType; break; }
		      case SRC     : { sDataType = col.SourceDataType; break; }   // KB 9 NOV
		      //case STOV    : { sDataType = col.SourceDataType; break; }  // KB 9 Nov
			  case STOV     : { sDataType = col.DataType; break; }
			  case DELTA    : { sDataType = col.DataType; break; }
			  case DMST     : { sDataType = col.DataType; break; }
			  case DM       : { sDataType = col.DataType; break; }
			  default   : { errit("(makeDDLNiveau-II) Unsupported layer [" + layer  +"]"); return false; }
		      }
		      sLijn += String.format( "%-20s" , sDataType);
		      //  
		      if( requestedLayer == LAYER.SST ) {   //  SST is NOT MANDATORY except key columns (unisue, foreign, etc)
		    	// look for _KEY and KEY_ columsn refering to the DVST surrogate key
		    	if( IsSingleElementFKReferringToSurrogate(tab.Name , col.Name ) ) {
		    		errit("Warning - newDVST flag is missing on [" + tab.Name + "." + col.Name + "] : FK destined for DVST. Skipping");
		    		continue;  // op die manier niet op de DDL
		    	}
		    	boolean ib = isPartOfFrameWork( col.Name );
		    	if( ib == false ) ib = isPartOfPrimaryKey( tab.Name , col.Name );
		    	if( ib == false ) ib = isPartOfNaturalKey( tab.Name , col.Name );
		        if( ib ) sLijn += " Not null";  else sLijn += " Null"; 
		      }
		      else {
		        if( col.IsMandatory ) sLijn += " not null"; 
		                         else sLijn += " null";	  
		      }
		      //
  	          writeln( sLijn );
		      
		    }
			// Keys
			for(int j=0;j<tab.key_list.size();j++)
			{
				pwdKey key = tab.key_list.get(j);
				if( requestedLayer == LAYER.SST ) {
					 if( key.isPrimary ) continue;   // skip the single column PK on _KEY field
				}
				sLijn = ",constraint ";
				sLijn += key.KeyName.trim().toUpperCase();
				if( key.isPrimary ) sLijn += " primary key ( "; else sLijn += " unique ( ";
				for(int k=0;k<key.keycol_list.size();k++)
				{
					if( k != 0 ) sLijn += " , ";
					sLijn += key.keycol_list.get(k).trim().toUpperCase();
				}
				sLijn += " )";
				//
			   writeln( sLijn );			
			}
			// FKS
			for(int j=0;j<tab.ref_list.size();j++)
			{
				pwdReference ref = tab.ref_list.get(j);
				
				// skip the foreign key that refers to a single value primary key of type _KEY or  KEY_
				if( requestedLayer == LAYER.SST ) {
				  if( ref.parent_col_list.size() > 0 ) {
				   String sTemp = ref.parent_col_list.get(0).trim().toUpperCase();
				   if( sTemp.startsWith("KEY_") || sTemp.endsWith("_KEY") ) continue;
				  }
				}
				
				sLijn = ",constraint ";
				sLijn += ref.ConstraintName.trim().toUpperCase();
				sLijn += " foreign key ( ";
				for(int k=0;k<ref.child_col_list.size();k++)
				{
					if( k != 0 ) sLijn += " , ";
					sLijn += ref.child_col_list.get(k).trim().toUpperCase();
				}
				sLijn += " ) ";
				writeln( sLijn ); 
				sLijn  = "     references ";
				sLijn += makeOwnerName( tab.OwnerName ) + ".";
				sLijn += makeTableName ( ref.ParentTable.trim().toUpperCase() , layer );
				sLijn += " ( ";
				for(int k=0;k<ref.parent_col_list.size();k++)
				{
					if( k != 0 ) sLijn += " , ";
					sLijn += ref.parent_col_list.get(k).trim().toUpperCase();
				}
				sLijn += " ) ";
				writeln( sLijn );
				if( ref.child_col_list.size() > 1 ) writeln("     match full");
				writeln("     on update restrict");
				writeln("     on delete restrict");
				writeln("     not deferrable");
				//
			}
			//
			if( (requestedLayer == LAYER.SST) || (requestedLayer == LAYER.DVST) ) {
			 writeln(") distribute on random;");
			 writeln("--");
			}
			if( (requestedLayer == LAYER.STOV) || (requestedLayer == LAYER.DELTA)||(requestedLayer == LAYER.DMST) ) {
			 writeln(");");
			 writeln("--");	
			}
			if( requestedLayer == LAYER.DM ) {
				 writeln(") distribute on ( " + (tab.Name + "_DM_SID").toLowerCase() + ");");
				 writeln("--");
			}
			if( requestedLayer == LAYER.SRC ) {
				 writeln(");");
				 writeln("--");
				}
			// Ext tables
			if( requestedLayer == LAYER.ORCLEXT ) {
				writeln(") ORGANIZATION EXTERNAL FIELDS (");
			    for(int k=0 ; k<tab.col_list.size() ; k++ )
			    {
			    	pwdColumn c = tab.col_list.get(k);
			    	if( k==0 ) sLijn = " "; else sLijn = ",";
			    	String sColOut="";
					switch( layer )
				    {
				      case SST     : { sColOut = c.Name; break; }
				      case DVST    : { sColOut = c.Name; break; }
				      case SRC     : { sColOut = c.SourceColumnName; break; }
				      case ORCLEXT : { sColOut = c.SourceColumnName; break; }
				      default   : { errit("(makeDDLNiveau-III) (sorceCol) Unsupported layer [" + layer  +"]"); return false; }
				    }
		    	    sLijn += String.format( "%-" + (this.maxColWidth+3) + "s" , sColOut.toUpperCase() );	
			    	sLijn += " POSITION (" + c.positionStart + ":" + c.positionStop + ")";
			    	writeln( sLijn );
			    }
				writeln(");");
			}	
		}
		return true;
	}
	
	//----------------------------------------------------------------
	public String getDDLScriptName()
	//----------------------------------------------------------------
	{
		  String sDir = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Ddl" + xMSet.xU.ctSlash;
		  String FName = sDir + (sSourceSystem + "_" + requestedLayer).trim().toLowerCase() + ".ddl";
		  return FName;
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
	     sLijn += "-- Source        : " + sXmlFile + xMSet.xU.ctEOL;
	     sLijn += "-- Project       : " + xMSet.getCurrentProject() + xMSet.xU.ctEOL;
	     sLijn += "-- Layer         : " + this.requestedLayer + xMSet.xU.ctEOL;
	     sLijn += "-- Source System : " + sSourceSystem + xMSet.xU.ctEOL;
	     sLijn += "-- Created on    : " + xMSet.xU.prntStandardDateTime(System.currentTimeMillis()) + xMSet.xU.ctEOL;
	     sLijn += "-- Created by    : " + xMSet.whoami() + xMSet.xU.ctEOL;
	     return sLijn;
	}
	//----------------------------------------------------------------
	private boolean createDDLScript()
	//----------------------------------------------------------------
	{
		  fout = new gpPrintStream( getDDLScriptName() , "UTF-8" );
		  fout.println(getFileHeader());
	      logit(5,"DDL created [" + getDDLScriptName() + "]");
		  return true;
	}

	//----------------------------------------------------------------
	private void writeln(String sIn)
	//----------------------------------------------------------------
	{
		if( fout == null ) {
			logit(9,sIn);
			return;
		}
		fout.println(sIn);
	}
	
	//----------------------------------------------------------------
	private void removeLookup()
	//----------------------------------------------------------------
	{
		String FName = this.getLookupFileName();
		if( xMSet.xU.IsBestand(FName) ) xMSet.xU.VerwijderBestand( FName );
	}
	
	//----------------------------------------------------------------
	private boolean dumpLookup()
	//----------------------------------------------------------------
	{
		pwdDumpCache dmp = new pwdDumpCache( xMSet );
		return dmp.dumpcache( sSourceSystem , ""+requestedLayer , table_list );
	}

	//----------------------------------------------------------------
	private boolean makeDeltaView()
	//----------------------------------------------------------------
	{
	    String sDir = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Ddl" + xMSet.xU.ctSlash;
	    String FName = sDir + (sSourceSystem + "_Delta_Vw_" + requestedLayer).trim().toLowerCase() + ".ddl";
	    fout = new gpPrintStream( FName , "UTF-8" );
		fout.println(getFileHeader());
	    logit(5,"DDL created [" + FName + "]");
	    //
	    String sLine = "";
		for(int i=0;i<table_list.size();i++)
		{
			pwdTable tab = table_list.get(i);
			int mix = getTabIdxScopeForMart( tab.Name );
			if( mix < 0 )  continue;
			String MartEntityName = this.mart_scope_list.get(mix).DMEntityName;
			//String viewname = "vw_stov_delta_" + (sSourceSystem).trim() + "_" + tab.Name.toLowerCase();
			String viewname = makeTableName( tab.Name , LAYER.DELTA );
			writeln("--");
			writeln("--");
			sLine = "--=====================================================================";
			writeln("--");
			writeln( sLine );
			writeln("-- View : " + tab.Name + "  ->  " + viewname );
			writeln( sLine );
			//
			sLine = "CREATE OR REPLACE VIEW  " + viewname + " (";
			writeln(sLine);
			for(int k=0;k<tab.col_list.size();k++)
			{
				sLine = tab.col_list.get(k).Name.toLowerCase();
				sLine = ( k == 0 ) ? "  " + sLine : ", " + sLine;
				writeln(sLine);
			}
			
			writeln(") AS SELECT ");
			//
			for(int k=0;k<tab.col_list.size();k++)
			{
				sLine = tab.col_list.get(k).Name;
				if( sLine.compareToIgnoreCase( tab.Name + "_DV_SID" ) == 0 ) {
					sLine = "E.HUB_" + tab.Name + "_SID AS " + tab.Name + "_DV_SID";
				}
				else
				if( sLine.compareToIgnoreCase( tab.Name + "_DM_SID" ) == 0 ) {
					sLine = "(D.counter + F.start_pos) AS " + tab.Name + "_DM_SID";
				}
				else {
					sLine = "E." + sLine.toLowerCase();
				}
				sLine = ( k == 0 ) ? "  " + sLine : ", " + sLine;
				writeln(sLine);
			}
			//
	        String CRLF = xMSet.xU.ctEOL;
			String sRep = 
"  FROM dev_edw_stov..stov_uchp_$STOV$_act E  INNER JOIN" + CRLF +
"(" + CRLF +
"SELECT C.key , C.batch_id_dv , row_number() OVER (ORDER BY NULL) counter" + CRLF +
"FROM"+ CRLF +
"(" + CRLF +
"SELECT A.$STOV$_key AS key , A.batch_id AS batch_id_dv , COALESCE( B.$STOV$_dm_sid , -99 ) AS right_sid" + CRLF +
"  FROM dev_edw_stov..stov_uchp_$STOV$_act A   LEFT JOIN  dm_wrrnty_$DM$ B" + CRLF +
"    ON ( A.$STOV$_key = B.$STOV$_key AND A.batch_id = B.dv_batch_id )" +  CRLF +
"   WHERE A.batch_id >=  ( SELECT COALESCE(MAX( dv_batch_id ),0) FROM dm_wrrnty_$DM$ )" + CRLF +
") C" + CRLF +
"WHERE C.right_sid < 0" + CRLF +
") D" +   CRLF +
"ON( E.$STOV$_key = D.key AND E.batch_id = D.batch_id_dv )"  + CRLF +
"CROSS JOIN" + CRLF +
"( SELECT COALESCE(MAX( $STOV$_dm_sid ),0) AS start_pos FROM dm_wrrnty_$DM$ ) F";
			//
			sRep = xMSet.xU.Remplaceer(sRep , "$STOV$" , tab.Name.trim().toLowerCase() );
			sRep = xMSet.xU.Remplaceer(sRep , "$DM$" , MartEntityName.trim().toLowerCase() );
			//
			writeln(sRep);
			writeln(";");
		}
		if( fout != null ) fout.close();
		return true;
	}
	
	//----------------------------------------------------------------
	private String getMartEntityName( String stab )
	//----------------------------------------------------------------
	{
	  int idx = getTabIdxScopeForMart( stab );
	  if( idx < 0 ) return stab;
	  String mname = mart_scope_list.get(idx).DMEntityName;
	  if( mname == null ) return stab;
	  return mname.trim().toUpperCase();
	}
	
	//----------------------------------------------------------------
	private int getTabIdxScopeForMart(String stab )
	//----------------------------------------------------------------
	{
		for(int i=0;i<mart_scope_list.size();i++)
		{
			if( mart_scope_list.get(i).TableName.compareToIgnoreCase( stab ) == 0 ) return i;
		}
		return -1;
	}
	
	//----------------------------------------------------------------
	private boolean isTabInScopeForMart(String stab )
	//----------------------------------------------------------------
	{
	    if(  getTabIdxScopeForMart( stab ) < 0 ) return false;
	    return true;
	}

	//----------------------------------------------------------------
	private boolean isColInScopeForMart(String stab , String scol)
	//----------------------------------------------------------------
	{
		for(int i=0;i<mart_scope_list.size();i++)
		{
			if( mart_scope_list.get(i).TableName.compareToIgnoreCase( stab ) != 0 ) continue;
			pwdMartTable tab = mart_scope_list.get(i);
			for(int k=0;k<tab.col_list.size();k++)
			{
				if( tab.col_list.get(k).Name.compareToIgnoreCase( scol ) == 0) return true;
			}
		}
		return false;
	}
	
	// removes all columns from the PowerDesigner list which are not in scope
	//----------------------------------------------------------------
	private boolean keepsWhatsInscope(LAYER requestedlayer)
	//----------------------------------------------------------------
	{
		if( requestedlayer == LAYER.DM )
		{
		 int remove=0;
		 for(int i=0;i<table_list.size();i++)
		 {
			pwdTable tab = table_list.get(i);
			for(int j=0;j<tab.col_list.size();j++)
			{
				pwdColumn col = tab.col_list.get(j);
				if( isColInScopeForMart( tab.Name.trim() , col.Name.trim() )) continue;
			    // Keep the SID and KEYS	
				if( col.Name.toUpperCase().trim().endsWith("_SID") ) continue;
				if( col.Name.toUpperCase().trim().endsWith("_KEY") ) continue;
				// Keep COMPANYNO - TYPEOFCONCERN
				if( col.Name.toUpperCase().trim().indexOf("COMPANYNO") >= 0 ) continue;
				if( col.Name.toUpperCase().trim().indexOf("TYPEOFCONCERN") >= 0) continue;
				//
				tab.col_list.get(j).DataType = "%MUST_BE_REMOVED%";
				remove++;
			}
		 }
		 //
		 int scrapped=0;
		 for(int k=0;k<remove;k++)
		 {
			for(int i=0;i<table_list.size();i++)
			{
				for(int j=0;j<table_list.get(i).col_list.size();j++)
				{
					if( table_list.get(i).col_list.get(j).DataType.compareToIgnoreCase( "%MUST_BE_REMOVED%") != 0 ) continue;
					logit(9, "Removing column [" + table_list.get(i).Name + "." + table_list.get(i).col_list.get(j).Name + "]" );
					table_list.get(i).col_list.remove(j);
					scrapped++;
					break;
				}
			}
			if( scrapped >= remove ) break;
		 }
		 if( scrapped != remove ) {
			errit("System error III - removing columns not in scope");
			return false;
		 }
		}
		// Remove empty tables
		int aantal = table_list.size();
		for(int k=0;k<aantal;k++)
		{
			for(int i=0;i<table_list.size();i++)
			{
				// in scope
				if( this.isTabInScopeForMart( table_list.get(i).Name ) == true ) continue;
				logit(1,"Removing EMPTY table [" + table_list.get(i).Name + "]");
				table_list.remove(i);
				break;
			}
		}
		//
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean setDataMartOrder()
	//----------------------------------------------------------------
	{
		// Sorts the columns as follows
		// 0 _DM_SID
		// 1 _DV_SID
		// 3 _SID
		// 5 table_KEY
		// 6 _KEY
		// rest  + 100
		// 900 RECORD_STATUS
		// 901 RECORD_SOURCE
		// 902 BATCH_ID_I , U , D
		// 903 DM_ST_DATETIME
		// 904 DM_END_DATETIME
		// 905 CRE_TMST
		
		for(int i=0;i<table_list.size();i++)
		{
			pwdTable tab = table_list.get(i);
			for(int j=0;j<tab.col_list.size();j++)
			{
				//int seq = tab.col_list.get(j).ColumnSequence;
				String colname = tab.col_list.get(j).Name;
				if( colname == null )  continue;
				colname = colname.trim().toUpperCase();
				//
				if( (colname.endsWith("_DM_SID")) && (colname.startsWith(tab.Name)) ) tab.col_list.get(j).ColumnSequence = 0;
				else
				if( colname.endsWith("_DV_SID") ) tab.col_list.get(j).ColumnSequence = 2;
				else
				if( colname.endsWith("_DM_SID") ) tab.col_list.get(j).ColumnSequence = 3;
				else
				if( colname.endsWith("_SID") ) tab.col_list.get(j).ColumnSequence = 4;
				else
				if( colname.compareTo( tab.Name + "_KEY") == 0) tab.col_list.get(j).ColumnSequence = 6;
				else
				if( colname.endsWith("_KEY") ) tab.col_list.get(j).ColumnSequence = 8;
				else
				if( colname.indexOf("COMPANYNO") >= 0 ) tab.col_list.get(j).ColumnSequence = 10;
				else
				if( colname.indexOf("TYPEOFCONCERN") >= 0 ) tab.col_list.get(j).ColumnSequence = 12;
				else
				if( colname.endsWith("RECORD_STATUS") ) tab.col_list.get(j).ColumnSequence = 900;
				else
				if( colname.endsWith("RECORD_SOURCE") ) tab.col_list.get(j).ColumnSequence = 902;
				else
				if( colname.startsWith("BATCH_ID") ) tab.col_list.get(j).ColumnSequence = 904;
				else
				if( colname.startsWith("DV_") ) tab.col_list.get(j).ColumnSequence = 906;
				else
				if( colname.startsWith("DM_") ) tab.col_list.get(j).ColumnSequence = 908;
				else
				tab.col_list.get(j).ColumnSequence = 100 + j;	
			}
		}
		//
		return true;
	}
	
	
	private void dumpKeyList( ArrayList<pwdKey> keylist )
	{
		for(int i=0;i<keylist.size();i++)
		{
			pwdKey key = keylist.get(i);
			errit(" ==> Key [" + key.KeyName + "] [PK=" + key.isPrimary + "]" + key.keycol_list );
		}
	}
	
}
