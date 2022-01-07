/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * DataGeneratorPanel.java
 * Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package com.weka.gui.explorer;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.weka.core.Instances;
import com.weka.core.OptionHandler;
import com.weka.core.Utils;
import com.weka.datagenerators.DataGenerator;
import com.weka.gui.GenericObjectEditor;
import com.weka.gui.Logger;
import com.weka.gui.PropertyPanel;
import com.weka.gui.SysErrLog;

/** 
 * A panel for generating artificial data via DataGenerators.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class DataGeneratorPanel
  extends JPanel {

  /** for serialization */
  private static final long serialVersionUID = -2520408165350629380L;

  /** the GOE for the generators */
  protected GenericObjectEditor m_GeneratorEditor = new GenericObjectEditor();

  /** the generated Instances */
  protected Instances m_Instances = null;

  /** the generated com.output (as text) */
  protected StringWriter m_Output = new StringWriter();

  /** The destination for log/status messages */
  protected Logger m_Log = new SysErrLog();

  /** register the classes */
  static {
     GenericObjectEditor.registerEditors();
  }
  
  /**
   * creates the panel
   */
  public DataGeneratorPanel() {
    setLayout(new BorderLayout());
   
    add(new PropertyPanel(m_GeneratorEditor), BorderLayout.CENTER);

    // editor
    m_GeneratorEditor.setClassType(DataGenerator.class);
    m_GeneratorEditor.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
	repaint();
      }
    });
    
    // set default generator
    setGenerator(null);
  }

  /**
   * Sets the Logger to receive informational messages
   *
   * @param value 	the Logger that will now get info messages
   */
  public void setLog(Logger value) {
    m_Log = value;
  }

  /**
   * returns the generated instances, null if the process was cancelled.
   *
   * @return the generated Instances
   */
  public Instances getInstances() {
    return m_Instances;
  }

  /**
   * returns the generated com.output as text
   * 
   * @return		the generated com.output
   */
  public String getOutput() {
    return m_Output.toString();
  }

  /**
   * sets the generator to use initially
   * 
   * @param value	the data generator to use
   */
  public void setGenerator(DataGenerator value) {
    if (value != null)
      m_GeneratorEditor.setValue(value);
    else
      m_GeneratorEditor.setValue(
          new com.weka.datagenerators.classifiers.classification.RDG1());
  }

  /**
   * returns the currently selected DataGenerator
   * 
   * @return		the current data generator
   */
  public DataGenerator getGenerator() {
    return (DataGenerator) m_GeneratorEditor.getValue();
  }


  /**
   * generates the instances, returns TRUE if successful
   *
   * stores com.output as a string
   *
   * @return		TRUE if successful
   * @see #getInstances()
   */
  public boolean execute() {
    return execute(true);
  }

  /**
   * generates the instances, returns TRUE if successful
   *
   * @param storeOutput whether to store com.output as string
   *
   * @return		TRUE if successful
   * @see #getInstances()
   */
  public boolean execute(boolean storeOutput) {
    
    boolean result = true;
    DataGenerator generator = (DataGenerator) m_GeneratorEditor.getValue();

    String cname = generator.getClass().getName().replaceAll(".*\\.", "");
    String cmd = generator.getClass().getName();
    if (generator instanceof OptionHandler) {
      cmd += " " + Utils.joinOptions(((OptionHandler) generator).getOptions());
    }

    try {
      m_Log.logMessage("Started " + cname);
      m_Log.logMessage("Command: " + cmd);
      m_Output = new StringWriter();
      generator.setOutput(new PrintWriter(m_Output));

      // define dataset format
      // computes actual number of examples to be produced
      generator.defineDataFormat();

      if (storeOutput) {
        m_Output.append(generator.getPrologue());
      } else {
        m_Output.append("String com.output of data generator has not been stored");
      }
      if (generator.getSingleModeFlag()) {
        m_Instances = generator.getDatasetFormat();
        for (int i = 0; i < generator.getNumExamplesAct(); i++) {
          m_Instances.add(generator.generateExample());
        }
      } else {
        m_Instances = generator.generateExamples();
      }
      if (storeOutput) {
        m_Output.append(m_Instances.toString());
        m_Output.append(generator.getEpilogue());
      }
      generator.getOutput().close();
      m_Log.logMessage("Finished " + cname);
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(this, "Error generating data:\n" + e.getMessage(),
          "Error", JOptionPane.ERROR_MESSAGE);
      m_Instances = null;
      m_Output    = new StringWriter();
      result      = false;
    }

    return result;
  }
}