/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ContactTransmutGUIMain.java
 *
 * Created on 30.4.2011, 13:48:29
 */

package gui;

import com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel;
import contacttransmut.InputFilter;
import contacttransmut.InternalDoc2CompiledDoc;
import contacttransmut.InternalDocColumnSchema;
import contacttransmut.InternalDocColumnSchemaImpl;
import contacttransmut.InternalDocCompiler;
import contacttransmut.ODSInput;
import contacttransmut.OutputFilter;
import contacttransmut.ReadCSV;
import contacttransmut.ReadCompiledDoc;
import contacttransmut.ReadVCF;
import contacttransmut.VCFTypesEnum;
import contacttransmut.WriteCSV;
import contacttransmut.WriteVCF;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.swing.SwingWorker;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.TableColumn;
import org.w3c.dom.Element;

/**
 *
 * @author Martin
 */
public class ContactTransmutGUIMain extends javax.swing.JFrame {

            
    private File inputFile;
    private Document internalDoc = null;
    private Document compiledDoc = null;
    private InternalDocColumnSchema columnSchema = null;
    private ContactsListTableModel tableModel = new ContactsListTableModel();
    //array of combo boxes in main window
    private ArrayList<javax.swing.JComboBox> comboBoxes = new ArrayList<JComboBox>();
    //array of text fields in ADD TO COLUMN window
    private List<JTextField> columnsToAdd = new LinkedList<JTextField>();
    //array of combo boxes in SPLIT INTO window
    private ArrayList<javax.swing.JComboBox> splitIntoColumnsTypes= new ArrayList<JComboBox>();

    private ComboBoxesManager comboMgr;
    private ColumnSchemaManager columnSchMgr;

    //swing worker for compiling
    private RefreshSwingWorker refreshSwingWorker;
    //swing worker for updatin statusbar -> runs simultaneously with refreshSwingWorker
    private UpdateStatusbarSwingWorker updateStatusbarSwingWorker;
    private InternalDoc2CompiledDoc compiler;

    //to disable updating of CS aso. -> for example when just creating combo boxes
    boolean updateEnabled = true;

    //to differ refresh and save calling
    boolean saving = false;

    //if there has been no change before saving, there is no need to compile again
    boolean compiled = false;

    //callable with right-click button
    JPopupMenu jPopupMenu;

    //screen width and height
    int maxHeight;
    int maxWidth;

    /** Creates new form ContactTransmutGUIMain */
    public ContactTransmutGUIMain() {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(ContactTransmutGUIMain.class.getName()).log(Level.SEVERE, null, ex);
        }

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        maxHeight = dim.height;
        maxWidth = dim.width;

        initComponents();


        jMainWindowFrame2.pack();
        jMainWindowFrame2.setLocation((maxWidth-jMainWindowFrame2.getWidth())/2, (maxHeight-jMainWindowFrame2.getHeight())/2);

        pack();
        setLocation((maxWidth-getWidth())/2, (maxHeight-getHeight())/2);

