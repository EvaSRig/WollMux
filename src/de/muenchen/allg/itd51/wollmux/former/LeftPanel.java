/*
 * Dateiname: LeftPanel.java
 * Projekt  : WollMux
 * Funktion : Der �bercontainer f�r die linke H�lfte des FormularMax 4000.
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

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.former.control.AllFormControlLineViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModel;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModelList;
import de.muenchen.allg.itd51.wollmux.former.group.AllGroupLineViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.group.GroupModel;
import de.muenchen.allg.itd51.wollmux.former.group.GroupModelList;
import de.muenchen.allg.itd51.wollmux.former.insertion.AllInsertionLineViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModelList;
import de.muenchen.allg.itd51.wollmux.former.section.AllSectionLineViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.section.SectionModel;
import de.muenchen.allg.itd51.wollmux.former.section.SectionModelList;
import de.muenchen.allg.itd51.wollmux.former.view.View;

/**
 * Der �bercontainer f�r die linke H�lfte des FormularMax 4000.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class LeftPanel implements View
{
  /**
   * H�lt in einem Panel FormControlModelLineViews f�r alle {@link FormControlModel}s.
   */
  private AllFormControlLineViewsPanel allFormControlModelLineViewsPanel;

  /**
   * H�lt in einem Panel InsertionModelLineViews f�r alle {@link InsertionModel}s.
   */
  private AllInsertionLineViewsPanel allInsertionModelLineViewsPanel;

  /**
   * H�lt in einem Panel GroupModelLineViews f�r alle {@link GroupModel}s.
   */
  private AllGroupLineViewsPanel allGroupModelLineViewsPanel;

  /**
   * H�lt in einem Panel SectionModelLineViews f�r alle {@link SectionModel}s.
   */
  private AllSectionLineViewsPanel allSectionModelLineViewsPanel;

  /**
   * Enth�lt alle im linken Panel angezeigten Views.
   */
  private JTabbedPane myTabbedPane;

  /**
   * Der FormularMax4000 zu dem dieses Panel geh�rt.
   */
  private FormularMax4000 formularMax4000;

  public LeftPanel(InsertionModelList insertionModelList,
      FormControlModelList formControlModelList, GroupModelList groupModelList,
      SectionModelList sectionModelList, FormularMax4000 formularMax4000,
      XTextDocument doc)
  {
    this.formularMax4000 = formularMax4000;
    allFormControlModelLineViewsPanel =
      new AllFormControlLineViewsPanel(formControlModelList, formularMax4000);
    allInsertionModelLineViewsPanel =
      new AllInsertionLineViewsPanel(insertionModelList, formularMax4000);
    allGroupModelLineViewsPanel =
      new AllGroupLineViewsPanel(groupModelList, formularMax4000);
    allSectionModelLineViewsPanel =
      new AllSectionLineViewsPanel(sectionModelList, formularMax4000, doc);

    myTabbedPane =
      new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    myTabbedPane.add(L.m("Formular-GUI"),
      allFormControlModelLineViewsPanel.JComponent());
    myTabbedPane.add(L.m("Einf�gungen"),
      allInsertionModelLineViewsPanel.JComponent());
    myTabbedPane.add(L.m("Sichtbarkeiten"), allGroupModelLineViewsPanel.JComponent());
    myTabbedPane.add(L.m("Bereiche"), allSectionModelLineViewsPanel.JComponent());

    myTabbedPane.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent e)
      {
        tabSwitched();
      }
    });
  }

  private void tabSwitched()
  {
    if (myTabbedPane.getSelectedComponent() == allFormControlModelLineViewsPanel.JComponent())
    {
      formularMax4000.broadcast(new Broadcast()
      {
        public void sendTo(BroadcastListener listener)
        {
          listener.broadcastAllFormControlsViewSelected();
        }
      });
    }
    else if (myTabbedPane.getSelectedComponent() == allInsertionModelLineViewsPanel.JComponent())
    {
      formularMax4000.broadcast(new Broadcast()
      {
        public void sendTo(BroadcastListener listener)
        {
          listener.broadcastAllInsertionsViewSelected();
        }
      });
    }
    else if (myTabbedPane.getSelectedComponent() == allGroupModelLineViewsPanel.JComponent())
    {
      formularMax4000.broadcast(new Broadcast()
      {
        public void sendTo(BroadcastListener listener)
        {
          listener.broadcastAllGroupsViewSelected();
        }
      });
    }
    else if (myTabbedPane.getSelectedComponent() == allSectionModelLineViewsPanel.JComponent())
    {
      formularMax4000.broadcast(new Broadcast()
      {
        public void sendTo(BroadcastListener listener)
        {
          listener.broadcastAllSectionsViewSelected();
        }
      });
    }
  }

  /**
   * Liefert den Index an dem Buttons auf dem aktuell sichtbaren Tab des
   * {@link AllFormControlLineViewsPanel} eingef�gt werden sollten oder -1, falls
   * dort kein Tab ausgew�hlt ist. Der zur�ckgelieferte Wert (falls nicht -1)
   * entspricht dem Index des letzten sichtbaren Elements + 1.
   * 
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
