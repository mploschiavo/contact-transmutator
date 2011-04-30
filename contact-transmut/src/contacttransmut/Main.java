/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package contacttransmut;

import java.io.IOException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;

/**
 *
 * @author ovečka
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, TransformerConfigurationException, TransformerException, XPathExpressionException {

        { //this is first basic test - load CSV into InternalDoc
            //This very simple code loads CSV into DOM tree and prints out the DOM tree along with some other information
            String fileName = "csv.csv";
            String encoding = "UTF-8";
            InputFilter test = new ReadCSV(fileName, encoding, ",", "\"");
            Document loadedContacts = test.read();

            //print out result
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = null;
            trans = tf.newTransformer();
            trans.transform(new DOMSource(loadedContacts), new StreamResult(System.out));
        }


    }
}
