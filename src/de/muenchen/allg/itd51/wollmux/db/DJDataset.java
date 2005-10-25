/*
* Dateiname: DJDataset.java
* Projekt  : WollMux
* Funktion : Ein vom DJ gelieferter Datensatz, der zu den Methoden von
*            Dataset noch DJ-spezifische Methoden anbietet.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 14.10.2005 | BNK | Erstellung
* 24.10.2005 | BNK | +copy()
*                  | +remove()
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

/**
 * TODO Doku
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface DJDataset extends Dataset
{

  
  
  /**
   * Schreibt newValue als neuen Wert des Datensatzes in Spalte columnName 
   * in den LOS des DJ, jedoch nur falls der Datensatz bereits aus dem LOS
   * kommt (also {@link #isFromLOS()} true liefert).
   * @throws ColumnNotFoundException falls keine Spalte namens columnName existiert. 
   * @throws UnsupportedOperationException, falls dieser Datensatz nicht aus
   *         dem LOS kommt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void set(String columnName, String newValue) throws ColumnNotFoundException, UnsupportedOperationException;
  
  /**
   * Liefert true, falls die Spalte columnName dieses Datensatzes nicht aus
   * den Hintergrunddatenbank kommt, sondern aus dem lokalen Override-Speicher
   * des DJ.
   * @throws ColumnNotFoundException falls keine Spalte namens columnName existiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean hasLocalOverride(String columnName) throws ColumnNotFoundException;
  
  /**
   * Liefert true, falls zu diesem Datensatz eine Hintergrunddatenbank
   * existiert, mit der einige seiner Spalten verkn�pft sind, oder �ber
   * {@link #discardLocalOverride(String)} verkn�pft werden k�nnen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean hasBackingStore();
  
  /**
   * Liefert true, falls dieser Datensatz aus dem Lokalen Override Speicher
   * kommt. ACHTUNG! Dies bedeutet nicht, dass es eine Spalte gibt, f�r die
   * hasLocalOverride(Spalte) true liefert, da der LOS auch Datens�tze erlaubt,
   * bei denen alle Spalten noch mit der Hintergrunddatenbank verkn�pft sind.
   * Zum Beispiel wird ein Datensatz nicht automatisch aus dem LOS entfernt,
   * wenn f�r alle Spalten discardLocalOverride() aufgerufen wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isFromLOS();
  
  /**
   * Liefert true, falls this der momentan im LOS ausgew�hlte Datensatz ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isSelectedDataset();
  
  /**
   * Macht this zum im LOS ausgew�hlten Datensatz. 
   * @throws UnsupportedOperationException falls this nicht aus dem LOS kommt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void select() throws UnsupportedOperationException;
  
  /**
   * Verwirft den Wert im LOS f�r Spalte columnName dieses Datensatzes und 
   * verkn�pft die Spalte wieder mit der Hintergrunddatenbank.
   * ACHTUNG! Ein Datensatz bei dem der lokale Override f�r alle Spalten
   * discardet wurde wird NICHT automatisch aus dem LOS entfernt. Insbesondere
   * liefert isFromLOS() weiterhin true.
   * Die Spalte muss auf jeden Fall existieren.
   * @throws ColumnNotFoundException falls keine Spalte namens columnName existiert.
   * @throws NoBackingStoreException falls der Datensatz nie mit einer Hintergrunddatenbank verkn�pft war.
   * Keine Exception wird geworfen, falls der die entsprechende Spalte
   * bereits mit einer Hintergrunddatenbank verkn�pft ist (z.B. weil der Datensatz
   * gar nicht aus dem LOS kommt).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void discardLocalOverride(String columnName) throws ColumnNotFoundException, NoBackingStoreException;
  
  /**
   * Legt eine Kopie dieses Datensatzes im LOS an. Achtung! Dies ver�ndert
   * nicht den R�ckgabewert von this.{@link #isFromLOS()}, da this
   * selbst dadurch nicht ver�ndert wird.
   * 
   * @return die neue Kopie.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DJDataset copy();
  
  /**
   * Entfernt diesen Datensatz aus dem LOS. Achtung! Nach dieser Operation ist
   * der Datensatz ung�ltig. Insbesondere ist der Wert von {@link #isFromLOS()}
   * nicht definiert.
   * @throws UnsupportedOperationException falls dieser Datensatz nicht aus
   * dem LOS kommt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void remove() throws UnsupportedOperationException;
}
