package org.ssssssss.magicapi.iplocation;

import net.cz88.czdb.QueryType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * TODO
 *
 * @author Default.W
 * @date 2025/10/18
 */
@ConfigurationProperties(prefix = "magic-api.plugin.custom.ip-location")
public class IpLocationConfig {

    private String ipv4Path;

    private String ipv6Path;

    private QueryType queryType;

    private String secret;

    public String getIpv4Path() {
        return ipv4Path;
    }

    public void setIpv4Path(String ipv4Path) {
        this.ipv4Path = ipv4Path;
    }

    public String getIpv6Path() {
        return ipv6Path;
    }

    public void setIpv6Path(String ipv6Path) {
        this.ipv6Path = ipv6Path;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public void setQueryType(QueryType queryType) {
        this.queryType = queryType;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Override
    public String toString() {
        return "IpLocationConfig{" +
                "ipv4Path='" + ipv4Path + '\'' +
                ", ipv6Path='" + ipv6Path + '\'' +
                ", searchType=" + queryType +
                ", secret='" + secret + '\'' +
                '}';
    }
}
