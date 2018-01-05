package pcGenerator.ddl;


import java.util.ArrayList;

import generalpurpose.gpPrintStream;
import generalpurpose.pcDevBoosterSettings;
import pcGenerator.powercenter.infaConstraint;
import pcGenerator.powercenter.infaSource;
import pcGenerator.powercenter.infaSourceField;
import pcGenerator.powercenter.infaSourceKloon;

// KB 15 Dec - fixed FIELDPROPERTY="0" error on Source Flat FIle

public class writeInfaMetadata {
	
	pcDevBoosterSettings xMSet = null;
	String sProjectFolder = null;
	String ImportSourceFileName = null;  // just to put in the export file header
	infaSourceKloon kloner = null;
	private boolean DEBUG = false;
	
	//----------------------------------------------------------------
	public writeInfaMetadata(pcDevBoosterSettings xi , String fi)
	//----------------------------------------------------------------
	{
		xMSet = xi;
		ImportSourceFileName = fi;
		kloner = new infaSourceKloon();
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
	private String getDBUID(String db , String dt)
	//----------------------------------------------------------------
	{
			String sOne = db; if( sOne == null ) sOne = "unknown";
			String sTwo = dt; if( sTwo == null ) sTwo = "unknown";
			String dbUID = (sOne.trim() + "-" + sTwo.trim()).toLowerCase();
			dbUID = xMSet.xU.Remplaceer( dbUID , " " , "_" );
			return dbUID;
	}
		
	//----------------------------------------------------------------
	private ArrayList<String> getDBNameList(ArrayList<infaSource>src)
	//----------------------------------------------------------------
	{
			ArrayList<String> list = new ArrayList<String>();
			for(int i=0;i<src.size();i++)
			{
		        String dbUID = getDBUID( src.get(i).Dbdname , src.get(i).Databasetype );
				boolean found = false;
				for(int j=0;j<list.size();j++)
				{
					if( list.get(j).compareToIgnoreCase( dbUID ) == 0 ) {
						found = true;
						break;
					}
				}
				if( found == false ) list.add(dbUID);
			}
			return list;
	}
	
	//----------------------------------------------------------------
	public boolean createMetadataFiles(ArrayList<infaSource> srcList)
	//----------------------------------------------------------------
	{
			   // Check tipes
		       for(int i=0 ; i<srcList.size() ; i++)
		       {
		    	 if( srcList.get(i).tipe == readInfaXML.ParseType.SOURCETABLE ) continue;
		    	 if( srcList.get(i).tipe == readInfaXML.ParseType.SOURCEFLATFILE ) continue;
		    	 if( srcList.get(i).tipe == readInfaXML.ParseType.TARGETTABLE ) continue;
		    	 if( srcList.get(i).tipe == readInfaXML.ParseType.TARGETFLATFILE ) continue;
		    	 if( srcList.get(i).tipe == readInfaXML.ParseType.POWERDESIGNERDDL ) continue;
				 errit("(createMetadataFiles) Got an unsupported tipe [" + srcList.get(i).tipe  + "] on source list item [" + srcList.get(i).Name + "]");
				 return false;
		       }
		       // extract the different databasename
			   ArrayList<String> dbnameList = getDBNameList(srcList);
		       if( DEBUG ) { for(int i=0;i<dbnameList.size();i++) logit(9,"-->" + dbnameList.get(i)); }
			   
		       // create fileName
			   for( int z=0;z<dbnameList.size();z++)
			   {
				   String dbUID = dbnameList.get(z);
				   boolean isOK=true;
				   for(int k=0;k<5;k++)
				   {
					switch( k )
					{
					case 0 : { isOK=writeTable(srcList , dbUID , readInfaXML.ParseType.POWERDESIGNERDDL , null); break; }
					case 1 : { isOK=writeTable(srcList , dbUID , readInfaXML.ParseType.SOURCETABLE , null ); break; }
					case 2 : { isOK=writeTable(srcList , dbUID , readInfaXML.ParseType.SOURCEFLATFILE , null); break; }
					case 3 : { isOK=writeTable(srcList , dbUID , readInfaXML.ParseType.TARGETTABLE , null); break; }
					case 4 : { isOK=writeTable(srcList , dbUID , readInfaXML.ParseType.TARGETFLATFILE , null); break; }
					}
					if( isOK == false ) return false;
				   }
			   }
		       return true;
	}
	
	//----------------------------------------------------------------
	private String calculateFileName(String dbUID ,  readInfaXML.ParseType tipe)
	//----------------------------------------------------------------
	{
		    String sFolder = null;
	        if( (tipe == readInfaXML.ParseType.SOURCETABLE) || (tipe == readInfaXML.ParseType.SOURCEFLATFILE) ) sFolder = "Sources";
	        if( (tipe == readInfaXML.ParseType.TARGETTABLE) || (tipe == readInfaXML.ParseType.TARGETFLATFILE) ) sFolder = "Targets";
	        if( tipe == readInfaXML.ParseType.POWERDESIGNERDDL ) sFolder = "Ddl";
			if( sFolder == null ) {
				errit("Unsupported tipe [" + tipe + "] in writeInfaMetadata.writeTable");
				return null;
			}
			String sShort = dbUID.toLowerCase();
		    if( (tipe == readInfaXML.ParseType.SOURCEFLATFILE) || (tipe == readInfaXML.ParseType.TARGETFLATFILE) ) {
		    	sShort = "FlatFile-" + sShort;
		    	sShort = xMSet.xU.Remplaceer(sShort,"-flat_file","");
		    }
		    // look for occurence of name and inrement counter
		    String sDestFolder = xMSet.getProjectDir() + xMSet.xU.ctSlash + sFolder;
		    ArrayList<String> list = xMSet.xU.GetFilesInDir(sDestFolder,null);
		    int counter=1;  // start
		    for(int i=0;i<list.size();i++)
		    {
		    	String ss = list.get(i).trim().toLowerCase();
		    	if( ss.startsWith( sShort.toLowerCase() + "-0" ) == true ) counter++;
	            //System.out.println( "-----> " + ss + " " + sShort +"-0" + "     " + ss.startsWith( sShort + "-0" ) + " " + counter);
		    }
		    sShort = sShort + "-" + String.format("%03d", counter );
		    String sTemp = sDestFolder + xMSet.xU.ctSlash + sShort + ".xml";
		    if( xMSet.xU.IsBestand( sTemp )) {
		    	errit("System error - writeMetadata - calculated file exists [" + sTemp + "] " + dbUID);
		    	return null;
		    }
		    return sTemp;
	}
	
	
	//----------------------------------------------------------------
	private boolean writeTable( ArrayList<infaSource> srcList , String dbUID ,  readInfaXML.ParseType tipe , String FNameOverrule)
	//----------------------------------------------------------------
	{
        String FName = FNameOverrule;
        if( FName == null ) FName = calculateFileName( dbUID , tipe);
	    if( FName == null ) return false;
	    //logit( 9 , "Writing to [" + FName + "]");
	    //
	    String sSrcTgtTag=null;
	    String sSrcTgtFieldTag=null;
	    String sSrcTgtEndTag=null;
	    switch ( tipe )
	    {
	    case POWERDESIGNERDDL : { sSrcTgtTag = "<POWERDESIGNER"; sSrcTgtFieldTag = "<FIELD"; sSrcTgtEndTag = "</POWERDESIGNER>"; break; }
	    case SOURCEFLATFILE   : ;
	    case SOURCETABLE : { sSrcTgtTag = "<SOURCE"; sSrcTgtFieldTag = "<SOURCEFIELD"; sSrcTgtEndTag = "</SOURCE>"; break; }
	    case TARGETFLATFILE : ;
	    case TARGETTABLE : { sSrcTgtTag = "<TARGET"; sSrcTgtFieldTag = "<TARGETFIELD"; sSrcTgtEndTag = "</TARGET>"; break; }
	    default : {
	    	errit("(writeTable) System error - unknown tipe [" + tipe + "]");
	    	return false;
	       }
	    }
	    //
	    gpPrintStream fout = null;
	    int teller = 0;
	    for(int i=0;i<srcList.size();i++)
		{
	      if ( srcList.get(i).tipe != tipe ) continue;
	      // if not the same database and type continue
	      String currDbUID = getDBUID( srcList.get(i).Dbdname , srcList.get(i).Databasetype );
	      if( currDbUID.compareToIgnoreCase( dbUID ) != 0 ) continue;
	      //
	      // op die wijze kan je de attribs wijzigen naar defaults
	      infaSource src = kloner.kloon( srcList.get(i) );
	      //
	      if( fout == null ) {
	    	  fout = new gpPrintStream( FName , "UTF-8" );
	          fout.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	          fout.println("<!-- Application : " + xMSet.getApplicationId() + "-->");
		      fout.println("<!-- Source      : " + ImportSourceFileName + " -->");
		      fout.println("<!-- Project     : " + xMSet.getCurrentProject() + " -->");
		      fout.println("<!-- DBName      : " + src.Dbdname + " -->");
		      fout.println("<!-- DBType      : " + src.Databasetype + " -->");
		      fout.println("<!-- Created on  : " + xMSet.xU.prntStandardDateTime(System.currentTimeMillis()) + " -->");
		      fout.println("<!-- Created by  : " + xMSet.whoami() + " -->");
		      fout.println("<InfoSet>" );
	      }
	    
	      // main source/target Header
	      String sLijn = "<!-- STRT [" + src.Dbdname + "][" + src.Databasetype + "][" + src.OwnerName + "][" + src.Name  + "] - [" + xMSet.xU.prntDateTime(System.currentTimeMillis(),"yyyyMMddHHmmss") + "] -->";
	      fout.println(sLijn);
	      teller++;
	      //
	      sLijn = assembleSourceTargetHeader( src );
	      fout.println( sSrcTgtTag + sLijn + ">");
	      // ? FLATFILE
	      if( (tipe == readInfaXML.ParseType.SOURCEFLATFILE) || (tipe == readInfaXML.ParseType.TARGETFLATFILE) ) {
	    	  sLijn = assembleFlatFileHeader( src );
	    	  fout.println( "  <FLATFILE" + sLijn + "/>");
	    	  //    <TABLEATTRIBUTE NAME ="Datetime Format" VALUE ="A  19 mm/dd/yyyy hh24:mi:ss"/>
	    	  for(int j=0;j<src.tableAttributeList.size();j++)
	    	  {
	    		  sLijn = "   <TABLEATTRIBUTE NAME =\"" + src.tableAttributeList.get(j).code + "\" VALUE =\"" + src.tableAttributeList.get(j).value + "\"/>";
	    		  fout.println(sLijn);
	    	  }
	      }
	      // Field
	      for(int j=0;j<src.fieldList.size();j++)
	      {
	    	  // KB 15 DEC - Source Flat Files moeten FIELDPROPERTY="0" hebben lege sring werkt nie tbij impor t- geen idee waarom
	    	  if( tipe == readInfaXML.ParseType.SOURCEFLATFILE ) {
	    		  if( src.fieldList.get(j).FieldProperty == null ) src.fieldList.get(j).FieldProperty = "0";
	    		  if( src.fieldList.get(j).FieldProperty.trim().length()==0 ) src.fieldList.get(j).FieldProperty = "0";
	    	  }
	    	  sLijn = assembleSourceFieldLine( src.fieldList.get(j) );
	    	  fout.println("   " + sSrcTgtFieldTag + sLijn + "/>" );
	      }
	      // KB 16 SEP  FOREIGN KEYS
	      if( (tipe == readInfaXML.ParseType.SOURCETABLE) || (tipe == readInfaXML.ParseType.TARGETTABLE) ) {
	    	  for(int j=0;j<src.constraintList.size();j++)
	    	  {
	    	  sLijn = assembleConstraintLine( src.constraintList.get(j) );
	    	  fout.println("   " + sLijn );
	    	  }
	      }
	      //
	      fout.println(sSrcTgtEndTag);
	      // garbage collector
	      src = null;
	   }
	    // end
	    if (fout != null ) {
	    	fout.println("</InfoSet>" );
	    	fout.close();
	    }
	    if( teller > 0 ) logit( 1 , "Exported [" + teller + "] to [" + FName + "]");
	    return true;
	}
	
	//----------------------------------------------------------------
	private String mkNumPart(int ival , String sCode)
	//----------------------------------------------------------------
	{
			if( ival < 0 ) return "";
			return " " + sCode.trim().toUpperCase() + "=\"" + ival + "\"";
	}
	
	//----------------------------------------------------------------
	private String mkPart(String sVal , String sCode)
	//----------------------------------------------------------------
	{
		if( sVal == null ) return "";
		return " " + sCode.trim().toUpperCase() + "=\"" + sVal + "\"";
	}
	
	//----------------------------------------------------------------
	private String assembleSourceTargetHeader( infaSource src )
	//----------------------------------------------------------------
	{
		String sRet = "";
		sRet += mkPart(src.BusinessName , readInfaXML.SOURCE_ATTRIBS.BUSINESSNAME.toString());
		sRet += mkPart(src.Constraint   , readInfaXML.SOURCE_ATTRIBS.CONSTRAINT.toString());
		sRet += mkPart(src.Databasetype , readInfaXML.SOURCE_ATTRIBS.DATABASETYPE.toString());
		sRet += mkPart(src.Dbdname      , readInfaXML.SOURCE_ATTRIBS.DBDNAME.toString());
		sRet += mkPart(src.Description  , readInfaXML.SOURCE_ATTRIBS.DESCRIPTION.toString());
		sRet += mkPart(src.Name         , readInfaXML.SOURCE_ATTRIBS.NAME.toString());
		sRet += mkPart(src.ObjectVersion, readInfaXML.SOURCE_ATTRIBS.OBJECTVERSION.toString());
		sRet += mkPart(src.OwnerName    , readInfaXML.SOURCE_ATTRIBS.OWNERNAME.toString());
		sRet += mkPart(src.TableOptions , readInfaXML.SOURCE_ATTRIBS.TABLEOPTIONS.toString());
		sRet += mkPart(src.VersionNumber, readInfaXML.SOURCE_ATTRIBS.VERSIONNUMBER.toString());
		return sRet;
	}
		
	//----------------------------------------------------------------
	private String assembleFlatFileHeader( infaSource src )
	//----------------------------------------------------------------
	{
			String sRet = "";
			sRet += mkPart(src.flafle.CodePage            , readInfaXML.FLATF_ATTRIBS.CODEPAGE.toString());
			sRet += mkPart(src.flafle.Consecdelimiterasone, readInfaXML.FLATF_ATTRIBS.CONSECDELIMITERSASONE.toString());
			sRet += mkPart(src.flafle.Delimited           , readInfaXML.FLATF_ATTRIBS.DELIMITED.toString());
			sRet += mkPart(src.flafle.Delimiters          , readInfaXML.FLATF_ATTRIBS.DELIMITERS.toString());
			sRet += mkPart(src.flafle.EscapeCharacter     , readInfaXML.FLATF_ATTRIBS.ESCAPE_CHARACTER.toString());
			sRet += mkPart(src.flafle.Keepescapechar      , readInfaXML.FLATF_ATTRIBS.KEEPESCAPECHAR.toString());
			sRet += mkPart(src.flafle.LineSequential      , readInfaXML.FLATF_ATTRIBS.LINESEQUENTIAL.toString());
			sRet += mkPart(src.flafle.Multidelimitersasand, readInfaXML.FLATF_ATTRIBS.MULTIDELIMITERSASAND.toString());
			sRet += mkPart(src.flafle.NullCharType        , readInfaXML.FLATF_ATTRIBS.NULLCHARTYPE.toString());
			sRet += mkPart(src.flafle.Nullcharacter       , readInfaXML.FLATF_ATTRIBS.NULL_CHARACTER.toString());
			sRet += mkPart(src.flafle.Padbytes            , readInfaXML.FLATF_ATTRIBS.PADBYTES.toString());
			sRet += mkPart(src.flafle.QuoteCharacter      , readInfaXML.FLATF_ATTRIBS.QUOTE_CHARACTER.toString());
			sRet += mkPart(src.flafle.Repeatable          , readInfaXML.FLATF_ATTRIBS.REPEATABLE.toString());
			sRet += mkPart(src.flafle.RowDelimiter        , readInfaXML.FLATF_ATTRIBS.ROWDELIMITER.toString());
			sRet += mkPart(src.flafle.ShiftSensitiveData  , readInfaXML.FLATF_ATTRIBS.SHIFTSENSITIVEDATA.toString());
			sRet += mkPart(src.flafle.Skiprows            , readInfaXML.FLATF_ATTRIBS.SKIPROWS.toString());
			sRet += mkPart(src.flafle.Striptrailingblanks , readInfaXML.FLATF_ATTRIBS.STRIPTRAILINGBLANKS.toString());
			return sRet;
	}	
	
	//----------------------------------------------------------------
	private String assembleSourceFieldLine( infaSourceField fld )
	//----------------------------------------------------------------
	{
		    String snul = "NULL"; 
		    if ( fld.mandatory ) snul = "NOTNULL";
		    //
			String sRet = "";
			sRet += mkPart( fld.BusinessName      , readInfaXML.FIELD_ATTRIBS.BUSINESSNAME.toString());
			sRet += mkPart( fld.DataType          , readInfaXML.FIELD_ATTRIBS.DATATYPE.toString());
			sRet += mkPart( xMSet.xU.transformToXMLEscape(fld.Description) , readInfaXML.FIELD_ATTRIBS.DESCRIPTION.toString());  // KB 01 DEC
			sRet += mkNumPart( fld.fieldNumber    , readInfaXML.FIELD_ATTRIBS.FIELDNUMBER.toString());
			sRet += mkPart( fld.FieldProperty     , readInfaXML.FIELD_ATTRIBS.FIELDPROPERTY.toString());
			sRet += mkPart( fld.FieldType         , readInfaXML.FIELD_ATTRIBS.FIELDTYPE.toString());
			sRet += mkPart( fld.Hidden            , readInfaXML.FIELD_ATTRIBS.HIDDEN.toString());
			sRet += mkPart( fld.isFileNameField   , readInfaXML.FIELD_ATTRIBS.ISFILENAMEFIELD.toString());
			sRet += mkPart( fld.KeyType           , readInfaXML.FIELD_ATTRIBS.KEYTYPE.toString());
			sRet += mkNumPart( fld.Length         , readInfaXML.FIELD_ATTRIBS.LENGTH.toString());
			sRet += mkNumPart( fld.Level          , readInfaXML.FIELD_ATTRIBS.LEVEL.toString());
			sRet += mkPart( snul                  , readInfaXML.FIELD_ATTRIBS.NULLABLE.toString());
			sRet += mkPart( fld.Name              , readInfaXML.FIELD_ATTRIBS.NAME.toString());
			sRet += mkNumPart( fld.Occurs         , readInfaXML.FIELD_ATTRIBS.OCCURS.toString());
			sRet += mkNumPart( fld.offset         , readInfaXML.FIELD_ATTRIBS.OFFSET.toString());
			sRet += mkNumPart( fld.physicalLength , readInfaXML.FIELD_ATTRIBS.PHYSICALLENGTH.toString());
			sRet += mkNumPart( fld.physicalOffset , readInfaXML.FIELD_ATTRIBS.PHYSICALOFFSET.toString());
			sRet += mkPart( fld.PictureText       , readInfaXML.FIELD_ATTRIBS.PICTURETEXT.toString());
			sRet += mkNumPart( fld.Precision      , readInfaXML.FIELD_ATTRIBS.PRECISION.toString());
			sRet += mkPart( fld.referencedField   , readInfaXML.FIELD_ATTRIBS.REFERENCEDFIELD.toString());
			sRet += mkPart( fld.referencedTable   , readInfaXML.FIELD_ATTRIBS.REFERENCEDTABLE.toString());
			sRet += mkNumPart( fld.scale          , readInfaXML.FIELD_ATTRIBS.SCALE.toString());
			sRet += mkPart( fld.UsageFlags        , readInfaXML.FIELD_ATTRIBS.USAGE_FLAGS.toString());
			return sRet;
	}		
	
	//----------------------------------------------------------------
	private String assembleConstraintLine( infaConstraint co)
	//----------------------------------------------------------------
	{
		String sLijn = "<CONSTRAINT>";
		sLijn += "<CNSTRNAME>" + co.Name + "</CNSTRNAME>";
		sLijn += "<CNSTRTYPE>" + co.Tipe + "</CNSTRTYPE>";
		if( co.key_list.size() > 0 ) {  // ?? always
			sLijn += "<CNSTRKEYS>";
			for(int i=0;i<co.key_list.size();i++)
			{
	    	  sLijn += "<KEY>" + co.key_list.get(i) + "</KEY>";
			}
			sLijn += "</CNSTRKEYS>";
		}
		if( co.ref_list.size() > 0 ) {  // ?? always
			sLijn += "<CNSTRFOREIGN>";
			sLijn += ("<OWNERREFERENCED>" + co.ReferencedOwner +  "</OWNERREFERENCED>").toUpperCase();  // voor null
			sLijn += ("<TABLEREFERENCED>" + co.ReferencedTableName + "</TABLEREFERENCED>").toUpperCase();
			sLijn += "<REFKEYS>";
			for(int i=0;i<co.ref_list.size();i++)
			{
	    	  sLijn += "<REFKEY>" + co.ref_list.get(i) + "</REFKEY>";
			}
			sLijn += "</REFKEYS>";
			sLijn += "</CNSTRFOREIGN>";
		}
	    sLijn += "</CONSTRAINT>";			
		return sLijn;
	}
}
