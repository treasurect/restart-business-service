package com.treasure.restart.func.more;

import com.treasure.common.exception.BusinessException;
import com.treasure.common.response.BaseResponse;
import com.treasure.restart.entity.AlbumItem;
import com.treasure.restart.entity.HistoryItem;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/ext")
public class ExtController {
    @Resource
    private ExtService extService;

    private final ObjectMapper objectMapper;

    public ExtController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("carousel/list")
    BaseResponse<List<AlbumItem>> getCarouselList() {
        return BaseResponse.success("获取成功", extService.getCarouselList());
    }

    @GetMapping("/history/{region}")
    public BaseResponse<List<HistoryItem>> getHistoryList(@PathVariable String region) {
        String historyPath = switch (region) {
            case "china" -> "/opt/restart-server/data/history_china.json";
            case "europe" -> "/opt/restart-server/data/history_europe.json";
            case "usa" -> "/opt/restart-server/data/history_usa.json";
            case "russia" -> "/opt/restart-server/data/history_russia.json";
            default -> throw new BusinessException(404, "不支持的数据类型");
        };
        try {
            Path path = Paths.get(historyPath);
            if (!path.toFile().exists()) throw new BusinessException(404, "历史数据文件不存在");
            List<HistoryItem> historyItems = objectMapper.readValue(path.toFile(), new TypeReference<List<HistoryItem>>() {
            });
            return BaseResponse.success("获取历史数据成功", historyItems);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(500, "获取历史数据失败");
        }
    }
}
