/*
 * Dateiname: HashableComponent.java
 * Projekt  : WollMux
 * Funktion : Wrapper, um UNO-Objekte hashbar zu machen.
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
 * http://ec.europa.eu/idabc/en/document/7330/5980
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 10.12.2007 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux;

import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;

import de.muenchen.allg.afid.UNO;

/**
 * Hilfsklasse, die es erm�glicht, UNO-Componenten in HashMaps abzulegen; der
 * Vergleich zweier HashableComponents mit equals(...) verwendet dazu den
 * sicheren UNO-Vergleich UnoRuntime.areSame(...). Die Methode hashCode
 * verwendet die sichere Oid, die UnoRuntime.generateOid(...) liefert.
 * 
 * @author lut
 */
public class HashableComponent
{
  private XInterface compo;

  public HashableComponent(Object compo)
  {
    this.compo = UNO.XInterface(compo);
    if (this.compo == null) throw new ClassCastException();
  }

  public int hashCode()
  {
    if (compo != null) return UnoRuntime.generateOid(compo).hashCode();
    return 0;
  }

  public boolean equals(Object b)
  {
    if (b != null && b instanceof HashableComponent)
    {
      HashableComponent other = (HashableComponent) b;
      return UnoRuntime.areSame(this.compo, other.compo);
    }
    return false;
  }
}
