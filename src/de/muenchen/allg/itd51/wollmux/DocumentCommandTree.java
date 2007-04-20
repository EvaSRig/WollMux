/*
 * Dateiname: DocumentCommandTree.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse repr�sentiert einen Baum von Dokumentkommandos
 *            eines Dokuments.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 17.05.2006 | LUT | Dokumentation erg�nzt
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.XNameAccess;
import com.sun.star.frame.XDesktop;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.AllVersions;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.DraftOnly;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.Form;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertContent;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFrag;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFunctionValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InvalidCommand;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InvalidCommandException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.NotInOriginal;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.RootElement;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetGroups;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetJumpMark;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetPrintFunction;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetType;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.UpdateFields;

/**
 * Diese Klasse repr�sentiert einen Baum von Dokumentkommandos eines Dokuments.
 * Der Baum kann sich selbst updaten, d.h. er scannt das Dokument nach neuen
 * Bookmarks und erzeugt ggf. neue Dokumentkommandos, die er zus�tzlich in den
 * Baum einh�ngen kann.
 */
public class DocumentCommandTree
{
  /**
   * Folgendes Pattern pr�ft ob es sich bei einem Bookmark um ein g�ltiges
   * "WM"-Kommando handelt und entfernt evtl. vorhandene Zahlen-Suffixe.
   */
  public static final Pattern wmCmdPattern = Pattern
      .compile("\\A\\p{Space}*(WM\\p{Space}*\\(.*\\))\\p{Space}*(\\d*)\\z");

  /**
   * Das Dokument, welches die DokumentCommands enth�lt.
   */
  private XBookmarksSupplier xBookmarksSupplier;

  /**
   * Ein HashSet aller bereits bekannten Bookmarks, die beim update nicht mehr
   * erneut eingelesen werden m�ssen.
   */
  private HashSet scannedBookmarks;

  /**
   * Das Wurzelelement des Kommando-Baumes.
   */
  private RootElement root;

  /**
   * Erzeugt einen neuen (vorerst leeren) DocumentCommandTree. Der Baum kann
   * anschlie�end durch die Methode update() gef�llt werden.
   * 
   * @param xBookmarkSupplier
   *          Das Dokument, welches die Dokumentkommandos enth�lt.
   */
  public DocumentCommandTree(XBookmarksSupplier xBookmarksSupplier)
  {
    this.xBookmarksSupplier = xBookmarksSupplier;
    this.scannedBookmarks = new HashSet();
    this.root = new DocumentCommand.RootElement();
  }

  /**
   * Diese Methode durchsucht das Dokument nach neuen, bisher nicht gescannten
   * Dokumentkommandos und f�gt diese dem Baum hinzu oder entfernt
   * Dokumentkommandos, die nicht mehr g�ltig sind, weil ihr zugeh�riges
   * Bookmark nicht merh existiert. Das Kommando true zur�ck, wenn sich der
   * Kommandobaum durch das update() ver�ndert hat, ansonsten false.
   * 
   * @return true, wenn sich der Kommandobaum durch das update ver�ndert hat und
   *         false, wenn der Baum unver�ndert ist.
   */
  public boolean update()
  {
    boolean changed = false;
    int compareCount = 0;

    // Kommandos, deren Bookmark inzwischen gel�scht wurde sind ung�ltig und
    // m�ssen aus dem Baum entfernt werden:
    if (root.removeRetieredChilds() == true) changed = true;

    Iterator iter = getSetOfNewBookmarks().iterator();
    while (iter.hasNext())
    {
      Bookmark bookmark = (Bookmark) iter.next();

      // Bookmark evaluieren. Ist Bookmark ein "WM"-Kommand?
      Matcher m = wmCmdPattern.matcher(bookmark.getName());
      DocumentCommand command = null;
      if (m.find())
      {
        String cmdString = m.group(1);
        try
        {
          ConfigThingy wmCmd = new ConfigThingy("", null, new StringReader(
              cmdString));
          command = createCommand(wmCmd, bookmark);
        }
        catch (IOException e)
        {
          Logger.error(e);
        }
        catch (SyntaxErrorException e)
        {
          command = new InvalidCommand(bookmark, e);
        }
      }

      if (command != null)
      {
        Logger.debug2("Adding: " + command);
        compareCount += root.add(command);
        changed = true;
      }
    }
    Logger.debug2("Update fertig - compareCount=" + compareCount);

    return changed;
  }

