package org.example.ajit9aser.mappers;

import org.example.ajit9aser.models.dto.user.UserRequestDto;
import org.example.ajit9aser.models.dto.user.UserResponseDto;
import org.example.ajit9aser.models.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(UserRequestDto dto);

    UserResponseDto toResponseDto(User user);
}
