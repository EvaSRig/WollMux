/*
* Dateiname: Function.java
* Projekt  : WollMux
* Funktion : Eine Funktion, die einen Wert in Abh�ngigkeit von Parametern berechnet.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 03.05.2006 | BNK | Erstellung
* 31.05.2006 | BNK | +getFunctionDialogReferences()
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.func;

import java.util.Collection;

import de.muenchen.allg.itd51.wollmux.L;

/**
 * Eine Funktion, die einen Wert in Abh�ngigkeit von Parametern berechnet.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Function
{
  public static final String ERROR = L.m("!��!FEHLERHAFTE DATEN!��!");
  
  /**
   * Liefert die Namen der Parameter, die die Funktion erwartet.
   * Die Reihenfolge ist undefiniert und es kann ein Name mehrfach vorkommen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String[] parameters();
  
  /**
   * Zu set werden die Namen aller Funktionsdialoge hinzugef�gt, die diese
   * Funktion referenziert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void getFunctionDialogReferences(Collection<String> set);
  
  /**
   * Ruft die Funktion mit Argumenten aus parameters auf und liefert das
   * Funktionsergebnis als String. Falls es sich um einen booleschen Wert
   * handelt, wird der String "true" oder "false" zur�ckgeliefert.
   * Falls w�hrend der Ausf�hrung ein Fehler auftritt, wird m�glicherweise (dies
   * h�ngt von der Funktion ab) das String-Objekt
   * {@link #ERROR} (== vergleichbar) zur�ckgeliefert. 
   * @param parameters sollte zu jedem der von {@link #parameters()} gelieferten
   *        Namen einen String-Wert enthalten.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getString(Values parameters);
  
   /**
   * Ruft die Funktion mit Argumenten aus parameters auf und liefert das
   * Funktionsergebnis als boolean. Falls der Wert seiner Natur nach ein
   * String ist, so wird true geliefert, falls er (ohne Ber�cksichtigung von
   * Gro�-/Kleinschreibung) der Zeichenkette "true" entspricht.
   * Falls w�hrend der Ausf�hrung ein Fehler auftritt wird false zur�ckgeliefert.
   * @param parameters sollte zu jedem der von {@link #parameters()} gelieferten
   *        Namen einen String-Wert enthalten.

   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean getBoolean(Values parameters);

}
