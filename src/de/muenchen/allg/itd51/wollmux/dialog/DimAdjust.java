/*
* Dateiname: DimAdjust.java
* Projekt  : WollMux
* Funktion : Statische Methoden zur Justierung der max.,pref.,min. Size einer JComponent.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 19.10.2007 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Dimension;

import javax.swing.JComponent;

/**
 * Statische Methoden zur Justierung der max.,pref.,min. Size einer JComponent.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DimAdjust
{
  
  /**
   * Setzt die maximale Breite von compo auf unendlich und liefert compo zur�ck.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static JComponent maxWidthUnlimited(JComponent compo)
  {
    Dimension dim = compo.getMaximumSize();
    dim.width = Integer.MAX_VALUE;
    compo.setMaximumSize(dim);
    return compo;
  }
  
  /**
   * Setzt die maximale Breite von compo auf unendlich, die maximale H�he auf
   * die bevorzugte H�he und liefert compo zur�ck.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static JComponent maxHeightIsPrefMaxWidthUnlimited(JComponent compo)
  {
    Dimension dim = compo.getMaximumSize();
    dim.width = Integer.MAX_VALUE;
    dim.height = compo.getPreferredSize().height;
    compo.setMaximumSize(dim);
    return compo;
  }
  
}
