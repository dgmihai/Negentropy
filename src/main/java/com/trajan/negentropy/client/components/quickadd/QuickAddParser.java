package com.trajan.negentropy.client.components.quickadd;

import com.trajan.negentropy.client.util.DurationConverter;
import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.Task;
import com.vaadin.flow.data.binder.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashSet;

public class QuickAddParser {
    private static final Logger logger = LoggerFactory.getLogger(QuickAddParser.class);

    public static String DELIMITER = "#";

    public static Task parse(String input) {
        logger.debug("Parsing " + input);
        Task task = new Task(null)
                .oneTime(true)
                .description("")
                .duration(Duration.ZERO)
                .tags(new HashSet<>());

        // Add '#' to the beginning of the input string to handle cases like '#rep' without a trailing space
        input = input.trim();

        // Your implementation here
        String[] splitInput = input.split(DELIMITER);
        task.name(splitInput[0].trim());

        for (int i = 1; i < splitInput.length; i++) {
            String keywordAndValue = splitInput[i].trim();
            String keyword = "";
            String value = "";

            if (!keywordAndValue.equals("rep")) {
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
                case "desc" -> task.description(value);
                case "tag" -> {
                    String[] tags = value.split(",");
                    for (String tag : tags) {
                        task.tags().add(new Tag(null, tag.trim()));
                    }
                }
                case "dur" -> {
                    Result<Duration> result = DurationConverter.toModel(value);
                    if (result.isError()) {
                        NotificationError.show("Invalid duration: " + value);
                        return null;
                    }
                    result.ifOk(task::duration);
                }
                case "rep" -> task.oneTime(false);
                default -> {
                    NotificationError.show("Invalid keyword specified: " + value);
                    return null;
                }
            }
        }
        return task;
    }
}



