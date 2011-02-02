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
 * All portions are Copyright (C) 2010-2011 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
isc.ClassFactory.defineClass('OBViewGrid', isc.OBGrid);

isc.OBViewGrid.addClassProperties({
  EDIT_LINK_FIELD_NAME: '_editLink',
  NO_COUNT_PARAMETER: '_noCount', // prevent the count operation on the server
  // note following 2 values should be the same
  // ListGrid._$ArrowUp and ListGrid._$ArrowDown
  ARROW_UP_KEY_NAME: 'Arrow_Up',
  ARROW_DOWN_KEY_NAME: 'Arrow_Down'
});

// = OBViewGrid =
// The OBViewGrid is the Openbravo specific subclass of the Smartclient
// ListGrid.
isc.OBViewGrid.addProperties({

  // ** {{{ view }}} **
  // The view member contains the pointer to the composite canvas which
  // handles this form
  // and the grid and other related components.
  view: null,
  
  // ** {{{ foreignKeyFieldNames }}} **
  // The list of fields which are foreign keys, these require custom
  // filtering.
  foreignKeyFieldNames: [],
  
  // ** {{{ editGrid }}} **
  // Controls if an edit link column is created in the grid, set to false to
  // prevent this.
  editGrid: true,
  
  // ** {{{ editLinkFieldProperties }}} **
  // The properties of the ListGridField created for the edit links.
  editLinkFieldProperties: {
    type: 'text',
    canSort: false,
    frozen: true,
    canFreeze: true,
    canEdit: false,
    canGroupBy: false,
    canHide: false,
    showTitle: true,
    title: '&nbsp;',
    autoFitWidth: true,
    canDragResize: false,
    canFilter: true,
    autoExpand: false,
    filterEditorType: 'StaticTextItem',
    filterEditorProperties: {
      textAlign: 'center'
    },
    name: isc.OBViewGrid.EDIT_LINK_FIELD_NAME
  },
  
  // ** {{{ dataPageSize }}} **
  // The data page size used for loading paged data from the server.
  dataPageSize: 100,
  
  autoFitFieldWidths: true,
  autoFitWidthApproach: 'title',
  canAutoFitFields: false,
  width: '100%',
  height: '100%',
  
  autoFetchTextMatchStyle: 'substring',
  showFilterEditor: true,
  canEdit: true,
  alternateRecordStyles: true,
  canReorderFields: true,
  canFreezeFields: true,
  canAddFormulaFields: true,
  canAddSummaryFields: true,
  canGroupBy: false,
  selectionAppearance: 'checkbox',
  useAllDataSourceFields: false,
  editEvent: 'none',
  showCellContextMenus: true,
  canOpenRecordEditor: true,
  showDetailFields: true,
  showErrorIcons: false,
  
  // internal sc grid property, see the ListGrid source code
  preserveEditsOnSetData: false,
  
  // enabling this results in a slower user interaction
  // it is better to allow fast grid interaction and if an error occurs
  // dismiss any new records being edited and go back to the edit row
  // which causes the error
  waitForSave: false,
  stopOnErrors: false,
  confirmDiscardEdits: true,
  
  canMultiSort: false,
  
  emptyMessage: OB.I18N.getLabel('OBUISC_ListGrid.loadingDataMessage'),
  discardEditsSaveButtonTitle: OB.I18N.getLabel('UINAVBA_Save'),
  
  quickDrawAheadRatio: 6.0,
  drawAheadRatio: 4.0,
  // note: don't set drawAllMaxCells too high as it results in extra reads
  // of data, Smartclient will try to read until drawAllMaxCells has been
  // reached
  drawAllMaxCells: 100,
  
  // keeps track if we are in objectSelectionMode or in toggleSelectionMode
  // objectSelectionMode = singleRecordSelection === true
  singleRecordSelection: false,
  
  // editing props
  rowEndEditAction: 'next',
  enforceVClipping: true,
  
  currentEditColumnLayout: null,
  
  dataProperties: {
    useClientFiltering: false,
    useClientSorting: false,
    
    transformData: function(newData, dsResponse){
      // correct the length if there is already data in the localData array
      if (this.localData) {
        for (var i = dsResponse.endRow + 1; i < this.localData.length; i++) {
          if (!Array.isLoading(this.localData[i]) && this.localData[i]) {
            dsResponse.totalRows = i + 1;
          } else {
            break;
          }
        }
      }
      if (this.localData && this.localData[dsResponse.totalRows]) {
        this.localData[dsResponse.totalRows] = null;
      }
    }
  },
  
  refreshFields: function(){
    this.setFields(this.completeFields.duplicate());
  },
  
  initWidget: function(){
    var thisGrid = this, localEditLinkField;
    if (this.editGrid) {
      // add the edit pencil in the beginning
      localEditLinkField = isc.addProperties({}, this.editLinkFieldProperties);
      localEditLinkField.width = this.editLinkColumnWidth;
      this.fields.unshift(localEditLinkField);
    }
    
    this.editFormDefaults = isc.addProperties({}, OB.ViewFormProperties, this.editFormDefaults);
    
    // added for showing counts in the filtereditor row
    this.checkboxFieldDefaults = isc.addProperties(this.checkboxFieldDefaults, {
      canFilter: true,
      // frozen is much nicer, but check out this forum discussion:
      // http://forums.smartclient.com/showthread.php?p=57581
      frozen: true,
      canFreeze: true,
      showHover: true,
      prompt: OB.I18N.getLabel('OBUIAPP_GridSelectAllColumnPrompt'),
      filterEditorProperties: {
        textAlign: 'center'
      },
      filterEditorType: 'StaticTextItem'
    });
    
    var ret = this.Super('initWidget', arguments);
    
    this.noDataEmptyMessage = OB.I18N.getLabel('OBUISC_ListGrid.loadingDataMessage'); // OB.I18N.getLabel('OBUIAPP_GridNoRecords')
    // + ' <span
    // onclick="window[\''
    // + this.ID +
    // '\'].createNew();"
    // class="OBLabelLink">'
    // +
    // OB.I18N.getLabel('OBUIAPP_GridCreateOne')+
    // '</span>';
    this.filterNoRecordsEmptyMessage = OB.I18N.getLabel('OBUIAPP_GridFilterNoResults') +
    ' <span onclick="window[\'' +
    this.ID +
    '\'].clearFilter();" class="OBLabelLink">' +
    OB.I18N.getLabel('OBUIAPP_GridClearFilter') +
    '</span>';
    return ret;
  },
  
  // overridden to support hover on the header for the checkbox field
  setFieldProperties: function(field, properties){
    var localField = field;
    if (isc.isA.Number(localField)) {
      localField = this.fields[localField];
    }
    if (this.isCheckboxField(localField) && properties) {
      properties.showHover = true;
      properties.prompt = OB.I18N.getLabel('OBUIAPP_GridSelectAllColumnPrompt');
    }
    
    return this.Super('setFieldProperties', arguments);
  },
  
  cellHoverHTML: function(record, rowNum, colNum){
    var field = this.getField(colNum), cellErrors, msg = '', i;
    if (this.isCheckboxField(field)) {
      return OB.I18N.getLabel('OBUIAPP_GridSelectColumnPrompt');
    }
    if (this.cellHasErrors(rowNum, colNum)) {
      cellErrors = this.getCellErrors(rowNum, colNum);
      // note cellErrors can be a string or array
      // accidentally both have the length property
      if (cellErrors && cellErrors.length > 0) {
        return OB.Utilities.getPromptString(cellErrors);
      }
    }
    return this.Super('cellHoverHTML', arguments);
  },
  
  setView: function(view){
    this.view = view;
    this.editFormDefaults.view = view;
    if(this.view.standardWindow.viewState && this.view.standardWindow.viewState[this.view.tabId]){
       this.setViewState(this.view.standardWindow.viewState[this.view.tabId]);	
    }
  },
  
  show: function(){
    var ret = this.Super('show', arguments);
    
    this.view.toolBar.updateButtonState();
    
    this.resetEmptyMessage();
    
    return ret;
  },
  
  headerClick: function(fieldNum, header){
    if (this.view.autoSaveForm) {
      this.setActionAfterAutoSave(this, this.headerClick, arguments);
      return;
    }
    var field = this.fields[fieldNum];
    if (this.isCheckboxField(field) && this.singleRecordSelection) {
      this.deselectAllRecords();
      this.singleRecordSelection = false;
    }
    return this.Super('headerClick', arguments);
  },
  
  deselectAllRecords: function(preventUpdateSelectInfo){
    if (this.view.autoSaveForm) {
      this.setActionAfterAutoSave(this, this.deselectAllRecords, arguments);
      return;
    }
    this.allSelected = false;
    var ret = this.Super('deselectAllRecords', arguments);
    if (!preventUpdateSelectInfo) {
      this.selectionUpdated();
    }
    return ret;
  },
  
  selectAllRecords: function(){
    if (this.view.autoSaveForm) {
      this.setActionAfterAutoSave(this, this.selectAllRecords, arguments);
      return;
    }
    this.allSelected = true;
    var ret = this.Super('selectAllRecords', arguments);
    this.selectionUpdated();
    return ret;
  },
  
  updateRowCountDisplay: function(){
    var newValue = '';
    if (this.data.getLength() > this.dataPageSize) {
      newValue = '>' + this.dataPageSize;
    } else if (this.data.getLength() === 0) {
      newValue = '&nbsp;';
    } else {
      newValue = '' + this.data.getLength();
    }
    if (this.filterEditor && this.filterEditor.getEditForm()) {
      this.filterEditor.getEditForm().setValue(isc.OBViewGrid.EDIT_LINK_FIELD_NAME, newValue);
    }
  },
  
  refreshContents: function(callback){
    this.resetEmptyMessage();
    this.view.updateTabTitle();
    
    // do not refresh if the parent is not selected and we have no data
    // anyway
    if (this.view.parentProperty && (!this.data || !this.data.getLength || this.data.getLength() === 0)) {
      selectedValues = this.view.parentView.viewGrid.getSelectedRecords();
      if (selectedValues.length === 0) {
        if (callback) {
          callback();
        }
        return;
      }
    }
    
    var context = {
      showPrompt: false,
      textMatchStyle: this.autoFetchTextMatchStyle
    };
    this.filterData(this.getCriteria(), callback, context);
  },
  
  // the dataarrived method is where different actions are done after
  // data has arrived in the grid:
  // - open the edit view if default edit mode is enabled
  // - if the user goes directly to a tab (from a link in another window)
  // then
  // opening the relevant record is done here or if no record is passed grid
  // mode is opened
  // - if there is only one record then select it directly
  dataArrived: function(startRow, endRow){
    // do this now, to replace the loading message
    if (this.view.readOnly) {
      this.noDataEmptyMessage = OB.I18N.getLabel('OBUIAPP_NoDataInGrid');
    } else {
      this.noDataEmptyMessage = OB.I18N.getLabel('OBUIAPP_GridNoRecords') +
      ' <span onclick="window[\'' +
      this.ID +
      '\'].createNew();" class="OBLabelLink">' +
      OB.I18N.getLabel('OBUIAPP_GridCreateOne') +
      '</span>';
    }
    this.resetEmptyMessage();
    
    var record, ret = this.Super('dataArrived', arguments);
    this.updateRowCountDisplay();
    if (this.getSelectedRecords() && this.getSelectedRecords().length > 0) {
      this.selectionUpdated();
    }
    
    if (this.targetOpenNewEdit) {
      delete this.targetOpenNewEdit;
      // not passing record opens new
      this.view.editRecord();
    } else if (this.targetOpenGrid) {
      // direct link from other window but without a record id
      // so just show grid mode
      // don't need to do anything here
      delete this.targetOpenGrid;
    } else if (this.targetRecordId) {
      // direct link from other tab to a specific record
      this.delayedHandleTargetRecord(startRow, endRow);
    } else if (this.view.shouldOpenDefaultEditMode()) {
      // ui-pattern: single record/edit mode
      this.view.openDefaultEditView(this.getRecord(startRow));
    } else if (this.data && this.data.getLength() === 1) {
      // one record select it directly
      record = this.getRecord(0);
      // this select method prevents state changing if the record
      // was already selected
      this.doSelectSingleRecord(record);
    }
    
    return ret;
  },
  
  // with a delay to handle the target record when the body has been drawn
  delayedHandleTargetRecord: function(startRow, endRow){
    var rowTop, recordIndex, i, data = this.data, tmpTargetRecordId = this.targetRecordId;
    if (!this.targetRecordId) {
      return;
    }
    if (this.body) {
      // don't need it anymore
      delete this.targetRecordId;
      var gridRecord = data.find(OB.Constants.ID, tmpTargetRecordId);
      
      // no grid record found, stop here
      if (!gridRecord) {
        return;
      }
      recordIndex = this.getRecordIndex(gridRecord);
      
      if (data.criteria) {
        data.criteria._targetRecordId = null;
      }
      
      // remove the isloading status from rows
      for (i = 0; i < startRow; i++) {
        if (Array.isLoading(data.localData[i])) {
          data.localData[i] = null;
        }
      }
      
      this.doSelectSingleRecord(gridRecord);
      this.scrollRecordIntoView(recordIndex, true);
      
      // go to the children, if needed
      if (this.view.standardWindow.directTabInfo) {
        this.view.openDirectChildTab();
      }
    } else {
      // wait a bit longer til the body is drawn
      this.delayCall('delayedHandleTargetRecord', [startRow, endRow], 200, this);
    }
  },
  
  // Prevents empty message to be shown in frozen part
  // http://forums.smartclient.com/showthread.php?p=57581
  createBodies: function(){
    var ret = this.Super('createBodies', arguments);
    if (this.frozenBody) {
      this.frozenBody.showEmptyMessage = false;
    }
    return ret;
  },
  
  selectRecordById: function(id, forceFetch){
    if (forceFetch) {
      this.targetRecordId = id;
      this.filterData(this.getCriteria());
      return;
    }
    
    var recordIndex, gridRecord = this.data.find(OB.Constants.ID, id);
    // no grid record fetch it
    if (!gridRecord) {
      this.targetRecordId = id;
      this.filterData(this.getCriteria());
      return;
    }
    recordIndex = this.getRecordIndex(gridRecord);
    this.scrollRecordIntoView(recordIndex, true);
    this.doSelectSingleRecord(gridRecord);
  },
  
  // overridden to prevent extra firing of selection updated event
  selectSingleRecord: function(record){
    this.deselectAllRecords(true);
    this.selectRecord(record);
  },
  
  // overridden to prevent extra firing of selection updated event
  // selectrecords will fire it once
  selectRecord: function(record, state, colNum){
    this.selectRecords(record, state, colNum);
  },
  
  filterData: function(criteria, callback, requestProperties){
    if (!requestProperties) {
      requestProperties = {};
    }
    requestProperties.showPrompt = false;
    
    var theView = this.view;
    var newCallBack = function(){
      theView.recordSelected();
      if (callback) {
        callback();
      }
    };
    
    return this.Super('filterData', [this.convertCriteria(criteria), newCallBack, requestProperties]);
  },
  
  fetchData: function(criteria, callback, requestProperties){
    if (!requestProperties) {
      requestProperties = {};
    }
    requestProperties.showPrompt = false;
    
    var theView = this.view;
    
    var newCallBack = function(){
      theView.recordSelected();
      if (callback) {
        callback();
      }
    };
    
    return this.Super('fetchData', [this.convertCriteria(criteria), newCallBack, requestProperties]);
  },
  
  getInitialCriteria: function(){
    var criteria = this.Super('getInitialCriteria', arguments);
    
    return this.convertCriteria(criteria);
  },
  
  getCriteria: function(){
    var criteria = this.Super('getCriteria', arguments) || {};
    criteria = this.convertCriteria(criteria);
    return criteria;
  },
  
  convertCriteria: function(criteria){
    var selectedValues;
    
    criteria = criteria || {};
    
    if (this.targetRecordId) {
      // do not filter on anything with a targetrecord
      criteria = {};
      // remove the filter clause we don't want to use
      this.filterClause = null;
      criteria._targetRecordId = this.targetRecordId;
    }
    
    // note pass in criteria otherwise infinite looping!
    this.resetEmptyMessage(criteria);
    
    if (this.view.parentProperty) {
      selectedValues = this.view.parentView.viewGrid.getSelectedRecords();
      if (selectedValues.length === 0) {
        criteria[this.view.parentProperty] = '-1';
      } else if (selectedValues.length > 1) {
        criteria[this.view.parentProperty] = '-1';
      } else {
        criteria[this.view.parentProperty] = selectedValues[0][OB.Constants.ID];
      }
    }
    
    // prevent the count operation
    criteria[isc.OBViewGrid.NO_COUNT_PARAMETER] = 'true';
    
    if (this.orderByClause) {
      criteria[OB.Constants.ORDERBY_PARAMETER] = this.orderByClause;
    }
    
    if (this.filterClause) {
      if (this.whereClause) {
        criteria[OB.Constants.WHERE_PARAMETER] = ' ((' + this.whereClause + ') and (' + this.filterClause + ")) ";
      } else {
        criteria[OB.Constants.WHERE_PARAMETER] = this.filterClause;
      }
      this.checkShowFilterFunnelIcon(criteria);
    } else if (this.whereClause) {
      criteria[OB.Constants.WHERE_PARAMETER] = this.whereClause;
      this.checkShowFilterFunnelIcon(criteria);
    } else {
      criteria[OB.Constants.WHERE_PARAMETER] = null;
      this.checkShowFilterFunnelIcon(criteria);
    }
    
    // add all the new session properties context info to the criteria
    isc.addProperties(criteria, this.view.getContextInfo(true, false));
    
    return criteria;
  },
  
  createNew: function(){
    this.view.editRecord();
  },
  
  clearFilter: function(){
    this.filterEditor.getEditForm().clearValues();
    this.filterEditor.performAction();
  },
  
  // determine which field can be autoexpanded to use extra space
  getAutoFitExpandField: function(){
    for (var i = 0; i < this.fields.length; i++) {
      var field = this.fields[i];
      if (field.autoExpand) {
        return field;
      }
    }
    return this.Super('getAutoFitExpandField', arguments);
  },
  
  recordClick: function(viewer, record, recordNum, field, fieldNum, value, rawValue){
    if (this.view.autoSaveForm) {
      this.setActionAfterAutoSave(this, this.recordClick, arguments);
    } else {
      this.handleRecordSelection(viewer, record, recordNum, field, fieldNum, value, rawValue, false);
    }
  },
  
  recordDoubleClick: function(viewer, record, recordNum, field, fieldNum, value, rawValue){
    if (this.view.autoSaveForm) {
      this.setActionAfterAutoSave(this, this.recordDoubleClick, arguments);
    } else {
      this.view.editRecord(record);
    }
  },
  
  setActionAfterAutoSave: function(target, method, parameters){
    this.view.autoSaveForm.setActionAfterAutoSave({
      target: target,
      method: method,
      parameters: parameters
    });
  },
  
  resetEmptyMessage: function(criteria){
    criteria = criteria || this.getCriteria();
    if (!this.view) {
      this.emptyMessage = this.noDataEmptyMessage;
    } else if (this.isGridFiltered(criteria)) {
      this.emptyMessage = this.filterNoRecordsEmptyMessage;
    } else if (this.view.isRootView) {
      this.emptyMessage = this.noDataEmptyMessage;
    } else {
      selectedValues = this.view.parentView.viewGrid.getSelectedRecords();
      if (selectedValues.length === 0) {
        this.emptyMessage = OB.I18N.getLabel('OBUIAPP_NoParentSelected');
      } else if (selectedValues.length > 1) {
        this.emptyMessage = OB.I18N.getLabel('OBUIAPP_MultipleParentsSelected');
      } else {
        this.emptyMessage = this.noDataEmptyMessage;
      }
    }
  },
  
  // +++++++++++++++++++++++++++++ Context menu on record click +++++++++++++++++++++++
  
  cellContextClick: function(record, rowNum, colNum){
    //this.handleRecordSelection(null, record, rowNum, null, colNum, null, null, true);
    this.view.setAsActiveView();
    var ret = this.Super('cellContextClick', arguments);
    return ret;
  },
  
  makeCellContextItems: function(record, rowNum, colNum){
    var sourceWindow = this.view.standardWindow.windowId;
    var menuItems = [];
    var field = this.getField(colNum);
    var grid = this;
    menuItems.add({
      title: OB.I18N.getLabel('OBUIAPP_CreateNewRecord'),
      click: function(){
        isc.say('Not implemented yet');
      }
    });
    if (this.canEdit && this.isWritable(record)) {
      menuItems.add({
        title: OB.I18N.getLabel('OBUIAPP_EditInGrid'),
        click: function(){
          grid.endEditing();
          grid.startEditing(rowNum, colNum);
        }
      });
    }
    if (field.canFilter) {
      menuItems.add({
        title: OB.I18N.getLabel('OBUIAPP_UseAsFilter'),
        click: function(){
          var value;
          var filterCriteria = grid.getCriteria();
          // a foreign key field, use the displayfield/identifier
          if (field.foreignKeyField && field.displayField) {
            value = record[field.displayField];
            filterCriteria[field.displayField] = value;
          } else {
            value = grid.getEditDisplayValue(rowNum, colNum, record);
            filterCriteria[field.name] = value;
          }
          grid.setCriteria(filterCriteria);
          grid.checkShowFilterFunnelIcon(grid.getCriteria());
          grid.filterData(grid.getCriteria());
        }
      });
    }
    if (field.foreignKeyField) {
      menuItems.add({
        title: OB.I18N.getLabel('OBUIAPP_OpenOnTab'),
        click: function(){
          var fldName = field.name;
          var dotIndex = fldName.indexOf('.');
          if (dotIndex !== -1) {
            fldName = fldName.substring(0, dotIndex);
          }
          OB.Utilities.openDirectView(sourceWindow, field.referencedKeyColumnName, field.targetEntity, record[fldName]);
        }
      });
    }
    
    return menuItems;
  },
  
  // +++++++++++++++++++++++++++++ Record Selection Handling +++++++++++++++++++++++
  
  updateSelectedCountDisplay: function(){
    var selection = this.getSelection();
    var selectionLength = selection.getLength();
    var newValue = '&nbsp;';
    if (selectionLength > 0) {
      newValue = selectionLength + '';
    }
    if (this.filterEditor) {
      this.filterEditor.getEditForm().setValue(this.getCheckboxField().name, newValue);
    }
  },
  
  // note when solving selection issues in the future also
  // consider using the selectionChanged method, but that
  // one has as disadvantage that it is called multiple times
  // for one select/deselect action
  selectionUpdated: function(record, recordList){
    this.stopHover();
    this.updateSelectedCountDisplay();
    this.view.recordSelected();
  },
  
  selectOnMouseDown: function(record, recordNum, fieldNum){
    // don't change selection on right mouse down
    var EH = isc.EventHandler, eventType;
    
    if (this.view.autoSaveForm) {
      // only call this method in case a checkbox click was done
      // in all other cases the recordClick will be called later
      // anyway
      if (this.getCheckboxFieldPosition() === fieldNum) {
        this.setActionAfterAutoSave(this, this.selectOnMouseDown, arguments);
      }
      return;
    }
    
    var previousSingleRecordSelection = this.singleRecordSelection;
    var currentSelectedRecordSelected = (this.getSelectedRecord() === record);
    if (this.getCheckboxFieldPosition() === fieldNum) {
      if (this.singleRecordSelection) {
        this.deselectAllRecords(true);
      }
      this.singleRecordSelection = false;
      this.Super('selectOnMouseDown', arguments);
      
      // handle a special case:
      // - singlerecordmode: checkbox is not checked
      // - user clicks on checkbox
      // in this case move to multi select mode and keep the record selected
      if (previousSingleRecordSelection && currentSelectedRecordSelected) {
        this.selectSingleRecord(record);
      }
      
      this.selectionUpdated();
      
      this.markForRedraw('Selection checkboxes need to be redrawn');
    } else {
      // do some checking, the handleRecordSelection should only be called
      // in case of keyboard navigation and not for real mouse clicks,
      // these are handled by the recordClick and recordDoubleClick methods
      // if this method here would also handle mouseclicks then the
      // doubleClick
      // event is not captured anymore
      eventType = EH.getEventType();
      if (!EH.isMouseEvent(eventType)) {
        this.handleRecordSelection(null, record, recordNum, null, fieldNum, null, null, true);
      }
    }
  },
  
  handleRecordSelection: function(viewer, record, recordNum, field, fieldNum, value, rawValue, fromSelectOnMouseDown){
    var EH = isc.EventHandler;
    var keyName = EH.getKey();
    
    // stop editing if the user clicks out of the row
    if (this.getEditRow() && this.getEditRow() !== recordNum) {
      this.endEditing();
    }
    // do nothing, click in the editrow itself
    if (this.getEditRow() && this.getEditRow() === recordNum) {
      return;
    }
    
    // if the arrow key was pressed and no ctrl/shift pressed then
    // go to single select mode
    var arrowKeyPressed = keyName && (keyName === isc.OBViewGrid.ARROW_UP_KEY_NAME || keyName === isc.OBViewGrid.ARROW_DOWN_KEY_NAME);
    
    var previousSingleRecordSelection = this.singleRecordSelection;
    if (arrowKeyPressed) {
      if (EH.ctrlKeyDown() || EH.shiftKeyDown()) {
        // move to multi-select mode, let the standard do it for us
        this.singleRecordSelection = false;
      } else {
        this.doSelectSingleRecord(record);
      }
    } else if (this.getCheckboxFieldPosition() === fieldNum) {
      if (this.singleRecordSelection) {
        this.deselectAllRecords(true);
      }
      // click in checkbox field is done by standard logic
      // in the selectOnMouseDown
      this.singleRecordSelection = false;
      this.selectionUpdated();
    } else if (isc.EventHandler.ctrlKeyDown()) {
      // only do something if record clicked and not from selectOnMouseDown
      // this method got called twice from one clicK: through recordClick
      // and
      // to selectOnMouseDown. Only handle one.
      if (!fromSelectOnMouseDown) {
        this.singleRecordSelection = false;
        // let ctrl-click also deselect records
        if (this.isSelected(record)) {
          this.deselectRecord(record);
        } else {
          this.selectRecord(record);
        }
      }
    } else if (isc.EventHandler.shiftKeyDown()) {
      this.singleRecordSelection = false;
      this.selection.selectOnMouseDown(this, recordNum, fieldNum);
    } else {
      // click on the record which was already selected
      this.doSelectSingleRecord(record);
    }
    
    this.updateSelectedCountDisplay();
    
    // mark some redraws if there are lines which don't
    // have a checkbox flagged, so if we move from single record selection
    // to multi record selection
    if (!this.singleRecordSelection && previousSingleRecordSelection) {
      this.markForRedraw('Selection checkboxes need to be redrawn');
    }
  },
  
  selectRecordForEdit: function(record){
    this.Super('selectRecordForEdit', arguments);
    this.doSelectSingleRecord(record);
  },
  
  doSelectSingleRecord: function(record){
    // if this record is already selected and the only one then do nothing
    // note that when navigating with the arrow key that at a certain 2 are
    // selected
    // when going into this method therefore the extra check on length === 1
    if (this.singleRecordSelection && this.isSelected(record) && this.getSelection().length === 1) {
      return;
    }
    this.singleRecordSelection = true;
    this.selectSingleRecord(record);
    
    // deselect the checkbox in the top
    var fieldNum = this.getCheckboxFieldPosition(), field = this.fields[fieldNum];
    var icon = this.checkboxFieldFalseImage || this.booleanFalseImage;
    var title = this.getValueIconHTML(icon, field);
    
    this.setFieldTitle(fieldNum, title);
  },
  
  // overridden to prevent the checkbox to be shown when only one
  // record is selected.
  getCellValue: function(record, recordNum, fieldNum, gridBody){
    var field = this.fields[fieldNum];
    if (!field || this.allSelected) {
      return this.Super('getCellValue', arguments);
    }
    // do all the cases which are handled in the super directly
    if (this.isCheckboxField(field)) {
      // NOTE: code copied from super class
      var icon;
      if (!this.body.canSelectRecord(record)) {
        // record cannot be selected but we want the space allocated for the
        // checkbox anyway.
        icon = '[SKINIMG]/blank.gif';
      } else if (this.singleRecordSelection && !this.allSelected) {
        // always show the false image
        icon = (this.checkboxFieldFalseImage || this.booleanFalseImage);
      } else {
        // checked if selected, otherwise unchecked
        var isSel = this.selection.isSelected(record) ? true : false;
        icon = isSel ? (this.checkboxFieldTrueImage || this.booleanTrueImage) : (this.checkboxFieldFalseImage || this.booleanFalseImage);
      }
      // if the record is disabled, make the checkbox image disabled as well
      // or if the record is new then also show disabled
      if (!record || record[this.recordEnabledProperty] === false) {
        icon = icon.replace('.', '_Disabled.');
      }
      
      var html = this.getValueIconHTML(icon, field);
      
      return html;
    } else {
      return this.Super('getCellValue', arguments);
    }
  },
  
  getSelectedRecords: function(){
    return this.getSelection();
  },
  
  // +++++++++++++++++ functions for the editing +++++++++++++++++
  
  saveEditedValues: function(rowNum, colNum, newValues, oldValues, editValuesID, editCompletionEvent, saveCallback){
    this.setEditValues(this.getEditRow(), this.getEditForm().getValues(), true);
    return this.Super('saveEditedValues', arguments);
  },
  
  editFailed: function(rowNum, colNum, newValues, oldValues, editCompletionEvent, dsResponse, dsRequest){
    var view = this.view;
    if (dsResponse) {
      view.setErrorMessageFromResponse(dsResponse, dsResponse.data, dsRequest);
    }
    if (!view.isVisible()) {
      isc.warn(OB.I18N.getLabel('OBUIAPP_AutoSaveError', [view.tabTitle]));
    }
    this.view.updateTabTitle();
    this.view.toolBar.updateButtonState();
  },
  
  editComplete: function(rowNum, colNum, newValues, oldValues, editCompletionEvent, dsResponse){
    var record = this.getRecord(rowNum);
    
    // we got here, so we must writable, make sure
    // that we stay that way
    record._writable = true;
    
    // during save the record looses the link to the editColumnLayout,
    // restore it
    if (oldValues.editColumnLayout && !record.editColumnLayout) {
      record.editColumnLayout = oldValues.editColumnLayout;
    }
    if (record.editColumnLayout) {
      record.editColumnLayout.showEditOpen();
    }
    this.view.updateTabTitle();
    this.view.toolBar.updateButtonState();
    
    return this.Super('editComplete', arguments);
  },
  
  discardEdits: function(rowNum, colNum, dontHideEditor, editCompletionEvent){
    var localArguments = arguments;
    var me = this;
    if (this.getEditForm().valuesHaveChanged()) {
      isc.ask(OB.I18N.getLabel('OBUIAPP_ConfirmCancelEdit'), function(value){
        if (value) {
          me.view.updateTabTitle();
          me.view.toolBar.updateButtonState();
          me.Super('discardEdits', localArguments);
        }
      });
    } else {
      this.view.updateTabTitle();
      this.view.toolBar.updateButtonState();
      me.Super('discardEdits', localArguments);
    }
  },
  
  rowEditorEnter: function(record, editValues, rowNum){
    this.view.isEditingGrid = true;
    if (this.baseStyleEdit) {
      this.baseStyleView = this.baseStyle;
      this.baseStyle = this.baseStyleEdit;
    }
    
    // also called in case of new
    var form = this.getEditForm();
    form.doEditRecordActions(false, false);
    
    if (record && record.editColumnLayout) {
      record.editColumnLayout.showSaveCancel();
    }
  },
  
  rowEditorExit: function(editCompletionEvent, record, newValues, rowNum){
    isc.Log.logDebug('hideInlineEditor ' + this.getEditRow(), 'OB');
    if (this.baseStyleView) {
      this.baseStyle = this.baseStyleView;
    }
    if (record && record.editColumnLayout) {
      isc.Log.logDebug('hideInlineEditor has record and editColumnLayout', 'OB');
      record.editColumnLayout.showEditOpen();
    } else if (this.currentEditColumnLayout) {
      this.currentEditColumnLayout.showEditOpen();
    } else {
      isc.Log.logDebug('hideInlineEditor has NO record and editColumnLayout', 'OB');
    }
    this.view.isEditingGrid = false;
  },
  
  // we are being reshown, get new values for the combos
  visibilityChanged: function(visible){
    if (visible && this.getEditRow()) {
      this.getEditForm().doChangeFICCall();
    }
  },
  
  isWritable: function(record){
    return record._writable;
  },
  
  // +++++++++++++++++ functions for the edit-link column +++++++++++++++++
  
  createRecordComponent: function(record, colNum){
    var layout = this.Super('createRecordComponent', arguments), rowNum;
    if (layout) {
      return layout;
    }
    if (this.isEditLinkColumn(colNum)) {
      rowNum = this.getRecordIndex(record);
      layout = isc.OBGridButtonsComponent.create({
        record: record,
        grid: this,
        rowNum: rowNum
      });
      if (!this.isWritable(record)) {
        layout.editButton.doNotShow = true;
        layout.editButton.hide();
        layout.buttonSeparator1.hide();
      } else {
        layout.editButton.doNotShow = this.view.readOnly;
        if (layout.editButton.doNotShow) {
          layout.editButton.hide();
          layout.buttonSeparator1.hide();
        } else {
          layout.editButton.show();
          layout.buttonSeparator1.show();
        }
      }
      
      record.editColumnLayout = layout;
    }
    return layout;
  },
  
  updateRecordComponent: function(record, colNum, component, recordChanged){
    var superComponent = this.Super('updateRecordComponent', arguments);
    if (superComponent) {
      return superComponent;
    }
    if (this.isEditLinkColumn(colNum)) {
      // clear the previous record pointer
      if (recordChanged && component.record.editColumnLayout === component) {
        component.record.editColumnLayout = null;
      }
      component.record = record;
      record.editColumnLayout = component;
      component.rowNum = this.getRecordIndex(record);
      if (!this.isWritable(record)) {
        component.editButton.setDisabled(true);
      } else {
        component.editButton.setDisabled(this.view.readOnly);
      }
      return component;
    }
    return null;
  },
  
  isEditLinkColumn: function(colNum){
    var fieldName = this.getFieldName(colNum);
    return (fieldName === isc.OBViewGrid.EDIT_LINK_FIELD_NAME);
  },
  
  reorderField: function(fieldNum, moveToPosition){
	var res = this.Super('reorderField', arguments);
	this.view.standardWindow.storeViewState();
	return res;
  },
  
  hideField: function(field, suppressRelayout){
	var res =  this.Super('hideField', arguments);
	this.view.standardWindow.storeViewState();
	return res;
  },
  
  showField: function(field, suppressRelayout){
	var res =  this.Super('showField', arguments);
	this.view.standardWindow.storeViewState();
	return res;
  },
  
  resizeField: function(fieldNum, newWidth, storeWidth){
	this.view.standardWindow.storeViewState();
	return this.Super('resizeField', arguments);
  }
  
});

