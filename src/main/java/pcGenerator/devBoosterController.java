package pcGenerator;
import office.xlsxCompare;
import office.xlsxToTxt;
import generalpurpose.pcDevBoosterSettings;
import pcGenerator.ddl.ddl2PowerDesignerExcel;
import pcGenerator.ddl.ddlCompare;
import pcGenerator.ddl.ddlTranslator;
import pcGenerator.ddl.externalTablesORCLImport;
import pcGenerator.ddl.infaImport;
import pcGenerator.ddl.powerDesignerDDLImport;
import pcGenerator.generator.generateMapping;
import pcGenerator.netezzaView.nzViewParser;
import powerdesigner.pwdController;
import powerdesigner.pwdCreateDQLayer;
import powerdesigner.pwdExtTblToExcel;
import powerdesigner.pwdMikelToDDL;



public class devBoosterController {
    
	pcDevBoosterSettings xMSet = null;
    
	public devBoosterController(pcDevBoosterSettings xi)
	{
		xMSet = xi;
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
	public void close()
	//----------------------------------------------------------------
	{
		xMSet.closeall();
	}
	
	//----------------------------------------------------------------
	public boolean import_Generic_DDL( String dbmake , String sDatabaseName , String InputFileName , String ConfigFileNameIn )
	//----------------------------------------------------------------
	{
		powerDesignerDDLImport gt = new powerDesignerDDLImport( xMSet , dbmake , sDatabaseName , InputFileName , ConfigFileNameIn );
		return gt.getIsValid();
	}
	
	//----------------------------------------------------------------
	public boolean import_Oracle_External_Tables_DDL( String sDatabaseName , String InputFileName )
	//----------------------------------------------------------------
	{
		externalTablesORCLImport gt = new externalTablesORCLImport( xMSet , sDatabaseName , InputFileName );
		return true;
	}
	
	//----------------------------------------------------------------
	public boolean import_INFA_Source_Target( String InputFileName )
	//----------------------------------------------------------------
	{
		infaImport ip = new infaImport( xMSet , InputFileName );
		return true;
	}
	
	//----------------------------------------------------------------
	public boolean generate_INFA_Mapping(String TemplateName , String SourceNameList , String TargetNameList)
	//----------------------------------------------------------------
	{
		generateMapping gm = new generateMapping( xMSet , TemplateName , SourceNameList , TargetNameList );
		return gm.generate();
	}
	
	//----------------------------------------------------------------
	public boolean translateDDL( String DDLFileNameIn , String srcDB , String tgtDB , String ConfigFileNameIn )
	//----------------------------------------------------------------
	{
		ddlTranslator trans = new ddlTranslator( xMSet );
		return trans.translateDDLFromTo(DDLFileNameIn, srcDB, tgtDB , ConfigFileNameIn );
	}

	//----------------------------------------------------------------
	public boolean translateDDLtoSST( String DDLFileNameIn , String srcDB  )
	//----------------------------------------------------------------
	{
			ddlTranslator trans = new ddlTranslator( xMSet );
			return trans.translateDDLFromToNZSST(DDLFileNameIn, srcDB );
	}
	//----------------------------------------------------------------
	public boolean compareDDLscripts(String DDLScriptOne , String dbTipeOne , String remove1 ,String DDLScriptTwo , String dbTipeTwo , String remove2 )
	//----------------------------------------------------------------
	{
		ddlCompare dcom = new ddlCompare( xMSet );
		return dcom.compareDDLScripts(DDLScriptOne, dbTipeOne, remove1 , DDLScriptTwo, dbTipeTwo , remove2);
	}
	//----------------------------------------------------------------
	public boolean import_powerdesigner(String sLongXml , String sLayer , String sSrcSystemName , String options)
	//----------------------------------------------------------------
	{
		    pwdController pwd = new pwdController( xMSet );
		    return pwd.loadPowerDesignerXMLToDevBooster(sLongXml, sLayer, sSrcSystemName , options);
	}
	//----------------------------------------------------------------
	public boolean create_mapping( String sMapTipe , String sSrcSystemName , String[] args)
	//----------------------------------------------------------------
	{
		    pwdController pwd = new pwdController( xMSet );
 		    return pwd.generateMapInstuctions( sMapTipe , sSrcSystemName , args);
	}
	//----------------------------------------------------------------
	public boolean extract_excel(String sInputFile , String sOutputFile )
	//----------------------------------------------------------------
	{
	        xlsxToTxt  xls = new xlsxToTxt( xMSet);
		    return xls.extract( sInputFile , sOutputFile );
	}
	//----------------------------------------------------------------
	public boolean Oracle_Ext_TO_Excel(String sDLLLong  , String ExcelOut)
	//----------------------------------------------------------------
	{
	        pwdExtTblToExcel  ox = new pwdExtTblToExcel( xMSet);
		    return ox.make_excel( sDLLLong , ExcelOut);
	}
	//----------------------------------------------------------------
	public boolean do_mikel(String sInputFile , String sOutputFile )
	//----------------------------------------------------------------
	{
	        pwdMikelToDDL  mik = new pwdMikelToDDL( xMSet);
		    return mik.extract( sInputFile , sOutputFile );
	}
	//----------------------------------------------------------------
	public boolean do_makeDQ(String[] args )
	//----------------------------------------------------------------
	{
	      pwdController pwd = new pwdController( xMSet );
		  return pwd.prepare_dq_layer(args);
	}
	//----------------------------------------------------------------
	public boolean do_compare_excel(String[] args )
	//----------------------------------------------------------------
	{
		  xlsxCompare comp = new xlsxCompare(xMSet);
		  return comp.do_compare(args);	  
	}
	//----------------------------------------------------------------
	public boolean do_ddl_to_powerdesigner_excel(boolean doExcel , String[] args )
	//----------------------------------------------------------------
	{
			  ddl2PowerDesignerExcel comp = new ddl2PowerDesignerExcel(doExcel , xMSet);
			  return comp.do_import(args);
	}
	//----------------------------------------------------------------
	public boolean do_import_infa_extract(String[] args )
	//----------------------------------------------------------------
	{
		      if( args.length != 1 ) {
			    errit("Usage : IMP-INFA-SRCTGT <file>");
			    return false;
		      }
		      String FFullName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Import" + xMSet.xU.ctSlash + args[0].trim();
		      infaImport imp = new infaImport( xMSet , FFullName );
		      return imp.getCompletionStatus();
    }
	//----------------------------------------------------------------
	public boolean do_switcher(String[] args )
	//----------------------------------------------------------------
	{
			      if( args.length != 3 ) {
				    errit("Usage : nzviewtool <SrcFile=file> <TgtFile=file> <ParameterFile=file");
				    return false;
			      }
			      //String FFullName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Import" + xMSet.xU.ctSlash + args[0].trim();
			      nzViewParser vp = new nzViewParser( xMSet );
			      return vp.doit(args);
    }
	
}
