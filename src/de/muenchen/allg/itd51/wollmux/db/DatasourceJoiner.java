/*
* Dateiname: DatasourceJoiner.java
* Projekt  : WollMux
* Funktion : stellt eine virtuelle Datenbank zur Verf�gung, die ihre Daten
*            aus verschiedenen Hintergrunddatenbanken zieht.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 06.10.2005 | BNK | Erstellung
* 24.10.2005 | BNK | +newDataset()
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.util.Iterator;


/**
 * TODO Doku
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DatasourceJoiner
{
  
  private LocalOverrideStorage myLOS;
  
  //Suche nach 
  //X           "vorname.nachname"
  //X           "vorname.nachname@muenchen.de"
  //X           "Nam"
  //O           "ITD5.1"  nicht unterst�tzt weil Minus vor 5.1 fehlt
  //X           "ITD-5.1"
  //O           "D"   liefert Personen mit Nachname-Anfangsbuchstabe D
  //X           "D-*"
  //O           "ITD5"    nicht unterst�tzt weil Minus vor 5 fehlt
  //X           "D-HAIII"
  //X           "5.1"
  //X           "D-III-ITD-5.1"
  //O           "D-HAIII-ITD-5.1"   nicht unterst�tzt, da HA nicht im lhmOUShortname
  //O           "D-HAIII-ITD5.1"    nicht unterst�tzt (siehe oben)

  //X           "Nam Vorn"
  //X           "Nam, Vorn"
  //X           "Vorname Name"
  //X           "Vorn Nam"
  //X           "ITD 5.1"
  //O           "D-HAIII-ITD 5.1"   steht nicht mit HA im LDAP
  //X           "V. Nachname"
  //X           "Vorname N."

  /* M�gliche Probleme:
   * 
   * - copy() muss auch bei einem Datensatz ohne Backing Store funktionieren
   * - der Datensatz k�nnte zwischenzeitlich im Backing Store gel�scht werden 
   * 
   * teilweise L�sung: F�r jeden Eintrag des LOS ist der Hintergrundspeicher im Cache.
   * Der Cache wird w�hrend der Ausf�hrung des WollMux nicht ge�ndert. 
   * �ber den Cache grunds�tzlich einen Backing Store zur Verf�gung
   * Auch mit newDataset() erzeugte Datens�tze bekommen Eintr�ge im Cache
   * als h�tten sie einen Backing Store. Bei diesen Eintr�gen (und nat�rlich
   * auch im LOS) werden alle Spalten mit einem String vorbelegt, der dem
   * Namen der Spalte entspricht.
   * 
   * Ein Problem an dieser L�sung k�nnte sein, dass Datens�tze evtl. Identifier
   * brauchen, die den Join aus dem sie entstanden sind beschreiben. Mal schaun.
   * 
   * */
  
  /*
   * Als allgemeines Konstrukt um die Rolle<->OrgaKurz Beziehung zu
   * beschreiben die M�glichkeit einbauen, in der Join-Datei f�r das Schema
   * der virtuellen Datenbank Fallbacks einzuf�hren. 
   * Beispiel: Rolle -> OrgaKurz
   * Falls von einem Datensatz die Spalte "Rolle" angefragt wird, diese
   * jedoch null ist, so wird der Wert der Spalte "OrgaKurz" zur�ckgeliefert.
   * Dies wird in Dataset oder QueryResults implementiert, indem diese bei
   * Instanziierung die Fallback-Listen bekommen.
   */
  
  
  //suchString kann vorne und/oder hinten ein oder mehrere * haben zur 
  // prefix/suffix/teilstring-suche (mehrere aufeinanderfolgende * sind
  //�quivalent zu einem *), * in der Mitte des suchStrings sind nicht erlaubt.
  //die Suche erfolgt grunds�tzlich case-insensitive
  //gesucht wird nur in der virtuellen Datenbank, nicht im LOS.
  //die Ergebnisse sind alle DJDatasets
  public QueryResults find(String spaltenName, String suchString)
  {
    return null;
  }
  
  public QueryResults find(String spaltenName1, String suchString1,String spaltenName2, String suchString2)
  {
    return null;
  }
  
  /**
   * Liefert den momentan im Lokalen Override Speicher ausgew�hlten Datensatz.
   * @throws DatasetNotFoundException falls der LOS leer ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DJDataset getSelectedDataset() throws DatasetNotFoundException
  {
    return new TestDJDataset();
  }
  
  /**
   * Liefert alle Datens�tze (als {@link de.muenchen.allg.itd51.wollmux.db.DJDataset}) des Lokalen Override Speichers.
   */
  public QueryResults getLOS()
  {
    return myLOS;
  }
  
  /**
   * Legt einen neuen Datensatz im LOS an, der nicht mit einer Hintergrunddatenbank
   * verkn�pft ist und liefert ihn zur�ck.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DJDataset newDataset()
  {
    return null;
  };
  
  private static class LocalOverrideStorage implements QueryResults
  {

    public int size()
    {
      // TODO Auto-generated method stub
      return 0;
    }

    public Iterator iterator()
    {
      // TODO Auto-generated method stub
      return null;
    }

    public boolean isEmpty()
    {
      return size() == 0;
    }
  }
  
}
