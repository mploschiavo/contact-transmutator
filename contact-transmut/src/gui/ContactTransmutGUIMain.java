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

/**
 *
 * @author Martin
 */
public class ContactTransmutGUIMain extends javax.swing.JFrame {

    /** Creates new form ContactTransmutGUIMain */
    public ContactTransmutGUIMain() {
        initComponents();
        setContentPane(jPanel1);
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
        jFrame1 = new javax.swing.JFrame();
        jPanel2 = new javax.swing.JPanel();
        jMainLabel2 = new javax.swing.JLabel();
        jNextButton2 = new javax.swing.JButton();
        jCancelButton2 = new javax.swing.JButton();
        jBackButton2 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jMainLabel1 = new javax.swing.JLabel();
        jBrowseButton1 = new javax.swing.JButton();
        jInputFileTextField1 = new javax.swing.JTextField();
        jSelectInputLabel1 = new javax.swing.JLabel();
        jNextButton1 = new javax.swing.JButton();
        jCancelButton1 = new javax.swing.JButton();
        jBackButton1 = new javax.swing.JButton();

        jFileChooser1.setCurrentDirectory(new java.io.File("C:\\"));

            jFrame1.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
            jFrame1.setTitle("Contact Transmutator 1.0");

            jMainLabel2.setFont(new java.awt.Font("Chiller", 1, 48)); // NOI18N
            jMainLabel2.setText("Contact Transmutator 1.0");

            jNextButton2.setText("Next >");
            jNextButton2.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    jNextButton2MouseReleased(evt);
                }
            });

            jCancelButton2.setText("Cancel");

            jBackButton2.setText("< Back");
            jBackButton2.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    jBackButton2MouseReleased(evt);
                }
            });

            javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
            jPanel2.setLayout(jPanel2Layout);
            jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addGap(80, 80, 80)
                    .addComponent(jMainLabel2)
                    .addContainerGap(70, Short.MAX_VALUE))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                    .addContainerGap(364, Short.MAX_VALUE)
                    .addComponent(jBackButton2)
                    .addGap(4, 4, 4)
                    .addComponent(jNextButton2)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jCancelButton2)
                    .addGap(19, 19, 19))
            );
            jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jMainLabel2)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 257, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jBackButton2)
                            .addComponent(jNextButton2))
                        .addComponent(jCancelButton2))
                    .addContainerGap())
            );

            javax.swing.GroupLayout jFrame1Layout = new javax.swing.GroupLayout(jFrame1.getContentPane());
            jFrame1.getContentPane().setLayout(jFrame1Layout);
            jFrame1Layout.setHorizontalGroup(
                jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jFrame1Layout.createSequentialGroup()
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap())
            );
            jFrame1Layout.setVerticalGroup(
                jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            );

            setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
            setTitle("Contact Transmutator 1.0");

            jMainLabel1.setFont(new java.awt.Font("Chiller", 1, 48)); // NOI18N
            jMainLabel1.setText("Contact Transmutator 1.0");

            jBrowseButton1.setText("Browse...");
            jBrowseButton1.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    jBrowseButton1MouseReleased(evt);
                }
            });

            jSelectInputLabel1.setFont(new java.awt.Font("Arial", 0, 18)); // NOI18N
            jSelectInputLabel1.setText("Please select the input file:");

            jNextButton1.setText("Next >");
            jNextButton1.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    jNextButton1MouseReleased(evt);
                }
            });

            jCancelButton1.setText("Cancel");

            jBackButton1.setText("< Back");
            jBackButton1.setEnabled(false);

            javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
            jPanel1.setLayout(jPanel1Layout);
            jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(138, 168, Short.MAX_VALUE)
                    .addComponent(jInputFileTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jBrowseButton1)
                    .addGap(87, 87, 87))
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(80, 80, 80)
                    .addComponent(jMainLabel1)
                    .addContainerGap(70, Short.MAX_VALUE))
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(100, 100, 100)
                    .addComponent(jSelectInputLabel1)
                    .addContainerGap(276, Short.MAX_VALUE))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                    .addContainerGap(364, Short.MAX_VALUE)
                    .addComponent(jBackButton1)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jNextButton1)
                    .addGap(4, 4, 4)
                    .addComponent(jCancelButton1)
                    .addGap(19, 19, 19))
            );
            jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jMainLabel1)
                    .addGap(30, 30, 30)
                    .addComponent(jSelectInputLabel1)
                    .addGap(18, 18, 18)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jBrowseButton1)
                        .addComponent(jInputFileTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(141, 141, 141)
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
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 335, Short.MAX_VALUE)
            );

            pack();
        }// </editor-fold>//GEN-END:initComponents

    private void jNextButton1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jNextButton1MouseReleased
        jPanel2.setSize(jPanel1.getWidth(), jPanel1.getHeight());
        setContentPane(jPanel2);
    }//GEN-LAST:event_jNextButton1MouseReleased

    private void jBrowseButton1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jBrowseButton1MouseReleased
        jFileChooser1.setVisible(true);
        //jFileChooser1.requestFocus();
    }//GEN-LAST:event_jBrowseButton1MouseReleased

    private void jNextButton2MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jNextButton2MouseReleased
        // TODO add your handling code here:
}//GEN-LAST:event_jNextButton2MouseReleased

    private void jBackButton2MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jBackButton2MouseReleased
        jPanel1.setSize(jPanel2.getWidth(), jPanel2.getHeight());
        setContentPane(jPanel1);
    }//GEN-LAST:event_jBackButton2MouseReleased

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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBackButton1;
    private javax.swing.JButton jBackButton2;
    private javax.swing.JButton jBrowseButton1;
    private javax.swing.JButton jCancelButton1;
    private javax.swing.JButton jCancelButton2;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JTextField jInputFileTextField1;
    private javax.swing.JLabel jMainLabel1;
    private javax.swing.JLabel jMainLabel2;
    private javax.swing.JButton jNextButton1;
    private javax.swing.JButton jNextButton2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel jSelectInputLabel1;
    // End of variables declaration//GEN-END:variables

}