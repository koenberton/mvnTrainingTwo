package pcGenerator.ddl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import office.xlsxWriterMark2;
import pcGenerator.generator.generatorConstants;
import pcGenerator.powercenter.infaConstraint;
import pcGenerator.powercenter.infaSource;
import pcGenerator.powercenter.infaSourceField;
import generalpurpose.gpPrintStream;
import generalpurpose.pcDevBoosterSettings;

public class ddl2PowerDesignerExcel {
	
	pcDevBoosterSettings xMSet = null;

	private boolean MINUSONE = false;
	private gpPrintStream mino = null;
	
	String dbname = null;
	rdbmsDatatype.DBMAKE rdbmstipe = null;
	String FConsolidatedScript=null;
	String Strip = null;
	xlsxWriterMark2 xcw = null;
	ArrayList<String> filefilter=null;
	boolean positioneel = false;
	private boolean mimicStefan=false;
	
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
	public ddl2PowerDesignerExcel(boolean doExcel , pcDevBoosterSettings im)
	//----------------------------------------------------------------
	{
		xMSet = im;
		MINUSONE = !doExcel;
	}

	//----------------------------------------------------------------
	private void usage()
	//----------------------------------------------------------------
	{
		errit("DDL2XLS <databasename> <databasetype> {FILTER=(aa,bb)} OPTION={BESTEFAN}");
	}
	
	//----------------------------------------------------------------
	public boolean do_import(String[] args)
	//----------------------------------------------------------------
	{
		    boolean isOK=true;
		    for(int i=0;i<args.length;i++)
		    {
		    	String sElem=args[i].trim();
		    	if(i==0) {
		    	   dbname = sElem;
		    	   continue;
		    	}
		        //
		    	if(i==1) {
		    		 rdbmsDatatype dbt = new rdbmsDatatype(xMSet);
		    		 rdbmstipe = dbt.getDBMAKEType( sElem);
		    		 if( rdbmstipe == null ) {
		    			 errit("Unsupported RDBMS type [" + sElem + "]");
		    			 isOK=false;
		    		 }
		    		 continue;
		    	}
		    	//
		    	if( sElem.toUpperCase().startsWith("FILTER=") ) {
		    		if( getFilters(sElem) == false ) {
		    			errit("Error extracting script name filter");
		    			isOK = false;
		    		}
		    	}
		    	else
	    		if( sElem.toUpperCase().startsWith("STRIP=") ) {
	    			try {
                      Strip = sElem.substring("STRIP=".length() , sElem.length()).trim();
                      if( Strip.length() < 1 ) Strip = null;
                      logit(5,"Table prefix to strip [" + Strip + "]");
	    			}
	    			catch(Exception e) {
	    				errit("Cannot extract prefix to strip [" + sElem + "]");
	    				isOK=false;
	    			}
		    	}	
	    		else
		    	if( sElem.toUpperCase().startsWith("POSITIONAL=") ) {
		    		String sPos = xMSet.xU.removeBelowIncludingSpaces(sElem);
		    		sPos = sPos.trim().toUpperCase();
		    		if( sPos.indexOf("=ON") >=  0 ) positioneel = true;
		    		if( sPos.indexOf("=TRUE") >=  0 ) positioneel = true;
		    		if( sPos.indexOf("=YES") >=  0 ) positioneel = true;
		    		if( sPos.indexOf("=1") >=  0 ) positioneel = true;
		    	    logit(5,"Postional switch [" + positioneel + "]");	
		    	}	
		    	else
	    		if( sElem.toUpperCase().startsWith("OPTION=MIMICSTEFAN") ) {
		    		mimicStefan=true;
		    	}	
		    	else {
		    		errit("Unsupported command line option [" + sElem + "]");
		    		isOK=false;
		    	}
		    }
		    if( (dbname == null) || (rdbmstipe==null) ) isOK = false;
		    if( isOK == false ) {
		    	usage();
		    	return false;
		    }
		    // combine all DDL / SQL files
		    if( consolidate_scripts() == false ) return false;
		    if( xMSet.xU.IsBestand(FConsolidatedScript) == false ) {
		    	errit("Cannot find consolidated script [" + FConsolidatedScript + "]");
		    }
		    logit( 5, "Importing [" + FConsolidatedScript + "] for databasetype [" + rdbmstipe + "]" );
		    //
		    db2Tokenize t1 = new db2Tokenize( xMSet , dbname , rdbmstipe , null);
		    ArrayList<infaSource> list = t1.parseFile( FConsolidatedScript );
		    if ( list == null) {
		    	errit("Error parsing [" + FConsolidatedScript + "]");
		    	return false;
		    }
		    if( MINUSONE ) {
		   	  if( generateMinusOne( list ) == false ) return false;
		    }
		    else {
		     if( dumpToPowerDesignerExcel( list ) == false ) return false;
		    }
			return true;
	}
		
