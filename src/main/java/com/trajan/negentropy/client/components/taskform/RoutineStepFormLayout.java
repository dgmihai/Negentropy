package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.server.facade.model.RoutineStep;

public class RoutineStepFormLayout extends TaskNodeDataFormLayout<RoutineStep>{
    public RoutineStepFormLayout(ClientDataController controller, RoutineStep node) {
        super(controller, node, RoutineStep.class);

        recurringCheckbox.removeFromParent();
        cronField.removeFromParent();
    }
}
