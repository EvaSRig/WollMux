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
import java.util.Vector;

import com.sun.star.awt.Size;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.drawing.XShape;
import com.sun.star.table.BorderLine;
import com.sun.star.text.HoriOrientation;
import com.sun.star.text.RelOrientation;
import com.sun.star.text.TextContentAnchorType;
import com.sun.star.text.VertOrientation;
import com.sun.star.text.WrapTextMode;
import com.sun.star.text.XText;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextFramesSupplier;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;

/**
 * TODO Doku anpassen an die �nderung dass jetzt nicht mehr die Infofelder verwendet werden. Am besten auch Methodennamen refaktorisieren
 * TODO Logger-Meldungen ebenfalls anpassen daran, dass nicht mehr Infofelder verwendet werden
 * Diese Klasse repr�sentiert eine Formularbeschreibung eines Formulardokuments,
 * die sich zusammensetzt aus dem Feld "WollMuxFormularbeschreibung" aus der
 * DocumentInfo des Dokuments und/oder aus einem oder mehrereren
 * WM(CMD'Form')-Kommandos mit den zugeh�rigen Notizfeldern, die die
 * Beschreibungstexte in ConfigThingy-Syntax enthalten. Beim Aufruf des
 * Konstruktors wird zun�chst die DocumentInfo des Dokuments ausgelesen und
 * evtl. dort enthaltene WollMuxFormularbeschreibungen �bernommen. Anschlie�end
 * k�nnen �ber die add()-Methode einzelne DocumentCommand.Form-Objekte
 * hinzugef�gt werden k�nnen. Logisch betrachtet werden alle Beschreibungstexte
 * zu einer gro�en ConfigThingy-Struktur zusammengef�gt und �ber die Methode
 * toConfigThingy() bereitgestellt.
 * 
 * Die Klasse bietet dar�ber hinaus Methoden zum Abspeichern und Auslesen der
 * original-Feldwerte im DocumentInfo Feld "WollMuxFormularwerte" an.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class FormDescriptor
{
 /**
  * Maximale L�nge von Textfeldern, die der WollMux schreibt. 
  * Die L�nge 32000 wurde gew�hlt, weil ich nicht sicher bin, ob die Grenze tats�chlich
  * 64kZeichen oder 64kBytes sind. In letzterem Fall k�nnten Zeichen, die 2 Bytes belegen
  * (eine interne UTF16 oder UTF8 kodierung angenommen) schon fr�her die Grenze treffen.
  * Also lieber auf der sicheren Seite sein und 32000. Eigentlich w�ren es ja
  * 32767, aber ich hab lieber den glatten Wert.
  */
  private static final int TEXTFIELD_MAXLEN = 32000;
  
  /**
   * Der Name der DocumentInfo-Benutzervariable, in der die WollMux-Formularbeschreibung
   * gespeichert wird.
   */
  private static final String WOLLMUX_FORMULARBESCHREIBUNG = "WollMuxFormularbeschreibung";

  /**
   * Der Name der DocumentInfo-Benutzervariable, in der die WollMux-Formularwerte
   * gespeichert werden.
   */
  private static final String WOLLMUX_FORMULARWERTE = "WollMuxFormularwerte";

  /**
   * Der Name des Frames in dem der WollMux seine Formulardaten speichert.
   */
  private static final String WOLLMUX_FRAME_NAME = "WollMuxDaten";
  
  /**
   * Das Dokument, das als Fabrik f�r neue Annotations ben�tigt wird.
   */
  private XTextDocument doc;

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
   * Zeigt an, ob der FormDescriptor leer ist, oder ob mindestens ein g�ltiger
   * Formulare-Abschnitt add() hinzugef�gt wurde, das einen Formular-Abschnitt
   * enth�lt.
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
    String value = getDocInfoValue(WOLLMUX_FORMULARBESCHREIBUNG);
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
   * Schreibt die Formularbeschreibung dieses FormDescriptors in das Infofeld
   * "WollMuxFormularbeschreibung" der DocumentInfo des Dokuments.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void writeDocInfoFormularbeschreibung()
  {
    writeDocInfo(WOLLMUX_FORMULARBESCHREIBUNG, toConfigThingy().stringRepresentation());
  }
  

  /**
   * Liest den Inhalt des WollMuxFormularwerte-Feldes aus der DocumentInfo des
   * Dokuments und �bertr�gt die gefundenen Werte (falls welche gefunden werden)
   * in die HashMap formFieldValues
   */
  private void readDocInfoFormularwerte()
  {
    String werteStr = getDocInfoValue(WOLLMUX_FORMULARWERTE);
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
    Vector textfields = getWollMuxTextFields(fieldName, false, 0);
    if (textfields.size() == 0) return null;
    Iterator iter = textfields.iterator();
    StringBuilder buffy = new StringBuilder();
    while (iter.hasNext())
    {
      buffy.append((String)UNO.getProperty(iter.next(), "Content"));
    }
    return buffy.toString();
  }
  
  /**
   * Liefert alle Informations-Textfelder mit Id fieldName zur�ck.
   * @param create falls true so werden entsprechende Felder angelegt, wenn sie 
   *        nicht existieren.
   * @size falls create == true werden soviele Felder angelegt, dass darin size Zeichen
   *       aufgeteilt in TEXTFIELD_MAXLEN lange Bl�cke untergebracht werden k�nnen.
   *       Eventuell vorhandene �bersch�ssige Felder werden gel�scht.
   *       Auch bei size == 0 wird mindestens ein Block geliefert.
   * @return leeren Vector falls das Feld nicht existiert und create == false oder 
   *         falls ein Fehler auftritt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private Vector getWollMuxTextFields(String fieldName, boolean create, int size)
  {
    Vector textfields = new Vector();
    XTextFramesSupplier supp = UNO.XTextFramesSupplier(doc);
    if (supp != null)
    {
      int blockCount = (size + (TEXTFIELD_MAXLEN-1)) / TEXTFIELD_MAXLEN;
      if (blockCount == 0) blockCount = 1;
      try{
        XNameAccess frameAccess = supp.getTextFrames();
        XShape frame;
        if (frameAccess.hasByName(WOLLMUX_FRAME_NAME))
          frame = UNO.XShape(frameAccess.getByName(WOLLMUX_FRAME_NAME));
        else
        {
          if (!create) return textfields;
          
          frame = UNO.XShape(UNO.XMultiServiceFactory(doc).createInstance("com.sun.star.text.TextFrame"));
          Size frameSize = new Size();
          frameSize.Height = 5;
          frameSize.Width = 5;
          frame.setSize(frameSize);
          UNO.setProperty(frame, "AnchorType", TextContentAnchorType.AT_PAGE);
          XText text = doc.getText();
          text.insertTextContent(text.getStart(), UNO.XTextContent(frame), false);
          
          UNO.setProperty(frame, "BackTransparent", Boolean.TRUE);
          UNO.setProperty(frame, "BorderDistance", new Integer(0));
          BorderLine line = new BorderLine(0, (short)0, (short)0, (short)0);
          UNO.setProperty(frame, "LeftBorder", line);
          UNO.setProperty(frame, "TopBorder", line);
          UNO.setProperty(frame, "BottomBorder", line);
          UNO.setProperty(frame, "RightBorder", line);
          UNO.setProperty(frame, "TextWrap", WrapTextMode.THROUGHT);
          UNO.setProperty(frame, "HoriOrient", new Short(HoriOrientation.NONE));
          UNO.setProperty(frame, "HoriOrientPosition", new Integer(0));
          UNO.setProperty(frame, "HoriOrientRelation", new Short(RelOrientation.PAGE_LEFT));
          UNO.setProperty(frame, "VertOrient", new Short(VertOrientation.BOTTOM));
          //UNO.setProperty(frame, "VertOrientPosition", new Integer(0));
          UNO.setProperty(frame, "VertOrientRelation", new Short(RelOrientation.PAGE_FRAME));
          UNO.setProperty(frame, "FrameIsAutomaticHeight", Boolean.FALSE);
          
          XNamed frameName = UNO.XNamed(frame);
          frameName.setName(WOLLMUX_FRAME_NAME);
        }
        
        
        XEnumeration paragraphEnu = UNO.XEnumerationAccess(frame).createEnumeration();
        while (paragraphEnu.hasMoreElements())
        {
          XEnumeration textportionEnu = UNO.XEnumerationAccess(paragraphEnu.nextElement()).createEnumeration();
          while (textportionEnu.hasMoreElements())
          {
            Object textfield = UNO.getProperty(textportionEnu.nextElement(), "TextField");
            String author = (String)UNO.getProperty(textfield, "Author");
            if (fieldName.equals(author))  //ACHTUNG! author.equals(fieldName) w�re falsch!
            {
              textfields.add(textfield);
            }
          }
        }
        
        /*
         * Falls create == true und zuviele Felder gefunden wurden, dann loesche die
         * �berz�hligen.
         */
        if (create && textfields.size() > blockCount)
        {
          XText frameText = UNO.XTextFrame(frame).getText();
          while (textfields.size() > blockCount)
          {
            Object textfield = textfields.remove(textfields.size()-1);
            frameText.removeTextContent(UNO.XTextContent(textfield));
          }
        }
        
        /*
         * Falls create == true und zu wenige Felder gefunden wurden, dann
         * erzeuge zus�tzliche.
         */
        if (create && textfields.size() < blockCount)
        {
          XText frameText = UNO.XTextFrame(frame).getText();
          while (textfields.size() < blockCount)
          {
            Object annotation = UNO.XMultiServiceFactory(doc).createInstance("com.sun.star.text.TextField.Annotation");
            frameText.insertTextContent(frameText.getEnd(), UNO.XTextContent(annotation), false);
            UNO.setProperty(annotation, "Author", fieldName);
            textfields.add(annotation);
          }
        }
        
      } catch(Exception x)
      {
        return textfields;
      }
    } //if (supp != null)
    return textfields;
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
   * ACHTUNG! Es wird eine Referenz auf ein internes Objekt geliefert! Nicht ver�ndern!
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
   * @param conf ein WM-Knoten, der "Formular"-Kinder hat.
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

    String infoFieldName = WOLLMUX_FORMULARWERTE;
    String infoFieldValue = werte.stringRepresentation();
    
    writeDocInfo(infoFieldName, infoFieldValue);
  }

  /**
   * Schreibt infoFieldValue in ein DocumentInfo-Benutzerfeld names infoFieldName. Ist ein
   * Feld dieses Namens bereits vorhanden, wird es �berschrieben. Ist kein Feld dieses Namens
   * wird das letzte freie Feld verwendet. Ist kein freies Feld vorhanden, gibt es eine
   * Log-Meldung und nichts wird geschrieben.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED */
  private void writeDocInfo(String infoFieldName, String infoFieldValue)
  {
    Vector textfields = getWollMuxTextFields(infoFieldName, true, infoFieldValue.length());
    if (textfields.size() == 0)
    {
      Logger.error("Konnte WollMux-Textfeld(er) \""+infoFieldName+"\" nicht anlegen");
      return;
    }
    
    Iterator iter = textfields.iterator();
    int start = 0;
    int len = infoFieldValue.length();
    while (iter.hasNext())
    {
      int blocksize = len - start;
      if (blocksize > TEXTFIELD_MAXLEN) blocksize = TEXTFIELD_MAXLEN; 
      String str = "";
      if (blocksize > 0)
      {
        str = infoFieldValue.substring(start, start + blocksize);
        start += blocksize;
      }
      
      UNO.setProperty(iter.next(), "Content", str);
    }
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
