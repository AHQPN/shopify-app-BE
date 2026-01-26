package org.chatapp.customshopify.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Builder
@NoArgsConstructor
public class ApiResponse<T>  {
    @Builder.Default
    private Integer code = 1000;
    private String message;
    private T data;
}
