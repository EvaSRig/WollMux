package de.muenchen.allg.itd51.wollmux.document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.RuntimeException;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.document.FormFieldFactory;
import de.muenchen.allg.itd51.wollmux.core.document.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.core.document.PersistentDataContainer.DataID;
import de.muenchen.allg.itd51.wollmux.core.document.SimulationResults;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.OverrideFragChainException;
import de.muenchen.allg.itd51.wollmux.core.document.VisibilityElement;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.OptionalHighlightColorProvider;
import de.muenchen.allg.itd51.wollmux.core.exceptions.UnavailableException;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.functions.Values.SimpleMap;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.dialog.DialogFactory;
import de.muenchen.allg.itd51.wollmux.dialog.formmodel.FormModel;
import de.muenchen.allg.itd51.wollmux.dialog.formmodel.InvalidFormDescriptorException;
import de.muenchen.allg.itd51.wollmux.dialog.formmodel.SingleDocumentFormModel;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;

public class TextDocumentController
{
  private TextDocumentModel model;
  
  /**
   * Enthält den Kontext für die Funktionsbibliotheken und Dialogbibliotheken dieses
   * Dokuments.
   */
  private HashMap<Object, Object> functionContext;

  /**
   * Enthält die Dialogbibliothek mit den globalen und dokumentlokalen
   * Dialogfunktionen oder null, wenn die Dialogbibliothek noch nicht benötigt wurde.
   */
  private DialogLibrary dialogLib;

  /**
   * Enthält die Funktionsbibliothek mit den globalen und dokumentlokalen Funktionen
   * oder null, wenn die Funktionsbilbiothek noch nicht benötigt wurde.
   */
  private FunctionLibrary functionLib;

  /**
   * Das TextDocumentModel kann in einem Simulationsmodus betrieben werden, in dem
   * Änderungen an Formularelementen (WollMux- und NON-WollMux-Felder) nur simuliert
   * und nicht tatsächlich durchgeführt werden. Benötigt wird dieser Modus für den
   * Seriendruck über den OOo-Seriendruck, bei dem die Änderungen nicht auf dem
   * gerade offenen TextDocument durchgeführt werden, sondern auf einer durch den
   * OOo-Seriendruckmechanismus verwalteten Kopie des Dokuments. Der Simulationsmodus
   * ist dann aktiviert, wenn {@link #simulationResult} != null ist.
   */
  private SimulationResults simulationResult = null;
  
  private DialogLibrary globalDialogs;

  private FunctionLibrary globalFunctions;

  public TextDocumentController(TextDocumentModel model, FunctionLibrary globalFunctions, DialogLibrary globalDialogs)
  {
    this.model = model;
    this.globalFunctions = globalFunctions;
    this.globalDialogs = globalDialogs;
    
    functionContext = new HashMap<Object, Object>();
    
    parseInitialOverrideFragMap(getInitialOverrideFragMap());
  }
  
  public TextDocumentModel getModel()
  {
    return model;
  }
  
  /**
   * Liefert eine Stringrepräsentation des TextDocumentControllers - Derzeit in 
   * der Form 'doc(<title>)'.
   * 
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  synchronized public String toString()
  {
    return "doc('" + getFrameController().getTitle() + "')";
  }
  
  public FrameController getFrameController()
  {
    return new FrameController(model.doc);
  }

  /**
   * Diese Methode fügt die Druckfunktion functionName der Menge der dem Dokument
   * zugeordneten Druckfunktionen hinzu. FunctionName muss dabei ein gültiger
   * Funktionsbezeichner sein.
   * 
   * @param functionName
   *          der Name der Druckfunktion, der ein gültiger Funktionsbezeichner sein
   *          und in einem Abschnitt "Druckfunktionen" in der wollmux.conf definiert
   *          sein muss.
   */
  public synchronized void addPrintFunction(String functionName)
  {
    model.addPrintFunction(functionName);
    model.updateLastTouchedByVersionInfo(WollMuxSingleton.getVersion(), Utils.getOOoVersion());
    
    // Frame veranlassen, die dispatches neu einzulesen - z.B. damit File->Print
    // auch auf die neue Druckfunktion reagiert.
    try
    {
      getFrameController().getFrame().contextChanged();
    }
    catch (java.lang.Exception e)
    {}
  }

  /**
   * Löscht die Druckfunktion functionName aus der Menge der dem Dokument
   * zugeordneten Druckfunktionen.
   * 
   * Wird z.B. in den Sachleitenden Verfügungen verwendet, um auf die ursprünglich
   * gesetzte Druckfunktion zurück zu schalten, wenn keine Verfügungspunkte vorhanden
   * sind.
   * 
   * @param functionName
   *          der Name der Druckfunktion, die aus der Menge gelöscht werden soll.
   */
  public synchronized void removePrintFunction(String functionName)
  {
    model.removePrintFunction(functionName);
    model.updateLastTouchedByVersionInfo(WollMuxSingleton.getVersion(), Utils.getOOoVersion());
    
    // Frame veranlassen, die dispatches neu einzulesen - z.B. damit File->Print
    // auch auf gelöschte Druckfunktion reagiert.
    try
    {
      getFrameController().getFrame().contextChanged();
    }
    catch (java.lang.Exception e)
    {}
  }
  
