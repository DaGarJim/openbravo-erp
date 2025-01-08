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
 * All portions are Copyright (C) 2023-2025 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.synchronization.event;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.openbravo.base.exception.OBException;
import org.openbravo.test.base.MockableBaseTest;

/**
 * Tests for the {@link SynchronizationEvent} class
 */
public class SynchronizationEventTest extends MockableBaseTest {

  private static final String CONTEXT_NAME = "TEST_CONTEXT";
  private static final String CONTEXT_FILTER_NAME = "TEST_FILTER";
  private static final String HANDLED_EVENT = "A";
  private static final String UNHANDLED_EVENT = "B";
  private static final String RECORD_ID = "1";
  private static final Map<String, Object> FILTER_PARAMS = Map.of("organization", "A", "date",
      "2022-04-26");

  @Mock
  private Instance<EventContextFilter> eventContextFilters;

  @Mock
  private Instance<EventTrigger> eventTriggers;

  @Mock
  private EventContextFilter eventContextFilter;

  @Mock
  private EventTrigger eventTrigger;

  @Mock
  private EventTrigger morePrioritizedEventTrigger;

  @InjectMocks
  @Spy
  private SynchronizationEvent synchronizationEvent;

  @Before
  public void stubMethods() {
    when(eventContextFilter.getName()).thenReturn(CONTEXT_FILTER_NAME);

    when(eventTrigger.getPriority()).thenReturn(100);
    when(eventTrigger.handlesEvent(HANDLED_EVENT)).thenReturn(true);
    when(eventTrigger.handlesEvent(UNHANDLED_EVENT)).thenReturn(false);

    when(morePrioritizedEventTrigger.getPriority()).thenReturn(50);
    when(morePrioritizedEventTrigger.handlesEvent(HANDLED_EVENT)).thenReturn(true);
    when(morePrioritizedEventTrigger.handlesEvent(UNHANDLED_EVENT)).thenReturn(false);
  }

  @After
  public void cleanUp() {
    synchronizationEvent.clearCurrentEventContext();
  }

  @Test
  public void triggerEvent() {
    // given a single EventTrigger instance...
    when(eventTriggers.stream()).thenReturn(Stream.of(eventTrigger));

    // ... when triggering an event handled by that EventTrigger instance...
    synchronizationEvent.triggerEvent(HANDLED_EVENT, RECORD_ID);

    // ... then the EventTrigger instance triggers the event
    verify(eventTrigger, times(1)).triggerEvent(HANDLED_EVENT, RECORD_ID);
  }

  @Test
  public void triggerMultipleRecordEvent() {
    // given a single EventTrigger instance...
    when(eventTriggers.stream()).thenReturn(Stream.of(eventTrigger));

    // ... when triggering a multiple record event handled by that EventTrigger instance...
    synchronizationEvent.triggerEvent(HANDLED_EVENT, FILTER_PARAMS);

    // ... then the EventTrigger instance triggers the event
    verify(eventTrigger, times(1)).triggerEvent(HANDLED_EVENT, FILTER_PARAMS);
  }

  @Test
  public void triggerHandledEventWithMostPrioritizedEventTrigger() {
    // given several EventTrigger instances...
    when(eventTriggers.stream()).thenReturn(Stream.of(eventTrigger, morePrioritizedEventTrigger));

    // ... when triggering an event handled by all those EventTrigger instances...
    synchronizationEvent.triggerEvent(HANDLED_EVENT, RECORD_ID);

    // ... then just the EventTrigger instance with most priority triggers the event
    verify(morePrioritizedEventTrigger, times(1)).triggerEvent(HANDLED_EVENT, RECORD_ID);
    verify(eventTrigger, never()).triggerEvent(HANDLED_EVENT, RECORD_ID);
  }

  @Test
  public void triggerHandledMultipleRecordEventWithMostPrioritizedEventTrigger() {
    // given several EventTrigger instances...
    when(eventTriggers.stream()).thenReturn(Stream.of(eventTrigger, morePrioritizedEventTrigger));

    // ... when triggering a multiple record event handled by all those EventTrigger instances...
    synchronizationEvent.triggerEvent(HANDLED_EVENT, FILTER_PARAMS);

    // ... then just the EventTrigger instance with most priority triggers the event
    verify(morePrioritizedEventTrigger, times(1)).triggerEvent(HANDLED_EVENT, FILTER_PARAMS);
    verify(eventTrigger, never()).triggerEvent(HANDLED_EVENT, FILTER_PARAMS);
  }

