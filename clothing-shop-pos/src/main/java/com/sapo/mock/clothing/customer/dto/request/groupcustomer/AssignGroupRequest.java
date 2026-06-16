package com.sapo.mock.clothing.customer.dto.request.groupcustomer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignGroupRequest {
    // null = rút khách ra khỏi nhóm, có giá trị = gán vào nhóm đó
    private Integer customerGroupId;
}