  /**
   * Liefert die Funktionsbibliothek mit den globalen Funktionen des WollMux und den
   * lokalen Funktionen dieses Dokuments.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public synchronized FunctionLibrary getFunctionLibrary()
  {
    if (functionLib == null)
    {
      ConfigThingy formConf = new ConfigThingy("");
      try
      {
        formConf = model.getFormDescription().get("Formular");
      }
      catch (NodeNotFoundException e)
      {}
      functionLib =
        FunctionFactory.parseFunctions(formConf, getDialogLibrary(), functionContext,
          globalFunctions);
    }
    return functionLib;
  }

  /**
   * Liefert die eine Bibliothek mit den globalen Dialogfunktionen des WollMux und
   * den lokalen Dialogfunktionen dieses Dokuments.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public synchronized DialogLibrary getDialogLibrary()
  {
    if (dialogLib == null)
    {
      ConfigThingy formConf = new ConfigThingy("");
      try
      {
        formConf = model.getFormDescription().get("Formular");
      }
      catch (NodeNotFoundException e)
      {}
      dialogLib =
        DialogFactory.parseFunctionDialogs(formConf,
          globalDialogs, functionContext);
    }
    return dialogLib;
  }
  
  /**
   * Liefert den Kontext mit dem die dokumentlokalen Dokumentfunktionen beim Aufruf
   * von getFunctionLibrary() und getDialogLibrary() erzeugt werden.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public synchronized Map<Object, Object> getFunctionContext()
  {
    return functionContext;
  }

/**
   * Liefert die zum aktuellen Stand gesetzten Formularwerte in einer Map mit ID als
   * Schlüssel. Änderungen an der zurückgelieferten Map zeigen keine Wirkung im
   * TextDocumentModel (da nur eine Kopie der internen Map zurückgegeben wird).
   * 
   * Befindet sich das TextDocumentModel in einem über {@link #startSimulation()}
   * gesetzten Simulationslauf, so werden die im Simulationslauf gesetzten Werte
   * zurück geliefert, die nicht zwangsweise mit den reell gesetzten Werten
   * übereinstimmen müssen.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public synchronized Map<String, String> getFormFieldValues()
  {
    if (simulationResult == null)
      return new HashMap<String, String>(model.getFormFieldValuesMap());
    else
      return new HashMap<String, String>(simulationResult.getFormFieldValues());
  }

  public synchronized ConfigThingy getFormDescription()
  {
    ConfigThingy formDescription = model.getFormDescription();
    applyFormularanpassung(formDescription);
    return formDescription;
  }

  /**
   * Wendet alle matchenden "Formularanpassung"-Abschnitte in der Reihenfolge ihres
   * auftretends in der wollmux,conf auf formularConf an und liefert das Ergebnis
   * zurück. Achtung! Das zurückgelieferte Objekt kann das selbe Objekt sein wie das
   * übergebene.
   * 
   * @param formularConf
   *          ein "WM" Knoten unterhalb dessen sich eine normale Formularbeschreibung
   *          befindet ("Formular" Knoten).
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private ConfigThingy applyFormularanpassung(ConfigThingy formularConf)
  {
    ConfigThingy anpassungen =
        WollMuxFiles.getWollmuxConf().query("Formularanpassung", 1);
    if (anpassungen.count() == 0) return formularConf;

    try
    {
      ConfigThingy formularConfOld = formularConf;
      formularConf = formularConf.getFirstChild(); // Formular-Knoten
      if (!formularConf.getName().equals("Formular")) return formularConfOld;
    }
    catch (NodeNotFoundException x)
    {
      return formularConf;
    }

    process_anpassung: for (ConfigThingy conf : anpassungen)
    {
      ConfigThingy matches = conf.query("Match", 1);
      for (ConfigThingy matchConf : matches)
      {
        for (ConfigThingy subMatchConf : matchConf)
        {
          if (!matches(formularConf, subMatchConf)) continue process_anpassung;
        }
      }

      ConfigThingy formularAnpassung = conf.query("Formular", 1);
      List<ConfigThingy> mergeForms = new ArrayList<ConfigThingy>(2);
      mergeForms.add(formularConf);
      String title = "";
      try
      {
        title = formularConf.get("TITLE", 1).toString();
      }
      catch (Exception x)
      {}
      try
      {
        mergeForms.add(formularAnpassung.getFirstChild());
      }
      catch (NodeNotFoundException x)
      {}
      ConfigThingy buttonAnpassung = conf.query("Buttonanpassung");
      if (buttonAnpassung.count() == 0) buttonAnpassung = null;
      formularConf =
        TextDocumentModel.mergeFormDescriptors(mergeForms, buttonAnpassung, title);
    }

    ConfigThingy formularConfWithWM = new ConfigThingy("WM");
    formularConfWithWM.addChild(formularConf);
    return formularConfWithWM;
  }

  /**
   * Liefert true, wenn der Baum, der durch conf dargestellt wird sich durch
   * Herauslöschen von Knoten in den durch matchConf dargestellten Baum überführen
   * lässt. Herauslöschen bedeutet in diesem Fall bei einem inneren Knoten, dass
   * seine Kinder seinen Platz einnehmen.
   * 
   * Anmerkung: Die derzeitige Implementierung setzt die obige Spezifikation nicht
   * korrekt um, da {@link ConfigThingy#query(String)} nur die Ergebnisse auf einer
   * Ebene zurückliefert. In der Praxis sollten jedoch keine Fälle auftreten wo dies
   * zum Problem wird.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private static boolean matches(ConfigThingy conf, ConfigThingy matchConf)
  {
    ConfigThingy resConf = conf.query(matchConf.getName());
    if (resConf.count() == 0) return false;
    testMatch: for (ConfigThingy subConf : resConf)
    {
      for (ConfigThingy subMatchConf : matchConf)
      {
        if (!matches(subConf, subMatchConf)) continue testMatch;
      }

      return true;
    }
    return false;
  }
  
  public synchronized void updateDocumentCommands()
  {
    model.getDocumentCommands().update();
  }

  /**
   * Übernimmt einen Formularwert ins Model uns ins Dokument.
   * 
   * @param id Name des Formularfelds
   * @param value Inhalt des Formularfelds
   */
  public synchronized void addFormFieldValue(String id, String value)
  {
    setFormFieldValue(id, value);
  }
  
  /**
   * Führt alle Funktionen aus funcs der Reihe nach aus, solange bis eine davon einen
   * nicht-leeren String zurückliefert und interpretiert diesen als Angabe, welche
   * Aktionen für das Dokument auszuführen sind. Derzeit werden nur "noaction" und
   * "allactions" unterstützt. Den Funktionen werden als {@link Values} diverse Daten
   * zur Verfügung gestellt. Derzeit sind dies
   * <ul>
   * <li>"User/<Name>" Werte von Benutzervariablen (vgl.
   * {@link #getUserFieldMaster(String)}</li>
   * </ul>
   * 
   * @return 0 => noaction, Integer.MAX_VALUE => allactions, -1 => WollMux-Default
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  public int evaluateDocumentActions(Iterator<Function> funcs)
  {
    Values values = new MyValues();
    while (funcs.hasNext())
    {
      Function f = funcs.next();
      String res = f.getString(values);
      if (res.length() > 0)
      {
        if (res.equals("noaction")) return 0;
        if (res.equals("allactions")) return Integer.MAX_VALUE;
        Logger.error(L.m(
          "Unbekannter Rückgabewert \"%1\" von Dokumentaktionen-Funktion", res));
      }
    }
    return -1;
  }
  
  /**
   * Fügt an der Stelle r ein neues Textelement vom Typ css.text.TextField.InputUser
   * ein, und verknüpft das Feld so, dass die Trafo trafo verwendet wird, um den
   * angezeigten Feldwert zu berechnen.
   * 
   * @param r
   *          die Textrange, an der das Feld eingefügt werden soll
   * @param trafoName
   *          der Name der zu verwendenden Trafofunktion
   * @param hint
   *          Ein Hinweistext, der im Feld angezeigt werden soll, wenn man mit der
   *          Maus drüber fährt - kann auch null sein, dann wird der Hint nicht
   *          gesetzt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public void addNewInputUserField(XTextRange r, String trafoName,
      String hint)
  {
    model.updateLastTouchedByVersionInfo(WollMuxSingleton.getVersion(), Utils.getOOoVersion());

    try
    {
      ConfigThingy conf = new ConfigThingy("WM");
      conf.add("FUNCTION").add(trafoName);
      String userFieldName = conf.stringRepresentation(false, '\'', false);

      // master erzeugen
      XPropertySet master = getUserFieldMaster(userFieldName);
      if (master == null)
      {
        master =
          UNO.XPropertySet(UNO.XMultiServiceFactory(model.doc).createInstance(
            "com.sun.star.text.FieldMaster.User"));
        UNO.setProperty(master, "Value", Integer.valueOf(0));
        UNO.setProperty(master, "Name", userFieldName);
      }

      // textField erzeugen
      XTextContent f =
        UNO.XTextContent(UNO.XMultiServiceFactory(model.doc).createInstance(
          "com.sun.star.text.TextField.InputUser"));
      UNO.setProperty(f, "Content", userFieldName);
      if (hint != null) UNO.setProperty(f, "Hint", hint);
      r.getText().insertTextContent(r, f, true);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Sammelt alle Formularfelder des Dokuments auf, die nicht von WollMux-Kommandos
   * umgeben sind, jedoch trotzdem vom WollMux verstanden und befüllt werden (derzeit
   * c,s,s,t,textfield,Database-Felder und manche
   * c,s,s,t,textfield,InputUser-Felder).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public synchronized void collectNonWollMuxFormFields()
  {
    model.getIdToTextFieldFormFields().clear();
    model.getStaticTextFieldFormFields().clear();

    try
    {
      XEnumeration xenu =
        UNO.XTextFieldsSupplier(model.doc).getTextFields().createEnumeration();
      while (xenu.hasMoreElements())
      {
        try
        {
          XDependentTextField tf = UNO.XDependentTextField(xenu.nextElement());
          if (tf == null) continue;

          if (UNO.supportsService(tf, "com.sun.star.text.TextField.InputUser"))
          {
            String varName = UNO.getProperty(tf, "Content").toString();
            String funcName = TextDocumentModel.getFunctionNameForUserFieldName(varName);
            
            if (funcName == null) continue;
            
            XPropertySet master = getUserFieldMaster(varName);
            FormField f = FormFieldFactory.createInputUserFormField(model.doc, tf, master);
            Function func = getFunctionLibrary().get(funcName);
            
            if (func == null)
            {
              Logger.error(L.m(
                "Die im Formularfeld verwendete Funktion '%1' ist nicht definiert.",
                funcName));
              continue;
            }
            
            String[] pars = func.parameters();
            if (pars.length == 0) model.getStaticTextFieldFormFields().add(f);
            for (int i = 0; i < pars.length; i++)
            {
              String id = pars[i];
              if (id != null && id.length() > 0)
              {
                if (!model.getIdToTextFieldFormFields().containsKey(id))
                  model.getIdToTextFieldFormFields().put(id, new Vector<FormField>());

                List<FormField> formFields = model.getIdToTextFieldFormFields().get(id);
                formFields.add(f);
              }
            }
          }

          if (UNO.supportsService(tf, "com.sun.star.text.TextField.Database"))
          {
            XPropertySet master = tf.getTextFieldMaster();
            String id = (String) UNO.getProperty(master, "DataColumnName");
            if (id != null && id.length() > 0)
            {
              if (!model.getIdToTextFieldFormFields().containsKey(id))
                model.getIdToTextFieldFormFields().put(id, new Vector<FormField>());

              List<FormField> formFields = model.getIdToTextFieldFormFields().get(id);
              formFields.add(FormFieldFactory.createDatabaseFormField(model.doc, tf));
            }
          }
        }
        catch (Exception x)
        {
          Logger.error(x);
        }
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Diese Methode bestimmt die Vorbelegung der Formularfelder des Formulars und
   * liefert eine HashMap zurück, die die id eines Formularfeldes auf den bestimmten
   * Wert abbildet. Der Wert ist nur dann klar definiert, wenn alle FormFields zu
   * einer ID unverändert geblieben sind, oder wenn nur untransformierte Felder
   * vorhanden sind, die alle den selben Wert enthalten. Gibt es zu einer ID kein
   * FormField-Objekt, so wird der zuletzt abgespeicherte Wert zu dieser ID aus dem
   * FormDescriptor verwendet. Die Methode sollte erst aufgerufen werden, nachdem dem
   * Model mit setIDToFormFields die verfügbaren Formularfelder bekanntgegeben
   * wurden.
   * 
   * @return eine vollständige Zuordnung von Feld IDs zu den aktuellen Vorbelegungen
   *         im Dokument. TESTED
   */
  public synchronized HashMap<String, String> getIDToPresetValue()
  {
    HashMap<String, String> idToPresetValue = new HashMap<String, String>();
    Set<String> ids = new HashSet<String>(model.getFormFieldValuesMap().keySet());

    // mapIdToPresetValue vorbelegen: Gibt es zu id mindestens ein untransformiertes
    // Feld, so wird der Wert dieses Feldes genommen. Gibt es kein untransformiertes
    // Feld, so wird der zuletzt im Formularwerte abgespeicherte Wert genommen.
    for (String id : ids)
    {
      List<FormField> fields = new ArrayList<FormField>();
      if (model.getIdToFormFields().get(id) != null) fields.addAll(model.getIdToFormFields().get(id));
      if (model.getIdToTextFieldFormFields().get(id) != null)
        fields.addAll(model.getIdToTextFieldFormFields().get(id));

      String value = model.getFirstUntransformedValue(fields);
      if (value == null) value = model.getFormFieldValuesMap().get(id);
      if (value != null) idToPresetValue.put(id, value);
    }

    // Alle id's herauslöschen, deren Felder-Werte nicht konsistent sind.
    for (String id : ids)
    {
      String value = idToPresetValue.get(id);
      if (value != null)
      {
        boolean fieldValuesConsistent =
          fieldValuesConsistent(model.getIdToFormFields().get(id), idToPresetValue, value)
            && fieldValuesConsistent(model.getIdToTextFieldFormFields().get(id),
              idToPresetValue, value);
        if (!fieldValuesConsistent) idToPresetValue.remove(id);
      }
    }

    // IDs, zu denen keine gültige Vorbelegung vorhanden ist auf FISHY setzen. Das
    // Setzen von FISHY darf erst am Ende der Methode erfolgen, damit FISHY nicht
    // bereits als Wert bei der Transformation berücksichtigt wird.
    for (String id : ids)
    {
      if (!idToPresetValue.containsKey(id))
        idToPresetValue.put(id, TextDocumentModel.FISHY);
    }
    return idToPresetValue;
  }
  
