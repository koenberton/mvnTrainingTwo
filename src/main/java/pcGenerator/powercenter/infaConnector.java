package pcGenerator.powercenter;

import pcGenerator.powercenter.infaInstance.TRANSFORMATION_TYPE;

public class infaConnector {
	
	public String FromFieldName = null;
	public String FromInstanceName = null;
	public infaInstance.TRANSFORMATION_TYPE fromTransformationType = infaInstance.TRANSFORMATION_TYPE.UNKNOWN;
	public String toFieldName = null;
	public String toInstanceName = null;
	public infaInstance.TRANSFORMATION_TYPE toTransformationType = infaInstance.TRANSFORMATION_TYPE.UNKNOWN;
	public int stage;  // used for sorting
	
	public infaConnector(String fromFieldN , String fromInstN  , infaInstance.TRANSFORMATION_TYPE t1 ,
			            String toFieldN , String toInstN  , infaInstance.TRANSFORMATION_TYPE t2 , int istg)
	{
	  FromFieldName = fromFieldN;
	  FromInstanceName = fromInstN;
	  fromTransformationType = t1;
	  toFieldName = toFieldN;
	  toInstanceName = toInstN;
	  toTransformationType = t2;
	  stage = istg;
	}

}
