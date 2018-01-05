package pcGenerator;
import generalpurpose.pcDevBoosterSettings;

import java.util.ArrayList;
import java.util.StringTokenizer;

import pcGenerator.ddl.externalTablesORCLImport;


public class devBoosterRobot {
	
	pcDevBoosterSettings xMSet = null;
	devBoosterController xCtrl=null;
	
	public devBoosterRobot(pcDevBoosterSettings xi)
	{
		xMSet = xi;
		xCtrl = new devBoosterController(xMSet);
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
	
	// steps through the tasklist file
	//----------------------------------------------------------------
	public boolean processTaskList(String FName)
	//----------------------------------------------------------------
	{
		if( xMSet.xU.IsBestand(FName) == false) {
			errit("Task List [" + FName + "] could not be found");
			return false;
		}
		
		String sTaskList = xMSet.xU.ReadContentFromFile( FName , 2000 );
		StringTokenizer st = new StringTokenizer(sTaskList, "\n");
		boolean ignore=false;
		while(st.hasMoreTokens()) 
		{ 
			  String sLine = st.nextToken().trim();
			  if( sLine.length() <= 0 ) continue;
			  if( sLine.startsWith("--") ) continue;
			  if( sLine.startsWith("//") ) continue;
			  //
			  StringTokenizer st2 = new StringTokenizer(sLine, " \"\t");
			  if( st2.hasMoreTokens() == false )  continue; 
			  String sCmd = st2.nextToken().trim().toUpperCase();
//System.err.println( "Command -> " + sCmd +  " " + sLine);
			  /*
			   * 
				* (COMP-DDL) Compare 2 DDLS and report on the differences	
				(CRE-PROJ) Create and initialize a project	
				* (GEN-MAP) Generate PowerCenter Mapping	
				* (IMP-DDL) Import a DDL from PowerDesigner and create PowerCenter  Sources, targets and Flat File objects
				(IMP-INFA-SRCTGT )Import  Sources and Targets from PowerCenter export files.
				* (IMP-ORCL-EXT) Import Oracle external table DDL and create PowerCenter Source and Target objects
				* (TRANS-DDL) Translate a DDL from one database technology to another	
				* (TRANS-DDL2SST) Translate a DDL to specific Netezza SST Table format
			   */
			  if( sCmd.compareToIgnoreCase("/*")==0) {
				   ignore = true;
				   continue;
			   }
			   if( ignore ) {
				   if( sCmd.compareToIgnoreCase("*/")==0) {
					   ignore = false;
					   continue;
				   }
				   continue;
			   }
	           //
			   if( sCmd.compareToIgnoreCase("VERSION")==0) {
				   xMSet.logit(1,"Project=" +xMSet.getCurrentProject() );
			   }
			   else
			   if( sCmd.compareToIgnoreCase("LOGLEVEL")==0) {
				   if( do_setting( sCmd , st2 ) == false ) return false;
			   }
			   else
			   if( sCmd.compareToIgnoreCase("EDWNULL")==0) {
				   if( do_setting( sCmd , st2 ) == false ) return false;
			   }
			   else
			   if( sCmd.compareToIgnoreCase("POWERMARTVERSION")==0) {
				   if( do_setting( sCmd , st2 ) == false ) return false;
			   }
			   else
			   if( sCmd.compareToIgnoreCase("GEN-MAP")==0) {
				   if( do_genmap( st2 ) == false ) return false;
			   }
			   else
			   if( sCmd.compareToIgnoreCase("COMP-DDL")==0) {
				   if( do_compare_ddl( st2 ) == false ) return false;
			   }
			   else
			   if( sCmd.compareToIgnoreCase("IMP-ORCL-EXT")==0) {
				   if( do_import_Oracle_External_Tables( st2 ) == false ) return false;
			   }
			   else
			   if( sCmd.compareToIgnoreCase("IMP-DDL")==0) {
					   if( do_import_ddl( st2 ) == false ) return false;
			   }
			   else
			   if( sCmd.compareToIgnoreCase("TRANS-DDL")==0) {
					   if( do_translate_ddl( st2 , false) == false ) return false;
			   }
			   else
			   if( sCmd.compareToIgnoreCase("TRANS-DDL2SST")==0) {
				   if( do_translate_ddl( st2 , true ) == false ) return false;
		       }
			   else
			   if( sCmd.compareToIgnoreCase("CLEAN-ALL")==0) {
				   if( do_clean() == false ) return false;
		       }
			   else
			   if( sCmd.compareToIgnoreCase("CHECK-ALL")==0) {
				       logit(5,"Checking ..");
					   return true;
			   }
			   else
			   if( sCmd.compareToIgnoreCase("IMP-PWD")==0) {
			       if( do_import_powerdesigner(st2) == false ) return false;
			   }
			   else 
			   if( sCmd.compareToIgnoreCase("CREATE-MAP")==0) {
				       if( do_create_map(st2) == false ) return false;
			   }	 
			   else 
			   if( sCmd.compareToIgnoreCase("XLSX2TXT")==0) {
					   if( do_extract_excel(st2) == false ) return false;
			   } 
			   else 
			   if( sCmd.compareToIgnoreCase("ORCLEXT2EXCEL")==0) {
				       if( do_oracle_ext_to_excel(st2) == false ) return false;
			   }
			   else 
			   if( sCmd.compareToIgnoreCase("MIKEL2DDL")==0) {
 			       if( do_mikel(st2) == false ) return false;
			   }
			   else 
			   if( sCmd.compareToIgnoreCase("PREPARE-DQ")==0) {
	 			    if( do_makeDQ(st2) == false ) return false;
			   }
			   else 
			   if( sCmd.compareToIgnoreCase("COMP-XCL")==0) {
		 			if( do_compare_excel(st2) == false ) return false;
			   }
			   else 
			   if( sCmd.compareToIgnoreCase("DDL2XCL")==0) {
			 		if( do_ddl_to_powerdesigner_excel(st2) == false ) return false;
			   }
			   else 
			   if( sCmd.compareToIgnoreCase("IMP-INFA-SRCTGT")==0) {
				 	if( do_import_infa_extract(st2) == false ) return false;
			   }
			   else 
			   if( sCmd.compareToIgnoreCase("MINUSONE")==0) {
					if( do_minusone(st2) == false ) return false;
			   }
			   else 
			   if( sCmd.compareToIgnoreCase("NZVIEWTOOL")==0) {
						if( do_switcher(st2) == false ) return false;
			   }
			   else {
				   errit("Unsupported command in tasklist [" + sCmd + "]");
				   return false;
			   }
		}		  
		return false;
	}
	
	//----------------------------------------------------------------
	private String noQuotes(String sIn)
	//----------------------------------------------------------------
	{
		return xMSet.xU.Remplaceer(sIn,"\"","");
	}
	
	//----------------------------------------------------------------
	private boolean do_setting(String sSubTipe , StringTokenizer st)
	//----------------------------------------------------------------
	{
		if( sSubTipe.compareToIgnoreCase("LOGLEVEL") == 0 ) {
			if ( st.hasMoreTokens() == false ) {
				errit("LOGLEVEL not specified");
				return false;
			}
			int ilevel = xMSet.xU.NaarInt(st.nextToken());
			if( (ilevel < 0) || (ilevel>9) ) {
				logit(5,"Invalid loglevel [" + ilevel + "]. Switching to level 5");
			    ilevel=5;	
			}
			xMSet.setLoglevel(ilevel);
			return true;
		}
		if( sSubTipe.compareToIgnoreCase("POWERMARTVERSION") == 0 ) {
			if ( st.hasMoreTokens() == false ) {
				errit("POWERMARTVERSION not specified");
				return false;
			}
			int ilevel = xMSet.xU.NaarInt(st.nextToken());
			if( (ilevel < 8) || (ilevel>9) ) {
				logit(5,"Invalid powermartversion [" + ilevel + "]. Switching to version 9");
			    ilevel=9;	
			}
			xMSet.setpowermartversion(ilevel);
			return true;
		}
		if( sSubTipe.compareToIgnoreCase("EDWNULL") == 0 ) {
			if ( st.hasMoreTokens() == false ) {
				errit("EDWNULL not specified");
				return false;
			}
			String sNull = st.nextToken();
			sNull = xMSet.xU.Remplaceer(sNull, "\"", "");
			sNull = xMSet.xU.Remplaceer(sNull, "'", "");
			xMSet.setEDWNULL(sNull);
			logit(5,"EDW NULL set to [" + xMSet.getEDWNULL() + "]");
			return true;
		}
		errit("Unknown switch [" + sSubTipe + "]");
		return false;
	}
	
	//----------------------------------------------------------------
	private boolean do_genmap( StringTokenizer st)
	//----------------------------------------------------------------
	{
		// <template> <sourcelist> <targetlist>
		if( st.hasMoreTokens() == false ) {
			errit("(GEN-MAP) template, source and targetlists not specified");
			return false;
		}
		String TemplateName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Templates" + xMSet.xU.ctSlash + st.nextToken();
		if( st.hasMoreTokens() == false ) {
			errit("(GEN-MAP) source list not specified");
			return false;
		}
		String SourceNameList = st.nextToken();
		if( st.hasMoreTokens() == false ) {
			errit("(GEN-MAP) Targetlist not specified");
			return false;
		}
		String TargetNameList = st.nextToken();
		return xCtrl.generate_INFA_Mapping(TemplateName, SourceNameList, TargetNameList);
	}
	
	//----------------------------------------------------------------
	private boolean do_compare_ddl( StringTokenizer st)
	//----------------------------------------------------------------
	{
			// <first ddl> DBMAKE <exclude instruction> <second ddl> DBMAKE <exclude instruction>
		    String ErrMsg = "(COMP-MAP) <First DDL script Name> <First DBType> <First Exclude List> <Second DDL Script Name> <Second DB Type> <Second Exclude List>";
			if( st.hasMoreTokens() == false ) {
				errit(ErrMsg);
				return false;
			}
			String FirstDDLScript = xMSet.getProjectDir() + xMSet.xU.ctSlash + "DDL" + xMSet.xU.ctSlash + st.nextToken();
			if( st.hasMoreTokens() == false ) {
				errit(ErrMsg);
				return false;
			}
			String sDB1 = st.nextToken();
			if( st.hasMoreTokens() == false ) {
				errit(ErrMsg);
				return false;
			}
			String sExclude1 = st.nextToken();
			if( st.hasMoreTokens() == false ) {
				errit(ErrMsg);
				return false;
			}
			String SecondDDLScript = xMSet.getProjectDir() + xMSet.xU.ctSlash + "DDL" + xMSet.xU.ctSlash + st.nextToken();
			if( st.hasMoreTokens() == false ) {
				errit(ErrMsg);
				return false;
			}
			String sDB2 = st.nextToken();
			if( st.hasMoreTokens() == false ) {
				errit(ErrMsg);
				return false;
			}
			String sExclude2 = st.nextToken();
			//
			return xCtrl.compareDDLscripts( FirstDDLScript , sDB1 , sExclude1 , SecondDDLScript, sDB2 , sExclude2);
	}
	
	//----------------------------------------------------------------
	private boolean do_import_Oracle_External_Tables( StringTokenizer st)
	//----------------------------------------------------------------
	{
			// Database Name DDL
			if( st.hasMoreTokens() == false ) {
				errit("(IMP-ORCL-EXT) <databasename> <DDL script>");
				return false;
			}
			String sDatabaseName = st.nextToken();
			if( st.hasMoreTokens() == false ) {
				errit("(IMP-ORCL-EXT) <databasename> <DDL script>");
				return false;
			}
			String OracleDDL = xMSet.getProjectDir() + xMSet.xU.ctSlash + "DDL" + xMSet.xU.ctSlash + st.nextToken();
			return xCtrl.import_Oracle_External_Tables_DDL( sDatabaseName , OracleDDL );
	}
	
	//----------------------------------------------------------------
	private boolean do_import_ddl( StringTokenizer st)
	//----------------------------------------------------------------
	{
				//  DBType DatabaseName Script
				if( st.hasMoreTokens() == false ) {
					errit("(IMP-DDL <type> <databasename> <script> {config}");
					return false;
				}
				String sDatabaseType = st.nextToken();
				if( st.hasMoreTokens() == false ) {
					errit("(IMP-DDL <type> <databasename> <script> {config}");
					return false;
				}
				String sDatabaseName = st.nextToken();
				if( st.hasMoreTokens() == false ) {
					errit("(IMP-DDL <type> <databasename> <script> {config}");
					return false;
				}
				String sDDL = xMSet.getProjectDir() + xMSet.xU.ctSlash + "DDL" + xMSet.xU.ctSlash + st.nextToken();
				// there might be a configfile
				String FConfigFileName = null;
				if( st.hasMoreTokens() ) {
					FConfigFileName =  xMSet.getProjectDir() + xMSet.xU.ctSlash + "DDL" + xMSet.xU.ctSlash + st.nextToken().trim();
				}
				return xCtrl.import_Generic_DDL( sDatabaseType , sDatabaseName , sDDL , FConfigFileName);
	}
	
	//----------------------------------------------------------------
	private boolean do_translate_ddl( StringTokenizer st , boolean sst)
	//----------------------------------------------------------------
	{
			//  DBType DatabaseName Script
			if( st.hasMoreTokens() == false ) {
				errit("(IMP-DDL <script> <from databasetype> <todatabasetype> {config}");
				return false;
			}
			String sDDL = xMSet.getProjectDir() + xMSet.xU.ctSlash + "DDL" + xMSet.xU.ctSlash + st.nextToken();
			if( st.hasMoreTokens() == false ) {
				errit("(IMP-DDL <script> <from databasetype> <todatabasetype> {config}");
				return false;
			}
			String sFromDatabaseType = st.nextToken();
		    if( sst ) {
				return xCtrl.translateDDLtoSST(sDDL , sFromDatabaseType );
		    }
		    else {
		    	if( st.hasMoreTokens() == false ) {
		    		errit("(IMP-DDL <script> <from databasetype> <to databasetype> {config}");
		    		return false;
		    	}
				String sToDatabaseType = st.nextToken();
				// there might be a configfile
				String FConfigFileName = null;
				if( st.hasMoreTokens() ) {
					FConfigFileName =  xMSet.getProjectDir() + xMSet.xU.ctSlash + "DDL" + xMSet.xU.ctSlash + st.nextToken().trim();
				}
				return xCtrl.translateDDL(sDDL , sFromDatabaseType , sToDatabaseType , FConfigFileName );
	        }
	}
	
	
	//----------------------------------------------------------------
	private boolean do_import_powerdesigner( StringTokenizer st)
	//----------------------------------------------------------------
	{
					//  PowerDesign XML
					if( st.hasMoreTokens() == false ) {
						errit("(IMP-PWD {FLATFILE,SST,DVST} <SourceSystem> <powerdesinger.xml> {RDBMS=}");
						return false;
					}
					String sLayer = st.nextToken().trim().toUpperCase();
					if( st.hasMoreTokens() == false ) {
						errit("(IMP-PWD {FLATFILE,SST,DVST} <script>");
						return false;
					}
					String sSrcSystemName = st.nextToken().trim().toUpperCase();
					if( st.hasMoreTokens() == false ) {
						errit("(IMP-PWD {FLATFILE,SST,DVST} <SourceSystem> <powerdesinger.xml> {RDBMS=}");
						return false;
					}
					String sXML = st.nextToken();
					String sLongXML = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Import" + xMSet.xU.ctSlash + sXML;
					String options=null;
					if( st.hasMoreTokens() == true ) {
						options =  st.nextToken();
						boolean isOK=true;
						if (options == null ) options = "";
						options = options.trim();
						if( options.toUpperCase().startsWith("RDBMS=") == false ) {
							errit("(IMP-PWD {FLATFILE,SST,DVST} <SourceSystem> <powerdesinger.xml> {RDBMS=}");
							return false;
						}
					}
					return xCtrl.import_powerdesigner( sLongXML , sLayer , sSrcSystemName , options);
	}
	
	//----------------------------------------------------------------
	private boolean do_clean()
	//----------------------------------------------------------------
	{
		devBoosterClean cc = new devBoosterClean(xMSet);
		return cc.CleanIt();
	}
	
	//----------------------------------------------------------------
	private boolean do_create_map( StringTokenizer st)
	//----------------------------------------------------------------
	{
						//  create MAP file
						if( st.hasMoreTokens() == false ) {
							errit("CREATE-MAP <SRC2SST,SST2DVST> <sourcesystem> SCOPE=? SRC=FLATFILE/DIRECT (targettablelist)");
							return false;
						}
						String sMapTipe = st.nextToken().trim().toUpperCase();
						if( st.hasMoreTokens() == false ) {
							errit("CREATE-MAP <SRC2SST,SST2DVST> <sourcesystem> SCOPE=? SRC=FLATFILE/DIRECT (targettablelist)");
							return false;
						}
						String sSrcSystemName = st.nextToken().trim().toUpperCase();
						if( st.hasMoreTokens() == false ) {
							errit("CREATE-MAP <SRC2SST,SST2DVST> <sourcesystem> SCOPE=? SRC=FLATFILE/DIRECT (targettablelist)");
							return false;
						}
						ArrayList<String> list = new ArrayList<String>();
						while( st.hasMoreTokens() )
						{
							String ss = st.nextToken().trim();
							list.add( ss );
						}
						String[] args = new String[ list.size() ];
						for(int i=0;i<list.size();i++ )
						{
							args[i] = list.get(i);
						}
						return xCtrl.create_mapping( sMapTipe  , sSrcSystemName , args );
	}
	
	//----------------------------------------------------------------
	private boolean do_extract_excel( StringTokenizer st)
	//----------------------------------------------------------------
	{
							//  extract XLS
							String sDir = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Import" + xMSet.xU.ctSlash;
							if( st.hasMoreTokens() == false ) {
								errit("(XLSX2TXT <inputfile.xlsx> <outputfile.xml> - missing input and output");
								return false;
							}
							String sInputFile = sDir + st.nextToken().trim();
							if( st.hasMoreTokens() == false ) {
								errit("(XLSX2TXT <inputfile.xlsx> <outputfile.xml> - missing output");
								return false;
							}
							String sOutputFile = sDir + st.nextToken().trim();
							
							return xCtrl.extract_excel( sInputFile , sOutputFile );
	}
	
	//----------------------------------------------------------------
	private boolean do_oracle_ext_to_excel( StringTokenizer st)
	//----------------------------------------------------------------
	{				
		        // Database Name DDL
				if( st.hasMoreTokens() == false ) {
					errit("(ORCLEXTToEXCEL) <DDL script> <Excel>");
					return false;
				}
				String OracleDDL = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Import" + xMSet.xU.ctSlash + st.nextToken();
				if( st.hasMoreTokens() == false ) {
					errit("(ORCLEXTToEXCEL) <DDL script> <Excel>");
					return false;
				}
				String ExcelLong = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Temp" + xMSet.xU.ctSlash + st.nextToken();
				return xCtrl.Oracle_Ext_TO_Excel( OracleDDL , ExcelLong);
	}
	
	//----------------------------------------------------------------
	private boolean do_mikel( StringTokenizer st)
	//----------------------------------------------------------------
	{
								//  extract XLS
								String sDir = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Import" + xMSet.xU.ctSlash;
								if( st.hasMoreTokens() == false ) {
									errit("(MIKEL2DDLT <inputfile.xlsx> <outputfile.xml> - missing input and output");
									return false;
								}
								String sInputFile = sDir + st.nextToken().trim();
								if( st.hasMoreTokens() == false ) {
									errit("(MIKEL2DDL <inputfile.xlsx> <outputfile.xml> - missing output");
									return false;
								}
								String sOutputFile = sDir + st.nextToken().trim();
								
								return xCtrl.do_mikel( sInputFile , sOutputFile );
	}
	
	
	//----------------------------------------------------------------
	private boolean do_makeDQ( StringTokenizer st)
	//----------------------------------------------------------------
	{
									ArrayList<String> list = new ArrayList<String>();
									while( st.hasMoreTokens() )
									{
										String ss = st.nextToken().trim();
										list.add( ss );
									}
									String[] args = new String[ list.size() ];
									for(int i=0;i<list.size();i++ )
									{
										args[i] = list.get(i);
									}
									return xCtrl.do_makeDQ( args );
	}
	
	//----------------------------------------------------------------
	private boolean do_compare_excel( StringTokenizer st)
	//----------------------------------------------------------------
	{
		ArrayList<String> list = new ArrayList<String>();
		while( st.hasMoreTokens() )
		{
			String ss = st.nextToken().trim();
			list.add( ss );
		}
		String[] args = new String[ list.size() ];
		for(int i=0;i<list.size();i++ )
		{
			args[i] = list.get(i);
		}
		return xCtrl.do_compare_excel(args);
	}
	
	//----------------------------------------------------------------
	private boolean do_ddl_to_powerdesigner_excel( StringTokenizer st)
	//----------------------------------------------------------------
	{
			ArrayList<String> list = new ArrayList<String>();
			while( st.hasMoreTokens() )
			{
				String ss = st.nextToken().trim();
				list.add( ss );
			}
			String[] args = new String[ list.size() ];
			for(int i=0;i<list.size();i++ )
			{
				args[i] = list.get(i);
			}
			return xCtrl.do_ddl_to_powerdesigner_excel(true,args);
	}
	
	//----------------------------------------------------------------
	private boolean do_import_infa_extract( StringTokenizer st)
	//----------------------------------------------------------------
	{
				ArrayList<String> list = new ArrayList<String>();
				while( st.hasMoreTokens() )
				{
					String ss = st.nextToken().trim();
					list.add( ss );
				}
				String[] args = new String[ list.size() ];
				for(int i=0;i<list.size();i++ )
				{
					args[i] = list.get(i);
				}
				return xCtrl.do_import_infa_extract(args);
	}
	
	//----------------------------------------------------------------
	private boolean do_minusone( StringTokenizer st)
	//----------------------------------------------------------------
	{
				ArrayList<String> list = new ArrayList<String>();
				while( st.hasMoreTokens() )
				{
					String ss = st.nextToken().trim();
					list.add( ss );
				}
				String[] args = new String[ list.size() ];
				for(int i=0;i<list.size();i++ )
				{
					args[i] = list.get(i);
				}
				return xCtrl.do_ddl_to_powerdesigner_excel(false,args);
		}
	
	//----------------------------------------------------------------
	private boolean do_switcher( StringTokenizer st)
	//----------------------------------------------------------------
	{
					ArrayList<String> list = new ArrayList<String>();
					while( st.hasMoreTokens() )
					{
						String ss = st.nextToken().trim();
						list.add( ss );
					}
					String[] args = new String[ list.size() ];
					for(int i=0;i<list.size();i++ )
					{
						args[i] = list.get(i);
					}
					return xCtrl.do_switcher(args);
	}
}
