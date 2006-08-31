/*
* Dateiname: FormControlModelList.java
* Projekt  : WollMux
* Funktion : Verwaltet eine Liste von FormControlModels.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 07.08.2006 | BNK | Erstellung
* 29.08.2006 | BNK | kommentiert.
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import de.muenchen.allg.itd51.parser.ConfigThingy;

/**
 * Verwaltet eine Liste von FormControlModels.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormControlModelList
{
  /**
   * Die Liste der {@link FormControlModel}s.
   */
  private Vector models = new Vector();
  
  /**
   * Liste aller {@link ItemListener}, die �ber �nderungen des Listeninhalts informiert
   * werden wollen.
   */
  private List listeners = new Vector(1);
  
  /**
   * L�scht alle bestehenden FormControlModels aus der Liste.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void clear()
  {
    while (!models.isEmpty())
    {
      FormControlModel model = (FormControlModel)models.remove(models.size() - 1);
      model.hasBeenRemoved();
    }
  }
  
  /**
   * Macht aus str einen Identifier, der noch von keinem FormControlModel dieser Liste
   * verwendet wird und liefert diesen Identifier zur�ck.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String makeUniqueId(String str)
  {
    Iterator iter = models.iterator();
    int count = 0;
    while (iter.hasNext())
    {
      FormControlModel model = (FormControlModel)iter.next();
      String id = model.getId();
      if (id.startsWith(str))
      {
        if (count == 0) ++count;
        String suffix = id.substring(str.length());
        try{
          int idx = Integer.parseInt(suffix);
          if (idx >= count) count = idx + 1;
        }catch(Exception x){}
      }
    }
    
    if (count > 0) 
      return str + count;
    else
      return str;
  }
  
  /**
   * Falls idx >= 0 wird model an Index idx in die Liste eingef�gt 
   * (das Element das sich vorher an diesem Index befand hat danach Index idx+1);
   * falls idx < 0 wird model an das Ende der Liste angeh�ngt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(FormControlModel model, int idx)
  {
    if (idx < 0)
      models.add(model);
    else
      models.add(idx, model);
    
    notifyListeners(model);
  }
  
  /**
   * model wird an das Ende der Liste angeh�ngt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(FormControlModel model)
  {
    models.add(model);
    notifyListeners(model);
  }
  
  /**
   * Liefert ein ConfigThingy, dessen Wurzel ein "Fenster"-Knoten ist und alle FormControlModels
   * dieser Liste enth�lt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy export()
  {
    ConfigThingy export = new ConfigThingy("Fenster");
    ConfigThingy conf = export;
    ConfigThingy tabConf = export;
    
    int phase = 0; //0: tab start, 1: Eingabefelder, 2: Buttons
    FormControlModel currentTab = FormControlModel.createTab("Eingabe", "Reiter1");
    Iterator iter = models.iterator();
    while (iter.hasNext())
    {
      FormControlModel model = (FormControlModel)iter.next();
      if (phase == 0 && model.getType() == FormControlModel.TAB_TYPE)
        currentTab = model;
      else if (phase > 0 && model.getType() == FormControlModel.TAB_TYPE)
      {
        currentTab = model;
        conf = export;
        phase = 0;
      }
      else if (phase == 0 && model.getType() == FormControlModel.BUTTON_TYPE)
      {
        tabConf = outputTab(currentTab, export);
        conf = tabConf.add("Buttons");
        conf.addChild(model.export());
        phase = 2;
      }
      else if (phase == 1 && model.getType() == FormControlModel.BUTTON_TYPE)
      {
        conf = tabConf.add("Buttons");
        conf.addChild(model.export());
        phase = 2;
      }
      else if (phase == 2 && model.getType() == FormControlModel.BUTTON_TYPE)
      {
        conf.addChild(model.export());
      }
      else if (phase == 0)
      {
        tabConf = outputTab(currentTab, export);
        conf = tabConf.add("Eingabefelder");
        conf.addChild(model.export());
        phase = 1;
      } 
      else if (phase >= 1)
      {
        conf.addChild(model.export());
      }
    }
  
    return export;
  }
  
  /**
   * Erzeugt ein ConfigThingy f�r den Reiter tab, h�ngt es an conf an und liefert es zur�ck.
   * Das erzeugte ConfigThingy hat folgenden Aufbau: <br>
   * ReiterId(TITLE "title" CLOSEACTION "action" TIP "tip" HOTKEY "hotkey")
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private ConfigThingy outputTab(FormControlModel tab, ConfigThingy conf)
  {
    conf = conf.add(tab.getId());
    conf.add("TITLE").add(tab.getLabel());
    conf.add("CLOSEACTION").add(tab.getAction());
    conf.add("TIP").add(tab.getTooltip());
    char hotkey = tab.getHotkey();
    if (hotkey > 0)
      conf.add("HOTKEY").add(""+hotkey);
    
    return conf;
  }

  /**
   * listener wird �ber �nderungen der Liste informiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addListener(ItemListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }

  /**
   * Benachrichtigt alle ItemListener �ber das Hinzuf�gen von model zur Liste.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void notifyListeners(FormControlModel model)
  {
    Iterator iter = listeners.iterator();
    while (iter.hasNext())
    {
      ItemListener listener = (ItemListener)iter.next();
      listener.itemAdded(model);
    }
  }
  
  /**
   * Interface f�r Klassen, die interessiert sind, zu erfahren, wenn sich die Liste
   * �ndert.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ItemListener
  {
    /**
     * Wird aufgerufen nachdem model zur Liste hinzugef�gt wurde.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void itemAdded(FormControlModel model);
  }
  

}
