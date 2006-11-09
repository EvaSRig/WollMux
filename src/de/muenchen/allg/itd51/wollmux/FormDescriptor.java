/*
 * Dateiname: FormDescriptor.java
 * Projekt  : WollMux
 * Funktion : Repr�sentiert die Formularbeschreibung eines Formulars in Form
 *            von ein bis mehreren WM(CMD'Form')-Kommandos mit den zugeh�rigen Notizen.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 24.07.2006 | LUT | Erstellung als FormDescriptor
 * 08.08.2006 | BNK | +fromConfigThingy(ConfigThingy conf)
 *                  | writeDocInfoFormularbeschreibung()
 * 11.08.2006 | BNK | umgeschrieben auf das Verwenden mehrerer Notizen in einem Rahmen
 * 19.10.2006 | BNK | WOLLMUX_FRAME_NAME public gemacht f�r Verwendung in FM4000.
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;

/**
 * TODO Doku anpassen an die �nderung dass jetzt nicht mehr die Infofelder
 * verwendet werden. Am besten auch Methodennamen refaktorisieren TODO
 * Logger-Meldungen ebenfalls anpassen daran, dass nicht mehr Infofelder
 * verwendet werden Diese Klasse repr�sentiert eine Formularbeschreibung eines
 * Formulardokuments, die sich zusammensetzt aus dem Feld
 * "WollMuxFormularbeschreibung" aus der DocumentInfo des Dokuments und/oder aus
 * einem oder mehrereren WM(CMD'Form')-Kommandos mit den zugeh�rigen
 * Notizfeldern, die die Beschreibungstexte in ConfigThingy-Syntax enthalten.
 * Beim Aufruf des Konstruktors wird zun�chst die DocumentInfo des Dokuments
 * ausgelesen und evtl. dort enthaltene WollMuxFormularbeschreibungen
 * �bernommen. Anschlie�end k�nnen �ber die add()-Methode einzelne
 * DocumentCommand.Form-Objekte hinzugef�gt werden k�nnen. Logisch betrachtet
 * werden alle Beschreibungstexte zu einer gro�en ConfigThingy-Struktur
 * zusammengef�gt und �ber die Methode toConfigThingy() bereitgestellt.
 * 
 * Die Klasse bietet dar�ber hinaus Methoden zum Abspeichern und Auslesen der
 * original-Feldwerte im DocumentInfo Feld "WollMuxFormularwerte" an.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class FormDescriptor
{

  /**
   * Enth�lt alle Formular-Abschnitte, die in der DocumentInfo bzw. den mit add
   * hinzugef�gten Form-Kommandos gefunden wurden.
   */
  private ConfigThingy formularConf;

  /**
   * Enth�lt die aktuellen Werte der Formularfelder als Zuordnung id -> Wert.
   */
  private HashMap formFieldValues;

  /**
   * Erzeugt einen neuen FormDescriptor. Danach k�nnen �ber add()
   * WM(CMD'Form')-Kommandos mit Formularbeschreibungsnotizen hinzugef�gt
   * werden.
   */
  public FormDescriptor()
  {
    this.formularConf = new ConfigThingy("WM");
    this.formFieldValues = new HashMap();

    // TODO textDocumentModel readDocInfoFormularbeschreibung();
    // TODO textDocumentModel readDocInfoFormularwerte();
  }

  /**
   * Wertet die WollMux-Formularbeschreibungsnotiz value aus, die von der Form
   * "WM( Formular(...) )" sein muss und f�gt diese der Gesamtbeschreibung
   * hinzu.
   * 
   * @param value
   *          darf null sein und wird in diesem Fall ignoriert, darf aber kein
   *          leerer String sein, sonst Fehler.
   */
  public void parseDocInfoFormularbeschreibung(String value)
  {
    // TODO textDocumentModel String value =
    // getDocInfoValue(WOLLMUX_FORMULARBESCHREIBUNG);
    if (value == null) return;

    try
    {
      ConfigThingy conf = new ConfigThingy("", null, new StringReader(value));
      addFormularSection(conf);
    }
    catch (java.lang.Exception e)
    {
      Logger
          .error(new ConfigurationErrorException(
              "Der Inhalt des Beschreibungsfeldes 'WollMuxFormularbeschreibung' in Datei->Eigenschaften->Benutzer ist fehlerhaft:\n"
                  + e.getMessage()));
      return;
    }
  }

  /**
   * Wertet werteStr aus (das von der Form "WM(FormularWerte(...))" sein muss
   * und �bertr�gt die gefundenen Werte in die internen Datenstrukturen.
   * 
   * @param werteStr
   *          darf null sein (und wird dann ignoriert) aber nicht der leere
   *          String.
   */
  public void parseDocInfoFormularwerte(String werteStr)
  {
    // TODO TextDocumentModel String werteStr =
    // getDocInfoValue(WOLLMUX_FORMULARWERTE);
    if (werteStr == null) return;

    // Werte-Abschnitt holen:
    ConfigThingy werte;
    try
    {
      ConfigThingy conf = new ConfigThingy("", null, new StringReader(werteStr));
      werte = conf.get("WM").get("Formularwerte");
    }
    catch (java.lang.Exception e)
    {
      Logger
          .error(new ConfigurationErrorException(
              "Der Inhalt des Beschreibungsfeldes 'WollMuxFormularwerte' in Datei->Eigenschaften->Benutzer ist fehlerhaft:\n"
                  + e.getMessage()));
      return;
    }

    // "Formularwerte"-Abschnitt auswerten.
    formFieldValues = new HashMap();
    Iterator iter = werte.iterator();
    while (iter.hasNext())
    {
      ConfigThingy element = (ConfigThingy) iter.next();
      try
      {
        String id = element.get("ID").toString();
        String value = element.get("VALUE").toString();
        formFieldValues.put(id, value);
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Liefert true gdw der FormDescriptor leer ist.
   */
  public boolean isEmpty()
  {
    return formularConf.count() == 0;
  }

  /**
   * F�gt den Formular-Abschnitt des �bergebenen configThingies zur
   * Gesamtbeschreibung formularConf hinzu
   * 
   * @param conf
   * @throws ConfigurationErrorException
   *           wenn kein Formular-Abschnitt vorhanden ist.
   */
  private void addFormularSection(ConfigThingy conf)
      throws ConfigurationErrorException
  {
    // Formular-Abschnitt auswerten:
    try
    {
      ConfigThingy formular = conf.get("WM").get("Formular");
      formularConf.addChild(formular);
    }
    catch (NodeNotFoundException e)
    {
      throw new ConfigurationErrorException(
          "Die Formularbeschreibung enth�lt keinen Abschnitt 'Formular':\n"
              + e.getMessage());
    }
  }

  /**
   * Liefert eine ConfigThingy-Repr�sentation, die unterhalb des Wurzelknotens
   * "WM" der Reihe nach die Vereinigung der "Formular"-Abschnitte aller
   * Formularbeschreibungen der enthaltenen WM(CMD'Form')-Kommandos enth�lt.
   * ACHTUNG! Es wird eine Referenz auf ein internes Objekt geliefert! Nicht
   * ver�ndern!
   * 
   * @return ConfigThingy-Repr�sentation mit dem Wurzelknoten "WM", die alle
   *         "Formular"-Abschnitte der Formularbeschreibungen enth�lt.
   */
  public ConfigThingy toConfigThingy()
  {
    return formularConf;
  }

  /**
   * Ersetzt die Formularbeschreibung dieses FormDescriptors durch die aus conf.
   * ACHTUNG! conf wird nicht kopiert sondern als Referenz eingebunden.
   * 
   * @param conf
   *          ein WM-Knoten, der "Formular"-Kinder hat.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void fromConfigThingy(ConfigThingy conf)
  {
    formularConf = conf;
  }

  /**
   * Informiert den FormDescriptor �ber den neuen Wert value der Formularfelder
   * mit der ID id; die �nderung wird erst nach einem Aufruf von
   * updateDocument() im "Formularwerte"-Abschnitt persistent gespeichert.
   * 
   * @param id
   *          die id der Formularfelder, deren Wert neu gesetzt wurde.
   * @param value
   *          der neu zu setzende Wert.
   */
  public void setFormFieldValue(String id, String value)
  {
    formFieldValues.put(id, value);
  }

  /**
   * Liefert den zuletzt gesetzten Wert des Formularfeldes mit der ID id zur�ck.
   * 
   * @param id
   *          Die id des Formularfeldes, dessen Wert zur�ck geliefert werden
   *          soll.
   * @return der zuletzt gesetzte Wert des Formularfeldes mit der ID id.
   */
  public String getFormFieldValue(String id)
  {
    return (String) formFieldValues.get(id);
  }

  /**
   * Liefert ein Set zur�ck, das alle dem FormDescriptor bekannten IDs f�r
   * Formularfelder enth�lt.
   * 
   * @return ein Set das alle dem FormDescriptor bekannten IDs f�r
   *         Formularfelder enth�lt.
   */
  public Set getFormFieldIDs()
  {
    return formFieldValues.keySet();
  }

  /**
   * Serialisiert die aktuellen Werte aller Fomularfelder.
   */
  public String getFormFieldValues()
  {
    // Neues ConfigThingy f�r "Formularwerte"-Abschnitt erzeugen:
    ConfigThingy werte = new ConfigThingy("WM");
    ConfigThingy formwerte = new ConfigThingy("Formularwerte");
    werte.addChild(formwerte);
    Iterator iter = formFieldValues.keySet().iterator();
    while (iter.hasNext())
    {
      String key = (String) iter.next();
      String value = (String) formFieldValues.get(key);
      if (key != null && value != null)
      {
        ConfigThingy entry = new ConfigThingy("");
        ConfigThingy cfID = new ConfigThingy("ID");
        cfID.add(key);
        ConfigThingy cfVALUE = new ConfigThingy("VALUE");
        cfVALUE.add(value);
        entry.addChild(cfID);
        entry.addChild(cfVALUE);
        formwerte.addChild(entry);
      }
    }

    // TODO textdocumentmodel String infoFieldName = WOLLMUX_FORMULARWERTE;
    // TODO textdocumentmodel writeDocInfo(infoFieldName, infoFieldValue);
    return werte.stringRepresentation();
  }

}
