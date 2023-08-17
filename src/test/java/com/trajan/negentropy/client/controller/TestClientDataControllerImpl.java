package com.trajan.negentropy.client.controller;

public class TestClientDataControllerImpl extends ClientDataControllerImpl {
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
