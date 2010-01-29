/*
 * Dateiname: GroupsProvider.java
 * Projekt  : WollMux
 * Funktion : c
 * 
 * Copyright (c) 2008 Landeshauptstadt M�nchen
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL), 
 * version 1.0 (or any later version).
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
 * 13.03.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.group;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.IDManager;
import de.muenchen.allg.itd51.wollmux.former.IDManager.ID;

/**
 * Ein Ding, das GROUPS �ber ihre IDs referenziert.
 * 
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class GroupsProvider implements Iterable<IDManager.ID>
{
  private Set<IDManager.ID> groups = new HashSet<IDManager.ID>();

  /**
   * Wer wird alles benachrichtigt, wenn Gruppen hinzukommen oder entfernt werden.
   */
  private List<WeakReference<GroupsChangedListener>> listeners =
    new Vector<WeakReference<GroupsChangedListener>>();

  /**
   * Der heilige Meister, den wir anbeten.
   */
  private FormularMax4000 formularMax4000;

  /**
   * Erzeugt einen neuen GroupsProvider, der
   * {@link FormularMax4000#documentNeedsUpdating()} aufruft, wenn etwas an seiner
   * Liste ge�ndert wird.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public GroupsProvider(FormularMax4000 formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
  }

  /**
   * Liefert die Menge der GROUPS-Werte dieses FormControlModels. Die gelieferte
   * Liste ist keine Referenz auf die interne Datenstruktur. Hinzuf�gen und L�schen
   * von Eintr�gen muss �ber
   * {@link #addGroup(de.muenchen.allg.itd51.wollmux.former.IDManager.ID)} bzw.
   * {@link #removeGroup(de.muenchen.allg.itd51.wollmux.former.IDManager.ID)}
   * erfolgen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Set<IDManager.ID> getGroups()
  {
    return new HashSet<IDManager.ID>(groups);
  }

  /**
   * F�gt id zu den GROUPS hinzu, falls noch nicht enthalten.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public void addGroup(IDManager.ID id)
  {
    if (!groups.contains(id))
    {
      groups.add(id);
      notifyListeners(id, false);
      formularMax4000.documentNeedsUpdating();
    }

  }

  /**
   * Entfernt id aus GROUPS, falls dort enthalten.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public void removeGroup(IDManager.ID id)
  {
    if (groups.remove(id)) groupHasBeenRemoved(id);
    // ACHTUNG! Die remove()-Methode von MyIterator muss mit removeGroup()
    // synchronisiert bleiben
  }

  /**
   * Wird von {@link #removeGroup(ID)} und {@link MyIterator#remove()} aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  private void groupHasBeenRemoved(IDManager.ID id)
  {
    notifyListeners(id, true);
    formularMax4000.documentNeedsUpdating();
  }

  /**
   * Ersetzt die interne Gruppenliste durch groups. ACHTUNG! FormularMax 4000 wird
   * nicht aufgefordert, das Dokument zu updaten. Diese Methode sollte nur zur
   * Initialisierung des Objekts vor der Verwendung aufgerufen werden.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public void initGroups(Set<IDManager.ID> groups)
  {
    this.groups = groups;
  }

  /**
   * Liefert true gdw, die GROUPS-Angabe nicht leer ist.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public boolean hasGroups()
  {
    return !groups.isEmpty();
  }

  /**
   * listener wird benachrichtigt, wenn Gruppen hinzugef�gt oder entfernt werden.
   * ACHTUNG! Es wird nur eine {@link WeakReference} auf listener gehalten! Das
   * heisst der Listener muss auf andere Weise am Leben gehalten werden. Andererseits
   * muss er nicht deregistriert werden, wenn er verschwindet.
   * 
   * @param listener
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * TESTED
   */
  public void addGroupsChangedListener(GroupsChangedListener listener)
  {
    Iterator<WeakReference<GroupsChangedListener>> iter = listeners.iterator();
    while (iter.hasNext())
    {
      Reference<GroupsChangedListener> ref = iter.next();
      GroupsChangedListener listen2 = ref.get();
      if (listen2 == null)
        iter.remove();
      else if (listen2 == listener) return;
    }
    listeners.add(new WeakReference<GroupsChangedListener>(listener));
  }

  /**
   * Benachrichtigt alle {@link GroupsChangedListener}, dass id hinzugef�gt
   * (remove==false) oder entfernt (remove==true) wurde.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * TESTED
   */
  private void notifyListeners(IDManager.ID id, boolean remove)
  {
    Iterator<WeakReference<GroupsChangedListener>> iter = listeners.iterator();
    while (iter.hasNext())
    {
      Reference<GroupsChangedListener> ref = iter.next();
      GroupsChangedListener listen = ref.get();
      if (listen == null)
        iter.remove();
      else
      {
        if (remove)
          listen.groupRemoved(id);
        else
          listen.groupAdded(id);
      }
    }
  }

  public interface GroupsChangedListener
  {
    public void groupAdded(IDManager.ID groupID);

    public void groupRemoved(IDManager.ID groupID);
  }

  public Iterator<ID> iterator()
  {
    return new MyIterator();
  }

  private class MyIterator implements Iterator<ID>
  {
    private Iterator<ID> iter;

    private ID lastReturnedID;

    public MyIterator()
    {
      iter = groups.iterator();
    }

    public boolean hasNext()
    {
      return iter.hasNext();
    }

    public ID next()
    {
      lastReturnedID = iter.next();
      return lastReturnedID;
    }

    public void remove()
    {
      iter.remove();
      groupHasBeenRemoved(lastReturnedID);
    }
  }
}
