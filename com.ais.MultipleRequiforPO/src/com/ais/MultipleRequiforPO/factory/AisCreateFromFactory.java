package com.ais.MultipleRequiforPO.factory;

import org.compiere.grid.ICreateFrom;
import org.compiere.grid.ICreateFromFactory;
import org.compiere.model.GridTab;
import org.compiere.model.I_C_Order;

import com.ais.MultipleRequiforPO.webui.form.WCreateFromRequisitionUI;


public class AisCreateFromFactory implements ICreateFromFactory {
	@Override
	public ICreateFrom create(GridTab mTab) 
	{
		String tableName = mTab.getTableName();
		if (tableName.equals(I_C_Order.Table_Name))
			return new WCreateFromRequisitionUI(mTab);
		
		return null;
	}
}

