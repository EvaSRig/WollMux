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

import com.sun.star.container.XEnumeration;
import com.sun.star.document.XDocumentInfo;
import com.sun.star.lang.ArrayIndexOutOfBoundsException;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;

/**
 * Diese Klasse repr�sentiert eine Formularbeschreibung eines Formulardokuments
 * in Form eines oder mehrerer WM(CMD'Form')-Kommandos mit den zugeh�rigen
 * Notizfeldern, die die Beschreibungstexte in ConfigThingy-Syntax enthalten.
 * Die Klasse startet zun�chst als ein leerer Container f�r
 * DocumentCommand.Form-Objekte, in den �ber die add()-Methode einzelne
 * DocumentCommand.Form-Objekte hinzugef�gt werden k�nnen. Logisch betrachtet
 * werden alle Beschreibungstexte zu einer gro�en ConfigThingy-Struktur
 * zusammengef�gt und �ber die Methode toConfigThingy() bereitgestellt.
 * 
 * Die Klasse bietet dar�ber hinaus Methoden zum Abspeichern und Auslesen der
 * original-Feldwerte im Notizfeld des ersten DocumentCommand.Form-Objekts an,
 * das einen Formularwerte-Abschnitt enth�lt.
 * 
 * Im Zusammenhang mit der EntwicklerGUI k�nnten auch alle Operationen der
 * EntwicklerGUI an der Formularbeschreibung (Hinzuf�gen/L�schen/Verschieben von
 * Eingabeelementen) �ber diese Klasse abstrahiert werden.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class FormDescriptor
{
  /**
   * Das Dokument, das als Fabrik f�r neue Annotations ben�tigt wird.
   */
  private XTextDocument doc;

  /**
   * Enth�lt alle Formular-Abschnitte, die in den mit add hinzugef�gten
   * Form-Kommandos gefunden wurden.
   */
  private ConfigThingy formularConf;

  /**
   * Enth�lt die aktuellen Werte der Formularfelder als Zuordnung id -> Wert.
   */
  private HashMap formFieldValues;

  /**
   * Zeigt an, ob der FormDescriptor leer ist, oder ob mindestens ein g�ltiges
   * WM(CMD'Form')-Kommando mit add() hinzugef�gt wurde, das einen
   * Formular-Abschnitt enth�lt.
   */
  private boolean isEmpty;

  /**
   * Erzeugt einen neuen FormDescriptor und wertet die
   * Formularbeschreibung/-Werte aus der DocumentInfo aus, falls sie vorhanden
   * sind. Danach k�nnen �ber add() weitere WM(CMD'Form')-Kommandos mit
   * Formularbeschreibungsnotizen hinzugef�gt werden.
   */
  public FormDescriptor(XTextDocument doc)
  {
    this.doc = doc;
    this.formularConf = new ConfigThingy("WM");
    this.formFieldValues = new HashMap();
    this.isEmpty = true;

    readDocInfoFormularbeschreibung();
    readDocInfoFormularwerte();
  }

  /**
   * Liest den Inhalt des WollMuxFormularbeschreibung-Feldes aus der
   * DocumentInfo des Dokuments und f�gt die Formularbeschreibung (falls eine
   * gefunden wurde) der Gesamtbeschreibung hinzu.
   */
  private void readDocInfoFormularbeschreibung()
  {
    String value = getDocInfoValue("WollMuxFormularbeschreibung");
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
   * Liest den Inhalt des WollMuxFormularwerte-Feldes aus der DocumentInfo des
   * Dokuments und �bertr�gt die gefundenen Werte (falls welche gefunden werden)
   * in die HashMap formFieldValues
   */
  private void readDocInfoFormularwerte()
  {
    String werteStr = getDocInfoValue("WollMuxFormularwerte");
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
   * Die Methode liest den Wert des Feldes fieldName aus der
   * DocumentInfo-Information des Dokuments oder gibt null zur�ck, wenn das Feld
   * nicht vorhanden ist.
   * 
   * @param fieldName
   *          Name des Feldes dessen Inhalt zur�ckgegeben werden soll.
   * @return Den Wert des Feldes fieldName oder null, wenn das Feld nicht
   *         vorhanden ist.
   */
  private String getDocInfoValue(String fieldName)
  {
    if (UNO.XDocumentInfoSupplier(doc) != null)
    {
      XDocumentInfo info = UNO.XDocumentInfoSupplier(doc).getDocumentInfo();
      for (short i = 0; i < info.getUserFieldCount(); ++i)
      {
        try
        {
          String name = info.getUserFieldName(i);
          if (name.equals(fieldName))
          {
            return info.getUserFieldValue(i);
          }
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
        }

      }
    }
    return null;
  }

  /**
   * Zeigt an, ob der FormDescriptor leer ist, oder ob mindestens ein g�ltiges
   * WM(CMD'Form')-Kommando mit add() hinzugef�gt wurde, das einen
   * Formular-Abschnitt enth�lt.
   */
  public boolean isEmpty()
  {
    return isEmpty;
  }

  /**
   * Die Methode f�gt die Formularbeschreibung, die unterhalb der Notiz des
   * WM(CMD'Form')-Kommandos gefunden wird zur Gesamtformularbeschreibung hinzu.
   * 
   * @param formCmd
   *          Das formCmd, das die Notzi mit der hinzuzuf�genden
   *          Formularbeschreibung enth�lt.
   * @throws ConfigurationErrorException
   *           Die Notiz der Formularbeschreibung ist nicht vorhanden, die
   *           Formularbeschreibung ist nicht vollst�ndig oder kann nicht
   *           geparst werden.
   */
  public void add(DocumentCommand.Form formCmd)
      throws ConfigurationErrorException
  {
    XTextRange range = formCmd.getTextRange();

    Object annotationField = findAnnotationFieldRecursive(range);
    if (annotationField == null)
      throw new ConfigurationErrorException(
          "Die Notiz mit der Formularbeschreibung fehlt.");

    Object content = UNO.getProperty(annotationField, "Content");
    if (content == null)
      throw new ConfigurationErrorException(
          "Die Notiz mit der Formularbeschreibung kann nicht gelesen werden.");

    ConfigThingy conf;
    try
    {
      conf = new ConfigThingy("", null, new StringReader(content.toString()));
    }
    catch (java.lang.Exception e)
    {
      throw new ConfigurationErrorException(
          "Die Formularbeschreibung innerhalb der Notiz ist fehlerhaft:\n"
              + e.getMessage());
    }

    addFormularSection(conf);
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
      isEmpty = false;
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
   * 
   * @return ConfigThingy-Repr�sentation mit dem Wurzelknoten "WM", die alle
   *         "Formular"-Abschnitte der Formularbeschreibungen enth�lt.
   */
  public ConfigThingy toConfigThingy()
  {
    return formularConf;
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
   * Diese Methode legt den aktuellen Werte aller Fomularfelder in einem Feld
   * WollMuxFormularwerte in der DocumentInfo des Dokuments ab. Ist kein
   * entsprechendes Feld in der DocumentInfo vorhanden, so wird es neu erzeugt.
   */
  public void updateDocument()
  {
    Logger.debug2(this.getClass().getSimpleName() + ".updateDocument()");

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

    // Formularwerte in die Dokumentinfo schreiben:
    if (UNO.XDocumentInfoSupplier(doc) != null)
    {
      short lastEmptyValue = -1;
      boolean written = false;

      // Das WollMuxFormularwerte-Feld in der bestehenden docinfo suchen und
      // verwenden.
      XDocumentInfo info = UNO.XDocumentInfoSupplier(doc).getDocumentInfo();
      for (short i = 0; i < info.getUserFieldCount(); ++i)
      {
        String name = "";
        String value = "";
        try
        {
          name = info.getUserFieldName(i);
          value = info.getUserFieldValue(i);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
        }

        if (name.equals("WollMuxFormularwerte"))
        {
          try
          {
            info.setUserFieldValue(i, werte.stringRepresentation());
            written = true;
            break;
          }
          catch (ArrayIndexOutOfBoundsException e)
          {
          }
        }
        else if (value.equals("")) lastEmptyValue = i;
      }

      // Falls kein entsprechendes Feld gefunden wurde, wird an der letzten
      // freien Stelle eines neu angelegt:
      if (!written && lastEmptyValue >= 0)
      {
        try
        {
          info.setUserFieldName(lastEmptyValue, "WollMuxFormularwerte");
          info.setUserFieldValue(lastEmptyValue, werte.stringRepresentation());
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
        }
      }
    }
  }

  /**
   * Diese Methode durchsucht das Element element bzw. dessen XEnumerationAccess
   * Interface rekursiv nach TextField.Annotation-Objekten und liefert das erste
   * gefundene TextField.Annotation-Objekt zur�ck, oder null, falls kein
   * entsprechendes Element gefunden wurde.
   * 
   * @param element
   *          Das erste gefundene InputField.
   */
  private static XTextField findAnnotationFieldRecursive(Object element)
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

}
