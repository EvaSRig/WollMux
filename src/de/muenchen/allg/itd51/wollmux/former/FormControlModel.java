/*
* Dateiname: FormControlModel.java
* Projekt  : WollMux
* Funktion : Repr�sentiert ein Formularsteuerelement.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 07.08.2006 | BNK | Erstellung
* 29.08.2006 | BNK | kommentiert
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import de.muenchen.allg.itd51.parser.ConfigThingy;

/**
 * Repr�sentiert ein Formularsteuerelement.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormControlModel
{
  /**
   * Die *_TYPE-Konstanten haben eine Doppelfunktion. Einerseits sind sie Typ-IDs, die mit
   *  == verglichen werden, andererseits wird bei der Ausgabe der String direkt als TYPE-Wert
   *  verwendet. Das ist etwas unsauber, aber jeder Code muss ein paar kleine Makel haben ;-) 
   */
  public static final String COMBOBOX_TYPE = "combobox";
  public static final String TEXTFIELD_TYPE = "textfield";
  public static final String TEXTAREA_TYPE = "textarea";
  public static final String TAB_TYPE = "tab";
  public static final String SEPARATOR_TYPE = "separator";
  public static final String GLUE_TYPE = "glue";
  public static final String CHECKBOX_TYPE = "checkbox";
  public static final String BUTTON_TYPE = "button";
  /**
   * Wird gesetzt, wenn versucht wird, einen TYPE einzustellen, der nicht bekannt ist.
   */
  public static final String UNKNOWN_TYPE = "unknown";
  
  /**
   * Signalisiert, dass dem Element keine ACTION zugeordnet ist.
   */
  public static final String NO_ACTION = "";
  
  /**
   * Attribut ID f�r das Attribut "LABEL".
   */
  public static final int LABEL_ATTR = 0;
  
  /** LABEL. */
  private String label;
  /** TYPE. Muss eine der *_TYPE Konstanten sein, da mit == verglichen wird. */
  private String type;
  /** ID. */
  private String id;
  /** ACTION. */
  private String action = NO_ACTION;
  /** DIALOG. */
  private String dialog = "";
  /** TIP. */
  private String tooltip = "";
  /** HOTKEY. */
  private char hotkey = 0;
  /** VALUES. */
  private List items = new Vector(0);
  /** EDIT. */
  private boolean editable = false;
  /** READONLY. */
  private boolean readonly = false;
  /** GROUPS. */
  private Set groups = new HashSet();
  /** LINES. */
  private int lines = 4;
  /** MINSIZE. */
  private int minsize = 0;
  /** PLAUSI (incl, PLAUSI-Knoten). */
  private ConfigThingy plausi = new ConfigThingy("PLAUSI");
  /** AUTOFILL (incl, AUTOFILL-Knoten). */
  private ConfigThingy autofill = new ConfigThingy("AUTOFILL");
  
  /**
   * Die {@link ModelChangeListener}, die �ber �nderungen dieses Models informiert werden wollen.
   */
  private List listeners = new Vector(1);
  
  /**
   * Parst conf als Steuerelement und erzeugt ein entsprechendes FormControlModel.
   * @param conf direkter Vorfahre von "TYPE", "LABEL", usw.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormControlModel(ConfigThingy conf)
  {
    label = "Steuerelement";
    type = "textfield";
    id = "";
    
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy attr = (ConfigThingy)iter.next();
      String name = attr.getName();
      String str = attr.toString();
      if (name.equals("LABEL")) label = str;
      else if (name.equals("TYPE")) setType(str);
      else if (name.equals("ID")) id = str;
      else if (name.equals("ACTION")) action = str;
      else if (name.equals("DIALOG")) dialog = str;
      else if (name.equals("TIP")) tooltip = str;
      else if (name.equals("HOTKEY")) hotkey = str.length() > 0 ? str.charAt(0) : 0;
      else if (name.equals("EDIT")) editable = str.equalsIgnoreCase("true");
      else if (name.equals("READONLY")) readonly = str.equalsIgnoreCase("true");
      else if (name.equals("LINES")) try{lines = Integer.parseInt(str); }catch(Exception x){}
      else if (name.equals("MINSIZE")) try{minsize = Integer.parseInt(str); }catch(Exception x){}
      else if (name.equals("VALUES")) items = parseValues(attr);
      else if (name.equals("GROUPS")) groups = parseGroups(attr);
      else if (name.equals("PLAUSI")) plausi = new ConfigThingy(attr);
      else if (name.equals("AUTOFILL")) autofill = new ConfigThingy(attr);
    }
  }
  
  /**
   * Liefert eine Liste, die die String-Werte aller Kinder von conf enth�lt. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private List parseValues(ConfigThingy conf)
  {
    Vector list = new Vector(conf.count());
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      list.add(iter.next().toString());
    }
    return list;
  }
  
  /**
   * Liefert eine Menge, die die String-Werte aller Kinder von conf enth�lt. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private Set parseGroups(ConfigThingy conf)
  {
    HashSet set = new HashSet(conf.count());
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      set.add(iter.next().toString());
    }
    return set;
  }
  
  /**
   * Erzeugt ein neues FormControlModel mit den gegebenen Parametern. Alle anderen
   * Eigenschaften erhalten Default-Werte (normalerweise der leere String).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel(String label, String type, String id)
  {
    this.label = label;
    this.type = type;
    this.id = id;
  }
  
  /**
   * Liefert ein FormControlModel, das eine Checkbox darstellt mit gegebenem LABEL label und
   * ID id.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static FormControlModel createCheckbox(String label, String id)
  {
    return new FormControlModel(label, CHECKBOX_TYPE, id);
  }
  
  /**
   * Liefert ein FormControlModel, das ein Textfeld darstellt mit gegebenem LABEL label und
   * ID id.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static FormControlModel createTextfield(String label, String id)
  {
    FormControlModel model = new FormControlModel(label, TEXTFIELD_TYPE, id);
    model.editable = true;
    return model;
  }
  
  /**
   * Liefert ein FormControlModel, das eine Combobox darstellt mit gegebenem LABEL label und
   * ID id.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static FormControlModel createComboBox(String label, String id, String[] items)
  {
    FormControlModel model = new FormControlModel(label, COMBOBOX_TYPE, id);
    model.items = new Vector(Arrays.asList(items));
    return model;
  }
  
  /**
   * Liefert ein FormControlModel, das den Beginn eines neuen Tabs darstellt mit gegebenem 
   * LABEL label und ID id.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static FormControlModel createTab(String label, String id)
  {
    FormControlModel model = new FormControlModel(label, TAB_TYPE, id);
    model.action = "abort";
    return model;
  }
  
  /**
   * Liefert die ID dieses FormControlModels.
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getId()
  {
    return id;
  }
  
  /**
   * Liefert den TYPE dieses FormControlModels, wobei immer eine der 
   * {@link #COMBOBOX_TYPE *_TYPE Konstanten} geliefert wird, so dass == verglichen werden
   * kann.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getType()
  {
    return type;
  }
  
  /**
   * Liefert das LABEL dieses FormControlModels.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getLabel()
  {
    return label;
  }
  
  /**
   * Liefert die ACTION dieses FormControlModels. Falls keine ACTION gesetzt ist wird die
   * Konstante {@link #NO_ACTION} geliefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getAction()
  {
    return action;
  }
  
  /**
   * Liefert den DIALOG dieses FormControlModels.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getDialog()
  {
    return dialog;
  }
  
  /**
   * Liefert den TIP dieses FormControlModels.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getTooltip()
  {
    return tooltip;
  }
  
  /**
   * Liefert den HOTKEY dieses FormControlModels.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public char getHotkey()
  {
    return hotkey; 
  }
  
  /**
   * Liefert das READONLY-Attribut dieses FormControlModels.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean getReadonly()
  {
    return readonly; 
  }
  
  /**
   * Liefert das EDIT-Attribut dieses FormControlModels.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean getEditable()
  {
    return editable; 
  }
  
  /**
   * Liefert das LINES-Attribut dieses FormControlModels.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int getLines()
  {
    return lines; 
  }
  
  /**
   * Liefert das MINSIZE-Attribut dieses FormControlModels.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int getMinsize()
  {
    return minsize; 
  }
  
  /**
   * Liefert die Liste der VALUES-Werte dieses FormControlModels.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public List getItems()
  {
    return items; 
  }
  
  /**
   * Liefert die Menge der GROUPS-Werte dieses FormControlModels.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Set getGroups()
  {
    return groups; 
  }
  
  /**
   * Ersetzt den aktuellen AUTOFILL durch conf. ACHTUNG! Es wird keine Kopie von conf
   * gemacht, sondern direkt eine Referenz auf conf integriert.
   * @param conf der "AUTOFILL"-Knoten.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setAutofill(ConfigThingy conf)
  {
    autofill = conf;
  }
  
  /**
   * Setzt das ACTION-Attribut auf action, wobei ein leerer String zu {@link #NO_ACTION}
   * konvertiert wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setAction(String action)
  {
    if (action.length() == 0) action = NO_ACTION;
    this.action = action;
  }
  
  /**
   * Setzt das LABEL-Attribut.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setLabel(String label)
  {
    this.label = label;
    notifyListeners(LABEL_ATTR, label);
  }
  
  /**
   * Setzt das TIP-Attribut.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setTooltip(String tooltip)
  {
    this.tooltip = tooltip;
  }
  
  /**
   * Setzt das HOTKEY-Attribut.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setHotkey(char hotkey)
  {
    this.hotkey = hotkey; 
  }
  
  /**
   * Setzt das READONLY-Attribut.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setReadonly(boolean readonly)
  {
    this.readonly = readonly;
  }
  
  /**
   * Setzt das EDIT-Attribut.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setEditable(boolean editable)
  {
    this.editable = editable; 
  }
  
  /**
   * Setzt das LINES-Attribut.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setLines(int lines)
  {
    this.lines = lines; 
  }
  
  /**
   * Setzt das MINSIZE-Attribut.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setMinsize(int minsize)
  {
    this.minsize = minsize; 
  }
  
  /**
   * Setzt das TYPE-Attribut. Dabei wird der �bergebene String in eine der
   * {@link #COMBOBOX_TYPE *_TYPE-Konstanten} �bersetzt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setType(String type)
  {
    if (type.equals(COMBOBOX_TYPE)) this.type = COMBOBOX_TYPE;
    else if (type.equals(TEXTFIELD_TYPE)) this.type = TEXTFIELD_TYPE;
    else if (type.equals(TEXTAREA_TYPE)) this.type = TEXTAREA_TYPE;
    else if (type.equals(TAB_TYPE)) this.type = TAB_TYPE;
    else if (type.equals(SEPARATOR_TYPE)) this.type = SEPARATOR_TYPE;
    else if (type.equals(GLUE_TYPE)) this.type = GLUE_TYPE;
    else if (type.equals(CHECKBOX_TYPE)) this.type = CHECKBOX_TYPE;
    else if (type.equals(BUTTON_TYPE)) this.type = BUTTON_TYPE;
    else this.type = UNKNOWN_TYPE;
  }
  
  /**
   * Liefert ein ConfigThingy, das dieses FormControlModel darstellt. Das ConfigThingy wird
   * immer neu erzeugt, kann vom Aufrufer also frei verwendet werden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy export()
  {
    ConfigThingy conf = new ConfigThingy("");
    conf.add("LABEL").add(getLabel());
    conf.add("TYPE").add(getType().toLowerCase());
    conf.add("ID").add(getId());
    conf.add("TIP").add(getTooltip());
    conf.add("READONLY").add(""+getReadonly());
    conf.add("EDIT").add(""+getEditable());
    conf.add("LINES").add(""+getLines());
    conf.add("MINSIZE").add(""+getMinsize());
    if (getAction().length() > 0) conf.add("ACTION").add(""+getAction());
    if (getDialog().length() > 0) conf.add("DIALOG").add(""+getDialog());
    if (getHotkey() > 0)
      conf.add("HOTKEY").add(""+getHotkey());
    
    List items = getItems();
    if (items.size() > 0)
    {
      ConfigThingy values = conf.add("VALUES");
      Iterator iter = items.iterator();
      while (iter.hasNext())
      {
        values.add(iter.next().toString());
      }
    }
    
    Set groups = getGroups();
    if (groups.size() > 0)
    {
      ConfigThingy grps = conf.add("GROUPS");
      Iterator iter = items.iterator();
      while (iter.hasNext())
      {
        grps.add(iter.next().toString());
      }
    }
    
    if (plausi.count() > 0)
      conf.addChild(new ConfigThingy(plausi));
    if (autofill.count() > 0)
      conf.addChild(new ConfigThingy(autofill));
    
    return conf; 
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
   * Benachrichtigt alle auf diesem Model registrierten Listener, dass das Model aus
   * seinem Container entfernt wurde. ACHTUNG! Darf nur von einem entsprechenden Container
   * aufgerufen werden, der das Model enth�lt.
   * @param index der Index an dem sich das Model in seinem Container befand.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
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
   * listener wird �ber �nderungen des FormControlModels informiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addListener(ModelChangeListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }
  
  /**
   * Interface f�r Listener, die �ber �nderungen eines FormControlModels informiert
   * werden wollen. 
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ModelChangeListener
  {
    /**
     * Wird aufgerufen wenn ein Attribut des Models sich ge�ndert hat. 
     * @param model das FormControlModel, das sich ge�ndert hat.
     * @param attributeId eine der {@link FormControlModel#LABEL_ATTR *_ATTR-Konstanten}.
     * @param newValue der neue Wert des Attributs. Numerische Attribute werden als Integer �bergeben.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void attributeChanged(FormControlModel model, int attributeId, Object newValue);
    
    /**
     * Wird aufgerufen, wenn model aus seinem Container entfernt wird (und damit
     * in keiner View mehr angezeigt werden soll).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void modelRemoved(FormControlModel model);
  }
}
