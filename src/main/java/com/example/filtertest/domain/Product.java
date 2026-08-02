package com.example.filtertest.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Table("products")
public record Product(@Id Integer id, String name, String description) implements Persistable<Integer> {

    // The API only ever reads products; the id is externally assigned rather than DB-generated, so
    // Spring Data's default null-id "is new" check would otherwise treat every save as an UPDATE.
    // Persistable.isNew()=true forces INSERT semantics, which is all seeding fixtures ever needs.
    // This is an internal persistence entity now (the API returns a separate ProductResponse DTO),
    // so there's no JSON-leak concern to guard against here.
    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return true;
    }
}
