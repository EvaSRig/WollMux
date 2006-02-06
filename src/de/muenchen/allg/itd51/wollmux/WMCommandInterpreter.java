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

import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.awt.FontWeight;
import com.sun.star.awt.PosSize;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.TextContentAnchorType;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;
import com.sun.star.util.CloseVetoException;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.dialog.FormGUI;

/**
 * Diese Klasse repr�sentiert den Kommando-Interpreter zur Auswertung von
 * WollMux-Kommandos in einem gegebenen Textdokument.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class WMCommandInterpreter
{

  private WollMuxSingleton mux;

  /**
   * Das Dokument, das interpretiert werden soll.
   */
  private UnoService document;

  /**
   * Z�hlt die Anzahl der w�hrend des interpret()-Vorgangs auftretenden Fehler.
   */
  private int errorFieldCount;

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
  public WMCommandInterpreter(XTextDocument xDoc, WollMuxSingleton mux)
  {
    this.document = new UnoService(xDoc);
    this.mux = mux;
    this.formDescriptors = new ConfigThingy("Forms");
    this.documentIsAFormular = false;

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
   * @throws WMCommandsFailedException
   */
  public void interpret() throws EndlessLoopException,
      WMCommandsFailedException
  {
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
    errorFieldCount = 0;
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
    if (count == MAXCOUNT)
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
    if (errorFieldCount != 0)
    {
      throw new WMCommandsFailedException(
          "Bei der Dokumenterzeugung mit dem Briefkopfsystem trat(en) "
              + errorFieldCount
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
   * Diese Methode startet die FormularGUI. Sie enth�lt vorerst nur eine
   * dummy-implementierung, bei der auch noch keine im Dokument gesetzten
   * Feldinhalte ausgewertet werden. D.h. die GUI startet immer mit einer leeren
   * IdToPreset-Map.
   */
  private void startFormGUI()
  {
    FormModel fm = new FormModel()
    {

      public void close()
      {
        try
        {
          document.xCloseable().close(false);
        }
        catch (CloseVetoException e)
        {
          Logger.error(e);
        }
      }

      public void setWindowVisible(boolean vis)
      {
        UnoService frame = new UnoService(document.xModel()
            .getCurrentController().getFrame());
        if (frame.xFrame() != null)
        {
          frame.xFrame().getContainerWindow().setVisible(vis);
        }
      }

      public void setWindowPosSize(int docX, int docY, int docWidth,
          int docHeight)
      {
        UnoService frame = new UnoService(document.xModel()
            .getCurrentController().getFrame());
        if (frame.xFrame() != null)
        {
          frame.xFrame().getContainerWindow().setPosSize(
              docX,
              docY,
              docWidth,
              docHeight,
              PosSize.POSSIZE);
        }
      }

    };
    new FormGUI(formDescriptors, fm, new HashMap());
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
      ConfigThingy wm = new ConfigThingy("", null, new StringReader(cmdString));
      ConfigThingy cmd = wm.get("CMD");
      WMCommandState state = new WMCommandState(wm);

      if (state.isDone() == false || state.getErrors() > 0)
      {

        // insertFrag
        if (cmd.toString().compareToIgnoreCase("insertFrag") == 0)
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
        else if (cmd.toString().compareToIgnoreCase("insertValue") == 0)
        {
          Logger.debug2("Cmd: insertValue mit DB_SPALTE \""
                        + wm.get("DB_SPALTE").toString()
                        + "\"");
          state = executeInsertValue(
              wm.get("DB_SPALTE").toString(),
              bookmarkName,
              wm,
              state);
        }

        // rufe den wollmux
        else if (cmd.toString().compareToIgnoreCase("RufeDenWollMux") == 0)
        {
          Logger.debug2("Cmd: RufeDenWollMux");
          state = executeRufeDenWollMux(bookmarkName, state);
        }

        // version
        else if (cmd.toString().compareToIgnoreCase("Version") == 0)
        {
          Logger.debug2("Cmd: Version");
          state = executeVersion(bookmarkName, state);
        }

        // form
        else if (cmd.toString().compareToIgnoreCase("Form") == 0)
        {
          Logger.debug2("Cmd: Form");
          state = executeForm(bookmarkName, state);
        }

        // unbekanntes Kommando
        else
        {
          String msg = "Unbekanntes WollMux-Kommando \""
                       + cmd.toString()
                       + "\"";
          Logger.error(msg);
          state.setErrors(1);
          insertErrorField(bookmarkName, msg, null);
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
      insertErrorField(bookmarkName, "", e);
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
      String bookmarkName, ConfigThingy wm, WMCommandState state)
  {
    state.setErrors(0);
    try
    {
      Dataset ds = mux.getDatasourceJoiner().getSelectedDataset();
      if (ds.get(spaltenname) == null || ds.get(spaltenname).equals(""))
        fillBookmark(bookmarkName, "");
      else
      {
        // Auswertung der AUTOSEP und SEPARATOR - Attribute
        String[] seps = getSeparators(wm);

        // Bookmark bef�llen
        fillBookmark(bookmarkName, seps[0] + ds.get(spaltenname) + seps[1]);
      }
      state.setDone(true);
    }
    catch (java.lang.Exception e)
    {
      Logger.error("Bookmark \"" + bookmarkName + "\":");
      Logger.error(e);
      insertErrorField(bookmarkName, "", e);
      state.setErrors(state.getErrors() + 1);
    }
    state.setDone(true);
    return state;
  }

  /**
   * Evaluiert die WM-Kommando-Attribute AUTOSEP und SEPARATOR und liefert ein
   * Array mit den String-Werten des linken (Index 0) und rechten (index 1)
   * Separators.
   * 
   * @param wmCmd
   *          WollMux-Kommando das optional die Attribute AUTOSEP und SEPARATOR
   *          enth�lt.
   * @return Array mit den String-Werten des linken (Index 0) und rechten (index
   *         1) Separators.
   * @throws java.lang.Exception
   *           Die Exception wird geworfen, wenn ein falscher AUTOSEP-Typ
   *           angegeben wurde.
   */
  private String[] getSeparators(ConfigThingy wmCmd) throws java.lang.Exception
  {
    String[] resultSeps = new String[] { /* left = */"", /* right = */"" };
    Iterator autoseps = wmCmd.query("AUTOSEP").iterator();
    Iterator seps = wmCmd.query("SEPARATOR").iterator();
    String currentSep = " "; // mit Default-Separator vorbelegt

    while (autoseps.hasNext())
    {
      ConfigThingy as = (ConfigThingy) autoseps.next();
      String sep = currentSep;
      if (seps.hasNext()) sep = ((ConfigThingy) seps.next()).toString();

      if (as.toString().compareToIgnoreCase("left") == 0)
      {
        resultSeps[0] = sep;
      }
      else if (as.toString().compareToIgnoreCase("right") == 0)
      {
        resultSeps[1] = sep;
      }
      else if (as.toString().compareToIgnoreCase("both") == 0)
      {
        resultSeps[0] = sep;
        resultSeps[1] = sep;
      }
      else
      {
        throw new java.lang.Exception(
            "Unbekannter AUTOSEP-typ \""
                + as.toString()
                + "\". Erwarte \"left\", \"right\" oder \"both\".");
      }

      currentSep = sep;
    }

    return resultSeps;
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
      String urlStr = mux.getTextFragmentList().getURLByID(frag_id);
      URL url = new URL(mux.getDEFAULT_CONTEXT(), urlStr);
      UnoService trans = UnoService.createWithContext(
          "com.sun.star.util.URLTransformer",
          mux.getXComponentContext());
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
      insertErrorField(bookmarkName, "", e);
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
      throws NoSuchElementException, WrappedTargetException/* , Exception */
  {
    if (allowDocumentModification)
    {

      // WMInsertField und insCursor erzeugen:
      XNameAccess bookmarkAccess = document.xBookmarksSupplier().getBookmarks();
      UnoService bookmark = new UnoService(bookmarkAccess
          .getByName(bookmarkName));
      WMInsertField insertField = new WMInsertField(createTextCursorByBookmark(
          bookmark).xTextCursor());
      UnoService insCursor = insertField.createInsertCursor();

      try
      {
        // Textfragment einf�gen:
        insCursor.xDocumentInsertable().insertDocumentFromURL(
            unoURL,
            new PropertyValue[] {});
      }
      catch (java.lang.Exception e)
      {
        Logger.error("Bookmark \"" + bookmarkName + "\":");
        Logger.error(e);
        insertErrorField(insCursor.xTextCursor(), "Bookmark \""
                                                  + bookmarkName
                                                  + "\":\n\n", e);
      }

      // Textmarken verstecken:
      insertField.hideMarks();

      return insertField.getTextRange();
    }
    return null;
  }

  /**
   * Gibt Informationen �ber die aktuelle Install-Version des WollMux aus.
   * 
   * @param bookmarkName
   *          Name des Bookmarks in das der Wert eingef�gt werden soll.
   */
  private WMCommandState executeVersion(String bookmarkName,
      WMCommandState state)
  {
    if (allowDocumentModification)
    {
      state.setErrors(0);
      XNameAccess bookmarkAccess = document.xBookmarksSupplier().getBookmarks();
      try
      {
        UnoService bookmark = new UnoService(bookmarkAccess
            .getByName(bookmarkName));
        WMInsertField insertField = new WMInsertField(
            createTextCursorByBookmark(bookmark).xTextRange());
        UnoService insCurs = insertField.createInsertCursor();
        insCurs.xTextCursor().setString("Build-Info: " + mux.getBuildInfo());

        insertField.hideMarks();
      }
      catch (Exception x)
      {
        state.setErrors(1);
        Logger.error(x);
      }
    }
    state.setDone(true);
    return state;
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
  private WMCommandState executeForm(String bookmarkName, WMCommandState state)
  {
    if (allowDocumentModification)
    {
      state.setErrors(0);
      XNameAccess bookmarkAccess = document.xBookmarksSupplier().getBookmarks();
      try
      {
        UnoService bookmark = new UnoService(bookmarkAccess
            .getByName(bookmarkName));
        UnoService cursor = createTextCursorByBookmark(bookmark);
        UnoService textfield = cursor.getPropertyValue("TextField");
        Object content = textfield.getPropertyValue("Content").getObject();
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
      catch (java.lang.Exception x)
      {
        state.setErrors(1);
        Logger.error(x);
      }
    }
    state.setDone(true);
    return state;
  }

  /**
   * Easteregg, ausgel�st durch den Befehl WM(CMD'rufeDenWollMux').
   * 
   * @param bookmarkName
   *          Name des Bookmarks in das der Wert eingef�gt werden soll.
   */
  private WMCommandState executeRufeDenWollMux(String bookmarkName,
      WMCommandState state)
  {
    if (allowDocumentModification)
    {
      state.setErrors(0);
      XNameAccess bookmarkAccess = document.xBookmarksSupplier().getBookmarks();
      try
      {
        UnoService bookmark = new UnoService(bookmarkAccess
            .getByName(bookmarkName));
        WMInsertField insertField = new WMInsertField(
            createTextCursorByBookmark(bookmark).xTextRange());
        UnoService insCurs = insertField.createInsertCursor();
        insCurs.xTextCursor().getText().insertString(
            insCurs.xTextRange().getEnd(),
            "... und er kommt!\n\n",
            false);

        // WollMux-Bild einf�gen:
        UnoService bild = null;
        for (short i = 7; i < 42; i += 1)
        {
          bild = document.create("com.sun.star.text.GraphicObject");
          bild.setPropertyValue(
              "GraphicURL",
              "http://limux.tvc.muenchen.de/wiki/images/4/4a/Wollmux.jpg");
          bild.setPropertyValue(
              "AnchorType",
              TextContentAnchorType.AS_CHARACTER);
          bild.setPropertyValue("RelativeHeight", new Short(i));
          bild.setPropertyValue("RelativeWidth", new Short(i));
          bild.setPropertyValue("LeftMargin", new Integer(i * i * i / 4));
          insertTextContent(
              insCurs.xTextRange().getEnd(),
              bild.xTextContent(),
              false);
          Integer waiter = new Integer(70 - i);
          synchronized (waiter)
          {
            try
            {
              waiter.wait(waiter.intValue());
            }
            catch (InterruptedException e)
            {
            }
          }
          insCurs.xTextCursor().getText()
              .removeTextContent(bild.xTextContent());
        }
        insCurs
            .xTextCursor()
            .getText()
            .insertString(
                insCurs.xTextRange(),
                "\nAber er gehorcht nur BNK, LUT und anderen eingeweihten Personen...",
                false);

        insertField.hideMarks();
      }
      catch (Exception x)
      {
        state.setErrors(1);
        Logger.error(x);
      }
    }
    state.setDone(true);
    return state;
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
      removeTextContent(oldBookmark.xTextContent());

      // neuen Bookmark unter dem alten Namen mit Ausdehnung hinzuf�gen.
      UnoService newBookmark;
      try
      {
        newBookmark = document.create("com.sun.star.text.Bookmark");
        newBookmark.xNamed().setName(bookmarkName);
        insertTextContent(xTextRange, newBookmark.xTextContent(), true);
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
        UnoService bookmarkCursor = createTextCursorByRange(oldBookmark
            .xTextContent().getAnchor());

        // altes Bookmark l�schen.
        removeTextContent(oldBookmark.xTextContent());

        // neues Bookmark hinzuf�gen.
        UnoService newBookmark;
        newBookmark = document.create("com.sun.star.text.Bookmark");
        newBookmark.xNamed().setName(newName);
        insertTextContent(bookmarkCursor.xTextRange(), newBookmark
            .xTextContent(), true);

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
        WMInsertField insertField = new WMInsertField(
            createTextCursorByBookmark(bookmark).xTextRange());
        insertField.createInsertCursor().xTextCursor().setString(text);

        // Markers verstecken:
        insertField.hideMarks();

        // Bookmark an neuen Range anpassen
        rerangeBookmark(bookmarkName, insertField.getTextRange());

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
   * Diese Methode f�gt ein Fehler-Feld an die Stelle des Bookmarks.
   * 
   * @param bookmarkName
   * @param text
   */
  private void insertErrorField(String bookmarkName, String text,
      java.lang.Exception e)
  {
    if (allowDocumentModification)
    {
      String msg = "Fehler in Bookmark \"" + bookmarkName + "\":\n\n" + text;
      try
      {
        XNameAccess bookmarkAccess = document.xBookmarksSupplier()
            .getBookmarks();

        // Textcursor erzeugen und mit dem neuen Text ausdehnen.
        UnoService bookmark = new UnoService(bookmarkAccess
            .getByName(bookmarkName));
        XTextRange range = insertErrorField(
            bookmark.xTextContent().getAnchor(),
            msg,
            e);

        // Bookmark an neuen Range anpassen
        if (range != null) rerangeBookmark(bookmarkName, range);
      }
      catch (NoSuchElementException x)
      {
        // Dieser Fall kann normalerweise nicht auftreten, da nur Bookmarks
        // verarbeitet werden, die auch wirklich existieren.
        Logger.error(x);
      }
      catch (WrappedTargetException x)
      {
        // interner UNO-Fehler beim Holen des Bookmarks. Sollte
        // normalerweise nicht auftreten.
        Logger.error(x);
      }
    }
  }

  private XTextRange insertErrorField(XTextRange range, String text,
      java.lang.Exception e)
  {
    errorFieldCount++;

    UnoService cursor = createTextCursorByRange(range);
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
      UnoService c = createTextCursorByRange(cursor.xTextCursor().getEnd());
      c.xTextCursor().goLeft((short) 2, false);
      UnoService note = document
          .create("com.sun.star.text.TextField.Annotation");
      note.setPropertyValue("Content", text);
      insertTextContent(c.xTextRange(), note.xTextContent(), false);
    }
    catch (java.lang.Exception x)
    {
      Logger.error(x);
    }
    return cursor.xTextRange();
  }

  private void insertTextContent(XTextRange xTextRange, XTextContent content,
      boolean absorb) throws IllegalArgumentException
  {
    xTextRange.getText().insertTextContent(xTextRange, content, absorb);
  }

  private void removeTextContent(XTextContent content)
      throws NoSuchElementException
  {
    content.getAnchor().getText().removeTextContent(content);
  }

  private UnoService createTextCursorByRange(XTextRange xTextRange)
  {
    return new UnoService(xTextRange.getText().createTextCursorByRange(
        xTextRange));
  }

  /**
   * Erzeugt einen textCursor an der Stelle des Bookmarks bookmarkName.
   * 
   * @param bookmarkName
   * @return
   * @throws NoSuchElementException
   * @throws WrappedTargetException
   */
  private UnoService createTextCursorByBookmark(UnoService bookmark)
      throws NoSuchElementException, WrappedTargetException
  {
    return createTextCursorByRange(bookmark.xTextContent().getAnchor());
  }
}
