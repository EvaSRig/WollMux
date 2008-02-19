//TODO L.m()
/*
* Dateiname: IDManager.java
* Projekt  : WollMux
* Funktion : Verwaltet Objekte, die ID-Strings repr�sentieren.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 11.07.2007 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.muenchen.allg.itd51.wollmux.DuplicateIDException;

/**
 * verwaltet Objekte, die ID-Strings repr�sentieren. Die ID-Objekte k�nnen an
 * mehreren Stellen verwendet werden und da jedes ID-Objekt alle seine Verwender
 * kennt (wenn sie sich als Listener registrieren) k�nnen �nderungen an der ID 
 * allen Verwendern mitgeteilt
 * werden.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class IDManager
{
  private Map mapNamespace2mapString2ID = new HashMap();
  
  /**
   * Liefert ein {@link IDManager.ID}-Objekt zur String-ID id im Namensraum
   * namespace. Falls dieser Manager
   * zu dieser String-ID noch kein Objekt hatte, wird ein neues angelegt, ansonsten
   * das bereits existierende zur�ckgeliefert. Wird ein neues ID-Objekt angelegt,
   * so ist dieses inaktiv (siehe {@link IDManager.ID#isActive()}). Diese Funktion
   * darf also nur von Aufrufern verwendet werden, die die ID als Referenz auf ein
   * anderes Objekt ben�tigen. Aufrufer, die sich selbst mit der ID identifizieren
   * wollen m�ssen {@link #getActiveID(Object, String)} verwenden. 
   * @param namespace ein beliebiger Identifikator f�r den gew�nschten Namensraum.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public ID getID(Object namespace, String id)
  {
    if (!mapNamespace2mapString2ID.containsKey(namespace))
      mapNamespace2mapString2ID.put(namespace, new HashMap());
    
    Map mapString2ID = (Map)mapNamespace2mapString2ID.get(namespace);
    
    if (!mapString2ID.containsKey(id))
      mapString2ID.put(id, new ID(mapString2ID, id));
    
    return (ID)mapString2ID.get(id);
  }
  
  /**
   * Falls dieser Manager im Namensraum namespace ein Objekt mit String-ID id hat,
   * so wird dieses zur�ckgeliefert, ansonsten null.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ID getExistingID(Object namespace, String id)
  {
    if (!mapNamespace2mapString2ID.containsKey(namespace))
      return null;
    
    Map mapString2ID = (Map)mapNamespace2mapString2ID.get(namespace);
    
    if (!mapString2ID.containsKey(id))
      return null;
    
    return (ID)mapString2ID.get(id); 
  }
  
  /**
   * Falls im angegebenen namespace bereits ein ID Objekt f�r die String-ID id
   * existiert und dieses {@link IDManager.ID#isActive()} aktiv ist, so wird eine
   * {@link DuplicateIDException} geworfen, ansonsten wird das existierende ID Objekt
   * aktiviert oder (falls noch keins existierte) ein aktiviertes ID Objekt neu 
   * angelegt und dann zur�ckgeliefert. Diese Funktion ist daf�r vorgesehen, von
   * Aufrufern verwendet zu werden, die sich selbst mit der ID identifizieren wollen.
   * Aufrufer, die die ID als Referenz auf ein anderes Objekt verwenden, m�ssen
   * {@link #getID(Object, String)} verwenden.
   * @param namespace ein beliebiger Identifikator f�r den gew�nschten Namensraum.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ID getActiveID(Object namespace, String id) throws DuplicateIDException
  {
    ID idO = getID(namespace, id);
    idO.activate();
    return idO;
  }
  
  /**
   * Liefert eine {@link Collection} mit allen {@link IDManager.ID} Objekten, die im
   * Namensraum namespace registriert sind. ACHTUNG! Die zur�ckgegebene Collection darf nicht
   * ge�ndert oder gespeichert werden, da sie direkt eine interne Datenstruktur ist! 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Collection getAllIDs(Object namespace)
  {
    if (!mapNamespace2mapString2ID.containsKey(namespace)) return new ArrayList();
    
    Map mapString2ID = (Map)mapNamespace2mapString2ID.get(namespace);
    return mapString2ID.values();
  }
  
  /**
   * Ein Objekt, das eine String-ID repr�sentiert.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static class ID
  {
    /**
     * Die String-ID, die dieses Objekt repr�sentiert.
     */
    private String id;
    
    /**
     * Die Map des verwaltenden IDManagers, in der diese ID gespeichert ist.
     * Wird verwendet, um Kollisionen zu �berpr�fen und das Mapping anzupassen,
     * wenn der ID-String dieses Objekts ge�ndert wird.
     */
    private Map mapString2ID;
    
    /**
     * true bedeutet, dass irgendwo ein Objekt tats�chlich verwendet wird, das sich
     * mit dieser ID identifiziert. False bedeutet, dass alle Verwender dieser ID
     * damit nur ein anderes Objekt referenzieren wollen (das derzeit nicht existiert).
     */
    private boolean active = false;
    
    /**
     * Liste von {@link WeakReference}s auf {@link IDManager.IDChangeListener}.
     */
    private List listeners = new Vector();
    
    /**
     * Erstellt ein neues ID Objekt, das inaktiv (siehe {@link #isActive()} ist.
     */
    private ID(Map mapString2ID, String id)
    {
      this.id = id;
      this.mapString2ID = mapString2ID;
    }
    
    /**
     * Liefert true, wenn irgendwo ein Objekt tats�chlich verwendet wird, das sich
     * mit dieser ID identifiziert. False bedeutet, dass alle Verwender dieser ID
     * damit nur ein anderes Objekt referenzieren wollen (das derzeit nicht existiert).
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public boolean isActive() { return active;}
    
    /**
     * Setzt diese ID auf {@link #isActive() aktiv} oder wirft 
     * {@link DuplicateIDException}, falls sie es schon ist.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void activate() throws DuplicateIDException
    {
      if (isActive()) throw new DuplicateIDException();
      active = true;
    }
    
    /**
     * Setzt diese ID auf {@link #isActive() inaktiv}.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void deactivate()
    {
      active = false;
    }
    
    /**
     * listen wird benachrichtigt, wenn sich dieses ID-Objekt �ndert.
     * ACHTUNG! listen wird nur �ber eine {@link java.lang.ref.WeakReference}
     * referenziert. Daher ist der Aufruf von {@link #removeIDChangeListener(IDChangeListener)} nur
     * notwendig, wenn man keine Events mehr empfangen m�cht, nicht jedoch vor der
     * Zerst�rung des Listeners.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    public void addIDChangeListener(IDChangeListener listen)
    {
      Iterator iter = listeners.iterator();
      while (iter.hasNext())
      {
        Reference ref = (Reference)iter.next();
        IDChangeListener listen2 = (IDChangeListener)ref.get();
        if (listen2 == null) 
          iter.remove();
        else
          if (listen2 == listen) return;
      }
      listeners.add(new WeakReference(listen));
    }
    
    /**
     * listen wird NICHT MEHR benachrichtigt, wenn sich dieses ID-Objekt �ndert.
     * ACHTUNG! listen wird von {@link #addIDChangeListener(IDChangeListener)} nur �ber eine 
     * {@link java.lang.ref.WeakReference}
     * referenziert. Daher ist der Aufruf von {@link #removeIDChangeListener(IDChangeListener)} nur
     * notwendig, wenn man keine Events mehr empfangen m�cht, nicht jedoch vor der
     * Zerst�rung des Listeners. 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    public void removeIDChangeListener(IDChangeListener listen)
    {
      Iterator iter = listeners.iterator();
      while (iter.hasNext())
      {
        Reference ref = (Reference)iter.next();
        IDChangeListener listen2 = (IDChangeListener)ref.get();
        if (listen2 == null || listen2 == listen) 
          iter.remove();
      }
    }
    
    /**
     * �ndert die String-ID dieses Objekts auf newID und benachrichtigt alle
     * {@link IDManager.IDChangeListener}. Falls newID == {@link #getID()}, so
     * passiert nichts, es werden keine Listener benachrichtigt und es gibt keine
     * Exception. ACHTUNG! Normalerweise darf diese Funktion nur von dem Objekt aufgerufen
     * werden, das sich mit dieser ID identifiziert, nicht von Objekten die diese ID nur
     * als Referenz verwenden.
     * @see #addIDChangeListener(IDChangeListener)
     * @throws DuplicateIDException wenn newID bereits im Namensraum dieses
     *         ID-Objekts verwendet wird. 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    public void setID(String newID) throws DuplicateIDException
    {
      if (newID.equals(id)) return;
      /*
       * Achtung! Hier wird bewusst nicht nach aktiven und inaktiven IDs unterschieden.
       * Man k�nnte versucht sein, Kollisionen mit inaktiven IDs zuzulassen und so
       * aufzul�sen, dass die aktive ID die inaktive ID "aufsammelt". 
       * Vorteile:
       *   Anlegen einer Einf�gung mit nicht vergebener ID und nachtr�gliches erzeugen eines
       *   Controls mit dieser ID w�rde funktionieren
       * Nachteile:
       *   - W�hrend des Tippens einer neuen ID w�rde evtl. schon eine ungewollte ID aufgesammelt.
       *   Beispiel: inaktive IDs "Anrede" und "Anrede2" existieren parallel. Es w�rde bereits
       *   "Anrede" aufgesammelt w�hrend des Tippens, auch wenn am Ende "Anrede2" gew�nscht ist.
       *   - Eventuell noch weitere. Die Folgen sind nicht so leicht abzusch�tzen. 
       */
      if (mapString2ID.containsKey(newID)) throw new DuplicateIDException("Kollision beim Versuch ID von \""+id+"\" auf \""+newID+"\" zu �ndern");
      mapString2ID.remove(id);
      id = newID;
      mapString2ID.put(id, this);
      Iterator iter = listeners.iterator();
      while (iter.hasNext())
      {
        Reference ref = (Reference)iter.next();
        IDChangeListener listen = (IDChangeListener)ref.get();
        if (listen == null) 
          iter.remove();
        else
          listen.idHasChanged(this);
      }
    }
    
    /**
     * Liefert die String-ID zur�ck, die dieses Objekt repr�sentiert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public String getID() {return id;}
    /**
     * wie {@link #getID()}.
     */
    public String toString() {return id;}
    
    /**
     * Liefert true, wenn this == obj, da �ber den IDManager sichergestellt wird,
     * dass zu einem ID-String in einem Namensraum jeweils nur ein einziges ID-Objekt
     * existiert. Dies ist auch Voraussetzung daf�r, dass die IDs ihre
     * Funktion erf�llen k�nnen.
     */
    public boolean equals(Object obj)
    {
      return this == obj;
    }
    
    public int hashCode()
    {
      return super.hashCode();
    }
  }
  
  /**
   * Ein IDChangeListener wird benachrichtigt, wenn sich ein
   * {@link IDManager.ID} Objekt �ndert.
   *
   * @see IDManager.ID#addIDChangeListener(IDChangeListener)
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public interface IDChangeListener
  {
    public void idHasChanged(ID id);
  }
}
