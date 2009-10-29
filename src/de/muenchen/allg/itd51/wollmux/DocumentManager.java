/*
 * Dateiname: DocumentManager.java
 * Projekt  : WollMux
 * Funktion : Verwaltet Informationen zu allen offenen OOo-Dokumenten
 * 
 * Copyright (c) 2009 Landeshauptstadt M�nchen
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
 * 27.10.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.util.HashMap;

import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;

/**
 * Verwaltet Informationen zu allen offenen OOo-Dokumenten.
 * 
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class DocumentManager
{
  private HashMap<HashableComponent, Info> info =
    new HashMap<HashableComponent, Info>();

  /**
   * F�gt compo den gemanageten Objekten hinzu, wobei die f�r Textdokumente
   * relevanten Informationen hinterlegt werden.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public synchronized void addTextDocument(XTextDocument compo)
  {

    info.put(new HashableComponent(compo), new TextDocumentInfo(compo));
  }

  /**
   * F�gt compo den gemanageten Objekten hinzu, ohne weitere Informationen zu
   * hinterlegen. compo ist also ein Objekt, an dem f�r den WollMux nur interessant
   * ist, dass es existiert.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public synchronized void add(XComponent compo)
  {
    info.put(new HashableComponent(compo), new Info());
  }

  /**
   * Entfernt alle Informationen �ber compo (falls vorhanden) aus diesem Manager.
   * 
   * @return die entfernten Informationen oder null falls keine vorhanden.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public synchronized Info remove(XComponent compo)
  {
    return info.remove(new HashableComponent(compo));
  }

  /**
   * Liefert die �ber dieses Objekt bekannten Informationen oder null, falls das
   * Objekt dem Manager nicht bekannt ist.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public synchronized Info getInfo(XComponent compo)
  {
    return info.get(new HashableComponent(compo));
  }

  public static class Info
  {
    /**
     * Liefert das zu diesem Dokument geh�rige TextDocumentModel. Falls es noch nicht
     * angelegt wurde, wird es angelegt.
     * 
     * @throws UnsupportedOperationException
     *           falls das Dokument kein TextDocument ist.
     * 
     * @author Matthias Benkmann (D-III-ITD-D101)
     */
    public TextDocumentModel getTextDocumentModel()
    {
      throw new UnsupportedOperationException();
    }

    /**
     * Liefert true gdw dieses Dokument ein TextDocumentModel zugeordnet haben kann
     * UND ein solches auch bereits angelegt wurde.
     * 
     * @author Matthias Benkmann (D-III-ITD-D101)
     */
    public boolean hasTextDocumentModel()
    {
      return false;
    }
  }

  public static class TextDocumentInfo extends Info
  {
    private TextDocumentModel model;

    private XTextDocument doc;

    public TextDocumentInfo(XTextDocument doc)
    {
      this.doc = doc;
    }

    /**
     * Auf die Methoden getTextDocumentModel() und hasTextDocumentModel() wird
     * m�glicherweise aus verschiedenen Threads zugegriffen (WollMux Event Queue und
     * Event Handler im Singleton), daher ist synchronized notwendig.
     */
    public synchronized TextDocumentModel getTextDocumentModel()
    {
      if (model == null) model = new TextDocumentModel(doc);
      return model;
    }

    public synchronized boolean hasTextDocumentModel()
    {
      return model != null;
    }

    public String toString()
    {
      return "TextDocumentInfo - model=" + model;
    }
  }
}
