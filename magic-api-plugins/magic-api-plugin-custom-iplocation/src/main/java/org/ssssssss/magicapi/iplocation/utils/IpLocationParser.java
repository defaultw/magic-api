package org.ssssssss.magicapi.iplocation.utils;

import org.springframework.util.StringUtils;
import org.ssssssss.magicapi.iplocation.bo.IpLocationDetail;

import java.util.Arrays;

/**
 * IP地址解析工具类
 * 针对格式："国家–省份–城市–区域 ISP"
 */
public class IpLocationParser {

    /**
     * 将DbSearcher返回的字符串解析为IpLocationDetail对象
     * 支持的输入格式示例：
     * 1. "柬埔寨"                 --> (country="柬埔寨")
     * 2. "中国–上海–上海 电信"     --> (country="中国", province="上海", city="上海", regionIsp="电信")
     * 3. "中国–浙江–杭州 阿里云"   --> (country="中国", province="浙江", city="杭州", regionIsp="阿里云")
     * 4. "新加坡 OVH"             --> (country="新加坡", regionIsp="OVH")
     * 5. "中国–上海–上海–虹口区 电信" --> (country="中国", province="上海", city="上海", regionIsp="虹口区 电信")
     *
     * @param locationStr DbSearcher.search返回的字符串
     * @return 解析后的IpLocationDetail对象，如果输入为null或空，则返回所有字段为null的对象
     */
    public static IpLocationDetail parse(String locationStr) {
        IpLocationDetail detail = new IpLocationDetail();
        
        if (!StringUtils.hasText(locationStr) || "未知".equals(locationStr.trim())) {
            return detail;
        }

        // 标准化字符串：替换多个空格为单个空格，并处理可能的分隔符变体
        String normalizedStr = locationStr.trim().replaceAll("\\s+", " ");
        // 使用正则表达式分割字符串，支持中文破折号和英文连字符
        // 限制分割次数为4，避免过度分割
        String[] parts = normalizedStr.split("–|-", 4); 

        // 根据分割后的部分数量进行解析
        switch (parts.length) {
            case 1:
                // 格式："柬埔寨"
                // 检查是否包含空格（如"新加坡 OVH"被整体匹配的情况）
                parseSinglePart(parts[0], detail);
                break;
            case 2:
                // 格式可能是："国家 区域ISP" 或 "国家–省份 区域ISP"
                // 需要进一步判断第二部分是否包含空格来确定具体格式
                parseTwoParts(parts[0], parts[1], detail);
                break;
            case 3:
                // 格式："中国–上海–上海 电信"
                parseThreeParts(parts[0], parts[1], parts[2], detail);
                break;
            case 4:
                // 完整格式："中国–上海–上海–虹口区 电信"
                parseFourParts(parts[0], parts[1], parts[2], parts[3], detail);
                break;
            default:
                // 对于更复杂或意外的情况，保守处理，尽可能提取信息
                handleComplexCase(parts, detail);
                break;
        }
        
        return detail;
    }

    private static void parseSinglePart(String part, IpLocationDetail detail) {
        // 检查这部分是否包含空格，如"新加坡 OVH"
        String[] subParts = part.split(" ", 2);
        if (subParts.length == 2) {
            detail.setCountry(getValueOrNull(subParts[0]));
            detail.setRegionIsp(getValueOrNull(subParts[1]));
        } else {
            // 不包含空格，则整个字符串为国家
            detail.setCountry(getValueOrNull(part));
        }
    }

    private static void parseTwoParts(String part1, String part2, IpLocationDetail detail) {
        // 假设第一部分是国家
        detail.setCountry(getValueOrNull(part1));
        
        // 第二部分可能是 "省份 城市 区域ISP" 的变体，或者是单纯的区域ISP
        // 这里简单处理，将整个第二部分作为regionIsp
        // 更精细的解析可以根据实际数据模式调整
        detail.setRegionIsp(getValueOrNull(part2));
    }

    private static void parseThreeParts(String part1, String part2, String part3, IpLocationDetail detail) {
        // 格式："中国–上海–上海 电信"
        detail.setCountry(getValueOrNull(part1));
        detail.setProvince(getValueOrNull(part2));
        
        // 第三部分可能包含城市和区域ISP（用空格分隔）
        String[] cityAndIsp = part3.split(" ", 2);
        if (cityAndIsp.length >= 1) {
            detail.setCity(getValueOrNull(cityAndIsp[0]));
        }
        if (cityAndIsp.length >= 2) {
            detail.setRegionIsp(getValueOrNull(cityAndIsp[1]));
        } else if (cityAndIsp.length == 1) {
            // 如果没有明确的ISP，但城市字段有值，可能城市名本身就包含了区域信息
            detail.setRegionIsp(getValueOrNull(cityAndIsp[0]));
        }
    }

    private static void parseFourParts(String part1, String part2, String part3, String part4, IpLocationDetail detail) {
        // 完整格式："中国–上海–上海–虹口区 电信"
        detail.setCountry(getValueOrNull(part1));
        detail.setProvince(getValueOrNull(part2));
        detail.setCity(getValueOrNull(part3));
        // 第四部分作为完整的区域ISP
        detail.setRegionIsp(getValueOrNull(part4));
    }

    private static void handleComplexCase(String[] parts, IpLocationDetail detail) {
        // 保守处理：尽可能提取信息
        if (parts.length > 0) detail.setCountry(getValueOrNull(parts[0]));
        if (parts.length > 1) detail.setProvince(getValueOrNull(parts[1]));
        if (parts.length > 2) detail.setCity(getValueOrNull(parts[2]));
        // 将剩余部分合并为regionIsp
        if (parts.length > 3) {
            StringBuilder regionIspBuilder = new StringBuilder();
            for (int i = 3; i < parts.length; i++) {
                if (regionIspBuilder.length() > 0) {
                    regionIspBuilder.append(" ");
                }
                regionIspBuilder.append(parts[i]);
            }
            detail.setRegionIsp(getValueOrNull(regionIspBuilder.toString()));
        }
    }

    /**
     * 空值处理：如果字符串为空或为无意义值，返回null
     */
    private static String getValueOrNull(String value) {
        if (value == null || value.trim().isEmpty() || 
            "0".equals(value.trim()) || "未知".equals(value.trim())) {
            return null;
        }
        return value.trim();
    }
}