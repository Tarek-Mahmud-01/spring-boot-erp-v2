package com.springboot.erp.modules.access.mapper;

import com.springboot.erp.modules.access.domain.Role;
import com.springboot.erp.modules.access.domain.User;
import com.springboot.erp.modules.access.dto.AuthDtos.CurrentUserResponse;
import com.springboot.erp.modules.access.dto.UserDtos.UserRow;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

/** MapStruct entity↔DTO mapper for the access module (ARCHITECTURE.md §2). */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @org.mapstruct.Mapping(target = "roles", source = "roles", qualifiedByName = "roleCodes")
    UserRow toRow(User user);

    @org.mapstruct.Mapping(target = "permissions", expression = "java(new java.util.ArrayList<>(user.permissionCodes()))")
    CurrentUserResponse toCurrentUser(User user);

    @Named("roleCodes")
    default List<String> roleCodes(java.util.Set<Role> roles) {
        return roles.stream().map(Role::getCode).sorted().toList();
    }
}
