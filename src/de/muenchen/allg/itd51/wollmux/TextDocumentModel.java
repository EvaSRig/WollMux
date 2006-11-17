/*
 * Dateiname: TextDocumentModel.java
 * Projekt  : WollMux
 * Funktion : Repr�sentiert ein aktuell ge�ffnetes TextDokument.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 15.09.2006 | LUT | Erstellung als TextDocumentModel
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import com.sun.star.awt.DeviceInfo;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XNameAccess;
import com.sun.star.frame.FrameSearchFlag;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseListener;
import com.sun.star.view.DocumentZoomType;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetPrintFunction;
import de.muenchen.allg.itd51.wollmux.dialog.FormGUI;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;

/**
 * Diese Klasse repr�sentiert das Modell eines aktuell ge�ffneten TextDokuments
 * und erm�glicht den Zugriff auf alle interessanten Aspekte des TextDokuments.
 * 
 * @author christoph.lutz
 * 
 */
public class TextDocumentModel
{
  /**
   * Enth�lt die Referenz auf das XTextDocument-interface des eigentlichen
   * TextDocument-Services der zugeh�rigen UNO-Komponente.
   */
  public final XTextDocument doc;

  /**
   * Ist true, wenn PrintSettings-Dialog mindestens einmal aufgerufen wurde und
   * false, wenn der Dialog noch nicht aufgerufen wurde.
   */
  public boolean printSettingsDone;

  /**
   * Die dataId unter der die WollMux-Formularbeschreibung in
   * {@link #persistentData} gespeichert wird.
   */
  private static final String DATA_ID_FORMULARBESCHREIBUNG = "WollMuxFormularbeschreibung";

  /**
   * Die dataId unter der die WollMux-Formularwerte in {@link #persistentData}
   * gespeichert werden.
   */
  private static final String DATA_ID_FORMULARWERTE = "WollMuxFormularwerte";

  /**
   * Die dataId unter der der Name der Druckfunktion in {@link #persistentData}
   * gespeichert wird.
   */
  private static final String DATA_ID_PRINTFUNCTION = "PrintFunction";

  /**
   * Die dataId unter der der Name der Druckfunktion in {@link #persistentData}
   * gespeichert wird.
   */
  private static final String DATA_ID_SETTYPE = "SetType";

  /**
   * Pattern zum Erkennen der Bookmarks, die {@link #deForm()} entfernen soll.
   */
  private static final Pattern BOOKMARK_KILL_PATTERN = Pattern
      .compile("(\\A\\s*(WM\\s*\\(.*CMD\\s*'((form)|(setGroups)|(insertFormValue))'.*\\))\\s*\\d*\\z)"
               + "|(\\A\\s*(WM\\s*\\(.*CMD\\s*'(setType)'.*'formDocument'\\))\\s*\\d*\\z)"
               + "|(\\A\\s*(WM\\s*\\(.*'formDocument'.*CMD\\s*'(setType)'.*\\))\\s*\\d*\\z)");

  /**
   * Erm�glicht den Zugriff auf einen Vector aller FormField-Objekte in diesem
   * TextDokument �ber den Namen der zugeordneten ID.
   */
  private HashMap idToFormFields;

  /**
   * Falls es sich bei dem Dokument um ein Formular handelt, wird das zugeh�rige
   * FormModel hier gespeichert und beim dispose() des TextDocumentModels mit
   * geschlossen.
   */
  private FormModel formModel;

  /**
   * TODO: dokumentieren... Falls es sich bei dem Dokument um ein Formular
   * handelt, wird das zugeh�rige FormModel hier gespeichert und beim dispose()
   * des TextDocumentModels mit geschlossen.
   */
  private FormGUI formGUI;

  /**
   * In diesem Feld wird der CloseListener gespeichert, nachdem die Methode
   * registerCloseListener() aufgerufen wurde.
   */
  private XCloseListener closeListener;

  /**
   * Enth�lt die Instanz des aktuell ge�ffneten, zu diesem Dokument geh�renden
   * FormularMax4000.
   */
  private FormularMax4000 currentMax4000;

  /**
   * Dieses Feld stellt ein Zwischenspeicher f�r Fragment-Urls dar. Es wird dazu
   * benutzt, im Fall eines openTemplate-Befehls die urls der �bergebenen
   * frag_id-Liste tempor�r zu speichern.
   */
  private String[] fragUrls;

  /**
   * Enth�lt den Typ des Dokuments oder null, falls keiner gesetzt ist.
   */
  private String type = null;

  /**
   * Enth�lt den Namen der aktuell gesetzten Druckfunktion oder null, wenn keine
   * Druckfunktion gesetzt ist.
   */
  private String printFunctionName;

  /**
   * Enth�lt den Namen der Druckfunktion, die vor dem letzten Aufruf der Methode
   * setPrintFunction(...) gesetzt war oder null wenn noch keine Druckfunktion
   * gesetzt war.
   */
  private String formerPrintFunctionName;

  /**
   * Enth�lt das zu diesem TextDocumentModel zugeh�rige XPrintModel.
   */
  private XPrintModel printModel = new PrintModel();

  /**
   * Baum aller im Dokument enthaltenen Dokumentkommandos.
   */
  private DocumentCommandTree docCmdTree;

