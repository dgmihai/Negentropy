package com.trajan.negentropy.client.components.filterform;

import com.trajan.negentropy.client.controller.ClientDataController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TreeFilterForm extends FilterForm {
    public TreeFilterForm(ClientDataController controller) {
        super(controller);
        controller.taskEntryDataProviderManager().setFilter(binder.getBean());
    }

    @Override
    protected void configureInteractions() {
        super.configureInteractions();

        binder.setBean(controller.settings().filter());

        binder.addValueChangeListener(event -> {
            log.debug("VALUE CHANGE LISTENER");
            controller.taskEntryDataProviderManager().setFilter(binder.getBean());
        });
    }
}
