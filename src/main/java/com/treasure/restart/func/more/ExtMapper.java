package com.treasure.restart.func.more;

import com.treasure.restart.entity.AlbumItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExtMapper {

    @Select("""
            SELECT
                mi.id AS id,
                mi.moment_id AS momentId,
                mi.image_url AS imageUrl,
                mi.sort,
                mi.created_at AS createdAt,
                m.content AS momentContent,
                m.user_id AS createUserId
                FROM moment_images mi
                INNER JOIN moments m ON mi.moment_id = m.id
                INNER JOIN (SELECT id FROM moments ORDER BY RAND() LIMIT 3) rm ON m.id = rm.id
                            WHERE mi.sort = (SELECT MIN(mi2.sort) FROM moment_images mi2 WHERE mi2.moment_id = mi.moment_id)
                ORDER BY RAND()
            """)
    List<AlbumItem> getCarouselList();
}
