/*
* Dateiname: TrafoDialog.java
* Projekt  : WollMux
* Funktion : Ein Dialog zum Bearbeiten einer TRAFO-Funktion.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 01.02.2008 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog.trafo;

import java.awt.event.ActionListener;

/**
 * Ein Dialog zum Bearbeiten einer TRAFO-Funktion.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class TrafoDialog
{
  /**
   * Zeigt den Dialog an.
   * @param closeAction wird aufgerufen, wenn der Dialog beendet wird. Als source wird
   * im �bergebenen ActionEvent ein {@link TrafoDialogParameters} Objekt �bergeben, das
   * den ge�nderten Trafo-Zustand beschreibt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public abstract void show(ActionListener closeAction);
  
  /**
   * Schlie�t den Dialog. Darf nur aufgerufen werden, wenn er gerade angezeigt wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public abstract void dispose();
}
