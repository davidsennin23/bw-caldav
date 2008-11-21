/*
 Copyright (c) 2000-2005 University of Washington.  All rights reserved.

 Redistribution and use of this distribution in source and binary forms,
 with or without modification, are permitted provided that:

   The above copyright notice and this permission notice appear in
   all copies and supporting documentation;

   The name, identifiers, and trademarks of the University of Washington
   are not used in advertising or publicity without the express prior
   written permission of the University of Washington;

   Recipients acknowledge that this distribution is made available as a
   research courtesy, "as is", potentially with defects, without
   any obligation on the part of the University of Washington to
   provide support, services, or repair;

   THE UNIVERSITY OF WASHINGTON DISCLAIMS ALL WARRANTIES, EXPRESS OR
   IMPLIED, WITH REGARD TO THIS SOFTWARE, INCLUDING WITHOUT LIMITATION
   ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE, AND IN NO EVENT SHALL THE UNIVERSITY OF
   WASHINGTON BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
   DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
   PROFITS, WHETHER IN AN ACTION OF CONTRACT, TORT (INCLUDING
   NEGLIGENCE) OR STRICT LIABILITY, ARISING OUT OF OR IN CONNECTION WITH
   THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
/* **********************************************************************
    Copyright 2005 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.BwResource;
import org.bedework.calfacade.svc.EventInfo;

import edu.rpi.cmt.access.Ace;
import edu.rpi.cmt.access.PrincipalInfo;

/** We map uris onto an object which may be a calendar or an
 * entity contained within that calendar.
 *
 * <p>The response to webdav actions obviously depends upon the type of
 * referenced entity.
 *
 * <p>The server will have to determine whether a name represents a publicly
 * available calendar or a user and the access to a calendar will, of course,
 * depend upon the authentication state of the user.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class CaldavURI {
  boolean exists;

  /* For a resource or an entity, this is the containing collection
   */
  BwCalendar cal;

  BwResource resource;

  EventInfo entity;

  PrincipalInfo principal;

  String entityName;
  String path;  // for principals

//  boolean userUri; // entityname is user

  boolean resourceUri; // entityname is resource

//  boolean groupUri;// entityname is group

  /** Reference to a collection
   *
   * @param cal
   * @param exists        true if the referenced object exists
   */
  CaldavURI(BwCalendar cal, boolean exists) {
    init(cal, null, null, null, exists);
  }

  /** Reference to a contained entity
   *
   * @param cal
   * @param entity
   * @param entityName
   * @param exists        true if the referenced object exists
   */
  CaldavURI(BwCalendar cal, EventInfo entity, String entityName,
            boolean exists) {
    init(cal, null, entity, entityName, exists);
  }

  /** Reference to a contained resource
   *
   * @param res
   * @param exists        true if the referenced object exists
   */
  CaldavURI(BwResource res,
            boolean exists) {
    init(res.getCalendar(), res, null, res.getName(), exists);
    resourceUri = true;
  }

  /**
   * @param pi
   */
  CaldavURI(PrincipalInfo pi) {
    principal = pi;
    exists = true;
    cal = null;
    entityName = pi.who;
    path = pi.prefix + pi.who;;
  }

  private void init(BwCalendar cal, BwResource res,
                    EventInfo entity, String entityName,
                    boolean exists) {
    this.cal = cal;
    this.resource = res;
    this.entity = entity;
    this.entityName = entityName;
    this.exists = exists;
  }

  /**
   * @return boolean
   */
  public boolean getExists() {
    return exists;
  }

  /**
   * @return BwCalendar
   */
  public BwCalendar getCal() {
    return cal;
  }

  /**
   * @return BwResource
   */
  public BwResource getResource() {
    return resource;
  }

  /**
   * @return Object
   */
  public EventInfo getEntity() {
    return entity;
  }

  /**
   * @return String
   */
  public String getCalName() {
    return cal.getName();
  }

  /**
   * @return String
   */
  public String getEntityName() {
    return entityName;
  }

  /**
   * @return String
   */
  public String getOwner() {
    if (principal != null){
      return principal.who;
    }

    if (entity != null) {
      BwEvent ev = entity.getEvent();
      if (ev != null) {
        return ev.getOwner().getAccount();
      }
    } else if (cal != null) {
      return cal.getOwner().getAccount();
    } else if (resource != null) {
      return resource.getOwner().getAccount();
    }

    return null;
  }

  /**
   * @return String
   */
  public String getPath() {
    if (principal != null) {
      return path;
    }

    return cal.getPath();
  }

  /**
   * @return String
   */
  public String getUri() {
    if ((entityName == null) ||
        (principal != null)) {
      return getPath();
    }
    return getPath() + "/" + entityName;
  }

  /**
   * @return true if this represents a calendar
   */
  public boolean isResource() {
    return resourceUri;
  }

  /**
   * @return true if this represents a calendar
   */
  public boolean isCollection() {
    return entityName == null;
  }

  /**
   * @return true if this represents a user
   */
  public boolean isUser() {
    if (principal == null) {
      return false;
    }
    return principal.whoType == Ace.whoTypeUser;
  }

  /**
   * @return true if this represents a group
   */
  public boolean isGroup() {
    if (principal == null) {
      return false;
    }
    return principal.whoType == Ace.whoTypeGroup;
  }

  /**
   * @param entityName
   * @return true if has same name
   */
  public boolean sameName(String entityName) {
    if ((entityName == null) && (getEntityName() == null)) {
      return true;
    }

    if ((entityName == null) || (getEntityName() == null)) {
      return false;
    }

    return entityName.equals(getEntityName());
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("CaldavURI{path=");

    sb.append(getPath());
    sb.append(", entityName=");
    sb.append(String.valueOf(entityName));
    sb.append("}");

    return sb.toString();
  }

  public int hashCode() {
    if (principal != null) {
      return  principal.hashCode();
    }

    int hc = entityName.hashCode();

    return hc * 3 + cal.getPath().hashCode();
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof CaldavURI)) {
      return false;
    }

    CaldavURI that = (CaldavURI)o;

    if (principal != null) {
      return  principal.equals(that.principal);
    }

    if (cal == null) {
      if (that.cal != null) {
        return false;
      }

      return true;
    }

    if (that.cal == null) {
      return false;
    }

    if (!cal.equals(that.cal)) {
      return false;
    }

    return sameName(that.entityName);
  }
}

