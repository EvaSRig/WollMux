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
* 28.10.2005 | BNK | Arbeit an der Baustelle
* 02.11.2005 | BNK | Testen und Debuggen
*                  | Aus Cache wird jetzt auch der ausgew�hlte gelesen 
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TimeoutException;


/**
 * TODO Doku
 * TODO Festverdrahtete Datasource, die bestimmte find-Anfragen wie "Credits"
 * beantworten kann und immer gefragt wird, wenn die Anfrage bei
 * der Hauptdatenbank keine Ergebnisse gebracht hat.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */

//TODO Folgende Doku auswerten und was noch brauchbar ist irgendwo unterbringen
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


public class DatasourceJoiner
{
  private static final long QUERY_TIMEOUT = 3000;
  private static final Pattern SUCHSTRING_PATTERN = Pattern.compile("^\\*?[^*]+\\*?$");
  private Map nameToDatasource = new HashMap();
  private LocalOverrideStorage myLOS;
  protected Datasource mainDatasource;
  
  /**
   * Erzeugt einen neuen DatasourceJoiner.
   * @param joinConf ein ConfigThingy mit "Datenquellen" Kindern.
   * @param mainSourceName der Name der Datenquelle, auf die sich die
   * Funktionen des DJ (find(),...) beziehen sollen.
   * @param losCache die Datei, in der der DJ die Datens�tze des LOS
   *        abspeichern soll. Falls diese Datei bereits existiert, wird sie vom
   *        Konstruktor eingelesen und verwendet.
   * @param context, der Kontext relativ zu dem Datenquellen URLs in ihrer Beschreibung
   *        auswerten sollen.
   * @throws ConfigurationErrorException falls ein schwerwiegender Fehler
   *         auftritt, der die Arbeit des DJ unm�glich macht, wie z.B.
   *         wenn die Datenquelle mainSourceName in der
   *         joinConf fehlt und gleichzeitig kein Cache verf�gbar ist.
   */
  public DatasourceJoiner(ConfigThingy joinConf, String mainSourceName, File losCache, URL context)
  throws ConfigurationErrorException
  {
    init(joinConf, mainSourceName, losCache, context);
  }
  
  
  protected DatasourceJoiner(){};
  
  protected void init(ConfigThingy joinConf, String mainSourceName, File losCache, URL context)
  throws ConfigurationErrorException
  { //TESTED
    ConfigThingy datenquellen = joinConf.query("Datenquellen").query("Datenquelle");
    Iterator iter = datenquellen.iterator();
    while (iter.hasNext())
    {
      ConfigThingy sourceDesc = (ConfigThingy)iter.next();
      ConfigThingy c = sourceDesc.query("NAME");
      if (c.count() == 0)
      {
        Logger.error("Datenquelle ohne NAME gefunden");
        continue;
      }
      String name = c.toString();
      
      c = sourceDesc.query("TYPE");
      if (c.count() == 0)
      {
        Logger.error("Datenquelle "+name+" hat keinen TYPE");
        continue;
      }
      String type = c.toString();
      
      Datasource ds = null;
      try{
        if (type.equals("conf"))
          ds = new ThingyDatasource(nameToDatasource, sourceDesc, context);
        else
          Logger.error("Ununterst�tzter Datenquellentyp: "+type);
      }
      catch(Exception x)
      {
        Logger.error("Fehler beim Initialisieren von Datenquelle \""+name+"\":", x);
      }
  
      if (ds == null)
      {
        Logger.error("Datenquelle '"+name+"' von Typ '"+type+"' konnte nicht initialisiert werden");
        /*
         * Falls schon eine alte Datenquelle name registriert ist, 
         * entferne diese Registrierung. Ansonsten w�rde mit der vorher
         * registrierten Datenquelle weitergearbeitet, was seltsame Effekte
         * zur Folge h�tte die schwierig nachzuvollziehen sind. 
         */
        nameToDatasource.remove(name);
        continue;
      }
      
      nameToDatasource.put(name, ds);
    }
    
    myLOS = new LocalOverrideStorage(losCache, context);
    
    Set schema = myLOS.getSchema();
    
    if (!nameToDatasource.containsKey(mainSourceName))
    { 
      if (schema == null) throw new ConfigurationErrorException("Datenquelle "+mainSourceName+" nicht definiert und Cache nicht vorhanden");
      
      Logger.error("Datenquelle "+mainSourceName+" nicht definiert => verwende alte Daten aus Cache");
      mainDatasource = new EmptyDatasource(schema, mainSourceName);
      nameToDatasource.put(mainSourceName, mainDatasource);
    }
    else
    {
      mainDatasource = (Datasource)nameToDatasource.get(mainSourceName);

      try{
        myLOS.refreshFromDatabase(mainDatasource, QUERY_TIMEOUT);
      } catch(TimeoutException x)
      {
        Logger.error("Timeout beim Zugriff auf Datenquelle "+mainDatasource.getName()+" => Benutze Daten aus Cache");
      }

    }
  }
 
