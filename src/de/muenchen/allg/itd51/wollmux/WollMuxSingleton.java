/*
 * Dateiname: WollMuxSingleton.java
 * Projekt  : WollMux
 * Funktion : Singleton f�r zentrale WollMux-Methoden.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 14.10.2005 | LUT | Erstellung
 * 09.11.2005 | LUT | + Logfile wird jetzt erweitert (append-modus)
 *                    + verwenden des Konfigurationsparameters SENDER_SOURCE
 *                    + Erster Start des wollmux �ber wm_configured feststellen.
 * 05.12.2005 | BNK | line.separator statt \n     
 * 13.04.2006 | BNK | .wollmux/ Handling ausgegliedert in WollMuxFiles.
 * 20.04.2006 | LUT | �berarbeitung Code-Kommentare  
 * 20.04.2006 | BNK | DEFAULT_CONTEXT ausgegliedert nach WollMuxFiles
 * 21.04.2006 | LUT | + Robusteres Verhalten bei Fehlern w�hrend dem Einlesen 
 *                    von Konfigurationsdateien; 
 *                    + wohldefinierte Datenstrukturen
 *                    + Flag f�r EventProcessor: acceptEvents
 * 08.05.2006 | LUT | + isDebugMode()
 * 10.05.2006 | BNK | +parseGlobalFunctions()
 *                  | +parseFunctionDialogs()
 * 26.05.2006 | BNK | DJ initialisierung ausgelagert nacht WollMuxFiles
 * 06.06.2006 | LUT | + Abl�sung der Event-Klasse durch saubere Objektstruktur
 * 19.12.2006 | BAB | + setzen von Shortcuts im Konstruktor
 * 29.12.2006 | BNK | +registerDatasources()
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import com.sun.star.container.XEnumeration;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.PrintFunctionLibrary;

/**
 * Diese Klasse ist ein Singleton, welcher den WollMux initialisiert und alle
 * zentralen WollMux-Methoden zur Verf�gung stellt. Selbst der WollMux-Service
 * de.muenchen.allg.itd51.wollmux.comp.WollMux, der fr�her zentraler Anlaufpunkt
 * war, bedient sich gr��tenteils aus den zentralen Methoden des Singletons.
 */
public class WollMuxSingleton implements XPALProvider
{

  private static WollMuxSingleton singletonInstance = null;

  /**
   * Enth�lt die im Funktionen-Abschnitt der wollmux,conf definierten
   * Funktionen.
   */
  private FunctionLibrary globalFunctions;

  /**
   * Enth�lt die im Funktionsdialoge-Abschnitt der wollmux,conf definierten
   * Dialoge.
   */
  private DialogLibrary funcDialogs;

  /**
   * Enth�lt die im Funktionen-Abschnitt der wollmux,conf definierten
   * Funktionen.
   */
  private PrintFunctionLibrary globalPrintFunctions;

  /**
   * Enth�lt den default XComponentContext in dem der WollMux (bzw. das OOo)
   * l�uft.
   */
  private XComponentContext ctx;

  /**
   * Enth�lt alle registrierten SenderBox-Objekte.
   */
  private Vector registeredPALChangeListener;

  /**
   * Enth�lt eine Zuordnung von HashableComponent Objekten, die die
   * XTextDocumente repr�sentieren, auf die zugeh�rigen TextDocumentModels
   */
  private HashMap currentTextDocumentModels;

