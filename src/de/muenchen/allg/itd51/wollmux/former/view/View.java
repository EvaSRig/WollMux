//TODO L.m()
/*
* Dateiname: View.java
* Projekt  : WollMux
* Funktion : �ber-Interface f�r alle Views im FormularMax 4000
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
 * http://ec.europa.eu/idabc/en/document/7330/5980
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