  /**
   * Liefert das Schema der Hauptdatenquelle zur�ck.
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Set getMainDatasourceSchema()
  { //TESTED
    return mainDatasource.getSchema();
  }
  
   
  /**
   * Durchsucht die Hauptdatenbank (nicht den LOS) 
   * nach Datens�tzen, die in Spalte
   * spaltenName den Wert suchString stehen haben. suchString kann
   * vorne und/oder hinten genau ein Sternchen '*' stehen haben, um
   * Pr�fix/Suffix/Teilstring-Suche zu realisieren. Folgen mehrerer Sternchen
   * oder Sternchen in der Mitte des Suchstrings sind verboten und
   * produzieren eine IllegalArgumentException. Ebenso verboten ist ein
   * suchString, der nur Sternchen enth�lt oder einer der leer ist.
   * Alle Ergebnisse sind {@link DJDataset}s.
   * Die Suche erfolgt grunds�tzlich case-insensitive.
   * @throws TimeoutException falls die Anfrage nicht innerhalb einer 
   * intern vorgegebenen Zeitspanne beendet werden konnte.
   */
  public QueryResults find(String spaltenName, String suchString) throws TimeoutException
  { //TESTED
    if (suchString == null || !SUCHSTRING_PATTERN.matcher(suchString).matches())
      throw new IllegalArgumentException("Illegaler Suchstring: "+suchString);
    
    List query = new Vector();
    query.add(new QueryPart(spaltenName, suchString));
    return find(query);
  }
  
  public QueryResults find(String spaltenName1, String suchString1,String spaltenName2, String suchString2) throws TimeoutException
  {
    if (suchString1 == null || !SUCHSTRING_PATTERN.matcher(suchString1).matches())
      throw new IllegalArgumentException("Illegaler Suchstring: "+suchString1);
    if (suchString2 == null || !SUCHSTRING_PATTERN.matcher(suchString2).matches())
      throw new IllegalArgumentException("Illegaler Suchstring: "+suchString2);
    
    List query = new Vector();
    query.add(new QueryPart(spaltenName1, suchString1));
    query.add(new QueryPart(spaltenName2, suchString2));
    return find(query);
  }
  
  private QueryResults find(List query) throws TimeoutException
  { //TESTED
    QueryResults res = mainDatasource.find(query, QUERY_TIMEOUT);
    List djDatasetsList = new Vector(res.size());
    Iterator iter = res.iterator();
    while (iter.hasNext())
    {
      Dataset ds = (Dataset)iter.next();
      djDatasetsList.add(new DJDatasetWrapper(ds));
    }
    return new QueryResultsList(djDatasetsList);
  }
  
  
  /**
   * Speichert den aktuellen LOS samt zugeh�rigem Cache in die Datei,
   * die dem Konstruktor als losCache �bergeben wurde.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void saveCacheAndLOS() throws IOException
  {
    //TODO saveCacheAndLOS()
    throw new IOException("noch nicht implementiert");
  }
  
  /**
   * Liefert den momentan im Lokalen Override Speicher ausgew�hlten Datensatz.
   * @throws DatasetNotFoundException falls der LOS leer ist (ansonsten ist
   * immer ein Datensatz selektiert).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DJDataset getSelectedDataset() throws DatasetNotFoundException
  {
    return myLOS.getSelectedDataset();
  }
  
  /**
   * Liefert alle Datens�tze (als {@link de.muenchen.allg.itd51.wollmux.db.DJDataset}) des Lokalen Override Speichers.
   */
  public QueryResults getLOS()
  {
    return new QueryResultsList(myLOS.iterator(), myLOS.size());
  }
  