  /**
   * Die WollMux-Hauptklasse ist als singleton realisiert.
   */
  private WollMuxSingleton(XComponentContext ctx)
  {
    // Der XComponentContext wir hier gesichert und vom WollMuxSingleton mit
    // getXComponentContext zur�ckgeliefert.
    this.ctx = ctx;

    this.currentTextDocumentModels = new HashMap();

    // Initialisiere die UNO-Klasse, so dass auch mit dieser Hilfsklasse
    // gearbeitet werden kann.
    try
    {
      UNO.init(ctx.getServiceManager());
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }

    boolean successfulStartup = true;

    registeredPALChangeListener = new Vector();

    WollMuxFiles.setupWollMuxDir();

    Logger.debug("StartupWollMux");
    Logger.debug("Build-Info: " + getBuildInfo());
    Logger.debug("wollmuxConfFile = "
                 + WollMuxFiles.getWollMuxConfFile().toString());
    Logger.debug("DEFAULT_CONTEXT \""
                 + WollMuxFiles.getDEFAULT_CONTEXT().toString()
                 + "\"");
    Logger.debug("CONF_VERSION: " + getConfVersionInfo());

    /*
     * Datenquellen/Registriere Abschnitte verarbeiten. ACHTUNG! Dies muss vor
     * getDatasourceJoiner() geschehen, da die entsprechenden Datenquellen
     * wom�glich schon f�r WollMux-Datenquellen ben�tigt werden.
     */
    registerDatasources(WollMuxFiles.getWollmuxConf(), WollMuxFiles
        .getDEFAULT_CONTEXT());

    // Versuchen, den DJ zu initialisieren und Flag setzen, falls nicht
    // erfolgreich.
    if (getDatasourceJoiner() == null) successfulStartup = false;

    /*
     * Globale Funktionsdialoge parsen. ACHTUNG! Muss vor parseGlobalFunctions()
     * erfolgen. Als context wird null �bergeben, weil globale Funktionen keinen
     * Kontext haben. TODO �berlegen, ob ein globaler Kontext doch Sinn machen
     * k�nnte. Dadurch k�nnten globale Funktionen globale Funktionsdialoge
     * darstellen, die global einheitliche Werte haben.
     */
    funcDialogs = WollMuxFiles.parseFunctionDialogs(WollMuxFiles
        .getWollmuxConf(), null, null);

    /*
     * Globale Funktionen parsen. ACHTUNG! Verwendet die Funktionsdialoge. Diese
     * m�ssen also vorher geparst sein. Als context wird null �bergeben, weil
     * globale Funktionen keinen Kontext haben.
     */
    globalFunctions = WollMuxFiles.parseFunctions(
        WollMuxFiles.getWollmuxConf(),
        getFunctionDialogs(),
        null,
        null);

    /*
     * Globale Druckfunktionen parsen.
     */
    globalPrintFunctions = WollMuxFiles.parsePrintFunctions(WollMuxFiles
        .getWollmuxConf());

    // Initialisiere EventProcessor
    WollMuxEventHandler.setAcceptEvents(successfulStartup);

    // register global EventListener
    try
    {
      UnoService eventBroadcaster = UnoService.createWithContext(
          "com.sun.star.frame.GlobalEventBroadcaster",
          ctx);
      eventBroadcaster.xEventBroadcaster().addEventListener(
          new GlobalEventListener());
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    // setzen von shortcuts
    ConfigThingy tastenkuerzel = new ConfigThingy("");
    try
    {
      tastenkuerzel = WollMuxFiles.getWollmuxConf().query("Tastenkuerzel")
          .getLastChild();
    }
    catch (NodeNotFoundException e)
    {
    }
    try
    {
      Shortcuts.createShortcuts(tastenkuerzel);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Diese Methode liefert die Instanz des WollMux-Singletons. Ist der WollMux
   * noch nicht initialisiert, so liefert die Methode null!
   * 
   * @return Instanz des WollMuxSingletons oder null.
   */
  public static WollMuxSingleton getInstance()
  {
    return singletonInstance;
  }

  /**
   * Diese Methode initialisiert das WollMuxSingleton (nur dann, wenn es noch
   * nicht initialisiert wurde)
   */
  public static void initialize(XComponentContext ctx)
  {
    if (singletonInstance == null)
    {
      singletonInstance = new WollMuxSingleton(ctx);

      // Event ON_FIRST_INITIALIZE erzeugen:
      WollMuxEventHandler.handleInitialize();
    }
  }

  /**
   * Diese Methode liefert die erste Zeile aus der buildinfo-Datei der aktuellen
   * WollMux-Installation zur�ck. Der Build-Status wird w�hrend dem
   * Build-Prozess mit dem Kommando "svn info" auf das Projektverzeichnis
   * erstellt. Die Buildinfo-Datei buildinfo enth�lt die Paketnummer und die
   * svn-Revision und ist im WollMux.uno.pkg-Paket sowie in der
   * WollMux.uno.jar-Datei abgelegt.
   * 
   * Kann dieses File nicht gelesen werden, so wird eine entsprechende
   * Ersatzmeldung erzeugt (siehe Sourcecode).
   * 
   * @return Der Build-Status der aktuellen WollMux-Installation.
   */
  public String getBuildInfo()
  {
    try
    {
      URL url = WollMuxSingleton.class.getClassLoader()
          .getResource("buildinfo");
      if (url != null)
      {
        BufferedReader in = new BufferedReader(new InputStreamReader(url
            .openStream()));
        return in.readLine().toString();
      }
    }
    catch (java.lang.Exception x)
    {
    }
    return "Version: unbekannt";
  }

  /**
   * Diese Methode liefert die Versionsinformation der aktuell verwendeten
   * wollmux-Konfiguration (z.B. "wollmux-standard-config-2.2.1") als String
   * zur�ck, wenn in der Konfiguration ein entsprechender CONF_VERSION-Schl�ssel
   * definiert ist, oder "unbekannt", falls der dieser Schl�ssel nicht
   * existiert.
   * 
   * @return Der Versionsinformation der aktuellen WollMux-Konfiguration (falls
   *         definiert) oder "unbekannt", falls nicht.
   */
  public String getConfVersionInfo()
  {
    ConfigThingy versions = getWollmuxConf().query("CONF_VERSION");
    try
    {
      return versions.getLastChild().toString();
    }
    catch (NodeNotFoundException e)
    {
      return "unbekannt";
    }
  }

  /**
   * @return Returns the xComponentContext.
   */
  public XComponentContext getXComponentContext()
  {
    return ctx;
  }

  /**
   * Diese Methode liefert eine Instanz auf den aktuellen DatasourceJoiner
   * zur�ck.
   * 
   * @return Returns the datasourceJoiner.
   */
  public DatasourceJoiner getDatasourceJoiner()
  {
    return WollMuxFiles.getDatasourceJoiner();
  }

  /**
   * Verarbeitet alle Datenquellen/Registriere-Unterabschnitte von conf und
   * registriert die entsprechenden Datenquellen in OOo, falls dort noch nicht
   * vorhanden.
   * 
   * @param context
   *          gibt an relativ zu was relative URLs aufgel�st werden sollen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static void registerDatasources(ConfigThingy conf, URL context)
  {
    Iterator iter = conf.query("Datenquellen").query("Registriere").iterator();
    while (iter.hasNext())
    {
      ConfigThingy regConf = (ConfigThingy) iter.next();
      String name;
      try
      {
        name = regConf.get("NAME").toString();
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(
            "NAME-Attribut fehlt in Datenquellen/Registriere-Abschnitt",
            e);
        continue;
      }

      String urlStr;
      try
      {
        urlStr = regConf.get("URL").toString();
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(
            "URL-Attribut fehlt in Datenquellen/Registriere-Abschnitt f�r Datenquelle \""
                + name
                + "\"",
            e);
        continue;
      }

      try
      {
        if (UNO.XNameAccess(UNO.dbContext).hasByName(name))
        {
          try
          {
            if (!regConf.get("REFRESH").toString().equalsIgnoreCase("true"))
              continue;

            // hierher (und damit weiter ohne continue) kommen wir nur, wenn
            // ein REFRESH-Abschnitt vorhanden ist und "true" enth�lt.
          }
          catch (java.lang.Exception x) // vor allem NodeNotFoundException
          {
            continue;
          }
        }
      }
      catch (java.lang.Exception x)
      {
        Logger.error("Fehler beim �berpr�fen, ob Datenquelle \""
                     + name
                     + "\" bereits registriert ist", x);
      }

      Logger.debug("Versuche, Datenquelle \""
                   + name
                   + "\" bei OOo zu registrieren f�r URL \""
                   + urlStr
                   + "\"");

      String parsedUrl;
      try
      {
        URL url = new URL(context, urlStr);
        parsedUrl = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;
      }
      catch (java.lang.Exception x)
      {
        Logger.error("Fehler beim Registrieren von Datenquelle \""
                     + name
                     + "\": Illegale URL: \""
                     + urlStr
                     + "\"", x);
        continue;
      }

      try
      {
        Object datasource = UNO.XNameAccess(UNO.dbContext).getByName(parsedUrl);
        UNO.dbContext.registerObject(name, datasource);
        if (!UnoRuntime.areSame(
            UNO.dbContext.getRegisteredObject(name),
            datasource))
          Logger.error("Testzugriff auf Datenquelle \""
                       + name
                       + "\" nach Registrierung fehlgeschlagen");
      }
      catch (Exception x)
      {
        Logger.error("Fehler beim Registrieren von Datenquelle \""
                     + name
                     + "\". Stellen Sie sicher, dass die URL \""
                     + parsedUrl
                     + "\" g�ltig ist.", x);
        continue;
      }

    }
  }

  /**
   * Diese Methode registriert einen XPALChangeEventListener, der updates
   * empf�ngt wenn sich die PAL �ndert. Die Methode ignoriert alle
   * XPALChangeEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht m�glich.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen
   * werden, sondern jeder Aufruf muss �ber den EventHandler laufen. Deswegen
   * exportiert WollMuxSingleton auch nicht das
   * XPALChangedBroadcaster-Interface.
   */
  public void addPALChangeEventListener(XPALChangeEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::addPALChangeEventListener()");

    if (listener == null) return;

    Iterator i = registeredPALChangeListener.iterator();
    while (i.hasNext())
    {
      Object l = i.next();
      if (UnoRuntime.areSame(l, listener)) return;
    }
    registeredPALChangeListener.add(listener);
  }

  /**
   * Diese Methode deregistriert einen XPALChangeEventListener wenn er bereits
   * registriert war.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen
   * werden, sondern jeder Aufruf muss �ber den EventHandler laufen. Deswegen
   * exportiert WollMuxSingleton auch nicht das
   * XPALChangedBroadcaster-Interface.
   */
  public void removePALChangeEventListener(XPALChangeEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::removePALChangeEventListener()");
    Iterator i = registeredPALChangeListener.iterator();
    while (i.hasNext())
    {
      Object l = i.next();
      if (UnoRuntime.areSame(l, listener)) i.remove();
    }
  }

  /**
   * Liefert einen Iterator auf alle registrierten SenderBox-Objekte.
   * 
   * @return Iterator auf alle registrierten SenderBox-Objekte.
   */
  public Iterator palChangeListenerIterator()
  {
    return registeredPALChangeListener.iterator();
  }

  /**
   * Diese Methode liefert eine alphabethisch aufsteigend sortierte Liste aller
   * Eintr�ge der Pers�nlichen Absenderliste (PAL) in einem String-Array, wobei
   * die einzelnen Eintr�ge in der Form "<Nachname>, <Vorname> (<Rolle>)"
   * sind.
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALProvider#getPALEntries()
   */
  public String[] getPALEntries()
  {
    DJDatasetListElement[] pal = getSortedPALEntries();
    String[] elements = new String[pal.length];
    for (int i = 0; i < pal.length; i++)
    {
      elements[i] = pal[i].toString();
    }
    return elements;
  }

  /**
   * Diese Methode liefert alle DJDatasetListElemente der Pers�nlichen
   * Absenderliste (PAL) in alphabetisch aufsteigend sortierter Reihenfolge.
   * 
   * @return alle DJDatasetListElemente der Pers�nlichen Absenderliste (PAL) in
   *         alphabetisch aufsteigend sortierter Reihenfolge.
   */
  public DJDatasetListElement[] getSortedPALEntries()
  {
    // Liste der entries aufbauen.
    QueryResults data = getDatasourceJoiner().getLOS();

    DJDatasetListElement[] elements = new DJDatasetListElement[data.size()];
    Iterator iter = data.iterator();
    int i = 0;
    while (iter.hasNext())
      elements[i++] = new DJDatasetListElement((DJDataset) iter.next());
    Arrays.sort(elements);

    return elements;
  }

  /**
   * Diese Methode liefert den aktuell aus der pers�nlichen Absenderliste (PAL)
   * ausgew�hlten Absender im Format "<Nachname>, <Vorname> (<Rolle>)" zur�ck.
   * Ist die PAL leer oder noch kein Absender ausgew�hlt, so liefert die Methode
   * den Leerstring "" zur�ck. Dieser Sonderfall sollte nat�rlich entsprechend
   * durch die aufrufende Methode behandelt werden.
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALProvider#getCurrentSender()
   * 
   * @return den aktuell aus der PAL ausgew�hlten Absender als String. Ist kein
   *         Absender ausgew�hlt wird der Leerstring "" zur�ckgegeben.
   */
  public String getCurrentSender()
  {
    try
    {
      DJDataset selected = getDatasourceJoiner().getSelectedDataset();
      return new DJDatasetListElement(selected).toString();
    }
    catch (DatasetNotFoundException e)
    {
      return "";
    }
  }

  /**
   * siehe {@link WollMuxFiles#getWollmuxConf()}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy getWollmuxConf()
  {
    return WollMuxFiles.getWollmuxConf();
  }

  /**
   * siehe {@link WollMuxFiles#getDEFAULT_CONTEXT()}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public URL getDEFAULT_CONTEXT()
  {
    return WollMuxFiles.getDEFAULT_CONTEXT();
  }

  /**
   * Liefert die Funktionsbibliothek, die die global definierten Funktionen
   * enth�lt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FunctionLibrary getGlobalFunctions()
  {
    return globalFunctions;
  }

  /**
   * Liefert die Funktionsbibliothek, die die global definierten Druckfunktionen
   * enth�lt.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public PrintFunctionLibrary getGlobalPrintFunctions()
  {
    return globalPrintFunctions;
  }

  /**
   * Liefert die Dialogbibliothek, die die Dialoge enth�lt, die in Funktionen
   * (Grundfunktion "DIALOG") verwendung finden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DialogLibrary getFunctionDialogs()
  {
    return funcDialogs;
  }

  /**
   * siehe {@link WollMuxFiles#isDebugMode()}.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public boolean isDebugMode()
  {
    return WollMuxFiles.isDebugMode();
  }

  /**
   * Liefert true, wenn f�r das XTextDocument doc ein TextDocumentModel im
   * WollMuxSingleton registriert ist, ansonsten false.
   * 
   * @param doc
   *          das Dokument f�r das ein TextDocumentModel gesucht wird.
   * @return true, wenn f�r das XTextDocument doc ein TextDocumentModel im
   *         WollMuxSingleton registriert ist, ansonsten false.
   */
  public boolean hasTextDocumentModel(XTextDocument doc)
  {
    HashableComponent key = new HashableComponent(doc);

    return currentTextDocumentModels.containsKey(key);
  }

  /**
   * Liefert das aktuelle TextDocumentModel zum �bergebenen XTextDocument doc;
   * existiert zu doc noch kein TextDocumentModel, so wird hier eines erzeugt
   * und das neu erzeugte zur�ck geliefert.
   * 
   * @param doc
   *          Das XTextDocument, zu dem das zugeh�rige TextDocumentModel
   *          zur�ckgeliefert werden soll.
   * @return Das zu doc zugeh�rige TextDocumentModel.
   */
  public TextDocumentModel getTextDocumentModel(XTextDocument doc)
  {
    HashableComponent key = new HashableComponent(doc);

    TextDocumentModel model = (TextDocumentModel) currentTextDocumentModels
        .get(key);
    if (model == null)
    {
      // Neues TextDocumentModel erzeugen, wenn es noch nicht existiert.
      model = new TextDocumentModel(doc);
      currentTextDocumentModels.put(key, model);
    }
    return model;
  }

  /**
   * L�scht das �bergebene TextDocumentModel von doc aus der internen Liste
   * aller aktuellen TextDocumentModels.
   * 
   * @param doc
   *          Das XTextDocument, dessen zugeh�riges TextDocumentModel aus der
   *          internen Liste gel�scht werden soll.
   */
  public void disposedTextDocument(XTextDocument doc)
  {
    HashableComponent key = new HashableComponent(doc);
    currentTextDocumentModels.remove(key);
  }

  /**
   * Diese Methode durchsucht das Element element bzw. dessen XEnumerationAccess
   * Interface rekursiv nach TextField.Annotation-Objekten und liefert das erste
   * gefundene TextField.Annotation-Objekt zur�ck, oder null, falls kein
   * entsprechendes Element gefunden wurde.
   * 
   * @param element
   *          Das erste gefundene AnnotationField oder null, wenn keines
   *          gefunden wurde.
   */
  public static XTextField findAnnotationFieldRecursive(Object element)
  {
    // zuerst die Kinder durchsuchen (falls vorhanden):
    if (UNO.XEnumerationAccess(element) != null)
    {
      XEnumeration xEnum = UNO.XEnumerationAccess(element).createEnumeration();

      while (xEnum.hasMoreElements())
      {
        try
        {
          Object child = xEnum.nextElement();
          XTextField found = findAnnotationFieldRecursive(child);
          // das erste gefundene Element zur�ckliefern.
          if (found != null) return found;
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }

    // jetzt noch schauen, ob es sich bei dem Element um eine Annotation
    // handelt:
    if (UNO.XTextField(element) != null)
    {
      Object textField = UNO.getProperty(element, "TextField");
      if (UNO.supportsService(
          textField,
          "com.sun.star.text.TextField.Annotation"))
      {
        return UNO.XTextField(textField);
      }
    }

    return null;
  }

  /**
   * Hilfsklasse, die es erm�glicht, UNO-Componenten in HashMaps abzulegen; der
   * Vergleich zweier HashableComponents mit equals(...) verwendet dazu den
   * sicheren UNO-Vergleich UnoRuntime.areSame(...). Die Methode hashCode
   * verwendet die sichere Oid, die UnoRuntime.generateOid(...) liefert.
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
      if (compo != null) return UnoRuntime.generateOid(compo).hashCode();
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
   * �berpr�ft, ob von url gelesen werden kann und wirft eine IOException, falls
   * nicht.
   * 
   * @throws IOException
   *           falls von url nicht gelesen werden kann.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void checkURL(URL url) throws IOException
  {
    url.openStream().close();
  }

  /**
   * Diese Methode erzeugt einen modalen Swing-Dialog zur Anzeige von
   * Informationen und kehrt erst nach Beendigung des Dialogs wieder zur�ck.
   * 
   * @param sTitle
   *          Titelzeile des Dialogs
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   */
  public static void showInfoModal(java.lang.String sTitle,
      java.lang.String sMessage)
  {
    try
    {
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

      JOptionPane pane = new JOptionPane(formattedMessage,
          javax.swing.JOptionPane.INFORMATION_MESSAGE);
      JDialog dialog = pane.createDialog(null, sTitle);
      dialog.setAlwaysOnTop(true);
      dialog.setVisible(true);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Holt sich den Frame von doc, f�hrt auf diesem ein queryDispatch() mit der
   * zu urlStr geh�renden URL aus und liefert den Ergebnis XDispatch zur�ck oder
   * null, falls der XDispatch nicht verf�gbar ist.
   * 
   * @param doc
   *          Das Dokument, dessen Frame f�r den Dispatch verwendet werden soll.
   * @param urlStr
   *          die URL in Form eines Strings (wird intern zu URL umgewandelt).
   * @return den gefundenen XDispatch oder null, wenn der XDispatch nicht
   *         verf�gbar ist.
   */
  public static XDispatch getDispatchForModel(XModel doc,
      com.sun.star.util.URL url)
  {
    if (doc == null) return null;

    XDispatchProvider dispProv = null;
    try
    {
      dispProv = UNO.XDispatchProvider(doc.getCurrentController().getFrame());
    }
    catch (java.lang.Exception e)
    {
    }

    if (dispProv != null)
    {
      return dispProv.queryDispatch(
          url,
          "_self",
          com.sun.star.frame.FrameSearchFlag.SELF);
    }
    return null;
  }

  /**
   * Der GlobalEventListener sorgt daf�r, dass der WollMux alle wichtigen
   * globalen Ereignisse wie z.B. ein OnNew on OnLoad abfangen und darauf
   * reagieren kann. In diesem Fall wird die Methode notifyEvent aufgerufen.
   * 
   * @author christoph.lutz
   */
  public static class GlobalEventListener implements
      com.sun.star.document.XEventListener
  {
    public void notifyEvent(com.sun.star.document.EventObject docEvent)
    {
      XTextDocument doc = UNO.XTextDocument(docEvent.Source);

      if (doc != null)
      {
        Logger.debug2("Incoming documentEvent for #"
                      + doc.hashCode()
                      + ": "
                      + docEvent.EventName);

        if (docEvent.EventName.equalsIgnoreCase("OnLoad")
            || docEvent.EventName.equalsIgnoreCase(("OnNew")))
        {
          // Verarbeitung von TextDocuments anstossen:
          WollMuxEventHandler.handleProcessTextDocument(doc);
        }
      }
    }

    public void disposing(EventObject arg0)
    {
      // nothing to do
    }
  }
}
