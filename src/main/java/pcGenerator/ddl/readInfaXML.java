package pcGenerator.ddl;

import generalpurpose.gpStringUtils;
import generalpurpose.pcDevBoosterSettings;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import pcGenerator.generator.generatorConstants;
import pcGenerator.powercenter.infaConstraint;
import pcGenerator.powercenter.infaPair;
import pcGenerator.powercenter.infaSource;
import pcGenerator.powercenter.infaSourceField;



public class readInfaXML {

	pcDevBoosterSettings xMSet = null;
	
	public enum ParseType { UNKNOWN , SOURCETABLE, TARGETTABLE, SOURCEFLATFILE, TARGETFLATFILE , POWERDESIGNERDDL }
	
	enum SOURCE_ATTRIBS { BUSINESSNAME, DATABASETYPE, DBDNAME, DESCRIPTION, NAME, OBJECTVERSION, OWNERNAME, VERSIONNUMBER , CONSTRAINT , TABLEOPTIONS }

	enum FIELD_ATTRIBS { BUSINESSNAME, DATATYPE, DESCRIPTION, FIELDNUMBER, FIELDPROPERTY, 
		                 FIELDTYPE, HIDDEN, KEYTYPE, LENGTH, LEVEL, NAME, 
		                 NULLABLE, OCCURS, OFFSET, PHYSICALLENGTH, PHYSICALOFFSET, 
		                 PICTURETEXT, PRECISION, SCALE, USAGE_FLAGS,
		                 REFERENCEDFIELD , REFERENCEDTABLE , ISFILENAMEFIELD }
	enum FLATF_ATTRIBS {
		CODEPAGE, CONSECDELIMITERSASONE, DELIMITED, DELIMITERS, ESCAPE_CHARACTER, KEEPESCAPECHAR,
		LINESEQUENTIAL, MULTIDELIMITERSASAND, NULLCHARTYPE, NULL_CHARACTER, PADBYTES,
		QUOTE_CHARACTER, REPEATABLE, ROWDELIMITER, SHIFTSENSITIVEDATA, SKIPROWS, STRIPTRAILINGBLANKS
	}
	
	//----------------------------------------------------------------
	private SOURCE_ATTRIBS getSourceToken(String s)
	//----------------------------------------------------------------
	{
		for(int i=0;i<SOURCE_ATTRIBS.values().length;i++)
		{
			if( SOURCE_ATTRIBS.values()[i].toString().compareToIgnoreCase(s) == 0 ) return SOURCE_ATTRIBS.values()[i];
		}
		return null;
	}
	//----------------------------------------------------------------
	private FIELD_ATTRIBS getFieldToken(String s)
	//----------------------------------------------------------------
	{
		for(int i=0;i<FIELD_ATTRIBS.values().length;i++)
		{
			if( FIELD_ATTRIBS.values()[i].toString().compareToIgnoreCase(s) == 0 ) return FIELD_ATTRIBS.values()[i];
		}
		return null;
	}
	//----------------------------------------------------------------
	private FLATF_ATTRIBS getFlatFToken(String s)
	//----------------------------------------------------------------
	{
		for(int i=0;i<FLATF_ATTRIBS.values().length;i++)
		{
			if( FLATF_ATTRIBS.values()[i].toString().compareToIgnoreCase(s) == 0 ) return FLATF_ATTRIBS.values()[i];
		}
		return null;
	}
	
	
	private String SourceFileName = null;
	infaSource srcWork = null;
	ArrayList<infaSource> srcList = null;
	
