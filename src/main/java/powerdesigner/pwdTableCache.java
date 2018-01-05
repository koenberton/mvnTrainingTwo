package powerdesigner;

import java.util.ArrayList;


public class pwdTableCache {
	String Name;
	String Code;
	String Delimiter;
	String CR;
	String SourceTableName;
	ArrayList<pwdColumnCache> collist=null;
	pwdTableCache()
	{
		Name=null;
		Code=null;
		Delimiter=null;
		CR=null;
		SourceTableName=null;
		collist = new ArrayList<pwdColumnCache>();
	}
}
