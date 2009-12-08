/*
 * This file is part of ###PROJECT_NAME###
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.navalplanner.web.orders;

import static org.navalplanner.web.I18nHelper._;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.InvalidValue;
import org.navalplanner.business.calendars.entities.BaseCalendar;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.orders.entities.IOrderLineGroup;
import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.web.common.IMessagesForUser;
import org.navalplanner.web.common.Level;
import org.navalplanner.web.common.MessagesForUser;
import org.navalplanner.web.common.OnlyOneVisible;
import org.navalplanner.web.common.Util;
import org.navalplanner.web.planner.order.IOrderPlanningGate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Tab;
import org.zkoss.zul.api.Window;

/**
 * Controller for CRUD actions <br />
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
@org.springframework.stereotype.Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class OrderCRUDController extends GenericForwardComposer {

    private final class LabelCreatorForInvalidValues implements
            IMessagesForUser.ICustomLabelCreator {
        @Override
        public Component createLabelFor(
                InvalidValue invalidValue) {
            if (invalidValue.getBean() instanceof OrderElement) {
                Label result = new Label();

                String orderElementName;
                if (invalidValue.getBean() instanceof Order) {
                    orderElementName = _("Order");
                } else {
                    orderElementName = ((OrderElement) invalidValue
                            .getBean()).getName();
                }

                result.setValue(orderElementName + " "
                        + invalidValue.getPropertyName() + ": "
                        + invalidValue.getMessage());
                return result;
            } else {
                return MessagesForUser
                        .createLabelFor(invalidValue);
            }
        }
    }

    private static final org.apache.commons.logging.Log LOG = LogFactory
            .getLog(OrderCRUDController.class);

    @Autowired
    private IOrderModel orderModel;

    private IMessagesForUser messagesForUser;

    private Component messagesContainer;

    private Window editWindow;

    private Window listWindow;

    private OnlyOneVisible cachedOnlyOneVisible;

    private IOrderPlanningGate planningControllerEntryPoints;

    private BaseCalendarsComboitemRenderer baseCalendarsComboitemRenderer = new BaseCalendarsComboitemRenderer();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        messagesForUser = new MessagesForUser(messagesContainer);
        comp.setVariable("controller", this, true);
    }

    private void addEditWindowIfNeeded() {
        if (editWindow != null) {
            return;
        }
        Map<String, Object> editWindowArgs = new HashMap<String, Object>();
        editWindowArgs.put("top_id", editWindowArgs);
        Component parent = listWindow.getParent();
        listWindow.setVisible(false);
        cachedOnlyOneVisible = null;
        editWindow = (Window) Executions.createComponents(
                "/orders/_edition.zul",
                parent, editWindowArgs);
        Map<String, Object> editOrderElementArgs = new HashMap<String, Object>();
        editOrderElementArgs.put("top_id", "editOrderElement");
        Component editOrderElement = Executions.createComponents(
                "/orders/_editOrderElement.zul",
                parent, editOrderElementArgs);
        try {
            setupEditControllers();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Util.createBindingsFor(editWindow);
        Util.reloadBindings(editWindow);
        Util.createBindingsFor(editOrderElement);
        Util.reloadBindings(editOrderElement);
    }

    private void setupEditControllers() throws Exception {
        Component comp = self;
        OrderElementController orderElementController = new OrderElementController();
        orderElementController.doAfterCompose(comp
                .getFellow("editOrderElement"));

        setupOrderElementTreeController(comp, orderElementController);
        setupDetailsOrderElementController(comp);
        setupAsignedHoursToOrderElementController(comp);
        setupManageOrderElementAdvancesController(comp);
        setupAssignedLabelsToOrderElementController(comp);
        setupAssignedCriterionRequirementsToOrderElementController(comp);
        setupAssignedMaterialsToOrderElementController(comp);
    }

    private void setupOrderElementTreeController(Component comp,
            OrderElementController orderElementController) throws Exception {
        OrderElementTreeController controller = new OrderElementTreeController(
                orderModel, orderElementController);
        controller.doAfterCompose(editWindow.getFellowIfAny("orderElementTree"));
    }

    private DetailsOrderElementController detailsController;

    private void setupDetailsOrderElementController(Component comp) throws Exception {
        Component orderElementDetails = editWindow.getFellowIfAny("orderElementDetails");
        detailsController = (DetailsOrderElementController)
            orderElementDetails.getVariable("detailsController", true);
    }

    private IOrderElementModel getOrderElementModel() {
        final Order order = (Order) orderModel.getOrder();
        return orderModel.getOrderElementModel(order);
    }

    private AsignedHoursToOrderElementController assignedHoursController;

    private void setupAsignedHoursToOrderElementController(Component comp) throws Exception{
        Component orderElementHours = editWindow.getFellowIfAny("orderElementHours");
        assignedHoursController = (AsignedHoursToOrderElementController)
            orderElementHours.getVariable("asignedHoursToOrderElementController", true);
    }

        private ManageOrderElementAdvancesController manageOrderElementAdvancesController;

    private void setupManageOrderElementAdvancesController(Component comp) throws Exception {
        Component orderElementAdvances = editWindow.getFellowIfAny("orderElementAdvances");
        manageOrderElementAdvancesController = (ManageOrderElementAdvancesController)
            orderElementAdvances.getVariable("manageOrderElementAdvancesController", true);
    }

    private AssignedLabelsToOrderElementController assignedLabelsController;

    private void setupAssignedLabelsToOrderElementController(Component comp)
    throws Exception {
        Component orderElementLabels = editWindow.getFellowIfAny("orderElementLabels");
        assignedLabelsController = (AssignedLabelsToOrderElementController)
            orderElementLabels.getVariable("assignedLabelsController", true);
    }

    private AssignedCriterionRequirementToOrderElementController assignedCriterionRequirementController;

    private void setupAssignedCriterionRequirementsToOrderElementController(
            Component comp) throws Exception {
        Component orderElementCriterionRequirements = editWindow
                .getFellowIfAny("orderElementCriterionRequirements");
        assignedCriterionRequirementController = (AssignedCriterionRequirementToOrderElementController) orderElementCriterionRequirements
                .getVariable("assignedCriterionRequirementController", true);
    }

    private AssignedMaterialsToOrderElementController assignedMaterialsController;

    private void setupAssignedMaterialsToOrderElementController(
            Component comp) throws Exception {
        Component orderElementMaterials = editWindow
                .getFellowIfAny("orderElementMaterials");
        assignedMaterialsController = (AssignedMaterialsToOrderElementController) orderElementMaterials
                .getVariable("assignedMaterialsController", true);
    }

    public List<Order> getOrders() {
        return orderModel.getOrders();
    }

    private OnlyOneVisible getVisibility() {
        if (cachedOnlyOneVisible == null) {
            addEditWindowIfNeeded();
            cachedOnlyOneVisible = new OnlyOneVisible(listWindow, editWindow);
        }
        return cachedOnlyOneVisible;
    }

    public IOrderLineGroup getOrder() {
        return orderModel.getOrder();
    }

    public void saveAndContinue() {
        final boolean couldSave = save();
        if (couldSave) {
            orderModel.initEdit((Order) orderModel.getOrder());
            initializeTabs();
            showWindow(editWindow);
        }
    }

    public void saveAndExit() {
        final boolean couldSave = save();
        if (couldSave) {
            goToList();
        }
    }

    private boolean save() {
        if (!manageOrderElementAdvancesController.save()) {
            selectTab("tabAdvances");
        }
        if (!assignedCriterionRequirementController.close()) {
            selectTab("tabRequirements");
        }
        try {
            orderModel.save();
            messagesForUser.showMessage(Level.INFO, _("Order saved"));
            return true;
        } catch (ValidationException e) {
            messagesForUser.showInvalidValues(e, new LabelCreatorForInvalidValues());
        }
        return false;
    }

    private void selectTab(String str) {
        Tab tab = (Tab) editWindow.getFellowIfAny(str);
        if (tab != null) {
            tab.setSelected(true);
        }
    }

    public void goToList() {
        showWindow(listWindow);
    }

    public void reloadHoursGroupOrder() {
        assignedCriterionRequirementController
                .openWindow(getOrderElementModel());
        assignedCriterionRequirementController.reload();
    }

    private void showWindow(Window window) {
        getVisibility().showOnly(window);
        Util.reloadBindings(window);
    }

    public void cancel() {
        goToList();
    }

    public void up() {
        if (onUp == null) {
            throw new IllegalStateException(
                    "in order to call up onUp action should have been set");
        }
        onUp.run();
    }

    public void confirmRemove(Order order) {
        try {
            int status = Messagebox.show(_("Confirm deleting {0}. Are you sure?", order.getName()), "Delete",
                    Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION);
            if (Messagebox.OK == status) {
                remove(order);
            }
        } catch (InterruptedException e) {
            messagesForUser.showMessage(
                    Level.ERROR, e.getMessage());
            LOG.error(_("Error on showing removing element: ", order.getId()), e);
        }
    }

    private void remove(Order order) {
        orderModel.remove(order);
        Util.reloadBindings(self);
        messagesForUser.showMessage(Level.INFO, _("Removed {0}", order.getName()));
    }

    public void schedule(Order order) {
        if (order.isScheduled()) {
            planningControllerEntryPoints.goToScheduleOf(order);
        }else{
            try {
                Messagebox
                        .show(_("The order has no scheduled elements"),
                                _("Information"), Messagebox.OK,
                        Messagebox.INFORMATION);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Runnable onUp;

    public void goToEditForm(Order order) {
        orderModel.initEdit(order);
        showEditWindow(_("Edit order"));
    }

    private void showEditWindow(String title) {
        addEditWindowIfNeeded();
        clearEditWindow();
        initializeTabs();
        editWindow.setTitle(title);
        showWindow(editWindow);
    }

    private void initializeTabs() {
        final IOrderElementModel orderElementModel = getOrderElementModel();

        detailsController.openWindow(orderElementModel);
        assignedHoursController.openWindow(orderElementModel);
        manageOrderElementAdvancesController.openWindow(orderElementModel);
        assignedLabelsController.openWindow(orderElementModel);
        assignedCriterionRequirementController.openWindow(orderElementModel);
        assignedMaterialsController.openWindow(orderElementModel);
    }

    private void clearEditWindow() {
        OrderElementTreeController controller = (OrderElementTreeController) editWindow.getVariable("orderElementTreeController", true);
        controller.clear();
    }

    public void goToCreateForm() {
        orderModel.prepareForCreate();
        showEditWindow(_("Create order"));
    }

    public void setPlanningControllerEntryPoints(
            IOrderPlanningGate planningControllerEntryPoints) {
        this.planningControllerEntryPoints = planningControllerEntryPoints;
    }

    public void setActionOnUp(Runnable onUp) {
        this.onUp = onUp;
    }

    public List<BaseCalendar> getBaseCalendars() {
        return orderModel.getBaseCalendars();
    }

    public BaseCalendarsComboitemRenderer getBaseCalendarsComboitemRenderer() {
        return baseCalendarsComboitemRenderer;
    }

    private class BaseCalendarsComboitemRenderer implements ComboitemRenderer {

        @Override
        public void render(Comboitem item, Object data) throws Exception {
            BaseCalendar calendar = (BaseCalendar) data;
            item.setLabel(calendar.getName());
            item.setValue(calendar);

            BaseCalendar current = orderModel.getCalendar();
            if ((current != null) && calendar.getId().equals(current.getId())) {
                Combobox combobox = (Combobox) item.getParent();
                combobox.setSelectedItem(item);
            }
        }

    }

    public void setBaseCalendar(BaseCalendar calendar) {
        orderModel.setCalendar(calendar);
    }

}
