package powerdesigner;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import generalpurpose.pcDevBoosterSettings;

public class pwdReadCache {

	pcDevBoosterSettings xMSet=null;
	private ArrayList<pwdTableCache> tlist = null;
	private int tablecacheCounter  = 0;
	private int columncacheCounter = 0;
	
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
		
	public pwdReadCache(pcDevBoosterSettings im)
	{
		xMSet = im;
		tlist = new ArrayList<pwdTableCache>();
	}
	
	
	//----------------------------------------------------------------
	public ArrayList<pwdTableCache> getCachedLookUps(String FName)
	//----------------------------------------------------------------
	{
		   if( xMSet.xU.IsBestand( FName ) == false ) {
			   errit("Cannot locate lookup file [" + FName + "]");
			   return null;
		   }
		  
		   //
		   String ENCODING = "UTF-8";
		   boolean inTable=false;
		   boolean inColumn=false;
		   tablecacheCounter  = 0;
		   columncacheCounter = 0;
		   boolean creaOK = true;
		   try {
		    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(FName),ENCODING));
	        String sLijn = null;
	        while ((sLijn=reader.readLine()) != null) {
	    	   String sClean = xMSet.xU.removeBelowIncludingSpaces( sLijn );
	    	   if( sClean.toUpperCase().startsWith("--GENERICINFORMATION") ) {
	    		   continue;
	    	   }
	    	   if( sClean.toUpperCase().startsWith("--TABLEINFORMATION") ) {
	    		   inTable=true;
	    		   inColumn=false;
	    		   continue;
	    	   }
	    	   if( sClean.toUpperCase().startsWith("--COLUMNINFORMATION") ) {
	    		   inTable=false;
	    		   inColumn=true;
	    		   continue;
	    	   }
	    	   if( sClean.toUpperCase().startsWith("--ENDOFLOOKUP") ) {
	    		   inTable=false;
	    		   inColumn=false;
	    		   continue;
	    	   }
	    	   //
	    	   if( inTable ) {
	    		 if( cacheTable(sLijn) == false ) {
	    			 creaOK=false;
	    			 break;
	    		 }
	    	   }
	    	   //
	    	   if( inColumn ) {
	    		 if( cacheColumn(sLijn) == false ) {
	      			 creaOK=false;
	      			 break;
	      		 } 
	    	   }
	        }
	        reader.close();
		   }
		   catch(Exception e ) {
			   errit("Error reading Lookup file [" + FName + "] " + e.getMessage());
			   return null;
		   }
		   if( creaOK == false ) tlist=null;
		   return tlist;	
		}
		
		
		//----------------------------------------------------------------
		private boolean cacheTable(String sIn)
		//----------------------------------------------------------------
		{
			StringTokenizer st = new StringTokenizer(sIn, "|");
			int counter=-1;
			pwdTableCache x = null;
			while(st.hasMoreTokens()) 
			{ 
			 String sElem = st.nextToken().trim();
			 counter++;
			 switch( counter )
			 {
			 case 0 : {
				 String sTableName = sElem.toUpperCase().trim();
				 x = new pwdTableCache();
				 x.Name = sTableName;
				 break;
			 }
			 case 1 : {
				 x.Code = sElem.toUpperCase();
				 break;
			 }
			 case 2 : {
				 if( sElem.compareToIgnoreCase("NULL") == 0 ) sElem=null;
				 x.Delimiter = sElem;
				 break;
			 }
			 case 3 : {
				 if( sElem.compareToIgnoreCase("NULL") == 0 ) sElem=null;
				 x.CR = sElem;
				 break;
			 }
			 case 4 : {
				 if( sElem.compareToIgnoreCase("NULL") == 0 ) sElem=null;
				 x.SourceTableName = sElem;
				 break;
			 }
			 default : {
				 errit("(cachetable) Unsupported counter [" + counter + "]");
				 return false;
			     }
			 }
			}
			if( x == null ) {
				errit("(cacheTable) System error");
				return false;
			}
			tablecacheCounter++;
			if( (tablecacheCounter == 1) ) return true;  // skip header with columnnames if all tables required
			tlist.add( x );
			return true;
		}

		//----------------------------------------------------------------
		private boolean cacheColumn(String sIn)
		//----------------------------------------------------------------
		{
				StringTokenizer st = new StringTokenizer(sIn, "|");
				int counter=-1;
				String sTableName=null;
				pwdColumnCache x = null;
				// Table | column | code | EDWCRC
				while(st.hasMoreTokens()) 
				{ 
				 String sElem = st.nextToken().trim();
				 counter++;
				 switch( counter )
				 {
				 case 0 : {
					 sTableName = sElem.toUpperCase().trim();
					 x = new pwdColumnCache();
					 break;
				 }
				 case 1 : {
					 x.ColumnName = sElem.toUpperCase();
					 break;
				 }
				 case 2 : {
					 x.ColumnCode = sElem.toUpperCase();
					 break;
				 }
				 case 3 : {
					 x.edwcrc = false;
					 if( sElem.compareToIgnoreCase("NULL") == 0 ) sElem="FALSE";
					 if( sElem.compareToIgnoreCase("TRUE") == 0 ) x.edwcrc = true;
					 break;
				 }
				 case 4 : {   // SourceColName is NULL in all cases excet ORCLEXT
					 if( sElem.compareToIgnoreCase("NULL") == 0 ) sElem=null;
					 x.SourceColumnName = (sElem == null ) ? null : sElem.toUpperCase();
					 break;
				 }
				 case 5 : {   // Datatype - added to detect NUMBERs on UCHP
					 if( sElem.compareToIgnoreCase("NULL") == 0 ) sElem=null;
					 x.DataType = (sElem == null ) ? "UNKNOWN" : sElem.toUpperCase();
					 break;
				 }
				 case 6 : {   // Mandatory - added for DATA Quality views
					 if( sElem.compareToIgnoreCase("NULL") == 0 ) sElem="FALSE";
					 if ( sElem.compareToIgnoreCase("TRUE") == 0 ) x.isMandatory = true;
					                                          else x.isMandatory = false;
					 break;
				 }
				 case 7 : {   // SOURCEDatatype - added to create DATA QUALITY views
					 if( sElem.compareToIgnoreCase("NULL") == 0 ) sElem=null;
					 x.SourceDataType = (sElem == null ) ? "UNKNOWN" : sElem.toUpperCase();
		//if( sIn.indexOf("SCC") >=0 ) errit( "[" + sIn + "] [" + sElem + "]");
					 break;
				 }
				 case 8 : {   // SOURCEMandatory - added for DATA Quality views
					 if( sElem.compareToIgnoreCase("NULL") == 0 ) sElem="FALSE";
					 if ( sElem.compareToIgnoreCase("TRUE") == 0 ) x.isSourceMandatory = true;
					                                          else x.isSourceMandatory = false;
					 break;
				 }
				 default : {
					 errit("(cachecolumn) Unsupported counter [" + counter + "]");
					 return false;
				     }
				 }
				}
				if( x == null ) {
					errit("(cacheColumn) System error");
					return false;
				}
				columncacheCounter++;
				if( (columncacheCounter == 1) ) return true;  // skip header if all required
				//
				int idx=-1;
			    for(int i=0;i<tlist.size();i++)
			    {
			    	if( tlist.get(i).Name.trim().compareToIgnoreCase( sTableName ) == 0 ) {
			    		idx=i;
			    		break;
			    	}
			    }
			    if( idx < 0 ) {
			    	 errit("(cachecolumn) cannot find table [" + sTableName + "] in cache to add column [" + x.ColumnName + "]");
			    	 return false;
			    }
			    tlist.get(idx).collist.add( x );
				return true;
		}

}