        jPopupMenu = new JPopupMenu();
        JMenuItem menuItemPaste = new JMenuItem("Paste");
        menuItemPaste.setEnabled(false);
        menuItemPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compiled = false;
                tableModel.pasteValues();
            }
        });
        JMenuItem menuItemCopy = new JMenuItem("Copy");
        menuItemCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ((JMenuItem)jPopupMenu.getSubElements()[1]).setEnabled(true);
                tableModel.copyValues();
            }
        });
        jPopupMenu.add(menuItemCopy);
        jPopupMenu.add(menuItemPaste);
        JMenuItem menuItemDelete = new JMenuItem("Delete");
        menuItemDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compiled = false;
                tableModel.deleteValues();
            }
        });
        jPopupMenu.add(menuItemDelete);


        jContactsListTable.setModel(tableModel);
        jContactsListTable.setAutoscrolls(true);
        jMainWindowPanel.setAutoscrolls(true);
        jRefreshProgressBar2.setVisible(false);
        jMainWindowStopButton2.setVisible(false);
        jMainWindowShowCompiledDocButton.setEnabled(false);
        
        JScrollBar sb1 = jContactsListScrollPane.getHorizontalScrollBar();
        JScrollBar sb2 = jComboBoxesScrollPane.getHorizontalScrollBar();
        sb1.setModel(sb2.getModel());

        jContactsListTable.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode()==KeyEvent.VK_C){
                    tableModel.copyValues();
                    jPopupMenu.getComponent(1).setEnabled(true);
                } else if (e.isControlDown() && e.getKeyCode()==KeyEvent.VK_V) {
                    compiled = false;
                    tableModel.pasteValues();
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    compiled = false;
                    tableModel.deleteValues();
                } else if (e.getKeyCode() == KeyEvent.VK_X) {
                    compiled = false;
                    tableModel.copyValues();
                    tableModel.deleteValues();
                }
            }
        });

        jContactsListTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3){
                    jPopupMenu.show(jContactsListTable, e.getX(), e.getY());
                }
                super.mouseClicked(e);
            }
        });

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooser1 = new javax.swing.JFileChooser();
        jOriginalFileTextFrame = new javax.swing.JFrame();
        jOriginalFileScrollPane = new javax.swing.JScrollPane();
        jOriginalFileTextArea = new javax.swing.JTextArea();
        jInternalDocTextFrame = new javax.swing.JFrame();
        jInternalDocScrollPane = new javax.swing.JScrollPane();
        jInternalDocTextArea = new javax.swing.JTextArea();
        jColumnSchemaTextFrame = new javax.swing.JFrame();
        jColumnSchemaScrollPane = new javax.swing.JScrollPane();
        jColumnSchemaTextArea = new javax.swing.JTextArea();
        jMainWindowFrame2 = new javax.swing.JFrame();
        jMainWindowPanel = new javax.swing.JPanel();
        jMainWindowBackButton = new javax.swing.JButton();
        jMainWindowCancelButton = new javax.swing.JButton();
        jMainWindowNextButton = new javax.swing.JButton();
        jMainWindowShowCompiledDocButton = new javax.swing.JButton();
        jMainWindowShowColumnSchemaButton = new javax.swing.JButton();
        jMainWindowShowInternalDocButton = new javax.swing.JButton();
        jLayeredPane2 = new javax.swing.JLayeredPane();
        jRefreshProgressBar2 = new javax.swing.JProgressBar();
        jMainWindowRefreshButton2 = new javax.swing.JButton();
        jMainWindowStopButton2 = new javax.swing.JButton();
        jMainWindowAddColumnButton2 = new javax.swing.JButton();
        jContactsListScrollPane = new javax.swing.JScrollPane();
        jContactsListTable = new javax.swing.JTable();
        jComboBoxesScrollPane = new javax.swing.JScrollPane();
        jComboBoxesToolBar = new javax.swing.JToolBar();
        jAddToFrame = new javax.swing.JFrame();
        jAddToOkButton = new javax.swing.JButton();
        jAddToCancelButton = new javax.swing.JButton();
        jAddToScrollPane = new javax.swing.JScrollPane();
        jAddToToolBar = new javax.swing.JToolBar();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jAddToBaseColumnTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jColumnToAddLabel = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jAddToDelimeterTextField = new javax.swing.JTextField();
        jAddToSubmitButton = new javax.swing.JButton();
        jAddToPlusButton = new javax.swing.JButton();
        jSplitIntoFrame = new javax.swing.JFrame();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jColumnToSplitLabel = new javax.swing.JLabel();
        jSplitIntoOkButton = new javax.swing.JButton();
        jSplitIntoCancelButton = new javax.swing.JButton();
        jSplitIntoNumberOfColumnsTextField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jSplitIntoContactsSettingsButton = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jSplitIntoDelimeterTextField = new javax.swing.JTextField();
        jSplitIntoContactsCheckBox = new javax.swing.JCheckBox();
        jSplitIntoNumberOfColumnsSettingsButton = new javax.swing.JButton();
        jSplitIntoContactsFrame = new javax.swing.JFrame();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jColumnToSplitLabel1 = new javax.swing.JLabel();
        jSplitIntoContactsOkButton = new javax.swing.JButton();
        jSplitIntoContactsCancelButton = new javax.swing.JButton();
        jSplitIntoContactsNumberOfColumnsTextField = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jSplitIntoContactsSwapsSettingsButton = new javax.swing.JButton();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jSplitIntoContactsDelimeterOfContactsTextField = new javax.swing.JTextField();
        jSplitIntoContactsDelimeterOfColumnsTextField = new javax.swing.JTextField();
        jSplitIntoContactsSwapsCheckBox = new javax.swing.JCheckBox();
        jLabel16 = new javax.swing.JLabel();
        jSplitIntoContactsAreEmployeesCheckBox = new javax.swing.JCheckBox();
        jSplitIntoContactsColumnTypesSettingsButton = new javax.swing.JButton();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jSplitIntoContactsOrigIntoContactsCheckBox = new javax.swing.JCheckBox();
        jSplitIntoContactsOrigIntoSourceCheckBox = new javax.swing.JCheckBox();
        jSplitIntoTypeSettingsFrame = new javax.swing.JFrame();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jColumnToSplitLabel2 = new javax.swing.JLabel();
        jSplitIntoTypeSettingsOkButton = new javax.swing.JButton();
        jSplitIntoTypeSettingsCancelButton = new javax.swing.JButton();
        jLabel23 = new javax.swing.JLabel();
        jSplitIntoTypeSettingsNumberOfColumnsLabel = new javax.swing.JLabel();
        jSplitIntoTypeSettingsScrollPane = new javax.swing.JScrollPane();
        jSplitIntoTypeSettingsToolBar = new javax.swing.JToolBar();
        jCompiledDocTextFrame = new javax.swing.JFrame();
        jCompiledDocScrollPane = new javax.swing.JScrollPane();
        jCompiledDocTextArea = new javax.swing.JTextArea();
        jFileChooserFrame = new javax.swing.JFrame();
        jFileChooserLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jMainLabel1 = new javax.swing.JLabel();
        jBrowseButton1 = new javax.swing.JButton();
        jInputFileTextField1 = new javax.swing.JTextField();
        jSelectInputLabel1 = new javax.swing.JLabel();
        jNextButton1 = new javax.swing.JButton();
        jCancelButton1 = new javax.swing.JButton();
        jBackButton1 = new javax.swing.JButton();
        jEncodingComboBox1 = new javax.swing.JComboBox();
        jEncodingLabel1 = new javax.swing.JLabel();

        jOriginalFileTextArea.setColumns(20);
        jOriginalFileTextArea.setFont(new java.awt.Font("Arial", 0, 12));
        jOriginalFileTextArea.setRows(5);
        jOriginalFileScrollPane.setViewportView(jOriginalFileTextArea);

        javax.swing.GroupLayout jOriginalFileTextFrameLayout = new javax.swing.GroupLayout(jOriginalFileTextFrame.getContentPane());
        jOriginalFileTextFrame.getContentPane().setLayout(jOriginalFileTextFrameLayout);
        jOriginalFileTextFrameLayout.setHorizontalGroup(
            jOriginalFileTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 680, Short.MAX_VALUE)
            .addGroup(jOriginalFileTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jOriginalFileTextFrameLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(jOriginalFileScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 668, Short.MAX_VALUE)
                    .addGap(0, 0, 0)))
        );
        jOriginalFileTextFrameLayout.setVerticalGroup(
            jOriginalFileTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 360, Short.MAX_VALUE)
            .addGroup(jOriginalFileTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jOriginalFileTextFrameLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(jOriginalFileScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
                    .addGap(0, 0, 0)))
        );

        jInternalDocTextFrame.setTitle("InternalDoc");

        jInternalDocTextArea.setColumns(20);
        jInternalDocTextArea.setEditable(false);
        jInternalDocTextArea.setFont(new java.awt.Font("Arial", 0, 12));
        jInternalDocTextArea.setRows(5);
        jInternalDocScrollPane.setViewportView(jInternalDocTextArea);

        javax.swing.GroupLayout jInternalDocTextFrameLayout = new javax.swing.GroupLayout(jInternalDocTextFrame.getContentPane());
        jInternalDocTextFrame.getContentPane().setLayout(jInternalDocTextFrameLayout);
        jInternalDocTextFrameLayout.setHorizontalGroup(
            jInternalDocTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 680, Short.MAX_VALUE)
            .addGroup(jInternalDocTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jInternalDocTextFrameLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(jInternalDocScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 668, Short.MAX_VALUE)
                    .addGap(0, 0, 0)))
        );
        jInternalDocTextFrameLayout.setVerticalGroup(
            jInternalDocTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 360, Short.MAX_VALUE)
            .addGroup(jInternalDocTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jInternalDocTextFrameLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(jInternalDocScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
                    .addGap(0, 0, 0)))
        );

        jColumnSchemaTextArea.setColumns(20);
        jColumnSchemaTextArea.setEditable(false);
        jColumnSchemaTextArea.setFont(new java.awt.Font("Arial", 0, 12));
        jColumnSchemaTextArea.setRows(5);
        jColumnSchemaScrollPane.setViewportView(jColumnSchemaTextArea);

        javax.swing.GroupLayout jColumnSchemaTextFrameLayout = new javax.swing.GroupLayout(jColumnSchemaTextFrame.getContentPane());
        jColumnSchemaTextFrame.getContentPane().setLayout(jColumnSchemaTextFrameLayout);
        jColumnSchemaTextFrameLayout.setHorizontalGroup(
            jColumnSchemaTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 680, Short.MAX_VALUE)
            .addGroup(jColumnSchemaTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jColumnSchemaTextFrameLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(jColumnSchemaScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 668, Short.MAX_VALUE)
                    .addGap(0, 0, 0)))
        );
        jColumnSchemaTextFrameLayout.setVerticalGroup(
            jColumnSchemaTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 360, Short.MAX_VALUE)
            .addGroup(jColumnSchemaTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jColumnSchemaTextFrameLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(jColumnSchemaScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
                    .addGap(0, 0, 0)))
        );

        jMainWindowFrame2.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        jMainWindowFrame2.setTitle("Contact Transmutator 1.0");
        jMainWindowFrame2.setMinimumSize(new java.awt.Dimension(100, 100));

        jMainWindowBackButton.setText("< Back");
        jMainWindowBackButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jMainWindowBackButtonMouseReleased(evt);
            }
        });

        jMainWindowCancelButton.setText("Cancel");
        jMainWindowCancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jMainWindowCancelButtonMouseReleased(evt);
            }
        });

        jMainWindowNextButton.setText("Next >");
        jMainWindowNextButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jMainWindowNextButtonMouseReleased(evt);
            }
        });

        jMainWindowShowCompiledDocButton.setText("Compiled Doc");
        jMainWindowShowCompiledDocButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jMainWindowShowCompiledDocButtonMouseReleased(evt);
            }
        });

        jMainWindowShowColumnSchemaButton.setText("Column Schema");
        jMainWindowShowColumnSchemaButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jMainWindowShowColumnSchemaButtonMouseReleased(evt);
            }
        });

        jMainWindowShowInternalDocButton.setText("Internal Doc");
        jMainWindowShowInternalDocButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jMainWindowShowInternalDocButtonMouseReleased(evt);
            }
        });

        jRefreshProgressBar2.setMaximum(100000);
        jRefreshProgressBar2.setToolTipText("");
        jRefreshProgressBar2.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        jRefreshProgressBar2.setStringPainted(true);
        jRefreshProgressBar2.setBounds(0, 10, 350, 35);
        jLayeredPane2.add(jRefreshProgressBar2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jMainWindowRefreshButton2.setText("REFRESH TABLE (apply changes)");
        jMainWindowRefreshButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jMainWindowRefreshButton2MouseReleased(evt);
            }
        });
        jMainWindowRefreshButton2.setBounds(0, 10, 240, 35);
        jLayeredPane2.add(jMainWindowRefreshButton2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jMainWindowStopButton2.setText("STOP");
        jMainWindowStopButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jMainWindowStopButton2MouseReleased(evt);
            }
        });
        jMainWindowStopButton2.setBounds(360, 10, 70, 35);
        jLayeredPane2.add(jMainWindowStopButton2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jMainWindowAddColumnButton2.setText("Add column");
        jMainWindowAddColumnButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jMainWindowAddColumnButton2MouseReleased(evt);
            }
        });

        javax.swing.GroupLayout jMainWindowPanelLayout = new javax.swing.GroupLayout(jMainWindowPanel);
        jMainWindowPanel.setLayout(jMainWindowPanelLayout);
        jMainWindowPanelLayout.setHorizontalGroup(
            jMainWindowPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jMainWindowPanelLayout.createSequentialGroup()
                .addComponent(jLayeredPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE)
                .addGap(3, 3, 3)
                .addComponent(jMainWindowShowColumnSchemaButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jMainWindowShowInternalDocButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jMainWindowShowCompiledDocButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jMainWindowAddColumnButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jMainWindowBackButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jMainWindowNextButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jMainWindowCancelButton)
                .addContainerGap())
        );
        jMainWindowPanelLayout.setVerticalGroup(
            jMainWindowPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jMainWindowPanelLayout.createSequentialGroup()
                .addGroup(jMainWindowPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLayeredPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 46, Short.MAX_VALUE)
                    .addGroup(jMainWindowPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jMainWindowPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false)
                            .addComponent(jMainWindowBackButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jMainWindowAddColumnButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jMainWindowShowCompiledDocButton, javax.swing.GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE)
                            .addComponent(jMainWindowShowInternalDocButton, javax.swing.GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE)
                            .addComponent(jMainWindowShowColumnSchemaButton, javax.swing.GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE)
                            .addComponent(jMainWindowNextButton, javax.swing.GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE)
                            .addComponent(jMainWindowCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );

        jContactsListScrollPane.setMinimumSize(new java.awt.Dimension(1, 1));

        jContactsListTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jContactsListTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jContactsListTable.setCellSelectionEnabled(true);
        jContactsListTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jContactsListTable.getTableHeader().setResizingAllowed(false);
        jContactsListTable.getTableHeader().setReorderingAllowed(false);
        jContactsListScrollPane.setViewportView(jContactsListTable);

        jComboBoxesScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        jComboBoxesToolBar.setFloatable(false);
        jComboBoxesToolBar.setRollover(true);
        jComboBoxesScrollPane.setViewportView(jComboBoxesToolBar);

        javax.swing.GroupLayout jMainWindowFrame2Layout = new javax.swing.GroupLayout(jMainWindowFrame2.getContentPane());
        jMainWindowFrame2.getContentPane().setLayout(jMainWindowFrame2Layout);
        jMainWindowFrame2Layout.setHorizontalGroup(
            jMainWindowFrame2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jMainWindowFrame2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jMainWindowFrame2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jMainWindowPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jMainWindowFrame2Layout.createSequentialGroup()
                        .addGroup(jMainWindowFrame2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jContactsListScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1060, Short.MAX_VALUE)
                            .addComponent(jComboBoxesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1060, Short.MAX_VALUE))
                        .addContainerGap())))
        );
        jMainWindowFrame2Layout.setVerticalGroup(
            jMainWindowFrame2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jMainWindowFrame2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jComboBoxesScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jContactsListScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 534, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jMainWindowPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jMainWindowFrame2.getAccessibleContext().setAccessibleName("Contact Transmutator");

        jAddToFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        jAddToFrame.setTitle("Add to..."); // NOI18N
        jAddToFrame.setResizable(false);

        jAddToOkButton.setText("OK");
        jAddToOkButton.setEnabled(false);
        jAddToOkButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jAddToOkButtonMouseReleased(evt);
            }
        });

        jAddToCancelButton.setText("Cancel");
        jAddToCancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jAddToCancelButtonMouseReleased(evt);
            }
        });

        jAddToToolBar.setFloatable(false);
        jAddToToolBar.setRollover(true);
        jAddToScrollPane.setViewportView(jAddToToolBar);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 18));
        jLabel1.setText("\"ADD TO COLUMN\" MENU:");

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel2.setText("Base column number:");

        jAddToBaseColumnTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jAddToBaseColumnTextFieldKeyTyped(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel3.setText("Column to add:");

        jColumnToAddLabel.setFont(new java.awt.Font("Tahoma", 1, 14));
        jColumnToAddLabel.setText("0");

        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel4.setText("Delimeter:");

        jAddToDelimeterTextField.setText(",");

        jAddToSubmitButton.setText("Submit");
        jAddToSubmitButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jAddToSubmitButtonMouseReleased(evt);
            }
        });

        jAddToPlusButton.setText("+");
        jAddToPlusButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jAddToPlusButtonMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout jAddToFrameLayout = new javax.swing.GroupLayout(jAddToFrame.getContentPane());
        jAddToFrame.getContentPane().setLayout(jAddToFrameLayout);
        jAddToFrameLayout.setHorizontalGroup(
            jAddToFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jAddToFrameLayout.createSequentialGroup()
                .addGroup(jAddToFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jAddToFrameLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 247, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jAddToFrameLayout.createSequentialGroup()
                        .addGap(81, 81, 81)
                        .addGroup(jAddToFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addGroup(jAddToFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(jAddToFrameLayout.createSequentialGroup()
                                    .addComponent(jLabel3)
                                    .addGap(52, 52, 52))
                                .addGroup(jAddToFrameLayout.createSequentialGroup()
                                    .addComponent(jLabel2)
                                    .addGap(18, 18, 18))))
                        .addGap(18, 18, 18)
                        .addGroup(jAddToFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jAddToFrameLayout.createSequentialGroup()
                                .addComponent(jAddToBaseColumnTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jAddToSubmitButton))
                            .addComponent(jColumnToAddLabel)
                            .addComponent(jAddToDelimeterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jAddToFrameLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jAddToScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 542, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addComponent(jAddToPlusButton)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jAddToFrameLayout.createSequentialGroup()
                .addContainerGap(446, Short.MAX_VALUE)
                .addComponent(jAddToOkButton, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jAddToCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jAddToFrameLayout.setVerticalGroup(
            jAddToFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jAddToFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13)
                .addGroup(jAddToFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jColumnToAddLabel))
                .addGap(18, 18, 18)
                .addGroup(jAddToFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jAddToDelimeterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addGap(18, 18, 18)
                .addGroup(jAddToFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jAddToBaseColumnTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(jAddToSubmitButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jAddToFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jAddToPlusButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jAddToScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 39, Short.MAX_VALUE)
                .addGroup(jAddToFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jAddToCancelButton)
                    .addComponent(jAddToOkButton))
                .addContainerGap())
        );

        jSplitIntoFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        jSplitIntoFrame.setTitle("Split into...");
        jSplitIntoFrame.setResizable(false);

        jLabel5.setFont(new java.awt.Font("Tahoma", 1, 18));
        jLabel5.setText("\"SPLIT INTO\" MULTIPLE COLUMNS MENU:");

        jLabel6.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel6.setText("Column to split:");

        jColumnToSplitLabel.setFont(new java.awt.Font("Tahoma", 1, 14));
        jColumnToSplitLabel.setText("0");

        jSplitIntoOkButton.setText("OK");
        jSplitIntoOkButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jSplitIntoOkButtonMouseReleased(evt);
            }
        });

        jSplitIntoCancelButton.setText("Cancel");
        jSplitIntoCancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jSplitIntoCancelButtonMouseReleased(evt);
            }
        });

        jSplitIntoNumberOfColumnsTextField.setText("0");

        jLabel7.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel7.setText("Number of columns:");

        jLabel8.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel8.setText("Into separate contacts:");

        jSplitIntoContactsSettingsButton.setText("Settings");
        jSplitIntoContactsSettingsButton.setEnabled(false);
        jSplitIntoContactsSettingsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jSplitIntoContactsSettingsButtonMouseReleased(evt);
            }
        });

        jLabel9.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel9.setText("Delimeter:");

        jSplitIntoDelimeterTextField.setText(",");

        jSplitIntoContactsCheckBox.setEnabled(false);
        jSplitIntoContactsCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jSplitIntoContactsCheckBoxMouseClicked(evt);
            }
        });

        jSplitIntoNumberOfColumnsSettingsButton.setText("Settings");
        jSplitIntoNumberOfColumnsSettingsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jSplitIntoNumberOfColumnsSettingsButtonMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout jSplitIntoFrameLayout = new javax.swing.GroupLayout(jSplitIntoFrame.getContentPane());
        jSplitIntoFrame.getContentPane().setLayout(jSplitIntoFrameLayout);
        jSplitIntoFrameLayout.setHorizontalGroup(
            jSplitIntoFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jSplitIntoFrameLayout.createSequentialGroup()
                .addGroup(jSplitIntoFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jSplitIntoFrameLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 395, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jSplitIntoFrameLayout.createSequentialGroup()
                        .addGap(39, 39, 39)
                        .addGroup(jSplitIntoFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jSplitIntoFrameLayout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addGap(59, 59, 59)
                                .addComponent(jColumnToSplitLabel))
                            .addGroup(jSplitIntoFrameLayout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addGap(18, 18, 18)
                                .addComponent(jSplitIntoContactsCheckBox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSplitIntoContactsSettingsButton))
                            .addGroup(jSplitIntoFrameLayout.createSequentialGroup()
                                .addGroup(jSplitIntoFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel7)
                                    .addComponent(jLabel9))
                                .addGap(34, 34, 34)
                                .addGroup(jSplitIntoFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jSplitIntoDelimeterTextField)
                                    .addComponent(jSplitIntoNumberOfColumnsTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 67, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jSplitIntoNumberOfColumnsSettingsButton)))))
                .addContainerGap(21, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jSplitIntoFrameLayout.createSequentialGroup()
                .addContainerGap(275, Short.MAX_VALUE)
                .addComponent(jSplitIntoOkButton, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitIntoCancelButton)
                .addContainerGap())
        );
        jSplitIntoFrameLayout.setVerticalGroup(
            jSplitIntoFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jSplitIntoFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jSplitIntoFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jColumnToSplitLabel))
                .addGap(24, 24, 24)
                .addGroup(jSplitIntoFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addGroup(jSplitIntoFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jSplitIntoContactsSettingsButton)
                        .addComponent(jSplitIntoContactsCheckBox)))
                .addGap(24, 24, 24)
                .addGroup(jSplitIntoFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jSplitIntoNumberOfColumnsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSplitIntoNumberOfColumnsSettingsButton))
                .addGap(23, 23, 23)
                .addGroup(jSplitIntoFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jSplitIntoDelimeterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jSplitIntoFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSplitIntoCancelButton)
                    .addComponent(jSplitIntoOkButton))
                .addContainerGap())
        );

        jSplitIntoContactsFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        jSplitIntoContactsFrame.setTitle("Split into contacts");
        jSplitIntoContactsFrame.setResizable(false);

        jLabel10.setFont(new java.awt.Font("Tahoma", 1, 18));
        jLabel10.setText("SPLIT INTO CONTACTS MENU:");

        jLabel11.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel11.setText("Column to split:");

        jColumnToSplitLabel1.setFont(new java.awt.Font("Tahoma", 1, 14));
        jColumnToSplitLabel1.setText("0");

        jSplitIntoContactsOkButton.setText("OK");
        jSplitIntoContactsOkButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jSplitIntoContactsOkButtonMouseReleased(evt);
            }
        });

        jSplitIntoContactsCancelButton.setText("Cancel");
        jSplitIntoContactsCancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jSplitIntoContactsCancelButtonMouseReleased(evt);
            }
        });

        jSplitIntoContactsNumberOfColumnsTextField.setText("0");

        jLabel12.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel12.setText("Number of columns in each contact:");

        jLabel13.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel13.setText("Detect swaps");
        jLabel13.setEnabled(false);

        jSplitIntoContactsSwapsSettingsButton.setText("Settings");
        jSplitIntoContactsSwapsSettingsButton.setEnabled(false);

        jLabel14.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel14.setText("Delimeter of contacts:");

        jLabel15.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel15.setText("Delimeter of columns:");

        jSplitIntoContactsDelimeterOfContactsTextField.setText(",");

        jSplitIntoContactsDelimeterOfColumnsTextField.setText(",");

        jSplitIntoContactsSwapsCheckBox.setEnabled(false);

        jLabel16.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel16.setText("Contacts are employees:");

        jSplitIntoContactsColumnTypesSettingsButton.setText("Settings");
        jSplitIntoContactsColumnTypesSettingsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jSplitIntoContactsColumnTypesSettingsButtonMouseReleased(evt);
            }
        });

        jLabel18.setText("(company/displayname of original contact into Company field of new contact)");

        jLabel19.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel19.setText("Original content into source as note:");

        jLabel20.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel20.setText("Original content into contacts as note:");

        javax.swing.GroupLayout jSplitIntoContactsFrameLayout = new javax.swing.GroupLayout(jSplitIntoContactsFrame.getContentPane());
        jSplitIntoContactsFrame.getContentPane().setLayout(jSplitIntoContactsFrameLayout);
        jSplitIntoContactsFrameLayout.setHorizontalGroup(
            jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jSplitIntoContactsFrameLayout.createSequentialGroup()
                .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jSplitIntoContactsFrameLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 395, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jSplitIntoContactsFrameLayout.createSequentialGroup()
                        .addGap(39, 39, 39)
                        .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jSplitIntoContactsFrameLayout.createSequentialGroup()
                                .addComponent(jLabel11)
                                .addGap(59, 59, 59)
                                .addComponent(jColumnToSplitLabel1))
                            .addGroup(jSplitIntoContactsFrameLayout.createSequentialGroup()
                                .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel14)
                                    .addComponent(jLabel15)
                                    .addComponent(jLabel16)
                                    .addComponent(jLabel13)
                                    .addComponent(jLabel20)
                                    .addComponent(jLabel19)
                                    .addComponent(jLabel12))
                                .addGap(34, 34, 34)
                                .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jSplitIntoContactsOrigIntoContactsCheckBox)
                                    .addComponent(jSplitIntoContactsOrigIntoSourceCheckBox)
                                    .addGroup(jSplitIntoContactsFrameLayout.createSequentialGroup()
                                        .addComponent(jSplitIntoContactsNumberOfColumnsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(14, 14, 14)
                                        .addComponent(jSplitIntoContactsColumnTypesSettingsButton))
                                    .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(jSplitIntoContactsDelimeterOfContactsTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jSplitIntoContactsFrameLayout.createSequentialGroup()
                                                .addComponent(jSplitIntoContactsAreEmployeesCheckBox)
                                                .addGap(18, 18, 18)
                                                .addComponent(jLabel18))
                                            .addComponent(jSplitIntoContactsDelimeterOfColumnsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGroup(jSplitIntoContactsFrameLayout.createSequentialGroup()
                                                .addComponent(jSplitIntoContactsSwapsCheckBox)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jSplitIntoContactsSwapsSettingsButton)))))))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jSplitIntoContactsFrameLayout.createSequentialGroup()
                .addContainerGap(553, Short.MAX_VALUE)
                .addComponent(jSplitIntoContactsOkButton, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitIntoContactsCancelButton)
                .addContainerGap())
        );
        jSplitIntoContactsFrameLayout.setVerticalGroup(
            jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jSplitIntoContactsFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(jColumnToSplitLabel1))
                .addGap(18, 18, 18)
                .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSplitIntoContactsNumberOfColumnsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSplitIntoContactsColumnTypesSettingsButton)
                    .addComponent(jLabel12))
                .addGap(23, 23, 23)
                .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSplitIntoContactsDelimeterOfContactsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14))
                .addGap(18, 18, 18)
                .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSplitIntoContactsDelimeterOfColumnsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15))
                .addGap(18, 18, 18)
                .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel16)
                    .addGroup(jSplitIntoContactsFrameLayout.createSequentialGroup()
                        .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSplitIntoContactsAreEmployeesCheckBox)
                            .addComponent(jLabel18))
                        .addGap(18, 18, 18)
                        .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSplitIntoContactsSwapsSettingsButton)
                            .addGroup(jSplitIntoContactsFrameLayout.createSequentialGroup()
                                .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jSplitIntoContactsFrameLayout.createSequentialGroup()
                                        .addComponent(jLabel13)
                                        .addGap(18, 18, 18))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jSplitIntoContactsFrameLayout.createSequentialGroup()
                                        .addComponent(jSplitIntoContactsSwapsCheckBox)
                                        .addGap(12, 12, 12)))
                                .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel19, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jSplitIntoContactsOrigIntoSourceCheckBox, javax.swing.GroupLayout.Alignment.LEADING))))))
                .addGap(24, 24, 24)
                .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jSplitIntoContactsFrameLayout.createSequentialGroup()
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 64, Short.MAX_VALUE)
                        .addGroup(jSplitIntoContactsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jSplitIntoContactsCancelButton)
                            .addComponent(jSplitIntoContactsOkButton)))
                    .addComponent(jSplitIntoContactsOrigIntoContactsCheckBox))
                .addContainerGap())
        );

        jSplitIntoTypeSettingsFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        jSplitIntoTypeSettingsFrame.setTitle("Split into...");
        jSplitIntoTypeSettingsFrame.setResizable(false);

        jLabel21.setFont(new java.awt.Font("Tahoma", 1, 18));
        jLabel21.setText("\"SPLIT INTO\" MULTIPLE COLUMNS MENU:");

        jLabel22.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel22.setText("Column to split:");

        jColumnToSplitLabel2.setFont(new java.awt.Font("Tahoma", 1, 14));
        jColumnToSplitLabel2.setText("0");

        jSplitIntoTypeSettingsOkButton.setText("OK");
        jSplitIntoTypeSettingsOkButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jSplitIntoTypeSettingsOkButtonMouseReleased(evt);
            }
        });

        jSplitIntoTypeSettingsCancelButton.setText("Cancel");
        jSplitIntoTypeSettingsCancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jSplitIntoTypeSettingsCancelButtonMouseReleased(evt);
            }
        });

        jLabel23.setFont(new java.awt.Font("Tahoma", 0, 12));
        jLabel23.setText("Number of columns:");

        jSplitIntoTypeSettingsNumberOfColumnsLabel.setFont(new java.awt.Font("Tahoma", 1, 14));
        jSplitIntoTypeSettingsNumberOfColumnsLabel.setText("0");

        jSplitIntoTypeSettingsToolBar.setFloatable(false);
        jSplitIntoTypeSettingsToolBar.setRollover(true);
        jSplitIntoTypeSettingsScrollPane.setViewportView(jSplitIntoTypeSettingsToolBar);

        javax.swing.GroupLayout jSplitIntoTypeSettingsFrameLayout = new javax.swing.GroupLayout(jSplitIntoTypeSettingsFrame.getContentPane());
        jSplitIntoTypeSettingsFrame.getContentPane().setLayout(jSplitIntoTypeSettingsFrameLayout);
        jSplitIntoTypeSettingsFrameLayout.setHorizontalGroup(
            jSplitIntoTypeSettingsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jSplitIntoTypeSettingsFrameLayout.createSequentialGroup()
                .addGroup(jSplitIntoTypeSettingsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jSplitIntoTypeSettingsFrameLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 395, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jSplitIntoTypeSettingsFrameLayout.createSequentialGroup()
                        .addGap(39, 39, 39)
                        .addGroup(jSplitIntoTypeSettingsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSplitIntoTypeSettingsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 520, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jSplitIntoTypeSettingsFrameLayout.createSequentialGroup()
                                .addGroup(jSplitIntoTypeSettingsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel22)
                                    .addComponent(jLabel23))
                                .addGap(34, 34, 34)
                                .addGroup(jSplitIntoTypeSettingsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jSplitIntoTypeSettingsNumberOfColumnsLabel)
                                    .addComponent(jColumnToSplitLabel2))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(0, 0, 0))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jSplitIntoTypeSettingsFrameLayout.createSequentialGroup()
                .addContainerGap(448, Short.MAX_VALUE)
                .addComponent(jSplitIntoTypeSettingsOkButton, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitIntoTypeSettingsCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jSplitIntoTypeSettingsFrameLayout.setVerticalGroup(
            jSplitIntoTypeSettingsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jSplitIntoTypeSettingsFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jSplitIntoTypeSettingsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(jColumnToSplitLabel2))
                .addGap(18, 18, 18)
                .addGroup(jSplitIntoTypeSettingsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel23)
                    .addComponent(jSplitIntoTypeSettingsNumberOfColumnsLabel))
                .addGap(18, 18, 18)
                .addComponent(jSplitIntoTypeSettingsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 67, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(jSplitIntoTypeSettingsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSplitIntoTypeSettingsCancelButton)
                    .addComponent(jSplitIntoTypeSettingsOkButton))
                .addContainerGap())
        );

        jCompiledDocTextArea.setColumns(20);
        jCompiledDocTextArea.setEditable(false);
        jCompiledDocTextArea.setFont(new java.awt.Font("Arial", 0, 12));
        jCompiledDocTextArea.setRows(5);
        jCompiledDocScrollPane.setViewportView(jCompiledDocTextArea);

        javax.swing.GroupLayout jCompiledDocTextFrameLayout = new javax.swing.GroupLayout(jCompiledDocTextFrame.getContentPane());
        jCompiledDocTextFrame.getContentPane().setLayout(jCompiledDocTextFrameLayout);
        jCompiledDocTextFrameLayout.setHorizontalGroup(
            jCompiledDocTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 680, Short.MAX_VALUE)
            .addGroup(jCompiledDocTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jCompiledDocTextFrameLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(jCompiledDocScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 668, Short.MAX_VALUE)
                    .addGap(0, 0, 0)))
        );
        jCompiledDocTextFrameLayout.setVerticalGroup(
            jCompiledDocTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 360, Short.MAX_VALUE)
            .addGroup(jCompiledDocTextFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jCompiledDocTextFrameLayout.createSequentialGroup()
                    .addGap(0, 0, 0)
                    .addComponent(jCompiledDocScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
                    .addGap(0, 0, 0)))
        );

        jFileChooserFrame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jFileChooserLabel.setFont(new java.awt.Font("Tahoma", 1, 24));
        jFileChooserLabel.setText("SAVING...");

        javax.swing.GroupLayout jFileChooserFrameLayout = new javax.swing.GroupLayout(jFileChooserFrame.getContentPane());
        jFileChooserFrame.getContentPane().setLayout(jFileChooserFrameLayout);
        jFileChooserFrameLayout.setHorizontalGroup(
            jFileChooserFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jFileChooserFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jFileChooserLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jFileChooserFrameLayout.setVerticalGroup(
            jFileChooserFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jFileChooserFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jFileChooserLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Contact Transmutator 1.0");
        setResizable(false);

        jMainLabel1.setFont(new java.awt.Font("Chiller", 1, 48));
        jMainLabel1.setText("Contact Transmutator");

        jBrowseButton1.setText("Browse...");
        jBrowseButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jBrowseButton1MouseReleased(evt);
            }
        });

        jSelectInputLabel1.setFont(new java.awt.Font("Arial", 0, 18));
        jSelectInputLabel1.setText("Please select the input file:");

        jNextButton1.setText("Next >");
        jNextButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jNextButton1MouseReleased(evt);
            }
        });

        jCancelButton1.setText("Cancel");
        jCancelButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jCancelButton1MouseReleased(evt);
            }
        });

        jBackButton1.setText("< Back");
        jBackButton1.setEnabled(false);

        jEncodingComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "UTF-8", "UTF-16", "US-ASCII ", "windows-1250", "windows-1252", "windows-1251", "ISO-8859-1", "ISO-8859-2", "ISO-8859-5", "KOI8-R" }));

        jEncodingLabel1.setFont(new java.awt.Font("Arial", 0, 18));
        jEncodingLabel1.setText("Encoding:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(154, 154, 154)
                .addComponent(jMainLabel1)
                .addContainerGap(80, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(383, Short.MAX_VALUE)
                .addComponent(jBackButton1)
                .addGap(4, 4, 4)
                .addComponent(jNextButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCancelButton1)
                .addContainerGap())
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(100, 100, 100)
                .addComponent(jSelectInputLabel1)
                .addContainerGap(286, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(136, 136, 136)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jEncodingLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(jEncodingComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jInputFileTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jBrowseButton1)))
                .addContainerGap(117, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jMainLabel1)
                .addGap(30, 30, 30)
                .addComponent(jSelectInputLabel1)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jInputFileTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBrowseButton1))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jEncodingLabel1)
                    .addComponent(jEncodingComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 75, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jBackButton1)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jCancelButton1)
                        .addComponent(jNextButton1)))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jNextButton1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jNextButton1MouseReleased

        //TODO: statusbar

        setCursor(Cursor.WAIT_CURSOR);

        String filePath = jInputFileTextField1.getText();

        // <editor-fold defaultstate="collapsed" desc="naplnenie InternalDoc a ColumnSchema">
        InputFilter inputFilter;
        inputFile = new File(filePath);
        //TODO: detect encoding
        String encoding = jEncodingComboBox1.getSelectedItem().toString();
        if (filePath.toLowerCase().endsWith(".csv")){
            inputFilter = new ReadCSV(inputFile.toString(),encoding,",","\"");
        } else if (filePath.toLowerCase().endsWith(".vcf")){
            inputFilter = new ReadVCF(inputFile.toString(),encoding);
        } else if (filePath.toLowerCase().endsWith(".ods")){
            try {
                inputFilter = new ODSInput(inputFile.toString());
            } catch (SAXException ex) {
                Logger.getLogger(ContactTransmutGUIMain.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this,"Problem with parsing of the document.","Error!",JOptionPane.ERROR_MESSAGE);
                return;
            } catch (ParserConfigurationException ex) {
                Logger.getLogger(ContactTransmutGUIMain.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this,"Problem with parsing of the document.","Error!",JOptionPane.ERROR_MESSAGE);
                return;
            } catch (IOException ex) {
                Logger.getLogger(ContactTransmutGUIMain.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this,"Unable to manipulate with the input file.","Error!",JOptionPane.ERROR_MESSAGE);
                return;
            } catch (Exception ex) {
                Logger.getLogger(ContactTransmutGUIMain.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this,"An error occured.","Error!",JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            JOptionPane.showMessageDialog(this,"Choose a valid file. (*.csv, *.ods, *.vcf)","Invalid file path!",JOptionPane.ERROR_MESSAGE);
            return;
        }

        internalDoc = inputFilter.read();
        columnSchema = inputFilter.getColumnSchema();
        //</editor-fold>

        jMainWindowFrame2.setTitle("Contact Transmutator - " + filePath);
       
        tableModel.initTable(internalDoc, columnSchema, jContactsListTable);

        
        comboMgr = new ComboBoxesManager();
        columnSchMgr = new ColumnSchemaManager();

        comboMgr.createMainComboBoxes();

        setVisible(false);
        jMainWindowFrame2.pack();
        jMainWindowFrame2.setVisible(true);

        updateTableWidths();

    }//GEN-LAST:event_jNextButton1MouseReleased

    private void jBrowseButton1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jBrowseButton1MouseReleased
        JFileChooser chooser = new JFileChooser();

        // <editor-fold defaultstate="collapsed" desc="filter typov suborov">
        FileFilter filter = null;
        filter = new FileFilter() {

            @Override
            public boolean accept(File f) {
                try {
                    if ((f.isFile() && (f.toString().toLowerCase().endsWith("csv")
                            || f.toString().toLowerCase().endsWith("ods")
                            || f.toString().toLowerCase().endsWith("vcf")))
                            || f.isDirectory()) {
                        return true;
                    } else {
                        return false;
                    }
                } catch (Exception e) {
                    //no idea why it comes out but everything works just OK
                    return true;
                }
            }

            @Override
            public String getDescription() {
                return "CSV, ODS, VCF files";
            }
        };
        // </editor-fold>
              
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(jPanel1);
        if(returnVal == JFileChooser.APPROVE_OPTION){
            jInputFileTextField1.setText(chooser.getSelectedFile().toString());
        }       
    }//GEN-LAST:event_jBrowseButton1MouseReleased

    private void jCancelButton1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jCancelButton1MouseReleased
        System.exit(0);
    }//GEN-LAST:event_jCancelButton1MouseReleased

    private void jMainWindowBackButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jMainWindowBackButtonMouseReleased

        if (!jMainWindowBackButton.isEnabled())
            return;

        setCursor(Cursor.DEFAULT_CURSOR);

        Object[] options = {"OK", "Cancel"};
        int n = JOptionPane.showOptionDialog(this, "All changes will be lost!", "Warning!", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,null, options, options[1]);
        if (n != 0){
            return;
        }

        jMainWindowFrame2.setVisible(false);
        jOriginalFileTextFrame.setVisible(false);
        jColumnSchemaTextFrame.setVisible(false);
        jCompiledDocTextFrame.setVisible(false);
        jInternalDocTextFrame.setVisible(false);
        jMainWindowShowCompiledDocButton.setEnabled(false);
        compiled = false;
        
        comboBoxes.clear();
        columnsToAdd.clear();
        splitIntoColumnsTypes.clear();
        jComboBoxesToolBar.removeAll();
        jColumnToSplitLabel.setText("-1");
        jColumnToAddLabel.setText("-1");
        jAddToBaseColumnTextField.setText("-1");
        
        setVisible(true);
    }//GEN-LAST:event_jMainWindowBackButtonMouseReleased

    private void jMainWindowCancelButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jMainWindowCancelButtonMouseReleased
        if (!jMainWindowCancelButton.isEnabled())
            return;
        Object[] options = {"Yes", "No."};
        int n = JOptionPane.showOptionDialog(this, "Really exit? All changes will be lost", "Exit?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        if (n == 0) {
            System.exit(0);
        }
    }//GEN-LAST:event_jMainWindowCancelButtonMouseReleased

    private void jMainWindowNextButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jMainWindowNextButtonMouseReleased
        if (!jMainWindowNextButton.isEnabled())
            return;
        saving = true;
        jMainWindowRefreshButton2MouseReleased(evt);
    }//GEN-LAST:event_jMainWindowNextButtonMouseReleased

    private void jMainWindowShowCompiledDocButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jMainWindowShowCompiledDocButtonMouseReleased
        if (!jMainWindowShowCompiledDocButton.isEnabled()){
            return;
        }
        jCompiledDocTextFrame.setLocation(61, 61);
        jCompiledDocTextFrame.setTitle("Compiled Doc");
        
        if (compiledDoc == null){
            jCompiledDocTextArea.setText("Document does not exist yet. Please REFRESH first.");
        } else {        
            //show CompiledDoc
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = null;
            try {
                trans = tf.newTransformer();
                trans.transform(new DOMSource(compiledDoc), new StreamResult(stream));
            } catch (TransformerException ex) {
                Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
            }
            String rawCD = stream.toString();
            rawCD = rawCD.replaceAll("<", "\n<");
            rawCD = rawCD.replaceAll("<contacts>", "<contacts>\n");
            rawCD = rawCD.replaceAll("</contacts>", "\n</contacts>");
            rawCD = rawCD.replaceAll("</contact>", "\n</contact>\n");
            rawCD = rawCD.replaceAll("\n</", "</");
            rawCD = rawCD.replaceAll("</root>", "\n</root>");
            jCompiledDocTextArea.setText(rawCD);
        }       

        jCompiledDocTextFrame.pack();
        jCompiledDocTextFrame.setVisible(true);

        
        // <editor-fold defaultstate="collapsed" desc=" Show Original Text option commented out">
