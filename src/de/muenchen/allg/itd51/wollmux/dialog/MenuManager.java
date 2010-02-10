/* Copyright (C) 2009 Matthias S. Benkmann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
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
 * *
 * @author Matthias S. Benkmann
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;

/**
 * Eine GUI zum Bearbeiten der Men�struktur der WollMuxBar.
 */
public class MenuManager
{
  /**
   * Regex zur Identifikation von legalen Schl�sseln.
   */
  private static Pattern KEY_PATTERN = Pattern.compile("^([a-zA-Z_][a-zA-Z_0-9]*)");

  private static final String UIELEMENT_WITHOUT_TYPE_ERR =
    L.m("Men�eintrag ohne TYPE-Attribut gefunden");

  /**
   * Text der an verschiedenen Stellen verwendet wird.
   */
  private static final String NEW_FILES_TEXT = L.m("Neue Datei(en)...");

  private static final String[] BUTTONLEISTE_PATH = new String[] {
    "Symbolleisten", "Briefkopfleiste" };

  private static final String[] MENUELEISTE_PATH = new String[] { "Menueleiste" };

  private static DataFlavor[] TRANSFER_DATAFLAVORS;
  static
  {
    try
    {
      TRANSFER_DATAFLAVORS =
        new DataFlavor[] {
          new DataFlavor(MyTransferable.class, "Menu Manager Item"),
          new DataFlavor("text/plain; charset=utf-8") };
    }
    catch (ClassNotFoundException x)
    {}
  }

  /**
   * Die vom Administrator vorgegebene wollmux.conf
   */
  private ConfigThingy defaultConf;

  /**
   * Die wollmuxbar.conf des Benutzers.
   */
  private ConfigThingy userConf;

  /**
   * Die Wurzel des Men�baumes. Direkt darunter liegen die {@link Node}s f�r
   * Buttonleiste und Men�leiste.
   */
  private Node menuTreeRoot;

  /**
   * Das Hauptfenster der GUI.
   */
  private JFrame myFrame;

  /**
   * Auf {@link #myFrame} registriert als WindowListener.
   */
  private MyWindowListener oehrchen;

  /**
   * Der JTree, der die wichtigste GUI Komponente ist.
   */
  private JTree myTree;

  /**
   * Das zu {@link #myTree} geh�rende {@link MyTreeModel}.
   */
  private MyTreeModel myTreeModel;

  /**
   * Das "Bearbeiten" Men� als Popup f�r den Rechts-Klick.
   */
  private JPopupMenu editMenuPopup = new JPopupMenu();

  /**
   * Der {@link JFileChooser} wird nach erstmaliger Initialisierung immer wieder
   * verwendet, damit das zuletzt gesetzte Verzeichnis immer wieder angeboten.
   */
  private JFileChooser fileChooser;

  /**
   * Wird nach dem Schlie�en des Dialogs aufgerufen.
   */
  private ActionListener finishedAction;

  /**
   * Alle verwendbaren CONFIG_IDs.
   */
  private List<ConfigID> configIDs;

  /**
   * Das Men� "CONF_IDs".
   */
  private JMenu confidsMenu;

