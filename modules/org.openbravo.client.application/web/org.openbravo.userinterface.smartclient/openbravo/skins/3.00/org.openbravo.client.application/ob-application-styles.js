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
 * All portions are Copyright (C) 2010 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

/* =====================================================================
 * Styling properties for:
 * 1) Main layout
 * 2) Main components (navbar flyout, main grid, form)
 * 3) Changes to standard isc.Dialog buttons
 =======================================================================*/


/* =====================================================================
 * Main layout styling properties
 =======================================================================*/
// note main layout styling is done a bit differently 
// as this needs to be set when the layout gets created
OB.MainLayoutStylingProperties = {};

// The toolbar showing the navigation bar components
OB.MainLayoutStylingProperties.Toolbar = {
  overflow: 'visible',
  defaultLayoutAlign: 'center',
  styleName: 'OBNavBarToolStrip',
  width: 1,
  layoutLeftMargin: 2,
  separatorSize: 0,
  height: 28
};

// Styling of the main layout containing everything
OB.MainLayoutStylingProperties.TopLayout = {
  width: '100%',
  height: '1',
  overflow: 'visible',
  layoutTopMargin: 4
};

// Properties for the custom company image
OB.MainLayoutStylingProperties.CompanyImageLogo = {
  width: 122,
  height: 34,
  src: OB.Application.contextUrl + 'utility/ShowImageLogo?logo=yourcompanymenu'
};

/* =====================================================================
 * Main components styling properties
 =======================================================================*/
// The quick run widget is used for flyouts in the navigation bar
isc.OBQuickRun.addProperties({

  // ** {{{ baseStyle }}} **
  // The base style for the quick run launch button. All other styles are
  // derived
  // from this base style.
  baseStyle: 'OBNavBarButton'
});

isc.OBPopup.addProperties({
  width: 600,
  height: 500
});

isc.OBStandardWindow.addProperties({
  toolBarHeight: 40
});

// Styling properties for the application menu button present in the navbar
OB.ApplicationMenuButtonStylingProperties = {
  baseStyle: 'OBNavBarButton',
  showMenuButtonImage: false,
  align: 'center',
  height: 26,
  iconHeight: 6,
  iconWidth: 10,
  iconSpacing: 10,
  iconAlign: 'left',
  iconOrientation: 'right',
  icon: {
    src: '[SKINIMG]../../org.openbravo.client.application/images/navbar/ico-green-arrow-down.gif'
  }
};

// Styling properties for the application menu
OB.ApplicationMenuStylingProperties = {
  baseStyle: 'OBNavBarComponentMenuItemCell',
  styleName: 'OBNavBarComponentMenu',
  bodyStyleName: 'OBNavBarComponentMenuBody',
  submenuOffset: -6
};

// Styling properties for the help/about navigation bar component
isc.OBHelpAbout.addProperties({
  baseStyle: 'OBNavBarButton',
  iconHeight: 6,
  iconWidth: 10,
  iconSpacing: 10,
  icon: {
    src: '[SKINIMG]../../org.openbravo.client.application/images/navbar/ico-green-arrow-down.gif'
  },
  iconOrientation: 'right'
});

// Styling properties for the logout button in the navbar
OB.LogoutNavbarComponentStylingProperties = {
  baseStyle: 'OBNavBarButton',
  height: 14,
  width: 14,
  src: '[SKINIMG]../../org.openbravo.client.application/images/navbar/iconClose.png',
  showTitle: false,
  imageType: 'normal',
  layoutAlign: 'center',
  overflow: 'visible',
  showRollOver: false,
  showFocused: false,
  showDown: false
};

// Styling properties for the quick launch and quick create components
// See also isc.OBQuickRun styling properties
OB.QuickLaunchNavbarComponentStylingProperties = {
  // todo: it is nicer to move this to a style but then this issue occurs:
  // https://issues.openbravo.com/view.php?id=13786
  width: 37,
  
  layoutProperties: {
    width: 250,
    membersMargin: 10
  }
};

/* =====================================================================
 * Width of the active bar on the left in the main view
 =======================================================================*/
OB.ActiveBarStyling = {
  width: 6,
  activeStyleName: 'OBViewActive',
  inActiveStyleName: 'OBViewInActive'
};

/* =====================================================================
 * Changed styling of the standard dialogs
 =======================================================================*/
isc.Dialog.OK.buttonConstructor = isc.OBFormButton;
isc.Dialog.OK.baseStyle = 'OBFormButton';
isc.Dialog.OK.titleStyle = 'OBFormButtonTitle';
isc.Dialog.CANCEL.buttonConstructor = isc.OBFormButton;
isc.Dialog.CANCEL.baseStyle = 'OBFormButton';
isc.Dialog.CANCEL.titleStyle = 'OBFormButtonTitle';
isc.Dialog.YES.buttonConstructor = isc.OBFormButton;
isc.Dialog.YES.baseStyle = 'OBFormButton';
isc.Dialog.YES.titleStyle = 'OBFormButtonTitle';
isc.Dialog.NO.buttonConstructor = isc.OBFormButton;
isc.Dialog.NO.baseStyle = 'OBFormButton';
isc.Dialog.NO.titleStyle = 'OBFormButtonTitle';
isc.Dialog.APPLY.buttonConstructor = isc.OBFormButton;
isc.Dialog.APPLY.baseStyle = 'OBFormButton';
isc.Dialog.APPLY.titleStyle = 'OBFormButtonTitle';
isc.Dialog.DONE.buttonConstructor = isc.OBFormButton;
isc.Dialog.DONE.baseStyle = 'OBFormButton';
isc.Dialog.DONE.titleStyle = 'OBFormButtonTitle';

isc.ListGrid.addProperties({
  alternateRecordStyles: true
});

// override the standard show prompt to show a more custom Openbravo
// loading prompt
// note the loading image is set in the index.html
isc._orginal_showPrompt = isc.showPrompt;
isc.showPrompt = function(prompt){
  var width, height, top, left, props = {}, dialog = isc.Dialog.Prompt, modalTarget;
  if (OB.OBModalTarget) {
    props = {
      showEdges: false,
      showModalMask: true,
      isModal: true,
      hiliteBodyColor: null,
      bodyColor: null,
      bodyStyle: 'OBLoadingPromptBody'
    };
    props.isModal = true;
    modalTarget = OB.OBModalTarget;
    props.modalTarget = modalTarget;
    isc.Dialog.OBModalTarget = null;
    
    // find the top/left position, center in the modalTarget
    width = dialog.getVisibleWidth();
    height = dialog.getVisibleHeight();
    left = modalTarget.getPageLeft() + ((modalTarget.getWidth() - width) / 2) + modalTarget.getScrollLeft();
    top = modalTarget.getPageTop() + ((modalTarget.getHeight() - height) / 2) + modalTarget.getScrollTop();
    props.left = Math.round(left);
    props.top = Math.max(Math.round(top), 0);
    props.autoCenter = false;
  }
  
  isc._orginal_showPrompt(prompt, props);
};

