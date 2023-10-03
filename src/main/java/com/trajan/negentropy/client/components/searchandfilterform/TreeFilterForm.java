package com.trajan.negentropy.client.components.searchandfilterform;

import com.trajan.negentropy.client.controller.UIController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TreeFilterForm extends TaskNodeFilterForm {
    public TreeFilterForm(UIController controller) {
        super(controller);
    }

    @Override
    protected void configureInteractions() {
        super.configureInteractions();

        binder.setBean(controller().settings().filter());

        binder.addValueChangeListener(event -> {
            controller().taskNetworkGraph().taskEntryDataProvider().setFilter(binder.getBean());
        });
    }
}
