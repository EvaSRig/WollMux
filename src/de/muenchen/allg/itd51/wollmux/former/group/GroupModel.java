/*
 * Dateiname: GroupModel.java
 * Projekt  : WollMux
 * Funktion : Eine Sichtbarkeitsgruppe, zu der 0 bis mehrere setGroups-Bookmarks geh�ren k�nnen.
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
 * 15.11.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.group;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.Bookmark;
import de.muenchen.allg.itd51.wollmux.DuplicateIDException;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.IDManager;
import de.muenchen.allg.itd51.wollmux.former.IDManager.ID;
import de.muenchen.allg.itd51.wollmux.former.IDManager.IDChangeListener;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelection;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionAccess;
import de.muenchen.allg.itd51.wollmux.former.function.ParamValue;

/**
 * Eine Sichtbarkeitsgruppe, zu der 0 bis mehrere setGroups-Bookmarks geh�ren k�nnen.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class GroupModel
{
  /**
   * F�r {@link ModelChangeListener#attributeChanged(GroupModel, int, Object)}, gibt
   * an, dass die ID (der Name) der Gruppe sich ge�ndert hat.
   */
  public static final int ID_ATTR = 0;

  /**
   * 
   */
  private Map<String, Bookmark> mapBookmarkNameToBookmark =
    new HashMap<String, Bookmark>();

  /**
   * Die ID (=der Name) dieser Gruppe.
   */
  private IDManager.ID id;

  /**
   * Die Liste der setGroups-{@link Bookmark}s, die zu dieser Gruppe geh�ren.
   */
  private List<Integer> bookmarks = new Vector<Integer>(1);

  /**
   * Die Sichtbarkeitsbedingung f�r diese Gruppe.
   */
  private FunctionSelection condition;

  /**
   * Die {@link ModelChangeListener}, die �ber �nderungen dieses Models informiert
   * werden wollen.
   */
  private List<ModelChangeListener> listeners = new Vector<ModelChangeListener>(1);

  /**
   * Der FormularMax4000 zu dem dieses Model geh�rt.
   */
  private FormularMax4000 formularMax4000;

  /**
   * Listener der �nderungen an {@link #id} �berwacht.
   */
  private MyIDChangeListener myIDChangeListener;

  /**
   * Erzeugt eine neue Gruppe mit Name/ID id. ACHTUNG! id muss activated sein!
   * 
   * @param condition
   *          wird direkt als Referenz �bernommen und bestimmt die
   *          Sichtbarkeitsbedingung dieser Gruppe.
   * @param formularMax4000
   *          der {@link FormularMax4000} zu dem diese Gruppe geh�rt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public GroupModel(IDManager.ID id, FunctionSelection condition,
      FormularMax4000 formularMax4000)
  {
    this.id = id;
    // Achtung! Wir m�ssen eine Referenz halten (siehe addIdChangeListener())
    myIDChangeListener = new MyIDChangeListener();
    id.addIDChangeListener(myIDChangeListener);
    this.condition = condition;
    this.formularMax4000 = formularMax4000;
    bookmarks.add(Integer.valueOf(0)); // Dummy-Statement, nur um die Warnung
    // wegzukriegen, dass bookmarks derzeit nicht
    // verwendet wird.
  }

  /**
   * Liefert den FormularMax4000 zu dem dieses Model geh�rt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormularMax4000 getFormularMax4000()
  {
    return formularMax4000;
  }

  /**
   * F�gt das Bookmark bm zu dieser Gruppe hinzu, wenn es in dieser Gruppe noch kein
   * Bookmark gleichen Namens gibt. Falls es schon ein Bookmark gleichen Namens gibt,
   * so wird dieses ersetzt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addBookmark(Bookmark bm)
  {
    mapBookmarkNameToBookmark.put(bm.getName(), bm);
  }

  /**
   * Benachrichtigt alle auf diesem Model registrierten Listener, dass das Model aus
   * seinem Container entfernt wurde. ACHTUNG! Darf nur von einem entsprechenden
   * Container aufgerufen werden, der das Model enth�lt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void hasBeenRemoved()
  {
    Iterator<ModelChangeListener> iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = iter.next();
      listener.modelRemoved(this);
    }
    formularMax4000.documentNeedsUpdating();
  }

  /**
   * listener wird �ber �nderungen des Models informiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addListener(ModelChangeListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }

  /**
   * Liefert immer true. Diese Funktion existiert nur, damit der entsprechende Code
   * in {@link AllGroupFuncViewsPanel} analog zu den anderen Panels gehalten werden
   * kann.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public boolean hasFunc()
  {
    return true;
  }

  /**
   * Liefert ein Interface zum Zugriff auf die Sichtbarkeitsbedingung dieses Objekts.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public FunctionSelectionAccess getConditionAccess()
  {
    return new MyConditionAccess();
  }

  /**
   * Liefert die {@link IDManager.ID} dieser Sichtbarkeitsgruppe zur�ck.
   * 
   * @return
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public IDManager.ID getID()
  {
    return id;
  }

  /**
   * Setzt newID als neuen Namen f�r diese Sichtbarkeitsgruppe und benachrichtigt
   * alle mittels
   * {@link #addListener(de.muenchen.allg.itd51.wollmux.former.group.GroupModel.ModelChangeListener)}
   * registrierten Listener.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * @throws DuplicateIDException
   *           falls newID bereits von einer anderen Sichtbarkeitsgruppe verwendet
   *           wird.
   * 
   * TESTED
   */
  public void setID(String newID) throws DuplicateIDException
  {
    id.setID(newID);
    /**
     * IDManager.ID ruft MyIDChangeListener.idHasChanged() auf, was wiederum die
     * Listener auf diesem Model benachrichtigt.
     */
  }

  /**
   * Liefert ein ConfigThingy zur�ck, dessen Name der Name der Gruppe ist und dessen
   * Inhalt die Definition der Sichtbarkeitsfunktion der Gruppe ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy export()
  {
    return condition.export(id.toString());
  }

  /**
   * Ruft f�r jeden auf diesem Model registrierten {@link ModelChangeListener} die
   * Methode {@link ModelChangeListener#attributeChanged(GroupModel, int, Object)}
   * auf.
   */
  protected void notifyListeners(int attributeId, Object newValue)
  {
    Iterator<ModelChangeListener> iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = iter.next();
      listener.attributeChanged(this, attributeId, newValue);
    }
    formularMax4000.documentNeedsUpdating();
  }

  /**
   * Interface f�r Listener, die �ber �nderungen eines Models informiert werden
   * wollen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ModelChangeListener
  {
    /**
     * Wird aufgerufen wenn ein Attribut des Models sich ge�ndert hat.
     * 
     * @param model
     *          das Model, das sich ge�ndert hat.
     * @param attributeId
     *          eine der {@link GroupModel#ID_ATTR Attribut-ID-Konstanten}.
     * @param newValue
     *          der neue Wert des Attributs. Die ID wird als {@link IDManager.ID}
     *          �bergeben.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void attributeChanged(GroupModel model, int attributeId, Object newValue);

    /**
     * Wird aufgerufen, wenn model aus seinem Container entfernt wird (und damit in
     * keiner View mehr angezeigt werden soll).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void modelRemoved(GroupModel model);
  }

  private class MyIDChangeListener implements IDChangeListener
  {
    public void idHasChanged(ID id)
    {
      notifyListeners(ID_ATTR, id);
    }
  }

  /**
   * Diese Klasse leitet Zugriffe weiter an das Objekt {@link GroupModel#condition}.
   * Bei �ndernden Zugriffen wird auch noch der FormularMax4000 benachrichtigt, dass
   * das Dokument geupdatet werden muss. Im Prinzip m�sste korrekterweise ein
   * �ndernder Zugriff auch einen Event an die ModelChangeListener schicken.
   * Allerdings ist dies derzeit nicht implementiert, weil es derzeit genau eine View
   * gibt f�r die Condition, so dass konkurrierende �nderungen gar nicht m�glich
   * sind.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyConditionAccess implements FunctionSelectionAccess
  {
    public boolean isReference()
    {
      return condition.isReference();
    }

    public boolean isExpert()
    {
      return condition.isExpert();
    }

    public boolean isNone()
    {
      return condition.isNone();
    }

    public String getFunctionName()
    {
      return condition.getFunctionName();
    }

    public ConfigThingy getExpertFunction()
    {
      return condition.getExpertFunction();
    }

    public void setParameterValues(Map<String, ParamValue> mapNameToParamValue)
    {
      condition.setParameterValues(mapNameToParamValue);
      formularMax4000.documentNeedsUpdating();
    }

    public void setFunction(String functionName, String[] paramNames)
    {
      condition.setFunction(functionName, paramNames);
      formularMax4000.documentNeedsUpdating();
    }

    public void setExpertFunction(ConfigThingy funConf)
    {
      condition.setExpertFunction(funConf);
      formularMax4000.documentNeedsUpdating();
    }

    public void setParameterValue(String paramName, ParamValue paramValue)
    {
      condition.setParameterValue(paramName, paramValue);
      formularMax4000.documentNeedsUpdating();
    }

    public String[] getParameterNames()
    {
      return condition.getParameterNames();
    }

    public boolean hasSpecifiedParameters()
    {
      return condition.hasSpecifiedParameters();
    }

    public ParamValue getParameterValue(String paramName)
    {
      return condition.getParameterValue(paramName);
    }

  }

}
