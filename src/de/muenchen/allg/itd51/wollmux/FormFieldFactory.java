/*
 * Dateiname: FormFieldFactory.java
 * Projekt  : WollMux
 * Funktion : Repr�sentiert eine Fabrik, die an der Stelle von 
 *            WM('insertFormValue'...)-Bookmark entsprechende FormField-Elemente
 *            erzeugt.
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
 * 08.06.2006 | LUT | Erstellung als FormField
 * 14.06.2006 | LUT | Umbenennung in FormFieldFactory und Unterst�tzung
 *                    von Checkboxen.
 * 07.09.2006 | BNK | Rewrite
 * 12.09.2006 | BNK | Bugfix: Bookmarks ohne Ausdehnung wurden nicht gefunden.
 * 03.01.2007 | BNK | +TextFieldFormField
 *                  | +createFormField(doc, textfield)
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.awt.XControlModel;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNamed;
import com.sun.star.drawing.XControlShape;
import com.sun.star.frame.XController;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.table.XCell;
import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.TextRangeRelation.TreeRelation;

/**
 * Repr�sentiert eine Fabrik, die an der Stelle von WM('insertFormValue'...)-Bookmark
 * entsprechende FormField-Elemente erzeugt.
 * 
 * @author lut
 */
public final class FormFieldFactory
{
  public static final Pattern INSERTFORMVALUE =
    Pattern.compile("\\A\\s*(WM\\s*\\(.*CMD\\s*'((insertFormValue))'.*\\))\\s*\\d*\\z");

  /**
   * Erzeugt ein Formualfeld im Dokument doc an der Stelle des
   * InsertFormValue-Kommandos cmd. Ist unter dem bookmark bereits ein
   * Formularelement (derzeit TextFeld vom Typ Input, DropDown oder eine Checkbox)
   * vorhanden, so wird dieses Feld als Formularelement f�r die Darstellung des
   * Wertes des Formularfeldes genutzt. Ist innerhalb des Bookmarks noch kein
   * Formularelement vorhanden, so wird ein DynamicInputFormField an der Stelle des
   * Bookmarks erzeugt, das erst dann ein InputField-Textfeld im Dokument anlegt,
   * wenn auf das Textfeld schreibend zugegriffen wird.
   * 
   * @param doc
   *          das Dokument, zu dem das Formularfeld geh�rt.
   * @param cmd
   *          das zugeh�rige insertFormValue-Kommando.
   */
  public static FormField createFormField(XTextDocument doc, InsertFormValue cmd,
      Map<String, FormField> bookmarkNameToFormField)
  {
    String bookmarkName = cmd.getBookmarkName();
    FormField formField = bookmarkNameToFormField.get(bookmarkName);
    if (formField != null) return formField;

    /*
     * Falls die range in einer Tabellenzelle liegt, wird sie auf die ganze Zelle
     * ausgeweitet, damit die ganze Zelle gescannt wird (Workaround f�r Bug
     * http://qa.openoffice.org/issues/show_bug.cgi?id=68261)
     */
    XTextRange range = cmd.getAnchor();
    if (range != null)
    {
      range = range.getText();
      XCell cell = UNO.XCell(range);
      if (cell == null)
        range = null;
      else if (WollMuxFiles.isDebugMode())
      {
        String cellName = (String) UNO.getProperty(cell, "CellName");
        Logger.debug(L.m("Scanne Zelle %1", cellName));
      }
    }

    if (range == null) range = cmd.getTextRange();

    if (range != null)
    {
      XEnumeration xenum = UNO.XEnumerationAccess(range).createEnumeration();
      handleParagraphEnumeration(xenum, doc, bookmarkNameToFormField);
    }

    return bookmarkNameToFormField.get(bookmarkName);
  }

