/*
* Dateiname: InsertionModel.java
* Projekt  : WollMux
* Funktion : Stellt eine Einf�gestelle im Dokument (insertValue oder insertFormValue) dar.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 06.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former.insertion;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.text.XBookmarksSupplier;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.Bookmark;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModel;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelection;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionAccess;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionProvider;
import de.muenchen.allg.itd51.wollmux.former.function.ParamValue;

/**
 * Stellt eine Einf�gestelle im Dokument (insertValue oder insertFormValue) dar.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class InsertionModel
{
  /**
   * Pattern zum Erkennen von insertValue und insertFormValue-Bookmarks.
   */
  public static final Pattern INSERTION_BOOKMARK = Pattern.compile("\\A\\s*(WM\\s*\\(.*CMD\\s*'((insertValue)|(insertFormValue))'.*\\))\\s*\\d*\\z");
  
  /**
   * Attribut-ID-Konstante f�r {@link ModelChangeListener#attributeChanged(InsertionModel, int, Object)}.
   */
  public static final int ID_ATTR = 0;
  
  /** 
   * Konstante f�r {@link #sourceType}, die angibt, dass die Daten f�r die Einf�gung
   * aus einer externen Datenquelle kommen. 
   */
  private static final int DATABASE_TYPE = 0;
  
  /** 
   * Konstante f�r {@link #sourceType}, die angibt, dass die Daten f�r die Einf�gung
   * aus dem Formular kommen. 
   */
  private static final int FORM_TYPE = 1;
  
  /**
   * Gibt an, um woher die Einf�gung ihre Daten bezieht.
   * @see #FORM_TYPE
   * @see #DATABASE_TYPE
   */
  private int sourceType = FORM_TYPE;
  
  /**
   * DB_SPALTE oder ID je nach {@link #sourceType}.
   */
  private String dataId = "";
  
  /**
   * Das Bookmarks, das diese Einf�gestelle umschlie�t.
   */
  private Bookmark bookmark;
  
  /**
   * Die TRAFO f�r diese Einf�gung.
   */
  private FunctionSelection trafo;
  
  /**
   * Die {@link ModelChangeListener}, die �ber �nderungen dieses Models informiert werden wollen.
   */
  private List listeners = new Vector(1);
  
  /**
   * Der FormularMax4000 zu dem dieses Model geh�rt.
   */
  private FormularMax4000 formularMax4000;

  /**
   * Erzeugt ein neues InsertionModel f�r das Bookmark mit Namen bookmarkName, das bereits
   * im Dokument vorhanden sein muss.
   * @param doc das Dokument in dem sich das Bookmark befindet
   * @param funcSelections ein FunctionSelectionProvider, der f�r das TRAFO Attribut eine passende
   *        FunctionSelection liefern kann.
   * @param formularMax4000 Der FormularMax4000 zu dem dieses InsertionModel geh�rt.
   * @throws SyntaxErrorException wenn bookmarkName nicht korrekte ConfigThingy-Syntax hat oder
   *         kein korrektes Einf�gekommando ist.
   * @throws NoSuchElementException wenn ein Bookmark dieses Namens in doc nicht existiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public InsertionModel(String bookmarkName, XBookmarksSupplier doc, FunctionSelectionProvider funcSelections, FormularMax4000 formularMax4000) throws SyntaxErrorException, NoSuchElementException
  {
    this.formularMax4000 = formularMax4000;
    bookmark = new Bookmark(bookmarkName,doc);
    String confStr = bookmarkName.replaceAll("\\d*\\z",""); //eventuell vorhandene Ziffern am Ende l�schen
    URL url = null;
    try{
      url = new URL("file:///");
    }catch(MalformedURLException x){}
    
    ConfigThingy conf;
    try{
      conf = new ConfigThingy("INSERT", url, new StringReader(confStr));
    }catch(IOException x)
    {
      throw new SyntaxErrorException(x);
    }
    
    String cmd = conf.query("CMD").toString();
    if (cmd.equals("insertValue"))
    {
      ConfigThingy dbSpalteConf = conf.query("DB_SPALTE");
      if (dbSpalteConf.count() == 0) throw new SyntaxErrorException();
      dataId = dbSpalteConf.toString();
      sourceType = DATABASE_TYPE;
    } else if (cmd.equals("insertFormValue"))
    {
      ConfigThingy idConf = conf.query("ID");
      if (idConf.count() == 0) throw new SyntaxErrorException();
      dataId = idConf.toString();
      sourceType = FORM_TYPE;
    } else 
      throw new SyntaxErrorException();
    
    ConfigThingy trafoConf = conf.query("TRAFO");
    if (trafoConf.count() == 0)
      this.trafo = new FunctionSelection();
    else
    {
      String functionName = trafoConf.toString();
      this.trafo = funcSelections.getFunctionSelection(functionName);
    }
  }
  
  /**
   * L�sst dieses {@link InsertionModel}s sein zugeh�riges Bookmark updaten. 
   * @param mapFunctionNameToConfigThingy bildet einen Funktionsnamen auf ein ConfigThingy ab, 
   *        dessen Wurzel der Funktionsname ist und dessen Inhalt eine Funktionsdefinition.
   *        Wenn diese Einf�gung mit einer TRAFO versehen ist, wird f�r das Aktualisieren des
   *        Bookmarks ein Funktionsname generiert, der noch nicht in dieser Map vorkommt
   *        und ein Mapping f�r diese Funktion wird in die Map eingef�gt.
   * @return false, wenn ein update nicht m�glich ist. In dem Fall wird das entsprechende
   *         Bookmark entfernt und dieses InsertionModel sollte nicht weiter verwendet werden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public boolean updateDocument(Map mapFunctionNameToConfigThingy)
  {
    ConfigThingy conf = new ConfigThingy("WM");
    String cmd = "insertValue";
    String idType = "DB_SPALTE";
    if (sourceType == FORM_TYPE) 
    {
      cmd = "insertFormValue";
      idType = "ID";
    }
    
    conf.add("CMD").add(cmd);
    conf.add(idType).add(getDataID());
    
    if (!trafo.isNone())
    {
        // Falls eine externe Funktion referenziert wird, ohne dass irgendwelche
        // ihrer Parameter gebunden wurden, dann nehmen wir direkt den
        // Original-Funktionsnamen f�r das TRAFO-Attribut ...
      if (trafo.isReference() && !trafo.hasSpecifiedParameters())
      {
        conf.add("TRAFO").add(trafo.getFunctionName());
      }
      else //  ... ansonsten m�ssen wir eine neue Funktion machen.
      {
        int count = 1;
        String funcName;
        do{
          funcName = "FM4000AutoGeneratedTrafo" + (count++);
        } while(mapFunctionNameToConfigThingy.containsKey(funcName));
        
        conf.add("TRAFO").add(funcName);
        mapFunctionNameToConfigThingy.put(funcName, trafo.export(funcName));
      }
    }
    
    String newBookmarkName = conf.stringRepresentation(false, '\'');
    return bookmark.rename(newBookmarkName) != Bookmark.BROKEN;
  }
  
  /**
   * Liefert je nach Typ der Einf�gung das DB_SPALTE oder ID Attribut.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getDataID()
  {
    return dataId;
  }
  
  /**
   * �ndert je nach Type der Einf�gung DB_SPALTE oder ID Attribut auf den Wert newId.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setDataID(String newId)
  {
    dataId = newId;
    notifyListeners(ID_ATTR, newId);
    formularMax4000.documentNeedsUpdating();
  }
  
  /**
   * Benachrichtigt alle auf diesem Model registrierten Listener, dass das Model aus
   * seinem Container entfernt wurde. ACHTUNG! Darf nur von einem entsprechenden Container
   * aufgerufen werden, der das Model enth�lt.
   * @param index der Index an dem sich das Model in seinem Container befand.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED */
  public void hasBeenRemoved()
  {
    Iterator iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = (ModelChangeListener)iter.next();
      listener.modelRemoved(this);
    }
  }
  
  /**
   * Ruft f�r jeden auf diesem Model registrierten {@link ModelChangeListener} die Methode
   * {@link ModelChangeListener#attributeChanged(FormControlModel, int, Object)} auf. 
   */
  private void notifyListeners(int attributeId, Object newValue)
  {
    Iterator iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = (ModelChangeListener)iter.next();
      listener.attributeChanged(this, attributeId, newValue);
    }
  }
  
  /**
   * listener wird �ber �nderungen des Models informiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addListener(ModelChangeListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }
  
  /**
   * Liefert ein Interface zum Zugriff auf die TRAFO dieses Objekts.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public FunctionSelectionAccess getTrafoAccess()
  {
    return new MyTrafoAccess();
  }
  
  /**
   * Interface f�r Listener, die �ber �nderungen eines Models informiert
   * werden wollen. 
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ModelChangeListener
  {
    /**
     * Wird aufgerufen wenn ein Attribut des Models sich ge�ndert hat. 
     * @param model das InsertionModel, das sich ge�ndert hat.
     * @param attributeId eine der {@link InsertionModel#ID_ATTR Attribut-ID-Konstanten}.
     * @param newValue der neue Wert des Attributs. Numerische Attribute werden als Integer �bergeben.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void attributeChanged(InsertionModel model, int attributeId, Object newValue);
    
    
    /**
     * Wird aufgerufen, wenn model aus seinem Container entfernt wird (und damit
     * in keiner View mehr angezeigt werden soll).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void modelRemoved(InsertionModel model);
  }
  
  /**
   * Diese Klasse leitet Zugriffe weiter an das Objekt {@link InsertionModel#trafo}. Bei
   * �ndernden Zugriffen wird auch noch der FormularMax4000 benachrichtigt, dass das Dokument
   * geupdatet werden muss. Im Prinzip m�sste korrekterweise ein
   * �ndernder Zugriff auf trafo auch einen Event an die ModelChangeListener schicken.
   * Allerdings ist dies derzeit nicht implementiert,
   * weil es derzeit genau eine View gibt f�r die Trafo, so dass konkurrierende �nderungen
   * gar nicht m�glich sind.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyTrafoAccess implements FunctionSelectionAccess
  {
    public boolean isReference() { return trafo.isReference();}
    public boolean isExpert()    { return trafo.isExpert(); }
    public boolean isNone()      { return trafo.isNone(); }
    public String getFunctionName()      { return trafo.getFunctionName();}
    public ConfigThingy getExpertFunction() { return trafo.getExpertFunction(); }

    public void setParameterValues(Map mapNameToParamValue)
    {
      trafo.setParameterValues(mapNameToParamValue);
      formularMax4000.documentNeedsUpdating();
    }

    public void setFunction(String functionName, String[] paramNames)
    {
      trafo.setFunction(functionName, paramNames);
      formularMax4000.documentNeedsUpdating();
    }
    
    public void setExpertFunction(ConfigThingy funConf)
    {
      trafo.setExpertFunction(funConf);
      formularMax4000.documentNeedsUpdating();
    }
    public void setParameterValue(String paramName, ParamValue paramValue)
    {
      trafo.setParameterValue(paramName, paramValue);
      formularMax4000.documentNeedsUpdating();
    }
    public String[] getParameterNames()
    {
      return trafo.getParameterNames();
    }
    public boolean hasSpecifiedParameters()
    {
      return trafo.hasSpecifiedParameters();
    }
    public ParamValue getParameterValue(String paramName)
    {
      return trafo.getParameterValue(paramName);
    }
    
  }
  
}