// = OBGridToolStrip =
// The component which is inside of OBGridButtonsComponent
isc.ClassFactory.defineClass('OBGridToolStrip', isc.ToolStrip);

isc.OBGridToolStrip.addProperties({});

// = OBGridToolStripIcon =
// The icons which are inside of OBGridToolStrip
isc.ClassFactory.defineClass('OBGridToolStripIcon', isc.ImgButton);

isc.OBGridToolStripIcon.addProperties({
  buttonType: null, /* This could be: edit - form - cancel - save */
  initWidget: function(){
    if (this.initWidgetStyle) {
      this.initWidgetStyle();
    }
    this.Super('initWidget', arguments);
  }
});

// = OBGridToolStripSeparator =
// The separator between icons of OBGridToolStrip
isc.ClassFactory.defineClass('OBGridToolStripSeparator', isc.Img);

isc.OBGridToolStripSeparator.addProperties({});

// = OBGridButtonsComponent =
// The component which is used to create the contents of the
// edit open column in the grid
isc.ClassFactory.defineClass('OBGridButtonsComponent', isc.HLayout);

isc.OBGridButtonsComponent.addProperties({
  OBGridToolStrip: null,
  saveCancelLayout: null,
  
  // the grid to which this component belongs
  grid: null,
  
  rowNum: null,
  
  // the record to which this component belongs
  record: null,
  
  initWidget: function(){
    var me = this, formButton, cancelButton, saveButton;
    
    this.progressIcon = isc.Img.create(this.grid.progressIconDefaults);
    
    this.editButton = isc.OBGridToolStripIcon.create({
      buttonType: 'edit',
      prompt: OB.I18N.getLabel('OBUIAPP_GridEditButtonPrompt'),
      action: function(){
        if (me.grid.view.autoSaveForm) {
          me.grid.setActionAfterAutoSave(me, me.doEdit, []);
        } else {
          me.doEdit();
        }
      },
      doNotShow: me.grid.view.readOnly,
      
      show: function(){
        if (this.doNotShow) {
          return;
        }
        return this.Super('show', arguments);
      }
    });
    
    formButton = isc.OBGridToolStripIcon.create({
      buttonType: 'form',
      prompt: OB.I18N.getLabel('OBUIAPP_GridFormButtonPrompt'),
      action: function(){
        if (me.grid.view.autoSaveForm) {
          me.grid.setActionAfterAutoSave(me, me.doOpen, []);
        } else {
          me.doOpen();
        }
      }
    });
    
    cancelButton = isc.OBGridToolStripIcon.create({
      buttonType: 'cancel',
      prompt: OB.I18N.getLabel('OBUIAPP_GridCancelButtonPrompt'),
      action: function(){
        me.doCancel();
      }
    });
    
    saveButton = isc.OBGridToolStripIcon.create({
      buttonType: 'save',
      prompt: OB.I18N.getLabel('OBUIAPP_GridSaveButtonPrompt'),
      action: function(){
        me.doSave();
      }
    });
    
    this.buttonSeparator1 = isc.OBGridToolStripSeparator.create({});
    
    if (me.grid.view.readOnly) {
      this.buttonSeparator1.visibility = 'hidden';
    }
    
    buttonSeparator2 = isc.OBGridToolStripSeparator.create({});
    
    this.OBGridToolStrip = isc.OBGridToolStrip.create({
      members: [formButton, this.buttonSeparator1, this.editButton, cancelButton, buttonSeparator2, saveButton]
    });
    
    this.addMember(this.progressIcon);
    this.addMember(this.OBGridToolStrip);
    this.OBGridToolStrip.hideMember(5);
    this.OBGridToolStrip.hideMember(4);
    this.OBGridToolStrip.hideMember(3);
  },
  
  toggleProgressIcon: function(toggle){
    if (toggle) {
      this.progressIcon.show();
      this.OBGridToolStrip.hide();
    } else {
      this.progressIcon.hide();
      this.OBGridToolStrip.show();
    }
  },
  
  showEditOpen: function(){
    this.OBGridToolStrip.hideMember(5);
    this.OBGridToolStrip.hideMember(4);
    this.OBGridToolStrip.hideMember(3);
    this.OBGridToolStrip.showMember(2);
    this.OBGridToolStrip.showMember(1);
    this.OBGridToolStrip.showMember(0);
    this.grid.currentEditColumnLayout = null;
  },
  
  showSaveCancel: function(){
    this.OBGridToolStrip.hideMember(2);
    this.OBGridToolStrip.hideMember(1);
    this.OBGridToolStrip.hideMember(0);
    this.OBGridToolStrip.showMember(3);
    this.OBGridToolStrip.showMember(4);
    this.OBGridToolStrip.showMember(5);
    this.grid.currentEditColumnLayout = this;
  },
  
  doEdit: function(){
    this.showSaveCancel();
    this.grid.selectSingleRecord(this.record);
    this.grid.startEditing(this.rowNum);
  },
  
  doOpen: function(){
    this.grid.endEditing();
    this.grid.view.editRecord(this.record);
  },
  
  doSave: function(){
    // note change back to editOpen is done in the editComplete event of the
    // grid itself
    this.grid.endEditing();
  },
  
  doCancel: function(){
    this.grid.cancelEditing();
  }
  
});
