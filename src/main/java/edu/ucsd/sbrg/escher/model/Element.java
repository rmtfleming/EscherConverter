/* ---------------------------------------------------------------------
 * This file is part of the program EscherConverter.
 *
 * Copyright (C) 2013-2017 by the University of California, San Diego.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package edu.ucsd.sbrg.escher.model;

/**
 * @author Andreas Dr&auml;ger
 */
public interface Element extends EscherBase {

  /**
   * @return the id
   */
  String getId();

  /**
   * @return {@code true} if the requested property is not {@code null}.
   */
  boolean isSetId();

  /**
   * @param id the id to set
   */
  void setId(String id);

}
