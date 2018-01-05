package powerdesigner;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import generalpurpose.pcDevBoosterSettings;

public class pwdMartReadFile {
	
	pcDevBoosterSettings xMSet=null;
	ArrayList<pwdMartTable> tab_list = null;
	
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
	pwdMartReadFile(pcDevBoosterSettings xi)
	//----------------------------------------------------------------
	{
		xMSet = xi;
	}

	//----------------------------------------------------------------
	public ArrayList<pwdMartTable> readMartFile(String FName , boolean isStaging)
	//----------------------------------------------------------------
	{
		String FullName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Import" + xMSet.xU.ctSlash + FName.trim();
		if( xMSet.xU.IsBestand( FullName ) == false ) {
			errit("Cannot find data mart scope list [" + FullName + "]");
			return null;
		}
	    //
		tab_list = new ArrayList<pwdMartTable>();
		String[] arline = new String[100];
		try {
		    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(FullName),"UTF-8"));
	        String sLijn = null;
	        while ((sLijn=reader.readLine()) != null) {
	        	sLijn = sLijn.trim();
	        	if( sLijn.length() < 3 ) continue;
	        	if( sLijn.trim().startsWith("--") ) continue;
	        	//
	        	StringTokenizer st = new StringTokenizer(sLijn, " \t");
	        	for(int k=0;k<arline.length;k++) arline[k]=null;
	        	int k=0;
	    		while(st.hasMoreTokens()) 
	    		{ 
	    	      String sElem = st.nextToken().trim();
	    	      if( sElem == null ) continue;
	    	      arline[k] = sElem;
	    	      k++;
	            }
	    		//  Just reads the table
	    		if( isStaging ) {
	    			pwdMartTable  tab = getMakeEmptyTable( arline );
	    			continue;
	    		}
	    		// First col is PASSTHRU skip if not TRUE
	    		if( arline[0] == null ) arline[0] = "FALSE";
	    		if( arline[0].trim().compareToIgnoreCase("TRUE") != 0 ) continue;
	        	//
	        	pwdMartTable  tab = getMakeTable( arline );
	        	if( tab == null ) continue;
	        	pwdMartColumn col = extractCol( arline );
	        	if( col == null ) continue;
	        	tab.col_list.add( col );
	        }
	        reader.close();
	      
	        /* DEBUG  
	        for(int i=0;i<tab_list.size();i++)
	        {
	        	sLijn = tab_list.get(i).EntityName;
	        	for(int j=0;j<tab_list.get(i).col_list.size();j++) {
	        		sLijn += " " + tab_list.get(i).col_list.get(j).Name;
	        	}
	        	logit(1,sLijn);
	        }
	        */
	        
	        return tab_list;
		}
		catch(Exception e) {
			errit("Error reading data mart scope list [" + FullName + "]" + e.getMessage());
			return null;
		}
		
	}

	//----------------------------------------------------------------
	private int getTableIdx( String stab)
	//----------------------------------------------------------------
	{
	  for(int i=0;i<tab_list.size();i++)
	  {
		if( tab_list.get(i).TableName.trim().compareToIgnoreCase( stab.trim() ) == 0 ) return i;
	  }
	  return -1;
	}
	
	//----------------------------------------------------------------
	private pwdMartTable getMakeTable(String[] arElem)
	//----------------------------------------------------------------
	{
		//  Pasthrou - Datamart EntityName - Table Name - Field
		if( arElem[1] == null ) return null;
		if( arElem[2] == null ) return null;
		int idx =  getTableIdx( arElem[2] );
		if( idx >= 0 ) return tab_list.get(idx); 
		pwdMartTable t = new pwdMartTable( arElem[2].trim() );
	    t.DMEntityName = arElem[1].trim();
	    t.TableName = arElem[2].trim();
	    tab_list.add( t );
		return tab_list.get( tab_list.size() - 1 );
	}
	
	//----------------------------------------------------------------
	private pwdMartTable getMakeEmptyTable(String[] arElem)
	//----------------------------------------------------------------
	{
		    String stab = arElem[0];
			if( stab == null ) return null;
			int idx =  getTableIdx( stab );
			if( idx >= 0 ) return tab_list.get(idx); 
			pwdMartTable t = new pwdMartTable( stab.trim() );
		    t.TableName = stab.trim();
		    t.DMEntityName = stab.trim();
			if( arElem[1] != null )   t.DMEntityName = arElem[1].trim().toUpperCase(); 
		    tab_list.add( t );
			return tab_list.get( tab_list.size() - 1 );
	}

	//----------------------------------------------------------------
	private pwdMartColumn extractCol( String[] arElem )
	//----------------------------------------------------------------
	{
		String sName = (arElem[3] == null) ? "" : arElem[3];
	    if( sName.length() < 1) {
	    	errit("Got an Empty columnname");
	    	return null;
	    }
	    pwdMartColumn c = new pwdMartColumn( sName );
	    //
        return c;
	}
}