  /**
   * Diese Methode blockt/unblocked die Contoller, die für das Rendering der
   * Darstellung in den Dokumenten zuständig sind, jedoch nur, wenn nicht der
   * debug-modus gesetzt ist.
   * 
   * @param state
   */
  public synchronized void setLockControllers(boolean lock)
  {
    try
    {
      if (WollMuxFiles.isDebugMode() == false
        && UNO.XModel(model.doc) != null)
      {
        if (lock)
          UNO.XModel(model.doc).lockControllers();
        else
          UNO.XModel(model.doc).unlockControllers();
      }
    }
    catch (java.lang.Exception e)
    {}
  }

  /**
   * Diese Methode prüft ob die Formularwerte der in fields enthaltenen
   * Formularfelder konsistent aus den in mapIdToValue enthaltenen Werten abgeleitet
   * werden können; Der Wert value beschreibt dabei den Wert der für
   * FormField-Objekte anzuwenden ist, die untransformiert sind oder deren Methode
   * field.singleParameterTrafo()==true zurück liefert. Ist in fields auch nur ein
   * Formularfeld enthalten, dessen Inhalt nicht konsistent aus diesen Werten
   * abgeleitet werden kann, so liefert die Methode false zurück. Die Methode liefert
   * true zurück, wenn die Konsistenzprüfung für alle Formularfelder erfolgreich
   * durchlaufen wurde.
   * 
   * @param fields
   *          Enthält die Liste der zu prüfenden Felder.
   * @param mapIdToValues
   *          enthält die für evtl. gesetzte Trafofunktionen zu verwendenden
   *          Parameter
   * @param value
   *          enthält den Wert der für untransformierte Felder oder für Felder, deren
   *          Trafofunktion nur einen einheitlichen Wert für sämtliche Parameter
   *          erwartet, verwendet werden soll.
   * @return true, wenn alle Felder konsistent aus den Werten mapIdToValue und value
   *         abgeleitet werden können oder false, falls nicht.
   * 
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  private boolean fieldValuesConsistent(List<FormField> fields,
      HashMap<String, String> mapIdToValues, String value)
  {
    if (fields == null) fields = new ArrayList<FormField>();
    if (mapIdToValues == null) mapIdToValues = new HashMap<String, String>();

    for (FormField field : fields)
    {
      // Soll-Wert refValue bestimmen
      String refValue = value;
      String trafoName = field.getTrafoName();
      if (trafoName != null)
      {
        if (field.singleParameterTrafo())
        {
          refValue = getTransformedValue(trafoName, value);
        }
        else
        {
          // Abbruch, wenn die Parameter für diese Funktion unvollständig sind.
          Function func = getFunctionLibrary().get(trafoName);
          if (func != null) for (String par : func.parameters())
            if (mapIdToValues.get(par) == null) return false;

          refValue = getTransformedValue(trafoName, mapIdToValues);
        }
      }

      // Ist-Wert mit Soll-Wert vergleichen:
      if (!field.getValue().equals(refValue)) return false;
    }
    return true;
  }

  /**
   * Diese Methode führt die Trafofunktion trafoName aus, übergibt ihr dabei die
   * aktuell dem TextDocumentModel bekannten Formularwerte als Parameter und liefert
   * das transformierte Ergebnis zurück; Die Trafofunktion trafoName darf nicht null
   * sein und muss global oder dokumentlokal definiert sein; Ist die Transfofunktion
   * nicht in der globalen oder dokumentlokalen Funktionsbibliothek enthalten, so
   * wird ein Fehlerstring zurückgeliefert und eine weitere Fehlermeldung in die
   * Log-Datei geschrieben.
   * 
   * Befindet sich das TextDocumentModel in einem über {@link #startSimulation()}
   * gesetzten Simulationslauf, so wird die Trafo mit den im Simulationslauf
   * gesetzten Formularwerten berechnet und zurück geliefert.
   * 
   * @param trafoName
   *          Der Name der Trafofunktion, der nicht null sein darf.
   * @return Der transformierte Wert falls die Trafo definiert ist oder ein
   *         Fehlerstring, falls die Trafo nicht definiert ist.
   */
  public String getTransformedValue(String trafoName)
  {
    if (simulationResult == null)
      return getTransformedValue(trafoName, model.getFormFieldValuesMap());
    else
      return getTransformedValue(trafoName, simulationResult.getFormFieldValues());
  }

