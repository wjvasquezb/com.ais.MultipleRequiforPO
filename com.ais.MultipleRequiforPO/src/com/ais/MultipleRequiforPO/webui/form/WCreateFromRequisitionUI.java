package com.ais.MultipleRequiforPO.webui.form;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Vector;
import java.util.logging.Level;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.apps.form.WCreateFromWindow;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.window.FDialog;
import org.compiere.apps.IStatusBar;
import org.compiere.grid.CreateFrom;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.GridTab;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;

public class WCreateFromRequisitionUI extends CreateFrom implements EventListener<Event>
{
	
	private WCreateFromWindow window;
	
	public WCreateFromRequisitionUI(GridTab tab) 
	{
		super(tab);
		log.info(getGridTab().toString());
		
		window = new WCreateFromWindow(this, getGridTab().getWindowNo());
		
		p_WindowNo = getGridTab().getWindowNo();

		try
		{
			if (!dynInit())
				return;
			zkInit();
			setInitOK(true);
		}
		catch(Exception e)
		{
			log.log(Level.SEVERE, "", e);
			setInitOK(false);
		}
		AEnv.showWindow(window);
	}
	
	/** Window No               */
	private int p_WindowNo;

	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(getClass());
		
	protected Label Requisition_Label = new Label(Msg.translate(Env.getCtx(), "Requisition"));
	protected WEditor requisitionLookup;
	
	protected Label User_Label = new Label(Msg.translate(Env.getCtx(), "User"));
	protected WEditor userLookup;
	
	protected Label Org_Label = new Label(Msg.translate(Env.getCtx(), "Org"));
	protected WTableDirEditor orgLookup;
	
	/**
	 *  Dynamic Init
	 *  @throws Exception if Lookups cannot be initialized
	 *  @return true if initialized
	 */
	public boolean dynInit() throws Exception
	{
		log.config("");
		//Refresh button
				Button refreshButton = window.getConfirmPanel().createButton(ConfirmPanel.A_REFRESH);
				refreshButton.addEventListener(Events.ON_CLICK, this);
				window.getConfirmPanel().addButton(refreshButton);				
		if (getGridTab().getValue("C_Order_ID") == null)
		{
			FDialog.error(0, window, "SaveErrorRowNotFound");
			return false;
		}
		if (getGridTab().getValueAsBoolean("IsSOTrx"))
		{
			FDialog.error(p_WindowNo, window, "Only Purchase Order");
			return false;
		}
		
		window.setTitle("Create From Requisition");
		
		MLookup lookup = MLookupFactory.get (Env.getCtx(), p_WindowNo, 0, MColumn.getColumn_ID(MRequisition.Table_Name, MRequisition.COLUMNNAME_M_Requisition_ID), DisplayType.Search);
		requisitionLookup = new WSearchEditor ("M_Requisition_ID", false, false, true, lookup);
		
		lookup = MLookupFactory.get (Env.getCtx(), p_WindowNo, 0, MColumn.getColumn_ID(MRequisition.Table_Name, MRequisition.COLUMNNAME_AD_User_ID), DisplayType.Search);
		userLookup = new WSearchEditor ("AD_User_ID", false, false, true, lookup);
		
		lookup = MLookupFactory.get (Env.getCtx(), p_WindowNo, 0, MColumn.getColumn_ID(MRequisition.Table_Name, MRequisition.COLUMNNAME_AD_Org_ID), DisplayType.TableDir);
		orgLookup = new WTableDirEditor ("AD_Org_ID",false,false,true,lookup);
		orgLookup.setValue(getGridTab().getValue("AD_Org_ID")); // default
		orgLookup.getComponent().addEventListener(Events.ON_CHANGE, this);
		
		return true;
	}   //  dynInit
	
	protected void zkInit() throws Exception
	{
		Requisition_Label.setText(Msg.getElement(Env.getCtx(), "M_Requisition_ID"));
		User_Label.setText(Msg.getElement(Env.getCtx(), "AD_User_ID", false));
		Org_Label.setText(Msg.getElement(Env.getCtx(), "AD_Org_ID", false));
        
        
		Borderlayout parameterLayout = new Borderlayout();
		parameterLayout.setHeight("130px");
		parameterLayout.setWidth("100%");
    	Panel parameterPanel = window.getParameterPanel();
		parameterPanel.appendChild(parameterLayout);
		
		Grid parameterStdLayout = GridFactory.newGridLayout();
    	Panel parameterStdPanel = new Panel();
		parameterStdPanel.appendChild(parameterStdLayout);

		Center center = new Center();
		parameterLayout.appendChild(center);
		center.appendChild(parameterStdPanel);
		
		Columns columns = new Columns();
		parameterStdLayout.appendChild(columns);
		Column column = new Column();
		columns.appendChild(column);		
		column = new Column();
		column.setWidth("15%");
		columns.appendChild(column);
		column.setWidth("35%");
		column = new Column();
		column.setWidth("15%");
		columns.appendChild(column);
		column = new Column();
		column.setWidth("35%");
		columns.appendChild(column);
		
		
		Rows rows = (Rows) parameterStdLayout.newRows();
		Row row = rows.newRow();
		row.appendChild(Requisition_Label.rightAlign());
		row.appendChild(requisitionLookup.getComponent());
		row.appendChild(Org_Label.rightAlign());
		row.appendChild(orgLookup.getComponent());
		row = rows.newRow();
		row.appendChild(User_Label.rightAlign());
		row.appendChild(userLookup.getComponent());
		
	}

