/*
 * Dateiname: WMInsertField.java
 * Projekt  : WollMux
 * Funktion : Repr�sentiert ein vom WMCommandInterpreter in das Dokument
 *            eingef�gte Feld.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 15.11.2005 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UnoService;

// so gehts:
// "x" ist die explizit sichtbare FRAGMENT_MARK, die sp�ter auf
// Hidden ("h") gesetzt wird.
// 1) bookmarkCursor = "xx"
// 2) insertCursor exakt in die Mitte der xx setzen.
// 3) Inhalte einf�gen in insCursor
// 4) alle "x" auf "h" setzen
// Ergebnis: bookmarCursor = "h<inhalt>h"

/**
 * Repr�sentiert ein vom WMCommandInterpreter in das Dokument eingef�gte Feld.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
public class WMInsertField
{
  /**
   * Versteckte Trennzeichen, die zum Beginn und zum Ende eines InsertFields
   * eingef�gt werden um verschachtelte WollMux-Kommandos zu erm�glichen.
   */
  private static final String FRAGMENT_MARK_OPEN = "<";

  private static final String FRAGMENT_MARK_CLOSE = ">";

  /**
   * Beinhaltet den Cursor, der den gesamten Inhalt, d.h. FRAGMENT_MARKS +
   * Eingef�gter Inhalt enth�lt.
   */
  UnoService cursor;

  /**
   * Erzeugt ein neues InsertField, das den Inhalt im gegebenen XTextRange range
   * �berschreibt.
   * 
   * @param range
   */
  public WMInsertField(XTextRange range)
  {
    cursor = new UnoService(range.getText().createTextCursorByRange(range));
    cursor.xTextCursor().setString(FRAGMENT_MARK_OPEN + FRAGMENT_MARK_CLOSE);
    try
    {
      cursor.setPropertyValue("CharHidden", Boolean.FALSE);
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Liefert den gesamten Bereich des InsertFields zur�ck, d.h. die MARKS und
   * den eingef�gten Inhalt.
   * 
   * @return Liefert den gesamten Bereich des InsertFields zur�ck, d.h. die
   *         MARKS und den eingef�gten Inhalt.
   * 
   */
  public XTextRange getTextRange()
  {
    return cursor.xTextCursor();
  }

  public void hideMarks()
  {
    UnoService hiddenCursor = new UnoService(cursor.xTextCursor().getText()
        .createTextCursor());

    // start-Marke verstecken
    hiddenCursor.xTextCursor()
        .gotoRange(cursor.xTextCursor().getStart(), false);
    hiddenCursor.xTextCursor().goRight(
        (short) FRAGMENT_MARK_OPEN.length(),
        true);
    try
    {
      hiddenCursor.setPropertyValue("CharHidden", Boolean.TRUE);
    }
    catch (Exception x)
    {
      Logger.error(x);
    }

    // end-Marke verstecken
    hiddenCursor.xTextCursor().gotoRange(cursor.xTextCursor().getEnd(), false);
    hiddenCursor.xTextCursor().goLeft(
        (short) FRAGMENT_MARK_CLOSE.length(),
        true);
    try
    {
      hiddenCursor.setPropertyValue("CharHidden", Boolean.TRUE);
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Diese Methode erzeugt einen XTextCursor, der verwendet werden kann, um
   * Inhalte in das InsertField einzuf�gen.
   * 
   * @return Ein UnoService, der den gew�nschten XTextCursor enth�lt.
   */
  public UnoService createInsertCursor()
  {
    XTextCursor insCursor = cursor.xTextCursor().getText()
        .createTextCursorByRange(cursor.xTextCursor());
    insCursor.goRight((short) FRAGMENT_MARK_OPEN.length(), false);
    insCursor.collapseToStart();
    return new UnoService(insCursor);
  }
}
