/*
* Dateiname: RightPanel.java
* Projekt  : WollMux
* Funktion : Managet die rechte H�lfte des FM4000.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 28.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.CardLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import de.muenchen.allg.itd51.wollmux.former.insertion.AllInsertionTrafoViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModelList;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Managet die rechte H�lfte des FM4000.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class RightPanel implements View
{
  /**
   * Identifikationsstring f�r {@link CardLayout}.
   */
  private static final String ALL_INSERTION_TRAFO_VIEWS_PANEL = "ALL_INSERTION_TRAFO_VIEWS_PANEL";
  
  /**
   * Das JPanel, dass alle Inhalte dieser View enth�lt.
   */
  private JPanel myPanel;
  
  /**
   * Erzeugt ein neues RightPanel. Zur Erl�uterung der Parameter siehe
   * {@link de.muenchen.allg.itd51.wollmux.former.insertion.AllInsertionTrafoViewsPanel#AllInsertionTrafoViewsPanel(InsertionModelList, FunctionLibrary, FormularMax4000)}.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public RightPanel(InsertionModelList insertionModelList, FunctionLibrary funcLib, FormularMax4000 formularMax4000)
  {
    myPanel = new JPanel(new CardLayout());
    AllInsertionTrafoViewsPanel allInsertionTrafoViewsPanel = new AllInsertionTrafoViewsPanel(insertionModelList, funcLib, formularMax4000);
    myPanel.add(allInsertionTrafoViewsPanel.JComponent(), ALL_INSERTION_TRAFO_VIEWS_PANEL);
  }

  public JComponent JComponent()
  {
    return myPanel;
  }

}
