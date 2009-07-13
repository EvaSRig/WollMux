/*
 * Dateiname: DocumentCommand.java
 * Projekt  : WollMux
 * Funktion : Beschreibt ein Dokumentkommando mit allen zugeh�rigen Eigenschaften.
 * 
 * Copyright (c) 2009 Landeshauptstadt M�nchen
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
 * 07.11.2005 | LUT | Erstellung als WMCommandState
 * 23.01.2006 | LUT | Erweiterung zum hierarchischen WM-Kommando
 * 26.04.2006 | LUT | Komplette �berarbeitung und Umbenennung in DocumentCommand
 * 17.05.2006 | LUT | Doku �berarbeitet
 * 13.11.2006 | BAB | Erweitern von 'insertFrag' um optionale Argumente 'ARGS'
 * 08.07.2009 | BED | getTextRange() aus Interface OptionalHighlightColorProvider entfernt
 *                  | getTextRange() in getTextCursor() umgearbeitet
 *                  | -createInsertCursor(boolean)
 *                  | +getTextCursorWithinInsertMarks()
 *                  | +setTextRangeString(String)
 *                  | +insertTextContentIntoBookmark(XTextContent, boolean)
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;

/**
 * Beschreibt ein Dokumentkommando mit allen zugeh�rigen Eigenschaften wie z.B. die
 * Gruppenzugeh�rigkeit, Sichtbarkeit und Ausf�hrstatus.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
abstract public class DocumentCommand
{
  /**
   * Das geparste ConfigThingy des zugeh�renden Bookmarks.
   */
  protected ConfigThingy wmCmd;

  /**
   * Das zu diesem DokumentCommand geh�rende Bookmark.
   */
  private Bookmark bookmark;

  // Status-Attribute:

  /**
   * Vorbelegung f�r den Status DONE.
   */
  private static final Boolean STATE_DEFAULT_DONE = Boolean.FALSE;

  /**
   * Vorbelegung f�r den Status ERROR
   */
  private static final Boolean STATE_DEFAULT_ERROR = Boolean.FALSE;

  /**
   * Enth�lt den aktuellen DONE-Status oder null, falls der Wert nicht ver�ndert
   * wurde.
   */
  private Boolean done;

  /**
   * Enth�lt den aktuellen EROOR-Status oder null, falls der Wert nicht ver�ndert
   * wurde.
   */
  private Boolean error;

  /**
   * Das Attribut visible gibt an, ob der Textinhalt des Kommandos sichtbar oder
   * ausgeblendet ist. Es wird nicht persistent im bookmark gespeichert.
   */
  private boolean visible = true;

  // Einf�gemarken und Status f�r InsertCursor zum sicheren Einf�gen von
  // Inhalten

  private boolean hasInsertMarks;

  private static final String INSERT_MARK_OPEN = "<";

  private static final String INSERT_MARK_CLOSE = ">";

  /* ************************************************************ */

  /**
   * Der Konstruktor liefert eine Instanz eines DocumentCommand an der Stelle des
   * Bookmarks bookmark mit dem geparsten Kommando wmCmd.
   * 
   * @param wmCmd
   *          das geparste WM-Kommando
   * @param bookmark
   *          das zugeh�rige Bookmark
   */
  private DocumentCommand(ConfigThingy wmCmd, Bookmark bookmark)
  {
    this.wmCmd = wmCmd;
    this.bookmark = bookmark;
    this.hasInsertMarks = false;

    // Sicher ist sicher: Fehlermeldung wenn normale Dokumentkommandos ein
    // GROUPS-Attribut besitzen (die jetzt nicht merh unterst�tzt werden).
    if (wmCmd.query("GROUPS").count() > 0 && !canHaveGroupsAttribute())
    {
      Logger.error(L.m(
        "Das Dokumentkommando '%1' darf kein GROUPS-Attribut besitzen.",
        getBookmarkName()));
    }
  }

  /**
   * Seit der Umstellung auf den neuen Scan �ber DocumentCommands darf nur noch das
   * Dokumentkommando SetGroups ein GROUPS-Attribut besitzen. Diese Methode stellt
   * das sicher und wird vom SetGroups-Kommando mit R�ckgabewert true �berschrieben.
   * 
   * @return false au�er es ist ein SetGroups-Kommando
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  protected boolean canHaveGroupsAttribute()
  {
    return false;
  }

  /**
   * Liefert true, wenn das Dokumentkommando Textinhalte in das zugeh�rige Bookmark
   * einf�gen m�chte, ansonsten false.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  protected boolean insertsTextContent()
  {
    return false;
  }

  /**
   * Liefert den Namen des zugeh�rigen Bookmarks zur�ck.
   * 
   * @return Liefert den Namen des zugeh�rigen Bookmarks zur�ck.
   */
  public String getBookmarkName()
  {
    return bookmark.getName();
  }

  /**
   * Diese Methode liefert eine String-Repr�sentation des DokumentCommands zur�ck.
   * Die String-Repr�sentation hat den Aufbau DocumentCommand[<bookmarkName>].
   */
  public String toString()
  {
    return "" + this.getClass().getSimpleName() + "["
      + (isRetired() ? "RETIRED:" : "") + (isDone() ? "DONE:" : "")
      + getBookmarkName() + "]";
  }

  /**
   * Callbackfunktion f�r die Ausf�hrung des Dokumentkommandos in einem
   * DocumentCommand.Executor wie z.B. dem DocumentCommandInterpreter. Die Methode
   * liefert die Anzahl der bei der Ausf�hrung entstandenen Fehler zur�ck.
   * 
   * @param executor
   * @return Anzahl der aufgetretenen Fehler
   */
  public abstract int execute(DocumentCommand.Executor executor);

  /**
   * Liefert einen TextCursor f�r die TextRange, an der das Bookmark dieses
   * {@link DocumentCommand}s verankert ist, zur�ck oder <code>null</code>, falls
   * das Bookmark nicht mehr existiert (zum Beispiel weil es inzwischen gel�scht
   * wurde). Aufgrund von OOo-Issue #67869 ist es im allgemeinen besser den von
   * dieser Methode erzeugten Cursor statt direkt die TextRange zu verwenden, da sich
   * mit dem Cursor der Inhalt des Bookmarks sicherer enumerieren l�sst. Der von
   * dieser Methode zur�ckgelieferte TextCursor sollte allerdings nicht verwendet
   * werden, um direkt Text innerhalb eines Bookmarks einzuf�gen! Daf�r sind die
   * Methoden {@link #setTextRangeString(String)},
   * {@link #insertTextContentIntoBookmark(XTextContent, boolean)} und
   * {@link #getTextCursorWithinInsertMarks()} da.
   * 
   * @return einen TextCursor f�r den Anchor des Bookmarks oder <code>null</code>
   *         wenn das Bookmark nicht mehr existiert
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public XTextCursor getTextCursor()
  {
    XTextCursor cursor = bookmark.getTextCursor();
    if (cursor == null)
    {
      Logger.debug(L.m(
        "Kann keinen Textcursor erstellen f�r Dokumentkommando '%1'\nIst das Bookmark vielleicht verschwunden?",
        this.toString()));
    }
    return cursor;
  }

  /**
   * Liefert die TextRange an der das Bookmark dieses Kommandos verankert ist oder
   * null, falls das Bookmark nicht mehr existiert. Die von dieser Methode
   * zur�ckgelieferte TextRange sollte nicht verwendet werden, um direkt Text
   * innerhalb eines Bookmarks einzuf�gen! Daf�r sind die Methoden
   * {@link #setTextRangeString(String)},
   * {@link #insertTextContentIntoBookmark(XTextContent, boolean)} und
   * {@link #getTextCursorWithinInsertMarks()} da.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @see de.muenchen.allg.itd51.wollmux.VisibilityElement#getAnchor()
   */
  public XTextRange getAnchor()
  {
    return bookmark.getAnchor();
  }

  /**
   * Liefert einen TextCursor ohne Ausdehnung zum Einf�gen von Inhalt innerhalb des
   * Bookmarks dieses {@link DocumentCommand}s zur�ck, wobei der urspr�nglich
   * enthaltene Inhalt des Ankers des Bookmarks durch zwei Insert Marks ({@link #INSERT_MARK_OPEN}
   * und {@link #INSERT_MARK_CLOSE}) ersetzt wird. Sollte das Bookmark kollabiert
   * gewesen sein, so wird es zun�chst dekollabiert, so dass die Insert Marks
   * innerhalb des Bookmarks eingef�gt werden k�nnen. Der zur�ckgelieferte Cursor
   * besitzt keine Ausdehnung und befindet sich zwischen den beiden eingef�gten
   * Insert Marks. Falls das Bookmark nicht mehr existiert liefert die Methode
   * <code>null</code> zur�ck.
   * 
   * @return TextCursor zum Einf�gen innerhalb von Insert Marks oder
   *         <code>null</code>
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public XTextCursor getTextCursorWithinInsertMarks()
  {
    // Insert Marks hinzuf�gen
    // (dabei wird das Bookmark falls n�tig dekollabiert)
    this.setTextRangeString(INSERT_MARK_OPEN + INSERT_MARK_CLOSE);
    hasInsertMarks = true;

    XTextCursor cursor = this.getTextCursor(); // Cursor mit Insert Marks
    if (cursor != null)
    {
      cursor.goRight(getStartMarkLength(), false);
      cursor.collapseToStart();
    }

    return cursor;
  }

  /**
   * Setzt den Inhalt der TextRange, an der das Bookmark dieses Kommandos verankert
   * ist, auf den �bergebenen String-Wert und kollabiert/dekollabiert das Bookmark je
   * nachdem ob der String leer (bzw. <code>null</code>) oder nicht-leer ist. Bei
   * einem leeren String wird das Bookmark kollabiert (sofern es dies nicht schon
   * ist), bei einem nicht-leeren String wird das Bookmark dekollabiert (sofern nicht
   * schon der Fall) und das dekollabierte Bookmark umfasst anschlie�end den
   * �bergebenen Text.
   * 
   * @param text
   *          der einzuf�gende String; falls leer (oder <code>null</code>) wird
   *          Bookmark kollabiert
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public void setTextRangeString(String text)
  {
    if (text != null && text.length() > 0)
    {
      bookmark.decollapseBookmark();
    }

    XTextRange range = bookmark.getAnchor();
    if (range != null)
    {
      range.setString(text); // setString(null) kein Problem
    }

    if (text == null || text.length() == 0)
    {
      bookmark.collapseBookmark();
    }
  }

  /**
   * F�gt in die TextRange, an der das Bookmark dieses Kommandos verankert ist, den
   * �bergebenen TextContent ein, wobei das Bookmark zuvor dekollabiert wird (sollte
   * es das nicht ohnehin schon sein). �ber den Parameter replace kann gesteuert
   * werden, ob beim Einf�gen der bisherige Inhalt der TextRange des Bookmarks
   * ersetzt werden soll oder nicht. Wird <code>false</code> �bergeben, so wird der
   * TextContent an das Ende der TextRange des Bookmarks (aber nat�rlich noch im
   * Bookmark) hinzugef�gt.
   * 
   * @param textContent
   *          der einzuf�gende TextContent
   * @param replace
   *          bestimmt ob der in der TextRange des Bookmarks enthaltene Inhalt von
   *          textContent ersetzt werden soll. Falls <code>true</code> wird der
   *          Inhalt ersetzt, ansonsten wird textContent an das Ende der TextRange
   *          des Bookmarks geh�ngt
   * @throws IllegalArgumentException
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public void insertTextContentIntoBookmark(XTextContent textContent, boolean replace)
      throws IllegalArgumentException
  {
    if (textContent != null)
    {
      bookmark.decollapseBookmark();
      XTextCursor cursor = bookmark.getTextCursor();
      if (cursor != null)
      {
        XText text = cursor.getText();
        text.insertTextContent(cursor, textContent, replace);
      }
    }
  }

  /**
   * Liefert die L�nge des End-Einf�gemarkers.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public short getEndMarkLength()
  {
    return (short) INSERT_MARK_CLOSE.length();
  }

  /**
   * Liefert die L�nge des Start-Einf�gemarkers.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public short getStartMarkLength()
  {
    return (short) INSERT_MARK_OPEN.length();
  }

  /**
   * Liefert entweder null falls kein Start-Einf�gemarke vorhanden oder liefert 2
   * Cursor, von denen der erste links neben der zweite rechts neben der
   * Start-Einf�gemarke steht.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public XParagraphCursor[] getStartMark()
  {
    XTextRange range = bookmark.getTextCursor();
    if (range == null || !hasInsertMarks) return null;
    XParagraphCursor[] cursor = new XParagraphCursor[2];
    XText text = range.getText();
    cursor[0] = UNO.XParagraphCursor(text.createTextCursorByRange(range.getStart()));
    cursor[1] = UNO.XParagraphCursor(text.createTextCursorByRange(cursor[0]));
    cursor[1].goRight(getStartMarkLength(), false);
    return cursor;
  }

  /**
   * Liefert entweder null falls keine End-Einf�gemarke vorhanden oder liefert 2
   * Cursor, von denen der erste links neben der zweite rechts neben der
   * End-Einf�gemarke steht.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public XParagraphCursor[] getEndMark()
  {
    XTextRange range = bookmark.getTextCursor();
    if (range == null || !hasInsertMarks) return null;
    XParagraphCursor[] cursor = new XParagraphCursor[2];
    XText text = range.getText();
    cursor[0] = UNO.XParagraphCursor(text.createTextCursorByRange(range.getEnd()));
    cursor[1] = UNO.XParagraphCursor(text.createTextCursorByRange(cursor[0]));
    cursor[0].goLeft(getStartMarkLength(), false);
    return cursor;
  }

  /**
   * Liefert true zur�ck, wenn das Dokumentkommando insertMarks besitzt, die zuvor
   * �ber den Aufruf von createInsertCursor() erzeugt worden sind, ansonsten false.
   */
  public boolean hasInsertMarks()
  {
    return hasInsertMarks;
  }

  /**
   * Teilt dem Dokumentkommando mit, dass die insertMarks des Dokumentkommandos
   * entfernt wurden und hasInsertMarks() in Folge dessen false zur�ck liefern muss.
   */
  public void unsetHasInsertMarks()
  {
    this.hasInsertMarks = false;
  }

  /**
   * Liefert true, wenn das Bookmark zu diesem Dokumentkommando nicht mehr existiert
   * und das Dokumentkommando daher nicht mehr zu gebrauchen ist oder andernfalls
   * false.
   * 
   * @return true, wenn das Bookmark zu diesem Dokumentkommando nicht mehr existiert,
   *         ansonsten false.
   */
  public boolean isRetired()
  {
    if (bookmark != null) return bookmark.getAnchor() == null;
    return false;
  }

  /**
   * Beschreibt ob das Kommando bereits abgearbeitet wurde. Ist DONE bisher noch
   * nicht definiert oder gesetzt worden, so wird der Defaultwert false
   * zur�ckgeliefert.
   * 
   * @return true, falls das Kommando bereits bearbeitet wurde, andernfalls false.
   */
  public boolean isDone()
  {
    if (done != null)
      return done.booleanValue();
    else if (isDefinedState("DONE"))
    {
      String doneStr = getState("DONE").toString();
      if (doneStr.compareToIgnoreCase("true") == 0)
        return true;
      else
        return false;
    }
    else
      return STATE_DEFAULT_DONE.booleanValue();
  }

  /**
   * Markiert ein Dokumentkommando als bearbeitet; Das Dokumentkommando wird
   * daraufhin aus dem Dokument gel�scht, wenn removeBookmark==true oder umbenannt,
   * wenn removeBookmark==false.
   * 
   * @param removeBookmark
   *          true, signalisiert, dass das zugeh�rige Bookmark gel�scht werden soll.
   *          False signalisiert, dass das Bookmark mit dem Zusatz "<alterName>
   *          STATE(DONE 'true')" versehen wird.
   */
  public void markDone(boolean removeBookmark)
  {

    this.done = Boolean.TRUE;
    flushToBookmark(removeBookmark);
  }

  /**
   * Liefert den Fehler-Status der Kommandobearbeitung zur�ck. Ist das Attribut ERROR
   * bisher nicht definiert oder kein Fehler gesetzt worden, so wird der Defaultwert
   * false zur�ckgliefert.
   * 
   * @return true, wenn bei der Kommandobearbeitung Fehler auftraten; andernfalls
   *         false
   */
  public boolean hasError()
  {
    if (error != null)
      return error.booleanValue();
    else if (isDefinedState("ERROR"))
    {
      Boolean errorBool = Boolean.valueOf(getState("ERROR").toString());
      return errorBool.booleanValue();
    }
    else
      return STATE_DEFAULT_ERROR.booleanValue();
  }

  /**
   * Erlaubt das explizite Setzen des Error-Attributs.
   * 
   * @param error
   */
  public void setErrorState(boolean error)
  {
    this.error = Boolean.valueOf(error);
  }

  /**
   * Liefert ein ConfigThingy, das ein "WM"-Kommando mit allen Statusinformationen
   * enth�lt. Neue Unterknoten werden dabei nur angelegt, wenn dies unbedingt
   * erforderlich ist, d.h. wenn ein Wert vom Defaultwert abweicht oder der Wert
   * bereits vorher gesetzt war.
   * 
   * @return Ein ConfigThingy, das das "WM"-Kommando mit allen Statusinformationen
   *         enth�lt.
   */
  protected ConfigThingy toConfigThingy()
  {
    // DONE:
    // Falls der Knoten existiert und sich der Status ge�ndert hat wird der neue
    // Status gesetzt. Falls der Knoten nicht existiert wird er nur erzeugt,
    // wenn der Status vom Standard abweicht.
    if (isDefinedState("DONE") && done != null)
    {
      setOrCreate("DONE", done.toString());
    }
    else if (isDone() != STATE_DEFAULT_DONE.booleanValue())
    {
      setOrCreate("DONE", "" + isDone() + "");
    }

    // ERRORS:
    // Falls der Knoten existiert und sich der Status ge�ndert hat wird der neue
    // Status gesetzt. Falls der Knoten nicht existiert wird er nur erzeugt,
    // wenn der Status vom Standard abweicht.
    if (isDefinedState("ERROR") && error != null)
    {
      setOrCreate("ERROR", error.toString());
    }
    else if (hasError() != STATE_DEFAULT_ERROR.booleanValue())
    {
      setOrCreate("ERRORS", "" + hasError() + "");
    }

    return wmCmd;
  }

  /**
   * Liefert den Inhalt des �bergebenen ConfigThingy-Objekts (�blicherweise das
   * wmCmd) als einen String, der geeignet ist, um den ihn in Bookmarknamen verwenden
   * zu k�nnen. D.h. alle Newline, Komma und andere f�r Bookmarknamen unvertr�gliche
   * Zeichen werden entfernt.
   * 
   * @param conf
   *          Das ConfigThingy-Objekt, zu dem die Stringrepr�sentation erzeugt werden
   *          soll.
   * @return Einen String, der als Bookmarkname verwendet werden kann.
   */
  public static String getCommandString(ConfigThingy conf)
  {
    // Neues WM-String zusammenbauen, der keine Zeilenvorsch�be, Kommas und
    // abschlie�ende Leerzeichen enth�lt:
    String wmCmdString = conf.stringRepresentation(true, '\'', true);
    wmCmdString = wmCmdString.replaceAll(",", " ");
    wmCmdString = wmCmdString.replaceAll("[\r\n]+", " ");
    while (wmCmdString.endsWith(" "))
      wmCmdString = wmCmdString.substring(0, wmCmdString.length() - 1);
    return wmCmdString;
  }

  /**
   * Schreibt den neuen Status des Dokumentkommandos in das Dokument zur�ck oder
   * l�scht ein Bookmark, wenn der Status DONE=true gesetzt ist - Die Methode liefert
   * entweder den Namen des neuen Bookmarks, welches die neuen Statusinformationen
   * enth�lt zur�ck, oder null, wenn das zugeh�rige Bookmark gel�scht wurde. Ist der
   * DEBUG-modus gesetzt, so werden in gar keinem Fall Bookmarks gel�scht, womit die
   * Fehlersuche erleichtert werden soll.
   * 
   * @return der Name des neuen Bookmarks oder null.
   */
  protected String flushToBookmark(boolean removeIfDone)
  {
    if (isDone() && removeIfDone)
    {
      bookmark.remove();
      return null;
    }
    else
    {
      String wmCmdString = getCommandString(toConfigThingy());

      // Neuen Status rausschreiben, wenn er sich ge�ndert hat:
      String name = bookmark.getName();
      name = name.replaceFirst("\\s*\\d+\\s*$", "");
      if (!wmCmdString.equals(name)) bookmark.rename(wmCmdString);

      return bookmark.getName();
    }
  }

  /**
   * Gibt Auskunft, ob ein Key unterhalb des STATE-Knotens definiert ist. z.B.
   * "WM(...) STATE (KEY '...')"
   * 
   * @param key
   * @return true, falls der Key definiert ist, andernfalls false.
   */
  protected boolean isDefinedState(String key)
  {
    return (getState(key) != null);
  }

  /**
   * Liefert das ConfigThingy zu dem gesuchten Key key unterhalt des STATE-Knotens.
   * 
   * @param key
   * @return
   */
  protected ConfigThingy getState(String key)
  {
    ConfigThingy state;
    try
    {
      state = wmCmd.get("STATE");
      return state.get(key);
    }
    catch (NodeNotFoundException e1)
    {
      return null;
    }
  }

  /**
   * Setzt einen Schl�ssel-Wert-Paar unterhalb des STATE-Knotens. Ist der Schl�ssel
   * bereits definiert, wird der bestehende Wert �berschrieben. Sind der STATE-Knoten
   * oder der Schl�ssel nicht definiert, so werden die entsprechenden Knoten erzeugt
   * und der Key key erh�lt ein Kindknoten mit dem Value value.
   * 
   * @param key
   * @param value
   */
  protected void setOrCreate(String key, String value)
  {
    // gew�nschte Struktur aufbauen:

    // a) STATE(...)
    ConfigThingy state;
    try
    {
      state = wmCmd.get("STATE");
    }
    catch (NodeNotFoundException e1)
    {
      state = wmCmd.add("STATE");
    }

    // b) STATE(KEY ...)
    ConfigThingy ctKey;
    try
    {
      ctKey = state.get(key);
    }
    catch (NodeNotFoundException e)
    {
      ctKey = state.add(key);
    }

    // c) STATE(KEY 'value')
    try
    {
      ctKey.getFirstChild().setName(value);
    }
    catch (NodeNotFoundException e)
    {
      ctKey.add(value);
    }
  }

  /**
   * gibt den Sichtbarkeitsstatus des Textinhaltes unter dem Dokumentkommando zur�ck.
   * 
   * @return true=sichtbar, false=ausgeblendet
   * @see de.muenchen.allg.itd51.wollmux.VisibilityElement#isVisible()
   */
  public boolean isVisible()
  {
    return visible;
  }

  /**
   * Setzt den Sichtbarkeitsstatus des Textinhaltes unter dem Dokumentkommando auf
   * visible. Der Status visible wird zudem nicht persistent im Bookmark hinterlegt.
   * 
   * @param visible
   *          true=sichtbar, false=ausgeblendet
   * @see de.muenchen.allg.itd51.wollmux.VisibilityElement#setVisible(boolean)
   */
  public void setVisible(boolean visible)
  {
    this.visible = visible;
    XTextCursor cursor = getTextCursor();
    if (cursor != null)
    {
      UNO.hideTextRange(cursor, !visible);
    }
  }

  // ********************************************************************************
  /**
   * Kommandos werden extern definiert - um das zu erm�glichen greift hier das
   * Prinzip des Visitor-Designpatterns.
   * 
   * @author christoph.lutz
   */
  static interface Executor
  {
    public int executeCommand(DocumentCommand.InsertFrag cmd);

    public int executeCommand(DocumentCommand.InsertValue cmd);

    public int executeCommand(DocumentCommand.InsertContent cmd);

    public int executeCommand(DocumentCommand.Form cmd);

    public int executeCommand(DocumentCommand.InvalidCommand cmd);

    public int executeCommand(DocumentCommand.UpdateFields cmd);

    public int executeCommand(DocumentCommand.SetType cmd);

    public int executeCommand(DocumentCommand.InsertFormValue cmd);

    public int executeCommand(DocumentCommand.SetGroups cmd);

    public int executeCommand(DocumentCommand.SetPrintFunction cmd);

    public int executeCommand(DocumentCommand.DraftOnly cmd);

    public int executeCommand(DocumentCommand.NotInOriginal cmd);

    public int executeCommand(DocumentCommand.OriginalOnly cmd);

    public int executeCommand(DocumentCommand.AllVersions cmd);

    public int executeCommand(DocumentCommand.SetJumpMark cmd);

    public int executeCommand(DocumentCommand.OverrideFrag cmd);
  }

  // ********************************************************************************
  /**
   * Beschreibt ein Dokumentkommando, das einen Wert in das Dokument einf�gt, der
   * �ber eine optionale Transformation umgewandelt werden kann (Derzeit
   * implementieren insertValue und insertFormValue dieses Interface).
   */
  public static interface OptionalTrafoProvider
  {
    public String getTrafoName();
  }

  // ********************************************************************************
  /**
   * Beschreibt ein Dokumentkommando, das das optionale Attribut HIGHLIGHT_COLOR
   * enthalten kann (derzeit AllVersions, DraftOnly, NotInOriginal und OriginalOnly)
   */
  public static interface OptionalHighlightColorProvider
  {
    public String getHighlightColor();
  }

  // ********************************************************************************
  /**
   * Eine Exception die geworfen wird, wenn ein Dokumentkommando als ung�ltig erkannt
   * wurde, z,b, aufgrund eines fehlenden Parameters.
   */
  static public class InvalidCommandException extends com.sun.star.uno.Exception
  {
    private static final long serialVersionUID = -3960668930339529734L;

    public InvalidCommandException(String message)
    {
      super(message);
    }
  }

  // ********************************************************************************
  /**
   * Beschreibt ein Dokumentkommando, das aufgrund eines Syntax-Fehlers oder eines
   * fehlenden Parameters ung�ltig ist, jedoch trotzdem im Dokument als Fehlerfeld
   * dargestellt werden soll.
   * 
   * @author lut
   * 
   */
  static public class InvalidCommand extends DocumentCommand
  {
    private java.lang.Exception exception;

    public InvalidCommand(ConfigThingy wmCmd, Bookmark bookmark,
        InvalidCommandException exception)
    {
      super(wmCmd, bookmark);
      this.exception = exception;
    }

    public InvalidCommand(Bookmark bookmark, SyntaxErrorException exception)
    {
      super(new ConfigThingy("WM"), bookmark);
      this.exception = exception;
    }

    public java.lang.Exception getException()
    {
      return exception;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    public String updateBookmark()
    {
      // der updateBookmark darf in diesem Fall nat�rlich nichts rausschreiben,
      // da das Kommando ja nicht mal in einer syntaktisch vollst�ndigen Version
      // vorliegt.
      return getBookmarkName();
    }
  }

  // ********************************************************************************
  /**
   * Beschreibt ein noch nicht implementiertes Dokumentkommando, das jedoch f�r die
   * Zukunft geplant ist und dess Ausf�hrung daher keine Fehler beim Erzeugen des
   * Briefkopfs liefern darf.
   */
  static public class NotYetImplemented extends DocumentCommand
  {
    public NotYetImplemented(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
      markDone(false);
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return 0;
    }
  }

  // ********************************************************************************
  /**
   * Beschreibt das Dokumentkommando Form, welches Zugriff auf die
   * Formularbeschreibung des Dokuments erm�glicht, die in Form einer Notiz innerhalb
   * des zum Form-Kommando zugeh�rigen Bookmarks abgelegt ist.
   * 
   * @author lut
   * 
   */
  static public class Form extends DocumentCommand
  {
    public Form(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Das Kommando InsertFrag f�gt ein externes Textfragment in das Dokument ein.
   */
  static public class InsertFrag extends DocumentCommand
  {
    private String fragID;

    private Vector<String> args = null;

    private boolean manualMode = false;

    private Set<String> styles = null;

    public InsertFrag(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);

      try
      {
        fragID = wmCmd.get("WM").get("FRAG_ID").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut FRAG_ID"));
      }

      args = new Vector<String>();
      try
      {
        ConfigThingy argsConf = wmCmd.get("WM").get("ARGS");
        Iterator<ConfigThingy> iter = argsConf.iterator();
        while (iter.hasNext())
        {
          ConfigThingy arg = iter.next();
          args.add(arg.getName());
        }
      }
      catch (NodeNotFoundException e)
      {
        // ARGS sind optional
      }

      String mode = "";
      try
      {
        mode = wmCmd.get("WM").get("MODE").toString();
        if (mode.equalsIgnoreCase("manual"))
        {
          manualMode = true;
        }
      }
      catch (NodeNotFoundException e)
      {
        // MODE ist optional;
      }

      styles = new HashSet<String>();
      try
      {
        ConfigThingy stylesConf = wmCmd.get("WM").get("STYLES");
        for (Iterator<ConfigThingy> iter = stylesConf.iterator(); iter.hasNext();)
        {
          String s = iter.next().toString();
          if (s.equalsIgnoreCase("all"))
          {
            styles.add("textstyles");
            styles.add("pagestyles");
            styles.add("numberingstyles");
          }
          else if (s.equalsIgnoreCase("textStyles"))
          {
            styles.add(s.toLowerCase());
          }
          else if (s.equalsIgnoreCase("pageStyles"))
          {
            styles.add(s.toLowerCase());
          }
          else if (s.equalsIgnoreCase("numberingStyles"))
          {
            styles.add(s.toLowerCase());
          }
          else
            throw new InvalidCommandException(L.m("STYLE '%1' ist unbekannt.", s));
        }
      }
      catch (NodeNotFoundException e)
      {
        // STYLES ist optional;
      }

    }

    public String getFragID()
    {
      return fragID;
    }

    public Vector<String> getArgs()
    {
      return args;
    }

    public boolean isManualMode()
    {
      return manualMode;
    }

    public boolean importStylesOnly()
    {
      return styles.size() > 0;
    }

    public Set<String> getStyles()
    {
      return styles;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    protected boolean insertsTextContent()
    {
      return true;
    }
  }

  // ********************************************************************************
  /**
   * Das Kommando InsertContent dient zum Mischen von Dokumenten und ist im Handbuch
   * des WollMux ausf�hrlicher beschrieben.
   */
  static public class InsertContent extends DocumentCommand
  {
    public InsertContent(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    protected boolean insertsTextContent()
    {
      return true;
    }
  }

  // ********************************************************************************
  /**
   * Dieses Kommando f�gt den Wert eines Absenderfeldes in den Briefkopf ein.
   */
  static public class InsertValue extends DocumentCommand implements
      OptionalTrafoProvider
  {
    private String dbSpalte;

    private String leftSeparator = "";

    private String rightSeparator = "";

    private String trafo = null;

    public InsertValue(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);
      try
      {
        dbSpalte = wmCmd.get("WM").get("DB_SPALTE").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut DB_SPALTE"));
      }

      // Auswertung der AUTOSEP bzw SEPERATOR-Attribute
      Iterator<ConfigThingy> autoseps = wmCmd.query("AUTOSEP").iterator();
      Iterator<ConfigThingy> seps = wmCmd.query("SEPARATOR").iterator();
      String currentSep = " "; // mit Default-Separator vorbelegt

      while (autoseps.hasNext())
      {
        ConfigThingy as = autoseps.next();
        String sep = currentSep;
        if (seps.hasNext()) sep = seps.next().toString();

        if (as.toString().compareToIgnoreCase("left") == 0)
        {
          leftSeparator = sep;
        }
        else if (as.toString().compareToIgnoreCase("right") == 0)
        {
          rightSeparator = sep;
        }
        else if (as.toString().compareToIgnoreCase("both") == 0)
        {
          leftSeparator = sep;
          rightSeparator = sep;
        }
        else
        {
          throw new InvalidCommandException(
            L.m(
              "Unbekannter AUTOSEP-Typ \"%1\". Erwarte \"left\", \"right\" oder \"both\".",
              as.toString()));
        }
        currentSep = sep;
      }

      // Auswertung des optionalen Arguments TRAFO
      try
      {
        trafo = wmCmd.get("WM").get("TRAFO").toString();
      }
      catch (NodeNotFoundException e)
      {
        // TRAFO ist optional
      }
    }

    public String getDBSpalte()
    {
      return dbSpalte;
    }

    public String getLeftSeparator()
    {
      return leftSeparator;
    }

    public String getRightSeparator()
    {
      return rightSeparator;
    }

    public String getTrafoName()
    {
      return trafo;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    protected boolean insertsTextContent()
    {
      return true;
    }
  }

  // ********************************************************************************
  /**
   * Dieses Kommando f�gt den Wert eines Absenderfeldes in den Briefkopf ein.
   */
  static public class InsertFormValue extends DocumentCommand implements
      OptionalTrafoProvider
  {
    private String id = null;

    private String trafo = null;

    public InsertFormValue(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);

      try
      {
        id = wmCmd.get("WM").get("ID").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut ID"));
      }

      try
      {
        trafo = wmCmd.get("WM").get("TRAFO").toString();
      }
      catch (NodeNotFoundException e)
      {
        // TRAFO ist optional
      }
    }

    public String getID()
    {
      return id;
    }

    public void setID(String id)
    {
      this.id = id;
      // Dokumentkommando anpassen:
      try
      {
        ConfigThingy idConf = wmCmd.query("WM").query("ID").getLastChild();
        // alten Wert von ID l�schen
        for (Iterator<ConfigThingy> iter = idConf.iterator(); iter.hasNext();)
        {
          iter.next();
          iter.remove();
        }
        // neuen Wert f�r ID setzen
        idConf.addChild(new ConfigThingy(id));
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(e);
      }
      flushToBookmark(false);
    }

    public String getTrafoName()
    {
      return trafo;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    protected boolean insertsTextContent()
    {
      return true;
    }
  }

  // ********************************************************************************
  /**
   * dieses Kommando sorgt daf�r, dass alle unter dem Bookmark liegenden TextFields
   * geupdatet werden.
   */
  static public class UpdateFields extends DocumentCommand
  {
    public UpdateFields(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * �ber dieses Dokumentkommando kann der Typ des Dokuments festgelegt werden. Es
   * gibt die Typen SETTYPE_normalTemplate, SETTYPE_templateTemplate und
   * SETTYPE_formDocument. Die SetType-Kommandos werden bereits im
   * OnProcessTextDocument-Event verarbeitet und spielen daher keine Rolle mehr f�r
   * den DocumentCommandInterpreter.
   */
  static public class SetType extends DocumentCommand
  {
    private String type;

    public SetType(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);
      type = "";
      try
      {
        type = wmCmd.get("WM").get("TYPE").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut TYPE"));
      }
      if (type.compareToIgnoreCase("templateTemplate") != 0
        && type.compareToIgnoreCase("normalTemplate") != 0
        && type.compareToIgnoreCase("formDocument") != 0)
        throw new InvalidCommandException(
          L.m("Angegebener TYPE ist ung�ltig oder falsch geschrieben. Erwarte \"templateTemplate\", \"normalTemplate\" oder \"formDocument\"!"));
    }

    String getType()
    {
      return type;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Dieses Kommando dient zum �berschreiben von FRAG_IDs, die mit insertFrag oder
   * insertContent eingef�gt werden sollen.
   */
  static public class OverrideFrag extends DocumentCommand
  {
    private String fragId;

    private String newFragId = null;

    public OverrideFrag(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);
      fragId = "";
      try
      {
        fragId = wmCmd.get("WM").get("FRAG_ID").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut FRAG_ID"));
      }
      try
      {
        newFragId = wmCmd.get("WM").get("NEW_FRAG_ID").toString();
      }
      catch (NodeNotFoundException e)
      {
        // NEW_FRAG_ID ist optional
      }
    }

    String getFragID()
    {
      return fragId;
    }

    /**
     * Liefert die neue FragID oder den Leerstring, wenn keine FragID angegeben
     * wurde.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    String getNewFragID()
    {
      if (newFragId == null) return "";
      return newFragId;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * �ber dieses Dokumentkommando kann der Typ des Dokuments festgelegt werden. Es
   * gibt die Typen SETTYPE_normalTemplate, SETTYPE_templateTemplate und
   * SETTYPE_formDocument. Die SetType-Kommandos werden bereits im
   * OnProcessTextDocument-Event verarbeitet und spielen daher keine Rolle mehr f�r
   * den DocumentCommandInterpreter.
   */
  static public class SetPrintFunction extends DocumentCommand
  {
    private String funcName;

    public SetPrintFunction(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);
      funcName = "";
      try
      {
        funcName = wmCmd.get("WM").get("FUNCTION").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut FUNCTION"));
      }
    }

    public String getFunctionName()
    {
      return funcName;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Das Kommando SetGroups dient dazu, dem vom Bookmark umschlossenen Textinhalt
   * eine oder mehrere Gruppen zuweisen zu k�nnen. Im Gegensatz zu anderen
   * Dokumentkommandos, die auch das GROUPS Attribut unterst�tzen, besitzt dieses
   * Kommando ausser der Zuordnung von Gruppen keine weitere Funktion.
   */
  static public class SetGroups extends DocumentCommand implements VisibilityElement
  {
    private Set<String> groupsSet;

    public SetGroups(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
      groupsSet = new HashSet<String>();
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    /**
     * Liefert alle Gruppen, die dem Dokumentkommando zugeordnet sind, wobei jede
     * Gruppe auch die Gruppenzugeh�rigkeit des Vaterelements im DocumentCommand-Baum
     * erbt.
     * 
     * @return Ein Set, das alle zugeordneten groupId's, einschlie�lich der vom Vater
     *         geerbten, als Strings enth�lt.
     * @see de.muenchen.allg.itd51.wollmux.VisibilityElement#getGroups()
     */
    public Set<String> getGroups()
    {
      // GROUPS-Attribut auslesen falls vorhanden.
      ConfigThingy groups = new ConfigThingy("");
      try
      {
        groups = wmCmd.get("GROUPS");
      }
      catch (NodeNotFoundException e)
      {}

      // Gruppen aus dem GROUPS-Argument in das Set aufnehmen:
      for (Iterator<ConfigThingy> iter = groups.iterator(); iter.hasNext();)
      {
        String groupId = iter.next().toString();
        groupsSet.add(groupId);
      }

      return groupsSet;
    }

    public void addGroups(Set<String> groups)
    {
      groupsSet.addAll(groups);
    }

    protected boolean canHaveGroupsAttribute()
    {
      return true;
    }

    /**
     * Diese Methode liefert eine String-Repr�sentation des DokumentCommands zur�ck.
     * Die String-Repr�sentation hat den Aufbau DocumentCommand[<bookmarkName>].
     */
    public String toString()
    {
      return "" + this.getClass().getSimpleName() + "["
        + (isRetired() ? "RETIRED:" : "") + (isDone() ? "DONE:" : "") + "GROUPS:"
        + groupsSet.toString() + getBookmarkName() + "]";
    }
  }

  // ********************************************************************************
  /**
   * Beim Drucken von Sachleitenden Verf�gungen wird die Ausfertigung, die ALLE
   * definierten Verf�gungpunkte enth�lt als "Entwurf" bezeichnet. Mit einem
   * DraftOnly-Kommando k�nnen Bl�cke im Text definiert werden (auch an anderen
   * Stellen), die ausschlie�lich im Entwurf angezeigt werden sollen.
   */
  static public class DraftOnly extends DocumentCommand implements
      OptionalHighlightColorProvider
  {
    String highlightColor = null;

    public DraftOnly(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);

      try
      {
        highlightColor = wmCmd.get("WM").get("HIGHLIGHT_COLOR").toString();
      }
      catch (NodeNotFoundException e)
      {
        // HIGHLIGHT_COLOR ist optional
      }
    }

    public String getHighlightColor()
    {
      return highlightColor;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Beim Drucken von Sachleitenden Verf�gungen wird der Verf�gungspunkt I als
   * Original bezeichnet. Mit dem NotInOriginal Kommando ist es m�glich Bl�cke im
   * Text zu definieren, die NIEMALS in Originalen abgedruckt werden sollen, jedoch
   * in allen anderen Ausdrucken, die nicht das Original sind (wie z.B. Abdr�cke und
   * Entwurf).
   */
  static public class NotInOriginal extends DocumentCommand implements
      OptionalHighlightColorProvider
  {
    String highlightColor = null;

    public NotInOriginal(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);

      try
      {
        highlightColor = wmCmd.get("WM").get("HIGHLIGHT_COLOR").toString();
      }
      catch (NodeNotFoundException e)
      {
        // HIGHLIGHT_COLOR ist optional
      }
    }

    public String getHighlightColor()
    {
      return highlightColor;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Beim Drucken von Sachleitenden Verf�gungen wird der Verf�gungspunkt I als
   * Original bezeichnet. Mit dem OriginalOnly Kommando ist es m�glich Bl�cke im Text
   * zu definieren, die ausschlie�lich in Originalen abgedruckt werden sollen.
   */
  static public class OriginalOnly extends DocumentCommand implements
      OptionalHighlightColorProvider
  {
    String highlightColor = null;

    public OriginalOnly(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);

      try
      {
        highlightColor = wmCmd.get("WM").get("HIGHLIGHT_COLOR").toString();
      }
      catch (NodeNotFoundException e)
      {
        // HIGHLIGHT_COLOR ist optional
      }
    }

    public String getHighlightColor()
    {
      return highlightColor;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Beim Drucken von Sachleitenden Verf�gungen werden alle Verf�gungspunkte
   * unterhalb des ausgew�hlten Verf�gungspunktes ausgeblendet. Mit dem AllVersions
   * Kommando ist es m�glich Bl�cke im Text zu definieren, die IMMER ausgedruckt
   * werden sollen, d.h. sowohl bei Originalen, als auch bei Abdrucken und Entw�rfen.
   */
  static public class AllVersions extends DocumentCommand implements
      OptionalHighlightColorProvider
  {
    String highlightColor = null;

    public AllVersions(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);

      try
      {
        highlightColor = wmCmd.get("WM").get("HIGHLIGHT_COLOR").toString();
      }
      catch (NodeNotFoundException e)
      {
        // HIGHLIGHT_COLOR ist optional
      }
    }

    public String getHighlightColor()
    {
      return highlightColor;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Falls nach dem Einf�gen eines Textbausteines keine Einf�gestelle vorhanden ist
   * wird die Marke 'setJumpMark' falls vorhanden angesprungen. Wird auch falls
   * vorhanden und keine Platzhalter vorhanden ist, mit PlatzhalterAnspringen
   * angesprungen.
   */
  static public class SetJumpMark extends DocumentCommand
  {
    public SetJumpMark(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }
}
