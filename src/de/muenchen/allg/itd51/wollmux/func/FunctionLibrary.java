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
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.func;

/**
 * Eine Bibliothek von benannten Functions
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionLibrary
{

  /**
   * Erzeugt eine leere Funktionsbibliothek. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FunctionLibrary()
  {
  
  }
  
  /**
   * Erzeugt eine Funktionsbibliothek, die baselib referenziert (nicht kopiert!).
   * baselib wird immer dann befragt, wenn die Funktionsbibliothek selbst keine
   * Funktion des entsprechenden Namens enth�lt.  
   * @param baselib
   */
  public FunctionLibrary(FunctionLibrary baselib)
  {
  
  }

  /**
   * F�gt func dieser Funktionsbibliothek unter dem Namen funcName hinzu.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public void add(String funcName, Function func)
  {
  }

  /**
   * Liefert die Function namens funcName zur�ck oder null, falls keine Funktion
   * mit diesem Namen bekannt ist. Wurde die Funktionsbibliothek mit einer
   * Referenz auf eine andere Funktionsbibliothek initialisiert, so wird diese
   * befragt, falls die Funktionsbibliothek selbst keine Funktion des entsprechenden
   * Namens kennt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public Function get(String funcName)
  {
    return null;
  }
  
}
