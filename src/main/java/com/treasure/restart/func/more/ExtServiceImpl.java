package com.treasure.restart.func.more;

import com.treasure.restart.client.UserClient;
import com.treasure.restart.client.UserSimpleVO;
import com.treasure.restart.entity.AlbumItem;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ExtServiceImpl implements ExtService {
    @Resource
    private ExtMapper extMapper;

    @Resource
    private UserClient userClient;

    @Override
    public List<AlbumItem> getCarouselList() {
        List<AlbumItem> items = extMapper.getCarouselList();
        enrichUserInfo(items);
        return items;
    }

    private void enrichUserInfo(List<AlbumItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<Long> userIds = items.stream()
                .map(AlbumItem::getCreateUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UserSimpleVO> userMap = userClient.getUsersByIds(userIds);

        for (AlbumItem item : items) {
            UserSimpleVO user = userMap.get(item.getCreateUserId());
            item.setCreateUserName(user == null ? null : user.username());
            item.setCreateUserAvatar(user == null ? null : user.avatar());
        }
    }
}
