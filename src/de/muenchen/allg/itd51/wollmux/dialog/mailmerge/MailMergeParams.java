/*
 * Dateiname: MailMergeParams.java
 * Projekt  : WollMux
 * Funktion : Dialoge zur Bestimmung der Parameter f�r den wirklichen Merge (z.B. ob in Gesamtdokument oder auf Drucker geschrieben werden soll.)
 * 
 * Copyright (c) 2008 Landeshauptstadt M�nchen
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
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 11.10.2007 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.dialog.NonNumericKeyConsumer;

/**
 * Dialoge zur Bestimmung der Parameter f�r den wirklichen Merge (z.B. ob in
 * Gesamtdokument oder auf Drucker geschrieben werden soll.)
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
class MailMergeParams
{
  /**
   * Auf welche Art hat der Benutzer die zu druckenden Datens�tze ausgew�hlt.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10)
   */
  enum DatasetSelectionType {
    /**
     * Alle Datens�tze.
     */
    ALL,

    /**
     * Der durch {@link MailMergeNew#rangeStart} und {@link MailMergeNew#rangeEnd}
     * gegebene Wert.
     */
    RANGE,

    /**
     * Die durch {@link MailMergeNew#selectedIndexes} bestimmten Datens�tze.
     */
    INDIVIDUAL;
  }

  public class IndexSelection
  {
    /**
     * Falls {@link #datasetSelectionType} == {@link DatasetSelectionType#RANGE}
     * bestimmt dies den ersten zu druckenden Datensatz (wobei der erste Datensatz
     * die Nummer 1 hat). ACHTUNG! Der Wert hier kann 0 oder gr��er als
     * {@link #rangeEnd} sein. Dies muss dort behandelt werden, wo er verwendet wird.
     */
    public int rangeStart = 1;

    /**
     * Falls {@link #datasetSelectionType} == {@link DatasetSelectionType#RANGE}
     * bestimmt dies den letzten zu druckenden Datensatz (wobei der erste Datensatz
     * die Nummer 1 hat). ACHTUNG! Der Wert hier kann 0 oder kleiner als
     * {@link #rangeStart} sein. Dies muss dort behandelt werden, wo er verwendet
     * wird.
     */
    public int rangeEnd = Integer.MAX_VALUE;

    /**
     * Falls {@link #datasetSelectionType} == {@link DatasetSelectionType#INDIVIDUAL}
     * bestimmt dies die Indizes der ausgew�hlten Datens�tze, wobei 1 den ersten
     * Datensatz bezeichnet.
     */
    public List<Integer> selectedIndexes = new Vector<Integer>();

  };

  enum MailMergeType {
    /**
     * Gesamtdokument erzeugen, das alle Serienbriefe in allen Ausfertigungen
     * enth�lt.
     */
    SINGLE_FILE(L.m("in neues Dokument schreiben")),

    /**
     * Eine Datei pro Serienbrief, wobei jede Datei alle Versionen (bei SLV-Druck)
     * enth�lt.
     */
    MULTI_FILE(L.m("in einzelne Dateien schreiben")),

    /**
     * Direkte Ausgabe auf dem Drucker.
     */
    PRINTER(L.m("auf dem Drucker ausgeben")),

    /**
     * Versenden per E-Mail.
     */
    // EMAIL(L.m("als E-Mails versenden"))
    ;

    /**
     * Label f�r die Anzeige dieser Option.
     */
    private final String menuLabel;

    MailMergeType(String menuLabel)
    {
      this.menuLabel = menuLabel;
    }

    public String toString()
    {
      return menuLabel;
    }
  }

  /**
   * Auf welche Art hat der Benutzer die zu druckenden Datens�tze ausgew�hlt.
   */
  private DatasetSelectionType datasetSelectionType = DatasetSelectionType.ALL;

  /**
   * Falls {@link DatasetSelectionType} != {@link DatasetSelectionType#ALL}, so
   * bestimmt dies die Indizes der ausgew�hlten Datens�tze.
   */
  private IndexSelection indexSelection = new IndexSelection();

  /**
   * Zeigt den Dialog an, der die Serienbriefverarbeitung (Direktdruck oder in neues
   * Dokument) anwirft.
   * 
   * @param parent
   *          Elternfenster f�r den anzuzeigenden Dialog.
   * 
   * @param mm
   *          Die Methode
   *          {@link MailMergeNew#doMailMerge(de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.MailMergeType, de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.DatasetSelectionType)}
   *          wird ausgel�st, wenn der Benutzer den Seriendruck startet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  void showDoMailmergeDialog(JFrame parent, final MailMergeNew mm)
  {
    final JDialog dialog = new JDialog(parent, L.m("Seriendruck"), true);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    Box vbox = Box.createVerticalBox();
    vbox.setBorder(new EmptyBorder(8, 5, 10, 5));
    dialog.add(vbox);

    Box hbox = Box.createHorizontalBox();
    JLabel label = new JLabel(L.m("Serienbriefe"));
    hbox.add(label);
    hbox.add(Box.createHorizontalStrut(5));

    Vector<MailMergeType> types = new Vector<MailMergeType>();
    for (MailMergeType type : MailMergeType.values())
      types.add(type);
    final JComboBox typeBox = new JComboBox(types);
    typeBox.addItemListener(new ItemListener()
    {
      public void itemStateChanged(ItemEvent e)
      {
      // printIntoDocument = (typeBox.getSelectedIndex() == 0);
      }
    });
    hbox.add(typeBox);

    vbox.add(hbox);
    vbox.add(Box.createVerticalStrut(5));

    Box selectBox = Box.createVerticalBox();
    Border border =
      BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
        L.m("Folgende Datens�tze verwenden"));
    selectBox.setBorder(border);

    hbox = Box.createHorizontalBox();
    ButtonGroup radioGroup = new ButtonGroup();
    JRadioButton rbutton;
    rbutton = new JRadioButton(L.m("Alle"), true);
    rbutton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        datasetSelectionType = DatasetSelectionType.ALL;
      }
    });
    hbox.add(rbutton);
    hbox.add(Box.createHorizontalGlue());
    radioGroup.add(rbutton);

    selectBox.add(hbox);
    selectBox.add(Box.createVerticalStrut(5));

    hbox = Box.createHorizontalBox();

    final JRadioButton rangebutton = new JRadioButton(L.m("Von"), false);
    rangebutton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        datasetSelectionType = DatasetSelectionType.RANGE;
      }
    });
    hbox.add(rangebutton);
    radioGroup.add(rangebutton);

    final JTextField start = new JTextField(4);
    start.addKeyListener(NonNumericKeyConsumer.instance);
    hbox.add(start);
    hbox.add(Box.createHorizontalStrut(5));
    label = new JLabel("Bis");
    hbox.add(label);
    hbox.add(Box.createHorizontalStrut(5));

    final JTextField end = new JTextField(4);
    end.addKeyListener(NonNumericKeyConsumer.instance);

    DocumentListener rangeDocumentListener = new DocumentListener()
    {
      public void update()
      {
        rangebutton.setSelected(true);
        datasetSelectionType = DatasetSelectionType.RANGE;
        try
        {
          indexSelection.rangeStart = Integer.parseInt(start.getText());
        }
        catch (Exception x)
        {}
        try
        {
          indexSelection.rangeEnd = Integer.parseInt(end.getText());
        }
        catch (Exception x)
        {}
      }

      public void insertUpdate(DocumentEvent e)
      {
        update();
      }

      public void removeUpdate(DocumentEvent e)
      {
        update();
      }

      public void changedUpdate(DocumentEvent e)
      {
        update();
      }
    };

    Document tfdoc = start.getDocument();
    tfdoc.addDocumentListener(rangeDocumentListener);
    tfdoc = end.getDocument();
    tfdoc.addDocumentListener(rangeDocumentListener);
    hbox.add(end);

    selectBox.add(hbox);
    selectBox.add(Box.createVerticalStrut(5));

    hbox = Box.createHorizontalBox();

    // TODO Anwahl muss selben Effekt haben wie das Dr�cken des "Einzelauswahl"
    // Buttons
    // final JRadioButton einzelauswahlRadioButton = new JRadioButton("");
    // hbox.add(einzelauswahlRadioButton);
    // radioGroup.add(einzelauswahlRadioButton);
    //
    // ActionListener einzelauswahlActionListener = new ActionListener()
    // {
    // public void actionPerformed(ActionEvent e)
    // {
    // einzelauswahlRadioButton.setSelected(true);
    // datasetSelectionType = DatasetSelectionType.INDIVIDUAL;
    // // TODO showEinzelauswahlDialog();
    // }
    // };
    //
    // einzelauswahlRadioButton.addActionListener(einzelauswahlActionListener);
    //
    // JButton button = new JButton(L.m("Einzelauswahl..."));
    // hbox.add(button);
    // hbox.add(Box.createHorizontalGlue());
    // button.addActionListener(einzelauswahlActionListener);

    selectBox.add(hbox);
    vbox.add(selectBox);
    vbox.add(Box.createVerticalStrut(5));

    hbox = Box.createHorizontalBox();
    hbox.add(new JLabel(L.m("Zielverzeichnis")));
    hbox.add(Box.createHorizontalGlue());
    vbox.add(hbox);

    hbox = Box.createHorizontalBox();
    final JTextField targetDirectory = new JTextField();
    hbox.add(targetDirectory);
    hbox.add(new JButton(new AbstractAction("Suchen...")
    {
      public void actionPerformed(ActionEvent e)
      {}
    }));

    hbox.add(Box.createHorizontalGlue());
    vbox.add(hbox);

    vbox.add(Box.createVerticalStrut(5));

    hbox = Box.createHorizontalBox();
    hbox.add(new JLabel(L.m("Dateinamenmuster")));
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(new JButton(new AbstractAction("Serienbrieffeld")
    {
      public void actionPerformed(ActionEvent e)
      {}
    }));
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(new JButton(new AbstractAction("Spezialfeld")
    {
      public void actionPerformed(ActionEvent e)
      {}
    }));
    vbox.add(hbox);

    vbox.add(Box.createVerticalStrut(5));

    hbox = Box.createHorizontalBox();
    final JTextField targetPattern = new JTextField();
    hbox.add(targetPattern);
    vbox.add(hbox);

    vbox.add(Box.createVerticalStrut(5));

    hbox = Box.createHorizontalBox();
    JButton button = new JButton(L.m("Abbrechen"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        dialog.dispose();
      }
    });
    hbox.add(button);

    hbox.add(Box.createHorizontalGlue());

    button = new JButton(L.m("Los geht's!"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        dialog.dispose();
        mm.doMailMerge((MailMergeType) typeBox.getSelectedItem(),
          datasetSelectionType, indexSelection);
      }
    });
    hbox.add(button);

    vbox.add(hbox);

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

}
