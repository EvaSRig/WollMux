//TODO L.m()
/*
 * Dateiname: FunctionSelectionAccess.java
 * Projekt  : WollMux
 * Funktion : Interface zum Zugriff auf eine FunctionSelection.
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 27.09.2006 | BNK | Erstellung
 * 16.03.2007 | BNK | +updateFieldReferences()
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.function;

import java.util.Map;

import de.muenchen.allg.itd51.parser.ConfigThingy;

/**
 * Interface zum Zugriff auf eine FunctionSelection.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface FunctionSelectionAccess
{

  /**
   * Dieser spezielle Funktionsname signalisiert, dass keine Funktion ausgew�hlt ist.
   */
  public static final String NO_FUNCTION = "<keine>";

  /**
   * Dieser spezielle Funktionsname signalisiert, dass der Benutzer die Funktion
   * manuell eingegeben hat und sie direkt in dieser FunctionSelection gespeichert ist.
   */
  public static final String EXPERT_FUNCTION = "<Experte>";

  /**
   * Liefert true gdw diese FunctionSelection eine Referenz auf eine benannte Funktion ist,
   * d,h, wenn {@link #getFunctionName()} einen sinnvollen Namen liefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isReference();
  
  /**
   * Liefert true gdw diese FunctionSelection eine vom Benutzer manuell eingegebene Funktion
   * ist, die von {@link #getExpertFunction()} zur�ckgeliefert werden kann.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isExpert();
  
  /**
   * Liefert true gdw, keine Funktion ausgew�hlt ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isNone();

  /**
   * Liefert true gdw diese FunctionSelection eine Referenz auf eine benannte Funktion ist und 
   * f�r mindestens einen Parameter dieser Funktion einen Wert spezifiziert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean hasSpecifiedParameters();
  
  /**
   * Liefert den Namen der Funktion, falls es eine Referenz auf eine externe Funktion ist, oder
   * {@link #NO_FUNCTION} bzw, {@link #EXPERT_FUNCTION}.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getFunctionName();
  
  /**
   * Liefert die Namen der Funktionsparameter, die die momentan ausgew�hlte Funktion 
   * erwartet.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String[] getParameterNames();

  /**
   * Nimmt eine Abbildung von Parameternamen (Strings) auf Parameterwerte ({@link ParamValue}s)
   * und �bernimmt diese direkt als Referenz.
   * @param mapNameToParamValue wird direkt als Referenz �bernommen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setParameterValues(Map<String, ParamValue> mapNameToParamValue);
  
  /**
   * Liefert den f�r Parameter paramName gesetzten Wert. Ist f�r paramName kein Wert gesetzt,
   * so wird dennoch ein ParamValue geliefert, jedoch eines f�r das {@link ParamValue#isUnspecified()}
   * true liefert. Das zur�ckgelieferte Objekt ist kann vom Aufrufer ver�ndert werden, ohne
   * Auswirkungen auf das FunctionSelectionAccess Objekt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ParamValue getParameterValue(String paramName);
  
  /**
   * Setzt den Wert f�r Parameter paramName auf paramValue.
   * @param paramValue wird direkt als Referenz in die internen Datenstrukturen �bernommen, darf also
   *        vom Aufrufer nachher nicht mehr ge�ndert werden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setParameterValue(String paramName, ParamValue paramValue);

  /**
   * �ndert den Namen der Funktion auf functionName und die Liste der Namen ihrer
   * Parameter auf paramNames.
   * @param functionName hier k�nnen auch die Spezialnamen {@link #NO_FUNCTION} oder
   *        {@link #EXPERT_FUNCTION} verwendet werden.
   * @param paramNames wird ignoriert, falls {@link #NO_FUNCTION} oder {@link #EXPERT_FUNCTION} 
   *        �bergeben wird. Wird ansonsten kopiert, nicht direkt als Referenz verwendet.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setFunction(String functionName, String[] paramNames);

  /**
   * Liefert eine Kopie der gespeicherten vom Benutzer manuell eingegebenen
   * Funktion. Ist keine gesetzt, so wird ein ConfigThingy ohne Kinder zur�ckgeliefert.
   * @return ein Objekt, das der Aufrufer �ndern darf.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy getExpertFunction();
  
  /**
   * Schaltet den Typ dieser FunctionSelection auf {@link #EXPERT_FUNCTION} und setzt
   * die zugeh�rige Funktionsdefinition auf conf. conf muss einen beliebigen Wurzelknoten haben,
   * wie z.B. "PLAUSI", "AUTOFILL" o.�. der noch keine Grundfunktion ist.
   * @param funConf wird kopiert, nicht als Referenz �bernommen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setExpertFunction(ConfigThingy funConf);

}
