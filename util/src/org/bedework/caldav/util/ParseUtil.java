/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/
package org.bedework.caldav.util;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.sss.util.DateTimeUtil;

import net.fortuna.ical4j.model.DateTime;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Calendar;

/** Some utilities for parsing caldav
 *
 * @author Mike Douglass douglm @ rpi.edu
 */
public class ParseUtil {
  /** The given node must be a time-range element
   *  <!ELEMENT time-range EMPTY>
   *
   *  <!ATTLIST time-range start CDATA
   *                       end CDATA>
   *
   *  Start and end are date with UTC time
   *
   * e.g.        <C:time-range start="20040902T000000Z"
   *                           end="20040902T235959Z"/>
   *
   * @param nd
   * @param tzid - timezone to use if specified
   * @return TimeRange
   * @throws WebdavException
   */
  public static TimeRange parseTimeRange(Node nd) throws WebdavException {
    DateTime start = null;
    DateTime end = null;

    NamedNodeMap nnm = nd.getAttributes();

    /* draft 5 has neither attribute required - the intent is that either
       may be absent */

    if ((nnm == null) || (nnm.getLength() == 0)) {
      // Infinite time-range?
      throw new WebdavBadRequest("Infinite time range");
    }

    int attrCt = nnm.getLength();

    try {
      Node nmAttr = nnm.getNamedItem("start");

      if (nmAttr != null) {
        attrCt--;
        start = new DateTime(nmAttr.getNodeValue());
      }

      nmAttr = nnm.getNamedItem("end");

      if (nmAttr != null) {
        attrCt--;
        end = new DateTime(nmAttr.getNodeValue());
      }
    } catch (Throwable t) {
      throw new WebdavBadRequest();
    }

    if (attrCt != 0) {
      throw new WebdavBadRequest();
    }

    return new TimeRange(start, end);
  }

  /** Get a date/time range given by the rfc formatted parameters and limited to
   * the given max range
   *
   * @param start
   * @param end
   * @param defaultField
   * @param defaultVal
   * @param maxField
   * @param maxVal
   * @return TimeRange or null for bad request
   * @throws WebdavException
   */
  public static TimeRange getPeriod(String start, String end,
                                    int defaultField, int defaultVal,
                                    int maxField,
                                    int maxVal) throws WebdavException {
    Calendar startCal = Calendar.getInstance();
    startCal.set(Calendar.HOUR_OF_DAY, 0);
    startCal.set(Calendar.MINUTE, 0);
    startCal.set(Calendar.SECOND, 0);

    Calendar endCal = Calendar.getInstance();
    endCal.set(Calendar.HOUR_OF_DAY, 0);
    endCal.set(Calendar.MINUTE, 0);
    endCal.set(Calendar.SECOND, 0);

    try {
      if (start != null) {
        startCal.setTime(DateTimeUtil.fromDate(start));
      }

      if (end == null) {
        endCal.add(defaultField, defaultVal);
      } else {
        endCal.setTime(DateTimeUtil.fromDate(end));
      }
    } catch (DateTimeUtil.BadDateException bde) {
      throw new WebdavBadRequest();
    }

    // Don't allow more than the max
    Calendar check = Calendar.getInstance();
    check.setTime(startCal.getTime());
    check.add(maxField, maxVal);

    if (check.before(endCal)) {
      return null;
    }

    return new TimeRange(new DateTime(startCal.getTime()),
                         new DateTime(endCal.getTime()));
  }
}