	gpStringUtils strul = new gpStringUtils();
	
	
	//----------------------------------------------------------------
	public readInfaXML(pcDevBoosterSettings im)
	//----------------------------------------------------------------
	{
		xMSet = im;
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
	public ArrayList<infaSource> parse_Export(String FInName , ParseType tipe , String lookForTableName)
	//----------------------------------------------------------------
	{
			logit( 5 ,"Request for extracting [Tipe=" + tipe + "] [Table=" + lookForTableName + "] in file[" + FInName + "]");
			
			//
			SourceFileName = FInName;
			//
			if( srcList != null ) srcList = null;
			srcList = new ArrayList<infaSource>();	
		    //	  
			boolean isOK=true;
			String ENCODING = "UTF-8";
			try {
				  BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(FInName),ENCODING));
		       	  String sLijn = null;
		       	  boolean inSourceTargetBlock = false;
		      	  String sStartSrcTgt = null;
        	  	  String sEndSrcTgt   = null;
        	  	  String sSrcTgtField = null;
        	  	  ParseType superTipe = null;
                 
        	  	  String sConstraint = "";
        	  	  boolean inConstraint = false;
		          while ((sLijn=reader.readLine()) != null) {
		        	  
		        	  	  switch( tipe )
		        	  	  {
		        	   		 case SOURCEFLATFILE : ; // overflow
			           		 case SOURCETABLE : {
			        	      sStartSrcTgt = "<SOURCE ";
				        	  sEndSrcTgt   = "</SOURCE>";
				        	  sSrcTgtField  = "<SOURCEFIELD ";
				        	  superTipe    = ParseType.SOURCETABLE;
				        	  break;
			           		 }
			           		 case TARGETFLATFILE : ; // overflow
			           		 case TARGETTABLE : {
			        	      sStartSrcTgt = "<TARGET ";
				        	  sEndSrcTgt   = "</TARGET>";
				        	  sSrcTgtField  = "<TARGETFIELD ";
				        	  superTipe    = ParseType.TARGETTABLE;
				        	  break;
			           		 }
			           		 default : {
			        		  errit("(parse_export) system error : unknown tipe");
			        		  isOK = false;
			        		  break;
			           		 }
		        	  	  }
		        	      //
		        	      if( sLijn.indexOf(sStartSrcTgt) >= 0 ) {
		         //System.out.println(" ==> Source :" + sLijn.substring(0,80) );
		        			  isOK = extract_Parent(sLijn,superTipe);
		         //System.out.println("      ==> Source :" + isOK );
		        			  if( isOK == false ) break;
		        			  inSourceTargetBlock = true;
		        		  }
		        	      if( inSourceTargetBlock )
		        	      {
   	    	    //System.out.println(" ==>    indexOf FLATFILE " + sLijn.indexOf("<FLATFILE ")  );
		        		   if( sLijn.indexOf("<FLATFILE ") >= 0 ) {  
		        			  if( (tipe == readInfaXML.ParseType.SOURCEFLATFILE) || (tipe == readInfaXML.ParseType.TARGETFLATFILE) ) {
		        //System.out.println(" ==> Flatfile :" + srcWork.Name);
		        				  isOK = extract_FlatFileHeader(sLijn,tipe);
		        				  if( isOK == false ) {
		        					  srcWork=null;
		        					  break;
		        				  }
		        				  // overrule tipe naar Flatfile
		        				  srcWork.tipe = tipe;
		        			  }
		        			  else {  // In FLATFILE BLOCK doch geen FLATFILE gevraagd - ga uit SourceBlock ttz sla dit blok over
		        				inSourceTargetBlock = false;
		        				srcWork = null;
		        			  }
		        		   }
		        		   if( sLijn.indexOf("<TABLEATTRIBUTE ") >= 0 ) {  
		        			  if( srcWork == null ) continue;
		        			  if( (srcWork.tipe == readInfaXML.ParseType.SOURCETABLE) || (srcWork.tipe == readInfaXML.ParseType.TARGETTABLE) ) continue;
		        			  isOK = extract_tableAttribute(sLijn);
		                      if( isOK == false ) break;
		        		   }
		        		   //  <SOURCEFIELD  <TARGETFIELD
		        		   if( sLijn.indexOf(sSrcTgtField) >= 0 ) {
		        			   isOK = extract_Field(sLijn,superTipe);
		        			   if( isOK == false ) break;
		        		   }
		        		   // Constraints
		        		   if( sLijn.indexOf("<CONSTRAINT>") >= 0 ) {
		        			   inConstraint=true;
		        			   sConstraint = "";
		        		   }
		        		   if( inConstraint ) {
		        			   sConstraint += sLijn;
		        			   if( sLijn.indexOf("</CONSTRAINT>") >= 0 ) {
		        				   inConstraint=false;
		        				   isOK = extract_Constraint( sConstraint );
		        				   if( isOK == false ) break;
		        			   }
		        		   }
		        		   
		        	      }  // end of source target block
		        	      
		        	      // </SOURCE>  or </TARGET>
		        		  if( sLijn.indexOf(sEndSrcTgt) >= 0 ) {
		        			  inSourceTargetBlock = false;
		        			  if( srcWork != null ) {
		       	  //System.out.println("Parsed [" + srcWork.Name + "] - Looking for=" + lookForTableName );
		        				  if( srcWork.tipe == tipe ) {
		        					  if( lookForTableName != null  ) {
		        						if( lookForTableName.trim().compareToIgnoreCase( srcWork.Name.trim() ) == 0 ) srcList.add(srcWork);
		        					  }
		        					  else srcList.add(srcWork);
		        				  }
		        			  }
		        			  srcWork=null;
		        		  }
		        	   
		        	  if( isOK == false ) break;
		        	  //System.out.println( String.format("%05d", lineCounter) + " " + sLijn );
		          }
		          reader.close();
		          
		          logit( 5 , FInName + " " + tipe + " " + srcList.size());
			} 
			catch (Exception e) {
				 errit("Error reading [" + FInName  + "] " + e.getMessage() );
				 e.printStackTrace();
			}
			if( isOK == false ) return null;
			
			
			// er mogen geen Name null voorkomen
			if( srcList.size() > 0 ) 
			{
				   for(int i=0;i<srcList.size();i++)
				   {
					   if( srcList.get(i).Name == null ) {
						   isOK=false;
						   break;
					   }
					   for(int j=0;j<srcList.get(i).fieldList.size();j++)
					   {
						   if( srcList.get(i).fieldList.get(j).Name == null ) {
							   isOK = false;
							   break;
						   }
					   }
					   //
					   for(int j=0;j<srcList.get(i).tableAttributeList.size();j++)
					   {
						   if( srcList.get(i).tableAttributeList.get(j).code == null ) {
							   errit("TableAttribute Code is Null for idx = " + i);
							   isOK = false;
							   break;
						   }
						   if( srcList.get(i).tableAttributeList.get(j).value == null ) {
							   errit("TableAttribute value is Null for idx = " + i);
							   isOK = false;
							   break;
						   }
					   }
				   }
			}
	        if( isOK == false ) return null;
	        //
			return srcList;
		}
		
		//----------------------------------------------------------------
		private String groom( String sIn )
		//----------------------------------------------------------------
		{
			String sTemp = sIn.trim();
			sTemp = strul.RemplaceerIgnoreCase(sTemp,"<SOURCE ","");
			sTemp = strul.RemplaceerIgnoreCase(sTemp,"<SOURCEFIELD ","");
			sTemp = strul.RemplaceerIgnoreCase(sTemp,"<TARGET ","");
			sTemp = strul.RemplaceerIgnoreCase(sTemp,"<FLATFILE ","");
			sTemp = strul.RemplaceerIgnoreCase(sTemp,"<TABLEATTRIBUTE ","");
		    if( sTemp.endsWith("/>") ) sTemp = sTemp.substring(0,sTemp.length()-2);
		    if( sTemp.endsWith(">") ) sTemp = sTemp.substring(0,sTemp.length()-1);
			return sTemp;
		}
		
		//----------------------------------------------------------------
		private boolean extract_Parent(String sLijn , ParseType tipe )
		//----------------------------------------------------------------
		{
			    boolean isSource = true;
			    if( (tipe == readInfaXML.ParseType.TARGETTABLE) || (tipe == readInfaXML.ParseType.TARGETFLATFILE) ) isSource=false;
			    //
			    String sTemp = groom(sLijn);
				ArrayList<infaPair> list = tokenize(sTemp);
				if( list == null ) return false;
				if( list.size() == 0 ) {
					errit("(extract_Parent) Empty element list after <SOURCE");
					return false;
				}
		//System.out.println(sTemp);
				srcWork = new infaSource(null,tipe);
				srcWork.BusinessName = null;
				srcWork.Databasetype = null;
				srcWork.Dbdname = null;
				srcWork.Description = null;
				srcWork.Name = null;
				srcWork.ObjectVersion = null;
				srcWork.OwnerName = null;
				srcWork.VersionNumber = null;
				srcWork.Constraint = null;
				srcWork.TableOptions = null;
				for(int i=0;i<list.size();i++) {
					readInfaXML.SOURCE_ATTRIBS att = getSourceToken(list.get(i).code);
					if( att == null ) {
						srcWork.Name = null;
						errit("(extract_Parent) Unsupported SourceHeader Code [" + list.get(i).code + "]");
						return false;
					}
					switch( att )
					{
					case BUSINESSNAME  : { srcWork.BusinessName = list.get(i).value; break; }
					case DATABASETYPE  : { srcWork.Databasetype = list.get(i).value; break; }
					case DBDNAME       : { srcWork.Dbdname = list.get(i).value; break;}
					case DESCRIPTION   : { srcWork.Description = list.get(i).value; break; }
					case NAME          : { srcWork.Name = list.get(i).value; break; }
					case OBJECTVERSION : { srcWork.ObjectVersion = list.get(i).value; break; }
					case OWNERNAME     : { srcWork.OwnerName = list.get(i).value; break; }
					case VERSIONNUMBER : { srcWork.VersionNumber = list.get(i).value; break; }
					case CONSTRAINT    : { srcWork.Constraint = list.get(i).value; break; }
					case TABLEOPTIONS  : { srcWork.TableOptions = list.get(i).value; break; }
					default : {
						srcWork.Name = null;
						errit("(extract_Parent)Unsupported SourceHeader Code [" + list.get(i).code + "]");
						return false;
					   }
					}
				}
				//
				int fouten=0;
				fouten += checkString(srcWork.BusinessName  ,"BusinessName",sLijn);
				fouten += checkString(srcWork.Databasetype  ,"Databasetype",sLijn);
				fouten += checkString(srcWork.Description   ,"Description",sLijn);
				fouten += checkString(srcWork.Name          ,"Name",sLijn);
				fouten += checkString(srcWork.ObjectVersion ,"ObjectVersion",sLijn);
				fouten += checkString(srcWork.VersionNumber ,"Versionnumber",sLijn);
				if( isSource == true ) {
					fouten += checkString(srcWork.Dbdname ,"DbdName",sLijn);
					fouten += checkString(srcWork.OwnerName ,"OwnerName",sLijn);
				}
				else {
				  fouten += checkString(srcWork.Constraint ,"Constraint",sLijn);
				  fouten += checkString(srcWork.TableOptions ,"TableOptions",sLijn);
				}
				//
				if( fouten > 0 ) {
					srcWork.Name = null;
					return false;
				}
			    return true;
		}
	
		
		//----------------------------------------------------------------
		private boolean extract_FlatFileHeader(String sLijn , ParseType tipe)
		//----------------------------------------------------------------
		{
				if ( srcWork == null ) {
					errit("System error - extract flat file - srcWork is NULL");
					return false;
				}
				if ( srcWork.Name == null ) {
					errit("System error - extract flat file - srcWork.NAME is NULL");
					return false;
				}
				boolean isSource=true;
		    	if( (tipe == readInfaXML.ParseType.TARGETTABLE) || (tipe == readInfaXML.ParseType.TARGETFLATFILE) ) isSource=false;
			
		       //
			   String sTemp = groom(sLijn);
				ArrayList<infaPair> list = tokenize(sTemp);
				if( list == null ) return false;
				if( list.size() == 0 ) {
					errit("Empty element list after <FLATFILE");
					return false;
				}
				//
				srcWork.flafle.CodePage             = null;
				srcWork.flafle.Consecdelimiterasone = null;
				srcWork.flafle.Delimited            = null;
				srcWork.flafle.Delimiters           = null;
				srcWork.flafle.EscapeCharacter      = null;
				srcWork.flafle.Keepescapechar       = null;
				srcWork.flafle.LineSequential       = null;
				srcWork.flafle.Multidelimitersasand = null;
				srcWork.flafle.Nullcharacter        = null;
				srcWork.flafle.NullCharType         = null;
				srcWork.flafle.Padbytes             = null;
				srcWork.flafle.QuoteCharacter       = null;
				srcWork.flafle.Repeatable           = null;
				srcWork.flafle.RowDelimiter         = null;
				srcWork.flafle.ShiftSensitiveData   = null;
				srcWork.flafle.Skiprows             = null;
				srcWork.flafle.Striptrailingblanks  = null;
				//
			    for(int i=0;i<list.size();i++) {
					//System.out.println(list.get(i).code + " " + list.get(i).value );
					readInfaXML.FLATF_ATTRIBS att = getFlatFToken(list.get(i).code);
					if( att == null ) {
						srcWork.Name = null;
						errit("System error Unsupport Flat File Header Code [" + list.get(i).code + "]");
						return false;
					}
					switch( att )
					{
					case CODEPAGE              : { srcWork.flafle.CodePage = list.get(i).value; break; }
					case CONSECDELIMITERSASONE : { srcWork.flafle.Consecdelimiterasone = list.get(i).value; break; }
					case DELIMITED             : { srcWork.flafle.Delimited = list.get(i).value; break; }
					case DELIMITERS            : { srcWork.flafle.Delimiters = list.get(i).value; break; }
					case ESCAPE_CHARACTER      : { srcWork.flafle.EscapeCharacter = list.get(i).value; break; }
					case KEEPESCAPECHAR        : { srcWork.flafle.Keepescapechar = list.get(i).value; break; }
					case LINESEQUENTIAL        : { srcWork.flafle.LineSequential = list.get(i).value; break; }
					case MULTIDELIMITERSASAND  : { srcWork.flafle.Multidelimitersasand = list.get(i).value; break; }
					case NULLCHARTYPE          : { srcWork.flafle.NullCharType = list.get(i).value; break; }
					case NULL_CHARACTER        : { srcWork.flafle.Nullcharacter = list.get(i).value; break; }
					case PADBYTES              : { srcWork.flafle.Padbytes = list.get(i).value; break; }
					case QUOTE_CHARACTER       : { srcWork.flafle.QuoteCharacter = list.get(i).value; break; }
					case REPEATABLE            : { srcWork.flafle.Repeatable = list.get(i).value; break; }
					case ROWDELIMITER          : { srcWork.flafle.RowDelimiter = list.get(i).value; break; }
					case SHIFTSENSITIVEDATA    : { srcWork.flafle.ShiftSensitiveData = list.get(i).value; break; }
					case SKIPROWS              : { srcWork.flafle.Skiprows = list.get(i).value; break; }
					case STRIPTRAILINGBLANKS   : { srcWork.flafle.Striptrailingblanks = list.get(i).value; break; }
					default : {
						srcWork.Name = null;
						errit("(extract_FlatFileHeader) Unsupported Flat File Header Code [" + list.get(i).code + "]");
						return false;
					   }
					}
				}
				//
				int fouten=0;
				fouten += checkString(srcWork.flafle.CodePage ,"Codepage",sLijn);
				fouten += checkString(srcWork.flafle.Consecdelimiterasone  ,"Consecdelimiterasone",sLijn);
				fouten += checkString(srcWork.flafle.Delimited       ,"Delimited",sLijn);
				fouten += checkString(srcWork.flafle.Delimiters      ,"Delimiters",sLijn);
				fouten += checkString(srcWork.flafle.EscapeCharacter ,"EscapeCharacter",sLijn);
				fouten += checkString(srcWork.flafle.Keepescapechar ,"Keepescapechar",sLijn);
				fouten += checkString(srcWork.flafle.LineSequential  ,"LineSequential",sLijn);
				fouten += checkString(srcWork.flafle.Multidelimitersasand,"Multidelimitersasand",sLijn);
				fouten += checkString(srcWork.flafle.Nullcharacter ,"Nullcharacter",sLijn);
				fouten += checkString(srcWork.flafle.NullCharType  ,"NullCharType",sLijn);
				fouten += checkString(srcWork.flafle.Padbytes      ,"Padbytes",sLijn);
				fouten += checkString(srcWork.flafle.QuoteCharacter,"QuoteCharacter",sLijn);
				fouten += checkString(srcWork.flafle.Repeatable    ,"Repeatable",sLijn);
				fouten += checkString(srcWork.flafle.RowDelimiter  ,"RowDelimiter",sLijn);
				fouten += checkString(srcWork.flafle.Skiprows             ,"Skiprows",sLijn);
				fouten += checkString(srcWork.flafle.Striptrailingblanks  ,"Striptrailingblanks",sLijn);
				//
				if( isSource ) {
				fouten += checkString(srcWork.flafle.ShiftSensitiveData   ,"ShiftSensitiveDate",sLijn);
				}
				//
				if( fouten > 0 ) {
					srcWork.Name = null;
					return false;
				}
				
			
			return true;
			
		}
		
		//----------------------------------------------------------------
		private boolean extract_Field(String sLijn , ParseType tipe )
		//----------------------------------------------------------------
		{
			    boolean isSource=true;
			    if( (tipe == readInfaXML.ParseType.TARGETTABLE) || (tipe == readInfaXML.ParseType.TARGETFLATFILE) ) isSource=false;
			    //
			    String sTemp = groom(sLijn);
				ArrayList<infaPair> list = tokenize(sTemp);
				if( list == null ) return false;
				if( list.size() == 0 ) {
					errit("Empty element list after SOURCEFIELD");
					return false;
				}
			
				// indien een fout - srcWork.Name = null
				infaSourceField fld = new infaSourceField(null,xMSet.getNextUID());
				fld.BusinessName = null;
				fld.DataType = null;
				fld.Description = null;
				fld.fieldNumber = -1;
				fld.FieldProperty = null;
				fld.FieldType = null;
				fld.Hidden = null;
				fld.KeyType = null;
				fld.Length = -1;
				fld.Level = -1;
				fld.Name = null;
				fld.Occurs = -1;
				fld.offset = -1;
				fld.physicalLength = -1;
				fld.physicalOffset = -1;
				fld.PictureText = null;
				fld.Precision = -1;
				fld.referencedField=null;
				fld.referencedTable=null;
				fld.isFileNameField=null;
				//
				for(int i=0;i<list.size();i++) {
					readInfaXML.FIELD_ATTRIBS att = getFieldToken(list.get(i).code);
					if( att == null ) {
						errit("(extract_Field) Unsupported field code [" +  list.get(i).code + "] on [" + sLijn + "]");
	             		srcWork.Name=fld.Name=null;
	             		return false;	
					}
					switch( att )
					{
					case BUSINESSNAME   : { fld.BusinessName = list.get(i).value; break; }
					case DATATYPE       : { fld.DataType = list.get(i).value; break; }
					case DESCRIPTION    : { fld.Description = list.get(i).value; break; }
					case FIELDPROPERTY  : { fld.FieldProperty = list.get(i).value; break; }
					case FIELDTYPE      : { fld.FieldType = list.get(i).value; break; }
					case HIDDEN         : { fld.Hidden = list.get(i).value; break; }
					case KEYTYPE        : { fld.KeyType = list.get(i).value; break; }
					case NAME           : { fld.Name = list.get(i).value; break; }
					case PICTURETEXT    : { fld.PictureText = list.get(i).value; break; }
					case USAGE_FLAGS    : { fld.UsageFlags = list.get(i).value; break; }
					case REFERENCEDFIELD: { fld.referencedField = list.get(i).value; break; }
					case REFERENCEDTABLE: { fld.referencedTable = list.get(i).value; break; }
					case ISFILENAMEFIELD: { fld.isFileNameField = list.get(i).value; break; }
					case NULLABLE       : { String sNul = list.get(i).value.trim().toUpperCase();
					                        if( sNul.compareToIgnoreCase("NULL") == 0 )fld.mandatory = false;
					                        else
				                        	if( sNul.compareToIgnoreCase("NOTNULL") == 0 ) fld.mandatory = true;	
				                        	else {
				                        		errit("(extract_Field) Unsupported NULLABLE value on [" + sLijn + "]");
				                				return false;	
				                        	}
						                    break; }
					case FIELDNUMBER    : {  fld.fieldNumber = getNumber(list.get(i).value,list.get(i).code,sLijn);
						                   if( fld.fieldNumber < 0 ) {
					                   	      	srcWork.Name=fld.Name=null;
					                   	      	return false;
					                    }
					                    break; }
					case LENGTH         : { fld.Length = getNumber(list.get(i).value,list.get(i).code,sLijn);
	                					  if( fld.Length < 0 ) {
	                							srcWork.Name=fld.Name=null;
	                							return false;
	                					}
	                					break; }
					case OFFSET         : { fld.offset = getNumber(list.get(i).value,list.get(i).code,sLijn);
											if( fld.offset < 0 ) {
												srcWork.Name=fld.Name=null;
												return false;
											}
										break; }
					case PHYSICALLENGTH : { fld.physicalLength = getNumber(list.get(i).value,list.get(i).code,sLijn);
											if( fld.physicalLength < 0 ) {
												srcWork.Name=fld.Name=null;
												return false;
											}
										break; }
					case PHYSICALOFFSET : { fld.physicalOffset = getNumber(list.get(i).value,list.get(i).code,sLijn);
											if( fld.physicalOffset < 0 ) {
												srcWork.Name=fld.Name=null;
												return false;
											}
										break; }
					case PRECISION     : { fld.Precision = getNumber(list.get(i).value,list.get(i).code,sLijn);
										if( fld.Precision < 0 ) {
											srcWork.Name=fld.Name=null;
											return false;
										}
										break; }
					case SCALE       : { fld.scale = getNumber(list.get(i).value,list.get(i).code,sLijn);
										if( fld.scale < 0 ) {
											srcWork.Name=fld.Name=null;
											return false;
										}
										break; }
					case LEVEL       : { fld.Level = getNumber(list.get(i).value,list.get(i).code,sLijn);
										if( fld.Level < 0 ) {
											srcWork.Name=fld.Name=null;
											return false;
										}
										break; }
					case OCCURS       : { fld.Occurs = getNumber(list.get(i).value,list.get(i).code,sLijn);
										if( fld.Occurs < 0 ) {
											srcWork.Name=fld.Name=null;
											return false;
										}
										break; }
	                default : {
	                	errit("(extract_Field) Unsupported field code [" +  list.get(i).code + "]");
	             		srcWork.Name=fld.Name=null;
	             		return false;
	                  }
	         		} // switch
				}	
				//
				int fouten=0;
				fouten += checkString(fld.BusinessName ,"BusinessName",sLijn);
				fouten += checkString(fld.DataType     ,"DataType",sLijn);
				fouten += checkString(fld.Description  ,"Description",sLijn);
				fouten += checkString(fld.KeyType      ,"KeyType",sLijn);
				fouten += checkString(fld.Name         ,"Name",sLijn);
				fouten += checkString(fld.PictureText  ,"PictureText",sLijn);
				//
				fouten += checkNumeric(fld.fieldNumber    ,"fieldnumber",sLijn);
				fouten += checkNumeric(fld.Precision      ,"precision",sLijn);
			    //
				if( isSource == true )
				{
				fouten += checkString(fld.Hidden       ,"Hidden",sLijn);
				fouten += checkString(fld.FieldProperty,"FieldProperty",sLijn);
				fouten += checkString(fld.FieldType    ,"FieldType",sLijn);
				fouten += checkNumeric(fld.Length         ,"length",sLijn);
				fouten += checkNumeric(fld.Level          ,"level",sLijn);
				fouten += checkNumeric(fld.Occurs         ,"occurs",sLijn);
				fouten += checkNumeric(fld.offset         ,"offset",sLijn);
				fouten += checkNumeric(fld.physicalLength ,"physicallenght",sLijn);
				fouten += checkNumeric(fld.physicalOffset ,"physicaloffset",sLijn);
				}
				else {  // komen niet altijd voor
					//fouten += checkString(fld.isFileNameField,"FileNameField",sLijn);	
					//fouten += checkString(fld.referencedField,"ReferencedField",sLijn);
					//fouten += checkString(fld.referencedTable,"ReferencedTable",sLijn);
				}
				
	            //		
				if( fouten != 0) {
					srcWork.Name = null;
					return false;
				}
				//
				srcWork.fieldList.add(fld);
				//
			    return true;
		}
			
		
		//----------------------------------------------------------------
		private boolean extract_tableAttribute(String sIn)
		//----------------------------------------------------------------
		{
			    if ( srcWork == null ) {
				   errit("System error - extract flat file - srcWork is NULL");
				   return false;
			    }
			    if ( srcWork.Name == null ) {
				   errit("System error - extract flat file - srcWork.NAME is NULL");
				   return false;
			    }
			    //
			    String sTemp = groom(sIn);
				ArrayList<infaPair> list = tokenize(sTemp);
				if( list == null ) return false;
				if( list.size() == 0 ) {
					errit("Empty element list after <TABLEATTRIBUTE");
					return false;
				}
				//
				String sCode  = null;
				String sValue = null;
				for(int i=0;i<list.size();i++) 
				{
				  //System.out.println(list.get(i).code + " " + list.get(i).value );
				  if( list.get(i).code.trim().toUpperCase().compareToIgnoreCase("NAME")==0)  sCode = list.get(i).value;
				  if( list.get(i).code.trim().toUpperCase().compareToIgnoreCase("VALUE")==0) {
					  sValue = list.get(i).value;
					  infaPair x = new infaPair(sCode,sValue);
					  srcWork.tableAttributeList.add(x);
					  sCode = null;
					  sValue=null;
				  }
				}	 
				//
				return true;
		}
		
		//----------------------------------------------------------------
		private int checkString(String sIn, String sCode , String sLijn)
		//----------------------------------------------------------------
		{
			if( sIn != null ) return 0;
			errit("(checkString) Missing value for [" + sCode.toUpperCase() + "] on [" + sLijn.trim() + "] in [" + SourceFileName + "]");
			return 1;
		}
		
		//----------------------------------------------------------------
		private int checkNumeric(int i, String sCode , String sLijn)
		//----------------------------------------------------------------
		{
				if( i >= 0 ) return 0;
				errit("(checkNumber) Invalid number [" + i + "] for attribute [" + sCode.toUpperCase() + "] on [" + sLijn.trim() + "] in [" + SourceFileName + "]");
				return 1;
		}
		
		//----------------------------------------------------------------
		private int getNumber(String sIn , String sCode , String sLijn)
		//----------------------------------------------------------------
		{
		    try { 
		    	int i=Integer.parseInt( sIn ); 
		    	return i; 
		    }
		    catch ( NumberFormatException e)  {	
		    	errit("(getNumber) Integer.parseInt() convesion error [" + sIn + "] on [" + sCode + "] -> " + sLijn);
		    	return -1;
		    }
		}
		
		//----------------------------------------------------------------
		private ArrayList<infaPair> tokenize(String sIn)
		//----------------------------------------------------------------
		{
			if( sIn == null )  return null;
			if( sIn.trim().length() == 0 ) return null;
			// voor de eerste dubble quote een space zetten
			// indien na eerste quote alle spaties vervangen door §
			String sTemp="";
			boolean eerste=false;
			char[] buf = sIn.trim().toCharArray();
	   	    for(int i=0;i<buf.length;i++)   // -1 om de afsluitende > eruit te strippen
		    {
	   	    	if( buf[i] == '"' ) {
	   	    		eerste = !eerste;
	   	    		if( eerste ) sTemp = sTemp + " \""; else sTemp = sTemp + "\"";
	   	    		continue;
	   	    	}
	   	    	if( (eerste==true) && (buf[i] == (char)0x20) ) {
	   	    		sTemp = sTemp + "§";
	   	    		continue;
	   	    	}
	   	    	sTemp = sTemp + buf[i];
	  	    }
	   	    //
	   	    ArrayList<infaPair> list = new ArrayList<infaPair>();
	   	    //
			StringTokenizer st = new StringTokenizer(sTemp, "= \n");
			infaPair xPair=null;
			while(st.hasMoreTokens()) { 
			  String sCode = st.nextToken().trim();
			  try {
				if( sCode.startsWith("<") ) continue;    // <SOURCE overslaan
			    if( sCode.startsWith("\"") == false ) {  // double quote
				  //String sEq  = st.nextToken().trim();
				  String sVal = st.nextToken().trim();
				  if( sVal == null ) {
					  errit("(tokenize) tokenizer error - val is null" + sIn );
					  return null;
				  }	  
				  // VAL moet starten en eindigen op dubble quote
				  if( sVal.startsWith("\"") == false ) {
					  errit("(tokenize) Missing start double quote [" + sVal + "] in line : " + sTemp );
					  return null;
				  }
				  if( sVal.endsWith("\"") == false ) {
					  errit("(tokenize) Missing end double quote [" + sVal + "] in line : " + sTemp );
					  return null;
				  }
				  // quotes strippen
				  sVal = strul.Remplaceer(sVal,"\"","");
				  sVal = strul.Remplaceer(sVal,"§"," ");
				  //
				  xPair = new infaPair(sCode,sVal);
				  list.add(xPair);
			    }
			  }
			  catch (Exception e) {
				  errit("(tokenize) tokenizer error " + sIn + xMSet.xU.LogStackTrace(e));
				  return null;
			  }
			} 
			return list;
		}
		
		
		//----------------------------------------------------------------
		private boolean extract_Constraint(String sIn )
		//----------------------------------------------------------------
		{
			//logit(5,"CON ->" + sIn );
			//
			String sName = xMSet.xU.extractXMLValue( sIn , "CNSTRNAME" ).trim();
			String sTipe = xMSet.xU.extractXMLValue( sIn , "CNSTRTYPE" ).trim();
			String sKeyList = xMSet.xU.extractXMLValue( sIn , "CNSTRKEYS" ).trim();
			generatorConstants.CONSTRAINT tipe = null;
			if( sTipe.compareToIgnoreCase( "PRIMARY" ) == 0 ) tipe = generatorConstants.CONSTRAINT.PRIMARY;
			if( sTipe.compareToIgnoreCase( "UNIQUE" ) == 0 ) tipe = generatorConstants.CONSTRAINT.UNIQUE;
			if( sTipe.compareToIgnoreCase( "FOREIGN" ) == 0 ) tipe = generatorConstants.CONSTRAINT.FOREIGN;
			if( tipe == null ) {
				errit(" Unknown tipe in CONSTRAINT [" + sIn + "]");
				return false;
			}
		    if( (sName.length()<1) || (sKeyList.length()<1) )  {
		    	errit("Empty names or key list in CONSTRAINT [" + sIn + "]");
		    	return false;
		    }
			infaConstraint co = new infaConstraint();
			co.Name = sName;
			co.Tipe = tipe;
	        //
	        String sList = xMSet.xU.Remplaceer( sKeyList , "<KEY>" , "" );
	        sList = "," + xMSet.xU.Remplaceer( sList , "</KEY>" , "," );
	        StringTokenizer st = new StringTokenizer(sList, ",");
	        while(st.hasMoreTokens()) 
			{ 
			  String sItem = st.nextToken().trim();
			  if( sItem.length() <=0 ) continue;
			  co.key_list.add( sItem );
			}
	        
			// FOREIGNs
			if( tipe != generatorConstants.CONSTRAINT.FOREIGN ) {
				srcWork.constraintList.add( co );
				return true;
			}
			String sRefTable = xMSet.xU.extractXMLValue( sIn , "TABLEREFERENCED" );
			String sRefOwner = xMSet.xU.extractXMLValue( sIn , "OWNERREFERENCED" );
			String sRefList = xMSet.xU.extractXMLValue( sIn , "REFKEYS" );
			
			if( (sRefOwner.length()<1) || (sRefTable.length()<1) )  {
		    	errit("Empty names or key list in CONSTRAINT [" + sIn + "]");
		    	return false;
		    }
			co.ReferencedOwner = sRefOwner;
			co.ReferencedTableName = sRefTable;
			
			sList = xMSet.xU.Remplaceer( sRefList , "<REFKEY>" , "" );
		    sList = "," + xMSet.xU.Remplaceer( sList , "</REFKEY>" , "," );
		    st = new StringTokenizer(sList, ",");
	        while(st.hasMoreTokens()) 
			{ 
			  String sItem = st.nextToken().trim();
			  if( sItem.length() <=0 ) continue;
			  co.ref_list.add( sItem );
			}
			//
			srcWork.constraintList.add( co );
			return true;
		}
		
		
}
