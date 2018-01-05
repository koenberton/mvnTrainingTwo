package pcGenerator.generator;

import pcGenerator.ddl.readInfaXML;
import pcGenerator.powercenter.infaSource;

public class generatorSrcTgt {

	readInfaXML.ParseType tipe = null;
	String TableName=null;
	infaSource tab_obj=null;
	
	generatorSrcTgt(String si , readInfaXML.ParseType it)
	{
		TableName = si.trim().toUpperCase();
		tipe = it;
		tab_obj=null;
	}
}
