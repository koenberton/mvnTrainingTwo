package generalpurpose;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class gpZipFileList 
{
    private boolean isOK=true;
    
	//-----------------------------------------------------------------------
    public gpZipFileList(String FNaam , ArrayList<String> list) 
    //-----------------------------------------------------------------------
    {      
    	    isOK = zipit( FNaam , list);
    }
      
    //-----------------------------------------------------------------------
    public boolean completedOK()
    //-----------------------------------------------------------------------
    {
    	return isOK;
    }
    
    //-----------------------------------------------------------------------
    private boolean zipit(String FDest , ArrayList<String> list)
    //-----------------------------------------------------------------------
    {
            
            try {
                  byte[] buffer = new byte[1024];
                  FileOutputStream fos = new FileOutputStream(FDest);
                  ZipOutputStream zos = new ZipOutputStream(fos);
                  //
                  for (int i=0; i < list.size(); i++) {
                        File srcFile = new File( list.get(i) );
                        FileInputStream fis = new FileInputStream(srcFile);
                        // begin writing a new ZIP entry, positions the stream to the start of the entry data
                        zos.putNextEntry(new ZipEntry(srcFile.getName()));
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                             zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();
                        // close the InputStream
                        fis.close();
                  }
                  // close the ZipOutputStream
                  zos.close();
                  return true;
            }
            catch (IOException ioe) {
                  System.err.println("Error creating zip file: " + ioe);
                  return false;
            }
      }

}
