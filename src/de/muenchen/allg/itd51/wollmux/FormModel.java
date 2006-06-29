/*
 * Dateiname: FormModel.java
 * Projekt  : WollMux
 * Funktion : Erlaubt Zugriff auf die Formularbestandteile eines Dokuments abstrahiert von den dahinterstehenden OOo-Objekten.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 27.12.2005 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

/**
 * Erlaubt Zugriff auf die Formularbestandteile eines Dokuments abstrahiert von
 * den dahinterstehenden OOo-Objekten. ACHTUNG! Der FormController ruft die
 * Methoden dieser Klasse aus dem Event Dispatching Thread auf. Dort d�rfen sie
 * aber meist nicht laufen. Deshalb m�ssen alle entsprechenden Methoden ein
 * Event Objekt erzeugen und in die WollMux-Queue zur sp�teren Ausf�hrung
 * schieben. Es muss daf�r gesorgt werden, dass das FormModel Objekt auch
 * funktioniert, wenn das zugrundeliegende Office-Dokument disposed wurde, da
 * der FormController evtl. im Moment des disposens darauf zugreifen m�chte.
 * Hoffentlich l�st obiges Umsetzen der Aufrufe in Event-Objekte dieses Problem
 * schon weitgehend.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface FormModel
{

  public void setWindowPosSize(int docX, int docY, int docWidth, int docHeight);

  public void setWindowVisible(boolean vis);

  public void close();

  public void setVisibleState(String groupId, boolean visible);
  
  /**
   * Es ist nicht garantiert, dass sich der Wert tats�chlich ge�ndert hat.
   * Die fieldId kann leer sein (aber nie null).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void valueChanged(String fieldId, String newValue);
  
  public void focusGained(String fieldId);
  
  public void focusLost(String fieldId);
}