  /**
   * Enth�lt die Gesamtbeschreibung alle Formular-Abschnitte, die in den
   * persistenten Daten bzw. in den mit add hinzugef�gten Form-Kommandos
   * gefunden wurden.
   */
  private ConfigThingy formularConf;

  /**
   * Enth�lt die aktuellen Werte der Formularfelder als Zuordnung id -> Wert.
   */
  private HashMap formFieldValues;

  /**
   * Verantwortlich f�r das Speichern persistenter Daten.
   */
  private PersistentData persistentData;

  /**
   * Enth�lt einen Vector aller notInOrininal-Dokumentkommandos des Dokuments,
   * die f�r die Ein/Ausblendungen in Sachleitenden Verf�gungen ben�tigt werden.
   */
  private Vector notInOriginalBlocks;

  /**
   * Enth�lt einen Vector aller draftOnly-Dokumentkommandos des Dokuments, die
   * f�r die Ein/Ausblendungen in Sachleitenden Verf�gungen ben�tigt werden.
   */
  private Vector draftOnlyBlocks;

  /**
   * Enth�lt einen Vector aller all-Dokumentkommandos des Dokuments, die f�r die
   * Ein/Ausblendungen in Sachleitenden Verf�gungen ben�tigt werden.
   */
  private Vector allVersionsBlocks;

  /**
   * �ber die Methode registerWollMuxDispatchInterceptor() wird hier der aktuell
   * auf dem Frame registrierte WollMuxDispatchInterceptor abgelegt, der f�r das
   * Abfangen von Dispatches wie z.B. dem .uno:Print erforderlich ist.
   */
  private DispatchInterceptor dispatchInterceptorController;

  /**
   * Erzeugt ein neues TextDocumentModel zum XTextDocument doc und sollte nie
   * direkt aufgerufen werden, da neue TextDocumentModels �ber das
   * WollMuxSingletonie (siehe WollMuxSingleton.getTextDocumentModel()) erzeugt
   * und verwaltet werden.
   * 
   * @param doc
   */
  public TextDocumentModel(XTextDocument doc)
  {
    this.doc = doc;
    this.idToFormFields = new HashMap();
    this.fragUrls = new String[] {};
    this.currentMax4000 = null;
    this.closeListener = null;
    this.printFunctionName = null;
    this.docCmdTree = new DocumentCommandTree(UNO.XBookmarksSupplier(doc));
    this.dispatchInterceptorController = null;
    this.printSettingsDone = false;
    this.formularConf = new ConfigThingy("WM");
    this.formFieldValues = new HashMap();
    this.formGUI = null;

    resetPrintBlocks();

    registerCloseListener();

    // WollMuxDispatchInterceptor registrieren
    try
    {
      dispatchInterceptorController = new DispatchInterceptor(UNO.XModel(doc)
          .getCurrentController().getFrame());
      dispatchInterceptorController.registerWollMuxDispatchInterceptor();
    }
    catch (java.lang.Exception e)
    {
      Logger.error("Kann DispatchInterceptor nicht registrieren:", e);
    }

    // Auslesen der Persistenten Daten:
    this.persistentData = new PersistentData(doc);
    this.type = persistentData.getData(DATA_ID_SETTYPE);
    this.printFunctionName = persistentData.getData(DATA_ID_PRINTFUNCTION);
    parseFormDescription(persistentData.getData(DATA_ID_FORMULARBESCHREIBUNG));
    parseFormValues(persistentData.getData(DATA_ID_FORMULARWERTE));
  }

  /**
   * Veranlasst das TextDocumentModel alle bisher erkannten Bl�cke zur
   * Drucksteuerung bei Sachleitenden Verf�gungen (notInOriginal, DraftOnly,
   * All) zu vergessen. Danach k�nnen die Bl�cke mit add<Blockname>Blocks(...)
   * neu hinzugef�gt werden.
   */
  public void resetPrintBlocks()
  {
    this.notInOriginalBlocks = new Vector();
    this.draftOnlyBlocks = new Vector();
    this.allVersionsBlocks = new Vector();
  }

  /**
   * Wertet den String-Inhalt value der Formularbeschreibungsnotiz aus, die von
   * der Form "WM( Formular(...) )" sein muss und f�gt diese der
   * Gesamtbeschreibung hinzu.
   * 
   * @param value
   *          darf null sein und wird in diesem Fall ignoriert, darf aber kein
   *          leerer String sein, sonst Fehler.
   */
  private void parseFormDescription(String value)
  {
    if (value == null) return;

    try
    {
      ConfigThingy conf = new ConfigThingy("", null, new StringReader(value));
      // Formular-Abschnitt auswerten:
      try
      {
        ConfigThingy formular = conf.get("WM").get("Formular");
        formularConf.addChild(formular);
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(new ConfigurationErrorException(
            "Die Formularbeschreibung enth�lt keinen Abschnitt 'Formular':\n"
                + e.getMessage()));
      }
    }
    catch (java.lang.Exception e)
    {
      Logger.error(new ConfigurationErrorException(
          "Formularbeschreibung ist fehlerhaft:\n" + e.getMessage()));
      return;
    }
  }

