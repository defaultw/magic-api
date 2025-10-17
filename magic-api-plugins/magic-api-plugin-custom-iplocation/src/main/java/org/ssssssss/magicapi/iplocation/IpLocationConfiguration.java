package org.ssssssss.magicapi.iplocation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

/**
 * TODO
 *
 * @author Default.W
 * @date 2025/10/17
 */
@Configuration
@EnableConfigurationProperties(IpLocationConfig.class)
public class IpLocationConfiguration implements MagicPluginConfiguration {

    private final IpLocationConfig ipLocationConfig;

    public IpLocationConfiguration(IpLocationConfig ipLocationConfig) {
        this.ipLocationConfig = ipLocationConfig;
    }

    @Override
    public Plugin plugin() {
        return new Plugin("IpLocation");
    }

    @Bean
    public IpLocationModule ipLocationModule(IpLocationConfig ipLocationConfig) {
        return new IpLocationModule(ipLocationConfig);
    }
}
