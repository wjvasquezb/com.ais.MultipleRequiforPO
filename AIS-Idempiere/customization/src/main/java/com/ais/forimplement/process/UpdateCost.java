package com.ais.forimplement.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.logging.Level;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MClient;
import org.compiere.model.MCost;
import org.compiere.model.MCostElement;
import org.compiere.model.MCostType;
import org.compiere.model.MProduct;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereSystemError;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;


public class UpdateCost extends SvrProcess
{
	/**	Product Category		*/
	private int		p_M_Product_Category_ID = 0;
	private BigDecimal p_MAcctSchema=null;
	private BigDecimal	p_M_CostType_ID = null;
	private BigDecimal  p_M_CostElement_ID=null;
	private int 	p_M_PriceList_Version_ID = 0;
	
	private static final String	TO_AveragePO = "A";
	private static final String	TO_AverageInvoiceHistory = "DI";
	private static final String	TO_AveragePOHistory = "DP";
	private static final String	TO_FiFo = "F";
	private static final String	TO_AverageInvoice = "I";
	private static final String	TO_LiFo = "L";
	private static final String	TO_PriceListLimit = "LL";
	private static final String	TO_StandardCost = "S";
	private static final String	TO_FutureStandardCost = "f";
	private static final String	TO_LastInvoicePrice = "i";
	private static final String	TO_LastPOPrice = "p";

	/** Standard Cost Element		*/
	private MCostElement 	m_ce = null;
	/** Client Accounting SChema	*/
	private MAcctSchema[]	m_ass = null;
	/** Map of Cost Elements		*/
	private HashMap<String,MCostElement>	m_ces = new HashMap<String,MCostElement>();
	
	

	
	/**
	 * 	Prepare
	 */
	protected void prepare ()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			log.fine("prepare - " + para[i]);
			if (para[i].getParameter() == null);
				
