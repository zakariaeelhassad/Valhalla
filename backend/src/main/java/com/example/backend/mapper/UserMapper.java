package com.example.backend.mapper;

import com.example.backend.dto.profile.UserResponse;
import com.example.backend.model.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
