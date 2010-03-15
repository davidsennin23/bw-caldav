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
package org.bedework.caldav.server;

import org.bedework.caldav.server.sysinterface.SysIntf;

import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cmt.access.AccessPrincipal;
import edu.rpi.cmt.access.PrivilegeDefs;
import edu.rpi.cmt.access.Acl.CurrentAccess;
import edu.rpi.sss.util.DateTimeUtil;

import org.w3c.dom.Element;

import java.io.InputStream;

import javax.xml.namespace.QName;

/** Class to represent a resource such as a file.
 *
 *   @author Mike Douglass   douglm rpi.edu
 */
public class CaldavResourceNode extends CaldavBwNode {
  private CalDAVResource resource;

  private AccessPrincipal owner;

  private String entityName;

  private CurrentAccess currentAccess;

  /** Place holder for status
   *
   * @param sysi
   * @param status
   * @param uri
   * @param debug
   */
  public CaldavResourceNode(final SysIntf sysi, final int status, final String uri, final boolean debug) {
    super(true, sysi, uri, debug);
    setStatus(status);
  }

  /** Constructor
   *
   * @param cdURI
   * @param sysi
   * @param debug
   * @throws WebdavException
   */
  public CaldavResourceNode(final CaldavURI cdURI,
                             final SysIntf sysi, final boolean debug) throws WebdavException {
    super(cdURI, sysi, debug);

    resource = cdURI.getResource();
    col = cdURI.getCol();
    collection = false;
    allowsGet = true;
    entityName = cdURI.getEntityName();

    if (resource != null) {
      exists = !resource.isNew();
      //resource.setPrevLastmod(resource.getLastmod());
      //resource.setPrevSeq(resource.getPrevSeq());
    } else {
      exists = false;
    }
  }

  @Override
  public void init(final boolean content) throws WebdavException {
    if (!content) {
      return;
    }

    try {
      if ((resource == null) && exists) {
        if (entityName == null) {
          exists = false;
          return;
        }
      }
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getOwner()
   */
  @Override
  public AccessPrincipal getOwner() throws WebdavException {
    if (owner == null) {
      if (resource == null) {
        return null;
      }

      owner = resource.getOwner();
    }

    return owner;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#removeProperty(org.w3c.dom.Element)
   */
  @Override
  public boolean removeProperty(final Element val,
                                final SetPropertyResult spr) throws WebdavException {
    warn("Unimplemented - removeProperty");

    return false;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#setProperty(org.w3c.dom.Element)
   */
  @Override
  public boolean setProperty(final Element val,
                             final SetPropertyResult spr) throws WebdavException {
    if (super.setProperty(val, spr)) {
      return true;
    }

    return false;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#update()
   */
  @Override
  public void update() throws WebdavException {
    if (resource != null) {
      getSysi().updateFile(resource, true);
    }
  }

  /**
   * @return String
   */
  public String getEntityName() {
    return entityName;
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#trailSlash()
   */
  @Override
  public boolean trailSlash() {
    return false;
  }

  /* ====================================================================
   *                   Property methods
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#knownProperty(edu.rpi.sss.util.xml.QName)
   */
  @Override
  public boolean knownProperty(final QName tag) {
    // Not ours
    return super.knownProperty(tag);
  }

  /**
   * @param val
   */
  public void setResource(final CalDAVResource val) {
    resource = val;
  }

  /** Returns the resource object
   *
   * @return CalDAVResource
   * @throws WebdavException
   */
  public CalDAVResource getResource() throws WebdavException {
    init(true);

    return resource;
  }

  /* ====================================================================
   *                   Overridden property methods
   * ==================================================================== */

  @Override
  public CurrentAccess getCurrentAccess() throws WebdavException {
    if (currentAccess != null) {
      return currentAccess;
    }

    if (resource == null) {
      return null;
    }

    try {
      currentAccess = getSysi().checkAccess(resource, PrivilegeDefs.privAny, true);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }

    return currentAccess;
  }

  @Override
  public String getEtagValue(final boolean strong) throws WebdavException {
    init(true);

    if (resource == null) {
      return null;
    }

    String val = resource.getTagValue();

    if (strong) {
      return "\"" + val + "\"";
    }

    return "W/\"" + val + "\"";
  }

  /* *
   * @param strong
   * @return etag before changes
   * @throws WebdavException
   * /
  public String getPrevEtagValue(boolean strong) throws WebdavException {
    init(true);

    if (resource == null) {
      return null;
    }

    return makeEtag(resource.getPrevLastmod(), resource.getPrevSeq(), strong);
  }*/

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("CaldavResourceNode{");
    sb.append("path=");
    sb.append(getPath());
    sb.append(", entityName=");
    sb.append(String.valueOf(entityName));
    sb.append("}");

    return sb.toString();
  }

  /* ====================================================================
   *                   Required webdav properties
   * ==================================================================== */

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentBinary()
   */
  @Override
  public boolean getContentBinary() throws WebdavException {
    return true;
  }

  @Override
  public InputStream getContentStream() throws WebdavException {
    return resource.getBinaryContent();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentString()
   */
  @Override
  public String getContentString() throws WebdavException {
    init(true);
    throw new WebdavException("binary content");
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentLang()
   */
  @Override
  public String getContentLang() throws WebdavException {
    return "en";
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentLen()
   */
  @Override
  public long getContentLen() throws WebdavException {
    init(true);

    if (resource == null) {
      return 0;
    }

    return resource.getContentLen();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getContentType()
   */
  @Override
  public String getContentType() throws WebdavException {
    if (resource == null) {
      return null;
    }

    return resource.getContentType();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getCreDate()
   */
  @Override
  public String getCreDate() throws WebdavException {
    init(false);

    if (resource == null) {
      return null;
    }

    return resource.getCreated();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getDisplayname()
   */
  @Override
  public String getDisplayname() throws WebdavException {
    return getEntityName();
  }

  /* (non-Javadoc)
   * @see edu.rpi.cct.webdav.servlet.shared.WebdavNsNode#getLastmodDate()
   */
  @Override
  public String getLastmodDate() throws WebdavException {
    init(false);

    if (resource == null) {
      return null;
    }

    try {
      return DateTimeUtil.fromISODateTimeUTCtoRfc822(resource.getLastmod());
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }
}