  /**
   * Diese Methode berechnet die Transformation des Wertes value mit der
   * Trafofunktion trafoName, die global oder dokumentlokal definiert sein muss;
   * Dabei wird für alle von der Trafofunktion erwarteten Parameter der Wert value
   * übergeben - eine Praxis, die für insertFormValue- und für insertValue-Befehle
   * üblich war und mit Einführung der UserFieldFormFields geändert wurde (siehe
   * {@link #getTransformedValue(String)}. Ist trafoName==null, so wird value
   * zurückgegeben. Ist die Transformationsionfunktion nicht in der globalen oder
   * dokumentlokalen Funktionsbibliothek enthalten, so wird ein Fehlerstring
   * zurückgeliefert und eine weitere Fehlermeldung in die Log-Datei geschrieben.
   * 
   * @param value
   *          Der zu transformierende Wert.
   * @param trafoName
   *          Der Name der Trafofunktion, der auch null sein darf.
   * @return Der transformierte Wert falls das trafoName gesetzt ist und die Trafo
   *         korrekt definiert ist. Ist trafoName==null, so wird value unverändert
   *         zurückgeliefert. Ist die Funktion trafoName nicht definiert, wird ein
   *         Fehlerstring zurückgeliefert.
   */
  public String getTransformedValue(String trafoName, String value)
  {
    String transformed = value;
    if (trafoName != null)
    {
      Function func = getFunctionLibrary().get(trafoName);
      if (func != null)
      {
        SimpleMap args = new SimpleMap();
        String[] pars = func.parameters();
        for (int i = 0; i < pars.length; i++)
          args.put(pars[i], value);
        transformed = func.getString(args);
      }
      else
      {
        transformed = L.m("<FEHLER: TRAFO '%1' nicht definiert>", trafoName);
        Logger.error(L.m("Die TRAFO '%1' ist nicht definiert.", trafoName));
      }
    }
    return transformed;
  }

  /**
   * Diese Methode führt die Trafofunktion trafoName aus, wobei die Werte der
   * erwarteten Parameter aus mapIdToValues gewonnen werden, und liefert das
   * transformierte Ergebnis zurück. Die Trafofunktion trafoName darf nicht null sein
   * und muss global oder dokumentlokal definiert sein; Ist die Transfofunktion nicht
   * in der globalen oder dokumentlokalen Funktionsbibliothek enthalten, so wird ein
   * Fehlerstring zurückgeliefert und eine weitere Fehlermeldung in die Log-Datei
   * geschrieben.
   * 
   * @param trafoName
   *          Der Name der Trafofunktion, der nicht null sein darf.
   * @param mapIdToValues
   *          eine Zuordnung von ids auf die zugehörigen Werte, aus der die Werte für
   *          die von der Trafofunktion erwarteten Parameter bestimmt werden.
   * @return Der transformierte Wert falls die Trafo definiert ist oder ein
   *         Fehlerstring, falls die Trafo nicht definiert ist.
   */
  private String getTransformedValue(String trafoName,
      Map<String, String> mapIdToValues)
  {
    Function func = getFunctionLibrary().get(trafoName);
    if (func != null)
    {
      SimpleMap args = new SimpleMap();
      String[] pars = func.parameters();
      for (int i = 0; i < pars.length; i++)
        args.put(pars[i], mapIdToValues.get(pars[i]));
      return func.getString(args);
    }
    else
    {
      Logger.error(L.m("Die TRAFO '%1' ist nicht definiert.", trafoName));
      return L.m("<FEHLER: TRAFO '%1' nicht definiert>", trafoName);
    }
  }
  
  /**
   * Markiert das Dokument als Formulardokument - damit liefert
   * {@link #isFormDocument()} zukünftig true und der Typ "formDocument" wird
   * persistent im Dokument hinterlegt.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public synchronized void markAsFormDocument()
  {
    model.updateLastTouchedByVersionInfo(WollMuxSingleton.getVersion(), Utils.getOOoVersion());
    model.setType("formDocument");
    model.getPersistentData().setData(DataID.SETTYPE, "formDocument");
  }

  /**
   * Entfernt die WollMux-Kommandos "insertFormValue", "setGroups", "setType
   * formDocument" und "form", sowie die WollMux-Formularbeschreibung und Daten aus
   * dem Dokument doc.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public synchronized void deForm()
  {
    model.updateLastTouchedByVersionInfo(WollMuxSingleton.getVersion(), Utils.getOOoVersion());

    XBookmarksSupplier bmSupp = UNO.XBookmarksSupplier(model.doc);
    XNameAccess bookmarks = bmSupp.getBookmarks();
    String[] names = bookmarks.getElementNames();
    for (int i = 0; i < names.length; ++i)
    {
      try
      {
        String bookmark = names[i];
        if (TextDocumentModel.BOOKMARK_KILL_PATTERN.matcher(bookmark).matches())
        {
          XTextContent bm = UNO.XTextContent(bookmarks.getByName(bookmark));
          bm.getAnchor().getText().removeTextContent(bm);
        }

      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }

    model.getPersistentData().removeData(DataID.FORMULARBESCHREIBUNG);
    model.getPersistentData().removeData(DataID.FORMULARWERTE);
  }

  /**
   * Liefert die aktuell im Dokument gesetzte FilenameGeneratorFunction in Form eines
   * ConfigThingy-Objekts, oder null, wenn keine gültige FilenameGeneratorFunction
   * gesetzt ist.
   */
  public synchronized ConfigThingy getFilenameGeneratorFunc()
  {
    String func = model.getPersistentData().getData(DataID.FILENAMEGENERATORFUNC);
    if (func == null) return null;
    try
    {
      return new ConfigThingy("func", func).getFirstChild();
    }
    catch (Exception e)
    {
      return null;
    }
  }

  /**
   * Setzt die FilenameGeneratorFunction, die verwendet wird für die Generierung des
   * Namensvorschlags beim Speichern neuer Dokumente persistent auf die durch
   * ConfigThingy c repräsentierte Funktion oder löscht diese Funktion, wenn c ==
   * null ist.
   */
  public synchronized void setFilenameGeneratorFunc(ConfigThingy c)
  {
    model.updateLastTouchedByVersionInfo(WollMuxSingleton.getVersion(), Utils.getOOoVersion());
    if (c == null)
      model.getPersistentData().removeData(DataID.FILENAMEGENERATORFUNC);
    else
      model.getPersistentData().setData(DataID.FILENAMEGENERATORFUNC, c.stringRepresentation());
  }
  
