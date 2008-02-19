//TODO L.m()
/*
* Dateiname: DatasetNotFoundException.java
* Projekt  : WollMux
* Funktion : Wird geworfen, falls kein passender Datensatz gefunden wurde und
*            die M�glichkeit, eine leere Ergebnisliste zur�ckzugeben nicht
*            existiert. 
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 14.10.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

/**
 * Wird geworfen, falls kein passender Datensatz gefunden wurde und
 * die M�glichkeit, eine leere Ergebnisliste zur�ckzugeben nicht
 * existiert. 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DatasetNotFoundException extends Exception
{
  private static final long serialVersionUID = -2369645834768269537L;
  public DatasetNotFoundException() {};
  public DatasetNotFoundException(String message) {super(message);}
  public DatasetNotFoundException(String message, Throwable cause) {super(message,cause);}
  public DatasetNotFoundException(Throwable cause) {super(cause);}
}