  /**
   * Zeigt eine GUI an, �ber die die Men�struktur der WollMuxBar bearbeitet werden
   * kann. Alle �nderungen werden in die Datei wollmuxbar.conf geschrieben.
   * 
   * @param defaultConf
   *          Die vom Administrator vorgegebene wollmux.conf
   * @param userConf
   *          Die wollmuxbar.conf des Benutzers.
   * @param finishedAction
   *          wird aufgerufen wenn Dialog geschlossen wurde. Darf null sein.
   */
  public MenuManager(ConfigThingy defaultConf, ConfigThingy userConf,
      ActionListener finishedAction)
  {
    this.defaultConf = defaultConf;
    this.userConf = userConf;
    this.finishedAction = finishedAction;
    this.menuTreeRoot = parseMenuTree(defaultConf, userConf);
    this.configIDs = parseConfigIDs(defaultConf, userConf);
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        createGUI();
      }
    });
  }

  private void createGUI()
  {
    myFrame = new JFrame(L.m("Men�-Manager"));
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    oehrchen = new MyWindowListener();
    myFrame.addWindowListener(oehrchen);

    Common.setWollMuxIcon(myFrame);

    myTreeModel = new MyTreeModel();
    myTree = new JTree(myTreeModel);
    myTree.setEditable(false);
    myTree.setDragEnabled(true);
    myTree.setTransferHandler(new MyTransferHandler());
    myTree.setDropMode(DropMode.ON_OR_INSERT);
    myTree.setExpandsSelectedPaths(true);
    myTree.setRootVisible(false);
    myTree.setToggleClickCount(1);
    myTree.setPreferredSize(new Dimension(500, 600));
    myTree.setVisibleRowCount(menuTreeRoot.children.get(0).children.size()
      + menuTreeRoot.children.get(1).children.size() + 10);
    myTree.expandPath(new TreePath(new Object[] {
      menuTreeRoot, myTreeModel.getChild(menuTreeRoot, 1) }));
    myTree.addMouseListener(new MyMouseListener());

    JScrollPane scrollPane =
      new JScrollPane(myTree, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    JPanel myContentPanel = new JPanel(new BorderLayout());
    myContentPanel.add(scrollPane, BorderLayout.CENTER);
    myContentPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

    JMenuBar menubar = new JMenuBar();
    myFrame.setJMenuBar(menubar);
    JMenu menu = new JMenu(L.m("Datei"));
    menubar.add(menu);
    JMenuItem menuItem;
    menuItem = new JMenuItem(new AbstractAction(L.m("Speichern"))
    {
      public void actionPerformed(ActionEvent e)
      {
        save();
      }
    });
    menu.add(menuItem);
    menuItem = new JMenuItem(new AbstractAction(L.m("Schlie�en"))
    {
      public void actionPerformed(ActionEvent e)
      {
        closeAfterQuestion();
      }
    });
    menu.add(menuItem);

    menu = new JMenu(L.m("Bearbeiten"));
    menubar.add(menu);

    createEditMenu(menu);
    createEditMenu(editMenuPopup);

    confidsMenu = new JMenu(L.m("CONF_IDs"));
    menubar.add(confidsMenu);
    rebuildCONF_IDsMenu();

    myFrame.setContentPane(myContentPanel);
    myFrame.pack();

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    // frameHeight = screenSize.height * 8 / 10;
    // myFrame.setSize(frameWidth, frameHeight);
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    myFrame.setLocation(x, y);
    myFrame.setResizable(true);

    myFrame.setVisible(true);
  }

  /**
   * Baut {@link #confidsMenu} neu auf.
   * 
   * TESTED
   */
  private void rebuildCONF_IDsMenu()
  {
    confidsMenu.removeAll();

    Collections.sort(configIDs);

    JMenu add = new JMenu(L.m("Hinzuf�gen"));
    for (ConfigID cid : configIDs)
    {
      final String id = cid.id;
      add.add(new AbstractAction(id)
      {
        public void actionPerformed(ActionEvent e)
        {
          myTreeModel.addConfigID(id);
        }
      });
    }
    confidsMenu.add(add);

    JMenu addRecursive = new JMenu(L.m("Rekursiv hinzuf�gen"));
    for (ConfigID cid : configIDs)
    {
      final String id = cid.id;
      addRecursive.add(new AbstractAction(id)
      {
        public void actionPerformed(ActionEvent e)
        {
          myTreeModel.addConfigIDRecursive(id);
        }
      });
    }
    confidsMenu.add(addRecursive);

    JMenu remove = new JMenu(L.m("Entfernen"));
    for (ConfigID cid : configIDs)
    {
      final String id = cid.id;
      remove.add(new AbstractAction(id)
      {
        public void actionPerformed(ActionEvent e)
        {
          myTreeModel.removeConfigID(id);
        }
      });
    }
    confidsMenu.add(remove);

    JMenu removeRecursive = new JMenu(L.m("Rekursiv entfernen"));
    for (ConfigID cid : configIDs)
    {
      final String id = cid.id;
      removeRecursive.add(new AbstractAction(id)
      {
        public void actionPerformed(ActionEvent e)
        {
          myTreeModel.removeConfigIDRecursive(id);
        }
      });
    }
    confidsMenu.add(removeRecursive);

    confidsMenu.addSeparator();
    confidsMenu.add(new JMenuItem(new AbstractAction(L.m("Liste Bearbeiten"))
    {
      public void actionPerformed(ActionEvent e)
      {
        editConfIDsList();
      }
    }));
  }

  private void editConfIDsList()
  { // TESTED
    JDialog myDialog = new JDialog(myFrame, L.m("CONF_ID Liste Bearbeiten"));
    myDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    myDialog.setModal(true);
    List<ConfigID> tempConfigIDs = new Vector<ConfigID>(configIDs);
    editConfIDsList(myDialog, tempConfigIDs);
  }

  /**
   * TESTED
   */
  private void editConfIDsList(final JDialog myDialog,
      final List<ConfigID> tempConfigIDs)
  {
    Box mainBox = Box.createVerticalBox();
    mainBox.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    myDialog.setContentPane(mainBox);

    for (final ConfigID cid : tempConfigIDs)
    {
      Box hbox = Box.createHorizontalBox();
      myDialog.add(hbox);
      myDialog.add(Box.createVerticalStrut(2));
      hbox.add(new JLabel(cid.id));
      hbox.add(Box.createHorizontalStrut(4));
      final JTextField tf = new JTextField(40);
      String label = cid.label_user;
      if (label == null) label = cid.label_default;
      tf.setText(label);
      hbox.add(tf);
      tf.getDocument().addDocumentListener(new DocumentListener()
      {
        public void changedUpdate(DocumentEvent e)
        {
          cid.label_user = tf.getText();
        }

        public void insertUpdate(DocumentEvent e)
        {
          changedUpdate(e);
        }

        public void removeUpdate(DocumentEvent e)
        {
          changedUpdate(e);
        }
      });

      if (cid.label_default != null)
      {
        hbox.add(new JButton(new AbstractAction(L.m("Standard"))
        {
          public void actionPerformed(ActionEvent e)
          {
            cid.label_user = null;
            tf.setText(cid.label_default);
          }
        }));
      }
      else
      {
        hbox.add(new JButton(new AbstractAction(L.m("L�schen"))
        {
          public void actionPerformed(ActionEvent e)
          {
            tempConfigIDs.remove(cid);
            editConfIDsList(myDialog, tempConfigIDs);
          }
        }));
      }
    }

    Box hbox = Box.createHorizontalBox();
    myDialog.add(hbox);
    myDialog.add(Box.createVerticalStrut(2));
    hbox.add(Box.createHorizontalGlue());
    hbox.add(new JButton(new AbstractAction(L.m("Neu"))
    {
      public void actionPerformed(ActionEvent e)
      {
        String confId =
          JOptionPane.showInputDialog(myDialog,
            L.m("Bitte geben Sie die neue CONF_ID ein"), L.m("CONF_ID eingeben"),
            JOptionPane.QUESTION_MESSAGE);

        if (confId == null) return;

        for (ConfigID cid : tempConfigIDs)
          if (confId.equals(cid.id))
          {
            String msg = L.m("Diese CONF_ID ist bereits vorhanden!");
            JOptionPane.showMessageDialog(myDialog, msg, msg,
              JOptionPane.ERROR_MESSAGE);
            return;
          }

        if (!KEY_PATTERN.matcher(confId).matches())
        {
          {
            String msg =
              L.m("Die CONF_ID darf nur Ziffern, Buchstaben und Unterstriche enthalten.\nAu�erdem darf sie nicht mit einer Ziffer beginnen.");
            JOptionPane.showMessageDialog(myDialog, msg, L.m("Syntaxfehler"),
              JOptionPane.ERROR_MESSAGE);
            return;
          }
        }

        tempConfigIDs.add(new ConfigID(confId, null, confId));
        editConfIDsList(myDialog, tempConfigIDs);
      }
    }));

    myDialog.add(new JSeparator(SwingConstants.HORIZONTAL));
    myDialog.add(Box.createVerticalStrut(2));

    Box buttonBox = Box.createHorizontalBox();
    myDialog.add(buttonBox);
    buttonBox.add(new JButton(new AbstractAction(L.m("Abbrechen"))
    {
      public void actionPerformed(ActionEvent e)
      {
        myDialog.dispose();
      }
    }));
    buttonBox.add(Box.createHorizontalGlue());
    buttonBox.add(new JButton(new AbstractAction(L.m("OK"))
    {
      public void actionPerformed(ActionEvent e)
      {
        myDialog.dispose();
        myTreeModel.updateConfigIDs(tempConfigIDs);
      }
    }));

    myDialog.pack();
    Rectangle parentBounds = myFrame.getBounds();
    int frameWidth = myDialog.getWidth();
    int frameHeight = myDialog.getHeight();
    int x = parentBounds.x + parentBounds.width / 2 - frameWidth / 2;
    int y = parentBounds.y + parentBounds.height / 2 - frameHeight / 2;
    if (y < 32) y = 32;
    myDialog.setLocation(x, y);

    myDialog.setResizable(false);
    myDialog.setVisible(true);
  }

  /**
   * F�gt menu die Men�punkte f�r das Bearbeiten-Men� hinzu.
   * 
   */
  private void createEditMenu(JComponent menu)
  {
    JMenuItem menuItem = new JMenuItem(new AbstractAction(L.m("Bearbeiten..."))
    {
      public void actionPerformed(ActionEvent e)
      {
        myTreeModel.editProperties();
      }
    });
    menu.add(menuItem);
    menuItem = new JMenuItem(new AbstractAction(L.m("Standard wiederherstellen"))
    {
      public void actionPerformed(ActionEvent e)
      {
        myTreeModel.restoreStandard();
      }
    });
    menu.add(menuItem);
    addSeparator(menu);
    menuItem = new JMenuItem(new AbstractAction(L.m("Neues (Unter)Men�"))
    {
      public void actionPerformed(ActionEvent e)
      {
        myTreeModel.newSubMenu();
      }
    });
    menu.add(menuItem);
    menuItem = new JMenuItem(new AbstractAction(NEW_FILES_TEXT)
    {
      public void actionPerformed(ActionEvent e)
      {
        myTreeModel.newFile();
      }
    });
    menu.add(menuItem);
    menuItem = new JMenuItem(new AbstractAction(L.m("Neuer Separator"))
    {
      public void actionPerformed(ActionEvent e)
      {
        myTreeModel.newSeparator();
      }
    });
    menu.add(menuItem);
    menuItem = new JMenuItem(new AbstractAction(L.m("Neues ..."))
    {
      public void actionPerformed(ActionEvent e)
      {
        myTreeModel.newSomething();
      }
    });
    menu.add(menuItem);
    addSeparator(menu);
    menuItem = new JMenuItem(new AbstractAction(L.m("L�schen"))
    {
      public void actionPerformed(ActionEvent e)
      {
        myTreeModel.deleteSelection();
      }
    });
    menu.add(menuItem);
  }

  /**
   * F�gt menu einen f�r die jeweilige Men�art passenden Separator hinzu.
   * 
   */
  private void addSeparator(JComponent menu)
  {
    if (menu instanceof JPopupMenu)
      menu.add(new JPopupMenu.Separator());
    else
      menu.add(new JSeparator(SwingConstants.HORIZONTAL));
  }

  /**
   * Liefert das empfohlene Startverzeichnis f�r einen {@link JFileChooser}.
   * 
   * TESTED
   */
  private File getFileChooserStartDirectory()
  {
    try
    {
      return new File(WollMuxFiles.getDEFAULT_CONTEXT().toURI());
    }
    catch (Exception x)
    {}

    String userHome = System.getProperty("user.home");
    return new File(userHome);
  }

  /**
   * Versucht aus der absoluten URL von file eine URL zu machen, die relativ zum
   * DEFAULT_CONTEXT ist. Alternativ wird die Variable ${user.home} in die URL
   * eingebaut, wenn sie unterhalb dieses Verzeichnisses liegt und dies g�nstiger
   * erscheint. Falls diese Heuristiken nicht fruchten wird eine absolute URL
   * zur�ckgeliefert.
   * 
   * TESTED
   */
  private String getRelativeURLifPossible(File file)
  {
    String path = canonicalPath(file);

    try
    {
      File defaultContextFile = new File(WollMuxFiles.getDEFAULT_CONTEXT().toURI());
      String defaultContextPath = canonicalPath(defaultContextFile);

      String relativePath = makeRelative(path, defaultContextPath);
      if (relativePath != null) return relativePath;
    }
    catch (Exception x)
    {
      // Hier fliegen wir z.B raus, wenn der DEFAULT_CONTEXT keine file: URL ist.
    }

    String userHome = System.getProperty("user.home");
    String homePath = canonicalPath(new File(userHome));
    String relativePath = makeRelative(path, homePath);
    if (relativePath != null) return turnIntoURL("${user.home}/" + relativePath);

    try
    {
      return file.toURI().toURL().toExternalForm();
    }
    catch (MalformedURLException x)
    {
      return "file:" + file.toString();
    }
  }

  private String turnIntoURL(String path)
  {
    return "file:" + path.replaceAll("[" + File.separator + "]", "/");
  }

  private String makeRelative(String pathToMakeRel,
      String pathRelativeToWhichItShouldBe)
  {
    if (pathToMakeRel.startsWith(pathRelativeToWhichItShouldBe))
    {
      pathToMakeRel =
        pathToMakeRel.substring(pathRelativeToWhichItShouldBe.length());
      if (pathToMakeRel.startsWith(File.separator))
        return pathToMakeRel.substring(1);
      else
        return pathToMakeRel;
    }
    else
      return null;
  }

  private String canonicalPath(File file)
  {
    String canonicalPath;
    /*
     * Derzeit auskommentiert, weil ich nicht sicher bin, ob das Aufl�sen von
     * Symlinks nicht in manchen Anwendungsf�llen st�rt.
     */
    // try
    // {
    // canonicalPath = file.getCanonicalPath();
    // }
    // catch (IOException x)
    // {
    canonicalPath = file.getAbsolutePath();
    // }
    return canonicalPath;
  }

  private class MyTreeModel implements TreeModel
  {
    private List<TreeModelListener> listeners = new ArrayList<TreeModelListener>();

    public void addTreeModelListener(TreeModelListener l)
    {
      if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l)
    {
      listeners.remove(l);
    }

    public Object getChild(Object parent, int index)
    {
      Node parentNode = (Node) parent;
      try
      {
        return parentNode.children.get(index);
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
      return null;
    }

    public int getChildCount(Object parent)
    {
      return ((Node) parent).children.size();
    }

    public int getIndexOfChild(Object parent, Object child)
    {
      if (parent != null && child != null)
        return ((Node) parent).children.indexOf(child);
      return -1;
    }

    public Object getRoot()
    {
      return menuTreeRoot;
    }

    public boolean isLeaf(Object n)
    {
      Node node = (Node) n;
      if (node.isMenuOrBar()) return false;
      return true;
    }

    public void valueForPathChanged(TreePath path, Object newValue)
    {
      throw new UnsupportedOperationException();
    }

    /**
     * F�gt allen momentan ausgew�hlten Knoten des Baums die CONF_ID id hinzu.
     * 
     * TESTED
     */
    public void addConfigID(String id)
    {
      if (warnIfNoSelection()) return;
      TreePath[] selectedPaths = myTree.getSelectionPaths();
      for (TreePath selectedPath : selectedPaths)
      {
        Node selectedNode = (Node) selectedPath.getLastPathComponent();
        if (selectedNode.addConfID(id)) nodeHasBeenModified(selectedPath);
      }
    }

    /**
     * F�gt allen momentan ausgew�hlten Knoten des Baums sowie all ihren Nachfahren
     * die CONF_ID id hinzu.
     * 
     * TESTED
     */
    public void addConfigIDRecursive(String id)
    {
      if (warnIfNoSelection()) return;
      TreePath[] selectedPaths = myTree.getSelectionPaths();
      for (TreePath selectedPath : selectedPaths)
        addConfigIDRecursive(selectedPath, id);
    }

    /**
     * F�gt path und allen seinen Nachfahren die CONF_ID id hinzu.
     * 
     * TESTED
     */
    private void addConfigIDRecursive(TreePath path, String id)
    {
      Node selectedNode = (Node) path.getLastPathComponent();
      if (selectedNode.addConfID(id)) nodeHasBeenModified(path);
      for (Node node : selectedNode.children)
        addConfigIDRecursive(path.pathByAddingChild(node), id);
    }

    /**
     * Entfernt von allen momentan ausgew�hlten Knoten des Baums die CONF_ID id.
     * 
     * TESTED
     */
    public void removeConfigID(String id)
    {
      if (warnIfNoSelection()) return;
      TreePath[] selectedPaths = myTree.getSelectionPaths();
      for (TreePath selectedPath : selectedPaths)
      {
        Node selectedNode = (Node) selectedPath.getLastPathComponent();
        if (selectedNode.removeConfID(id)) nodeHasBeenModified(selectedPath);
      }
    }

    /**
     * Entfernt von allen momentan ausgew�hlten Knoten des Baums sowie all ihren
     * Nachfahren die CONF_ID id.
     * 
     * TESTED
     */
    public void removeConfigIDRecursive(String id)
    {
      if (warnIfNoSelection()) return;
      TreePath[] selectedPaths = myTree.getSelectionPaths();
      for (TreePath selectedPath : selectedPaths)
        removeConfigIDRecursive(selectedPath, id);
    }

    /**
     * Entfernt von path und allen seinen Nachfahren die CONF_ID id.
     * 
     * TESTED
     */
    private void removeConfigIDRecursive(TreePath path, String id)
    {
      Node selectedNode = (Node) path.getLastPathComponent();
      if (selectedNode.removeConfID(id)) nodeHasBeenModified(path);
      for (Node node : selectedNode.children)
        removeConfigIDRecursive(path.pathByAddingChild(node), id);
    }

    /**
     * �bertr�gt die Unterschiede zwischen tempConfigIDs und
     * {@link MenuManager#configIDs} auf den Baum und setzt dann configIDs auf
     * tempConfigIDs. Das CONF_IDs Men� wird ebenfalls neu aufgebaut.
     * 
     * TESTED
     */
    public void updateConfigIDs(List<ConfigID> tempConfigIDs)
    {
      /*
       * Zuerst entfernen wir aus dem ganzen Baum als CONF_IDs, die in der neuen
       * Liste nicht mehr vorkommen
       */
      for (ConfigID cidold : configIDs)
      {
        boolean found = false;
        for (ConfigID cidnew : tempConfigIDs)
        {
          if (cidnew.id.equals(cidold.id))
          {
            found = true;
            break;
          }
        }
        if (!found) removeConfigIDRecursive(new TreePath(menuTreeRoot), cidold.id);
      }

      configIDs = tempConfigIDs;
      rebuildCONF_IDsMenu();
    }

    /**
     * Entfernt alle durch paths bezeichneten Knoten aus dem Baum.
     * 
     * TESTED
     */
    public void nuke(TreePath[] paths)
    {
      for (TreePath path : paths)
      {
        // Root und die Leisten k�nnen nicht gel�scht werden.
        if (path.getPathCount() < 3) continue;
        TreePath parentPath = path.getParentPath();
        Node removedNode = (Node) path.getLastPathComponent();
        Node parentNode = ((Node) parentPath.getLastPathComponent());
        int childIndex = getIndexOfChild(parentNode, removedNode);
        parentNode.removeChild(removedNode);

        for (TreeModelListener listen : listeners)
          listen.treeNodesRemoved(new TreeModelEvent(this, parentPath,
            new int[] { childIndex }, new Object[] { removedNode }));

        if (!parentNode.userModified)
        {
          parentNode.userModified = true;
          for (TreeModelListener listen : listeners)
            listen.treeNodesChanged(new TreeModelEvent(this, parentPath));
        }
      }

    }

    /**
     * F�gt Kopien von den Nodes die durch paths beschrieben sind an index in den
     * Node identifiziert durch parentPath ein.
     * 
     * TESTED
     */
    public void copy(TreePath[] paths, TreePath parentPath, int index)
    {
      Node parentNode = (Node) parentPath.getLastPathComponent();

      for (TreePath path : paths)
      {
        // Root und die Leisten k�nnen nicht kopiert werden.
        if (path.getPathCount() < 3) continue;
        Node sourceNode = (Node) path.getLastPathComponent();
        parentNode.recursiveCopy(sourceNode, index);

        for (TreeModelListener listen : listeners)
        {
          listen.treeNodesInserted(new TreeModelEvent(this, parentPath,
            new int[] { index }, new Object[] { sourceNode }));
        }
        ++index;
      }

      if (!parentNode.userModified)
      {
        parentNode.userModified = true;
        for (TreeModelListener listen : listeners)
          listen.treeNodesChanged(new TreeModelEvent(this, parentPath));
      }
    }

    /**
     * F�gt newNode (was ein neuer unabh�ngiger Node sein muss) als Referenz in den
     * durch parentPath bezeichneten Knoten ein an Position index.
     * 
     * TESTED
     */
    public void insert(Node newNode, TreePath parentPath, int index)
    {
      Node parentNode = (Node) parentPath.getLastPathComponent();

      parentNode.children.add(index, newNode);

      for (TreeModelListener listen : listeners)
      {
        listen.treeNodesInserted(new TreeModelEvent(this, parentPath,
          new int[] { index }, new Object[] { newNode }));
      }

      if (!parentNode.userModified)
      {
        parentNode.userModified = true;
        for (TreeModelListener listen : listeners)
          listen.treeNodesChanged(new TreeModelEvent(this, parentPath));
      }
    }

    /**
     * Nimmt das erste aktuell ausgew�hlte Men� und reinitialisiert es, wobei nur
     * Daten aus {@link MenuManager#defaultConf} verwendet werden, keine aus
     * userConf.
     * 
     */
    public void restoreStandard()
    {
      ConfigThingy noUserConf = new ConfigThingy("wollmuxbarConf");

      TreePath selection = myTree.getSelectionPath();
      if (selection == null) return;
      if (!confirm(
        L.m("�nderungen aufheben?"),
        L.m("Wollen Sie wirklich alle �nderungen\ndes ausgew�hlten Men�s und\naller Untermen�s aufheben?")))
        return;
      if (selection.getPathCount() == 1)
      {
        menuTreeRoot = parseMenuTree(defaultConf, noUserConf);
        for (TreeModelListener listen : listeners)
          listen.treeStructureChanged((new TreeModelEvent(this,
            new Object[] { menuTreeRoot })));
      }
      else if (selection.getPathCount() == 2)
      {
        Node node = (Node) selection.getLastPathComponent();
        String[] menuPath;
        if (node.conf.getName().equals("Menueleiste"))
          menuPath = MENUELEISTE_PATH;
        else
          menuPath = BUTTONLEISTE_PATH;

        ActiveConfigSection leisteSection;
        try
        {
          leisteSection = getActiveConfigSection(menuPath, defaultConf, noUserConf);
        }
        catch (NodeNotFoundException x)
        {
          leisteSection =
            new ActiveConfigSection(new ConfigThingy(node.conf.getName()), true);
        }

        node.children.clear();
        node.userModified = leisteSection.userModified;
        node.conf = new ConfigThingy(leisteSection.conf);
        parseMenuTreeRecursive(node, node.conf, defaultConf, noUserConf,
          new HashSet<String>());
        for (TreeModelListener listen : listeners)
          listen.treeStructureChanged((new TreeModelEvent(this, selection)));
      }
      else
      { // if (selection.getPathCount() > 2)
        Node node = (Node) selection.getLastPathComponent();
        if (!node.isMenuOrBar()) return; // Wir k�nnen nur ganze Men�s
        // wiederherstellen

        String menuId = null;
        try
        {
          menuId = node.conf.get("MENU").toString();
        }
        catch (NodeNotFoundException x)
        {
          // Unm�glich, da der initiale Parse-Durchgang schon getestet hat.
        }

        ActiveConfigSection menuSection;
        try
        {
          menuSection = getActiveConfigSection(new String[] {
            "Menues", menuId, "Elemente" }, defaultConf, noUserConf);
        }
        catch (NodeNotFoundException x)
        {
          menuSection = new ActiveConfigSection(new ConfigThingy("Elemente"), true);
        }

        node.children.clear();
        node.userModified = menuSection.userModified;
        node.conf = new ConfigThingy(menuSection.conf);
        parseMenuTreeRecursive(node, node.conf, defaultConf, noUserConf,
          new HashSet<String>());
        for (TreeModelListener listen : listeners)
          listen.treeStructureChanged((new TreeModelEvent(this, selection)));
      }
    };

    public void deleteSelection()
    {
      TreePath[] paths = myTree.getSelectionPaths();
      if (paths != null) nuke(paths);
    };

    public void newSubMenu()
    {
      if (!allowedInsertPositionSelected(true)) return;
      String menuName =
        JOptionPane.showInputDialog(myFrame, L.m("Name des neuen Men�s"),
          L.m("Neues (Unter)Men�"), JOptionPane.QUESTION_MESSAGE);
      if (menuName == null || menuName.length() == 0) return;
      ConfigThingy conf = new ConfigThingy("");
      conf.add("TYPE").add("menu");
      conf.add("MENU").add(generateMenuId(menuName));
      conf.add("LABEL").add(menuName);
      Node newNode = new Node(menuName, new ArrayList<Node>(), true, conf);

      newNode(newNode, true);
    };

    /**
     * Erzeugt eine MENU ID die in keinem Menues-Abschnitt in
     * {@link MenuManager#userConf} oder {@link MenuManager#defaultConf} bisher
     * vorkommt und auch nirgends im Baum. Die Generierung orientiert sich an name.
     * 
     * TESTED
     */
    private String generateMenuId(String name)
    {
      name = name.replaceAll("\\W", "_");
      String baseid = "mm_" + name;
      for (int count = 0;; ++count)
      {
        String id = baseid;
        if (count > 0) id = id + count;

        try
        {
          if (findMenuRecursive(menuTreeRoot, id)) continue;

          getActiveConfigSection(new String[] {
            "Menues", id }, defaultConf, userConf);
        }
        catch (NodeNotFoundException x)
        {
          return id;
        }
      }
    }

    /**
     * Liefert true gdw, node selbst oder ein Nachfahre ein Men� ist mit
     * MENU-Attribut id.
     * 
     * TESTED
     */
    private boolean findMenuRecursive(Node node, String id)
    {
      // id.equals, NICHT node.menuId().equals, da letzteres null sein kann.
      if (id.equals(node.menuId())) return true;

      for (Node child : node.children)
        if (findMenuRecursive(child, id)) return true;

      return false;
    }

    /**
     * F�gt vor dem ersten ausgew�hlten Element (falls vorhanden und falls erlaubt)
     * einen Separator ein und liefert dessen TreePath zur�ck (oder null falls keiner
     * eingef�gt wurde).
     */
    public TreePath newSeparator()
    {
      ConfigThingy conf = new ConfigThingy("");
      conf.add("TYPE").add("separator");
      Node newNode = new Node(getLabel(conf), new ArrayList<Node>(), false, conf);
      return newNode(newNode, false);
    }

    /**
     * Liefer true, wenn eine erlaubte Einf�geposition selektiert ist.
     * 
     * @param barsAllowed
     *          falls true gilt eine selektierte Men�- oder Buttonleiste als legal.
     */
    private boolean allowedInsertPositionSelected(boolean barsAllowed)
    {
      TreePath selectedPath = myTree.getSelectionPath();
      if (selectedPath == null) return false;
      // Kann auf/in/vor Wurzel nichts einf�gen
      if (selectedPath.getPathCount() < 2) return false;
      if (!barsAllowed && selectedPath.getPathCount() < 3) return false;
      return true;
    }

    /**
     * F�gt vor dem ersten ausgew�hlten Element (falls vorhanden und falls erlaubt)
     * newNode ein (was ein frischer unabh�ngiger Node sein muss) und liefert den
     * TreePath des neuen Knoten zur�ck (oder null falls keiner eingef�gt wurde).
     * 
     * @param appendIfMenuSelected
     *          falls true, so wird wenn ein Men� selektiert ist, ans Ende von diesem
     *          angeh�ngt. Falls false, so wird auch bei selektiertem Men� davor
     *          eingef�gt. In letzterem Fall sind die Men�- und Buttonleiste keine
     *          legalen Einf�gepositionen und diese funktioniert tut nichts.
     * 
     */
    private TreePath newNode(Node newNode, boolean appendIfMenuSelected)
    {
      TreePath selectedPath = myTree.getSelectionPath();
      if (selectedPath == null) return null;
      // Kann vor/auf/in Wurzel nichts einf�gen
      if (selectedPath.getPathCount() < 2) return null;
      if (!appendIfMenuSelected && selectedPath.getPathCount() < 3) return null;

      Node selectedNode = (Node) selectedPath.getLastPathComponent();
      if (appendIfMenuSelected && selectedNode.isMenuOrBar())
      {
        insert(newNode, selectedPath, getChildCount(selectedNode));
        return selectedPath.pathByAddingChild(newNode);
      }
      else
      {
        TreePath parentPath = selectedPath.getParentPath();
        Node parentNode = (Node) parentPath.getLastPathComponent();
        int index = getIndexOfChild(parentNode, selectedNode);
        insert(newNode, parentPath, index);
        return parentPath.pathByAddingChild(newNode);
      }

    };

    /**
     * L�sst den Benutzer in einem FileChooser Dateien ausw�hlen und f�gt f�r diese
     * entsprechende openExt-Men�eintr�ge ein. Wenn derzeit ein Men� selektiert ist,
     * werden die Eintr�ge an dessen Ende angef�gt. Ist ein anderer Eintrag
     * selektiert, so werden die Eintr�ge davor eingef�gt.
     * 
     * TESTED
     */
    public void newFile()
    {
      if (!allowedInsertPositionSelected(true))
      ;

      if (fileChooser == null)
      {
        fileChooser = new JFileChooser(getFileChooserStartDirectory());
      }

      fileChooser.setMultiSelectionEnabled(true);
      fileChooser.setAcceptAllFileFilterUsed(true);
      fileChooser.setFileHidingEnabled(false);
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.setDialogTitle(NEW_FILES_TEXT);
      fileChooser.setSelectedFiles(new File[] {});

      if (fileChooser.showOpenDialog(myFrame) != JFileChooser.APPROVE_OPTION)
        return;

      for (File file : fileChooser.getSelectedFiles())
        newFile(file);
    }

    /**
     * Falls momentan ein Men� selektiert ist, wird ein openExt-Eintrag f�r file an
     * sein Ende gesetzt. Falls was anderes selektiert ist, wird der Eintrag davor
     * eingef�gt.
     * 
     * TESTED
     */
    private void newFile(File file)
    {
      ConfigThingy conf = new ConfigThingy("");
      String ext = "";
      String label = file.getName();
      int dotIdx = label.lastIndexOf('.');
      if (dotIdx >= 0)
      {
        ext = label.substring(dotIdx + 1);
        label = label.substring(0, dotIdx);
      }
      if (ext.length() == 0) ext = "<noext>";
      conf.add("LABEL").add(label);
      conf.add("TYPE").add("button");
      conf.add("ACTION").add("openExt");
      conf.add("EXT").add(ext);
      String url = getRelativeURLifPossible(file);
      conf.add("URL").add(url);
      Node newNode = new Node(label, new ArrayList<Node>(), false, conf);

      newNode(newNode, true);
    }

    public void newSomething()
    {
      ConfigThingy conf = new ConfigThingy("");
      conf.add("TYPE").add("button");
      conf.add("ACTION").add("abort");
      conf.add("LABEL").add("");
      Node newNode = new Node("", new ArrayList<Node>(), false, conf);
      TreePath path = newNode(newNode, true);
      if (path == null) return;
      myTree.setSelectionPath(path);
      editProperties();
    }

    /**
     * �ffnet einen modalen Dialog �ber den das zugeh�rige ConfigThingy des ersten
     * ausgew�hlten Elements des Baumes bearbeitet werden kann.
     * 
     * TESTED
     */
    public void editProperties()
    {
      final TreePath selectedPath = myTree.getSelectionPath();
      if (selectedPath == null) return;
      // Kann Wurzel oder ...leiste nicht bearbeiten
      if (selectedPath.getPathCount() < 3) return;
      final Node selectedNode = (Node) selectedPath.getLastPathComponent();
      final ConfigThingy originalConf = selectedNode.conf;
      UIElementConfigThingyEditor.showEditDialog(myFrame, originalConf,
        new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            if (e.getActionCommand().equals("OK"))
            {
              ConfigThingy changedConf = (ConfigThingy) e.getSource();
              propertiesHaveBeenEdited(selectedPath, selectedNode, originalConf,
                changedConf);
            }
          }
        });
    }

    private void propertiesHaveBeenEdited(TreePath selectedPath, Node selectedNode,
        ConfigThingy originalConf, ConfigThingy changedConf)
    {
      if (changedConf == null
        || originalConf.stringRepresentation().equals(
          changedConf.stringRepresentation())) return;

      selectedNode.conf = changedConf;
      selectedNode.label = getLabel(changedConf);

      nodeHasBeenModified(selectedPath);
    }

    /**
     * Markiert den Elternknoten von selectedPath als userModified und benachrichtigt
     * die Listener sowohl �ber die �nderung von selectedPath als auch von dem
     * Elternpfad.
     */
    private void nodeHasBeenModified(TreePath selectedPath)
    {
      TreePath parentPath = selectedPath.getParentPath();
      Node parentNode = (Node) parentPath.getLastPathComponent();
      if (!parentNode.userModified)
      {
        parentNode.userModified = true;
        for (TreeModelListener listen : listeners)
          listen.treeNodesChanged(new TreeModelEvent(this, parentPath));
      }
      for (TreeModelListener listen : listeners)
        listen.treeNodesChanged(new TreeModelEvent(this, selectedPath));
    }
  }

  /**
   * Zeigt einen modalen Dialog an mit Titel title und Frage message und liefert
   * true, wenn der Benutzer Ja anw�hlt.
   * 
   */
  private boolean confirm(String title, String message)
  {
    return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(myFrame, message,
      title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
  }

  /**
   * Bringt eine Warnung, wenn nichts ausgew�hlt ist.
   * 
   * @return true wenn die Warnung gebracht wurde (d.h. wenn nichts ausgew�hlt ist).
   * 
   */
  private boolean warnIfNoSelection()
  {
    if (myTree.getSelectionCount() == 0)
    {
      JOptionPane.showMessageDialog(myFrame,
        L.m("Bitte w�hlen Sie mindestens einen Eintrag aus!"),
        L.m("Nichts ausgew�hlt"), JOptionPane.INFORMATION_MESSAGE);
      return true;
    }
    return false;
  }

  private class MyTransferHandler extends TransferHandler
  {
    public int getSourceActions(JComponent c)
    {
      if (c == myTree)
        return COPY_OR_MOVE;
      else
        return NONE;
    }

    protected Transferable createTransferable(JComponent c)
    {
      if (c != myTree) return null;
      TreePath[] selection = myTree.getSelectionPaths();

      // We cannot drag the root or the 2 nodes below it
      for (TreePath path : selection)
        if (path.getPathCount() < 3) return null;

      return new MyTransferable(selection);
    }

    protected void exportDone(JComponent c, Transferable data, int action)
    {
      if (action == MOVE)
      {
        TreePath[] selection = myTree.getSelectionPaths();
        myTreeModel.nuke(selection);
      }
    }

    /**
     * Liefert true gdw path ein Nachfahre eines Favoriten-Men�-Knotens oder selbst
     * so ein Knoten ist.
     */
    private boolean isInFavoritesMenu(TreePath path)
    {
      for (int i = path.getPathCount() - 1; i > 1; --i)
      {
        if (((Node) path.getPathComponent(i)).isFavoritesMenu()) return true;
      }
      return false;
    }

    public boolean canImport(TransferHandler.TransferSupport trans)
    {
      try
      {
        MyTransferable transferable =
          (MyTransferable) trans.getTransferable().getTransferData(
            TRANSFER_DATAFLAVORS[0]);

        // Feststellen, ob alle markierten Elemente in einem Favoriten-Men� sind.
        TreePath[] sources = transferable.getDraggedStuff();
        boolean allinfavorites = true;
        for (TreePath path : sources)
        {
          if (!isInFavoritesMenu(path))
          {
            allinfavorites = false;
            break;
          }
        }

        JTree.DropLocation location = (JTree.DropLocation) trans.getDropLocation();
        TreePath dropPath = location.getPath();
        // kann weder auf noch in wurzel was droppen
        if (dropPath.getPathCount() < 2) return false;

        // Falls das dropTarget innerhalb von mindestens einer der Selektionen liegt,
        // d�rfen wir nicht droppen, weil sonst der Node gel�scht w�rde
        for (TreePath path : sources)
        {
          if (path.isDescendant(dropPath)) return false;
        }

        // Falls wir von au�erhalb eines Favoriten-Men�s in ein solches droppen, dann
        // immer COPY nehmen.
        if (isInFavoritesMenu(dropPath) && !allinfavorites)
          if (trans.isDrop()) trans.setDropAction(COPY);

        return true;
      }
      catch (Exception x)
      {
        return false;
      }
    }

    public boolean importData(TransferHandler.TransferSupport trans)
    {
      if (!canImport(trans)) return false;

      JTree.DropLocation location = (JTree.DropLocation) trans.getDropLocation();
      int childIndex = location.getChildIndex();
      TreePath dropPath = location.getPath();
      Node dropTarget = (Node) dropPath.getLastPathComponent();

      // Falls Drop auf einem Objekt, �bersetzen in Einf�gung dahinter bzw. am Ende
      if (childIndex < 0)
      {
        if (!dropTarget.isMenuOrBar())
        {
          Node oldDropTarget = dropTarget;
          dropPath = dropPath.getParentPath();
          dropTarget = (Node) dropPath.getLastPathComponent();
          childIndex = myTreeModel.getIndexOfChild(dropTarget, oldDropTarget);
        }
        else
          childIndex = dropTarget.children.size();
      }

      int action = trans.getDropAction();
      if (action == MOVE || action == COPY)
      {
        try
        {
          MyTransferable transferable =
            (MyTransferable) trans.getTransferable().getTransferData(
              TRANSFER_DATAFLAVORS[0]);
          TreePath[] paths = transferable.getDraggedStuff();
          myTreeModel.copy(paths, dropPath, childIndex);
          return true;
        }
        catch (Exception x)
        {
          Logger.error(x);
        }
      }
      return false;
    }
  }

  private class MyTransferable implements Transferable
  {
    private TreePath[] selection;

    public TreePath[] getDraggedStuff()
    {
      return selection;
    }

    public MyTransferable(TreePath[] selection)
    {
      this.selection = selection;
    }

    public DataFlavor[] getTransferDataFlavors()
    {
      return TRANSFER_DATAFLAVORS;
    }

    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, IOException
    {
      if (flavor.equals(TRANSFER_DATAFLAVORS[0]))
      {
        return this;
      }
      else if (flavor.equals(TRANSFER_DATAFLAVORS[1]))
      {
        // FIXME: Wenn Export des Baums implementiert ist, hier nachr�sten
        return new ByteArrayInputStream("FIXME".getBytes("utf-8"));
      }
      else
        return null;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
      for (DataFlavor flavor2 : TRANSFER_DATAFLAVORS)
        if (flavor2.equals(flavor)) return true;
      return false;
    }

  }

  private class MyMouseListener implements MouseListener
  {
    public void mouseClicked(MouseEvent e)
    {}

    public void mouseEntered(MouseEvent e)
    {}

    public void mouseExited(MouseEvent e)
    {}

    public void mousePressed(MouseEvent e)
    {
      maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e)
    {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e)
    {
      if (e.isPopupTrigger())
      {
        Point p =
          SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(),
            myTree);
        TreePath path = myTree.getClosestPathForLocation(p.x, p.y);
        if (path != null)
        {
          myTree.setSelectionPath(path);
          Rectangle bounds = myTree.getPathBounds(path);
          editMenuPopup.show(myTree, e.getX() + 16, bounds.y + bounds.height);
        }
      }
    }
  }

  private class MyWindowListener implements WindowListener
  {
    public void windowActivated(WindowEvent e)
    {}

    public void windowClosed(WindowEvent e)
    {}

    public void windowClosing(WindowEvent e)
    {
      closeAfterQuestion();
    }

    public void windowDeactivated(WindowEvent e)
    {}

    public void windowDeiconified(WindowEvent e)
    {}

    public void windowIconified(WindowEvent e)
    {}

    public void windowOpened(WindowEvent e)
    {}

  }

  private void closeAfterQuestion()
  {
    int answer =
      JOptionPane.showConfirmDialog(myFrame,
        L.m("Sie verlassen den Men�-Manager.\nWollen Sie das Men� speichern?"),
        L.m("Men�-Manager verlassen?"), JOptionPane.YES_NO_CANCEL_OPTION,
        JOptionPane.QUESTION_MESSAGE);

    if (answer == JOptionPane.YES_OPTION)
    {
      doSave();
      dispose();
    }
    else if (answer == JOptionPane.NO_OPTION)
      dispose();
    else
      return;
  }

  private void dispose()
  {
    /*
     * Wegen folgendem Java Bug (WONTFIX)
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304 sind die folgenden
     * 3 Zeilen n�tig, damit die GUI gc'ed werden kann. Die Befehle sorgen daf�r,
     * dass kein globales Objekt (wie z.B. der Keyboard-Fokus-Manager) indirekt �ber
     * den JFrame die GUI kennt.
     */
    myFrame.removeWindowListener(oehrchen);
    myFrame.getContentPane().remove(0);
    myFrame.setJMenuBar(null);

    myFrame.dispose();
    myFrame = null;

    if (finishedAction != null)
      finishedAction.actionPerformed(new ActionEvent(this, 0, ""));
  }

  private static class Node
  {
    /**
     * Die Beschriftung des Knotens im Baum.
     */
    public String label;

    /**
     * Die Kindknoten (nie null, aber wom�glich leer).
     */
    public List<Node> children;

    /**
     * true gdw die Liste der Kindknoten aus der Benutzerkonfiguration kommt und
     * nicht aus der globalen Systemkonfiguration. ACHTUNG! Wenn das Label eines
     * Knotens ge�ndert wird, setzt das nicht dessen userModified-Flag sondern das,
     * des Vaterknotens. Dies liegt daran, dass das LABEL im aufrufenden Men�
     * vergeben wird und nicht im aufgerufenen Men�.
     */
    public boolean userModified;

    /**
     * Das ConfigThingy das zu dem Node geh�rt. Mit Ausnahme der Wurzel und beiden
     * darunterliegenden Knoten f�r Button- und Men�leiste, ist dies ein
     * UIElement-beschreibendes ConfigThingy. ACHTUNG! Dies ist eine von defaultConf
     * bzw. userConf unabh�ngige Kopie. An ihr darf alles ver�ndert werden au�er bei
     * Men�s das MENU-Attribut (weil sonst "Standard wiederherstellen" nicht mehr
     * funktionieren w�rde).
     */
    public ConfigThingy conf;

    public Node(String label, List<Node> children, boolean userModified,
        ConfigThingy conf)
    {
      this.label = label;
      this.children = children;
      this.userModified = userModified;
      this.conf = new ConfigThingy(conf);
    }

    public String toString()
    { // TESTED
      ConfigThingy cids = conf.query("CONF_ID", 1);
      if (cids.count() > 0)
      {
        boolean comma = false;
        StringBuilder buffy = new StringBuilder("[");

        for (ConfigThingy cidgroup : cids)
        {
          for (ConfigThingy cid : cidgroup)
          {
            if (comma)
              buffy.append(", ");
            else
              comma = true;
            buffy.append(cid.toString());
          }
        }
        buffy.append("] ");

        buffy.append(label);
        if (userModified) buffy.append('*');

        return buffy.toString();
      }

      return userModified ? label + "*" : label;
    }

    /**
     * Liefert true gdw diese Node ein Men� oder die Button- oder die Men�leiste
     * darstellt.
     */
    public boolean isMenuOrBar()
    {
      try
      {
        return conf.getName().length() > 0
          || conf.get("TYPE").toString().equals("menu");
      }
      catch (NodeNotFoundException x)
      {
        return false;
      }
    }

    /**
     * Liefert true gdw dies ein Men� ist, in dem der Benutzer Favoriten ablegt.
     */
    public boolean isFavoritesMenu()
    {
      try
      {
        return conf.get("FAVO").toString().equals("1");
      }
      catch (NodeNotFoundException x)
      {
        return false;
      }
    }

    /**
     * Liefert den Wert des MENU-Attributs oder null, falls es sich bei diesem Node
     * nicht um ein Men� handelt.
     */
    public String menuId()
    {
      try
      {
        return conf.get("MENU").toString();
      }
      catch (NodeNotFoundException x)
      {
        return null;
      }
    }

    /**
     * Entfernt child aus der Liste der Kinder von this.
     * 
     */
    public void removeChild(Node child)
    {
      children.remove(child);
    }

    /**
     * F�gt eine Kopie von sourceNode unter Index index in die Kinder von this ein.
     * Falls sourceNode Kinder hat, so werden diese rekursiv in den kopierten Knoten
     * kopiert.
     * 
     */
    public void recursiveCopy(Node sourceNode, int index)
    {
      children.add(index, new Node(sourceNode));
    }

    /**
     * Copy constructor (deep copy).
     */
    public Node(Node orig)
    {
      this(orig.label, new ArrayList<Node>(), orig.userModified, new ConfigThingy(
        orig.conf));
      for (Node child : orig.children)
        children.add(new Node(child));
    }

    /**
     * F�gt diesem Knoten die CONF_ID id hinzu falls nicht bereits vorhanden. Bei
     * Knoten, die keine CONF_ID unterst�tzen wird ohne etwas zu tun false geliefert.
     * 
     * @return true wenn id neu hinzugef�gt wurde (d.h. nicht bereits vorhanden war)
     * 
     * TESTED
     */
    public boolean addConfID(String id)
    {
      // Falls wir einer der oberen Knoten sind, die kein CONF_ID unterst�tzen.
      if (conf.getName().length() > 0) return false;

      ConfigThingy conf_id_conf = conf.query("CONF_ID");
      if (conf_id_conf.count() == 0) conf_id_conf.addChild(conf.add("CONF_ID"));

      for (ConfigThingy cidGroup : conf_id_conf)
        for (ConfigThingy cid : cidGroup)
          if (cid.getName().equals(id)) return false;

      try
      {
        conf_id_conf.getFirstChild().add(id);
      }
      catch (NodeNotFoundException x)
      {
        // Kann nicht passieren, weil conf_id_conf mindestens 1 Kind hat
      }

      return true;
    }

    /**
     * Entfernt aus diesem Knoten die CONF_ID id falls vorhanden.
     * 
     * @return true wenn eine CONF_ID entfernt wurde
     * 
     * TESTED
     */
    public boolean removeConfID(String id)
    {
      boolean haveRemoved = false;
      Iterator<ConfigThingy> iter = conf.iterator();
      while (iter.hasNext())
      {
        ConfigThingy subConf = iter.next();
        if (subConf.getName().equals("CONF_ID"))
        {
          Iterator<ConfigThingy> subIter = subConf.iterator();
          while (subIter.hasNext())
            if (subIter.next().getName().equals(id))
            {
              subIter.remove();
              haveRemoved = true;
            }
          if (subConf.count() == 0) iter.remove();
        }
      }

      return haveRemoved;
    }
  }

  /**
   * Parst die WollMuxBarKonfigurationen-Abschnitte und liefert eine Liste mit den
   * vorhandenen ConfigIDs zur�ck.
   * 
   * TESTED
   */
  static List<ConfigID> parseConfigIDs(ConfigThingy defaultConf,
      ConfigThingy userConf)
  {
    List<ConfigID> configIDs = new Vector<ConfigID>();
    ConfigThingy[] results =
      new ConfigThingy[] {
        defaultConf.query("WollMuxBarKonfigurationen", 1).query("Labels", 2),
        userConf.query("WollMuxBarKonfigurationen", 1).query("Labels", 2) };
    for (int i = 0; i < 2; ++i)
    {
      ConfigThingy resConf = results[i];
      for (ConfigThingy labelsConf : resConf)
        for (ConfigThingy labelConf : labelsConf)
        {
          String id;
          try
          {
            id = labelConf.get("CONF_ID").toString();
          }
          catch (NodeNotFoundException x)
          {
            Logger.error(L.m("Ein WollMuxBarKonfigurationen/Labels Eintrag besitzt keine CONF_ID"));
            continue;
          }

          String label;
          try
          {
            label = labelConf.get("LABEL").toString();
          }
          catch (NodeNotFoundException x)
          {
            Logger.error(L.m("Ein WollMuxBarKonfigurationen/Labels Eintrag besitzt kein LABEL"));
            continue;
          }

          ConfigID confid = null;
          for (ConfigID confid2 : configIDs)
            if (confid2.id.equals(id))
            {
              confid = confid2;
              break;
            }

          if (confid == null)
          {
            if (i == 0)
              configIDs.add(new ConfigID(id, label, null));
            else
              configIDs.add(new ConfigID(id, null, label));
          }
          else
          {
            if (i == 0)
              confid.label_default = label;
            else
              confid.label_user = label;
          }
        }
    }

    return configIDs;
  }

  /**
   * Parst die Symbolleisten, Briefkopfleiste, Menueleiste und Menues-Abschnitte und
   * liefert den Wurzel-Knoten des Ergebnisbaumes zur�ck.
   * 
   * TESTED
   */
  private static Node parseMenuTree(ConfigThingy defaultConf, ConfigThingy userConf)
  {
    ActiveConfigSection buttonleisteSection;
    try
    {
      buttonleisteSection =
        getActiveConfigSection(BUTTONLEISTE_PATH, defaultConf, userConf);
    }
    catch (NodeNotFoundException x)
    {
      buttonleisteSection =
        new ActiveConfigSection(new ConfigThingy("Briefkopfleiste"), true);
    }

    ActiveConfigSection menueleisteSection;
    try
    {
      menueleisteSection =
        getActiveConfigSection(MENUELEISTE_PATH, defaultConf, userConf);
    }
    catch (NodeNotFoundException x)
    {
      menueleisteSection =
        new ActiveConfigSection(new ConfigThingy("Menueleiste"), true);
    }

    Node root =
      new Node("Wurzel", new ArrayList<Node>(), false, new ConfigThingy("Wurzel"));

    Node buttonleisteNode =
      new Node(L.m("Buttonleiste"), new ArrayList<Node>(),
        buttonleisteSection.userModified, buttonleisteSection.conf);
    root.children.add(buttonleisteNode);

    Node menueleisteNode =
      new Node(L.m("Men�leiste"), new ArrayList<Node>(),
        menueleisteSection.userModified, menueleisteSection.conf);
    root.children.add(menueleisteNode);

    parseMenuTreeRecursive(buttonleisteNode, buttonleisteNode.conf, defaultConf,
      userConf, new HashSet<String>());
    parseMenuTreeRecursive(menueleisteNode, menueleisteNode.conf, defaultConf,
      userConf, new HashSet<String>());

    return root;
  }

  /**
   * Wertet die Kinder von menuConf aus, die die �bliche Form f�r UIElemente haben
   * m�ssen. F�r jedes so beschriebene UIElement wird in node.children ein weiterer
   * Node hinzugef�gt. Falls der TYPE des UIElements "menu" lautet, so wird dieses
   * Men� aus defaultConf bzw. userConf herausgesucht (die letzte Instanz gewinnt,
   * wobei userConf als nach defaultConf stehend z�hlt) und rekursiv verarbeitet.
   * 
   * @param alreadySeen
   *          Um rekursive Men�strukturen zu unterbinden wird hier bei jedem
   *          Rekursschritt die ID des rekursiv betretenen Men�s hineingesteckt und
   *          der Aufbau wird abgebrochen wenn ein Men� hier bereits vorhanden ist.
   * 
   * TESTED
   */
  private static void parseMenuTreeRecursive(Node node, ConfigThingy menuConf,
      ConfigThingy defaultConf, ConfigThingy userConf, Set<String> alreadySeen)
  {
    for (ConfigThingy conf : menuConf)
    {
      String type;
      try
      {
        type = conf.get("TYPE").toString();
      }
      catch (NodeNotFoundException x)
      {
        Logger.error(UIELEMENT_WITHOUT_TYPE_ERR);
        continue;
      }

      String label = getLabel(conf);

      if (type.equals("menu"))
      {
        String menuId;
        try
        {
          menuId = conf.get("MENU").toString();
        }
        catch (NodeNotFoundException x)
        {
          Logger.error(L.m("'menu' Men�eintrag ohne MENU-Attribut gefunden"));
          continue;
        }

        if (alreadySeen.contains(menuId))
        {
          Logger.error(L.m(
            "Men� '%1' enth�lt sich direkt oder indirekt selbst als Untermen�",
            menuId));
          continue;
        }

        try
        {
          ActiveConfigSection menuSection = getActiveConfigSection(new String[] {
            "Menues", menuId, "Elemente" }, defaultConf, userConf);
          Node menuNode =
            new Node(label, new ArrayList<Node>(), menuSection.userModified, conf);

          node.children.add(menuNode);

          alreadySeen.add(menuId);
          parseMenuTreeRecursive(menuNode, menuSection.conf, defaultConf, userConf,
            alreadySeen);
          alreadySeen.remove(menuId);
        }
        catch (NodeNotFoundException x)
        {
          Logger.error(L.m("Men� '%1' nicht definiert", menuId));
          continue;
        }
      }
      else
      { // if (!type.equals("menu"))
        node.children.add(new Node(label, new ArrayList<Node>(), false, conf));
      }
    }
  }

  private void save()
  {
    if (!confirm(L.m("Men� speichern?"),
      L.m("Wollen Sie das neue Men� wirklich speichern?"))) return;

    doSave();
  }

  private void doSave()
  {
    ConfigThingy conf = userConf;
    Iterator<ConfigThingy> iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy dingy = iter.next();
      String name = dingy.getName();
      if (name.equals("Symbolleisten") || name.equals("Menues")
        || name.equals("Menueleiste"))
      {
        iter.remove();
      }
      else if (name.equals("WollMuxBarKonfigurationen"))
      {
        Iterator<ConfigThingy> subIter = dingy.iterator();
        while (subIter.hasNext())
        {
          if (subIter.next().getName().equals("Labels")) subIter.remove();
        }
        if (dingy.count() == 0) iter.remove();
      }
    }

    Node buttonleiste = menuTreeRoot.children.get(0);
    if (buttonleiste.userModified)
    {
      ConfigThingy subconf = conf.add("Symbolleisten").add("Briefkopfleiste");
      for (Node child : buttonleiste.children)
        subconf.addChild(new ConfigThingy(child.conf));
    }

    Node menueleiste = menuTreeRoot.children.get(1);
    if (menueleiste.userModified)
    {
      ConfigThingy subconf = conf.add("Menueleiste");
      for (Node child : menueleiste.children)
        subconf.addChild(new ConfigThingy(child.conf));
    }

    addUserModifiedMenuesRecursive(menuTreeRoot, conf.add("Menues"));

    boolean mustWriteConfigIDs = false;
    for (ConfigID cid : configIDs)
    {
      // Sicherstellen, dass keine �berfl�ssigen label_user vorhanden sind
      if (cid.label_user != null && cid.label_user.equals(cid.label_default))
        cid.label_user = null;
      if (cid.label_user != null)
      {
        mustWriteConfigIDs = true;
        // Nicht break; weil das Aufr�umen der label_user f�r alle ben�tigt wird
      }
    }

    if (mustWriteConfigIDs)
    {
      ConfigThingy wmbk;
      try
      {
        wmbk = conf.query("WollMuxBarKonfigurationen").getLastChild();
      }
      catch (NodeNotFoundException x1)
      {
        wmbk = conf.add("WollMuxBarKonfigurationen");
      }

      ConfigThingy labelsConf = wmbk.add("Labels");
      for (ConfigID cid : configIDs)
        if (cid.label_user != null)
        {
          ConfigThingy entry = labelsConf.add("");
          entry.add("CONF_ID").add(cid.id);
          entry.add("LABEL").add(cid.label_user);
        }
    }

    File wollmuxbarConfFile =
      new File(WollMuxFiles.getWollMuxDir(), WollMuxBar.WOLLMUXBAR_CONF);
    try
    {
      WollMuxFiles.writeConfToFile(wollmuxbarConfFile, userConf);
    }
    catch (Exception x)
    {
      Logger.error(x);
      JOptionPane.showMessageDialog(myFrame, L.m(
        "Beim Speichern ist ein Fehler aufgetreten:\n%1", x.getMessage()),
        L.m("Fehler beim Speichern"), JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * F�gt conf f�r jedes Men� im Teilbaum der node als Wurzel hat, wenn es
   * userModified gesetzt hat, einen Abschnitt f�r dieses Men� hinzu.
   * 
   * TESTED
   */
  private void addUserModifiedMenuesRecursive(Node node, ConfigThingy conf)
  {
    String menu = null;
    if (node.userModified)
    {
      try
      {
        menu = node.conf.get("MENU", 1).toString();
      }
      catch (Exception x)
      {}

      if (menu != null)
      {
        ConfigThingy subconf = conf.add(menu).add("Elemente");
        for (Node child : node.children)
          subconf.addChild(new ConfigThingy(child.conf));
      }
    }

    for (Node child : node.children)
      addUserModifiedMenuesRecursive(child, conf);
  }

  /**
   * Liefert ein f�r das UIElement conf passendes Label f�r die Anzeige im Baum.
   * 
   */
  private static String getLabel(ConfigThingy conf)
  {
    String type = "";
    try
    {
      type = conf.get("TYPE").toString();
    }
    catch (NodeNotFoundException x)
    {
      Logger.error(UIELEMENT_WITHOUT_TYPE_ERR);
    }

    // Use type as default label (think of "glue" and "separator")
    String label = "--- " + type + " ---";
    try
    {
      if (!type.equals("separator") && !type.equals("glue"))
        label = conf.get("LABEL").toString();
    }
    catch (Exception x)
    {}
    return label;
  }

  /**
   * Sucht das letzte vorkommen eines Abschnitts sectionPath[N] der verschachtelt ist
   * Abschnitten sectionPath[N-1]...sectionPath[0] (wobei bei vorkommen von
   * Abschnitten auf verschiedenen tiefen des Baumes nur die oberste Ebene mit einem
   * sectionName-Abschnitt betrachtet wird (selbes verhalten wie
   * {@link ConfigThingy#query(String)}). Die userConf gilt als hinter der
   * defaultConf stehend.
   * 
   * @throws NodeNotFoundException
   *           falls kein entsprechender Abschnitt gefunden wurde.
   * 
   * TESTED
   */
  private static ActiveConfigSection getActiveConfigSection(String[] sectionPath,
      ConfigThingy defaultConf, ConfigThingy userConf) throws NodeNotFoundException
  {
    if (sectionPath.length == 0) throw new NodeNotFoundException();
    ConfigThingy conf = userConf;
    for (int i = 0; i < sectionPath.length; ++i)
      conf = conf.query(sectionPath[i]);
    if (conf.count() > 0) return new ActiveConfigSection(conf.getLastChild(), true);

    conf = defaultConf;
    for (int i = 0; i < sectionPath.length; ++i)
      conf = conf.query(sectionPath[i]);
    if (conf.count() > 0)
      return new ActiveConfigSection(conf.getLastChild(), false);

    StringBuilder buffy = new StringBuilder();
    for (String name : sectionPath)
      buffy.append("/" + name);

    throw new NodeNotFoundException(L.m(
      "Kein Konfigurationsabschnitt '%1' gefunden", buffy.toString()));
  }

  /**
   * Steht f�r einen Konfigurationsabschnitt in der wollmux,conf der nach dem
   * Letzter-Gewinnt-Prinzip bestimmt wurde.
   */
  private static class ActiveConfigSection
  {
    /**
     * Die Wurzel des Konfigurationsabschnitts.
     */
    public ConfigThingy conf;

    /**
     * true gdw der Konfigurationsabschnitt aus der Benutzerkonfiguration und nicht
     * aus der globalen Systemkonfiguration kommt.
     */
    public boolean userModified;

    public ActiveConfigSection(ConfigThingy conf, boolean userModified)
    {
      this.conf = conf;
      this.userModified = userModified;
    }
  }

  static class ConfigID implements Comparable<ConfigID>
  {
    public String id;

    public String label_default;

    public String label_user;

    private ConfigID(String id, String label_default, String label_user)
    {
      this.id = id;
      this.label_default = label_default;
      this.label_user = label_user;
    }

    public String toString()
    {
      return id + ": [" + label_default + "] [" + label_user + "]";
    }

    public int compareTo(ConfigID o)
    {
      return id.compareTo(o.id);
    }
  }

  public static void main(String[] args)
  {
    WollMuxFiles.setupWollMuxDir();

    ConfigThingy wollmuxConf = WollMuxFiles.getWollmuxConf();

    ConfigThingy wollmuxbarConf = null;
    File wollmuxbarConfFile =
      new File(WollMuxFiles.getWollMuxDir(), WollMuxBar.WOLLMUXBAR_CONF);
    if (wollmuxbarConfFile.exists())
    {
      try
      {
        wollmuxbarConf =
          new ConfigThingy("wollmuxbarConf", wollmuxbarConfFile.toURI().toURL());
      }
      catch (Exception x)
      {
        Logger.error(
          L.m("Fehler beim Lesen von '%1'", wollmuxbarConfFile.toString()), x);
      }
    }

    if (wollmuxbarConf == null) wollmuxbarConf = new ConfigThingy("wollmuxbarConf");

    new MenuManager(wollmuxConf, wollmuxbarConf, new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        System.out.println("Finished");
      }
    });
  }

}
