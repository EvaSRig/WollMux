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
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.PrintStream;
import java.util.Date;

/**
 * <p>
 * Der Logger ist ein simpler Logging Mechanismus, der im Programmablauf
 * auftretende Nachrichten verschiedener Priorit�ten entgegennimmt und die
 * Nachrichten entsprechend der Einstellung des Logging-Modus auf einem
 * PrintStream ausgibt (Standardeinstellung: System.err). Die
 * Logging-Nachrichten werden �ber unterschiedliche Methodenaufrufe entsprechend
 * der Logging-Priorit�t abgesetzt. Folgende Methoden stehen daf�r zur
 * Verf�gung: critical(), log(), debug(), debug2()
 * </p>
 * <p>
 * Der Logging-Modus kann �ber die init()-Methode initialisiert werden. Er
 * beschreibt, welche Nachrichten aus den Priorit�tsstufen angezeigt werden und
 * welche nicht. Jeder Logging Modus zeigt die Nachrichten seiner Priorit�t und
 * die Nachrichten der h�heren Priorit�tsstufen an. Standardm�ssig ist der Modus
 * Logging.ERROR voreingestellt.
 * </p>
 */
public class Logger {

	/**
	 * Der PrintStream, auf den die Nachrichten geschrieben werden.
	 */
	private static PrintStream out = System.err;

	/**
	 * Im Logging-Modus <code>NONE</code> werden keine Nachrichten ausgegeben.
	 * Dieser Modus ist die Defaulteinstellung.
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
	 * Nachrichten und wichtige Programminformationen an, die im t�glichen
	 * Einsatz interessant sind.
	 */
	public static final int LOG = 3;

	/**
	 * Der Logging-Modus <code>DEBUG</code> wird genutzt, um detaillierte
	 * Informationen �ber den Programmablauf auszugeben. Er ist vor allem f�r
	 * DEBUG-Zwecke geeignet.
	 */
	public static final int DEBUG = 5;

	/**
	 * Der Logging-Modus <code>ALL</code> gibt uneingeschr�nkt alle
	 * Nachrichten aus. Er enth�lt auch Nachrichten der Priorit�t debug2, die
	 * sehr detaillierte Informationen ausgibt, die selbst f�r normale
	 * DEBUG-Zwecke zu genau sind.
	 */
	public static final int ALL = 7;

	/**
	 * Das Feld <code>mode</code> enth�lt den aktuellen Logging-Mode
	 */
	private static int mode = ERROR;

	/**
	 * �ber die Methode init wird der Logger mit einem PrintStream und einem
	 * Logging-Modus initialisiert. Ohne diese Methode schreibt der Logger auf
	 * System.err im Modus LOG.
	 * 
	 * @param loggingMode
	 *            Der neue Logging-Modus kann �ber die statischen Felder
	 *            Logger.MODUS (z. B. Logger.DEBUG) angegeben werden.
	 */
	public static void init(PrintStream outputPrintStream, int loggingMode) {
		out = outputPrintStream;
		mode = loggingMode;
		Logger.debug2("Logger::init(): LoggingMode = " + mode);
	}

	/**
	 * �ber die Methode init wird der Logger in dem Logging-Modus loggingMode
	 * initialisiert. Ohne diese Methode schreibt der Logger auf System.err im
	 * Modus LOG.
	 * 
	 * @param loggingMode
	 *            Der neue Logging-Modus kann �ber die statischen Felder
	 *            Logger.MODUS (z. B. Logger.DEBUG) angegeben werden.
	 */
	public static void init(int loggingMode) {
		mode = loggingMode;
		Logger.debug2("Logger::init(): LoggingMode = " + mode);
	}

	/**
	 * Nachricht der h�chsten Priorit�t "critical" absetzen. Als "critical" sind
	 * nur Ereignisse einzustufen, die den Programmablauf unvorhergesehen
	 * ver�ndern oder die weitere Ausf�hrung unm�glich machen.
	 * 
	 * @param msg
	 *            Die Logging-Nachricht
	 */
	public static void error(String msg) {
		if (mode >= ERROR)
			println("ERROR: " + msg);
	}

	/**
	 * Wie {@link #critical(String)}, nur dass statt dem String eine Exception
	 * ausgegeben wird.
	 * 
	 * @param e
	 */
	public static void error(Exception e) {
		if (mode >= ERROR)
			printException("ERROR: ", e);
	}

	/**
	 * Nachricht der Priorit�t "log" absetzen. "log" enth�lt alle Nachrichten,
	 * die f�r den t�glichen Programmablauf beim Endanwender oder zur Auffindung
	 * der g�ngigsten Bedienfehler interessant sind.
	 * 
	 * @param msg
	 *            Die Logging-Nachricht
	 */
	public static void log(String msg) {
		if (mode >= LOG)
			println("LOG: " + msg);
	}

	/**
	 * Wie {@link #log(String)}, nur dass statt dem String eine Exception
	 * ausgegeben wird.
	 * 
	 * @param e
	 */
	public static void log(Exception e) {
		if (mode >= LOG)
			printException("LOG: ", e);
	}

	/**
	 * Nachricht der Priorit�t "debug" absetzen. Die debug-Priorit�t dient zu
	 * debugging Zwecken. Sie enth�lt Informationen, die f�r Programmentwickler
	 * interessant sind.
	 * 
	 * @param msg
	 *            Die Logging-Nachricht
	 */
	public static void debug(String msg) {
		if (mode >= DEBUG)
			println("DEBUG: " + msg);
	}

	/**
	 * Wie {@link #debug(String)}, nur dass statt dem String eine Exception
	 * ausgegeben wird.
	 * 
	 * @param e
	 */
	public static void debug(Exception e) {
		if (mode >= DEBUG)
			printException("DEBUG: ", e);
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
	 *            Die Logging-Nachricht.
	 */
	public static void debug2(String msg) {
		if (mode >= ALL)
			println("DEBUG2: " + msg);
	}

	/**
	 * Wie {@link #debug2(String)}, nur dass statt dem String eine Exception
	 * ausgegeben wird.
	 * 
	 * @param e
	 */
	public static void debug2(Exception e) {
		if (mode >= ALL)
			printException("DEBUG2: ", e);
	}

	/**
	 * Gebe den String s auf dem PrintStream aus.
	 * 
	 * @param s
	 */
	private static void println(String s) {
		if (out != null) {
			out.println(new Date() + " " + s);
		}
	}

	/**
	 * Gebe die Exception e samt StackTrace auf dem PrintStream aus.
	 * 
	 * @param s
	 */
	private static void printException(String prefix, Exception e) {
		prefix = new Date() + " " + prefix;
		if (out != null) {
			out.println(prefix + e.toString());
			StackTraceElement[] se = e.getStackTrace();
			for (int i = 0; i < se.length; i++) {
				out.println(prefix + se[i].toString());
			}
		}
	}
}