/*
 * Dateiname: WollMuxSingleton.java
 * Projekt  : WollMux
 * Funktion : Singleton f�r zentrale WollMux-Methoden.
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
 * http://ec.europa.eu/idabc/en/document/7330
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
 * 27.03.2007 | BNK | Default-oooEinstellungen ausgelagert nach data/...
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XIndexContainer;
import com.sun.star.document.XEventListener;
import com.sun.star.form.FormButtonType;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.ui.XModuleUIConfigurationManagerSupplier;
import com.sun.star.ui.XUIConfigurationManager;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.uno.XInterface;
import com.sun.star.util.XChangesBatch;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
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
 * de.muenchen.allg.itd51.wollmux.comp.WollMux, der fr�her zentraler Anlaufpunkt war,
 * bedient sich gr��tenteils aus den zentralen Methoden des Singletons.
 */
public class WollMuxSingleton implements XPALProvider
{
  public static final String OVERRIDE_FRAG_DB_SPALTE = "OVERRIDE_FRAG_DB_SPALTE";

  private static WollMuxSingleton singletonInstance = null;

  /**
   * Enth�lt die im Funktionen-Abschnitt der wollmux,conf definierten Funktionen.
   */
  private FunctionLibrary globalFunctions;

  /**
   * Enth�lt die im Funktionsdialoge-Abschnitt der wollmux,conf definierten Dialoge.
   */
  private DialogLibrary funcDialogs;

  /**
   * Enth�lt die im Funktionen-Abschnitt der wollmux,conf definierten Funktionen.
   */
  private PrintFunctionLibrary globalPrintFunctions;

  /**
   * Der Wert von {@link #OVERRIDE_FRAG_DB_SPALTE}, d,h, der Name der Spalte, die
   * die pers�nliche OverrideFrag-Liste enth�lt. "" falls nicht definiert.
   */
  private String overrideFragDbSpalte;

  /**
   * Enth�lt den default XComponentContext in dem der WollMux (bzw. das OOo) l�uft.
   */
  private XComponentContext ctx;

  /**
   * Enth�lt alle registrierten SenderBox-Objekte.
   */
  private Vector<XPALChangeEventListener> registeredPALChangeListener;

  /**
   * Enth�lt alle registrierten XEventListener, die bei Status�nderungen der
   * Dokumentbearbeitung informiert werden.
   */
  private Vector<XEventListener> registeredDocumentEventListener;

  /**
   * Enth�lt eine Zuordnung von HashableComponent Objekten, die die XTextDocumente
   * repr�sentieren, auf die zugeh�rigen TextDocumentModels
   */
  private HashMap<HashableComponent, TextDocumentModel> currentTextDocumentModels;

