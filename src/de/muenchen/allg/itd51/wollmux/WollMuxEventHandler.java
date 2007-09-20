/*
 * Dateiname: WollMuxEventHandler.java
 * Projekt  : WollMux
 * Funktion : Erm�glicht die Einstellung neuer WollMuxEvents in die EventQueue.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 24.10.2005 | LUT | Erstellung als EventHandler.java
 * 01.12.2005 | BNK | +on_unload() das die Toolbar neu erzeugt (b�ser Hack zum 
 *                  | Beheben des Seitenansicht-Toolbar-Verschwindibus-Problems)
 *                  | Ausgabe des hashCode()s in den Debug-Meldungen, um Events 
 *                  | Objekten zuordnen zu k�nnen beim Lesen des Logfiles
 * 27.03.2005 | LUT | neues Kommando openDocument
 * 21.04.2006 | LUT | +ConfigurationErrorException statt NodeNotFoundException bei
 *                    fehlendem URL-Attribut in Textfragmenten
 * 06.06.2006 | LUT | + Abl�sung der Event-Klasse durch saubere Objektstruktur
 *                    + �berarbeitung vieler Fehlermeldungen
 *                    + Zeilenumbr�che in showInfoModal, damit keine unlesbaren
 *                      Fehlermeldungen mehr ausgegeben werden.
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XNamed;
import com.sun.star.form.binding.InvalidBindingStateException;
import com.sun.star.frame.DispatchResultEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFrames;
import com.sun.star.frame.XNotifyingDispatch;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;
import com.sun.star.uno.RuntimeException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XStringSubstitution;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.FormModelImpl.InvalidFormDescriptorException;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel.PrintFailedException;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton.InvalidIdentifierException;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.dialog.AbsenderAuswaehlen;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.dialog.PersoenlicheAbsenderlisteVerwalten;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.PrintFunction;
import de.muenchen.allg.itd51.wollmux.func.StandardPrint;

/**
 * Erm�glicht die Einstellung neuer WollMuxEvents in die EventQueue.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class WollMuxEventHandler
{
  /**
   * Mit dieser Methode ist es m�glich die Entgegennahme von Events zu
   * blockieren. Alle eingehenden Events werden ignoriert, wenn accept auf false
   * gesetzt ist und entgegengenommen, wenn accept auf true gesetzt ist.
   * 
   * @param accept
   */
  public static void setAcceptEvents(boolean accept)
  {
    EventProcessor.getInstance().setAcceptEvents(accept);
  }

  /**
   * Der EventProcessor sorgt f�r eine synchronisierte Verarbeitung aller
   * Wollmux-Events. Alle Events werden in eine synchronisierte eventQueue
   * hineingepackt und von einem einzigen eventProcessingThread sequentiell
   * abgearbeitet.
   * 
   * @author lut
   */
  public static class EventProcessor
  {
    /**
     * Gibt an, ob der EventProcessor �berhaupt events entgegennimmt. Ist
     * acceptEvents=false, werden alle Events ignoriert.
     */
    private boolean acceptEvents = false;

    private List eventQueue = new LinkedList();

    private static EventProcessor singletonInstance;

    private static Thread eventProcessorThread;

    private static EventProcessor getInstance()
    {
      if (singletonInstance == null) singletonInstance = new EventProcessor();
      return singletonInstance;
    }

    /**
     * Mit dieser Methode ist es m�glich die Entgegennahme von Events zu
     * blockieren. Alle eingehenden Events werden ignoriert, wenn accept auf
     * false gesetzt ist und entgegengenommen, wenn accept auf true gesetzt ist.
     * 
     * @param accept
     */
    private void setAcceptEvents(boolean accept)
    {
      acceptEvents = accept;
      if (accept)
        Logger.debug("EventProcessor: akzeptiere neue Events.");
      else
        Logger.debug("EventProcessor: blockiere Entgegennahme von Events!");
    }

    private EventProcessor()
    {
      // starte den eventProcessorThread
      eventProcessorThread = new Thread(new Runnable()
      {
        public void run()
        {
          Logger.debug("Starte EventProcessor-Thread");
          try
          {
            while (true)
            {
              WollMuxEvent event;
              synchronized (eventQueue)
              {
                while (eventQueue.isEmpty())
                  eventQueue.wait();
                event = (WollMuxEvent) eventQueue.remove(0);
              }

              event.process();
            }
          }
          catch (InterruptedException e)
          {
            Logger.error("EventProcessor-Thread wurde unterbrochen:");
            Logger.error(e);
          }
          Logger.debug("Beende EventProcessor-Thread");
        }
      });
      eventProcessorThread.start();
    }

    /**
     * Diese Methode f�gt ein Event an die eventQueue an wenn der WollMux
     * erfolgreich initialisiert wurde und damit events akzeptieren darf.
     * Anschliessend weckt sie den EventProcessor-Thread.
     * 
     * @param event
     */
    private void addEvent(WollMuxEventHandler.WollMuxEvent event)
    {
      if (acceptEvents) synchronized (eventQueue)
      {
        eventQueue.add(event);
        eventQueue.notifyAll();
      }
    }
  }

  /**
   * Interface f�r die Events, die dieser EventHandler abarbeitet.
   */
  public interface WollMuxEvent
  {
    /**
     * Startet die Ausf�hrung des Events und darf nur aus dem EventProcessor
     * aufgerufen werden.
     */
    public void process();

    /**
     * Gibt an, ob das Event eine Referenz auf das Objekt o, welches auch ein
     * UNO-Service sein kann, enth�lt.
     */
    public boolean requires(Object o);
  }

  /**
   * Repr�sentiert einen Fehler, der benutzersichtbar in einem Fehlerdialog
   * angezeigt wird.
   * 
   * @author christoph.lutz
   */
  private static class WollMuxFehlerException extends java.lang.Exception
  {
    private static final long serialVersionUID = 3618646713098791791L;

    public WollMuxFehlerException(String msg)
    {
      super(msg);
    }

    public WollMuxFehlerException(String msg, java.lang.Exception e)
    {
      super(msg, e);
    }
  }

  private static class CantStartDialogException extends WollMuxFehlerException
  {
    private static final long serialVersionUID = -1130975078605219254L;

    public CantStartDialogException(java.lang.Exception e)
    {
      super("Der Dialog konnte nicht gestartet werden!\n\n"
            + "Bitte kontaktieren Sie Ihre Systemadministration.", e);
    }
  }

  /**
   * Dient als Basisklasse f�r konkrete Event-Implementierungen.
   */
  private static class BasicEvent implements WollMuxEvent
  {

    private boolean[] lock = new boolean[] { true };

    /**
     * Diese Method ist f�r die Ausf�hrung des Events zust�ndig. Nach der
     * Bearbeitung entscheidet der R�ckgabewert ob unmittelbar die Bearbeitung
     * des n�chsten Events gestartet werden soll oder ob das GUI blockiert
     * werden soll bis das n�chste actionPerformed-Event beim EventProcessor
     * eintrifft.
     */
    public void process()
    {
      Logger.debug("Process WollMuxEvent " + this.toString());
      try
      {
        doit();
      }
      catch (WollMuxFehlerException e)
      {
        errorMessage(e);
      }
      // Notnagel f�r alle Runtime-Exceptions.
      catch (Throwable t)
      {
        Logger.error(t);
      }
    }

    /**
     * Logged die �bergebene Fehlermeldung nach Logger.error() und erzeugt ein
     * Dialogfenster mit der Fehlernachricht.
     */
    protected void errorMessage(Throwable t)
    {
      Logger.error(t);
      String msg = "";
      if (t.getMessage() != null) msg += t.getMessage();
      Throwable c = t.getCause();
      if (c != null)
      {
        msg += "\n\n" + c;
      }
      WollMuxSingleton.showInfoModal("WollMux-Fehler", msg);
    }

    /**
     * Jede abgeleitete Event-Klasse sollte die Methode doit redefinieren, in
     * der die eigentlich event-Bearbeitung erfolgt. Die Methode doit muss alle
     * auftretenden Exceptions selbst behandeln, Fehler die jedoch
     * benutzersichtbar in einem Dialog angezeigt werden sollen, k�nnen �ber
     * eine WollMuxFehlerException nach oben weitergereicht werden.
     */
    protected void doit() throws WollMuxFehlerException
    {
    };

    /**
     * Diese Methode kann am Ende einer doit()-Methode aufgerufen werden und
     * versucht die Absturzwahrscheinlichkeit von OOo/WollMux zu senken in dem
     * es den GarbageCollector der JavaVM triggert freien Speicher freizugeben.
     * Durch derartige Aufr�umaktionen insbesondere nach der Bearbeitung von
     * Events, die viel mit Dokumenten/Cursorn/Uno-Objekten interagieren, wird
     * die Stabilit�t des WollMux sp�rbar gesteigert.
     * 
     * In der Vergangenheit gab es z.B. sporadische, nicht immer reproduzierbare
     * Abst�rze von OOo, die vermutlich in einem fehlerhaften Speichermanagement
     * in der schwer zu durchschauenden Kette JVM->UNO-Proxies->OOo begr�ndet
     * waren.
     */
    protected void stabilize()
    {
      System.gc();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.WollMuxEventHandler.WollMuxEvent#requires(java.lang.Object)
     */
    public boolean requires(Object o)
    {
      return false;
    }

    public String toString()
    {
      return this.getClass().getSimpleName();
    }

    /**
     * Setzt den Enable-Status aller OOo-Fenster, die der desktop aktuell
     * liefert auf enabled. �ber den Status kann gesteuert werden, ob das
     * Fenster Benutzerinteraktionen wie z.B. Mausklicks auf Men�punkte oder
     * Tastendr�cke verarbeitet. Die Verarbeitung findet nicht statt, wenn
     * enabled==false gesetzt ist, ansonsten schon.
     * 
     * @param enabled
     */
    static void enableAllOOoWindows(boolean enabled)
    {
      try
      {
        XFrames frames = UNO.XFramesSupplier(UNO.desktop).getFrames();
        for (int i = 0; i < frames.getCount(); i++)
        {
          try
          {
            XFrame frame = UNO.XFrame(frames.getByIndex(i));
            XWindow contWin = frame.getContainerWindow();
            if (contWin != null) contWin.setEnable(enabled);
          }
          catch (java.lang.Exception e)
          {
          }
        }
      }
      catch (java.lang.Exception e)
      {
      }
    }

    /**
     * Setzt einen lock, der in Verbindung mit setUnlock und der
     * waitForUnlock-Methode verwendet werden kann, um quasi Modalit�t f�r nicht
     * modale Dialoge zu realisieren und setzt alle OOo-Fenster auf
     * enabled==false. setLock() sollte stets vor dem Aufruf des nicht modalen
     * Dialogs erfolgen, nach dem Aufruf des nicht modalen Dialogs folgt der
     * Aufruf der waitForUnlock()-Methode. Der nicht modale Dialog erzeugt bei
     * der Beendigung ein ActionEvent, das daf�r sorgt, dass setUnlock
     * aufgerufen wird.
     */
    protected void setLock()
    {
      enableAllOOoWindows(false);
      synchronized (lock)
      {
        lock[0] = true;
      }
    }

    /**
     * Macht einen mit setLock() gesetzten Lock r�ckg�ngig, und setzt alle
     * OOo-Fenster auf enabled==true und bricht damit eine evtl. wartende
     * waitForUnlock()-Methode ab.
     */
    protected void setUnlock()
    {
      synchronized (lock)
      {
        lock[0] = false;
        lock.notifyAll();
      }
      enableAllOOoWindows(true);
    }

    /**
     * Wartet so lange, bis der vorher mit setLock() gesetzt lock mit der
     * Methode setUnlock() aufgehoben wird. So kann die quasi Modalit�t nicht
     * modale Dialoge zu realisiert werden. setLock() sollte stets vor dem
     * Aufruf des nicht modalen Dialogs erfolgen, nach dem Aufruf des nicht
     * modalen Dialogs folgt der Aufruf der waitForUnlock()-Methode. Der nicht
     * modale Dialog erzeugt bei der Beendigung ein ActionEvent, das daf�r
     * sorgt, dass setUnlock aufgerufen wird.
     */
    protected void waitForUnlock()
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
      {
      }
    }

    /**
     * Dieser ActionListener kann nicht modalen Dialogen �bergeben werden und
     * sorgt in Verbindung mit den Methoden setLock() und waitForUnlock() daf�r,
     * dass quasi modale Dialoge realisiert werden k�nnen.
     */
    protected UnlockActionListener unlockActionListener = new UnlockActionListener();

    protected class UnlockActionListener implements ActionListener
    {
      public ActionEvent actionEvent = null;

      public void actionPerformed(ActionEvent arg0)
      {
        actionEvent = arg0;
        setUnlock();
      }
    }

    /**
     * Dieser DispatchResultListener kann der Methode
     * XNotifyableDispatch.dispatchWithNotification �bergeben werden und sorgt
     * in Verbindung mit den Methoden setLock() und waitForUnlock() daf�r, dass
     * ein Dispatch quasi synchron ausgef�hrt werden kann.
     */
    protected UnlockDispatchResultListener unlockDispatchResultListener = new UnlockDispatchResultListener();

    protected class UnlockDispatchResultListener implements
        XDispatchResultListener
    {
      public DispatchResultEvent resultEvent = null;

      public void disposing(EventObject arg0)
      {
        setUnlock();
      }

      public void dispatchFinished(DispatchResultEvent arg0)
      {
        resultEvent = arg0;
        setUnlock();
      }
    }
  }

  /**
   * Stellt das WollMuxEvent event in die EventQueue des EventProcessors.
   * 
   * @param event
   */
  private static void handle(WollMuxEvent event)
  {
    EventProcessor.getInstance().addEvent(event);
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den Dialog AbsenderAuswaehlen startet.
   */
  public static void handleShowDialogAbsenderAuswaehlen()
  {
    handle(new OnShowDialogAbsenderAuswaehlen());
  }

  /**
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) und aus dem
   * WollMuxEventHandler ausgel�st und sorgt daf�r, dass der Dialog
   * AbsenderAusw�hlen gestartet wird.
   * 
   * @author christoph.lutz
   */
  private static class OnShowDialogAbsenderAuswaehlen extends BasicEvent
  {
    protected void doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      ConfigThingy conf = mux.getWollmuxConf();

      // Konfiguration auslesen:
      ConfigThingy whoAmIconf;
      ConfigThingy PALconf;
      ConfigThingy ADBconf;
      try
      {
        whoAmIconf = requireLastSection(conf, "AbsenderAuswaehlen");
        PALconf = requireLastSection(conf, "PersoenlicheAbsenderliste");
        ADBconf = requireLastSection(conf, "AbsenderdatenBearbeiten");
      }
      catch (ConfigurationErrorException e)
      {
        throw new CantStartDialogException(e);
      }

      // Dialog modal starten:
      try
      {
        setLock();
        new AbsenderAuswaehlen(whoAmIconf, PALconf, ADBconf, mux
            .getDatasourceJoiner(), unlockActionListener);
        waitForUnlock();
      }
      catch (java.lang.Exception e)
      {
        throw new CantStartDialogException(e);
      }

      WollMuxEventHandler.handlePALChangedNotify();
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den Dialog
   * PersoenlichtAbsenderListe-Verwalten startet.
   */
  public static void handleShowDialogPersoenlicheAbsenderliste()
  {
    handle(new OnShowDialogPersoenlicheAbsenderlisteVerwalten());
  }

  /**
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) und aus dem
   * WollMuxEventHandler ausgel�st und sorgt daf�r, dass der Dialog
   * Pers�nlicheAbsendeliste-Verwalten gestartet wird.
   * 
   * @author christoph.lutz
   */
  private static class OnShowDialogPersoenlicheAbsenderlisteVerwalten extends
      BasicEvent
  {
    protected void doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();
      ConfigThingy conf = mux.getWollmuxConf();

      // Konfiguration auslesen:
      ConfigThingy PALconf;
      ConfigThingy ADBconf;
      try
      {
        PALconf = requireLastSection(conf, "PersoenlicheAbsenderliste");
        ADBconf = requireLastSection(conf, "AbsenderdatenBearbeiten");
      }
      catch (ConfigurationErrorException e)
      {
        throw new CantStartDialogException(e);
      }

      // Dialog modal starten:
      try
      {
        setLock();
        new PersoenlicheAbsenderlisteVerwalten(PALconf, ADBconf, mux
            .getDatasourceJoiner(), unlockActionListener);
        waitForUnlock();
      }
      catch (java.lang.Exception e)
      {
        throw new CantStartDialogException(e);
      }

      handlePALChangedNotify();
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den Funktionsdialog dialogName aufruft
   * und die zur�ckgelieferten Werte in die entsprechenden FormField-Objekte des
   * Dokuments doc eintr�gt.
   * 
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) und aus dem
   * WollMuxEventHandler ausgel�st.
   */
  public static void handleFunctionDialog(TextDocumentModel model,
      String dialogName)
  {
    handle(new OnFunctionDialog(model, dialogName));
  }

  private static class OnFunctionDialog extends BasicEvent
  {
    private TextDocumentModel model;

    private String dialogName;

    private OnFunctionDialog(TextDocumentModel model, String dialogName)
    {
      this.model = model;
      this.dialogName = dialogName;
    }

    protected void doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      // Dialog aus Funktionsdialog-Bibliothek holen:
      Dialog dialog = mux.getFunctionDialogs().get(dialogName);
      if (dialog == null)
        throw new WollMuxFehlerException("Funktionsdialog '"
                                         + dialogName
                                         + "' ist nicht definiert.");

      // Dialoginstanz erzeugen und modal anzeigen:
      Dialog dialogInst = null;
      try
      {
        dialogInst = dialog.instanceFor(new HashMap());

        setLock();
        dialogInst.show(unlockActionListener, mux.getGlobalFunctions(), mux
            .getFunctionDialogs());
        waitForUnlock();
      }
      catch (ConfigurationErrorException e)
      {
        throw new CantStartDialogException(e);
      }

      // Abbruch, wenn der Dialog nicht mit OK beendet wurde.
      String cmd = unlockActionListener.actionEvent.getActionCommand();
      if (!cmd.equalsIgnoreCase("select")) return;

      // Dem Dokument den Fokus geben, damit die �nderungen des Benutzers
      // transparent mit verfolgt werden k�nnen.
      try
      {
        model.getFrame().getContainerWindow().setFocus();
      }
      catch (java.lang.Exception e)
      {
        // keine Gef�hrdung des Ablaufs falls das nicht klappt.
      }

      // Alle Werte die der Funktionsdialog sicher zur�ck liefert werden in
      // das Dokument �bernommen.
      Collection schema = dialogInst.getSchema();
      Iterator iter = schema.iterator();
      while (iter.hasNext())
      {
        String id = (String) iter.next();
        String value = dialogInst.getData(id).toString();

        // Formularwerte ins Model uns ins Dokument �bernehmen.
        model.setFormFieldValue(id, value);
        model.updateFormFields(id);
      }

      stabilize();
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "("
             + model
             + ", '"
             + dialogName
             + "')";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das einen modalen Dialog anzeigt, der
   * wichtige Versionsinformationen �ber den WollMux, die Konfiguration und die
   * WollMuxBar (nur falls wollmuxBarVersion nicht der Leersting ist) enth�lt.
   * Anmerkung: das WollMux-Modul hat keine Ahnung, welche WollMuxBar verwendet
   * wird. Daher ist es m�glich, �ber den Parameter wollMuxBarVersion eine
   * Versionsnummer der WollMuxBar zu �bergeben, die im Dialog angezeigt wird,
   * falls wollMuxBarVersion nicht der Leerstring ist.
   * 
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) ausgel�st, wenn die
   * WollMux-url "wollmux:about" aufgerufen wurde.
   */
  public static void handleAbout(String wollMuxBarVersion)
  {
    handle(new OnAbout(wollMuxBarVersion));
  }

  private static class OnAbout extends BasicEvent
  {
    private String wollMuxBarVersion;

    private final URL WM_URL = this.getClass().getClassLoader().getResource(
        "data/wollmux_klein.jpg");

    private OnAbout(String wollMuxBarVersion)
    {
      this.wollMuxBarVersion = wollMuxBarVersion;
    }

    protected void doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      String str = "WollMux " + mux.getBuildInfo();
      if (wollMuxBarVersion != null && !wollMuxBarVersion.equals(""))
        str += "\nWollMux-Leiste " + wollMuxBarVersion;

      str += "\n\nWollMux-Konfiguration: " + mux.getConfVersionInfo();
      str += "\n" + WollMuxFiles.getDEFAULT_CONTEXT().toExternalForm();

      Common.setLookAndFeelOnce();

      ImageIcon icon = new ImageIcon(WM_URL);

      JOptionPane pane = new JOptionPane(str, JOptionPane.INFORMATION_MESSAGE,
          JOptionPane.DEFAULT_OPTION, icon);
      JDialog dialog = pane.createDialog(
          null,
          "Info �ber Vorlagen und Formulare (WollMux)");

      // Hintergrundfarbe aller Komponenten auf weiss setzen
      try
      {
        pane.setBackground(Color.WHITE);
        pane.getComponent(0).setBackground(Color.WHITE);
        pane.getComponent(1).setBackground(Color.WHITE);
      }
      catch (java.lang.Exception e)
      {
      }

      dialog.setAlwaysOnTop(true);
      dialog.setVisible(true); // Wartet bis OK Dialog beendet!
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "()";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den FormularMax4000 aufruft f�r das
   * Dokument doc.
   * 
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) und aus dem
   * WollMuxEventHandler ausgel�st.
   */
  public static void handleFormularMax4000Show(TextDocumentModel model)
  {
    handle(new OnFormularMax4000Show(model));
  }

  private static class OnFormularMax4000Show extends BasicEvent
  {
    private final TextDocumentModel model;

    private OnFormularMax4000Show(TextDocumentModel model)
    {
      this.model = model;
    }

    protected void doit() throws WollMuxFehlerException
    {
      // Bestehenden Max in den Vordergrund holen oder neuen Max erzeugen.
      FormularMax4000 max = model.getCurrentFormularMax4000();
      if (max != null)
      {
        max.toFront();
      }
      else
      {
        max = new FormularMax4000(model, new ActionListener()
        {
          public void actionPerformed(ActionEvent actionEvent)
          {
            if (actionEvent.getSource() instanceof FormularMax4000)
              WollMuxEventHandler.handleFormularMax4000Returned(model);
          }
        }, WollMuxSingleton.getInstance().getGlobalFunctions(),
            WollMuxSingleton.getInstance().getGlobalPrintFunctions());
        model.setCurrentFormularMax4000(max);
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + model + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das aufgerufen wird, wenn ein
   * FormularMax4000 beendet wird und die entsprechenden internen Referenzen
   * gel�scht werden k�nnen.
   * 
   * Dieses Event wird vom EventProcessor geworfen, wenn der FormularMax
   * zur�ckkehrt.
   */
  public static void handleFormularMax4000Returned(TextDocumentModel model)
  {
    handle(new OnFormularMax4000Returned(model));
  }

  private static class OnFormularMax4000Returned extends BasicEvent
  {
    private TextDocumentModel model;

    private OnFormularMax4000Returned(TextDocumentModel model)
    {
      this.model = model;
    }

    protected void doit() throws WollMuxFehlerException
    {
      model.setCurrentFormularMax4000(null);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + model.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das Auskunft dar�ber gibt, dass ein
   * TextDokument geschlossen wurde und damit auch das TextDocumentModel
   * disposed werden soll.
   * 
   * Dieses Event wird ausgel�st, wenn ein TextDokument geschlossen wird.
   */
  public static void handleTextDocumentClosed(XTextDocument doc)
  {
    handle(new OnTextDocumentClosed(doc));
  }

  private static class OnTextDocumentClosed extends BasicEvent
  {
    private XTextDocument doc;

    private OnTextDocumentClosed(XTextDocument doc)
    {
      this.doc = doc;
    }

    protected void doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      if (mux.hasTextDocumentModel(doc))
      {
        TextDocumentModel model = mux.getTextDocumentModel(doc);
        model.dispose();
      }

      stabilize();
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das die eigentliche Dokumentbearbeitung
   * eines TextDokuments startet.
   * 
   * @param xTextDoc
   *          Das XTextDocument, das durch den WollMux verarbeitet werden soll.
   */
  public static void handleProcessTextDocument(XTextDocument xTextDoc)
  {
    handle(new OnProcessTextDocument(xTextDoc));
  }

  /**
   * Dieses Event wird immer dann ausgel�st, wenn der GlobalEventBroadcaster von
   * OOo ein ON_NEW oder ein ON_LOAD-Event wirft. Das Event sorgt daf�r, dass
   * die eigentliche Dokumentbearbeitung durch den WollMux angestossen wird.
   * 
   * @author christoph.lutz
   */
  private static class OnProcessTextDocument extends BasicEvent
  {
    XTextDocument xTextDoc;

    public OnProcessTextDocument(XTextDocument xTextDoc)
    {
      this.xTextDoc = xTextDoc;
    }

    protected void doit() throws WollMuxFehlerException
    {
      if (xTextDoc == null) return;

      WollMuxSingleton mux = WollMuxSingleton.getInstance();
      TextDocumentModel model = mux.getTextDocumentModel(xTextDoc);

      // Konfigurationsabschnitt Textdokument verarbeiten:
      ConfigThingy tds = new ConfigThingy("Textdokument");
      try
      {
        tds = mux.getWollmuxConf().query("Fenster").query("Textdokument")
            .getLastChild();
        model.setWindowViewSettings(tds);
      }
      catch (NodeNotFoundException e)
      {
      }

      // M�gliche Aktionen f�r das neu ge�ffnete Dokument:
      DocumentCommandInterpreter dci = new DocumentCommandInterpreter(model,
          mux);

      try
      {
        // Dokumentkommandos setType, setPrintFunction, all, draftOnly und
        // notInOriginal auswerten.
        dci.scanGlobalDocumentCommands();

        // Bei Vorlagen: Ausf�hrung der Dokumentkommandos
        if (model.isTemplate())
        {
          dci.executeTemplateCommands();

          // manche Kommandos sind erst nach der Expansion verf�gbar
          dci.scanGlobalDocumentCommands();
        }

        // Bei Formularen:
        if (model.isFormDocument())
        {
          // Konfigurationsabschnitt Fenster/Formular verarbeiten
          try
          {
            model.setDocumentZoom(mux.getWollmuxConf().query("Fenster").query(
                "Formular").getLastChild().query("ZOOM"));
          }
          catch (java.lang.Exception e)
          {
          }

          // Ausf�hrung der Dokumentkommandos des Formularsystems
          dci.executeFormCommands();

          // FormGUI starten, falls es kein Teil eines Multiform-Dokuments ist.
          if (!model.isPartOfMultiformDocument())
          {
            FormModel fm;
            try
            {
              fm = FormModelImpl.createSingleDocumentFormModel(model);
            }
            catch (InvalidFormDescriptorException e)
            {
              throw new WMCommandsFailedException(
                  "Die Vorlage bzw. das Formular enth�lt keine g�ltige Formularbeschreibung\n\n"
                      + "Bitte kontaktieren Sie Ihre Systemadministration.");
            }

            model.setFormModel(fm);
            fm.startFormGUI();
          }
        }
      }
      catch (java.lang.Exception e)
      {
        throw new WollMuxFehlerException("Fehler bei der Dokumentbearbeitung.",
            e);
      }

      // ContextChanged ausl�sen, damit die Dispatches aktualisiert werden.
      try
      {
        model.getFrame().contextChanged();
      }
      catch (java.lang.Exception e)
      {
      }

      stabilize();
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + xTextDoc.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Wird wollmux:Open mit der Option FORMGUIS "merged" gestartet, so werden
   * zuerst die Einzeldokumente ge�ffnet und dann dieses Event aufgerufen, das
   * daf�r zust�ndig ist, die eine FormGUI f�r den MultiDocument-Modus zu
   * erzeugen und zu starten.
   * 
   * @param docs
   *          Ein Vector of TextDocumentModels, die in einem Multiformular
   *          zusammengefasst werden sollen.
   */
  public static void handleProcessMultiform(
      Vector /* of TextDocumentModel */docs, ConfigThingy buttonAnpassung)
  {
    handle(new OnProcessMultiform(docs, buttonAnpassung));
  }

  private static class OnProcessMultiform extends BasicEvent
  {
    Vector docs;

    ConfigThingy buttonAnpassung;

    public OnProcessMultiform(Vector /* of TextDocumentModel */docs,
        ConfigThingy buttonAnpassung)
    {
      this.docs = docs;
      this.buttonAnpassung = buttonAnpassung;
    }

    protected void doit() throws WollMuxFehlerException
    {
      FormModel fm;
      try
      {
        fm = FormModelImpl.createMultiDocumentFormModel(docs, buttonAnpassung);
      }
      catch (InvalidFormDescriptorException e)
      {
        throw new WollMuxFehlerException("Fehler bei der Dokumentbearbeitung.",
            new WMCommandsFailedException(
                "Die Vorlage bzw. das Formular enth�lt keine g�ltige Formularbeschreibung\n\n"
                    + "Bitte kontaktieren Sie Ihre Systemadministration."));
      }

      // FormModel in allen Dokumenten registrieren:
      for (Iterator iter = docs.iterator(); iter.hasNext();)
      {
        TextDocumentModel doc = (TextDocumentModel) iter.next();
        doc.setFormModel(fm);
      }

      // FormGUI starten:
      fm.startFormGUI();
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + docs + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das die Bearbeitung aller neu
   * hinzugekommener Dokumentkommandos �bernimmt.
   * 
   * Dieses Event wird immer dann ausgel�st, wenn nach dem �ffnen eines
   * Dokuments neue Dokumentkommandos eingef�gt wurden, die nun bearbeitet
   * werden sollen - z.B. wenn ein Textbaustein eingef�gt wurde.
   * 
   * @param model
   *          Das TextDocumentModel, das durch den WollMux verarbeitet werden
   *          soll.
   */
  public static void handleReprocessTextDocument(TextDocumentModel model)
  {
    handle(new OnReprocessTextDocument(model));
  }

  private static class OnReprocessTextDocument extends BasicEvent
  {
    TextDocumentModel model;

    public OnReprocessTextDocument(TextDocumentModel model)
    {
      this.model = model;
    }

    protected void doit() throws WollMuxFehlerException
    {
      if (model == null) return;

      // Dokument mit neuen Dokumentkommandos �ber den
      // DocumentCommandInterpreter bearbeiten:
      model.getDocumentCommands().update();
      DocumentCommandInterpreter dci = new DocumentCommandInterpreter(model,
          WollMuxSingleton.getInstance());
      try
      {
        dci.executeTemplateCommands();

        // manche Kommandos sind erst nach der Expansion verf�gbar
        dci.scanGlobalDocumentCommands();
      }
      catch (java.lang.Exception e)
      {
        // Hier wird keine Exception erwartet, da Fehler (z.B. beim manuellen
        // Einf�gen von Textbausteinen) bereits dort als Popup angezeigt werden
        // sollen, wo sie auftreten.
      }

      stabilize();
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + model + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Obsolete, aber aus Kompatibilit�tgr�nden noch vorhanden. Bitte handleOpen()
   * statt dessen verwenden.
   * 
   * Erzeugt ein neues WollMuxEvent, welches daf�r sorgt, dass ein Dokument
   * ge�ffnet wird.
   * 
   * Dieses Event wird gestartet, wenn der WollMux-Service (...comp.WollMux) das
   * Dispatch-Kommando wollmux:openTemplate bzw. wollmux:openDocument empf�ngt
   * und sort daf�r, dass das entsprechende Dokument ge�ffnet wird.
   * 
   * @param fragIDs
   *          Eine List mit fragIDs, wobei das erste Element die FRAG_ID des zu
   *          �ffnenden Dokuments beinhalten muss. Weitere Elemente werden in
   *          eine Liste zusammengefasst und als Parameter f�r das
   *          Dokumentkommando insertContent verwendet.
   * @param asTemplate
   *          true, wenn das Dokument als "Unbenannt X" ge�ffnet werden soll
   *          (also im "Template-Modus") und false, wenn das Dokument zum
   *          Bearbeiten ge�ffnet werden soll.
   */
  public static void handleOpenDocument(List fragIDs, boolean asTemplate)
  {
    handle(new OnOpenDocument(fragIDs, asTemplate));
  }

  private static class OnOpenDocument extends BasicEvent
  {
    private boolean asTemplate;

    private List fragIDs;

    private OnOpenDocument(List fragIDs, boolean asTemplate)
    {
      this.fragIDs = fragIDs;
      this.asTemplate = asTemplate;
    }

    protected void doit() throws WollMuxFehlerException
    {
      // Baue ein ConfigThingy (als String), das die neue open-Methode versteht
      // und leite es weiter an diese.
      Iterator iter = fragIDs.iterator();
      String fragIdStr = "";
      while (iter.hasNext())
        fragIdStr += "'" + iter.next() + "' ";
      handleOpen("AS_TEMPLATE '"
                 + asTemplate
                 + "' FORMGUIS 'independent' Fragmente( FRAG_ID_LIST ("
                 + fragIdStr
                 + "))");
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "("
             + ((asTemplate) ? "asTemplate" : "asDocument")
             + ", "
             + fragIDs
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * �ffnet ein oder mehrere Dokumente anhand der Beschreibung openConfStr
   * (ConfigThingy-Syntax) und ist ausf�hrlicher beschrieben unter
   * http://limux.tvc.muenchen.de/wiki/index.php/Schnittstellen_des_WollMux_f%C3%BCr_Experten#wollmux:Open
   * 
   * Dieses Event wird gestartet, wenn der WollMux-Service (...comp.WollMux) das
   * Dispatch-Kommando wollmux:open empf�ngt.
   * 
   * @param openConfStr
   *          Die Beschreibung der zu �ffnenden Fragmente in ConfigThingy-Syntax
   */
  public static void handleOpen(String openConfStr)
  {
    handle(new OnOpen(openConfStr));
  }

  private static class OnOpen extends BasicEvent
  {
    private String openConfStr;

    private OnOpen(String openConfStr)
    {
      this.openConfStr = openConfStr;
    }

    protected void doit() throws WollMuxFehlerException
    {
      boolean asTemplate = true;
      boolean merged = false;
      ConfigThingy conf;
      ConfigThingy fragConf;
      ConfigThingy buttonAnpassung = null;
      try
      {
        conf = new ConfigThingy("OPEN", null, new StringReader(openConfStr));
        fragConf = conf.get("Fragmente");
      }
      catch (java.lang.Exception e)
      {
        throw new WollMuxFehlerException(
            "Fehlerhaftes \"wollmux:Open\" Kommando", e);
      }

      try
      {
        asTemplate = conf.get("AS_TEMPLATE", 1).toString().equalsIgnoreCase(
            "true");
      }
      catch (java.lang.Exception x)
      {
      }

      try
      {
        buttonAnpassung = conf.get("Buttonanpassung", 1);
      }
      catch (java.lang.Exception x)
      {
      }

      try
      {
        merged = conf.get("FORMGUIS", 1).toString().equalsIgnoreCase("merged");
      }
      catch (java.lang.Exception x)
      {
      }

      Iterator iter = fragConf.iterator();
      Vector docs = new Vector();
      while (iter.hasNext())
      {
        ConfigThingy fragListConf = (ConfigThingy) iter.next();
        List fragIds = new Vector();
        Iterator fragIter = fragListConf.iterator();
        while (fragIter.hasNext())
        {
          fragIds.add(fragIter.next().toString());
        }
        if (!fragIds.isEmpty())
        {
          TextDocumentModel model = openTextDocument(
              fragIds,
              asTemplate,
              merged);
          if (model != null) docs.add(model);
        }
      }

      if (merged)
      {
        handleProcessMultiform(docs, buttonAnpassung);
      }
    }

    /**
     * 
     * @param fragIDs
     * @param asTemplate
     * @param asPartOfMultiform
     * @return
     * @throws WollMuxFehlerException
     */
    private TextDocumentModel openTextDocument(List fragIDs,
        boolean asTemplate, boolean asPartOfMultiform)
        throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      // das erste Argument ist das unmittelbar zu landende Textfragment und
      // wird nach urlStr aufgel�st. Alle weiteren Argumente (falls vorhanden)
      // werden nach argsUrlStr aufgel�st.
      String loadUrlStr = "";
      String[] fragUrls = new String[fragIDs.size() - 1];
      String urlStr = "";

      Iterator iter = fragIDs.iterator();
      for (int i = 0; iter.hasNext(); ++i)
      {
        String frag_id = (String) iter.next();

        // Fragment-URL holen und aufbereiten:
        Vector urls = new Vector();

        java.lang.Exception error = new ConfigurationErrorException(
            "Das Textfragment mit der FRAG_ID '"
                + frag_id
                + "' ist nicht definiert!");
        try
        {
          urls = VisibleTextFragmentList.getURLsByID(frag_id);
        }
        catch (InvalidIdentifierException e)
        {
          error = e;
        }
        if (urls.size() == 0)
        {
          throw new WollMuxFehlerException(
              "Die URL zum Textfragment mit der FRAG_ID '"
                  + frag_id
                  + "' kann nicht bestimmt werden:", error);
        }

        // Nur die erste funktionierende URL verwenden. Dazu werden alle URL zu
        // dieser FRAG_ID gepr�ft und in die Variablen loadUrlStr und fragUrls
        // �bernommen.
        String errors = "";
        boolean found = false;
        Iterator iterUrls = urls.iterator();
        while (iterUrls.hasNext() && !found)
        {
          urlStr = (String) iterUrls.next();

          // URL erzeugen und pr�fen, ob sie aufgel�st werden kann
          URL url;
          try
          {
            url = WollMuxFiles.makeURL(urlStr);
            urlStr = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;
            url = WollMuxFiles.makeURL(urlStr);
            WollMuxSingleton.checkURL(url);
          }
          catch (MalformedURLException e)
          {
            Logger.log(e);
            errors += "Die URL '"
                      + urlStr
                      + "' ist ung�ltig:\n"
                      + e.getLocalizedMessage()
                      + "\n\n";
            continue;
          }
          catch (IOException e)
          {
            Logger.log(e);
            errors += e.getLocalizedMessage() + "\n\n";
            continue;
          }

          found = true;
        }

        if (!found)
        {
          throw new WollMuxFehlerException(
              "Das Textfragment mit der FRAG_ID '"
                  + frag_id
                  + "' kann nicht aufgel�st werden:\n\n"
                  + errors);
        }

        // URL in die in loadUrlStr (zum sofort �ffnen) und in argsUrlStr (zum
        // sp�ter �ffnen) aufnehmen
        if (i == 0)
        {
          loadUrlStr = urlStr;
        }
        else
        {
          fragUrls[i - 1] = urlStr;
        }
      }

      // open document as Template (or as document):
      TextDocumentModel model = null;
      try
      {
        XComponent doc = UNO.loadComponentFromURL(loadUrlStr, asTemplate, true);

        if (UNO.XTextDocument(doc) != null)
        {
          model = mux.getTextDocumentModel(UNO.XTextDocument(doc));
          model.setFragUrls(fragUrls);
          if (asPartOfMultiform)
            model.setPartOfMultiformDocument(asPartOfMultiform);
        }
      }
      catch (java.lang.Exception x)
      {
        // sollte eigentlich nicht auftreten, da bereits oben gepr�ft.
        throw new WollMuxFehlerException("Die Vorlage mit der URL '"
                                         + loadUrlStr
                                         + "' kann nicht ge�ffnet werden.", x);
      }
      return model;
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "('" + openConfStr + "')";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das daf�r sorgt, dass alle registrierten
   * XPALChangeEventListener geupdated werden.
   */
  public static void handlePALChangedNotify()
  {
    handle(new OnPALChangedNotify());
  }

  /**
   * Dieses Event wird immer dann erzeugt, wenn ein Dialog zur Bearbeitung der
   * PAL geschlossen wurde und immer dann wenn die PAL z.B. durch einen
   * wollmux:setSender-Befehl ge�ndert hat. Das Event sorgt daf�r, dass alle im
   * WollMuxSingleton registrierten XPALChangeListener geupdatet werden.
   * 
   * @author christoph.lutz
   */
  private static class OnPALChangedNotify extends BasicEvent
  {
    protected void doit()
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      // registrierte PALChangeListener updaten
      Iterator i = mux.palChangeListenerIterator();
      while (i.hasNext())
      {
        Logger.debug2("OnPALChangedNotify: Update XPALChangeEventListener");
        EventObject eventObject = new EventObject();
        eventObject.Source = WollMuxSingleton.getInstance();
        try
        {
          ((XPALChangeEventListener) i.next()).updateContent(eventObject);
        }
        catch (java.lang.Exception x)
        {
          i.remove();
        }
      }

      // Cache und LOS auf Platte speichern.
      try
      {
        mux.getDatasourceJoiner().saveCacheAndLOS(
            WollMuxFiles.getLosCacheFile());
      }
      catch (IOException e)
      {
        Logger.error(e);
      }
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent zum setzen des aktuellen Absenders.
   * 
   * @param senderName
   *          Name des Senders in der Form "Nachname, Vorname (Rolle)" wie sie
   *          auch der PALProvider bereith�lt.
   * @param idx
   *          der zum Sender senderName passende index in der sortierten
   *          Senderliste - dient zur Konsistenz-Pr�fung, damit kein Sender
   *          gesetzt wird, wenn die PAL der setzenden Komponente nicht mit der
   *          PAL des WollMux �bereinstimmt.
   */
  public static void handleSetSender(String senderName, int idx)
  {
    handle(new OnSetSender(senderName, idx));
  }

  /**
   * Dieses Event wird ausgel�st, wenn im WollMux-Service die methode setSender
   * aufgerufen wird. Es sort daf�r, dass ein neuer Absender gesetzt wird.
   * 
   * @author christoph.lutz
   */
  private static class OnSetSender extends BasicEvent
  {
    private String senderName;

    private int idx;

    public OnSetSender(String senderName, int idx)
    {
      this.senderName = senderName;
      this.idx = idx;
    }

    protected void doit()
    {
      DJDatasetListElement[] pal = WollMuxSingleton.getInstance()
          .getSortedPALEntries();

      // nur den neuen Absender setzen, wenn index und sender �bereinstimmen,
      // d.h.
      // die Absenderliste der entfernten WollMuxBar konsistent war.
      if (idx >= 0
          && idx < pal.length
          && pal[idx].toString().equals(senderName))
      {
        pal[idx].getDataset().select();
      }
      else
      {
        Logger.error("Setzen des Senders '"
                     + senderName
                     + "' schlug fehl, da der index '"
                     + idx
                     + "' nicht mit der PAL �bereinstimmt (Inkosistenzen?)");
      }

      WollMuxEventHandler.handlePALChangedNotify();
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "("
             + senderName
             + ", "
             + idx
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das die Druckfunktion zum TextDokument doc
   * auf functionName setzt.
   * 
   * Kann derzeit �ber den XWollMux-Service aufgerufen werden und steht damit
   * als Teil der Komfortdruckfunktionen auch in externen Routinen (z.B.
   * Basic-Makros, ...) zur Verf�gung.
   * 
   * @param doc
   *          Das TextDokument, zu dem die Druckfunktion gesetzt werden soll.
   * @param functionName
   *          der Name der Druckfunktion. Der Name muss ein g�ltiger
   *          Funktionsbezeichner sein und in einem Abschnitt "Druckfunktionen"
   *          in der wollmux.conf definiert sein.
   */
  public static void handleSetPrintFunction(XTextDocument doc,
      String functionName)
  {
    handle(new OnSetPrintFunction(doc, functionName));
  }

  private static class OnSetPrintFunction extends BasicEvent
  {
    private String functionName;

    private XTextDocument doc;

    public OnSetPrintFunction(XTextDocument doc, String functionName)
    {
      this.functionName = functionName;
      this.doc = doc;
    }

    protected void doit()
    {
      if (doc != null)
      {
        TextDocumentModel model = WollMuxSingleton.getInstance()
            .getTextDocumentModel(doc);
        model.setPrintFunction(functionName);
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", '"
             + functionName
             + "')";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, welches daf�r sorgt, dass alle
   * Formularfelder Dokument auf den neuen Wert gesetzt werden. Bei
   * Formularfeldern mit TRAFO-Funktion wird die Transformation entsprechend
   * durchgef�hrt.
   * 
   * Dieses Event wird (derzeit) vom FormModelImpl ausgel�st, wenn in der
   * Formular-GUI der Wert des Formularfeldes fieldID ge�ndert wurde und sorgt
   * daf�r, dass die Wert�nderung auf alle betroffenen Formularfelder im
   * Dokument doc �bertragen werden.
   * 
   * @param idToFormValues
   *          Eine HashMap die unter dem Schl�ssel fieldID den Vektor aller
   *          FormFields mit der ID fieldID liefert.
   * @param fieldId
   *          Die ID der Formularfelder, deren Werte angepasst werden sollen.
   * @param newValue
   *          Der neue untransformierte Wert des Formularfeldes.
   * @param funcLib
   *          Die Funktionsbibliothek, die zur Gewinnung der Trafo-Funktion
   *          verwendet werden soll.
   */
  public static void handleFormValueChanged(TextDocumentModel model,
      String fieldId, String newValue, FunctionLibrary funcLib)
  {
    handle(new OnFormValueChanged(model, fieldId, newValue, funcLib));
  }

  private static class OnFormValueChanged extends BasicEvent
  {
    private String fieldId;

    private String newValue;

    private FunctionLibrary funcLib;

    private TextDocumentModel model;

    public OnFormValueChanged(TextDocumentModel model, String fieldId,
        String newValue, FunctionLibrary funcLib)
    {
      this.fieldId = fieldId;
      this.newValue = newValue;
      this.funcLib = funcLib;
      this.model = model;
    }

    protected void doit()
    {
      model.setFormFieldValue(fieldId, newValue);
      model.updateFormFields(fieldId, funcLib);
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "("
             + fieldId
             + "', '"
             + newValue
             + "')";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, welches daf�r sorgt, dass alle
   * Sichtbarkeitselemente (Dokumentkommandos oder Bereiche mit Namensanhang
   * 'GROUPS ...') im �bergebenen Dokument, die einer bestimmten Gruppe groupId
   * zugeh�ren ein- oder ausgeblendet werden.
   * 
   * Dieses Event wird (derzeit) vom FormModelImpl ausgel�st, wenn in der
   * Formular-GUI bestimmte Text-Teile des �bergebenen Dokuments ein- oder
   * ausgeblendet werden sollen.
   * 
   * @param model
   *          Das TextDocumentModel, welches die Sichtbarkeitselemente enth�lt.
   * @param groupId
   *          Die GROUP (ID) der ein/auszublendenden Gruppe.
   * @param visible
   *          Der neue Sichtbarkeitsstatus (true=sichtbar, false=ausgeblendet)
   */
  public static void handleSetVisibleState(TextDocumentModel model,
      String groupId, boolean visible)
  {
    handle(new OnSetVisibleState(model, groupId, visible));
  }

  private static class OnSetVisibleState extends BasicEvent
  {
    private TextDocumentModel model;

    private String groupId;

    private boolean visible;

    public OnSetVisibleState(TextDocumentModel model, String groupId,
        boolean visible)
    {
      this.model = model;
      this.groupId = groupId;
      this.visible = visible;
    }

    protected void doit()
    {
      // invisibleGroups anpassen:
      HashSet invisibleGroups = model.getInvisibleGroups();
      if (visible)
        invisibleGroups.remove(groupId);
      else
        invisibleGroups.add(groupId);

      VisibilityElement firstChangedElement = null;

      // Sichtbarkeitselemente durchlaufen und alle ggf. updaten:
      Iterator iter = model.visibleElementsIterator();
      while (iter.hasNext())
      {
        VisibilityElement visibleElement = (VisibilityElement) iter.next();
        Set groups = visibleElement.getGroups();
        if (!groups.contains(groupId)) continue;

        // Visibility-Status neu bestimmen:
        boolean setVisible = true;
        Iterator i = groups.iterator();
        while (i.hasNext())
        {
          String groupId = (String) i.next();
          if (invisibleGroups.contains(groupId)) setVisible = false;
        }

        // Element merken, dessen Sichtbarkeitsstatus sich zuerst �ndert und
        // den focus (ViewCursor) auf den Start des Bereichs setzen. Da das
        // Setzen eines ViewCursors in einen unsichtbaren Bereich nicht
        // funktioniert, wird die Methode focusRangeStart zwei mal aufgerufen,
        // je nach dem, ob der Bereich vor oder nach dem Setzen des neuen
        // Sichtbarkeitsstatus sichtbar ist.
        if (setVisible != visibleElement.isVisible()
            && firstChangedElement == null)
        {
          firstChangedElement = visibleElement;
          if (firstChangedElement.isVisible()) focusRangeStart(visibleElement);
        }

        // neuen Sichtbarkeitsstatus setzen:
        try
        {
          visibleElement.setVisible(setVisible);
        }
        catch (RuntimeException e)
        {
          // Absicherung gegen das manuelle L�schen von Dokumentinhalten
        }
      }

      // Den Cursor (nochmal) auf den Anfang des Ankers des Elements setzen,
      // dessen Sichtbarkeitsstatus sich zuerst ge�ndert hat (siehe Begr�ndung
      // oben).
      if (firstChangedElement != null && firstChangedElement.isVisible())
        focusRangeStart(firstChangedElement);
    }

    /**
     * Diese Methode setzt den ViewCursor auf den Anfang des Ankers des
     * Sichtbarkeitselements.
     * 
     * @param visibleElement
     *          Das Sichtbarkeitselement, auf dessen Anfang des Ankers der
     *          ViewCursor gesetzt werden soll.
     */
    private void focusRangeStart(VisibilityElement visibleElement)
    {
      try
      {
        model.getViewCursor().gotoRange(
            visibleElement.getAnchor().getStart(),
            false);
      }
      catch (java.lang.Exception e)
      {
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "('"
             + groupId
             + "', "
             + visible
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das den ViewCursor des Dokuments auf das aktuell in der
   * Formular-GUI bearbeitete Formularfeld setzt.
   * 
   * Dieses Event wird (derzeit) vom FormModelImpl ausgel�st, wenn in der
   * Formular-GUI ein Formularfeld den Fokus bekommen hat und es sorgt daf�r,
   * dass der View-Cursor des Dokuments das entsprechende FormField im Dokument
   * anspringt.
   * 
   * @param idToFormValues
   *          Eine HashMap die unter dem Schl�ssel fieldID den Vektor aller
   *          FormFields mit der ID fieldID liefert.
   * @param fieldId
   *          die ID des Formularfeldes das den Fokus bekommen soll. Besitzen
   *          mehrere Formularfelder diese ID, so wird bevorzugt das erste
   *          Formularfeld aus dem Vektor genommen, das keine Trafo enth�lt.
   *          Ansonsten wird das erste Formularfeld im Vektor verwendet.
   */
  public static void handleFocusFormField(TextDocumentModel model,
      String fieldId)
  {
    handle(new OnFocusFormField(model, fieldId));
  }

  private static class OnFocusFormField extends BasicEvent
  {
    private TextDocumentModel model;

    private String fieldId;

    public OnFocusFormField(TextDocumentModel model, String fieldId)
    {
      this.model = model;
      this.fieldId = fieldId;
    }

    protected void doit()
    {
      model.focusFormField(fieldId);
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + model.doc
             + ", '"
             + fieldId
             + "')";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das die Position und Gr��e des �bergebenen
   * Dokument-Fensters auf die vorgegebenen Werte setzt. AHTUNG: Die Ma�angaben
   * beziehen sich auf die linke obere Ecke des Fensterinhalts OHNE die
   * Titelzeile und die Fensterdekoration des Rahmens. Um die linke obere Ecke
   * des gesamten Fensters richtig zu setzen, m�ssen die Gr��enangaben des
   * Randes der Fensterdekoration und die H�he der Titelzeile VOR dem Aufruf der
   * Methode entsprechend eingerechnet werden.
   * 
   * @param model
   *          Das XModel-Interface des Dokuments dessen Position/Gr��e gesetzt
   *          werden soll.
   * @param docX
   *          Die linke obere Ecke des Fensterinhalts X-Koordinate der Position
   *          in Pixel, gez�hlt von links oben.
   * @param docY
   *          Die Y-Koordinate der Position in Pixel, gez�hlt von links oben.
   * @param docWidth
   *          Die Gr��e des Dokuments auf der X-Achse in Pixel
   * @param docHeight
   *          Die Gr��e des Dokuments auf der Y-Achse in Pixel. Auch hier wird
   *          die Titelzeile des Rahmens nicht beachtet und muss vorher
   *          entsprechend eingerechnet werden.
   */
  public static void handleSetWindowPosSize(TextDocumentModel model, int docX,
      int docY, int docWidth, int docHeight)
  {
    handle(new OnSetWindowPosSize(model, docX, docY, docWidth, docHeight));
  }

  /**
   * Dieses Event wird vom FormModelImpl ausgel�st, wenn die Formular-GUI die
   * Position und die Ausmasse des Dokuments ver�ndert. Ruft direkt
   * setWindowsPosSize der UNO-API auf.
   * 
   * @author christoph.lutz
   */
  private static class OnSetWindowPosSize extends BasicEvent
  {
    private TextDocumentModel model;

    private int docX, docY, docWidth, docHeight;

    public OnSetWindowPosSize(TextDocumentModel model, int docX, int docY,
        int docWidth, int docHeight)
    {
      this.model = model;
      this.docX = docX;
      this.docY = docY;
      this.docWidth = docWidth;
      this.docHeight = docHeight;
    }

    protected void doit()
    {
      model.setWindowPosSize(docX, docY, docWidth, docHeight);
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "("
             + docX
             + ", "
             + docY
             + ", "
             + docWidth
             + ", "
             + docHeight
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das die Anzeige des �bergebenen Dokuments auf sichtbar
   * oder unsichtbar schaltet. Dabei wird direkt die entsprechende Funktion der
   * UNO-API verwendet.
   * 
   * @param model
   *          Das XModel interface des dokuments, welches sichtbar oder
   *          unsichtbar geschaltet werden soll.
   * @param visible
   *          true, wenn das Dokument sichtbar geschaltet werden soll und false,
   *          wenn das Dokument unsichtbar geschaltet werden soll.
   */
  public static void handleSetWindowVisible(TextDocumentModel model,
      boolean visible)
  {
    handle(new OnSetWindowVisible(model, visible));
  }

  /**
   * Dieses Event wird vom FormModelImpl ausgel�st, wenn die Formular-GUI das
   * bearbeitete Dokument sichtbar/unsichtbar schalten m�chte. Ruft direkt
   * setVisible der UNO-API auf.
   * 
   * @author christoph.lutz
   */
  private static class OnSetWindowVisible extends BasicEvent
  {
    private TextDocumentModel model;

    boolean visible;

    public OnSetWindowVisible(TextDocumentModel model, boolean visible)
    {
      this.model = model;
      this.visible = visible;
    }

    protected void doit()
    {
      model.setWindowVisible(visible);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + visible + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das das �bergebene Dokument schlie�t.
   * 
   * @param model
   *          Das zu schlie�ende TextDocumentModel.
   */
  public static void handleCloseTextDocument(TextDocumentModel model)
  {
    handle(new OnCloseTextDocument(model));
  }

  /**
   * Dieses Event wird vom FormModelImpl ausgel�st, wenn der Benutzer die
   * Formular-GUI schlie�t und damit auch das zugeh�rige TextDokument
   * geschlossen werden soll.
   * 
   * @author christoph.lutz
   */
  private static class OnCloseTextDocument extends BasicEvent
  {
    private TextDocumentModel model;

    public OnCloseTextDocument(TextDocumentModel model)
    {
      this.model = model;
    }

    protected void doit()
    {
      model.close();
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + model.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das ggf. notwendige interaktive
   * Initialisierungen vornimmt. Derzeit wird vor allem die Konsitenz der
   * pers�nlichen Absenderliste gepr�ft und der AbsenderAusw�hlen Dialog
   * gestartet, falls die Liste leer ist.
   */
  public static void handleInitialize()
  {
    handle(new OnInitialize());
  }

  /**
   * Dieses Event wird als erstes WollMuxEvent bei der Initialisierung des
   * WollMux im WollMuxSingleton erzeugt und �bernimmt alle benutzersichtbaren
   * (interaktiven) Initialisierungen wie z.B. das Darstellen des
   * AbsenderAusw�hlen-Dialogs, falls die PAL leer ist.
   * 
   * @author christoph.lutz TESTED
   */
  private static class OnInitialize extends BasicEvent
  {
    protected void doit()
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      DatasourceJoiner dsj = mux.getDatasourceJoiner();

      if (dsj.getLOS().size() == 0)
      {
        // falls es keine Datens�tze im LOS gibt:
        // Die initialen Daten nach Heuristik versuchen zu finden:
        int found = searchDefaultSender(dsj);

        // Absender Ausw�hlen Dialog starten:
        // wurde genau ein Datensatz gefunden, kann davon ausgegangen werden,
        // dass dieser OK ist - der Dialog muss dann nicht erscheinen.
        if (found != 1)
          WollMuxEventHandler.handleShowDialogAbsenderAuswaehlen();
        else
          handlePALChangedNotify();
      }
      else
      {
        // Liste der nicht zuordnenbaren Datens�tze erstellen und ausgeben:
        String names = "";
        List l = dsj.getStatus().lostDatasets;
        if (l.size() > 0)
        {
          Iterator i = l.iterator();
          while (i.hasNext())
          {
            Dataset ds = (Dataset) i.next();
            try
            {
              names += "- " + ds.get("Nachname") + ", ";
              names += ds.get("Vorname") + " (";
              names += ds.get("Rolle") + ")\n";
            }
            catch (ColumnNotFoundException x)
            {
              Logger.error(x);
            }
          }
          String message = "Die folgenden Datens�tze konnten nicht "
                           + "aus der Datenbank aktualisiert werden:\n\n"
                           + names
                           + "\nWenn dieses Problem nicht tempor�rer "
                           + "Natur ist, sollten Sie diese Datens�tze aus "
                           + "ihrer Absenderliste l�schen und neu hinzuf�gen!";
          WollMuxSingleton.showInfoModal("WollMux-Info", message);
        }
      }
    }

    /**
     * Wertet den Konfigurationsabschnitt
     * PersoenlicheAbsenderlisteInitialisierung/Suchstrategie aus und versucht
     * nach der angegebenen Strategie (mindestens) einen Datensatz im DJ dsj zu
     * finden, der den aktuellen Benutzer repr�sentiert. Fehlt der
     * Konfigurationsabschnitt, so wird die Defaultsuche
     * BY_OOO_USER_PROFILE(Vorname "${givenname}" Nachname "${sn}") gestartet.
     * Liefert ein Element der Suchstrategie mindestens einen Datensatz zur�ck,
     * so werden die anderen Elemente der Suchstrategie nicht mehr ausgewertet.
     * 
     * @param dsj
     *          Der DatasourceJoiner, in dem nach dem aktuellen Benutzer gesucht
     *          wird.
     * @return liefert die Anzahl der Datens�tze, die nach Durchlaufen der
     *         Suchstrategie gefunden wurden.
     */
    private int searchDefaultSender(DatasourceJoiner dsj)
    {
      // Auswertung des Abschnitts
      // PersoenlicheAbsenderlisteInitialisierung/Suchstrategie
      ConfigThingy wmConf = WollMuxFiles.getWollmuxConf();
      ConfigThingy strat = null;
      try
      {
        strat = wmConf.query("PersoenlicheAbsenderlisteInitialisierung").query(
            "Suchstrategie").getLastChild();
      }
      catch (NodeNotFoundException e)
      {
      }

      if (strat != null)
      {
        // Suche �ber Suchstrategie aus Konfiguration
        for (Iterator iter = strat.iterator(); iter.hasNext();)
        {
          ConfigThingy element = (ConfigThingy) iter.next();
          int found = 0;
          if (element.getName().equals("BY_JAVA_PROPERTY"))
          {
            found = new ByJavaPropertyFinder(dsj).find(element);
          }
          else if (element.getName().equals("BY_OOO_USER_PROFILE"))
          {
            found = new ByOOoUserProfileFinder(dsj).find(element);
          }
          else
          {
            Logger.error("Ung�ltiger Schl�ssel in Suchstategie: "
                         + element.stringRepresentation());
          }
          if (found != 0) return found;
        }
      }
      else
      {
        // Standardsuche �ber das OOoUserProfile:
        return new ByOOoUserProfileFinder(dsj).find(
            "Vorname",
            "${givenname}",
            "Nachname",
            "${sn}");
      }

      return 0;
    }

    /**
     * Ein DataFinder sucht Datens�tze im �bergebenen dsj, wobei in der
     * Beschreibung der gesuchten Werte Variablen in der Form "${varname}"
     * verwendet werden k�nnen, die vor der Suche in einer anderen Datenquelle
     * aufgel�st werden. Die Aufl�sung erledigt durch die konkrete Klasse.
     */
    private static abstract class DataFinder
    {
      private DatasourceJoiner dsj;

      public DataFinder(DatasourceJoiner dsj)
      {
        this.dsj = dsj;
      }

      /**
       * Erwartet ein ConfigThingy, das ein oder zwei Schl�ssel-/Wertpaare
       * enth�lt (in der Form "<KNOTEN>(<dbSpalte1> 'wert1' [<dbSpalte2>
       * 'wert2'])" nach denen in der Datenquelle gesucht werden soll. Die
       * Beiden Wertpaare werden dabei UND verkn�pft. Die Werte wert1 und wert2
       * k�nnen �ber die Syntax "${name}" Variablen referenzieren, die vor der
       * Suche aufgel�st werden.
       * 
       * @param conf
       *          Das ConfigThingy, das die Suchabfrage beschreibt.
       * @return Die Anzahl der gefundenen Datens�tze.
       */
      public int find(ConfigThingy conf)
      {
        int count = 0;
        String id1 = "";
        String id2 = "";
        String value1 = "";
        String value2 = "";
        for (Iterator iter = conf.iterator(); iter.hasNext();)
        {
          ConfigThingy element = (ConfigThingy) iter.next();
          if (count == 0)
          {
            id1 = element.getName();
            value1 = element.toString();
            count++;
          }
          else if (count == 1)
          {
            id2 = element.getName();
            value2 = element.toString();
            count++;
          }
          else
          {
            Logger
                .error("Nur max zwei Schl�ssel/Wert-Paare werden als Argumente f�r Suchanfragen akzeptiert!");
          }
        }

        if (count == 1)
        {
          return find(id1, value1);
        }
        else if (count == 2)
        {
          return find(id1, value1, id2, value2);
        }
        return 0;
      }

      /**
       * Sucht in der Datenquelle nach Datens�tzen deren Feld dbSpalte den
       * evaluierten Wert von value enth�lt und �bertr�gt die gefundenen Werte
       * in die PAL.
       * 
       * @param dbSpalte
       *          der Feldname �ber den nach dem evaluierten Wert von value
       *          gesucht wird.
       * @param value
       *          value wird vor der Suche mittels evaluate() evaluiert (d.h.
       *          evtl. vorhandene Variablen durch die entsprechenden Inhalte
       *          ersetzt ersetzt).
       * @return die Anzahl der gefundenen Datens�tze
       */
      protected int find(String dbSpalte, String value)
      {
        Logger.debug2(this.getClass().getSimpleName()
                      + ".tryToFind("
                      + dbSpalte
                      + " '"
                      + value
                      + "')");
        try
        {
          String v = evaluate(value);
          if (v.length() == 0) return 0;
          QueryResults r = dsj.find(dbSpalte, v);
          return addToPAL(r);
        }
        catch (TimeoutException e)
        {
          Logger.error(e);
        }
        catch (IllegalArgumentException e)
        {
          Logger.debug(e);
        }
        return 0;
      }

      /**
       * Sucht in der Datenquelle nach Datens�tzen wobei die beiden
       * Suchbedingungen (dbSpalte1==evaluate(value1) und
       * dbSpalte2==evaluate(value2)) mit UND verkn�pft sind - die gefundenen
       * Werte werden danach in die PAL kopiert.
       * 
       * @param dbSpalte1
       *          der Feldname �ber den nach dem evaluierten Wert von value
       *          gesucht wird.
       * @param value1
       *          value wird vor der Suche mittels evaluate() evaluiert (d.h.
       *          evtl. vorhandene Variablen durch die entsprechenden Inhalte
       *          ersetzt ersetzt).
       * @param dbSpalte2
       *          der Feldname �ber den nach dem evaluierten Wert von value
       *          gesucht wird.
       * @param value2
       *          value wird vor der Suche mittels evaluate() evaluiert (d.h.
       *          evtl. vorhandene Variablen durch die entsprechenden Inhalte
       *          ersetzt ersetzt).
       * @return die Anzahl der gefundenen Datens�tze
       */
      protected int find(String dbSpalte1, String value1, String dbSpalte2,
          String value2)
      {
        Logger.debug2(this.getClass().getSimpleName()
                      + ".tryToFind("
                      + dbSpalte1
                      + " '"
                      + value1
                      + "' "
                      + dbSpalte2
                      + " '"
                      + value2
                      + "')");
        try
        {
          String v1 = evaluate(value1);
          String v2 = evaluate(value2);
          if (v1.length() == 0 || v2.length() == 0) return 0;
          QueryResults r = dsj.find(dbSpalte1, v1, dbSpalte2, v2);
          return addToPAL(r);
        }
        catch (TimeoutException e)
        {
          Logger.error(e);
        }
        catch (IllegalArgumentException e)
        {
          Logger.debug(e);
        }
        return 0;
      }

      /**
       * Kopiert alle matches von QueryResults in die PAL.
       */
      private int addToPAL(QueryResults r)
      {
        for (Iterator iter = r.iterator(); iter.hasNext();)
        {
          DJDataset element = (DJDataset) iter.next();
          element.copy();
        }
        return r.size();
      }

      /**
       * Ersetzt die Variablen in exp durch deren evaluierten Inhalt, wobei die
       * Evaluierung �ber getValueForKey() erfolgt, die von jeder konkreten
       * Klasse implementiert wird. Evaluate() stellt auch sicher, dass die von
       * getValueForKey() zur�ckgelieferten Werte nicht selbst Variablen
       * enthalten k�nnen (indem die Variablenbegrenzer "${" und "}" durch "<"
       * bzw. ">" ersetzt werden.
       * 
       * @param exp
       *          der zu evaluierende Ausdruck
       * @return
       */
      protected String evaluate(String exp)
      {
        final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^\\}]*)\\}");
        while (true)
        {
          Matcher m = VAR_PATTERN.matcher(exp);
          if (!m.find()) break;
          String key = m.group(1);
          String value = getValueForKey(key);
          // keine Variablenbegrenzer "${" und "}" in value zulassen:
          value = value.replaceAll("\\$\\{", "<");
          value = value.replaceAll("\\}", ">");
          exp = m.replaceFirst(value);
        }
        return exp;
      }

      /**
       * Liefert den Wert zu einer Variable namens key und muss von jeder
       * konkreten Finder-Klasse implementiert werden.
       * 
       * @param key
       *          Der Schl�ssel, zu dem der Wert zur�ckgeliefert werden soll.
       * @return der zugeh�rige Wert zum Schl�ssel key.
       */
      protected abstract String getValueForKey(String key);
    }

    /**
     * Ein konkreter DataFinder, der f�r die Aufl�sung der Variable in
     * getValueForKey im Benutzerprofil der OOo Registry nachschaut (das selbe
     * wie Extras->Optionen->OpenOffice.org->Benutzerdaten).
     * 
     * @author christoph.lutz
     */
    private static class ByOOoUserProfileFinder extends DataFinder
    {
      public ByOOoUserProfileFinder(DatasourceJoiner dsj)
      {
        super(dsj);
      }

      protected String getValueForKey(String key)
      {
        try
        {
          UnoService confProvider = UnoService.createWithContext(
              "com.sun.star.configuration.ConfigurationProvider",
              WollMuxSingleton.getInstance().getXComponentContext());

          UnoService confView = confProvider.create(
              "com.sun.star.configuration.ConfigurationAccess",
              new UnoProps("nodepath", "/org.openoffice.UserProfile/Data")
                  .getProps());
          return confView.xNameAccess().getByName(key).toString();
        }
        catch (Exception e)
        {
          Logger.error("Konnte den Wert zum Schl�ssel '"
                       + key
                       + "' des OOoUserProfils nicht bestimmen:", e);
        }
        return "";
      }
    }

    /**
     * Ein konkreter DataFinder, der f�r die Aufl�sung der Variable in
     * getValueForKey die Methode System.getProperty(key) verwendet.
     * 
     * @author christoph.lutz
     */
    private static class ByJavaPropertyFinder extends DataFinder
    {
      public ByJavaPropertyFinder(DatasourceJoiner dsj)
      {
        super(dsj);
      }

      protected String getValueForKey(String key)
      {
        try
        {
          return System.getProperty(key).toString();
        }
        catch (java.lang.Exception e)
        {
          Logger.error("Konnte den Wert der JavaProperty '"
                       + key
                       + "' nicht bestimmen:", e);
        }
        return "";
      }
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent zum Registrieren des �bergebenen
   * XPALChangeEventListeners.
   * 
   * @param listener
   */
  public static void handleAddPALChangeEventListener(
      XPALChangeEventListener listener, Integer wollmuxConfHashCode)
  {
    handle(new OnAddPALChangeEventListener(listener, wollmuxConfHashCode));
  }

  /**
   * Dieses Event wird ausgel�st, wenn sich ein externer PALChangeEventListener
   * beim WollMux-Service registriert. Es sorgt daf�r, dass der
   * PALChangeEventListener in die Liste der registrierten
   * PALChangeEventListener im WollMuxSingleton aufgenommen wird.
   * 
   * @author christoph.lutz
   */
  private static class OnAddPALChangeEventListener extends BasicEvent
  {
    private XPALChangeEventListener listener;

    private Integer wollmuxConfHashCode;

    public OnAddPALChangeEventListener(XPALChangeEventListener listener,
        Integer wollmuxConfHashCode)
    {
      this.listener = listener;
      this.wollmuxConfHashCode = wollmuxConfHashCode;
    }

    protected void doit()
    {
      WollMuxSingleton.getInstance().addPALChangeEventListener(listener);

      WollMuxEventHandler.handlePALChangedNotify();

      // Pr�fen, ob WollMux-Installation noch passt (keine Halb- oder
      // Doppelinstallation)
      WollMuxEventHandler.handleCheckInstallation();

      // Konsistenzpr�fung: Stimmt WollMux-Konfiguration der entfernten
      // Komponente mit meiner Konfiguration �berein? Ansonsten Fehlermeldung.
      if (wollmuxConfHashCode != null)
      {
        int myWmConfHash = WollMuxFiles.getWollmuxConf().stringRepresentation()
            .hashCode();
        if (myWmConfHash != wollmuxConfHashCode.intValue())
          errorMessage(new InvalidBindingStateException(
              "Die Konfiguration des WollMux muss neu eingelesen werden.\n\nBitte beenden Sie den WollMux und OpenOffice.org und schie�en Sie alle laufenden 'soffice.bin'-Prozesse �ber den Taskmanager ab."));
      }

    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(listener, o);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den �bergebenen XPALChangeEventListener
   * deregistriert.
   * 
   * @param listener
   *          der zu deregistrierende XPALChangeEventListener
   */
  public static void handleRemovePALChangeEventListener(
      XPALChangeEventListener listener)
  {
    handle(new OnRemovePALChangeEventListener(listener));
  }

  /**
   * Dieses Event wird vom WollMux-Service (...comp.WollMux) ausgel�st wenn sich
   * ein externe XPALChangeEventListener beim WollMux deregistriert. Der zu
   * entfernende XPALChangeEventListerner wird anschlie�end im WollMuxSingleton
   * aus der Liste der registrierten XPALChangeEventListener genommen.
   * 
   * @author christoph.lutz
   */
  private static class OnRemovePALChangeEventListener extends BasicEvent
  {
    private XPALChangeEventListener listener;

    public OnRemovePALChangeEventListener(XPALChangeEventListener listener)
    {
      this.listener = listener;
    }

    protected void doit()
    {
      WollMuxSingleton.getInstance().removePALChangeEventListener(listener);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent das signaisiert, dass die Druckfunktion
   * aufgerufen werden soll, die im TextDocumentModel model aktuell definiert
   * ist. Die Methode erwartet, dass vor dem Aufruf gepr�ft wurde, ob model eine
   * Druckfunktion definiert. Ist dennoch keine Druckfunktion definiert, so
   * erscheint eine Fehlermeldung im Log.
   * 
   * Das Event wird ausgel�st, wenn der registrierte WollMuxDispatchInterceptor
   * eines Dokuments eine entsprechende Nachricht bekommt.
   */
  public static void handleExecutePrintFunction(TextDocumentModel model)
  {
    handle(new OnExecutePrintFunction(model));
  }

  private static class OnExecutePrintFunction extends BasicEvent
  {
    private TextDocumentModel model;

    public OnExecutePrintFunction(TextDocumentModel model)
    {
      this.model = model;
    }

    protected void doit() throws WollMuxFehlerException
    {

      PrintFunction printFunc = null;
      String printFunctionName = model.getPrintFunctionName();
      if (printFunctionName != null)
      {
        printFunc = WollMuxSingleton.getInstance().getGlobalPrintFunctions()
            .get(printFunctionName);
        if (printFunc == null)
          Logger.error("Druckfunktion '"
                       + printFunctionName
                       + "' nicht definiert.");
      }
      else
        Logger.error("Dem Textdokument ist keine Druckfunktion zugeordnet!");
      if (printFunc == null) return;

      // TODO: sollte evtl. einmal durch eine allgemeine Funktion
      // checkPreconditions() ersetzt werden, die pr�ft ob die Vorbedingungen
      // f�r eine Druckfunktion noch erf�llt sind und andernfalls die Funktion
      // l�scht. Das erfordert aber die M�glichkeit, Vorbedingungen zu
      // definieren und Druckfunktionen zuzuordnen...
      //
      // Ziffernanpassung der Sachleitenden Verf�gungen durlaufen lassen, um zu
      // erkennen, ob Verf�gungspunkte manuell aus dem Dokument gel�scht
      // wurden ohne die entsprechenden Kn�pfe zum Einf�gen/Entfernen von
      // Ziffern zu dr�cken.
      if (SachleitendeVerfuegung.PRINT_FUNCTION_NAME.equals(printFunctionName))
      {
        SachleitendeVerfuegung.ziffernAnpassen(model);
        stabilize();
        // hat ZiffernAnpassen die PrintFunction ver�ndert?
        if (!SachleitendeVerfuegung.PRINT_FUNCTION_NAME.equals(model
            .getPrintFunctionName()))
        {
          // dann einen neuen Dispatch absetzen und beenden:
          UNO.dispatch(model.doc, ".uno:Print");
          return;
        }
      }

      // Druckfunktion ausf�hren:
      printFunc.invoke(model.getPrintModel());
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + model + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass das Dokument doc in
   * der Anzahl numberOfCopies ausgedruckt werden soll. Nach Beendigung des
   * Events soll der CallBack des �bergebenen ActionsListeners aufgerufen
   * werden.
   * 
   * Das Event dient als Hilfe f�r die Komfortdruckfunktionen und wird vom
   * XPrintModel aufgerufen und mit diesem synchronisiert.
   */
  public static void handlePrintViaPrintModel(XTextDocument doc, HashMap props,
      ActionListener listener)
  {
    handle(new OnPrintViaPrintModel(doc, props, listener));
  }

  private static class OnPrintViaPrintModel extends BasicEvent
  {
    private XTextDocument doc;

    private HashMap props;

    private ActionListener listener;

    public OnPrintViaPrintModel(XTextDocument doc, HashMap props,
        ActionListener listener)
    {
      this.doc = doc;
      this.props = props;
      this.listener = listener;
    }

    protected void doit() throws WollMuxFehlerException
    {
      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);
      Integer cc = (Integer) props.get("CopyCount");
      if (cc == null) cc = new Integer(1);
      try
      {
        model.print(cc.shortValue());
      }
      catch (PrintFailedException e)
      {
        errorMessage(e);
      }

      stabilize();
      listener.actionPerformed(null);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass eine weitere Ziffer
   * der Sachleitenden Verf�gungen eingef�gt werden, bzw. eine bestehende Ziffer
   * gel�scht werden soll.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen, wenn Aufgrund eines
   * Drucks auf den Knopf der OOo-Symbolleiste ein "wollmux:ZifferEinfuegen"
   * dispatch erfolgte.
   */
  public static void handleButtonZifferEinfuegenPressed(TextDocumentModel model)
  {
    handle(new OnZifferEinfuegen(model));
  }

  private static class OnZifferEinfuegen extends BasicEvent
  {
    private TextDocumentModel model;

    public OnZifferEinfuegen(TextDocumentModel model)
    {
      this.model = model;
    }

    protected void doit() throws WollMuxFehlerException
    {
      XTextCursor viewCursor = model.getViewCursor();
      if (viewCursor != null)
      {
        XTextRange vc = SachleitendeVerfuegung.insertVerfuegungspunkt(
            model,
            viewCursor);
        if (vc != null) viewCursor.gotoRange(vc, false);
      }

      stabilize();
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + model + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass eine Abdruckzeile
   * der Sachleitenden Verf�gungen eingef�gt werden, bzw. eine bestehende
   * Abdruckzeile gel�scht werden soll.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen, wenn Aufgrund eines
   * Drucks auf den Knopf der OOo-Symbolleiste ein "wollmux:Abdruck" dispatch
   * erfolgte.
   */
  public static void handleButtonAbdruckPressed(TextDocumentModel model)
  {
    handle(new OnAbdruck(model));
  }

  private static class OnAbdruck extends BasicEvent
  {
    private TextDocumentModel model;

    public OnAbdruck(TextDocumentModel model)
    {
      this.model = model;
    }

    protected void doit() throws WollMuxFehlerException
    {
      XTextCursor viewCursor = model.getViewCursor();
      if (viewCursor != null)
      {
        XTextRange vc = SachleitendeVerfuegung.insertAbdruck(model, viewCursor);
        if (vc != null) viewCursor.gotoRange(vc, false);
      }

      stabilize();
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + model + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass eine Zuleitungszeile
   * der Sachleitenden Verf�gungen eingef�gt werden, bzw. eine bestehende
   * Zuleitungszeile gel�scht werden soll.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen, wenn Aufgrund eines
   * Drucks auf den Knopf der OOo-Symbolleiste ein "wollmux:Zuleitungszeile"
   * dispatch erfolgte.
   */
  public static void handleButtonZuleitungszeilePressed(TextDocumentModel model)
  {
    handle(new OnButtonZuleitungszeilePressed(model));
  }

  private static class OnButtonZuleitungszeilePressed extends BasicEvent
  {
    private TextDocumentModel model;

    public OnButtonZuleitungszeilePressed(TextDocumentModel model)
    {
      this.model = model;
    }

    protected void doit() throws WollMuxFehlerException
    {
      XTextCursor viewCursor = model.getViewCursor();
      if (viewCursor != null)
      {
        XTextRange vc = SachleitendeVerfuegung.insertZuleitungszeile(
            model,
            viewCursor);
        if (vc != null) viewCursor.gotoRange(vc, false);
      }

      stabilize();
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + model + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das �ber den Bereich des viewCursors im
   * Dokument doc ein neues Bookmark mit dem Namen "WM(CMD'<blockname>')" legt,
   * wenn nicht bereits ein solches Bookmark im markierten Block definiert ist.
   * Ist bereits ein Bookmark mit diesem Namen vorhanden, so wird dieses
   * gel�scht.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen, wenn Aufgrund eines
   * Drucks auf den Knopf der OOo-Symbolleiste ein "wollmux:markBlock#<blockname>"
   * dispatch erfolgte.
   * 
   * @param model
   *          Das Textdokument, in dem der Block eingef�gt werden soll.
   * @param blockname
   *          Derzeit werden folgende Blocknamen akzeptiert "draftOnly",
   *          "notInOriginal" und "allVersions". Alle anderen Blocknamen werden
   *          ignoriert und keine Aktion ausgef�hrt.
   */
  public static void handleMarkBlock(TextDocumentModel model, String blockname)
  {
    handle(new OnMarkBlock(model, blockname));
  }

  private static class OnMarkBlock extends BasicEvent
  {
    private TextDocumentModel model;

    private String blockname;

    public OnMarkBlock(TextDocumentModel model, String blockname)
    {
      this.model = model;
      this.blockname = blockname;
    }

    protected void doit() throws WollMuxFehlerException
    {
      if (UNO.XBookmarksSupplier(model.doc) == null || blockname == null)
        return;

      ConfigThingy slvConf = WollMuxSingleton.getInstance().getWollmuxConf()
          .query("SachleitendeVerfuegungen");
      Integer highlightColor = null;

      XTextCursor range = model.getViewCursor();

      if (range == null) return;

      if (range.isCollapsed())
      {
        WollMuxSingleton.showInfoModal(
            "Fehler",
            "Bitte w�hlen Sie einen Bereich aus, der markiert werden soll.");
        return;
      }

      String markChange = null;
      if (blockname.equalsIgnoreCase("allVersions"))
      {
        markChange = "wird immer gedruckt";
        highlightColor = getHighlightColor(
            slvConf,
            "ALL_VERSIONS_HIGHLIGHT_COLOR");
      }
      else if (blockname.equalsIgnoreCase("draftOnly"))
      {
        markChange = "wird nur im Entwurf gedruckt";
        highlightColor = getHighlightColor(
            slvConf,
            "DRAFT_ONLY_HIGHLIGHT_COLOR");
      }
      else if (blockname.equalsIgnoreCase("notInOriginal"))
      {
        markChange = "wird immer gedruckt, ausser im Original";
        highlightColor = getHighlightColor(
            slvConf,
            "NOT_IN_ORIGINAL_HIGHLIGHT_COLOR");
      }
      else
        return;

      String bookmarkStart = "WM(CMD '" + blockname + "'";
      String hcAtt = "";
      if (highlightColor != null)
      {
        String colStr = "00000000";
        colStr += Integer.toHexString(highlightColor.intValue());
        colStr = colStr.substring(colStr.length() - 8, colStr.length());
        hcAtt = " HIGHLIGHT_COLOR '" + colStr + "'";
      }
      String bookmarkName = bookmarkStart + hcAtt + ")";

      Set bmNames = getBookmarkNamesStartingWith(bookmarkStart, range);

      if (bmNames.size() > 0)
      {
        // bereits bestehende Bl�cke l�schen
        Iterator iter = bmNames.iterator();
        while (iter.hasNext())
        {
          bookmarkName = iter.next().toString();
          try
          {
            Bookmark b = new Bookmark(bookmarkName, UNO
                .XBookmarksSupplier(model.doc));
            if (bookmarkName.contains("HIGHLIGHT_COLOR"))
              UNO.setPropertyToDefault(b.getTextRange(), "CharBackColor");
            b.remove();
          }
          catch (NoSuchElementException e)
          {
          }
        }
        WollMuxSingleton.showInfoModal(
            "Markierung des Blockes aufgehoben",
            "Der ausgew�hlte Block enthielt bereits eine Markierung \"Block "
                + markChange
                + "\". Die bestehende Markierung wurde aufgehoben.");
      }
      else
      {
        // neuen Block anlegen
        Bookmark b = new Bookmark(bookmarkName, model.doc, range);
        if (highlightColor != null)
        {
          UNO.setProperty(b.getTextRange(), "CharBackColor", highlightColor);
          // ViewCursor kollabieren, da die Markierung die Farben verf�lscht
          // darstellt.
          XTextCursor vc = model.getViewCursor();
          if (vc != null) vc.collapseToEnd();
        }
        WollMuxSingleton.showInfoModal(
            "Block wurde markiert",
            "Der ausgew�hlte Block " + markChange + ".");
      }

      // PrintBl�cke neu einlesen:
      model.getDocumentCommands().update();
      DocumentCommandInterpreter dci = new DocumentCommandInterpreter(model,
          WollMuxSingleton.getInstance());
      dci.scanGlobalDocumentCommands();

      stabilize();
    }

    /**
     * Liefert einen Integer der Form AARRGGBB (hex), der den Farbwert
     * repr�sentiert, der in slvConf im Attribut attribute hinterlegt ist oder
     * null, wenn das Attribut nicht existiert oder der dort enthaltene
     * String-Wert sich nicht in eine Integerzahl konvertieren l�sst.
     * 
     * @param slvConf
     * @param attribute
     */
    private static Integer getHighlightColor(ConfigThingy slvConf,
        String attribute)
    {
      try
      {
        String highlightColor = slvConf.query(attribute).getLastChild()
            .toString();
        if (highlightColor.equals("")
            || highlightColor.equalsIgnoreCase("none")) return null;
        int hc = Integer.parseInt(highlightColor, 16);
        return new Integer(hc);
      }
      catch (NodeNotFoundException e)
      {
        return null;
      }
      catch (NumberFormatException e)
      {
        Logger.error("Der angegebene Farbwert im Attribut '"
                     + attribute
                     + "' ist ung�ltig!");
        return null;
      }
    }

    /**
     * Liefert die Namen aller Bookmarks, die in im Bereich range existieren und
     * (case insesitive) mit dem Namen bookmarkName anfangen.
     * 
     * @param bookmarkName
     * @param range
     */
    private static HashSet getBookmarkNamesStartingWith(String bookmarkName,
        XTextRange range)
    {
      // Hier findet eine iteration des �ber den XEnumerationAccess des ranges
      // statt. Man k�nnte statt dessen auch �ber range-compare mit den bereits
      // bestehenden Bl�cken aus TextDocumentModel.get<blockname>Blocks()
      // vergleichen...
      bookmarkName = bookmarkName.toLowerCase();
      HashSet found = new HashSet();
      HashSet started = new HashSet();
      XTextCursor cursor = range.getText().createTextCursorByRange(range);
      if (UNO.XEnumerationAccess(cursor) != null)
      {
        XEnumeration xenum = UNO.XEnumerationAccess(cursor).createEnumeration();
        while (xenum.hasMoreElements())
        {
          XEnumeration parEnum = null;
          try
          {
            parEnum = UNO.XEnumerationAccess(xenum.nextElement())
                .createEnumeration();
          }
          catch (java.lang.Exception e)
          {
          }

          while (parEnum != null && parEnum.hasMoreElements())
          {
            XTextContent textPortion = null;
            try
            {
              textPortion = UNO.XTextContent(parEnum.nextElement());
            }
            catch (java.lang.Exception e)
            {
            }
            XNamed bookmark = UNO.XNamed(UNO.getProperty(
                textPortion,
                "Bookmark"));
            String name = (bookmark != null) ? bookmark.getName() : "";

            if (name.toLowerCase().startsWith(bookmarkName))
            {
              boolean isStart = ((Boolean) UNO.getProperty(
                  textPortion,
                  "IsStart")).booleanValue();
              if (isStart)
                started.add(name);
              else if (started.contains(name)) found.add(name);
            }
          }
        }
      }
      return found;
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + model.hashCode()
             + ", '"
             + blockname
             + "')";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das daf�r sorgt, dass eine Datei
   * wollmux.dump erzeugt wird, die viele f�r die Fehlersuche relevanten
   * Informationen enth�lt wie z.B. Versionsinfo, Inhalt der wollmux.conf,
   * cache.conf, StringRepr�sentation der Konfiguration im Speicher und eine
   * Kopie der Log-Datei.
   * 
   * Das Event wird von der WollMuxBar geworfen, die (speziell f�r Admins, nicht
   * f�r Endbenutzer) einen entsprechenden Button besitzt.
   */
  public static void handleDumpInfo()
  {
    handle(new OnDumpInfo());
  }

  private static class OnDumpInfo extends BasicEvent
  {

    protected void doit() throws WollMuxFehlerException
    {
      final String title = "Fehlerinfos erstellen";

      String name = WollMuxFiles.dumpInfo();

      if (name != null)
        WollMuxSingleton.showInfoModal(
            title,
            "Die Fehlerinformationen des WollMux wurden erfolgreich in die Datei '"
                + name
                + "' geschrieben.");
      else
        WollMuxSingleton
            .showInfoModal(
                title,
                "Die Fehlerinformationen des WollMux konnten nicht geschrieben werden\n\nDetails siehe Datei wollmux.log!");
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "()";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass das gesamte Office
   * (und damit auch der WollMux) OHNE Sicherheitsabfragen(!) beendet werden
   * soll.
   * 
   * Das Event wird von der WollMuxBar geworfen, die (speziell f�r Admins, nicht
   * f�r Endbenutzer) einen entsprechenden Button besitzt.
   */
  public static void handleKill()
  {
    handle(new OnKill());
  }

  private static class OnKill extends BasicEvent
  {
    protected void doit() throws WollMuxFehlerException
    {
      if (UNO.desktop != null)
      {
        UNO.desktop.terminate();
      }
      else
      {
        System.exit(0);
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "()";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das daf�r sorgt, dass bei Sachleitenden
   * Verf�gungen der Verf�gungspunkt mit der Nummer verfPunkt ausgedruck wird,
   * wobei alle darauffolgenden Verf�gungspunkte ausgeblendet sind.
   * 
   * Das Event wird �blicherweise von einer XPrintModel Implementierung (z.B.
   * TextDocument.PrintModel) geworfen, wenn aus der Druckfunktion die
   * zugeh�rige Methode aufgerufen wurde.
   * 
   * @param doc
   *          Das Dokument, welches die Verf�gungspunkte enth�lt.
   * @param verfPunkt
   *          Die Nummer des auszuduruckenden Verf�gungspunktes, wobei alle
   *          folgenden Verf�gungspunkte ausgeblendet werden.
   * @param numberOfCopies
   *          Die Anzahl der Ausfertigungen, in der verfPunkt ausgedruckt werden
   *          soll.
   * @param isDraft
   *          wenn isDraft==true, werden alle draftOnly-Bl�cke eingeblendet,
   *          ansonsten werden sie ausgeblendet.
   * @param isOriginal
   *          wenn isOriginal, wird die Ziffer des Verf�gungspunktes I
   *          ausgeblendet und alle notInOriginal-Bl�cke ebenso. Andernfalls
   *          sind Ziffer und notInOriginal-Bl�cke eingeblendet.
   * @param unlockActionListener
   *          Der unlockActionListener wird nach Beendigung des Druckens
   *          aufgerufen, wenn er != null.
   */
  public static void handlePrintVerfuegungspunkt(XTextDocument doc,
      short verfPunkt, short numberOfCopies, boolean isDraft,
      boolean isOriginal, short pageRangeType, String pageRangeValue,
      ActionListener unlockActionListener)
  {
    handle(new OnPrintVerfuegungspunkt(doc, verfPunkt, numberOfCopies, isDraft,
        isOriginal, pageRangeType, pageRangeValue, unlockActionListener));
  }

  private static class OnPrintVerfuegungspunkt extends BasicEvent
  {
    private XTextDocument doc;

    private short verfPunkt;

    private short numberOfCopies;

    private boolean isDraft;

    private boolean isOriginal;

    private short pageRangeType;

    private String pageRangeValue;

    private ActionListener listener;

    public OnPrintVerfuegungspunkt(XTextDocument doc, short verfPunkt,
        short numberOfCopies, boolean isDraft, boolean isOriginal,
        short pageRangeType, String pageRangeValue, ActionListener listener)
    {
      this.doc = doc;
      this.verfPunkt = verfPunkt;
      this.numberOfCopies = numberOfCopies;
      this.isDraft = isDraft;
      this.isOriginal = isOriginal;
      this.pageRangeType = pageRangeType;
      this.pageRangeValue = pageRangeValue;
      this.listener = listener;
    }

    protected void doit() throws WollMuxFehlerException
    {
      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);

      try
      {
        SachleitendeVerfuegung.printVerfuegungspunkt(
            model,
            verfPunkt,
            numberOfCopies,
            isDraft,
            isOriginal,
            pageRangeType,
            pageRangeValue);
      }
      catch (PrintFailedException e)
      {
        errorMessage(e);
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }

      if (listener != null) listener.actionPerformed(null);

      stabilize();
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", verfPunkt="
             + verfPunkt
             + ", copies="
             + numberOfCopies
             + ", isDraft="
             + isDraft
             + ", isOriginal="
             + isOriginal
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das daf�r sorgt, dass der PrintSetupDialog
   * von OOo angezeigt wird, �ber den der aktuelle Drucker ausgew�hlt und
   * ge�ndert werden kann.
   * 
   * Das Event wird aus der Implementierung von XPrintModel (siehe
   * TextDocumentModel) geworfen, wenn die gleichnamige Methode dort aufgerufen
   * wird.
   * 
   * @param doc
   *          Das Dokument f�r das die Druckereinstellung gelten sollen.
   * @param onlyOnce
   *          Gibt an, dass der Dialog nur beim ersten Aufruf (aus Sicht eines
   *          Dokuments) der Methode angezeigt wird. Wurde bereits vor dem
   *          Aufruf ein PrintSetup-Dialog gestartet, so �ffnet sich der Dialog
   *          nicht und die Methode endet ohne Aktion.
   * @param unlockActionListener
   *          Der unlockActionListener wird immer aufgerufen, wenn sich doit()
   *          fertig ist.
   */
  public static void handleShowPrinterSetupDialog(XTextDocument doc,
      boolean onlyOnce, ActionListener unlockActionListener)
  {
    handle(new OnShowPrinterSetup(doc, onlyOnce, unlockActionListener));
  }

  private static class OnShowPrinterSetup extends BasicEvent
  {
    private XTextDocument doc;

    private boolean onlyOnce;

    private ActionListener listener;

    public OnShowPrinterSetup(XTextDocument doc, boolean onlyOnce,
        ActionListener listener)
    {
      this.doc = doc;
      this.onlyOnce = onlyOnce;
      this.listener = listener;
    }

    protected void doit() throws WollMuxFehlerException
    {
      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);

      // dialog nicht anzeigen, wenn er bereits einmal angezeigt wurde und
      // onlyOnce gesetzt ist.
      if (model.printSettingsDone && onlyOnce)
      {
        if (listener != null) listener.actionPerformed(null);
        return;
      }

      // Dialog anzeigen:
      try
      {
        com.sun.star.util.URL url = UNO
            .getParsedUNOUrl(DispatchHandler.DISP_unoPrinterSetup);
        XNotifyingDispatch disp = UNO.XNotifyingDispatch(WollMuxSingleton
            .getDispatchForModel(UNO.XModel(doc), url));

        if (disp != null)
        {
          setLock();
          disp.dispatchWithNotification(
              url,
              new PropertyValue[] {},
              unlockDispatchResultListener);
          waitForUnlock();
        }
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }

      // Leider gibt der Dialog keine verwertbare Statusinformation zur�ck, die
      // beschreibt ob der Dialog mit "Abbrechen" oder mit "OK" beendet wurde.
      // Dann w�rde ich printSettingsDone nur dann auf true sezten, wenn der
      // Dialog mit OK beendet wurde...
      model.printSettingsDone = true;

      if (listener != null) listener.actionPerformed(null);

      stabilize();
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", onlyOnce="
             + onlyOnce
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das daf�r sorgt, dass im Textdokument doc
   * das Formularfeld mit der ID id auf den Wert value gesetzt wird. Ist das
   * Dokument ein Formulardokument (also mit einer angezeigten FormGUI), so wird
   * die �nderung �ber die FormGUI vorgenommen, die zugleich daf�r sorgt, dass
   * von id abh�ngige Formularfelder mit angepasst werden. Besitzt das Dokument
   * keine Formularbeschreibung, so wird der Wert direkt gesetzt, ohne
   * �bh�ngigkeiten zu beachten. Nach der erfolgreichen Ausf�hrung aller
   * notwendigen Anpassungen wird der unlockActionListener benachrichtigt.
   * 
   * Das Event wird aus der Implementierung von XPrintModel (siehe
   * TextDocumentModel) geworfen, wenn dort die Methode setFormValue aufgerufen
   * wird.
   * 
   * @param doc
   *          Das Dokument, in dem das Formularfeld mit der ID id neu gesetzt
   *          werden soll.
   * @param id
   *          Die ID des Formularfeldes, dessen Wert ver�ndert werden soll. Ist
   *          die FormGUI aktiv, so werden auch alle von id abh�ngigen
   *          Formularwerte neu gesetzt.
   * @param value
   *          Der neue Wert des Formularfeldes id
   * @param unlockActionListener
   *          Der unlockActionListener wird immer informiert, wenn alle
   *          notwendigen Anpassungen durchgef�hrt wurden.
   */
  public static void handleSetFormValueViaPrintModel(XTextDocument doc,
      String id, String value, ActionListener unlockActionListener)
  {
    handle(new OnSetFormValueViaPrintModel(doc, id, value, unlockActionListener));
  }

  private static class OnSetFormValueViaPrintModel extends BasicEvent
  {
    private XTextDocument doc;

    private String id;

    private String value;

    private final ActionListener listener;

    public OnSetFormValueViaPrintModel(XTextDocument doc, String id,
        String value, ActionListener listener)
    {
      this.doc = doc;
      this.id = id;
      this.value = value;
      this.listener = listener;
    }

    protected void doit() throws WollMuxFehlerException
    {
      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);

      FormModel formModel = model.getFormModel();
      if (formModel != null)
      {
        // Werte �ber den FormController (den das FormModel kennt) setzen lassen
        // (damit sind auch automatisch alle Abh�ngigkeiten richtig aufgel�st)
        formModel.setValue(id, value, new ActionListener()
        {
          public void actionPerformed(ActionEvent arg0)
          {
            handleSetFormValueViaPrintModelFinished(listener);
          }
        });
      }
      else
      {
        // Werte selber setzen:
        model.setFormFieldValue(id, value);
        model.updateFormFields(id);
        if (listener != null) listener.actionPerformed(null);
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", id='"
             + id
             + "', value='"
             + value
             + "')";
    }
  }

  // *******************************************************************************************

  /**
   * Sammelt alle Formularfelder des Dokuments model auf, die nicht von
   * WollMux-Kommandos umgeben sind, jedoch trotzdem vom WollMux verstanden und
   * bef�llt werden (derzeit c,s,s,t,textfield,Database-Felder).
   * 
   * Das Event wird aus der Implementierung von XPrintModel (siehe
   * TextDocumentModel) geworfen, wenn dort die Methode
   * collectNonWollMuxFormFields aufgerufen wird.
   * 
   * @param model
   * @param unlockActionListener
   *          Der unlockActionListener wird immer informiert, wenn alle
   *          notwendigen Anpassungen durchgef�hrt wurden.
   */
  public static void handleCollectNonWollMuxFormFieldsViaPrintModel(
      TextDocumentModel model, ActionListener listener)
  {
    handle(new OnCollectNonWollMuxFormFieldsViaPrintModel(model, listener));
  }

  private static class OnCollectNonWollMuxFormFieldsViaPrintModel extends
      BasicEvent
  {
    private TextDocumentModel model;

    private ActionListener listener;

    public OnCollectNonWollMuxFormFieldsViaPrintModel(TextDocumentModel model,
        ActionListener listener)
    {
      this.model = model;
      this.listener = listener;
    }

    protected void doit() throws WollMuxFehlerException
    {
      model.collectNonWollMuxFormFields();

      stabilize();
      listener.actionPerformed(null);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + model + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Dieses WollMuxEvent ist das Gegenst�ck zu handleSetFormValueViaPrintModel
   * und wird dann erzeugt, wenn nach einer �nderung eines Formularwertes -
   * gesteuert durch die FormGUI - alle abh�ngigen Formularwerte angepasst
   * wurden. In diesem Fall ist die einzige Aufgabe dieses Events, den
   * unlockActionListener zu informieren, den handleSetFormValueViaPrintModel()
   * nicht selbst informieren konnte.
   * 
   * Das Event wird aus der Implementierung vom
   * OnSetFormValueViaPrintModel.doit() erzeugt, wenn Feld�nderungen �ber die
   * FormGUI laufen.
   * 
   * @param unlockActionListener
   *          Der zu informierende unlockActionListener.
   */
  public static void handleSetFormValueViaPrintModelFinished(
      ActionListener unlockActionListener)
  {
    handle(new OnSetFormValueViaPrintModelFinished(unlockActionListener));
  }

  private static class OnSetFormValueViaPrintModelFinished extends BasicEvent
  {
    private ActionListener listener;

    public OnSetFormValueViaPrintModelFinished(
        ActionListener unlockActionListener)
    {
      this.listener = unlockActionListener;
    }

    protected void doit() throws WollMuxFehlerException
    {
      if (listener != null) listener.actionPerformed(null);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "()";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, das ein Textbaustein �ber
   * den Textbaustein-Bezeichner direkt ins Dokument eingef�gt wird. Mit
   * reprocess wird �bergeben, wann die Dokumentenkommandos ausgewertet werden
   * soll. Mir reprocess = true sofort.
   * 
   * Das Event wird von WollMux.dispatch(...) geworfen z.B �ber Druck eines
   * Tastenkuerzels oder Druck auf den Knopf der OOo-Symbolleiste ein
   * "wollmux:TextbausteinEinfuegen" dispatch erfolgte.
   */
  public static void handleTextbausteinEinfuegen(TextDocumentModel model,
      boolean reprocess)
  {
    handle(new OnTextbausteinEinfuegen(model, reprocess));
  }

  private static class OnTextbausteinEinfuegen extends BasicEvent
  {
    private TextDocumentModel model;

    private boolean reprocess;

    public OnTextbausteinEinfuegen(TextDocumentModel model, boolean reprocess)
    {
      this.model = model;
      this.reprocess = reprocess;

    }

    protected void doit() throws WollMuxFehlerException
    {
      XTextCursor viewCursor = model.getViewCursor();
      boolean atLeastOne = TextModule.createInsertFragFromIdentifier(
          model.doc,
          viewCursor,
          reprocess);

      if (reprocess)
      {
        if (atLeastOne)
        {
          handleReprocessTextDocument(model);
        }
        else
        {
          WollMuxSingleton.showInfoModal(
              "WollMux-Fehler",
              "An der Einf�gestelle konnte kein Textbaustein gefunden werden.");
        }
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "("
             + model
             + ", "
             + reprocess
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, das der n�chste
   * Platzhalter ausgehend vom Cursor angesprungen wird * Das Event wird von
   * WollMux.dispatch(...) geworfen z.B �ber Druck eines Tastenkuerzels oder
   * Druck auf den Knopf der OOo-Symbolleiste ein
   * "wollmux:PlatzhalterAnspringen" dispatch erfolgte.
   */
  public static void handleJumpToPlaceholder(TextDocumentModel model)
  {
    handle(new OnJumpToPlaceholder(model));
  }

  private static class OnJumpToPlaceholder extends BasicEvent
  {
    private TextDocumentModel model;

    public OnJumpToPlaceholder(TextDocumentModel model)
    {
      this.model = model;
    }

    protected void doit() throws WollMuxFehlerException
    {
      XTextCursor viewCursor = model.getViewCursor();

      try
      {
        TextModule.jumpPlaceholders(model.doc, viewCursor);
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }

      stabilize();
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + model + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, das die n�chste Marke
   * 'setJumpMark' angesprungen werden soll. Wird im
   * DocumentCommandInterpreter.DocumentExpander.fillPlaceholders aufgerufen
   * wenn nach dem Einf�gen von Textbausteine keine Einf�gestelle vorhanden ist
   * aber eine Marke 'setJumpMark'
   */
  public static void handleJumpToMark(XTextDocument doc, boolean msg)
  {
    handle(new OnJumpToMark(doc, msg));
  }

  private static class OnJumpToMark extends BasicEvent
  {
    private XTextDocument doc;

    private boolean msg;

    public OnJumpToMark(XTextDocument doc, boolean msg)
    {
      this.doc = doc;
      this.msg = msg;
    }

    protected void doit() throws WollMuxFehlerException
    {

      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModel(doc);

      XTextCursor viewCursor = model.getViewCursor();
      if (viewCursor == null) return;

      DocumentCommand cmd = model.getFirstJumpMark();

      if (cmd != null)
      {
        try
        {
          XTextRange range = cmd.getTextRange();
          if (range != null) viewCursor.gotoRange(range, false);
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }

        boolean modified = model.getDocumentModified();
        cmd.markDone(true);
        model.setDocumentModified(modified);

        model.getDocumentCommands().update();

      }
      else
      {
        if (msg)
        {
          WollMuxSingleton.showInfoModal(
              "WollMux",
              "Kein Platzhalter und keine Marke 'setJumpMark' vorhanden!");
        }
      }

      stabilize();
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "(#"
             + doc.hashCode()
             + ", "
             + msg
             + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass die
   * Seriendruckfunktion des WollMux gestartet werden soll.
   * 
   * Das Event wird �ber den DispatchHandler aufgerufen, wenn z.B. �ber das Men�
   * "Extras->Seriendruck (WollMux)" die dispatch-url wollmux:Seriendruck
   * abgesetzt wurde.
   */
  public static void handleSeriendruck(TextDocumentModel model)
  {
    handle(new OnSeriendruck(model));
  }

  private static class OnSeriendruck extends BasicEvent
  {
    private TextDocumentModel model;

    public OnSeriendruck(TextDocumentModel model)
    {
      this.model = model;
    }

    protected void doit() throws WollMuxFehlerException
    {
      // muss in eigenem Thread laufen, da sich das PrintModel bereits mit dem
      // WollMux-Eventhandler synchronisiert.
      final XPrintModel pmod = model.getPrintModel();
      Thread t = new Thread(new Runnable()
      {
        public void run()
        {
          StandardPrint.superMailMerge(pmod);
        }
      });
      t.start();
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + model + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Der Handler f�r das Drucken eines TextDokuments f�hrt in Abh�ngigkeit von
   * der Existenz von Serienbrieffeldern und Druckfunktion die entsprechenden
   * Aktionen aus.
   * 
   * Das Event wird �ber den DispatchHandler aufgerufen, wenn z.B. �ber das Men�
   * "Datei->Drucken" oder �ber die Symbolleiste die dispatch-url .uno:Print
   * bzw. .uno:PrintDefault abgesetzt wurde.
   */
  public static void handlePrint(TextDocumentModel model, XDispatch origDisp,
      com.sun.star.util.URL origUrl, PropertyValue[] origArgs)
  {
    handle(new OnPrint(model, origDisp, origUrl, origArgs));
  }

  private static class OnPrint extends BasicEvent
  {
    private TextDocumentModel model;

    private XDispatch origDisp;

    private com.sun.star.util.URL origUrl;

    private PropertyValue[] origArgs;

    public OnPrint(TextDocumentModel model, XDispatch origDisp,
        com.sun.star.util.URL origUrl, PropertyValue[] origArgs)
    {
      this.model = model;
      this.origDisp = origDisp;
      this.origUrl = origUrl;
      this.origArgs = origArgs;
    }

    protected void doit() throws WollMuxFehlerException
    {
      boolean hasPrintFunction = model.getPrintFunctionName() != null;
      boolean hasMailMergeFields = model.hasMailMergeFields();

      if (!hasMailMergeFields && !hasPrintFunction)
      {
        // Forward auf Standardfunktion
        if (origDisp != null) origDisp.dispatch(origUrl, origArgs);
      }
      else if (!hasMailMergeFields && hasPrintFunction)
      {
        // Druckfunktion aufrufen
        handleExecutePrintFunction(model);
      }
      else if (hasMailMergeFields && !hasPrintFunction)
      {
        // "Was tun?"-Dialog
        if (!mailmergeWrapperDialog())
          if (origDisp != null) origDisp.dispatch(origUrl, origArgs);
      }
      else if (hasMailMergeFields && hasPrintFunction)
      {
        // TODO: hier wird sp�ter die kombinierte Seriendruck+Printfunction
        // Funktionalit�t integriert.
        if (!mailmergeWrapperDialog()) handleExecutePrintFunction(model);
      }
    }

    /**
     * Liefert true, wenn der Dialog die Anfrage vollst�ndig bearbeiten konnte
     * oder false, wenn die Standard-Druckfunktion aufgerufen werden soll.
     * 
     * @return
     */
    private boolean mailmergeWrapperDialog()
    {
      final String SERIENBRIEF_WOLLMUX = "Seriendruck (WollMux)";
      final String SERIENBRIEF_DEFAULT = "Seriendruck (Standard)";
      final String PRINT_DEFAULT = "Drucken";

      JOptionPane pane = new JOptionPane(
          "Das Dokument enth�lt Serienbrief-Felder. Was wollen Sie tun?",
          javax.swing.JOptionPane.INFORMATION_MESSAGE);
      pane.setOptions(new String[] {
                                    SERIENBRIEF_WOLLMUX,
                                    SERIENBRIEF_DEFAULT,
                                    PRINT_DEFAULT });
      JDialog dialog = pane.createDialog(null, "Seriendruck");
      dialog.setAlwaysOnTop(true);
      dialog.setVisible(true);

      Object value = pane.getValue();

      if (SERIENBRIEF_WOLLMUX.equals(value))
      {
        handleSeriendruck(model);
        return true;
      }

      if (SERIENBRIEF_DEFAULT.equals(value))
      {
        UNO.dispatch(model.doc, ".uno:MergeDialog");
        return true;
      }

      if (PRINT_DEFAULT.equals(value))
      {
        return false;
      }

      return true;
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + model + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass der FormController
   * (der zeitgleich mit einer FormGUI zum TextDocument model gestartet wird)
   * vollst�ndig initialisiert ist und notwendige Aktionen wie z.B. das
   * Zur�cksetzen des Modified-Status des Dokuments durchgef�hrt werden k�nnen.
   * Vor dem Zur�cksetzen des Modified-Status, wird auf die erste Seite des
   * Dokuments gesprungen.
   * 
   * Das Event wird vom FormModel erzeugt, wenn es vom FormController eine
   * entsprechende Nachricht erh�lt.
   */
  public static void handleFormControllerInitCompleted(TextDocumentModel model)
  {
    handle(new OnFormControllerInitCompleted(model));
  }

  private static class OnFormControllerInitCompleted extends BasicEvent
  {
    private TextDocumentModel model;

    public OnFormControllerInitCompleted(TextDocumentModel model)
    {
      this.model = model;
    }

    protected void doit() throws WollMuxFehlerException
    {
      // Springt zum Dokumentenanfang
      try
      {
        model.getViewCursor().gotoRange(model.doc.getText().getStart(), false);
      }
      catch (java.lang.Exception e)
      {
        Logger.debug(e);
      }

      // Beim �ffnen eines Formulars werden viele �nderungen am Dokument
      // vorgenommen (z.B. das Setzen vieler Formularwerte), ohne dass jedoch
      // eine entsprechende Benutzerinteraktion stattgefunden hat. Der
      // Modified-Status des Dokuments wird daher zur�ckgesetzt, damit nur
      // wirkliche Interaktionen durch den Benutzer modified=true setzen.
      model.setDocumentModified(false);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + model + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMux-Event, in dem gepr�ft wird, ob der WollMux
   * korrekt installiert ist und keine Doppel- oder Halbinstallationen
   * vorliegen. Ist der WollMux fehlerhaft installiert, erscheint eine
   * Fehlermeldung mit entsprechenden Hinweisen.
   * 
   * Das Event wird geworfen, wenn der WollMux startet oder wenn ein der
   * Sachleitenden Verf�gungen eingef�gt werden, bzw. eine bestehende Ziffer
   * gel�scht werden soll.
   */
  public static void handleCheckInstallation()
  {
    handle(new OnCheckInstallation());
  }

  private static class OnCheckInstallation extends BasicEvent
  {
    protected void doit() throws WollMuxFehlerException
    {
      // Standardwerte f�r den Warndialog:
      boolean showdialog = true;
      String title = "Mehrfachinstallation des WollMux";
      String msg = "Der WollMux ist mehrfach auf Ihrem System installiert.";
      String logMsg = msg;

      // Abschnitt Dialoge/MehrfachinstallationWarndialog auswerten
      try
      {
        ConfigThingy warndialog = WollMuxSingleton.getInstance()
            .getWollmuxConf().query("Dialoge").query(
                "MehrfachinstallationWarndialog").getLastChild();
        try
        {
          msg = warndialog.get("MSG").toString();
        }
        catch (NodeNotFoundException e)
        {
          showdialog = false;
        }
        try
        {
          title = warndialog.get("TITLE").toString();
        }
        catch (NodeNotFoundException e)
        {
        }
      }
      catch (NodeNotFoundException e)
      {
        // Ist der Abschnitt nicht vorhanden, so greifen Standardwerte.
      }

      // Infos der Installationen einlesen.
      HashMap wmInsts = getInstallations();

      // Variablen recentInstPath / recentInstLastModified bestimmen
      String recentInstPath = "";
      Date recentInstLastModified = null;
      for (Iterator iter = wmInsts.keySet().iterator(); iter.hasNext();)
      {
        String path = (String) iter.next();
        Date d = (Date) wmInsts.get(path);
        if (recentInstLastModified == null
            || d.compareTo(recentInstLastModified) > 0)
        {
          recentInstLastModified = d;
          recentInstPath = path;
        }
      }

      // Variable wrongInstList bestimmen:
      String otherInstsList = "";
      for (Iterator iter = wmInsts.keySet().iterator(); iter.hasNext();)
      {
        String path = (String) iter.next();
        if (path.equals(recentInstPath)) continue;
        otherInstsList += "- " + path + "\n";
      }

      // recentInstlastModifiedDate vergleichen mit dem Zeitstempel der
      // Installation, die in der letzten Pr�fung gefunden wurde.
      boolean newerInstallationDetected = false;
      boolean firstCheck = false;

      WollMuxSingleton mux = WollMuxSingleton.getInstance();
      Date oldCurrentInstLastModified = mux.getCurrentWollMuxInstallationDate();
      if (oldCurrentInstLastModified == null)
      {
        firstCheck = true;
        mux.setCurrentWollMuxInstallationDate(recentInstLastModified);
        oldCurrentInstLastModified = recentInstLastModified;
      }
      if (recentInstLastModified != null)
        newerInstallationDetected = oldCurrentInstLastModified
            .compareTo(recentInstLastModified) < 0;

      // Im Fehlerfall Dialog und Fehlermeldung bringen.
      if ((wmInsts.size() > 1 && firstCheck) || newerInstallationDetected)
      {

        // Variablen in msg evaluieren:
        DateFormat f = DateFormat.getDateInstance();
        msg = msg.replaceAll("\\$\\{RECENT_INST_PATH\\}", recentInstPath);
        msg = msg.replaceAll("\\$\\{RECENT_INST_LAST_MODIFIED\\}", f
            .format(recentInstLastModified));
        msg = msg.replaceAll("\\$\\{OTHER_INSTS_LIST\\}", otherInstsList);

        logMsg += "\nDie juengste WollMux-Installation liegt unter:\n- "
                  + recentInstPath
                  + "\nAusserdem wurden folgende WollMux-Installationen gefunden:\n"
                  + otherInstsList;
        Logger.error(logMsg);

        if (showdialog) WollMuxSingleton.showInfoModal(title, msg, 0);
      }
    }

    /**
     * Liefert eine HashMap mit den aktuell auf dem System vorhandenen
     * WollMux-Installationen, die in den Sch�ssel/Wert-Paaren
     * Pfad/Modified-Datum belegt sind.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    private HashMap getInstallations()
    {
      HashMap wmInstallations = new HashMap();

      // Installationspfade der Pakete bestimmen:
      String myPath = null; // user-Pfad
      String oooPath = null; // shared-Pfad

      try
      {
        XStringSubstitution xSS = UNO.XStringSubstitution(UNO
            .createUNOService("com.sun.star.util.PathSubstitution"));

        // Benutzerinstallationspfad LiMux =
        // /home/<Benutzer>/.openoffice.org2/user
        // Benutzerinstallationspfad Windows 2000 C:/Dokumente und
        // Einstellungen/<Benutzer>/Anwendungsdaten/OpenOffice.org2/user
        myPath = xSS.substituteVariables(
            "$(user)/uno_packages/cache/uno_packages/",
            true);
        // Sharedinstallationspfad LiMux /opt/openoffice.org2.0/
        // Sharedinstallationspfad Windows C:/Programme/OpenOffice.org<version>
        oooPath = xSS.substituteVariables(
            "$(inst)/share/uno_packages/cache/uno_packages/",
            true);
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
        return wmInstallations;
      }

      if (myPath == null || oooPath == null)
      {
        Logger
            .error("Bestimmung der Installationspfade f�r das WollMux-Paket fehlgeschlagen.");
        return wmInstallations;
      }

      findWollMuxInstallations(wmInstallations, myPath);
      findWollMuxInstallations(wmInstallations, oooPath);

      return wmInstallations;
    }

    /**
     * Sucht im �bergebenen Pfad path nach Verzeichnissen die WollMux.uno.pkg
     * enthalten und f�gt die Schl�ssel-/Wertpaare
     * <VerzeichnisDasWollMuxEnth�lt>/<DatumDesVerzeichnis> zur �bergebenen
     * HashMap wmInstallations
     * 
     * @param wmInstallations
     *          HashMap, in der gefundene WollMux-Installationen abgelegt werden
     *          als Pfas(als String) und Datum(als Date).
     * @param path
     *          Der Pfad in dem nach WollMux-Installationen gesucht werden soll.
     * 
     * @author Bettina Bauer (D-III-ITD-5.1), Christoph Lutz (D-III-ITD-5.1)
     */
    private static void findWollMuxInstallations(HashMap wmInstallations,
        String path)
    {
      URI uriPath;
      uriPath = null;
      try
      {
        uriPath = new URI(path);
      }
      catch (URISyntaxException e)
      {
        Logger.error(e);
        return;
      }

      File[] installedPackages = new File(uriPath).listFiles();
      if (installedPackages != null)
      {
        // iterieren �ber die Installationsverzeichnisse mit automatisch
        // generierten Namen (z.B. 31GFBd_)
        for (int i = 0; i < installedPackages.length; i++)
        {
          if (installedPackages[i].isDirectory())
          {
            File dir = installedPackages[i];
            File[] dateien = dir.listFiles();
            for (int j = 0; j < dateien.length; j++)
            {
              // Wenn das Verzeichnis WollMux.uno.pkg enth�lt, speichern des
              // Verzeichnisnames und des Verzeichnisdatum in einer HashMap
              if (dateien[j].isDirectory()
                  && dateien[j].getName().startsWith("WollMux."))
              {
                // Name des Verzeichnis in dem sich WollMux.uno.pkg befindet
                String directoryName = dateien[j].getAbsolutePath();
                // Datum des Verzeichnis in dem sich WollMux.uno.pkg befindet
                Date directoryDate = new Date(dateien[j].lastModified());
                wmInstallations.put(directoryName, directoryDate);
              }
            }
          }
        }
      }
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "()";
    }
  }

  // *******************************************************************************************
  // Globale Helper-Methoden

  private static ConfigThingy requireLastSection(ConfigThingy cf,
      String sectionName) throws ConfigurationErrorException
  {
    try
    {
      return cf.query(sectionName).getLastChild();
    }
    catch (NodeNotFoundException e)
    {
      throw new ConfigurationErrorException(
          "Der Schl�ssel '"
              + sectionName
              + "' fehlt in der Konfigurationsdatei.", e);
    }
  }
}
