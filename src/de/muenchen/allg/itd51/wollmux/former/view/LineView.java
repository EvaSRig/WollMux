/*
 * Dateiname: LineView.java
 * Projekt  : WollMux
 * Funktion : Abstrakte Oberklasse f�r einzeilige Sichten.
 * 
 * Copyright (c) 2008 Landeshauptstadt M�nchen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
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
 * 13.09.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.view;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JPanel;

import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;

public abstract class LineView implements View
{
  /**
   * Farbe f�r den Hintergrund, wenn die View markiert ist.
   */
  protected static final Color MARKED_BACKGROUND_COLOR = Color.BLUE;

  /**
   * Breite des Randes um die View.
   */
  protected static final int BORDER = 4;

  /**
   * Das Panel, das alle Komponenten dieser View enth�lt.
   */
  protected JPanel myPanel;

  /**
   * Die Hintergrundfarbe im unmarkierten Zustand.
   */
  protected Color unmarkedBackgroundColor;

  /**
   * Der FormularMax4000, zu dem diese View geh�rt.
   */
  protected FormularMax4000 formularMax4000;

  public JComponent JComponent()
  {
    return myPanel;
  }

  /**
   * Markiert diese View optisch als ausgew�hlt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void mark()
  {
    myPanel.setBackground(MARKED_BACKGROUND_COLOR);
  }

  /**
   * Entfernt die optische Markierung als ausgew�hlt von dieser View.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void unmark()
  {
    myPanel.setBackground(unmarkedBackgroundColor);
  }

}
