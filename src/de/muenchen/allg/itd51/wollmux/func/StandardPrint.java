/*
 * Copyright (c) 2008 Landeshauptstadt M�nchen
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL), version 1.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see 
 * http://ec.europa.eu/idabc/en/document/7330
 *
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @author Christoph Lutz (D-III-ITD-5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.func;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.PrintModels.InternalPrintModel;
import de.muenchen.allg.itd51.wollmux.dialog.SachleitendeVerfuegungenDruckdialog.VerfuegungspunktInfo;

public class StandardPrint
{

  /**
   * Unter diesem Key werden in den Properties eines XPrintModels die Einstellungen
   * zu den Sachleitenden Verf�gungen als Objekte vom Typ List<VerfuegungspunktInfo>
   * abgelegt, die von der Druckfunktion SachleitendeVerfuegungOutput wieder
   * ausgelesen werden.
   */
  public static final String PROP_SLV_SETTINGS = "SLV_Settings";

  /**
   * GUI der Sachleitenden Verf�gungen: Diese Komfortdruckfunktion erzeugt die GUI,
   * mit deren Hilfe die Steuerdaten (in Form der Properties "SLV_SettingsFromGUI")
   * f�r den Druck der Sachleitenden Verf�gungen festgelegt werden k�nnen und leitet
   * den Druck mittels pmod.printWithProps() an die n�chste Druckfunktion der
   * Aufrufkette (kann z.B. Seriendruck sein) weiter. Damit die SLVs letztendlich
   * auch wirklich in den ben�tigten Ausfertigungen gedruckt werden, l�dt die
   * Druckfunktion noch das Ausgabemodul "SachleitendeVerfuegungOutput" zur Liste der
   * auszuf�hrenden Druckfunktionen hinzu.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void sachleitendeVerfuegung(XPrintModel pmod)
  {
    // Druckfunktion SachleitendeVerfuegungOutput f�r SLV-Ausgabe hinzuladen:
    try
    {
      pmod.usePrintFunction("SachleitendeVerfuegungOutput");
    }
    catch (NoSuchMethodException e)
    {
      String method = "sachleitendeVerfuegungOutput";
      int order = 150;
      PrintFunction func = getInternalPrintFunction(method, order);
      if (pmod instanceof InternalPrintModel)
      {
        Logger.debug(L.m(
          "Verwende interne Druckfunktion '%1' mit ORDER-Wert '%2' als Fallback.",
          method, order));
        ((InternalPrintModel) pmod).useInternalPrintFunction(func);
      }
    }

    List<VerfuegungspunktInfo> settings =
      SachleitendeVerfuegung.callPrintDialog(pmod.getTextDocument());
    if (settings != null)
    {
      try
      {
        pmod.setPropertyValue(PROP_SLV_SETTINGS, settings);
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
        pmod.cancel();
        return;
      }
      pmod.printWithProps();
    }
  }

  /**
   * Ausgabemodul der Sachleitenden Verf�gungen: Diese Komfortdruckfunktion druckt
   * die Verf�gungspunkte aus, die �ber die GUI ausgew�hlt wurden. Dabei wird f�r
   * jeden Verf�gungspunkt die Methode printVerfuegungspunkt(...) ausgef�hrt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  @SuppressWarnings("unchecked")
  public static void sachleitendeVerfuegungOutput(XPrintModel pmod)
  {
    List<VerfuegungspunktInfo> settings = new ArrayList<VerfuegungspunktInfo>();
    try
    {
      settings =
        (List<VerfuegungspunktInfo>) pmod.getPropertyValue(PROP_SLV_SETTINGS);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }

    for (VerfuegungspunktInfo v : settings)
    {
      if (pmod.isCanceled()) return;
      if (v.copyCount > 0)
      {
        SachleitendeVerfuegung.printVerfuegungspunkt(pmod, v.verfPunktNr, v.isDraft,
          v.isOriginal, v.copyCount);
      }
    }
  }

  /**
   * mit dieser Komfortdruckfuntion habe ich getestet, ob die Parameter�bergabe bei
   * Druckfunktionen (arg als ConfigThingy) funktioniert und ob pmod sich �ber die
   * UNO-Mechanismen korrekt inspizieren l�sst.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void myTestPrintFunction(XPrintModel pmod, Object arg)
  {
    ConfigThingy conf = new ConfigThingy("ARG");
    if (arg != null && arg instanceof ConfigThingy) conf = (ConfigThingy) arg;

    new UnoService(pmod).msgboxFeatures();

    pmod.setFormValue("EmpfaengerZeile1", conf.stringRepresentation());
    pmod.print((short) 1);

    new UnoService(pmod).msgboxFeatures();
  }

  /**
   * Druckt das zu pmod geh�rende Dokument f�r jeden Datensatz der aktuell �ber
   * Bearbeiten/Datenbank austauschen eingestellten Tabelle einmal aus.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static void mailMergeWithoutSelection(XPrintModel pmod)
  {
    MailMerge.mailMerge(pmod, false);
  }

  /**
   * Druckt das zu pmod geh�rende Dokument f�r die Datens�tze, die der Benutzer in
   * einem Dialog ausw�hlt. F�r die Anzeige der Datens�tze im Dialog wird die Spalte
   * "WollMuxDescription" verwendet. Falls die Spalte "WollMuxSelected" vorhanden ist
   * und "1", "ja" oder "true" enth�lt, so ist der entsprechende Datensatz in der
   * Auswahlliste bereits vorselektiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static void mailMergeWithSelection(XPrintModel pmod)
  {
    MailMerge.mailMerge(pmod, true);
  }

  /**
   * Startet den ultimativen MailMerge f�r pmod.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void superMailMerge(XPrintModel pmod)
  {
    MailMerge.superMailMerge(pmod);
  }

  /**
   * Erzeugt eine interne Druckfunktion, die auf die in dieser Klasse definierte
   * Methode mit dem Namen functionName verweist und den Order-Wert order besitzt.
   * 
   * @param functionName
   *          Enth�lt den Namen einer in dieser Klasse definierten
   *          Standard-Druckfunktion
   * @param order
   *          enth�lt den Order-Wert, der f�r die bestimmung der Reihenfolge der
   *          Ausf�hrung ben�tigt wird.
   * @return die neue Druckfunktion oder null, wenn die Funktion nicht definiert ist.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static PrintFunction getInternalPrintFunction(final String functionName,
      int order)
  {
    ConfigThingy conf = new ConfigThingy("EXTERN");
    ConfigThingy url = new ConfigThingy("URL");
    url.addChild(new ConfigThingy("java:" + StandardPrint.class.getName() + "."
      + functionName));
    conf.addChild(url);
    try
    {
      return new PrintFunction(conf, functionName, order);
    }
    catch (ConfigurationErrorException e)
    {
      Logger.error(L.m("Interne Druckfunktion '%1' nicht definiert!", functionName),
        e);
      return null;
    }
  }

  /**
   * H�ngt das zu pmod geh�rige TextDocument an das im Property
   * PrintIntoFile_OutputDocument gespeicherte XTextDocument an. Falls noch kein
   * solches Property existiert, wird ein leeres Dokument angelegt.
   * 
   * @throws Exception
   *           falls was schief geht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void printIntoFile(XPrintModel pmod) throws Exception
  {
    boolean firstAppend = true;
    XTextDocument outputDoc = null;
    try
    {
      outputDoc =
        UNO.XTextDocument(pmod.getPropertyValue("PrintIntoFile_OutputDocument"));
      firstAppend = false;
    }
    catch (UnknownPropertyException e)
    {
      outputDoc =
        UNO.XTextDocument(UNO.loadComponentFromURL("private:factory/swriter", true,
          true));
      pmod.setPropertyValue("PrintIntoFile_OutputDocument", outputDoc);
    }

    PrintIntoFile.appendToFile(outputDoc, pmod.getTextDocument(), firstAppend);
  }

}
