/*
* Dateiname: OneInsertionLineView.java
* Projekt  : WollMux
* Funktion : Eine einzelige Sicht auf eine Einf�gestelle im Dokument.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 13.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former.insertion;

import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.view.LineView;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

public class OneInsertionLineView extends LineView
{
  
  /**
   * Das Model zu dieser View.
   */
  private InsertionModel model;
  
  /**
   * Typischerweise ein Container, der die View enth�lt und daher �ber �nderungen
   * auf dem Laufenden gehalten werden muss.
   */
  private ViewChangeListener bigDaddy;
  
  /**
   * Wird auf alle Teilkomponenten dieser View registriert.
   */
  private MyMouseListener myMouseListener = new MyMouseListener();
  
  /**
   * Wird vor dem �ndern eines Attributs des Models gesetzt, damit der rekursive Aufruf
   * des ChangeListeners nicht unn�tigerweise das Feld updatet, das wir selbst gerade gesetzt
   * haben.
   */
  private boolean ignoreAttributeChanged = false;
  
  /**
   * Die ComboBox, die DB_SPALTE bzw, ID anzeigt und �ndern l�sst.
   */
  private JComboBox idBox;

  
  /**
   * Erzeugt eine neue View f�r model.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public OneInsertionLineView(InsertionModel model, ViewChangeListener bigDaddy, FormularMax4000 formularMax4000)
  {
    this.model = model;
    this.bigDaddy = bigDaddy;
    this.formularMax4000 = formularMax4000;
    myPanel = new JPanel();
    myPanel.setOpaque(true);
    myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.X_AXIS));
    myPanel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
    myPanel.addMouseListener(myMouseListener);
    myPanel.add(makeIDView());
    unmarkedBackgroundColor = myPanel.getBackground();
    model.addListener(new MyModelChangeListener());
    myPanel.validate();
      //Notwendig, um BoxLayout beim Aufziehen daran zu hindern, die Textfelder h�sslich
      //hoch zu machen.
    myPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, myPanel.getPreferredSize().height));
  }
  
  /**
  * Liefert eine Komponente, die je nach Art der Einf�gung das DB_SPALTE oder ID
  * Attribut anzeigt und �nderungen an das Model weitergibt. 
  * @author Matthias Benkmann (D-III-ITD 5.1)
  * TESTED */
  private JComponent makeIDView()
  {
    idBox = new JComboBox();
    idBox.setEditable(true);
    idBox.setSelectedItem(model.getDataID());
    JTextComponent tc = ((JTextComponent)idBox.getEditor().getEditorComponent());
    final Document comboDoc = tc.getDocument();
    comboDoc.addDocumentListener(new DocumentListener(){
      public void update()
      {
        ignoreAttributeChanged = true;
        try{
          model.setDataID(comboDoc.getText(0, comboDoc.getLength()));
        }catch(BadLocationException x){Logger.error(x);}
        ignoreAttributeChanged = false;
      }
      
      public void insertUpdate(DocumentEvent e) {update();}
      public void removeUpdate(DocumentEvent e) {update();}
      public void changedUpdate(DocumentEvent e) {update();}
    });
    
    idBox.addMouseListener(myMouseListener);
    return idBox;
  }
  
  private void idChangedDueToExternalReasons(String newId)
  {
    idBox.setSelectedItem(newId);
  }
  
  private class MyModelChangeListener implements InsertionModel.ModelChangeListener
  {
    public void modelRemoved(InsertionModel model)
    {
      bigDaddy.viewShouldBeRemoved(OneInsertionLineView.this);
    }

    public void attributeChanged(InsertionModel model, int attributeId, Object newValue)
    {
      if (ignoreAttributeChanged) return;
      switch(attributeId)
      {
        case InsertionModel.ID_ATTR: idChangedDueToExternalReasons((String)newValue); break;
      }
    }
  }
  
  /**
   * Wird auf alle Teilkomponenten der View registriert. Setzt MousePressed-Events um in
   * Broadcasts, die signalisieren, dass das entsprechende Model selektiert wurde. Je nachdem
   * ob CTRL gedr�ckt ist oder nicht wird die Selektion erweitert oder ersetzt. 
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyMouseListener implements MouseListener
  {
    public void mouseClicked(MouseEvent e){}
    public void mousePressed(MouseEvent e)
    {
      int state = 1;
      if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK)
        state = 0;
      //formularMax4000.broadcast(new BroadcastFormControlModelSelection(getModel(), state, state!=0));
    }
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
  }
}
