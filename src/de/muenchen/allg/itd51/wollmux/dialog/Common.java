/*
* Dateiname: Common.java
* Projekt  : WollMux
* Funktion : Enth�lt von den Dialogen gemeinsam genutzten Code.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 22.11.2005 | BNK | Erstellung
* 26.06.2006 | BNK | +zoomFonts
*                  | refak. von setLookAndFeel() zu setLookAndFeelOnce()
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Font;
import java.util.Enumeration;

import javax.swing.JFrame;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import de.muenchen.allg.itd51.wollmux.Logger;

/**
 * Enth�lt von den Dialogen gemeinsam genutzten Code.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class Common
{
  private static boolean lafSet = false;
  
  /**
   * F�hrt {@link #setLookAndFeel()} aus, aber nur, wenn es bisher
   * noch nicht ausgef�hrt wurde.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void setLookAndFeelOnce()
  {
    if (!lafSet) setLookAndFeel();
  }
  
  /**
   * Setzt das System Look and Feel, falls es nicht MetalLookAndFeel ist. 
   * Ansonsten setzt es GTKLookAndFeel falls m�glich.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static void setLookAndFeel()
  {
    String lafName = UIManager.getSystemLookAndFeelClassName();
    if (lafName.equals("javax.swing.plaf.metal.MetalLookAndFeel"))
      lafName = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
    try{UIManager.setLookAndFeel(lafName);}catch(Exception x){};
    JFrame.setDefaultLookAndFeelDecorated(true);
    lafSet = true;
  }
  
  /**
   * Multipliziert alle Font-Gr��en mit zoomFactor. ACHTUNG! Nach jedem Aufruf
   * von setLookAndFeel() kann diese Funktion genau einmal verwendet werden und
   * hat in folgenden Aufrufen keine Wirkung mehr, bis wieder setLookAndFeel()
   * aufgerufen wird (was den Zoom wieder zur�cksetzt).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void zoomFonts(double zoomFactor)
  {
    Logger.debug("zoomFonts("+zoomFactor+")");
    UIDefaults def = UIManager.getLookAndFeelDefaults();
    Enumeration enu = def.keys();
    int changedFonts = 0;
    while (enu.hasMoreElements())
    {
      Object key = enu.nextElement();
      if (key.toString().endsWith(".font"))
      {
        try{
          FontUIResource res = (FontUIResource)def.get(key);
          Font fnt = res.deriveFont((float)(res.getSize()*zoomFactor));
          def.put(key, fnt);
          ++changedFonts;
        }catch(Exception x) {}
      }
    }
    Logger.debug(changedFonts+" Fontgr��en ver�ndert!");
  }
}
