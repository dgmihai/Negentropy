package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.controller.dataproviders.TaskEntryDataProviderManager;

public class TestClientDataControllerImpl extends ClientDataController {
    public TestClientDataControllerImpl(SessionServices services) {
        this.services = services;
        this.taskNetworkGraph = new TestTaskNetworkGraph(services);
        this.taskEntryDataProviderManager = new TaskEntryDataProviderManager();
    }

    public static class TestTaskNetworkGraph extends TaskNetworkGraph {
        TestTaskNetworkGraph(SessionServices services) {
            this.services = services;
            init();
        }
    }
}
