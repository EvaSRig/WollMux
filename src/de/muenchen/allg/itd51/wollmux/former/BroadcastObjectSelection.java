/*
 * Dateiname: BroadcastObjectSelection.java
 * Projekt  : WollMux
 * Funktion : Nachricht, dass in einer View ein Objekt ausgew�hlt wurde.
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
 * 29.09.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former;

/**
 * Nachricht, dass in einer View ein Object ausgew�hlt wurde. Diese Nachricht wird
 * von anderen Views ausgewertet, um ihre Selektionen ebenfalls anzupassen.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class BroadcastObjectSelection implements Broadcast
{
  /**
   * das {@link Object} das ausgew�hlt wurde.
   */
  private Object myObject;

  /**
   * -1 => abw�hlen, 1 => anw�hlen, 0: toggle.
   */
  private int state;

  /**
   * true => Selektion erst ganz l�schen vor an/abw�hlen des Objektes.
   */
  private boolean clearSelection;

  /**
   * Erzeugt eine neue Nachricht.
   * 
   * @param myObject
   *          das {@link Object} das ausgew�hlt wurde.
   * @param state
   *          -1 => abw�hlen, 1 => anw�hlen, 0: toggle
   * @param clearSelection
   *          true => Selektion erst ganz l�schen vor an/abw�hlen von myObject.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public BroadcastObjectSelection(Object model, int state, boolean clearSelection)
  {
    this.myObject = model;
    this.state = state;
    this.clearSelection = clearSelection;
  }

  /**
   * Liefert -1 f�r abw�hlen, 1 f�r ausw�hlen, 0 f�r toggle.
   * 
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int getState()
  {
    return state;
  }

  /**
   * true => Selektion erst ganz l�schen vor an/abw�hlen des Objekts.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean getClearSelection()
  {
    return clearSelection;
  }

  /**
   * Liefert das Objekt zur�ck, das de/selektiert wurde.
   */
  public Object getObject()
  {
    return myObject;
  }

}
