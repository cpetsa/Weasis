/*******************************************************************************
 * Copyright (c) 2010 Nicolas Roduit.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Nicolas Roduit - initial API and implementation
 ******************************************************************************/
package org.weasis.core.ui.graphic;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.weasis.core.api.gui.util.GeomUtil;
import org.weasis.core.api.image.measure.MeasurementsAdapter;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.ui.util.MouseEventDouble;

/**
 * @author Benoit Jacquemoud
 */
public class FourPointsAngleToolGraphic extends AbstractDragGraphic {

    public static final Icon ICON = new ImageIcon(
        FourPointsAngleToolGraphic.class.getResource("/icon/22x22/draw-4p-angle.png")); //$NON-NLS-1$

    public static final Measurement ANGLE = new Measurement("Angle", 1, true);
    public static final Measurement COMPLEMENTARY_ANGLE = new Measurement("Compl. Angle", 2, true, true, false);

    // ///////////////////////////////////////////////////////////////////////////////////////////////////

    Point2D ptA, ptB, ptC, ptD; // Let AB & CD two line segments which define the median line IJ
    Point2D ptI, ptJ; // Let I,J be the middle points of AB & CD line segments

    Point2D ptE, ptF, ptG, ptH; // Let EF & GH two line segments which define the median line KL
    Point2D ptK, ptL; // Let K,L be the middle points of EF & GH line segments

    Point2D ptP; // Let P be the intersection point, if exist, of the two line segments IJ & KL

    Point2D[] lineIJP; // (IJP) or (JIP) or (IPJ) or (JPI) <= ordered array of points along IJ segment.
    Point2D[] lineKLP; // (KLP) or (LKP) or (KPL) or (LPK) <= ordered array of points along KL segment.

    boolean lineParallel; // estimate if IJ & KL line segments are parallel not not
    boolean intersectIJsegment; // estimate if intersection point, if exist, is inside IJ segment or not
    boolean intersectKLsegment; // estimate if intersection point, if exist, is inside KL segment or not

    // estimate if line segments are valid or not
    boolean lineABvalid, lineCDvalid, lineEFvalid, lineGHvalid, lineIJvalid, lineKLvalid;

    double angleDeg; // smallest angle in Degrees in the range of [-180 ; 180] between IJ & KL line segments

    // ///////////////////////////////////////////////////////////////////////////////////////////////////

    public FourPointsAngleToolGraphic(float lineThickness, Color paintColor, boolean labelVisible) {
        super(8, paintColor, lineThickness, labelVisible);
        init();
    }

    @Override
    public Icon getIcon() {
        return ICON;
    }

    @Override
    public String getUIName() {
        return "Four Points Angle Tool";
    }

    @Override
    protected void updateShapeOnDrawing(MouseEventDouble mouseEvent) {
        updateTool();

        Shape newShape = null;
        Path2D path = new Path2D.Double(Path2D.WIND_NON_ZERO, 6);

        if (lineABvalid) {
            path.append(new Line2D.Double(ptA, ptB), false);
        }

        if (lineCDvalid) {
            path.append(new Line2D.Double(ptC, ptD), false);
        }

        if (lineIJvalid) {
            path.append(new Line2D.Double(ptI, ptJ), false);
        }

        if (lineEFvalid) {
            path.append(new Line2D.Double(ptE, ptF), false);
        }

        if (lineGHvalid) {
            path.append(new Line2D.Double(ptG, ptH), false);
        }

        if (lineKLvalid) {
            path.append(new Line2D.Double(ptK, ptL), false);
        }

        // Do not show decoration when lines are nearly parallel
        // Can cause stack overflow BUG on paint method when drawing infinite line with DashStroke
        if (lineIJvalid && lineKLvalid && !lineParallel && Math.abs(angleDeg) > 0.1) {

            AdvancedShape aShape = (AdvancedShape) (newShape = new AdvancedShape(4));
            aShape.addShape(path);

            // Let arcAngle be the partial section of the ellipse that represents the measured angle
            double startingAngle = GeomUtil.getAngleDeg(ptP, lineIJP[0]);

            double radius = 32;
            Rectangle2D arcAngleBounds =
                new Rectangle2D.Double(ptP.getX() - radius, ptP.getY() - radius, 2 * radius, 2 * radius);

            Shape arcAngle = new Arc2D.Double(arcAngleBounds, startingAngle, angleDeg, Arc2D.OPEN);

            double rMax = Math.min(ptP.distance(lineIJP[0]), ptP.distance(lineKLP[0])) * 2 / 3;
            double scalingMin = radius / rMax;

            aShape.addInvShape(arcAngle, ptP, scalingMin, true);

            if (!intersectIJsegment) {
                aShape.addShape(new Line2D.Double(ptP, lineIJP[1]), getDashStroke(1.0f), true);
            }

            if (!intersectKLsegment) {
                aShape.addShape(new Line2D.Double(ptP, lineKLP[1]), getDashStroke(1.0f), true);
            }

        } else if (path.getCurrentPoint() != null) {
            newShape = path;
        }

        setShape(newShape, mouseEvent);
        updateLabel(mouseEvent, getDefaultView2d(mouseEvent));

    }

