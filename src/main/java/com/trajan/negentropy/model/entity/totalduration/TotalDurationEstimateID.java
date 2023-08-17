package com.trajan.negentropy.model.entity.totalduration;

import com.trajan.negentropy.model.entity.TaskEntity;

import java.io.Serializable;

public class TotalDurationEstimateID implements Serializable {
    private TaskEntity task;
    private int importance;
}