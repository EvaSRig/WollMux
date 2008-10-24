/* 
 * Dateiname: FunkyDatasource.java
 * Projekt  : WollMux
 * Funktion : Datasource, die mit WollMux-Funktionen berechnete Spalten erm�glicht.
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
 * 24.10.2008 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.db;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.TimeoutException;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Datasource, die mit WollMux-Funktionen berechnete Spalten erm�glicht. ACHTUNG!
 * Diese Datasource verh�lt sich bei Suchanfragen nicht entsprechend dem normalen
 * Verhalten einer Datasource, da sie immer auf den Originaldaten sucht, jedoch
 * transformierte Datens�tze zur�ckliefert.
 * 
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class FunkyDatasource implements Datasource
{
  private Datasource source;

  private Set<String> schema;

  private String name;

  private ColumnTransformer columnTransformer;

  /**
   * Erzeugt eine neue FunkyDatasource.
   * 
   * @param nameToDatasource
   *          enth�lt alle bis zum Zeitpunkt der Definition dieser UnionDatasource
   *          bereits vollst�ndig instanziierten Datenquellen.
   * @param sourceDesc
   *          der "Datenquelle"-Knoten, der die Beschreibung dieser UnionDatasource
   *          enth�lt.
   * @param urlContext
   *          der Kontext relativ zu dem URLs aufgel�st werden sollen (zur Zeit nicht
   *          verwendet).
   */
  public FunkyDatasource(Map<String, Datasource> nameToDatasource,
      ConfigThingy sourceDesc, URL urlContext) throws ConfigurationErrorException
  {
    try
    {
      name = sourceDesc.get("NAME", 1).toString();
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(L.m("NAME der Datenquelle fehlt"));
    }

    String sourceName;
    try
    {
      sourceName = sourceDesc.get("SOURCE", 1).toString();
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(L.m(
        "SOURCE der Datenquelle \"%1\" fehlt", name));
    }

    source = nameToDatasource.get(sourceName);

    if (source == null)
      throw new ConfigurationErrorException(
        L.m(
          "Fehler bei Initialisierung von Datenquelle \"%1\": Referenzierte Datenquelle \"%2\" nicht (oder fehlerhaft) definiert",
          name, sourceName));

    /*
     * Kommentar kopiert von der entsprechenden Stelle aus WollMuxFiles:
     * 
     * Zum Zeitpunkt wo der DJ initialisiert wird sind die Funktions- und
     * Dialogbibliothek des WollMuxSingleton noch nicht initialisiert, deswegen
     * k�nnen sie hier nicht verwendet werden. Man k�nnte die Reihenfolge nat�rlich
     * �ndern, aber diese Reihenfolgeabh�ngigkeit gef�llt mir nicht. Besser w�re auch
     * bei den Funktionen WollMuxSingleton.getFunctionDialogs() und
     * WollMuxSingleton.getGlobalFunctions() eine on-demand initialisierung nach dem
     * Prinzip if (... == null) initialisieren. Aber das heben wir uns f�r einen
     * Zeitpunkt auf, wo es ben�tigt wird und nehmen jetzt erst mal leere
     * Dummy-Bibliotheken.
     */
    FunctionLibrary funcLib = new FunctionLibrary();
    DialogLibrary dialogLib = new DialogLibrary();
    Map<Object, Object> context = new HashMap<Object, Object>();
    columnTransformer =
      new ColumnTransformer(sourceDesc, "Spaltenumsetzung", funcLib, dialogLib,
        context);

    schema = columnTransformer.getSchema();
    schema.addAll(source.getSchema());
  }

  public Set<String> getSchema()
  {
    return schema;
  }

  public QueryResults getDatasetsByKey(Collection<String> keys, long timeout)
      throws TimeoutException
  {
    return columnTransformer.transform(source.getDatasetsByKey(keys, timeout));
  }

  public QueryResults getContents(long timeout) throws TimeoutException
  {
    return columnTransformer.transform(source.getContents(timeout));
  }

  public QueryResults find(List<QueryPart> query, long timeout)
      throws TimeoutException
  {
    return columnTransformer.transform(source.find(query, timeout));
  }

  public String getName()
  {
    return name;
  }

}
