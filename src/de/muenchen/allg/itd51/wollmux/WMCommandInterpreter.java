/*
 * Dateiname: WMCommandInterpreter.java
 * Projekt  : WollMux
 * Funktion : Scannt alle Bookmarks eines Dokuments und interpretiert ggf. die 
 *            WM-Kommandos.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 14.10.2005 | LUT | Erstellung
 * 24.10.2005 | LUT | + Sauberes umschliessen von Bookmarks in 
 *                      executeInsertFrag.
 *                    + Abschalten der lock-Controllers  
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.beans.PropertyValue;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.io.IOException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;
import de.muenchen.allg.itd51.wollmux.db.Dataset;

/**
 * Diese Klasse repr�sentiert den Kommando-Interpreter zur Auswertung von
 * WollMux-Kommandos in einem gegebenen Textdokument.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class WMCommandInterpreter
{

  /**
   * Das Dokument, das interpretiert werden soll.
   */
  private UnoService document;

  /**
   * Dieses Flag gibt an, ob der WMCommandInterpreter �nderungen am Dokument
   * vornehmen darf. Kommandos, die das Dokument nicht ver�ndern, k�nnen
   * trotzdem ausgef�hrt werden.
   */
  private boolean allowDocumentModification;

  /**
   * Abbruchwert f�r die Anzahl der Interationsschritte beim Auswerten neu
   * hinzugekommener Bookmarks.
   */
  private static final int MAXCOUNT = 100;

  /**
   * Verstecktes Trennzeichen, das zum Beginn und zum Ende eines Textfragments
   * eingef�gt wird um verschachtelte WollMux-Kommandos zu erm�glichen.
   */
  private static final String FRAGMENT_MARK_OPEN = "<..";

  private static final String FRAGMENT_MARK_CLOSE = "..>";

  /**
   * Der Konstruktor erzeugt einen neuen Kommandointerpreter, der alle Kommandos
   * im �bergebenen xDoc scannen und entsprechend aufl�sen kann.
   * 
   * @param xDoc
   */
  public WMCommandInterpreter(XTextDocument xDoc)
  {
    this.document = new UnoService(xDoc);

    // Wenn das Dokument als Dokument (und nicht als Template) ge�ffnet wurde,
    // soll das Dokument nicht vom WollMux ver�ndert werden:
    allowDocumentModification = (xDoc.getURL() == null || xDoc.getURL().equals(
        ""));
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
   */
  public void interpret() throws EndlessLoopException
  {
    // Die Sichtbare Darstellung in OOo abschalten:
    // document.xModel().lockControllers();
    // Wurde auskommentiert, da der Aufbau zu langsam ist und die arbeitsweise
    // transparenter ist, wenn der Benutzer sieht dass sich was tut...

    // Bereits abgearbeitete Bookmarks merken.
    HashMap evaluatedBookmarks = new HashMap();

    // Folgendes Pattern pr�ft ob es sich bei dem Bookmark um ein g�ltiges
    // WM-Kommando handelt und entfernt evtl. vorhandene Zahlen-Postfixe.
    Pattern wmCmd = Pattern
        .compile("\\A\\p{Space}*(WM\\p{Space}*\\(.*\\))\\p{Space}*(\\d*)\\z");

    // Solange durch die st�ndig neu erzeugte Liste aller Bookmarks gehen, bis
    // alle Bookmarks ausgewertet wurden oder die Abbruchbedingung zur
    // Vermeindung von Endlosschleifen erf�llt ist.
    boolean changed = true;
    int count = 0;
    while (changed && MAXCOUNT > ++count)
    {
      changed = false;
      XNameAccess bookmarkAccess = document.xBookmarksSupplier().getBookmarks();
      String[] bookmarks = bookmarkAccess.getElementNames();

      // Alle Bookmarks durchlaufen und ggf. die execute-Routine aufrufen.
      for (int i = 0; i < bookmarks.length; i++)
      {
        if (!evaluatedBookmarks.containsKey(bookmarks[i]))
        {
          String bookmarkName = bookmarks[i];

          Logger.debug2("Evaluate Bookmark \"" + bookmarkName + "\".");
          changed = true;
          evaluatedBookmarks.put(bookmarkName, Boolean.TRUE);

          // Bookmark evaluieren.
          Matcher m = wmCmd.matcher(bookmarkName);
          if (m.find())
          {
            Logger.debug2("Found WM-Command: " + m.group(1));
            String newBookmarkName = execute(m.group(1), bookmarkName, m
                .group(2));

            // Wenn sich der Bookmark ge�ndert hat, muss evaluatedBookmarks
            // angepasst werden:
            if (!bookmarkName.equals(newBookmarkName))
            {
              evaluatedBookmarks.put(newBookmarkName, evaluatedBookmarks
                  .remove(bookmarkName));
            }
          }
          else
          {
            Logger.debug2("Normales Bookmark gefunden, kein WM-Kommando.");
          }
        }
      }
    }
    if (count == MAXCOUNT)
    {
      // EndlessLoopException mit dem Namen des Dokuments schmeissen.
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

    // Lock-Controllers wieder aufheben:
    // document.xModel().unlockControllers();
  }

  /**
   * Generisches Execute eines Wollmux-Kommandos.
   * 
   * @param cmdString
   *          Das WollMux-Kommando, das in dem Bookmark enthalten ist.
   * @param bookmarkName
   *          Der Name des zugeh�rigen Bookmarks f�r die weitere Verarbeitung.
   *          Im Fehlerfall wird auf den Bookmarknamen verwiesen.
   */
  private String execute(String cmdString, String bookmarkName, String suffix)
  {
    try
    {
      ConfigThingy wm = new ConfigThingy("", WollMux.getDEFAULT_CONTEXT(),
          new StringReader(cmdString));
      ConfigThingy cmd = wm.get("CMD");
      WMCommandState state = new WMCommandState(wm);

      if (state.isDone() == false || state.getErrors() > 0)
      {

        // insertFrag
        if (cmd.toString().equals("insertFrag"))
        {
          Logger.debug2("Cmd: insertFrag mit FRAG_ID \""
                        + wm.get("FRAG_ID").toString()
                        + "\"");

          state = executeInsertFrag(
              wm.get("FRAG_ID").toString(),
              bookmarkName,
              state);
        }

        // insertValue
        else if (cmd.toString().equals("insertValue"))
        {
          Logger.debug2("Cmd: insertValue mit DB_SPALTE \""
                        + wm.get("DB_SPALTE").toString()
                        + "\"");
          state = executeInsertValue(
              wm.get("DB_SPALTE").toString(),
              bookmarkName,
              state);
        }

        // unbekanntes Kommando
        else
        {
          String msg = bookmarkName
                       + ": "
                       + "Unbekanntes WollMux-Kommando \""
                       + cmd.toString()
                       + "\"";
          Logger.error(msg);
          state.setErrors(1);
          fillBookmark(bookmarkName, msg);
        }
      }

      // Neuen Status rausschreiben:
      ConfigThingy wmCmd = state.toConfigThingy();
      String wmCmdString = wmCmd.stringRepresentation(true, '\'') + suffix;
      wmCmdString = wmCmdString.replaceAll("[\r\n]+", " ");
      Logger.debug2("EXECUTE STATE: " + wmCmdString);

      return renameBookmark(bookmarkName, wmCmdString);
    }
    catch (java.lang.Exception e)
    {
      Logger.error("Bookmark \"" + bookmarkName + "\":");
      Logger.error(e);
      fillBookmark(bookmarkName, bookmarkName + ": " + e.toString());
    }
    return bookmarkName;
  }

  /**
   * Diese Methode f�gt einen Spaltenwert aus dem aktuellen Datensatz ein. Im
   * Fehlerfall wird die Fehlermeldung eingef�gt.
   * 
   * @param spaltenname
   *          Name der Datenbankspalte
   * @param bookmarkName
   *          Name des Bookmarks in das der Wert eingef�gt werden soll.
   */
  private WMCommandState executeInsertValue(String spaltenname,
      String bookmarkName, WMCommandState state)
  {
    state.setErrors(0);
    try
    {
      Dataset ds = WollMux.getDatasourceJoiner().getSelectedDataset();
      if (ds.get(spaltenname) == null)
        fillBookmark(bookmarkName, "");
      else
        fillBookmark(bookmarkName, ds.get(spaltenname));
      state.setDone(true);
    }
    catch (java.lang.Exception e)
    {
      Logger.error("Bookmark \"" + bookmarkName + "\":");
      Logger.error(e);
      fillBookmark(bookmarkName, bookmarkName + ": " + e.toString());
      state.setErrors(state.getErrors() + 1);
    }
    state.setDone(true);
    return state;
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
  private WMCommandState executeInsertFrag(String frag_id, String bookmarkName,
      WMCommandState state)
  {
    state.setErrors(0);
    try
    {
      // Fragment-URL holen und aufbereiten. Kontext ist der DEFAULT_CONTEXT.
      String urlStr = WollMux.getTextFragmentList().getURLByID(frag_id);
      URL url = new URL(WollMux.getDEFAULT_CONTEXT(), urlStr);
      UnoService trans = UnoService.createWithContext(
          "com.sun.star.util.URLTransformer",
          WollMux.getXComponentContext());
      com.sun.star.util.URL[] unoURL = new com.sun.star.util.URL[] { new com.sun.star.util.URL() };
      unoURL[0].Complete = url.toExternalForm();
      trans.xURLTransformer().parseStrict(unoURL);
      urlStr = unoURL[0].Complete;

      Logger.debug("F�ge Textfragment \""
                   + frag_id
                   + "\" von URL \""
                   + urlStr
                   + "\" ein.");

      // Workaround f�r den insertDocumentFromURL-Fehler (Einfrieren von OOo
      // wenn Ressource nicht aufl�sbar).
      if (url.openConnection().getContentLength() <= 0)
      {
        throw new IOException("Fragment "
                              + frag_id
                              + " ("
                              + url.toExternalForm()
                              + ") ist leer oder nicht verf�gbar");
      }

      // Dokument einf�gen
      XTextRange bookmarkCursor = insertDocumentWithMarks(bookmarkName, urlStr);

      // Bookmark an neuen Range anpassen
      rerangeBookmark(bookmarkName, bookmarkCursor);

    }
    catch (java.lang.Exception e)
    {
      Logger.error("Bookmark \"" + bookmarkName + "\":");
      Logger.error(e);
      fillBookmark(bookmarkName, bookmarkName + ": " + e.toString());
      state.setErrors(state.getErrors() + 1);
    }
    state.setDone(true);
    return state;
  }

  /**
   * Diese private Methode f�gt ein Document an die Stelle von bookmarkName ein.
   * Der eingef�gte Inhalt wird von unsichtbaren FRAGMENT_MARKen umgeben um ein
   * verschachteltes Einf�gen von Inhalten zu erm�glichen. Das Bookmark wird
   * nicht automatisch verg��ert.
   * 
   * @param bookmarkName
   *          der Name des Bookmarks an dessen Stelle das Dokument eingef�gt
   *          werden soll.
   * @param unoURL
   *          die bereits f�r uno aufbereitete unoURL
   * @return
   * @throws NoSuchElementException
   * @throws WrappedTargetException
   * @throws Exception
   */
  private XTextRange insertDocumentWithMarks(String bookmarkName, String unoURL)
      throws NoSuchElementException, WrappedTargetException, Exception
  {
    if (allowDocumentModification)
    {
      // so gehts:
      // "x" ist die explizit sichtbare FRAGMENT_MARK, die sp�ter auf
      // Hidden ("h") gesetzt wird.
      // 1) bookmarkCursor = "xx"
      // 2) insertCursor exakt in die Mitte der xx setzen.
      // 3) Inhalt aus Fragmentdatei einf�gen in insCursor
      // 4) alle "x" auf "h" setzen
      // Ergebnis: bookmarCursor = "h<inhalt>h"

      // TextCursor erzeugen, der den gesamten Ersetzungsbereich des Bookmarks
      // umschlie�t und mit dem Inhalt der beiden FRAGMENT_MARKs vorbelegen.
      XNameAccess bookmarkAccess = document.xBookmarksSupplier().getBookmarks();
      UnoService bookmark = new UnoService(bookmarkAccess
          .getByName(bookmarkName));
      UnoService text = new UnoService(document.xTextDocument().getText());
      UnoService bookmarkCursor = new UnoService(text.xText()
          .createTextCursorByRange(bookmark.xTextContent().getAnchor()));
      bookmarkCursor.xTextCursor().setString(
          FRAGMENT_MARK_OPEN + FRAGMENT_MARK_CLOSE);
      bookmarkCursor.setPropertyValue("CharHidden", Boolean.FALSE);

      // InsertCurser erzeugen, in den das Textfragment eingef�gt wird.
      UnoService insCursor = new UnoService(text.xText()
          .createTextCursorByRange(bookmarkCursor.xTextCursor()));
      insCursor.xTextCursor().goRight(
          (short) FRAGMENT_MARK_OPEN.length(),
          false);
      insCursor.xTextCursor().collapseToStart();

      try
      {
        // Textfragment einf�gen:
        PropertyValue[] props = new PropertyValue[] { new PropertyValue() };
        insCursor.xDocumentInsertable().insertDocumentFromURL(unoURL, props);
      }
      catch (java.lang.Exception e)
      {
        Logger.error("Bookmark \"" + bookmarkName + "\":");
        Logger.error(e);
        insCursor.xTextCursor().setString(bookmarkName + ": " + e.toString());
      }

      // FRAGMENT_MARKen verstecken:
      UnoService hiddenCursor = new UnoService(text.xText().createTextCursor());
      // start-Marke
      hiddenCursor.xTextCursor().gotoRange(
          bookmarkCursor.xTextRange().getStart(),
          false);
      hiddenCursor.xTextCursor().goRight(
          (short) FRAGMENT_MARK_OPEN.length(),
          true);
      hiddenCursor.setPropertyValue("CharHidden", Boolean.TRUE);
      // end-Marke
      hiddenCursor.xTextCursor().gotoRange(
          bookmarkCursor.xTextRange().getEnd(),
          false);
      hiddenCursor.xTextCursor().goLeft(
          (short) FRAGMENT_MARK_CLOSE.length(),
          true);
      hiddenCursor.setPropertyValue("CharHidden", Boolean.TRUE);

      // das war's
      return bookmarkCursor.xTextRange();
    }
    return null;
  }

  /**
   * Diese Methode ordnet dem Bookmark bookmarkName eine neue Range xTextRang
   * zu. Damit kann z.B. ein Bookmark ohne Ausdehnung eine Ausdehnung xTextRange
   * erhalten.
   * 
   * @param bookmarkName
   * @param xTextRange
   * @throws WrappedTargetException
   * @throws NoSuchElementException
   */
  private void rerangeBookmark(String bookmarkName, XTextRange xTextRange)
      throws NoSuchElementException, WrappedTargetException
  {
    if (allowDocumentModification)
    {
      // Ein Bookmark ohne Ausdehnung kann nicht einfach nachtr�glich
      // erweitert werden. Dies geht nur beim Erzeugen und Einf�gen mit
      // insertTextContent(...). Um ein solches Bookmark mit einer
      // Ausdehnung versehen zu k�nnen, muss es zu erst gel�scht, und
      // anschlie�end wieder neu erzeugt und mit der Ausdehnung xTextRange
      // eingef�gt werden.

      // altes Bookmark l�schen.
      UnoService oldBookmark = new UnoService(document.xBookmarksSupplier()
          .getBookmarks().getByName(bookmarkName));

      document.xTextDocument().getText().removeTextContent(
          oldBookmark.xTextContent());

      // neuen Bookmark unter dem alten Namen mit Ausdehnung hinzuf�gen.
      UnoService newBookmark;
      try
      {
        newBookmark = document.create("com.sun.star.text.Bookmark");
        newBookmark.xNamed().setName(bookmarkName);
        document.xTextDocument().getText().insertTextContent(
            xTextRange,
            newBookmark.xTextContent(),
            true);
      }
      catch (Exception e)
      {
        // Fehler beim Erzeugen des Service Bookmark. Sollt normal nicht
        // passieren.
        Logger.error(e);
        Logger.error("ReRange: Bookmark \""
                     + bookmarkName
                     + "\" konnte nicht neu erzeugt werden und ging "
                     + "verloren.");
      }
    }
  }

  /**
   * Diese Methode benennt das Bookmark oldName zu dem Namen newName um. Ist der
   * Name bereits definiert, so h�ngt OpenOffice an den Namen automatisch eine
   * Nummer an. Die Methode gibt den tats�chlich erzeugten Bookmarknamen zur�ck.
   * 
   * @param oldName
   * @param newName
   * @return den tats�chlich erzeugten Namen des Bookmarks.
   */
  private String renameBookmark(String oldName, String newName)
  {
    if (allowDocumentModification)
    {
      try
      {
        // altes Bookmark holen.
        UnoService oldBookmark = new UnoService(document.xBookmarksSupplier()
            .getBookmarks().getByName(oldName));

        // Bereich merken:
        UnoService text = new UnoService(document.xTextDocument().getText());
        UnoService bookmarkCursor = new UnoService(text.xText()
            .createTextCursorByRange(oldBookmark.xTextContent().getAnchor()));

        // altes Bookmark l�schen.
        document.xTextDocument().getText().removeTextContent(
            oldBookmark.xTextContent());

        // neues Bookmark hinzuf�gen.
        UnoService newBookmark;
        newBookmark = document.create("com.sun.star.text.Bookmark");
        newBookmark.xNamed().setName(newName);
        document.xTextDocument().getText().insertTextContent(
            bookmarkCursor.xTextRange(),
            newBookmark.xTextContent(),
            true);

        return newBookmark.xNamed().getName();
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }
    return oldName;
  }

  /**
   * Diese Methode f�llt ein Bookmark bookmarkName mit einem �bergebenen Text
   * text. Das Bookmark umschlie�t diesen Text hinterher vollst�ndig.
   * 
   * @param bookmarkName
   * @param text
   */
  private void fillBookmark(String bookmarkName, String text)
  {
    if (allowDocumentModification)
    {
      XNameAccess bookmarkAccess = document.xBookmarksSupplier().getBookmarks();
      try
      {

        // Textcursor erzeugen und mit dem neuen Text ausdehnen.
        UnoService bookmark = new UnoService(bookmarkAccess
            .getByName(bookmarkName));
        UnoService cursor = new UnoService(document.xTextDocument().getText()
            .createTextCursorByRange(bookmark.xTextContent().getAnchor()));
        cursor.xTextCursor().setString(text);

        // Bookmark an neuen Range anpassen
        rerangeBookmark(bookmarkName, cursor.xTextRange());

      }
      catch (NoSuchElementException e)
      {
        // Dieser Fall kann normalerweise nicht auftreten, da nur Bookmarks
        // verarbeitet werden, die auch wirklich existieren.
        Logger.error(e);
      }
      catch (WrappedTargetException e)
      {
        // interner UNO-Fehler beim Holen des Bookmarks. Sollte
        // normalerweise nicht auftreten.
        Logger.error(e);
      }
    }
  }

  /**
   * Methode zum Testen des WMCommandoInterpreters.
   * 
   * @param args
   */
  public static void main(String[] args)
  {
    try
    {
      if (args.length < 3)
      {
        System.out.println("USAGE: <config_url> <los_cache> <document_url>");
        System.exit(0);
      }
      File cwd = new File("testdata");

      args[0] = args[0].replaceAll("\\\\", "/");
      args[1] = args[1].replaceAll("\\\\", "/");

      // Remote-Kontext herstellen
      UNO.init();

      // WollMux starten
      new WollMux(UNO.defaultContext);
      WollMux.initialize(System.err, new File(cwd, args[0]), new File(cwd,
          args[1]), cwd.toURL());
      WollMux.startupWollMux();

      Logger.init(Logger.ALL);

      // Dokument URL aufbereiten und Dokument zum Parsen �ffnen
      String urlStr = new URL(cwd.toURL(), args[2]).toExternalForm();
      UnoService trans = UnoService.createWithContext(
          "com.sun.star.util.URLTransformer",
          WollMux.getXComponentContext());
      com.sun.star.util.URL[] unoURL = new com.sun.star.util.URL[] { new com.sun.star.util.URL() };
      unoURL[0].Complete = urlStr;
      trans.xURLTransformer().parseStrict(unoURL);
      urlStr = unoURL[0].Complete;

      UNO.loadComponentFromURL(urlStr, true, false);

      // new WMCommandInterpreter(UNO.XTextDocument(UNO.compo)).interpret();
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
    // System.exit(0);
  }
}
