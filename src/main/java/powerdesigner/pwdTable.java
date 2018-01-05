package powerdesigner;

import java.util.ArrayList;

public class pwdTable {

	String Name;
	String Code;
	String Comment;
	String EntityL0;
	String EntityL1;
	boolean IsHub;
	boolean IsLink;
	boolean HasReference;
	boolean IsReferenced;
	boolean IsSattelite;
	boolean IsReference;
	String SourceFileDelimiter;
	String SourceFileName;
	String CR;
	String CR_Hist;
	String SourceTableName;
	//
	int dependency_depth;    // is the order of table CREATION  lowest depth is most referenced
	String OwnerName;
	//
	ArrayList<pwdColumn> col_list = null;
	ArrayList<pwdKey> key_list = null;
	ArrayList<pwdReference> ref_list = null;

	public pwdTable()
	{
		Name=null;
		Code=null;
		Comment=null;
		EntityL0=null;
		EntityL1=null;
		IsHub=false;
		IsLink=false;
		HasReference=false;
		IsReferenced=false;
		IsSattelite=false;
		IsReference=false;
		SourceFileDelimiter=null;
		SourceFileName=null;
		CR=null;
		CR_Hist=null;
		dependency_depth=0;
		SourceTableName=null;
		OwnerName="STUDENT15";
		//
		col_list = new ArrayList<pwdColumn>();
		key_list = new ArrayList<pwdKey>();
		ref_list = new ArrayList<pwdReference>();
	}
	
}
