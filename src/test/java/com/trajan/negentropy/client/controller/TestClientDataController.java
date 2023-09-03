package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.controller.dataproviders.TaskEntryDataProviderManager;

public class TestClientDataController extends ClientDataController {
    public TestClientDataController(SessionServices services) {
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