  @Test
  public void triggerUnhandledEvent() {
    // given several EventTrigger instances...
    when(eventTriggers.stream()).thenReturn(Stream.of(eventTrigger, morePrioritizedEventTrigger));

    // ... when triggering an event not handled by any of those EventTrigger instances...
    synchronizationEvent.triggerEvent(UNHANDLED_EVENT, RECORD_ID);

    // ... then the event is never triggered
    verify(morePrioritizedEventTrigger, never()).triggerEvent(UNHANDLED_EVENT, RECORD_ID);
    verify(eventTrigger, never()).triggerEvent(UNHANDLED_EVENT, RECORD_ID);
  }

  @Test
  public void triggerUnhandledMultipleRecordEvent() {
    // given several EventTrigger instances...
    when(eventTriggers.stream()).thenReturn(Stream.of(eventTrigger, morePrioritizedEventTrigger));

    // ... when triggering a multiple record event not handled by any of those EventTrigger
    // instances...
    synchronizationEvent.triggerEvent(UNHANDLED_EVENT, FILTER_PARAMS);

    // ... then the event is never triggered
    verify(morePrioritizedEventTrigger, never()).triggerEvent(UNHANDLED_EVENT, FILTER_PARAMS);
    verify(eventTrigger, never()).triggerEvent(UNHANDLED_EVENT, FILTER_PARAMS);
  }

  @Test
  public void noCurrentEventContextIsSetByDefault() {
    assertThat(synchronizationEvent.getCurrentEventContext(), nullValue());
  }

  @Test
  public void canSetAndClearTheCurrentEventContext() {
    synchronizationEvent.setCurrentEventContext(CONTEXT_NAME);
    assertThat(synchronizationEvent.getCurrentEventContext(), equalTo(CONTEXT_NAME));

    synchronizationEvent.clearCurrentEventContext();
    assertThat(synchronizationEvent.getCurrentEventContext(), nullValue());
  }

  @Test
  public void checkEventTriggeringNotAllowedByEventContextFilter() {
    when(eventContextFilters.stream()).thenReturn(Stream.of(eventContextFilter));
    doReturn(false).when(eventContextFilter).matches();

    assertThat(synchronizationEvent.isEventTriggeringAllowed(CONTEXT_FILTER_NAME), equalTo(false));
  }

  @Test
  public void checkEventTriggeringAllowedByEventContextFilter() {
    when(eventContextFilters.stream()).thenReturn(Stream.of(eventContextFilter));
    doReturn(true).when(eventContextFilter).matches();

    assertThat(synchronizationEvent.isEventTriggeringAllowed(CONTEXT_FILTER_NAME), equalTo(true));
  }

  @Test
  public void checkEventTriggeringNotAllowedIfThereIsNoEventContextFilter() {
    when(eventContextFilters.stream()).thenReturn(Stream.empty());

    assertThat(synchronizationEvent.isEventTriggeringAllowed(CONTEXT_FILTER_NAME), equalTo(false));
  }

  @Test
  public void checkEventTriggeringNotAllowedByUnknownEventContextFilter() {
    when(eventContextFilters.stream()).thenReturn(Stream.of(eventContextFilter));

    assertThat(synchronizationEvent.isEventTriggeringAllowed("UNKNOWN"), equalTo(false));
  }

  @Test
  public void checkEventTriggeringNotAllowedByNullEventContextFilter() {
    when(eventContextFilters.stream()).thenReturn(Stream.of(eventContextFilter));

    assertThat(synchronizationEvent.isEventTriggeringAllowed(null), equalTo(false));
  }

  @Test
  public void checkErrorWhenMultipleEventContextFiltersWithSameNameAreFound() {
    when(eventContextFilters.stream())
        .thenReturn(Stream.of(eventContextFilter, eventContextFilter));

    OBException exception = assertThrows(OBException.class,
        () -> synchronizationEvent.isEventTriggeringAllowed(CONTEXT_FILTER_NAME));
    assertThat(exception.getMessage(),
        equalTo("Found multiple event context filters with name " + CONTEXT_FILTER_NAME));
  }
}
