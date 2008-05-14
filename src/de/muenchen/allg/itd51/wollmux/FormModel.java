/*
 * Dateiname: FormModel.java
 * Projekt  : WollMux
 * Funktion : Erlaubt Zugriff auf die Formularbestandteile eines Dokuments abstrahiert von den dahinterstehenden OOo-Objekten.
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
 * 27.12.2005 | BNK | Erstellung
 * 28.04.2008 | BNK | +save(), +saveAs()
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.awt.event.ActionListener;

/**
 * Erlaubt Zugriff auf die Formularbestandteile eines Dokuments abstrahiert von den
 * dahinterstehenden OOo-Objekten. ACHTUNG! Der FormController ruft die Methoden
 * dieser Klasse aus dem Event Dispatching Thread auf. Dort d�rfen sie aber meist
 * nicht laufen. Deshalb m�ssen alle entsprechenden Methoden �ber den
 * WollMuxEventHandler ein Event Objekt erzeugen und in die WollMux-Queue zur
 * sp�teren Ausf�hrung schieben. Es muss daf�r gesorgt werden, dass das FormModel
 * Objekt auch funktioniert, wenn das zugrundeliegende Office-Dokument disposed
 * wurde, da der FormController evtl. im Moment des disposens darauf zugreifen
 * m�chte. Hoffentlich l�st obiges Umsetzen der Aufrufe in Event-Objekte dieses
 * Problem schon weitgehend.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface FormModel
{

  /**
   * Setzt die Position und Gr��e des Fensters des zugeh�rigen Dokuments auf die
   * vorgegebenen Werte setzt. ACHTUNG: Die Ma�angaben beziehen sich auf die linke
   * obere Ecke des Fensterinhalts OHNE die Titelzeile und die Fensterdekoration des
   * Rahmens. Um die linke obere Ecke des gesamten Fensters richtig zu setzen, m�ssen
   * die Gr��enangaben des Randes der Fensterdekoration und die H�he der Titelzeile
   * VOR dem Aufruf der Methode entsprechend eingerechnet werden.
   * 
   * @param docX
   *          Die linke obere Ecke des Fensterinhalts X-Koordinate der Position in
   *          Pixel, gez�hlt von links oben.
   * @param docY
   *          Die Y-Koordinate der Position in Pixel, gez�hlt von links oben.
   * @param docWidth
   *          Die Gr��e des Dokuments auf der X-Achse in Pixel
   * @param docHeight
   *          Die Gr��e des Dokuments auf der Y-Achse in Pixel. Auch hier wird die
   *          Titelzeile des Rahmens nicht beachtet und muss vorher entsprechend
   *          eingerechnet werden.
   * 
   * @author christoph.lutz
   */
  public void setWindowPosSize(int docX, int docY, int docWidth, int docHeight);

  /**
   * Setzt den Sichtbarkeitsstatus des Fensters des zugeh�rigen Dokuments auf vis
   * (true=sichtbar, false=unsichtbar).
   * 
   * @param vis
   *          true=sichtbar, false=unsichtbar
   * 
   * @author christoph.lutz
   */
  public void setWindowVisible(boolean vis);

  /**
   * Versucht das Dokument zu schlie�en. Wurde das Dokument ver�ndert
   * (Modified-Status des Dokuments==true), so erscheint der Dialog
   * "Speichern"/"Verwerfen"/"Abbrechen" (�ber den ein sofortiges Schlie�en des
   * Dokuments durch den Benutzer verhindert werden kann)
   * 
   * @author christoph.lutz
   */
  public void close();

  /**
   * Setzt den Sichtbarkeitsstatus der Sichtbarkeitsgruppe mit der ID groupID auf
   * visible.
   * 
   * @param groupId
   *          Die ID der Gruppe, die Sichtbar/unsichtbar geschalten werden soll.
   * @param visible
   *          true==sichtbar, false==unsichtbar
   * 
   * @author christoph.lutz
   */
  public void setVisibleState(String groupId, boolean visible);

  /**
   * Setzt den Wert aller Formularfelder im Dokument, die von fieldId abh�ngen auf
   * den neuen Wert newValue (bzw. auf das Ergebnis der zu diesem Formularelement
   * hinterlegten Trafo-Funktion).
   * 
   * Es ist nicht garantiert, dass sich der Wert tats�chlich ge�ndert hat. Die
   * fieldId kann leer sein (aber nie null).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void valueChanged(String fieldId, String newValue);

  /**
   * Das Formularfeld im Dokument mit der ID fieldId erh�lt den Fokus. Gibt es im
   * Dokument mehrere Formularfelder, die von der ID abh�ngen, so erh�lt immer das
   * erste Formularfeld den Fokus - bevorzugt werden dabei auch die nicht
   * transformierten Formularfelder.
   * 
   * @param fieldId
   *          id des Formularfeldes, das den Fokus bekommen soll.
   * 
   * @author christoph.lutz
   */
  public void focusGained(String fieldId);

  /**
   * Not Yet Implemented: Nimmt dem Formularfeld mit der ID fieldId den Fokus wieder
   * weg - ergibt aber bisher keinen Sinn.
   * 
   * @param fieldId
   * 
   * @author christoph.lutz
   */
  public void focusLost(String fieldId);

  /**
   * Informiert das FormModel, dass das zugrundeliegende Dokument source geschlossen
   * wird und das FormModel entsprechend handeln soll um sicherzustellen, dass das
   * Dokument in Zukunft nicht mehr angesprochen wird.
   * 
   * Abh�ngig von der Implementierung des FormModels werden unterschiedliche Aktionen
   * erledigt. Dazu geh�ren z.B. das Beenden einer bereits gestarteten FormGUI oder
   * das Wiederherstellen der Fensterattribute des Dokumentfensters auf die Werte,
   * die das Fenster vor dem Starten der FormGUI hatte.
   * 
   * @param source
   *          Das Dokument das geschlossen wurde.
   * 
   * @author christoph.lutz
   */
  public void disposing(TextDocumentModel source);

  /**
   * Teilt der FormGUI die zu diesem FormModel geh�rt mit, dass der Wert des
   * Formularfeldes mit der id fieldId auf den neuen Wert value gesetzt werden soll
   * und ruft nach erfolgreicher aktion die Methode actionPerformed(ActionEvent arg0)
   * des Listeners listener.
   * 
   * @param fieldId
   *          die Id des Feldes das in der FormGUI auf den neuen Wert value gesetzt
   *          werden soll.
   * @param value
   *          der neue Wert value.
   * @param listener
   *          der Listener der informiert wird, nachdem der Wert erfolgreich gesetzt
   *          wurde.
   * 
   * @author christoph.lutz
   */
  public void setValue(String fieldId, String value, ActionListener listener);

  /**
   * Erzeugt eine FormGUI zu diesem FormModel und startet diese.
   * 
   * @author christoph.lutz
   */
  public void startFormGUI();

  /**
   * Startet den Ausdruck unter Verwendung eventuell vorhandener
   * Komfortdruckfunktionen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void print();

  /**
   * Exportiert das Dokument als PDF. Bei Multi-Form wird die Aktion f�r alle
   * Formulare der Reihe nach aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void pdf();

  /**
   * Speichert das Dokument (Datei/Speichern). Bei Multi-Form wird die Aktion f�r
   * alle Formulare der Reihe nach aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void save();

  /**
   * Speichert das Dokument (Datei/Speichern unter...). Bei Multi-Form wird die
   * Aktion f�r alle Formulare der Reihe nach aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void saveAs();

  /**
   * �ber diese Methode kann der FormController das FormModel informieren, dass er
   * vollst�ndig initialisiert wurde und notwendige Aktionen wie z.B. das
   * zur�cksetzen des modified-Status des Dokuments durchgef�hrt werden sollen.
   */
  public void formControllerInitCompleted();
}
