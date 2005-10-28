/*
* Dateiname: Datasource.java
* Projekt  : WollMux
* Funktion : Interface f�r Datenquellen, die der DJ verwalten kann
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 27.10.2005 | BNK | Erstellung
* 28.10.2005 | BNK | Erweiterung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.util.Collection;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.TimeoutException;

/**
 * ACHTUNG! Die Konstruktoren dieser Klasse d�rfen keine potentiell
 * lange blockierenden Aktionen (zum Beispiel Netzverbindung herstellen) 
 * ausf�hren. Sie d�rfen auch nicht versagen, falls irgendeine Rahmenbedingung
 * nicht gegeben ist, die nur f�r Zugriffe auf
 * die Datens�tze relevant ist (z.B. Verbindung zum LDAP-Server). 
 * Der Konstruktor darf (und muss) nur dann versagen, wenn es nicht m�glich 
 * ist, die Datenquelle in einen Zustand zu bringen, in dem sie die
 * Methoden ausf�hren kann, die unabh�ngig von den Datens�tzen sind.
 * Am wichtigsten sind hier die Methoden zur Abfrage des Schemas.
 * F�r die Methoden, die auf Datens�tze zugreifen gilt, dass ihr Versagen
 * aufgrund von Rahmenbedingungen (z.B. kein Netz) nicht dazu f�hren darf,
 * dass das Datenquellen-Objekt in einen unbrauchbaren Zustand ger�t.
 * Woimmer sinnvoll sollte es m�glich sein, eine Operation zu einem
 * sp�teren Zeitpunkt zu wiederholen, wenn die Rahmenbedingungen sich
 * ge�ndert haben, und dann sollte die Operation gelingen. Dies bedeutet
 * insbesondere, dass Verbindungsaufbau zu Servern wo n�tig jeweils neu
 * versucht wird und nicht nur einmalig im Konstruktor.
 * 
 * Argumente gegen Datasource-Typ "override":
 * - (korrekte) Suche nur schwierig und ineffizient zu implementieren
 * - w�rde vermutlich dazu f�hren, dass Daten im LDAP schlechter gepflegt
 *   werden, weil es einfacher ist, einen Override einzuf�hren
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Datasource
{
  /**
   * Liefert ein Set, das die Titel aller Spalten der Datenquelle enth�lt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Set getSchema();
  
  /**
   * Liefert alle Datens�tze, deren Schl�ssel in der Collection keys
   * enthalten sind.
   * @param timeout die maximale Zeit in Millisekunden, die vergehen darf, bis die
   * Funktion zur�ckkehrt.
   * @throws TimeoutException, falls die Anfrage nicht rechtzeitig beendet
   * werden konnte.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public QueryResults getDatasetsByKey(Collection keys, long timeout) 
    throws TimeoutException;
  
}
