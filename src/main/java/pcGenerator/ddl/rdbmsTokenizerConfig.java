package pcGenerator.ddl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import generalpurpose.pcDevBoosterSettings;


// KB 27OCT - added for ePurch project
// KB 29NOV - added DROP/COMMENT/WARNING options

public class rdbmsTokenizerConfig {
	
    pcDevBoosterSettings xMSet = null;
    
    class dataTypeMapping
    {
    	String ltipe;
    	String rtipe;
    	dataTypeMapping(String l , String r)
    	{
    		ltipe=l;
    		rtipe=r;
    	}
    }
    private ArrayList<dataTypeMapping> dataTypeMappingList = null;
    private ArrayList<String> tableIgnoreList = null;
    private String overruleDatabaseName=null;
    private String overruleOwnerName=null;
    private boolean suppressWarnings=false;
    private boolean suppressComments=false;
    private boolean suppressDrops=false;
    private boolean suppressPrimaryConstraint=false;
    private boolean suppressUniqueConstraint=false;
    private boolean suppressForeignConstraint=false;
    private boolean globalOK=true;
    
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
    public rdbmsTokenizerConfig(pcDevBoosterSettings im)
    //----------------------------------------------------------------
    {
    	xMSet = im;
    	tableIgnoreList = new ArrayList<String>();
    	dataTypeMappingList = new ArrayList<dataTypeMapping>();
    }
    
    //----------------------------------------------------------------
    public boolean parseConfig(String ConfigFileNameIn)
    //----------------------------------------------------------------
    {
     	if( ConfigFileNameIn == null ) return true;  // no config so OK
        if( readConfig(ConfigFileNameIn) == false ) return false;    
        sho();
        return globalOK;
    }
    
