/*
* Dateiname: Plausi.java
* Projekt  : WollMux
* Funktion : Standardfunktionen f�r Plausibilit�tschecks in Formularen
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 26.01.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux;

/**
 * Standardfunktionen f�r Plausibilit�tschecks in Formularen
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class Plausi
{
  public static Boolean immerWahr()
  {
    return new Boolean(true);
  }
  
  public static Boolean zahlenBereich(String low, String hi, String zahl)
  {
    try{
      long l = Long.parseLong(zahl);
      long lo = Long.parseLong(low);
      long high = Long.parseLong(hi);
      if (l < lo) return new Boolean(false);
      if (l > high) return new Boolean(false);
    } catch(Exception x)
    {
      return new Boolean(false);
    }
    return new Boolean(true);
  }

}
