/*
 * Dateiname: DocumentCommand.java
 * Projekt  : WollMux
 * Funktion : Beschreibt ein Element aus einer hierarchischen Struktur von
 *            DokumentKommandos und dessen Status.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 07.11.2005 | LUT | Erstellung als WMCommandState
 * 23.01.2005 | LUT | Erweiterung zum hierarchischen WM-Kommando
 * 26.04.2005 | LUT | Komplette �berarbeitung und Umbenennung in DocumentCommand
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;

/**
 * Beschreibt ein Dokumentkommando, das als Element eines hierarchischen
 * Kommandobaumes weitere Kinder-Elemente enthalten kann und weitere
 * Eigenschaften wie z.B. den Gruppenzugeh�rigkeit, Sichtbarkeit und
 * Ausf�hrstatus besitzt.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
abstract public class DocumentCommand
{
  /**
   * Das geparste ConfigThingy des zugeh�renden Bookmarks.
   */
  private ConfigThingy wmCmd;

  /**
   * Die Liste aller Kinder-Elemente.
   */
  private LinkedList childs; // Kinder vom Typ DocumentCommand

  // private DocumentCommand parent;

  /**
   * Das zu diesem DokumentCommand geh�rende Bookmark.
   */
  private Bookmark bookmark;

  // Status-Attribute:

  /**
   * Vorbelegung f�r den Status DONE.
   */
  private static final Boolean STATE_DEFAULT_DONE = new Boolean(false);

  /**
   * Vorbelegung f�r den Status ERROR
   */
  private static final Boolean STATE_DEFAULT_ERROR = new Boolean(false);

  /**
   * Enth�lt den aktuellen DONE-Status oder null, falls der Wert nicht ver�ndert
   * wurde.
   */
  private Boolean done;

  /**
   * Enth�lt den aktuellen EROOR-Status oder null, falls der Wert nicht
   * ver�ndert wurde.
   */
  private Boolean error;

  // Einf�gemarken und Status f�r InsertCursor zum sicheren Einf�gen von
  // Inhalten

  private boolean hasInsertMarks;

  private static final String INSERT_MARK_OPEN = "<";

  private static final String INSERT_MARK_CLOSE = ">";

  // Relationen zweier Dokument-Kommandos A und B:

  private static final int REL_B_IS_CHILD_OF_A = 1;

  private static final int REL_B_IS_PARENT_OF_A = -1;

  private static final int REL_B_OVERLAPS_A = 0;

  private static final int REL_B_IS_SIBLING_BEFORE_A = -2;

  private static final int REL_B_IS_SIBLING_AFTER_A = 2;

  /* ************************************************************ */

  /**
   * Der Konstruktor liefert eine Instanz eines DocumentCommand an der Stelle
   * des Bookmarks bookmark mit dem geparsten Kommando wmCmd.
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
    // this.parent = null; TODO: wieder rein!
    this.childs = new LinkedList();
    this.hasInsertMarks = false;
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
   * Diese Methode liefert eine String-Repr�sentation des DokumentCommands
   * zur�ck. Die String-Repr�sentation hat den Aufbau DocumentCommand[<bookmarkName>].
   */
  public String toString()
  {
    return "" + this.getClass().getSimpleName() + "[" + getBookmarkName() + "]";
  }

  /**
   * F�gt dem aktuellen Dokumentkommando ein neues Kind-Element b hinzu. Dabei
   * werden die TextRanges der beiden Kommandos verglichen und deren Relation
   * bestimmt. Je nach Relation wird das neue Kommando als Kind vor oder nach
   * einem bereits bestehenden Kind eingeh�ngt oder die Anfrage an ein
   * Kind-Element weitergereicht.
   * 
   * @param b
   *          Das Kindelement b
   * @return Die Anzahl der aufkummulierten TextRange Vergleiche, die zum
   *         Hinzuf�gen ben�tigt wurden. Kann f�r Performance-Statistiken
   *         verwendet werden.
   */
  protected int add(DocumentCommand b)
  {
    int compareCount = 0;

    ListIterator childsIterator = childs.listIterator(childs.size());
    while (childsIterator.hasPrevious())
    {
      DocumentCommand a = (DocumentCommand) childsIterator.previous();
      int rel = a.getRelation(b);
      compareCount++;

      // b liegt vor a: weiter gehen
      if (rel == REL_B_IS_SIBLING_BEFORE_A)
      {
        // Weiter gehen
      }

      // b liegt nach a: danach einf�gen
      if (rel == REL_B_IS_SIBLING_AFTER_A)
      {
        // b.parent = this; TODO: wieder rein!
        childsIterator.next();
        childsIterator.add(b);
        return compareCount;
      }

      // b ist Vater von a: a aush�ngen und zum Kind von b machen.
      if (rel == REL_B_IS_PARENT_OF_A)
      {
        // a.parent = b; TODO: wieder rein!
        b.add(a);
        childsIterator.remove();
      }

      // a ist Vater von b:
      if (rel == REL_B_IS_CHILD_OF_A)
      {
        compareCount += a.add(b);
        return compareCount;
      }

      // identische ranges
      if (rel == REL_B_OVERLAPS_A)
      {
        Logger
            .error("Ignoriere Dokumentkommando \""
                   + b.getBookmarkName()
                   + "\", da es sich mit dem Dokumentkommando \""
                   + a.getBookmarkName()
                   + "\" �berlappt. Diese beiden Kommandos d�rfen sich nicht �berlappen!");
        return compareCount;
      }
    }

    // falls bisher noch nicht hinzugef�gt: am Anfang einf�gen.
    // b.parent = this; TODO: wieder rein!
    childsIterator.add(b);
    return compareCount;
  }

  /**
   * Liefert einen ListIterator �ber alle Kinder-Elemente des Dokumentkommandos.
   * 
   * @return Liefert einen ListIterator �ber alle Kinder-Elemente des
   *         Dokumentkommandos.
   */
  protected ListIterator getChildIterator()
  {
    return childs.listIterator();
  }

  /**
   * Diese Methode liefert die Beziehung des aktuellen DocumentCommand und des
   * DocumentCommand b in Form einer Konstante DocumentCommand.REL_xxx zur�ck.
   * 
   * @param b
   *          Das DocumentCommand b, mit dem das aktuelle DocumentCommand
   *          verglichen wird.
   * @return Die Relation der zwei DocumentCommands in Form einer Konstante
   *         DocumentCommand.REL_xxx.
   */
  protected int getRelation(DocumentCommand b)
  {
    int cmp = bookmark.compare(b.bookmark);

    if (cmp == Bookmark.POS_AABB) return REL_B_IS_SIBLING_AFTER_A;
    if (cmp == Bookmark.POS_BBAA) return REL_B_IS_SIBLING_BEFORE_A;

    if (cmp == Bookmark.POS_88AA) return REL_B_IS_CHILD_OF_A;
    if (cmp == Bookmark.POS_A88A) return REL_B_IS_CHILD_OF_A;
    if (cmp == Bookmark.POS_AA88) return REL_B_IS_CHILD_OF_A;

    if (cmp == Bookmark.POS_88BB) return REL_B_IS_PARENT_OF_A;
    if (cmp == Bookmark.POS_B88B) return REL_B_IS_PARENT_OF_A;
    if (cmp == Bookmark.POS_BB88) return REL_B_IS_PARENT_OF_A;

    if (cmp == Bookmark.POS_8888)
    {
      if (this.canHaveChilds() && b.canHaveChilds())
        return REL_B_IS_CHILD_OF_A;
      else if (this.canHaveChilds())
        return REL_B_IS_CHILD_OF_A;
      else if (b.canHaveChilds()) return REL_B_IS_PARENT_OF_A;
      return REL_B_OVERLAPS_A;
    }

    return REL_B_IS_SIBLING_AFTER_A;
  }

  /**
   * Diese Methode gibt Auskunft dar�ber, ob ein DocumentCommand Unterkommandos
   * besitzen darf. Ein Kommando darf Kinder besitzen, wenn es theoretisch mehr
   * als einen atomaren Textinhalt umschlie�en kann. Ein Atom (kleinste, nicht
   * teilbare Einheit) selbst darf jedoch keine Kinder besitzen. Beispiel: Ein
   * WM(CMD'insertFrag'...) kann nach der Aufl�sung mehrere Unterelemente
   * besitzen und w�rde daher "true" zur�ckliefern. Ein Kommando
   * WM(CMD'insertValue'...) hingegen repr�sentiert exakt einen atomaren Wert
   * und darf keine weiteren Kinder mehr enthalten.
   * 
   * @return
   */
  protected abstract boolean canHaveChilds();

  /**
   * Callbackfunktion f�r die Ausf�hrung des Dokumentkommandos in einem
   * DocumentCommand.Executor wie z.B. dem DocumentCommandInterpreter. Die
   * Methode liefert die Anzahl der bei der Ausf�hrung entstandenen Fehler
   * zur�ck.
   * 
   * @param executor
   * @return Anzahl der aufgetretenen Fehler
   */
  public abstract int execute(DocumentCommand.Executor executor);

  /**
   * Diese Methode erm�glicht den Zugriff auf den vollst�ndigen TextRange des
   * Bookmarks, welches das DocumentCommand definiert - die Methode darf aber
   * nur verwendet werden, um die Inhalte an dieser Positon auszulesen und nicht
   * um neue Textinhalte an dieser Stelle einzuf�gen (daf�r gibt es
   * createInsertCursor()); ist das Bookmark nicht (mehr) vorhanden, wird null
   * zur�ck geliefert.
   * 
   * @return
   */
  public XTextRange getTextRange()
  {
    return bookmark.getTextRange();
  }

  /**
   * Erzeugt einen Cursor, der ein sicheres Einf�gen von Inhalten an der
   * Cursorposition gew�hrleistet, ohne dabei die Hierarchie oder Ordnung der
   * Dokumentkommandos zu zerst�ren. Kann das Dokumentkommando Kinder besitzen,
   * so wird der Cursor in INSERT_MARKs eingeschlossen. Kann das
   * Dokumentkommando keine Kinder besitzen, so wird ein einfacher TextCursor
   * aus dem vollen TextRange des Bookmarks erzeugt. Ist das Bookmark nicht
   * (mehr) vorhanden, wird null zur�ck geliefert. Jeder Aufruf von
   * createInsertCursor �berschreibt die bestehenden Inhalte des bisherigen
   * DocumentCommands. Der InsertCursor von Dokumentkommandos mit Kindern sieht
   * in etwa wie folgt aus: "<" CURSOR ">". Der InsertCursor hat direkt nach
   * der Erzeugung keine Ausdehnung. Die INSERT_MARKS "<" und ">" k�nnen nach
   * Beendigung der Dokumentgenerierung �ber die Methode cleanInsertMarks()
   * wieder entfernt werden.
   * 
   * @return XTextCursor zum Einf�gen oder null
   */
  public XTextCursor createInsertCursor()
  {
    XTextRange range = bookmark.getTextRange();

    if (range != null)
    {
      if (canHaveChilds())
      {
        // wenn
        XTextCursor cursor = range.getText().createTextCursorByRange(range);
        cursor.setString(INSERT_MARK_OPEN + INSERT_MARK_CLOSE);
        hasInsertMarks = true;

        bookmark.rerangeBookmark(cursor);

        cursor.goRight((short) INSERT_MARK_OPEN.length(), false);
        cursor.collapseToStart();

        return cursor;
      }
      else
        return range.getText().createTextCursorByRange(range);
    }
    return null;
  }

  /**
   * Hat das DocumentCommand �ber createInsertCursor Einf�gemarken erzeugt,
   * werden diese aus dem Dokumentkommando gel�scht.
   */
  public void cleanInsertMarks()
  {
    if (hasInsertMarks)
    {
      XTextRange range = bookmark.getTextRange();
      if (range != null)
      {

        // INSERT_MARKs mit Hilfe eines Cursors von links und rechts l�schen
        XTextCursor cursor;

        // INSERT_MARK links l�schen:
        cursor = range.getText().createTextCursorByRange(range.getStart());
        cursor.goRight((short) INSERT_MARK_OPEN.length(), true);
        cursor.setString("");

        // INSERT_MARK rechts l�schen:
        cursor = range.getText().createTextCursorByRange(range.getEnd());
        cursor.goLeft((short) INSERT_MARK_CLOSE.length(), true);
        cursor.setString("");
        hasInsertMarks = false;
      }
    }
  }

  /**
   * Beschreibt ob das Kommando bereits abgearbeitet wurde. Ist DONE bisher noch
   * nicht definiert oder gesetzt worden, so wird der Defaultwert false
   * zur�ckgeliefert.
   * 
   * @return true, falls das Kommando bereits bearbeitet wurde, andernfalls
   *         false.
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
   * Setzt den Status f�r das Attribut DONE.
   * 
   * @param done
   *          true, signalisiert, dass das Kommando bereits bearbeitet wurde,
   *          false das Gegenteil.
   */
  public void setDoneState(boolean done)
  {
    this.done = new Boolean(done);
  }

  /**
   * Liefert den Fehler-Status der Kommandobearbeitung zur�ck. Ist das Attribut
   * ERROR bisher nicht definiert oder kein Fehler gesetzt worden, so wird der
   * Defaultwert false zur�ckgliefert.
   * 
   * @return true, wenn bei der Kommandobearbeitung Fehler auftraten;
   *         andernfalls false
   */
  public boolean hasError()
  {
    if (error != null)
      return error.booleanValue();
    else if (isDefinedState("ERROR"))
    {
      Boolean errorBool = new Boolean(getState("ERROR").toString());
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
    this.error = new Boolean(error);
  }

  /**
   * Liefert ein ConfigThingy, das ein "WM"-Kommando mit allen
   * Statusinformationen enth�lt. Neue Unterknoten werden dabei nur angelegt,
   * wenn dies unbedingt erforderlich ist, d.h. wenn ein Wert vom Defaultwert
   * abweicht oder der Wert bereits vorher gesetzt war.
   * 
   * @return Ein ConfigThingy, das das "WM"-Kommando mit allen
   *         Statusinformationen enth�lt.
   */
  private ConfigThingy toConfigThingy()
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
   * Schribt den neuen Status des Dokumentkommandos in das Dokument zur�ck oder
   * l�scht ein Bookmark, wenn der Status DONE=true gesetzt ist - Die Methode
   * liefert entweder den Namen des neuen Bookmarks, welches die neuen
   * Statusinformationen enth�lt zur�ck, oder null, wenn das zugeh�rige Bookmark
   * gel�scht wurde. Ist der debug-modus gesetzt, so werden in gar keinem Fall
   * Bookmarks gel�scht, womit die Fehlersuche erleichtert werden soll.
   * 
   * @return der Name des neuen Bookmarks oder null.
   */
  public String updateBookmark(boolean debug)
  {
    if (!isDone() || debug)
    {
      // Neues WM-String zusammenbauen, der keine Zeilenvorsch�be und
      // abschlie�ende Leerzeichen enth�lt:
      String wmCmdString = toConfigThingy().stringRepresentation(true, '\'');
      wmCmdString = wmCmdString.replaceAll("[\r\n]+", " ");
      while (wmCmdString.endsWith(" "))
        wmCmdString = wmCmdString.substring(0, wmCmdString.length() - 1);

      // Neuen Status rausschreiben:
      bookmark.rename(wmCmdString);

      return bookmark.getName();
    }
    else
    {
      bookmark.remove();
      return null;
    }
  }

  /**
   * Gibt Auskunft, ob ein Key unterhalb des STATE-Knotens definiert ist. z.B.
   * "WM(...) STATE (KEY '...')"
   * 
   * @param key
   * @return true, falls der Key definiert ist, andernfalls false.
   */
  private boolean isDefinedState(String key)
  {
    return (getState(key) != null);
  }

  /**
   * Liefert das ConfigThingy zu dem gesuchten Key key unterhalt des
   * STATE-Knotens.
   * 
   * @param key
   * @return
   */
  private ConfigThingy getState(String key)
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
   * Setzt einen Schl�ssel-Wert-Paar unterhalb des STATE-Knotens. Ist der
   * Schl�ssel bereits definiert, wird der bestehende Wert �berschrieben. Sind
   * der STATE-Knoten oder der Schl�ssel nicht definiert, so werden die
   * entsprechenden Knoten erzeugt und der Key key erh�lt ein Kindknoten mit dem
   * Value value.
   * 
   * @param key
   * @param value
   */
  private void setOrCreate(String key, String value)
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

    public int executeCommand(DocumentCommand.Version cmd);
  }

  // ********************************************************************************
  /**
   * Beschreibt ein Dokumentkommando, das Dokumentinhalte �ber
   * insertDocumentFromURL einf�gt (Derzeit implementieren insertFrag und
   * insertContent dieses Interface).
   */
  static interface FragmentInserter
  {
  };

  // ********************************************************************************
  /**
   * Eine Exception die geworfen wird, wenn ein Dokumentkommando als ung�ltig
   * erkannt wurde, z,b, aufgrund eines fehlenden Parameters.
   */
  static public class InvalidCommandException extends Exception
  {
    private static final long serialVersionUID = -3960668930339529734L;

    public InvalidCommandException(String message)
    {
      super(message);
    }
  }

  // ********************************************************************************
  /**
   * Beschreibt ein Dokumentkommando, das aufgrund eines Syntax-Fehlers oder
   * eines fehlenden Parameters ung�ltig ist, jedoch trotzdem im Dokument als
   * Fehlerfeld dargestellt werden soll.
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

    protected boolean canHaveChilds()
    {
      return true;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    public String updateBookmark()
    {
      // der updateBookmark darf in diesem Fall nat�rlich nichts rausschreiben.
      return getBookmarkName();
    }
  }

  // ********************************************************************************
  /**
   * Beschreibt das Dokumentkommando Form, welches Zugriff auf die
   * Formularbeschreibung des Dokuments erm�glicht, die in Form einer Notiz
   * innerhalb des zum Form-Kommando zugeh�rigen Bookmarks abgelegt ist.
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

    protected boolean canHaveChilds()
    {
      return false;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Dieses besondere Element dient als Wurzel f�r den DocumentCommandTree.
   */
  static public class RootElement extends DocumentCommand
  {
    public RootElement()
    {
      super(new ConfigThingy("WM"), null);
    }

    public String getBookmarkName()
    {
      return "<root>";
    }

    protected int getRelation(DocumentCommand b)
    {
      return REL_B_IS_CHILD_OF_A;
    }

    protected boolean canHaveChilds()
    {
      return true;
    }

    public int execute(DocumentCommand.Executor e)
    {
      return 0;
    }
  }

  // ********************************************************************************
  /**
   * Das Kommando InsertFrag f�gt ein externes Textfragment in das Dokument ein.
   */
  static public class InsertFrag extends DocumentCommand implements
      FragmentInserter
  {
    private String fragID;

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
        throw new InvalidCommandException("Fehlendes Attribut FRAG_ID");
      }
    }

    public String getFragID()
    {
      return fragID;
    }

    protected boolean canHaveChilds()
    {
      return true;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    protected int add(DocumentCommand b)
    {
      if (b instanceof InsertFrag)
      {
        InsertFrag bif = (InsertFrag) b;
        // Falls sich ein Fragment selbst aufruft, wandle das Kommando um in ein
        // InvalidCommand - zur Vermeidung von Endlosschleifen.
        if (this.getFragID().equals(bif.getFragID()))
        {
          InvalidCommandException ivc = new InvalidCommandException(
              "Das Fragment mit der FRAG_ID \""
                  + fragID
                  + "\" ruft sich direkt oder indirekt selbst auf "
                  + "und w�rde damit eine Endlosschleife verursachen.");
          b = new InvalidCommand(b.wmCmd, b.bookmark, ivc);
        }
      }
      return super.add(b);
    }
  }

  // ********************************************************************************
  /**
   * Das Kommando InsertContent dient zum Mischen von Dokumenten und ist im
   * Handbuch des WollMux ausf�hrlicher beschrieben.
   */
  static public class InsertContent extends DocumentCommand implements
      FragmentInserter
  {
    private String fragID;

    public InsertContent(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);
    }

    public String getFragID()
    {
      return fragID;
    }

    protected boolean canHaveChilds()
    {
      return true;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Dieses Kommando f�gt den Wert eines Absenderfeldes in den Briefkopf ein.
   */
  static public class InsertValue extends DocumentCommand
  {
    private String dbSpalte;

    private String leftSeparator = "";

    private String rightSeparator = "";

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
        throw new InvalidCommandException("Fehlendes Attribut DB_SPALTE");
      }

      // Auswertung der AUTOSEP bzw SEPERATOR-Attribute
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
              "Unbekannter AUTOSEP-Typ \""
                  + as.toString()
                  + "\". Erwarte \"left\", \"right\" oder \"both\".");
        }
        currentSep = sep;
      }
    }

    public String getDBSpalte()
    {
      return dbSpalte;
    }

    protected boolean canHaveChilds()
    {
      return false;
    }

    public String getLeftSeparator()
    {
      return leftSeparator;
    }

    public String getRightSeparator()
    {
      return rightSeparator;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  /**
   * Dieses Kommando f�gt die Versionsnummer der aktuellen WollMux-Installation
   * in das Dokument ein.
   */
  static public class Version extends DocumentCommand
  {
    public Version(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);
    }

    protected boolean canHaveChilds()
    {
      return false;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  // ********************************************************************************
  static public class ON extends DocumentCommand
  {
    public ON(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);
    }

    protected boolean canHaveChilds()
    {
      return false;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return 0;
    }
  }

  // ********************************************************************************
  static public class OFF extends DocumentCommand
  {
    public OFF(ConfigThingy wmCmd, Bookmark bookmark)
        throws InvalidCommandException
    {
      super(wmCmd, bookmark);
    }

    protected boolean canHaveChilds()
    {
      return false;
    }

    public int execute(DocumentCommand.Executor visitable)
    {
      return 0;
    }
  }
}
