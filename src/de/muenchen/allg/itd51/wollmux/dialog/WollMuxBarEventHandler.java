/*
* Dateiname: WollMuxBarEventHandler.java
* Projekt  : WollMux
* Funktion : Dient der thread-safen Kommunikation der WollMuxBar mit dem WollMux im OOo.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 18.04.2006 | BNK | Erstellung
* 21.04.2006 | BNK | Vern�nftige Meldung wenn keine Verbindung zum OOo WOllMux hergestellt werden konnte
* 24.04.2006 | BNK | kleinere Aufr�umarbeiten. Code Review.
* 24.04.2006 | BNK | [R1390]Popup-Fenster, wenn Verbindung zu OOo WollMux nicht hergestellt
*                  | werden konnte.
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.LinkedList;
import java.util.List;

import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.frame.XDispatch;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.XPALProvider;
import de.muenchen.allg.itd51.wollmux.XWollMux;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;

/**
 * Dient der thread-safen Kommunikation der WollMuxBar mit dem WollMux im OOo.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class WollMuxBarEventHandler
{
  /**
   * Die Event-Queue.
   */
  private List eventQueue = new LinkedList();
  
  /**
   * Die WollMuxBar, f�r die Events behandelt werden.
   */
  private WollMuxBar wollmuxbar;
  
  /**
   * Der WollMux-Service, mit dem die WollMuxBar Informationen austauscht.
   * Der WollMux-Service sollte nicht �ber dieses Feld, sondern ausschlie�lich �ber
   * die Methode getRemoteWollMux bezogen werden, da diese mit einem m�glichen
   * Schlie�en von OOo w�hrend die WollMuxBar l�uft klarkommt.
   */
  private Object remoteWollMux;
  
  /**
   * Dieses Objekt wird beim WollMux als {@link XPALChangeEventListener} registriert. 
   */
  private XPALChangeEventListener myPALChangeEventListener;
  
  /**
   * Der Thread, der die Events abarbeitet.
   */
  private Thread myThread;
  
  /**
   * Startet die Event-Verarbeitung.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public WollMuxBarEventHandler(WollMuxBar wollmuxbar)
  {
    myPALChangeEventListener = new MyPALChangeEventListener();
    this.wollmuxbar = wollmuxbar;
    
    myThread = new EventProcessor();
    /*
     * Weil wir den Terminate-Event ausf�hren m�ssen, um uns korrekt vom
     * WollMux abzumelden d�rfen wir kein Daemon sein.
     */
    myThread.setDaemon(false); 
    myThread.start();
  }
  
  /**
   * Wartet, bis der Event-bearbeitende Thread sich beendet hat.
   * ACHTUNG! Der Thread beendet sich erst, wenn ein 
   * handleTerminate() abgesetzt wurde. Wird kein handleTerminate()
   * abgesetzt, dann wartet diese Methode endlos.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public void waitForThreadTermination()
  {
    try{
      myThread.join();
    }catch(Exception x) {Logger.error(x);}
  }
  
  /**
   * Startet OOo falls noetig und stellt Kontakt mit dem WollMux her, um
   * �ber den aktuellen Senderbox-Inhalt informiert zu werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void connectWithWollMux()
  {
    handle(new WollMuxConnectionEvent());
  }
  
  /**
   * Erzeugt eine wollmuxUrl und �bergibt sie dem WollMux zur Bearbeitung.
   *  
   * @param dispatchCmd das Kommando, das der WollMux ausf�hren soll. (z.B. "openTemplate")
   * @param arg ein optionales Argument (z.B. "{fragid}"). Ist das Argument null oder
   *        der Leerstring, so wird es nicht mit �bergeben.
   */
  public void handleWollMuxUrl(String dispatchCmd, String arg)
  {
    handle(new WollMuxUrlEvent(dispatchCmd, arg));
  }
  
  /**
   * L�sst die Senderboxes sich updaten.
   * @param entries die Eintr�ge der PAL
   * @param current der ausgew�hlte Eintrag
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void handleSenderboxUpdate(String[] entries, String current)
  {
    handle(new SenderboxUpdateEvent(entries, current));
  }
  
  /**
   * Teilt dem WollMux mit, dass PAL Eintrag entry gew�hlt wurde, der der
   * index-te Eintrag der PAL (gez�hlt ab 0) ist.
   * Es werden sowohl der Eintrag als auch der Index �bergeben, damit der WollMux
   * auf Konsistenz pr�fen kann. Schlie�lich ist es m�glich, dass in der
   * Zwischenzeit konkurrierende �nderungen der Senderbox stattgefunden haben.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void handleSelectPALEntry(String entry, int index)
  {
    handle(new SelectPALEntryEvent(entry, index));
  }
  
  /**
   * L�sst den EventHandler sich ordnungsgem�� deinitialisieren und seine 
   * Verbindung zum entfernten WollMux l�sen, sowie seinen Bearbeitungsthread
   * beenden. Achtung! Es sollte die Methode waitForThreadTermination() verwendet 
   * werden, bevor mit System.exit() die JVM beendet wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void handleTerminate()
  {
    handle(new TerminateEvent());
  }
  
  /**
   * Schiebt Event e in die Queue.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void handle(Event e)
  {
    if (WollMuxFiles.isDebugMode())
      Logger.debug2("F�ge "+e.getClass().getSimpleName()+" zur Event-Queue hinzu");
    synchronized (eventQueue)
    {
      eventQueue.add(e);
      eventQueue.notifyAll();
    }
  }

  /**
   * Interface f�r die Events, die dieser EventHandler abarbeitet.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private interface Event
  {
    public void process();
  }
  
  private class WollMuxUrlEvent implements Event
  {
    private String url;
    
    public WollMuxUrlEvent(String dispatchCmd, String arg) 
    {
      if(arg != null && !arg.equals("")) 
        arg = "#" + arg;
      else 
        arg = "";
      
      url = WollMux.wollmuxProtocol + ":" + dispatchCmd + arg;
    }
    
    public void process()
    {
      XDispatch disp = null;
      disp = (XDispatch) UnoRuntime.queryInterface(XDispatch.class, getRemoteWollMux(true));
      if(disp != null) {
        com.sun.star.util.URL dispatchUrl = new com.sun.star.util.URL();
        dispatchUrl.Complete = url;
        disp.dispatch(dispatchUrl, new PropertyValue[] {});
      }
    }
  }
  
  private class SenderboxUpdateEvent implements Event
  {
    private String[] palEntries;
    private String selectedEntry;
    
    public SenderboxUpdateEvent(String[] entries, String current)
    {
      palEntries = entries;
      selectedEntry = current; 
    }

    public void process()
    {
//    GUI-Funktionen im Event-Dispatching Thread ausf�hren wg. Thread-Safety.
      try{
        final WollMuxBar wmbar = wollmuxbar;
        final String[] entries = palEntries;
        final String selected = selectedEntry;
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            wmbar.updateSenderboxes(entries, selected);
          }
        });
      }
      catch(Exception x) {Logger.error(x);}
    }
  }
  
  private class SelectPALEntryEvent implements Event
  {
    private String entry;
    private int index;
    
    public SelectPALEntryEvent(String entry, int index)
    {
      this.entry = entry;
      this.index = index;
    }
    
    public void process()
    {
      XWollMux mux = getRemoteWollMux(true);
      if (mux != null)
        mux.setCurrentSender(entry, (short)index);
    }
  }
  
  private class TerminateEvent implements Event
  {
    public void process()
    {
      XWollMux mux = getRemoteWollMux(false);
      if (mux != null)
        mux.removePALChangeEventListener(myPALChangeEventListener);
    }
  }
  
  private class WollMuxConnectionEvent implements Event
  {
    public void process()
    {
      getRemoteWollMux(true);
    }
  }

  private class MyPALChangeEventListener implements XPALChangeEventListener
  {
    public void updateContent(EventObject eventObject)
    {
      XPALProvider palProv = (XPALProvider) UnoRuntime.queryInterface(
          XPALProvider.class,eventObject.Source);
      if (palProv != null)
      {
        try{
          String[] entries = palProv.getPALEntries();
          String current = palProv.getCurrentSender();
          String[] entriesCopy = new String[entries.length];
          System.arraycopy(entries, 0, entriesCopy, 0, entries.length);
          handleSenderboxUpdate(entriesCopy, current);
        } catch(Exception x)
        {
          Logger.error(x);
        }
      }
    }

    /**
     * Wird zur Zeit (2006-04-19) nicht verwendet.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void disposing(EventObject arg0)
    {}
  }
  
  /**
   * Der Thread, der die Events verarbeitet. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class EventProcessor extends Thread
  {
    public void run()
    {
      while(true)
      {
        try{
          Event e;
          synchronized(eventQueue)
          {
            while (eventQueue.isEmpty()) {eventQueue.wait();}
            e = (Event)eventQueue.remove(0);
          }
          
          processEvent(e);
          if (e instanceof TerminateEvent) 
          {
            Logger.debug("WollMuxBarEventHandler terminating");
            return;
          }
        }catch (InterruptedException e)
        {
          Logger.error(e);
        }
      }
    }
    
    private void processEvent(Event e)
    {
      try{
        e.process();
      } catch(Exception x)
      {
        Logger.error(x);
      }
    }
  }
  
  /**
   * Diese Methode liefert eine Instanz auf den entfernten WollMux zur�ck, wobei
   * der connect-Parameter steuert, ob falls notwendig eine neue UNO-Verbindung
   * aufgebaut wird.
   * 
   * @param connect Die Methode versucht immer zuerst, eine bestehende Verbindung
   * mit OOo zu verwenden, um das WollMux-Objekt zu bekommen. Der Parameter connect
   * steuert das Verhalten, falls entweder bisher keine Verbindung mit OOo
   * hergestellt wurde oder die Verbindung abgerissen ist. Falls connect == false,
   * so wird in diesen F�llen null zur�ckgeliefert ohne dass versucht wird, eine
   * neue Verbindung aufzubauen. Falls connect == true, so wird versucht, eine
   * neue Verbindung aufzubauen.
   * 
   * @return Instanz eines g�ltigen WollMux.  Konnte oder sollte keine Verbindung 
   * hergestellt werden, so wird null zur�ckgeliefert. 
   * TESTED 
   */
  private XWollMux getRemoteWollMux(boolean connect) 
  {
    if(remoteWollMux != null) 
    {
      try {
        return (XWollMux) UnoRuntime.queryInterface(XWollMux.class, remoteWollMux);
      } catch (DisposedException e) 
      {
        remoteWollMux = null;
      }
    }
    
    if(connect)
    {
      try
      {
        XComponentContext ctx = Bootstrap.bootstrap();
        XMultiServiceFactory factory = (XMultiServiceFactory) UnoRuntime.queryInterface(XMultiServiceFactory.class, ctx.getServiceManager());
        remoteWollMux = factory.createInstance("de.muenchen.allg.itd51.wollmux.WollMux");
        XWollMux mux = (XWollMux) UnoRuntime.queryInterface(XWollMux.class, remoteWollMux);
        mux.addPALChangeEventListener(myPALChangeEventListener);
        return mux;
      } 
      catch (Exception e) 
      { 
        Logger.error("Konnte keine Verbindung zum WollMux-Modul in OpenOffice herstellen");
        try{
          final WollMuxBar wmbar = wollmuxbar;
          javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              wmbar.connectionFailedWarning();
            }
          });
        }
        catch(Exception x) {}
      }
    }
    
    return null;
  }
}


