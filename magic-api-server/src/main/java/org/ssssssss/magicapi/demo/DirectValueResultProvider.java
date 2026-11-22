package org.ssssssss.magicapi.demo;

import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.interceptor.ResultProvider;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.modules.db.model.PageResult;
import org.springframework.stereotype.Component;

/**
 * 自定义接口返回值：成功时直接返回接口数据，不再包装 JsonBean
 * （官方 ResultProvider 模式，见
 * https://www.ssssssss.org/magic-api/pages/base/response/）
 * <p>
 * 成功（code=1）且非分页结果 → 原样返回脚本数据（裸数据）；
 * 分页结果（db.page / response.page 产生的 PageResult）、
 * exit/assert（code≠1）、异常、参数校验失败 → 保留默认 JsonBean 包装，
 * 便于调用方统一识别错误码。
 * <p>
 * starter 以 @ConditionalOnMissingBean(ResultProvider.class) 注册默认实现，
 * 本类声明为 @Component 后即接管全部响应构建。
 */
@Component
public class DirectValueResultProvider implements ResultProvider {

	@Override
	public Object buildResult(RequestEntity requestEntity, int code, String message, Object data) {
		// 成功且非分页：直接返回接口数据
		if (code == Constants.RESPONSE_CODE_SUCCESS && !(data instanceof PageResult)) {
			return data;
		}
		// 分页 / exit / assert / 异常 / 参数校验失败：保留默认包装
		return new JsonBean<>(code, message, data, (int) (System.currentTimeMillis() - requestEntity.getRequestTime()));
	}
}
