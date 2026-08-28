package cn.y.yapiclient.innerservice;

import cn.y.yapimodel.entity.UserInterface;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

/**
 * 用户接口调用服务内部调用
 */
public interface InnerUserInterfaceService {

    /**
     * 统计接口调用次数
     * @param interfaceId
     * @param userId
     * @return
     */
    boolean invokeCount(long interfaceId, long userId);

    long count(QueryWrapper<UserInterface> queryWrapper);
}
