package de.muenchen.allg.itd51.wollmux.func;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.PrintModels.InternalPrintModel;
import de.muenchen.allg.itd51.wollmux.PrintModels.PrintModelProps;

public class StandardPrint
{  
  
  /**
   * GUI der Sachleitenden Verf�gungen: Diese Komfortdruckfunktion erzeugt die
   * GUI, mit deren Hilfe die Steuerdaten (in Form der Properties "SLV_*") f�r
   * den Druck der Sachleitenden Verf�gungen festgelegt werden k�nnen und leitet
   * den Druck mittels pmod.printWithProps() an die n�chste Druckfunktion der
   * Aufrufkette (kann z.B. Seriendruck sein) weiter. Damit die SLVs
   * letztendlich auch wirklich in den ben�tigten Ausfertigungen gedruckt
   * werden, l�dt die Druckfunktion noch das Ausgabemodul
   * "SachleitendeVerfuegungOutput" zur Liste der auszuf�hrenden Druckfunktionen
   * hinzu.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void sachleitendeVerfuegung(XPrintModel pmod)
  {
    // Druckfunktion SachleitendeVerfuegungOutput f�r SLV-Ausgabe hinzuladen:
    if (!pmod.usePrintFunction("SachleitendeVerfuegungOutput"))
    {
      String method = "sachleitendeVerfuegungOutput";
      int order = 150;
      PrintFunction func = getInternalPrintFunction(method, order);
      if (pmod instanceof InternalPrintModel) {
        Logger.debug("Verwende interne Druckfunktion '" + method + "' mit ORDER-Wert '" + order + "' als Fallback.");
        ((InternalPrintModel) pmod).useInternalPrintFunction(func);
      }
    }

    SachleitendeVerfuegung.showPrintDialog(pmod);
  }

  /**
   * Ausgabemodul der Sachleitenden Verf�gungen: Diese Komfortdruckfunktion
   * druckt die Verf�gungspunkte aus, die in der Property "SLV_verfPunkte"
   * angegeben sind; dabei werden auch die anderen Steuerdaten in Form der
   * Properties "SLV_*" entsprechend f�r jeden Verf�gungspunkt ausgewertet und
   * der Druck �ber pmod.printWithProps() gestartet.
   * 
   * So ist es z.B. m�glich, die Steuerdaten f�r den SLV-Druck in einem
   * vorgeschalteten Dialog zu erzeugen und mit dieser Komfortdruckfunktion die
   * Ausfertigungen drucken zu lassen. Es ist aber auch m�glich, die Steuerdaten
   * in einem nicht interaktiven Druckermodul zu belegen.
   * 
   * Diese Komfortdruckfunktion �berschreibt die Properties "CopyCount",
   * "PageRangeType" und "PageRangeValue"
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void sachleitendeVerfuegungOutput(XPrintModel pmod)
  {
    try
    {
      Object[] verfPunkte = (Object[]) pmod.getPropertyValue(PrintModelProps.PROP_SLV_VERF_PUNKTE);
      Object[] isDraftFlags = (Object[]) pmod.getPropertyValue(PrintModelProps.PROP_SLV_IS_DRAFT_FLAGS);
      Object[] isOriginalFlags = (Object[]) pmod.getPropertyValue(PrintModelProps.PROP_SLV_IS_ORIGINAL_FLAGS);
      Object[] pageRangeTypes = (Object[]) pmod.getPropertyValue(PrintModelProps.PROP_SLV_PAGE_RANGE_TYPES);
      Object[] pageRangeValues = (Object[]) pmod.getPropertyValue(PrintModelProps.PROP_SLV_PAGE_RANGE_VALUES);
      Object[] copyCounts = (Object[]) pmod.getPropertyValue(PrintModelProps.PROP_SLV_COPY_COUNTS);

      for (int i = 0; i < verfPunkte.length; i++)
      {
        short copyCount = AnyConverter.toShort(copyCounts[i]);
        if (copyCount > 0)
        {
          pmod.setPropertyValue(PrintModelProps.PROP_PAGE_RANGE_TYPE, pageRangeTypes[i]);
          pmod.setPropertyValue(PrintModelProps.PROP_PAGE_RANGE_VALUE, pageRangeValues[i]);
          pmod.setPropertyValue(PrintModelProps.PROP_COPY_COUNT, copyCounts[i]);

          short verfPunkt = AnyConverter.toShort(verfPunkte[i]);
          boolean isDraft = AnyConverter.toBoolean(isDraftFlags[i]);
          boolean isOriginal = AnyConverter.toBoolean(isOriginalFlags[i]);

          SachleitendeVerfuegung.printVerfuegungspunkt(pmod, verfPunkt, isDraft, isOriginal);
        }
      }
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * mit dieser Komfortdruckfuntion habe ich getestet, ob die Parameter�bergabe
   * bei Druckfunktionen (arg als ConfigThingy) funktioniert und ob pmod sich
   * �ber die UNO-Mechanismen korrekt inspizieren l�sst.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void myTestPrintFunction(XPrintModel pmod, Object arg)
  {
    ConfigThingy conf = new ConfigThingy("ARG");
    if(arg != null && arg instanceof ConfigThingy) conf = (ConfigThingy) arg;
    
    new UnoService(pmod).msgboxFeatures();

    pmod.setFormValue("EmpfaengerZeile1", conf.stringRepresentation());
    pmod.print((short)1);
    
    new UnoService(pmod).msgboxFeatures();
  }

  /**
   * Druckt das zu pmod geh�rende Dokument f�r jeden Datensatz der aktuell �ber
   * Bearbeiten/Datenbank austauschen eingestellten Tabelle einmal aus.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public static void mailMergeWithoutSelection(XPrintModel pmod)
  {
    MailMerge.mailMerge(pmod, false);
  }

  /**
   * Druckt das zu pmod geh�rende Dokument f�r die Datens�tze, die der Benutzer in einem Dialog
   * ausw�hlt. F�r die Anzeige der Datens�tze im Dialog wird die Spalte "WollMuxDescription"
   * verwendet. Falls die Spalte "WollMuxSelected" vorhanden ist und "1", "ja" oder "true"
   * enth�lt, so ist der entsprechende Datensatz in der Auswahlliste bereits vorselektiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public static void mailMergeWithSelection(XPrintModel pmod)
  {
    MailMerge.mailMerge(pmod, true);
  }
  
  /**
   * Startet den ultimativen MailMerge f�r pmod.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void superMailMerge(XPrintModel pmod)
  {
    MailMerge.superMailMerge(pmod);
  }

  /**
   * Erzeugt eine interne Druckfunktion, die auf die in dieser Klasse definierte
   * Methode mit dem Namen functionName verweist und den Order-Wert order
   * besitzt.
   * 
   * @param functionName
   *          Enth�lt den Namen einer in dieser Klasse definierten
   *          Standard-Druckfunktion
   * @param order
   *          enth�lt den Order-Wert, der f�r die bestimmung der Reihenfolge der
   *          Ausf�hrung ben�tigt wird.
   * @return die neue Druckfunktion oder null, wenn die Funktion nicht definiert
   *         ist.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static PrintFunction getInternalPrintFunction(
      final String functionName, int order)
  {
    ConfigThingy conf = new ConfigThingy("EXTERN");
    ConfigThingy url = new ConfigThingy("URL");
    url.addChild(new ConfigThingy("java:" + StandardPrint.class.getName() + "." + functionName));
    conf.addChild(url);
    try
    {
      return new PrintFunction(conf, functionName, order);
    }
    catch (ConfigurationErrorException e)
    {
      Logger.error("Interne Druckfunktion '" + functionName + "' nicht definiert!", e);
      return null;
    }
  }

  /**
   * H�ngt das zu pmod geh�rige TextDocument an das im Property
   * PrintIntoFile_OutputDocument gespeicherte XTextDocument an. Falls
   * noch kein solches Property existiert, wird ein leeres Dokument angelegt.
   * @throws Exception falls was schief geht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void printIntoFile(XPrintModel pmod) throws Exception
  {
    boolean firstAppend = true;
    XTextDocument outputDoc = null;
    try
    {
      outputDoc = UNO.XTextDocument(pmod.getPropertyValue("PrintIntoFile_OutputDocument"));
      firstAppend = false;
    }
    catch (UnknownPropertyException e)
    {
      outputDoc = UNO.XTextDocument(UNO.loadComponentFromURL("private:factory/swriter", true, true));
      pmod.setPropertyValue("PrintIntoFile_OutputDocument", outputDoc);
    }
    
    PrintIntoFile.appendToFile(outputDoc, pmod.getTextDocument(), firstAppend);
  }
  
  
}
