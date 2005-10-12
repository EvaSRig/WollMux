/*
* Dateiname: ConfigThingy.java
* Projekt  : WollMux
* Funktion : Parsen und Repr�sentation von WollMux-Konfigurationsdateien
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 05.10.2005 | BNK | Erstellung
* 10.10.2005 | BNK | Parser fertiggestellt
*                  | InputStream -> URL, Unterst�tzung von includes
*                  | alles ab # ist jetzt Kommentar
*                  | Testprogramm hinzugef�gt
* 11.10.2005 | BNK | Einbetten von Zeilenumbr�chen mittels %n
*                  | Ausf�hrlich kommentiert
* 12.10.2005 | BNK | get() benennt jetzt Ergebnisknoten "<query results>"
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ein ConfigThingy repr�sentiert einen Knoten eines Baumes, der durch das
 * Parsen einer WollMux-Konfigurationsdatei entsteht. 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ConfigThingy
{
  /** Die Kindknoten. */
  private List children;
  /** Der Name des Knotens. Bei Bl�ttern ist dies der (String-)Wert des Knotens. */
  private String name;
  
  /**
   * Parst die Datei, die durch URL bestimmt ist.
   * @param name der Name der Wurzel des erzeugten ConfigThingy-Baumes.
   * @throws IOException falls das Laden von Daten von url (oder einer includeten
   * URL) fehlschl�gt.
   * @throws SyntaxErrorException falls beim Parsen der Daten von url ein
   * syntaktischer Fehler gefunden wird.
   */
  public ConfigThingy(String name, URL url) throws IOException
  {
    this(name);
    childrenFromUrl(url);
  }
  
  /**
   * Parst url und h�ngt die entsprechenden Knoten als Kinder an this an.
   * @throws IOException falls das Laden von Daten von url (oder einer includeten
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  protected void childrenFromUrl(URL url) throws IOException
  {
    Stack stack = new Stack();
    stack.push(this);
    List tokens = tokenize(url);
    Iterator liter = tokens.iterator();
    Token token1, token2;
    do{
      token1 = (Token)liter.next();
      ConfigThingy child;
      switch (token1.type())
      {
        case Token.INCLUDE:
            token2 = (Token)liter.next();
            switch (token2.type())
            {
              case Token.STRING:
                   try{
                     ((ConfigThingy)stack.peek()).childrenFromUrl(new URL(url,token2.contentString()));
                   } catch(IOException iox)
                   {
                     throw new IOException(token2.url()+" in Zeile "+token2.line()+" bei Zeichen "+token2.position()+": %include fehlgeschlagen: "+iox.toString());
                   }
                  break;
              default:
                  throw new SyntaxErrorException(token2.url()+": URL-String (umschlossen von G�nsef��chen) erwartet in Zeile "+token2.line()+" bei Zeichen "+token2.position());
            }
            break;
            
        case Token.KEY:
            token2 = (Token)liter.next();
            switch (token2.type())
            {
              case Token.OPENPAREN:
                  child = new ConfigThingy(token1.contentString());
                  ((ConfigThingy)stack.peek()).addChild(child);
                  stack.push(child);
                  break;
              case Token.STRING:
                  child = new ConfigThingy(token1.contentString());
                  ConfigThingy grandchild = new ConfigThingy(token2.contentString());
                  child.addChild(grandchild);
                  ((ConfigThingy)stack.peek()).addChild(child);
                  break;
              default:
                  throw new SyntaxErrorException(token2.url()+": Syntaxfehler in Zeile "+token2.line()+" bei Zeichen "+token2.position());
            }
            break;
        
        case Token.STRING:
            child = new ConfigThingy(token1.contentString());
            ((ConfigThingy)stack.peek()).addChild(child);
            break;
        
        case Token.CLOSEPAREN:
            //Achtung: Wurzel darf nicht gepoppt werden.
          if (stack.size() <= 1) throw new SyntaxErrorException(token1.url()+": Klammer ')' ohne passende Klammer '(' in Zeile "+token1.line()+" bei Zeichen "+token1.position()); 
          stack.pop();  
          break;
          
        case Token.OPENPAREN:
          child = new ConfigThingy("");
          ((ConfigThingy)stack.peek()).addChild(child);
          stack.push(child);
          break;

        case Token.END:
          break;

        default:
          throw new SyntaxErrorException(token1.url()+": Syntaxfehler in Zeile "+token1.line()+" bei Zeichen "+token1.position());
      }
      
    } while(token1.type() != Token.END);
    
    if (stack.size() > 1)
      throw new SyntaxErrorException(token1.url()+": "+(stack.size() - 1) + " schlie�ende Klammern fehlen");
  }
  
  /**
   * Erzeugt ein ConfigThingy mit Name/Wert name, ohne Kinder.
   */
  private ConfigThingy(String name)
  {
    this.name = name;
    this.children = new Vector();
  }
  
  /**
   * Erzeugt ein anonymes ConfigThingy mit Kindern aus children.
   */
  private ConfigThingy(String name, List children)
  {
    this.name=name;
    this.children = children;
  }

  /**
   * F�gt dem ConfigThingy ein weiteres Kind hinzu.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void addChild(ConfigThingy child)
  {
    children.add(child);
  }
  
  /**
   * Liefert die Anzahl der Kinder zur�ck.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int count()
  {
    return children.size();
  }
    
  /**
   * Liefert einen Iterator �ber die Kinder dieses ConfigThingys.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Iterator iterator()
  {
    return children.iterator();
  }
  
  /**
   * Liefert den Namen dieses Knotens des Config-Baumes zur�ck. Im Falle eines
   * Blattes entspricht dies dem (String-)Wert.
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getName()
  {
    return name;
  }

  /**
   * Durchsucht den Teilbaum mit Wurzel this nach Knoten mit Name name auf
   * Suchtiefe searchlevel und
   * f�gt sie (oder falls getParents==true ihre Eltern) in die Liste found ein.
   * @param parent Der Elternknoten von this.
   * @param name der Name nach dem zu suchen ist.
   * @param found in diese Liste werden die gefundenen Knoten 
   *        (oder falls getParents==true ihre Eltern) eingef�gt. Jeder Knoten
   *        taucht maximal einmal in dieser Liste auf, d.h. falls getParents==true
   *        und ein Knoten mehrere Kinder mit Name name hat, wird dieser Knoten
   *        trotzdem nur einmal eingef�gt.
   * @param parentLevel die Suchtiefe bei Breitensuche von parent, d.h. this hat Suchtiefe parentLevel + 1
   * @param searchLevel die Suchtiefe bei Breitensuche auf der die Knoten gesucht werden sollen.
   *        Knoten auf anderen Suchtiefen werden nicht in found eingef�gt. 
   * @param getParents falls true werden die Elternknoten statt der gefundenen
   *        Knoten eingef�gt. Jeder Elternknoten wird allerdings grunds�tzlich
   *        nur einmal eingef�gt, auch wenn er mehrere passende Kinder hat.
   * @return true falls noch mindestens ein Knoten mit Suchtiefe searchlevel
   *         erreicht wurde, d.h. falls eine Suche mit h�herem searchlevel
   *         prinzipiell Ergebnisse bringen k�nnte.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private boolean rollcall(ConfigThingy parent, String name, List found, int parentLevel, int searchLevel, boolean getParents)
  {
    int level = parentLevel + 1;
    if (searchLevel == level)
    {
      if (name.equals(this.name))
      {
        if (getParents)
        {
          if (!found.contains(parent)) found.add(parent);
        }
        else
          found.add(this);
      }
    }
    else //if searchLevel < level
    {
      Iterator iter = children.iterator();
      boolean haveMore = false;
      while (iter.hasNext())
      {
        ConfigThingy child = (ConfigThingy)iter.next();
        boolean result = child.rollcall(this, name, found, level, searchLevel, getParents); 
        haveMore = haveMore || result;
      }
      return haveMore;
    }
    return true;
  }
  
  
  /**
   * F�hrt eine Breitensuch nach Nachfahrenknoten von this durch, die name
   * als Name haben. 
   * @return null, falls kein entsprechender Knoten gefunden wurde. Falls
   * es entsprechende Knoten gibt, wird die niedrigste Suchtiefe bestimmt auf
   * der entsprechende Knoten zu finden sind und es werden alle Knoten auf
   * dieser Suchtiefe zur�ckgeliefert. Falls dies genau einer ist, wird er
   * direkt zur�ckgeliefert, ansonsten wird ein ConfigThingy mit Namen
   * "<query results>" geliefert, das diese Knoten (und nur diese) 
   * als Kinder hat. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy get(String name)
  {
    return get(name, false);
  }
  
  /**
   * Wie {@link #get(String)}, aber es werden die Elternknoten der gefundenen Knoten
   * zur�ckgeliefert anstatt der Knoten selbst. Es ist zu beachten, dass jeder
   * Elternknoten nur genau einmal in den Ergebnissen enthalten ist, auch wenn
   * er mehrere passende Kinder hat,
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy getByChild(String name)
  {
    return get(name, true);
  }
  
  /**
   * Falls getParents == false verh�lt sich diese Funktion wie {@link #get(String)},
   * falls getParents == true wie {@link #getByChild(String)}.
   * @param name
   * @param getParents
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  protected ConfigThingy get(String name, boolean getParents)
  {
    List found = new Vector();
    boolean haveMore;
    int searchlevel = 1;
    do{
      haveMore = rollcall(this, name, found, -1, searchlevel, getParents);
      ++searchlevel;
    }while(found.isEmpty() && haveMore );
    
    switch (found.size())
    {
      case 0: return null;
      case 1: return (ConfigThingy)found.get(0);
      default: return new ConfigThingy("<query results>",found); 
    }
  }
  
  /**
   * Falls der Knoten this ein Blatt ist wird der Name des Knotens geliefert,
   * ansonsten die Konkatenation aller Bl�tter des unter this
   * liegenden Teilbaums. 
   */
  public String toString()
  {
    if (children.isEmpty()) return name;
    StringBuffer buf = new StringBuffer();
    Iterator iter = children.iterator();
    while (iter.hasNext()) buf.append(iter.next().toString());
    return buf.toString();
  }
  
  /**
   * Die Methode {@link ConfigThingy#tokenize(URL)} liefert eine Liste von
   * Objekten, die alle dieses Interface implementieren.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private interface Token
  {
    public static final int KEY = 0;
    public static final int STRING = 1;
    public static final int OPENPAREN = 2;
    public static final int CLOSEPAREN = 3;
    public static final int END = 4;
    public static final int INCLUDE = 5;
    public static final int LINECOMMENT = 6;
    
    /**
     * Liefert die URL des Dokuments, in dem dieses Token gefunden wurde.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public URL url();
    
    /**
     * Liefert die Zeile in der dieses Token gefunden wurde.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public int line();
    
    /**
     * Liefert die Position des ersten Zeichens dieses Tokens in seiner Zeile,
     * gez�hlt ab 1.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public int position();
    
    /**
     * Liefert die Art dieses Tokens, z.B. {@link #KEY}.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public int type();

    /**
     * Liefert die Textrepr�sentation dieses Tokens. Diese ist NICHT zwangsweise
     * identisch mit der Zeichenfolge aus der dieses Token geparst wurde.
     * Zum Beispiel tauchen Trennzeichen wie die G�nsef��chen zur Abgrenzung
     * von Strings in dem hier zur�ckgelieferten String nicht auf. Ebensowenig
     * sind in diesem String Escape-Sequenzen zu finden, die im Eingabedatenstrom
     * verwendet werden, um bestimmte Zeichen wie z.B. Newline darzustellen.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public String contentString();
  }
  
  /**
   * Abstrakte Basis-Klasse f�r Tokens, die ihren {@link #contentString()} Wert
   * in einer String-Variable speichern.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static abstract class StringContentToken implements Token
  {
    protected String content;
    protected int myLine;
    protected int myPosition;
    protected URL myURL;
    
    public StringContentToken(URL url, int line, int position)
    {
      myURL = url;
      myLine = line;
      myPosition = position;
    }
    
    public URL url() {return myURL;}
    public int line() {return myLine;}
    public int position() {return myPosition;}
    public String contentString() {return content;}
  }
  

  /**
   * Token f�r einen String gem�� Syntax f�r WollMux-Config-Dateien.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class StringToken extends StringContentToken
  {
    /**
     * Regex zur Identifikation von legalen Strings.
     */
    private static Pattern p = Pattern.compile("^\"((([^\"])|(\"\"))*)\"");
    
    /**
     * Erzeugt ein neues StringToken 
     * @param tokenData ein String, f�r den {@link #atStartOf(String)} einen
     * Wert gr��er 0 zur�ckliefert.
     */
    public StringToken(String tokenData, URL url, int line, int position)
    {
      super(url, line, position);
      Matcher m = p.matcher(tokenData);
      if (!m.find()) throw new IllegalArgumentException("String token expected!");
      //extract string inside "..." and replace "" with ", %n with "\n" and 
      //%% with %
      content = m.group(1).replaceAll("\"\"","\"").replaceAll("%n","\n").replaceAll("%%","%");
    }
    public int type() {return Token.STRING;}
    
    /**
     * Liefert die L�nge des l�ngsten Prefixes von str, das sich als Token
     * dieser Klasse interpretieren l�sst.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public static int atStartOf(String str)
    {
      Matcher m = p.matcher(str);
      if (!m.find()) return 0;
      return m.end();
    }
  }
  
  /**
   * Ein Token f�r einen Schl�ssel gem�� Syntax f�r WollMux-Config-Dateien.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class KeyToken extends StringContentToken
  {
    /**
     * Regex zur Identifikation von legalen Schl�sseln.
     */
    private static Pattern p = Pattern.compile("^([a-zA-Z_][a-zA-Z_0-9]*)");
    
    /**
     * Erzeugt ein neues KeyToken 
     * @param tokenData ein String, f�r den {@link #atStartOf(String)} einen
     * Wert gr��er 0 zur�ckliefert.
     */
    public KeyToken(String tokenData, URL url, int line, int position)
    {
      super(url, line, position);
      Matcher m = p.matcher(tokenData);
      if (!m.find()) throw new IllegalArgumentException("Key token expected!");
      content = m.group(1);
    }
    public int type() {return Token.KEY;}
    
    /**
     * Liefert die L�nge des l�ngsten Prefixes von str, das sich als Token
     * dieser Klasse interpretieren l�sst.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public static int atStartOf(String str)
    {
      Matcher m = p.matcher(str);
      if (!m.find()) return 0;
      return m.end();
    }
  }

  /**
   * Token f�r �ffnende runde Klammer.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class OpenParenToken extends StringContentToken
  {
    public OpenParenToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = "(";
    }
    public int type() {return Token.OPENPAREN;}
    
    /**
     * Liefert 1, falls str mit '(' beginnt, ansonsten 0.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public static int atStartOf(String str)
    {
      return str.startsWith("(") ? 1 : 0;
    }
  }
  
  /**
   * Token f�r schlie�ende runde Klammer.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class CloseParenToken extends StringContentToken
  {
    public CloseParenToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = ")";
    }
    public int type() {return Token.CLOSEPAREN;}
    
    /**
     * Liefert 1, falls str mit ')' beginnt, ansonsten 0.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public static int atStartOf(String str)
    {
      return str.startsWith(")") ? 1 : 0;
    }
  }

  /**
   * Token f�r den String "%include".
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class IncludeToken extends StringContentToken
  {
    private static final String inc = "%include";
    public IncludeToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = inc;
    }
    public int type() {return Token.INCLUDE;}
    
    /**
     * Liefert die L�nge des l�ngsten Prefixes von str, das sich als Token
     * dieser Klasse interpretieren l�sst.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public static int atStartOf(String str)
    {
      return str.startsWith(inc) ? inc.length() : 0;
    }
  }

  /**
   * Token f�r einen Kommentar gem�� Syntax von WollMux-Config-Dateien.
   * ACHTUNG: Tokens dieser Klasse werden derzeit von {@link ConfigThingy#tokenize(URL)}
   * nicht zur�ckgeliefert, sondern verworfen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class LineCommentToken extends StringContentToken
  {
  
    /**
     * Erzeugt einen neuen LineCommenToken.
     * @param tokenData ein String, dessen erstes Zeichen '#' ist.
     */
    public LineCommentToken(String tokenData, URL url, int line, int position)
    {
      super(url, line, position);
      content = tokenData.substring(1);
    }
    public int type() {return Token.LINECOMMENT;}
  
    /**
     * Liefert die L�nge des l�ngsten Prefixes von str, das sich als Token
     * dieser Klasse interpretieren l�sst.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public static int atStartOf(String str)
    {
      return str.startsWith("#") ? str.length() : 0;
    }
  }
  
  /**
   * Signalisiert das Ende des Eingabedatenstroms.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class EndToken extends StringContentToken
  {
    public EndToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = "";
    }
    public int type() {return Token.END;}
   
    /**
     * Liefert immer 0.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public static int atStartOf(String str) {return 0;}
  }
  
  
  /**
   * Zerlegt die Datei, die durch url bestimmt wird in {@link Token}s. 
   * @return die Liste der identifizierten Tokens, abgeschlossen durch
   * mindestens 7 {@link EndToken}s.
   * @throws IOException falls beim Zugriff auf die Daten von url etwas
   * schief geht.
   * @throws SyntaxErrorException falls eine Zeichenfolge nicht als Token
   * identifiziert werden kann.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static List tokenize(URL url) throws IOException, SyntaxErrorException
  {
    InputStream ins = url.openStream();
    List tokens = new Vector();
    BufferedReader in = new BufferedReader(new InputStreamReader(ins));
    String line;
    
    int lineNo = 0;
    Pattern whitespace = Pattern.compile("^(\\p{Space}|,|;)+");
    while(null != (line = in.readLine()))
    {
      ++lineNo;
      int pos = 0;
      
      while (line.length() > 0)
      {
        Matcher wsm = whitespace.matcher(line);
        if (wsm.find())
        {
          pos += wsm.end();
          line = line.substring(wsm.end());
          if (line.length() == 0) continue;
        }
        
        int tokenLength;
        if (0 != (tokenLength = KeyToken.atStartOf(line)))
        {
          tokens.add(new KeyToken(line,url,lineNo,pos+1));
        }
        else
          if (0 != (tokenLength = StringToken.atStartOf(line)))
          {
            tokens.add(new StringToken(line,url, lineNo,pos+1));
          }
          else
            if (0 != (tokenLength = OpenParenToken.atStartOf(line)))
            {
              tokens.add(new OpenParenToken(url,lineNo,pos+1));
            }
            else
              if (0 != (tokenLength = CloseParenToken.atStartOf(line)))
              {
                tokens.add(new CloseParenToken(url,lineNo,pos+1));
              }
              else
                if (0 != (tokenLength = IncludeToken.atStartOf(line)))
                {
                  tokens.add(new IncludeToken(url,lineNo,pos+1));
                }
                else
                  if (0 != (tokenLength = LineCommentToken.atStartOf(line)))
                  {
                    //LineCommentTokens werden nicht in tokens eingef�gt, weil
                    //der Parser im Fall von 2er Paaren wie KEY STRING nicht in
                    //der Lage ist �ber Kommentare hinwegzulesen. Anstatt ihm das
                    //Einzubauen ist es einfacher, Kommentare einfach wegzuschmeissen.
                    //tokens.add(new LineCommentToken(line,url,lineNo,pos+1));
                  }
              else
              {
                throw new SyntaxErrorException(url+": Syntaxfehler in Zeile "+lineNo+" bei Zeichen "+(pos+1));
              }
        
        pos += tokenLength;
        line = line.substring(tokenLength);
      }
    }
    
      // add a couple EndTokens so that users don't have to worry about
      // checking if there's enough input remaining
    ++lineNo;
    for (int i = 0; i < 7; ++i) tokens.add(new EndToken(url,lineNo,0));
    
    return tokens;
  }
  
/**
 * Liefert eine textuelle Baumdarstellung von conf. Jeder Zeile wird
 * childPrefix vorangestellt.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
  public static String treeDump(ConfigThingy conf, String childPrefix)
  {
    StringBuffer buf = new StringBuffer();
    buf.append("\""+conf.name+"\"\n");
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy child = (ConfigThingy)iter.next();
      buf.append(childPrefix+"|\n"+childPrefix+"+--");
      char ch = iter.hasNext()?'|':' ';
      buf.append(treeDump(child, childPrefix+ch+"  "));
    }
    return buf.toString();
  }
  
  
  /**
   * Testet die Funktionsweise von ConfigThingy
   * @param args url [ get1 get2 ... ], dabei ist url die URL einer zu lesenden 
   *              Config-Datei. Das Programm gibt den sich daraus ergebenden
   *              ConfigThingy-Baum aus. Optional k�nnen noch weitere Argumente
   *              �bergeben werden, die einen get-Pfad angeben 
   *              (configthingy.get(get1).get(get2)...) dessen Ergebnis 
   *              ausgegeben wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void main(String[] args) throws IOException
  {
    if (args.length < 1)
    {
      System.out.println("USAGE: <url> [ <get1> <get2> ... ]");
      System.exit(0);
    }
    
    File cwd = new File(System.getProperty("user.dir"));

    args[0] = args[0].replaceAll("\\\\","/");
    ConfigThingy conf = new ConfigThingy(args[0], new URL(cwd.toURL(),args[0]));
    
    System.out.println("Dies ist der Ergebnisbaum: \n\n"+treeDump(conf, ""));
    
    if (args.length > 1)
    {
      System.out.println("\n\n");
      
      ConfigThingy byChild = null;
      
      String getstr = "";
      String getbychildstr = "";
      for (int i = 1; i < args.length; ++i)
      {
        if (i + 1 == args.length) 
        {
          byChild = conf.getByChild(args[i]);
          getbychildstr = getstr + ".getByChild(\""+args[i]+"\")"; 
        }
        getstr = getstr + ".get(\""+args[i]+"\")";
        conf = conf.get(args[i]);
        if (conf == null) break;
      }
      
      if (conf == null)
      {
        System.out.println(getstr+" == null!");
      }
      else
      {
        System.out.println("Dies ist der Teilbaum f�r "+getstr+": \n\n");
        System.out.println(treeDump(conf, ""));
        System.out.println("\n\nDies ist der toString() Wert des Teilbaums:\n"+"\""+conf.toString()+"\"");
      }
      
      System.out.print("\n\n");
      
      if (byChild == null)
      {
        System.out.println(getbychildstr+" == null!");
      }
      else
      {
        System.out.println("Dies ist der Teilbaum f�r "+getbychildstr+": \n\n");
        System.out.println(treeDump(byChild, ""));
        System.out.println("\n\nDies ist der toString() Wert des Teilbaums:\n"+"\""+byChild.toString()+"\"");
      }
    }
  }
}
