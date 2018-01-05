package powerdesigner;

import java.util.ArrayList;

public class pwdMartTable {

	String DMEntityName;
	String TableName;
	ArrayList<pwdMartColumn> col_list=null;
	
	pwdMartTable(String sin)
	{
	  TableName =  DMEntityName = sin;	
	  col_list = new ArrayList<pwdMartColumn>();
	}
}
