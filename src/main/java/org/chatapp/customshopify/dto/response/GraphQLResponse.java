package org.chatapp.customshopify.dto.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class GraphQLResponse<T> {
    private T data;
    private List<GraphQLError> errors;
    
    @Data
    public static class GraphQLError {
        private String message;
        private List<Location> locations;
        private List<String> path;
        private Map<String, Object> extensions;
    }
    
    @Data
    public static class Location {
        private int line;
        private int column;
    }
    
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}
