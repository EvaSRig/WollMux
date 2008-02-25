/*
 * Dateiname: Bookmark.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse repr�sentiert ein Bookmark in OOo und bietet Methoden
 *            f�r den vereinfachten Zugriff und die Manipulation von Bookmarks an.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 17.05.2006 | LUT | Dokumentation erg�nzt
 * 07.08.2006 | BNK | +Bookmark(XNamed bookmark, XTextDocument doc)
 * 29.09.2006 | BNK | rename() gibt nun im Fehlerfall das BROKEN-String-Objekt zur�ck
 * 29.09.2006 | BNK | Unn�tige renames vermeiden, um OOo nicht zu stressen
 * 29.09.2006 | BNK | Auch im optimierten Fall wo kein rename stattfindet auf BROKEN testen
 * 20.10.2006 | BNK | rename() Debug-Meldung nicht mehr ausgeben, wenn No Op Optimierung triggert.
 * 31.10.2006 | BNK | +select() zum Setzen des ViewCursors
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.util.Iterator;
import java.util.Vector;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;

/**
 * Diese Klasse repr�sentiert ein Bookmark in OOo und bietet Methoden f�r den
 * vereinfachten Zugriff und die Manipulation von Bookmarks an.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class Bookmark
{
  /**
   * Wird festgestellt, dass das Bookmark aus dem Dokument gel�scht wurde, so
   * wird der Name auf diesen String gesetzt (== vergleichbar).
   */
  public static final String BROKEN = "WM(CMD'bookmarkBroken')";

  /**
   * Enth�lt den Namen des Bookmarks
   */
  private String name;

  /**
   * Enth�lt den UnoService des Dokuments dem das Bookmark zugeordnet ist.
   */
  private UnoService document;

  /**
   * Der Konstruktor liefert eine Instanz eines bereits im Dokument doc
   * bestehenden Bookmarks mit dem Namen name zur�ck; ist das Bookmark im
   * angebegenen Dokument nicht enthalten, so wird eine NoSuchElementException
   * zur�ckgegeben.
   * 
   * @param name
   *          Der Name des bereits im Dokument vorhandenen Bookmarks.
   * @param doc
   *          Das Dokument, welches Das Bookmark name enth�lt.
   * @throws NoSuchElementException
   *           Das Bookmark name ist im angegebenen Dokument nicht enthalten.
   */
  public Bookmark(String name, XBookmarksSupplier doc) throws NoSuchElementException
  {
    this.document = new UnoService(doc);
    this.name = name;
    UnoService bookmark = getBookmarkService(name, document);
    if (bookmark.xTextContent() == null)
      throw new NoSuchElementException(L.m("Bookmark '%1' existiert nicht.", name));
  }

  /**
   * Der Konstruktor liefert eine Instanz eines bereits im Dokument doc
   * bestehenden Bookmarks bookmark zur�ck.
   */
  public Bookmark(XNamed bookmark, XTextDocument doc)
  {
    this.document = new UnoService(doc);
    this.name = bookmark.getName();
  }

  /**
   * Der Konstruktor erzeugt ein neues Bookmark name im Dokument doc an der
   * Position, die durch range beschrieben ist.
   * 
   * @param name
   *          Der Name des neu zu erstellenden Bookmarks.
   * @param doc
   *          Das Dokument, welches das Bookmark name enthalten soll.
   * @param range
   *          Die Position, an der das Dokument liegen soll.
   */
  public Bookmark(String name, XTextDocument doc, XTextRange range)
  {
    this.document = new UnoService(doc);
    this.name = name;

    // Bookmark-Service erzeugen
    UnoService bookmark = new UnoService(null);
    try
    {
      bookmark = document.create("com.sun.star.text.Bookmark");
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    // Namen setzen
    if (bookmark.xNamed() != null)
    {
      bookmark.xNamed().setName(name);
    }

    // Bookmark ins Dokument einf�gen
    if (document.xTextDocument() != null && bookmark.xTextContent() != null
        && range != null)
    {
      try
      {
        // der TextCursor ist erforderlich, damit auch Bookmarks mit Ausdehnung
        // erfolgreich gesetzt werden k�nnen. Das geht mit normalen TextRanges
        // nicht.
        XTextCursor cursor = range.getText().createTextCursorByRange(range);
        range.getText().insertTextContent(cursor, bookmark.xTextContent(), true);
        this.name = bookmark.xNamed().getName();
      }
      catch (IllegalArgumentException e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Vor jedem Zugriff auf den BookmarkService bookmark sollte der Service neu
   * geholt werden, damit auch der Fall behandelt wird, dass das Bookmark
   * inzwischen vom Anwender gel�scht wurde. Ist das Bookmark nicht (mehr) im
   * Dokument vorhanden, so wird ein new UnoService(null) zur�ckgeliefert,
   * welches leichter verarbeitet werden kann.
   * 
   * @param name
   *          Der Name des bereits im Dokument vorhandenen Bookmarks.
   * @param document
   *          Das Dokument, welches Das Bookmark name enth�lt.
   * @return Den UnoService des Bookmarks name im Dokument document.
   */
  private static UnoService getBookmarkService(String name, UnoService document)
  {
    if (document.xBookmarksSupplier() != null)
    {
      try
      {
        return new UnoService(
          document.xBookmarksSupplier().getBookmarks().getByName(name));
      }
      catch (WrappedTargetException e)
      {
        Logger.error(e);
      }
      catch (NoSuchElementException e)
      {
      }
    }
    return new UnoService(null);
  }

  /**
   * Diese Methode liefert den (aktuellen) Namen des Bookmarks als String
   * zur�ck.
   * 
   * @return liefert den (aktuellen) Namen des Bookmarks als String zur�ck.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Setzt den ViewCursor auf dieses Bookmark.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void select()
  {
    UnoService bm = getBookmarkService(getName(), document);
    if (bm.getObject() != null)
    {
      try
      {
        XTextRange anchor = bm.xTextContent().getAnchor();
        XTextRange cursor = anchor.getText().createTextCursorByRange(anchor);
        UNO.XTextViewCursorSupplier(document.xModel().getCurrentController()).getViewCursor().gotoRange(
          cursor, false);
      }
      catch (java.lang.Exception x)
      {
      }
    }
  }

  /**
   * Diese Methode liefert eine String-Repr�sentation mit dem Aufbau "Bookmark[<name>]"
   * zur�ck.
   */
  public String toString()
  {
    return "Bookmark[" + getName() + "]";
  }

  /**
   * Diese Methode liefert das Dokument zu dem das Bookmark geh�rt.
   */
  public XTextDocument getDocument()
  {
    return document.xTextDocument();
  }

  /**
   * Diese Methode benennt dieses Bookmark in newName um. Ist der Name bereits
   * definiert, so wird automatisch eine Nummer an den Namen angeh�ngt. Die
   * Methode gibt den tats�chlich erzeugten Bookmarknamen zur�ck.
   * 
   * @return den tats�chlich erzeugten Namen des Bookmarks. Falls das Bookmark
   *         verschwunden ist, so wird das Objekt {@link #BROKEN}
   *         zur�ckgeliefert (== vergleichbar).
   * @throws Exception
   */
  public String rename(String newName)
  {
    XNameAccess bookmarks = UNO.XBookmarksSupplier(document.getObject()).getBookmarks();

    // Um OOo nicht zu stressen vermeiden wir unn�tige Renames
    // Wir testen aber trotzdem ob das Bookmark BROKEN ist
    if (name.equals(newName))
    {
      if (!bookmarks.hasByName(name)) name = BROKEN;
      return name;
    }

    Logger.debug("Rename \"" + name + "\" --> \"" + newName + "\"");

    // Falls bookmark <newName> bereits existiert, <newName>N verwenden (N ist
    // eine nat�rliche Zahl)
    if (bookmarks.hasByName(newName))
    {
      int count = 1;
      while (bookmarks.hasByName(newName + count))
        ++count;
      newName = newName + count;
    }

    XNamed bm = null;
    try
    {
      bm = UNO.XNamed(bookmarks.getByName(name));
    }
    catch (NoSuchElementException x)
    {
      Logger.debug(L.m("Umbenennung kann nicht durchgef�hrt werden, da die Textmarke verschwunden ist :~-("));
    }
    catch (java.lang.Exception x)
    {
      Logger.error(x);
    }

    if (bm != null)
    {
      bm.setName(newName);
      name = bm.getName();
    }
    else
      name = BROKEN;

    return name;
  }

  /**
   * Diese Methode weist dem Bookmark einen neuen TextRange (als Anchor) zu.
   * 
   * @param xTextRange
   *          Der neue TextRange des Bookmarks.
   */
  public void rerangeBookmark(XTextRange xTextRange)
  {
    // altes Bookmark l�schen.
    remove();

    // neues Bookmark unter dem alten Namen mit neuer Ausdehnung hinzuf�gen.
    try
    {
      UnoService bookmark = document.create("com.sun.star.text.Bookmark");
      bookmark.xNamed().setName(name);
      xTextRange.getText().insertTextContent(xTextRange, bookmark.xTextContent(),
        true);
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Die Methode gibt die XTextRange des Bookmarks zur�ck, oder null, falls das
   * Bookmark nicht vorhanden ist (z,B, weil es inzwischen gel�scht wurde). Als
   * Workaround f�r Bug #67869 erzeugt diese Methode jedoch noch einen
   * TextCursor, mit dessen Hilfe sich der Inhalt des Bookmarks sicherer
   * enumerieren lassen kann.
   * 
   * @return
   */
  public XTextRange getTextRange()
  {
    // Workaround f�r OOo-Bug: fehlerhafter Anchor bei Bookmarks in Tabellen.
    // http://www.openoffice.org/issues/show_bug.cgi?id=67869 . Ein
    // TextCursor-Objekt verh�lt sich dahingehend robuster.
    XTextRange range = getAnchor();
    if (range != null) return range.getText().createTextCursorByRange(range);
    return null;
  }

  /**
   * Liefert die TextRange an der dieses Bookmark verankert ist oder null falls
   * das Bookmark nicht mehr existiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public XTextRange getAnchor()
  {
    XBookmarksSupplier supp = UNO.XBookmarksSupplier(document.getObject());
    try
    {
      return UNO.XTextContent(supp.getBookmarks().getByName(name)).getAnchor();
    }
    catch (Exception x)
    {
      return null;
    }
  }

  /**
   * Diese Methode l�scht das Bookmark aus dem Dokument.
   */
  public void remove()
  {
    UnoService bookmark = getBookmarkService(name, document);
    if (bookmark.xTextContent() != null)
    {
      try
      {
        XTextRange range = bookmark.xTextContent().getAnchor();
        range.getText().removeTextContent(bookmark.xTextContent());
        bookmark = new UnoService(null);
      }
      catch (NoSuchElementException e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Entfernt allen Text (aber keine Bookmarks) aus range.
   * 
   * @param doc
   *          das Dokument, das range enth�lt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void removeTextFromInside(XTextDocument doc, XTextRange range)
  {
    try
    {
      // ein Bookmark erzeugen, was genau die Range, die wir l�schen wollen vom
      // Rest des Textes abtrennt, d.h. welches daf�r sorgt, dass unser Text
      // eine
      // eigene Textportion ist.
      Object bookmark = UNO.XMultiServiceFactory(doc).createInstance(
        "com.sun.star.text.Bookmark");
      UNO.XNamed(bookmark).setName("killer");
      range.getText().insertTextContent(range, UNO.XTextContent(bookmark), true);
      String name = UNO.XNamed(bookmark).getName();

      // Aufsammeln der zu entfernenden TextPortions (sollte genau eine sein)
      // und
      // der Bookmarks, die evtl. als Kollateralschaden entfernt werden.
      Vector<String> collateral = new Vector<String>();
      Vector<Object> victims = new Vector<Object>();
      XEnumeration xEnum = UNO.XEnumerationAccess(range).createEnumeration();
      while (xEnum.hasMoreElements())
      {
        boolean kill = false;
        XEnumerationAccess access = UNO.XEnumerationAccess(xEnum.nextElement());
        if (access != null)
        {
          XEnumeration xEnum2 = access.createEnumeration();
          while (xEnum2.hasMoreElements())
          {
            Object textPortion = xEnum2.nextElement();
            if ("Bookmark".equals(UNO.getProperty(textPortion, "TextPortionType")))
            {
              String portionName = UNO.XNamed(
                UNO.getProperty(textPortion, "Bookmark")).getName();
              if (name.equals(portionName))
              {
                kill = ((Boolean) UNO.getProperty(textPortion, "IsStart")).booleanValue();
              }
              else
                collateral.add(portionName);
            }

            if (kill
                && "Text".equals(UNO.getProperty(textPortion, "TextPortionType")))
            {
              victims.add(textPortion);
            }
          }
        }
      }

      /*
       * Zu entfernenden Content l�schen.
       */
      /*
       * Iterator iter = victims.iterator(); XText text = range.getText(); while
       * (iter.hasNext()) {
       * text.removeTextContent(UNO.XTextContent(iter.next())); }
       */
      range.setString("");

      UNO.XTextContent(bookmark).getAnchor().getText().removeTextContent(
        UNO.XTextContent(bookmark));

      /*
       * Verlorene Bookmarks regenerieren.
       */
      XNameAccess bookmarks = UNO.XBookmarksSupplier(doc).getBookmarks();
      Iterator<String> iter = collateral.iterator();
      while (iter.hasNext())
      {
        String portionName = iter.next();
        if (!bookmarks.hasByName(portionName))
        {
          Logger.debug(L.m("Regeneriere Bookmark '%1'", portionName));
          bookmark = UNO.XMultiServiceFactory(doc).createInstance(
            "com.sun.star.text.Bookmark");
          UNO.XNamed(bookmark).setName(portionName);
          range.getText().insertTextContent(range, UNO.XTextContent(bookmark), true);
        }

      }
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Diese Methode liefert den Wert der Property IsCollapsed zur�ck, wenn das
   * Ankerobjekt des Bookmarks diese Property besitzt, ansonsten wird false
   * geliefert. Das entsprechende Ankerobjekt wird durch entsprechende
   * Enumerationen �ber das Bookmarkobject gewonnen.
   * 
   * @return true, wenn die Property IsCollapsed existiert und true ist.
   *         Ansonsten wird false geliefert.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public boolean isCollapsed()
  {
    XTextRange anchor = getTextRange();
    if (anchor == null) return false;
    try
    {
      Object par = UNO.XEnumerationAccess(anchor).createEnumeration().nextElement();
      XEnumeration xenum = UNO.XEnumerationAccess(par).createEnumeration();
      while (xenum.hasMoreElements())
      {
        try
        {
          Object element = xenum.nextElement();
          String tpt = "" + UNO.getProperty(element, "TextPortionType");
          if (!tpt.equals("Bookmark")) continue;
          XNamed bm = UNO.XNamed(UNO.getProperty(element, "Bookmark"));
          if (bm == null || !name.equals(bm.getName())) continue;
          return AnyConverter.toBoolean(UNO.getProperty(element, "IsCollapsed"));
        }
        catch (java.lang.Exception e2)
        {
        }
      }
    }
    catch (java.lang.Exception e)
    {
    }
    return false;
  }

  /**
   * Diese Methode wandelt ein kollabiertes Bookmark (IsCollapsed()==true) in
   * ein nicht-kollabiertes Bookmark (IsCollapsed()==false) ohne Ausdehnung um.
   * Auf diese Weise wird OOo-Issue #73568 umgangen, gem�� dem kein Inhalt in
   * das Bookmark eingef�gt werden kann, wenn IsCollapsed==true ist. Ist das
   * Bookmark bereits nicht-kollabiert, so wird nichts unternommen.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void decollapseBookmark()
  {
    XTextRange range = getAnchor();
    if (range == null) return;

    // alte Range sichern und beenden, wenn nicht-kolabiert.
    XTextCursor cursor = range.getText().createTextCursorByRange(range);
    if (!cursor.isCollapsed()) return;

    Logger.debug(L.m("Dekollabiere Bookmark '%1'", name));

    // altes Bookmark l�schen.
    remove();

    // neues Bookmark unter dem alten Namen mit neuer Ausdehnung hinzuf�gen.
    try
    {
      UnoService bookmark = document.create("com.sun.star.text.Bookmark");
      bookmark.xNamed().setName(name);
      cursor.setString("x");
      cursor.getText().insertTextContent(cursor, bookmark.xTextContent(), true);
      bookmark.xTextContent().getAnchor().setString("");
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Definition von equals, damit Bookmarks �ber HashMaps/HashSets verwaltet
   * werden k�nnen.
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object b)
  {
    try
    {
      return name.equals(((Bookmark) b).name);
    }
    catch (java.lang.Exception e)
    {
      return false;
    }
  }

  /**
   * Definition von hashCode, damit Bookmarks �ber HashMaps/HashSets verwaltet
   * werden k�nnen.
   * 
   * @see java.lang.Object#hashCode()
   */
  public int hashCode()
  {
    return name.hashCode();
  }

}
