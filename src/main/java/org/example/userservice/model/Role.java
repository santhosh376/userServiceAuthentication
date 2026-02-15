package org.example.userservice.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.annotation.JsonDeserialize;

@Getter
@Setter
@Entity
@JsonDeserialize(as =  Role.class)
public class Role extends BaseModel {
    private String role;
}
