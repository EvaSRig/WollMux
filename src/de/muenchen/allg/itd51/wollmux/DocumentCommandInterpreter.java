/*
 * Dateiname: DocumentCommandInterpreter.java
 * Projekt  : WollMux
 * Funktion : Interpretiert die in einem Dokument enthaltenen Dokumentkommandos.
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
 * 14.10.2005 | LUT | Erstellung als WMCommandInterpreter
 * 24.10.2005 | LUT | + Sauberes umschliessen von Bookmarks in 
 *                      executeInsertFrag.
 *                    + Abschalten der lock-Controllers  
 * 02.05.2006 | LUT | Komplett-�berarbeitung und Umbenennung in
 *                    DocumentCommandInterpreter.
 * 05.05.2006 | BNK | Dummy-Argument zum Aufruf des FormGUI Konstruktors hinzugef�gt.
 * 17.05.2006 | LUT | Doku �berarbeitet.
 * 22.08.2006 | BNK | cleanInsertMarks() und EmptyParagraphCleaner verschmolzen zu
 *                  | SurroundingGarbageCollector und dabei komplettes Rewrite.
 * 23.08.2006 | BNK | nochmal Rewrite. Ich glaube dieser Code h�lt den Rekord im WollMux
 *                  | was rewrites angeht.
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyAttribute;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.style.XStyle;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.style.XStyleLoader;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertContent;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFrag;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.OverrideFrag;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetPrintFunction;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetType;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.UpdateFields;
import de.muenchen.allg.itd51.wollmux.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel.OverrideFragChainException;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.ooo.TextDocument;

/**
 * Diese Klasse repr�sentiert den Kommando-Interpreter zur Auswertung von
 * WollMux-Kommandos in einem gegebenen Textdokument.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class DocumentCommandInterpreter
{

  private TextDocumentModel model;

  /**
   * Enth�lt die Instanz auf das zentrale WollMuxSingleton.
   */
  private WollMuxSingleton mux;

  /**
   * Der Konstruktor erzeugt einen neuen Kommandointerpreter, der alle
   * Dokumentkommandos im �bergebenen Dokument xDoc scannen und interpretieren kann.
   * 
   * @param xDoc
   *          Das Dokument, dessen Kommandos ausgef�hrt werden sollen.
   * @param mux
   *          Die Instanz des zentralen WollMux-Singletons
   * @param frag_urls
   *          Eine Liste mit fragment-urls, die f�r das Kommando insertContent
   *          ben�tigt wird.
   */
  public DocumentCommandInterpreter(TextDocumentModel model, WollMuxSingleton mux)
  {
    this.model = model;
    this.mux = mux;
  }

  /**
   * Der Konstruktor erzeugt einen neuen Kommandointerpreter, der alle
   * Dokumentkommandos im �bergebenen Dokument xDoc scannen und interpretieren kann.
   * 
   * @param xDoc
   *          Das Dokument, dessen Kommandos ausgef�hrt werden sollen.
   * @param frag_urls
   *          Eine Liste mit fragment-urls, die f�r das Kommando insertContent
   *          ben�tigt wird.
   */
  public DocumentCommandInterpreter(TextDocumentModel model)
  {
    this.model = model;
    this.mux = WollMuxSingleton.getInstance();
  }

  /**
   * Diese Methode sollte vor executeTemplateCommands und vor executeFormCommands
   * aufgerufen werden und sorgt daf�r, dass alle globalen Einstellungen des
   * Dokuments an das TextDocumentModel weitergereicht werden.
   */
  public void scanGlobalDocumentCommands()
  {
    Logger.debug("scanGlobalDocumentCommands");
    boolean modified = model.getDocumentModified();

    GlobalDocumentCommandsScanner s = new GlobalDocumentCommandsScanner();
    s.execute(model.getDocumentCommands());

    model.setIDToFormFields(s.idToFormFields);
    model.collectNonWollMuxFormFields();

    model.setDocumentModified(modified);
  }

  /**
   * �ber diese Methode wird die Ausf�hrung der Kommandos gestartet, die f�r das
   * Expandieren und Bef�llen von Dokumenten notwendig sind.
   * 
   * @throws WMCommandsFailedException
   */
  public void executeTemplateCommands() throws WMCommandsFailedException
  {
    Logger.debug("executeTemplateCommands");
    boolean modified = model.getDocumentModified();

    // Z�hler f�r aufgetretene Fehler bei der Bearbeitung der Kommandos.
    int errors = 0;

    // Zuerst alle Kommandos bearbeiten, die irgendwie Kinder bekommen
    // k�nnen, damit der DocumentCommandTree vollst�ndig aufgebaut werden
    // kann.
    errors +=
      new DocumentExpander(model.getFragUrls()).execute(model.getDocumentCommands());

    // �bertr�gt beim �bergebenen XTextDocument doc die Eigenschaften der
    // Seitenvorlage Wollmuxseite auf die Seitenvorlage Standard, falls
    // Seitenvorlage Wollmuxseite vorhanden ist.
    pageStyleWollmuxseiteToStandard(model.doc);

    // Ziffern-Anpassen der Sachleitenden Verf�gungen aufrufen:
    SachleitendeVerfuegung.ziffernAnpassen(model);

    // Jetzt k�nnen die TextFelder innerhalb der updateFields Kommandos
    // geupdatet werden. Durch die Auslagerung in einen extra Schritt wird die
    // Reihenfolge der Abarbeitung klar definiert (zuerst die updateFields
    // Kommandos, dann die anderen Kommandos). Dies ist wichtig, da
    // insbesondere das updateFields Kommando exakt mit einem anderen Kommando
    // �bereinander liegen kann. Ausserdem liegt updateFields thematisch n�her
    // am expandieren der Textfragmente, da updateFields im Prinzip nur dessen
    // Schw�che beseitigt.
    errors += new TextFieldUpdater().execute(model.getDocumentCommands());

    // Hauptverarbeitung: Jetzt alle noch �brigen DocumentCommands (z.B.
    // insertValues) in einem einzigen Durchlauf mit execute bearbeiten.
    errors += new MainProcessor().execute(model.getDocumentCommands());

    // Da keine neuen Elemente mehr eingef�gt werden m�ssen, k�nnen
    // jetzt die INSERT_MARKS "<" und ">" der insertFrags und
    // InsertContent-Kommandos gel�scht werden.
    // errors += cleanInsertMarks(tree);

    // Erst nachdem die INSERT_MARKS entfernt wurden, lassen sich leere
    // Abs�tze zum Beginn und Ende der insertFrag bzw. insertContent-Kommandos
    // sauber erkennen und entfernen.
    // errors += new EmptyParagraphCleaner().execute(tree);
    SurroundingGarbageCollector collect = new SurroundingGarbageCollector();
    errors += collect.execute(model.getDocumentCommands());
    collect.removeGarbage();

    // da hier bookmarks entfernt werden, muss der Baum upgedatet werden
    model.getDocumentCommands().update();

    // Jetzt wird der Dokumenttyp formDocument gesetzt, um das Dokument als
    // Formulardokument auszuzeichnen, falls es ein Formularfenster definiert.
    if (model.hasFormGUIWindow()) model.setType("formDocument");

    // Document-Modified auf false setzen, da nur wirkliche
    // Benutzerinteraktionen den Modified-Status beeinflussen sollen.
    model.setDocumentModified(modified);

    // ggf. eine WMCommandsFailedException werfen:
    if (errors != 0)
    {
      throw new WMCommandsFailedException(
        L.m(
          "Die verwendete Vorlage enth�lt %1 Fehler.\n\nBitte kontaktieren Sie Ihre Systemadministration.",
          ((errors == 1) ? "einen" : "" + errors)));
    }
  }

  /**
   * �bertr�gt beim �bergebenen XTextDocument doc die Eigenschaften der Seitenvorlage
   * Wollmuxseite auf die Seitenvorlage Standard, falls Seitenvorlage Wollmuxseite
   * vorhanden ist.
   * 
   * @param doc
   *          Das dessen Seitenvorlage Standard an die Seitenvorlage Wollmuxseite
   *          angepasst werden soll, falls Wollmuxseite vorhanden ist.
   */
  public static void pageStyleWollmuxseiteToStandard(XTextDocument doc)
  {

    XStyleFamiliesSupplier styleFamiliesSupplier = UNO.XStyleFamiliesSupplier(doc);
    XNameAccess nameAccess = styleFamiliesSupplier.getStyleFamilies();

    XNameContainer nameContainer = null;
    // Holt die Seitenvorlagen
    try
    {
      nameContainer = UNO.XNameContainer(nameAccess.getByName("PageStyles"));
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }

    XStyle styleWollmuxseite = null;
    XStyle styleStandard = null;
    // Holt die Seitenvorlage Wollmuxseite und Standard
    try
    {
      styleWollmuxseite = UNO.XStyle(nameContainer.getByName("Wollmuxseite"));
      styleStandard = UNO.XStyle(nameContainer.getByName("Standard"));
    }
    catch (java.lang.Exception e)
    {}

    // Falls eine Seitenvorlage Wollmuxseite vorhanden ist, werden deren
    // Properties ausgelesen und alle, die nicht READONLY sind, auf die
    // Seitenvorlage Standard �bertragen.
    if (styleWollmuxseite != null)
    {
      XMultiPropertySet multiPropertySetWollmuxseite =
        UNO.XMultiPropertySet(styleWollmuxseite);
      XPropertySetInfo propertySetInfo =
        multiPropertySetWollmuxseite.getPropertySetInfo();
      Property[] propertys = propertySetInfo.getProperties();
      HashSet<String> set = new HashSet<String>();
      // Schleife �ber die Properties der Wollmuxseite. Wenn die Properties
      // nicht read-only sind werden sie in einem HashSet set
      // zwischengespeichert.
      for (int i = 0; i < propertys.length; i++)
      {
        String name = propertys[i].Name;
        boolean readonly =
          (propertys[i].Attributes & PropertyAttribute.READONLY) != 0;
        if (!readonly)
        {
          set.add(name);
        }
      }
      // In der Property "HeaderText" und "FooterText" befindet sich der Text
      // aus der Kopf-/Fu�zeile, w�rde dieser mit der Seitenvorlage
      // "Wollmuxseite" �berschrieben geht der Text verloren.
      set.remove("HeaderText");
      set.remove("FooterText");
      // Die Property "FollowStyle" wird nicht �bertragen da sie in der
      // Seitenvorlage "Wollmuxseite" Wollmuxseite ist und so die Folgeseiten
      // ein Seitenformat "Wollmuxseite" bekommen w�rden.
      set.remove("FollowStyle");
      int size;
      // Schleife wird so lange durchlaufen bis sich die Gr��e des HashSet set
      // nicht mehr �ndert. Es gibt Properties die auf das erste Mal nicht
      // gesetzt werden k�nnen, weil es Abh�ngigkeiten zu anderen Properties
      // gibt die zuerst gesetzt werden m�ssen. z.B mu� die Property f�r
      // Kopfzeile "HeaderIsOn" oder f�r Fu�zeile "FooterIsOn" "true"
      // sein, damit anderen Properties der Kopf-/Fu�zeile ver�ndert werden
      // k�nnen. Die Property "UserDefinesAttributes" wird nie gesetzt und
      // "TextColumns" wird nicht als ge�ndert erkannt.
      do
      {
        size = set.size();
        // Schleife �ber die HashSet set. �ber den Property-Name (aus dem
        // HashSet) wird die entsprechende Property aus der Seitenvorlage
        // "Wollmux" geholt und dann in die Seitenvorlage "Standard" �bertragen
        // und anschlie�end wenn dies funktioniert hat, wird der Property-Name
        // aus dem Iterator gel�scht.
        for (Iterator<String> iter = set.iterator(); iter.hasNext();)
        {
          String element = iter.next();
          Object value = UNO.getProperty(styleWollmuxseite, element);
          Object checkset = UNO.setProperty(styleStandard, element, value);
          if (UnoRuntime.areSame(value, checkset))
          {
            iter.remove();
          }
        }
      } while (size != set.size());
    }
  }

  /**
   * Hierbei handelt es sich um einen minimalen Scanner, der zu aller erst abl�uft
   * und die globalen Einstellungen des Dokuments (setType, setPrintFunction)
   * ausliest und dem TextDocumentModel zur Verf�gung stellt.
   * 
   * @author christoph.lutz
   */
  private class GlobalDocumentCommandsScanner extends DocumentCommands.Executor
  {
    public HashMap<String, List<FormField>> idToFormFields =
      new HashMap<String, List<FormField>>();

    private Map<String, FormField> bookmarkNameToFormField =
      new HashMap<String, FormField>();

    public int execute(DocumentCommands commands)
    {
      return executeAll(commands);
    }

    /**
     * Legacy-Methode: Stellt sicher, dass die im veralteten Dokumentkommando
     * SetPrintFunction gesetzten Werte in die persistenten Daten des Dokuments
     * �bertragen werden und das Dokumentkommando danach gel�scht wird.
     */
    public int executeCommand(SetPrintFunction cmd)
    {
      model.addPrintFunction(cmd.getFunctionName());
      cmd.markDone(true);
      return 0;
    }

    public int executeCommand(SetType cmd)
    {
      model.setType(cmd);

      // Wenn eine Mischvorlage zum Bearbeiten ge�ffnet wurde soll der typ
      // "templateTemplate" NICHT gel�scht werden, ansonsten schon.
      if (!(model.hasURL() && cmd.getType().equalsIgnoreCase("templateTemplate")))
        cmd.markDone(true);
      return 0;
    }

    public int executeCommand(InsertFormValue cmd)
    {
      // idToFormFields aufbauen
      String id = cmd.getID();
      LinkedList<FormField> fields;
      if (idToFormFields.containsKey(id))
      {
        fields = (LinkedList<FormField>) idToFormFields.get(id);
      }
      else
      {
        fields = new LinkedList<FormField>();
        idToFormFields.put(id, fields);
      }
      FormField field =
        FormFieldFactory.createFormField(model.doc, cmd, bookmarkNameToFormField);

      if (field != null)
      {
        field.setCommand(cmd);

        // sortiertes Hinzuf�gen des neuen FormFields zur Liste:
        ListIterator<FormField> iter = fields.listIterator();
        while (iter.hasNext())
        {
          FormField fieldA = iter.next();
          if (field.compareTo(fieldA) < 0)
          {
            iter.previous();
            break;
          }
        }
        iter.add(field);
      }

      return 0;
    }

  }

  /**
   * Der SurroundingGarbageCollector erfasst leere Abs�tze und Einf�gemarker um
   * Dokumentkommandos herum.
   */
  private class SurroundingGarbageCollector extends DocumentCommands.Executor
  {
    /**
     * Speichert Muellmann-Objekte, die zu l�schenden M�ll entfernen.
     */
    private List<Muellmann> muellmaenner = new Vector<Muellmann>();

    private abstract class Muellmann
    {
      protected XTextRange range;

      public Muellmann(XTextRange range)
      {
        this.range = range;
      }

      public abstract void tueDeinePflicht();
    }

    private class RangeMuellmann extends Muellmann
    {
      public RangeMuellmann(XTextRange range)
      {
        super(range);
      }

      public void tueDeinePflicht()
      {
        Bookmark.removeTextFromInside(model.doc, range);
      }
    }

    private class ParagraphMuellmann extends Muellmann
    {
      public ParagraphMuellmann(XTextRange range)
      {
        super(range);
      }

      public void tueDeinePflicht()
      {
        TextDocument.deleteParagraph(range);
      }
    }

    /**
     * Diese Methode erfasst leere Abs�tze und Einf�gemarker, die sich um die im
     * Kommandobaum tree enthaltenen Dokumentkommandos befinden.
     */
    private int execute(DocumentCommands commands)
    {
      int errors = 0;
      Iterator<DocumentCommand> iter = commands.iterator();
      while (iter.hasNext())
      {
        DocumentCommand cmd = iter.next();
        errors += cmd.execute(this);
      }

      return errors;
    }

    /**
     * L�scht die vorher als M�ll identifizierten Inhalte. type filter text
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    private void removeGarbage()
    {
      try
      {
        model.setLockControllers(true);
        Iterator<Muellmann> iter = muellmaenner.iterator();
        while (iter.hasNext())
        {
          Muellmann muellmann = iter.next();
          muellmann.tueDeinePflicht();
        }
      }
      finally
      {
        model.setLockControllers(false);
      }
    }

    public int executeCommand(InsertFrag cmd)
    {
      if (cmd.hasInsertMarks())
      {
        // ist der ManualMode gesetzt, so darf ein leerer Paragraph am Ende des
        // Dokuments nicht gel�scht werden, da sonst der ViewCursor auf den
        // Start des Textbereiches zur�ck gesetzt wird. Im Falle der
        // automatischen Einf�gung soll aber ein leerer Paragraph am Ende
        // gel�scht werden.
        collectSurroundingGarbageForCommand(cmd, cmd.isManualMode());
      }
      cmd.unsetHasInsertMarks();

      // Kommando l�schen wenn der WollMux nicht im debugModus betrieben wird.
      cmd.markDone(!mux.isDebugMode());

      return 0;
    }

    public int executeCommand(InsertContent cmd)
    {
      if (cmd.hasInsertMarks())
      {
        collectSurroundingGarbageForCommand(cmd, false);
      }
      cmd.unsetHasInsertMarks();

      // Kommando l�schen wenn der WollMux nicht im debugModus betrieben wird.
      cmd.markDone(!mux.isDebugMode());

      return 0;
    }

    // Helper-Methoden:

    /**
     * Diese Methode erfasst Einf�gemarken und leere Abs�tze zum Beginn und zum Ende
     * des �bergebenen Dokumentkommandos cmd, wobei �ber removeAnLastEmptyParagraph
     * gesteuert werden kann, ob ein Absatz am Ende eines Textes gel�scht werden soll
     * (bei true) oder nicht (bei false).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    private void collectSurroundingGarbageForCommand(DocumentCommand cmd,
        boolean removeAnLastEmptyParagraph)
    {
      /*
       * Im folgenden steht eine 0 in der ersten Stelle daf�r, dass vor dem
       * Einf�gemarker kein Text mehr steht (der Marker also am Anfang des Absatzes
       * ist). Eine 0 an der zweiten Stelle steht daf�r, dass hinter dem
       * Einf�gemarker kein Text mehr folgt (der Einf�gemarker also am Ende des
       * Absatzes steht). Ein "T" an dritter Stelle gibt an, dass hinter dem Absatz
       * des Einf�gemarkers eine Tabelle folgt. Ein "E" an dritter Stelle gibt an,
       * dass hinter dem Cursor das Dokument aufh�rt und kein weiterer Absatz kommt.
       * 
       * Startmarke: Grunds�tzlich gibt es die folgenden F�lle zu unterscheiden.
       * 
       * 00: Einf�gemarker und Zeilenumbruch DAHINTER l�schen
       * 
       * 01: nur Einf�gemarker l�schen
       * 
       * 10: nur Einf�gemarker l�schen
       * 
       * 11: nur Einf�gemarker l�schen
       * 
       * 00T: Einf�gemarker und Zeilenumbruch DAVOR l�schen
       * 
       * Die F�lle 01T, 10T und 11T werden nicht unterst�tzt.
       * 
       * Endmarke: Es gibt die folgenden F�lle:
       * 
       * 00: Einf�gemarker und Zeilenumbruch DAHINTER l�schen
       * 
       * 00E: Einf�gemarker und Zeilenumbruch DAVOR l�schen
       * 
       * 01, 10, 11: Einf�gemarker l�schen
       * 
       * DO NOT TOUCH THIS CODE ! Dieser Code ist komplex und fehleranf�llig.
       * Kleinste �nderungen k�nnen daf�r sorgen, dass irgendeine der 1000e von
       * Vorlagen pl�tzlich anders dargestellt wird. Das gew�nschte Verhalten dieses
       * Codes ist in diesem Kommentar vollst�ndig dokumentiert und �nderungen
       * sollten nur erfolgen, falls obiger Kommentar nicht korrekt umgesetzt wurde.
       * Um neue Anforderungen umzusetzen sollten unbedingt alle anderen
       * M�glichkeiten in Betracht gezogen werden bevor hier eine �nderung erfolgt.
       * Sollte eine �nderung unumgehbar sein, so ist sie VOR der Implementierung im
       * Wiki und in obigem Kommentar zu dokumentieren. Dabei ist darauf zu achten,
       * dass ein neuer Fall sich mit keinem der anderen F�lle �berschneidet.
       * 
       */
      XParagraphCursor[] start = cmd.getStartMark();
      XParagraphCursor[] end = cmd.getEndMark();
      if (start == null || end == null) return;

      // Startmarke auswerten:
      if (start[0].isStartOfParagraph() && start[1].isEndOfParagraph())
      {
        muellmaenner.add(new ParagraphMuellmann(start[1]));
      }
      else
      // if start mark is not the only text in the paragraph
      {
        start[1].goLeft((short) 1, true);
        muellmaenner.add(new RangeMuellmann(start[1]));
      }

      // Endemarke auswerten:

      // Pr�fen ob der Cursor am Ende des Dokuments steht. Anmerkung: hier kann
      // nicht der bereits vorhandene cursor end[1] zum Testen verwendet werden,
      // weil dieser durch den goRight ver�ndert w�rde. Man k�nnte ihn zwar mit
      // goLeft nachtr�glich wieder zur�ck schieben, aber das funzt nicht wenn
      // danach eine Tabelle kommt.
      XParagraphCursor docEndTest = cmd.getEndMark()[1];
      boolean isEndOfDocument = !docEndTest.goRight((short) 1, false);

      if (removeAnLastEmptyParagraph == false) isEndOfDocument = false;

      if (end[0].isStartOfParagraph() && end[1].isEndOfParagraph()
        && !isEndOfDocument)
      {
        muellmaenner.add(new ParagraphMuellmann(end[1]));
      }
      else
      {
        end[0].goRight(cmd.getEndMarkLength(), true);
        muellmaenner.add(new RangeMuellmann(end[0]));
      }
    }
  }

  /**
   * Der DocumentExpander sorgt daf�r, dass das Dokument nach Ausf�hrung der
   * enthaltenen Kommandos komplett aufgebaut ist und alle Textfragmente eingef�gt
   * wurden.
   * 
   * @author christoph.lutz
   * 
   */
  private class DocumentExpander extends DocumentCommands.Executor
  {
    private String[] fragUrls;

    private int fragUrlsCount = 0;

    // Markierung des ersten nicht ausgef�llten Platzhalter nach dem Einf�gen
    // von Textbausteinen
    private boolean firstEmptyPlaceholder = false;

    /**
     * Erzeugt einen neuen DocumentExpander, mit der Liste fragUrls, die die URLs
     * beschreibt, von denen die Textfragmente f�r den insertContent Befehl bezogen
     * werden sollen.
     * 
     * @param fragUrls
     */
    public DocumentExpander(String[] fragUrls)
    {
      this.fragUrls = fragUrls;
      this.fragUrlsCount = 0;
    }

    /**
     * F�hrt die Dokumentkommandos von commands aus, welche so lange aktualisiert
     * werden, bis das Dokument vollst�ndig aufgebaut ist. Die Dokumentkommandos
     * OverrideFrags erhalten dabei eine Sonderrolle, da sie bereits vor den anderen
     * Dokumentkommandos (insertFrag/insertContent) abgefeiert werden.
     * 
     * @param tree
     * @return
     * @throws WMCommandsFailedException
     */
    public int execute(DocumentCommands commands) throws WMCommandsFailedException
    {
      int errors = 0;

      // so lange wiederholen, bis sich der Baum durch das Expandieren nicht
      // mehr �ndert.
      do
      {
        errors += executeOverrideFrags(commands);
        errors += executeAll(commands);
      } while (commands.update());

      return errors;
    }

    /**
     * f�hrt alle OverrideFrag-Kommandos aus commands aus, wenn sie nicht den Status
     * DONE=true oder ERROR=true besitzen.
     * 
     * @param commands
     * @return Anzahl der bei der Ausf�hrung aufgetretenen Fehler.
     */
    protected int executeOverrideFrags(DocumentCommands commands)
    {
      int errors = 0;

      // Alle DocumentCommands durchlaufen und mit execute aufrufen.
      for (Iterator<DocumentCommand> iter = commands.iterator(); iter.hasNext();)
      {
        DocumentCommand cmd = iter.next();
        if (!(cmd instanceof OverrideFrag)) continue;

        if (cmd.isDone() == false && cmd.hasError() == false)
        {
          // Kommando ausf�hren und Fehler z�hlen
          errors += cmd.execute(this);
        }
      }
      return errors;
    }

    /**
     * Wertet ein OverrideFrag-Kommandos aus, �ber das Fragmente umgemapped werden
     * k�nnen, und setzt das Kommando sofort auf DONE. Dies geschieht vor der
     * Bearbeitung der anderen Kommandos (insertFrag/insertContent), da das mapping
     * beim insertFrag/insertContent ben�tigt wird.
     */
    public int executeCommand(OverrideFrag cmd)
    {
      try
      {
        model.setOverrideFrag(cmd.getFragID(), cmd.getNewFragID());
        cmd.markDone(!mux.isDebugMode());
        return 0;
      }
      catch (OverrideFragChainException e)
      {
        insertErrorField(cmd, e);
        Logger.error(e);
        cmd.setErrorState(true);
        return 1;
      }
    }

    /**
     * Diese Methode f�gt das Textfragment frag_id in den gegebenen Bookmark
     * bookmarkName ein. Im Fehlerfall wird eine entsprechende Fehlermeldung
     * eingef�gt.
     */
    public int executeCommand(InsertFrag cmd)
    {
      cmd.setErrorState(false);
      boolean found = false;
      String errors = "";
      String fragId = "";

      try
      {
        fragId = model.getOverrideFrag(cmd.getFragID());

        // Bei leeren FragIds wird nur der Text unter dem Dokumentkommando
        // gel�scht und das Dokumentkommando auf DONE gesetzt.
        if (fragId.length() == 0)
        {
          XTextCursor insCursor = cmd.createInsertCursor(false);
          if (insCursor != null) insCursor.setString("");
          cmd.markDone(false);
          return 0;
        }

        Vector<String> urls = VisibleTextFragmentList.getURLsByID(fragId);
        if (urls.size() == 0)
        {
          throw new ConfigurationErrorException(L.m(
            "Das Textfragment mit der FRAG_ID '%1' ist nicht definiert!",
            cmd.getFragID()));
        }
        // Iterator �ber URLs
        Iterator<String> iter = urls.iterator();
        while (iter.hasNext() && found == false)
        {
          String urlStr = iter.next();
          try
          {
            URL url = WollMuxFiles.makeURL(urlStr);

            Logger.debug(L.m("F�ge Textfragment '%1' von URL '%2' ein.",
              cmd.getFragID(), url.toExternalForm()));

            // styles bzw. fragment einf�gen:
            if (cmd.importStylesOnly())
              insertStylesFromURL(cmd, cmd.getStyles(), url);
            else
              insertDocumentFromURL(cmd, url);

            found = true;
            fillPlaceholders(model.doc, model.getViewCursor(),
              cmd.createInsertCursor(false), cmd.getArgs());
          }
          catch (java.lang.Exception e)
          {
            // Exception wird nicht beachtet. Wenn die aktuelle URL nicht
            // funktioniert wird die n�chste URL ausgewertet
            errors += e.getLocalizedMessage() + "\n\n";
            Logger.debug(e);
          }
        }
        if (!found)
        {
          throw new Exception(errors);
        }
      }
      catch (java.lang.Exception e)
      {
        if (cmd.isManualMode())
        {
          WollMuxSingleton.showInfoModal(L.m("WollMux-Fehler"), L.m(
            "Das Textfragment mit der FRAG_ID '%1' konnte nicht eingef�gt werden:",
            cmd.getFragID())
            + "\n\n" + e.getMessage());
        }
        else
        {
          insertErrorField(cmd, e);
        }
        Logger.error(e);
        cmd.setErrorState(true);
        return 1;
      }

      // Kommando als Done markieren aber noch aufheben. Gel�scht wird das
      // Bookmark dann erst durch den SurroundingGarbageCollector.
      cmd.markDone(false);
      return 0;
    }

    /**
     * Diese Methode f�gt das n�chste Textfragment aus der dem WMCommandInterpreter
     * �bergebenen frag_urls liste ein. Im Fehlerfall wird eine entsprechende
     * Fehlermeldung eingef�gt.
     */
    public int executeCommand(InsertContent cmd)
    {
      cmd.setErrorState(false);
      if (fragUrls.length > fragUrlsCount)
      {
        String urlStr = fragUrls[fragUrlsCount++];

        try
        {
          Logger.debug(L.m("F�ge Textfragment von URL '%1' ein.", urlStr));

          insertDocumentFromURL(cmd, WollMuxFiles.makeURL(urlStr));
        }
        catch (java.lang.Exception e)
        {
          insertErrorField(cmd, e);
          cmd.setErrorState(true);
          return 1;
        }
      }
      // Kommando als Done markieren aber noch aufheben. Gel�scht wird das
      // Bookmark dann erst durch den SurroundingGarbageCollector.
      cmd.markDone(false);
      return 0;
    }

    // Helper-Methoden:

    /**
     * Die Methode f�gt das externe Dokument von der URL url an die Stelle von cmd
     * ein. Die Methode enth�lt desweiteren notwendige Workarounds f�r die Bugs des
     * insertDocumentFromURL der UNO-API. public int execute(DocumentCommandTree
     * tree) { return executeDepthFirst(tree, false); }
     * 
     * @param cmd
     *          Einf�geposition
     * @param url
     *          die URL des einzuf�genden Textfragments
     * @throws java.io.IOException
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws java.io.IOException
     * @throws IOException
     */
    private void insertDocumentFromURL(DocumentCommand cmd, URL url)
        throws IllegalArgumentException, java.io.IOException, IOException
    {
      // Workaround: OOo friert ein, wenn ressource bei insertDocumentFromURL
      // nicht aufl�sbar. http://qa.openoffice.org/issues/show_bug.cgi?id=57049
      // Hier wird versucht, die URL �ber den java-Klasse url aufzul�sen und bei
      // Fehlern abgebrochen.
      WollMuxSingleton.checkURL(url);

      // URL durch den URLTransformer von OOo jagen, damit die URL auch von OOo
      // verarbeitet werden kann.
      String urlStr = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;

      // Workaround: Alten Paragraphenstyle merken. Problembeschreibung siehe
      // http://qa.openoffice.org/issues/show_bug.cgi?id=60475
      String paraStyleName = null;
      UnoService endCursor = new UnoService(null);
      XTextRange range = cmd.getTextRange();
      if (range != null)
      {
        endCursor =
          new UnoService(range.getText().createTextCursorByRange(range.getEnd()));
      }
      try
      {
        if (endCursor.xPropertySet() != null)
          paraStyleName =
            endCursor.getPropertyValue("ParaStyleName").getObject().toString();
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }

      // Liste aller TextFrames vor dem Einf�gen zusammenstellen (ben�tigt f�r
      // das
      // Updaten der enthaltenen TextFields sp�ter).
      HashSet<String> textFrames = new HashSet<String>();
      if (UNO.XTextFramesSupplier(model.doc) != null)
      {
        String[] names =
          UNO.XTextFramesSupplier(model.doc).getTextFrames().getElementNames();
        for (int i = 0; i < names.length; i++)
        {
          textFrames.add(names[i]);
        }
      }

      // Textfragment einf�gen:
      UnoService insCursor = new UnoService(cmd.createInsertCursor(true));
      if (insCursor.xDocumentInsertable() != null && urlStr != null)
      {
        insCursor.xDocumentInsertable().insertDocumentFromURL(urlStr,
          new PropertyValue[] {});
      }

      // Workaround: ParagraphStyleName f�r den letzten eingef�gten Paragraphen
      // wieder setzen (siehe oben).
      if (endCursor.xPropertySet() != null && paraStyleName != null)
      {
        try
        {
          endCursor.setPropertyValue("ParaStyleName", paraStyleName);
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }

    /**
     * Diese Methode importiert alle in styles angegebenen Formatvorlagen aus dem
     * durch url beschriebenen Fragment definiert und ersetzt dabei auch die bereits
     * bestehenden Formatvorlagen des aktuellen Dokuments. Nach der erfolgreichen
     * Einf�gung der Formatvorlagen wird der Inhalt des Dokumentkommandos gel�scht,
     * da ja mit dem Einf�gen keine Textinhalte eingef�gt werden.
     * 
     * @param cmd
     *          das Dokumentkommando dessen Inhalt nach dem erfolgreichen Einf�gen
     *          gel�scht wird.
     * @param styles
     *          ein Set mit den in Kleinbuchstaben geschriebenen Namen der zu
     *          importierenden styles.
     * @param url
     *          die URL des einzuf�genden Textfragments
     * @throws java.io.IOException
     * @throws IOException
     */
    private void insertStylesFromURL(DocumentCommand cmd, Set<String> styles, URL url)
        throws java.io.IOException, IOException
    {
      // Workaround f�r Einfrierfehler von OOo, wenn ressource nicht aufl�sbar
      // (ich habe nicht gepr�ft, ob das f�r insertStylesFromURL notwendig ist,
      // aber schaden kann es bestimmt nicht)
      WollMuxSingleton.checkURL(url);

      // URL durch den URLTransformer von OOo jagen, damit die URL auch von OOo
      // verarbeitet werden kann.
      String urlStr = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;

      // Styles einf�gen:
      try
      {
        UnoProps props = new UnoProps();
        props.setPropertyValue("OverwriteStyles", Boolean.TRUE);
        props.setPropertyValue("LoadCellStyles",
          Boolean.valueOf(styles.contains("cellstyles")));
        props.setPropertyValue("LoadTextStyles",
          Boolean.valueOf(styles.contains("textstyles")));
        props.setPropertyValue("LoadFrameStyles",
          Boolean.valueOf(styles.contains("framestyles")));
        props.setPropertyValue("LoadPageStyles",
          Boolean.valueOf(styles.contains("pagestyles")));
        props.setPropertyValue("LoadNumberingStyles",
          Boolean.valueOf(styles.contains("numberingstyles")));
        XStyleFamiliesSupplier sfs = UNO.XStyleFamiliesSupplier(model.doc);
        XStyleLoader loader = UNO.XStyleLoader(sfs.getStyleFamilies());
        loader.loadStylesFromURL(urlStr, props.getProps());
      }
      catch (NullPointerException e)
      {
        Logger.error(e);
      }

      // Textinhalt l�schen
      XTextRange range = cmd.createInsertCursor(false);
      if (range != null) range.setString("");
    }

    /**
     * Diese Methode f�llt die Einfuegestellen(Platzhalter) aus dem eingef�gten
     * Textbaustein mit den �bergebenen Argumente args
     * 
     * @param range
     *          der Bereich des eingef�gten Textbausteins
     * @param args
     *          Argumente die beim Aufruf zum Einf�gen �bergeben werden
     */
    private void fillPlaceholders(XTextDocument doc, XTextCursor viewCursor,
        XTextRange range, Vector<String> args)
    {
      // Vector mit allen Platzhalterfelder
      Vector<XTextField> placeholders = new Vector<XTextField>();

      XEnumeration xEnum = UNO.XEnumerationAccess(range).createEnumeration();
      XEnumerationAccess enuAccess;
      // Schleife �ber den Textbereich
      while (xEnum.hasMoreElements())
      {
        Object ele = null;
        try
        {
          ele = xEnum.nextElement();
        }
        catch (Exception e)
        {
          continue;
        }
        enuAccess = UNO.XEnumerationAccess(ele);
        if (enuAccess != null) // ist wohl ein SwXParagraph
        {
          XEnumeration textPortionEnu = enuAccess.createEnumeration();
          // Schleife �ber SwXParagraph und schauen ob es Platzhalterfelder gibt
          // diese diese werden dann im Vector placeholders gesammelt
          while (textPortionEnu.hasMoreElements())
          {
            Object textPortion;
            try
            {
              textPortion = textPortionEnu.nextElement();
            }
            catch (java.lang.Exception x)
            {
              continue;
            }
            String textPortionType =
              (String) UNO.getProperty(textPortion, "TextPortionType");
            // Wenn es ein Textfeld ist
            if (textPortionType.equals("TextField"))
            {
              XTextField textField = null;
              try
              {
                textField =
                  UNO.XTextField(UNO.getProperty(textPortion, "TextField"));
                // Wenn es ein Platzhalterfeld ist, dem Vector placeholders
                // hinzuf�gen
                if (UNO.supportsService(textField,
                  "com.sun.star.text.TextField.JumpEdit"))
                {
                  placeholders.add(textField);
                }
              }
              catch (java.lang.Exception e)
              {
                continue;
              }
            }
          }
        }
      }

      // Enumeration �ber den Vector placeholders mit Platzhalterfeldern die mit
      // den �bergebenen Argumenten gef�llt werden
      Enumeration<XTextField> enumPlaceholders = placeholders.elements();
      for (int j = 0; j < args.size() && j < placeholders.size(); j++)
      {
        Object placeholderObj = enumPlaceholders.nextElement();
        XTextField textField = UNO.XTextField(placeholderObj);
        XTextRange textFieldAnchor = textField.getAnchor();

        // bei einem Parameter ohne Inhalt bleibt die Einf�gestelle und die
        // erste ist nach dem Einf�gen markiert sonst wird
        // sie ersetzt
        if (!(args.elementAt(j).equals("")))
        {
          textFieldAnchor.setString(args.elementAt(j));
          // setzen des ViewCursor auf die erste nicht ausgef�llte Einf�gestelle
          // nach dem Einf�gen des Textbausteines
        }
        else if (firstEmptyPlaceholder != true)
        {
          try
          {
            firstEmptyPlaceholder = true;
            viewCursor.gotoRange(textFieldAnchor, false);
          }
          catch (java.lang.Exception e)
          {}
        }
      }

      // wenn weniger Parameter als Einf�gestellen angegeben wurden wird nach
      // dem Einf�gen des Textbaustein und f�llen der Argumente, die erste
      // unausgef�llte Einf�gestelle markiert.
      if (placeholders.size() > args.size())
      {
        if (firstEmptyPlaceholder == false)
        {
          XTextField textField = UNO.XTextField(placeholders.get(args.size()));
          XTextRange textFieldAnchor = textField.getAnchor();
          viewCursor.gotoRange(textFieldAnchor, false);
          firstEmptyPlaceholder = true;
        }
      }

      // Wenn nach dem Einf�gen keine Platzhalter vorhanden ist springt der
      // Cursor auf die definierte Marke setJumpMark (falls Vorhanden)
      if (placeholders.size() <= args.size())
      {
        WollMuxEventHandler.handleJumpToMark(doc, false);
      }

      // Wenn mehr Platzhalter angegeben als Einf�gestellen vorhanden, erscheint
      // ein Eintrag in der wollmux.log. Wenn in einer Conf Datei im Bereich
      // Textbausteine dort im Bereich Warnungen ein Eintrag mit
      // MSG_TOO_MANY_ARGS "true|on|1" ist, erscheint die Fehlermeldung in einem
      // Fenster im Writer.
      if (placeholders.size() < args.size())
      {

        String error =
          (L.m("Es sind mehr Parameter angegeben als Platzhalter vorhanden sind"));

        Logger.error(error);

        ConfigThingy conf = mux.getWollmuxConf();
        ConfigThingy WarnungenConf = conf.query("Textbausteine").query("Warnungen");

        String message = "";
        try
        {
          message = WarnungenConf.getLastChild().toString();
        }
        catch (NodeNotFoundException e)
        {}

        if (message.equals("true") || message.equals("on") || message.equals("1"))
        {
          WollMuxSingleton.showInfoModal("WollMux", error);
        }
      }
    }
  }

  /**
   * Der Hauptverarbeitungsschritt, in dem vor allem die Textinhalte gef�llt werden.
   * 
   * @author christoph.lutz
   * 
   */
  private class MainProcessor extends DocumentCommands.Executor
  {
    /**
     * Hauptverarbeitungsschritt starten.
     */
    private int execute(DocumentCommands commands)
    {
      try
      {
        model.setLockControllers(true);

        int errors = executeAll(commands);

        return errors;
      }
      finally
      {
        model.setLockControllers(false);
      }
    }

    /**
     * Besitzt das Dokument ein (inzwischen veraltetes) Form-Dokumentkommando, so
     * wird dieses in die persistenten Daten des Dokuments kopiert und die zugeh�rige
     * Notiz gel�scht, wenn nicht bereits eine Formularbeschreibung dort existiert.
     */
    public int executeCommand(DocumentCommand.Form cmd)
    {
      cmd.setErrorState(false);
      try
      {
        model.addToCurrentFormDescription(cmd);
      }
      catch (ConfigurationErrorException e)
      {
        insertErrorField(cmd, e);
        cmd.setErrorState(true);
        return 1;
      }
      cmd.markDone(!mux.isDebugMode());
      return 0;
    }

    /**
     * Diese Methode bearbeitet ein InvalidCommand und f�gt ein Fehlerfeld an der
     * Stelle des Dokumentkommandos ein.
     */
    public int executeCommand(DocumentCommand.InvalidCommand cmd)
    {
      insertErrorField(cmd, cmd.getException());
      cmd.setErrorState(true);
      return 1;
    }

    /**
     * Diese Methode f�gt einen Spaltenwert aus dem aktuellen Datensatz der
     * Absenderdaten ein. Im Fehlerfall wird die Fehlermeldung eingef�gt.
     */
    public int executeCommand(DocumentCommand.InsertValue cmd)
    {
      cmd.setErrorState(false);

      String spaltenname = cmd.getDBSpalte();
      String value = null;
      try
      {
        Dataset ds = mux.getDatasourceJoiner().getSelectedDatasetTransformed();
        value = ds.get(spaltenname);
        if (value == null) value = "";

        // ggf. TRAFO durchf�hren
        value = model.getTransformedValue(value, cmd.getTrafoName(), false);
      }
      catch (DatasetNotFoundException e)
      {
        value = L.m("<FEHLER: Kein Absender ausgew�hlt!>");
      }
      catch (ColumnNotFoundException e)
      {
        insertErrorField(cmd, e);
        cmd.setErrorState(true);
        return 1;
      }

      XTextCursor insCursor = cmd.createInsertCursor(false);
      if (insCursor != null)
      {
        if (value == null || value.equals(""))
        {
          insCursor.setString("");
        }
        else
        {
          insCursor.setString(cmd.getLeftSeparator() + value
            + cmd.getRightSeparator());
        }
      }
      cmd.markDone(!mux.isDebugMode());
      return 0;
    }
  }

  /**
   * Dieser Executor hat die Aufgabe alle updateFields-Befehle zu verarbeiten.
   */
  private class TextFieldUpdater extends DocumentCommands.Executor
  {
    /**
     * Ausf�hrung starten
     */
    private int execute(DocumentCommands commands)
    {
      try
      {
        model.setLockControllers(true);

        int errors = executeAll(commands);

        return errors;
      }
      finally
      {
        model.setLockControllers(false);
      }

    }

    /**
     * Diese Methode updated alle TextFields, die das Kommando cmd umschlie�t.
     */
    public int executeCommand(UpdateFields cmd)
    {
      XTextRange range = cmd.getTextRange();
      if (range != null)
      {
        UnoService cursor = new UnoService(range);
        updateTextFieldsRecursive(cursor);
      }
      cmd.markDone(!mux.isDebugMode());
      return 0;
    }

    /**
     * Diese Methode durchsucht das Element element bzw. dessen XEnumerationAccess
     * Interface rekursiv nach TextFeldern und ruft deren Methode update() auf.
     * 
     * @param element
     *          Das Element das geupdated werden soll.
     */
    private void updateTextFieldsRecursive(UnoService element)
    {
      // zuerst die Kinder durchsuchen (falls vorhanden):
      if (element.xEnumerationAccess() != null)
      {
        XEnumeration xEnum = element.xEnumerationAccess().createEnumeration();

        while (xEnum.hasMoreElements())
        {
          try
          {
            UnoService child = new UnoService(xEnum.nextElement());
            updateTextFieldsRecursive(child);
          }
          catch (java.lang.Exception e)
          {
            Logger.error(e);
          }
        }
      }

      // jetzt noch update selbst aufrufen (wenn verf�gbar):
      if (element.xTextField() != null)
      {
        try
        {
          UnoService textField = element.getPropertyValue("TextField");
          if (textField.xUpdatable() != null)
          {
            textField.xUpdatable().update();
          }
        }
        catch (Exception e)
        {}
      }
    }
  }

  // �bergreifende Helper-Methoden:

  /**
   * Diese Methode f�gt ein Fehler-Feld an die Stelle des Dokumentkommandos ein.
   */
  private void insertErrorField(DocumentCommand cmd, java.lang.Exception e)
  {
    String msg = L.m("Fehler in Dokumentkommando '%1'", cmd.getBookmarkName());

    // Meldung auch auf dem Logger ausgeben
    if (e != null)
      Logger.error(msg, e);
    else
      Logger.error(msg);

    UnoService cursor = new UnoService(cmd.createInsertCursor(false));
    cursor.xTextCursor().setString(L.m("<FEHLER:  >"));

    // Text fett und rot machen:
    try
    {
      cursor.setPropertyValue("CharColor", Integer.valueOf(0xff0000));
      cursor.setPropertyValue("CharWeight", Float.valueOf(FontWeight.BOLD));
    }
    catch (java.lang.Exception x)
    {
      Logger.error(x);
    }

    // Ein Annotation-Textfield erzeugen und einf�gen:
    try
    {
      XTextRange range = cursor.xTextCursor().getEnd();
      XTextCursor c = range.getText().createTextCursorByRange(range);
      c.goLeft((short) 2, false);
      XTextContent note =
        UNO.XTextContent(UNO.XMultiServiceFactory(model.doc).createInstance(
          "com.sun.star.text.TextField.Annotation"));
      UNO.setProperty(note, "Content", msg + ":\n\n" + e.getMessage());
      c.getText().insertTextContent(c, note, false);
    }
    catch (java.lang.Exception x)
    {
      Logger.error(x);
    }
  }
}
