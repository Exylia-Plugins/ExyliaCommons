package net.exylia.commons.v2.namedcommand.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamedCommandEntry {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String name;
    private String command;

    public static NamedCommandEntry of(String name, String command) {
        return NamedCommandEntry.builder()
                .name(name)
                .command(command)
                .build();
    }

    public String getDisplayName() {
        if (name != null && !name.isBlank()) return name;
        return command != null ? command : "(not set)";
    }

    public NamedCommandEntry copy() {
        return NamedCommandEntry.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .command(command)
                .build();
    }

    public static List<String> toCommandStrings(List<NamedCommandEntry> entries) {
        return entries.stream()
                .map(NamedCommandEntry::getCommand)
                .collect(Collectors.toList());
    }
}