  /**
   * Liefert ein Set das nur die Bookmarks enth�lt, die seit dem letzten Aufruf
   * dieser Methode NEU hinzugekommen sind. Die bisher bekannten Bookmarks
   * werden dabei in einem Set abgelegt, das stets die �ltesten Instanzen der
   * g�ltigen Bookmarks enth�lt. Das ist notwendig, damit Bookmarks nicht als
   * "neu" erkannt werden, wenn sie der WollMux selbst umbenannt hat.
   * 
   * @return
   */
  private HashSet getSetOfNewBookmarks()
  {
    HashSet newBookmarks = new HashSet();
    if (xBookmarksSupplier == null) return newBookmarks;

    // Seit dem letzten scan k�nnen sich Bookmarknamen ver�ndert haben. Die
    // folgenden drei Zeilen �bertragen die bisher bekannten Elemente in eine
    // tempor�re HashMap, womit auch die HashCodes der bekannten Elemente neu
    // bestimmt werden. Die HashMap ist ausserdem notwendig, um �ber get auf die
    // urspr�nglichen Bookmark Instanzen zugreifen zu k�nnen.
    HashMap oldBookmarks = new HashMap();
    Iterator iter = scannedBookmarks.iterator();
    while (iter.hasNext())
    {
      Object key = iter.next();
      oldBookmarks.put(key, key);
    }

    // Aus oldBookmarks alle Bookmarks entfernen, die nicht mehr existieren
    // und alle neuen Bookmarks hinzuf�gen. Die neuen Bookmarks zus�tzlich in
    // newBookmarks aufnehmen:
    HashSet actualBookmarks = new HashSet();
    XNameAccess bma = xBookmarksSupplier.getBookmarks();
    String[] elements = bma.getElementNames();
    for (int i = 0; i < elements.length; i++)
      try
      {
        Bookmark bookmark = new Bookmark(elements[i], xBookmarksSupplier);
        if (oldBookmarks.containsKey(bookmark))
        {
          // hier die urspr�ngliche Instanz verwenden anstatt bookmark
          actualBookmarks.add(oldBookmarks.get(bookmark));
        }
        else
        {
          actualBookmarks.add(bookmark);
          newBookmarks.add(bookmark);
        }
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }

    // was jetzt aktuell ist wird sp�ter alt
    scannedBookmarks = actualBookmarks;

    return newBookmarks;
  }

