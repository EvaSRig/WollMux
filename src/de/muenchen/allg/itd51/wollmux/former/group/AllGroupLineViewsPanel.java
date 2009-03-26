/*
 * Dateiname: AllGroupLineViewsPanel.java
 * Projekt  : WollMux
 * Funktion : Enth�lt alle OneGroupLineViews
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
 * 12.03.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.group;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.DuplicateIDException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.IDManager;
import de.muenchen.allg.itd51.wollmux.former.IndexList;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelection;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

/**
 * Enth�lt alle OneInsertionLineViews.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllGroupLineViewsPanel implements View
{
  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;

  /**
   * Der {@link FormularMax4000} zu dem diese View geh�rt.
   */
  private FormularMax4000 formularMax4000;

  /**
   * Wird auf alle {@link OneGroupLineView}s registriert.
   */
  private ViewChangeListener myViewChangeListener;

  /**
   * Das JPanel, das die ganze View enth�lt.
   */
  private JPanel myPanel;

  /**
   * Das JPanel, das die
   * {@link de.muenchen.allg.itd51.wollmux.former.group.OneGroupLineView}s enth�lt.
   */
  private JPanel mainPanel;

  /**
   * Die JScrollPane, die {@link #mainPanel} enth�lt.
   */
  private JScrollPane scrollPane;

  /**
   * Die Liste der {@link OneGroupLineView}s in dieser View.
   */
  private List<OneGroupLineView> views = new Vector<OneGroupLineView>();

  /**
   * Liste von Indizes der selektierten Objekte in der {@link #views} Liste.
   */
  private IndexList selection = new IndexList();

  /**
   * Die Liste der Models, zu denen diese View die LineViews enth�lt.
   */
  private GroupModelList groupModelList;

  /**
   * Erzeugt ein AllGroupLineViewsPanel, die den Inhalt von groupModelList anzeigt.
   * ACHTUNG! groupModelList sollte leer sein, da nur neu hinzugekommene Elemente in
   * der View angezeigt werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public AllGroupLineViewsPanel(GroupModelList groupModelList,
      FormularMax4000 formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
    this.groupModelList = groupModelList;
    groupModelList.addListener(new MyItemListener());
    formularMax4000.addBroadcastListener(new MyBroadcastListener());
    myViewChangeListener = new MyViewChangeListener();

    mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

    mainPanel.add(Box.createGlue());

    scrollPane =
      new JScrollPane(mainPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.getVerticalScrollBar().setUnitIncrement(
      Common.getVerticalScrollbarUnitIncrement());

    JPanel buttonPanel = new JPanel(new GridBagLayout());

    myPanel = new JPanel(new BorderLayout());
    myPanel.add(scrollPane, BorderLayout.CENTER);
    myPanel.add(buttonPanel, BorderLayout.SOUTH);

    GridBagConstraints gbcButton =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(BUTTON_BORDER, BUTTON_BORDER,
          BUTTON_BORDER, BUTTON_BORDER), 0, 0);
    JButton button = new JButton(L.m("L�schen"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        deleteSelectedElements();
      }
    });
    buttonPanel.add(button, gbcButton);

    ++gbcButton.gridx;
    button = new JButton(L.m("Neu"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        createNewGroup();
      }
    });
    buttonPanel.add(button, gbcButton);

    ++gbcButton.gridx;
  }

  private void createNewGroup()
  {
    int num = 1;
    IDManager.ID id;
    while (true)
    {
      try
      {
        id =
          formularMax4000.getIDManager().getActiveID(
            FormularMax4000.NAMESPACE_GROUPS, "Sichtbarkeit" + num);
        break;
      }
      catch (DuplicateIDException x)
      {
        ++num;
      }
    }
    FunctionSelection condition = new FunctionSelection();
    ConfigThingy funConf = new ConfigThingy("Code");
    funConf.add("CAT").add("true");
    condition.setExpertFunction(funConf);
    GroupModel model = new GroupModel(id, condition, formularMax4000);
    groupModelList.add(model);
  }

  /**
   * F�gt dieser View eine {@link OneGroupLineView} f�r model hinzu.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void addItem(GroupModel model)
  {
    OneGroupLineView view =
      new OneGroupLineView(model, myViewChangeListener, formularMax4000);
    views.add(view);

    /*
     * view vor dem letzten Element von mainPanel einf�gen, weil das letzte Element
     * immer ein Glue sein soll.
     */
    mainPanel.add(view.JComponent(), mainPanel.getComponentCount() - 1);

    mainPanel.validate();
    scrollPane.validate();
  }

  /**
   * Entfernt view aus diesem Container (falls dort vorhanden).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void removeItem(OneGroupLineView view)
  {
    int index = views.indexOf(view);
    if (index < 0) return;
    views.remove(index);
    mainPanel.remove(view.JComponent());
    mainPanel.validate();
    selection.remove(index);
    selection.fixup(index, -1, views.size() - 1);
  }

  /**
   * Hebt die Selektion aller Elemente auf.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void clearSelection()
  {
    Iterator<Integer> iter = selection.iterator();
    while (iter.hasNext())
    {
      Integer I = iter.next();
      OneGroupLineView view = views.get(I.intValue());
      view.unmark();
    }
    selection.clear();
  }

  /**
   * L�scht alle ausgew�hlten Elemente.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void deleteSelectedElements()
  {
    /**
     * Die folgende Schleife muss auf diese Weise geschrieben werden und nicht mit
     * einem Iterator, weil es ansonsten eine ConcurrentModificationException gibt,
     * da �ber {@link ViewChangeListener#viewShouldBeRemoved(View)} die Selektion
     * w�hrend des remove() gleich ver�ndert wird, was den Iterator invalidieren
     * w�rde.
     */
    while (!selection.isEmpty())
    {
      int i = selection.lastElement();
      OneGroupLineView view = views.get(i);
      GroupModel model = view.getModel();
      groupModelList.remove(model);

      /**
       * Es gibt keine M�glichkeit, eine ID aus dem IDManager zu entfernen und ein
       * Inaktivieren reicht nicht, um Namenskollisionen zu verhindern. Deswegen
       * benennen wir die ID einfach um in einen zuf�lligen String.
       */
      IDManager.ID id = model.getID();
      while (true)
      {
        try
        {
          id.setID("ExistiertNichtMehr" + Math.random());
          break;
        }
        catch (DuplicateIDException x)
        {
          continue;
        }
      }
    }
  }

  public JComponent JComponent()
  {
    return myPanel;
  }

  private class MyItemListener implements GroupModelList.ItemListener
  {

    public void itemAdded(GroupModel model, int index)
    {
      addItem(model);
    }

    public void itemRemoved(GroupModel model, int index)
    {}

  }

  private class MyViewChangeListener implements ViewChangeListener
  {

    public void viewShouldBeRemoved(View view)
    {
      removeItem((OneGroupLineView) view);
    }

  }

  private class MyBroadcastListener extends BroadcastListener
  {
    public void broadcastGroupModelSelection(BroadcastObjectSelection b)
    {
      if (b.getClearSelection()) clearSelection();
      GroupModel model = (GroupModel) b.getObject();
      for (int index = 0; index < views.size(); ++index)
      {
        OneGroupLineView view = views.get(index);
        if (view.getModel() == model)
        {
          int state = b.getState();
          if (state == 0) // toggle
            state = selection.contains(index) ? -1 : 1;

          switch (state)
          {
            case -1: // abw�hlen
              view.unmark();
              selection.remove(index);
              break;
            case 1: // ausw�hlen
              view.mark();
              selection.add(index);
              break;
          }
        }
      }
    }
  }
}
