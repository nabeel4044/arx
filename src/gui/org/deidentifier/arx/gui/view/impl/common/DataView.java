/*
 * ARX: Efficient, Stable and Optimal Data Anonymization
 * Copyright (C) 2012 - 2013 Florian Kohlmayer, Fabian Prasser
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.deidentifier.arx.gui.view.impl.common;

import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.DataDefinition;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.RowSet;
import org.deidentifier.arx.gui.Controller;
import org.deidentifier.arx.gui.model.Model;
import org.deidentifier.arx.gui.model.ModelEvent;
import org.deidentifier.arx.gui.model.ModelEvent.ModelPart;
import org.deidentifier.arx.gui.resources.Resources;
import org.deidentifier.arx.gui.view.SWTUtil;
import org.deidentifier.arx.gui.view.def.IDataView;
import org.deidentifier.arx.gui.view.def.IView;
import org.eclipse.nebula.widgets.nattable.layer.ILayerListener;
import org.eclipse.nebula.widgets.nattable.layer.event.ILayerEvent;
import org.eclipse.nebula.widgets.nattable.selection.event.CellSelectionEvent;
import org.eclipse.nebula.widgets.nattable.selection.event.ColumnSelectionEvent;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolItem;

/**
 * A view on a <code>Data</code> object
 * @author Prasser, Kohlmayer
 */
public class DataView implements IDataView, IView {

    private final Image       IMAGE_INSENSITIVE;
    private final Image       IMAGE_SENSITIVE;
    private final Image       IMAGE_QUASI_IDENTIFYING;
    private final Image       IMAGE_IDENTIFYING;

    private final DataTable   table;
    private final ModelPart target;
    private final ModelPart reset;
    private final Controller  controller;
    private final ToolItem    sortButton;
    
    private DataHandle        handle = null;
    private Model             model;

    /** 
     * Creates a new data view
     * 
     * @param parent
     * @param controller
     * @param title
     * @param target
     * @param reset
     */
    public DataView(final Composite parent,
                    final Controller controller,
                    final String title,
                    final ModelPart target,
                    final ModelPart reset) {

        // Register
        controller.addListener(ModelPart.RESEARCH_SUBSET, this);
        controller.addListener(ModelPart.ATTRIBUTE_TYPE, this);
        controller.addListener(ModelPart.MODEL, this);
        controller.addListener(ModelPart.SORT_ORDER, this);
        controller.addListener(target, this);
        this.controller = controller;
        if (reset != null) {
            controller.addListener(reset, this);
        }
        this.reset = reset;
        this.target = target;

        // Load images
        IMAGE_INSENSITIVE       = controller.getResources().getImage("bullet_green.png"); //$NON-NLS-1$
        IMAGE_SENSITIVE         = controller.getResources().getImage("bullet_purple.png"); //$NON-NLS-1$
        IMAGE_QUASI_IDENTIFYING = controller.getResources().getImage("bullet_yellow.png"); //$NON-NLS-1$
        IMAGE_IDENTIFYING       = controller.getResources().getImage("bullet_red.png"); //$NON-NLS-1$

        // Create title bar
        TitleBar bar = new TitleBar("id-140");
        bar.add(Resources.getMessage("DataView.0"), //$NON-NLS-1$
                controller.getResources().getImage("sort_none.png"),
                new Runnable() {
                    @Override
                    public void run() {
                        
                    }
                });
        bar.add(Resources.getMessage("DataView.1"), //$NON-NLS-1$ 
                controller.getResources().getImage("sort_column.png"),
                new Runnable() {
                    @Override
                    public void run() {
                        
                    }
                });
        bar.add(Resources.getMessage("DataView.2"), //$NON-NLS-1$ 
                controller.getResources().getImage("sort_groups.png"),
                new Runnable() {
                    @Override
                    public void run() {
                        
                    }
                });
        bar.add(Resources.getMessage("DataView.3"), //$NON-NLS-1$ 
                controller.getResources().getImage("sort_subset.png"),
                new Runnable() {
                    @Override
                    public void run() {
                        
                    }
                });
        
        // Build border
        TitledFolder folder = new TitledFolder(parent, controller, bar, null);
        folder.setLayoutData(SWTUtil.createFillGridData());
        Composite c = folder.createItem(title, null);
        folder.setSelection(0);
        GridLayout l = new GridLayout();
        l.numColumns = 1;
        c.setLayout(l);
        
        // Build table
        table = new DataTable(controller, c);
        table.addSelectionLayerListener(new ILayerListener(){
            @Override
            public void handleLayerEvent(ILayerEvent arg0) {
                if (arg0 instanceof CellSelectionEvent) {
                    actionCellSelected((CellSelectionEvent)arg0);
                } else if (arg0 instanceof ColumnSelectionEvent) {
                    actionColumnSelected((ColumnSelectionEvent)arg0);
                }
            }
        });
        
        this.sortButton = folder.getBarItem(Resources.getMessage("DataView.2")); //$NON-NLS-1$
        this.sortButton.setEnabled(false);
    }
    
