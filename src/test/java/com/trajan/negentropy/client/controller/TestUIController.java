package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.controller.dataproviders.TaskEntryDataProviderManager;
import com.trajan.negentropy.client.sessionlogger.SessionLogger;

public class TestUIController extends UIController {
    public TestUIController(SessionServices services) {
        this.services = services;
        this.log = new SessionLogger(getClass());
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
