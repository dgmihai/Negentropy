package com.trajan.negentropy.client.components.quickcreate;

import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.vaadin.flow.data.binder.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class QuickCreateParser {
    private static final Logger logger = LoggerFactory.getLogger(QuickCreateParser.class);

    public static String DELIMITER = "#";

    public static final String DESCRIPTION = "desc";
    public static final String TAGS = "tag";
    public static final String DURATION = "dur";
    public static final String RECURRING = "rec";
    public static final String TOP = "top";

    public static Pair<Task, TaskNodeInfoData<TaskNodeDTO>> parse(String input) throws ParseException {
        logger.debug("Parsing " + input);
        Task task = new Task();
        task.description("")
            .duration(Duration.ZERO);
        TaskNodeInfoData<TaskNodeDTO> nodeDTO = new TaskNodeDTO()
                .recurring(false);

        // Add '#' to the beginning of the input string to handle cases like '#rep' without a trailing space
        input = input.trim();

        String[] splitInput = input.split(DELIMITER);
        task.name(splitInput[0].trim());

        for (int i = 1; i < splitInput.length; i++) {
            String keywordAndValue = splitInput[i].trim();
            String keyword = "";
            String value = "";

            // TODO: Top for placing at front instead of at back
            if (!keywordAndValue.equals(RECURRING) && !keywordAndValue.equals(TOP)) {
                int firstSpaceIndex = keywordAndValue.indexOf(' ');
                if (firstSpaceIndex == -1) {
                    continue;
                }
                keyword = keywordAndValue.substring(0, firstSpaceIndex).toLowerCase();
                value = keywordAndValue.substring(firstSpaceIndex + 1).trim();
            } else {
                keyword = keywordAndValue;
            }

            switch (keyword) {
                case DESCRIPTION -> task.description(value);
                case TAGS -> {
                    Set<Tag> tagSet = new HashSet<>();
                    String[] tags = value.split(",");
                    for (String tag : tags) {
                        tagSet.add(new Tag(null, tag.trim()));
                    }
                    task.tags(tagSet);
                }
                case DURATION -> {
                    Result<Duration> result = DurationConverter.toModel(value);
                    if (result.isError()) {
                        throw new ParseException("Invalid duration: " + value);
                    }
                    result.ifOk(task::duration);
                }
                case RECURRING -> nodeDTO.recurring(true);
                case TOP -> nodeDTO.position(0);
                default -> throw new ParseException("Invalid keyword specified: " + value);
            }
        }

        return Pair.of(task, nodeDTO);
    }

    public static class ParseException extends Exception{
        public ParseException(String message) {
            super(message);
        }
    }
}



