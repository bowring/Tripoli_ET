/*
 * Copyright 2022 James Bowring, Noah McLean, Scott Burdick, and CIRDLES.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cirdles.tripoli.gui.dataViews.plots.plotsControllers.tripoliPlots;

import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import org.cirdles.tripoli.gui.dataViews.plots.AbstractPlot;
import org.cirdles.tripoli.gui.dataViews.plots.TicGeneratorForAxes;
import org.cirdles.tripoli.plots.linePlots.BeamShapeRecord;

public class BeamShapeLinePlotX extends AbstractPlot {
    private final Tooltip tooltip;
    protected BeamShapeRecord beamShapeRecord;
    private int leftBoundary;
    private int rightBoundary;

    /**
     * @param bounds
     * @param beamShapeRecord
     */
    private BeamShapeLinePlotX(Rectangle bounds, BeamShapeRecord beamShapeRecord) {
        super(bounds, 50, 35, beamShapeRecord.title(), beamShapeRecord.xAxisLabel(), beamShapeRecord.yAxisLabel());
        this.beamShapeRecord = beamShapeRecord;


        setupPlotContextMenu();
        tooltip = new Tooltip();
        Tooltip.install(this, tooltip);
        this.setOnMouseMoved(new MouseMovedHandler());
        this.setOnMouseClicked(new MouseClickEventHandler());
    }

    public static AbstractPlot generatePlot(Rectangle bounds, BeamShapeRecord beamShapeRecord) {
        return new BeamShapeLinePlotX(bounds, beamShapeRecord);
    }

    @Override
    public void preparePanel(boolean reScaleX, boolean reScaleY) {
        xAxisData = beamShapeRecord.xData();
        minX = xAxisData[0];
        maxX = xAxisData[xAxisData.length - 1];

        leftBoundary = beamShapeRecord.leftBoundary();
        rightBoundary = beamShapeRecord.rightBoundary();

        yAxisData = beamShapeRecord.yData();
        minY = Double.MAX_VALUE;
        maxY = -Double.MAX_VALUE;


        for (int i = 0; i < yAxisData.length; i++) {
            minY = StrictMath.min(minY, yAxisData[i]);
            maxY = StrictMath.max(maxY, yAxisData[i]);
        }


        displayOffsetX = 0.0;
        displayOffsetY = 0.0;

        prepareExtents(true, true);
        calculateTics();
        repaint();
    }

    @Override
    public void paint(GraphicsContext g2d) {
        super.paint(g2d);
    }

    public void prepareExtents(boolean reScaleX, boolean reScaleY) {
        double xMarginStretch = TicGeneratorForAxes.generateMarginAdjustment(minX, maxX, 0.01);
        if (xMarginStretch == 0.0) {
            xMarginStretch = maxX * 0.01;
        }
        minX -= xMarginStretch;
        maxX += xMarginStretch;

        double yMarginStretch = TicGeneratorForAxes.generateMarginAdjustment(minY, maxY, 0.01);
        maxY += yMarginStretch;
        minY -= yMarginStretch;
    }

    /**
     * @param g2d
     */
    @Override
    public void showLegend(GraphicsContext g2d) {

    }

    @Override
    public void plotData(GraphicsContext g2d) {
        g2d.setLineWidth(2.2);
        // new line graph
        g2d.setStroke(Paint.valueOf("Black"));
        g2d.beginPath();
        g2d.moveTo(mapX(xAxisData[0]), mapY(yAxisData[0]));
        for (int i = 0; i < xAxisData.length; i++) {
            // line tracing through points
            if (pointInPlot(xAxisData[i], yAxisData[i])) {
                // line tracing through points
                g2d.lineTo(mapX(xAxisData[i]), mapY(yAxisData[i]));
            } else {
                // out of bounds
                g2d.moveTo(mapX(xAxisData[i]), mapY(yAxisData[i]));
            }

        }

        g2d.stroke();
        g2d.beginPath();
        g2d.setLineDashes(8);
        g2d.setStroke(Paint.valueOf("Blue"));
        for (int i = leftBoundary; i <= rightBoundary; i++) {
            // line tracing through points
            if (pointInPlot(xAxisData[i], yAxisData[leftBoundary])) {
                // line tracing through points
                g2d.lineTo(mapX(xAxisData[i]), mapY(yAxisData[leftBoundary]));
            } else {
                // out of bounds
                g2d.moveTo(mapX(xAxisData[i]), mapY(yAxisData[leftBoundary]));
            }

        }
        g2d.stroke();

        g2d.setFill(Paint.valueOf("Red"));
        if (pointInPlot(xAxisData[leftBoundary], yAxisData[leftBoundary])) {
            // line tracing through points

            g2d.fillOval(mapX(xAxisData[leftBoundary]) - 3.5, mapY(yAxisData[leftBoundary]) - 3.5, 7, 7);
            g2d.fillOval(mapX(xAxisData[rightBoundary]) - 3.5, mapY(yAxisData[rightBoundary]) - 3.5, 7, 7);
        } else if (pointInPlot(xAxisData[rightBoundary], yAxisData[rightBoundary])) {
            // line tracing through points
            g2d.fillOval(mapX(xAxisData[rightBoundary]) - 3.5, mapY(yAxisData[rightBoundary]) - 3.5, 7, 7);
        } else {
            // out of bounds
            g2d.moveTo(mapX(xAxisData[leftBoundary]) - 3.5, mapY(yAxisData[leftBoundary]));
            g2d.moveTo(mapX(xAxisData[rightBoundary]) - 3.5, mapY(yAxisData[rightBoundary]));
        }


        g2d.beginPath();
        g2d.setLineDashes(0);

        g2d.stroke();
    }

    @Override
    public void plotStats(GraphicsContext g2d) {

    }

    @Override
    public void setupPlotContextMenu() {
        plotContextMenu = new ContextMenu();
        MenuItem plotContextMenuItem1 = new MenuItem("Restore plot");
        plotContextMenuItem1.setOnAction((mouseEvent) -> {
            refreshPanel(true, true);
        });

        plotContextMenu.getItems().addAll(plotContextMenuItem1);

    }

    private void showToolTip(Node node, MouseEvent event, double xPos, double yPos) {
        String x = String.format("%.3f", xPos);
        String y = String.format("%.2f", yPos);
        tooltip.setText("(x=" + x + ", y=" + y + ")");
        tooltip.setAnchorX(event.getSceneX());
        tooltip.show(node, event.getScreenX() + 15, event.getScreenY() + 15);
    }

    private class MouseClickEventHandler implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent mouseEvent) {
            plotContextMenu.hide();
            boolean isPrimary = mouseEvent.getButton().compareTo(MouseButton.PRIMARY) == 0;

            if (mouseInHouse(mouseEvent.getX(), mouseEvent.getY())) {
                if (isPrimary) {
                } else {
                    plotContextMenu.show((Node) mouseEvent.getSource(), Side.LEFT, mouseEvent.getSceneX(), mouseEvent.getSceneY());
                }
            }

        }
    }


    private class MouseMovedHandler implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent event) {

            Node potNode;

            if (mouseInHouse(event.getX(), event.getY())) {

                potNode = ((Canvas) event.getSource()).getParent();
                int minIndex = 0;
                double minVal = Integer.MAX_VALUE;


                // Displays toolTip of x and y positions on the Beam Shape line plot
                for (int i = 1; i < getxAxisData().length; i++) {
                    double diff = Math.abs(getxAxisData()[i - 1] - getxAxisData()[i]);

                    if (getyAxisData()[i] < minVal) {
                        minVal = getyAxisData()[i];
                        minIndex = i;
                    }
                    if ((getxAxisData()[i] >= convertMouseXToValue(event.getX()) - diff && getxAxisData()[i] <= convertMouseXToValue(event.getX()) + diff)) {
                        double diffY = Math.abs(getyAxisData()[i - 1] - getyAxisData()[i]);
                        if ((getyAxisData()[i] >= convertMouseYToValue(event.getY()) - diffY && getyAxisData()[i] <= convertMouseYToValue(event.getY()) + diffY)) {
                            showToolTip(potNode, event, getxAxisData()[i], getyAxisData()[i]);
                            ((Canvas) event.getSource()).setCursor(Cursor.CROSSHAIR);
                        } else if (convertMouseYToValue(event.getY()) - maxY / 200 <= getyAxisData()[minIndex] && convertMouseYToValue(event.getY()) + maxY / 200 >= getyAxisData()[minIndex]) {
                            showToolTip(potNode, event, getxAxisData()[i], getyAxisData()[i]);
                            ((Canvas) event.getSource()).setCursor(Cursor.CROSSHAIR);
                        } else {
                            ((Canvas) event.getSource()).setCursor(Cursor.DEFAULT);
                        }
                    }
                }


            } else {
                ((Canvas) event.getSource()).getParent().getScene().setCursor(Cursor.DEFAULT);
                tooltip.hide();
            }
        }
    }
}