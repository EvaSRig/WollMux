/*
* Dateiname: Dataset.java
* Projekt  : WollMux
* Funktion : Interface f�r Datens�tze einer Tabelle.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 06.10.2005 | BNK | Erstellung
* 14.10.2005 | BNK | ->Interface
* 14.10.2005 | BNK | get() throws ColumnNotFoundException
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

/**
 * Interface f�r Datens�tze einer Tabelle.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Dataset
{
  /**
   * Liefert den Wert des Datensatzes aus der Spalte  columnName (null
   * falls nicht belegt).
   * @throws ColumnNotFoundException, falls die Spalte nicht existiert. 
   * Man beachte, dass dies eine Eigenschaft des Datenbankschemas ist und
   * nichts damit zu tun hat, ob der Wert des Datensatzes in der 
   * entsprechenden Spalte gesetzt ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String get(String columnName) throws ColumnNotFoundException;
  
  /**
   * Liefert den Schl�sselwert dieses Datensatzes. Dieser sollte den
   * Datensatz in seiner Datenbank eindeutig identifizieren muss es aber
   * nicht.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getKey();
}
