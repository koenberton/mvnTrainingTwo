package pcGenerator.ddl;

import generalpurpose.gpUtils;
import generalpurpose.pcDevBoosterSettings;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import pcGenerator.generator.generatorConstants;
import pcGenerator.powercenter.infa2DCoordinate;
import pcGenerator.powercenter.infaConstraint;
import pcGenerator.powercenter.infaSource;
import pcGenerator.powercenter.infaSourceField;


/*
 * 09OCT KB fixed error on Oracle Number
 */

public class db2Tokenize {

                pcDevBoosterSettings xMSet = null;
                rdbmsTokenizerConfig config = null;
                gpUtils xU = null;
                ArrayList<infaSource> srcList=null;
                
                int NbrOfTablesDetected =0 ;
                private String DatabaseName = null;
                private boolean DEBUG = false;
                private String ConfigFileName=null;
                
                
                private String delimList   = ".,;()[]{}'\"<>!=-+/*:";
                private char[] delimBuffer = delimList.toCharArray();
                
                // Imperative to ensure that delimlist corresponds to be below
                enum PLAINSQL { 
                    PERIOD , COMMA , SEMICOLON , 
                    LEFTPARENTESIS, RIGHTPARENTESIS , LEFTBRACKET , RIGHTBRACKET , LEFTACCOLADE , RIGHTACCOLADE ,
                    SINGLEQUOTE , DOUBLEQUOTE ,
                    LESS , GREATER , EXCLAMATION , EQUAL ,
                    MINUS , PLUS , DIVIDE , ASTERIKS , COLON ,
                    
                 end_of_single , 
                    
                    NOTEQUAL , LESSOREQUAL , GREATEROREQUAL , 
                    
                 end_of_delims ,
                
                    BYTE , CHAR, ORCL_EXTERNAL_TABLE_START ,
                    
                 end_of_Oracle ,
                
                    CREATE , DROP, DELETE , ALTER ,  
                    TABLE , INDEX , CONSTRAINT , TRIGGER , 
                    ADD , PRIMARY , KEY , UNIQUE , WITH , DEFAULT , FOREIGN , REFERENCES ,
                    USING , DESC , ASC , ON , 
                    NOT , NULL , NOTNULL ,
                    TABLESPACE , RESTRICT ,
                    IN , CHECK ,
                    FREEPAGE , PCTFREE , GBPCACHE , BUFFERPOOL, PIECESIZE , CLOSE , YES , NO ,
                    COMMENT , COLUMN , IS
                }
                               
                //----------------------------------------------------------------
                private PLAINSQL getDelimiter(String sIn)
                //----------------------------------------------------------------
                {
                               char[] ibuf = sIn.toCharArray();
                               char cs = ibuf[0];
                               int idx = delimList.indexOf(cs);
                               if( idx < 0 ) return null;
                               return PLAINSQL.values()[idx];
                }
                
                //----------------------------------------------------------------
                private PLAINSQL getToken(String s)
                //----------------------------------------------------------------
                {
                               boolean active=false;
                               for(int i=0;i<PLAINSQL.values().length;i++)
                               {
                                               if( PLAINSQL.values()[i].toString().compareToIgnoreCase("end_of_delims") == 0 ) {active=true; continue; }
                                               if( !active ) continue;
                                               if( PLAINSQL.values()[i].toString().compareToIgnoreCase(s) == 0 ) return PLAINSQL.values()[i];
                               }
                               return null;
                }
                
                class Element
                {
                               PLAINSQL token = null;
                               String value = "";
                               String originalText = "";
                               Element(PLAINSQL t , String s , String sOrig)
                               {
                                               token = t;
                                               value = s;
                                               originalText = sOrig;
                                               if( token != null ) s = null;
                                               if( s != null ) token = null;
                                               
                                               if( (token == null) && (value == null) ) {
                                                               errit("Whoa - tokenizer systeem fout");
                                               }
                               }
                }
                ArrayList<Element> tokenStack = null;
                
                private rdbmsDatatype.DBMAKE dbmake = null;
                
