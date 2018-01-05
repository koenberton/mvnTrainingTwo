package pcGenerator.powercenter;

import java.util.ArrayList;

import pcGenerator.generator.generatorConstants;


public class infaSourceKloon {

	//----------------------------------------------------------------
	public infaSourceKloon()
	//----------------------------------------------------------------
	{
	}

	//----------------------------------------------------------------
	public infaSource kloon(infaSource parent)
	//----------------------------------------------------------------
	{
			infaSource kloon = new infaSource(parent.Name , parent.tipe);
			//
			kloon.tipe          = parent.tipe;
			kloon.BusinessName  = parent.BusinessName;
		    kloon.Description   = parent.Description;
			kloon.Databasetype  = parent.Databasetype;
			kloon.Dbdname       = parent.Dbdname;
			kloon.Name          = parent.Name;
		    kloon.ObjectVersion = parent.ObjectVersion;
			kloon.OwnerName     = parent.OwnerName;
			kloon.Constraint    = parent.Constraint;
			kloon.TableOptions  = parent.TableOptions;
		    kloon.VersionNumber = parent.VersionNumber;
		    kloon.SQLOverride   = parent.SQLOverride;
		    //
		    for(int i=0;i<parent.fieldList.size();i++)
			{
				infaSourceField klf = new infaSourceField( parent.fieldList.get(i).Name , -1L);
				//
				klf.UID             = parent.fieldList.get(i).UID;
				klf.BusinessName    = parent.fieldList.get(i).BusinessName;
				klf.DataType        = parent.fieldList.get(i).DataType;
				klf.Description     = parent.fieldList.get(i).Description;
				klf.fieldNumber     = parent.fieldList.get(i).fieldNumber;
				klf.FieldProperty   = parent.fieldList.get(i).FieldProperty;
				klf.FieldType       = parent.fieldList.get(i).FieldType;
				klf.Hidden          = parent.fieldList.get(i).Hidden;
				klf.KeyType         = parent.fieldList.get(i).KeyType;
				klf.Length          = parent.fieldList.get(i).Length;
				klf.Level           = parent.fieldList.get(i).Level;
				klf.mandatory       = parent.fieldList.get(i).mandatory;
				klf.Name            = parent.fieldList.get(i).Name;
				klf.Occurs          = parent.fieldList.get(i).Occurs;
				klf.offset          = parent.fieldList.get(i).offset;
				klf.physicalLength  = parent.fieldList.get(i).physicalLength;
				klf.physicalOffset  = parent.fieldList.get(i).physicalOffset;
				klf.PictureText     = parent.fieldList.get(i).PictureText;
				klf.Precision       = parent.fieldList.get(i).Precision;
				klf.referencedField = parent.fieldList.get(i).referencedField;
				klf.referencedTable = parent.fieldList.get(i).referencedTable;
				klf.scale           = parent.fieldList.get(i).scale;
				klf.UsageFlags      = parent.fieldList.get(i).UsageFlags;
				klf.isFileNameField = parent.fieldList.get(i).isFileNameField;
				//
				kloon.fieldList.add(klf);
			}
		    //
		    kloon.flafle.CodePage             = parent.flafle.CodePage;
		    kloon.flafle.CodePage             = parent.flafle.CodePage;
		    kloon.flafle.Consecdelimiterasone = parent.flafle.Consecdelimiterasone;
		    kloon.flafle.Delimited            = parent.flafle.Delimited;
		    kloon.flafle.Delimiters           = parent.flafle.Delimiters;
		    kloon.flafle.EscapeCharacter      = parent.flafle.EscapeCharacter;
		    kloon.flafle.EscapeCharacter      = parent.flafle.EscapeCharacter;
		    kloon.flafle.LineSequential       = parent.flafle.LineSequential;
		    kloon.flafle.Multidelimitersasand = parent.flafle.Multidelimitersasand;
		    kloon.flafle.NullCharType         = parent.flafle.NullCharType;
		    kloon.flafle.Nullcharacter        = parent.flafle.Nullcharacter;
		    kloon.flafle.Padbytes             = parent.flafle.Padbytes;
		    kloon.flafle.QuoteCharacter       = parent.flafle.QuoteCharacter;
		    kloon.flafle.Repeatable           = parent.flafle.Repeatable;
		    kloon.flafle.RowDelimiter         = parent.flafle.RowDelimiter;
		    kloon.flafle.ShiftSensitiveData   = parent.flafle.ShiftSensitiveData;
		    kloon.flafle.Skiprows             = parent.flafle.Skiprows;
		    kloon.flafle.Striptrailingblanks  = parent.flafle.Striptrailingblanks;
		    kloon.flafle.isFixedWidth         = parent.flafle.isFixedWidth;
		    //
		    for(int i=0;i<parent.tableAttributeList.size();i++)
			{
		    	infaPair ip = new infaPair( parent.tableAttributeList.get(i).code , parent.tableAttributeList.get(i).value );
		    	kloon.tableAttributeList.add(ip);
			}
		    for(int i=0;i<parent.flafle.extTablePositionList.size();i++)
		    {
		      	infa2DCoordinate ip = new infa2DCoordinate( parent.flafle.extTablePositionList.get(i).x ,parent.flafle.extTablePositionList.get(i).y );
				kloon.flafle.extTablePositionList.add(ip);    	
		    }
		    for(int i=0;i<parent.constraintList.size();i++)
		    {
		    	infaConstraint co = new infaConstraint();
		    	co.Name 				= parent.constraintList.get(i).Name;
		    	co.Tipe 				= parent.constraintList.get(i).Tipe;
		    	co.ReferencedTableName 	= parent.constraintList.get(i).ReferencedTableName;
		    	co.ReferencedOwner 		= parent.constraintList.get(i).ReferencedOwner;
		    	for(int k=0;k<parent.constraintList.get(i).key_list.size();k++)
		    	{
		    		String sKey = parent.constraintList.get(i).key_list.get(k);
		    		co.key_list.add( sKey );
		    	}
		    	for(int k=0;k<parent.constraintList.get(i).ref_list.size();k++)
		    	{
		    		String sRef = parent.constraintList.get(i).ref_list.get(k);
		    		co.ref_list.add( sRef );
		    	}
		    	kloon.constraintList.add( co );
		    }
		    //
			return kloon;
	}
}
