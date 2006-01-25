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
 * R�ckgabewerte an den FormController, der die Methoden aufruft sind so
 * nat�rlich nicht m�glich. Deshalb muss bei entsprechenden �nderungen jeweils
 * ein Callback erfolgen, typischerweise ein Aufruf der Funktion
 * FormController.updateUI().
 * 
 * FormModel und/oder FormController m�ssen auf die Situation vorbereitet sein,
 * dass FormController eine �nderung anstossen m�chte, die aufgrund einer
 * vorhergehenden �nderung gar nicht mehr m�glich ist. Beispiel:
 * 
 * 1. FormController ersetzt Textfragment F1 durch F2.
 * 
 * 2. Fragment F11 war in F1 enthalten und ist deshalb jetzt nicht mehr
 * vorhanden.
 * 
 * 3. Bevor der updateUI() Aufruf abgearbeitet wurde, der dem Benutzer das UI
 * zum �ndern von F11 genommen h�tte setzt der Benutzer noch eine �nderung
 * von F11 ab.
 * 
 * Eventuell wird der Zugriff auf die FormModel-Funktionen, die die Struktur
 * des Models auslesen synchronisiert erfolgen m�ssen,
 * damit der WollMux-Main-Thread und der Event-Dispatching Thread sich nicht
 * in die Quere kommen k�nnen.
 * Andere L�sung w�re, auch diese lesenden Zugriffe �ber Callbacks zu 
 * realisieren, d.h. FormController ruft Methode auf "requestModelStructure()"
 * und erh�lt dann nach einer Weile den Callback mit der entsprechenden
 * Struktur. Mal sehen, ob sich das im FormController mit vertretbarem
 * Aufwand realisieren l�sst. Eventuell kann aber sogar ganz auf
 * strukturauslesende Methoden verzichtet werden, wenn alle n�tigen Infos
 * in FormUIDescription drinstecken.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface FormModel
{

  
}