  /**
   * Erzeugt ein neues FormField f�r das Serienbrieffeld textfield vom Typ
   * c,s,s,text,textfield,Database, das im Dokument doc liegt. Die Methoden
   * {@link Object#equals(java.lang.Object)} und {@link Object#hashCode()} beziehen
   * sich auf das zugrundeliegende UNO-Objekt, wobei verschiedene Proxies des selben
   * Objekts als gleich behandelt werden.
   * 
   * @param doc
   *          das zugeh�rige Dokument doc
   * @param textfield
   *          ein Serienbrieffeld vom Typ css.text.textfield.Database.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static FormField createDatabaseFormField(XTextDocument doc,
      XTextField textfield)
  {
    return new DatabaseFormField(doc, textfield);
  }

  /**
   * Erzeugt ein neues FormField f�r ein Eingabefeld einer Benutzervariablen vom Typ
   * c,s,s,text,textfield,InputUser und den zugeh�rigen TextFieldMaster master die im
   * Dokument doc liegen. Die Methoden {@link Object#equals(java.lang.Object)} und
   * {@link Object#hashCode()} beziehen sich auf das zugrundeliegende UNO-Objekt,
   * wobei verschiedene Proxies des selben Objekts als gleich behandelt werden.
   * 
   * @param doc
   *          das zugeh�rige Dokument doc
   * @param textfield
   *          das InputUser-Objekt.
   * @param master
   *          bei InputUser-Objekten kann auf den angezeigten Wert nicht direkt
   *          zugegriffen werden. Diese Zugriffe erfolgen �ber einen TextFieldMaster,
   *          der dem InputUser-Objekt zugeordnet ist. VORSICHT: Das Objekt
   *          textfield.TextFieldMaster ist dabei nicht als Master geeignet, da
   *          dieser Master keine direkte M�glichkeit zum Setzen der Anzeigewerte
   *          anbietet. Das statt dessen geeignete TextFieldMaster-Objekt muss �ber
   *          doc.getTextFieldMasters() bezogen werden, wobei textfield und master
   *          dann zusammen geh�ren, wenn textfield.Content.equals(master.Name) gilt.
   * @return
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static FormField createInputUserFormField(XTextDocument doc,
      XTextField textfield, XPropertySet master)
  {
    return new InputUserFormField(doc, textfield, master);
  }

  /**
   * Geht die XEnumeration enu von Abs�tzen und TextTables durch und erzeugt f�r alle
   * in Abs�tzen (nicht TextTables) enthaltenen insertFormValue-Bookmarks
   * entsprechende Eintr�ge in mapBookmarkNameToFormField. Falls n�tig wird das
   * entsprechende FormField erzeugt.
   * 
   * @param doc
   *          das Dokument in dem sich die enumierten Objekte befinden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static void handleParagraphEnumeration(XEnumeration enu,
      XTextDocument doc, Map<String, FormField> mapBookmarkNameToFormField)
  {
    XEnumerationAccess enuAccess;
    while (enu.hasMoreElements())
    {
      Object ele;
      try
      {
        ele = enu.nextElement();
      }
      catch (java.lang.Exception x)
      {
        continue;
      }
      enuAccess = UNO.XEnumerationAccess(ele);
      if (enuAccess != null) // ist wohl ein SwXParagraph
      {
        handleParagraph(enuAccess, doc, mapBookmarkNameToFormField);
      }
    }
  }

  /**
   * Geht die XEnumeration enuAccess von TextPortions durch und erzeugt f�r alle
   * enthaltenen insertFormValue-Bookmarks entsprechende Eintr�ge in
   * mapBookmarkNameToFormField. Falls n�tig wird das entsprechende FormField
   * erzeugt.
   * 
   * @param doc
   *          das Dokument in dem sich die enumierten Objekte befinden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static void handleParagraph(XEnumerationAccess paragraph,
      XTextDocument doc, Map<String, FormField> mapBookmarkNameToFormField)
  {
    /*
     * Der Name des zuletzt gestarteten insertFormValue-Bookmarks.
     */
    String lastInsertFormValueStart = null;
    XNamed lastInsertFormValueBookmark = null;

