package pcGenerator.ddl;

import generalpurpose.gpUtils;
import generalpurpose.pcDevBoosterSettings;

import java.util.ArrayList;

import pcGenerator.generator.generatorConstants;
import pcGenerator.powercenter.infaDataType;
import pcGenerator.powercenter.infaPair;
import pcGenerator.powercenter.infaSource;
import pcGenerator.powercenter.infaSourceField;
import pcGenerator.powercenter.infaSourceKloon;


public class powerDesignerDDLImport {

    enum LayerType { DDL, SOURCE , SOURCEFLATFILE , TARGETFLATFILE , TARGET }
	
	boolean isValid = true;
	pcDevBoosterSettings xMSet = null;
	generatorConstants gConst = null;
	gpUtils xU = null;
	ArrayList<infaSource> srcList = null;
	
	//----------------------------------------------------------------
	public powerDesignerDDLImport(pcDevBoosterSettings xi , String inDBmake , String DBName , String FInName , String ConfigFileNameIn )
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
		   if( isValid )
		   {
			   rdbmsDatatype rb = new rdbmsDatatype(xMSet);
			   rdbmsDatatype.DBMAKE dbmake = rb.getDBMAKEType(inDBmake);
			   if( dbmake != null ) {
				   switch( dbmake )
				   {
				   case ORACLE : {    
					   db2Tokenize db2T = new db2Tokenize( xMSet , DBName , dbmake , ConfigFileNameIn );
					   srcList = db2T.parseFile( FInName);
					   if( srcList == null ) isValid = false;
					   break;
				   }
				   case DB2 : {    
					   db2Tokenize db2T = new db2Tokenize( xMSet , DBName , dbmake , ConfigFileNameIn );
					   srcList = db2T.parseFile( FInName);
					   if( srcList == null ) isValid = false;
					   break;
				   }
				   case NETEZZA : {    
					   db2Tokenize db2T = new db2Tokenize( xMSet , DBName , dbmake , ConfigFileNameIn );
					   srcList = db2T.parseFile( FInName);
					   if( srcList == null ) isValid = false;
					   break;
				   }
				   default : {
					   errit("(powerDesignerDDLImport) Unsupported DBMake [" + dbmake  + "]" );
					   isValid = false;
					   break;
				   }
				   }
				   if( isValid ) {
					   isValid = create_infosets(dbmake , DBName , FInName);
				   } 
			   }
			   else {
				   errit("(powerDesignerDDLImport) Unknown DBMake [" + dbmake  + "]" );  
			   }
		   }
		   String sstat = ( isValid == true ) ? "succesfully" : "with errors"; 
		   logit(1,"PowerDesigner Import completed "+ sstat);
	}
	
	//----------------------------------------------------------------
	public boolean getIsValid()
	//----------------------------------------------------------------
	{
		return isValid;
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
	private boolean create_infosets(rdbmsDatatype.DBMAKE dbmake  , String DBName , String FNaamIn)
	//----------------------------------------------------------------
	{
		     if( srcList == null ) {
		    	 System.err.println("infa source list is NULL");
		    	 return false;
		     }
		     if( srcList.size() == 0 ) {
		    	 errit("(create_infosets) The parser did not put any infaSources on the list to process");
		    	 return false;
		     }
		     // PowerDesigner DDL
		     logit(5,"Creating DDL Layer for [" + dbmake + "] [" + DBName + "] from " + FNaamIn );
			 if( makeInfaDDLDump( dbmake , DBName , LayerType.DDL , FNaamIn) == false ) return false;
			 
			 // DB2 propagator SOURCE 
			 logit(5,"Creating SOURCE Layer for [" + dbmake + "] [" + DBName + "] from " +  FNaamIn );
			 if( makeInfaDDLDump( dbmake , DBName , LayerType.SOURCE , FNaamIn ) == false ) return false;
			 
			 // Source Flat File Source derived from the DDL
			 logit(5,"Creating SOURCEFLATFILE Layer for [" + dbmake + "] [" + DBName + "] from " + FNaamIn );
			 if( makeInfaDDLDump( dbmake , DBName , LayerType.SOURCEFLATFILE , FNaamIn ) == false ) return false;
			
			 // Target Flat File derived from the DDL
			 logit(5,"Creating TARGETFLATFILE Layer for [" + dbmake + "] [" + DBName + "] from " + FNaamIn );
			 if( makeInfaDDLDump( dbmake , DBName , LayerType.TARGETFLATFILE , FNaamIn ) == false ) return false;
		     
			// Target Table derived from the DDL
			 if( dbmake == rdbmsDatatype.DBMAKE.NETEZZA ) {
			  logit(5,"Creating TARGET Layer for [" + dbmake + "] [" + DBName + "] from " + FNaamIn );
			  if( makeInfaDDLDump( dbmake , DBName , LayerType.TARGET , FNaamIn ) == false ) return false;
			 }
			 if( dbmake == rdbmsDatatype.DBMAKE.ORACLE ) {
				  logit(5,"Creating TARGET Layer for [" + dbmake + "] [" + DBName + "] from " + FNaamIn );
				  if( makeInfaDDLDump( dbmake , DBName , LayerType.TARGET , FNaamIn ) == false ) return false;
			 }
			 
			 //	 
			 return true;
		}

	//----------------------------------------------------------------
	private boolean makeInfaDDLDump(rdbmsDatatype.DBMAKE dbmake , String DBName ,  LayerType tipe  , String FInName)
	//----------------------------------------------------------------
	{
		    String DatabaseName = null;
		    switch( tipe )
		    {
		     case DDL : {
		    	DatabaseName = DBName;
		    	break;
		     }
		     case SOURCE : {
		    	DatabaseName = DBName + "-Source";
		    	break;
		     } 
		     case SOURCEFLATFILE : {
		    	DatabaseName = DBName + "-FlatFile";
		    	break;
		     }
		     case TARGETFLATFILE : {
			    	DatabaseName = DBName + "-FlatFile";
			    	break;
			     }
		     case TARGET : {
		    	 DatabaseName = DBName + "-Target";
			    	break;
		     }
		     default : {
		    	errit("(makeInfaDDLDump) System error - unhandled LayerType " + tipe); 
		    	return false;
		     }
		    }
		    
		    //System.err.println("Tipe=" + tipe + " Name=" + DBName + " Make=" + dbmake );
		    
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
			    	 src.tipe = readInfaXML.ParseType.POWERDESIGNERDDL;
			    	 return true;   // nothing more to do
			     }
			     case SOURCE : {
			    	src.tipe = readInfaXML.ParseType.SOURCETABLE;
		    		return transformToSource(src); 
			     } 
			     case SOURCEFLATFILE : {
			    	src.tipe = readInfaXML.ParseType.SOURCEFLATFILE;
			    	boolean ib = transformToFlatFile( src );
			    	if( ib == false ) return false;
			    	// put default settings in place - after datatype checks on the fields
			    	if( applyDefaultFlatFileSettings(src) == false ) return false;
			    	return true;
			     }
			     case TARGETFLATFILE : {
				   	src.tipe = readInfaXML.ParseType.TARGETFLATFILE;
				   	boolean ib = transformToFlatFile( src );
				   	if( ib == false ) return false;
				   	// put default settings in place - after datatype checks on the fields
				   	if( applyDefaultFlatFileSettings(src) == false ) return false;
				   	return true;
				 }
			     case TARGET : {
				    	src.tipe = readInfaXML.ParseType.TARGETTABLE;
			    		return transformToTarget(src); 
				     } 
			     default : {
			    	errit("(tansform attributes) System error - unhandled LayerType [" + tipe +"] for [" + src.Name + "]"); 
			    	return false;
			     }
			    }
	}
	
	//----------------------------------------------------------------
	private boolean transformToSource( infaSource src )
	//----------------------------------------------------------------
	{
		    // create a infaDatabaseType and recuperate the its settings
			int ofs=0;
			int pofs=0;
			for(int i=0;i<src.fieldList.size();i++)
			{
			  infaSourceField fld = src.fieldList.get(i);
			  infaDataType tp = new infaDataType( xMSet , src.Databasetype , "SOURCE" , fld.DataType , fld.Precision , fld.scale );
			  if( tp.isValid == false ) return false;
			  if( checkNegZeroNumeric( tp.Precision , src.Name , fld.Name , "PRECISION" , fld.DataType) == false ) return false;
			  if( checkNegZeroNumeric( tp.PhysicalLength , src.Name , fld.Name , "PHYSICALLENGTH" , fld.DataType) == false ) return false;
			  if( checkNegNumeric( tp.Length , src.Name , fld.Name , "LENGTH" , fld.DataType) == false ) return false;
			  if( checkNegNumeric( tp.Scale , src.Name , fld.Name , "SCALE" , fld.DataType) == false ) return false;
			  // overwrite
			  src.fieldList.get(i).DataType = tp.DataType;
			  src.fieldList.get(i).Precision = tp.Precision;
			  src.fieldList.get(i).scale = tp.Scale;
			  src.fieldList.get(i).Length = tp.Length;
			  src.fieldList.get(i).physicalLength = tp.PhysicalLength;
			  src.fieldList.get(i).offset = ofs;
			  src.fieldList.get(i).physicalOffset = pofs;
			  //  No idea why - but in P8 the source must have fielproperty set to 0 if not a flatfile
			  if( xMSet.getpowermartversion() > 8 ) { 
				  //if( src.flafle != null ) {
					  	src.fieldList.get(i).FieldProperty = "0";
				  //}
			  }
			  // defaults
			  //
			  ofs += tp.Length;
			  pofs += tp.PhysicalLength;
			  //
			  tp = null;
			}
			return true;
	}
	
	//----------------------------------------------------------------
	private boolean transformToTarget( infaSource src )
	//----------------------------------------------------------------
	{
			    // create a infaDatabaseType and recuperate the its settings
				int ofs=0;
				int pofs=0;
				for(int i=0;i<src.fieldList.size();i++)
				{
				  infaSourceField fld = src.fieldList.get(i);
				  infaDataType tp = new infaDataType( xMSet , src.Databasetype , "TARGET" , fld.DataType , fld.Precision , fld.scale );
				  if( tp.isValid == false ) return false;
				  if( checkNegZeroNumeric( tp.Precision , src.Name , fld.Name , "PRECISION" , fld.DataType) == false ) return false;
				  if( checkNegZeroNumeric( tp.PhysicalLength , src.Name , fld.Name , "PHYSICALLENGTH" , fld.DataType) == false ) return false;
				  if( checkNegNumeric( tp.Length , src.Name , fld.Name , "LENGTH" , fld.DataType) == false ) return false;
				  if( checkNegNumeric( tp.Scale , src.Name , fld.Name , "SCALE" , fld.DataType) == false ) return false;
				  // overwrite
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
		
	//----------------------------------------------------------------
	private boolean checkNegZeroNumeric( int i , String TableName , String ColName , String sTag , String dtype)
	//----------------------------------------------------------------
	{
		if( i > 0 ) return true;
		errit("(pwdDDLImport) Got an invalid number [" + i + "] for attribute [" + sTag + "] on " + TableName + "." + ColName + " type [" + dtype + "] after datatype convertor");
		return false;
	}
	
	//----------------------------------------------------------------
	private boolean checkNegNumeric( int i , String TableName , String ColName , String sTag , String dtype )
	//----------------------------------------------------------------
	{
			if( i >= 0 ) return true;
			errit("(pwdDDLImport) Got an invalid number [" + i + "] for attribute [" + sTag + "] on " + TableName + "." + ColName + " type [" + dtype + "] after datatype convertor");
			return false;
	}
	
	//----------------------------------------------------------------
	private boolean transformToFlatFile( infaSource src )
	//----------------------------------------------------------------
	{
		    // create a infaDatabaseType 
		    // Flat File  datatype is {string|nstring} / length = precision / physical prec = precision / scale = 0
			int ofs=0;
			int pofs=0;
			for(int i=0;i<src.fieldList.size();i++)
			{
			  infaSourceField fld = src.fieldList.get(i);
			  infaDataType tp = new infaDataType( xMSet , src.Databasetype , "SOURCE" , fld.DataType , fld.Precision , fld.scale );
			  if( tp.isValid == false ) return false;
			  if( checkNegZeroNumeric( tp.Precision , src.Name , fld.Name , "PRECISION" , fld.DataType ) == false ) return false;
			  // overwrite
			  String stringTipe = "string";
			  if(  tp.DataType.compareToIgnoreCase("VARGRAPHIC") == 0 ) stringTipe = "nstring";
			  src.fieldList.get(i).DataType = stringTipe;
			  src.fieldList.get(i).Precision = tp.Precision;
			  src.fieldList.get(i).scale = 0;
			  src.fieldList.get(i).Length = tp.Precision;
			  src.fieldList.get(i).physicalLength = tp.Precision;
			  src.fieldList.get(i).offset = ofs;
			  src.fieldList.get(i).physicalOffset = pofs;
			  src.fieldList.get(i).KeyType = "NOT A KEY";
			  //
			  ofs += tp.Length;
			  pofs += tp.PhysicalLength;
			  //
			  tp = null;
			}
			return true;
	}
	
	//----------------------------------------------------------------	
	private boolean applyDefaultFlatFileSettings( infaSource src)
	//----------------------------------------------------------------
	{
		src.BusinessName  = "";
		src.Databasetype  = "Flat File";
		src.Description   = "Flat File derived from table " + src.Name;
		src.Dbdname       = src.Dbdname;
		src.Name          = "FF_" + src.Name;
		src.ObjectVersion = "1";
		src.OwnerName     = "";
		src.VersionNumber = "1";
		//
		src.flafle.CodePage   = gConst.getConstantValueFor("FF_Codepage"); if (src.flafle.CodePage == null ) return false;
        src.flafle.Consecdelimiterasone = gConst.getConstantValueFor("FF_Consecdelimiterasone"); if (src.flafle.Consecdelimiterasone == null) return false;
        src.flafle.Delimited  = gConst.getConstantValueFor("FF_Delimited"); if (src.flafle.Delimited == null) return false;
        src.flafle.Delimiters = gConst.getConstantValueFor("FF_Delimiters"); if (src.flafle.Delimiters == null) return false;
        src.flafle.EscapeCharacter = gConst.getConstantValueFor("FF_EscapeCharacter"); if (src.flafle.EscapeCharacter == null) return false;
        src.flafle.Keepescapechar = gConst.getConstantValueFor("FF_Keepescapechar"); if (src.flafle.Keepescapechar == null) return false;
        src.flafle.LineSequential = gConst.getConstantValueFor("FF_LineSequential"); if (src.flafle.LineSequential == null) return false;
        src.flafle.Multidelimitersasand = gConst.getConstantValueFor("FF_Multidelimitersasand"); if (src.flafle.Multidelimitersasand == null) return false;
        src.flafle.NullCharType = gConst.getConstantValueFor("FF_NullCharType"); if (src.flafle.NullCharType == null) return false;
        src.flafle.Nullcharacter = gConst.getConstantValueFor("FF_Nullcharacter"); if (src.flafle.Nullcharacter == null) return false;
        src.flafle.Padbytes = gConst.getConstantValueFor("FF_Padbytes"); if (src.flafle.Padbytes  == null) return false;
        src.flafle.QuoteCharacter = gConst.getConstantValueFor("FF_QuoteCharacter"); if (src.flafle.QuoteCharacter == null) return false;
        src.flafle.Repeatable = gConst.getConstantValueFor("FF_Repeatable"); if (src.flafle.Repeatable == null) return false;
        src.flafle.RowDelimiter = gConst.getConstantValueFor("FF_RowDelimiter"); if (src.flafle.RowDelimiter == null) return false;
        src.flafle.ShiftSensitiveData = gConst.getConstantValueFor("FF_ShiftSensitiveData"); if (src.flafle.ShiftSensitiveData == null) return false;
        src.flafle.Skiprows = gConst.getConstantValueFor("FF_Skiprows"); if (src.flafle.Skiprows == null) return false;
        src.flafle.Striptrailingblanks = gConst.getConstantValueFor("FF_Striptrailingblanks"); if (src.flafle.Striptrailingblanks == null) return false;
        //
        for(int i=0;i<10;i++)
        {
              infaPair pp = null;
              if( src.tipe == readInfaXML.ParseType.SOURCEFLATFILE ) 
              {
              switch( i)
              {
              case 0 : {pp = new infaPair("Base Table Name",gConst.getConstantValueFor("FTA_Base_Table_Name")); if (pp.value == null) return false; break; }
              case 1 : {pp = new infaPair("Search Specification",gConst.getConstantValueFor("FTA_Search_Specification")); if (pp.value == null) return false; ; break; }
              case 2 : {pp = new infaPair("Sort Specification", gConst.getConstantValueFor("FTA_Sort_Specification")); if (pp.value == null) return false; break; }
              case 3 : {pp = new infaPair("Datetime Format",gConst.getConstantValueFor("FTA_Datetime_Format")); if (pp.value == null) return false; ; break; }
              case 4 : {pp = new infaPair("Thousand Separator",gConst.getConstantValueFor("FTA_Thousand_Separator")); if (pp.value == null) return false; ; break; }
              case 5 : {pp = new infaPair("Decimal Separator",gConst.getConstantValueFor("FTA_Decimal_Separator")); if (pp.value == null) return false; ; break; }
              default : continue;
              }
              }
              if( src.tipe == readInfaXML.ParseType.TARGETFLATFILE ) 
              {
              switch( i)
              {
              case 3 : {pp = new infaPair("Datetime Format",gConst.getConstantValueFor("FTA_Datetime_Format")); if (pp.value == null) return false; break; }
              case 4 : {pp = new infaPair("Thousand Separator",gConst.getConstantValueFor("FTA_Thousand_Separator")); if (pp.value == null) return false; break; }
              case 5 : {pp = new infaPair("Decimal Separator",gConst.getConstantValueFor("FTA_Decimal_Separator")); if (pp.value == null) return false; break; }
              case 6 : {pp = new infaPair("Line Endings",gConst.getConstantValueFor("FTA_Line_Endings")); if (pp.value == null) return false; break; }
              default : continue;
              }
              }
            src.tableAttributeList.add(pp);
        }
        return true;
		
		
	}
	
}
