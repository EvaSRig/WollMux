/*
* Dateiname: InsertionModel.java
* Projekt  : WollMux
* Funktion : Stellt eine Einf�gestelle im Dokument (insertValue oder insertFormValue) dar.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 06.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

/**
 * Stellt eine Einf�gestelle im Dokument (insertValue oder insertFormValue) dar.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class InsertionModel
{
  /** 
   * Konstante f�r {@link #sourceType}, die angibt, dass die Daten f�r die Einf�gung
   * aus einer externen Datenquelle kommen. 
   */
  private static final int DATABASE_TYPE = 0;
  
  /** 
   * Konstante f�r {@link #sourceType}, die angibt, dass die Daten f�r die Einf�gung
   * aus dem Formular kommen. 
   */
  private static final int FORM_TYPE = 1;
  
  /**
   * Gibt an, um woher die Einf�gung ihre Daten bezieht.
   * @see #FORM_TYPE
   * @see #DATABASE_TYPE
   */
  private int sourceType = FORM_TYPE;
  
  /**
   * Der Name des Bookmarks, das diese Einf�gestelle umschlie�t.
   */
  private String bookmarkName;
  
  
}