    //----------------------------------------------------------------
    private boolean readConfig(String ConfigFileNameIn)
     //----------------------------------------------------------------
    {
        if( xMSet.xU.IsBestand( ConfigFileNameIn ) == false ) {
        	errit("Cannot locate the DDL configuration file [" + ConfigFileNameIn + "]");
        	return false;
        }
    	try {
    	 BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ConfigFileNameIn),"UTF-8"));
         String sLijn = null;
         while ((sLijn=reader.readLine()) != null) {
            if( parseLine( sLijn ) == false ) return false;
         }
    	 reader.close();
       }
       catch(Exception e ) {
    	errit("Error reading configuration file [" + ConfigFileNameIn + "]" + e.getMessage() );
    	return false;
       }
       //
       return true;
    }	
    
    //----------------------------------------------------------------
    private boolean parseLine(String sLijn)
    //----------------------------------------------------------------
    {
        if( sLijn == null ) return true;
        String sClean = xMSet.xU.removeBelowIncludingSpaces(sLijn).trim();
        if( sClean.length() < 1) return true;
        if( sClean.startsWith("--") ) return true;
        if( sClean.indexOf("=") < 0 ) {
        	errit("Syntax error in configuration setting [" + sLijn + "]");
        	return false;
        }
        //
        if( sClean.toUpperCase().startsWith("OWNER=") )  {
        	overruleOwnerName = fetchRight(sLijn,"=");
        	if( overruleOwnerName == null ) {
                errit("system error I " + sLijn);
                return false;
        	}
        	overruleOwnerName = overruleOwnerName.trim();
        	return true;
        }
        //
        if( sClean.toUpperCase().startsWith("DATABASENAME=") )  {
        	overruleDatabaseName = fetchRight(sLijn,"=");
        	if( overruleDatabaseName == null ) {
                errit("system error II " + sLijn);
                return false;
        	}
        	overruleDatabaseName = overruleDatabaseName.trim();
        	return true;
        }
        //
        if( sClean.toUpperCase().startsWith("DROP=") )  {
         	suppressDrops = !naarBoolean(fetchRight(sLijn,"="),sLijn);
         	return true;
        }
        //
        if( sClean.toUpperCase().startsWith("COMMENT=") )  {
         	suppressComments = !naarBoolean(fetchRight(sLijn,"="),sLijn);
         	return true;
        }
        //
        if( sClean.toUpperCase().startsWith("WARNING=") )  {
         	suppressWarnings = !naarBoolean(fetchRight(sLijn,"="),sLijn);
         	return true;
        }
        //
        if( sClean.toUpperCase().startsWith("PRIMARYCONSTRAINT=") )  {
         	suppressPrimaryConstraint = !naarBoolean(fetchRight(sLijn,"="),sLijn);
         	return true;
        }
        if( sClean.toUpperCase().startsWith("UNIQUECONSTRAINT=") )  {
         	suppressUniqueConstraint = !naarBoolean(fetchRight(sLijn,"="),sLijn);
         	return true;
        }
        if( sClean.toUpperCase().startsWith("FOREIGNCONSTRAINT=") )  {
         	suppressForeignConstraint = !naarBoolean(fetchRight(sLijn,"="),sLijn);
         	return true;
        }
        //
        if( sClean.toUpperCase().startsWith("IGNORE=") )  {
        	String stab = fetchRight(sLijn,"=");
        	if( stab == null ) {
                errit("system error III " + sLijn);
                return false;
        	}
        	stab = stab.toUpperCase().trim();
        	if( stab.length() < 1 ) {
        		errit("DDL configuration file has an IGNORE command without right hand side");
        		return false;
        	}
        	tableIgnoreList.add( stab );
        	return true;
        }
        //
        if( sClean.indexOf("=>") >= 0 )  {
        	String sLeft = fetchLeft( sLijn , "=>");
        	String sRight = fetchRight( sLijn , "=>");
        	if( (sLeft == null) || (sRight==null) ) {
                errit("system error IV " + sLijn);
                return false;
        	}
        	sLeft = sLeft.toUpperCase().trim();
        	sRight = sRight.toUpperCase().trim();
        	if( (sLeft.length() == 0) || (sRight.length()==0) ) {
                errit("DDL configuration. There is an empty right or left hand side in the datatype mapping [" + sLijn + "]");
                return false;
        	}
        	// pampering for the Oracle NUMBER datatype
        	if( sLeft.compareToIgnoreCase("NUMBER")==0) sLeft = "NUMBER(15)";
        	//
        	sLeft = xMSet.xU.removeBelowIncludingSpaces(sLeft).trim();  // only the left hand side
        	//
        	dataTypeMapping x = new dataTypeMapping( sLeft , sRight );
            dataTypeMappingList.add( x );
        	return true;
        }
        // fall off the edge
        //
        errit("Unsupported configuration commmand in [" + sLijn + "]");
    	return false;
    }
    
    //----------------------------------------------------------------
    private String fetchRight(String sIn , String stok)
    //----------------------------------------------------------------
    {
    	int idx = sIn.indexOf(stok);
    	if( idx < 0 ) return null;
    	String sRet = null;
    	try {
    		sRet = sIn.substring( idx + stok.length() );
    		//errit( ""+ sIn + "===>" + sRet );
    		return sRet;
    	}
    	catch(Exception e) {
    		return null;
    	}
    }
    
    //----------------------------------------------------------------
    private boolean naarBoolean(String sin, String sComment)
    //----------------------------------------------------------------
    {
   	   if( sin == null ) sin = "";
       sin = sin.trim().toUpperCase();
       //
       if( sin.compareToIgnoreCase("YES")==0) return true;
       if( sin.compareToIgnoreCase("TRUE")==0) return true;
       if( sin.compareToIgnoreCase("ON")==0) return true;
       if( sin.compareToIgnoreCase("1")==0) return true;
       //
       if( sin.compareToIgnoreCase("NO")==0) return false;
       if( sin.compareToIgnoreCase("FALSE")==0) return false;
       if( sin.compareToIgnoreCase("OFF")==0) return false;
       if( sin.compareToIgnoreCase("0")==0) return false;
       //
 	   errit("Found unsupported logical switch [" + sin + "] on [" + sComment + "]");
 	   globalOK=false;
	   return false;
    }
    
    //----------------------------------------------------------------
    private String fetchLeft(String sIn , String stok)
    //----------------------------------------------------------------
    {
    	int idx = sIn.indexOf(stok);
    	if( idx < 0 ) return null;
    	String sRet = null;
    	try {
    		sRet = sIn.substring( 0 , idx  );
    		return sRet;
    	}
    	catch(Exception e) {
    		return null;
    	}
    }
    
    //----------------------------------------------------------------
    private void sho()
    //----------------------------------------------------------------
    {
      logit( 9 , dumpConfig());
    }
   
    //----------------------------------------------------------------
    public String dumpConfig()
    //----------------------------------------------------------------
    {
    	 String sRet = "";
         if( overruleOwnerName != null )  
         sRet += "-- Owner             [" + overruleOwnerName + "]" + xMSet.xU.ctEOL;
         if( overruleDatabaseName != null ) 
         sRet += "-- Databasename      [" + overruleDatabaseName + "]" + xMSet.xU.ctEOL;
         sRet += "-- Suppress Warnings [" + suppressWarnings + "]" + xMSet.xU.ctEOL;
         sRet += "-- Suppress DROP     [" + suppressDrops + "]" + xMSet.xU.ctEOL;
         sRet += "-- Suppress COMMENT  [" + suppressComments + "]" + xMSet.xU.ctEOL;
         sRet += "-- Primary Key       [" + !suppressPrimaryConstraint + "]" + xMSet.xU.ctEOL;
         sRet += "-- Unique Key        [" + !suppressUniqueConstraint + "]" + xMSet.xU.ctEOL;
         sRet += "-- Foreign Key       [" + !suppressForeignConstraint + "]" + xMSet.xU.ctEOL;
         sRet += "--" + xMSet.xU.ctEOL;
         //
         for(int i=0;i<tableIgnoreList.size();i++)
         {
       	 sRet += "-- IGNORE [" + tableIgnoreList.get(i) + "]"  + xMSet.xU.ctEOL;  
         }
         for(int i=0;i<dataTypeMappingList.size();i++)
         {
       	  dataTypeMapping x = dataTypeMappingList.get(i);  
       	  sRet += "-- DATATYPEMAPPING [" + x.ltipe + " => " + x.rtipe + "]" + xMSet.xU.ctEOL;
         }
         return sRet;
    }
    
    //----------------------------------------------------------------
    public boolean isTableOnIgnoreList(String stab)
    //----------------------------------------------------------------
    {
    	 if( stab == null ) return false;
    	 for(int i=0;i<tableIgnoreList.size();i++)
         {
            if( tableIgnoreList.get(i).compareToIgnoreCase( stab ) == 0 ) return true;
         }
    	 return false;
    }
    
    //----------------------------------------------------------------
    public String getOwnerFromConfig()
    //----------------------------------------------------------------
    {
    	return overruleOwnerName;
    }
    
    //----------------------------------------------------------------
    public String getDatabaseNameFromConfig()
    //----------------------------------------------------------------
    {
    	return overruleDatabaseName;
    }
    
    //----------------------------------------------------------------
    public String getOverruleDataType(String tipein)
    //----------------------------------------------------------------
    {
    	 if( tipein == null ) return null;
    	 for(int i=0;i<dataTypeMappingList.size();i++)
    	 {
          	  dataTypeMapping x = dataTypeMappingList.get(i);
          	  if( x.ltipe.compareToIgnoreCase(tipein.trim())==0) return x.rtipe;
         }
    	 //errit( "-->" + tipein + " not found");
    	 return null;
    }
   
    //----------------------------------------------------------------
    public boolean getSuppressWarnings()
    //----------------------------------------------------------------
    {
    	return suppressWarnings;
    }
    
    //----------------------------------------------------------------
    public boolean getSuppressDrops()
    //----------------------------------------------------------------
    {
    	return suppressDrops;
    }
    
    //----------------------------------------------------------------
    public boolean getSuppressComments()
    //----------------------------------------------------------------
    {
    	return suppressComments;
    }
    
    //----------------------------------------------------------------
    public boolean getSuppressPrimaryConstraint()
    //----------------------------------------------------------------
    {
    	return suppressPrimaryConstraint;
    }
    
    //----------------------------------------------------------------
    public boolean getSuppressUniqueConstraint()
    //----------------------------------------------------------------
    {
    	return suppressUniqueConstraint;
    }
    
    //----------------------------------------------------------------
    public boolean getSuppressForeignConstraint()
    //----------------------------------------------------------------
    {
    	return suppressForeignConstraint;
    }
    
    
}
