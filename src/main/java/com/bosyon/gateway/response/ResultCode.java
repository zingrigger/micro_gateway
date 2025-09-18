package com.bosyon.gateway.response;


public enum ResultCode {
	
	//成功
	SUCCESS(200, "成功"),
	//服务不可用
	SERVER_ERROR(50001, "服务不可用"),
	//失败
	FAILED(503, "失败"),
	//incorrect错误
	LOGIN_ERROR(404100, "登录失败，请重新登录"),
	//load menus error
	MENUS_ERROR(404200, "加载菜单失败"),
	//result is null
	RESULT_NULL(404200, "结果为空"),
	//服务错误
	SERVER_EXCEPTION(500100, "系统错误"),
	//参数错误
	PARAM_EXCEPTION(400100, "参数错误"),
	//token过期
	TOKEN_EXPIRED(400101, "token过期"),
	//token解析错误
	TOKEN_PARSE_ERROR(400102, "token解析错误"),
	//手机号/邮箱格式错误
	PHONE_EMAIL_ERROR(503100, "手机号/邮箱格式错误");
	
	
	//返回码
	private final int code;
	//描述
	private final String depict;

	//构造函数
	ResultCode(int code, String depict) {
		this.code = code;
		this.depict = depict;
	}

	public int getCode() {
		return code;
	}

	public String getDepict() {
		return depict;
	}
	
}
