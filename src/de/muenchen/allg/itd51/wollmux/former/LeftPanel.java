//TODO L.m()
/*
* Dateiname: LeftPanel.java
* Projekt  : WollMux
* Funktion : Der �bercontainer f�r die linke H�lfte des FormularMax 4000.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 22.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.muenchen.allg.itd51.wollmux.former.control.AllFormControlLineViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModel;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModelList;
import de.muenchen.allg.itd51.wollmux.former.insertion.AllInsertionLineViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModelList;
import de.muenchen.allg.itd51.wollmux.former.view.View;

/**
 * Der �bercontainer f�r die linke H�lfte des FormularMax 4000.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class LeftPanel implements View
{
  /**
   * H�lt in einem Panel FormControlModelLineViews f�r alle 
   * {@link FormControlModel}s. 
   */
  private AllFormControlLineViewsPanel allFormControlModelLineViewsPanel;
  
  /**
   * H�lt in einem Panel InsertionModelLineViews f�r alle 
   * {@link InsertionModel}s. 
   */
  private AllInsertionLineViewsPanel allInsertionModelLineViewsPanel;
  
  /**
   * Enth�lt alle im linken Panel angezeigten Views.
   */
  private JTabbedPane myTabbedPane;
  
  /**
   * Der FormularMax4000 zu dem dieses Panel geh�rt.
   */
  private FormularMax4000 formularMax4000;
  
  public LeftPanel(InsertionModelList insertionModelList,
      FormControlModelList formControlModelList, FormularMax4000 formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
    allFormControlModelLineViewsPanel = new AllFormControlLineViewsPanel(formControlModelList, formularMax4000);
    allInsertionModelLineViewsPanel = new AllInsertionLineViewsPanel(insertionModelList, formularMax4000);
    
    myTabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    myTabbedPane.add("Formular-GUI", allFormControlModelLineViewsPanel.JComponent());
    myTabbedPane.add("Einf�gungen", allInsertionModelLineViewsPanel.JComponent());
    
    myTabbedPane.addChangeListener(new ChangeListener(){
      public void stateChanged(ChangeEvent e)
      {
        tabSwitched();
      }});
  }
  
  private void tabSwitched()
  {
    if (myTabbedPane.getSelectedComponent() == allFormControlModelLineViewsPanel.JComponent())
    {
      formularMax4000.broadcast(new Broadcast(){
        public void sendTo(BroadcastListener listener)
        {
          listener.broadcastAllFormControlsViewSelected();
        }});
    } else if (myTabbedPane.getSelectedComponent() == allInsertionModelLineViewsPanel.JComponent())
    {
      formularMax4000.broadcast(new Broadcast(){
        public void sendTo(BroadcastListener listener)
        {
          listener.broadcastAllInsertionsViewSelected();
        }});
    }
  }
  
  /**
   * Liefert den Index an dem Buttons auf dem aktuell sichtbaren Tab des
   * {@link AllFormControlLineViewsPanel} eingef�gt
   * werden sollten oder -1, falls dort kein Tab ausgew�hlt ist. Der zur�ckgelieferte
   * Wert (falls nicht -1) entspricht dem Index des letzten sichtbaren
   * Elements + 1.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int getButtonInsertionIndex()
  {
    return allFormControlModelLineViewsPanel.getButtonInsertionIndex(); 
  }
  
  public JComponent JComponent()
  {
    return myTabbedPane;
  }
  
  /**
   * Ruft {@link AllFormControlLineViewsPanel#mergeCheckboxesIntoCombobox()} auf und
   * liefert dessen Ergebnis zur�ck.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ComboboxMergeDescriptor mergeCheckboxesIntoCombobox()
  {
    return allFormControlModelLineViewsPanel.mergeCheckboxesIntoCombobox();   
  }

}