  /**
   * Entfernt alle Bookmarks, die keine WollMux-Bookmarks sind aus dem Dokument doc.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public synchronized void removeNonWMBookmarks()
  {
    model.updateLastTouchedByVersionInfo(WollMuxSingleton.getVersion(), Utils.getOOoVersion());

    XBookmarksSupplier bmSupp = UNO.XBookmarksSupplier(model.doc);
    XNameAccess bookmarks = bmSupp.getBookmarks();
    String[] names = bookmarks.getElementNames();
    for (int i = 0; i < names.length; ++i)
    {
      try
      {
        String bookmark = names[i];
        if (!TextDocumentModel.WOLLMUX_BOOKMARK_PATTERN.matcher(bookmark).matches())
        {
          XTextContent bm = UNO.XTextContent(bookmarks.getByName(bookmark));
          bm.getAnchor().getText().removeTextContent(bm);
        }

      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }
  }

  /**
   * Diese Methode setzt die Eigenschaften "Sichtbar" (visible) und die Anzeige der
   * Hintergrundfarbe (showHighlightColor) für alle Druckblöcke eines bestimmten
   * Blocktyps blockName (z.B. allVersions).
   * 
   * @param blockName
   *          Der Blocktyp dessen Druckblöcke behandelt werden sollen.
   * @param visible
   *          Der Block wird sichtbar, wenn visible==true und unsichtbar, wenn
   *          visible==false.
   * @param showHighlightColor
   *          gibt an ob die Hintergrundfarbe angezeigt werden soll (gilt nur, wenn
   *          zu einem betroffenen Druckblock auch eine Hintergrundfarbe angegeben
   *          ist).
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public synchronized void setPrintBlocksProps(String blockName, boolean visible,
      boolean showHighlightColor)
  {
    model.updateLastTouchedByVersionInfo(WollMuxSingleton.getVersion(), Utils.getOOoVersion());

    Iterator<DocumentCommand> iter = new HashSet<DocumentCommand>().iterator();
    if (blockName.equals(SachleitendeVerfuegung.BLOCKNAME_SLV_ALL_VERSIONS))
      iter = model.getDocumentCommands().allVersionsIterator();
    if (blockName.equals(SachleitendeVerfuegung.BLOCKNAME_SLV_DRAFT_ONLY))
      iter = model.getDocumentCommands().draftOnlyIterator();
    if (blockName.equals(SachleitendeVerfuegung.BLOCKNAME_SLV_NOT_IN_ORIGINAL))
      iter = model.getDocumentCommands().notInOriginalIterator();
    if (blockName.equals(SachleitendeVerfuegung.BLOCKNAME_SLV_ORIGINAL_ONLY))
      iter = model.getDocumentCommands().originalOnlyIterator();
    if (blockName.equals(SachleitendeVerfuegung.BLOCKNAME_SLV_COPY_ONLY))
      iter = model.getDocumentCommands().copyOnlyIterator();

    while (iter.hasNext())
    {
      DocumentCommand cmd = iter.next();
      cmd.setVisible(visible);
      String highlightColor =
        ((OptionalHighlightColorProvider) cmd).getHighlightColor();

      if (highlightColor != null)
      {
        if (showHighlightColor)
          try
          {
            Integer bgColor = Integer.valueOf(Integer.parseInt(highlightColor, 16));
            UNO.setProperty(cmd.getTextCursor(), "CharBackColor", bgColor);
          }
          catch (NumberFormatException e)
          {
            Logger.error(L.m(
              "Fehler in Dokumentkommando '%1': Die Farbe HIGHLIGHT_COLOR mit dem Wert '%2' ist ungültig.",
              "" + cmd, highlightColor));
          }
        else
        {
          UNO.setPropertyToDefault(cmd.getTextCursor(), "CharBackColor");
        }
      }
    }
  }

  /**
   * Stellt sicher, dass persistente Daten dieses Dokuments auch tatsächlich
   * persistiert werden.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public synchronized void flushPersistentData()
  {
    model.getPersistentData().flush();
  }
  
  /**
   * Blendet alle Sichtbarkeitselemente eines Dokuments (Dokumentkommandos oder
   * Bereiche mit Namensanhang 'GROUPS ...'), die einer bestimmten Gruppe groupId
   * zugehören, ein oder aus.
   * 
   * @param groupId
   * @param visible
   */
  public synchronized void setVisibleState(String groupId, boolean visible)
  {
    try
    {
      Map<String, Boolean> groupState = model.getMapGroupIdToVisibilityState();
      if (simulationResult != null)
        groupState = simulationResult.getGroupsVisibilityState();

      groupState.put(groupId, visible);

      VisibilityElement firstChangedElement = null;

      // Sichtbarkeitselemente durchlaufen und alle ggf. updaten:
      Iterator<VisibilityElement> iter = model.getDocumentCommands().getSetGroups().iterator();
      while (iter.hasNext())
      {
        VisibilityElement visibleElement = iter.next();
        Set<String> groups = visibleElement.getGroups();
        if (!groups.contains(groupId)) continue;

        // Visibility-Status neu bestimmen:
        boolean setVisible = true;
        for (String gid : groups)
        {
          if (groupState.get(gid).equals(Boolean.FALSE)) setVisible = false;
        }

        // Element merken, dessen Sichtbarkeitsstatus sich zuerst ändert und
        // den focus (ViewCursor) auf den Start des Bereichs setzen. Da das
        // Setzen eines ViewCursors in einen unsichtbaren Bereich nicht
        // funktioniert, wird die Methode focusRangeStart zwei mal aufgerufen,
        // je nach dem, ob der Bereich vor oder nach dem Setzen des neuen
        // Sichtbarkeitsstatus sichtbar ist.
        if (setVisible != visibleElement.isVisible() && firstChangedElement == null)
        {
          firstChangedElement = visibleElement;
          if (firstChangedElement.isVisible()) model.focusRangeStart(visibleElement);
        }

        // neuen Sichtbarkeitsstatus setzen:
        try
        {
          visibleElement.setVisible(setVisible);
        }
        catch (RuntimeException e)
        {
          // Absicherung gegen das manuelle Löschen von Dokumentinhalten
        }
      }

      // Den Cursor (nochmal) auf den Anfang des Ankers des Elements setzen,
      // dessen Sichtbarkeitsstatus sich zuerst geändert hat (siehe Begründung
      // oben).
      if (firstChangedElement != null && firstChangedElement.isVisible())
        model.focusRangeStart(firstChangedElement);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Diese Methode entfernt alle Reste, die von nicht mehr referenzierten
   * AUTOFUNCTIONS übrig bleiben: AUTOFUNCTIONS-Definitionen aus der
   * Funktionsbibliothek, der Formularbeschreibung in den persistenten Daten und
   * nicht mehr benötigte TextFieldMaster von ehemaligen InputUser-Textfeldern -
   * Durch die Aufräumaktion ändert sich der DocumentModified-Status des Dokuments
   * nicht.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1) TESTED
   */
  private void cleanupGarbageOfUnreferencedAutofunctions()
  {
    boolean modified = model.isDocumentModified();

    // Liste aller derzeit eingesetzten Trafos aufbauen:
    HashSet<String> usedFunctions = new HashSet<String>();
    for (Map.Entry<String, List<FormField>> ent : model.getIdToFormFields().entrySet())
    {

      List<FormField> l = ent.getValue();
      for (Iterator<FormField> iterator = l.iterator(); iterator.hasNext();)
      {
        FormField f = iterator.next();
        String trafoName = f.getTrafoName();
        if (trafoName != null) usedFunctions.add(trafoName);
      }
    }

    for (Map.Entry<String, List<FormField>> ent : model.getIdToTextFieldFormFields().entrySet())
    {

      List<FormField> l = ent.getValue();
      for (Iterator<FormField> iterator = l.iterator(); iterator.hasNext();)
      {
        FormField f = iterator.next();
        String trafoName = f.getTrafoName();
        if (trafoName != null) usedFunctions.add(trafoName);
      }
    }
    for (Iterator<FormField> iterator = model.getStaticTextFieldFormFields().iterator(); iterator.hasNext();)
    {
      FormField f = iterator.next();
      String trafoName = f.getTrafoName();
      if (trafoName != null) usedFunctions.add(trafoName);
    }

    // Nicht mehr benötigte Autofunctions aus der Funktionsbibliothek löschen:
    FunctionLibrary funcLib = getFunctionLibrary();
    for (Iterator<String> iter = funcLib.getFunctionNames().iterator(); iter.hasNext();)
    {
      String name = iter.next();
      if (name == null || !name.startsWith(TextDocumentModel.AUTOFUNCTION_PREFIX)
        || usedFunctions.contains(name)) continue;
      funcLib.remove(name);
    }

    // Nicht mehr benötigte Autofunctions aus der Formularbeschreibung der
    // persistenten Daten löschen.
    ConfigThingy functions =
      model.getFormDescription().query("Formular").query("Funktionen");
    for (Iterator<ConfigThingy> iter = functions.iterator(); iter.hasNext();)
    {
      ConfigThingy funcs = iter.next();
      for (Iterator<ConfigThingy> iterator = funcs.iterator(); iterator.hasNext();)
      {
        String name = iterator.next().getName();
        if (name == null || !name.startsWith(TextDocumentModel.AUTOFUNCTION_PREFIX)
          || usedFunctions.contains(name)) continue;
        iterator.remove();
      }
    }
    storeCurrentFormDescription();

    // Nicht mehr benötigte TextFieldMaster von ehemaligen InputUser-Textfeldern
    // löschen:
    XNameAccess masters = UNO.XTextFieldsSupplier(model.doc).getTextFieldMasters();
    String prefix = "com.sun.star.text.FieldMaster.User.";
    String[] masterNames = masters.getElementNames();
    for (int i = 0; i < masterNames.length; i++)
    {
      String masterName = masterNames[i];
      if (masterName == null || !masterName.startsWith(prefix)) continue;
      String varName = masterName.substring(prefix.length());
      String trafoName = TextDocumentModel.getFunctionNameForUserFieldName(varName);
      if (trafoName != null && !usedFunctions.contains(trafoName))
      {
        try
        {
          XComponent m = UNO.XComponent(masters.getByName(masterName));
          m.dispose();
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }

    model.setDocumentModified(modified);
  }

  /**
   * Ersetzt die Formularbeschreibung dieses Dokuments durch die aus conf. Falls conf
   * == null, so wird die Formularbeschreibung gelöscht. ACHTUNG! conf wird nicht
   * kopiert sondern als Referenz eingebunden.
   * 
   * @param conf
   *          ein WM-Knoten, der "Formular"-Kinder hat. Falls conf == null, so wird
   *          die Formularbeschreibungsnotiz gelöscht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public synchronized void setFormDescription(ConfigThingy conf)
  {
    if (conf != null)
      model.setFormularConf(conf);
    else
      model.setFormularConf(new ConfigThingy("WM"));
    storeCurrentFormDescription();
    model.setDocumentModified(true);
  }

  /**
   * Speichert den neuen Wert value zum Formularfeld fieldId im
   * Formularwerte-Abschnitt in den persistenten Daten oder löscht den Eintrag für
   * fieldId aus den persistenten Daten, wenn value==null ist. ACHTUNG! Damit der
   * neue Wert angezeigt wird, ist ein Aufruf von {@link #updateFormFields(String)}
   * erforderlich.
   * 
   * Befindet sich das TextDocumentModel in einem über {@link #startSimulation()}
   * gestarteten Simulationslauf, so werden die persistenten Daten nicht verändert
   * und der neue Wert nur in einem internen Objekt des Simulationslaufs gespeichert
   * anstatt im Dokument.
   * 
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1)
   */
  public synchronized void setFormFieldValue(String fieldId, String value)
  {
    if (simulationResult == null)
    {
      model.updateLastTouchedByVersionInfo(WollMuxSingleton.getVersion(), Utils.getOOoVersion());
      if (value == null)
        model.getFormFieldValues().remove(fieldId);
      else
        model.getFormFieldValues().put(fieldId, value);
      model.getPersistentData().setData(DataID.FORMULARWERTE, getFormFieldValuesString());
    }
    else
      simulationResult.setFormFieldValue(fieldId, value);
  }
  
  /**
   * Serialisiert die aktuellen Werte aller Fomularfelder.
   */
  private String getFormFieldValuesString()
  {
    // Neues ConfigThingy für "Formularwerte"-Abschnitt erzeugen:
    ConfigThingy werte = new ConfigThingy("WM");
    ConfigThingy formwerte = new ConfigThingy("Formularwerte");
    werte.addChild(formwerte);
    for (Map.Entry<String, String> ent : model.getFormFieldValues().entrySet())
    {
      String key = ent.getKey();
      String value = ent.getValue();
      if (key != null && value != null)
      {
        ConfigThingy entry = new ConfigThingy("");
        ConfigThingy cfID = new ConfigThingy("ID");
        cfID.add(key);
        ConfigThingy cfVALUE = new ConfigThingy("VALUE");
        cfVALUE.add(value);
        entry.addChild(cfID);
        entry.addChild(cfVALUE);
        formwerte.addChild(entry);
      }
    }

    return werte.stringRepresentation();
  }

  
  /**
   * Speichert die aktuelle Formularbeschreibung in den persistenten Daten des
   * Dokuments oder löscht den entsprechenden Abschnitt aus den persistenten Daten,
   * wenn die Formularbeschreibung nur aus einer leeren Struktur ohne eigentlichen
   * Formularinhalt besteht.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void storeCurrentFormDescription()
  {
    model.updateLastTouchedByVersionInfo(WollMuxSingleton.getVersion(), Utils.getOOoVersion());

    ConfigThingy conf = model.getFormDescription();
    try
    {
      if ((conf.query("Fenster").count() > 0 && conf.get("Fenster").count() > 0)
        || (conf.query("Sichtbarkeit").count() > 0 && conf.get("Sichtbarkeit").count() > 0)
        || (conf.query("Funktionen").count() > 0 && conf.get("Funktionen").count() > 0))
        model.getPersistentData().setData(DataID.FORMULARBESCHREIBUNG,
          conf.stringRepresentation());
      else
        model.getPersistentData().removeData(DataID.FORMULARBESCHREIBUNG);
    }
    catch (NodeNotFoundException e)
    {
      Logger.error(L.m("Dies kann nicht passieren."), e);
    }
  }

  /**
   * Ersetzt die aktuelle Selektion (falls vorhanden) durch ein WollMux-Formularfeld
   * mit ID id, dem Hinweistext hint und der durch trafoConf definierten TRAFO. Das
   * Formularfeld ist direkt einsetzbar, d.h. sobald diese Methode zurückkehrt, kann
   * über setFormFieldValue(id,...) das Feld befüllt werden. Ist keine Selektion
   * vorhanden, so tut die Funktion nichts.
   * 
   * @param trafoConf
   *          darf null sein, dann wird keine TRAFO gesetzt. Ansonsten ein
   *          ConfigThingy mit dem Aufbau "Bezeichner( FUNKTIONSDEFINITION )", wobei
   *          Bezeichner ein beliebiger Bezeichner ist und FUNKTIONSDEFINITION ein
   *          erlaubter Parameter für
   *          {@link de.muenchen.allg.itd51.wollmux.func.FunctionFactory#parse(ConfigThingy, FunctionLibrary, DialogLibrary, Map)}
   *          , d.h. der oberste Knoten von FUNKTIONSDEFINITION muss eine erlaubter
   *          Funktionsname, z.B. "AND" sein. Der Bezeichner wird NICHT als Name der
   *          TRAFO verwendet. Stattdessen wird ein neuer eindeutiger TRAFO-Name
   *          generiert.
   * @param hint
   *          Ein Hinweistext der als Tooltip des neuen Formularfeldes angezeigt
   *          werden soll. hint kann null sein, dann wird kein Hinweistext angezeigt.
   * 
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1) TESTED
   */
  public synchronized void replaceSelectionWithTrafoField(ConfigThingy trafoConf,
      String hint)
  {
    String trafoName = addLocalAutofunction(trafoConf);

    if (trafoName != null) try
    {
      // Neues UserField an der Cursorposition einfügen
      addNewInputUserField(model.getViewCursor(), trafoName, hint);

      // Datenstrukturen aktualisieren
      collectNonWollMuxFormFields();

      // Formularwerte-Abschnitt für alle referenzierten fieldIDs vorbelegen
      // wenn noch kein Wert gesetzt ist und Anzeige aktualisieren.
      Function f = getFunctionLibrary().get(trafoName);
      String[] fieldIds = new String[] {};
      if (f != null) fieldIds = f.parameters();
      for (int i = 0; i < fieldIds.length; i++)
      {
        String fieldId = fieldIds[i];
        // Feldwert mit leerem Inhalt vorbelegen, wenn noch kein Wert gesetzt
        // ist.
        if (!model.getFormFieldValues().containsKey(fieldId)) setFormFieldValue(fieldId, "");
      }

      // Nicht referenzierte Autofunktionen/InputUser-TextFieldMaster löschen
      cleanupGarbageOfUnreferencedAutofunctions();
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Die Methode fügt die Formular-Abschnitte aus der Formularbeschreibung der Notiz
   * von formCmd zur aktuellen Formularbeschreibung des Dokuments in den persistenten
   * Daten hinzu und löscht die Notiz (sowie den übrigen Inhalt von formCmd).
   * 
   * @param formCmd
   *          Das formCmd, das die Notiz mit den hinzuzufügenden Formular-Abschnitten
   *          einer Formularbeschreibung enthält.
   * @throws ConfigurationErrorException
   *           Die Notiz der Formularbeschreibung ist nicht vorhanden, die
   *           Formularbeschreibung ist nicht vollständig oder kann nicht geparst
   *           werden.
   */
  synchronized public void addToCurrentFormDescription(DocumentCommand.Form formCmd)
      throws ConfigurationErrorException
  {
    XTextRange range = formCmd.getTextCursor();

    XTextContent annotationField =
      UNO.XTextContent(TextDocumentModel.findAnnotationFieldRecursive(range));
    if (annotationField == null)
      throw new ConfigurationErrorException(
        L.m("Die zugehörige Notiz mit der Formularbeschreibung fehlt."));

    Object content = UNO.getProperty(annotationField, "Content");
    if (content == null)
      throw new ConfigurationErrorException(
        L.m("Die zugehörige Notiz mit der Formularbeschreibung kann nicht gelesen werden."));

    // Formularbeschreibung übernehmen und persistent speichern:
    TextDocumentModel.addToFormDescription(model.getFormDescription(), content.toString());
    storeCurrentFormDescription();

    // Notiz (sowie anderen Inhalt des Bookmarks) löschen
    formCmd.setTextRangeString("");
  }

  /**
   * Ändert die Definition der TRAFO mit Name trafoName auf trafoConf. Die neue
   * Definition wirkt sich sofort auf folgende
   * {@link #setFormFieldValue(String, String)} Aufrufe aus.
   * 
   * @param trafoConf
   *          ein ConfigThingy mit dem Aufbau "Bezeichner( FUNKTIONSDEFINITION )",
   *          wobei Bezeichner ein beliebiger Bezeichner ist und FUNKTIONSDEFINITION
   *          ein erlaubter Parameter für
   *          {@link de.muenchen.allg.itd51.wollmux.func.FunctionFactory#parse(ConfigThingy, FunctionLibrary, DialogLibrary, Map)}
   *          , d.h. der oberste Knoten von FUNKTIONSDEFINITION muss eine erlaubter
   *          Funktionsname, z.B. "AND" sein. Der Bezeichner wird NICHT verwendet.
   *          Der Name der TRAFO wird ausschließlich durch trafoName festgelegt.
   * @throws UnavailableException
   *           wird geworfen, wenn die Trafo trafoName nicht schreibend verändert
   *           werden kann, weil sie z.B. nicht existiert oder in einer globalen
   *           Funktionsbeschreibung definiert ist.
   * @throws ConfigurationErrorException
   *           beim Parsen der Funktion trafoConf trat ein Fehler auf.
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1) TESTED
   */
  public synchronized void setTrafo(String trafoName, ConfigThingy trafoConf)
      throws UnavailableException, ConfigurationErrorException
  {
    // Funktionsknoten aus Formularbeschreibung zum Anpassen holen
    ConfigThingy func;
    try
    {
      func =
        model.getFormDescription().query("Formular").query("Funktionen").query(trafoName,
          2).getLastChild();
    }
    catch (NodeNotFoundException e)
    {
      throw new UnavailableException(e);
    }

    // Funktion parsen und in Funktionsbibliothek setzen:
    FunctionLibrary funcLib = getFunctionLibrary();
    Function function =
      FunctionFactory.parseChildren(trafoConf, funcLib, getDialogLibrary(),
        getFunctionContext());
    funcLib.add(trafoName, function);

    // Kinder von func löschen, damit sie später neu gesetzt werden können
    for (Iterator<ConfigThingy> iter = func.iterator(); iter.hasNext();)
    {
      iter.next();
      iter.remove();
    }

    // Kinder von trafoConf auf func übertragen
    for (Iterator<ConfigThingy> iter = trafoConf.iterator(); iter.hasNext();)
    {
      ConfigThingy f = iter.next();
      func.addChild(new ConfigThingy(f));
    }

    // neue Formularbeschreibung sichern
    storeCurrentFormDescription();

    // Die neue Funktion kann von anderen IDs abhängen als die bisherige
    // Funktion. Hier muss dafür gesorgt werden, dass in idToTextFieldFormFields
    // veraltete ID-Zuordnungen gelöscht und neue ID-Zuordungen eingetragen
    // werden. Am einfachsten macht dies vermutlich ein
    // collectNonWollMuxFormFields(). InsertFormValue-Dokumentkommandos haben
    // eine feste ID-Zuordnung und kommen aus dieser auch nicht aus. D.h.
    // InsertFormValue-Bookmarks müssen nicht aktualisiert werden.
    collectNonWollMuxFormFields();
  }

  /**
   * Erzeugt in der Funktionsbeschreibung eine neue Funktion mit einem automatisch
   * generierten Namen, registriert sie in der Funktionsbibliothek, so dass diese
   * sofort z.B. als TRAFO-Funktion genutzt werden kann und liefert den neuen
   * generierten Funktionsnamen zurück oder null, wenn funcConf fehlerhaft ist.
   * 
   * Der automatisch generierte Name ist, nach dem Prinzip
   * PRAEFIX_aktuelleZeitinMillisekunden_zahl aufgebaut. Es wird aber in jedem Fall
   * garantiert, dass der neue Name eindeutig ist und nicht bereits in der
   * Funktionsbibliothek vorkommt.
   * 
   * @param funcConf
   *          Ein ConfigThingy mit dem Aufbau "Bezeichner( FUNKTIONSDEFINITION )",
   *          wobei Bezeichner ein beliebiger Bezeichner ist und FUNKTIONSDEFINITION
   *          ein erlaubter Parameter für
   *          {@link de.muenchen.allg.itd51.wollmux.func.FunctionFactory#parse(ConfigThingy, FunctionLibrary, DialogLibrary, Map)}
   *          , d.h. der oberste Knoten von FUNKTIONSDEFINITION muss eine erlaubter
   *          Funktionsname, z.B. "AND" sein. Der Bezeichner wird NICHT als Name der
   *          TRAFO verwendet. Stattdessen wird ein neuer eindeutiger TRAFO-Name
   *          generiert.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private String addLocalAutofunction(ConfigThingy funcConf)
  {
    FunctionLibrary funcLib = getFunctionLibrary();
    DialogLibrary dialogLib = getDialogLibrary();
    Map<Object, Object> context = getFunctionContext();

    // eindeutigen Namen für die neue Autofunktion erzeugen:
    Set<String> currentFunctionNames = funcLib.getFunctionNames();
    String name = null;
    for (int i = 0; name == null || currentFunctionNames.contains(name); ++i)
      name = TextDocumentModel.AUTOFUNCTION_PREFIX + System.currentTimeMillis() + "_" + i;

    try
    {
      funcLib.add(name, FunctionFactory.parseChildren(funcConf, funcLib, dialogLib,
        context));

      // Funktion zur Formularbeschreibung hinzufügen:
      ConfigThingy betterNameFunc = new ConfigThingy(name);
      for (Iterator<ConfigThingy> iter = funcConf.iterator(); iter.hasNext();)
      {
        ConfigThingy func = iter.next();
        betterNameFunc.addChild(func);
      }
      model.getFunktionenConf().addChild(betterNameFunc);

      storeCurrentFormDescription();
      return name;
    }
    catch (ConfigurationErrorException e)
    {
      Logger.error(e);
      return null;
    }
  }

  /**
   * Diese Methode liefert den TextFieldMaster, der für Zugriffe auf das Benutzerfeld
   * mit den Namen userFieldName zuständig ist.
   * 
   * @param userFieldName
   * @return den TextFieldMaster oder null, wenn das Benutzerfeld userFieldName nicht
   *         existiert.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private XPropertySet getUserFieldMaster(String userFieldName)
  {
    XNameAccess masters = UNO.XTextFieldsSupplier(model.doc).getTextFieldMasters();
    String elementName = "com.sun.star.text.FieldMaster.User." + userFieldName;
    if (masters.hasByName(elementName))
    {
      try
      {
        return UNO.XPropertySet(masters.getByName(elementName));
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }
    }
    return null;
  }

  /**
   * Stellt diverse Daten zur Verfügung in der Syntax "Namensraum/Name". Derzeit
   * unterstützte Namensräume sind
   * <ul>
   * <li>"User/" Werte von Benutzervariablen (vgl.
   * {@link #getUserFieldMaster(String)}</li>
   * </ul>
   * 
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  private class MyValues implements Values
  {
    public static final int MYVALUES_NAMESPACE_UNKNOWN = 0;

    public static final int MYVALUES_NAMESPACE_USER = 1;

    @Override
    public String getString(String id)
    {
      switch (namespace(id))
      {
        case MYVALUES_NAMESPACE_USER:
          return getString_User(id);
        default:
          return "";
      }
    }

    @Override
    public boolean getBoolean(String id)
    {
      switch (namespace(id))
      {
        case MYVALUES_NAMESPACE_USER:
          return getBoolean_User(id);
        default:
          return false;
      }
    }

    @Override
    public boolean hasValue(String id)
    {
      switch (namespace(id))
      {
        case MYVALUES_NAMESPACE_USER:
          return hasValue_User(id);
        default:
          return false;
      }
    }

    private int namespace(String id)
    {
      if (id.startsWith("User/")) return MYVALUES_NAMESPACE_USER;
      return MYVALUES_NAMESPACE_UNKNOWN;
    }

    private String getString_User(String id)
    {
      try
      {
        id = id.substring(id.indexOf('/') + 1);
        return getUserFieldMaster(id).getPropertyValue("Content").toString();
      }
      catch (Exception x)
      {
        return "";
      }
    }

    private boolean getBoolean_User(String id)
    {
      return getString_User(id).equalsIgnoreCase("true");
    }

    private boolean hasValue_User(String id)
    {
      try
      {
        id = id.substring(id.indexOf('/') + 1);
        return getUserFieldMaster(id) != null;
      }
      catch (Exception x)
      {
        return false;
      }
    }
  }
  
  /**
   * Nimmt ein ConfigThingy von folgender Form
   * 
   * <pre>
   * overrideFrag(
   *   (FRAG_ID 'A' NEW_FRAG_ID 'B')
   *   (FRAG_ID 'C' NEW_FRAG_ID 'D')
   *   ...
   * )
   * </pre>
   * 
   * parst es und initialisiert damit {@link #overrideFragMap}. NEW_FRAG_ID ist
   * optional und wird als leerer String behandelt wenn es fehlt.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private void parseInitialOverrideFragMap(ConfigThingy initialOverrideFragMap)
  {
    for (ConfigThingy conf : initialOverrideFragMap)
    {
      String oldFragId;
      try
      {
        oldFragId = conf.get("FRAG_ID").toString();
      }
      catch (NodeNotFoundException x)
      {
        Logger.error(L.m(
          "FRAG_ID Angabe fehlt in einem Eintrag der %1: %2\nVielleicht haben Sie die Klammern um (FRAG_ID 'A' NEW_FRAG_ID 'B') vergessen?",
          TextDocumentModel.OVERRIDE_FRAG_DB_SPALTE, conf.stringRepresentation()));
        continue;
      }

      String newFragId = "";
      try
      {
        newFragId = conf.get("NEW_FRAG_ID").toString();
      }
      catch (NodeNotFoundException x)
      {
        // NEW_FRAG_ID ist optional
      }

      try
      {
        model.setOverrideFrag(oldFragId, newFragId);
      }
      catch (OverrideFragChainException x)
      {
        Logger.error(L.m("Fehlerhafte Angabe in %1: %2",
          TextDocumentModel.OVERRIDE_FRAG_DB_SPALTE, conf.stringRepresentation()), x);
      }
    }
  }

  /**
   * Liefert die persönliche OverrideFrag-Liste des aktuell gewählten Absenders.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private ConfigThingy getInitialOverrideFragMap()
  {
    ConfigThingy overrideFragConf = new ConfigThingy("overrideFrag");
    
    String overrideFragDbSpalte = null;
    ConfigThingy overrideFragDbSpalteConf =
      WollMuxFiles.getWollmuxConf().query(TextDocumentModel.OVERRIDE_FRAG_DB_SPALTE, 1);
    try
    {
      overrideFragDbSpalte = overrideFragDbSpalteConf.getLastChild().toString();
    }
    catch (NodeNotFoundException x)
    {
      // keine OVERRIDE_FRAG_DB_SPALTE Direktive gefunden
      overrideFragDbSpalte = "";
    }

    if (!overrideFragDbSpalte.isEmpty())
    {
      try
      {
        Dataset ds = DatasourceJoiner.getDatasourceJoiner().getSelectedDatasetTransformed();
        String value = ds.get(overrideFragDbSpalte);
        if (value == null) value = "";
        overrideFragConf = new ConfigThingy("overrideFrag", value);
      }
      catch (DatasetNotFoundException e)
      {
        Logger.log(L.m("Kein Absender ausgewählt => %1 bleibt wirkungslos",
          TextDocumentModel.OVERRIDE_FRAG_DB_SPALTE));
      }
      catch (ColumnNotFoundException e)
      {
        Logger.error(L.m("%2 spezifiziert Spalte '%1', die nicht vorhanden ist",
          overrideFragDbSpalte, TextDocumentModel.OVERRIDE_FRAG_DB_SPALTE), e);
      }
      catch (IOException x)
      {
        Logger.error(L.m("Fehler beim Parsen der %2 '%1'", overrideFragDbSpalte,
          TextDocumentModel.OVERRIDE_FRAG_DB_SPALTE), x);
      }
      catch (SyntaxErrorException x)
      {
        Logger.error(L.m("Fehler beim Parsen der %2 '%1'", overrideFragDbSpalte,
          TextDocumentModel.OVERRIDE_FRAG_DB_SPALTE), x);
      }
    }

    return overrideFragConf;
  }

  /**
   * Erzeugt ein FormModel für ein einfaches Formular mit genau einem zugehörigen
   * Formulardokument.
   * 
   * @param doc
   *          Das Dokument zu dem ein FormModel erzeugt werden soll.
   * @param visible
   *          false zeigt an, dass das Dokument unsichtbar ist (und es die FormGUI
   *          auch sein sollte).
   * @return ein FormModel dem genau ein Formulardokument zugeordnet ist.
   * @throws InvalidFormDescriptorException
   */
  public FormModel createSingleDocumentFormModel(boolean visible) throws InvalidFormDescriptorException
  {
  
    // Abschnitt "Formular" holen:
    ConfigThingy formConf;
    try
    {
      formConf = getFormDescription().get("Formular");
    }
    catch (NodeNotFoundException e)
    {
      throw new InvalidFormDescriptorException(
        L.m("Kein Abschnitt 'Formular' in der Formularbeschreibung vorhanden"));
    }
  
    // Abschnitt Fenster/Formular aus wollmuxConf holen:
    ConfigThingy formFensterConf;
    try
    {
      formFensterConf =
        WollMuxFiles.getWollmuxConf().query("Fenster").query("Formular").getLastChild();
    }
    catch (NodeNotFoundException x)
    {
      formFensterConf = new ConfigThingy("");
    }
  
    return new SingleDocumentFormModel(this, formFensterConf, formConf,
      getFunctionContext(), getFunctionLibrary(), getDialogLibrary(),
      visible);
  }
}
