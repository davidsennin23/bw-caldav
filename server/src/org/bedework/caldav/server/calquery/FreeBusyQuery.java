/* **********************************************************************
    Copyright 2007 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
package org.bedework.caldav.server.calquery;

import org.bedework.caldav.server.CalDavParseUtil;
import org.bedework.caldav.server.CaldavBWIntf;
import org.bedework.caldav.server.SysIntf;
import org.bedework.calfacade.base.TimeRange;
import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwEventObj;
import org.bedework.calfacade.CalFacadeDefs;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.cct.webdav.servlet.shared.WebdavNsIntf;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;
import edu.rpi.sss.util.xml.tagdefs.WebdavTags;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

/**
 * @author Mike Douglass douglm @ rpi.edu
 */
public class FreeBusyQuery {
  private boolean debug;

  private CaldavBWIntf intf;

  protected transient Logger log;

  private TimeRange timeRange;

  /** Constructor
   *
   * @param intf
   * @param debug
   */
  public FreeBusyQuery(WebdavNsIntf intf, boolean debug) {
    this.intf = (CaldavBWIntf)intf;
    this.debug = debug;
  }

  /** The given node is is the free-busy-query time-range element
   * Should have exactly one time-range element.
   *
   * @param nd
   * @throws WebdavException
   */
  public void parse(Node nd) throws WebdavException {
    try {
      if (timeRange != null) {
        throw new WebdavBadRequest();
      }

      if (!CaldavTags.timeRange.nodeMatches(nd)) {
        throw new WebdavBadRequest();
      }

      timeRange = CalDavParseUtil.parseTimeRange(nd, null,
                                                 intf.getSysi().getTimezones());

      if (debug) {
        trace("Parsed time range " + timeRange);
      }
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavBadRequest();
    }
  }

  /**
   * @param sysi
   * @param cal
   * @param account
   * @param depth
   * @return BwEvent
   * @throws WebdavException
   */
  public BwEvent getFreeBusy(SysIntf sysi, BwCalendar cal,
                             String account,
                             int depth) throws WebdavException {
    try {
      int calType = cal.getCalType();

      if (!BwCalendar.collectionInfo[calType].allowFreeBusy) {
        throw new WebdavForbidden(WebdavTags.supportedReport);
      }

      if ((depth == 0) && (calType != BwCalendar.calTypeCollection)) {
        /* Cannot return anything */
        cal = null;
      } else if ((depth == 1) && !cal.getCalendarCollection()) {
        /* Make new cal object with just calendar collections as children */
        BwCalendar newCal = new BwCalendar();

        for (BwCalendar ch: sysi.getCalendars(cal)) {
          if (ch.getCalType() == BwCalendar.calTypeCollection) {
            newCal.addChild(ch);
          }
        }
        cal = newCal;
      }

      BwEvent fb;
      if (cal == null) {
        fb = new BwEventObj();
        fb.setEntityType(CalFacadeDefs.entityTypeFreeAndBusy);
        fb.setDtstart(timeRange.getStart());
        fb.setDtend(timeRange.getEnd());
      } else {
        fb = sysi.getFreeBusy(cal, account,
                              timeRange.getStart(),
                              timeRange.getEnd());
      }

      if (debug) {
        trace("Got " + fb);
      }

      return fb;
    } catch (WebdavException wde) {
      throw wde;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Debug method
   *
   */
  public void dump() {
    trace("<free-busy-query>");

    timeRange.dump(getLogger(), "  ");

    trace("</free-busy-query>");
  }

  /*
  private Element[] getChildren(Node nd) throws WebdavException {
    try {
      return XmlUtil.getElementsArray(nd);
    } catch (Throwable t) {
      if (debug) {
        getLogger().error("<filter>: parse exception: ", t);
      }

      throw new WebdavBadRequest();
    }
  }
  */

  /** ===================================================================
   *                   Logging methods
   *  =================================================================== */

  /**
   * @return Logger
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void debugMsg(String msg) {
    getLogger().debug(msg);
  }

  protected void error(Throwable t) {
    getLogger().error(this, t);
  }

  protected void logIt(String msg) {
    getLogger().info(msg);
  }

  protected void trace(String msg) {
    getLogger().debug(msg);
  }
}