                //----------------------------------------------------------------
                public db2Tokenize( pcDevBoosterSettings xm , String inDatabaseName , rdbmsDatatype.DBMAKE idbmake , String ConfigFileNameIn )
                //----------------------------------------------------------------
                {
                   xMSet = xm;
                   xU = xMSet.xU;
                   dbmake = idbmake;
                   DatabaseName = inDatabaseName;
                   tokenStack = new ArrayList<Element>();
                   ConfigFileName = ConfigFileNameIn;
                   config = new rdbmsTokenizerConfig(xMSet);
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
                private void do_error(boolean fatal , String sIn)
                //----------------------------------------------------------------
                {
                   if( fatal ) { 
                        errit("FATAL error : " + sIn);
                         return;
                    }
                    errit("fatal error : " + sIn);
                }
                
                //----------------------------------------------------------------
                private boolean parseConfigFile()
                //----------------------------------------------------------------
                {
                	if( ConfigFileName == null ) return true;  // no config so ok
                	return config.parseConfig(ConfigFileName);
                }
                
                //----------------------------------------------------------------
                public ArrayList<infaSource> parseFile( String FName )
                //----------------------------------------------------------------
                {
                     NbrOfTablesDetected=0;
                     srcList = new ArrayList<infaSource>();  
                     if( parseConfigFile() == false ) {
                    	 errit("Cannot interpret the DDL configuration");
                    	 return null;
                     }
                     if( lees_en_converteer(FName) == false ) return null;
                     collapseQuotes();
                     lookAhead();
                     if( DEBUG ) shoStack();
                     getTables();
                     logit ( 1 , "Tables [Detected=" + NbrOfTablesDetected +"] [Skipped=" + (NbrOfTablesDetected-srcList.size()) + "]");
                     return srcList;
                }
                
                //----------------------------------------------------------------
                private void shoStack()
                //----------------------------------------------------------------
                {
                               boolean NL=false;
                               String sLijn = "";
                               int counter=0;
                               for(int i=0;i<tokenStack.size();i++)
                               {
                                 if( tokenStack.get(i).token != null ) {
                                                 sLijn = sLijn + " " + tokenStack.get(i).token.toString();
                                                 if( tokenStack.get(i).token == PLAINSQL.COMMA )   NL=true;
                                                 if( tokenStack.get(i).token == PLAINSQL.SEMICOLON )   NL=true;
                                                 if( NL ) {
                                                                 logit(5,String.format("%04d", counter++) + " " + sLijn);
                                                                 NL=false;
                                                                 sLijn = "";
                                                 }
                                 }
                                 else {
                                                 String sVal = tokenStack.get(i).value;
                                                 if( sVal == null ) sLijn = sLijn + " [" + "HOEZO EEN NULL?" + "]";
                                                                                                  else sLijn = sLijn + " [" + sVal.toLowerCase() + "]";
                                 }
                               }
                }
                
                
    //----------------------------------------------------------------
    private boolean lees_en_converteer(String FNaam )
    //----------------------------------------------------------------
    {
                //
                switch( dbmake )
                {
                case NETEZZA : ;
                case DB2     : ;
                case ORACLE  : break;
                default : {
                               do_error( true , "Unsupported database type [" + dbmake + "]");
                               return false;
                }
                }
                String ENCODING = xU.getEncoding(FNaam);
                //   
                long startt = System.currentTimeMillis();
                long elapsed =0L;
                int lineCounter=0;
                try {
                          BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(FNaam),ENCODING));
                          String sLijn = null;
                          while ((sLijn=reader.readLine()) != null) {
                                  lineCounter++;
                              String s1 = xMSet.xU.compress_spaces(sLijn);
                              if( s1 == null ) continue;
                              if( s1.length() <= 0) continue;
                              //
                              String s2 = expand_delims(s1);
                              if( s2 == null ) continue;
                              if( s2.length() <= 0) continue;
                              //
                              extract_tokens(s2+" ");
                              //
                              long currt = System.currentTimeMillis();
                              if( (currt - startt) > 250L ) {
                                  elapsed += currt - startt;
                                  startt = currt;
                                  logit(5, "" + String.format("% 5d", elapsed) + "msec  [#lines=" + lineCounter + "] [#tokens=" + tokenStack.size() + "]");
                              }
                          }
                          reader.close();
                                }
                                               catch (Exception e) {
                                                               do_error( true , "Error reading file [" + FNaam + "]" + e.getMessage());
                                                               return false;
                                   }
                return true;
    }
    
    //----------------------------------------------------------------
    private String expand_delims(String sIn)
    //----------------------------------------------------------------
    {
                  String sRet = "";
                  char[] buf = sIn.toCharArray();
                  boolean found=false;
    
                      for(int i=0;i<buf.length;i++)
                      {
                                  found = false;
                                  for(int j=0;j<delimBuffer.length;j++)  
                                  {
                                               if( delimBuffer[j] == buf[i] ) { 
                                                               sRet = sRet + (char)0x20 + buf[i] + (char)0x20; found = true; break; }  
                                  }
                                  if( found ) continue;
                                  sRet = sRet + buf[i];
                      }
                      return sRet;
    }

    //----------------------------------------------------------------
    private void extract_tokens(String sIn)
    //----------------------------------------------------------------
    {
                  db2Tokenize.PLAINSQL tok = null;
          db2Tokenize.PLAINSQL prevtok = null;
          
                  char[] buf = sIn.toCharArray();
                  String word="";
          for(int i=0;i<buf.length;i++)
                      {
                                  if( buf[i] != (char)0x20 ) {
                                                 word = word + buf[i];
                                                 continue;
                                  }
                                  
                                  String sToken = word;
                                  String sOrig = word;
                                  word="";
                                  tok=null;
                                  if( sToken.length() < 1) continue;
                                  
                                  // Delimiters
                                  if( sToken.length() == 1 ) {
                                                 tok = getDelimiter(sToken);
                                  }
                                  // Tokens
                                  if( tok == null) {
                                                 tok = getToken(sToken);
                                  }
                                  if( tok != null ) {
                                                 // First process possible double tokens, i.e.
                                                 // NOT NULL <>  !=  <=  >=
                                                 if( prevtok != null ) {
                                                  boolean overwrite = false;        
                                                  if( (prevtok == PLAINSQL.LESS) && (tok == PLAINSQL.GREATER)  ) { tok = PLAINSQL.NOTEQUAL; overwrite=true; }
                                                  if( (prevtok == PLAINSQL.EXCLAMATION) && (tok == PLAINSQL.EQUAL)  ) { tok = PLAINSQL.NOTEQUAL; overwrite=true; }
                                                  if( (prevtok == PLAINSQL.LESS) && (tok == PLAINSQL.EQUAL)  ) { tok = PLAINSQL.LESSOREQUAL; overwrite=true; }
                                                  if( (prevtok == PLAINSQL.GREATER) && (tok == PLAINSQL.EQUAL)  ) { tok = PLAINSQL.GREATEROREQUAL; overwrite=true; }
                                                  if( (prevtok == PLAINSQL.NOT) && (tok == PLAINSQL.NULL)  ) { tok = PLAINSQL.NOTNULL; overwrite=true; }
                                                  //
                                                  if( overwrite == true ) {
                                                                  tokenStack.get( tokenStack.size()-1 ).token = tok;
                                                                  prevtok = null; // wellicht ok zo
                                                                  continue;
                                                  }
                                                 }
                                                 //
                                                 Element x = new Element(tok,null,sOrig);
                                                 tokenStack.add(x);
                                                 prevtok=tok;
                                  continue;
                                 }
                                   
                                  // values
                                  Element x = new Element(null,sToken,sOrig);
                                  tokenStack.add(x);
                                  prevtok=null;
                      }
    }

    //----------------------------------------------------------------
    private void collapseQuotes()
    //----------------------------------------------------------------
    {
                // bijvoorbeeld create table "CHECK", check is reserved word
                // zet de single en double quotes door naar volgende veld
                // indien volgende veld een token dan de originele tekst terugzetten en token op NULL
                boolean openQuote=false;
                for(int i=0;i<tokenStack.size()-2;i++)
                {
                               if( tokenStack.get(i).token != null ) {
                                if( (tokenStack.get(i).token == PLAINSQL.DOUBLEQUOTE) || (tokenStack.get(i).token == PLAINSQL.SINGLEQUOTE)) {
                                                openQuote = !openQuote;
                                                if( openQuote )  {
                                                                if( (tokenStack.get(i+1).token != null) && 
                                                                               ((tokenStack.get(i+2).token == PLAINSQL.DOUBLEQUOTE) || (tokenStack.get(i+2).token == PLAINSQL.SINGLEQUOTE)) ) {
                                                                               tokenStack.get(i+1).value = tokenStack.get(i+1).originalText;
                                                                               tokenStack.get(i+1).token = null;
                                                                               //System.err.println("swap " + tokenStack.get(i+1).value);
                                                                }
                                                }
                                }
                               }
                }
    }
    
    
    // scan through original text en look for patterns like CHAR FOR BIG DATA, ORGANIZATION EXTERNAL 
    // and collapse into a single token entry
    //----------------------------------------------------------------
    private void lookAhead()
    //----------------------------------------------------------------
    {
                for(int i=0;i<tokenStack.size();i++)
                {
                               String sEval2 = "";
                               if( i < (tokenStack.size()-2) ) {
                                   sEval2 = tokenStack.get(i).originalText.trim() + "_" + 
                                            tokenStack.get(i+1).originalText.trim();
                               }
                               String sEval3 = "";
                               if( i < (tokenStack.size()-3) ) {
                                   sEval3 = tokenStack.get(i).originalText.trim()   + "_" + 
                                            tokenStack.get(i+1).originalText.trim() + "_" +
                                            tokenStack.get(i+2).originalText.trim();
                               }
                               String sEval4 = "";
                               if( i < (tokenStack.size()-4) ) {
                                   sEval4 = tokenStack.get(i).originalText.trim()   + "_" + 
                                            tokenStack.get(i+1).originalText.trim() + "_" +
                                            tokenStack.get(i+2).originalText.trim() + "_" +
                                            tokenStack.get(i+3).originalText.trim();
                               }
                               String sFound = null;
                               if ( sEval2.compareToIgnoreCase("ORGANIZATION_EXTERNAL") == 0 ) sFound = "ORGANIZATION_EXTERNAL";
                               if ( sEval2.compareToIgnoreCase("LONG_VARCHAR") == 0 ) sFound = "LONG_VARCHAR";
                               if ( sEval2.compareToIgnoreCase("LONG_VARGRAPHIC") == 0 ) sFound = "LONG_VARGRAPHIC";
                               if ( sEval2.compareToIgnoreCase("CHARACTER_VARYING") == 0 ) sFound = "_VARCHAR";  // 1 times an _
                               if ( sEval2.compareToIgnoreCase("DOUBLE_PRECISION") == 0 ) sFound = "_DOUBLE";  
                               if ( sEval2.compareToIgnoreCase("NATIONAL_CHARACTER") == 0 ) sFound = "_NCHAR";
                               //
                               if ( sEval3.compareToIgnoreCase("NATIONAL_CHARACTER_VARYING") == 0 ) sFound = "__NVARCHAR";  // 2 times an _
                               //
                               if ( sEval4.compareToIgnoreCase("CHAR_FOR_BIT_DATA") == 0 ) sFound = "CHAR_FOR_BIT_DATA";
                               if ( sEval4.compareToIgnoreCase("VARCHAR_FOR_BIT_DATA") == 0 ) sFound = "VARCHAR_FOR_BIT_DATA";
            //System.out.println(sEval2 + " -> " + sFound);
                               //
                               if( sFound == null ) continue;
                               //
            int nWords = xU.TelDelims( sFound , '_' );
            if( nWords <= 0 ) continue;
                               //logit(9,"Collapsing [" + sFound + "] items=" + nWords);
                    for(int j=0;j<(nWords+1);j++)
                               {
                                               if( j == 0 ) {
                                                               if( sFound.compareTo("ORGANIZATION_EXTERNAL") == 0 )  {
                                                                              tokenStack.get(i).token = PLAINSQL.ORCL_EXTERNAL_TABLE_START;
                                                                              tokenStack.get(i).value = null;                  
                                                                              tokenStack.get(i).originalText = sFound;
                                                               }
                                                               else {
                                                                              // [KB-25/08] issue solved
                                                                              if( sFound.compareTo("_VARCHAR") == 0 ) sFound = "VARCHAR";
                                                                              if( sFound.compareTo("_DOUBLE") == 0 ) sFound = "DOUBLE";
                                                                              if( sFound.compareTo("_NCHAR") == 0 ) sFound = "NCHAR";
                                                                              if( sFound.compareTo("__NVARCHAR") == 0 ) sFound = "NVARCHAR";
                                                                              //
                                                                              tokenStack.get(i).token = null;
                                                                              tokenStack.get(i).value = sFound;                                         
                                                                              tokenStack.get(i).originalText = sFound;
                                                               }
                                               }
                                               else {
                                                               tokenStack.get(i+j).token = null;
                            tokenStack.get(i+j).value = null;                                 
                                                               tokenStack.get(i+j).originalText = null;
                                               }
                               }
                    i += nWords;  // en avant want anders knal je op die nulls en krijg je een mooie exception ..
                }
                // housekeeping - remove all token=null and value=null
                int aantal = tokenStack.size();
                for(int k=0;k<aantal;k++)
                {
                               boolean found = false;
                               for(int i=0;i<tokenStack.size();i++)
                               {
                                               if( tokenStack.get(i).token != null ) continue;
                                               if( tokenStack.get(i).value != null ) continue;
                                               found = true;
                                               tokenStack.remove(i);
                                               break;
                                }
                               if( found == false ) break;
                }
    }
    
    // MAIN routine
    //----------------------------------------------------------------
    private void getTables()
    //----------------------------------------------------------------
    {
        String owner=null;
        String ddldatabasename=null;
        String tableName=null;
        int pos=-1;
        boolean inTable=false;
        boolean inAlter=false;
        boolean inComment=false;
        int startAlter=-1;
        int startComment=-1;
        boolean singleQuoteOpen=false;
        boolean inExternalTable=false;
        int colpos=-1;
        boolean inColumn=false;
        boolean inExternalColumn=false;
        boolean expectPrecision=false;
        String colName=null;
        String colType=null;
        int haakjesDiepte=0;
        int fieldNumber=0;
        int extColCount=-1;
        int extPositionStart=-1;
        int extPositionStop=-1;
        int sourceFieldIdx=-1;
        boolean inconstraint=false;
        boolean rescindconstraint=false;
        boolean toadPrimarySyntax=false;
        boolean skipUntilComma=false;
        ArrayList<Element> constraintStack = null;
        //    
        infaSource xSource=null;
        infaSourceField xField=null;
        rdbmsDatatype rdbmsdt = new rdbmsDatatype(xMSet);
        //
        // inTable op FALSE zetten herinitialiseert de parser
                for(int i=1;i<tokenStack.size();i++)
                {
                  // Ignore this these tokens
                  if( tokenStack.get(i).token != null ) {
                                 if( tokenStack.get(i).token == PLAINSQL.DOUBLEQUOTE ) continue;
                                 if( tokenStack.get(i).token == PLAINSQL.SINGLEQUOTE ) continue;
                  }
                  if( tokenStack.get(i).token == PLAINSQL.LEFTPARENTESIS ) {
                                haakjesDiepte++;
                  }
                  if( tokenStack.get(i).token == PLAINSQL.RIGHTPARENTESIS ) {
                                haakjesDiepte--;
                  }
                  // ======================  ALTER : scan until ;
                  if( inAlter ) {
                	  if( tokenStack.get(i).token == PLAINSQL.SINGLEQUOTE ) {
                		  singleQuoteOpen = !singleQuoteOpen;
                		  continue;
                	  }
                	  if( (tokenStack.get(i).token == PLAINSQL.SEMICOLON) && (singleQuoteOpen==false) ) {
                		  inAlter=false;
                		  boolean ib = do_AlterTable( startAlter , i );
                		  continue;
                	  }
                	  continue;
                  }
                  // ======================  COMMENT ON {table,column}
                  if( inComment ) {
                	  if( tokenStack.get(i).token == PLAINSQL.SINGLEQUOTE ) {
                		  singleQuoteOpen = !singleQuoteOpen;
                		  continue;
                	  }
                	  if( (tokenStack.get(i).token == PLAINSQL.SEMICOLON) && (singleQuoteOpen==false) ) {
                		  inComment=false;
                		  do_comment( startComment , i );
                		  continue;
                	  }
                	  continue;
                  }
                  else {
                	  if( (tokenStack.get(i).token == PLAINSQL.COMMENT) && ((i+5) < tokenStack.size()) ) {
                	    if( (tokenStack.get(i+1).token == PLAINSQL.ON) &&
                	    	((tokenStack.get(i+2).token == PLAINSQL.TABLE) || (tokenStack.get(i+2).token == PLAINSQL.COLUMN)) ) {
                	    		inComment=true;
                	    		singleQuoteOpen=false;
                	    		startComment=i;
                	    	}
                	    
                	  }
                  }
                  // ======================  INTABLE  : scan until CREATE TABLE 
                  if( inTable == false ) 
                  {
                    if( (tokenStack.get(i).token != null ) || (tokenStack.get(i-1).token) != null) {
                                 //  create/alter table
                                 if( tokenStack.get(i).token != PLAINSQL.TABLE ) continue;
                                 //  ALTER table
                                 if( tokenStack.get(i-1).token == PLAINSQL.ALTER ) {
                                	 inAlter=true;
                                	 startAlter=i-1;
                                	 singleQuoteOpen=false;
                                	 continue;
                                 }
                                 //  CREATE table
                                 if( tokenStack.get(i-1).token != PLAINSQL.CREATE ) continue;
                                 //
                                 pos = 0;
                                 // look ahead whether this is database.owner.table or datbase.owner
                                 if( i < tokenStack.size()-4 ) {
                                	 // <owner>.table
                                	 if( (tokenStack.get(i+2).token != null)  ) {
                                         if( (tokenStack.get(i+2).token == PLAINSQL.PERIOD) ) { pos = 0; }
                                     }
                                	 // <database>.<owner>.table
                                     if( (tokenStack.get(i+2).token != null) && (tokenStack.get(i+4).token != null) ) {
                                          if( (tokenStack.get(i+2).token == PLAINSQL.PERIOD) && (tokenStack.get(i+4).token == PLAINSQL.PERIOD) ) { pos = -2; }
                                     }
                                     // just create table <name>   (without owner and database
                                     if( (tokenStack.get(i+1).token == null) && ((tokenStack.get(i+2).token != null)) ) {
                                         if( (tokenStack.get(i+2).token == PLAINSQL.LEFTPARENTESIS) ) { pos = 2; }
                                     }
                                 } 
                                 //
                                 inTable = true;
                                 NbrOfTablesDetected++;
                                 inColumn=false;
                                 haakjesDiepte=0;
                                 ddldatabasename=null;
                                 owner=null;
                                 inconstraint=false;
                                 continue;
                    }
                  }
                  else {   // database.owner.table  or owner.table
                                 
                               // ==========================  IN TABLE : evaluate the <db>.<ower>.<table> and swict h to IN COLUMN
                    pos++;
                    switch( pos )
                    {
                    case -1 : { ddldatabasename = tokenStack.get(i).value;
                                               if( (tokenStack.get(i).token != null) || (ddldatabasename == null) ) {
                                                               do_error(false,"0 Error parsing <database>.<owner>.<table>");
                                                              inTable=false;
                                                               break;
                                               }
                                               break; 
                }
                    case 1 : { owner = tokenStack.get(i).value;
                               if( (tokenStack.get(i).token != null) || (owner == null) ) {
                                   do_error(false,"1 Error parsing <database>.<owner>.<table>");
                                           inTable=false;
                                           break;
                               }
                               break; 
                             }
                    case 0 : ;
                    case 2 : { if( tokenStack.get(i).token != PLAINSQL.PERIOD ) {
                                           do_error( false, "2 Error parsing <database>.<owner>.<table>");
                                           inTable=false;
                                           break;
                               }
                               break;
                             }
                    case 3 : { tableName = tokenStack.get(i).value;
                               if( (tokenStack.get(i).token != null) || (tableName == null) ) {
                                do_error( false , "3 Error parsing <datbase>.<owner>.<tablename>" + " " + owner + " " + tokenStack.get(i).token + " " + tokenStack.get(i+1).value);
                                       inTable=false;
                                       break;
                       }
                               break; 
                             }
                    case 4 : { if( tokenStack.get(i).token != PLAINSQL.LEFTPARENTESIS ) {
                                             do_error( false, "4 Error parsing <database>.<owner>.<tablename> (  ==> " + ddldatabasename + "." + owner + "." + tableName );
                                             inTable=false;
                                             break;
                                   }
                               if( haakjesDiepte != 1 ) {
                                 do_error( false, "Wrong levels in parentesis " + tableName + " " + haakjesDiepte);
                                             inTable=false;
                                             break;
                               }
                               // KB 27OCT - ignore list
                               if( ignore_table(tableName) ) {
                            	   logit(1,"Table [" + tableName + "] will be excluded from processing");
                            	   inTable=false;
                            	   continue;
                               }
                               //
                               inColumn=true;
                               colpos=-1;
                               colName=null;
                               colType=null;
                               expectPrecision=false;
                               fieldNumber=0;
                               //
                               xSource = new infaSource(tableName,readInfaXML.ParseType.UNKNOWN);  
                               xSource.Databasetype = ""+dbmake;
                               xSource.OwnerName = ( owner == null ) ? "" : owner;
                               xSource.Dbdname  = ( ddldatabasename == null ) ? DatabaseName : ddldatabasename;
                               //logit(5, "Adding [" + xSource.Dbdname + "." + xSource.OwnerName + "." + xSource.Name + "]" );
                               continue;
                             }
                    default : break;
                    }
                    
                    //==============  IN COLUMN
                    
                    if( inColumn ) 
                    {
                                colpos++;
                                if( colpos == 0) {
                                               if( tokenStack.get(i).token != null ) {
                                            	   				// CHECK
                               	   								if( tokenStack.get(i).token == PLAINSQL.CHECK ) {
                               	   									logit(9,"got CHECK" );
                               	   									skipUntilComma=true;
                               	   									continue;
                               	   								}
                                            	   				// TOAD sometimes omits the CONSTRAINT <name> 
                               	   								else
                                            	   				if( tokenStack.get(i).token == PLAINSQL.PRIMARY ) {
                                            	   					logit(9,"Fixing TOAD issue CONSTRAINT <name> is not preceding PRIMARY (col)" );
                                            	   					inColumn  = false;
                                            	   					toadPrimarySyntax=true;
                                            	   					continue;
                                            	   				}
                                            	   				else
                                                                if( tokenStack.get(i).token == PLAINSQL.CONSTRAINT ) {
                                                                    //errit("(gettables) in constraint - zou niet mogen");
                                                                    inColumn  = false;
                                                                    rescindconstraint=true;
                                                                    continue;
                                                                }
                                                                else {
                                                                 // Reserved words should be enclosed in double quotes; sometimes they are not ..
                                                                 // ignore DATE - COMMENT - COLUMN reserved words
                                                                 String si = tokenStack.get(i).token.toString().toUpperCase().trim();
                                                                 if( (si.compareToIgnoreCase("DATE") != 0) &&
                                                                	 (si.compareToIgnoreCase("COMMENT") != 0) &&
                                                                	 (si.compareToIgnoreCase("COLUMN") != 0) ) {
                                                                   do_error( false , "Error I parsing <colname> " + tokenStack.get(i).token + " on " + tableName);
                                                                   inTable=false;
                                                                   inColumn=false;
                                                                   break;
                                                                 }
                                                                 else {
                                                                   colName = si;
                                                                 }
                                                                }
                                               }
                                               else colName = tokenStack.get(i).value;
                                               //
                                    xField = new infaSourceField(colName,xMSet.getNextUID());
                                    fieldNumber++;
                                    xField.fieldNumber = fieldNumber;
                                }
                                // <colname> DATATYPE ( x , y ) 
                                if( (colpos == 1) && (skipUntilComma==false) ) {
                                               // dit moet een value hebben
                                               // KB 25 augustus - error caused by clubbing together name and datatype
                                			   // DATATYPES are not treated as tokens
                                               if( tokenStack.get(i).value == null ) {
                                            	   if( tokenStack.get(i).token == PLAINSQL.CHAR ) {  // CHAR is both a SQL token as a datatype
                                            		   tokenStack.get(i).value = "CHAR";
                                            	   }
                                            	   else {
                                                                  do_error( false , "Error II parsing <colname> " + tokenStack.get(i).token + " on " + tableName + "." + colName + " " + tokenStack.get(i).originalText);
                                                                  inTable=false;
                                                                  inColumn=false;
                                                                  break;
                                            	   }               
                                               }
                                               String sDataType = tokenStack.get(i).value.trim().toUpperCase();
                                               //  Oracle substitute
                                               if( (dbmake == rdbmsDatatype.DBMAKE.ORACLE) && (sDataType.compareToIgnoreCase("NUMBER")==0) ) {
                                            	               // NUMBER ( x , y ) 
                                                               if(  (tokenStack.get(i+1).token == PLAINSQL.LEFTPARENTESIS) && (tokenStack.get(i+3).token == PLAINSQL.COMMA) ) sDataType = "NUMBERPS";
                                                               // NUMBER ( x )  KB 9 OCTOBER
                                                               if(  (tokenStack.get(i+1).token == PLAINSQL.LEFTPARENTESIS) && (tokenStack.get(i+3).token == PLAINSQL.RIGHTPARENTESIS) ) {
                                                            	   xField.DataType = "NUMBER";
                                                            	   expectPrecision = true;
                                                            	   continue;
                                                               }
                                                               //logit(9,"GOT NUMBER on " + colName +  "->" + sDataType );
                                               }
                                               // NETEZZA substitute
                                               if( (dbmake == rdbmsDatatype.DBMAKE.NETEZZA) && (sDataType.compareToIgnoreCase("CHARACTER")==0) ) {
                                            	   sDataType = "CHAR";
                                               }
                                               //
                                               String validatedDatatype = rdbmsdt.getDatatypeString( dbmake , sDataType );
                                               if( validatedDatatype == null ) {
                                                     do_error( false , "Unknown type [" + tokenStack.get(i).value + "] on [" + tableName + "." + colName + "] for [" + dbmake + "]");
                                                     inTable=false;
                                                     inColumn=false;
                                                     break;
                                               }
                                               else {
                                                colType = validatedDatatype;
                                                xField.DataType = colType.trim().toUpperCase();
                                                // do we expect precision and scale
                                                pcDevBoosterSettings.TRISTATE itri = rdbmsdt.expectPrecision( dbmake , xField.DataType );
                                                if( itri == pcDevBoosterSettings.TRISTATE.NULL ) {
                                                                 inTable=false;
                                                                 inColumn=false;
                                                                 break;
                                                }
                                                expectPrecision = (itri == pcDevBoosterSettings.TRISTATE.YES) ? true : false;
                                                //
                                                // KB 10 Dec - special CHAR zondermeer is OK en wordt naar CHAR(1) omgezet
                                                if( (expectPrecision == true) && (xField.DataType.toUpperCase().startsWith("CHAR")) ) {
                                                	if( tokenStack.get(i+1).token != null ) {
                                                	  if( tokenStack.get(i+1).token != PLAINSQL.LEFTPARENTESIS ) {
                                                		expectPrecision = false;
                                                		xField.Precision = 1;
                                                		xField.scale = 0;
                                                	  }
                                                	}
                                                }
                                                //
                                                continue;       
                                               }
                                } // colpos = 1
                                // pre-empten
                                if( (expectPrecision) && (skipUntilComma==false) ) {
                                    expectPrecision = false;
                                               // ( a ) or ( a , b )
                                               int error = 0;
                                               if( tokenStack.get(i).token != PLAINSQL.LEFTPARENTESIS ) error=1;
                                               if( tokenStack.get(i+2).token == PLAINSQL.RIGHTPARENTESIS ) {
                                            	                 //logit( 9,"PREC" + tokenStack.get(i+1).value);
                                                                 int iPrec = xU.NaarInt(tokenStack.get(i+1).value);
                                                                 if( iPrec <= 0 ) {
                                                                                 error = 4;
                                                                 }
                                                                 else {
                                                                                 xField.Precision =iPrec;
                                                                                 xField.scale = 0;   // DECIMAL(n)
                                                                 }              
                                               }                              
                                               else 
                                               if( tokenStack.get(i+2).token == PLAINSQL.COMMA ) {
                                                               if(  tokenStack.get(i+4).token == PLAINSQL.RIGHTPARENTESIS  ) {
                                                     int iPrec  = xU.NaarInt(tokenStack.get(i+1).value);
                                                     int iScale = xU.NaarInt(tokenStack.get(i+3).value);
                                                     if( (iPrec <= 0) || (iScale < 0) ) {
                                              error = 5;
                                                                 }
                                                                 else {
                                                                                 xField.Precision =iPrec;
                                                                                 xField.scale = iScale;
                                                                 }            
                                                               }
                                                               else error = 3;
                                               }                              
                                               else //  for ORACLE this is acceptable  CHAR ( n BYTE ) or ( n CHAR)
                                                               if( ((tokenStack.get(i+2).token == PLAINSQL.BYTE) && (dbmake == rdbmsDatatype.DBMAKE.ORACLE)) ||
                                                            	   ((tokenStack.get(i+2).token == PLAINSQL.CHAR) && (dbmake == rdbmsDatatype.DBMAKE.ORACLE))	   ) {
                                                                               if( tokenStack.get(i+3).token == PLAINSQL.RIGHTPARENTESIS ) {
                                                                                              int iPrec = xU.NaarInt(tokenStack.get(i+1).value);
                                                                                              if( iPrec <= 0 ) {
                                                                                                              error = 4;
                                                                                              }
                                                                                              else {
                                                                                                              xField.Precision =iPrec;
                                                                                                              xField.scale = 0;   // CHAR(n BYTE)
                                                                                              }                              
                                                                               }
                                                                               else error = 2;
                                                               }
                                               else error = 2;   
                            //
                                               if ( error != 0 ) {
                                                                 String ss="";
                                                                 switch(error)
                                                                 {
                                                                 case  1 : {ss = "Missing right parentesis"; break;}
                                                                 case  2 : {ss = "Missing left parentesis"; break;}
                                                                 case  3 : {ss = "Missing left parentesis after comma"; break; }
                                                                 case  4 : {ss = "Conversion error on precision"; break; }
                                                                 case  5 : {ss = "Conversion error on scale"; break; }
                                                                 default : {ss = "Unknown error"; break; }
                                                                 }
                                                                 inTable = false;
                                                                 do_error( false , "Precision/scale error (" + ss + " [" + error +"]) on " + tableName + "." + colName + " " +
                                                                           " -> " + tokenStack.get(i).token + " " + tokenStack.get(i).value + " " + 
                                                                                                              tokenStack.get(i+1).token + " " + tokenStack.get(i+1).value  );
                                               }      
                                }              
                                //
                                if( (tokenStack.get(i).token != null) && (skipUntilComma==false) ) {
                                    if( tokenStack.get(i).token == PLAINSQL.NOTNULL ) {
                                        xField.mandatory = true;
                                        continue;
                                    }
                                }
                                
                                //  een komma of wel een afsluitend )
                                if( ((tokenStack.get(i).token == PLAINSQL.COMMA) && (haakjesDiepte==1)) || 
                                    ((tokenStack.get(i).token == PLAINSQL.RIGHTPARENTESIS)  && (haakjesDiepte==0)) ) {
                                                colpos =-1;
                                                colName =null;
                                                colType=null;
                                                // add Column to the list
                                                if( skipUntilComma == false ) xSource.fieldList.add(xField);
                                                skipUntilComma=false;
                                                //logit(5,"added " + xSource.Name + "." + xField.Name + " P=" + xField.Precision + " S=" + xField.scale );
                                                
                                                // last column ?
                                                if((tokenStack.get(i).token == PLAINSQL.RIGHTPARENTESIS)  && (haakjesDiepte==0)) {
                                                	//logit(5,"Last col for [" + xSource.Name + "] is [" + xField.Name + "]");
                                                	inColumn = false;
                                                }
                                                continue;
                                }  // komma of afsluitend
                                
                    }  //  end of IN COLUMN
                  
                    
                    //================================= CONSTRAINTS   (IN TABLE and not IN COLUMN)
                    if( (tokenStack.get(i).token != null) || (rescindconstraint==true) ) {
                    	             
                                   if( (tokenStack.get(i).token == PLAINSQL.CONSTRAINT) || (rescindconstraint==true)  || (toadPrimarySyntax==true) ) {
                                	           inconstraint=true;  
                                               constraintStack=null;
                                               constraintStack = new ArrayList<Element>();
                                               if( rescindconstraint ) {  // add CONSTRAINT to the stack
                                            	   rescindconstraint = false;
                                            	   Element x = new Element( PLAINSQL.CONSTRAINT , null , "CONSTRAINT");
                                            	   constraintStack.add( x );
                                               }
                                               if( toadPrimarySyntax ) {  // add CONSTRAINT <name>
                                            	   toadPrimarySyntax = false;
                                            	   Element x = new Element( PLAINSQL.CONSTRAINT , null , "CONSTRAINT");
                                            	   constraintStack.add( x );
                                            	   String pkName = ("PK_" + tableName.trim()).toUpperCase();
                                            	   Element y = new Element( null , pkName , pkName );
                                            	   constraintStack.add( y );
                                            	   Element z = new Element( PLAINSQL.PRIMARY , null, "PRIMARY" );
                                            	   constraintStack.add( z );
                                            	   //logit(9,"Fixed TOAD issue PRIMARY " + pkName );
                                               }
                                   }
                                   if( (tokenStack.get(i).token == PLAINSQL.SEMICOLON) && (haakjesDiepte==0) ) {
                                                               inTable = false;
                                                               inExternalTable = false;
                                                               inconstraint=false;
                                                   srcList.add(xSource);   // end of create table statement
                                                   if( xSource.flafle != null ) {
                                                    logit(5,"Added [" + xSource.Name + "] [Cols=" + xSource.fieldList.size() + "] [Cons=" + xSource.constraintList.size() + "] [Ext=" + xSource.flafle.extTablePositionList.size() + "]");
                                                    //logit(5 , "" + xSource.flafle.extTablePositionList.get(0).x);
                                                   }
                                                   else
                                                   logit(5,"Added [" + xSource.Name + "] [Cols=" + xSource.fieldList.size() + "] [Cons=" + xSource.constraintList.size() + "]");
                                                   	   
                                               }
                    } //  CONSTRAINTS 
                               
                    //===========================  In CONSTRAINT  ( read until , or last ) , ie. haakjesdiepte = 0
                    if( inconstraint ) {
                                if( ((tokenStack.get(i).token == PLAINSQL.COMMA) && (haakjesDiepte==1)) ||
                                               ((tokenStack.get(i).token == PLAINSQL.RIGHTPARENTESIS) && (haakjesDiepte==0)) ) {
                                               // ADD
                                               infaConstraint cc = extractConstraint( constraintStack );
                                               constraintStack = null;
                                               inconstraint=false;
                                               if( cc == null ) {
                                            	   errit("Error parsing constraint");
                                            	   inTable=false;
                                            	   inColumn=false;
                                               }
                                               else {
                                                      shoConstraint( cc );
                                                      xSource.constraintList.add( cc );         
                                               }
                                               continue;
                                }
                                constraintStack.add( tokenStack.get(i) );
                    }
                    
                               //================================ ORACLE EXTERNAL TABLE
                               //  ORACLE external table
                               if( dbmake != rdbmsDatatype.DBMAKE.ORACLE ) continue;
                               if( tokenStack.get(i).token == PLAINSQL.ORCL_EXTERNAL_TABLE_START ) {
                                               inExternalTable = true;
                                               inExternalColumn = false;
                                               
                                               // you need to intialize the list becaise the order of cols in tabel != order in flatfile
                                               xSource.flafle.isFixedWidth = true;
                                               for(int k=0;k<xSource.fieldList.size();k++)
                                               {
                                                               infa2DCoordinate ip = new infa2DCoordinate(-1,-1);
                                                               xSource.flafle.extTablePositionList.add(ip);
                                               }
                               }
                               
                               //===========   IN EXTERNAL TABLE
                               if( inExternalTable == true ) {
                                               if( (tokenStack.get(i).token == PLAINSQL.LEFTPARENTESIS) && (tokenStack.get(i-1).value != null) ) {
                                                               if( tokenStack.get(i-1).value.trim().compareToIgnoreCase("FIELDS") == 0 ) {
                                                                              inExternalColumn = true;
                                                                              extColCount = -1;
                                                                              continue;
                                                               }
                                               }
                               }
                               
                               //============ IN EXTERNAL COLUMN
                               // COLNAME POSITION ( x : y ) ,    => 
                               // COLNAME POSITION ( x : y ) )   indien laatste
                               if( inExternalColumn  ) {
                                               extColCount++;
                                   //System.out.println( "" + i + " " + extColCount + " " + tokenStack.get(i).originalText);
                                               switch( extColCount )
                                               {
                                               case 0 : { 
                                                               String sExtColName = (tokenStack.get(i).originalText == null ) ? "null" : tokenStack.get(i).originalText.trim().toUpperCase();
                                                               if( sExtColName == null ) sExtColName = "null";
                                                               // verify whether external column name also exists as a column
                                                               sourceFieldIdx = -1;
                                                               extPositionStart = -1;
                                                               extPositionStop = -1;
                                                               for(int j=0;j<xSource.fieldList.size();j++)
                                                               {
                                                                              if( sExtColName.compareToIgnoreCase( xSource.fieldList.get(j).Name ) == 0 ) {
                                                                                              sourceFieldIdx = j;
                                                                                              break;
                                                                              }
                                                               }
                                                               if( sourceFieldIdx < 0 ) {
                                                                              do_error( false , "External column error on [" + tableName +"] - external column [" + sExtColName +"] does not feature as a column");
                                                               inTable=false;
                                                               break;
                                                               }
                                                               break;
                                               }
                                               case 1 : {
                                                               String sPos = (tokenStack.get(i).originalText == null) ? "null" : tokenStack.get(i).originalText.trim().toUpperCase();
                                                   if( sPos.compareToIgnoreCase("POSITION") != 0 ) {
                                                               do_error( false , "External column error on [" + tableName +"] - expected POSITION but got [" + tokenStack.get(i).originalText + "]");
                                                               inTable=false;
                                                               break;
                                                   }
                                                   break;
                                               }
                                               case 2 : {
                                                               boolean isOk = true;
                                                   PLAINSQL tipe = tokenStack.get(i).token;
                                                   if( tipe == null ) isOk=false;
                                                   else {
                                                               if( tipe != PLAINSQL.LEFTPARENTESIS ) isOk = false;
                                                   }
                                                   if( isOk == false ) {
                                                               do_error( false , "External column error on [" + tableName +"] - expected LEFTPARENTESIS but got [" + tokenStack.get(i).originalText + "]");
                                                               inTable=false;
                                                               break;
                                                   }
                                                               break;
                                               }
                                               case 4 : {
                                                               boolean isOk = true;
                                                   PLAINSQL tipe = tokenStack.get(i).token;
                                                   if( tipe == null ) isOk=false;
                                                   else {
                                                               if( tipe != PLAINSQL.COLON ) isOk = false;
                                                   }
                                                   if( isOk == false ) {
                                                               do_error( false , "External column error on [" + tableName +"] - expected COLON but got [" + tokenStack.get(i).originalText + "]");
                                                               inTable=false;
                                                               break;
                                                   }
                                                               break;
                                               }
                                               case 6 : {
                                                               boolean isOk = true;
                                                   PLAINSQL tipe = tokenStack.get(i).token;
                                                   if( tipe == null ) isOk=false;
                                                   else {
                                                               if( tipe != PLAINSQL.RIGHTPARENTESIS ) isOk = false;
                                                   }
                                                   if( isOk == false ) {
                                                               do_error( false , "External column error on [" + tableName +"] - expected RIGHTPARENTESIS but got [" + tokenStack.get(i).originalText + "]");
                                                               inTable=false;
                                                               break;
                                                   }
                                                               break;
                                               }
                                               case 7 : {
                                                               boolean isOk = true;
                                                   PLAINSQL tipe = tokenStack.get(i).token;
                                                   if( tipe == null ) isOk=false;
                                                   else {
                                                               if( (tipe != PLAINSQL.COMMA) && (tipe != PLAINSQL.RIGHTPARENTESIS) ) isOk = false;
                                                   }
                                                   if( isOk == false ) {
                                                               do_error( false , "External column error on [" + tableName +"] - expected RIGHTPARENTESIS/COMMA but got [" + tokenStack.get(i).originalText + "]");
                                                               inTable=false;
                                                               break;
                                                   }
                                                   // ok
                                                   extColCount = -1;
                                                   if( tipe == PLAINSQL.RIGHTPARENTESIS ) inExternalColumn = false;
                                                               break;
                                               }
                                               case 3 :
                                               case 5 : {
                                                               int ipos = xU.NaarInt( tokenStack.get(i).originalText );
                                                               if( ipos < 0 ) {
                                                                              do_error( false , "External column error on start/end position [" + tableName +"] - got [" + tokenStack.get(i).originalText + "]");
                                                               inTable=false;
                                                               break;
                                                               }
                                                               if( extColCount == 3 ) extPositionStart = ipos; 
                                                   else {
                                                                  extPositionStop = ipos;
                                                                  if( (sourceFieldIdx >= 0) && (sourceFieldIdx<xSource.fieldList.size()) && (extPositionStart>=0) ) {
                                                                                 xSource.flafle.extTablePositionList.get(sourceFieldIdx).x = extPositionStart;
                                                                                 xSource.flafle.extTablePositionList.get(sourceFieldIdx).y = extPositionStop;
                                                                                }
                                                   }
                                                               break;
                                               }
                                               default : {
                                                               do_error( false , "External column error on [" + tableName +"] - extColCounter too high [" + extColCount +"]");
                                                               inTable=false;
                                                               break;
                                               }
                                               }
                               } // EXTERNAL COLUMN
                               
                  }  // END of IN TABLE (sorry)
                }
    }
    
    //----------------------------------------------------------------
    private void shoConstraint( infaConstraint c)
    //----------------------------------------------------------------
    {
                logit( 9 , "CONSTRAINT : " + c.Name + " " + c.Tipe + " " + c.key_list.toString() + " " + c.ReferencedOwner + "." + c.ReferencedTableName + " " + c.ref_list.toString() );
    }
    
    //----------------------------------------------------------------
    private infaConstraint extractConstraint(ArrayList<Element> costck)
    //----------------------------------------------------------------
    {
                String sVerbose = "";
                for(int i=0;i<costck.size();i++)
                {
                               if( costck.get(i).token != null ) sVerbose += costck.get(i).token; 
                                                            else sVerbose += costck.get(i).value; 
                               sVerbose += " "; 
                }
   //logit(5 , "Processing constraint -> " + sVerbose );
                if( costck.size() < 6 ) {
                               errit("Cannot parse constraint [" + sVerbose + "]" );
                               return null;
                }
                // CONSTRAINT name {PRIMARY KEY/UNIQUE} ( list ) 
                // CONSTRAINT name FOREIGN KEY ( list ) REFERENCES ( list )
                infaConstraint cc = new infaConstraint();
                cc.Name = costck.get(1).value;
                if( cc.Name == null) {
                               errit("Constraint NAME is null [" + sVerbose + "]" );
                               return null;
               }
                cc.Name = cc.Name.trim().toUpperCase();
                if( cc.Name.length() == 0) {
                               errit("Constraint NAME is null [" + sVerbose + "]" );
                               return null;
                }
                if( costck.get(0).token != PLAINSQL.CONSTRAINT ) {
                               errit("Constraint does not start by CONSTRAINT [" + sVerbose + "]" );
                               return null;
                }
                boolean isOK=false;
                int pos = 3;
                if( costck.get(3).token == PLAINSQL.KEY ) {
                               if( costck.get(2).token == PLAINSQL.FOREIGN ) { isOK=true;  cc.Tipe = generatorConstants.CONSTRAINT.FOREIGN; }
                               else
                               if( costck.get(2).token == PLAINSQL.PRIMARY ) { isOK=true;  cc.Tipe = generatorConstants.CONSTRAINT.PRIMARY; }
                               else isOK = false;
                }
                else
                if( costck.get(2).token == PLAINSQL.UNIQUE ) {
                			   cc.Tipe = generatorConstants.CONSTRAINT.UNIQUE;
                               isOK = true;
                               pos = 2;
                }
                if( (isOK == false) || (cc.Tipe == generatorConstants.CONSTRAINT.UNKNOWN) ) {
                               errit("Constraint does not comprise {PRIMARY KEY / FOREIGN KEY + UNIQUE } [" + sVerbose + "]" );
                               return null;
                }
                pos++;
                if( costck.get(pos).token == null ) {
                               errit("Constraint does not comprise opening parenthesis 1 [" + sVerbose + "]" );
                               return null;
                }
                if( costck.get(pos).token != PLAINSQL.LEFTPARENTESIS ) {
                               errit("Constraint does not comprise opening parenthesis 2 [" + sVerbose + "]" );
                               return null;
                }
                // list uitlezen tot volgende parentesis
                for(int i=0;i<200;i++)
                {
                	pos++;
                	if( i > 20 ) {
                		errit("Probably too many elements in key field list[" + sVerbose + "]");
                		return null;
                	}
                	if( costck.get(pos).token == PLAINSQL.RIGHTPARENTESIS ) {
                		break;
                	}
                	if( costck.get(pos).token == PLAINSQL.COMMA ) {
                		continue;
                	}
                	if( costck.get(pos).token != null ) {
                		errit("There is a key word in the elements of the key field list [" + sVerbose + "]");
                		return null;
                	}
                	String sCol = costck.get(pos).value.trim().toUpperCase();
                	cc.key_list.add( sCol );
                }
                // exit if PRIMARY or UNIQUE
                if( cc.Tipe != generatorConstants.CONSTRAINT.FOREIGN ) return cc;
      
       
       				// foreign key
       			   pos++;
       			   if( costck.get(pos).token == null ) {
                               errit("FK Constraint does not comprise REFERENCES 1 [" + sVerbose + "]" );
                               return null;
                   }
                   if( costck.get(pos).token != PLAINSQL.REFERENCES ) {
                               errit("FL Constraint does not comprise REFERENCES 2 [" + sVerbose + "]" );
                               return null;
                   }
                   
                   //  Name of referenced table <db>.<owner>.<name>
                   int tiperef = 0;
                   if( costck.get(pos+2).token != null ) {
                	   if( costck.get(pos+2).token  == PLAINSQL.PERIOD ) tiperef = 1;  //  <owner>.<table>
                	   if( costck.get(pos+4).token != null )  {
                		   if( costck.get(pos+4).token  == PLAINSQL.PERIOD ) tiperef = 2;  //  <db>.<owner>.<table>
                      }
                   }
                   if( tiperef == 0 ) {
                	   pos++;
                	   if( costck.get(pos).value == null ) {
                               errit("FL Constraint does not comprise the name of referenced table [" + sVerbose + "]" );
                               return null;  
                	   }
                	   cc.ReferencedTableName = costck.get(pos).value.trim().toUpperCase();
                   }
                   if( tiperef == 2 ) { // negeer de databasename
                	   pos++;
                	   pos++;
                	   tiperef=1;
                   }
                   if( tiperef == 1 ) {
                	   pos++;
                	   if( costck.get(pos).value == null ) {
                               errit("FL Constraint does not comprise the name of owner.table [" + sVerbose + "]" );
                               return null;  
                	   }
                	   cc.ReferencedOwner = costck.get(pos).value.trim().toUpperCase();
                	   pos++;
                	   pos++;
                	   if( costck.get(pos).value == null ) {
                           errit("FL Constraint does not comprise the name of referenced table [" + sVerbose + "]" );
                           return null;  
                	   }
                	   cc.ReferencedTableName = costck.get(pos).value.trim().toUpperCase();
                   }
                   
                   // open de haakjes
                   pos++;
                   if( costck.get(pos).token == null ) {
                               errit("Constraint does not comprise opening parenthesis 1 [" + sVerbose + "]" );
                               return null;
                   }
                   if( costck.get(pos).token != PLAINSQL.LEFTPARENTESIS ) {
                               errit("Constraint does not comprise opening parenthesis 2 [" + sVerbose + "]" );
                               return null;
                   }
                   
                   // ref list
                   for(int i=0;i<200;i++)
                   {
                	   pos++;
                	   if( i > 20 ) {
                		   errit("Probably too many elements in key field list[" + sVerbose + "]");
                		   return null;
                	   }
                	   if( costck.get(pos).token == PLAINSQL.RIGHTPARENTESIS ) {
                		   break;
                	   }
                	   if( costck.get(pos).token == PLAINSQL.COMMA ) {
                		   continue;
                	   }
                	   if( costck.get(pos).token != null ) {
                		   errit("There is a key word in the elements of the referenced field list [" + sVerbose + "] " + costck.get(pos).token);
                		   return null;
                	   }
                	   String sCol = costck.get(pos).value.trim().toUpperCase();
                	   cc.ref_list.add( sCol );
                   }
                   if( cc.ref_list.size() != cc.key_list.size() ) {
                                errit("The number of key elements differs from number of referenced elements [" + sVerbose + "] " + costck.get(pos).token);
                     return null;
                   }
                   // OK
       return cc; 
    }
    
    //----------------------------------------------------------------
    private int getSourceIdxViaName(String stab)
    //----------------------------------------------------------------
    {
    	for(int i=0;i<srcList.size();i++)
    	{
    		if( srcList.get(i).Name.compareToIgnoreCase(stab) == 0 ) return i;
    	}
    	return -1;
    }
    
    //----------------------------------------------------------------
    private int getColumnIdxViaName(int tabidx , String colname)
    //----------------------------------------------------------------
    {
    	if( (tabidx<0) || (tabidx>= srcList.size())) return -1;
    	infaSource tab = srcList.get(tabidx);
    	for(int i=0;i<tab.fieldList.size();i++)
    	{
    	  if( tab.fieldList.get(i).Name.compareToIgnoreCase(colname) == 0) return i;	
    	}
    	return -1;
    }
    //----------------------------------------------------------------
    private boolean do_AlterTable(int start,int stop)
    //----------------------------------------------------------------
    {
    	String sL = "";
    	for(int i=start;i<=stop;i++) {
    		if( tokenStack.get(i).token != null )	sL += tokenStack.get(i).token + " ";
    		                                   else sL += tokenStack.get(i).value + " ";
    	}
    	//logit( 9 , sL );
    	
    	// ALTER TABLE a.b.c ADD ( CONSTRAINT ...  ) ;
    	// Read until ADD
    	int idx=-1;
    	int pdx = -1;
    	for(int i=start;i<stop;i++) {
    		if( tokenStack.get(i).token != PLAINSQL.ADD ) continue;
    		if( tokenStack.get(i+1).token != PLAINSQL.LEFTPARENTESIS ) continue;
    		idx=i+2;
    		pdx=idx-3;
    		break;
    	}
    	// KB - 24NOV    alter table a.b.c ADD CONSTRAINT - so without a round bracket
	    if( idx < 0 ) {
	    	for(int i=start;i<stop;i++) {
	    		if( tokenStack.get(i).token != PLAINSQL.ADD ) continue;
	    		if( tokenStack.get(i+1).token != PLAINSQL.CONSTRAINT ) continue;
	    		idx=i+1;
	    		pdx=idx-2;
	    		break;
	    	}
    	}
        //
    	if( idx < 0 ) {
    		errit("Cannot find 'ADD ( CONSTRAINT' nor 'ADD CONSTRAINT' in [" + sL + "] (1)");
    		return false;
    	}
    	// Table
        String stab = tokenStack.get(pdx).value;
        if( stab == null ) {
        	errit("Probably incorrect ALTER TABLE statement (1). Table [" + stab + "] name looks erroneous [" + sL + "]");
    		return false;
        }
        stab = stab.trim().toUpperCase();
    	int tabIdx = getSourceIdxViaName(stab);
        if( tabIdx < 0 ) {
        	errit("Cannot locate table [" + stab + "] used in [" + sL + "]");
        	return false;
        }
        
    	// Types
    	if( ((idx+5) > stop) || (tokenStack.get(idx).token == null) ) {
    		errit("Probably incorrect ALTER TABLE statement (2) [" + sL + "] ");
    		errit( "" + idx + " " + stop + " " + tokenStack.get(idx).token );
    		return false;
    	}
    	switch( tokenStack.get(idx).token )
    	{
    	case CONSTRAINT : break; 
    	default : {
    		logit(1,"Unsupported ALTER TABLE type [" + tokenStack.get(idx).token + "]   in [" + sL + "]");
    		return false;
    	  }
    	}
    	
    	// CURRENTLY only ADD CONSTRAINT
    	// Just reuse the exractconstrait by creating an element list
    	ArrayList<Element> list = new ArrayList<Element>();
    	for(int i=idx;i<=stop;i++)
    	{
    		list.add(  tokenStack.get(i) );
    	}
    	infaConstraint cc = extractConstraint( list );
    	if( cc == null )  return false;
    	// add
    	srcList.get(tabIdx).constraintList.add( cc );
    	int conIdx = srcList.get(tabIdx).constraintList.size() - 1;
    	logit(5,"Added a constraint to [" + srcList.get(tabIdx).Name + "] [C=" + (conIdx+1) + "]");
    	shoConstraint( srcList.get(tabIdx).constraintList.get(conIdx));
        return true;
    }
     
    //----------------------------------------------------------------
    private boolean do_comment(int start,int stop)
    //----------------------------------------------------------------
    {
    	String sL = "";
    	for(int i=start;i<=stop;i++) {
    		if( tokenStack.get(i).token != null )	sL += tokenStack.get(i).token + " ";
    		                                   else sL += tokenStack.get(i).value + " ";
    	}
    	
    	//  COMMENT ON {TABLE|COLUMN) a.b.c. IS singelquote
    	if( tokenStack.get(start+2).token != PLAINSQL.COLUMN ) {
    		logit(1,"Currently only COMMENT on COLUMN supported");
    		return true;
    	}
    	int idx=-1;
    	for(int i=start;i<stop;i++) {
    		if( tokenStack.get(i).token != PLAINSQL.IS ) continue;
    		if( tokenStack.get(i+1).token != PLAINSQL.SINGLEQUOTE ) continue;
    		idx=i+2;
    		break;
    	}
    	if( (idx < 0) || ((idx+3)>tokenStack.size()) ) {
    		errit("Probably wrong COMMENT statement [" + sL + "]");
    		return false;
    	}
    	// columnname and tabname
    	String colname = tokenStack.get(idx-3).value;
    	if( tokenStack.get(idx-4).token != PLAINSQL.PERIOD ) colname = null;
    	String tabname = tokenStack.get(idx-5).value;
        if( (colname==null) || (tabname==null) ) {
        	errit("Probably wrong COMMENT statement. Cannot determine column or tablename [" + sL + "]");
    		return false;
        }
        tabname = tabname.trim().toUpperCase();
        colname = colname.trim().toUpperCase();
        int tabIdx = getSourceIdxViaName(tabname);
        if( tabIdx < 0 ) {
        	errit("Cannot locate table [" + tabname + "] used in [" + sL + "]");
        	return false;
        }
        int colIdx = getColumnIdxViaName(tabIdx , colname);
        if( colIdx < 0 ) {
        	errit("Cannot locate column [" + tabname + "." + colname + "] used in [" + sL + "]");
        	return false;
        }
        //
        boolean isOK = false;
        String sComment = "";
        for(int i=idx;i<stop;i++) {
    		if( tokenStack.get(i+1).token == PLAINSQL.SINGLEQUOTE ) {
    			isOK=true;
    			break;
    		};
    		sComment += tokenStack.get(i).originalText + " ";
    	}
        if( isOK == false ) {
        	errit("Cannot locate closing single quote on [" + tabname + "." + colname + "] comment [" + sL + "]");
        	return false;
        }
        srcList.get(tabIdx).fieldList.get(colIdx).Description = sComment.trim();
        logit(9, "Added comment to [" + tabname + "." + colname + "] : " + sComment);
    	return true;
    }
    
    // validate whether the table is on the ignore list
    //----------------------------------------------------------------
    private boolean ignore_table(String sIn)
    //----------------------------------------------------------------
    {
    	String sTab = sIn == null ? "" : sIn.toUpperCase().trim();
    	if( sTab.length() == 0 ) {
    		errit("(ignore table) empty or null tablename");
    		return true;   // inverse logic
    	}
    	if( config == null ) return true;  // everything wil be ignored, config cannot be null
    	return config.isTableOnIgnoreList( sIn.trim() );
    }
    
}