  /**
   * Legt einen neuen Datensatz im LOS an, der nicht mit einer Hintergrunddatenbank
   * verkn�pft ist und liefert ihn zur�ck. Alle Felder des neuen Datensatzes
   * sind mit dem Namen der entsprechenden Spalte initialisiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DJDataset newDataset()
  {
    return myLOS.newDataset();
  };
  
  private static class LocalOverrideStorage
  {
    private static final String LOS_ONLY_MAGIC = "GEHORCHE DEM WOLLMUX!";
    private File cacheFile;
    private List data = new LinkedList();
    private Set losSchema = null;
    private DJDataset selectedDataset = null;
    private long nextGeneratedKey = new Date().getTime();
    
    public LocalOverrideStorage(File losCache, URL context)
    { //TESTED
      cacheFile = losCache;
      String selectKey = "";
      if (cacheFile.canRead())
      {
        try
        {
          ConfigThingy cacheData = new ConfigThingy(losCache.getPath(),context, new FileReader(cacheFile));
          /*
           * Falls der Cache korrupt ist sollen keine korrupten Daten in unseren
           * globalen Felder stehen, deswegen erzeugen wir erstmal alles in
           * tempor�ren Variablen und kopieren diese nachher in die Felder
           * losSchema und this.data. 
           */
          Set newSchema = new HashSet();
          List data = new LinkedList();
          Iterator iter = cacheData.get("Schema").iterator();
          while (iter.hasNext())
            newSchema.add(iter.next().toString());
          
          iter = cacheData.get("Daten").iterator();
          while (iter.hasNext())
          {
            ConfigThingy dsconf = (ConfigThingy)iter.next();
            
            Map dscache = new HashMap();
            Iterator iter2 = dsconf.get("Cache").iterator();
            while (iter2.hasNext())
            {
              ConfigThingy dsNode = (ConfigThingy)iter2.next();
              String spalte = dsNode.getName();
              if (!newSchema.contains(spalte))
              {
                Logger.error(cacheFile.getPath()+" enth�lt korrupten Datensatz (Spalte "+spalte+" nicht im Schema) => Cache wird ignoriert!");
                return;
              }
              
              dscache.put(spalte, dsNode.toString());
            }
            
            Map dsoverride = new HashMap();
            iter2 = dsconf.get("Override").iterator();
            while (iter2.hasNext())
            {
              ConfigThingy dsNode = (ConfigThingy)iter2.next();
              String spalte = dsNode.getName();
              if (!newSchema.contains(spalte))
              {
                Logger.error(cacheFile.getPath()+" enth�lt korrupten Datensatz (Spalte "+spalte+" nicht im Schema) => Cache wird ignoriert!");
                return;
              }
              
              dsoverride.put(spalte, dsNode.toString());
            }
            
            data.add(new LOSDJDataset(dscache, dsoverride, newSchema, dsconf.get("Key").toString()));
            
          }
          
          selectKey = cacheData.get("Ausgewaehlt").toString();
          
