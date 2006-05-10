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
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.dialog.FormGUI;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Diese Klasse repr�sentiert den Kommando-Interpreter zur Auswertung von
 * WollMux-Kommandos in einem gegebenen Textdokument.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class DocumentCommandInterpreter implements DocumentCommand.Executor
{

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
   * Abbruchwert f�r die Anzahl der Interationsschritte beim Auswerten neu
   * hinzugekommener Bookmarks.
   */
  private static final int MAXCOUNT = 100;

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
   * WM-Kommandos gestartet. Ein WM-Kommando besitzt sie Syntax "WM (
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
  public void interpret() throws EndlessLoopException,
      WMCommandsFailedException
  {
    // Dokumentkommando-Baum scannen:

    DocumentCommandTree tree = new DocumentCommandTree(document.xComponent(),
        mux.isDebugMode());

    // Z�hler f�r aufgetretene Fehler bei der Bearbeitung der Kommandos.
    int errorCount = 0;

    // Zuerst alle Kommandos bearbeiten, die irgendwie Kinder bekommen k�nnen,
    // d.h. alle insertFrags und insertContents. Das geschieht so lange, bis
    // sich der Baum nicht mehr �ndert. Der loopCount dient zur Vermeidung von
    // Endlosschleifen.
    boolean changed = true;
    int loopCount = 0;
    while (changed && MAXCOUNT > ++loopCount)
    {
      changed = false;

      // Alle (neuen) DocumentCommands durchlaufen und mit execute aufrufen.
      tree.update();
      Iterator iter = tree.depthFirstIterator(false);
      while (iter.hasNext())
      {
        DocumentCommand cmd = (DocumentCommand) iter.next();

        if (cmd.canHaveChilds() && cmd.isDone() == false)
        {
          // Kommando ausf�hren und Fehler z�hlen
          errorCount += cmd.execute(this);
          changed = true;
        }
      }
    }

    // Bei der Bearbeitung der insertValues muss man nicht jede �nderung sofort
    // sehen:
    if (document.xModel() != null) document.xModel().lockControllers();

    // Und jetzt nochmal alle (�brigen) DocumentCommands (z.B. insertValues) in
    // einem einzigen Durchlauf mit execute aufrufen.
    tree.update();
    Iterator iter = tree.depthFirstIterator(false);
    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      if (cmd.isDone() == false)
      {
        // Kommandos ausf�hren und Fehler z�hlen
        errorCount += cmd.execute(this);
      }
    }

    // entfernen der INSERT_MARKS
    tree.cleanInsertMarks();

    // jetzt nochmal den Baum durchgehen und alle leeren Abs�tze zum Beginn und
    // Ende der insertFrags und insertContents entfernen.
    iter = tree.depthFirstIterator(false);
    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      if (cmd.isDone() == true
          && cmd instanceof DocumentCommand.ExternalContentInserter)
      {
        cleanEmptyParagraphs(cmd);
      }
    }

    // jetzt soll man wieder was sehen:
    if (document.xModel() != null) document.xModel().unlockControllers();

    // updates der Bookmarks:
    tree.updateBookmarks();

    // Document-Modified auf false setzen, da nur wirkliche
    // Benutzerinteraktionen den Modified-Status beeinflussen sollen.
    try
    {
      document.xModifiable().setModified(false);
    }
    catch (PropertyVetoException x)
    {
      // wenn jemand was dagegen hat, dann setze ich halt nichts.
    }

    // ggf. EndlessLoopException mit dem Namen des Dokuments schmeissen.
    if (loopCount == MAXCOUNT)
    {
      UnoService frame = new UnoService(document.xModel()
          .getCurrentController().getFrame());
      String name;
      try
      {
        name = frame.getPropertyValue("Title").toString();
      }
      catch (Exception e)
      {
        name = "";
      }

      throw new EndlessLoopException(
          "Endlosschleife bei der Textfragment-Ersetzung in Dokument \""
              + name
              + "\"");
    }

    // ggf. eine WMCommandsFailedException werfen:
    if (errorCount != 0)
    {
      throw new WMCommandsFailedException(
          "Bei der Dokumenterzeugung mit dem Briefkopfsystem trat(en) "
              + errorCount
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

  private void cleanEmptyParagraphs(DocumentCommand cmd)
  {
    Logger.debug2("cleanEmptyParagraphs(" + cmd + ")");

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
        && fragStart.xParagraphCursor().isStartOfParagraph())
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
          // enthaltene TextTable nicht mehr umschlie�t.
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
    new FormGUI(formDescriptors, fm, new HashMap(), new FunctionLibrary(),
        new DialogLibrary());
  }

  public int executeCommand(DocumentCommand.InvalidCommand cmd)
  {
    insertErrorField(
        cmd,
        "Dokumentkommando ist ung�ltig: " + cmd.getMessage(),
        null);
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
      insertErrorField(cmd, "", e);
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

      Logger.debug("F�ge Textfragment \""
                   + cmd.getFragID()
                   + "\" von URL \""
                   + urlStr
                   + "\" ein.");

      // Workaround f�r den insertDocumentFromURL-Fehler (Einfrieren von OOo
      // wenn Ressource nicht aufl�sbar).
      if (url.openConnection().getContentLength() <= 0)
      {
        throw new IOException("Fragment "
                              + cmd.getFragID()
                              + " ("
                              + url.toExternalForm()
                              + ") ist leer oder nicht verf�gbar");
      }

      // Textfragment einf�gen:
      UnoService insCursor = new UnoService(cmd.createInsertCursor());
      if (insCursor.xDocumentInsertable() != null)
      {
        insCursor.xDocumentInsertable().insertDocumentFromURL(
            urlStr,
            new PropertyValue[] {});
      }
    }
    catch (java.lang.Exception e)
    {
      insertErrorField(cmd, "", e);
      cmd.setErrorState(true);
      return 1;
    }
    cmd.setDoneState(true);
    return 0;
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

        // Textfragment einf�gen:
        UnoService insCursor = new UnoService(cmd.createInsertCursor());
        if (insCursor.xDocumentInsertable() != null)
          insCursor.xDocumentInsertable().insertDocumentFromURL(
              urlStr,
              new PropertyValue[] {});
      }
      catch (java.lang.Exception e)
      {
        insertErrorField(cmd, "", e);
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
      insertErrorField(cmd, "", e);
      cmd.setErrorState(true);
      return 1;
    }
    cmd.setDoneState(true);
    return 0;
  }

  /**
   * Diese Methode f�gt ein Fehler-Feld an die Stelle des Dokumentkommandos ein.
   */
  private void insertErrorField(DocumentCommand cmd, String text,
      java.lang.Exception e)
  {
    String msg = "Fehler in Dokumentkommando \""
                 + cmd.getBookmarkName()
                 + "\": ";

    // Meldung auch auf dem Logger ausgeben
    if (e != null)
      Logger.error(msg + text, e);
    else
      Logger.error(msg + text);

    // Meldung f�r die mehrzeilige Ausgabe aufbereiten
    text = msg + "\n\n" + text;

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

    // msg += Stacktrace von e
    if (e != null)
    {
      if (e.getMessage() != null)
        text += e.getMessage() + "\n\n" + e.getClass().getName() + ":\n";
      else
        text += e.toString() + "\n\n";
      StackTraceElement[] element = e.getStackTrace();
      for (int i = 0; i < element.length; i++)
      {
        text += element[i].toString() + "\n";
      }
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
      note.setPropertyValue("Content", text);
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
