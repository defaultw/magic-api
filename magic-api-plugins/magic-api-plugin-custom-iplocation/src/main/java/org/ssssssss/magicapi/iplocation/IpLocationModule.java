package org.ssssssss.magicapi.iplocation;

import net.cz88.czdb.DbSearcher;
import net.cz88.czdb.exception.IpFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.iplocation.bo.IpLocationDetail;
import org.ssssssss.magicapi.iplocation.utils.IpLocationParser;
import org.ssssssss.script.annotation.Comment;

import java.io.IOException;

/**
 * TODO
 *
 * @author Default.W
 * @date 2025/10/17
 */
@MagicModule("ipLocation")
public class IpLocationModule {

    private final IpLocationConfig ipLocationConfig;

    private static volatile DbSearcher searcher4;
    private static volatile DbSearcher searcher6;

    private final Logger logger = LoggerFactory.getLogger(IpLocationModule.class);


    public IpLocationModule(IpLocationConfig ipLocationConfig) {
        this.ipLocationConfig = ipLocationConfig;
    }

    @Comment("查询IPv4地址对应的物理地址")
    public String ipv4(String ip) {
        try {
            return getSearcher4().search(ip);
        } catch (Exception e) {
            logger.error("查询ipv4对应地理位置失败", e);
        }
        return "未知";
    }

    @Comment("查询IPv4地址对应的物理地址，返回详情对象")
    public IpLocationDetail ipv4Detail(String ip) {
        return IpLocationParser.parse(ipv4(ip));
    }

    @Comment("查询IPv6地址对应的物理地址")
    public String ipv6(String ip) {
        try {
            return getSearcher6().search(ip);
        } catch (Exception e) {
            logger.error("查询ipv6对应地理位置失败", e);
        }
        return "未知";
    }

    @Comment("查询IPv4地址对应的物理地址，返回详情对象")
    public IpLocationDetail ipv6Detail(String ip) {
        return IpLocationParser.parse(ipv6(ip));
    }

    private DbSearcher getSearcher4() throws Exception {
        // 第一次检查：避免不必要的同步开销[5](@ref)
        DbSearcher result = searcher4;
        if (result == null) {
            synchronized (IpLocationModule.class) {
                result = searcher4;
                // 第二次检查：防止多个线程同时通过第一次检查后重复创建实例[1](@ref)
                if (result == null) {
                    searcher4 = result = createDbSearcher(4);
                }
            }
        }
        return result;
    }

    private DbSearcher getSearcher6() throws Exception {
        DbSearcher result = searcher6;
        if (result == null) {
            synchronized (IpLocationModule.class) {
                result = searcher6;
                if (result == null) {
                    searcher6 = result = createDbSearcher(6);
                }
            }
        }
        return result;
    }

    private DbSearcher createDbSearcher(int ipType) throws Exception {
        String path = "";
        if (ipType == 4) {
            path = ipLocationConfig.getIpv4Path();
        } else if (ipType == 6) {
            path = ipLocationConfig.getIpv6Path();
        }

        if (path == null || path.trim().isEmpty()) {
            logger.warn("IP类型{}的数据库路径为空，跳过初始化", ipType);
            throw new NullPointerException("IP类型{}的数据库路径为空，跳过初始化");
        }

        return new DbSearcher(path, ipLocationConfig.getQueryType(), ipLocationConfig.getSecret());
    }

}
