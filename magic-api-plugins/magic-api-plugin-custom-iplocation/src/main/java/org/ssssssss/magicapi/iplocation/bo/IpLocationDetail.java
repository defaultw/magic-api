package org.ssssssss.magicapi.iplocation.bo;

/**
 * TODO
 *
 * @author Default.W
 * @date 2025/10/18
 */
public class IpLocationDetail {

    /** 国家 */
    private String country;

    /** 省份/地区 */
    private String province;

    /** 城市 */
    private String city;

    /** 区域与运营商（作为一个整体字段，例如"虹口区 电信"、"阿里云"） */
    private String regionIsp;

    public IpLocationDetail() {}

    public IpLocationDetail(String country, String province, String city, String regionIsp) {
        this.country = country;
        this.province = province;
        this.city = city;
        this.regionIsp = regionIsp;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getRegionIsp() {
        return regionIsp;
    }

    public void setRegionIsp(String regionIsp) {
        this.regionIsp = regionIsp;
    }

    @Override
    public String toString() {
        return "IpLocationDetail{" +
                "country='" + country + '\'' +
                ", province='" + province + '\'' +
                ", city='" + city + '\'' +
                ", regionIsp='" + regionIsp + '\'' +
                '}';
    }
}
