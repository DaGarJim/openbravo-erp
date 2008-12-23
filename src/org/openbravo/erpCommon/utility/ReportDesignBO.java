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
 * The Initial Developer of the Original Code is Openbravo SL 
 * All portions are Copyright (C) 2007 Openbravo SL 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import java.util.Iterator;

import net.sf.jasperreports.engine.JRElement;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignExpressionChunk;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignLine;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;

public class ReportDesignBO {
    private int px = 0;

    private int pageWidth = 0;

    private JasperDesign jasperDesign;

    private GridReportVO gridReportVO;

    public ReportDesignBO(JasperDesign jasperDesign, GridReportVO gridReportVO) {
        super();
        this.jasperDesign = jasperDesign;
        this.gridReportVO = gridReportVO;
        this.jasperDesign.setPageWidth(gridReportVO.getTotalWidth());
        this.pageWidth = jasperDesign.getPageWidth()
                - jasperDesign.getLeftMargin() - jasperDesign.getRightMargin();
    }

    private void addField(GridColumnVO columnVO) throws JRException {
        addFieldHeader(columnVO);
        addFieldValue(columnVO);
        px += calcPorc(columnVO.getWidth());
    }

    private void addFieldHeader(GridColumnVO columnVO) {
        JRDesignBand bHeader = (JRDesignBand) jasperDesign.getColumnHeader();
        JRDesignStaticText text = new JRDesignStaticText();
        int w = columnVO.getWidth();
        if (columnVO.getTitle().length() * 15 > w)
            columnVO.setWidth(columnVO.getTitle().length() * 15);
        text.setText(columnVO.getTitle());
        text.setWidth(columnVO.getWidth());
        text.setHeight(bHeader.getHeight());
        text.setX(px);
        // Set syle
        text.setFontName(gridReportVO.getHeaderBandStyle().getFontName());
        text.setFontSize(gridReportVO.getHeaderBandStyle().getFontSize());
        text.setForecolor(gridReportVO.getHeaderBandStyle().getForeColor());
        text.setBold(gridReportVO.getHeaderBandStyle().isBold());
        text.setItalic(gridReportVO.getHeaderBandStyle().isItalic());
        text.setUnderline(gridReportVO.getHeaderBandStyle().isUnderline());
        bHeader.addElement(text);
    }

    private void addFieldValue(GridColumnVO columnVO) throws JRException {
        JRDesignBand bDetalle = (JRDesignBand) jasperDesign.getDetail();

        JRDesignField f = new JRDesignField();
        f.setName(columnVO.getDbName());
        f.setValueClass(columnVO.getFieldClass());
        jasperDesign.addField(f);

        JRDesignExpressionChunk chunk = new JRDesignExpressionChunk();
        chunk.setText(columnVO.getDbName());
        chunk.setType(JRDesignExpressionChunk.TYPE_FIELD);
        JRDesignExpression expression = new JRDesignExpression();
        expression.addChunk(chunk);
        expression.setValueClass(columnVO.getFieldClass());
        JRDesignTextField textField = new JRDesignTextField();
        textField.setWidth(calcPorc(columnVO.getWidth()));
        textField.setHeight(bDetalle.getHeight());
        textField.setX(px);
        textField.setExpression(expression);
        textField.setBlankWhenNull(true);
        textField.setFontName(gridReportVO.getDetailBandStyle().getFontName());
        textField.setFontSize(gridReportVO.getDetailBandStyle().getFontSize());
        textField
                .setForecolor(gridReportVO.getDetailBandStyle().getForeColor());
        textField.setBold(gridReportVO.getDetailBandStyle().isBold());
        textField.setItalic(gridReportVO.getDetailBandStyle().isItalic());
        textField.setUnderline(gridReportVO.getDetailBandStyle().isUnderline());
        textField.setStretchWithOverflow(true);
        textField
                .setStretchType(JRElement.STRETCH_TYPE_RELATIVE_TO_TALLEST_OBJECT);

        bDetalle.addElement(textField);
    }

    private int calcPorc(int width) {
        if (gridReportVO.getPagination()) {
            return width;
        } else {
            return width * pageWidth / gridReportVO.getTotalWidth();
        }
    }

    public void define() throws JRException {
        defineTitle(gridReportVO.getTitle());
        defineLineWidth();
        Iterator<?> it = gridReportVO.getColumns().iterator();
        // jasperDesign.getTitle().setPrintWhenExpression(false);
        while (it.hasNext()) {
            addField((GridColumnVO) it.next());
        }
    }

    private void defineTitle(String title) throws JRException {
        JRDesignBand bTitulo = (JRDesignBand) jasperDesign.getTitle();
        JRDesignStaticText text = (JRDesignStaticText) bTitulo
                .getElementByKey("staticTitle");
        text.setText(title);
    }

    private void defineLineWidth() throws JRException {
        JRDesignBand bTitulo = (JRDesignBand) jasperDesign.getTitle();
        JRDesignLine line = (JRDesignLine) bTitulo
                .getElementByKey("title-top-line");
        line.setWidth(this.pageWidth);
        line = (JRDesignLine) bTitulo.getElementByKey("title-bottom-line");
        line.setWidth(this.pageWidth);
        bTitulo = (JRDesignBand) jasperDesign.getColumnHeader();
        line = (JRDesignLine) bTitulo.getElementByKey("columnHeader-top-line");
        line.setWidth(this.pageWidth);
        line = (JRDesignLine) bTitulo
                .getElementByKey("columnHeader-bottom-line");
        line.setWidth(this.pageWidth);
        bTitulo = (JRDesignBand) jasperDesign.getPageFooter();
        line = (JRDesignLine) bTitulo.getElementByKey("pageFooter-top-line");
        line.setWidth(this.pageWidth);
    }
}