  /**
   * Die WollMux-Hauptklasse ist als singleton realisiert.
   */
  private WollMuxSingleton(XComponentContext ctx)
  {
    // Der XComponentContext wir hier gesichert und vom WollMuxSingleton mit
    // getXComponentContext zur�ckgeliefert.
    this.ctx = ctx;

    this.currentTextDocumentModels =
      new HashMap<HashableComponent, TextDocumentModel>();

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

    registeredPALChangeListener = new Vector<XPALChangeEventListener>();

    registeredDocumentEventListener = new Vector<XEventListener>();

    WollMuxFiles.setupWollMuxDir();

    Logger.debug(L.m("StartupWollMux"));
    Logger.debug("Build-Info: " + getBuildInfo());
    Logger.debug("wollmuxConfFile = " + WollMuxFiles.getWollMuxConfFile().toString());
    Logger.debug("DEFAULT_CONTEXT \"" + WollMuxFiles.getDEFAULT_CONTEXT().toString()
      + "\"");
    Logger.debug("CONF_VERSION: " + getConfVersionInfo());

    /*
     * Datenquellen/Registriere Abschnitte verarbeiten. ACHTUNG! Dies muss vor
     * getDatasourceJoiner() geschehen, da die entsprechenden Datenquellen wom�glich
     * schon f�r WollMux-Datenquellen ben�tigt werden.
     */
    registerDatasources(WollMuxFiles.getWollmuxConf(),
      WollMuxFiles.getDEFAULT_CONTEXT());

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
    funcDialogs =
      WollMuxFiles.parseFunctionDialogs(WollMuxFiles.getWollmuxConf(), null, null);

    /*
     * Globale Funktionen parsen. ACHTUNG! Verwendet die Funktionsdialoge. Diese
     * m�ssen also vorher geparst sein. Als context wird null �bergeben, weil globale
     * Funktionen keinen Kontext haben.
     */
    globalFunctions =
      WollMuxFiles.parseFunctions(WollMuxFiles.getWollmuxConf(),
        getFunctionDialogs(), null, null);

    /*
     * Globale Druckfunktionen parsen.
     */
    globalPrintFunctions =
      WollMuxFiles.parsePrintFunctions(WollMuxFiles.getWollmuxConf());

    // Initialisiere EventProcessor
    WollMuxEventHandler.setAcceptEvents(successfulStartup);

    // register global EventListener
    try
    {
      UnoService eventBroadcaster =
        UnoService.createWithContext("com.sun.star.frame.GlobalEventBroadcaster",
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
      tastenkuerzel =
        WollMuxFiles.getWollmuxConf().query("Tastenkuerzel").getLastChild();
    }
    catch (NodeNotFoundException e)
    {}
    try
    {
      Shortcuts.createShortcuts(tastenkuerzel);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }

    // "Extras->Seriendruck (WollMux)" erzeugen:
    List<String> removeButtonsFor = new ArrayList<String>();
    removeButtonsFor.add(DispatchHandler.DISP_wmSeriendruck);
    createMenuButton(DispatchHandler.DISP_wmSeriendruck,
      L.m("Seriendruck (WollMux)"), ".uno:ToolsMenu", ".uno:MailMergeWizard",
      removeButtonsFor);

    // "Help->Info �ber WollMux" erzeugen:
    removeButtonsFor.clear();
    removeButtonsFor.add(DispatchHandler.DISP_wmAbout);
    createMenuButton(DispatchHandler.DISP_wmAbout,
      L.m("Info �ber Vorlagen und Formulare (WollMux)"), ".uno:HelpMenu",
      ".uno:About", removeButtonsFor);

    // Setzen der in den Abschnitten OOoEinstellungen eingestellten
    // Konfigurationsoptionen
    ConfigThingy oooEinstellungenConf =
      WollMuxFiles.getWollmuxConf().query("OOoEinstellungen");
    for (Iterator<ConfigThingy> iter = oooEinstellungenConf.iterator(); iter.hasNext();)
    {
      ConfigThingy settings = iter.next();
      setConfigurationValues(settings);
    }
  }

  /**
   * Diese Methode liefert die Instanz des WollMux-Singletons. Ist der WollMux noch
   * nicht initialisiert, so liefert die Methode null!
   * 
   * @return Instanz des WollMuxSingletons oder null.
   */
  public static WollMuxSingleton getInstance()
  {
    return singletonInstance;
  }

  /**
   * Diese Methode initialisiert das WollMuxSingleton (nur dann, wenn es noch nicht
   * initialisiert wurde)
   */
  public static void initialize(XComponentContext ctx)
  {
    if (singletonInstance == null)
    {
      singletonInstance = new WollMuxSingleton(ctx);

      // Pr�fen ob Doppelt- oder Halbinstallation vorliegt.
      WollMuxEventHandler.handleCheckInstallation();

      // Event ON_FIRST_INITIALIZE erzeugen:
      WollMuxEventHandler.handleInitialize();
    }
  }

  /**
   * Diese Methode liefert die erste Zeile aus der buildinfo-Datei der aktuellen
   * WollMux-Installation zur�ck. Der Build-Status wird w�hrend dem Build-Prozess mit
   * dem Kommando "svn info" auf das Projektverzeichnis erstellt. Die Buildinfo-Datei
   * buildinfo enth�lt die Paketnummer und die svn-Revision und ist im
   * WollMux.oxt-Paket sowie in der WollMux.uno.jar-Datei abgelegt.
   * 
   * Kann dieses File nicht gelesen werden, so wird eine entsprechende Ersatzmeldung
   * erzeugt (siehe Sourcecode).
   * 
   * @return Der Build-Status der aktuellen WollMux-Installation.
   */
  public String getBuildInfo()
  {
    try
    {
      URL url = WollMuxSingleton.class.getClassLoader().getResource("buildinfo");
      if (url != null)
      {
        BufferedReader in =
          new BufferedReader(new InputStreamReader(url.openStream()));
        return in.readLine().toString();
      }
    }
    catch (java.lang.Exception x)
    {}
    return L.m("Version: unbekannt");
  }

  /**
   * Diese Methode liefert die Versionsinformation der aktuell verwendeten
   * wollmux-Konfiguration (z.B. "wollmux-standard-config-2.2.1") als String zur�ck,
   * wenn in der Konfiguration ein entsprechender CONF_VERSION-Schl�ssel definiert
   * ist, oder "unbekannt", falls der dieser Schl�ssel nicht existiert.
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
      return L.m("unbekannt");
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
   * Diese Methode liefert eine Instanz auf den aktuellen DatasourceJoiner zur�ck.
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
    Iterator<ConfigThingy> iter =
      conf.query("Datenquellen").query("Registriere").iterator();
    while (iter.hasNext())
    {
      ConfigThingy regConf = iter.next();
      String name;
      try
      {
        name = regConf.get("NAME").toString();
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(
          L.m("NAME-Attribut fehlt in Datenquellen/Registriere-Abschnitt"), e);
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
          L.m(
            "URL-Attribut fehlt in Datenquellen/Registriere-Abschnitt f�r Datenquelle '%1'",
            name), e);
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
        Logger.error(L.m(
          "Fehler beim �berpr�fen, ob Datenquelle '%1' bereits registriert ist",
          name), x);
      }

      Logger.debug(L.m(
        "Versuche, Datenquelle '%1' bei OOo zu registrieren f�r URL '%2'", name,
        urlStr));

      String parsedUrl;
      try
      {
        URL url = new URL(context, ConfigThingy.urlEncode(urlStr));
        parsedUrl = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;
      }
      catch (java.lang.Exception x)
      {
        Logger.error(L.m(
          "Fehler beim Registrieren von Datenquelle '%1': Illegale URL: '%2'", name,
          urlStr), x);
        continue;
      }

      try
      {
        Object datasource = UNO.XNameAccess(UNO.dbContext).getByName(parsedUrl);
        UNO.dbContext.registerObject(name, datasource);
        if (!UnoRuntime.areSame(UNO.dbContext.getRegisteredObject(name), datasource))
          Logger.error(L.m(
            "Testzugriff auf Datenquelle '%1' nach Registrierung fehlgeschlagen",
            name));
      }
      catch (Exception x)
      {
        Logger.error(
          L.m(
            "Fehler beim Registrieren von Datenquelle '%1'. Stellen Sie sicher, dass die URL '%2' g�ltig ist.",
            name, parsedUrl), x);
        continue;
      }

    }
  }

  /**
   * Diese Methode registriert einen XPALChangeEventListener, der updates empf�ngt
   * wenn sich die PAL �ndert. Die Methode ignoriert alle
   * XPALChangeEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht m�glich.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen werden,
   * sondern jeder Aufruf muss �ber den EventHandler laufen. Deswegen exportiert
   * WollMuxSingleton auch nicht das XPALChangedBroadcaster-Interface.
   */
  public void addPALChangeEventListener(XPALChangeEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::addPALChangeEventListener()");

    if (listener == null) return;

    Iterator<XPALChangeEventListener> i = registeredPALChangeListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) return;
    }
    registeredPALChangeListener.add(listener);
  }

  /**
   * Diese Methode registriert einen XEventListener, der Nachrichten empf�ngt wenn
   * sich der Status der Dokumentbearbeitung �ndert (z.B. wenn ein Dokument
   * vollst�ndig bearbeitet/expandiert wurde). Die Methode ignoriert alle
   * XEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht m�glich.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen werden,
   * sondern jeder Aufruf muss �ber den EventHandler laufen. Deswegen exportiert
   * WollMuxSingleton auch nicht das XEventBroadcaster-Interface.
   */
  public void addDocumentEventListener(XEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::addDocumentEventListener()");

    if (listener == null) return;

    Iterator<XEventListener> i = registeredDocumentEventListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) return;
    }
    registeredDocumentEventListener.add(listener);
  }

  /**
   * Diese Methode deregistriert einen XPALChangeEventListener wenn er bereits
   * registriert war.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen werden,
   * sondern jeder Aufruf muss �ber den EventHandler laufen. Deswegen exportiert
   * WollMuxSingleton auch nicht das XPALChangedBroadcaster-Interface.
   */
  public void removePALChangeEventListener(XPALChangeEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::removePALChangeEventListener()");
    Iterator<XPALChangeEventListener> i = registeredPALChangeListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) i.remove();
    }
  }

  /**
   * Diese Methode deregistriert einen XEventListener wenn er bereits registriert
   * war.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen werden,
   * sondern jeder Aufruf muss �ber den EventHandler laufen. Deswegen exportiert
   * WollMuxSingleton auch nicht das XEventBroadcaster-Interface.
   */
  public void removeDocumentEventListener(XEventListener listener)
  {
    Logger.debug2("WollMuxSingleton::removeDocumentEventListener()");
    Iterator<XEventListener> i = registeredDocumentEventListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) i.remove();
    }
  }

  /**
   * Erzeugt einen persistenten Men�eintrag mit der KommandoUrl cmdUrl und dem Label
   * label in dem durch mit insertIntoMenuUrl beschriebenen Toplevelmen� des Writers
   * und ordnet ihn direkt oberhalb des bereits bestehenden Men�punktes mit der URL
   * insertBeforeElementUrl an. Alle Buttons, deren Url in der Liste removeCmdUrls
   * aufgef�hrt sind werden dabei vorher gel�scht (v.a. sollte cmdUrl aufgef�hrt
   * sein, damit nicht der selbe Button doppelt erscheint).
   */
  private static void createMenuButton(String cmdUrl, String label,
      String insertIntoMenuUrl, String insertBeforeElementUrl,
      List<String> removeCmdUrls)
  {
    final String settingsUrl = "private:resource/menubar/menubar";

    try
    {
      // Men�leiste aus des Moduls com.sun.star.text.TextDocument holen:
      XModuleUIConfigurationManagerSupplier suppl =
        UNO.XModuleUIConfigurationManagerSupplier(UNO.createUNOService("com.sun.star.ui.ModuleUIConfigurationManagerSupplier"));
      XUIConfigurationManager cfgMgr =
        UNO.XUIConfigurationManager(suppl.getUIConfigurationManager("com.sun.star.text.TextDocument"));
      XIndexAccess menubar = UNO.XIndexAccess(cfgMgr.getSettings(settingsUrl, true));

      // Elemente des .uno:ToolsMenu besorgen:
      XIndexContainer toolsMenu = null;
      int idx = findElementWithCmdURL(menubar, insertIntoMenuUrl);
      if (idx >= 0)
      {
        UnoProps desc = new UnoProps((PropertyValue[]) menubar.getByIndex(idx));
        toolsMenu =
          UNO.XIndexContainer(desc.getPropertyValue("ItemDescriptorContainer"));
      }

      // Seriendruck-Button l�schen, wenn er bereits vorhanden ist.
      for (String rCmdUrl : removeCmdUrls)
      {
        idx = findElementWithCmdURL(toolsMenu, rCmdUrl);
        if (idx >= 0) toolsMenu.removeByIndex(idx);
      }

      // SeriendruckAssistent suchen
      idx = findElementWithCmdURL(toolsMenu, insertBeforeElementUrl);
      if (idx >= 0)
      {
        UnoProps desc = new UnoProps();
        desc.setPropertyValue("CommandURL", cmdUrl);
        desc.setPropertyValue("Type", FormButtonType.PUSH);
        desc.setPropertyValue("Label", label);
        toolsMenu.insertByIndex(idx, desc.getProps());
        cfgMgr.replaceSettings(settingsUrl, menubar);
        UNO.XUIConfigurationPersistence(cfgMgr).store();
      }
    }
    catch (java.lang.Exception e)
    {}
  }

  /**
   * Liefert den Index des ersten Men�elements aus dem Men� menu zur�ck, dessen
   * CommandURL mit cmdUrl identisch ist oder -1, falls kein solches Element gefunden
   * wurde.
   * 
   * @return Liefert den Index des ersten Men�elements mit CommandURL cmdUrl oder -1.
   */
  private static int findElementWithCmdURL(XIndexAccess menu, String cmdUrl)
  {
    try
    {
      for (int i = 0; i < menu.getCount(); ++i)
      {
        PropertyValue[] desc = (PropertyValue[]) menu.getByIndex(i);
        for (int j = 0; j < desc.length; j++)
        {
          if ("CommandURL".equals(desc[j].Name) && cmdUrl.equals(desc[j].Value))
            return i;
        }
      }
    }
    catch (java.lang.Exception e)
    {}
    return -1;
  }

  /**
   * Setzt die im ConfigThingy �bergebenen OOoEinstellungen-Abschnitt enthaltenen
   * Einstellungen in der OOo-Registry.
   * 
   * @param oooEinstellungenConf
   *          Der Knoten OOoEinstellungen eines solchen Abschnitts.
   */
  private static void setConfigurationValues(ConfigThingy oooEinstellungenConf)
  {
    for (Iterator<ConfigThingy> iter = oooEinstellungenConf.iterator(); iter.hasNext();)
    {
      ConfigThingy element = iter.next();
      try
      {
        String node = element.get("NODE").toString();
        String prop = element.get("PROP").toString();
        String type = element.get("TYPE").toString();
        String value = element.get("VALUE").toString();
        Object v = getObjectByType(type, value);

        setConfigurationValue(node, prop, v);
      }
      catch (java.lang.Exception e)
      {
        Logger.error(L.m("OOoEinstellungen: Konnte Einstellung '%1'nicht setzen:",
          element.stringRepresentation()), e);
      }
    }
  }

  /**
   * Konvertiert den als String �bergebenen Wert value in ein Objekt vom Typ type
   * oder liefert eine IllegalArgumentException, wenn die Werte nicht konvertiert
   * werden k�nnen.
   * 
   * @param type
   *          Der Typ in den konvertiert werden soll ('boolean', 'integer', 'float',
   *          'string').
   * @param value
   *          Der zu konvertierende Wert.
   * @return Das neue Objekt vom entsprechenden Typ.
   * @throws IllegalArgumentException
   *           type oder value sind ung�ltig oder fehlerhaft.
   */
  private static Object getObjectByType(String type, String value)
      throws IllegalArgumentException
  {
    if (type.equalsIgnoreCase("boolean"))
    {
      return new Boolean(value);
    }
    else if (type.equalsIgnoreCase("integer"))
    {
      return new Integer(value);
    }
    else if (type.equalsIgnoreCase("float"))
    {
      return new Float(value);
    }
    else if (type.equalsIgnoreCase("string"))
    {
      return new String(value);
    }

    throw new IllegalArgumentException(
      L.m(
        "Der TYPE '%1' ist nicht g�ltig. G�ltig sind 'boolean', 'integer', 'float' und 'string'.",
        type));
  }

  /**
   * Setzt eine Einstellung value in der OOo-Registry, wobei die Position im
   * Registry-Baum durch node und prop beschrieben wird.
   * 
   * @param node
   *          z.B. "/org.openoffice.Inet/Settings"
   * @param prop
   *          z.B. "ooInetProxyType"
   * @param value
   *          der zu setzende Wert als Objekt vom entsprechenden Typ.
   */
  private static void setConfigurationValue(String node, String prop, Object value)
  {
    XChangesBatch updateAccess = UNO.getConfigurationUpdateAccess(node);
    if (value != null) UNO.setProperty(updateAccess, prop, value);
    if (updateAccess != null) try
    {
      updateAccess.commitChanges();
    }
    catch (WrappedTargetException e)
    {
      Logger.error(e);
    }
  }

  /**
   * Liefert einen Iterator auf alle registrierten SenderBox-Objekte.
   * 
   * @return Iterator auf alle registrierten SenderBox-Objekte.
   */
  public Iterator<XPALChangeEventListener> palChangeListenerIterator()
  {
    return registeredPALChangeListener.iterator();
  }

  /**
   * Liefert einen Iterator auf alle registrierten XEventListener-Objekte, die �ber
   * �nderungen am Status der Dokumentverarbeitung informiert werden sollen.
   * 
   * @return Iterator auf alle registrierten XEventListener-Objekte.
   */
  public Iterator<XEventListener> documentEventListenerIterator()
  {
    return registeredDocumentEventListener.iterator();
  }

  /**
   * Diese Methode liefert eine alphabethisch aufsteigend sortierte Liste aller
   * Eintr�ge der Pers�nlichen Absenderliste (PAL) in einem String-Array, wobei die
   * einzelnen Eintr�ge in der Form "<Nachname>, <Vorname> (<Rolle>)" sind.
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
   * Diese Methode liefert alle DJDatasetListElemente der Pers�nlichen Absenderliste
   * (PAL) in alphabetisch aufsteigend sortierter Reihenfolge.
   * 
   * @return alle DJDatasetListElemente der Pers�nlichen Absenderliste (PAL) in
   *         alphabetisch aufsteigend sortierter Reihenfolge.
   */
  public DJDatasetListElement[] getSortedPALEntries()
  {
    // Liste der entries aufbauen.
    QueryResults data = getDatasourceJoiner().getLOS();

    DJDatasetListElement[] elements = new DJDatasetListElement[data.size()];
    Iterator<Dataset> iter = data.iterator();
    int i = 0;
    while (iter.hasNext())
      elements[i++] = new DJDatasetListElement((DJDataset) iter.next());
    Arrays.sort(elements);

    return elements;
  }

  /**
   * Diese Methode liefert den aktuell aus der pers�nlichen Absenderliste (PAL)
   * ausgew�hlten Absender im Format "<Nachname>, <Vorname> (<Rolle>)" zur�ck. Ist
   * die PAL leer oder noch kein Absender ausgew�hlt, so liefert die Methode den
   * Leerstring "" zur�ck. Dieser Sonderfall sollte nat�rlich entsprechend durch die
   * aufrufende Methode behandelt werden.
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
   * Liefert die Funktionsbibliothek, die die global definierten Funktionen enth�lt.
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
   * Liefert die pers�nliche OverrideFrag-Liste des aktuell gew�hlten Absenders.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * TESTED
   */
  /* package private */ConfigThingy getInitialOverrideFragMap()
  {
    ConfigThingy overrideFragConf = new ConfigThingy("overrideFrag");
    if (overrideFragDbSpalte == null)
    {
      ConfigThingy overrideFragDbSpalteConf =
        getWollmuxConf().query(OVERRIDE_FRAG_DB_SPALTE, 1);
      try
      {
        overrideFragDbSpalte = overrideFragDbSpalteConf.getLastChild().toString();
      }
      catch (NodeNotFoundException x)
      {
        // keine OVERRIDE_FRAG_DB_SPALTE Direktive gefunden
        overrideFragDbSpalte = "";
      }
    }

    if (overrideFragDbSpalte.length() > 0)
    {
      try
      {
        Dataset ds = getDatasourceJoiner().getSelectedDatasetTransformed();
        String value = ds.get(overrideFragDbSpalte);
        if (value == null) value = "";
        overrideFragConf = new ConfigThingy("overrideFrag", value);
      }
      catch (DatasetNotFoundException e)
      {
        Logger.log(L.m("Kein Absender ausgew�hlt => %1 bleibt wirkungslos",
          OVERRIDE_FRAG_DB_SPALTE));
      }
      catch (ColumnNotFoundException e)
      {
        Logger.error(L.m("%2 spezifiziert Spalte '%1', die nicht vorhanden ist",
          overrideFragDbSpalte, OVERRIDE_FRAG_DB_SPALTE), e);
      }
      catch (IOException x)
      {
        Logger.error(L.m("Fehler beim Parsen der %2 '%1'", overrideFragDbSpalte,
          OVERRIDE_FRAG_DB_SPALTE), x);
      }
      catch (SyntaxErrorException x)
      {
        Logger.error(L.m("Fehler beim Parsen der %2 '%1'", overrideFragDbSpalte,
          OVERRIDE_FRAG_DB_SPALTE), x);
      }
    }

    return overrideFragConf;
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
   * Diese Methode tut nichts ausser zu pr�fen, ob es sich bei dem �bergebenen String
   * id um einen g�ltigen Bezeichner gem�� der Syntax f�r WollMux-Config-Dateien
   * handelt und im negativen Fall eine InvalidIdentifierException zu werfen.
   * 
   * @param id
   *          zu pr�fende ID
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @throws InvalidIdentifierException
   */
  public static void checkIdentifier(String id) throws InvalidIdentifierException
  {
    if (!id.matches("^[a-zA-Z_][a-zA-Z_0-9]*$"))
      throw new InvalidIdentifierException(id);
  }

  public static class InvalidIdentifierException extends java.lang.Exception
  {
    private static final long serialVersionUID = 495666967644874471L;

    private String invalidId;

    public InvalidIdentifierException(String invalidId)
    {
      this.invalidId = invalidId;
    }

    public String getMessage()
    {
      return L.m(
        "Der Bezeichner '%1' ist ung�ltig, und darf nur die Zeichen a-z, A-Z, _ und 0-9 enthalten, wobei das erste Zeichen keine Ziffer sein darf.",
        invalidId);
    }
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
   * existiert zu doc noch kein TextDocumentModel, so wird hier eines erzeugt und das
   * neu erzeugte zur�ck geliefert.
   * 
   * @param doc
   *          Das XTextDocument, zu dem das zugeh�rige TextDocumentModel
   *          zur�ckgeliefert werden soll.
   * @return Das zu doc zugeh�rige TextDocumentModel.
   */
  public TextDocumentModel getTextDocumentModel(XTextDocument doc)
  {
    HashableComponent key = new HashableComponent(doc);

    TextDocumentModel model = currentTextDocumentModels.get(key);
    if (model == null)
    {
      // Neues TextDocumentModel erzeugen, wenn es noch nicht existiert.
      model = new TextDocumentModel(doc);
      currentTextDocumentModels.put(key, model);
    }
    return model;
  }

  /**
   * Liefert das aktuelle TextDocumentModel zum �bergebenen XFrame frame oder null,
   * falls dem Frame kein entsprechendes TextDocumentModel zugeordnet werden kann.
   * 
   * ACHTUNG: Die Methode wird vom DispatchHandler verwendet, um das
   * TextDocumentModel eines Frames zu bestimmen und wird daher NICHT mit dem
   * WollMuxEventHandler synchronisiert aufgerufen. Daher darf z.B. im Gegensatz zu
   * getTextDocumentModel(XTextDocument) hier auch kein TextDocumentModel erzeugt
   * werden, wenn es nicht bereits existiert.
   * 
   * Der Zugriff auf das TextDocumentModel in der nicht synchronisierten
   * Aufrufmethode des DispatchHandler darf daher auch auschlie�end lesend sein.
   * 
   * @param frame
   *          Der Frame, �ber den das TextDocumentModel gesucht werden soll.
   * @return Das zu frame zugeh�rige TextDocumentModel oder null, falls keine
   *         Zuordnung gefunden werden konnte.
   */
  public TextDocumentModel getTextDocumentModelForFrame(XFrame frame)
  {
    if (frame == null) return null;

    try
    {
      HashableComponent key =
        new HashableComponent(frame.getController().getModel());
      return currentTextDocumentModels.get(key);
    }
    catch (java.lang.Exception e)
    {
      return null;
    }
  }

  /**
   * L�scht das �bergebene TextDocumentModel von doc aus der internen Liste aller
   * aktuellen TextDocumentModels.
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
   *          Das erste gefundene AnnotationField oder null, wenn keines gefunden
   *          wurde.
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
      if (UNO.supportsService(textField, "com.sun.star.text.TextField.Annotation"))
      {
        return UNO.XTextField(textField);
      }
    }

    return null;
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
   * Diese Methode erzeugt einen modalen Swing-Dialog zur Anzeige von Informationen
   * und kehrt erst nach Beendigung des Dialogs wieder zur�ck. Der sichtbare Text
   * wird dabei ab einer L�nge von 50 Zeichen automatisch umgebrochen.
   * 
   * @param sTitle
   *          Titelzeile des Dialogs
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   */
  public static void showInfoModal(java.lang.String sTitle, java.lang.String sMessage)
  {
    showInfoModal(sTitle, sMessage, 50);
  }

  /**
   * Diese Methode erzeugt einen modalen Swing-Dialog zur Anzeige von Informationen
   * und kehrt erst nach Beendigung des Dialogs wieder zur�ck.
   * 
   * @param sTitle
   *          Titelzeile des Dialogs
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   * @param margin
   *          ist margin > 0 und sind in einer Zeile mehr als margin Zeichen
   *          vorhanden, so wird der Text beim n�chsten Leerzeichen umgebrochen.
   */
  public static void showInfoModal(java.lang.String sTitle,
      java.lang.String sMessage, int margin)
  {
    try
    {
      // zu lange Strings ab margin Zeichen umbrechen:
      String formattedMessage = "";
      String[] lines = sMessage.split("\n");
      for (int i = 0; i < lines.length; i++)
      {
        String[] words = lines[i].split(" ");
        int chars = 0;
        for (int j = 0; j < words.length; j++)
        {
          String word = words[j];
          if (margin > 0 && chars > 0 && chars + word.length() > margin)
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

      JOptionPane pane =
        new JOptionPane(formattedMessage,
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
   * Holt sich den Frame von doc, f�hrt auf diesem ein queryDispatch() mit der zu
   * urlStr geh�renden URL aus und liefert den Ergebnis XDispatch zur�ck oder null,
   * falls der XDispatch nicht verf�gbar ist.
   * 
   * @param doc
   *          Das Dokument, dessen Frame f�r den Dispatch verwendet werden soll.
   * @param urlStr
   *          die URL in Form eines Strings (wird intern zu URL umgewandelt).
   * @return den gefundenen XDispatch oder null, wenn der XDispatch nicht verf�gbar
   *         ist.
   */
  public static XDispatch getDispatchForModel(XModel doc, com.sun.star.util.URL url)
  {
    if (doc == null) return null;

    XDispatchProvider dispProv = null;
    try
    {
      dispProv = UNO.XDispatchProvider(doc.getCurrentController().getFrame());
    }
    catch (java.lang.Exception e)
    {}

    if (dispProv != null)
    {
      return dispProv.queryDispatch(url, "_self",
        com.sun.star.frame.FrameSearchFlag.SELF);
    }
    return null;
  }

  /**
   * Der GlobalEventListener sorgt daf�r, dass der WollMux alle wichtigen globalen
   * Ereignisse wie z.B. ein OnNew on OnLoad abfangen und darauf reagieren kann. In
   * diesem Fall wird die Methode notifyEvent aufgerufen. Wichtig ist dabei, dass der
   * Verarbeitungsstatus f�r alle Dokumenttypen (auch nicht-Textdokumente) erfasst
   * wird, damit der WollMux auch f�r diese Komponenten onWollMuxProcessingFinished
   * liefern kann.
   * 
   * @author christoph.lutz
   */
  public static class GlobalEventListener implements
      com.sun.star.document.XEventListener
  {
    private static ArrayList<XComponent> processedCompos =
      new ArrayList<XComponent>();

    private static boolean isProcessed(XComponent compo)
    {
      if (compo == null) return false;
      for (XComponent c : processedCompos)
        if (UnoRuntime.areSame(c, compo)) return true;
      return false;
    }

    private static void setProcessed(XComponent compo)
    {
      if (compo == null) return;
      if (!isProcessed(compo)) processedCompos.add(compo);
    }

    private static void unset(XComponent compo)
    {
      if (compo == null) return;
      for (Iterator<XComponent> iter = processedCompos.iterator(); iter.hasNext();)
      {
        if (UnoRuntime.areSame(iter.next(), compo)) iter.remove();
      }
    }

    public void notifyEvent(com.sun.star.document.EventObject docEvent)
    {
      XComponent compo = UNO.XComponent(docEvent.Source);
      if (compo == null) return;

      XTextDocument xTextDoc = UNO.XTextDocument(compo);

      String event = docEvent.EventName;

      // Im Gegensatz zu OnLoad oder OnNew wird das Event OnViewCreated auch bei
      // unsichtbar ge�ffneten Dokumenten erzeugt. Daher wird nun haupts�chlich
      // dieses Event zur Initiierung der Dokumentbearbeitung verwendet.
      if (event.equals("OnViewCreated") && !isProcessed(compo))
      {
        if (xTextDoc != null)
        {
          WollMuxEventHandler.handleProcessTextDocument(xTextDoc);
        }
        else
        {
          WollMuxEventHandler.handleNotifyDocumentEventListener(
            "OnWollMuxProcessingFinished", compo);
        }
        setProcessed(compo);
      }

      // Die Events OnLoad und OnNew kommen nur bei sichtbar ge�ffneten Dokumenten.
      // Das macht die Events f�r unsichtbar ge�ffnete Dokumente unbrauchbar. Nur
      // falls das Event OnViewCreated nicht empfangen wurden (z.B. m�glicherweise
      // bei alten OOo-Verisonen), greift als Fallback die Dokumentbearbeitung �ber
      // OnNew bzw. OnLoad.
      else if ((event.equals("OnLoad") || event.equals(("OnNew")))
        && !isProcessed(compo))
      {
        if (xTextDoc != null)
        {
          WollMuxEventHandler.handleProcessTextDocument(xTextDoc);
        }
        else
        {
          WollMuxEventHandler.handleNotifyDocumentEventListener(
            "OnWollMuxProcessingFinished", compo);
        }
        setProcessed(compo);
      }

      // h�lt die Liste processedCompos sauber:
      else if ((event.equals("OnUnload"))) unset(compo);
    }

    public void disposing(EventObject arg0)
    {
    // nothing to do
    }
  }
}
