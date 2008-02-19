//TODO L.m()
/*
* Dateiname: UnavailableException.java
* Projekt  : WollMux
* Funktion : Zeigt an, dass die gew�nschte(n) Funktion(en)/Daten nicht verf�gbar sind.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 18.10.2007 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux;

/**
 * Zeigt an, dass die gew�nschte(n) Funktion(en)/Daten nicht verf�gbar sind.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class UnavailableException extends Exception
{

  /**
   * Unterdr�ckt Warnung.
   */
  private static final long serialVersionUID = 5874615503838299278L;

  public UnavailableException()
  {
    super();
  }

  public UnavailableException(String message)
  {
    super(message);
  }

  public UnavailableException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public UnavailableException(Throwable cause)
  {
    super(cause);
  }

}
