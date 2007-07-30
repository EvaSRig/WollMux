/*
 * Dateiname: TestHandler.java
 * Projekt  : WollMux
 * Funktion : Enth�lt die DispatchHandler f�r alle dispatch-Urls, die
 *            mit "wollmux:Test" anfangen
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 07.05.2007 | LUT | Erstellung als TestHandler.java
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Enth�lt die DispatchHandler f�r alle dispatch-Urls, die mit "wollmux:Test"
 * anfangen und f�r den automatisierten Test durch wollmux-qatest ben�tigt
 * werden.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class TestHandler
{

  /**
   * Dieses File enth�lt die Argumente, die einem TestHandler �bergeben werden
   * sollen und vor dem Aufruf des Teshandlers �ber das testtool geschrieben
   * wurden.
   */
  public static File WOLLMUX_QATEST_ARGS_FILE = new File(
      "/tmp/wollmux_qatest.args");

  /**
   * Bearbeitet den Test, der im Argument arg spezifiziert ist und im
   * TextDocumentModel model ausgef�hrt werden soll.
   * 
   * @param model
   * @param arg
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void doTest(TextDocumentModel model, String arg)
  {
    String[] args = arg.split("#");
    String cmd = args[0];

    /** ************************************************************** */
    if (cmd.equalsIgnoreCase("AlleVerfuegungspunkteDrucken"))
    {
      short count = (short) SachleitendeVerfuegung
          .countVerfuegungspunkte(model.doc);
      for (short i = 1; i <= count; i++)
      {
        boolean isDraft = (i == count) ? true : false;
        boolean isOriginal = (i == 1) ? true : false;
        WollMuxEventHandler.handlePrintVerfuegungspunkt(
            model.doc,
            i,
            (short) 1,
            isDraft,
            isOriginal,
            TextDocumentModel.PAGE_RANGE_TYPE_ALL,
            "",
            null);
      }
    }

    /** ************************************************************** */
    if (cmd.equalsIgnoreCase("SchreibeFormularwerte"))
    {
      Map idsAndValues = getWollmuxTestArgs();
      for (Iterator iter = idsAndValues.keySet().iterator(); iter.hasNext();)
      {
        String id = "" + iter.next();
        String value = "" + idsAndValues.get(id);
        WollMuxEventHandler.handleSetFormValueViaPrintModel(
            model.doc,
            id,
            value,
            null);
      }
    }
  }

  /**
   * Liest die Argumente aus der Datei WOLLMUX_QATEST_ARGS_FILE in eine HashMap
   * ein und liefert diese zur�ck. Die Argumente werden in der Datei in Zeilen
   * der Form "<key>,<value>" abgelegt erwartet (key darf dabei kein ","
   * enthalten).
   * 
   * @return
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static HashMap getWollmuxTestArgs()
  {
    HashMap args = new HashMap();
    try
    {
      BufferedReader br = new BufferedReader(new FileReader(
          WOLLMUX_QATEST_ARGS_FILE));

      for (String line = null; (line = br.readLine()) != null;)
      {
        String[] keyValue = line.split(",", 2);
        args.put(keyValue[0], keyValue[1]);
      }
    }
    catch (java.lang.Exception e)
    {
      Logger.error(
          "Argumentdatei f�r wollmux-qatest konnte nicht gelesen werden",
          e);
    }
    return args;
  }
}