  /**
   * Wertet werteStr aus (das von der Form "WM(FormularWerte(...))" sein muss
   * und �bertr�gt die gefundenen Werte nach formFieldValues.
   * 
   * @param werteStr
   *          darf null sein (und wird dann ignoriert) aber nicht der leere
   *          String.
   */
  private void parseFormValues(String werteStr)
  {
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
      Logger.error(new ConfigurationErrorException(
          "Formularwerte-Abschnitt ist fehlerhaft:\n" + e.getMessage()));
      return;
    }

    // "Formularwerte"-Abschnitt auswerten.
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
   * Wird derzeit vom DocumentCommandInterpreter aufgerufen und gibt dem
   * TextDocumentModel alle FormFields bekannt, die beim Durchlauf des
   * FormScanners gefunden wurden.
   * 
   * @param idToFormFields
   */
  public void setIDToFormFields(HashMap idToFormFields)
  {
    this.idToFormFields = idToFormFields;
  }

  /**
   * Erm�glicht den Zugriff auf einen Vector aller FormField-Objekte in diesem
   * TextDokument �ber den Namen der zugeordneten ID.
   * 
   * @return Eine HashMap, die unter dem Schl�ssel ID einen Vector mit den
   *         entsprechenden FormFields bereith�lt.
   */
  public HashMap getIDToFormFields()
  {
    return idToFormFields;
  }

  /**
   * Speichert das FormModel formModel im TextDocumentModel und wird derzeit vom
   * DocumentCommandInterpreter aufgerufen, wenn er ein FormModel erzeugt.
   * 
   * @param formModel
   */
  public void setFormModel(FormModel formModel)
  {
    this.formModel = formModel;
  }

  /**
   * Liefert das zuletzt per setFormModel() gesetzte FormModel zur�ck.
   * 
   * @return
   */
  public FormModel getFormModel()
  {
    return this.formModel;
  }

  /**
   * Der DocumentCommandInterpreter liest sich die Liste der setFragUrls()
   * gespeicherten Fragment-URLs hier aus, wenn die Dokumentkommandos
   * insertContent ausgef�hrt werden.
   * 
   * @return
   */
  public String[] getFragUrls()
  {
    return fragUrls;
  }

  /**
   * �ber diese Methode kann der openDocument-Eventhandler die Liste der mit
   * einem insertContent-Kommando zu �ffnenden frag-urls speichern.
   */
  public void setFragUrls(String[] fragUrls)
  {
    this.fragUrls = fragUrls;
  }

  /**
   * Setzt die Instanz des aktuell ge�ffneten, zu diesem Dokument geh�renden
   * FormularMax4000.
   * 
   * @param max
   */
  public void setCurrentFormularMax4000(FormularMax4000 max)
  {
    currentMax4000 = max;
  }

  /**
   * Liefert die Instanz des aktuell ge�ffneten, zu diesem Dokument geh�renden
   * FormularMax4000 zur�ck, oder null, falls kein FormularMax gestartet wurde.
   * 
   * @return
   */
  public FormularMax4000 getCurrentFormularMax4000()
  {
    return currentMax4000;
  }

  /**
   * Liefert den Baum der Dokumentkommandos zu diesem Dokuments in einem nicht
   * zwangsweise aktualisierten Zustand. Der Zustand kann �ber die
   * update()-Methode des zur�ckgegebenen DocumentCommandTrees aktualisiert
   * werden.
   * 
   * @return Baum der Dokumentkommandos dieses Dokuments.
   */
  public DocumentCommandTree getDocumentCommandTree()
  {
    return docCmdTree;
  }

  /**
   * Liefert true, wenn das Dokument eine Vorlage ist oder wie eine Vorlage
   * behandelt werden soll, ansonsten false.
   * 
   * @return true, wenn das Dokument eine Vorlage ist oder wie eine Vorlage
   *         behandelt werden soll, ansonsten false.
   */
  public boolean isTemplate()
  {
    if (type != null)
    {
      if (type.equalsIgnoreCase("normalTemplate"))
        return true;
      else if (type.equalsIgnoreCase("templateTemplate"))
        return false;
      else if (type.equalsIgnoreCase("formDocument")) return false;
    }

    // Das Dokument ist automatisch eine Vorlage, wenn es keine zugeh�rige URL
    // gibt (dann steht ja in der Fenster�berschrift auch "Unbenannt1" statt
    // einem konkreten Dokumentnamen).
    return !hasURL();
  }

  /**
   * liefert true, wenn das Dokument eine URL besitzt, die die Quelle des
   * Dokuments beschreibt und es sich damit um ein in OOo im "Bearbeiten"-Modus
   * ge�ffnetes Dokument handelt oder false, wenn das Dokument keine URL besitzt
   * und es sich damit um eine Vorlage handelt.
   * 
   * @return liefert true, wenn das Dokument eine URL besitzt, die die Quelle
   *         des Dokuments beschreibt und es sich damit um ein in OOo im
   *         "Bearbeiten"-Modus ge�ffnetes Dokument handelt oder false, wenn das
   *         Dokument keine URL besitzt und es sich damit um eine Vorlage
   *         handelt.
   */
  public boolean hasURL()
  {
    return doc.getURL() != null && !doc.getURL().equals("");
  }