    @Override
    public List<MeasureItem> computeMeasurements(ImageElement imageElement, boolean releaseEvent) {

        if (imageElement != null && isShapeValid()) {
            MeasurementsAdapter adapter = imageElement.getMeasurementAdapter();

            if (adapter != null) {
                ArrayList<MeasureItem> measVal = new ArrayList<MeasureItem>(2);

                if (ANGLE.isComputed() || COMPLEMENTARY_ANGLE.isComputed()) {

                    double positiveAngle = Math.abs(angleDeg);

                    if (ANGLE.isComputed()) {
                        measVal.add(new MeasureItem(ANGLE, positiveAngle, "deg"));
                    }

                    if (COMPLEMENTARY_ANGLE.isComputed()) {
                        measVal.add(new MeasureItem(COMPLEMENTARY_ANGLE, 180.0 - positiveAngle, "deg"));
                    }
                }
                return measVal;
            }
        }
        return null;
    }

    @Override
    public boolean isShapeValid() {
        updateTool();
        return (lineABvalid && lineCDvalid && lineEFvalid && lineGHvalid && lineIJvalid && lineKLvalid);
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void init() {
        ptA = getHandlePoint(0);
        ptB = getHandlePoint(1);
        ptC = getHandlePoint(2);
        ptD = getHandlePoint(3);

        ptI = ptA;
        ptJ = ptC;

        ptE = getHandlePoint(4);
        ptF = getHandlePoint(5);
        ptG = getHandlePoint(6);
        ptH = getHandlePoint(7);

        ptK = ptE;
        ptL = ptG;

        lineIJP = lineKLP = null;

        lineParallel = intersectIJsegment = intersectKLsegment = false;
        lineABvalid = lineCDvalid = lineEFvalid = lineGHvalid = lineIJvalid = lineKLvalid = false;

        angleDeg = 0.0;
    }

    protected void updateTool() {
        init();

        if (lineABvalid = (ptA != null && ptB != null && !ptB.equals(ptA))) {
            ptI = GeomUtil.getMidPoint(ptA, ptB);
        }

        if (lineCDvalid = (ptC != null && ptD != null && !ptC.equals(ptD))) {
            ptJ = GeomUtil.getMidPoint(ptC, ptD);
        }

        lineIJvalid = (ptI != null && ptJ != null && !ptI.equals(ptJ));

        if (lineEFvalid = (ptE != null && ptF != null && !ptE.equals(ptF))) {
            ptK = GeomUtil.getMidPoint(ptE, ptF);
        }

        if (lineGHvalid = (ptG != null && ptH != null && !ptG.equals(ptH))) {
            ptL = GeomUtil.getMidPoint(ptG, ptH);
        } else if (ptG == null && lineEFvalid) {
            ptL = GeomUtil.getPerpendicularPointFromLine(ptE, ptF, ptK, 1.0); // temporary before GHvalid
        }

        lineKLvalid = (ptK != null && ptL != null && !ptK.equals(ptL));

        if (lineIJvalid && lineKLvalid) {

            double denominator =
                (ptJ.getX() - ptI.getX()) * (ptL.getY() - ptK.getY()) - (ptJ.getY() - ptI.getY())
                    * (ptL.getX() - ptK.getX());

            lineParallel = (denominator == 0); // If denominator is zero, IJ & KL are parallel

            if (!lineParallel) {

                double numerator1 =
                    (ptI.getY() - ptK.getY()) * (ptL.getX() - ptK.getX()) - (ptI.getX() - ptK.getX())
                        * (ptL.getY() - ptK.getY());
                double numerator2 =
                    (ptI.getY() - ptK.getY()) * (ptJ.getX() - ptI.getX()) - (ptI.getX() - ptK.getX())
                        * (ptJ.getY() - ptI.getY());

                double r = numerator1 / denominator; // equ1
                double s = numerator2 / denominator; // equ2

                ptP =
                    new Point2D.Double(ptI.getX() + r * (ptJ.getX() - ptI.getX()), ptI.getY() + r
                        * (ptJ.getY() - ptI.getY()));

                // If 0<=r<=1 & 0<=s<=1, segment intersection exists
                // If r<0 or r>1 or s<0 or s>1, line segments intersect but not segments

                // If r>1, P is located on extension of IJ
                // If r<0, P is located on extension of JI
                // If s>1, P is located on extension of KL
                // If s<0, P is located on extension of LK

                lineIJP = new Point2D[3]; // order can be IJP (r>1) or JIP (r<0) or IPJ / JPI (0<=r<=1)
                lineKLP = new Point2D[3]; // order can be KLP (s>1) or LKP (s<0) or KPL / LPK (0<=s<=1)

                intersectIJsegment = (r >= 0 && r <= 1) ? true : false; // means IJPline[1].equals(P)
                intersectKLsegment = (s >= 0 && s <= 1) ? true : false; // means KLPline[1].equals(P)

                lineIJP[0] = r >= 0 ? ptI : ptJ;
                lineIJP[1] = r < 0 ? ptI : r > 1 ? ptJ : ptP;
                lineIJP[2] = r < 0 ? ptP : r > 1 ? ptP : ptJ;

                if (intersectIJsegment) {
                    if (ptP.distance(lineIJP[0]) < ptP.distance(lineIJP[2])) {
                        Point2D switchPt = (Point2D) lineIJP[2].clone();
                        lineIJP[2] = (Point2D) lineIJP[0].clone();
                        lineIJP[0] = switchPt;
                    }
                } else if (ptP.distance(lineIJP[0]) < ptP.distance(lineIJP[1])) {
                    Point2D switchPt = (Point2D) lineIJP[1].clone();
                    lineIJP[1] = (Point2D) lineIJP[0].clone();
                    lineIJP[0] = switchPt;
                }

                lineKLP[0] = s >= 0 ? ptK : ptL;
                lineKLP[1] = s < 0 ? ptK : s > 1 ? ptL : ptP;
                lineKLP[2] = s < 0 ? ptP : s > 1 ? ptP : ptL;

                if (intersectKLsegment) {
                    if (ptP.distance(lineKLP[0]) < ptP.distance(lineKLP[2])) {
                        Point2D switchPt = (Point2D) lineKLP[2].clone();
                        lineKLP[2] = (Point2D) lineKLP[0].clone();
                        lineKLP[0] = switchPt;
                    }
                } else if (ptP.distance(lineKLP[0]) < ptP.distance(lineKLP[1])) {
                    Point2D switchPt = (Point2D) lineKLP[1].clone();
                    lineKLP[1] = (Point2D) lineKLP[0].clone();
                    lineKLP[0] = switchPt;
                }

                angleDeg = GeomUtil.getSmallestRotationAngleDeg(GeomUtil.getAngleDeg(lineIJP[0], ptP, lineKLP[0]));
            }
        }
    }

    @Override
    public List<Measurement> getMeasurementList() {
        List<Measurement> list = new ArrayList<Measurement>();
        list.add(ANGLE);
        list.add(COMPLEMENTARY_ANGLE);
        return list;
    }
}
