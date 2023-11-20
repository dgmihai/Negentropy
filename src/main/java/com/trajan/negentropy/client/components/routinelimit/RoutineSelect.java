package com.trajan.negentropy.client.components.routinelimit;

import com.trajan.negentropy.client.components.routinelimit.RoutineSelect.SelectOptions;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.model.filter.RoutineLimitFilter;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Scope("prototype")
public class RoutineSelect extends Select<SelectOptions> {
    @Autowired private UserSettings settings;
    @Setter private Supplier<RoutineLimitFilter> customFilterSupplier;

    @Getter
    public enum SelectOptions {
        WITH_CURRENT_FILTER("With Current Filter"),
        WITH_CUSTOM_FILTER("With Custom Filter"),
        NO_FILTER("No Filter");

        private final String value;

        SelectOptions(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @PostConstruct
    public void init() {
        this.setItems(SelectOptions.values());
        this.setValue(SelectOptions.WITH_CURRENT_FILTER);
        this.addThemeVariants(SelectVariant.LUMO_SMALL);
    }

    public RoutineLimitFilter getFilter() {
        return switch(this.getValue()) {
            case WITH_CURRENT_FILTER -> RoutineLimitFilter.parse(settings.filter());
            case WITH_CUSTOM_FILTER -> {
                if (customFilterSupplier == null) {
                    throw new NotImplementedException("Custom filter not yet implemented");
                } else {
                    yield customFilterSupplier.get();
                }
            }
            case NO_FILTER -> (RoutineLimitFilter) new RoutineLimitFilter()
                    .completed(false);
        };
    }
}
