package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.client.session.TaskEntryDataProvider;
import com.trajan.negentropy.client.session.TaskNetworkGraph;

public class TestUIController extends UIController {
    public TestUIController(SessionServices services) {
        this.services = services;
        this.taskNetworkGraph = new TestTaskNetworkGraph(services);
    }

    public static class TestTaskNetworkGraph extends TaskNetworkGraph {
        TestTaskNetworkGraph(SessionServices services) {
            this.services = services;
            this.taskEntryDataProvider = new TaskEntryDataProvider();
            init();
        }
    }
}
