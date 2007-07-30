/*
 * Dateiname: SachleitendeVerfuegung.java
 * Projekt  : WollMux
 * Funktion : Hilfen f�r Sachleitende Verf�gungen.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 26.09.2006 | LUT | Erstellung als SachleitendeVerfuegung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import com.sun.star.awt.FontWeight;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.style.XStyle;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.OptionalHighlightColorProvider;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel.PrintFailedException;
import de.muenchen.allg.itd51.wollmux.dialog.SachleitendeVerfuegungenDruckdialog;

public class SachleitendeVerfuegung
{
  public static final String PRINT_FUNCTION_NAME = "SachleitendeVerfuegung";

  private static final String CHARACTER_STYLES = "CharacterStyles";

  private static final String PARAGRAPH_STYLES = "ParagraphStyles";

  private static final String ParaStyleNameVerfuegungspunkt = "WollMuxVerfuegungspunkt";

  private static final String ParaStyleNameVerfuegungspunkt1 = "WollMuxVerfuegungspunkt1";

  private static final String ParaStyleNameAbdruck = "WollMuxVerfuegungspunktAbdruck";

  private static final String ParaStyleNameVerfuegungspunktMitZuleitung = "WollMuxVerfuegungspunktMitZuleitung";

  private static final String ParaStyleNameZuleitungszeile = "WollMuxZuleitungszeile";

  private static final String ParaStyleNameDefault = "Flie�text";

  private static final String CharStyleNameDefault = "Flie�text";

  private static final String CharStyleNameRoemischeZiffer = "WollMuxRoemischeZiffer";

  private static final String FrameNameVerfuegungspunkt1 = "WollMuxVerfuegungspunkt1";

  private static final String zifferPattern = "^([XIV]+|\\d+)\\.\t";

  /**
   * Enth�lt einen Vector mit den ersten 15 r�mischen Ziffern. Mehr wird in
   * Sachleitenden Verf�gungen sicherlich nicht ben�tigt :-)
   */
  private static final String[] romanNumbers = new String[] {
                                                             "I.",
                                                             "II.",
                                                             "III.",
                                                             "IV.",
                                                             "V.",
                                                             "VI.",
                                                             "VII.",
                                                             "VIII.",
                                                             "IX.",
                                                             "X.",
                                                             "XI.",
                                                             "XII.",
                                                             "XIII.",
                                                             "XIV.",
                                                             "XV." };

  /**
   * Setzt das Absatzformat des Absatzes, der range ber�hrt, auf
   * "WollMuxVerfuegungspunkt" ODER setzt alle in range enthaltenen
   * Verf�gungspunkte auf Flie�text zur�ck, wenn range einen oder mehrere
   * Verf�gungspunkte ber�hrt.
   * 
   * @param range
   *          Die XTextRange, in der sich zum Zeitpunkt des Aufrufs der Cursor
   *          befindet.
   * @return die Position zur�ck, auf die der ViewCursor gesetzt werden soll
   *         oder null, falls der ViewCursor unver�ndert bleibt.
   */
  public static XTextRange insertVerfuegungspunkt(TextDocumentModel model,
      XTextRange range)
  {
    if (range == null) return null;

    // Notwendige Absatzformate definieren (falls nicht bereits definiert)
    createUsedStyles(model.doc);

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));

    // Enth�lt der markierte Bereich bereits Verfuegungspunkte, so werden diese
    // gel�scht
    boolean deletedAtLeastOne = removeAllVerfuegungspunkte(cursor);

    if (!deletedAtLeastOne)
    {
      // neuen Verf�gungspunkt setzen:
      cursor.collapseToStart();
      cursor.gotoStartOfParagraph(false);
      cursor.gotoEndOfParagraph(true);
      if (isZuleitungszeile(cursor))
        formatVerfuegungspunktWithZuleitung(cursor);
      else
        formatVerfuegungspunkt(cursor);
    }

    // Ziffernanpassung durchf�hren:
    ziffernAnpassen(model);

    return null;
  }

  /**
   * Erzeugt am Ende des Paragraphen, der von range ber�hrt wird, einen neuen
   * Paragraphen, setzt diesen auf das Absatzformat
   * WollMuxVerfuegungspunktAbdruck und belegt ihn mit dem String "Abdruck von
   * <Vorg�nger>" ODER l�scht alle Verf�gungspunkte die der range ber�hrt, wenn
   * in ihm mindestens ein bereits bestehender Verf�gungspunkt enthalten ist.
   * 
   * @param doc
   *          Das Dokument, in dem der Verf�gungspunkt eingef�gt werden soll
   *          (wird f�r die Ziffernanpassung ben�tigt)
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Verf�gungspunkten gesucht wird.
   * @return die Position zur�ck, auf die der ViewCursor gesetzt werden soll
   *         oder null, falls der ViewCursor unver�ndert bleibt.
   */
  public static XTextRange insertAbdruck(TextDocumentModel model,
      XTextRange range)
  {
    if (range == null) return null;

    // Notwendige Absatzformate definieren (falls nicht bereits definiert)
    createUsedStyles(model.doc);

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));

    // Enth�lt der markierte Bereich bereits Verfuegungspunkte, so werden diese
    // gel�scht
    boolean deletedAtLeastOne = removeAllAbdruecke(cursor);

    if (!deletedAtLeastOne)
    {
      // Abdruck einf�gen, wenn kein Verf�gungspunkt gel�scht wurde:

      // Startposition des cursors setzen. Bereiche werden auf den Anfang
      // kollabiert. Bei Verf�gungspunkten wird am Absatzende eingef�gt.
      cursor.collapseToStart();
      if (isVerfuegungspunkt(cursor)) cursor.gotoEndOfParagraph(false);

      int count = countVerfPunkteBefore(model.doc, cursor) + 1;
      cursor.setString("\r" + abdruckString(count) + "\r");
      // Falls Cursor auf dem Zeilenanfang stand, wird die Formatierung auf
      // Standardformatierung gesetzt
      if (cursor.isStartOfParagraph()) formatDefault(cursor.getStart());
      cursor.gotoNextParagraph(false);
      cursor.gotoEndOfParagraph(true);
      formatAbdruck(cursor);
      cursor.gotoNextParagraph(false);
    }

    // Ziffern anpassen:
    ziffernAnpassen(model);

    return cursor;
  }

  /**
   * Formatiert alle Paragraphen die der TextRange range ber�hrt mit dem
   * Absatzformat WollMuxZuleitungszeile und markiert diese Zeilen damit auch
   * semantisch als Zuleitungszeilen ODER setzt das Absatzformat der
   * ensprechenden Paragraphen wieder auf Flie�text zur�ck, wenn mindestens ein
   * Paragraph bereits eine Zuleitungszeile ist.
   * 
   * @param doc
   *          Das Dokument in dem die sich range befindet.
   * @param range
   * @return die Position zur�ck, auf die der ViewCursor gesetzt werden soll
   *         oder null, falls der ViewCursor unver�ndert bleibt.
   */
  public static XTextRange insertZuleitungszeile(TextDocumentModel model,
      XTextRange range)
  {
    if (range == null) return null;

    // Notwendige Absatzformate definieren (falls nicht bereits definiert)
    createUsedStyles(model.doc);

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));
    XTextCursor createdZuleitung = null;

    boolean deletedAtLeastOne = removeAllZuleitungszeilen(cursor);

    if (!deletedAtLeastOne && UNO.XEnumerationAccess(cursor) != null)
    {
      // Im cursor enthaltene Paragraphen einzeln iterieren und je nach Typ
      // entweder eine Zuleitungszeile oder einen Verf�gungspunkt mit Zuleitung
      // setzen.
      XEnumeration paragraphs = UNO.XEnumerationAccess(cursor)
          .createEnumeration();
      while (paragraphs.hasMoreElements())
      {
        XTextRange par = null;
        try
        {
          par = UNO.XTextRange(paragraphs.nextElement());
        }
        catch (java.lang.Exception e)
        {
        }

        if (par != null)
        {
          if (isAbdruck(par))
          {
            if (cursor.isCollapsed()) // Ignorieren, wenn Bereich ausgew�hlt.
            {
              // Zuleitung in neuer Zeile erzeugen:
              par.getEnd().setString("\r");
              createdZuleitung = par.getText().createTextCursorByRange(
                  par.getEnd());
              if (createdZuleitung != null)
              {
                createdZuleitung.goRight((short) 1, false);
                formatZuleitungszeile(createdZuleitung);
              }
            }
          }
          else if (isVerfuegungspunkt(par))
            formatVerfuegungspunktWithZuleitung(par);
          else
            formatZuleitungszeile(par);
        }
      }
    }
    return createdZuleitung;
  }

  /**
   * Diese Methode l�scht alle Verf�gungspunkte, die der Bereich des Cursors
   * cursor ber�hrt, und liefert true zur�ck, wenn mindestens ein
   * Verf�gungspunkt gel�scht wurde oder false, wenn sich in dem Bereich des
   * Cursors kein Verf�gungspunkt befand.
   * 
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Verf�gungspunkten gesucht wird.
   * 
   * @return true, wenn mindestens ein Verf�gungspunkt gel�scht wurde oder
   *         false, wenn kein der cursor keinen Verf�gungspunkt ber�hrt.
   */
  private static boolean removeAllVerfuegungspunkte(XParagraphCursor cursor)
  {
    boolean deletedAtLeastOne = false;
    if (UNO.XEnumerationAccess(cursor) != null)
    {
      XEnumeration xenum = UNO.XEnumerationAccess(cursor).createEnumeration();

      while (xenum.hasMoreElements())
      {
        XTextRange par = null;
        try
        {
          par = UNO.XTextRange(xenum.nextElement());
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }

        if (par != null)
        {
          boolean isVerfuegungspunktMitZuleitung = isVerfuegungspunktMitZuleitung(par);
          if (isVerfuegungspunkt(par))
          {
            // Einen evtl. bestehenden Verfuegungspunkt zur�cksetzen
            removeSingleVerfuegungspunkt(par);
            deletedAtLeastOne = true;
          }
          if (isVerfuegungspunktMitZuleitung) formatZuleitungszeile(par);
        }
      }
    }
    return deletedAtLeastOne;
  }

  /**
   * Diese Methode l�scht alle Abdruck-Zeilen, die der Bereich des Cursors
   * cursor ber�hrt, und liefert true zur�ck, wenn mindestens ein Abdruck
   * gel�scht wurde oder false, wenn sich in dem Bereich des Cursors kein
   * Abdruck befand.
   * 
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Abdr�cken gesucht wird.
   * 
   * @return true, wenn mindestens ein Abdruck gel�scht wurde oder false, wenn
   *         kein der cursor keinen Verf�gungspunkt ber�hrt.
   */
  private static boolean removeAllAbdruecke(XParagraphCursor cursor)
  {
    boolean deletedAtLeastOne = false;
    if (UNO.XEnumerationAccess(cursor) != null)
    {
      XEnumeration xenum = UNO.XEnumerationAccess(cursor).createEnumeration();

      while (xenum.hasMoreElements())
      {
        XTextRange par = null;
        try
        {
          par = UNO.XTextRange(xenum.nextElement());
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }

        if (par != null)
        {
          if (isAbdruck(par))
          {
            // Einen evtl. bestehenden Verfuegungspunkt zur�cksetzen
            removeSingleVerfuegungspunkt(par);
            deletedAtLeastOne = true;
          }
        }
      }
    }
    return deletedAtLeastOne;
  }

  /**
   * Diese Methode l�scht alle Zuleitungszeilen, die der Bereich des Cursors
   * cursor ber�hrt, und liefert true zur�ck, wenn mindestens eine
   * Zuleitungszeile gel�scht wurde oder false, wenn sich in dem Bereich des
   * Cursors keine Zuleitungszeile befand.
   * 
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Zuleitungszeilen gesucht wird.
   * 
   * @return true, wenn mindestens eine Zuleitungszeile gel�scht wurde oder
   *         false, wenn kein der cursor keine Zuleitungszeile ber�hrt.
   */
  private static boolean removeAllZuleitungszeilen(XParagraphCursor cursor)
  {
    boolean deletedAtLeastOne = false;
    if (UNO.XEnumerationAccess(cursor) != null)
    {
      XEnumeration xenum = UNO.XEnumerationAccess(cursor).createEnumeration();

      while (xenum.hasMoreElements())
      {
        XTextRange par = null;
        try
        {
          par = UNO.XTextRange(xenum.nextElement());
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }

        if (par != null)
        {
          if (isZuleitungszeile(par))
          {
            // Zuleitungszeile zur�cksetzen
            formatDefault(par);
            deletedAtLeastOne = true;
          }
          else if (isVerfuegungspunktMitZuleitung(par))
          {
            // Zuleitung aus Verf�gungspunkt entfernen:
            formatVerfuegungspunkt(par);
            deletedAtLeastOne = true;
          }
        }
      }
    }
    return deletedAtLeastOne;
  }

  /**
   * L�scht die r�mische Ziffer+PUNKT+Tab aus einem als
   * "WollMuxVerfuegungspunkt[...]" markierten Absatz heraus und setzt das
   * Absatzformat auf "Flie�text" zur�ck.
   * 
   * @param par
   *          der Cursor, der sich in der entsprechenden Zeile befinden muss.
   */
  private static void removeSingleVerfuegungspunkt(XTextRange par)
  {
    formatDefault(par);

    // Pr�fe, ob der Absatz mit einer r�mischen Ziffer beginnt.
    XTextCursor zifferOnly = getZifferOnly(par, false);

    // r�mische Ziffer l�schen.
    if (zifferOnly != null) zifferOnly.setString("");

    // wenn es sich bei dem Paragraphen um einen Abdruck handelt, wird dieser
    // vollst�ndig gel�scht.
    if (isAbdruck(par))
    {
      // Den Absatz mit dem String "Ziffer.\tAbdruck von..." l�schen
      par.setString("");

      XParagraphCursor parDeleter = UNO.XParagraphCursor(par.getText()
          .createTextCursorByRange(par.getEnd()));

      // L�scht das Leerzeichen vor dem Abdruck und nach dem Abdruck (falls eine
      // Leerzeile folgt)
      parDeleter.goLeft((short) 1, true);
      parDeleter.setString("");
      if (parDeleter.goRight((short) 1, false) && parDeleter.isEndOfParagraph())
      {
        parDeleter.goLeft((short) 1, true);
        parDeleter.setString("");
      }
    }
  }

  /**
   * Formatiert den �bergebenen Absatz paragraph in der Standardschriftart
   * "Flie�text".
   * 
   * @param paragraph
   */
  private static void formatDefault(XTextRange paragraph)
  {
    UNO.setProperty(paragraph, "ParaStyleName", ParaStyleNameDefault);
    formatRoemischeZifferOnly(paragraph);
  }

  /**
   * Formatiert den �bergebenen Absatz paragraph als Abdruck.
   * 
   * @param paragraph
   */
  private static void formatAbdruck(XTextRange paragraph)
  {
    UNO.setProperty(paragraph, "ParaStyleName", ParaStyleNameAbdruck);
    formatRoemischeZifferOnly(paragraph);
  }

  /**
   * Formatiert den �bergebenen Absatz paragraph als Verf�gungspunkt mit
   * Zuleitungszeile.
   * 
   * @param paragraph
   */
  private static void formatVerfuegungspunktWithZuleitung(XTextRange paragraph)
  {
    UNO.setProperty(
        paragraph,
        "ParaStyleName",
        ParaStyleNameVerfuegungspunktMitZuleitung);
    formatRoemischeZifferOnly(paragraph);
  }

  /**
   * Formatiert den �bergebenen Absatz paragraph als Verf�gungspunkt.
   * 
   * @param paragraph
   */
  private static void formatVerfuegungspunkt(XTextRange paragraph)
  {
    UNO.setProperty(paragraph, "ParaStyleName", ParaStyleNameVerfuegungspunkt);
    formatRoemischeZifferOnly(paragraph);
  }

  /**
   * Formatiert den �bergebenen Absatz paragraph als Zuleitungszeile.
   * 
   * @param paragraph
   */
  private static void formatZuleitungszeile(XTextRange paragraph)
  {
    UNO.setProperty(paragraph, "ParaStyleName", ParaStyleNameZuleitungszeile);
    formatRoemischeZifferOnly(paragraph);
  }

  /**
   * Holt sich aus dem �bergebenen Absatz paragraph nur den Breich der r�mischen
   * Ziffer (+Tab) und formatiert diesen im Zeichenformat
   * WollMuxRoemischeZiffer.
   * 
   * @param paragraph
   */
  private static void formatRoemischeZifferOnly(XTextRange paragraph)
  {
    XTextCursor zifferOnly = getZifferOnly(paragraph, false);
    if (zifferOnly != null)
    {
      UNO
          .setProperty(
              zifferOnly,
              "CharStyleName",
              CharStyleNameRoemischeZiffer);

      // Zeichen danach auf Standardformatierung setzen, damit der Text, der
      // danach geschrieben wird nicht auch obiges Zeichenformat besitzt:
      // ("Standard" gilt laut DevGuide auch in englischen Versionen)
      UNO.setProperty(zifferOnly.getEnd(), "CharStyleName", "Standard");
    }
  }

  /**
   * Liefert true, wenn es sich bei dem �bergebenen Absatz paragraph um einen
   * als Verfuegungspunkt markierten Absatz handelt.
   * 
   * @param paragraph
   *          Das Objekt mit der Property ParaStyleName, die f�r den Vergleich
   *          herangezogen wird.
   * @return true, wenn der Name des Absatzformates mit
   *         "WollMuxVerfuegungspunkt" beginnt.
   */
  private static boolean isVerfuegungspunkt(XTextRange paragraph)
  {
    String paraStyleName = "";
    try
    {
      paraStyleName = AnyConverter.toString(UNO.getProperty(
          paragraph,
          "ParaStyleName"));
    }
    catch (IllegalArgumentException e)
    {
    }
    return paraStyleName.startsWith(ParaStyleNameVerfuegungspunkt);
  }

  /**
   * Liefert true, wenn es sich bei dem �bergebenen Absatz paragraph um einen
   * als VerfuegungspunktMitZuleitung markierten Absatz handelt.
   * 
   * @param paragraph
   *          Das Objekt mit der Property ParaStyleName, die f�r den Vergleich
   *          herangezogen wird.
   * @return true, wenn der Name des Absatzformates mit
   *         "WollMuxVerfuegungspunktMitZuleitung" beginnt.
   */
  private static boolean isVerfuegungspunktMitZuleitung(XTextRange paragraph)
  {
    String paraStyleName = "";
    try
    {
      paraStyleName = AnyConverter.toString(UNO.getProperty(
          paragraph,
          "ParaStyleName"));
    }
    catch (IllegalArgumentException e)
    {
    }
    return paraStyleName.startsWith(ParaStyleNameVerfuegungspunktMitZuleitung);
  }

  /**
   * Liefert true, wenn es sich bei dem �bergebenen Absatz paragraph um einen
   * als Zuleitungszeile markierten Absatz handelt.
   * 
   * @param paragraph
   *          Das Objekt mit der Property ParaStyleName, die f�r den Vergleich
   *          herangezogen wird.
   * @return true, wenn der Name des Absatzformates mit "WollMuxZuleitungszeile"
   *         beginnt.
   */
  private static boolean isZuleitungszeile(XTextRange paragraph)
  {
    String paraStyleName = "";
    try
    {
      paraStyleName = AnyConverter.toString(UNO.getProperty(
          paragraph,
          "ParaStyleName"));
    }
    catch (IllegalArgumentException e)
    {
    }
    return paraStyleName.startsWith(ParaStyleNameZuleitungszeile);
  }

  /**
   * Liefert true, wenn der �bergebene Paragraph paragraph den f�r Abdrucke
   * typischen String in der Form "Abdruck von I[, II, ...][ und n]" enth�lt,
   * andernfalls false.
   * 
   * @param paragraph
   *          der zu pr�fende Paragraph
   * @return
   */
  private static boolean isAbdruck(XTextRange paragraph)
  {
    String str = paragraph.getString();
    return str.contains("Abdruck von I.")
           || str.contains("Abdruck von <Vorg�nger>.");
  }

  /**
   * Z�hlt die Anzahl Verf�gungspunkte im Dokument vor der Position von
   * range.getStart() (einschlie�lich) und liefert deren Anzahl zur�ck, wobei
   * auch ein evtl. vorhandener Rahmen WollMuxVerfuegungspunkt1 mit gez�hlt
   * wird.
   * 
   * @param doc
   *          Das Dokument in dem sich range befindet (wird ben�tigt f�r den
   *          Rahmen WollMuxVerfuegungspunkt1)
   * @param range
   *          Die TextRange, bei der mit der Z�hlung begonnen werden soll.
   * @return die Anzahl Verf�gungspunkte vor und mit range.getStart()
   */
  public static int countVerfPunkteBefore(XTextDocument doc,
      XParagraphCursor range)
  {
    int count = 0;

    // Z�hler f�r Verfuegungspunktnummer auf 1 initialisieren, wenn ein
    // Verfuegungspunkt1 vorhanden ist.
    XTextRange punkt1 = getVerfuegungspunkt1(doc);
    if (punkt1 != null) count++;

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range.getStart()));
    if (cursor != null) do
    {
      if (isVerfuegungspunkt(cursor)) count++;
    } while (cursor.gotoPreviousParagraph(false));

    return count;
  }

  /**
   * Sucht nach allen Abs�tzen im Haupttextbereich des Dokuments doc (also nicht
   * in Frames), deren Absatzformatname mit "WollMuxVerfuegungspunkt" beginnt
   * und numeriert die bereits vorhandenen r�mischen Ziffern neu durch oder
   * erzeugt eine neue Ziffer, wenn in einem entsprechenden Verf�gungspunkt noch
   * keine Ziffer gesetzt wurde. Ist ein Rahmen mit dem Namen
   * WollMuxVerfuegungspunkt1 vorhanden, der einen als Verf�gungpunkt markierten
   * Paragraphen enth�lt, so wird dieser Paragraph immer (gem�� Konzept) als
   * Verf�gungspunkt "I" behandelt.
   * 
   * @param doc
   *          Das Dokument, in dem alle Verf�gungspunkte angepasst werden
   *          sollen.
   */
  public static void ziffernAnpassen(TextDocumentModel model)
  {
    XTextRange punkt1 = getVerfuegungspunkt1(model.doc);

    // Z�hler f�r Verfuegungspunktnummer auf 1 initialisieren, wenn ein
    // Verfuegungspunkt1 vorhanden ist.
    int count = 0;
    if (punkt1 != null) count++;

    // Paragraphen des Texts enumerieren und dabei alle Verfuegungspunkte neu
    // nummerieren. Die Enumeration erfolgt �ber einen ParagraphCursor, da sich
    // dieser stabiler verh�lt als das Durchgehen der XEnumerationAccess, bei
    // der es zu OOo-Abst�rzen kam. Leider konnte ich das Problem nicht exakt
    // genug isolieren um ein entsprechende Ticket bei OOo dazu aufmachen zu
    // k�nnen, da der Absturz nur sporadisch auftrat.
    XParagraphCursor cursor = UNO.XParagraphCursor(model.doc.getText()
        .createTextCursorByRange(model.doc.getText().getStart()));
    if (cursor != null)
      do
      {
        // ganzen Paragraphen markieren
        cursor.gotoEndOfParagraph(true);

        if (isVerfuegungspunkt(cursor))
        {
          count++;

          if (isAbdruck(cursor))
          {
            // Behandlung von Paragraphen mit einem "Abdruck"-String
            String abdruckStr = abdruckString(count);
            if (!cursor.getString().equals(abdruckStr))
            {
              cursor.setString(abdruckStr);
              formatRoemischeZifferOnly(cursor);
            }
          }
          else
          {
            // Behandlung von normalen Verf�gungspunkten:
            String numberStr = romanNumber(count) + "\t";
            XTextRange zifferOnly = getZifferOnly(cursor, false);
            if (zifferOnly != null)
            {
              // Nummer aktualisieren wenn sie nicht mehr stimmt.
              if (!zifferOnly.getString().equals(numberStr))
                zifferOnly.setString(numberStr);
            }
            else
            {
              // Nummer neu anlegen, wenn wie noch gar nicht existierte
              zifferOnly = cursor.getText().createTextCursorByRange(
                  cursor.getStart());
              zifferOnly.setString(numberStr);
              formatRoemischeZifferOnly(zifferOnly);
            }
          }
        }
      } while (cursor.gotoNextParagraph(false));

    // Verfuegungspunt1 setzen
    if (punkt1 != null)
    {
      XTextRange zifferOnly = getZifferOnly(punkt1, false);
      if (zifferOnly != null)
      {
        if (count == 1) zifferOnly.setString("");
      }
      else
      {
        if (count > 1) punkt1.getStart().setString(romanNumbers[0]);
      }
    }

    // Setzte die Druckfunktion SachleitendeVerfuegung wenn mindestens manuell
    // eingef�gter Verf�gungspunkt vorhanden ist. Ansonsten setze die
    // Druckfunktion zur�ck.
    int effectiveCount = (punkt1 != null) ? count - 1 : count;
    if (effectiveCount > 0)
      model.setPrintFunction(PRINT_FUNCTION_NAME);
    else
      model.resetPrintFunction(PRINT_FUNCTION_NAME);
  }

  /**
   * Liefert eine XTextRange, die genau die r�mische Ziffer (falls vorhanden mit
   * darauf folgendem \t-Zeichen) am Beginn eines Absatzes umschlie�t oder null,
   * falls keine Ziffer gefunden wurde. Bei der Suche nach der Ziffer werden nur
   * die ersten 7 Zeichen des Absatzes gepr�ft.
   * 
   * @param par
   *          die TextRange, die den Paragraphen umschlie�t, in dessen Anfang
   *          nach der r�mischen Ziffer gesucht werden soll.
   * @param includeNoTab
   *          ist includeNoTab == true, so enth�lt der cursor immer nur die
   *          Ziffer ohne das darauf folgende Tab-Zeichen.
   * @return die TextRange, die genau die r�mische Ziffer umschlie�t falls eine
   *         gefunden wurde oder null, falls keine Ziffer gefunden wurde.
   */
  private static XTextCursor getZifferOnly(XTextRange par, boolean includeNoTab)
  {
    XParagraphCursor cursor = UNO.XParagraphCursor(par.getText()
        .createTextCursorByRange(par.getStart()));

    for (int i = 0; i < 7; i++)
    {
      String text = "";
      if (!cursor.isEndOfParagraph())
      {
        cursor.goRight((short) 1, true);
        text = cursor.getString();
        if (includeNoTab) text += "\t";
      }
      else
      {
        // auch eine Ziffer erkennen, die nicht mit \t endet.
        text = cursor.getString() + "\t";
      }
      if (text.matches(zifferPattern + "$")) return cursor;
    }

    return null;
  }

  /**
   * Liefert das Textobjekt des TextRahmens WollMuxVerfuegungspunkt1 oder null,
   * falls der Textrahmen nicht existiert. Der gesamte Text innerhalb des
   * Textrahmens wird dabei automatisch mit dem Absatzformat
   * WollMuxVerfuegungspunkt1 vordefiniert.
   * 
   * @param doc
   *          das Dokument, in dem sich der TextRahmen WollMuxVerfuegungspunkt1
   *          befindet (oder nicht).
   * @return Das Textobjekts des TextRahmens WollMuxVerfuegungspunkt1 oder null,
   *         falls der Textrahmen nicht existiert.
   */
  private static XTextRange getVerfuegungspunkt1(XTextDocument doc)
  {
    XTextFrame frame = null;
    try
    {
      frame = UNO.XTextFrame(UNO.XTextFramesSupplier(doc).getTextFrames()
          .getByName(FrameNameVerfuegungspunkt1));
    }
    catch (java.lang.Exception e)
    {
    }

    if (frame != null)
    {
      XTextCursor cursor = frame.getText().createTextCursorByRange(
          frame.getText());
      if (isVerfuegungspunkt(cursor)) return cursor;

      // Absatzformat WollMuxVerfuegungspunkt1 setzen wenn noch nicht gesetzt.
      UNO.setProperty(cursor, "ParaStyleName", ParaStyleNameVerfuegungspunkt1);
      return cursor;
    }
    else
      return null;
  }

  /**
   * Erzeugt einen String in der Form "i.<tab>Abdruck von I.[, II., ...][ und
   * <i-1>]", der passend zu einem Abdruck mit der Verf�gungsnummer number
   * angezeigt werden soll.
   * 
   * @param number
   *          Die Nummer des Verf�gungspunktes des Abdrucks
   * @return String in der Form "Abdruck von I.[, II., ...][ und <i-1>]" oder
   *         AbdruckDefaultStr, wenn der Verf�gungspunkt bei i==0 und i==1
   *         keinen Vorg�nger besitzen kann.
   */
  private static String abdruckString(int number)
  {
    String str = romanNumber(number) + "\t" + "Abdruck von " + romanNumber(1);
    for (int j = 2; j < (number - 1); ++j)
      str += ", " + romanNumber(j);
    if (number >= 3) str += " und " + romanNumber(number - 1);
    return str;
  }

  /**
   * Liefert die r�mische Zahl zum �bgebenen integer Wert i. Die r�mischen
   * Zahlen werden dabei aus dem begrenzten Array romanNumbers ausgelesen. Ist i
   * kein g�ltiger Index des Arrays, so sieht der R�ckgabewert wie folgt aus "<dezimalzahl(i)>.".
   * Hier kann bei Notwendigkeit nat�rlich auch ein Berechnungsschema f�r
   * r�mische Zahlen implementiert werden, was f�r die Sachleitenden Verf�gungen
   * vermutlich aber nicht erforderlich sein wird.
   * 
   * @param i
   *          Die Zahl, zu der eine r�mische Zahl geliefert werden soll.
   * @return Die r�mische Zahl, oder "<dezimalzahl(i)>, wenn i nicht in den
   *         Arraygrenzen von romanNumbers.
   */
  private static String romanNumber(int i)
  {
    String number = "" + i + ".";
    if (i > 0 && i < romanNumbers.length) number = romanNumbers[i - 1];
    return number;
  }

  /**
   * Erzeugt einen Vector mit Elementen vom Typ Verfuegungspunkt, der dem
   * Druckdialog �bergeben werden kann und alle f�r den Druckdialog notwendigen
   * Informationen enth�lt.
   * 
   * @param doc
   *          Das zu scannende Dokument
   * @return Vector of Verfuegungspunkt, der f�r jeden Verfuegungspunkt im
   *         Dokument doc einen Eintrag enth�lt.
   */
  private static Vector scanVerfuegungspunkte(XTextDocument doc)
  {
    Vector verfuegungspunkte = new Vector();

    // Verf�gungspunkt1 hinzuf�gen wenn verf�gbar.
    XTextRange punkt1 = getVerfuegungspunkt1(doc);
    if (punkt1 != null)
    {
      Verfuegungspunkt original = new Verfuegungspunkt("I. Original");
      original.addZuleitungszeile("Empf�nger siehe Empf�ngerfeld");
      verfuegungspunkte.add(original);
    }

    Verfuegungspunkt currentVerfpunkt = null;

    // Paragraphen des Texts enumerieren und Verf�gungspunkte erstellen. Die
    // Enumeration erfolgt �ber einen ParagraphCursor, da sich
    // dieser stabiler verh�lt als das Durchgehen der XEnumerationAccess, bei
    // der es zu OOo-Abst�rzen kam. Leider konnte ich das Problem nicht exakt
    // genug isolieren um ein entsprechende Ticket bei OOo dazu aufmachen zu
    // k�nnen, da der Absturz nur sporadisch auftrat.
    XParagraphCursor cursor = UNO.XParagraphCursor(doc.getText()
        .createTextCursorByRange(doc.getText().getStart()));

    if (cursor != null)
      do
      {
        // ganzen Paragraphen markieren
        cursor.gotoEndOfParagraph(true);

        if (isVerfuegungspunkt(cursor))
        {
          String heading = cursor.getString();
          currentVerfpunkt = new Verfuegungspunkt(heading);
          currentVerfpunkt.setMinNumberOfCopies(1);
          verfuegungspunkte.add(currentVerfpunkt);
        }

        // Zuleitungszeilen hinzuf�gen (auch wenn der Paragraph Verf�gungspunkt
        // und Zuleitungszeile zugleich ist)
        if ((isZuleitungszeile(cursor) || isVerfuegungspunktMitZuleitung(cursor))
            && currentVerfpunkt != null)
        {
          String zuleit = cursor.getString();
          // nicht leere Zuleitungszeilen zum Verf�gungspunkt hinzuf�gen.
          if (!(zuleit.length() == 0))
            currentVerfpunkt.addZuleitungszeile(zuleit);
        }
      } while (cursor.gotoNextParagraph(false));

    return verfuegungspunkte;
  }

  /**
   * Repr�sentiert einen vollst�ndigen Verf�gungspunkt, der aus �berschrift
   * (r�mische Ziffer + �berschrift) und Inhalt besteht. Die Klasse bietet
   * Methden an, �ber die auf alle f�r den Druck wichtigen Eigenschaften des
   * Verf�gungspunktes zugegriffen werden kann (z.B. �berschrift, Anzahl
   * Zuleitungszeilen, ...)
   * 
   * @author christoph.lutz
   */
  public static class Verfuegungspunkt
  {
    /**
     * Enth�lt den vollst�ndigen Text der erste Zeile des Verf�gungspunktes
     * einschlie�lich der r�mischen Ziffer.
     */
    protected String heading;

    /**
     * Vector of String, der alle Zuleitungszeilen enth�lt, die mit addParagraph
     * hinzugef�gt wurden.
     */
    protected Vector zuleitungszeilen;

    /**
     * Enth�lt die Anzahl der Ausdrucke, die mindestens ausgedruckt werden
     * sollen.
     */
    protected int minNumberOfCopies;

    /**
     * Erzeugt einen neuen Verf�gungspunkt, wobei firstPar der Absatz ist, der
     * die erste Zeile mit der r�mischen Ziffer und der �berschrift enth�lt.
     * 
     * @param heading
     *          Text der ersten Zeile des Verf�gungspunktes mit der r�mischen
     *          Ziffer und der �berschrift.
     */
    public Verfuegungspunkt(String heading)
    {
      this.heading = heading;
      this.zuleitungszeilen = new Vector();
      this.minNumberOfCopies = 0;
    }

    /**
     * F�gt einen weitere Zuleitungszeile des Verf�gungspunktes hinzu (wenn
     * paragraph nicht null ist).
     * 
     * @param paragraph
     *          XTextRange, das den gesamten Paragraphen der Zuleitungszeile
     *          enth�lt.
     */
    public void addZuleitungszeile(String zuleitung)
    {
      zuleitungszeilen.add(zuleitung);
    }

    /**
     * Liefert die Anzahl der Ausfertigungen zur�ck, mit denen der
     * Verf�gungspunkt ausgeduckt werden soll; Die Anzahl erh�ht sich mit jeder
     * hinzugef�gten Zuleitungszeile. Der Mindestwert kann mit
     * setMinNumberOfCopies gesetzt werden.
     * 
     * @return Anzahl der Ausfertigungen mit denen der Verf�gungspunkt gedruckt
     *         werden soll.
     */
    public int getNumberOfCopies()
    {
      if (zuleitungszeilen.size() > minNumberOfCopies)
        return zuleitungszeilen.size();
      else
        return minNumberOfCopies;
    }

    /**
     * Setzt die Anzahl der Ausfertigungen, die Mindestens ausgedruckt werden
     * sollen, auch dann wenn z.B. keine Zuleitungszeilen vorhanden sind.
     * 
     * @param minNumberOfCopies
     *          Anzahl der Ausfertigungen mit denen der Verf�gungspunkt
     *          mindestens ausgedruckt werden soll.
     */
    public void setMinNumberOfCopies(int minNumberOfCopies)
    {
      this.minNumberOfCopies = minNumberOfCopies;
    }

    /**
     * Liefert einen Vector of Strings, der die Texte der Zuleitungszeilen
     * beinhaltet, die dem Verf�gungspunkt mit addParagraph hinzugef�gt wurden.
     * 
     * @return Vector of Strings mit den Texten der Zuleitungszeilen.
     */
    public Vector getZuleitungszeilen()
    {
      return zuleitungszeilen;
    }

    /**
     * Liefert den Text der �berschrift des Verf�gungspunktes einschlie�lich der
     * r�mischen Ziffer als String zur�ck, wobei mehrfache Leerzeichen (\s+)
     * durch einfache Leerzeichen ersetzt werden.
     * 
     * @return r�mischer Ziffer + �berschrift
     */
    public String getHeading()
    {
      String text = heading;

      // Tabs und Spaces durch single spaces ersetzen
      text = text.replaceAll("\\s+", " ");

      return text;
    }
  }

  /**
   * Zeigt den Druckdialog f�r Sachleitende Verf�gungen an und druckt das
   * Dokument gem�� Druckdialog in den gew�nschten Ausfertigungen aus.
   * 
   * ACHTUNG: Diese Methode l�uft nicht im WollMuxEventHandler-Thread, und daher
   * darf nur auf die Daten zugegriffen werden, die pmod anbietet.
   * 
   * @param pmod
   */
  public static void showPrintDialog(XPrintModel pmod)
  {
    Logger.debug("SachleitendeVerfuegung.print - started");

    Vector vps = scanVerfuegungspunkte(pmod.getTextDocument());
    Iterator iter = vps.iterator();
    while (iter.hasNext())
    {
      Verfuegungspunkt vp = (Verfuegungspunkt) iter.next();
      String text = "Verf�gungspunkt '" + vp.getHeading() + "'";
      Iterator zuleits = vp.getZuleitungszeilen().iterator();
      while (zuleits.hasNext())
      {
        String zuleit = (String) zuleits.next();
        text += "\n  --> '" + zuleit + "'";
      }
      Logger.debug2(text);
    }

    // Beschreibung des Druckdialogs auslesen.
    ConfigThingy conf = WollMuxSingleton.getInstance().getWollmuxConf();
    ConfigThingy svdds = conf.query("Dialoge").query(
        "SachleitendeVerfuegungenDruckdialog");
    ConfigThingy printDialogConf = null;
    try
    {
      printDialogConf = svdds.getLastChild();
    }
    catch (NodeNotFoundException e)
    {
      Logger
          .error(
              "Fehlende Dialogbeschreibung f�r den Dialog 'SachleitendeVerfuegungenDruckdialog'.",
              e);
      return;
    }

    // Druckdialog starten
    try
    {
      new SachleitendeVerfuegungenDruckdialog(printDialogConf, vps, pmod, null);
    }
    catch (ConfigurationErrorException e)
    {
      Logger.error(e);
      return;
    }

    // pmod.print((short)1);
    Logger.debug("SachleitendeVerfuegung.print - finished");
  }

  /**
   * Liefert die Anzahl der im XTextDocument doc enthaltenen Verf�gungspunkte
   * zur�ck.
   * 
   * @param doc
   *          das TextDocument in dem gez�hlt werden soll.
   * @return die Anzahl der im XTextDocument doc enthaltenen Verf�gungspunkte
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static int countVerfuegungspunkte(XTextDocument doc)
  {
    if (doc != null)
      return scanVerfuegungspunkte(doc).size();
    else
      return 0;
  }

  /**
   * Druckt den Verf�gungpunkt verfPunkt aus dem Dokument doc in der gew�nschten
   * Anzahl numberOfCopies aus. ACHTUNG: Diese Methode darf nur aus dem
   * WollMuxEventHandler-Thread gestartet werden, da sie auf Datenstrukturen des
   * WollMux zugreift.
   * 
   * @param model
   *          Das TextDocumentModel, dessen Inhalt Sachleitende Verf�gungen
   *          enth�lt und ausgedruckt werden soll.
   * @param verfPunkt
   *          Die Nummer des auszuduruckenden Verf�gungspunktes, wobei alle
   *          folgenden Verf�gungspunkte ausgeblendet werden.
   * @param numberOfCopies
   *          Die Anzahl der Ausfertigungen, in der verfPunkt ausgedruckt werden
   *          soll.
   * @param isDraft
   *          wenn isDraft==true, werden alle draftOnly-Bl�cke eingeblendet,
   *          ansonsten werden sie ausgeblendet.
   * @param isOriginal
   *          wenn isOriginal, wird die Ziffer des Verf�gungspunktes I
   *          ausgeblendet und alle notInOriginal-Bl�cke ebenso. Andernfalls
   *          sind Ziffer und notInOriginal-Bl�cke eingeblendet.
   * @throws PrintFailedException
   */
  public static void printVerfuegungspunkt(TextDocumentModel model,
      short verfPunkt, short numberOfCopies, boolean isDraft,
      boolean isOriginal, int pageRangeType, String pageRangeValue)
      throws PrintFailedException
  {
    // Z�hler f�r Verfuegungspunktnummer auf 1 initialisieren, wenn ein
    // Verfuegungspunkt1 vorhanden ist.
    XTextRange punkt1 = getVerfuegungspunkt1(model.doc);
    int count = 0;
    if (punkt1 != null) count++;

    // Auszublendenden Bereich festlegen:
    XTextRange setInvisibleRange = null;
    XTextRange lastVisibleVerfPunkt = null;
    XParagraphCursor cursor = UNO.XParagraphCursor(model.doc.getText()
        .createTextCursorByRange(model.doc.getText().getStart()));
    if (cursor != null)
      do
      {
        cursor.gotoEndOfParagraph(true);

        if (isVerfuegungspunkt(cursor))
        {
          // Punkt1 merken
          if (punkt1 == null)
            punkt1 = cursor.getText().createTextCursorByRange(cursor);

          count++;

          // range des zuletzt sichtbaren Verf�gungspunktes sichern
          if (count == verfPunkt)
            lastVisibleVerfPunkt = cursor.getText().createTextCursorByRange(
                cursor);

          if (count == (verfPunkt + 1))
          {
            cursor.collapseToStart();
            cursor.gotoRange(cursor.getText().getEnd(), true);
            setInvisibleRange = cursor;
          }
        }
      } while (setInvisibleRange == null && cursor.gotoNextParagraph(false));

    // ensprechende Verf�gungspunkte ausblenden
    if (setInvisibleRange != null)
    {
      UNO.setProperty(setInvisibleRange, "CharHidden", Boolean.TRUE);
      // Workaround f�r update Bug
      // http://qa.openoffice.org/issues/show_bug.cgi?id=78896
      UNO.setProperty(setInvisibleRange, "CharHidden", Boolean.FALSE);
      UNO.setProperty(setInvisibleRange, "CharHidden", Boolean.TRUE);
    }

    // Sichtbarkeitsstand der all, draftOnly bzw. notInOriginal-Bl�cke und
    // merken.
    HashMap /* of DocumentCommand */oldVisibilityStates = new HashMap();

    // Ein/Ausblenden der draftOnly bzw. notInOriginal-Bl�cke:
    for (Iterator iter = model.getDraftOnlyBlocksIterator(); iter.hasNext();)
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      oldVisibilityStates.put(cmd, new Boolean(cmd.isVisible()));
      cmd.setVisible(isDraft);
    }

    for (Iterator iter = model.getNotInOrininalBlocksIterator(); iter.hasNext();)
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      oldVisibilityStates.put(cmd, new Boolean(cmd.isVisible()));
      cmd.setVisible(!isOriginal);
    }

    for (Iterator iter = model.getAllVersionsBlocksIterator(); iter.hasNext();)
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      oldVisibilityStates.put(cmd, new Boolean(cmd.isVisible()));
      cmd.setVisible(true);
    }

    // Hintergrundfarbe der Printbl�cke aufheben, wenn eine gesetzt ist.
    for (Iterator iter = oldVisibilityStates.keySet().iterator(); iter
        .hasNext();)
    {
      OptionalHighlightColorProvider cmd = (OptionalHighlightColorProvider) iter
          .next();
      if (cmd.getHighlightColor() != null)
        UNO.setPropertyToDefault(cmd.getTextRange(), "CharBackColor");
    }

    // Ziffer von Punkt 1 ausblenden falls isOriginal
    XTextRange punkt1ZifferOnly = null;
    if (isOriginal && punkt1 != null)
    {
      punkt1ZifferOnly = getZifferOnly(punkt1, true);
      UNO.setProperty(punkt1ZifferOnly, "CharHidden", Boolean.TRUE);
    }

    // f�gt einen Seitenumbruch vor dem letzten Verf�gungspunkt ein, wenn Teile
    // des Punktes auf die n�chste Seite umgebrochen werden m�ssen.
