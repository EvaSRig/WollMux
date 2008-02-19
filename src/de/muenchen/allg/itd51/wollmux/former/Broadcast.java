//TODO L.m()
/*
* Dateiname: Broadcast.java
* Projekt  : WollMux
* Funktion : Interface f�r Nachrichten auf dem globalen Broadcast-Kanal
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 04.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

/**
 * Interface f�r Nachrichten auf dem globalen Broadcast-Kanal. 
 * Siehe auch {@link de.muenchen.allg.itd51.wollmux.former.BroadcastListener}.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Broadcast
{
  /**
   * Sendet diese Broadcast-Nachricht an listener.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void sendTo(BroadcastListener listener);
}
