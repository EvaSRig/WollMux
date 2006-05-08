/*
* Dateiname: Values.java
* Projekt  : WollMux
* Funktion : Eine Menge benannter Values.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 04.05.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.func;

/**
 * Eine Menge benannter {@link de.muenchen.allg.itd51.wollmux.func.Value}s.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Values
{
  /**
   * Liefert true genau dann wenn ein Wert mit der ID id vorhanden ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean hasValue(String id);
  
  /**
   * Der aktuelle Wert des mit id identifizierten Values als String. 
   * Falls es sich um einen booleschen Wert
   * handelt, wird der String "true" oder "false" zur�ckgeliefert.
   * Falls kein Wert mit dieser id vorhanden ist wird der leere String
   * geliefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getString(String id);
  
  /**
   * Der aktuelle Wert des mit id identifizierten Values als boolean. 
   * Falls der Wert seiner Natur nach ein
   * String ist, so ist das Ergebnis implementierungsabh�ngig.
   * Falls kein Wert mit dieser id vorhanden ist wird false geliefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean getBoolean(String id);

  /**
   * Dummy-Klasse, die ein Values-Interface zur Verf�gung stellt, das keine
   * Werte enth�lt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static class None implements Values
  {
    public boolean hasValue(String id) { return false; }
    public String getString(String id) { return ""; }
    public boolean getBoolean(String id) { return false; }
  }
}