    /*
     * enumeriere alle TextPortions des Paragraphs
     */
    XEnumeration textPortionEnu = paragraph.createEnumeration();
    while (textPortionEnu.hasMoreElements())
    {
      /*
       * Diese etwas seltsame Konstruktion beugt Exceptions durch Bugs wie 68261 vor.
       */
      Object textPortion;
      try
      {
        textPortion = textPortionEnu.nextElement();
      }
      catch (java.lang.Exception x)
      {
        continue;
      }
      ;

      String textPortionType =
        (String) UNO.getProperty(textPortion, "TextPortionType");
      if (textPortionType.equals("Bookmark"))
      {
        XNamed bookmark = null;
        boolean isStart = false;
        boolean isCollapsed = false;
        try
        {
          isStart =
            ((Boolean) UNO.getProperty(textPortion, "IsStart")).booleanValue();
          isCollapsed =
            ((Boolean) UNO.getProperty(textPortion, "IsCollapsed")).booleanValue();
          if (isCollapsed) isStart = true;
          bookmark = UNO.XNamed(UNO.getProperty(textPortion, "Bookmark"));
        }
        catch (java.lang.Exception x)
        {
          continue;
        }
        if (bookmark == null) continue;

        String name = bookmark.getName();
        Matcher m = INSERTFORMVALUE.matcher(name);
        if (m.matches())
        {
          if (isStart)
          {
            lastInsertFormValueStart = name;
            lastInsertFormValueBookmark = bookmark;
          }
          if (!isStart || isCollapsed)
          {
            if (name.equals(lastInsertFormValueStart))
            {
              handleNewInputField(lastInsertFormValueStart, bookmark,
                mapBookmarkNameToFormField, doc);
              lastInsertFormValueStart = null;
            }
          }
        }
      }
      else if (textPortionType.equals("TextField"))
      {
        XDependentTextField textField = null;
        int textfieldType = 0; // 0:input, 1:dropdown, 2: reference
        try
        {
          textField =
            UNO.XDependentTextField(UNO.getProperty(textPortion, "TextField"));
          XServiceInfo info = UNO.XServiceInfo(textField);
          if (info.supportsService("com.sun.star.text.TextField.DropDown"))
            textfieldType = 1;
          else if (info.supportsService("com.sun.star.text.TextField.Input"))
            textfieldType = 0;
          else
            continue; // sonstiges TextField
        }
        catch (java.lang.Exception x)
        {
          continue;
        }

        switch (textfieldType)
        {
          case 0:
            handleInputField(textField, lastInsertFormValueStart,
              mapBookmarkNameToFormField, doc);
            break;
          case 1:
            handleDropdown(textField, lastInsertFormValueStart,
              mapBookmarkNameToFormField, doc);
            break;
        }
        lastInsertFormValueStart = null;
      }
      else if (textPortionType.equals("Frame"))
      {
        XControlModel model = null;
        try
        {
          XEnumeration contentEnum =
            UNO.XContentEnumerationAccess(textPortion).createContentEnumeration(
              "com.sun.star.text.TextPortion");
          while (contentEnum.hasMoreElements())
          {
            XControlShape tempShape;
            try
            {
              tempShape = UNO.XControlShape(contentEnum.nextElement());
            }
            catch (Exception x)
            {
              // Wegen OOo Bugs kann nextElement() werfen auch wenn hasMoreElements()
              continue;
            }

            if (tempShape != null)
            {
              XControlModel tempModel = tempShape.getControl();
              XServiceInfo info = UNO.XServiceInfo(tempModel);
              if (info.supportsService("com.sun.star.form.component.CheckBox"))
              {
                model = tempModel;
              }
            }
          }
        }
        catch (java.lang.Exception x)
        {
          continue;
        }

        handleCheckbox(model, lastInsertFormValueStart, mapBookmarkNameToFormField,
          doc);
        lastInsertFormValueStart = null;
      }
      else
        // sonstige TextPortion
        continue;
    }

