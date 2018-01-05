package generalpurpose;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class gpUtils {

	private gpStringUtils strUtil = null;
	public char ctSlash = '\\';
	public String ctEOL = "\r\n";
	
			
	//---------------------------------------------------------------------------------
	public gpUtils()
	//---------------------------------------------------------------------------------
	{
		strUtil = new gpStringUtils();
		ctSlash = System.getProperty("file.separator").toCharArray()[0];
		ctEOL = System.getProperty("line.separator");
	}
	
	//
	//---------------------------------------------------------------------------------
	private void logit(int level , String sIn )
	//---------------------------------------------------------------------------------
	{
		if( level != 0) System.out.println(sIn); else System.err.println(sIn);
	}
	//
	//---------------------------------------------------------------------------------
    String RPad(String sIn, int len)
    //---------------------------------------------------------------------------------
    {
      if( sIn == null ) sIn ="";
      int j=len-sIn.length();
      for(int i=0;i<j;i++) sIn = sIn + " ";
      return sIn.substring(0,len);
    }
    //
	//---------------------------------------------------------------------------------
	public String LogStackTrace(Exception e)
	//---------------------------------------------------------------------------------
	{
			      try {
			        StringWriter sw = new StringWriter();
			        PrintWriter pw = new PrintWriter(sw);
			        e.printStackTrace(pw);
			        return sw.toString();
			      }
			      catch(Exception e2) {
			    	e.printStackTrace();
			        return "";
			      }
	} 
	//
	//---------------------------------------------------------------------------------
	public String GetFileSpecs(String sF)
	//---------------------------------------------------------------------------------
	{           String sTemp="";
			    File fObj = new File(sF);
			    if ( fObj.exists() == true )
			    {
				 if ( fObj.isFile() == true ) {
			     sTemp =  "NME=" + fObj.getName() +
				         "|MOD=" + fObj.lastModified() +   
				         "|LEN=" + fObj.length() + 
				         "|PAR=" + fObj.getParent();
				 }
			    } 
				return sTemp;
	}
	//
	//---------------------------------------------------------------------------------
	public String GetFileName(String sF)
	//---------------------------------------------------------------------------------
	{           
			    File fObj = new File(sF);
			    if ( fObj.exists() == true )
			    {
				 if ( fObj.isFile() == true ) return fObj.getName();
			    } 
				return null;
	}
	//
	//---------------------------------------------------------------------------------
	public String GetParent(String sF)
	//---------------------------------------------------------------------------------
	{           
			    File fObj = new File(sF);
			    if ( fObj.exists() == true )
			    {
				 if ( fObj.isFile() == true ) return fObj.getParent();
			    } 
				return null;
	}
	//
	//---------------------------------------------------------------------------------
	public long getModificationTime(String sF)
	//---------------------------------------------------------------------------------
	{         
			    File fObj = new File(sF);
			    if ( (fObj.exists() == true) && (fObj.isFile() == true) )
			    {
				 	 return fObj.lastModified();
			    }  
				return -1L;
	}
	//
	//---------------------------------------------------------------------------------
	public long getFileSize(String sF)
	//---------------------------------------------------------------------------------
	{         
			    File fObj = new File(sF);
			    if ( (fObj.exists() == true) && (fObj.isFile() == true) )
			    {
				 	 return fObj.length();
			    }  
				return -1L;
	}
	//
	//---------------------------------------------------------------------------------
	String LPadZero( String sIn , int lengte )
	//---------------------------------------------------------------------------------
	{   
			    	int ii;
			    	String sZero = "";
			    	for(ii=0;ii<lengte;ii++) sZero = sZero + "0";
			    	return sZero.substring(0,lengte-sIn.length()) + sIn;
	}
	//---------------------------------------------------------------------------------
	public String GetSuffix( String FNaam )
	//---------------------------------------------------------------------------------
	{
		try {
			  int idx =	FNaam.lastIndexOf('.');
			  if( idx < 0 ) return "";
		 	  return FNaam.substring(  idx + 1 , FNaam.length() ).toUpperCase();
		}
		catch( Exception e )  { return ""; }
	}
	//
	//---------------------------------------------------------------------------------
	public int TelDelims ( String sIn , char ctKar )
	//---------------------------------------------------------------------------------
	{   
	    return strUtil.TelDelims(sIn, ctKar);
	}
	//
	//---------------------------------------------------------------------------------
	public String GetVeld( String sIn , int idx , char delim )
	//---------------------------------------------------------------------------------
	{ 
		return strUtil.GetVeld(sIn, idx, delim);
	}
	//
	//---------------------------------------------------------------------------------
	String RemplaceerOLD( String sIn , String sPattern , String sReplace )
	// ---------------------------------------------------------------------------------
	{
		return strUtil.Remplaceer(sIn, sPattern, sReplace);
	}
	//
	//---------------------------------------------------------------------------------
	public String RemplaceerIgnoreCase( String sIn , String sPattern , String sReplace )
	// ---------------------------------------------------------------------------------
	{
        return strUtil.RemplaceerIgnoreCase(sIn, sPattern, sReplace);
	}
	//
	//---------------------------------------------------------------------------------
	public String Remplaceer( String sIn , String sPattern , String sReplace )
	// ---------------------------------------------------------------------------------
	{   
		return strUtil.Remplaceer(sIn,sPattern,sReplace);
	}

	//---------------------------------------------------------------------------------
	public int NaarInt(String sIn)
	//---------------------------------------------------------------------------------
	{
				 int ii=-1;
					
				 try {
					  ii=Integer.parseInt( sIn );
					  return ii;
					 }
					 catch ( NumberFormatException e)
					 {
						 return -1;
					 }
	}
	//
	//---------------------------------------------------------------------------------
	public long NaarLong(String sIn)
	//---------------------------------------------------------------------------------
	{
				 long ll=-1;
					
				 try {
					  ll=Long.parseLong( sIn );
					  return ll;
					 }
					 catch ( NumberFormatException e)
					 {
						 return -1;
					 }
	}
	//
	//---------------------------------------------------------------------------------
	public double NaarDouble(String sIn)
	//---------------------------------------------------------------------------------
	{
				 double ll=-1;
					
				 try {
					  ll=Double.parseDouble( sIn );
					  return ll;
					 }
					 catch ( NumberFormatException e)
					 {
						 return -1;
					 }
	}
	//---------------------------------------------------------------------------------
	public boolean ValueInBooleanValuePair(String sIn)
	//---------------------------------------------------------------------------------
	{
		   String sWaarde = this.GetVeld(sIn, 2 , '=');
		   if( sWaarde.trim().toUpperCase().compareTo("Y")==0 ) return true;
		   if( sWaarde.trim().toUpperCase().compareTo("YES")==0 ) return true;
		   if( sWaarde.trim().toUpperCase().compareTo("1")==0 ) return true;
		   if( sWaarde.trim().toUpperCase().compareTo("J")==0 ) return true;
		   if( sWaarde.trim().toUpperCase().compareTo("TRUE")==0 ) return true;
		   if( sWaarde.trim().toUpperCase().compareTo("ON")==0 ) return true;
		   if( sWaarde.trim().toUpperCase().compareTo("JA")==0 ) return true;
		   if( sWaarde.trim().toUpperCase().compareTo("OUI")==0 ) return true;
		   return false;	
	}
	//
	//---------------------------------------------------------------------------------
	public boolean IsDir( String sDir )
	//---------------------------------------------------------------------------------
	{
				try {
				 File fObj = new File(sDir);
				 if ( fObj.exists() == true )
				 {
					if ( fObj.isDirectory() == true ) return true;
				 }
				 return false;
				} catch ( Exception e ) {
					e.printStackTrace();
					return false;
				}
	}
	//
	//---------------------------------------------------------------------------------
	public boolean IsBestand( String sIn )
	//---------------------------------------------------------------------------------
	{
				if( sIn == null ) return false;
				try {
				 File fObj = new File(sIn);
				 if ( fObj.exists() == true )
				 {
					if ( fObj.isFile() == true ) return true;
				 } 
				 return false;
				} catch ( Exception e ) {
					e.printStackTrace();
					return false;
				}
	}
	//
	//---------------------------------------------------------------------------------
	public ArrayList<String> GetFilesInDir( String sDirName , String sPatroon)
	//---------------------------------------------------------------------------------
			{
				ArrayList<String> sLijst = new ArrayList<String>();
				File  dirObj = new File( sDirName );
				{
					if ((dirObj.exists() == true)  ) {
						if (dirObj.isDirectory() == true) {
							File [] fileList = dirObj.listFiles();
							for (int i = 0; i < fileList.length; i++) {
								if (fileList[i].isDirectory()) continue;
								if (fileList[i].isFile()) {
									if( sPatroon != null ) {
										/*
									  if ( fileList[i].getName().length() >= sPatroon.length() ) {
										if ( fileList[i].getName().substring(0,sPatroon.length()).compareToIgnoreCase(sPatroon)!=0) continue;
										sLijst.add(fileList[i].getName());
									  }
									*/
									 if ( fileList[i].getName().length() >= sPatroon.length() ) {
									 		if ( fileList[i].getName().toUpperCase().indexOf(sPatroon.toUpperCase()) < 0 ) continue;
											sLijst.add(fileList[i].getName()); 	}
									} else {
										sLijst.add(fileList[i].getName());
									}
								}
							}
						}
					}
				}		
				return sLijst;
	}
	//---------------------------------------------------------------------------------
	int countFilesInDir( String sDirName , String sPatroon)
	//---------------------------------------------------------------------------------
	{  int teller=0;
	
					
					File  dirObj = new File( sDirName );
					{
						if ((dirObj.exists() == true)  ) {
							if (dirObj.isDirectory() == true) {
								File [] fileList = dirObj.listFiles();
								for (int i = 0; i < fileList.length; i++) {
									if (fileList[i].isDirectory()) continue;
									if (fileList[i].isFile()) {
										if( sPatroon != null ) {
										  if ( fileList[i].getName().length() >= sPatroon.length() ) {
											if ( fileList[i].getName().substring(0,sPatroon.length()).compareToIgnoreCase(sPatroon)!=0) continue;
											teller++;
										  }
										} else {
											teller++;
										}
									}
								}
							}
						}
					}		
					return teller;
	}
			//
			//---------------------------------------------------------------------------------
			ArrayList<String> GetDirsInDir( String sDirName , String sPatroon)
			//---------------------------------------------------------------------------------
			{
				ArrayList<String> sLijst = new ArrayList<String>();
				File  dirObj = new File( sDirName );
				{
					if ((dirObj.exists() == true)  ) {
						if (dirObj.isDirectory() == true) {
							File [] fileList = dirObj.listFiles();
							for (int i = 0; i < fileList.length; i++) {
								if (fileList[i].isFile()) continue;
								if (fileList[i].isDirectory()) {
									if( sPatroon != null ) {
									  if ( fileList[i].getName().length() >= sPatroon.length() ) {
										if ( fileList[i].getName().substring(0,sPatroon.length()).compareToIgnoreCase(sPatroon)!=0) continue;
										sLijst.add(fileList[i].getName());
									  }
									} else {
										sLijst.add(fileList[i].getName());
									}
								}
							}
						}
					}
				}		
				return sLijst;
			}
			//
			//---------------------------------------------------------------------------------
			ArrayList<String> GetDirsInDir( String sDirName )
			//---------------------------------------------------------------------------------
			{
				return GetDirsInDir( sDirName , null);
			}
			//
			//---------------------------------------------------------------------------------
			public ArrayList<String> GetFilesInDirRecursive( String sDirName , String sPatroon)
			//---------------------------------------------------------------------------------
			{
				ArrayList<String> sLijst = new ArrayList<String>();
				File  dirObj = new File( sDirName );
				{
					if ((dirObj.exists() == true)  ) {
						if (dirObj.isDirectory() == true) {
							File [] fileList = dirObj.listFiles();
							for (int i = 0; i < fileList.length; i++) {
								// Afdalen
								if (fileList[i].isDirectory()) {
									ArrayList<String> xL = GetFilesInDirRecursive( fileList[i].getAbsolutePath() , sPatroon);
									for(int k=0;k<xL.size();k++) sLijst.add(xL.get(k));
								}
								if (fileList[i].isFile()) {
									if( sPatroon != null ) {
									  if ( fileList[i].getName().length() >= sPatroon.length() ) {
										if ( fileList[i].getName().substring(0,sPatroon.length()).compareToIgnoreCase(sPatroon)!=0) continue;
										sLijst.add(fileList[i].getAbsolutePath());
									  }
									} else {
										sLijst.add(fileList[i].getAbsolutePath());
									}
								}
							}
						}
					}
				}		
				return sLijst;
			}
			//
			//---------------------------------------------------------------------------------
			ArrayList<String> GetFilesInDir( String sDirName )
			//---------------------------------------------------------------------------------
			{
				return GetFilesInDir( sDirName , null);
			}
			//
	
			//---------------------------------------------------------------------------------
			boolean CreateDirectory(String sDirNaam)
			//---------------------------------------------------------------------------------
			{
				if( this.IsDir( sDirNaam ) ) return true; // bestaat
				boolean success = (new File(sDirNaam)).mkdir();
				if( success == true ) return this.IsDir( sDirNaam );
				return false;
			}
			//
			//---------------------------------------------------------------------------------
			public void copyFile(String sIn , String sOut) throws IOException 
			//---------------------------------------------------------------------------------
			{
				
				   InputStream in = null;
				   OutputStream out = null; 
				   byte[] buffer = new byte[16384];
				   try {
				      in = new FileInputStream(sIn);
				      out = new FileOutputStream(sOut);
				      while (true) {
				         synchronized (buffer) {
				            int amountRead = in.read(buffer);
				            if (amountRead == -1) {
				               break;
				            }
				            out.write(buffer, 0, amountRead); 
				         }
				      } 
				   } finally {
				      if (in != null) {
				         in.close();
				      }
				      if (out != null) {
				    	 out.flush();
				         out.close();
				      }
				   }
			    
			}
			//
			//---------------------------------------------------------------------------------
			public String ReadContentFromFile(String FNaam, int MaxLines)
			//---------------------------------------------------------------------------------
			{
				String sRet= "";
				int teller=0;
				try {
				  File inFile  = new File(FNaam);  // File to read from.
		       	  BufferedReader reader = new BufferedReader(new FileReader(inFile));
		       	  String sLijn = null;
		          while ((sLijn=reader.readLine()) != null) {
		        	teller++; if( teller > 1) sRet = sRet + "\n";
		        	sRet = sRet + sLijn;
		        	if( teller > MaxLines ) {
		        		sRet = sRet + "\n\n --> Maximum  number [" + MaxLines + "] of display lines has been reached. \n --> Use alternative editor to view file [" + FNaam + "]";
		        		break;
		        	}
		          }
		          reader.close();
		          return ( sRet );
				}
				catch (Exception e) {
					return ("Error reading file [" + FNaam + "]");
			    }
			}
			
		    //
			//---------------------------------------------------------------------------------
		    String getHTMLContent( String sIn )
		    //---------------------------------------------------------------------------------
		    {
		      if( sIn == null ) return null;
		      String sRet = "";
		      char[] buf = sIn.toCharArray();
		      boolean inBetweenTag=false;
		      for(int i=0;i<buf.length;i++)
		      {
		    	  if( buf[i] == '<' ) { inBetweenTag = true; continue; }
		    	  if( buf[i] == '>' ) { inBetweenTag = false; continue; }
		     	  if( inBetweenTag ) continue;
		     	  sRet = sRet + buf[i];
		      }
		      return sRet;
		    }
		    
		    //---------------------------------------------------------------------------------
			public boolean VerwijderBestand( String sIn)
			//---------------------------------------------------------------------------------
			{
		        File FObj = new File(sIn);
		        if ( FObj.isFile() != true ) {
		        	logit( 0 ,"ERROR '" + sIn + ") -> file not found");
		        	return false;
		        }
		        if ( FObj.getAbsolutePath().length() < 10 ) {
		        	logit( 0 , sIn + "->lijkt mij geen goed idee om te schrappen");
		        	return false;  // domme veiligheid
		        }
		        FObj.delete();
		        File XObj = new File(sIn);
		        if ( XObj.isFile() == true ) {
		        	logit(0,"ERROR" + sIn+ " -> could not be deleted");	
		        }
		        return true;
			}
			//---------------------------------------------------------------------------------
			public String HouLettersEnCijfers(String sIn)
			//---------------------------------------------------------------------------------
			{
				String sTemp = "";
			    char[] SChar = sIn.toCharArray();
			    for(int ii=0;ii<SChar.length;ii++) 
				{	
					if ( ((SChar[ii] >= '0') && (SChar[ii] <= '9')) ||
						 ((SChar[ii] >= 'A') && (SChar[ii] <= 'Z')) ||
						 ((SChar[ii] >= 'a') && (SChar[ii] <= 'z')) 
						) sTemp = sTemp + SChar[ii];
				}		
				return sTemp;
			    	
			}
			
			//
			//---------------------------------------------------------------------------------
		    public String GetValueFromTagBuffer( String sBuffer , String sTag )
		    //---------------------------------------------------------------------------------
		    {
		    	String sRet = null;
    	    	//
		    	if ( sTag == null ) return null;
		    	// 
		    	if( sBuffer.indexOf(sTag) < 0 ) return null;
		    	// <tag> </tag>
		    	if( sBuffer.indexOf( "<"+sTag+">") >= 0) {
		    		int istart = sBuffer.indexOf( "<"+sTag+">") + sTag.length() + 2;
		    		int istop  = sBuffer.indexOf( "</"+sTag+">");
		    		if( (istart <  istop) && (istop>=0) ) {
		    			try {
		    			 sRet = sBuffer.substring(istart,istop);
		    			}
		    			catch( Exception e) { sRet=null; };
		    		}
		    		return sRet;
		    	}
		    	
		        //  href=""   -> lees alles dat na = komt en tussen quotes
		    	int istart=-1;
		    	if( (istart=sBuffer.indexOf(" " + sTag + "=")) >= 0 ) {
		    		 String sTemp = sBuffer.substring(istart);   //  je hebt nu  tag="iets" , knip dus 2de value met delim "
		    		 sRet = strUtil.GetVeld(sTemp,2,'"');  
		    	}
		    	return sRet;
		    }
		    //
		 	//---------------------------------------------------------------------------------
			String getXMLValueIdx(String FNaam , String sTag  , int idx)
			//---------------------------------------------------------------------------------
			{
				int teller=0;
				if( this.IsBestand(FNaam) == false ) return null;
				String sText = this.ReadContentFromFile(FNaam, 1000);
				int aantal = this.TelDelims(sText,'\n');
		    	for(int i=0;i<=aantal;i++)
				{
					String sLijn = this.GetVeld(sText,(i+1),'\n').trim();
					if( sLijn == null ) continue;
					if (sLijn.length() == 0 ) continue;
					if( sLijn.indexOf("<"+sTag+">") < 0 )  continue;
					teller++;
					if( teller < idx ) continue;
					if( teller > idx ) break;
					sLijn = this.GetValueFromTagBuffer(sLijn,sTag);
					if( sLijn == null ) sLijn = "";
					return sLijn;
				}
				return null;
			}
			 //
		 	//---------------------------------------------------------------------------------
			String getXMLValue(String FNaam , String sTag  )
		 	//---------------------------------------------------------------------------------
			{
				return getXMLValueIdx(FNaam,sTag,1);
			}
            //
			//---------------------------------------------------------------------------------
			String getDirHash(String sDirName)
			//---------------------------------------------------------------------------------
			{
				String sRet = null;
			    String sNoHash=null;
				File  dirObj = new File( sDirName );
				{
					if ((dirObj.exists() == true)  ) {
						if (dirObj.isDirectory() == true) {
							File [] fileList = dirObj.listFiles();
							sNoHash = "";
							for (int i = 0; i < fileList.length; i++) {
								if (fileList[i].isDirectory()) continue;
								if (fileList[i].isFile()) {
									    String sSpec = this.GetFileSpecs( sDirName + "//" + fileList[i].getName());
									    sSpec = this.Remplaceer(sSpec,("PAR="+sDirName).trim(),"");
										sNoHash = sNoHash + sSpec;
								}
							}
						}
					}
				}		
				if( sNoHash == null ) {
					logit(0,"Cannot calculate MD5 hash for Directory [" + sDirName + "]");
					return null;
				}
				sRet = makeMD5Hex(sNoHash);
				//System.out.println("MD5 ->>"+sRet);
				return sRet;
			}
			//
			//---------------------------------------------------------------------------------
			String makeMD5Hex(String sIn)
			//---------------------------------------------------------------------------------
			{
				try {
					   String s = sIn;
					   MessageDigest md5 = MessageDigest.getInstance("MD5");
					   md5.update(s.getBytes(),0,s.length());
					   BigInteger xBig = new BigInteger(1,md5.digest());
					   String sRet = String.format("%032x",xBig);
					   return sRet.toUpperCase();

					} catch (Exception e) {
						logit(0,"MD5 checksum [" + sIn + "]");
					    logit(0,this.LogStackTrace(e));
					    return null;
					}
			}
			
			//
			//---------------------------------------------------------------------------------
			String removeXMLEscape(String sIn)
			//---------------------------------------------------------------------------------
			{
				String sRet = sIn;
				if( sRet.indexOf("&") < 0 ) return sRet;
				sRet = this.Remplaceer(sRet,"&amp;","&");
				sRet = this.Remplaceer(sRet,"&quot;", "\"");
				sRet = this.Remplaceer(sRet,"&lt;", "<");
				sRet = this.Remplaceer(sRet,"&gt;", ">");
				sRet = this.Remplaceer(sRet,"&apos;", "'");
				return sRet;
			}
			//
			//---------------------------------------------------------------------------------
			public String transformToXMLEscape(String sIn)
			//---------------------------------------------------------------------------------
			{
				String sRet = sIn;
				sRet = this.Remplaceer(sRet,"'" ,"µapos;");
				sRet = this.Remplaceer(sRet,">" ,"µgt;");
				sRet = this.Remplaceer(sRet,"<" ,"µlt;");
				sRet = this.Remplaceer(sRet,"\"","µquot;");
				sRet = this.Remplaceer(sRet,"'" ,"µapos;");
				sRet = this.Remplaceer(sRet,"&" ,"µamp;");
				sRet = this.Remplaceer(sRet,"µ" ,"&");
				return sRet;
			}
			//
			//---------------------------------------------------------------------------------
			public String getMSDosDrive(String sIn)
			//---------------------------------------------------------------------------------
			{
				String sRet = "";
				char[] SChar = sIn.toUpperCase().toCharArray();
			    int len = sIn.length();
			    if( len < 3 ) return null;
			    if( SChar[1] != ':') return null;
			    if( (SChar[0] >= 'A') && (SChar[0] <= 'Z') ) sRet = ""+SChar[0]+":";
			    return sRet;
			}
			//
		    // ---------------------------------------------------------------------------------
			public boolean isValidURL(String sUrl)
			// ---------------------------------------------------------------------------------
			{
				try
			    {
			         URL url = new URL(sUrl);
			         return true;
			    }catch(Exception e)
			    {
			    	 //logit("Not a valid URL [" + sUrl + "]");
			         //logit(LogStackTrace(e));
			         return false;
			    }
			}
			//
		    // ---------------------------------------------------------------------------------
			public boolean isGrafisch(String sF)
		    // ---------------------------------------------------------------------------------
			{
				if( sF.toLowerCase().endsWith(".jpg")) return true;
				if( sF.toLowerCase().endsWith(".jpeg")) return true;
				if( sF.toLowerCase().endsWith(".png")) return true;
				if( sF.toLowerCase().endsWith(".gif")) return true;
				return false;
			}
			//
		    // ---------------------------------------------------------------------------------
			public String getHostNameFromURL(String sUrl)
			// ---------------------------------------------------------------------------------
			{
				if( sUrl.indexOf("http")!=0) sUrl = "http://" + sUrl;
				try
			    {
			         URL url = new URL(sUrl);
			         return url.getHost();
			    }catch(Exception e)
			    {
			    	 //logit("Not a valid URL [" + sUrl + "]");
			         //logit(LogStackTrace(e));
			         return "";
			    }
			}
			
			//
		    // ---------------------------------------------------------------------------------
			public String[] sortStringArray(String[] in)
			// ---------------------------------------------------------------------------------
			{
				int aantal = in.length;
				String[] lst = new String[aantal];
				for(int i=0;i<aantal;i++) lst[i] = in[i];
				for(int i=0;i<aantal;i++)
				{
					boolean swap=false;
					for(int j=0;j<(aantal-1);j++)
					{
						if( lst[j].compareToIgnoreCase(lst[j+1]) > 0 ) {
							String s = lst[j];
							lst[j] = lst[j+1];
							lst[j+1] = s;
							swap=true;
						}
					}
					if( swap == false ) break;
				}
				return lst;
			}
			//
		    // ---------------------------------------------------------------------------------
			public String prntStandardDateTime(long l)
			// ---------------------------------------------------------------------------------
			{
				return prntDateTime(l,"dd-MMM-yyyy HH:mm:ss");
			}
			//
		    // ---------------------------------------------------------------------------------
			public String prntDateTime(long l,String sPattern)
			// ---------------------------------------------------------------------------------
			{
				Date date = new Date(l);
				SimpleDateFormat ft = new SimpleDateFormat (sPattern);
			    return ft.format(date).trim();
			}
			//
			//---------------------------------------------------------------------------------
			public String extractXMLValueBAD(String sLijn , String sTag)
			//---------------------------------------------------------------------------------
			{
		        if( sLijn.toUpperCase().indexOf("<"+sTag.toUpperCase()+">") < 0) return null;
		        String sRet = sLijn;
		        sRet = this.RemplaceerIgnoreCase(sRet,"<"+sTag+">","");
		        sRet = this.RemplaceerIgnoreCase(sRet,"</"+sTag+">","").trim();
		 		return sRet;
			}
			
			//---------------------------------------------------------------------------------
			public String extractXMLValue(String sLijn , String sTag)
			//---------------------------------------------------------------------------------
			{
				try {
				 String sRet = sLijn.toUpperCase();
		         int i = sRet.indexOf("<"+sTag.toUpperCase()+">");
		         if( i < 0) return null;
		         i += sTag.length() + 2;
		         int j = sRet.indexOf("</"+sTag.toUpperCase()+">");
		         if( j < 0) return null;
		         if (i > j ) return null; 
		         //System.err.println( "" + i + " " + j + " " + sLijn.substring(i,j) );
		         return sLijn.substring(i,j);
				}
				catch ( Exception e ) {
					System.err.println("System error extractXMLValue");
					return null;
				}
			}
			
			
			public URL maakFileURL(String FNaam)
			{
				if( IsBestand( FNaam ) == false ) return null;
			    URL imgURL = null;
			    String sURL = "file:///" + FNaam;
		        try {
		    		imgURL = new URL( sURL );
		    	}
		    	catch( Exception e ) {
		    	    	imgURL = null;
		    	    	System.err.println( "[" + sURL + "] not a valid url" + e.getMessage() );
		    	}
		        return imgURL;
			}
			
			public int getIdxFromList( String lst[] , String s)
			{
				for(int i=0;i<lst.length;i++)
				{
					if( s.compareToIgnoreCase( lst[i] ) == 0 ) return i;
				}
				return -1;
			}


    public String getEncoding(String FName)
	{
		String sEnc = testUTF(FName);
		if( sEnc == null ) return "ISO-8859-1";  // assume this to be the default
		return sEnc;
	}
	
	
	public String byteToHex(byte b)
	{
		  int k = b;
    	  if( k < 0 ) k += 256;
    	  String hex = Integer.toHexString(k); 
    	  if( hex.length()==1) hex = "0"+hex;
    	  return hex.toUpperCase();
	}
	
	 
	 
	   private String testUTF(String FName )
	   {
		   // BOM byte order mark  BE Big Endian LE Little Endian
		   // UTF32 BOM is 00 00 FE FF (for BE) or FF FE 00 00 (for LE).
		   // UTF16 BOM is FE FF (for BE) or FF FE (for LE)
		   int totalRead = 0;
		   try {
	          byte[] buffer = new byte[1024];
	          FileInputStream inputStream = new FileInputStream(FName);
	          int nRead = 0;
	          byte b='\0';
	          byte bs;
	          byte utf1 = '\0';
	          int byteCounter=0;
	          boolean gotC0=false;
	          byte b1='\0';
	          byte b2='\0';
	          byte b3='\0';
	          byte b4='\0';
	      	  boolean isUTF16=false;
        	  boolean isUTF32=false;
        	  boolean isUTF8=false;
              while( (nRead = inputStream.read(buffer)) != -1) {
	        	   
	               if( (isUTF16==true) || (isUTF32==true) || (isUTF8==true) ) break;             	   
	               totalRead += nRead;
	               for(int i=0;i<nRead;i++)
	               {
	                  b=(byte)buffer[i];
	                  byteCounter++;
	                 
	                  //UTF16 starts with FFFE or FEFF
	                  //UTF32 starts with 0000FEFF or FFFE0000
	                  if( byteCounter < 5 ) {
	                   if( byteCounter == 1 ) b1=(byte)(b & 0xff);
	                   if( byteCounter == 2 ) b2=(byte)(b & 0xff);
	                   if( byteCounter == 3 ) b3=(byte)(b & 0xff);
	                   if( byteCounter == 4 ) b4=(byte)(b & 0xff);
	                   //
	                   if( byteCounter == 4 ) {
	                	  //String header =  (byteToHex(b1).trim()+byteToHex(b2).trim()+byteToHex(b3).trim()+byteToHex(b4).trim()).trim().toUpperCase();
	                	  //System.out.println(header);
	                	  if( (b1==(byte)0x00) && (b2==(byte)0x00) && (b3==(byte)0xfe) && (b4==(byte)0xff) ) isUTF32=true;
	                	  if( (b1==(byte)0xff) && (b2==(byte)0xfe) && (b3==(byte)0x00) && (b4==(byte)0x00) ) isUTF32=true;
		                  if( isUTF32 == false ) {
		                	  if( (b1==(byte)0xff) && (b2==(byte)0xfe) ) isUTF16=true;
		                	  if( (b1==(byte)0xfe) && (b2==(byte)0xff) ) isUTF16=true;
		                  }
	                	  if( (isUTF32) || (isUTF16) ) break;
	                   }
	                  }
	                  else {  // only check for UTF8 once past the first 4 chars - FFFE and FEFF match C080
	                   //         
	                   //  Files with UTF8 charset comprise characters having {Cx8x} sequences
	                   //  110xxxxx 10xxxxxx  -> C0 en 80
	                   bs=b;
	                   if( gotC0 == false ) {
	               	    utf1 = (byte)(bs & 0xc0); 
	               	    if( utf1 == (byte)0xc0 ) { gotC0=true; }
	                   }
	                   // is there a 80
	                   else {
	                	  gotC0=false;
	                	  utf1 = (byte)(bs & 0x80); 
		               	  if( utf1 == (byte)0x80 ) {
		               	      isUTF8=true;
		               	      break;
		               	  }
		               }
		              }
		              
	               } // for   
	           } // while
	           //
	           inputStream.close();		
	           //
	           if( isUTF32 ) return "UTF-32";
	           if( isUTF16 ) return "UTF-16";
	           if( isUTF8  ) return "UTF-8";
	           return null;
	       }
			catch( Exception e ) {
				System.out.println( "Error analyzing [" + FName + "] " + e.getMessage() );
				return null;
			}
	   }
	   
	   
	   
	   public Object cloneObject(Object obj){
	        try{
	            Object clone = obj.getClass().newInstance();
	            for (Field field : obj.getClass().getDeclaredFields()) {
	                field.setAccessible(true);
	                if(field.get(obj) == null || Modifier.isFinal(field.getModifiers())){
	                    continue;
	                }
	                if(field.getType().isPrimitive() || field.getType().equals(String.class)
	                        || field.getType().getSuperclass().equals(Number.class)
	                        || field.getType().equals(Boolean.class)){
	                    field.set(clone, field.get(obj));
	                }else{
	                    Object childObj = field.get(obj);
	                    if(childObj == obj){
	                        field.set(clone, clone);
	                    }else{
	                        field.set(clone, cloneObject(field.get(obj)));
	                    }
	                }
	            }
	            return clone;
	        }catch(Exception e){
	            return null;
	        }
	    }
	   
	   
	 //---------------------------------------------------------------------------------
	  public String transformSpacesInQuotes(String sIn , char c)
	 //---------------------------------------------------------------------------------
	 {
			String sTemp = "";
		    char[] bfr = sIn.toCharArray();
		    boolean inQuote=false;
		    for(int i=0;i<bfr.length;i++) 
			{	
		    	if( bfr[i] == (char)'"') {
		    		inQuote = !inQuote;
		    	}
				if (inQuote ) {
					if( bfr[i] == (char)0x20 )  { sTemp = sTemp + c ; continue; }
				}
				sTemp = sTemp + bfr[i];
			}		
			return sTemp;
		    	
	}
	  
	//----------------------------------------------------------------
    public String dedup_spaces(String sIn)
    //----------------------------------------------------------------
    {
	    	  String sRet = "";
	    	  char[] buf = sIn.toCharArray();
		      for(int i=0;i<buf.length;i++)
		      {
		    	  if( buf[i] == (char)0x20 ) {
		    		  if( i == 0 ) continue;
		    		  if( buf[i-1] == (char)0x20 ) continue;
		    		  buf[i] = (char)0x20;
		    	  }
		     	  sRet = sRet + buf[i];
		      }
	    	  return sRet;
    }
    
	//---------------------------------------------------------------------------------
	public String removeBelowSpaces(String sIn)
	//---------------------------------------------------------------------------------
	{
			String sTemp = "";
		    char[] bfr = sIn.toCharArray();
		    for(int i=0;i<bfr.length;i++) 
			{	
				if ( bfr[i] >= (char)0x20) sTemp = sTemp + bfr[i];
			}		
			return sTemp;
		    	
	}
	//---------------------------------------------------------------------------------
	public String removeBelowIncludingSpaces(String sIn)
	//---------------------------------------------------------------------------------
	{
				String sTemp = "";
			    char[] bfr = sIn.toCharArray();
			    for(int i=0;i<bfr.length;i++) 
				{	
					if ( bfr[i] > (char)0x20) sTemp = sTemp + bfr[i];
				}		
				return sTemp;
			    	
	}
	
	//----------------------------------------------------------------
    public String compress_spaces(String sIn)
    //----------------------------------------------------------------
    {
    	  String sRet = "";
    	  char[] buf = sIn.toCharArray();
	      for(int i=0;i<buf.length;i++)
	      {
	    	  if( buf[i] == (char)'\t' ) buf[i] = (char)0x20;
	    	  if( buf[i] < (char)0x20 ) continue;
	    	  if( buf[i] == (char)0x20 ) {
	    		  if( i == 0 ) continue;
	    		  if( buf[i-1] <= (char)0x20 ) continue;
	    		  buf[i] = (char)0x20;
	    	  }
	    	  if( buf[i] == (char)'-' ) {
	    		  if( i < (buf.length - 1) ) {
	    			  if( buf[i+1] == (char)'-' ) break;
	    		  }
	    	  }
	     	  sRet = sRet + buf[i];
	      }
    	  return sRet;
    }
    
   //----------------------------------------------------------------
   public String justkeepthenumerics(String sIn)
   //----------------------------------------------------------------
   {
    	  String sRet = "";
    	  char[] buf = sIn.toCharArray();
	      for(int i=0;i<buf.length;i++)
	      {
	    	  if( (buf[i] < (char)'0') || (buf[i] > (char)'9') ) continue;
	    	  sRet = sRet + buf[i];
	      }
    	  return sRet;
    }
   
    //----------------------------------------------------------------
 	private int extractPrecSca(String sin, int loc)
 	//----------------------------------------------------------------
 	{
 		String sRet = (sin == null) ? "" : sin.trim();
 		if( sRet.length() <= 0 ) return -1;
 		if( (sRet.startsWith("(") == false) || (sRet.endsWith(")") == false) ) return -1;
 		int idx = sRet.indexOf(",");
 		if( idx < 0 )  return -1;
 		try {
 		 String sLeft = sRet.substring( 1 , idx ).trim();
 		 String sRight = sRet.substring( idx+1 , sRet.length() - 1).trim();
 		 //errit( sin + " " + sRet + "[" + sLeft + "] [" + sRight + "]");
 		 if( loc == 1 ) return NaarInt(sLeft);
 		 if( loc == 2 ) return NaarInt(sRight);
 		 return -1;
 		}
 		catch(Exception e) {
 			return -1;
 		}
 	}
 	//----------------------------------------------------------------
 	public int extractPrecision(String sin)
 	//----------------------------------------------------------------
 	{
 		return extractPrecSca(sin,1);
 	}
 	//----------------------------------------------------------------
 	public int extractScale(String sin)
 	//----------------------------------------------------------------
 	{
 		return extractPrecSca(sin,2);
 	}
 	
 	//
	//---------------------------------------------------------------------------------
	public boolean isWholeNumber(String sIn)
	//---------------------------------------------------------------------------------
	{
				 long ll=-1;
					
				 try {
					  ll=Long.parseLong( sIn );
					  return true;
					 }
					 catch ( NumberFormatException e)
					 {
						 return false;
					 }
	}
	//
	//---------------------------------------------------------------------------------
	public boolean isDouble(String sIn)
	//---------------------------------------------------------------------------------
	{
				 double ll=-1;
					
				 try {
					  ll=Double.parseDouble( sIn );
					  return true;
					 }
					 catch ( NumberFormatException e)
					 {
						 return false;
					 }
	}
	//
	//---------------------------------------------------------------------------------
	public boolean isBoolean(String sIn)
	//---------------------------------------------------------------------------------
	{
				if( sIn == null ) return false;
				String ss = sIn.trim().toUpperCase();
				if( ss.length() < 4 ) return false;
				if( ss.compareToIgnoreCase("TRUE") == 0 ) return true;
				if( ss.compareToIgnoreCase("FALSE") == 0 ) return true;
				return false;
	}
	//
	//---------------------------------------------------------------------------------
	public static boolean isNumeric(String sIn)
	//---------------------------------------------------------------------------------
	{
	    for (char c : sIn.toCharArray())
	    {
	        if( c == (char)'.' ) continue;
	        if( c == (char)'-' ) continue;
	        if (!Character.isDigit(c)) return false;
	    }
	    return true;
	}
}
