/*
 * Dateiname: CoupledWindowController.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse steuert die Ankopplung von Fenstern an ein XTopWindow-Hauptfenster von OOo.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 22.01.2008 | LUT | Erstellung als CoupledWindowController
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;

import com.sun.star.awt.XTopWindow;
import com.sun.star.awt.XTopWindowListener;
import com.sun.star.lang.EventObject;

import de.muenchen.allg.afid.UNO;

/**
 * Diese Klasse steuert die logische Ankopplung von eigentlich unabh�ngigen
 * Fenstern an ein XTopWindow-Hauptfenster von OOo. Die angekoppelten Fenster
 * werden nur sichtbar, wenn das Hauptfenster den Fokus erh�lt. Verliert das
 * Hauptfenster oder ein angeoppeltes Fenster den Fokus an ein Fenster, das
 * nicht vom CoupledWindowController �berwacht wird, so werden alle
 * angekoppelten Fenster unsichtbar gestellt.
 * 
 * ACHTUNG: Die Windowmanager unter Windows und auf dem Basisclient verhalten
 * sich unterschiedlich und es werden unterschiedliche Ereignisse f�r gleiche
 * Aktionen generiert: z.B. erh�lt die Seriendruckleiste (das erste Beispiel
 * eines angekoppelten Fensters) auf dem Basisclient immer den Fokus, wenn
 * setCoupledWindowsVisible(true) aufgerufen wurde. Unter Windows wird das
 * Fenster zwar kurz aktiv, bekommt aber nicht den Fokus. Auch Situationen wie
 * das Schlie�en eines AWT-Fensters, das ein Parent-Window gesetzt hat, f�hren
 * zu unterschiedlichen Events auf dem Basisclient und unter Windows. Es ist
 * auch nicht gew�hrleistet, dass die Ereignisse Deaktivierung/Aktivierung beim
 * Fensterwechsel immer in der selben Reihenfolge eintreffen. Diese Klasse
 * enth�lt einen Stand, der nach langer Arbeit und Probiererei nun endlich in
 * den meisten F�llen funktioniert. Wenn hier etwas ge�ndert werden muss, dann
 * unbedingt unter Windows und auf dem Basisclient ausf�hrlich testen!!!
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class CoupledWindowController
{

  /**
   * Enth�lt alle mit addCoupledWindow registrierten angekoppelten Fenster.
   * Dieses Feld ist als ArrayList angelegt, damit gew�hrleistet ist, dass die
   * Fenster immer in der Reihenfolge der Registrierung sichtbar/unsichtbar
   * geschalten werden (also nicht in willk�rlicher Reihenfolge, die ein HashSet
   * mit sich bringen w�rde).
   */
  private ArrayList coupledWindows = new ArrayList();

  /**
   * Enth�lt den WindowStateWatcher, mit dem der Fensterstatus �berwacht und die
   * notwendigen Aktionen angestossen werden.
   */
  private WindowStateWatcher windowState = new WindowStateWatcher();

  /**
   * Der WindowStateWatcher �berwacht den Fensterstatus der angekoppelten
   * Fenster und st��t ggf. die notwendigen Aktionen an. Der WindowStateWatcher
   * enth�lt die Listener, mit denen das Hauptfenster und die angeoppelten
   * Fenster �berwacht werden und weiss immer bescheid, welches (registrierte)
   * Fenster aktuell den Fokus besitzt. Er veranlasst entsprechende Aktionen,
   * wenn das Hauptfenster den Fokus bekommt oder die Anwendung den Fokus an ein
   * fremdes Fenster abgibt.
   */
  private class WindowStateWatcher
  {
    /**
     * Wenn ein Fenster deaktiviert und nach DEACTIVATION_TIMEOUT Millisekunden
     * kein neues Fenster aktiviert wurde, dann werden alle angekoppelten
     * Fenster auf unsichtbar gestellt.
     */
    private static final int DEACTIVATION_TIMEOUT = 200;

    /**
     * Enth�lt das aktuell aktive Fenster oder null, wenn kein Fenster aktiv
     * ist.
     */
    private Object[] activeWindow = new Object[] { null };

    /**
     * Enth�lt eine eindeutige Nummer, die dem zuletzt gestarteten Warte-Thread
     * f�r ein TimeoutEvent �bergeben wurde.
     */
    private int lastValidTimeoutEvent = 0;

    /**
     * Nachdem alle angekoppelten Fenster unsichtbar gestellt wurden, wird
     * dieses Flag auf true gesetzt und gibt an, dass die Reaktivierung
     * ausschlie�lich durch des Hauptfenster veranlasst werden darf.
     */
    private boolean acceptMainWindowOnly = true;

    /**
     * Behandelt ein timeoutEvent, das beim Fokusverlust an ein fremdes Fenster
     * erzeugt wird. Ein Timeout-Event hat eine eindeutige Nummer, die mit
     * lastValideTimeoutEvent �bereinstimmen muss, damit das Event g�ltig ist
     * (dient dazu, damit ein bereits laufender Warte-Thread nicht abgew�rgt
     * werden muss, wenn bereits der n�chste - dann g�ltige - Warte-Thread
     * angestossen wurde). Ist kein aktives Fenster vorhanden, so werden alle
     * angekoppelten Fenster unsichtbar gemacht.
     * 
     * @param nr
     *          eindeutige Nummer des timeout-Events, die mit
     *          lastValidTimeoutEvent �bereinstimmen muss, damit das Event
     *          ausgef�hrt wird.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public void timeoutEvent(int nr)
    {
      synchronized (activeWindow)
      {
        if (nr != lastValidTimeoutEvent)
        {
          Logger.debug2("ignoriere ung�ltiges timeout event");
          return;
        }

        if (activeWindow[0] == null)
        {
          Logger
              .debug2("Timeout und kein aktives Fenster - stelle angekoppelte Fenster unsichtbar");
          setCoupledWindowsVisible(false);
          acceptMainWindowOnly = true;
        }
        else
        {
          Logger.debug2("Timeout aber keine Aktion da Fenster aktiv #"
                        + activeWindow[0].hashCode());
        }
      }
    }

    /**
     * Registriert die Aktivierung eines Fensters, das durch den Schl�ssel key
     * eindeutig beschrieben wird.
     * 
     * @param key
     *          der Schl�ssel des derzeit aktiven Fensters, mit dem das Fenster
     *          sp�ter deaktiviert werden kann.
     * @param isMainWindow
     *          Das Hauptfenster hat eine Sonderrolle, da nur die Aktivierung
     *          des Hauptfenster das acceptMainWindow-Flag zur�cksetzen darf.
     *          �ber diesen Parameter kann angegeben werden, ob das Fenster als
     *          Hauptfenster interpretiert gewertet werden soll.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public void activationEvent(Object key, boolean isMainWindow)
    {
      if (key == null) return;

      if (acceptMainWindowOnly && !isMainWindow)
      {
        Logger.debug2("Aktivierung ignoriert da Fenster kein Hauptfenster #"
                      + key.hashCode());
        return;
      }

      synchronized (activeWindow)
      {
        Object lastActiveWindow = activeWindow[0];
        activeWindow[0] = key;
        if (lastActiveWindow == null)
        {
          setCoupledWindowsVisible(true);
          acceptMainWindowOnly = false;
        }

        Logger.debug2("Aktivierung von Fenster #" + activeWindow[0].hashCode());
      }
    }

    /**
     * Registriert die Deaktivierung eines Fensters, das durch den Schl�ssel key
     * eindeutig beschrieben ist. Key muss dabei identisch mit dem Schl�ssel
     * sein, mit dem das Fenster aktiviert wurde, sonst wird auch nichts
     * unternommen. Eine bestimmte Zeit nach dem Deaktivieren des Fensters wird
     * ein Timeout-Event abgesetzt.
     * 
     * @param key
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public void deactivationEvent(Object key)
    {
      if (key == null) return;
      synchronized (activeWindow)
      {
        if (key.equals(activeWindow[0]))
        {
          Logger.debug2("Deaktivierung von Fenster #" + key.hashCode());
          activeWindow[0] = null;
          startWaitForTimeout();
        }
        else
        {
          Logger.debug2("Deaktierung ignoriert, da Fenster nicht aktiv #"
                        + key.hashCode());
        }
      }
    }

    /**
     * Erzeugt nach einer bestimmten Zeit ein Timeout-Event �ber das die
     * angekoppelten Fenster unsichtbar geschalten werden, wenn bei Ausf�hrung
     * des Timeout-Events kein Fenster aktiv ist.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    private void startWaitForTimeout()
    {
      final int nr = ++lastValidTimeoutEvent;
      Thread t = new Thread()
      {
        public void run()
        {
          try
          {
            Thread.sleep(DEACTIVATION_TIMEOUT);
          }
          catch (InterruptedException e)
          {
          }
          timeoutEvent(nr);
        }
      };
      t.setDaemon(true);
      t.start();
    }

    /**
     * Enth�lt den WindowListener der das Hauptfenster �berwacht.
     */
    private final XTopWindowListener topWindowListener = new XTopWindowListener()
    {
      public void windowDeactivated(EventObject arg0)
      {
        windowState.deactivationEvent(UNO.XInterface(arg0.Source));
      }

      public void windowActivated(EventObject arg0)
      {
        windowState.activationEvent(UNO.XInterface(arg0.Source), true);
      }

      public void windowNormalized(EventObject arg0)
      {
        // nicht relevant
      }

      public void windowMinimized(EventObject arg0)
      {
        synchronized (activeWindow)
        {
          activeWindow[0] = null;
          acceptMainWindowOnly = true;
        }
        setCoupledWindowsVisible(false);
      }

      public void windowClosed(EventObject arg0)
      {
        // nicht relevant
      }

      public void windowClosing(EventObject arg0)
      {
        // nicht relevant
      }

      public void windowOpened(EventObject arg0)
      {
        // nicht relevant
      }

      public void disposing(EventObject arg0)
      {
        // nicht relevant
      }
    };

    /**
     * Enth�lt den WindowListener mit dem angekoppelte Fenster �berwacht werden
     */
    private final WindowListener coupledWindowListener = new WindowListener()
    {
      public void windowActivated(WindowEvent e)
      {
        windowState.activationEvent(e.getSource(), false);
      }

      public void windowClosed(WindowEvent e)
      {
        // nicht relevant
      }

      public void windowClosing(WindowEvent e)
      {
        // nicht relevant
      }

      public void windowDeactivated(WindowEvent e)
      {
        // Deaktivierungsevent ignorieren, wenn die Aktivierung an ein
        // Unterfenster eines registrierten Fensters abgegeben wurde. Dies wird
        // �ber eine R�ckw�rtssuche in der Owner-Hierarchie des OppositeWindows
        // festgestellt werden.
        Window w = e.getOppositeWindow();
        while (w != null)
        {
          for (Iterator iter = coupledWindows.iterator(); iter.hasNext();)
          {
            CoupledWindow win = (CoupledWindow) iter.next();
            if (win.equals(w)) return;
          }
          w = w.getOwner();
        }

        // Deaktivierungsevent weiterreichen
        windowState.deactivationEvent(e.getSource());
      }

      public void windowDeiconified(WindowEvent e)
      {
        // nicht relevant
      }

      public void windowIconified(WindowEvent e)
      {
        // nicht relevant
      }

      public void windowOpened(WindowEvent e)
      {
        // nicht relevant
      }
    };
  }

  /**
   * Koppelt das AWT-Window window an das Hauptfenster an. Die Methode muss
   * aufgerufen werden, solange das Fenster window unsichtbar und nicht aktiv
   * ist (also z.B. vor dem Aufruf von window.setVisible(true)).
   * 
   * @param window
   *          das Fenster, das an das Hauptfenster angekoppelt werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void addCoupledWindow(Window window)
  {
    if (window == null) return;
    CoupledWindow toAdd = new CoupledAWTWindow(window);
    Logger.debug2("addCoupledWindow #" + toAdd.hashCode());
    toAdd.addWindowListener(windowState.coupledWindowListener);
    coupledWindows.add(toAdd);
  }

  /**
   * L�st die Bindung eines angekoppelten Fensters window an das Hauptfenster.
   * 
   * @param window
   *          das Fenster, dessen Bindung zum Hauptfenster gel�st werden soll.
   *          Ist das Fenster nicht angekoppelt, dann passiert nichts.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void removeCoupledWindow(Window window)
  {
    if (window == null) return;
    CoupledWindow toRemove = new CoupledAWTWindow(window);
    Logger.debug2("removeCoupledWindow #" + toRemove.hashCode());
    for (Iterator iter = coupledWindows.iterator(); iter.hasNext();)
    {
      CoupledWindow w = (CoupledWindow) iter.next();
      if (w.equals(toRemove))
      {
        iter.remove();
        toRemove.removeWindowListener(windowState.coupledWindowListener);
      }
    }
    windowState.deactivationEvent(window);
  }

  /**
   * Diese Methode macht alle angekoppelten Fenster sichtbar oder unsichtbar.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void setCoupledWindowsVisible(boolean visible)
  {
    for (Iterator iter = coupledWindows.iterator(); iter.hasNext();)
    {
      CoupledWindow win = (CoupledWindow) iter.next();
      win.setVisible(visible);
    }
  }

  /**
   * Registriert ein Hauptfenster in diesem CoupledWindowController und sollte
   * immer vor der Benutzung des Controllers aufgerufen werden.
   * 
   * @param w
   *          Das XTopWindow welches das entsprechende Hauptfenster ist.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void setTopWindow(XTopWindow w)
  {
    if (w == null) return;
    w.addTopWindowListener(windowState.topWindowListener);
    windowState.activationEvent(UNO.XInterface(w), true);
  }

  /**
   * Deregistriert ein Hauptfenster und sollte aufgerufen werden, wenn der
   * Controller nicht mehr ben�tigt wird und aufger�umt werden kann.
   * 
   * @param w
   *          Das XTopWindow welches fr�her als Hauptfenster diente.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void unsetTopWindow(XTopWindow w)
  {
    if (w == null) return;
    w.removeTopWindowListener(windowState.topWindowListener);
    windowState.deactivationEvent(UNO.XInterface(w));
  }

  /**
   * Gibt an, ob in diesem CoupledWindowController angekoppelte Fenster
   * registriert wurden.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public boolean hasCoupledWindows()
  {
    return coupledWindows.size() > 0;
  }

  /**
   * Beschreibt ein beliebiges ankoppelbares Fenster.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private interface CoupledWindow
  {
    /**
     * Setzt das angekoppelte Fenster auf sichtbar oder unsichtbar und kann
     * dabei auch den Fokus erhalten (Das ist eine unsch�ner Nebeneffekt der
     * AWT-Methode setVisible(...), der hier ber�cksichtigt ist). �ndert visible
     * nicht den Sichtbarkeitsstatus des Fenster, so hat diese Methode keine
     * Auswirkung.
     * 
     * @param visible
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public void setVisible(boolean visible);

    /**
     * Registriert auf dem angekoppelten Fenster einen WindowListener �ber den
     * das Hauptfenster mitkriegen kann, dass ein angekoppeltes Fenster den
     * Fokus an ein fremdes, nicht angekoppeles, Fenster verloren hat und somit
     * auch alle angekoppelten Fenster unsichtbar gestellt werden sollen.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     * @param listener
     */
    public void addWindowListener(WindowListener listener);

    /**
     * Entfernt einen mit addFocusListener(...) registrierten WindowListener vom
     * angkoppelten Fenster.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     * @param listener
     */
    public void removeWindowListener(WindowListener listener);
  }

  /**
   * Diese Klasse repr�sentiert ein CoupledWindow-Objekt, dem ein
   * java.awt.Window-Objekt zugrundeliegt und implementiert die Methoden
   * hashCode() und equals() damit das Objekt sinnvoll verglichen und in einer
   * HashMap verwaltet werden kann.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static class CoupledAWTWindow implements CoupledWindow
  {
    private Window window;

    public CoupledAWTWindow(Window window)
    {
      this.window = window;
    }

    public void setVisible(final boolean visible)
    {
      if (window.isVisible() != visible) window.setVisible(visible);
    }

    public void addWindowListener(final WindowListener l)
    {
      try
      {
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            try
            {
              window.addWindowListener(l);
            }
            catch (Exception x)
            {
            }
          }
        });
      }
      catch (Exception x)
      {
      }
    }

    public void removeWindowListener(final WindowListener l)
    {
      try
      {
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            try
            {
              window.removeWindowListener(l);
            }
            catch (Exception x)
            {
            }
          }
        });
      }
      catch (Exception x)
      {
      }
    }

    public int hashCode()
    {
      return window.hashCode();
    }

    public boolean equals(Object o)
    {
      if (o instanceof CoupledAWTWindow)
      {
        CoupledAWTWindow w = (CoupledAWTWindow) o;
        return window.equals(w.window);
      }
      else if (o instanceof Window)
      {
        Window w = (Window) o;
        return window.equals(w);
      }
      else
        return false;
    }
  }
}
