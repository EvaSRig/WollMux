//TODO L.m()
/*
* Dateiname: FunctionLibrary.java
* Projekt  : WollMux
* Funktion : Eine Bibliothek von benannten Functions
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 03.05.2006 | BNK | Erstellung
* 08.05.2006 | BNK | Fertig implementiert.
* 26.09.2006 | BNK | +hasFunction()
* 27.09.2006 | BNK | +getFunctionNames()
* 15.11.2007 | BNK | +remove()
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.func;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Eine Bibliothek von benannten Functions
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionLibrary
{
  private Map<String, Function> mapIdToFunction;
  private FunctionLibrary baselib;
  
  /**
   * Erzeugt eine leere Funktionsbibliothek. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FunctionLibrary()
  {
    this(null);
  }
  
  /**
   * Erzeugt eine Funktionsbibliothek, die baselib referenziert (nicht kopiert!).
   * baselib wird immer dann befragt, wenn die Funktionsbibliothek selbst keine
   * Funktion des entsprechenden Namens enth�lt. baselib darf null sein. 
   * @param baselib
   */
  public FunctionLibrary(FunctionLibrary baselib)
  {
    mapIdToFunction = new HashMap<String, Function>();
    this.baselib = baselib; 
  }

  /**
   * F�gt func dieser Funktionsbibliothek unter dem Namen funcName hinzu.
   * Eine bereits existierende Funktion mit diesem Namen wird dabei ersetzt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(String funcName, Function func)
  {
    if (func == null || funcName == null) throw new NullPointerException("Weder Funktionsname noch Funktion darf null sein");
    mapIdToFunction.put(funcName, func);
  }

  /**
   * Liefert die Function namens funcName zur�ck oder null, falls keine Funktion
   * mit diesem Namen bekannt ist. Wurde die Funktionsbibliothek mit einer
   * Referenz auf eine andere Funktionsbibliothek initialisiert, so wird diese
   * befragt, falls die Funktionsbibliothek selbst keine Funktion des entsprechenden
   * Namens kennt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Function get(String funcName)
  {
    Function func = mapIdToFunction.get(funcName);
    if (func == null && baselib != null) func = baselib.get(funcName);
    return func;
  }
  
  /**
   * Versucht, alle Funktionen namens funcName aus dieser und evtl, verketteter
   * Funktionsbibliotheken zu entfernen.
   * @return true, falls nach Ausf�hrung des Befehls {@link #hasFunction(String)) f�r
   *         funcName false zur�ckliefert, false sonst.
   *         D.h. true wird geliefert, wenn 
   *         alle Funktionen entfernt werden konnten. Falls false
   *         zur�ckgeliefert wird, wurden evtl. manche, aber definitiv nicht alle Funktionen
   *         entfernt. Falls von vorneherein keine Funktion funcName vorhanden war,
   *         wird auch true geliefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean remove(String funcName)
  {
    mapIdToFunction.remove(funcName);
    if (baselib != null) return baselib.remove(funcName);
    return true;
  }
  
  /**
   * Liefert true wenn diese Funktionsbibliothek eine Funktion namens funcName kennt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean hasFunction(String funcName)
  {
    if (mapIdToFunction.containsKey(funcName)) return true;
    if (baselib != null) return baselib.hasFunction(funcName);
    return false;
  }
  
  /**
   * Liefert die Namen aller Funktionen, die �ber diese Funktionsbibliothek
   * verf�gbar sind.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Set<String> getFunctionNames()
  {
    Set<String> names = new HashSet<String>(mapIdToFunction.keySet());
    if (baselib != null) names.addAll(baselib.getFunctionNames());
    return names;
  }
}
