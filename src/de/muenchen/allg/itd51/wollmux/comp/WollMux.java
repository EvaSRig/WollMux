/*
 * Dateiname: WollMux.java
 * Projekt  : WollMux
 * Funktion : zentraler UNO-Service WollMux 
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
 * 05.12.2005 | BNK | line.separator statt \n                 |  
 * 06.06.2006 | LUT | + Abl�sung der Event-Klasse durch saubere Objektstruktur
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux.comp;

import com.sun.star.document.XEventListener;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.itd51.wollmux.DispatchHandler;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.XWollMux;

/**
 * Diese Klasse stellt den zentralen UNO-Service WollMux dar. Der Service hat
 * folgende Funktionen: Als XDispatchProvider und XDispatch behandelt er alle
 * "wollmux:kommando..." URLs und als XWollMux stellt er die Schnittstelle f�r
 * externe UNO-Komponenten dar. Der Service wird beim Starten von OpenOffice.org
 * automatisch (mehrfach) instanziiert, wenn OOo einen dispatchprovider f�r die in
 * der Datei Addons.xcu enthaltenen wollmux:... dispatches besorgen m�chte (dies
 * geschieht auch bei unsichtbar ge�ffneten Dokumenten). Als Folge wird das
 * WollMux-Singleton bei OOo-Start (einmalig) initialisiert.
 */
