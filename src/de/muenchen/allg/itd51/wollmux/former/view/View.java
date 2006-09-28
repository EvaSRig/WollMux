/*
* Dateiname: View.java
* Projekt  : WollMux
* Funktion : �ber-Interface f�r alle Views im FormularMax 4000
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 29.08.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former.view;

import javax.swing.JComponent;

/**
 * �ber-Interface f�r alle Views im FormularMax 4000.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface View
{
  /**
   * Liefert die Komponente f�r diese View.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public JComponent JComponent();
}
