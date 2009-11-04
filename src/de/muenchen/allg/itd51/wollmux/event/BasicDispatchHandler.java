/*
 * Dateiname: BasicDispatchHandler.java
 * Projekt  : WollMux
 * Funktion : Dient als Basisklasse f�r DispatchHandler, die XDispatch implementieren und f�r die Bearbeitung von Dispatch-Aufrufen zu GENAU EINER URL zust�ndig sind.
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
 * 28.10.2006 | LUT | Erstellung als DispatchInterceptor
 * 10.01.2007 | LUT | Umbenennung in DispatchHandler: Behandelt jetzt
 *                    auch globale WollMux Dispatches
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.event;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Vector;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XStatusListener;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.URL;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;

  /**
   * Dient als Basisklasse f�r DispatchHandler, die XDispatch implementieren und f�r
   * die Bearbeitung von Dispatch-Aufrufen zu GENAU EINER URL zust�ndig sind.
   * 
   * @author christoph.lutz
   */
  abstract class BasicDispatchHandler implements XDispatch
  {
    /**
     * Enth�lt die url, auf die dieser DispatchHandler reagiert.
     */
    protected final String urlStr;

    /**
     * Enth�lt alle aktuell registrierten StatusListener
     */
    protected final Vector<XStatusListener> statusListener =
      new Vector<XStatusListener>();

    /**
     * Erzeugt einen DispatchHandler, der Dispatches mit der url urlStr bearbeiten
     * kann.
     * 
     * @param urlStr
     */
    public BasicDispatchHandler(String urlStr)
    {
      this.urlStr = urlStr;
    }

    /**
     * F�hrt den Dispatch selbst durch, wobei arg das Argument der URL enth�lt (z.B.
     * "internerBriefkopf", wenn url="wollmux:openTemplate#internerBriefkopf" war)
     * und props das PropertyValues[], das auch schon der urspr�nglichen dispatch
     * Methode mitgeliefert wurde. Es kann davon ausgegangen werden, dass arg nicht
     * null ist und falls es nicht vorhanden ist den Leerstring enth�lt.
     * 
     * @param arg
     *          Das Argument das mit der URL mitgeliefert wurde.
     * @param props
     */
    protected abstract void dispatch(String arg, PropertyValue[] props);

    /**
     * Pr�ft ob der DispatchHandler zum aktuellen Zeitpunkt in der Lage ist, den
     * Dispatch abzuhandeln und liefert false zur�ck, wenn der DispatchHandler nicht
     * verwendet werden soll.
     * 
     * @param url
     *          die zu pr�fende URL ohne Argumente (z.B. "wollmux:openTemplate", wenn
     *          die urpr�ngliche URL "wollmux:openTemplate#internerBriefkopf"
     *          enthielt).
     * @return true, wenn der DispatchHandler verwendet werden soll, andernfalls
     *         false.
     */
    public boolean providesUrl(String url)
    {
      return urlStr.equalsIgnoreCase(url);
    }

    /**
     * Benachrichtigt den �bergebenen XStatusListener listener mittels
     * listener.statusChanged() �ber den aktuellen Zustand des DispatchHandlers und
     * setzt z.B. den Zust�nde IsEnabled (Standardm��ig wird IsEnabled=true
     * �bermittelt).
     * 
     * @param listener
     * @param url
     */
    protected void notifyStatusListener(XStatusListener listener, URL url)
    {
      FeatureStateEvent fse = new FeatureStateEvent();
      fse.FeatureURL = url;
      fse.IsEnabled = true;
      listener.statusChanged(fse);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatch#dispatch(com.sun.star.util.URL,
     *      com.sun.star.beans.PropertyValue[])
     */
    public void dispatch(URL url, PropertyValue[] props)
    {
      Logger.debug2(this.getClass().getSimpleName() + ".dispatch('" + url.Complete
        + "')");

      // z.B. "wollmux:OpenTemplate#internerBriefkopf"
      // =====> {"wollmux:OpenTemplate", "internerBriefkopf"}
      String arg = "";
      String[] parts = url.Complete.split("#", 2);
      if (parts.length == 2) arg = parts[1];

      // arg durch den URL-Decoder jagen:
      try
      {
        arg = URLDecoder.decode(arg, ConfigThingy.CHARSET);
      }
      catch (UnsupportedEncodingException e)
      {
        Logger.error(L.m("Fehler in Dispatch-URL '%1':", url.Complete), e);
      }

      dispatch(arg, props);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatch#addStatusListener(com.sun.star.frame.XStatusListener,
     *      com.sun.star.util.URL)
     */
    public void addStatusListener(XStatusListener listener, URL url)
    {
      boolean alreadyRegistered = false;
      Iterator<XStatusListener> iter = statusListener.iterator();
      while (iter.hasNext())
        if (UnoRuntime.areSame(UNO.XInterface(iter.next()), listener))
          alreadyRegistered = true;

      if (!alreadyRegistered) statusListener.add(listener);

      notifyStatusListener(listener, url);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatch#removeStatusListener(com.sun.star.frame.XStatusListener,
     *      com.sun.star.util.URL)
     */
    public void removeStatusListener(XStatusListener listener, URL x)
    {
      Iterator<XStatusListener> iter = statusListener.iterator();
      while (iter.hasNext())
        if (UnoRuntime.areSame(UNO.XInterface(iter.next()), listener))
          iter.remove();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
      return this.getClass().getSimpleName() + "('" + urlStr + "')";
    }
  }
