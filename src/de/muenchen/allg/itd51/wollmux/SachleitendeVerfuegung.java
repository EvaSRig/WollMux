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
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.dialog.SachleitendeVerfuegungenDruckdialog;

public class SachleitendeVerfuegung
{
  private static final String ParaStyleNameVerfuegungspunkt = "WollMuxVerfuegungspunkt";

  private static final String ParaStyleNameVerfuegungspunkt1 = "WollMuxVerfuegungspunkt1";

  private static final String ParaStyleNameAbdruck = "WollMuxVerfuegungspunktAbdruck";

  private static final String ParaStyleNameZuleitungszeile = "WollMuxZuleitungszeile";

  private static final String ParaStyleNameDefault = "Flie�text";

  private static final String FrameNameVerfuegungspunkt1 = "WollMuxVerfuegungspunkt1";

  private static final String AbdruckDefaultStr = "Abdruck von <Vorg�nger>.";

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
   */
  public static void verfuegungspunktEinfuegen(XTextDocument doc,
      XTextRange range)
  {
    if (doc == null || range == null) return;

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));

    // Enth�lt der markierte Bereich bereits Verfuegungspunkte, so werden diese
    // gel�scht
    boolean deletedAtLeastOne = alleVerfuegungspunkteLoeschen(cursor);

    if (!deletedAtLeastOne)
    {
      // Wenn kein Verf�gungspunkt gel�scht wurde, sollen alle markierten
      // Paragraphen als Verfuegungspunkte markiert werden.
      UNO.setProperty(cursor, "ParaStyleName", ParaStyleNameVerfuegungspunkt);
    }

    // Ziffernanpassung durchf�hren:
    ziffernAnpassen(doc);
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
   */
  public static void abdruck(XTextDocument doc, XTextRange range)
  {
    if (doc == null || range == null) return;

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));

    // Enth�lt der markierte Bereich bereits Verfuegungspunkte, so werden diese
    // gel�scht
    boolean deletedAtLeastOne = alleVerfuegungspunkteLoeschen(cursor);

    if (!deletedAtLeastOne)
    {
      // Abdruck einf�gen, wenn kein Verf�gungspunkt gel�scht wurde.
      cursor.collapseToEnd();
      cursor.gotoEndOfParagraph(false);
      cursor.setString("\r" + AbdruckDefaultStr);
      cursor.gotoNextParagraph(false);
      UNO.setProperty(cursor, "ParaStyleName", ParaStyleNameAbdruck);
      cursor.gotoEndOfParagraph(false);
      cursor.setString("\r");
    }

    // Ziffern anpassen:
    ziffernAnpassen(doc);
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
   */
  public static void zuleitungszeile(XTextDocument doc, XTextRange range)
  {
    if (doc == null || range == null) return;

    XParagraphCursor cursor = UNO.XParagraphCursor(range.getText()
        .createTextCursorByRange(range));

    boolean deletedAtLeastOne = alleZuleitungszeilenLoeschen(cursor);

    if (!deletedAtLeastOne)
    {
      // Sind im markierten Bereich Verf�gungspunkte enthalten, so werden diese
      // zur�ckgesetzt und neu numeriert, damit die entsprechenden Abs�tze
      // anschlie�end zu Zuleitungszeilen gemacht werden k�nnen.
      if (alleVerfuegungspunkteLoeschen(cursor)) ziffernAnpassen(doc);

      // Absatzformat f�r Zuleitungszeilen setzen (auf den gesamten Bereich)
      UNO.setProperty(cursor, "ParaStyleName", ParaStyleNameZuleitungszeile);

      // Nach dem Setzen einer Zuleitungszeile, soll der Cursor auf dem Ende der
      // Markierung stehen, damit man direkt von dort weiter schreiben kann.
      try
      {
        UNO.XTextViewCursorSupplier(UNO.XModel(doc).getCurrentController())
            .getViewCursor().gotoRange(cursor.getEnd(), false);
      }
      catch (Exception e)
      {
      }
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
    String text = paragraph.getString();
    return text.matches(".*Abdruck von (I|<Vorg�nger>)\\..*");
  }

  /**
   * Diese Methode l�scht alle Verf�gungspunkte, die der bereich des Cursors
   * cursor ber�hrt, und liefert true zur�ck, wenn mindestens ein
   * Verf�gungspunkt gel�scht wurde oder false, wenn sich in dem Bereich des
   * Cursors kein Verf�gungspunkt befand.
   * 
   * @param doc
   *          Das Dokument, in dem der Verf�gungspunkt eingef�gt werden soll
   *          (wird f�r die Ziffernanpassung ben�tigt)
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Verf�gungspunkten gesucht wird.
   * 
   * @return true, wenn mindestens ein Verf�gungspunkt gel�scht wurde oder
   *         false, wenn kein der cursor keinen Verf�gungspunkt ber�hrt.
   */
  private static boolean alleVerfuegungspunkteLoeschen(XParagraphCursor cursor)
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

        if (par != null && isVerfuegungspunkt(par))
        {
          // Einen evtl. bestehenden Verfuegungspunkt zur�cksetzen
          verfuegungspunktLoeschen(par);
          deletedAtLeastOne = true;
        }
      }
    }
    return deletedAtLeastOne;
  }

  /**
   * Diese Methode l�scht alle Zuleitungszeilen, die der bereich des Cursors
   * cursor ber�hrt, und liefert true zur�ck, wenn mindestens eine
   * Zuleitungszeile gel�scht wurde oder false, wenn sich in dem Bereich des
   * Cursors keine Zuleitungszeile befand.
   * 
   * @param doc
   *          Das Dokument, in dem der Verf�gungspunkt eingef�gt werden soll
   *          (wird f�r die Ziffernanpassung ben�tigt)
   * @param cursor
   *          Der Cursor, in dessen Bereich nach Verf�gungspunkten gesucht wird.
   * 
   * @return true, wenn mindestens ein Verf�gungspunkt gel�scht wurde oder
   *         false, wenn kein der cursor keinen Verf�gungspunkt ber�hrt.
   */
  private static boolean alleZuleitungszeilenLoeschen(XParagraphCursor cursor)
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

        if (par != null && isZuleitungszeile(par))
        {
          // Zuleitungszeile zur�cksetzen
          UNO.setProperty(par, "ParaStyleName", ParaStyleNameDefault);

          deletedAtLeastOne = true;
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
  private static void verfuegungspunktLoeschen(XTextRange par)
  {
    UNO.setProperty(par, "ParaStyleName", ParaStyleNameDefault);

    // Pr�fe, ob der Absatz mit einer r�mischen Ziffer beginnt.
    XTextCursor zifferOnly = getZifferOnly(par);
    if (zifferOnly != null)
    {
      // r�mische Ziffer l�schen.
      zifferOnly.setString("");

      // wenn n�chstes Zeichen ein Whitespace-Zeichen ist, wird dieses gel�scht
      zifferOnly.goRight((short) 1, true);
      if (zifferOnly.getString().matches("[ \t]")) zifferOnly.setString("");
    }

    // wenn es sich bei dem Paragraphen um einen Abdruck handelt, wird dieser
    // vollst�ndig gel�scht.
    if (isAbdruck(par))
    {
      // l�scht den String "Abdruck von..."
      par.setString("");

      // l�scht das Returnzeichen ("\r") zum n�chsten Absatz
      XParagraphCursor parDeleter = UNO.XParagraphCursor(par.getText()
          .createTextCursorByRange(par.getEnd()));
      if (parDeleter.goRight((short) 1, false))
      {
        parDeleter.goLeft((short) 1, true);
        parDeleter.setString("");

        // wenn die auf die ehemalige Abdruckzeile folgende Zeile auch noch leer
        // ist, so wird diese Zeile auch noch gel�scht.
        if (parDeleter.isEndOfParagraph())
        {
          parDeleter.goLeft((short) 1, true);
          parDeleter.setString("");
        }
      }
    }
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
  private static void ziffernAnpassen(XTextDocument doc)
  {
    XTextRange punkt1 = getVerfuegungspunkt1(doc);

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
    XParagraphCursor cursor = UNO.XParagraphCursor(doc.getText()
        .createTextCursorByRange(doc.getText().getStart()));
    if (cursor != null)
      do
      {
        // ganzen Paragraphen markieren
        cursor.gotoEndOfParagraph(true);

        if (isVerfuegungspunkt(cursor))
        {
          count++;

          // String mit r�mischer Zahl erzeugen
          String number = romanNumber(count);

          // Enth�lt der Paragraph einen "Abdruck"-String, so wird dieser neu
          // gesetzt:
          if (isAbdruck(cursor))
          {
            cursor.setString(abdruckString(count));
          }

          XTextRange zifferOnly = getZifferOnly(cursor);
          if (zifferOnly != null)
          {
            // Nummer aktualisieren wenn sie nicht mehr stimmt.
            if (!zifferOnly.getString().equals(number))
              zifferOnly.setString(number);
          }
          else
          {
            // zuerst den Tab einf�gen, damit dieses in der Standardformatierung
            // des Absatzes erhalten bleibt.
            cursor.getStart().setString("\t"); // Rechtsverschiebung des
            // Cursors
            cursor.gotoStartOfParagraph(true); // Korrektur der Verschiebung

            // neue Nummer erzeugen mit Formatierung "fett". Die Formatierung
            // darf sich nur auf die Nummer auswirken und nicht auch noch auf
            // das darauffolgende "\t"-Zeichen
            zifferOnly = cursor.getText().createTextCursorByRange(
                cursor.getStart());
            UNO.setProperty(
                zifferOnly,
                "CharWeight",
                new Float(FontWeight.BOLD));
            zifferOnly.setString(number);
          }
        }
      } while (cursor.gotoNextParagraph(false));

    // Verfuegungspunt1 setzen
    if (punkt1 != null)
    {
      XTextRange zifferOnly = getZifferOnly(punkt1);
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
    if (effectiveCount == 0)
      WollMuxEventHandler.handleSetPrintFunction(doc, "");
    else
      WollMuxEventHandler.handleSetPrintFunction(doc, "SachleitendeVerfuegung");
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
      UNO.setProperty(cursor, "ParaStyleName", ParaStyleNameVerfuegungspunkt1);
      return cursor;
    }
    else
      return null;
  }

  /**
   * Erzeugt einen String in der Form "Abdruck von I.[, II., ...][ und <i-1>]",
   * der passend zu einem Abdruck mit der Verf�gungsnummer i angezeigt werden
   * soll.
   * 
   * @param i
   *          Die Nummer des Verf�gungspunktes des Abdrucks
   * @return String in der Form "Abdruck von I.[, II., ...][ und <i-1>]" oder
   *         AbdruckDefaultStr, wenn der Verf�gungspunkt bei i==0 und i==1
   *         keinen Vorg�nger besitzen kann.
   */
  private static String abdruckString(int i)
  {
    if (i < 2) return AbdruckDefaultStr;

    String str = "Abdruck von " + romanNumber(1);
    for (int j = 2; j < (i - 1); ++j)
      str += ", " + romanNumber(j);
    if (i >= 3) str += " und " + romanNumber(i - 1);
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

  private static Vector scanVerfuegungspunkte(XTextDocument doc)
  {
    Vector verfuegungspunkte = new Vector();

    // Verf�gungspunkt1 hinzuf�gen wenn verf�gbar.
    XTextRange punkt1 = getVerfuegungspunkt1(doc);
    if (punkt1 != null)
      verfuegungspunkte.add(new VerfuegungspunktOriginal(punkt1));

    Verfuegungspunkt currentVerfpunkt = null;

    // Paragraphen des Texts enumerieren und Verf�gungspunkte erstellen. Die
    // Enumeration erfolgt �ber einen ParagraphCursor, da sich
    // dieser stabiler verh�lt als das Durchgehen der XEnumerationAccess, bei
    // der es zu OOo-Abst�rzen kam. Leider konnte ich das Problem nicht exakt
    // genug isolieren um ein entsprechende Ticket bei OOo dazu aufmachen zu
    // k�nnen, da der Absturz nur sporadisch auftrat.
    XParagraphCursor cursor = UNO.XParagraphCursor(doc.getText()
        .createTextCursorByRange(doc.getText().getStart()));

    if (cursor != null) do
    {
      // ganzen Paragraphen markieren
      cursor.gotoEndOfParagraph(true);

      if (isVerfuegungspunkt(cursor))
      {
        currentVerfpunkt = new Verfuegungspunkt(cursor);
        verfuegungspunkte.add(currentVerfpunkt);
      }
      else if (currentVerfpunkt != null)
      {
        currentVerfpunkt.addParagraph(cursor);
      }

    } while (cursor.gotoNextParagraph(false));

    return verfuegungspunkte;
  }

  /**
   * TODO: Verfuegungspunkt refaktorisieren, so dass keine TextRanges mehr
   * erforderlich sind - Verfuegungspunkt wird jetzt lediglich als Datenspeicher
   * f�r den Druckdialog ben�tigt, der keine Zugriff auf die TextRanges
   * ben�tigt.
   * 
   * Repr�sentiert einen vollst�ndigen Verf�gungspunkt, der aus �berschrift
   * (r�mische Ziffer + �berschrift) und Inhalt besteht. Die Klasse bietet
   * Methden an, �ber die auf alle f�r den Druck wichtigen Eigenschaften des
   * Verf�gungspunktes zugegriffen werden kann (z.B. �berschrift, Anzahl
   * Zuleitungszeilen, ...)
   * 
   * @author christoph.lutz
   * 
   */
  public static class Verfuegungspunkt
  {
    /**
     * Vector mit den XTextRanges aller Paragraphen des Verf�gungspunktes.
     */
    protected Vector /* of XTextRange */paragraphs;

    /**
     * Vector of String, der alle Zuleitungszeilen enth�lt, die mit addParagraph
     * hinzugef�gt wurden.
     */
    protected Vector zuleitungszeilen;

    /**
     * Erzeugt einen neuen Verf�gungspunkt, wobei firstPar der Absatz ist, der
     * die erste Zeile mit der r�mischen Ziffer und der �berschrift enth�lt.
     * 
     * @param firstPar
     *          Die erste Zeile des Verf�gungspunktes mit der r�mischen Ziffer
     *          und der �berschrift.
     * @throws java.lang.NullPointerException
     *           wenn paragraph null ist
     */
    public Verfuegungspunkt(XTextRange firstPar)
    {
      if (firstPar == null)
        throw new NullPointerException("XTextRange firstPar ist null");

      this.paragraphs = new Vector();
      this.zuleitungszeilen = new Vector();

      paragraphs.add(firstPar.getText().createTextCursorByRange(firstPar));
    }

    /**
     * F�gt einen weiteren Paragraphen des Verf�gungspunktes hinzu (wenn
     * paragraph nicht null ist).
     * 
     * @param paragraph
     *          XTextRange, das den gesamten Paragraphen enth�lt.
     */
    public void addParagraph(XTextRange paragraph)
    {
      if (paragraph == null) return;
      XTextCursor par = paragraph.getText().createTextCursorByRange(paragraph);

      paragraphs.add(par);

      if (isZuleitungszeile(par)) zuleitungszeilen.add(par.getString());
    }

    /**
     * Liefert true, wenn es sich bei dem �bergebenen Absatz paragraph um einen
     * als Zuleitungszeile markierten Absatz handelt.
     * 
     * @param paragraph
     *          Das Objekt mit der Property ParaStyleName, die f�r den Vergleich
     *          herangezogen wird.
     * @return true, wenn der Name des Absatzformates mit
     *         "WollMuxZuleitungszeile" beginnt.
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
     * Liefert den gesamten Bereich �ber den sich der Verf�gungspunkt erstreckt,
     * angefangen vom Startpunkt der ersten Zeile des Verf�gungspunktes bis hin
     * zum Ende des letzten enthaltenen Paragraphen.
     * 
     * @return der gesamte Bereich �ber den sich der Verf�gungspunkt erstreckt.
     */
    public XTextRange getCompleteRange()
    {
      XTextRange firstPar = UNO.XTextRange(paragraphs.get(0));
      XTextRange lastPar = UNO
          .XTextRange(paragraphs.get(paragraphs.size() - 1));

      XTextCursor cursor = firstPar.getText().createTextCursor();
      cursor.gotoRange(firstPar.getStart(), false);
      cursor.gotoRange(lastPar.getEnd(), true);
      return cursor;
    }

    /**
     * Liefert die Anzahl der Zuleitungszeilen zur�ck, die dem Verf�gungspunkt
     * mit addParagraph hinzugef�gt wurden.
     * 
     * @return Anzahl der Zuleitungszeilen dieses Verf�gungspunktes.
     */
    public int getZuleitungszeilenCount()
    {
      return zuleitungszeilen.size();
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
     * r�mischen Ziffer als String zur�ck.
     * 
     * @return r�mischer Ziffer + �berschrift
     */
    public String getHeading()
    {
      String text = "";
      XTextRange firstPar = UNO.XTextRange(paragraphs.get(0));
      if (firstPar != null) text = firstPar.getString();

      // Tabs und Spaces durch single spaces ersetzen
      text = text.replaceAll("\\s+", " ");

      return text;
    }
  }

  /**
   * Enth�lt die Besonderheiten des ersten Verf�gungspunktes eines externen
   * Briefkopfes wie z.B. die Darstellung der �berschrift als "I. Original".
   * 
   * @author christoph.lutz
   * 
   */
  public static class VerfuegungspunktOriginal extends Verfuegungspunkt
  {
    public VerfuegungspunktOriginal(XTextRange punkt1)
    {
      super(punkt1);

      if (punkt1 == null)
        throw new NullPointerException("XTextRange punkt1 ist null");

      zuleitungszeilen.add("Empf�nger siehe Empf�ngerfeld");
    }

    public String getHeading()
    {
      return "I. Original";
    }

    public void addParagraph(XTextRange paragraph)
    {
      // addParagraph ergibt bei Verfuegungspunkt1 keinen Sinn und wird daher
      // disabled.
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
      new SachleitendeVerfuegungenDruckdialog(printDialogConf, vps, null);
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
   * Druckt den Verf�gungpunkt verfPunkt aus dem Dokument doc in der gew�nschten
   * Anzahl numberOfCopies aus. ACHTUNG: Diese Methode darf nur aus dem
   * WollMuxEventHandler-Thread gestartet werden, da sie auf Datenstrukturen des
   * WollMux zugreift.
   * 
   * @param doc
   * @param verfPunkt
   * @param numberOfCopies
   */
  public static void printVerfuegungspunkt(TextDocumentModel model,
      short verfPunkt, short numberOfCopies, boolean isDraft)
  {
    boolean isOriginal = (verfPunkt == 1);

    // Z�hler f�r Verfuegungspunktnummer auf 1 initialisieren, wenn ein
    // Verfuegungspunkt1 vorhanden ist.
    XTextRange punkt1 = getVerfuegungspunkt1(model.doc);
    int count = 0;
    if (punkt1 != null) count++;

    // Auszublendenden Bereich festlegen:
    XTextRange setInvisibleRange = null;
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
      UNO.setProperty(setInvisibleRange, "CharHidden", Boolean.TRUE);

    // Sichtbarkeitsstand der draftOnly bzw. notInOriginal-Bl�cke merken.
    HashMap /* of DocumentCommand */oldVisibilityStates = new HashMap();

    // Ein/Ausblenden der draftOnly bzw. notInOriginal-Bl�cke:
    Iterator iter = model.getDraftOnlyBlocksIterator();
    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      oldVisibilityStates.put(cmd, new Boolean(cmd.isVisible()));
      cmd.setVisible(isDraft);
    }

    iter = model.getNotInOrininalBlocksIterator();
    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      oldVisibilityStates.put(cmd, new Boolean(cmd.isVisible()));
      cmd.setVisible(!isOriginal);
    }

    // Ziffer von Punkt 1 ausblenden falls isOriginal
    XTextRange punkt1ZifferOnly = null;
    if (isOriginal && punkt1 != null)
    {
      punkt1ZifferOnly = getZifferOnly(punkt1);
      UNO.setProperty(punkt1ZifferOnly, "CharHidden", Boolean.TRUE);
    }

    // Druck des Dokuments mit den entsprechenden Ein/Ausbledungen
    model.print(numberOfCopies);

    // Ausblendung von Ziffer von Punkt 1 wieder aufheben
    if (punkt1ZifferOnly != null)
      UNO.setProperty(punkt1ZifferOnly, "CharHidden", Boolean.FALSE);

    // Alte Sichtbarkeitszust�nde der draftOnly bzw. notInOriginal-Bl�cke
    // zur�cksetzten.
    iter = oldVisibilityStates.keySet().iterator();
    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      cmd.setVisible(((Boolean) oldVisibilityStates.get(cmd)).booleanValue());
    }

    // Verf�gungspunkte wieder einblenden:
    if (setInvisibleRange != null)
      UNO.setProperty(setInvisibleRange, "CharHidden", Boolean.FALSE);
  }

  public static void main(String[] args) throws Exception
  {
    UNO.init();

    XTextDocument doc = UNO.XTextDocument(UNO.desktop.getCurrentComponent());

    if (doc == null)
    {
      System.err.println("Keine Textdokument");
      return;
    }

  }
}
