package com.wesleyhome.library.server.repository;

import com.wesleyhome.library.db.tables.records.MediaItemsRecord;
import org.jooq.DSLContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.wesleyhome.library.db.Tables.MEDIA_ITEMS;

@Repository
public class MediaRepository {

    private final DSLContext dsl;
    private final JdbcTemplate jdbc;

    public MediaRepository(DSLContext dsl, JdbcTemplate jdbc) {
        this.dsl = dsl;
        this.jdbc = jdbc;
    }

    public Optional<MediaItemsRecord> findById(String id) {
        return dsl.selectFrom(MEDIA_ITEMS)
                .where(MEDIA_ITEMS.ID.eq(id))
                .fetchOptional();
    }

    public List<MediaItemsRecord> findAll(String query, String type, int limit, int offset) {
        var select = dsl.selectFrom(MEDIA_ITEMS);
        
        if (query != null && !query.isBlank()) {
            select.where(MEDIA_ITEMS.TITLE.containsIgnoreCase(query));
        }
        if (type != null) {
            select.where(MEDIA_ITEMS.MEDIA_TYPE.eq(type));
        }
        
        return select.limit(limit).offset(offset).fetch();
    }

    public void save(MediaItemsRecord record) {
        // Use dsl to generate the SQL and parameters, then execute via JdbcTemplate
        // Or just use dsl.execute() since DSLContext is already integrated with Spring Transaction Manager
        record.attach(dsl.configuration());
        record.store();
    }

    public void deleteById(String id) {
        dsl.deleteFrom(MEDIA_ITEMS)
                .where(MEDIA_ITEMS.ID.eq(id))
                .execute();
    }
}
