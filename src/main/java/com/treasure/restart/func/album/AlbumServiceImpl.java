package com.treasure.restart.func.album;

import com.treasure.restart.func.UserClient;
import com.treasure.restart.dto.UserSimpleVO;
import com.treasure.restart.dto.AlbumListResponse;
import com.treasure.restart.entity.AlbumItem;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AlbumServiceImpl implements AlbumService {
    @Resource
    private AlbumMapper albumMapper;

    @Resource
    private UserClient userClient;

    @Override
    public AlbumListResponse getAlbumList() {
        List<AlbumItem> imageList = albumMapper.getAlbumList();
        enrichUserInfo(imageList);

        AlbumListResponse response = new AlbumListResponse();
        response.setImageList(imageList);
        response.setTotal(albumMapper.getCount());
        return response;
    }

    private void enrichUserInfo(List<AlbumItem> imageList) {
        if (imageList == null || imageList.isEmpty()) {
            return;
        }

        List<Long> userIds = imageList.stream()
                .map(AlbumItem::getCreateUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UserSimpleVO> userMap = userClient.getUsersByIds(userIds);

        for (AlbumItem item : imageList) {
            UserSimpleVO user = userMap.get(item.getCreateUserId());
            item.setCreateUserName(user == null ? null : user.username());
            item.setCreateUserAvatar(user == null ? null : user.avatar());
        }
    }
}
