//TODO L.m()
/*
 * Dateiname: TextRangeRelation.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse repr�sentiert die m�glichen Beziehungen zwischen 
 *            zwei Textbereichen a und b.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 30.10.2007 | LUT | Erstellung als TextRangeRelation
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;

import de.muenchen.allg.afid.UNO;

/**
 * Es gibt viele M�glichkeiten wie zwei Textbereiche (XTextRange-Objekte)
 * zueinander angeordnet sein k�nnen; Diese Klasse �bersetzt die m�glichen
 * Beziehungen in eine verst�ndliche Form und repr�sentiert damit die Beziehung
 * zwischen zwei Textbereichen.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class TextRangeRelation
{
  /**
   * Ein interner integer-Wert der das Ergebnis von compareTextRanges() enth�lt.
   */
  private int rel;

  /**
   * Erzeugt ein neues Objekt das die Beziehung der Textbereiche a und b
   * darstellt.
   */
  public TextRangeRelation(XTextRange a, XTextRange b)
  {
    this.rel = compareTextRanges(a, b);
  }

  /**
   * Der Textbereich b tritt im Dokument vor dem Textbereich a auf. Das
   * Orderscheme beschreibt dabei die Position anschaulich und ist wie folgt
   * definiert: A:=Der Textbereich a steht an dieser Stelle alleine, 8:=Die
   * Textbereiche a und b �berlappen sich an dieser Stelle, B:=der Textbereich b
   * steht an dieser Stelle alleine.
   */
  public boolean followsOrderschemeBBAA()
  {
    return rel == -4;
  }

  /**
   * Der Textbereich b startet vor dem Textbereich a, aber h�rt gleichzeitig mit
   * A auf. Das Orderscheme beschreibt dabei die Position anschaulich und ist
   * wie folgt definiert: A:=Der Textbereich a steht an dieser Stelle alleine,
   * 8:=Die Textbereiche a und b �berlappen sich an dieser Stelle, B:=der
   * Textbereich b steht an dieser Stelle alleine.
   */
  public boolean followsOrderschemeBB88()
  {
    return rel == -3;
  }

  /**
   * Der Textbereich b enth�lt den Textbereich a vollst�ndig. Das Orderscheme
   * beschreibt dabei die Position anschaulich und ist wie folgt definiert:
   * A:=Der Textbereich a steht an dieser Stelle alleine, 8:=Die Textbereiche a
   * und b �berlappen sich an dieser Stelle, B:=der Textbereich b steht an
   * dieser Stelle alleine.
   */
  public boolean followsOrderschemeB88B()
  {
    return rel == -2;
  }

  /**
   * Der Textbereich b startet mit dem Textbereich a, h�rt jedoch vor dem
   * Textbereich a auf. Das Orderscheme beschreibt dabei die Position
   * anschaulich und ist wie folgt definiert: A:=Der Textbereich a steht an
   * dieser Stelle alleine, 8:=Die Textbereiche a und b �berlappen sich an
   * dieser Stelle, B:=der Textbereich b steht an dieser Stelle alleine.
   */
  public boolean followsOrderscheme88AA()
  {
    return rel == -1;
  }

  /**
   * A und B liegen an der selben Position. Das Orderscheme beschreibt dabei die
   * Position anschaulich und ist wie folgt definiert: A:=Der Textbereich a
   * steht an dieser Stelle alleine, 8:=Die Textbereiche a und b �berlappen sich
   * an dieser Stelle, B:=der Textbereich b steht an dieser Stelle alleine.
   */
  public boolean followsOrderscheme8888()
  {
    return rel == -0;
  }

  /**
   * Der Textbereich a startet mit dem Textbereich b, h�rt jedoch vor dem
   * Textbereich b auf Das Orderscheme beschreibt dabei die Position anschaulich
   * und ist wie folgt definiert: A:=Der Textbereich a steht an dieser Stelle
   * alleine, 8:=Die Textbereiche a und b �berlappen sich an dieser Stelle,
   * B:=der Textbereich b steht an dieser Stelle alleine.
   */
  public boolean followsOrderscheme88BB()
  {
    return rel == 1;
  }

  /**
   * Der Textbereich a enth�lt den Textbereich b vollst�ndig. Das Orderscheme
   * beschreibt dabei die Position anschaulich und ist wie folgt definiert:
   * A:=Der Textbereich a steht an dieser Stelle alleine, 8:=Die Textbereiche a
   * und b �berlappen sich an dieser Stelle, B:=der Textbereich b steht an
   * dieser Stelle alleine.
   */
  public boolean followsOrderschemeA88A()
  {
    return rel == 2;
  }

  /**
   * Der Textbereich a startet vor dem Textbereich b, h�rt jedoch gemeinsam mit
   * dem Textbereich b auf. Das Orderscheme beschreibt dabei die Position
   * anschaulich und ist wie folgt definiert: A:=Der Textbereich a steht an
   * dieser Stelle alleine, 8:=Die Textbereiche a und b �berlappen sich an
   * dieser Stelle, B:=der Textbereich b steht an dieser Stelle alleine.
   */
  public boolean followsOrderschemeAA88()
  {
    return rel == 3;
  }

  /**
   * Der Textbereich a liegt im Dokument vor dem Textbereich b. Das Orderscheme
   * beschreibt dabei die Position anschaulich und ist wie folgt definiert:
   * A:=Der Textbereich a steht an dieser Stelle alleine, 8:=Die Textbereiche a
   * und b �berlappen sich an dieser Stelle, B:=der Textbereich b steht an
   * dieser Stelle alleine.
   */
  public boolean followsOrderschemeAABB()
  {
    return rel == 4;
  }

  /**
   * Die beiden Textbereiche vergleichbar und liegen im selben Text-Objekt.
   */
  public boolean rangesAreComparable()
  {
    return rel >= -4 && rel <= 4;
  }

  /**
   * Diese Klasse repr�sentiert die Beziehung zweier Textbereiche die abh�ngig
   * von ihrer Verschachtelung (�berlappung) in einem Baum angeordnet werden
   * sollen. Dabei legt die TreeRelation fest, ob die beiden Textbereiche
   * identisch, benachbart, Eltern bzw. Kinder des jeweils anderen oder gar
   * nicht vergleichbar sind.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static class TreeRelation
  {
    /**
     * Enth�lt die TextRangeRelation zu den Bereichen a und b.
     */
    private TextRangeRelation rel;

    /**
     * Erzeugt eine neue Relation f�r eine baumartige Anordnung der Textbereiche
     * a und b.
     */
    public TreeRelation(XTextRange a, XTextRange b)
    {
      this.rel = new TextRangeRelation(a, b);
    }

    /**
     * Erzeugt eine neue Relation f�r eine baumartige Anordnung der Textbereiche
     * a und b aus der TextRangeRelation rel.
     */
    public TreeRelation(TextRangeRelation rel)
    {
      this.rel = rel;
    }

    /**
     * Der Textbereich b ist Teil des Textbereichs a und damit in Kind von a.
     */
    public boolean isBChildOfA()
    {
      return rel.followsOrderscheme88AA() || rel.followsOrderschemeA88A()
             || rel.followsOrderschemeAA88();
    }

    /**
     * Der Textbereich a ist Teil des Textbereichs b und damit ein Kind von b.
     */
    public boolean isAChildOfB()
    {
      return rel.followsOrderscheme88BB() || rel.followsOrderschemeB88B()
             || rel.followsOrderschemeBB88();
    }

    /**
     * Die beiden Textbereiche sind benachbart, wobei a hinter b liegt.
     */
    public boolean isASiblingAfterB()
    {
      return rel.followsOrderschemeBBAA();
    }

    /**
     * Die beiden Textbereiche sind benachbart, wobei a vor b liegt.
     */
    public boolean isASiblingBeforeB()
    {
      return rel.followsOrderschemeAABB();
    }

    /**
     * Die beiden Textbereiche liegen exakt �bereinander.
     */
    public boolean isAEqualB()
    {
      return rel.followsOrderscheme8888();
    }

    /**
     * Die beiden Textbereiche vergleichbar und liegen im selben Text-Objekt.
     */
    public boolean rangesAreComparable()
    {
      return rel.rangesAreComparable();
    }

    /**
     * Liefert true, wenn der Textbereich A logisch vor dem Textbereich B
     * startet - Diese Bedingung ist genau dann erf�llt, wenn A als Nachbar vor
     * B liegt oder A den Textbereich B voll umschlie�t (also B ein Kind von A
     * ist) und A somit z.B. bei sortierten Listen vor B einsortiert geh�rt.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public boolean isALessThanB()
    {
      return isASiblingBeforeB() || isBChildOfA();
    }

    /**
     * Liefert true, wenn der Textbereich A logisch nach dem Textbereich B
     * startet - Diese Bedingung ist genau dann erf�llt, wenn A als Nachbar nach
     * B liegt oder B den Textbereich A voll umschlie�t (also A ein Kind von B
     * ist) und B somit z.B. bei sortierten Listen vor A einsortiert geh�rt.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public boolean isAGreaterThanB()
    {
      return isASiblingAfterB() || isAChildOfB();
    }
  }

  /**
   * Diese Methode vergleicht die beiden TextRanges a und b und liefert einen
   * internen Integer-Wert der durch diese Klasse vern�nftiger Form aufbereitet
   * wird.
   */
  private static int compareTextRanges(XTextRange a, XTextRange b)
  {
    // F�lle: A:=a alleine, 8:=�berlagerung von a und b, B:=b alleine
    // -4 = BBBBAAAA bzw. BB88AA
    // -3 = BB88
    // -2 = B88B
    // -1 = 88AA
    // +0 = 8888
    // +1 = 88BB
    // +2 = A88A
    // +3 = AA88
    // +4 = AAAABBBB bzw. AA88BB

    XTextRangeCompare compare = null;
    if (a != null) compare = UNO.XTextRangeCompare(a.getText());
    if (compare != null && a != null && b != null)
    {
      try
      {
        int start = compare.compareRegionStarts(a, b) + 1;
        int end = compare.compareRegionEnds(a, b) + 1;
        return (3 * start + 1 * end) - 4;
      }
      catch (IllegalArgumentException e)
      {
        // nicht loggen! Tritt regul�r auf, wenn Textbereiche mit verschiedenen
        // Text-Objekten verglichen werden.
      }
    }
    return -50;
  }
}
