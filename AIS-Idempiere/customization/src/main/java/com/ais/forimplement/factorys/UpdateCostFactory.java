package com.ais.forimplement.factorys;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

import com.ais.forimplement.process.UpdateCost;

public class UpdateCostFactory implements  IProcessFactory{
	
	public ProcessCall newProcessInstance(String className) {
		if (className.equals("com.ais.forimplement.process.UpdateCost"))
	        return new UpdateCost();
	    else
	    
		return null;
	}
	

}
