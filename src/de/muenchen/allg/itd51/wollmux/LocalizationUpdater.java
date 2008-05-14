/*
 * Dateiname: LocalizationUpdater.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse liest alle zu lokalisierenden Strings des WollMux 
 *            aus dem Source-Code und aktualisiert die Datei localization.conf.
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
 * 21.02.2008 | LUT | Erstellung als LocalizationUpdater
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;

/**
 * Diese Klasse liest alle zu lokalisierenden Strings des WollMux aus dem
 * Source-Code und aktualisiert die Datei localization.conf. Dabei wird wie
 * folgt vorgegangen: Bereits bestehende Eintr�ge bleiben in der Reihenfolge, in
 * der sie bestehen. Durch den Update neu hinzukommende Eintr�ge werden an das
 * Ende der Liste angeh�ngt. Bereits bestehende Eintr�ge, zu denen es im Code
 * keine zugeh�rigen Original-Strings mehr gibt, werden auskommentiert und an
 * das Ende der Liste verschoben. Sind unter den auskommentierten, verschobenen
 * Zeilen auch Eintr�ge dabei, die tats�chlich eine �bersetzung in anderen
 * Sprachen besitzen, so wird nach dem Update eine Warnung ausgegeben, die
 * darauf hinweist, dass hier bereits �bersetzte Strings mit dem n�chsten
 * Update-Lauf entfernt werden.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class LocalizationUpdater
{
  /**
   * Enth�lt den Pfad zur Konfigurationsdatei localization.conf aus Sicht des
   * Projekt-Hauptverzeichnisses.
   */
  private static File localizationConfFile = new File("./src/data/localization.conf");

  /**
   * Enth�lt das Wurzelverzeichnis der Source-Dateien aus Sicht des
   * Projekt-Hauptverzeichnises.
   */
  private static File sourcesDir = new File("./src/");

  /**
   * Enth�lt das Pattern, mit dem nach L.m-Strings gesucht wird. In Gruppe 1 ist
   * der String dieser zu lokalisierenden Message enthalten.
   */
  private static Pattern L_m_Pattern = Pattern.compile("L.m\\(\\s*\"((?:\\\\\"|[^\"])*)\"");

  /**
   * Muss aus dem Hauptverzeichnis des WollMux-Projekts ausgef�hrt werden und
   * aktualisiert die Datei localization.conf.
   * 
   * @param args
   *          die args werden nicht ausgewertet
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void main(String[] args)
  {
    updateLocalizationConf();
  }

  /**
   * Macht die eigentliche Arbeit.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static void updateLocalizationConf()
  {
    System.out.println(L.m("Aktualisiere die Datei localization.conf"));

    // localization.conf einlesen und knownOriginals sammeln.
    HashSet<String> knownOriginals = new HashSet<String>();
    HashSet<String> currentOriginals = new HashSet<String>();
    ConfigThingy localizationConf = new ConfigThingy("localization");
    ConfigThingy messages;
    try
    {
      localizationConf = new ConfigThingy("localization",
        localizationConfFile.toURL());
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    try
    {
      messages = localizationConf.query("L10n").query("Messages").getLastChild();
      for (Iterator iter = messages.iterator(); iter.hasNext();)
      {
        ConfigThingy element = (ConfigThingy) iter.next();
        if (element.getName().equalsIgnoreCase("original"))
          knownOriginals.add(element.toString());
      }
    }
    catch (NodeNotFoundException e)
    {
      messages = new ConfigThingy("Messages");
    }

    // Alle .java Files aus scanForSourcesDir iterieren und L.m()s rausziehen
    int countNew = 0;
    List<File> sources = new ArrayList<File>();
    addJavaFilesRecursive(sources, sourcesDir);
    int count = 0;
    int lastProgress = 0;
    for (Iterator<File> iter = sources.iterator(); iter.hasNext();)
    {
      File file = iter.next();

      String str = readFile(file, "ISO-8859-1");

      Matcher m = L_m_Pattern.matcher(str);
      while (m.find())
      {
        String original = evalString(m.group(1));
        currentOriginals.add(original);
        if (!knownOriginals.contains(original))
        {
          messages.add("original").add(original);
          knownOriginals.add(original);
          countNew++;
        }
      }

      // Fortschrittsanzeige in
      count++;
      int progress = (int) ((1.0 * count / sources.size()) * 100);
      if (progress / 10 != lastProgress / 10)
      {
        System.out.println(L.m("Fortschritt: %1 %", new Integer(progress)));
        lastProgress = progress;
      }
    }

    // Messages-Abschnitt der localization.conf neu erzeugen (die
    // StringRepresentation von ConfigThingy macht das nicht sch�n genug,
    // deshalb hier eine eigene Ausgaberoutine) und dabei vorangestellte
    // Abschnitte unver�ndert lassen.
    String str = "";
    String origContent = readFile(localizationConfFile, "UTF-8");
    String origContentBeforeMessages = origContent.split("\\sMessages\\s*\\(", 2)[0];
    if (origContentBeforeMessages.length() == 0)
      str += "L10n(\n  Messages(\n";
    else
      str += origContentBeforeMessages + " Messages(\n";

    String removed = "";
    int countRemoved = 0;
    HashMap<String, Integer> countTranslations = new HashMap<String, Integer>();
    boolean valid = false;
    boolean removedTranslatedMessagesWarning = false;
    for (Iterator iter = messages.iterator(); iter.hasNext();)
    {
      ConfigThingy element = (ConfigThingy) iter.next();
      String elementStr = element.stringRepresentation();
      elementStr.replaceAll("\\n", "%n");

      if (element.getName().equalsIgnoreCase("original"))
      {
        valid = currentOriginals.contains(element.toString());
        if (valid)
        {
          str += "\n    " + elementStr + "\n";
        }
        else
        {
          removed += "\n# removed:\n#    " + elementStr + "\n";
          countRemoved++;
        }
      }
      else
      {
        if (valid)
        {
          str += "       " + elementStr + "\n";
          String language = element.getName().toLowerCase();
          Integer ct = countTranslations.get(language);
          int cti = (ct != null) ? ct.intValue() : 0;
          countTranslations.put(language, new Integer(cti + 1));
        }
        else
        {
          removedTranslatedMessagesWarning = true;
          removed += "#       " + elementStr + "\n";
        }
      }
    }
    str += removed;
    str += "\n  )\n)";

    try
    {
      FileWriter writer = new FileWriter(localizationConfFile);
      writer.write(str);
      writer.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    // Statistik und Warnung ausgeben:
    System.out.println("");
    System.out.println(L.m("Neue original-Strings: %1", new Integer(countNew)));
    System.out.println(L.m("Auskommentierte original-Strings: %1", new Integer(
      countRemoved)));
    System.out.println(L.m("Gesamtzahl aktuelle original-Strings: %1", new Integer(
      currentOriginals.size())));
    for (Iterator<String> iter = countTranslations.keySet().iterator(); iter.hasNext();)
    {
      String language = iter.next();
      int ct = countTranslations.get(language).intValue();
      System.out.println(L.m("Davon nicht �bersetzt in Sprache %1: %2", language,
        new Integer(currentOriginals.size() - ct)));
    }

    if (removedTranslatedMessagesWarning)
      System.err.println("\n"
                         + L.m("ACHTUNG: Bitte �berpr�fen Sie den Inhalt Ihrer Datei localization.conf,\nda bereits �bersetzte aber nicht mehr ben�tigte Eintr�ge auskommentiert\nwurden und mit der n�chsten Aktualisierung endg�ltig entfernt werden."));
  }

  /**
   * Liefert den kompletten Inhalt der mit encoding encodierten Datei file als
   * String zur�ck.
   * 
   * @param file
   * @throws FileNotFoundException
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static String readFile(File file, String encoding)
  {
    String str = "";
    try
    {
      InputStreamReader r = new InputStreamReader(new FileInputStream(file),
        encoding);
      char[] buff = new char[1024];
      int count;
      while ((count = r.read(buff)) > 0)
      {
        str += new String(buff, 0, count);
      }
      r.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    return str;
  }

  /**
   * F�gt alle Dateien, die mit .java enden aus diesem Verzeichnis fileOrDir und
   * aus allen Unterverzeichnissen zur Liste l hinzu.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static void addJavaFilesRecursive(List<File> l, File fileOrDir)
  {
    if (fileOrDir.isFile() && fileOrDir.getName().endsWith(".java"))
    {
      l.add(fileOrDir);
      return;
    }

    if (fileOrDir.isDirectory())
    {
      File[] files = fileOrDir.listFiles();
      for (int i = 0; i < files.length; i++)
      {
        addJavaFilesRecursive(l, files[i]);
      }
    }
  }

  /**
   * Diese Methode evaluiert einen String aus dem SourceCode, der auch
   * Character-Escape-Sequenzen enthalten kann, in der Form, wie ihn der
   * Java-Compiler interpretieren w�rde und liefert den Java-String zur�ck.
   * 
   * Derzeit werden folgende Escape-Sequenzen aus
   * http://java.sun.com/docs/books/tutorial/java/data/characters.html
   * umgesetzt: \t, \b, \n, \r, \f, \', \", \\
   * 
   * @param str
   *          Ein String aus dem SourceCode, der zu �bersetzen ist.
   * @return den evaluierten String
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static String evalString(String str)
  {
    String evalStr = new String(str);
    evalStr = evalStr.replaceAll("\\\\t", "\t");
    evalStr = evalStr.replaceAll("\\\\b", "\b");
    evalStr = evalStr.replaceAll("\\\\n", "\n");
    evalStr = evalStr.replaceAll("\\\\r", "\r");
    evalStr = evalStr.replaceAll("\\\\f", "\f");
    evalStr = evalStr.replaceAll("\\\\'", "\'");
    evalStr = evalStr.replaceAll("\\\\\"", "\"");
    evalStr = evalStr.replaceAll("\\\\\\\\", "\\\\");
    return evalStr;
  }
}
