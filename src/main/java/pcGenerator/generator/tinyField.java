package pcGenerator.generator;

public class tinyField {
	
		int tableIdx = -1;
		String FieldName = null;
		int rank = -1;
		String PortName = null;
		String dqualfunction=null;
		tinyField(int tgtTIdx , String sIn)
		{
			tableIdx    = tgtTIdx;
			FieldName   = sIn;
			PortName    = null;
			rank        = -1;
			dqualfunction=null;
		}
	
}
