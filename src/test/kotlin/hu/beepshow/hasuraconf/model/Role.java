package hu.beepshow.hasuraconf.model;

import hu.beepshow.hasuraconf.annotation.HasuraEnum;

import javax.persistence.*;

// Enum type conforming to Hasura's expected enum type table definition
// https://docs.hasura.io/1.0/graphql/manual/schema/enums.html
@Entity
@Table(name = "user_role_type")
@HasuraEnum
public enum Role {
    ROLE_USER("A normal user"),
    ROLE_ORGANIZER("An organizer"),
    ROLE_ADMIN("An admin user");

    @Id
    @Column(columnDefinition = "TEXT")
    public String value = toString();

    @Column(columnDefinition = "TEXT")
    public String description;

    Role(String description)
    {
        this.description = description;
    }
}
