package pcGenerator.powercenter;

import java.util.ArrayList;

import pcGenerator.generator.generatorConstants;

public class infaConstraint {

	 public String Name=null;
	 public generatorConstants.CONSTRAINT Tipe = null;
     public String ReferencedTableName=null;
     public String ReferencedOwner=null;
     
     public ArrayList<String> key_list=null;
     public ArrayList<String> ref_list=null;
     
     // Make sure to also update infaSourceKloon if you change the definition
     
     public infaConstraint()
     {
                    Name=null;
                    ReferencedOwner=null;
                    Tipe=generatorConstants.CONSTRAINT.UNKNOWN;
                    key_list = new ArrayList<String>();
                    ref_list = new ArrayList<String>();
     }
     
}
