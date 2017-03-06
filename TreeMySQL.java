/*
* The program here includes:
*   1) A class for automated access to MySQL databases (without knowing the numbers or the names of databases, tables, or fields) and also
*   2) A modified version of the Oracle TreeDemo program for modelling and visualization of the retrieved databases.
*
* Link to the original Oracle program:
* https://docs.oracle.com/javase/tutorial/uiswing/examples/components/TreeDemoProject/src/components/TreeDemo.java
*
* Quoted copyright notice of the original Oracle program:
* "Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved."
*
*
* @author Aliakbar Jafarpour
* @version 1.0
*
*
*/

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import java.sql.*;
import java.awt.*;


public class TreeMySQL extends JPanel implements TreeSelectionListener {
    private JEditorPane htmlPane;
    private JTree tree;

    //Optionally set the look and feel.
    private static boolean useSystemLookAndFeel = false;

    private MySQLClass mysql = new MySQLClass();
    

    public TreeMySQL() {
        super(new GridLayout(1,0));

        //Create the nodes.
        DefaultMutableTreeNode top =
            new DefaultMutableTreeNode("Databases");
        createNodes(top);

        //Create a tree that allows one selection at a time.
        tree = new JTree(top);
        tree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);

        //Listen for when the selection changes.
        tree.addTreeSelectionListener(this);

        //Create the scroll pane and add the tree to it. 
        JScrollPane treeView = new JScrollPane(tree);

        //Create the HTML viewing pane.
        htmlPane = new JEditorPane();
        htmlPane.setEditable(false);
        initHelp();
        JScrollPane htmlView = new JScrollPane(htmlPane);

        //Add the scroll panes to a split pane.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(treeView);
        splitPane.setBottomComponent(htmlView);

        Dimension minimumSize = new Dimension(100, 50);
        htmlView.setMinimumSize(minimumSize);
        treeView.setMinimumSize(minimumSize);
        splitPane.setDividerLocation(100); 
        splitPane.setPreferredSize(new Dimension(500, 300));

        //Add the split pane to this panel.
        add(splitPane);
    }

    /** Required by TreeSelectionListener interface. */
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        
        if (node == null) return;
        
        Object nodeInfo = node.getUserObject();
        
        if (node.isLeaf()) {
            DefaultMutableTreeNode table = (DefaultMutableTreeNode) node.getParent();
            DefaultMutableTreeNode database = (DefaultMutableTreeNode) table.getParent();
       		String column_ = mysql.getColumn(database.toString(), table.toString(), node.toString());
       		if (column_ != null) {
       			htmlPane.setText(column_);
       		}
        } else {
       		//htmlPane.setText("");
        }
    }
    private void initHelp() {
        htmlPane.setText("");
    }
    private void createNodes(DefaultMutableTreeNode top) {
        DefaultMutableTreeNode database = null;
        DefaultMutableTreeNode table = null;
        DefaultMutableTreeNode field = null;

        String[] tables;
        String[] fields;

        if (mysql == null) {
        	return;
        }
        if (mysql.databases == null) {
        	return;
        }
        if (mysql.databases.length == 0) {
        	return;
        }

        for (int dbCntr = 0; dbCntr < mysql.databases.length; dbCntr++) {
	        database = new DefaultMutableTreeNode(mysql.databases[dbCntr]);
	        top.add(database);
	        tables = mysql.getTables(mysql.databases[dbCntr]);
        	for (int tblCntr = 0; tblCntr < tables.length ; tblCntr++) {
		        table = new DefaultMutableTreeNode(tables[tblCntr]);
		        database.add(table);
		        fields = mysql.getFields(mysql.databases[dbCntr], tables[tblCntr]);
        		for (int fldCntr = 0; fldCntr < fields.length ; fldCntr++) {
		        	field = new DefaultMutableTreeNode(fields[fldCntr]);
		        	table.add(field);
        		}
        	}
        }
    }
        
    private static void createAndShowGUI() {
        if (useSystemLookAndFeel) {
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Couldn't use system look and feel.");
            }
        }

        //Create and set up the window.
        JFrame frame = new JFrame("MySQL Viewer");
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Add content to the window.
        frame.add(new TreeMySQL());

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}

class MySQLClass {
	private String table;
	private final String DB_URL_BASE = "jdbc:mysql://";
	private String DB_URL;
	private String DB_Host = "localhost";
	private String DB_Name = "pets";
	private String USER = "root";
	private String PASS = "";

	public String[] databases;
	public String[] tables;
	public String[] fields;