          losSchema = newSchema;
          this.data = data;
        }
        catch (FileNotFoundException e) { Logger.error(e); }
        catch (IOException e) { Logger.error(e); }
        catch (SyntaxErrorException e) { Logger.error(e); } 
        catch (NodeNotFoundException e) { Logger.error(e); }
      }
      else
        Logger.log("Cache-Datei "+losCache.getPath()+" kann nicht gelesen werden.");
      
      selectDataset(selectKey);
    }
    
    public void selectDataset(String selectKey)
    { //TESTED
      if (!data.isEmpty()) selectedDataset = (DJDataset)data.get(0);
      Iterator iter = data.iterator();
      while (iter.hasNext())
      {
        LOSDJDataset ds = (LOSDJDataset)iter.next();
        if (selectKey.equals(ds.getKey()))
        {
          selectedDataset = ds;
          return;
        }
      }
      
    }

    private String generateKey()
    {
      return LOS_ONLY_MAGIC + (nextGeneratedKey++);
    }
    
    public DJDataset newDataset()
    {
      Map dsoverride = new HashMap();
      Iterator iter = losSchema.iterator();
      while (iter.hasNext())
      {
        String spalte = (String)iter.next();
        dsoverride.put(spalte,spalte);
      }
      DJDataset ds = new LOSDJDataset(null, dsoverride, losSchema, generateKey()); 
      data.add(ds);
      if (selectedDataset == null) selectedDataset = ds;
      return ds;
    }
    
    public DJDataset copyNonLOSDataset(Dataset ds)
    {
      if (ds instanceof LOSDJDataset)
        Logger.error("Diese Funktion darf nicht f�r LOSDJDatasets aufgerufen werden, da sie immer eine Kopie mit Backing Store erzeugt.");
      
      Map dsoverride = new HashMap();
      Map dscache = new HashMap();
      Iterator iter = losSchema.iterator();
      while (iter.hasNext())
      {
        String spalte = (String)iter.next();
        try
        {
          String wert = ds.get(spalte);
          dscache.put(spalte,wert);
        }
        catch (ColumnNotFoundException e)
        {
          Logger.error(e);
        }
      }
      DJDataset newDs = new LOSDJDataset(dscache, dsoverride, losSchema, ds.getKey()); 
      data.add(newDs);
      if (selectedDataset == null) selectedDataset = newDs;
      return newDs;
    }

    public DJDataset getSelectedDataset() throws DatasetNotFoundException
    {
      if (data.isEmpty()) throw new DatasetNotFoundException("Der Lokale Override Speicher ist leer");
      return selectedDataset;
    }

    public void refreshFromDatabase(Datasource database, long timeout) throws TimeoutException
    { //TESTED
      Map keyToLOSDJDataset = new HashMap();
      Iterator iter = data.iterator();
      while (iter.hasNext())
      {
        LOSDJDataset ds = (LOSDJDataset)iter.next();
        keyToLOSDJDataset.put(ds.getKey(), ds);
      }
      QueryResults res = database.getDatasetsByKey(keyToLOSDJDataset.keySet(), timeout);
      
      /*
       * Schema anpassen und DANACH data leeren. Dadurch werden die
       * LOS-Speicher der LOSDJDatasets an das neue Schema angepasst,
       * bevor der Speicher geleert wird. Dies ist notwendig, da die
       * LOS-Speicher sp�ter direkt an die aus res neu erzeugten 
       * LOSDJDatasets weitergereicht werden.
       */
      this.setSchema(database.getSchema()); 
      data.clear();
      String selectKey = "";
      if (selectedDataset != null) selectKey = selectedDataset.getKey();
      selectedDataset = null;
      
      /*
       * Neue Datens�tze auf Basis der Query erzeugen. Dabei werden die
       * LOS-Speicher von den korrespndierenden alten (gefunden via
       * keyToLOSDJDataset) direkt �bernommen.
       * ACHTUNG: Hierbei werden auch tempor�r im Hintergrundspeicher
       * "verlorene" Datens�tze wieder mit dem Hintergrundspeicher
       * verkn�pft. Sie langer Kommentar weiter unten.
       * Bei evtl. �nderungen bitte beachten!!!
       */
      
      iter = res.iterator();
      while (iter.hasNext())
      {
        try{
          Dataset sourceDS = (Dataset)iter.next();
          
          Map dscache = new HashMap();
          
          Iterator spalte = losSchema.iterator();
          while (spalte.hasNext())
          {
            String spaltenName = (String)spalte.next();
            String spaltenWert = sourceDS.get(spaltenName);
            if (spaltenWert != null)
              dscache.put(spaltenName, spaltenWert);
          }
          
          String key = sourceDS.getKey();
          
          LOSDJDataset override = (LOSDJDataset)keyToLOSDJDataset.remove(key);
          Map dsoverride;
          if (override == null)
            dsoverride = new HashMap();
          else
            dsoverride = override.getLOS();
          
          data.add(new LOSDJDataset(dscache, dsoverride, losSchema, key));
        }catch(Exception x) {Logger.error(x);}
      }
      
      
      /* TODO Folgender Kommentar sollte vermutlich auch ins Handbuch.
       * Es ist m�glich, dass noch Datens�tze aus dem alten LOS �brig sind
       * f�r die keine aktuellen Daten gefunden wurden. Dies sind entweder
       * Datens�tze, die von vorneherein nicht mit einer Hintergrunddatenbank
       * verkn�pft waren oder Datens�tze, die aufgrund von �nderungen des
       * Hintergrundspeichers nicht mehr gefunden wurden. Die Datens�tze,
       * die von vorneherein nur im LOS existierten m�ssen auf jeden Fall
       * erhalten bleiben. Bei den anderen ist es eine gute Frage, was
       * sinnvoll ist. Momentan bleiben auch sie erhalten. Das hat folgende
       * Vor- und Nachteile:
       * Vorteile:
       *   - Falls das Verschwinden des Datensatzes nur ein tempor�res Problem
       *     war, so wird er wenn er wieder im Hintergrundspeicher auftaucht
       *     (und den selben Schl�ssel hat) wieder damit verkn�pft.
       *   - Der Benutzer verliert nie Eintr�ge seiner Absenderliste
       * Nachteile:
       *   - Der Benutzer merkt evtl. nicht, dass er pl�tzlich vom
       *     Hintergrundspeicher abgekoppelt ist und bekommt gew�nschte
       *     �nderungen nicht mit.
       *   - Die Admins haben keine M�glichkeit, einen Eintrag aus der
       *     Absenderliste eines Benutzers zu entfernen (ausser sie
       *     greifen direkt auf sein .wollmux Verzeichnis zu.
       *   - Falls ein Datensatz bewusst entfernt wurde und sp�ter ein
       *     neuer Datensatz mit dem selben Schl�ssel angelegt wird, so
       *     wird der Eintrag in der Absenderliste mit dem neuen Eintrag
       *     verkn�pft, obwohl dieser nichts mit dem alten zu tun hat.
       */
      iter = keyToLOSDJDataset.values().iterator();
      while (iter.hasNext())
        data.add(iter.next());
      
      selectDataset(selectKey);
    }

    /**
     * Liefert null, falls bislang kein Schema vorhanden (weil das Laden
     * der Cache-Datei im Konstruktur fehlgeschlagen ist).
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public Set getSchema() {return losSchema;} //TESTED
    
    /**
     * �ndert das Datenbankschema. Spalten des alten Schemas, die im neuen
     * nicht mehr vorhanden sind werden aus den Datens�tzen gel�scht. 
     * Im neuen Schema hinzugekommene Spalten werden in den Datens�tzen
     * als unbelegt betrachtet. 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void setSchema(Set schema)
    { //TESTED
      if (losSchema == null)
      {
        losSchema = new HashSet(schema);
        return;
      }
      
      Set spaltenDieDazuGekommenSind = new HashSet(schema);
      spaltenDieDazuGekommenSind.removeAll(losSchema);
      
      losSchema.addAll(spaltenDieDazuGekommenSind);
      
      Set spaltenDieWeggefallenSind = new HashSet(losSchema);
      spaltenDieWeggefallenSind.removeAll(schema);
      
      losSchema.removeAll(spaltenDieWeggefallenSind);
      
      if (spaltenDieWeggefallenSind.isEmpty() 
       && spaltenDieDazuGekommenSind.isEmpty()) return;
      
      Logger.log("Das Datenbank-Schema wurde ge�ndert. Der Cache "+cacheFile.getPath()+" wird angepasst.");
      
      Iterator iter = data.iterator();
      while (iter.hasNext())
      {
        LOSDJDataset ds = (LOSDJDataset)iter.next();
        
        Iterator spalte = spaltenDieWeggefallenSind.iterator();
        while (spalte.hasNext())
          ds.drop((String)spalte.next());
        
        ds.setSchema(losSchema);
      }
    }
    
    public int size()
    {
      return data.size();
    }

    public Iterator iterator()
    {
      return data.iterator();
    }

    public boolean isEmpty()
    {
      return data.isEmpty();
    }
    
    private class LOSDJDataset extends DJDatasetBase
    {
      private String key;
      
      public LOSDJDataset(Map dscache, Map dsoverride, Set schema, String key)
      { //TESTED
        super(dscache, dsoverride, schema);
        this.key = key;
      }

      public void drop(String columnName)
      { //TESTED
        if (isFromLOS()) myLOS.remove(columnName);
        if (hasBackingStore()) myBS.remove(columnName);
      }

      public void setSchema(Set losSchema)
      { //TESTED
        this.schema = losSchema;        
      }

      public DJDataset copy()
      {
        DJDataset newDS = new LOSDJDataset(this.myBS, isFromLOS()? new HashMap(this.myLOS): new HashMap(), this.schema, this.key);
        LocalOverrideStorage.this.data.add(newDS);
        if (selectedDataset == null) selectedDataset = newDS;
        return newDS;
      }

      public void remove() throws UnsupportedOperationException
      {
        if (!isFromLOS()) throw new UnsupportedOperationException("Versuch, einen Datensatz, der nicht aus dem LOS kommt zu entfernen");
        LocalOverrideStorage.this.data.remove(this);
        if (selectedDataset == this)
        {
          if (LocalOverrideStorage.this.data.isEmpty()) 
            selectedDataset = null;
          else
            selectedDataset = (DJDataset)LocalOverrideStorage.this.data.get(0);
        }
      }

      public boolean isSelectedDataset()
      {
        return this == selectedDataset;
      }

      public void select() throws UnsupportedOperationException
      {
        if (!isFromLOS()) throw new UnsupportedOperationException();
        selectedDataset = this;
      }

      public String getKey()
      { //TESTED
        return this.key;
      }
    }
  }
  
  private class DJDatasetWrapper implements DJDataset
  {
    private Dataset myDS;
    
    public DJDatasetWrapper(Dataset ds)
    {
      myDS = ds;
    }
    
    public void set(String columnName, String newValue) throws ColumnNotFoundException, UnsupportedOperationException, IllegalArgumentException
    {
      throw new UnsupportedOperationException("Datensatz kommt nicht aus dem LOS");
    }

    public boolean hasLocalOverride(String columnName) throws ColumnNotFoundException
    {
      return false;
    }

    public boolean hasBackingStore()
    {
      return true;
    }

    public boolean isFromLOS()
    {
      return false;
    }

    public boolean isSelectedDataset()
    {
      return false;
    }

    public void select() throws UnsupportedOperationException
    {
      throw new UnsupportedOperationException("Datensatz kommt nicht aus dem LOS");
    }

    public void discardLocalOverride(String columnName) throws ColumnNotFoundException, NoBackingStoreException
    {
      //nichts zu tun
    }

    public DJDataset copy()
    {
      return myLOS.copyNonLOSDataset(myDS);
    }

    public void remove() throws UnsupportedOperationException
    {
      throw new UnsupportedOperationException("Datensatz kommt nicht aus dem LOS");
    }

    public String get(String columnName) throws ColumnNotFoundException
    {
      return myDS.get(columnName);
    }

    public String getKey()
    {
      return myDS.getKey();
    }
  }
}