	/**
	 *  Action Listener
	 *  @param e event
	 * @throws Exception 
	 */
	public void onEvent(Event e) throws Exception
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Action=" + e.getTarget().getId());
		if(e.getTarget().equals(window.getConfirmPanel().getButton(ConfirmPanel.A_REFRESH)))
		{
			loadRequisition();
			window.tableChanged(null);
		}
	}

	/**
	 *  Load Data - Order
	 *  @param C_Order_ID Order
	 *  @param forInvoice true if for invoice vs. delivery qty
	 */
	protected void loadRequisition ()
	{
		loadTableOIS(getRequisitionData(requisitionLookup.getValue(), orgLookup.getValue(), 
				userLookup.getValue()));
	}   //  LoadOrder
	
	/**
	 *  Load Order/Invoice/Shipment data into Table
	 *  @param data data
	 */
	protected void loadTableOIS (Vector<?> data)
	{
		window.getWListbox().clear();
		
		//  Remove previous listeners
		window.getWListbox().getModel().removeTableModelListener(window);
		//  Set Model
		ListModelTable model = new ListModelTable(data);
		model.addTableModelListener(window);
		window.getWListbox().setData(model, getOISColumnNames());
		//
		
		configureMiniTable(window.getWListbox());
	}   //  loadOrder
	
	public void showWindow()
	{
		window.setVisible(true);
	}
	
	public void closeWindow()
	{
		window.dispose();
	}

	@Override
	public Object getWindow() {
		// TODO Auto-generated method stub
		return window;
	}

	@Override
	public void info(IMiniTable miniTable, IStatusBar statusBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean save(IMiniTable miniTable, String trxName) {
	//  Order
			int C_Order_ID = ((Integer)getGridTab().getValue("C_Order_ID")).intValue();
			MOrder order = new MOrder (Env.getCtx(), C_Order_ID, trxName);
			if (log.isLoggable(Level.CONFIG)) log.config(order.toString());

			//  Lines
			for (int i = 0; i < miniTable.getRowCount(); i++)
			{
				if (((Boolean)miniTable.getValueAt(i, 0)).booleanValue())
				{
					

					KeyNamePair pp = (KeyNamePair)miniTable.getValueAt(i, 2);   //  1-documentno  line id
					int M_RequisitionLine_ID = pp.getKey();
					MRequisitionLine rLine = new MRequisitionLine (Env.getCtx(), M_RequisitionLine_ID, trxName);

					//	Create new Order Line
					MOrderLine m_orderLine = new MOrderLine (order);
					m_orderLine.setDatePromised(rLine.getDateRequired());
					if (rLine.getM_Product_ID() >0)
					{
						m_orderLine.setProduct(MProduct.get(Env.getCtx(), rLine.getM_Product_ID()));
						m_orderLine.setM_AttributeSetInstance_ID(rLine.getM_AttributeSetInstance_ID());
					}
					else
					{
						m_orderLine.setC_Charge_ID(rLine.getC_Charge_ID());
						
					}
					m_orderLine.setPriceActual(rLine.getPriceActual());
					m_orderLine.setAD_Org_ID(rLine.getAD_Org_ID());
					m_orderLine.setQty(rLine.getQty());
					//	Added by Jorge Colmenarez 2020-03-23 11:40
					//	Add support for Accounting Elements
					m_orderLine.setC_Project_ID(rLine.get_ValueAsInt("C_Project_ID"));
					m_orderLine.setC_Activity_ID(rLine.get_ValueAsInt("C_Activity_ID"));
					m_orderLine.setC_Campaign_ID(rLine.get_ValueAsInt("C_Campaign_ID"));
					m_orderLine.setAD_OrgTrx_ID(rLine.get_ValueAsInt("AD_OrgTrx_ID"));
					m_orderLine.setUser1_ID(rLine.get_ValueAsInt("User1_ID"));
					m_orderLine.setUser2_ID(rLine.get_ValueAsInt("User2_ID"));
					//	End Jorge Colmenarez
					m_orderLine.setLine(rLine.getLine());
					m_orderLine.saveEx();
					
					//	Update Requisition Line
					rLine.setC_OrderLine_ID(m_orderLine.getC_OrderLine_ID());
					rLine.saveEx();
					
				}   //   if selected
			}   //  for all rows

			return true;
	}
	
	/**
	 *  Load Data - Shipment not invoiced
	 *  @param M_InOut_ID InOut
	 */
	protected Vector<Vector<Object>> getRequisitionData(Object Requisition, Object Org,  Object User)
	{
		//
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		StringBuilder sql = new StringBuilder("select r.M_Requisition_ID,r.DocumentNo,r.DateRequired,r.PriorityRule,rl.M_Product_ID,");   //1-5
				sql.append(" p.Name as ProductName,rl.Description,rl.Qty,rl.C_BPartner_ID, bp.Name as BpName, rl.M_RequisitionLine_ID, u.Name as Username, o.Name as OrgName,");  //6-13
				sql.append(" c.C_Charge_ID, c.name as ChargeName");// 14-15
				sql.append(" from M_Requisition r " );
				sql.append(" inner join M_RequisitionLine rl on (r.m_requisition_id=rl.m_requisition_id)" );
				sql.append(" inner join AD_User u on (r.AD_User_ID=u.AD_User_ID)" );
				sql.append(" inner join AD_Org o on (r.AD_Org_ID=o.AD_Org_ID)" );
				sql.append(" left outer join M_Product p on (rl.M_Product_ID=p.M_Product_ID)" );
				sql.append(" left outer join C_Charge c on (rl.C_Charge_ID=c.C_Charge_ID)" );
				sql.append(" left outer join C_BPartner bp on (rl.C_BPartner_ID=bp.C_BPartner_ID)" );
				sql.append(" where r.docstatus='CO' and rl.C_OrderLine_ID is null");
		
		if(Requisition!=null)
			sql.append(" AND rl.M_Requisition_ID=?");
		if(Org!=null)
			sql.append(" AND r.AD_Org_ID=?");
		if(User!=null)
			sql.append(" AND r.AD_User_ID=?");
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			int i=1;
			pstmt = DB.prepareStatement(sql.toString(), null);
			if(Requisition!=null)
				pstmt.setInt(i++, (Integer)Requisition);
			if(Org!=null)
				pstmt.setInt(i++, (Integer)Org);
			if(User!=null)
				pstmt.setInt(i++, (Integer)User);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				Vector<Object> line = new Vector<Object>(7);
				boolean add = line.add(false);           //  0-Selection
				line.add(rs.getString(13)); //1-OrgName
				
				KeyNamePair pp =  new KeyNamePair(rs.getInt(11), rs.getString(2).trim());
				line.add(pp);  //  2-DocumentNo Line iD
				line.add(rs.getTimestamp(3));//  3-DateRequired
				
				if(rs.getString(10)!=null)
				{
					pp = new KeyNamePair(rs.getInt(9), rs.getString(10).trim());
					line.add(pp);	//  4-BPartner
				}
				else
					line.add(null); //  4-BPartner
			
				if( rs.getString(6)!=null)
				{
				pp = new KeyNamePair(rs.getInt(5), rs.getString(6).trim());
				line.add(pp);				// 5-Product
				}
				else
				line.add(null);      // 5-Product
				
				if(rs.getString(15)!=null)
					line.add(rs.getString(15)); //6-charge
				else
					line.add(null); //6-charge
				
				line.add(rs.getBigDecimal(8));//7- Qty
		
				line.add(rs.getString(7));                           //  8-description
				line.add(rs.getString(12).trim());                     	//  9-user
				data.add(line);
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return data;
	}   //  loadShipment

	protected void configureMiniTable (IMiniTable miniTable)
	{
		miniTable.setColumnClass(0, Boolean.class, false);      //  0-Selection
		miniTable.setColumnClass(1, String.class, true);        //  1-OrgName
		miniTable.setColumnClass(2, String.class, true);        //  2-DocumentNo
		miniTable.setColumnClass(3, Timestamp.class, true);        //  3-DateRequired
		miniTable.setColumnClass(4, String.class, true);        //  4-BPartner
		miniTable.setColumnClass(5, String.class, true);        //  5-Product
		miniTable.setColumnClass(6, String.class, true);        //  6-Charge
		miniTable.setColumnClass(7, BigDecimal.class, true);        //  7-Qty
		miniTable.setColumnClass(8, String.class, true);        //  8-Description
		miniTable.setColumnClass(9, String.class, true);        //  9-User
		
		//  Table UI
		miniTable.autoSize();
	}
		
	protected Vector<String> getOISColumnNames()
	{
		//  Header Info
	    Vector<String> columnNames = new Vector<String>(7);
	    columnNames.add(Msg.getMsg(Env.getCtx(), "Select"));
	    columnNames.add(Msg.getElement(Env.getCtx(), "AD_Org_ID"));
	    columnNames.add(Msg.translate(Env.getCtx(), "Documentno"));
	    columnNames.add(Msg.translate(Env.getCtx(), "DateRequired"));
	    columnNames.add(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
	    columnNames.add(Msg.getElement(Env.getCtx(), "M_Product_ID", false));
	    columnNames.add(Msg.getElement(Env.getCtx(), "C_Charge_ID", false));
	    columnNames.add(Msg.getElement(Env.getCtx(), "Qty"));
	    columnNames.add(Msg.getElement(Env.getCtx(), "Description", false));
	    columnNames.add(Msg.getElement(Env.getCtx(), "AD_User_ID", false));
	    

	    return columnNames;
	}
	
	
}