	public MySQLClass() {
		if (setDatabaseParams()) {
			return;
		}
		Connection conn = null;
		Statement stmt = null;
		try{
			conn = DriverManager.getConnection(DB_URL_BASE + DB_Host + "/", USER, PASS);
			if (conn == null) {
				return;
			}
			databases = getDataBases(conn);
			conn.close();

			DB_Name = databases[2];
			DB_URL = DB_URL_BASE + DB_Host + "/" + DB_Name;
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			tables = getTables(conn);
			table = tables[0];
			fields = getFields(conn, table);
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);
			
			String sql;
			while(rs.next()){
				sql = "";
				for (String f : fields) {
					sql += "" + f + ": " + rs.getString(f) + ", ";
				}
				sql = deleteLastChars(sql, 2);
			}

			rs.close();
			stmt.close();
			conn.close();
		} catch(SQLException se){
			se.printStackTrace();
		} catch(Exception e){
			e.printStackTrace();
		} finally{
			try{
			if(stmt!=null)
				stmt.close();
			}catch(SQLException se2){
			}
			try{
				if(conn!=null)
					conn.close();
			}catch(SQLException se){
				se.printStackTrace();
			}
		}
	}
	public String getColumn(String DB_Name_, String table_, String field_) {
		String column_ = "";
		try{
			DB_URL = DB_URL_BASE + DB_Host + "/" + DB_Name_;
			Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);
			while(rs.next()){
				for (String f : fields) {
					if (f.equals(field_)) {
						column_ += (deleteLastChars(rs.getString(f), 2) + "\n");
					}
				}
			}
			rs.close();
			stmt.close();
			conn.close();
		} catch(Exception e){
			e.printStackTrace();
		} finally{
		}
		return column_;
	}
	public String[] getFields(String DB_Name_, String table_) {
		String[] fields_ = new String[1];
		try{
			DB_URL = DB_URL_BASE + DB_Host + "/" + DB_Name_;
			Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
			fields_ = getFields(conn, table_);
			conn.close();
		} catch(Exception e){
			e.printStackTrace();
		} finally{
		}
		return fields_;
	}
	public String[] getTables(String DB_Name_) {
		String[] tables = new String[0];
		try{
			DB_URL = DB_URL_BASE + DB_Host + "/" + DB_Name_;
			Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
			tables = getTables(conn);
			conn.close();
		} catch(SQLException se){
			se.printStackTrace();
		} catch(Exception e){
			e.printStackTrace();
		} finally{
		}
		return tables;
	}

	
	private boolean setDatabaseParams() {
		boolean flag = false;
		
		JTextField hostField = new JTextField(DB_Host);
		JTextField dbField = new JTextField(DB_Name);
		JTextField userField = new JTextField(USER);
		JTextField passField = new JTextField(PASS);
		
		JPanel myPanel = new JPanel();
		myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));

		myPanel.add(new JLabel("Name of Host"));
		myPanel.add(hostField);
		myPanel.add(Box.createVerticalStrut(10));
		
		myPanel.add(new JLabel("Name of Database"));
		myPanel.add(dbField);
		myPanel.add(Box.createVerticalStrut(10));
		
		myPanel.add(new JLabel("Username"));
		myPanel.add(userField);
		myPanel.add(Box.createVerticalStrut(10));
		
		myPanel.add(new JLabel("Password"));
		myPanel.add(passField);
		myPanel.add(Box.createVerticalStrut(10));
		
		int result = JOptionPane.showConfirmDialog(null, myPanel, "Database parameters", JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {
			DB_Host = hostField.getText();
			DB_Name = dbField.getText();
			USER = userField.getText();
			PASS = passField.getText();
		} else {
			flag = true;
		}
		return flag;
	}
	private String[] getDataBases(Connection conn) {
		String[] databases = new String[0];
		try{
			ResultSet rs__ = conn.getMetaData().getCatalogs();
			java.util.ArrayList<String> dbNames = new java.util.ArrayList<String>();
			while (rs__.next()) {
			  dbNames.add(rs__.getString("TABLE_CAT"));
			}
			rs__.close();
			int nElements = dbNames.size();
			databases = new String[nElements];
			for (int cntr = 0; cntr < nElements; cntr++) {
				databases[cntr] = dbNames.get(cntr);
			}
		} catch(SQLException se){
			se.printStackTrace();
		} catch(Exception e){
			e.printStackTrace();
		} finally{
		}
		return databases;
	}
	private String[] getTables(Connection conn) {
		String[] tables = new String[0];
		try{
			DatabaseMetaData md = conn.getMetaData();
			ResultSet rs_ = md.getTables(null, null, "%", null);
			java.util.ArrayList<String> tableNames = new java.util.ArrayList<String>();
			while (rs_.next()) {
			  tableNames.add(rs_.getString(3));
			}			
			rs_.close();

			int nElements = tableNames.size();
			tables = new String[nElements];
			for (int cntr = 0; cntr < nElements; cntr++) {
				tables[cntr] = tableNames.get(cntr);
			}
		} catch(SQLException se){
			se.printStackTrace();
		} catch(Exception e){
			e.printStackTrace();
		} finally{
		}
		return tables;
	}
	private String[] getFields(Connection conn, String table_) {
		String[] fields_ = new String[1];
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM " + table_);
			fields_ = getFields(rs.getMetaData(), table_);
			rs.close();
			stmt.close();
		} catch(Exception e){
			e.printStackTrace();
		} finally{
		}
		return fields_;
	}
	private String[] getFields(ResultSetMetaData rsmd, String table_) {
		String[] fields_ = new String[1];
		try{
			int n = rsmd.getColumnCount();
			fields_ = new String[n];
			for (int cntr = 0; cntr < n; cntr++) {
				fields_[cntr] = rsmd.getColumnName(cntr + 1);
			}
		} catch(Exception e){
			e.printStackTrace();
		} finally{
		}
		return fields_;
	}
	private String formQuery() {
		String sql = "SELECT ";
		for (String f : fields) {
			sql += f + ", ";
		}
		sql = deleteLastChars(sql, 2);
		sql += " FROM " + table;
		return sql;
	}
	private String deleteLastChars(String s, int n) {
		String sNew = "";
		int nS = s.length();
		if (nS > n) {
			sNew = s.substring(0, nS - n);
		}
		return sNew;
	}
	private String x2s(int x) {
		return String.valueOf(x);
	}
}