  /**
   * Liefert true, wenn das Dokument vom Typ formDocument ist ansonsten false.
   * ACHTUNG: Ein Dokument k�nnte theoretisch mit einem WM(CMD'setType'
   * TYPE'formDocument') Kommandos als Formulardokument markiert seine, OHNE
   * eine g�ltige Formularbeschreibung zu besitzen. Dies kann mit der Methode
   * hasFormDescriptor() gepr�ft werden.
   * 
   * @return Liefert true, wenn das Dokument vom Typ formDocument ist ansonsten
   *         false.
   */
  public boolean isFormDocument()
  {
    return (type != null && type.equalsIgnoreCase("formDocument"));
  }

  /**
   * Liefert true, wenn das Dokument ein Formular mit einer g�ltigen
   * Formularbeschreibung enth�lt und damit die Dokumentkommandos des
   * Formularsystems bearbeitet werden sollen.
   * 
   * @return true, wenn das Dokument ein Formular mit einer g�ltigen
   *         Formularbeschreibung ist, ansonsten false.
   */
  public boolean hasFormDescriptor()
  {
    return !(formularConf.count() == 0);
  }

  /**
   * Setzt den Typ des Dokuments auf type und speichert den Wert persistent im
   * Dokument ab.
   */
  public void setType(String type)
  {
    this.type = type;

    // Persistente Daten entsprechend anpassen
    if (type != null)
    {
      persistentData.setData(DATA_ID_SETTYPE, type);
    }
    else
    {
      persistentData.removeData(DATA_ID_SETTYPE);
    }
  }

  /**
   * Wird vom {@link DocumentCommandInterpreter} beim Scannen der
   * Dokumentkommandos aufgerufen wenn ein setType-Dokumentkommando bearbeitet
   * werden muss und setzt den Typ des Dokuments NICHT PERSISTENT auf
   * cmd.getType(), wenn nicht bereits ein type gesetzt ist. Ansonsten wird das
   * Kommando ignoriert.
   */
  public void setType(DocumentCommand.SetType cmd)
  {
    if (type == null) this.type = cmd.getType();
  }

  /**
   * Diese Methode setzt die diesem TextDocumentModel zugeh�rige Druckfunktion
   * auf den Wert functionName, der ein g�ltiger Funktionsbezeichner sein muss
   * oder setzt eine Druckfunktion auf den davor zuletzt gesetzten Wert zur�ck,
   * wenn functionName der Leerstring ist und fr�her bereits eine Druckfunktion
   * registriert war. War fr�her noch keine Druckfunktion registriert, so wird
   * das entsprechende setPrintFunction-Dokumentkommando aus dem Dokument
   * gel�scht.
   * 
   * @param newFunctionName
   *          der Name der Druckfunktion (zum setzen), der Leerstring "" (zum
   *          zur�cksetzen auf die zuletzt gesetzte Druckfunktion) oder
   *          "default" zum L�schen der Druckfunktion. Der zu setzende Name muss
   *          ein g�ltiger Funktionsbezeichner sein und in einem Abschnitt
   *          "Druckfunktionen" in der wollmux.conf definiert sein.
   */
  public void setPrintFunction(String newFunctionName)
  {
    // nichts machen, wenn der Name bereits gesetzt ist.
    if (printFunctionName != null && printFunctionName.equals(newFunctionName))
      return;

    // Bei null oder Leerstring: Name der vorhergehenden Druckfunktion
    // verwenden.
    if (newFunctionName == null || newFunctionName.equals(""))
      newFunctionName = formerPrintFunctionName;
    else if (newFunctionName != null
             && newFunctionName.equalsIgnoreCase("default"))
      newFunctionName = null;

    // Neuen Funktionsnamen setzen und alten merken
    formerPrintFunctionName = printFunctionName;
    printFunctionName = newFunctionName;

    // Persistente Daten entsprechend anpassen
    if (printFunctionName != null)
    {
      persistentData.setData(DATA_ID_PRINTFUNCTION, printFunctionName);
    }
    else
    {
      persistentData.removeData(DATA_ID_PRINTFUNCTION);
    }
  }

  /**
   * Wird vom DocumentCommandInterpreter beim parsen des Dokumentkommandobaumes
   * aufgerufen, wenn das Dokument ein setPrintFunction-Kommando enth�lt und
   * setzt die in cmd.getFunctionName() enthaltene Druckfunktion PERSISTENT im
   * Dokument, falls nicht bereits eine Druckfunktion definiert ist. Ansonsten
   * wird das Dokumentkommando ignoriert.
   * 
   * @param cmd
   *          Das gefundene setPrintFunction-Dokumentkommando.
   */
  public void setPrintFunction(SetPrintFunction cmd)
  {
    if (printFunctionName == null) setPrintFunction(cmd.getFunctionName());
  }

  /**
   * Liefert den Namen der aktuellen Druckfunktion zur�ck, oder null, wenn keine
   * Druckfunktion definiert ist.
   */
  public String getPrintFunctionName()
  {
    return printFunctionName;
  }

  /**
   * F�gt der Liste der NotInOriginal-Kommandos dieses Dokuments ein weiteres
   * Dokumentkommando cmd dieses Typs hinzu.
   * 
   * @param cmd
   *          das hinzuzuf�gende Dokumentkommando
   */
  public void addNotInOriginalBlock(DocumentCommand.NotInOriginal cmd)
  {
    notInOriginalBlocks.add(cmd);
  }

