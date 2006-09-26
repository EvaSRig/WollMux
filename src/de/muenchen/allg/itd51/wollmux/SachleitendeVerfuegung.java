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

import com.sun.star.container.XEnumeration;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;

public class SachleitendeVerfuegung
{
  private static final String PARA_STYLE_NAME_DEFAULT = "Flie�text";

  private static final String FRAME_NAME_VERFUEGUNGSPUNKT1 = "WollMuxVerfuegungspunkt1";

  /**
   * Enth�lt einen Vector mit den ersten 20 r�mischen Ziffern. Mehr wird in
   * Sachleitenden Verf�gungen sicherlich nicht ben�tigt :-)
   */
  private static String[] romanNumbers = new String[] {
                                                       "I.",
                                                       "II.",
                                                       "III.",
                                                       "IV.",
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
   * Erzeugt einen neuen Absatz am Ende des Absatzes der range ber�hrt und setzt
   * das Absatzformat f�r den neuen Absatz auf "WollMuxVerfuegungspunkt" ODER
   * l�scht einen bestehenden Verf�gungspunkt, wenn der Paragraph, in der sich
   * range befindet, bereits das Absatzformat "WollMuxVerf�gungspunkt[...]"
   * besitzt.
   * 
   * @param range
   *          Die XTextRange, in der sich zum Zeitpunkt des Aufrufs der Cursor
   *          befindet.
   */
  public static void verfuegungspunktEinfuegen(XTextDocument doc,
      XTextRange range)
  {
    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));

    // Einen evtl. bestehenden Verfuegungspunkt zur�cksetzen
    if (isVerfuegungspunkt(cursor))
    {
      loescheVerfuegungspunkt(cursor);
      return;
    }

    // Verfuegungspunkt im n�chsten Paragraph erzeugen.
    cursor.gotoEndOfParagraph(false);
    try
    {
      cursor.getText().insertControlCharacter(
          cursor,
          com.sun.star.text.ControlCharacter.PARAGRAPH_BREAK,
          true);
    }
    catch (IllegalArgumentException e)
    {
      Logger.error(e);
    }

    cursor.gotoNextParagraph(false);
    UNO.setProperty(cursor, "ParaStyleName", "WollMuxVerfuegungspunkt");

