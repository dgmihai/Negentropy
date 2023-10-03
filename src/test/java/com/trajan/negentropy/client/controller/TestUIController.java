package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.client.session.TaskNetworkGraph;
import com.trajan.negentropy.client.sessionlogger.SessionLogger;

public class TestUIController extends UIController {
    public TestUIController(SessionServices services) {
        this.services = services;
        this.log = new SessionLogger(getClass());
        this.taskNetworkGraph = new TestTaskNetworkGraph(services);
    }

    public static class TestTaskNetworkGraph extends TaskNetworkGraph {
        TestTaskNetworkGraph(SessionServices services) {
            this.services = services;
            init();
        }
    }
}