			else if (name.equals("C_AcctSchema_ID"))
				p_MAcctSchema= (BigDecimal) para[i].getParameter();
			else if(name.equals("M_CostType_ID"))
				p_M_CostType_ID = (BigDecimal)para[i].getParameter();
			else if (name.equals("M_CostElement_ID"))
				p_M_CostElement_ID = (BigDecimal) para[i].getParameter();
			else if (name.equals("M_PriceList_Version_ID"))
				p_M_PriceList_Version_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);		
		}
	}	//	prepare	

	/**
	 * 	Process
	 *	@return info
	 *	@throws Exception
	 */
	protected String doIt() throws Exception
	{
		
		
		if(log.isLoggable(Level.INFO)) log.info("MAcctSchema=" + p_MAcctSchema + ", SetTypeCostTo=" + p_M_CostType_ID + ", MCostElement=" + p_M_CostElement_ID + ", M_PriceList_Version_ID=" + p_M_PriceList_Version_ID);
		if (p_MAcctSchema  == null)
			p_MAcctSchema  = new BigDecimal(0);
		if (p_M_CostType_ID  == null)
			p_M_CostType_ID  = new BigDecimal(0);
		if (p_M_CostElement_ID  == null)
			p_M_CostElement_ID  = new BigDecimal(0);
	
		MCostElement  mcostelement= new MCostElement(getCtx(), p_M_CostElement_ID.intValue(),null);
		//MAcctSchema macctSchema	= new MAcctSchema(getCtx(), p_MAcctSchema.intValue(),null);
		MCostType  costtipe = new MCostType(getCtx(), p_M_CostType_ID.intValue(),null);
		//	PLV required
		
		
		
		
		
		/***********/
		if (p_M_PriceList_Version_ID == 0
				&& (mcostelement.getCostingMethod().equals(TO_PriceListLimit) || mcostelement.getCostingMethod().equals(TO_PriceListLimit)))
				throw new AdempiereUserError ("@FillMandatory@  @M_PriceList_Version_ID@");
		
		/*****wv*****/
		if (!isValidCost(mcostelement.getCostingMethod()))
			throw new AdempiereUserError ("@NotFound@ @M_CostElement_ID@ (Future) " + p_M_CostType_ID);
				
		/**********/
		//	Prepare
		MClient client = MClient.get(getCtx());
		m_ce = MCostElement.getMaterialCostElement(client, MAcctSchema.COSTINGMETHOD_StandardCosting);
		
		
		if (m_ce.get_ID() == 0)
			throw new AdempiereUserError ("@NotFound@ @M_CostElement_ID@ (StdCost)");
		if (log.isLoggable(Level.CONFIG)) log.config(m_ce.toString());
		m_ass = MAcctSchema.getClientAcctSchema(getCtx(), client.getAD_Client_ID());
		for (int i = 0; i < m_ass.length; i++)
			createNewCost(m_ass[i]);
		commitEx();
		
		//	Update Cost
		int counter = updatecost();
		
		return "#" + counter;
	}	//	doIt
	
	

	private boolean isValidCost(String to)
	{
				
		if (to.equals(TO_AverageInvoiceHistory))
			to = TO_AverageInvoice;
		if (to.equals(TO_AveragePOHistory))
			to = TO_AveragePO;
		if (to.equals(TO_FutureStandardCost))
			to = TO_StandardCost;
		if (to.equals(TO_LastInvoicePrice))
			to = TO_LastInvoicePrice;
		if (to.equals(TO_LastPOPrice))
			to = TO_LastPOPrice;
		
		
		//
		if (to.equals(TO_AverageInvoice)
			|| to.equals(TO_AveragePO)
			|| to.equals(TO_FiFo)
			|| to.equals(TO_LiFo)
			|| to.equals(TO_StandardCost))
		{
			
			return false;
		}
		return true;
	}	//	isValid
	
	
	

	private void createNewCost (MAcctSchema as)
	{
		if (!as.getCostingLevel().equals(MAcctSchema.COSTINGLEVEL_Client))
		{
			String txt = "Costing Level prevents creating new Costing records for " + as.getName();
			log.warning(txt);
			addLog(0, null, null, txt);
			return;
		}
		String sql = "SELECT * FROM M_Product p "
			+ "WHERE NOT EXISTS (SELECT * FROM M_Cost c WHERE c.M_Product_ID=p.M_Product_ID"
			+ " AND c.M_CostType_ID=? AND c.C_AcctSchema_ID=? AND c.M_CostElement_ID=?"
			+ " AND c.M_AttributeSetInstance_ID=0) "
			+ "AND AD_Client_ID=?";
		
		int counter = 0;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			pstmt.setInt (1, as.getM_CostType_ID());
			pstmt.setInt (2, as.getC_AcctSchema_ID());
			pstmt.setInt (3, p_M_CostElement_ID.intValue());
			pstmt.setInt (4, as.getAD_Client_ID());
			rs = pstmt.executeQuery ();
		
			while (rs.next ())
			{
				if (createNew (new MProduct (getCtx(), rs, null), as))
					counter++;
			}
		}
		catch (Exception e)
		{
			log.log (Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		if (log.isLoggable(Level.INFO)) log.info("#" + counter);
		addLog(0, null, new BigDecimal(counter), "Created for " + as.getName());
	}	//	createNew
	
	
	
	
	/**
	 * 	Create New Client level Costing Record
	 *	@param product product
	 *	@param as acct schema
	 *	@return true if created
	 */
	private boolean createNew (MProduct product, MAcctSchema as)
	{
		MCost cost = MCost.get(product, 0, as, 0, p_M_CostElement_ID.intValue(), get_TrxName());
		if (cost.is_new())
			return cost.save();
		return false;
	}	//	createNew

	/*************************************************************************
	 *
	 * 
	 * 
	 */
	private int updatecost()
	{
		int counter = 0;
		String sql = "SELECT * FROM M_Cost c WHERE M_CostElement_ID=? and m_costtype_id =? and M_Product_ID in(1002723,1002709,1002700)";
		if (p_M_Product_Category_ID != 0)
			sql += " AND EXISTS (SELECT * FROM M_Product p "
				+ "WHERE c.M_Product_ID=p.M_Product_ID AND p.M_Product_Category_ID=?)";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			
			
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt (1, p_M_CostElement_ID.intValue());
			pstmt.setInt (2, p_M_CostType_ID.intValue());
			if (p_M_Product_Category_ID != 0)
				pstmt.setInt (2, p_M_Product_Category_ID);
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{				
				MCost cost = new MCost (getCtx(), rs, get_TrxName());
				for (int i = 0; i < m_ass.length; i++)
				{
					//	Update Costs only for default Cost Type
					if (m_ass[i].getC_AcctSchema_ID() == p_MAcctSchema.intValue() 
						&& m_ass[i].getM_CostType_ID() == p_M_CostType_ID.intValue())
					{
						if (m_ass[i].getC_AcctSchema_ID() == p_MAcctSchema.intValue()) 
						{
							//if (updatecost (cost,mcostelement.getCostingMethod()))
							if (updatecost (cost,m_ass[i].getCostingMethod()))
								counter++;
						}
						//else
						//{
						//	if (updatecost (cost, m_ass[i].getCostingMethod()))
								//counter++;
					//	}
					}
				}
				}
		}
						
		catch (Exception e)
		{
			if (e instanceof RuntimeException) 
			{
				throw (RuntimeException)e;
			}
			else
			{
				throw new RuntimeException(e);
			}
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		if (log.isLoggable(Level.INFO)) log.info("#" + counter);
		addLog(0, null, new BigDecimal(counter), "@Updated@");
		return counter;
	}	//	update

	
	
	
	/**
	 * wv
	 * 	Update Cost Records
	 *	@param cost cost
	 * @param costmetodo 
	 * @param inventoryDoc 
	 *	@return true if updated
	 *	@throws Exception
	 */
	private boolean updatecost (MCost cost, String costmetodo) throws Exception
	{
		boolean updated = false;
		if (costmetodo.equals(TO_LastInvoicePrice))
		{MAcctSchema.get(getCtx(), p_M_CostType_ID.intValue());
			BigDecimal costs = getCosts(cost, costmetodo);
			if (costs != null && costs.signum() != 0)
			{   	
				cost.setCurrentCostPrice(costs);
				updated = true;	
			}			
		}
		
		if (updated)
			updated = cost.save();
		return updated;
	}
	//	update
	
	
	/**
	 * 	Get Costs
	 *	@param cost cost
	 *	@param to where to get costs from 
	 *	@return costs (could be 0) or null if not found
	 *	@throws Exception
	 */
	private BigDecimal getCosts (MCost cost, String to) throws Exception
	{
		BigDecimal retValue = null;
		
		//	Average Invoice
		if (to.equals(TO_AverageInvoice))
		{
			MCostElement ce = getCostElement(TO_AverageInvoice);
			if (ce == null)
				throw new AdempiereSystemError("CostElement not found: " + TO_AverageInvoice);
			MCost xCost = MCost.get(getCtx(), cost.getAD_Client_ID(), cost.getAD_Org_ID(), cost.getM_Product_ID(), cost.getM_CostType_ID(), cost.getC_AcctSchema_ID(), ce.getM_CostElement_ID(), cost.getM_AttributeSetInstance_ID(), get_TrxName());
			if (xCost != null)
				retValue = xCost.getCurrentCostPrice();
		}
		//	Average Invoice History
		else if (to.equals(TO_AverageInvoiceHistory))
		{
			MCostElement ce = getCostElement(TO_AverageInvoice);
			if (ce == null)
				throw new AdempiereSystemError("CostElement not found: " + TO_AverageInvoice);
			MCost xCost = MCost.get(getCtx(), cost.getAD_Client_ID(), cost.getAD_Org_ID(), cost.getM_Product_ID(), cost.getM_CostType_ID(), cost.getC_AcctSchema_ID(), ce.getM_CostElement_ID(), cost.getM_AttributeSetInstance_ID(), get_TrxName());
			if (xCost != null) 
				retValue = xCost.getHistoryAverage();
		}
		
		//	Average PO
		else if (to.equals(TO_AveragePO))
		{
			MCostElement ce = getCostElement(TO_AveragePO);
			if (ce == null)
				throw new AdempiereSystemError("CostElement not found: " + TO_AveragePO);
			MCost xCost = MCost.get(getCtx(), cost.getAD_Client_ID(), cost.getAD_Org_ID(), cost.getM_Product_ID(), cost.getM_CostType_ID(), cost.getC_AcctSchema_ID(), ce.getM_CostElement_ID(), cost.getM_AttributeSetInstance_ID(), get_TrxName());
			if (xCost != null)
				retValue = xCost.getCurrentCostPrice();
		}
		//	Average PO History
		else if (to.equals(TO_AveragePOHistory))
		{
			MCostElement ce = getCostElement(TO_AveragePO);
			if (ce == null)
				throw new AdempiereSystemError("CostElement not found: " + TO_AveragePO);
			MCost xCost = MCost.get(getCtx(), cost.getAD_Client_ID(), cost.getAD_Org_ID(), cost.getM_Product_ID(), cost.getM_CostType_ID(), cost.getC_AcctSchema_ID(), ce.getM_CostElement_ID(), cost.getM_AttributeSetInstance_ID(), get_TrxName());
			if (xCost != null) 
				retValue = xCost.getHistoryAverage();
		}
		
		//	FiFo
		else if (to.equals(TO_FiFo))
		{
			MCostElement ce = getCostElement(TO_FiFo);
			if (ce == null)
				throw new AdempiereSystemError("CostElement not found: " + TO_FiFo);
			MCost xCost = MCost.get(getCtx(), cost.getAD_Client_ID(), cost.getAD_Org_ID(), cost.getM_Product_ID(), cost.getM_CostType_ID(), cost.getC_AcctSchema_ID(), ce.getM_CostElement_ID(), cost.getM_AttributeSetInstance_ID(), get_TrxName());
			if (xCost != null)
				retValue = xCost.getCurrentCostPrice();
		}

		//	Future Std Costs
		else if (to.equals(TO_FutureStandardCost))
			retValue = cost.getFutureCostPrice();
		
		//	Last Inv Price
		else if (to.equals(TO_LastInvoicePrice))
		{
			MCostElement ce = getCostElement(TO_LastInvoicePrice);
			if (ce != null)
			{
				
			retValue = getPrice(cost);
			}
			if (retValue == null)
			{
				MProduct product = new MProduct(getCtx(), cost.getM_Product_ID(), get_TrxName());
				MAcctSchema as = MAcctSchema.get(getCtx(), cost.getC_AcctSchema_ID());
				retValue = MCost.getLastInvoicePrice(product, 
					cost.getM_AttributeSetInstance_ID(), cost.getAD_Org_ID(), as.getC_Currency_ID());				
			}
		}
		
		//	Last PO Price
		else if (to.equals(TO_LastPOPrice))
		{
			MCostElement ce = getCostElement(TO_LastPOPrice);
			if (ce != null)
			{
				MCost xCost = MCost.get(getCtx(), cost.getAD_Client_ID(), cost.getAD_Org_ID(), cost.getM_Product_ID(), cost.getM_CostType_ID(), cost.getC_AcctSchema_ID(), ce.getM_CostElement_ID(), cost.getM_AttributeSetInstance_ID(), get_TrxName());
				if (xCost != null)
					retValue = xCost.getCurrentCostPrice();
			}
			if (retValue == null)
			{
				MProduct product = new MProduct(getCtx(), cost.getM_Product_ID(), get_TrxName());
				MAcctSchema as = MAcctSchema.get(getCtx(), cost.getC_AcctSchema_ID());
				retValue = MCost.getLastPOPrice(product, 
					cost.getM_AttributeSetInstance_ID(), cost.getAD_Org_ID(), as.getC_Currency_ID());				
			}
		}
	
		//	FiFo
		else if (to.equals(TO_LiFo))
		{
			MCostElement ce = getCostElement(TO_LiFo);
			if (ce == null)
				throw new AdempiereSystemError("CostElement not found: " + TO_LiFo);
			MCost xCost = MCost.get(getCtx(), cost.getAD_Client_ID(), cost.getAD_Org_ID(), cost.getM_Product_ID(), cost.getM_CostType_ID(), cost.getC_AcctSchema_ID(), ce.getM_CostElement_ID(), cost.getM_AttributeSetInstance_ID(), get_TrxName());
			if (xCost != null)
				retValue = xCost.getCurrentCostPrice();
		}
		
		//	Price List
		else if (to.equals(TO_PriceListLimit))
			retValue = getPrice(cost);
		
		//	Standard Costs
		else if (to.equals(TO_StandardCost))
			retValue = cost.getCurrentCostPrice();
		
		return retValue;
	}	//	getCosts
	
	
	/**
	 * 	Get Cost Element
	 *	@param CostingMethod method
	 *	@return costing element or null
	 */
	private MCostElement getCostElement (String CostingMethod)
	{
		MCostElement ce = m_ces.get(CostingMethod);
		if (ce == null)
		{
			ce = MCostElement.getMaterialCostElement(getCtx(), CostingMethod);
			m_ces.put(CostingMethod, ce);
		}
		return ce;
	}	//	getCostElement

	/**
	 * 	Get Price from Price List
	 * 	@param cost cost record
	 *	@return price or null
	 */
	private BigDecimal getPrice (MCost cost)
	{
		BigDecimal retValue = null;
		String sql = "SELECT PriceLimit "
			+ "FROM M_ProductPrice "
			+ "WHERE M_Product_ID=? AND M_PriceList_Version_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			pstmt.setInt (1, cost.getM_Product_ID());
			pstmt.setInt (2, p_M_PriceList_Version_ID);
			rs = pstmt.executeQuery ();
			if (rs.next ())
			{
				retValue = rs.getBigDecimal(1);
			}
		}
		catch (Exception e)
		{
			log.log (Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return retValue;
	}	//	getPrice
	
}	//	CostUpdate

