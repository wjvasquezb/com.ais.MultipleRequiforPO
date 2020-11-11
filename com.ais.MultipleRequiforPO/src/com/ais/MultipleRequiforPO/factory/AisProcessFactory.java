package com.ais.MultipleRequiforPO.factory;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

import com.ais.MultipleRequiforPO.process.RequisitionPOCreate;



public class AisProcessFactory implements IProcessFactory {

	@Override
	public ProcessCall newProcessInstance(String className) {
		
		/*if(className.equals("net.frontuari.process.ImportPayment"))
			return new ImportPayment();
		
		if(className.equals("net.frontuari.process.ImportProduct"))
			return new ImportProduct();*/
		if(className.equals("com.ais.customizations.process.RequisitionPOCreate"))
			return new RequisitionPOCreate();
		
		return null;
	}

}