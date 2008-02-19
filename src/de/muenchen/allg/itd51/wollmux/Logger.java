//TODO L.m()
/*
 * Dateiname: Logger.java
 * Projekt  : WollMux
 * Funktion : Logging-Mechanismus zum Schreiben von Nachrichten auf eine PrintStream.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 13.10.2005 | LUT | Erstellung
 * 14.10.2005 | BNK | Kommentar korrigiert: Standard ist LOG nicht NONE
 *                  | System.err als Standardausgabestrom
 * 14.10.2005 | LUT | critical(*) --> error(*)
 *                    + Anzeige des Datums bei allen Meldungen.
 * 27.10.2005 | BNK | Leerzeile nach jeder Logmeldung                  
 * 31.10.2005 | BNK | +error(msg, e)
 *                  | "critical" -> "error"
 * 02.11.2005 | BNK | LOG aus Default-Modus
 * 24.11.2005 | BNK | In init() das Logfile nicht l�schen.
 * 05.12.2005 | BNK | line.separator statt \n
 * 06.12.2005 | BNK | bessere Separatoren, kein Test mehr in init, ob Logfile schreibbar
 * 20.04.2006 | BNK | bessere Datum/Zeitangabe, Angabe des Aufrufers
 * 24.04.2006 | BNK | korrekte Monatsangabe.
 * 15.05.2006 | BNK | Cause ausgeben in printException()
 * 16.05.2006 | BNK | println() und printException() vereinheitlicht
 * 30.05.2006 | BNK | bei init(PrintStream,...) den file zur�cksetzen, damit
 *                  | die Zuweisung auch wirksam wird.
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;

/**
 * <p>
 * Der Logger ist ein simpler Logging Mechanismus, der im Programmablauf
 * auftretende Nachrichten verschiedener Priorit�ten entgegennimmt und die
 * Nachrichten entsprechend der Einstellung des Logging-Modus auf einem
 * PrintStream ausgibt (Standardeinstellung: System.err). Die
 * Logging-Nachrichten werden �ber unterschiedliche Methodenaufrufe entsprechend
 * der Logging-Priorit�t abgesetzt. Folgende Methoden stehen daf�r zur
 * Verf�gung: error(), log(), debug(), debug2()
 * </p>
 * <p>
 * Der Logging-Modus kann �ber die init()-Methode initialisiert werden. Er
 * beschreibt, welche Nachrichten aus den Priorit�tsstufen angezeigt werden und
 * welche nicht. Jeder Logging Modus zeigt die Nachrichten seiner Priorit�t und
 * die Nachrichten der h�heren Priorit�tsstufen an. Standardm�ssig ist der Modus
 * Logging.LOG voreingestellt.
 * </p>
 */
public class Logger
{

  /**
   * Der PrintStream, auf den die Nachrichten geschrieben werden.
   */
  private static PrintStream outputStream = System.err;

  /**
   * optional: Datei, aus der der PrintStream erzeugt wird.
   */
  private static File file = null;

  /**
   * Im Logging-Modus <code>NONE</code> werden keine Nachrichten ausgegeben.
   */
  public static final int NONE = 0;

  /**
   * Der Logging-Modus <code>ERROR</code> zeigt Nachrichten der h�chsten
   * Priorit�tsstufe "ERROR" an. ERROR enth�lt Nachrichten, die den
   * Programmablauf beeinflussen - z.B. Fehlermeldungen und Exceptions.
   */
  public static final int ERROR = 1;

  /**
   * Der Logging-Modus <code>LOG</code> ist der Standard Modus. Er zeigt
   * Nachrichten und wichtige Programminformationen an, die im t�glichen Einsatz
   * interessant sind. Dieser Modus ist die Defaulteinstellung.
   */
  public static final int LOG = 3;

  /**
   * Der Logging-Modus <code>DEBUG</code> wird genutzt, um detaillierte
   * Informationen �ber den Programmablauf auszugeben. Er ist vor allem f�r
   * DEBUG-Zwecke geeignet.
   */
  public static final int DEBUG = 5;

  /**
   * Der Logging-Modus <code>ALL</code> gibt uneingeschr�nkt alle Nachrichten
   * aus. Er enth�lt auch Nachrichten der Priorit�t debug2, die sehr
   * detaillierte Informationen ausgibt, die selbst f�r normale DEBUG-Zwecke zu
   * genau sind.
   */
  public static final int ALL = 7;

  /**
   * Das Feld <code>mode</code> enth�lt den aktuellen Logging-Mode
   */
  private static int mode = LOG;