    /**
     * Column selection event
     * @param arg1
     */
    private void actionColumnSelected(ColumnSelectionEvent arg1) {
        if (model != null) {
            int column = arg1.getColumnPositionRanges().iterator().next().start - 1;
            if (column>=0){
                final String attr = handle.getAttributeName(column);
                model.setSelectedAttribute(attr);
                controller.update(new ModelEvent(this,
                                                 ModelPart.SELECTED_ATTRIBUTE,
                                                 attr));
            }
        }
    }
    
    /**
     * Cell selection event
     * @param arg1
     */
    private void actionCellSelected(CellSelectionEvent arg1) {

        if (model != null) {
            int column = arg1.getColumnPosition();
            if (column==0){
                int row = arg1.getRowPosition();
                if (row>=0){
                    RowSet subset = model.getInputConfig().getResearchSubset();
                    if (subset.contains(row)) {
                        subset.remove(row);
                    } else {
                        subset.add(row);
                    }
                    model.setSubsetManual();
                    controller.update(new ModelEvent(this,
                                                     ModelPart.RESEARCH_SUBSET,
                                                     subset));
                }
            }
        }
    }

    @Override
    public void addScrollBarListener(final Listener listener) {
        table.addScrollBarListener(listener);
    }

    @Override
    public void dispose() {
        controller.removeListener(this);
        IMAGE_INSENSITIVE.dispose();
        IMAGE_SENSITIVE.dispose();
        IMAGE_QUASI_IDENTIFYING.dispose();
        IMAGE_IDENTIFYING.dispose();
        table.dispose();
    }

    @Override
    public ViewportLayer getViewportLayer() {
        return table.getViewportLayer();
    }

    @Override
    public void reset() {
        table.reset();
        handle = null;
    }

    @Override
    public void update(final ModelEvent event) {

        if (event.part == reset) {
            
            reset();

        } else if (event.part == ModelPart.MODEL) {
            
            // Store and reset
            model = (Model) event.data;
            reset();

        } else if (event.part == target) {

            // No result avail
            if (event.data == null) {
                reset();
                return;
            }
            
            // Obtain data definition
            DataDefinition definition = model.getInputConfig().getInput().getDefinition();
            if (target == ModelPart.OUTPUT) {
                definition = model.getOutputConfig().getInput().getDefinition();
            }

            // Update the table
            handle = (DataHandle) event.data;
            table.setData(handle, 
                          model.getInputConfig().getResearchSubset(), 
                          target == ModelPart.OUTPUT ? model.getColors() : null, 
                          target == ModelPart.OUTPUT ? model.getGroups() : null);
            
            // Update the attribute types
            table.getHeaderImages().clear();
            for (int i = 0; i < handle.getNumColumns(); i++) {
                updateHeaderImage(i, definition.getAttributeType(handle.getAttributeName(i)));
            }
            
            // Redraw
            table.setEnabled(true);
            table.redraw();

        } else if (event.part == ModelPart.ATTRIBUTE_TYPE) {
            
            if ((model != null) && (handle != null)) {
                
                final String attr = (String) event.data;


                // Obtain data definition
                DataDefinition definition = model.getInputConfig().getInput().getDefinition();
                if (target == ModelPart.OUTPUT) {
                    definition = model.getOutputConfig().getInput().getDefinition();
                }

                // Update the attribute types
                final int index = handle.getColumnIndexOf(attr);
                updateHeaderImage(index, definition.getAttributeType(attr));
                
                // Redraw
                table.setEnabled(true);
                table.redraw();
            }
            
        } else if (event.part == ModelPart.RESEARCH_SUBSET) {
            
            // Update research subset
            table.setResearchSubset((RowSet)event.data);
            table.redraw();
            
        } else if (event.part == ModelPart.SORT_ORDER) {
            
            table.redraw();
        }
        
        // Enable/Disable sort button
        if (event.part == ModelPart.OUTPUT ||
            event.part == ModelPart.INPUT) {
            if (model != null && model.getOutput()!=null){
                sortButton.setEnabled(true);
            } else {
                sortButton.setEnabled(false);
            }
        }
    }

    /**
     * Updates the header image in the table
     * @param index
     * @param type
     */
    private void updateHeaderImage(final int index, final AttributeType type) {
        if (table.getHeaderImages().size() <= index) {
            table.getHeaderImages().add(null);
        }
        if (type == AttributeType.INSENSITIVE_ATTRIBUTE) {
            table.getHeaderImages().set(index, IMAGE_INSENSITIVE);
        } else if (type == AttributeType.IDENTIFYING_ATTRIBUTE) {
            table.getHeaderImages().set(index, IMAGE_IDENTIFYING);
        } else if (type == AttributeType.SENSITIVE_ATTRIBUTE) {
            table.getHeaderImages().set(index, IMAGE_SENSITIVE);
        } else {
            table.getHeaderImages().set(index, IMAGE_QUASI_IDENTIFYING);
        }
    }
}
