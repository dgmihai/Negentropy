package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskData;
import com.trajan.negentropy.model.data.HasTaskNodeData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.vaadin.flow.data.binder.Binder;

public interface Bound {

    interface BoundToTask extends Bound, TaskNodeProvider, HasTaskData {
        Binder<Task> taskBinder();

        @Override
        default Task task() {
            return taskBinder().getBean();
        }

        @Override
        default boolean isValid() {
            return taskBinder().isValid();
        }

        @Override
        default TaskNodeInfoData<?> getNodeInfo() {
            return new TaskNodeDTO();
        }

        @Override
        default Task getTask() {
            return isValid()
                    ? task()
                    : null;
        }
    }

    interface BoundToTaskAndNodeInfo extends BoundToTask {
        Binder<? extends TaskNodeInfoData<?>> nodeInfoBinder();

        @Override
        default boolean isValid() {
            return taskBinder().isValid() && nodeInfoBinder().isValid();
        }

        @Override
        default TaskNodeInfoData<?> getNodeInfo() {
            return nodeInfoBinder().getBean();
        }
    }

    interface BoundToTaskNodeData<T extends HasTaskNodeData> extends Bound, TaskNodeProvider, HasTaskNodeData, HasTaskData {
        Binder<T> binder();

        @Override
        default boolean isValid() {
            return binder().isValid();
        }

        @Override
        default Task getTask() {
            return isValid()
                    ? binder().getBean().task()
                    : null;
        }

        @Override
        default TaskNodeDTOData<?> getNodeInfo() {
            return binder().getBean().node();
        }

        @Override
        default TaskNode node() {
            return binder().getBean().node();
        }
    }

    interface BoundToRoutineStep<T extends RoutineStep> extends Bound, TaskNodeProvider {
        Binder<T> binder();

        @Override
        default boolean isValid() {
            return binder().isValid();
        }

        @Override
        default TaskNodeInfoData<?> getNodeInfo() {
            return isValid()
                    ? binder().getBean().node()
                    : null;
        }

        @Override
        default Task getTask() {
            return isValid()
                    ? binder().getBean().task()
                    : null;
        }
    }
}
