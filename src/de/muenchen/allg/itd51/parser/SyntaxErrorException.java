/*
* Dateiname: SyntaxErrorException.java
* Projekt  : n/a
* Funktion : Signalisiert einen Fehler in einer zu parsenden Zeichenfolge 
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 06.10.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.parser;

/**
 * TODO Doku
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class SyntaxErrorException extends RuntimeException
{
  public SyntaxErrorException() {};
  public SyntaxErrorException(String message) {super(message);}
  public SyntaxErrorException(String message, Throwable cause) {super(message,cause);}
  public SyntaxErrorException(Throwable cause) {super(cause);}
}