    if (lastInsertFormValueStart != null)
      handleNewInputField(lastInsertFormValueStart, lastInsertFormValueBookmark,
        mapBookmarkNameToFormField, doc);

  }

  /**
   * F�gt ein neues Eingabefeld innerhalb des Bookmarks bookmark ein, erzeugt ein
   * dazugeh�riges FormField und setzt ein passendes Mapping von bookmarkName auf
   * dieses FormField in mapBookmarkNameToFormField
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD 5.1)
   */
  private static void handleNewInputField(String bookmarkName, XNamed bookmark,
      Map<String, FormField> mapBookmarkNameToFormField, XTextDocument doc)
  {
    FormField formField = new DynamicInputFormField(doc);
    mapBookmarkNameToFormField.put(bookmarkName, formField);
  }

  private static void handleInputField(XDependentTextField textfield,
      String bookmarkName, Map<String, FormField> mapBookmarkNameToFormField,
      XTextDocument doc)
  {
    if (textfield != null)
    {
      FormField formField = new InputFormField(doc, null, textfield);
      mapBookmarkNameToFormField.put(bookmarkName, formField);
    }
  }

  private static void handleDropdown(XDependentTextField textfield,
      String bookmarkName, Map<String, FormField> mapBookmarkNameToFormField,
      XTextDocument doc)
  {
    if (textfield != null)
    {
      FormField formField = new DropDownFormField(doc, null, textfield);
      mapBookmarkNameToFormField.put(bookmarkName, formField);
    }
  }

  private static void handleCheckbox(XControlModel checkbox, String bookmarkName,
      Map<String, FormField> mapBookmarkNameToFormField, XTextDocument doc)
  {
    if (checkbox != null)
    {
      FormField formField = new CheckboxFormField(doc, null, checkbox);
      mapBookmarkNameToFormField.put(bookmarkName, formField);
    }
  }

  /**
   * Dieses Interface beschreibt die Eigenschaften eines Formularfeldes unter einem
   * WM(CMD'insertFormValue'...)-Kommando.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  interface FormField extends Comparable<FormField>
  {
    /**
     * FIXME Unsch�ne Fixup-Funktion, die in FormScanner.executeCommand() aufgerufen
     * wird, da der Scan von Tabellenzellen nur die Bookmarks, aber nicht die
     * zugeh�rigen Commands kennt.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public abstract void setCommand(InsertFormValue cmd);

    /**
     * Wenn das Feld die 1-zu-1 Ersetzung der Referenz auf die ID oldFieldId durch
     * eine neue ID newFieldId unterst�tzt, dann wird diese Ersetzung vorgenommen und
     * true zur�ckgeliefert, ansonsten false.
     */
    public abstract boolean substituteFieldID(String oldFieldId, String newFieldId);

    /**
     * Liefert die XTextRange, an der das Formularfeld verankert ist.
     */
    public XTextRange getAnchor();

    /**
     * Die Methode liefert den Namen der Trafo, die auf dieses Formularfeld gesetzt
     * ist oder null, wenn keine Trafo gesetzt ist.
     */
    public abstract String getTrafoName();

    /**
     * Eine R�ckgabe von true gibt an, dass die Trafo (falls definiert) nur einen
     * Parameter sinnvoll verarbeiten kann. Ein derartiges Verhalten ist f�r alle
     * Dokumentkommandos WM(CMD'insertFormValue' ID'<id>' TRAFO '<trafo>')
     * spezifiziert. In diesem Fall erwartet die Trafo f�r jeden in der Funktion
     * geforderten Parameter den Wert von <id>; Eine R�ckgabe von false beschreibt,
     * dass die Trafo auch mehrere Parameter verarbeiten kann (wie z.B.
     * InputUserFields der Fall).
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public abstract boolean singleParameterTrafo();

    /**
     * Diese Methode belegt den Wert des Formularfeldes im Dokument mit dem neuen
     * Inhalt value.
     * 
     * @param value
     */
    public abstract void setValue(String value);

    /**
     * Diese Methode liefert den aktuellen Wert des Formularfeldes als String zur�ck
     * oder den Leerstring, falls der Wert nicht bestimmt werden kann.
     * 
     * @return der aktuelle Wert des Formularfeldes als String
     */
    public abstract String getValue();

    /**
     * Setzt den ViewCursor auf die Position des InputFields.
     * 
     * @param doc
     */
    public abstract void focus();

    /**
     * L�scht das Formularfeld aus dem Dokument
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public abstract void dispose();
  }

  private static abstract class BasicFormField implements FormField
  {
    protected XTextDocument doc;

    protected InsertFormValue cmd;

    public void setCommand(InsertFormValue cmd)
    {
      this.cmd = cmd;
    };

    /**
     * Erzeugt ein Formualfeld im Dokument doc an der Stelle des
     * InsertFormValue-Kommandos cmd. Ist unter dem bookmark bereits ein TextFeld vom
     * Typ InputField vorhanden, so wird dieses Feld als inputField f�r die
     * Darstellung des Wertes des Formularfeldes genutzt. Ist innerhalb des Bookmarks
     * noch kein InputField vorhanden, so wird ein neues InputField in den Bookmark
     * eingef�gt.
     * 
     * @param doc
     *          das Dokument, zu dem das Formularfeld geh�rt.
     * @param cmd
     *          das zugeh�rige insertFormValue-Kommando.
     * @param focusRange
     *          Beschreibt die range, auf die der ViewCursor beim Aufruf der
     *          focus()-methode gesetzt werden soll. Der Parameter ist erforderlich,
     *          da das Setzen des viewCursors auf die TextRanges des Kommandos cmd
     *          unter Linux nicht sauber funktioniert.
     */
    public BasicFormField(XTextDocument doc, InsertFormValue cmd)
    {
      this.doc = doc;
      this.cmd = cmd;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormField#getTrafoName()
     */
    public String getTrafoName()
    {
      return cmd.getTrafoName();
    }

    /**
     * Diese Methode liest den Inhalt des internen Formularelements und liefert den
     * Wert als String zur�ck.
     * 
     * @param value
     *          der neue Wert des Formularelements.
     */
    public abstract String getFormElementValue();

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormField#getValue()
     */
    public String getValue()
    {
      return getFormElementValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormField#focus()
     */
    public void focus()
    {
      if (cmd == null) return;
      try
      {
        XController controller = UNO.XModel(doc).getCurrentController();
        XTextCursor cursor = UNO.XTextViewCursorSupplier(controller).getViewCursor();
        XTextRange focusRange = cmd.getTextRange();
        if (focusRange != null) cursor.gotoRange(focusRange, false);
      }
      catch (java.lang.Exception e)
      {}
    }

    /**
     * Vergleicht die Positionen der Dokumentkommandos der Formularfelder im Dokument
     * liefert -1 zur�ck, wenn this vor other liegt, 1, wenn this nach other liegt
     * und beide Formularfelder dem selben Text-Objekt zugeordnet sind und 0, wenn
     * sich die Dokumentkommandos �berlappen; l�sst sich die Ordnung nicht bestimmen,
     * da die Text-Objekte sich unterscheiden, dann wird -1 geliefert.
     * 
     * @param other
     *          Das Vergleichsobjekt.
     * 
     * @return
     */
    public int compareTo(FormField other)
    {
      BasicFormField other2;
      try
      {
        other2 = (BasicFormField) other;
      }
      catch (Exception x)
      {
        return -1;
      }

      TreeRelation rel = new TreeRelation(cmd.getAnchor(), other2.cmd.getAnchor());
      if (rel.isAGreaterThanB())
        return 1;
      else if (rel.isALessThanB())
        return -1;
      else if (rel.isAEqualB()) return 0;

      return -1;
    }

    public boolean substituteFieldID(String oldFieldId, String newFieldId)
    {
      if (oldFieldId == null || newFieldId == null) return false;
      if (cmd.getID().equals(oldFieldId))
      {
        cmd.setID(newFieldId);
        return true;
      }
      return false;
    }

    public XTextRange getAnchor()
    {
      return cmd.getAnchor();
    }

    public void dispose()
    {
      cmd.markDone(true);
    }
  }

  /**
   * Repr�sentiert ein FormField, das den Formularwert in einem Input-Field
   * darstellt.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  private static class InputFormField extends BasicFormField
  {
    protected XTextField inputField;

    public InputFormField(XTextDocument doc, InsertFormValue cmd,
        XTextField inputField)
    {
      super(doc, cmd);
      this.inputField = inputField;
    }

    public void setValue(String value)
    {
      if (inputField != null && UNO.XUpdatable(inputField) != null)
      {
        UNO.setProperty(inputField, "Content", value);
        UNO.XUpdatable(inputField).update();
      }
    }

    public String getFormElementValue()
    {
      if (inputField != null)
      {
        Object content = UNO.getProperty(inputField, "Content");
        if (content != null) return content.toString();
      }
      return "";
    }

    public boolean singleParameterTrafo()
    {
      return true;
    }
  }

  /**
   * Repr�sentiert ein FormField-Objekt, das zun�chst kein Formularelement enth�lt,
   * aber eines vom Typ c,s,s,text,TextField,InputField erzeugt, wenn mittels focus()
   * oder setFormElementValue(...) darauf zugegriffen wird und der zu setzende Wert
   * nicht der Leerstring ist.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  private static class DynamicInputFormField extends InputFormField
  {

    public DynamicInputFormField(XTextDocument doc)
    {
      super(doc, null, null);
    }

    public void setCommand(InsertFormValue cmd)
    {
      super.setCommand(cmd);
      // R43846 (#2439)
      if (inputField == null)
      {
        XTextRange range = cmd.createInsertCursor(false);
        if (range != null)
        {
          String textSurroundedByBookmark = range.getString();
          String trimmedText = textSurroundedByBookmark.trim();
          Pattern p = Workarounds.workaroundForIssue101249();
          if (trimmedText.length() > 0 && !p.matcher(trimmedText).matches())
          {
            // 1. Kollabiere in allen F�llen wo jetzt eine Warnung ausgegeben wird
            // das
            // Bookmark vor den Text (Warnung muss weiterhin ausgegeben werden).
            // [FERTIG]
            // FIXME 2. Kollabierte Bookmarks werden dann und nur dann dekollabiert,
            // wenn
            // ein nicht-leerer Text eingef�gt wird.
            // FIXME 3. Alle Stellen, die Bookmarks und daraus abgeleitete Textranges
            // verarbeiten m�ssen einen Spezialfall f�r kollabierte Bookmarks haben
            // FIXME 4. An allen Stellen, wo der Inhalt eines Bookmarks ver�ndert
            // wird,
            // wird gepr�ft ob der neue Inhalt der leere String ist. Falls ja, wird
            // das Bookmark durch ein kollabiertes ersetzt.
            // FIXME 5. Gleich beim initialen Scan werden nicht kollabierte Bookmarks
            // mit
            // leerem Inhalt in kollabierte umgewandelt (Performanceverlust in gro�em
            // Formular m�glich, weil bislang unn�tige Zugriffe auf Bookmarks und
            // deren Inhalt, was in OOo eventuell eine lineare Suche ausl�sen kann)
            //
            //
            Logger.log(L.m(
              "Kollabiere Textmarke \"%2\" die um den Text \"%1\" herum liegt.",
              textSurroundedByBookmark, cmd.getBookmarkName()));
            try
            {
              String bmName = cmd.getBookmarkName();
              new Bookmark(bmName, UNO.XBookmarksSupplier(doc)).remove();
              XTextRange start = range.getStart();
              XTextContent bookmark =
                UNO.XTextContent(UNO.XMultiServiceFactory(doc).createInstance(
                  "com.sun.star.text.Bookmark"));
              UNO.XNamed(bookmark).setName(bmName);
              start.getText().insertTextContent(start, bookmark, false);
            }
            catch (Exception x)
            {}

          }
        }
      }
    }

    public void setValue(String value)
    {
      if (cmd == null) return;

      if (value.length() == 0)
      {
        // wenn kein inputField vorhanden ist, so wird der Inhalt des Bookmarks
        // gel�scht.
        if (inputField == null)
        {
          XTextRange range = cmd.createInsertCursor(false);
          if (range != null)
            range.setString(Workarounds.workaroundForIssue101283());
        }
      }
      else
      {
        // Erzeuge Formularelement wenn notwendig
        if (inputField == null) createInputField();
      }
      super.setValue(value);
    }

    private void createInputField()
    {
      if (cmd == null) return;

      String bookmarkName = cmd.getBookmarkName();

      Logger.debug2(L.m("Erzeuge neues Input-Field f�r Bookmark \"%1\"",
        bookmarkName));
      try
      {
        XTextRange range = cmd.createInsertCursor(false);
        XText text = range.getText();
        XTextField field =
          UNO.XTextField(UNO.XMultiServiceFactory(doc).createInstance(
            "com.sun.star.text.TextField.Input"));
        XTextCursor cursor = text.createTextCursorByRange(range);

        if (cursor != null && field != null)
          text.insertTextContent(cursor, field, true);

        inputField = field;
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Repr�sentiert ein FormField, das den Formularwert in einem DropDown-Field
   * darstellt.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  private static class DropDownFormField extends BasicFormField
  {
    private XTextField dropdownField;

    private String[] origItemList = null;

    public DropDownFormField(XTextDocument doc, InsertFormValue cmd,
        XTextField dropdownField)
    {
      super(doc, cmd);
      this.dropdownField = dropdownField;

      if (dropdownField != null)
        origItemList = (String[]) UNO.getProperty(dropdownField, "Items");

    }

    public void setValue(String value)
    {
      // DropDownFormFelder k�nnen in OOo nicht mit dem Leerstring belegt
      // werden. Die Verwendung des Leerstrings f�rht dazu, dass ein anderes als
      // das ausgew�hlte Element angezeigt wird. Daher werden Leerstrings auf
      // ein Leerzeichen umgeschrieben. OOo-Issue: #70087
      if (value.equals("")) value = " ";

      if (dropdownField != null && UNO.XUpdatable(dropdownField) != null)
      {
        extendItemsList(value);
        UNO.setProperty(dropdownField, "SelectedItem", value);
        UNO.XUpdatable(dropdownField).update();
      }
    }

    /**
     * Die Methode pr�ft, ob der String value bereits in der zum Zeitpunkt des
     * Konstruktoraufrufs eingelesenen Liste oritItemList der erlaubten Eintr�ge der
     * ComboBox vorhanden ist und erweitert die Liste um value, falls nicht.
     * 
     * @param value
     *          der Wert, der ggf. an in die Liste der erlaubten Eintr�ge aufgenommen
     *          wird.
     */
    private void extendItemsList(String value)
    {
      if (origItemList != null)
      {
        boolean found = false;
        for (int i = 0; i < origItemList.length; i++)
        {
          if (value.equals(origItemList[i]))
          {
            found = true;
            break;
          }
        }

        if (!found)
        {
          String[] extendedItems = new String[origItemList.length + 1];
          for (int i = 0; i < origItemList.length; i++)
            extendedItems[i] = origItemList[i];
          extendedItems[origItemList.length] = value;
          UNO.setProperty(dropdownField, "Items", extendedItems);
        }
      }
    }

    public String getFormElementValue()
    {
      if (dropdownField != null)
      {
        Object content = UNO.getProperty(dropdownField, "SelectedItem");
        if (content != null) return content.toString();
      }
      return "";
    }

    public boolean singleParameterTrafo()
    {
      return true;
    }
  }

  /**
   * Repr�sentiert ein FormField, das den Formularwert in einer Checkbox darstellt.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  private static class CheckboxFormField extends BasicFormField
  {
    private Object checkbox;

    /**
     * Erzeugt eine neue CheckboxFormField, das eine bereits im Dokument doc
     * bestehende Checkbox checkbox vom Service-Typ
     * com.sun.star.form.component.CheckBox an der Stelle des Kommandos cmd
     * repr�sentiert.
     * 
     * @param doc
     *          Das Dokument in dem sich das Checkbox-Formularfeld-Kommando befindet
     * @param cmd
     *          das zum Formularfeld zugeh�rige insertFormValue-Kommando
     * @param checkbox
     *          Ein UNO-Service vom Typ von com.sun.star.form.component.CheckBox das
     *          den Zugriff auf das entsprechende FormControl-Element erm�glicht.
     * @param focusRange
     *          Beschreibt die range, auf die der ViewCursor beim Aufruf der
     *          focus()-methode gesetzt werden soll.
     */
    public CheckboxFormField(XTextDocument doc, InsertFormValue cmd, Object checkbox)
    {
      super(doc, cmd);

      this.checkbox = checkbox;
    }

    public void setValue(String value)
    {
      Boolean bv = Boolean.valueOf(value);

      UNO.setProperty(checkbox, "State",
        ((bv.booleanValue()) ? Short.valueOf((short) 1) : Short.valueOf((short) 0)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormFieldFactory.BasicFormField#getFormElementValue()
     */
    public String getFormElementValue()
    {
      Object state = UNO.getProperty(checkbox, "State");
      if (state != null && state.equals(Short.valueOf((short) 1)))
        return "true";
      else
        return "false";
    }

    public boolean singleParameterTrafo()
    {
      return true;
    }
  }

  /**
   * Kapselt ein Serienbrieffeld UNO-Objekt vom Typ c,s,s,text,textfield,Database als
   * FormField. In einem Serienbrieffeld kann keine TRAFO-Funktion gesetzt werden -
   * deshalb liefert die Methode getTrafoName() immer null zur�ck. Die Objekte dieser
   * Klasse betrachten zum Zwecke von equals() und hashCode() die zugrundeliegenden
   * UNO-Objekte.
   * 
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1)
   */
  private static class DatabaseFormField implements FormField
  {
    private XTextField textfield;

    private XTextDocument doc;

    public DatabaseFormField(XTextDocument doc, XTextField textfield)
    {
      this.textfield = textfield;
      this.doc = doc;
    }

    /**
     * Nicht verwendet.
     */
    public void setCommand(InsertFormValue cmd)
    {
    // nicht verwendet
    }

    public String getTrafoName()
    {
      // diese Felder sind immer untransformiert
      return null;
    }

    public void setValue(String value)
    {
      if (value == null) return;
      UNO.setProperty(textfield, "Content", value);
      UNO.setProperty(textfield, "CurrentPresentation", value);
    }

    public String getValue()
    {
      String cont = (String) UNO.getProperty(textfield, "Content");
      if (cont == null)
        cont = (String) UNO.getProperty(textfield, "CurrentPresentation");
      if (cont != null) return cont;
      return "";
    }

    public void focus()
    {
      try
      {
        XController controller = UNO.XModel(doc).getCurrentController();
        XTextCursor cursor = UNO.XTextViewCursorSupplier(controller).getViewCursor();
        XTextRange focusRange = UNO.XTextContent(textfield).getAnchor();
        if (focusRange != null) cursor.gotoRange(focusRange, false);
      }
      catch (java.lang.Exception e)
      {}
    }

    public int hashCode()
    {
      return UnoRuntime.generateOid(UNO.XInterface(textfield)).hashCode();
    }

    public boolean equals(Object b)
    {
      return UnoRuntime.areSame(UNO.XInterface(textfield), UNO.XInterface(b));
    }

    public boolean substituteFieldID(String oldFieldId, String newFieldId)
    {
      return false;
    }

    public XTextRange getAnchor()
    {
      return textfield.getAnchor();
    }

    public void dispose()
    {
      if (textfield != null) textfield.dispose();
    }

    public int compareTo(FormField o)
    {
      throw new UnsupportedOperationException();
    }

    public boolean singleParameterTrafo()
    {
      // Der R�ckgabewert spielt keine Rolle da diese Felder immer untransformiert
      // sind.
      return false;
    }
  }

  /**
   * Kapselt ein Eingabefeld f�r eine Benutzervariable vom Typ
   * c,s,s,text,textfield,InputUser und den zugeh�rigen TextFieldMaster master als
   * FormField. Bei InputUser-Objekten kann auf den angezeigten Wert nicht direkt
   * zugegriffen werden. Diese Zugriffe erfolgen �ber einen TextFieldMaster, der dem
   * InputUser-Objekt zugeordnet ist. VORSICHT: Das Objekt textfield.TextFieldMaster
   * ist dabei nicht als Master geeignet, da dieser Master keine direkte M�glichkeit
   * zum Setzen der Anzeigewerte anbietet. Das statt dessen geeignete
   * TextFieldMaster-Objekt muss �ber doc.getTextFieldMasters() bezogen werden, wobei
   * textfield und master dann zusammen geh�ren, wenn
   * textfield.Content.equals(master.Name) gilt. Die Objekte dieser Klasse betrachten
   * zum Zwecke von equals() und hashCode() die zugrundeliegenden UNO-Objekte.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static class InputUserFormField implements FormField
  {
    private XTextDocument doc;

    private XTextField textfield;

    private XPropertySet master;

    public InputUserFormField(XTextDocument doc, XTextField textfield,
        XPropertySet master)
    {
      this.doc = doc;
      this.textfield = textfield;
      this.master = master;
    }

    public void setCommand(InsertFormValue cmd)
    {
    // nicht notwendig
    }

    public void setValue(final String value)
    {
      if (value == null) return;
      UNO.setProperty(master, "Content", value);
      if (UNO.XUpdatable(textfield) != null) UNO.XUpdatable(textfield).update();
    }

    public String getTrafoName()
    {
      return TextDocumentModel.getFunctionNameForUserFieldName(""
        + UNO.getProperty(textfield, "Content"));
    }

    public String getValue()
    {
      if (master == null) return "";
      return "" + UNO.getProperty(master, "Content");
    }

    public void focus()
    {
      try
      {
        XController controller = UNO.XModel(doc).getCurrentController();
        XTextCursor cursor = UNO.XTextViewCursorSupplier(controller).getViewCursor();
        XTextRange focusRange = UNO.XTextContent(textfield).getAnchor();
        if (focusRange != null) cursor.gotoRange(focusRange, false);
      }
      catch (java.lang.Exception e)
      {}
    }

    public int hashCode()
    {
      return UnoRuntime.generateOid(UNO.XInterface(textfield)).hashCode();
    }

    public boolean equals(Object b)
    {
      return UnoRuntime.areSame(UNO.XInterface(textfield), UNO.XInterface(b));
    }

    public boolean substituteFieldID(String oldFieldId, String newFieldId)
    {
      return false;
    }

    public XTextRange getAnchor()
    {
      return textfield.getAnchor();
    }

    public void dispose()
    {
      if (textfield != null) textfield.dispose();
    }

    public int compareTo(FormField o)
    {
      throw new UnsupportedOperationException();
    }

    public boolean singleParameterTrafo()
    {
      return false;
    }
  }
}
