package com.trajan.negentropy.model.entity.netduration;

import com.trajan.negentropy.model.entity.TaskEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NetDurationID implements Serializable {
    private TaskEntity task;
    private int importance;
}