/*
* Dateiname: UIElementEventHandler.java
* Projekt  : WollMux
* Funktion : Interface f�r Klassen, die auf Events reagieren, die von UIElements verursacht werden.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 11.01.2006 | BNK | Erstellung
* 24.04.2006 | BNK | Kommentiert.
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

/**
 * Interface f�r Klassen, die auf Events reagieren, die von UIElements verursacht werden.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface UIElementEventHandler
{
  /**
   * Wird aufgerufen, wenn auf einem UI Element ein Event registriert wird.
   * @param source das UIElement auf dem der Event registriert wurde.
   * @param eventType die Art des Events. Zur Zeit werden folgende Typen unterst�tzt
   * (diese Liste kann erweitert werden, auch f�r existierende UIElemente; ein
   * Handler sollte also zwingend den Typ �berpr�fen und unbekannte Typen ohne
   * Fehlermeldung ignorieren):
   * <dl>
   *   <dt>action</dt>
   *   <dd>Eine ACTION wurde ausgel�st (normalerweise durch einen Button).
   *   Das Array args enth�lt als erstes Element den Namen der ACTION. Falls die
   *   ACTION weitere Parameter ben�tigt, so werden diese in den folgenden
   *   Arrayelementen �bergeben.</dd>
   *   
   *   <dt>valueChanged</dt>
   *   <dd>Wird von Elementen ausgel�st, die der Benutzer bearbeiten kann 
   *   (zum Beispiel Textfields), wenn der Wert ge�ndert wurde. Achtung! Es ist 
   *   nicht
   *   garantiert, dass der Wert sich tats�chlich ge�ndert hat. Dieser Event wird
   *   auch ausgel�st, wenn der Benutzer aus einer Auswahl (z.B. ComboBox)
   *   ein Element ausgew�hlt hat.</dd>
   * </dl> 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void processUiElementEvent(UIElement source, String eventType, Object[] args);
}
