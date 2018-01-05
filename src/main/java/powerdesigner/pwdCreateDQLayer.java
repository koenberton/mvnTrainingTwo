package powerdesigner;

import java.util.ArrayList;


import pcGenerator.ddl.ddlDataTypeConvertor;
import pcGenerator.ddl.rdbmsDatatype;
import pcGenerator.generator.generatorSrcTgtManager;
import pcGenerator.powercenter.infaSource;
import generalpurpose.gpPrintStream;
import generalpurpose.pcDevBoosterSettings;


public class pwdCreateDQLayer {
	
		pcDevBoosterSettings xMSet=null;
		generatorSrcTgtManager stmngr = null;
		ddlDataTypeConvertor conv = null;
		
		private gpPrintStream fout = null;
	
		private String sSourceSystem = null;
		private boolean parseOK = true;
		private ArrayList<pwdTableCache> view_list =  null;
		private ArrayList<pwdTableCache> source_list =  null;
		private int max_col_width = 30;
		
	    rdbmsDatatype.DBMAKE srcDBTipe = null;
	    ArrayList<infaSource> ddlList = null;
	    ArrayList<pwdTable> new_table_list = null;
	    
		class tableXref
		{
		  String pwdTableName;
		  String sourceTableName;
		  String stofViewName;
		  tableXref(String sv)
	      {
	        	  sourceTableName=null;
	        	  pwdTableName=null;
	        	  stofViewName=sv;  
	      }
		}				
	    ArrayList<tableXref> viewList = null;
	    class sourceTableInfo
	    {
	    	  String tableName;
	    	  String sourceTableName;
			  sourceTableInfo(String stab , String sstab )
		      {
		        	  sourceTableName=sstab;
		        	  tableName=stab;  
		      }
	    }
	    ArrayList<sourceTableInfo> sourcetableList=null;
		
	    
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
		public pwdCreateDQLayer(pcDevBoosterSettings im )
		//----------------------------------------------------------------
		{
			xMSet = im;
			conv = new ddlDataTypeConvertor(xMSet);
			viewList = new ArrayList<tableXref>();
			sourcetableList = new ArrayList<sourceTableInfo>();
			new_table_list = new ArrayList<pwdTable>();
		}
		
		//----------------------------------------------------------------
		private void usage()
		//----------------------------------------------------------------
		{
			errit("(MAKE-DQ SYS= SRCDATABASEMAKE={ORACLE,DB2,NETEZZA}");
		}
		
		//  Input of datatype specifications are the Lookups created by readPwdXML
		//----------------------------------------------------------------
		public boolean makeDQLayer(String[] args)
		//----------------------------------------------------------------
		{
			String STOVLKPFile = null;
			String SRCLKPFile = null;
			String sDbMake=null;
			for(int i=0;i<args.length ; i++)
			{
				String ss = args[i].trim();
				if( ss.startsWith("SYS=") ) {
					sSourceSystem = ss.substring("SYS=".length());
					if( sSourceSystem.length() < 1 ) sSourceSystem = null;
				}
				else
				if( ss.startsWith("SRCDATABASEMAKE=") ) {
						sDbMake = ss.substring("SRCDATABASEMAKE=".length());
						if( sDbMake.length() < 1 ) sDbMake = null;
			    }
				else {
					errit("Unsupported commandline element [" + ss + "]");
					parseOK=false;
				}
			}
			if( (sSourceSystem==null)  ) {
			    usage();
			    errit("missing options");
			    return false;		
			}
			if( sDbMake == null  ) {
				usage();
				errit("SRCDATABASEMAKE is missing");
				return false;
			}
			// lookups
			String sDir = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Lookup" + xMSet.xU.ctSlash;
			STOVLKPFile = sDir + "LKP_" + sSourceSystem + "_stov.xml";
			if( xMSet.xU.IsBestand( STOVLKPFile ) == false ) {
				errit("Cannot locate STOV Lookup file [" + STOVLKPFile + "]");
				return false;
			}
			SRCLKPFile = sDir + "LKP_" + sSourceSystem + "_sst.xml";
			if( xMSet.xU.IsBestand( SRCLKPFile ) == false ) {
				errit("Cannot locate STOV Lookup file [" + SRCLKPFile + "]");
				return false;
			}
			//
			rdbmsDatatype rd = new rdbmsDatatype(xMSet);
			srcDBTipe = rd.getDBMAKEType(sDbMake);
			if( srcDBTipe == null ) {
				return false;
			}
			// Housekeeping
			if( xMSet.xU.IsBestand( getDDLName())) {
				xMSet.xU.VerwijderBestand( getDDLName() );
				if( xMSet.xU.IsBestand( getDDLName()))  {
					errit("Cannot remove [" + getDDLName() + "]");
					parseOK=false;
				}
			}
			//
			if( !parseOK ) return false;
			// read metadata from the Lookup areas
			if( read_View_Cache(STOVLKPFile) == false ) return false;
			if( read_Source_Cache(SRCLKPFile) == false ) return false;
			//
			do_max_col_width();
			//
			if( do_create_all_dq() == false ) return false;
			//
			if( do_create_missing_cols() == false ) return false;
			// create DDL script
			if( dump_to_ddl() == false ) return false;
	        // dump the caches		
			if( dump_to_lookup() == false ) return false;
			//
			return true;
		}
		