  /**
   * F�gt der Liste der DraftOnly-Kommandos dieses Dokuments ein weiteres
   * Dokumentkommando cmd dieses Typs hinzu.
   * 
   * @param cmd
   *          das hinzuzuf�gende Dokumentkommando
   */
  public void addDraftOnlyBlock(DocumentCommand.DraftOnly cmd)
  {
    draftOnlyBlocks.add(cmd);
  }

  /**
   * F�gt der Liste der DraftOnly-Kommandos dieses Dokuments ein weiteres
   * Dokumentkommando cmd dieses Typs hinzu.
   * 
   * @param cmd
   *          das hinzuzuf�gende Dokumentkommando
   */
  public void addAllVersionsBlock(DocumentCommand.AllVersions cmd)
  {
    allVersionsBlocks.add(cmd);
  }

  /**
   * Liefert einen Iterator zur�ck, der die Iteration aller
   * NotInOrininal-Dokumentkommandos dieses Dokuments erm�glicht.
   * 
   * @return ein Iterator, der die Iteration aller
   *         NotInOrininal-Dokumentkommandos dieses Dokuments erm�glicht. Der
   *         Iterator kann auch keine Elemente enthalten.
   */
  public Iterator getNotInOrininalBlocksIterator()
  {
    return notInOriginalBlocks.iterator();
  }

  /**
   * Liefert einen Iterator zur�ck, der die Iteration aller
   * DraftOnly-Dokumentkommandos dieses Dokuments erm�glicht.
   * 
   * @return ein Iterator, der die Iteration aller DraftOnly-Dokumentkommandos
   *         dieses Dokuments erm�glicht. Der Iterator kann auch keine Elemente
   *         enthalten.
   */
  public Iterator getDraftOnlyBlocksIterator()
  {
    return draftOnlyBlocks.iterator();
  }

  /**
   * Liefert einen Iterator zur�ck, der die Iteration aller
   * All-Dokumentkommandos dieses Dokuments erm�glicht.
   * 
   * @return ein Iterator, der die Iteration aller All-Dokumentkommandos dieses
   *         Dokuments erm�glicht. Der Iterator kann auch keine Elemente
   *         enthalten.
   */
  public Iterator getAllVersionsBlocksIterator()
  {
    return allVersionsBlocks.iterator();
  }

  /**
   * Liefert den ViewCursor des aktuellen Dokuments oder null, wenn kein
   * Controller (oder auch kein ViewCursor) f�r das Dokument verf�gbar ist.
   * 
   * @return Liefert den ViewCursor des aktuellen Dokuments oder null, wenn kein
   *         Controller (oder auch kein ViewCursor) f�r das Dokument verf�gbar
   *         ist.
   */
  public XTextCursor getViewCursor()
  {
    if (UNO.XModel(doc) == null) return null;
    XTextViewCursorSupplier suppl = UNO.XTextViewCursorSupplier(UNO.XModel(doc)
        .getCurrentController());
    if (suppl != null) return suppl.getViewCursor();
    return null;
  }

  /**
   * Entfernt die WollMux-Kommandos "insertFormValue", "setGroups", "setType
   * formDocument" und "form", sowie die WollMux-Formularbeschreibung und Daten
   * aus dem Dokument doc.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void deForm()
  {
    XBookmarksSupplier bmSupp = UNO.XBookmarksSupplier(doc);
    XNameAccess bookmarks = bmSupp.getBookmarks();
    String[] names = bookmarks.getElementNames();
    for (int i = 0; i < names.length; ++i)
    {
      try
      {
        String bookmark = names[i];
        if (BOOKMARK_KILL_PATTERN.matcher(bookmark).matches())
        {
          XTextContent bm = UNO.XTextContent(bookmarks.getByName(bookmark));
          bm.getAnchor().getText().removeTextContent(bm);
        }

      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }

    persistentData.removeData(DATA_ID_FORMULARBESCHREIBUNG);
    persistentData.removeData(DATA_ID_FORMULARWERTE);
  }

  /**
   * Liefert die aktuelle Formularbeschreibung.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy getFormDescription()
  {
    return formularConf;
  }

  /**
   * Ersetzt die Formularbeschreibung dieses Dokuments durch die aus conf.
   * ACHTUNG! conf wird nicht kopiert sondern als Referenz eingebunden.
   * 
   * @param conf
   *          ein WM-Knoten, der "Formular"-Kinder hat.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setFormDescription(ConfigThingy conf)
  {
    formularConf = conf;
    persistentData.setData(DATA_ID_FORMULARBESCHREIBUNG, formularConf
        .stringRepresentation());
    setDocumentModified(true);
  }

  /**
   * Setzt den Wert des WollMuxFormularfeldes fieldId auf value.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setFormFieldValue(String fieldId, String value)
  {
    formFieldValues.put(fieldId, value);
    persistentData.setData(DATA_ID_FORMULARWERTE, getFormFieldValues());
  }

  /**
   * Serialisiert die aktuellen Werte aller Fomularfelder.
   */
  private String getFormFieldValues()
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

