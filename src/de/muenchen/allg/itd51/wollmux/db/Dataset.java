//TODO L.m()
/*
* Dateiname: Dataset.java
* Projekt  : WollMux
* Funktion : Interface f�r Datens�tze einer Tabelle.
* 
 * Copyright (c) 2008 Landeshauptstadt M�nchen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
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
