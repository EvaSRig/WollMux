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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.sun.star.awt.DeviceInfo;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.frame.FrameSearchFlag;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;
import com.sun.star.uno.RuntimeException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XURLTransformer;
import com.sun.star.view.DocumentZoomType;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.FormFieldFactory.FormField;
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

/**
 * Erm�glicht die Einstellung neuer WollMuxEvents in die EventQueue.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class WollMuxEventHandler
{

  /**
   * Interface f�r die Events, die dieser EventHandler abarbeitet.
   */
  public interface WollMuxEvent
  {
    /**
     * Startet die Ausf�hrung des Events und darf nur aus dem EventProcessor
     * aufgerufen werden.
     */
    public boolean process();

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
   * Hilfsklasse, die es erm�glicht, UNO-Componenten in HashMaps abzulegen; der
   * Vergleich zweier HashableComponents mit equals(...) verwendet dazu den
   * sicheren UNO-Vergleich UnoRuntime.areSame(...) und die Methode hashCode
   * wird direkt an das UNO-Objekt weitergeleitet.
   * 
   * @author lut
   */
  public static class HashableComponent
  {
    private Object compo;

    public HashableComponent(XComponent compo)
    {
      this.compo = compo;
    }

    public int hashCode()
    {
      if (compo != null) return compo.hashCode();
      return 0;
    }

    public boolean equals(Object b)
    {
      if (b != null && b instanceof HashableComponent)
      {
        HashableComponent other = (HashableComponent) b;
        return UnoRuntime.areSame(this.compo, other.compo);
      }
      return false;
    }
  }

  /**
   * Dient als Basisklasse f�r konkrete Event-Implementierungen.
   */
  private static class BasicEvent implements WollMuxEvent
  {

    /**
     * Diese Method ist f�r die Ausf�hrung des Events zust�ndig. Nach der
     * Bearbeitung entscheidet der R�ckgabewert ob unmittelbar die Bearbeitung
     * des n�chsten Events gestartet werden soll oder ob das GUI blockiert
     * werden soll bis das n�chste actionPerformed-Event beim EventProcessor
     * eintrifft.
     * 
     * @return einer der Werte <code>EventProcessor.processNextEvent</code>
     *         oder <code>EventProcessor.waitForGUIReturn</code>.
     */
    public boolean process()
    {
      Logger.debug("Process WollMuxEvent " + this.toString());
      try
      {
        return doit();
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
      return EventProcessor.processTheNextEvent;
    }

    /**
     * Logged die �bergebene Fehlermeldung nach Logger.error() und erzeugt ein
     * Dialogfenster mit der Fehlernachricht.
     */
    private void errorMessage(Throwable t)
    {
      Logger.error(t);
      String msg = "";
      if (t.getMessage() != null) msg += t.getMessage();
      Throwable c = t.getCause();
      if (c != null)
      {
        msg += "\n\n" + c;
      }
      showInfoModal("WollMux-Fehler", msg);
    }

    /**
     * Jede abgeleitete Event-Klasse sollte die Methode doit redefinieren, in
     * der die eigentlich event-Bearbeitung erfolgt. Die Methode doit muss alle
     * auftretenden Exceptions selbst behandeln, Fehler die jedoch
     * benutzersichtbar in einem Dialog angezeigt werden sollen, k�nnen �ber
     * eine WollMuxFehlerException nach oben weitergereicht werden.
     * 
     * @return EventProcessor.processTheNextEvent oder
     *         EventProcessor.waitForGUIReturn
     */
    protected boolean doit() throws WollMuxFehlerException
    {
      return EventProcessor.processTheNextEvent;
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

    /**
     * Wenn in dem �bergebenen Vector mit FormField-Elementen ein
     * nicht-transformiertes Feld vorhanden ist, so wird das erste
     * nicht-transformierte Feld zur�ckgegeben, ansonsten wird das erste
     * transformierte Feld zur�ckgegeben, oder null, falls der Vector keine
     * Elemente enth�lt.
     * 
     * @param formFields
     *          Vektor mit FormField-Elementen
     * @return Ein FormField Element, wobei untransformierte Felder bevorzugt
     *         werden.
     */
    protected static FormField preferUntransformedFormField(Vector formFields)
    {
      Iterator iter = formFields.iterator();
      FormField field = null;
      while (iter.hasNext())
      {
        FormField f = (FormField) iter.next();
        if (field == null) field = f;
        if (!f.hasTrafo()) return f;
      }
      return field;
    }

    public String toString()
    {
      return this.getClass().getSimpleName();
    }
  }

  /**
   * Stellt das WollMuxEvent event in die EventQueue des EventProcessors.
   * 
   * @param event
   */
  private static void handle(WollMuxEvent event)
  {
    WollMuxSingleton.getInstance().getEventProcessor().addEvent(event);
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
    protected boolean doit() throws WollMuxFehlerException
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

      // Dialog starten:
      try
      {
        new AbsenderAuswaehlen(whoAmIconf, PALconf, ADBconf, mux
            .getDatasourceJoiner(), mux.getEventProcessor());
      }
      catch (java.lang.Exception e)
      {
        throw new CantStartDialogException(e);
      }
      return EventProcessor.waitForGUIReturn;
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
    protected boolean doit() throws WollMuxFehlerException
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

      // Dialog starten:
      try
      {
        new PersoenlicheAbsenderlisteVerwalten(PALconf, ADBconf, mux
            .getDatasourceJoiner(), mux.getEventProcessor());
      }
      catch (java.lang.Exception e)
      {
        throw new CantStartDialogException(e);
      }

      return EventProcessor.waitForGUIReturn;
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
  public static void handleFunctionDialogShow(XComponent doc, String dialogName)
  {
    handle(new OnFunctionDialogShow(doc, dialogName));
  }

  private static class OnFunctionDialogShow extends BasicEvent
  {
    private XComponent doc;

    private String dialogName;

    private OnFunctionDialogShow(XComponent doc, String dialogName)
    {
      this.doc = doc;
      this.dialogName = dialogName;
    }

    protected boolean doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      Dialog dialog = mux.getFunctionDialogs().get(dialogName);

      if (dialog != null)
      {
        try
        {
          // Dialoginstanz erzeugen und anzeigen
          Dialog dialogInst = dialog.instanceFor(new HashMap());
          dialogInst.show(
              mux.getEventProcessor(),
              mux.getGlobalFunctions(),
              mux.getFunctionDialogs());

          // DialogContext f�r die Weiterverarbeitung in
          // OnFunctionDialogSelectDone sichern.
          OnFunctionDialogSelectDone.dialogContexts.add(new DialogContext(doc,
              dialogInst));

          return EventProcessor.waitForGUIReturn;
        }
        catch (ConfigurationErrorException e)
        {
          Logger.error(e);
        }
      }
      else
      {
        Logger.error("Funktionsdialog '"
                     + dialogName
                     + "' ist nicht definiert.");
      }

      return EventProcessor.processTheNextEvent;
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(doc, o);
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "("
             + doc.hashCode()
             + ", '"
             + dialogName
             + "')";
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
  public static void handleFormularMax4000Show(XComponent doc)
  {
    handle(new OnFormularMax4000Show(doc));
  }

  private static class OnFormularMax4000Show extends BasicEvent
  {
    private XComponent doc;

    private OnFormularMax4000Show(XComponent doc)
    {
      this.doc = doc;
    }

    protected boolean doit() throws WollMuxFehlerException
    {
      XTextDocument text = UNO.XTextDocument(doc);
      if (text != null)
      {
        new FormularMax4000(text);
      }

      return EventProcessor.processTheNextEvent;
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(doc, o); // TODO Ist das korrekt so?
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + doc.hashCode() + "')";
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
  public static void handleFunctionDialogSelectDone()
  {
    handle(new OnFunctionDialogSelectDone());
  }

  private static class OnFunctionDialogSelectDone extends BasicEvent
  {
    private static Vector dialogContexts = new Vector();

    protected boolean doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      if (dialogContexts.size() > 0)
      {
        // Den DialogContext vom Stack nehmen:
        DialogContext dctx = (DialogContext) dialogContexts.remove(0);

        // Dem Dokument den Fokus geben, damit die �nderungen vom Benutzer
        // transparent mit verfolgt werden k�nnen.
        try
        {
          UNO.XModel(dctx.doc).getCurrentController().getFrame()
              .getContainerWindow().setFocus();
        }
        catch (java.lang.Exception e)
        {
          // keine Gef�hrdung des Ablaufs falls das nicht klappt.
        }

        // Alle Werte die der Funktionsdialog sicher zur�ck liefert werden in
        // das Dokument �bernommen.
        HashMap idToFormFields = mux.getIDToFormFieldsForDocument(dctx.doc);
        if (idToFormFields != null)
        {
          Collection schema = dctx.dialogInstance.getSchema();
          Iterator iter = schema.iterator();
          while (iter.hasNext())
          {
            String id = (String) iter.next();
            if (idToFormFields.containsKey(id))
            {
              Vector formFields = (Vector) idToFormFields.get(id);
              String value = dctx.dialogInstance.getData(id).toString();

              // Formularwerte der entsprechenden Formularfelder auf value
              // setzen.
              Iterator ffiter = formFields.iterator();
              while (ffiter.hasNext())
              {
                FormField formField = (FormField) ffiter.next();
                if (formField != null)
                  formField.setValue(value, mux.getGlobalFunctions());
              }
            }
          }
        }
      }
      return EventProcessor.processTheNextEvent;
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "()";
    }
  }

  private static class DialogContext
  {
    private XComponent doc;

    private Dialog dialogInstance;

    private DialogContext(XComponent doc, Dialog dialogInstance)
    {
      this.doc = doc;
      this.dialogInstance = dialogInstance;
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

    /**
     * Dieses Feld stellt ein Zwischenspeicher f�r Fragment-Urls dar, der
     * HashableComponent-Objekte (mit Dokument-Instanzen) auf
     * Fragment-URL-Listen mapped. Es wird dazu benutzt, im Fall eines
     * openTemplate-Befehls die urls der �bergebenen frag_id-Liste tempor�r zu
     * speichern. Das Event on_new/on_load holt sich die tempor�r gespeicherten
     * Argumente aus der hashMap und �bergibt sie dem WMCommandInterpreter.
     */
    private static HashMap docFragUrlsBuffer = new HashMap();

    public OnProcessTextDocument(XTextDocument xTextDoc)
    {
      this.xTextDoc = xTextDoc;
    }

    protected boolean doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      UnoService doc = new UnoService(xTextDoc);
      if (doc.supportsService("com.sun.star.text.TextDocument"))
      {
        // Close-Events aller Textdokumente abfangen.
        doc.xCloseable().addCloseListener(mux.getEventProcessor());

        // Konfigurationsabschnitt Textdokument verarbeiten:
        ConfigThingy tds = new ConfigThingy("Textdokument");
        try
        {
          tds = mux.getWollmuxConf().query("Fenster").query("Textdokument")
              .getLastChild();
          // Einstellungen setzen:
          setWindowViewSettings(xTextDoc, tds);
        }
        catch (NodeNotFoundException e)
        {
        }

        // Beim on_opendocument erzeugte frag_id-liste aus puffer holen.
        String[] fragUrls = (String[]) docFragUrlsBuffer
            .remove(new HashableComponent(doc.xComponent()));
        if (fragUrls == null) fragUrls = new String[] {};

        // M�gliche Aktionen f�r das neu ge�ffnete Dokument:
        boolean processNormalCommands = false;
        boolean processFormCommands = false;

        // Bestimmung des Dokumenttyps (openAsTemplate?):
        if (doc.xTextDocument() != null)
          processNormalCommands = (doc.xTextDocument().getURL() == null || doc
              .xTextDocument().getURL().equals(""));

        // Auswerten der Special-Bookmarks "WM(CMD 'setType' TYPE '...')"
        if (doc.xBookmarksSupplier() != null)
        {
          XNameAccess bookmarks = doc.xBookmarksSupplier().getBookmarks();
          if (bookmarks.hasByName(DocumentCommand.SETTYPE_normalTemplate))
          {
            processNormalCommands = true;
            processFormCommands = false;

            // Bookmark l�schen
            removeBookmark(doc, DocumentCommand.SETTYPE_normalTemplate);
          }
          else if (bookmarks
              .hasByName(DocumentCommand.SETTYPE_templateTemplate))
          {
            processNormalCommands = false;
            processFormCommands = false;

            // Bookmark l�schen
            removeBookmark(doc, DocumentCommand.SETTYPE_templateTemplate);
          }
          else if (bookmarks.hasByName(DocumentCommand.SETTYPE_formDocument))
          {
            processNormalCommands = false;
            processFormCommands = true;

            // Bookmark l�schen (wird sp�ter
            // DocumentCommandInterpreter.executeFormCommands() aber wieder
            // erzeugt, da Formulardokumente immer Formulardokumente bleiben)
            removeBookmark(doc, DocumentCommand.SETTYPE_formDocument);
          }
        }

        // Ausf�hrung der Dokumentkommandos
        if (processNormalCommands || processFormCommands)
        {
          DocumentCommandInterpreter dci = new DocumentCommandInterpreter(doc
              .xTextDocument(), mux);

          try
          {
            if (processNormalCommands) dci.executeTemplateCommands(fragUrls);

            if (processFormCommands || dci.isFormular())
            {
              // Zoom-Faktor des Abschnitts Fenster/Formular verarbeiten.
              try
              {
                String zoom = mux.getWollmuxConf().query("Fenster").query(
                    "Formular").getLastChild().get("ZOOM").toString();
                setDocumentZoom(xTextDoc, zoom);
              }
              catch (NodeNotFoundException e)
              {
                // ZOOM ist optional
              }
              catch (ConfigurationErrorException e)
              {
                Logger.error(e);
              }

              // Formularbearbeitung ausf�hren:
              dci.executeFormCommands();
            }
          }
          catch (java.lang.Exception e)
          {
            throw new WollMuxFehlerException(
                "Fehler bei der Dokumentbearbeitung.", e);
          }
        }
      }
      return EventProcessor.processTheNextEvent;
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(xTextDoc, o);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + xTextDoc.hashCode() + ")";
    }

    /**
     * Die Methode l�scht das Bookmark name aus dem Dokument doc und setzt den
     * document-modified-Status anschlie�end auf false, weil nur wirkliche
     * Benutzerinteraktion zur Speichern-Abfrage beim Schlie�en f�hren sollte.
     * 
     * @param doc
     * @param name
     */
    private static void removeBookmark(UnoService doc, String name)
    {
      try
      {
        if (doc.xBookmarksSupplier() != null)
        {
          Bookmark b = new Bookmark(name, doc.xBookmarksSupplier());
          b.remove();
        }
      }
      catch (NoSuchElementException e)
      {
        Logger.error(e);
      }

      // So tun als ob das Dokument (durch das L�schen des Bookmarks) nicht
      // ver�ndert worden w�re:
      if (doc.xModifiable() != null) try
      {
        doc.xModifiable().setModified(false);
      }
      catch (PropertyVetoException e)
      {
        Logger.error(e);
      }
    }

    /**
     * Diese Methode liest die (optionalen) Attribute X, Y, WIDTH, HEIGHT und
     * ZOOM aus dem �bergebenen Konfigurations-Abschnitt settings und setzt die
     * Fenstereinstellungen der Komponente compo entsprechend um. Bei den
     * P�rchen X/Y bzw. SIZE/WIDTH m�ssen jeweils beide Komponenten im
     * Konfigurationsabschnitt angegeben sein.
     * 
     * @param compo
     *          Die Komponente, deren Fenstereinstellungen gesetzt werden sollen
     * @param settings
     *          der Konfigurationsabschnitt, der X, Y, WIDHT, HEIGHT und ZOOM
     *          als direkte Kinder enth�lt.
     */
    private static void setWindowViewSettings(XComponent compo,
        ConfigThingy settings)
    {
      // Fenster holen (zum setzen der Fensterposition und des Zooms)
      XWindow window = null;
      try
      {
        window = UNO.XModel(compo).getCurrentController().getFrame()
            .getContainerWindow();
      }
      catch (java.lang.Exception e)
      {
      }

      // Insets bestimmen (Rahmenma�e des Windows)
      int insetLeft = 0, insetTop = 0, insetRight = 0, insetButtom = 0;
      if (UNO.XDevice(window) != null)
      {
        DeviceInfo di = UNO.XDevice(window).getInfo();
        insetButtom = di.BottomInset;
        insetTop = di.TopInset;
        insetRight = di.RightInset;
        insetLeft = di.LeftInset;
      }

      // Position setzen:
      try
      {
        int xPos = new Integer(settings.get("X").toString()).intValue();
        int yPos = new Integer(settings.get("Y").toString()).intValue();
        if (window != null)
        {
          window.setPosSize(
              xPos + insetLeft,
              yPos + insetTop,
              0,
              0,
              PosSize.POS);
        }
      }
      catch (java.lang.Exception e)
      {
      }
      // Dimensions setzen:
      try
      {
        int width = new Integer(settings.get("WIDTH").toString()).intValue();
        int height = new Integer(settings.get("HEIGHT").toString()).intValue();
        if (window != null)
          window.setPosSize(
              0,
              0,
              width - insetLeft - insetRight,
              height - insetTop - insetButtom,
              PosSize.SIZE);
      }
      catch (java.lang.Exception e)
      {
      }

      // Zoom setzen:
      try
      {
        setDocumentZoom(UNO.XModel(compo), settings.get("ZOOM").toString());
      }
      catch (NodeNotFoundException e)
      {
        // ZOOM ist optional
      }
      catch (ConfigurationErrorException e)
      {
        Logger.error(e);
      }
    }
  }

  // *******************************************************************************************

  /**
   * Dieses Event sorgt daf�r, dass die dispose() des �bergebenen FormModels
   * model im EventProcessor-Thread ausgef�hrt wird. Die dispose()-Methode darf
   * nicht ausserhalb des EventProcessor-Threads ausgef�hrt werden.
   * 
   * @param model
   *          das zu disposende FormModel
   */
  public static void handleDisposeFormModel(FormModel model)
  {
    handle(new OnDisposeFormModel(model));
  }

  private static class OnDisposeFormModel extends BasicEvent
  {
    FormModel formModel;

    public OnDisposeFormModel(FormModel model)
    {
      this.formModel = model;
    }

    protected boolean doit()
    {
      if (formModel != null) formModel.dispose();

      return EventProcessor.processTheNextEvent;
    }

    public boolean requires(Object o)
    {
      return formModel.equals(o);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + formModel + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, welches daf�r sorgt, dass ein Dokument
   * ge�ffnet wird.
   * 
   * @param fragIDs
   *          Ein Vector mit fragIDs, wobei das erste Element die FRAG_ID des zu
   *          �ffnenden Dokuments beinhalten muss. Weitere Elemente werden in
   *          eine Liste zusammengefasst und als Parameter f�r das
   *          Dokumentkommando insertContent verwendet.
   * @param asTemplate
   *          true, wenn das Dokument als "Unbenannt X" ge�ffnet werden soll
   *          (also im "Template-Modus") und false, wenn das Dokument zum
   *          Bearbeiten ge�ffnet werden soll.
   */
  public static void handleOpenDocument(Vector fragIDs, boolean asTemplate)
  {
    handle(new OnOpenDocument(fragIDs, asTemplate));
  }

  /**
   * Dieses Event wird gestartet, wenn der WollMux-Service (...comp.WollMux) das
   * Dispatch-Kommando wollmux:openTemplate bzw. wollmux:openDocument empf�ngt
   * und sort daf�r, dass das entsprechende Dokument ge�ffnet wird.
   * 
   * @author christoph.lutz
   */
  private static class OnOpenDocument extends BasicEvent
  {
    private boolean asTemplate;

    private Vector fragIDs;

    private OnOpenDocument(Vector fragIDs, boolean asTemplate)
    {
      this.fragIDs = fragIDs;
      this.asTemplate = asTemplate;
    }

    protected boolean doit() throws WollMuxFehlerException
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      // das erste Argument ist das unmittelbar zu landende Textfragment und
      // wird nach urlStr aufgel�st. Alle weiteren Argumente (falls vorhanden)
      // werden nach argsUrlStr aufgel�st.
      String loadUrlStr = "";
      String[] fragUrls = new String[fragIDs.size() - 1];

      Iterator iter = fragIDs.iterator();
      for (int i = 0; iter.hasNext(); ++i)
      {
        String frag_id = (String) iter.next();

        // Fragment-URL holen und aufbereiten:
        String urlStr;
        try
        {
          urlStr = mux.getTextFragmentList().getURLByID(frag_id);
        }
        catch (java.lang.Exception e)
        {
          throw new WollMuxFehlerException(
              "Die URL zum Textfragment mit der FRAG_ID '"
                  + frag_id
                  + "' kann nicht bestimmt werden.", e);
        }
        URL url;
        try
        {
          url = new URL(mux.getDEFAULT_CONTEXT(), urlStr);
          urlStr = url.toExternalForm();
        }
        catch (MalformedURLException e)
        {
          throw new WollMuxFehlerException(
              "Die URL '"
                  + urlStr
                  + "' des Textfragments mit der FRAG_ID '"
                  + frag_id
                  + "' ist ung�ltig.", e);
        }

        // URL durch den URL-Transformer jagen
        try
        {
          XURLTransformer trans = UNO.XURLTransformer(UNO
              .createUNOService("com.sun.star.util.URLTransformer"));
          com.sun.star.util.URL[] unoURL = new com.sun.star.util.URL[] { new com.sun.star.util.URL() };
          unoURL[0].Complete = urlStr;
          if (trans != null) trans.parseStrict(unoURL);
          urlStr = unoURL[0].Complete;
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }

        // Workaround f�r Fehler in insertDocumentFromURL: Pr�fen ob URL
        // aufgel�st werden kann, da sonst der insertDocumentFromURL einfriert.
        try
        {
          url = new URL(urlStr);
        }
        catch (MalformedURLException e)
        {
          // darf nicht auftreten, da url bereits oben gepr�ft wurde...
          Logger.error(e);
        }
        try
        {
          WollMuxSingleton.checkURL(url);
        }
        catch (IOException e)
        {
          Logger.error(e);
          throw new WollMuxFehlerException(
              "Fehler beim Laden des Fragments mit der FRAG_ID '"
                  + frag_id
                  + "' von der URL '"
                  + url.toExternalForm()
                  + "'\n", e);
        }

        // URL in die in loadUrlStr (zum sofort �ffnen) und in argsUrlStr (zum
        // sp�ter �ffnen) aufnehmen
        if (i == 0)
          loadUrlStr = urlStr;
        else
          fragUrls[i - 1] = urlStr;
      }

      // open document as Template (or as document):
      try
      {
        XComponent doc = UNO.loadComponentFromURL(loadUrlStr, asTemplate, true);
        OnProcessTextDocument.docFragUrlsBuffer.put(
            new HashableComponent(doc),
            fragUrls);
      }
      catch (java.lang.Exception x)
      {
        throw new WollMuxFehlerException("Die Vorlage mit der URL '"
                                         + loadUrlStr
                                         + "' kann nicht ge�ffnet werden.", x);
      }

      return EventProcessor.processTheNextEvent;
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
    protected boolean doit()
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

      return EventProcessor.processTheNextEvent;
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

    protected boolean doit()
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
      return EventProcessor.processTheNextEvent;
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
   * Erzeugt ein neues WollMuxEvent, welches daf�r sorgt, dass alle
   * Formularfelder Dokument auf den neuen Wert gesetzt werden. Bei
   * Formularfeldern mit TRAFO-Funktion wird die Transformation entsprechend
   * durchgef�hrt.
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
  public static void handleFormValueChanged(FormDescriptor fd,
      HashMap idToFormValues, String fieldId, String newValue,
      FunctionLibrary funcLib)
  {
    handle(new OnFormValueChanged(fd, idToFormValues, fieldId, newValue,
        funcLib));
  }

  /**
   * Dieses Event wird (derzeit) vom FormModelImpl ausgel�st, wenn in der
   * Formular-GUI der Wert des Formularfeldes fieldID ge�ndert wurde und sorgt
   * daf�r, dass die Wert�nderung auf alle betroffenen Formularfelder im
   * Dokument doc �bertragen werden.
   * 
   * @author christoph.lutz
   */
  private static class OnFormValueChanged extends BasicEvent
  {
    private HashMap idToFormValues;

    private String fieldId;

    private String newValue;

    private FunctionLibrary funcLib;

    private FormDescriptor fd;

    public OnFormValueChanged(FormDescriptor fd, HashMap idToFormValues,
        String fieldId, String newValue, FunctionLibrary funcLib)
    {
      this.idToFormValues = idToFormValues;
      this.fieldId = fieldId;
      this.newValue = newValue;
      this.funcLib = funcLib;
      this.fd = fd;
    }

    protected boolean doit()
    {
      // Wenn es FormFields zu dieser id gibt, so werden alle FormFields auf den
      // neuen Stand gebracht.
      Vector formFields = (Vector) idToFormValues.get(fieldId);
      if (formFields != null)
      {
        Iterator i = formFields.iterator();
        while (i.hasNext())
        {
          FormField field = (FormField) i.next();
          try
          {
            field.setValue(newValue, funcLib);
          }
          catch (RuntimeException e)
          {
            // Absicherung gegen das manuelle L�schen von Dokumentinhalten.
          }
        }
      }

      // FormularDescriptor �ber die �nderung informieren. Dies ist vor allen
      // auch dazu notwendig, um Originalwerte zu sichern, zu denen es kein
      // FormField gibt.
      fd.setFormFieldValue(fieldId, newValue);
      fd.updateDocument();
      return EventProcessor.processTheNextEvent;
    }

    public String toString()
    {
      return this.getClass().getSimpleName()
             + "("
             + fd
             + ", '"
             + fieldId
             + "', '"
             + newValue
             + "')";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, welches daf�r sorgt, dass alle Textbereiche
   * im �bergebenen Dokument, die einer bestimmten Gruppe zugeh�ren ein- oder
   * ausgeblendet werden.
   * 
   * @param doc
   *          Das Dokument, welches die Textbereiche, die �ber Dokumentkommandos
   *          spezifiziert sind enth�lt.
   * @param invisibleGroups
   *          Enth�lt ein HashSet, das die groupId's aller als unsichtbar
   *          markierten Gruppen enth�lt.
   * @param groupId
   *          Die GROUP (ID) der ein/auszublendenden Gruppe.
   * @param visible
   *          Der neue Sichtbarkeitsstatus (true=sichtbar, false=ausgeblendet)
   * @param cmdTree
   *          Die DocumentCommandTree-Struktur, die den Zustand der
   *          Sichtbarkeiten enth�lt.
   */
  public static void handleSetVisibleState(DocumentCommandTree cmdTree,
      HashSet invisibleGroups, String groupId, boolean visible)
  {
    handle(new OnSetVisibleState(cmdTree, invisibleGroups, groupId, visible));
  }

  /**
   * Dieses Event wird (derzeit) vom FormModelImpl ausgel�st, wenn in der
   * Formular-GUI bestimmte Text-Teile des �bergebenen Dokuments ein- oder
   * ausgeblendet werden sollen.
   * 
   * @author christoph.lutz
   */
  private static class OnSetVisibleState extends BasicEvent
  {
    private String groupId;

    private boolean visible;

    private DocumentCommandTree cmdTree;

    private HashSet invisibleGroups;

    public OnSetVisibleState(DocumentCommandTree cmdTree,
        HashSet invisibleGroups, String groupId, boolean visible)
    {
      this.cmdTree = cmdTree;
      this.invisibleGroups = invisibleGroups;
      this.groupId = groupId;
      this.visible = visible;
    }

    protected boolean doit()
    {
      // invisibleGroups anpassen:
      if (visible)
        invisibleGroups.remove(groupId);
      else
        invisibleGroups.add(groupId);

      DocumentCommand firstChangedCmd = null;

      // Kommandobaum durchlaufen und alle betroffenen Elemente updaten:
      Iterator iter = cmdTree.depthFirstIterator(false);
      while (iter.hasNext())
      {
        DocumentCommand cmd = (DocumentCommand) iter.next();
        Set groups = cmd.getGroups();
        if (!groups.contains(groupId)) continue;

        // Visibility-Status neu bestimmen:
        boolean setVisible = true;
        Iterator i = groups.iterator();
        while (i.hasNext())
        {
          String groupId = (String) i.next();
          if (invisibleGroups.contains(groupId)) setVisible = false;
        }

        // Kommando merken, dessen Sichtbarkeitsstatus sich zuerst �ndert und
        // den focus (ViewCursor) auf den Start des Bereichs setzen. Da das
        // Setzen eines ViewCursors in einen unsichtbaren Bereich nicht
        // funktioniert, wird die Methode focusRangeStart zwei mal aufgerufen,
        // je nach dem, ob der Bereich vor oder nach dem Setzen des neuen
        // Sichtbarkeitsstatus sichtbar ist.
        if (setVisible != cmd.isVisible() && firstChangedCmd == null)
        {
          firstChangedCmd = cmd;
          if (firstChangedCmd.isVisible()) focusRangeStart(cmd);
        }

        // neuen Sichtbarkeitsstatus setzen:
        try
        {
          cmd.setVisible(setVisible);
        }
        catch (RuntimeException e)
        {
          // Absicherung gegen das manuelle L�schen von Dokumentinhalten
        }
      }

      // Den Cursor (nochmal) auf den Anfang des Bookmarks setzen, dessen
      // Sichtbarkeitsstatus sich zuerst ge�ndert hat (siehe Begr�ndung oben).
      if (firstChangedCmd != null && firstChangedCmd.isVisible())
        focusRangeStart(firstChangedCmd);

      return EventProcessor.processTheNextEvent;
    }

    /**
     * Diese Methode setzt den ViewCursor auf den Anfang des Bookmarks des
     * Dokumentkommandos cmd.
     * 
     * @param cmd
     *          Das Dokumentkommando, auf dessen Start der ViewCursor gesetzt
     *          werden soll.
     */
    private void focusRangeStart(DocumentCommand cmd)
    {
      XTextRange range = cmd.getTextRange();
      if (range != null)
        try
        {
          XController controller = UNO.XModel(cmdTree.getBookmarksSupplier())
              .getCurrentController();
          XTextCursor cursor = UNO.XTextViewCursorSupplier(controller)
              .getViewCursor();
          cursor.gotoRange(range.getStart(), false);
          // Stellt sicher,
          // cursor.goLeft((short) 1, false);
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
   * @param idToFormValues
   *          Eine HashMap die unter dem Schl�ssel fieldID den Vektor aller
   *          FormFields mit der ID fieldID liefert.
   * @param fieldId
   *          die ID des Formularfeldes das den Fokus bekommen soll. Besitzen
   *          mehrere Formularfelder diese ID, so wird bevorzugt das erste
   *          Formularfeld aus dem Vektor genommen, das keine Trafo enth�lt.
   *          Ansonsten wird das erste Formularfeld im Vektor verwendet.
   */
  public static void handleFocusFormField(HashMap idToFormValues, String fieldId)
  {
    handle(new OnFocusFormField(idToFormValues, fieldId));
  }

  /**
   * Dieses Event wird (derzeit) vom FormModelImpl ausgel�st, wenn in der
   * Formular-GUI ein Formularfeld den Fokus bekommen hat und es sorgt daf�r,
   * dass der View-Cursor des Dokuments das entsprechende FormField im Dokument
   * anspringt.
   * 
   * @author christoph.lutz
   */
  private static class OnFocusFormField extends BasicEvent
  {
    private HashMap idToFormValues;

    private String fieldId;

    public OnFocusFormField(HashMap idToFormValues, String fieldId)
    {
      this.idToFormValues = idToFormValues;
      this.fieldId = fieldId;
    }

    protected boolean doit()
    {
      if (idToFormValues.containsKey(fieldId))
      {
        Vector formFields = (Vector) idToFormValues.get(fieldId);
        FormField field = preferUntransformedFormField(formFields);
        try
        {
          if (field != null) field.focus();
        }
        catch (RuntimeException e)
        {
          // Absicherung gegen das manuelle L�schen von Dokumentinhalten.
        }
      }
      else
      {
        Logger.debug(this
                     + ": Es existiert kein Formularfeld mit der ID '"
                     + fieldId
                     + "' in diesem Dokument");
      }
      return EventProcessor.processTheNextEvent;
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "('" + fieldId + "')";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das die Position und Gr��e des �bergebenen
   * Dokument-Fensters auf die vorgegebenen Werte setzt. ACHTUNG: Die Ma�angaben
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
  public static void handleSetWindowPosSize(XModel model, int docX, int docY,
      int docWidth, int docHeight)
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
    private XModel model;

    private int docX, docY, docWidth, docHeight;

    public OnSetWindowPosSize(XModel model, int docX, int docY, int docWidth,
        int docHeight)
    {
      this.model = model;
      this.docX = docX;
      this.docY = docY;
      this.docWidth = docWidth;
      this.docHeight = docHeight;
    }

    protected boolean doit()
    {
      try
      {
        XFrame frame = model.getCurrentController().getFrame();
        frame.getContainerWindow().setPosSize(
            docX,
            docY,
            docWidth,
            docHeight,
            PosSize.POSSIZE);
      }
      catch (java.lang.Exception e)
      {
      }
      return EventProcessor.processTheNextEvent;
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(model, o);
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
  public static void handleSetWindowVisible(XModel model, boolean visible)
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
    private XModel model;

    boolean visible;

    public OnSetWindowVisible(XModel model, boolean visible)
    {
      this.model = model;
      this.visible = visible;
    }

    protected boolean doit()
    {
      XFrame frame = model.getCurrentController().getFrame();
      if (frame != null)
      {
        frame.getContainerWindow().setVisible(visible);
      }
      return EventProcessor.processTheNextEvent;
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(model, o);
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
   * @param doc
   *          Das zu schlie�ende XTextDocument.
   */
  public static void handleCloseTextDocument(XTextDocument doc)
  {
    handle(new OnCloseTextDocument(doc));
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
    private XTextDocument doc;

    public OnCloseTextDocument(XTextDocument doc)
    {
      this.doc = doc;
    }

    protected boolean doit()
    {
      // Damit OOo vor dem Schlie�en eines ver�nderten Dokuments den
      // save/dismiss-Dialog anzeigt, muss die suspend()-Methode aller
      // XController gestartet werden, die das Model der Komponente enthalten.
      // Man bekommt alle XController �ber die Frames, die der Desktop liefert.
      Object desktop = UNO.createUNOService("com.sun.star.frame.Desktop");
      if (UNO.XFramesSupplier(desktop) != null)
      {
        XFrame[] frames = UNO.XFramesSupplier(desktop).getFrames().queryFrames(
            FrameSearchFlag.ALL);
        for (int i = 0; i < frames.length; i++)
        {
          XController c = frames[i].getController();
          if (c != null && UnoRuntime.areSame(c.getModel(), doc))
            c.suspend(true);
        }
      }

      // Hier das eigentliche Schlie�en:
      try
      {
        if (UNO.XCloseable(doc) != null) UNO.XCloseable(doc).close(true);
      }
      catch (CloseVetoException e)
      {
      }

      return EventProcessor.processTheNextEvent;
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(doc, o);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + doc.hashCode() + ")";
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
   * @author christoph.lutz
   */
  private static class OnInitialize extends BasicEvent
  {
    protected boolean doit()
    {
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      DatasourceJoiner dsj = mux.getDatasourceJoiner();

      // falls es noch keine Datens�tze im LOS gibt.
      if (dsj.getLOS().size() == 0)
      {

        // Die initialen Daten aus den OOo UserProfileData holen:
        String vorname = getUserProfileData("givenname");
        String nachname = getUserProfileData("sn");
        Logger.debug2("Initialize mit Vorname=\""
                      + vorname
                      + "\" und Nachname=\""
                      + nachname
                      + "\"");

        // im DatasourceJoiner nach dem Benutzer suchen:
        QueryResults r = null;
        if (!vorname.equals("") && !nachname.equals("")) try
        {
          r = dsj.find("Vorname", vorname, "Nachname", nachname);
        }
        catch (TimeoutException e)
        {
          Logger.error(e);
        }

        // Auswertung der Suchergebnisse:
        if (r != null)
        {
          // alle matches werden in die PAL kopiert:
          Iterator i = r.iterator();
          while (i.hasNext())
          {
            ((DJDataset) i.next()).copy();
          }
        }

        // Absender Ausw�hlen Dialog starten:
        WollMuxEventHandler.handleShowDialogAbsenderAuswaehlen();
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
          showInfoModal("WollMux-Info", message);
        }
      }
      return EventProcessor.processTheNextEvent;
    }

    private static String getUserProfileData(String key)
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
      XPALChangeEventListener listener)
  {
    handle(new OnAddPALChangeEventListener(listener));
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

    public OnAddPALChangeEventListener(XPALChangeEventListener listener)
    {
      this.listener = listener;
    }

    protected boolean doit()
    {
      WollMuxSingleton.getInstance().addPALChangeEventListener(listener);

      WollMuxEventHandler.handlePALChangedNotify();

      return EventProcessor.processTheNextEvent;
    }

    public boolean requires(Object o)
    {
      return UnoRuntime.areSame(listener, o);
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + listener.hashCode() + ")";
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

    protected boolean doit()
    {
      WollMuxSingleton.getInstance().removePALChangeEventListener(listener);
      return EventProcessor.processTheNextEvent;
    }

    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + listener.hashCode() + ")";
    }
  }

  // *******************************************************************************************
  // Globale Helper-Methoden
  /**
   * Diese Methode erzeugt einen modalen UNO-Dialog zur Anzeige von
   * Fehlermeldungen bei der Bearbeitung eines Events.
   * 
   * @param sTitle
   * @param sMessage
   */
  private static void showInfoModal(java.lang.String sTitle,
      java.lang.String sMessage)
  {
    try
    {
      XComponentContext m_xCmpCtx = WollMuxSingleton.getInstance()
          .getXComponentContext();

      // hole aktuelles Window:
      UnoService desktop = UnoService.createWithContext(
          "com.sun.star.frame.Desktop",
          m_xCmpCtx);

      // wenn ein Frame vorhanden ist, wird dieser als Parent f�r die Erzeugung
      // einer Infobox �ber das Toolkit verwendet, ansonsten wird ein
      // swing-Dialog gestartet.
      XFrame xFrame = desktop.xDesktop().getCurrentFrame();
      if (xFrame != null)
      {
        XWindow xParent = xFrame.getContainerWindow();

        // get access to the office toolkit environment
        com.sun.star.awt.XToolkit xKit = (com.sun.star.awt.XToolkit) UnoRuntime
            .queryInterface(com.sun.star.awt.XToolkit.class, m_xCmpCtx
                .getServiceManager().createInstanceWithContext(
                    "com.sun.star.awt.Toolkit",
                    m_xCmpCtx));

        // describe the info box ini it's parameters
        com.sun.star.awt.WindowDescriptor aDescriptor = new com.sun.star.awt.WindowDescriptor();
        aDescriptor.WindowServiceName = "infobox";
        aDescriptor.Bounds = new com.sun.star.awt.Rectangle(0, 0, 300, 200);
        aDescriptor.WindowAttributes = com.sun.star.awt.WindowAttribute.BORDER
                                       | com.sun.star.awt.WindowAttribute.MOVEABLE
                                       | com.sun.star.awt.WindowAttribute.CLOSEABLE;
        aDescriptor.Type = com.sun.star.awt.WindowClass.MODALTOP;
        aDescriptor.ParentIndex = 1;
        aDescriptor.Parent = (com.sun.star.awt.XWindowPeer) UnoRuntime
            .queryInterface(com.sun.star.awt.XWindowPeer.class, xParent);

        // create the info box window
        com.sun.star.awt.XWindowPeer xPeer = xKit.createWindow(aDescriptor);
        com.sun.star.awt.XMessageBox xInfoBox = (com.sun.star.awt.XMessageBox) UnoRuntime
            .queryInterface(com.sun.star.awt.XMessageBox.class, xPeer);
        if (xInfoBox == null) return;

        // fill it with all given informations and show it
        xInfoBox.setCaptionText("" + sTitle + "");
        xInfoBox.setMessageText("" + sMessage + "");
        xInfoBox.execute();
      }
      else
      {
        // zeige eine swing-infoBox an, falls kein OOo Parent vorhanden ist.

        // zu lange Strings umbrechen:
        final int MAXCHARS = 50;
        String formattedMessage = "";
        String[] lines = sMessage.split("\n");
        for (int i = 0; i < lines.length; i++)
        {
          String[] words = lines[i].split(" ");
          int chars = 0;
          for (int j = 0; j < words.length; j++)
          {
            String word = words[j];
            if (chars > 0 && chars + word.length() > MAXCHARS)
            {
              formattedMessage += "\n";
              chars = 0;
            }
            formattedMessage += word + " ";
            chars += word.length() + 1;
          }
          if (i != lines.length - 1) formattedMessage += "\n";
        }

        // infobox ausgeben:
        Common.setLookAndFeelOnce();
        javax.swing.JOptionPane.showMessageDialog(
            null,
            formattedMessage,
            sTitle,
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
      }
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
  }

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

  /**
   * Diese Methode setzt den ZoomTyp bzw. den ZoomValue der Dokumentenansicht
   * des Dokuments doc auf den neuen Wert zoom, der entwender eine ganzzahliger
   * Prozentwert (ohne "%"-Zeichen") oder einer der Werte "Optimal",
   * "PageWidth", "PageWidthExact" oder "EntirePage" ist.
   * 
   * @param controller
   * @param zoom
   * @throws ConfigurationErrorException
   */
  private static void setDocumentZoom(XModel doc, String zoom)
      throws ConfigurationErrorException
  {
    Short zoomType = null;
    Short zoomValue = null;

    if (zoom != null)
    {
      // ZOOM-Argument auswerten:
      if (zoom.equalsIgnoreCase("Optimal"))
        zoomType = new Short(DocumentZoomType.OPTIMAL);

      if (zoom.equalsIgnoreCase("PageWidth"))
        zoomType = new Short(DocumentZoomType.PAGE_WIDTH);

      if (zoom.equalsIgnoreCase("PageWidthExact"))
        zoomType = new Short(DocumentZoomType.PAGE_WIDTH_EXACT);

      if (zoom.equalsIgnoreCase("EntirePage"))
        zoomType = new Short(DocumentZoomType.ENTIRE_PAGE);

      if (zoomType == null)
      {
        try
        {
          zoomValue = new Short(zoom);
        }
        catch (NumberFormatException e)
        {
        }
      }
    }

    // ZoomType bzw ZoomValue setzen:
    Object viewSettings = null;
    try
    {
      viewSettings = UNO.XViewSettingsSupplier(doc.getCurrentController())
          .getViewSettings();
    }
    catch (java.lang.Exception e)
    {
    }
    if (zoomType != null)
      UNO.setProperty(viewSettings, "ZoomType", zoomType);
    else if (zoomValue != null)
      UNO.setProperty(viewSettings, "ZoomValue", zoomValue);
    else
      throw new ConfigurationErrorException("Ung�ltiger ZOOM-Wert '"
                                            + zoom
                                            + "'");
  }
}
