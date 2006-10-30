/*
* Dateiname: InsertionModelList.java
* Projekt  : WollMux
* Funktion : Verwaltet eine Liste von InsertionModels.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 06.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former.insertion;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;

/**
 * Verwaltet eine Liste von InsertionModels
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class InsertionModelList
{
  /**
   * Die Liste der {@link InsertionModel}s. 
   */
  private List models = new LinkedList();
  
  /**
   * Liste aller {@link ItemListener}, die �ber �nderungen des Listeninhalts informiert
   * werden wollen.
   */
  private List listeners = new Vector(1);
  
  /**
   * Erzeugt eine neue InsertionModelList.
   * @param formularMax4000 der FormularMax4000 zu dem diese Liste geh�rt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public InsertionModelList(FormularMax4000 formularMax4000)
  {
    formularMax4000.addBroadcastListener(new MyBroadcastListener());
  }
  
  /**
   * F�gt model dieser Liste hinzu.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(InsertionModel model)
  {
    int idx = models.size();
    models.add(idx, model);
    notifyListeners(model, idx);
  }
  
  /**
   * L�scht alle bestehenden InsertionModels aus der Liste.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void clear()
  {
    while (!models.isEmpty())
    {
      int index = models.size() - 1;
      InsertionModel model = (InsertionModel)models.remove(index);
      model.hasBeenRemoved();
    }
  }
  
  /**
   * Bittet die InsertionModelList darum, das Element model aus sich zu entfernen
   * (falls es in der Liste ist).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public void remove(InsertionModel model)
  {
    int index = models.indexOf(model);
    if (index < 0) return;
    models.remove(index);
    model.hasBeenRemoved();
  }
  
  /**
   * L�sst alle in dieser Liste gespeicherten {@link InsertionModel}s ihre zugeh�rigen
   * Bookmarks updaten. Falls beim Update eines Bookmarks etwas schiefgeht wird das
   * entsprechende {@link InsertionModel} aus der Liste gel�scht. Das Ausf�hren dieser Funktion
   * triggert also potentiell einige Listener.
   * @param mapFunctionNameToConfigThingy bildet einen Funktionsnamen auf ein ConfigThingy ab, 
   *        dessen Wurzel der Funktionsname ist und dessen Inhalt eine Funktionsdefinition.
   *        Wenn eine Einf�gung mit einer TRAFO versehen ist, wird f�r das Aktualisieren des
   *        Bookmarks ein Funktionsname generiert, der noch nicht in dieser Map vorkommt
   *        und ein Mapping f�r diese Funktion wird in die Map eingef�gt.
   *        Nach dem Aufruf von updateDocument() sind zu dieser Map also Eintr�ge hinzugekommen
   *        f�r alle TRAFOs, die in den Einf�gungen vorkommen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public void updateDocument(Map mapFunctionNameToConfigThingy)
  {
    List defunct = new Vector();
    Iterator iter = models.iterator();
    while (iter.hasNext())
    {
      InsertionModel model = (InsertionModel)iter.next();
      if (!model.updateDocument(mapFunctionNameToConfigThingy))
        defunct.add(model);
    }
    
    iter = defunct.iterator();
    while (iter.hasNext())
    {
      remove((InsertionModel)iter.next());
    }
  }
  
  /**
   * listener wird �ber �nderungen der Liste informiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addListener(ItemListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }
  
  /**
   * Benachrichtigt alle ItemListener �ber das Hinzuf�gen von model zur Liste an Index index.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void notifyListeners(InsertionModel model, int index)
  {
    Iterator iter = listeners.iterator();
    while (iter.hasNext())
    {
      ItemListener listener = (ItemListener)iter.next();
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
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void itemAdded(InsertionModel model, int index);
  }

  private class MyBroadcastListener extends BroadcastListener
  {
    public void broadcastFormControlIdHasChanged(String oldId, String newId) 
    {
      Iterator iter = models.iterator();
      while (iter.hasNext())
      {
        InsertionModel model = (InsertionModel)iter.next();
        model.broadcastFormControlIdHasChanged(oldId, newId);
      }
    
    }
  }
}

