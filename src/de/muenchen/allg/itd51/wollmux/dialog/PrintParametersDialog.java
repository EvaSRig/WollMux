package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.frame.DispatchResultEvent;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.frame.XNotifyingDispatch;
import com.sun.star.lang.EventObject;
import com.sun.star.text.XTextDocument;
import com.sun.star.view.XPrintable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.DispatchHandler;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;

public class PrintParametersDialog
{
  /**
   * Kommando-String, der dem closeActionListener �bermittelt wird, wenn der Dialog
   * �ber den Drucken-Knopf geschlossen wird.
   */
  public static final String CMD_SUBMIT = "submit";

  /**
   * Kommando-String, der dem closeActionListener �bermittelt wird, wenn der Dialog
   * �ber den Abbrechen oder "X"-Knopf geschlossen wird.
   */
  public static final String CMD_CANCEL = "cancel";

  private XTextDocument doc;

  private boolean showCopyCount;

  private JDialog dialog;

  private ActionListener closeActionListener;

  private JTextField printerNameField;

  private JSpinner copyCountSpinner;

  private PageRangeType currentPageRangeType = null;

  private String currentPageRangeValue = null;

  private WindowListener myWindowListener = new WindowListener()
  {

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
      abort(CMD_CANCEL);
    }

    public void windowOpened(WindowEvent e)
    {
    }
  };

  /**
   * TODO: dok
   * 
   * @param doc
   * @param showCopyCount
   * @param listener
   */
  public PrintParametersDialog(XTextDocument doc, boolean showCopyCount,
      ActionListener listener)
  {
    this.doc = doc;
    this.showCopyCount = showCopyCount;
    this.closeActionListener = listener;
    createGUI();
  }

  /**
   * Mini-Klasse f�r den R�ckgabewert eines Seitenbereichs.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static class PageRange
  {
    public final PageRangeType pageRangeType;

    public final String pageRangeValue;

    public PageRange(PageRangeType pageRangeType, String pageRangeValue)
    {
      this.pageRangeType = pageRangeType;
      this.pageRangeValue = pageRangeValue;
    }

    public String toString()
    {
      return "PageRange(" + pageRangeType + ", '" + pageRangeValue + "')";
    }
  }

  /**
   * Definiert die in diesem Dialog m�glichen Einstellungen zur Auswahl des
   * Seitenbereichs.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static enum PageRangeType {
    // TODO: Formatierung nochmal anpassen....
    ALL(L.m("Alles")), USER_DEFINED(L.m("Seiten"), "1,3,5,10-100<etwasPlatz>",
        L.m("M�gliche Eingaben sind z.B. '1', '2-5' oder '1,3,5'")), CURRENT_PAGE(
        L.m("Aktuelle Seite")), CURRENT_AND_FOLLOWING(
        L.m("Aktuelle Seite bis Dokumentende"));

    public final String label;

    public final boolean hasAdditionalTextField;

    public final String additionalTextFieldPrototypeDisplayValue;

    public final String additionalTextFieldHint;

    private PageRangeType(String label)
    {
      this.label = label;
      this.hasAdditionalTextField = false;
      this.additionalTextFieldPrototypeDisplayValue = null;
      this.additionalTextFieldHint = "";
    }

    private PageRangeType(String label,
        String additionalTextFieldPrototypeDisplayValue,
        String additionalTextFieldHint)
    {
      this.label = label;
      this.hasAdditionalTextField = true;
      this.additionalTextFieldPrototypeDisplayValue = additionalTextFieldPrototypeDisplayValue;
      this.additionalTextFieldHint = additionalTextFieldHint;
    }
  };

  /**
   * Liefert ein PageRange-Objekt zur�ck, das Informationen �ber den aktuell
   * ausgew�hlten Druckbereich enth�lt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public PageRange getPageRange()
  {
    return new PageRange(currentPageRangeType, currentPageRangeValue);
  }

  /**
   * Liefert die Anzahl in der GUI eingestellter Kopien als Short zur�ck; Zeigt der
   * Dialog kein Elemente zur Eingabe der Kopien an, oder ist die Eingabe keine
   * g�ltige Zahl, so wird new Short((short) 1) zur�ck geliefert.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public Short getCopyCount()
  {
    try
    {
      return new Short(copyCountSpinner.getValue().toString());
    }
    catch (NumberFormatException e)
    {
      return new Short((short) 1);
    }
  }

  private void createGUI()
  {
    dialog = new JDialog();
    dialog.setTitle(L.m("Einstellungen f�r den Druck"));
    dialog.addWindowListener(myWindowListener);
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    dialog.add(panel);

    Box vbox = Box.createVerticalBox();
    Box hbox;

    hbox = Box.createHorizontalBox();
    hbox.setBorder(BorderFactory.createTitledBorder(
      BorderFactory.createRaisedBevelBorder(), L.m("Drucker")));
    hbox.add(new JLabel(L.m("Name")));
    hbox.add(Box.createHorizontalStrut(10));
    printerNameField = new JTextField(" " + getCurrentPrinterName() + " ");
    printerNameField.setEditable(false);
    hbox.add(printerNameField);
    hbox.add(Box.createHorizontalStrut(10));
    hbox.add(Box.createHorizontalGlue());
    JButton printerSettingsButton = new JButton(
      L.m("Drucker wechseln/einrichten..."));
    printerSettingsButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        showPrintSettingsDialog();
      }
    });
    hbox.add(printerSettingsButton);
    vbox.add(hbox);

    hbox = Box.createHorizontalBox();
    Box vboxPageRange = Box.createVerticalBox();
    vboxPageRange.setBorder(BorderFactory.createTitledBorder(
      BorderFactory.createEtchedBorder(), L.m("Druckbereich")));
    Box vboxCopies = Box.createVerticalBox();
    vboxCopies.setBorder(BorderFactory.createTitledBorder(
      BorderFactory.createEtchedBorder(), L.m("Kopien")));
    hbox.add(vboxPageRange);
    if (showCopyCount) hbox.add(vboxCopies);
    vbox.add(hbox);

    // JRadio-Buttons f�r den Druckbereich erzeugen
    ButtonGroup pageRangeButtons = new ButtonGroup();
    JRadioButton firstButton = null;
    for (final PageRangeType t : PageRangeType.values())
    {
      hbox = Box.createHorizontalBox();

      final JTextField additionalTextfield;
      if (t.hasAdditionalTextField)
      {
        additionalTextfield = new JTextField(
          "" + t.additionalTextFieldPrototypeDisplayValue);
        DimAdjust.fixedSize(additionalTextfield);
        DimAdjust.fixedMaxSize(additionalTextfield, 0, 0);
        additionalTextfield.setToolTipText(t.additionalTextFieldHint);
        additionalTextfield.setText("");
        additionalTextfield.getDocument().addDocumentListener(new DocumentListener()
        {
          public void changedUpdate(DocumentEvent e)
          {
            currentPageRangeValue = additionalTextfield.getText();
          }

          public void removeUpdate(DocumentEvent e)
          {
            currentPageRangeValue = additionalTextfield.getText();
          }

          public void insertUpdate(DocumentEvent e)
          {
            currentPageRangeValue = additionalTextfield.getText();
          }
        });
      }
      else
        additionalTextfield = null;

      final JRadioButton button = new JRadioButton(t.label);
      if (firstButton == null) firstButton = button;
      button.addChangeListener(new ChangeListener()
      {
        public void stateChanged(ChangeEvent e)
        {
          if (button.isSelected())
          {
            currentPageRangeType = t;
            if (additionalTextfield != null)
              currentPageRangeValue = additionalTextfield.getText();
            else
              currentPageRangeValue = null;
          }
          if (additionalTextfield != null)
          {
            additionalTextfield.setEditable(button.isSelected());
            additionalTextfield.setFocusable(button.isSelected());
          }
        }
      });
      // stateChanged()-Event forcieren:
      button.setSelected(true);
      button.setSelected(false);

      hbox.add(button);
      if (additionalTextfield != null)
      {
        hbox.add(additionalTextfield);
      }

      hbox.add(Box.createHorizontalGlue());
      pageRangeButtons.add(button);
      vboxPageRange.add(hbox);
    }
    if (firstButton != null) firstButton.setSelected(true);

    hbox = Box.createHorizontalBox();
    hbox.add(new JLabel(L.m("Exemplare  ")));
    copyCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    DimAdjust.fixedMaxSize(copyCountSpinner, 0, 0);
    hbox.add(copyCountSpinner);
    vboxCopies.add(hbox);
    vboxCopies.add(Box.createVerticalGlue());

    panel.add(vbox, BorderLayout.CENTER);

    JButton button;
    hbox = Box.createHorizontalBox();
    button = new JButton(L.m("Abbrechen"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        abort(CMD_CANCEL);
      }
    });
    hbox.add(button);
    hbox.add(Box.createHorizontalGlue());
    button = new JButton(L.m("Drucken"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        printButtonPressed();
      }
    });
    hbox.add(button);
    panel.add(hbox, BorderLayout.SOUTH);

    dialog.setVisible(false);
    dialog.setAlwaysOnTop(true);
    dialog.pack();
    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    dialog.setLocation(x, y);
    dialog.setResizable(false);
    dialog.setVisible(true);
  }

  protected void abort(String commandStr)
  {
    /*
     * Wegen folgendem Java Bug (WONTFIX)
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304 sind die folgenden
     * 3 Zeilen n�tig, damit der Dialog gc'ed werden kann. Die Befehle sorgen daf�r,
     * dass kein globales Objekt (wie z.B. der Keyboard-Fokus-Manager) indirekt �ber
     * den JFrame den MailMerge kennt.
     */
    if (dialog != null)
    {
      dialog.removeWindowListener(myWindowListener);
      dialog.getContentPane().remove(0);
      dialog.setJMenuBar(null);

      dialog.dispose();
      dialog = null;
    }

    if (closeActionListener != null)
      closeActionListener.actionPerformed(new ActionEvent(this, 0, commandStr));
  }

  protected void printButtonPressed()
  {
    abort(CMD_SUBMIT);
  }

  /**
   * Ruft den printSettings-Dialog auf.
   * 
   * @author christoph.lutz
   */
  private void showPrintSettingsDialog()
  {
    dialog.setAlwaysOnTop(false);
    Thread t = new Thread()
    {
      public void run()
      {
        // Dialog anzeigen:
        try
        {
          com.sun.star.util.URL url = UNO.getParsedUNOUrl(DispatchHandler.DISP_unoPrinterSetup);
          XNotifyingDispatch disp = UNO.XNotifyingDispatch(WollMuxSingleton.getDispatchForModel(
            UNO.XModel(doc), url));

          if (disp != null)
          {
            disp.dispatchWithNotification(url, new PropertyValue[] {},
              new XDispatchResultListener()
              {
                public void disposing(EventObject arg0)
                {
                }

                public void dispatchFinished(DispatchResultEvent arg0)
                {
                  SwingUtilities.invokeLater(new Runnable()
                  {
                    public void run()
                    {
                      printerNameField.setText(" " + getCurrentPrinterName() + " ");
                      dialog.pack();
                      dialog.setAlwaysOnTop(true);
                    }
                  });
                }
              });
          }
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    };
    t.setDaemon(false);
    t.start();
  }

  /**
   * Liefert den Namen des aktuell zu diesem Dokument eingestellten Druckers.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public String getCurrentPrinterName()
  {
    XPrintable printable = UNO.XPrintable(doc);
    PropertyValue[] printer = null;
    if (printable != null) printer = printable.getPrinter();
    UnoProps printerInfo = new UnoProps(printer);
    try
    {
      return (String) printerInfo.getPropertyValue("Name");
    }
    catch (UnknownPropertyException e)
    {
      return L.m("unbekannt");
    }
  }

  public static void main(String[] args) throws Exception
  {
    UNO.init();
    new PrintParametersDialog(UNO.XTextDocument(UNO.desktop.getCurrentComponent()),
      true, new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          try
          {
            System.out.println(e.getActionCommand());
            PrintParametersDialog ppd = (PrintParametersDialog) e.getSource();
            System.out.println(ppd.getPageRange());
            System.out.println(ppd.getCopyCount());
            Thread.sleep(1000);
          }
          catch (InterruptedException e1)
          {
          }
          System.exit(0);
        }
      });
  }
}
