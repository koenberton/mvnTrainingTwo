package powerdesigner;

import java.util.ArrayList;

public class pwdReference {

	 String ConstraintName;
	 String ParentTable;    // referenced table
	 ArrayList<String> child_col_list;
	 ArrayList<String> parent_col_list;
	 
	 public pwdReference()
	 {
		 ConstraintName=null;
		 ParentTable=null;
		 child_col_list = new ArrayList<String>();
		 parent_col_list = new ArrayList<String>();
	 }
}