    // Ziffernanpassung durchf�hren:
    ziffernAnpassen(doc);
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
  private static boolean isVerfuegungspunkt(Object paragraph)
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
    return paraStyleName.startsWith("WollMuxVerfuegungspunkt");
  }

  /**
   * L�scht die r�mische Ziffer+PUNKT+Tab aus einem als
   * "WollMuxVerfuegungspunkt[...]" markierten Absatz heraus und setzt das
   * Absatzformat auf "Flie�text" zur�ck.
   * 
   * @param par
   *          der Cursor, der sich in der entsprechenden Zeile befinden muss.
   */
  private static void loescheVerfuegungspunkt(XParagraphCursor par)
  {
    UNO.setProperty(par, "ParaStyleName", PARA_STYLE_NAME_DEFAULT);

    // Pr�fe, ob der Absatz mit einer r�mischen Ziffer beginnt.
    XTextCursor zifferOnly = getZifferOnly(par);
    if (zifferOnly != null)
    {
      // r�mische Ziffer l�schen.
      zifferOnly.setString("");

      // wenn n�chstes Zeichen ein Whitespace-Zeichen ist, wird dieses gel�scht
      zifferOnly.goRight((short) 1, true);
      if (zifferOnly.getString().matches("\\s")) zifferOnly.setString("");
    }
  }

  public static void ziffernAnpassen(XTextDocument doc)
  {
    XTextRange punkt1 = getVerfuegungspunkt1(doc);

    // Z�hler f�r Verfuegungspunktnummer auf 1 initialisieren, wenn ein
    // Verfuegungspunkt1 vorhanden ist.
    int count = (punkt1 == null) ? 0 : 1;

    // Paragraphen des Texts enumerieren und dabei alle Verfuegungspunkte neu
    // numerieren.
    if (UNO.XEnumerationAccess(doc.getText()) != null)
    {
      XEnumeration xenum = UNO.XEnumerationAccess(doc.getText())
          .createEnumeration();
      while (xenum.hasMoreElements())
      {
        XTextRange paragraph = null;
        try
        {
          paragraph = UNO.XTextRange(xenum.nextElement());
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }

        if (paragraph != null && isVerfuegungspunkt(paragraph))
        {
          // String mit r�hmischer Zahl erzeugen
          String number = "" + (count + 1) + ".";
          if (count < romanNumbers.length) number = romanNumbers[count];
          count++;

          // Neue nummer setzen:
          XTextRange zifferOnly = getZifferOnly(paragraph);
          if (zifferOnly != null)
          {
            zifferOnly.setString(number);
          }
          else
          {
            paragraph.getStart().setString(number + "\t");
          }
        }
      }
    }

    // Verfuegungspunt1 setzen
    if (punkt1 != null)
    {
      XTextRange zifferOnly = getZifferOnly(punkt1);
      if (zifferOnly != null)
      {
        if (count > 1)
          zifferOnly.setString(romanNumbers[0]);
        else
          punkt1.getStart().setString(romanNumbers[0]);
      }
    }
  }

  /**
   * Liefert eine XTextRange, die genau die r�mische Ziffer am Beginn eines
   * Absatzes umschlie�t oder null, falls keine Ziffer gefunden wurde. Bei der
   * Suche nach der Ziffer werden nur die ersten 6 Zeichen des Absatzes gepr�ft.
   * 
   * @param par
   *          die TextRange, die den Paragraphen umschlie�t, in dessen Anfang
   *          nach der r�mischen Ziffer gesucht werden soll.
   * @return die TextRange, die genau die r�mische Ziffer umschlie�t falls eine
   *         gefunden wurde oder null, falls keine Ziffer gefunden wurde.
   */
  private static XTextCursor getZifferOnly(XTextRange par)
  {
    XTextCursor cursor = par.getText().createTextCursorByRange(par.getStart());

    final String zifferPattern = "^([XIV]+|\\d+)\\.$";

    for (int i = 0; i < 6; i++)
    {
      cursor.goRight((short) 1, true);
      String text = cursor.getString();
      if (text.matches(zifferPattern)) return cursor;
    }

    return null;
  }

  /**
   * Liefert den ersten Paragraphen aus dem TextRahmen WollMuxVerfuegungspunkt1,
   * dessen Absatzformatname mit dem String "WollMuxVerfuegungspunkt" beginnt.
   * 
   * @param doc
   *          das Dokument, in dem sich der TextRahmen WollMuxVerfuegungspunkt1
   *          befindet.
   * @return Die TextRange, die den ganzen Absatz des Verfuegungspunktes
   *         umschlie�t oder null, falls kein entsprechender TextRahmen oder
   *         darin kein als Verfuegungspunkt markierter Absatz vorhanden ist.
   */
  private static XTextRange getVerfuegungspunkt1(XTextDocument doc)
  {
    XTextFrame frame = null;
    try
    {
      frame = UNO.XTextFrame(UNO.XTextFramesSupplier(doc).getTextFrames()
          .getByName(FRAME_NAME_VERFUEGUNGSPUNKT1));
    }
    catch (java.lang.Exception e)
    {
    }

    if (UNO.XEnumerationAccess(frame) != null)
    {
      XEnumeration xenum = UNO.XEnumerationAccess(frame).createEnumeration();
      while (xenum.hasMoreElements())
      {
        XTextRange paragraph = null;
        try
        {
          paragraph = UNO.XTextRange(xenum.nextElement());
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }

        if (paragraph != null && isVerfuegungspunkt(paragraph))
        {
          return paragraph;
        }
      }
    }

    return null;
  }
}
