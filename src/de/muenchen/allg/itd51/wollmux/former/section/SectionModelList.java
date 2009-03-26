/*
 * Dateiname: SectionModelList.java
 * Projekt  : WollMux
 * Funktion : Verwaltet eine Liste von SectionModels.
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
 * 24.03.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.section;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;

/**
 * Verwaltet eine Liste von SectionModels.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class SectionModelList implements Iterable<SectionModel>
{
  /**
   * Die Liste der {@link SectionModel}s.
   */
  private List<SectionModel> models = new LinkedList<SectionModel>();

  /**
   * Liste aller {@link ItemListener}, die �ber �nderungen des Listeninhalts
   * informiert werden wollen.
   */
  private List<ItemListener> listeners = new Vector<ItemListener>(1);

  /**
   * Der FormularMax4000 zu dem diese SectionModelList geh�rt.
   */
  // private FormularMax4000 formularMax4000;
  /**
   * Erzeugt eine neue SectionModelList.
   * 
   * @param formularMax4000
   *          der FormularMax4000 zu dem diese Liste geh�rt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public SectionModelList(FormularMax4000 formularMax4000)
  {
  // this.formularMax4000 = formularMax4000;
  // this.formularMax4000.addBroadcastListener(new MyBroadcastListener());
  }

  /**
   * F�gt model dieser Liste hinzu.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(SectionModel model)
  {
    int idx = models.size();
    models.add(idx, model);
    notifyListeners(model, idx, false);
  }

  /**
   * L�scht alle bestehenden SectionModels aus der Liste.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void clear()
  {
    while (!models.isEmpty())
    {
      int index = models.size() - 1;
      SectionModel model = models.remove(index);
      model.hasBeenRemoved();
      notifyListeners(model, index, true);
    }
  }

  /**
   * Liefert true gdw keine SectionModels in der Liste vorhanden sind.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isEmpty()
  {
    return models.isEmpty();
  }

  /**
   * Bittet die SectionModelList darum, das Element model aus sich zu entfernen
   * (falls es in der Liste ist).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void remove(SectionModel model)
  {
    int index = models.indexOf(model);
    if (index < 0) return;
    models.remove(index);
    model.hasBeenRemoved();
    notifyListeners(model, index, true);
  }

  /**
   * L�sst alle in dieser Liste gespeicherten {@link SectionModel}s ihre Name
   * updaten (und damit die entsprechenden GROUPS-Angaben). Falls beim Update eines
   * Bereichs etwas schiefgeht wird das entsprechende {@link SectionModel} aus der
   * Liste gel�scht. Das Ausf�hren dieser Funktion triggert also potentiell einige
   * Listener.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void updateDocument()
  {
    List<SectionModel> defunct = new Vector<SectionModel>();
    Iterator<SectionModel> iter = models.iterator();
    while (iter.hasNext())
    {
      SectionModel model = iter.next();
      if (!model.updateDocument()) defunct.add(model);
    }

    iter = defunct.iterator();
    while (iter.hasNext())
    {
      remove(iter.next());
    }
  }

  /**
   * Liefert einen Iterator �ber alle Models dieser Liste.
   */
  public Iterator<SectionModel> iterator()
  {
    return models.iterator();
  }

  /**
   * listener wird �ber �nderungen der Liste informiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addListener(ItemListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }

  /**
   * Benachrichtigt alle ItemListener �ber das Hinzuf�gen oder Entfernen von model
   * zur bzw. aus der Liste an/von Index index.
   * 
   * @param removed
   *          falls true, wurde model entfernt, ansonsten hinzugef�gt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void notifyListeners(SectionModel model, int index, boolean removed)
  {
    Iterator<ItemListener> iter = listeners.iterator();
    while (iter.hasNext())
    {
      ItemListener listener = iter.next();
      if (removed)
        listener.itemRemoved(model, index);
      else
        listener.itemAdded(model, index);
    }
  }

  /**
   * Interface f�r Klassen, die interessiert sind, zu erfahren, wenn sich die Liste
   * �ndert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ItemListener
  {
    /**
     * Wird aufgerufen nachdem model zur Liste hinzugef�gt wurde (an Index index).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void itemAdded(SectionModel model, int index);

    /**
     * Wird aufgerufen, nachdem model aus der Liste entfernt wurde.
     * 
     * @param index
     *          der alte Index von model in der Liste.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void itemRemoved(SectionModel model, int index);
  }

  /*
   * private class MyBroadcastListener extends BroadcastListener {}
   */

}
