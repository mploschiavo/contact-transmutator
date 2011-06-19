/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gui;

import contacttransmut.InternalDocColumnSchema;
import java.util.ArrayList;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author Martin
 */
public class ContactsListTableModel extends AbstractTableModel{

    private static Document internalDoc;
    private static InternalDocColumnSchema columnSchema;
    private int rowCount = 0;
    private int columnCount = 0;
    private ArrayList<ArrayList<String>> data;
    private JTable table;
    private ArrayList<ArrayList<String>> dataCopied;
    int[] selectedColumnsToCopy;
    int[] selectedRowsToCopy;

    public void initTable(Document internalD, InternalDocColumnSchema columnSch, JTable table){
        internalDoc = internalD;
        columnSchema = columnSch;
        this.table = table;

        NodeList contactList = internalDoc.getElementsByTagName("contact");

        rowCount = contactList.getLength();
        columnCount = columnSchema.getColumnCount();

        fireTableStructureChanged();

        dataCopied = new ArrayList<ArrayList<String>>();

        data = new ArrayList<ArrayList<String>>();
        data.clear();
        for (int i = 0; i < contactList.getLength(); i++) {
            Element contact = (Element) contactList.item(i);
            data.add(new ArrayList<String>());
            ArrayList<String> currentList = data.get(i);
            for (int z=0; z<columnCount; z++){
                currentList.add("");
            }
            NodeList uncategorizedList = contact.getElementsByTagName("uncategorized");
            for (int j = 0; j < uncategorizedList.getLength(); j++) {
                Element uncategorized = (Element) uncategorizedList.item(j);
                NodeList dataList = uncategorized.getElementsByTagName("data");
                for (int k = 0; k < dataList.getLength(); k++) {
                    Element eData = (Element) dataList.item(k);
                    currentList.set(Integer.parseInt(eData.getAttribute("counter")), eData.getTextContent());
                }
            }
        }

        fireTableDataChanged();
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= rowCount || columnIndex>= columnCount){
            throw new IndexOutOfBoundsException("Index out of bounds. Max is "
                    + String.valueOf(rowCount-1) + ", " + String.valueOf(columnCount-1)
                    + ". Input values: "
                    + String.valueOf(rowIndex) + ", " + String.valueOf(columnIndex));
        }
        return data.get(rowIndex).get(columnIndex);
    }

    @Override
    public String getColumnName(int columnIndex) {
        return String.valueOf(columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        data.get(rowIndex).set(columnIndex, (String) aValue);
        fireTableCellUpdated(rowIndex, columnIndex);
        NodeList allDataElements = internalDoc.getElementsByTagName("data");
        int dataToChange = columnCount*rowIndex + columnIndex;
        Element dataElement = (Element) allDataElements.item(dataToChange);
        dataElement.setTextContent((String) aValue);
    }

    void copyValues() {
        selectedColumnsToCopy = table.getSelectedColumns();
        selectedRowsToCopy = table.getSelectedRows();
        dataCopied.clear();
        for (int row = 0; row<selectedRowsToCopy.length; row++){
            dataCopied.add(new ArrayList<String>());
            for (int column = 0; column<selectedColumnsToCopy.length; column++){
                int rowIndex = selectedRowsToCopy[row];
                int columnIndex = selectedColumnsToCopy[column];
                dataCopied.get(row).add(data.get(rowIndex).get(columnIndex));
            }
        }
    }

    void pasteValues() {
        int[] selectedColumnsToPaste = table.getSelectedColumns();
        int[] selectedRowsToPaste = table.getSelectedRows();

        int topMostRowSelectedToPaste = getRowCount();
        for (Integer row : selectedRowsToPaste){
            if (row<topMostRowSelectedToPaste)
                topMostRowSelectedToPaste = row;
        }

        int topMostColumnSelectedToPaste = getColumnCount();
        for (Integer column : selectedColumnsToPaste){
            if (column<topMostColumnSelectedToPaste)
                topMostColumnSelectedToPaste = column;
        }

        int topMostRowSelectedToCopy = getRowCount();
        for (Integer row : selectedRowsToCopy){
            if (row<topMostRowSelectedToCopy)
                topMostRowSelectedToCopy = row;
        }

        int topMostColumnSelectedToCopy = getColumnCount();
        for (Integer column : selectedColumnsToCopy){
            if (column<topMostColumnSelectedToCopy)
                topMostColumnSelectedToCopy = column;
        }

        int rowDifferenceIndex = topMostRowSelectedToPaste - topMostRowSelectedToCopy;
        int columnDifferenceIndex = topMostColumnSelectedToPaste - topMostColumnSelectedToCopy;

        for (int row = 0; row<dataCopied.size(); row++){
            for (int column = 0; column<dataCopied.get(row).size(); column++){
                int targetRow = selectedRowsToCopy[row]+rowDifferenceIndex;
                int targetColumn = selectedColumnsToCopy[column]+columnDifferenceIndex;
                if (targetRow < getRowCount() && targetColumn < getColumnCount())
                    setValueAt(dataCopied.get(row).get(column),targetRow,targetColumn);
            }
        }
    }

    void deleteValues() {
        int[] selectedColumnsToDelete = table.getSelectedColumns();
        int[] selectedRowsToDelete = table.getSelectedRows();
        for(Integer row : selectedRowsToDelete){
            for (Integer column : selectedColumnsToDelete){
                setValueAt("", row, column);
            }
        }
    }

}
