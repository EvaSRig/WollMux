/*
* Dateiname: TestDJDataset.java
* Projekt  : WollMux
* Funktion : Implementierung von DJDataset zu Testzwecken.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 14.10.2005 | BNK | Erstellung
* 20.10.2005 | BNK | Stark erweitert 
* 20.10.2005 | BNK | Unterst�tzung f�r Fallback
* 03.11.2005 | BNK | besser kommentiert
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implementierung von DJDataset zu Testzwecken. Eine zentrale Eigenschaft von
 * TestDJDataset ist, dass es f�r Spalten, f�r die kein Wert gesetzt ist den
 * Spaltennamen als Antwort auf get()-Anfragen liefern kann, so dass alle Spalten
 * von aussen betrachtet belegt sind.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class TestDJDataset extends DJDatasetBase
{
  /**
   * siehe Konstruktor.
   */
  private Map fallback = null;
  
  /** Erzeugt einen TestDJDataset, der jedes Schema unterst�tzt und 
   * als aus dem LOS kommend betrachtet wird.
   */
  public TestDJDataset() 
  {
    super(new HashMap(), new HashMap(), null);
  }
  
  /**
   * Erzeugt ein neues TestDJDataset. 
   * @param backingStore mappt Spaltennamen auf den Spaltenwert des Datensatzes
   *        in der Hintergrunddatenbank. Es m�ssen nicht alle Spalten enthalten
   *        sein. Der Mechanismus zum automatischen Generieren von Spaltenwerten
   *        aus dem Spaltennamen existiert weiter.
   * @param schema falls nicht null �bergeben wird, erzeugen Zugriffe auf
   *        Spalten mit Namen, die nicht in schema sind Exceptions.
   * @param isFromLOS legt fest, ob der Datensatz als aus dem LOS kommend
   *        betrachtet werden soll (also insbesondere ob er {@link #set(String, String)}
   *        unterst�tzen soll).
   * @param fallback falls fallback nicht null ist, so wird 
   *        falls der Wert f�r eine Spalte nicht gesetzt ist (nicht
   *        zu verwechseln mit gesetzt auf den leeren String!) versucht,
   *        anhand dieser Map den Spaltennamen auf einen anderen Spaltennamen
   *        umzusetzen, dessen Wert dann geliefert wird. 
   */
  public TestDJDataset(Map backingStore, Set schema, boolean isFromLOS, Map fallback)
  {
    super(backingStore, isFromLOS?new HashMap():null,schema);
    this.fallback = fallback;
  }
  
  /** 
   * Liefert die Map, die dem Konstruktor als backingStore Argument �bergeben
   * wurde.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Map getBS()
  {
    return myBS;
  }
  
  public String get(String spaltenName) throws ColumnNotFoundException
  {
    String str = super.get(spaltenName);
    if (str != null) return str;
    
    if (fallback != null && fallback.containsKey(spaltenName)) 
      return get((String)fallback.get(spaltenName)); 

    return spaltenName;
  }

  public DJDataset copy() { return new TestDJDataset(hasBackingStore()? new HashMap(myBS):null, schema, true, fallback);}
  
  public void remove(){}

  public boolean isSelectedDataset()
  {
    return false;
  }

  public void select() throws UnsupportedOperationException
  {
    if (!isFromLOS()) throw new UnsupportedOperationException();
  }

  public String getKey()
  {
    return ""+this.hashCode();
  };
}
