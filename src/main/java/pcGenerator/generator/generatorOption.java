package pcGenerator.generator;

public class generatorOption {
	
	generatorConstants.SRC_TGT_TIPE tipe=null;
	int index=-1;
	generatorConstants.OPTION option = null;
	String value;
	String raw;
	
	generatorOption()
	{
		tipe   = generatorConstants.SRC_TGT_TIPE.UNKNOWN;
		option = generatorConstants.OPTION.UNKNOWN;
		index  = -1;
		value  = null;
		raw    = null;
	}
}