  /**
   * Fabrikmethode f�r die Erzeugung neuer Dokumentkommandos an dem Bookmark
   * bookmark mit dem als ConfigThingy vorgeparsten Dokumentkommando wmCmd.
   * 
   * @param wmCmd
   *          Das vorgeparste Dokumentkommando
   * @param bookmark
   *          Das Bookmark zu dem Dokumentkommando
   * @return Ein passende konkretes DocumentCommand-Instanz.
   */
  private DocumentCommand createCommand(ConfigThingy wmCmd, Bookmark bookmark)
  {
    String cmd = "";
    try
    {
      // CMD-Attribut auswerten
      try
      {
        cmd = wmCmd.get("WM").get("CMD").toString();
      }
      catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException("Fehlendes CMD-Attribut");
      }

      // spezielle Kommando-Instanzen erzeugen
      if (cmd.compareToIgnoreCase("insertFrag") == 0)
      {
        return new DocumentCommand.InsertFrag(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("insertValue") == 0)
      {
        return new DocumentCommand.InsertValue(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("insertContent") == 0)
      {
        return new DocumentCommand.InsertContent(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("form") == 0)
      {
        return new DocumentCommand.Form(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("updateFields") == 0)
      {
        return new DocumentCommand.UpdateFields(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("setType") == 0)
      {
        return new DocumentCommand.SetType(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("insertFormValue") == 0)
      {
        return new DocumentCommand.InsertFormValue(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("insertFunctionValue") == 0)
      {
        return new DocumentCommand.InsertFunctionValue(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("setGroups") == 0)
      {
        return new DocumentCommand.SetGroups(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("setPrintFunction") == 0)
      {
        return new DocumentCommand.SetPrintFunction(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("draftOnly") == 0)
      {
        return new DocumentCommand.DraftOnly(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("notInOriginal") == 0)
      {
        return new DocumentCommand.NotInOriginal(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("allVersions") == 0)
      {
        return new DocumentCommand.AllVersions(wmCmd, bookmark);
      }

      else if (cmd.compareToIgnoreCase("setJumpMark") == 0)
      {
        return new DocumentCommand.SetJumpMark(wmCmd, bookmark);
      }

      throw new InvalidCommandException("Unbekanntes Kommando \"" + cmd + "\"");
    }
    catch (InvalidCommandException e)
    {
      return new DocumentCommand.InvalidCommand(wmCmd, bookmark, e);
    }
  }

  /**
   * Diese Methode liefert einen Iterator �ber alle Elemente des Kommandobaumes
   * ohne dem Wurzelelement, wobei die Elemente bei reverse==false in der
   * Reihenfolge einer Tiefensuche zur�ckgeliefert werden - bei reverse==true
   * wird ebenfalls eine Tiefensuche durchgef�hrt, nur dass mit dem jeweils
   * letzten Element der Kinderliste begonnen wird.
   * 
   * @param reverse
   *          gibt an, ob bei der Tiefensuche mit dem jeweils letzten Element
   *          der Kinderliste begonnen wird.
   * @return Iterator �ber alle Elemente des Kommandobaumes ohne dem
   *         Wurzelelement.
   */
  public Iterator depthFirstIterator(boolean reverse)
  {
    Vector list = new Vector();
    depthFirstAddToList(root, list, reverse);
    return list.iterator();
  }

  /**
   * Hilfsfunktion f�r die Erstellung des depthFirstIterators - f�gt das
   * Kommando cmd und dessen Kinder (rekursiv) zur Liste list hinzu. Ist
   * Reverse=true, so wird beim Hinzuf�gen der Kinder in umgekehrter Reihenfolge
   * verfahren.
   * 
   * @param cmd
   * @param list
   * @param reverse
   */
  private void depthFirstAddToList(DocumentCommand cmd, List list,
      boolean reverse)
  {
    // Element hinzuf�gen (ausser RootElement)
    if (!(cmd instanceof RootElement)) list.add(cmd);

    // Kinder hinzuf�gen
    ListIterator i = cmd.getChildIterator();

    while (i.hasNext())
    {
      DocumentCommand child = (DocumentCommand) i.next();
      if (!reverse) depthFirstAddToList(child, list, false);
    }
    if (!reverse) return;
    // Der vorherige Vorw�rtsdurchgang positoniert zugleich den Iterator ans
    // Ende. Von da an kann man jetzt r�ckw�rts gehen.
    while (i.hasPrevious())
    {
      DocumentCommand child = (DocumentCommand) i.previous();
      if (reverse) depthFirstAddToList(child, list, true);
    }
  }

  /**
   * Die Methode gibt eine String Repr�sentation des gesamten Kommandobaumes
   * zur�ck und ist f�r test- und debugging-Zwecke gedacht.
   */
  public String dumpTree()
  {
    return dumpTree(root, "");
  }

  /**
   * Hilfmethode f�r dumpTree()
   * 
   * @param cmd
   * @param spaces
   * @return
   */
  private String dumpTree(DocumentCommand cmd, String spaces)
  {
    String str = spaces + cmd.toString() + " GROUPS: " + cmd.getGroups() + "\n";

    // und jetzt die Kinder
    Iterator i = cmd.getChildIterator();
    while (i.hasNext())
    {
      str += dumpTree(((DocumentCommand) i.next()), spaces + "    ");
    }

    return str;
  }

  /**
   * Main-Methode f�r Testzwecke. Erzeugt einen Kommandobaum aus dem aktuell in
   * OOo ge�ffneten Dokument und gibt diesen auf System.out aus.
   * 
   * @param args
   * @throws BootstrapException
   * @throws Exception
   */
  public static void main(String[] args) throws BootstrapException, Exception
  {
    Logger.init("all");

    XComponentContext ctx = Bootstrap.bootstrap();

    XDesktop xDesktop = (XDesktop) UnoRuntime.queryInterface(
        XDesktop.class,
        ctx.getServiceManager().createInstanceWithContext(
            "com.sun.star.frame.Desktop",
            ctx));

    UnoService compo = new UnoService(xDesktop.getCurrentComponent());
    DocumentCommandTree tree = new DocumentCommandTree(compo
        .xBookmarksSupplier());
    tree.update();

    System.out.println(tree.dumpTree());

    System.exit(0);
  }

  public static class TreeExecutor implements DocumentCommand.Executor
  {
    /**
     * Durchl�uft den Dokumentenbaum tree in der in reverse angegebener Richtung
     * und f�hrt alle enthaltenen Dokumentkommandos aus, die nicht den Status
     * DONE=true oder ERROR=true besitzen. Der Wert reverse=false entspricht
     * dabei der normalen Reihenfolge einer Tiefensuche, bei reverse=true wird
     * der Baum zwar ebenfalls in einer Tiefensuche durchlaufen, jedoch wird
     * stets mit dem letzten Kind angefangen.
     * 
     * @param tree
     * @param reverse
     * @return Anzahl der bei der Ausf�hrung aufgetretenen Fehler.
     */
    protected int executeDepthFirst(DocumentCommandTree tree, boolean reverse)
    {
      int errors = 0;

      // Alle (neuen) DocumentCommands durchlaufen und mit execute aufrufen.
      Iterator iter = tree.depthFirstIterator(reverse);
      while (iter.hasNext())
      {
        DocumentCommand cmd = (DocumentCommand) iter.next();

        if (cmd.isDone() == false && cmd.hasError() == false)
        {
          // Kommando ausf�hren und Fehler z�hlen
          errors += cmd.execute(this);
        }
      }
      return errors;
    }

    public int executeCommand(RootElement cmd)
    {
      return 0;
    }

    public int executeCommand(InsertFrag cmd)
    {
      return 0;
    }

    public int executeCommand(InsertValue cmd)
    {
      return 0;
    }

    public int executeCommand(InsertContent cmd)
    {
      return 0;
    }

    public int executeCommand(Form cmd)
    {
      return 0;
    }

    public int executeCommand(InvalidCommand cmd)
    {
      return 0;
    }

    public int executeCommand(UpdateFields cmd)
    {
      return 0;
    }

    public int executeCommand(SetType cmd)
    {
      return 0;
    }

    public int executeCommand(InsertFormValue cmd)
    {
      return 0;
    }

    public int executeCommand(SetGroups cmd)
    {
      return 0;
    }

    public int executeCommand(InsertFunctionValue cmd)
    {
      return 0;
    }

    public int executeCommand(SetPrintFunction cmd)
    {
      return 0;
    }

    public int executeCommand(DraftOnly cmd)
    {
      return 0;
    }

    public int executeCommand(NotInOriginal cmd)
    {
      return 0;
    }

    public int executeCommand(AllVersions cmd)
    {
      return 0;
    }

    public int executeCommand(SetJumpMark cmd)
    {
      return 0;
    }
  }

}