  /**
   * �ber die Methode init wird der Logger mit einem PrintStream und einem
   * Logging-Modus initialisiert. Ohne diese Methode schreibt der Logger auf
   * System.err im Modus LOG.
   * 
   * @param loggingMode
   *          Der neue Logging-Modus kann �ber die statischen Felder
   *          Logger.MODUS (z. B. Logger.DEBUG) angegeben werden.
   */
  public static void init(PrintStream outputPrintStream, int loggingMode)
  {
    outputStream = outputPrintStream;
    file = null; //evtl. vorher erfolgte Zuweisung aufheben, damit outputStream auch wirklich verwendet wird
    mode = loggingMode;
    Logger.debug2("========================== Logger::init(): LoggingMode = " + mode+" ========================");
  }

  /**
   * �ber die Methode init wird der Logger mit einer Ausgabedatei und einem
   * Logging-Modus initialisiert. Ohne diese Methode schreibt der Logger auf
   * System.err im Modus LOG.
   * 
   * @param outputFile
   *          Datei, in die die Ausgaben geschrieben werden.
   * @param loggingMode
   *          Der neue Logging-Modus kann �ber die statischen Felder
   *          Logger.MODUS (z. B. Logger.DEBUG) angegeben werden.
   * @throws FileNotFoundException
   */
  public static void init(File outputFile, int loggingMode)
  {
    file = outputFile;
    mode = loggingMode;
    Logger.debug2("========================== Logger::init(): LoggingMode = " + mode+" ========================");
  }

  /**
   * �ber die Methode init wird der Logger in dem Logging-Modus loggingMode
   * initialisiert. Ohne diese Methode schreibt der Logger auf System.err im
   * Modus LOG.
   * 
   * @param loggingMode
   *          Der neue Logging-Modus kann �ber die statischen Felder
   *          Logger.MODUS (z. B. Logger.DEBUG) angegeben werden.
   */
  public static void init(int loggingMode)
  {
    mode = loggingMode;
    Logger.debug2("========================== Logger::init(): LoggingMode = " + mode+" ========================");
  }

  /**
   * �ber die Methode init wird der Logger in dem Logging-Modus loggingMode
   * initialisiert, der in Form eines den obigen Konstanten-Namen
   * �bereinstimmenden Strings vorliegt. Ohne diese Methode schreibt der Logger
   * auf System.err im Modus LOG.
   * 
   * @param loggingMode
   *          Der neue Logging-Modus kann �ber die statischen Felder
   *          Logger.MODUS (z. B. Logger.DEBUG) angegeben werden.
   */
  public static void init(String loggingMode)
  {
    if (loggingMode.compareToIgnoreCase("NONE") == 0) init(NONE);
    if (loggingMode.compareToIgnoreCase("ERROR") == 0) init(ERROR);
    if (loggingMode.compareToIgnoreCase("LOG") == 0) init(LOG);
    if (loggingMode.compareToIgnoreCase("DEBUG") == 0) init(DEBUG);
    if (loggingMode.compareToIgnoreCase("ALL") == 0) init(ALL);
  }

  /**
   * Nachricht der h�chsten Priorit�t "error" absetzen. Als "error" sind nur
   * Ereignisse einzustufen, die den Programmablauf unvorhergesehen ver�ndern
   * oder die weitere Ausf�hrung unm�glich machen.
   * 
   * @param msg
   *          Die Logging-Nachricht
   */
  public static void error(String msg)
  {
    if (mode >= ERROR) printInfo("ERROR("+getCaller(2)+"): ", msg, null);
  }

  /**
   * Wie {@link #error(String)}, nur dass statt dem String eine Exception
   * ausgegeben wird.
   * 
   * @param e
   */
  public static void error(Throwable e)
  {
    if (mode >= ERROR) printInfo("ERROR("+getCaller(2)+"): ", null, e);
  }

  /**
   * Wie {@link #error(String)}, nur dass statt dem String eine Exception
   * ausgegeben wird.
   * 
   * @param e
   */
  public static void error(String msg, Exception e)
  {
    if (mode >= ERROR) printInfo("ERROR(" + getCaller(2) + "): ", msg, e);
  }

  /**
   * Nachricht der Priorit�t "log" absetzen. "log" enth�lt alle Nachrichten, die
   * f�r den t�glichen Programmablauf beim Endanwender oder zur Auffindung der
   * g�ngigsten Bedienfehler interessant sind.
   * 
   * @param msg
   *          Die Logging-Nachricht
   */
  public static void log(String msg)
  {
    if (mode >= LOG) printInfo("LOG("+getCaller(2)+"): ", msg, null);
  }

  /**
   * Wie {@link #log(String)}, nur dass statt dem String eine Exception
   * ausgegeben wird.
   * 
   * @param e
   */
  public static void log(Throwable e)
  {
    if (mode >= LOG) printInfo("LOG("+getCaller(2)+"): ", null, e);
  }

