/* 
 * Dateiname: PrintProgressBar.java
 * Projekt  : WollMux
 * Funktion : Implementiert eine Fortschrittsanzeige f�r den WollMux-Komfortdruck
 * 
 * Copyright (c) 2008 Landeshauptstadt M�nchen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
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
 * 04.07.2008 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import de.muenchen.allg.itd51.wollmux.L;

/**
 * Diese Klasse repr�sentiert eine Fortschrittsanzeige f�r den WollMux-Komfortdruck,
 * die damit zurecht kommt, dass potentiell mehrere Druckfunktionen hintereinander
 * geschaltet sind und die Zahl insgesamt zu druckender Versionen sich daher aus der
 * Multiplikation der Werte der einzelnen Druckfunktionen ergibt. Im Fall, dass
 * mehrere Druckfunktionen ihren Status an die PrintProgressBar berichten, erfolgt
 * auch z.B. eine Anzeige des Druckstatus in der Form "3 von 10 (=2x5)", aus der
 * hervorgeht, dass zwei Druckfunktionen beteiligt sind, von denen die eine 2
 * Versionen und die andere 5 Versionen erstellen wird.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class PrintProgressBar
{
  /**
   * Enth�lt eine Zuordnung eines Schl�ssels, der eine Komfortdruckfunktion
   * repr�sentiert, auf den Maximalwert von dieser Komfortdruckfunktion zu
   * erwartender Versionen.
   */
  private HashMap<Object, Integer> maxValues;

  /**
   * Enth�lt eine Zuordnung eines Schl�ssels, der eine Komfortdruckfunktion
   * repr�sentiert, auf den Bearbeitungsstatus der entsprechenden
   * Komfortdruckfunktion. F�r den Bearbeitungsstatus gilt: 0 <= currentValue <=
   * maxValue.
   */
  private HashMap<Object, Integer> currentValues;

  /**
   * Enth�lt die Schl�ssel der gerade aktiven Komfortdruckfunktionen in zeitlich
   * gesehen umgekehrter Registrierungsreihenfolge. D.h. ein maxValue, der zu einem
   * sp�teren Zeitpunkt mittels setMaxValue(key, maxValue) registriert wird, wird
   * immer am Anfang der LinkedList eingef�gt.
   */
  private LinkedList<Object> order;

  /**
   * Enth�lt den JFrame in dem die Fortschrittsanzeige dargestellt wird.
   */
  private JFrame myFrame;

  /**
   * Enth�lt den WindowListener, mit dem der "X"-Button des Fensters abgefangen wird.
   */
  private WindowListener oehrchen;

  /**
   * Enth�lt den Listener der aufgerufen wird, wenn der "X"- oder der
   * "Abbrechen"-Button bet�tigt wurde.
   */
  private ActionListener abortListener;

  /**
   * Enth�lt die JProgressBar zur Darstellung des Gesamtdruckverlaufs
   */
  private JProgressBar pb;

  /**
   * Enth�lt das Label mit dem Statustext (z.B. "3 von 10 (=2x5)")
   */
  private JLabel statusLabel;

  /**
   * Enth�lt den Abbrechen-Knopf
   */
  private JButton cancelButton;

  /**
   * Erzeugt ein neues PrintProgressBar-Objekt und zeigt das entsprechende Fenster
   * mit der Verlaufsinformation sofort sichtbar an.
   * 
   * @param abortListener
   *          Der abortListener wird informiert, wenn der "X"-Button oder der
   *          "Abbrechen"-Knopf des Fensters bet�tigt wurde.
   */
  public PrintProgressBar(ActionListener abortListener)
  {
    order = new LinkedList<Object>();
    maxValues = new HashMap<Object, Integer>();
    currentValues = new HashMap<Object, Integer>();
    this.abortListener = abortListener;
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        createGui();
      }
    });
  }

  /**
   * Erzeugt das Fenster und alle enthaltenen Elemente und schaltet es sichtbar.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void createGui()
  {
    myFrame = new JFrame(L.m("Drucke"));
    oehrchen = new MyWindowListener();
    myFrame.addWindowListener(oehrchen);

    JPanel panel = new JPanel();

    pb = new JProgressBar(0, 100);
    pb.setStringPainted(true);
    panel.add(pb);

    statusLabel = new JLabel("                ");
    panel.add(statusLabel);

    cancelButton = new JButton(L.m("Abbrechen"));
    cancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        cancel();
      }
    });
    panel.add(cancelButton);

    myFrame.add(panel);
    refresh();
    myFrame.pack();
    myFrame.setVisible(true);
    myFrame.setAlwaysOnTop(true);
  }

  /**
   * F�ngt den "X"-Knopf des Fensters ab und ruft in diesem Fall die cancel-Methode
   * auf.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private class MyWindowListener implements WindowListener
  {
    public void windowOpened(WindowEvent e)
    {}

    public void windowClosing(WindowEvent e)
    {
      cancel();
    }

    public void windowClosed(WindowEvent e)
    {}

    public void windowIconified(WindowEvent e)
    {}

    public void windowDeiconified(WindowEvent e)
    {}

    public void windowActivated(WindowEvent e)
    {}

    public void windowDeactivated(WindowEvent e)
    {}

  }

  /**
   * Wird aufgerufen, wenn der "X"- oder "Abbrechen"-Knopf gedr�ckt wurde und sorgt
   * daf�r, dass das Fenster vollst�ndig disposed und der abortListener informiert
   * wird.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void cancel()
  {
    if (myFrame == null) return;
    myFrame.removeWindowListener(oehrchen);
    myFrame.getContentPane().remove(0);
    myFrame.setJMenuBar(null);

    myFrame.dispose();
    myFrame = null;

    if (abortListener != null) new Thread()
    {
      public void run()
      {
        abortListener.actionPerformed(new ActionEvent(this, 0, ""));
      }
    }.start();
  }

  /**
   * Schlie�t das Fenster dieser PrintProgressBar ohne den abortListener zu
   * informieren.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void dispose()
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        abortListener = null;
        cancel();
      }
    });
  }

  /**
   * Registriert eine Komfortdruckfunktion (vertreten durch den Schl�ssel key) mit
   * der Maximalzahl der von dieser Komfortdruckfunktion zu erwartenden Versionen
   * oder entfernt eine bereits registrierte Druckfunktion, wenn maxValue==0 ist.
   * Beim Registrieren wird die Zahl der von der Druckfunktion bereits gedruckten
   * Versionen mit 0 initialisiert, wenn die Druckfunktion bisher noch nicht bekannt
   * war.
   * 
   * @param key
   *          wird gehashed und repr�sentiert die Komfortdruckfunktion, von der
   *          maxValue Versionen zu erwarten sind.
   * @param maxValue
   *          die Anzahl der von der Druckfunktion zu erwartenden Versionen oder 0
   *          zum deregistrieren einer Druckfunktion.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void setMaxValue(Object key, int maxValue)
  {
    if (key == null) return;

    if (maxValue == 0)
    {
      // Z�hler f�r key l�schen, wenn maxValue==0
      maxValues.remove(key);
      currentValues.remove(key);
      for (Iterator<Object> iter = order.iterator(); iter.hasNext();)
      {
        Object k = iter.next();
        if (k != null && k.equals(key)) iter.remove();
      }
    }
    else
    {
      // neuen maxWert setzen, Reihenfolge festhalten und currentValue
      // initialisieren
      if (!maxValues.containsKey(key)) order.addFirst(key);
      maxValues.put(key, maxValue);
      if (!currentValues.containsKey(key)) currentValues.put(key, 0);
    }

    refresh();
  }

  /**
   * Informiert die PrintProgressBar �ber den Fortschritt value einer Druckfunktion,
   * die durch key repr�sentiert wird.
   * 
   * @param key
   *          repr�sentiert eine Druckfunktion, die �ber den neuen Fortschritt
   *          informiert.
   * @param value
   *          enth�lt die aktuellen Anzahl der Versionen, die bereits von der
   *          Druckfunktion gedruckt wurden und muss damit im Bereich 0 <= value <=
   *          maxValue (siehe setMaxValue(...)) liegen.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void setValue(Object key, int value)
  {
    if (key == null) return;
    Integer max = maxValues.get(key);
    if (max == null) return;
    if (value > max) value = max;
    if (value < 0) value = 0;

    currentValues.put(key, value);
    refresh();
  }

  /**
   * Baut die Ansicht der PrintProgressBar neu auf. Eine der Hauptaufgaben von
   * refresh ist es dabei, den status-String (z.B. "1 von 4 Versionen" oder bei mehr
   * als einer registrierten Druckfunktion "3 von 10 (=2x5) Versionen") zusammen zu
   * setzen und die Gesamtzahl zu erwartender Versionen und den aktuellen
   * Fortschrittswert zu berechnen. Die Gesamtzahl ergibt sich aus der Multiplikation
   * der einzelnen Maximal-Werte der registrierten Druckfunktionen. Bei der
   * Berechnung des aktuellen Druckfortschritts spielt die Reihenfolge der
   * registrierten Druckfunktionen eine Rolle, da das Erh�hen einer fr�her
   * registrierten Druckfunktion einschlie�t, dass die sp�ter registrierten
   * Druckfunktionen damit auch schon entsprechend oft durchlaufen wurden.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void refresh()
  {
    int allMax = 1;
    int allCurrent = 0;
    StringBuffer fromMaxString = new StringBuffer();
    boolean showfms = order.size() > 1;
    if (showfms) fromMaxString.append(" (=");
    boolean first = true;

    for (Object key : order)
    {
      allCurrent += currentValues.get(key) * allMax;
      if (first)
        first = false;
      else if (showfms) fromMaxString.append("x");
      if (showfms) fromMaxString.append(maxValues.get(key));
      allMax *= maxValues.get(key);
    }
    if (showfms) fromMaxString.append(")");

    refresh(allCurrent, allMax, fromMaxString.toString());
  }

  /**
   * Enth�lt die Teile von refresh, die �ber den Swing-EDT aufgerufen werden.
   * 
   * @param allCurrent
   *          gesamtzahl aller zu erwartenden Versionen
   * @param allMax
   *          gesamtzahl aller bereits gedruckten Versionen
   * @param fromMaxString
   *          Darstellung abh�ngig von der Anzahl registrierter Druckfunktionen
   *          entweder "" oder "(=2x5)"
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void refresh(final int allCurrent, final int allMax,
      final String fromMaxString)
  {
    if (myFrame == null) return;
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        pb.setMaximum(allMax);
        pb.setValue(allCurrent);
        statusLabel.setText(L.m(" %1 von %2%3 Versionen", allCurrent, allMax,
          fromMaxString));
        myFrame.pack();
      }
    });
  }

  /**
   * Testmethode
   * 
   * @param args
   * @throws InterruptedException
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void main(String[] args) throws InterruptedException
  {
    PrintProgressBar bar = new PrintProgressBar(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        System.out.println(L.m("Druckvorgang abgebrochen!"));
      }
    });
    Thread.sleep(2000);
    bar.setMaxValue("Hallo", 3);
    Thread.sleep(4000);
    bar.setValue("Hallo", 1);
    Thread.sleep(4000);
    bar.setValue("Hallo", 2);
    Thread.sleep(4000);
    bar.setValue("Hallo", 3);
    Thread.sleep(4000);
    bar.setMaxValue("bar", 200);
    bar.setValue("bar", 2);
    Thread.sleep(4000);
    bar.setValue("bar", 4);
    Thread.sleep(10000);
    bar.dispose();
  }

}
