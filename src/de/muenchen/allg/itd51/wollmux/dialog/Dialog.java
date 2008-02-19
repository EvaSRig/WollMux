//TODO L.m()
/*
* Dateiname: Dialog.java
* Projekt  : WollMux
* Funktion : Ein Dialog, der dem Benutzer erlaubt verschiedenen Werte zu setzen.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 04.05.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Ein Dialog, der dem Benutzer erlaubt verschiedenen Werte zu setzen.
 */
public interface Dialog
{
  /**
   * Liefert die Instanz dieses Dialogs f�r den gegebenen context (neu erstellt,
   * falls bisher noch nicht verwendet).
   * @param context F�r jeden Kontext h�lt der Dialog eine unabh�ngige Kopie von
   *        seinem Zustand vor. Auf diese Weise l�sst sich der Dialog an verschiedenen
   *        Stellen unabh�ngig voneinander einsetzen. ACHTUNG! Diese Map wird nicht
   *        als Schl�ssel verwendet, sondern in ihr werden Werte abgelegt.
   * @throws ConfigurationErrorException wenn der Dialog mit fehlerhaften Daten
   *         initialisiert wurde (und der Fehler erst bei der Instanziierung
   *         diagnostiziert werden konnte).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Dialog instanceFor(Map context) throws ConfigurationErrorException;
  
  /**
   * Liefert den durch id identifizierten Wert des Dialogs. Falls der Dialog noch
   * nicht aufgerufen wurde wird ein Standardwert geliefert (typischerweise der
   * leere String). Der R�ckgabewert null ist ebenfalls m�glich und signalisiert,
   * dass der Dialog das entsprechende Feld nicht hat und auch nie haben wird.
   * Die R�ckgabe von null ist in diesem Fall allerdings nicht verpflichtend, sondern
   * es ist ebenfalls der leere String m�glich. Die R�ckgabe von null sollte
   * jedoch erfolgen, falls es dem Dialog irgendwie m�glich ist.
   *  
   * Diese Funktion darf nur f�r mit instanceFor()
   * erzeugte Instanzen aufgerufen werden. Ansonsten liefert sie immer null.
   * Diese Funktion ist Thread-safe. Insbesondere muss sie nicht im EDT aufgerufen werden.
   * Sie kann sowohl vor, w�hrend als auch nach dem Aufruf von show() aufgerufen werden,
   * auch nachdem der Dialog schon geschlossen wurde.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Object getData(String id);
  
  /**
   * Liefert eine Menge von ids, f�r die {@link #getData(String)} niemals null liefert.
   * Dies ist nicht zwangsweise eine vollst�ndige Liste aller ids, f�r die der Dialog
   * Werte zur�ckliefern kann. Es ist ebenfalls nicht garantiert, dass der Dialog
   * jemeils f�r eine dieser ids etwas anderes als den leeren String zur�ckliefert.
   * Diese Funktion kann schon vor instanceFor() aufgerufen werden, es ist jedoch
   * m�glich, dass bei  Aufruf f�r eine mit instanceFor() erzeugte Instanz mehr
   * Information (d.h. eine gr��ere Menge) zur�ckgeliefert wird.
   * Das zur�ckgelieferte Objekt darf ver�ndert werden. Dies hat keine Auswirkungen
   * auf den Dialog.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Collection getSchema();
 
  /**
   * Zeigt den Dialog an. Diese Funktion darf nur f�r mit instanceFor() erzeugte
   * Instanzen aufgerufen werden. Ansonsten tut sie nichts.
   * @param dialogEndListener falls nicht null, wird 
   *        die {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *        Methode aufgerufen (im Event Dispatching Thread), 
   *        nachdem der Dialog geschlossen wurde.
   *        Das actionCommand des ActionEvents gibt die Aktion an, die
   *        das Beenden des Dialogs veranlasst hat.
   * @param funcLib falls der Dialog Funktionen auswertet, so werden Referenzen
   *        auf Funktionen mit dieser Bibliothek aufgel�st.
   * @param dialogLib falls der Dialog wiederum Funktionsdialoge unterst�tzt,
   *        so werden Referenzen auf Funktionsdialoge �ber diese Bibliothek
   *        aufgel�st.
   * @throws ConfigurationErrorException wenn der Dialog mit fehlerhaften Daten
   *         initialisiert wurde (und der Fehler erst bei der Anzeige
   *         diagnostiziert werden konnte).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void show(ActionListener dialogEndListener, FunctionLibrary funcLib,
      DialogLibrary dialogLib) throws ConfigurationErrorException;
}