  /**
   * Nachricht der Priorit�t "debug" absetzen. Die debug-Priorit�t dient zu
   * debugging Zwecken. Sie enth�lt Informationen, die f�r Programmentwickler
   * interessant sind.
   * 
   * @param msg
   *          Die Logging-Nachricht
   */
  public static void debug(String msg)
  {
    if (mode >= DEBUG) printInfo("DEBUG("+getCaller(2)+"): ", msg, null);
  }

  /**
   * Wie {@link #debug(String)}, nur dass statt dem String eine Exception
   * ausgegeben wird.
   * 
   * @param e
   */
  public static void debug(Throwable e)
  {
    if (mode >= DEBUG) printInfo("DEBUG("+getCaller(2)+"): ", null, e);
  }

  /**
   * Nachricht der geringsten Priorit�t "debug2" absetzen. Das sind Meldungen,
   * die im Normalfall selbst f�r debugging-Zwecke zu detailliert sind.
   * Beispielsweise Logging-Meldungen von privaten Unterfunktionen, die die
   * Ausgabe nur unn�tig un�bersichtlich machen, aber nicht zum schnellen
   * Auffinden von Standard-Fehlern geeignet sind. "debug2" ist geeignet, um
   * ganz spezielle Fehler ausfindig zu machen.
   * 
   * @param msg
   *          Die Logging-Nachricht.
   */
  public static void debug2(String msg)
  {
    if (mode >= ALL) printInfo("DEBUG2("+getCaller(2)+"): ", msg, null);
  }

  /**
   * Wie {@link #debug2(String)}, nur dass statt dem String eine Exception
   * ausgegeben wird.
   * 
   * @param e
   */
  public static void debug2(Throwable e)
  {
    if (mode >= ALL) printInfo("DEBUG2("+getCaller(2)+"): ", null, e);
  }

  /**
   * Gibt msg gefolgt vom Stacktrace von e aus, wobei jeder Zeile prefix vorangestellt wird.
   */
  private static void printInfo(String prefix, String msg, Throwable e)
  {
    // Ausgabestream oeffnen bzw. festlegen:
    PrintStream out;
    FileOutputStream fileOut = null;
    if (file != null)
      try
      {
        fileOut = new FileOutputStream(file, true);
        out = new PrintStream(fileOut);
      }
      catch (FileNotFoundException x)
      {
        out = Logger.outputStream;
      }
    else
    {
      out = Logger.outputStream;
    }

    // Zeit und Datum holen und aufbereiten
    Calendar now = Calendar.getInstance();
    int day = now.get(Calendar.DAY_OF_MONTH);
    int month = now.get(Calendar.MONTH) + 1;
    int hour = now.get(Calendar.HOUR_OF_DAY);
    int minute = now.get(Calendar.MINUTE);
    String dayStr = ""+day;
    String monthStr = ""+month;
    String hourStr = ""+hour;
    String minuteStr = ""+minute;
    if (day < 10) dayStr = "0"+dayStr;
    if (month < 10) monthStr = "0"+monthStr;
    if (hour < 10) hourStr = "0"+hourStr;
    if (minute < 10) minuteStr = "0"+minuteStr;
    prefix = ""+now.get(Calendar.YEAR)+"-"+monthStr+"-"+dayStr+" "+hourStr+":"+minuteStr+" "+prefix;

    // Ausgabe schreiben:
    if (msg != null)
    {
      out.print(prefix);
      out.println(msg);
    }
    
    while (e != null)
    {
      out.print(prefix);
      out.println(e.toString());
      StackTraceElement[] se = e.getStackTrace();
      for (int i = 0; i < se.length; i++)
      {
        out.print(prefix);
        out.println(se[i].toString());
      }
      
      e = e.getCause();
      if (e != null) 
      {
        out.print(prefix);
        out.println("-------- CAUSED BY ------");
      }
    }while(e != null);
    out.println();
    out.flush();

    // Ein File wird nach der Ausgabe geschlossen:
    if (fileOut != null)
    {
      try
      {
        out.close();
        fileOut.close();
      }
      catch (IOException e1)
      {
      }
    }
  }
  
  /**
   * Liefert Datei (ohne java Extension) und Zeilennummer des Elements level des Stacks.
   * Level 1 ist dabei die Funktion, die getCaller() aufruft.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static String getCaller(int level)
  {
    try{
      Throwable grosserWurf = new Throwable();
      grosserWurf.fillInStackTrace();
      StackTraceElement[] dickTracy = grosserWurf.getStackTrace();
      return dickTracy[level].getFileName().replaceAll("\\.java","")+":"+dickTracy[level].getLineNumber();
    } catch(Exception x){return "Unknown:???";}
  }
}
