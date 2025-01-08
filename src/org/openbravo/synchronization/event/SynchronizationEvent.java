/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2022-2025 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.synchronization.event;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;

/**
 * The entry point to launch synchronization events
 */
@ApplicationScoped
public class SynchronizationEvent {

  private static final Logger log = LogManager.getLogger();
  private static final ThreadLocal<String> currentEventContext = new ThreadLocal<>();

  @Inject
  @Any
  private Instance<EventContextFilter> eventContextFilters;

  @Inject
  @Any
  private Instance<EventTrigger> eventTriggers;

  /**
   * @return the SynchronizationEvent instance
   */
  public static SynchronizationEvent getInstance() {
    return WeldUtils.getInstanceFromStaticBeanManager(SynchronizationEvent.class);
  }

  /**
   * Sets the current event context
   *
   * @param eventContext
   *          The identifier of the event context to be set as the current one
   */
  public void setCurrentEventContext(String eventContext) {
    currentEventContext.set(eventContext);
  }

  /**
   * Clears the current event context
   */
  public void clearCurrentEventContext() {
    currentEventContext.remove();
  }

  /**
   * @return the current event context or null if no event context has been explicitly set
   */
  public String getCurrentEventContext() {
    return currentEventContext.get();
  }

  /**
   * Checks if the given event context is the current one
   *
   * @param eventContext
   *          The identifier of the event context to be checked if it is the current one
   *
   * @return true if the given event context is the current one or false otherwise
   */
  public boolean isCurrentEventContext(String eventContext) {
    String current = getCurrentEventContext();
    return current != null && current.equals(eventContext);
  }

  /**
   * Checks if it is allowed to trigger an event according to the current context and the context
   * filter whose name is provided. Note that if no event context filter with the given name is
   * found, false is returned.
   *
   * @param eventContextFilter
   *          The name of the event context filter
   *
   * @return true if event triggering is allowed; false otherwise
   */
  public boolean isEventTriggeringAllowed(String eventContextFilter) {
    return getEventContextFilter(eventContextFilter).map(filter -> filter.matches()).orElse(false);
  }

  /**
   * Retrieves an event context filter by its name
   * 
   * @param eventContextFilter
   *          The name of the event context filter
   *
   * @return an Optional describing the event context filter instance with the provided name or an
   *         empty optional if no instance can be found
   *
   * @throws OBException
   *           in case multiple event context filters with the same name are found
   */
  private Optional<EventContextFilter> getEventContextFilter(String eventContextFilter) {
    if (eventContextFilter == null) {
      return Optional.empty();
    }
    List<EventContextFilter> contexts = eventContextFilters.stream()
        .filter(filter -> filter.getName().equals(eventContextFilter))
        .collect(Collectors.toList());
    int size = contexts.size();
    if (size > 1) {
      throw new OBException("Found multiple event context filters with name " + eventContextFilter);
    }
    return size == 0 ? Optional.empty() : Optional.of(contexts.get(0));
  }

  /**
   * Triggers a single record synchronization event
   * 
   * @param event
   *          The unique identifier of the synchronization event
   * @param recordId
   *          the ID that identifies the record related to the event. This is used to generate the
   *          event payload.
   */
  public void triggerEvent(String event, String recordId) {
    log.trace("Triggering event {} for record ID {} under context {}", event, recordId,
        getCurrentEventContext());
    Optional<EventTrigger> optTrigger = getEventTrigger(event);
    if (optTrigger.isPresent()) {
      optTrigger.get().triggerEvent(event, recordId);
      log.trace("Triggered event {} for record ID {} under context {}", event, recordId,
          getCurrentEventContext());
    } else {
      log.trace("No trigger found for event {}, record ID {}", event, recordId);
    }
  }

  /**
   * Triggers a multiple record synchronization event
   * 
   * @param event
   *          The unique identifier of the multiple record synchronization event
   * @param params
   *          The map of parameters used to obtain the records that will be related to the event.
   *          The keys are the parameter name and the map values are the values for each parameter.
   */
  public void triggerEvent(String event, Map<String, Object> params) {
    log.trace("Triggering multiple record event {} with params {} under context {}", event, params,
        getCurrentEventContext());
    Optional<EventTrigger> optTrigger = getEventTrigger(event);
    if (optTrigger.isPresent()) {
      optTrigger.get().triggerEvent(event, params);
      log.trace("Triggered multiple record event {} with params {} under context {}", event, params,
          getCurrentEventContext());
    } else {
      log.trace("No trigger found for multiple record event {}, params {}", event, params);
    }
  }

  /**
   * Selects the EvenTrigger instance with most priority that is able to trigger the provided event.
   * 
   * @param event
   *          The unique identifier of a synchronization event
   *
   * @return an Optional describing the EvenTrigger instance with most priority that is able to
   *         trigger the provided event
   */
  private Optional<EventTrigger> getEventTrigger(String event) {
    return eventTriggers.stream()
        .filter(trigger -> trigger.handlesEvent(event))
        .sorted(Comparator.comparingInt(EventTrigger::getPriority))
        .findFirst();
  }
}