//        jOriginalFileTextFrame.setLocation(1, 1);
//        if (jOriginalFileTextFrame.isVisible()) {
//            jOriginalFileTextFrame.requestFocus();
//        }

//            String line = null;
//            BufferedReader buff = null;
//            try {
//                buff = new BufferedReader(
//                        new InputStreamReader(
//                        new FileInputStream(
//                        inputFile)));
//
//                jOriginalFileTextArea.setText(null);
//                while ((line = buff.readLine()) != null) {
//                    jOriginalFileTextArea.append(line + "\n");
//                }
//            } catch (FileNotFoundException ex) {
//                JOptionPane.showMessageDialog(this, "The file specified was not found. Check the file path!", "File not found!", JOptionPane.ERROR_MESSAGE);
//                return;
//            } catch (IOException ex) {
//                JOptionPane.showMessageDialog(this, "An error occured. Please try again.", "Error!", JOptionPane.ERROR_MESSAGE);
//                return;
//            } finally {
//                try {
//                    if (buff != null) {
//                        buff.close();
//                    }
//                } catch (IOException ex) {
//                    JOptionPane.showMessageDialog(this, "An error occured. Please try again.", "Error!", JOptionPane.ERROR_MESSAGE);
//                    return;
//                }
//            }

//        jOriginalFileTextFrame.setTitle(inputFile.getName());
//        jOriginalFileTextFrame.pack();
//        jOriginalFileTextFrame.setVisible(true);
        // </editor-fold>
    }//GEN-LAST:event_jMainWindowShowCompiledDocButtonMouseReleased

    private void jMainWindowShowColumnSchemaButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jMainWindowShowColumnSchemaButtonMouseReleased
        jColumnSchemaTextFrame.setLocation(1, 1);
        jColumnSchemaTextFrame.setTitle("Column Schema");
        if (jColumnSchemaTextFrame.isVisible()) {
            jColumnSchemaTextFrame.requestFocus();
        } else {
            String rawIntDoc = columnSchema.toString();
            rawIntDoc = rawIntDoc.replaceAll("/>", "/>\n");
            rawIntDoc = rawIntDoc.replaceAll("</mergeset>", "</mergeset>\n");
            rawIntDoc = rawIntDoc.replaceAll("<column>", "\n<column>\n");
            rawIntDoc = rawIntDoc.replaceAll("</column>", "</column>\n");
            rawIntDoc = rawIntDoc.replaceAll("columnschema>", "columnschema>\n\n");
            rawIntDoc = rawIntDoc.replaceAll("<root>", "\n<root>\n");
            rawIntDoc = rawIntDoc.replaceAll("\">", "\">\n");

            jColumnSchemaTextArea.setText(rawIntDoc);

            jColumnSchemaTextFrame.pack();
            jColumnSchemaTextFrame.setVisible(true);
        }
    }//GEN-LAST:event_jMainWindowShowColumnSchemaButtonMouseReleased

    private void jMainWindowShowInternalDocButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jMainWindowShowInternalDocButtonMouseReleased
        jInternalDocTextFrame.setLocation(31, 31);
        if (jInternalDocTextFrame.isVisible()) {
            jInternalDocTextFrame.requestFocus();
        } else {

            if (internalDoc == null) {
                jInternalDocTextArea.setText("Document is null!!!");
            } else {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer trans = null;
                try {
                    trans = tf.newTransformer();
                    trans.transform(new DOMSource(internalDoc), new StreamResult(stream));
                } catch (TransformerException ex) {
                    Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
                }
                String rawIntDoc = stream.toString();
                rawIntDoc = rawIntDoc.replaceAll("/>", "/>\n");
                rawIntDoc = rawIntDoc.replaceAll("</data>", "</data>\n");
                rawIntDoc = rawIntDoc.replaceAll("<contact>", "\n<contact>\n");
                rawIntDoc = rawIntDoc.replaceAll("</contact>", "</contact>\n");
                rawIntDoc = rawIntDoc.replaceAll("<root>", "\n<root>\n");
                rawIntDoc = rawIntDoc.replaceAll("uncategorized>", "uncategorized>\n");
                jInternalDocTextArea.setText(rawIntDoc);
            }

            jInternalDocTextFrame.pack();
            jInternalDocTextFrame.setVisible(true);
        }
    }//GEN-LAST:event_jMainWindowShowInternalDocButtonMouseReleased

    private void jAddToCancelButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jAddToCancelButtonMouseReleased
        int myColumn = Integer.parseInt(jColumnToAddLabel.getText());
        int indexSetBefore = 0;
        if (columnSchema.isColumnMergedInOther(myColumn)){
            indexSetBefore = comboMgr.getIndexOfValue("ADD_TO_COLUMN_#...");
        }
        else if(columnSchema.isColumnAggregated(myColumn))
        {
            indexSetBefore = comboMgr.getIndexOfValue("SPIT_INTO...");
        }
        else{
            indexSetBefore = comboMgr.getIndexOfValue(columnSchema.queryCandidateType(myColumn));
            comboBoxes.get(myColumn).setSelectedIndex(9);
        }
        comboBoxes.get(myColumn).setSelectedIndex(indexSetBefore);
        jAddToFrame.setVisible(false);
        setButtonsEnabled(jMainWindowFrame2, true);
    }//GEN-LAST:event_jAddToCancelButtonMouseReleased

    private void jAddToSubmitButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jAddToSubmitButtonMouseReleased
        jAddToSubmitButtonMouseReleasedAction();
    }//GEN-LAST:event_jAddToSubmitButtonMouseReleased

    private void jAddToPlusButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jAddToPlusButtonMouseReleased
        jAddToToolBar.add(new JLabel(jAddToDelimeterTextField.getText()));
        columnsToAdd.add(new JTextField(2));
        jAddToToolBar.add(columnsToAdd.get(columnsToAdd.size()-1));
        jAddToToolBar.setVisible(false);
        jAddToToolBar.setVisible(true);
    }//GEN-LAST:event_jAddToPlusButtonMouseReleased

    private void jAddToOkButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jAddToOkButtonMouseReleased
        //check inputs
        String myColumn = jColumnToAddLabel.getText();
        String baseColumnStr = jAddToBaseColumnTextField.getText();
        int baseColumn  = Integer.parseInt(baseColumnStr);
        boolean myColumnAdded = false;
        ArrayList<Integer> columnsToAddNumbers = new ArrayList<Integer>();
        for (int i = 0; i<columnsToAdd.size(); i++){
            String component1 = columnsToAdd.get(i).getText();
            if (component1.equals(""))
                continue;
            if (component1.equals(myColumn))
                myColumnAdded = true;
            if (component1.equals(baseColumnStr)){
                JOptionPane.showMessageDialog(this,"Cannot add column to itself.","Error",JOptionPane.ERROR_MESSAGE);
                return;
            }
            for (int j = i+1; j<columnsToAdd.size(); j++){
                String component2 = columnsToAdd.get(j).getText();
                if (component2.equals(""))
                    continue;
                if (component1.equals(component2)){
                    JOptionPane.showMessageDialog(this,"Cannot add column to itself.","Error",JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }
        if (!myColumnAdded){
            JOptionPane.showMessageDialog(this,"You didn't add the \"Column to add\".","Info",JOptionPane.INFORMATION_MESSAGE);
        }

        try{
            for (int i=0; i<columnsToAdd.size(); i++){
                if (columnsToAdd.get(i).getText().equals(""))
                    continue;
                int columnNumber = Integer.parseInt(columnsToAdd.get(i).getText());
                if (columnNumber >= jContactsListTable.getColumnCount() ){
                    JOptionPane.showMessageDialog(this,"Too high column number","Error",JOptionPane.ERROR_MESSAGE);
                    return;
                }
                columnsToAddNumbers.add(columnNumber);
            }
        }catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,"Invalid column number","Error",JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (columnSchema.isColumnMergedInOther(baseColumn)){
            if (columnSchema.queryMergeOrder(baseColumn) != 1){
                Object[] options = {"Yes", "No, I will change the settings"};
                int n = JOptionPane.showOptionDialog(this, "The base column is added to another. Cancel the other addition?", "Problem!", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,null, options, options[1]);
                if (n == 0){
                    int mergeset = columnSchema.queryMergeSet(baseColumn);
                    HashMap<Integer, Integer> allMergesetMembers = columnSchema.getAllMergesetMembers(mergeset);
                    for (Integer i : allMergesetMembers.values()){
                        comboBoxes.get(i).setSelectedIndex(9);
                    }
                    columnSchema.deleteMergeset(mergeset);
                    columnSchMgr.update();
                } else {
                    return;
                }
            }

            //if this mergeset existed before, it could loose some components -> set all to Note
            if (columnSchema.queryMergeOrder(baseColumn) == 1){
                int mergeset = columnSchema.queryMergeSet(baseColumn);
                HashMap<Integer, Integer> allMergesetMembers = columnSchema.getAllMergesetMembers(mergeset);
                for (Integer i : allMergesetMembers.values()){
                    comboBoxes.get(i).setSelectedIndex(9);
                }
            }
        }

        //set all merge components to add to
        for (Integer i : columnsToAddNumbers){
            comboBoxes.get(i).setSelectedIndex(comboMgr.getIndexOfValue("ADD_TO_COLUMN_#..."));
        }
        columnSchMgr.update();
        comboMgr.updateComboBoxesEnabledValues(comboBoxes);
        comboMgr.updateAddToNumbers();

        setButtonsEnabled(jMainWindowFrame2, true);

        compiled = false;

        jAddToFrame.setVisible(false);
    }//GEN-LAST:event_jAddToOkButtonMouseReleased

    private void jAddToBaseColumnTextFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jAddToBaseColumnTextFieldKeyTyped
        jAddToOkButton.setEnabled(false);
    }//GEN-LAST:event_jAddToBaseColumnTextFieldKeyTyped

    private void jSplitIntoCancelButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSplitIntoCancelButtonMouseReleased
        if (!jSplitIntoCancelButton.isEnabled())
            return;

        int myColumn = Integer.parseInt(jColumnToSplitLabel.getText());

        columnSchMgr.popColumnSchema();
        if (columnSchema.isColumnMergedInOther(myColumn) && columnSchema.queryMergeOrder(myColumn) !=1){
            comboBoxes.get(myColumn).setSelectedIndex(comboMgr.getIndexOfValue("ADD_TO_COLUMN_#..."));
        }
        else if (!columnSchema.isColumnAggregated(myColumn)){
            comboBoxes.get(myColumn).setSelectedIndex(comboMgr.getIndexOfValue(columnSchema.queryCandidateType(myColumn)));
        }

        setButtonsEnabled(jMainWindowFrame2, true);

        jSplitIntoFrame.setVisible(false);
        jSplitIntoContactsFrame.setVisible(false);
        jSplitIntoTypeSettingsFrame.setVisible(false);
    }//GEN-LAST:event_jSplitIntoCancelButtonMouseReleased

    private void jSplitIntoNumberOfColumnsSettingsButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSplitIntoNumberOfColumnsSettingsButtonMouseReleased
        if (!jSplitIntoNumberOfColumnsSettingsButton.isEnabled())
            return;

        setButtonsEnabled(jSplitIntoFrame, false);

        columnSchMgr.pushColumnSchema();
        try{
            fillInSplitIntoTypesSettingsForm();
        } catch (NumberFormatException ex){
            JOptionPane.showMessageDialog(this,"Invalid number of columns","Error",JOptionPane.ERROR_MESSAGE);
            columnSchMgr.popColumnSchema();
            setButtonsEnabled(jSplitIntoFrame, true);
            return;
        }
        jSplitIntoTypeSettingsFrame.pack();
        jSplitIntoTypeSettingsFrame.setLocation((maxWidth-jSplitIntoTypeSettingsFrame.getWidth())/2, (maxHeight-jSplitIntoTypeSettingsFrame.getHeight())/2);
        jSplitIntoTypeSettingsFrame.setVisible(true);
    }//GEN-LAST:event_jSplitIntoNumberOfColumnsSettingsButtonMouseReleased

    private void jSplitIntoTypeSettingsCancelButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSplitIntoTypeSettingsCancelButtonMouseReleased
        //revert changes in this window
        columnSchMgr.popColumnSchema();
        fillInSplitIntoTypesSettingsForm();
        setButtonsEnabled(jSplitIntoFrame, true);
        jSplitIntoTypeSettingsFrame.setVisible(false);
    }//GEN-LAST:event_jSplitIntoTypeSettingsCancelButtonMouseReleased

    private void jSplitIntoTypeSettingsOkButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSplitIntoTypeSettingsOkButtonMouseReleased
        columnSchMgr.discardPopColumnSchema();
        setButtonsEnabled(jSplitIntoFrame, true);
        jSplitIntoTypeSettingsFrame.pack();
        jSplitIntoTypeSettingsFrame.setLocation((maxWidth-jSplitIntoTypeSettingsFrame.getWidth())/2, (maxHeight-jSplitIntoTypeSettingsFrame.getHeight())/2);
        jSplitIntoTypeSettingsFrame.setVisible(false);
    }//GEN-LAST:event_jSplitIntoTypeSettingsOkButtonMouseReleased

    private void jSplitIntoOkButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSplitIntoOkButtonMouseReleased
        if (!jSplitIntoOkButton.isEnabled())
            return;

        try{
            int temp = Integer.parseInt(jSplitIntoNumberOfColumnsTextField.getText());
        } catch (NumberFormatException ex){
            JOptionPane.showMessageDialog(this,"Invalid number of columns","Error",JOptionPane.ERROR_MESSAGE);
            return;
        }

        columnSchMgr.discardPopColumnSchema();
        columnSchMgr.update();
        comboMgr.updateComboBoxesEnabledValues(comboBoxes);
        comboMgr.updateAddToNumbers();

        setButtonsEnabled(jMainWindowFrame2, true);

        compiled = false;

        jSplitIntoFrame.setVisible(false);
        jSplitIntoContactsFrame.setVisible(false);
        jSplitIntoTypeSettingsFrame.setVisible(false);
    }//GEN-LAST:event_jSplitIntoOkButtonMouseReleased

    private void jSplitIntoContactsCheckBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSplitIntoContactsCheckBoxMouseClicked
        if (!jSplitIntoContactsCheckBox.isEnabled())
            return;

        if (!jSplitIntoContactsCheckBox.isSelected()){
            jSplitIntoContactsSettingsButton.setEnabled(false);
            jSplitIntoNumberOfColumnsTextField.setEnabled(true);
            jSplitIntoNumberOfColumnsSettingsButton.setEnabled(true);
            jSplitIntoDelimeterTextField.setEnabled(true);
            columnSchMgr.update();
            return;
        }
        columnSchMgr.pushColumnSchema();
        jSplitIntoContactsSettingsButton.setEnabled(true);
        jSplitIntoNumberOfColumnsTextField.setEnabled(false);
        jSplitIntoNumberOfColumnsSettingsButton.setEnabled(false);
        jSplitIntoDelimeterTextField.setEnabled(false);

        setButtonsEnabled(jSplitIntoFrame, false);

        jSplitIntoContactsFrame.pack();
        jSplitIntoContactsFrame.setVisible(true);
    }//GEN-LAST:event_jSplitIntoContactsCheckBoxMouseClicked

    private void jSplitIntoContactsColumnTypesSettingsButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSplitIntoContactsColumnTypesSettingsButtonMouseReleased
        columnSchMgr.pushColumnSchema();
        jSplitIntoNumberOfColumnsTextField.setText(jSplitIntoContactsNumberOfColumnsTextField.getText());
        try{
            fillInSplitIntoTypesSettingsForm();
        } catch (NumberFormatException ex){
            JOptionPane.showMessageDialog(this,"Invalid number of columns","Error",JOptionPane.ERROR_MESSAGE);
            columnSchMgr.popColumnSchema();
            return;
        }

        setButtonsEnabled(jSplitIntoContactsFrame, false);

        jSplitIntoTypeSettingsFrame.pack();
        jSplitIntoTypeSettingsFrame.setVisible(true);
    }//GEN-LAST:event_jSplitIntoContactsColumnTypesSettingsButtonMouseReleased

    private void jSplitIntoContactsCancelButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSplitIntoContactsCancelButtonMouseReleased
        if (!jSplitIntoContactsCancelButton.isEnabled())
            return;
        setButtonsEnabled(jSplitIntoFrame, true);

        jSplitIntoContactsFrame.setVisible(false);
        int colNumber = Integer.parseInt(jColumnToSplitLabel.getText());
        columnSchMgr.popColumnSchema();
        //fill in the settings form
        jSplitIntoContactsNumberOfColumnsTextField.setText(String.valueOf(columnSchema.queryAggregateSettingNumberofcolumns(colNumber)));
        jSplitIntoContactsDelimeterOfContactsTextField.setText(columnSchema.queryAggregateSettingSeparatecontactsdelimiter(colNumber));
        jSplitIntoContactsDelimeterOfColumnsTextField.setText(columnSchema.queryAggregateSettingDelimiter(colNumber));
        jSplitIntoContactsAreEmployeesCheckBox.setSelected(columnSchema.queryAggregateSettingEmployees(colNumber));
        jSplitIntoContactsOrigIntoSourceCheckBox.setSelected(columnSchema.queryAggregateSettingOriginalsourcenote(colNumber));
        jSplitIntoContactsOrigIntoContactsCheckBox.setSelected(columnSchema.queryAggregateSettingOriginaltargetnote(colNumber));
    }//GEN-LAST:event_jSplitIntoContactsCancelButtonMouseReleased

    private void jSplitIntoContactsOkButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSplitIntoContactsOkButtonMouseReleased
        if (!jSplitIntoContactsOkButton.isEnabled())
            return;
        setButtonsEnabled(jSplitIntoFrame, true);

        columnSchMgr.discardPopColumnSchema();
        columnSchMgr.update();
        jSplitIntoContactsFrame.setVisible(false);
        jSplitIntoNumberOfColumnsTextField.setText(jSplitIntoContactsNumberOfColumnsTextField.getText());
        jSplitIntoDelimeterTextField.setText(jSplitIntoContactsDelimeterOfColumnsTextField.getText());
    }//GEN-LAST:event_jSplitIntoContactsOkButtonMouseReleased

    private void jSplitIntoContactsSettingsButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSplitIntoContactsSettingsButtonMouseReleased
        if (!jSplitIntoContactsSettingsButton.isEnabled())
            return;
        columnSchMgr.pushColumnSchema();

        setButtonsEnabled(jSplitIntoFrame, false);

        jSplitIntoContactsFrame.setVisible(true);
    }//GEN-LAST:event_jSplitIntoContactsSettingsButtonMouseReleased

    private void jMainWindowRefreshButton2MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jMainWindowRefreshButton2MouseReleased
        if (!jMainWindowRefreshButton2.isEnabled())
            return;
        setButtonsEnabled(jMainWindowFrame2, false);
        tableModel.setCellsAreEditable(false);

        if (!compiled){
            jMainWindowRefreshButton2.setVisible(false);
            jRefreshProgressBar2.setVisible(true);
            jMainWindowStopButton2.setVisible(true);
        }

        int rowCount = tableModel.getRowCount();
        int divider = 1;
        if (rowCount > 100){
            jRefreshProgressBar2.setMaximum(100);
            divider = rowCount/100;
        } else {
            jRefreshProgressBar2.setMaximum(rowCount);
        }
        jRefreshProgressBar2.setValue(0);
        jRefreshProgressBar2.setIndeterminate(false);

        refreshSwingWorker = new RefreshSwingWorker();
        updateStatusbarSwingWorker = new UpdateStatusbarSwingWorker(divider);
        updateStatusbarSwingWorker.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if ("progress".equals(evt.getPropertyName())) {
                    int value = (Integer) evt.getNewValue();
                    jRefreshProgressBar2.setValue(value);
                    if (value >= 99){
                        jRefreshProgressBar2.setIndeterminate(true);
                        jRefreshProgressBar2.setToolTipText("Refreshing table, please wait!");
                        jRefreshProgressBar2.setString("Refreshing table, please wait!");
                    } else {
                        jRefreshProgressBar2.setToolTipText(value+"%");
                        jRefreshProgressBar2.setString(value+"%");
                    }
                }
            }
        });
        refreshSwingWorker.execute();
        updateStatusbarSwingWorker.execute();

    }//GEN-LAST:event_jMainWindowRefreshButton2MouseReleased

    private void jMainWindowStopButton2MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jMainWindowStopButton2MouseReleased
        compiled = false;

        boolean cancel = false;
        try {
            cancel = refreshSwingWorker.cancel(true);
        } catch (Exception e) {
        }
        if (!cancel)
            JOptionPane.showMessageDialog(this,"Sorry. Not supported operation yet.","Cancel processing.",JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_jMainWindowStopButton2MouseReleased

    private void jMainWindowAddColumnButton2MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jMainWindowAddColumnButton2MouseReleased
        compiled = false;

        if (!jMainWindowAddColumnButton2.isEnabled())
            return;
        columnSchema.addColumn();

        //refresh gui
        comboBoxes.clear();
        columnsToAdd.clear();
        splitIntoColumnsTypes.clear();
        jComboBoxesToolBar.removeAll();
        jColumnToSplitLabel.setText("-1");
        jColumnToAddLabel.setText("-1");
        jAddToBaseColumnTextField.setText("-1");

        tableModel.initTable(internalDoc, columnSchema, jContactsListTable);

        comboMgr.createMainComboBoxes();

        jMainWindowFrame2.repaint();
        jMainWindowFrame2.setVisible(true);

        updateTableWidths();


    }//GEN-LAST:event_jMainWindowAddColumnButton2MouseReleased


    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ContactTransmutGUIMain().setVisible(true);
            }
        });
    }

    private void setButtonsEnabled(javax.swing.JFrame window, boolean value){
        if (jMainWindowFrame2.equals(window)){
            jMainWindowBackButton.setEnabled(value);
            jMainWindowAddColumnButton2.setEnabled(value);
            jMainWindowNextButton.setEnabled(value);
            jMainWindowRefreshButton2.setEnabled(value);
            for (JComboBox box : comboBoxes){
                box.setEnabled(value);
            }
            return;
        }
        if (jSplitIntoFrame.equals(window)){
            jSplitIntoCancelButton.setEnabled(value);
            jSplitIntoDelimeterTextField.setEnabled(value);
            jSplitIntoNumberOfColumnsSettingsButton.setEnabled(value);
            jSplitIntoNumberOfColumnsTextField.setEnabled(value);
            jSplitIntoOkButton.setEnabled(value);
        }

        //TODO: change after implementing of intoSeparateContacts
    }

    private void jAddToSubmitButtonMouseReleasedAction(){
        int myColumn = Integer.parseInt(jColumnToAddLabel.getText());
        int baseColumn = 0;
        try{
            baseColumn = Integer.parseInt(jAddToBaseColumnTextField.getText());
        } catch (NumberFormatException ex){
            JOptionPane.showMessageDialog(this,"Type valid base column number","Error",JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (columnSchema.isColumnMergedInOther(baseColumn) && columnSchema.queryMergeOrder(baseColumn) != 1){
            JOptionPane.showMessageDialog(this,"The base column has allready been added to another column.","Error",JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (myColumn == baseColumn){
            JOptionPane.showMessageDialog(this,"Cannot add column to itself.","Error",JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (columnSchema.isColumnAggregated(baseColumn)){
            JOptionPane.showMessageDialog(this,"Cannot add columns to the column that will be splitted.","Error",JOptionPane.ERROR_MESSAGE);
            return;
        }

        jAddToOkButton.setEnabled(true);
        columnsToAdd.clear();
        jAddToToolBar.removeAll();
        jAddToToolBar.add(new JLabel(String.valueOf(baseColumn)));
        if (columnSchema.queryMergeSet(baseColumn) == null){
            jAddToToolBar.add(new JLabel(jAddToDelimeterTextField.getText()));
            columnsToAdd.add(new JTextField(String.valueOf(myColumn),2));
            jAddToToolBar.add(columnsToAdd.get(0));
            for (int i=0; i<10; i++){
                jAddToToolBar.add(new JLabel(jAddToDelimeterTextField.getText()));
                columnsToAdd.add(new JTextField(2));
                jAddToToolBar.add(columnsToAdd.get(i+1));
            }
            jAddToToolBar.setVisible(false);
            jAddToToolBar.setVisible(true);
            return;
        }
        HashMap<Integer, Integer> allMergesetMembers = columnSchema.getAllMergesetMembers(columnSchema.queryMergeSet(baseColumn));
        for (int i = 1; i< allMergesetMembers.size(); i++){
            jAddToToolBar.add(new JLabel(jAddToDelimeterTextField.getText()));
            columnsToAdd.add(new JTextField(String.valueOf(allMergesetMembers.get(i+1)),2));
            jAddToToolBar.add(columnsToAdd.get(i-1));
        }
        for (int i = allMergesetMembers.size(); i < 13; i++) {
            jAddToToolBar.add(new JLabel(jAddToDelimeterTextField.getText()));
            columnsToAdd.add(new JTextField(2));
            jAddToToolBar.add(columnsToAdd.get(i-1));
        }
        jAddToToolBar.setVisible(false);
        jAddToToolBar.setVisible(true);
        jAddToPlusButton.setEnabled(true);
        jAddToPlusButton.setVisible(false);
        jAddToPlusButton.setVisible(true);
        return;
    }

    private void fillInSplitIntoTypesSettingsForm() throws NumberFormatException{
        int columnNumber = Integer.parseInt(jColumnToSplitLabel.getText());
        int numberOfColumns = Integer.parseInt(jSplitIntoNumberOfColumnsTextField.getText());
        jSplitIntoTypeSettingsToolBar.removeAll();
        jSplitIntoTypeSettingsNumberOfColumnsLabel.setText(String.valueOf(numberOfColumns));
        ArrayList<Object[]> itemsList = new ArrayList<Object[]>();
        updateEnabled = false;
        //columnSchMgr.pushColumnSchema();
        for (int i = 0; i < numberOfColumns; i++) {
            // <editor-fold defaultstate="collapsed" desc="fill in the values">
            itemsList.add(new Object[]{
                        new ComboItem(VCFTypesEnum.Formatted_Name.toDisplayString()), //0
                        new ComboItem(VCFTypesEnum.Name.toDisplayString()), //1
                        new ComboItem(VCFTypesEnum.Telephone.toDisplayString()), //2
                        new ComboItem(VCFTypesEnum.Telephone_home.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Telephone_work.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Telephone_work_fax.toDisplayString()), //5
                        new ComboItem(VCFTypesEnum.Telephone_work_video.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Email.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Email_work.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Note.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Photograph.toDisplayString()), //10
                        new ComboItem(VCFTypesEnum.Sound.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Delivery_Address.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Delivery_Address_home.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Delivery_Address_work.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Label_Address.toDisplayString()), //15
                        new ComboItem(VCFTypesEnum.Label_Address_home.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Label_Address_work.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Birthday.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Nickname.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Organization_Name_or_Organizational_unit.toDisplayString()), //20
                        new ComboItem(VCFTypesEnum.Role_or_occupation.toDisplayString()),
                        new ComboItem(VCFTypesEnum.Logo.toDisplayString()),
                        new ComboItem(VCFTypesEnum.URL.toDisplayString()),
                        new ComboItem(VCFTypesEnum.URL_home.toDisplayString()),
                        new ComboItem(VCFTypesEnum.URL_work.toDisplayString()), //25
                        new ComboItem(VCFTypesEnum.Unique_Identifier.toDisplayString()) //26
                    });
            //</editor-fold>
            splitIntoColumnsTypes.add(new JComboBox(itemsList.get(i)));
            splitIntoColumnsTypes.get(i).setRenderer(new ComboRenderer());
            splitIntoColumnsTypes.get(i).addActionListener(new ComboListener(splitIntoColumnsTypes.get(i)));
            String type = columnSchema.queryAggregatedCandidateType(columnNumber, i);
            int temp = comboMgr.getIndexOfValue(type);
            splitIntoColumnsTypes.get(i).setSelectedIndex(temp); //will be set to note if there is no type
            jSplitIntoTypeSettingsToolBar.add(splitIntoColumnsTypes.get(i));
        }
        updateEnabled = true;
        //columnSchMgr.popColumnSchema();
        columnSchMgr.update();
        comboMgr.updateComboBoxesEnabledValues(splitIntoColumnsTypes);
    }

    private void prepareSplitIntoWindow(int colNumber) {
        jColumnToSplitLabel.setText(String.valueOf(colNumber));
        jColumnToSplitLabel1.setText(String.valueOf(colNumber));
        jColumnToSplitLabel2.setText(String.valueOf(colNumber));
        if (columnSchema.isColumnAggregated(colNumber)) {
            jSplitIntoNumberOfColumnsTextField.setText(String.valueOf(columnSchema.queryAggregateSettingNumberofcolumns(colNumber)));
            jSplitIntoDelimeterTextField.setText(columnSchema.queryAggregateSettingDelimiter(colNumber));
            if (columnSchema.queryAggregateSettingIntoseparatecontacts(colNumber)) {
                jSplitIntoContactsCheckBox.setSelected(true);
                jSplitIntoContactsSettingsButton.setEnabled(true);
                jSplitIntoNumberOfColumnsTextField.setEnabled(false);
                jSplitIntoNumberOfColumnsSettingsButton.setEnabled(false);
                jSplitIntoDelimeterTextField.setEnabled(false);
                //fill in the settings form
                jSplitIntoContactsNumberOfColumnsTextField.setText(String.valueOf(columnSchema.queryAggregateSettingNumberofcolumns(colNumber)));
                jSplitIntoContactsDelimeterOfContactsTextField.setText(columnSchema.queryAggregateSettingSeparatecontactsdelimiter(colNumber));
                jSplitIntoContactsDelimeterOfColumnsTextField.setText(columnSchema.queryAggregateSettingDelimiter(colNumber));
                jSplitIntoContactsAreEmployeesCheckBox.setSelected(columnSchema.queryAggregateSettingEmployees(colNumber));
                jSplitIntoContactsOrigIntoSourceCheckBox.setSelected(columnSchema.queryAggregateSettingOriginalsourcenote(colNumber));
                jSplitIntoContactsOrigIntoContactsCheckBox.setSelected(columnSchema.queryAggregateSettingOriginaltargetnote(colNumber));
            }
        } else {
            jSplitIntoNumberOfColumnsTextField.setText("0");
            jSplitIntoDelimeterTextField.setText(",");
            jSplitIntoContactsCheckBox.setSelected(false);
            jSplitIntoContactsSettingsButton.setEnabled(false);
            jSplitIntoNumberOfColumnsTextField.setEnabled(true);
            jSplitIntoNumberOfColumnsSettingsButton.setEnabled(true);
            jSplitIntoDelimeterTextField.setEnabled(true);
        }
    }

    private void prepareAddToWindow(int columnNumber){
        jAddToPlusButton.setEnabled(false);
        jColumnToAddLabel.setText(String.valueOf(columnNumber));
        jAddToToolBar.removeAll();
        jAddToBaseColumnTextField.setText("");
        if (columnSchema.isColumnMergedInOther(columnNumber)) {
            int mergsetNumber = columnSchema.queryMergeSet(columnNumber);
            int firstInMergset = columnSchema.getAllMergesetMembers(mergsetNumber).get(1);
            jAddToBaseColumnTextField.setText(String.valueOf(firstInMergset));
            jAddToDelimeterTextField.setText(columnSchema.queryMergesetDelimiter(mergsetNumber));
            jAddToSubmitButtonMouseReleasedAction();
        }
    }

    private void updateTableWidths(){
        TableColumn column = null;
        int width = comboBoxes.get(0).getWidth();
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            column = jContactsListTable.getColumnModel().getColumn(i);
            column.setWidth(width);
            column.setMaxWidth(width);
            column.setMinWidth(width);
            column.setPreferredWidth(width);
            column.setResizable(false);
        }
        jMainWindowFrame2.repaint();
    }

    // <editor-fold defaultstate="collapsed" desc="Combo Box">
    private class ComboRenderer extends JLabel implements ListCellRenderer {

        public ComboRenderer() {
            setOpaque(true);
            setBorder(new EmptyBorder(1, 1, 1, 1));
        }

        public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            if (!((CanEnable) value).isEnabled()) {
                setBackground(list.getBackground());
                setForeground(UIManager.getColor("Label.disabledForeground"));
            }
            setFont(list.getFont());
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    private class ComboListener implements ActionListener {

        JComboBox combo;
        Object currentItem;

        ComboListener(JComboBox combo) {
            this.combo = combo;
            combo.setSelectedIndex(0);
            currentItem = combo.getSelectedItem();
        }

        public void actionPerformed(ActionEvent e) {
            Object tempItem = combo.getSelectedItem();
            if (!((CanEnable) tempItem).isEnabled()) {
                combo.setSelectedItem(currentItem);
            } else {
                combo.setSelectedItem(tempItem);
                if (!updateEnabled){
                    currentItem = combo.getSelectedItem();
                    return;
                }
                //get the number of column currently changed
                int columnNumber = comboMgr.getIndexOfComboBox(combo);
                
                //if a type was selected and the combo box is from main window
                String tempItemVCFFormat = comboMgr.getSelectedValueStr(combo);
                if (comboMgr.getIndexOfValue(tempItemVCFFormat) <= (comboMgr.getIndexOfValue("DELETE_THIS")) && columnNumber >= 0){
                    columnSchMgr.update();
                    comboMgr.updateComboBoxesEnabledValues(comboBoxes);
                    comboMgr.updateAddToNumbers();
                }

                //if the add to was selected
                else if (comboMgr.getIndexOfValue(tempItemVCFFormat) == comboMgr.getIndexOfValue("ADD_TO_COLUMN_#...")) {
                    if (!jAddToFrame.isVisible()){
                        prepareAddToWindow(columnNumber);
                        setButtonsEnabled(jMainWindowFrame2, false);
                        jAddToFrame.pack();
                        jAddToFrame.setLocation((maxWidth-jAddToFrame.getWidth())/2, (maxHeight-jAddToFrame.getHeight())/2);
                        jAddToFrame.setVisible(true);
                    }
                }
                    
                //if the SPLIT INTO was selected
                else if (comboMgr.getIndexOfValue(tempItemVCFFormat) == comboMgr.getIndexOfValue("SPLIT_INTO...")){
                    if (!jSplitIntoFrame.isVisible()){
                        //if the column is aggregated
                        if (columnSchema.isColumnAggregated(columnNumber)){
                            columnSchMgr.pushColumnSchema();
                            prepareSplitIntoWindow(columnNumber);
                            setButtonsEnabled(jMainWindowFrame2, false);
                            jSplitIntoFrame.pack();
                            jSplitIntoFrame.setLocation((maxWidth-jSplitIntoFrame.getWidth())/2, (maxHeight-jSplitIntoFrame.getHeight())/2);
                            jSplitIntoFrame.setVisible(true);
                        }
                        //if the column is not aggregated
                        else {
                            //if the column is base in a mergeset
                            if (columnSchema.isColumnMergedInOther(columnNumber) && columnSchema.queryMergeOrder(columnNumber) == 1) {
                                Object[] options = {"Yes", "No"};
                                int n = JOptionPane.showOptionDialog(jMainWindowFrame2, "The column is the base column in a merging. Cancel the merging?", "Problem!", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
                                int mergeset = columnSchema.queryMergeSet(columnNumber);
                                if (n == 0) {
                                    HashMap<Integer, Integer> allMergesetMembers = columnSchema.getAllMergesetMembers(mergeset);
                                    for (Integer i : allMergesetMembers.values()) {
                                        columnSchMgr.pushColumnSchema();
                                        if (i != allMergesetMembers.get(1))
                                            comboBoxes.get(i).setSelectedIndex(9);
                                        columnSchMgr.popColumnSchema();
                                    }
                                    columnSchema.deleteMergeset(mergeset);
                                    columnSchMgr.update();
                                } else {
                                    combo.setSelectedIndex(comboMgr.getIndexOfValue(columnSchema.queryMergesetCandidateType(mergeset)));
                                    return;
                                }
                            }
                            //continue
                            columnSchMgr.pushColumnSchema();
                            prepareSplitIntoWindow(columnNumber);
                            columnSchMgr.update();
                            comboMgr.updateComboBoxesEnabledValues(comboBoxes);
                            comboMgr.updateAddToNumbers();
                            setButtonsEnabled(jMainWindowFrame2, false);
                            jSplitIntoFrame.pack();
                            jSplitIntoFrame.setLocation((maxWidth-jSplitIntoFrame.getWidth())/2, (maxHeight-jSplitIntoFrame.getHeight())/2);
                            jSplitIntoFrame.setVisible(true);
                        }
                    }
                }
                //if columnNumber is <0, then it was SPLIT INTO combo box changed
                else if (columnNumber < 0){
                    columnSchMgr.update();
                    comboMgr.updateComboBoxesEnabledValues(splitIntoColumnsTypes);
                }
                else {
                    combo.setSelectedItem(currentItem);
                }
                currentItem = combo.getSelectedItem();
            }
        }
    }

    private class ComboItem implements CanEnable {

        Object value;
        boolean isEnable;

        ComboItem(Object value, boolean isEnable) {
            this.value = value;
            this.isEnable = isEnable;
        }

        ComboItem(Object value) {
            this(value, true);
        }

        public boolean isEnabled() {
            return isEnable;
        }

        public void setEnabled(boolean isEnable) {
            this.isEnable = isEnable;
        }

        public VCFTypesEnum toVCFType(){
            String val = (String)value;
            if (val.equals("ADD TO COLUMN #...") || val.equals("SPLIT INTO..."))
                return null;
            if (val.startsWith("Organization"))
                return VCFTypesEnum.Organization_Name_or_Organizational_unit;
            return VCFTypesEnum.valueOf(((String)value).replace(" ", "_"));
        }

        @Override
        public String toString() {
            return (String)value;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ComboItem other = (ComboItem) obj;
            if (this.value != other.value && (this.value == null || !this.value.equals(other.value))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return (this.value != null ? this.value.hashCode() : 0);
        }

    }
    // </editor-fold>

    private class ColumnSchemaManager {

        Stack<InternalDocColumnSchema> columnSchemaStack;

        ColumnSchemaManager(){
            columnSchemaStack = new Stack<InternalDocColumnSchema>();
        }

        public void popColumnSchema(){
            columnSchema = columnSchemaStack.pop();
            System.err.println("Old column schema restored");
        }
        
        public void discardPopColumnSchema(){
            columnSchemaStack.pop();
            System.err.println("Old column schema discarded");
        }

        public void update(){
            if (!updateEnabled)
                return;

            compiled = false;
            System.err.println("Column schema updated");
            //find new mergeset number
            int newMergesetNum = 0;
            ArrayList<Integer> allMergesetNum = columnSchema.getAllMergesets();
            if (allMergesetNum != null ){
                boolean success = false;
                do{
                    newMergesetNum++;
                    success = true;
                    for (Integer mergeset : allMergesetNum){
                        if (mergeset == newMergesetNum){
                            success = false;
                        }
                    }
                }while(!success);
            }

            //save old CS
            InternalDocColumnSchema oldCs = columnSchema;
            //set CS to default
            int columnCount = oldCs.getColumnCount();
            InternalDocColumnSchema cs = new InternalDocColumnSchemaImpl(columnCount);

            for(JComboBox combo : comboBoxes){
                int column = comboBoxes.indexOf(combo);

                //if a type is selected
                if (combo.getSelectedIndex() <= comboMgr.getIndexOfValue("DELETE_THIS")){
                    String type = comboMgr.getSelectedValueStr(combo);

                    //if merged in oldCS and first in that mergeset
                    if (oldCs.isColumnMergedInOther(column) && oldCs.queryMergeOrder(column) == 1){
                        int oldMergesetNum = oldCs.queryMergeSet(column);
                        cs.createMergeset(oldMergesetNum,oldCs.queryMergesetDelimiter(oldMergesetNum));
                        cs.columnMergeOn(column, oldMergesetNum, 1);
                        cs.setMergesetCandidateType(oldMergesetNum, type);
                        cs.setMergesetSelectedType(oldMergesetNum, type);
                    }
                    //if 1st in newly created mergeset -> values in addto window
                    else if (!jAddToBaseColumnTextField.getText().equals("") && Integer.parseInt(jAddToBaseColumnTextField.getText()) == column){
                        cs.createMergeset(newMergesetNum,jAddToDelimeterTextField.getText());
                        cs.columnMergeOn(column, newMergesetNum, 1);
                        cs.setMergesetCandidateType(newMergesetNum, type);
                        cs.setMergesetSelectedType(newMergesetNum, type);
                    }
                    //if it is just a column with type
                    else {
                        cs.setSelectedtypeType(column, type);
                        cs.setCandidateType(column, type);
                    }
                }

                //if add to is selected
                else if (combo.getSelectedIndex() == comboMgr.getIndexOfValue("ADD_TO_COLUMN_#...")){
                    ArrayList<Integer> newToMergeList = new ArrayList<Integer>();
                    for (int i = 0; i < columnsToAdd.size(); i++) {
                        String component = columnsToAdd.get(i).getText();
                        if (component.equals("")) {
                            continue;
                        }
                        newToMergeList.add(Integer.parseInt(component));
                    }
                    //if is in newly created mergeset
                    if (newToMergeList.contains(column)){
                        int baseColumn = Integer.parseInt(jAddToBaseColumnTextField.getText());
                        if (oldCs.isColumnMergedInOther(baseColumn)){
                            cs.columnMergeOn(column, oldCs.queryMergeSet(baseColumn), newToMergeList.indexOf(column)+2);
                        }
                        cs.columnMergeOn(column, newMergesetNum, newToMergeList.indexOf(column)+2);
                    }
                    //if merged in old CS
                    else if(oldCs.isColumnMergedInOther(column)){
                        cs.columnMergeOn(column, oldCs.queryMergeSet(column), oldCs.queryMergeOrder(column));
                    }
                }

                //if SPLIT INTO is selected
                else if (combo.getSelectedIndex() == comboMgr.getIndexOfValue("SPLIT_INTO...")){
                    //if is aggregated in old CS
                    if (oldCs.isColumnAggregated(column)){
                        //if column is in split window
                        if (Integer.parseInt(jColumnToSplitLabel.getText()) == column){
                            int numberOfColumns = Integer.parseInt(jSplitIntoNumberOfColumnsTextField.getText());
                            cs.columnAggregateOn(
                                column,
                                jSplitIntoDelimeterTextField.getText(),
                                numberOfColumns,
                                jSplitIntoContactsCheckBox.isSelected(),
                                jSplitIntoContactsAreEmployeesCheckBox.isSelected(),
                                jSplitIntoContactsOrigIntoSourceCheckBox.isSelected(),
                                jSplitIntoContactsOrigIntoContactsCheckBox.isSelected(),
                                jSplitIntoContactsDelimeterOfContactsTextField.getText(),
                                jSplitIntoContactsSwapsCheckBox.isSelected());
                            for (int i=0; i<splitIntoColumnsTypes.size(); i++){
                                cs.setAggregatedCandidateType(column, i, comboMgr.getSelectedValueStr(splitIntoColumnsTypes.get(i)));
                                cs.setAggregatedSelectedtypeType(column, i, comboMgr.getSelectedValueStr(splitIntoColumnsTypes.get(i)));
                            }
                            for (int i=splitIntoColumnsTypes.size(); i<numberOfColumns; i++){
                                cs.setAggregatedCandidateType(column, i, VCFTypesEnum.Note.toString());
                                cs.setAggregatedSelectedtypeType(column, i, VCFTypesEnum.Note.toString());
                            }
                        }
                        //else it is and old aggregation -> copy values
                        else{
                            cs.columnAggregateOn(
                                    column,
                                    oldCs.queryAggregateSettingDelimiter(column),
                                    oldCs.queryAggregateSettingNumberofcolumns(column),
                                    oldCs.queryAggregateSettingIntoseparatecontacts(column),
                                    oldCs.queryAggregateSettingEmployees(column),
                                    oldCs.queryAggregateSettingOriginalsourcenote(column),
                                    oldCs.queryAggregateSettingOriginaltargetnote(column),
                                    oldCs.queryAggregateSettingSeparatecontactsdelimiter(column),
                                    oldCs.queryAggregateSettingAutodetectswaps(column));
                            for (int i = 0; i < oldCs.queryAggregateSettingNumberofcolumns(column); i++) {
                                String type = oldCs.queryAggregatedSelectedtypeType(column, i);
                                cs.setAggregatedCandidateType(column, i, type);
                                cs.setAggregatedSelectedtypeType(column, i, type);
                            }
                        }
                    }
                    //else column is newly aggregated but has no subcolumns (it has only been chosen to be splitted)
                    else {
                        cs.columnAggregateOn(
                                column,
                                "",
                                0,
                                false,
                                false,
                                false,
                                false,
                                "",
                                false);
                    }
                }
            }
            columnSchema = cs;
        }

        public void pushColumnSchema() {
            columnSchemaStack.push(columnSchema.returnClonedColumnSchema());
        }
    }

    private class ComboBoxesManager {

        public ComboBoxesManager() {
        }

        public void createMainComboBoxes() {
            ArrayList<Object[]> itemsList = new ArrayList<Object[]>();
            comboBoxes.clear();
            for (int i = 0; i < jContactsListTable.getColumnCount(); i++) {
                // <editor-fold defaultstate="collapsed" desc="fill in the values">
                itemsList.add(new Object[]{
                            new ComboItem(VCFTypesEnum.Formatted_Name.toDisplayString()), //0
                            new ComboItem(VCFTypesEnum.Name.toDisplayString()), //1
                            new ComboItem(VCFTypesEnum.Telephone.toDisplayString()), //2
                            new ComboItem(VCFTypesEnum.Telephone_home.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Telephone_work.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Telephone_work_fax.toDisplayString()), //5
                            new ComboItem(VCFTypesEnum.Telephone_work_video.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Email.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Email_work.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Note.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Photograph.toDisplayString()), //10
                            new ComboItem(VCFTypesEnum.Sound.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Delivery_Address.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Delivery_Address_home.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Delivery_Address_work.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Label_Address.toDisplayString()), //15
                            new ComboItem(VCFTypesEnum.Label_Address_home.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Label_Address_work.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Birthday.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Nickname.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Organization_Name_or_Organizational_unit.toDisplayString()), //20
                            new ComboItem(VCFTypesEnum.Role_or_occupation.toDisplayString()),
                            new ComboItem(VCFTypesEnum.Logo.toDisplayString()),
                            new ComboItem(VCFTypesEnum.URL.toDisplayString()),
                            new ComboItem(VCFTypesEnum.URL_home.toDisplayString()),
                            new ComboItem(VCFTypesEnum.URL_work.toDisplayString()), //25
                            new ComboItem(VCFTypesEnum.Unique_Identifier.toDisplayString()), //26
                            new ComboItem("DELETE_THIS"),
                            new ComboItem("ADD TO COLUMN #..."),
                            new ComboItem("SPLIT INTO...")
                        });
                //</editor-fold>
                comboBoxes.add(new JComboBox(itemsList.get(i)));
                comboBoxes.get(i).setRenderer(new ComboRenderer());
                comboBoxes.get(i).addActionListener(new ComboListener(comboBoxes.get(i)));

                //if column is aggregated, set its combo box to agregated
                updateEnabled = false;
                if (columnSchema.isColumnAggregated(i)) {
                    comboBoxes.get(i).setSelectedIndex(comboMgr.getIndexOfValue("SPLIT_INTO..."));
                } //if column is merged
                else if (columnSchema.isColumnMergedInOther(i)) {
                    //if is first in merging, set its combo box to the mergeset type
                    if (columnSchema.queryMergeOrder(i) == 1) {
                        String candidType = columnSchema.queryMergesetCandidateType(columnSchema.queryMergeSet(i));
                        comboBoxes.get(i).setSelectedIndex(comboMgr.getIndexOfValue(candidType));
                    } //if it is not first, set its combo box to merging
                    else {
                        comboBoxes.get(i).setSelectedIndex(comboMgr.getIndexOfValue("ADD_TO_COLUMN_#..."));
                    }
                } //if there is some condidate type set in CS, set its combo box
                else {
                    String type = columnSchema.queryCandidateType(i);
                    int indexToSelect = comboMgr.getIndexOfValue(type);
                    comboBoxes.get(i).setSelectedIndex(indexToSelect);
                }
                jComboBoxesToolBar.add(comboBoxes.get(i));
                comboBoxes.get(i).setVisible(true);
                updateEnabled = true;
            }
            columnSchMgr.update();
            comboMgr.updateComboBoxesEnabledValues(comboBoxes);
            comboMgr.updateAddToNumbers();

        }

        public void updateComboBoxesEnabledValues(ArrayList<JComboBox> comboBoxes) {
            if (!updateEnabled) {
                return;
            }
            System.err.println("Combo boxes enabled values updated.");
            boolean isNameUsed = columnSchema.isTypeInColumnSchema(VCFTypesEnum.Name);
            boolean isBirthdayUsed = columnSchema.isTypeInColumnSchema(VCFTypesEnum.Birthday);
            boolean isUiUsed = columnSchema.isTypeInColumnSchema(VCFTypesEnum.Unique_Identifier);
            for (JComboBox combo : comboBoxes) {
                //unable to delete whole mergeset
                int column = comboMgr.getIndexOfComboBox(combo);
                if(column >=0 && columnSchema.isColumnMergedInOther(column) && columnSchema.queryMergeOrder(column) == 1){
                    comboMgr.setComboItemEnabled(combo, comboMgr.getIndexOfValue("DELETE_THIS"), false);
                } else if (combo.getItemCount() > comboMgr.getIndexOfValue("DELETE_THIS")) {
                    comboMgr.setComboItemEnabled(combo, comboMgr.getIndexOfValue("DELETE_THIS"), true);
                }

                int index = comboMgr.getIndexOfValue(VCFTypesEnum.Name.toString());
                if (isNameUsed) {
                    if (combo.getSelectedIndex() != (index)) {
                        comboMgr.setComboItemEnabled(combo, index, false);
                    }
                } else {
                    comboMgr.setComboItemEnabled(combo, index, true);
                }
                index = comboMgr.getIndexOfValue(VCFTypesEnum.Birthday.toString());
                if (isBirthdayUsed) {
                    if (combo.getSelectedIndex() != (index)) {
                        comboMgr.setComboItemEnabled(combo, index, false);
                    }
                } else {
                    comboMgr.setComboItemEnabled(combo, index, true);
                }
                index = comboMgr.getIndexOfValue(VCFTypesEnum.Unique_Identifier.toString());
                if (isUiUsed) {
                    if (combo.getSelectedIndex() != (index)) {
                        comboMgr.setComboItemEnabled(combo, index, false);
                    }
                } else {
                    comboMgr.setComboItemEnabled(combo, index, true);
                }
            }
        }

        public void updateAddToNumbers() {
            if (!updateEnabled) {
                return;
            }
            System.err.println("Add to lables updated.");
            for (JComboBox combo : comboBoxes) {
                int column = comboMgr.getIndexOfComboBox(combo);
                if (combo.getSelectedIndex() == getIndexOfValue("ADD_TO_COLUMN_#...")) {
                    int mergeset = columnSchema.queryMergeSet(column);
                    HashMap<Integer, Integer> allMergesetMembers = columnSchema.getAllMergesetMembers(mergeset);
                    if (allMergesetMembers != null && allMergesetMembers.get(1) != null) {
                        int baseColumn = allMergesetMembers.get(1);
                        ((ComboItem)combo.getItemAt(combo.getSelectedIndex())).value = "ADD TO COLUMN " + baseColumn;
                        continue;
                    }
                }
                ((ComboItem)combo.getItemAt(comboMgr.getIndexOfValue("ADD_TO_COLUMN_#..."))).value = "ADD TO COLUMN #...";
            }
        }

        public String getSelectedValueStr(int comboBoxIndex) {
            JComboBox temp = getComboBoxAtIndex(comboBoxIndex);
            return getSelectedValueStr(temp);
        }

        public String getSelectedValueStr(JComboBox comboBox) {
            String selectedItem = ((ComboItem) comboBox.getSelectedItem()).toString();
            if (selectedItem.startsWith("Organization"))
                return VCFTypesEnum.Organization_Name_or_Organizational_unit.toString();
            return selectedItem.replace(" ", "_");
        }

        public VCFTypesEnum getSelectedValueVCF(int comboBoxIndex) {
            JComboBox temp = getComboBoxAtIndex(comboBoxIndex);
            return getSelectedValueVCF(temp);
        }

        public VCFTypesEnum getSelectedValueVCF(JComboBox comboBox) {
            return ((ComboItem) comboBox.getSelectedItem()).toVCFType();
        }

        public int getIndexOfComboBox(JComboBox comboBox) {
            return comboBoxes.indexOf(comboBox);
        }

        public JComboBox getComboBoxAtIndex(int index) {
            return comboBoxes.get(index);
        }

        public void setComboItemEnabled(int comboIndex, int itemIndex, boolean value) {
            ((CanEnable) comboBoxes.get(comboIndex).getItemAt(itemIndex)).setEnabled(value);
        }

        public void setComboItemEnabled(JComboBox comboBox, int itemIndex, boolean value) {
            ((CanEnable) comboBox.getItemAt(itemIndex)).setEnabled(value);
        }

        private int getIndexOfValue(String value) {
            // <editor-fold defaultstate="collapsed" desc="if-return field">
            if (value.equals(VCFTypesEnum.Name.toString())) {
                return 1;
            }
            if (value.equals(VCFTypesEnum.Formatted_Name.toString())) {
                return 0;
            }
            if (value.equals(VCFTypesEnum.Nickname.toString())) {
                return 19;
            }
            if (value.equals(VCFTypesEnum.Photograph.toString())) {
                return 10;
            }
            if (value.equals(VCFTypesEnum.Birthday.toString())) {
                return 18;
            }
            if (value.equals(VCFTypesEnum.Delivery_Address.toString())) {
                return 12;
            }
            if (value.equals(VCFTypesEnum.Delivery_Address_work.toString())) {
                return 14;
            }
            if (value.equals(VCFTypesEnum.Delivery_Address_home.toString())) {
                return 13;
            }
            if (value.equals(VCFTypesEnum.Label_Address.toString())) {
                return 15;
            }
            if (value.equals(VCFTypesEnum.Label_Address_work.toString())) {
                return 17;
            }
            if (value.equals(VCFTypesEnum.Label_Address_home.toString())) {
                return 16;
            }
            if (value.equals(VCFTypesEnum.Telephone.toString())) {
                return 2;
            }
            if (value.equals(VCFTypesEnum.Telephone_work.toString())) {
                return 4;
            }
            if (value.equals(VCFTypesEnum.Telephone_work_fax.toString())) {
                return 5;
            }
            if (value.equals(VCFTypesEnum.Telephone_work_video.toString())) {
                return 6;
            }
            if (value.equals(VCFTypesEnum.Telephone_home.toString())) {
                return 3;
            }
            if (value.equals(VCFTypesEnum.Email.toString())) {
                return 7;
            }
            if (value.equals(VCFTypesEnum.Email_work.toString())) {
                return 8;
            }
            if (value.equals(VCFTypesEnum.Role_or_occupation.toString())) {
                return 21;
            }
            if (value.equals(VCFTypesEnum.Logo.toString())) {
                return 22;
            }
            if (value.startsWith("Organization")) {
                return 20;
            }
            if (value.equals(VCFTypesEnum.Note.toString())) {
                return 9;
            }
            if (value.equals(VCFTypesEnum.Sound.toString())) {
                return 11;
            }
            if (value.equals(VCFTypesEnum.URL.toString())) {
                return 23;
            }
            if (value.equals(VCFTypesEnum.URL_work.toString())) {
                return 25;
            }
            if (value.equals(VCFTypesEnum.URL_home.toString())) {
                return 24;
            }
            if (value.equals(VCFTypesEnum.Unique_Identifier.toString())) {
                return 26;
            }
            if (value.equals("DELETE_THIS")) {
                return 27;
            }
            if (value.startsWith("ADD_TO_COLUMN_")) {
                return 28;
            }
            if (value.equals("SPLIT_INTO...")) {
                return 29;
            }
            return 9;
            // </editor-fold>
        }
    }

    private class RefreshSwingWorker extends SwingWorker<List<Object>, Integer>{

        @Override
        protected List<Object> doInBackground() throws Exception {
            List<Object> result = new ArrayList<Object>();
            if (compiled){
                result.add(compiledDoc);
                result.add(internalDoc);
                result.add(columnSchema);
                return result;
            }
            compiler = new InternalDocCompiler(internalDoc, columnSchema,true);
            compiler.compile();
            Document compiledDoc = compiler.getCompiledValidContacts();
            InputFilter readCompiledDoc = new ReadCompiledDoc(compiledDoc);
            result.add(compiledDoc);
            result.add(readCompiledDoc.read());
            result.add(readCompiledDoc.getColumnSchema());
            return result;
        }

        @Override
        protected void done() {
            if (!compiled){
                List<Object> result = null;
                try {
                    result = refreshSwingWorker.get();
                } catch (InterruptedException ex) {
                    Logger.getLogger(ContactTransmutGUIMain.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(ContactTransmutGUIMain.class.getName()).log(Level.SEVERE, null, ex);
               }

                if (result == null) {
                    JOptionPane.showMessageDialog(jMainWindowFrame2,"Error while processing data.","Error",JOptionPane.ERROR_MESSAGE);
                   return;
               }
                internalDoc = (Document) result.get(1);
                columnSchema = (InternalDocColumnSchema) result.get(2);
                compiledDoc = (Document) result.get(0);

                jMainWindowShowCompiledDocButton.setEnabled(true);

                //refresh gui
                comboBoxes.clear();
                columnsToAdd.clear();
                splitIntoColumnsTypes.clear();
                jComboBoxesToolBar.removeAll();
                jColumnToSplitLabel.setText("-1");
                jColumnToAddLabel.setText("-1");
                jAddToBaseColumnTextField.setText("-1");

                tableModel.initTable(internalDoc, columnSchema, jContactsListTable);

                comboMgr.createMainComboBoxes();

                jRefreshProgressBar2.setVisible(false);
                jMainWindowStopButton2.setVisible(false);
                jMainWindowRefreshButton2.setVisible(true);
                jMainWindowFrame2.repaint();
                jMainWindowFrame2.setVisible(true);

                updateTableWidths();
                
                compiled = true;
            }

            if (saving) {
                saving = false;
                //choose where to save and the filetype
                String savePath = "";
                JFileChooser chooser = new JFileChooser();
                // <editor-fold defaultstate="collapsed" desc="filter typov suborov">
                FileFilter filterVCF = new FileFilter() {

                    @Override
                    public boolean accept(File f) {
                        try {
                            if ((f.isFile() && f.toString().toLowerCase().endsWith("vcf")) || f.isDirectory()) {
                                return true;
                            } else {
                                return false;
                            }
                        } catch (Exception e) {
                            //no idea why it comes out but everything works just OK
                            return true;
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "VCF files";
                    }
                };
                FileFilter filterODS = new FileFilter() {

                    @Override
                    public boolean accept(File f) {
                        try {
                            if ((f.isFile() && f.toString().toLowerCase().endsWith("ods")) || f.isDirectory()) {
                                return true;
                            } else {
                                return false;
                            }
                        } catch (Exception e) {
                            //no idea why it comes out but everything works just OK
                            return true;
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "ODS files";
                    }
                };
                FileFilter filterCSV = new FileFilter() {

                    @Override
                    public boolean accept(File f) {
                        try {
                            if ((f.isFile() && f.toString().toLowerCase().endsWith("csv")) || f.isDirectory()) {
                                return true;
                            } else {
                                return false;
                            }
                        } catch (Exception e) {
                            //no idea why it comes out but everything works just OK
                            return true;
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "CSV files";
                    }
                };
                FileFilter filterADR = new FileFilter() {

                    @Override
                    public boolean accept(File f) {
                        try {
                            if ((f.isFile() && f.toString().toLowerCase().endsWith("adr")) || f.isDirectory()) {
                                return true;
                            } else {
                                return false;
                            }
                        } catch (Exception e) {
                            //no idea why it comes out but everything works just OK
                            return true;
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "ADR files";
                    }
                };
                // </editor-fold>
                chooser.addChoosableFileFilter(filterCSV);
                //chooser.addChoosableFileFilter(filterADR);
                //chooser.addChoosableFileFilter(filterODS);
                chooser.addChoosableFileFilter(filterVCF);
                chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
                chooser.setDialogTitle("Save as...");
                chooser.setApproveButtonText("Save");
                chooser.setAlignmentX(100);
                chooser.setAlignmentY(100);
                int returnVal = chooser.showOpenDialog(jMainWindowPanel);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    savePath = (chooser.getSelectedFile().toString());
                    if (!chooser.getFileFilter().equals(chooser.getAcceptAllFileFilter())) {
                        String fileType = chooser.getFileFilter().getDescription();
                        int index = fileType.indexOf(" ");
                        fileType = fileType.substring(0, index);
                        if (!savePath.toLowerCase().endsWith(fileType.toLowerCase())) {
                            savePath = savePath + "." + fileType.toLowerCase();
                        }
                    }
                } else {
                    setButtonsEnabled(jMainWindowFrame2, true);
                    jMainWindowFrame2.repaint();
                    jMainWindowFrame2.setVisible(true);

                    tableModel.setCellsAreEditable(true);

                    updateTableWidths();
                    return;
                }

                //TODO: add output encoding options
                String encoding = jEncodingComboBox1.getSelectedItem().toString();
                String output = "";
                OutputFilter outputFilter = null;
                if (savePath.toLowerCase().endsWith(".csv")) {
                    output = "CSV";
                } else if (savePath.toLowerCase().endsWith(".vcf")) {
                    output = "VCF";
                } else if (savePath.toLowerCase().endsWith(".ods")) {
                    JOptionPane.showMessageDialog(jMainWindowFrame2, "File type not supported yet.", "Sorry!", JOptionPane.INFORMATION_MESSAGE);
                    saving = true;
                    done();
                    return;
                } else if (savePath.toLowerCase().endsWith(".adr")) {
                    JOptionPane.showMessageDialog(jMainWindowFrame2, "File type not supported yet.", "Sorry!", JOptionPane.INFORMATION_MESSAGE);
                    saving = true;
                    done();
                    return;
                } else {
                    JOptionPane.showMessageDialog(jMainWindowFrame2, "Choose a valid file type. (*.csv, *.ods, *.vcf, *.adr)", "Invalid file path!", JOptionPane.ERROR_MESSAGE);
                    saving = true;
                    done();
                    return;
                }
                if (output.equals("CSV")) {
                    outputFilter = new WriteCSV(savePath, encoding, ",", "\"", compiledDoc);
                } else if (output.equals("VCF")) {
                    outputFilter = new WriteVCF(savePath, encoding, compiledDoc);
                }

                outputFilter.write();
                Object[] options = {"Continue editing...", "Close program"};
                int n = JOptionPane.showOptionDialog(jMainWindowFrame2, "Saved to " + savePath, "Continue?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
                if (n != 0) {
                    System.exit(0);
                }
            }

            

            setButtonsEnabled(jMainWindowFrame2, true);
            tableModel.setCellsAreEditable(true);

            jMainWindowFrame2.repaint();
            jMainWindowFrame2.setVisible(true);

            updateTableWidths();
            compiler = null;
        }

        @Override
        protected void process(List<Integer> chunks) {
            super.process(chunks);
        }


    }

    private class UpdateStatusbarSwingWorker extends SwingWorker<Void, Void>{

        private int divider = 1;

        public UpdateStatusbarSwingWorker(int divider) {
            this.divider = divider;
        }



        @Override
        protected Void doInBackground() throws Exception {
            int counter = 0;
            while (compiler == null && counter<10000){
                Thread.sleep(10);
                counter++;
            }

            if (compiler == null){
                System.err.println("Compiler not initialized");
                return null;
            }

            int maxNumberOfStatus = compiler.getMaxContacts();

            counter = 0;
            while (maxNumberOfStatus == 0 && counter <10000){
                Thread.sleep(10);
                maxNumberOfStatus = compiler.getMaxContacts();
            }

            int progress = 0;

            while (progress < maxNumberOfStatus){
                progress = compiler.getCurrentStatus();
                setProgress(progress/divider);
                Thread.sleep(10);
            }
            return null;
        }
    }





    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField jAddToBaseColumnTextField;
    private javax.swing.JButton jAddToCancelButton;
    private javax.swing.JTextField jAddToDelimeterTextField;
    private javax.swing.JFrame jAddToFrame;
    private javax.swing.JButton jAddToOkButton;
    private javax.swing.JButton jAddToPlusButton;
    private javax.swing.JScrollPane jAddToScrollPane;
    private javax.swing.JButton jAddToSubmitButton;
    private javax.swing.JToolBar jAddToToolBar;
    private javax.swing.JButton jBackButton1;
    private javax.swing.JButton jBrowseButton1;
    private javax.swing.JButton jCancelButton1;
    private javax.swing.JScrollPane jColumnSchemaScrollPane;
    private javax.swing.JTextArea jColumnSchemaTextArea;
    private javax.swing.JFrame jColumnSchemaTextFrame;
    private javax.swing.JLabel jColumnToAddLabel;
    private javax.swing.JLabel jColumnToSplitLabel;
    private javax.swing.JLabel jColumnToSplitLabel1;
    private javax.swing.JLabel jColumnToSplitLabel2;
    private javax.swing.JScrollPane jComboBoxesScrollPane;
    private javax.swing.JToolBar jComboBoxesToolBar;
    private javax.swing.JScrollPane jCompiledDocScrollPane;
    private javax.swing.JTextArea jCompiledDocTextArea;
    private javax.swing.JFrame jCompiledDocTextFrame;
    private javax.swing.JScrollPane jContactsListScrollPane;
    private javax.swing.JTable jContactsListTable;
    private javax.swing.JComboBox jEncodingComboBox1;
    private javax.swing.JLabel jEncodingLabel1;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JFrame jFileChooserFrame;
    private javax.swing.JLabel jFileChooserLabel;
    private javax.swing.JTextField jInputFileTextField1;
    private javax.swing.JScrollPane jInternalDocScrollPane;
    private javax.swing.JTextArea jInternalDocTextArea;
    private javax.swing.JFrame jInternalDocTextFrame;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLayeredPane jLayeredPane2;
    private javax.swing.JLabel jMainLabel1;
    private javax.swing.JButton jMainWindowAddColumnButton2;
    private javax.swing.JButton jMainWindowBackButton;
    private javax.swing.JButton jMainWindowCancelButton;
    private javax.swing.JFrame jMainWindowFrame2;
    private javax.swing.JButton jMainWindowNextButton;
    private javax.swing.JPanel jMainWindowPanel;
    private javax.swing.JButton jMainWindowRefreshButton2;
    private javax.swing.JButton jMainWindowShowColumnSchemaButton;
    private javax.swing.JButton jMainWindowShowCompiledDocButton;
    private javax.swing.JButton jMainWindowShowInternalDocButton;
    private javax.swing.JButton jMainWindowStopButton2;
    private javax.swing.JButton jNextButton1;
    private javax.swing.JScrollPane jOriginalFileScrollPane;
    private javax.swing.JTextArea jOriginalFileTextArea;
    private javax.swing.JFrame jOriginalFileTextFrame;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JProgressBar jRefreshProgressBar2;
    private javax.swing.JLabel jSelectInputLabel1;
    private javax.swing.JButton jSplitIntoCancelButton;
    private javax.swing.JCheckBox jSplitIntoContactsAreEmployeesCheckBox;
    private javax.swing.JButton jSplitIntoContactsCancelButton;
    private javax.swing.JCheckBox jSplitIntoContactsCheckBox;
    private javax.swing.JButton jSplitIntoContactsColumnTypesSettingsButton;
    private javax.swing.JTextField jSplitIntoContactsDelimeterOfColumnsTextField;
    private javax.swing.JTextField jSplitIntoContactsDelimeterOfContactsTextField;
    private javax.swing.JFrame jSplitIntoContactsFrame;
    private javax.swing.JTextField jSplitIntoContactsNumberOfColumnsTextField;
    private javax.swing.JButton jSplitIntoContactsOkButton;
    private javax.swing.JCheckBox jSplitIntoContactsOrigIntoContactsCheckBox;
    private javax.swing.JCheckBox jSplitIntoContactsOrigIntoSourceCheckBox;
    private javax.swing.JButton jSplitIntoContactsSettingsButton;
    private javax.swing.JCheckBox jSplitIntoContactsSwapsCheckBox;
    private javax.swing.JButton jSplitIntoContactsSwapsSettingsButton;
    private javax.swing.JTextField jSplitIntoDelimeterTextField;
    private javax.swing.JFrame jSplitIntoFrame;
    private javax.swing.JButton jSplitIntoNumberOfColumnsSettingsButton;
    private javax.swing.JTextField jSplitIntoNumberOfColumnsTextField;
    private javax.swing.JButton jSplitIntoOkButton;
    private javax.swing.JButton jSplitIntoTypeSettingsCancelButton;
    private javax.swing.JFrame jSplitIntoTypeSettingsFrame;
    private javax.swing.JLabel jSplitIntoTypeSettingsNumberOfColumnsLabel;
    private javax.swing.JButton jSplitIntoTypeSettingsOkButton;
    private javax.swing.JScrollPane jSplitIntoTypeSettingsScrollPane;
    private javax.swing.JToolBar jSplitIntoTypeSettingsToolBar;
    // End of variables declaration//GEN-END:variables

}
