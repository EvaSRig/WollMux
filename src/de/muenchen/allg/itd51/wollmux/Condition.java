/*
* Dateiname: Condition.java
* Projekt  : WollMux
* Funktion : Repr�sentiert eine Bedingung, die wahr oder falsch sein kann und Repr�sentiert eine Bedingung, die wahr oder falsch sein kann und von verschiedenen Werten abh�ngt.
* 
* Copyright: Landeshauptstadt M�nchen
*
* �nderungshistorie:
* Datum      | Wer | �nderungsgrund
* -------------------------------------------------------------------
* 02.02.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux;

import java.util.Collection;
import java.util.Map;

/**
 * Repr�sentiert eine Bedingung, die wahr oder falsch sein kann und Repr�sentiert eine Bedingung, die wahr oder falsch sein kann 
 * und von verschiedenen Werten abh�ngt.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Condition
{
  /**
   * Liefert true, wenn die Bedingung f�r die Values aus mapIdToValue
   * erf erf�llt ist.
   * @param mapIdToValue bildet IDs auf ihre Values ab.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean check(Map mapIdToValue);
  
  /**
   * Liefert eine Collection der IDs der Values von denen diese
   * Condition abh�ngt, d,h, die IDs die mindestens in der Map vorhanden sein
   * m�ssen, die an check() �bergeben wird. ACHTUNG! Die zur�ckgelieferte
   * Collection darf nicht ver�ndert werden!
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Collection dependencies();

}