//    boolean insertedPageBreak = false;                        
    boolean insertedPageBreak = insertPageBreakIfNecessary(
        model,
        lastVisibleVerfPunkt);

    // -----------------------------------------------------------------------
    // Druck des Dokuments
    // -----------------------------------------------------------------------
    model.printWithPageRange(numberOfCopies, pageRangeType, pageRangeValue);

    // zuvor eingef�gten Seitenumbruch wieder entfernen:
    if (insertedPageBreak && lastVisibleVerfPunkt != null)
    {
      UNO.setProperty(lastVisibleVerfPunkt, "PageDescName", "");
    }

    // Ausblendung von Ziffer von Punkt 1 wieder aufheben
    if (punkt1ZifferOnly != null)
      UNO.setProperty(punkt1ZifferOnly, "CharHidden", Boolean.FALSE);

    // Alte Hintergrundfarben der Sichtbarkeitsbl�cke wieder herstellen, wenn
    // welche gesetzt sind:
    for (Iterator iter = oldVisibilityStates.keySet().iterator(); iter
        .hasNext();)
    {
      OptionalHighlightColorProvider cmd = (OptionalHighlightColorProvider) iter
          .next();
      if (cmd.getHighlightColor() != null)
      {
        try
        {
          Integer bgColor = new Integer(Integer.parseInt(cmd
              .getHighlightColor(), 16));
          UNO.setProperty(cmd.getTextRange(), "CharBackColor", bgColor);
        }
        catch (NumberFormatException e)
        {
          Logger.error("Fehler in Dokumentkommando '"
                       + cmd
                       + "': Die Farbe HIGHLIGHT_COLOR mit dem Wert '"
                       + cmd.getHighlightColor()
                       + "' ist ung�ltig.");
        }
      }
    }

    // Alte Sichtbarkeitszust�nde der draftOnly bzw. notInOriginal-Bl�cke
    // zur�cksetzten.
    for (Iterator iter = oldVisibilityStates.keySet().iterator(); iter
        .hasNext();)
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      cmd.setVisible(((Boolean) oldVisibilityStates.get(cmd)).booleanValue());
    }

    // Verf�gungspunkte wieder einblenden:
    if (setInvisibleRange != null)
      UNO.setProperty(setInvisibleRange, "CharHidden", Boolean.FALSE);
  }

  /**
   * F�gt einen Seitenumbruch vor dem Absatz par ein, wenn der Text ab par bis
   * zum Dokumentende nicht auf die selbe Seite passt und Teile des Rests
   * dadurch alleine stehen w�rden. Der Seitenumbruch wird nicht eingebaut, wenn
   * sich der Rest ab par sowieso �ber mehr als eine Seite erstreckt, da in
   * diesem Fall ein auseinanderreissen unvermeidbar ist.
   * 
   * @param model
   *          Das TextDocumentModel, in dem sich par befindet.
   * @param par
   *          Der Absatz, vor dem der PageBreak bei Bedarf eingef�gt werden
   *          soll.
   * @return true, wenn ein Seitenumbruch eingef�gt wurde, ansonsten false.
   */
  private static boolean insertPageBreakIfNecessary(TextDocumentModel model,
      XTextRange par)
  {
    XTextCursor viewCursor = model.getViewCursor();
    XTextCursor oldViewCursor = null;
    try
    {
      // Dieser Aufruf fliegt auf die Schnauze, wenn der Cursor nicht im Text
      // steht, sondern z.b. ein Rahmen markiert ist. Dann kann der viewCursor
      // so nicht verwendet werden.
      oldViewCursor = viewCursor.getText().createTextCursorByRange(viewCursor);
    }
    catch (java.lang.Exception e)
    {
    }

    boolean insertedPageBreak = false;

    if (par != null && viewCursor != null && oldViewCursor != null)
    {
      // Gesamtseitenzahl des Dokuments und Seitennummer von par bestimmen
      viewCursor.gotoRange(par, false);
      int a = model.getPageCount();
      int b = getPageOfViewCursor(viewCursor);
      viewCursor.gotoRange(oldViewCursor, false);

      // Seitenumbruch einf�gen, wenn die Seitenzahlen der beiden Positionen
      // sich unterscheiden.
      if (a != 0 && b != 0 && a != b)
      {
        Object pageDescName = UNO.getProperty(par, "PageDescName");
        if (pageDescName != null && !pageDescName.toString().equals(""))
        {
          Object pageStyleName = UNO.getProperty(par, "PageStyleName");
          // ist diese Property gesetzt, so wird der Seitenumbruch eingef�gt:
          UNO.setProperty(par, "PageDescName", pageStyleName);
          UNO.setProperty(par, "PageNumberOffset", new Short((short) (b + 1)));
          insertedPageBreak = true;
        }

        // nochmal die Seitenzahlen bestimmen:
        viewCursor.gotoRange(par, false);
        a = model.getPageCount();
        b = getPageOfViewCursor(viewCursor);
        viewCursor.gotoRange(oldViewCursor, false);

        // Wenn der Rest auch mit Umbruch nicht draufpasst, wird der Umbruch
        // wieder rausgenommen, da er eh nichts bringt.
        if (a != 0 && b != 0 && a != b)
        {
          UNO.setProperty(par, "PageDescName", "");
          insertedPageBreak = false;
        }
      }
    }
    return insertedPageBreak;
  }

  /**
   * Liefert die Seite (>0) in der sich der ViewCursor viewCursor befindet oder
   * 0, falls der ViewCursor diese Information nicht bereitstellen kann.
   * 
   * @param viewCursor
   * @return
   */
  private static int getPageOfViewCursor(XTextCursor viewCursor)
  {
    if (UNO.XPageCursor(viewCursor) == null) return 0;
    return UNO.XPageCursor(viewCursor).getPage();
  }

  /**
   * Liefert das Absatzformat (=ParagraphStyle) des Dokuments doc mit dem Namen
   * name oder null, falls das Absatzformat nicht definiert ist.
   * 
   * @param doc
   *          das Dokument in dem nach einem Absatzformat name gesucht werden
   *          soll.
   * @param name
   *          der Name des gesuchten Absatzformates
   * @return das Absatzformat des Dokuments doc mit dem Namen name oder null,
   *         falls das Absatzformat nicht definiert ist.
   */
  private static XStyle getParagraphStyle(XTextDocument doc, String name)
  {
    XStyle style = null;

    XNameContainer pss = getStyleContainer(doc, PARAGRAPH_STYLES);
    if (pss != null) try
    {
      style = UNO.XStyle(pss.getByName(name));
    }
    catch (java.lang.Exception e)
    {
    }
    return style;
  }

  /**
   * Erzeugt im Dokument doc ein neues Absatzformat (=ParagraphStyle) mit dem
   * Namen name und dem ParentStyle parentStyleName und liefert das neu erzeugte
   * Absatzformat zur�ck oder null, falls das Erzeugen nicht funktionierte.
   * 
   * @param doc
   *          das Dokument in dem das Absatzformat name erzeugt werden soll.
   * @param name
   *          der Name des zu erzeugenden Absatzformates
   * @param parentStyleName
   *          Name des Vorg�nger-Styles von dem die Eigenschaften dieses Styles
   *          abgeleitet werden soll oder null, wenn kein Vorg�nger gesetzt
   *          werden soll (in diesem Fall wird automatisch "Standard" verwendet)
   * @return das neu erzeugte Absatzformat oder null, falls das Absatzformat
   *         nicht erzeugt werden konnte.
   */
  private static XStyle createParagraphStyle(XTextDocument doc, String name,
      String parentStyleName)
  {
    XNameContainer pss = getStyleContainer(doc, PARAGRAPH_STYLES);
    XStyle style = null;
    try
    {
      style = UNO.XStyle(UNO.XMultiServiceFactory(doc).createInstance(
          "com.sun.star.style.ParagraphStyle"));
      pss.insertByName(name, style);
      if (style != null && parentStyleName != null)
        style.setParentStyle(parentStyleName);
      return UNO.XStyle(pss.getByName(name));
    }
    catch (Exception e)
    {
    }
    return null;
  }

  /**
   * Liefert das Zeichenformat (=CharacterStyle) des Dokuments doc mit dem Namen
   * name oder null, falls das Format nicht definiert ist.
   * 
   * @param doc
   *          das Dokument in dem nach einem Zeichenformat name gesucht werden
   *          soll.
   * @param name
   *          der Name des gesuchten Zeichenformates
   * @return das Zeichenformat des Dokuments doc mit dem Namen name oder null,
   *         falls das Absatzformat nicht definiert ist.
   */
  private static XStyle getCharacterStyle(XTextDocument doc, String name)
  {
    XStyle style = null;

    XNameContainer pss = getStyleContainer(doc, CHARACTER_STYLES);
    if (pss != null) try
    {
      style = UNO.XStyle(pss.getByName(name));
    }
    catch (java.lang.Exception e)
    {
    }
    return style;
  }

  /**
   * Erzeugt im Dokument doc ein neues Zeichenformat (=CharacterStyle) mit dem
   * Namen name und dem ParentStyle parentStyleName und liefert das neu erzeugte
   * Zeichenformat zur�ck oder null, falls das Erzeugen nicht funktionierte.
   * 
   * @param doc
   *          das Dokument in dem das Zeichenformat name erzeugt werden soll.
   * @param name
   *          der Name des zu erzeugenden Zeichenformates
   * @param parentStyleName
   *          Name des Vorg�nger-Styles von dem die Eigenschaften dieses Styles
   *          abgeleitet werden soll oder null, wenn kein Vorg�nger gesetzt
   *          werden soll (in diesem Fall wird automatisch "Standard" verwendet)
   * @return das neu erzeugte Zeichenformat oder null, falls das Zeichenformat
   *         nicht erzeugt werden konnte.
   */
  private static XStyle createCharacterStyle(XTextDocument doc, String name,
      String parentStyleName)
  {
    XNameContainer pss = getStyleContainer(doc, CHARACTER_STYLES);
    XStyle style = null;
    try
    {
      style = UNO.XStyle(UNO.XMultiServiceFactory(doc).createInstance(
          "com.sun.star.style.CharacterStyle"));
      pss.insertByName(name, style);
      if (style != null && parentStyleName != null)
        style.setParentStyle(parentStyleName);
      return UNO.XStyle(pss.getByName(name));
    }
    catch (Exception e)
    {
    }
    return null;
  }

  /**
   * Liefert den Styles vom Typ type des Dokuments doc.
   * 
   * @param doc
   *          Das Dokument, dessen StyleContainer zur�ckgeliefert werden soll.
   * @param type
   *          kann z.B. CHARACTER_STYLE oder PARAGRAPH_STYLE sein.
   * @return Liefert den Container der Styles vom Typ type des Dokuments doc
   *         oder null, falls der Container nicht bestimmt werden konnte.
   */
  private static XNameContainer getStyleContainer(XTextDocument doc,
      String containerName)
  {
    try
    {
      return UNO.XNameContainer(UNO.XNameAccess(
          UNO.XStyleFamiliesSupplier(doc).getStyleFamilies()).getByName(
          containerName));
    }
    catch (java.lang.Exception e)
    {
    }
    return null;
  }

  /**
   * Falls die f�r Sachleitenden Verf�gungen notwendigen Absatz- und
   * Zeichenformate nicht bereits existieren, werden sie hier erzeugt und mit
   * fest verdrahteten Werten vorbelegt.
   * 
   * @param doc
   */
  private static void createUsedStyles(XTextDocument doc)
  {
    XStyle style = null;

    // Absatzformate:

    style = getParagraphStyle(doc, ParaStyleNameDefault);
    if (style == null)
    {
      style = createParagraphStyle(doc, ParaStyleNameDefault, null);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "CharHeight", new Integer(11));
      UNO.setProperty(style, "CharFontName", "Arial");
    }

    style = getParagraphStyle(doc, ParaStyleNameVerfuegungspunkt);
    if (style == null)
    {
      style = createParagraphStyle(
          doc,
          ParaStyleNameVerfuegungspunkt,
          ParaStyleNameDefault);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "CharWeight", new Float(FontWeight.BOLD));
      UNO.setProperty(style, "ParaFirstLineIndent", new Integer(-700));
      UNO.setProperty(style, "ParaTopMargin", new Integer(460));
    }

    style = getParagraphStyle(doc, ParaStyleNameVerfuegungspunkt1);
    if (style == null)
    {
      style = createParagraphStyle(
          doc,
          ParaStyleNameVerfuegungspunkt1,
          ParaStyleNameVerfuegungspunkt);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "ParaFirstLineIndent", new Integer(0));
      UNO.setProperty(style, "ParaTopMargin", new Integer(0));
    }

    style = getParagraphStyle(doc, ParaStyleNameAbdruck);
    if (style == null)
    {
      style = createParagraphStyle(
          doc,
          ParaStyleNameAbdruck,
          ParaStyleNameVerfuegungspunkt);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "CharWeight", new Integer(100));
      UNO.setProperty(style, "ParaFirstLineIndent", new Integer(-700));
      UNO.setProperty(style, "ParaTopMargin", new Integer(460));
    }

    style = getParagraphStyle(doc, ParaStyleNameZuleitungszeile);
    if (style == null)
    {
      style = createParagraphStyle(
          doc,
          ParaStyleNameZuleitungszeile,
          ParaStyleNameDefault);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "CharUnderline", new Integer(1));
      UNO.setProperty(style, "CharWeight", new Float(FontWeight.BOLD));
    }

    style = getParagraphStyle(doc, ParaStyleNameVerfuegungspunktMitZuleitung);
    if (style == null)
    {
      style = createParagraphStyle(
          doc,
          ParaStyleNameVerfuegungspunktMitZuleitung,
          ParaStyleNameVerfuegungspunkt);
      UNO.setProperty(style, "FollowStyle", ParaStyleNameDefault);
      UNO.setProperty(style, "CharUnderline", new Integer(1));
    }

    // Zeichenformate:

    style = getCharacterStyle(doc, CharStyleNameDefault);
    if (style == null)
    {
      style = createCharacterStyle(doc, CharStyleNameDefault, null);
      UNO.setProperty(style, "FollowStyle", CharStyleNameDefault);
      UNO.setProperty(style, "CharHeight", new Integer(11));
      UNO.setProperty(style, "CharFontName", "Arial");
      UNO.setProperty(style, "CharUnderline", new Integer(0));
    }

    style = getCharacterStyle(doc, CharStyleNameRoemischeZiffer);
    if (style == null)
    {
      style = createCharacterStyle(
          doc,
          CharStyleNameRoemischeZiffer,
          CharStyleNameDefault);
      UNO.setProperty(style, "FollowStyle", CharStyleNameDefault);
      UNO.setProperty(style, "CharWeight", new Float(FontWeight.BOLD));
    }
  }
}
