/*
 * Dateiname: DocumentCommandInterpreter.java
 * Projekt  : WollMux
 * Funktion : Scannt alle Bookmarks eines Dokuments und interpretiert die enthaltenen
 *            Dokumentkommandos.
 * 
 * Copyright: Landeshauptstadt M�nchen
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
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.dialog.FormGUI;

/**
 * Diese Klasse repr�sentiert den Kommando-Interpreter zur Auswertung von
 * WollMux-Kommandos in einem gegebenen Textdokument.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class DocumentCommandInterpreter implements DocumentCommand.Executor
{

  /**
   * Enth�lt die Instanz auf das zentrale WollMuxSingleton.
   */
  private WollMuxSingleton mux;

  /**
   * Das Dokument, das interpretiert werden soll.
   */
  private UnoService document;

  /**
   * Die Liste der Fragment-urls, die bei den Kommandos "insertContent"
   * eingef�gt werden sollen.
   */
  private String[] fragUrls;

  /**
   * Die Liste der Fragment-urls, die bei den Kommandos "insertContent"
   * eingef�gt werden sollen.
   */
  private int fragUrlsCount = 0;

  /**
   * Das ConfigThingy enth�lt alle Form-Desriptoren, die im Lauf des
   * interpret-Vorgangs aufgesammelt werden.
   */
  private ConfigThingy formDescriptors;

  /**
   * Dieses Flag wird in executeForm auf true gesetzt, wenn das Dokument
   * mindestens eine Formularbeschreibung enth�lt.
   */
  private boolean documentIsAFormular;

  /**
   * Der Konstruktor erzeugt einen neuen Kommandointerpreter, der alle Kommandos
   * im �bergebenen xDoc scannen und entsprechend aufl�sen kann.
   * 
   * @param xDoc
   */
  public DocumentCommandInterpreter(XTextDocument xDoc, WollMuxSingleton mux,
      String[] frag_urls)
  {
    this.document = new UnoService(xDoc);
    this.mux = mux;
    this.formDescriptors = new ConfigThingy("Forms");
    this.documentIsAFormular = false;
    this.fragUrls = frag_urls;
  }

  /**
   * �ber diese Methode wird die eigentliche Ausf�hrung der Interpretation der
   * Dokumentkommandos gestartet. Ein WM-Kommando besitzt sie Syntax "WM (
   * Unterkommando ){Zahl}", wobei Spaces an jeder Stelle auftauchen oder
   * weggelassen werden d�rfen. Der Wert {Zahl} am Ende des Kommandos dient zur
   * Unterscheidung verschiedener Bookmarks in OOo und ist optional.
   * 
   * Alle Bookmarks, die nicht dieser Syntax entsprechen, werden als normale
   * Bookmarks behandelt und nicht vom Interpreter bearbeitet.
   * 
   * @throws EndlessLoopException
   * @throws WMCommandsFailedException
   */
  public void interpret() throws WMCommandsFailedException
  {

    // Dokumentkommando-Baum scannen:
    DocumentCommandTree tree = new DocumentCommandTree(document.xComponent(),
        mux.isDebugMode());
    tree.update();

    // Z�hler f�r aufgetretene Fehler bei der Bearbeitung der Kommandos.
    int errors = 0;

    if (isOpenAsTemplate(tree))
    {
      // 1) Zuerst alle Kommandos bearbeiten, die irgendwie Kinder bekommen
      // k�nnen, damit der DocumentCommandTree vollst�ndig aufgebaut werden
      // kann.
      errors += processInsertFrags(tree);

      // 2) W�hrend der Bearbeitung der einfachen Kommandos (z.B. insertValues)
      // muss man nicht jede �nderung sofort sehen (Geschwindigkeitsvorteil, da
      // OOo nicht neu rendern muss). Ist im Debug-Modus abgeschalten:
      if (document.xModel() != null && !mux.isDebugMode())
        document.xModel().lockControllers();

      // 3) Und jetzt nochmal alle (�brigen) DocumentCommands (z.B.
      // insertValues) in einem einzigen Durchlauf mit execute aufrufen.
      errors += processAllDocumentCommands(tree);

      // 4) Da keine neuen Elemente mehr eingef�gt werden m�ssen, k�nnen
      // jetzt die INSERT_MARKS "<" und ">" der insertFrags und
      // InsertContent-Kommandos gel�scht werden.
      tree.cleanInsertMarks();

      // 5) Erst nachdem die INSERT_MARKS entfernt wurden, lassen sich leere
      // Abs�tze zum Beginn und Ende der insertFrag bzw. insertContent-Kommandos
      // sauber erkennen und entfernen.
      cleanEmptyParagraphs(tree);

      // 6) Jetzt darf man wieder was sehen:
      if (document.xModel() != null && !mux.isDebugMode())
        document.xModel().unlockControllers();
    }

    // Folgende Schritte sollen auch ausgef�hrt werden, wenn das Dokument "als
    // Dokument" ge�ffnet wurde:

    // 7) Die Status�nderungen der Dokumentkommandos auf die Bookmarks
    // �bertragen bzw. die Bookmarks abgearbeiteter Kommandos l�schen. Der
    // Schritt soll aus folgenden Gr�nden auch dann ausgef�hrt werden, wenn das
    // Dokument "als Dokument" ge�ffnet wurde:
    // a) Damit ein evtl. vorhandenes Dokumentkommando BeADocument gel�scht
    // wird.
    // b) Zur Normierung der enthaltenen Bookmarks.
    tree.updateBookmarks();

    // 8) Document-Modified auf false setzen, da nur wirkliche
    // Benutzerinteraktionen den Modified-Status beeinflussen sollen.
    setDocumentModified(false);

    // ggf. eine WMCommandsFailedException werfen:
    if (errors != 0)
    {
      throw new WMCommandsFailedException(
          "Bei der Dokumenterzeugung mit dem Briefkopfsystem trat(en) "
              + errors
              + " Fehler auf.\n\n"
              + "Bitte �berpr�fen Sie das Dokument und kontaktieren ggf. die "
              + "f�r Sie zust�ndige Systemadministration.");
    }

    // Formulardialog starten:
    if (documentIsAFormular)
    {
      startFormGUI();
    }
  }

  /**
   * Die Methode ermittelt, ob das Dokument als Vorlage behandelt wird und damit
   * die enthaltenen Dokumentkommandos ausgewertet werden sollen (bei true) oder
   * nicht (bei false).
   */
  private boolean isOpenAsTemplate(DocumentCommandTree tree)
  {
    // Vorbelegung: im Zweifelsfall immer als Template �ffnen.
    boolean isTemplate = true;

    // Bei Templates besitzt das xDoc kein URL-Attribut, bei normalen
    // Dokumenten schon.
    if (document.xTextDocument() != null)
      isTemplate = (document.xTextDocument().getURL() == null || document
          .xTextDocument().getURL().equals(""));

    // jetzt noch die Kommandos BeATemplate und BeADocument auswerten:
    Iterator iter = tree.depthFirstIterator(false);
    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();

      // gleich abbrechen, wenn eines der beiden Kommandos gefunden wurde.
      if (cmd instanceof DocumentCommand.ON)
      {
        cmd.setDoneState(true);
        return true;
      }
      if (cmd instanceof DocumentCommand.OFF)
      {
        cmd.setDoneState(true);
        return false;
      }
    }

    return isTemplate;
  }

  private void setDocumentModified(boolean state)
  {
    try
    {
      document.xModifiable().setModified(state);
    }
    catch (PropertyVetoException x)
    {
      // wenn jemand was dagegen hat, dann setze ich halt nichts.
    }
  }

  private void cleanEmptyParagraphs(DocumentCommandTree tree)
  {
    Iterator iter = tree.depthFirstIterator(false);
    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      if (cmd.isDone() == true
          && cmd instanceof DocumentCommand.FragmentInserter)
      {
        cleanEmptyParagraphsForCommand(cmd);
      }
    }
  }

  private int processInsertFrags(DocumentCommandTree tree)
  {
    int errors = 0;
    for (boolean changed = true; changed;)
    {
      changed = false;

      // Alle (neuen) DocumentCommands durchlaufen und mit execute aufrufen.
      tree.update();
      Iterator iter = tree.depthFirstIterator(false);
      while (iter.hasNext())
      {
        DocumentCommand cmd = (DocumentCommand) iter.next();

        if (cmd.isDone() == false
            && cmd.hasError() == false
            && cmd instanceof DocumentCommand.FragmentInserter)
        {
          // Kommando ausf�hren und Fehler z�hlen
          errors += cmd.execute(this);
          changed = true;
        }
      }
    }
    return errors;
  }

  private int processAllDocumentCommands(DocumentCommandTree tree)
  {
    int errors = 0;
    Iterator iter = tree.depthFirstIterator(false);
    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      if (cmd.isDone() == false && cmd.hasError() == false)
      {
        // Kommandos ausf�hren und Fehler z�hlen
        errors += cmd.execute(this);
      }
    }
    return errors;
  }

  private void cleanEmptyParagraphsForCommand(DocumentCommand cmd)
  {
    Logger.debug2("cleanEmptyParagraphsForCommand(" + cmd + ")");

    // Ersten Absatz l�schen, falls er leer ist:

    // ben�tigte TextCursor holen:
    XTextRange range = cmd.getTextRange();
    UnoService fragStart = new UnoService(null);
    UnoService marker = new UnoService(null);
    if (range != null)
    {
      fragStart = new UnoService(range.getText().createTextCursorByRange(
          range.getStart()));
      marker = new UnoService(range.getText().createTextCursor());
    }

    if (fragStart.xParagraphCursor() != null
        && fragStart.xParagraphCursor().isEndOfParagraph())
    {
      // Der Cursor ist am Ende des Absatzes. Das sagt uns, dass der erste
      // eingef�gte Absatz ein leerer Absatz war. Damit soll dieser Absatz
      // gel�scht werden:

      // Jetzt wird der marker verwendet, um die zu l�schenden Absatzvorsch�be
      // zu markieren. Hier muss man zuerst eins nach rechts gehen und den
      // Bereich von rechts nach links aufziehen, denn sonst w�rden nach dem
      // kommenden L�schvorgang (setString("")) die Absatzmerkmale des
      // vorherigen Absatzes benutzt und nicht die des n�chsten Absatzes wie
      // gew�nscht.
      marker.xTextCursor().gotoRange(fragStart.xTextRange(), false);
      marker.xParagraphCursor().goRight((short) 1, false);
      marker.xParagraphCursor().goLeft((short) 1, true);

      // In manchen F�llen verh�lt sich der Textcursor nach den obigen zwei
      // Zeilen anders als erwartet. Z.B. wenn der n�chsten Absatz eine
      // TextTable ist. In diesem Fall ist nach obigen zwei Zeilen die ganze
      // Tabelle markiert und nicht nur das Absatztrennzeichen. Der Cursor
      // markiert also mehr Inhalt als nur den erwarteten Absatzvorschub. In
      // einem solchen Fall, darf der markierte Inhalt nicht gel�scht werden.
      // Anders ausgedr�ckt, darf der Absatz nur gel�scht werden, wenn beim
      // Markieren ausschlie�lich Text markiert wurde.
      if (isFollowedByTextParagraph(marker.xEnumerationAccess()))
      {
        // Normalfall: hier darf gel�scht werden
        Logger.debug2("Loesche Absatzvorschubzeichen");

        // Workaround: Normalerweise reicht der setString("") zum L�schen des
        // Zeichens. Jedoch im Spezialfall, dass der zweite Absatz auch leer
        // ist, w�rde der zweite Absatz ohne den folgenden Workaround seine
        // Formatierung verlieren. Das Problem ist gemeldet unter:
        // http://qa.openoffice.org/issues/show_bug.cgi?id=65384

        // Workaround: bevor der Absatz gel�scht wird, f�ge ich in den zweiten
        // Absatz einen Inhalt ein.
        marker.xTextCursor().goRight((short) 1, false); // cursor korrigieren
        marker.xTextCursor().setString("c");
        marker.xTextCursor().collapseToStart();
        marker.xTextCursor().goLeft((short) 1, true); // cursor wie vorher

        // hier das eigentliche L�schen des Absatzvorschubs
        marker.xTextCursor().setString("");

        // Workaround: Nun wird der vorher eingef�gte Inhalt wieder gel�scht.
        marker.xTextCursor().goRight((short) 1, true);
        marker.xTextCursor().setString("");
      }
      else
      {
        // In diesem Fall darf normalerweise nichts gel�scht werden, ausser der
        // Einf�gepunkt des insertFrags/insertContent selbst ist ein
        // leerer Absatz. Dieser leere Absatz kann als ganzes gel�scht
        // werden. Man erkennt den Fall daran, dass fragStart auch der Anfang
        // des Absatzes ist.
        if (fragStart.xParagraphCursor().isStartOfParagraph())
        {
          Logger.debug2("Loesche den ganzen leeren Absatz");
          deleteParagraph(fragStart.xTextCursor());
          // Hierbei wird das zugeh�rige Bookmark ung�ltig, da es z.B. eine
          // enthaltene TextTable nicht mehr umschlie�t. Aus diesem Grund werden
          // Bookmarks nach erfolgreicher Ausf�hrung gel�scht...
        }
      }
    }

    // Letzten Absatz l�schen, falls er leer ist:

    // der Range muss hier nochmal geholt werden, f�r den Fall, dass obige
    // Zeilen das Bookmark mit l�schen (der delete Paragraph tut dies z.B. beim
    // insertFrag "Fusszeile" im Zusammenspiel mit TextTables).
    range = cmd.getTextRange();
    UnoService fragEnd = new UnoService(null);
    if (range != null)
    {
      fragEnd = new UnoService(range.getText().createTextCursorByRange(
          range.getEnd()));
      marker = new UnoService(range.getText().createTextCursor());
    }

    if (fragEnd.xParagraphCursor() != null
        && fragEnd.xParagraphCursor().isStartOfParagraph())
    {
      marker.xTextCursor().gotoRange(fragEnd.xTextRange(), false);
      marker.xTextCursor().goLeft((short) 1, true);
      marker.xTextCursor().setString("");
    }
  }

  /**
   * Die Methode pr�ft, ob der zweite Paragraph des markierte Bereichs ein
   * TextParagraph ist und gibt true zur�ck, wenn der zweite Paragraph nicht
   * vorhanden ist oder er den Service com.sun.star.text.Paragraph
   * implementiert.
   * 
   * @param enumAccess
   * @return
   */
  private boolean isFollowedByTextParagraph(XEnumerationAccess enumAccess)
  {
    if (enumAccess != null)
    {
      XEnumeration xenum = enumAccess.createEnumeration();
      Object element2 = null;

      if (xenum.hasMoreElements()) try
      {
        xenum.nextElement();
      }
      catch (Exception e)
      {
      }

      if (xenum.hasMoreElements())
      {
        try
        {
          element2 = xenum.nextElement();
        }
        catch (Exception e)
        {
        }
      }
      else
        return true;

      return new UnoService(element2)
          .supportsService("com.sun.star.text.Paragraph");
    }
    return false;
  }

  /**
   * L�scht den ganzen ersten Absatz an der Cursorposition.
   * 
   * @param textCursor
   */
  private void deleteParagraph(XTextCursor textCursor)
  {
    // Beim L�schen des Absatzes erzeugt OOo ein ungewolltes "Zombie"-Bookmark.
    // Issue Siehe http://qa.openoffice.org/issues/show_bug.cgi?id=65247

    UnoService cursor = new UnoService(textCursor);

    // Ersten Absatz des Bookmarks holen:
    UnoService par = new UnoService(null);
    if (cursor.xEnumerationAccess().createEnumeration() != null)
    {
      XEnumeration xenum = cursor.xEnumerationAccess().createEnumeration();
      if (xenum.hasMoreElements()) try
      {
        par = new UnoService(xenum.nextElement());
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }

    // L�sche den Paragraph
    if (cursor.xTextCursor() != null && par.xTextContent() != null) try
    {
      cursor.xTextCursor().getText().removeTextContent(par.xTextContent());
    }
    catch (NoSuchElementException e)
    {
      Logger.error(e);
    }
  }

  /**
   * Diese Methode startet die FormularGUI. Sie enth�lt vorerst nur eine
   * dummy-implementierung, bei der auch noch keine im Dokument gesetzten
   * Feldinhalte ausgewertet werden. D.h. die GUI startet immer mit einer leeren
   * IdToPreset-Map.
   */
  private void startFormGUI()
  {
    FormModel fm = new FormModelImpl(document.xComponent());
    new FormGUI(formDescriptors, fm, new HashMap(), mux.getGlobalFunctions(),
        mux.getFunctionDialogs());
  }

  public int executeCommand(DocumentCommand.InvalidCommand cmd)
  {
    insertErrorField(cmd, cmd.getException());
    cmd.setErrorState(true);
    return 1;
  }

  /**
   * Diese Methode f�gt einen Spaltenwert aus dem aktuellen Datensatz ein. Im
   * Fehlerfall wird die Fehlermeldung eingef�gt.
   */
  public int executeCommand(DocumentCommand.InsertValue cmd)
  {
    cmd.setErrorState(false);
    try
    {
      String spaltenname = cmd.getDBSpalte();
      Dataset ds;
      try
      {
        ds = mux.getDatasourceJoiner().getSelectedDataset();
      }
      catch (DatasetNotFoundException e)
      {
        throw new Exception(
            "Kein Absender ausgew�hlt! Bitte w�hlen Sie einen Absender aus!");
      }
      XTextCursor insCursor = cmd.createInsertCursor();
      if (insCursor != null)
      {
        if (ds.get(spaltenname) == null || ds.get(spaltenname).equals(""))
        {
          insCursor.setString("");
        }
        else
        {
          insCursor.setString(cmd.getLeftSeparator()
                              + ds.get(spaltenname)
                              + cmd.getRightSeparator());
        }
      }
    }
    catch (java.lang.Exception e)
    {
      insertErrorField(cmd, e);
      cmd.setErrorState(true);
      return 1;
    }
    cmd.setDoneState(true);
    return 0;
  }

  /**
   * Diese Methode f�gt das Textfragment frag_id in den gegebenen Bookmark
   * bookmarkName ein. Im Fehlerfall wird die Fehlermeldung eingef�gt.
   * 
   * @param frag_id
   *          FRAG_ID, des im Abschnitt Textfragmente in der Konfigurationsdatei
   *          definierten Textfragments.
   * @param bookmarkName
   *          Name des bookmarks, in das das Fragment eingef�gt werden soll.
   */
  public int executeCommand(DocumentCommand.InsertFrag cmd)
  {
    cmd.setErrorState(false);
    try
    {
      // Fragment-URL holen und aufbereiten. Kontext ist der DEFAULT_CONTEXT.
      String urlStr = mux.getTextFragmentList().getURLByID(cmd.getFragID());

      URL url = new URL(mux.getDEFAULT_CONTEXT(), urlStr);

      Logger.debug("F�ge Textfragment \""
                   + cmd.getFragID()
                   + "\" von URL \""
                   + url.toExternalForm()
                   + "\" ein.");

      // fragment einf�gen:
      insertDocumentFromURL(cmd, url);
    }
    catch (java.lang.Exception e)
    {
      insertErrorField(cmd, e);
      cmd.setErrorState(true);
      return 1;
    }
    cmd.setDoneState(true);
    return 0;
  }

  /**
   * Die Methode f�gt das externe Dokument von der URL url an die Stelle von cmd
   * ein. Die Methode enth�lt desweiteren notwendige Workarounds f�r die Bugs
   * des insertDocumentFromURL der UNO-API.
   * 
   * @param cmd
   * @param url
   * @throws java.io.IOException
   * @throws IOException
   * @throws IllegalArgumentException
   */
  private void insertDocumentFromURL(DocumentCommand cmd, URL url)
      throws java.io.IOException, IOException, IllegalArgumentException
  {

    // Workaround: OOo friert ein, wenn ressource bei insertDocumentFromURL
    // nicht aufl�sbar. http://qa.openoffice.org/issues/show_bug.cgi?id=57049
    // Hier wird versucht, die URL �ber den java-Klasse url aufzul�sen und bei
    // Fehlern abgebrochen.
    if (url.openConnection().getContentLength() <= 0)
    {
      throw new IOException("Das Textfragment mit der URL \""
                            + url.toExternalForm()
                            + "\" ist leer oder nicht verf�gbar");
    }

    // URL durch den URLTransformer von OOo jagen, damit die URL auch von OOo
    // verarbeitet werden kann.
    String urlStr = null;
    try
    {
      UnoService trans = UnoService.createWithContext(
          "com.sun.star.util.URLTransformer",
          mux.getXComponentContext());
      com.sun.star.util.URL[] unoURL = new com.sun.star.util.URL[] { new com.sun.star.util.URL() };
      unoURL[0].Complete = url.toExternalForm();
      trans.xURLTransformer().parseStrict(unoURL);
      urlStr = unoURL[0].Complete;
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    // Workaround: Alten Paragraphenstyle merken. Problembeschreibung siehe
    // http://qa.openoffice.org/issues/show_bug.cgi?id=60475
    String paraStyleName = null;
    UnoService endCursor = new UnoService(null);
    XTextRange range = cmd.getTextRange();
    if (range != null)
    {
      endCursor = new UnoService(range.getText().createTextCursorByRange(
          range.getEnd()));
    }
    try
    {
      if (endCursor.xPropertySet() != null)
        paraStyleName = endCursor.getPropertyValue("ParaStyleName").getObject()
            .toString();
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }

    // Textfragment einf�gen:
    UnoService insCursor = new UnoService(cmd.createInsertCursor());
    if (insCursor.xDocumentInsertable() != null && urlStr != null)
    {
      insCursor.xDocumentInsertable().insertDocumentFromURL(
          urlStr,
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
   * Diese Methode f�gt das n�chste Textfragment aus der dem
   * WMCommandInterpreter �bergebenen frag_urls liste ein. Im Fehlerfall wird
   * die Fehlermeldung eingef�gt.
   * 
   * @param frag_id
   *          FRAG_ID, des im Abschnitt Textfragmente in der Konfigurationsdatei
   *          definierten Textfragments.
   * @param bookmarkName
   *          Name des bookmarks, in das das Fragment eingef�gt werden soll.
   */
  public int executeCommand(DocumentCommand.InsertContent cmd)
  {
    cmd.setErrorState(false);
    if (fragUrls.length > fragUrlsCount)
    {
      String urlStr = fragUrls[fragUrlsCount++];

      try
      {
        Logger.debug("F�ge Textfragment von URL \"" + urlStr + "\" ein.");

        insertDocumentFromURL(cmd, new URL(urlStr));
      }
      catch (java.lang.Exception e)
      {
        insertErrorField(cmd, e);
        cmd.setErrorState(true);
        return 1;
      }
    }
    cmd.setDoneState(true);
    return 0;
  }

  /**
   * Hinter einem Form-Kommando verbirgt sich eine Notiz, die das Formular
   * beschreibt, das in der FormularGUI angezeigt werden soll. Das Kommando
   * executeForm sammelt alle solchen Formularbeschreibungen im formDescriptor.
   * Enth�lt der formDescriptor mehr als einen Eintrag, wird nach dem
   * interpret-Vorgang die FormGUI gestartet.
   * 
   * @param bookmarkName
   * @param state
   * @return
   */
  public int executeCommand(DocumentCommand.Form cmd)
  {
    cmd.setErrorState(false);
    try
    {
      XTextRange range = cmd.getTextRange();
      Object content = null;
      if (range != null)
      {
        UnoService cursor = new UnoService(range.getText()
            .createTextCursorByRange(range));
        UnoService textfield = cursor.getPropertyValue("TextField");
        content = textfield.getPropertyValue("Content").getObject();
      }
      if (content != null)
      {
        ConfigThingy ct = new ConfigThingy("", null, new StringReader(content
            .toString()));
        ConfigThingy formulars = ct.query("Formular");
        if (formulars.count() == 0)
          throw new ConfigurationErrorException(
              "Formularbeschreibung enth�lt keinen Abschnitt \"Formular\".");
        documentIsAFormular = true;
        Iterator formIter = formulars.iterator();
        while (formIter.hasNext())
        {
          ConfigThingy form = (ConfigThingy) formIter.next();
          formDescriptors.addChild(form);
        }
      }
    }
    catch (java.lang.Exception e)
    {
      insertErrorField(cmd, e);
      cmd.setErrorState(true);
      return 1;
    }
    cmd.setDoneState(true);
    return 0;
  }

  /**
   * Gibt Informationen �ber die aktuelle Install-Version des WollMux aus.
   */
  public int executeCommand(DocumentCommand.Version cmd)
  {
    XTextCursor insCurs = cmd.createInsertCursor();
    if (insCurs != null)
      insCurs.setString("Build-Info: " + mux.getBuildInfo());

    cmd.setDoneState(true);
    return 0;
  }

  /**
   * Diese Methode f�gt ein Fehler-Feld an die Stelle des Dokumentkommandos ein.
   */
  private void insertErrorField(DocumentCommand cmd, java.lang.Exception e)
  {
    String msg = "Fehler in Dokumentkommando \"" + cmd.getBookmarkName() + "\"";

    // Meldung auch auf dem Logger ausgeben
    if (e != null)
      Logger.error(msg, e);
    else
      Logger.error(msg);

    UnoService cursor = new UnoService(cmd.createInsertCursor());
    cursor.xTextCursor().setString("<FEHLER:  >");

    // Text fett und rot machen:
    try
    {
      cursor.setPropertyValue("CharColor", new Integer(0xff0000));
      cursor.setPropertyValue("CharWeight", new Float(FontWeight.BOLD));
    }
    catch (java.lang.Exception x)
    {
      Logger.error(x);
    }

    // Ein Annotation-Textfield erzeugen und einf�gen:
    try
    {
      XTextRange range = cursor.xTextCursor().getEnd();
      UnoService c = new UnoService(range.getText().createTextCursorByRange(
          range));
      c.xTextCursor().goLeft((short) 2, false);
      UnoService note = document
          .create("com.sun.star.text.TextField.Annotation");
      note.setPropertyValue("Content", msg + ":\n\n" + e.getMessage());
      c.xTextRange().getText().insertTextContent(
          c.xTextRange(),
          note.xTextContent(),
          false);
    }
    catch (java.lang.Exception x)
    {
      Logger.error(x);
    }
  }
}