		//----------------------------------------------------------------
		public String getDatabaseName()
		//----------------------------------------------------------------
		{
	       return (sSourceSystem.trim() + "_DQ").toUpperCase().trim();		
		}
		
		//----------------------------------------------------------------
		private boolean read_Source_Cache(String FName)
		//----------------------------------------------------------------
		{
			pwdReadCache rd = new pwdReadCache( xMSet );
	        source_list = rd.getCachedLookUps(FName);
	        if( source_list == null ) {
	        	errit("Cannot read cache list from [" + FName + "]");
	        	return false;
	        }
	        if( source_list.size() == 0 ) {
	        	errit("There are no entries in the source cache [" + FName + "]");
	        	return false;
	        }
			return true;
		}
		
		//----------------------------------------------------------------
		private boolean read_View_Cache(String FName)
		//----------------------------------------------------------------
		{
			pwdReadCache rd = new pwdReadCache( xMSet );
		    view_list = rd.getCachedLookUps(FName);
		    if( view_list == null ) {
		       	errit("Cannot read cache list from [" + FName + "]");
		       	return false;
		    }
		    if( view_list.size() == 0 ) {
	        	errit("There are no entries in the view cache [" + FName + "]");
	        	return false;
	        }
		    return true;
		}
		
		//----------------------------------------------------------------
		private void do_max_col_width()
		//----------------------------------------------------------------
		{
			for(int i=0;i<view_list.size();i++)
			{
			 for(int j=0;j<view_list.get(i).collist.size();j++)
			 {
			   	int k = view_list.get(i).collist.get(j).ColumnName.length();
			   	if( k > max_col_width ) max_col_width = k;
			 }
			}
			max_col_width += 5;
		}
		
		//----------------------------------------------------------------
		private String getFileHeader()
		//----------------------------------------------------------------
		{
			 String sLijn = "";
			 sLijn += "-- Application   : " + xMSet.getApplicationId() + xMSet.xU.ctEOL;
		     //sLijn += "-- Source        : " +  + xMSet.xU.ctEOL;
		     sLijn += "-- Project       : " + xMSet.getCurrentProject() + xMSet.xU.ctEOL;
		     sLijn += "-- Layer         : " + "DATAQUALITY" + xMSet.xU.ctEOL;
		     sLijn += "-- Source System : " + sSourceSystem + xMSet.xU.ctEOL;
		     sLijn += "-- Created on    : " + xMSet.xU.prntStandardDateTime(System.currentTimeMillis()) + xMSet.xU.ctEOL;
		     sLijn += "-- Created by    : " + xMSet.whoami() + xMSet.xU.ctEOL;
		     return sLijn;
		}
		
