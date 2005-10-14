/*
* Dateiname: QueryResults.java
* Projekt  : WollMux
* Funktion : Ergebnisse einer Datenbankanfrage.
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

import java.util.Iterator;

/**
 * Ergebnisse einer Datenbankanfrage.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface QueryResults
{
  /**
   * Die Anzahl der Ergebnisse.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int size();
  
  /**
   * Iterator �ber die Ergebnisse ({@link Dataset} Objekte).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Iterator iterator();
}