    return werte.stringRepresentation();
  }

  /**
   * Liefert den Wert des WollMuxFormularfeldes mit der ID fieldId.
   */
  public String getFormFieldValue(String fieldId)
  {
    return (String) formFieldValues.get(fieldId);
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
   * TODO: dokumentieren getFormGUI
   * 
   * @return
   */
  public FormGUI getFormGUI()
  {
    return formGUI;
  }

  /**
   * TODO: dokumentieren setFormGUI
   * 
   * @param formGUI
   */
  public void setFormGUI(FormGUI formGUI)
  {
    this.formGUI = formGUI;
  }

  /**
   * Setzt das Fensters des TextDokuments auf Sichtbar (visible==true) oder
   * unsichtbar (visible == false).
   * 
   * @param visible
   */
  public void setWindowVisible(boolean visible)
  {
    XModel xModel = UNO.XModel(doc);
    if (xModel != null)
    {
      XFrame frame = xModel.getCurrentController().getFrame();
      if (frame != null)
      {
        frame.getContainerWindow().setVisible(visible);
      }
    }
  }

  /**
   * Diese Methode setzt den DocumentModified-Status auf state.
   * 
   * @param state
   */
  public void setDocumentModified(boolean state)
  {
    try
    {
      UNO.XModifiable(doc).setModified(state);
    }
    catch (java.lang.Exception x)
    {
    }
  }

  /**
   * Diese Methode blockt/unblocked die Contoller, die f�r das Rendering der
   * Darstellung in den Dokumenten zust�ndig sind, jedoch nur, wenn nicht der
   * debug-modus gesetzt ist.
   * 
   * @param state
   */
  public void setLockControllers(boolean lock)
  {
    try
    {
      if (WollMuxSingleton.getInstance().isDebugMode() == false
          && UNO.XModel(doc) != null)
      {
        if (lock)
          UNO.XModel(doc).lockControllers();
        else
          UNO.XModel(doc).unlockControllers();
      }
    }
    catch (java.lang.Exception e)
    {
    }
  }

  /**
   * Setzt die Position des Fensters auf die �bergebenen Koordinaten, wobei die
   * Nachteile der UNO-Methode setWindowPosSize greifen, bei der die
   * Fensterposition nicht mit dem �usseren Fensterrahmen beginnt, sondern mit
   * der grauen Ecke links �ber dem File-Men�.
   * 
   * @param docX
   * @param docY
   * @param docWidth
   * @param docHeight
   */
  public void setWindowPosSize(int docX, int docY, int docWidth, int docHeight)
  {
    try
    {
      UNO.XModel(doc).getCurrentController().getFrame().getContainerWindow()
          .setPosSize(docX, docY, docWidth, docHeight, PosSize.POSSIZE);
    }
    catch (java.lang.Exception e)
    {
    }
  }

  /**
   * Diese Methode setzt den ZoomTyp bzw. den ZoomValue der Dokumentenansicht
   * des Dokuments auf den neuen Wert zoom, der entwender eine ganzzahliger
   * Prozentwert (ohne "%"-Zeichen") oder einer der Werte "Optimal",
   * "PageWidth", "PageWidthExact" oder "EntirePage" ist.
   * 
   * @param zoom
   * @throws ConfigurationErrorException
   */
  private void setDocumentZoom(String zoom) throws ConfigurationErrorException
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

  /**
   * Diese Methode liest die (optionalen) Attribute X, Y, WIDTH, HEIGHT und ZOOM
   * aus dem �bergebenen Konfigurations-Abschnitt settings und setzt die
   * Fenstereinstellungen des Dokuments entsprechend um. Bei den P�rchen X/Y
   * bzw. SIZE/WIDTH m�ssen jeweils beide Komponenten im Konfigurationsabschnitt
   * angegeben sein.
   * 
   * @param settings
   *          der Konfigurationsabschnitt, der X, Y, WIDHT, HEIGHT und ZOOM als
   *          direkte Kinder enth�lt.
   */
  public void setWindowViewSettings(ConfigThingy settings)
  {
    // Fenster holen (zum setzen der Fensterposition und des Zooms)
    XWindow window = null;
    try
    {
      window = UNO.XModel(doc).getCurrentController().getFrame()
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
        window.setPosSize(xPos + insetLeft, yPos + insetTop, 0, 0, PosSize.POS);
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
    setDocumentZoom(settings);
  }

  /**
   * Diese Methode setzt den ZoomTyp bzw. den ZoomValue der Dokumentenansicht
   * des Dokuments auf den neuen Wert den das ConfigThingy conf im Knoten ZOOM
   * angibt, der entwender eine ganzzahliger Prozentwert (ohne "%"-Zeichen")
   * oder einer der Werte "Optimal", "PageWidth", "PageWidthExact" oder
   * "EntirePage" ist.
   * 
   * @param zoom
   * @throws ConfigurationErrorException
   */
  public void setDocumentZoom(ConfigThingy conf)
  {
    try
    {
      setDocumentZoom(conf.get("ZOOM").toString());
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
  public void addFormCommand(DocumentCommand.Form formCmd)
      throws ConfigurationErrorException
  {
    XTextRange range = formCmd.getTextRange();

    Object annotationField = WollMuxSingleton
        .findAnnotationFieldRecursive(range);
    if (annotationField == null)
      throw new ConfigurationErrorException(
          "Die Notiz mit der Formularbeschreibung fehlt.");

    Object content = UNO.getProperty(annotationField, "Content");
    if (content == null)
      throw new ConfigurationErrorException(
          "Die Notiz mit der Formularbeschreibung kann nicht gelesen werden.");

    parseFormDescription(content.toString());
  }

  /**
   * Druckt das Dokument mit der Anzahl von Ausfertigungen numberOfCopies auf
   * dem aktuell eingestellten Drucker aus.
   * 
   * @param numberOfCopies
   * @throws PrintFailedException
   */
  public void print(short numberOfCopies) throws PrintFailedException
  {
    if (UNO.XPrintable(doc) != null)
    {
      PropertyValue[] args = new PropertyValue[] {
                                                  new PropertyValue(),
                                                  new PropertyValue() };
      args[0].Name = "CopyCount";
      args[0].Value = new Short(numberOfCopies);
      args[1].Name = "Wait";
      args[1].Value = Boolean.TRUE;

      try
      {
        UNO.XPrintable(doc).print(args);
      }
      catch (java.lang.Exception e)
      {
        throw new PrintFailedException(e);
      }
    }
  }

  /**
   * Das Drucken des Dokuments hat aus irgend einem Grund nicht funktioniert.
   * 
   * @author christoph.lutz
   */
  public static class PrintFailedException extends Exception
  {
    private static final long serialVersionUID = 1L;

    PrintFailedException(Exception e)
    {
      super("Das Drucken des Dokuments schlug fehl: ", e);
    }
  }

  /**
   * Schliesst das XTextDocument, das diesem Model zugeordnet ist. Ist der
   * closeListener registriert (was WollMuxSingleton bereits bei der Erstellung
   * des TextDocumentModels standardm�ig macht), so wird nach dem close() auch
   * automatisch die dispose()-Methode aufgerufen.
   */
  public void close()
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
  }

  /**
   * Ruft die Dispose-Methoden von allen aktiven, dem TextDocumentModel
   * zugeordneten Dialogen auf und gibt den Speicher des TextDocumentModels
   * frei.
   */
  public void dispose()
  {
    if (currentMax4000 != null) currentMax4000.dispose();
    currentMax4000 = null;

    if (formModel != null) formModel.dispose();
    formModel = null;

    // L�scht das TextDocumentModel von doc aus dem WollMux-Singleton.
    WollMuxSingleton.getInstance().disposedTextDocument(doc);
  }

  /**
   * Registriert genau einen XCloseListener in der Komponente des
   * XTextDocuments, so dass beim Schlie�en des Dokuments die entsprechenden
   * WollMuxEvents ausgef�hrt werden - ist in diesem TextDocumentModel bereits
   * ein XCloseListener registriert, so wird nichts getan.
   */
  private void registerCloseListener()
  {
    if (closeListener == null && UNO.XCloseable(doc) != null)
    {
      closeListener = new XCloseListener()
      {
        public void disposing(EventObject arg0)
        {
          WollMuxEventHandler.handleTextDocumentClosed(doc);
        }

        public void notifyClosing(EventObject arg0)
        {
          WollMuxEventHandler.handleTextDocumentClosed(doc);
        }

        public void queryClosing(EventObject arg0, boolean arg1)
            throws CloseVetoException
        {
        }
      };
      UNO.XCloseable(doc).addCloseListener(closeListener);
    }
  }

  /**
   * Liefert das zu diesem TextDocumentModel zugeh�rige XPrintModel.
   */
  public XPrintModel getPrintModel()
  {
    return printModel;
  }

  /**
   * Das XPrintModel ist Bestandteil der Komfortdruckfunktionen, wobei jede
   * Druckfunktion ein XPrintModel �bergeben bekommt, das das Drucken aus der
   * Komfortdruckfunktion heraus erleichtern soll. Da die einzelnen
   * Druckfunktionen in eigenen Threads laufen, muss jede Druckfunktion sicher
   * stellen, dass die zu erledigenden Aktionen mit dem
   * WollMuxEventHandler-Thread synchronisiert werden. Dies geschieht �ber einen
   * lock-wait-callback-Mechanismus. Vor dem Einstellen des Action-Ereignisses
   * in den WollMuxEventHandler wird ein lock gesetzt. Nach dem Einstellen des
   * Ereignisses wird so lange gewartet, bis der WollMuxEventHandler die
   * �bergebene Callback-Methode aufruft.
   * 
   * @author christoph.lutz
   */
  public class PrintModel implements XPrintModel, XServiceInfo
  {
    /**
     * Dieses Feld ent�lt eine Liste aller Services, die dieser UNO-Service
     * implementiert.
     */
    private final java.lang.String[] SERVICENAMES = { "de.muenchen.allg.itd51.wollmux.PrintModel" };

    /**
     * Das lock-Flag, das vor dem Einstellen eines WollMuxEvents auf true
     * gesetzt werden muss und signalisiert, ob das WollMuxEvent erfolgreich
     * abgearbeitet wurde.
     */
    private boolean[] lock = new boolean[] { true };

    /**
     * Liefert das XTextDocument mit dem die Druckfunktion aufgerufen wurde.
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#getTextDocument()
     */
    public XTextDocument getTextDocument()
    {
      return doc;
    }

    /**
     * Druckt das TextDocument mit numberOfCopies Ausfertigungen auf dem aktuell
     * eingestellten Drucker aus.
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#print(short)
     */
    public void print(short numberOfCopies)
    {
      setLock();
      WollMuxEventHandler
          .handlePrint(doc, numberOfCopies, unlockActionListener);
      waitForUnlock();
    }

    /**
     * Falls das TextDocument Sachleitende Verf�gungen enth�lt, ist es mit
     * dieser Methode m�glich, den Verf�gungspunkt mit der Nummer verfPunkt
     * auszudrucken, wobei alle darauffolgenden Verf�gungspunkte ausgeblendet
     * werden.
     * 
     * @param verfPunkt
     *          Die Nummer des auszuduruckenden Verf�gungspunktes, wobei alle
     *          folgenden Verf�gungspunkte ausgeblendet werden.
     * @param numberOfCopies
     *          Die Anzahl der Ausfertigungen, in der verfPunkt ausgedruckt
     *          werden soll.
     * @param isDraft
     *          wenn isDraft==true, werden alle draftOnly-Bl�cke eingeblendet,
     *          ansonsten werden sie ausgeblendet.
     * @param isOriginal
     *          wenn isOriginal, wird die Ziffer des Verf�gungspunktes I
     *          ausgeblendet und alle notInOriginal-Bl�cke ebenso. Andernfalls
     *          sind Ziffer und notInOriginal-Bl�cke eingeblendet.
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#printVerfuegungspunkt(short,
     *      short, boolean, boolean)
     */
    public void printVerfuegungspunkt(short verfPunkt, short numberOfCopies,
        boolean isDraft, boolean isOriginal)
    {
      setLock();
      WollMuxEventHandler.handlePrintVerfuegungspunkt(
          doc,
          verfPunkt,
          numberOfCopies,
          isDraft,
          isOriginal,
          unlockActionListener);
      waitForUnlock();
    }

    /**
     * Zeigt den PrintSetupDialog an, �ber den der aktuelle Drucker ausgew�hlt
     * und ge�ndert werden kann.
     * 
     * @param onlyOnce
     *          Gibt an, dass der Dialog nur beim ersten Aufruf (aus Sicht eines
     *          Dokuments) der Methode angezeigt wird. Wurde bereits vor dem
     *          Aufruf ein PrintSetup-Dialog gestartet, so �ffnet sich der
     *          Dialog nicht und die Methode endet ohne Aktion.
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#showPrinterSetupDialog()
     */
    public void showPrinterSetupDialog(boolean onlyOnce)
    {
      setLock();
      WollMuxEventHandler.handleShowPrinterSetupDialog(
          doc,
          onlyOnce,
          unlockActionListener);
      waitForUnlock();
    }

    /*
     * TODO: dokumentieren setFormValue (auch in der IDL!!!) (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setFormValue(java.lang.String,
     *      java.lang.String)
     */
    public void setFormValue(String id, String value)
    {
      setLock();
      WollMuxEventHandler.handleSetFormValueViaPrintModel(
          doc,
          id,
          value,
          unlockActionListener);
      waitForUnlock();
    }

    /**
     * Setzt einen lock, der in Verbindung mit setUnlock und der
     * waitForUnlock-Methode verwendet werden kann, um eine Synchronisierung mit
     * dem WollMuxEventHandler-Thread zu realisieren. setLock() sollte stets vor
     * dem Absetzen des WollMux-Events erfolgen, nach dem Absetzen des
     * WollMux-Events folgt der Aufruf der waitForUnlock()-Methode. Das
     * WollMuxEventHandler-Event erzeugt bei der Beendigung ein ActionEvent, das
     * daf�r sorgt, dass setUnlock aufgerufen wird.
     */
    protected void setLock()
    {
      lock[0] = true;
    }

    /**
     * Macht einen mit setLock() gesetzten Lock r�ckg�ngig und bricht damit eine
     * evtl. wartende waitForUnlock()-Methode ab.
     */
    protected void setUnlock()
    {
      synchronized (lock)
      {
        lock[0] = false;
        lock.notifyAll();
      }
    }

    /**
     * Wartet so lange, bis der vorher mit setLock() gesetzt lock mit der
     * Methode setUnlock() aufgehoben wird. So kann die Synchronisierung mit
     * Events aus dem WollMuxEventHandler-Thread realisiert werden. setLock()
     * sollte stets vor dem Aufruf des Events erfolgen, nach dem Aufruf des
     * Events folgt der Aufruf der waitForUnlock()-Methode. Das Event erzeugt
     * bei der Beendigung ein ActionEvent, das daf�r sorgt, dass setUnlock
     * aufgerufen wird.
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
     * Dieser ActionListener kann WollMuxHandler-Events �bergeben werden und
     * sorgt in Verbindung mit den Methoden setLock() und waitForUnlock() daf�r,
     * dass eine Synchronisierung mit dem WollMuxEventHandler-Thread realisiert
     * werden kann.
     */
    protected UnlockActionListener unlockActionListener = new UnlockActionListener();

    protected class UnlockActionListener implements ActionListener
    {
      public ActionEvent actionEvent = null;

      public void actionPerformed(ActionEvent arg0)
      {
        setUnlock();
        actionEvent = arg0;
      }
    }

    // Folgende Methoden werden ben�tigt, damit der Service unter Basic
    // angesprochen werden kann:
    // FIXME: queryInterface(XPrintModel) auf das Objekt geht nicht.
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
      return (PrintModel.class.getName());
    }

  }
}