		//----------------------------------------------------------------
	    private void writeln(String sIn)
	    //----------------------------------------------------------------
	    {
	    	if( fout != null ) {
	    		fout.println( sIn );
	    		return;
	    	}
	    	logit(1,sIn);
	    }
	
		//----------------------------------------------------------------
	    public String getDDLName()
	   	//----------------------------------------------------------------
	    {
	    	return xMSet.getProjectDir() + xMSet.xU.ctSlash + "Ddl" + xMSet.xU.ctSlash + sSourceSystem + "_DQ.ddl";
	    }
	    
		//----------------------------------------------------------------
		private boolean do_create_all_dq()
		//----------------------------------------------------------------
		{
			for(int i=0;i<view_list.size();i++)
			{
			   do_create_dq( view_list.get(i) );	
			}
			return true;
		}
		
		//----------------------------------------------------------------
		private boolean do_create_dq( pwdTableCache vw )
		//----------------------------------------------------------------
		{
			pwdTable tnew = new pwdTable();
			tnew.Name  = vw.Name;
			tnew.Code  = tnew.Name;
			tnew.SourceFileDelimiter = "";
			tnew.CR = vw.CR;
			tnew.SourceTableName =  "DQ_" + sSourceSystem + "_" + vw.Name.toUpperCase();
			
			
			for(int i=0;i<vw.collist.size();i++)
			{
				pwdColumnCache ff = vw.collist.get(i);
				String newdatatype = null;
			
				// fetch original datattype; if CHAR then re-use the view;; else overrule
				if( ff.DataType.toUpperCase().indexOf("CHAR") <= 0 ) {  // not a character, just reuse
					newdatatype = ff.DataType;
				}
				else {
					// CHAR datatypes on view
					// er zijn een aantal kolmommen die men mag negeren doordat die automatisch toegevoegd worden
					String odatatype = null;
					//if( ff.ColumnName.toUpperCase().startsWith("EDW_DQ") ) {
					//   odatatype = null;
					//}
					//else {
					   odatatype = get_original_datatype( vw.Name , ff.ColumnName , ff.DataType );
					//}
					if ( odatatype == null ) {
						odatatype = ff.DataType;
					}
					// if original datatype is CHAR then just use the precision/Scale from the STOV view
					
					if( odatatype.toUpperCase().indexOf("CHAR") >= 0 ) {
						newdatatype = ff.DataType;
					}
					else {   // transformeren
						newdatatype = transformdatatype( odatatype );
					}
				}
			    // mandatory - go back to source def
				boolean iman = get_original_mandatory( vw.Name , ff.ColumnName , ff.isMandatory );
				//   Name = Powerdesigner   Soure =  new tabel, dus de DQ
				pwdColumn cnew = new pwdColumn();
				cnew.Name              = ff.ColumnName; // ff.SourceColumnName;   // name of the column on the view
				cnew.Code              = cnew.Name;
				cnew.EDWCheckSum       = false;
				cnew.SourceColumnName  = ff.SourceColumnName;
			    cnew.DataType          = ff.DataType; // newdatatype;
			    cnew.IsMandatory       = ff.isMandatory; //iman;
			    cnew.SourceDataType    = newdatatype; // ff.DataType;
			    cnew.IsSourceMandatory = iman; //ff.isMandatory;
			    
			    tnew.col_list.add( cnew );
			}
			
			new_table_list.add( tnew );
			return true;
		}
	
		//----------------------------------------------------------------
		private pwdColumnCache  getColumnCache (  String tabname , String colname )
		//----------------------------------------------------------------
		{
			int idx = -1;
			for(int i=0;i<source_list.size();i++)
			{
 			  if( source_list.get(i).Name.compareToIgnoreCase( tabname ) == 0 ) {
				  idx = i;
				  break;
			  }
			}
			if( idx < 0 ) {
				errit("Cannot find original datatype for [" + tabname + "." + colname + "] : table not found" );
				return null;
			}
			//
            pwdTableCache tt = source_list.get(idx);			
			idx = -1;
			for(int i=0;i<tt.collist.size();i++)
			{
				if( tt.collist.get(i).ColumnName.compareToIgnoreCase(colname) == 0 ) {
					idx = i;
					break;
				}
//errit( "[" + tabname + "." + colname + "] " +  tt.collist.get(i).ColumnName + " " + tt.collist.get(i).ColumnName.compareToIgnoreCase(colname) );
			}
			if( idx < 0 ) {
				errit("Cannot find original datatype for [" + tabname + "." + colname + "] : column not found on " + tabname );
				return null;
			}
			return tt.collist.get(idx);
		}
		
