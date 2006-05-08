/*
* Dateiname: Dialog.java
* Projekt  : WollMux
* Funktion : Ein Dialog, der dem Benutzer erlaubt verschiedenen Werte zu setzen.
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
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.event.ActionListener;

/**
 * Ein Dialog, der dem Benutzer erlaubt verschiedenen Werte zu setzen.
 */
public interface Dialog
{
  /**
   * Liefert den durch id identifizierten Wert den der Dialog im Kontext context
   * hat. 
   * @param context F�r jeden Kontext h�lt der Dialog eine unabh�ngige Kopie von
   *        seinem Zustand vor. Auf diese Weise l�sst sich der Dialog an verschiedenen
   *        Stellen unabh�ngig voneinander einsetzen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Object getData(Object context, String id);
 
  /**
   * Zeigt den Dialog an.
   * @param context F�r jeden Kontext h�lt der Dialog eine unabh�ngige Kopie von
   *        seinem Zustand vor. Auf diese Weise l�sst sich der Dialog an verschiedenen
   *        Stellen unabh�ngig voneinander einsetzen.
   * @param dialogEndListener falls nicht null, wird 
   *        die {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *        Methode aufgerufen (im Event Dispatching Thread), 
   *        nachdem der Dialog geschlossen wurde.
   *        Das actionCommand des ActionEvents gibt die Aktion an, die
   *        das Beenden des Dialogs veranlasst hat.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void show(Object context, ActionListener dialogEndListener);
}
