package pcGenerator.ddl;

import java.util.ArrayList;

import pcGenerator.generator.generatorConstants;
import pcGenerator.powercenter.infa2DCoordinate;
import pcGenerator.powercenter.infaDataType;
import pcGenerator.powercenter.infaPair;
import pcGenerator.powercenter.infaSource;
import pcGenerator.powercenter.infaSourceField;
import pcGenerator.powercenter.infaSourceKloon;
import pcGenerator.powercenter.infaSourceUtils;
import generalpurpose.gpPrintStream;
import generalpurpose.gpUtils;
import generalpurpose.pcDevBoosterSettings;


public class externalTablesORCLImport {
	
	enum LayerType { DDL, SOURCE , SOURCEFLATFILE , TARGETFLATFILE  }
	
	pcDevBoosterSettings xMSet = null;
	gpUtils xU = null;
	generatorConstants gConst = null;
	
	boolean DEBUG = false;
	boolean isValid = true;
	ArrayList<infaSource> srcList = null;
	
	
	//----------------------------------------------------------------
	public externalTablesORCLImport(pcDevBoosterSettings xi , String DBName , String FInName )
	//----------------------------------------------------------------
	{
		xMSet = xi;
		xU = xMSet.xU;
		gConst = new generatorConstants(xMSet);
	    //
		if( xU.IsBestand( FInName ) == false ) {
			   errit("Cannot open [" + FInName + "] for reading.");
			   isValid = false;
		}
		if( isValid ) {
			   db2Tokenize db2T = new db2Tokenize( xMSet , DBName , rdbmsDatatype.DBMAKE.ORACLE , null );
			   srcList = db2T.parseFile( FInName);
			   if( srcList == null ) isValid = false;
			   if( isValid ) {
				   if( DEBUG ) show();
				   isValid = create_infosets( rdbmsDatatype.DBMAKE.ORACLE , "BEAT-EXT-TBLS" , FInName );
				   if( isValid ) {
					   isValid = consolidate_all_external_table_fields();
					   if( DEBUG ) show();
					   if ( isValid ) {  // just redo with adapted ORCL specs
						   isValid = create_infosets( rdbmsDatatype.DBMAKE.ORACLE , "BEAT-EXT-TBLS-CNSLDTD" , FInName );
						   if( isValid ) isValid = quickDDL();
					   }
				   }
			   }
		}
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
	private void show()
	//----------------------------------------------------------------
	{
		infaSourceUtils u = new infaSourceUtils(xMSet);
		for(int i=0;i<srcList.size();i++)
		{
			u.show(srcList.get(i));
		}
	}
	
	//----------------------------------------------------------------
	private boolean create_infosets(rdbmsDatatype.DBMAKE dbmake  , String DBName , String FNaamIn)
	//----------------------------------------------------------------
	{
			     if( srcList == null ) {
			    	 errit("infa source list is NULL");
			    	 return false;
			     }
			     if( srcList.size() == 0 ) {
			    	 errit("The parser did not put any infaSources on the list to process");
			    	 return false;
			     }
			     // Just store the Oracle DDL 
				 if( makeORCLDDLDump( dbmake , DBName , LayerType.DDL , FNaamIn) == false ) return false;
				
				 // transform the ORCL into Flatfile - fixed width
				 if( makeORCLDDLDump( dbmake , DBName , LayerType.SOURCEFLATFILE , FNaamIn) == false ) return false;
				 
				 return true;
	}
	
	//----------------------------------------------------------------
	private boolean makeORCLDDLDump(rdbmsDatatype.DBMAKE dbmake , String DBName ,  LayerType tipe  , String FInName)
	//----------------------------------------------------------------
	{
			    String DatabaseName = null;
			    switch( tipe )
			    {
			     case DDL : {
			    	DatabaseName = DBName + "-ext-tables";
			    	break;
			     }
			     case SOURCEFLATFILE : {
				    	DatabaseName = DBName + "-ext-tables";
				    	break;
				     }
			     default : {
			    	errit("(externalTablesORCL) System error - unhandled LayerType " + tipe); 
			    	return false;
			     }
			    }
			    
			    // loop through list and transform attributes
			    infaSourceKloon kloon = new infaSourceKloon();
			    ArrayList<infaSource> kloonList = new ArrayList<infaSource>();
			    for(int i=0 ;i<srcList.size() ; i++)
			    {
			    	// kloon the source entity - so that it canbe modified
			        infaSource src = kloon.kloon(srcList.get(i));
				    // overwrite
			        src.Databasetype = ""+dbmake;
			        src.Dbdname = DBName;
			        //
			        if( transform_attributes( src , tipe , dbmake ) == false ) {
				    	errit("TABLE [" + srcList.get(i).Name + "] was not exported [Tipe=" + tipe + "] [Name=" + DBName + "] [Make=" + dbmake +"]" );
				        continue;
				    }
				    kloonList.add( src );
			    }
			   // call the writeInfaMetadata class
			   if( kloonList.size() <= 0 ) {
				   errit("Nothing left to export");
				   return false;
			   }
			   writeInfaMetadata wrt = new writeInfaMetadata( xMSet , FInName );
			   boolean ib = wrt.createMetadataFiles( kloonList );
			   kloonList = null;
			   if( ib == false ) return false;
			   //
			   return true;
	}
	
	//----------------------------------------------------------------
	private boolean transform_attributes(infaSource src , LayerType tipe , rdbmsDatatype.DBMAKE dbmake )
	//----------------------------------------------------------------
	{
		 switch( tipe )
	     {
	     			 case DDL : {
	     					src.tipe = readInfaXML.ParseType.POWERDESIGNERDDL;  // will do
	     					boolean ib = set_ORCL_INFA_Specs( src );
	     					if( ib == false ) return false;
	     					return true;
		     		 }
				     case SOURCEFLATFILE : {
				    	src.tipe = readInfaXML.ParseType.SOURCEFLATFILE;
				    	boolean ib = transformORCLToFixedWidthFlatFile( src );
				    	if( ib == false ) return false;
				    	// source level
				    	// overrule
						src.Databasetype  = "FLAT FILE";
					    src.Description   = "Flat file derived from BEAT external ORCL table " + src.Name;
					    src.Name          = "FF_" + src.Name;
					    src.TableOptions  = null;
					    src.VersionNumber = "1";
					    src.ObjectVersion = "1";
					    //
					    /*  09SEP2015 KB - Applied default settings from generatorConstatns
					    src.flafle.CodePage = "MS1252";
					    src.flafle.Consecdelimiterasone = "NO";
					    src.flafle.Delimited = "NO";
					    src.flafle.Delimiters = ",";
					    src.flafle.EscapeCharacter = "";
					    src.flafle.Keepescapechar = "NO";
					    src.flafle.LineSequential = "NO";
					    src.flafle.Multidelimitersasand = "YES";
					    src.flafle.Nullcharacter = "*";
					    src.flafle.NullCharType = "ASCII";
					    src.flafle.Padbytes = "1";             // ?
					    src.flafle.QuoteCharacter = "DOUBLE";  // ??
					    src.flafle.Repeatable = "NO";
					    src.flafle.RowDelimiter = "0";   // geen 10 - wellicht een indicate van een fixed width
					    src.flafle.ShiftSensitiveData = "NO";
					    src.flafle.Skiprows = "0";
					    src.flafle.Striptrailingblanks = "NO";
					    */
					    src.flafle.CodePage 			= gConst.getConstantValueFor("FF_CodePage"); 
					    src.flafle.Consecdelimiterasone = gConst.getConstantValueFor("FF_Consecdelimiterasone");
					    src.flafle.Delimited 			= gConst.getConstantValueFor("FF_Delimited");
					    src.flafle.Delimiters 			= gConst.getConstantValueFor("FF_Delimiters");
					    src.flafle.EscapeCharacter 		= gConst.getConstantValueFor("FF_EscapeCharacter");
					    src.flafle.Keepescapechar 		= gConst.getConstantValueFor("FF_Keepescapechar");
					    src.flafle.LineSequential 		= gConst.getConstantValueFor("FF_LineSequential");
					    src.flafle.Multidelimitersasand = gConst.getConstantValueFor("FF_Multidelimitersasand");
					    src.flafle.Nullcharacter 		= gConst.getConstantValueFor("FF_Nullcharacter");
					    src.flafle.NullCharType 		= gConst.getConstantValueFor("FF_NullCharType");
					    src.flafle.Padbytes 			= gConst.getConstantValueFor("FF_Padbytes");
					    src.flafle.QuoteCharacter 		= gConst.getConstantValueFor("FF_QuoteCharacter");
					    src.flafle.Repeatable 			= gConst.getConstantValueFor("FF_Repeatable");
					    src.flafle.RowDelimiter 		= gConst.getConstantValueFor("FF_RowDelimiter");
					    src.flafle.ShiftSensitiveData 	= gConst.getConstantValueFor("FF_ShiftSensitiveData");
					    src.flafle.Skiprows 			= gConst.getConstantValueFor("FF_Skiprows");
					    src.flafle.Striptrailingblanks 	= gConst.getConstantValueFor("FF_Striptrailingblanks");

					    // Overrule default values for Fixed with
					    src.flafle.Delimited 		= "NO";
					   
					    
					    
					    //
					    src.tableAttributeList = null;
					    src.tableAttributeList = new ArrayList<infaPair>();
					    for(int i=0;i<7;i++)
					    {
					    	infaPair ip = null;
					    	switch(i)
					    	{
					    	case 0 : { ip = new infaPair("Base Table Name",""); break; }
					    	case 1 : { ip = new infaPair("Search Specification",""); break; }
					    	case 2 : { ip = new infaPair("Sort Specification",""); break; }
					    	case 3 : { ip = new infaPair("Datetime Format","A  19 mm/dd/yyyy hh24:mi:ss"); break; }
					    	case 4 : { ip = new infaPair("Thousand Separator","None"); break; }
					     	case 5 : { ip = new infaPair("Decimal Separator","."); break; }
					    	case 6 : { ip = new infaPair("Add Currently Processed Flat File Name Port","NO"); break; }
					    	}
					    	if( ip == null ) continue;
					    	src.tableAttributeList.add(ip);
					    }
					    
				    	return true;
				     }
				     default : {
				    	errit("(orclExternal) System error - unhandled LayerType [" + tipe +"] for [" + src.Name + "]"); 
				    	return false;
				     }
		  }
	}
	
	// get the precision, length, etc right
	//----------------------------------------------------------------
	private boolean set_ORCL_INFA_Specs( infaSource src )
	//----------------------------------------------------------------
	{
		int ofs = 0;
		int pofs=0;
		for(int i=0;i<src.fieldList.size();i++)
		{
			infaSourceField f = src.fieldList.get(i);
			infaDataType tp = new infaDataType ( xMSet , "ORACLE" , "SOURCE" , f.DataType , f.Precision , f.scale );
			if( tp.isValid == false ) return false;
			// overrule
		    src.fieldList.get(i).DataType = tp.DataType;
			src.fieldList.get(i).Precision = tp.Precision;
			src.fieldList.get(i).scale = tp.Scale;
			src.fieldList.get(i).Length = tp.Length;
			src.fieldList.get(i).physicalLength = tp.PhysicalLength;
			src.fieldList.get(i).offset = ofs;
			src.fieldList.get(i).physicalOffset = pofs;
			// defaults
			//
			ofs += tp.Length;
			pofs += tp.PhysicalLength;
			//
			tp = null;
		}
		return true;
	}
	
	// naar nstring of string
	//----------------------------------------------------------------
	private boolean transformORCLToFixedWidthFlatFile( infaSource src )
	//----------------------------------------------------------------
	{
		if( src.flafle.isFixedWidth == false ) {
			errit("[" + src.Name +"] is not a fixed width" );
			return false;
		}
		if( src.flafle.extTablePositionList.size() != src.fieldList.size()  ) {
			errit("[" + src.Name +"] system error - flat file number POSITIONS does not match number of fields" );
			return false;
		}
	    //	
		for(int i=0;i<src.fieldList.size();i++)
		{
			infaSourceField f = src.fieldList.get(i);
			int strPos = src.flafle.extTablePositionList.get(i).x;
			int endPos = src.flafle.extTablePositionList.get(i).y;
			if( (strPos<0) || (endPos<0) || (strPos>endPos) ) {
				errit("Error in POSITION specs (" + strPos + "," + endPos + ") on [" + src.Name + "." + f.Name + "]");
				return false;
			}
			// check
		    int prevPrecision = src.fieldList.get(i).Precision;
			// Datatypes
			src.fieldList.get(i).DataType       = "nstring";
			src.fieldList.get(i).Precision      = endPos - strPos + 1;
			src.fieldList.get(i).scale          = 0;
			src.fieldList.get(i).Length         = src.fieldList.get(i).Precision;
			src.fieldList.get(i).physicalLength = src.fieldList.get(i).Precision;
			src.fieldList.get(i).offset         = strPos - 1;
			src.fieldList.get(i).physicalOffset = src.fieldList.get(i).offset;
			// overrules
			src.fieldList.get(i).FieldProperty = "0";  // niet default
			src.fieldList.get(i).FieldType = "ELEMITEM";
			src.fieldList.get(i).Hidden = "NO";
			src.fieldList.get(i).KeyType = "NOT A KEY";
	        src.fieldList.get(i).Level = 0;
	        src.fieldList.get(i).mandatory = false;
	        src.fieldList.get(i).Occurs = 0;
	        src.fieldList.get(i).PictureText = "";
	        src.fieldList.get(i).UsageFlags = "";
	        //System.out.println("" + strPos + " " + endPos + " " + src.fieldList.get(i).physicalLength );
	        if( prevPrecision > src.fieldList.get(i).Precision ) {
	        	errit("POSITION(" + strPos + "," + endPos + ") -> " + src.fieldList.get(i).Precision + " on [" + src.Name + "." + f.Name + 
	        			        "] probably too small to accommodate [" + src.fieldList.get(i).DataType + "] with anticipated precision [" + prevPrecision +"]");
	        	return false;
	        }
		}
	
		return true;
	}
	
	
	//----------------------------------------------------------------
	private boolean consolidate_all_external_table_fields()
	//----------------------------------------------------------------
	{
	   if( srcList == null ) {
		   errit("Source list is NULL");
		   return false;
	   }
	   if( srcList.size() < 0 ) {
		   errit("Empty source list");
		   return false;
	   }
	   if( srcList.get(0).flafle.isFixedWidth == false ) {
		   errit("Not fixed width");
		   return false;
	   }
	   for(int i=0;i<srcList.size();i++)
	   {
		   if( consolidate( srcList.get(i) ) == false ) {
			   logit(1,"Table [" + srcList.get(i).Name + "] will be excluded from list");
			   srcList.get(i).Name = null;
		   };
	   }
	   
	   // remove items that have been flagged with name is NULL
	   int ncount = srcList.size();
	   int tel=0;
	   for(int k=0;k<ncount;k++)
	   {
		   for(int i=0;i<srcList.size();i++)
		   {
		   if( srcList.get(i).Name == null ) {
			   srcList.remove(i);
			   tel++;
			   break;
		   }
		   }
	   }
	   if( tel > 0 ) logit(1,"Removed [" + tel + "] tables from consolidated DDL");
	   return true;
	}
		
	//----------------------------------------------------------------
	private boolean consolidate( infaSource src )
	//----------------------------------------------------------------
	{
		if(  src.flafle.extTablePositionList == null ) {
	         errit( "External Table field info is missing for [" + src.Name + "]");
	         return false;
	    }
		if(  src.flafle.extTablePositionList.size() != src.fieldList.size() ) {
	         errit( "Number of external table columns [" + src.flafle.extTablePositionList.size() + "] does no match number of columns [" + src.fieldList.size() + "] for [" + src.Name + "]");
	         return false;
	    }
		// Look for _S and _SIGN and its counterpart
		ArrayList<infa2DCoordinate> list = new ArrayList<infa2DCoordinate>();
		int direction = -1;
		for(int i=0;i<src.fieldList.size();i++)
		{
		  //
	      String colname = src.fieldList.get(i).Name.trim().toUpperCase();
	      if( (colname.endsWith("_S")==false) && (colname.endsWith("_SIGN")==false) ) continue;
	      if( src.flafle.extTablePositionList.get(i).x != src.flafle.extTablePositionList.get(i).y ) {
	    	  errit("Found an external table SIGN column with size differnt from 1 character for [" + src.Name + "." + colname + "]");
	    	  return false;
	      }
	      // stick something to end
	      String sNoSign = xMSet.xU.Remplaceer( colname+"§" , "_SIGN§" , "");
	      sNoSign = xMSet.xU.Remplaceer( sNoSign , "_S§" , "");
	      sNoSign = xMSet.xU.Remplaceer( sNoSign , "_§" , "");
	      int match = -1;
	      for(int k=0;k<src.fieldList.size();k++)
	      {
	    	  if( sNoSign.compareToIgnoreCase( src.fieldList.get(k).Name.trim() )  != 0 ) continue;
	    	  match = k;
	    	  break;
	      }
	      if( match < 0 ) {
	    	  errit("Cannot find matching column for [" + src.Name + "." + colname + "]");
	    	  /*
	    	  for(int k=0;k<src.fieldList.size();k++)
		      {
		    	  logit( 1 , " > Is (" + sNoSign + ") == (" + src.fieldList.get(k).Name + ") " + sNoSign.compareToIgnoreCase( src.fieldList.get(k).Name.trim()) );
		      }
		      */
	    	  return false;
	      }
	      int curDirection = ( i < match ) ? 1 : 2;   // 1 + precedes    2 + follows
	      if( direction < 0 ) direction = curDirection;
	      if( direction != curDirection ) {
	    	  logit(1 ,  src.Name + " the SIGN field both precedes and succeeds the numeric field. probably an error in the DDL" );
	    	  // return false;  -- ignore
	      }
	      infa2DCoordinate x = new infa2DCoordinate(i,match);	  
	      list.add(x);
	    }
		//if( list.size() == 0 )  return true;  // nothing to do
		// collapse
		for(int i=0 ; i<list.size() ; i++)
		{
			int x = list.get(i).x;   // SIGN idx 
			int y = list.get(i).y;   // matching field idx
			if( DEBUG ) logit( 5 , "Collapsing (" + x + "," + y +") [" + src.fieldList.get(x).Name + "] and [" + src.fieldList.get(y).Name + "]");
		    //
			if( x < y ) {    //  SIGN precedes
				 if( (src.flafle.extTablePositionList.get(x).y + 1 ) != src.flafle.extTablePositionList.get(y).x ) {
			    	  errit("External table SIGN column [" + src.Name + "." + src.fieldList.get(x).Name + 
			    			 "] End Position [" + src.flafle.extTablePositionList.get(x).y +
			    			 "] is not aligned with successor [" +  src.fieldList.get(y).Name + 
			    			 "] Start Poistion [" + src.flafle.extTablePositionList.get(y).x + "]");
			    	  return false;
			      }
				 int startp = src.flafle.extTablePositionList.get(x).x;
				 int endp = src.flafle.extTablePositionList.get(y).y;
				 //
				 src.fieldList.get(x).DataType = "%PLEASE%REMOVE%";
				 src.flafle.extTablePositionList.get(x).x = -100;
				 src.flafle.extTablePositionList.get(x).y = -200;
				 //
				 src.fieldList.get(y).DataType  = "VARCHAR2";
				 src.fieldList.get(y).Precision = endp - startp + 1;
				 src.fieldList.get(y).scale     = 0;
				 src.flafle.extTablePositionList.get(y).x = startp;
				 //
			}
			else {   // SIGN succeeds
				 if( (src.flafle.extTablePositionList.get(y).y + 1 ) != src.flafle.extTablePositionList.get(x).x ) {
			    	  errit("External table SIGN column [" + src.Name + "." + src.fieldList.get(x).Name + 
			    			 "] Start Position [" + src.flafle.extTablePositionList.get(x).x +
			    			 "] is not aligned with predecessor [" +  src.fieldList.get(y).Name + 
			    			 "] End Poistion [" + src.flafle.extTablePositionList.get(y).y + "]");
			    	  return false;
			      }
				 int startp = src.flafle.extTablePositionList.get(y).x;
				 int endp = src.flafle.extTablePositionList.get(x).y;
				 //
				 src.fieldList.get(x).DataType = "%PLEASE%REMOVE%";
				 src.flafle.extTablePositionList.get(x).x = -100;
				 src.flafle.extTablePositionList.get(x).y = -200;
				 //
				 src.fieldList.get(y).DataType  = "VARCHAR2";
				 src.fieldList.get(y).Precision = endp - startp + 1;
				 src.fieldList.get(y).scale     = 0;
				 src.flafle.extTablePositionList.get(y).y = endp;
			}
		}  // collapse
		// remove
		int ncount = src.fieldList.size();
		for(int k=0;k<ncount;k++)
		{
			for(int i=0;i<src.fieldList.size();i++)
			{
				if( src.fieldList.get(i).DataType.trim().compareToIgnoreCase("%PLEASE%REMOVE%") == 0 ) {
					// Careful you need to remove both the  field and flat file extention
					src.fieldList.remove( i );
					src.flafle.extTablePositionList.remove(i);
					break;
				}
			}
		}
		//
		String sPrecSucc =  (direction == 1) ? "PRECEDING" : "SUCCEEDING";
		logit( 5 , "Combined [" + list.size() + "] columns on [" + src.Name + "]. SIGN columns defined to be " + sPrecSucc );
		return true;
	}
	
	
	//----------------------------------------------------------------
	private boolean quickDDL()
	//----------------------------------------------------------------
	{
		//  translateDDLFromTo( String DDLFileNameIn , rdbmsDatatype.DBMAKE srcDBTipe , rdbmsDatatype.DBMAKE tgtDBTipe , boolean SSTConvertor )
		// dump it and call translator from ORA to ORA
		try {
		 String FName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "orcl-ext-consol.txt";
		 gpPrintStream fout = new gpPrintStream( FName , "ASCII" );
		 for(int i=0;i<srcList.size();i++)
		 {
			String sL="create table " + srcList.get(i).Dbdname + "." + srcList.get(i).Name + "(";
			fout.println(sL);
			for(int j=0;j<srcList.get(i).fieldList.size();j++)
			{
		      infaSourceField f = srcList.get(i).fieldList.get(j);
		      sL = ( j==0 ) ? " " + f.Name : "," + f.Name;
		      //
		      sL += " " + f.DataType + " ";
		      //
		      String sPrecScale = "";
			  if( f.Precision > 0) {
				   if( f.scale > 0 ) {
						sPrecScale = "(" + f.Precision + "," + f.scale + ")";
					}
					else sPrecScale = "(" + f.Precision + ")";
			  }
			  sL += " " + sPrecScale;
		      //
			  if( f.mandatory ) sL += " NOT NULL";
		      fout.println(sL);
			}
			// ORGANIZE EXT
			if( srcList.get(i).flafle.isFixedWidth == false ) {
				fout.println(");");
				fout.println("");
				continue;  // should not occur
			}
			else fout.println(")");
			if( srcList.get(i).flafle.extTablePositionList == null ) {
				errit( "No POSITION information on [" + srcList.get(i).Name + "]");
				continue;  // not fatal
			}
			for(int k=0;k<srcList.get(i).flafle.extTablePositionList.size();k++)
			{
			  sL = ( k == 0 ) ? "ORGANIZATION EXTERNAL FIELDS ( " + xMSet.xU.ctEOL + "   ": " , ";
			  sL += srcList.get(i).fieldList.get(k).Name + " ";
			  sL += "POSITION (";
			  sL += srcList.get(i).flafle.extTablePositionList.get(k).x + ":" ;
			  sL += srcList.get(i).flafle.extTablePositionList.get(k).y + ")" ;
			  fout.println(sL);
			}
			fout.println(");");
			fout.println("");
		 }
		 fout.close();
		 // just call the translator and translate from ORCl to ORCL
		 ddlTranslator trans = new ddlTranslator( xMSet );
		 return trans.translateDDLFromTo ( FName , "ORACLE" ,  "ORACLE" , null );
	  }
	  catch(Exception e) {
		  errit("(quickDDL) Unknown error");
			return false;
	  }
		
	}
	
	
		
}
