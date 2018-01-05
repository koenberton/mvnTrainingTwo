package powerdesigner;

import pcGenerator.ddl.externalTablesORCLImport;
import pcGenerator.ddl.powerDesignerDDLImport;
import generalpurpose.pcDevBoosterSettings;

public class pwdController {
	
	pcDevBoosterSettings xMSet=null;
	
	
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
	public pwdController(pcDevBoosterSettings iM)
	//----------------------------------------------------------------
	{
		xMSet = iM;
	}

	//----------------------------------------------------------------
	public boolean loadPowerDesignerXMLToDevBooster(String sLongXml , String sLayer , String sSrcSystemName , String options)
	//----------------------------------------------------------------
	{
		// Check OPTIONS
		String TgtRDBMS=null;
		if ( options != null ) {
			if( options.toUpperCase().startsWith("RDBMS=") ) {
		        TgtRDBMS = options.substring(("RDBMS=").length());	
			}
		}
	    // import the XML	
		readPwdXml pwd = new readPwdXml( xMSet);
		if( pwd.importXml( sLongXml , sLayer , sSrcSystemName ) == false ) {
			errit("loadPowerDesignerXMLToDevBooster will not continue loading metadata towards PowerCenter format");
			return false;
		}
		String FName = pwd.getDDLScriptName();
		//
		logit(9 , "Will now continue by importing the DDL script [" + FName + "]");
		// import the DDL in devbooster
		String sDatabaseName = ( sSrcSystemName + "_" + sLayer ).trim().toUpperCase();
		if( sLayer.compareToIgnoreCase("ORCLEXT") == 0 ) {
			externalTablesORCLImport gt = new externalTablesORCLImport( xMSet , sDatabaseName , FName );
			return true;
		}
		else
		if( sLayer.compareToIgnoreCase("SRC") == 0 ) {
			String RDBMSRequested = (TgtRDBMS == null ) ? "ORACLE" : TgtRDBMS;     // KB 16 DEC
			powerDesignerDDLImport gt = new powerDesignerDDLImport( xMSet , RDBMSRequested , sDatabaseName , FName , null );
			return gt.getIsValid();
		}
		else {   // DVST - SST - STOV
			powerDesignerDDLImport gt = new powerDesignerDDLImport( xMSet , "NETEZZA" , sDatabaseName , FName , null );
			return gt.getIsValid();
		}
	}
	
	//----------------------------------------------------------------
	public boolean generateMapInstuctions( String sLayer , String sSrcSystemName , String[] args)
	//----------------------------------------------------------------
	{
		      pwdCreateMapFile pwd = new pwdCreateMapFile( xMSet );
		      return pwd.create_map_file( sLayer , sSrcSystemName , args);
	}
	
	//----------------------------------------------------------------
	public boolean prepare_dq_layer( String[] args)
	//----------------------------------------------------------------
	{
		  pwdCreateDQLayer  mik = new pwdCreateDQLayer( xMSet);
		  if( mik.makeDQLayer( args ) == false ) return false;
		  //
		  logit(5,"Will now import the DDL [" + mik.getDDLName() + "]");
		  powerDesignerDDLImport gt = new powerDesignerDDLImport( xMSet , "NETEZZA" , mik.getDatabaseName() , mik.getDDLName() , null );
		  return gt.getIsValid();
	}
}