	//----------------------------------------------------------------
	private boolean getFilters(String sIn)
	//----------------------------------------------------------------
	{
		try {
		   if( sIn.length() <= "FILTER=".length() ) return false;
		   String sTemp = sIn.substring( "FILTER".length() + 1 , sIn.length() ).trim();
		   if( (sTemp.startsWith("(") == false ) || (sTemp.endsWith(")") == false) ) {
			 errit("No enclosing round bracket on the source script list [" + sIn + "]");
			 return false;
		   }
		   sTemp = sTemp.substring(1,sTemp.length()-1);
		   if( sTemp.length() < 1) {
			 errit("Nothing to extract in source script list [" + sIn + "]");
			 return false;
		    }
		    //
		    StringTokenizer st = new StringTokenizer( sTemp, ",");
			while(st.hasMoreTokens()) 
			{ 
				  String sLine = st.nextToken().trim();
				  sLine = xMSet.xU.Remplaceer(sLine,"\"","");
				  if( sLine.length() < 1) continue;
				  if( filefilter == null ) filefilter = new ArrayList<String>();
				  filefilter.add( sLine );
			}
			if( filefilter != null )  logit(5,"FileFilter " + filefilter );
		    return true;
		}
		catch(Exception e ) {
			errit("Cannot extract source filters from [" + sIn + "] " + e.getMessage());
			return false;
		}
	}
	
