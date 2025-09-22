package com.bosyon.gateway.monitor;

import com.bosyon.gateway.response.Return;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/net")
@CrossOrigin("*")
public class NetWorkCheck {

	 @GetMapping("/check")
	public Return<?> checkNetWork() {
		return Return.success("network is ok...");
	}
}
