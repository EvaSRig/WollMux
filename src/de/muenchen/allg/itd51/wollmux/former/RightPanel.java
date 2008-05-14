//TODO L.m()
/*
* Dateiname: RightPanel.java
* Projekt  : WollMux
* Funktion : Managet die rechte H�lfte des FM4000.
* 
 * Copyright (c) 2008 Landeshauptstadt M�nchen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
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

import de.muenchen.allg.itd51.wollmux.former.control.AllFormControlExtViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModelList;
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
   * Identifikationsstring f�r {@link CardLayout}.
   */
  private static final String ALL_FORMCONTROL_EXT_VIEWS_PANEL = "ALL_FORMCONTROL_EXT_VIEWS_PANEL";
  
  /**
   * Das JPanel, dass alle Inhalte dieser View enth�lt.
   */
  private JPanel myPanel;
  
  /**
   * Das CardLayout f�r myPanel.
   */
  private CardLayout cards;
  
  /**
   * Erzeugt ein neues RightPanel. Zur Erl�uterung der Parameter siehe
   * {@link de.muenchen.allg.itd51.wollmux.former.insertion.AllInsertionTrafoViewsPanel#AllInsertionTrafoViewsPanel(InsertionModelList, FunctionLibrary, FormularMax4000)}
   * und 
   * {@link de.muenchen.allg.itd51.wollmux.former.control.AllFormControlExtViewsPanel#AllFormControlExtViewsPanel(FormControlModelList, FunctionLibrary, FormularMax4000)}.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public RightPanel(InsertionModelList insertionModelList, FormControlModelList formControlModelList, FunctionLibrary funcLib, FormularMax4000 formularMax4000)
  {
    cards = new CardLayout();
    myPanel = new JPanel(cards);
    AllFormControlExtViewsPanel allFormControlExtViewsPanel = new AllFormControlExtViewsPanel(formControlModelList, funcLib, formularMax4000);
    myPanel.add(allFormControlExtViewsPanel.JComponent(), ALL_FORMCONTROL_EXT_VIEWS_PANEL);
    AllInsertionTrafoViewsPanel allInsertionTrafoViewsPanel = new AllInsertionTrafoViewsPanel(insertionModelList, funcLib, formularMax4000);
    myPanel.add(allInsertionTrafoViewsPanel.JComponent(), ALL_INSERTION_TRAFO_VIEWS_PANEL);
    
    formularMax4000.addBroadcastListener(new MyBroadcastListener());
  }

  public JComponent JComponent()
  {
    return myPanel;
  }

  private class MyBroadcastListener extends BroadcastListener
  {
    public void broadcastAllInsertionsViewSelected() 
    {
      cards.show(myPanel, ALL_INSERTION_TRAFO_VIEWS_PANEL);
    }
    
    public void broadcastAllFormControlsViewSelected() 
    {
      cards.show(myPanel, ALL_FORMCONTROL_EXT_VIEWS_PANEL);
    }
  }
}