	//----------------------------------------------------------------
	private boolean consolidate_scripts()
	//----------------------------------------------------------------
	{
		int nbr=0;
		gpPrintStream co = null;
		
		String sDir = xMSet.getProjectDir() + xMSet.xU.ctSlash + "DDL";
		ArrayList<String> list = xMSet.xU.GetFilesInDir(sDir, null);
		
		for(int i=0;i<list.size();i++)
		{
			String Fin = sDir + xMSet.xU.ctSlash + list.get(i).trim();
			if( (Fin.toUpperCase().endsWith(".DDL") == false) && (Fin.toUpperCase().endsWith(".SQL") == false)  ) continue;
			//  filter
			if( filefilter != null ) {
				boolean keep=false;
				for(int k=0;k<filefilter.size();k++)
				{
					if( list.get(i).trim().toUpperCase().startsWith( filefilter.get(k).trim().toUpperCase()) ) {
						keep=true;
						break;
					}
				}
				if( keep == false ) {
					logit(1,"Skipping [" + Fin + "] no match with filter");
					continue;
				}
			}
			//
			nbr++;
			if( nbr == 1 ) {
				FConsolidatedScript = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Temp" + xMSet.xU.ctSlash + dbname.trim() + "_cons_ddl.txt";
				if (xMSet.xU.IsBestand(FConsolidatedScript)) {
				    if( xMSet.xU.VerwijderBestand(FConsolidatedScript) == false ) {
				    	errit("Cannot remove script [" + FConsolidatedScript + "]");
				    	return false;
				    }
				}
				co = new gpPrintStream( FConsolidatedScript , "UTF-8" );
				co.do_standard_header(xMSet.getApplicationId());
				co.println("-- Consolidated script of " + list );
				co.println("--");
			}
			co.println("-- Dump of the contents of [" + Fin + "]");
			co.println("--");
			try {
				    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(Fin),"UTF-8"));
			        String sLijn = null;
			        while ((sLijn=reader.readLine()) != null) {
			          co.println(sLijn); 
				    } 	   
			        reader.close();
			}
			catch(Exception e) {
				  errit( "Error reading file [" + Fin + "]");
				  return false;
			}
		}
		if( nbr == 0 ) {
			errit("No files ending on DDL or SQL found in [" + sDir + "]");
			return false;
		}
		if( co != null ) {
			co.close();
		}
        //		
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean writeExcelLine( String TabSheetName , String[] sLineIn )
	//---------------------------------------------------------------------------------
	{
			Object[] oi = new Object[sLineIn.length];
			for(int k=0;k<sLineIn.length;k++) oi[k]=sLineIn[k];
			return xcw.addRow( TabSheetName , oi);
	}
			
	//---------------------------------------------------------------------------------
	private boolean writeExcelLine( String TabSheetName , String[] sLineIn , xlsxWriterMark2.XCOLOR[] aColorIn)
	//---------------------------------------------------------------------------------
	{
			Object[] oi = new Object[sLineIn.length];
			Object[] cc = new Object[sLineIn.length];
			for(int k=0;k<sLineIn.length;k++) {
						oi[k]=sLineIn[k];
						cc[k]=aColorIn[k];
			}
			return xcw.addRow( TabSheetName , oi , cc );
	}

	//---------------------------------------------------------------------------------
	private boolean dumpToPowerDesignerExcel(ArrayList<infaSource> ddllist )
	//---------------------------------------------------------------------------------
	{
		String FExcelName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Export" + xMSet.xU.ctSlash + "Pwd_" + dbname.trim().toLowerCase() + ".xlsx";
		if( xMSet.xU.IsBestand( FExcelName ) ) {
			if( xMSet.xU.VerwijderBestand( FExcelName ) == false ) {
				errit("Cannot remove Excel [" + FExcelName + "] - It is probably in use");
				return false;
			}
		}
		//
		logit( 5 , "Dumping to Excel [" + FExcelName + "]" );
		xcw = new xlsxWriterMark2( xMSet);
		//
		if( dumpTableSheet(ddllist) == false ) return false;
		//
		if( dumpColumnSheet(ddllist) == false ) return false;
		//
		if( dumpReferenceSheet(ddllist) == false ) return false;
		//
		if( dumpReferenceJoinSheet(ddllist) == false ) return false;
		//
		if( dumpTableKeySheet(ddllist) == false ) return false;
		//
		if( dumpReference2Sheet(ddllist) == false ) return false;
		//
		if( xcw.dumpToExcel(FExcelName) == false ) return false;
		//
		return true;
	}
	

	//---------------------------------------------------------------------------------
	private boolean maakHeader(String SheetName , String sHeader)
	//---------------------------------------------------------------------------------
	{
		StringTokenizer st = new StringTokenizer(sHeader,"|");
		int nbr = st.countTokens();
		String lijn[] = new String[nbr];
		int i=-1;
		while(st.hasMoreTokens()) 
		{ 
			i++;
			lijn[i] = st.nextToken().trim();
		}	
		// kleur
		xlsxWriterMark2.XCOLOR[] aColor = new xlsxWriterMark2.XCOLOR[nbr];
		for(int k=0;k<aColor.length;k++) aColor[k] = xlsxWriterMark2.XCOLOR.BLUE;
		//
		if( writeExcelLine( SheetName , lijn , aColor) == false ) return false;
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private String stripper(String sin)
	//---------------------------------------------------------------------------------
	{
		if( Strip == null ) return sin;
		if( sin.toUpperCase().startsWith( Strip.toUpperCase() ) == false ) return sin;
		//
		try {
		  String stemp = sin.substring( Strip.length() );
		  return stemp;
		}
		catch(Exception e) { return sin; }
	}
	
	//---------------------------------------------------------------------------------
	private boolean dumpTableSheet(ArrayList<infaSource> ddllist)
	//---------------------------------------------------------------------------------
	{
		String SheetName = "Table";
		xcw.addSheet(SheetName);
		// header
		String header = "Owner|Name|Code|Comment|EntityL0|EntityL1|IsHub|IsLink|HasReference|IsReferenced|IsSattelite|IsReference|SourceFileDelimiter|SourceFileName|SourceTableName|CR|CR_Hist";
	    if( maakHeader( SheetName , header) == false ) return false;	
		//
		for(int i=0;i<ddllist.size();i++)
		{
		  infaSource tab = ddllist.get(i);
		  String lijn[] = new String[17];
		  lijn[0]="";
		  lijn[1]=stripper(tab.Name);
		  lijn[2]=stripper(tab.Name);
		  lijn[3]="";
		  lijn[4]=tab.BusinessName;
		  lijn[5]=tab.BusinessName;
		  lijn[6]="FALSE";
		  lijn[7]="FALSE";
		  lijn[8]="FALSE";
		  lijn[9]="FALSE";
		  lijn[10]="";
		  lijn[11]="";
		  lijn[12]="";
		  lijn[13]=stripper(tab.Name);
		  lijn[14]=stripper(tab.Name);
		  lijn[15]="";
		  lijn[16]="";
		  //
		  if( writeExcelLine( SheetName , lijn) == false ) return false;
		}
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean dumpColumnSheet(ArrayList<infaSource> ddllist)
	//---------------------------------------------------------------------------------
	{
		String SheetName = "Table.Column";
		xcw.addSheet(SheetName);
		// header
		String header = "Table|Name|DataType|Code|Comment|Length|Precision|Primary|Mandatory|Column Sequence|BusinessLongDesc|BusinessShortDesc|BusinessLongDescVCE|BusinessShortDescVCE|BusinessRule|ComposedKeyDefinition|RefTable|NewColumnIndicator_SST|NewColumnIndicator_DVST|VarcharConvertedDataType|SourceName|SourceCode|SourceDataType|SourceMandatory|EDW_CheckSum|CR|CR_Hist";
		if( maakHeader( SheetName , header) == false ) return false;	
		//
		for(int i=0;i<ddllist.size();i++)
		{
		  infaSource tab = ddllist.get(i);
		  for(int j=0;j<tab.fieldList.size();j++)
		  {
			  infaSourceField fld = tab.fieldList.get(j);
			  String lijn[] = new String[25];
			  for(int z=0;z<lijn.length;z++) lijn[z]=null;
			  String NaturalKeyList = null;
			  // sigh ..
			  // Powerdesigner Len = precision MINUS the scale  and   PowerDesigner precision = scale
			  int pwdLen = fld.Precision;
			  int pwdPrec = fld.scale;
			  String sLen  = null;
			  String sPrec = null;
			  String sDataType = fld.DataType;
			  if( pwdLen != -1 ) {
				  if( pwdPrec > 0 ) {
					  sPrec = "" + pwdPrec;
					  pwdLen = pwdLen - pwdPrec;
					  sDataType = fld.DataType + "(" + pwdLen + "," + pwdPrec + ")";
				  }
				  else {
					  sDataType = fld.DataType + "(" + pwdLen + ")";
				  }
				  sLen = ""+pwdLen;
			  }
			  // PK
			  boolean isPK = isPartOfPrimaryKey( fld.Name , tab);
			  if( isPK ) {
				  NaturalKeyList = getNaturalKeyList( tab );
			  }
			  // FK
			  boolean isFK = isForeignKeyColumn( tab ,  fld );
			  //
			  lijn[0]  = stripper(tab.Name);
			  lijn[1]  = fld.Name;
			  lijn[2]  = sDataType;
			  lijn[3]  = fld.Name;  
			  lijn[4]  = (NaturalKeyList != null) ? "Composite BK Column : (" + NaturalKeyList + ")" : null;
			  lijn[5]  = sLen;
			  lijn[6]  = sPrec;
			  lijn[7]  = isPK == true ? "TRUE" : null;
			  lijn[8]  = (fld.mandatory == true ) ? "TRUE" : null;
			  lijn[9]  = ""+(j+1);
			  lijn[10] = null; 
			  lijn[11] = null; 
			  lijn[12] = fld.Description; 
			  lijn[13] = null; 
			  lijn[14] = null; 
			  lijn[15] = (NaturalKeyList != null) ? "BK(" + NaturalKeyList + ")" : null;
			  if( isFK ) {
				  lijn[15] = "FK(" + getForeignKeyColumnList( ddllist , tab ,  fld) + ")";
				  lijn[16] = "REF(" + getForeignKeyReferencedTable( tab ,  fld) + ")";
			  }
			  
			  //  use a DDL to create an export
			  if( mimicStefan ) {
				  String vc = "NVARCHAR(" + xMSet.NetezzaSSTSizeNEW(pwdLen) + ")";
				  if( vc.compareToIgnoreCase("NVARCHAR(1)")==0) vc = "CHAR(1)";
				  if( sDataType.toUpperCase().indexOf("CHAR") >=0 ) {
				   lijn[2]  = vc;
				  }  
			   if( fld.Name.trim().toUpperCase().endsWith("_KEY") ) {
				   lijn[17]="TRUE";
				   lijn[18] = "TRUE"  ;
			   }
			   lijn[19] = vc;
			   lijn[20] = fld.Name;
			   lijn[21] = fld.Name;
			   lijn[22] = sDataType;
			   lijn[23]  = (fld.mandatory == true ) ? "TRUE" : null;
			   if( fld.Name.trim().toUpperCase().startsWith("EDW") == false ) {
				   if( fld.Name.trim().toUpperCase().endsWith("_KEY") == false ) lijn[24]="TRUE";
			   }
			  }
			  //
			  
			  if( writeExcelLine( SheetName , lijn) == false ) return false;
			  
		  }
		}  
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean dumpKey( String SheetName , infaSource tab , boolean tableref)
	//---------------------------------------------------------------------------------
	{
		   if( tab.constraintList == null ) return true;
		   ArrayList<String> duplist = new ArrayList<String>();
		   for(int k=0;k<tab.constraintList.size();k++)
		   {
			   infaConstraint con = tab.constraintList.get(k);
			   if( con.Tipe != generatorConstants.CONSTRAINT.FOREIGN ) continue;
			   // single cols
			   if( con.key_list.size() != 1 ) continue;
			   if( con.ref_list.size() != 1 ) continue;
			   // duplicate
			   boolean keep=true;
			   for(int z=0;z<duplist.size();z++)
			   {
				   if( con.ReferencedTableName.trim().compareToIgnoreCase(duplist.get(z)) == 0) {
					   keep=false;
					   break;
				   }
			   }
			   if( keep == false ) continue;
			   duplist.add( con.ReferencedTableName );
			   //
			   String lijn[] = new String[3];
			   if( tableref ) {
			    lijn[0] = stripper(con.Name);
			    lijn[1] = tab.Name;
			    lijn[2] = con.ReferencedTableName;
			   }
			   else  {
			    lijn[0] = stripper(con.Name);
			    lijn[1] = con.key_list.get(0);
			    lijn[2] = con.ref_list.get(0);
			   }
			   //
			   if( writeExcelLine( SheetName , lijn) == false ) return false;
		   }
		   return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean dumpReferenceSheet(ArrayList<infaSource> ddllist)
	//---------------------------------------------------------------------------------
	{
		String SheetName = "Reference";
		xcw.addSheet(SheetName);
		// header
		String header = "Name|Parent Table|Child Table";
		if( maakHeader( SheetName , header) == false ) return false;	
		// ForeignKeyName - Parent table - ref table
		for(int i=0;i<ddllist.size();i++)
		{
		   if( dumpKey( SheetName , ddllist.get(i) , true) == false ) return false;
		}
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean dumpReferenceJoinSheet(ArrayList<infaSource> ddllist)
	//---------------------------------------------------------------------------------
	{
		String SheetName = "Reference.Reference Join";
		xcw.addSheet(SheetName);
		// header
		String header = "Parent|Parent Table Column|Child Table Column";
		if( maakHeader( SheetName , header) == false ) return false;	
		//
		// ForeignKeyName - parent col - child col
		for(int i=0;i<ddllist.size();i++)
		{
		   if( dumpKey( SheetName , ddllist.get(i) , false) == false ) return false;
		}
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean dumpTableKeySheet(ArrayList<infaSource> ddllist)
	//---------------------------------------------------------------------------------
	{
		String SheetName = "Table.Key";
		xcw.addSheet(SheetName);
		// header
		String header = "Table|Name|Primary|Columns";
		if( maakHeader( SheetName , header) == false ) return false;	
		//
		for(int i=0;i<ddllist.size();i++)
		{
		  infaSource tab = ddllist.get(i);
		  if( tab.constraintList == null ) return true;
		  for(int k=0;k<tab.constraintList.size();k++)
		  {
			   infaConstraint con = tab.constraintList.get(k);
			   if( con.Tipe == generatorConstants.CONSTRAINT.FOREIGN ) continue;
			   for(int z=0;z<con.key_list.size();z++)
			   {
				   String lijn[] = new String[4];
				   lijn[0] = stripper(tab.Name);
				   lijn[1] = con.Name;
				   lijn[2] = con.Tipe == generatorConstants.CONSTRAINT.PRIMARY ? "TRUE" : "FALSE";
				   lijn[3] = stripper(tab.Name) + "." + con.key_list.get(z); 
				   //
				   if( writeExcelLine( SheetName , lijn) == false ) return false;
			   }
		  }
		}
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean dumpReference2Sheet(ArrayList<infaSource> ddllist)
	//---------------------------------------------------------------------------------
	{
		String SheetName = "Reference2";
		xcw.addSheet(SheetName);
		// header
		String header = "Name|Parent Table|Child Table|Parent Key";
		if( maakHeader( SheetName , header) == false ) return false;	
		//
		// ForeignKeyName - Parent table - ref table - iets
		// identical to reference
		for(int i=0;i<ddllist.size();i++)
		{
		   if( dumpKey( SheetName , ddllist.get(i) , true) == false ) return false;
		}
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean isPartOfPrimaryKey( String fldname , infaSource tab)
	//---------------------------------------------------------------------------------
	{
		if( tab.constraintList == null ) return false;
		if( tab.constraintList.size() < 1 ) return false;
		for(int i=0;i<tab.constraintList.size();i++)
		{
			infaConstraint con = tab.constraintList.get(i);
			if( con.Tipe !=  generatorConstants.CONSTRAINT.PRIMARY ) continue;
			for(int k=0;k<con.key_list.size();k++)
			{
				if( fldname.trim().compareToIgnoreCase( con.key_list.get(k).trim() ) == 0 ) return true;
			}
		}
		return false;
	}
	
	//---------------------------------------------------------------------------------
	private String getNaturalKeyList( infaSource tab)
	//---------------------------------------------------------------------------------
	{
			if( tab.constraintList == null ) return null;
			if( tab.constraintList.size() < 1 ) return null;  
			for(int i=0;i<tab.constraintList.size();i++)
			{
				infaConstraint con = tab.constraintList.get(i);
				if( con.Tipe !=  generatorConstants.CONSTRAINT.UNIQUE ) continue;
				String sRet="";
				for(int k=0;k<con.key_list.size();k++)
				{
					if( k>0 ) sRet += ";";   // KB 24 nOV 2016 changed , to ;
					sRet += con.key_list.get(k).trim().toUpperCase();
				}
				return sRet;
			}
			return null;
	}
	
	//---------------------------------------------------------------------------------
	private int getForeignKeyIndex( infaSource tab ,  infaSourceField fld )
	//---------------------------------------------------------------------------------
	{
		if( tab.constraintList == null ) return -1;
		if( isPartOfPrimaryKey( fld.Name , tab) == true ) return -1;  // this is the PK
		String sCol = fld.Name.trim().toUpperCase();
		for(int i=0;i<tab.constraintList.size();i++)
		{
			infaConstraint con = tab.constraintList.get(i);
			if( con.Tipe !=  generatorConstants.CONSTRAINT.FOREIGN ) continue;
			for(int k=0;k<con.key_list.size();k++)
			{
				if( con.key_list.get(k).trim().toUpperCase().compareToIgnoreCase(sCol) == 0) return i;
			}
		}
		return -1;
	}
	
	//---------------------------------------------------------------------------------
	private boolean isForeignKeyColumn( infaSource tab ,  infaSourceField fld )
	//---------------------------------------------------------------------------------
	{
		if( getForeignKeyIndex(tab,fld) < 0 ) return false; 
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private String getForeignKeyColumnList( ArrayList<infaSource> ddllist , infaSource tab ,  infaSourceField fld )
	//---------------------------------------------------------------------------------
	{
		int idx = getForeignKeyIndex(tab,fld);
		if( idx < 0 ) return null;
		infaConstraint con = tab.constraintList.get(idx);
		// get the list of Natural Key elements from the refrenxced table
		String stab = con.ReferencedTableName.trim().toUpperCase();
		if( stab == null ) return null;
		int pdx = -1;
		for(int i=0;i<ddllist.size();i++)
		{
			if( ddllist.get(i).Name.trim().compareToIgnoreCase(stab) == 0) {
				pdx=i;
				break;
			}
		}
		if( pdx < 0 ) return null;
		return getNaturalKeyList(ddllist.get(pdx));
	}

	//---------------------------------------------------------------------------------
	private String getForeignKeyReferencedTable( infaSource tab ,  infaSourceField fld )
	//---------------------------------------------------------------------------------
	{
		int idx = getForeignKeyIndex(tab,fld);
		if( idx < 0 ) return null;
		return tab.constraintList.get(idx).ReferencedTableName;
	}
	
	//---------------------------------------------------------------------------------
	//---------------------------------------------------------------------------------
			
	
	private boolean generateMinusOne( ArrayList<infaSource> ddllist)
	{
		String FMinusName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Export" + xMSet.xU.ctSlash + "Minus_" + dbname.trim().toLowerCase() + ".txt";
		if( xMSet.xU.IsBestand( FMinusName ) ) {
			if( xMSet.xU.VerwijderBestand( FMinusName ) == false ) {
				errit("Cannot remove Excel [" + FMinusName + "] - It is probably in use");
				return false;
			}
		}
		//
		logit( 5 , "Dumping to File [" + FMinusName + "]" );
		logit( 5 , "POSITIONAL SQL [" + positioneel + "]" );
			
		//
		mino = new gpPrintStream( FMinusName , "UTF-8" );
		mino.do_standard_header(xMSet.getApplicationId());
		mino.println("-- Minus One and Two Script" );
		mino.println("--");
		
		boolean isok=true;
		//
		mino.println("-- START For testing purposes");
		for(int i=0;i<ddllist.size();i++)
		{
			if( do_minus_tab( ddllist.get(i) , true) == false ) isok=false;
		}
		mino.println("-- END For testing purposes");
		//
		for(int i=0;i<ddllist.size();i++)
		{
			if( do_minus_tab( ddllist.get(i) , false) == false ) isok=false;
		}
		
		mino.do_standard_tail("");
		mino.close();
		//
		return isok;
	}
	
	
	private boolean do_minus_tab( infaSource tab, boolean generatedelete)
	{
		
		// PK
	    boolean foundPK=false;
	    String sFirstSID = null;
	    String sPK = (tab.Name + "_DM_SID").toUpperCase().trim();
	    if( Strip != null ) {
	    	String uStrip = Strip.toUpperCase().trim();
	    	uStrip = xMSet.xU.Remplaceer(uStrip,"(","");
	    	uStrip = xMSet.xU.Remplaceer(uStrip,")","");
	    	//mino.println( "--" + uStrip );
	    	if( uStrip.length() > 0 ) {
	    	if( sPK.startsWith( uStrip) ) {
	    	 	sPK = sPK.substring( uStrip.length() );
	    	 }
	    	}
	    }
	    sPK = sPK.toLowerCase();
	    
	    if( generatedelete ) {
	    	mino.println("--");
	    	mino.println( "DELETE FROM " + tab.Name + " WHERE " + sPK + " IN (-1,-2);" );
	    	return true;
	    }
		
		//
	    mino.println("--");
		mino.println("INSERT INTO " + tab.Name );
		if( positioneel ) {
	        mino.println("SELECT minusvalues.* FROM " + tab.Name );
	    }
		else {
			mino.println("(");
			for(int i=0;i<tab.fieldList.size();i++)
			{
				String sCol = tab.fieldList.get(i).Name.trim().toLowerCase();
				sCol = ( i == 0 ) ? " " + sCol : "," + sCol;
				mino.println(sCol);
			}
			mino.println(") SELECT");
			for(int i=0;i<tab.fieldList.size();i++)
			{
				String sCol = tab.fieldList.get(i).Name.trim().toLowerCase();
				sCol = ( i == 0 ) ? " minusvalues." + sCol : ",minusvalues." + sCol;
				mino.println(sCol);
			}
			mino.println("FROM " + tab.Name);
		}
	    mino.println("RIGHT JOIN ( SELECT");
	    //
	    for(int k=0;k<2;k++) {
	     //
	     int iminus = ( k==0 ) ? -1 : -2;
	     String sLong = ( k==0 ) ? "Unknown" : "Not applicable";
	     String sShort = ( k==0 ) ? "U" : "X";
	     //
		 for(int i=0;i<tab.fieldList.size();i++)
		 {
		   infaSourceField fld = tab.fieldList.get(i);
		   //
		   if( sPK.compareToIgnoreCase(fld.Name.trim())==0) foundPK=true;
		   //
		   if( sFirstSID == null ) {
			   if( fld.Name.toUpperCase().endsWith("_SID") ) {
				   sFirstSID = fld.Name.trim();
			   }
		   }
		   //
		   // ti :  0 = num , 1 = s , 2 = date , 3 = KEY
		   // 4  :  active
		   // 5  :  source_dm
		   // 6 : RECORD_SOURCE
		   // 7 : RECORD_SOURCE
		   
		   int tipe = 0;
		   if( fld.DataType.toUpperCase().indexOf("CHAR") >= 0 ) tipe = 1;
		   if( fld.DataType.toUpperCase().indexOf("CHAR") >= 0 ) tipe = 1;
		   if( fld.DataType.toUpperCase().indexOf("DATE") >= 0 ) tipe = 2;
		   if( fld.DataType.toUpperCase().indexOf("TIME") >= 0 ) tipe = 2;
		   if( fld.Name.toUpperCase().endsWith("_KEY") && (tipe == 1)) tipe = 3;
		   if( fld.Name.toUpperCase().endsWith("ACTIVE_FLAG") && (tipe == 0)) tipe = 4;
		   if( fld.Name.toUpperCase().endsWith("SOURCE_DM_SID") && (tipe == 0) && (tab.Name.compareToIgnoreCase("D_SOURCE")!=0)) tipe = 5;
		   if( fld.Name.toUpperCase().endsWith("RECORD_SOURCE") && (tipe == 1)) tipe = 6;
		   if( fld.Name.toUpperCase().endsWith("RECORD_STATUS") && (tipe == 1)) tipe = 7;
		   if( fld.Name.toUpperCase().endsWith("DM_ST_DATETIME") && (tipe == 2)) tipe = 8;
		   if( fld.Name.toUpperCase().endsWith("DM_END_DATETIME") && (tipe == 2)) tipe = 9;
		   //
		   if( fld.Name.toUpperCase().endsWith("DM_TMSTMP_I") && (tipe == 2)) tipe = 10;
		   if( fld.Name.toUpperCase().endsWith("DM_TMSTMP_U") && (tipe == 2)) tipe = 10;
		   if( fld.Name.toUpperCase().endsWith("DM_TMSTMP_D") && (tipe == 2)) tipe = 10;
		   if( fld.Name.toUpperCase().endsWith("DM_TMSTMP") && (tipe == 2)) tipe = 10;
		   //
		   if( fld.Name.toUpperCase().endsWith("EDW_CHECKSUM") && (tipe == 1)) tipe = 11;
		   if( fld.Name.toUpperCase().endsWith("DM_CHECKSUM") && (tipe == 1)) tipe = 11;
		   if( fld.Name.toUpperCase().endsWith("EDW_DQ_INFO") && (tipe == 1)) tipe = 11;
		   // look for BIGINT versions of dates
		   if( isANumericDate( fld , tab ) ) tipe = 33;
		 
		   // SPECIAL cases
		   if( tab.Name.compareToIgnoreCase("D_IMPORTER") == 0 ) {
			   if( fld.Name.compareToIgnoreCase("IMPORTERNO") == 0 ) {
				   if( iminus == -2 ) {
					   tipe = 501;
				   }
			   }
		   }
		   if( tab.Name.compareToIgnoreCase("D_VEHICLE") == 0 ) {
			   if( (fld.Name.compareToIgnoreCase("VIN") == 0) || 
				   (fld.Name.compareToIgnoreCase("CHASSISNO") == 0) || 
				   (fld.Name.compareToIgnoreCase("CHASSISSERIES") == 0) ) {
				   if( iminus == -2 ) {
					   tipe = 502;
				   }
			   }
		   }
		   
		   
		   
		   String slijn = "";
		   switch( tipe )
		   {
		   case 0  : { slijn += iminus; break; }
		   case 1  : { if( fld.Precision <= 15 ) slijn += "'" + sShort + "'"; else  slijn += "'" + sLong + "'"; break; }
		   case 2  : { slijn += "TO_DATE('19000101','yyyymmdd')"; break; }
		   case 3  : { slijn += "'" + iminus + "'"; break; }
		   case 4  : { slijn += "1"; break; }
		   case 5  : { slijn += "3"; break; }
		   case 6  : { slijn += "'Inferred'"; break; }
		   case 7  : { slijn += "'I'"; break; }
		   case 8  : { slijn += "TO_DATE('19000101','yyyymmdd')"; break; }
		   case 9  : { slijn += "TO_DATE('29990101','yyyymmdd')"; break; }
		   case 10 : { slijn += "current_timestamp"; break; }
		   case 11 : { slijn += "''"; break; }
		   case 33 : { slijn += "19000101"; break; }
		   case 501: { mino.println("--  D_IMPORTER.IMPORTERNO = 0 WHEN SID = -2");
			           slijn += "0"; break; }
		   case 502: { mino.println("--  D_VEHICLE." + fld.Name + " = 0 WHEN SID = -2");
			           slijn += "''"; break; }
		   default:  { slijn += "huh?"; break; }
		   }
		   slijn = ( slijn.startsWith("'") ) ? slijn : " " + slijn;
		   slijn = ( i== 0 ) ? "  " + slijn : " ," + slijn;
		   //String sFiller = "                                               ".substring(slijn.length());
		   String sFiller = " ";
		   mino.println(slijn + sFiller + " as " + fld.Name.trim().toLowerCase());
		   
		   
		 }
		 //
		 if( k == 0 ) mino.println( "UNION SELECT");
	    }
		//
	    if( foundPK == false ) {
	    	if( sFirstSID != null ) {
	           sPK = sFirstSID.toLowerCase();    		
	    	}
	    	else 	sPK = "<TUP>";
			String serr = "-- COULD NOT FIND THE SURROGATE PRIMARY KEY ON [" + tab.Name + "] falling back to [" + sPK + "]";
			errit(serr);
			mino.println(serr);
		}
	    //
		mino.println(") minusvalues ON ( minusvalues." + sPK + " = " + tab.Name.toLowerCase() + "." + sPK + " )");
		mino.println("WHERE " + tab.Name.toLowerCase() + "." + sPK + " IS NULL;");
		//
		
		mino.println("--");
		mino.println("");
		
		return true;
	}
	
	
	private boolean isANumericDate( infaSourceField fld ,  infaSource tab )
	{
		// BIGINT or INTEGER
		if( (fld.DataType.toUpperCase().indexOf("BIGINT") < 0) && (fld.DataType.toUpperCase().indexOf("INTEGER") < 0) ) return false;
		// must end on _SID
		String sCol = fld.Name.trim().toUpperCase();
		if( sCol.endsWith("_SID") == false ) return false;
		// Strip
		String sLook = null;
		if( sCol.endsWith("_DM_SID")  ) {
			sLook = sCol.substring(0,sCol.length()-"_DM_SID".length());
		}
        if( sCol.endsWith("_SID") ) {
    		sLook = sCol.substring(0,sCol.length()-"_SID".length());
   	    }
        if( sLook == null ) return false;
        //
        //errit( sCol + " looking for [" + sLook + "]");
        sLook = sLook.trim().toUpperCase();
        for(int i=0;i<tab.fieldList.size();i++)
        {
        	 infaSourceField x = tab.fieldList.get(i);
        	 if( (x.DataType.indexOf("DATE") < 0 ) && (x.DataType.indexOf("TIME") < 0 ) ) continue;
        	 if( x.Name.trim().compareToIgnoreCase( sLook ) == 0 ) {
        		 //errit( "-->" + sCol + " looking for [" + sLook + "]");
        		 logit(5,"--> [" + tab.Name + "." + sCol + "] is a DATE SID");
        		 return true;
        	 }
        }
        
		return false;
	}
}
