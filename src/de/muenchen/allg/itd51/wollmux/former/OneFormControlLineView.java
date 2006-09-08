/*
* Dateiname: OneFormControlLineView.java
* Projekt  : WollMux
* Funktion : Eine einzeilige Sicht auf ein einzelnes Formularsteuerelement. 
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 29.08.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;



/**
 * Eine einzeilige Sicht auf ein einzelnes Formularsteuerelement.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OneFormControlLineView implements View
{
  /**
   * Farbe f�r den Hintergrund, wenn die View markiert ist.
   */
  private static final Color MARKED_BACKGROUND_COLOR = Color.BLUE;
  
  /**
   * Breite des Randes um die View.
   */
  private static final int BORDER = 4;
  
  /**
   * Standardbreite des Textfelds, das das Label anzeigt.
   */
  private static final int LABEL_COLUMNS = 20;
  
  /**
   * Typischerweise ein Container, der die View enth�lt und daher �ber �nderungen
   * auf dem Laufenden gehalten werden muss.
   */
  private ViewChangeListener bigDaddy;
  
  /**
   * Das Panel, das alle Komponenten dieser View enth�lt.
   */
  private JPanel myPanel;
  
  /**
   * Der FormularMax4000, zu dem diese View geh�rt.
   */
  private FormularMax4000 formularMax4000;
  
  /**
   * Wird vor dem �ndern eines Attributs des Models gesetzt, damit der rekursive Aufruf
   * des ChangeListeners nicht unn�tigerweise das Feld updatet, das wir selbst gerade gesetzt
   * haben.
   */
  private boolean ignoreAttributeChanged = false;
  
  /**
   * Das Model zur View.
   */
  private FormControlModel model;

  /**
   * Das JTextField, das das LABEL anzeigt und �ndern l�sst.
   */
  private JTextField labelTextfield;
  
  /**
   * Wird auf alle Teilkomponenten dieser View registriert.
   */
  private MyMouseListener myMouseListener = new MyMouseListener();

  /**
   * Die Hintergrundfarbe im unmarkierten Zustand.
   */
  private Color unmarkedBackgroundColor;
    
  /**
   * Erzeugt eine View f�r model.
   * @param bigDaddy typischerweise ein Container, der die View enth�lt und daher �ber �nderungen
   *        auf dem Laufenden gehalten werden muss.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public OneFormControlLineView(FormControlModel model, ViewChangeListener bigDaddy, FormularMax4000 formularMax4000)
  {
    this.model = model;
    this.bigDaddy = bigDaddy;
    this.formularMax4000 = formularMax4000;
    myPanel = new JPanel();
    myPanel.setOpaque(true);
    myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.X_AXIS));
    myPanel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
    myPanel.addMouseListener(myMouseListener);
    myPanel.add(makeLabelView());
    unmarkedBackgroundColor = myPanel.getBackground();
    model.addListener(new MyModelChangeListener());
  }
  
  /**
   * Liefert eine Komponente, die das LABEL des FormControlModels anzeigt und �nderungen
   * an das Model weitergibt. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private JComponent makeLabelView()
  {
    labelTextfield = new JTextField(model.getLabel(), LABEL_COLUMNS);
    labelTextfield.getDocument().addDocumentListener(new DocumentListener(){
      public void update()
      {
        ignoreAttributeChanged = true;
        model.setLabel(labelTextfield.getText());
        ignoreAttributeChanged = false;
      }

      public void insertUpdate(DocumentEvent e) {update();}
      public void removeUpdate(DocumentEvent e) {update();}
      public void changedUpdate(DocumentEvent e) {update();}
      });
    
    labelTextfield.addMouseListener(myMouseListener);
    setTypeSpecificTraits(labelTextfield, model.getType());
    return labelTextfield;
  }
  
  /**
   * Setzt optische Aspekte wie Rand von compo abh�ngig von type.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setTypeSpecificTraits(JComponent compo, String type)
  {
    if (type == FormControlModel.TAB_TYPE)
    {
      Font f = compo.getFont();
      f = f.deriveFont(Font.BOLD);
      compo.setFont(f);
      compo.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
    }
    else if (type == FormControlModel.BUTTON_TYPE)
    {
      compo.setBackground(Color.LIGHT_GRAY);
      compo.setBorder(BorderFactory.createRaisedBevelBorder());
    }
  }
  
  /**
   * Wird aufgerufen, wenn das LABEL des durch diese View dargestellten {@link FormControlModel}s
   * durch eine andere Ursache als diese View ge�ndert wurde.
   * @param newLabel das neue Label.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void labelChanged(String newLabel)
  {
    labelTextfield.setText(newLabel);
  }
  
  /**
   * Wird aufgerufen, wenn das TYPE des durch diese View dargestellten {@link FormControlModel}s
   * durch eine andere Ursache als diese View ge�ndert wurde.
   * @param newType das neue Label.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void typeChanged(String newType)
  {
    setTypeSpecificTraits(labelTextfield, newType);
  }
  
  public JComponent JComponent()
  {
    return myPanel;
  }

   /**
   * Liefert das {@link FormControlModel} das zu dieser View geh�rt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormControlModel getModel()
  {
    return model;
  }

  /**
   * Markiert diese View optisch als ausgew�hlt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void mark()
  {
    myPanel.setBackground(MARKED_BACKGROUND_COLOR);
  }
  
  /**
   * Entfernt die optische Markierung als ausgew�hlt von dieser View.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void unmark()
  {
    myPanel.setBackground(unmarkedBackgroundColor);
  }
  
  /**
   * Interface f�r Klassen, die an �nderungen dieser View interessiert sind.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ViewChangeListener
  {
    /**
     * Wird aufgerufen, wenn alle Referenzen auf diese View entfernt werden sollten,
     * weil die view ung�ltig geworden ist (typischerweise weil das zugrundeliegende Model
     * nicht mehr da ist).
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void viewShouldBeRemoved(OneFormControlLineView view);
  }

  private class MyModelChangeListener implements FormControlModel.ModelChangeListener
  {
    public void attributeChanged(FormControlModel model, int attributeId, Object newValue)
    {
      if (ignoreAttributeChanged) return;
      switch(attributeId)
      {
        case FormControlModel.LABEL_ATTR: labelChanged((String)newValue); break;
        case FormControlModel.TYPE_ATTR: typeChanged((String)newValue); break;
      }
    }

    public void modelRemoved(FormControlModel model)
    {
      bigDaddy.viewShouldBeRemoved(OneFormControlLineView.this);
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
      formularMax4000.broadcast(new BroadcastFormControlModelSelection(getModel(), state, state!=0));
    }
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
  }
}