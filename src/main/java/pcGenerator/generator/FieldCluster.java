package pcGenerator.generator;

import java.util.ArrayList;

public class FieldCluster {
	
		int stage = -9;
		int portAddRequestCounter = 0;     // bit messy - used to detect when the last element is updated in order to trigger processing
		tinyField clstrTargetField;
		ArrayList<tinyField> clstrElementList =  null;
		//
		FieldCluster(int tgtTIdx , String sIn, int ist)
		{
			clstrTargetField = new tinyField(tgtTIdx,sIn);
			clstrElementList = new ArrayList<tinyField>();
			portAddRequestCounter=0;
			stage = ist;
		}
	
}