		//----------------------------------------------------------------
		private String get_original_datatype( String tabname , String colname , String datatype)
		//----------------------------------------------------------------
		{
			// some columns are exempt
			if( colname.toUpperCase().startsWith("IBMSNAP_")) return datatype;
			if( colname.toUpperCase().startsWith("EDW_CHECKSUM")) return datatype;
			if( colname.toUpperCase().startsWith("RECORD_STATUS")) return datatype;
			if( colname.toUpperCase().startsWith("RECORD_SOURCE")) return datatype;
			// STOV fields
			if( colname.toUpperCase().startsWith("LOAD_ST_DATETIME")) return datatype;
			if( colname.toUpperCase().startsWith("LOAD_END_DATETIME")) return datatype;
			if( colname.toUpperCase().startsWith("LOAD_DATETIME")) return datatype;
			if( colname.toUpperCase().startsWith("DV_CURRENT_FLAG")) return datatype;
			if( colname.toUpperCase().startsWith("DV_VERSION")) return datatype;
			//
			pwdColumnCache cc = getColumnCache( tabname , colname );
			if( cc == null ) return null;
      		return cc.SourceDataType;
		}
		
		//----------------------------------------------------------------
	    private boolean get_original_mandatory( String tabname , String colname , boolean ismandatory )
		//----------------------------------------------------------------
		{
			if( ismandatory ) return true;
			// some columns are by definition required
			if( colname.toUpperCase().startsWith("IBMSNAP_")) return true;
			if( colname.toUpperCase().startsWith("EDW_CHECKSUM")) return true;
			if( colname.toUpperCase().startsWith("RECORD_STATUS")) return true;
			if( colname.toUpperCase().startsWith("RECORD_SOURCE")) return true;
			if( colname.toUpperCase().startsWith("KEY_")) return true;
			if( this.sSourceSystem.compareToIgnoreCase("MDHUB")!=0) {   // MDHUB has some _KEY colums on the source views
			   if( colname.toUpperCase().endsWith("_KEY")) return true;
			}
			if( colname.toUpperCase().endsWith("_SID")) return true;
			//
			pwdColumnCache cc = getColumnCache( tabname , colname );
			if( cc == null ) return false;
      		return cc.isSourceMandatory;
		}
		
	    
	  //----------------------------------------------------------------
	    private boolean do_create_missing_cols()
	    //----------------------------------------------------------------
	    {
	    	for(int i=0;i< new_table_list.size();i++)
			{
	    		pwdColumn cnew = new pwdColumn();
				cnew.Name              = "EDW_DQ_INFO";
				cnew.Code              = cnew.Name;
				cnew.EDWCheckSum       = false;
				cnew.SourceColumnName  = cnew.Name;
			    cnew.DataType          = "NVARCHAR(300)";
			    cnew.IsMandatory       = true;
			    cnew.SourceDataType    = cnew.DataType;
			    cnew.IsSourceMandatory = false;
			    //
			    new_table_list.get(i).col_list.add( cnew );
			}
	    	return true;
	    }
	    
	    
		//----------------------------------------------------------------
		private String transformdatatype( String sIn)
		//----------------------------------------------------------------
		{
			String sTemp = xMSet.xU.removeBelowIncludingSpaces(sIn).trim().toUpperCase();
			String sTipe  = null;
			int precision =-1;
			int scale     = -1;
			int idx = sTemp.indexOf("(");
			if( idx < 0 ) {
				sTipe = sTemp;
				precision=-1;
				scale=-1;
			}
			else {
				sTipe = sTemp.substring(0,idx);
				String sRest = sTemp.substring(idx);
				if( sRest.endsWith(")") == false) {
					errit("Invalid datatype [" + sIn + "] : missing right parenthesis");
					return null;
				}
				sRest = sRest.substring( 1 , sRest.length() - 1);
				idx = sRest.indexOf(","); 
				if( idx < 0 ) {   // N( a )
					int i = xMSet.xU.NaarInt( sRest );
				    if( i < 0) {
				      errit("Invalid datatype [" + sIn + "] :  precision in  A(n) cannot be derived");
					  return null;
				    }
				    precision = i;
				    scale = -1;
				}
				else {   // N( a , b )
					String sPrec = sRest.substring(0,idx);
					String sScale = sRest.substring(idx+1);
					int i = xMSet.xU.NaarInt( sPrec );
				    if( i < 0) {
					  errit("Invalid datatype [" + sIn + "] :  precision in  A(n,m) cannot be derived");
					  return null;
					}
				    precision = i;
				    i = xMSet.xU.NaarInt( sScale );
				    if( i < 0) {
					  errit("Invalid datatype [" + sIn + "] :  scale in  A(n,m) cannot be derived");
					  return null;
					}
				    scale = i;
				}
			}
			//
			String tgtTipe = conv.getEquivalentDataType(sTipe,precision, scale, srcDBTipe , rdbmsDatatype.DBMAKE.NETEZZA );
			//logit(5, "IN tipe=" + sTipe + "prec=" + precision + "scale=" + scale +  "  OUT tipe=" + tgtTipe );
			
			// overrule : DECIMAL(n) / NUMERIC(n) must become een BIGINT  (NZ guideline)
			if( (tgtTipe.compareToIgnoreCase("DECIMAL") == 0) || (tgtTipe.compareToIgnoreCase("NUMERIC") == 0) ) {
				if( scale == -1 ) {
					tgtTipe = "BIGINT";
					precision =-1;
					scale=-1;
				}
			}
			
	
			String sPrec = "?";
			if( conv.expectPrecision(tgtTipe.trim().toUpperCase()) == false ) {
				sPrec = "";
			}
			else {
			 if( precision > 0) {
				if( scale > 0 ) {
					sPrec = "(" + precision + "," + scale + ")";
				}
				else sPrec = "(" + precision + ")";
			 }
			 else sPrec = "";
			}
			return (tgtTipe + sPrec).trim();
		}
		
