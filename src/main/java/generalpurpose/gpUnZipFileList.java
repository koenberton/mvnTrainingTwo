package generalpurpose;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


public class gpUnZipFileList {

  public gpUnZipFileList(String FZipName , String TargetDir , String pattern)
  {
	  unzipFileIntoDirectory(FZipName , TargetDir , pattern );
  }
  
  private void unzipFileIntoDirectory(String zipFile, String sDir , String pattern) 
  {
	if( pattern != null ) {
		if (pattern.trim().length() <= 0) pattern=null;
	}
	byte[] buffer = new byte[1024];
	File folder = new File(sDir);
	if ( folder.exists() == false ) {
	     System.err.println("Cannot locate directory [" + sDir + "]");
	     return;
	}
	if ( folder.isDirectory() == false ) {
		System.err.println("[" + sDir + "] is not a directory");
	    return;	
	}
	//
	try
	{
    	//get the zip file content
    	ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
    	//get the zipped file list entry
    	ZipEntry ze = zis.getNextEntry();
    	while(ze!=null){
    	    String fileName = ze.getName();
    	    File newFile = new File(sDir + File.separator + fileName);
            //new File(newFile.getParent()).mkdirs();
            if( pattern != null ) {
    	    	if ( fileName.toUpperCase().contains(pattern.toUpperCase()) == false ) {
    	    		ze = zis.getNextEntry();
    	    		continue;
    	    	}
    	    }
      //System.err.println("Extracting -> " + fileName);
            FileOutputStream fos = new FileOutputStream(newFile);             
            int len;
            while ((len = zis.read(buffer)) > 0) {
       		   fos.write(buffer, 0, len);
            }
            fos.close();   
            ze = zis.getNextEntry();
    	}
        zis.closeEntry();
    	zis.close();
    }
	catch(Exception e){
       e.printStackTrace(); 
    }
     
	
  }

}











