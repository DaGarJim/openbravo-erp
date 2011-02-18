/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2009-2010 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.json;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.ActiveEnabled;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalUtil;

/**
 * Is responsible for converting Openbravo business objects ({@link BaseOBObject} to a json
 * representation. This converter supports both converting single BaseOBObject instances and a
 * collection of business objects.
 * 
 * Values are converted as follows:
 * <ul>
 * <li>Reference values are converted as a JSONObject with only the id and identifier set.</li>
 * <li>Primitive date values are converted to a representation following the xml formatting.</li>
 * <li>Other primitive values are converted by the JSONObject itself.</li>
 * </ul>
 * 
 * @author mtaal
 */
public class DataToJsonConverter {

  public static final String REF_SEPARATOR = "/";

  // TODO: need to be revisited when client side data formatting is solved
  private final SimpleDateFormat xmlDateFormat = new SimpleDateFormat("yyyy-MM-dd");
  private final SimpleDateFormat xmlDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  // additional properties to return as a flat list
  private List<String> additionalProperties = new ArrayList<String>();

  /**
   * Convert a list of Maps with key value pairs to a list of {@link JSONObject}.
   * 
   * @param data
   *          the list of Maps
   * @return the corresponding list of JSONObjects
   */
  public List<JSONObject> convertToJsonObjects(List<Map<String, Object>> data) {
    try {
      final List<JSONObject> jsonObjects = new ArrayList<JSONObject>();
      for (Map<String, Object> dataInstance : data) {
        final JSONObject jsonObject = new JSONObject();
        for (String key : dataInstance.keySet()) {
          final Object value = dataInstance.get(key);
          if (value instanceof BaseOBObject) {
            addBaseOBObject(jsonObject, key, (BaseOBObject) value);
          } else {
            // TODO: format!
            jsonObject.put(key, convertPrimitiveValue(value));
          }
        }
        jsonObjects.add(jsonObject);
      }
      return jsonObjects;
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Convert a list of {@link BaseOBObject} to a list of {@link JSONObject}.
   * 
   * @param bobs
   *          the list of BaseOBObjects to convert
   * @return the corresponding list of JSONObjects
   */
  public List<JSONObject> toJsonObjects(List<BaseOBObject> bobs) {
    final List<JSONObject> jsonObjects = new ArrayList<JSONObject>();
    for (BaseOBObject bob : bobs) {
      jsonObjects.add(toJsonObject((BaseOBObject) bob, DataResolvingMode.FULL));
    }
    return jsonObjects;
  }

  /**
   * Convert a single {@link BaseOBObject} into a {@link JSONObject}.
   * 
   * @param bob
   *          the BaseOBObject to convert
   * @param dataResolvingMode
   *          the data resolving mode determines how much information is converted (only the
   *          identifying info or everything).
   * @return the converted object
   */
  public JSONObject toJsonObject(BaseOBObject bob, DataResolvingMode dataResolvingMode) {
    try {
      final JSONObject jsonObject = new JSONObject();
      jsonObject.put(JsonConstants.IDENTIFIER, bob.getIdentifier());
      jsonObject.put(JsonConstants.ENTITYNAME, bob.getEntityName());
      jsonObject.put(JsonConstants.REF, encodeReference(bob));
      if (dataResolvingMode == DataResolvingMode.SHORT) {
        jsonObject.put(JsonConstants.ID, bob.getId());
        if (bob instanceof ActiveEnabled) {
          jsonObject.put(JsonConstants.ACTIVE, ((ActiveEnabled) bob).isActive());
        }
        return jsonObject;
      }

      for (Property property : bob.getEntity().getProperties()) {
        if (property.isOneToMany()) {
          // ignore these for now....
          continue;
        }
        final Object value = bob.get(property.getName());
        if (value != null) {
          if (property.isPrimitive()) {
            // TODO: format!
            jsonObject.put(property.getName(), convertPrimitiveValue(property, value));
          } else {
            addBaseOBObject(jsonObject, property.getName(), (BaseOBObject) value);
          }
        } else {
          jsonObject.put(property.getName(), JSONObject.NULL);
        }
      }
      for (String additionalProperty : additionalProperties) {
        final Object value = getValueFromPath(bob, additionalProperty);
        if (value instanceof BaseOBObject) {
          addBaseOBObject(jsonObject, additionalProperty, (BaseOBObject) value);
        } else {
          final Property property = DalUtil
              .getPropertyFromPath(bob.getEntity(), additionalProperty);
          // identifier
          if (additionalProperty.endsWith(JsonConstants.IDENTIFIER)) {
            jsonObject.put(additionalProperty, value);
          } else {
            jsonObject.put(additionalProperty, convertPrimitiveValue(property, value));
          }
        }
      }

      return jsonObject;
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * ToDO: replace with call to DalUtil.getValueFromPath after MP14 because of small issue solved in
   * DalUtil in that release.
   */
  private Object getValueFromPath(BaseOBObject bob, String propertyPath) {
    final String[] parts = propertyPath.split("\\.");
    BaseOBObject currentBob = bob;
    Property result = null;
    Object value = null;
    for (String part : parts) {
      // only consider it as an identifier if it is called an identifier and
      // the entity does not accidentally have an identifier property
      // && !currentEntity.hasProperty(part)
      // NOTE disabled for now, there is one special case: AD_Column.IDENTIFIER
      // which is NOT HANDLED
      if (part.equals(JsonConstants.IDENTIFIER)) {
        return currentBob.getIdentifier();
      }
      final Entity currentEntity = currentBob.getEntity();
      if (!currentEntity.hasProperty(part)) {
        return null;
      }
      value = currentBob.get(part);
      // if there is a next step, just make it
      // if it is last then we stop anyway
      if (value instanceof BaseOBObject) {
        currentBob = (BaseOBObject) value;
      } else {
        return value;
      }
    }
    return result;
  }

  private void addBaseOBObject(JSONObject jsonObject, String propertyName, BaseOBObject obObject)
      throws JSONException {
    // jsonObject.put(propertyName, toJsonObject(obObject, DataResolvingMode.SHORT));
    jsonObject.put(propertyName, obObject.getId());
    // jsonObject.put(propertyName + "." + JsonConstants.ID, obObject.getId());
    // jsonObject.put(propertyName + "." + JsonConstants.IDENTIFIER, obObject.getIdentifier());
  }

  // TODO: do some form of formatting here?
  protected Object convertPrimitiveValue(Property property, Object value) {
    final Class<?> clz = property.getPrimitiveObjectType();
    if (Date.class.isAssignableFrom(clz)) {
      if (property.isDatetime()) {
        return xmlDateTimeFormat.format(value);
      } else {
        return xmlDateFormat.format(value);
      }
    }
    return value;
  }

  protected Object convertPrimitiveValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Date) {
      return xmlDateFormat.format(value);
    }
    return value;
  }

  protected String encodeReference(BaseOBObject bob) {
    return bob.getEntityName() + REF_SEPARATOR + bob.getId();
  }

  public List<String> getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperties(List<String> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }
}
