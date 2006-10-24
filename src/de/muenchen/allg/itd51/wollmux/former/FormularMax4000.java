/*
* Dateiname: FormularMax4000.java
* Projekt  : WollMux
* Funktion : Stellt eine GUI bereit zum Bearbeiten einer WollMux-Formularvorlage.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 03.08.2006 | BNK | Erstellung
* 08.08.2006 | BNK | Viel Arbeit reingesteckt.
* 28.08.2006 | BNK | kommentiert
* 31.08.2006 | BNK | Code-Editor-Fenster wird jetzt in korrekter Gr��e dargestellt
*                  | Das Hauptfenster passt sein Gr��e an, wenn Steuerelemente dazukommen oder verschwinden
* 06.09.2006 | BNK | Hoch und Runterschieben funktionieren jetzt.
* 19.10.2006 | BNK | Quelltexteditor nicht mehr in einem eigenen Frame
* 20.10.2006 | BNK | R�ckschreiben ins Dokument erfolgt jetzt automatisch.
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import com.sun.star.container.XNameAccess;
import com.sun.star.document.XDocumentInfo;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFramesSupplier;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.FormDescriptor;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.Container;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.DropdownFormControl;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.InsertionBookmark;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.TextRange;
import de.muenchen.allg.itd51.wollmux.former.DocumentTree.Visitor;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModel;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModelList;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelection;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionProvider;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModelList;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Stellt eine GUI bereit zum Bearbeiten einer WollMux-Formularvorlage.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormularMax4000
{
  public static final String STANDARD_TAB_NAME = "Reiter";

  /**
   * Regex f�r Test ob String mit Buchstabe oder Underscore beginnt.
   * ACHTUNG! Das .* am Ende ist notwendig, da String.matches() immer den
   * ganzen String testet.
   */
  private static final String STARTS_WITH_LETTER_RE = "^[a-zA-Z_].*";
  
  /**
   * Pattern zum Erkennen der Bookmarks, die {@link #deForm(XTextDocument)} entfernen soll.
   */
  public static final Pattern BOOKMARK_KILL_PATTERN = Pattern.compile("(\\A\\s*(WM\\s*\\(.*CMD\\s*'((form)|(setGroups)|(insertFormValue))'.*\\))\\s*\\d*\\z)"+
                                                                     "|(\\A\\s*(WM\\s*\\(.*CMD\\s*'(setType)'.*'formDocument'\\))\\s*\\d*\\z)"+
                                                                     "|(\\A\\s*(WM\\s*\\(.*'formDocument'.*CMD\\s*'(setType)'.*\\))\\s*\\d*\\z)"
                                                                     );

  /**
   * Der Standard-Formulartitel, solange kein anderer gesetzt wird.
   */
  private static final String GENERATED_FORM_TITLE = "Generiert durch FormularMax 4000";

  /**
   * Maximale Anzahl Zeichen f�r ein automatisch generiertes Label.
   */
  private static final int GENERATED_LABEL_MAXLENGTH = 30;
  
  /**
   * Wird als Label gesetzt, falls kein sinnvolles Label automatisch generiert werden
   * konnte.
   */
  private static final String NO_LABEL = "";
  
  /**
   * Wird tempor�r als Label gesetzt, wenn kein Label ben�tigt wird, weil es sich nur um
   * eine Einf�gestelle handelt, die nicht als Formularsteuerelement erfasst werden soll.
   */
  private static final String INSERTION_ONLY = "<<InsertionOnly>>";
  
  /**
   * URL des Quelltexts f�r den Standard-Empf�ngerauswahl-Tab.
   */
  private final URL EMPFAENGER_TAB_URL = this.getClass().getClassLoader().getResource("data/empfaengerauswahl_controls.conf");
  
  /**
   * URL des Quelltexts f�r die Standardbuttons f�r einen mittleren Tab.
   */
  private final URL STANDARD_BUTTONS_MIDDLE_URL = this.getClass().getClassLoader().getResource("data/standardbuttons_mitte.conf");
  
  /**
   * URL des Quelltexts f�r die Standardbuttons f�r den letzten Tab.
   */
  private final URL STANDARD_BUTTONS_LAST_URL = this.getClass().getClassLoader().getResource("data/standardbuttons_letztes.conf");
  
  /**
   * Beim Import neuer Formularfelder oder Checkboxen schaut der FormularMax4000 nach
   * speziellen Hinweisen/Namen/Eintr�gen, die diesem Muster entsprechen. 
   * Diese Zusatzinformationen werden herangezogen um Labels, IDs und andere Informationen zu
   * bestimmen.
   * 
   * Eingabefeld: Als "Hinweis" kann    "Label<<ID>>" angegeben werden und wird beim Import
   *              entsprechend ber�cksichtigt. Wird nur "<<ID>>" angegeben, so markiert das
   *              Eingabefeld eine reine Einf�gestelle (insertValue oder insertContent) und
   *              beim Import wird daf�r kein Formularsteuerelement erzeugt. Wird ID
   *              ein "glob:" vorangestellt, so wird gleich ein insertValue-Bookmark
   *              erstellt.
   * 
   * Eingabeliste/Dropdown: Als "Name" kann "Label<<ID>>" angegeben werden und wird beim
   *                        Import ber�cksichtigt.
   *                        Als Spezialeintrag in der Liste kann "<<Freitext>>" eingetragen werden
   *                        und signalisiert dem FM4000, dass die ComboBox im Formular
   *                        auch die Freitexteingabe erlauben soll.
   *                        Wie bei Eingabefeldern auch ist die Angabe "<<ID>>" ohne Label
   *                        m�glich und signalisiert, dass es sich um eine reine Einf�gestelle
   *                        handelt, die kein Formularelement erzeugen soll.
   * 
   * Checkbox: Bei Checkboxen kann als "Hilfetext" "Label<<ID>>" angegeben werden und wird
   *           beim Import entsprechend ber�cksichtigt.
   *           
   * Technischer Hinweis: Auf dieses Pattern getestet wird grunds�tzlich der String, der von
   * {@link DocumentTree.FormControl#getDescriptor()} geliefert wird.
   * 
   */
  private static final Pattern MAGIC_DESCRIPTOR_PATTERN = Pattern.compile("\\A(.*)<<(.*)>>\\z");
  
  /**
   * Pr�fix zur Markierung von IDs der magischen Deskriptor-Syntax um anzuzeigen, dass
   * ein insertValue anstatt eines insertFormValue erzeugt werden soll.
   */
  private static final String GLOBAL_PREFIX = "glob:";

  /**
   * ActionListener f�r Buttons mit der ACTION "abort". 
   */
  private ActionListener actionListener_abort = new ActionListener()
     { public void actionPerformed(ActionEvent e){ abort(); } };

  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;

  /**
   * Falls nicht null wird dieser Listener aufgerufen nachdem der FM4000
   * geschlossen wurde.
   */
  private ActionListener abortListener = null;
  
  /**
   * Das Haupt-Fenster des FormularMax4000.
   */
  private JFrame myFrame;
  
  /**
   * Oberster Container der FM4000 GUI-Elemente. Wird direkt in die ContentPane von myFrame
   * gesteckt.
   */
  private JSplitPane mainContentPanel;
  
  /**
   * Oberster Container f�r den Quelltexteditor. Wird direkt in die ContentPane von myFrame
   * gesteckt. 
   */
  private JPanel editorContentPanel;
  
  /**
   * Der �bercontainer f�r die linke H�lfte des FM4000.
   */
  private LeftPanel leftPanel;
  
  /**
   * Der Titel des Formulars.
   */
  private String formTitle = GENERATED_FORM_TITLE;
  
  /**
   * Das Dokument, an dem dieser FormularMax 4000 h�ngt.
   */
  private XTextDocument doc;
  
  /**
   * Verwaltet die FormControlModels dieses Formulars.
   */
  private FormControlModelList formControlModelList;
  
  /**
   * Verwaltet die {@link InsertionModel}s dieses Formulars.
   */
  private InsertionModelList insertionModelList;
  
  /**
   * Wird verwendet f�r das Auslesen und Zur�ckspeichern der Formularbeschreibung.
   */
  private FormDescriptor formDescriptor;
  
  /**
   * Funktionsbibliothek, die globale Funktionen zur Verf�gung stellt.
   */
  private FunctionLibrary functionLibrary;
  
  /**
   * Verantwortlich f�r das �bersetzen von TRAFO, PLAUSI und AUTOFILL in
   * {@link FunctionSelection}s.
   */
  private FunctionSelectionProvider functionSelectionProvider;
  
  /**
   * Der globale Broadcast-Kanal wird f�r Nachrichten verwendet, die verschiedene permanente
   * Objekte erreichen m�ssen, die aber von (transienten) Objekten ausgehen, die mit diesen 
   * globalen Objekten
   * wegen des Ausuferns der Verbindungen nicht in einer Beziehung stehen sollen. Diese Liste
   * enth�lt alle {@link BroadcastListener}, die auf dem globalen Broadcast-Kanal horchen. 
   * Dies d�rfen nur
   * permanente Objekte sein, d.h. Objekte deren Lebensdauer nicht vor Beenden des
   * FM4000 endet. 
   */
  private List broadcastListeners = new Vector();

  /**
   * Wird auf myFrame registriert, damit zum Schlie�en des Fensters abort() aufgerufen wird.
   */
  private MyWindowListener oehrchen;

  /**
   * Die Haupt-Men�leiste des FM4000.
   */
  private JMenuBar mainMenuBar;
  
  /**
   * Die Men�leiste, die angezeigt wird wenn der Quelltexteditor offen ist.
   */
  private JMenuBar editorMenuBar;

  /**
   * Der Quelltexteditor.
   */
  private JEditorPane editor;
  
  /**
   * Wird bei jeder �nderung von Formularaspekten gestartet, um nach einer Verz�gerung die
   * �nderungen in das Dokument zu �bertragen.
   */
  private Timer writeChangesTimer;
  
  /**
   * Sendet die Nachricht b an alle Listener, die auf dem globalen Broadcast-Kanal registriert
   * sind.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED*/
  public void broadcast(Broadcast b)
  {
    Iterator iter = broadcastListeners.iterator();
    while (iter.hasNext())
    {
      b.sendTo((BroadcastListener)iter.next());
    }
  }
  
  /**
   * listener wird �ber globale {@link Broadcast}s informiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED*/
  public void addBroadcastListener(BroadcastListener listener)
  {
    if (!broadcastListeners.contains(listener))
      broadcastListeners.add(listener);
  }
  
  /**
   * Wird bei jeder �nderung einer internen Datenstruktur aufgerufen, die ein Updaten des
   * Dokuments erforderlich macht um persistent zu werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void documentNeedsUpdating()
  {
    writeChangesTimer.restart();
  }
  
  /**
   * Startet eine Instanz des FormularMax 4000 f�r das Dokument doc.
   * @param abortListener (falls nicht null) wird aufgerufen, nachdem der FormularMax 4000 geschlossen wurde.
   * @param funcLib Funktionsbibliothek, die globale Funktionen zur Verf�gung stellt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormularMax4000(XTextDocument doc, ActionListener abortListener, FunctionLibrary funcLib)
  {
    this.doc = doc;
    this.abortListener = abortListener;
    this.functionLibrary = funcLib;
    initFormDescriptor(doc);
    
    //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{createGUI();}catch(Exception x){Logger.error(x);};
        }
      });
    }
    catch(Exception x) {Logger.error(x);}
  }
  
  private void createGUI()
  {
    Common.setLookAndFeelOnce();
    
    
    formControlModelList = new FormControlModelList(this);
    insertionModelList = new InsertionModelList();
    
    //  Create and set up the window.
    myFrame = new JFrame("FormularMax 4000");
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    oehrchen = new MyWindowListener();
    //der WindowListener sorgt daf�r, dass auf windowClosing mit abort reagiert wird
    myFrame.addWindowListener(oehrchen);
    
    leftPanel = new LeftPanel(insertionModelList, formControlModelList, this);
    RightPanel rightPanel = new RightPanel(insertionModelList, formControlModelList, functionLibrary, this);
    
    mainContentPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel.JComponent(), rightPanel.JComponent());
    myFrame.getContentPane().add(mainContentPanel);
    
    mainMenuBar = new JMenuBar();
    //========================= Datei ============================
    JMenu menu = new JMenu("Datei");
    
    JMenuItem menuItem = new JMenuItem("Formularfelder aus Dokument einlesen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        scan(doc);
        setFrameSize();
      }});
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Formulartitel setzen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        setFormTitle();
        setFrameSize();
      }});
    menu.add(menuItem);
    
    menuItem = new JMenuItem("WollMux-Formularmerkmale aus Dokument entfernen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        deForm(doc); 
      }});
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Formularbeschreibung editieren");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        editFormDescriptor();
      }});
    menu.add(menuItem);

    
    mainMenuBar.add(menu);
//  ========================= Einf�gen ============================
    menu = new JMenu("Einf�gen");
    menuItem = new JMenuItem("Empf�ngerauswahl-Tab");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        insertStandardEmpfaengerauswahl();
        setFrameSize();
      }
      });
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Abbrechen, <-Zur�ck, Weiter->");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        insertStandardButtonsMiddle();
        setFrameSize();
      }
      });
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Abbrechen, <-Zur�ck, PDF, Drucken");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        insertStandardButtonsLast();
        setFrameSize();
      }
      });
    menu.add(menuItem);
    
    
    mainMenuBar.add(menu);

    myFrame.setJMenuBar(mainMenuBar);

    writeChangesTimer = new Timer(500, new ActionListener()
    { public void actionPerformed(ActionEvent e)
      {
        updateDocument(doc);
    }});
    writeChangesTimer.setCoalesce(true);
    writeChangesTimer.setRepeats(false);
    
    initEditor();

    initModelsAndViews();
    
    writeChangesTimer.stop();
    
    setFrameSize();
    myFrame.setResizable(true);
    myFrame.setVisible(true);
  }
  
  /**
   * Wertet {@link #formDescriptor}, sowie die Bookmarks von {@link #doc} aus und initialisiert 
   * alle internen
   * Strukturen entsprechend. Dies aktualisiert auch die entsprechenden Views.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void initModelsAndViews()
  {
    formControlModelList.clear();
    ConfigThingy conf = formDescriptor.toConfigThingy();
    parseGlobalFormInfo(conf);
    
    ConfigThingy fensterAbschnitte = conf.query("Formular").query("Fenster");
    Iterator fensterAbschnittIterator = fensterAbschnitte.iterator();
    while (fensterAbschnittIterator.hasNext())
    {
      ConfigThingy fensterAbschnitt = (ConfigThingy)fensterAbschnittIterator.next();
      Iterator tabIter = fensterAbschnitt.iterator();
      while (tabIter.hasNext())
      {
        ConfigThingy tab = (ConfigThingy)tabIter.next();
        parseTab(tab, -1);
      }
    }
    
    /*
     * Immer mindestens 1 Tab in der Liste.
     */
    if (formControlModelList.isEmpty())
    {
      String id = formControlModelList.makeUniqueId(STANDARD_TAB_NAME);
      FormControlModel separatorTab = FormControlModel.createTab(id, id, this);
      formControlModelList.add(separatorTab,0);
    }
    
    insertionModelList.clear();
    XBookmarksSupplier bmSupp = UNO.XBookmarksSupplier(doc); 
    String[] bookmarks = bmSupp.getBookmarks().getElementNames();
    for (int i = 0; i < bookmarks.length; ++i)
    {
      try{
        String bookmark = bookmarks[i];
        if (InsertionModel.INSERTION_BOOKMARK.matcher(bookmark).matches())
          insertionModelList.add(new InsertionModel(bookmark, bmSupp, functionSelectionProvider, this));
      }catch(Exception x)
      {
        Logger.error(x);
      }
    }

    setFrameSize();
  }
  
  /**
   * Initialisiert den formDescriptor mit den Formularbeschreibungsdaten des
   * Dokuments doc.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void initFormDescriptor(XTextDocument doc)
  {
    formDescriptor = new FormDescriptor(doc);
  }
  
  /**
   * Bringt einen modalen Dialog zum Bearbeiten des Formulartitels.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setFormTitle()
  {
    String newTitle = JOptionPane.showInputDialog(myFrame, "Bitte Formulartitel eingeben", formTitle);
    if (newTitle != null)
    {
      formTitle = newTitle;
      documentNeedsUpdating();
    }
  }
  
  /**
   * Speichert die aktuelle Formularbeschreibung im Dokument und aktualisiert Bookmarks etc.
   * 
   * @return die aktualisierte Formularbeschreibung
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private ConfigThingy updateDocument(XTextDocument doc)
  {
    Logger.debug("�bertrage Formularbeschreibung ins Dokument");
    Map mapFunctionNameToConfigThingy = new HashMap();
    insertionModelList.updateDocument(mapFunctionNameToConfigThingy);
    ConfigThingy conf = buildFormDescriptor(mapFunctionNameToConfigThingy);
    formDescriptor.fromConfigThingy(new ConfigThingy(conf));
    formDescriptor.writeDocInfoFormularbeschreibung();
    setModifiedState(doc);
    return conf;
  }

  /**
   * Setzt den Ge�ndert Status von doc auf wahr.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setModifiedState(XTextDocument doc)
  {
    try{UNO.XModifiable(doc).setModified(true);}catch(Exception x) {}
  }
  
  /**
   * Entfernt die WollMux-Kommandos "insertFormValue", "setGroups", "setType formDocument" und
   *  "form", sowie einen Rahmen mit Namen {@link FormDescriptor#WOLLMUX_FRAME_NAME} aus dem
   *  Dokument doc.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private static void deForm(XTextDocument doc)
  {
    XBookmarksSupplier bmSupp = UNO.XBookmarksSupplier(doc);
    XNameAccess bookmarks = bmSupp.getBookmarks();
    String[] names = bookmarks.getElementNames();
    for (int i = 0; i < names.length; ++i)
    {
      try{
        String bookmark = names[i];
        if (BOOKMARK_KILL_PATTERN.matcher(bookmark).matches())
        {
          XTextContent bm = UNO.XTextContent(bookmarks.getByName(bookmark));
          bm.getAnchor().getText().removeTextContent(bm);
        }
          
      }catch(Exception x)
      {
        Logger.error(x);
      }
    }
    
    XTextFramesSupplier frameSupp = UNO.XTextFramesSupplier(doc);
    XNameAccess frames = frameSupp.getTextFrames();
    try{
      XTextContent frame = UNO.XTextContent(frames.getByName(FormDescriptor.WOLLMUX_FRAME_NAME));
      /* Dies funktioniert nicht, weil f�r an der Seite verankerte Rahmen getAnchor() null liefert.
       * Siehe Issue 70643
       
      XTextRange range = frame.getAnchor();
      XText text = range.getText();
      text.removeTextContent(frame);
      */
      doc.getText().removeTextContent(frame);
    } catch(Exception x) {}
  }

  /**
   * Liefert ein ConfigThingy zur�ck, das den aktuellen Zustand der Formularbeschreibung
   * repr�sentiert. Zum Exportieren der Formularbeschreibung sollte {@link #updateDocument(XTextDocument)}
   * verwendet werden.
   * @param mapFunctionNameToConfigThingy bildet einen Funktionsnamen auf ein ConfigThingy ab, 
   *        dessen Wurzel der Funktionsname ist und dessen Inhalt eine Funktionsdefinition ist.
   *        Diese Funktionen ergeben den Funktionen-Abschnitt. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED */
  private ConfigThingy buildFormDescriptor(Map mapFunctionNameToConfigThingy)
  {
    ConfigThingy conf = new ConfigThingy("WM");
    ConfigThingy form = conf.add("Formular");
    form.add("TITLE").add(formTitle);
    form.addChild(formControlModelList.export());
    if (!mapFunctionNameToConfigThingy.isEmpty())
    {
      ConfigThingy funcs = form.add("Funktionen");
      Iterator iter = mapFunctionNameToConfigThingy.values().iterator();
      while (iter.hasNext())
      {
        funcs.addChild((ConfigThingy)iter.next());
      }
    }
    return conf;
  }
  
  /**
   * Extrahiert aus conf die globalen Eingenschaften des Formulars wie z,B, den Formulartitel
   * oder die Funktionen des Funktionen-Abschnitts.
   * @param conf der WM-Knoten der �ber einer beliebigen Anzahl von Formular-Knoten sitzt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void parseGlobalFormInfo(ConfigThingy conf)
  {
    ConfigThingy tempConf = conf.query("Formular").query("TITLE");
    if (tempConf.count() > 0) formTitle = tempConf.toString();
    tempConf = conf.query("Formular").query("Funktionen");
    if (tempConf.count() >= 1)
    {
      try{tempConf = tempConf.getFirstChild();}catch(Exception x){}
    }
    else
    {
      tempConf = new ConfigThingy("Formular");
    }
    functionSelectionProvider = new FunctionSelectionProvider(functionLibrary, tempConf);
  }
  
  /**
   * F�gt am Anfang der Liste eine Standard-Empfaengerauswahl-Tab ein.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void insertStandardEmpfaengerauswahl()
  {
    try{ 
      ConfigThingy conf = new ConfigThingy("Empfaengerauswahl", EMPFAENGER_TAB_URL);
      parseTab(conf, 0);
      documentNeedsUpdating();
    }catch(Exception x) { Logger.error(x);}
  }
  
  /**
   * H�ngt die Standardbuttons f�r einen mittleren Tab an das Ende der Liste.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void insertStandardButtonsMiddle()
  {
    try{ 
      ConfigThingy conf = new ConfigThingy("Buttons", STANDARD_BUTTONS_MIDDLE_URL);
      int index = leftPanel.getButtonInsertionIndex();
      parseGrandchildren(conf, index, false);
      documentNeedsUpdating();
    }catch(Exception x) { Logger.error(x);}
  }
  
  /**
   * H�ngt die Standardbuttons f�r den letzten Tab an das Ende der Liste.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void insertStandardButtonsLast()
  {
    try{ 
      ConfigThingy conf = new ConfigThingy("Buttons", STANDARD_BUTTONS_LAST_URL);
      int index = leftPanel.getButtonInsertionIndex();
      parseGrandchildren(conf, index, false);
      documentNeedsUpdating();
    }catch(Exception x) { Logger.error(x);}
  }
  
  /**
   * Parst das Tab conf und f�gt entsprechende FormControlModels der 
   * {@link #formControlModelList} hinzu.
   * @param conf der Knoten direkt �ber "Eingabefelder" und "Buttons".
   * @param idx falls >= 0 werden die Steuerelemente am entsprechenden Index der
   *        Liste in die Formularbeschreibung eingef�gt, ansonsten ans Ende angeh�ngt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void parseTab(ConfigThingy conf, int idx)
  {
    String id = conf.getName();
    String label = id;
    String action = FormControlModel.NO_ACTION;
    String tooltip = "";
    char hotkey = 0;
    
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy attr = (ConfigThingy)iter.next();
      String name = attr.getName();
      String str = attr.toString();
      if (name.equals("TITLE")) label = str; 
      else if (name.equals("CLOSEACTION")) action = str;
      else if (name.equals("TIP")) tooltip = str;
      else if (name.equals("HOTKEY")) hotkey = str.length() > 0 ? str.charAt(0) : 0;
    }
    
    FormControlModel tab = FormControlModel.createTab(label, id, this);
    tab.setAction(action);
    tab.setTooltip(tooltip);
    tab.setHotkey(hotkey);
    
    if (idx >= 0)
    {
      formControlModelList.add(tab, idx++);
      idx += parseGrandchildren(conf.query("Eingabefelder"), idx, true);
      parseGrandchildren(conf.query("Buttons"), idx, false);
    }
    else
    {
      formControlModelList.add(tab);
      parseGrandchildren(conf.query("Eingabefelder"), -1, true);
      parseGrandchildren(conf.query("Buttons"), -1, false);
    }
    
    documentNeedsUpdating();
  }
  
  /**
   * Parst die Kinder der Kinder von grandma als Steuerelemente und f�gt der
   * {@link #formControlModelList} entsprechende FormControlModels hinzu.
   * @param idx falls >= 0 werden die Steuerelemente am entsprechenden Index der
   *        Liste in die Formularbeschreibung eingef�gt, ansonsten ans Ende angeh�ngt.
   * @param killLastGlue falls true wird das letzte Steuerelement entfernt, wenn es
   *        ein glue ist.
   * @return die Anzahl der erzeugten Steuerelemente.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private int parseGrandchildren(ConfigThingy grandma, int idx, boolean killLastGlue)
  {
    if (idx < 0) idx = formControlModelList.size();
    
    boolean lastIsGlue = false;
    FormControlModel model = null;
    int count = 0;
    Iterator grandmaIter = grandma.iterator();
    while (grandmaIter.hasNext())
    {
      Iterator iter = ((ConfigThingy)grandmaIter.next()).iterator();
      while (iter.hasNext())
      {
        model = new FormControlModel((ConfigThingy)iter.next(), functionSelectionProvider, this);
        lastIsGlue = model.isGlue();
        ++count;
        formControlModelList.add(model, idx++);
      }
    }
    if (killLastGlue && lastIsGlue)
    {
      formControlModelList.remove(model);
      --count;
    }
    
    documentNeedsUpdating();
    
    return count;
  }
  
  /**
   * Scannt das Dokument doc durch und erzeugt {@link FormControlModel}s f�r alle
   * Formularfelder, die noch kein umschlie�endes WollMux-Bookmark haben.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void scan(XTextDocument doc)
  {
    try{
      XDocumentInfo info = UNO.XDocumentInfoSupplier(doc).getDocumentInfo();
      try{
        String tit = ((String)UNO.getProperty(info,"Title")).trim();
        if (formTitle == GENERATED_FORM_TITLE && tit.length() > 0)
          formTitle = tit;
      }catch(Exception x){}
      DocumentTree tree = new DocumentTree(doc);
      Visitor visitor = new DocumentTree.Visitor(){
        private Map insertions = new HashMap();
        private StringBuilder text = new StringBuilder();
        private StringBuilder fixupText = new StringBuilder();
        private FormControlModel fixupCheckbox = null;
        
        private void fixup()
        {
          if (fixupCheckbox != null && fixupCheckbox.getLabel() == NO_LABEL)
          {
            fixupCheckbox.setLabel(makeLabelFromStartOf(fixupText, 2*GENERATED_LABEL_MAXLENGTH));
            fixupCheckbox = null;
          }
          fixupText.setLength(0);
        }
        
        public boolean container(Container container, int count)
        {
          fixup();
          
          if (container.getType() != DocumentTree.PARAGRAPH_TYPE) text.setLength(0);
          
          return true;
        }
        
        public boolean textRange(TextRange textRange)
        {
          String str = textRange.getString(); 
          text.append(str);
          fixupText.append(str);
          return true;
        }
        
        public boolean insertionBookmark(InsertionBookmark bookmark)
        {
          if (bookmark.isStart())
            insertions.put(bookmark.getName(), bookmark);
          else
            insertions.remove(bookmark.getName());
          
          return true;
        }
        
        public boolean formControl(FormControl control)
        {
          fixup();
          
          if (insertions.isEmpty())
          {
            FormControlModel model = registerFormControl(control, text);
            if (model != null && model.getType() == FormControlModel.CHECKBOX_TYPE)
              fixupCheckbox = model;
          }
          
          return true;
        }
      };
      visitor.visit(tree);
    } 
    catch(Exception x) {Logger.error("Fehler w�hrend des Scan-Vorgangs",x);}
    
    documentNeedsUpdating();
  }
  
  /**
   * F�gt der {@link #formControlModelList} ein neues {@link FormControlModel} hinzu f�r
   * das {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl} control, 
   * wobei
   * text der Text sein sollte, der im Dokument vor control steht. Dieser Text wird zur
   * Generierung des Labels herangezogen. Es wird ebenfalls der 
   * {@link #insertionModelList} ein entsprechendes {@link InsertionModel} hinzugef�gt.
   * Zus�tzlich wird immer ein entsprechendes Bookmark um das Control herumgelegt, das
   * die Einf�gestelle markiert. 
   * 
   * @return null, falls es sich bei dem Control nur um eine reine Einf�gestelle
   *         handelt. In diesem Fall wird nur der {@link #insertionModelList}
   *         ein Element hinzugef�gt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerFormControl(FormControl control, StringBuilder text)
  {
    String label;
    String id;
    String descriptor = control.getDescriptor();
    Matcher m = MAGIC_DESCRIPTOR_PATTERN.matcher(descriptor);
    if (m.matches())
    {
      label = m.group(1).trim();
      if (label.length() == 0) label = INSERTION_ONLY; 
      id = m.group(2).trim();
    }
    else
    {
      label = makeLabelFromEndOf(text, GENERATED_LABEL_MAXLENGTH);
      id = descriptor;
    }
    
    id = makeControlId(label, id);
    
    FormControlModel model = null;
    
    if (label != INSERTION_ONLY)
    {
      switch (control.getType())
      {
        case DocumentTree.CHECKBOX_CONTROL: model = registerCheckbox(control, label, id); break;
        case DocumentTree.DROPDOWN_CONTROL: model = registerDropdown((DropdownFormControl)control, label, id); break;
        case DocumentTree.INPUT_CONTROL:    model = registerInput(control, label, id); break;
        default: Logger.error("Unbekannter Typ Formular-Steuerelement"); return null;
      }
    }
    
    String bookmarkName = insertFormValue(id);
    if (label == INSERTION_ONLY)
    {
      if (id.startsWith(GLOBAL_PREFIX))
      {
        id = id.substring(GLOBAL_PREFIX.length());
        bookmarkName = insertValue(id);
      }
    }
    
    bookmarkName = control.surroundWithBookmark(bookmarkName);

    try{
      InsertionModel imodel = new InsertionModel(bookmarkName, UNO.XBookmarksSupplier(doc), functionSelectionProvider, this);
      insertionModelList.add(imodel);
    }catch(Exception x)
    {
      Logger.error("Es wurde ein fehlerhaftes Bookmark generiert: \""+bookmarkName+"\"", x);
    }
    
    return model;
  }

  /**
   * Bastelt aus dem Ende des Textes text ein Label das maximal maxlen Zeichen lang ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeLabelFromEndOf(StringBuilder text, int maxlen)
  {
    String label;
    String str = text.toString().trim();
    int len = str.length();
    if (len > maxlen) len = maxlen;
    label = str.substring(str.length() - len);
    if (label.length() < 2) label = NO_LABEL;
    return label;
  }
  
  /**
   * Bastelt aus dem Start des Textes text ein Label, das maximal maxlen Zeichen lang ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeLabelFromStartOf(StringBuilder text, int maxlen)
  {
    String label;
    String str = text.toString().trim();
    int len = str.length();
    if (len > maxlen) len = maxlen;
    label = str.substring(0, len);
    if (label.length() < 2) label = NO_LABEL;
    return label;
  }
  
  /**
   * F�gt {@link #formControlModelList} ein neues {@link FormControlModel} f�r eine Checkbox
   * hinzu und liefert es zur�ck.
   * @param control das entsprechende {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl}
   * @param label das Label
   * @param id die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerCheckbox(FormControl control, String label, String id)
  {
    FormControlModel model = null;
    label = NO_LABEL; //immer fixUp-Text von hinter der Checkbox benutzen, weil meist bessere Ergebnisse
    model = FormControlModel.createCheckbox(label, id, this);
    if (control.getString().equalsIgnoreCase("true"))
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add("true");
      model.setAutofill(functionSelectionProvider.getFunctionSelection(autofill));
    }
    formControlModelList.add(model);
    return model;
  }
  
  /**
   * F�gt {@link #formControlModelList} ein neues {@link FormControlModel} f�r eine Auswahlliste
   * hinzu und liefert es zur�ck.
   * @param control das entsprechende {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl}
   * @param label das Label
   * @param id die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerDropdown(DropdownFormControl control, String label, String id)
  {
    FormControlModel model = null;
    String[] items = control.getItems();
    boolean editable = false;
    for (int i = 0; i < items.length; ++i)
    {
      if (items[i].equalsIgnoreCase("<<Freitext>>")) 
      {
        String[] newItems = new String[items.length - 1];
        System.arraycopy(items, 0, newItems, 0, i);
        System.arraycopy(items, i + 1, newItems, i, items.length - i - 1);
        items = newItems;
        editable = true;
        break;
      }
    }
    model = FormControlModel.createComboBox(label, id, items, this);
    model.setEditable(editable);
    String preset = control.getString().trim();
    if (preset.length() > 0)
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add(preset);
      model.setAutofill(functionSelectionProvider.getFunctionSelection(autofill));
    }
    formControlModelList.add(model);
    return model;
  }
  
  /**
   * F�gt {@link #formControlModelList} ein neues {@link FormControlModel} f�r ein Eingabefeld
   * hinzu und liefert es zur�ck.
   * @param control das entsprechende {@link de.muenchen.allg.itd51.wollmux.former.DocumentTree.FormControl}
   * @param label das Label
   * @param id die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerInput(FormControl control, String label, String id)
  {
    FormControlModel model = null;
    model = FormControlModel.createTextfield(label, id, this);
    String preset = control.getString().trim();
    if (preset.length() > 0)
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add(preset);
      model.setAutofill(functionSelectionProvider.getFunctionSelection(autofill));
    }
    formControlModelList.add(model);
    return model;
  }
  
  /**
   * Macht aus str einen passenden Bezeichner f�r ein Steuerelement. Falls 
   * label == {@link #INSERTION_ONLY}, so
   * muss der Bezeichner nicht eindeutig sein (dies ist der Marker f�r eine reine
   * Einf�gestelle, f�r die kein Steuerelement erzeugt werden muss).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeControlId(String label, String str)
  {
    if (label == INSERTION_ONLY)
    {
      boolean glob = str.startsWith(GLOBAL_PREFIX);
      if (glob) str = str.substring(GLOBAL_PREFIX.length());
      str = str.replaceAll("[^a-zA-Z_0-9]","");
      if (str.length() == 0) str = "Einfuegung";
      if (!str.matches(STARTS_WITH_LETTER_RE)) str = "_" + str;
      if (glob) str = GLOBAL_PREFIX + str;
      return str;
    }
    else
    {
      str = str.replaceAll("[^a-zA-Z_0-9]","");
      if (str.length() == 0) str = "Steuerelement";
      if (!str.matches(STARTS_WITH_LETTER_RE)) str = "_" + str;
      return formControlModelList.makeUniqueId(str);
    }
  }

  private static class NoWrapEditorKit extends DefaultEditorKit
  {
    private static final long serialVersionUID = -2741454443147376514L;
    private ViewFactory vf = null;

    public ViewFactory getViewFactory()
    {
      if (vf == null) vf=new NoWrapFactory();
      return vf;
    };

    private class NoWrapFactory implements ViewFactory
    {
      public View create(Element e)
      {
        return new PlainView(e);
      }
   
    };
  };

  /**
   * Initialisiert die GUI f�r den Quelltexteditor.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void initEditor()
  {
    JMenu menu;
    JMenuItem menuItem;
    editorMenuBar = new JMenuBar();
    //========================= Datei ============================
    menu = new JMenu("Datei");
    
    menuItem = new JMenuItem("Speichern");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        try
        {
          ConfigThingy conf = new ConfigThingy("", null, new StringReader(editor.getText()));
          myFrame.setJMenuBar(mainMenuBar);
          myFrame.getContentPane().remove(editorContentPanel);
          myFrame.getContentPane().add(mainContentPanel);
          formDescriptor.fromConfigThingy(conf);
          documentNeedsUpdating();
          initModelsAndViews();
        }
        catch (Exception e1)
        {
          JOptionPane.showMessageDialog(myFrame, e1.getMessage(), "Fehler beim Parsen der Formularbeschreibung", JOptionPane.WARNING_MESSAGE);
        }
      }});
    menu.add(menuItem);
    
    menuItem = new JMenuItem("Abbrechen");
    menuItem.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        myFrame.setJMenuBar(mainMenuBar);
        myFrame.getContentPane().remove(editorContentPanel);
        myFrame.getContentPane().add(mainContentPanel);
        setFrameSize();
      }});
    menu.add(menuItem);
    
        
    editorMenuBar.add(menu);

    editor = new JEditorPane("text/plain","");
    editor.setEditorKit(new NoWrapEditorKit());
    
    editor.setFont(new Font("Monospaced",Font.PLAIN,editor.getFont().getSize()+2));
    JScrollPane scrollPane = new JScrollPane(editor, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    editorContentPanel = new JPanel(new BorderLayout());
    editorContentPanel.add(scrollPane, BorderLayout.CENTER);
  }

  
  /**
   * �ffnet ein Fenster zum Editieren der Formularbeschreibung. Beim Schliessend des Fensters
   * wird die ge�nderte Formularbeschreibung neu geparst, falls sie syntaktisch korrekt ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void editFormDescriptor()
  {
    editor.setCaretPosition(0);
    editor.setText(updateDocument(doc).stringRepresentation());
    myFrame.getContentPane().remove(mainContentPanel);
    myFrame.getContentPane().add(editorContentPanel);
    myFrame.setJMenuBar(editorMenuBar);
    setFrameSize();
  }
  
  /**
   * Liefert "WM(CMD'insertValue' DB_SPALTE '&lt;id>').
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String insertValue(String id)
  {
    return "WM(CMD 'insertValue' DB_SPALTE '"+id+"')";
  }
  
  /**
   * Liefert "WM(CMD'insertFormValue' ID '&lt;id>').
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String insertFormValue(String id)
  {
    return "WM(CMD 'insertFormValue' ID '"+id+"')";
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    if (writeChangesTimer.isRunning())
    {
      Logger.debug("Schreibe wartende �nderungen ins Dokument vor abort()");
      writeChangesTimer.stop();
      updateDocument(doc);
    }
    myFrame.dispose();
    myFrame = null;
    if (abortListener != null)
      abortListener.actionPerformed(new ActionEvent(this, 0, ""));
  }
  
  /**
   * Schliesst den FM4000 und alle zugeh�rigen Fenster.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void dispose()
  {
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{abort();}catch(Exception x){};
        }
      });
    }
    catch(Exception x) {}
  }
  
  /**
   * Bringt den FormularMax 4000 in den Vordergrund.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void toFront()
  {
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{myFrame.toFront();}catch(Exception x){};
        }
      });
    }
    catch(Exception x) {}
  }
  
  /**
   * Workaround f�r Problem unter Windows, dass das Layout bei myFrame.pack() die 
   * Taskleiste nicht ber�cksichtigt (das Fenster also dahinter verschwindet), zumindest
   * solange nicht bis man die Taskleiste mal in ihrer Gr��e ver�ndert hat.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setFrameSize()
  {
    myFrame.pack();
    fixFrameSize(myFrame);
  }

  /**
   * Sorgt daf�r, dass die Ausdehnung von frame nicht die maximal erlaubten
   * Fensterdimensionen �berschreitet.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void fixFrameSize(JFrame frame)
  {
    Rectangle maxWindowBounds;
    
    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    maxWindowBounds = genv.getMaximumWindowBounds();
    String lafName = UIManager.getSystemLookAndFeelClassName(); 
    if (!lafName.contains("plaf.windows."))
      maxWindowBounds.height-=32; //Sicherheitsabzug f�r KDE Taskleiste
    
    Rectangle frameBounds = frame.getBounds();
    if (frameBounds.x < maxWindowBounds.x)
    {
      frameBounds.width -= (maxWindowBounds.x - frameBounds.x);
      frameBounds.x = maxWindowBounds.x;
    }
    if (frameBounds.y < maxWindowBounds.y)
    {
      frameBounds.height -= (maxWindowBounds.y - frameBounds.y);
      frameBounds.y = maxWindowBounds.y;
    }
    if (frameBounds.width > maxWindowBounds.width)
      frameBounds.width = maxWindowBounds.width;
    if (frameBounds.height > maxWindowBounds.height)
      frameBounds.height = maxWindowBounds.height;
    frame.setBounds(frameBounds);
  }

  private class MyWindowListener implements WindowListener
  {
    public void windowOpened(WindowEvent e) {}
    public void windowClosing(WindowEvent e) {closeAction.actionPerformed(null); }
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e){}   
    
  }
  
  /**
   * Ruft den FormularMax4000 f�r das aktuelle Vordergrunddokument auf, falls dieses
   * ein Textdokument ist. 
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void main(String[] args) throws Exception
  {
    UNO.init();
    WollMuxFiles.setupWollMuxDir();
    Logger.init(System.err, Logger.DEBUG);
    XTextDocument doc = UNO.XTextDocument(UNO.desktop.getCurrentComponent());
    Map context = new HashMap();
    DialogLibrary dialogLib = WollMuxFiles.parseFunctionDialogs(WollMuxFiles.getWollmuxConf(), null, context);
    new FormularMax4000(doc,null, WollMuxFiles.parseFunctions(WollMuxFiles.getWollmuxConf(), dialogLib, context, null));
  }

}
