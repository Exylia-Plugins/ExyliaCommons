package net.exylia.commons.database.io;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class EntityExport {
    private String entityClass;
    private String tableName;
    private int count;
    private List<Map<String, Object>> entities;

}
