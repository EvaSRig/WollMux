/*
 * Dateiname: SyncActionListener.java
 * Projekt  : WollMux
 * Funktion : Vereinfacht die Synchronisation verschiedener Threads mittels 
 *            ActionListener
 * 
 * Copyright (c) 2009 Landeshauptstadt M�nchen
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
 * 24.09.2009 | LUT | Erstellung als SyncActionListener.java
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Diese Klasse vereinfacht die Synchronisation verschiedener Threads �ber einen
 * ActionListener. Die Anwendung erfolgt in der Regel in folgenden Schritten:
 * 
 * SyncActionListener s = new SyncActionListener();
 * aufrufEinerMethodeDieEinenActionListenerErwartet(..., s); EventObject result =
 * s.synchronize();
 * 
 * Es ist sicher gestellt, dass s.synchronize() erst zur�ck kehrt, wenn der
 * ActionListener benachrichtigt wurde. Dabei wird das EventObject zur�ck gegeben,
 * mit dem {@link #actionPerformed(ActionEvent)} des Listeners aufgerufen wurde.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class SyncActionListener implements ActionListener
{
  /**
   * Das lock-Flag �ber das die Synchronisierung erfolgt.
   */
  private boolean[] lock = new boolean[] { true };

  /**
   * Enth�lt nach erfolgter Syncronisierung das zur�ckgegebene ActionEvent
   */
  private ActionEvent result = null;

  /**
   * Kehrt erst zur�ck, wenn {@link #actionPerformed(ActionEvent)} des Listeners
   * aufgerufen wurde und liefert das dabei �bermittelte EventObject zur�ck.
   */
  public ActionEvent synchronize()
  {
    try
    {
      synchronized (lock)
      {
        while (lock[0] == true)
          lock.wait();
      }
    }
    catch (InterruptedException e)
    {}
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent arg0)
  {
    result = arg0;
    synchronized (lock)
    {
      lock[0] = false;
      lock.notifyAll();
    }
  }
}