		//----------------------------------------------------------------
		private boolean dump_to_ddl()
		//----------------------------------------------------------------
		{
			fout = new gpPrintStream( getDDLName() , "UTF-8");
			writeln( getFileHeader() );
			//
			String sLijn = "";
			for(int i=0;i< new_table_list.size();i++)
			{
			    pwdTable tab = new_table_list.get(i);
				writeln( "-- ") ;
				writeln( "create table " + (getDatabaseName() + ".DQ_" + sSourceSystem + "_" + tab.Name).toUpperCase()  + "(");  
                //
				for(int j=0;j<tab.col_list.size();j++)
				{
					pwdColumn col = tab.col_list.get(j);
					//
					sLijn = ( j == 0 ) ? "  " : ", ";
					sLijn += String.format("%-" + max_col_width + "s", col.SourceColumnName );   
			        //
					sLijn += " " + col.SourceDataType.toLowerCase();
					String sNull = (col.IsSourceMandatory == true ) ? "not null"  : "null";
				    //
					sLijn += "  " + sNull;
					writeln( sLijn );
				}
				writeln(") distribute on random;");
				writeln("");
			}
			fout.close();
			return true;
		}

		//----------------------------------------------------------------
		private boolean dump_to_lookup()
		//----------------------------------------------------------------
		{
			pwdDumpCache dmp = new pwdDumpCache( xMSet );
			return dmp.dumpcache( sSourceSystem , "dq" , new_table_list );
		}
}