public class WollMux extends WeakBase implements XServiceInfo, XDispatchProvider,
    XWollMux
{

  /**
   * Dieses Feld ent�lt eine Liste aller Services, die dieser UNO-Service
   * implementiert.
   */
  public static final java.lang.String[] SERVICENAMES =
    { "de.muenchen.allg.itd51.wollmux.WollMux" };

  /**
   * Der Konstruktor initialisiert das WollMuxSingleton und startet damit den
   * eigentlichen WollMux. Der Konstuktor wird aufgerufen, bevor OpenOffice.org die
   * Methode executeAsync() aufrufen kann, die bei einem ON_FIRST_VISIBLE_TASK-Event
   * �ber den Job-Mechanismus ausgef�hrt wird.
   * 
   * @param context
   */
  public WollMux(XComponentContext ctx)
  {
    WollMuxSingleton.initialize(ctx);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#getSupportedServiceNames()
   */
  public String[] getSupportedServiceNames()
  {
    return SERVICENAMES;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#supportsService(java.lang.String)
   */
  public boolean supportsService(String sService)
  {
    int len = SERVICENAMES.length;
    for (int i = 0; i < len; i++)
    {
      if (sService.equals(SERVICENAMES[i])) return true;
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#getImplementationName()
   */
  public String getImplementationName()
  {
    return (WollMux.class.getName());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatchProvider#queryDispatch(com.sun.star.util.URL,
   *      java.lang.String, int)
   */
  public XDispatch queryDispatch( /* IN */com.sun.star.util.URL aURL,
  /* IN */String sTargetFrameName,
  /* IN */int iSearchFlags)
  {
    return DispatchHandler.globalWollMuxDispatches.queryDispatch(aURL,
      sTargetFrameName, iSearchFlags);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatchProvider#queryDispatches(com.sun.star.frame.DispatchDescriptor[])
   */
  public XDispatch[] queryDispatches( /* IN */DispatchDescriptor[] seqDescripts)
  {
    return DispatchHandler.globalWollMuxDispatches.queryDispatches(seqDescripts);
  }

  /**
   * Diese Methode liefert eine Factory zur�ck, die in der Lage ist den UNO-Service
   * zu erzeugen. Die Methode wird von UNO intern ben�tigt. Die Methoden
   * __getComponentFactory und __writeRegistryServiceInfo stellen das Herzst�ck des
   * UNO-Service dar.
   * 
   * @param sImplName
   * @return
   */
  public synchronized static XSingleComponentFactory __getComponentFactory(
      java.lang.String sImplName)
  {
    com.sun.star.lang.XSingleComponentFactory xFactory = null;
    if (sImplName.equals(WollMux.class.getName()))
      xFactory = Factory.createComponentFactory(WollMux.class, SERVICENAMES);
    return xFactory;
  }

  /**
   * Diese Methode registriert den UNO-Service. Sie wird z.B. beim unopkg-add im
   * Hintergrund aufgerufen. Die Methoden __getComponentFactory und
   * __writeRegistryServiceInfo stellen das Herzst�ck des UNO-Service dar.
   * 
   * @param xRegKey
   * @return
   */
  public synchronized static boolean __writeRegistryServiceInfo(XRegistryKey xRegKey)
  {
    return Factory.writeRegistryServiceInfo(WollMux.class.getName(),
      WollMux.SERVICENAMES, xRegKey);
  }

  /**
   * Diese Methode registriert einen XPALChangeEventListener, der updates empf�ngt
   * wenn sich die PAL �ndert. Nach dem Registrieren wird sofort ein
   * ON_SELECTION_CHANGED Ereignis ausgel�st, welches daf�r sort, dass sofort ein
   * erster update aller Listener ausgef�hrt wird. Die Methode ignoriert alle
   * XPALChangeEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht m�glich.
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALChangeEventBroadcaster#addPALChangeEventListener(de.muenchen.allg.itd51.wollmux.XPALChangeEventListener)
   */
  public void addPALChangeEventListener(XPALChangeEventListener l)
  {
    WollMuxEventHandler.handleAddPALChangeEventListener(l, null);
  }

  /**
   * Diese Methode registriert einen XPALChangeEventListener, der updates empf�ngt
   * wenn sich die PAL �ndert; nach der Registrierung wird gepr�ft, ob der WollMux
   * und der XPALChangeEventListener die selbe WollMux-Konfiguration verwenden, wozu
   * der Listener den HashCode wollmuxConfHashCode der aktuellen
   * WollMux-Konfiguration �bermittelt. Stimmt wollmuxConfHashCode nicht mit dem
   * HashCode der WollMux-Konfiguration des WollMux �berein, so erscheint ein Dialog,
   * der vor m�glichen Fehlern warnt. Nach dem Registrieren wird sofort ein
   * ON_SELECTION_CHANGED Ereignis ausgel�st, welches daf�r sort, dass sofort ein
   * erster update aller Listener ausgef�hrt wird. Die Methode ignoriert alle
   * XPALChangeEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht m�glich.
   * 
   * @param l
   *          Der zu registrierende XPALChangeEventListener
   * @param wollmuxConfHashCode
   *          Der HashCode der WollMux-Config der zur Konsistenzpr�fung herangezogen
   *          wird und �ber
   *          WollMuxFiles.getWollMuxConf().getStringRepresentation().hashCode()
   *          erzeugt wird.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @see de.muenchen.allg.itd51.wollmux.XPALChangeEventBroadcaster#addPALChangeEventListenerWithConsistencyCheck(de.muenchen.allg.itd51.wollmux.XPALChangeEventListener,
   *      int)
   */
  public void addPALChangeEventListenerWithConsistencyCheck(
      XPALChangeEventListener l, int wollmuxConfHashCode)
  {
    WollMuxEventHandler.handleAddPALChangeEventListener(l, new Integer(
      wollmuxConfHashCode));
  }

  /**
   * Diese Methode registriert einen Listener im WollMux, �ber den der WollMux �ber
   * den Status der Dokumentbearbeitung informiert (z.B. wenn ein Dokument
   * vollst�ndig bearbeitet/expandiert wurde). Die Methode ignoriert alle
   * XEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht m�glich.
   * 
   * Tritt ein entstprechendes Ereignis ein, so erfolgt der Aufruf der entsprechenden
   * Methoden XEventListener.notifyEvent(...) immer gleichzeitig (d.h. f�r jeden
   * Listener in einem eigenen Thread).
   * 
   * Der WollMux liefert derzeit folgende Events:
   * 
   * OnWollMuxProcessingFinished: Dieses Event wird erzeugt, wenn ein Textdokument
   * nach dem �ffnen vollst�ndig vom WollMux bearbeitet und expandiert wurde oder bei
   * allen anderen Dokumenttypen direkt nach dem �ffnen. D.h. f�r jedes in OOo
   * ge�ffnete Dokument erfolgt fr�her oder sp�ter ein solches Event.
   * 
   * @param l
   *          Der XEventListener, der bei Status�nderungen der Dokumentbearbeitung
   *          informiert werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @see com.sun.star.document.XEventBroadcaster#addEventListener(com.sun.star.document.XEventListener)
   */
  public void addEventListener(XEventListener l)
  {
    WollMuxEventHandler.handleAddDocumentEventListener(l);
  }

  /**
   * Diese Methode deregistriert einen XPALChangeEventListener wenn er bereits
   * registriert war.
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALChangeEventBroadcaster#removePALChangeEventListener(de.muenchen.allg.itd51.wollmux.XPALChangeEventListener)
   */
  public void removePALChangeEventListener(XPALChangeEventListener l)
  {
    WollMuxEventHandler.handleRemovePALChangeEventListener(l);
  }

  /**
   * Diese Methode deregistriert einen mit registerEventListener(XEventListener l)
   * registrierten XEventListener.
   * 
   * @param l
   *          der XEventListener, der deregistriert werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @see com.sun.star.document.XEventBroadcaster#removeEventListener(com.sun.star.document.XEventListener)
   */
  public void removeEventListener(XEventListener l)
  {
    WollMuxEventHandler.handleRemoveDocumentEventListener(l);
  }

  /**
   * Diese Methode setzt den aktuellen Absender der Pers�nlichen Absenderliste (PAL)
   * auf den Absender sender. Der Absender wird nur gesetzt, wenn die Parameter
   * sender und idx in der alphabetisch sortierten Absenderliste des WollMux
   * �bereinstimmen - d.h. die Absenderliste der veranlassenden SenderBox zum
   * Zeitpunkt der Auswahl konsistent zur PAL des WollMux war. Die Methode verwendet
   * f�r sender das selben Format wie es vom XPALProvider:getCurrentSender()
   * geliefert wird.
   */
  public void setCurrentSender(String sender, short idx)
  {
    Logger.debug2("WollMux.setCurrentSender(\"" + sender + "\", " + idx + ")");
    WollMuxEventHandler.handleSetSender(sender, idx);
  }

  /**
   * Diese Methode liefert den Wert zur Datenbankspalte dbSpalte, der dem Wert
   * entspricht, den das Dokumentkommando WM(CMD'insertValue' DB_SPALTE'<dbSpalte>')
   * in das Dokument einf�gen w�rde, oder den Leerstring "" wenn dieser Wert nicht
   * bestimmt werden kann (z.B. wenn ein ung�ltiger Spaltennamen dbSpalte �bergeben
   * wurde). So ist es z.B. m�glich, aus externen Anwendungen (z.B. Basic-Makros) auf
   * Werte des aktuell gesetzten Absenders des WollMux zuzugreifen.
   * 
   * Anmerkung: Ist die aufrufende Anwendung ein Basic-Makro, so muss damit gerechnet
   * werden, dass dieses Makro bereits in einer synchronisierten Umgebung abl�uft
   * (das ist Standard bei Basic-Makros). Um Deadlocks in Zusammenhang mit dem
   * WollMux zu vermeiden, darf die Methode also nicht �ber den WollMuxEventHandler
   * synchonisiert werden. Aufgrund der fehlenden Synchronisierung auf Seiten des
   * WollMux ist die Methode jedoch mit Vorsicht zu genie�en. Insbesondere sollten
   * die �ber diese Methode gelieferten Werte NICHT verwendet werden, um z.B. mit
   * setCurrentSender() Daten des WollMux zu manipulieren.
   * 
   * Zum Auslesen und Reagieren auf �nderungen der PersoenlichenAbsenderlist (PAL)
   * sollten die Methoden verwendet werden, die der XPALProvider anbietet. Diese
   * Methoden sind �ber den WollMuxEventHandler synchronisiert.
   * 
   * @param dbSpalte
   *          Name der Datenbankspalte deren Wert zur�ckgeliefert werden soll.
   * @return Der Wert der Datenbankspalte dbSpalte des aktuell ausgew�hlten Absenders
   *         oder "", wenn der Wert nicht bestimmt werden kann.
   */
  public String getValue(String dbSpalte)
  {
    try
    {
      String value =
        WollMuxSingleton.getInstance().getDatasourceJoiner().getSelectedDatasetTransformed().get(
          dbSpalte);
      if (value == null) value = "";
      return value;
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
      return "";
    }
  }
}
